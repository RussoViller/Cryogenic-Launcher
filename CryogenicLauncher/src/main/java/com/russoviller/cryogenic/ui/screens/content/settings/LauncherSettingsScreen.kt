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

import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoBackground
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.TextInputSettingsCard
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.PaletteStyle
import com.russoviller.colorpicker.ColorPickerController
import com.russoviller.colorpicker.components.HueBarPicker
import com.russoviller.colorpicker.rememberColorPickerController
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.contract.MediaPickerContract
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.AppLanguage
import com.russoviller.cryogenic.setting.enums.applyLanguage
import com.russoviller.cryogenic.setting.enums.BackgroundBlur
import com.russoviller.cryogenic.setting.enums.DarkMode
import com.russoviller.cryogenic.setting.enums.MirrorSourceType
import com.russoviller.cryogenic.setting.unit.floatRange
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedColumn
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.RadioCard
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleEditDialog
import com.russoviller.cryogenic.ui.components.TitleAndSummary
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.components.toColorOrNull
import com.russoviller.cryogenic.ui.components.toHex
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.DisabledAlpha
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.EnumSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.ListSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCardColumn
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.file.shareFile
import com.russoviller.cryogenic.utils.isChinaMainland
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.BackgroundViewModel
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel
import kotlinx.coroutines.Dispatchers
import java.io.File

private const val TAG = "LauncherSettingsScreen"

private sealed interface CustomColorOperation {
    data object None : CustomColorOperation
    /** 展示自定义主题颜色 Dialog */
    data object Dialog: CustomColorOperation
}

