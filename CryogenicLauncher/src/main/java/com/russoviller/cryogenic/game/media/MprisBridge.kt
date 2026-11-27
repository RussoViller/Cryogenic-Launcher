package com.russoviller.cryogenic.game.media

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Process
import com.russoviller.cryogenic.utils.logging.Logger
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Barramento D-Bus mínimo dentro do processo do jogo, com um player MPRIS que espelha a
 * música que está tocando no celular.
 *
 * Mods de PC que leem a música atual (ex.: Show the Lyrics) usam, no Linux, o barramento de
 * sessão D-Bus e os players MPRIS. No Android esse barramento não existe, então eles nunca
 * encontravam o Spotify/YouTube. O launcher sobe este barramento num socket abstrato e passa
 * o endereço ao jogo em DBUS_SESSION_BUS_ADDRESS; só aceita conexões do próprio app.
 */
object MprisBridge {
    private const val TAG = "MprisBridge"
    private const val BUS = "org.freedesktop.DBus"
    private const val BUS_PATH = "/org/freedesktop/DBus"
    private const val PLAYER_NAME = "org.mpris.MediaPlayer2.cryogenic"
    private const val PLAYER_OWNER = ":1.0"
    private const val PLAYER_PATH = "/org/mpris/MediaPlayer2"
    private const val IFACE_ROOT = "org.mpris.MediaPlayer2"
    private const val IFACE_PLAYER = "org.mpris.MediaPlayer2.Player"
    private const val IFACE_PROPS = "org.freedesktop.DBus.Properties"
    private const val IFACE_PEER = "org.freedesktop.DBus.Peer"
    private const val IFACE_INTROSPECT = "org.freedesktop.DBus.Introspectable"

    private val guid = UUID.randomUUID().toString().replace("-", "")
    private val nextClient = AtomicInteger(1)

    @Volatile
    private var server: LocalServerSocket? = null

    @Volatile
    private var address: String? = null
    private lateinit var appContext: Context

    @Volatile
    private var cached: Pair<Long, PhoneTrack?>? = null

    /** Inicia o barramento (uma vez por processo) e devolve o endereço para DBUS_SESSION_BUS_ADDRESS. */
    @Synchronized
    fun start(context: Context): String? {
        address?.let { return it }
        appContext = context.applicationContext
        val name = "cryogenic-dbus-${Process.myPid()}"
        val socket = try {
            LocalServerSocket(name)
        } catch (e: IOException) {
            Logger.warning(TAG, "Cannot open media bus: ${e.message}")
            return null
        }
        server = socket
        thread(name = "Cryo-MediaBus", isDaemon = true) {
            while (true) {
                val client = try {
                    socket.accept()
                } catch (e: IOException) {
                    break
                }
                val uid = runCatching { client.peerCredentials.uid }.getOrDefault(-1)
                if (uid != Process.myUid()) {
                    runCatching { client.close() }
                    continue
                }
                thread(name = "Cryo-MediaBus-Client", isDaemon = true) { serve(client) }
            }
        }
        Logger.info(TAG, "Media bus ready (notification access: ${PhoneMedia.hasAccess(appContext)})")
        return "unix:abstract=$name".also { address = it }
    }

    private fun track(): PhoneTrack? {
        trackOverride?.let { return it() }
        val now = System.currentTimeMillis()
        cached?.let { (time, value) -> if (now - time < 250) return value }
        val value = PhoneMedia.current(appContext)
        cached = now to value
        return value
    }

    // ---------------------------------------------------------------- conexão

    private fun serve(socket: LocalSocket) {
        try {
            socket.use { serveStreams(BufferedInputStream(it.inputStream), it.outputStream) }
        } catch (e: IOException) {
            //cliente fechou
        } catch (e: Exception) {
            Logger.warning(TAG, "Media bus client error: ${e.message}")
        }
    }

