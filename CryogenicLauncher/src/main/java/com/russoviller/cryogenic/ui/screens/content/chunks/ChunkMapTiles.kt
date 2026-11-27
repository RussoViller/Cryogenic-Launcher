package com.russoviller.cryogenic.ui.screens.content.chunks

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.russoviller.cryogenic.game.chunks.RegionRenderer
import com.russoviller.cryogenic.game.chunks.unpackX
import com.russoviller.cryogenic.game.chunks.unpackZ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.BitSet
import java.util.concurrent.Executors
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/**
 * Imagens das regiões do mapa, feitas fora da thread da tela:
 * - 3 níveis de detalhe (512/256/128 px por região) conforme o zoom: com zoom longe, centenas
 *   de regiões cabem na tela sem estourar a memória;
 * - fila única com uma thread por núcleo livre (2 a 6), sempre a região visível mais perto do centro
 *   primeiro; regiões que saíram da tela antes da vez não são lidas;
 * - cache em disco (cacheDir/chunkmap) das regiões já lidas: abrir o mesmo mapa de novo é rápido;
 *   arquivo de região alterado (lastModified/tamanho) gera cache novo;
 * - limite de memória: tiles fora da tela saem primeiro, os visíveis nunca.
 * Tudo que mexe nos mapas internos roda na thread principal.
 */
