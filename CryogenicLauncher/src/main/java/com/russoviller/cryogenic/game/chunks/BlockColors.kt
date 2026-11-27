package com.russoviller.cryogenic.game.chunks

/**
 * Cores dos blocos vistos de cima (média aproximada da textura do topo), tinta de grama, folhas
 * e água por bioma (valores do jogo) e tradução dos IDs numéricos de antes do 1.13.
 */
internal object BlockColors {
    private const val GRASS = 1
    private const val FOLIAGE = 2
    private const val WATER = 3

    private val INVISIBLE = setOf(
        "air", "cave_air", "void_air", "light", "barrier", "structure_void", "glass", "glass_pane",
        "tripwire", "string", "moving_piston"
    )
    private val WATERY = setOf("water", "flowing_water", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "bubble_column")

    fun isInvisible(name: String) = name in INVISIBLE
    fun isWater(name: String) = name in WATERY

    /** Vidro colorido e afins: deixa ver o bloco de baixo, misturando as cores. */
    fun isTranslucent(name: String) = name.endsWith("stained_glass") || name.endsWith("stained_glass_pane") ||
        name == "tinted_glass" || name == "ice" || name == "slime_block" || name == "honey_block"

    private val DYES = mapOf(
        "white" to 0xE9ECEC, "orange" to 0xF07613, "magenta" to 0xBD44B3, "light_blue" to 0x3AAFD9,
        "yellow" to 0xF8C627, "lime" to 0x70B919, "pink" to 0xED8DAC, "gray" to 0x3E4447,
        "light_gray" to 0x8E8E86, "cyan" to 0x158991, "purple" to 0x792AAC, "blue" to 0x35399D,
        "brown" to 0x724728, "green" to 0x546D1B, "red" to 0xA12722, "black" to 0x141519
    )
    private val DYED = listOf("wool", "carpet", "concrete", "terracotta", "glass", "bed", "banner", "candle", "shulker_box")
    private val TERRACOTTA = mapOf(
        "white" to 0xD1B2A1, "orange" to 0xA15325, "magenta" to 0x95576C, "light_blue" to 0x716D8A,
        "yellow" to 0xBA8523, "lime" to 0x677534, "pink" to 0xA14E4E, "gray" to 0x392A23,
        "light_gray" to 0x876A61, "cyan" to 0x565B5B, "purple" to 0x764656, "blue" to 0x4A3B5B,
        "brown" to 0x4D3323, "green" to 0x4C522A, "red" to 0x8E3C2E, "black" to 0x251610
    )
    private val WOOD = linkedMapOf(
        "dark_oak" to 0x4F3218, "pale_oak" to 0xE4DAD8, "oak" to 0xA2834F, "spruce" to 0x735531,
        "birch" to 0xC0AF79, "jungle" to 0xA07351, "acacia" to 0xA8582E, "mangrove" to 0x773632,
        "cherry" to 0xE3B3AD, "bamboo" to 0xC2AF52, "crimson" to 0x653147, "warped" to 0x2B6963
    )