    /** Atende um cliente até ele fechar a conexão. */
    internal fun serveStreams(input: InputStream, output: OutputStream) {
        val unique = ":1.${nextClient.getAndIncrement()}"
        if (!authenticate(input, output)) return
        var serial = 1
        while (true) {
            val message = readMessage(input) ?: return
            if (message.type != 1) continue
            val reply = handle(message, unique) ?: continue
            if (message.flags and 1 != 0) continue
            val bytes = when (reply) {
                is Reply.Ok -> encode(2, serial++, message.serial, unique, reply.signature, reply.values)
                is Reply.Error -> encode(
                    3, serial++, message.serial, unique, "s", listOf(Dv.Str(reply.message)), reply.name
                )
            }
            output.write(bytes)
            output.flush()
        }
    }

    /** Só para testes: substitui a leitura das sessões de mídia do Android. */
    @Volatile
    internal var trackOverride: (() -> PhoneTrack?)? = null

    /** Autenticação em texto do D-Bus: aceita EXTERNAL/ANONYMOUS do próprio app. */
    private fun authenticate(input: InputStream, output: OutputStream): Boolean {
        if (input.read() != 0) return false
        while (true) {
            val line = readLine(input) ?: return false
            val answer = when {
                line == "AUTH" -> "REJECTED EXTERNAL ANONYMOUS"
                line.startsWith("AUTH EXTERNAL") || line.startsWith("AUTH ANONYMOUS") -> "OK $guid"
                line.startsWith("AUTH") -> "REJECTED EXTERNAL ANONYMOUS"
                line.startsWith("DATA") -> "OK $guid"
                line.startsWith("NEGOTIATE_UNIX_FD") -> "ERROR"
                line == "BEGIN" -> return true
                else -> "ERROR"
            }
            output.write("$answer\r\n".toByteArray(Charsets.US_ASCII))
            output.flush()
        }
    }

