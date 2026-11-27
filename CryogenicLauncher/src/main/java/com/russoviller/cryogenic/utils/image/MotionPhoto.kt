package com.russoviller.cryogenic.utils.image

import java.io.File
import java.io.RandomAccessFile

private val MP4_BRANDS = setOf("isom", "iso2", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "qt  ", "M4V ", "3gp4", "3gp5", "hvc1", "heic", "msnv", "dash")

fun File.extractMotionPhotoVideo(): Boolean {
    if (!exists() || length() < 1024) return false
    val bytes = readBytes()
    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return false
    var index = 8
    while (index < bytes.size - 12) {
        if (bytes[index] == 'f'.code.toByte() &&
            bytes[index + 1] == 't'.code.toByte() &&
            bytes[index + 2] == 'y'.code.toByte() &&
            bytes[index + 3] == 'p'.code.toByte()
        ) {
            val start = index - 4
            val boxSize = ((bytes[start].toInt() and 0xFF) shl 24) or
                ((bytes[start + 1].toInt() and 0xFF) shl 16) or
                ((bytes[start + 2].toInt() and 0xFF) shl 8) or
                (bytes[start + 3].toInt() and 0xFF)
            val brand = String(bytes, index + 4, 4, Charsets.ISO_8859_1)
            if (boxSize in 8..256 && brand in MP4_BRANDS && bytes.size - start > 4096) {
                val video = File(parentFile, "$name.motion")
                RandomAccessFile(video, "rw").use { out ->
                    out.setLength(0)
                    out.write(bytes, start, bytes.size - start)
                }
                if (!delete() || !video.renameTo(this)) {
                    video.copyTo(this, overwrite = true)
                    video.delete()
                }
                return true
            }
        }
        index++
    }
    return false
}
