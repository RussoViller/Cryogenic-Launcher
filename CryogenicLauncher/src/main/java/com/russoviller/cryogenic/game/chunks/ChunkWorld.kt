package com.russoviller.cryogenic.game.chunks

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.BitSet
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/*
 * Leitura e edição dos arquivos de região (.mca) do Minecraft Java para o "Apagar chunks".
 *
 * Formato da região: 32×32 chunks; os primeiros 4 KiB guardam, para cada chunk, onde os dados
 * estão no arquivo (3 bytes de setor + 1 byte de tamanho) e os 4 KiB seguintes o horário de
 * gravação. Apagar um chunk = zerar essas duas entradas: o jogo passa a ver o chunk como nunca
 * gerado e cria terreno novo (com os biomas e estruturas da versão atual) na próxima visita.
 * O mesmo chunk é apagado também nas pastas entities/ (1.17+) e poi/ (1.14+), como fazem as
 * ferramentas de PC (ex.: MCA Selector), para não sobrarem mobs ou pontos de interesse órfãos.
 */

/** Uma dimensão do mundo e a pasta que contém region/, entities/ e poi/. */
data class WorldDimension(val id: String, val root: File) {
    val regionDir get() = File(root, "region")
    val folders get() = listOf("region", "entities", "poi").map { File(root, it) }
}

const val DIM_OVERWORLD = "minecraft:overworld"
const val DIM_NETHER = "minecraft:the_nether"
const val DIM_END = "minecraft:the_end"

/** Chave única de região/chunk em um Long. */
fun packXZ(x: Int, z: Int): Long = (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)
fun unpackX(key: Long): Int = (key shr 32).toInt()
fun unpackZ(key: Long): Int = key.toInt()

private val REGION_NAME = Regex("""r\.(-?\d+)\.(-?\d+)\.mca""")

object ChunkWorld {
    /**
     * Dimensões existentes no mundo. Aceita o formato antigo (region/, DIM-1/, DIM1/) e o do
     * 26.1+ (dimensions/minecraft/overworld ...), além de dimensões de mods/datapacks
     * (dimensions/<namespace>/<nome>).
     */
    fun listDimensions(world: File): List<WorldDimension> {
        val result = linkedMapOf<String, WorldDimension>()
        val dims = File(world, "dimensions")
        fun add(id: String, root: File) {
            if (File(root, "region").isDirectory && id !in result) result[id] = WorldDimension(id, root)
        }
        add(DIM_OVERWORLD, File(dims, "minecraft/overworld"))
        add(DIM_OVERWORLD, world)
        add(DIM_NETHER, File(dims, "minecraft/the_nether"))
        add(DIM_NETHER, File(world, "DIM-1"))
        add(DIM_END, File(dims, "minecraft/the_end"))
        add(DIM_END, File(world, "DIM1"))
        dims.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { ns ->
            ns.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { add("${ns.name}:${it.name}", it) }
        }
        return result.values.toList()
    }

    /** Regiões existentes da dimensão (chave = packXZ da região). */
    fun listRegions(dim: WorldDimension): Map<Long, File> {
        val map = HashMap<Long, File>()
        dim.regionDir.listFiles()?.forEach { file ->
            val m = REGION_NAME.matchEntire(file.name) ?: return@forEach
            if (file.length() < 8192) return@forEach
            map[packXZ(m.groupValues[1].toInt(), m.groupValues[2].toInt())] = file
        }
        return map
    }

