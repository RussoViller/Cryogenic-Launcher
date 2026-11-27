package com.russoviller.cryogenic.game.chunks

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.BitSet
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/**
 * Desenha uma região vista de cima, como o MCA Selector: para cada coluna pega o bloco do topo
 * (pelo mapa de altura WORLD_SURFACE) e pinta com a cor do bloco, com grama, folhas e água
 * tingidas pelo bioma, água mais escura conforme a profundidade e relevo sombreado.
 * Formatos: 1.18+ (sections/block_states), 1.13–1.17 (Level.Sections/Palette) e antigos com
 * IDs numéricos (Level.Sections/Blocks). Chunk sem blocos legíveis cai no modo por altura.
 */
object RegionRenderer {
    private val inflater = ThreadLocal.withInitial { Inflater() }
    private val buffer = ThreadLocal.withInitial { ByteArray(256 * 1024) }

    /** Tags grandes que não entram no mapa (luz, entidades, ticks...). */
    private val TREE_SKIP = setOf(
        "block_entities", "Entities", "TileEntities", "entities", "structures", "Structures",
        "block_ticks", "fluid_ticks", "TileTicks", "LiquidTicks", "PostProcessing", "Lights", "CarvingMasks",
        "SkyLight", "BlockLight", "ToBeTicked", "LiquidsToBeTicked", "UpgradeData", "blending_data",
        "below_zero_retrogen", "Heightmaps_unused"
    )

