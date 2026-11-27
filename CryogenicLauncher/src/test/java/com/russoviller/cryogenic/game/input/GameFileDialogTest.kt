package com.russoviller.cryogenic.game.input

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GameFileDialogTest {

    private fun mimes() {
        val map = shadowOf(MimeTypeMap.getSingleton())
        map.addExtensionMimeTypeMapping("wav", "audio/x-wav")
        map.addExtensionMimeTypeMapping("mp3", "audio/mpeg")
        map.addExtensionMimeTypeMapping("ogg", "audio/ogg")
    }

    @Test
    fun mimeTypes() {
        mimes()
        assertArrayEquals(arrayOf("*/*"), GameFileDialog.mimeTypesFor(null))
        assertArrayEquals(arrayOf("*/*"), GameFileDialog.mimeTypesFor("*.*"))
        assertArrayEquals(arrayOf("*/*"), GameFileDialog.mimeTypesFor("*.wav\n*.unknownext"))
        assertArrayEquals(
            arrayOf("audio/x-wav", "audio/*", "audio/mpeg", "audio/ogg", "application/ogg"),
            GameFileDialog.mimeTypesFor("*.wav\n*.mp3\n*.ogg")
        )
    }

    @Test
    fun fileNames() {
        mimes()
        assertEquals("click.wav", GameFileDialog.sanitizeFileName("click.wav", null))
        assertEquals("a_b_c.mp3", GameFileDialog.sanitizeFileName("a|b:c.mp3", null))
        assertEquals("sound.wav", GameFileDialog.sanitizeFileName("x/y/sound.wav", null))
        assertEquals("file.wav", GameFileDialog.sanitizeFileName(null, "audio/x-wav"))
        assertEquals("beep.mp3", GameFileDialog.sanitizeFileName("beep", "audio/mpeg"))
    }

    /** Fluxo completo no lado Android: thread do jogo pede -> seletor abre -> arquivo copiado -> caminho devolvido. */
    @Test
    fun openCopiesPickedFile() {
        mimes()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        GameFileDialog.attach(activity)
        val source = Files.createTempDirectory("pick").toFile().resolve("Key Click.wav").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val result = AtomicReference<String?>("<none>")
        val game = Thread { result.set(GameFileDialog.open("Select a sound file", null, "*.wav\n*.mp3", "Audio", false)) }
        game.start()

        var started: Intent? = null
        for (i in 0 until 200) {
            ShadowLooper.idleMainLooper()
            started = shadowOf(activity).peekNextStartedActivityForResult()?.intent
            if (started != null) break
            Thread.sleep(10)
        }
        assertNotNull("system picker not launched", started)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, started!!.action)
        assertTrue(started.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.contains("audio/*"))

        shadowOf(activity).receiveResult(started, Activity.RESULT_OK, Intent().setData(Uri.fromFile(source)))
        game.join(5000)
        val path = result.get()
        assertNotNull(path)
        val copied = File(path!!)
        assertEquals("Key Click.wav", copied.name)
        assertTrue(copied.absolutePath.startsWith(File(activity.cacheDir, "file_dialog").absolutePath))
        assertArrayEquals(source.readBytes(), copied.readBytes())
    }

    @Test
    fun cancelReturnsEmpty() {
        mimes()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        GameFileDialog.attach(activity)
        val result = AtomicReference<String?>("<none>")
        val game = Thread { result.set(GameFileDialog.open(null, null, null, null, false)) }
        game.start()
        var started: Intent? = null
        for (i in 0 until 200) {
            ShadowLooper.idleMainLooper()
            started = shadowOf(activity).peekNextStartedActivityForResult()?.intent
            if (started != null) break
            Thread.sleep(10)
        }
        assertNotNull(started)
        shadowOf(activity).receiveResult(started, Activity.RESULT_CANCELED, null)
        game.join(5000)
        assertEquals("", result.get())
    }

    @Test
    fun noActivityIsUnavailable() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        GameFileDialog.attach(activity)
        GameFileDialog.detach(activity)
        val result = AtomicReference<String?>("<none>")
        val game = Thread { result.set(GameFileDialog.open(null, null, null, null, false)) }
        game.start(); game.join(5000)
        assertNull(result.get())
    }
}
