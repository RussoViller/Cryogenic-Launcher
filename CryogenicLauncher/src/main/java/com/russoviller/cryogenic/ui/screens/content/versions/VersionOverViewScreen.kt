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

package com.russoviller.cryogenic.ui.screens.content.versions

import com.russoviller.cryogenic.ui.components.CryoOutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.context.copyLocalFile
import com.russoviller.cryogenic.contract.MediaPickerContract
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionFolders
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedColumn
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleEditDialog
import com.russoviller.cryogenic.ui.components.SimpleTaskDialog
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.DeleteVersionDialog
import com.russoviller.cryogenic.ui.screens.content.elements.ImportFileButton
import com.russoviller.cryogenic.ui.screens.content.elements.RenameVersionDialog
import com.russoviller.cryogenic.ui.screens.content.home.version.VersionCardDir
import com.russoviller.cryogenic.ui.screens.content.home.version.VersionCardManager
import com.russoviller.cryogenic.ui.screens.content.versions.layouts.VersionChunkBackground
import com.russoviller.cryogenic.ui.screens.content.versions.layouts.VersionOverviewItem
import com.russoviller.cryogenic.utils.file.ensureDirectory
import com.russoviller.cryogenic.utils.image.isImageFile
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import org.apache.commons.io.FileUtils
import java.io.File

private const val TAG = "VersionOverView"

