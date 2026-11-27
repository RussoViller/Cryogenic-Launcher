package com.russoviller.cryogenic.cmod

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w900dp-h400dp-land-xxhdpi")
class CmodUiKitTest {

    @Test
    fun buildsLauncherStylePage() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val ui = CmodUiKit(activity)
        assertEquals(Color.WHITE, ui.textColor)
        assertTrue(ui.gradientColors.size >= 2)

        val page = ui.column()
        page.addView(ui.title("Example Tools"))
        page.addView(ui.caption("Mod de exemplo do Cryogenic Launcher (CryoMod API)."))
        val card = ui.card()
        card.addView(ui.title("Informações de arquivo"))
        card.addView(ui.button("Escolher arquivo") {})
        card.addView(ui.text("Nome: mundo.mcworld\nTamanho: 12.4 MB"))
        page.addView(card)
        val card2 = ui.card()
        card2.addView(ui.title("Controles"))
        card2.addView(ui.input("Seu nome"))
        card2.addView(ui.switchButton("Modo turbo", true, null))
        card2.addView(ui.caption("Volume: 50"))
        card2.addView(ui.slider(100, 50, null))
        card2.addView(ui.progress(false).apply { progress = 40 })
        val row = ui.row()
        row.addView(ui.button("Dizer oi", null), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(ui.secondaryButton("Salvar nome", null), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card2.addView(row)
        card2.addView(ui.divider())
        page.addView(card2)
        val root = ui.scroll(page)

        val w = 1500
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val h = page.measuredHeight
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, w, h)
        assertTrue(h > 0)
        System.getProperty("cryo.previewDir")?.let { d ->
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.rgb(8, 20, 38))
            root.draw(c)
            File(d).mkdirs()
            File(d, "cmod_ui_kit.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun api2PiecesAndDialogs() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val ui = CmodUiKit(activity)
        val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val page = ui.column()
        page.addView(ui.image(bmp, 40f, 40f))
        var chipClicks = 0
        val chip = ui.chip("Tag", true) { chipClicks++ }
        page.addView(chip)
        page.addView(ui.checkbox("Check", true, null))
        page.addView(ui.choice(arrayOf("A", "B"), 1, null))
        var itemClicks = 0
        val item = ui.listItem(bmp, "Title", "Sub") { itemClicks++ }
        page.addView(item)
        val grid = ui.grid(3)
        repeat(5) { grid.addView(ui.chip("c$it", false, null)) }
        page.addView(grid)
        chip.performClick(); item.performClick()
        assertEquals(1, chipClicks)
        assertEquals(1, itemClicks)
        assertEquals(3, grid.columnCount)

        var confirmed: Boolean? = null
        ui.confirm("Title", "Message") { confirmed = it }
        val d1 = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertTrue(d1.isShowing)
        fun find(v: View, text: String): View? {
            if (v is android.widget.TextView && v.text.toString() == text) return v
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), text)?.let { return it }
            return null
        }
        find(d1.window!!.decorView, activity.getString(android.R.string.ok))!!.performClick()
        assertEquals(true, confirmed)
        assertTrue(!d1.isShowing)

        var typed: String? = "x"
        ui.prompt("Name", "hint", "abc") { typed = it }
        val d2 = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        find(d2.window!!.decorView, activity.getString(android.R.string.cancel))!!.performClick()
        assertEquals(null, typed)
    }
}
