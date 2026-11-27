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

package com.russoviller.cryogenic.ui.screens.content.elements

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoGlass
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.ui.components.cryoChamferTile
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import androidx.compose.foundation.layout.fillMaxSize
import com.russoviller.cryogenic.ui.components.CryoButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import com.russoviller.cryogenic.game.path.GamePath
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.game.version.installed.cleanup.CleanFailedException
import com.russoviller.cryogenic.game.version.installed.cleanup.GameAssetCleaner
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.components.LittleTextLabel
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleCheckEditDialog
import com.russoviller.cryogenic.ui.components.SimpleEditDialog
import com.russoviller.cryogenic.ui.components.SimpleTaskDialog
import com.russoviller.cryogenic.ui.components.TextRailItem
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.content.home.version.VersionCardDir
import com.russoviller.cryogenic.ui.screens.content.home.version.VersionCardManager
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.utils.string.isNotEmptyOrBlank
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

private const val TAG = "VersionsManageElements"

sealed interface GamePathOperation {
    data object None: GamePathOperation
    data object PathExists: GamePathOperation
    data class AddNewPath(val path: String): GamePathOperation
    data class RenamePath(val item: GamePath): GamePathOperation
    data class DeletePath(val item: GamePath): GamePathOperation
}

sealed interface VersionsOperation {
    data object None: VersionsOperation
    data class Rename(val version: Version): VersionsOperation
    data class Copy(val version: Version): VersionsOperation
    data class Delete(val version: Version, val text: String? = null): VersionsOperation
    data class InvalidDelete(val version: Version): VersionsOperation
    data class RunTask(val title: Int, val task: suspend () -> Unit): VersionsOperation
}

sealed interface CleanupOperation {
    data object None : CleanupOperation
    /** 清理前的提醒 */
    data object Tip : CleanupOperation
    /** 开始清理 */
    data object Clean : CleanupOperation
    /** 清理失败 */
    data class Error(val error: Throwable) : CleanupOperation
    /** 成功清除 */
    data class Success(val count: Int, val size: String) : CleanupOperation
}

enum class VersionCategory(val textRes: Int) {
    /** 全部 */
    ALL(R.string.generic_all),
    /** 原版 */
    VANILLA(R.string.versions_manage_category_vanilla),
    /** 带有模组加载器 */
    MODLOADER(R.string.versions_manage_category_modloader)
}

