package com.russoviller.cryogenic.game.account.wardrobe

import android.graphics.BitmapFactory
import com.russoviller.cryogenic.path.PathManager
import java.io.File
import java.security.MessageDigest
import java.util.UUID

object SkinLibrary {
    data class Entry(val file: File, val model: SkinModelType)

    private val dir: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "skin_library").apply { mkdirs() }

    private fun hashOf(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun list(): List<Entry> {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".png") } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }.map { file ->
            val model = if (file.nameWithoutExtension.endsWith("_slim")) SkinModelType.ALEX else SkinModelType.STEVE
            Entry(file, model)
        }
    }

    private fun pixelsOf(file: File): Pair<Int, IntArray>? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        return w to pixels
    }

    private fun sameSkin(a: Pair<Int, IntArray>, b: Pair<Int, IntArray>): Boolean {
        val (wa, pa) = a
        val (wb, pb) = b
        val baseA = if (pa.size == wa * wa / 2) pa.copyOf(wa * wa) else pa
        val baseB = if (pb.size == wb * wb / 2) pb.copyOf(wb * wb) else pb
        if (wa != wb || baseA.size != baseB.size) return false
        var compared = 0
        var different = 0
        val limit = minOf(pa.size, pb.size)
        for (i in 0 until limit) {
            val x = baseA[i]
            val y = baseB[i]
            val ax = x ushr 24
            val ay = y ushr 24
            if (ax == 0 || ay == 0) continue
            compared++
            val dr = kotlin.math.abs(((x shr 16) and 0xFF) - ((y shr 16) and 0xFF))
            val dg = kotlin.math.abs(((x shr 8) and 0xFF) - ((y shr 8) and 0xFF))
            val db = kotlin.math.abs((x and 0xFF) - (y and 0xFF))
            if (dr > 8 || dg > 8 || db > 8) different++
        }
        return compared > 0 && different * 50 < compared
    }

    private fun findSame(source: File): File? {
        val pixels = pixelsOf(source) ?: return null
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".png") } ?: return null
        return files.firstOrNull { other -> pixelsOf(other)?.let { sameSkin(pixels, it) } == true }
    }

    @Synchronized
    fun save(source: File, model: SkinModelType, touch: Boolean = true) {
        if (!source.isFile) return
        val suffix = if (model == SkinModelType.ALEX) "slim" else "wide"
        val existing = findSame(source)
        if (existing != null) {
            val base = existing.nameWithoutExtension.substringBeforeLast("_")
            val target = File(dir, "${base}_$suffix.png")
            if (existing != target) existing.renameTo(target)
            if (touch) target.setLastModified(System.currentTimeMillis())
            return
        }
        val hash = hashOf(source)
        val target = File(dir, "${hash}_$suffix.png")
        source.copyTo(target, overwrite = true)
        target.setLastModified(System.currentTimeMillis())
    }

    fun saveBytes(bytes: ByteArray, model: SkinModelType): File {
        val temp = File(PathManager.DIR_IMAGE_CACHE, "skin_add_${UUID.randomUUID()}.png")
        temp.parentFile?.mkdirs()
        temp.writeBytes(bytes)
        save(temp, model)
        return temp
    }

    @Synchronized
    fun dedupe() {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".png") }?.sortedByDescending { it.lastModified() } ?: return
        val kept = ArrayList<Pair<Int, IntArray>>()
        files.forEach { file ->
            val pixels = pixelsOf(file) ?: return@forEach
            if (kept.any { sameSkin(pixels, it) }) file.delete() else kept.add(pixels)
        }
    }

    fun delete(entry: Entry) {
        entry.file.delete()
    }

    fun prepare(entry: Entry): File {
        val cache = File(PathManager.DIR_IMAGE_CACHE, "skin_pick_${UUID.randomUUID()}")
        cache.parentFile?.mkdirs()
        entry.file.copyTo(cache, overwrite = true)
        entry.file.setLastModified(System.currentTimeMillis())
        return cache
    }
}
