package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.live.LiveChatMessage
import com.russoviller.cryogenic.live.LiveChatOverlay
import com.russoviller.cryogenic.live.LiveClient
import com.russoviller.cryogenic.live.LiveConfig
import com.russoviller.cryogenic.live.LiveConfigStore
import com.russoviller.cryogenic.live.LivePanel
import com.russoviller.cryogenic.live.LiveStatus
import com.russoviller.cryogenic.live.TikTokLiveLauncherContent
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w900dp-h400dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class LivePreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private fun dir(): File? = System.getProperty("cryo.previewDir")?.let { File(it).apply { mkdirs() } }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF6EA8D8), Color(0xFF3F7A3A), Color(0xFF8B6A3F))))
                ) { content() }
            }
        }
        rule.mainClock.advanceTimeBy(800L)
        rule.waitForIdle()
        dir()?.let { d ->
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(d, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun seedConfig() {
        val ctx = RuntimeEnvironment.getApplication()
        LiveConfig.save(ctx, LiveConfig(serverUrl = "rtmp://push-rtmp-l11-va01.tiktokcdn.com/game/", streamKey = "stream-123?expire=1", tiktokUser = "russoviller.mc"))
        LiveConfigStore.reload(ctx)
    }

    @Test
    fun panelHome() {
        assumeTrue(dir() != null)
        LiveClient.applyTestState(LiveStatus.IDLE, 0L, false, true, emptyList())
        shot("live_panel_home.png") {
            LivePanel(Modifier.fillMaxSize().padding(60.dp), {}, {}, Color(0x99223344), Color.White)
        }
        rule.onNodeWithText("TikTok", substring = true).assertIsDisplayed()
        rule.onNodeWithText("TikTok", substring = true).performClick()
        rule.mainClock.advanceTimeBy(600L)
        rule.waitForIdle()
        dir()?.let { d ->
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(d, "live_panel_tiktok.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun launcherScreenLive() {
        assumeTrue(dir() != null)
        seedConfig()
        LiveClient.applyTestState(
            LiveStatus.LIVE, System.currentTimeMillis() - 754_000L, privacy = true, micOn = false,
            messages = listOf(
                LiveChatMessage(1, "joaozinho", "salve russo!!"),
                LiveChatMessage(2, "mari.pvp", "que pvp limpo"),
                LiveChatMessage(3, "", "entrou na live")
            )
        )
        shot("live_launcher_screen.png") {
            TikTokLiveLauncherContent(Modifier.fillMaxSize().padding(12.dp), Color(0x99223344), Color.White)
        }
    }

    @Test
    fun launcherScreenIdle() {
        assumeTrue(dir() != null)
        seedConfig()
        LiveClient.applyTestState(LiveStatus.IDLE, 0L, privacy = false, micOn = true, messages = emptyList())
        shot("live_launcher_idle.png") {
            TikTokLiveLauncherContent(Modifier.fillMaxSize().padding(12.dp), Color(0x99223344), Color.White)
        }
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Open in full screen").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(600L)
        rule.waitForIdle()
        rule.onNodeWithText("Exit full screen", substring = true).assertExists()
        rule.onNodeWithText("Exit full screen", substring = true).performClick()
        rule.mainClock.advanceTimeBy(600L)
        rule.waitForIdle()
        rule.onNodeWithText("Exit full screen", substring = true).assertDoesNotExist()
    }

    @Test
    fun gameTikTokPanel() {
        assumeTrue(dir() != null)
        seedConfig()
        LiveClient.applyTestState(LiveStatus.LIVE, System.currentTimeMillis() - 65_000L, privacy = false, micOn = true, messages = emptyList())
        shot("live_game_tiktok.png") {
            com.russoviller.cryogenic.live.TikTokLivePanel(
                Modifier.padding(start = 300.dp, top = 10.dp, bottom = 10.dp, end = 20.dp).fillMaxSize(),
                Color(0x99223344), Color.White
            )
        }
    }

    @Test
    fun configsDialog() {
        assumeTrue(dir() != null)
        shot("configs_dialog.png") {
            com.russoviller.cryogenic.ui.screens.content.extras.ConfigsDialogContent(Modifier.fillMaxSize().padding(12.dp)) {}
        }
    }

    @Test
    fun optionsMenu() {
        assumeTrue(dir() != null)
        shot("options_menu.png") {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                com.russoviller.cryogenic.ui.components.CryoMenuPanel {
                    fun item(text: String, icon: Int, enabled: Boolean = true, destructive: Boolean = false): @Composable () -> Unit = {
                        com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem(
                            text = { androidx.compose.material3.Text(text) },
                            onClick = {},
                            enabled = enabled,
                            destructive = destructive,
                            leadingIcon = {
                                androidx.compose.material3.Icon(
                                    modifier = Modifier.padding(0.dp).then(Modifier),
                                    painter = androidx.compose.ui.res.painterResource(icon),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    item("Quick play", com.russoviller.cryogenic.R.drawable.ic_play_arrow_filled)()
                    item("Rename", com.russoviller.cryogenic.R.drawable.ic_edit_filled)()
                    item("Backup save", com.russoviller.cryogenic.R.drawable.ic_save_filled, enabled = false)()
                    com.russoviller.cryogenic.ui.components.CryoMenuDivider()
                    item("Delete", com.russoviller.cryogenic.R.drawable.ic_delete_filled, destructive = true)()
                }
            }
        }
    }

    @Test
    fun launcherModsEmpty() {
        assumeTrue(dir() != null)
        com.russoviller.cryogenic.path.PathManager.DIR_FILES_PRIVATE = java.nio.file.Files.createTempDirectory("cmodprev").toFile()
        com.russoviller.cryogenic.cmod.CmodManager.reloadForTest()
        shot("launcher_mods_empty.png") {
            com.russoviller.cryogenic.cmod.LauncherModsContent(Modifier.fillMaxSize().padding(12.dp), onDismiss = {})
        }
    }

    @Test
    fun launcherModsList() {
        assumeTrue(dir() != null)
        val example = java.io.File("../cmod-sdk/out/russoviller.example.cmod")
        assumeTrue(example.isFile)
        com.russoviller.cryogenic.path.PathManager.DIR_FILES_PRIVATE = java.nio.file.Files.createTempDirectory("cmodprev").toFile()
        example.inputStream().use { com.russoviller.cryogenic.cmod.CmodManager.install(it) }
        com.russoviller.cryogenic.cmod.CmodManager.reloadForTest()
        shot("launcher_mods_list.png") {
            com.russoviller.cryogenic.cmod.LauncherModsContent(Modifier.fillMaxSize().padding(12.dp), onDismiss = {})
        }
    }

    @Test
    fun chatOverlay() {
        assumeTrue(dir() != null)
        seedConfig()
        LiveClient.applyTestState(
            LiveStatus.LIVE, System.currentTimeMillis() - 65_000L, privacy = false, micOn = true,
            messages = (1..10).map { LiveChatMessage(it.toLong(), "viewer$it", "mensagem número $it do chat da live") }
        )
        shot("live_chat_overlay.png") { LiveChatOverlay(Modifier.fillMaxSize()) }
    }
}
