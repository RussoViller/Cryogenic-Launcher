package com.russoviller.cryogenic.game.multirt

import android.app.Application
import com.russoviller.cryogenic.path.PathManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/** libjsound.so do APK é (re)instalado no runtime antes de cada launch. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RuntimesManagerJsoundTest {
    @Test
    fun ensureJsoundCopiesUpdatesAndSkips() {
        val work = Files.createTempDirectory("jsound").toFile()
        PathManager.DIR_MULTIRT = File(work, "runtimes").apply { mkdirs() }
        val nativeDir = File(work, "native").apply { mkdirs() }
        PathManager.DIR_NATIVE_LIB = nativeDir.absolutePath
        val local = File(nativeDir, "libjsound.so").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        // JRE 17+: <runtime>/lib
        val jre21 = File(PathManager.DIR_MULTIRT, "jre-21").apply { File(this, "lib").mkdirs() }
        val rt21 = Runtime("jre-21", "21", "aarch64", 21, true, false)
        assertTrue(RuntimesManager.ensureJsound(rt21))
        val t21 = File(jre21, "lib/libjsound.so")
        assertArrayEquals(local.readBytes(), t21.readBytes())
        // igual: não copia de novo
        assertFalse(RuntimesManager.ensureJsound(rt21))
        // APK novo com lib diferente (mesmo tamanho): atualiza
        local.writeBytes(byteArrayOf(9, 9, 9))
        assertTrue(RuntimesManager.ensureJsound(rt21))
        assertArrayEquals(local.readBytes(), t21.readBytes())

        // JRE 8: <runtime>/jre/lib/<arch>
        val jre8 = File(PathManager.DIR_MULTIRT, "jre-8").apply {
            File(this, "jre/lib/aarch64").mkdirs()
            File(this, "bin").mkdirs(); File(this, "bin/javac").writeText("")
        }
        val rt8 = Runtime("jre-8", "1.8.0", "aarch64", 8, true, true)
        assertTrue(RuntimesManager.ensureJsound(rt8))
        assertArrayEquals(local.readBytes(), File(jre8, "jre/lib/aarch64/libjsound.so").readBytes())

        // runtime inexistente: nada acontece
        assertFalse(RuntimesManager.ensureJsound(Runtime("nope", null, "aarch64", 21, false, false)))
    }
}