@Composable
fun LauncherSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
) {
    val context = LocalContext.current

    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Launcher, settingsScreenKey, false)
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
                    var customColorOperation by remember { mutableStateOf<CustomColorOperation>(CustomColorOperation.None) }
                    CustomColorOperation(
                        customColorOperation = customColorOperation,
                        updateOperation = { customColorOperation = it }
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.launcherLanguage,
                        items = AppLanguage.entries,
                        title = stringResource(R.string.settings_launcher_language),
                        summary = stringResource(R.string.cryo_language_summary),
                        getItemText = { stringResource(it.textRes) },
                        onValueChange = { applyLanguage(it) }
                    )

                    var showThemeStudio by remember { mutableStateOf(false) }
                    ThemeStudioCard(onOpen = { showThemeStudio = true })
                    if (showThemeStudio) {
                        ThemeStudioDialog(
                            onDismiss = { showThemeStudio = false },
                            onEditCustomColor = { customColorOperation = CustomColorOperation.Dialog }
                        )
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.launcherFullScreen,
                        title = stringResource(R.string.settings_launcher_full_screen_title),
                        summary = stringResource(R.string.settings_launcher_full_screen_summary)
                    )
                }
            }

            //Modo inativo: bloco próprio, separado do Estúdio de temas
            AnimatedItem { ctx ->
                var showIdleMode by remember { mutableStateOf(false) }
                IdleModeCard(
                    modifier = Modifier.animated(ctx),
                    onOpen = { showIdleMode = true }
                )
                if (showIdleMode) {
                    IdleModeDialog(onDismiss = { showIdleMode = false })
                }
            }

            //Wallpaper principal e outros: wallpaper, brilho e animações em uma janela própria
            AnimatedItem { ctx ->
                var showAppearance by remember { mutableStateOf(false) }
                CryoSettingsEntryCard(
                    modifier = Modifier.animated(ctx),
                    iconRes = R.drawable.ic_image_outlined,
                    title = stringResource(R.string.cryo_tab_appearance),
                    summary = listOf(
                        stringResource(R.string.cryo_cat_wallpaper),
                        stringResource(R.string.cryo_cat_glow),
                        stringResource(R.string.cryo_cat_transitions)
                    ).joinToString(" · "),
                    onOpen = { showAppearance = true }
                )
                if (showAppearance) {
                    AppearanceSettingsDialog(onDismiss = { showAppearance = false }, submitError = submitError)
                }
            }

            //Discord: opções em uma janela própria
            AnimatedItem { ctx ->
                var showDiscord by remember { mutableStateOf(false) }
                CryoSettingsEntryCard(
                    modifier = Modifier.animated(ctx),
                    iconRes = R.drawable.ic_cryo_discord,
                    title = stringResource(R.string.cryo_tab_discord),
                    summary = stringResource(R.string.cryo_discord_summary),
                    onOpen = { showDiscord = true }
                )
                if (showDiscord) {
                    DiscordSettingsDialog(onDismiss = { showDiscord = false })
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    //这些镜像源都是为了改善中国大陆内陆的网络环境而存在的
                    //境外不需要这些镜像源，反而可能拖慢境外的下载速度
                    //所以不应该向中国境外开放这些选项
                    val isChinaMainland = remember { isChinaMainland() }
                    if (isChinaMainland) {
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = AllSettings.gameDownloadSource,
                            items = MirrorSourceType.entries,
                            title = stringResource(R.string.settings_launcher_mirror_game_source_title),
                            getItemText = { stringResource(it.textRes) }
                        )

                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.assetPlatformSource,
                            items = MirrorSourceType.entries,
                            title = stringResource(R.string.settings_launcher_mirror_asset_platform_source_title),
                            getItemText = { stringResource(it.textRes) }
                        )
                    }

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = if (isChinaMainland) {
                            CardPosition.Middle
                        } else {
                            CardPosition.Top
                        },
                        unit = AllSettings.launcherLogRetentionDays,
                        title = stringResource(R.string.settings_launcher_log_retention_days_title),
                        summary = stringResource(R.string.settings_launcher_log_retention_days_summary),
                        valueRange = AllSettings.launcherLogRetentionDays.floatRange,
                        suffix = stringResource(R.string.unit_day)
                    )

                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        title = stringResource(R.string.settings_launcher_log_share_title),
                        summary = stringResource(R.string.settings_launcher_log_share_summary),
                        onClick = {
                            TaskSystem.submitTask(
                                Task.runTask(
                                    id = "ZIP_LOGS",
                                    task = { task ->
                                        task.updateProgress(-1f)
                                        task.updateMessage(androidText(R.string.settings_launcher_log_share_packing))
                                        val logsFile = File(PathManager.DIR_CACHE, "logs.zip")
                                        Logger.pack(logsFile)
                                        task.updateProgress(1f)
                                        task.updateMessage(null)
                                        //分享压缩包
                                        shareFile(
                                            context = context,
                                            file = logsFile
                                        )
                                    },
                                    onError = { e ->
                                        Logger.error(TAG, "Failed to package log files.", e)
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Janela "Wallpaper principal e outros": wallpaper, brilho e animações de transição. */
@Composable
internal fun AppearanceSettingsDialog(
    onDismiss: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
) {
    CryoSettingsDialog(
        iconRes = R.drawable.ic_image_outlined,
        title = stringResource(R.string.cryo_tab_appearance),
        onDismiss = onDismiss
    ) {
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            isVisible = true
        ) {
            AnimatedItem { ctx ->
                CryoSettingsHeader(modifier = Modifier.animated(ctx), text = stringResource(R.string.cryo_cat_wallpaper))
            }
            //启动器背景设置板块
            LocalBackgroundViewModel.current?.let { backgroundViewModel ->
                AnimatedItem { ctx ->
                    SettingsCardColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animated(ctx)
                    ) {
                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top
                        ) {
                            CustomBackground(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundViewModel = backgroundViewModel,
                                submitError = submitError
                            )
                        }

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.launcherBackgroundOpacity,
                            title = stringResource(R.string.settings_launcher_background_opacity_title),
                            summary = stringResource(R.string.settings_launcher_background_opacity_summary),
                            valueRange = AllSettings.launcherBackgroundOpacity.floatRange,
                            suffix = "%",
                            fineTuningControl = true
                        )

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.videoBackgroundVolume,
                            title = stringResource(R.string.settings_launcher_background_video_volume_title),
                            summary = stringResource(R.string.settings_launcher_background_video_volume_summary),
                            valueRange = AllSettings.videoBackgroundVolume.floatRange,
                            suffix = "%",
                            enabled = backgroundViewModel.isValid && backgroundViewModel.isVideo,
                            fineTuningControl = true
                        )

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Bottom,
                            unit = AllSettings.backgroundBlur,
                            title = stringResource(R.string.settings_title_blur),
                            summary = stringResource(R.string.settings_launcher_background_blur_summary),
                            valueRange = AllSettings.backgroundBlur.floatRange,
                            suffix = "Dp",
                            fineTuningControl = true,
                            appendContent = {
                                val unit = AllSettings.backgroundBlurType
                                val state = unit.state
                                IconButton(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = DisabledAlpha),
                                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = DisabledAlpha),
                                    ),
                                    onClick = {
                                        unit.save(state.switch())
                                    },
                                ) {
                                    Crossfade(
                                        targetState = state
                                    ) { target ->
                                        val painter = when (target) {
                                            BackgroundBlur.Background -> painterResource(R.drawable.ic_blur_circular_outlined)
                                            BackgroundBlur.Foreground -> painterResource(R.drawable.ic_blur_circular_filled)
                                        }
                                        Icon(
                                            painter = painter,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            AnimatedItem { ctx ->
                CryoSettingsHeader(modifier = Modifier.animated(ctx), text = stringResource(R.string.cryo_cat_glow))
            }
            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    //Liquid Glass e a cutscene de entrada
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.cryoInterface2,
                        title = stringResource(R.string.cryo_ui2_setting_title),
                        summary = stringResource(R.string.cryo_ui2_setting_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoLiquidSkipCutscene,
                        title = stringResource(R.string.cryo_ui2_cutscene_title),
                        summary = stringResource(R.string.cryo_ui2_cutscene_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoLiquidSkipHold,
                        title = stringResource(R.string.cryo_ui2_skiphold_title),
                        summary = stringResource(R.string.cryo_ui2_skiphold_summary)
                    )
                    val glowEnabled = AllSettings.cryoGlowEnabled.state
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoGlowEnabled,
                        title = stringResource(R.string.cryo_settings_glow_title),
                        summary = stringResource(R.string.cryo_settings_glow_summary)
                    )
                    AnimatedVisibility(visible = glowEnabled) {
                        Column {
                            IntSliderSettingsCard(
                                modifier = Modifier.fillMaxWidth(),
                                position = CardPosition.Middle,
                                unit = AllSettings.cryoGlowSize,
                                title = stringResource(R.string.cryo_settings_glow_size_title),
                                summary = stringResource(R.string.cryo_settings_glow_size_summary),
                                valueRange = AllSettings.cryoGlowSize.floatRange,
                                fineTuningControl = true
                            )
                            IntSliderSettingsCard(
                                modifier = Modifier.fillMaxWidth(),
                                position = CardPosition.Middle,
                                unit = AllSettings.cryoGlowIntensity,
                                title = stringResource(R.string.cryo_settings_glow_intensity_title),
                                summary = stringResource(R.string.cryo_settings_glow_intensity_summary),
                                valueRange = AllSettings.cryoGlowIntensity.floatRange,
                                suffix = "%",
                                fineTuningControl = true
                            )
                        }
                    }
                    val neonEnabled = AllSettings.cryoNeonEnabled.state
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = if (neonEnabled) CardPosition.Middle else CardPosition.Bottom,
                        unit = AllSettings.cryoNeonEnabled,
                        title = stringResource(R.string.cryo_neon_title),
                        summary = stringResource(R.string.cryo_neon_summary)
                    )
                    AnimatedVisibility(visible = neonEnabled) {
                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Bottom,
                            unit = AllSettings.cryoNeonThickness,
                            title = stringResource(R.string.cryo_neon_thickness_title),
                            valueRange = AllSettings.cryoNeonThickness.floatRange,
                            fineTuningControl = true
                        )
                    }
                }
            }

            AnimatedItem { ctx ->
                CryoSettingsHeader(modifier = Modifier.animated(ctx), text = stringResource(R.string.cryo_cat_transitions))
            }
            //动画设置板块
            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.launcherSwapAnimEnabled,
                        title = stringResource(R.string.settings_launcher_swap_anim_enabled_title),
                        summary = stringResource(R.string.settings_launcher_swap_anim_enabled_summary)
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoTransitionStyle,
                        items = com.russoviller.cryogenic.setting.enums.CryoTransitionStyle.entries,
                        title = stringResource(R.string.cryo_trans_style_title),
                        getItemText = { stringResource(it.textRes) },
                        enabled = AllSettings.launcherSwapAnimEnabled.state
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoTransitionIntensity,
                        title = stringResource(R.string.cryo_trans_intensity_title),
                        summary = stringResource(R.string.cryo_trans_intensity_summary),
                        valueRange = AllSettings.cryoTransitionIntensity.floatRange,
                        suffix = "%",
                        enabled = AllSettings.launcherSwapAnimEnabled.state,
                        fineTuningControl = true
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.launcherAnimateSpeedLevel,
                        title = stringResource(R.string.settings_launcher_animate_speed_title),
                        summary = stringResource(R.string.settings_launcher_animate_speed_summary),
                        valueRange = AllSettings.launcherAnimateSpeedLevel.floatRange,
                        valueText = { value -> "×" + (value / 10f) },
                        enabled = AllSettings.launcherSwapAnimEnabled.state,
                    )
                }
            }

        }
    }
}

