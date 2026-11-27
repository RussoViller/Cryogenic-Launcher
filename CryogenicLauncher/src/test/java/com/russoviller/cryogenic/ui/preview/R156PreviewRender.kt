package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.streamer.StreamText
import com.russoviller.cryogenic.streamer.StreamTextView
import androidx.compose.ui.test.isDialog
import com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker
import com.russoviller.cryogenic.ui.screens.content.elements.ModConflictDialog
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
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h370dp-land-xxhdpi")
class R156PreviewRender {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun texts() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF3A5A3E)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreamTextView(item = StreamText(text = "Meta: 50/100 subs", enabled = true))
                    StreamTextView(item = StreamText(text = "!discord", enabled = true, size = 30, color = 0xFF7FE7FF.toInt(), glow = 70, bg = false))
                    StreamTextView(item = StreamText(text = "AO VIVO", enabled = true, size = 20, color = 0xFFFFFFFF.toInt(), bgColor = 0xFFFF4D4D.toInt(), bgOpacity = 100, bgRound = 30, glow = 30))
                    StreamTextView(item = StreamText(text = "linha 1\nlinha 2", enabled = true, size = 16, bold = false, bgOpacity = 35, bgRound = 0))
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500L)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "texts.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun conflicts() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        fun m(name: String, v: String, f: String) = ModConflictChecker.ScannedMod(File("/x/$f"), name.lowercase(), name, v, setOf(name.lowercase()), emptyList())
        val a = m("Better Combat", "2.0.3", "bettercombat-fabric-2.0.3+1.21.1.jar")
        val b = m("Epic Fight", "21.1.4", "epicfight-21.1.4.jar")
        val c = m("Combat Roll", "1.3.3", "combat_roll-1.3.3.jar")
        val s = m("Sodium", "0.6.0", "sodium-fabric-0.6.0.jar")
        val e = m("Embeddium", "1.0.11", "embeddium-1.0.11.jar")
        val groups = listOf(
            ModConflictChecker.Group(listOf(a, b, c), listOf(
                ModConflictChecker.Link(ModConflictChecker.Kind.DECLARED, listOf(a, b)),
                ModConflictChecker.Link(ModConflictChecker.Kind.DECLARED, listOf(b, c))
            )),
            ModConflictChecker.Group(listOf(s, e), listOf(ModConflictChecker.Link(ModConflictChecker.Kind.KNOWN, listOf(s, e), "renderer")))
        )
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ModConflictDialog(groups = groups, recheck = { groups }, onAllSolved = {}, onContinueAnyway = {}, onCancel = {})
            }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500L)
        rule.waitForIdle()
        val bmp = rule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "conflicts.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun gfxError() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        com.russoviller.cryogenic.ui.screens.game.CryoGraphicsError.gameVersion = "26.3"
        com.russoviller.cryogenic.ui.screens.game.CryoGraphicsError.showForPreview(
            "No supported graphics backend was found.\n\n- Tried OpenGL:\n  glGetError mismatch\n\n- Tried Vulkan:\n  VK_ERROR_LAYER_NOT_PRESENT: Error creating instance\n\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions)."
        )
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                com.russoviller.cryogenic.ui.screens.game.CryoGraphicsErrorOverlay()
            }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(800L)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "gfx.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun tags() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF3B2410)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.ClassesIdentifier(
                        classes = com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses.MOD
                    )
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.ClassesIdentifier(
                        classes = com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses.SHADERS,
                        iconSize = 16.dp,
                        textStyle = MaterialTheme.typography.labelMedium
                    )
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.PlatformIdentifier(
                        platform = com.russoviller.cryogenic.game.download.assets.platform.Platform.MODRINTH
                    )
                    com.russoviller.cryogenic.ui.components.LittleTextLabel(text = "Fabric")
                    com.russoviller.cryogenic.ui.components.LittleTextLabel(
                        text = "Invalid",
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.InstalledModBadge(size = 22.dp)
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.FavoriteIdentifier(isFavorite = false, onClick = {}, iconSize = 16.dp, textStyle = MaterialTheme.typography.labelMedium)
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.FavoriteIdentifier(isFavorite = true, onClick = {}, iconSize = 16.dp, textStyle = MaterialTheme.typography.labelMedium)
                    com.russoviller.cryogenic.ui.screens.content.download.assets.elements.FavoriteToggleLabel(isFavorite = true, onClick = {})
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500L)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "tags.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun panels() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxSize().background(Color(0xFF0B1626)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.russoviller.cryogenic.ui.components.CryoSectionHeader("Atalhos")
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.russoviller.cryogenic.ui.components.CryoActionTile(
                                modifier = Modifier.weight(1f),
                                iconRes = com.russoviller.cryogenic.R.drawable.ic_keyboard_filled,
                                title = "Teclado virtual", subtitle = "Abre o teclado do celular para digitar", onClick = {}
                            )
                            com.russoviller.cryogenic.ui.components.CryoActionTile(
                                modifier = Modifier.weight(1f),
                                iconRes = com.russoviller.cryogenic.R.drawable.ic_cryo_send,
                                title = "Enviar Tecla", subtitle = "Envia uma tecla do PC para o jogo", onClick = {}
                            )
                        }
                        com.russoviller.cryogenic.ui.components.CryoToggleTile(
                            iconRes = com.russoviller.cryogenic.R.drawable.ic_cryo_layers,
                            title = "RUSSO HUD 2°", subtitle = "12 botões · 1 joysticks", checked = true, onCheckedChange = {}
                        )
                        com.russoviller.cryogenic.ui.components.CryoSliderTile(
                            iconRes = com.russoviller.cryogenic.R.drawable.ic_cryo_opacity,
                            title = "Opacidade do HUD", value = 80, valueRange = 0f..100f, onValueChange = {}, onValueChangeFinished = {}, suffix = "%"
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var sel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(1) }
                        com.russoviller.cryogenic.ui.components.CryoSegmentedTabs(
                            titles = listOf("Básico", "Evento do Launcher", "Tecla"), selected = sel, onSelect = { sel = it }
                        )
                        com.russoviller.cryogenic.ui.screens.main.control_editor.InfoLayoutSwitchItem(title = "Alternar", value = true, onValueChange = {})
                        com.russoviller.cryogenic.ui.screens.main.control_editor.InfoLayoutTextItem(title = "Enviar texto", onClick = {})
                    }
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(800L)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "panels.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
