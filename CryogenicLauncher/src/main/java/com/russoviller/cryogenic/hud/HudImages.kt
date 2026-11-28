package com.russoviller.cryogenic.hud

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Imagens (PNG) dos botões dos HUDs. Ficam na pasta "images" de cada esquema e os HUDs guardam
 * só o nome (igual ao formato do PojaRush). Para compartilhar, o HUD vai num .zip junto com as imagens.
 */
object HudImages {
    private val EXT = listOf("png", "webp", "jpg", "jpeg")
    private const val MAX_SIZE = 512

    fun dir(scheme: HudScheme): File = File(HudStore.dir(scheme), "images")

    private fun clean(name: String): String =
        name.substringAfterLast('/').substringAfterLast('\\').trim()

    /** Acha o arquivo da imagem pelo nome (com ou sem extensão). */
    fun file(scheme: HudScheme, ref: String): File? {
        if (ref.isBlank()) return null
        val base = dir(scheme)
        val name = clean(ref)
        File(base, name).takeIf { it.isFile }?.let { return it }
        EXT.forEach { e -> File(base, "$name.$e").takeIf { it.isFile }?.let { return it } }
        // nomes parecidos (maiúsculas, espaços)
        val key = name.substringBeforeLast('.').lowercase().filter { it.isLetterOrDigit() }
        return base.listFiles()?.firstOrNull { f ->
            f.isFile && f.extension.lowercase() in EXT && f.nameWithoutExtension.lowercase().filter { it.isLetterOrDigit() } == key
        }
    }

    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun bitmap(scheme: HudScheme, ref: String): ImageBitmap? = runCatching { decode(scheme, ref) }.getOrNull()

    private fun decode(scheme: HudScheme, ref: String): ImageBitmap? {
        val f = file(scheme, ref) ?: return null
        val key = f.absolutePath + ":" + f.lastModified() + ":" + f.length()
        cache.get(key)?.let { return it }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > MAX_SIZE * 2 || bounds.outHeight / sample > MAX_SIZE * 2) sample *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }.getOrNull()?.also { cache.put(key, it) }
    }

    fun list(scheme: HudScheme): List<File> =
        dir(scheme).listFiles()?.filter { it.isFile && it.extension.lowercase() in EXT }?.sortedBy { it.name.lowercase() } ?: emptyList()

    /** Salva uma imagem e devolve o nome para guardar no botão. */
    fun save(scheme: HudScheme, fileName: String, input: InputStream): String {
        val d = dir(scheme).apply { mkdirs() }
        val raw = clean(fileName).ifBlank { "image.png" }
        val ext = raw.substringAfterLast('.', "png").lowercase().takeIf { it in EXT } ?: "png"
        val base = raw.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_\\-]"), "_").trim('_').ifBlank { "image" }.take(40)
        var target = File(d, "$base.$ext")
        var n = 2
        while (target.exists()) target = File(d, "${base}_${n++}.$ext")
        target.outputStream().use { input.copyTo(it) }
        return target.nameWithoutExtension
    }

    fun delete(scheme: HudScheme, ref: String) {
        file(scheme, ref)?.delete()
    }

    /** Nomes de imagem usados pelo HUD. */
    fun refs(layout: HudLayout): Set<String> = buildSet {
        layout.buttons.forEach { b -> add(b.style.image); add(b.style.imagePressed) }
        layout.sticks.forEach { s -> add(s.style.image); add(s.style.imagePressed) }
    }.filter { it.isNotBlank() }.toSet()

    fun missing(scheme: HudScheme, layout: HudLayout): List<String> = refs(layout).filter { file(scheme, it) == null }

    fun isZip(bytes: ByteArray) = bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    class Unpacked(val json: String?, val images: Int)

    /** Lê um .zip de HUD: o primeiro .json vira o HUD e as imagens vão para a pasta do esquema. */
    fun unpack(scheme: HudScheme, bytes: ByteArray): Unpacked {
        var json: String? = null
        var images = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e: ZipEntry? = zip.nextEntry
            while (e != null) {
                if (!e.isDirectory) {
                    val name = e.name.substringAfterLast('/')
                    val ext = name.substringAfterLast('.', "").lowercase()
                    when {
                        ext == "json" && json == null -> json = zip.readBytes().decodeToString()
                        ext in EXT -> {
                            // mantém o nome original (o HUD aponta para ele)
                            val d = dir(scheme).apply { mkdirs() }
                            val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                            File(d, safe).outputStream().use { zip.copyTo(it) }
                            images++
                        }
                    }
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        return Unpacked(json, images)
    }

    /** Cria um .zip com o HUD e as imagens que ele usa (para compartilhar). */
    fun pack(scheme: HudScheme, hudFile: File, layout: HudLayout?, out: File): File {
        out.parentFile?.mkdirs()
        ZipOutputStream(out.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(hudFile.name.substringBeforeLast('.') + ".json"))
            hudFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            layout?.let { refs(it) }?.mapNotNull { file(scheme, it) }?.distinct()?.forEach { f ->
                zip.putNextEntry(ZipEntry("images/" + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return out
    }
}
