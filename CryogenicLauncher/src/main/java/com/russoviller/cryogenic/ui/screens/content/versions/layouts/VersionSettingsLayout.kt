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

package com.russoviller.cryogenic.ui.screens.content.versions.layouts

import com.russoviller.cryogenic.ui.components.cryoBackground

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.version.installed.SettingState
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.ui.components.TitleAndSummary
import com.russoviller.cryogenic.ui.screens.content.elements.VersionIconImage
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCard
import kotlin.math.min

@Composable
fun StatefulDropdownMenuFollowGlobal(
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    currentValue: SettingState,
    onValueChange: (SettingState) -> Unit,
    enabled: Boolean = true,
    iconSize: Dp = 20.dp,
    summary: String? = null
) {
    var value by remember { mutableStateOf(currentValue) }

    SettingsCard(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        position = position
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TitleAndSummary(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                title = title,
                summary = summary
            )
            Row(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.6f))
                    .border(1.dp, com.russoviller.cryogenic.ui.components.cryoBorderBrush(), androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                SettingState.entries.forEach { state ->
                    val selected = state == value
                    val accent = when (state) {
                        SettingState.ENABLE -> androidx.compose.ui.graphics.Color(0xFF7CF5A8)
                        SettingState.DISABLE -> androidx.compose.ui.graphics.Color(0xFFFF8FA6)
                        SettingState.FOLLOW_GLOBAL -> com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
                    }
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .then(
                                if (selected) Modifier.cryoBackground(com.russoviller.cryogenic.ui.components.cryoGradient())
                                else Modifier
                            )
                            .clickable(enabled = enabled && !selected) {
                                value = state
                                onValueChange(state)
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (selected) com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep else accent)
                        )
                        Text(
                            text = stringResource(state.textRes),
                            style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = if (selected) com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep else com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleableIntSliderSettingsCard(
    currentValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Int,
    position: CardPosition,
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit = {},
    onValueChangeFinished: () -> Unit = {},
    previewContent: @Composable (ColumnScope.() -> Unit) = {}
) {
    var checked by remember { mutableStateOf(currentValue >= valueRange.start) }
    var value by remember {
        val v1 = currentValue.takeIf { it >= valueRange.start } ?: defaultValue
        mutableIntStateOf(min(v1, valueRange.endInclusive.toInt()))
    }

    if (!enabled) checked = false

    IntSliderSettingsCard(
        modifier = modifier,
        position = position,
        value = value,
        title = title,
        summary = summary,
        valueRange = valueRange,
        onValueChange = {
            value = it
            onValueChange(value)
        },
        onValueChangeFinished = onValueChangeFinished,
        suffix = suffix,
        enabled = checked,
        fineTuningControl = true,
        appendContent = {
            Checkbox(
                modifier = Modifier.padding(start = 12.dp),
                checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    checked = it
                    value = defaultValue
                    onValueChange(if (checked) value else -1)
                    onValueChangeFinished()
                }
            )
        },
        previewContent = previewContent
    )
}

@Composable
fun VersionOverviewItem(
    modifier: Modifier = Modifier,
    version: Version,
    versionName: String = version.getVersionName(),
    versionSummary: String,
    refreshKey: Any? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VersionIconImage(
            version = version,
            modifier = Modifier.size(34.dp),
            refreshKey = refreshKey
        )
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                maxLines = 1,
                text = versionName,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                modifier = Modifier
                    .alpha(0.7f)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                maxLines = 1,
                text = versionSummary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
