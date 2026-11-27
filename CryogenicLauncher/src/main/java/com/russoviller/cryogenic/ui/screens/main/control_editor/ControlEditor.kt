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

import com.russoviller.cryogenic.ui.components.cryoBackground

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.russoviller.cryogenic.ui.theme.cryoBorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.guide.guideNode
import com.russoviller.layer_controller.ControlEditorLayer
import com.russoviller.layer_controller.data.ButtonSize
import com.russoviller.layer_controller.data.CenterPosition
import com.russoviller.layer_controller.data.DefaultDirectionEvents
import com.russoviller.layer_controller.data.DefaultLockEvents
import com.russoviller.layer_controller.data.JoystickData
import com.russoviller.layer_controller.data.NormalData
import com.russoviller.layer_controller.data.TextData
import com.russoviller.layer_controller.data.VisibilityType
import com.russoviller.layer_controller.data.createAdaptiveButtonSize
import com.russoviller.layer_controller.data.createWidgetWithUUID
import com.russoviller.layer_controller.data.lang.createTranslatable
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.isLauncherInDarkTheme
import com.russoviller.cryogenic.ui.components.MenuState
import com.russoviller.cryogenic.ui.components.rememberBoxSize
import com.russoviller.cryogenic.ui.guide.GuideKeys
import com.russoviller.cryogenic.ui.screens.main.control_editor.edit_widget.EditWidgetDialog
import com.russoviller.cryogenic.ui.screens.main.control_editor.style_cabinet.OpenCabinetSource
import com.russoviller.cryogenic.viewmodel.EditorViewModel
import java.io.File

/**
 * 控制布局编辑器主要UI，用于编辑控制布局
 * @param exit 保存后执行的退出
 * @param menuExit 通过菜单直接调用的“直接退出”
 */
@Composable
fun BoxWithConstraintsScope.ControlEditor(
    viewModel: EditorViewModel,
    targetFile: File,
    exit: () -> Unit,
    menuExit: () -> Unit
) {
    val layers by viewModel.observableLayout.layers.collectAsStateWithLifecycle()
    val styles by viewModel.observableLayout.styles.collectAsStateWithLifecycle()
    val joystickStyles by viewModel.observableLayout.joystickStyles.collectAsStateWithLifecycle()

    /** 默认新建的控件层的名称 */
    val defaultLayerName = stringResource(R.string.control_editor_edit_layer_default)
    /** 默认新建的按键的名称 */
    val defaultButtonName = stringResource(R.string.control_editor_edit_button_default)
    /** 默认新建的文本框的名称 */
    val defaultTextName = stringResource(R.string.control_editor_edit_text_default)

    val density = LocalDensity.current
    val screenSize = rememberBoxSize()

    if (viewModel.isPreviewMode) {
        PreviewControlBox(
            modifier = Modifier.fillMaxSize(),
            observableLayout = viewModel.observableLayout,
            previewScenario = viewModel.previewScenario,
            previewHideLayerWhen = viewModel.previewHideLayerWhen,
        )
    } else {
        ControlEditorLayer(
            observedLayout = viewModel.observableLayout,
            selectedWidget = viewModel.selectedWidget?.data,
            onButtonTap = viewModel::onWidgetTapped,
            onBackgroundClick = {
                //点击背景层时清除选中的控件
                viewModel.selectedWidget = null
            },
            floatingButtons = {
                CryoWidgetToolDock(
                    onSettings = viewModel::openWidgetEditor,
                    onClone = {
                        val widget = viewModel.selectedWidget
                        if (widget != null) {
                            viewModel.requestCloneWidget(widget.data, widget.layer)
                        }
                    },
                    onDelete = {
                        val widget = viewModel.selectedWidget
                        if (widget != null) {
                            viewModel.requestDeleteWidget(widget.data, widget.layer)
                        }
                    }
                )
            },
            enableSnap = AllSettings.editorEnableWidgetSnap.state,
            snapInAllLayers = AllSettings.editorSnapInAllLayers.state,
            snapMode = AllSettings.editorWidgetSnapMode.state,
            focusedLayer = viewModel.selectedLayer?.takeIf { viewModel.isLayerFocus },
            isDark = isLauncherInDarkTheme()
        )
    }

    EditorMenu(
        state = viewModel.editorMenu,
        closeScreen = { viewModel.editorMenu = MenuState.HIDE },
        layers = layers,
        onReorder = { from, to ->
            viewModel.observableLayout.reorder(from, to)
        },
        selectedLayer = viewModel.selectedLayer,
        onLayerSelected = { layer ->
            viewModel.selectedLayer = layer
        },
        createLayer = {
            viewModel.createLayer(defaultLayerName)
        },
        onAttribute = viewModel::editLayer,
        onHideSwitch = { layer ->
            layer.editorHide = layer.editorHide.not()
            viewModel.onLayerHideChanged(layer.editorHide, layer)
        },
        addNewButton = {
            viewModel.addWidget(layers) { layer ->
                layer.addNormalButton(
                    createWidgetWithUUID { uuid ->
                        NormalData(
                            text = createTranslatable(default = defaultButtonName),
                            uuid = uuid,
                            position = CenterPosition,
                            buttonSize = createAdaptiveButtonSize(
                                referenceLength = screenSize.height,
                                density = density.density
                            ),
                            visibilityType = VisibilityType.ALWAYS,
                            isSwipple = false,
                            isPenetrable = false,
                            isToggleable = false
                        )
                    }
                )
            }
        },
        addNewText = {
            viewModel.addWidget(layers) { layer ->
                layer.addTextBox(
                    createWidgetWithUUID { uuid ->
                        TextData(
                            text = createTranslatable(default = defaultTextName),
                            uuid = uuid,
                            position = CenterPosition,
                            buttonSize = createAdaptiveButtonSize(
                                referenceLength = screenSize.height,
                                density = density.density,
                                type = ButtonSize.Type.WrapContent //文本框默认使用包裹内容
                            ),
                            visibilityType = VisibilityType.ALWAYS
                        )
                    }
                )
            }
        },
        addNewJoystick = {
            viewModel.addWidget(layers) { layer ->
                layer.addJoystickButton(
                    createWidgetWithUUID { uuid ->
                        JoystickData(
                            uuid = uuid,
                            position = CenterPosition,
                            sizeType = ButtonSize.Type.Percentage,
                            visibilityType = VisibilityType.ALWAYS,
                            directionEvents = DefaultDirectionEvents,
                            lockEvents = DefaultLockEvents,
                        )
                    }
                )
            }
        },
        openStyleList = viewModel::openStyleList,
        openJoystickStyleList = viewModel::openJoystickStyleList,
        openStyleCabinet = {
            viewModel.openStyleCabinet(OpenCabinetSource.Menu)
        },
        isLayerFocus = viewModel.isLayerFocus,
        onLayerFocusChanged = { viewModel.isLayerFocus = it },
        isPreviewMode = viewModel.isPreviewMode,
        onPreviewChanged = { mode ->
            viewModel.applyEditorHide()
            viewModel.isPreviewMode = mode
        },
        previewScenario = viewModel.previewScenario,
        onPreviewScenarioChanged = { scenario ->
            viewModel.previewScenario = scenario
        },
        previewHideLayerWhen = viewModel.previewHideLayerWhen,
        onPreviewHideLayerChanged = { hideWhen ->
            viewModel.previewHideLayerWhen = hideWhen
        },
        onSave = {
            viewModel.save(targetFile, onSaved = {})
        },
        saveAndExit = {
            viewModel.save(targetFile, onSaved = exit)
        },
        exitWithoutSave = exit,
        onExit = menuExit,
    )

    MenuBox(
        modifier = Modifier.guideNode(
            key = GuideKeys.Editor.Step.MenuBall,
            holeRadius = 12.dp
        ),
        position = viewModel.editorBallPosition,
        onPositionChanged = { viewModel.editorBallPosition = it },
        opened = viewModel.editorMenu == MenuState.SHOW
    ) {
        viewModel.switchMenu()
    }

    EditWidgetDialog(
        data = viewModel.selectedWidget,
        visible = viewModel.editorOperation == EditorOperation.SelectButton,
        styles = styles,
        joystickStyles = joystickStyles,
        onDismissRequest = viewModel::dismissOperation,
        onDelete = { data, layer ->
            viewModel.requestDeleteWidget(data, layer)
        },
        onClone = { data, layer ->
            viewModel.requestCloneWidget(data, layer)
        },
        onEditWidgetText = viewModel::editWidgetText,
        switchControlLayers = { data, type ->
            viewModel.switchLayersVisibility(data, type)
        },
        sendText = viewModel::sendText,
        openStyleList = viewModel::openStyleList,
        openJoystickStyleList = viewModel::openJoystickStyleList
    )

    //全部对话框
    EditorDialogHost(viewModel)
}