    /** Quais dos 1024 chunks da região existem (lendo só o cabeçalho de 4 KiB). */
    fun readPresence(file: File): BitSet {
        val bits = BitSet(1024)
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4096)
                raf.readFully(header)
                for (i in 0 until 1024) {
                    val loc = readInt(header, i * 4)
                    if (loc != 0) bits.set(i)
                }
            }
        }
        return bits
    }

    /** O mundo está aberto no jogo? (o jogo segura uma trava no session.lock) */
    fun isWorldInUse(world: File): Boolean {
        val lock = File(world, "session.lock")
        if (!lock.exists()) return false
        return runCatching {
            FileChannel.open(lock.toPath(), StandardOpenOption.WRITE).use { ch ->
                val l = ch.tryLock()
                if (l == null) true else {
                    l.release()
                    false
                }
            }
        }.getOrDefault(true)
    }

    /** Pasta de cópias de segurança dentro do próprio mundo. */
    fun newBackupDir(world: File, label: String): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date())
        return File(world, "cryogenic_backups/${label}_$stamp")
    }

    /**
     * Apaga os chunks selecionados ([selection]: região → índices 0..1023) em region/, entities/
     * e poi/. Com [backupDir], copia antes cada arquivo que vai ser alterado. Retorna quantos
     * chunks foram apagados de fato.
     */
    fun deleteChunks(world: File, dim: WorldDimension, selection: Map<Long, BitSet>, backupDir: File?): Int {
        var deleted = 0
        for ((regionKey, bits) in selection) {
            if (bits.isEmpty) continue
            val name = "r.${unpackX(regionKey)}.${unpackZ(regionKey)}.mca"
            for (folder in dim.folders) {
                val file = File(folder, name)
                if (!file.isFile || file.length() < 8192) continue
                if (backupDir != null) {
                    val target = File(backupDir, file.relativeTo(world).path)
                    target.parentFile?.mkdirs()
                    file.copyTo(target, overwrite = true)
                }
                val count = clearChunks(file, bits)
                if (folder.name == "region") deleted += count
            }
        }
        return deleted
    }

    /** Zera a localização e o horário dos chunks; região sem nenhum chunk vira arquivo apagado. */
    private fun clearChunks(file: File, bits: BitSet): Int {
        var count = 0
        var empty: Boolean
        RandomAccessFile(file, "rw").use { raf ->
            val header = ByteArray(8192)
            raf.seek(0)
            raf.readFully(header)
            var i = bits.nextSetBit(0)
            while (i in 0 until 1024) {
                if (readInt(header, i * 4) != 0) count++
                writeInt(header, i * 4, 0)
                writeInt(header, 4096 + i * 4, 0)
                i = bits.nextSetBit(i + 1)
            }
            raf.seek(0)
            raf.write(header)
            raf.fd.sync()
            empty = (0 until 1024).all { readInt(header, it * 4) == 0 }
        }
        if (empty) file.delete()
        return count
    }

    /**
     * Apaga a dimensão inteira (region/, entities/ e poi/). Com [backupDir], as pastas são
     * movidas para a cópia de segurança (instantâneo e sem ocupar espaço a mais).
     */
    fun deleteDimension(world: File, dim: WorldDimension, backupDir: File?): Boolean {
        var ok = true
        for (folder in dim.folders) {
            if (!folder.exists()) continue
            if (backupDir != null) {
                val target = File(backupDir, folder.relativeTo(world).path)
                target.parentFile?.mkdirs()
                if (!folder.renameTo(target)) {
                    ok = folder.copyRecursively(target, overwrite = true) && folder.deleteRecursively() && ok
                }
            } else {
                ok = folder.deleteRecursively() && ok
            }
        }
        return ok
    }

    /**
     * Spawn do mundo (em blocos), para abrir o mapa no lugar certo. Antigo: Data.SpawnX/SpawnZ;
     * 1.21.9+: Data.spawn.pos (int[3]).
     */
    fun readSpawn(world: File): Pair<Int, Int>? = runCatching {
        val data = GZIPInputStream(File(world, "level.dat").inputStream().buffered()).use { it.readBytes() }
        var x: Int? = null
        var z: Int? = null
        var pos: IntArray? = null
        NbtScanner(data).visit(
            onInt = { path, name, v ->
                if (path.lastOrNull() == "Data") {
                    if (name == "SpawnX") x = v
                    if (name == "SpawnZ") z = v
                }
            },
            onIntArray = { path, name, v -> if (name == "pos" && path.lastOrNull() == "spawn" && v.size >= 3) pos = v },
            skipCompound = { _, name -> name == "Player" || name == "WorldGenSettings" || name == "DragonFight" }
        )
        pos?.let { return@runCatching it[0] to it[2] }
        val sx = x ?: return@runCatching null
        val sz = z ?: return@runCatching null
        sx to sz
    }.getOrNull()

    internal fun readInt(b: ByteArray, o: Int) =
        (b[o].toInt() and 0xFF shl 24) or (b[o + 1].toInt() and 0xFF shl 16) or (b[o + 2].toInt() and 0xFF shl 8) or (b[o + 3].toInt() and 0xFF)

    private fun writeInt(b: ByteArray, o: Int, v: Int) {
        b[o] = (v ushr 24).toByte(); b[o + 1] = (v ushr 16).toByte(); b[o + 2] = (v ushr 8).toByte(); b[o + 3] = v.toByte()
    }
}

/** Partes grandes do chunk que o mapa não usa: puladas sem ler. */
internal val SKIPPED_TAGS = setOf(
    "sections", "Sections", "block_entities", "Entities", "TileEntities", "structures", "Structures",
    "block_ticks", "fluid_ticks", "TileTicks", "LiquidTicks", "PostProcessing", "Lights", "CarvingMasks"
)

/** Resultado da leitura de uma região para o mapa. */
class RegionImage(
    /** 512×512 cores (RGB) por bloco; chunks que não existem ficam com [EMPTY_COLOR]. */
    val pixels: IntArray,
    /** Tempo que jogadores passaram em cada chunk (ticks), índice = x + z*32. -1 = sem dado. */
    val inhabited: LongArray,
    val presence: BitSet
)

