package com.russoviller.cryogenic.game.cpu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CpuAffinityTest {
    /** Snapdragon 8s Gen 4 (POCO F7): 2x 2.0 GHz, 3x 2.8 GHz, 2x 3.0 GHz, 1x 3.2 GHz (só a forma importa). */
    private fun fakeSys(capacity: Boolean): File {
        val root = Files.createTempDirectory("sys").toFile()
        val freqs = longArrayOf(2016000, 2016000, 2803000, 2803000, 2803000, 3014000, 3014000, 3214000)
        val caps = longArrayOf(400, 400, 870, 870, 870, 950, 950, 1024)
        for (i in freqs.indices) {
            File(root, "cpu$i/cpufreq").mkdirs()
            File(root, "cpu$i/cpufreq/cpuinfo_max_freq").writeText("${freqs[i]}\n")
            if (capacity) File(root, "cpu$i/cpu_capacity").writeText("${caps[i]}\n")
        }
        File(root, "cpufreq").mkdirs() // não é núcleo
        File(root, "possible").writeText("0-7")
        return root
    }

    @Test
    fun readsCapacityOrFrequency() {
        val withCap = CpuAffinity.readCoreScores(fakeSys(true))
        assertEquals(8, withCap.size)
        assertEquals(1024L, withCap[7])
        val freqOnly = CpuAffinity.readCoreScores(fakeSys(false))
        assertEquals(3214000L, freqOnly[7])
    }

    @Test
    fun performanceExcludesEfficiencyCluster() {
        val plan = CpuAffinity.plan(CpuAffinityMode.PERFORMANCE, CpuAffinity.readCoreScores(fakeSys(true)))
        assertTrue(plan.active)
        assertEquals(listOf(2, 3, 4, 5, 6, 7), plan.gameCores)
        assertTrue(plan.renderCores.isEmpty())
        assertEquals(mapOf("CRYO_AFFINITY_GAME" to "2,3,4,5,6,7"), plan.env())
    }

    @Test
    fun maxDedicatesFastestCoreToRender() {
        val plan = CpuAffinity.plan(CpuAffinityMode.MAX, CpuAffinity.readCoreScores(fakeSys(false)))
        assertEquals(listOf(2, 3, 4, 5, 6, 7), plan.gameCores)
        assertEquals(listOf(7), plan.renderCores)
        assertEquals("7", plan.env()["CRYO_AFFINITY_RENDER"])
    }

    @Test
    fun maxWithoutEnoughCoresDoesNotDedicate() {
        // 2 eficiência + 1 desempenho + 1 prime: sobraria só 1 núcleo -> não dedica
        val plan = CpuAffinity.plan(CpuAffinityMode.MAX, mapOf(0 to 1L, 1 to 1L, 2 to 2L, 3 to 3L))
        assertEquals(listOf(2, 3), plan.gameCores)
        assertTrue(plan.renderCores.isEmpty())
    }

    @Test
    fun offAndUniformCpusDoNothing() {
        assertFalse(CpuAffinity.plan(CpuAffinityMode.OFF, mapOf(0 to 1L, 1 to 2L)).active)
        assertFalse(CpuAffinity.plan(CpuAffinityMode.PERFORMANCE, mapOf(0 to 5L, 1 to 5L, 2 to 5L)).active)
        assertFalse(CpuAffinity.plan(CpuAffinityMode.PERFORMANCE, emptyMap()).active)
        assertTrue(CpuAffinity.plan(CpuAffinityMode.PERFORMANCE, emptyMap()).env().isEmpty())
    }
}
