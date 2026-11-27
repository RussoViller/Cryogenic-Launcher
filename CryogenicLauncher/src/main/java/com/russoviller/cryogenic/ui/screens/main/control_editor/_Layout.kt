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

package com.russoviller.cryogenic.ui.screens.main.control_editor

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.colorpicker.components.TransparentChecker
import com.russoviller.colorpicker.rememberColorPickerController
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.ColorPickerDialog
import com.russoviller.cryogenic.ui.components.DefaultSwitch
import com.russoviller.cryogenic.ui.components.LittleTextLabel
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleTextSlider
import com.russoviller.cryogenic.ui.components.SliderValueEditDialog
import com.russoviller.cryogenic.ui.screens.content.elements.DisabledAlpha
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween


@Composable
fun InfoLayoutSliderItem(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    decimalFormat: String = "#0.00",
    suffix: String? = null,
    fineTuningControl: Boolean = true,
    fineTuningStep: Float = 0.5f,
    enabled: Boolean = true,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
) {
    var showValueEditDialog by remember { mutableStateOf(false) }

    InfoLayoutItem(
        modifier = modifier,
        onClick = {},
        enabled = enabled,
        color = color,
        contentColor = contentColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CryoModuleTitle(title)
            SimpleTextSlider(
                modifier = Modifier.fillMaxWidth(),
                shorter = false,
                value = value,
                decimalFormat = decimalFormat,
                enabled = enabled,
                onValueChange = onValueChange,
                valueRange = valueRange,
                onValueChangeFinished = onValueChangeFinished,
                onTextClick = { showValueEditDialog = true },
                suffix = suffix,
                fineTuningControl = fineTuningControl,
                fineTuningStep = fineTuningStep
            )
        }
    }

    if (showValueEditDialog) {
        SliderValueEditDialog(
            onDismissRequest = { showValueEditDialog = false },
            title = title,
            valueRange = valueRange,
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = {
                onValueChangeFinished?.invoke()
            }
        )
    }
}

/**
 * 列表信息设置项
 */
@Composable
fun <E> InfoLayoutListItem(
    modifier: Modifier = Modifier,
    title: String,
    items: List<E>,
    selectedItem: E,
    onItemSelected: (E) -> Unit,
    getItemText: @Composable (E) -> String,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    maxListHeight: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = cryoModuleShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (expanded) Modifier.cryoGlow(12.dp, selected = true, strength = 0.6f) else Modifier)
            .clip(shape)
            .background(cryoModuleFill(expanded))
            .border(1.dp, cryoModuleBorder(expanded), shape)
    ) {
        InfoListLayoutHeader(
            modifier = Modifier.fillMaxWidth(),
            items = items,
            title = title,
            selectedItemLayout = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .cryoBackground(cryoGradient())
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    MarqueeText(
                        text = getItemText(selectedItem),
                        style = MaterialTheme.typography.labelMedium.copy(
                            shadow = null,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                        ),
                        color = CryoGlassColors.Deep
                    )
                }
            },
            expanded = expanded,
            onClick = { expanded = !expanded }
        )

        if (items.isNotEmpty()) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = getAnimateTween()),
                exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { item ->
                        val isSelected = selectedItem == item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.30f), CryoGlassColors.Aurora.copy(alpha = 0.10f)))
                                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.Transparent))
                                )
                                .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (isSelected) 0.8f else 0.12f), RoundedCornerShape(10.dp))
                                .clickable {
                                    if (selectedItem != item) onItemSelected(item)
                                    expanded = false
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .rotate(45f)
                                    .background(if (isSelected) CryoGlassColors.Ice else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                    .border(1.5.dp, CryoGlassColors.Ice.copy(alpha = if (isSelected) 1f else 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            )
                            MarqueeText(
                                modifier = Modifier.weight(1f),
                                text = getItemText(item),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else CryoGlassColors.Frost
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <E> InfoListLayoutHeader(
    modifier: Modifier = Modifier,
    items: List<E>,
    title: String,
    selectedItemLayout: @Composable RowScope.() -> Unit,
    expanded: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .drawBehind { drawCryoModuleMarker(expanded) }
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CryoModuleTitle(title, modifier = Modifier.weight(1f, fill = false))
        Row(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            selectedItemLayout()
        }

        if (!items.isEmpty()) {
            val rotation by animateFloatAsState(
                targetValue = if (expanded) -180f else 0f,
                animationSpec = getAnimateTween()
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation),
                    painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                    contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse),
                    tint = CryoGlassColors.Ice
                )
            }
        }
    }
}

fun cryoModuleShape(cut: Dp): Shape = androidx.compose.foundation.shape.RoundedCornerShape(cut + 4.dp)

@Composable
private fun cryoModuleFill(active: Boolean): Brush {
    val ice = CryoGlassColors.Ice
    return Brush.linearGradient(
        listOf(ice.copy(alpha = if (active) 0.22f else 0.07f), CryoGlassColors.Navy.copy(alpha = 0.45f), CryoGlassColors.Deep.copy(alpha = 0.62f))
    )
}

