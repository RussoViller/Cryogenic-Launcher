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

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoShardShape
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import android.os.Environment
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionComparator
import com.russoviller.cryogenic.game.version.installed.VersionType
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.game.version.installed.cleanup.GameAssetCleaner
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.activities.MainActivity
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.EdgeDirection
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.ScalingActionButton
import com.russoviller.cryogenic.ui.components.ScalingLabel
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.components.topBarWheelFallback
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.content.elements.CleanupOperation
import com.russoviller.cryogenic.ui.screens.content.elements.GamePathItemLayout
import com.russoviller.cryogenic.ui.screens.content.elements.GamePathOperation
import com.russoviller.cryogenic.ui.screens.content.elements.VersionCategory
import com.russoviller.cryogenic.ui.screens.content.elements.VersionCategoryItem
import com.russoviller.cryogenic.ui.screens.content.elements.VersionItemLayout
import com.russoviller.cryogenic.ui.screens.content.elements.VersionsOperation
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.canHandlePermission
import com.russoviller.cryogenic.utils.checkStoragePermissions
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.ScreenBackStackViewModel
import com.russoviller.cryogenic.viewmodel.backgroundVisible
import com.russoviller.cryogenic.viewmodel.sendKeepScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

private class VersionsScreenViewModel : ViewModel() {
    /** 版本类别分类 */
    var versionCategory by mutableStateOf(VersionCategory.ALL)
        private set
    /** 重排序刷新key */
    var resortKey by mutableIntStateOf(0)
        private set

    /** 游戏路径相关操作 */
    var gamePathOperation by mutableStateOf<GamePathOperation>(GamePathOperation.None)

    /** 全部版本的数量 */
    var allVersionsCount by mutableIntStateOf(0)
    /** 原版版本数量 */
    var vanillaVersionsCount by mutableIntStateOf(0)
    /** 模组加载器版本数量 */
    var modloaderVersionsCount by mutableIntStateOf(0)

    fun startRefreshVersions() {
        if (!VersionsManager.isRefreshing.value) {
            VersionsManager.refresh("VersionsScreenViewModel.startRefreshVersions")
        }
    }

    private var currentJob: Job? = null
    private var mutex: Mutex = Mutex()

    /**
     * 变更当前版本列表的过滤类型
     */
    fun changeCategory(category: VersionCategory) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            mutex.withLock {
                this@VersionsScreenViewModel.versionCategory = category
            }
        }
    }

    /**
     * 重新排序当前版本列表
     */
    fun resortVersions() {
        resortKey++
    }

    /** 清理游戏文件操作 */
    var cleanupOperation by mutableStateOf<CleanupOperation>(CleanupOperation.None)

    /** 游戏无用资源清理者 */
    var cleaner by mutableStateOf<GameAssetCleaner?>(null)

    fun cleanUnusedFiles(
        onStart: () -> Unit = {},
        onStop: () -> Unit = {}
    ) {
        cleaner = GameAssetCleaner(
            scope = viewModelScope
        ).also {
            cleanupOperation = CleanupOperation.Clean
            it.start(
                onEnd = { count, size ->
                    cleaner = null
                    cleanupOperation = CleanupOperation.Success(count, size)
                    onStop()
                },
                onThrowable = { th ->
                    cleaner = null
                    cleanupOperation = CleanupOperation.Error(th)
                    onStop()
                }
            )
        }
        onStart()
    }

    fun cancelCleaner() {
        cleaner?.cancel()
        cleaner = null
        cleanupOperation = CleanupOperation.None
    }

    override fun onCleared() {
        cancelCleaner()
        currentJob?.cancel()
    }
}

@Composable
private fun rememberVersionViewModel() : VersionsScreenViewModel {
    return viewModel(
        key = NormalNavKey.VersionsManager.toString()
    ) {
        VersionsScreenViewModel()
    }
}

@Composable
private fun rememberVersions(
    versions: StateFlow<List<Version>>,
    viewModel: VersionsScreenViewModel,
): State<List<Version>> {
    val vers by versions.collectAsStateWithLifecycle()
    val category = viewModel.versionCategory
    val resortKey = viewModel.resortKey

    return remember(vers, category, resortKey) {
        derivedStateOf {
            viewModel.allVersionsCount = vers.size

            val vanillaVersions = vers
                .filter { ver -> ver.versionType == VersionType.VANILLA }
                .also { viewModel.vanillaVersionsCount = it.size }
            val modloaderVersions = vers
                .filter { ver -> ver.versionType == VersionType.MODLOADERS }
                .also { viewModel.modloaderVersionsCount = it.size }

            when (category) {
                VersionCategory.ALL -> vers
                VersionCategory.VANILLA -> vanillaVersions
                VersionCategory.MODLOADER -> modloaderVersions
            }.sortedWith(VersionComparator)
        }
    }
}

