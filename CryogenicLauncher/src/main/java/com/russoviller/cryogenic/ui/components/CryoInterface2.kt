package com.russoviller.cryogenic.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Tempo que o botão precisa ficar pressionado. */
private const val HOLD_MS = 3500

/* Roteiro da cutscene, em segundos (total: 11 s). */
private const val T_DARK = 2.5f      // a tela termina de escurecer
private const val T_STAR = 2.5f      // a estrela nasce
private const val T_CHARGE = 5.5f    // a estrela começa a carregar
private const val T_BOOM = 7.2f      // explosão: a interface troca aqui, escondida pelo clarão
private const val T_END = 11f

/** Estado do Liquid Glass e da cutscene de entrada. */
object CryoInterface2 {
    /** Cutscene tocando agora. */
    var playing by mutableStateOf(false)
        private set

    val enabled: Boolean get() = AllSettings.cryoInterface2.state

    /** Fator do som dos wallpapers (0..1): abaixa junto com a tela escurecendo e volta no fim. */
    val audioScale = mutableFloatStateOf(1f)

    /** Tempo da cutscene em segundos (lido na hora de desenhar, sem recompor). */
    val time = mutableFloatStateOf(0f)

    /**
     * Raio da abertura depois da explosão, em fração do maior lado da tela: a interface nova
     * aparece por um círculo que cresce do centro (e a onda de vidro corre na borda dele).
     */
    fun revealFraction(): Float {
        if (!playing) return 0f
        val p = time.floatValue - T_BOOM - 0.35f
        if (p <= 0f) return 0f
        val k = (p / 2.3f).coerceIn(0f, 1f)
        val e = 1f - (1f - k) * (1f - k) * (1f - k)
        return 1.15f * e
    }

    /** Força da onda de vidro sobre a interface (0 quando parada). */
    fun waveAmplitude(): Float {
        if (!playing) return 0f
        val p = time.floatValue - T_BOOM - 0.35f
        if (p <= 0f || p >= 2.5f) return 0f
        val inK = (p / 0.25f).coerceIn(0f, 1f)
        return inK * (1f - p / 2.5f)
    }

    /** Liga (com cutscene, se não estiver ignorada) ou desliga o Liquid Glass. */
    fun toggle() {
        if (playing) return
        when {
            //voltar para a interface padrão é direto, sem cutscene
            AllSettings.cryoInterface2.getValue() -> AllSettings.cryoInterface2.save(false)
            AllSettings.cryoLiquidSkipCutscene.getValue() -> AllSettings.cryoInterface2.save(true)
            else -> playing = true
        }
    }

    internal fun finish() {
        if (!AllSettings.cryoInterface2.getValue()) AllSettings.cryoInterface2.save(true)
        audioScale.floatValue = 1f
        time.floatValue = 0f
        playing = false
    }
}

/**
 * Botão "Liquid Glass" da barra do topo.
 * Para ligar: segurar por 3,5 segundos (a pílula enche de líquido), ou um toque com
 * "Ignorar tempo de pressionar" ligado. Para voltar ao padrão: um toque.
 */