    fun render(file: File, dimensionId: String): RegionImage {
        val pixels = IntArray(512 * 512) { EMPTY_COLOR }
        val inhabited = LongArray(1024) { -1L }
        val presence = BitSet(1024)
        val kind = when {
            dimensionId.contains("nether") -> 1
            dimensionId.contains("the_end") -> 2
            else -> 0
        }
        val water = WaterLayer()
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4096)
                raf.readFully(header)
                val length = raf.length()
                for (i in 0 until 1024) {
                    val loc = ChunkWorld.readInt(header, i * 4)
                    if (loc == 0) continue
                    presence.set(i)
                    val chunk = runCatching { readChunkBytes(raf, loc, length, file, i)?.let { ChunkView.parse(it, TREE_SKIP) } }.getOrNull()
                    inhabited[i] = chunk?.inhabited ?: -1L
                    runCatching { paintChunk(pixels, water, i and 31, i shr 5, chunk, kind) }
                        .onFailure { paintUnknown(pixels, i and 31, i shr 5) }
                }
            }
        }
        water.compose(pixels)
        return RegionImage(pixels, inhabited, presence)
    }

    /** Só o tempo habitado de cada chunk (para o filtro "pouco visitados"). */
    fun readInhabited(file: File): LongArray {
        val out = LongArray(1024) { -1L }
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4096)
                raf.readFully(header)
                val length = raf.length()
                for (i in 0 until 1024) {
                    val loc = ChunkWorld.readInt(header, i * 4)
                    if (loc == 0) continue
                    out[i] = runCatching {
                        val data = readChunkBytes(raf, loc, length, file, i) ?: return@runCatching 0L
                        var t = 0L
                        NbtScanner(data).visit(
                            onLong = { _, name, v -> if (name == "InhabitedTime") t = v },
                            skipCompound = { _, name -> name in SKIPPED_TAGS }
                        )
                        t
                    }.getOrNull() ?: 0L
                }
            }
        }
        return out
    }

    /**
     * NBT descomprimido do chunk. Aceita gzip (1), zlib (2), sem compressão (3) e LZ4 (4, opção
     * region-file-compression do servidor). Chunk grande demais para a região (bit 128) fica num
     * arquivo c.X.Z.mcc ao lado: lido de lá.
     */
    /** NBT descomprimido de um chunk (índice 0..1023 dentro da região), ou null. */
    internal fun chunkBytes(file: File, index: Int): ByteArray? = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(4096)
            raf.readFully(header)
            val loc = ChunkWorld.readInt(header, index * 4)
            if (loc == 0) null else readChunkBytes(raf, loc, raf.length(), file, index)
        }
    }.getOrNull()

    /** Tipo de compressão gravado no chunk (1 gzip, 2 zlib, 3 nenhuma, 4 lz4; +128 = arquivo .mcc). */
    internal fun chunkCompression(file: File, index: Int): Int = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(4096)
            raf.readFully(header)
            val loc = ChunkWorld.readInt(header, index * 4)
            if (loc == 0) return@use -1
            raf.seek((loc ushr 8).toLong() * 4096L + 4)
            raf.readUnsignedByte()
        }
    }.getOrDefault(-1)

    private fun readChunkBytes(raf: RandomAccessFile, loc: Int, fileLength: Long, region: File, index: Int): ByteArray? {
        val offset = (loc ushr 8).toLong() * 4096L
        if (offset + 5 > fileLength) return null
        raf.seek(offset)
        val len = raf.readInt() - 1
        val compression = raf.readUnsignedByte()
        if (compression and 128 != 0) {
            val (rx, rz) = regionCoords(region) ?: return null
            val ext = File(region.parentFile, "c.${rx * 32 + (index and 31)}.${rz * 32 + (index shr 5)}.mcc")
            if (!ext.isFile || ext.length() > 64L * 1024 * 1024) return null
            return decompress(compression and 127, ext.readBytes())
        }
        if (len <= 0 || offset + 5 + len > fileLength || len > 8 * 1024 * 1024) return null
        val raw = ByteArray(len)
        raf.readFully(raw)
        return decompress(compression, raw)
    }

    private fun decompress(compression: Int, raw: ByteArray): ByteArray? = when (compression) {
        1 -> GZIPInputStream(ByteArrayInputStream(raw)).use { it.readBytes() }
        2 -> inflate(raw)
        3 -> raw
        4 -> Lz4Block.decode(raw)
        else -> null
    }

    private fun regionCoords(file: File): Pair<Int, Int>? {
        val parts = file.name.split('.')
        if (parts.size != 4) return null
        val x = parts[1].toIntOrNull() ?: return null
        val z = parts[2].toIntOrNull() ?: return null
        return x to z
    }

    private fun inflate(raw: ByteArray): ByteArray {
        val inf = inflater.get()!!
        inf.reset()
        inf.setInput(raw)
        var out = buffer.get()!!
        var size = 0
        while (!inf.finished()) {
            if (size == out.size) {
                out = out.copyOf(out.size * 2)
                buffer.set(out)
            }
            val n = inf.inflate(out, size, out.size - size)
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
            size += n
        }
        return out.copyOf(size)
    }

    // ------------------------------------------------------------------ pintura

    private fun paintUnknown(pixels: IntArray, cx: Int, cz: Int) {
        for (bz in 0 until 16) for (bx in 0 until 16) {
            pixels[(cz * 16 + bz) * 512 + cx * 16 + bx] = if ((bx + bz) % 4 == 0) 0xFF3A4150.toInt() else 0xFF2C323E.toInt()
        }
    }

    private fun paintChunk(pixels: IntArray, water: WaterLayer, cx: Int, cz: Int, c: ChunkView?, kind: Int) {
        if (c == null || (!c.hasBlocks && c.surface == null)) {
            paintUnknown(pixels, cx, cz)
            return
        }
        //altura do bloco mostrado em cada coluna (para o relevo)
        val shownY = IntArray(256)
        val colors = IntArray(256)
        val isWater = BooleanArray(256)
        for (i in 0 until 256) {
            val x = i and 15
            val z = i shr 4
            if (!c.hasBlocks) {
                //chunk incompleto: só o mapa de altura
                val top = c.surface!![i] - 1
                shownY[i] = top
                colors[i] = if (top < c.minY) EMPTY_COLOR else HeightColors.color(kind, top, c.floor?.get(i)?.minus(1) ?: top)
                continue
            }
            //o topo vem dos blocos (não do mapa de altura, que pode estar velho em mundo atualizado)
            val y = if (kind == 1) netherFloor(c, x, z) else c.topVisible(x, z)
            if (y == Int.MIN_VALUE) {
                //blocos não legíveis nessa coluna: usa o mapa de altura, se tiver (nunca fica preto à toa)
                val top = c.surface?.get(i)?.minus(1)
                if (top != null && top >= c.minY && kind != 2) {
                    shownY[i] = top
                    colors[i] = HeightColors.color(kind, top, c.floor?.get(i)?.minus(1) ?: top)
                } else {
                    shownY[i] = c.minY
                    colors[i] = if (kind == 2) 0xFF07060C.toInt() else EMPTY_COLOR
                }
                continue
            }
            val name = c.block(x, y, z) ?: "air"
            shownY[i] = y
            if (BlockColors.isWater(name)) {
                var d = y
                val limit = y - 64
                while (d > c.minY && d > limit) {
                    val below = c.block(x, d - 1, z) ?: break
                    if (!BlockColors.isWater(below) && !BlockColors.isInvisible(below)) break
                    d--
                }
                val floorName = c.block(x, d - 1, z)
                val floor = floorName?.let { BlockColors.color(it, c.biome(x, d - 1, z)) } ?: 0xFF5A5040.toInt()
                water.set((cz * 16 + z) * 512 + cx * 16 + x, BlockColors.water(c.biome(x, y, z)), floor, y - d + 1)
                isWater[i] = true
            } else if (BlockColors.isTranslucent(name)) {
                //vidro colorido: mistura com o que está embaixo dele
                val glass = BlockColors.color(name, c.biome(x, y, z))
                var d = y - 1
                var below: String? = null
                while (d >= c.minY && d > y - 32) {
                    val n = c.block(x, d, z) ?: break
                    if (!BlockColors.isInvisible(n) && !BlockColors.isTranslucent(n)) { below = n; break }
                    d--
                }
                colors[i] = if (below == null) glass
                else blend(BlockColors.color(below, c.biome(x, d, z)), glass, 0.55f)
            } else {
                colors[i] = BlockColors.color(name, c.biome(x, y, z))
            }
        }
        for (i in 0 until 256) {
            if (isWater[i]) continue
            val x = i and 15
            val z = i shr 4
            val h = shownY[i]
            val north = if (z > 0) shownY[i - 16] else h
            val west = if (x > 0) shownY[i - 1] else h
            val diff = (h - north) + (h - west)
            val shade = when {
                diff > 0 -> (1f + diff * 0.05f).coerceAtMost(1.18f)
                diff < 0 -> (1f + diff * 0.07f).coerceAtLeast(0.72f)
                else -> 1f
            }
            pixels[(cz * 16 + z) * 512 + cx * 16 + x] = if (shade == 1f) colors[i] else darken(colors[i], shade)
        }
    }

    /** Nether: abaixo do teto de bedrock, procura o primeiro espaço aberto e mostra o chão dele. */
    private fun netherFloor(c: ChunkView, x: Int, z: Int): Int {
        val top = c.maxY
        if (top == Int.MIN_VALUE) return Int.MIN_VALUE
        var y = minOf(top, 110)
        var air = false
        while (y >= c.minY) {
            val name = c.block(x, y, z) ?: return Int.MIN_VALUE
            val empty = BlockColors.isInvisible(name)
            if (!air) {
                if (empty) air = true
            } else if (!empty) return y
            y--
        }
        return minOf(top, 110)
    }

    private fun darken(c: Int, k: Float): Int {
        val r = (((c shr 16) and 0xFF) * k).toInt().coerceIn(0, 255)
        val g = (((c shr 8) and 0xFF) * k).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) * k).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** a = quanto de [top] por cima de [base]. */
    private fun blend(base: Int, top: Int, a: Float): Int {
        fun ch(v: Int, s: Int) = (v shr s) and 0xFF
        val r = (ch(base, 16) * (1 - a) + ch(top, 16) * a).toInt()
        val g = (ch(base, 8) * (1 - a) + ch(top, 8) * a).toInt()
        val b = (ch(base, 0) * (1 - a) + ch(top, 0) * a).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}

