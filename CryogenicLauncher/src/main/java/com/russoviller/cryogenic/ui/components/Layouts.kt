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

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween

@Composable
fun ScalingLabel(
    modifier: Modifier = Modifier,
    text: String,
    influencedByBackground: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor(),
    blur: Int = AllSettings.backgroundBlur.state,
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    Surface(
        modifier = modifier.graphicsLayer { scaleY = scale.value; scaleX = scale.value },
        shape = shape,
        color = color,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .backgroundGlass(blur, color, influencedByBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun ScalingLabel(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    influencedByBackground: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = itemColor(influencedByBackground = influencedByBackground),
    contentColor: Color = onItemColor(),
) {
    ScalingLabel(
        modifier = modifier,
        onClick = onClick,
        text = {
            Text(text = text)
        },
        influencedByBackground = influencedByBackground,
        shape = shape,
        color = color,
        contentColor = contentColor,
    )
}

@Composable
fun ScalingLabel(
    modifier: Modifier = Modifier,
    text: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    influencedByBackground: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = itemColor(influencedByBackground = influencedByBackground),
    contentColor: Color = onItemColor(),
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    Surface(
        modifier = modifier.graphicsLayer { scaleY = scale.value; scaleX = scale.value },
        shape = shape,
        color = color,
        contentColor = contentColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            content = text,
        )
    }
}

@Composable
fun LittleTextLabel(
    modifier: Modifier = Modifier,
    text: String,
    singleLine: Boolean = true,
    color: Color = MaterialTheme.colorScheme.tertiary,
    contentColor: Color = MaterialTheme.colorScheme.onTertiary,
    shape: Shape = MaterialTheme.shapes.large,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium
) {
    //Visual próprio do Cryogenic; vermelho para avisos (errorContainer), gelo para o resto
    val scheme = MaterialTheme.colorScheme
    val accent = when (color) {
        scheme.tertiary -> CryoGlassColors.Ice
        scheme.errorContainer, scheme.error -> Color(0xFFFF5C7A)
        else -> color
    }
    CryoTag(
        text = text,
        modifier = modifier,
        accent = accent,
        fontSize = if (textStyle.fontSize.value > 11f) 11.sp else textStyle.fontSize,
        marquee = singleLine
    )
}

@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    itemName: String,
    summary: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    CryoOptionTile(
        modifier = modifier,
        selected = selected,
        square = false,
        itemName = itemName,
        summary = summary,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    itemName: String,
    summary: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    CryoOptionTile(
        modifier = modifier,
        selected = checked,
        square = true,
        itemName = itemName,
        summary = summary,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) }
    )
}

@Composable
private fun CryoOptionTile(
    modifier: Modifier,
    selected: Boolean,
    square: Boolean,
    itemName: String,
    summary: (@Composable () -> Unit)?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    val ice = CryoGlassColors.Ice
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (selected) Modifier.cryoGlow(14.dp, selected = true) else Modifier)
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    if (selected) listOf(ice.copy(alpha = 0.24f), CryoGlassColors.Aurora.copy(alpha = 0.08f))
                    else listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.02f))
                )
            )
            .border(if (selected) 1.5.dp else 1.dp, if (selected) cryoBorderBrush(1.4f) else androidx.compose.ui.graphics.SolidColor(ice.copy(alpha = 0.18f)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val markShape = if (square) androidx.compose.foundation.shape.RoundedCornerShape(6.dp) else androidx.compose.foundation.shape.CircleShape
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(markShape)
                .cryoBackground(
                    if (selected) cryoGradient()
                    else androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent))
                )
                .border(1.5.dp, ice.copy(alpha = if (selected) 0f else 0.6f), markShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = if (selected) androidx.compose.ui.graphics.Color.White else CryoGlassColors.Frost
            )
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides CryoGlassColors.Frost.copy(alpha = 0.75f)
            ) {
                summary?.invoke()
            }
        }
    }
}

data class IDItem(val id: String, val title: String)

@Composable
fun SliderValueEditDialog(
    onDismissRequest: () -> Unit,
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    intCheck: Boolean = false
) {
    CryoNumberDialog(
        title = title,
        valueRange = valueRange,
        value = value,
        isInt = intCheck,
        onDismissRequest = onDismissRequest,
        onConfirm = { newValue ->
            onValueChange(newValue)
            onValueChangeFinished()
        }
    )
}

@Composable
fun TitleAndSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = titleStyle
        )
        summary?.let { text ->
            Text(
                modifier = Modifier.alpha(0.7f),
                text = text,
                style = summaryStyle
            )
        }
    }
}

@Composable
fun FocusableBox(
    modifier: Modifier = Modifier,
    requestKey: Any? = null,
    canRequestFocus: () -> Boolean = { true }
) {
    val focusRequester = remember { FocusRequester() }
    val currentCanRequestFocus by rememberUpdatedState(canRequestFocus)

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable(enabled = true)
    )

    LaunchedEffect(requestKey) {
        if (currentCanRequestFocus()) {
            focusRequester.requestFocus()
        }
    }
}
