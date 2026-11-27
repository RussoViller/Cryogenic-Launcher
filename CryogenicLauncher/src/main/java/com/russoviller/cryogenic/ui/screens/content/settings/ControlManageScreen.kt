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

package com.russoviller.cryogenic.ui.screens.content.settings

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russoviller.layer_controller.data.lang.createTranslatable
import com.russoviller.layer_controller.layout.ControlLayout
import com.russoviller.layer_controller.layout.EmptyControlLayout
import com.russoviller.layer_controller.layout.EmptyLayoutInfo
import com.russoviller.layer_controller.observable.ObservableControlLayout
import com.russoviller.layer_controller.observable.ObservableTranslatableString
import com.russoviller.layer_controller.utils.AUTHOR_NAME_LENGTH
import com.russoviller.layer_controller.utils.NAME_LENGTH
import com.russoviller.layer_controller.utils.VERSION_NAME_LENGTH
import com.russoviller.layer_controller.utils.newRandomFileName
import com.russoviller.layer_controller.utils.saveToFile
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.control.ControlData
import com.russoviller.cryogenic.game.control.ControlManager
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.activities.startEditorActivity
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.AnimatedRow
import com.russoviller.cryogenic.ui.components.BackgroundCard
import com.russoviller.cryogenic.ui.components.CardTitleLayout
import com.russoviller.cryogenic.ui.components.EdgeDirection
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.OwnOutlinedTextField
import com.russoviller.cryogenic.ui.components.ScalingActionButton
import com.russoviller.cryogenic.ui.components.ScalingLabel
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleEditDialog
import com.russoviller.cryogenic.ui.components.SingleLineTextCheck
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.ImportMultipleFileButton
import com.russoviller.cryogenic.ui.screens.main.control_editor.edit_translatable.EditTranslatableTextDialog
import com.russoviller.cryogenic.ui.screens.main.control_editor.orUnspecified
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.file.shareFile
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.utils.string.isEmptyOrBlank
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.Locale

private sealed interface ControlOperation {
    data object None : ControlOperation
    /** 创建新布局弹窗 */
    data object CreateNew : ControlOperation
    /** 删除控制布局 */
    data class Delete(val data: ControlData) : ControlOperation
    /** 编辑普通的文本 */
    data class EditText(
        val data: ControlData,
        val string: ObservableTranslatableString,
        val type: EditTextType
    ) : ControlOperation
    /** 编辑描述 */
    data class EditDescription(val data: ControlData) : ControlOperation
    /** 编辑版本名称 */
    data class EditVersion(val data: ControlData) : ControlOperation
}

private enum class EditTextType(val length: Int, val titleRes: Int, val allowEmpty: Boolean) {
    NAME(length = NAME_LENGTH, titleRes = R.string.control_manage_create_new_name, false),
    AUTHOR(length = AUTHOR_NAME_LENGTH, titleRes = R.string.control_manage_create_new_author, true)
}

private class ControlViewModel : ViewModel() {
    var operation by mutableStateOf<ControlOperation>(ControlOperation.None)

