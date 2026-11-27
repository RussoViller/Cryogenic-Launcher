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

package com.russoviller.cryogenic.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.legal.AvailableAgreement
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CryoAnimatedTitle
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoLogo
import com.russoviller.cryogenic.ui.components.cryoArtPainter
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.main.control_editor.InfoLayoutTextItem
import com.russoviller.cryogenic.viewmodel.SplashBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/*
 * Tela de boas-vindas do Cryogenic (primeira abertura depois de instalar).
 *
 * 1. Cutscene (~4,6 s, toque para pular): ponto de luz, rachaduras de gelo, ondas de choque,
 *    a logo surge girando, "CRYOGENIC" cai letra por letra, "LAUNCHER" se abre e um clarão de
 *    gelo "congela" a tela e revela a boas-vindas.
 * 2. Apresentação em 3 etapas (deslizar ou Próximo): a arte do Cryogenic ocupa a tela toda, com
 *    aurora, neve e gelo se formando nas bordas.
 *      01 — logo em órbita, "BEM-VINDO AO" digitado e a marca CRYOGENIC com brilho;
 *      02 — o que é o Cryogenic e três cartões de recursos;
 *      03 — tudo pronto (+ termos de uso, quando houver) e o botão Começar.
 * 3. Ao começar: a tela se congela (clarão + neve acelerando) e segue para o Launcher.
 *
 * Tudo que se mexe o tempo todo (neve, aurora, brilhos) lê o relógio só na fase de desenho:
 * nada é recomposto a cada quadro.
 */

/** Duração da cutscene, em ms. As etapas abaixo são frações dela. */
private const val CUTSCENE_MS = 4600

/** Fração da cutscene em que a tela de boas-vindas começa a aparecer por baixo do clarão. */
private const val WELCOME_REVEAL_AT = 0.84f

@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    availableAgreement: StateFlow<AvailableAgreement?>,
    screenViewModel: SplashBackStackViewModel,
    modifier: Modifier = Modifier
) {
    BaseScreen(
        screenKey = SplashOperation.Welcome,
        currentKey = screenViewModel.splashScreen.currentKey
    ) { isVisible ->
        val agreement by availableAgreement.collectAsStateWithLifecycle()
        var agreementRequired by remember(agreement) { mutableStateOf(agreement != null) }
        var agreementChecked by remember(agreement) { mutableStateOf(false) }

        WelcomeExperience(
            modifier = modifier,
            isVisible = isVisible,
            canStart = !agreementRequired || agreementChecked,
            hasTerms = agreementRequired,
            onFinished = onFinished
        ) { termsModifier ->
            WelcomeTerms(
                availableAgreement = availableAgreement,
                agreementChecked = agreementChecked,
                onAgreementCheckedChange = { agreementChecked = it },
                onAgreementRequiredChange = { agreementRequired = it },
                modifier = termsModifier
            )
        }
    }
}

/** Quantidade de etapas da apresentação (boas-vindas, recursos, tudo pronto). */
private const val WELCOME_STEPS = 3

/**
 * A experiência completa (cutscene + apresentação em etapas), sem depender de ViewModel nem de
 * ajustes salvos.
 * @param hasTerms se há termos de uso para aceitar na última etapa
 * @param terms conteúdo dos termos (lista de documentos + confirmação)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun WelcomeExperience(
    isVisible: Boolean,
    canStart: Boolean,
    hasTerms: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    terms: @Composable (Modifier) -> Unit
) {
    val scope = rememberCoroutineScope()
    val time = rememberWelcomeTime()
    val cut = remember { Animatable(0f) }
    val enter = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    var started by remember { mutableStateOf(false) }
    val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { WELCOME_STEPS })

    LaunchedEffect(isVisible) {
        if (isVisible && cut.value < 1f) {
            cut.animateTo(1f, tween(durationMillis = (CUTSCENE_MS * (1f - cut.value)).toInt(), easing = LinearEasing))
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { cut.value >= WELCOME_REVEAL_AT }.first { it }
        enter.animateTo(1f, tween(durationMillis = 1600, easing = LinearEasing))
    }

    val showWelcome by remember { derivedStateOf { cut.value >= WELCOME_REVEAL_AT - 0.02f } }
    val showCutscene by remember { derivedStateOf { cut.value < 1f } }

    val start: () -> Unit = {
        if (canStart && !started) {
            started = true
            scope.launch {
                leave.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
                onFinished()
            }
        }
    }
    val goTo: (Int) -> Unit = { page ->
        scope.launch { pager.animateScrollToPage(page.coerceIn(0, WELCOME_STEPS - 1)) }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(CryoGlassColors.Deep, CryoGlassColors.Navy, CryoGlassColors.Deep))
        )
    ) {
        if (showWelcome) {
            WelcomeSceneArt(modifier = Modifier.fillMaxSize(), time = time, enter = enter.asState(), pager = pager)
        }

        WelcomeBackdrop(
            modifier = Modifier.fillMaxSize(),
            time = time,
            cut = cut.asState(),
            leave = leave.asState()
        )

        if (showWelcome) {
            FrostFrame(modifier = Modifier.fillMaxSize(), enter = enter.asState())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val l = leave.value
                        alpha = (1f - l) * seg(enter.value, 0f, 0.25f)
                        scaleX = 1f + 0.05f * l
                        scaleY = 1f + 0.05f * l
                    }
            ) {
                WelcomeSteps(
                    pager = pager,
                    time = time,
                    enter = enter.asState(),
                    canStart = canStart,
                    hasTerms = hasTerms,
                    started = started,
                    onStart = start,
                    goTo = goTo,
                    terms = terms
                )
            }
        }

        if (showCutscene) {
            WelcomeCutscene(
                modifier = Modifier.fillMaxSize(),
                time = time,
                cut = cut.asState(),
                onSkip = {
                    if (cut.value < WELCOME_REVEAL_AT - 0.04f) {
                        scope.launch {
                            cut.snapTo(WELCOME_REVEAL_AT - 0.04f)
                            cut.animateTo(1f, tween(durationMillis = 650, easing = LinearEasing))
                        }
                    }
                }
            )
        }

        // Clarão de gelo: no fim da cutscene e ao começar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val c = cut.value
                    val flashIn = bell(c, 0.80f, 0.86f, 0.98f) * 0.55f
                    val flashOut = sin(leave.value * PI.toFloat()).coerceAtLeast(0f) * 0.6f
                    alpha = maxOf(flashIn, flashOut)
                }
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.95f), CryoGlassColors.Frost.copy(alpha = 0.75f), CryoGlassColors.Ice.copy(alpha = 0.35f))
                    )
                )
        )
    }
}


// =====================================================================
// Relógio e curvas
// =====================================================================

/** Segundos desde que a tela abriu (lido só na fase de desenho). */
@Composable
private fun rememberWelcomeTime(): State<Float> {
    val state = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0L) start = now
                state.floatValue = (now - start) / 1_000_000_000f
            }
        }
    }
    return state
}

