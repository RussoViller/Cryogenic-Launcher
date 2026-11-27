package com.russoviller.cryogenic.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.StrokeCap
import com.russoviller.cryogenic.ui.theme.cryoCornerBracketPath
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun cryoTacticalPanelShape(onStart: Boolean, cutPx: Float, slantPx: Float): Shape =
    androidx.compose.foundation.shape.RoundedCornerShape(cutPx)

fun Modifier.cryoTacticalBackdrop(
    shape: Shape,
    onStart: Boolean,
    fillAlpha: Float,
    corner: Dp = 22.dp
): Modifier = this.drawWithCache {
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val deep = CryoGlassColors.Deep
    val navy = CryoGlassColors.Navy
    val outline = shape.createOutline(size, layoutDirection, this)
    val fill = Brush.verticalGradient(
        listOf(navy.copy(alpha = 0.88f * fillAlpha), deep.copy(alpha = 0.94f * fillAlpha))
    )
    val gridStep = 18.dp.toPx()
    val gridColor = ice.copy(alpha = 0.045f)
    val stroke = 1.2.dp.toPx()
    val border = Brush.verticalGradient(listOf(ice.copy(alpha = 0.9f), ice.copy(alpha = 0.25f), aurora.copy(alpha = 0.8f)))
    val bracket = 22.dp.toPx()
    val bracketStroke = 3.dp.toPx()
    val chevW = 10.dp.toPx()
    val chevH = 14.dp.toPx()
    val chevGap = 6.dp.toPx()
    val chevCount = 9
    val chevPath = Path().apply {
        if (onStart) {
            moveTo(0f, 0f); lineTo(chevW * 0.5f, 0f); lineTo(chevW, chevH / 2f); lineTo(chevW * 0.5f, chevH); lineTo(0f, chevH); lineTo(chevW * 0.5f, chevH / 2f)
        } else {
            moveTo(chevW, 0f); lineTo(chevW * 0.5f, 0f); lineTo(0f, chevH / 2f); lineTo(chevW * 0.5f, chevH); lineTo(chevW, chevH); lineTo(chevW * 0.5f, chevH / 2f)
        }
        close()
    }
    val bracketRadius = (corner.toPx() - bracketStroke / 2f).coerceAtLeast(0f)
    val bracketX = if (onStart) bracketStroke / 2f else size.width - bracketStroke / 2f
    val bracketDir = if (onStart) 1f else -1f
    val topBracket = cryoCornerBracketPath(Offset(bracketX, bracketStroke / 2f), bracketDir, 1f, bracketRadius, bracket + bracketRadius * 0.5f)
    val bottomBracket = cryoCornerBracketPath(Offset(bracketX, size.height - bracketStroke / 2f), bracketDir, -1f, bracketRadius, bracket + bracketRadius * 0.5f)
    val chevX = if (onStart) size.width - chevW - 4.dp.toPx() else 4.dp.toPx()
    val chevTotal = chevCount * chevH + (chevCount - 1) * chevGap
    val chevTop = (size.height - chevTotal) / 2f
    onDrawWithContent {
        drawOutline(outline, fill)
        var x = gridStep
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
            x += gridStep
        }
        var y = gridStep
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
            y += gridStep
        }
        drawContent()
        drawOutline(outline, border, style = Stroke(stroke))
        drawPath(topBracket, ice, style = Stroke(bracketStroke, cap = StrokeCap.Round))
        drawPath(bottomBracket, aurora, style = Stroke(bracketStroke, cap = StrokeCap.Round))
        for (i in 0 until chevCount) {
            val center = (chevCount - 1) / 2f
            val a = (1f - kotlin.math.abs(i - center) / (center + 1f)) * 0.75f
            drawContext.canvas.save()
            drawContext.canvas.translate(chevX, chevTop + i * (chevH + chevGap))
            drawPath(chevPath, ice.copy(alpha = a))
            drawContext.canvas.restore()
        }
    }
}

fun Modifier.cryoChamferTile(
    cut: Dp,
    selected: Boolean = false
): Modifier = this.clip(androidx.compose.foundation.shape.RoundedCornerShape(cut)).drawWithCache {
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val c = cut.toPx().coerceAtMost(size.minDimension / 2f)
    val path = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(c)))
    }
    val liquid = cryoLiquid
    val fill = if (liquid) Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (selected) 0.28f else 0.15f), ice.copy(alpha = if (selected) 0.16f else 0.04f), Color.White.copy(alpha = 0.05f))
    ) else Brush.horizontalGradient(
        listOf(ice.copy(alpha = if (selected) 0.24f else 0.10f), ice.copy(alpha = 0.02f))
    )
    val border = if (liquid) cryoLiquidRim(if (selected) 1.1f else 0.75f) else Brush.horizontalGradient(
        listOf(ice.copy(alpha = if (selected) 0.95f else 0.55f), ice.copy(alpha = 0.10f), aurora.copy(alpha = if (selected) 0.7f else 0.25f))
    )
    val stroke = (if (selected) 1.5.dp else 1.dp).toPx()
    val borderPath = Path().apply {
        val h = stroke / 2f
        addRoundRect(RoundRect(h, h, size.width - h, size.height - h, CornerRadius((c - h).coerceAtLeast(0f))))
    }
    val marker = Path().apply {
        val s = 5.dp.toPx()
        val cy = size.height / 2f
        val cx = 9.dp.toPx()
        moveTo(cx, cy - s); lineTo(cx + s, cy); lineTo(cx, cy + s); lineTo(cx - s, cy); close()
    }
    onDrawWithContent {
        drawPath(path, fill)
        drawPath(marker, if (selected) aurora else ice.copy(alpha = 0.8f))
        drawContent()
        drawPath(borderPath, border, style = Stroke(stroke))
    }
}
