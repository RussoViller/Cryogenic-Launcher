package com.russoviller.cryogenic.game.seedmap

import androidx.annotation.StringRes
import com.russoviller.cryogenic.R

/** Dimensões (mesmos números do cubiomes). */
enum class SeedDimension(val id: Int, @StringRes val titleRes: Int) {
    Overworld(0, R.string.cryo_seedmap_dim_overworld),
    Nether(-1, R.string.cryo_seedmap_dim_nether),
    End(1, R.string.cryo_seedmap_dim_end)
}

/**
 * Estruturas que o mapa consegue achar. [type] é o StructureType do cubiomes
 * (-1 = fortaleza/stronghold, que usa outro cálculo). [minChunkZoom]: mapa precisa estar
 * pelo menos nesse zoom (px por bloco) para mostrar, porque algumas aparecem demais.
 */
enum class SeedStructure(
    val type: Int,
    @StringRes val titleRes: Int,
    val color: Int,
    val glyph: String,
    val minZoom: Float = 1f / 64f
) {
    Village(5, R.string.cryo_seedmap_st_village, 0xFFE8B04A.toInt(), "V"),
    DesertPyramid(1, R.string.cryo_seedmap_st_desert_pyramid, 0xFFF2D98A.toInt(), "D"),
    JungleTemple(2, R.string.cryo_seedmap_st_jungle_temple, 0xFF4FAE4A.toInt(), "J"),
    SwampHut(3, R.string.cryo_seedmap_st_swamp_hut, 0xFF6B7F3A.toInt(), "B"),
    Igloo(4, R.string.cryo_seedmap_st_igloo, 0xFFE6F4FF.toInt(), "I"),
    OceanRuin(6, R.string.cryo_seedmap_st_ocean_ruin, 0xFF4AA7C9.toInt(), "R", 1f / 16f),
    Shipwreck(7, R.string.cryo_seedmap_st_shipwreck, 0xFF9C6B3C.toInt(), "N", 1f / 16f),
    Monument(8, R.string.cryo_seedmap_st_monument, 0xFF3FD3C4.toInt(), "M"),
    Mansion(9, R.string.cryo_seedmap_st_mansion, 0xFF7A4A2A.toInt(), "W", 1f / 256f),
    Outpost(10, R.string.cryo_seedmap_st_outpost, 0xFF8A8F99.toInt(), "P"),
    RuinedPortal(11, R.string.cryo_seedmap_st_ruined_portal, 0xFF9B4DFF.toInt(), "Q", 1f / 16f),
    RuinedPortalNether(12, R.string.cryo_seedmap_st_ruined_portal, 0xFF9B4DFF.toInt(), "Q", 1f / 16f),
    AncientCity(13, R.string.cryo_seedmap_st_ancient_city, 0xFF1E5A66.toInt(), "A"),
    Treasure(14, R.string.cryo_seedmap_st_treasure, 0xFFFFD84A.toInt(), "T", 1f / 16f),
    Mineshaft(15, R.string.cryo_seedmap_st_mineshaft, 0xFFB08A5A.toInt(), "m", 1f / 8f),
    Fortress(18, R.string.cryo_seedmap_st_fortress, 0xFF8A2A2A.toInt(), "F"),
    Bastion(19, R.string.cryo_seedmap_st_bastion, 0xFF2E2E36.toInt(), "B"),
    EndCity(20, R.string.cryo_seedmap_st_end_city, 0xFFD9C6F2.toInt(), "E"),
    TrailRuins(23, R.string.cryo_seedmap_st_trail_ruins, 0xFFC98A5A.toInt(), "R"),
    TrialChambers(24, R.string.cryo_seedmap_st_trial_chambers, 0xFFD87A2E.toInt(), "C"),
    Stronghold(-1, R.string.cryo_seedmap_st_stronghold, 0xFF3A3A3A.toInt(), "S", 1f / 256f);

    /** Dimensão onde a estrutura existe nessa versão, ou null se não existe. */
    fun dimensionIn(mc: Int): SeedDimension? {
        if (this == Stronghold) return SeedDimension.Overworld
        if (!SeedNative.available) return null
        val d = runCatching { SeedNative.structureDim(type, mc) }.getOrDefault(0)
        if (d == 0) return null
        return SeedDimension.entries.firstOrNull { it.id == d - 2 }
    }
}

