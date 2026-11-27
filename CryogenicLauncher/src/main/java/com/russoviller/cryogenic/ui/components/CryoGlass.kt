package com.russoviller.cryogenic.ui.components

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.BackgroundBlur
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Immutable
data class CryoSpec(
    val ice: Color,
    val iceSoft: Color,
    val aurora: Color,
    val frost: Color,
    val deep: Color,
    val navy: Color,
    /** Degradê personalizado (tema Personalizado). null = degradê padrão iceSoft → ice → aurora. */
    val gradient: List<Color>? = null
)

private val CryogenicSpec = CryoSpec(
    ice = Color(0xFF7FD8FF),
    iceSoft = Color(0xFF4FB8F0),
    aurora = Color(0xFF6FE3D2),
    frost = Color(0xFFE6F7FF),
    deep = Color(0xFF061226),
    navy = Color(0xFF0B1E3A)
)

/** Tema Prateado: metal escovado, do aço ao prata claro. */
private val SilverSpec = CryoSpec(
    ice = Color(0xFFC9D1DB),
    iceSoft = Color(0xFF8F9AA8),
    aurora = Color(0xFFF1F4F8),
    frost = Color(0xFFF6F8FA),
    deep = Color(0xFF0A0C10),
    navy = Color(0xFF1C2129)
)

/** Cor sem matiz (branco, cinza, preto): o tema personalizado usa tons neutros em vez da paleta do Material. */
fun isAchromatic(color: Color): Boolean {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[1] < 0.10f || hsv[2] < 0.06f
}

private fun lightnessOf(color: Color): Float {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(color.toArgb(), hsl)
    return hsl[2]
}

private fun withLightness(color: Color, l: Float): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[2] = l.coerceIn(0f, 1f)
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

/**
 * Tema personalizado com branco, cinza/prata ou preto.
 * - branco: detalhes brancos;
 * - cinza/prata: detalhes no tom escolhido (clareado o bastante para aparecer no fundo escuro);
 * - preto: fundos pretos puros (AMOLED) com detalhes cinza.
 */
fun cryoNeutralSpec(seed: Color): CryoSpec {
    val l = lightnessOf(seed)
    return when {
        l >= 0.85f -> CryoSpec(
            ice = Color(0xFFF2F4F7),
            iceSoft = Color(0xFFC9CED6),
            aurora = Color(0xFFFFFFFF),
            frost = Color(0xFFFFFFFF),
            deep = Color(0xFF0A0B0D),
            navy = Color(0xFF1C1E22)
        )
        l >= 0.22f -> {
            val main = maxOf(l, 0.70f)
            CryoSpec(
                ice = withLightness(seed, main),
                iceSoft = withLightness(seed, main - 0.24f),
                aurora = withLightness(seed, minOf(0.96f, main + 0.18f)),
                frost = Color(0xFFF4F6F9),
                deep = Color(0xFF0B0D10),
                navy = Color(0xFF1E2228)
            )
        }
        else -> CryoSpec(
            ice = Color(0xFFBDBDBD),
            iceSoft = Color(0xFF6E6E6E),
            aurora = Color(0xFFEDEDED),
            frost = Color(0xFFF5F5F5),
            deep = Color(0xFF000000),
            navy = Color(0xFF0A0A0A)
        )
    }
}

