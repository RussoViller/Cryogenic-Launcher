package com.russoviller.cryogenic.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoLiquid
import com.russoviller.cryogenic.music.CryoNowPlaying
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.screens.game.elements.formatMusicTime
import com.russoviller.cryogenic.ui.screens.game.elements.sliderColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Esconde o player flutuante enquanto o menu do jogo ou um editor estiver aberto. */
object CryoMusicUi {
    var suppressed by mutableStateOf(false)
    /** O player flutuante pediu para abrir o menu de música (a tela do jogo atende e volta para false). */
    var openMenuPending by mutableStateOf(false)

    fun openMusicMenu() {
        com.russoviller.cryogenic.ui.components.XModePanelRequest.id = com.russoviller.cryogenic.ui.screens.game.elements.MUSIC_PANEL_ID
        openMenuPending = true
    }
}

private const val AUTO_HIDE_MS = 5000L
private val CARD_WIDTH = 292.dp
private val SQUARE_SIZE = 212.dp
private val DISC_SIZE = 224.dp

/** Largura da janela aberta em cada estilo (0 = padrão, 1 = quadrado, 2 = disco). */
private fun styleWidth(style: Int) = when (style) {
    1 -> SQUARE_SIZE
    2 -> DISC_SIZE
    else -> CARD_WIDTH
}
private val BUBBLE_SIZE = 54.dp
private val EDGE_MARGIN = 8.dp

/** Mola das janelas flutuantes: macia, desliza e para sem quicar demais. */
private fun <T> floatSpring() = spring<T>(dampingRatio = 0.8f, stiffness = 320f)

/**
 * Player de música flutuante (opcional), no estilo das janelas flutuantes do celular:
 * - Janela aberta: arrasta para qualquer lugar e dá para "jogar" com o dedo; ela desliza e para com mola.
 * - Jogar ou arrastar para a beirada vira uma bolinha encostada no lado da tela.
 * - Some sozinha (vira bolinha) depois de ~5 s sem toque.
 * - A bolinha também arrasta livre e gruda no lado mais perto; um toque abre de novo.
 */