/** Janela só com as opções do Discord. */
@Composable
internal fun DiscordSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    CryoSettingsDialog(
        iconRes = R.drawable.ic_cryo_discord,
        title = stringResource(R.string.cryo_tab_discord),
        onDismiss = onDismiss
    ) {
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            isVisible = true
        ) {
            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    val discordInstalled = remember { com.russoviller.cryogenic.discord.CryoDiscordPresence.isDiscordInstalled(context) }
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.cryoDiscordPresence,
                        title = stringResource(R.string.cryo_discord_title),
                        summary = if (discordInstalled) {
                            stringResource(R.string.cryo_discord_summary)
                        } else {
                            stringResource(R.string.cryo_discord_not_installed)
                        },
                        onCheckedChange = {
                            com.russoviller.cryogenic.discord.CryoDiscordPresence.settingsChanged(context)
                        }
                    )
                    DiscordStatusRow()
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoDiscordShowServer,
                        title = stringResource(R.string.cryo_discord_show_server_title),
                        summary = stringResource(R.string.cryo_discord_show_server_summary)
                    )
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.cryoDiscordShowMusic,
                        title = stringResource(R.string.cryo_discord_show_music_title),
                        summary = stringResource(R.string.cryo_discord_show_music_summary),
                        onCheckedChange = {
                            com.russoviller.cryogenic.discord.CryoDiscordPresence.refresh()
                        }
                    )
                    TextInputSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.cryoDiscordAppId,
                        title = stringResource(R.string.cryo_discord_app_id_title),
                        summary = stringResource(R.string.cryo_discord_app_id_summary),
                        onValueChange = {
                            com.russoviller.cryogenic.discord.CryoDiscordPresence.settingsChanged(context)
                        }
                    )
                }
            }

        }
    }
}

