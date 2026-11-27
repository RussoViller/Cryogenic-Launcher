package com.russoviller.cryogenic.ui.components

import com.russoviller.cryogenic.ui.theme.cryoRoundedPolygonPath
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import kotlinx.coroutines.delay
import java.util.Date

val LocalXModePeek = compositionLocalOf<((Boolean) -> Unit)?> { null }

class XModeButton(
    val iconRes: Int,
    val label: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val active: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val panel: (@Composable () -> Unit)? = null,
    /** Nome fixo para abrir este painel de fora do menu (ver [XModePanelRequest]). */
    val id: String? = null,
    /** Painel que precisa de quase a tela toda (ex.: mapa do ChunkBase). */
    val wide: Boolean = false
)

/** Pedido para abrir um painel do menu pelo [XModeButton.id] (ex.: o player de música flutuante). */
object XModePanelRequest {
    var id by mutableStateOf<String?>(null)
}

private class WingShape(private val onStart: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bevel = size.height * 0.22f
        return Outline.Rounded(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(bevel)))
    }
}

private val CenterShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cut = size.height * 0.12f
        return Outline.Rounded(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(cut)))
    }
}

private val TabShape = RoundedCornerShape(50)

@Composable
private fun rememberClockText(): String {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        //acorda só na virada de cada minuto (antes eram 4 vezes por minuto, com até 15 s de atraso)
        while (true) {
            now = Date()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }
    return remember(now) { DateFormat.getTimeFormat(context).format(now) }
}

