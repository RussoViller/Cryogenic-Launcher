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

import kotlinx.coroutines.launch
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import coil3.compose.AsyncImage
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.bridge.CursorShape
import com.russoviller.cryogenic.context.copyLocalFile
import com.russoviller.cryogenic.contract.MediaPickerContract
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.GestureActionType
import com.russoviller.cryogenic.setting.enums.MouseControlMode
import com.russoviller.cryogenic.setting.enums.MouseTouchBlockMode
import com.russoviller.cryogenic.setting.unit.ParcelableSettingUnit
import com.russoviller.cryogenic.setting.unit.floatRange
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedColumn
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.LittleTextLabel
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.TitleAndSummary
import com.russoviller.cryogenic.ui.components.TooltipIconButton
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.infiniteShimmer
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.control.gyroscope.isGyroscopeAvailable
import com.russoviller.cryogenic.ui.control.mouse.CursorHotspot
import com.russoviller.cryogenic.ui.control.mouse.MouseHotspotEditorDialog
import com.russoviller.cryogenic.ui.control.mouse.MousePointer
import com.russoviller.cryogenic.ui.control.mouse.arrowPointerFile
import com.russoviller.cryogenic.ui.control.mouse.crossHairPointerFile
import com.russoviller.cryogenic.ui.control.mouse.iBeamPointerFile
import com.russoviller.cryogenic.ui.control.mouse.linkPointerFile
import com.russoviller.cryogenic.ui.control.mouse.notAllowedPointerFile
import com.russoviller.cryogenic.ui.control.mouse.resizeAllPointerFile
import com.russoviller.cryogenic.ui.control.mouse.resizeEWPointerFile
import com.russoviller.cryogenic.ui.control.mouse.resizeNSPointerFile
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.ListSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCardColumn
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.russoviller.cryogenic.utils.formatKeyCode
import com.russoviller.cryogenic.utils.image.isImageFile
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import com.russoviller.cryogenic.ui.components.safeRadialGradient

