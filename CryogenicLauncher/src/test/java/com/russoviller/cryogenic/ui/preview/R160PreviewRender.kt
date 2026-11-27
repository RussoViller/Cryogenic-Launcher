package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.music.CryoMusicLibrary
import com.russoviller.cryogenic.music.CryoPlayer
import com.russoviller.cryogenic.ui.screens.game.MusicBubble
import com.russoviller.cryogenic.ui.screens.game.MusicCard
import com.russoviller.cryogenic.ui.screens.game.MusicDisc
import com.russoviller.cryogenic.ui.screens.game.MusicSquare
import com.russoviller.cryogenic.ui.screens.game.elements.CryoMusicPanel
import com.russoviller.cryogenic.ui.screens.game.elements.ImportPreviewHost
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
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h370dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R160PreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        rule.setContent { MaterialTheme(colorScheme = darkColorScheme()) { content() } }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(800L)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun song(title: String, artist: String, ms: Long) =
        CryoPlayer.Song("/x/$title.mp3", title, artist, null, null, "", ms, true)

    private val songs = listOf(
        song("Blinding Lights", "The Weeknd", 200_000L),
        song("Do I Wanna Know?", "Arctic Monkeys", 272_000L),
        song("Levitating", "Dua Lipa", 203_000L),
        song("Sweater Weather", "The Neighbourhood", 240_000L),
        song("Starboy", "The Weeknd", 230_000L)
    )

    @Test
    fun panelLibrary() {
        CryoPlayer.previewState(songs, 1, true)
        CryoMusicLibrary.previewState(
            CryoMusicLibrary.Library(
                songs,
                listOf(
                    CryoMusicLibrary.Playlist("Treino", File("/x/Treino"), songs.take(3)),
                    CryoMusicLibrary.Playlist("Lives", File("/x/Lives"), songs.take(2))
                )
            )
        )
        shot("music_library") {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B1424)).padding(12.dp)) { CryoMusicPanel(Modifier.fillMaxSize()) }
        }
    }

    @Test
    fun importFolders() {
        CryoPlayer.previewState(songs, 0, false)
        CryoMusicLibrary.previewState(CryoMusicLibrary.Library(songs, emptyList()))
        shot("music_import_folders") {
            ImportPreviewHost()
        }
    }

    @Test
    fun floating() {
        CryoPlayer.previewState(songs, 0, true)
        shot("music_floating") {
            Row(Modifier.fillMaxSize().background(Color(0xFF4F7A45)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.width(292.dp)) {
                    MusicCard(position = mutableLongStateOf(70_000L), showVolume = false, onToggleVolume = {}, onTouch = {}, onMinimize = {})
                }
                Box(Modifier.width(212.dp).clip(RoundedCornerShape(22.dp))) {
                    MusicSquare(position = mutableLongStateOf(70_000L), showVolume = false, onToggleVolume = {}, onTouch = {}, onMinimize = {})
                }
                Box(Modifier.width(224.dp).clip(CircleShape)) {
                    MusicDisc(position = mutableLongStateOf(70_000L), showVolume = false, onToggleVolume = {}, onTouch = {}, onMinimize = {})
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(54.dp).clip(CircleShape)) { MusicBubble(position = mutableLongStateOf(70_000L)) }
                }
            }
        }
    }

    @Test
    fun crashScreen() {
        shot("crash_screen") {
            com.russoviller.cryogenic.ui.screens.main.ErrorScreen(
                crashType = com.russoviller.cryogenic.ui.activities.CrashType.GAME_CRASH,
                canUpload = true,
                body = { androidx.compose.material3.Text("Exit code: 1") }
            )
        }
    }

    private val logLines = listOf(
        "[12:00:01] [main/INFO]: Loading Minecraft 1.20.1 with Fabric Loader 0.16.9",
        "[12:00:01] [main/INFO]: Loading 42 mods:",
        "[12:00:02] [main/WARN]: Mod 'examplemod' uses a deprecated mixin config",
        "[12:00:03] [Render thread/INFO]: Setting user: Steve",
        "[12:00:04] [Render thread/INFO]: Backend library: LWJGL version 3.3.1",
        "[12:00:04] [Render thread/INFO]: OpenGL: Adreno (TM) 825 GL version 3.2",
        "[12:00:05] [Render thread/INFO]: Reloading ResourceManager: vanilla, fabric",
        "[12:00:06] [Render thread/WARN]: Missing texture minecraft:block/unknown",
        "[12:00:07] [Render thread/ERROR]: Failed to load shader program",
        "java.lang.RuntimeException: Shader compile failed",
        "\tat net.minecraft.client.gl.ShaderProgram.<init>(ShaderProgram.java:120)",
        "\tat net.minecraft.client.render.GameRenderer.loadPrograms(GameRenderer.java:390)",
        "[12:00:08] [Render thread/INFO]: Sound engine started",
        "[12:00:09] [Render thread/INFO]: Created: 1024x512x4 minecraft:textures/atlas/blocks.png-atlas",
        "[12:00:10] [Server thread/INFO]: Starting integrated minecraft server version 1.20.1",
        "[12:00:12] [Server thread/INFO]: Done (1.932s)! For help, type \"help\"",
        "Process exited with exit code 1"
    )

    @Test
    fun logAll() {
        shot("log_all") { com.russoviller.cryogenic.ui.screens.game.elements.LogBoxPreviewHost(logLines, 0) }
    }

    @Test
    fun logErrors() {
        shot("log_errors") { com.russoviller.cryogenic.ui.screens.game.elements.LogBoxPreviewHost(logLines, 3) }
    }

    @Test
    fun exportInfo() {
        shot("export_info") {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFF3E9FF), Color(0xFFFFB3E6), Color(0xFF9FD8FF))))) {
                com.russoviller.cryogenic.ui.screens.content.versions.export.ExportInfoPreviewHost(
                    com.russoviller.cryogenic.game.version.export.ExportInfo(
                        gamePath = File("/x"),
                        name = "Cryo Pack",
                        version = "1.0.0",
                        author = "",
                        minMemory = 4096,
                        packType = com.russoviller.cryogenic.game.version.export.PackType.CurseForge,
                        packCurseForge = true
                    )
                )
            }
        }
    }

    @Test
    fun streamerHome() {
        shot("streamer_home") {
            Box(Modifier.fillMaxSize().background(Color(0xFF2A1A0C)).padding(24.dp)) {
                com.russoviller.cryogenic.streamer.StreamerPanel(
                    modifier = Modifier.fillMaxSize(),
                    onAdjustPositions = {},
                    color = Color(0xFF4A2E14),
                    contentColor = Color.White
                )
            }
        }
    }

    @Test
    fun assetsPanel() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF203040)).padding(16.dp)) {
                    com.russoviller.cryogenic.ui.screens.game.elements.CryoAssetsPanel(com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses.SHADERS, null)
                }
            }
        }
        rule.waitUntil(40_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("cryo_asset_row")).fetchSemanticsNodes().isNotEmpty() }
        repeat(8) { Thread.sleep(1000); rule.waitForIdle() }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "assets_panel.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val navSel = androidx.compose.runtime.mutableIntStateOf(0)

    @Composable
    private fun Ui2Sample() {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3B1D6E), Color(0xFFE56BC0), Color(0xFF7FD0FF), Color(0xFF2A3FB0))))) {
            //"wallpaper" com formas, para ver a transparência do vidro
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 14) {
                    drawCircle(Color.White.copy(alpha = 0.35f), radius = 40f + i * 9f, center = androidx.compose.ui.geometry.Offset(size.width * ((i * 37 % 100) / 100f), size.height * ((i * 61 % 100) / 100f)))
                }
            }
            Row(Modifier.fillMaxSize()) {
                val names = listOf("Início", "Versões", "Download", "Contas", "Configs")
                com.russoviller.cryogenic.ui.components.CryoSidebar(
                    entries = names.mapIndexed { i, n ->
                        com.russoviller.cryogenic.ui.components.CryoNavEntry(
                            icon = androidx.compose.ui.res.painterResource(com.russoviller.cryogenic.R.drawable.ic_star_filled),
                            label = n, selected = navSel.intValue == i, onClick = { navSel.intValue = i }
                        )
                    },
                    bottomEntries = emptyList(),
                    footer = "r1.6.5"
                )
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        com.russoviller.cryogenic.ui.components.Interface2Button()
                        com.russoviller.cryogenic.ui.components.CryoButton(onClick = {}) { androidx.compose.material3.Text("Jogar") }
                    }
                    com.russoviller.cryogenic.ui.components.CryoTabStrip(
                        tabs = listOf("Minecraft", "Mods", "Shaders").mapIndexed { i, n ->
                            com.russoviller.cryogenic.ui.components.CryoTab(icon = {}, label = n, selected = navSel.intValue % 3 == i, onClick = {})
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(250.dp, 130.dp).then(glass()).padding(16.dp)) {
                            androidx.compose.material3.Text("Minecraft Java", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                        Column(Modifier.width(230.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.russoviller.cryogenic.ui.components.CryoToggleTile(modifier = Modifier, iconRes = com.russoviller.cryogenic.R.drawable.ic_star_filled, title = "Opção ligada", subtitle = "Liquid Glass", checked = true, onCheckedChange = {})
                            com.russoviller.cryogenic.ui.components.CryoToggleTile(modifier = Modifier, iconRes = com.russoviller.cryogenic.R.drawable.ic_star_filled, title = "Opção desligada", subtitle = "Liquid Glass", checked = false, onCheckedChange = {})
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun glass(): Modifier {
        return with(com.russoviller.cryogenic.ui.components.GlassAccess) { mod() }
    }

    @Test
    fun interface2() {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        File(dir!!).mkdirs()
        com.russoviller.cryogenic.setting.AllSettings.cryoInterface2.save(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize()) {
                    Ui2Sample()
                    com.russoviller.cryogenic.ui.components.Interface2Cutscene(modifier = Modifier.fillMaxSize())
                }
            }
        }
        fun snap(name: String) {
            rule.waitForIdle()
            val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
            File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        rule.mainClock.advanceTimeBy(300L)
        snap("ui2_0_classic.png")
        rule.runOnUiThread { com.russoviller.cryogenic.ui.components.CryoInterface2.toggle() }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(4000L); snap("ui2_1_star.png")
        rule.mainClock.advanceTimeBy(2800L); snap("ui2_2_charge.png")
        rule.mainClock.advanceTimeBy(500L); snap("ui2_3a_boom.png")
        rule.mainClock.advanceTimeBy(700L); snap("ui2_3b_burst.png")
        rule.mainClock.advanceTimeBy(500L); snap("ui2_3c_fireworks.png")
        rule.mainClock.advanceTimeBy(600L); snap("ui2_4_title.png")
        rule.mainClock.advanceTimeBy(2300L); snap("ui2_5_liquid.png")
        //gota da barra lateral indo de "Início" para "Contas"
        rule.runOnUiThread { navSel.intValue = 3 }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(130L); snap("ui2_6_blob_moving.png")
        rule.mainClock.advanceTimeBy(1500L); snap("ui2_7_blob_done.png")
        assert(com.russoviller.cryogenic.setting.AllSettings.cryoInterface2.getValue())
        assert(!com.russoviller.cryogenic.ui.components.CryoInterface2.playing)
    }
}
