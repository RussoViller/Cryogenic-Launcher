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

package com.russoviller.cryogenic.filemanager.ui.dialogs

import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.filemanager.ui.components.FmDialogHeader
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.filemanager.logic.entry.FmEntry
import com.russoviller.cryogenic.filemanager.ui.components.ExposedDropdown
import com.russoviller.cryogenic.filemanager.ui.components.FmDialogSurface
import com.russoviller.cryogenic.filemanager.ui.components.FmEditDialog
import com.russoviller.cryogenic.filemanager.ui.components.PropertyRow
import com.russoviller.cryogenic.filemanager.ui.components.fmFilenameInvalid
import com.russoviller.cryogenic.filemanager.ui.theme.fmErrorColor
import com.russoviller.cryogenic.filemanager.viewmodel.DirScanUiState
import com.russoviller.cryogenic.ui.components.OwnOutlinedTextField
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.utils.file.formatFileSize

private enum class CreateType {
    File,
    Folder
}

@Composable
private fun CreateType.stringResource(): String {
    return when (this) {
        CreateType.File -> stringResource(R.string.fm_new_file)
        CreateType.Folder -> stringResource(R.string.resource_pack_manage_type_folder)
    }
}

/**
 * 新建文件或文件夹对话框。
 * @param onConfirmFile 以输入的名称创建文件
 * @param onConfirmFolder 以输入的名称创建文件夹
 */
@Composable
fun FmCreateDialog(
    onDismiss: () -> Unit,
    onConfirmFile: (String) -> Unit,
    onConfirmFolder: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CreateType.File) }

    // 实时校验文件名合法性，错误就地标注在输入框下方
    val filenameError = key(name) { fmFilenameInvalid(name) }
    val isError = name.isEmpty() || filenameError != null

    FmDialogSurface(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.control_editor_layers_create),
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            OwnOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.fm_new_name)) },
                isError = isError,
                supportingText = {
                    when {
                        name.isEmpty() -> Text(stringResource(R.string.generic_cannot_empty))
                        filenameError != null -> Text(filenameError)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            ExposedDropdown(
                modifier = Modifier.fillMaxWidth(),
                selectedText = type.stringResource(),
                label = stringResource(R.string.fm_property_type),
                options = CreateType.entries,
                optionLabel = { type ->
                    type.stringResource()
                },
                onSelect = { type0 ->
                    type = type0
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            CryoTonalButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.generic_cancel))
            }

            CryoButton(
                enabled = !isError,
                onClick = {
                    when (type) {
                        CreateType.File -> onConfirmFile(name)
                        CreateType.Folder -> onConfirmFolder(name)
                    }
                }
            ) {
                Text(stringResource(R.string.generic_confirm))
            }
        }
    }
}

/**
 * 跳转目录对话框。
 * @param currentPath 输入框默认填充的当前路径
 * @param onConfirm 以校验通过的目标路径跳转
 */
@Composable
fun FmJumpDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // 初始化时默认将光标定位到路径末尾，便于直接追加修改
    var input by remember {
        mutableStateOf(TextFieldValue(currentPath, TextRange(currentPath.length)))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val emptyError = stringResource(R.string.fm_jump_invalid)

    FmEditDialog(
        title = stringResource(R.string.fm_jump_to),
        value = input,
        onValueChange = {
            input = it
            error = null
        },
        label = {
            Text(stringResource(R.string.fm_jump_hint))
        },
        isError = error != null,
        supportingText = error?.let {
            { Text(it) }
        },
        singleLine = true,
        onDismissRequest = onDismiss,
        onCancel = onDismiss,
        onConfirm = {
            if (input.text.isBlank()) {
                error = emptyError
            } else {
                keyboard?.hide()
                onConfirm(input.text.trim())
            }
        }
    )
}

/**
 * 删除确认对话框，可选择是否放入回收站
 * @param count 待删除的条目数量
 * @param onConfirm 确认删除，参数表示是否放入回收站
 */
@Composable
fun FmDeleteConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: (toTrash: Boolean) -> Unit
) {
    var toTrash by remember { mutableStateOf(true) }
    val accent = if (toTrash) CryoGlassColors.Ice else CryoDanger

    FmDialogSurface(onDismissRequest = onDismiss) {
        FmDialogHeader(
            title = stringResource(R.string.generic_delete),
            iconRes = R.drawable.ic_delete_filled,
            kicker = stringResource(R.string.cryo_fm_delete_kicker),
            accent = CryoDanger
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(CryoDanger.copy(alpha = 0.22f), CryoGlassColors.Deep)))
                    .border(1.dp, CryoDanger.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.cryo_fm_items),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CryoDanger
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.fm_delete, count),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeleteModeCard(
                selected = toTrash,
                iconRes = R.drawable.ic_restore_from_trash_filled,
                title = stringResource(R.string.fm_move_to_trash),
                summary = stringResource(R.string.cryo_fm_trash_summary),
                accent = CryoGlassColors.Ice,
                onClick = { toTrash = true }
            )
            DeleteModeCard(
                selected = !toTrash,
                iconRes = R.drawable.ic_delete_forever_filled,
                title = stringResource(R.string.cryo_fm_delete_forever),
                summary = stringResource(R.string.fm_permanent_delete),
                accent = CryoDanger,
                onClick = { toTrash = false }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CryoTonalButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                MarqueeText(text = stringResource(R.string.generic_cancel))
            }
            CryoButton(
                modifier = Modifier.weight(1f),
                colors = if (toTrash) null else androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = CryoDanger,
                    contentColor = CryoGlassColors.Deep
                ),
                onClick = { onConfirm(toTrash) }
            ) {
                MarqueeText(text = stringResource(R.string.generic_delete))
            }
        }
    }
}

