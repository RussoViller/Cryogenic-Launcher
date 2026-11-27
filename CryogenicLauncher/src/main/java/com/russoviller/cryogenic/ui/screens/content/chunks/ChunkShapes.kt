package com.russoviller.cryogenic.ui.screens.content.chunks

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.russoviller.cryogenic.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Formas da marcação em massa. Pontos em blocos do mundo (não da tela): dá para começar a forma,
 * mover o mapa com o analógico e terminar bem longe, marcando áreas maiores que a tela.
 * - Quadrado: do ponto inicial até o dedo.
 * - Círculo: centro no ponto inicial, raio até o dedo.
 * - Triângulo: dentro do retângulo arrastado, ponta no lado de onde o dedo começou.
 */
internal enum class ChunkShape(val titleRes: Int, val glyph: String) {
    Rect(R.string.cryo_chunks_shape_rect, "▢"),
    Circle(R.string.cryo_chunks_shape_circle, "◯"),
    Triangle(R.string.cryo_chunks_shape_triangle, "△");

    /** Chama [visit] para cada chunk (coordenadas de chunk) cujo centro está dentro da forma. */
    fun forEachChunk(sx: Float, sz: Float, ex: Float, ez: Float, visit: (Int, Int) -> Unit) {
        when (this) {
            Rect -> {
                val x1 = floor(min(sx, ex) / 16f).toInt()
                val x2 = floor(max(sx, ex) / 16f).toInt()
                val z1 = floor(min(sz, ez) / 16f).toInt()
                val z2 = floor(max(sz, ez) / 16f).toInt()
                for (cx in x1..x2) for (cz in z1..z2) visit(cx, cz)
            }
            Circle -> {
                val r = hypot(ex - sx, ez - sz)
                //até meio chunk de raio: pelo menos o chunk do centro
                val rr = max(r, 8f)
                val x1 = floor((sx - rr) / 16f).toInt()
                val x2 = ceil((sx + rr) / 16f).toInt()
                val z1 = floor((sz - rr) / 16f).toInt()
                val z2 = ceil((sz + rr) / 16f).toInt()
                for (cx in x1..x2) for (cz in z1..z2) {
                    val dx = cx * 16f + 8f - sx
                    val dz = cz * 16f + 8f - sz
                    if (dx * dx + dz * dz <= rr * rr) visit(cx, cz)
                }
            }
            Triangle -> {
                val (a, b, c) = triangle(sx, sz, ex, ez)
                val x1 = floor(min(sx, ex) / 16f).toInt()
                val x2 = floor(max(sx, ex) / 16f).toInt()
                val z1 = floor(min(sz, ez) / 16f).toInt()
                val z2 = floor(max(sz, ez) / 16f).toInt()
                for (cx in x1..x2) for (cz in z1..z2) {
                    if (inside(Offset(cx * 16f + 8f, cz * 16f + 8f), a, b, c)) visit(cx, cz)
                }
            }
        }
    }

    companion object {
        /** Vértices do triângulo: ponta no meio do lado onde o arrasto começou, base no lado oposto. */
        fun triangle(sx: Float, sz: Float, ex: Float, ez: Float): Triple<Offset, Offset, Offset> {
            val left = min(sx, ex)
            val right = max(sx, ex)
            val midX = (left + right) / 2f
            return Triple(Offset(midX, sz), Offset(left, ez), Offset(right, ez))
        }

        private fun inside(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
            fun sign(p1: Offset, p2: Offset, p3: Offset) = (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
            val d1 = sign(p, a, b)
            val d2 = sign(p, b, c)
            val d3 = sign(p, c, a)
            val neg = d1 < 0 || d2 < 0 || d3 < 0
            val pos = d1 > 0 || d2 > 0 || d3 > 0
            return !(neg && pos)
        }

        /** Desenha a forma na tela. [toScreen] converte blocos para pixels. */
        fun DrawScope.drawShapePreview(
            shape: ChunkShape,
            sx: Float, sz: Float, ex: Float, ez: Float,
            erase: Boolean,
            toScreen: (Float, Float) -> Offset,
            scale: Float
        ) {
            val color = if (erase) Color(0xFF7FD8FF) else Color(0xFFFF3D5A)
            val fill = color.copy(alpha = 0.18f)
            val stroke = Stroke(2f * density)
            when (shape) {
                Rect -> {
                    val a = toScreen(min(sx, ex), min(sz, ez))
                    val size = Size(abs(ex - sx) * scale, abs(ez - sz) * scale)
                    drawRect(fill, a, size)
                    drawRect(color, a, size, style = stroke)
                }
                Circle -> {
                    val c = toScreen(sx, sz)
                    val r = hypot(ex - sx, ez - sz) * scale
                    drawCircle(fill, r, c)
                    drawCircle(color, r, c, style = stroke)
                    drawCircle(color, 3f * density, c)
                }
                Triangle -> {
                    val (a, b, c) = triangle(sx, sz, ex, ez)
                    val path = Path().apply {
                        val pa = toScreen(a.x, a.y)
                        val pb = toScreen(b.x, b.y)
                        val pc = toScreen(c.x, c.y)
                        moveTo(pa.x, pa.y)
                        lineTo(pb.x, pb.y)
                        lineTo(pc.x, pc.y)
                        close()
                    }
                    drawPath(path, fill)
                    drawPath(path, color, style = stroke)
                }
            }
        }
    }
}
