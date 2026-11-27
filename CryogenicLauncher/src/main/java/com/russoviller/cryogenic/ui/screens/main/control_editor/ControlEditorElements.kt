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

package com.russoviller.cryogenic.ui.screens.main.control_editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.russoviller.cryogenic.ui.components.XModeButton
import com.russoviller.cryogenic.ui.components.XModeMenu
import com.russoviller.cryogenic.ui.components.cryoChamferTile
import com.russoviller.cryogenic.ui.components.cryoGlow
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.russoviller.guide.guideLazyList
import com.russoviller.guide.guideNode
import com.russoviller.layer_controller.data.HideLayerWhen
import com.russoviller.layer_controller.data.JoystickTriggerMode
import com.russoviller.layer_controller.data.VisibilityType
import com.russoviller.layer_controller.observable.ObservableControlLayer
import com.russoviller.layer_controller.utils.snap.SnapMode
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.bridge.CURSOR_DISABLED
import com.russoviller.cryogenic.bridge.CURSOR_ENABLED
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.DualMenuSubscreen
import com.russoviller.cryogenic.ui.components.FloatingBall
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.MenuListLayout
import com.russoviller.cryogenic.ui.components.MenuState
import com.russoviller.cryogenic.ui.components.MenuSwitchButton
import com.russoviller.cryogenic.ui.components.MenuTextButton
import com.russoviller.cryogenic.ui.components.ScalingActionButton
import com.russoviller.cryogenic.ui.components.lazyScrollWithBar
import com.russoviller.cryogenic.ui.guide.GuideKeys
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.string.isNotEmptyOrBlank
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import com.russoviller.cryogenic.ui.components.cryoTile
import com.russoviller.cryogenic.ui.components.cryoBackground

/**
 * 预览控制布局的场景
 */
enum class PreviewScenario(
    val textRes: Int,
    val cursorMode: Int,
    val isCursorGrabbing: Boolean = cursorMode == CURSOR_DISABLED
) {
    InGame(R.string.control_editor_menu_preview_mode_in_game, cursorMode = CURSOR_DISABLED),
    InMenu(R.string.control_editor_menu_preview_mode_in_menu, cursorMode = CURSOR_ENABLED)
}

@Composable
fun VisibilityType.getVisibilityText(): String {
    val textRes = when (this) {
        VisibilityType.ALWAYS -> R.string.control_editor_edit_visibility_always
        VisibilityType.IN_GAME -> R.string.control_editor_edit_visibility_in_game
        VisibilityType.IN_MENU -> R.string.control_editor_edit_visibility_in_menu
    }
    return stringResource(textRes)
}

@Composable
fun JoystickTriggerMode.getTriggerModeText(): String {
    val textRes = when (this) {
        JoystickTriggerMode.DRAG -> R.string.control_editor_edit_joystick_trigger_mode_drag
        JoystickTriggerMode.TOUCH -> R.string.control_editor_edit_joystick_trigger_mode_touch
    }
    return stringResource(textRes)
}

/**
 * 文本为空或全空白时，回退显示为“未指定”
 */
@Composable
fun String.orUnspecified(): String {
    return takeIf { it.isNotEmptyOrBlank() } ?: stringResource(R.string.generic_unspecified)
}

