package com.russoviller.cryogenic.ui.screens.content.chunks

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.russoviller.cryogenic.game.seedmap.SeedBiomes
import com.russoviller.cryogenic.game.seedmap.SeedNative
import com.russoviller.cryogenic.game.seedmap.SeedStructure
import java.util.concurrent.Executors
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadFactory
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** De onde vêm os biomas e estruturas (nativo no aparelho, falso nos testes). */
interface SeedSource {
    /** Biomas na escala [scale] (1:scale), canto (x, z) já na escala. */
    fun biomes(scale: Int, x: Int, z: Int, w: Int, h: Int): IntArray?
    fun structures(type: Int, x0: Int, z0: Int, x1: Int, z1: Int, max: Int): IntArray?
    fun strongholds(count: Int): IntArray?
    /** Chamado na thread que vai parar de usar a fonte (libera o que for dela). */
    fun releaseThread() {}
}

/** Gerador nativo: um por thread (o do cubiomes não é seguro entre threads). */
class NativeSeedSource(
    private val mc: Int,
    private val seed: Long,
    private val dim: Int,
    private val large: Boolean,
    /** Altura em blocos para ver biomas de caverna; null = superfície. */
    caveY: Int? = null
) : SeedSource {
    private val handle = ThreadLocal<Long>()

    private fun h(): Long {
        handle.get()?.let { if (it != 0L) return it }
        val created = SeedNative.create(mc, seed, dim, large)
        handle.set(created)
        return created
    }

    //y na escala 1:4: 63/4 ≈ 15 (nível do mar) no Overworld; no Nether/End o meio do mundo
    private val y = when {
        caveY != null && dim == 0 -> Math.floorDiv(caveY, 4)
        dim == -1 || dim == 1 -> 16
        else -> 15
    }

    override fun biomes(scale: Int, x: Int, z: Int, w: Int, h: Int): IntArray? =
        h().takeIf { it != 0L }?.let { SeedNative.biomes(it, scale, x, z, w, h, y) }

    override fun structures(type: Int, x0: Int, z0: Int, x1: Int, z1: Int, max: Int): IntArray? =
        h().takeIf { it != 0L }?.let { SeedNative.structures(it, type, x0, z0, x1, z1, max) }

    override fun strongholds(count: Int): IntArray? =
        h().takeIf { it != 0L }?.let { SeedNative.strongholds(it, count) }

    override fun releaseThread() {
        val v = handle.get() ?: return
        handle.remove()
        if (v != 0L) SeedNative.free(v)
    }
}

/** Mancha de um bioma achado na busca: centro aproximado e tamanho (em blocos²). */
data class BiomeSpot(val biome: Int, val x: Int, val z: Int, val area: Int, val distance: Int)

/**
 * Mapa de biomas em blocos de 128×128 amostras, em 4 níveis (1:4, 1:16, 1:64, 1:256).
 * Fila única com prioridade para o que está perto do centro da tela.
 */
class SeedMapTiles(private val source: SeedSource) {
    private class Tile(val ids: IntArray) {
        var image: ImageBitmap? = null
        var filterVersion = -1
    }

    private data class TileKey(val lod: Int, val tx: Int, val tz: Int)
    private data class CellKey(val type: Int, val cx: Int, val cz: Int)