@Composable
fun CryoMusicFloatingPlayer(modifier: Modifier = Modifier) {
    if (!AllSettings.cryoMusicFloating.state || CryoMusicUi.suppressed) return
    if (com.russoviller.cryogenic.streamer.StreamOverlayStore.editing || com.russoviller.cryogenic.streamer.StreamExtrasStore.editing != null) return
    val style = AllSettings.cryoMusicFloatingStyle.state

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()
        val bubble = with(density) { BUBBLE_SIZE.toPx() }
        val margin = with(density) { EDGE_MARGIN.toPx() }
        var cardW by remember(style) { mutableFloatStateOf(with(density) { styleWidth(style).toPx() }) }
        var cardH by remember(style) { mutableFloatStateOf(with(density) { (if (style == 0) 150.dp else styleWidth(style)).toPx() }) }

        fun clampCard(p: Offset) = Offset(
            p.x.coerceIn(margin, (areaW - cardW - margin).coerceAtLeast(margin)),
            p.y.coerceIn(margin, (areaH - cardH - margin).coerceAtLeast(margin))
        )
        fun bubbleX(right: Boolean) = if (right) areaW - bubble - margin else margin
        fun clampBubbleY(y: Float) = y.coerceIn(margin, (areaH - bubble - margin).coerceAtLeast(margin))

        val cardPos = remember {
            Animatable(
                clampCard(
                    Offset(
                        AllSettings.cryoMusicCardX.getValue() / 1000f * (areaW - cardW),
                        AllSettings.cryoMusicCardY.getValue() / 1000f * (areaH - cardH)
                    )
                ),
                Offset.VectorConverter
            )
        }
        val bubblePos = remember {
            Animatable(
                Offset(
                    bubbleX(AllSettings.cryoMusicBubbleRight.getValue()),
                    clampBubbleY(AllSettings.cryoMusicBubbleY.getValue() / 1000f * (areaH - bubble))
                ),
                Offset.VectorConverter
            )
        }
        //tela girou / mudou de tamanho: mantém tudo dentro
        LaunchedEffect(areaW, areaH) {
            cardPos.snapTo(clampCard(cardPos.value))
            bubblePos.snapTo(Offset(bubbleX(bubblePos.value.x + bubble / 2 > areaW / 2), clampBubbleY(bubblePos.value.y)))
        }

        var expanded by remember { mutableStateOf(false) }
        var touchTick by remember { mutableIntStateOf(0) }
        var cardDragging by remember { mutableStateOf(false) }
        var bubbleDragging by remember { mutableStateOf(false) }
        var showVolume by remember { mutableStateOf(false) }
        val morph = remember { Animatable(0f) }

        LaunchedEffect(expanded) {
            morph.animateTo(if (expanded) 1f else 0f, spring(dampingRatio = 0.84f, stiffness = 380f))
            if (!expanded) showVolume = false
        }

        fun saveCard() {
            val p = cardPos.value
            AllSettings.cryoMusicCardX.save(((p.x / (areaW - cardW).coerceAtLeast(1f)) * 1000).roundToInt().coerceIn(0, 1000))
            AllSettings.cryoMusicCardY.save(((p.y / (areaH - cardH).coerceAtLeast(1f)) * 1000).roundToInt().coerceIn(0, 1000))
        }
        fun saveBubble() {
            val p = bubblePos.value
            AllSettings.cryoMusicBubbleRight.save(p.x + bubble / 2 > areaW / 2)
            AllSettings.cryoMusicBubbleY.save(((p.y / (areaH - bubble).coerceAtLeast(1f)) * 1000).roundToInt().coerceIn(0, 1000))
        }

        /** Vira bolinha encostada no lado [right], na altura da janela. */
        fun collapse(right: Boolean) {
            val target = Offset(bubbleX(right), clampBubbleY(cardPos.value.y + cardH / 2 - bubble / 2))
            scope.launch {
                bubblePos.snapTo(target)
                saveBubble()
            }
            expanded = false
            //depois de sumir, a janela volta para dentro da tela (reabre no lugar certo)
            scope.launch {
                delay(450)
                if (!expanded) {
                    cardPos.snapTo(clampCard(cardPos.value))
                    saveCard()
                }
            }
        }
        fun collapseNearest() = collapse(cardPos.value.x + cardW / 2 > areaW / 2)

        fun expand() {
            touchTick++
            expanded = true
        }

        //Trocar de música não abre a janela: ela só abre quando a pessoa toca na bolinha
        val key = CryoNowPlaying.key
        //Some sozinha depois de ~5 s sem toque
        LaunchedEffect(expanded, touchTick, cardDragging) {
            if (expanded && !cardDragging) {
                delay(AUTO_HIDE_MS)
                collapseNearest()
            }
        }

        //posição da música (barra da janela e anel da bolinha)
        //o relógio só anda enquanto a música toca (1x por segundo) e só redesenha a barra/anel
        val position = remember { mutableLongStateOf(0L) }
        val isPlaying = CryoNowPlaying.playing
        LaunchedEffect(isPlaying, key) {
            position.longValue = CryoNowPlaying.positionMs()
            while (isPlaying) {
                delay(1000)
                position.longValue = CryoNowPlaying.positionMs()
            }
        }

        val showCard by remember { derivedStateOf { morph.value > 0.002f } }
        val showBubble by remember { derivedStateOf { morph.value < 0.998f } }
        val cardLift by animateFloatAsState(if (cardDragging) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "cardLift")
        val bubbleLift by animateFloatAsState(if (bubbleDragging) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "bubbleLift")

        // ---------------- bolinha
        if (showBubble) {
            Box(
                modifier = Modifier
                    .offset {
                        val p = lerp(bubblePos.value, cardPos.value, morph.value)
                        IntOffset(p.x.roundToInt(), p.y.roundToInt())
                    }
                    .size(BUBBLE_SIZE)
                    .graphicsLayer {
                        val m = morph.value
                        val s = 1f + m * 0.35f + bubbleLift * 0.08f
                        scaleX = s
                        scaleY = s
                        alpha = (1f - m * 2.2f).coerceIn(0f, 1f)
                        shadowElevation = (6f + bubbleLift * 10f) * density.density
                        //a bolinha minimizada é sempre redonda, em qualquer estilo
                        shape = CircleShape
                        clip = true
                    }
                    .pointerInput(areaW, areaH) {
                        val tracker = VelocityTracker()
                        var dragPos = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                dragPos = bubblePos.value
                                bubbleDragging = true
                                scope.launch { bubblePos.stop() }
                            },
                            onDragEnd = {
                                bubbleDragging = false
                                val v = tracker.calculateVelocity()
                                val projectedX = bubblePos.value.x + bubble / 2 + v.x * 0.12f
                                val right = projectedX > areaW / 2
                                val target = Offset(bubbleX(right), clampBubbleY(bubblePos.value.y + v.y * 0.1f))
                                scope.launch {
                                    bubblePos.animateTo(target, floatSpring(), initialVelocity = Offset(v.x, v.y))
                                    saveBubble()
                                }
                            },
                            onDragCancel = {
                                bubbleDragging = false
                                scope.launch {
                                    bubblePos.animateTo(
                                        Offset(bubbleX(bubblePos.value.x + bubble / 2 > areaW / 2), clampBubbleY(bubblePos.value.y)),
                                        floatSpring()
                                    )
                                    saveBubble()
                                }
                            }
                        ) { change, amount ->
                            change.consume()
                            dragPos += amount
                            tracker.addPosition(change.uptimeMillis, dragPos)
                            scope.launch { bubblePos.snapTo(dragPos) }
                        }
                    }
                    .clickable { expand() }
            ) {
                MusicBubble(position = position)
            }
        }

        // ---------------- janela
        if (showCard) {
            Box(
                modifier = Modifier
                    .offset {
                        val p = lerp(bubblePos.value, cardPos.value, morph.value)
                        IntOffset(p.x.roundToInt(), p.y.roundToInt())
                    }
                    .width(styleWidth(style))
                    .onSizeChanged {
                        cardW = it.width.toFloat()
                        cardH = it.height.toFloat()
                    }
                    .graphicsLayer {
                        val m = morph.value
                        transformOrigin = TransformOrigin(0f, 0f)
                        val lift = 1f + cardLift * 0.03f
                        scaleX = lerp(bubble / cardW, 1f, m) * lift
                        scaleY = lerp(bubble / cardH.coerceAtLeast(1f), 1f, m) * lift
                        alpha = (m * 1.6f - 0.15f).coerceIn(0f, 1f)
                        shadowElevation = (10f + cardLift * 14f) * density.density
                        shape = when (style) {
                            1 -> RoundedCornerShape(22.dp)
                            2 -> CircleShape
                            else -> RoundedCornerShape(24.dp)
                        }
                        clip = true
                    }
                    .pointerInput(areaW, areaH) {
                        val tracker = VelocityTracker()
                        var dragPos = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                dragPos = cardPos.value
                                cardDragging = true
                                touchTick++
                                scope.launch { cardPos.stop() }
                            },
                            onDragEnd = {
                                cardDragging = false
                                touchTick++
                                val v = tracker.calculateVelocity()
                                val p = cardPos.value
                                val projected = Offset(p.x + v.x * 0.16f, p.y + v.y * 0.16f)
                                val outLeft = -projected.x
                                val outRight = projected.x + cardW - areaW
                                when {
                                    //jogou ou arrastou para a beirada: vira bolinha
                                    outLeft > cardW * 0.32f || (v.x < -2600f && abs(v.x) > abs(v.y)) -> collapse(false)
                                    outRight > cardW * 0.32f || (v.x > 2600f && abs(v.x) > abs(v.y)) -> collapse(true)
                                    else -> scope.launch {
                                        cardPos.animateTo(clampCard(projected), floatSpring(), initialVelocity = Offset(v.x, v.y))
                                        saveCard()
                                    }
                                }
                            },
                            onDragCancel = {
                                cardDragging = false
                                scope.launch {
                                    cardPos.animateTo(clampCard(cardPos.value), floatSpring())
                                    saveCard()
                                }
                            }
                        ) { change, amount ->
                            change.consume()
                            dragPos += amount
                            tracker.addPosition(change.uptimeMillis, dragPos)
                            scope.launch { cardPos.snapTo(dragPos) }
                        }
                    }
            ) {
                val onToggleVolume: () -> Unit = { showVolume = !showVolume; touchTick++ }
                val onTouch: () -> Unit = { touchTick++ }
                val onMinimize: () -> Unit = { collapseNearest() }
                when (style) {
                    1 -> MusicSquare(position, showVolume, onToggleVolume, onTouch, onMinimize)
                    2 -> MusicDisc(position, showVolume, onToggleVolume, onTouch, onMinimize)
                    else -> MusicCard(position, showVolume, onToggleVolume, onTouch, onMinimize)
                }
            }
        }
    }
}

