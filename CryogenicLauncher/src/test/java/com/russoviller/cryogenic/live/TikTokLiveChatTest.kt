package com.russoviller.cryogenic.live

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TikTokLiveChatTest {

    private fun varint(o: ByteArrayOutputStream, v: Long) {
        var x = v
        while (x and 0x7fL.inv() != 0L) { o.write(((x and 0x7f) or 0x80).toInt()); x = x ushr 7 }
        o.write(x.toInt())
    }
    private fun bytes(o: ByteArrayOutputStream, n: Int, b: ByteArray) { varint(o, (n.toLong() shl 3) or 2); varint(o, b.size.toLong()); o.write(b) }
    private fun num(o: ByteArrayOutputStream, n: Int, v: Long) { varint(o, n.toLong() shl 3); varint(o, v) }
    private fun msg(block: (ByteArrayOutputStream) -> Unit) = ByteArrayOutputStream().also(block).toByteArray()

    @Test
    fun decodesChatFrames() {
        val user = msg { bytes(it, 3, "Russo 🎮".toByteArray()); bytes(it, 38, "russoviller.mc".toByteArray()) }
        val chat = msg { bytes(it, 2, user); bytes(it, 3, "olá mundo".toByteArray()) }
        val seq = msg { num(it, 3, 1234) }
        val control = msg { num(it, 2, 3) }
        val result = msg {
            bytes(it, 1, msg { m -> bytes(m, 1, "WebcastChatMessage".toByteArray()); bytes(m, 2, chat) })
            bytes(it, 1, msg { m -> bytes(m, 1, "WebcastRoomUserSeqMessage".toByteArray()); bytes(m, 2, seq) })
            bytes(it, 1, msg { m -> bytes(m, 1, "WebcastControlMessage".toByteArray()); bytes(m, 2, control) })
            bytes(it, 2, "cur".toByteArray())
            bytes(it, 5, "ext".toByteArray())
            bytes(it, 7, msg { e -> bytes(e, 1, "k".toByteArray()); bytes(e, 2, "v".toByteArray()) })
            num(it, 9, 1)
            bytes(it, 10, "wss://push.example/ws".toByteArray())
        }
        val gz = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(result) } }.toByteArray()
        val frame = msg { num(it, 2, 987654321L); bytes(it, 6, "pb".toByteArray()); bytes(it, 7, "msg".toByteArray()); bytes(it, 8, gz) }

        val pf = TikTokLiveChat.Proto.decodePushFrame(frame)
        assertEquals(987654321L, pf.logId)
        assertEquals("msg", pf.payloadType)
        val r = TikTokLiveChat.Proto.decodeFetchResult(TikTokLiveChat.gunzipIfNeeded(pf.payload))
        assertEquals(3, r.messages.size)
        assertEquals("cur", r.cursor)
        assertEquals("ext", r.internalExt)
        assertEquals(mapOf("k" to "v"), r.routeParams)
        assertTrue(r.needsAck)
        assertEquals("wss://push.example/ws", r.pushServer)
        assertEquals("Russo 🎮" to "olá mundo", TikTokLiveChat.Proto.decodeChat(r.messages[0].payload))
        assertEquals(1234L, TikTokLiveChat.Proto.decodeViewers(r.messages[1].payload))
        assertEquals(3L, TikTokLiveChat.Proto.decodeControlAction(r.messages[2].payload))

        // ack: PushFrame{logId=2, "pb", "ack", internalExt}
        val ack = TikTokLiveChat.Proto.decodePushFrame(TikTokLiveChat.Proto.encodeAck(987654321L, "ext".toByteArray()))
        assertEquals(987654321L, ack.logId)
        assertEquals("pb", ack.payloadEncoding)
        assertEquals("ack", ack.payloadType)
        assertEquals("ext", String(ack.payload))
    }

    /** Teste real (internet): -Dcryo.liveChatUser=<@ ao vivo>. Mostra as mensagens recebidas. */
    @Test
    fun liveChatOnline() {
        val u = System.getProperty("cryo.liveChatUser").orEmpty()
        assumeTrue(u.isNotEmpty())
        val got = ArrayList<String>()
        val states = ArrayList<String>()
        var detail = ""
        var viewers = ""
        val chat = TikTokLiveChat(
            onMessage = { name, text -> got += "$name: $text" },
            onState = { states += it },
            onViewers = { viewers = it },
            onDetail = { detail = it }
        )
        chat.start(u)
        val end = System.currentTimeMillis() + 90_000
        // espera mensagens novas pela conexão (WebSocket), não só o histórico do pacote inicial
        var history = -1
        while (System.currentTimeMillis() < end) {
            if (history < 0 && detail.startsWith("WS on")) history = got.size
            if (history >= 0 && got.size >= history + 1 && Regex("WS on · [1-9]").containsMatchIn(detail)) break
            Thread.sleep(200)
            shadowOf(Looper.getMainLooper()).idle()
        }
        chat.stop()
        println("HISTORY $history")
        println("METHODS ${chat.methodCounts}")
        println("STATES $states")
        println("DETAIL $detail")
        println("VIEWERS $viewers")
        got.forEach { println("CHAT $it") }
        assertTrue("no chat messages (detail=$detail)", got.isNotEmpty())
        assertTrue("no messages through the WebSocket (detail=$detail)", history >= 0 && got.size > history)
    }
}
