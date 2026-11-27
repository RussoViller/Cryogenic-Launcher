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

package com.russoviller.cryogenic.ui.screens.content.download.game

import com.russoviller.cryogenic.ui.components.cryoGlow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoFillFactor
import com.russoviller.cryogenic.ui.components.cryoGradient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import com.russoviller.cryogenic.game.addons.modloader.fabriclike.FabricLikeVersion
import com.russoviller.cryogenic.game.addons.modloader.forgelike.forge.ForgeVersion
import com.russoviller.cryogenic.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.russoviller.cryogenic.game.addons.modloader.modlike.ModVersion
import com.russoviller.cryogenic.game.addons.modloader.optifine.OptiFineVersion
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.components.influencedByBackgroundColor
import com.russoviller.cryogenic.ui.components.rememberMaxHeight
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.getTimeAgo
import com.russoviller.cryogenic.ui.components.safeRadialGradient

/** Addon 加载状态 */
sealed interface AddonState {
    /** 已完成加载 */
    data object None : AddonState
    /** 加载中 */
    data object Loading : AddonState
    /**
     * 加载出现异常
     * @param message 异常消息代理对象
     */
    data class Error(val message: AndroidStringText): AddonState
}

/**
 * 简易 Addon 文本占位
 */
@Composable
private fun AddonTextLayout(
    modifier: Modifier = Modifier,
    title: String,
    summary: String
) {
    AddonTextLayout(
        modifier = modifier,
        title = title,
        summary = {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

/**
 * 简易 Addon 文本占位
 */
@Composable
private fun AddonTextLayout(
    modifier: Modifier = Modifier,
    title: String,
    summary: AndroidStringText
) {
    AddonTextLayout(
        modifier = modifier,
        title = title,
        summary = {
            AndroidStringText(
                text = summary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

/**
 * 简易 Addon 文本占位
 */
@Composable
private fun AddonTextLayout(
    modifier: Modifier = Modifier,
    title: String,
    summary: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        summary()
    }
}

/**
 * Addon 版本列表
 * @param state Addon 当前的加载状态
 * @param items Addon 版本列表
 * @param current 当前选择的 Addon 版本
 * @param incompatibleSet 当前 Addon 的不兼容列表
 * @param checkIncompatible 检查当前的 Addon 的不兼容情况
 * @param triggerCheckIncompatible 手动触发检查不兼容情况
 * @param error 设置错误名称，让该列表不可用
 * @param iconPainter Addon 的图标
 * @param maxListHeight 列表展开最高显示高度
 * @param autoCollapse 选择版本后是否自动收起
 */
@Composable
fun <E> AddonListLayout(
    modifier: Modifier = Modifier,
    state: AddonState,
    items: List<E>?,
    current: E?,
    incompatibleSet: Set<ModLoader>,
    checkIncompatible: () -> Unit = {},
    triggerCheckIncompatible: Array<Any> = arrayOf(Unit),
    error: String? = null,
    iconPainter: Painter,
    title: String,
    getItemText: @Composable (E) -> String,
    summary: (@Composable (E) -> Unit)? = null,
    maxListHeight: Dp = rememberMaxHeight(),
    autoCollapse: Boolean = true,
    onValueChange: (E?) -> Unit = {},
    onReload: () -> Unit = {},
    shape: Shape = MaterialTheme.shapes.large,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
) {
    var selectedItem by remember { mutableStateOf<E?>(null) }

    LaunchedEffect(state, selectedItem, *triggerCheckIncompatible) {
        //加载状态 || 选择项变更 || 手动触发，检查一次不兼容情况
        checkIncompatible()
    }

    LaunchedEffect(state, items, current) {
        selectedItem = items?.firstOrNull { it == current }
    }
    var expanded by remember { mutableStateOf(false) }

    fun clear() {
        onValueChange(null)
        selectedItem = null
        if (autoCollapse) expanded = false
    }

    //不兼容列表不为空，清除当前Addon的选择
    LaunchedEffect(incompatibleSet) {
        if (incompatibleSet.isNotEmpty()) clear()
    }

    val accent = when {
        incompatibleSet.isNotEmpty() || error != null -> CryoDanger
        selectedItem != null -> CryoGlassColors.Aurora
        else -> CryoGlassColors.Ice
    }
    val chamfer = cryoChamferShape(12.dp)
    val fillFactor = cryoFillFactor().coerceAtMost(1f)
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides CryoGlassColors.Frost
    ) {
        Column(
            modifier = modifier
                .then(if (selectedItem != null) Modifier.cryoGlowSafe() else Modifier)
                .clip(chamfer)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = if (selectedItem != null) 0.24f else 0.10f),
                            CryoGlassColors.Navy.copy(alpha = 0.55f + 0.35f * fillFactor),
                            CryoGlassColors.Deep.copy(alpha = 0.65f + 0.3f * fillFactor)
                        )
                    )
                )
                .border(
                    if (selectedItem != null) 1.5.dp else 1.dp,
                    when {
                        incompatibleSet.isNotEmpty() || error != null -> Brush.linearGradient(listOf(CryoDanger.copy(alpha = 0.85f), CryoDanger.copy(alpha = 0.2f)))
                        selectedItem != null -> cryoBorderBrush(1.3f)
                        else -> cryoBorderBrush(0.55f)
                    },
                    chamfer
                )
                .fillMaxWidth()
        ) {
            AddonListHeader(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                iconPainter = iconPainter,
                title = title,
                items = items,
                selectedItem = selectedItem,
                getItemText = getItemText,
                incompatibleSet = incompatibleSet,
                error = error,
                expanded = expanded,
                accent = accent,
                enabled = state == AddonState.None && !items.isNullOrEmpty() && incompatibleSet.isEmpty() && error == null,
                onClick = { expanded = !expanded },
                onClear = ::clear,
                onReload = onReload
            )

            if (state == AddonState.None && !items.isNullOrEmpty() && error == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(items) { index, item ->
                                AddonListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    index = index,
                                    selected = selectedItem == item,
                                    itemName = getItemText(item),
                                    summary = summary?.let {
                                        { it.invoke(item) }
                                    },
                                    onClick = {
                                        if (expanded && selectedItem != item) {
                                            onValueChange(item)
                                            selectedItem = item
                                            if (autoCollapse) expanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <E> AddonListHeader(
    modifier: Modifier = Modifier,
    state: AddonState,
    iconPainter: Painter,
    title: String,
    items: List<E>?,
    selectedItem: E?,
    getItemText: @Composable (E) -> String,
    incompatibleSet: Set<ModLoader>,
    error: String? = null,
    expanded: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {},
    onReload: () -> Unit = {}
) {
    val ice = CryoGlassColors.Ice
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(safeRadialGradient(listOf(accent.copy(alpha = 0.28f), CryoGlassColors.Deep.copy(alpha = 0.95f))))
                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(30.dp),
                painter = iconPainter,
                contentDescription = null
            )
        }

        val summaryText: String = when (state) {
            is AddonState.Loading -> stringResource(R.string.generic_loading)
            is AddonState.Error -> ""
            is AddonState.None -> when {
                error != null -> error
                incompatibleSet.isNotEmpty() -> stringResource(R.string.download_game_addon_incompatible_with, incompatibleSet.joinToString(", ") { it.displayName })
                items.isNullOrEmpty() -> stringResource(R.string.download_game_addon_unavailable)
                selectedItem == null -> stringResource(R.string.download_game_addon_available)
                else -> getItemText(selectedItem)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state is AddonState.Error) {
                AndroidStringText(
                    text = androidText(R.string.download_game_addon_list_load_error, state.message),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CryoDanger),
                    maxLines = 2
                )
            } else {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (selectedItem != null) CryoGlassColors.Aurora else CryoGlassColors.Frost.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when (state) {
            is AddonState.Loading -> {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = ice
                )
            }
            is AddonState.Error -> {
                AddonCircleButton(iconRes = R.drawable.ic_refresh, description = stringResource(R.string.generic_refresh), tint = ice, onClick = onReload)
            }
            is AddonState.None -> {
                if (selectedItem != null && enabled) {
                    AddonCircleButton(iconRes = R.drawable.ic_deselect, description = stringResource(R.string.generic_clear), tint = ice, onClick = onClear)
                }
                val blocked = error != null || incompatibleSet.isNotEmpty()
                val none = items.isNullOrEmpty()
                val label = when {
                    blocked -> stringResource(R.string.cryo_addon_badge_blocked)
                    none -> stringResource(R.string.cryo_addon_badge_none)
                    selectedItem != null -> stringResource(R.string.cryo_addon_badge_selected)
                    else -> stringResource(R.string.cryo_addon_choose)
                }
                val pillShape = RoundedCornerShape(12.dp)
                val filled = !blocked && !none
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(pillShape)
                        .then(
                            if (filled) Modifier.cryoBackground(cryoGradient())
                            else Modifier
                                .background((if (blocked) CryoDanger else CryoGlassColors.Frost).copy(alpha = 0.08f))
                                .border(1.dp, (if (blocked) CryoDanger else CryoGlassColors.Frost).copy(alpha = 0.45f), pillShape)
                        )
                        .clickable(enabled = enabled, onClick = onClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedItem != null && filled) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = CryoGlassColors.Deep
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                        fontWeight = FontWeight.Black,
                        color = when {
                            filled -> CryoGlassColors.Deep
                            blocked -> CryoDanger
                            else -> CryoGlassColors.Frost.copy(alpha = 0.7f)
                        },
                        maxLines = 1
                    )
                    if (filled) {
                        val rotation by animateFloatAsState(
                            targetValue = if (expanded) -180f else 0f,
                            animationSpec = getAnimateTween()
                        )
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation),
                            painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                            contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse),
                            tint = CryoGlassColors.Deep
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonCircleButton(iconRes: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, tint.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = tint
        )
    }
}

@Composable
fun AddonListItem(
    modifier: Modifier = Modifier,
    index: Int = -1,
    selected: Boolean,
    itemName: String,
    summary: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val ice = CryoGlassColors.Ice
    val shape = cryoChamferShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        (if (selected) CryoGlassColors.Aurora else ice).copy(alpha = if (selected) 0.24f else 0.06f),
                        CryoGlassColors.Navy.copy(alpha = 0.7f),
                        CryoGlassColors.Deep.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, if (selected) cryoBorderBrush(1.3f) else cryoBorderBrush(0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (index >= 0) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = ice.copy(alpha = 0.8f)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            summary?.invoke()
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.cryoBackground(cryoGradient())
                    else Modifier.border(1.dp, ice.copy(alpha = 0.35f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
        }
    }
}

private fun Modifier.cryoGlowSafe(): Modifier = this.cryoGlow(16.dp, selected = true, strength = 0.6f)

@Composable
fun AddonWarningItem(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = influencedByBackgroundColor(
        color = MaterialTheme.colorScheme.errorContainer,
        enabled = true
    ),
    contentColor: Color = MaterialTheme.colorScheme.onErrorContainer,
    blur: Int = AllSettings.backgroundBlur.state,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .backgroundGlass(blur, color)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 4.dp),
                    painter = painterResource(R.drawable.ic_warning_filled),
                    contentDescription = null
                )
            }

            AddonTextLayout(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.generic_warning),
                summary = text
            )
        }
    }
}