    fun createNew(
        layout: ControlLayout,
        submitError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            saveToNew(layout, submitError)
        }
    }

    fun copyNew(
        layout: ObservableControlLayout,
        submitError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            saveToNew(layout.pack(), submitError)
        }
    }

    private suspend fun saveToNew(
        layout: ControlLayout,
        submitError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val file = File(PathManager.DIR_CONTROL_LAYOUTS, "${newRandomFileName()}.json")
            try {
                layout.saveToFile(file)
            } catch (e: Exception) {
                submitError(e)
                FileUtils.deleteQuietly(file)
            }
            ControlManager.refresh()
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

@Composable
private fun rememberControlViewModel() = viewModel(
    key = NormalNavKey.Settings.ControlManager.toString()
) {
    ControlViewModel()
}

@Composable
fun ControlManageScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val viewModel = rememberControlViewModel()
    val dataList by ControlManager.dataList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    ControlOperation(
        operation = viewModel.operation,
        changeOperation = { viewModel.operation = it },
        onCreate = { name, author, versionName ->
            val layout = EmptyControlLayout.copy(
                info = EmptyLayoutInfo.copy(
                    name = createTranslatable(default = name),
                    author = createTranslatable(default = author),
                    versionName = versionName
                )
            )
            viewModel.createNew(layout) { e ->
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.control_manage_failed_to_save),
                        message = androidText(e.getMessageOrToString())
                    )
                )
            }
        },
        onDelete = { data ->
            ControlManager.deleteControl(data)
        },
        onSave = { data ->
            ControlManager.saveControl(data) { e ->
                ErrorViewModel.ThrowableMessage(
                    title = androidText(R.string.control_manage_failed_to_save),
                    message = androidText(e.getMessageOrToString())
                )
            }
        }
    )

    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.ControlManager, settingsScreenKey, false)
    ) { isVisible ->
        val selectedLayout by ControlManager.selectedLayout.collectAsStateWithLifecycle()
        val isRefreshing by ControlManager.isRefreshing.collectAsStateWithLifecycle()

        AnimatedRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                ControlLayoutList(
                    modifier = Modifier
                        .weight(0.45f)
                        .padding(end = 6.dp)
                        .animated(ctx, from = AnimatedDirection.Left),
                    dataList = dataList,
                    locale = locale,
                    isLoading = isRefreshing,
                    onRefresh = {
                        ControlManager.refresh()
                    },
                    onCreate = {
                        viewModel.operation = ControlOperation.CreateNew
                    },
                    onCopy = { data ->
                        viewModel.copyNew(data.controlLayout) { e ->
                            submitError(
                                ErrorViewModel.ThrowableMessage(
                                    title = androidText(R.string.control_manage_failed_to_save),
                                    message = androidText(e.getMessageOrToString())
                                )
                            )
                        }
                    },
                    onDelete = { data ->
                        viewModel.operation = ControlOperation.Delete(data)
                    },
                    eventViewModel = eventViewModel,
                )
            }

            AnimatedItem { ctx ->
                ControlLayoutInfo(
                    modifier = Modifier
                        .weight(0.55f)
                        .padding(start = 6.dp)
                        .animated(ctx, from = AnimatedDirection.Right),
                    isLoading = isRefreshing,
                    data = selectedLayout,
                    locale = locale,
                    onShareLayout = { data ->
                        shareFile(context, data.file)
                    },
                    onEditLayout = { data ->
                        startEditorActivity(context, data.file)
                    },
                    onEditText = { data, string, type ->
                        viewModel.operation = ControlOperation.EditText(data, string, type)
                    },
                    onEditDescription = { data ->
                        viewModel.operation = ControlOperation.EditDescription(data)
                    },
                    onEditVersion = { data ->
                        viewModel.operation = ControlOperation.EditVersion(data)
                    }
                )
            }
        }
    }
}

/**
 * 控制布局相关操作
 */
