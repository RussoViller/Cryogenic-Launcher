package com.russoviller.cryogenic.streamer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.russoviller.cryogenic.setting.AllSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Um PNG do modo streamer (imagem da galeria copiada para a pasta do app).
 * Posição ([x], [y]) e largura ([w]) são frações da tela; a altura sai da proporção do recorte,
 * então a imagem nunca é esticada. O recorte ([cropL]..[cropB]) é em frações da imagem.
 */
@Serializable
data class StreamImage(
    val file: String = "",
    val enabled: Boolean = true,
    val opacity: Int = 100,
    val x: Float = 0.05f,
    val y: Float = 0.05f,
    val w: Float = 0.2f,
    val imgW: Int = 0,
    val imgH: Int = 0,
    val cropL: Float = 0f,
    val cropT: Float = 0f,
    val cropR: Float = 1f,
    val cropB: Float = 1f
) {
    val hasImage: Boolean get() = file.isNotEmpty() && imgW > 0 && imgH > 0
    val cropWidth: Float get() = (cropR - cropL) * imgW
    val cropHeight: Float get() = (cropB - cropT) * imgH

    /** Altura na tela (fração), para a largura [w] numa tela [screenW] x [screenH]. */
    fun heightOn(screenW: Float, screenH: Float): Float =
        if (screenH <= 0f || cropWidth <= 0f) w else w * screenW * cropHeight / cropWidth / screenH
}

/** Um texto do modo streamer. Cores em ARGB; [size] em sp; [glow], [opacity] e [bgOpacity] de 0 a 100. */
@Serializable
data class StreamText(
    val text: String = "",
    val enabled: Boolean = false,
    val x: Float = 0.05f,
    val y: Float = 0.1f,
    val size: Int = 24,
    val color: Int = 0xFFFFFFFF.toInt(),
    val bold: Boolean = true,
    val opacity: Int = 100,
    val glow: Int = 0,
    val bg: Boolean = true,
    val bgColor: Int = 0xFF0A1A2E.toInt(),
    val bgOpacity: Int = 60,
    val bgRound: Int = 10
)

enum class StreamEditMode { IMAGES, TEXTS }

/** Estado dos PNGs e textos na tela (processo do jogo), salvos nos ajustes. */
object StreamExtrasStore {
    const val IMAGE_COUNT = 8
    const val TEXT_COUNT = 6
    const val MIN_SIZE = 0.03f
    const val MIN_CROP = 0.04f
    const val TEXT_MIN = 8
    const val TEXT_MAX = 120

    /** Maior lado salvo: imagens maiores são reduzidas ao importar (economiza memória). */
    private const val MAX_SIDE = 2048

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val images = mutableStateListOf<StreamImage>()
    val texts = mutableStateListOf<StreamText>()

    /** Tela de ajuste aberta (null = nenhuma). */
    var editing by mutableStateOf<StreamEditMode?>(null)

    /** PNG sendo recortado (null = nenhum). */
    var croppingImage by mutableStateOf<Int?>(null)

    private var loaded = false

