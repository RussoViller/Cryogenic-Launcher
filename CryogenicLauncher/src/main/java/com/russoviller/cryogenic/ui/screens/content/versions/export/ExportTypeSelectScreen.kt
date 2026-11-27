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

package com.russoviller.cryogenic.ui.screens.content.versions.export

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.components.cryoChamferTile
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.version.export.PackType
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedLazyColumn
import com.russoviller.cryogenic.ui.components.BackgroundCard
import com.russoviller.cryogenic.ui.components.WarningCard
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.rememberSettingsCardShape
import com.russoviller.cryogenic.ui.components.safeRadialGradient

/**
 * 整合包导出类型选择页
 */
@Composable
fun ExportTypeSelectScreen(
    mainScreenKey: TitledNavKey?,
    exportScreenKey: TitledNavKey?,
    onTypeSelect: (PackType) -> Unit
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionExport::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.VersionExports.SelectType, exportScreenKey, false)
    ) { isVisible ->
        val ctx = rememberAnimatedContext(isVisible = isVisible)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .animated(ctx),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .cryoBackground(cryoGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_folder_zip_filled),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Text(
                    text = stringResource(R.string.cryo_export_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.cryo_export_before),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                    color = CryoGlassColors.Ice
                )
                listOf(
                    R.string.versions_export_tip_1,
                    R.string.versions_export_tip_2,
                    R.string.versions_export_tip_3,
                    R.string.versions_export_tip_4,
                    R.string.versions_export_tip_5
                ).forEach { res ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "❄", style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Ice)
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(res),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(R.string.cryo_export_choose),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
                    color = CryoGlassColors.Ice
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExportFormatCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        tag = ".zip",
                        title = stringResource(R.string.versions_export_type_mcbbs),
                        summary = stringResource(R.string.versions_export_type_mcbbs_summary, BuildKeys.LAUNCHER_SHORT_NAME),
                        icon = painterResource(R.drawable.img_chest),
                        onClick = { onTypeSelect(PackType.MCBBS) }
                    )
                    ExportFormatCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        tag = ".mrpack",
                        title = stringResource(R.string.versions_export_type_modrinth),
                        summary = stringResource(R.string.versions_export_type_summary_common),
                        icon = painterResource(R.drawable.img_platform_modrinth),
                        onClick = { onTypeSelect(PackType.Modrinth) }
                    )
                    if (Platform.CURSEFORGE.isEnabled) {
                        ExportFormatCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            tag = ".zip",
                            title = stringResource(R.string.versions_export_type_curseforge),
                            summary = stringResource(R.string.versions_export_type_summary_common),
                            icon = painterResource(R.drawable.img_platform_curseforge),
                            onClick = { onTypeSelect(PackType.CurseForge) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    tag: String,
    title: String,
    summary: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .cryoChamferTile(18.dp)
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(safeRadialGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.35f), CryoGlassColors.Deep)))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(38.dp),
                painter = icon,
                contentDescription = title,
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            modifier = Modifier.weight(1f),
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                text = tag,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
        }
    }
}

/**
 * 导出类型布局
 */
@Composable
private fun TypeItem(
    title: String,
    summary: String,
    icon: Painter,
    position: CardPosition,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = rememberSettingsCardShape(position = position)

    BackgroundCard(
        modifier = modifier,
        onClick = onClick,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //图标
            Image(
                modifier = Modifier.size(28.dp),
                painter = icon,
                contentDescription = title,
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = summary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_arrow_right_rounded),
                contentDescription = null
            )
        }
    }
}
