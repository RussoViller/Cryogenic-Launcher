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

package com.russoviller.cryogenic.ui.screens.content.download.assets.elements

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoChamferTile
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.layer_controller.utils.animateShapeAsState
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.download.assets.platform.PlatformDisplayLabel
import com.russoviller.cryogenic.game.download.assets.platform.PlatformFilterCode
import com.russoviller.cryogenic.game.download.assets.platform.PlatformSortField
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.LittleTextLabel
import com.russoviller.cryogenic.ui.components.OwnOutlinedTextField
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import org.jackhuang.hmcl.addon.AddonProjectInfo

/**
 * 搜索资源过滤器UI
 * @param enablePlatform 是否允许更改目标平台
 * @param searchPlatform 目标平台
 * @param searchName 搜索名称
 * @param searchedMcMods 搜索得到的 MCMOD 项目
 * @param searchedVersions 搜索得到的Minecraft版本号
 * @param gameVersion 游戏版本
 * @param sortField 排序方式
 * @param allCategories 可用资源类别列表
 * @param categories 已选择的资源类别
 * @param enableModLoader 是否启用模组加载器过滤
 * @param modloaders 可用模组加载器列表
 * @param modloader 模组加载器
 * @param onModLoaderChange 模组加载器变更时
 * @param extraFilter 额外的过滤器UI
 */
