package com.russoviller.cryogenic.sponsor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A janela do Launcher tem que bater com a regra do .github/workflows/sponsors.yml. */
class SponsorScheduleTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    /** Regra do Action: roda às 00:00 (UTC-3) nos dias em que (dia desde 1970, em UTC-3) % 3 == 0. */
    private fun actionRunsOn(localDay: Long) = Math.floorMod(localDay, 3L) == 0L

    @Test
    fun windowIs72hAndStartsOnActionDays() {
        var t = 1_790_000_000_000L // 2026
        repeat(400) {
            val start = CryoSponsors.slotStart(t)
            val local = start - 3 * hour
            assertEquals("começa às 00:00 em UTC-3", 0L, Math.floorMod(local, day))
            assertTrue("começa num dia em que o Action roda", actionRunsOn(Math.floorDiv(local, day)))
            assertTrue(start <= t && t < start + 72 * hour)
            assertEquals(start + 72 * hour, CryoSponsors.nextUpdate(t))
            t += 7 * hour + 13 * 60_000L
        }
    }
}
