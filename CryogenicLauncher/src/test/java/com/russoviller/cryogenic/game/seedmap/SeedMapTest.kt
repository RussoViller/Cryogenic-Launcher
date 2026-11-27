package com.russoviller.cryogenic.game.seedmap

import com.russoviller.cryogenic.ui.screens.content.chunks.NativeSeedSource
import com.russoviller.cryogenic.ui.screens.content.chunks.SeedMapTiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** ChunkBase: versão, estruturas por versão e biomas (com a biblioteca compilada para o PC). */
class SeedMapTest {
    private val seed = 3055141959546L

    @Test
    fun versions() {
        assertEquals(26, SeedVersions.of("1.21.1").mc)
        assertEquals(25, SeedVersions.of("1.20.4").mc)
        assertEquals(22, SeedVersions.of("1.18.2").mc)
        assertEquals(18, SeedVersions.of("1.15.2").mc)
        assertEquals(29, SeedVersions.of("26.1").mc)
        assertEquals(29, SeedVersions.of("1.21.11").mc)
        assertEquals(30, SeedVersions.of("26.2").mc)
        assertEquals(31, SeedVersions.of("26.3").mc)
        assertTrue(!SeedVersions.of("26.3").approximate)
        assertTrue(SeedVersions.of("26.4-snapshot-10").approximate)
        assertTrue(!SeedVersions.of("1.21.4").approximate)
    }

    @Test
    fun structuresPerVersion() {
        assumeTrue(SeedNative.available)
        assertNull(SeedStructure.TrialChambers.dimensionIn(SeedVersions.of("1.20.4").mc))
        assertEquals(SeedDimension.Overworld, SeedStructure.TrialChambers.dimensionIn(SeedVersions.of("1.21.1").mc))
        assertNull(SeedStructure.AncientCity.dimensionIn(SeedVersions.of("1.18.2").mc))
        assertEquals(SeedDimension.Nether, SeedStructure.Bastion.dimensionIn(SeedVersions.of("1.16.5").mc))
        assertNull(SeedStructure.Bastion.dimensionIn(SeedVersions.of("1.15.2").mc))
        assertEquals(SeedDimension.End, SeedStructure.EndCity.dimensionIn(SeedVersions.of("1.21.1").mc))
    }

    @Test
    fun biomesAndStructures() {
        assumeTrue(SeedNative.available)
        val mc = SeedVersions.of("1.21.1").mc
        val src = NativeSeedSource(mc, seed, 0, false)
        try {
            val one = src.biomes(4, 0, 0, 1, 1)
            assertNotNull(one)
            assertEquals("Deep Cold Ocean", SeedBiomes.name(mc, one!![0]))
            val villages = src.structures(SeedStructure.Village.type, -2048, -2048, 2047, 2047, 64)!!
            val list = villages.toList().chunked(2).map { it[0] to it[1] }
            assertTrue(list.toString(), (-944 to 640) in list && (0 to 864) in list)
            val sh = src.strongholds(3)!!
            assertEquals(-1916, sh[0]); assertEquals(196, sh[1])
            //cerejeira existe na 1.20+, não na 1.19
            val cherry = SeedBiomes.ids(mc, SeedDimension.Overworld).map { SeedBiomes.name(mc, it) }
            assertTrue(cherry.toString(), "Cherry Grove" in cherry)
            assertTrue("Cherry Grove" !in SeedBiomes.ids(SeedVersions.of("1.19.2").mc, SeedDimension.Overworld).map { SeedBiomes.name(mc, it) })
        } finally {
            src.releaseThread()
        }
    }

    @Test
    fun netherAndEnd() {
        assumeTrue(SeedNative.available)
        val mc = SeedVersions.of("1.21.1").mc
        val nether = NativeSeedSource(mc, seed, -1, false)
        val end = NativeSeedSource(mc, seed, 1, false)
        try {
            val f = nether.structures(SeedStructure.Fortress.type, -4096, -4096, 4095, 4095, 256)!!
            val b = nether.structures(SeedStructure.Bastion.type, -4096, -4096, 4095, 4095, 256)!!
            assertTrue(f.isNotEmpty() && b.isNotEmpty())
            val e = end.structures(SeedStructure.EndCity.type, -6000, -6000, 6000, 6000, 256)!!
            assertTrue(e.isNotEmpty())
            //cidade do End nunca perto do centro (ilha principal)
            for (i in e.indices step 2) assertTrue(e[i].toLong() * e[i] + e[i + 1].toLong() * e[i + 1] >= 1008L * 1008)
            //Overworld não devolve estrutura do Nether
            val ow = NativeSeedSource(mc, seed, 0, false)
            assertEquals(0, ow.structures(SeedStructure.Bastion.type, -4096, -4096, 4095, 4095, 256)!!.size)
            ow.releaseThread()
        } finally {
            nether.releaseThread()
            end.releaseThread()
        }
    }

    /** 26.3: pontos conferidos com o /locate do servidor oficial 26.3 (mesma seed). */
    @Test
    fun newBiomes263() {
        assumeTrue(SeedNative.available)
        val mc = SeedVersions.of("26.3").mc
        val names = SeedBiomes.ids(mc, SeedDimension.Overworld).map { SeedBiomes.name(mc, it) }
        assertTrue("Dappled Forest" in names && "Sulfur Caves" in names)
        val old = SeedBiomes.ids(SeedVersions.of("26.1").mc, SeedDimension.Overworld).map { SeedBiomes.name(mc, it) }
        assertTrue("Dappled Forest" !in old && "Sulfur Caves" !in old)
        fun at(x: Int, y: Int, z: Int): String {
            val src = NativeSeedSource(mc, seed, 0, false, caveY = y)
            try { return SeedBiomes.name(mc, src.biomes(4, x shr 2, z shr 2, 1, 1)!![0]) } finally { src.releaseThread() }
        }
        assertEquals("Dappled Forest", at(224, 83, 2576))
        assertEquals("Sulfur Caves", at(928, 19, 1392))
        assertEquals("Pale Garden", at(-800, 83, 1904))
        assertEquals("Cherry Grove", at(-928, 83, 1424))
        //tesouro e mina conferidos com o /locate (estruturas mais perto do spawn)
        val src = NativeSeedSource(mc, seed, 0, false)
        try {
            val t = src.structures(SeedStructure.Treasure.type, 0, -1024, 2047, 1023, 512)!!.toList().chunked(2)
            assertTrue(t.toString(), listOf(729, -119) in t)
            val m = src.structures(SeedStructure.Mineshaft.type, 0, 0, 2047, 2047, 512)!!.toList().chunked(2)
            assertTrue(listOf(112, 800) in m)
        } finally {
            src.releaseThread()
        }
    }

    @Test
    fun spots() {
        // grade 6x6 com duas manchas do bioma 7
        val g = IntArray(36)
        g[0] = 7; g[1] = 7; g[6] = 7
        g[35] = 7; g[34] = 7
        val res = SeedMapTiles.findSpots(g, 6, setOf(7), 0, 0, 16, 0, 0)
        assertEquals(2, res.size)
        assertTrue(res[0].distance <= res[1].distance)
        assertEquals(3 * 256, res[0].area)
    }
}