/**
 * Água da região inteira, montada no fim: a cor de cada bioma é suavizada com as vizinhas (como
 * a mistura de biomas do jogo) e a profundidade também, para não ficar com degraus e bordas
 * retas entre chunks/biomas.
 */
private class WaterLayer {
    private var mask: BooleanArray? = null
    private lateinit var tint: IntArray
    private lateinit var floor: IntArray
    private lateinit var depth: FloatArray

    fun set(index: Int, waterColor: Int, floorColor: Int, d: Int) {
        if (mask == null) {
            mask = BooleanArray(SIZE)
            tint = IntArray(SIZE)
            floor = IntArray(SIZE)
            depth = FloatArray(SIZE)
        }
        mask!![index] = true
        //metade da cor do bioma, metade do azul padrão: biomas vizinhos não contrastam tanto
        tint[index] = mix(waterColor, BASE_WATER, 0.45f)
        floor[index] = floorColor
        depth[index] = d.toFloat()
    }

    fun compose(pixels: IntArray) {
        val m = mask ?: return
        val r = FloatArray(SIZE)
        val g = FloatArray(SIZE)
        val b = FloatArray(SIZE)
        val w = FloatArray(SIZE)
        val dep = FloatArray(SIZE)
        val dw = FloatArray(SIZE)
        for (i in 0 until SIZE) if (m[i]) {
            val c = tint[i]
            r[i] = ((c shr 16) and 0xFF).toFloat()
            g[i] = ((c shr 8) and 0xFF).toFloat()
            b[i] = (c and 0xFF).toFloat()
            w[i] = 1f
            dep[i] = depth[i]
            dw[i] = 1f
        }
        //cor: raio 7 blocos; profundidade: raio 2 (tira os degraus sem apagar o fundo)
        boxBlur(arrayOf(r, g, b, w), 7)
        boxBlur(arrayOf(dep, dw), 2)
        for (i in 0 until SIZE) {
            if (!m[i]) continue
            val wt = w[i].coerceAtLeast(1e-3f)
            val d = (dep[i] / dw[i].coerceAtLeast(1e-3f)).coerceAtLeast(1f)
            val color = (0xFF shl 24) or ((r[i] / wt).toInt().coerceIn(0, 255) shl 16) or
                ((g[i] / wt).toInt().coerceIn(0, 255) shl 8) or (b[i] / wt).toInt().coerceIn(0, 255)
            //raso mostra o fundo; o fundo some e a água escurece aos poucos com a profundidade
            val t = ((d - 1f) / 30f).coerceIn(0f, 1f)
            val a = 0.5f + 0.45f * (1f - (1f - t) * (1f - t))
            val dark = 1f - 0.38f * (d / 48f).coerceIn(0f, 1f)
            pixels[i] = mix(floor[i], scale(color, dark), a)
        }
    }