/** Cartão no estilo do Estúdio de temas / Modo inativo, que abre uma janela de opções. */
@Composable
internal fun CryoSettingsEntryCard(
    iconRes: Int,
    title: String,
    summary: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = com.russoviller.cryogenic.ui.components.CryoGlassColors
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            //Segue a opacidade da interface, igual aos outros cartões dos Ajustes
            .cryoGlass(shape = shape, cornerRadius = 20.dp)
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(54.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .cryoBackground(com.russoviller.cryogenic.ui.components.cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = glass.Deep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, glass.Ice))
                )
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = glass.Frost.copy(alpha = 0.8f)
            )
        }
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            tint = glass.Frost
        )
    }
}

/** Janela opaca (igual à do Modo inativo) com título, botão de fechar e conteúdo rolável. */
@Composable
private fun CryoSettingsDialog(
    iconRes: Int,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val glass = com.russoviller.cryogenic.ui.components.CryoGlassColors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.russoviller.cryogenic.ui.components.LocalCryoGlassFixed provides true
        ) {
        Column(
            modifier = Modifier
                .padding(all = 12.dp)
                .widthIn(max = 720.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = rememberDialogMaxHeight())
                //Segue a opacidade da interface escolhida pelo usuário
                .cryoGlass(shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
                .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = glass.Ice
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, glass.Ice))
                    )
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .border(1.dp, com.russoviller.cryogenic.ui.components.cryoBorderBrush(), androidx.compose.foundation.shape.CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.generic_close),
                        tint = glass.Frost
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
        }
    }
}

