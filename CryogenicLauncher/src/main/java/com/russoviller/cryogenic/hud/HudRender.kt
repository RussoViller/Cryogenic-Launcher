package com.russoviller.cryogenic.hud

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/** Cálculo de posição/tamanho dos controles na tela. */
class HudGeometry(val width: Int, val height: Int, val density: Float) {
    fun buttonSize(b: HudButton) = Size(
        b.width.px(width, height, density).coerceIn(4f, width.toFloat()),
        b.height.px(width, height, density).coerceIn(4f, height.toFloat())
    )

    fun buttonRect(b: HudButton, moved: Offset? = null): Rect {
        val s = buttonSize(b)
        val topLeft = moved ?: Offset(b.x * max(0f, width - s.width), b.y * max(0f, height - s.height))
        return Rect(topLeft, s)
    }

    fun stickSize(s: HudStick): Float = s.size.px(width, height, density).coerceIn(24f, min(width, height).toFloat())

    fun stickRect(s: HudStick): Rect {
        val size = stickSize(s)
        return Rect(Offset(s.x * max(0f, width - size), s.y * max(0f, height - size)), Size(size, size))
    }

    /** Converte um canto superior esquerdo em px para a posição 0..1 do espaço livre. */
    fun fractionOf(topLeft: Offset, size: Size): Offset = Offset(
        if (width - size.width > 0f) (topLeft.x / (width - size.width)).coerceIn(0f, 1f) else 0f,
        if (height - size.height > 0f) (topLeft.y / (height - size.height)).coerceIn(0f, 1f) else 0f
    )
}

/** Estado visual de um joystick durante o jogo. */
data class HudStickVisual(
    val knob: Offset = Offset.Zero,
    val center: Offset? = null,
    val dir: Int = -1,
    val armed: Boolean = false,
    val locked: Boolean = false,
    val active: Boolean = false
)

internal fun Int.toHudColor(alpha: Float = 1f): Color {
    val c = Color(this)
    return if (alpha >= 0.999f) c else c.copy(alpha = c.alpha * alpha)
}

internal fun DrawScope.drawHudButton(
    b: HudButton,
    rect: Rect,
    pressed: Boolean,
    alpha: Float,
    density: Float,
    measurer: TextMeasurer
) {
    val st = b.style
    val a = (alpha * st.opacity).coerceIn(0f, 1f)
    if (a <= 0.01f) return
    val radiusPx = (if (st.radiusPx) st.radiusDp else st.radiusDp * density).coerceIn(0f, min(rect.width, rect.height) / 2f)
    val cr = CornerRadius(radiusPx, radiusPx)
    drawRoundRect(
        color = (if (pressed) st.fillPressed else st.fill).toHudColor(a),
        topLeft = rect.topLeft, size = rect.size, cornerRadius = cr
    )
    val strokePx = st.strokeDp * density
    if (strokePx > 0.1f) {
        val half = strokePx / 2f
        drawRoundRect(
            color = (if (pressed) st.strokePressed else st.stroke).toHudColor(a),
            topLeft = Offset(rect.left + half, rect.top + half),
            size = Size(max(0f, rect.width - strokePx), max(0f, rect.height - strokePx)),
            cornerRadius = CornerRadius(max(0f, radiusPx - half), max(0f, radiusPx - half)),
            style = Stroke(strokePx)
        )
    }
    if (b.text.isNotEmpty()) {
        drawHudText(b.text, rect, (if (pressed) st.textPressed else st.text).toHudColor(a), st.textSp, measurer)
    }
}

