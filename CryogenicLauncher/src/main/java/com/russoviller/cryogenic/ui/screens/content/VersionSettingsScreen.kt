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

package com.russoviller.cryogenic.ui.screens.content

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.russoviller.cryogenic.ui.components.CryoTab
import com.russoviller.cryogenic.ui.components.CryoTabStrip
import com.russoviller.cryogenic.ui.components.CryoRailItem
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.notification.NotificationManager
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.base.VersionViewModelStoreScope
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.screens.NestedNavDisplay
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.CategoryIcon
import com.russoviller.cryogenic.ui.screens.content.elements.CategoryItem
import com.russoviller.cryogenic.ui.screens.content.versions.ModifyVersionScreen
import com.russoviller.cryogenic.ui.screens.content.versions.ModsManagerScreen
import com.russoviller.cryogenic.ui.screens.content.versions.ResourcePackManageScreen
import com.russoviller.cryogenic.ui.screens.content.versions.SavesManagerScreen
import com.russoviller.cryogenic.ui.screens.content.versions.ScreenshotsManagerScreen
import com.russoviller.cryogenic.ui.screens.content.versions.ServerListScreen
import com.russoviller.cryogenic.ui.screens.content.versions.SchematicsManagerScreen
import com.russoviller.cryogenic.ui.screens.content.versions.ShadersManagerScreen
import com.russoviller.cryogenic.ui.screens.content.versions.VersionConfigScreen
import com.russoviller.cryogenic.ui.screens.content.versions.VersionOverViewScreen
import com.russoviller.cryogenic.ui.screens.navigateOnce
import com.russoviller.cryogenic.ui.screens.onBack
import com.russoviller.cryogenic.ui.screens.rememberTransitionSpec
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.ModifyOperation
import com.russoviller.cryogenic.viewmodel.ModifyPayload
import com.russoviller.cryogenic.viewmodel.ModifyVersionViewModel
import com.russoviller.cryogenic.viewmodel.ScreenBackStackViewModel
import com.russoviller.cryogenic.viewmodel.sendToast

@Composable
fun VersionSettingsScreen(
    key: NestedNavKey.VersionSettings,
    modifyViewModel: ModifyVersionViewModel,
    backScreenViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    onExportModpack: () -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    val cBackToMainScreen by rememberUpdatedState(backToMainScreen)
    DisposableEffect(key) {
        val listener = object : suspend () -> Unit {
            override suspend fun invoke() {
                cBackToMainScreen()
            }
        }
        VersionsManager.registerListener(listener)
        onDispose {
            VersionsManager.unregisterListener(listener)
        }
    }

    VersionViewModelStoreScope(version = key.version) {
        BaseScreen(
            screenKey = key,
            currentKey = backScreenViewModel.mainScreen.currentKey
        ) { isVisible ->
            Column(modifier = Modifier.fillMaxSize()) {
                TabMenu(
                    isVisible = isVisible,
                    backStack = key.backStack,
                    versionsScreenKey = key.currentKey,
                    version = key.version,
                    modifier = Modifier.fillMaxWidth()
                )

                NavigationUI(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    key = key,
                    modifyViewModel = modifyViewModel,
                    backScreenViewModel = backScreenViewModel,
                    versionsScreenKey = key.currentKey,
                    onCurrentKeyChange = { newKey ->
                        key.currentKey = newKey
                    },
                    backToMainScreen = backToMainScreen,
                    onExport = onExportModpack,
                    version = key.version,
                    onModify = { payload ->
                        if (modifyViewModel.installOperation !is ModifyOperation.None) {
                            //不是待修改状态，拒绝此次修改
                            return@NavigationUI
                        }
                        if (!NotificationManager.checkNotificationEnabled(context)) {
                            //警告通知权限
                            modifyViewModel.installOperation = ModifyOperation.WarningForNotification(payload)
                        } else {
                            modifyViewModel.installOperation = ModifyOperation.Confirm(payload)
                        }
                    },
                    eventViewModel = eventViewModel,
                    submitError = submitError
                )
            }
        }
    }
}

private val settingItems = listOf(
    CategoryItem(NormalNavKey.Versions.OverView, { CategoryIcon(R.drawable.ic_dashboard_outlined, R.string.versions_settings_overview) }, R.string.versions_settings_overview),
    CategoryItem(NormalNavKey.Versions.Config, { CategoryIcon(R.drawable.ic_build_outlined, R.string.versions_settings_config) }, R.string.versions_settings_config),
    CategoryItem(NormalNavKey.Versions.ModifyVersion, { CategoryIcon(R.drawable.ic_edit_outlined, R.string.versions_modify_version) }, R.string.versions_modify_version),
    CategoryItem(NormalNavKey.Versions.ModsManager, { CategoryIcon(R.drawable.ic_extension_outlined, R.string.mods_manage) }, R.string.mods_manage, division = true),
    CategoryItem(NormalNavKey.Versions.ResourcePackManager, { CategoryIcon(R.drawable.ic_format_paint_outlined, R.string.resource_pack_manage) }, R.string.resource_pack_manage),
    CategoryItem(NormalNavKey.Versions.ShadersManager, { CategoryIcon(R.drawable.ic_lightbulb, R.string.shader_pack_manage) }, R.string.shader_pack_manage),
    CategoryItem(NormalNavKey.Versions.SavesManager, { CategoryIcon(R.drawable.ic_public, R.string.saves_manage) }, R.string.saves_manage),
    CategoryItem(NormalNavKey.Versions.ScreenshotsManager, { CategoryIcon(R.drawable.ic_photo_library_outlined, R.string.screenshots_manage) }, R.string.screenshots_manage),
    CategoryItem(NormalNavKey.Versions.SchematicsManager, { CategoryIcon(R.drawable.ic_package_2_outlined, R.string.cryo_schematics) }, R.string.cryo_schematics),
    CategoryItem(NormalNavKey.Versions.ServerList, { CategoryIcon(R.drawable.ic_dns_outlined, R.string.servers_list) }, R.string.servers_list, division = true),
)

