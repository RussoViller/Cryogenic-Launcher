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

package com.russoviller.cryogenic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.setting.enums.isLauncherInDarkTheme
import com.russoviller.cryogenic.ui.components.influencedByBackgroundColor

/** 应用整体背景的颜色 */
@Composable
fun backgroundColor(): Color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.40f)
@Composable
fun onBackgroundColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * 卡片背景颜色
 * [androidx.compose.material3.Card]
 * [com.russoviller.cryogenic.ui.components.BackgroundCard]
 * [androidx.compose.ui.window.Dialog]
 * @param influencedByBackground 是否受背景内容影响，更改自身不透明度
 */
@Composable
fun cardColor(
    influencedByBackground: Boolean = true
): Color = if (influencedByBackground) {
    com.russoviller.cryogenic.ui.components.CryoGlassColors.Navy.copy(
        alpha = (0.66f * com.russoviller.cryogenic.ui.components.cryoFillFactor()).coerceIn(0f, 1f)
    )
} else {
    com.russoviller.cryogenic.ui.components.CryoGlassColors.Navy.copy(alpha = 0.96f)
}
@Composable
fun onCardColor(): Color = MaterialTheme.colorScheme.onSurface
/**
 * 卡片顶部Title的背景颜色，半透明的surface
 */
@Composable
fun cardTitleColor(
    alpha: Float = 0.5f
): Color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = alpha * 0.8f * com.russoviller.cryogenic.ui.components.cryoFillFactor().coerceAtMost(1f))

/**
 * 卡片上的Item的背景颜色
 * @param influencedByBackground 是否受背景内容影响，更改自身不透明度
 */
@Composable
fun itemColor(
    influencedByBackground: Boolean = true,
    isDark: Boolean = isLauncherInDarkTheme()
): Color {
    return com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.04f + 0.05f * com.russoviller.cryogenic.ui.components.cryoFillFactor().coerceAtMost(1f))
}
@Composable
fun onItemColor() = MaterialTheme.colorScheme.onSurface
