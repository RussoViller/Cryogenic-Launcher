package com.russoviller.cryogenic.extras

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.utils.image.isImageFile
import com.russoviller.cryogenic.utils.video.isVideoFile
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import java.io.File
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/** Referência a um wallpaper guardado na biblioteca. */
@Serializable
data class WallpaperRef(val id: String, val video: Boolean)

/**
 * Biblioteca de wallpapers do Launcher.
 *
 * Cada wallpaper (imagem, GIF ou vídeo) é guardado uma única vez, pelo hash do conteúdo, em
 * `files/wallpaper_library/<id>.file`, com uma miniatura `<id>.thumb.jpg`. Várias predefinições
 * que usam o mesmo wallpaper apontam para o mesmo arquivo, então não ocupam espaço a mais.
 * Arquivos que nenhuma predefinição usa mais são apagados por [collectGarbage].
 *
 * Todas as funções fazem E/S de disco: chame fora da thread principal.
 */
object CryoWallpaperLibrary {
    private const val THUMB_MAX = 640

    val dir: File get() = File(PathManager.DIR_FILES_PRIVATE, "wallpaper_library")

    fun file(id: String) = File(dir, "$id.file")
    fun thumb(id: String) = File(dir, "$id.thumb.jpg")

    /** Lê o tipo do arquivo: null se não for imagem nem vídeo. */
    fun mediaKind(file: File): Boolean? = when {
        !file.isFile -> null
        file.isImageFile() -> false
        file.isVideoFile() -> true
        else -> null
    }

    /** Guarda [source] na biblioteca (se ainda não estiver lá) e devolve a referência. */
    fun store(source: File): WallpaperRef? {
        val video = mediaKind(source) ?: return null
        dir.mkdirs()
        val id = sha256(source)
        val target = file(id)
        if (!target.isFile || target.length() != source.length()) {
            copyAtomically(source, target)
        }
        if (!thumb(id).isFile) writeThumb(target, video, thumb(id))
        return WallpaperRef(id, video)
    }

    /** Copia o wallpaper [ref] para [target] (ex.: o arquivo do fundo do Launcher). */
    fun restore(ref: WallpaperRef, target: File): Boolean {
        val source = file(ref.id)
        if (!source.isFile) return false
        copyAtomically(source, target)
        return true
    }

    /** Apaga da biblioteca tudo que não estiver em [keep]. */
    fun collectGarbage(keep: Set<String>) {
        dir.listFiles()?.forEach { f ->
            val id = f.name.substringBefore('.')
            if (id !in keep) FileUtils.deleteQuietly(f)
        }
    }

    /** Tamanho total ocupado pela biblioteca, em bytes. */
    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    /** Gera uma miniatura em memória (usada também para o visual atual, sem guardar nada). */
    fun decodeThumb(file: File, video: Boolean, maxSide: Int = THUMB_MAX): Bitmap? = runCatching {
        if (video) videoFrame(file, maxSide) else decodeImage(file, maxSide)
    }.getOrNull()

    private fun writeThumb(file: File, video: Boolean, out: File) {
        val bmp = decodeThumb(file, video) ?: return
        runCatching {
            val tmp = File(out.parentFile, out.name + ".tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 86, it) }
            if (!tmp.renameTo(out)) {
                FileUtils.copyFile(tmp, out)
                FileUtils.deleteQuietly(tmp)
            }
        }
        bmp.recycle()
    }

    private fun decodeImage(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        return scaleDown(bmp, maxSide)
    }

    private fun videoFrame(file: File, maxSide: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val frame = if (Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxSide, maxSide)
            } else {
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } ?: retriever.frameAtTime
            frame?.let { scaleDown(it, maxSide) }
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleDown(bmp: Bitmap, maxSide: Int): Bitmap {
        val longest = max(bmp.width, bmp.height)
        if (longest <= maxSide) return bmp
        val scale = maxSide.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).roundToInt().coerceAtLeast(1),
            (bmp.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
        if (scaled != bmp) bmp.recycle()
        return scaled
    }

    /** Copia para um arquivo temporário e só então troca pelo destino, para nunca deixar um arquivo pela metade. */
    private fun copyAtomically(source: File, target: File) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        FileUtils.deleteQuietly(tmp)
        FileUtils.copyFile(source, tmp)
        FileUtils.deleteQuietly(target)
        if (!tmp.renameTo(target)) {
            FileUtils.copyFile(tmp, target)
            FileUtils.deleteQuietly(tmp)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1 shl 16).use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.take(32)
    }
}
