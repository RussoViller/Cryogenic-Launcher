package com.russoviller.cryogenic.ui.screens.content.download.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.russoviller.cryogenic.ui.components.safeRadialGradient

enum class VersionEmblem(val color: Color) {
    SPARK(Color(0xFF7FE7FF)),
    KEY(Color(0xFFE88A3A)),
    BLOSSOM(Color(0xFFFF8FC7)),
    LEAF(Color(0xFF63D98B)),
    PEAKS(Color(0xFFB9D7FF)),
    CRYSTAL(Color(0xFFB57BFF)),
    FLAME(Color(0xFFFF5A3C)),
    HONEY(Color(0xFFFFC53D)),
    HOUSE(Color(0xFFD7A26C)),
    WAVE(Color(0xFF3FA9FF)),
    PALETTE(Color(0xFFFF6FD8)),
    COMPASS(Color(0xFFE6C36A)),
    SNOW(Color(0xFFCFF4FF)),
    SHIELD(Color(0xFFFF7A7A)),
    GEM(Color(0xFF4FF0E0)),
    GLOBE(Color(0xFF6FC3FF)),
    CUBE(Color(0xFF8BD17C)),
    CODE(Color(0xFF7CF5A8)),
    SMILE(Color(0xFFFFD84D)),
    HOURGLASS(Color(0xFFC9B79C))
}

fun emblemFor(category: VersionCategory): VersionEmblem = when (category.kind) {
    VersionCategoryKind.SNAPSHOT -> VersionEmblem.CODE
    VersionCategoryKind.APRIL_FOOLS -> VersionEmblem.SMILE
    VersionCategoryKind.OLD -> VersionEmblem.HOURGLASS
    VersionCategoryKind.RELEASE -> when (category.title) {
        "1.21" -> VersionEmblem.KEY
        "1.20" -> VersionEmblem.BLOSSOM
        "1.19" -> VersionEmblem.LEAF
        "1.18" -> VersionEmblem.PEAKS
        "1.17" -> VersionEmblem.CRYSTAL
        "1.16" -> VersionEmblem.FLAME
        "1.15" -> VersionEmblem.HONEY
        "1.14" -> VersionEmblem.HOUSE
        "1.13" -> VersionEmblem.WAVE
        "1.12" -> VersionEmblem.PALETTE
        "1.11" -> VersionEmblem.COMPASS
        "1.10" -> VersionEmblem.SNOW
        "1.9" -> VersionEmblem.SHIELD
        "1.8" -> VersionEmblem.GEM
        "1.7" -> VersionEmblem.GLOBE
        else -> if (!category.title.startsWith("1.")) VersionEmblem.SPARK else VersionEmblem.CUBE
    }
}

@Composable
fun VersionEmblemIcon(emblem: VersionEmblem, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawEmblem(emblem) }
}