@Composable
private fun rememberBattery(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    //o sistema avisa quando a bateria muda; nada de ficar consultando de tempos em tempos
    androidx.compose.runtime.DisposableEffect(context) {
        fun read(intent: Intent?) {
            if (intent == null) return
            val l = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            level = if (l >= 0) l * 100 / scale else -1
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: Intent?) = read(intent)
        }
        val sticky = runCatching {
            androidx.core.content.ContextCompat.registerReceiver(
                context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }.getOrNull()
        read(sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return level to charging
}

@Composable
fun XModeMenu(
    visible: Boolean,
    closeScreen: () -> Unit,
    topLabel: String,
    onTopAction: () -> Unit,
    leftButtons: List<XModeButton>,
    rightButtons: List<XModeButton>,
    modeLabel: String = "CRYO-MODE",
    /** Segunda página da asa direita (arrastando para a esquerda). */
    rightPageButtons: List<XModeButton> = emptyList()
) {
    var expanded by remember { mutableStateOf<XModeButton?>(null) }
    var expandedFromLeft by remember { mutableStateOf(true) }
    var peeking by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (!visible) expanded = null }
    //abre direto o painel pedido de fora do menu (quando o menu já está na tela)
    val requested = XModePanelRequest.id
    LaunchedEffect(visible, requested) {
        if (!visible || requested == null) return@LaunchedEffect
        val fromLeft = leftButtons.firstOrNull { it.id == requested }
        val target = fromLeft ?: (rightButtons + rightPageButtons).firstOrNull { it.id == requested }
        if (target?.panel != null) {
            expandedFromLeft = fromLeft != null
            expanded = target
        }
        XModePanelRequest.id = null
    }
    LaunchedEffect(expanded) { peeking = false }
    val scrimAlpha by animateFloatAsState(if (peeking) 0f else 1f, tween(160), label = "scrim")
    BackHandler(enabled = visible && expanded != null) { expanded = null }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(scrimAlpha)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.55f))))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { if (expanded != null) expanded = null else closeScreen() }
                    )
            )
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 860.dp)
                .fillMaxWidth(0.94f)
                .padding(bottom = 6.dp),
            visible = visible && expanded == null,
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 3 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 4 }
        ) {
            XModeDock(
                topLabel = topLabel,
                onTopAction = onTopAction,
                leftButtons = leftButtons,
                rightButtons = rightButtons,
                rightPageButtons = rightPageButtons,
                modeLabel = modeLabel,
                onOpen = { button, fromLeft ->
                    if (button.panel != null) {
                        expandedFromLeft = fromLeft
                        expanded = button
                    } else {
                        button.onClick?.invoke()
                    }
                }
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = visible && expanded != null,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.18f,
                transformOrigin = TransformOrigin(if (expandedFromLeft) 0.12f else 0.88f, 0.92f),
                animationSpec = tween(320)
            ),
            exit = fadeOut(tween(160)) + scaleOut(
                targetScale = 0.2f,
                transformOrigin = TransformOrigin(if (expandedFromLeft) 0.12f else 0.88f, 0.92f),
                animationSpec = tween(220)
            )
        ) {
            val button = expanded
            if (button != null) {
                CompositionLocalProvider(LocalXModePeek provides { peeking = it }) {
                    XModePanel(
                        button = button,
                        peeking = peeking,
                        onClose = { expanded = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun XModePanel(button: XModeButton, peeking: Boolean, onClose: () -> Unit) {
    val liquid = CryoLiquid.on
    val shape = if (liquid) RoundedCornerShape(28.dp) else cryoChamferShape(20.dp)
    val surface by animateFloatAsState(if (peeking) 0f else 1f, tween(160), label = "surface")
    val content by animateFloatAsState(if (peeking) 0.42f else 1f, tween(160), label = "content")
    Column(
        modifier = Modifier
            .fillMaxWidth(if (button.wide) 0.96f else 0.62f)
            .fillMaxHeight(if (button.wide) 0.94f else 0.86f)
            .then(if (surface > 0.5f && !liquid) Modifier.cryoGlow(20.dp, selected = true, strength = 0.8f) else Modifier)
            .clip(shape)
            .background(
                if (liquid) Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f * surface),
                        CryoGlassColors.Navy.copy(alpha = 0.86f * surface),
                        CryoGlassColors.Deep.copy(alpha = 0.92f * surface)
                    )
                )
                else Brush.verticalGradient(listOf(CryoGlassColors.Navy.copy(alpha = 0.97f * surface), CryoGlassColors.Deep.copy(alpha = 0.98f * surface)))
            )
            .border(
                if (liquid) 1.dp else 1.5.dp,
                if (liquid) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.15f), CryoGlassColors.Ice.copy(alpha = 0.5f)))
                else Brush.linearGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Ice.copy(alpha = 0.1f), CryoGlassColors.Aurora)),
                shape
            )
            .alpha(content)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(19.dp),
                    painter = painterResource(button.iconRes),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (liquid) "LIQUID // MODULE" else "CRYO // MODULE",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, fontSize = 9.sp),
                    color = CryoGlassColors.Ice
                )
                Text(
                    text = button.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    ),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(17.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_close),
                    tint = CryoGlassColors.Ice
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.7f), CryoGlassColors.Aurora.copy(alpha = 0.3f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CompositionLocalProvider(LocalContentColor provides CryoGlassColors.Frost) {
                button.panel?.invoke()
            }
        }
    }
}