    private val EXACT: Map<String, Int> = hashMapOf(
        //terra e pedra
        "grass_block" to -GRASS, "short_grass" to -GRASS, "grass" to -GRASS, "tall_grass" to -GRASS,
        "fern" to -GRASS, "large_fern" to -GRASS, "sugar_cane" to -GRASS,
        "dirt" to 0x866043, "coarse_dirt" to 0x77553B, "rooted_dirt" to 0x90674C, "podzol" to 0x5B3F18,
        "mycelium" to 0x6F6265, "dirt_path" to 0x94793E, "grass_path" to 0x94793E, "farmland" to 0x6E4A2A,
        "mud" to 0x3C393D, "packed_mud" to 0x8E6B50, "mud_bricks" to 0x89684F, "clay" to 0xA0A6B3,
        "stone" to 0x7D7D7D, "cobblestone" to 0x7F7F7F, "mossy_cobblestone" to 0x6E765E, "smooth_stone" to 0x9E9E9E,
        "andesite" to 0x888888, "polished_andesite" to 0x848686, "diorite" to 0xBCBCBC, "polished_diorite" to 0xC0C1C2,
        "granite" to 0x956755, "polished_granite" to 0x9A6A59, "tuff" to 0x6C6D66, "calcite" to 0xDFE0DC,
        "deepslate" to 0x505053, "cobbled_deepslate" to 0x4D4D50, "gravel" to 0x837F7E, "bedrock" to 0x555555,
        "dripstone_block" to 0x866B5C, "pointed_dripstone" to 0x866B5C, "obsidian" to 0x0F0A18, "crying_obsidian" to 0x200A3C,
        "sand" to 0xDBD3A0, "suspicious_sand" to 0xDBD3A0, "red_sand" to 0xBE6621, "sandstone" to 0xD8CB9B,
        "cut_sandstone" to 0xD8CB9B, "smooth_sandstone" to 0xE0D6AA, "chiseled_sandstone" to 0xD8CB9B,
        "red_sandstone" to 0xBA631D, "suspicious_gravel" to 0x837F7E,
        "terracotta" to 0x985E44,
        //neve e gelo
        "snow" to 0xF9FEFE, "snow_block" to 0xF9FEFE, "powder_snow" to 0xF8FDFD, "ice" to 0x91B7FD,
        "packed_ice" to 0x8DB4FA, "blue_ice" to 0x74A7FD, "frosted_ice" to 0x8CB4FC,
        //líquidos
        "water" to -WATER, "flowing_water" to -WATER, "seagrass" to -WATER, "tall_seagrass" to -WATER,
        "kelp" to -WATER, "kelp_plant" to -WATER, "bubble_column" to -WATER,
        "lava" to 0xCF5B14, "flowing_lava" to 0xCF5B14,
        //plantas (inclui as novas do 1.21.5+: arbusto, serapilheira, flores silvestres...)
        "bush" to -GRASS, "firefly_bush" to 0x5E7A36, "leaf_litter" to 0x9B6A3A, "wildflowers" to -GRASS,
        "pink_petals" to 0xE7A8C9, "short_dry_grass" to 0xB9A36B, "tall_dry_grass" to 0xB09A62,
        "cactus_flower" to 0xD2829C, "glow_lichen" to 0x7F9C8B, "hanging_roots" to 0x8C6A54,
        "mangrove_roots" to 0x4A3B2A, "muddy_mangrove_roots" to 0x46392E, "dried_kelp_block" to 0x3A4A2A,
        "sea_pickle" to 0x6B7136, "spore_blossom" to 0xCE6EA0, "pale_hanging_moss" to 0x8C9488,
        "torchflower" to 0xC9763B, "pitcher_plant" to 0x6C8FB3, "open_eyeblossom" to 0xB8693A,
        "closed_eyeblossom" to 0x6B6E66, "melon_stem" to -FOLIAGE, "pumpkin_stem" to -FOLIAGE,
        "attached_melon_stem" to -FOLIAGE, "attached_pumpkin_stem" to -FOLIAGE, "cocoa" to 0x8A5A2A,
        "bamboo_sapling" to 0x5B8A2C, "frogspawn" to 0x6B5F5A, "red_mushroom" to 0xC82E2D, "brown_mushroom" to 0x956F51,
        "oak_leaves" to -FOLIAGE, "jungle_leaves" to -FOLIAGE, "acacia_leaves" to -FOLIAGE,
        "dark_oak_leaves" to -FOLIAGE, "mangrove_leaves" to -FOLIAGE, "vine" to -FOLIAGE, "leaves" to -FOLIAGE, "leaves2" to -FOLIAGE,
        "birch_leaves" to 0x80A755, "spruce_leaves" to 0x619961, "azalea_leaves" to 0x5A7225,
        "flowering_azalea_leaves" to 0x6D7A39, "cherry_leaves" to 0xF2BCD3, "pale_oak_leaves" to 0x878D76,
        "azalea" to 0x667D31, "flowering_azalea" to 0x707A40, "moss_block" to 0x596E2D, "moss_carpet" to 0x596E2D,
        "pale_moss_block" to 0x6C7468, "pale_moss_carpet" to 0x6C7468,
        "cactus" to 0x5B8E2E, "pumpkin" to 0xC27419, "carved_pumpkin" to 0xC27419, "melon" to 0x72921E,
        "hay_block" to 0xA68B0C, "lily_pad" to 0x208030, "bamboo" to 0x5B8A2C, "dead_bush" to 0x6B4F29,
        "big_dripleaf" to 0x6F8F33, "small_dripleaf" to 0x6F8F33, "sweet_berry_bush" to 0x3D6B33,
        "brown_mushroom_block" to 0x956F51, "red_mushroom_block" to 0xC82E2D, "mushroom_stem" to 0xCBC4B9,
        "dandelion" to 0xC8C02E, "poppy" to 0x8C3226, "sunflower" to 0xD6B02A, "lilac" to 0x9E7BA3,
        "rose_bush" to 0x8A3B2E, "peony" to 0xA98AA6, "cornflower" to 0x4E73B8, "oxeye_daisy" to 0xC4C9B6,
        "azure_bluet" to 0xA9B8B5, "allium" to 0x9E6BC6, "blue_orchid" to 0x2FA2A8, "lily_of_the_valley" to 0x9DB58C,
        "wheat" to 0xA89A3E, "carrots" to 0x4E8A2B, "potatoes" to 0x5A8E33, "beetroots" to 0x5C7E2E,
        //construção
        "bricks" to 0x966153, "stone_bricks" to 0x7A7A7A, "mossy_stone_bricks" to 0x737960, "cracked_stone_bricks" to 0x767676,
        "deepslate_bricks" to 0x464648, "deepslate_tiles" to 0x363637, "polished_deepslate" to 0x484849,
        "quartz_block" to 0xEAE5DE, "smooth_quartz" to 0xEBE6DF, "quartz_bricks" to 0xEAE5DD, "quartz_pillar" to 0xEBE6E0,
        "prismarine" to 0x63A296, "prismarine_bricks" to 0x63AC9E, "dark_prismarine" to 0x335B4B, "sea_lantern" to 0xACC8BE,
        "glowstone" to 0xFBDA74, "redstone_lamp" to 0x5F3619, "bookshelf" to 0x75603B, "tnt" to 0xDB441A,
        "iron_block" to 0xDCDCDC, "gold_block" to 0xF6D03D, "diamond_block" to 0x62EDE4, "emerald_block" to 0x2ACB57,
        "lapis_block" to 0x1E438C, "redstone_block" to 0xAF1805, "coal_block" to 0x101010, "copper_block" to 0xC06C50,
        "amethyst_block" to 0x8561BF, "budding_amethyst" to 0x8561BF, "slime_block" to 0x6FC05B, "honey_block" to 0xFAB73A,
        "glass" to 0xC0D8DC, "tinted_glass" to 0x2C2730, "bone_block" to 0xE5E1CF, "chest" to 0xA2823F,
        "crafting_table" to 0x81603A, "furnace" to 0x6E6E6E, "rail" to 0x7D6E52, "torch" to 0xD6A045,
        //nether
        "netherrack" to 0x723232, "nether_bricks" to 0x2C1519, "red_nether_bricks" to 0x450709,
        "nether_wart_block" to 0x730303, "warped_wart_block" to 0x167E86, "crimson_nylium" to 0x831F1F,
        "warped_nylium" to 0x2B7265, "soul_sand" to 0x513E32, "soul_soil" to 0x4B3A2E, "basalt" to 0x4D4C51,
        "polished_basalt" to 0x58585B, "smooth_basalt" to 0x484850, "blackstone" to 0x2A2328, "magma_block" to 0x8E3F1F,
        "shroomlight" to 0xF19646, "nether_quartz_ore" to 0x765249, "nether_gold_ore" to 0x73362A, "ancient_debris" to 0x5F4038,
        "crimson_roots" to 0x7E082A, "warped_roots" to 0x14967E, "nether_sprouts" to 0x139478, "weeping_vines" to 0x7D0F0F,
        "twisting_vines" to 0x148F7C, "crimson_fungus" to 0x8C2E1F, "warped_fungus" to 0x1A7A6B, "fire" to 0xD98C1F,
        "soul_fire" to 0x34C2C7, "nether_wart" to 0x6E1517,
        //end
        "end_stone" to 0xDBDE9E, "end_stone_bricks" to 0xDADFA3, "purpur_block" to 0xA97DA9, "purpur_pillar" to 0xAB80AB,
        "chorus_plant" to 0x5D395D, "chorus_flower" to 0x977897, "end_rod" to 0xD9D2CC, "dragon_egg" to 0x0C0910,
        //sculk
        "sculk" to 0x0D1E24, "sculk_vein" to 0x0D1E24, "sculk_catalyst" to 0x0F2A2E, "sculk_sensor" to 0x0F3C44
    )

