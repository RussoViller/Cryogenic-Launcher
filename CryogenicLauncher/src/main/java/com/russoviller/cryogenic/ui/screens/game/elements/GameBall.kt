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

package com.russoviller.cryogenic.ui.screens.game.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.enums.FpsDisplayMode
import com.russoviller.cryogenic.ui.components.FloatingBall
import com.russoviller.cryogenic.ui.screens.content.elements.MemoryPreview

@Composable
fun DraggableGameBall(
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    onSavePos: () -> Unit,
    gameFps: () -> Int?,
    fpsDisplayMode: FpsDisplayMode,
    fpsHistory: () -> List<Int>,
    fpsMax: () -> Int,
    fpsMin: () -> Int,
    showMemory: Boolean,
    opened: Boolean,
    showCpu: Boolean = false,
    showGpu: Boolean = false,
    alpha: Float = 1f,
    onClick: () -> Unit = {}
) {
    FloatingBall(
        modifier = Modifier.focusProperties {
            canFocus = false
        },
        position = position,
        onPositionChanged = onPositionChanged,
        onSavePos = onSavePos,
        onClick = onClick,
        alpha = alpha
    ) {
        GameBallContent(
            gameFps = gameFps,
            fpsDisplayMode = fpsDisplayMode,
            fpsHistory = fpsHistory,
            fpsMax = fpsMax,
            fpsMin = fpsMin,
            showMemory = showMemory,
            opened = opened,
            showCpu = showCpu,
            showGpu = showGpu,
        )
    }
}

@Composable
private fun GameBallContent(
    gameFps: () -> Int?,
    fpsDisplayMode: FpsDisplayMode,
    fpsHistory: () -> List<Int>,
    fpsMax: () -> Int,
    fpsMin: () -> Int,
    showMemory: Boolean,
    opened: Boolean,
    showCpu: Boolean = false,
    showGpu: Boolean = false,
) {
    val cpu by com.russoviller.cryogenic.utils.device.rememberUsagePercent(showCpu) {
        com.russoviller.cryogenic.utils.device.CryoUsageMonitor.cpuPercent()
    }
    val gpu by com.russoviller.cryogenic.utils.device.rememberGpuReading(showGpu)
    val showUsage = showCpu || showGpu
    val fps = gameFps()
    val history = fpsHistory()
    val fpsMaxValue = fpsMax()
    val fpsMinValue = fpsMin()

    val showFps = remember(fps) {
        fps != null
    }

    Row(
        modifier = Modifier.padding(all = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val showIcon = !(showMemory || showUsage || (showFps && fpsDisplayMode == FpsDisplayMode.CHART))

        AnimatedVisibility(
            visible = showIcon
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(opened) { state ->
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(
                            if (state) {
                                R.drawable.ic_cryo_menu_open
                            } else {
                                R.drawable.ic_cryo_menu
                            }
                        ),
                        contentDescription = null
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showFps || showMemory || showUsage
        ) {
            Spacer(Modifier.width(4.dp))
        }

        //实际内容
        Column(
            modifier = Modifier
                .wrapContentSize()
                .animateContentSize()
                .width(IntrinsicSize.Max)
        ) {
            CustomAnimatedVisibility(
                visible = showFps || showMemory || showUsage
            ) {
                Spacer(Modifier.height(4.dp))
            }
            //帧率显示
            CustomAnimatedVisibility(
                visible = showFps
            ) {
                if (fpsDisplayMode == FpsDisplayMode.CHART) {
                    //帧率图表
                    FpsChart(
                        modifier = Modifier.padding(end = 4.dp),
                        history = history,
                        fpsMax = fpsMaxValue,
                        fpsMin = fpsMinValue
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(end = 4.dp),
                        text = "FPS: ${fps ?: 0}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            CustomAnimatedVisibility(
                visible = showFps && showMemory
            ) {
                Spacer(Modifier.height(4.dp))
            }
            //内存显示
            CustomAnimatedVisibility(
                visible = showMemory
            ) {
                CryoRamMeter(
                    modifier = Modifier
                        .width(176.dp)
                        .padding(end = 4.dp)
                )
            }
            CustomAnimatedVisibility(
                visible = showUsage
            ) {
                Column(
                    modifier = Modifier.padding(top = if (showFps || showMemory) 4.dp else 0.dp, end = 4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    if (showCpu) {
                        Text(
                            text = "CPU: ${cpu?.let { "$it%" } ?: "--"}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (showGpu) {
                        val reading = gpu
                        Text(
                            text = "GPU: " + when {
                                reading?.percent != null -> "${reading.percent}%"
                                reading?.mhz != null -> "${reading.mhz}MHz"
                                reading == null || reading.pending -> "--"
                                else -> "N/D"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            CustomAnimatedVisibility(
                visible = showFps || showMemory || showUsage
            ) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.CustomAnimatedVisibility(
    visible: Boolean,
    content: @Composable (AnimatedVisibilityScope.() -> Unit)
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandIn(expandFrom = Alignment.CenterStart) + fadeIn(),
        exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
        content = content
    )
}


private fun usageColor(fraction: Float): androidx.compose.ui.graphics.Color = when {
    fraction < 0.6f -> androidx.compose.ui.graphics.lerp(
        com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice,
        com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora,
        fraction / 0.6f
    )
    fraction < 0.85f -> androidx.compose.ui.graphics.Color(0xFFFFC857)
    else -> androidx.compose.ui.graphics.Color(0xFFFF5A72)
}

@Composable
private fun CryoRamMeter(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var total by androidx.compose.runtime.remember { androidx.compose.runtime.mutableDoubleStateOf(0.0) }
    var used by androidx.compose.runtime.remember { androidx.compose.runtime.mutableDoubleStateOf(0.0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                total = com.russoviller.cryogenic.utils.platform.getTotalMemory(context).toDouble() / 1048576.0
                used = com.russoviller.cryogenic.utils.platform.getUsedMemory(context).toDouble() / 1048576.0
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    val target = if (total > 0) (used / total).toFloat().coerceIn(0f, 1f) else 0f
    val ratio by androidx.compose.animation.core.animateFloatAsState(target, label = "ram")
    val color = usageColor(ratio)
    val ice = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(13.dp),
                painter = painterResource(R.drawable.ic_memory),
                contentDescription = null,
                tint = ice
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "RAM",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                ),
                color = ice
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = color
            )
        }
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val count = 20
            val gap = 2.dp.toPx()
            val segW = (size.width - gap * (count - 1)) / count
            val lit = ratio * count
            for (i in 0 until count) {
                val x = i * (segW + gap)
                val f = ((lit - i).coerceIn(0f, 1f))
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(segW, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                )
                if (f > 0f) {
                    drawRoundRect(
                        color = usageColor(i / (count - 1f)).copy(alpha = 0.35f + 0.65f * f),
                        topLeft = Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(segW, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatGb(used) + " / " + formatGb(total) + " GB",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatGb((total - used).coerceAtLeast(0.0)) + " GB",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = ice.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatGb(mb: Double): String = String.format(java.util.Locale.US, "%.1f", mb / 1024.0)

