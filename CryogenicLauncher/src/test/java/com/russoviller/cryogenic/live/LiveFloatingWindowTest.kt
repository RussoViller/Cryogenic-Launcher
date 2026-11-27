package com.russoviller.cryogenic.live

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import android.os.SystemClock
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSettings
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w900dp-h400dp-land-xxhdpi")
class LiveFloatingWindowTest {
    private val noop = object : LiveFloatingWindow.Actions {
        override fun setMic(on: Boolean) {}
        override fun setMicVolume(value: Int) {}
        override fun setInternalVolume(value: Int) {}
        override fun setPrivacy(on: Boolean) {}
        override fun stopLive() {}
        override fun openApp() {}
        override fun saveOpacity(value: Int) {}
    }

    private fun texts(v: View): List<TextView> =
        if (v is ViewGroup) (0 until v.childCount).flatMap { texts(v.getChildAt(it)) } else listOfNotNull(v as? TextView)

    @Test
    fun bubbleShowsViewersAndCommentPreviewForEightSeconds() {
        ShadowSettings.setCanDrawOverlays(true)
        val ctx = RuntimeEnvironment.getApplication()
        val window = LiveFloatingWindow(ctx, noop)
        window.show(85)
        assertTrue(window.showing)
        window.update(LiveFloatingWindow.State(status = LiveStatus.LIVE, startedAt = System.currentTimeMillis(), viewers = "128"))
        val root = window.rootView!!
        val all = texts(root)
        assertTrue(all.any { it.text.toString() == "128" })

        window.showPreview(LiveChatMessage(1, "joaozinho", "salve russo!!"))
        val preview = all.first { it.text.toString().contains("salve russo") }
        assertEquals(View.VISIBLE, preview.visibility)
        window.showPreview(LiveChatMessage(2, "mari.pvp", "que pvp limpo"))
        // contador de comentários novos da pílula
        assertTrue(texts(root).any { it.text.toString() == "2" })
        assertEquals("1,2K".replace(",", "") == "12K", true)
        assertTrue(LiveFloatingWindow.formatCount(1234).matches(Regex("1[.,]2K")))
        assertEquals("1K", LiveFloatingWindow.formatCount(1000))
        assertEquals("999", LiveFloatingWindow.formatCount(999))

        System.getProperty("cryo.previewDir")?.let { d ->
            root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            val bmp = Bitmap.createBitmap(root.measuredWidth.coerceAtLeast(1), root.measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply { drawColor(Color.parseColor("#3F7A3A")); root.draw(this) }
            File(d).mkdirs()
            File(d, "live_bubble_preview.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        ShadowLooper.idleMainLooper(7_000L, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(View.VISIBLE, preview.visibility)
        ShadowLooper.idleMainLooper(1_500L, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(View.GONE, preview.visibility)
        window.hide()
        assertTrue(!window.showing)
    }

    private fun all(v: View): List<View> =
        listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()

    private fun layoutRoot(root: View) {
        root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun openChatScrollsBackToOlderMessages() {
        ShadowSettings.setCanDrawOverlays(true)
        val ctx = RuntimeEnvironment.getApplication()
        val window = LiveFloatingWindow(ctx, noop)
        window.show(85)
        window.update(LiveFloatingWindow.State(status = LiveStatus.LIVE, startedAt = System.currentTimeMillis(), viewers = "57", chatState = TikTokChatReader.STATE_OK))
        val root = window.rootView!! as ViewGroup
        // toque na pílula abre o painel
        val bubble = (root.getChildAt(1) as ViewGroup).getChildAt(0)
        val t = SystemClock.uptimeMillis()
        bubble.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 10f, 10f, 0))
        bubble.dispatchTouchEvent(MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, 10f, 10f, 0))

        val messages = (1L..100L).map { LiveChatMessage(it, "user$it", "mensagem número $it") }
        window.updateChat(messages)
        layoutRoot(root)
        val scroll = all(root).filterIsInstance<ScrollView>().first { it.getChildAt(0) is TextView }
        val newPill = texts(root).first { it.text.toString().contains("↓") }
        assertTrue("começa no fim", scroll.scrollY > 0)
        val bottom = scroll.scrollY

        // sobe para ler as antigas
        scroll.scrollTo(0, 0)
        ShadowLooper.idleMainLooper()
        window.updateChat(messages + LiveChatMessage(101, "nova", "chegou agora"))
        layoutRoot(root)
        assertEquals("não puxa para baixo", 0, scroll.scrollY)
        assertEquals(View.VISIBLE, newPill.visibility)
        assertTrue((scroll.getChildAt(0) as TextView).text.toString().contains("mensagem número 1\n"))

        // toca em "novas mensagens": volta ao fim e acompanha
        newPill.performClick()
        ShadowLooper.idleMainLooper()
        assertTrue(scroll.scrollY >= bottom)
        assertEquals(View.GONE, newPill.visibility)
        window.updateChat(messages + LiveChatMessage(101, "nova", "chegou agora") + LiveChatMessage(102, "outra", "mais uma"))
        layoutRoot(root)
        assertTrue(scroll.scrollY > bottom)
        assertFalse(newPill.visibility == View.VISIBLE)

        System.getProperty("cryo.previewDir")?.let { d ->
            scroll.scrollTo(0, scroll.scrollY - 200)
            ShadowLooper.idleMainLooper()
            val bmp = Bitmap.createBitmap(root.measuredWidth.coerceAtLeast(1), root.measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply { drawColor(Color.parseColor("#3F7A3A")); root.draw(this) }
            File(d).mkdirs()
            File(d, "live_panel_landscape.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        window.hide()
    }
}
