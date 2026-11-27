package com.russoviller.cryogenic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object CryoPalette {
    val Frost = Color(0xFFE6F7FF)
    val IceCyan = Color(0xFF7FD8FF)
    val Glacier = Color(0xFF38B6F0)
    val DeepIce = Color(0xFF0077B6)
    val Abyss = Color(0xFF06101F)
    val Polar = Color(0xFFB6F0F5)
    val Aurora = Color(0xFF6FE3D2)

    const val SEED_LIGHT = 0xFF0077B6
    const val SEED_DARK = 0xFF7FD8FF
}

internal val cryogenicLight = lightColorScheme(
    primary = Color(0xFF0077B6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEEFF),
    onPrimaryContainer = Color(0xFF002B42),
    secondary = Color(0xFF3D6787),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E9F8),
    onSecondaryContainer = Color(0xFF0B2A40),
    tertiary = Color(0xFF00838F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8F4F7),
    onTertiaryContainer = Color(0xFF00363B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF3FAFF),
    onBackground = Color(0xFF0B1D2E),
    surface = Color(0xFFF3FAFF),
    onSurface = Color(0xFF0B1D2E),
    surfaceVariant = Color(0xFFD7E7F3),
    onSurfaceVariant = Color(0xFF3F5568),
    outline = Color(0xFF6F8BA3),
    outlineVariant = Color(0xFFB9D0E2),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1E3245),
    inverseOnSurface = Color(0xFFE6F3FC),
    inversePrimary = Color(0xFF7FD8FF),
    surfaceDim = Color(0xFFD2E3F0),
    surfaceBright = Color(0xFFF8FCFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF7FE),
    surfaceContainer = Color(0xFFE4F2FC),
    surfaceContainerHigh = Color(0xFFDAEBF8),
    surfaceContainerHighest = Color(0xFFCFE4F4),
)

internal val cryogenicDark = darkColorScheme(
    primary = Color(0xFF7FD8FF),
    onPrimary = Color(0xFF002A3D),
    primaryContainer = Color(0xFF0E4A6B),
    onPrimaryContainer = Color(0xFFC9EEFF),
    secondary = Color(0xFFA9C9E8),
    onSecondary = Color(0xFF12324B),
    secondaryContainer = Color(0xFF1F3A55),
    onSecondaryContainer = Color(0xFFD3E8FA),
    tertiary = Color(0xFFB6F0F5),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF134E57),
    onTertiaryContainer = Color(0xFFCFF8FB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF06101F),
    onBackground = Color(0xFFE3F4FF),
    surface = Color(0xFF06101F),
    onSurface = Color(0xFFE3F4FF),
    surfaceVariant = Color(0xFF18304A),
    onSurfaceVariant = Color(0xFFA9C3DB),
    outline = Color(0xFF4E7392),
    outlineVariant = Color(0xFF26425E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3F4FF),
    inverseOnSurface = Color(0xFF13263A),
    inversePrimary = Color(0xFF0077B6),
    surfaceDim = Color(0xFF06101F),
    surfaceBright = Color(0xFF1A3352),
    surfaceContainerLowest = Color(0xFF040B16),
    surfaceContainerLow = Color(0xFF081526),
    surfaceContainer = Color(0xFF0A1A2E),
    surfaceContainerHigh = Color(0xFF10233B),
    surfaceContainerHighest = Color(0xFF162C47),
)
