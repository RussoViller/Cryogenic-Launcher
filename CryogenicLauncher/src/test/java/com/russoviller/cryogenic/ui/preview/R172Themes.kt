package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import com.materialkolor.PaletteStyle
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.cryoSpecFor
import com.russoviller.cryogenic.ui.screens.content.settings.ThemeStudioDialog
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import com.russoviller.cryogenic.ui.theme.CryogenicLauncherTheme
import com.russoviller.cryogenic.ui.theme.launcherColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Tema Prateado e cor personalizada branca / prata / preta. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w880dp-h400dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R172Themes {
    @get:Rule val rule = createComposeRule()

    private fun hsvSat(c: Color): Float {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(c.toArgb(), hsv)
        return hsv[1]
    }

    @Test
    fun neutralCustomColors() {
        val ctx = RuntimeEnvironment.getApplication()
        for ((seed, name) in listOf(Color.White to "branco", Color(0xFFB0B4BA) to "prata", Color.Black to "preto")) {
            val scheme = launcherColorScheme(ctx, ColorThemeType.CUSTOM, true, seed, PaletteStyle.TonalSpot)
            val spec = cryoSpecFor(ColorThemeType.CUSTOM, scheme, seed)
            //sem matiz inventado
            assertTrue("$name ice ${spec.ice}", hsvSat(spec.ice) < 0.12f)
            assertTrue("$name primary ${scheme.primary}", hsvSat(scheme.primary) < 0.12f)
            //detalhe visível no fundo escuro
            assertTrue("$name ice escuro demais", spec.ice.luminance() > spec.navy.luminance() + 0.25f)
            if (name == "preto") assertEquals(Color.Black, spec.deep)
        }
    }

    @Test
    fun silverStudio() {
        AllSettings.launcherColorTheme.save(ColorThemeType.SILVER)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CryogenicLauncherTheme {
                ThemeStudioDialog(onDismiss = {}, onEditCustomColor = {})
            }
        }
        rule.mainClock.advanceTimeBy(800)
        val dir = File("/home/claude/build/prev170").apply { mkdirs() }
        File(dir, "r172_theme_silver.png").outputStream().use {
            rule.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