    private fun readLine(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return null
            if (b == '\n'.code) break
            if (b != '\r'.code) buffer.write(b)
            if (buffer.size() > 4096) return null
        }
        return buffer.toString(Charsets.US_ASCII.name())
    }

    // ---------------------------------------------------------------- métodos

    private sealed interface Reply {
        class Ok(val signature: String = "", val values: List<Dv> = emptyList()) : Reply
        class Error(val name: String, val message: String) : Reply
    }

    private fun handle(message: Message, unique: String): Reply? {
        val destination = message.destination
        return when {
            destination == BUS || (destination == null && message.path == BUS_PATH) -> handleBus(message, unique)
            destination == PLAYER_NAME || destination == PLAYER_OWNER -> handlePlayer(message)
            message.iface == IFACE_PEER && message.member == "Ping" -> Reply.Ok()
            else -> Reply.Error(
                "org.freedesktop.DBus.Error.ServiceUnknown",
                "The name ${destination ?: ""} was not provided by any .service files"
            )
        }
    }

    private fun playerOnline() = track() != null

    private fun handleBus(message: Message, unique: String): Reply {
        val arg = message.args.firstOrNull() as? String
        return when (message.member) {
            "Hello" -> Reply.Ok("s", listOf(Dv.Str(unique)))
            "ListNames" -> {
                val names = mutableListOf(BUS, unique)
                if (playerOnline()) names += listOf(PLAYER_OWNER, PLAYER_NAME)
                Reply.Ok("as", listOf(Dv.StrArray(names)))
            }
            "ListActivatableNames" -> Reply.Ok("as", listOf(Dv.StrArray(listOf(BUS))))
            "NameHasOwner" -> Reply.Ok("b", listOf(Dv.Bool(ownerOf(arg, unique) != null)))
            "GetNameOwner" -> ownerOf(arg, unique)?.let { Reply.Ok("s", listOf(Dv.Str(it))) }
                ?: Reply.Error("org.freedesktop.DBus.Error.NameHasNoOwner", "Could not get owner of name '$arg'")
            "AddMatch", "RemoveMatch" -> Reply.Ok()
            "GetId" -> Reply.Ok("s", listOf(Dv.Str(guid)))
            "RequestName" -> Reply.Ok("u", listOf(Dv.U32(1)))
            "ReleaseName" -> Reply.Ok("u", listOf(Dv.U32(1)))
            "GetConnectionUnixUser" -> Reply.Ok("u", listOf(Dv.U32(Process.myUid().toLong())))
            "GetConnectionUnixProcessID" -> Reply.Ok("u", listOf(Dv.U32(Process.myPid().toLong())))
            "Ping" -> Reply.Ok()
            else -> unknownMethod(message)
        }
    }

    private fun ownerOf(name: String?, unique: String): String? = when (name) {
        BUS -> BUS
        unique -> unique
        PLAYER_NAME, PLAYER_OWNER -> if (playerOnline()) PLAYER_OWNER else null
        else -> null
    }

    private fun handlePlayer(message: Message): Reply {
        val current = track() ?: return Reply.Error(
            "org.freedesktop.DBus.Error.ServiceUnknown",
            "The name $PLAYER_NAME was not provided by any .service files"
        )
        if (message.path != PLAYER_PATH && message.iface != IFACE_PEER) {
            return Reply.Error("org.freedesktop.DBus.Error.UnknownObject", "No such object path '${message.path}'")
        }
        return when (message.iface) {
            IFACE_PROPS -> when (message.member) {
                "GetAll" -> {
                    val props = properties(message.args.firstOrNull() as? String, current)
                    Reply.Ok("a{sv}", listOf(Dv.Dict(props)))
                }
                "Get" -> {
                    val iface = message.args.getOrNull(0) as? String
                    val name = message.args.getOrNull(1) as? String
                    properties(iface, current)[name]?.let { Reply.Ok("v", listOf(Dv.Variant(it))) }
                        ?: Reply.Error("org.freedesktop.DBus.Error.InvalidArgs", "No such property '$name'")
                }
                "Set" -> Reply.Error("org.freedesktop.DBus.Error.PropertyReadOnly", "Property is read-only")
                else -> unknownMethod(message)
            }
            IFACE_PLAYER, null -> control(message)
            IFACE_ROOT -> Reply.Ok()
            IFACE_PEER -> Reply.Ok()
            IFACE_INTROSPECT -> Reply.Ok("s", listOf(Dv.Str(INTROSPECTION)))
            else -> unknownMethod(message)
        }
    }

    private fun unknownMethod(message: Message) = Reply.Error(
        "org.freedesktop.DBus.Error.UnknownMethod",
        "No such method '${message.member}' on interface '${message.iface}'"
    )

    private fun properties(iface: String?, track: PhoneTrack): Map<String, Dv> = when (iface) {
        IFACE_PLAYER -> linkedMapOf(
            "PlaybackStatus" to Dv.Str(track.status),
            "LoopStatus" to Dv.Str("None"),
            "Rate" to Dv.Double(track.rate),
            "Shuffle" to Dv.Bool(false),
            "Metadata" to Dv.Dict(metadata(track)),
            "Volume" to Dv.Double(1.0),
            "Position" to Dv.I64(track.positionUs),
            "MinimumRate" to Dv.Double(0.25),
            "MaximumRate" to Dv.Double(4.0),
            "CanGoNext" to Dv.Bool(true),
            "CanGoPrevious" to Dv.Bool(true),
            "CanPlay" to Dv.Bool(true),
            "CanPause" to Dv.Bool(true),
            "CanSeek" to Dv.Bool(track.lengthUs > 0),
            "CanControl" to Dv.Bool(true)
        )
        IFACE_ROOT -> linkedMapOf(
            "CanQuit" to Dv.Bool(false),
            "CanRaise" to Dv.Bool(false),
            "HasTrackList" to Dv.Bool(false),
            "Identity" to Dv.Str("Cryogenic"),
            "DesktopEntry" to Dv.Str("cryogenic"),
            "SupportedUriSchemes" to Dv.StrArray(emptyList()),
            "SupportedMimeTypes" to Dv.StrArray(emptyList())
        )
        else -> emptyMap()
    }

    private fun metadata(track: PhoneTrack): Map<String, Dv> {
        val id = Integer.toHexString("${track.source}|${track.title}|${track.artists}|${track.album}".hashCode())
        return linkedMapOf<String, Dv>(
            "mpris:trackid" to Dv.ObjectPath("/org/mpris/MediaPlayer2/Track/t$id"),
            "xesam:title" to Dv.Str(track.title),
            "xesam:artist" to Dv.StrArray(track.artists),
            "xesam:album" to Dv.Str(track.album)
        ).apply {
            if (track.lengthUs > 0) put("mpris:length", Dv.I64(track.lengthUs))
        }
    }

    /** Controles do player (play/pause/próxima...) repassados ao app que está tocando. */
    private fun control(message: Message): Reply {
        val controller = activeController() ?: return Reply.Ok()
        val controls = controller.transportControls
        when (message.member) {
            "Play" -> controls.play()
            "Pause" -> controls.pause()
            "PlayPause" -> if (controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                controls.pause()
            } else controls.play()
            "Stop" -> controls.stop()
            "Next" -> controls.skipToNext()
            "Previous" -> controls.skipToPrevious()
            "Seek" -> {
                val offsetUs = (message.args.firstOrNull() as? Long) ?: return Reply.Ok()
                val now = track()?.positionUs ?: 0L
                controls.seekTo(((now + offsetUs) / 1000L).coerceAtLeast(0L))
            }
            "SetPosition" -> {
                val positionUs = (message.args.getOrNull(1) as? Long) ?: return Reply.Ok()
                controls.seekTo((positionUs / 1000L).coerceAtLeast(0L))
            }
            "OpenUri" -> Unit
            else -> return unknownMethod(message)
        }
        cached = null
        return Reply.Ok()
    }

    private fun activeController(): MediaController? = runCatching {
        if (!PhoneMedia.hasAccess(appContext)) return null
        val manager = appContext.getSystemService(MediaSessionManager::class.java) ?: return null
        val sessions = manager.getActiveSessions(PhoneMedia.listenerComponent(appContext))
        sessions.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
    }.getOrNull()

    private const val INTROSPECTION = """<!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN" "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
<node><interface name="org.mpris.MediaPlayer2"/><interface name="org.mpris.MediaPlayer2.Player"/><interface name="org.freedesktop.DBus.Properties"/></node>"""

    // ---------------------------------------------------------------- formato binário

    private class Message(
        val type: Int,
        val flags: Int,
        val serial: Long,
        val path: String?,
        val iface: String?,
        val member: String?,
        val destination: String?,
        val args: List<Any?>
    )

    private fun readFully(input: InputStream, size: Int): ByteArray {
        val data = ByteArray(size)
        var read = 0
        while (read < size) {
            val n = input.read(data, read, size - read)
            if (n < 0) throw EOFException()
            read += n
        }
        return data
    }

    private fun readMessage(input: InputStream): Message? {
        val fixed = try {
            readFully(input, 16)
        } catch (e: EOFException) {
            return null
        }
        val order = when (fixed[0].toInt().toChar()) {
            'l' -> ByteOrder.LITTLE_ENDIAN
            'B' -> ByteOrder.BIG_ENDIAN
            else -> throw IOException("bad endianness")
        }
        val head = ByteBuffer.wrap(fixed).order(order)
        val bodyLength = head.getInt(4).toLong() and 0xFFFFFFFFL
        val serial = head.getInt(8).toLong() and 0xFFFFFFFFL
        val fieldsLength = head.getInt(12).toLong() and 0xFFFFFFFFL
        if (fieldsLength > 65536 || bodyLength > 1 shl 20) throw IOException("message too large")
        val fieldsEnd = 16 + fieldsLength.toInt()
        val bodyStart = (fieldsEnd + 7) and 7.inv()
        val rest = readFully(input, bodyStart - 16 + bodyLength.toInt())
        val all = ByteArray(16 + rest.size).also {
            System.arraycopy(fixed, 0, it, 0, 16)
            System.arraycopy(rest, 0, it, 16, rest.size)
        }
        val buffer = ByteBuffer.wrap(all).order(order)
        val reader = DbusReader(buffer, 0)
        buffer.position(12)
        @Suppress("UNCHECKED_CAST")
        val fields = reader.read("a(yv)") as List<List<Any?>>
        val map = fields.associate { (it[0] as Number).toInt() to it[1] }
        val signature = map[8] as? String ?: ""
        buffer.position(bodyStart)
        val bodyReader = DbusReader(buffer, bodyStart)
        val args = mutableListOf<Any?>()
        var i = 0
        while (i < signature.length) {
            val end = DbusReader.typeEnd(signature, i)
            args += bodyReader.read(signature.substring(i, end))
            i = end
        }
        return Message(
            type = fixed[1].toInt() and 0xFF,
            flags = fixed[2].toInt() and 0xFF,
            serial = serial,
            path = map[1] as? String,
            iface = map[2] as? String,
            member = map[3] as? String,
            destination = map[6] as? String,
            args = args
        )
    }

    /** Leitor genérico de valores D-Bus (alinhamento relativo a [base]). */
    private class DbusReader(private val buffer: ByteBuffer, private val base: Int) {
        private fun align(n: Int) {
            val rel = buffer.position() - base
            val pad = (n - rel % n) % n
            buffer.position(buffer.position() + pad)
        }

        fun read(sig: String): Any? = when (sig[0]) {
            'y' -> buffer.get().toInt() and 0xFF
            'b' -> { align(4); buffer.int != 0 }
            'n' -> { align(2); buffer.short.toInt() }
            'q' -> { align(2); buffer.short.toInt() and 0xFFFF }
            'i' -> { align(4); buffer.int }
            'u', 'h' -> { align(4); buffer.int.toLong() and 0xFFFFFFFFL }
            'x', 't' -> { align(8); buffer.long }
            'd' -> { align(8); buffer.double }
            's', 'o' -> {
                align(4)
                val len = buffer.int
                val bytes = ByteArray(len).also { buffer.get(it) }
                buffer.get()
                String(bytes, Charsets.UTF_8)
            }
            'g' -> {
                val len = buffer.get().toInt() and 0xFF
                val bytes = ByteArray(len).also { buffer.get(it) }
                buffer.get()
                String(bytes, Charsets.US_ASCII)
            }
            'v' -> {
                val inner = read("g") as String
                read(inner)
            }
            '(' -> {
                align(8)
                val list = mutableListOf<Any?>()
                var i = 1
                while (sig[i] != ')') {
                    val end = typeEnd(sig, i)
                    list += read(sig.substring(i, end))
                    i = end
                }
                list
            }
            'a' -> {
                align(4)
                val length = buffer.int
                val element = sig.substring(1)
                if (element[0] == '{') {
                    align(8)
                    val end = buffer.position() + length
                    val keySig = element.substring(1, typeEnd(element, 1))
                    val valueSig = element.substring(1 + keySig.length, element.length - 1)
                    val map = linkedMapOf<Any?, Any?>()
                    while (buffer.position() < end) {
                        align(8)
                        map[read(keySig)] = read(valueSig)
                    }
                    map
                } else {
                    align(alignment(element[0]))
                    val end = buffer.position() + length
                    val list = mutableListOf<Any?>()
                    while (buffer.position() < end) list += read(element)
                    list
                }
            }
            else -> throw IOException("unsupported type $sig")
        }

        companion object {
            fun alignment(c: Char) = when (c) {
                'y', 'g', 'v' -> 1
                'n', 'q' -> 2
                'x', 't', 'd', '(', '{' -> 8
                else -> 4
            }

            fun typeEnd(sig: String, start: Int): Int = when (sig[start]) {
                'a' -> typeEnd(sig, start + 1)
                '(', '{' -> {
                    val close = if (sig[start] == '(') ')' else '}'
                    var i = start + 1
                    while (sig[i] != close) i = typeEnd(sig, i)
                    i + 1
                }
                else -> start + 1
            }
        }
    }

    /** Valores que o launcher envia. */
    private sealed interface Dv {
        val sig: String

        data class Str(val v: String) : Dv { override val sig = "s" }
        data class ObjectPath(val v: String) : Dv { override val sig = "o" }
        data class Bool(val v: Boolean) : Dv { override val sig = "b" }
        data class U32(val v: Long) : Dv { override val sig = "u" }
        data class I64(val v: Long) : Dv { override val sig = "x" }
        data class Double(val v: kotlin.Double) : Dv { override val sig = "d" }
        data class StrArray(val v: List<String>) : Dv { override val sig = "as" }
        data class Dict(val v: Map<String, Dv>) : Dv { override val sig = "a{sv}" }
        data class Variant(val v: Dv) : Dv { override val sig = "v" }
    }

    private class DbusWriter {
        private val out = ByteArrayOutputStream()
        private val scratch = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        val size get() = out.size()

        fun align(n: Int) {
            while (out.size() % n != 0) out.write(0)
        }

        fun byte(v: Int) = out.write(v)
        fun u32(v: Long) { align(4); scratch.clear(); scratch.putInt(v.toInt()); out.write(scratch.array(), 0, 4) }
        fun i64(v: Long) { align(8); scratch.clear(); scratch.putLong(v); out.write(scratch.array(), 0, 8) }
        fun f64(v: kotlin.Double) { align(8); scratch.clear(); scratch.putDouble(v); out.write(scratch.array(), 0, 8) }
        fun str(v: String) {
            val bytes = v.toByteArray(Charsets.UTF_8)
            u32(bytes.size.toLong()); out.write(bytes); out.write(0)
        }
        fun sig(v: String) {
            val bytes = v.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size); out.write(bytes); out.write(0)
        }

        /** Escreve um array: comprimento em bytes, alinhamento do elemento e os elementos. */
        fun array(elementAlign: Int, block: () -> Unit) {
            align(4)
            val lengthAt = out.size()
            u32(0)
            align(elementAlign)
            val start = out.size()
            block()
            val length = out.size() - start
            patch(lengthAt, length)
        }

        private fun patch(at: Int, value: Int) {
            val bytes = out.toByteArray()
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(at, value)
            out.reset()
            out.write(bytes)
        }

        fun value(v: Dv) {
            when (v) {
                is Dv.Str -> str(v.v)
                is Dv.ObjectPath -> str(v.v)
                is Dv.Bool -> u32(if (v.v) 1 else 0)
                is Dv.U32 -> u32(v.v)
                is Dv.I64 -> i64(v.v)
                is Dv.Double -> f64(v.v)
                is Dv.StrArray -> array(4) { v.v.forEach { str(it) } }
                is Dv.Dict -> array(8) {
                    v.v.forEach { (key, item) ->
                        align(8)
                        str(key)
                        sig(item.sig)
                        value(item)
                    }
                }
                is Dv.Variant -> { sig(v.v.sig); value(v.v) }
            }
        }

        fun bytes(): ByteArray = out.toByteArray()
    }

    private fun encode(
        type: Int,
        serial: Int,
        replySerial: Long,
        destination: String,
        signature: String,
        values: List<Dv>,
        errorName: String? = null
    ): ByteArray {
        val body = DbusWriter().apply { values.forEach { value(it) } }.bytes()
        val header = DbusWriter()
        header.byte('l'.code)
        header.byte(type)
        header.byte(1) //NO_REPLY_EXPECTED
        header.byte(1)
        header.u32(body.size.toLong())
        header.u32(serial.toLong())
        header.array(8) {
            fun field(code: Int, sig: String, write: () -> Unit) {
                header.align(8)
                header.byte(code)
                header.sig(sig)
                write()
            }
            errorName?.let { field(4, "s") { header.str(it) } }
            field(5, "u") { header.u32(replySerial) }
            field(6, "s") { header.str(destination) }
            field(7, "s") { header.str(BUS) }
            if (signature.isNotEmpty()) field(8, "g") { header.sig(signature) }
        }
        header.align(8)
        return header.bytes() + body
    }
}