fun cryoSpecFor(type: ColorThemeType, scheme: ColorScheme, customColor: Color? = null): CryoSpec = when (type) {
    ColorThemeType.CRYOGENIC -> CryogenicSpec
    ColorThemeType.SILVER -> SilverSpec
    ColorThemeType.CUSTOM -> if (customColor != null && isAchromatic(customColor)) cryoNeutralSpec(customColor)
    else CryoSpec(
        ice = scheme.primary,
        iceSoft = lerp(scheme.primary, scheme.secondary, 0.45f),
        aurora = scheme.tertiary,
        frost = lerp(scheme.onSurface, Color.White, 0.5f),
        deep = lerp(scheme.surfaceContainerLowest, Color.Black, 0.55f),
        navy = lerp(scheme.surfaceContainerHigh, Color.Black, 0.35f)
    )
    ColorThemeType.URBAN_ASH -> CryoSpec(
        ice = Color(0xFFD0D8E2),
        iceSoft = Color(0xFF8E9BAD),
        aurora = Color(0xFFE0C29B),
        frost = Color(0xFFF2F4F7),
        deep = Color(0xFF0D0F12),
        navy = Color(0xFF1D2128)
    )
    ColorThemeType.VOLCANIC -> CryoSpec(
        ice = Color(0xFFFF7A2E),
        iceSoft = Color(0xFFE5281C),
        aurora = Color(0xFFFFC23D),
        frost = Color(0xFFFFEFE6),
        deep = Color(0xFF140504),
        navy = Color(0xFF2A0B07)
    )
    ColorThemeType.MARINE -> CryoSpec(
        ice = Color(0xFF3FA9FF),
        iceSoft = Color(0xFF1446C8),
        aurora = Color(0xFF2EE6C9),
        frost = Color(0xFFE6F4FF),
        deep = Color(0xFF020B1C),
        navy = Color(0xFF071A3A)
    )
    ColorThemeType.GALAXY -> CryoSpec(
        ice = Color(0xFFB07CFF),
        iceSoft = Color(0xFF5B3BFF),
        aurora = Color(0xFFFF5FD2),
        frost = Color(0xFFF3EAFF),
        deep = Color(0xFF07041A),
        navy = Color(0xFF170E3A)
    )
    ColorThemeType.DESERT -> CryoSpec(
        ice = Color(0xFFF0C88F),
        iceSoft = Color(0xFFC8794A),
        aurora = Color(0xFFF7E2AE),
        frost = Color(0xFFFFF5E8),
        deep = Color(0xFF150F09),
        navy = Color(0xFF34261A)
    )
    //Folhas de Outono: vermelho (iceSoft) → laranja (ice) → amarelo (aurora) no degradê
    ColorThemeType.SAKURA -> CryoSpec(
        ice = Color(0xFFFFB3D1),
        iceSoft = Color(0xFFE5639E),
        aurora = Color(0xFFFFDDEB),
        frost = Color(0xFFFFF3F8),
        deep = Color(0xFF170812),
        navy = Color(0xFF3A1430)
    )
    ColorThemeType.GOLD -> CryoSpec(
        ice = Color(0xFFFFD24A),
        iceSoft = Color(0xFFE39A12),
        aurora = Color(0xFFFFF0A8),
        frost = Color(0xFFFFF8E1),
        deep = Color(0xFF110C02),
        navy = Color(0xFF2C2006)
    )
    else -> CryoSpec(
        ice = scheme.primary,
        iceSoft = lerp(scheme.primary, scheme.secondary, 0.45f),
        aurora = scheme.tertiary,
        frost = lerp(scheme.onSurface, Color.White, 0.5f),
        deep = lerp(scheme.surfaceContainerLowest, Color.Black, 0.55f),
        navy = lerp(scheme.surfaceContainerHigh, Color.Black, 0.35f)
    )
}

object CryoTheme {
    var spec by mutableStateOf(CryogenicSpec)
    @Volatile var inGame: Boolean = false
    val gradientPhase = mutableFloatStateOf(0f)
}

@Immutable
data class CryoColorAdjust(val saturation: Float = 0f, val vibrance: Float = 0f, val contrast: Float = 0f) {
    val isIdentity: Boolean get() = saturation == 0f && vibrance == 0f && contrast == 0f

    fun apply(color: Color): Color {
        if (isIdentity || color == Color.Unspecified) return color
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        var sat = hsl[1]
        sat = if (saturation >= 0f) sat + (1f - sat) * saturation * sat.coerceAtLeast(0.25f) else sat * (1f + saturation)
        sat = if (vibrance >= 0f) sat + (1f - sat) * (1f - sat) * vibrance else sat * (1f + vibrance * (1f - sat))
        hsl[1] = sat.coerceIn(0f, 1f)
        val rgb = Color(ColorUtils.HSLToColor(hsl))
        val factor = 1f + contrast
        fun c(v: Float) = ((v - 0.5f) * factor + 0.5f).coerceIn(0f, 1f)
        return Color(c(rgb.red), c(rgb.green), c(rgb.blue), color.alpha)
    }

    fun apply(spec: CryoSpec): CryoSpec = if (isIdentity) spec else CryoSpec(
        ice = apply(spec.ice),
        iceSoft = apply(spec.iceSoft),
        aurora = apply(spec.aurora),
        frost = apply(spec.frost),
        deep = apply(spec.deep),
        navy = apply(spec.navy),
        gradient = spec.gradient?.map { apply(it) }
    )