@Composable
private fun ControlOperation(
    operation: ControlOperation,
    changeOperation: (ControlOperation) -> Unit,
    onCreate: (name: String, author: String, versionName: String) -> Unit,
    onDelete: (ControlData) -> Unit,
    onSave: (ControlData) -> Unit
) {
    when (operation) {
        is ControlOperation.None -> {}
        is ControlOperation.CreateNew -> {
            CreateNewLayoutDialog(
                onDismissRequest = { changeOperation(ControlOperation.None) },
                onCreate = onCreate
            )
        }
        is ControlOperation.Delete -> {
            val data = operation.data
            val layoutName = if (data.isSupport) {
                data.controlLayout.info.name.translate().orUnspecified()
            } else {
                data.file.name
            }
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.control_manage_delete_message, layoutName),
                onDismiss = {
                    changeOperation(ControlOperation.None)
                },
                onConfirm = {
                    onDelete(data)
                    changeOperation(ControlOperation.None)
                }
            )
        }
        is ControlOperation.EditText -> {
            EditTranslatableTextDialog(
                title = stringResource(operation.type.titleRes),
                text = operation.string,
                onDismissRequest = {
                    operation.string.reset()
                    changeOperation(ControlOperation.None)
                },
                onClose = {
                    if (!operation.type.allowEmpty) {
                        if (operation.string.default.isEmptyOrBlank()) return@EditTranslatableTextDialog
                        if (operation.string.matchQueue.any { it.value.isEmptyOrBlank() }) return@EditTranslatableTextDialog
                    }
                    onSave(operation.data)
                    changeOperation(ControlOperation.None)
                },
                allowEmpty = operation.type.allowEmpty,
                closeText = stringResource(R.string.generic_save),
                take = operation.type.length
            )
        }
        is ControlOperation.EditDescription -> {
            val string = operation.data.controlLayout.info.description
            EditTranslatableTextDialog(
                title = stringResource(R.string.control_manage_info_description),
                text = string,
                singleLine = false,
                onDismissRequest = {
                    string.reset()
                    changeOperation(ControlOperation.None)
                },
                onClose = {
                    onSave(operation.data)
                    changeOperation(ControlOperation.None)
                },
                closeText = stringResource(R.string.generic_save)
            )
        }
        is ControlOperation.EditVersion -> {
            val info = operation.data.controlLayout.info
            SimpleEditDialog(
                title = stringResource(R.string.control_manage_create_new_version_name),
                value = info.versionName,
                onValueChange = {
                    info.versionName = it.take(VERSION_NAME_LENGTH)
                },
                supportingText = {
                    Text(stringResource(R.string.generic_input_length, info.versionName.length, VERSION_NAME_LENGTH))
                },
                singleLine = true,
                onDismissRequest = {
                    info.resetVersionName()
                    changeOperation(ControlOperation.None)
                },
                onConfirm = {
                    onSave(operation.data)
                    changeOperation(ControlOperation.None)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ControlLayoutList(
    modifier: Modifier = Modifier,
    dataList: List<ControlData>,
    locale: Locale,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onCopy: (ControlData) -> Unit,
    onDelete: (ControlData) -> Unit,
    eventViewModel: EventViewModel,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp)
    ) {
        ControlListHeader(
            modifier = Modifier.fillMaxWidth(),
            count = dataList.size,
            onRefresh = onRefresh,
            onCreate = onCreate,
            eventViewModel = eventViewModel,
        )
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else if (dataList.isNotEmpty()) {
            val scrollState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = scrollState,
            ) {
                items(dataList, key = { it.file.name }) { data ->
                    ControlLayoutItem(
                        modifier = Modifier.fillMaxWidth(),
                        data = data,
                        locale = locale,
                        selected = data.file.name == AllSettings.controlLayout.state,
                        onSelected = { ControlManager.selectControl(data) },
                        onCopy = { onCopy(data) },
                        onDelete = { onDelete(data) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ScalingLabel(text = stringResource(R.string.control_manage_list_empty))
            }
        }
    }
}

@Composable
private fun HudRoundButton(iconRes: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CryoGlassColors.Ice.copy(alpha = 0.10f))
            .border(1.dp, cryoBorderBrush(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = CryoGlassColors.Ice
        )
    }
}

@Composable
private fun ControlListHeader(
    modifier: Modifier = Modifier,
    count: Int,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    eventViewModel: EventViewModel,
) {
    Column(modifier = modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cryo_hud_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    ),
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.cryo_hud_count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudRoundButton(R.drawable.ic_refresh, stringResource(R.string.generic_refresh), onRefresh)
                HudRoundButton(R.drawable.ic_add_box_outlined, stringResource(R.string.control_manage_create_new), onCreate)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CryoGlassColors.Ice.copy(alpha = 0.08f))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) {
            ImportMultipleFileButton(
                extension = "json",
                progressUris = { uris ->
                    eventViewModel.sendEvent(EventViewModel.Event.ImportControls(uris))
                }
            )
        }
    }
}