/** Progresso 0..1 de [v] entre [from] e [to]. */
private fun seg(v: Float, from: Float, to: Float): Float = ((v - from) / (to - from)).coerceIn(0f, 1f)

private fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

private fun easeInOut(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f

/** Sobe e passa um pouco do ponto antes de assentar (efeito "mola"). */
private fun overshoot(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val p = x - 1f
    return 1f + c3 * p * p * p + c1 * p * p
}

/** Sobe de [a] a [b] e desce até [c]. */
private fun bell(v: Float, a: Float, b: Float, c: Float): Float = when {
    v <= a || v >= c -> 0f
    v < b -> seg(v, a, b)
    else -> 1f - seg(v, b, c)
}

/** Revelação escalonada do item [index] durante a entrada (0..1). */
private fun reveal(enter: Float, index: Int): Float = easeOut(seg(enter, index * 0.07f, index * 0.07f + 0.45f))

private fun Modifier.revealItem(enter: State<Float>, index: Int, dx: Float = 0f, dy: Float = 22f): Modifier =
    this.graphicsLayer {
        val p = reveal(enter.value, index)
        alpha = p
        translationX = dx * density * (1f - p)
        translationY = dy * density * (1f - p)
    }

// =====================================================================
// Fundo: aurora + neve
// =====================================================================

private class Flake(val x: Float, val y: Float, val r: Float, val speed: Float, val phase: Float, val alpha: Float)

@Composable
private fun WelcomeBackdrop(
    modifier: Modifier,
    time: State<Float>,
    cut: State<Float>,
    leave: State<Float>
) {
    val flakes = remember {
        val r = Random(5251)
        List(72) {
            Flake(
                x = r.nextFloat(),
                y = r.nextFloat(),
                r = 0.6f + r.nextFloat() * 1.9f,
                speed = 0.018f + r.nextFloat() * 0.045f,
                phase = r.nextFloat() * 6.28f,
                alpha = 0.25f + r.nextFloat() * 0.6f
            )
        }
    }
    val deep = CryoGlassColors.Deep
    val navy = CryoGlassColors.Navy
    val ice = CryoGlassColors.Ice
    val iceSoft = CryoGlassColors.IceSoft
    val aurora = CryoGlassColors.Aurora
    val frost = CryoGlassColors.Frost

    Canvas(modifier = modifier) {
        val t = time.value
        val w = size.width
        val h = size.height
        // a aurora acende junto com a cutscene
        val glow = 0.25f + 0.75f * easeOut(seg(cut.value, 0.05f, 0.6f))

        // faixas de aurora ondulando
        val bands = 3
        for (b in 0 until bands) {
            val baseY = h * (0.22f + b * 0.17f)
            val amp = h * (0.06f + b * 0.02f)
            val freq = 1.4f + b * 0.6f
            val speed = 0.22f + b * 0.09f
            val path = Path()
            val steps = 36
            for (i in 0..steps) {
                val px = w * i / steps
                val u = i / steps.toFloat()
                val py = baseY + sin(u * freq * 2f * PI.toFloat() + t * speed + b * 1.7f) * amp +
                    sin(u * 9f + t * 0.6f + b) * amp * 0.18f
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val colors = when (b) {
                0 -> listOf(Color.Transparent, iceSoft, aurora, Color.Transparent)
                1 -> listOf(Color.Transparent, aurora, ice, Color.Transparent)
                else -> listOf(Color.Transparent, ice, iceSoft, Color.Transparent)
            }
            val brush = Brush.horizontalGradient(colors)
            val baseAlpha = (0.10f - b * 0.02f) * glow
            // três passadas (larga e fraca → fina e forte) imitam um brilho borrado
            drawPath(path, brush, alpha = baseAlpha * 0.6f, style = Stroke(width = h * 0.20f, cap = StrokeCap.Round))
            drawPath(path, brush, alpha = baseAlpha, style = Stroke(width = h * 0.09f, cap = StrokeCap.Round))
            drawPath(path, brush, alpha = baseAlpha * 1.6f, style = Stroke(width = h * 0.025f, cap = StrokeCap.Round))
        }

        // neve (acelera ao começar)
        val boost = 1f + leave.value * 6f
        val unit = min(w, h) / 360f
        flakes.forEach { f ->
            val y = ((f.y + t * f.speed * boost) % 1.08f) - 0.04f
            val x = f.x + sin(t * 0.5f + f.phase) * 0.012f
            drawCircle(
                color = frost,
                radius = f.r * unit,
                center = Offset(((x % 1f) + 1f) % 1f * w, y * h),
                alpha = f.alpha * glow
            )
        }

        // vinheta
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, deep.copy(alpha = 0.75f)),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.75f
            )
        )
    }
}

