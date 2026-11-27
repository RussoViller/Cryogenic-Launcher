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

import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Canvas
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R

@Composable
fun DefaultSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    val scheme = MaterialTheme.colorScheme
    val progress by animateFloatAsState(if (checked) 1f else 0f, label = "cryo_switch")
    val alpha = if (enabled) 1f else 0.38f
    val trackOn = Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora))
    val trackOff = CryoGlassColors.Deep.copy(alpha = 0.55f)
    val outline = CryoGlassColors.Ice.copy(alpha = 0.55f)
    val thumbOn = CryoGlassColors.Deep
    Canvas(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange
            )
    ) {
        val r = size.height / 2f
        drawRoundRect(color = trackOff.copy(alpha = alpha), cornerRadius = CornerRadius(r))
        drawRoundRect(brush = trackOn, alpha = progress * alpha, cornerRadius = CornerRadius(r))
        if (progress < 1f) {
            drawRoundRect(
                color = outline.copy(alpha = (1f - progress) * alpha),
                cornerRadius = CornerRadius(r),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        val pad = 3.dp.toPx()
        val thumbR = r - pad
        val cx = pad + thumbR + (size.width - (pad + thumbR) * 2f) * progress
        drawCircle(color = Color.Black.copy(alpha = 0.25f * alpha), radius = thumbR, center = Offset(cx, r + 1.dp.toPx()))
        drawCircle(color = androidx.compose.ui.graphics.lerp(Color.White, thumbOn, progress).copy(alpha = alpha), radius = thumbR, center = Offset(cx, r))
        drawCircle(color = Color.White.copy(alpha = progress * alpha), radius = thumbR * 0.38f, center = Offset(cx, r))
    }
}

/**
 * Represents the color used for the switch's track, depending on [enabled] and [checked].
 *
 * @param enabled whether the [Switch] is enabled or not
 * @param checked whether the [Switch] is checked or not
 */
@Stable
private fun SwitchColors.trackColor(enabled: Boolean, checked: Boolean): Color =
    if (enabled) {
        if (checked) checkedTrackColor else uncheckedTrackColor
    } else {
        if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
    }
