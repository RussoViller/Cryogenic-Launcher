package com.russoviller.cryogenic.ui.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onLast
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.game.seedmap.SeedNative
import com.russoviller.cryogenic.game.seedmap.SeedVersions
import com.russoviller.cryogenic.ui.screens.content.chunks.SeedMapContent
import com.russoviller.cryogenic.ui.screens.content.chunks.WorldEntry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w880dp-h400dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R172SeedMap {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun overworld() {
        assumeTrue(SeedNative.available)
        val world = WorldEntry(tmp.newFolder("w"), "Mundo de teste", "1.21.1", null, seed = 3055141959546L)
        //o indicador de carregamento é animação infinita: o relógio anda só quando o teste manda
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF1A1030)).padding(8.dp)) {
                    SeedMapContent(world, 3055141959546L, SeedVersions.of("1.21.1"), onBack = {})
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        val dir = File("/home/claude/build/prev170").apply { mkdirs() }
        File(dir, "r172_seedmap.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        rule.onNode(androidx.compose.ui.test.hasText("Caves")).performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(400)
        rule.waitForIdle()
        File(dir, "r172_seedmap_caves.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Desenha os tiles direto (sem esperar a tela): biomas + vilas + fortalezas. */
    @Test
    fun tilesRender() = render("1.21.1", null, "r172_seedtiles.png")

    @Test
    fun cavesRender() = render("26.3", 0, "r172_seedtiles_caves.png")

    @Test
    fun dappledRender() = render("26.3", null, "r172_seedtiles_263.png")

    private fun render(version: String, caveY: Int?, file: String) {
        assumeTrue(SeedNative.available)
        val mc = SeedVersions.of(version).mc
        val tiles = com.russoviller.cryogenic.ui.screens.content.chunks.SeedMapTiles(
            com.russoviller.cryogenic.ui.screens.content.chunks.NativeSeedSource(mc, 3055141959546L, 0, false, caveY)
        )
        val w = 900; val h = 500
        val s = 1f / 32f
        val left = -w / 2f / s; val top = 1500f - h / 2f / s
        val img = androidx.compose.ui.graphics.ImageBitmap(w, h)
        val types = listOf(com.russoviller.cryogenic.game.seedmap.SeedStructure.Village, com.russoviller.cryogenic.game.seedmap.SeedStructure.Stronghold)
        fun drawAll(): List<Triple<com.russoviller.cryogenic.game.seedmap.SeedStructure, Int, Int>> {
            val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
            var markers = emptyList<Triple<com.russoviller.cryogenic.game.seedmap.SeedStructure, Int, Int>>()
            scope.draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat())) {
                tiles.draw(this, left, top, s)
                markers = tiles.structuresIn(types, left, top, left + w / s, top + h / s, s)
                for ((st, x, z) in markers) {
                    val c = androidx.compose.ui.geometry.Offset((x - left) * s, (z - top) * s)
                    drawCircle(Color.Black, 6f, c)
                    drawCircle(Color(st.color), 4.5f, c)
                }
            }
            return markers
        }
        val t0 = System.currentTimeMillis()
        var markers = drawAll()
        while (System.currentTimeMillis() - t0 < 120_000) {
            Thread.sleep(300)
            markers = drawAll()
            if (!tiles.isBusy()) { Thread.sleep(300); markers = drawAll(); if (!tiles.isBusy()) break }
        }
        println("CRYO seedmap ms=" + (System.currentTimeMillis() - t0) + " markers=" + markers.size)
        tiles.dispose()
        val dir = File("/home/claude/build/prev170").apply { mkdirs() }
        File(dir, file).outputStream().use { img.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        org.junit.Assert.assertTrue(markers.isNotEmpty())
    }
}
