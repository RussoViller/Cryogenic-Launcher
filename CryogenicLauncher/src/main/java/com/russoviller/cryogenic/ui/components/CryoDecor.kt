package com.russoviller.cryogenic.ui.components

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.russoviller.cryogenic.setting.enums.isLauncherInDarkTheme
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private class Flake(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val phase: Float,
    val speed: Float
)

@Composable
fun CryoBackdrop(
    modifier: Modifier = Modifier,
    flakeCount: Int = 64,
    drawBase: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isLauncherInDarkTheme()
    val flakes = remember(flakeCount) {
        val random = Random(0x57197F)
        List(flakeCount) {
            Flake(
                x = random.nextFloat(),
                y = random.nextFloat(),
                size = 1f + random.nextInt(3),
                alpha = 0.25f + random.nextFloat() * 0.55f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.35f + random.nextFloat() * 0.65f
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "cryo_backdrop")
    val time = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 90_000, easing = LinearEasing)),
        label = "cryo_time"
    )

    val baseTop = scheme.surfaceContainerLowest
    val baseMid = scheme.surfaceContainer
    val baseBottom = scheme.surfaceContainerHigh
    val glowA = scheme.primary.copy(alpha = if (dark) 0.16f else 0.20f)
    val glowB = scheme.tertiary.copy(alpha = if (dark) 0.12f else 0.16f)
    val flakeColor = if (dark) Color.White else scheme.primary
    val frostLine = scheme.primary.copy(alpha = if (dark) 0.10f else 0.14f)

    Canvas(modifier = modifier) {
        val t = time.value
        if (drawBase) {
            drawRect(Brush.verticalGradient(listOf(baseTop, baseMid, baseBottom)))
        }
        drawAurora(t, glowA, glowB)
        drawFrostFloor(frostLine)

        val cell = 2.dp.toPx()
        flakes.forEach { flake ->
            val travel = (flake.y + t * 14f * flake.speed) % 1f
            val drift = sin(t * 2f * PI.toFloat() * 9f + flake.phase) * 0.012f
            val px = floor(((flake.x + drift + 1f) % 1f) * size.width / cell) * cell
            val py = floor(travel * size.height / cell) * cell
            val side = flake.size * cell
            val alpha = flake.alpha * (if (dark) 1f else 0.55f)
            drawRect(
                color = flakeColor.copy(alpha = alpha),
                topLeft = Offset(px, py),
                size = Size(side, side)
            )
            if (flake.size >= 3f) {
                drawRect(
                    color = flakeColor.copy(alpha = alpha * 0.45f),
                    topLeft = Offset(px - cell, py + cell),
                    size = Size(side + cell * 2, cell)
                )
                drawRect(
                    color = flakeColor.copy(alpha = alpha * 0.45f),
                    topLeft = Offset(px + cell, py - cell),
                    size = Size(cell, side + cell * 2)
                )
            }
        }
    }
}

private fun DrawScope.drawAurora(t: Float, glowA: Color, glowB: Color) {
    val wave = sin(t * 2f * PI.toFloat() * 3f)
    val centerA = Offset(size.width * (0.20f + 0.10f * wave), size.height * 0.05f)
    val centerB = Offset(size.width * (0.85f - 0.08f * wave), size.height * 0.25f)
    val radius = size.maxDimension * 0.55f
    drawCircle(
        brush = safeRadialGradient(listOf(glowA, Color.Transparent), center = centerA, radius = radius),
        radius = radius,
        center = centerA
    )
    drawCircle(
        brush = safeRadialGradient(listOf(glowB, Color.Transparent), center = centerB, radius = radius * 0.8f),
        radius = radius * 0.8f,
        center = centerB
    )
}

private fun DrawScope.drawFrostFloor(color: Color) {
    val height = size.height * 0.28f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color),
            startY = size.height - height,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height - height),
        size = Size(size.width, height)
    )
}

fun Modifier.cryoFrostEdge(color: Color, top: Boolean = false): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val y = if (top) 0f else size.height - stroke
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, color, color.copy(alpha = color.alpha * 0.4f), Color.Transparent)
        ),
        topLeft = Offset(0f, y),
        size = Size(size.width, stroke)
    )
}