@Composable
fun SearchFilter(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    enablePlatform: Boolean = true,
    searchPlatform: Platform,
    onPlatformChange: (Platform) -> Unit = {},
    searchName: String,
    onSearchNameChange: (String) -> Unit = {},
    onSearch: () -> Unit,
    searchedMcMods: List<AddonProjectInfo>,
    searchedVersions: List<String>,
    gameVersion: String,
    onGameVersionChange: (String) -> Unit = {},
    sortField: PlatformSortField,
    onSortFieldChange: (PlatformSortField) -> Unit = {},
    allCategories: List<PlatformFilterCode>,
    categories: List<PlatformFilterCode>,
    onCategoryChanged: (List<PlatformFilterCode>) -> Unit = {},
    enableModLoader: Boolean = true,
    modloaders: List<PlatformDisplayLabel> = emptyList(),
    modloader: PlatformDisplayLabel? = null,
    onModLoaderChange: (PlatformDisplayLabel?) -> Unit = {},
    extraFilter: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SuggestionsText(
                value = searchName,
                onValueChange = onSearchNameChange,
                label = stringResource(R.string.download_assets_filter_search_name),
                onSearch = onSearch,
                suggestions = searchedMcMods,
                suggestionLabel = { item ->
                    Text(
                        item.mainName(),
                        style = MaterialTheme.typography.labelMedium
                    )
                    //英文名/次要名称
                    if (item.subName().isNotBlank()) {
                        Text(
                            modifier = Modifier.alpha(0.7f),
                            text = item.subName(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                onSuggestionClick = { item ->
                    onSearchNameChange(
                        item.subName().ifEmpty { item.abbrName() }
                    )
                    onSearch()
                    onSearch()
                }
            )
        }

        item {
            SuggestionsText(
                value = gameVersion,
                onValueChange = onGameVersionChange,
                label = stringResource(R.string.download_assets_filter_game_version),
                onSearch = onSearch,
                suggestions = searchedVersions,
                suggestionLabel = { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                onSuggestionClick = { item ->
                    onGameVersionChange(item)
                    onSearch()
                }
            )
        }

        extraFilter?.invoke(this@LazyColumn)

        if (enablePlatform) {
            item {
                PlatformListLayout(
                    modifier = Modifier.fillMaxWidth(),
                    searchPlatform = searchPlatform,
                    onPlatformChange = onPlatformChange,
                )
            }
        }

        item {
            FilterListLayout(
                modifier = Modifier.fillMaxWidth(),
                items = PlatformSortField.entries,
                selectionMode = FilterSelectionMode.Single,
                selectedItems = listOfNotNull(sortField),
                onSelectionChange = { new ->
                    new.first().takeIf { it != sortField }?.let { value ->
                        onSortFieldChange(value)
                    }
                },
                getItemLabel = { item ->
                    stringResource(item.getDisplayName())
                },
                title = stringResource(R.string.sort_by),
                cancelable = false
            )
        }

        item {
            FilterListLayout(
                modifier = Modifier.fillMaxWidth(),
                items = allCategories,
                selectionMode = FilterSelectionMode.Multiple,
                selectedItems = categories,
                onSelectionChange = { news ->
                    news.takeIf { it.toSet() != categories.toSet() }?.let { value ->
                        onCategoryChanged(value)
                    }
                },
                getItemLabel = { item ->
                    stringResource(item.getDisplayName())
                },
                title = stringResource(R.string.download_assets_filter_category)
            )
        }

        if (enableModLoader) {
            item {
                FilterListLayout(
                    modifier = Modifier.fillMaxWidth(),
                    items = modloaders,
                    selectionMode = FilterSelectionMode.Single,
                    selectedItems = listOfNotNull(modloader),
                    onSelectionChange = { new ->
                        val value = new.firstOrNull()
                        if (value != modloader) onModLoaderChange(value)
                    },
                    getItemLabel = { item ->
                        item.getDisplayName()
                    },
                    title = stringResource(R.string.download_assets_filter_modloader)
                )
            }
        }
    }
}



enum class FilterSelectionMode {
    /**
     * 一次性只能选择一个项
     */
    Single,

    /**
     * 支持选择更多项
     */
    Multiple
}

@Composable
private fun <E> SuggestionsText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onSearch: () -> Unit,
    suggestions: List<E>,
    suggestionLabel: @Composable FlowRowScope.(E) -> Unit,
    onSuggestionClick: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val showSuggestions = isFocused && suggestions.isNotEmpty()
    ExposedDropdownMenuBox(
        expanded = showSuggestions,
        onExpandedChange = {
            focusManager.clearFocus(false)
        }
    ) {
        val fieldShape by animateShapeAsState(
            if (showSuggestions) RoundedCornerShape(
                topStart = 16.0.dp, topEnd = 16.0.dp,
                bottomStart = 0.dp, bottomEnd = 0.dp
            )
            else RoundedCornerShape(16.0.dp)
        )
        OwnOutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            value = value,
            onValueChange = onValueChange,
            shape = fieldShape,
            label = {
                Text(text = label)
            },
            trailingIcon = {
                IconButton(
                    onClick = onSearch
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.generic_search)
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                }
            ),
            interactionSource = interactionSource
        )

        ExposedDropdownMenu(
            expanded = showSuggestions,
            onDismissRequest = {
                focusManager.clearFocus(false)
            },
            shape = RoundedCornerShape(
                topStart = 0.dp, topEnd = 0.dp,
                bottomStart = 16.0.dp, bottomEnd = 16.0.dp,
            ),
            containerColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.97f),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.55f)),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            suggestions.forEach { item ->
                DropdownMenuItem(
                    text = {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            suggestionLabel(item)
                        }
                    },
                    onClick = {
                        onSuggestionClick(item)
                        focusManager.clearFocus(false)
                    }
                )
            }
        }
    }
}

/**
 * 基础过滤器UI，已经配置好合适的颜色和形状
 */
@Composable
fun BaseFilterLayout(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides CryoGlassColors.Frost) {
        Column(
            modifier = modifier
                .cryoChamferTile(12.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 10.dp)
        ) { content() }
    }
}

/**
 * 列表过滤器UI
 */