    fun apply(scheme: ColorScheme): ColorScheme = if (isIdentity) scheme else scheme.copy(
        primary = apply(scheme.primary),
        onPrimary = apply(scheme.onPrimary),
        primaryContainer = apply(scheme.primaryContainer),
        onPrimaryContainer = apply(scheme.onPrimaryContainer),
        inversePrimary = apply(scheme.inversePrimary),
        secondary = apply(scheme.secondary),
        onSecondary = apply(scheme.onSecondary),
        secondaryContainer = apply(scheme.secondaryContainer),
        onSecondaryContainer = apply(scheme.onSecondaryContainer),
        tertiary = apply(scheme.tertiary),
        onTertiary = apply(scheme.onTertiary),
        tertiaryContainer = apply(scheme.tertiaryContainer),
        onTertiaryContainer = apply(scheme.onTertiaryContainer),
        background = apply(scheme.background),
        surface = apply(scheme.surface),
        surfaceVariant = apply(scheme.surfaceVariant),
        surfaceTint = apply(scheme.surfaceTint),
        surfaceContainerLowest = apply(scheme.surfaceContainerLowest),
        surfaceContainerLow = apply(scheme.surfaceContainerLow),
        surfaceContainer = apply(scheme.surfaceContainer),
        surfaceContainerHigh = apply(scheme.surfaceContainerHigh),
        surfaceContainerHighest = apply(scheme.surfaceContainerHighest),
        outline = apply(scheme.outline),
        outlineVariant = apply(scheme.outlineVariant)
    )

    companion object {
        fun fromSettings(): CryoColorAdjust = CryoColorAdjust(
            saturation = AllSettings.cryoThemeSaturation.state / 100f,
            vibrance = AllSettings.cryoThemeVibrance.state / 100f,
            contrast = AllSettings.cryoThemeContrast.state / 200f
        )
    }
}

class CryoGradientBrush(private val colors: List<Color>) : ShaderBrush() {
    override fun createShader(size: Size): Shader = LinearGradientShader(
        from = Offset.Zero,
        to = Offset(size.width, 0f),
        colors = colors
    )

    override fun equals(other: Any?): Boolean = other is CryoGradientBrush && other.colors == colors
    override fun hashCode(): Int = colors.hashCode()
}

fun cryoGradientColorsLoop(): List<Color> = CryoTheme.spec.gradient?.let { it + it.reversed().drop(1) } ?: listOf(
    CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora, CryoGlassColors.Ice, CryoGlassColors.IceSoft
)

fun Modifier.cryoBackground(brush: Brush, shape: Shape = RectangleShape, alpha: Float = 1f): Modifier {
    if (brush !is CryoGradientBrush) return this.background(brush, shape, alpha)
    return this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind {
            if (AllSettings.cryoGradientAnimated.state && !CryoTheme.inGame) {
                val w = size.width.coerceAtLeast(1f)
                val shift = CryoTheme.gradientPhase.floatValue * 2f * w
                drawOutline(
                    outline,
                    Brush.linearGradient(
                        colors = cryoGradientColorsLoop(),
                        start = Offset(shift - 2f * w, 0f),
                        end = Offset(shift, size.height * 0.35f),
                        tileMode = TileMode.Repeated
                    ),
                    alpha = alpha
                )
            } else {
                drawOutline(outline, brush, alpha = alpha)
            }
            if (cryoLiquid) {
                //Liquid Glass: brilho de vidro por cima do degradê + aro claro
                drawOutline(
                    outline,
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.55f * alpha),
                        0.48f to Color.White.copy(alpha = 0.08f * alpha),
                        0.52f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.10f * alpha)
                    )
                )
                drawOutline(outline, Color.White.copy(alpha = 0.7f * alpha), style = Stroke(1.4.dp.toPx()))
            }
        }
    }
}

/** Interface 2.0 (Liquid Glass) ligada. */
val cryoLiquid: Boolean get() = AllSettings.cryoInterface2.state

/** Aro de vidro: claro em cima à esquerda, some no meio e volta embaixo à direita. */
fun cryoLiquidRim(strength: Float = 1f): Brush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = (0.95f * strength).coerceAtMost(1f)),
        Color.White.copy(alpha = (0.22f * strength).coerceAtMost(1f)),
        CryoGlassColors.Ice.copy(alpha = (0.45f * strength).coerceAtMost(1f)),
        Color.White.copy(alpha = (0.8f * strength).coerceAtMost(1f))
    )
)

@Composable
fun CryoGradientTicker() {
    val animated = AllSettings.cryoGradientAnimated.state
    val speed = AllSettings.cryoGradientSpeed.state
    LaunchedEffect(animated, speed) {
        if (!animated) {
            CryoTheme.gradientPhase.floatValue = 0f
            return@LaunchedEffect
        }
        val period = (12_000f - (speed - 1) / 99f * 10_800f).toLong().coerceAtLeast(600L)
        var start = -1L
        var base = CryoTheme.gradientPhase.floatValue
        while (true) {
            //Jogo aberto: não pede quadros (economiza CPU/GPU para o jogo); volta sozinho depois
            if (CryoTheme.inGame) {
                kotlinx.coroutines.delay(500L)
                start = -1L
                base = CryoTheme.gradientPhase.floatValue
                continue
            }
            val now = withFrameNanos { it }
            if (start < 0) start = now
            val elapsed = (now - start) / 1_000_000L
            CryoTheme.gradientPhase.floatValue = (base + elapsed.toFloat() / period) % 1f
            ambientPause(now)
        }
    }
}

