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

package com.russoviller.cryogenic.ui.screens.main.control_editor.edit_widget

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.CryoTab
import com.russoviller.cryogenic.ui.components.CryoTabStrip
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.theme.cryoBorder
import com.russoviller.cryogenic.ui.components.CryoRailItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.russoviller.layer_controller.event.ClickEvent
import com.russoviller.layer_controller.observable.ObservableButtonStyle
import com.russoviller.layer_controller.observable.ObservableClickEventsProvider
import com.russoviller.layer_controller.observable.ObservableControlLayer
import com.russoviller.layer_controller.observable.ObservableJoystickData
import com.russoviller.layer_controller.observable.ObservableJoystickStyle
import com.russoviller.layer_controller.observable.ObservableNormalData
import com.russoviller.layer_controller.observable.ObservableTranslatableString
import com.russoviller.layer_controller.observable.ObservableWidget
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.EdgeDirection
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.clearWith
import com.russoviller.cryogenic.ui.screens.content.elements.CategoryItem
import com.russoviller.cryogenic.ui.screens.rememberSwapTween
import com.russoviller.cryogenic.ui.screens.rememberTitledNavBackStack
import com.russoviller.cryogenic.ui.screens.rememberTransitionSpec
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor

private enum class EditWidgetDialogState(val alpha: Float, val buttonText: Int) {
    /** 完全不透明 */
    OPAQUE(1.0f, R.string.control_editor_edit_dialog_open_preview) {
        override fun nextByUser(): EditWidgetDialogState = SEMI_TRANSPARENT_USER
    },
    /** 半透明 */
    SEMI_TRANSPARENT(0.3f, R.string.control_editor_edit_dialog_close_preview) {
        override fun nextByUser(): EditWidgetDialogState = OPAQUE
    },
    /** 半透明（用户主动选择） */
    SEMI_TRANSPARENT_USER(0.3f, R.string.control_editor_edit_dialog_close_preview){
        override fun nextByUser(): EditWidgetDialogState = OPAQUE
    };

    abstract fun nextByUser(): EditWidgetDialogState
}

/**
 * 控件编辑对话框
 * **不再真正使用Dialog，真的会有性能问题！**
 */