// =====================================================================
// Cutscene
// =====================================================================

@Composable
private fun WelcomeCutscene(
    modifier: Modifier,
    time: State<Float>,
    cut: State<Float>,
    onSkip: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val frost = CryoGlassColors.Frost
    val deep = CryoGlassColors.Deep
    //Centro da logo na tela: luz, rachaduras, ondas e halo saem exatamente dela
    var logoCenter by remember { mutableStateOf<Offset?>(null) }
    val rays = remember {
        val r = Random(913)
        List(11) { i -> floatArrayOf(i / 11f * 6.283f + r.nextFloat() * 0.35f, 0.55f + r.nextFloat() * 0.45f, r.nextFloat()) }
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkip
            )
            .graphicsLayer {
                // a cutscene some por baixo do clarão
                alpha = 1f - seg(cut.value, 0.86f, 0.97f)
            },
        contentAlignment = Alignment.Center
    ) {
        // escurece o fundo no começo e vai abrindo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = cut.value
            drawRect(deep, alpha = 1f - 0.55f * easeOut(seg(c, 0.0f, 0.35f)))

            val center = logoCenter ?: Offset(size.width / 2f, size.height * 0.40f)
            val unit = min(size.width, size.height)

            // ponto de luz que cresce
            val core = easeOut(seg(c, 0.0f, 0.12f)) * (1f - seg(c, 0.30f, 0.45f))
            if (core > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, frost.copy(alpha = 0.8f), ice.copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = unit * (0.04f + 0.22f * core)
                    ),
                    radius = unit * (0.04f + 0.22f * core),
                    center = center,
                    alpha = core
                )
            }

            // rachaduras de gelo saindo do centro
            val crack = easeOut(seg(c, 0.05f, 0.24f))
            val crackAlpha = 1f - seg(c, 0.30f, 0.50f)
            if (crack > 0f && crackAlpha > 0f) {
                rays.forEach { ray ->
                    val len = unit * 0.48f * ray[1] * crack
                    val a = ray[0]
                    val mid = Offset(center.x + cos(a + 0.08f) * len * 0.5f, center.y + sin(a + 0.08f) * len * 0.5f)
                    val end = Offset(center.x + cos(a) * len, center.y + sin(a) * len)
                    val stroke = (1.2f + ray[2] * 1.6f) * density
                    drawLine(ice, center, mid, strokeWidth = stroke, alpha = crackAlpha * 0.9f, cap = StrokeCap.Round)
                    drawLine(frost, mid, end, strokeWidth = stroke * 0.7f, alpha = crackAlpha * 0.7f, cap = StrokeCap.Round)
                }
            }

            // duas ondas de choque
            for ((startAt, k) in listOf(0.12f to 1f, 0.20f to 0.8f)) {
                val p = seg(c, startAt, startAt + 0.30f)
                if (p > 0f && p < 1f) {
                    drawCircle(
                        color = if (k == 1f) ice else aurora,
                        radius = unit * 0.75f * easeOut(p) * k,
                        center = center,
                        alpha = (1f - p) * 0.9f,
                        style = Stroke(width = 3.5f * density * (1f - p) + 0.5f * density)
                    )
                }
            }

            // halo atrás da logo
            val halo = easeOut(seg(c, 0.14f, 0.34f)) * (1f - seg(c, 0.84f, 0.95f))
            if (halo > 0f) {
                val pulse = 0.85f + 0.15f * sin(time.value * 3f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ice.copy(alpha = 0.55f), aurora.copy(alpha = 0.18f), Color.Transparent),
                        center = center,
                        radius = unit * 0.24f * pulse
                    ),
                    radius = unit * 0.24f * pulse,
                    center = center,
                    alpha = halo
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val shortSide = min(maxWidth.value, maxHeight.value)
            val logoSize = (shortSide * 0.30f).coerceIn(84f, 150f).dp
            val titleSize = (shortSide * 0.13f).coerceIn(30f, 60f).sp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val out = easeInOut(seg(cut.value, 0.82f, 0.95f))
                        scaleX = 1f - 0.12f * out
                        scaleY = 1f - 0.12f * out
                        translationY = -24f * density * out
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // logo: surge pequena, girando, e assenta com uma "mola"
                CryoLogo(
                    modifier = Modifier
                        .size(logoSize)
                        .onGloballyPositioned { coords ->
                            val p = coords.positionInRoot()
                            val c = Offset(p.x + coords.size.width / 2f, p.y + coords.size.height / 2f)
                            if (logoCenter == null || (logoCenter!! - c).getDistance() > 2f) logoCenter = c
                        }
                        .graphicsLayer {
                            val p = seg(cut.value, 0.12f, 0.32f)
                            val s = 0.3f + 0.7f * overshoot(p)
                            alpha = seg(cut.value, 0.12f, 0.18f)
                            scaleX = s
                            scaleY = s
                            rotationZ = -40f * (1f - easeOut(p))
                        }
                )
                Spacer(modifier = Modifier.height(18.dp))

                // CRYOGENIC caindo letra por letra
                val word = "CRYOGENIC"
                val titleBrush = Brush.linearGradient(listOf(frost, ice, aurora))
                Row {
                    word.forEachIndexed { i, ch ->
                        Text(
                            modifier = Modifier.graphicsLayer {
                                val p = seg(cut.value, 0.30f + i * 0.022f, 0.38f + i * 0.022f)
                                alpha = p
                                translationY = -26f * density * (1f - easeOut(p))
                                val s = 1.5f - 0.5f * easeOut(p)
                                scaleX = s
                                scaleY = s
                            },
                            text = ch.toString(),
                            style = TextStyle(
                                brush = titleBrush,
                                fontSize = titleSize,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                shadow = androidx.compose.ui.graphics.Shadow(ice.copy(alpha = 0.6f), blurRadius = 24f)
                            )
                        )
                    }
                }

                // LAUNCHER se abrindo + linha
                Text(
                    modifier = Modifier.graphicsLayer {
                        val p = easeOut(seg(cut.value, 0.52f, 0.68f))
                        alpha = p
                        scaleX = 0.6f + 0.4f * p
                    },
                    text = "L  A  U  N  C  H  E  R",
                    style = TextStyle(
                        color = frost.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(220.dp)
                        .graphicsLayer {
                            scaleX = easeOut(seg(cut.value, 0.56f, 0.72f))
                        }
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, ice, aurora, Color.Transparent)))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    modifier = Modifier.graphicsLayer {
                        val p = easeOut(seg(cut.value, 0.64f, 0.76f))
                        alpha = p
                        translationY = 10f * density * (1f - p)
                    },
                    text = stringResource(R.string.welcome_screen_subtitle),
                    style = TextStyle(
                        color = frost.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // "toque para pular"
            Text(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .graphicsLayer {
                        alpha = seg(cut.value, 0.06f, 0.14f) * (1f - seg(cut.value, 0.78f, 0.84f)) *
                            (0.55f + 0.45f * sin(time.value * 2.6f).let { it * it })
                    }
                    .border(1.dp, ice.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                text = stringResource(R.string.welcome_screen_skip) + "  ›",
                style = TextStyle(
                    color = frost,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

// =====================================================================
// Cenário da apresentação: arte em tela cheia + moldura de gelo
// =====================================================================

/** Arte do Cryogenic ocupando a tela toda, com paralaxe que acompanha as etapas. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WelcomeSceneArt(
    modifier: Modifier,
    time: State<Float>,
    enter: State<Float>,
    pager: androidx.compose.foundation.pager.PagerState
) {
    val deep = CryoGlassColors.Deep
    Box(modifier = modifier.graphicsLayer { alpha = easeOut(seg(enter.value, 0f, 0.5f)) }) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = time.value
                    val s = 1.16f + 0.03f * sin(t * 0.3f)
                    scaleX = s
                    scaleY = s
                    // a arte desliza um pouco para o lado conforme a etapa (paralaxe)
                    val page = pager.currentPage + pager.currentPageOffsetFraction
                    translationX = (-page * 26f + sin(t * 0.2f) * 8f) * density
                    translationY = cos(t * 0.16f) * 5f * density
                },
            painter = cryoArtPainter(R.drawable.img_cryo_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(deep.copy(alpha = 0.42f), deep.copy(alpha = 0.62f), deep.copy(alpha = 0.92f))))
        )
    }
}

/** Gelo se formando nas bordas da tela, como em um vidro congelado. */
@Composable
private fun FrostFrame(modifier: Modifier, enter: State<Float>) {
    val frost = CryoGlassColors.Frost
    val ice = CryoGlassColors.Ice
    // cristais: ponto de partida na borda (0..1 do perímetro), ângulo, tamanho e galhos
    val crystals = remember {
        val r = Random(4471)
        List(26) { floatArrayOf(r.nextFloat(), (r.nextFloat() - 0.5f) * 1.2f, 0.04f + r.nextFloat() * 0.07f, r.nextFloat()) }
    }
    Canvas(modifier = modifier) {
        val grow = easeOut(seg(enter.value, 0.15f, 0.85f))
        if (grow <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val unit = min(w, h)

        // brilho frio nos cantos
        listOf(Offset(0f, 0f), Offset(w, 0f), Offset(0f, h), Offset(w, h)).forEach { c ->
            drawCircle(
                brush = Brush.radialGradient(listOf(frost.copy(alpha = 0.16f * grow), Color.Transparent), center = c, radius = unit * 0.42f),
                radius = unit * 0.42f,
                center = c
            )
        }

        // cristais nascendo das bordas
        crystals.forEach { k ->
            val p = k[0] * 4f
            val side = p.toInt().coerceAtMost(3)
            val f = p - side
            val (start, inward) = when (side) {
                0 -> Offset(f * w, 0f) to PI.toFloat() / 2f
                1 -> Offset(w, f * h) to PI.toFloat()
                2 -> Offset((1f - f) * w, h) to -PI.toFloat() / 2f
                else -> Offset(0f, (1f - f) * h) to 0f
            }
            val ang = inward + k[1]
            val len = unit * k[2] * grow
            val end = Offset(start.x + cos(ang) * len, start.y + sin(ang) * len)
            val stroke = (0.8f + k[3]) * density
            drawLine(frost, start, end, strokeWidth = stroke, alpha = 0.32f, cap = StrokeCap.Round)
            // dois galhos em "V" no meio
            val mid = Offset(start.x + cos(ang) * len * 0.55f, start.y + sin(ang) * len * 0.55f)
            for (s in listOf(-0.7f, 0.7f)) {
                val b = Offset(mid.x + cos(ang + s) * len * 0.32f, mid.y + sin(ang + s) * len * 0.32f)
                drawLine(ice, mid, b, strokeWidth = stroke * 0.7f, alpha = 0.26f, cap = StrokeCap.Round)
            }
        }
    }
}

// =====================================================================
// Apresentação em etapas
// =====================================================================

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WelcomeSteps(
    pager: androidx.compose.foundation.pager.PagerState,
    time: State<Float>,
    enter: State<Float>,
    canStart: Boolean,
    hasTerms: Boolean,
    started: Boolean,
    onStart: () -> Unit,
    goTo: (Int) -> Unit,
    terms: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        Column(modifier = Modifier.fillMaxSize()) {
            // topo: marca pequena + pular
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .revealItem(enter, 1, dy = -14f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❄  CRYOGENIC  ·  ${BuildConfig.VERSION_NAME}",
                    color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                val last = pager.currentPage >= WELCOME_STEPS - 1
                Text(
                    modifier = Modifier
                        .graphicsLayer { alpha = if (last) 0f else 1f }
                        .clip(CircleShape)
                        .clickable(enabled = !last) { goTo(WELCOME_STEPS - 1) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    text = stringResource(R.string.welcome_screen_skip_intro) + "  »",
                    color = CryoGlassColors.Ice,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                beyondViewportPageCount = 1
            ) { page ->
                // a etapa que sai fica menor, inclinada e transparente
                val pageModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction)
                        val a = kotlin.math.abs(off).coerceIn(0f, 1f)
                        alpha = 1f - a * 0.85f
                        val s = 1f - 0.12f * a
                        scaleX = s
                        scaleY = s
                        rotationY = off * -18f
                        cameraDistance = 14f * density
                    }
                val active = pager.settledPage == page && enter.value > 0.25f
                Box(modifier = pageModifier, contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> StepHello(time, active, landscape)
                        1 -> StepFeatures(time, active, landscape)
                        else -> StepReady(time, active, hasTerms, terms)
                    }
                }
            }

            WelcomeDock(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealItem(enter, 3, dy = 24f),
                pager = pager,
                time = time,
                canStart = canStart,
                started = started,
                onStart = onStart,
                goTo = goTo,
                compact = !landscape
            )
        }
    }
}

/** Revelação própria de cada etapa: começa quando a etapa fica na tela. */
@Composable
private fun rememberStepReveal(active: Boolean): State<Float> {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active && anim.value < 1f) anim.animateTo(1f, tween(durationMillis = 1300, easing = LinearEasing))
    }
    return anim.asState()
}

/** Etapa 1: logo em órbita, "BEM-VINDO AO" digitado e a marca CRYOGENIC. */
@Composable
private fun StepHello(time: State<Float>, active: Boolean, landscape: Boolean) {
    val rv = rememberStepReveal(active)
    val ice = CryoGlassColors.Ice
    val content: @Composable () -> Unit = {
        OrbitLogo(
            modifier = Modifier
                .size(if (landscape) 150.dp else 190.dp)
                .revealItem(rv, 0, dy = 0f)
                .graphicsLayer {
                    val p = reveal(rv.value, 0)
                    val s = 0.6f + 0.4f * overshoot(p)
                    scaleX = s
                    scaleY = s
                },
            time = time
        )
        Column(
            horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally
        ) {
            TypedLine(
                modifier = Modifier.revealItem(rv, 1),
                text = stringResource(R.string.welcome_screen_kicker).uppercase(),
                typing = reveal(rv.value, 1) > 0.4f,
                time = time
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerWordmark(
                modifier = Modifier.revealItem(rv, 2, dy = 30f),
                time = time,
                fontSize = if (landscape) 54.sp else 46.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .graphicsLayer {
                        scaleX = reveal(rv.value, 3)
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (landscape) 0f else 0.5f, 0.5f)
                    }
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(ice, CryoGlassColors.Aurora, Color.Transparent)))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.revealItem(rv, 4),
                text = stringResource(R.string.welcome_screen_subtitle),
                color = CryoGlassColors.Frost.copy(alpha = 0.82f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (landscape) {
        Row(
            modifier = Modifier.padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) { content() }
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) { content() }
    }
}

/** Etapa 2: o que o Cryogenic é + três cartões de recursos. */
@Composable
private fun StepFeatures(time: State<Float>, active: Boolean, landscape: Boolean) {
    val rv = rememberStepReveal(active)
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepTitle(modifier = Modifier.revealItem(rv, 0), number = "02", text = stringResource(R.string.welcome_screen_features_title))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            modifier = Modifier.revealItem(rv, 1),
            text = stringResource(R.string.welcome_screen_intro_primary, BuildKeys.LAUNCHER_NAME) + " " +
                stringResource(R.string.welcome_screen_intro_core),
            color = CryoGlassColors.Frost.copy(alpha = 0.8f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        val items = listOf(
            Triple(R.drawable.ic_bolt, R.string.welcome_screen_feature_light, R.string.welcome_screen_feature_light_desc),
            Triple(R.drawable.ic_cryo_snowflake, R.string.welcome_screen_feature_modern, R.string.welcome_screen_feature_modern_desc),
            Triple(R.drawable.ic_android_filled, R.string.welcome_screen_feature_android, R.string.welcome_screen_feature_android_desc)
        )
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items.forEachIndexed { i, (icon, title, desc) ->
                    FeatureCard(
                        modifier = Modifier
                            .weight(1f)
                            .revealItem(rv, 2 + i, dy = 34f),
                        time = time,
                        index = i,
                        icon = icon,
                        title = stringResource(title),
                        desc = stringResource(desc),
                        compact = false
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { i, (icon, title, desc) ->
                    FeatureCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .revealItem(rv, 2 + i, dx = if (i % 2 == 0) -40f else 40f, dy = 0f),
                        time = time,
                        index = i,
                        icon = icon,
                        title = stringResource(title),
                        desc = stringResource(desc),
                        compact = true
                    )
                }
            }
        }
    }
}

/** Etapa 3: tudo pronto (+ termos, quando houver). */
@Composable
private fun StepReady(time: State<Float>, active: Boolean, hasTerms: Boolean, terms: @Composable (Modifier) -> Unit) {
    val rv = rememberStepReveal(active)
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .revealItem(rv, 0, dy = 0f)
                .graphicsLayer {
                    val s = 0.5f + 0.5f * overshoot(reveal(rv.value, 0))
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center
        ) {
            HexBadge(modifier = Modifier.fillMaxSize(), time = time, index = 3)
            Text(text = "✓", color = CryoGlassColors.Deep, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(10.dp))
        StepTitle(modifier = Modifier.revealItem(rv, 1), number = "03", text = stringResource(R.string.welcome_screen_ready_title))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            modifier = Modifier.revealItem(rv, 2),
            text = stringResource(if (hasTerms) R.string.welcome_screen_ready_terms else R.string.welcome_screen_ready_desc),
            color = CryoGlassColors.Frost.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        terms(Modifier.fillMaxWidth().revealItem(rv, 3))
    }
}

@Composable
private fun StepTitle(modifier: Modifier, number: String, text: String) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = number,
            style = TextStyle(
                brush = Brush.linearGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora)),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(width = 2.dp, height = 22.dp)
                .background(CryoGlassColors.Ice.copy(alpha = 0.5f))
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