@Composable
fun GamePathItemLayout(
    item: GamePath,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    enabled: Boolean = true,
) {
    val notDefault = item.id != GamePathManager.DEFAULT_ID
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DisabledAlpha)
            .then(if (selected) Modifier.cryoGlow(10.dp, selected = true) else Modifier)
            .cryoChamferTile(10.dp, selected = selected)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (notDefault) item.title else stringResource(R.string.versions_manage_game_path_default),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.path,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis
            )
        }
        Box {
            Icon(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { menuExpanded = !menuExpanded }
                    .padding(5.dp),
                painter = painterResource(R.drawable.ic_more_horiz),
                contentDescription = stringResource(R.string.generic_more),
                tint = CryoGlassColors.Ice
            )
            DropdownMenu(
                expanded = menuExpanded,
                shape = MaterialTheme.shapes.large,
                containerColor = CryoGlassColors.Navy.copy(alpha = 0.97f),
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    enabled = notDefault,
                    text = { Text(text = stringResource(R.string.generic_rename)) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.ic_edit_filled),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onRename()
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    enabled = notDefault,
                    text = { Text(text = stringResource(R.string.generic_delete)) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.ic_delete_filled),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onDelete()
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GamePathOperation(
    gamePathOperation: GamePathOperation,
    changeState: (GamePathOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    fun doRunCatching(block: () -> Unit) {
        runCatching {
            block()
        }.onFailure { e ->
            submitError(
                ErrorViewModel.ThrowableMessage(
                    title = androidText(R.string.versions_manage_game_path_error_title),
                    message = androidText(e.getMessageOrToString())
                )
            )
        }
    }
    when (gamePathOperation) {
        is GamePathOperation.None -> {}
        is GamePathOperation.AddNewPath -> {
            NameEditPathDialog(
                onDismissRequest = { changeState(GamePathOperation.None) },
                onConfirm = { value ->
                    if (GamePathManager.containsPath(gamePathOperation.path)) {
                        changeState(GamePathOperation.PathExists)
                    } else {
                        doRunCatching {
                            GamePathManager.addNewPath(title = value, path = gamePathOperation.path)
                        }
                        changeState(GamePathOperation.None)
                    }
                }
            )
        }
        is GamePathOperation.RenamePath -> {
            NameEditPathDialog(
                initValue = gamePathOperation.item.title,
                onDismissRequest = { changeState(GamePathOperation.None) },
                onConfirm = { value ->
                    doRunCatching {
                        GamePathManager.modifyTitle(gamePathOperation.item, value)
                    }
                    changeState(GamePathOperation.None)
                }
            )
        }
        is GamePathOperation.DeletePath -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_game_path_delete_title),
                text = stringResource(R.string.versions_manage_game_path_delete_message),
                onDismiss = { changeState(GamePathOperation.None) },
                onConfirm = {
                    doRunCatching {
                        GamePathManager.removePath(gamePathOperation.item)
                    }
                    changeState(GamePathOperation.None)
                }
            )
        }
        is GamePathOperation.PathExists -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_game_path_exists_title),
                text = stringResource(R.string.versions_manage_game_path_exists_message),
                onDismiss = { changeState(GamePathOperation.None) }
            )
        }
    }
}

@Composable
fun VersionCategoryItem(
    modifier: Modifier = Modifier,
    value: VersionCategory,
    versionsCount: Int,
    selected: Boolean,
    shape: Shape = MaterialTheme.shapes.large,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    TextRailItem(
        modifier = modifier,
        text = {
            Text(
                text = stringResource(value.textRes),
                style = style
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($versionsCount)",
                style = style
            )
        },
        onClick = onClick,
        selected = selected,
        shape = shape,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        enabled = enabled
    )
}

@Composable
private fun NameEditPathDialog(
    initValue: String = "",
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String) -> Unit = {}
) {
    var value by remember { mutableStateOf(initValue) }
    SimpleEditDialog(
        title = stringResource(R.string.versions_manage_game_path_add_new),
        value = value,
        onValueChange = { value = it },
        label = { Text(text = stringResource(R.string.versions_manage_game_path_edit_title)) },
        isError = value.isEmpty(),
        supportingText = {
            if (value.isEmpty()) Text(text = stringResource(R.string.generic_cannot_empty))
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (value.isNotEmptyOrBlank()) {
                onConfirm(value.trim())
            }
        }
    )
}