const val EMPTY_COLOR = 0xFF0B0F17.toInt()

/**
 * Leitor de NBT mínimo e rápido: percorre a árvore só procurando os valores pedidos e pula o
 * resto (listas de seções, entidades...) sem montar objetos.
 */
internal class NbtScanner(data: ByteArray) {
    private val input = DataInputStream(ByteArrayInputStream(data))
    private val path = ArrayList<String>()

    private var onLong: (List<String>, String, Long) -> Unit = { _, _, _ -> }
    private var onInt: (List<String>, String, Int) -> Unit = { _, _, _ -> }
    private var onLongArray: (List<String>, String, LongArray) -> Unit = { _, _, _ -> }
    private var onIntArray: (List<String>, String, IntArray) -> Unit = { _, _, _ -> }
    private var skip: (List<String>, String) -> Boolean = { _, _ -> false }

    fun visit(
        onLong: (List<String>, String, Long) -> Unit = { _, _, _ -> },
        onInt: (List<String>, String, Int) -> Unit = { _, _, _ -> },
        onLongArray: (List<String>, String, LongArray) -> Unit = { _, _, _ -> },
        onIntArray: (List<String>, String, IntArray) -> Unit = { _, _, _ -> },
        skipCompound: (List<String>, String) -> Boolean = { _, _ -> false }
    ) {
        this.onLong = onLong
        this.onInt = onInt
        this.onLongArray = onLongArray
        this.onIntArray = onIntArray
        this.skip = skipCompound
        val type = input.readUnsignedByte()
        if (type != 10) return
        input.readUTF()
        readCompound()
    }

    /** Lê inteiros (int/long) cujo caminho e nome passam no filtro. */
    fun scanRoot(filter: (String, String) -> Boolean): Map<String, Long> {
        val out = HashMap<String, Long>()
        visit(
            onLong = { p, n, v -> if (filter(p.joinToString("/"), n)) out[n] = v },
            onInt = { p, n, v -> if (filter(p.joinToString("/"), n)) out[n] = v.toLong() }
        )
        return out
    }

    private fun readCompound() {
        while (true) {
            val type = input.readUnsignedByte()
            if (type == 0) return
            val name = input.readUTF()
            readPayload(type, name)
        }
    }

    private fun readPayload(type: Int, name: String) {
        when (type) {
            1 -> input.skipBytes(1)
            2 -> input.skipBytes(2)
            3 -> onInt(path, name, input.readInt())
            4 -> onLong(path, name, input.readLong())
            5 -> input.skipBytes(4)
            6 -> input.skipBytes(8)
            7 -> skipFully(input.readInt().toLong())
            8 -> skipFully(input.readUnsignedShort().toLong())
            9 -> {
                val elem = input.readUnsignedByte()
                val count = input.readInt()
                if (elem == 10 && !skip(path, name)) {
                    path.add(name)
                    repeat(count) { readCompound() }
                    path.removeAt(path.size - 1)
                } else repeat(count) { skipPayload(elem) }
            }
            10 -> {
                if (skip(path, name)) {
                    skipCompound()
                } else {
                    path.add(name)
                    readCompound()
                    path.removeAt(path.size - 1)
                }
            }
            11 -> {
                val n = input.readInt()
                if (n in 0..4096) {
                    val arr = IntArray(n) { input.readInt() }
                    onIntArray(path, name, arr)
                } else skipFully(n * 4L)
            }
            12 -> {
                val n = input.readInt()
                if (n in 1..64) {
                    val arr = LongArray(n) { input.readLong() }
                    onLongArray(path, name, arr)
                } else skipFully(n * 8L)
            }
        }
    }

    private fun skipCompound() {
        while (true) {
            val type = input.readUnsignedByte()
            if (type == 0) return
            skipFully(input.readUnsignedShort().toLong())
            skipPayload(type)
        }
    }

    private fun skipPayload(type: Int) {
        when (type) {
            1 -> input.skipBytes(1)
            2 -> input.skipBytes(2)
            3, 5 -> input.skipBytes(4)
            4, 6 -> input.skipBytes(8)
            7 -> skipFully(input.readInt().toLong())
            8 -> skipFully(input.readUnsignedShort().toLong())
            9 -> {
                val elem = input.readUnsignedByte()
                val count = input.readInt()
                repeat(count) { skipPayload(elem) }
            }
            10 -> skipCompound()
            11 -> skipFully(input.readInt() * 4L)
            12 -> skipFully(input.readInt() * 8L)
        }
    }

    private fun skipFully(n: Long) {
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
