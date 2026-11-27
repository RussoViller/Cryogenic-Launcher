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

import com.russoviller.cryogenic.ui.components.cryoTacticalBackdrop
import com.russoviller.cryogenic.ui.components.cryoBackground
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import com.russoviller.cryogenic.ui.screens.content.versions.layouts.CryoVersionPage
import com.russoviller.cryogenic.ui.screens.content.versions.layouts.CryoVersionInfoPanel
import com.russoviller.cryogenic.ui.components.CryoButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.addons.modloader.AddonVersion
import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import com.russoviller.cryogenic.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.russoviller.cryogenic.game.addons.modloader.fabriclike.legacyfabric.LegacyFabricVersions
import com.russoviller.cryogenic.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.russoviller.cryogenic.game.addons.modloader.forgelike.forge.ForgeVersions
import com.russoviller.cryogenic.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.russoviller.cryogenic.game.addons.modloader.optifine.OptiFineVersions
import com.russoviller.cryogenic.game.download.game.GameDownloadInfo
import com.russoviller.cryogenic.game.download.game.models.LaunchFor
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionInfo
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedLazyColumn
import com.russoviller.cryogenic.ui.components.TextTransition
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.influencedByBackgroundColor
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.download.game.AddonList
import com.russoviller.cryogenic.ui.screens.content.download.game.AddonState
import com.russoviller.cryogenic.ui.screens.content.download.game.CurrentAddon
import com.russoviller.cryogenic.ui.screens.content.download.game.FabricList
import com.russoviller.cryogenic.ui.screens.content.download.game.ForgeList
import com.russoviller.cryogenic.ui.screens.content.download.game.LegacyFabricList
import com.russoviller.cryogenic.ui.screens.content.download.game.LoaderVerSupports
import com.russoviller.cryogenic.ui.screens.content.download.game.NeoForgeList
import com.russoviller.cryogenic.ui.screens.content.download.game.OptiFineList
import com.russoviller.cryogenic.ui.screens.content.download.game.QuiltList
import com.russoviller.cryogenic.ui.screens.content.download.game.SelectGameVersionHost
import com.russoviller.cryogenic.ui.screens.content.download.game.SelectGameVersionScreen
import com.russoviller.cryogenic.ui.screens.content.download.game.isOptiFineCompatibleWithForge
import com.russoviller.cryogenic.ui.screens.content.download.game.rememberLoaderVerSupports
import com.russoviller.cryogenic.ui.screens.content.download.game.runWithState
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.screens.navigateTo
import com.russoviller.cryogenic.ui.screens.onBack
import com.russoviller.cryogenic.ui.screens.rememberTitledNavBackStack
import com.russoviller.cryogenic.ui.screens.rememberTransitionSpec
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.GSON
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.ModifyDiffs
import com.russoviller.cryogenic.viewmodel.ModifyOperation
import com.russoviller.cryogenic.viewmodel.ModifyPayload
import com.russoviller.cryogenic.viewmodel.ModifyVersionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ModifyVersionScreen"

@Composable
fun ModifyVersionScreen(
    viewModel: ModifyVersionViewModel,
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    eventViewModel: EventViewModel,
    onModify: (ModifyPayload) -> Unit
) {
    val backStack = rememberTitledNavBackStack(NormalNavKey.Versions.ModifyVersion)

    //用户选择的目标 Minecraft 版本；null 表示未变更
    //宿主在嵌套导航期间持续组合，选择结果跨主页与选择屏存活，退出修改标签页时销毁
    var selectedGameVersion by remember { mutableStateOf<String?>(null) }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { onBack(backStack) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = rememberTransitionSpec(),
        popTransitionSpec = rememberTransitionSpec(),
        entryProvider = entryProvider {
            entry<NormalNavKey.Versions.ModifyVersion> {
                ModifyVersionContent(
                    mainScreenKey = mainScreenKey,
                    versionsScreenKey = versionsScreenKey,
                    version = version,
                    modifyViewModel = viewModel,
                    selectedGameVersion = selectedGameVersion,
                    onSelectGameVersion = {
                        backStack.navigateTo(NormalNavKey.Versions.SelectGameVersion)
                    },
                    onModify = onModify
                )
            }
            entry<NormalNavKey.Versions.SelectGameVersion> {
                SelectGameVersionScreen(
                    host = SelectGameVersionHost.VersionSettings(
                        mainScreenKey = mainScreenKey,
                        versionSettingsScreenKey = versionsScreenKey
                    ),
                    eventViewModel = eventViewModel
                ) { selected ->
                    selectedGameVersion = selected
                    onBack(backStack)
                }
            }
        }
    )
}