@Composable
private fun CryoSettingsHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice,
                            com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora
                        )
                    )
                )
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
        )
    }
}

@Composable
private fun CustomColorOperation(
    customColorOperation: CustomColorOperation,
    updateOperation: (CustomColorOperation) -> Unit
) {
    when (customColorOperation) {
        is CustomColorOperation.None -> {}
        is CustomColorOperation.Dialog -> {
            ColorStudioDialog(
                initialColor = Color(AllSettings.launcherCustomColor.getValue()),
                initialStyle = AllSettings.launcherCustomPaletteStyle.getValue(),
                initialGradient = AllSettings.cryoCustomGradient.getValue(),
                onCancel = { updateOperation(CustomColorOperation.None) },
                onConfirm = { color, style, gradient ->
                    AllSettings.launcherCustomColor.save(color.toArgb())
                    AllSettings.launcherCustomPaletteStyle.save(style)
                    AllSettings.cryoCustomGradient.save(gradient)
                    updateOperation(CustomColorOperation.None)
                }
            )
        }
    }
}

@Composable
private fun CustomThemeDialog(
    colorController: ColorPickerController,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    onChangeFinished: () -> Unit = {},
    onCancel: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val selectedColor by colorController.color
    val selectedHex = remember(selectedColor) {
        selectedColor.toHex()
    }

    /**
     * 是否开启编辑Hex对话框
     */
    var editHex by remember {
        mutableStateOf(false)
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 16.dp)
                    .heightIn(max = (maxHeight - 32.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shadowElevation = 3.dp,
                color = cardColor(false),
                contentColor = onCardColor(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(all = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_launcher_color_theme_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val scrollState = rememberLazyListState()
                            //颜色风格
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fadeEdge(scrollState)
                                    .nonInteractiveScrollbar(
                                        state = scrollState.scrollIndicatorState!!,
                                        orientation = Orientation.Vertical,
                                    ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                state = scrollState,
                            ) {
                                //标题
                                item {
                                    Text(
                                        text = stringResource(R.string.settings_launcher_color_theme_style),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                items(PaletteStyle.entries) { style ->
                                    RadioCard(
                                        selected = paletteStyle == style,
                                        text = style.name,
                                        onClick = {
                                            onPaletteStyleChange(style)
                                        }
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScrollWithBar(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HueBarPicker(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .fillMaxWidth(),
                                    controller = colorController,
                                    onChangeFinished = onChangeFinished
                                )

                                //颜色预览
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val originalColor = remember {
                                        colorController.getOriginalColor()
                                    }

                                    //初始颜色
                                    Text(
                                        text = originalColor.toHex(),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .background(color = originalColor)
                                    )
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    //当前颜色
                                    Text(
                                        text = selectedHex,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .background(color = selectedColor)
                                        )
                                        //手动编辑Hex
                                        IconButton(
                                            modifier = Modifier.size(36.dp),
                                            onClick = { editHex = true }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_edit_outlined),
                                                contentDescription = stringResource(R.string.theme_color_picker_edit_hex)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onChangeFinished()
                                onCancel()
                            }
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        CryoButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onConfirm(selectedColor)
                            }
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_confirm))
                        }
                    }
                }
            }
        }
    }

    if (editHex) {
        var value by remember {
            mutableStateOf(selectedHex)
        }
        val newColor = remember(value) {
            //尝试转换为颜色对象
            value.toColorOrNull()
        }

        SimpleEditDialog(
            title = stringResource(R.string.theme_color_picker_edit_hex),
            value = value,
            onValueChange = { new ->
                value = new
            },
            isError = newColor == null,
            supportingText = {
                if (newColor == null) {
                    Text(text = stringResource(R.string.theme_color_picker_edit_hex_invalid))
                }
            },
            onDismissRequest = { editHex = false },
            onConfirm = {
                if (newColor != null) {
                    colorController.setColor(newColor.copy(alpha = 1f))
                    editHex = false
                }
            }
        )
    }
}

