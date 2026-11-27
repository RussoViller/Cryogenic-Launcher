package com.russoviller.cryogenic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun cryoChamferShape(cut: Dp): Shape = RoundedCornerShape(cut)

@Composable
fun CryoItemSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape? = null,
    color: Color = Color.Unspecified,
    contentColor: Color = CryoGlassColors.Frost,
    accent: Color = CryoGlassColors.Ice,
    content: @Composable () -> Unit
) {
    val itemShape = cryoChamferShape(14.dp)
    val fillFactor = cryoFillFactor().coerceAtMost(1f)
    Box(
        modifier = modifier
            .clip(itemShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.10f + 0.06f * fillFactor),
                        CryoGlassColors.Navy.copy(alpha = 0.35f + 0.35f * fillFactor),
                        CryoGlassColors.Deep.copy(alpha = 0.45f + 0.35f * fillFactor)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), accent.copy(alpha = 0.08f), CryoGlassColors.Aurora.copy(alpha = 0.35f))),
                itemShape
            )
            .drawBehind {
                val w = 3.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(accent, CryoGlassColors.Aurora.copy(alpha = 0.4f))),
                    topLeft = Offset(0f, size.height * 0.22f),
                    size = Size(w, size.height * 0.56f),
                    cornerRadius = CornerRadius(w / 2f)
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