@Composable
private fun ModifyVersionContent(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    modifyViewModel: ModifyVersionViewModel,
    selectedGameVersion: String?,
    onSelectGameVersion: () -> Unit = {},
    onModify: (ModifyPayload) -> Unit
) {
    val versionInfo = remember(version) {
        version.getVersionInfo() ?: error("Version information is unavailable: ${version.getVersionName()}")
    }
    val originalGameVersion = versionInfo.minecraftVersion
    val targetGameVersion = selectedGameVersion ?: originalGameVersion

    val loaderSupports = rememberLoaderVerSupports(targetGameVersion)
    val installedLoaders = rememberInstalledLoaders(version, versionInfo)

    val addonsVM = viewModel(
        key = version.toString() + "_ModifyVersion_" + targetGameVersion + "_" + installedLoaders
    ) {
        ModifyAddonsViewModel(
            gameVersion = targetGameVersion,
            originalGameVersion = originalGameVersion,
            installedLoaders = installedLoaders,
            loaderSupports = loaderSupports
        )
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ModifyVersion, versionsScreenKey, false)
    ) { isVisible ->
        val scrollState = rememberLazyListState()
        val applyModify: () -> Unit = applyModify@{
            val diffs = addonsVM.currentDiffs ?: return@applyModify
            onModify(
                ModifyPayload(
                    info = GameDownloadInfo(
                        gameVersion = targetGameVersion,
                        customVersionName = version.getVersionName(),
                        optifine = addonsVM.currentAddon.optifineVersion.value,
                        forge = addonsVM.currentAddon.forgeVersion.value,
                        neoforge = addonsVM.currentAddon.neoforgeVersion.value
                            .takeIf { loaderSupports.isNeoForgeSupports },
                        fabric = addonsVM.currentAddon.fabricVersion.value
                            .takeIf { loaderSupports.isFabricSupports },
                        legacyFabric = addonsVM.currentAddon.legacyFabricVersion.value
                            .takeIf { loaderSupports.isLegacyFabricSupports },
                        quilt = addonsVM.currentAddon.quiltVersion.value
                            .takeIf { loaderSupports.isQuiltSupports }
                    ),
                    currentVersion = version,
                    diffs = diffs
                )
            )
        }
        com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutFrame(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            side = {
                ModifyLoadoutPanel(
                    versionName = version.getVersionName(),
                    originalGameVersion = originalGameVersion,
                    targetGameVersion = targetGameVersion,
                    installedLoaders = installedLoaders,
                    diffs = addonsVM.currentDiffs
                )
            },
            footer = {
                com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.versions_modify_start),
                    iconRes = R.drawable.ic_check,
                    enabled = addonsVM.currentDiffs != null && modifyViewModel.installOperation is ModifyOperation.None,
                    onClick = applyModify
                )
            }
        ) {
        AnimatedLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nonInteractiveScrollbar(
                    state = scrollState.scrollIndicatorState!!,
                    orientation = Orientation.Vertical,
                ),
            isVisible = isVisible,
            contentPadding = PaddingValues(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            state = scrollState,
        ) {
            animatedItem { ctx ->
                MCVersionRow(
                    modifier = Modifier.animated(ctx),
                    current = originalGameVersion,
                    selected = targetGameVersion,
                    onClick = onSelectGameVersion
                )
            }

            // Minecraft 版本变更，给出提示
            if (targetGameVersion != originalGameVersion) {
                animatedItem { ctx ->
                    ModifyTipItem(
                        modifier = Modifier.animated(ctx),
                        title = stringResource(R.string.versions_modify_mc_changed_title),
                        text = stringResource(R.string.versions_modify_mc_changed_tip)
                    )
                }
            }

            //未识别的已安装组件提示
            val unrecognizedInstalled = if (targetGameVersion == originalGameVersion) {
                // 切换 Minecraft 版本后视为移除，不再提示
                installedLoaders.filter { it.loader in addonsVM.unmatchedLoaders }
            } else {
                emptyList()
            }
            if (unrecognizedInstalled.isNotEmpty()) {
                animatedItem { ctx ->
                    ModifyTipItem(
                        modifier = Modifier.animated(ctx),
                        title = stringResource(R.string.versions_modify_unrecognized_title),
                        text = unrecognizedInstalled.joinToString("、") { "${it.loader.displayName} ${it.version}" }
                    )
                }
            }
    
            animatedItem { ctx ->
                OptiFineList(
                    modifier = Modifier.animated(ctx),
                    currentAddon = addonsVM.currentAddon,
                    addonList = addonsVM.addonList,
                    onValueChanged = { addonsVM.updateDiffs() },
                    onReload = { addonsVM.reloadOptiFine() }
                )
            }
    
            animatedItem { ctx ->
                ForgeList(
                    modifier = Modifier.animated(ctx),
                    currentAddon = addonsVM.currentAddon,
                    addonList = addonsVM.addonList,
                    onValueChanged = { addonsVM.updateDiffs() },
                    onReload = { addonsVM.reloadForge() }
                )
            }
    
            if (loaderSupports.isNeoForgeSupports) {
                animatedItem { ctx ->
                    NeoForgeList(
                        modifier = Modifier.animated(ctx),
                        currentAddon = addonsVM.currentAddon,
                        addonList = addonsVM.addonList,
                        onValueChanged = { addonsVM.updateDiffs() },
                        onReload = { addonsVM.reloadNeoForge() }
                    )
                }
            }
    
            if (loaderSupports.isFabricSupports) {
                animatedItem { ctx ->
                    FabricList(
                        modifier = Modifier.animated(ctx),
                        currentAddon = addonsVM.currentAddon,
                        addonList = addonsVM.addonList,
                        onValueChanged = { addonsVM.updateDiffs() },
                        onReload = { addonsVM.reloadFabric() }
                    )
                }
            }
    
            if (loaderSupports.isLegacyFabricSupports) {
                animatedItem { ctx ->
                    LegacyFabricList(
                        modifier = Modifier.animated(ctx),
                        currentAddon = addonsVM.currentAddon,
                        addonList = addonsVM.addonList,
                        onValueChanged = { addonsVM.updateDiffs() },
                        onReload = { addonsVM.reloadLegacyFabric() }
                    )
                }
            }
    
            if (loaderSupports.isQuiltSupports) {
                animatedItem { ctx ->
                    QuiltList(
                        modifier = Modifier.animated(ctx),
                        currentAddon = addonsVM.currentAddon,
                        addonList = addonsVM.addonList,
                        onValueChanged = { addonsVM.updateDiffs() },
                        onReload = { addonsVM.reloadQuilt() }
                    )
                }
            }
    
        }
        }
    }
}

