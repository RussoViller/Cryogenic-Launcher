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

package com.russoviller.cryogenic.ui.screens.content.elements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.ActionMenuSide
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.PetModelView
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.petlib.ArmModel
import com.russoviller.petlib.PetAnimation
import com.russoviller.petlib.PetTapReaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.russoviller.cryogenic.ui.components.safeRadialGradient

class AccountPanelDrag {
    var dragging by mutableStateOf(false)
    var dragDx by mutableStateOf(0f)
    var preview by mutableStateOf<ActionMenuSide?>(null)
}

@Composable
fun AccountPanelHost(
    modifier: Modifier = Modifier,
    outerPadding: Dp = 12.dp,
    panelFraction: Float = 0.31f,
    panel: @Composable (drag: AccountPanelDrag, panelModifier: Modifier) -> Unit,
    content: @Composable (contentModifier: Modifier) -> Unit
) {
    val side = AllSettings.cryoAccountPanelSide.state
    val drag = remember { AccountPanelDrag() }
    val scope = rememberCoroutineScope()
    val settle = remember { Animatable(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelWidth = (maxWidth * panelFraction).coerceIn(220.dp, 340.dp)
        val totalPx = with(density) { maxWidth.toPx() }
        val panelPx = with(density) { panelWidth.toPx() }
        val padPx = with(density) { outerPadding.toPx() }
        fun slotX(s: ActionMenuSide) = if (s == ActionMenuSide.START) padPx else totalPx - panelPx - padPx

        val effective = drag.preview ?: side
        val contentStart by animateDpAsState(
            targetValue = if (effective == ActionMenuSide.START) panelWidth + outerPadding * 2 else outerPadding,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
            label = "contentStart"
        )
        val contentEnd by animateDpAsState(
            targetValue = if (effective == ActionMenuSide.START) outerPadding else panelWidth + outerPadding * 2,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
            label = "contentEnd"
        )
        val lift by animateFloatAsState(if (drag.dragging) 1f else 0f, tween(180), label = "lift")

        content(
            Modifier
                .fillMaxSize()
                .padding(start = contentStart, end = contentEnd, top = outerPadding, bottom = outerPadding)
        )

        if (drag.dragging) {
            val target = drag.preview ?: side
            Box(
                modifier = Modifier
                    .offset { IntOffset(slotX(target).roundToInt(), padPx.roundToInt()) }
                    .width(panelWidth)
                    .fillMaxHeight()
                    .padding(bottom = outerPadding * 2)
                    .graphicsLayer { alpha = lift }
            ) {
                SlotGhost()
            }
        }

        val baseX = slotX(side)
        panel(
            drag,
            Modifier
                .zIndex(2f)
                .offset { IntOffset((baseX + drag.dragDx + settle.value).roundToInt(), padPx.roundToInt()) }
                .width(panelWidth)
                .fillMaxHeight()
                .padding(bottom = outerPadding * 2)
                .graphicsLayer {
                    val s = 1f + 0.04f * lift
                    scaleX = s
                    scaleY = s
                    rotationZ = (drag.dragDx / totalPx.coerceAtLeast(1f)) * 6f * lift
                    shadowElevation = 24f * lift
                }
                .pointerInput(side, totalPx, panelPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                        longPress.consume()
                        scope.launch { settle.snapTo(0f) }
                        drag.dragging = true
                        drag.dragDx = 0f
                        drag.preview = side
                        var lastX = baseX
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            drag.dragDx += change.positionChange().x
                            change.consume()
                            lastX = baseX + drag.dragDx
                            val center = lastX + panelPx / 2f
                            drag.preview = if (center < totalPx / 2f) ActionMenuSide.START else ActionMenuSide.END
                        }
                        val target = drag.preview ?: side
                        val releaseX = baseX + drag.dragDx
                        val landing = slotX(target)
                        drag.dragging = false
                        drag.dragDx = 0f
                        drag.preview = null
                        if (target != side) AllSettings.cryoAccountPanelSide.save(target)
                        scope.launch {
                            settle.snapTo(releaseX - landing)
                            settle.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 380f))
                        }
                    }
                }
        )
    }
}

@Composable
private fun SlotGhost() {
    val transition = rememberInfiniteTransition(label = "ghost")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "dash"
    )
    val ice = CryoGlassColors.Ice
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = ice.copy(alpha = 0.08f),
            cornerRadius = CornerRadius(22.dp.toPx())
        )
        drawRoundRect(
            color = ice.copy(alpha = 0.8f),
            cornerRadius = CornerRadius(22.dp.toPx()),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), phase)
            )
        )
    }
}

