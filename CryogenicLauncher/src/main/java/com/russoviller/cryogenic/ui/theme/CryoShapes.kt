package com.russoviller.cryogenic.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val CRYO_ARC_K = 0.5523f

fun cryoShardShape(major: Dp, minor: Dp = major / 3) = RoundedCornerShape(major)

fun Path.addCryoRoundedPolygon(points: List<Offset>, radii: List<Float>) {
    val n = points.size
    if (n < 3) return
    for (i in 0 until n) {
        val p = points[i]
        val prev = points[(i - 1 + n) % n]
        val next = points[(i + 1) % n]
        val v0 = prev - p
        val v1 = next - p
        val l0 = v0.getDistance()
        val l1 = v1.getDistance()
        val r = minOf(radii[i], l0 / 2f, l1 / 2f).coerceAtLeast(0f)
        val a = if (l0 > 0f) p + v0 * (r / l0) else p
        val b = if (l1 > 0f) p + v1 * (r / l1) else p
        if (i == 0) moveTo(a.x, a.y) else lineTo(a.x, a.y)
        if (r > 0f) {
            val c1 = a + (p - a) * CRYO_ARC_K
            val c2 = b + (p - b) * CRYO_ARC_K
            cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
        }
    }
    close()
}

fun cryoRoundedPolygonPath(points: List<Offset>, radius: Float): Path =
    Path().apply { addCryoRoundedPolygon(points, List(points.size) { radius }) }

fun cryoRoundedPolygonPath(points: List<Offset>, radii: List<Float>): Path =
    Path().apply { addCryoRoundedPolygon(points, radii) }

fun cryoCornerBracketPath(corner: Offset, dirX: Float, dirY: Float, radius: Float, length: Float): Path {
    val r = radius.coerceAtLeast(0f)
    val len = length.coerceAtLeast(r)
    val startV = Offset(corner.x, corner.y + dirY * len)
    val a = Offset(corner.x, corner.y + dirY * r)
    val b = Offset(corner.x + dirX * r, corner.y)
    val endH = Offset(corner.x + dirX * len, corner.y)
    return Path().apply {
        moveTo(startV.x, startV.y)
        lineTo(a.x, a.y)
        if (r > 0f) {
            val c1 = a + (corner - a) * CRYO_ARC_K
            val c2 = b + (corner - b) * CRYO_ARC_K
            cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
        }
        lineTo(endH.x, endH.y)
    }
}

val CryoShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun cryoFrostBrush(alpha: Float = 1f): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.linearGradient(
        colors = listOf(
            scheme.primary.copy(alpha = 0.70f * alpha),
            scheme.primary.copy(alpha = 0.10f * alpha),
            Color.Transparent,
            scheme.tertiary.copy(alpha = 0.45f * alpha)
        )
    )
}

@Composable
fun cryoBorder(width: Dp = 1.dp, alpha: Float = 1f): BorderStroke =
    if (com.russoviller.cryogenic.ui.components.CryoLiquid.on) BorderStroke(width + 0.5.dp, com.russoviller.cryogenic.ui.components.cryoLiquidRim(alpha))
    else BorderStroke(
        width,
        Brush.linearGradient(
            listOf(
                com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.70f * alpha),
                com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.12f * alpha),
                com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora.copy(alpha = 0.45f * alpha)
            )
        )
    )