    private val lock = Object()
    private val tiles = object : LinkedHashMap<TileKey, Tile>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TileKey, Tile>?) = size > MAX_TILES
    }
    private val cells = object : LinkedHashMap<CellKey, IntArray>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CellKey, IntArray>?) = size > MAX_CELLS
    }
    private val pendingTiles = LinkedHashSet<TileKey>()
    private val pendingCells = LinkedHashSet<CellKey>()
    private val working = HashSet<Any>()
    private var strongholds: IntArray? = null
    private var strongholdsRequested = false
    private var centerX = 0f
    private var centerZ = 0f
    @Volatile private var closed = false
    private var searchTask: (() -> Unit)? = null

    /** Muda quando algo novo fica pronto (redesenha). Só muda na thread principal. */
    var version by mutableIntStateOf(0)
        private set

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private val bumpPosted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Avisa a tela (junta vários avisos seguidos num só redesenho). */
    private fun bump() {
        if (bumpPosted.compareAndSet(false, true)) {
            scope.launch {
                bumpPosted.set(false)
                version++
            }
        }
    }

    private var filter: Set<Int> = emptySet()
    private var filterVersion = 0

    private val threads = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 4)
    private val executor = Executors.newFixedThreadPool(threads, ThreadFactory { r ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            try { r.run() } finally { runCatching { source.releaseThread() } }
        }, "cryo-seedmap").apply { isDaemon = true }
    })

    init {
        repeat(threads) { executor.execute { workLoop() } }
    }

    fun setFilter(ids: Set<Int>) {
        if (ids == filter) return
        filter = ids
        filterVersion++
        version++
    }

    // ---------------------------------------------------------------- fila

    private fun workLoop() {
        while (!closed) {
            val job: Any = synchronized(lock) {
                var picked: Any? = searchTask?.also { searchTask = null }
                if (picked == null) picked = pickTile()
                if (picked == null) picked = pickCell()
                if (picked == null && strongholdsRequested && strongholds == null && "sh" !in working) picked = "sh"
                if (picked == null) {
                    try { lock.wait(400) } catch (_: InterruptedException) { return }
                    null
                } else {
                    working += picked
                    picked
                }
            } ?: continue
            try {
                run(job)
            } catch (_: Throwable) {
            } finally {
                synchronized(lock) { working -= job }
            }
        }
    }

    private fun pickTile(): TileKey? {
        if (pendingTiles.isEmpty()) return null
        var best: TileKey? = null
        var bestScore = Double.MAX_VALUE
        for (k in pendingTiles) {
            val size = tileBlocks(k.lod)
            val dx = (k.tx + 0.5) * size - centerX
            val dz = (k.tz + 0.5) * size - centerZ
            //níveis grossos primeiro (aparece algo rápido), depois o mais perto do centro
            val score = k.lod * -1e12 + dx * dx + dz * dz
            if (score < bestScore) { bestScore = score; best = k }
        }
        best?.let { pendingTiles -= it }
        return best
    }

    private fun pickCell(): CellKey? {
        if (pendingCells.isEmpty()) return null
        var best: CellKey? = null
        var bestScore = Double.MAX_VALUE
        for (k in pendingCells) {
            val dx = (k.cx + 0.5) * CELL - centerX
            val dz = (k.cz + 0.5) * CELL - centerZ
            val score = dx * dx + dz * dz
            if (score < bestScore) { bestScore = score; best = k }
        }
        best?.let { pendingCells -= it }
        return best
    }

    @Suppress("UNCHECKED_CAST")
    private fun run(job: Any) {
        when (job) {
            is TileKey -> {
                val scale = SCALES[job.lod]
                val ids = source.biomes(scale, job.tx * TILE, job.tz * TILE, TILE, TILE) ?: IntArray(TILE * TILE) { -1 }
                synchronized(lock) { tiles[job] = Tile(ids) }
                bump()
            }
            is CellKey -> {
                val x0 = job.cx * CELL
                val z0 = job.cz * CELL
                val pos = source.structures(job.type, x0, z0, x0 + CELL - 1, z0 + CELL - 1, 512) ?: IntArray(0)
                synchronized(lock) { cells[job] = pos }
                bump()
            }
            "sh" -> {
                val pos = source.strongholds(128) ?: IntArray(0)
                synchronized(lock) { strongholds = pos }
                bump()
            }
            is Function0<*> -> (job as () -> Unit)()
        }
    }

    // ---------------------------------------------------------------- busca de biomas

    /**
     * Procura manchas dos biomas [ids] num raio em volta de (x, z), na escala 1:16.
     * [onDone] roda na thread principal.
     */
    fun search(ids: Set<Int>, x: Int, z: Int, radius: Int, onDone: (List<BiomeSpot>) -> Unit) {
        val task: () -> Unit = task@{
            if (ids.isEmpty()) { scope.launch { onDone(emptyList()) }; return@task }
            val scale = 16
            val n = (radius * 2 / scale).coerceIn(16, 1024)
            val ox = Math.floorDiv(x - radius, scale)
            val oz = Math.floorDiv(z - radius, scale)
            val grid = IntArray(n * n)
            //em faixas, para não pedir um bloco enorme de memória nativa de uma vez
            val band = 64
            var row = 0
            while (row < n) {
                val h = min(band, n - row)
                val part = source.biomes(scale, ox, oz + row, n, h)
                if (part != null) System.arraycopy(part, 0, grid, row * n, n * h)
                row += h
            }
            val spots = findSpots(grid, n, ids, ox, oz, scale, x, z)
            scope.launch { onDone(spots) }
        }
        synchronized(lock) {
            searchTask = task
            lock.notifyAll()
        }
    }

    // ---------------------------------------------------------------- desenho

    fun lodFor(scale: Float): Int = when {
        scale >= 0.25f -> 0
        scale >= 1f / 16f -> 1
        scale >= 1f / 64f -> 2
        else -> 3
    }

    /**
     * Desenha os biomas da área visível. [s] = px por bloco; (left, top) = bloco no canto.
     */
    fun draw(scope: DrawScope, left: Float, top: Float, s: Float) {
        val w = scope.size.width
        val h = scope.size.height
        val right = left + w / s
        val bottom = top + h / s
        val lod = lodFor(s)
        synchronized(lock) {
            centerX = (left + right) / 2f
            centerZ = (top + bottom) / 2f
            pendingTiles.clear()
            //do mais grosso ao mais fino: o fino cobre o grosso quando fica pronto
            for (l in 3 downTo lod) {
                val size = tileBlocks(l)
                val tx1 = floor(left / size).toInt()
                val tx2 = floor(right / size).toInt()
                val tz1 = floor(top / size).toInt()
                val tz2 = floor(bottom / size).toInt()
                for (tz in tz1..tz2) for (tx in tx1..tx2) {
                    val key = TileKey(l, tx, tz)
                    val tile = tiles[key]
                    if (tile == null) {
                        //só pede o nível certo e um mais grosso (mais grossos que isso ficariam muitos)
                        if ((l == lod || l == min(3, lod + 1)) && key !in working) pendingTiles += key
                        continue
                    }
                    val img = imageOf(tile)
                    drawClipped(scope, img, tx * size.toFloat(), tz * size.toFloat(), size.toFloat(), left, top, right, bottom, s)
                }
            }
            if (pendingTiles.isNotEmpty()) lock.notifyAll()
        }
    }

    private fun imageOf(tile: Tile): ImageBitmap {
        val cached = tile.image
        if (cached != null && tile.filterVersion == filterVersion) return cached
        val f = filter
        val px = IntArray(TILE * TILE)
        for (i in px.indices) {
            val id = tile.ids[i]
            px[i] = when {
                id < 0 -> 0xFF0B0F17.toInt()
                f.isEmpty() -> SeedBiomes.color(id)
                id in f -> SeedBiomes.color(id)
                else -> 0xFFF2F4F7.toInt()
            }
        }
        val img = Bitmap.createBitmap(px, TILE, TILE, Bitmap.Config.ARGB_8888).asImageBitmap()
        tile.image = img
        tile.filterVersion = filterVersion
        return img
    }

    /** Desenha só a parte do tile que está na tela (destinos gigantes travam o desenho). */
    private fun drawClipped(
        scope: DrawScope, img: ImageBitmap, bx: Float, bz: Float, size: Float,
        left: Float, top: Float, right: Float, bottom: Float, s: Float
    ) {
        val cx1 = max(bx, left)
        val cz1 = max(bz, top)
        val cx2 = min(bx + size, right)
        val cz2 = min(bz + size, bottom)
        if (cx2 <= cx1 || cz2 <= cz1) return
        val per = size / TILE //blocos por pixel do tile
        val sx1 = floor((cx1 - bx) / per).toInt().coerceIn(0, TILE - 1)
        val sz1 = floor((cz1 - bz) / per).toInt().coerceIn(0, TILE - 1)
        val sx2 = ceil((cx2 - bx) / per).toInt().coerceIn(sx1 + 1, TILE)
        val sz2 = ceil((cz2 - bz) / per).toInt().coerceIn(sz1 + 1, TILE)
        val dx1 = ((bx + sx1 * per - left) * s).roundToInt()
        val dz1 = ((bz + sz1 * per - top) * s).roundToInt()
        val dx2 = ((bx + sx2 * per - left) * s).roundToInt()
        val dz2 = ((bz + sz2 * per - top) * s).roundToInt()
        scope.drawImage(
            image = img,
            srcOffset = IntOffset(sx1, sz1),
            srcSize = IntSize(sx2 - sx1, sz2 - sz1),
            dstOffset = IntOffset(dx1, dz1),
            dstSize = IntSize(max(1, dx2 - dx1), max(1, dz2 - dz1)),
            filterQuality = FilterQuality.None
        )
    }

    /**
     * Estruturas visíveis dos tipos pedidos: lista de (estrutura, x, z). Pede as células que faltam.
     */
    fun structuresIn(types: Collection<SeedStructure>, left: Float, top: Float, right: Float, bottom: Float, s: Float): List<Triple<SeedStructure, Int, Int>> {
        val out = ArrayList<Triple<SeedStructure, Int, Int>>()
        synchronized(lock) {
            pendingCells.clear()
            for (st in types) {
                if (s < st.minZoom) continue
                if (st == SeedStructure.Stronghold) {
                    if (!strongholdsRequested) { strongholdsRequested = true; lock.notifyAll() }
                    val pos = strongholds ?: continue
                    var i = 0
                    while (i + 1 < pos.size) {
                        val x = pos[i]; val z = pos[i + 1]
                        if (x >= left - 64 && x <= right + 64 && z >= top - 64 && z <= bottom + 64) out += Triple(st, x, z)
                        i += 2
                    }
                    continue
                }
                val cx1 = floor(left / CELL).toInt()
                val cx2 = floor(right / CELL).toInt()
                val cz1 = floor(top / CELL).toInt()
                val cz2 = floor(bottom / CELL).toInt()
                if ((cx2 - cx1 + 1) * (cz2 - cz1 + 1) > 400) continue //longe demais: não pede milhares
                for (cz in cz1..cz2) for (cx in cx1..cx2) {
                    val key = CellKey(st.type, cx, cz)
                    val pos = cells[key]
                    if (pos == null) {
                        if (key !in working) pendingCells += key
                        continue
                    }
                    var i = 0
                    while (i + 1 < pos.size) {
                        out += Triple(st, pos[i], pos[i + 1])
                        i += 2
                    }
                }
            }
            if (pendingCells.isNotEmpty()) lock.notifyAll()
        }
        return out
    }

    /** Pontos já calculados das fortalezas (strongholds), para a lista. */
    fun strongholdList(): IntArray? = synchronized(lock) { strongholds }

    fun isBusy(): Boolean = synchronized(lock) { pendingTiles.isNotEmpty() || working.isNotEmpty() }

    fun dispose() {
        closed = true
        scope.cancel()
        synchronized(lock) { lock.notifyAll() }
        executor.shutdown()
    }

    companion object {
        const val TILE = 128
        /** Tamanho de uma célula de busca de estruturas, em blocos. */
        const val CELL = 2048
        private const val MAX_TILES = 320
        private const val MAX_CELLS = 2000
        private val SCALES = intArrayOf(4, 16, 64, 256)

        fun tileBlocks(lod: Int) = TILE * SCALES[lod]

        /**
         * Manchas conectadas (8 vizinhos) dos biomas pedidos numa grade [n]×[n] de amostras,
         * cada uma com [scale] blocos. Devolve do mais perto ao mais longe de (cx, cz).
         */
        fun findSpots(grid: IntArray, n: Int, ids: Set<Int>, ox: Int, oz: Int, scale: Int, cx: Int, cz: Int): List<BiomeSpot> {
            val seen = BooleanArray(n * n)
            val stack = IntArray(n * n)
            val out = ArrayList<BiomeSpot>()
            for (start in 0 until n * n) {
                if (seen[start] || grid[start] !in ids) continue
                val biome = grid[start]
                var sp = 0
                stack[sp++] = start
                seen[start] = true
                var count = 0
                var sumX = 0L
                var sumZ = 0L
                while (sp > 0) {
                    val i = stack[--sp]
                    val x = i % n
                    val z = i / n
                    count++
                    sumX += x
                    sumZ += z
                    for (dz in -1..1) for (dx in -1..1) {
                        if (dx == 0 && dz == 0) continue
                        val nx = x + dx
                        val nz = z + dz
                        if (nx < 0 || nz < 0 || nx >= n || nz >= n) continue
                        val j = nz * n + nx
                        if (!seen[j] && grid[j] == biome) {
                            seen[j] = true
                            stack[sp++] = j
                        }
                    }
                }
                //centro da mancha; se cair fora do bioma (mancha em "C"), usa a amostra mais perto do centro
                var mx = (sumX / count).toInt()
                var mz = (sumZ / count).toInt()
                if (grid[mz * n + mx] != biome) {
                    var bestD = Long.MAX_VALUE
                    for (i in 0 until n * n) {
                        if (grid[i] != biome) continue
                        val x = i % n; val z = i / n
                        val d = (x - mx).toLong() * (x - mx) + (z - mz).toLong() * (z - mz)
                        if (d < bestD) { bestD = d; if (d == 0L) break }
                    }
                    //acha de novo o ponto com essa distância dentro da mesma mancha (aproximação simples)
                    for (i in 0 until n * n) {
                        if (grid[i] != biome) continue
                        val x = i % n; val z = i / n
                        if ((x - mx).toLong() * (x - mx) + (z - mz).toLong() * (z - mz) == bestD) { mx = x; mz = z; break }
                    }
                }
                val bx = (ox + mx) * scale + scale / 2
                val bz = (oz + mz) * scale + scale / 2
                val dist = Offset((bx - cx).toFloat(), (bz - cz).toFloat()).getDistance().roundToInt()
                out += BiomeSpot(biome, bx, bz, count * scale * scale, dist)
            }
            out.sortBy { it.distance }
            return out
        }
    }
}