private class DockGeometry(
    val w: Float,
    val h: Float,
    val shellTop: Float,
    val shoulderY: Float,
    val wingSlope: Float,
    val tipInset: Float,
    val tipY: Float,
    val centerTopW: Float,
    val slant: Float,
    val centerTopY: Float,
    val centerBottomY: Float,
    val corner: Float
) {
    val cx = w / 2f
    val centerTopLeft = cx - centerTopW / 2f
    val centerTopRight = cx + centerTopW / 2f
    val centerBottomLeft = centerTopLeft - slant
    val centerBottomRight = centerTopRight + slant
    private val rise = shoulderY - shellTop
    val raisedLeft = centerTopLeft - slant * 0.4f
    val raisedRight = centerTopRight + slant * 0.4f

    /** Casco: asas com pontas em seta e o meio elevado, tudo em cortes retos. */
    fun shell(): Path = cryoRoundedPolygonPath(
        listOf(
            Offset(tipInset, h),
            Offset(0f, tipY),
            Offset(wingSlope, shoulderY),
            Offset(raisedLeft - rise * 1.3f, shoulderY),
            Offset(raisedLeft, shellTop),
            Offset(raisedRight, shellTop),
            Offset(raisedRight + rise * 1.3f, shoulderY),
            Offset(w - wingSlope, shoulderY),
            Offset(w, tipY),
            Offset(w - tipInset, h)
        ),
        corner
    )

    /** Painel central: trapézio mais largo embaixo. */
    fun center(): Path = cryoRoundedPolygonPath(
        listOf(
            Offset(centerTopLeft, centerTopY),
            Offset(centerTopRight, centerTopY),
            Offset(centerBottomRight, centerBottomY),
            Offset(centerBottomLeft, centerBottomY)
        ),
        corner * 1.5f
    )

    /** Uma das 3 lâminas de luz da ponta (i = 0 é a de cima). */
    fun blade(left: Boolean, i: Int, thickness: Float, gap: Float): Path {
        //As lâminas acompanham a aresta inclinada de fora (da ponta até o ombro)
        val ax = if (left) 0f else w
        val bx = if (left) wingSlope else w - wingSlope
        val dir = if (left) 1f else -1f
        val t0 = 0.10f + i * 0.27f
        val t1 = t0 + 0.21f
        fun edge(t: Float) = Offset(ax + (bx - ax) * (1f - t), tipY + (shoulderY - tipY) * (1f - t))
        val a = edge(t0)
        val b = edge(t1)
        val inset = dir * gap
        return Path().apply {
            moveTo(a.x + inset, a.y)
            lineTo(b.x + inset, b.y)
            lineTo(b.x + inset + dir * thickness, b.y)
            lineTo(a.x + inset + dir * thickness, a.y)
            close()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.neonStroke(path: Path, color: Color, core: Float, intensity: Float) {
    drawPath(path, color.copy(alpha = 0.08f * intensity), style = Stroke(core * 6f))
    drawPath(path, color.copy(alpha = 0.18f * intensity), style = Stroke(core * 3.4f))
    drawPath(path, color.copy(alpha = 0.5f * intensity), style = Stroke(core * 1.8f))
    drawPath(path, color, style = Stroke(core))
    drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(core * 0.3f))
}

private val DockHeight = 192.dp
private val DockTabHeight = 26.dp
private val DockShellTop = 13.dp
private val DockShoulder = 36.dp
private val DockWingSlope = 34.dp
private val DockTipInset = 22.dp
private val DockSlant = 20.dp
private val DockCenterTop = 30.dp
private val DockCenterBottomInset = 8.dp
private val DockSideInset = 50.dp
private val DockWingGap = 8.dp
private val DockBannerHeight = 24.dp

/** Etiqueta em hexágono achatado (aba de saída e faixa CRYO-MODE). */
private class PlateShape(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(size.width / 3f)
        return Outline.Generic(
            Path().apply {
                moveTo(c, 0f)
                lineTo(size.width - c, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width - c, size.height)
                lineTo(c, size.height)
                lineTo(0f, size.height / 2f)
                close()
            }
        )
    }
}

@Composable
private fun XModeDock(
    topLabel: String,
    onTopAction: () -> Unit,
    leftButtons: List<XModeButton>,
    rightButtons: List<XModeButton>,
    rightPageButtons: List<XModeButton>,
    modeLabel: String,
    onOpen: (XModeButton, Boolean) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "rog")
    val pulse = transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse"
    )
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "sweep"
    )
    //Luz que "corre" pelas 3 lâminas de cada ponta
    val chase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "chase"
    )
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(DockHeight)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
    ) {
        val density = LocalDensity.current
        val centerTopWDp = (maxWidth * 0.27f).coerceIn(190.dp, 250.dp)
        val geo = with(density) {
            DockGeometry(
                w = maxWidth.toPx(),
                h = maxHeight.toPx(),
                shellTop = DockShellTop.toPx(),
                shoulderY = DockShoulder.toPx(),
                wingSlope = DockWingSlope.toPx(),
                tipInset = DockTipInset.toPx(),
                tipY = maxHeight.toPx() * 0.70f,
                centerTopW = centerTopWDp.toPx(),
                slant = DockSlant.toPx(),
                centerTopY = DockCenterTop.toPx(),
                centerBottomY = (maxHeight - DockCenterBottomInset).toPx(),
                corner = 5.dp.toPx()
            )
        }
        val centerBottomWDp = centerTopWDp + DockSlant * 2
        val wingWidth = (maxWidth - centerBottomWDp) / 2 - DockSideInset - DockWingGap

        //Casco: a arte fixa (preenchimento, trama, pontos, colchetes e neon base) é gravada uma vez
        //numa camada da GPU e reaproveitada; a cada quadro só entram as luzes animadas.
        val liquid = CryoLiquid.on
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val shell = geo.shell()
                    val center = geo.center()
                    val blades = List(6) { k -> geo.blade(k < 3, k % 3, 7.dp.toPx(), 7.dp.toPx()) }
                    val glowR = geo.h * 0.75f
                    val tipGlows = listOf(0f, geo.w).map { gx ->
                        Offset(gx, geo.tipY) to safeRadialGradient(
                            listOf(aurora.copy(alpha = if (liquid) 0.30f else 0.42f), Color.Transparent),
                            center = Offset(gx, geo.tipY),
                            radius = glowR
                        )
                    }
                    val bladeBrush = Brush.verticalGradient(listOf(ice, aurora), startY = geo.shoulderY, endY = geo.tipY)
                    val art = obtainGraphicsLayer()
                    art.record {
                        if (liquid) drawLiquidDock(geo, shell, center, blades, ice, aurora)
                        else drawNeonDock(geo, shell, center, blades, ice, aurora)
                    }
                    onDrawBehind {
                        drawLayer(art)
                        val p = pulse.value
                        val w = geo.w
                        //brilho das pontas respirando
                        clipPath(shell) {
                            for ((c, brush) in tipGlows) drawCircle(brush, glowR, c, alpha = p)
                        }
                        //contorno: um traço de brilho pulsando por cima do neon/vidro fixo
                        val edgeColor = if (liquid) Color.White else ice
                        drawPath(shell, edgeColor.copy(alpha = (if (liquid) 0.10f else 0.16f) * p), style = Stroke(1.8.dp.toPx() * 2.4f))
                        //reflexo que corre pelo contorno
                        val sx = w * sweep.value
                        drawPath(
                            shell,
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = if (liquid) 0.7f else 1f), Color.Transparent),
                                startX = sx - w * 0.06f,
                                endX = sx + w * 0.06f
                            ),
                            style = Stroke(if (liquid) 1.6.dp.toPx() else 2.2.dp.toPx())
                        )
                        //lâminas das pontas acendendo em sequência
                        val c = chase.value
                        for (k in 0 until 6) {
                            val i = k % 3
                            val d = kotlin.math.abs(c - i).let { minOf(it, 3f - it) }
                            val lit = (1f - d).coerceIn(0f, 1f)
                            if (lit <= 0.01f) continue
                            drawPath(blades[k], bladeBrush, alpha = lit * 0.72f)
                        }
                    }
                }
        )

        //Aba de cima: forçar saída
        val tabShape = remember(liquid) { if (liquid) TabShape else PlateShape(12.dp) }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(centerTopWDp * 0.86f)
                .height(DockTabHeight)
                .clip(tabShape)
                .background(
                    if (liquid) Brush.verticalGradient(listOf(CryoDanger.copy(alpha = 0.85f), Color(0xFF8E0F30).copy(alpha = 0.75f)))
                    else Brush.horizontalGradient(listOf(Color(0xFF8E0F30), CryoDanger, Color(0xFF8E0F30)))
                )
                .border(
                    1.dp,
                    if (liquid) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.2f)))
                    else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.65f)),
                    tabShape
                )
                .clickable(onClick = onTopAction),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    modifier = Modifier.size(13.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = topLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontStyle = FontStyle.Italic,
                        shadow = if (liquid) null else androidx.compose.ui.graphics.Shadow(color = Color(0xFFFF4D7A), blurRadius = 12f)
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        //Painel central: faixa do modo, relógio/bateria, marca e rodapé
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DockCenterBottomInset)
                .width(centerTopWDp)
                .height(maxHeight - DockCenterTop - DockCenterBottomInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bannerShape = remember(liquid) { if (liquid) TabShape else PlateShape(11.dp) }
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .fillMaxWidth(0.72f)
                    .height(DockBannerHeight)
                    .clip(bannerShape)
                    .cryoBackground(cryoGradient(), bannerShape)
                    .border(1.dp, Color.White.copy(alpha = if (liquid) 0.9f else 0.7f), bannerShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 3.sp,
                        fontStyle = FontStyle.Italic,
                        shadow = null
                    ),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep,
                    maxLines = 1
                )
            }
            DockClockRow()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CRYOGENIC",
                    style = MaterialTheme.typography.titleLarge.copy(
                        brush = Brush.verticalGradient(listOf(Color.White, ice)),
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(color = ice, blurRadius = if (liquid) 14f else 24f)
                    ),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = "LAUNCHER",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 6.sp, fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = aurora,
                    maxLines = 1
                )
            }
            //Rodapé: uso de CPU e GPU ao vivo (só ele recompõe a cada leitura)
            DockStatsRow()
        }

        RogWing(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = DockSideInset, bottom = DockCenterBottomInset)
                .width(wingWidth)
                .height(maxHeight - DockShoulder - DockCenterBottomInset - 4.dp),
            buttons = leftButtons,
            onOpen = { onOpen(it, true) }
        )
        RogWing(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = DockSideInset, bottom = DockCenterBottomInset)
                .width(wingWidth)
                .height(maxHeight - DockShoulder - DockCenterBottomInset - 4.dp),
            buttons = rightButtons,
            pageButtons = rightPageButtons,
            onOpen = { onOpen(it, false) }
        )
    }
}

