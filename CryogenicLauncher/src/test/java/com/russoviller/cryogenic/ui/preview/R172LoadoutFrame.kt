package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutArt
import com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutDivider
import com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutFrame
import com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutKicker
import com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutPrimaryButton
import com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutTitle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Painel lateral com fonte grande: o botão do rodapé tem que continuar visível. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h400dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R172LoadoutFrame {
    @get:Rule
    val rule = createComposeRule()

    private fun render(fontScale: Float, name: String) {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A1030))) {
                        LoadoutFrame(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            side = {
                                LoadoutKicker(text = "LOADOUT // VERSÃO")
                                LoadoutTitle(text = "1.21.1 Shaders Test", maxLines = 2, large = false)
                                LoadoutArt(gameVersion = "1.21.1", modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 36.dp))
                                Text("1.21.1", color = Color.White)
                                LoadoutDivider()
                                LoadoutKicker(text = "INSTALADO AGORA")
                                Text("Fabric 0.19.5", color = Color.White)
                                LoadoutKicker(text = "ALTERAÇÕES PENDENTES: 1")
                                Text("~ Fabric 0.19.5 → 0.20.1", color = Color.White)
                            },
                            footer = {
                                LoadoutPrimaryButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Iniciar modificação",
                                    iconRes = R.drawable.ic_check,
                                    onClick = {}
                                )
                            }
                        ) {}
                    }
                }
            }
        }
        rule.waitForIdle()
        val dir = File("/home/claude/build/prev170").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        rule.onNodeWithText("Iniciar modificação").assertIsDisplayed()
    }

    @Test fun bigFont() = render(1.6f, "r172_loadout_bigfont")
    @Test fun normalFont() = render(1.0f, "r172_loadout_normal")
}
