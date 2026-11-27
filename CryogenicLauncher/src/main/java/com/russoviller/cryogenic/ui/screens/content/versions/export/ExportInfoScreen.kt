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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.version.export.ExportInfo
import com.russoviller.cryogenic.game.version.export.PackType
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSectionHeader
import com.russoviller.cryogenic.ui.components.CryoSlider
import com.russoviller.cryogenic.ui.components.CryoToggleTile
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.components.lazyScrollWithBar
import com.russoviller.cryogenic.ui.components.safeRadialGradient
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.utils.platform.getMaxMemoryForSettings
import com.russoviller.cryogenic.utils.string.isEmptyOrBlank
import com.russoviller.cryogenic.utils.string.isNotEmptyOrBlank
import com.russoviller.cryogenic.utils.string.toSingleLine

/*
 * Fundos com opacidade fixa: esta tela não segue a opacidade do fundo do launcher,
 * para os textos e os quadros ficarem sempre legíveis, com qualquer papel de parede.
 */
private val PanelShape = RoundedCornerShape(18.dp)

private fun Modifier.exportPanel(): Modifier = this
    .clip(PanelShape)
    .background(Brush.verticalGradient(listOf(Color(0xFF0E1A2C), Color(0xFF080F1B))))
    .border(1.dp, Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.6f), CryoGlassColors.Ice.copy(alpha = 0.12f), CryoGlassColors.Aurora.copy(alpha = 0.35f))), PanelShape)

private fun Modifier.exportTile(active: Boolean = false, accent: Color = CryoGlassColors.Ice): Modifier = this
    .clip(PanelShape)
    .background(if (active) Color(0xFF16304A) else Color(0xFF132238))
    .border(1.dp, accent.copy(alpha = if (active) 0.85f else 0.3f), PanelShape)

/** Visual de cada formato de exportação (vale para todos: MCBBS, Modrinth e CurseForge). */
private class PackStyle(val iconRes: Int, val titleRes: Int, val extension: String)

private fun PackType.style(): PackStyle = when (this) {
    PackType.MCBBS -> PackStyle(R.drawable.img_chest, R.string.versions_export_type_mcbbs, ".zip")
    PackType.Modrinth -> PackStyle(R.drawable.img_platform_modrinth, R.string.versions_export_type_modrinth, ".mrpack")
    PackType.CurseForge -> PackStyle(R.drawable.img_platform_curseforge, R.string.versions_export_type_curseforge, ".zip")
}

/**
 * 填写整合包配置信息
 * @param onFinishClick 结束编辑
 */
@Composable
fun ExportInfoScreen(
    info: ExportInfo,
    onInfoEdited: (ExportInfo) -> Unit,
    onFinishClick: () -> Unit,
    mainScreenKey: TitledNavKey?,
    exportScreenKey: TitledNavKey?,
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionExport::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.VersionExports.EditInfo, exportScreenKey, false)
    ) { isVisible ->
        ExportInfoContent(info, onInfoEdited, onFinishClick, isVisible)
    }
}

/** Prévia da tela (usada nos testes de captura de tela). */
@Composable
internal fun ExportInfoPreviewHost(info: ExportInfo) {
    ExportInfoContent(info = info, onInfoEdited = {}, onFinishClick = {}, isVisible = true)
}