/** Relógio único do brilho neon: uma volta (0..1) a cada ~3,6 s, lido só na fase de desenho. */
object CryoNeon {
    val phase = mutableFloatStateOf(0f)
    fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCryoNeonFor(cornerPx: Float, strength: Float) {
        drawCryoNeon(cornerPx.coerceAtMost(size.minDimension / 2f), phase.floatValue, strength, 2.dp.toPx())
    }
}

@Composable
fun CryoNeonTicker() {
    val running = AllSettings.cryoNeonEnabled.state
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var start = -1L
        while (true) {
            if (CryoTheme.inGame) {
                kotlinx.coroutines.delay(500L)
                start = -1L
                continue
            }
            val now = withFrameNanos { it }
            if (start < 0) start = now
            CryoNeon.phase.floatValue = ((now - start) / 1_000_000L % 3600L) / 3600f
            ambientPause(now)
        }
    }
}

/**
 * Animações de enfeite (degradê, neon, neve do wallpaper) andam a [AMBIENT_FPS] quadros por
 * segundo em vez de a cada quadro da tela (até 120/144 Hz): o movimento é lento, então o olho não
 * nota, mas a interface parada deixa de redesenhar a tela inteira a cada vsync. Rolagens, toques
 * e transições continuam na taxa cheia da tela.
 */
const val AMBIENT_FPS = 30
private const val AMBIENT_FRAME_NANOS = 1_000_000_000L / AMBIENT_FPS

/** Espera até o próximo quadro de enfeite, descontando o tempo já gasto desde [frameNanos]. */
suspend fun ambientPause(frameNanos: Long) {
    val spent = System.nanoTime() - frameNanos
    val wait = (AMBIENT_FRAME_NANOS - spent) / 1_000_000L
    if (wait > 1L) kotlinx.coroutines.delay(wait)
}

/**
 * Contorno neon na cor do tema: halo em camadas, linha viva e um feixe de luz que corre pela borda.
 * Só traços (sem desfoque), então é barato de desenhar a cada quadro.
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCryoNeon(
    cornerPx: Float,
    phase: Float,
    strength: Float = 1f,
    corePx: Float = 1.6.dp.toPx()
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    //Espessura escolhida pelo usuário: 1 = 0,55x ... 4 (padrão) = 1,0x ... 10 = 1,9x
    val core = corePx * (0.4f + AllSettings.cryoNeonThickness.state * 0.15f)
    //Pulsa pouco: o neon fica sempre forte, só "respira"
    val pulse = 0.88f + 0.12f * kotlin.math.sin(phase * 2f * Math.PI.toFloat())
    val k = (strength * pulse).coerceIn(0f, 1f)
    val radius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
    //Halo em camadas largas (o "clarão" do tubo de neon)
    for ((mul, alpha) in listOf(11f to 0.07f, 7.5f to 0.13f, 4.6f to 0.24f, 2.6f to 0.5f)) {
        drawRoundRect(ice.copy(alpha = alpha * k), cornerRadius = radius, style = Stroke(core * mul))
    }
    //Tubo colorido saturado + miolo branco quente
    drawRoundRect(
        Brush.linearGradient(listOf(ice, aurora, ice, aurora), start = Offset.Zero, end = Offset(w, h)),
        cornerRadius = radius,
        style = Stroke(core * 1.5f),
        alpha = strength.coerceIn(0f, 1f)
    )
    drawRoundRect(Color.White.copy(alpha = 0.85f * k), cornerRadius = radius, style = Stroke(core * 0.45f))
    //Dois feixes de luz correndo pela borda em sentidos opostos
    val span = w + h
    val band = span * 0.14f
    for (dir in 0..1) {
        val p = if (dir == 0) phase else 1f - phase
        val head = -band + (span + band * 2f) * p
        drawRoundRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White, Color.Transparent),
                start = Offset(head - band, 0f),
                end = Offset(head + band, band * 0.6f)
            ),
            cornerRadius = radius,
            style = Stroke(core * 1.9f),
            alpha = strength.coerceIn(0f, 1f)
        )
    }
}

/** Borda neon animada sempre ligada (independe das opções de brilho). */
fun Modifier.cryoNeonBorder(cornerRadius: Dp, strength: Float = 1f): Modifier = this.drawWithContent {
    drawContent()
    drawCryoNeon(cornerRadius.toPx().coerceAtMost(size.minDimension / 2f), CryoNeon.phase.floatValue, strength)
}