@Composable
private fun ControlLayoutItem(
    modifier: Modifier = Modifier,
    data: ControlData,
    locale: Locale,
    selected: Boolean,
    onSelected: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val info = data.controlLayout.info
    val ice = CryoGlassColors.Ice
    val error = MaterialTheme.colorScheme.error
    Row(
        modifier = modifier
            .then(if (selected) Modifier.cryoGlow(16.dp, selected = true) else Modifier)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (selected) listOf(ice.copy(alpha = 0.26f), CryoGlassColors.Aurora.copy(alpha = 0.08f))
                    else listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f))
                )
            )
            .border(if (selected) 1.5.dp else 1.dp, if (selected) cryoBorderBrush(1.4f) else Brush.linearGradient(listOf(ice.copy(alpha = 0.2f), ice.copy(alpha = 0.2f))), shape)
            .clickable(enabled = data.isSupport && !selected, onClick = onSelected)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .cryoBackground(if (selected) cryoGradient() else Brush.linearGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_videogame_asset_outlined),
                contentDescription = null,
                tint = if (selected) CryoGlassColors.Deep else ice
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            MarqueeText(
                text = if (data.isSupport) info.name.translate(locale).orUnspecified() else data.file.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!data.isSupport) {
                    Text(
                        text = stringResource(R.string.control_manage_info_unsupport),
                        style = MaterialTheme.typography.labelSmall,
                        color = error,
                        maxLines = 1
                    )
                } else {
                    if (selected) HudChip(stringResource(R.string.cryo_hud_active), CryoGlassColors.Aurora)
                    if (!info.versionName.isEmptyOrBlank()) HudChip("v" + info.versionName, ice)
                }
            }
        }
        Icon(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onCopy)
                .padding(6.dp),
            painter = painterResource(R.drawable.ic_copy_all_outlined),
            contentDescription = stringResource(R.string.generic_copy),
            tint = CryoGlassColors.Frost
        )
        Icon(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(6.dp),
            painter = painterResource(R.drawable.ic_delete_outlined),
            contentDescription = stringResource(R.string.generic_delete),
            tint = error
        )
    }
}

@Composable
private fun HudChip(text: String, color: Color) {
    com.russoviller.cryogenic.ui.components.CryoTag(text = text, accent = color)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ControlLayoutInfo(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    data: ControlData? = null,
    locale: Locale,
    onShareLayout: (ControlData) -> Unit,
    onEditLayout: (ControlData) -> Unit,
    onEditText: (ControlData, ObservableTranslatableString, type: EditTextType) -> Unit,
    onEditDescription: (ControlData) -> Unit,
    onEditVersion: (ControlData) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp)
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ScalingLabel(text = stringResource(R.string.control_manage_info_empty))
            }
            !data.isSupport -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ScalingLabel(text = stringResource(R.string.control_manage_info_unsupport))
            }
            else -> {
                val info = data.controlLayout.info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(CryoGlassColors.Ice.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cryo_hud_selected),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = CryoGlassColors.Ice
                    )
                    MarqueeText(
                        text = info.name.translate(locale).orUnspecified(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice, CryoGlassColors.Aurora))
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ControlInfoTile(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_person_outlined,
                            title = stringResource(R.string.control_manage_create_new_author),
                            value = info.author.translate(locale).orUnspecified(),
                            onEdit = { onEditText(data, info.author, EditTextType.AUTHOR) }
                        )
                        ControlInfoTile(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_videogame_asset_outlined,
                            title = stringResource(R.string.control_manage_create_new_version_name),
                            value = info.versionName.orUnspecified(),
                            onEdit = { onEditVersion(data) }
                        )
                    }
                    ControlInfoTile(
                        modifier = Modifier.fillMaxWidth(),
                        iconRes = R.drawable.ic_edit_outlined,
                        title = stringResource(R.string.control_manage_create_new_name),
                        value = info.name.translate(locale).orUnspecified(),
                        onEdit = { onEditText(data, info.name, EditTextType.NAME) }
                    )
                    val description = info.description.translate(locale)
                    ControlInfoTile(
                        modifier = Modifier.fillMaxWidth(),
                        iconRes = R.drawable.ic_description_outlined,
                        title = stringResource(R.string.control_manage_info_description),
                        value = if (description.isEmptyOrBlank()) stringResource(R.string.control_manage_info_description_empty) else description,
                        multiline = true,
                        onEdit = { onEditDescription(data) }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onShareLayout(data) }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_share_filled),
                            contentDescription = null
                        )
                        MarqueeText(text = stringResource(R.string.generic_share))
                    }
                    CryoButton(
                        modifier = Modifier.weight(1.4f),
                        onClick = { onEditLayout(data) }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_edit_filled),
                            contentDescription = null
                        )
                        MarqueeText(text = stringResource(R.string.control_manage_info_edit))
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlInfoTile(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    value: String,
    multiline: Boolean = false,
    onEdit: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.22f), shape)
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = if (multiline) 6 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(R.drawable.ic_edit_outlined),
            contentDescription = null,
            tint = CryoGlassColors.Frost.copy(alpha = 0.5f)
        )
    }
}


