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

package com.russoviller.cryogenic.ui.components

import com.russoviller.cryogenic.ui.theme.addCryoRoundedPolygon
import com.russoviller.cryogenic.ui.theme.cryoRoundedPolygonPath
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.defaultMinSize
import com.russoviller.cryogenic.setting.AllSettings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.annotation.IntRange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.screens.content.elements.DisabledAlpha
import com.russoviller.cryogenic.utils.math.addBigDecimal
import com.russoviller.cryogenic.utils.math.subtractBigDecimal
import java.text.DecimalFormat

/**
 * 简单的文本滑动条，支持实时显示当前滑动条的数值，支持显示自定义数值的单位
 * @param shorter 是否使用更短的指示器的滑动条
 */
@Composable
fun SimpleTextSlider(
    modifier: Modifier = Modifier,
    shorter: Boolean = false,
    value: Float,
    decimalFormat: String = "#0.00",
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    toInt: Boolean = false,
    suffix: String? = null,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    fineTuningControl: Boolean = false,
    fineTuningStep: Float = 0.5f,
    valueText: ((Float) -> String)? = null,
    appendContent: @Composable () -> Unit = {}
) {
    val formatter = remember(decimalFormat) { DecimalFormat(decimalFormat) }
    fun getTextString(value: Float) = valueText?.invoke(value) ?: "${if (toInt) {
        value.toInt()
    } else {
        formatter.format(value)
    }}"

    fun changeValue(newValue: Float, finished: Boolean) {
        onValueChange(newValue)
        if (finished) onValueChangeFinished?.invoke()
    }

    LaunchedEffect(Unit) {
        //检查值是否被刻意的修改为超出范围
        if (value !in valueRange) {
            val newValue = value.coerceIn(valueRange)
            //调回范围内
            changeValue(newValue, true)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CryoSlider(
            value = value,
            enabled = enabled,
            onValueChange = { changeValue(it, false) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            compact = shorter,
            tickLabel = { getTextString(it) + (suffix ?: "") },
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .alpha(alpha = if (enabled) 1f else DisabledAlpha)
                .padding(start = 12.dp)
                .align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            if (fineTuningControl) {
                CryoStepButton(R.drawable.ic_arrow_left_rounded, enabled) {
                    val newValue = value.subtractBigDecimal(fineTuningStep)
                    if (newValue <= valueRange.start) {
                        changeValue(valueRange.start, true)
                    } else {
                        changeValue(newValue, true)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = if (shorter) 44.dp else 56.dp)
                    .then(
                        if (onTextClick != null) {
                            Modifier.clickable(enabled = enabled, onClick = onTextClick)
                        } else Modifier
                    ),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                Text(
                    text = getTextString(value),
                    style = (if (shorter) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall).copy(
                        brush = Brush.verticalGradient(listOf(Color.White, CryoGlassColors.Ice)),
                        fontWeight = FontWeight.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    maxLines = 1
                )
                suffix?.let { text ->
                    Text(
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        color = CryoGlassColors.Ice.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
            if (fineTuningControl) {
                CryoStepButton(R.drawable.ic_arrow_right_rounded, enabled) {
                    val newValue = value.addBigDecimal(fineTuningStep)
                    if (newValue >= valueRange.endInclusive) {
                        changeValue(valueRange.endInclusive, true)
                    } else {
                        changeValue(newValue, true)
                    }
                }
            }
        }
        appendContent()
    }
}

@Composable
private fun CryoStepButton(iconRes: Int, enabled: Boolean, onClick: () -> Unit) {
    val hex = androidx.compose.foundation.shape.GenericShape { size, _ ->
        val pts = listOf(
            Offset(size.width * 0.5f, 0f),
            Offset(size.width, size.height * 0.25f),
            Offset(size.width, size.height * 0.75f),
            Offset(size.width * 0.5f, size.height),
            Offset(0f, size.height * 0.75f),
            Offset(0f, size.height * 0.25f)
        )
        addCryoRoundedPolygon(pts, List(6) { size.minDimension * 0.22f })
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(width = 28.dp, height = 31.dp)
            .clip(hex)
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.22f), CryoGlassColors.Deep.copy(alpha = 0.9f))))
            .border(1.5.dp, Brush.verticalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Ice.copy(alpha = 0.25f))), hex)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
    }
}

@Composable
fun IndicatorSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
    onDragStateChange: ((Boolean) -> Unit)? = null
) {
    CryoSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        compact = true,
        onValueChangeFinished = onValueChangeFinished,
        onDragStateChange = onDragStateChange
    )
}

