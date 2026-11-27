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

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R

/**
 * 收藏状态文本
 */
@Composable
fun favoriteStateText(isFavorite: Boolean): String = stringResource(
    if (isFavorite) R.string.favorite_added else R.string.favorite_add
)

/**
 * 收藏开关标签，图标与文本跟随收藏状态切换
 */
@Composable
fun FavoriteToggleLabel(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    shape: Shape = MaterialTheme.shapes.small
) {
    CryoFavoritePill(
        isFavorite = isFavorite,
        onClick = onClick,
        modifier = modifier,
        iconSize = iconSize,
        fontSize = if (style.fontSize.isSpecified) style.fontSize else 11.sp
    )
}

@Composable
fun FavoriteIdentifier(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 12.dp,
    shape: Shape = MaterialTheme.shapes.large,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
) {
    CryoFavoritePill(
        isFavorite = isFavorite,
        onClick = onClick,
        modifier = modifier,
        iconSize = iconSize,
        fontSize = if (textStyle.fontSize.isSpecified) textStyle.fontSize else 10.sp
    )
}

/** Cor do favorito no Cryogenic. */
private val FavoriteAccent = androidx.compose.ui.graphics.Color(0xFFFF6B9A)

/**
 * Botão de favoritar no visual do Cryogenic: pílula arredondada; favoritado = preenchido em
 * degradê rosa com coração cheio (com um "pulo" ao marcar); não favoritado = contorno.
 */
@Composable
private fun CryoFavoritePill(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    iconSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val accent = FavoriteAccent
    val pillShape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    val scale = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
    var first by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(isFavorite) {
        if (first) { first = false; return@LaunchedEffect }
        if (isFavorite) {
            scale.animateTo(1.35f, androidx.compose.animation.core.tween(120))
            scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f))
        }
    }
    Row(
        modifier = modifier
            .clip(pillShape)
            .then(
                if (isFavorite) Modifier.background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent, androidx.compose.ui.graphics.Color(0xFFB57BFF)))
                ) else Modifier
                    .background(com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.6f))
                    .border(1.dp, accent.copy(alpha = 0.7f), pillShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            painter = painterResource(
                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outlined
            ),
            contentDescription = null,
            tint = if (isFavorite) androidx.compose.ui.graphics.Color.White else accent
        )
        Text(
            text = favoriteStateText(isFavorite).uppercase(),
            style = TextStyle(
                fontFamily = com.russoviller.cryogenic.ui.components.CryoTagFont,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.8.sp,
                shadow = null
            ),
            color = if (isFavorite) androidx.compose.ui.graphics.Color.White else accent,
            maxLines = 1
        )
    }
}
