package com.russoviller.cryogenic.live

import android.os.Handler
import android.os.Looper
import com.russoviller.cryogenic.utils.logging.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Chat da live do TikTok direto pela conexão do chat (webcast), sem abrir a página do TikTok.
 *
 * 1. Descobre a sala da live (api-live/user/room do TikTok) pelo @usuário.
 * 2. Pede ao servidor de assinatura (Euler Stream, o mesmo das bibliotecas TikTok-Live-Connector e
 *    TikTokLiveJava) o pacote inicial da sala e os cookies da conexão: o TikTok exige que o pedido
 *    seja assinado e não tem API pública para isso.
 * 3. Abre o WebSocket do chat e decodifica as mensagens (protobuf) aqui mesmo.
 *
 * Tudo roda em uma thread de prioridade mínima e só troca alguns KB por minuto: nada de navegador,
 * JavaScript ou página desenhada (o leitor antigo, pela página web, pesava no jogo).
 *
 * Callbacks sempre na thread principal.
 */
class TikTokLiveChat(
    private val onMessage: (user: String, text: String) -> Unit,
    private val onState: (String) -> Unit,
    private val onViewers: (String) -> Unit = {},
    private val onAvatar: (String) -> Unit = {},
    private val onDetail: (String) -> Unit = {},
    /** chave opcional do Euler Stream (mais conexões por dia) */
    private val apiKey: () -> String = { "" }
) {
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        // o WebSocket: pings próprios (heartbeat do TikTok) + ping do protocolo para detectar queda
        .pingInterval(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Volatile private var user = ""
    @Volatile private var generation = 0
    private var executor: ScheduledExecutorService? = null
    private var heartbeat: ScheduledFuture<*>? = null
    private var roomCheck: ScheduledFuture<*>? = null
    @Volatile private var socket: WebSocket? = null

    // diagnóstico (só na thread do leitor; publicados na principal)
    @Volatile private var socketOpen = false
    @Volatile private var frames = 0
    @Volatile private var messages = 0
    @Volatile private var roomId = ""
    @Volatile private var signInfo = ""
    @Volatile private var failures = 0
    /** sala que estava ao vivo nesta sessão (para saber quando o TikTok encerrou a live) */
    @Volatile private var liveRoomId = ""
    /** o TikTok encerrou a live da sala [endedRoomId]: espera uma live nova (outra sala) */
    @Volatile private var ended = false
    @Volatile private var endedRoomId = ""
    private var state = ""
    private var lastDetail = ""

    val running: Boolean get() = executor != null

    fun start(tiktokUser: String) {
        val normalized = LiveConfig.normalizeUser(tiktokUser)
        if (normalized.isEmpty()) {
            stop()
            post { onState("") }
            return
        }
        if (executor != null && normalized == user) return
        stop()
        user = normalized
        val gen = ++generation
        failures = 0
        liveRoomId = ""
        ended = false
        endedRoomId = ""
        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread({
                runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST) }
                r.run()
            }, "TikTokLiveChat").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
        }
        setState(STATE_LOADING)
        schedule(gen, 0L) { connect(gen) }
    }

    fun stop() {
        generation++
        heartbeat?.cancel(false)
        heartbeat = null
        roomCheck?.cancel(false)
        roomCheck = null
        socket?.let { runCatching { it.close(1000, null) } }
        socket = null
        executor?.shutdownNow()
        executor = null
        user = ""
        socketOpen = false
        frames = 0
        messages = 0
        roomId = ""
        signInfo = ""
        lastDetail = ""
    }

    // ------------------------------------------------------------------ conexão

    private fun schedule(gen: Int, delayMs: Long, block: () -> Unit) {
        val ex = executor ?: return
        runCatching { ex.schedule({ if (gen == generation) runCatching(block).onFailure { Logger.error(TAG, "Chat task failed", it) } }, delayMs, TimeUnit.MILLISECONDS) }
    }

    /** Nova tentativa com espera crescente (o servidor de assinatura grátis tem limite diário). */
    private fun retry(gen: Int, state: String) {
        setState(state)
        failures++
        val delay = when {
            state == STATE_NOT_FOUND -> 20_000L
            state == STATE_ENDED || state == STATE_SUSPENDED -> 30_000L
            state == STATE_LIMIT -> 5 * 60_000L
            else -> (10_000L shl (failures - 1).coerceIn(0, 4)).coerceAtMost(120_000L)
        }
        schedule(gen, delay) { connect(gen) }
    }

    private fun connect(gen: Int) {
        val u = user
        if (u.isEmpty()) return
        socketOpen = false
        publishDetail()
        // 1) sala da live
        val room = runCatching { fetchRoom(u) }.getOrElse {
            signInfo = "room: " + (it.message ?: it.javaClass.simpleName).take(30)
            publishDetail()
            retry(gen, if (state == STATE_OK || state == STATE_WAITING) STATE_LOADING else state.ifEmpty { STATE_LOADING })
            return
        }
        if (room.avatar.startsWith("https://")) post { onAvatar(room.avatar) }
        if (room.viewers >= 0 && room.live) post { onViewers(LiveFloatingWindow.formatCount(room.viewers.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())) }
        if (ended) {
            // depois do fim: só volta quando o TikTok abrir uma live nova (outra sala)
            if (!room.live || room.roomId.isEmpty() || room.roomId == endedRoomId) {
                signInfo = "ended ${endedRoomId.takeLast(6)}"
                publishDetail()
                retry(gen, if (state == STATE_SUSPENDED) STATE_SUSPENDED else STATE_ENDED)
                return
            }
            ended = false
            endedRoomId = ""
        }
        if (!room.live || room.roomId.isEmpty()) {
            roomId = room.roomId
            signInfo = "status ${room.status}"
            publishDetail()
            if (liveRoomId.isNotEmpty() && room.notLive) {
                // estava ao vivo e a sala fechou: o TikTok encerrou a live
                markEnded(gen, STATE_ENDED)
                return
            }
            // a live ainda não começou no TikTok (ou @ errado): tenta de novo
            retry(gen, STATE_NOT_FOUND)
            return
        }
        roomId = room.roomId
        liveRoomId = room.roomId
        // 2) assinatura + pacote inicial
        val sign = runCatching { fetchSigned(room.roomId) }.getOrElse {
            val limit = it is SignLimit
            signInfo = "sign: " + (it.message ?: it.javaClass.simpleName).take(40)
            publishDetail()
            retry(gen, if (limit) STATE_LIMIT else STATE_LOADING)
            return
        }
        signInfo = "sign ok"
        if (gen != generation) return
        handleResult(sign.result)
        // 3) WebSocket
        val url = buildSocketUrl(sign.result, sign.roomId.ifEmpty { room.roomId })
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Referer", "https://www.tiktok.com/")
            .header("Origin", "https://www.tiktok.com")
            .header("Cookie", sign.cookie)
            .build()
        socket = http.newWebSocket(request, Listener(gen))
        setState(if (messages > 0) STATE_OK else STATE_WAITING)
    }

    private inner class Listener(private val gen: Int) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (gen != generation) { webSocket.cancel(); return }
            socketOpen = true
            failures = 0
            publishDetail()
            heartbeat?.cancel(false)
            heartbeat = executor?.scheduleWithFixedDelay({
                if (gen == generation) webSocket.send(HEARTBEAT.toByteString())
            }, 0L, 10_000L, TimeUnit.MILLISECONDS)
            // confere a sala de vez em quando: se o TikTok fechar a live sem avisar pelo chat
            roomCheck?.cancel(false)
            roomCheck = executor?.scheduleWithFixedDelay({ checkRoom(gen) }, 60_000L, 60_000L, TimeUnit.MILLISECONDS)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            if (gen != generation) return
            frames++
            runCatching { handleFrame(webSocket, bytes.toByteArray()) }
                .onFailure { Logger.warning(TAG, "Bad chat frame: ${it.message}") }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = lost(webSocket, "closed $code")

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
            lost(webSocket, (response?.header("handshake-msg") ?: t.message ?: t.javaClass.simpleName).take(40))

        private fun lost(webSocket: WebSocket, why: String) {
            if (gen != generation || socket !== webSocket) return
            socket = null
            socketOpen = false
            heartbeat?.cancel(false)
            roomCheck?.cancel(false)
            // fechada por nós porque a live acabou: markEnded já agendou a próxima checagem
            if (ended) return
            signInfo = "ws: $why"
            publishDetail()
            retry(gen, STATE_LOADING)
        }
    }

    private fun handleFrame(ws: WebSocket, data: ByteArray) {
        val frame = Proto.decodePushFrame(data)
        if (frame.payload.isEmpty()) return
        if (frame.payloadType != "msg") return
        val payload = gunzipIfNeeded(frame.payload)
        val result = Proto.decodeFetchResult(payload)
        if (result.needsAck && frame.logId != 0L) {
            ws.send(Proto.encodeAck(frame.logId, result.internalExt.toByteArray()).toByteString())
        }
        handleResult(result)
    }

    /** quantas mensagens de cada tipo chegaram (diagnóstico/testes) */
    internal val methodCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private fun handleResult(result: Proto.FetchResult) {
        for (m in result.messages) {
            methodCounts.merge(m.method, 1, Int::plus)
            when (m.method) {
                "WebcastChatMessage" -> Proto.decodeChat(m.payload)?.let { (name, text) ->
                    messages++
                    post {
                        setState(STATE_OK)
                        onMessage(name.take(40), text.take(300))
                    }
                }
                "WebcastRoomUserSeqMessage" -> Proto.decodeViewers(m.payload)?.let { total ->
                    post { onViewers(LiveFloatingWindow.formatCount(total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())) }
                }
                "WebcastControlMessage" -> when (Proto.decodeControlAction(m.payload)) {
                    // 3 = live encerrada; 4 = encerrada pelo TikTok (aviso/restrição)
                    3L -> markEnded(generation, STATE_ENDED)
                    4L -> markEnded(generation, STATE_SUSPENDED)
                    else -> Unit
                }
            }
        }
        publishDetail()
    }

    private fun checkRoom(gen: Int) {
        if (gen != generation || ended) return
        val u = user.ifEmpty { return }
        val room = runCatching { fetchRoom(u) }.getOrNull() ?: return
        if (room.notLive || (room.live && liveRoomId.isNotEmpty() && room.roomId.isNotEmpty() && room.roomId != liveRoomId)) markEnded(gen, STATE_ENDED)
    }

    /** O TikTok encerrou a live: avisa, fecha a conexão e passa a esperar uma live nova. */
    private fun markEnded(gen: Int, why: String) {
        if (gen != generation || ended) return
        ended = true
        endedRoomId = liveRoomId.ifEmpty { roomId }
        liveRoomId = ""
        socketOpen = false
        heartbeat?.cancel(false)
        roomCheck?.cancel(false)
        socket?.let { ws -> socket = null; runCatching { ws.close(1000, null) } }
        signInfo = "ended"
        publishDetail()
        Logger.warning(TAG, "TikTok ended the live ($why) room=$endedRoomId")
        failures = 0
        retry(gen, why)
    }

    // ------------------------------------------------------------------ HTTP

    private class SignLimit(message: String) : Exception(message)

    private class Signed(val result: Proto.FetchResult, val cookie: String, val roomId: String)

    private fun fetchRoom(u: String): RoomInfo {
        val url = "https://www.tiktok.com/api-live/user/room/?aid=1988&app_name=tiktok_web&device_platform=web_pc" +
            "&sourceType=54&uniqueId=" + enc(u)
        val req = Request.Builder().url(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Accept", "application/json")
            .header("Referer", "https://www.tiktok.com/@$u/live")
            .build()
        http.newCall(req).execute().use { r ->
            if (r.code != 200) throw IllegalStateException("http ${r.code}")
            val body = r.body.byteStream().use { readLimited(it, 2_000_000) }
            return parseRoomInfo(String(body, Charsets.UTF_8))
        }
    }

    private fun fetchSigned(room: String): Signed {
        val params = LinkedHashMap(DEFAULT_PARAMS)
        params["client"] = "ttlive-java"
        params["room_id"] = room
        val builder = Request.Builder().url(SIGN_URL + "?" + query(params))
            .header("User-Agent", DESKTOP_UA)
            .header("Accept", "application/protobuf")
        apiKey().trim().takeIf { it.isNotEmpty() }?.let { builder.header("x-api-key", it) }
        http.newCall(builder.build()).execute().use { r ->
            val body = r.body.byteStream().use { readLimited(it, 4_000_000) }
            when (r.code) {
                200 -> Unit
                429, 401, 402, 403 -> throw SignLimit("http ${r.code}")
                else -> throw IllegalStateException("http ${r.code}")
            }
            val cookie = r.header("x-set-tt-cookie").orEmpty()
            if (cookie.isEmpty()) throw IllegalStateException("no cookie")
            val result = Proto.decodeFetchResult(gunzipIfNeeded(body))
            if (result.pushServer.isEmpty()) throw IllegalStateException("no push server")
            return Signed(result, cookie, r.header("x-room-id").orEmpty())
        }
    }

    private fun buildSocketUrl(result: Proto.FetchResult, room: String): String {
        val params = LinkedHashMap(DEFAULT_PARAMS)
        params["room_id"] = room
        params["cursor"] = result.cursor
        params["internal_ext"] = result.internalExt
        params["resp_content_type"] = "protobuf"
        result.routeParams.forEach { (k, v) -> if (v.isNotEmpty()) params[k] = v }
        val base = result.pushServer
        return base + (if (base.contains('?')) "&" else "?") + query(params)
    }

    // ------------------------------------------------------------------ estado

    private fun post(block: () -> Unit) {
        val gen = generation
        main.post { if (gen == generation) block() }
    }

    private fun setState(value: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { setState(value) }
            return
        }
        if (value == state) return
        state = value
        onState(value)
    }

    private fun publishDetail() {
        val d = buildString {
            append("WS ").append(if (socketOpen) "on" else "off")
            append(" · ").append(frames).append(" pkt · ").append(messages).append(" msg")
            if (roomId.isNotEmpty()) append(" | room ").append(roomId.takeLast(6))
            if (signInfo.isNotEmpty()) append(" | ").append(signInfo)
        }
        post {
            if (d != lastDetail) {
                lastDetail = d
                onDetail(d)
            }
        }
    }

    internal data class RoomInfo(val statusCode: Int, val status: Int, val viewers: Long, val avatar: String, val roomId: String = "") {
        /** 2 = ao vivo (status da sala no TikTok) */
        val live: Boolean get() = status == 2 || status < 0 && viewers >= 0
        /** o TikTok respondeu de verdade que a sala não está ao vivo (não é erro/limite da consulta) */
        val notLive: Boolean get() = statusCode == 0 && status >= 0 && status != 2
    }

    /** Leitura mínima de protobuf para as mensagens do chat (sem biblioteca). */
    internal object Proto {
        class Msg(val method: String, val payload: ByteArray)
        class FetchResult(
            val messages: List<Msg>,
            val cursor: String,
            val internalExt: String,
            val routeParams: Map<String, String>,
            val needsAck: Boolean,
            val pushServer: String
        )
        class PushFrame(val logId: Long, val payloadEncoding: String, val payloadType: String, val payload: ByteArray)

        private class Field(val number: Int, val wire: Int, val varint: Long, val bytes: ByteArray?)

        private fun fields(b: ByteArray): List<Field> {
            val out = ArrayList<Field>()
            var i = 0
            fun varint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    if (i >= b.size) throw IllegalArgumentException("truncated")
                    val c = b[i++].toInt() and 0xff
                    if (shift < 64) result = result or ((c and 0x7f).toLong() shl shift)
                    if (c and 0x80 == 0) return result
                    shift += 7
                }
            }
            while (i < b.size) {
                val key = varint()
                val number = (key ushr 3).toInt()
                when (val wire = (key and 7).toInt()) {
                    0 -> out += Field(number, 0, varint(), null)
                    1 -> i += 8
                    2 -> {
                        val len = varint().toInt()
                        if (len < 0 || i + len > b.size) throw IllegalArgumentException("bad length")
                        out += Field(number, 2, 0, b.copyOfRange(i, i + len))
                        i += len
                    }
                    5 -> i += 4
                    else -> throw IllegalArgumentException("wire $wire")
                }
            }
            return out
        }

        private fun List<Field>.bytes(n: Int) = firstOrNull { it.number == n && it.wire == 2 }?.bytes
        private fun List<Field>.str(n: Int) = bytes(n)?.toString(Charsets.UTF_8).orEmpty()
        private fun List<Field>.long(n: Int) = firstOrNull { it.number == n && it.wire == 0 }?.varint

        fun decodePushFrame(b: ByteArray): PushFrame {
            val f = fields(b)
            return PushFrame(f.long(2) ?: 0L, f.str(6), f.str(7), f.bytes(8) ?: ByteArray(0))
        }

        fun decodeFetchResult(b: ByteArray): FetchResult {
            val f = fields(b)
            val messages = f.filter { it.number == 1 && it.wire == 2 }.mapNotNull { m ->
                runCatching {
                    val mf = fields(m.bytes!!)
                    Msg(mf.str(1), mf.bytes(2) ?: ByteArray(0))
                }.getOrNull()
            }
            val route = LinkedHashMap<String, String>()
            f.filter { it.number == 7 && it.wire == 2 }.forEach { e ->
                runCatching {
                    val ef = fields(e.bytes!!)
                    route[ef.str(1)] = ef.str(2)
                }
            }
            return FetchResult(messages, f.str(2), f.str(5), route, (f.long(9) ?: 0L) != 0L, f.str(10))
        }

        /** (nome, texto) do WebcastChatMessage: user=2 {nickname=3, username=38}, content=3 */
        fun decodeChat(b: ByteArray): Pair<String, String>? {
            val f = fields(b)
            val text = f.str(3).trim()
            if (text.isEmpty()) return null
            val user = f.bytes(2)?.let { runCatching { fields(it) }.getOrNull() }
            val name = user?.str(3)?.trim()?.ifEmpty { null } ?: user?.str(38)?.trim().orEmpty()
            return name to text
        }

        /** espectadores: WebcastRoomUserSeqMessage.total (3) */
        fun decodeViewers(b: ByteArray): Long? = fields(b).long(3)?.takeIf { it >= 0 }

        /** WebcastControlMessage.action (2): 3 = live encerrada */
        fun decodeControlAction(b: ByteArray): Long? = fields(b).long(2)

        private fun ByteArrayOutputStream.varint(v: Long) {
            var x = v
            while (true) {
                if (x and 0x7fL.inv() == 0L) { write(x.toInt()); return }
                write(((x and 0x7f) or 0x80).toInt())
                x = x ushr 7
            }
        }

        private fun ByteArrayOutputStream.bytesField(n: Int, value: ByteArray) {
            varint((n.toLong() shl 3) or 2)
            varint(value.size.toLong())
            write(value)
        }

        /** PushFrame{logId=2, payloadEncoding=6 "pb", payloadType=7 "ack", payload=8 internalExt} */
        fun encodeAck(logId: Long, internalExt: ByteArray): ByteArray = ByteArrayOutputStream().apply {
            varint(2L shl 3)
            varint(logId)
            bytesField(6, "pb".toByteArray())
            bytesField(7, "ack".toByteArray())
            bytesField(8, internalExt)
        }.toByteArray()
    }

    companion object {
        private const val TAG = "TikTokLiveChat"
        private const val SIGN_URL = "https://tiktok.eulerstream.com/webcast/fetch"

        const val STATE_LOADING = "loading"
        const val STATE_WAITING = "waiting"
        const val STATE_OK = "ok"
        /** a live/sala não foi encontrada (live ainda não reconhecida pelo TikTok ou @ errado) */
        const val STATE_NOT_FOUND = "notfound"
        /** o TikTok encerrou a live (queda longa ou live fechada): a chave dessa live não vale mais */
        const val STATE_ENDED = "ended"
        /** o TikTok encerrou a live por conta própria (aviso/restrição) */
        const val STATE_SUSPENDED = "suspended"
        /** o TikTok pediu verificação (página aberta à mão) */
        const val STATE_BLOCKED = "blocked"
        /** limite do servidor de assinatura grátis (sem chave) */
        const val STATE_LIMIT = "limit"

        internal const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        /** "2\u0002pb:\u0002hb": PushFrame{payloadEncoding="pb", payloadType="hb"} */
        private val HEARTBEAT = byteArrayOf(0x32, 0x02, 0x70, 0x62, 0x3a, 0x02, 0x68, 0x62)

        private val DEFAULT_PARAMS: Map<String, String> = linkedMapOf(
            "aid" to "1988",
            "app_language" to "en-US",
            "app_name" to "tiktok_web",
            "browser_language" to "en",
            "browser_name" to "Mozilla",
            "browser_online" to "true",
            "browser_platform" to "Win32",
            "browser_version" to DESKTOP_UA.removePrefix("Mozilla/"),
            "cookie_enabled" to "true",
            "device_platform" to "web",
            "focus_state" to "true",
            "from_page" to "user",
            "history_len" to "4",
            "is_fullscreen" to "false",
            "is_page_visible" to "true",
            "did_rule" to "3",
            "fetch_rule" to "1",
            "identity" to "audience",
            "last_rtt" to "0",
            "live_id" to "12",
            "screen_height" to "1152",
            "screen_width" to "2048",
            "tz_name" to "America/Sao_Paulo",
            "referer" to "https://www.tiktok.com/",
            "root_referer" to "https://www.tiktok.com/",
            "version_code" to "180800",
            "webcast_sdk_version" to "1.3.0",
            "update_version_code" to "1.3.0"
        )

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

        private fun query(params: Map<String, String>) = params.entries.joinToString("&") { (k, v) -> enc(k) + "=" + enc(v) }

        private fun readLimited(input: java.io.InputStream, max: Int): ByteArray {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > max) throw IllegalStateException("too big")
            }
            return out.toByteArray()
        }

        internal fun gunzipIfNeeded(b: ByteArray): ByteArray {
            if (b.size < 3 || b[0] != 0x1f.toByte() || b[1] != 0x8b.toByte()) return b
            return GZIPInputStream(b.inputStream()).use { readLimited(it, 8_000_000) }
        }

        /**
         * Lê a resposta de api-live/user/room: status da sala (data.liveRoom.status / data.user.status),
         * id da sala (data.user.roomId), espectadores (userCount, procurado em todo o JSON) e foto.
         */
        internal fun parseRoomInfo(text: String): RoomInfo {
            val root = JSONObject(text)
            val statusCode = root.optInt("statusCode", root.optInt("status_code", 0))
            val data = root.optJSONObject("data")
            val liveRoom = data?.optJSONObject("liveRoom")
            val userObj = data?.optJSONObject("user")
            val status = liveRoom?.optInt("status", -1)?.takeIf { it >= 0 }
                ?: userObj?.optInt("status", -1) ?: -1
            val stats = liveRoom?.optJSONObject("liveRoomStats")
            val viewers = stats?.let { findCount(it, 0) } ?: data?.let { findCount(it, 0) } ?: -1L
            val avatar = userObj?.let { o ->
                listOf("avatarThumb", "avatarMedium", "avatarLarger").map { o.optString(it) }.firstOrNull { it.startsWith("https://") }
            }.orEmpty()
            val roomId = userObj?.optString("roomId").orEmpty().takeIf { it.isNotEmpty() && it != "0" }
                ?: liveRoom?.optString("roomId").orEmpty().takeIf { it.isNotEmpty() && it != "0" }
                ?: ""
            return RoomInfo(statusCode, status, viewers, avatar, roomId)
        }

        private val COUNT_KEYS = listOf("userCount", "user_count", "viewerCount", "viewer_count")

        private fun findCount(o: Any?, depth: Int): Long? {
            if (depth > 8) return null
            when (o) {
                is JSONObject -> {
                    for (k in COUNT_KEYS) {
                        if (o.has(k)) {
                            val v = o.optLong(k, -1L)
                            if (v >= 0) return v
                        }
                    }
                    val keys = o.keys()
                    while (keys.hasNext()) findCount(o.opt(keys.next()), depth + 1)?.let { return it }
                }
                is JSONArray -> for (i in 0 until o.length()) findCount(o.opt(i), depth + 1)?.let { return it }
            }
            return null
        }

        /** Vídeo/áudio da live (bloqueado na página do TikTok aberta à mão). */
        internal fun isMediaStream(url: String): Boolean {
            val u = url.lowercase()
            val path = u.substringBefore('?')
            return (u.contains("pull-") || u.contains("/stage/stream")) &&
                (path.endsWith(".flv") || path.endsWith(".m3u8") || path.endsWith(".ts") || path.contains(".flv")) ||
                path.endsWith(".mp4") && u.contains("tiktokcdn")
        }
    }
}