@Composable
fun EditWidgetDialog(
    visible: Boolean,
    data: SelectedWidgetData?,
    styles: List<ObservableButtonStyle>,
    joystickStyles: List<ObservableJoystickStyle>,
    onDismissRequest: () -> Unit,
    onDelete: (ObservableWidget, ObservableControlLayer) -> Unit,
    onClone: (ObservableWidget, ObservableControlLayer) -> Unit,
    onEditWidgetText: (ObservableTranslatableString) -> Unit,
    switchControlLayers: (ObservableClickEventsProvider, ClickEvent.Type) -> Unit,
    sendText: (ObservableClickEventsProvider) -> Unit,
    openStyleList: () -> Unit,
    openJoystickStyleList: () -> Unit,
) {
    val tween = rememberSwapTween()

    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = visible,
        enter = fadeIn(animationSpec = tween),
        exit = fadeOut(animationSpec = tween)
    ) {
        val backStack = rememberTitledNavBackStack(EditWidgetCategory.Info)
        var dialogTransparent by remember { mutableStateOf(EditWidgetDialogState.OPAQUE) }

        val alpha by animateFloatAsState(
            dialogTransparent.alpha
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            //防止底下的控件被点击
            if (visible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismissRequest
                        )
                )
            }

            if (data != null) {
                val categories = remember(data) {
                    when (data.data) {
                        is ObservableNormalData -> editWidgetCategories
                        is ObservableJoystickData -> editJoystickCategories
                        else -> editWidgetCategories.filterNot { it.key == EditWidgetCategory.ClickEvent }
                    }
                }

                CompositionLocalProvider(LocalContentColor provides CryoGlassColors.Frost) {
                    val inspectorShape = RoundedCornerShape(26.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .fillMaxHeight()
                            .padding(all = 10.dp)
                            .cryoGlass(shape = inspectorShape, cornerRadius = 26.dp, window = true)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            com.russoviller.cryogenic.ui.components.CryoIconOrb(
                                iconRes = when (data.data) {
                                    is ObservableNormalData -> R.drawable.ic_touch_app_filled
                                    is ObservableJoystickData -> R.drawable.ic_sports_esports_filled
                                    else -> R.drawable.ic_cryo_snowflake
                                },
                                size = 42
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.cryo_inspector_title),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    ),
                                    color = CryoGlassColors.Ice,
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(
                                        when (data.data) {
                                            is ObservableNormalData -> R.string.cryo_inspector_button
                                            is ObservableJoystickData -> R.string.cryo_inspector_joystick
                                            else -> R.string.cryo_inspector_text
                                        }
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(androidx.compose.ui.graphics.Color.White, CryoGlassColors.Ice)
                                        )
                                    ),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                    .clickable(onClick = onDismissRequest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.generic_close),
                                    tint = CryoGlassColors.Ice
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(CryoGlassColors.Ice.copy(alpha = 0.7f), CryoGlassColors.Aurora.copy(alpha = 0.3f), androidx.compose.ui.graphics.Color.Transparent)
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(156.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CryoGlassColors.Deep.copy(alpha = 0.45f))
                                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEachIndexed { index, item ->
                                    CryoInspectorTab(
                                        index = index,
                                        selected = backStack.lastOrNull() == item.key,
                                        icon = { item.icon() },
                                        label = stringResource(item.textRes),
                                        onClick = { backStack.clearWith(item.key) }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(androidx.compose.ui.graphics.Color.Transparent, CryoGlassColors.Ice.copy(alpha = 0.45f), androidx.compose.ui.graphics.Color.Transparent)
                                        )
                                    )
                            )
                            EditWidgetNavigation(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                backStack = backStack,
                                data = data.data,
                                styles = styles,
                                joystickStyles = joystickStyles,
                                switchControlLayers = switchControlLayers,
                                sendText = sendText,
                                openStyleList = openStyleList,
                                openJoystickStyleList = openJoystickStyleList,
                                onEditWidgetText = onEditWidgetText,
                                onPreviewRequested = {
                                    if (dialogTransparent == EditWidgetDialogState.SEMI_TRANSPARENT_USER) return@EditWidgetNavigation
                                    dialogTransparent = EditWidgetDialogState.SEMI_TRANSPARENT
                                },
                                onDismissRequested = {
                                    if (dialogTransparent == EditWidgetDialogState.SEMI_TRANSPARENT_USER) return@EditWidgetNavigation
                                    dialogTransparent = EditWidgetDialogState.OPAQUE
                                }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(all = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (dialogTransparent != EditWidgetDialogState.SEMI_TRANSPARENT) {
                                CryoDockAction(
                                    iconRes = R.drawable.ic_image_outlined,
                                    text = stringResource(dialogTransparent.buttonText),
                                    accent = true,
                                    onClick = { dialogTransparent = dialogTransparent.nextByUser() }
                                )
                                Spacer(Modifier.width(12.dp))
                            } else {
                                Spacer(Modifier)
                            }

                            val scrollState = rememberScrollState()
                            LaunchedEffect(Unit) {
                                scrollState.scrollTo(scrollState.maxValue)
                            }
                            Row(
                                modifier = Modifier
                                    .fadeEdge(
                                        state = scrollState,
                                        direction = EdgeDirection.Horizontal
                                    )
                                    .cryoGlass(shape = RoundedCornerShape(50), glow = false, cornerRadius = 22.dp)
                                    .horizontalScroll(state = scrollState)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CryoDockAction(
                                    iconRes = R.drawable.ic_delete_outlined,
                                    text = stringResource(R.string.generic_delete),
                                    danger = true,
                                    onClick = { onDelete(data.data, data.layer) }
                                )
                                CryoDockAction(
                                    iconRes = R.drawable.ic_content_copy_filled,
                                    text = stringResource(R.string.control_editor_edit_dialog_clone_widget),
                                    onClick = { onClone(data.data, data.layer) }
                                )
                                CryoDockAction(
                                    iconRes = R.drawable.ic_close,
                                    text = stringResource(R.string.generic_close),
                                    accent = true,
                                    onClick = onDismissRequest
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CryoInspectorTab(
    index: Int,
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(if (selected) Modifier.cryoGlow(22.dp, selected = true, strength = 0.5f) else Modifier)
            .clip(shape)
            .cryoBackground(
                if (selected) com.russoviller.cryogenic.ui.components.cryoGradient()
                else androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.04f), androidx.compose.ui.graphics.Color.Transparent)
                )
            )
            .clickable(onClick = onClick)
            .padding(start = 5.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //ícone em círculo
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    if (selected) CryoGlassColors.Deep.copy(alpha = 0.85f)
                    else CryoGlassColors.Ice.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides CryoGlassColors.Ice) {
                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
            }
        }
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(shadow = null, fontSize = 11.sp, lineHeight = 12.sp),
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CryoDockAction(
    iconRes: Int,
    text: String,
    accent: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val danger1 = androidx.compose.ui.graphics.Color(0xFFFF6B8A)
    val contentColor = when {
        accent -> CryoGlassColors.Deep
        danger -> danger1
        else -> CryoGlassColors.Frost
    }
    Row(
        modifier = Modifier
            .height(38.dp)
            .then(if (accent) Modifier.cryoGlow(19.dp, selected = true, strength = 0.7f) else Modifier)
            .clip(RoundedCornerShape(50))
            .cryoBackground(
                when {
                    accent -> com.russoviller.cryogenic.ui.components.cryoGradient()
                    danger -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(danger1.copy(alpha = 0.18f), danger1.copy(alpha = 0.06f)))
                    else -> androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.12f), CryoGlassColors.Navy.copy(alpha = 0.3f)))
                }
            )
            .border(
                1.dp,
                when {
                    accent -> androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f)
                    danger -> danger1.copy(alpha = 0.55f)
                    else -> CryoGlassColors.Ice.copy(alpha = 0.3f)
                },
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (accent) CryoGlassColors.Deep.copy(alpha = 0.85f) else contentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (accent) CryoGlassColors.Ice else contentColor
            )
        }
        Text(
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun EditWidgetTabLayout(
    modifier: Modifier = Modifier,
    items: List<CategoryItem>,
    currentKey: TitledNavKey?,
    navigateTo: (TitledNavKey) -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        items.forEach { item ->
            CryoRailItem(
                selected = currentKey == item.key,
                onClick = {
                    navigateTo(item.key)
                },
                icon = {
                    item.icon()
                },
                label = {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(item.textRes),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EditWidgetNavigation(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<TitledNavKey>,
    data: ObservableWidget,
    styles: List<ObservableButtonStyle>,
    joystickStyles: List<ObservableJoystickStyle>,
    onEditWidgetText: (ObservableTranslatableString) -> Unit,
    switchControlLayers: (ObservableClickEventsProvider, ClickEvent.Type) -> Unit,
    sendText: (ObservableClickEventsProvider) -> Unit,
    openStyleList: () -> Unit,
    openJoystickStyleList: () -> Unit,
    onPreviewRequested: () -> Unit,
    onDismissRequested: () -> Unit
) {
    val currentKey = backStack.lastOrNull()

    if (backStack.isNotEmpty()) {
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = { /* 忽略 */ },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<EditWidgetCategory.Info> { key ->
                    EditWidgetInfo(
                        screenKey = key,
                        currentKey = currentKey,
                        data = data,
                        onPreviewRequested = onPreviewRequested,
                        onDismissRequested = onDismissRequested
                    )
                }
                entry<EditWidgetCategory.TextStyle> { key ->
                    EditTextStyle(
                        screenKey = key,
                        currentKey = currentKey,
                        data = data,
                        onEditWidgetText = onEditWidgetText
                    )
                }
                entry<EditWidgetCategory.ClickEvent> { key ->
                    EditWidgetClickEvent(
                        screenKey = key,
                        currentKey = currentKey,
                        data = data as ObservableNormalData,
                        switchControlLayers = switchControlLayers,
                        sendText = sendText
                    )
                }
                entry<EditWidgetCategory.Style> { key ->
                    EditWidgetStyle(
                        screenKey = key,
                        currentKey = currentKey,
                        data = data,
                        styles = styles,
                        openStyleList = openStyleList
                    )
                }
                entry<EditWidgetCategory.JoystickConfig> { key ->
                    EditJoystickConfig(
                        screenKey = key,
                        currentKey = currentKey,
                        data = data as ObservableJoystickData
                    )
                }
                entry<EditWidgetCategory.DirectionEvents> { key ->
                    EditJoystickEvents(
                        data = data as ObservableJoystickData,
                        switchControlLayers = switchControlLayers,
                        sendText = sendText,
                    )
                }
                entry<EditWidgetCategory.JoystickStyle> { key ->
                    EditJoystickStyle(
                        screenKey = key,
                        currentKey = currentKey,
                        data = data as ObservableJoystickData,
                        joystickStyles = joystickStyles,
                        openJoystickStyleList = openJoystickStyleList,
                    )
                }
            }
        )
    }
}