/** Liquid Glass: fundo de vidro escuro com brilho em cima (o jogo fica legível atrás). */
private fun liquidGlassFill(alpha: Float = 1f) = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.20f * alpha),
        CryoGlassColors.Navy.copy(alpha = 0.80f * alpha),
        CryoGlassColors.Deep.copy(alpha = 0.90f * alpha)
    )
)

/** Liquid Glass: aro claro em cima que some embaixo. */
private fun liquidGlassRim(strength: Float = 1f) = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = (0.85f * strength).coerceAtMost(1f)),
        Color.White.copy(alpha = 0.15f * strength),
        CryoGlassColors.Ice.copy(alpha = 0.45f * strength)
    )
)

/** Liquid Glass: bolha de vidro dos botões. */
private fun liquidBubbleFill(active: Boolean, accent: Color) =
    if (active) Brush.verticalGradient(listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.18f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.05f)))

/** Bolinha: capa da música com o anel de progresso (sem animação contínua: não gasta FPS do jogo). Sempre redonda. */
@Composable
internal fun MusicBubble(position: LongState) {
    val accent = CryoGlassColors.Ice
    val duration = CryoNowPlaying.durationMs
    val liquid = CryoLiquid.on
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (liquid) liquidGlassFill() else androidx.compose.ui.graphics.SolidColor(CryoGlassColors.Deep))
            .drawWithContent {
                drawContent()
                if (liquid) {
                    //brilho de lente no topo da bolha de vidro
                    drawOval(
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.Transparent), startY = 0f, endY = size.height * 0.45f),
                        topLeft = Offset(size.width * 0.18f, size.height * 0.06f),
                        size = Size(size.width * 0.64f, size.height * 0.36f)
                    )
                }
                //a posição é lida só aqui (fase de desenho): atualizar o anel não recompõe nada
                val progress = if (duration > 0) (position.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
                val stroke = 3.dp.toPx()
                drawCircle(color = Color.White.copy(alpha = if (liquid) 0.28f else 0.15f), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        CoverImage(
            modifier = Modifier
                .size(BUBBLE_SIZE - 10.dp)
                .clip(CircleShape)
        )
        if (!CryoNowPlaying.playing && CryoNowPlaying.source != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(modifier = Modifier.size(14.dp), painter = painterResource(R.drawable.ic_pause_filled), contentDescription = null, tint = Color.White)
            }
        }
    }
}