    /** Cor ARGB do bloco, já tingida pelo bioma quando o bloco é tingível. */
    fun color(name: String, biome: String): Int {
        val raw = EXACT[name] ?: guessed.getOrPut(name) { guess(name) }
        val rgb = when (raw) {
            -GRASS -> grass(biome)
            -FOLIAGE -> foliage(biome)
            -WATER -> water(biome) and 0xFFFFFF
            else -> raw
        }
        return (0xFF shl 24) or (rgb and 0xFFFFFF)
    }

    /** Palpites já feitos (o mesmo nome aparece milhares de vezes por região). */
    private val guessed = java.util.concurrent.ConcurrentHashMap<String, Int>()

    /** Blocos que não estão na tabela: pela cor de tinta, madeira ou material no nome. */
    private fun guess(name: String): Int {
        for ((dye, c) in DYES) if (name.startsWith(dye + "_")) {
            val rest = name.removePrefix(dye + "_")
            //só blocos tingidos (red_sandstone, brown_mushroom... seguem as outras regras)
            if (DYED.none { rest.contains(it) }) continue
            return when {
                rest.contains("terracotta") -> TERRACOTTA.getValue(dye)
                rest.contains("glass") -> c
                else -> c
            }
        }
        for ((wood, c) in WOOD) if (name.startsWith(wood + "_") || name.startsWith("stripped_" + wood + "_")) {
            return when {
                name.endsWith("_log") || name.endsWith("_stem") -> if (name.startsWith("stripped")) c else darkenRgb(c, 0.85f)
                name.endsWith("_leaves") -> 0x5A8A2E
                else -> c
            }
        }
        return when {
            name.endsWith("_ore") -> if (name.startsWith("deepslate")) 0x505053 else 0x7D7D7D
            name.contains("deepslate") -> 0x4A4A4C
            name.contains("blackstone") -> 0x2D282D
            name.contains("sandstone") -> if (name.contains("red")) 0xBA631D else 0xD8CB9B
            name.contains("quartz") -> 0xEAE5DE
            name.contains("purpur") -> 0xA97DA9
            name.contains("prismarine") -> 0x63A296
            name.contains("nether_brick") -> 0x2C1519
            name.contains("end_stone") -> 0xDBDE9E
            name.contains("cobblestone") -> 0x7F7F7F
            name.contains("stone_brick") || name.contains("stone") -> 0x7D7D7D
            name.contains("brick") -> 0x966153
            name.contains("copper") -> if (name.contains("oxidized")) 0x52A284 else if (name.contains("weathered")) 0x6C9D6B else if (name.contains("exposed")) 0xA17E68 else 0xC06C50
            name.contains("coral") -> 0xC8506A
            name.contains("mushroom") -> 0x9C7A5E
            //plantas desconhecidas: verde da grama do bioma (não cinza)
            name.contains("grass") || name.contains("bush") || name.contains("fern") || name.contains("vine") ||
                name.contains("leaves") || name.contains("moss") || name.contains("petals") || name.contains("clover") -> -GRASS
            name.contains("tulip") || name.contains("orchid") || name.contains("flower") || name.contains("blossom") -> 0x7FA04A
            name.contains("sapling") || name.contains("propagule") || name.contains("roots") || name.contains("sprout") -> 0x4E7A2A
            name.contains("ice") -> 0x91B7FD
            name.contains("snow") -> 0xF9FEFE
            name.contains("glass") -> 0xC0D8DC
            name.contains("wool") || name.contains("carpet") -> 0xE9ECEC
            name.contains("candle") || name.contains("torch") || name.contains("lantern") -> 0xD6A045
            name.contains("iron") || name.contains("anvil") || name.contains("chain") -> 0xA8A8A8
            name.contains("gold") -> 0xF6D03D
            else -> 0x8A8A8A
        }
    }

