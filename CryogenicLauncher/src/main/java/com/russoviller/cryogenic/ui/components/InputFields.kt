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

package com.russoviller.cryogenic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.string.toSingleLine

@Composable
fun SimpleTextInputField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    hint: (@Composable () -> Unit)? = null,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    shape: Shape = RoundedCornerShape(percent = 50),
    blur: Int = AllSettings.backgroundBlur.state,
    contextPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    textStyle: TextStyle = TextStyle(color = CryoGlassColors.Frost).copy(fontSize = 12.sp),
    cursorBrush: Brush = SolidColor(CryoGlassColors.Ice),
    singleLine: Boolean = false,
    imePanEnabled: Boolean = true,
) {
    Surface(
        modifier = (if (imePanEnabled) modifier.imePanAnchor() else modifier)
            .border(1.dp, cryoBorderBrush(), shape),
        color = CryoGlassColors.Deep.copy(alpha = 0.55f),
        contentColor = CryoGlassColors.Frost,
        shape = shape
    ) {
        val focusManager = LocalFocusManager.current

        BasicTextField(
            modifier = Modifier
                .wrapContentHeight()
                .padding(contextPadding),
            value = value,
            onValueChange = { new ->
                onValueChange(
                    if (singleLine) new.toSingleLine() else new
                )
            },
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                }
            ),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && hint != null) {
                        if (singleLine) {
                            //Campo de uma linha: a dica não pode aumentar a altura do campo. Quando o campo
                            //ficava estreito (ex.: botões de seleção aparecendo na barra de mods), a dica
                            //quebrava letra por letra e a barra de cima ficava gigante.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clipToBounds(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                hint()
                            }
                        } else {
                            hint()
                        }
                    }
                    innerTextField()
                }
            }
        )
    }
}

private class SimpleTextFieldLabelScope(
    override val labelMinimizedProgress: Float
) : TextFieldLabelScope

@Composable
fun SmallOutlinedEditField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Attached(),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CryoGlassColors.Deep.copy(alpha = 0.55f),
        unfocusedContainerColor = CryoGlassColors.Deep.copy(alpha = 0.40f),
        disabledContainerColor = CryoGlassColors.Deep.copy(alpha = 0.25f),
        focusedBorderColor = CryoGlassColors.Ice,
        unfocusedBorderColor = CryoGlassColors.Ice.copy(alpha = 0.35f),
        focusedTextColor = Color.White,
        unfocusedTextColor = CryoGlassColors.Frost,
        focusedLabelColor = CryoGlassColors.Ice,
        unfocusedLabelColor = CryoGlassColors.Frost.copy(alpha = 0.7f),
        cursorColor = CryoGlassColors.Ice,
        focusedTrailingIconColor = CryoGlassColors.Ice,
        unfocusedTrailingIconColor = CryoGlassColors.Ice.copy(alpha = 0.8f),
        focusedPlaceholderColor = CryoGlassColors.Frost.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = CryoGlassColors.Frost.copy(alpha = 0.5f)
    ),
    contentColor: Color? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    textStyle: TextStyle = TextStyle(fontSize = 12.sp),
    cursorBrush: Brush = SolidColor(LocalTextSelectionColors.current.handleColor),
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    imePanEnabled: Boolean = true,
) {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val focusManager = LocalFocusManager.current

    val effectiveContentColor = contentColor ?: LocalContentColor.current
    val effectiveTextStyle = textStyle.copy(color = effectiveContentColor)

    val borderWidth by animateDpAsState(
        if (isFocused) 2.dp else 1.dp
    )
    val borderColor by animateColorAsState(
        if (isFocused) colors.focusedIndicatorColor else colors.unfocusedIndicatorColor
    )
    val labelScope = remember(labelPosition) {
        SimpleTextFieldLabelScope(
            if (labelPosition is TextFieldLabelPosition.Attached) 1f else 0f
        )
    }

    BasicTextField(
        modifier = if (imePanEnabled) modifier.imePanAnchor() else modifier,
        value = value,
        onValueChange = { new ->
            onValueChange(
                if (singleLine) new.toSingleLine() else new
            )
        },
        textStyle = effectiveTextStyle,
        cursorBrush = cursorBrush,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions
            ?: if (singleLine) KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            else KeyboardOptions.Default,
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onDone?.invoke(this@KeyboardActions)
            },
            onGo = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onGo?.invoke(this@KeyboardActions)
            },
            onNext = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onNext?.invoke(this@KeyboardActions)
            },
            onPrevious = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onPrevious?.invoke(this@KeyboardActions)
            },
            onSearch = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onSearch?.invoke(this@KeyboardActions)
            },
            onSend = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onSend?.invoke(this@KeyboardActions)
            }
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .border(width = borderWidth, color = borderColor, shape = shape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (label != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            label(labelScope)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leadingIcon?.invoke()

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty() && label == null) {
                                placeholder?.invoke()
                            }
                            innerTextField()
                        }

                        trailingIcon?.invoke()
                    }
                }
            }
        },
        interactionSource = interactionSource,
    )
}

