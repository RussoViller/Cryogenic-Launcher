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

import com.russoviller.cryogenic.ui.components.CryoIconButton
import android.os.Build
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.control.ControlManager
import com.russoviller.cryogenic.game.multirt.RuntimesManager
import com.russoviller.cryogenic.game.plugin.driver.DriverPluginManager
import com.russoviller.cryogenic.game.renderer.ConfigurableRenderer
import com.russoviller.cryogenic.game.renderer.Renderers
import com.russoviller.cryogenic.game.support.touch_controller.VibrationHandler
import com.russoviller.cryogenic.game.version.installed.GraphicsApi
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionConfig
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.unit.floatRange
import com.russoviller.cryogenic.setting.unit.getOrMin
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedColumn
import com.russoviller.cryogenic.ui.components.IDItem
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.MemoryPreview
import com.russoviller.cryogenic.ui.screens.content.elements.MicrophoneCheckOperation
import com.russoviller.cryogenic.ui.screens.content.elements.MicrophoneCheckState
import com.russoviller.cryogenic.ui.screens.content.settings.DriverSummaryLayout
import com.russoviller.cryogenic.ui.screens.content.settings.RendererConfigDialog
import com.russoviller.cryogenic.ui.screens.content.settings.RendererSummaryLayout
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.EnumSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.ListSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCardColumn
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SimpleIDListCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.TextInputSettingsCard
import com.russoviller.cryogenic.ui.screens.content.versions.layouts.StatefulDropdownMenuFollowGlobal
import com.russoviller.cryogenic.ui.screens.content.versions.layouts.ToggleableIntSliderSettingsCard
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.platform.getMaxMemoryForSettings
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.utils.string.isNotEmptyOrBlank
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "VersionConfigScreen"

/** 页面退出后的配置保存作用域 */
private val configSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Composable
fun VersionConfigScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    onCheckVulkan: (Version) -> Unit,
    showToast: (AndroidStringText) -> Unit,
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
        Triple(NormalNavKey.Versions.Config, versionsScreenKey, false)
    ) { isVisible ->
        val config = version.getVersionConfig()
        //标记本次是否修改过配置
        var configDirty by remember { mutableStateOf(false) }

        DisposableEffect(version) {
            onDispose {
                if (configDirty) {
                    // 页面退出时统一进行保存
                    saveConfigAsync(config, submitError)
                }
            }
        }

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                VersionConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    config = config,
                    onConfigChanged = { configDirty = true },
                )
            }

            AnimatedItem { ctx ->
                GameConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    config = config,
                    onConfigChanged = { configDirty = true },
                )
            }

            AnimatedItem { ctx ->
                SupportConfigs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    config = config,
                    onConfigChanged = { configDirty = true },
                    onCheckVulkan = {
                        onCheckVulkan(version)
                    },
                    showToast = showToast,
                )
            }
        }
    }
}

