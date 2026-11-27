package com.russoviller.cryogenic.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class LiveAudioMixTest {
    @Test
    fun ringKeepsNewestAndPullsInOrder() {
        val ring = VoiceRing().apply { capacity = 10 }
        ring.push(ShortArray(6) { (it + 1).toShort() }, 6)
        ring.push(ShortArray(6) { (it + 7).toShort() }, 6) // 12 > 10: descarta as 2 mais antigas
        assertEquals(10, ring.available())
        val out = ShortArray(4)
        assertEquals(4, ring.pull(out, 4))
        assertEquals(listOf<Short>(3, 4, 5, 6), out.toList())
        val rest = ShortArray(20)
        assertEquals(6, ring.pull(rest, 20))
        assertEquals(12.toShort(), rest[5])
        assertEquals(0, ring.pull(rest, 5))
    }

    @Test
    fun resampler48kTo44k() {
        val r = LinearResampler()
        // 1 s de seno de 440 Hz a 48 kHz, entregue em blocos de 960 (como o Voice Chat)
        val total = 48000
        val src = ShortArray(total) { (sin(2 * PI * 440 * it / 48000.0) * 10000).toInt().toShort() }
        val out = ArrayList<Short>()
        var i = 0
        while (i < total) {
            val block = src.copyOfRange(i, minOf(total, i + 960))
            out.addAll(r.process(block, 48000, 44100).toList())
            i += 960
        }
        // ~44100 amostras por segundo
        assertTrue("count=${out.size}", abs(out.size - 44100) <= 3)
        // continua sendo um seno de 440 Hz (sem estalos nas emendas dos blocos)
        var maxErr = 0.0
        for (k in 1 until out.size - 1) {
            val expected = sin(2 * PI * 440 * k / 44100.0) * 10000
            maxErr = maxOf(maxErr, abs(out[k] - expected))
        }
        assertTrue("maxErr=$maxErr", maxErr < 150)
    }

    @Test
    fun resamplerSameRateIsCopy() {
        val r = LinearResampler()
        val src = shortArrayOf(1, 2, 3)
        assertEquals(src.toList(), r.process(src, 44100, 44100).toList())
    }

    @Test
    fun jitterBufferWaitsAndRecovers() {
        val ring = VoiceRing().apply { capacity = 10000 }
        val jb = JitterBuffer(ring, prebuffer = 300, maxLatency = 2000, keepLatency = 600)
        val out = ShortArray(100)
        ring.push(ShortArray(200) { 1000 }, 200)
        assertEquals("ainda juntando", 0, jb.read(out, 100))
        ring.push(ShortArray(200) { 1000 }, 200)
        assertEquals(100, jb.read(out, 100))
        assertTrue("entra suave", out[0] < 100 && out[99] == 1000.toShort())
        assertEquals(100, jb.read(out, 100))
        assertEquals(100, jb.read(out, 100))
        assertEquals(100, jb.read(out, 100))
        // acabou: não toca de novo até juntar o pré-buffer
        assertEquals(0, jb.read(out, 100))
        ring.push(ShortArray(100) { 1000 }, 100)
        assertEquals(0, jb.read(out, 100))
        // atraso acumulado demais: corta para o alvo
        ring.push(ShortArray(3000) { 7 }, 3000)
        assertEquals(100, jb.read(out, 100))
        assertEquals(500, ring.available())
    }
}