@Composable
fun OptiFineVersionSummary(
    optifine: OptiFineVersion,
    iconSize: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    val typeText = if (optifine.isPreview) {
        stringResource(R.string.download_game_addon_preview)
    } else {
        stringResource(R.string.download_game_addon_release)
    }

    Row(
        modifier = Modifier.alpha(alpha = 0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //版本状态
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_package_2_outlined),
                contentDescription = null
            )
            Text(text = typeText, style = textStyle)
        }
        //发布时间
        optifine.releaseDate.takeIf { it.isNotEmpty() }?.let { releaseDate ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    painter = painterResource(R.drawable.ic_autorenew),
                    contentDescription = null
                )
                Text(text = releaseDate, style = textStyle)
            }
        }

        //兼容状态
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                optifine.forgeVersion == null -> {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.download_game_addon_incompatible_with, ModLoader.FORGE.displayName),
                        style = textStyle
                    )
                }
                optifine.forgeVersion.isNotEmpty() -> {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.download_game_addon_compatible_with, "${ModLoader.FORGE.displayName} ${optifine.forgeVersion}"),
                        style = textStyle
                    )
                }
            }
        }
    }
}

@Composable
fun ForgeVersionSummary(
    forgeVersion: ForgeVersion,
    iconSize: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    Row(
        modifier = Modifier.alpha(alpha = 0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (forgeVersion.isRecommended) {
            //Forge 官方推荐
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    painter = painterResource(R.drawable.ic_star_filled),
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.download_game_addon_recommended, ModLoader.FORGE.displayName),
                    style = textStyle
                )
            }
        }
        //发布时间
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_autorenew),
                contentDescription = null
            )
            Text(text = forgeVersion.releaseTime, style = textStyle)
        }
    }
}