/** Relógio e bateria: atualizam sozinhos, sem recompor o resto do menu. */
@Composable
private fun DockClockRow() {
    val clock = rememberClockText()
    val (battery, charging) = rememberBattery()
    val liquid = CryoLiquid.on
    val neonText = if (liquid) null else androidx.compose.ui.graphics.Shadow(color = CryoGlassColors.Ice, blurRadius = 18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = clock,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, shadow = neonText, lineHeight = 14.sp),
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        if (battery >= 0) {
            SegmentedBattery(level = battery, charging = charging)
            Spacer(Modifier.width(5.dp))
            Text(
                text = "$battery%",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, shadow = neonText, lineHeight = 14.sp),
                fontWeight = FontWeight.Black,
                color = if (battery <= 15 && !charging) CryoDanger else Color.White,
                maxLines = 1
            )
        }
    }
}

/** CPU e GPU ao vivo (1 leitura por segundo, fora da thread da interface). */
@Composable
private fun DockStatsRow() {
    val cpu by com.russoviller.cryogenic.utils.device.rememberUsagePercent(true) {
        com.russoviller.cryogenic.utils.device.CryoUsageMonitor.cpuPercent()
    }
    val gpu by com.russoviller.cryogenic.utils.device.rememberGpuReading(true)
    val gpuText = gpu?.percent?.let { "$it%" } ?: gpu?.mhz?.let { "${it}MHz" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockStat(label = "CPU", value = cpu?.let { "$it%" } ?: "--", alignEnd = false)
        Spacer(Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(R.drawable.ic_sports_esports_filled),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
        Spacer(Modifier.weight(1f))
        DockStat(label = "GPU", value = gpuText ?: "--", alignEnd = true)
    }
}

/** Arte fixa do casco no estilo neon (padrão). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonDock(
    geo: DockGeometry,
    shell: Path,
    center: Path,
    blades: List<Path>,
    ice: Color,
    aurora: Color
) {
    val w = geo.w
    val h = geo.h
    drawPath(
        shell,
        Brush.verticalGradient(
            listOf(CryoGlassColors.Navy.copy(alpha = 0.94f), CryoGlassColors.Deep.copy(alpha = 0.97f), Color(0xFA020305)),
            startY = geo.shellTop,
            endY = h
        )
    )
    clipPath(shell) {
        //trama diagonal bem discreta
        val step = 11.dp.toPx()
        var x = -h
        while (x < w) {
            drawLine(ice.copy(alpha = 0.03f), Offset(x, h), Offset(x + h, 0f), 1.dp.toPx())
            x += step
        }
        //reflexo no ombro
        drawRect(
            Brush.verticalGradient(listOf(ice.copy(alpha = 0.14f), Color.Transparent), startY = geo.shoulderY, endY = geo.shoulderY + 22.dp.toPx()),
            topLeft = Offset(0f, geo.shoulderY),
            size = Size(w, 22.dp.toPx())
        )
        //linha de base
        val baseY = h - 3.dp.toPx()
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, ice.copy(alpha = 0.85f), aurora.copy(alpha = 0.85f), Color.Transparent), startX = 0f, endX = w),
            Offset(geo.tipInset, baseY), Offset(w - geo.tipInset, baseY), 1.5.dp.toPx()
        )
    }
    neonStroke(shell, ice, 1.8.dp.toPx(), 0.7f)
    //lâminas apagadas (a luz que corre é desenhada por cima a cada quadro)
    val bladeBrush = Brush.verticalGradient(listOf(ice, aurora), startY = geo.shoulderY, endY = geo.tipY)
    for (blade in blades) {
        drawPath(blade, aurora.copy(alpha = 0.06f), style = Stroke(5.dp.toPx()))
        drawPath(blade, bladeBrush, alpha = 0.28f)
    }
    //painel central
    drawPath(center, Brush.verticalGradient(listOf(ice.copy(alpha = 0.14f), Color(0xEE020407)), startY = geo.centerTopY, endY = geo.centerBottomY))
    clipPath(center) {
        val ccx = geo.cx
        val ccy = (geo.centerTopY + geo.centerBottomY) / 2f + 8.dp.toPx()
        val rx = geo.centerTopW * 0.42f
        val ry = (geo.centerBottomY - geo.centerTopY) * 0.30f
        //matriz de pontos atrás da marca
        val dot = 8.dp.toPx()
        val dotR = 1.3.dp.toPx()
        var yy = ccy - ry
        while (yy <= ccy + ry) {
            var xx = ccx - rx
            while (xx <= ccx + rx) {
                val d = kotlin.math.hypot((xx - ccx) / rx, (yy - ccy) / ry)
                if (d < 1f) drawCircle(ice.copy(alpha = (1f - d) * 0.42f), dotR, Offset(xx, yy))
                xx += dot
            }
            yy += dot
        }
        //colchetes em ângulo nos dois lados da marca
        val bw = 10.dp.toPx()
        val bh = ry * 0.72f
        for (sgn in listOf(-1f, 1f)) {
            val bx = ccx + sgn * rx
            val bracket = Path().apply {
                moveTo(bx - sgn * bw, ccy - bh)
                lineTo(bx, ccy - bh * 0.55f)
                lineTo(bx, ccy + bh * 0.55f)
                lineTo(bx - sgn * bw, ccy + bh)
            }
            drawPath(bracket, ice.copy(alpha = 0.22f), style = Stroke(5.dp.toPx()))
            drawPath(bracket, ice.copy(alpha = 0.9f), style = Stroke(1.6.dp.toPx()))
        }
        val lineY = geo.centerBottomY - 28.dp.toPx()
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, ice.copy(alpha = 0.6f), Color.Transparent), startX = geo.centerBottomLeft, endX = geo.centerBottomRight),
            Offset(geo.centerBottomLeft, lineY), Offset(geo.centerBottomRight, lineY), 1.dp.toPx()
        )
    }
    neonStroke(center, ice, 1.6.dp.toPx(), 0.85f)
}

/**
 * Arte fixa do casco no Liquid Glass: vidro escuro translúcido com borda clara, reflexo de lente
 * no topo e painel central mais claro. Sem trama, pontos ou neon em camadas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiquidDock(
    geo: DockGeometry,
    shell: Path,
    center: Path,
    blades: List<Path>,
    ice: Color,
    aurora: Color
) {
    val w = geo.w
    val h = geo.h
    //corpo de vidro: claro em cima, escurecendo para o jogo continuar legível atrás
    drawPath(
        shell,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.20f), CryoGlassColors.Navy.copy(alpha = 0.52f), CryoGlassColors.Deep.copy(alpha = 0.70f)),
            startY = geo.shellTop,
            endY = h
        )
    )
    clipPath(shell) {
        //reflexo de lente: faixa clara logo abaixo da borda de cima
        drawRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), startY = geo.shoulderY, endY = geo.shoulderY + 30.dp.toPx()),
            topLeft = Offset(0f, geo.shoulderY),
            size = Size(w, 30.dp.toPx())
        )
        //luz de tom do tema vinda de baixo
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, ice.copy(alpha = 0.10f)), startY = h * 0.55f, endY = h),
            topLeft = Offset(0f, h * 0.55f),
            size = Size(w, h * 0.45f)
        )
    }
    //aro de vidro: fino e claro em cima, some embaixo; por dentro, uma sombra leve dá espessura
    drawPath(shell, Color.Black.copy(alpha = 0.22f), style = Stroke(4.dp.toPx()))
    drawPath(
        shell,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.25f), ice.copy(alpha = 0.55f)), startY = geo.shellTop, endY = h),
        style = Stroke(1.4.dp.toPx())
    )
    //lâminas como cápsulas de vidro apagadas
    for (blade in blades) {
        drawPath(blade, Color.White.copy(alpha = 0.10f))
        drawPath(blade, Color.White.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
    }
    //painel central: vidro mais claro
    drawPath(
        center,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), CryoGlassColors.Deep.copy(alpha = 0.55f)), startY = geo.centerTopY, endY = geo.centerBottomY)
    )
    clipPath(center) {
        val ccx = geo.cx
        val ccy = (geo.centerTopY + geo.centerBottomY) / 2f + 8.dp.toPx()
        val r = geo.centerTopW * 0.45f
        drawCircle(
            safeRadialGradient(listOf(aurora.copy(alpha = 0.22f), Color.Transparent), center = Offset(ccx, ccy), radius = r),
            r,
            Offset(ccx, ccy)
        )
        val lineY = geo.centerBottomY - 28.dp.toPx()
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), startX = geo.centerBottomLeft, endX = geo.centerBottomRight),
            Offset(geo.centerBottomLeft, lineY), Offset(geo.centerBottomRight, lineY), 1.dp.toPx()
        )
    }
    drawPath(center, Color.Black.copy(alpha = 0.18f), style = Stroke(3.dp.toPx()))
    drawPath(
        center,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.18f)), startY = geo.centerTopY, endY = geo.centerBottomY),
        style = Stroke(1.2.dp.toPx())
    )
}

@Composable
private fun DockStat(label: String, value: String, alignEnd: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val name = @Composable {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic, fontSize = 9.sp),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Aurora,
                maxLines = 1
            )
        }
        if (!alignEnd) name()
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1
        )
        if (alignEnd) name()
    }
}

/**
 * Asa de botões. Quando não cabem todos, vira páginas inteiras (2 linhas × colunas que cabem),
 * trocadas por arraste com encaixe — sem rolagem livre dentro do pager, que brigava com o
 * arraste da página e fazia a asa "voltar".
 */
@Composable
private fun RogWing(
    modifier: Modifier,
    buttons: List<XModeButton>,
    onOpen: (XModeButton) -> Unit,
    pageButtons: List<XModeButton> = emptyList()
) {
    val all = buttons + pageButtons
    BoxWithConstraints(modifier = modifier) {
        val cellWidth = 63.dp
        val gap = 2.dp
        val columns = ((maxWidth + gap) / (cellWidth + gap)).toInt().coerceAtLeast(1)
        val perPage = columns * 2
        val pages = all.chunked(perPage)
        if (pages.size <= 1) {
            WingPage(buttons = all, onOpen = onOpen)
            return@BoxWithConstraints
        }
        val pagerState = rememberPagerState(pageCount = { pages.size })
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { it }
        ) { page ->
            WingPage(buttons = pages[page], onOpen = onOpen)
        }
        //indicador de página
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pages.size) { i ->
                val active = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .width(if (active) 14.dp else 5.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (active) CryoGlassColors.Ice else CryoGlassColors.Ice.copy(alpha = 0.35f))
                )
            }
        }
    }
}

