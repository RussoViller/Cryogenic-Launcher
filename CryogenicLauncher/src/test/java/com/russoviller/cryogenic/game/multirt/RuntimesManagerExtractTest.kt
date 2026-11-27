package com.russoviller.cryogenic.game.multirt

import android.app.Application
import com.russoviller.cryogenic.path.PathManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Extração do Java embutido (bin pack .tar.xz): usa os arquivos reais do APK quando
 * -Dcryo.jreDir aponta para uma pasta com universal.tar.xz e bin-arm64.tar.xz.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RuntimesManagerExtractTest {
    private lateinit var jreDir: File
    private lateinit var work: File

    @Before
    fun setUp() {
        val dir = System.getProperty("cryo.jreDir")
        assumeTrue(dir != null)
        jreDir = File(dir!!)
        work = Files.createTempDirectory("multirt").toFile()
        PathManager.DIR_MULTIRT = File(work, "runtimes").apply { mkdirs() }
        PathManager.DIR_NATIVE_LIB = work.absolutePath
    }

    private fun install(universal: File, bins: File) = runBlocking {
        universal.inputStream().use { u ->
            bins.inputStream().use { b ->
                RuntimesManager.installRuntimeBinPack(u, b, "jre-test", "2")
            }
        }
    }

    private fun runtimeDir() = File(PathManager.DIR_MULTIRT, "jre-test")
    private fun tempDir() = File(PathManager.DIR_MULTIRT, ".jre-test.part")

    @Test
    fun extractValidCorruptAndNotXz() {
        val universal = File(jreDir, "universal.tar.xz")
        val bins = File(jreDir, "bin-arm64.tar.xz")

        // 1) arquivo válido: instala completo, com o marcador "version"
        install(universal, bins)
        assertTrue(File(runtimeDir(), "bin/java").exists())
        assertEquals("2", File(runtimeDir(), "version").readText())
        assertTrue(RuntimesManager.isRuntimeComplete("jre-test"))
        assertFalse(tempDir().exists())

        // 2) dados corrompidos no meio do .xz: erro de leitura, nada parcial fica para trás
        //    e a instalação boa anterior continua no lugar
        val corrupt = File(work, "corrupt.tar.xz")
        val bytes = bins.readBytes()
        for (i in 0 until 256) {
            val pos = bytes.size / 2 + i
            bytes[pos] = (bytes[pos].toInt() xor 0x5A).toByte()
        }
        corrupt.writeBytes(bytes)
        try {
            install(universal, corrupt)
            fail("corrupted archive was accepted")
        } catch (e: IOException) {
            println("CORRUPT -> ${e.javaClass.name}: ${e.message}")
        }
        assertFalse(tempDir().exists())
        assertTrue(File(runtimeDir(), "bin/java").exists())
        assertEquals("2", File(runtimeDir(), "version").readText())

        // 3) não é XZ (ex.: página de erro): recusado antes de extrair
        val html = File(work, "page.tar.xz").apply { writeText("<html><body>404</body></html>") }
        try {
            install(html, bins)
            fail("non-XZ file was accepted")
        } catch (e: RuntimesManager.NotXZArchiveException) {
            println("NOT_XZ -> ${e.message}")
        }
        assertFalse(tempDir().exists())

        // 4) arquivo cortado no meio (download/gravação interrompidos)
        val cut = File(work, "cut.tar.xz").apply { writeBytes(bins.readBytes().copyOf(bins.length().toInt() / 3)) }
        try {
            install(universal, cut)
            fail("truncated archive was accepted")
        } catch (e: IOException) {
            println("TRUNCATED -> ${e.javaClass.name}: ${e.message}")
        }
        assertFalse(tempDir().exists())

        // 5) pasta temporária perdida (processo morto) não aparece como runtime
        tempDir().mkdirs()
        assertFalse(RuntimesManager.getRuntimes(forceLoad = true).any { it.name.startsWith(".") })

        // um runtime sem o executável não é considerado completo
        File(runtimeDir(), "bin/java").delete()
        assertFalse(RuntimesManager.isRuntimeComplete("jre-test"))
        work.deleteRecursively()
    }
}