@Composable
fun VersionOverViewScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    backToMainScreen: () -> Unit,
    onExport: () -> Unit,
    eventViewModel: EventViewModel,
    version: Version,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.OverView, versionsScreenKey, false)
    ) { isVisible ->
        var versionSummary by remember { mutableStateOf(version.getVersionSummary()) }
        var refreshVersionIcon by remember { mutableIntStateOf(0) }

        var iconFileExists by remember { mutableStateOf(version.getVersionIconFile().exists()) }

        var versionsOperation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }
        VersionsOperation(
            versionsOperation = versionsOperation,
            updateOperation = { versionsOperation = it },
            submitError = submitError,
            resetIcon = {
                val iconFile = version.getVersionIconFile()
                FileUtils.deleteQuietly(iconFile)
                refreshVersionIcon++
                iconFileExists = iconFile.exists()
            },
            setVersionSummary = { value ->
                version.getVersionConfig().apply {
                    this.versionSummary = value
                    save()
                }
                versionSummary = version.getVersionSummary()
            }
        )

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                VersionInfoLayout(
                    modifier = Modifier.animated(ctx),
                    version = version,
                    versionSummary = versionSummary,
                    iconFileExists = iconFileExists,
                    submitError = submitError,
                    refreshKey = refreshVersionIcon,
                    onIconPicked = {
                        iconFileExists = version.getVersionIconFile().exists()
                        versionsOperation = VersionsOperation.None
                        refreshVersionIcon++
                    },
                    resetIcon = { versionsOperation = VersionsOperation.ResetIconAlert }
                )
            }

            AnimatedItem { ctx ->
                VersionManagementLayout(
                    modifier = Modifier.animated(ctx),
                    version = version,
                    onShareLog = { logFile ->
                        eventViewModel.sendEvent(
                            EventViewModel.Event.LogShare.ShareGameLog(logFile)
                        )
                    },
                    onEditSummary = { versionsOperation = VersionsOperation.EditSummary(version) },
                    onRename = { versionsOperation = VersionsOperation.Rename(version) },
                    onExport = onExport,
                    onDelete = { versionsOperation = VersionsOperation.Delete(version) }
                )
            }

            AnimatedItem { ctx ->
                VersionQuickActions(
                    modifier = Modifier.animated(ctx),
                    accessFolder = { path ->
                        val folder = if (path.isEmpty()) {
                            version.getGameDir()
                        } else {
                            File(version.getGameDir(), path)
                        }
                        runCatching {
                            folder.ensureDirectory()
                        }.onFailure { e ->
                            submitError(
                                ErrorViewModel.ThrowableMessage(
                                    title = androidText(R.string.error_create_dir, folder.absolutePath),
                                    message = androidText(e.getMessageOrToString())
                                )
                            )
                            return@VersionQuickActions
                        }
                        eventViewModel.sendEvent(
                            EventViewModel.Event.OpenFileManager(
                                rootPath = version.getGameHome(),
                                currentPath = folder.absolutePath,
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun VersionInfoLayout(
    modifier: Modifier = Modifier,
    version: Version,
    versionSummary: String,
    iconFileExists: Boolean,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    refreshKey: Any? = null,
    onIconPicked: () -> Unit = {},
    resetIcon: () -> Unit = {}
) {
    val context = LocalContext.current
    val errorImportImageText = stringResource(R.string.error_import_image)
    val iconFile = remember {
        version.getVersionIconFile()
    }

    VersionChunkBackground(
        modifier = modifier,
        paddingValues = PaddingValues(all = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionOverviewItem(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f),
                version = version,
                versionSummary = versionSummary,
                refreshKey = refreshKey
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImportFileButton(
                    contract = MediaPickerContract(
                        allowImages = true,
                        allowVideos = false,
                        allowMultiple = false
                    ),
                    onLaunch = { launcher ->
                        launcher.launch(Unit)
                    },
                    progressOutput = { uri ->
                        uri?.get(0)?.let { result ->
                            TaskSystem.submitTask(
                                Task.runTask(
                                    dispatcher = Dispatchers.IO,
                                    task = {
                                        context.copyLocalFile(result, iconFile)
                                        if (!iconFile.isImageFile()) error("The selected file is not an image!")
                                    },
                                    onError = { e ->
                                        Logger.error(TAG, "Failed to import icon!", e)
                                        FileUtils.deleteQuietly(iconFile)
                                        submitError(
                                            ErrorViewModel.ThrowableMessage(
                                                title = androidText(errorImportImageText),
                                                message = androidText(e.getMessageOrToString())
                                            )
                                        )
                                    },
                                    onFinally = onIconPicked
                                )
                            )
                        }
                    },
                    painter = painterResource(R.drawable.ic_image_outlined),
                    text = stringResource(R.string.versions_overview_custom_version_icon)
                )
                if (iconFileExists) {
                    IconTextButton(
                        onClick = resetIcon,
                        painter = painterResource(R.drawable.ic_restart_alt),
                        contentDescription = stringResource(R.string.versions_overview_reset_version_icon),
                        text = stringResource(R.string.versions_overview_reset_version_icon)
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionManagementLayout(
    modifier: Modifier = Modifier,
    version: Version,
    onShareLog: (File) -> Unit,
    onEditSummary: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val logFile = remember(version) {
        version.getLatestLog()
    }
    val logExists = remember(logFile) {
        logFile.exists()
    }

    VersionChunkBackground(
        modifier = modifier,
        paddingValues = PaddingValues(all = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                text = stringResource(R.string.versions_settings_overview_management),
                style = MaterialTheme.typography.labelLarge
            )

            VersionActionGrid(
                actions = listOf(
                    VersionAction(R.drawable.ic_description_outlined, R.string.versions_overview_edit_version_summary, onClick = onEditSummary),
                    VersionAction(R.drawable.ic_edit_outlined, R.string.versions_manage_rename_version, onClick = onRename),
                    VersionAction(R.drawable.ic_file_export_outlined, R.string.versions_export, onClick = onExport),
                    VersionAction(
                        R.drawable.ic_terminal_outlined,
                        R.string.versions_overview_log,
                        enabled = logExists,
                        onClick = { onShareLog(logFile) }
                    ),
                    VersionAction(R.drawable.ic_delete_outlined, R.string.versions_manage_delete_version, danger = true, onClick = onDelete)
                )
            )
        }
    }
}

@Composable
private fun VersionQuickActions(
    modifier: Modifier = Modifier,
    accessFolder: (folderName: String) -> Unit = {}
) {
    VersionChunkBackground(
        modifier = modifier,
        paddingValues = PaddingValues(all = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                text = stringResource(R.string.versions_settings_overview_quick_actions),
                style = MaterialTheme.typography.labelLarge
            )

            VersionActionGrid(
                actions = listOf(
                    VersionAction(R.drawable.ic_folder_outlined, R.string.versions_overview_version_folder) { accessFolder("") },
                    VersionAction(R.drawable.ic_save_filled, R.string.versions_overview_saves_folder) { accessFolder(VersionFolders.SAVES.folderName) },
                    VersionAction(R.drawable.ic_format_paint_outlined, R.string.versions_overview_resource_pack_folder) { accessFolder(VersionFolders.RESOURCE_PACK.folderName) },
                    VersionAction(R.drawable.ic_lightbulb, R.string.versions_overview_shaders_pack_folder) { accessFolder(VersionFolders.SHADERS.folderName) },
                    VersionAction(R.drawable.ic_extension_outlined, R.string.versions_overview_mod_folder) { accessFolder(VersionFolders.MOD.folderName) },
                    VersionAction(R.drawable.ic_photo_library_outlined, R.string.versions_overview_screenshot_folder) { accessFolder("screenshots") },
                    VersionAction(R.drawable.ic_article_outlined, R.string.versions_overview_logs_folder) { accessFolder("logs") },
                    VersionAction(R.drawable.ic_warning_outlined, R.string.versions_overview_crash_report_folder) { accessFolder("crash-reports") }
                )
            )
        }
    }
}

/**
 * Ação exibida como bloco na grade de ações da visão geral da versão.
 */
private class VersionAction(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

private val VersionActionGap = 10.dp
private val VersionActionMinWidth = 200.dp

/**
 * Grade responsiva de ações: blocos do mesmo tamanho, com espaço fixo entre linhas e colunas.
 * A quantidade de colunas acompanha a largura disponível (1 a 4).
 */
@Composable
private fun VersionActionGrid(
    actions: List<VersionAction>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(bottom = 4.dp)
    ) {
        val columns = ((maxWidth + VersionActionGap) / (VersionActionMinWidth + VersionActionGap))
            .toInt()
            .coerceIn(1, 4)
        Column(verticalArrangement = Arrangement.spacedBy(VersionActionGap)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(VersionActionGap)
                ) {
                    rowActions.forEach { action ->
                        VersionActionTile(
                            action = action,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    repeat(columns - rowActions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionActionTile(
    action: VersionAction,
    modifier: Modifier = Modifier
) {
    val accent = if (action.danger) MaterialTheme.colorScheme.error else CryoGlassColors.Ice
    CryoOutlinedButton(
        modifier = modifier.heightIn(min = 56.dp),
        enabled = action.enabled,
        onClick = action.onClick,
        colors = if (action.danger) {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else null,
        border = if (action.danger) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
        } else null,
        contentPadding = PaddingValues(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(19.dp),
                painter = painterResource(action.icon),
                contentDescription = null,
                tint = accent
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(action.label),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier
                .size(18.dp)
                .alpha(0.55f),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null
        )
    }
}

/**
 * 版本概览操作
 */
sealed interface VersionsOperation {
    data object None: VersionsOperation
    data object ResetIconAlert: VersionsOperation
    data class EditSummary(val version: Version): VersionsOperation
    data class Rename(val version: Version): VersionsOperation
    data class Delete(val version: Version): VersionsOperation
    data class RunTask(val title: Int, val task: suspend () -> Unit): VersionsOperation
}

@Composable
private fun VersionsOperation(
    versionsOperation: VersionsOperation,
    updateOperation: (VersionsOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    resetIcon: () -> Unit = {},
    setVersionSummary: (String) -> Unit = {}
) {
    when(versionsOperation) {
        is VersionsOperation.None -> {}
        is VersionsOperation.ResetIconAlert -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.versions_overview_reset_version_icon_message),
                onDismiss = { updateOperation(VersionsOperation.None) },
                onConfirm = {
                    resetIcon()
                    updateOperation(VersionsOperation.None)
                }
            )
        }
        is VersionsOperation.Rename -> {
            RenameVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateOperation(VersionsOperation.None) },
                onConfirm = {
                    updateOperation(
                        VersionsOperation.RunTask(
                            title = R.string.versions_manage_rename_version,
                            task = {
                                VersionsManager.renameVersion(versionsOperation.version, it)
                                VersionCardManager.onVersionRenamed(
                                    gameHome = versionsOperation.version.getGameHome(),
                                    oldName = versionsOperation.version.getVersionName(),
                                    newName = it
                                )
                            }
                        )
                    )
                }
            )
        }
        is VersionsOperation.Delete -> {
            val version = versionsOperation.version

            DeleteVersionDialog(
                version = version,
                onDismissRequest = { updateOperation(VersionsOperation.None) },
                onConfirm = { title, task ->
                    updateOperation(
                        VersionsOperation.RunTask(
                            title = title,
                            task = task
                        )
                    )
                }
            )
        }
        is VersionsOperation.EditSummary -> {
            val version = versionsOperation.version
            var value by remember { mutableStateOf(version.getVersionConfig().versionSummary) }

            SimpleEditDialog(
                title = stringResource(R.string.versions_overview_edit_version_summary),
                value = value,
                onValueChange = { value = it },
                label = {
                    Text(text = stringResource(R.string.versions_overview_edit_version_summary_label))
                },
                singleLine = true,
                onDismissRequest = { updateOperation(VersionsOperation.None) },
                onConfirm = {
                    setVersionSummary(value)
                    updateOperation(VersionsOperation.None)
                }
            )
        }
        is VersionsOperation.RunTask -> {
            val errorMessage = stringResource(R.string.versions_manage_task_error)
            SimpleTaskDialog(
                title = stringResource(versionsOperation.title),
                task = versionsOperation.task,
                context = Dispatchers.IO,
                onDismiss = { updateOperation(VersionsOperation.None) },
                onError = { e ->
                    Logger.error(TAG, "Failed to run task.", e)
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = androidText(errorMessage),
                            message = androidText(e.getMessageOrToString())
                        )
                    )
                }
            )
        }
    }
}