/** Conta mods, pacotes de textura, shaders, mundos, capturas e esquemas da instância. */
private fun countVersionFolders(version: Version): Map<TitledNavKey, Int> {
    val gameDir = version.getGameDir()
    fun list(name: String) = java.io.File(gameDir, name).listFiles()?.filter { !it.name.startsWith(".") }.orEmpty()
    fun ext(file: java.io.File, vararg exts: String) = exts.any { file.name.endsWith(it, ignoreCase = true) }
    return mapOf(
        NormalNavKey.Versions.ModsManager to list("mods").count {
            it.isFile && ext(it, ".jar", ".jar.disabled", ".litemod", ".litemod.disabled")
        },
        NormalNavKey.Versions.ResourcePackManager to list("resourcepacks").count {
            it.isDirectory || ext(it, ".zip", ".zip.disabled")
        },
        NormalNavKey.Versions.ShadersManager to list("shaderpacks").count {
            it.isDirectory || ext(it, ".zip", ".zip.disabled")
        },
        NormalNavKey.Versions.SavesManager to list("saves").count { it.isDirectory },
        NormalNavKey.Versions.ScreenshotsManager to list("screenshots").count {
            it.isFile && ext(it, ".png", ".jpg", ".jpeg")
        },
        NormalNavKey.Versions.SchematicsManager to list("schematics").count {
            it.isFile && ext(it, ".litematic", ".schem", ".schematic", ".nbt")
        }
    )
}

@Composable
private fun TabMenu(
    isVisible: Boolean,
    backStack: NavBackStack<TitledNavKey>,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    modifier: Modifier = Modifier
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    //Quantidade de itens em cada pasta, recontada ao trocar de aba (ex.: depois de apagar mods)
    var counts by remember(version) { mutableStateOf<Map<TitledNavKey, Int>>(emptyMap()) }
    LaunchedEffect(version, versionsScreenKey, isVisible) {
        counts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { countVersionFolders(version) }.getOrDefault(emptyMap())
        }
    }
    CryoTabStrip(
        modifier = modifier.animated(ctx, from = AnimatedDirection.Top),
        tabs = settingItems.map { item ->
            CryoTab(
                icon = { item.icon() },
                label = stringResource(item.textRes),
                selected = versionsScreenKey === item.key,
                onClick = { backStack.navigateOnce(item.key) },
                badge = counts[item.key]?.toString()
            )
        }
    )
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    key: NestedNavKey.VersionSettings,
    modifyViewModel: ModifyVersionViewModel,
    backScreenViewModel: ScreenBackStackViewModel,
    versionsScreenKey: TitledNavKey?,
    onCurrentKeyChange: (TitledNavKey?) -> Unit,
    backToMainScreen: () -> Unit,
    onExport: () -> Unit,
    version: Version,
    onModify: (ModifyPayload) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = key,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.Versions.OverView> {
                    VersionOverViewScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        backToMainScreen = backToMainScreen,
                        onExport = onExport,
                        version = version,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.Config> {
                    VersionConfigScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        onCheckVulkan = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.VulkanCheck(version)
                            )
                        },
                        showToast = { text ->
                            eventViewModel.sendToast(text)
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ModifyVersion> {
                    ModifyVersionScreen(
                        viewModel = modifyViewModel,
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        eventViewModel = eventViewModel,
                        onModify = onModify
                    )
                }
                entry(NormalNavKey.Versions.ModsManager) {
                    ModsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadModScreen
                            )
                        },
                        onSwapMoreInfo = { projectId, platform ->
                            backScreenViewModel.mainScreen.removeAndNavigateTo(
                                NestedNavKey.AssetInfo::class,
                                NestedNavKey.AssetInfo(platform, projectId, PlatformClasses.MOD)
                            )
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.SavesManager> {
                    SavesManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadSavesScreen
                            )
                        },
                        onQuickPlay = { version, saveName ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.PlaySave(
                                    version = version,
                                    saveName = saveName
                                )
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ResourcePackManager> {
                    ResourcePackManageScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload =  {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadResourcePackScreen
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ShadersManager> {
                    ShadersManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadShadersScreen
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ScreenshotsManager> {
                    ScreenshotsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.SchematicsManager> {
                    SchematicsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ServerList> {
                    ServerListScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        onQuickPlay = { version, address ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.PlayServer(
                                    version = version,
                                    address = address
                                )
                            )
                        },
                        backToMainScreen = backToMainScreen,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}
