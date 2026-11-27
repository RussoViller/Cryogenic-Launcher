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

package com.russoviller.cryogenic.ui.screens.content.versions.elements

import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import android.content.Context
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonSyntaxException
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.utils.getMcmodTitle
import com.russoviller.cryogenic.game.download.jvm_server.JvmCrashException
import com.russoviller.cryogenic.game.download.jvm_server.isProcessStartRefused
import com.russoviller.cryogenic.game.version.download.DownloadFailedException
import com.russoviller.cryogenic.game.version.mod.LocalMod
import com.russoviller.cryogenic.game.version.mod.RemoteMod
import com.russoviller.cryogenic.game.version.mod.isEnabled
import com.russoviller.cryogenic.game.version.mod.update.ModUpdater
import com.russoviller.cryogenic.game.version.mod.update.SelectableModManifest
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.ProgressDialog
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.AssetsIcon
import com.russoviller.cryogenic.ui.screens.content.elements.TitleTaskFlowDialog
import com.russoviller.cryogenic.ui.screens.content.versions.elements.ModStateFilter.All
import com.russoviller.cryogenic.ui.screens.content.versions.elements.ModStateFilter.Disabled
import com.russoviller.cryogenic.ui.screens.content.versions.elements.ModStateFilter.Enabled
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.logging.Logger
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeoutException

sealed interface ModsOperation {
    data object None : ModsOperation
    /** 执行任务中 */
    data object Progress : ModsOperation
    /** 删除模组对话框 */
    data class Delete(val mod: LocalMod) : ModsOperation
}

sealed interface ModsUpdateOperation {
    data object None : ModsUpdateOperation
    /** 警告用户更新模组的注意事项 */
    data class Warning(val mods: List<RemoteMod>) : ModsUpdateOperation
    /** 开始更新模组 */
    data object Update : ModsUpdateOperation
    /** 更新模组时出现异常 */
    data class Error(val th: Throwable) : ModsUpdateOperation
    /** 更新模组成功 */
    data object Success : ModsUpdateOperation
}

sealed interface ModsConfirmOperation {
    data object None : ModsConfirmOperation
    /** 等待用户确认模组更新的信息 */
    data class WaitingConfirm(
        val list: List<SelectableModManifest>,
        val skipped: List<com.russoviller.cryogenic.game.version.mod.update.SkippedMod> = emptyList()
    ) : ModsConfirmOperation
}

@Composable
fun ModsOperation(
    modsOperation: ModsOperation,
    updateOperation: (ModsOperation) -> Unit,
    onDelete: (LocalMod) -> Unit,
    modsDir: java.io.File? = null,
    loader: com.russoviller.cryogenic.game.addons.modloader.ModLoader? = null
) {
    when (modsOperation) {
        is ModsOperation.None -> {}
        is ModsOperation.Progress -> {
            ProgressDialog()
        }
        is ModsOperation.Delete -> {
            val mod = modsOperation.mod
            //Mods que precisam deste (param de funcionar se ele for apagado)
            val neededBy by androidx.compose.runtime.produceState(initialValue = emptyList<String>(), mod.file, modsDir, loader) {
                if (modsDir != null) {
                    val all = com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker.scanDir(modsDir, loader)
                    value = com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker
                        .dependentsOf(listOf(mod.file), all)[mod.file].orEmpty().map { it.name }
                }
            }
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = {
                    Text(text = stringResource(R.string.mods_manage_delete_warning, mod.name))
                    DependentModsWarning(
                        title = stringResource(R.string.cryo_dep_delete_warning),
                        names = neededBy
                    )
                },
                onCancel = {
                    updateOperation(ModsOperation.None)
                },
                onDismissRequest = {
                    updateOperation(ModsOperation.None)
                },
                onConfirm = {
                    onDelete(mod)
                    updateOperation(ModsOperation.None)
                }
            )
        }
    }
}