/**
 * 创建新控制布局对话框
 */
@Composable
private fun CreateNewLayoutDialog(
    onDismissRequest: () -> Unit,
    onCreate: (name: String, author: String, versionName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0") }

    val blankError = stringResource(R.string.control_manage_create_new_field_blank)
    val longError = stringResource(R.string.control_manage_create_new_field_long)

    var nameError by remember { mutableStateOf<String?>(null) }
    val isNameError = remember(name) {
        nameError = when {
            name.isEmptyOrBlank() -> blankError
            name.length > NAME_LENGTH -> longError
            else -> null
        }
        nameError != null
    }
    var authorNameError by remember { mutableStateOf<String?>(null) }
    val isAuthorNameError = remember(author) {
        authorNameError = when {
//            author.isEmptyOrBlank() -> blankError
            author.length > AUTHOR_NAME_LENGTH -> longError
            else -> null
        }
        authorNameError != null
    }
    var versionNameError by remember { mutableStateOf<String?>(null) }
    val isVersionNameError = remember(versionName) {
        versionNameError = when {
//            versionName.isEmptyOrBlank() -> blankError
            versionName.length > VERSION_NAME_LENGTH -> longError
            else -> null
        }
        versionNameError != null
    }

    Dialog(
        onDismissRequest = onDismissRequest
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
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.control_manage_create_new_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .weight(1f, fill = false)
                            .verticalScrollWithBar(state = scrollState)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val focusManager = LocalFocusManager.current
                        val authorFocus = remember { FocusRequester() }
                        val versionNameFocus = remember { FocusRequester() }

                        SingleLineTextCheck(
                            text = name,
                            onSingleLined = { name = it }
                        )

                        //名称
                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = name,
                            onValueChange = {
                                name = it
                            },
                            label = {
                                Text(text = stringResource(R.string.control_manage_create_new_name))
                            },
                            isError = isNameError,
                            supportingText = {
                                nameError?.let { Text(it) } ?: run {
                                    Text(stringResource(R.string.generic_input_length, name.length, NAME_LENGTH))
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    authorFocus.requestFocus()
                                }
                            ),
                            shape = MaterialTheme.shapes.large
                        )

                        SingleLineTextCheck(
                            text = author,
                            onSingleLined = { author = it }
                        )

                        //作者
                        OwnOutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(authorFocus),
                            value = author,
                            onValueChange = {
                                author = it
                            },
                            label = {
                                Text(text = stringResource(R.string.control_manage_create_new_author))
                            },
                            isError = isAuthorNameError,
                            supportingText = {
                                authorNameError?.let { Text(it) } ?: run {
                                    Text(stringResource(R.string.generic_input_length, author.length, AUTHOR_NAME_LENGTH))
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    versionNameFocus.requestFocus()
                                }
                            ),
                            shape = MaterialTheme.shapes.large
                        )

                        SingleLineTextCheck(
                            text = versionName,
                            onSingleLined = { versionName = it }
                        )

                        //版本
                        OwnOutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(versionNameFocus),
                            value = versionName,
                            onValueChange = {
                                versionName = it
                            },
                            label = {
                                Text(text = stringResource(R.string.control_manage_create_new_version_name))
                            },
                            isError = isVersionNameError,
                            supportingText = {
                                versionNameError?.let { Text(it) } ?: run {
                                    Text(stringResource(R.string.generic_input_length, versionName.length, VERSION_NAME_LENGTH))
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus(true)
                                }
                            ),
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismissRequest
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        CryoButton(
                            modifier = Modifier.weight(1f),
                            onClick = onClick@{
                                if (isNameError || isAuthorNameError || isVersionNameError) return@onClick
                                onDismissRequest()
                                onCreate(name, author, versionName)
                            }
                        ) {
                            MarqueeText(text = stringResource(R.string.control_manage_create_new))
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewCreateNewLayoutDialog() {
    CreateNewLayoutDialog(
        onDismissRequest = {},
        onCreate = { _, _, _ -> }
    )
}
