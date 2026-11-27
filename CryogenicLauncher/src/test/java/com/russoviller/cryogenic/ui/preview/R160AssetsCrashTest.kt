package com.russoviller.cryogenic.ui.preview

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses
import com.russoviller.cryogenic.ui.screens.game.elements.CryoAssetsPanel
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h370dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R160AssetsCrashTest {
    @get:Rule
    val rule = createComposeRule()

    private fun run(classes: PlatformClasses, category: String) {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        rule.setContent { MaterialTheme(colorScheme = darkColorScheme()) { Box(Modifier.fillMaxSize()) { CryoAssetsPanel(classes, null) } } }
        rule.waitUntil(30_000) { rule.onAllNodesWithText("Modrinth").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Modrinth").performClick()
        Thread.sleep(6000); rule.waitForIdle()
        rule.onNodeWithText(category).performClick()
        Thread.sleep(8000); rule.waitForIdle()
        rule.onNodeWithText(category).performClick()
        Thread.sleep(3000); rule.waitForIdle()
    }

    @Test fun textures() = run(PlatformClasses.RESOURCE_PACK, "Vanilla Tweaks")
    @Test fun shaders() = run(PlatformClasses.SHADERS, "Cartoon")

    private fun rapid(classes: PlatformClasses, categories: List<String>) {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        rule.setContent { MaterialTheme(colorScheme = darkColorScheme()) { Box(Modifier.fillMaxSize()) { CryoAssetsPanel(classes, null) } } }
        rule.waitUntil(30_000) { rule.onAllNodesWithText("Modrinth").fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(4000); rule.waitForIdle()
        //toques rápidos: cada um cancela a busca anterior no meio do caminho
        repeat(4) { round ->
            categories.forEach { c ->
                rule.onNodeWithText(c).performClick()
                Thread.sleep(if (round % 2 == 0) 40L else 250L)
            }
        }
        repeat(10) { Thread.sleep(800); rule.waitForIdle() }
    }

    @Test fun texturesRapid() = rapid(PlatformClasses.RESOURCE_PACK, listOf("Combat", "Decoration", "Vanilla Tweaks", "Photorealistic", "Mod Support", "All"))
    @Test fun shadersRapid() = rapid(PlatformClasses.SHADERS, listOf("Cartoon", "Cursed", "Fantasy", "Photorealistic", "Semi-Realistic", "All"))

    private fun typing(classes: PlatformClasses) {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        rule.setContent { MaterialTheme(colorScheme = darkColorScheme()) { Box(Modifier.fillMaxSize()) { CryoAssetsPanel(classes, null) } } }
        rule.waitUntil(30_000) { rule.onAllNodesWithText("Modrinth").fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(3000); rule.waitForIdle()
        //digita uma letra por vez enquanto a busca anterior ainda está carregando
        var text = ""
        "fresh animated".forEach { ch ->
            text += ch
            rule.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextReplacement(text)
            Thread.sleep(if (ch == ' ') 900L else 520L)
        }
        repeat(8) { Thread.sleep(800); rule.waitForIdle() }
    }

    @Test fun texturesTyping() = typing(PlatformClasses.RESOURCE_PACK)
    @Test fun shadersTyping() = typing(PlatformClasses.SHADERS)

    private fun openItem(classes: PlatformClasses) {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        rule.setContent { MaterialTheme(colorScheme = darkColorScheme()) { Box(Modifier.fillMaxSize()) { CryoAssetsPanel(classes, null) } } }
        rule.waitUntil(40_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("cryo_asset_row")).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(androidx.compose.ui.test.hasTestTag("cryo_asset_row"))[0].performClick()
        repeat(15) { Thread.sleep(1000); rule.waitForIdle() }
    }

    @Test fun texturesOpen() = openItem(PlatformClasses.RESOURCE_PACK)
    @Test fun shadersOpen() = openItem(PlatformClasses.SHADERS)
}