@Composable
fun ControlSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Control, settingsScreenKey, false)
    ) { isVisible ->
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.physicalMouseMode,
                        title = stringResource(R.string.settings_control_mouse_physical_mouse_mode_title),
                        summary = stringResource(R.string.settings_control_mouse_physical_mouse_mode_summary),
                        trailingIcon = {
                            TooltipIconButton(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = 8.dp),
                                tooltipTitle = stringResource(R.string.generic_warning),
                                tooltipMessage = stringResource(R.string.settings_control_mouse_physical_mouse_warning)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_warning_filled),
                                    contentDescription = stringResource(R.string.generic_warning),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    )

                    var operation by remember { mutableStateOf<PhysicalKeyOperation>(PhysicalKeyOperation.None) }
                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        onClick = { operation = PhysicalKeyOperation.Bind }
                    ) {
                        PhysicalKeyImeTrigger(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 16.dp),
                            operation = operation,
                            changeOperation = { operation = it },
                            eventViewModel = eventViewModel
                        )
                    }
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    val mouseSize = AllSettings.mouseSize.state

                    var showMouseStudio by remember { mutableStateOf(false) }
                    if (showMouseStudio) {
                        MouseStyleStudioDialog(
                            mouseSize = mouseSize,
                            onDismiss = { showMouseStudio = false },
                            submitError = submitError
                        )
                    }
                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        onClick = { showMouseStudio = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .cryoBackground(cryoGradient()),
                                contentAlignment = Alignment.Center
                            ) {
                                MousePointer(
                                    mouseSize = 28.dp,
                                    cursorShape = CursorShape.Arrow,
                                    mouseFile = arrowPointerFile,
                                    centerIcon = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.cryo_mouse_studio_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.cryo_mouse_studio_summary),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                                contentDescription = null,
                                tint = CryoGlassColors.Ice
                            )
                        }
                    }

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.mouseSize,
                        title = stringResource(R.string.settings_control_mouse_size_title),
                        valueRange = AllSettings.mouseSize.floatRange,
                        suffix = "Dp",
                        fineTuningControl = true
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.hideMouse,
                        title = stringResource(R.string.settings_control_mouse_hide_title),
                        summary = stringResource(R.string.settings_control_mouse_hide_summary),
                        enabled = AllSettings.mouseControlMode.state == MouseControlMode.CLICK //仅点击模式下可更改设置
                    )

                    SwitchSettingsCard(
                        unit = AllSettings.enableMouseClick,
                        position = CardPosition.Middle,
                        title = stringResource(R.string.settings_control_mouse_enable_click_title),
                        summary = stringResource(R.string.settings_control_mouse_enable_click_summary),
                        enabled = AllSettings.mouseControlMode.state == MouseControlMode.SLIDE //仅滑动模式下可更改设置
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.mouseControlMode,
                        items = MouseControlMode.entries,
                        title = stringResource(R.string.settings_control_mouse_control_mode_title),
                        summary = stringResource(R.string.settings_control_mouse_control_mode_summary),
                        getItemText = { stringResource(it.nameRes) }
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.mouseTouchBlockMode,
                        items = MouseTouchBlockMode.entries,
                        title = stringResource(R.string.settings_control_mouse_block_touch_title),
                        summary = stringResource(R.string.settings_control_mouse_block_touch_summary),
                        getItemText = { stringResource(it.nameRes) }
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cursorSensitivity,
                        title = stringResource(R.string.settings_control_mouse_sensitivity_title),
                        summary = stringResource(R.string.settings_control_mouse_sensitivity_summary),
                        valueRange = AllSettings.cursorSensitivity.floatRange,
                        suffix = "%",
                        fineTuningControl = true
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.mouseCaptureSensitivity,
                        title = stringResource(R.string.settings_control_mouse_capture_sensitivity_title),
                        summary = stringResource(R.string.settings_control_mouse_capture_sensitivity_summary),
                        valueRange = AllSettings.mouseCaptureSensitivity.floatRange,
                        suffix = "%",
                        fineTuningControl = true
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.mouseLongPressDelay,
                        title = stringResource(R.string.settings_control_mouse_long_press_delay_title),
                        summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                        valueRange = AllSettings.mouseLongPressDelay.floatRange,
                        suffix = "ms",
                        fineTuningControl = true
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.gestureControl,
                        title = stringResource(R.string.settings_control_gesture_control_title),
                        summary = stringResource(R.string.settings_control_gesture_control_summary)
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gestureTapMouseAction,
                        items = GestureActionType.entries,
                        title = stringResource(R.string.settings_control_gesture_tap_action_title),
                        summary = stringResource(R.string.settings_control_gesture_tap_action_summary),
                        getItemText = { stringResource(it.nameRes) },
                        enabled = AllSettings.gestureControl.state
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gestureLongPressMouseAction,
                        items = GestureActionType.entries,
                        title = stringResource(R.string.settings_control_gesture_long_press_action_title),
                        summary = stringResource(R.string.settings_control_gesture_long_press_action_summary),
                        getItemText = { stringResource(it.nameRes) },
                        enabled = AllSettings.gestureControl.state
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.gestureLongPressDelay,
                        title = stringResource(R.string.settings_control_gesture_long_press_delay_title),
                        summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                        valueRange = AllSettings.gestureLongPressDelay.floatRange,
                        suffix = "ms",
                        enabled = AllSettings.gestureControl.state,
                        fineTuningControl = true
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    //检查陀螺仪是否可用
                    val context = LocalContext.current
                    val isGyroscopeAvailable = remember(context) {
                        isGyroscopeAvailable(context = context)
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.gyroscopeControl,
                        title = stringResource(R.string.settings_control_gyroscope_title),
                        summary = stringResource(R.string.settings_control_gyroscope_summary),
                        enabled = isGyroscopeAvailable,
                        trailingIcon = if (!isGyroscopeAvailable) {
                            @Composable {
                                TooltipIconButton(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(horizontal = 8.dp),
                                    tooltipTitle = stringResource(R.string.generic_warning),
                                    tooltipMessage = stringResource(R.string.settings_control_gyroscope_unsupported)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_warning_filled),
                                        contentDescription = stringResource(R.string.generic_warning),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else null
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gyroscopeSensitivity,
                        title = stringResource(R.string.settings_control_gyroscope_sensitivity_title),
                        valueRange = AllSettings.gyroscopeSensitivity.floatRange,
                        suffix = "%",
                        enabled = isGyroscopeAvailable && AllSettings.gyroscopeControl.state,
                        fineTuningControl = true
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gyroscopeSampleRate,
                        title = stringResource(R.string.settings_control_gyroscope_sample_rate_title),
                        summary = stringResource(R.string.settings_control_gyroscope_sample_rate_summary),
                        valueRange = AllSettings.gyroscopeSampleRate.floatRange,
                        suffix = "ms",
                        enabled = isGyroscopeAvailable && AllSettings.gyroscopeControl.state,
                        fineTuningControl = true
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gyroscopeSmoothing,
                        title = stringResource(R.string.settings_control_gyroscope_smoothing_title),
                        summary = stringResource(R.string.settings_control_gyroscope_smoothing_summary),
                        enabled = isGyroscopeAvailable && AllSettings.gyroscopeControl.state
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gyroscopeSmoothingWindow,
                        title = stringResource(R.string.settings_control_gyroscope_smoothing_window_title),
                        summary = stringResource(R.string.settings_control_gyroscope_smoothing_window_summary),
                        valueRange = AllSettings.gyroscopeSmoothingWindow.floatRange,
                        enabled = isGyroscopeAvailable && AllSettings.gyroscopeControl.state && AllSettings.gyroscopeSmoothing.state
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.gyroscopeInvertX,
                        title = stringResource(R.string.settings_control_gyroscope_invert_x_title),
                        summary = stringResource(R.string.settings_control_gyroscope_invert_x_summary),
                        enabled = isGyroscopeAvailable && AllSettings.gyroscopeControl.state
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.gyroscopeInvertY,
                        title = stringResource(R.string.settings_control_gyroscope_invert_y_title),
                        summary = stringResource(R.string.settings_control_gyroscope_invert_y_summary),
                        enabled = isGyroscopeAvailable && AllSettings.gyroscopeControl.state
                    )
                }
            }
        }
    }
}

