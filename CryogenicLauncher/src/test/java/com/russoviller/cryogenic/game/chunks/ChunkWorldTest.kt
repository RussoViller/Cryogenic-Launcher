package com.russoviller.cryogenic.game.chunks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.BitSet
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

/** Mundo sintético: regiões com chunks 1.18+, 1.14 e 1.12, nos formatos de pasta antigo e 26.1. */
class ChunkWorldTest {
    @get:Rule val tmp = TemporaryFolder()

    // ------------------------------------------------------------ NBT mínimo
    private class Nbt {
        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        fun begin(name: String = "") = apply { out.writeByte(10); out.writeUTF(name) }
        fun compound(name: String, body: Nbt.() -> Unit) = apply { out.writeByte(10); out.writeUTF(name); body(); out.writeByte(0) }
        fun int(name: String, v: Int) = apply { out.writeByte(3); out.writeUTF(name); out.writeInt(v) }
        fun long(name: String, v: Long) = apply { out.writeByte(4); out.writeUTF(name); out.writeLong(v) }
        fun string(name: String, v: String) = apply { out.writeByte(8); out.writeUTF(name); out.writeUTF(v) }
        fun intArray(name: String, v: IntArray) = apply { out.writeByte(11); out.writeUTF(name); out.writeInt(v.size); v.forEach { out.writeInt(it) } }
        fun longArray(name: String, v: LongArray) = apply { out.writeByte(12); out.writeUTF(name); out.writeInt(v.size); v.forEach { out.writeLong(it) } }
        fun compoundList(name: String, n: Int, body: Nbt.(Int) -> Unit) = apply {
            out.writeByte(9); out.writeUTF(name); out.writeByte(10); out.writeInt(n)
            repeat(n) { body(it); out.writeByte(0) }
        }
        fun end(): ByteArray { out.writeByte(0); return bytes.toByteArray() }
    }

    /** 9 bits por valor, 7 por long (1.16+). */
    private fun padded(values: IntArray): LongArray {
        val longs = LongArray(37)
        values.forEachIndexed { i, v -> longs[i / 7] = longs[i / 7] or (v.toLong() shl ((i % 7) * 9)) }
        return longs
    }

    /** 9 bits por valor atravessando os longs (1.13–1.15). */
    private fun spanning(values: IntArray): LongArray {
        val longs = LongArray(36)
        values.forEachIndexed { i, v ->
            val bit = i * 9
            val li = bit / 64
            val bo = bit % 64
            longs[li] = longs[li] or (v.toLong() shl bo)
            if (bo + 9 > 64) longs[li + 1] = longs[li + 1] or (v.toLong() ushr (64 - bo))
        }
        return longs
    }

    private fun modernChunk(surface: Int, floor: Int, inhabited: Long): ByteArray =
        Nbt().begin()
            .int("DataVersion", 4325)
            .int("yPos", -4)
            .long("InhabitedTime", inhabited)
            //parte grande que o leitor deve pular
            .compoundList("sections", 3) { y -> int("Y", y); longArray("data", LongArray(256) { -1L }) }
            .compound("Heightmaps") {
                longArray("WORLD_SURFACE", padded(IntArray(256) { surface + 64 }))
                longArray("OCEAN_FLOOR", padded(IntArray(256) { floor + 64 }))
            }
            .end()

    private fun chunk114(surface: Int, inhabited: Long): ByteArray =
        Nbt().begin()
            .int("DataVersion", 1976)
            .compound("Level") {
                long("InhabitedTime", inhabited)
                compound("Heightmaps") {
                    longArray("WORLD_SURFACE", spanning(IntArray(256) { surface }))
                    longArray("OCEAN_FLOOR", spanning(IntArray(256) { surface }))
                }
            }
            .end()

    private fun chunk112(surface: Int, inhabited: Long): ByteArray =
        Nbt().begin()
            .compound("Level") {
                long("InhabitedTime", inhabited)
                intArray("HeightMap", IntArray(256) { surface })
            }
            .end()