@Composable
fun NeoForgeSummary(
    neoforgeVersion: NeoForgeVersion,
    iconSize: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    val typeText = if (neoforgeVersion.isBeta) {
        stringResource(R.string.download_game_addon_debug)
    } else {
        stringResource(R.string.download_game_addon_release)
    }

    Row(
        modifier = Modifier.alpha(alpha = 0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //版本状态
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_package_2_outlined),
                contentDescription = null
            )
            Text(text = typeText, style = textStyle)
        }
    }
}

@Composable
fun FabricLikeSummary(
    fabricLikeVersion: FabricLikeVersion,
    iconSize: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    val typeText = if (fabricLikeVersion.stable) {
        stringResource(R.string.download_game_addon_stable)
    } else {
        stringResource(R.string.download_game_addon_debug)
    }

    Row(
        modifier = Modifier.alpha(alpha = 0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //版本状态
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_package_2_outlined),
                contentDescription = null
            )
            Text(text = typeText, style = textStyle)
        }
    }
}

@Composable
fun ModSummary(
    modVersion: ModVersion,
    iconSize: Dp = 14.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    val version = modVersion.version

    Row(
        modifier = Modifier.alpha(alpha = 0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //版本状态
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_package_2_outlined),
                contentDescription = null
            )
            Text(
                text = stringResource(version.versionType.textRes),
                style = textStyle
            )
        }
        //更新时间
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_autorenew),
                contentDescription = null
            )
            Text(
                text = getTimeAgo(
                    context = LocalContext.current,
                    dateString = version.datePublished
                ),
                style = textStyle
            )
        }
    }
}