internal class ChunkMapTiles(
    private val files: Map<Long, File>,
    private val dimensionId: String,
    cacheRoot: File
) {
    private class Tile(val lod: Int, val image: ImageBitmap, val bytes: Int)

    /** Uma região por núcleo livre (deixa 2 para a tela), entre 2 e 6 ao mesmo tempo. */
    private val threads = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 6)
    private val executor = Executors.newFixedThreadPool(threads) { r ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE)
            r.run()
        }, "cryo-chunkmap").apply { isDaemon = true }
    }
    private val worker = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cacheDir = File(cacheRoot, "chunkmap")

    private val tiles = HashMap<Long, Tile>()
    private val inhabited = HashMap<Long, LongArray>()
    private val inhabitedImages = HashMap<Long, ImageBitmap>()
    private val presenceImages = HashMap<Long, ImageBitmap>()
    private val wanted = HashMap<Long, Int>()
    private val running = HashSet<Long>()
    /** Regiões que não deu para ler: não ficam sendo pedidas de novo a cada quadro. */
    private val failed = HashSet<Long>()
    private val signal = Channel<Unit>(Channel.CONFLATED)
    private var memory = 0

    /** Muda quando uma imagem nova fica pronta (o mapa redesenha). */
    var revision by mutableIntStateOf(0)
        private set

    //estado do último desenho (usado para escolher a próxima região)
    private var visible: Set<Long> = emptySet()
    private var centerX = 0f
    private var centerZ = 0f

    init {
        repeat(threads) { scope.launch { workLoop() } }
    }

    fun dispose() {
        scope.cancel()
        signal.close()
        executor.shutdownNow()
    }

    fun inhabitedOf(key: Long): LongArray? = inhabited[key]

    /** Nível de detalhe para o zoom (px por bloco). */
    fun lodFor(scale: Float): Int = when {
        scale >= 0.75f -> 512
        scale >= 0.25f -> 256
        else -> 128
    }

    /** Registra o que está na tela e pede o que falta. Chamar a cada desenho. */
    fun frame(visibleKeys: Set<Long>, cx: Float, cz: Float, lod: Int) {
        visible = visibleKeys
        centerX = cx
        centerZ = cz
        var added = false
        wanted.keys.retainAll(visibleKeys)
        for (key in visibleKeys) {
            if (!files.containsKey(key) || key in failed) continue
            val tile = tiles[key]
            if (tile == null || tile.lod != lod) {
                if (wanted.put(key, lod) != lod) added = true
            }
        }
        if (added) signal.trySend(Unit)
    }

    /** Melhor imagem que já existe para a região (pode ser de outro nível enquanto carrega). */
    fun imageFor(key: Long): ImageBitmap? = tiles[key]?.image

    fun inhabitedImage(key: Long): ImageBitmap? = inhabitedImages[key]

    /** Chunks existentes em cinza (enquanto a região não foi lida): 1 px por chunk. */
    fun presenceImage(key: Long, bits: BitSet): ImageBitmap = presenceImages.getOrPut(key) {
        val px = IntArray(1024)
        var i = bits.nextSetBit(0)
        while (i in 0 until 1024) {
            px[i] = 0xFF232A36.toInt()
            i = bits.nextSetBit(i + 1)
        }
        Bitmap.createBitmap(px, 32, 32, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    private suspend fun workLoop() {
        while (true) {
            val next = pickNext()
            if (next == null) {
                if (signal.receiveCatching().isClosed) return
                continue
            }
            val (key, lod) = next
            running += key
            val file = files[key]
            val result = if (file == null) null else runCatching { withContext(worker) { load(file, lod) } }.getOrNull()
            running -= key
            if (result != null) {
                store(key, lod, result)
                if (wanted[key] == lod) wanted.remove(key)
            } else {
                failed += key
                wanted.remove(key)
            }
        }
    }

    /** Região pedida, visível e mais perto do centro da tela. */
    private fun pickNext(): Pair<Long, Int>? {
        var best: Long? = null
        var bestDist = Double.MAX_VALUE
        for ((key, _) in wanted) {
            if (key in running || key !in visible) continue
            val d = hypot((unpackX(key) * 512f + 256f - centerX).toDouble(), (unpackZ(key) * 512f + 256f - centerZ).toDouble())
            if (d < bestDist) {
                bestDist = d
                best = key
            }
        }
        val key = best ?: return null
        return key to wanted.getValue(key)
    }

    private class Loaded(val image: Bitmap, val inhabited: LongArray, val heat: Bitmap)

    private fun store(key: Long, lod: Int, r: Loaded) {
        tiles.remove(key)?.let { memory -= it.bytes }
        val bytes = r.image.byteCount
        tiles[key] = Tile(lod, r.image.asImageBitmap(), bytes)
        memory += bytes
        inhabited[key] = r.inhabited
        inhabitedImages[key] = r.heat.asImageBitmap()
        presenceImages.remove(key)
        trim()
        revision++
    }

    /** Solta tiles fora da tela (os mais longe primeiro) até caber no limite. */
    private fun trim() {
        if (memory <= MEMORY_LIMIT) return
        val candidates = tiles.keys.filter { it !in visible }.sortedByDescending {
            hypot((unpackX(it) * 512f - centerX).toDouble(), (unpackZ(it) * 512f - centerZ).toDouble())
        }
        for (key in candidates) {
            if (memory <= MEMORY_LIMIT) break
            tiles.remove(key)?.let { memory -= it.bytes }
            inhabitedImages.remove(key)
        }
    }

    // ------------------------------------------------------------------ leitura (thread de fundo)

    private fun load(file: File, lod: Int): Loaded {
        val cached = readCache(file)
        val pixels565: ShortArray
        val heat: LongArray
        if (cached != null) {
            pixels565 = cached.first
            heat = cached.second
        } else {
            val img = RegionRenderer.render(file, dimensionId)
            pixels565 = ShortArray(512 * 512) { i ->
                val c = img.pixels[i]
                ((((c shr 16) and 0xF8) shl 8) or (((c shr 8) and 0xFC) shl 3) or ((c and 0xF8) shr 3)).toShort()
            }
            heat = img.inhabited
            runCatching { writeCache(file, pixels565, heat) }
        }
        val full = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        full.copyPixelsFromBuffer(java.nio.ShortBuffer.wrap(pixels565))
        val image = if (lod >= 512) full else Bitmap.createScaledBitmap(full, lod, lod, true)
        if (image !== full) full.recycle()
        return Loaded(image, heat, heatBitmap(heat))
    }

    private fun heatBitmap(arr: LongArray): Bitmap {
        val px = IntArray(1024)
        for (i in 0 until 1024) {
            val t = arr[i]
            if (t <= 1200) continue
            val a = ((kotlin.math.ln(t / 1200.0) / 6.0).coerceIn(0.15, 0.7) * 255).toInt()
            px[i] = (a shl 24) or 0xFFA726
        }
        return Bitmap.createBitmap(px, 32, 32, Bitmap.Config.ARGB_8888)
    }

    private fun cacheFile(file: File): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(file.absolutePath.toByteArray())
        val id = digest.take(10).joinToString("") { "%02x".format(it) }
        return File(cacheDir, "${id}_${file.lastModified()}_${file.length()}.bin")
    }

    private fun readCache(file: File): Pair<ShortArray, LongArray>? = runCatching {
        val f = cacheFile(file)
        if (!f.isFile) return null
        DataInputStream(BufferedInputStream(InflaterInputStream(f.inputStream()), 65536)).use { input ->
            if (input.readInt() != CACHE_MAGIC) return null
            val heat = LongArray(1024) { input.readLong() }
            val raw = ByteArray(512 * 512 * 2)
            input.readFully(raw)
            val pixels = ShortArray(512 * 512)
            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pixels)
            f.setLastModified(System.currentTimeMillis())
            pixels to heat
        }
    }.getOrNull()

    private fun writeCache(file: File, pixels: ShortArray, heat: LongArray) {
        cacheDir.mkdirs()
        //versões antigas da mesma região saem
        val prefix = cacheFile(file).name.substringBefore('_') + "_"
        cacheDir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
        val target = cacheFile(file)
        val tmp = File(cacheDir, target.name + ".tmp")
        DataOutputStream(BufferedOutputStream(DeflaterOutputStream(tmp.outputStream(), Deflater(1)), 65536)).use { out ->
            out.writeInt(CACHE_MAGIC)
            heat.forEach { out.writeLong(it) }
            val raw = ByteBuffer.allocate(512 * 512 * 2).order(ByteOrder.LITTLE_ENDIAN)
            raw.asShortBuffer().put(pixels)
            out.write(raw.array())
        }
        tmp.renameTo(target)
        trimDisk()
    }

    /** Cache em disco até ~96 MB: os menos usados saem. */
    private fun trimDisk() {
        val list = cacheDir.listFiles()?.filter { it.name.endsWith(".bin") } ?: return
        var total = list.sumOf { it.length() }
        if (total <= DISK_LIMIT) return
        for (f in list.sortedBy { it.lastModified() }) {
            if (total <= DISK_LIMIT * 3 / 4) break
            total -= f.length()
            f.delete()
        }
    }

    companion object {
        private const val MEMORY_LIMIT = 48 * 1024 * 1024
        private const val DISK_LIMIT = 96L * 1024 * 1024
        private const val CACHE_MAGIC = 0x43524D36 //"CRM6" (muda quando o desenho muda)

        /**
         * Desenha só o pedaço visível de uma imagem que cobre uma região inteira: com zoom alto a
         * região teria milhares de pixels de largura; aqui o destino nunca passa do tamanho da tela.
         */
        fun DrawScope.drawRegion(
            image: ImageBitmap,
            rx: Int, rz: Int,
            left: Float, top: Float, right: Float, bottom: Float,
            scale: Float
        ) {
            val k = image.width / 512f
            val ox = rx * 512f
            val oz = rz * 512f
            val bx0 = maxOf(left, ox)
            val bz0 = maxOf(top, oz)
            val bx1 = minOf(right, ox + 512f)
            val bz1 = minOf(bottom, oz + 512f)
            if (bx1 <= bx0 || bz1 <= bz0) return
            val sx0 = floor((bx0 - ox) * k).toInt().coerceIn(0, image.width - 1)
            val sz0 = floor((bz0 - oz) * k).toInt().coerceIn(0, image.height - 1)
            val sx1 = ceil((bx1 - ox) * k).toInt().coerceIn(sx0 + 1, image.width)
            val sz1 = ceil((bz1 - oz) * k).toInt().coerceIn(sz0 + 1, image.height)
            val dx0 = ((ox + sx0 / k - left) * scale)
            val dz0 = ((oz + sz0 / k - top) * scale)
            val dx1 = ((ox + sx1 / k - left) * scale)
            val dz1 = ((oz + sz1 / k - top) * scale)
            val dl = floor(dx0).toInt()
            val dt = floor(dz0).toInt()
            drawImage(
                image,
                srcOffset = IntOffset(sx0, sz0),
                srcSize = IntSize(sx1 - sx0, sz1 - sz0),
                dstOffset = IntOffset(dl, dt),
                dstSize = IntSize(maxOf(1, ceil(dx1).toInt() - dl), maxOf(1, ceil(dz1).toInt() - dt)),
                filterQuality = FilterQuality.None
            )
        }
    }
}