@Composable
fun CryoLogo(modifier: Modifier = Modifier, floating: Boolean = false) {
    val bob = if (floating) {
        val transition = rememberInfiniteTransition(label = "cryo_logo")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
            label = "cryo_logo_bob"
        )
    } else null
    Image(
        modifier = modifier
            .graphicsLayer {
                bob?.let { translationY = sin(it.value * 2f * PI.toFloat()) * 4.dp.toPx() }
            }
            .clip(CircleShape),
        painter = BitmapPainter(ImageBitmap.imageResource(R.drawable.img_cryo_brand), filterQuality = FilterQuality.None),
        contentDescription = null
    )
}

/** Quantidade de quadros em [R.drawable.img_cryo_logo_frames] (folha 64x64 px por quadro, lado a lado). */
private const val CRYO_LOGO_FRAMES = 32
private const val CRYO_LOGO_FRAME_PX = 64
private const val CRYO_LOGO_FRAME_NANOS = 80_000_000L

/**
 * Logo animada do Cryogenic (versão em sprite sheet do GIF oficial: 32 quadros, 80 ms cada).
 * Desenhada com filtro "nearest" para manter a pixel art nítida em qualquer tamanho.
 * Pausa enquanto o jogo está em execução e quando o app não está desenhando quadros.
 */
@Composable
fun CryoAnimatedLogo(modifier: Modifier = Modifier) {
    val sheet = ImageBitmap.imageResource(R.drawable.img_cryo_logo_frames)
    val frames = remember(sheet) { (sheet.width / CRYO_LOGO_FRAME_PX).coerceIn(1, CRYO_LOGO_FRAMES) }
    val frame = remember { mutableIntStateOf(0) }
    LaunchedEffect(frames) {
        var start = -1L
        while (true) {
            val now = withFrameNanos { it }
            if (start < 0L) start = now
            if (!CryoTheme.inGame) {
                frame.intValue = (((now - start) / CRYO_LOGO_FRAME_NANOS) % frames).toInt()
            }
            delay(if (CryoTheme.inGame) 500L else 40L)
        }
    }
    Canvas(modifier = modifier.clip(CircleShape)) {
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        if (w <= 0 || h <= 0) return@Canvas
        drawImage(
            image = sheet,
            srcOffset = IntOffset(frame.intValue * CRYO_LOGO_FRAME_PX, 0),
            srcSize = IntSize(CRYO_LOGO_FRAME_PX, CRYO_LOGO_FRAME_PX),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
fun CryoAnimatedTitle(
    modifier: Modifier = Modifier,
    text: String = "CRYOGENIC",
    fontSize: Int = 17,
    letterSpacing: Float = 3.5f
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "cryo_title")
    val shimmer = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing)),
        label = "cryo_title_shimmer"
    )
    val wave = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2200, easing = LinearEasing)),
        label = "cryo_title_wave"
    )
    val base = scheme.primary
    val glint = Color.White
    val deep = scheme.tertiary
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val count = text.length
        text.forEachIndexed { index, char ->
            val pos = index.toFloat() / (count - 1).coerceAtLeast(1)
            //cor e altura lidas só na hora de desenhar: a animação não recompõe nem remede o texto
            androidx.compose.foundation.text.BasicText(
                modifier = Modifier.graphicsLayer {
                    translationY = sin((wave.value - pos * 0.6f) * 2f * PI.toFloat()) * 1.6.dp.toPx()
                },
                text = char.toString(),
                maxLines = 1,
                color = {
                    val d = kotlin.math.abs(shimmer.value - pos)
                    val k = (1f - d * 4f).coerceIn(0f, 1f)
                    lerp(if (index % 2 == 0) base else deep, glint, k)
                },
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = fontSize.sp,
                    letterSpacing = letterSpacing.sp,
                    lineHeight = fontSize.sp,
                    shadow = Shadow(color = base.copy(alpha = 0.55f), blurRadius = 12f)
                )
            )
        }
    }
}

@Composable
fun CryoBrandTitle(
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showIcon) {
            CryoLogo(modifier = Modifier.size(28.dp))
        }
        CryoAnimatedTitle()
    }
}

@Composable
fun CryoRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    alwaysShowLabel: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = modifier.then(
            if (selected) Modifier.drawBehind {
                val bar = 3.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, accent, Color.Transparent)),
                    topLeft = Offset(0f, size.height * 0.10f),
                    size = Size(bar, size.height * 0.80f)
                )
            } else Modifier
        ),
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = scheme.onPrimary,
            selectedTextColor = scheme.primary,
            indicatorColor = scheme.primary,
            unselectedIconColor = scheme.onSurfaceVariant,
            unselectedTextColor = scheme.onSurfaceVariant
        )
    )
}
