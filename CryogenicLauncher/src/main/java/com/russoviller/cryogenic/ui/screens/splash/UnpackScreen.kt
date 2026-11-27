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

import androidx.compose.foundation.layout.width
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.aspectRatio
import com.russoviller.cryogenic.ui.components.cryoBackground

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.components.InstallableItem
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoLogo
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoTacticalBackdrop
import com.russoviller.cryogenic.ui.components.cryoTacticalPanelShape
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.viewmodel.SplashBackStackViewModel
import com.russoviller.cryogenic.ui.components.safeRadialGradient

private val CryoGreen = Color(0xFF7CF5A8)
private val CryoAmber = Color(0xFFFFC46B)

@Composable
fun UnpackScreen(
    items: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel,
    onAgreeClick: () -> Unit = {}
) {
    BaseScreen(
        screenKey = SplashOperation.Unpacking,
        currentKey = screenViewModel.splashScreen.currentKey
    ) { isVisible ->
        val states = items.map { item -> key(item) { item.state.collectAsStateWithLifecycle().value } }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReactorPanel(
                isVisible = isVisible,
                states = states,
                modifier = Modifier
                    .weight(4.2f)
                    .fillMaxHeight(),
                onAgreeClick = onAgreeClick
            )
            ModuleTimeline(
                isVisible = isVisible,
                items = items,
                states = states,
                modifier = Modifier
                    .weight(5.8f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ReactorPanel(
    isVisible: Boolean,
    states: List<InstallableItem.State>,
    modifier: Modifier = Modifier,
    onAgreeClick: () -> Unit = {}
) {
    var installing by remember { mutableStateOf(false) }
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val relevant = states.filter { it != InstallableItem.State.NOT_EXISTS }
    val done = relevant.count { it == InstallableItem.State.FINISHED }
    val progress = if (relevant.isEmpty()) 0f else done.toFloat() / relevant.size
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(progress, label = "reactor")
    val transition = rememberInfiniteTransition(label = "reactor_spin")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (installing) 2400 else 9000, easing = LinearEasing)),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse"
    )
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val iceSoft = CryoGlassColors.IceSoft

    Column(
        modifier = modifier
            .animated(ctx, from = AnimatedDirection.Left)
            .cryoGlass(shape = cryoChamferShape(26.dp), cornerRadius = 26.dp, opaque = true)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CryoLogo(modifier = Modifier.size(38.dp), floating = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CRYOGENIC // SETUP",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontFamily = FontFamily.Monospace),
                    color = ice,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.cryo_unpack_subtitle),
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, ice))
                    ),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            ) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f - 6.dp.toPx()
                for (i in 0 until 60) {
                    val ang = Math.toRadians(i * 6.0 - 90.0)
                    val active = i / 60f <= animatedProgress
                    val inner = r - (if (i % 5 == 0) 9.dp.toPx() else 5.dp.toPx())
                    drawLine(
                        color = if (active) ice else Color.White.copy(alpha = 0.12f),
                        start = Offset(c.x + (kotlin.math.cos(ang) * inner).toFloat(), c.y + (kotlin.math.sin(ang) * inner).toFloat()),
                        end = Offset(c.x + (kotlin.math.cos(ang) * r).toFloat(), c.y + (kotlin.math.sin(ang) * r).toFloat()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                val ringR = r - 16.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.06f), ringR, c, style = androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx()))
                drawArc(
                    brush = Brush.sweepGradient(listOf(iceSoft, ice, aurora, iceSoft), center = c),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(c.x - ringR, c.y - ringR),
                    size = Size(ringR * 2f, ringR * 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                val innerR = ringR - 18.dp.toPx()
                rotate(spin, c) {
                    for (k in 0 until 3) {
                        drawArc(
                            color = ice.copy(alpha = 0.7f),
                            startAngle = k * 120f,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = Offset(c.x - innerR, c.y - innerR),
                            size = Size(innerR * 2f, innerR * 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                        )
                    }
                }
                drawCircle(
                    safeRadialGradient(listOf(ice.copy(alpha = 0.28f * pulse), Color.Transparent), center = c, radius = innerR),
                    innerR,
                    c
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        brush = Brush.verticalGradient(listOf(Color.White, ice))
                    ),
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "$done/${relevant.size} " + stringResource(R.string.cryo_unpack_modules).lowercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                )
            }
        }
        Text(
            text = if (installing) stringResource(R.string.splash_screen_installing) else stringResource(R.string.splash_screen_unpack_desc),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.85f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 4
        )
        CryoButton(
            enabled = !installing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = {
                installing = true
                onAgreeClick()
            }
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_download_2_filled),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.splash_screen_agree),
                style = MaterialTheme.typography.titleSmall.copy(shadow = null),
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ModuleTimeline(
    isVisible: Boolean,
    items: List<InstallableItem>,
    states: List<InstallableItem.State>,
    modifier: Modifier = Modifier,
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val density = LocalDensity.current
    val panelShape = remember(density) {
        with(density) { cryoTacticalPanelShape(false, 22.dp.toPx(), 12.dp.toPx()) }
    }
    Column(
        modifier = modifier
            .animated(ctx, from = AnimatedDirection.Right)
            .clip(panelShape)
            .cryoTacticalBackdrop(panelShape, false, 1f)
            .padding(start = 26.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_package_2_outlined),
                contentDescription = null,
                tint = CryoGlassColors.Ice
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cryo_unpack_list_title),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice,
                maxLines = 1
            )
            Text(
                text = "SEQ ${items.size.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Frost.copy(alpha = 0.6f)
            )
        }
        val scrollState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 2.dp),
            state = scrollState,
        ) {
            items(items.size) { index ->
                TimelineItem(
                    index = index,
                    last = index == items.lastIndex,
                    item = items[index],
                    state = states.getOrElse(index) { InstallableItem.State.NOT_STARTED }
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    index: Int,
    last: Boolean,
    item: InstallableItem,
    state: InstallableItem.State
) {
    val message by item.task.taskMessage.collectAsStateWithLifecycle()
    val running = state == InstallableItem.State.RUNNING
    val (accent, label) = when (state) {
        InstallableItem.State.NOT_STARTED -> CryoGlassColors.Ice to stringResource(R.string.cryo_unpack_state_new)
        InstallableItem.State.PENDING -> CryoAmber to stringResource(R.string.cryo_unpack_state_update)
        InstallableItem.State.RUNNING -> CryoGlassColors.Ice to stringResource(R.string.cryo_unpack_state_running)
        InstallableItem.State.FINISHED -> CryoGreen to stringResource(R.string.cryo_unpack_state_done)
        InstallableItem.State.NOT_EXISTS -> CryoGlassColors.Frost.copy(alpha = 0.5f) to stringResource(R.string.cryo_unpack_state_missing)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!last) {
                Box(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.7f), CryoGlassColors.Ice.copy(alpha = 0.1f))
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(24.dp)
                    .then(if (running) Modifier.cryoGlow(12.dp, selected = true) else Modifier)
                    .clip(cryoChamferShape(6.dp))
                    .cryoBackground(
                        if (state == InstallableItem.State.FINISHED) cryoGradient()
                        else Brush.linearGradient(listOf(accent.copy(alpha = 0.3f), CryoGlassColors.Deep))
                    )
                    .border(1.5.dp, accent, cryoChamferShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (state == InstallableItem.State.FINISHED) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                } else {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, shadow = null),
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
                .clip(cryoChamferShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = if (running) 0.18f else 0.06f), CryoGlassColors.Deep.copy(alpha = 0.55f))
                    )
                )
                .border(1.dp, accent.copy(alpha = if (running) 0.8f else 0.22f), cryoChamferShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .animateContentSize(animationSpec = getAnimateTween()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace, shadow = null),
                    fontWeight = FontWeight.Black,
                    color = accent,
                    maxLines = 1
                )
            }
            item.summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }
            if (running) {
                message?.let { taskMessage ->
                    Text(
                        text = "> $taskMessage",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CryoGlassColors.Ice,
                        maxLines = 1
                    )
                }
                CryoSweepBar(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CryoSweepBar(modifier: Modifier = Modifier) {
    val ice = CryoGlassColors.Ice
    val iceSoft = CryoGlassColors.IceSoft
    val aurora = CryoGlassColors.Aurora
    val deep = CryoGlassColors.Deep
    val transition = rememberInfiniteTransition(label = "unpack_sweep")
    val sweep by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "unpack_sweep_value"
    )
    Canvas(modifier = modifier.height(6.dp)) {
        val r = size.height / 2f
        drawRoundRect(color = deep, cornerRadius = CornerRadius(r))
        val w = size.width * 0.35f
        val start = (size.width * sweep).coerceAtLeast(0f)
        val end = (size.width * sweep + w).coerceAtMost(size.width)
        if (end > start) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(iceSoft, ice, aurora), startX = start, endX = end),
                topLeft = Offset(start, 0f),
                size = Size(end - start, size.height),
                cornerRadius = CornerRadius(r)
            )
        }
    }
}