    private fun lighten(c: Int): Int {
        val r = ((c shr 16) and 0xFF) + (255 - ((c shr 16) and 0xFF)) / 3
        val g = ((c shr 8) and 0xFF) + (255 - ((c shr 8) and 0xFF)) / 3
        val b = (c and 0xFF) + (255 - (c and 0xFF)) / 3
        return (r shl 16) or (g shl 8) or b
    }

    private fun darkenRgb(c: Int, k: Float): Int {
        val r = (((c shr 16) and 0xFF) * k).toInt()
        val g = (((c shr 8) and 0xFF) * k).toInt()
        val b = ((c and 0xFF) * k).toInt()
        return (r shl 16) or (g shl 8) or b
    }

    // ------------------------------------------------------------------ biomas

    /** Grama vista de cima: tinta do bioma (a textura já vem um pouco mais escura que a tinta). */
    fun grass(biome: String): Int = darkenRgb(grassTint(biome), 0.86f)

    private fun grassTint(b: String): Int = when {
        b.contains("swamp") -> 0x6A7039
        b.contains("mangrove") -> 0x8DB127
        b.contains("mushroom") -> 0x55C93F
        b.contains("cherry") -> 0xB6DB61
        b.contains("pale_garden") -> 0x778272
        b.contains("dark_forest") || b.contains("roofed") -> 0x507A32
        b.contains("badlands") || b.contains("mesa") -> 0x90814D
        b.contains("desert") || b.contains("savanna") || b.contains("nether") || b.contains("basalt") || b.contains("crimson") || b.contains("warped") || b.contains("soul") -> 0xBFB755
        b.contains("jungle") || b.contains("bamboo") -> if (b.contains("sparse") || b.contains("edge")) 0x64C73F else 0x59C93C
        b.contains("birch") -> 0x88BB67
        b.contains("snowy") || b.contains("frozen") || b.contains("ice") || b.contains("grove") || b.contains("peaks") || b.contains("slopes") -> 0x80B497
        b.contains("old_growth") || b.contains("giant") || b.contains("redwood") -> 0x86B87F
        b.contains("taiga") -> 0x86B783
        b.contains("windswept") || b.contains("mountain") || b.contains("extreme") || b.contains("gravelly") || b.contains("stony") -> 0x8AB689
        b.contains("meadow") -> 0x83BB6D
        b.contains("beach") || b.contains("ocean") || b.contains("river") -> 0x8EB971
        b.contains("forest") || b.contains("flower") -> 0x79C05A
        else -> 0x91BD59
    }

