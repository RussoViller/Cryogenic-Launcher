package com.russoviller.cryogenic.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

/**
 * Menor raio aceito para gradientes radiais.
 *
 * `android.graphics.RadialGradient` lança
 * `IllegalArgumentException: ending radius must be > 0` quando recebe raio <= 0.
 * Raios calculados a partir do tamanho do layout (tamanhos 0 na primeira medição,
 * telas pequenas, multi-janela, fontes/DPI grandes, animações em 0) podem chegar
 * a 0 ou ficar negativos e derrubar o launcher na thread de renderização.
 */
const val MIN_GRADIENT_RADIUS = 1f

/**
 * Normaliza um raio de gradiente:
 * - `Float.POSITIVE_INFINITY` é mantido (sentinela do Compose: "metade da menor dimensão",
 *   que o próprio Compose só aplica quando o tamanho desenhado não é vazio);
 * - NaN, valores negativos, zero e valores abaixo de [MIN_GRADIENT_RADIUS] viram [MIN_GRADIENT_RADIUS].
 */
@Stable
fun safeGradientRadius(radius: Float): Float = when {
    radius == Float.POSITIVE_INFINITY -> radius
    radius.isNaN() || radius < MIN_GRADIENT_RADIUS -> MIN_GRADIENT_RADIUS
    else -> radius
}

/**
 * Substituto direto de [Brush.radialGradient] que nunca produz raio inválido.
 * Mesma assinatura e mesmo resultado visual para qualquer raio >= [MIN_GRADIENT_RADIUS].
 */
@Stable
fun safeRadialGradient(
    colors: List<Color>,
    center: Offset = Offset.Unspecified,
    radius: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp
): Brush = Brush.radialGradient(
    colors = colors,
    center = center,
    radius = safeGradientRadius(radius),
    tileMode = tileMode
)

/**
 * Versão com `colorStops` de [safeRadialGradient].
 */
@Stable
fun safeRadialGradient(
    vararg colorStops: Pair<Float, Color>,
    center: Offset = Offset.Unspecified,
    radius: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp
): Brush = Brush.radialGradient(
    colorStops = colorStops,
    center = center,
    radius = safeGradientRadius(radius),
    tileMode = tileMode
)