/** Versão do Minecraft para o cubiomes. */
data class SeedVersion(val mc: Int, val label: String, val approximate: Boolean)

object SeedVersions {
    // valores do enum MCVersion do cubiomes (29+ = versões acrescentadas pelo Launcher)
    private const val MC_1_21_4 = 28
    private const val MC_1_21_5 = 29
    private const val MC_26_2 = 30
    private const val MC_26_3 = 31
    private const val NEWEST = MC_26_3

    /** Biomas 3D (cavernas) existem da 1.18 em diante. */
    fun hasCaveBiomes(mc: Int) = mc >= 22

    fun of(name: String?): SeedVersion {
        val raw = name?.trim().orEmpty()
        val m = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?""").find(raw)
        if (m == null) {
            //beta e snapshots sem número: usa a mais nova
            if (raw.startsWith("b1.7", true)) return SeedVersion(1, "Beta 1.7", false)
            if (raw.startsWith("b1.8", true)) return SeedVersion(2, "Beta 1.8", false)
            return SeedVersion(NEWEST, "26.3", true)
        }
        val major = m.groupValues[1].toInt()
        val minor = m.groupValues[2].toInt()
        val patch = m.groupValues[3].toIntOrNull() ?: 0
        if (major >= 26) {
            //26.x: número do ano + drop
            return when {
                major == 26 && minor <= 1 -> SeedVersion(MC_1_21_5, "26.$minor", false)
                major == 26 && minor == 2 -> SeedVersion(MC_26_2, "26.2", false)
                major == 26 && minor == 3 -> SeedVersion(MC_26_3, "26.3", false)
                else -> SeedVersion(NEWEST, "26.3", true)
            }
        }
        if (major != 1) return SeedVersion(NEWEST, "26.3", true)
        return when {
            minor <= 15 -> SeedVersion(minor + 3, "1.$minor", false)
            minor == 16 -> if (patch <= 1) SeedVersion(19, "1.16.1", false) else SeedVersion(20, "1.16.5", false)
            minor == 17 -> SeedVersion(21, "1.17", false)
            minor == 18 -> SeedVersion(22, "1.18", false)
            minor == 19 -> if (patch <= 2) SeedVersion(23, "1.19.2", false) else SeedVersion(24, "1.19.4", false)
            minor == 20 -> SeedVersion(25, "1.20", false)
            minor == 21 -> when {
                patch <= 1 -> SeedVersion(26, "1.21.1", false)
                patch <= 3 -> SeedVersion(27, "1.21.3", false)
                patch == 4 -> SeedVersion(MC_1_21_4, "1.21.4", false)
                else -> SeedVersion(MC_1_21_5, "1.21.$patch", false)
            }
            else -> SeedVersion(NEWEST, "26.3", true)
        }
    }
}

/** Nomes e cores dos biomas. */
object SeedBiomes {
    private val colors: IntArray by lazy {
        val raw = if (SeedNative.available) runCatching { SeedNative.biomeColors() }.getOrNull() else null
        IntArray(256) { i ->
            if (raw == null || raw.size < 768) (0xFF shl 24) or ((i * 0x9E3779B1.toInt()) ushr 8 and 0xFFFFFF)
            else (0xFF shl 24) or ((raw[i * 3].toInt() and 0xFF) shl 16) or ((raw[i * 3 + 1].toInt() and 0xFF) shl 8) or (raw[i * 3 + 2].toInt() and 0xFF)
        }
    }

    fun color(id: Int): Int = if (id in 0..255) colors[id] else 0xFF000000.toInt()

    fun ids(mc: Int, dim: SeedDimension): List<Int> =
        if (!SeedNative.available) emptyList()
        else runCatching { SeedNative.biomeIds(mc, dim.id)?.toList() }.getOrNull().orEmpty()

    /** "cherry_grove" -> "Cherry Grove". */
    fun name(mc: Int, id: Int): String {
        val raw = if (SeedNative.available) runCatching { SeedNative.biomeName(mc, id) }.getOrNull() else null
        return raw?.split('_')?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } } ?: "#$id"
    }
}
