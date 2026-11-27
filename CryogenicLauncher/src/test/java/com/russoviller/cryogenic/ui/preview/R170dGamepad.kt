package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.screens.content.settings.GamepadSettingsPreview
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
class R170dGamepad {
    @get:Rule
    val rule = createComposeRule()

    private fun run(page: Int, name: String, liquid: Boolean = false) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        File(dir!!).mkdirs()
        AllSettings.cryoInterface2.save(liquid)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0E1726))) {
                    GamepadSettingsPreview(page, setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R1, android.view.MotionEvent.AXIS_RTRIGGER))
                }
            }
        }
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun general() = run(0, "gamepad_general.png")
    @Config(qualifiers = "w900dp-h1100dp-land-xxhdpi")
    @Test fun mapping() = run(1, "gamepad_mapping.png")
    @Config(qualifiers = "w700dp-h1100dp-xxhdpi")
    @Test fun mappingLiquid() = run(1, "gamepad_mapping_liquid.png", true)
    @Test fun test() = run(2, "gamepad_test.png")
}
