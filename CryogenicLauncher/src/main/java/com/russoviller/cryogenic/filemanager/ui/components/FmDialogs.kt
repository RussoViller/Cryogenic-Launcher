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

package com.russoviller.cryogenic.filemanager.ui.components

import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGradient
import androidx.compose.foundation.background
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.filemanager.ui.theme.fmCardColor
import com.russoviller.cryogenic.filemanager.ui.theme.fmOnCardColor
import com.russoviller.cryogenic.ui.components.ImePanContainer
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.OwnOutlinedTextField
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.imePanAnchor
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar

@Composable
internal fun FmDialogHeader(
    title: String,
    iconRes: Int? = null,
    kicker: String? = null,
    accent: androidx.compose.ui.graphics.Color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (iconRes != null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.35f), com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep)
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        modifier = Modifier.size(22.dp),
                        painter = androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = null,
                        tint = accent
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (kicker ?: stringResource(R.string.cryo_fm_dialog_kicker)).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                    ),
                    color = accent,
                    maxLines = 1
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(androidx.compose.ui.graphics.Color.White, accent)
                        )
                    ),
                    maxLines = 2
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.8f), accent.copy(alpha = 0.15f), androidx.compose.ui.graphics.Color.Transparent)
                    )
                )
        )
    }
}

@Composable
internal fun FmDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val shape = com.russoviller.cryogenic.ui.components.cryoChamferShape(22.dp)
            Column(
                modifier = modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight()
                    .cryoGlass(shape = shape, cornerRadius = 22.dp, opaque = true)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
internal fun FmDialogButtons(
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CryoTonalButton(
            modifier = Modifier.weight(1f),
            onClick = onDismiss
        ) {
            MarqueeText(text = dismissText)
        }

        CryoButton(
            modifier = Modifier.weight(1f),
            enabled = confirmEnabled,
            onClick = onConfirm
        ) {
            MarqueeText(text = confirmText)
        }
    }
}


/** 显示带文本输入框的编辑对话框 */
@Composable
fun FmEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    confirmText: String = stringResource(R.string.generic_confirm),
    confirmEnabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    iconRes: Int? = null,
    kicker: String? = null,
    onDismissRequest: () -> Unit = {},
    onCancel: () -> Unit = onDismissRequest,
    onConfirm: () -> Unit = {}
) {
    FmDialogSurface(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        FmDialogHeader(title = title, iconRes = iconRes, kicker = kicker)

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fadeEdge(state = scrollState)
                .weight(1f, fill = false)
                .verticalScrollWithBar(state = scrollState)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            extraBody?.let {
                it()
                Spacer(modifier = Modifier.size(8.dp))
            }

            val focusManager = LocalFocusManager.current
            OwnOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                label = label,
                isError = isError,
                supportingText = supportingText,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = if (singleLine) {
                    keyboardOptions.copy(imeAction = ImeAction.Done)
                } else {
                    keyboardOptions
                },
                keyboardActions = keyboardActions ?: KeyboardActions(
                    onDone = {
                        focusManager.clearFocus(true)
                        onConfirm()
                    }
                ),
                visualTransformation = visualTransformation,
                shape = MaterialTheme.shapes.large
            )
            extraContent?.invoke()
        }

        FmDialogButtons(
            confirmText = confirmText,
            confirmEnabled = confirmEnabled,
            onConfirm = onConfirm,
            onDismiss = onCancel
        )
    }
}

/** 显示带文本输入框的编辑对话框 */
@Composable
fun FmEditDialog(
    title: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    confirmText: String = stringResource(R.string.generic_confirm),
    confirmEnabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    iconRes: Int? = null,
    kicker: String? = null,
    onDismissRequest: () -> Unit = {},
    onCancel: () -> Unit = onDismissRequest,
    onConfirm: () -> Unit = {}
) {
    focusRequester?.let {
        LaunchedEffect(Unit) {
            it.requestFocus()
        }
    }

    FmDialogSurface(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        FmDialogHeader(title = title, iconRes = iconRes, kicker = kicker)

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fadeEdge(state = scrollState)
                .weight(1f, fill = false)
                .verticalScrollWithBar(state = scrollState)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            extraBody?.let {
                it()
                Spacer(modifier = Modifier.size(8.dp))
            }

            val focusManager = LocalFocusManager.current
            label?.let { lbl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 4.dp)
                ) {
                    androidx.compose.material3.ProvideTextStyle(
                        MaterialTheme.typography.labelMedium.copy(color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice)
                    ) { lbl() }
                }
            }
            OutlinedTextField(
                modifier = Modifier
                    .imePanAnchor()
                    .fillMaxWidth()
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
                value = value,
                onValueChange = onValueChange,
                label = null,
                isError = isError,
                supportingText = supportingText,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = if (singleLine) {
                    keyboardOptions.copy(imeAction = ImeAction.Done)
                } else {
                    keyboardOptions
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus(true)
                        onConfirm()
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = androidx.compose.ui.graphics.Color.White
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice,
                    unfocusedBorderColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.35f),
                    focusedContainerColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.6f),
                    unfocusedContainerColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.45f),
                    cursorColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice,
                    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                        handleColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice,
                        backgroundColor = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.35f)
                    )
                )
            )
            extraContent?.invoke()
        }

        FmDialogButtons(
            confirmText = confirmText,
            confirmEnabled = confirmEnabled,
            onConfirm = onConfirm,
            onDismiss = onCancel
        )
    }
}

/** 显示带文本输入框与复选框的编辑对话框 */
@Composable
fun FmCheckEditDialog(
    title: String,
    text: String? = null,
    value: String,
    checked: Boolean,
    checkBoxText: String? = null,
    onValueChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    confirmText: String = stringResource(R.string.generic_confirm),
    confirmEnabled: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onCancel: () -> Unit = onDismissRequest,
    onConfirm: () -> Unit = {}
) {
    FmEditDialog(
        title = title,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        extraBody = text?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        extraContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange
                    )
                    checkBoxText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmText = confirmText,
        confirmEnabled = confirmEnabled,
        onDismissRequest = onDismissRequest,
        onCancel = onCancel,
        onConfirm = onConfirm
    )
}


/** 通用下拉选择框 */
@Composable
fun <T> ExposedDropdown(
    selectedText: String,
    label: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OwnOutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
fun <T> FmChoiceChips(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val active = option == selected
                val shape = RoundedCornerShape(12.dp)
                Text(
                    modifier = Modifier
                        .clip(shape)
                        .then(
                            if (active) Modifier.cryoBackground(cryoGradient())
                            else Modifier.background(com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.5f))
                        )
                        .border(
                            1.dp,
                            com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = if (active) 0.9f else 0.3f),
                            shape
                        )
                        .androidxClickable { onSelect(option) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        shadow = null
                    ),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (active) com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep else androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}

private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(onClick = onClick)
)

@Composable
fun FmToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.04f))
            .border(1.dp, com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost.copy(alpha = 0.7f)
                )
            }
        }
        com.russoviller.cryogenic.ui.components.DefaultSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