    private fun foliage(b: String): Int = darkenRgb(
        when {
            b.contains("swamp") -> 0x6A7039
            b.contains("mangrove") -> 0x8DB127
            b.contains("dark_forest") || b.contains("roofed") -> 0x59AE30
            b.contains("badlands") || b.contains("mesa") -> 0x9E814D
            b.contains("desert") || b.contains("savanna") -> 0xAEA42A
            b.contains("jungle") || b.contains("bamboo") -> 0x30BB0B
            b.contains("birch") -> 0x6BA941
            b.contains("snowy") || b.contains("frozen") || b.contains("ice") || b.contains("grove") -> 0x60A17B
            b.contains("taiga") -> 0x68A464
            b.contains("windswept") || b.contains("mountain") || b.contains("extreme") -> 0x6DA36B
            b.contains("forest") || b.contains("flower") || b.contains("meadow") -> 0x59AE30
            b.contains("cherry") -> 0xB6DB61
            else -> 0x77AB2F
        }, 0.78f
    )

    fun water(b: String): Int = (0xFF shl 24) or when {
        b.contains("swamp") -> 0x617B64
        b.contains("mangrove") -> 0x3A7A6A
        b.contains("warm_ocean") -> 0x43D5EE
        b.contains("lukewarm") -> 0x45ADF2
        b.contains("frozen") -> 0x3938C9
        b.contains("cold") -> 0x3D57D6
        b.contains("meadow") -> 0x0E4ECF
        b.contains("cherry") -> 0x5DB7EF
        else -> 0x3F76E4
    }

