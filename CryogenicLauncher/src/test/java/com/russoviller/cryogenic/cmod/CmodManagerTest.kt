package com.russoviller.cryogenic.cmod

import com.russoviller.cryogenic.path.PathManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CmodManagerTest {
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("cmodtest").toFile()
        PathManager.DIR_FILES_PRIVATE = dir
    }

    private val json = """{"id":"test.mod","name":"Test","version":"1.0","author":"me","description":"d","main":"a.B","apiVersion":1}"""

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { z ->
            entries.forEach { (name, data) ->
                z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    @Test
    fun parseInfo() {
        val info = CmodInfo.parse(json)
        assertEquals("test.mod", info.id)
        assertEquals("a.B", info.main)
        assertEquals(1, info.apiVersion)
        listOf(
            """{"id":"a","name":"x","main":"a.B"}""",          // id curto
            """{"id":"../evil","name":"x","main":"a.B"}""",    // id com barra
            """{"id":"good.id","name":"x"}"""                  // sem main
        ).forEach {
            try { CmodInfo.parse(it); fail("aceitou $it") } catch (_: Exception) {}
        }
    }

    @Test
    fun installKeepsOnlyFormatFiles() {
        val info = CmodManager.install(ByteArrayInputStream(zip(
            "cmod.json" to json.toByteArray(),
            "classes.dex" to byteArrayOf(1, 2, 3),
            "classes2.dex" to byteArrayOf(4),
            "assets/data.txt" to "ok".toByteArray(),
            "lib/evil.so" to byteArrayOf(9),
            "META-INF/MANIFEST.MF" to byteArrayOf(9)
        )))
        assertEquals("test.mod", info.id)
        val modDir = File(dir, "cmods/test.mod")
        assertTrue(File(modDir, "classes.dex").isFile)
        assertTrue(File(modDir, "classes2.dex").isFile)
        assertEquals("ok", File(modDir, "assets/data.txt").readText())
        assertFalse(File(modDir, "lib").exists())
        assertFalse(File(modDir, "META-INF").exists())
        assertTrue(File(dir, "cmods_data/test.mod").isDirectory)
        // nenhuma pasta temporária sobrando
        assertTrue(File(dir, "cmods").list()!!.none { it.startsWith(".install") })
    }

    @Test
    fun installRejectsBadFiles() {
        val bad = listOf(
            zip("classes.dex" to byteArrayOf(1)),                                   // sem cmod.json
            zip("cmod.json" to json.toByteArray()),                                 // sem dex
            zip("cmod.json" to "nope".toByteArray(), "classes.dex" to byteArrayOf(1)),
            zip("cmod.json" to json.toByteArray(), "classes.dex" to byteArrayOf(1), "assets/../../x" to byteArrayOf(1)),
            "not a zip".toByteArray()
        )
        bad.forEachIndexed { i, bytes ->
            try {
                CmodManager.install(ByteArrayInputStream(bytes))
                fail("aceitou o arquivo $i")
            } catch (_: CmodManager.InvalidCmod) {}
        }
        assertFalse(File(dir, "cmods/test.mod").exists())
        assertFalse(File(dir, "x").exists())
    }

    @Test
    fun exampleCmodFromSdkInstalls() {
        val example = File("../cmod-sdk/out/russoviller.example.cmod")
        if (!example.isFile) return
        val info = example.inputStream().use { CmodManager.install(it) }
        assertEquals("russoviller.example", info.id)
        assertEquals("com.russoviller.cmodexample.ExampleMod", info.main)
        assertTrue(File(dir, "cmods/russoviller.example/classes.dex").length() > 0)
        assertTrue(File(dir, "cmods/russoviller.example/icon.png").isFile)
    }
}
