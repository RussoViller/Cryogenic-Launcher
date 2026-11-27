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

import com.russoviller.cryogenic.ui.components.cryoTacticalBackdrop
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.ui.components.BackgroundCard

@Composable
fun VersionChunkBackground(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable ColumnScope.() -> Unit
) {
    val chamfer = com.russoviller.cryogenic.ui.components.cryoChamferShape(22.dp)
    val ice = CryoGlassColors.Ice
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cryoGlass(shape = chamfer, cornerRadius = 22.dp)
            .drawBehind {
                val step = 26.dp.toPx()
                val line = ice.copy(alpha = 0.035f)
                var x = step
                while (x < size.width) {
                    drawLine(line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(line, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
                    y += step
                }
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, ice.copy(alpha = 0.8f), CryoGlassColors.Aurora.copy(alpha = 0.6f), Color.Transparent)),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.84f, 2.dp.toPx())
                )
            }
            .padding(paddingValues),
        content = content
    )
}

enum class CryoVersionPage(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val folder: String,
    val tipsRes: List<Int>
) {
    Mods(
        com.russoviller.cryogenic.R.drawable.ic_extension_outlined,
        com.russoviller.cryogenic.R.string.cryo_page_mods_title,
        com.russoviller.cryogenic.R.string.cryo_page_mods_desc,
        "mods",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_mods_tip1, com.russoviller.cryogenic.R.string.cryo_page_mods_tip2)
    ),
    Saves(
        com.russoviller.cryogenic.R.drawable.ic_public,
        com.russoviller.cryogenic.R.string.cryo_page_saves_title,
        com.russoviller.cryogenic.R.string.cryo_page_saves_desc,
        "saves",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_saves_tip1, com.russoviller.cryogenic.R.string.cryo_page_saves_tip2)
    ),
    ResourcePacks(
        com.russoviller.cryogenic.R.drawable.ic_format_paint_outlined,
        com.russoviller.cryogenic.R.string.cryo_page_resource_title,
        com.russoviller.cryogenic.R.string.cryo_page_resource_desc,
        "resourcepacks",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_resource_tip1, com.russoviller.cryogenic.R.string.cryo_page_resource_tip2)
    ),
    Shaders(
        com.russoviller.cryogenic.R.drawable.ic_lightbulb,
        com.russoviller.cryogenic.R.string.cryo_page_shaders_title,
        com.russoviller.cryogenic.R.string.cryo_page_shaders_desc,
        "shaderpacks",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_shaders_tip1, com.russoviller.cryogenic.R.string.cryo_page_shaders_tip2)
    ),
    Screenshots(
        com.russoviller.cryogenic.R.drawable.ic_photo_library_outlined,
        com.russoviller.cryogenic.R.string.cryo_page_screens_title,
        com.russoviller.cryogenic.R.string.cryo_page_screens_desc,
        "screenshots",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_screens_tip1, com.russoviller.cryogenic.R.string.cryo_page_screens_tip2)
    ),
    ModifyVersion(
        com.russoviller.cryogenic.R.drawable.ic_edit_filled,
        com.russoviller.cryogenic.R.string.cryo_page_modify_title,
        com.russoviller.cryogenic.R.string.cryo_page_modify_desc,
        "versions",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_modify_tip1, com.russoviller.cryogenic.R.string.cryo_page_modify_tip2)
    ),
    Servers(
        com.russoviller.cryogenic.R.drawable.ic_dns_outlined,
        com.russoviller.cryogenic.R.string.cryo_page_servers_title,
        com.russoviller.cryogenic.R.string.cryo_page_servers_desc,
        "servers.dat",
        listOf(com.russoviller.cryogenic.R.string.cryo_page_servers_tip1, com.russoviller.cryogenic.R.string.cryo_page_servers_tip2)
    )
}

@Composable
fun CryoVersionChunk(
    page: CryoVersionPage,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CryoVersionInfoPanel(
            page = page,
            modifier = Modifier
                .width(210.dp)
                .fillMaxHeight()
        )
        VersionChunkBackground(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            paddingValues = paddingValues,
            shape = RoundedCornerShape(20.dp),
            content = content
        )
    }
}

@Composable
fun CryoVersionInfoPanel(
    page: CryoVersionPage,
    modifier: Modifier = Modifier
) {
    CryoTacticalInfoPanel(
        iconRes = page.iconRes,
        title = stringResource(page.titleRes),
        description = stringResource(page.descRes),
        folder = page.folder,
        tips = page.tipsRes.map { stringResource(it) },
        modifier = modifier
    )
}

@Composable
fun CryoTacticalInfoPanel(
    iconRes: Int,
    title: String,
    description: String,
    folder: String,
    tips: List<String>,
    modifier: Modifier = Modifier,
    extra: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val panelShape = androidx.compose.runtime.remember(density) {
        with(density) { com.russoviller.cryogenic.ui.components.cryoTacticalPanelShape(true, 22.dp.toPx(), 12.dp.toPx()) }
    }
    val fill = 0.55f + 0.45f * com.russoviller.cryogenic.ui.components.cryoFillFactor().coerceAtMost(1f)
    Column(
        modifier = modifier
            .clip(panelShape)
            .cryoTacticalBackdrop(panelShape, true, fill)
            .padding(start = 14.dp, top = 14.dp, end = 24.dp, bottom = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(com.russoviller.cryogenic.ui.components.cryoChamferShape(10.dp))
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Column {
                Text(
                    text = "CRYO // " + folder.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, fontSize = 9.sp),
                    color = CryoGlassColors.Ice,
                    maxLines = 1
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    ),
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(CryoGlassColors.Aurora)
            )
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CryoGlassColors.Deep.copy(alpha = 0.6f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                text = "/" + folder,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice,
                maxLines = 1
            )
        }
        extra?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Ice
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(10) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Ice.copy(alpha = if (i < 7) 0.7f - i * 0.06f else 0.12f))
                )
            }
        }
        if (tips.isNotEmpty()) Text(
            text = stringResource(com.russoviller.cryogenic.R.string.cryo_tips),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
            color = CryoGlassColors.Ice
        )
        tips.forEachIndexed { index, tip ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "%02d".format(index + 1),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Aurora
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = tip,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.85f)
                )
            }
        }
        actions()
    }
}