@Composable
fun VersionsManageScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    navigateToExport: (Version) -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: com.russoviller.cryogenic.viewmodel.ModpackImportViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val viewModel = rememberVersionViewModel()
    val importContext = androidx.compose.ui.platform.LocalContext.current
    var showModpackWarning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val modpackPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            modpackImportViewModel.import(
                context = importContext,
                uri = it,
                onStart = { eventViewModel.sendKeepScreen(true) },
                onStop = { eventViewModel.sendKeepScreen(false) }
            )
        }
    }
    if (showModpackWarning) {
        com.russoviller.cryogenic.ui.screens.content.download.assets.search.ModpackImportWarningDialog(
            onDismiss = { showModpackWarning = false },
            onConfirm = { modpackPicker.launch("*/*") }
        )
    }

    val versions by rememberVersions(VersionsManager.versions, viewModel)
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    GamePathOperation(
        gamePathOperation = viewModel.gamePathOperation,
        changeState = { viewModel.gamePathOperation = it },
        submitError = submitError
    )

    BaseScreen(
        screenKey = NormalNavKey.VersionsManager,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row {
            LeftMenu(
                isVisible = isVisible,
                isRefreshing = isRefreshing,
                swapToFileSelector = { path ->
                    backScreenViewModel.mainScreen.backStack.navigateToFileSelector(
                        startPath = path,
                        selectFile = false,
                        saveKey = NormalNavKey.VersionsManager
                    ) { path ->
                        viewModel.gamePathOperation = GamePathOperation.AddNewPath(path)
                    }
                },
                onImportModpack = {
                    if (modpackImportViewModel.importOperation == com.russoviller.cryogenic.viewmodel.ModpackImportOperation.None) {
                        showModpackWarning = true
                    }
                },
                onCleanupGameFiles = {
                    if (viewModel.cleanupOperation == CleanupOperation.None) {
                        viewModel.cleanupOperation = CleanupOperation.Tip
                    }
                },
                changePathOperation = {
                    viewModel.gamePathOperation = it
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.5f)
            )

            VersionsLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(7.5f),
                isVisible = isVisible,
                isRefreshing = isRefreshing,
                versions = versions,
                currentVersion = currentVersion,
                versionCategory = viewModel.versionCategory,
                onCategoryChange = { viewModel.changeCategory(it) },
                allVersionsCount = viewModel.allVersionsCount,
                vanillaVersionsCount = viewModel.vanillaVersionsCount,
                modloaderVersionsCount = viewModel.modloaderVersionsCount,
                navigateToVersions = navigateToVersions,
                navigateToExport = navigateToExport,
                submitError = submitError,
                onRefresh = {
                    viewModel.startRefreshVersions()
                },
                onVersionPinned = {
                    viewModel.resortVersions()
                },
                onInstall = {
                    backScreenViewModel.navigateToDownload()
                }
            )

            CleanupOperation(
                operation = viewModel.cleanupOperation,
                changeOperation = { viewModel.cleanupOperation = it },
                cleaner = viewModel.cleaner,
                onClean = {
                    viewModel.cleanUnusedFiles(
                        onStart = {
                            eventViewModel.sendKeepScreen(true)
                        },
                        onStop = {
                            eventViewModel.sendKeepScreen(false)
                        }
                    )
                },
                onCancel = {
                    viewModel.cancelCleaner()
                    eventViewModel.sendKeepScreen(false)
                },
                submitError = submitError
            )
        }
    }
}