/** Logo com dois anéis de gelo girando em órbita e faíscas passando. */
@Composable
private fun OrbitLogo(modifier: Modifier, time: State<Float>) {
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val frost = CryoGlassColors.Frost
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time.value
            val c = center
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(listOf(ice.copy(alpha = 0.40f), aurora.copy(alpha = 0.10f), Color.Transparent), center = c, radius = r),
                radius = r,
                center = c
            )
            // anéis inclinados (elipses), cada um girando em um sentido
            listOf(Triple(0.96f, 0.34f, 0.35f), Triple(0.80f, 0.30f, -0.55f)).forEachIndexed { i, (rx, ry, speed) ->
                val tilt = (if (i == 0) 24f else -38f) + t * speed * 20f
                rotate(tilt, c) {
                    drawOval(
                        brush = Brush.linearGradient(listOf(ice.copy(alpha = 0.1f), ice, aurora, ice.copy(alpha = 0.1f))),
                        topLeft = Offset(c.x - r * rx, c.y - r * ry),
                        size = Size(r * rx * 2f, r * ry * 2f),
                        style = Stroke(width = 1.6f * density)
                    )
                    // faísca percorrendo o anel
                    val a = t * (1.1f + i * 0.5f) * (if (i == 0) 1f else -1f)
                    val p = Offset(c.x + cos(a) * r * rx, c.y + sin(a) * r * ry)
                    drawCircle(Brush.radialGradient(listOf(Color.White, frost.copy(alpha = 0.5f), Color.Transparent), center = p, radius = 7f * density), 7f * density, p)
                }
            }
        }
        CryoLogo(modifier = Modifier.fillMaxSize(0.56f), floating = true)
    }
}

