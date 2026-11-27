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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.screens.content.chunks.ChunkEditor
import com.russoviller.cryogenic.ui.screens.content.chunks.WorldEntry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.sin
import kotlin.math.cos

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w900dp-h400dp-land-xxhdpi", shadows = [ShadowMMKV::class], instrumentedPackages = ["com.tencent.mmkv"])
class R170cChunkEraser {
    @get:Rule
    val rule = createComposeRule()

    private val PALETTE = listOf("air", "grass_block", "dirt", "stone", "water", "sand", "oak_leaves", "snow_block", "gravel", "spruce_leaves")

    private fun nbtChunk(heights: IntArray, floors: IntArray, inhabited: Long, cx: Int = 0, cz: Int = 0): ByteArray {
        fun pack(v: IntArray) = LongArray(37).also { l -> v.forEachIndexed { i, h -> l[i / 7] = l[i / 7] or ((h + 64).toLong() shl ((i % 7) * 9)) } }
        val b = ByteArrayOutputStream()
        DataOutputStream(b).apply {
            writeByte(10); writeUTF("")
            writeByte(3); writeUTF("DataVersion"); writeInt(4325)
            writeByte(3); writeUTF("yPos"); writeInt(-4)
            writeByte(4); writeUTF("InhabitedTime"); writeLong(inhabited)
            //seções com blocos de verdade (y 32..111)
            writeByte(9); writeUTF("sections"); writeByte(10); writeInt(5)
            for (sy in 2..6) {
                writeByte(1); writeUTF("Y"); writeByte(sy)
                writeByte(10); writeUTF("block_states")
                writeByte(9); writeUTF("palette"); writeByte(10); writeInt(PALETTE.size)
                PALETTE.forEach { writeByte(8); writeUTF("Name"); writeUTF("minecraft:" + it); writeByte(0) }
                val data = LongArray(256)
                for (i in 0 until 4096) {
                    val x = i and 15; val z = (i shr 4) and 15; val y = sy * 16 + (i shr 8)
                    val h = floors[z * 16 + x]
                    val tree = h > 64 && h < 90 && ((cx * 16 + x) * 7 + (cz * 16 + z) * 13) % 23 == 0
                    val v = when {
                        y < h - 3 -> 3
                        y < h -> 2
                        y == h -> when { h <= 63 -> 5; h > 92 -> 7; h > 84 -> 8; else -> 1 }
                        y <= 62 -> 4
                        tree && y <= h + 4 -> if (h > 78) 9 else 6
                        else -> 0
                    }
                    data[i / 16] = data[i / 16] or (v.toLong() shl ((i % 16) * 4))
                }
                writeByte(12); writeUTF("data"); writeInt(256); data.forEach { writeLong(it) }
                writeByte(0)
                writeByte(10); writeUTF("biomes")
                writeByte(9); writeUTF("palette"); writeByte(8); writeInt(1); writeUTF(if (cx < 0) "minecraft:forest" else "minecraft:plains")
                writeByte(0)
                writeByte(0)
            }
            writeByte(10); writeUTF("Heightmaps")
            for ((n, v) in listOf("WORLD_SURFACE" to heights, "OCEAN_FLOOR" to floors)) {
                writeByte(12); writeUTF(n); val p = pack(v); writeInt(p.size); p.forEach { writeLong(it) }
            }
            writeByte(0)
            writeByte(0)
        }
        return b.toByteArray()
    }

    private fun terrain(x: Int, z: Int): Int =
        (66 + 14 * sin(x / 37.0) * cos(z / 29.0) + 9 * sin((x + z) / 13.0) + 22 * sin(x / 160.0 + 1) * cos(z / 140.0)).toInt()