/** Capa em alta qualidade (filtro de redimensionamento alto) ou nota musical. */
@Composable
private fun CoverImage(
    modifier: Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    iconAlignment: Alignment = Alignment.Center,
    iconPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val art = CryoNowPlaying.art
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.45f), CryoGlassColors.Deep))),
        contentAlignment = iconAlignment
    ) {
        if (art is ImageBitmap) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(modifier = Modifier.padding(top = iconPadding).size(iconSize), painter = painterResource(R.drawable.ic_music_note), contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

/** Janela aberta: capa, nome, progresso, voltar/tocar/passar e volume. */
@Composable
internal fun MusicCard(
    position: LongState,
    showVolume: Boolean,
    onToggleVolume: () -> Unit,
    onTouch: () -> Unit,
    onMinimize: () -> Unit
) {
    val source = CryoNowPlaying.source
    val accent = CryoGlassColors.Ice
    val duration = CryoNowPlaying.durationMs
    val liquid = CryoLiquid.on
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (liquid) liquidGlassFill()
                else Brush.linearGradient(
                    listOf(CryoGlassColors.Navy.copy(alpha = 0.96f), CryoGlassColors.Deep.copy(alpha = 0.97f))
                )
            )
            .border(
                1.dp,
                if (liquid) liquidGlassRim()
                else Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), CryoGlassColors.Ice.copy(alpha = 0.15f))),
                RoundedCornerShape(24.dp)
            )
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //alça para arrastar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CoverImage(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                iconSize = 24.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CryoNowPlaying.title?.ifBlank { null } ?: stringResource(R.string.cryo_music_nothing),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, shadow = null),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (source == null) stringResource(R.string.cryo_music_nothing_sub) else CryoNowPlaying.artist.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(shadow = null),
                    color = if (source == null) CryoGlassColors.Frost.copy(alpha = 0.7f) else accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CardButton(R.drawable.ic_cryo_library_music, size = 30, onClick = { CryoMusicUi.openMusicMenu() })
            CardButton(R.drawable.ic_cryo_minimize, size = 30, onClick = onMinimize)
        }
        //progresso (desenhado direto; o relógio não recompõe a janela)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .drawBehind {
                        val f = if (duration > 0) (position.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
                        drawRect(color = accent, size = Size(size.width * f, size.height))
                    }
            )
            Row {
                PositionText(position)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (duration > 0) formatMusicTime(duration) else "--:--", style = smallTime(), color = CryoGlassColors.Frost.copy(alpha = 0.8f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardButton(R.drawable.ic_cryo_skip_previous, size = 38, enabled = source != null) { CryoNowPlaying.previous(); onTouch() }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .alpha(if (source != null) 1f else 0.4f)
                    .clip(CircleShape)
                    .cryoBackground(cryoGradient())
                    .clickable(enabled = source != null) { CryoNowPlaying.togglePlay(); onTouch() },
                contentAlignment = Alignment.Center
            ) {
                if (CryoNowPlaying.buffering) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CryoGlassColors.Deep)
                } else {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(if (CryoNowPlaying.playing) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            CardButton(R.drawable.ic_cryo_skip_next, size = 38, enabled = source != null) { CryoNowPlaying.next(); onTouch() }
            Spacer(modifier = Modifier.weight(1f))
            CardButton(R.drawable.ic_cryo_volume, size = 38, active = showVolume, enabled = CryoNowPlaying.volumeAvailable, accent = accent, onClick = onToggleVolume)
        }
        AnimatedVisibility(visible = showVolume && CryoNowPlaying.volumeAvailable, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Slider(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    value = CryoNowPlaying.volume,
                    onValueChange = { CryoNowPlaying.setVolume(it); onTouch() },
                    onValueChangeFinished = { CryoNowPlaying.saveVolume() },
                    valueRange = 0f..1f,
                    colors = sliderColors(accent)
                )
                Text(
                    text = "${(CryoNowPlaying.volume * 100).toInt()}%",
                    style = smallTime(),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CardButton(
    iconRes: Int,
    size: Int,
    active: Boolean = false,
    enabled: Boolean = true,
    accent: Color = CryoGlassColors.Ice,
    onClick: () -> Unit
) {
    val liquid = CryoLiquid.on
    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .then(
                if (liquid) Modifier
                    .background(liquidBubbleFill(active, accent))
                    .border(1.dp, if (active) androidx.compose.ui.graphics.SolidColor(accent) else liquidGlassRim(0.9f), CircleShape)
                else Modifier
                    .background(if (active) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
                    .border(1.dp, if (active) accent else CryoGlassColors.Ice.copy(alpha = 0.25f), CircleShape)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size((size * 0.52f).dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) accent else Color.White
        )
    }
}

@Composable
private fun smallTime() = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null)

/** Só este texto é refeito a cada segundo. */
@Composable
private fun PositionText(position: LongState) {
    Text(formatMusicTime(position.longValue), style = smallTime(), color = CryoGlassColors.Frost.copy(alpha = 0.8f))
}

/** Botão redondo escuro, legível por cima da capa. */
@Composable
private fun GlassButton(iconRes: Int, size: Int, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val accent = CryoGlassColors.Ice
    val liquid = CryoLiquid.on
    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .then(
                if (liquid) Modifier
                    //por cima da capa: base escura para o ícone continuar legível, com vidro claro por cima
                    .background(Color.Black.copy(alpha = 0.30f))
                    .background(liquidBubbleFill(active, accent))
                    .border(1.dp, if (active) androidx.compose.ui.graphics.SolidColor(accent) else liquidGlassRim(0.9f), CircleShape)
                else Modifier
                    .background(if (active) accent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, if (active) accent else Color.White.copy(alpha = 0.22f), CircleShape)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(modifier = Modifier.size((size * 0.52f).dp), painter = painterResource(iconRes), contentDescription = null, tint = if (active) accent else Color.White)
    }
}

/** Estilo quadrado: capa em tela cheia, controles numa barra de vidro embaixo. */
@Composable
internal fun MusicSquare(
    position: LongState,
    showVolume: Boolean,
    onToggleVolume: () -> Unit,
    onTouch: () -> Unit,
    onMinimize: () -> Unit
) {
    val source = CryoNowPlaying.source
    val accent = CryoGlassColors.Ice
    val duration = CryoNowPlaying.durationMs
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(CryoGlassColors.Deep)
    ) {
        //sem capa: a nota fica em cima, longe do nome da música
        CoverImage(modifier = Modifier.fillMaxSize(), iconSize = 52.dp, iconAlignment = Alignment.TopCenter, iconPadding = 46.dp)
        //escurece em cima (botões) e embaixo (texto/controles) para tudo ficar legível
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.22f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to CryoGlassColors.Deep.copy(alpha = 0.97f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(modifier = Modifier.size(11.dp), painter = painterResource(R.drawable.ic_music_note), contentDescription = null, tint = accent)
                    Text(
                        text = if (CryoNowPlaying.playing) "LIVE" else "PAUSE",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, shadow = null),
                        color = accent
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                GlassButton(R.drawable.ic_cryo_library_music, size = 28, onClick = { CryoMusicUi.openMusicMenu() })
                GlassButton(R.drawable.ic_cryo_minimize, size = 28, onClick = onMinimize)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showVolume) {
                VolumeRow(accent = accent, onTouch = onTouch)
            } else {
                Text(
                    text = CryoNowPlaying.title?.ifBlank { null } ?: stringResource(R.string.cryo_music_nothing),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, shadow = null),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (source == null) stringResource(R.string.cryo_music_nothing_sub) else CryoNowPlaying.artist.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(shadow = null),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            ProgressLine(position = position, duration = duration, accent = accent)
            Spacer(modifier = Modifier.height(8.dp))
            val liquid = CryoLiquid.on
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(if (liquid) liquidGlassFill(0.85f) else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.08f)))
                    .border(1.dp, if (liquid) liquidGlassRim() else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.12f)), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassButton(R.drawable.ic_cryo_skip_previous, size = 32, enabled = source != null) { CryoNowPlaying.previous(); onTouch() }
                PlayPause(size = 40, enabled = source != null, onTouch = onTouch)
                GlassButton(R.drawable.ic_cryo_skip_next, size = 32, enabled = source != null) { CryoNowPlaying.next(); onTouch() }
                GlassButton(R.drawable.ic_cryo_volume, size = 32, active = showVolume, onClick = onToggleVolume)
            }
        }
    }
}

