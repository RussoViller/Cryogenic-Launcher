package com.russoviller.cryogenic.game.chunks

import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * Diagnóstico de um chunk do mapa: o que está salvo nele e como o mapa leu cada seção.
 * Serve para achar chunks desenhados errado sem precisar da pasta region inteira.
 */
internal object ChunkInspector {
    private val SKIP = setOf(
        "block_entities", "Entities", "TileEntities", "entities", "structures", "Structures",
        "block_ticks", "fluid_ticks", "TileTicks", "LiquidTicks", "PostProcessing", "Lights", "CarvingMasks",
        "SkyLight", "BlockLight", "ToBeTicked", "LiquidsToBeTicked", "UpgradeData"
    )

    @Suppress("UNCHECKED_CAST")
    fun report(region: File, cx: Int, cz: Int): String {
        val index = (cx and 31) + (cz and 31) * 32
        val sb = StringBuilder()
        sb.append("Chunk ").append(cx).append(", ").append(cz)
            .append("  (blocos ").append(cx * 16).append(", ").append(cz * 16).append(")\n")
        sb.append("Arquivo: ").append(region.name).append("  [").append(index).append("]\n")
        val compression = RegionRenderer.chunkCompression(region, index)
        val bytes = RegionRenderer.chunkBytes(region, index)
        sb.append("Compressão: ").append(compression).append("  NBT: ").append(bytes?.size ?: -1).append(" bytes\n")
        if (bytes == null) return sb.append("Não deu para ler o chunk.").toString()
        val root = runCatching { NbtTree.read(bytes, SKIP) }.getOrElse {
            return sb.append("NBT inválido: ").append(it.message).toString()
        }
        val level = root["Level"] as? Map<String, Any?>
        val base = level ?: root
        sb.append("DataVersion: ").append(root["DataVersion"]).append("  Status: ").append(base["Status"] ?: base["status"])
            .append("  yPos: ").append(root["yPos"] ?: level?.get("yPos")).append('\n')
        sb.append("Chaves: ").append(base.keys.sorted().joinToString(", ")).append('\n')
        val view = runCatching { ChunkView.parse(bytes, SKIP) }.getOrNull()
        if (view != null) {
            val top = view.topVisible(8, 8)
            sb.append("Topo (8,8): y=").append(if (top == Int.MIN_VALUE) "nenhum" else top.toString())
            if (top != Int.MIN_VALUE) sb.append(" ").append(view.block(8, top, 8))
            view.surface?.let { sb.append("  heightmap=").append(it[8 * 16 + 8] - 1) }
            sb.append('\n')
        }
        val list = (root["sections"] ?: base["Sections"]) as? List<Any?>
        sb.append("Seções: ").append(list?.size ?: 0).append('\n')
        val dataVersion = (root["DataVersion"] as? Int) ?: 0
        list?.forEach { any ->
            val sec = any as? Map<String, Any?> ?: return@forEach
            val y = (sec["Y"] as? Number)?.toInt()
            val states = sec["block_states"] as? Map<String, Any?>
            val palette = (states?.get("palette") ?: sec["Palette"]) as? List<Any?>
            val data = (states?.get("data") ?: sec["BlockStates"]) as? LongArray
            sb.append(" Y=").append(y)
            if (palette == null) {
                sb.append(" sem paleta")
                if (sec["Blocks"] != null) sb.append(" (formato antigo)")
                sb.append(" chaves=").append(sec.keys.sorted().joinToString(","))
                if (states != null) sb.append(" block_states=").append(states.keys.sorted().joinToString(","))
                sb.append('\n')
                return@forEach
            }
            val padded = states != null || dataVersion >= 2529
            val fromPalette = maxOf(4, 32 - Integer.numberOfLeadingZeros(maxOf(1, palette.size) - 1))
            sb.append(" pal=").append(palette.size).append(" longs=").append(data?.size ?: 0)
            if (data != null && data.isNotEmpty() && palette.size > 1) {
                val bits = PackedBits.choose(data, padded, 4096, palette.size, fromPalette)
                val bad = PackedBits.outOfRange(data, padded, 4096, bits, palette.size)
                sb.append(" bits=").append(bits).append("(paleta ").append(fromPalette).append(")")
                    .append(" fora=").append(bad)
            }
            val names = palette.take(3).joinToString("/") { e ->
                val raw = when (e) {
                    is Map<*, *> -> (e["Name"] ?: e["name"] ?: e["id"])?.toString()
                    else -> e?.toString()
                } ?: "?"
                raw.removePrefix("minecraft:").take(24)
            }
            sb.append(" [").append(names).append(if (palette.size > 3) "…" else "").append("]\n")
        }
        return sb.toString()
    }

    /** Salva o NBT do chunk (gzip, como um .nbt comum) em [out]. */
    fun export(region: File, cx: Int, cz: Int, out: File): Boolean {
        val index = (cx and 31) + (cz and 31) * 32
        val bytes = RegionRenderer.chunkBytes(region, index) ?: return false
        out.parentFile?.mkdirs()
        GZIPOutputStream(out.outputStream()).use { it.write(bytes) }
        return true
    }
}