@Composable
fun OwnOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CryoGlassColors.Deep.copy(alpha = 0.55f),
        unfocusedContainerColor = CryoGlassColors.Deep.copy(alpha = 0.40f),
        disabledContainerColor = CryoGlassColors.Deep.copy(alpha = 0.25f),
        focusedBorderColor = CryoGlassColors.Ice,
        unfocusedBorderColor = CryoGlassColors.Ice.copy(alpha = 0.35f),
        focusedTextColor = Color.White,
        unfocusedTextColor = CryoGlassColors.Frost,
        focusedLabelColor = CryoGlassColors.Ice,
        unfocusedLabelColor = CryoGlassColors.Frost.copy(alpha = 0.7f),
        cursorColor = CryoGlassColors.Ice,
        focusedTrailingIconColor = CryoGlassColors.Ice,
        unfocusedTrailingIconColor = CryoGlassColors.Ice.copy(alpha = 0.8f),
        focusedPlaceholderColor = CryoGlassColors.Frost.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = CryoGlassColors.Frost.copy(alpha = 0.5f)
    ),
    imePanEnabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val fieldShape = if (shape == OutlinedTextFieldDefaults.shape) RoundedCornerShape(14.dp) else shape

    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
    if (label != null) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 12.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora)))
            )
            androidx.compose.material3.ProvideTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    color = if (isError) MaterialTheme.colorScheme.error else CryoGlassColors.Ice,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides if (isError) MaterialTheme.colorScheme.error else CryoGlassColors.Ice
                ) {
                    label()
                }
            }
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = { string ->
            if (singleLine) {
                onValueChange(string.toSingleLine())
            } else {
                onValueChange(string)
            }
        },
        modifier = if (imePanEnabled) modifier.imePanAnchor() else modifier,
        enabled = enabled,
        readOnly = readOnly,
        //Campos numéricos: mesmo visor da janela de números (monoespaçado e em negrito)
        textStyle = if (keyboardOptions?.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number ||
            keyboardOptions?.keyboardType == androidx.compose.ui.text.input.KeyboardType.Decimal
        ) textStyle.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
        ) else textStyle,
        label = null,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions
            ?: if (singleLine) KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            else KeyboardOptions.Default,
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onDone?.invoke(this@KeyboardActions)
            },
            onGo = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onGo?.invoke(this@KeyboardActions)
            },
            onNext = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onNext?.invoke(this@KeyboardActions)
            },
            onPrevious = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onPrevious?.invoke(this@KeyboardActions)
            },
            onSearch = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onSearch?.invoke(this@KeyboardActions)
            },
            onSend = {
                focusManager.clearFocus(true)
                (keyboardActions ?: KeyboardActions.Default).onSend?.invoke(this@KeyboardActions)
            }
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = fieldShape,
        colors = colors,
    )
    }
}

/**
 * 有范围约束的整数输入框，仅允许输入数字
 * 输入为空或非法时不提交
 */
@Composable
fun IntInputField(
    value: Int,
    permitted: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    fun toText(value: Int): String = value.takeIf { it > 0 }?.toString().orEmpty()

    var text by remember { mutableStateOf(toText(value)) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!focused) text = toText(value)
    }

    OwnOutlinedTextField(
        modifier = modifier.onFocusChanged { state ->
            focused = state.isFocused
            if (!state.isFocused) text = toText(value)
        },
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(6)
            text = filtered
            filtered.toIntOrNull()?.takeIf { it > 0 }?.let { nonZero ->
                onValueChange(nonZero.coerceIn(permitted))
            }
        },
        textStyle = MaterialTheme.typography.labelMedium,
        label = { Text(text = label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}