/** Estilo disco: vinil com sulcos, capa no selo do centro e o progresso na borda. */
@Composable
internal fun MusicDisc(
    position: LongState,
    showVolume: Boolean,
    onToggleVolume: () -> Unit,
    onTouch: () -> Unit,
    onMinimize: () -> Unit
) {
    val source = CryoNowPlaying.source
    val accent = CryoGlassColors.Ice
    val duration = CryoNowPlaying.durationMs
    val liquid = CryoLiquid.on
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                if (liquid) Brush.radialGradient(listOf(CryoGlassColors.Navy.copy(alpha = 0.78f), CryoGlassColors.Deep.copy(alpha = 0.92f)))
                else Brush.radialGradient(listOf(Color(0xFF18202E), Color(0xFF07090F)))
            )
            .drawWithContent {
                val r = size.minDimension / 2
                if (liquid) {
                    //disco de vidro: brilho de lente em cima e aro claro
                    drawOval(
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), startY = 0f, endY = size.height * 0.5f),
                        topLeft = Offset(size.width * 0.14f, size.height * 0.04f),
                        size = Size(size.width * 0.72f, size.height * 0.42f)
                    )
                    drawCircle(brush = liquidGlassRim(), radius = r - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                }
                //sulcos do vinil (desenho estático: não gasta FPS)
                val groove = 3.dp.toPx()
                var gr = r * 0.42f
                while (gr < r - 10.dp.toPx()) {
                    drawCircle(color = Color.White.copy(alpha = 0.035f), radius = gr, style = Stroke(1f))
                    gr += groove
                }
                //reflexo
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent)),
                    startAngle = 200f,
                    sweepAngle = 70f,
                    useCenter = true,
                    topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                    size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx())
                )
                drawContent()
                val progress = if (duration > 0) (position.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
                val stroke = 4.dp.toPx()
                drawCircle(color = Color.White.copy(alpha = 0.10f), radius = r - stroke / 2, style = Stroke(stroke))
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                //ponto no fim do progresso
                val angle = Math.toRadians((-90f + 360f * progress).toDouble())
                val rr = r - stroke / 2
                drawCircle(
                    color = Color.White,
                    radius = stroke * 0.95f,
                    center = Offset(center.x + (rr * kotlin.math.cos(angle)).toFloat(), center.y + (rr * kotlin.math.sin(angle)).toFloat())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(R.drawable.ic_cryo_library_music, size = 24, onClick = { CryoMusicUi.openMusicMenu() })
                GlassButton(R.drawable.ic_cryo_volume, size = 24, active = showVolume, onClick = onToggleVolume)
                GlassButton(R.drawable.ic_cryo_minimize, size = 24, onClick = onMinimize)
            }
            //selo do disco com a capa e o furo no meio
            Box(contentAlignment = Alignment.Center) {
                CoverImage(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .border(2.dp, accent.copy(alpha = 0.7f), CircleShape),
                    iconSize = 28.dp
                )
                if (CryoNowPlaying.art is ImageBitmap) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF07090F))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                }
            }
            if (showVolume) {
                VolumeRow(accent = accent, onTouch = onTouch)
            } else {
                Text(
                    text = CryoNowPlaying.title?.ifBlank { null } ?: stringResource(R.string.cryo_music_nothing),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, shadow = null),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.offset(y = (-4).dp),
                    text = if (source == null) stringResource(R.string.cryo_music_nothing_sub) else CryoNowPlaying.artist.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(shadow = null),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(R.drawable.ic_cryo_skip_previous, size = 30, enabled = source != null) { CryoNowPlaying.previous(); onTouch() }
                PlayPause(size = 42, enabled = source != null, onTouch = onTouch)
                GlassButton(R.drawable.ic_cryo_skip_next, size = 30, enabled = source != null) { CryoNowPlaying.next(); onTouch() }
            }
        }
    }
}