object CryoGlassColors {
    val Ice: Color get() = CryoTheme.spec.ice
    val IceSoft: Color get() = CryoTheme.spec.iceSoft
    val Frost: Color get() = CryoTheme.spec.frost
    val Deep: Color get() = CryoTheme.spec.deep
    val Navy: Color get() = CryoTheme.spec.navy
    val Aurora: Color get() = CryoTheme.spec.aurora
}

fun cryoGlassShape(radius: Dp = 18.dp) = RoundedCornerShape(radius)

fun cryoFillFactor(): Float = cryoFillFactor(AllSettings.launcherBackgroundOpacity.state / 100f)

fun cryoFillFactor(opacity: Float): Float = ((opacity - 0.2f) / 0.8f).coerceIn(0f, 1f) * 1.15f

fun cryoGradient(): Brush = CryoGradientBrush(
    CryoTheme.spec.gradient ?: listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora)
)

fun cryoBorderBrush(strength: Float = 1f): Brush = Brush.linearGradient(
    listOf(
        CryoGlassColors.Ice.copy(alpha = (0.75f * strength).coerceAtMost(1f)),
        CryoGlassColors.Ice.copy(alpha = (0.14f * strength).coerceAtMost(1f)),
        CryoGlassColors.Aurora.copy(alpha = (0.55f * strength).coerceAtMost(1f))
    )
)

fun Modifier.cryoGlow(cornerRadius: Dp, selected: Boolean = false, strength: Float = 1f): Modifier {
    if (CryoTheme.inGame) return this
    if (!AllSettings.cryoGlowEnabled.state) {
        //Só o neon ligado (sem o halo borrado): desenha apenas o contorno animado
        if (!AllSettings.cryoNeonEnabled.state) return this
        val s = (strength * (if (selected) 1f else 0.8f)).coerceIn(0f, 1f)
        return this.drawBehind {
            drawCryoNeon(cornerRadius.toPx().coerceAtMost(size.minDimension / 2f), CryoNeon.phase.floatValue, s)
        }
    }
    val level = AllSettings.cryoGlowSize.state
    val intensity = AllSettings.cryoGlowIntensity.state / 100f
    val color = CryoGlassColors.Ice
    val alpha = ((0.25f + 0.75f * intensity) * strength * (if (selected) 1f else 0.8f)).coerceIn(0f, 1f)
    //O brilho borrado não depende do tamanho do cartão: é uma moldura pequena (cantos + 1 faixa de
    //borda) desenhada uma única vez e esticada em volta de qualquer cartão. Antes cada tamanho de
    //cartão gerava a sua própria imagem borrada na thread da interface (cartões grandes, janelas e
    //qualquer coisa que mudasse de tamanho refaziam o desfoque), o que travava a tela.
    return this.drawWithCache {
        val radius = (1.5f + level * 1.6f).dp.toPx()
        val stroke = (1f + level * 0.35f).dp.toPx()
        val w = size.width.toInt()
        val h = size.height.toInt()
        val corner = cornerRadius.toPx().coerceAtMost(minOf(w, h) / 2f).coerceAtLeast(0f)
        val pad = (radius * 2f + stroke).toInt() + 1
        val slice = pad + kotlin.math.ceil(corner).toInt() + pad
        val tint = androidx.compose.ui.graphics.ColorFilter.tint(color.copy(alpha = alpha))
        val fits = w + pad * 2 >= slice * 2 && h + pad * 2 >= slice * 2
        val frame = if (w > 0 && h > 0 && fits) cryoGlowFrame(corner, radius, stroke, pad, slice) else null
        //Elemento pequeno demais para a moldura (botões redondos, pílulas): imagem do tamanho exato
        val exact = if (w > 0 && h > 0 && !fits) cryoGlowMask(w, h, pad, corner, radius, stroke) else null
        //Linha nítida em degradê por cima do halo: a borda fica definida em vez de só "embaçada"
        val edge = Brush.linearGradient(
            listOf(color, CryoGlassColors.Aurora, color),
            start = Offset.Zero,
            end = Offset(w.toFloat(), h.toFloat())
        )
        val edgeStroke = Stroke((0.8f + level * 0.07f).dp.toPx())
        val edgeRadius = androidx.compose.ui.geometry.CornerRadius(corner)
        onDrawBehind {
            if (frame != null) {
                drawGlowFrame(frame, w, h, pad, slice, tint)
            } else if (exact != null) {
                drawImage(
                    image = exact,
                    topLeft = Offset(-pad.toFloat(), -pad.toFloat()),
                    colorFilter = tint
                )
            }
            if (w > 0 && h > 0) {
                if (AllSettings.cryoNeonEnabled.state) {
                    drawCryoNeon(corner, CryoNeon.phase.floatValue, strength = alpha, corePx = edgeStroke.width * 1.3f)
                } else {
                    drawRoundRect(edge, cornerRadius = edgeRadius, style = edgeStroke, alpha = (alpha * 0.9f).coerceIn(0f, 1f))
                }
            }
        }
    }
}