@Composable
private fun CryoWidgetToolDock(
    onSettings: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = com.russoviller.cryogenic.ui.components.cryoChamferShape(12.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 7.dp)
                .clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                })
                .background(CryoGlassColors.Ice)
        )
        Row(
            modifier = Modifier
                .cryoGlow(14.dp, strength = 0.8f)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(CryoGlassColors.Navy.copy(alpha = 0.96f), CryoGlassColors.Deep.copy(alpha = 0.98f))
                    )
                )
                .border(1.dp, Brush.linearGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Ice.copy(alpha = 0.15f), CryoGlassColors.Aurora)), shape)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockTool(R.drawable.ic_settings_filled, stringResource(R.string.generic_setting), onSettings)
            DockDivider()
            DockTool(R.drawable.ic_file_copy_filled, stringResource(R.string.cryo_editor_clone_short), onClone)
            DockDivider()
            DockTool(R.drawable.ic_delete_filled, stringResource(R.string.generic_delete), onDelete, danger = true)
        }
    }
}

@Composable
private fun DockDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 1.dp, height = 34.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, CryoGlassColors.Ice.copy(alpha = 0.5f), Color.Transparent)))
    )
}

@Composable
private fun DockTool(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val accent = if (danger) Color(0xFFFF6B8A) else CryoGlassColors.Ice
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(com.russoviller.cryogenic.ui.components.cryoChamferShape(8.dp))
                .cryoBackground(
                    if (danger) Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.15f)))
                    else cryoGradient()
                )
                .border(1.dp, accent.copy(alpha = 0.7f), com.russoviller.cryogenic.ui.components.cryoChamferShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(iconRes),
                contentDescription = text,
                tint = if (danger) accent else CryoGlassColors.Deep
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(shadow = null, letterSpacing = 0.5.sp),
            fontWeight = FontWeight.Bold,
            color = if (danger) accent else Color.White,
            maxLines = 1
        )
    }
}