@Composable
private fun PlayPause(size: Int, enabled: Boolean, onTouch: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .cryoBackground(cryoGradient())
            .clickable(enabled = enabled) { CryoNowPlaying.togglePlay(); onTouch() },
        contentAlignment = Alignment.Center
    ) {
        if (CryoNowPlaying.buffering) {
            CircularProgressIndicator(modifier = Modifier.size((size * 0.42f).dp), strokeWidth = 2.dp, color = CryoGlassColors.Deep)
        } else {
            Icon(
                modifier = Modifier.size((size * 0.52f).dp),
                painter = painterResource(if (CryoNowPlaying.playing) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
    }
}

@Composable
private fun VolumeRow(accent: Color, onTouch: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Slider(
            modifier = Modifier
                .weight(1f)
                .height(26.dp),
            value = CryoNowPlaying.volume,
            onValueChange = { CryoNowPlaying.setVolume(it); onTouch() },
            onValueChangeFinished = { CryoNowPlaying.saveVolume() },
            valueRange = 0f..1f,
            colors = sliderColors(accent)
        )
        Text(text = "${(CryoNowPlaying.volume * 100).toInt()}%", style = smallTime(), color = Color.White)
    }
}

/** Barra fina de progresso (desenhada direto: o relógio não recompõe a janela). */
@Composable
private fun ProgressLine(position: LongState, duration: Long, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .drawBehind {
                val f = if (duration > 0) (position.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
                drawRect(color = accent, size = Size(size.width * f, size.height))
            }
    )
}