/** Uma página da asa: colunas de 2 botões, centralizadas, sem rolagem. */
@Composable
private fun WingPage(buttons: List<XModeButton>, onOpen: (XModeButton) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
    ) {
        buttons.chunked(2).forEach { column ->
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                column.forEach { button ->
                    RogButton(
                        modifier = Modifier.weight(1f),
                        button = button,
                        onClick = { onOpen(button) }
                    )
                }
                if (column.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RogButton(button: XModeButton, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = if (button.danger) CryoDanger else CryoGlassColors.Ice
    val alpha = if (button.enabled) 1f else 0.35f
    val liquid = CryoLiquid.on
    Column(
        modifier = modifier
            .width(63.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = button.enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .drawBehind {
                    //Só o botão ligado tem halo; os outros ficam limpos, com anel fino
                    if (button.active) {
                        drawCircle(
                            safeRadialGradient(listOf(accent.copy(alpha = 0.6f * alpha), Color.Transparent), radius = size.minDimension * 0.72f),
                            radius = size.minDimension * 0.72f
                        )
                    }
                }
                .clip(CircleShape)
                .cryoBackground(
                    when {
                        button.active -> cryoGradient()
                        //bolha de vidro: clara em cima, transparente embaixo
                        liquid -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f * alpha), Color.White.copy(alpha = 0.06f * alpha)))
                        else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f * alpha), Color.Black.copy(alpha = 0.35f)))
                    }
                )
                .border(
                    if (liquid) 1.dp else 1.5.dp,
                    when {
                        button.active -> Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.5f)))
                        liquid -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f * alpha), Color.White.copy(alpha = 0.15f * alpha), accent.copy(alpha = 0.5f * alpha)))
                        else -> Brush.linearGradient(listOf(accent.copy(alpha = 0.95f * alpha), accent.copy(alpha = 0.35f * alpha)))
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(19.dp),
                painter = painterResource(button.iconRes),
                contentDescription = null,
                tint = if (button.active) CryoGlassColors.Deep else Color.White.copy(alpha = alpha)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
                .height(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = button.label,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 10.5.sp, fontSize = 9.sp),
                fontWeight = FontWeight.Bold,
                color = (if (button.active) accent else Color.White).copy(alpha = alpha),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SegmentedBattery(level: Int, charging: Boolean) {
    val on = when {
        charging -> Color(0xFF7CF5A8)
        level <= 15 -> CryoDanger
        else -> CryoGlassColors.Ice
    }
    Canvas(modifier = Modifier.size(width = 34.dp, height = 13.dp)) {
        val bodyW = size.width - 3.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            size = Size(bodyW, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(1.2.dp.toPx())
        )
        drawRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(bodyW, size.height * 0.3f),
            size = Size(2.dp.toPx(), size.height * 0.4f)
        )
        val inset = 2.dp.toPx()
        val segments = 5
        val gap = 1.dp.toPx()
        val segW = (bodyW - inset * 2 - gap * (segments - 1)) / segments
        val filled = ((level + 19) / 20).coerceIn(0, segments)
        for (i in 0 until segments) {
            drawRect(
                color = if (i < filled) on else Color.White.copy(alpha = 0.08f),
                topLeft = Offset(inset + i * (segW + gap), inset),
                size = Size(segW, size.height - inset * 2)
            )
        }
    }
}

@Composable
private fun BatteryGlyph(level: Int, charging: Boolean) {
    val color = when {
        charging -> Color(0xFF7CF5A8)
        level <= 15 -> CryoDanger
        else -> Color(0xFF7CF5A8)
    }
    Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
        val bodyW = size.width - 3.dp.toPx()
        drawRoundRect(
            color = Color.White,
            size = Size(bodyW, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(1.2.dp.toPx())
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(bodyW, size.height * 0.3f),
            size = Size(2.dp.toPx(), size.height * 0.4f)
        )
        val inset = 2.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size((bodyW - inset * 2) * (level / 100f), size.height - inset * 2)
        )
    }
}
