package com.russoviller.cryogenic.live

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.russoviller.cryogenic.utils.logging.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Lê o chat da live do TikTok pela página web da live (www.tiktok.com/@usuario/live), num WebView
 * fora da tela. O TikTok não tem API pública de chat, então são usados dois caminhos:
 *
 * 1. **Conexão do chat da própria página** (principal): um script que roda antes da página
 *    observa o WebSocket/fetch do chat da live (webcast) e decodifica as mensagens (protobuf).
 * 2. **Página desenhada** (reserva): observa os itens de chat que a página mostra.
 *
 * O WebView fora da tela é tratado como "aba escondida" pelo navegador; o script faz a página se
 * comportar como visível para ela continuar atualizando o chat. O vídeo da live é bloqueado
 * (não gasta internet e o som da página não vaza para a transmissão).
 *
 * Só roda na thread principal.
 */
class TikTokChatReader(
    private val context: Context,
    private val onMessage: (user: String, text: String) -> Unit,
    private val onState: (String) -> Unit,
    /** número de espectadores como a página mostra ("128", "1,2 mil", "1.2K"); vazio = não encontrado */
    private val onViewers: (String) -> Unit = {},
    /** link da foto de perfil de quem está ao vivo */
    private val onAvatar: (String) -> Unit = {},
    /** linha curta de diagnóstico (conexão do chat, mensagens recebidas, página) */
    private val onDetail: (String) -> Unit = {}
) {
    private var webView: WebView? = null
    private var user: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var lastMessageAt = 0L
    private var started = 0L
    private var pageFinishedAt = 0L

    // diagnóstico
    private var socketOpen = false
    private var frames = 0
    private var wsMessages = 0
    private var pageMessages = 0
    private var pageChatFound = false
    private var blocked = false
    private var pageTitle = ""
    private var lastDetail = ""
    private var state = ""
    /** resultado da consulta da sala (espectadores) para o diagnóstico */
    private var apiInfo = ""
    private var poller: ScheduledExecutorService? = null

    private val watchdog = object : Runnable {
        override fun run() {
            val view = webView ?: return
            view.evaluateJavascript(INJECT, null)
            val now = System.currentTimeMillis()
            val connected = socketOpen || pageChatFound
            when {
                lastMessageAt != 0L -> Unit
                blocked -> setState(STATE_BLOCKED)
                connected -> setState(STATE_WAITING)
                pageFinishedAt != 0L && now - pageFinishedAt > 25_000L -> setState(STATE_NOT_FOUND)
            }
            // sem chat por muito tempo: recarrega a página (a live pode ter começado depois)
            val reloadAfter = if (connected) 5 * 60_000L else 60_000L
            if (lastMessageAt == 0L && now - started > reloadAfter || lastMessageAt != 0L && now - lastMessageAt > 10 * 60_000L) {
                started = now
                lastMessageAt = 0L
                load()
            }
            publishDetail()
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    val running: Boolean get() = webView != null

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "RequiresFeature")
    fun start(tiktokUser: String) {
        val normalized = LiveConfig.normalizeUser(tiktokUser)
        if (normalized.isEmpty()) {
            stop()
            onState("")
            return
        }
        if (webView != null && normalized == user) return
        stop()
        user = normalized
        val view = WebView(context)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
            loadsImagesAutomatically = false
            blockNetworkImage = true
            userAgentString = DESKTOP_UA
        }
        // a página do TikTok roda com prioridade baixa: o sistema dá CPU para o jogo primeiro
        runCatching { view.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_WAIVED, true) }
        CookieManager.getInstance().setAcceptCookie(true)
        runCatching { CookieManager.getInstance().setAcceptThirdPartyCookies(view, true) }
        view.addJavascriptInterface(Bridge(), "CryoChat")
        // o script do chat precisa rodar antes da página abrir a conexão do chat
        val earlyScript = runCatching {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(view, EARLY, setOf("https://*.tiktok.com"))
                true
            } else false
        }.getOrDefault(false)
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                // bloqueia o vídeo/áudio da live, imagens e fontes da página (só o chat interessa)
                if (isMediaStream(url) || isHeavyResource(url)) {
                    return WebResourceResponse("text/plain", "utf-8", 204, "No Content", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                }
                return null
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                pageFinishedAt = 0L
                if (!earlyScript) view?.evaluateJavascript(EARLY, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                pageFinishedAt = System.currentTimeMillis()
                view?.evaluateJavascript(INJECT, null)
            }

            // o processo da página pode ser fechado pelo sistema (pouca memória / prioridade baixa):
            // sem isto o app inteiro fecharia. Abre a página de novo depois de alguns segundos.
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                Logger.warning(TAG, "Chat page renderer gone (crash=${detail?.didCrash()})")
                val u = user
                stop()
                handler.postDelayed({ if (webView == null && u.isNotEmpty()) start(u) }, 10_000L)
                return true
            }
        }
        // o WebView precisa de tamanho para a página montar o layout de computador com o chat
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1280, 800)
        attachHidden(view)
        view.onResume()
        view.resumeTimers()
        webView = view
        started = System.currentTimeMillis()
        lastMessageAt = 0L
        state = ""
        setState(STATE_LOADING)
        load()
        handler.postDelayed(watchdog, WATCHDOG_MS)
        startPoller(normalized)
    }

    /**
     * Espectadores pela consulta da sala do TikTok (api-live/user/room), fora do WebView: não
     * depende da página nem da conexão do chat. A cada 20 s, numa thread de prioridade mínima.
     */
    private fun startPoller(u: String) {
        poller?.shutdownNow()
        apiInfo = ""
        poller = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "TikTokRoom").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
        }.also { ex ->
            ex.scheduleWithFixedDelay({
                val result = runCatching { fetchRoom(u) }
                handler.post {
                    if (u != user) return@post
                    result.onSuccess { info ->
                        apiInfo = when {
                            info.statusCode != 0 -> "API code ${info.statusCode}"
                            info.live && info.viewers >= 0 -> "API ${info.viewers}"
                            info.status >= 0 -> "API status ${info.status}"
                            else -> "API ?"
                        }
                        if (info.live && info.viewers >= 0) onViewers(LiveFloatingWindow.formatCount(info.viewers.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()))
                        if (info.avatar.startsWith("https://")) onAvatar(info.avatar)
                    }.onFailure { e ->
                        apiInfo = "API " + (e.message ?: e.javaClass.simpleName).take(24)
                    }
                    publishDetail()
                }
            }, 2L, 20L, TimeUnit.SECONDS)
        }
    }

    private fun fetchRoom(u: String): RoomInfo {
        val url = "https://www.tiktok.com/api-live/user/room/?aid=1988&app_name=tiktok_web&device_platform=web_pc" +
            "&sourceType=54&uniqueId=" + URLEncoder.encode(u, "UTF-8")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", DESKTOP_UA)
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Referer", "https://www.tiktok.com/@$u/live")
            runCatching { CookieManager.getInstance().getCookie("https://www.tiktok.com") }.getOrNull()
                ?.takeIf { it.isNotBlank() }?.let { conn.setRequestProperty("Cookie", it) }
            val code = conn.responseCode
            if (code != 200) throw IllegalStateException("http $code")
            val bytes = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > 2_000_000) throw IllegalStateException("too big")
                }
                out.toByteArray()
            }
            return parseRoomInfo(String(bytes, Charsets.UTF_8))
        } finally {
            conn.disconnect()
        }
    }

    private fun load() {
        socketOpen = false
        pageChatFound = false
        blocked = false
        pageFinishedAt = 0L
        webView?.loadUrl("https://www.tiktok.com/@$user/live")
    }

    private fun setState(value: String) {
        if (value == state) return
        state = value
        onState(value)
    }

    private fun publishDetail() {
        val d = detailText(socketOpen, frames, wsMessages, pageChatFound, pageMessages, pageTitle) +
            (if (apiInfo.isNotEmpty()) " | $apiInfo" else "") +
            (if (hiddenWindow != null) " | win" else " | nowin")
        if (d != lastDetail) {
            lastDetail = d
            onDetail(d)
        }
    }

    /**
     * Fora de uma janela, o navegador trata a página como aba escondida e não desenha nada: várias
     * partes da página do TikTok (lista do chat, conexão) só andam quando ela desenha. Com a permissão
     * de sobreposição, a página fica numa janela invisível de 1 pixel (sem toque, sem foco): para o
     * navegador ela está visível, mas só 1 pixel é desenhado.
     */
    private fun attachHidden(view: WebView) {
        if (!runCatching { android.provider.Settings.canDrawOverlays(context) }.getOrDefault(false)) return
        runCatching {
            val wm = context.getSystemService(android.view.WindowManager::class.java) ?: return
            val box = android.widget.FrameLayout(context)
            box.addView(view, android.widget.FrameLayout.LayoutParams(1280, 800))
            val lp = android.view.WindowManager.LayoutParams(
                1, 1,
                android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                x = 0
                y = 0
                alpha = 0f
                title = "CryoChatReader"
            }
            wm.addView(box, lp)
            hiddenWindow = box
        }.onFailure {
            Logger.warning(TAG, "Chat page window failed: ${it.message}")
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            hiddenWindow = null
        }
    }

    private var hiddenWindow: android.widget.FrameLayout? = null

    private fun detachHidden() {
        val box = hiddenWindow ?: return
        hiddenWindow = null
        runCatching { context.getSystemService(android.view.WindowManager::class.java)?.removeViewImmediate(box) }
        runCatching { box.removeAllViews() }
    }

    fun stop() {
        handler.removeCallbacks(watchdog)
        detachHidden()
        poller?.shutdownNow()
        poller = null
        apiInfo = ""
        webView?.let { view ->
            runCatching {
                view.stopLoading()
                view.removeJavascriptInterface("CryoChat")
                view.destroy()
            }
        }
        webView = null
        user = ""
        frames = 0
        wsMessages = 0
        pageMessages = 0
        lastDetail = ""
    }

    private inner class Bridge {
        /** source: "ws" (conexão do chat) ou "dom" (página desenhada) */
        @JavascriptInterface
        fun onMessage(user: String?, text: String?, source: String?) {
            val t = text?.trim().orEmpty()
            if (t.isEmpty()) return
            handler.post {
                if (source == "ws") wsMessages++ else pageMessages++
                lastMessageAt = System.currentTimeMillis()
                setState(STATE_OK)
                onMessage(user?.trim().orEmpty().take(40), t.take(300))
            }
        }

        @JavascriptInterface
        fun onViewers(text: String?) {
            val raw = text?.trim().orEmpty().take(16)
            // número puro (conexão do chat): mesmo formato do TikTok (1,2K)
            val v = raw.toIntOrNull()?.let { LiveFloatingWindow.formatCount(it) } ?: raw
            if (v.isEmpty()) return
            handler.post { onViewers(v) }
        }

        @JavascriptInterface
        fun onAvatar(url: String?) {
            val u = url?.trim().orEmpty()
            if (u.startsWith("https://")) handler.post { onAvatar(u) }
        }

        @JavascriptInterface
        fun onReady(found: Boolean) {
            handler.post { if (found) pageChatFound = true }
        }

        /** kind: "ws" (conexão do chat aberta), "frame" (pacote recebido), "blocked" (verificação), "title" */
        @JavascriptInterface
        fun onDiag(kind: String?, info: String?) {
            handler.post {
                when (kind) {
                    "ws" -> socketOpen = true
                    "frame" -> frames++
                    "blocked" -> blocked = info == "1"
                    "title" -> pageTitle = info?.trim().orEmpty().take(40)
                }
            }
        }
    }

    internal data class RoomInfo(val statusCode: Int, val status: Int, val viewers: Long, val avatar: String) {
        /** 2 = ao vivo (status da sala no TikTok) */
        val live: Boolean get() = status == 2 || status < 0 && viewers >= 0
    }

    companion object {
        private const val TAG = "TikTokChat"
        private const val WATCHDOG_MS = 10_000L
        const val STATE_LOADING = "loading"
        const val STATE_WAITING = "waiting"
        const val STATE_OK = "ok"
        /** a página abriu, mas a live/chat não apareceu (live ainda não reconhecida ou @ errado) */
        const val STATE_NOT_FOUND = "notfound"
        /** o TikTok pediu verificação (captcha) */
        const val STATE_BLOCKED = "blocked"

        internal const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        internal fun detailText(socket: Boolean, frames: Int, wsMessages: Int, pageChat: Boolean, pageMessages: Int, title: String): String =
            buildString {
                append("WS ").append(if (socket) "on" else "off")
                append(" · ").append(frames).append(" pkt · ").append(wsMessages).append(" msg")
                append(" | DOM ").append(if (pageChat) "on" else "off").append(" · ").append(pageMessages).append(" msg")
                if (title.isNotEmpty()) append(" | ").append(title)
            }

        /** Imagens, fontes e vídeos da página: não são necessários para ler o chat. */
        internal fun isHeavyResource(url: String): Boolean {
            val path = url.lowercase().substringBefore('?').substringBefore('#')
            return HEAVY_EXT.any { path.endsWith(it) }
        }

        private val HEAVY_EXT = listOf(
            ".png", ".jpg", ".jpeg", ".webp", ".gif", ".avif", ".heic", ".image", ".svg",
            ".woff", ".woff2", ".ttf", ".otf", ".mp4", ".webm", ".mp3", ".m4a", ".aac", ".flv", ".m3u8"
        )

        /**
         * Lê a resposta de api-live/user/room: status da sala (data.liveRoom.status / data.user.status),
         * espectadores (userCount, procurado em todo o JSON) e foto (data.user.avatarThumb).
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
            return RoomInfo(statusCode, status, viewers, avatar)
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

        internal fun isMediaStream(url: String): Boolean {
            val u = url.lowercase()
            val path = u.substringBefore('?')
            return (u.contains("pull-") || u.contains("/stage/stream")) &&
                (path.endsWith(".flv") || path.endsWith(".m3u8") || path.endsWith(".ts") || path.contains(".flv")) ||
                path.endsWith(".mp4") && u.contains("tiktokcdn")
        }

        /**
         * Roda antes da página (document start):
         * - página sempre "visível" para o JS (o WebView fora da tela conta como aba escondida);
         * - observa o WebSocket e o fetch/XHR do chat (webcast) e decodifica o protobuf:
         *   PushFrame{payloadEncoding=6, payloadType=7, payload=8} -> Response{messages=1}
         *   -> Message{method=1, payload=2, msgId=3}
         *   -> WebcastChatMessage{user=2{nickname=3, uniqueId=38}, content=3}
         *   -> WebcastRoomUserSeqMessage{total=3}.
         * Idempotente.
         */
        internal val EARLY = """
            (function(){
              if (window.__cryoEarly) return; window.__cryoEarly = true;
              function diag(k, v){ try { CryoChat.onDiag(k, v == null ? '' : String(v)); } catch(e){} }
              // --- sem animações (a página agora desenha: economiza GPU/CPU do jogo)
              (function addCss(){
                try {
                  var root = document.head || document.documentElement;
                  if (!root) { setTimeout(addCss, 50); return; }
                  var css = document.createElement('style');
                  css.textContent = '*,*::before,*::after{animation:none!important;transition:none!important}video,img{display:none!important}';
                  root.appendChild(css);
                } catch (e) {}
              })();
              // --- página sempre visível
              try {
                Object.defineProperty(Document.prototype, 'hidden', { configurable: true, get: function(){ return false; } });
                Object.defineProperty(Document.prototype, 'visibilityState', { configurable: true, get: function(){ return 'visible'; } });
                Object.defineProperty(Document.prototype, 'webkitHidden', { configurable: true, get: function(){ return false; } });
                Document.prototype.hasFocus = function(){ return true; };
                document.addEventListener('visibilitychange', function(e){ e.stopImmediatePropagation(); }, true);
                window.addEventListener('blur', function(e){ e.stopImmediatePropagation(); }, true);
              } catch (e) {}
              // --- protobuf
              var dec = new TextDecoder('utf-8');
              function fields(b){
                var out = [], i = 0, n = b.length;
                while (i < n) {
                  var key = 0, shift = 0, c;
                  do { if (i >= n) return out; c = b[i++]; key += (c & 0x7f) * Math.pow(2, shift); shift += 7; } while (c & 0x80);
                  var f = Math.floor(key / 8), w = key % 8;
                  if (w === 0) {
                    var v = 0; shift = 0; var start = i;
                    do { if (i >= n) return out; c = b[i++]; if (shift < 53) v += (c & 0x7f) * Math.pow(2, shift); shift += 7; } while (c & 0x80);
                    out.push({ f: f, w: 0, v: v, raw: b.subarray(start, i) });
                  } else if (w === 2) {
                    var len = 0; shift = 0;
                    do { if (i >= n) return out; c = b[i++]; len += (c & 0x7f) * Math.pow(2, shift); shift += 7; } while (c & 0x80);
                    if (i + len > n) return out;
                    out.push({ f: f, w: 2, b: b.subarray(i, i + len) });
                    i += len;
                  } else if (w === 1) { i += 8; }
                  else if (w === 5) { i += 4; }
                  else return out;
                }
                return out;
              }
              function str(x){ try { return dec.decode(x); } catch(e){ return ''; } }
              function first(fs, f){ for (var i = 0; i < fs.length; i++) if (fs[i].f === f) return fs[i]; return null; }
              var seenIds = {}, seenCount = 0;
              function chat(payload, idKey){
                var fs = fields(payload);
                var content = first(fs, 3), userF = first(fs, 2);
                if (!content || content.w !== 2) return;
                var text = str(content.b).trim();
                if (!text) return;
                var name = '';
                if (userF && userF.w === 2) {
                  var us = fields(userF.b);
                  var nick = first(us, 3), uid = first(us, 38);
                  name = (nick && nick.w === 2 ? str(nick.b) : '') || (uid && uid.w === 2 ? str(uid.b) : '');
                }
                if (idKey) {
                  if (seenIds[idKey]) return;
                  seenIds[idKey] = 1;
                  if (++seenCount > 2000) { seenIds = {}; seenCount = 0; }
                }
                window.__cryoWsChat = true;
                CryoChat.onMessage(name, text, 'ws');
              }
              function viewers(payload){
                var t = first(fields(payload), 3);
                if (t && t.w === 0 && t.v > 0) { window.__cryoWsViewers = true; CryoChat.onViewers(String(t.v)); }
              }
              function response(b){
                var fs = fields(b);
                for (var i = 0; i < fs.length; i++) {
                  if (fs[i].f !== 1 || fs[i].w !== 2) continue;
                  var m = fields(fs[i].b);
                  var method = first(m, 1), payload = first(m, 2), id = first(m, 3);
                  if (!method || method.w !== 2 || !payload || payload.w !== 2) continue;
                  var name = str(method.b);
                  var key = id && id.raw ? Array.prototype.join.call(id.raw, ',') : '';
                  if (name === 'WebcastChatMessage') chat(payload.b, key);
                  else if (name === 'WebcastRoomUserSeqMessage') viewers(payload.b);
                }
              }
              function gunzip(b, done){
                try {
                  var ds = new DecompressionStream('gzip');
                  new Response(new Blob([b]).stream().pipeThrough(ds)).arrayBuffer()
                    .then(function(x){ done(new Uint8Array(x)); }).catch(function(){});
                } catch (e) {}
              }
              function isGzip(b){ return b.length > 2 && b[0] === 0x1f && b[1] === 0x8b; }
              function frame(b){
                diag('frame');
                var fs = fields(b);
                var type = first(fs, 7), enc = first(fs, 6), payload = first(fs, 8);
                var t = type && type.w === 2 ? str(type.b) : '';
                if (t && t !== 'msg') return;
                if (!payload || payload.w !== 2) return;
                var p = payload.b;
                var e = enc && enc.w === 2 ? str(enc.b) : '';
                if (e === 'gzip' || isGzip(p)) gunzip(p, response); else response(p);
              }
              function bytes(data, done){
                try {
                  if (data instanceof ArrayBuffer) done(new Uint8Array(data));
                  else if (ArrayBuffer.isView(data)) done(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));
                  else if (typeof Blob !== 'undefined' && data instanceof Blob) data.arrayBuffer().then(function(x){ done(new Uint8Array(x)); }).catch(function(){});
                } catch (e) {}
              }
              function isChatUrl(u){ return /webcast|\/im\/|ws_proxy/i.test(String(u || '')); }

              // --- WebSocket do chat
              try {
                var NativeWS = window.WebSocket;
                var Hooked = function(url, protocols){
                  var ws = arguments.length > 1 ? new NativeWS(url, protocols) : new NativeWS(url);
                  try {
                    if (isChatUrl(url)) {
                      ws.addEventListener('open', function(){ diag('ws'); });
                      ws.addEventListener('message', function(ev){ bytes(ev.data, function(b){ try { frame(b); } catch(e){} }); });
                    }
                  } catch (e) {}
                  return ws;
                };
                Hooked.prototype = NativeWS.prototype;
                Hooked.CONNECTING = 0; Hooked.OPEN = 1; Hooked.CLOSING = 2; Hooked.CLOSED = 3;
                window.WebSocket = Hooked;
              } catch (e) {}

              // --- busca inicial do chat (fetch / XHR em /webcast/im/fetch)
              function fetched(b){ try { if (isGzip(b)) gunzip(b, response); else response(b); } catch(e){} }
              try {
                var nf = window.fetch;
                if (nf) window.fetch = function(input){
                  var p = nf.apply(this, arguments);
                  try {
                    var u = typeof input === 'string' ? input : (input && input.url) || '';
                    if (/\/webcast\/im\/fetch/i.test(u)) p.then(function(r){ r.clone().arrayBuffer().then(function(x){ diag('ws'); fetched(new Uint8Array(x)); }).catch(function(){}); }).catch(function(){});
                  } catch (e) {}
                  return p;
                };
              } catch (e) {}
              try {
                var xo = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function(m, u){
                  try {
                    if (/\/webcast\/im\/fetch/i.test(String(u))) this.addEventListener('load', function(){
                      try { var r = this.response; if (r instanceof ArrayBuffer) { diag('ws'); fetched(new Uint8Array(r)); } } catch(e){}
                    });
                  } catch (e) {}
                  return xo.apply(this, arguments);
                };
              } catch (e) {}
            })();
        """.trimIndent()

        /**
         * Reserva pela página desenhada + espectadores/foto/diagnóstico. Usa os atributos data-e2e
         * da página (chat-message / message-owner-name). Idempotente: pode ser injetado várias vezes.
         */
        internal val INJECT = """
            (function(){
              try {
                document.querySelectorAll('video,audio').forEach(function(v){ try { v.muted = true; v.pause(); } catch(e){} });
                try { CryoChat.onDiag('title', document.title || ''); } catch (e) {}
                // verificação (captcha) na frente da página
                try {
                  var caps = document.querySelectorAll('#captcha-verify-container,#captcha_container,[id*="captcha" i],[class*="captcha" i],iframe[src*="captcha" i]');
                  var shown = false;
                  for (var ci = 0; ci < caps.length && !shown; ci++) { var r = caps[ci].getBoundingClientRect(); shown = r.width > 40 && r.height > 40; }
                  CryoChat.onDiag('blocked', shown ? '1' : '0');
                } catch (e) {}
                // espectadores: elementos de contagem da sala (o número que a página mostra)
                try {
                  if (!window.__cryoWsViewers) {
                    var cand = document.querySelectorAll('[data-e2e*="people" i],[data-e2e*="viewer" i],[data-e2e*="user-count" i],[data-e2e*="usercount" i],[class*="UserCount" i],[class*="ViewerCount" i],[class*="PeopleCount" i],[class*="viewer-count" i]');
                    var vtxt = '';
                    for (var i = 0; i < cand.length && !vtxt; i++) {
                      var t = ((cand[i].innerText || cand[i].textContent) || '').replace(/\s+/g,' ').trim();
                      var m = t.match(/^(\d[\d.,]*\s*(?:[kKmM]|mil|mi)?)/);
                      if (m && t.length <= 24) vtxt = m[1].trim();
                    }
                    if (vtxt) CryoChat.onViewers(vtxt);
                  }
                } catch (e) {}
                // foto de perfil de quem está ao vivo
                try {
                  var av = document.querySelector('[data-e2e*="avatar" i] img, img[class*="Avatar" i], img[src*="avt-"]');
                  if (av && av.src) CryoChat.onAvatar(av.src);
                } catch (e) {}
                if (window.__cryoWsChat) return;
                var SEL = '[data-e2e="chat-message"],[data-e2e*="chat-message" i],[class*="DivChatMessage" i],[class*="ChatMessageItem" i]';
                if (window.__cryoChat) { CryoChat.onReady(!!document.querySelector(SEL)); return; }
                window.__cryoChat = true;
                var seen = new WeakSet();
                function txt(e){ return ((e && (e.innerText || e.textContent)) || '').replace(/\s+/g,' ').trim(); }
                function emit(it){
                  if (seen.has(it)) return; seen.add(it);
                  // a conexão do chat já entrega as mensagens: a página é só reserva
                  if (window.__cryoWsChat) return;
                  var nameEl = it.querySelector('[data-e2e="message-owner-name"],[data-e2e*="owner-name" i],[class*="OwnerName" i],[class*="Nickname" i]');
                  var user = txt(nameEl);
                  var all = txt(it);
                  var text = all;
                  if (user && all.indexOf(user) === 0) text = all.substring(user.length);
                  text = text.replace(/^[\s:：·]+/, '').trim();
                  if (text) CryoChat.onMessage(user, text, 'dom');
                }
                function scan(node){
                  if (!(node instanceof Element)) return;
                  if (node.matches(SEL)) emit(node);
                  node.querySelectorAll(SEL).forEach(emit);
                }
                // o histórico que já estava na tela não é repetido
                document.querySelectorAll(SEL).forEach(function(n){ seen.add(n); });
                var obs = new MutationObserver(function(ms){
                  // a conexão do chat já entrega as mensagens: para de observar a página (economiza CPU)
                  if (window.__cryoWsChat) { obs.disconnect(); return; }
                  ms.forEach(function(m){ m.addedNodes.forEach(scan); });
                });
                obs.observe(document.body, { childList: true, subtree: true });
                CryoChat.onReady(!!document.querySelector(SEL));
              } catch (e) {}
            })();
        """.trimIndent()
    }
}
