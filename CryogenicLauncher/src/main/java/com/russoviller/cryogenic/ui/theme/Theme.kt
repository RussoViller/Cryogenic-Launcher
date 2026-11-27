/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.russoviller.cryogenic.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.withGradient
import com.russoviller.cryogenic.setting.enums.isLauncherInDarkTheme
import com.russoviller.cryogenic.ui.theme.components.activeMaskView
import com.russoviller.cryogenic.viewmodel.BackgroundViewModel
import com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel

private val urbanAshLight = lightColorScheme(
    primary = primaryLight.urbanAsh,
    onPrimary = onPrimaryLight.urbanAsh,
    primaryContainer = primaryContainerLight.urbanAsh,
    onPrimaryContainer = onPrimaryContainerLight.urbanAsh,
    secondary = secondaryLight.urbanAsh,
    onSecondary = onSecondaryLight.urbanAsh,
    secondaryContainer = secondaryContainerLight.urbanAsh,
    onSecondaryContainer = onSecondaryContainerLight.urbanAsh,
    tertiary = tertiaryLight.urbanAsh,
    onTertiary = onTertiaryLight.urbanAsh,
    tertiaryContainer = tertiaryContainerLight.urbanAsh,
    onTertiaryContainer = onTertiaryContainerLight.urbanAsh,
    error = errorLight.urbanAsh,
    onError = onErrorLight.urbanAsh,
    errorContainer = errorContainerLight.urbanAsh,
    onErrorContainer = onErrorContainerLight.urbanAsh,
    background = backgroundLight.urbanAsh,
    onBackground = onBackgroundLight.urbanAsh,
    surface = surfaceLight.urbanAsh,
    onSurface = onSurfaceLight.urbanAsh,
    surfaceVariant = surfaceVariantLight.urbanAsh,
    onSurfaceVariant = onSurfaceVariantLight.urbanAsh,
    outline = outlineLight.urbanAsh,
    outlineVariant = outlineVariantLight.urbanAsh,
    scrim = scrimLight.urbanAsh,
    inverseSurface = inverseSurfaceLight.urbanAsh,
    inverseOnSurface = inverseOnSurfaceLight.urbanAsh,
    inversePrimary = inversePrimaryLight.urbanAsh,
    surfaceDim = surfaceDimLight.urbanAsh,
    surfaceBright = surfaceBrightLight.urbanAsh,
    surfaceContainerLowest = surfaceContainerLowestLight.urbanAsh,
    surfaceContainerLow = surfaceContainerLowLight.urbanAsh,
    surfaceContainer = surfaceContainerLight.urbanAsh,
    surfaceContainerHigh = surfaceContainerHighLight.urbanAsh,
    surfaceContainerHighest = surfaceContainerHighestLight.urbanAsh,
)