/** Largura (px) da faixa central da moldura, a parte que é esticada. */
private const val GLOW_CENTER = 4

/** Desenha a moldura de brilho em volta de um cartão de [w] x [h]: cantos fixos, bordas esticadas. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlowFrame(
    frame: androidx.compose.ui.graphics.ImageBitmap,
    w: Int, h: Int, pad: Int, slice: Int,
    tint: androidx.compose.ui.graphics.ColorFilter
) {
    //Colunas/linhas de origem (na moldura) e de destino (na tela): canto, meio esticado, canto
    val src = intArrayOf(0, slice, slice + GLOW_CENTER, slice * 2 + GLOW_CENTER)
    val dstX = intArrayOf(-pad, -pad + slice, w + pad - slice, w + pad)
    val dstY = intArrayOf(-pad, -pad + slice, h + pad - slice, h + pad)
    for (row in 0..2) {
        for (col in 0..2) {
            if (row == 1 && col == 1) continue //O miolo do cartão não tem brilho
            val dw = dstX[col + 1] - dstX[col]
            val dh = dstY[row + 1] - dstY[row]
            if (dw <= 0 || dh <= 0) continue
            //Na faixa esticada usa só os 2 px do meio, para a borda não puxar cor dos cantos
            val sx = if (col == 1) src[1] + 1 else src[col]
            val sw = if (col == 1) GLOW_CENTER - 2 else slice
            val sy = if (row == 1) src[1] + 1 else src[row]
            val sh = if (row == 1) GLOW_CENTER - 2 else slice
            drawImage(
                image = frame,
                srcOffset = androidx.compose.ui.unit.IntOffset(sx, sy),
                srcSize = androidx.compose.ui.unit.IntSize(sw, sh),
                dstOffset = androidx.compose.ui.unit.IntOffset(dstX[col], dstY[row]),
                dstSize = androidx.compose.ui.unit.IntSize(dw, dh),
                colorFilter = tint
            )
        }
    }
}

/** Molduras de brilho já desenhadas, por forma (canto, raio e traço): só algumas, e pequenas. */
private val glowFrames = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(24)

private fun cryoGlowFrame(
    corner: Float, radius: Float, stroke: Float, pad: Int, slice: Int
): androidx.compose.ui.graphics.ImageBitmap? {
    val key = "${(corner * 4f).toInt()}:${(radius * 4f).toInt()}:${(stroke * 4f).toInt()}:$pad"
    glowFrames.get(key)?.let { return it }
    val side = slice * 2 + GLOW_CENTER
    if (side.toLong() * side > 4_000_000L) return null
    return runCatching {
        val bitmap = android.graphics.Bitmap.createBitmap(side, side, android.graphics.Bitmap.Config.ALPHA_8)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke
            this.color = android.graphics.Color.WHITE
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            pad.toFloat(), pad.toFloat(), (side - pad).toFloat(), (side - pad).toFloat(), corner, corner, paint
        )
        bitmap.asImageBitmap().also { glowFrames.put(key, it) }
    }.getOrNull()
}

/** Brilho no tamanho exato, só para elementos pequenos (onde a moldura não cabe). */
private val glowMasks = object : android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(6 * 1024 * 1024) {
    override fun sizeOf(key: String, value: androidx.compose.ui.graphics.ImageBitmap) = value.width * value.height
}

private fun cryoGlowMask(
    w: Int, h: Int, pad: Int, corner: Float, radius: Float, stroke: Float
): androidx.compose.ui.graphics.ImageBitmap? {
    val key = "$w:$h:$pad:${corner.toInt()}:${radius.toInt()}:${stroke.toInt()}"
    glowMasks.get(key)?.let { return it }
    val bw = w + pad * 2
    val bh = h + pad * 2
    if (bw.toLong() * bh > 1_500_000L) return null
    return runCatching {
        val bitmap = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ALPHA_8)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke
            this.color = android.graphics.Color.WHITE
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            pad.toFloat(), pad.toFloat(), (pad + w).toFloat(), (pad + h).toFloat(), corner, corner, paint
        )
        bitmap.asImageBitmap().also { glowMasks.put(key, it) }
    }.getOrNull()
}