    private fun makeWorld(): File {
        val world = Files.createTempDirectory("cryoworld").toFile()
        val nbt = ByteArrayOutputStream().also { b ->
            DataOutputStream(b).apply {
                writeByte(10); writeUTF(""); writeByte(10); writeUTF("Data")
                writeByte(3); writeUTF("SpawnX"); writeInt(40)
                writeByte(3); writeUTF("SpawnZ"); writeInt(30)
                writeByte(0); writeByte(0)
            }
        }.toByteArray()
        GZIPOutputStream(File(world, "level.dat").outputStream()).use { it.write(nbt) }
        for (rx in -1..0) for (rz in -1..0) {
            val file = File(world, "region/r.$rx.$rz.mca").apply { parentFile.mkdirs() }
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(8192)
                var sector = 2
                for (i in 0 until 1024) {
                    val cx = rx * 32 + (i and 31)
                    val cz = rz * 32 + (i shr 5)
                    //mundo explorado em volta do spawn
                    if (cx * cx + cz * cz > 26 * 26) continue
                    val h = IntArray(256) { terrain(cx * 16 + (it and 15), cz * 16 + (it shr 4)) }
                    val surf = IntArray(256) {
                        val x = cx * 16 + (it and 15); val z = cz * 16 + (it shr 4)
                        val tree = h[it] > 64 && h[it] < 90 && (x * 7 + z * 13) % 23 == 0
                        maxOf(h[it], 62) + 1 + if (tree) 5 else 0
                    }
                    val near = cx * cx + cz * cz < 25
                    val data = nbtChunk(surf, h, if (near) 600_000L else 200L, cx, cz)
                    val z = ByteArrayOutputStream().also { b -> DeflaterOutputStream(b).use { it.write(data) } }.toByteArray()
                    val payload = ByteArrayOutputStream().also { b -> DataOutputStream(b).apply { writeInt(z.size + 1); writeByte(2); write(z) } }.toByteArray()
                    val count = (payload.size + 4095) / 4096
                    raf.seek(sector * 4096L); raf.write(payload.copyOf(count * 4096))
                    raf.seek(i * 4L); raf.writeInt((sector shl 8) or count)
                    sector += count
                }
            }
        }
        File(world, "DIM-1/region").mkdirs()
        File(world, "DIM-1/region/r.0.0.mca").writeBytes(File(world, "region/r.0.0.mca").readBytes())
        return world
    }

    private fun run(liquid: Boolean) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        File(dir!!).mkdirs()
        AllSettings.cryoInterface2.save(liquid)
        val world = makeWorld()
        repeat(3) {
            val t = System.nanoTime()
            com.russoviller.cryogenic.game.chunks.RegionRenderer.render(File(world, "region/r.0.0.mca"), "minecraft:overworld")
            println("CRYO render ms=" + (System.nanoTime() - t) / 1_000_000)
        }
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0E1726)).padding(8.dp)) {
                    ChunkEditor(WorldEntry(world, "Mapa de teste", "1.21.11", null), onBack = {})
                }
            }
        }
        //espera as regiões carregarem (IO real)
        repeat(40) {
            Thread.sleep(150)
            rule.mainClock.advanceTimeBy(200L)
            rule.waitForIdle()
        }
        val tag = if (liquid) "liquid" else "neon"
        save(dir, "chunks_${tag}_map.png")
        //marca uma faixa passando o dedo
        rule.onRoot().performTouchInput { swipe(Offset(width * 0.25f, height * 0.35f), Offset(width * 0.55f, height * 0.6f), 400) }
        rule.mainClock.advanceTimeBy(300L)
        rule.waitForIdle()
        save(dir, "chunks_${tag}_marked.png")
        //formas: círculo arrastado
        rule.onNodeWithText("Shapes").performClick()
        rule.onNodeWithText("Circle").performClick()
        rule.waitForIdle()
        rule.onRoot().performTouchInput { swipe(Offset(width * 0.35f, height * 0.5f), Offset(width * 0.45f, height * 0.75f), 300) }
        rule.mainClock.advanceTimeBy(300L)
        rule.waitForIdle()
        save(dir, "chunks_${tag}_circle.png")
        //zoom máximo (+) e mínimo (−): não pode travar
        fun press(xDp: Float, yDp: Float, times: Int) = repeat(times) {
            rule.onRoot().performTouchInput { click(Offset(xDp * density, yDp * density)) }
            rule.mainClock.advanceTimeBy(100L)
            rule.waitForIdle()
        }
        val t0 = System.nanoTime()
        press(633f, 303f, 8)
        repeat(10) { Thread.sleep(100); rule.mainClock.advanceTimeBy(100L); rule.waitForIdle() }
        save(dir, "chunks_${tag}_zoomin.png")
        press(633f, 357f, 14)
        repeat(30) { Thread.sleep(150); rule.mainClock.advanceTimeBy(150L); rule.waitForIdle() }
        save(dir, "chunks_${tag}_zoomout.png")
        println("CRYO zoom test ms=" + (System.nanoTime() - t0) / 1_000_000)
    }

    private fun save(dir: String, name: String) {
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun sidebar(liquid: Boolean) {
        val dir = System.getProperty("cryo.previewDir")
        assumeTrue(dir != null)
        AllSettings.cryoInterface2.save(liquid)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0E1726))) {
                    val p = androidx.compose.ui.res.painterResource(com.russoviller.cryogenic.R.drawable.ic_home_filled)
                    val c = androidx.compose.ui.res.painterResource(com.russoviller.cryogenic.R.drawable.ic_cryo_chunks)
                    com.russoviller.cryogenic.ui.components.CryoSidebar(
                        entries = listOf("Início", "Versões", "Download", "Contas", "Configs").mapIndexed { i, t ->
                            com.russoviller.cryogenic.ui.components.CryoNavEntry(p, t, i == 0) {}
                        },
                        bottomEntries = listOf(com.russoviller.cryogenic.ui.components.CryoNavEntry(p, "WebPage", false) {}),
                        footer = "r1.7.0",
                        moreEntries = listOf(com.russoviller.cryogenic.ui.components.CryoNavEntry(c, "Apagar chunks", false) {}),
                        moreLabel = "Mais",
                        backLabel = "Voltar"
                    )
                }
            }
        }
        rule.waitForIdle()
        val tag = if (liquid) "liquid" else "neon"
        save(dir!!, "sidebar_${tag}_p0.png")
        rule.onNodeWithText("Mais").performClick()
        rule.mainClock.advanceTimeBy(600L)
        rule.waitForIdle()
        save(dir, "sidebar_${tag}_p1.png")
    }

    @Test
    fun sidebarNeon() = sidebar(false)

    @Test
    fun sidebarLiquid() = sidebar(true)

    @Test
    fun neon() = run(false)

    @Test
    fun liquid() = run(true)
}