    // ------------------------------------------------------------------ IDs antigos (até 1.12)

    private val LEGACY_BIOMES = mapOf(
        0 to "ocean", 1 to "plains", 2 to "desert", 3 to "mountains", 4 to "forest", 5 to "taiga", 6 to "swamp",
        7 to "river", 8 to "nether", 9 to "the_end", 10 to "frozen_ocean", 11 to "frozen_river", 12 to "snowy_tundra",
        13 to "snowy_mountains", 14 to "mushroom_fields", 15 to "mushroom_field_shore", 16 to "beach", 17 to "desert_hills",
        18 to "wooded_hills", 19 to "taiga_hills", 20 to "mountain_edge", 21 to "jungle", 22 to "jungle_hills",
        23 to "jungle_edge", 24 to "deep_ocean", 25 to "stone_shore", 26 to "snowy_beach", 27 to "birch_forest",
        28 to "birch_forest_hills", 29 to "dark_forest", 30 to "snowy_taiga", 31 to "snowy_taiga_hills",
        32 to "giant_tree_taiga", 33 to "giant_tree_taiga_hills", 34 to "wooded_mountains", 35 to "savanna",
        36 to "savanna_plateau", 37 to "badlands", 38 to "wooded_badlands_plateau", 39 to "badlands_plateau",
        44 to "warm_ocean", 45 to "lukewarm_ocean", 46 to "cold_ocean", 47 to "deep_warm_ocean",
        48 to "deep_lukewarm_ocean", 49 to "deep_cold_ocean", 50 to "deep_frozen_ocean", 129 to "sunflower_plains",
        130 to "desert_lakes", 131 to "gravelly_mountains", 132 to "flower_forest", 133 to "taiga_mountains",
        134 to "swamp_hills", 140 to "ice_spikes", 149 to "modified_jungle", 151 to "modified_jungle_edge",
        155 to "tall_birch_forest", 156 to "tall_birch_hills", 157 to "dark_forest_hills", 158 to "snowy_taiga_mountains",
        160 to "giant_spruce_taiga", 161 to "giant_spruce_taiga_hills", 162 to "modified_gravelly_mountains",
        163 to "shattered_savanna", 164 to "shattered_savanna_plateau", 165 to "eroded_badlands",
        166 to "modified_wooded_badlands_plateau", 167 to "modified_badlands_plateau", 168 to "bamboo_jungle",
        169 to "bamboo_jungle_hills", 170 to "soul_sand_valley", 171 to "crimson_forest", 172 to "warped_forest",
        173 to "basalt_deltas", 174 to "dripstone_caves", 175 to "lush_caves"
    )

    fun legacyBiome(id: Int): String = LEGACY_BIOMES[id] ?: "plains"