/** Texto curto digitado com cursor de gelo. */
@Composable
private fun TypedLine(modifier: Modifier, text: String, typing: Boolean, time: State<Float>) {
    var typed by remember { mutableIntStateOf(0) }
    LaunchedEffect(typing, text) {
        if (!typing) return@LaunchedEffect
        while (typed < text.length) {
            delay(if (text[typed] == ' ') 35L else 60L)
            typed++
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.take(typed),
            color = CryoGlassColors.Ice,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(width = 10.dp, height = 3.dp)
                .graphicsLayer { alpha = if (((time.value * 2f).toInt() % 2) == 0) 1f else 0.1f }
                .background(CryoGlassColors.Ice)
        )
    }
}

/** "CRYOGENIC" grande, em degradê, com um brilho de gelo passando pelas letras. */
@Composable
private fun ShimmerWordmark(modifier: Modifier, time: State<Float>, fontSize: androidx.compose.ui.unit.TextUnit) {
    val ice = CryoGlassColors.Ice
    Text(
        modifier = modifier
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val cycle = (time.value % 3.2f) / 3.2f
                    val x = -size.width * 0.4f + cycle * size.width * 1.8f
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                            start = Offset(x - size.height * 0.6f, 0f),
                            end = Offset(x + size.height * 0.6f, size.height)
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop
                    )
                }
            },
        text = "CRYOGENIC",
        maxLines = 1,
        style = TextStyle(
            brush = Brush.linearGradient(listOf(CryoGlassColors.Frost, ice, CryoGlassColors.Aurora)),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            shadow = androidx.compose.ui.graphics.Shadow(ice.copy(alpha = 0.55f), blurRadius = 26f)
        )
    )
}