/**
 * Dentro de janelas de opções (Estúdio de temas, Wallpaper, Discord, Modo Descanso...): o vidro usa
 * opacidade fixa de [CRYO_WINDOW_OPACITY] em vez da opacidade escolhida para a interface, e não
 * desenha a cópia desfocada do wallpaper (numa janela ela aparecia por cima de tudo).
 */
const val CRYO_WINDOW_OPACITY = 0.85f
val LocalCryoGlassFixed = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun Modifier.cryoGlass(
    shape: Shape = cryoGlassShape(),
    alpha: Float = 1f,
    glow: Boolean = true,
    selected: Boolean = false,
    cornerRadius: Dp = 18.dp,
    opaque: Boolean = false,
    /** Fundo de janela de opções: 85% de opacidade fixa, não muda com o ajuste de opacidade. */
    window: Boolean = false
): Modifier {
    val fixed = (LocalCryoGlassFixed.current || window) && !opaque
    val fillFactor = when {
        opaque -> 1f
        fixed -> cryoFillFactor(CRYO_WINDOW_OPACITY)
        else -> cryoFillFactor()
    }
    val top = CryoGlassColors.Navy.copy(
        alpha = when {
            opaque -> 0.97f
            window -> CRYO_WINDOW_OPACITY
            else -> (0.58f * alpha * fillFactor).coerceIn(0f, 1f)
        }
    )
    val bottom = CryoGlassColors.Deep.copy(
        alpha = when {
            opaque -> 0.98f
            window -> (CRYO_WINDOW_OPACITY + 0.03f).coerceAtMost(1f)
            else -> (0.70f * alpha * fillFactor).coerceIn(0f, 1f)
        }
    )
    val liquid = cryoLiquid
    val border = if (liquid) cryoLiquidRim(if (selected) 1.2f else 1f) else cryoBorderBrush(if (selected) 1.3f else 1f)
    val ice = CryoGlassColors.Ice
    val foregroundBlur = !fixed && CryoLiquid.blurType == BackgroundBlur.Foreground
    //Liquid Glass: sem desfoque atrás (janelas, fundo de vídeo) o vidro fica mais fechado para dar para ler
    val clear = liquid && foregroundBlur && CryoLiquid.blurRadius > 0
    val solid = opaque || window || fixed
    //vidro real (Android 13+): o fundo refratado aparece por trás, então o tom pode ser bem mais leve
    val lens = liquid && !solid && !CryoTheme.inGame && CryoBackdrop.supported
    val lensActive = lens && CryoBackdrop.layer != null
    return this
        .then(if (glow) Modifier.cryoGlow(cornerRadius, selected) else Modifier)
        .then(
            if (liquid && !CryoTheme.inGame) Modifier.drawBehind {
                //sombra macia embaixo do vidro (ele "flutua" sobre o fundo)
                val r = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
                for (i in 1..4) {
                    val grow = i * 1.6.dp.toPx()
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.11f / i),
                        topLeft = Offset(-grow, -grow + 3.dp.toPx()),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r + grow),
                        style = Stroke(2.2.dp.toPx())
                    )
                }
            } else Modifier
        )
        .clip(shape)
        .then(if (lens) Modifier.cryoLens(cornerRadius) else Modifier)
        .then(
            if (foregroundBlur) Modifier.backgroundGlass(CryoLiquid.blurRadius, Color.Transparent)
            else Modifier
        )
        .drawWithCache {
            val stroke = (if (liquid) 1.8.dp else if (selected) 1.5.dp else 1.dp).toPx()
            //Liquid Glass: base bem transparente (o fundo desfocado aparece) com um leve tom do tema
            val k = if (lensActive) 0.30f else if (clear) 0.40f else if (solid) 0.9f else 0.62f
            val fill = if (liquid) Brush.verticalGradient(
                listOf(top.copy(alpha = top.alpha * k), bottom.copy(alpha = bottom.alpha * (k + 0.1f)))
            ) else Brush.verticalGradient(listOf(top, bottom))
            val band = minOf(size.height * 0.6f, 44.dp.toPx()).coerceAtLeast(1f)
            //película clara do vidro
            val frost = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.17f * alpha), Color.White.copy(alpha = 0.06f * alpha), ice.copy(alpha = 0.10f * alpha))
            )
            val sheenTop = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.30f * alpha), Color.White.copy(alpha = 0.06f * alpha), Color.Transparent),
                startY = 0f, endY = band
            )
            val sheenBottom = Brush.verticalGradient(
                listOf(Color.Transparent, ice.copy(alpha = 0.24f * alpha)),
                startY = size.height - band, endY = size.height
            )
            //luz batendo no canto de cima à esquerda
            val corner = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.22f * alpha), Color.Transparent),
                start = Offset.Zero,
                end = Offset(minOf(size.width * 0.55f, 260.dp.toPx()), minOf(size.height * 0.9f, 200.dp.toPx()))
            )
            //borda grossa do vidro: dois aros internos mais fracos
            val inset1 = stroke + 1.dp.toPx()
            val inset2 = stroke + 3.dp.toPx()
            val bevel1 = if (liquid && size.minDimension > inset1 * 4) shape.createOutline(Size(size.width - inset1 * 2, size.height - inset1 * 2), layoutDirection, this) else null
            val bevel2 = if (liquid && size.minDimension > inset2 * 4) shape.createOutline(Size(size.width - inset2 * 2, size.height - inset2 * 2), layoutDirection, this) else null
            val outline = shape.createOutline(size, layoutDirection, this)
            val borderOutline = shape.createOutline(
                Size(size.width - stroke, size.height - stroke), layoutDirection, this
            )
            val highlight = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.18f * alpha), Color.Transparent)
            )
            val line = 1.dp.toPx()
            onDrawWithContent {
                drawOutline(outline, fill)
                if (liquid) {
                    drawOutline(outline, frost)
                    drawOutline(outline, corner)
                    drawOutline(outline, sheenTop)
                    drawOutline(outline, sheenBottom)
                    bevel1?.let { translate(inset1, inset1) { drawOutline(it, Color.White.copy(alpha = 0.20f * alpha), style = Stroke(1.2.dp.toPx())) } }
                    bevel2?.let { translate(inset2, inset2) { drawOutline(it, Color.White.copy(alpha = 0.08f * alpha), style = Stroke(1.6.dp.toPx())) } }
                }
                drawContent()
                translate(stroke / 2f, stroke / 2f) {
                    drawOutline(borderOutline, border, style = Stroke(stroke))
                }
                if (!liquid) drawRect(
                    brush = highlight,
                    topLeft = Offset(size.width * 0.08f, 0f),
                    size = Size(size.width * 0.84f, line)
                )
            }
        }
}