@Composable
fun VersionsOperation(
    versionsOperation: VersionsOperation,
    updateVersionsOperation: (VersionsOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    when(versionsOperation) {
        is VersionsOperation.None -> {}
        is VersionsOperation.Rename -> {
            RenameVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = {
                    updateVersionsOperation(
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
        is VersionsOperation.Copy -> {
            CopyVersionDialog(
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = { name, copyAll ->
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = R.string.versions_manage_copy_version,
                            task = { VersionsManager.copyVersion(versionsOperation.version, name, copyAll) }
                        )
                    )
                }
            )
        }
        is VersionsOperation.InvalidDelete -> {
            updateVersionsOperation(
                VersionsOperation.Delete(
                    versionsOperation.version,
                    stringResource(R.string.versions_manage_delete_version_tip_invalid)
                )
            )
        }
        is VersionsOperation.Delete -> {
            val version = versionsOperation.version
            DeleteVersionDialog(
                version = version,
                message = versionsOperation.text,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = { title, task ->
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = title,
                            task = task
                        )
                    )
                }
            )
        }
        is VersionsOperation.RunTask -> {
            SimpleTaskDialog(
                title = stringResource(versionsOperation.title),
                task = versionsOperation.task,
                context = Dispatchers.IO,
                onDismiss = { updateVersionsOperation(VersionsOperation.None) },
                onError = { e ->
                    Logger.error(TAG, "Failed to run task.", e)
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = androidText(R.string.versions_manage_task_error),
                            message = androidText(e.getMessageOrToString())
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun RenameVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String) -> Unit = {}
) {
    var name by remember { mutableStateOf(version.getVersionName()) }

    val filenameInvalidMessage = key(name) {
        isVersionNameInvalid(name)
    }

    val isVersionExists = remember(name) {
        VersionsManager.isVersionExists(name, true)
    }
    val isError = name.isEmpty() || filenameInvalidMessage != null || isVersionExists

    SimpleEditDialog(
        title = stringResource(R.string.versions_manage_rename_version),
        value = name,
        onValueChange = { name = it },
        isError = isError,
        supportingText = {
            when {
                name.isEmpty() -> Text(stringResource(R.string.generic_cannot_empty))
                filenameInvalidMessage != null -> Text(filenameInvalidMessage)
                isVersionExists -> Text(stringResource(R.string.versions_manage_install_exists))
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (!isError) {
                onConfirm(name)
            }
        }
    )
}

@Composable
fun CopyVersionDialog(
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String, copyAll: Boolean) -> Unit = { _, _ -> }
) {
    var copyAll by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    val filenameInvalidMessage = key(name) {
        isVersionNameInvalid(name)
    }

    val isVersionExists = remember(name) {
        VersionsManager.isVersionExists(name, true)
    }
    val isError = name.isEmpty() || filenameInvalidMessage != null || isVersionExists

    SimpleCheckEditDialog(
        title = stringResource(R.string.versions_manage_copy_version),
        text = stringResource(R.string.versions_manage_copy_version_tip),
        checkBoxText = stringResource(R.string.versions_manage_copy_version_all),
        checked = copyAll,
        value = name,
        onCheckedChange = { copyAll = it },
        onValueChange = { name = it },
        isError = isError,
        supportingText = {
            when {
                name.isEmpty() -> Text(stringResource(R.string.generic_cannot_empty))
                filenameInvalidMessage != null -> Text(filenameInvalidMessage)
                isVersionExists -> Text(stringResource(R.string.versions_manage_install_exists))
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (!isError) {
                onConfirm(name, copyAll)
            }
        }
    )
}

@Composable
fun DeleteVersionDialog(
    version: Version,
    message: String? = null,
    onDismissRequest: () -> Unit = {},
    onConfirm: (title: Int, task: suspend () -> Unit) -> Unit = { _, _ -> },
    onVersionDeleted: () -> Unit = {}
) {
    val deleteVersion = {
        onConfirm(R.string.versions_manage_delete_version) {
            VersionsManager.deleteVersion(version)
            onVersionDeleted()
        }
    }

    if (message != null) {
        SimpleAlertDialog(
            title = stringResource(R.string.versions_manage_delete_version),
            text = message,
            onDismiss = onDismissRequest,
            onConfirm = deleteVersion
        )
    } else {
        SimpleAlertDialog(
            title = stringResource(R.string.versions_manage_delete_version),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.versions_manage_delete_version_tip_hint1, version.getVersionName()))
                    Text(text = stringResource(R.string.versions_manage_delete_version_tip_hint2))
                    Text(text = stringResource(R.string.versions_manage_delete_version_tip_hint3))
                    Text(
                        text = stringResource(R.string.versions_manage_delete_version_tip_hint4),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            onConfirm = deleteVersion,
            onCancel = onDismissRequest,
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
fun CleanupOperation(
    operation: CleanupOperation,
    changeOperation: (CleanupOperation) -> Unit,
    cleaner: GameAssetCleaner?,
    onClean: () -> Unit,
    onCancel: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    when(operation) {
        is CleanupOperation.None -> {}
        is CleanupOperation.Tip -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_cleanup),
                text = {
                    Text(stringResource(R.string.versions_manage_cleanup_tip))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.versions_manage_cleanup_warning))
                    Text("../assets/..")
                    //不再清理依赖库，文件并不会太大，也有可能导致其他问题：#617
//                    Text("../libraries/..")
                },
                onConfirm = onClean,
                onCancel = { changeOperation(CleanupOperation.None) }
            )
        }
        is CleanupOperation.Clean -> {
            if (cleaner != null) {
                val tasks = cleaner.tasksFlow.collectAsStateWithLifecycle()
                if (tasks.value.isNotEmpty()) {
                    //清理无用游戏文件流程对话框
                    TitleTaskFlowDialog(
                        title = stringResource(R.string.versions_manage_cleanup),
                        tasks = tasks.value,
                        onCancel = {
                            onCancel()
                            changeOperation(CleanupOperation.None)
                        }
                    )
                }
            }
        }
        is CleanupOperation.Error -> {
            val error = operation.error
            if (error is CleanFailedException) {
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Text(
                            text = stringResource(R.string.generic_warning),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fadeEdge(state = scrollState)
                                .verticalScrollWithBar(state = scrollState)
                        ) {
                            Text(stringResource(R.string.versions_manage_cleanup_failed_files))
                            error.files.forEach { file ->
                                Text(file.absolutePath)
                            }
                        }
                    },
                    confirmButton = {
                        CryoButton(onClick = { changeOperation(CleanupOperation.None) }) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                    }
                )
            } else {
                changeOperation(CleanupOperation.None)
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.versions_manage_cleanup_failed),
                        message = androidText(error.getMessageOrToString())
                    )
                )
            }
        }
        is CleanupOperation.Success -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_cleanup),
                text = stringResource(R.string.versions_manage_cleanup_success, operation.count, operation.size)
            ) {
                changeOperation(CleanupOperation.None)
            }
        }
    }
}