@Composable
private fun DeleteModeCard(
    selected: Boolean,
    iconRes: Int,
    title: String,
    summary: String,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(accent.copy(alpha = 0.25f), CryoGlassColors.Deep.copy(alpha = 0.6f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.02f)))
            )
            .border(if (selected) 1.5.dp else 1.dp, accent.copy(alpha = if (selected) 0.85f else 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accent
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.75f)
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, accent.copy(alpha = if (selected) 1f else 0.4f), CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
        )
    }
}

/**
 * 重命名对话框，实时校验并就地提示错误。
 * @param entry 待重命名的条目
 * @param initialName 输入框的初始名称
 * @param isFile 是否为文件，决定默认选中范围
 * @param onConfirm 以校验通过的新名称确认重命名
 * @param validate 名称校验函数，返回 null 表示通过，否则返回错误提示文本
 */
@Composable
fun FmRenameDialog(
    entry: FmEntry,
    initialName: String,
    isFile: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    validate: (FmEntry, String) -> String?
) {
    val initialSelection: TextRange = if (isFile) {
        val dot = initialName.lastIndexOf('.')
        if (dot > 0) {
            TextRange(0, dot)
        } else {
            TextRange(0, initialName.length)
        }
    } else {
        TextRange(0, initialName.length)
    }
    var tfv by remember { mutableStateOf(TextFieldValue(initialName, initialSelection)) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    // 输入变化即校验，有错误时禁用确认
    val err = key(tfv.text) { validate(entry, tfv.text) }

    FmEditDialog(
        title = stringResource(R.string.generic_rename),
        value = tfv,
        onValueChange = { tfv = it },
        isError = err != null,
        supportingText = err?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        focusRequester = focus,
        iconRes = if (isFile) R.drawable.ic_edit_filled else R.drawable.ic_folder_filled,
        kicker = stringResource(R.string.cryo_fm_rename_kicker),
        extraBody = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CryoGlassColors.Deep.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                text = stringResource(R.string.cryo_fm_rename_from, entry.name),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                maxLines = 1
            )
        },
        confirmEnabled = err == null,
        onDismissRequest = onDismiss,
        onCancel = onDismiss,
        onConfirm = {
            // 有错误时确认已禁用，此处直接提交
            keyboard?.hide()
            onConfirm(tfv.text)
        }
    )
}

/**
 * 文件属性对话框。
 * @param name 条目名称
 * @param path 条目完整路径
 * @param isDirectory 是否为目录
 * @param sizeText 大小文本
 * @param modifiedText 修改时间文本
 * @param dirScan 目录扫描状态，用于展示目录的异步统计结果
 * @param onDismiss 关闭对话框
 */
@Composable
fun FmPropertyDialog(
    name: String,
    path: String,
    isDirectory: Boolean,
    sizeText: String,
    modifiedText: String,
    dirScan: DirScanUiState? = null,
    onDismiss: () -> Unit
) {
    val typeText = if (isDirectory) {
        stringResource(R.string.resource_pack_manage_type_folder)
    } else {
        stringResource(R.string.fm_new_file)
    }
    val dirStats = dirScan?.stats
    val scanning = dirScan?.running == true
    val calculating = stringResource(R.string.fm_calculating)
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    FmDialogSurface(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.russoviller.cryogenic.filemanager.ui.components.FmIcons.IconFor(name = name, isDirectory = isDirectory)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cryo_fm_property_kicker),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                    color = CryoGlassColors.Ice
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2
                )
            }
        }
        val scroll = androidx.compose.foundation.rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scroll)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sizeValue = if (isDirectory) {
                if (dirStats != null) formatFileSize(dirStats.totalSize) else if (scanning) calculating else "-"
            } else sizeText
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(label = stringResource(R.string.fm_property_size), value = sizeValue)
                StatTile(label = stringResource(R.string.fm_property_type), value = typeText)
            }
            if (isDirectory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = stringResource(R.string.fm_property_files),
                        value = dirStats?.fileCount?.toString() ?: if (scanning) "…" else "-"
                    )
                    StatTile(
                        label = stringResource(R.string.fm_property_folders),
                        value = dirStats?.dirCount?.toString() ?: if (scanning) "…" else "-"
                    )
                }
            }
            if (scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            InfoBlock(label = stringResource(R.string.fm_property_modified), value = modifiedText)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CryoGlassColors.Deep.copy(alpha = 0.6f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.fm_property_path).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                        color = CryoGlassColors.Ice
                    )
                    Icon(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { clipboard.setText(androidx.compose.ui.text.AnnotatedString(path)) }
                            .padding(5.dp),
                        painter = painterResource(R.drawable.ic_content_copy_filled),
                        contentDescription = null,
                        tint = CryoGlassColors.Ice
                    )
                }
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White
                )
            }
        }
        CryoButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
            MarqueeText(text = stringResource(R.string.generic_close))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatTile(label: String, value: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.14f), CryoGlassColors.Deep.copy(alpha = 0.6f))))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
            color = CryoGlassColors.Ice,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Visible
        )
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
            color = CryoGlassColors.Ice
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White
        )
    }
}