private sealed interface PhysicalKeyOperation {
    data object None: PhysicalKeyOperation
    data object Bind: PhysicalKeyOperation
}

@Composable
private fun PhysicalKeyImeTrigger(
    modifier: Modifier = Modifier,
    operation: PhysicalKeyOperation,
    changeOperation: (PhysicalKeyOperation) -> Unit,
    eventViewModel: EventViewModel
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
        ) {
            TitleAndSummary(
                title = stringResource(R.string.settings_control_physical_key_bind_ime_title),
                summary = stringResource(R.string.settings_control_physical_key_bind_ime_summary)
            )
            when (operation) {
                PhysicalKeyOperation.None -> {}
                PhysicalKeyOperation.Bind -> {
                    LaunchedEffect(Unit) {
                        eventViewModel.sendEvent(EventViewModel.Event.Key.StartKeyCapture)
                        //接收Activity发送的按键事件
                        eventViewModel.events
                            .filterIsInstance<EventViewModel.Event.Key.OnKeyDown>()
                            .collect { event ->
                                changeOperation(PhysicalKeyOperation.None)
                                AllSettings.physicalKeyImeCode.save(event.key.keyCode)
                            }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            eventViewModel.sendEvent(EventViewModel.Event.Key.StopKeyCapture)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LittleTextLabel(text = stringResource(R.string.control_keyboard_bind_title))
                        MarqueeText(
                            modifier = Modifier
                                .weight(1f)
                                .infiniteShimmer(
                                    initialValue = 0.5f,
                                    targetValue = 1f
                                ),
                            text = stringResource(R.string.control_keyboard_bind_summary),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            val code = AllSettings.physicalKeyImeCode.state
            when {
                code == null -> {
                    Text(
                        modifier = Modifier.padding(end = 12.dp),
                        text = stringResource(R.string.settings_control_physical_key_bind_ime_un_bind),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                else -> {
                    IconTextButton(
                        onClick = { AllSettings.physicalKeyImeCode.save(null) },
                        painter = painterResource(R.drawable.ic_restart_alt),
                        contentDescription = stringResource(R.string.generic_reset),
                        text = stringResource(
                            R.string.settings_control_physical_key_bind_ime_bound,
                            formatKeyCode(code)
                        )
                    )
                }
            }
        }
    }
}

private sealed interface MousePointerOperation {
    data object None: MousePointerOperation
    /** 重置鼠标指针前的提醒 */
    data object PreReset: MousePointerOperation
    /** 重置鼠标指针 */
    data object Reset: MousePointerOperation
    /** 变更鼠标热点 */
    data object Hotspot: MousePointerOperation
}

@Composable
private fun MousePointerCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    title: String,
    summary: String,
    mouseSize: Int,
    mousePointerFile: File,
    cursorShape: CursorShape,
    hotspot: ParcelableSettingUnit<CursorHotspot>,
    mouseOperation: MousePointerOperation,
    changeOperation: (MousePointerOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    var triggerState by remember { mutableIntStateOf(0) }
    var fileExists by remember { mutableStateOf(false) }

    LaunchedEffect(triggerState) {
        fileExists = withContext(Dispatchers.IO) { mousePointerFile.exists() }
    }

    MousePointerOperation(
        operation = mouseOperation,
        changeOperation = changeOperation,
        mousePointerFile = mousePointerFile,
        hotspot = hotspot,
        cursorShape = cursorShape,
        onRefresh = {
            triggerState++
        }
    )

    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = false,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        context.copyLocalFile(result[0], mousePointerFile)
                        if (!mousePointerFile.isImageFile()) error("The selected file is not an image!")
                        triggerState++
                        changeOperation(MousePointerOperation.None)
                    },
                    onError = { th ->
                        FileUtils.deleteQuietly(mousePointerFile)
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(R.string.error_import_image),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    SettingsCard(
        modifier = modifier,
        position = position
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { filePicker.launch(Unit) }
                    .padding(all = 16.dp)
            ) {
                TitleAndSummary(
                    title = title,
                    summary = summary
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MousePointer(
                    modifier = Modifier.padding(all = 8.dp),
                    mouseSize = mouseSize.dp,
                    cursorShape = cursorShape,
                    mouseFile = mousePointerFile,
                    centerIcon = true,
                    triggerRefresh = triggerState,
                    crossfade = true
                )

                IconTextButton(
                    onClick = {
                        if (mouseOperation == MousePointerOperation.None) {
                            changeOperation(MousePointerOperation.Hotspot)
                        }
                    },
                    painter = painterResource(R.drawable.ic_highlight_mouse_cursor),
                    contentDescription = stringResource(R.string.settings_control_mouse_pointer_hotspot),
                    text = stringResource(R.string.settings_control_mouse_pointer_hotspot)
                )

                AnimatedVisibility(
                    visible = fileExists
                ) {
                    IconTextButton(
                        onClick = {
                            if (mouseOperation == MousePointerOperation.None) {
                                changeOperation(MousePointerOperation.PreReset)
                            }
                        },
                        painter = painterResource(R.drawable.ic_restart_alt),
                        contentDescription = stringResource(R.string.generic_reset),
                        text = stringResource(R.string.generic_reset)
                    )
                }
            }
        }
    }
}

@Composable
private fun MousePointerOperation(
    operation: MousePointerOperation,
    changeOperation: (MousePointerOperation) -> Unit,
    mousePointerFile: File,
    hotspot: ParcelableSettingUnit<CursorHotspot>,
    cursorShape: CursorShape,
    onRefresh: () -> Unit
) {
    when (operation) {
        is MousePointerOperation.None -> {}
        is MousePointerOperation.PreReset -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.settings_control_mouse_pointer_reset_message),
                onConfirm = {
                    //正式开始重置鼠标指针
                    changeOperation(MousePointerOperation.Reset)
                },
                onDismiss = {
                    changeOperation(MousePointerOperation.None)
                }
            )
        }
        is MousePointerOperation.Reset -> {
            LaunchedEffect(Unit) {
                FileUtils.deleteQuietly(mousePointerFile)
                onRefresh()
                changeOperation(MousePointerOperation.None)
            }
        }
        is MousePointerOperation.Hotspot -> {
            MouseHotspotEditorDialog(
                hotspot = hotspot,
                cursorShape = cursorShape,
                onClose = {
                    changeOperation(MousePointerOperation.None)
                }
            )
        }
    }
}