@Composable
fun MenuBox(
    modifier: Modifier = Modifier,
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    opened: Boolean,
    onClick: () -> Unit
) {
    FloatingBall(
        modifier = modifier,
        position = position,
        onPositionChanged = onPositionChanged,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .padding(all = 2.dp)
                .size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(opened) { state ->
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(
                        if (state) {
                            R.drawable.ic_cryo_menu_open
                        } else {
                            R.drawable.ic_cryo_menu
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun EditorMenu(
    state: MenuState,
    closeScreen: () -> Unit,
    layers: List<ObservableControlLayer>,
    onReorder: (from: Int, to: Int) -> Unit,
    selectedLayer: ObservableControlLayer?,
    onLayerSelected: (ObservableControlLayer?) -> Unit,
    createLayer: () -> Unit,
    onAttribute: (ObservableControlLayer) -> Unit,
    onHideSwitch: (ObservableControlLayer) -> Unit,
    addNewButton: () -> Unit,
    addNewText: () -> Unit,
    addNewJoystick: () -> Unit,
    openStyleList: () -> Unit,
    openJoystickStyleList: () -> Unit,
    openStyleCabinet: () -> Unit,
    isLayerFocus: Boolean,
    onLayerFocusChanged: (Boolean) -> Unit,
    isPreviewMode: Boolean,
    onPreviewChanged: (Boolean) -> Unit,
    previewScenario: PreviewScenario,
    onPreviewScenarioChanged: (PreviewScenario) -> Unit,
    previewHideLayerWhen: HideLayerWhen,
    onPreviewHideLayerChanged: (HideLayerWhen) -> Unit,
    onSave: () -> Unit,
    saveAndExit: () -> Unit,
    exitWithoutSave: () -> Unit,
    onExit: () -> Unit
) {
    XModeMenu(
        visible = state == MenuState.SHOW,
        closeScreen = closeScreen,
        topLabel = stringResource(R.string.cryo_xmode_exit_editor),
        onTopAction = onExit,
        modeLabel = "CRYO-EDITOR",
        leftButtons = listOf(
            XModeButton(R.drawable.ic_add, stringResource(R.string.cryo_xmode_add_button), enabled = isPreviewMode.not(), onClick = addNewButton),
            XModeButton(R.drawable.ic_sports_esports_filled, stringResource(R.string.cryo_xmode_add_joystick), enabled = isPreviewMode.not(), onClick = addNewJoystick),
            XModeButton(R.drawable.ic_settings_filled, stringResource(R.string.cryo_xmode_tools), panel = {
                Column(modifier = Modifier.fillMaxSize()) {
                    EditorMenuContent(
                        modifier = Modifier.weight(1f),
                        closeScreen = closeScreen,
                        addNewButton = addNewButton,
                        addNewText = addNewText,
                        addNewJoystick = addNewJoystick,
                        openStyleList = openStyleList,
                        openJoystickStyleList = openJoystickStyleList,
                        openStyleCabinet = openStyleCabinet,
                        isPreviewMode = isPreviewMode,
                        onPreviewChanged = onPreviewChanged,
                        previewScenario = previewScenario,
                        onPreviewScenarioChanged = onPreviewScenarioChanged,
                        previewHideLayerWhen = previewHideLayerWhen,
                        onPreviewHideLayerChanged = onPreviewHideLayerChanged,
                        onSave = onSave,
                        saveAndExit = saveAndExit,
                        onExit = onExit
                    )
                }
            })
        ),
        rightButtons = listOf(
            XModeButton(R.drawable.ic_dashboard_outlined, stringResource(R.string.control_editor_layers_title), panel = {
                Column(modifier = Modifier.fillMaxSize()) {
                    //Focar na camada selecionada
                    com.russoviller.cryogenic.ui.components.CryoToggleTile(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        iconRes = if (isLayerFocus) R.drawable.ic_center_focus_strong_filled else R.drawable.ic_center_focus_strong_outlined,
                        title = stringResource(R.string.cryo_xmode_layer_focus),
                        subtitle = stringResource(R.string.cryo_layers_focus_sub),
                        checked = isLayerFocus,
                        onCheckedChange = { onLayerFocusChanged(it) },
                        enabled = isPreviewMode.not() && selectedLayer != null
                    )
                    ControlLayerMenu(
                        layers = layers,
                        onReorder = onReorder,
                        selectedLayer = selectedLayer,
                        onLayerSelected = onLayerSelected,
                        createLayer = createLayer,
                        onAttribute = onAttribute,
                        onHideSwitch = onHideSwitch,
                        enabled = isPreviewMode.not()
                    )
                }
            }),
            XModeButton(R.drawable.ic_image_outlined, stringResource(R.string.cryo_xmode_preview), active = isPreviewMode, onClick = { onPreviewChanged(isPreviewMode.not()) }),
            XModeButton(R.drawable.ic_check, stringResource(R.string.generic_save), onClick = onSave),
            XModeButton(R.drawable.ic_check, stringResource(R.string.cryo_xmode_save_exit), onClick = saveAndExit),
            XModeButton(R.drawable.ic_close, stringResource(R.string.cryo_xmode_exit_no_save), danger = true, onClick = exitWithoutSave)
        )
    )
}

@Composable
private fun EditorMenuContent(
    closeScreen: () -> Unit,
    addNewButton: () -> Unit,
    addNewText: () -> Unit,
    addNewJoystick: () -> Unit,
    openStyleList: () -> Unit,
    openJoystickStyleList: () -> Unit,
    openStyleCabinet: () -> Unit,
    isPreviewMode: Boolean,
    onPreviewChanged: (Boolean) -> Unit,
    previewScenario: PreviewScenario,
    onPreviewScenarioChanged: (PreviewScenario) -> Unit,
    previewHideLayerWhen: HideLayerWhen,
    onPreviewHideLayerChanged: (HideLayerWhen) -> Unit,
    onSave: () -> Unit,
    saveAndExit: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor()
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .guideLazyList(listState) { key ->
                when (key) {
                    GuideKeys.Editor.Step.CreateLayer -> "add_button"
                    GuideKeys.Editor.Step.AddStyles -> "joystick_style_list"
                    GuideKeys.Editor.Step.StyleCabinet -> "style_cabinet"
                    GuideKeys.Editor.Step.Preview -> "using_physical_mouse"
                    GuideKeys.Editor.Step.Save -> "save_and_exit"
                    else -> null
                }
            }
            .lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //添加按钮
        item(key = "add_button") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.AddButtons,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                enabled = isPreviewMode.not(),
                text = stringResource(R.string.control_editor_menu_new_widget_button),
                onClick = addNewButton,
                color = color,
                contentColor = contentColor,
            )
        }

        //Textos no HUD foram removidos: agora ficam em Modo streamer → Textos na tela

        //添加摇杆
        item(key = "add_joystick") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.AddButtons,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                enabled = isPreviewMode.not(),
                text = stringResource(R.string.control_editor_menu_new_widget_joystick),
                onClick = addNewJoystick,
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //控件外观列表
        item(key = "control_appearance_list") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.AddStyles,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.control_editor_edit_style_config),
                enabled = isPreviewMode.not(),
                onClick = {
                    openStyleList()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }

        //摇杆样式列表
        item(key = "joystick_style_list") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.AddStyles,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.control_editor_edit_joystick_style_list),
                enabled = isPreviewMode.not(),
                onClick = {
                    openJoystickStyleList()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }

        // 样式收藏柜
        item(key = "style_cabinet") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.StyleCabinet,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.control_editor_style_cabinet),
                enabled = isPreviewMode.not(),
                onClick = {
                    openStyleCabinet()
                    closeScreen()
                },
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //预览控制布局
        item(key = "preview_control_layout") {
            MenuSwitchButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.Preview,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.control_editor_menu_preview_mode),
                switch = isPreviewMode,
                onSwitch = { onPreviewChanged(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        //预览场景
        item(key = "preview_scenario") {
            MenuListLayout(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.Preview,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                title = stringResource(R.string.control_editor_menu_preview_mode_scenario),
                items = PreviewScenario.entries,
                currentItem = previewScenario,
                onItemChange = onPreviewScenarioChanged,
                getItemText = { scenario ->
                    stringResource(scenario.textRes)
                },
                color = color,
                contentColor = contentColor,
                enabled = isPreviewMode
            )
        }

        //正在使用实体鼠标
        item(key = "using_physical_mouse") {
            MenuSwitchButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.Preview,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.control_editor_menu_preview_is_mouse),
                switch = previewHideLayerWhen == HideLayerWhen.WhenMouse,
                onSwitch = { value ->
                    onPreviewHideLayerChanged(
                        if (value) HideLayerWhen.WhenMouse
                        else HideLayerWhen.None
                    )
                },
                color = color,
                contentColor = contentColor,
                enabled = isPreviewMode
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //控件吸附
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.control_editor_menu_widget_snap),
                switch = AllSettings.editorEnableWidgetSnap.state,
                onSwitch = { AllSettings.editorEnableWidgetSnap.save(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        //所有控制层范围吸附
        item {
            MenuSwitchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.control_editor_menu_widget_snap_all_layers),
                switch = AllSettings.editorSnapInAllLayers.state,
                onSwitch = { AllSettings.editorSnapInAllLayers.save(it) },
                color = color,
                contentColor = contentColor,
            )
        }

        //控件吸附模式
        item {
            MenuListLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_menu_widget_snap_mode),
                items = SnapMode.entries,
                currentItem = AllSettings.editorWidgetSnapMode.state,
                onItemChange = { AllSettings.editorWidgetSnapMode.save(it) },
                getItemText = { mode ->
                    val textRes = when (mode) {
                        SnapMode.FullScreen -> R.string.control_editor_menu_widget_snap_mode_fullscreen
                        SnapMode.Local -> R.string.control_editor_menu_widget_snap_mode_local
                    }
                    stringResource(textRes)
                },
                color = color,
                contentColor = contentColor,
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        //保存
        item(key = "save") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.Save,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.generic_save),
                onClick = onSave,
                color = color,
                contentColor = contentColor,
            )
        }

        //保存并退出
        item(key = "save_and_exit") {
            MenuTextButton(
                modifier = Modifier
                    .guideNode(
                        key = GuideKeys.Editor.Step.Save,
                        holeRadius = 16.dp
                    )
                    .fillMaxWidth(),
                text = stringResource(R.string.control_editor_menu_save_and_exit),
                onClick = saveAndExit,
                color = color,
                contentColor = contentColor,
            )
        }

        //直接退出
        item(key = "exit_directly") {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.control_editor_exit_confirm),
                onClick = onExit,
                color = color,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun ColumnScope.ControlLayerMenu(
    layers: List<ObservableControlLayer>,
    onReorder: (from: Int, to: Int) -> Unit,
    selectedLayer: ObservableControlLayer?,
    onLayerSelected: (ObservableControlLayer?) -> Unit,
    createLayer: () -> Unit,
    onAttribute: (ObservableControlLayer) -> Unit,
    onHideSwitch: (ObservableControlLayer) -> Unit,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    enabled: Boolean = true
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            onReorder(from.index, to.index)
        }
    )

    LaunchedEffect(Unit) {
        runCatching {
            val index = layers.indexOfFirst { it == selectedLayer }
            if (index >= 0 && index < layers.size) {
                lazyListState.animateScrollToItem(index)
            }
        }
    }

    //检查列表新增情况，自动滚动到顶部
    var previousSize by remember { mutableIntStateOf(0) }
    val currentSize = layers.size
    LaunchedEffect(currentSize) {
        if (currentSize != previousSize) {
            if (currentSize - previousSize > 0) {
                runCatching {
                    lazyListState.animateScrollToItem(0)
                }
            }
            previousSize = currentSize
        }
    }

    LazyColumn(
        modifier = Modifier
            .guideNode(GuideKeys.Editor.Step.LayerList)
            .weight(1f)
            .lazyScrollWithBar(lazyListState),
        state = lazyListState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(layers, { it.uuid }) { layer ->
            val layerNumber = layers.indexOf(layer) + 1
            ReorderableItem(
                state = reorderableLazyListState,
                key = layer.uuid,
                enabled = enabled,
            ) { isDragging ->
                val shadowElevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                ControlLayerItem(
                    modifier = Modifier.fillMaxWidth(),
                    number = layerNumber,
                    isDragging = isDragging,
                    layer = layer,
                    dragButtonModifier = Modifier.draggableHandle(),
                    selected = selectedLayer == layer,
                    shadowElevation = shadowElevation,
                    onSelected = {
                        onLayerSelected(layer)
                    },
                    onUnSelected = {
                        onLayerSelected(null)
                    },
                    onAttribute = {
                        onAttribute(layer)
                    },
                    onHideSwitch = {
                        onHideSwitch(layer)
                    },
                    color = color,
                    contentColor = contentColor,
                    enabled = enabled
                )
            }
        }
    }
    com.russoviller.cryogenic.ui.components.CryoButton(
        modifier = Modifier
            .guideNode(
                key = GuideKeys.Editor.Step.CreateLayer,
                holeRadius = 50.dp
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth(),
        enabled = enabled,
        onClick = createLayer
    ) {
        Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_add), contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        MarqueeText(text = stringResource(R.string.control_editor_layers_create))
    }
}