    /** Desfoque de caixa separável (soma da janela), em cada canal. */
    private fun boxBlur(ch: Array<FloatArray>, radius: Int) {
        val tmp = FloatArray(512)
        for (arr in ch) {
            for (z in 0 until 512) {
                val o = z * 512
                var sum = 0f
                for (x in -radius..radius) sum += arr[o + x.coerceIn(0, 511)]
                for (x in 0 until 512) {
                    tmp[x] = sum
                    sum += arr[o + (x + radius + 1).coerceAtMost(511)] - arr[o + (x - radius).coerceAtLeast(0)]
                }
                System.arraycopy(tmp, 0, arr, o, 512)
            }
            for (x in 0 until 512) {
                var sum = 0f
                for (z in -radius..radius) sum += arr[z.coerceIn(0, 511) * 512 + x]
                for (z in 0 until 512) {
                    tmp[z] = sum
                    sum += arr[(z + radius + 1).coerceAtMost(511) * 512 + x] - arr[(z - radius).coerceAtLeast(0) * 512 + x]
                }
                for (z in 0 until 512) arr[z * 512 + x] = tmp[z]
            }
        }
    }

    companion object {
        private const val SIZE = 512 * 512
        private const val BASE_WATER = 0xFF3F76E4.toInt()

        fun mix(a: Int, b: Int, t: Float): Int {
            fun ch(v: Int, s: Int) = (v shr s) and 0xFF
            val r = (ch(a, 16) + (ch(b, 16) - ch(a, 16)) * t).toInt().coerceIn(0, 255)
            val g = (ch(a, 8) + (ch(b, 8) - ch(a, 8)) * t).toInt().coerceIn(0, 255)
            val bl = (ch(a, 0) + (ch(b, 0) - ch(a, 0)) * t).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
        }

        fun scale(c: Int, k: Float): Int = mix(0xFF000000.toInt(), c, k)
    }
}