@Composable
private fun VersionConfigs(
    config: VersionConfig,
    onConfigChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCardColumn(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_version_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        StatefulDropdownMenuFollowGlobal(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            currentValue = config.isolationType,
            onValueChange = { type ->
                if (config.isolationType != type) {
                    config.isolationType = type
                    onConfigChanged()
                }
            },
            title = stringResource(R.string.versions_config_isolation_title),
            summary = stringResource(R.string.versions_config_isolation_summary)
        )

        StatefulDropdownMenuFollowGlobal(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            currentValue = config.skipGameIntegrityCheck,
            onValueChange = { type ->
                if (config.skipGameIntegrityCheck != type) {
                    config.skipGameIntegrityCheck = type
                    onConfigChanged()
                }
            },
            title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
            summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
        )

        val renderers = Renderers.getRenderers()
        val renderersIdList = getIDList(renderers) { IDItem(it.getUniqueIdentifier(), it.getRendererName()) }
        var currentRendererId by remember { mutableStateOf(config.renderer) }
        val configurableRenderer = remember(currentRendererId) {
            if (currentRendererId.isNotEmpty()) {
                renderers.filterIsInstance<ConfigurableRenderer>()
                    .find { it.getUniqueIdentifier() == currentRendererId }
                    ?.takeIf { it.configSpec.entries.isNotEmpty() }
            } else null
        }
        var showConfigDialog by remember { mutableStateOf(false) }

        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = renderersIdList,
            autoCollapse = false,
            currentId = currentRendererId,
            defaultId = "",
            title = stringResource(R.string.versions_config_renderer),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                renderers.find { it.getUniqueIdentifier() == item.id }?.let { renderer ->
                    RendererSummaryLayout(renderer)
                }
            },
            trailingIcon = {
                // 设置跟随全局时不能独立修改
                if (configurableRenderer != null) {
                    CryoIconButton(onClick = { showConfigDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_filled),
                            contentDescription = stringResource(R.string.settings_renderer_config_title)
                        )
                    }
                }
            },
            onValueChange = { item ->
                if (config.renderer != item.id) {
                    config.renderer = item.id
                    currentRendererId = item.id
                    onConfigChanged()
                }
            }
        )

        if (showConfigDialog && configurableRenderer != null) {
            RendererConfigDialog(
                renderer = configurableRenderer,
                versionFolder = config.getVersionPath(),
                onDismissRequest = { showConfigDialog = false }
            )
        }

        val drivers = DriverPluginManager.getDriverList()
        val driversIdList = getIDList(drivers) { IDItem(it.id, it.name) }
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = driversIdList,
            autoCollapse = false,
            currentId = config.driver,
            defaultId = "",
            title = stringResource(R.string.versions_config_vulkan_driver),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                drivers.find { it.id == item.id }?.let { driver ->
                    DriverSummaryLayout(driver)
                }
            },
            onValueChange = { item ->
                if (config.driver != item.id) {
                    config.driver = item.id
                    onConfigChanged()
                }
            }
        )

        val graphicsApis = GraphicsApi.entries
        val defaultGraphicsTitle = stringResource(R.string.settings_game_graphics_api_default)
        val defaultOpenGLTitle = stringResource(R.string.settings_game_graphics_api_default_opengl)
        val graphicsApisList = getIDList(graphicsApis) {
            val title = when (it) {
                GraphicsApi.DEFAULT -> defaultGraphicsTitle
                GraphicsApi.DEFAULT_OPENGL -> defaultOpenGLTitle
                else -> it.displayName
            }
            IDItem(it.name, title)
        }
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            items = graphicsApisList,
            currentId = config.graphicsApi?.name ?: "",
            defaultId = "",
            title = stringResource(R.string.settings_game_graphics_api_title),
            summary = stringResource(R.string.settings_game_graphics_api_summary),
            getItemText = { it.title },
            getItemId = { it.id },
            onValueChange = { item ->
                if (config.graphicsApi?.name != item.id) {
                    config.graphicsApi = GraphicsApi.entries.find { it.name == item.id }
                    onConfigChanged()
                }
            }
        )

        val controls by ControlManager.dataList.collectAsStateWithLifecycle()
        val unspecifiedName = stringResource(R.string.generic_unspecified)
        val controlsIdList = getIDList(controls.filter { it.isSupport }) {
            IDItem(
                it.file.name,
                it.controlLayout.info.name.translate()
                    .takeIf { name -> name.isNotEmptyOrBlank() } ?: unspecifiedName
            )
        }
        ListSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            items = controlsIdList,
            currentId = config.control,
            defaultId = "",
            title = stringResource(R.string.versions_config_control),
            getItemText = { it.title },
            getItemId = { it.id },
            onValueChange = {
                if (config.control != it.id) {
                    config.control = it.id
                    onConfigChanged()
                }
            }
        )
    }
}

@Composable
private fun GameConfigs(
    config: VersionConfig,
    onConfigChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCardColumn(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_game_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        SimpleIDListCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            items = getIDList(RuntimesManager.getRuntimes().filter { it.isCompatible() }) { IDItem(it.name, it.name) },
            currentId = config.javaRuntime,
            defaultId = "",
            title = stringResource(R.string.settings_game_java_runtime_title),
            summary = stringResource(R.string.versions_config_java_runtime_summary),
            onValueChange = { item ->
                if (config.javaRuntime != item.id) {
                    config.javaRuntime = item.id
                    onConfigChanged()
                }
            }
        )

        /**
         * 临时已分配内存，用于UI状态更新
         */
        var ramAllocation by remember { mutableIntStateOf(config.ramAllocation) }
        ToggleableIntSliderSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            currentValue = config.ramAllocation,
            valueRange = AllSettings.ramAllocation.floatRange.start..getMaxMemoryForSettings(LocalContext.current).toFloat(),
            defaultValue = AllSettings.ramAllocation.getOrMin(),
            title = stringResource(R.string.settings_game_java_memory_title),
            summary = stringResource(R.string.settings_game_java_memory_summary),
            suffix = "MB",
            onValueChange = {
                config.ramAllocation = it
                ramAllocation = it
                onConfigChanged()
            },
            previewContent = {
                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = ramAllocation >= 256
                ) {
                    MemoryPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 8.dp),
                        preview = ramAllocation.takeIf { it >= 256 }?.toDouble(),
                    )
                }
            }
        )

        var gameArgs by remember { mutableStateOf(config.gameArgs) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            value = gameArgs,
            title = stringResource(R.string.versions_config_game_args_title),
            summary = stringResource(R.string.versions_config_game_args_summary),
            onValueChange = { value ->
                gameArgs = value
                if (config.gameArgs != value) {
                    config.gameArgs = value
                    onConfigChanged()
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_disable_if_blank))
            }
        )

        var jvmArgs by remember { mutableStateOf(config.jvmArgs) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            value = jvmArgs,
            title = stringResource(R.string.settings_game_jvm_args_title),
            summary = stringResource(R.string.settings_game_jvm_args_summary),
            onValueChange = { value ->
                jvmArgs = value
                if (config.jvmArgs != value) {
                    config.jvmArgs = value
                    onConfigChanged()
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        var serverIp by remember { mutableStateOf(config.serverIp) }
        TextInputSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            value = serverIp,
            title = stringResource(R.string.versions_config_auto_join_server_ip_title),
            summary = stringResource(R.string.versions_config_auto_join_server_ip_summary),
            onValueChange = { value ->
                serverIp = value
                if (config.serverIp != value) {
                    config.serverIp = value
                    onConfigChanged()
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_disable_if_blank))
            }
        )
    }
}

