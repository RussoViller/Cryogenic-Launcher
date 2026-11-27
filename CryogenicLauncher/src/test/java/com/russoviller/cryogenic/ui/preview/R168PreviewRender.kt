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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.cryoBackdropSource
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlassShape
import com.russoviller.cryogenic.ui.components.cryoShockwave
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
class R168PreviewRender {
    @get:Rule
    val rule = createComposeRule()

    private fun backdrop(): Modifier = Modifier.drawBehind {
        drawRect(Brush.linearGradient(listOf(Color(0xFF0B2A4A), Color(0xFF3A1458), Color(0xFF0E4A3C))))
        val step = 26.dp.toPx()
        var x = 0f
        var i = 0
        while (x < size.width) {
            drawRect(if (i % 2 == 0) Color(0xFFFFB347) else Color(0xFF38C8FF), Offset(x, 0f), Size(step * 0.35f, size.height))
            x += step; i++
        }
        drawCircle(Color(0xFFFF4FA0), radius = size.height * 0.28f, center = Offset(size.width * 0.3f, size.height * 0.55f))
        drawCircle(Color.White, radius = size.height * 0.12f, center = Offset(size.width * 0.72f, size.height * 0.35f))
    }

    init {
        //o Skia do Robolectric é mais antigo e só entende sample(shader, p)
        com.russoviller.cryogenic.ui.components.agslSource = { it.replace(Regex("content\\.eval\\(")) { "sample(content, " } }
    }

    private fun save(name: String) {
        val dir = System.getProperty("cryo.previewDir")!!
        File(dir).mkdirs()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Desenha o fundo de teste num Bitmap (para os shaders lerem como imagem). */
    private fun backdropBitmap(w: Int, h: Int): android.graphics.Bitmap {
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        p.shader = android.graphics.LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(0xFF0B2A4A.toInt(), 0xFF3A1458.toInt(), 0xFF0E4A3C.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        val step = 70f
        var x = 0f; var i = 0
        while (x < w) { p.color = if (i % 2 == 0) 0xFFFFB347.toInt() else 0xFF38C8FF.toInt(); c.drawRect(x, 0f, x + step * 0.35f, h.toFloat(), p); x += step; i++ }
        p.color = 0xFFFF4FA0.toInt(); c.drawCircle(w * 0.3f, h * 0.55f, h * 0.28f, p)
        p.color = 0xFFFFFFFF.toInt(); c.drawCircle(w * 0.72f, h * 0.35f, h * 0.12f, p)
        return bmp
    }

    /** Matemática dos shaders (lente e onda) aplicada direto num Bitmap: mostra o efeito real. */
    @Test
    fun shaderMath() {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        val dir = File(System.getProperty("cryo.previewDir")!!).apply { mkdirs() }
        val w = 2400; val h = 1110
        val bg = backdropBitmap(w, h)
        val legacy = { src: String -> src.replace(Regex("content\\.eval\\(")) { "sample(content, " } }
        fun bgShader(left: Float, top: Float) = android.graphics.BitmapShader(bg, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
            .apply { setLocalMatrix(android.graphics.Matrix().apply { setTranslate(-left, -top) }) }
        fun lens(pw: Float, ph: Float, radius: Float, left: Float, top: Float) =
            android.graphics.RuntimeShader(legacy(com.russoviller.cryogenic.ui.components.LENS_SHADER)).apply {
                setInputShader("content", bgShader(left, top))
                val edge = minOf(72f, minOf(pw, ph) * 0.34f)
                setFloatUniform("size", pw, ph)
                setFloatUniform("radius", radius)
                setFloatUniform("edge", edge)
                setFloatUniform("strength", edge * 1.1f)
                setFloatUniform("dispersion", 0.22f)
            }
        val panels = listOf(
            floatArrayOf(150f, 200f, 900f, 660f, 84f),
            floatArrayOf(1250f, 220f, 780f, 192f, 96f),
            floatArrayOf(1450f, 560f, 360f, 360f, 180f)
        )
        var waveMode by androidx.compose.runtime.mutableStateOf(false)
        rule.setContent {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / w
                val sy = size.height / h
                if (!waveMode) {
                    drawImage(bg.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
                    for (pnl in panels) {
                        val (l, t, pw, ph, r) = pnl.toList()
                        withTransform({ scale(sx, sy, Offset.Zero); translate(l, t) }) {
                            drawRoundRect(androidx.compose.ui.graphics.ShaderBrush(lens(pw, ph, r, l, t)), size = Size(pw, ph), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
                            drawRoundRect(Color(0x2E0A1A33), size = Size(pw, ph), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
                        }
                    }
                } else {
                    val ws = android.graphics.RuntimeShader(legacy(com.russoviller.cryogenic.ui.components.SHOCKWAVE_SHADER)).apply {
                        setInputShader("content", bgShader(0f, 0f))
                        setFloatUniform("uCenter", w / 2f, h / 2f)
                        setFloatUniform("uRadius", w * 0.3f)
                        setFloatUniform("uWidth", 46f * 3f)
                        setFloatUniform("uAmp", 28f * 3f)
                    }
                    withTransform({ scale(sx, sy, Offset.Zero) }) {
                        drawRect(androidx.compose.ui.graphics.ShaderBrush(ws), size = Size(w.toFloat(), h.toFloat()))
                    }
                }
            }
        }
        rule.waitForIdle()
        save("lens.png")
        waveMode = true
        rule.waitForIdle()
        save("wave.png")
    }

    @Test
    fun liquidGlass() {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        AllSettings.cryoInterface2.save(true)
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().cryoBackdropSource().then(backdrop()))
                    Row(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(300.dp, 220.dp).cryoGlass(shape = cryoGlassShape(28.dp), cornerRadius = 28.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Liquid Glass", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.size(260.dp, 64.dp).cryoGlass(shape = cryoGlassShape(32.dp), cornerRadius = 32.dp), contentAlignment = Alignment.Center) {
                                Text("Pílula", color = Color.White)
                            }
                            Box(Modifier.size(120.dp).cryoGlass(shape = cryoGlassShape(60.dp), cornerRadius = 60.dp))
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        save("glass.png")
    }

    @Test
    fun shockwave() {
        assumeTrue(System.getProperty("cryo.previewDir") != null)
        rule.setContent {
            Box(Modifier.fillMaxSize().cryoShockwave({ 0.3f }, { 1f }).then(backdrop()))
        }
        rule.waitForIdle()
        save("wave.png")
    }
}