/** Cores pela altura, para chunks sem blocos legíveis (incompletos). */
private object HeightColors {
    fun color(kind: Int, h: Int, floor: Int): Int = when (kind) {
        1 -> mix(0xFF461010.toInt(), 0xFFC84628.toInt(), (h - 30) / 100f)
        2 -> if (h <= 0) 0xFF07060C.toInt() else mix(0xFFA09670.toInt(), 0xFFE8E2AA.toInt(), (h - 40) / 60f)
        else -> if (h - floor >= 1) mix(0xFF4078DC.toInt(), 0xFF122664.toInt(), (h - floor) / 24f)
        else mix(0xFF6E9E4C.toInt(), 0xFF8C8C8E.toInt(), (h - 64) / 100f)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(v: Int, s: Int) = (v shr s) and 0xFF
        val r = (ch(a, 16) + (ch(b, 16) - ch(a, 16)) * k).toInt()
        val g = (ch(a, 8) + (ch(b, 8) - ch(a, 8)) * k).toInt()
        val bl = (ch(a, 0) + (ch(b, 0) - ch(a, 0)) * k).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}

/** Blocos e biomas de um chunk, lidos do NBT. */
internal class ChunkView(
    val minY: Int,
    val surface: IntArray?,
    val floor: IntArray?,
    val inhabited: Long,
    private val sections: Map<Int, BlockSection>,
    private val biomeSections: Map<Int, BiomeSection>,
    private val oldBiomes: IntArray?
) {
    val hasBlocks get() = sections.isNotEmpty()
    private val sectionYs = sections.keys.sortedDescending().toIntArray()
    /** Bloco mais alto possível (topo da seção salva mais alta). */
    val maxY: Int get() = if (sectionYs.isEmpty()) Int.MIN_VALUE else sectionYs[0] * 16 + 15

    /**
     * Primeiro bloco visível de cima para baixo, lendo os blocos (seção inteira de ar é pulada
     * de uma vez). Int.MIN_VALUE = coluna vazia.
     */
    fun topVisible(x: Int, z: Int): Int {
        for (sy in sectionYs) {
            val s = sections.getValue(sy)
            val only = s.uniform()
            if (only != null && BlockColors.isInvisible(only)) continue
            val ly = s.topVisible(x, z)
            if (ly >= 0) return sy * 16 + ly
        }
        return Int.MIN_VALUE
    }

    /** Nome do bloco (sem "minecraft:"); null se a seção não existe (nada salvo ali = ar). */
    fun block(x: Int, y: Int, z: Int): String? {
        val s = sections[Math.floorDiv(y, 16)] ?: return if (sections.isEmpty()) null else "air"
        return s.get(x, y and 15, z)
    }

    fun biome(x: Int, y: Int, z: Int): String {
        if (biomeSections.isNotEmpty()) {
            val s = biomeSections[Math.floorDiv(y, 16)] ?: biomeSections.values.first()
            return s.get(x shr 2, (y and 15) shr 2, z shr 2)
        }
        val old = oldBiomes ?: return "plains"
        val id = when (old.size) {
            1024 -> old[((y.coerceIn(0, 255) shr 2) shl 4) or ((z shr 2) shl 2) or (x shr 2)]
            256 -> old[z * 16 + x]
            else -> return "plains"
        }
        return BlockColors.legacyBiome(id)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun parse(data: ByteArray, skip: Set<String>): ChunkView {
            val root = NbtTree.read(data, skip)
            val dataVersion = (root["DataVersion"] as? Int) ?: 0
            val level = root["Level"] as? Map<String, Any?>
            val base = level ?: root
            val inhabited = (base["InhabitedTime"] as? Long) ?: 0L
            val yPos = (root["yPos"] as? Int) ?: (level?.get("yPos") as? Int)
            val minY = when {
                yPos != null -> yPos * 16
                dataVersion >= 2860 -> -64
                else -> 0
            }
            val heightmaps = base["Heightmaps"] as? Map<String, Any?>
            val surface = (heightmaps?.get("WORLD_SURFACE") as? LongArray)?.let { unpackHeights(it, minY) }
                ?: (base["HeightMap"] as? IntArray)?.takeIf { it.size == 256 }
            val floor = (heightmaps?.get("OCEAN_FLOOR") as? LongArray)?.let { unpackHeights(it, minY) }

            val sections = HashMap<Int, BlockSection>()
            val biomeSections = HashMap<Int, BiomeSection>()
            val list = (root["sections"] ?: base["Sections"]) as? List<Any?>
            //1.16 (2529) em diante os valores não atravessam os longs
            val padded = dataVersion >= 2529
            list?.forEach { any ->
                val sec = any as? Map<String, Any?> ?: return@forEach
                val y = (sec["Y"] as? Number)?.toInt() ?: return@forEach
                val states = sec["block_states"] as? Map<String, Any?>
                val palette = (states?.get("palette") ?: sec["Palette"]) as? List<Any?>
                if (palette != null && palette.isNotEmpty()) {
                    val names = Array(palette.size) { i -> paletteName(palette[i]) }
                    val longs = (states?.get("data") ?: sec["BlockStates"]) as? LongArray
                    sections[y] = BlockSection.Palette(names, longs, states != null || padded)
                } else {
                    val blocks = sec["Blocks"] as? ByteArray
                    if (blocks != null && blocks.size == 4096) {
                        sections[y] = BlockSection.Legacy(blocks, sec["Data"] as? ByteArray)
                    }
                }
                val biomes = sec["biomes"] as? Map<String, Any?>
                val bp = biomes?.get("palette") as? List<Any?>
                if (bp != null && bp.isNotEmpty()) {
                    biomeSections[y] = BiomeSection(
                        Array(bp.size) { (bp[it] as? String)?.removePrefix("minecraft:") ?: "plains" },
                        biomes["data"] as? LongArray
                    )
                }
            }
            val oldBiomes = when (val b = base["Biomes"]) {
                is IntArray -> b
                is ByteArray -> IntArray(b.size) { b[it].toInt() and 0xFF }
                else -> null
            }
            return ChunkView(minY, surface, floor, inhabited, sections, biomeSections, oldBiomes)
        }

        /** Nome do bloco de uma entrada da paleta (compound com Name, ou texto "minecraft:x[props]"). */
        @Suppress("UNCHECKED_CAST")
        private fun paletteName(entry: Any?): String {
            val raw = when (entry) {
                is Map<*, *> -> (entry["Name"] ?: entry["name"] ?: entry["id"] ?: entry[""]) as? String
                is String -> entry
                else -> null
            } ?: return "air"
            return raw.substringBefore('[').removePrefix("minecraft:").ifEmpty { "air" }
        }

        /** As 256 alturas (9 bits cada): 1.16+ sem atravessar os longs, 1.13–1.15 atravessando. */
        fun unpackHeights(longs: LongArray, minY: Int): IntArray? {
            if (longs.isEmpty()) return null
            val out = IntArray(256)
            val totalBits = longs.size * 64
            if (totalBits % 256 == 0 && totalBits / 256 in 1..16) {
                val bits = totalBits / 256
                val mask = (1L shl bits) - 1
                for (i in 0 until 256) {
                    val bitIndex = i * bits
                    val li = bitIndex / 64
                    val bo = bitIndex % 64
                    var v = longs[li] ushr bo
                    if (bo + bits > 64 && li + 1 < longs.size) v = v or (longs[li + 1] shl (64 - bo))
                    out[i] = (v and mask).toInt() + minY
                }
            } else {
                val perLong = (256 + longs.size - 1) / longs.size
                val bits = 64 / perLong
                val mask = (1L shl bits) - 1
                for (i in 0 until 256) {
                    val li = i / perLong
                    if (li >= longs.size) break
                    out[i] = ((longs[li] ushr ((i % perLong) * bits)) and mask).toInt() + minY
                }
            }
            return out
        }
    }
}

internal sealed class BlockSection {
    abstract fun get(x: Int, y: Int, z: Int): String
    /** Seção de um bloco só (ex.: toda de ar), ou null. */
    open fun uniform(): String? = null
    /** y local (0..15) do bloco visível mais alto da coluna, ou -1. */
    abstract fun topVisible(x: Int, z: Int): Int

