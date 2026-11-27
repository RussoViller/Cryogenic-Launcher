package com.russoviller.cryogenic.extras

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CryoConfigsTest {
    private val config = CryoConfig(
        id = "abc",
        name = "Modo PvP",
        createdAt = 1L,
        values = mapOf(
            "renderer" to JsonPrimitive("opengles3_mobileglues"),
            "cryoInterface2" to JsonPrimitive(true),
            "resolutionRatio" to JsonPrimitive(85),
            "ramAllocation" to JsonNull,
            "nativeLibPlugins" to JsonArray(listOf(JsonPrimitive("a")))
        )
    )

    @Test
    fun exportRoundTrip() {
        val bytes = CryoConfigs.encodeExport(config)
        assertEquals("CRYOCFG1", bytes.copyOfRange(0, 8).toString(Charsets.US_ASCII))
        val back = CryoConfigs.parseExport(bytes)
        assertEquals("Modo PvP", back.name)
        assertEquals("", back.id)
        assertEquals(config.values, back.values)
    }

    @Test
    fun rejectsOtherFiles() {
        val bad = listOf(
            "hello".toByteArray(),
            "CRYOPRE1{}".toByteArray(),
            "CRYOCFG1 not json".toByteArray(),
            "CRYOCFG1{\"format\":\"other\",\"config\":{\"id\":\"\",\"name\":\"x\",\"createdAt\":0}}".toByteArray()
        )
        bad.forEach { b ->
            val ok = runCatching { CryoConfigs.parseExport(b) }.isSuccess
            assertFalse(String(b), ok)
        }
    }

    @Test
    fun fileNameAndExclusions() {
        assertEquals("Modo-PvP.cryoconf", CryoConfigs.exportFileName(config))
        assertEquals("config.cryoconf", CryoConfigs.exportFileName(config.copy(name = " / ")))
        listOf("launcherBackgroundOpacity", "cryoHeroWallpaperOpacity", "cryoPortraitWallpaperOpacity",
            "cryoFmBackgroundOpacity", "cryoFmWallpaperVolume", "videoBackgroundVolume", "cryoCurseForgeApiKey")
            .forEach { assertTrue(it, it in CryoConfigs.EXCLUDED_KEYS) }
        assertFalse("renderer" in CryoConfigs.EXCLUDED_KEYS)
        assertFalse("cryoInterface2" in CryoConfigs.EXCLUDED_KEYS)
    }

    @Test
    fun inUseComparison() {
        val current = config.values + mapOf("other" to JsonPrimitive(1))
        assertTrue(CryoConfigs.isCurrent(config, current))
        assertFalse(CryoConfigs.isCurrent(config, current + ("resolutionRatio" to JsonPrimitive(100))))
        assertFalse(CryoConfigs.isCurrent(config, emptyMap()))
    }
}