private class PointerSpec(
    val shape: CursorShape,
    val file: File,
    val hotspot: ParcelableSettingUnit<CursorHotspot>,
    val nameRes: Int,
    val whenRes: Int,
    val exampleRaw: Int,
    val allVersions: Boolean
)

@Composable
private fun rememberPointerSpecs(): List<PointerSpec> = remember {
    listOf(
        PointerSpec(CursorShape.Arrow, arrowPointerFile, AllSettings.arrowMouseHotspot, R.string.cryo_mouse_arrow, R.string.cryo_mouse_arrow_when, R.raw.cursor_example_arrow, true),
        PointerSpec(CursorShape.Hand, linkPointerFile, AllSettings.linkMouseHotspot, R.string.cryo_mouse_link, R.string.cryo_mouse_link_when, R.raw.cursor_example_link, false),
        PointerSpec(CursorShape.IBeam, iBeamPointerFile, AllSettings.iBeamMouseHotspot, R.string.cryo_mouse_ibeam, R.string.cryo_mouse_ibeam_when, R.raw.cursor_example_ibeam, false),
        PointerSpec(CursorShape.CrossHair, crossHairPointerFile, AllSettings.crossHairMouseHotspot, R.string.cryo_mouse_crosshair, R.string.cryo_mouse_crosshair_when, R.raw.cursor_example_crosshair, false),
        PointerSpec(CursorShape.ResizeNS, resizeNSPointerFile, AllSettings.resizeNSMouseHotspot, R.string.cryo_mouse_resize_ns, R.string.cryo_mouse_resize_ns_when, R.raw.cursor_example_resize_ns, false),
        PointerSpec(CursorShape.ResizeEW, resizeEWPointerFile, AllSettings.resizeEWMouseHotspot, R.string.cryo_mouse_resize_ew, R.string.cryo_mouse_resize_ew_when, R.raw.cursor_example_resize_ew, false),
        PointerSpec(CursorShape.ResizeAll, resizeAllPointerFile, AllSettings.resizeAllMouseHotspot, R.string.cryo_mouse_resize_all, R.string.cryo_mouse_resize_all_when, R.raw.cursor_example_resize_all, false),
        PointerSpec(CursorShape.NotAllowed, notAllowedPointerFile, AllSettings.notAllowedMouseHotspot, R.string.cryo_mouse_not_allowed, R.string.cryo_mouse_not_allowed_when, R.raw.cursor_example_not_allowed, false)
    )
}