@Composable
fun Interface2Button(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val wave = rememberCryoClock(1400, enabled = progress.value > 0f)
    val skipHold = AllSettings.cryoLiquidSkipHold.state
    val hint = stringResource(R.string.cryo_ui2_hold_hint)
    val offText = stringResource(R.string.cryo_ui2_off_toast)
    val on = CryoInterface2.enabled
    Row(
        modifier = modifier
            .height(38.dp)
            .then(if (on) Modifier.cryoGlow(19.dp, selected = true, strength = 0.8f) else Modifier)
            .cryoGlass(shape = cryoGlassShape(19.dp), glow = false, cornerRadius = 19.dp, selected = on)
            .drawWithContent {
                //enchimento do "segurar": líquido com a frente ondulando
                val p = progress.value
                if (p > 0f) {
                    val front = size.width * p
                    val amp = 3.dp.toPx() * (1f - p * 0.5f)
                    val phase = wave.value * 6.2832f
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(front, 0f)
                        val steps = 12
                        for (i in 0..steps) {
                            val y = size.height * i / steps
                            lineTo(front + sin(phase + i * 0.9f) * amp, y)
                        }
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, brush = Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.18f), Color.White.copy(alpha = 0.36f))))
                }
                drawContent()
            }
            .pointerInput(on, skipHold) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (on) {
                        //ligado: um toque volta para a interface padrão
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            CryoInterface2.toggle()
                            Toast.makeText(context, offText, Toast.LENGTH_SHORT).show()
                        }
                        return@awaitEachGesture
                    }
                    if (skipHold) {
                        //sem tempo de pressionar: um toque liga
                        if (waitForUpOrCancellation() != null) {
                            hapticConfirm(view)
                            CryoInterface2.toggle()
                        }
                        return@awaitEachGesture
                    }
                    var completed = false
                    val job: Job = scope.launch {
                        progress.snapTo(0f)
                        var tick = 1
                        progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing)) {
                            //pulsos leves a cada quarto do caminho
                            if (value >= tick * 0.25f && tick < 4) {
                                tick++
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                        completed = true
                        hapticConfirm(view)
                        CryoInterface2.toggle()
                        progress.snapTo(0f)
                    }
                    waitForUpOrCancellation()
                    if (!completed) {
                        val short = progress.value < 0.12f
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(220)) }
                        if (short) Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(R.drawable.ic_star_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
        Text(
            text = "Liquid Glass",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

private fun hapticConfirm(view: android.view.View) {
    val type = if (android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM
    else android.view.HapticFeedbackConstants.LONG_PRESS
    view.performHapticFeedback(type)
}

private fun smooth(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun easeOut(x: Float): Float {
    val t = 1f - x.coerceIn(0f, 1f)
    return 1f - t * t * t
}

private class Spark(val angle: Float, val speed: Float, val size: Float, val tint: Int, val drag: Float, val twinkle: Float)
private class Dot(val x: Float, val y: Float, val size: Float, val phase: Float)
private class Burst(val x: Float, val y: Float, val at: Float, val tint: Int, val sparks: List<Spark>)
private class Shard(val angle: Float, val speed: Float, val size: Float, val spin: Float, val tint: Int)
private class Ember(val x: Float, val delay: Float, val speed: Float, val sway: Float, val size: Float, val phase: Float)

/** Sons da cutscene (gerados para o Cryogenic). SoundPool: toca sem travar a animação. */
private class CutsceneSounds(context: android.content.Context) {
    private val pool = android.media.SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val loaded = java.util.Collections.synchronizedSet(HashSet<Int>())
    private val appear: Int
    private val charge: Int
    private val boom: Int

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        appear = pool.load(context, R.raw.cryo_star_appear, 1)
        charge = pool.load(context, R.raw.cryo_star_charge, 1)
        boom = pool.load(context, R.raw.cryo_star_boom, 1)
    }

    private fun play(id: Int, volume: Float) {
        if (id in loaded) pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun appear() = play(appear, 0.8f)
    fun charge() = play(charge, 0.85f)
    fun boom() = play(boom, 1f)
    fun release() = runCatching { pool.release() }
}

/**
 * Cutscene de entrada do Liquid Glass, por cima de qualquer tela:
 * a tela escurece, nasce uma estrela (som de brilho), ela carrega (som subindo) e explode
 * (estrondo, ondas, raios de luz, faíscas, estilhaços, fogos secundários e brasas caindo).
 * O clarão esconde a troca para o Liquid Glass e depois some mostrando a interface nova. Dura 11 segundos.
 */
@Composable
fun Interface2Cutscene(modifier: Modifier = Modifier) {
    if (!CryoInterface2.playing) return
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val time = CryoInterface2.time
    val palette = listOf(Color.White, CryoGlassColors.Ice, CryoGlassColors.Aurora, Color(0xFFFF8AD8), Color(0xFFFFE08A))
    val sparks = remember {
        val r = Random(2026)
        List(260) {
            Spark(
                angle = r.nextFloat() * 6.2832f,
                speed = 0.25f + r.nextFloat() * 1.05f,
                size = 1.0f + r.nextFloat() * 2.8f,
                tint = r.nextInt(5),
                drag = 1.4f + r.nextFloat() * 2.2f,
                twinkle = r.nextFloat() * 6.2832f
            )
        }
    }
    val bursts = remember {
        val r = Random(91)
        List(7) { i ->
            Burst(
                x = 0.12f + r.nextFloat() * 0.76f,
                y = 0.12f + r.nextFloat() * 0.66f,
                at = 0.35f + i * 0.17f + r.nextFloat() * 0.1f,
                tint = 1 + r.nextInt(4),
                sparks = List(34) { Spark(it / 34f * 6.2832f + r.nextFloat() * 0.2f, 0.6f + r.nextFloat() * 0.5f, 1.2f + r.nextFloat() * 1.4f, 0, 2.6f, r.nextFloat() * 6.2832f) }
            )
        }
    }
    val shards = remember {
        val r = Random(33)
        List(46) { Shard(r.nextFloat() * 6.2832f, 0.3f + r.nextFloat() * 0.8f, 5f + r.nextFloat() * 9f, (r.nextFloat() - 0.5f) * 900f, r.nextInt(3)) }
    }
    val embers = remember {
        val r = Random(12)
        List(130) { Ember(r.nextFloat(), r.nextFloat() * 1.6f, 0.12f + r.nextFloat() * 0.22f, 8f + r.nextFloat() * 22f, 0.8f + r.nextFloat() * 2f, r.nextFloat() * 6.2832f) }
    }
    val dots = remember {
        val r = Random(57)
        List(130) { Dot(r.nextFloat(), r.nextFloat(), 0.8f + r.nextFloat() * 2.2f, r.nextFloat() * 6.2832f) }
    }
    //ninguém toca nem volta durante a cutscene
    BackHandler(enabled = true) {}
    val sounds = remember { runCatching { CutsceneSounds(context.applicationContext) }.getOrNull() }
    DisposableEffect(Unit) {
        //saiu da tela no meio (app fechado, tela trocada): a interface nova entra mesmo assim
        onDispose {
            sounds?.release()
            if (CryoInterface2.playing) CryoInterface2.finish()
        }
    }
    LaunchedEffect(Unit) {
        var start = -1L
        var switched = false
        var playedAppear = false
        var playedCharge = false
        var playedBoom = false
        while (true) {
            val now = withFrameNanos { it }
            if (start < 0) start = now
            val t = (now - start) / 1_000_000_000f
            time.floatValue = t
            //som do fundo: abaixa no mesmo ritmo da tela escurecendo, fica mudo na estrela e na
            //explosão e volta devagar enquanto a interface nova aparece
            val pb = t - T_BOOM
            CryoInterface2.audioScale.floatValue = when {
                t < T_DARK -> 1f - smooth(t / T_DARK)
                pb < 1.2f -> 0f
                else -> smooth((pb - 1.2f) / (T_END - T_BOOM - 1.2f))
            }
            if (!playedAppear && t >= T_STAR) {
                playedAppear = true
                sounds?.appear()
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            }
            //o som da carga termina exatamente na explosão
            if (!playedCharge && t >= T_BOOM - 1.8f) { playedCharge = true; sounds?.charge() }
            if (!playedBoom && t >= T_BOOM) {
                playedBoom = true
                sounds?.boom()
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            if (!switched && t >= T_BOOM + 0.2f) {
                switched = true
                AllSettings.cryoInterface2.save(true)
            }
            if (t >= T_END) break
        }
        CryoInterface2.finish()
    }

    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time.floatValue
            val p = t - T_BOOM                       // tempo desde a explosão
            val unit = size.minDimension
            val reach = size.maxDimension * 0.75f
            //tremor da tela na explosão
            val quake = if (p in 0f..0.9f) (1f - p / 0.9f) * unit * 0.018f else 0f
            val center = Offset(size.width / 2f + sin(t * 97f) * quake, size.height / 2f + cos(t * 83f) * quake)

            // 1) a tela escurece; depois da explosão a interface nova aparece por um círculo de
            //    vidro que cresce do centro (a onda entorta a interface na borda dele)
            val night = Color(0xFF02040A)
            if (p < 0.35f) {
                drawRect(color = night.copy(alpha = smooth(t / T_DARK).coerceIn(0f, 1f)))
            } else {
                val fade = 1f - smooth((p - 2.2f) / 0.6f)
                if (fade > 0f) {
                    val hole = CryoInterface2.revealFraction() * size.maxDimension
                    val feather = unit * 0.22f
                    val outer = hole + feather
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to Color.Transparent,
                            (hole / outer).coerceIn(0f, 0.999f) to Color.Transparent,
                            1f to night.copy(alpha = fade),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = outer.coerceAtLeast(1f)
                        )
                    )
                    //aro de luz na borda da abertura
                    if (hole > 0f) drawCircle(
                        brush = Brush.radialGradient(
                            0f to Color.Transparent,
                            ((hole - feather * 0.25f) / outer).coerceIn(0f, 0.999f) to Color.Transparent,
                            (hole / outer).coerceIn(0f, 0.999f) to ice.copy(alpha = 0.55f * fade),
                            1f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = outer.coerceAtLeast(1f)
                        ),
                        radius = outer.coerceAtLeast(1f),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }

            // 2) poeira de estrelas no fundo; é puxada para a estrela enquanto ela carrega
            val charge = ((t - T_CHARGE) / (T_BOOM - T_CHARGE)).coerceIn(0f, 1f)
            val dustAlpha = smooth((t - 1.4f) / 1.6f) * (1f - smooth(p / 0.3f))
            if (dustAlpha > 0f) {
                val pull = charge * charge
                for (d in dots) {
                    val x = d.x * size.width + (center.x - d.x * size.width) * pull
                    val y = d.y * size.height + (center.y - d.y * size.height) * pull
                    val twinkle = 0.45f + 0.55f * sin(t * 2.4f + d.phase)
                    val a = (dustAlpha * twinkle).coerceIn(0f, 1f)
                    val rr = d.size.dp.toPx() * (1f - pull * 0.5f)
                    //gotinhas de vidro: corpo gelado translúcido, aro claro e um ponto de brilho
                    drawCircle(color = ice.copy(alpha = a * 0.35f), radius = rr, center = Offset(x, y))
                    drawCircle(color = Color.White.copy(alpha = a * 0.7f), radius = rr, center = Offset(x, y), style = Stroke(rr * 0.25f))
                    drawCircle(color = Color.White.copy(alpha = a), radius = rr * 0.32f, center = Offset(x - rr * 0.35f, y - rr * 0.35f))
                }
            }

            // 3) a estrela (com anéis de energia girando enquanto carrega)
            if (t >= T_STAR && p < 0.12f) {
                val born = easeOut((t - T_STAR) / 1.6f)
                val pulse = 1f + 0.07f * sin(t * 5f) + charge * 0.12f * sin(t * 38f)
                val shake = charge * charge * unit * 0.008f
                val c = Offset(center.x + sin(t * 91f) * shake, center.y + cos(t * 77f) * shake)
                val r = unit * (0.028f * born + 0.03f * charge) * pulse
                if (charge > 0f) {
                    for (i in 0 until 3) {
                        val rr = r * (6f - i * 1.4f) * (1.2f - charge * 0.5f)
                        rotate(degrees = t * (120f + i * 70f) * (if (i % 2 == 0) 1f else -1f), pivot = c) {
                            drawArc(
                                color = (if (i == 1) aurora else ice).copy(alpha = 0.55f * charge),
                                startAngle = 0f,
                                sweepAngle = 110f,
                                useCenter = false,
                                topLeft = Offset(c.x - rr, c.y - rr),
                                size = Size(rr * 2, rr * 2),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                drawStar(c, r, t, charge, ice, aurora)
            }

            // 4) explosão
            if (p >= 0f) {
                // núcleo de plasma que se expande
                val core = (p / 0.9f).coerceIn(0f, 1f)
                if (core < 1f) {
                    val cr = reach * 0.55f * easeOut(core)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 1f - core), ice.copy(alpha = 0.7f * (1f - core)), aurora.copy(alpha = 0.35f * (1f - core)), Color.Transparent),
                            center = center,
                            radius = cr.coerceAtLeast(1f)
                        ),
                        radius = cr.coerceAtLeast(1f),
                        center = center
                    )
                }
                // raios de luz girando
                val rays = (p / 1.8f).coerceIn(0f, 1f)
                if (rays < 1f) {
                    val a = (1f - rays) * 0.5f
                    rotate(degrees = p * 40f, pivot = center) {
                        for (i in 0 until 18) {
                            val ang = i * 20f * (3.14159f / 180f)
                            val len = reach * (0.6f + 0.4f * ((i * 7) % 5) / 5f) * easeOut(rays * 2f)
                            val w = (if (i % 2 == 0) 0.07f else 0.035f)
                            val path = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x + cos(ang - w) * len, center.y + sin(ang - w) * len)
                                lineTo(center.x + cos(ang + w) * len, center.y + sin(ang + w) * len)
                                close()
                            }
                            drawPath(
                                path,
                                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = a), (if (i % 3 == 0) aurora else ice).copy(alpha = a * 0.5f), Color.Transparent), center = center, radius = len.coerceAtLeast(1f))
                            )
                        }
                    }
                }
                // ondas de choque (5, cores alternadas)
                for (i in 0 until 5) {
                    val k = ((p - i * 0.13f) / (1.5f + i * 0.15f)).coerceIn(0f, 1f)
                    if (k > 0f && k < 1f) {
                        val color = when (i % 3) { 0 -> ice; 1 -> aurora; else -> Color(0xFFFF8AD8) }
                        drawCircle(
                            color = color.copy(alpha = (1f - k) * 0.85f),
                            radius = reach * easeOut(k) * (1f - i * 0.06f),
                            center = center,
                            style = Stroke(width = (12f - i * 2f).dp.toPx() * (1f - k * 0.7f))
                        )
                    }
                }
                // reflexo de lente horizontal
                val flare = (p / 1.2f).coerceIn(0f, 1f)
                if (flare < 1f) {
                    val fw = size.width * (0.3f + 0.9f * easeOut(flare))
                    val fh = 6.dp.toPx() * (1f - flare)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, ice.copy(alpha = 0.8f * (1f - flare)), Color.White.copy(alpha = 1f - flare), ice.copy(alpha = 0.8f * (1f - flare)), Color.Transparent),
                            startX = center.x - fw / 2f,
                            endX = center.x + fw / 2f
                        ),
                        topLeft = Offset(center.x - fw / 2f, center.y - fh / 2f),
                        size = Size(fw, fh.coerceAtLeast(0.5f))
                    )
                }
                // faíscas principais: saem rápido, freiam no ar e caem com gravidade
                val pk = (p / 3.4f).coerceIn(0f, 1f)
                if (pk < 1f) {
                    val gravity = unit * 0.16f
                    for (s in sparks) {
                        val dist = reach * s.speed * (1f - kotlin.math.exp(-s.drag * p)) / s.drag * 2.2f
                        val dir = Offset(cos(s.angle), sin(s.angle))
                        val fall = gravity * p * p * 0.5f
                        val pos = Offset(center.x + dir.x * dist, center.y + dir.y * dist + fall)
                        val vel = kotlin.math.exp(-s.drag * p)
                        val tail = Offset(pos.x - dir.x * reach * 0.06f * vel * s.speed, pos.y - dir.y * reach * 0.06f * vel * s.speed - gravity * p * 0.03f)
                        val blink = 0.55f + 0.45f * sin(p * 18f + s.twinkle)
                        val a = ((1f - pk) * (if (p > 1f) blink else 1f)).coerceIn(0f, 1f)
                        val color = palette[s.tint].copy(alpha = a)
                        drawLine(color = color, start = tail, end = pos, strokeWidth = s.size.dp.toPx() * 0.7f, cap = StrokeCap.Round)
                        drawCircle(color = color, radius = s.size.dp.toPx() * (1f - pk * 0.6f), center = pos)
                    }
                }
                // estilhaços de vidro girando
                val sk = (p / 2.4f).coerceIn(0f, 1f)
                if (sk < 1f) {
                    for (sh in shards) {
                        val dist = reach * sh.speed * easeOut(sk) * 0.9f
                        val pos = Offset(center.x + cos(sh.angle) * dist, center.y + sin(sh.angle) * dist + unit * 0.1f * sk * sk)
                        val sz = sh.size.dp.toPx()
                        rotate(degrees = sh.spin * p, pivot = pos) {
                            val path = Path().apply {
                                moveTo(pos.x, pos.y - sz)
                                lineTo(pos.x + sz * 0.55f, pos.y + sz * 0.6f)
                                lineTo(pos.x - sz * 0.5f, pos.y + sz * 0.35f)
                                close()
                            }
                            drawPath(path, color = palette[sh.tint].copy(alpha = 0.55f * (1f - sk)))
                            drawPath(path, color = Color.White.copy(alpha = 0.9f * (1f - sk)), style = Stroke(1.dp.toPx()))
                        }
                    }
                }
                // fogos secundários espalhados pela tela
                for (b in bursts) {
                    val bp = p - b.at
                    if (bp < 0f || bp > 1.4f) continue
                    val bk = bp / 1.4f
                    val bc = Offset(b.x * size.width, b.y * size.height)
                    val br = unit * 0.16f
                    if (bp < 0.25f) {
                        drawCircle(color = Color.White.copy(alpha = 1f - bp / 0.25f), radius = br * 0.3f * (bp / 0.25f), center = bc)
                    }
                    drawCircle(color = palette[b.tint].copy(alpha = (1f - bk) * 0.7f), radius = br * easeOut(bk), center = bc, style = Stroke(2.dp.toPx() * (1f - bk)))
                    for (s in b.sparks) {
                        val d = br * s.speed * (1f - kotlin.math.exp(-s.drag * bp)) / s.drag * 3f
                        val pos = Offset(bc.x + cos(s.angle) * d, bc.y + sin(s.angle) * d + unit * 0.05f * bp * bp)
                        drawCircle(color = palette[b.tint].copy(alpha = (1f - bk).coerceIn(0f, 1f)), radius = s.size.dp.toPx() * (1f - bk * 0.5f), center = pos)
                    }
                }
                // brasas brilhantes caindo devagar enquanto a interface nova aparece
                for (e in embers) {
                    val ep = p - 0.6f - e.delay
                    if (ep < 0f || ep > 2.6f) continue
                    val ek = ep / 2.6f
                    val x = e.x * size.width + sin(ep * 2.2f + e.phase) * e.sway.dp.toPx()
                    val y = size.height * (-0.05f + e.speed * ep * 1.6f) + (e.phase * 30f)
                    val a = (sin(ek * 3.14159f) * (0.6f + 0.4f * sin(ep * 9f + e.phase))).coerceIn(0f, 1f)
                    drawCircle(color = palette[(e.phase * 10).toInt() % 5].copy(alpha = a), radius = e.size.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = Color.White.copy(alpha = a * 0.25f), radius = e.size.dp.toPx() * 3f, center = Offset(x, y))
                }
                // clarão branco: sobe em 0,12 s, segura (a interface troca por baixo) e some
                //clarão curto: as faíscas aparecem logo depois, sobre o fundo ainda escuro
                val flash = when {
                    p < 0.12f -> p / 0.12f
                    p < 0.4f -> 1f
                    else -> 1f - smooth((p - 0.4f) / 0.75f)
                }
                if (flash > 0f) drawRect(color = Color.White.copy(alpha = (flash * 0.96f).coerceIn(0f, 1f)))
            }
        }

        // 5) título
        Column(
            modifier = Modifier.graphicsLayer {
                val p = time.floatValue - T_BOOM
                alpha = (smooth((p - 0.9f) / 0.7f) * (1f - smooth((p - 3.0f) / 0.7f))).coerceIn(0f, 1f)
                val s = 0.92f + 0.08f * easeOut((p - 0.9f) / 1.4f)
                scaleX = s
                scaleY = s
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 24f)
            Text(
                text = "LIQUID GLASS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    shadow = shadow
                ),
                color = Color.White
            )
            Text(
                text = "CRYOGENIC LAUNCHER",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    shadow = shadow
                ),
                color = ice
            )
        }
    }
}