    private val LEGACY_DYES = arrayOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    )
    private val LEGACY_WOOD = arrayOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")

    fun legacyBlock(id: Int, meta: Int): String = when (id) {
        0 -> "air"
        1 -> when (meta) { 1, 2 -> "granite"; 3, 4 -> "diorite"; 5, 6 -> "andesite"; else -> "stone" }
        2 -> "grass_block"
        3 -> when (meta) { 1 -> "coarse_dirt"; 2 -> "podzol"; else -> "dirt" }
        4 -> "cobblestone"
        5 -> LEGACY_WOOD.getOrElse(meta and 7) { "oak" } + "_planks"
        7 -> "bedrock"
        8, 9 -> "water"
        10, 11 -> "lava"
        12 -> if (meta == 1) "red_sand" else "sand"
        13 -> "gravel"
        14 -> "gold_ore"
        15 -> "iron_ore"
        16 -> "coal_ore"
        17 -> LEGACY_WOOD.getOrElse(meta and 3) { "oak" } + "_log"
        18 -> when (meta and 3) { 1 -> "spruce_leaves"; 2 -> "birch_leaves"; 3 -> "jungle_leaves"; else -> "oak_leaves" }
        20 -> "glass"
        21 -> "lapis_ore"
        22 -> "lapis_block"
        24 -> "sandstone"
        31 -> if (meta == 0) "dead_bush" else "short_grass"
        32 -> "dead_bush"
        35 -> LEGACY_DYES[meta and 15] + "_wool"
        37 -> "dandelion"
        38 -> "poppy"
        39, 40 -> "brown_mushroom"
        41 -> "gold_block"
        42 -> "iron_block"
        43, 44 -> "smooth_stone"
        45 -> "bricks"
        46 -> "tnt"
        47 -> "bookshelf"
        48 -> "mossy_cobblestone"
        49 -> "obsidian"
        50 -> "torch"
        51 -> "fire"
        53, 134, 135, 136, 163, 164 -> "oak_planks"
        54 -> "chest"
        56 -> "diamond_ore"
        57 -> "diamond_block"
        58 -> "crafting_table"
        59 -> "wheat"
        60 -> "farmland"
        61, 62 -> "furnace"
        66 -> "rail"
        67 -> "cobblestone"
        78 -> "snow"
        79 -> "ice"
        80 -> "snow_block"
        81 -> "cactus"
        82 -> "clay"
        83 -> "sugar_cane"
        85 -> "oak_planks"
        86 -> "pumpkin"
        87 -> "netherrack"
        88 -> "soul_sand"
        89 -> "glowstone"
        95 -> LEGACY_DYES[meta and 15] + "_stained_glass"
        98 -> "stone_bricks"
        99 -> "brown_mushroom_block"
        100 -> "red_mushroom_block"
        102 -> "glass_pane"
        103 -> "melon"
        106 -> "vine"
        108, 109 -> "stone_bricks"
        110 -> "mycelium"
        111 -> "lily_pad"
        112, 113, 114 -> "nether_bricks"
        121 -> "end_stone"
        125, 126 -> LEGACY_WOOD.getOrElse(meta and 7) { "oak" } + "_planks"
        128 -> "sandstone"
        129 -> "emerald_ore"
        133 -> "emerald_block"
        152 -> "redstone_block"
        153 -> "nether_quartz_ore"
        155, 156 -> "quartz_block"
        159 -> LEGACY_DYES[meta and 15] + "_terracotta"
        160 -> LEGACY_DYES[meta and 15] + "_stained_glass"
        161 -> if (meta and 3 == 1) "dark_oak_leaves" else "acacia_leaves"
        162 -> if (meta and 3 == 1) "dark_oak_log" else "acacia_log"
        168 -> "prismarine"
        169 -> "sea_lantern"
        170 -> "hay_block"
        171 -> LEGACY_DYES[meta and 15] + "_carpet"
        172 -> "terracotta"
        173 -> "coal_block"
        174 -> "packed_ice"
        175 -> when (meta and 7) { 0 -> "sunflower"; 1 -> "lilac"; 4 -> "rose_bush"; 5 -> "peony"; else -> "tall_grass" }
        179, 180, 181, 182 -> "red_sandstone"
        201, 202, 203, 204, 205 -> "purpur_block"
        206 -> "end_stone_bricks"
        208 -> "dirt_path"
        213 -> "magma_block"
        214 -> "nether_wart_block"
        215 -> "red_nether_bricks"
        216 -> "bone_block"
        251 -> LEGACY_DYES[meta and 15] + "_concrete"
        252 -> LEGACY_DYES[meta and 15] + "_concrete_powder"
        else -> "stone"
    }
}