    class Palette(private val names: Array<String>, private val data: LongArray?, private val padded: Boolean) : BlockSection() {
        /**
         * Bits por bloco tirados do tamanho dos dados, não da paleta: o Lithium salva seções com
         * mais bits do que a paleta pede (ex.: 14 entradas = 4 bits, mas 342 longs = 5 bits), e o
         * próprio jogo lê pelo tamanho dos dados desde a correção do MC-239610.
         */
        private val bits = PackedBits.choose(
            data, padded, entries = 4096, paletteSize = names.size,
            fromPalette = maxOf(4, 32 - Integer.numberOfLeadingZeros(names.size - 1))
        )
        private val mask = (1L shl bits) - 1
        private val perLong = 64 / bits

        override fun uniform(): String? = if (names.size == 1 || data == null || data.isEmpty()) names[0] else null

        /** Quais entradas da paleta são invisíveis (calculado uma vez por seção). */
        private val invisible = BooleanArray(names.size) { BlockColors.isInvisible(names[it]) }

        private fun indexAt(x: Int, y: Int, z: Int): Int {
            if (names.size == 1 || data == null || data.isEmpty()) return 0
            val index = (y shl 8) or (z shl 4) or x
            val v = if (padded) {
                val li = index / perLong
                if (li >= data.size) return 0
                (data[li] ushr ((index % perLong) * bits)) and mask
            } else {
                val bitIndex = index * bits
                val li = bitIndex ushr 6
                if (li >= data.size) return 0
                val bo = bitIndex and 63
                var r = data[li] ushr bo
                if (bo + bits > 64 && li + 1 < data.size) r = r or (data[li + 1] shl (64 - bo))
                r and mask
            }
            val i = v.toInt()
            return if (i < names.size) i else 0
        }