@Composable
private fun ControlLayerItem(
    modifier: Modifier = Modifier,
    number: Int,
    isDragging: Boolean,
    layer: ObservableControlLayer,
    dragButtonModifier: Modifier,
    selected: Boolean,
    onSelected: () -> Unit,
    onUnSelected: () -> Unit,
    onAttribute: () -> Unit,
    onHideSwitch: () -> Unit,
    color: Color,
    contentColor: Color,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = MaterialTheme.shapes.large,
    shadowElevation: Dp = 0.dp,
    enabled: Boolean = true
) {
    val ice = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    val buttons by layer.normalButtons.collectAsState()
    val joysticks by layer.joystickButtons.collectAsState()
    val hidden = layer.editorHide
    Row(
        modifier = modifier
            .then(if (selected || isDragging) Modifier.cryoGlow(18.dp, selected = true, strength = 0.7f) else Modifier)
            .cryoTile(active = selected)
            .clickable(enabled = enabled) { if (selected) onUnSelected() else onSelected() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .alpha(if (hidden) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        //número da camada
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.cryoBackground(com.russoviller.cryogenic.ui.components.cryoGradient())
                    else Modifier
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
                        .border(1.dp, ice.copy(alpha = 0.4f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = if (selected) com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep else ice
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MarqueeText(
                text = layer.name.orUnspecified(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
                color = androidx.compose.ui.graphics.Color.White
            )
            Text(
                text = stringResource(R.string.cryo_layers_widgets, buttons.size, joysticks.size),
                style = MaterialTheme.typography.labelSmall,
                color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost.copy(alpha = 0.75f),
                maxLines = 1
            )
        }
        //mostrar/esconder no editor
        LayerIconButton(
            iconRes = if (hidden) R.drawable.ic_visibility_off_outlined else R.drawable.ic_visibility_outlined,
            active = !hidden,
            enabled = enabled,
            onClick = onHideSwitch
        )
        LayerIconButton(
            iconRes = R.drawable.ic_more_horiz,
            active = false,
            enabled = enabled,
            onClick = onAttribute
        )
        Box(
            modifier = dragButtonModifier
                .size(width = 28.dp, height = 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = ice.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LayerIconButton(iconRes: Int, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val ice = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) ice.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f))
            .border(1.dp, ice.copy(alpha = if (active) 0.7f else 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) ice else com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost.copy(alpha = 0.7f)
        )
    }
}