private val urbanAshDark = darkColorScheme(
    primary = primaryDark.urbanAsh,
    onPrimary = onPrimaryDark.urbanAsh,
    primaryContainer = primaryContainerDark.urbanAsh,
    onPrimaryContainer = onPrimaryContainerDark.urbanAsh,
    secondary = secondaryDark.urbanAsh,
    onSecondary = onSecondaryDark.urbanAsh,
    secondaryContainer = secondaryContainerDark.urbanAsh,
    onSecondaryContainer = onSecondaryContainerDark.urbanAsh,
    tertiary = tertiaryDark.urbanAsh,
    onTertiary = onTertiaryDark.urbanAsh,
    tertiaryContainer = tertiaryContainerDark.urbanAsh,
    onTertiaryContainer = onTertiaryContainerDark.urbanAsh,
    error = errorDark.urbanAsh,
    onError = onErrorDark.urbanAsh,
    errorContainer = errorContainerDark.urbanAsh,
    onErrorContainer = onErrorContainerDark.urbanAsh,
    background = backgroundDark.urbanAsh,
    onBackground = onBackgroundDark.urbanAsh,
    surface = surfaceDark.urbanAsh,
    onSurface = onSurfaceDark.urbanAsh,
    surfaceVariant = surfaceVariantDark.urbanAsh,
    onSurfaceVariant = onSurfaceVariantDark.urbanAsh,
    outline = outlineDark.urbanAsh,
    outlineVariant = outlineVariantDark.urbanAsh,
    scrim = scrimDark.urbanAsh,
    inverseSurface = inverseSurfaceDark.urbanAsh,
    inverseOnSurface = inverseOnSurfaceDark.urbanAsh,
    inversePrimary = inversePrimaryDark.urbanAsh,
    surfaceDim = surfaceDimDark.urbanAsh,
    surfaceBright = surfaceBrightDark.urbanAsh,
    surfaceContainerLowest = surfaceContainerLowestDark.urbanAsh,
    surfaceContainerLow = surfaceContainerLowDark.urbanAsh,
    surfaceContainer = surfaceContainerDark.urbanAsh,
    surfaceContainerHigh = surfaceContainerHighDark.urbanAsh,
    surfaceContainerHighest = surfaceContainerHighestDark.urbanAsh,
)

private fun customLight(
    color: Color,
    style: PaletteStyle,
): ColorScheme {
    return dynamicColorScheme(
        primary = color,
        isDark = false,
        style = style,
    )
}

private fun customDark(
    color: Color,
    style: PaletteStyle,
): ColorScheme {
    return dynamicColorScheme(
        primary = color,
        isDark = true,
        style = style,
    )
}

/** Esquema neutro para cor personalizada sem matiz; preto no escuro vira fundo AMOLED. */
private fun neutralScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    val base = dynamicColorScheme(primary = seed, isDark = darkTheme, style = PaletteStyle.Monochrome)
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(seed.toArgb(), hsl)
    if (!darkTheme || hsl[2] >= 0.22f) return base
    return base.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        surfaceContainerHighest = Color(0xFF222222)
    )
}

fun launcherColorScheme(
    context: android.content.Context,
    colorTheme: ColorThemeType,
    darkTheme: Boolean,
    customColor: Color,
    customPaletteStyle: PaletteStyle,
    dynamicColor: Boolean = true
): ColorScheme = when {
    dynamicColor && colorTheme == ColorThemeType.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    colorTheme == ColorThemeType.VOLCANIC -> dynamicColorScheme(primary = Color(0xFFFF5A1F), isDark = darkTheme, style = PaletteStyle.Vibrant)
    colorTheme == ColorThemeType.MARINE -> dynamicColorScheme(primary = Color(0xFF1E7BFF), isDark = darkTheme, style = PaletteStyle.Vibrant)
    colorTheme == ColorThemeType.GALAXY -> dynamicColorScheme(primary = Color(0xFF8E5CFF), isDark = darkTheme, style = PaletteStyle.Vibrant)
    colorTheme == ColorThemeType.DESERT -> dynamicColorScheme(primary = Color(0xFFD9A066), isDark = darkTheme, style = PaletteStyle.TonalSpot)
    colorTheme == ColorThemeType.GOLD -> dynamicColorScheme(primary = Color(0xFFFFC21A), isDark = darkTheme, style = PaletteStyle.Vibrant)
    colorTheme == ColorThemeType.SAKURA -> dynamicColorScheme(primary = Color(0xFFFF8DC0), isDark = darkTheme, style = PaletteStyle.TonalSpot)
    colorTheme == ColorThemeType.SILVER -> dynamicColorScheme(primary = Color(0xFFB8C2CE), isDark = darkTheme, style = PaletteStyle.Monochrome)
    //branco, cinza ou preto: escala neutra (a paleta do Material inventaria um matiz)
    colorTheme == ColorThemeType.CUSTOM && com.russoviller.cryogenic.ui.components.isAchromatic(customColor) ->
        neutralScheme(customColor, darkTheme)
    colorTheme == ColorThemeType.VERDEJANTE -> if (darkTheme) customDark(color = Color(0xFF00FF53), style = PaletteStyle.Vibrant)
        else customLight(color = Color(0xFF00FF53), style = PaletteStyle.Vibrant)
    darkTheme -> when (colorTheme) {
        ColorThemeType.CRYOGENIC -> cryogenicDark
        ColorThemeType.URBAN_ASH -> urbanAshDark
        ColorThemeType.CUSTOM -> customDark(color = customColor, style = customPaletteStyle)
        else -> cryogenicDark
    }
    else -> when (colorTheme) {
        ColorThemeType.CRYOGENIC -> cryogenicLight
        ColorThemeType.URBAN_ASH -> urbanAshLight
        ColorThemeType.CUSTOM -> customLight(color = customColor, style = customPaletteStyle)
        else -> cryogenicLight
    }
}