        override fun get(x: Int, y: Int, z: Int): String = names[indexAt(x, y, z)]

        override fun topVisible(x: Int, z: Int): Int {
            for (ly in 15 downTo 0) if (!invisible[indexAt(x, ly, z)]) return ly
            return -1
        }
    }

    /** Antes do 1.13: IDs numéricos (Blocks) + metadado (Data, 4 bits). */
    class Legacy(private val blocks: ByteArray, private val meta: ByteArray?) : BlockSection() {
        override fun get(x: Int, y: Int, z: Int): String {
            val index = (y shl 8) or (z shl 4) or x
            val id = blocks[index].toInt() and 0xFF
            val m = meta?.getOrNull(index shr 1)?.toInt()?.let { if (index and 1 == 0) it and 0x0F else (it shr 4) and 0x0F } ?: 0
            return BlockColors.legacyBlock(id, m)
        }

        override fun topVisible(x: Int, z: Int): Int {
            for (ly in 15 downTo 0) {
                val id = blocks[(ly shl 8) or (z shl 4) or x].toInt() and 0xFF
                //0 ar, 20 vidro, 102 painel de vidro
                if (id != 0 && id != 20 && id != 102) return ly
            }
            return -1
        }
    }
}

internal class BiomeSection(private val names: Array<String>, private val data: LongArray?) {
    //também pelo tamanho dos dados (64 biomas por seção)
    private val bits = if (names.size <= 1 || data == null || data.isEmpty()) 0
    else PackedBits.choose(
        data, padded = true, entries = 64, paletteSize = names.size,
        fromPalette = 32 - Integer.numberOfLeadingZeros(names.size - 1)
    )

    fun get(x: Int, y: Int, z: Int): String {
        if (bits == 0 || data == null || data.isEmpty()) return names[0]
        val index = (y shl 4) or (z shl 2) or x
        val perLong = 64 / bits
        val li = index / perLong
        if (li >= data.size) return names[0]
        val v = (data[li] ushr ((index % perLong) * bits)) and ((1L shl bits) - 1)
        return names.getOrNull(v.toInt()) ?: names[0]
    }
}

/**
 * Quantos bits por valor um array compactado usa. Normalmente é o que a paleta pede, mas mods
 * como o Lithium salvam com mais bits (e o jogo aceita, lendo pelo tamanho do array). Como um
 * mesmo tamanho serve para vários bits (ex.: 1024 longs = 13 a 16 bits), testa cada candidato e
 * fica com o que dá todos os índices dentro da paleta.
 */
internal object PackedBits {
    fun choose(data: LongArray?, padded: Boolean, entries: Int, paletteSize: Int, fromPalette: Int): Int {
        if (data == null || data.isEmpty()) return fromPalette
        val candidates = (1..32).filter { sizeFor(it, padded, entries) == data.size }
        if (candidates.isEmpty()) {
            //tamanho estranho: o maior número de bits que ainda cabe no array
            return if (padded) (64 / ((entries + data.size - 1) / data.size)).coerceIn(1, 32)
            else (data.size * 64 / entries).coerceIn(1, 32)
        }
        if (fromPalette in candidates) return fromPalette
        if (candidates.size == 1) return candidates[0]
        var best = candidates[0]
        var bestBad = Int.MAX_VALUE
        for (b in candidates) {
            val bad = outOfRange(data, padded, entries, b, paletteSize)
            if (bad == 0) return b
            if (bad < bestBad) { bestBad = bad; best = b }
        }
        return best
    }