@Composable
private fun cryoModuleBorder(active: Boolean): Brush {
    val ice = CryoGlassColors.Ice
    return Brush.linearGradient(
        listOf(ice.copy(alpha = if (active) 0.95f else 0.32f), ice.copy(alpha = 0.06f), CryoGlassColors.Aurora.copy(alpha = if (active) 0.6f else 0.18f))
    )
}

/** Barrinha de destaque à esquerda (acende quando ativo). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCryoModuleMarker(active: Boolean) {
    val w = 3.dp.toPx()
    val h = (size.height * 0.46f).coerceAtLeast(12.dp.toPx())
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(CryoGlassColors.Ice.copy(alpha = if (active) 1f else 0.45f), CryoGlassColors.Aurora.copy(alpha = if (active) 0.9f else 0.25f)),
            startY = (size.height - h) / 2f,
            endY = (size.height + h) / 2f
        ),
        topLeft = androidx.compose.ui.geometry.Offset(7.dp.toPx(), (size.height - h) / 2f),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2f)
    )
}

@Composable
private fun CryoModuleTitle(title: String, modifier: Modifier = Modifier) {
    MarqueeText(
        modifier = modifier,
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp)
        ),
        color = CryoGlassColors.Frost
    )
}

@Composable
fun InfoLayoutSwitchItem(
    modifier: Modifier = Modifier,
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor()
) {
    InfoLayoutItem(
        modifier = modifier,
        onClick = {
            onValueChange(!value)
        },
        enabled = enabled,
        color = color,
        contentColor = contentColor
    ) {
        MarqueeText(
            modifier = Modifier
                .alpha(if (enabled) 1f else DisabledAlpha)
                .weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        DefaultSwitch(
            checked = value,
            onCheckedChange = onValueChange,
            enabled = enabled
        )
    }
}

@Composable
fun <E> InfoLayoutSelectItem(
    modifier: Modifier = Modifier,
    title: String,
    options: List<E>,
    current: E,
    onClick: (E) -> Unit,
    label: @Composable (E) -> Unit,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor()
) {
    InfoLayoutItem(
        modifier = modifier,
        onClick = {},
        color = color,
        contentColor = contentColor
    ) {
        MarqueeText(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CryoGlassColors.Deep.copy(alpha = 0.45f))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEach { option ->
                val isSelected = current == option
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .cryoBackground(
                            if (isSelected) cryoGradient()
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onClick(option) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides if (isSelected) CryoGlassColors.Deep else CryoGlassColors.Frost
                    ) {
                        label(option)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoLayoutTextItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    selected: Boolean = false,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    enabled: Boolean = true,
) {
    InfoLayoutTextItem(
        modifier = modifier,
        title = title,
        icon = {
            if (showArrow) {
                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    painter = painterResource(R.drawable.ic_arrow_right_rounded),
                    contentDescription = null
                )
            }
        },
        onClick = onClick,
        selected = selected,
        color = color,
        contentColor = contentColor,
        enabled = enabled,
    )
}

@Composable
fun InfoLayoutTextItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    selected: Boolean = false,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    enabled: Boolean = true,
) {
    InfoLayoutItem(
        modifier = modifier,
        onClick = onClick,
        selected = selected,
        color = color,
        contentColor = contentColor,
        enabled = enabled,
    ) {
        MarqueeText(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        icon()
    }
}

@Composable
fun InfoLayoutColorItem(
    modifier: Modifier = Modifier,
    title: String,
    color: Color,
    onColorChanged: (Color) -> Unit
) {
    var showColorDialog by remember { mutableStateOf(false) }

    InfoLayoutTextItem(
        modifier = modifier,
        title = title,
        icon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(28.dp)) {
                    TransparentChecker(
                        modifier = Modifier.fillMaxSize(),
                        gridSize = 18f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = color)
                    )
                }

                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    painter = painterResource(R.drawable.ic_arrow_right_rounded),
                    contentDescription = null
                )
            }
        },
        onClick = {
            showColorDialog = true
        }
    )

    if (showColorDialog) {
        var tempColor by remember { mutableStateOf(color) }
        val colorController = rememberColorPickerController(initialColor = tempColor)

        val currentColor by remember(colorController) { colorController.color }

        LaunchedEffect(currentColor) {
            onColorChanged(currentColor)
        }

        ColorPickerDialog(
            colorController = colorController,
            onCancel = {
                onColorChanged(colorController.getOriginalColor())
                showColorDialog = false
            },
            onConfirm = { color ->
                showColorDialog = false
                onColorChanged(color)
            }
        )
    }
}

@Composable
fun InfoLayoutItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    borderColor: Color = CryoGlassColors.Ice,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    content: @Composable RowScope.() -> Unit
) {
    val itemShape = cryoModuleShape(12.dp)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (selected) Modifier.cryoGlow(12.dp, selected = true) else Modifier)
                .clip(itemShape)
                .background(cryoModuleFill(selected))
                .border(if (selected) 1.5.dp else 1.dp, cryoModuleBorder(selected), itemShape)
                .drawBehind { drawCryoModuleMarker(selected) }
                .clickable(enabled = enabled, onClick = onClick)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