@Composable
fun CryogenicLauncherTheme(
    darkTheme: Boolean = isLauncherInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    backgroundViewModel: BackgroundViewModel? = null,
    textShadow: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorTheme = AllSettings.launcherColorTheme.state
    val customColorInt = AllSettings.launcherCustomColor.state
    val customColor = Color(customColorInt)
    val customPaletteStyle = AllSettings.launcherCustomPaletteStyle.state

    val context = LocalContext.current

    val adjust = com.russoviller.cryogenic.ui.components.CryoColorAdjust.fromSettings()
    val targetColorScheme = remember(darkTheme, dynamicColor, colorTheme, customColor, customPaletteStyle, adjust) {
        adjust.apply(launcherColorScheme(context, colorTheme, darkTheme, customColor, customPaletteStyle, dynamicColor))
    }

    var currentDarkTheme by remember { mutableStateOf(darkTheme) }
    var currentDisplayScheme by remember { mutableStateOf(targetColorScheme) }

    LaunchedEffect(darkTheme, targetColorScheme) {
        if (darkTheme != currentDarkTheme) {
            context.activeMaskView(
                maskComplete = {
                    currentDarkTheme = darkTheme
                    currentDisplayScheme = targetColorScheme
                },
                maskAnimFinish = {

                }
            )
        } else {
            currentDarkTheme = darkTheme
            currentDisplayScheme = targetColorScheme
        }
    }

    val customGradient = AllSettings.cryoCustomGradient.state
    val cryoSpec = remember(colorTheme, currentDisplayScheme, adjust, customGradient, customColor) {
        val base = com.russoviller.cryogenic.ui.components.cryoSpecFor(colorTheme, currentDisplayScheme, customColor)
        //Tema Personalizado com degradê próprio: as cores principais saem do degradê
        val withGradient = if (colorTheme == ColorThemeType.CUSTOM) {
            base.withGradient(com.russoviller.cryogenic.ui.components.CryoCustomGradient.activeColors(customGradient))
        } else base
        adjust.apply(withGradient)
    }
    com.russoviller.cryogenic.ui.components.CryoGradientTicker()
    com.russoviller.cryogenic.ui.components.CryoNeonTicker()
    androidx.compose.runtime.SideEffect {
        com.russoviller.cryogenic.ui.components.CryoTheme.spec = cryoSpec
    }
    val fillFactor = com.russoviller.cryogenic.ui.components.cryoFillFactor()
    val shadowNeeded = textShadow && fillFactor < 0.6f
    val typography = remember(shadowNeeded, fillFactor) {
        if (shadowNeeded) cryoTypography(fillFactor) else AppTypography
    }

    CompositionLocalProvider(
        LocalBackgroundViewModel provides backgroundViewModel
    ) {
        MaterialExpressiveTheme(
            colorScheme = currentDisplayScheme,
            motionScheme = MotionScheme.expressive(),
            typography = typography,
            shapes = CryoShapes,
            content = content
        )
    }
}
