package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.XModeButton
import com.russoviller.cryogenic.ui.components.XModeMenu
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
class R170PreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private fun render(name: String, liquid: Boolean, swipe: Boolean = false) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        File(dir!!).mkdirs()
        AllSettings.cryoInterface2.save(liquid)
        val left = listOf(
            XModeButton(R.drawable.ic_dashboard_filled, "Controles", panel = {}),
            XModeButton(R.drawable.ic_touch_app_filled, "Gestos", panel = {}),
            XModeButton(R.drawable.ic_keyboard_filled, "Teclado", onClick = {}),
            XModeButton(R.drawable.ic_mouse_filled, "Mouse", panel = {}),
            XModeButton(R.drawable.ic_mobile_rotate_filled, "Giroscópio", panel = {}),
            XModeButton(R.drawable.ic_cryo_layers, "Camadas do HUD", panel = {})
        )
        val right = listOf(
            XModeButton(R.drawable.ic_video_settings, "Resolução e Hotbar", panel = {}),
            XModeButton(R.drawable.ic_cryo_menu, "Menu flutuante", active = true, panel = {}),
            XModeButton(R.drawable.ic_cryo_stream, "Modo streamer", panel = {}),
            XModeButton(R.drawable.ic_rocket_launch_filled, "FPS e Memória", panel = {}),
            XModeButton(R.drawable.ic_terminal_outlined, "Log", onClick = {}),
            XModeButton(R.drawable.ic_music_note, "Música", panel = {})
        )
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF6EA8D8), Color(0xFF3F7A3A), Color(0xFF8B6A3F))))
                ) {
                    XModeMenu(
                        visible = true,
                        closeScreen = {},
                        topLabel = "FORÇAR SAÍDA",
                        onTopAction = {},
                        leftButtons = left,
                        rightButtons = right,
                        rightPageButtons = listOf(
                            XModeButton(R.drawable.ic_cryo_texture, "Texturas", panel = {}),
                            XModeButton(R.drawable.ic_cryo_shader, "Shaders", panel = {}),
                            XModeButton(R.drawable.ic_public, "ChunkBase", panel = {})
                        )
                    )
                }
            }
        }
        rule.mainClock.advanceTimeBy(1200L)
        rule.waitForIdle()
        if (swipe) {
            //arrasta a asa direita para a página seguinte (dedo na asa, da direita para a esquerda)
            rule.onRoot().performTouchInput { swipe(Offset(width * 0.84f, height * 0.85f), Offset(width * 0.62f, height * 0.85f), 300) }
            rule.mainClock.advanceTimeBy(1500L)
            rule.waitForIdle()
        }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (swipe) rule.onNodeWithText("ChunkBase").assertIsDisplayed()
    }

    @Test
    fun menuNeon() = render("menu_neon.png", false)

    @Test
    fun menuPages() = render("menu_pages.png", false, swipe = true)

    @Test
    fun menuLiquid() = render("menu_liquid.png", true)
}