@Composable
fun OperatorSkinStage(
    modifier: Modifier = Modifier,
    skin: ByteArray?,
    cape: ByteArray?,
    arm: ArmModel?,
    dragging: Boolean
) {
    //uma única animação lenta: girar a luz do pedestal e subir as partículas
    val transition = rememberInfiniteTransition(label = "stage")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "time"
    )
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(CryoGlassColors.Navy.copy(alpha = 0.35f), CryoGlassColors.Deep.copy(alpha = 0.9f))
                )
            )
            .border(1.dp, cryoBorderBrush(if (dragging) 1.4f else 0.6f), shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val floorY = h - 22.dp.toPx()

            //aura atrás do personagem
            drawCircle(
                brush = safeRadialGradient(
                    listOf(aurora.copy(alpha = 0.30f), ice.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(cx, h * 0.48f),
                    radius = h * 0.55f
                ),
                radius = h * 0.55f,
                center = Offset(cx, h * 0.48f)
            )

            //cone de luz vindo de cima (holofote)
            val beam = Path().apply {
                moveTo(cx - w * 0.07f, 0f)
                lineTo(cx + w * 0.07f, 0f)
                lineTo(cx + w * 0.34f, floorY)
                lineTo(cx - w * 0.34f, floorY)
                close()
            }
            drawPath(
                beam,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), ice.copy(alpha = 0.06f), Color.Transparent),
                    startY = 0f,
                    endY = floorY
                )
            )

            //partículas de luz subindo devagar dentro do feixe
            for (i in 0 until 16) {
                val seed = i * 0.6180339f
                val lane = ((seed * 997f) % 1f) - 0.5f
                val prog = (time * (0.8f + (i % 4) * 0.2f) + seed) % 1f
                val y = floorY - prog * floorY * 0.9f
                val spread = 0.12f + 0.22f * (y / floorY)
                val x = cx + lane * w * spread * 2f + sin((time * 6.2832f * 2f) + i) * 4.dp.toPx()
                val fade = sin(prog * 3.14159f)
                drawCircle(Color.White.copy(alpha = 0.55f * fade), (1.2f + (i % 3) * 0.7f).dp.toPx(), Offset(x, y))
            }

            //pedestal de vidro: elipse com topo brilhante, borda clara e uma luz percorrendo a borda
            val rw = w * 0.34f
            val rh = rw * 0.22f
            //sombra no chão
            drawOval(
                brush = safeRadialGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), center = Offset(cx, floorY + rh * 0.6f), radius = rw * 1.1f),
                topLeft = Offset(cx - rw * 1.1f, floorY - rh * 0.3f),
                size = Size(rw * 2.2f, rh * 1.9f)
            )
            //lateral do disco (espessura)
            val thick = 7.dp.toPx()
            drawOval(
                brush = Brush.verticalGradient(listOf(ice.copy(alpha = 0.45f), aurora.copy(alpha = 0.25f)), startY = floorY - rh, endY = floorY + rh + thick),
                topLeft = Offset(cx - rw, floorY - rh + thick),
                size = Size(rw * 2f, rh * 2f)
            )
            //topo
            drawOval(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), ice.copy(alpha = 0.14f), CryoGlassColors.Deep.copy(alpha = 0.6f)),
                    startY = floorY - rh,
                    endY = floorY + rh
                ),
                topLeft = Offset(cx - rw, floorY - rh),
                size = Size(rw * 2f, rh * 2f)
            )
            //anel interno (onde o personagem pisa)
            drawOval(
                color = ice.copy(alpha = 0.35f),
                topLeft = Offset(cx - rw * 0.62f, floorY - rh * 0.62f),
                size = Size(rw * 1.24f, rh * 1.24f),
                style = Stroke(1.2.dp.toPx())
            )
            //borda clara
            drawOval(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(cx - rw, floorY - rh),
                size = Size(rw * 2f, rh * 2f),
                style = Stroke(1.5.dp.toPx())
            )
            //luz que percorre a borda: um brilho único e suave (sem tracejado girando)
            val angle = time * 6.2832f
            val gx = cx + cos(angle) * rw
            val gy = floorY + sin(angle) * rh
            val front = (sin(angle) + 1f) / 2f // mais forte na frente do disco
            drawCircle(
                brush = safeRadialGradient(listOf(Color.White.copy(alpha = 0.9f * (0.35f + 0.65f * front)), ice.copy(alpha = 0.4f * front), Color.Transparent), center = Offset(gx, gy), radius = 22.dp.toPx()),
                radius = 22.dp.toPx(),
                center = Offset(gx, gy)
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val feetY = maxHeight - STAGE_FEET_INSET
            val viewSize = ((feetY - STAGE_HEAD_INSET) / MODEL_HEIGHT_FRACTION)
                .coerceAtMost(maxWidth / MODEL_WIDTH_FRACTION)
            val viewTop = feetY - viewSize * MODEL_FEET_FRACTION
            PetModelView(
                modifier = Modifier
                    .requiredSize(viewSize)
                    .align(Alignment.TopCenter)
                    .offset(y = viewTop + ((viewSize - maxHeight) / 2).coerceAtLeast(0.dp)),
                skin = skin,
                cape = cape,
                arm = arm,
                yaw = Math.toRadians(35.0).toFloat(),
                animation = null,
                tapReaction = PetTapReaction.Squash,
                dragYaw = !dragging
            )
        }
    }
}

private val STAGE_FEET_INSET = 22.dp
private val STAGE_HEAD_INSET = 2.dp
private const val MODEL_HEIGHT_FRACTION = 0.75f
private const val MODEL_FEET_FRACTION = 0.85f
private const val MODEL_WIDTH_FRACTION = 0.40f

@Composable
fun OperatorHeader(
    name: String,
    dragging: Boolean
) {
    val ice = CryoGlassColors.Ice
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.cryo_account_operator),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontFamily = FontFamily.Monospace),
            color = ice,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(
                brush = Brush.horizontalGradient(listOf(Color.White, ice))
            ),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OperatorActionButton(
    modifier: Modifier = Modifier,
    icon: Int,
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val shape = cryoChamferShape(10.dp)
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .then(
                if (primary) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(Color.White.copy(alpha = 0.06f))
            )
            .border(1.dp, if (primary) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)) else cryoBorderBrush(0.8f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (primary) CryoGlassColors.Deep else CryoGlassColors.Ice
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            fontWeight = FontWeight.Black,
            color = if (primary) CryoGlassColors.Deep else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