@Composable
fun VersionItemLayout(
    version: Version,
    selected: Boolean,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    onSelected: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onPinned: () -> Unit = {}
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    val ice = CryoGlassColors.Ice
    val isValid = remember(version) { version.isValid() }
    val versionInfo = remember(version) { version.getVersionInfo() }
    CompositionLocalProvider(LocalContentColor provides CryoGlassColors.Frost) {
        Row(
            modifier = modifier
                .graphicsLayer { scaleY = scale.value; scaleX = scale.value }
                .then(if (selected) Modifier.cryoGlow(14.dp, selected = true) else Modifier)
                .clip(cryoChamferShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            CryoGlassColors.Deep.copy(alpha = 0.78f),
                            CryoGlassColors.Navy.copy(alpha = 0.55f),
                            CryoGlassColors.Deep.copy(alpha = 0.35f)
                        )
                    )
                )
                .cryoChamferTile(14.dp, selected = selected)
                .clickable {
                    if (!selected) onSelected()
                }
                .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .cryoBackground(
                        if (selected) cryoGradient()
                        else Brush.verticalGradient(listOf(ice.copy(alpha = 0.35f), ice.copy(alpha = 0.05f)))
                    )
            )
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(cryoChamferShape(14.dp))
                    .cryoBackground(
                        if (selected) cryoGradient()
                        else Brush.linearGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep))
                    )
                    .padding(3.dp)
                    .clip(cryoChamferShape(12.dp))
                    .background(CryoGlassColors.Deep),
                contentAlignment = Alignment.Center
            ) {
                VersionIconImage(
                    modifier = Modifier.fillMaxSize(),
                    version = version,
                    fill = true
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected) VersionChip(stringResource(R.string.cryo_versions_in_use), CryoGlassColors.Aurora)
                }
                if (isValid && version.isSummaryValid()) {
                    Text(
                        text = remember(version) { version.getVersionSummary() },
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isValid) {
                        VersionChip(stringResource(R.string.versions_manage_invalid), MaterialTheme.colorScheme.error)
                    } else {
                        versionInfo?.let { info ->
                            VersionChip("MC " + info.minecraftVersion, ice)
                            info.loaderInfos.forEach { loader ->
                                VersionChip(loader.loader.displayName + " " + loader.version, CryoGlassColors.Aurora)
                            }
                        }
                    }
                }
            }

            VersionActionIcon(
                enabled = isValid,
                highlighted = version.pinnedState,
                onClick = {
                    val currentValue = version.pinnedState
                    runCatching {
                        version.setPinnedAndSave(!currentValue)
                    }.onFailure { e ->
                        Logger.error(TAG, "Failed to save version config!", e)
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(R.string.versions_config_failed_to_save),
                                message = androidText(e.getMessageOrToString())
                            )
                        )
                    }.onSuccess {
                        onPinned()
                    }
                }
            ) {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(45f),
                    painter = painterResource(if (version.pinnedState) R.drawable.ic_pinned_filled else R.drawable.ic_pinned_outlined),
                    contentDescription = stringResource(R.string.versions_manage_pin)
                )
            }
            VersionActionIcon(
                enabled = isValid,
                highlighted = true,
                onClick = onSettingsClick
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_settings_filled),
                    contentDescription = stringResource(R.string.versions_manage_settings)
                )
            }

            Row {
                var menuExpanded by remember { mutableStateOf(false) }

                VersionActionIcon(
                    enabled = true,
                    highlighted = false,
                    onClick = { menuExpanded = !menuExpanded }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_more_horiz),
                        contentDescription = stringResource(R.string.generic_more)
                    )
                }

                if (menuExpanded) {
                    CryoVersionActionsPopup(
                        versionName = version.getVersionName(),
                        onDismiss = { menuExpanded = false },
                        onRename = { menuExpanded = false; onRenameClick() },
                        onCopy = { menuExpanded = false; onCopyClick() },
                        onExport = { menuExpanded = false; onExportClick() },
                        onDelete = { menuExpanded = false; onDeleteClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CryoVersionActionsPopup(
    versionName: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offset = with(density) { androidx.compose.ui.unit.IntOffset(0, 42.dp.roundToPx()) }
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, androidx.compose.animation.core.tween(160))
    }
    androidx.compose.ui.window.Popup(
        alignment = Alignment.TopEnd,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = appear.value
                    scaleX = 0.92f + 0.08f * appear.value
                    scaleY = 0.92f + 0.08f * appear.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                }
                .width(270.dp)
                .cryoGlow(20.dp, selected = true, strength = 0.6f)
                .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp, opaque = true)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 26.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora)))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_version_actions_title),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = CryoGlassColors.Ice,
                        maxLines = 1
                    )
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = versionName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            CryoVersionActionTile(R.drawable.ic_edit_filled, stringResource(R.string.generic_rename), stringResource(R.string.cryo_version_action_rename_desc), onRename)
            CryoVersionActionTile(R.drawable.ic_file_copy_filled, stringResource(R.string.generic_copy), stringResource(R.string.cryo_version_action_copy_desc), onCopy)
            CryoVersionActionTile(R.drawable.ic_folder_zip_filled, stringResource(R.string.versions_export), stringResource(R.string.cryo_version_action_export_desc), onExport)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, CryoGlassColors.Ice.copy(alpha = 0.35f), Color.Transparent)))
            )
            CryoVersionActionTile(R.drawable.ic_delete_filled, stringResource(R.string.generic_delete), stringResource(R.string.cryo_version_action_delete_desc), onDelete, danger = true)
        }
    }
}