/**
 * 修改版本的附加内容加载器
 */
private class ModifyAddonsViewModel(
    /** 选定的目标 Minecraft 版本 */
    private val gameVersion: String,
    /** 版本当前的 Minecraft 版本 */
    private val originalGameVersion: String,
    /** 当前已安装的加载器信息 */
    private val installedLoaders: List<VersionInfo.LoaderInfo>,
    /** 加载器支持情况，决定初始化时拉取哪些加载器的版本列表 */
    loaderSupports: LoaderVerSupports
) : ViewModel() {
    val addonList = AddonList()
    val currentAddon = CurrentAddon()

    /** 当前修改内容，无变更时为 null */
    var currentDiffs by mutableStateOf<ModifyDiffs?>(null)
        private set

    /** 无法与版本列表匹配的已安装加载器，用户未主动选择新版本时保留原样 */
    var unmatchedLoaders by mutableStateOf<Set<ModLoader>>(emptySet())
        private set

    /** 已加载版本列表、等待预选的加载器（列表加载失败也计入） */
    private val pendingPreselects = linkedMapOf<ModLoader, () -> Unit>()

    /** 已完成预选尝试的加载器 */
    private val attemptedPreselects = mutableSetOf<ModLoader>()

    /** 会加载版本列表的加载器；其余已安装加载器（如 LiteLoader）无法参与预选 */
    private val reloadableLoaders = buildSet {
        add(ModLoader.OPTIFINE)
        add(ModLoader.FORGE)
        if (loaderSupports.isNeoForgeSupports) add(ModLoader.NEOFORGE)
        if (loaderSupports.isFabricSupports) add(ModLoader.FABRIC)
        if (loaderSupports.isLegacyFabricSupports) add(ModLoader.LEGACY_FABRIC)
        if (loaderSupports.isQuiltSupports) add(ModLoader.QUILT)
    }

    private fun findInstalled(loader: ModLoader): VersionInfo.LoaderInfo? {
        return installedLoaders.firstOrNull { it.loader == loader }
    }

    private fun selectedVersionOf(loader: ModLoader): String? = when (loader) {
        ModLoader.OPTIFINE -> currentAddon.optifineVersion.value?.getAddonVersion()
        ModLoader.FORGE -> currentAddon.forgeVersion.value?.getAddonVersion()
        ModLoader.NEOFORGE -> currentAddon.neoforgeVersion.value?.getAddonVersion()
        ModLoader.FABRIC -> currentAddon.fabricVersion.value?.getAddonVersion()
        ModLoader.LEGACY_FABRIC -> currentAddon.legacyFabricVersion.value?.getAddonVersion()
        ModLoader.QUILT -> currentAddon.quiltVersion.value?.getAddonVersion()
        else -> null
    }

    /**
     * 对比当前安装信息与用户的选择，生成变更内容
     */
    fun updateDiffs() {
        val diffs = buildList {
            if (gameVersion != originalGameVersion) {
                add(ModifyDiffs.McChange(original = originalGameVersion, updateTo = gameVersion))
            }

            //已安装加载器的变更情况
            //切换了 Minecraft 版本后旧加载器几乎必然无法匹配，未选择即视为移除，不再保留原样
            val keepUnmatched = gameVersion == originalGameVersion
            installedLoaders.forEach { installed ->
                val selected = selectedVersionOf(installed.loader)
                if (installed.loader !in attemptedPreselects) return@forEach
                //未识别的加载器默认保留原样，仅在用户主动选择非等价的新版本时生成变更
                if (keepUnmatched && installed.loader in unmatchedLoaders) {
                    if (selected != null && !selectedEqualsInstalled(installed, selected)) {
                        add(
                            ModifyDiffs.LoaderChange(
                                modloader = installed.loader,
                                original = installed.version,
                                updateTo = selected
                            )
                        )
                    }
                    return@forEach
                }
                when {
                    selected == null -> add(ModifyDiffs.LoaderRemove(modloader = installed.loader))
                    !selectedEqualsInstalled(installed, selected) -> add(
                        ModifyDiffs.LoaderChange(
                            modloader = installed.loader,
                            original = installed.version,
                            updateTo = selected
                        )
                    )
                }
            }

            //新安装的加载器
            listOf(
                ModLoader.OPTIFINE, ModLoader.FORGE, ModLoader.NEOFORGE,
                ModLoader.FABRIC, ModLoader.LEGACY_FABRIC, ModLoader.QUILT
            ).forEach { loader ->
                if (installedLoaders.none { it.loader == loader }) {
                    selectedVersionOf(loader)?.let { version ->
                        add(ModifyDiffs.LoaderInstall(modloader = loader, version = version))
                    }
                }
            }
        }

        currentDiffs = diffs.takeIf { it.isNotEmpty() }?.let { ModifyDiffs(list = it) }
    }

    /**
     * 判断选中的版本与已安装版本是否等价
     * OptiFine 的已安装信息可能来自库坐标或旧版短版本串，需要与版本列表条目做等价匹配
     */
    private fun selectedEqualsInstalled(
        installed: VersionInfo.LoaderInfo,
        selected: String
    ): Boolean {
        if (selected == installed.version) return true
        if (installed.loader != ModLoader.OPTIFINE) return false
        return addonList.optifineList?.any {
            it.getAddonVersion() == selected && it.matchesInstalledVersion(installed.version)
        } == true
    }

    /**
     * 独立加载单个加载器的版本列表，加载完成后刷新初始化状态与变更内容
     */
    private fun <T> launchAddonReload(
        updateState: (AddonState) -> Unit,
        fetch: suspend () -> T?,
        afterLoaded: (T?) -> Unit
    ) {
        viewModelScope.launch {
            runWithState(updateState, fetch).also { versions ->
                afterLoaded(versions)
                updateDiffs()
            }
        }
    }

    /**
     * 登记已加载版本列表的加载器，等待按优先级执行预选
     */
    private fun enqueuePreselect(loader: ModLoader, action: () -> Unit) {
        pendingPreselects[loader] = action
        attemptedPreselects.remove(loader) //重新加载后允许再次尝试
        runPendingPreselects()
    }

    /**
     * 按主加载器优先级依次执行预选,主加载器先完成预选，其余加载器为它让位
     * 更高优先级的加载器列表尚未加载完成时，低优先级的等待，保证预选结果确定
     */
    private fun runPendingPreselects() {
        installedLoaders
            .sortedBy {
                VersionInfo.PRIMARY_PRIORITY.indexOf(it.loader).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
            }
            .forEach { installed ->
                val loader = installed.loader
                if (loader in attemptedPreselects) return@forEach
                val action = pendingPreselects[loader]
                if (action == null) {
                    if (loader in reloadableLoaders) return //更高优先级的列表仍在加载，等待
                    //无法加载版本列表的加载器，直接视为未识别
                    attemptedPreselects.add(loader)
                    unmatchedLoaders = unmatchedLoaders + loader
                    return@forEach
                }
                attemptedPreselects.add(loader)
                action()
            }
    }

    /**
     * 预选已安装的加载器版本
     * 仅在当前行未选择版本、通过校验且与已选择的其他加载器全部兼容时才填入；
     * 无法匹配时将加载器标记为未识别，修改时默认保留原样
     */
    private fun <T : AddonVersion> preselectInstalled(
        state: MutableState<T?>,
        loader: ModLoader,
        versions: List<T>?,
        installedVersion: String,
        matcher: (T, String) -> Boolean = { version, installed -> version.isVersion(installed) },
        validator: (T) -> Boolean = { true }
    ) {
        if (state.value != null) return

        val candidate = versions?.find { matcher(it, installedVersion) }
        if (
            candidate == null ||
            !validator(candidate) ||
            !currentAddon.isCompatibleWithSelection(candidate, loader, addonList)
        ) {
            unmatchedLoaders = unmatchedLoaders + loader
            return
        }

        state.value = candidate
        unmatchedLoaders = unmatchedLoaders - loader
    }

    fun reloadOptiFine() {
        launchAddonReload(
            { currentAddon.optifineState = it },
            { OptiFineVersions.fetchOptiFineList(gameVersion = gameVersion) }
        ) { versions ->
            addonList.optifineList = versions
            enqueuePreselect(ModLoader.OPTIFINE) {
                findInstalled(ModLoader.OPTIFINE)?.let { installed ->
                    preselectInstalled(
                        state = currentAddon.optifineVersion,
                        loader = ModLoader.OPTIFINE,
                        versions = versions,
                        installedVersion = installed.version,
                        matcher = { version, installed -> version.matchesInstalledVersion(installed) },
                        //与已选 Forge 成对校验兼容性
                        validator = { version ->
                            currentAddon.forgeVersion.value?.let { forge ->
                                isOptiFineCompatibleWithForge(version, forge)
                            } ?: true
                        }
                    )
                }
            }
        }
    }

    fun reloadForge() {
        launchAddonReload(
            { currentAddon.forgeState = it },
            { ForgeVersions.fetchForgeList(gameVersion) }
        ) { versions ->
            addonList.forgeList = versions
            enqueuePreselect(ModLoader.FORGE) {
                findInstalled(ModLoader.FORGE)?.let { installed ->
                    preselectInstalled(
                        state = currentAddon.forgeVersion,
                        loader = ModLoader.FORGE,
                        versions = versions,
                        installedVersion = installed.version,
                        //与已选 OptiFine 成对校验兼容性
                        validator = { version ->
                            currentAddon.optifineVersion.value?.let { optifine ->
                                isOptiFineCompatibleWithForge(optifine, version)
                            } ?: true
                        }
                    )
                }
            }
        }
    }

    fun reloadNeoForge() {
        launchAddonReload(
            { currentAddon.neoforgeState = it },
            { NeoForgeVersions.fetchNeoForgeList(gameVersion = gameVersion) }
        ) { versions ->
            addonList.neoforgeList = versions
            enqueuePreselect(ModLoader.NEOFORGE) {
                findInstalled(ModLoader.NEOFORGE)?.let { installed ->
                    preselectInstalled(currentAddon.neoforgeVersion, ModLoader.NEOFORGE, versions, installed.version)
                }
            }
        }
    }

    fun reloadFabric() {
        launchAddonReload(
            { currentAddon.fabricState = it },
            { FabricVersions.fetchFabricLoaderList(gameVersion) }
        ) { versions ->
            addonList.fabricList = versions
            enqueuePreselect(ModLoader.FABRIC) {
                findInstalled(ModLoader.FABRIC)?.let { installed ->
                    preselectInstalled(currentAddon.fabricVersion, ModLoader.FABRIC, versions, installed.version)
                }
            }
        }
    }

    fun reloadLegacyFabric() {
        launchAddonReload(
            { currentAddon.legacyFabricState = it },
            { LegacyFabricVersions.fetchFabricLoaderList(gameVersion) }
        ) { versions ->
            addonList.legacyFabricList = versions
            enqueuePreselect(ModLoader.LEGACY_FABRIC) {
                findInstalled(ModLoader.LEGACY_FABRIC)?.let { installed ->
                    preselectInstalled(currentAddon.legacyFabricVersion, ModLoader.LEGACY_FABRIC, versions, installed.version)
                }
            }
        }
    }

    fun reloadQuilt() {
        launchAddonReload(
            { currentAddon.quiltState = it },
            { QuiltVersions.fetchQuiltLoaderList(gameVersion) }
        ) { versions ->
            addonList.quiltList = versions
            enqueuePreselect(ModLoader.QUILT) {
                findInstalled(ModLoader.QUILT)?.let { installed ->
                    preselectInstalled(currentAddon.quiltVersion, ModLoader.QUILT, versions, installed.version)
                }
            }
        }
    }

    init {
        //不会被加载版本列表的已安装加载器，直接标记为未识别，避免阻塞其他加载器的预选
        installedLoaders.forEach { installed ->
            if (installed.loader !in reloadableLoaders) {
                attemptedPreselects.add(installed.loader)
                unmatchedLoaders = unmatchedLoaders + installed.loader
            }
        }
        reloadOptiFine()
        reloadForge()
        if (loaderSupports.isNeoForgeSupports) {
            reloadNeoForge()
        }
        if (loaderSupports.isFabricSupports) {
            reloadFabric()
        }
        if (loaderSupports.isLegacyFabricSupports) {
            reloadLegacyFabric()
        }
        if (loaderSupports.isQuiltSupports) {
            reloadQuilt()
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

/**
 * @return 从版本解析出当前已安装的全部加载器
 */
@Composable
private fun rememberInstalledLoaders(
    version: Version,
    versionInfo: VersionInfo
): List<VersionInfo.LoaderInfo> {
    val installedLoaders by produceState(emptyList(), version, versionInfo) {
        value = withContext(Dispatchers.IO) {
            buildList {
                runCatching {
                    val jsonFile = File(version.getVersionPath(), "${version.getVersionName()}.json")
                    GSON.fromJson(jsonFile.readText(), LaunchFor::class.java)
                }.onFailure { e ->
                    Logger.warning(TAG, "Failed to parse the launchFor info of the version.", e)
                }.getOrNull()?.infos?.forEach { info ->
                    if (info.name.equals("Minecraft", ignoreCase = true)) return@forEach
                    ModLoader.entries.find { it.displayName.equals(info.name, ignoreCase = true) }?.let { loader ->
                        add(VersionInfo.LoaderInfo(loader = loader, version = info.version))
                    }
                }
                versionInfo.loaderInfos.forEach { loaderInfo ->
                    if (none { it.loader == loaderInfo.loader }) {
                        add(loaderInfo)
                    }
                }
            }.distinctBy { it.loader }
        }
    }
    return installedLoaders
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ModifyLoadoutPanel(
    versionName: String,
    originalGameVersion: String,
    targetGameVersion: String,
    installedLoaders: List<VersionInfo.LoaderInfo>,
    diffs: ModifyDiffs?
) {
    val ice = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    val aurora = com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora
    val frost = com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost
    val mono = androidx.compose.ui.text.font.FontFamily.Monospace
    com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutKicker(text = stringResource(R.string.cryo_modify_kicker))
    com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutTitle(text = versionName, maxLines = 2, large = false)
    com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutArt(
        gameVersion = targetGameVersion,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .heightIn(min = 36.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.img_minecraft),
            contentDescription = null
        )
        Text(
            text = originalGameVersion,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = mono),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        if (targetGameVersion != originalGameVersion) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(R.drawable.ic_arrow_right_rounded),
                contentDescription = null,
                tint = aurora
            )
            Text(
                text = targetGameVersion,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = mono),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = aurora,
                maxLines = 1
            )
        }
    }
    com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutDivider()
    com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutKicker(text = stringResource(R.string.cryo_modify_installed).uppercase())
    if (installedLoaders.isEmpty()) {
        Text(
            text = stringResource(R.string.cryo_modify_vanilla),
            style = MaterialTheme.typography.labelSmall,
            color = frost.copy(alpha = 0.7f),
            maxLines = 1
        )
    } else {
        installedLoaders.forEach { loader ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ice, androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = loader.loader.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = loader.version,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = mono),
                    color = frost.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
    com.russoviller.cryogenic.ui.screens.content.download.game.LoadoutKicker(
        text = stringResource(R.string.cryo_modify_changes, diffs?.list?.size ?: 0).uppercase(),
        color = if (diffs != null) aurora else ice
    )
    if (diffs != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            diffs.list.forEach { diff ->
                val (symbol, color, text) = when (diff) {
                    is ModifyDiffs.McChange -> Triple("~", aurora, "Minecraft ${diff.original} → ${diff.updateTo}")
                    is ModifyDiffs.LoaderChange -> Triple("~", aurora, "${diff.modloader.displayName} ${diff.original} → ${diff.updateTo}")
                    is ModifyDiffs.LoaderInstall -> Triple("+", ice, "${diff.modloader.displayName} ${diff.version}")
                    is ModifyDiffs.LoaderRemove -> Triple("−", com.russoviller.cryogenic.ui.components.CryoDanger, diff.modloader.displayName)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.10f))
                        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = mono),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = color
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = mono),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MCVersionRow(
    modifier: Modifier = Modifier,
    current: String,
    selected: String,
    onClick: () -> Unit
) {
    val ice = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
    val aurora = com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora
    val shape = com.russoviller.cryogenic.ui.components.cryoChamferShape(12.dp)
    val changed = selected != current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(
                        (if (changed) aurora else ice).copy(alpha = 0.22f),
                        com.russoviller.cryogenic.ui.components.CryoGlassColors.Navy.copy(alpha = 0.8f),
                        com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.5.dp, com.russoviller.cryogenic.ui.components.cryoBorderBrush(1.2f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(com.russoviller.cryogenic.ui.components.safeRadialGradient(listOf(ice.copy(alpha = 0.3f), com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep)))
                .border(1.dp, ice.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(40.dp),
                painter = painterResource(R.drawable.img_minecraft),
                contentDescription = null
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_modify_base_kicker),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, letterSpacing = 2.sp),
                color = ice
            )
            if (!changed) {
                Text(
                    text = current,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    color = Color.White
                )
            } else {
                TextTransition(
                    from = current,
                    to = selected
                )
            }
        }
        Row(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .cryoBackground(com.russoviller.cryogenic.ui.components.cryoGradient())
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_autorenew),
                contentDescription = null,
                tint = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep
            )
            Text(
                text = stringResource(R.string.cryo_modify_change_mc),
                style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep
            )
        }
    }
}

@Composable
private fun ModifyTipItem(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val aurora = com.russoviller.cryogenic.ui.components.CryoGlassColors.Aurora
    val shape = com.russoviller.cryogenic.ui.components.cryoChamferShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(aurora.copy(alpha = 0.18f), com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.8f))))
            .border(1.dp, aurora.copy(alpha = 0.55f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.ic_warning_filled),
            contentDescription = null,
            tint = aurora
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = Color.White
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = com.russoviller.cryogenic.ui.components.CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
        }
    }
}
