package com.russoviller.cryogenic.game.chunks

import java.io.ByteArrayOutputStream

/**
 * Leitor do formato "LZ4Block" (lz4-java, usado pelo Minecraft com region-file-compression=lz4).
 * Cada bloco: "LZ4Block" + token + tamanho comprimido + tamanho original + checksum (LE),
 * seguido dos dados (crus ou LZ4). Termina num bloco de tamanho 0.
 */
internal object Lz4Block {
    private val MAGIC = "LZ4Block".toByteArray(Charsets.US_ASCII)
    private const val HEADER = 21

    fun decode(data: ByteArray): ByteArray? {
        val out = ByteArrayOutputStream(data.size * 3)
        var pos = 0
        while (pos + HEADER <= data.size) {
            for (i in MAGIC.indices) if (data[pos + i] != MAGIC[i]) return null
            val method = data[pos + 8].toInt() and 0xF0
            val compressed = le(data, pos + 9)
            val original = le(data, pos + 13)
            pos += HEADER
            if (original == 0) break
            if (compressed < 0 || original < 0 || pos + compressed > data.size || original > 64 * 1024 * 1024) return null
            when (method) {
                0x10 -> out.write(data, pos, compressed)
                0x20 -> {
                    val block = ByteArray(original)
                    if (decompress(data, pos, compressed, block) != original) return null
                    out.write(block)
                }
                else -> return null
            }
            pos += compressed
        }
        return out.toByteArray()
    }

    private fun le(b: ByteArray, i: Int): Int =
        (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8) or
            ((b[i + 2].toInt() and 0xFF) shl 16) or ((b[i + 3].toInt() and 0xFF) shl 24)

    /** Descompressão de um bloco LZ4 cru. Devolve quantos bytes escreveu (-1 se inválido). */
    fun decompress(src: ByteArray, start: Int, length: Int, dst: ByteArray): Int {
        var s = start
        val end = start + length
        var d = 0
        while (s < end) {
            val token = src[s++].toInt() and 0xFF
            var lit = token ushr 4
            if (lit == 15) {
                var b: Int
                do {
                    if (s >= end) return -1
                    b = src[s++].toInt() and 0xFF
                    lit += b
                } while (b == 255)
            }
            if (s + lit > end || d + lit > dst.size) return -1
            System.arraycopy(src, s, dst, d, lit)
            s += lit
            d += lit
            if (s >= end) break //último trecho só tem literais
            if (s + 2 > end) return -1
            val offset = (src[s].toInt() and 0xFF) or ((src[s + 1].toInt() and 0xFF) shl 8)
            s += 2
            if (offset == 0 || offset > d) return -1
            var match = token and 0x0F
            if (match == 15) {
                var b: Int
                do {
                    if (s >= end) return -1
                    b = src[s++].toInt() and 0xFF
                    match += b
                } while (b == 255)
            }
            match += 4
            if (d + match > dst.size) return -1
            var from = d - offset
            //cópia byte a byte (as regiões podem se sobrepor)
            repeat(match) { dst[d++] = dst[from++] }
        }
        return d
    }
}