@Composable
fun ModsUpdateOperation(
    operation: ModsUpdateOperation,
    changeOperation: (ModsUpdateOperation) -> Unit,
    modsUpdater: ModUpdater?,
    onUpdate: (List<RemoteMod>) -> Unit,
    onCancel: () -> Unit
) {
    when (operation) {
        is ModsUpdateOperation.None -> {}
        is ModsUpdateOperation.Warning -> {
            //警告更新模组可能带来问题
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = {
                    Text(text = stringResource(R.string.mods_update_warning_1))
                    Text(text = stringResource(R.string.mods_update_warning_2))
                    Text(text = stringResource(R.string.mods_update_warning_3))
                },
                confirmText = stringResource(R.string.mods_update),
                onCancel = {
                    changeOperation(ModsUpdateOperation.None)
                },
                onConfirm = {
                    onUpdate(operation.mods)
                }
            )
        }
        is ModsUpdateOperation.Update -> {
            if (modsUpdater != null) {
                val tasks = modsUpdater.tasksFlow.collectAsStateWithLifecycle()
                if (tasks.value.isNotEmpty()) {
                    //更新模组流程对话框
                    TitleTaskFlowDialog(
                        title = stringResource(R.string.mods_update),
                        tasks = tasks.value,
                        onCancel = {
                            onCancel()
                            changeOperation(ModsUpdateOperation.None)
                        }
                    )
                }
            }
        }
        is ModsUpdateOperation.Error -> {
            val th = operation.th
            Logger.error("UpdateMods", "Failed to update the mods", th)
            val message = when (th) {
                is HttpRequestTimeoutException, is SocketTimeoutException, is TimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is SerializationException, is JsonSyntaxException -> stringResource(R.string.error_parse_failed)
                is JvmCrashException -> stringResource(R.string.download_install_error_jvm_crash, th.code)
                is DownloadFailedException -> stringResource(R.string.download_install_error_download_failed)
                else -> when {
                    th.isProcessStartRefused() -> stringResource(R.string.download_install_error_process_start)
                    else -> th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                }
            }
            val dismiss = {
                changeOperation(ModsUpdateOperation.None)
            }
            AlertDialog(
                onDismissRequest = dismiss,
                title = {
                    Text(text = stringResource(R.string.mods_update_failed))
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .verticalScrollWithBar(state = scrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = stringResource(R.string.mods_update_failed_text))
                        Text(text = message)
                    }
                },
                confirmButton = {
                    CryoButton(onClick = dismiss) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            )
        }
        is ModsUpdateOperation.Success -> {
            SimpleAlertDialog(
                title = stringResource(R.string.mods_update_success),
                text = stringResource(R.string.mods_update_success_text)
            ) {
                changeOperation(ModsUpdateOperation.None)
            }
        }
    }
}

@Composable
fun ModsConfirmOperation(
    operation: ModsConfirmOperation,
    onCancel: () -> Unit,
    onConfirm: (List<SelectableModManifest>) -> Unit
) {
    when (operation) {
        is ModsConfirmOperation.None -> {}
        is ModsConfirmOperation.WaitingConfirm -> {
            val list = operation.list
            ModsUpdateListDialog(
                manifests = list,
                skipped = operation.skipped,
                onCancel = {
                    onCancel()
                },
                onConfirm = {
                    onConfirm(list)
                }
            )
        }
    }
}

/**
 * 模组更新：展示需要更新的模组的详细信息
 */
