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

package com.russoviller.cryogenic.ui.screens.content.settings.layouts

import com.russoviller.cryogenic.ui.components.cryoBackground

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.unit.EnumSettingUnit
import com.russoviller.cryogenic.setting.unit.StringListSettingUnit
import com.russoviller.cryogenic.setting.unit.StringSettingUnit
import com.russoviller.cryogenic.ui.components.IDItem
import com.russoviller.cryogenic.ui.components.SimpleListItem
import com.russoviller.cryogenic.ui.components.TitleAndSummary
import com.russoviller.cryogenic.ui.screens.content.elements.DisabledAlpha
import com.russoviller.cryogenic.utils.animation.getAnimateTween

@Composable
private fun <E> BaseListSettingsCard(
    items: List<E>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    title: String,
    position: CardPosition,
    itemLayout: @Composable (E) -> Unit,
    modifier: Modifier = Modifier,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    summary: String? = null,
    middleLayout: (@Composable ColumnScope.() -> Unit)? = null,
    trailingIcon: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    require(items.isNotEmpty()) { "Items list cannot be empty" }

    LaunchedEffect(enabled) {
        if (!enabled) onExpandChange(false)
    }

    SettingsCard(
        modifier = modifier,
        position = position,
        outerShape = outerShape,
        innerShape = innerShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha = if (enabled) 1f else DisabledAlpha)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onExpandChange(!expanded) }
                        .padding(all = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) innerColumn@ {
                        TitleAndSummary(
                            title = title,
                            summary = summary,
                            titleStyle = titleStyle,
                            summaryStyle = summaryStyle
                        )
                        middleLayout?.invoke(this@innerColumn)
                    }
                    trailingIcon?.let { trailing ->
                        Row(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            content = trailing
                        )
                    }
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) -180f else 0f,
                        animationSpec = getAnimateTween()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .cryoBackground(
                                if (expanded) com.russoviller.cryogenic.ui.components.cryoGradient()
                                else androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(
                                        com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.12f),
                                        com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .border(1.dp, com.russoviller.cryogenic.ui.components.cryoBorderBrush(), CircleShape)
                            .clickable(enabled = enabled) { onExpandChange(!expanded) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.rotate(rotation),
                            painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                            contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse),
                            tint = if (expanded) com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep else com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxWidth(),
                        visible = expanded,
                        enter = expandVertically(animationSpec = getAnimateTween()),
                        exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { item ->
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            itemLayout(item)
                                        }
                                    }
                                    if (row.size == 1) Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <E> ListSettingsCard(
    items: List<E>,
    currentId: String,
    defaultId: String,
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    summary: String? = null,
    getItemText: @Composable (E) -> String,
    getItemId: (E) -> String,
    getItemSummary: (@Composable (E) -> Unit)? = null,
    trailingIcon: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    autoCollapse: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
    onValueChange: (E) -> Unit = {},
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    require(items.isNotEmpty()) { "Items list cannot be empty" }

    var selectedItem by remember(currentId) {
        mutableStateOf(
            items.firstOrNull { getItemId(it) == currentId }
                ?: items.firstOrNull { getItemId(it) == defaultId }
                ?: items.first()
        )
    }
    var expanded by remember { mutableStateOf(false) }

    if (items.size <= 8 && getItemSummary == null) {
        SettingsCard(
            modifier = modifier,
            position = position,
            outerShape = outerShape,
            innerShape = innerShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else DisabledAlpha)
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TitleAndSummary(
                        modifier = Modifier.weight(1f),
                        title = title,
                        summary = summary,
                        titleStyle = titleStyle,
                        summaryStyle = summaryStyle
                    )
                    trailingIcon?.let { trailing -> Row(content = trailing) }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        CryoChoiceChip(
                            text = getItemText(item),
                            selected = getItemId(selectedItem) == getItemId(item),
                            enabled = enabled,
                            onClick = {
                                if (getItemId(selectedItem) != getItemId(item)) {
                                    selectedItem = item
                                    onValueChange(item)
                                }
                            }
                        )
                    }
                }
            }
        }
        return
    }

    BaseListSettingsCard(
        items = items,
        expanded = expanded,
        onExpandChange = { expanded = it },
        title = title,
        position = position,
        itemLayout = { item ->
            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                selected = getItemId(selectedItem) == getItemId(item),
                itemName = getItemText(item),
                summary = getItemSummary?.let {
                    { it.invoke(item) }
                },
                onClick = {
                    if (expanded && getItemId(selectedItem) != getItemId(item)) {
                        selectedItem = item
                        onValueChange(item)
                        if (autoCollapse) expanded = false
                    }
                }
            )
        },
        modifier = modifier,
        outerShape = outerShape,
        innerShape = innerShape,
        summary = summary,
        middleLayout = {
            CryoSelectedChip(getItemText(selectedItem))
        },
        trailingIcon = trailingIcon,
        enabled = enabled,
        itemListPadding = itemListPadding,
        titleStyle = titleStyle,
        summaryStyle = summaryStyle
    )
}