    private fun defaultImage(i: Int) = StreamImage(file = "", x = 0.04f + (i % 4) * 0.2f, y = 0.06f + (i / 4) * 0.3f)
    private fun defaultText(i: Int) = StreamText(x = 0.04f, y = 0.08f + i * 0.12f)

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val savedImages = runCatching {
            json.decodeFromString<List<StreamImage>>(AllSettings.cryoStreamImages.getValue())
        }.getOrNull().orEmpty()
        val savedTexts = runCatching {
            json.decodeFromString<List<StreamText>>(AllSettings.cryoStreamTexts.getValue())
        }.getOrNull().orEmpty()
        images.clear()
        for (i in 0 until IMAGE_COUNT) {
            val s = savedImages.getOrNull(i)?.let(::sanitizeImage)
            // arquivo apagado por fora: o quadrado volta a ficar vazio
            images.add(if (s != null && s.hasImage && File(s.file).isFile) s else defaultImage(i))
        }
        texts.clear()
        for (i in 0 until TEXT_COUNT) texts.add(savedTexts.getOrNull(i)?.let(::sanitizeText) ?: defaultText(i))
    }

    private fun sanitizeImage(o: StreamImage): StreamImage {
        val w = o.w.coerceIn(MIN_SIZE, 1f)
        val l = o.cropL.coerceIn(0f, 1f - MIN_CROP)
        val t = o.cropT.coerceIn(0f, 1f - MIN_CROP)
        return o.copy(
            opacity = o.opacity.coerceIn(0, 100),
            w = w,
            x = o.x.coerceIn(0f, 1f - w),
            y = o.y.coerceIn(0f, 0.98f),
            cropL = l,
            cropT = t,
            cropR = o.cropR.coerceIn(l + MIN_CROP, 1f),
            cropB = o.cropB.coerceIn(t + MIN_CROP, 1f)
        )
    }

    private fun sanitizeText(o: StreamText): StreamText = o.copy(
        text = o.text.take(300),
        x = o.x.coerceIn(0f, 0.98f),
        y = o.y.coerceIn(0f, 0.98f),
        size = o.size.coerceIn(TEXT_MIN, TEXT_MAX),
        opacity = o.opacity.coerceIn(0, 100),
        glow = o.glow.coerceIn(0, 100),
        bgOpacity = o.bgOpacity.coerceIn(0, 100),
        bgRound = o.bgRound.coerceIn(0, 40)
    )

    /** Mantém o PNG inteiro dentro da tela (a altura depende da tela). */
    fun fitToScreen(o: StreamImage, screenW: Float, screenH: Float): StreamImage {
        var w = o.w
        var h = o.copy(w = w).heightOn(screenW, screenH)
        if (h > 1f) {
            w *= 1f / h
            h = 1f
        }
        return o.copy(w = w, x = o.x.coerceIn(0f, 1f - w), y = o.y.coerceIn(0f, (1f - h).coerceAtLeast(0f)))
    }

    fun updateImage(index: Int, save: Boolean = true, change: (StreamImage) -> StreamImage) {
        ensureLoaded()
        images[index] = sanitizeImage(change(images[index]))
        if (save) saveImages()
    }

    fun updateText(index: Int, save: Boolean = true, change: (StreamText) -> StreamText) {
        ensureLoaded()
        texts[index] = sanitizeText(change(texts[index]))
        if (save) saveTexts()
    }

    fun saveImages() = AllSettings.cryoStreamImages.save(json.encodeToString(images.toList()))
    fun saveTexts() = AllSettings.cryoStreamTexts.save(json.encodeToString(texts.toList()))

    fun removeImage(index: Int) {
        ensureLoaded()
        val old = images[index]
        deleteFile(old.file)
        images[index] = defaultImage(index).copy(x = old.x, y = old.y)
        saveImages()
    }

    private fun deleteFile(path: String) {
        if (path.isEmpty()) return
        StreamImageCache.forget(path)
        runCatching { File(path).delete() }
    }

    /**
     * Copia a imagem escolhida para a pasta do app (em PNG, mantendo a transparência)
     * e coloca no quadrado [index]. Retorna false se não deu para abrir a imagem.
     */
    suspend fun importImage(context: Context, index: Int, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        //GIF: copia o arquivo original para manter a animação
        if (isGif(context, uri)) return@withContext importGif(context, index, uri)
        val bitmap = runCatching { decodeScaled(context, uri) }.getOrNull() ?: return@withContext false
        val dir = File(context.filesDir, "stream_images").apply { mkdirs() }
        val file = File(dir, "png_${index}_${System.currentTimeMillis()}.png")
        val ok = runCatching {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.getOrDefault(false)
        val w = bitmap.width
        val h = bitmap.height
        bitmap.recycle()
        if (!ok) {
            file.delete()
            return@withContext false
        }
        withContext(Dispatchers.Main) {
            ensureLoaded()
            val old = images[index]
            deleteFile(old.file)
            // largura inicial: imagem com cerca de 1/4 da altura da tela
            images[index] = sanitizeImage(
                old.copy(
                    file = file.absolutePath, enabled = true, imgW = w, imgH = h,
                    w = (0.25f * w / h * 9f / 16f).coerceIn(0.08f, 0.5f),
                    cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f
                )
            )
            saveImages()
        }
        true
    }

    /** Maior GIF aceito (o arquivo é copiado como está). */
    private const val MAX_GIF_BYTES = 30L * 1024 * 1024

    private fun isGif(context: Context, uri: Uri): Boolean {
        if (context.contentResolver.getType(uri)?.equals("image/gif", ignoreCase = true) == true) return true
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val head = ByteArray(4)
                input.read(head) == 4 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() &&
                    head[2] == 'F'.code.toByte() && head[3] == '8'.code.toByte()
            } ?: false
        }.getOrDefault(false)
    }

    private suspend fun importGif(context: Context, index: Int, uri: Uri): Boolean {
        val dir = File(context.filesDir, "stream_images").apply { mkdirs() }
        val file = File(dir, "gif_${index}_${System.currentTimeMillis()}.gif")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_GIF_BYTES) throw java.io.IOException("GIF too large")
                        out.write(buf, 0, n)
                    }
                }
                true
            } ?: false
        }.getOrDefault(false)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (copied) BitmapFactory.decodeFile(file.absolutePath, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (!copied || w <= 0 || h <= 0) {
            file.delete()
            return false
        }
        withContext(Dispatchers.Main) {
            ensureLoaded()
            val old = images[index]
            deleteFile(old.file)
            images[index] = sanitizeImage(
                old.copy(
                    file = file.absolutePath, enabled = true, imgW = w, imgH = h,
                    w = (0.25f * w / h * 9f / 16f).coerceIn(0.08f, 0.5f),
                    cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f
                )
            )
            saveImages()
        }
        return true
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val side = max(info.size.width, info.size.height)
                if (side > MAX_SIDE) {
                    val s = MAX_SIDE.toFloat() / side
                    decoder.setTargetSize(
                        (info.size.width * s).roundToInt().coerceAtLeast(1),
                        (info.size.height * s).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
}

/** Imagens já abertas (para não ler o arquivo de novo a cada tela). */
object StreamImageCache {
    private val cache = mutableStateMapOf<String, ImageBitmap>()

    fun get(path: String): ImageBitmap? = cache[path]

    suspend fun load(path: String): ImageBitmap? {
        cache[path]?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        } ?: return null
        cache[path] = bmp
        return bmp
    }

    fun forget(path: String) {
        cache.remove(path)
    }
}