    /** Região com os chunks dados (índice → NBT), comprimidos em zlib. */
    private fun writeRegion(file: File, chunks: Map<Int, ByteArray>) {
        file.parentFile.mkdirs()
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(8192)
            var sector = 2
            for ((index, nbt) in chunks) {
                val z = ByteArrayOutputStream().also { b -> DeflaterOutputStream(b).use { it.write(nbt) } }.toByteArray()
                val payload = ByteArrayOutputStream().also { b ->
                    DataOutputStream(b).apply { writeInt(z.size + 1); writeByte(2); write(z) }
                }.toByteArray()
                val count = (payload.size + 4095) / 4096
                raf.seek(sector * 4096L)
                raf.write(payload.copyOf(count * 4096))
                raf.seek(index * 4L)
                raf.writeInt((sector shl 8) or count)
                raf.seek(4096 + index * 4L)
                raf.writeInt(1_700_000_000)
                sector += count
            }
        }
    }

    private fun levelDat(world: File, body: Nbt.() -> Unit) {
        val nbt = Nbt().begin().compound("Data", body).end()
        GZIPOutputStream(File(world, "level.dat").outputStream()).use { it.write(nbt) }
    }

    private fun idx(x: Int, z: Int) = x + z * 32
    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF
    private fun pixel(img: RegionImage, cx: Int, cz: Int) = img.pixels[(cz * 16 + 8) * 512 + cx * 16 + 8]

    // ------------------------------------------------------------ testes

    @Test
    fun legacyLayout_renderAndDelete() {
        val world = tmp.newFolder("old")
        levelDat(world) { int("SpawnX", 100); int("SpawnZ", -40); string("LevelName", "Base") }
        val region = File(world, "region/r.0.-1.mca")
        writeRegion(region, mapOf(
            idx(0, 0) to modernChunk(surface = 80, floor = 80, inhabited = 50_000), //terra
            idx(1, 0) to modernChunk(surface = 63, floor = 50, inhabited = 100),    //água
            idx(2, 0) to chunk114(surface = 90, inhabited = 10),
            idx(3, 0) to chunk112(surface = 70, inhabited = 0)
        ))
        writeRegion(File(world, "entities/r.0.-1.mca"), mapOf(idx(0, 0) to chunk112(1, 0), idx(1, 0) to chunk112(1, 0)))
        writeRegion(File(world, "DIM-1/region/r.0.0.mca"), mapOf(0 to modernChunk(40, 40, 0)))

        val dims = ChunkWorld.listDimensions(world)
        assertEquals(listOf(DIM_OVERWORLD, DIM_NETHER), dims.map { it.id })
        val over = dims.first()
        assertEquals(setOf(packXZ(0, -1)), ChunkWorld.listRegions(over).keys)
        val presence = ChunkWorld.readPresence(region)
        assertEquals(4, presence.cardinality())
        assertEquals(100 to -40, ChunkWorld.readSpawn(world))

        val img = RegionRenderer.render(region, DIM_OVERWORLD)
        assertEquals(longArrayOf(50_000, 100, 10, 0).toList(), img.inhabited.take(4))
        val land = pixel(img, 0, 0)
        val water = pixel(img, 1, 0)
        assertTrue("terra deve ser verde: ${Integer.toHexString(land)}", g(land) > b(land) && g(land) > r(land))
        assertTrue("água deve ser azul: ${Integer.toHexString(water)}", b(water) > r(water) && b(water) > g(water))
        for (cx in 2..3) assertTrue("chunk antigo com altura", pixel(img, cx, 0) != EMPTY_COLOR && g(pixel(img, cx, 0)) > b(pixel(img, cx, 0)))
        assertEquals(EMPTY_COLOR, pixel(img, 5, 5))
        assertEquals(img.inhabited.toList(), RegionRenderer.readInhabited(region).toList().mapIndexed { i, v -> if (presence[i]) v else -1L })

        //apaga 2 chunks com backup
        val backup = ChunkWorld.newBackupDir(world, "chunks")
        val original = region.readBytes()
        val sel = mapOf(packXZ(0, -1) to BitSet().apply { set(idx(0, 0)); set(idx(1, 0)); set(idx(9, 9)) })
        assertEquals(2, ChunkWorld.deleteChunks(world, over, sel, backup))
        assertEquals(setOf(idx(2, 0), idx(3, 0)), ChunkWorld.readPresence(region).stream().toArray().toSet())
        assertTrue(original.contentEquals(File(backup, "region/r.0.-1.mca").readBytes()))
        assertTrue(File(backup, "entities/r.0.-1.mca").isFile)
        //entidades daquela região ficaram vazias: arquivo apagado
        assertFalse(File(world, "entities/r.0.-1.mca").exists())
        //horários zerados
        RandomAccessFile(region, "r").use { raf -> raf.seek(4096); assertEquals(0, raf.readInt()) }

        //apaga o resto sem backup: região some
        val rest = mapOf(packXZ(0, -1) to BitSet().apply { set(idx(2, 0)); set(idx(3, 0)) })
        assertEquals(2, ChunkWorld.deleteChunks(world, over, rest, null))
        assertFalse(region.exists())
    }

    @Test
    fun newLayout_dimensionsAndDeleteDimension() {
        val world = tmp.newFolder("new")
        levelDat(world) { compound("spawn") { string("dimension", "minecraft:overworld"); intArray("pos", intArrayOf(-300, 70, 512)) } }
        val base = File(world, "dimensions/minecraft")
        writeRegion(File(base, "overworld/region/r.-1.0.mca"), mapOf(0 to modernChunk(70, 70, 0)))
        writeRegion(File(base, "the_end/region/r.0.0.mca"), mapOf(0 to modernChunk(60, 60, 0)))
        writeRegion(File(base, "the_end/poi/r.0.0.mca"), mapOf(0 to chunk112(1, 0)))
        writeRegion(File(world, "dimensions/mymod/sky/region/r.0.0.mca"), mapOf(0 to modernChunk(60, 60, 0)))

        val dims = ChunkWorld.listDimensions(world)
        assertEquals(listOf(DIM_OVERWORLD, DIM_END, "mymod:sky"), dims.map { it.id })
        assertEquals(-300 to 512, ChunkWorld.readSpawn(world))
        val end = dims[1]
        val endImg = RegionRenderer.render(File(base, "the_end/region/r.0.0.mca"), end.id)
        assertTrue(pixel(endImg, 0, 0) != EMPTY_COLOR)

        val backup = ChunkWorld.newBackupDir(world, "dimension")
        assertTrue(ChunkWorld.deleteDimension(world, end, backup))
        assertFalse(File(base, "the_end/region").exists())
        assertFalse(File(base, "the_end/poi").exists())
        assertTrue(File(backup, "dimensions/minecraft/the_end/region/r.0.0.mca").isFile)
        assertTrue(File(backup, "dimensions/minecraft/the_end/poi/r.0.0.mca").isFile)
        assertEquals(listOf(DIM_OVERWORLD, "mymod:sky"), ChunkWorld.listDimensions(world).map { it.id })
    }

    /** Seção 1.18+ (4 bits por bloco, sem atravessar os longs). */
    private fun Nbt.section(y: Int, palette: List<String>, block: (Int, Int, Int) -> Int) {
        int("Y", y)
        compound("block_states") {
            out.writeByte(9); out.writeUTF("palette"); out.writeByte(10); out.writeInt(palette.size)
            palette.forEach { string("Name", "minecraft:$it"); out.writeByte(0) }
            val data = LongArray(256)
            for (i in 0 until 4096) data[i / 16] = data[i / 16] or (block(i and 15, i shr 8, (i shr 4) and 15).toLong() shl ((i % 16) * 4))
            longArray("data", data)
        }
        compound("biomes") {
            out.writeByte(9); out.writeUTF("palette"); out.writeByte(8); out.writeInt(1); out.writeUTF("minecraft:plains")
        }
    }

    @Test
    fun blockColors() {
        val world = tmp.newFolder("blocks")
        val palette = listOf("air", "grass_block", "stone", "water", "sand", "black_concrete", "black_stained_glass", "bush")
        //x < 8: grama em y=64; x >= 8: água de 60 a 62 sobre areia em 59
        fun block(x: Int, y: Int, z: Int = 0): Int = when {
            //x 0..1: piso de concreto preto em y=70 (mapa de altura velho diz 65)
            x < 2 && y == 70 -> 5
            //x 2..3: vidro preto em 72 sobre concreto preto em 70
            x in 2..3 && y == 72 -> 6
            x in 2..3 && y == 70 -> 5
            //x 4: arbusto (1.21.5) sobre a grama
            x == 4 && y == 65 -> 7
            x < 8 -> if (y < 64) 2 else if (y == 64) 1 else 0
            else -> if (y < 59) 2 else if (y == 59) 4 else if (y <= 62) 3 else 0
        }
        val modern = Nbt().begin()
            .int("DataVersion", 4325).int("yPos", -4).long("InhabitedTime", 0)
            .compoundList("sections", 2) { n ->
                val sy = 3 + n
                section(sy, palette) { x, ly, _ -> block(x, sy * 16 + ly) }
            }
            .compound("Heightmaps") {
                longArray("WORLD_SURFACE", padded(IntArray(256) { (if ((it and 15) < 8) 65 else 63) + 64 }))
            }
            .end()
        //1.12: areia (id 12) no topo em y=70
        val legacy = Nbt().begin()
            .compound("Level") {
                long("InhabitedTime", 0)
                intArray("HeightMap", IntArray(256) { 71 })
                compoundList("Sections", 1) {
                    out.writeByte(1); out.writeUTF("Y"); out.writeByte(4)
                    val blocks = ByteArray(4096) { i -> if ((i shr 8) <= 6) 12 else 0 }
                    out.writeByte(7); out.writeUTF("Blocks"); out.writeInt(4096); out.write(blocks)
                }
            }
            .end()
        val region = File(world, "region/r.0.0.mca")
        writeRegion(region, mapOf(0 to modern, 1 to legacy))
        val img = RegionRenderer.render(region, DIM_OVERWORLD)
        val grass = img.pixels[8 * 512 + 6]
        val concrete = img.pixels[8 * 512 + 0]
        val glass = img.pixels[8 * 512 + 2]
        val bush = img.pixels[8 * 512 + 4]
        for (c in listOf(concrete, glass)) assertTrue("preto: ${Integer.toHexString(c)}", r(c) < 70 && g(c) < 70 && b(c) < 70)
        assertTrue("arbusto verde: ${Integer.toHexString(bush)}", g(bush) > r(bush) && g(bush) > b(bush))
        val water = img.pixels[8 * 512 + 12]
        val sand = img.pixels[8 * 512 + 16 + 8]
        assertTrue("grama verde: ${Integer.toHexString(grass)}", g(grass) > r(grass) && g(grass) > b(grass))
        assertTrue("água azul: ${Integer.toHexString(water)}", b(water) > r(water) && b(water) > g(water))
        assertTrue("areia clara amarelada: ${Integer.toHexString(sand)}", r(sand) > 180 && g(sand) > 170 && b(sand) < r(sand))
    }

    @Test
    fun lz4AndExternalChunks() {
        //bloco LZ4 de verdade (gerado pelo lz4 oficial) dentro do formato LZ4Block
        val original = java.util.Base64.getDecoder().decode("aGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyBoZWxsbyBjcnlvZ2VuaWMgaGVsbG8gY3J5b2dlbmljIGhlbGxvIGNyeW9nZW5pYyAAAQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl9gYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXp7fH1+f4CBgoOEhYaHiImKi4yNjo+QkZKTlJWWl5iZmpucnZ6foKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr/AwcLDxMXGx8jJysvMzc7P0NHS09TV1tfY2drb3N3e3+Dh4uPk5ebn6Onq6+zt7u/w8fLz9PX29/j5+vv8/f7/")
        val packed = java.util.Base64.getDecoder().decode("/wFoZWxsbyBjcnlvZ2VuaWMgEAD///////////////9p8PEAAQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl9gYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXp7fH1+f4CBgoOEhYaHiImKi4yNjo+QkZKTlJWWl5iZmpucnZ6foKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr/AwcLDxMXGx8jJysvMzc7P0NHS09TV1tfY2drb3N3e3+Dh4uPk5ebn6Onq6+zt7u/w8fLz9PX29/j5+vv8/f7/")
        fun wrap(method: Int, payload: ByteArray, size: Int): ByteArray {
            val b = java.nio.ByteBuffer.allocate(21 + payload.size + 21).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            b.put("LZ4Block".toByteArray()); b.put(method.toByte()); b.putInt(payload.size); b.putInt(size); b.putInt(0); b.put(payload)
            b.put("LZ4Block".toByteArray()); b.put(0x10.toByte()); b.putInt(0); b.putInt(0); b.putInt(0)
            return b.array()
        }
        assertTrue(original.contentEquals(Lz4Block.decode(wrap(0x20, packed, original.size))))

        //região com um chunk em LZ4 (bloco cru) e outro grande demais, salvo em .mcc
        val world = tmp.newFolder("lz4")
        val chunk = modernChunk(80, 80, 0)
        val region = File(world, "region/r.1.2.mca").apply { parentFile.mkdirs() }
        RandomAccessFile(region, "rw").use { raf ->
            raf.setLength(8192 + 4096 * 64)
            val lz = wrap(0x10, chunk, chunk.size)
            raf.seek(2 * 4096L); raf.writeInt(lz.size + 1); raf.writeByte(4); raf.write(lz)
            val count = (lz.size + 5 + 4095) / 4096
            raf.seek(0); raf.writeInt((2 shl 8) or count)
            //chunk 1: só o cabeçalho com bit 128 (zlib) e os dados no c.33.64.mcc
            raf.seek((2 + count) * 4096L); raf.writeInt(1); raf.writeByte(128 or 2)
            raf.seek(4); raf.writeInt(((2 + count) shl 8) or 1)
        }
        val z = ByteArrayOutputStream().also { b -> DeflaterOutputStream(b).use { it.write(modernChunk(63, 50, 0)) } }.toByteArray()
        File(region.parentFile, "c.33.64.mcc").writeBytes(z)
        val img = RegionRenderer.render(region, DIM_OVERWORLD)
        val lzPixel = pixel(img, 0, 0)
        val mccPixel = pixel(img, 1, 0)
        assertTrue("lz4 lido: ${Integer.toHexString(lzPixel)}", g(lzPixel) > b(lzPixel))
        assertTrue(".mcc lido: ${Integer.toHexString(mccPixel)}", b(mccPixel) > r(mccPixel))
    }

    /** Seção salva pelo Lithium: paleta pede 4 bits, mas os dados têm 5 bits (342 longs). */
    @Test
    fun lithiumOversizedStorage() {
        val world = tmp.newFolder("lithium")
        val palette = listOf("air", "stone", "sand")
        val bits = 5
        val perLong = 64 / bits
        val data = LongArray((4096 + perLong - 1) / perLong)
        for (i in 0 until 4096) {
            val y = i shr 8
            val v = if (y <= 4) 2L else 0L //areia até y=4 da seção
            data[i / perLong] = data[i / perLong] or (v shl ((i % perLong) * bits))
        }
        assertEquals(342, data.size)
        val nbt = Nbt().begin()
            .int("DataVersion", 4325).int("yPos", -4).long("InhabitedTime", 0)
            .compoundList("sections", 1) {
                int("Y", 4)
                compound("block_states") {
                    out.writeByte(9); out.writeUTF("palette"); out.writeByte(10); out.writeInt(palette.size)
                    palette.forEach { string("Name", "minecraft:$it"); out.writeByte(0) }
                    longArray("data", data)
                }
            }
            .end()
        val region = File(world, "region/r.0.0.mca")
        writeRegion(region, mapOf(0 to nbt))
        val img = RegionRenderer.render(region, DIM_OVERWORLD)
        val p = pixel(img, 0, 0)
        assertTrue("areia (não preto/cinza): ${Integer.toHexString(p)}", r(p) > 180 && g(p) > 170 && b(p) < r(p))
    }

    /** Seção com mais de 256 estados salva com 15 bits (1024 longs, mesmo tamanho de 13 a 16 bits). */
    @Test
    fun ambiguousWideStorage() {
        val world = tmp.newFolder("wide")
        val palette = listOf("air", "stone") + (0 until 298).map { "oak_stairs_$it" } + listOf("sand")
        val sandIdx = palette.size - 1L
        val bits = 15
        val perLong = 64 / bits
        val data = LongArray((4096 + perLong - 1) / perLong)
        for (i in 0 until 4096) {
            val y = i shr 8
            val v = if (y <= 4) sandIdx else 0L
            data[i / perLong] = data[i / perLong] or (v shl ((i % perLong) * bits))
        }
        assertEquals(1024, data.size)
        val nbt = Nbt().begin()
            .int("DataVersion", 3955).int("yPos", -4).long("InhabitedTime", 0)
            .compoundList("sections", 1) {
                int("Y", 4)
                compound("block_states") {
                    out.writeByte(9); out.writeUTF("palette"); out.writeByte(10); out.writeInt(palette.size)
                    palette.forEach { string("Name", "minecraft:$it"); out.writeByte(0) }
                    longArray("data", data)
                }
            }
            .end()
        val region = File(world, "region/r.0.0.mca")
        writeRegion(region, mapOf(0 to nbt))
        val report = ChunkInspector.report(region, 0, 0)
        assertTrue(report, report.contains("Y=4 pal=301 longs=1024 bits=15") && report.contains("fora=0"))
        val exported = File(world, "c.nbt")
        assertTrue(ChunkInspector.export(region, 0, 0, exported) && exported.length() > 0)
        val img = RegionRenderer.render(region, DIM_OVERWORLD)
        val p = pixel(img, 0, 0)
        assertTrue("areia: ${Integer.toHexString(p)}", r(p) > 180 && g(p) > 170 && b(p) < r(p))
    }

    /** 1.21.5+/26.x: paleta mista (texto + compound) salva como compounds de chave vazia. */
    @Test
    fun heterogeneousPalette() {
        val world = tmp.newFolder("hetero")
        val data = LongArray(256)
        for (i in 0 until 4096) {
            val y = i shr 8
            val v = if (y <= 4) 1L else 0L //areia até y=4 da seção
            data[i / 16] = data[i / 16] or (v shl ((i % 16) * 4))
        }
        val nbt = Nbt().begin()
            .int("DataVersion", 5023).int("yPos", -4).long("InhabitedTime", 0)
            .compoundList("sections", 1) {
                int("Y", 4)
                compound("block_states") {
                    out.writeByte(9); out.writeUTF("palette"); out.writeByte(10); out.writeInt(3)
                    string("", "minecraft:air"); out.writeByte(0)
                    string("", "minecraft:sand"); out.writeByte(0)
                    string("id", "minecraft:oak_log"); compound("properties") { string("axis", "y") }; out.writeByte(0)
                    longArray("data", data)
                }
            }
            .end()
        val region = File(world, "region/r.0.0.mca")
        writeRegion(region, mapOf(0 to nbt))
        val img = RegionRenderer.render(region, DIM_OVERWORLD)
        val p = pixel(img, 0, 0)
        assertTrue("areia: ${Integer.toHexString(p)}", r(p) > 180 && g(p) > 170 && b(p) < r(p))
    }

    @Test
    fun worldInUse() {
        val world = tmp.newFolder("lock")
        assertFalse(ChunkWorld.isWorldInUse(world))
        val lock = File(world, "session.lock").apply { writeText("x") }
        assertFalse(ChunkWorld.isWorldInUse(world))
        FileChannel.open(lock.toPath(), StandardOpenOption.WRITE).use { ch ->
            ch.lock().use { assertTrue(ChunkWorld.isWorldInUse(world)) }
        }
        assertFalse(ChunkWorld.isWorldInUse(world))
    }
}