/** Hexágono de gelo (fundo dos ícones). */
@Composable
private fun HexBadge(modifier: Modifier, time: State<Float>, index: Int) {
    val ice = CryoGlassColors.Ice
    val iceSoft = CryoGlassColors.IceSoft
    val aurora = CryoGlassColors.Aurora
    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension / 2f
        val pulse = 0.75f + 0.25f * sin(time.value * 2f + index * 1.4f)
        drawCircle(Brush.radialGradient(listOf(ice.copy(alpha = 0.35f * pulse), Color.Transparent), center = c, radius = r * 1.25f), r * 1.25f, c)
        val path = Path()
        for (k in 0..5) {
            val a = (PI.toFloat() / 3f) * k - PI.toFloat() / 2f
            val p = Offset(c.x + cos(a) * r * 0.9f, c.y + sin(a) * r * 0.9f)
            if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, Brush.linearGradient(listOf(iceSoft, ice, aurora)))
        drawPath(path, Color.White.copy(alpha = 0.55f), style = Stroke(width = 1.4f * density))
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier,
    time: State<Float>,
    index: Int,
    icon: Int,
    title: String,
    desc: String,
    compact: Boolean
) {
    val shape = RoundedCornerShape(22.dp)
    val base = modifier
        .graphicsLayer {
            // flutua devagar, cada cartão no seu ritmo
            translationY = sin(time.value * 1.3f + index * 2.1f) * 3f * density
        }
        .clip(shape)
        .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy.copy(alpha = 0.72f), CryoGlassColors.Deep.copy(alpha = 0.82f))))
        .frostBorder(time, 22f, CryoGlassColors.Ice.copy(alpha = 0.75f), CryoGlassColors.Aurora.copy(alpha = 0.75f))
    val badge: @Composable () -> Unit = {
        Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            HexBadge(modifier = Modifier.fillMaxSize(), time = time, index = index)
            androidx.compose.material3.Icon(
                modifier = Modifier.size(20.dp),
                painter = androidx.compose.ui.res.painterResource(icon),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
    }
    if (compact) {
        // em pé: ícone à esquerda, texto à direita
        Row(
            modifier = base.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            badge()
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, color = CryoGlassColors.Frost.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    } else {
        Column(
            modifier = base.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            badge()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = CryoGlassColors.Frost.copy(alpha = 0.72f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Base: contador da etapa, barra de progresso de gelo e os botões. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WelcomeDock(
    modifier: Modifier,
    pager: androidx.compose.foundation.pager.PagerState,
    time: State<Float>,
    canStart: Boolean,
    started: Boolean,
    onStart: () -> Unit,
    goTo: (Int) -> Unit,
    compact: Boolean
) {
    val ice = CryoGlassColors.Ice
    val page = pager.currentPage
    val last = page >= WELCOME_STEPS - 1
    Row(
        modifier = modifier.padding(start = if (compact) 18.dp else 22.dp, end = 16.dp, bottom = 14.dp, top = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "0${page + 1}",
            style = TextStyle(
                brush = Brush.linearGradient(listOf(CryoGlassColors.Frost, ice)),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        )
        if (!compact) Text(
            text = " / 0$WELCOME_STEPS",
            color = CryoGlassColors.Frost.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(if (compact) 10.dp else 14.dp))
        // barra de progresso: cada etapa é um segmento, o atual se estica
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(WELCOME_STEPS) { i ->
                val width by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (i == page) (if (compact) 26.dp else 38.dp) else (if (compact) 8.dp else 12.dp),
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    label = "dock_seg"
                )
                Box(
                    modifier = Modifier
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .clickable { goTo(i) }
                        .background(
                            if (i <= page) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, ice, CryoGlassColors.Aurora))
                            else Brush.horizontalGradient(listOf(ice.copy(alpha = 0.22f), ice.copy(alpha = 0.22f)))
                        )
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (page > 0) {
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { goTo(page - 1) }
                    .border(1.dp, ice.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = if (compact) 14.dp else 16.dp, vertical = 12.dp),
                text = if (compact) "‹" else "‹  " + stringResource(R.string.welcome_screen_back),
                color = CryoGlassColors.Frost,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        WelcomeStartButton(
            modifier = Modifier.widthIn(min = if (compact) 130.dp else 170.dp),
            time = time,
            enabled = if (last) canStart && !started else true,
            label = stringResource(if (last) R.string.welcome_screen_start else R.string.welcome_screen_next),
            onClick = { if (last) onStart() else goTo(page + 1) }
        )
    }
}

/** Borda de gelo com um brilho que gira em volta do cartão. */
private fun Modifier.frostBorder(time: State<Float>, radius: Float, ice: Color, aurora: Color): Modifier =
    this.drawWithCache {
        val r = CornerRadius(radius * density, radius * density)
        val stroke = 1.5f * density
        onDrawWithContent {
            drawContent()
            val a = time.value * 0.9f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val reach = maxOf(size.width, size.height) * 0.6f
            val brush = Brush.linearGradient(
                colors = listOf(ice.copy(alpha = 0.18f), aurora, ice, ice.copy(alpha = 0.18f)),
                start = Offset(cx + cos(a) * reach, cy + sin(a) * reach),
                end = Offset(cx - cos(a) * reach, cy - sin(a) * reach)
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = r,
                style = Stroke(width = stroke)
            )
        }
    }


/** Botão principal (Próximo / Começar): degradê do tema, brilho que passa e seta que chama. */
@Composable
private fun WelcomeStartButton(
    modifier: Modifier,
    time: State<Float>,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    val iceSoft = CryoGlassColors.IceSoft
    val aurora = CryoGlassColors.Aurora
    val deep = CryoGlassColors.Deep
    val shape = CircleShape
    Box(
        modifier = modifier
            .height(54.dp)
            .drawBehind {
                if (!enabled) return@drawBehind
                val p = 0.5f + 0.5f * sin(time.value * 2.4f)
                val grow = (3f + 4f * p) * density
                drawRoundRect(
                    color = ice,
                    alpha = 0.10f + 0.16f * p,
                    topLeft = Offset(-grow, -grow),
                    size = Size(size.width + grow * 2f, size.height + grow * 2f),
                    cornerRadius = CornerRadius(size.height / 2f + grow)
                )
            }
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(iceSoft, ice, aurora)))
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), shape)
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    if (enabled) {
                        // faixa de brilho atravessando o botão
                        val cycle = (time.value % 2.8f) / 2.8f
                        val x = -size.width * 0.3f + cycle * size.width * 1.6f
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                                start = Offset(x - size.height, 0f),
                                end = Offset(x + size.height, size.height)
                            )
                        )
                    }
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                color = deep,
                maxLines = 1,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                modifier = Modifier.graphicsLayer {
                    translationX = if (enabled) sin(time.value * 4f).coerceAtLeast(0f) * 5f * density else 0f
                },
                text = "→",
                color = deep,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun WelcomeTerms(
    availableAgreement: StateFlow<AvailableAgreement?>,
    agreementChecked: Boolean,
    onAgreementCheckedChange: (Boolean) -> Unit,
    onAgreementRequiredChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val agreement by availableAgreement.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var currentAgreement by remember(agreement) {
        mutableStateOf<AvailableAgreement.DocumentFile?>(null)
    }
    var documents by remember(agreement) {
        mutableStateOf<List<AvailableAgreement.DocumentFile.Data>>(emptyList())
    }
    var selectedDocument by remember(agreement) {
        mutableStateOf<AvailableAgreement.DocumentFile.Data?>(null)
    }

    LaunchedEffect(agreement) {
        val region = if (Locale.getDefault().country.equals("CN", ignoreCase = true)) "zh" else "global"
        val selectedAgreement = agreement?.files?.firstOrNull {
            it.effectiveRegion.equals(region, ignoreCase = true)
        } ?: agreement?.files?.firstOrNull {
            it.effectiveRegion.equals("global", ignoreCase = true)
        }
        currentAgreement = selectedAgreement

        val availableDocuments = withContext(Dispatchers.IO) {
            selectedAgreement?.dataList.orEmpty().filter {
                it.title.isNotBlank() && it.path.isNotBlank() && runCatching {
                    context.assets.open(it.path).close()
                }.isSuccess
            }
        }
        documents = availableDocuments
        onAgreementRequiredChange(availableDocuments.isNotEmpty())
        if (availableDocuments.isEmpty()) onAgreementCheckedChange(false)
    }

    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyMedium
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (documents.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_screen_agreement_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = CryoGlassColors.Ice,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        documents.forEach { document ->
                            InfoLayoutTextItem(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedDocument = document },
                                title = document.title,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreementChecked,
                            onCheckedChange = onAgreementCheckedChange,
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = CryoGlassColors.Ice,
                                uncheckedColor = CryoGlassColors.Ice.copy(alpha = 0.7f),
                                checkmarkColor = CryoGlassColors.Deep
                            )
                        )

                        Text(
                            text = stringResource(R.string.welcome_screen_agreement_confirmation),
                            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }

    selectedDocument?.let { document ->
        AgreementDocumentDialog(
            document = document,
            onDismissRequest = { selectedDocument = null }
        )
    }
}