private sealed interface BackgroundOperation {
    data object None : BackgroundOperation
    data object PreReset : BackgroundOperation
    data object Reset : BackgroundOperation
}

@Composable
private fun CustomBackground(
    backgroundViewModel: BackgroundViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var operation by remember { mutableStateOf<BackgroundOperation>(BackgroundOperation.None) }

    BackgroundOperation(
        operation = operation,
        changeOperation = { operation = it },
        backgroundViewModel = backgroundViewModel
    )

    val importErrorText = stringResource(R.string.error_import_image)
    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = true,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(R.string.settings_launcher_background_importing))
                        backgroundViewModel.import(context, result[0] /* 取决于上面的allowMultiple，此处一定会是单个元素的列表 */)
                    },
                    onError = { th ->
                        backgroundViewModel.delete()
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(importErrorText),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { filePicker.launch(Unit) }
                .padding(all = 16.dp),
        ) {
            TitleAndSummary(
                title = stringResource(R.string.settings_launcher_background_title),
                summary = stringResource(R.string.settings_launcher_background_summary),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.padding(horizontal = 16.dp),
            visible = backgroundViewModel.isValid
        ) {
            IconTextButton(
                painter = painterResource(R.drawable.ic_restart_alt),
                text = stringResource(R.string.generic_reset),
                onClick = {
                    if (operation == BackgroundOperation.None) {
                        operation = BackgroundOperation.PreReset
                    }
                }
            )
        }
    }
}

@Composable
private fun BackgroundOperation(
    operation: BackgroundOperation,
    changeOperation: (BackgroundOperation) -> Unit,
    backgroundViewModel: BackgroundViewModel
) {
    when (operation) {
        is BackgroundOperation.None -> {}
        is BackgroundOperation.PreReset -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.settings_launcher_background_reset_message),
                onConfirm = {
                    changeOperation(BackgroundOperation.Reset)
                },
                onDismiss = {
                    changeOperation(BackgroundOperation.None)
                }
            )
        }
        is BackgroundOperation.Reset -> {
            LaunchedEffect(Unit) {
                backgroundViewModel.delete()
                changeOperation(BackgroundOperation.None)
            }
        }
    }
}


@Composable
private fun DiscordStatusRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot by com.russoviller.cryogenic.discord.CryoDiscordPresence.status.collectAsStateWithLifecycle()
    val ice = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    val (color, text) = when (snapshot.status) {
        com.russoviller.cryogenic.discord.CryoDiscordPresence.Status.DISABLED -> androidx.compose.ui.graphics.Color.Gray to stringResource(R.string.cryo_discord_status_disabled)
        com.russoviller.cryogenic.discord.CryoDiscordPresence.Status.NOT_INSTALLED -> androidx.compose.ui.graphics.Color(0xFFFFB74D) to stringResource(R.string.cryo_discord_status_not_installed)
        com.russoviller.cryogenic.discord.CryoDiscordPresence.Status.CONNECTING -> ice to stringResource(R.string.cryo_discord_status_connecting)
        com.russoviller.cryogenic.discord.CryoDiscordPresence.Status.WAITING -> ice to stringResource(R.string.cryo_discord_status_waiting, snapshot.appId.toString())
        com.russoviller.cryogenic.discord.CryoDiscordPresence.Status.CONNECTED -> androidx.compose.ui.graphics.Color(0xFF7CF5A8) to stringResource(R.string.cryo_discord_status_connected, snapshot.appId.toString())
        com.russoviller.cryogenic.discord.CryoDiscordPresence.Status.ERROR -> androidx.compose.ui.graphics.Color(0xFFFF6B81) to stringResource(R.string.cryo_discord_status_error, snapshot.message ?: "?")
    }
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        com.russoviller.cryogenic.ui.components.CryoTonalButton(
            onClick = { com.russoviller.cryogenic.discord.CryoDiscordPresence.reconnect(context) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(text = stringResource(R.string.cryo_discord_reconnect), maxLines = 1)
        }
    }
}
