package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.XModeButton
import com.russoviller.cryogenic.ui.components.XModeMenu
import com.russoviller.cryogenic.ui.components.cryoNeonBorder
import com.russoviller.cryogenic.ui.screens.game.ReactorCore
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renderiza o menu do jogo e o núcleo da tela de inicialização no computador, para conferir o visual. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h370dp-land-xxhdpi")
class R155PreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(1200L)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun dock() {
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF2A3A2E))) {
                    fun b(icon: Int, label: String, active: Boolean = false) = XModeButton(icon, label, active = active, onClick = {})
                    XModeMenu(
                        visible = true,
                        closeScreen = {},
                        topLabel = "FORÇAR SAÍDA",
                        onTopAction = {},
                        leftButtons = listOf(
                            b(R.drawable.ic_videogame_asset_outlined, "Controles"), b(R.drawable.ic_touch_app_filled, "Gestos"),
                            b(R.drawable.ic_setting_launcher, "Teclado"), b(R.drawable.ic_memory, "Mouse"),
                            b(R.drawable.ic_video_settings, "Giroscópio")
                        ),
                        rightButtons = listOf(
                            b(R.drawable.ic_video_settings, "Resolução e Hotbar"), b(R.drawable.ic_memory, "Menu flutuante", true),
                            b(R.drawable.ic_cryo_stream, "Modo streamer"), b(R.drawable.ic_rocket_launch_filled, "FPS e Memória"),
                            b(R.drawable.ic_java, "Log")
                        )
                    )
                }
            }
        }
        shot("dock")
    }

    @Test
    fun reactor() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF05070A)), contentAlignment = Alignment.Center) {
                ReactorCore(Modifier.size(196.dp), progress = 1f, drift = 0.37f, blink = 0.8f, ready = false)
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(40.dp)
                        .size(180.dp, 110.dp)
                        .background(Color(0xFF1B2430), RoundedCornerShape(22.dp))
                        .cryoNeonBorder(22.dp)
                )
            }
        }
        shot("reactor")
    }
}