@Composable
private fun LeftMenu(
    isVisible: Boolean,
    isRefreshing: Boolean,
    swapToFileSelector: (path: String) -> Unit,
    onImportModpack: () -> Unit,
    onCleanupGameFiles: () -> Unit,
    changePathOperation: (GamePathOperation) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    Column(
        modifier = modifier
            .animated(ctx, from = AnimatedDirection.Left)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp),
    ) {
        val gamePaths by GamePathManager.gamePathData.collectAsStateWithLifecycle()
        val currentPath by GamePathManager.currentPath.collectAsStateWithLifecycle()
        val context = LocalContext.current

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
            Text(
                text = stringResource(R.string.cryo_deco_hangar),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                color = CryoGlassColors.Ice
            )
            Text(
                text = stringResource(R.string.cryo_versions_paths_count, gamePaths.size),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(all = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(gamePaths, key = { it.id }) { pathItem ->
                GamePathItemLayout(
                    item = pathItem,
                    selected = currentPath == pathItem.path,
                    enabled = canHandlePermission,
                    onClick = {
                        if (!isRefreshing) { //避免频繁刷新，防止currentGameInfo意外重置
                            if (pathItem.id == GamePathManager.DEFAULT_ID) {
                                GamePathManager.saveDefaultPath()
                            } else {
                                (context as? MainActivity)?.let { activity ->
                                    checkStoragePermissions(
                                        activity = activity,
                                        message = activity.getString(R.string.versions_manage_game_storage_permissions),
                                        messageSdk30 = activity.getString(R.string.versions_manage_game_storage_permissions_sdk30),
                                        hasPermission = {
                                            GamePathManager.saveCurrentPath(pathItem.id)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    onDelete = {
                        changePathOperation(GamePathOperation.DeletePath(pathItem))
                    },
                    onRename = {
                        changePathOperation(GamePathOperation.RenamePath(pathItem))
                    }
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CryoButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onImportModpack,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_package_2_outlined),
                    contentDescription = null
                )
                MarqueeText(text = stringResource(R.string.import_modpack))
            }
            CryoTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    (context as? MainActivity)?.let { activity ->
                        checkStoragePermissions(
                            activity = activity,
                            message = activity.getString(R.string.versions_manage_game_path_storage_permissions),
                            messageSdk30 = activity.getString(R.string.versions_manage_game_path_storage_permissions_sdk30),
                            hasPermission = {
                                swapToFileSelector(Environment.getExternalStorageDirectory().absolutePath)
                            }
                        )
                    }
                },
                enabled = canHandlePermission,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_create_new_folder_filled),
                    contentDescription = null
                )
                MarqueeText(text = stringResource(R.string.cryo_versions_add_path))
            }
            CryoTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCleanupGameFiles,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_auto_delete_outlined),
                    contentDescription = null
                )
                MarqueeText(text = stringResource(R.string.cryo_versions_cleanup))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionsLayout(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isRefreshing: Boolean,
    versions: List<Version>,
    currentVersion: Version?,
    versionCategory: VersionCategory,
    onCategoryChange: (VersionCategory) -> Unit,
    allVersionsCount: Int,
    vanillaVersionsCount: Int,
    modloaderVersionsCount: Int,
    navigateToVersions: (Version) -> Unit,
    navigateToExport: (Version) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    onRefresh: () -> Unit,
    onVersionPinned: () -> Unit,
    onInstall: () -> Unit,
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    PullToRefreshBox(
        modifier = modifier.animated(ctx),
        isRefreshing = false,
        enabled = !isRefreshing,
        onRefresh = onRefresh
    ) {
        if (isRefreshing) { //版本正在刷新中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            var versionsOperation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }
            VersionsOperation(
                versionsOperation = versionsOperation,
                updateVersionsOperation = { versionsOperation = it },
                submitError = submitError
            )

            // 操作栏滚动吸附
            val density = LocalDensity.current
            val topAppBarState = rememberTopAppBarState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
            val headerTopPaddingPx = with(density) { 12.dp.toPx() }
            var headerHeightPx by remember { mutableIntStateOf(0) }

            // 操作栏阴影跟随滚动线性过渡
            val listState = rememberLazyListState()
            val listScrolledFraction = remember(listState, headerTopPaddingPx) {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) 1f
                    else {
                        val rampPx = headerTopPaddingPx + (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0)
                        (listState.firstVisibleItemScrollOffset / rampPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    }
                }
            }
            val barShownFraction = remember(topAppBarState) {
                derivedStateOf { 1f - topAppBarState.collapsedFraction }
            }
            //背景可见时不使用阴影，因为卡片会半透明化
            val withShadow = !backgroundVisible()

            //悬浮操作栏的阴影在图层块内读取滚动比例，滚动时只更新图层不重组
            val barShape = MaterialTheme.shapes.large
            val barShadowModifier = Modifier.graphicsLayer {
                if (withShadow) {
                    shadowElevation = 5.dp.toPx() * listScrolledFraction.value * barShownFraction.value
                }
                shape = barShape
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (versions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .topBarWheelFallback(topAppBarState)
                            .nonInteractiveScrollbar(
                                state = listState.scrollIndicatorState!!,
                                orientation = Orientation.Vertical,
                            )
                            .clipToBounds()
                            .topFade {
                                (headerHeightPx + headerTopPaddingPx + 80f) *
                                        listScrolledFraction.value *
                                        barShownFraction.value
                            },
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = with(density) {
                                (headerHeightPx + topAppBarState.heightOffset).coerceAtLeast(0f).toDp()
                            },
                            end = 12.dp,
                            bottom = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        state = listState,
                    ) {
                        items(versions, key = { it.toString() }) { version ->
                            VersionItemLayout(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                                version = version,
                                selected = version == currentVersion,
                                submitError = submitError,
                                onSelected = {
                                    if (version == currentVersion) return@VersionItemLayout
                                    if (!VersionsManager.saveVersion(version)) {
                                        //不允许选择无效版本
                                        versionsOperation = VersionsOperation.InvalidDelete(version)
                                    }
                                },
                                onSettingsClick = {
                                    navigateToVersions(version)
                                },
                                onRenameClick = { versionsOperation = VersionsOperation.Rename(version) },
                                onCopyClick = { versionsOperation = VersionsOperation.Copy(version) },
                                onExportClick = { navigateToExport(version) },
                                onDeleteClick = { versionsOperation = VersionsOperation.Delete(version) },
                                onPinned = onVersionPinned
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScalingLabel(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.versions_manage_no_versions)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .onSizeChanged {
                            headerHeightPx = it.height
                            topAppBarState.heightOffsetLimit = -it.height.toFloat()
                        }
                        .offset { IntOffset(x = 0, y = topAppBarState.heightOffset.roundToInt()) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 12.dp)
                            .cryoGlass(shape = RoundedCornerShape(18.dp), cornerRadius = 18.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VERSIONS // DECK",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                                color = CryoGlassColors.Ice
                            )
                            Text(
                                text = stringResource(R.string.cryo_versions_installed, allVersionsCount),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                                ),
                                maxLines = 1
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(
                                Triple(VersionCategory.ALL, allVersionsCount, R.drawable.ic_package_2_outlined),
                                Triple(VersionCategory.VANILLA, vanillaVersionsCount, R.drawable.ic_videogame_asset_outlined),
                                Triple(VersionCategory.MODLOADER, modloaderVersionsCount, R.drawable.ic_extension_outlined)
                            ).forEach { (category, count, icon) ->
                                val selected = versionCategory == category
                                Row(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(CryoShardShape)
                                        .cryoBackground(
                                            if (selected) cryoGradient()
                                            else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.08f), CryoGlassColors.Ice.copy(alpha = 0.03f)))
                                        )
                                        .border(1.dp, cryoBorderBrush(), CryoShardShape)
                                        .clickable { onCategoryChange(category) }
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        modifier = Modifier.size(16.dp),
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        tint = if (selected) CryoGlassColors.Deep else CryoGlassColors.Ice
                                    )
                                    Text(
                                        text = stringResource(category.textRes) + " · " + count,
                                        style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) CryoGlassColors.Deep else Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CryoGlassColors.Ice.copy(alpha = 0.10f))
                                .border(1.dp, cryoBorderBrush(), CircleShape)
                                .clickable(onClick = onRefresh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = stringResource(R.string.generic_refresh),
                                tint = CryoGlassColors.Ice
                            )
                        }
                        CryoButton(
                            onClick = onInstall,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = null
                            )
                            Text(text = stringResource(R.string.cryo_versions_install), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 在组件顶部绘制指定像素高度的线性渐隐；高度在绘制阶段读取，随滚动只触发重绘
 */
private fun Modifier.topFade(heightPx: () -> Float): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val height = heightPx()
        if (height <= 0f) return@drawWithContent
        inset(
            left = 0f,
            top = 0f,
            right = 0f,
            bottom = (size.height - height).coerceAtLeast(0f)
        ) {
            drawRect(
                brush = TopFadeBrush,
                blendMode = BlendMode.DstOut
            )
        }
    }

/** 顶部渐隐使用的固定渐变 */
private val TopFadeBrush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent)