internal fun DrawScope.drawHudText(text: String, rect: Rect, color: Color, textSp: Float, measurer: TextMeasurer) {
    val w = rect.width.toInt().coerceAtLeast(1)
    val h = rect.height.toInt().coerceAtLeast(1)
    val layout = measurer.measure(
        text = text,
        style = TextStyle(color = color, fontSize = textSp.coerceIn(4f, 60f).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        overflow = TextOverflow.Clip,
        maxLines = 2,
        constraints = Constraints(maxWidth = w, maxHeight = h)
    )
    drawText(
        layout,
        topLeft = Offset(rect.left + (rect.width - layout.size.width) / 2f, rect.top + (rect.height - layout.size.height) / 2f)
    )
}

internal fun DrawScope.drawHudStick(
    s: HudStick,
    rect: Rect,
    visual: HudStickVisual,
    alpha: Float,
    density: Float
) {
    val st = s.style
    val a = (alpha * st.opacity).coerceIn(0f, 1f)
    if (a <= 0.01f) return
    val size = rect.width
    val center = visual.center ?: rect.center
    val strokePx = st.strokeDp * density
    if (s.mode == HudStickMode.DPAD) {
        val gap = size * st.gapRatio.coerceIn(0f, 0.3f)
        val cell = (size - gap * 2f) / 3f
        val radius = (st.radiusDp * density).coerceIn(0f, cell / 2f)
        val origin = Offset(center.x - size / 2f, center.y - size / 2f)
        // 0=L, 2=N, 4=O, 6=S → células da cruz
        val cells = listOf(0 to Offset(2f, 1f), 2 to Offset(1f, 0f), 4 to Offset(0f, 1f), 6 to Offset(1f, 2f))
        for ((dir, c) in cells) {
            val on = visual.dir == dir || visual.dir == (dir + 1) % 8 || visual.dir == (dir + 7) % 8
            val tl = Offset(origin.x + c.x * (cell + gap), origin.y + c.y * (cell + gap))
            drawRoundRect((if (on) st.fillPressed else st.fill).toHudColor(a), tl, Size(cell, cell), CornerRadius(radius, radius))
            if (strokePx > 0.1f) drawRoundRect(
                (if (on) st.strokePressed else st.stroke).toHudColor(a), tl, Size(cell, cell), CornerRadius(radius, radius), style = Stroke(strokePx)
            )
            run {
                val tri = cell * 0.18f
                val cx = tl.x + cell / 2f; val cy = tl.y + cell / 2f
                val path = androidx.compose.ui.graphics.Path().apply {
                    when (dir) {
                        0 -> { moveTo(cx + tri, cy); lineTo(cx - tri, cy - tri); lineTo(cx - tri, cy + tri) }
                        2 -> { moveTo(cx, cy - tri); lineTo(cx - tri, cy + tri); lineTo(cx + tri, cy + tri) }
                        4 -> { moveTo(cx - tri, cy); lineTo(cx + tri, cy - tri); lineTo(cx + tri, cy + tri) }
                        else -> { moveTo(cx, cy + tri); lineTo(cx - tri, cy - tri); lineTo(cx + tri, cy - tri) }
                    }
                    close()
                }
                drawPath(path, (if (on) st.textPressed else st.text).toHudColor(a))
            }
        }
        return
    }
    val bgRadius = (st.radiusRatio * size).coerceIn(0f, size / 2f)
    val tl = Offset(center.x - size / 2f, center.y - size / 2f)
    drawRoundRect(st.fill.toHudColor(a), tl, Size(size, size), CornerRadius(bgRadius, bgRadius))
    if (strokePx > 0.1f) {
        val half = strokePx / 2f
        drawRoundRect(
            (if (visual.armed || visual.locked) st.knobFill else st.stroke).toHudColor(a),
            Offset(tl.x + half, tl.y + half), Size(size - strokePx, size - strokePx),
            CornerRadius(max(0f, bgRadius - half), max(0f, bgRadius - half)), style = Stroke(strokePx)
        )
    }
    if (s.canLock) {
        // marca de travar a corrida no topo
        val markR = size * 0.035f
        drawCircle(
            (if (visual.armed || visual.locked) st.knobFill else st.stroke).toHudColor(a * if (visual.armed || visual.locked) 1f else 0.6f),
            markR, Offset(center.x, tl.y - markR * 2.2f)
        )
    }
    val knobSize = size * st.knobRatio.coerceIn(0.1f, 0.9f)
    val travel = (size - knobSize) / 2f
    val k = Offset(center.x + visual.knob.x * travel, center.y + visual.knob.y * travel)
    val knobRadius = (st.knobRadiusRatio * knobSize).coerceIn(0f, knobSize / 2f)
    drawRoundRect(
        st.knobFill.toHudColor(a * if (visual.active) 1f else 0.85f),
        Offset(k.x - knobSize / 2f, k.y - knobSize / 2f), Size(knobSize, knobSize), CornerRadius(knobRadius, knobRadius)
    )
    if (strokePx > 0.1f) drawRoundRect(
        st.knobStroke.toHudColor(a), Offset(k.x - knobSize / 2f, k.y - knobSize / 2f), Size(knobSize, knobSize),
        CornerRadius(knobRadius, knobRadius), style = Stroke(max(1f, strokePx * 0.6f))
    )
}