@Composable
private fun CryoVersionActionTile(
    iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val accent = if (danger) Color(0xFFFF6B8A) else CryoGlassColors.Ice
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = if (danger) 0.14f else 0.10f), CryoGlassColors.Navy.copy(alpha = 0.35f))
                )
            )
            .border(1.dp, accent.copy(alpha = if (danger) 0.45f else 0.22f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .cryoBackground(
                    if (danger) Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.15f)))
                    else cryoGradient()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (danger) accent else CryoGlassColors.Deep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (danger) accent else Color.White,
                maxLines = 1
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_arrow_right_rounded),
            contentDescription = null,
            tint = accent.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CommonVersionInfoLayout(
    version: Version,
    modifier: Modifier = Modifier,
    iconSize: Dp = 34.dp,
) {
    val isValid = remember(version) { version.isValid() }
    val versionName = remember(version) { version.getVersionName() }
    val isSummaryValid = remember(version) { version.isSummaryValid() }
    val versionInfo = remember(version) { version.getVersionInfo() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        VersionIconImage(
            modifier = Modifier.size(iconSize),
            version = version
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            //版本名称
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                maxLines = 1,
                text = versionName,
                style = MaterialTheme.typography.labelLarge
            )
            //版本描述
            if (isValid && isSummaryValid) {
                val versionSummary = remember(version) {
                    version.getVersionSummary()
                }

                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    maxLines = 1,
                    text = versionSummary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            //版本详细信息
            FlowRow(
                modifier = Modifier.alpha(0.7f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isValid) {
                    LittleTextLabel(
                        text = stringResource(R.string.versions_manage_invalid),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                } else {
                    versionInfo?.let { versionInfo ->
                        Text(
                            text = versionInfo.minecraftVersion,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        versionInfo.loaderInfos.forEach { loaderInfo ->
                            Text(
                                text = loaderInfo.loader.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = loaderInfo.version,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VersionIconImage(
    version: Version?,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null,
    fill: Boolean = false
) {
    val defaultIconRes = remember(version) {
        version?.let { getLoaderIconRes(it.getVersionInfo()?.primaryLoader?.loader) } ?: R.drawable.img_minecraft
    }
    val defaultIcon = painterResource(defaultIconRes)

    val model = remember(version, refreshKey) {
        version?.let {
            val iconFile = it.getVersionIconFile()
            if (iconFile.exists()) iconFile
            else null
        } ?: defaultIcon
    }

    when (model) {
        is Painter -> {
            Image(
                painter = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = if (fill) modifier.padding(14.dp) else modifier
            )
        }
        else -> {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = if (fill) ContentScale.Crop else ContentScale.Fit,
                modifier = modifier
            )
        }
    }
}

/**
 * 模组加载器图标展示组件，包装 [Image]
 */
@Composable
fun ModLoaderIcon(
    modloader: ModLoader,
    @DrawableRes
    defaultIcon: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    val icon = getLoaderIconRes(modloader, defaultIcon)
    Image(
        modifier = modifier,
        painter = painterResource(icon),
        contentDescription = null,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

private fun getLoaderIconRes(
    loader: ModLoader?,
    @DrawableRes
    defaultIcon: Int = R.drawable.img_minecraft,
): Int {
    return when (loader) {
        ModLoader.FABRIC,
        ModLoader.BABRIC -> R.drawable.img_loader_fabric
        ModLoader.LEGACY_FABRIC -> R.drawable.img_loader_legacy_fabric

        ModLoader.FORGE -> R.drawable.img_anvil
        ModLoader.QUILT -> R.drawable.img_loader_quilt
        ModLoader.NEOFORGE -> R.drawable.img_loader_neoforge
        ModLoader.OPTIFINE -> R.drawable.img_loader_optifine
        ModLoader.LITE_LOADER -> R.drawable.img_chicken_old
        else -> defaultIcon
    }
}

@Composable
private fun VersionChip(text: String, color: Color) {
    com.russoviller.cryogenic.ui.components.CryoTag(text = text, accent = color)
}

@Composable
private fun VersionActionIcon(
    enabled: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .cryoBackground(
                if (highlighted) cryoGradient()
                else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.10f), CryoGlassColors.Ice.copy(alpha = 0.04f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (highlighted) 0f else 0.4f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (highlighted) CryoGlassColors.Deep else CryoGlassColors.Ice) {
            content()
        }
    }
}
