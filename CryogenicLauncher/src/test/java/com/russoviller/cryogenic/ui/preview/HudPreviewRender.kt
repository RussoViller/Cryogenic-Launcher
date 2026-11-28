package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.hud.FclCodec
import com.russoviller.cryogenic.hud.HudDefaults
import com.russoviller.cryogenic.hud.HudEditor
import com.russoviller.cryogenic.hud.HudLayout
import com.russoviller.cryogenic.hud.HudPreview
import com.russoviller.cryogenic.hud.HudRuntime
import com.russoviller.cryogenic.hud.HudRuntimeLayout
import com.russoviller.cryogenic.hud.HudScheme
import com.russoviller.cryogenic.hud.HudSchemeSelector
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w900dp-h400dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class HudPreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private fun dir(): File? = System.getProperty("cryo.previewDir")?.let { File(it).apply { mkdirs() } }

    private fun set(content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF6EA8D8), Color(0xFF3F7A3A), Color(0xFF8B6A3F))))
                ) { content() }
            }
        }
        rule.mainClock.advanceTimeBy(600L)
        rule.waitForIdle()
    }

    private fun save(name: String) {
        repeat(3) {
            rule.mainClock.advanceTimeBy(200L)
            rule.waitForIdle()
        }
        dir()?.let { d ->
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(d, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Composable
    private fun Runtime(layout: HudLayout) {
        val scope = rememberCoroutineScope()
        val rt = androidx.compose.runtime.remember { HudRuntime(layout, scope) { _, _ -> } }
        HudRuntimeLayout(
            modifier = Modifier.fillMaxSize(), runtime = rt, opacity = { 1f }, isCursorGrabbing = true, enabled = true,
            checkOccupiedPointers = { false }, markPointerAsMoveOnly = {}
        ) {}
    }

    private val fclDefault get() = FclCodec.parse(File("src/main/assets/hud/fcl_default.json").readText(), "FCL")

    @Test
    fun runtimeFclStarter() {
        assumeTrue(dir() != null)
        set { Runtime(HudDefaults.starter(HudScheme.FCL)) }
        save("hud_runtime_fcl_starter.png")
    }

    @Test
    fun runtimeFclDefault() {
        assumeTrue(dir() != null)
        set { Runtime(fclDefault) }
        save("hud_runtime_fcl_default.png")
    }

    @Test
    fun runtimePojarushStarterPressed() {
        assumeTrue(dir() != null)
        set { Runtime(HudDefaults.starter(HudScheme.POJARUSH)) }
        // segura o joystick para cima-direita e aperta o pulo
        rule.onRoot().performTouchInput {
            val w = width.toFloat(); val h = height.toFloat()
            val size = 0.44f * h
            val c = Offset(0.04f * (w - size) + size / 2f, 0.93f * (h - size) + size / 2f)
            down(0, c)
            moveTo(0, c + Offset(size * 0.3f, -size * 0.3f))
        }
        save("hud_runtime_pojarush_pressed.png")
    }

    @Test
    fun editorFcl() {
        assumeTrue(dir() != null)
        set { HudEditor(fclDefault, inGame = false, onSave = { true }, onExit = {}) }
        save("hud_editor_fcl.png")
        // seleciona um botão e abre as propriedades
        rule.onRoot().performTouchInput {
            val w = width.toFloat(); val h = height.toFloat()
            val b = fclDefault.buttons.first()
            val bw = b.width.px(width, height, density); val bh = b.height.px(width, height, density)
            click(Offset(b.x * (w - bw) + bw / 2f, b.y * (h - bh) + bh / 2f))
        }
        rule.mainClock.advanceTimeBy(300L)
        rule.onNodeWithText("Properties").performClick()
        save("hud_editor_fcl_props.png")
    }

    @Test
    fun editorPojarushStick() {
        assumeTrue(dir() != null)
        val l = HudDefaults.starter(HudScheme.POJARUSH)
        set { HudEditor(l, inGame = true, onSave = { true }, onExit = {}) }
        rule.onRoot().performTouchInput {
            val w = width.toFloat(); val h = height.toFloat()
            val size = 0.44f * h
            click(Offset(0.04f * (w - size) + size / 2f, 0.93f * (h - size) + size / 2f))
        }
        rule.mainClock.advanceTimeBy(300L)
        rule.onNodeWithText("Properties").performClick()
        save("hud_editor_pojarush_stick.png")
        rule.onNodeWithText("Layers").performClick()
        save("hud_editor_pojarush_layers.png")
        rule.onAllNodesWithText("Layers")[0].performClick()
        rule.onAllNodesWithText("Info")[0].performClick()
        save("hud_editor_info.png")
    }

    @Test
    fun selectorAndPreview() {
        assumeTrue(dir() != null)
        set {
            Column(Modifier.fillMaxSize().background(Color(0xFF050A12)).padding(12.dp)) {
                HudSchemeSelector(HudScheme.FCL, {}, Modifier.fillMaxWidth())
                androidx.compose.foundation.layout.Row(Modifier.padding(top = 12.dp)) {
                    HudPreview(HudDefaults.starter(HudScheme.FCL), Modifier.width(300.dp))
                    HudPreview(fclDefault, Modifier.padding(start = 12.dp).width(300.dp))
                }
            }
        }
        save("hud_selector_preview.png")
    }
}