@Composable
private fun ModsUpdateListDialog(
    manifests: List<SelectableModManifest>,
    skipped: List<com.russoviller.cryogenic.game.version.mod.update.SkippedMod>,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val selectedStates = manifests.map { it.selected.collectAsStateWithLifecycle() }
    Dialog(
        onDismissRequest = {}
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.mods_update_task_show),
                        style = MaterialTheme.typography.titleMedium
                    )
                    //Resumo: quantos podem ser atualizados e quantos ficaram sem atualização
                    Text(
                        text = stringResource(R.string.cryo_mods_update_summary, manifests.size, skipped.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    //Selecionar / desmarcar todos de uma vez
                    if (manifests.size > 1) {
                        val allSelected = selectedStates.all { it.value }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { manifests.forEach { it.updateSelected(!allSelected) } }
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { value -> manifests.forEach { it.updateSelected(value) } }
                            )
                            Text(
                                text = stringResource(R.string.cryo_mods_update_select_all, manifests.size),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    val scrollState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .weight(1f, fill = false)
                            .nonInteractiveScrollbar(
                                state = scrollState.scrollIndicatorState!!,
                                orientation = Orientation.Vertical,
                            ),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (manifests.isNotEmpty()) {
                            item(key = "header_updates") {
                                UpdateSectionHeader(stringResource(R.string.cryo_mods_update_available, manifests.size))
                            }
                        }
                        items(manifests) { entry ->
                            ModsUpdateEntryItem(
                                modifier = Modifier.fillMaxWidth(),
                                entry = entry
                            )
                        }
                        if (skipped.isNotEmpty()) {
                            item(key = "header_skipped") {
                                UpdateSectionHeader(stringResource(R.string.cryo_mods_update_without, skipped.size))
                            }
                            items(skipped) { mod ->
                                SkippedModItem(modifier = Modifier.fillMaxWidth(), mod = mod)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CryoTonalButton(
                            modifier = Modifier.weight(0.5f),
                            onClick = onCancel
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        val selectedCount = selectedStates.count { it.value }
                        CryoButton(
                            modifier = Modifier.weight(0.5f),
                            enabled = selectedCount > 0,
                            onClick = onConfirm
                        ) {
                            MarqueeText(
                                text = if (manifests.isEmpty()) stringResource(R.string.mods_update)
                                else "${stringResource(R.string.mods_update)} ($selectedCount)"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 模组更新：单个模组更新详情展示
 * 展示旧版本与新版本对比
 */
@Composable
private fun ModsUpdateEntryItem(
    entry: SelectableModManifest,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
) {
    val context = LocalContext.current

    val data = entry.data
    val newVersion = entry.new
    val selected by entry.selected.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        onClick = {
            entry.updateSelected(!selected)
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssetsIcon(
                    modifier = Modifier.clip(shape = RoundedCornerShape(12.dp)),
                    size = 52.dp,
                    iconUrl = data.project.iconUrl
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val title = data.project.title
                    val displayTitle = data.mcMod?.getMcmodTitle(title, context) ?: title
                    MarqueeText(
                        modifier = Modifier.fillMaxWidth(),
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    //旧版本
                    MarqueeText(
                        modifier = Modifier.fillMaxWidth(),
                        text = data.currentVersion ?: "???", //未知
                        style = MaterialTheme.typography.labelSmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                    //新版本
                    MarqueeText(
                        modifier = Modifier.fillMaxWidth(),
                        text = newVersion.platformVersion(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Checkbox(
                checked = selected,
                onCheckedChange = { value ->
                    entry.updateSelected(value)
                }
            )
        }
    }
}

@Composable
private fun UpdateSectionHeader(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    )
}

/** Mod que ficou sem atualização, com o motivo. */
@Composable
private fun SkippedModItem(
    mod: com.russoviller.cryogenic.game.version.mod.update.SkippedMod,
    modifier: Modifier = Modifier
) {
    val reason = when (mod.reason) {
        com.russoviller.cryogenic.game.version.mod.update.SkippedMod.Reason.UP_TO_DATE -> R.string.cryo_mods_skip_up_to_date
        com.russoviller.cryogenic.game.version.mod.update.SkippedMod.Reason.NO_COMPATIBLE -> R.string.cryo_mods_skip_no_compatible
        com.russoviller.cryogenic.game.version.mod.update.SkippedMod.Reason.NOT_FOUND -> R.string.cryo_mods_skip_not_found
        com.russoviller.cryogenic.game.version.mod.update.SkippedMod.Reason.FAILED -> R.string.cryo_mods_skip_failed
    }
    val warn = mod.reason != com.russoviller.cryogenic.game.version.mod.update.SkippedMod.Reason.UP_TO_DATE
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = itemColor(false).copy(alpha = 0.6f),
        contentColor = onItemColor()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetsIcon(
                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp)),
                size = 40.dp,
                iconUrl = mod.iconUrl
            )
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    modifier = Modifier.fillMaxWidth(),
                    text = mod.name,
                    style = MaterialTheme.typography.titleSmall
                )
                MarqueeText(
                    modifier = Modifier.fillMaxWidth(),
                    text = listOfNotNull(mod.currentVersion, stringResource(reason)).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (warn) Color(0xFFFFB454) else com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora
                )
            }
        }
    }
}

enum class ModStateFilter(val textRes: Int) {
    All(R.string.generic_all),
    Enabled(R.string.generic_enabled),
    Disabled(R.string.generic_disabled)
}

/**
 * 根据名称，筛选模组
 */
fun List<RemoteMod>.filterMods(
    nameFilter: String,
    stateFilter: ModStateFilter = All,
    context: Context? = null
) = this.filter { mod ->
    val matchesName = nameFilter.isEmpty() || (
            mod.localMod.file.name.contains(nameFilter, true) ||
            mod.localMod.name.contains(nameFilter, true) ||
            mod.projectInfo?.title?.contains(nameFilter, true) == true ||
            mod.mcMod?.getMcmodTitle(mod.localMod.name, context)?.contains(nameFilter, true) == true
    )

    val matchesState = when (stateFilter) {
        All -> true
        Enabled -> mod.localMod.file.isEnabled()
        Disabled -> !mod.localMod.file.isEnabled()
    }

    matchesName && matchesState
}