@Composable
fun <E> ListSettingsCard(
    unit: StringSettingUnit,
    items: List<E>,
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    summary: String? = null,
    getItemText: @Composable (E) -> String,
    getItemId: (E) -> String,
    getItemSummary: (@Composable (E) -> Unit)? = null,
    trailingIcon: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    autoCollapse: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
    onValueChange: (E) -> Unit = {},
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    ListSettingsCard(
        modifier = modifier,
        items = items,
        currentId = unit.state,
        defaultId = unit.defaultValue,
        position = position,
        outerShape = outerShape,
        innerShape = innerShape,
        title = title,
        summary = summary,
        getItemText = getItemText,
        getItemId = getItemId,
        getItemSummary = getItemSummary,
        trailingIcon = trailingIcon,
        enabled = enabled,
        autoCollapse = autoCollapse,
        itemListPadding = itemListPadding,
        onValueChange = { item ->
            unit.save(getItemId(item))
            onValueChange(item)
        },
        titleStyle = titleStyle,
        summaryStyle = summaryStyle
    )
}

@Composable
fun <E: Enum<E>> ListSettingsCard(
    unit: EnumSettingUnit<E>,
    items: List<E>,
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    summary: String? = null,
    getItemText: @Composable (E) -> String,
    getItemSummary: (@Composable (E) -> Unit)? = null,
    enabled: Boolean = true,
    autoCollapse: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
    onValueChange: (E) -> Unit = {},
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    ListSettingsCard(
        modifier = modifier,
        items = items,
        currentId = unit.state.name,
        defaultId = unit.defaultValue.name,
        position = position,
        outerShape = outerShape,
        innerShape = innerShape,
        title = title,
        summary = summary,
        getItemText = getItemText,
        getItemId = { it.name },
        getItemSummary = getItemSummary,
        enabled = enabled,
        autoCollapse = autoCollapse,
        itemListPadding = itemListPadding,
        onValueChange = { item ->
            unit.save(item)
            onValueChange(item)
        },
        titleStyle = titleStyle,
        summaryStyle = summaryStyle
    )
}

@Composable
fun <E> StringListSettingsCard(
    unit: StringListSettingUnit,
    items: List<E>,
    onItemsChange: List<String>.(check: Boolean, item: E) -> List<String>,
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    summary: String? = null,
    getItemID: (E) -> String,
    getItemText: @Composable (E) -> String,
    getItemSummary: (@Composable (E) -> Unit)? = null,
    getItemCheck: (contains: Boolean) -> Boolean = { it },
    trailingIcon: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    require(items.isNotEmpty()) { "Items list cannot be empty" }

    var expanded by remember { mutableStateOf(false) }

    if (items.size <= 8 && getItemSummary == null) {
        SettingsCard(
            modifier = modifier,
            position = position,
            outerShape = outerShape,
            innerShape = innerShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else DisabledAlpha)
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TitleAndSummary(
                        modifier = Modifier.weight(1f),
                        title = title,
                        summary = summary,
                        titleStyle = titleStyle,
                        summaryStyle = summaryStyle
                    )
                    trailingIcon?.let { trailing -> Row(content = trailing) }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val listState = unit.state
                    items.forEach { item ->
                        val checked = getItemCheck(getItemID(item) in listState)
                        CryoChoiceChip(
                            text = getItemText(item),
                            selected = checked,
                            enabled = enabled,
                            onClick = {
                                unit.save(listState.onItemsChange(!checked, item))
                            }
                        )
                    }
                }
            }
        }
        return
    }

    BaseListSettingsCard(
        items = items,
        expanded = expanded,
        onExpandChange = { expanded = it },
        title = title,
        position = position,
        itemLayout = { item ->
            val itemID = getItemID(item)
            val listState = unit.state
            val contains = itemID in listState

            SimpleListItem(
                modifier = Modifier.fillMaxWidth(),
                checked = getItemCheck(contains),
                onCheckedChange = { value ->
                    unit.save(
                        listState.onItemsChange(value, item)
                    )
                },
                itemName = getItemText(item),
                summary = getItemSummary?.let {
                    { it.invoke(item) }
                }
            )
        },
        trailingIcon = trailingIcon,
        modifier = modifier,
        outerShape = outerShape,
        innerShape = innerShape,
        summary = summary,
        enabled = enabled,
        itemListPadding = itemListPadding,
        titleStyle = titleStyle,
        summaryStyle = summaryStyle
    )
}

@Composable
fun SimpleIDListCard(
    items: List<IDItem>,
    currentId: String,
    defaultId: String,
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(bottom = 4.dp),
    onValueChange: (IDItem) -> Unit = {}
) {
    ListSettingsCard(
        modifier = modifier,
        position = position,
        items = items,
        currentId = currentId,
        defaultId = defaultId,
        title = title,
        summary = summary,
        getItemText = { it.title },
        getItemId = { it.id },
        enabled = enabled,
        itemListPadding = itemListPadding,
        onValueChange = onValueChange
    )
}

@Composable
fun CryoChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Text(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(CryoGlassColors.IceSoft, CryoGlassColors.Aurora)
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))
                    )
                }
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.35f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.Medium,
        color = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost,
        maxLines = 1
    )
}

@Composable
fun CryoSelectedChip(text: String) {
    Row(
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.14f))
            .border(1.dp, com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice,
            maxLines = 1
        )
    }
}