@Composable
fun rememberCryoClock(periodMillis: Int, enabled: Boolean = true, ambient: Boolean = false): State<Float> {
    return produceState(0f, periodMillis, enabled, ambient) {
        if (!enabled) return@produceState
        var start = -1L
        while (true) {
            val now = withFrameNanos { it }
            if (start < 0) start = now
            value = (((now - start) / 1_000_000L) % periodMillis) / periodMillis.toFloat()
            if (ambient) ambientPause(now)
        }
    }
}

@Composable
fun CryoWallpaper(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    /** Grava só a imagem (sem a neve) como fundo do Liquid Glass: o vidro não precisa refazer o efeito a cada floco. */
    backdrop: Boolean = false
) {
    val snow = remember {
        val r = Random(57197)
        List(36) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat(), 1f + r.nextFloat() * 2.0f, r.nextFloat() * 6.28f) }
    }
    val blur = AllSettings.backgroundBlur.state
    val blurred = blur > 0 && Build.VERSION.SDK_INT >= 31
    val moving = animated && !blurred
    val clock = rememberCryoClock(periodMillis = 80_000, enabled = moving, ambient = true)

    Box(
        modifier = modifier
            .background(Color(0xFF0A1424))
            .then(if (blurred) Modifier.blur(blur.dp) else Modifier)
    ) {
        androidx.compose.foundation.Image(
            modifier = Modifier
                .fillMaxSize()
                .cryoBackdropSource(enabled = backdrop),
            painter = cryoArtPainter(com.russoviller.cryogenic.R.drawable.img_cryo_wallpaper),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        if (moving) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val tau = 2f * PI.toFloat()
                        val vignette = safeRadialGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                            center = Offset(w / 2f, h / 2f),
                            radius = size.maxDimension * 0.8f
                        )
                        onDrawBehind {
                            val t = clock.value
                            val sway = t * tau * 8f
                            for (f in snow) {
                                val y = ((f[1] + t * 18f * f[2]) % 1f) * h
                                val x = ((f[0] + sin(sway + f[4]) * 0.01f + 1f) % 1f) * w
                                drawCircle(Color.White.copy(alpha = 0.30f + 0.35f * f[2] / 1.4f), f[3], Offset(x, y))
                            }
                            drawRect(vignette)
                        }
                    }
            )
        }
    }
}