    private fun sizeFor(bits: Int, padded: Boolean, entries: Int): Int =
        if (padded) { val per = 64 / bits; (entries + per - 1) / per }
        else (entries * bits + 63) / 64

    internal fun outOfRange(data: LongArray, padded: Boolean, entries: Int, bits: Int, paletteSize: Int): Int {
        val mask = if (bits >= 64) -1L else (1L shl bits) - 1
        var bad = 0
        if (padded) {
            val per = 64 / bits
            for (i in 0 until entries) {
                val li = i / per
                if (li >= data.size) break
                if (((data[li] ushr ((i % per) * bits)) and mask) >= paletteSize) bad++
            }
        } else {
            for (i in 0 until entries) {
                val bi = i * bits
                val li = bi ushr 6
                if (li >= data.size) break
                val bo = bi and 63
                var r = data[li] ushr bo
                if (bo + bits > 64 && li + 1 < data.size) r = r or (data[li + 1] shl (64 - bo))
                if ((r and mask) >= paletteSize) bad++
            }
        }
        return bad
    }
}

/** Leitor de NBT para árvore (Map/List/arrays), pulando as tags pedidas em qualquer nível. */
internal object NbtTree {
    fun read(data: ByteArray, skip: Set<String>): Map<String, Any?> {
        val input = DataInputStream(ByteArrayInputStream(data))
        if (input.readUnsignedByte() != 10) return emptyMap()
        input.readUTF()
        return compound(input, skip)
    }

    private fun compound(input: DataInputStream, skip: Set<String>): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        while (true) {
            val type = input.readUnsignedByte()
            if (type == 0) return map
            val name = input.readUTF()
            if (name in skip) skip(input, type) else map[name] = payload(input, type, skip)
        }
    }

    private fun payload(input: DataInputStream, type: Int, skip: Set<String>): Any? = when (type) {
        1 -> input.readByte()
        2 -> input.readShort()
        3 -> input.readInt()
        4 -> input.readLong()
        5 -> input.readFloat()
        6 -> input.readDouble()
        7 -> ByteArray(checkSize(input.readInt(), 1 shl 20)).also { input.readFully(it) }
        8 -> input.readUTF()
        9 -> {
            val elem = input.readUnsignedByte()
            val n = checkSize(input.readInt(), 1 shl 20)
            //1.21.5+: lista com tipos misturados vira lista de compounds, e cada item simples fica
            //embrulhado num compound de chave vazia ({"": "minecraft:stone"}). Desembrulha aqui.
            ArrayList<Any?>(minOf(n, 4096)).apply {
                repeat(n) {
                    val v = payload(input, elem, skip)
                    add(if (elem == 10 && v is Map<*, *> && v.size == 1 && v.containsKey("")) v[""] else v)
                }
            }
        }
        10 -> compound(input, skip)
        11 -> IntArray(checkSize(input.readInt(), 1 shl 18)) { input.readInt() }
        12 -> LongArray(checkSize(input.readInt(), 1 shl 18)) { input.readLong() }
        else -> error("NBT tipo $type")
    }

    private fun checkSize(n: Int, max: Int): Int {
        require(n in 0..max) { "NBT tamanho $n" }
        return n
    }

    private fun skip(input: DataInputStream, type: Int) {
        when (type) {
            1 -> input.skipBytes(1)
            2 -> input.skipBytes(2)
            3, 5 -> input.skipBytes(4)
            4, 6 -> input.skipBytes(8)
            7 -> skipFully(input, input.readInt().toLong())
            8 -> skipFully(input, input.readUnsignedShort().toLong())
            9 -> {
                val elem = input.readUnsignedByte()
                repeat(input.readInt()) { skip(input, elem) }
            }
            10 -> while (true) {
                val t = input.readUnsignedByte()
                if (t == 0) break
                skipFully(input, input.readUnsignedShort().toLong())
                skip(input, t)
            }
            11 -> skipFully(input, input.readInt() * 4L)
            12 -> skipFully(input, input.readInt() * 8L)
        }
    }

    private fun skipFully(input: DataInputStream, n: Long) {
        var left = n
        while (left > 0) {
            val s = input.skip(left)
            if (s <= 0) {
                input.readByte()
                left--
            } else left -= s
        }
    }
}