@Composable
private fun MouseStyleStudioDialog(
    mouseSize: Int,
    onDismiss: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    val specs = rememberPointerSpecs()
    var selectedIndex by remember { mutableIntStateOf(0) }
    val spec = specs[selectedIndex]
    var refresh by remember { mutableIntStateOf(0) }
    var operation by remember { mutableStateOf<MousePointerOperation>(MousePointerOperation.None) }
    var showSetas by remember { mutableStateOf(false) }
    val setasTargets = specs.map { SetasTarget(it.shape, stringResource(it.nameRes), it.file) }
    var setasEntries by remember { mutableStateOf<List<SetasEntry>?>(null) }
    var setasInitial by remember { mutableStateOf<Map<com.russoviller.cryogenic.bridge.CursorShape, SetasEntry>>(emptyMap()) }
    val setasDoneText = stringResource(R.string.cryo_setas_done)
    val setasEmptyText = stringResource(R.string.cryo_setas_empty)
    val setasScope = androidx.compose.runtime.rememberCoroutineScope()
    fun applySetasNow(map: Map<com.russoviller.cryogenic.bridge.CursorShape, SetasEntry>) {
        setasScope.launch {
            val count = withContext(Dispatchers.IO) { runCatching { applySetas(map, setasTargets) }.getOrDefault(0) }
            refresh++
            android.widget.Toast.makeText(context, setasDoneText.format(count), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val setasPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            setasScope.launch {
                val entries = withContext(Dispatchers.IO) { runCatching { readSetasZip(context, uri) }.getOrDefault(emptyList()) }
                if (entries.isEmpty()) {
                    android.widget.Toast.makeText(context, setasEmptyText, android.widget.Toast.LENGTH_LONG).show()
                    return@launch
                }
                val classified = classifySetas(entries)
                if (classified.size == setasTargets.size) {
                    applySetasNow(classified)
                } else {
                    setasInitial = classified
                    setasEntries = entries
                }
            }
        }
    }
    setasEntries?.let { entries ->
        SetasAssignDialog(
            entries = entries,
            initial = setasInitial,
            targets = setasTargets,
            onConfirm = { map ->
                setasEntries = null
                applySetasNow(map)
            },
            onDismiss = { setasEntries = null }
        )
    }
    if (showSetas) {
        com.russoviller.cryogenic.ui.screens.content.web.CryoWebDialog(
            anchor = "setas",
            title = stringResource(R.string.cryo_mouse_get_setas),
            onDismiss = { showSetas = false }
        )
    }
    val fileExists by produceState(false, spec, refresh) {
        value = withContext(Dispatchers.IO) { spec.file.exists() }
    }

    MousePointerOperation(
        operation = operation,
        changeOperation = { operation = it },
        mousePointerFile = spec.file,
        hotspot = spec.hotspot,
        cursorShape = spec.shape,
        onRefresh = { refresh++ }
    )

    val currentSpec by rememberUpdatedState(spec)
    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = false,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            val target = currentSpec.file
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        context.copyLocalFile(result[0], target)
                        if (!target.isImageFile()) error("The selected file is not an image!")
                        refresh++
                    },
                    onError = { th ->
                        FileUtils.deleteQuietly(target)
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(R.string.error_import_image),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(com.russoviller.cryogenic.ui.components.LocalCryoGlassFixed provides true) {
        Row(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight()
                .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = stringResource(R.string.cryo_mouse_studio_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Text(
                    text = stringResource(R.string.cryo_mouse_studio_pick_type),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    specs.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft.copy(alpha = 0.55f), CryoGlassColors.Aurora.copy(alpha = 0.15f)))
                                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
                                )
                                .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.2f), RoundedCornerShape(14.dp))
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CryoGlassColors.Deep.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                MousePointer(
                                    mouseSize = 24.dp,
                                    cursorShape = item.shape,
                                    mouseFile = item.file,
                                    centerIcon = true,
                                    triggerRefresh = refresh
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(item.nameRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (item.allVersions) stringResource(R.string.cryo_mouse_all_versions) else "MC 1.21.9+",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.allVersions) CryoGlassColors.Aurora else CryoGlassColors.Ice,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                CryoButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSetas = true }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_download_2_filled),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.cryo_mouse_get_setas), maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                CryoTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.generic_close))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MouseStudioPreview(
                        label = stringResource(R.string.cryo_mouse_current)
                    ) {
                        MousePointer(
                            mouseSize = mouseSize.dp.coerceAtLeast(40.dp),
                            cursorShape = spec.shape,
                            mouseFile = spec.file,
                            centerIcon = true,
                            triggerRefresh = refresh,
                            crossfade = true
                        )
                    }
                    MouseStudioPreview(
                        label = stringResource(R.string.cryo_mouse_example)
                    ) {
                        AsyncImage(
                            modifier = Modifier.size(64.dp),
                            model = spec.exampleRaw,
                            contentDescription = null,
                            filterQuality = FilterQuality.None
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(spec.nameRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(spec.whenRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (fileExists) stringResource(R.string.cryo_mouse_status_custom) else stringResource(R.string.cryo_mouse_status_default),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (fileExists) CryoGlassColors.Aurora else CryoGlassColors.Ice
                        )
                    }
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
                        .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    text = stringResource(R.string.cryo_mouse_example_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoButton(
                        modifier = Modifier.weight(1f),
                        onClick = { filePicker.launch(Unit) }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_upload),
                            contentDescription = null
                        )
                        MarqueeText(text = stringResource(R.string.cryo_mouse_choose))
                    }
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { if (operation == MousePointerOperation.None) operation = MousePointerOperation.Hotspot }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_highlight_mouse_cursor),
                            contentDescription = null
                        )
                        MarqueeText(text = stringResource(R.string.cryo_mouse_hotspot))
                    }
                    if (fileExists) {
                        CryoTonalButton(
                            modifier = Modifier.weight(0.8f),
                            onClick = { if (operation == MousePointerOperation.None) operation = MousePointerOperation.PreReset }
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_restart_alt),
                                contentDescription = null
                            )
                            MarqueeText(text = stringResource(R.string.generic_reset))
                        }
                    }
                }

                CryoButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { setasPicker.launch("application/zip") }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_folder_zip_filled),
                        contentDescription = null
                    )
                    MarqueeText(text = stringResource(R.string.cryo_setas_import))
                }

                MouseStudioSection(stringResource(R.string.cryo_mouse_steps_title)) {
                    listOf(
                        R.string.cryo_mouse_step1,
                        R.string.cryo_mouse_step2,
                        R.string.cryo_mouse_step3,
                        R.string.cryo_mouse_step4,
                        R.string.cryo_mouse_step5
                    ).forEachIndexed { index, res ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .cryoBackground(cryoGradient()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(shadow = null),
                                    fontWeight = FontWeight.Black,
                                    color = CryoGlassColors.Deep
                                )
                            }
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(res),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }

                MouseStudioSection(stringResource(R.string.cryo_mouse_tips_title)) {
                    listOf(
                        R.string.cryo_mouse_tip_format,
                        R.string.cryo_mouse_tip_hotspot,
                        R.string.cryo_mouse_tip_version,
                        R.string.cryo_mouse_tip_size
                    ).forEach { res ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "❄", color = CryoGlassColors.Ice, style = MaterialTheme.typography.bodySmall)
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(res),
                                style = MaterialTheme.typography.bodySmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                MouseStudioSection(stringResource(R.string.cryo_mouse_map_title)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        specs.forEachIndexed { index, item ->
                            Column(
                                modifier = Modifier
                                    .width(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = if (index == selectedIndex) 0.12f else 0.04f))
                                    .clickable { selectedIndex = index }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    modifier = Modifier.size(48.dp),
                                    model = item.exampleRaw,
                                    contentDescription = null,
                                    filterQuality = FilterQuality.None
                                )
                                Text(
                                    text = stringResource(item.nameRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
            }
    }
}

@Composable
private fun MouseStudioPreview(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    safeRadialGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.25f), CryoGlassColors.Deep))
                )
                .border(1.dp, cryoBorderBrush(), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Ice
        )
    }
}

@Composable
private fun MouseStudioSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
            color = CryoGlassColors.Ice
        )
        content()
    }
}