private fun DrawScope.drawEmblem(emblem: VersionEmblem) {
    val c = emblem.color
    val dark = Color(0xFF071018)
    val w = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val glow = safeRadialGradient(listOf(c.copy(alpha = 0.45f), Color.Transparent), center = Offset(cx, cy), radius = w * 0.55f)
    drawCircle(glow, w * 0.55f, Offset(cx, cy))
    val stroke = Stroke(w * 0.06f, cap = StrokeCap.Round)
    val fill = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), c), startY = cy - w * 0.4f, endY = cy + w * 0.4f)
    when (emblem) {
        VersionEmblem.SPARK -> {
            val p = Path()
            for (i in 0 until 8) {
                val r = if (i % 2 == 0) w * 0.42f else w * 0.12f
                val a = i * PI / 4 - PI / 2
                val x = cx + (cos(a) * r).toFloat()
                val y = cy + (sin(a) * r).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, fill)
            drawCircle(Color.White, w * 0.05f, Offset(cx + w * 0.28f, cy - w * 0.28f))
        }
        VersionEmblem.KEY -> {
            drawCircle(c, w * 0.16f, Offset(cx - w * 0.18f, cy - w * 0.16f), style = Stroke(w * 0.08f))
            drawLine(c, Offset(cx - w * 0.07f, cy - w * 0.05f), Offset(cx + w * 0.3f, cy + w * 0.32f), w * 0.09f, StrokeCap.Round)
            drawLine(c, Offset(cx + w * 0.17f, cy + w * 0.19f), Offset(cx + w * 0.06f, cy + w * 0.3f), w * 0.08f, StrokeCap.Round)
            drawLine(c, Offset(cx + w * 0.27f, cy + w * 0.29f), Offset(cx + w * 0.17f, cy + w * 0.39f), w * 0.08f, StrokeCap.Round)
        }
        VersionEmblem.BLOSSOM -> {
            for (i in 0 until 5) {
                val a = i * 2 * PI / 5 - PI / 2
                drawCircle(c, w * 0.15f, Offset(cx + (cos(a) * w * 0.2f).toFloat(), cy + (sin(a) * w * 0.2f).toFloat()))
            }
            drawCircle(Color(0xFFFFE27A), w * 0.1f, Offset(cx, cy))
        }
        VersionEmblem.LEAF -> {
            rotate(-35f, Offset(cx, cy)) {
                val p = Path().apply {
                    moveTo(cx, cy - w * 0.42f)
                    quadraticTo(cx + w * 0.34f, cy, cx, cy + w * 0.42f)
                    quadraticTo(cx - w * 0.34f, cy, cx, cy - w * 0.42f)
                    close()
                }
                drawPath(p, fill)
                drawLine(dark.copy(alpha = 0.5f), Offset(cx, cy - w * 0.32f), Offset(cx, cy + w * 0.36f), w * 0.03f)
            }
        }
        VersionEmblem.PEAKS -> {
            val p = Path().apply {
                moveTo(cx - w * 0.45f, cy + w * 0.32f)
                lineTo(cx - w * 0.12f, cy - w * 0.3f)
                lineTo(cx + w * 0.06f, cy + w * 0.02f)
                lineTo(cx + w * 0.2f, cy - w * 0.16f)
                lineTo(cx + w * 0.45f, cy + w * 0.32f)
                close()
            }
            drawPath(p, Brush.verticalGradient(listOf(c, Color(0xFF4F6E9A)), startY = cy - w * 0.3f, endY = cy + w * 0.32f))
            val snow = Path().apply {
                moveTo(cx - w * 0.12f, cy - w * 0.3f)
                lineTo(cx - w * 0.22f, cy - w * 0.12f)
                lineTo(cx - w * 0.02f, cy - w * 0.12f)
                close()
            }
            drawPath(snow, Color.White)
        }
        VersionEmblem.CRYSTAL -> {
            listOf(Triple(-0.18f, 0.08f, -18f), Triple(0f, -0.05f, 0f), Triple(0.18f, 0.1f, 18f)).forEach { (dx, dy, rot) ->
                rotate(rot, Offset(cx + w * dx, cy + w * dy)) {
                    val x = cx + w * dx
                    val y = cy + w * dy
                    val p = Path().apply {
                        moveTo(x, y - w * 0.3f)
                        lineTo(x + w * 0.08f, y - w * 0.18f)
                        lineTo(x + w * 0.08f, y + w * 0.22f)
                        lineTo(x - w * 0.08f, y + w * 0.22f)
                        lineTo(x - w * 0.08f, y - w * 0.18f)
                        close()
                    }
                    drawPath(p, fill)
                }
            }
        }
        VersionEmblem.FLAME -> {
            val p = Path().apply {
                moveTo(cx, cy - w * 0.42f)
                cubicTo(cx + w * 0.38f, cy - w * 0.05f, cx + w * 0.3f, cy + w * 0.38f, cx, cy + w * 0.4f)
                cubicTo(cx - w * 0.3f, cy + w * 0.38f, cx - w * 0.38f, cy - w * 0.02f, cx - w * 0.06f, cy - w * 0.18f)
                cubicTo(cx - w * 0.04f, cy - w * 0.05f, cx + w * 0.02f, cy - w * 0.2f, cx, cy - w * 0.42f)
                close()
            }
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFE07A), c, Color(0xFFB0122A)), startY = cy - w * 0.42f, endY = cy + w * 0.4f))
        }
        VersionEmblem.HONEY -> {
            listOf(Offset(-0.15f, -0.12f), Offset(0.15f, -0.12f), Offset(0f, 0.14f)).forEach { o ->
                val p = Path()
                for (i in 0 until 6) {
                    val a = i * PI / 3 + PI / 6
                    val x = cx + w * o.x + (cos(a) * w * 0.15f).toFloat()
                    val y = cy + w * o.y + (sin(a) * w * 0.15f).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, fill)
                drawPath(p, dark.copy(alpha = 0.5f), style = Stroke(w * 0.025f))
            }
        }
        VersionEmblem.HOUSE -> {
            drawRect(fill, Offset(cx - w * 0.28f, cy - w * 0.04f), Size(w * 0.56f, w * 0.38f))
            val roof = Path().apply {
                moveTo(cx - w * 0.38f, cy)
                lineTo(cx, cy - w * 0.38f)
                lineTo(cx + w * 0.38f, cy)
                close()
            }
            drawPath(roof, Color(0xFFB4532A))
            drawRect(dark.copy(alpha = 0.6f), Offset(cx - w * 0.07f, cy + w * 0.12f), Size(w * 0.14f, w * 0.22f))
        }
        VersionEmblem.WAVE -> {
            for (row in 0 until 3) {
                val p = Path()
                val y0 = cy - w * 0.2f + row * w * 0.18f
                p.moveTo(cx - w * 0.4f, y0)
                for (i in 1..8) {
                    val x = cx - w * 0.4f + i * w * 0.1f
                    val y = y0 + (if (i % 2 == 0) 0f else -w * 0.08f)
                    p.lineTo(x, y)
                }
                drawPath(p, c.copy(alpha = 1f - row * 0.25f), style = stroke)
            }
        }
        VersionEmblem.PALETTE -> {
            listOf(Color(0xFFFF5A5A), Color(0xFFFFD84D), Color(0xFF5AE08A), Color(0xFF5A9BFF)).forEachIndexed { i, col ->
                val a = i * PI / 2 - PI / 4
                drawCircle(col, w * 0.16f, Offset(cx + (cos(a) * w * 0.16f).toFloat(), cy + (sin(a) * w * 0.16f).toFloat()))
            }
        }
        VersionEmblem.COMPASS -> {
            drawCircle(c, w * 0.38f, Offset(cx, cy), style = Stroke(w * 0.07f))
            val n = Path().apply {
                moveTo(cx, cy - w * 0.28f)
                lineTo(cx + w * 0.08f, cy)
                lineTo(cx - w * 0.08f, cy)
                close()
            }
            drawPath(n, Color(0xFFFF5A5A))
            val s = Path().apply {
                moveTo(cx, cy + w * 0.28f)
                lineTo(cx + w * 0.08f, cy)
                lineTo(cx - w * 0.08f, cy)
                close()
            }
            drawPath(s, Color.White)
        }
        VersionEmblem.SNOW -> {
            for (i in 0 until 6) {
                rotate(i * 60f, Offset(cx, cy)) {
                    drawLine(c, Offset(cx, cy), Offset(cx, cy - w * 0.4f), w * 0.06f, StrokeCap.Round)
                    drawLine(c, Offset(cx, cy - w * 0.24f), Offset(cx + w * 0.1f, cy - w * 0.34f), w * 0.05f, StrokeCap.Round)
                    drawLine(c, Offset(cx, cy - w * 0.24f), Offset(cx - w * 0.1f, cy - w * 0.34f), w * 0.05f, StrokeCap.Round)
                }
            }
        }
        VersionEmblem.SHIELD -> {
            val p = Path().apply {
                moveTo(cx, cy - w * 0.4f)
                lineTo(cx + w * 0.32f, cy - w * 0.28f)
                lineTo(cx + w * 0.28f, cy + w * 0.08f)
                quadraticTo(cx + w * 0.2f, cy + w * 0.3f, cx, cy + w * 0.42f)
                quadraticTo(cx - w * 0.2f, cy + w * 0.3f, cx - w * 0.28f, cy + w * 0.08f)
                lineTo(cx - w * 0.32f, cy - w * 0.28f)
                close()
            }
            drawPath(p, fill)
            drawLine(dark.copy(alpha = 0.5f), Offset(cx, cy - w * 0.3f), Offset(cx, cy + w * 0.3f), w * 0.04f)
        }
        VersionEmblem.GEM -> {
            val p = Path().apply {
                moveTo(cx - w * 0.3f, cy - w * 0.12f)
                lineTo(cx - w * 0.16f, cy - w * 0.3f)
                lineTo(cx + w * 0.16f, cy - w * 0.3f)
                lineTo(cx + w * 0.3f, cy - w * 0.12f)
                lineTo(cx, cy + w * 0.36f)
                close()
            }
            drawPath(p, fill)
            drawLine(dark.copy(alpha = 0.4f), Offset(cx - w * 0.3f, cy - w * 0.12f), Offset(cx + w * 0.3f, cy - w * 0.12f), w * 0.025f)
        }
        VersionEmblem.GLOBE -> {
            drawCircle(fill, w * 0.36f, Offset(cx, cy))
            drawOval(dark.copy(alpha = 0.45f), Offset(cx - w * 0.15f, cy - w * 0.36f), Size(w * 0.3f, w * 0.72f), style = Stroke(w * 0.03f))
            drawLine(dark.copy(alpha = 0.45f), Offset(cx - w * 0.36f, cy), Offset(cx + w * 0.36f, cy), w * 0.03f)
        }
        VersionEmblem.CUBE -> {
            val top = Path().apply {
                moveTo(cx, cy - w * 0.38f)
                lineTo(cx + w * 0.33f, cy - w * 0.19f)
                lineTo(cx, cy)
                lineTo(cx - w * 0.33f, cy - w * 0.19f)
                close()
            }
            val left = Path().apply {
                moveTo(cx - w * 0.33f, cy - w * 0.19f)
                lineTo(cx, cy)
                lineTo(cx, cy + w * 0.38f)
                lineTo(cx - w * 0.33f, cy + w * 0.19f)
                close()
            }
            val right = Path().apply {
                moveTo(cx + w * 0.33f, cy - w * 0.19f)
                lineTo(cx, cy)
                lineTo(cx, cy + w * 0.38f)
                lineTo(cx + w * 0.33f, cy + w * 0.19f)
                close()
            }
            drawPath(top, c)
            drawPath(left, c.copy(red = c.red * 0.7f, green = c.green * 0.7f, blue = c.blue * 0.7f))
            drawPath(right, c.copy(red = c.red * 0.5f, green = c.green * 0.5f, blue = c.blue * 0.5f))
        }
        VersionEmblem.CODE -> {
            drawRoundRect(dark.copy(alpha = 0.8f), Offset(cx - w * 0.4f, cy - w * 0.3f), Size(w * 0.8f, w * 0.6f), CornerRadius(w * 0.08f))
            drawRoundRect(c, Offset(cx - w * 0.4f, cy - w * 0.3f), Size(w * 0.8f, w * 0.6f), CornerRadius(w * 0.08f), style = Stroke(w * 0.04f))
            val l = Path().apply {
                moveTo(cx - w * 0.12f, cy - w * 0.12f)
                lineTo(cx - w * 0.24f, cy)
                lineTo(cx - w * 0.12f, cy + w * 0.12f)
            }
            drawPath(l, c, style = stroke)
            val r = Path().apply {
                moveTo(cx + w * 0.12f, cy - w * 0.12f)
                lineTo(cx + w * 0.24f, cy)
                lineTo(cx + w * 0.12f, cy + w * 0.12f)
            }
            drawPath(r, c, style = stroke)
            drawLine(c, Offset(cx + w * 0.04f, cy - w * 0.14f), Offset(cx - w * 0.04f, cy + w * 0.14f), w * 0.05f, StrokeCap.Round)
        }
        VersionEmblem.SMILE -> {
            drawCircle(fill, w * 0.38f, Offset(cx, cy))
            drawCircle(dark, w * 0.05f, Offset(cx - w * 0.13f, cy - w * 0.08f))
            drawCircle(dark, w * 0.05f, Offset(cx + w * 0.13f, cy - w * 0.08f))
            drawArc(dark, 20f, 140f, false, Offset(cx - w * 0.2f, cy - w * 0.1f), Size(w * 0.4f, w * 0.3f), style = Stroke(w * 0.05f, cap = StrokeCap.Round))
        }
        VersionEmblem.HOURGLASS -> {
            drawLine(c, Offset(cx - w * 0.26f, cy - w * 0.38f), Offset(cx + w * 0.26f, cy - w * 0.38f), w * 0.07f, StrokeCap.Round)
            drawLine(c, Offset(cx - w * 0.26f, cy + w * 0.38f), Offset(cx + w * 0.26f, cy + w * 0.38f), w * 0.07f, StrokeCap.Round)
            val p = Path().apply {
                moveTo(cx - w * 0.2f, cy - w * 0.34f)
                lineTo(cx + w * 0.2f, cy - w * 0.34f)
                lineTo(cx, cy)
                lineTo(cx + w * 0.2f, cy + w * 0.34f)
                lineTo(cx - w * 0.2f, cy + w * 0.34f)
                lineTo(cx, cy)
                close()
            }
            drawPath(p, fill)
        }
    }
}
