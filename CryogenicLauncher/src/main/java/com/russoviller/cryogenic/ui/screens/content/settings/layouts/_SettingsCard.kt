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

import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoGlass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.ui.components.BackgroundCard
import com.russoviller.cryogenic.ui.components.TitleAndSummary

/**
 * 根据卡片在UI组中的位置，选择不同的圆角形状
 */
enum class CardPosition {
    /**
     * 位于 UI 组的顶部
     * ``` txt
     *   _______
     *  +       +
     * |         |
     * |         |
     * |         |
     *  ---------
     * ```
     */
    Top,

    /**
     * 位于 UI 组的顶部左侧
     * ``` txt
     *   ________
     *  +        |
     * |         |
     * |         |
     * |         |
     *  ---------
     * ```
     */
    TopStart,

    /**
     * 位于 UI 组的顶部右侧
     * ``` txt
     *  ________
     * |        +
     * |         |
     * |         |
     * |         |
     *  ---------
     * ```
     */
    TopEnd,

    /**
     * 位于 UI 组的中部
     * ``` txt
     *  _________
     * |         |
     * |         |
     * |         |
     * |         |
     *  ---------
     * ```
     */
    Middle,

    /**
     * 位于 UI 组的底部
     * ``` txt
     *  _________
     * |         |
     * |         |
     * |         |
     *  +       +
     *   -------
     * ```
     */
    Bottom,

    /**
     * 位于 UI 组的底部左侧
     * ``` txt
     *  _________
     * |         |
     * |         |
     * |         |
     *  +        |
     *   --------
     * ```
     */
    BottomStart,

    /**
     * 位于 UI 组的底部右侧
     * ``` txt
     *  _________
     * |         |
     * |         |
     * |         |
     * |        +
     *  --------
     * ```
     */
    BottomEnd,

    /**
     * 单个 UI 组件
     * ``` txt
     *   _______
     *  +       +
     * |         |
     * |         |
     *  +       +
     *   -------
     * ```
     */
    Single
}

/**
 * 根据 UI 组件在组中的位置决定的形状
 */
@Composable
fun rememberSettingsCardShape(
    position: CardPosition,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp
): Shape {
    return remember(position, outerShape, innerShape) {
        val outerCut = 18.dp
        val innerCut = 0.dp
        when (position) {
            CardPosition.Top -> RoundedCornerShape(
                topStart = outerCut,
                topEnd = outerCut,
                bottomStart = innerCut,
                bottomEnd = innerCut
            )
            CardPosition.TopStart -> RoundedCornerShape(
                topStart = outerCut,
                topEnd = innerCut,
                bottomStart = innerCut,
                bottomEnd = innerCut
            )
            CardPosition.TopEnd -> RoundedCornerShape(
                topStart = innerCut,
                topEnd = outerCut,
                bottomStart = innerCut,
                bottomEnd = innerCut
            )
            CardPosition.Middle -> RoundedCornerShape(innerCut)
            CardPosition.Bottom -> RoundedCornerShape(
                topStart = innerCut,
                topEnd = innerCut,
                bottomStart = outerCut,
                bottomEnd = outerCut
            )
            CardPosition.BottomStart -> RoundedCornerShape(
                topStart = innerCut,
                topEnd = innerCut,
                bottomStart = outerCut,
                bottomEnd = innerCut
            )
            CardPosition.BottomEnd -> RoundedCornerShape(
                topStart = innerCut,
                topEnd = innerCut,
                bottomStart = innerCut,
                bottomEnd = outerCut
            )
            CardPosition.Single -> RoundedCornerShape(outerCut)
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)

    CryoSettingsSurface(
        modifier = modifier,
        position = position,
        shape = shape,
        onClick = null,
        enabled = true,
        content = content
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)

    CryoSettingsSurface(
        modifier = modifier,
        position = position,
        shape = shape,
        onClick = onClick,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SettingsCard(
    position: CardPosition,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    innerPadding: PaddingValues = PaddingValues(all = 16.dp),
    onClick: () -> Unit,
    trailingIcon: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)

    CryoSettingsSurface(
        modifier = modifier,
        position = position,
        shape = shape,
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleAndSummary(
                modifier = Modifier.weight(1f),
                title = title,
                summary = summary,
                titleStyle = titleStyle,
                summaryStyle = summaryStyle
            )
            trailingIcon?.let { trailing ->
                Row(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    content = trailing
                )
            }
        }
    }
}

@Composable
fun SettingsCardColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}

@Composable
private fun CryoSettingsSurface(
    modifier: Modifier,
    position: CardPosition,
    shape: Shape,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val single = position == CardPosition.Single
    val divider = position == CardPosition.Middle || position == CardPosition.Bottom
    val dividerColor = CryoGlassColors.Ice.copy(alpha = 0.14f)
    Column(
        modifier = modifier
            .then(
                if (single) Modifier.cryoGlass(shape = shape, glow = false)
                else Modifier
                    .drawBehind {
                        if (divider) {
                            drawRect(
                                color = dividerColor,
                                topLeft = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 0f),
                                size = androidx.compose.ui.geometry.Size(size.width - 32.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                    .clip(RoundedCornerShape(16.dp))
            )
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier
            ),
        content = {
            CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
                content()
            }
        }
    )
}