@Composable
fun CryoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    compact: Boolean = false,
    tickLabel: ((Float) -> String)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    onDragStateChange: ((Boolean) -> Unit)? = null
) {
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentOnDragState by rememberUpdatedState(onDragStateChange)
    DisposableEffect(Unit) {
        onDispose { currentOnDragState?.invoke(false) }
    }
    val currentValue by rememberUpdatedState(value)
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    fun fromFraction(f: Float): Float {
        val clamped = f.coerceIn(0f, 1f)
        val snapped = if (steps > 0) {
            val segments = steps + 1
            kotlin.math.round(clamped * segments) / segments
        } else clamped
        return valueRange.start + snapped * span
    }
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val ice = CryoGlassColors.Ice
    val iceSoft = CryoGlassColors.IceSoft
    val aurora = CryoGlassColors.Aurora
    val deep = CryoGlassColors.Deep
    val navy = CryoGlassColors.Navy
    val glow = AllSettings.cryoGlowEnabled.state
    val showLabels = !compact && tickLabel != null
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = CryoGlassColors.Frost.copy(alpha = 0.6f),
        shadow = null
    )
    val labels = remember(valueRange, showLabels, tickLabel != null) {
        if (!showLabels) emptyList() else listOf(
            0f to tickLabel!!.invoke(valueRange.start),
            0.5f to tickLabel.invoke(fromFraction(0.5f)),
            1f to tickLabel.invoke(valueRange.endInclusive)
        )
    }
    Canvas(
        modifier = modifier
            .height(if (showLabels) 44.dp else 30.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val pad = 12.dp.toPx()
                    val v = fromFraction((offset.x - pad) / (size.width - pad * 2f))
                    if (v != currentValue) currentOnChange(v)
                    currentOnFinished?.invoke()
                }
            }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { currentOnDragState?.invoke(true) },
                    onDragEnd = {
                        currentOnDragState?.invoke(false)
                        currentOnFinished?.invoke()
                    },
                    onDragCancel = {
                        currentOnDragState?.invoke(false)
                        currentOnFinished?.invoke()
                    }
                ) { change, _ ->
                    change.consume()
                    val pad = 12.dp.toPx()
                    val v = fromFraction((change.position.x - pad) / (size.width - pad * 2f))
                    if (v != currentValue) currentOnChange(v)
                }
            }
    ) {
        val thumbR = 11.dp.toPx()
        val cy = if (showLabels) 16.dp.toPx() else size.height / 2f
        val startX = 12.dp.toPx()
        val usable = size.width - startX * 2f
        val x = startX + usable * fraction
        val endX = startX + usable
        val cap = 6.dp.toPx()
        val line = 1.5.dp.toPx()
        fun hexPath(cx: Float, cy0: Float, rad: Float): androidx.compose.ui.graphics.Path {
            val pts = List(6) { k ->
                val ang = Math.toRadians((60.0 * k) - 90.0)
                Offset(cx + (kotlin.math.cos(ang) * rad).toFloat(), cy0 + (kotlin.math.sin(ang) * rad).toFloat())
            }
            return cryoRoundedPolygonPath(pts, rad * 0.30f)
        }
        val leftCap = androidx.compose.ui.graphics.Path().apply {
            moveTo(startX + cap, cy - cap * 0.75f)
            lineTo(startX, cy)
            lineTo(startX + cap, cy + cap * 0.75f)
        }
        val rightCap = androidx.compose.ui.graphics.Path().apply {
            moveTo(endX - cap, cy - cap * 0.75f)
            lineTo(endX, cy)
            lineTo(endX - cap, cy + cap * 0.75f)
        }
        drawLine(Color.White.copy(alpha = 0.10f), Offset(startX + cap, cy), Offset(endX - cap, cy), line)
        drawLine(ice.copy(alpha = 0.18f), Offset(startX + cap * 2f, cy + 4.dp.toPx()), Offset(startX + usable * 0.62f, cy + 4.dp.toPx()), 1.dp.toPx())
        drawLine(ice.copy(alpha = 0.12f), Offset(startX + usable * 0.30f, cy + 7.dp.toPx()), Offset(endX - cap * 3f, cy + 7.dp.toPx()), 1.dp.toPx())
        drawPath(leftCap, ice.copy(alpha = 0.9f), style = Stroke(line))
        drawPath(rightCap, ice.copy(alpha = 0.45f), style = Stroke(line))
        if (x > startX + cap) {
            if (glow) {
                drawLine(ice.copy(alpha = 0.25f), Offset(startX + cap, cy), Offset(x, cy), 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            drawLine(
                brush = Brush.horizontalGradient(listOf(iceSoft, ice, aurora), startX = startX, endX = x),
                start = Offset(startX + cap, cy),
                end = Offset(x, cy),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, ice.copy(alpha = 0.7f)), startX = startX, endX = x),
                start = Offset(startX + cap * 2f, cy - 4.dp.toPx()),
                end = Offset(x - thumbR * 1.4f, cy - 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        val tickCount = if (steps in 1..20) steps + 1 else 10
        val tickY = cy - 9.dp.toPx()
        for (i in 0..tickCount) {
            val tx = startX + cap + (usable - cap * 2f) * i / tickCount
            val major = i == 0 || i == tickCount || i * 2 == tickCount
            val active = tx <= x
            drawLine(
                color = (if (active) ice else Color.White).copy(alpha = if (active) 0.85f else 0.22f),
                start = Offset(tx, tickY),
                end = Offset(tx, tickY - (if (major) 5.dp else 3.dp).toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        labels.forEach { (pos, text) ->
            val layout = measurer.measure(text, labelStyle)
            val lx = (startX + usable * pos - layout.size.width / 2f)
                .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
            drawText(layout, topLeft = Offset(lx, cy + 10.dp.toPx()))
        }
        if (glow) {
            drawCircle(
                brush = safeRadialGradient(listOf(ice.copy(alpha = 0.5f), Color.Transparent), center = Offset(x, cy), radius = thumbR * 2.2f),
                radius = thumbR * 2.2f,
                center = Offset(x, cy)
            )
        }
        drawPath(hexPath(x, cy, thumbR + 1.dp.toPx()), deep)
        drawPath(hexPath(x, cy, thumbR), Brush.sweepGradient(listOf(iceSoft, aurora, ice, iceSoft), center = Offset(x, cy)), style = Stroke(2.5.dp.toPx()))
        drawPath(hexPath(x, cy, thumbR * 0.55f), Brush.verticalGradient(listOf(Color.White, ice), startY = cy - thumbR, endY = cy + thumbR))
    }
}
