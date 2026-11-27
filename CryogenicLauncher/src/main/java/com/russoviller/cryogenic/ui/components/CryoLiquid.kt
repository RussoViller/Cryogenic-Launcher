package com.russoviller.cryogenic.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.BackgroundBlur
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interface 2.0 (Liquid Glass): estado compartilhado das animações do vidro.
 */
object CryoLiquid {
    val on: Boolean get() = AllSettings.cryoInterface2.state

    // ---- desfoque do fundo ----

    /** O desfoque do fundo só vale na interface padrão: no Liquid Glass ele fica desligado. */
    val blurType: BackgroundBlur
        get() = AllSettings.backgroundBlurType.state

    val blurRadius: Int
        get() = if (on) 0 else AllSettings.backgroundBlur.state
}

/** Aperto líquido: o elemento encolhe ao ser tocado e volta quicando. */
@Composable
fun Modifier.cryoLiquidPress(interaction: InteractionSource): Modifier {
    if (!CryoLiquid.on) return this
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "liquidPress"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Gota que marca o item selecionado e desliza de um item para o outro:
 * a ponta da frente sai rápido e a de trás vem depois, então ela estica e encolhe como líquido.
 */
class LiquidBlob(private val vertical: Boolean) {
    private val lead = Animatable(0f)
    private val trail = Animatable(0f)
    private var crossStart by mutableStateOf(0f)
    private var crossEnd by mutableStateOf(0f)
    private var ready by mutableStateOf(false)
    private var target by mutableStateOf<Pair<Float, Float>?>(null)

    /** Posição do item selecionado dentro do container. */
    fun report(position: Offset, width: Int, height: Int) {
        val main = if (vertical) position.y to position.y + height else position.x to position.x + width
        if (vertical) {
            crossStart = position.x
            crossEnd = position.x + width
        } else {
            crossStart = position.y
            crossEnd = position.y + height
        }
        if (target != main) target = main
    }

    @Composable
    fun Animate() {
        val t = target
        LaunchedEffect(t) {
            if (t == null) return@LaunchedEffect
            if (!ready) {
                lead.snapTo(t.first)
                trail.snapTo(t.second)
                ready = true
                return@LaunchedEffect
            }
            val forward = t.first > lead.value
            val fast = spring<Float>(dampingRatio = 0.72f, stiffness = 620f)
            val slow = spring<Float>(dampingRatio = 0.58f, stiffness = 210f)
            coroutineScope {
                launch { lead.animateTo(t.first, if (forward) slow else fast) }
                launch { trail.animateTo(t.second, if (forward) fast else slow) }
            }
        }
    }

    internal fun DrawScope.draw(colors: List<Color>, glow: Color) {
        if (!ready) return
        val a = minOf(lead.value, trail.value)
        val b = maxOf(lead.value, trail.value)
        val topLeft = if (vertical) Offset(crossStart, a) else Offset(a, crossStart)
        val size = if (vertical) Size(crossEnd - crossStart, b - a) else Size(b - a, crossEnd - crossStart)
        if (size.width <= 0f || size.height <= 0f) return
        val radius = CornerRadius(minOf(size.width, size.height) / 2f)
        //brilho em volta
        for (i in 1..4) {
            val grow = i * 2.dp.toPx()
            drawRoundRect(
                color = glow.copy(alpha = 0.16f / i),
                topLeft = Offset(topLeft.x - grow, topLeft.y - grow),
                size = Size(size.width + grow * 2, size.height + grow * 2),
                cornerRadius = CornerRadius(radius.x + grow),
                style = Stroke(2.dp.toPx())
            )
        }
        drawRoundRect(
            brush = Brush.linearGradient(colors, start = topLeft, end = Offset(topLeft.x + size.width, topLeft.y + size.height * 0.4f)),
            topLeft = topLeft,
            size = size,
            cornerRadius = radius
        )
        //brilho de vidro em cima + aro claro
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f), Color.Transparent),
                startY = topLeft.y,
                endY = topLeft.y + size.height
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = radius
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.75f),
            topLeft = topLeft,
            size = size,
            cornerRadius = radius,
            style = Stroke(1.5.dp.toPx())
        )
    }
}

/** Desenha a gota atrás dos itens (colocar depois do padding do container). */
fun Modifier.liquidBlob(blob: LiquidBlob, colors: List<Color>, glow: Color): Modifier =
    this.drawBehind { with(blob) { draw(colors, glow) } }

/** No item selecionado: informa a posição dele para a gota. */
fun Modifier.liquidBlobTarget(blob: LiquidBlob?, selected: Boolean): Modifier =
    if (blob == null || !selected) this
    else this.onGloballyPositioned { c -> blob.report(c.positionInParent(), c.size.width, c.size.height) }