@Composable
private fun ExportInfoContent(
    info: ExportInfo,
    onInfoEdited: (ExportInfo) -> Unit,
    onFinishClick: () -> Unit,
    isVisible: Boolean
) {
    run {
        val options = info.packType.options
        val isNameEmpty = remember(info) { info.name.isEmptyOrBlank() }
        val isVersionEmpty = remember(info) { info.version.isEmptyOrBlank() }
        val isAuthorEmpty = remember(info) { info.author.isEmptyOrBlank() }
        val canFinish = !isNameEmpty && !isVersionEmpty && (!options.requireAuthor || !isAuthorEmpty)
        val packRemote = remember(info) {
            when (info.packType) {
                PackType.Modrinth -> info.packModrinth
                PackType.CurseForge -> info.packCurseForge
                else -> false
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            //prévia + checklist + botão
            PackPreview(
                modifier = Modifier
                    .weight(0.36f)
                    .fillMaxHeight(),
                info = info,
                isNameEmpty = isNameEmpty,
                isVersionEmpty = isVersionEmpty,
                isAuthorEmpty = isAuthorEmpty,
                canFinish = canFinish,
                packRemote = packRemote,
                onFinishClick = onFinishClick
            )

            //formulário
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight()
                    .exportPanel()
                    .lazyScrollWithBar(listState),
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, bottom = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CryoSectionHeader(stringResource(R.string.cryo_export_section_identity)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CryoField(
                            modifier = Modifier.weight(0.62f),
                            iconRes = R.drawable.ic_edit_outlined,
                            label = stringResource(R.string.versions_export_pack_name),
                            value = info.name,
                            required = true,
                            isError = isNameEmpty,
                            onValueChange = { newName ->
                                val safeName = newName
                                    .toSingleLine()
                                    .replace("[\\\\\"?:*/<>|]".toRegex(), "")
                                onInfoEdited(info.copy(name = safeName))
                            }
                        )
                        CryoField(
                            modifier = Modifier.weight(0.38f),
                            iconRes = R.drawable.ic_code,
                            label = stringResource(R.string.versions_export_pack_version),
                            value = info.version,
                            required = true,
                            isError = isVersionEmpty,
                            onValueChange = { onInfoEdited(info.copy(version = it.toSingleLine())) }
                        )
                    }
                }
                if (options.requireAuthor) {
                    item {
                        CryoField(
                            iconRes = R.drawable.ic_person_outlined,
                            label = stringResource(R.string.versions_export_pack_author),
                            value = info.author,
                            required = true,
                            isError = isAuthorEmpty,
                            onValueChange = { onInfoEdited(info.copy(author = it.toSingleLine())) }
                        )
                    }
                }

                if (options.requireSummary || options.requireWebsiteUrl) {
                    item { CryoSectionHeader(stringResource(R.string.cryo_export_section_details)) }
                }
                if (options.requireSummary) {
                    item {
                        CryoField(
                            iconRes = R.drawable.ic_description_outlined,
                            label = stringResource(R.string.versions_export_pack_summary),
                            value = info.summary ?: "",
                            hint = stringResource(R.string.versions_export_pack_summary_hint),
                            singleLine = false,
                            onValueChange = { new -> onInfoEdited(info.copy(summary = new.takeIf { it.isNotEmptyOrBlank() })) }
                        )
                    }
                }
                if (options.requireWebsiteUrl) {
                    item {
                        CryoField(
                            iconRes = R.drawable.ic_link,
                            label = stringResource(R.string.versions_export_pack_website),
                            value = info.url,
                            onValueChange = { onInfoEdited(info.copy(url = it.toSingleLine())) }
                        )
                    }
                }

                if (options.requireMinMemory || options.requireGameArgs || options.requireJavaArgs) {
                    item { CryoSectionHeader(stringResource(R.string.cryo_export_section_launch)) }
                }
                if (options.requireMinMemory) {
                    item {
                        val context = LocalContext.current
                        val maxMemory = remember { getMaxMemoryForSettings(context).coerceAtLeast(1) }
                        var memory by remember(info.minMemory) { mutableIntStateOf(info.minMemory) }
                        MemoryTile(
                            value = memory,
                            max = maxMemory,
                            onValueChange = { memory = it },
                            onValueChangeFinished = { onInfoEdited(info.copy(minMemory = memory)) }
                        )
                    }
                }
                if (options.requireGameArgs) {
                    item {
                        CryoField(
                            iconRes = R.drawable.ic_terminal_outlined,
                            label = stringResource(R.string.versions_export_pack_game_args),
                            value = info.gameArgs,
                            monospace = true,
                            onValueChange = { onInfoEdited(info.copy(gameArgs = it.toSingleLine())) }
                        )
                    }
                }
                if (options.requireJavaArgs) {
                    item {
                        CryoField(
                            iconRes = R.drawable.ic_java,
                            label = stringResource(R.string.versions_export_pack_java_args),
                            value = info.javaArgs,
                            monospace = true,
                            onValueChange = { onInfoEdited(info.copy(javaArgs = it.toSingleLine())) }
                        )
                    }
                }

                val showModrinth = options.requirePackModrinth
                val showCurseForge = Platform.CURSEFORGE.isEnabled && options.requirePackCurseForge
                if (showModrinth || showCurseForge) {
                    item { CryoSectionHeader(stringResource(R.string.cryo_export_section_remote)) }
                }
                if (showModrinth) {
                    item {
                        //是否打包 Modrinth 的远程资源
                        CryoToggleTile(
                            modifier = Modifier.fillMaxWidth(),
                            iconRes = R.drawable.ic_download_2_outlined,
                            title = stringResource(R.string.versions_export_pack_pack_remote, Platform.MODRINTH.displayName),
                            checked = info.packModrinth,
                            onCheckedChange = { onInfoEdited(info.copy(packModrinth = it)) }
                        )
                    }
                }
                if (showCurseForge) {
                    item {
                        //是否打包 CurseForge 的远程资源
                        val enabled = info.packType == PackType.CurseForge ||
                                (info.packType == PackType.Modrinth && info.packModrinth)
                        CryoToggleTile(
                            modifier = Modifier.fillMaxWidth(),
                            iconRes = R.drawable.ic_download_2_outlined,
                            title = if (info.packType == PackType.Modrinth) {
                                stringResource(R.string.versions_export_pack_also_pack_curseforge)
                            } else {
                                stringResource(R.string.versions_export_pack_pack_remote, Platform.CURSEFORGE.displayName)
                            },
                            checked = info.packCurseForge,
                            onCheckedChange = { onInfoEdited(info.copy(packCurseForge = it)) },
                            enabled = enabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackPreview(
    modifier: Modifier,
    info: ExportInfo,
    isNameEmpty: Boolean,
    isVersionEmpty: Boolean,
    isAuthorEmpty: Boolean,
    canFinish: Boolean,
    packRemote: Boolean,
    onFinishClick: () -> Unit
) {
    val style = info.packType.style()
    val options = info.packType.options
    Column(
        modifier = modifier
            .exportPanel()
            .padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CryoTag(text = stringResource(style.titleRes), marquee = true, modifier = Modifier.weight(1f, fill = false))
                CryoTag(text = style.extension, accent = CryoGlassColors.Aurora)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        painter = painterResource(style.iconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.name.takeIf { it.isNotBlank() } ?: stringResource(R.string.cryo_export_untitled),
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                        ),
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = buildList {
                        if (info.version.isNotBlank()) add("v" + info.version.removePrefix("v"))
                        if (options.requireAuthor && info.author.isNotBlank()) add(info.author)
                    }.joinToString("  ·  ")
                    if (sub.isNotEmpty()) {
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = CryoGlassColors.Ice,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (options.requireSummary) {
                Text(
                    text = info.summary?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cryo_export_no_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = CryoGlassColors.Frost.copy(alpha = if (info.summary.isNullOrBlank()) 0.5f else 0.85f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (options.requireMinMemory) {
                PreviewStat(R.drawable.ic_memory, stringResource(R.string.versions_export_pack_min_memory), memoryText(info.minMemory))
            }

            //checklist do que falta
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CheckLine(stringResource(R.string.versions_export_pack_name), !isNameEmpty)
                CheckLine(stringResource(R.string.versions_export_pack_version), !isVersionEmpty)
                if (options.requireAuthor) CheckLine(stringResource(R.string.versions_export_pack_author), !isAuthorEmpty)
            }

            //对于打包远端资源的提示
            AnimatedVisibility(visible = packRemote) {
                val platformText = buildList {
                    if (info.packModrinth) add(Platform.MODRINTH.displayName)
                    if (info.packCurseForge) add(Platform.CURSEFORGE.displayName)
                }.joinToString(" ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFC857).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(modifier = Modifier.size(16.dp), painter = painterResource(R.drawable.ic_lightbulb), contentDescription = null, tint = Color(0xFFFFC857))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.versions_export_tip_remote_1, platformText),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        if (info.packType == PackType.Modrinth) {
                            Text(
                                text = stringResource(R.string.versions_export_tip_remote_2),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.padding(top = 10.dp))
        Text(
            modifier = Modifier.padding(bottom = 6.dp),
            text = stringResource(if (canFinish) R.string.cryo_export_ready else R.string.cryo_export_missing),
            style = MaterialTheme.typography.labelSmall,
            color = if (canFinish) CryoGlassColors.Ice else CryoDanger
        )
        CryoButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onFinishClick,
            enabled = canFinish
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val text = stringResource(R.string.versions_export_pack_select_files)
                Icon(painter = painterResource(R.drawable.ic_select_all), contentDescription = text)
                Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PreviewStat(iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(modifier = Modifier.size(15.dp), painter = painterResource(iconRes), contentDescription = null, tint = CryoGlassColors.Ice)
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = CryoGlassColors.Frost,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
private fun CheckLine(label: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (ok) CryoGlassColors.Ice else Color.Transparent)
                .border(1.dp, if (ok) CryoGlassColors.Ice else CryoDanger, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (ok) Icon(modifier = Modifier.size(11.dp), painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = CryoGlassColors.Deep)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) Color.White else CryoDanger,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MemoryTile(value: Int, max: Int, onValueChange: (Int) -> Unit, onValueChangeFinished: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .exportTile(active = value > 0)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_memory), contentDescription = null, tint = CryoGlassColors.Ice)
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.versions_export_pack_min_memory),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CryoGlassColors.Ice.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                text = memoryText(value),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice
            )
        }
        //o slider precisa da largura toda (sem ela só a bolinha aparecia, num canto)
        CryoSlider(
            modifier = Modifier.fillMaxWidth(),
            value = value.coerceIn(0, max).toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..max.toFloat(),
            steps = (max / 512 - 1).coerceIn(0, 63),
            onValueChangeFinished = onValueChangeFinished
        )
        Row {
            Text(text = "0", style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.6f))
            Spacer(Modifier.weight(1f))
            Text(text = memoryText(max), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun memoryText(mb: Int): String = when {
    mb <= 0 -> stringResource(R.string.cryo_export_memory_none)
    mb >= 1024 -> String.format(java.util.Locale.ROOT, "%.1f GB", mb / 1024f)
    else -> "$mb MB"
}

@Composable
private fun CryoField(
    iconRes: Int,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    required: Boolean = false,
    isError: Boolean = false,
    hint: String? = null,
    singleLine: Boolean = true,
    monospace: Boolean = false
) {
    val accent = if (isError) CryoDanger else CryoGlassColors.Ice
    Column(
        modifier = modifier
            .exportTile(accent = accent)
            .border(1.dp, accent.copy(alpha = if (isError) 0.8f else 0f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(modifier = Modifier.size(14.dp), painter = painterResource(iconRes), contentDescription = null, tint = accent)
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (required) {
                Text(
                    text = if (isError) stringResource(R.string.generic_cannot_empty) else "*",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) CryoDanger else CryoGlassColors.Frost.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 22.dp else 64.dp),
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(CryoGlassColors.Ice)
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.55f)
            )
        }
    }
}
