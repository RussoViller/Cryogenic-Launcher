package com.russoviller.cryogenic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

private fun TextStyle.frost(weight: FontWeight, spacing: Float) =
    copy(fontWeight = weight, letterSpacing = spacing.sp)

val AppTypography = base.copy(
    displayLarge = base.displayLarge.frost(FontWeight.Light, 1.50f),
    displayMedium = base.displayMedium.frost(FontWeight.Light, 1.25f),
    displaySmall = base.displaySmall.frost(FontWeight.Normal, 1.00f),
    headlineLarge = base.headlineLarge.frost(FontWeight.SemiBold, 1.00f),
    headlineMedium = base.headlineMedium.frost(FontWeight.SemiBold, 0.75f),
    headlineSmall = base.headlineSmall.frost(FontWeight.SemiBold, 0.50f),
    titleLarge = base.titleLarge.frost(FontWeight.SemiBold, 0.75f),
    titleMedium = base.titleMedium.frost(FontWeight.SemiBold, 0.50f),
    titleSmall = base.titleSmall.frost(FontWeight.SemiBold, 0.40f),
    labelLarge = base.labelLarge.frost(FontWeight.SemiBold, 0.60f),
    labelMedium = base.labelMedium.frost(FontWeight.Medium, 0.50f),
    labelSmall = base.labelSmall.frost(FontWeight.Medium, 0.50f)
)

fun cryoTypography(fillFactor: Float): Typography {
    val shadowAlpha = (0.30f + 0.55f * (1f - fillFactor.coerceIn(0f, 1f))).coerceIn(0f, 0.85f)
    val shadow = androidx.compose.ui.graphics.Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = shadowAlpha),
        offset = androidx.compose.ui.geometry.Offset(0f, 1.5f),
        blurRadius = 3f
    )
    fun TextStyle.s() = copy(shadow = shadow)
    return AppTypography.copy(
        displayLarge = AppTypography.displayLarge.s(),
        displayMedium = AppTypography.displayMedium.s(),
        displaySmall = AppTypography.displaySmall.s(),
        headlineLarge = AppTypography.headlineLarge.s(),
        headlineMedium = AppTypography.headlineMedium.s(),
        headlineSmall = AppTypography.headlineSmall.s(),
        titleLarge = AppTypography.titleLarge.s(),
        titleMedium = AppTypography.titleMedium.s(),
        titleSmall = AppTypography.titleSmall.s(),
        bodyLarge = AppTypography.bodyLarge.s(),
        bodyMedium = AppTypography.bodyMedium.s(),
        bodySmall = AppTypography.bodySmall.s(),
        labelLarge = AppTypography.labelLarge.s(),
        labelMedium = AppTypography.labelMedium.s(),
        labelSmall = AppTypography.labelSmall.s()
    )
}