@Composable
private fun SupportConfigs(
    config: VersionConfig,
    onConfigChanged: () -> Unit,
    onCheckVulkan: () -> Unit,
    showToast: (AndroidStringText) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SettingsCardColumn(modifier = modifier) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_support_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        val vibrator = remember(context) { context.getSystemService<Vibrator>() }
        var touchVibrateKind by remember { mutableStateOf(config.touchVibrateKind) }
        val effectiveVibrateKind = touchVibrateKind ?: VibrationHandler.VibrateKind.default
        var touchVibrateDuration by remember { mutableIntStateOf(config.touchVibrateDuration) }
        EnumSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            value = effectiveVibrateKind,
            title = stringResource(R.string.versions_config_vibrate_kind_title),
            summary = stringResource(R.string.versions_config_vibrate_kind_summary),
            entries = VibrationHandler.VibrateKind.entries,
            getRadioEnable = { enum ->
                if (enum == VibrationHandler.VibrateKind.ONE_SHOT) {
                    true
                } else {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                }
            },
            getRadioText = { enum ->
                when (enum) {
                    VibrationHandler.VibrateKind.ONE_SHOT -> stringResource(R.string.vibrate_kind_one_shot)
                    VibrationHandler.VibrateKind.CLICK -> stringResource(R.string.vibrate_kind_click)
                    VibrationHandler.VibrateKind.DOUBLE_CLICK -> stringResource(R.string.vibrate_kind_double_click)
                    VibrationHandler.VibrateKind.HEAVY_CLICK -> stringResource(R.string.vibrate_kind_heavy_click)
                    VibrationHandler.VibrateKind.TICK -> stringResource(R.string.vibrate_kind_tick)
                }
            },
            maxItemsInEachRow = 4,
            onRadioClick = { enum ->
                touchVibrateKind = enum
                vibrator?.let { vibrator ->
                    VibrationHandler.vibrate(
                        vibrator = vibrator,
                        vibrateDuration = touchVibrateDuration,
                        vibrateKind = touchVibrateKind,
                    )
                }
                if (config.touchVibrateKind != enum) {
                    config.touchVibrateKind = enum
                    onConfigChanged()
                }
            }
        )

        AnimatedVisibility(effectiveVibrateKind == VibrationHandler.VibrateKind.ONE_SHOT) {
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                value = touchVibrateDuration,
                title = stringResource(R.string.versions_config_vibrate_duration_title),
                summary = stringResource(R.string.versions_config_vibrate_duration_summary),
                valueRange = 80f..500f,
                onValueChange = {
                    touchVibrateDuration = it
                    config.touchVibrateDuration = touchVibrateDuration
                    onConfigChanged()
                },
                onValueChangeFinished = {
                    vibrator?.let { vibrator ->
                        VibrationHandler.vibrate(
                            vibrator = vibrator,
                            vibrateDuration = touchVibrateDuration,
                            vibrateKind = touchVibrateKind,
                        )
                    }
                },
                suffix = "ms",
                fineTuningControl = true
            )
        }

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            title = stringResource(R.string.game_vulkan_check_title),
            summary = stringResource(R.string.game_vulkan_check_text),
            onClick = onCheckVulkan
        )

        //检查麦克风
        var microphoneState by remember { mutableStateOf<MicrophoneCheckState>(MicrophoneCheckState.None) }
        MicrophoneCheckOperation(
            state = microphoneState,
            changeState = { microphoneState = it },
            onShowToast = showToast,
        )
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.versions_config_microphone_check_title),
            summary = stringResource(R.string.versions_config_microphone_check_summary),
            onClick = {
                microphoneState = MicrophoneCheckState.Start
            }
        )
    }
}

@Composable
private fun <E> getIDList(list: List<E>, toIDItem: (E) -> IDItem): List<IDItem> {
    return list.map {
        toIDItem(it)
    }.toMutableList().apply {
        add(0, IDItem("", stringResource(R.string.generic_follow_global)))
    }
}

private fun saveConfigAsync(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    configSaveScope.launch {
        runCatching {
            config.saveWithThrowable()
        }.onFailure { e ->
            Logger.error(TAG, "Failed to save version config!", e)
            submitError(
                ErrorViewModel.ThrowableMessage(
                    title = androidText(R.string.versions_config_failed_to_save),
                    message = androidText(e.getMessageOrToString())
                )
            )
        }
    }
}
