package com.russoviller.cryogenic.ui.screens.splash

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renderiza a tela de boas-vindas quadro a quadro no computador (sem celular), para conferir o
 * visual. Só roda quando a pasta de saída é informada:
 *   ./gradlew CryogenicLauncher:testDebugUnitTest --tests '*WelcomePreviewRender*' -Dcryo.previewDir=/caminho
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class WelcomePreviewRender {
    @get:Rule
    val rule = createComposeRule()

    /** [clicks]: momento (ms) -> texto do botão a tocar. */
    private fun render(name: String, stepMs: Long, untilMs: Long, keep: Set<Long>, clicks: Map<Long, String>) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        val out = File(dir!!, name).apply { mkdirs() }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7FD8FF),
                    tertiary = Color(0xFF6FE3D2),
                    secondary = Color(0xFF4FB8F0),
                    background = Color(0xFF061226),
                    onBackground = Color(0xFFE6F7FF),
                    onSurface = Color(0xFFE6F7FF)
                )
            ) {
                WelcomeExperience(
                    modifier = Modifier.fillMaxSize(),
                    isVisible = true,
                    canStart = true,
                    hasTerms = true,
                    onFinished = {}
                ) {
                    Text(text = "☐  Terms of Use  ›\n☐  Privacy Policy  ›", color = Color(0xFFE6F7FF))
                }
            }
        }
        var t = 0L
        var frame = 0
        while (t <= untilMs) {
            rule.mainClock.advanceTimeBy(stepMs)
            t += stepMs
            clicks[t]?.let { label -> rule.onNodeWithText(label, substring = true).performClick() }
            rule.waitForIdle()
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(out, "f%04d.png".format(frame++)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (t in keep) File(out, "t${t}ms.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h370dp-land-xxhdpi")
    fun landscape() = render(
        "landscape", 50L, 11500L,
        keep = setOf(900L, 2600L, 6500L, 7000L, 9000L, 11500L),
        clicks = mapOf(7000L to "NEXT", 9500L to "NEXT")
    )

    @Test
    @Config(qualifiers = "w400dp-h860dp-port-xxhdpi")
    fun portrait() = render(
        "portrait", 250L, 11500L,
        keep = setOf(6500L, 9000L, 11500L),
        clicks = mapOf(7000L to "NEXT", 9500L to "NEXT")
    )
}