/**
 * Estrela: núcleo branco com halo, 4 pontas principais com degradê (branco → gelo → transparente),
 * 4 pontas finas na diagonal, raios de difração longos e um anel de brilho. Cresce enquanto carrega.
 */
private fun DrawScope.drawStar(center: Offset, r: Float, t: Float, charge: Float, ice: Color, aurora: Color) {
    if (r <= 0f) return
    //halo grande e macio
    val glow = r * (8f + 10f * charge)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.45f + 0.35f * charge), ice.copy(alpha = 0.30f), aurora.copy(alpha = 0.14f), Color.Transparent),
            center = center,
            radius = glow
        ),
        radius = glow,
        center = center
    )
    //anel de brilho fino em volta do núcleo
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Transparent, ice.copy(alpha = 0.55f), Color.Transparent),
            center = center,
            radius = r * 2.6f
        ),
        radius = r * 2.6f,
        center = center,
        style = Stroke(width = r * 0.35f)
    )
    //raios de difração: linhas bem finas e longas, piscando de leve
    val flicker = 0.75f + 0.25f * sin(t * 9f)
    for (k in 0 until 4) {
        val a = (k * 90f + 45f + t * 6f) * (3.14159f / 180f)
        val len = r * (9f + 9f * charge) * flicker
        val dir = Offset(cos(a), sin(a))
        drawLine(
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), start = center, end = center + dir * len),
            start = center,
            end = center + dir * len,
            strokeWidth = r * 0.07f + 0.6f
        )
        drawLine(
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), start = center, end = center - dir * len),
            start = center,
            end = center - dir * len,
            strokeWidth = r * 0.07f + 0.6f
        )
    }
    /** Estrela fina de 4 pontas (curvas cúbicas com controles perto do centro: braços finos e pontudos). */
    fun sparkle(lv: Float, lh: Float): Path = Path().apply {
        val cv = lv * 0.09f
        val ch = lh * 0.09f
        moveTo(center.x, center.y - lv)
        cubicTo(center.x, center.y - cv, center.x + ch, center.y, center.x + lh, center.y)
        cubicTo(center.x + ch, center.y, center.x, center.y + cv, center.x, center.y + lv)
        cubicTo(center.x, center.y + cv, center.x - ch, center.y, center.x - lh, center.y)
        cubicTo(center.x - ch, center.y, center.x, center.y - cv, center.x, center.y - lv)
        close()
    }
    val spin = t * 10f
    //pontas finas na diagonal
    rotate(degrees = 45f + spin, pivot = center) {
        val l = r * (2.6f + 2.4f * charge)
        drawPath(
            sparkle(l, l),
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.85f), ice.copy(alpha = 0.7f), Color.Transparent),
                center = center,
                radius = l
            )
        )
    }
    //pontas principais (vertical um pouco mais longa)
    rotate(degrees = spin, pivot = center) {
        val lv = r * (4.6f + 5.4f * charge)
        val lh = r * (3.8f + 4.2f * charge)
        drawPath(
            sparkle(lv, lh),
            brush = Brush.radialGradient(
                0f to Color.White,
                0.3f to Color.White,
                0.6f to ice,
                0.88f to aurora.copy(alpha = 0.55f),
                1f to Color.Transparent,
                center = center,
                radius = lv
            )
        )
    }
    //núcleo
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, Color.White, ice.copy(alpha = 0.6f), Color.Transparent), center = center, radius = r * 1.3f),
        radius = r * 1.3f,
        center = center
    )
}
