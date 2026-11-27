package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.screens.game.CryoMusicFloatingPlayer
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
class R170bMusicFloat {
    @get:Rule
    val rule = createComposeRule()

    private fun run(liquid: Boolean, style: Int = 0) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        File(dir!!).mkdirs()
        AllSettings.cryoInterface2.save(liquid)
        AllSettings.cryoMusicFloating.save(true)
        AllSettings.cryoMusicFloatingStyle.save(style)
        AllSettings.cryoMusicBubbleRight.save(false)
        AllSettings.cryoMusicBubbleY.save(500)
        com.russoviller.cryogenic.ui.components.CryoTheme.inGame = true
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF3F7A3A))) {
                    CryoMusicFloatingPlayer(modifier = Modifier.fillMaxSize())
                }
            }
        }
        rule.waitForIdle()
        val tag = (if (liquid) "liquid" else "neon") + style
        save(dir, "float_${tag}_bubble.png")
        //toque na bolinha (lado esquerdo, meio da altura)
        rule.onRoot().performTouchInput { click(Offset(8.dp2px(this) + 27.dp2px(this), height / 2f)) }
        rule.mainClock.advanceTimeBy(800L)
        rule.waitForIdle()
        save(dir, "float_${tag}_open.png")
    }

    private fun Int.dp2px(scope: androidx.compose.ui.test.TouchInjectionScope) = this * scope.density

    private fun save(dir: String, name: String) {
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun neon() = run(false)

    @Test
    fun liquid() = run(true)

    @Test
    fun liquidSquare() = run(true, 1)

    @Test
    fun liquidDisc() = run(true, 2)
}