@Composable
fun <E> FilterListLayout(
    title: String,
    items: List<E>,
    selectionMode: FilterSelectionMode,
    selectedItems: List<E>,
    onSelectionChange: (List<E>) -> Unit,
    getItemLabel: @Composable (E) -> String,
    modifier: Modifier = Modifier,
    selectedLabel: @Composable FlowRowScope.(E) -> Unit = { item ->
        LittleTextLabel(
            text = getItemLabel(item),
            shape = MaterialTheme.shapes.small
        )
    },
    itemLayout: @Composable (E) -> Unit = { item ->
        Text(
            text = getItemLabel(item),
            style = MaterialTheme.typography.labelMedium
        )
    },
    cancelable: Boolean = true,
    maxListHeight: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(false) }

    val selected = selectedItems.isNotEmpty()
    val isSingle = selectionMode == FilterSelectionMode.Single

    BaseFilterLayout(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FilterHeader(
                title = title,
                expanded = expanded,
                selected = selected,
                selectedLabels = {
                    if (selectedItems.isEmpty()) {
                        LittleTextLabel(
                            text = stringResource(R.string.download_assets_filter_none),
                            shape = MaterialTheme.shapes.small
                        )
                    } else {
                        selectedItems.forEach { item ->
                            selectedLabel(item)
                        }
                    }
                },
                cancelable = cancelable,
                onExpandToggle = { expanded = !expanded },
                onClear = { onSelectionChange(emptyList()) }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = getAnimateTween()),
                exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight + 120.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 6.dp, end = 8.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { item ->
                        val isSelected = selectedItems.contains(item)
                        FilterListItem(
                            selected = isSelected,
                            selectionMode = selectionMode,
                            onCheckedChange = { checked ->
                                val newSelection = when {
                                    isSingle && checked -> listOf(item)
                                    !isSingle && checked -> selectedItems + item
                                    !isSingle && !checked -> selectedItems - item
                                    else -> emptyList()
                                }
                                onSelectionChange(newSelection)
                            },
                            itemLayout = {
                                itemLayout(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformListLayout(
    searchPlatform: Platform,
    onPlatformChange: (Platform) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterListLayout(
        modifier = modifier,
        items = Platform.available,
        selectionMode = FilterSelectionMode.Single,
        selectedItems = listOfNotNull(searchPlatform),
        onSelectionChange = { new ->
            new.first().takeIf { it != searchPlatform }?.let { value ->
                onPlatformChange(value)
            }
        },
        getItemLabel = { item ->
            item.displayName
        },
        selectedLabel = { item ->
            PlatformIdentifier(
                platform = item,
                shape = MaterialTheme.shapes.small
            )
        },
        itemLayout = { platform ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(platform.getDrawable()),
                    contentDescription = platform.displayName
                )
                Text(
                    text = platform.displayName,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        title = stringResource(R.string.download_assets_filter_search_platform),
        cancelable = false
    )
}

@Composable
private fun FilterHeader(
    title: String,
    expanded: Boolean,
    selected: Boolean,
    selectedLabels: @Composable FlowRowScope.() -> Unit,
    cancelable: Boolean,
    onExpandToggle: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onExpandToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Black
                ),
                color = CryoGlassColors.Ice
            )
            FlowRow(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = selectedLabels
            )
        }

        Row(
            modifier = Modifier.padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (expanded) -180f else 0f,
                animationSpec = getAnimateTween()
            )
            Icon(
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation),
                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                contentDescription = null
            )
            AnimatedVisibility(
                visible = selected && cancelable
            ) {
                IconButton(
                    onClick = {
                        if (selected && cancelable) onClear()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_deselect),
                        contentDescription = stringResource(R.string.generic_clear)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterListItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    selectionMode: FilterSelectionMode,
    itemLayout: @Composable () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    val onClick = {
        val newValue = selectionMode != FilterSelectionMode.Multiple || !selected
        onCheckedChange(newValue)
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .cryoBackground(
                if (selected) cryoGradient()
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0f else 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost,
            LocalTextStyle provides MaterialTheme.typography.labelMedium.copy(shadow = null)
        ) {
            itemLayout()
        }
    }
}
