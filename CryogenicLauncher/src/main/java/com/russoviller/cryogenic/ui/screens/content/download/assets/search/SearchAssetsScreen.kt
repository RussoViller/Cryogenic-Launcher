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

package com.russoviller.cryogenic.ui.screens.content.download.assets.search

import androidx.compose.ui.res.stringResource
import com.russoviller.cryogenic.ui.components.cryoFillFactor
import com.russoviller.cryogenic.ui.components.cryoTacticalBackdrop
import com.russoviller.cryogenic.ui.components.cryoTacticalPanelShape
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses
import com.russoviller.cryogenic.game.download.assets.platform.PlatformDisplayLabel
import com.russoviller.cryogenic.game.download.assets.platform.PlatformFilterCode
import com.russoviller.cryogenic.game.download.assets.platform.PlatformSearchFilter
import com.russoviller.cryogenic.game.download.assets.platform.PlatformSearchResult
import com.russoviller.cryogenic.game.download.assets.platform.navigatePage
import com.russoviller.cryogenic.game.download.assets.platform.nextPage
import com.russoviller.cryogenic.game.download.assets.platform.previousPage
import com.russoviller.cryogenic.game.download.assets.platform.searchAssets
import com.russoviller.cryogenic.game.download.assets.utils.searchMcMods
import com.russoviller.cryogenic.game.version.mod.InstalledMod
import com.russoviller.cryogenic.game.versioninfo.MinecraftVersion
import com.russoviller.cryogenic.game.versioninfo.MinecraftVersions
import com.russoviller.cryogenic.game.versioninfo.popularVersions
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedDirection
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.AssetsPage
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.ResultListLayout
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.SearchAssetsState
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.SearchFilter
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jackhuang.hmcl.addon.AddonProjectInfo

private const val TAG = "SearchAssetsScreen"

/**
 * 资源搜索屏幕的 view model
 * @param initialPlatform 初始设定的平台
 * @param platformClasses 资源搜索的类型
 */
private class SearchScreenViewModel(
    initialPlatform: Platform,
    private val platformClasses: PlatformClasses
): ViewModel() {
    var searchResult by mutableStateOf<SearchAssetsState>(SearchAssetsState.Searching)
    val pages = mutableStateListOf<AssetsPage?>()

    var searchPlatform by mutableStateOf(initialPlatform)
    var searchFilter by mutableStateOf(PlatformSearchFilter())

    private val _searchedMcMods = MutableStateFlow<List<AddonProjectInfo>>(emptyList())
    /** 搜索得到的所有 MCMOD 项目 */
    val searchedMcMods = _searchedMcMods.asStateFlow()
    private val _searchedVersions = MutableStateFlow<List<String>>(emptyList())
    /** 搜索得到的所有Minecraft版本 */
    val searchedVersions = _searchedVersions.asStateFlow()

    var currentSearchJob: Job? = null
    var currentSearchMCMODSJob: Job? = null
    var currentSearchVersionJob: Job? = null

    /**
     * 仅更新搜索名称
     */
    fun updateNameFilter(searchName: String) {
        searchFilter = searchFilter.copy(searchName = searchName)
        currentSearchMCMODSJob?.cancel()
        currentSearchMCMODSJob = viewModelScope.launch {
            val result = try {
                searchName.searchMcMods(classes = platformClasses) ?: emptyList()
            } catch (_: CancellationException) {
                emptyList()
            }.take(20) //仅展示20个搜索结果
            withContext(Dispatchers.Main) {
                _searchedMcMods.update { result }
            }
            currentSearchMCMODSJob = null
        }
    }

    /**
     * 仅更新版本名称
     */
    fun updateVersionFilter(version: String) {
        searchFilter = searchFilter.copy(gameVersion = version)
        refreshVerSuggestions(version)
    }

    private fun refreshVerSuggestions(
        version: String
    ) {
        currentSearchVersionJob?.cancel()
        currentSearchVersionJob = viewModelScope.launch {
            val allVersions = MinecraftVersions.allVersions.value
            val result: List<String> = when {
                version.isEmpty() -> popularVersions
                allVersions.isEmpty() -> popularVersions.filter { ver ->
                    ver.contains(version)
                }.take(20) //仅展示20个搜索结果
                else -> allVersions.filter {
                    it.version.id.contains(version) &&
                            //CurseForge只能使用正式版进行过滤
                            (searchPlatform != Platform.CURSEFORGE || it.type == MinecraftVersion.Type.Release)
                }.map { it.version.id }.take(20) //仅展示20个搜索结果
            }
            withContext(Dispatchers.Main) {
                _searchedVersions.update { result }
            }
            currentSearchVersionJob = null
        }
    }

    /**
     * 重置并重新搜索
     */
    fun resetSearch() {
        pages.clear()
        searchFilter = searchFilter.copy(index = 0) //重置索引到起始处
        search()
    }

    /**
     * 更新过滤器时，重置已有结果，重新触发搜索
     */
    fun researchWithFilter(filter: PlatformSearchFilter) {
        pages.clear()
        searchFilter = filter.copy(index = 0) //重置索引到起始处
        search()
    }

    private fun putResult(result: PlatformSearchResult) {
        result.getAssetsPage(platformClasses).also { page ->
            Logger.info(TAG, "Searched page info: {pageNumber: ${page.pageNumber}, pageIndex: ${page.pageIndex}, totalPage: ${page.totalPage}, isLastPage: ${page.isLastPage}}")

            val targetIndex = page.pageNumber - 1

            if (pages.size > targetIndex) {
                pages[targetIndex] = page //替换已有页
            } else {
                while (pages.size < targetIndex) {
                    pages += null
                }
                pages += page
            }

            searchResult = SearchAssetsState.Success(page)
        }
    }

    fun search() {
        currentSearchJob?.cancel() //取消上一个搜索

        currentSearchJob = viewModelScope.launch {
            searchResult = SearchAssetsState.Searching
            searchAssets(
                searchPlatform = searchPlatform,
                searchFilter = searchFilter,
                platformClasses = platformClasses,
                onSuccess = { result ->
                    putResult(result)
                },
                onError = {
                    searchResult = it
                }
            )
        }
    }

    init {
        //初始化后，执行一次搜索
        search()
        refreshVerSuggestions("")
        viewModelScope.launch {
            runCatching {
                MinecraftVersions.refreshVersions(force = false)
            }.onFailure {
                Logger.warning(TAG, "Failed to refresh Minecraft versions")
            }
        }
    }

    override fun onCleared() {
        currentSearchJob?.cancel()
        currentSearchMCMODSJob?.cancel()
    }
}

@Composable
private fun rememberSearchAssetsViewModel(
    navKey: TitledNavKey,
    initialPlatform: Platform,
    platformClasses: PlatformClasses
): SearchScreenViewModel {
    val screenKey = navKey.toString()
    return viewModel(
        key = "${screenKey}_search"
    ) {
        SearchScreenViewModel(initialPlatform, platformClasses)
    }
}

/**
 * @param parentScreenKey 父屏幕Key
 * @param parentCurrentKey 父屏幕当前Key
 * @param screenKey 屏幕的Key
 * @param currentKey 当前的Key
 * @param platformClasses 搜索资源的分类
 * @param initialPlatform 初始搜索平台
 * @param onPlatformChange 搜索平台变更
 * @param enablePlatform 是否允许更改平台
 * @param getCategories 根据平台获取可用的资源类别过滤器
 * @param enableModLoader 是否允许更改模组加载器
 * @param getModloaders 根据平台获取可用的模组加载器过滤器
 * @param mapCategories 通过平台获取类别本地化信息
 * @param swapToDownload 跳转到下载详情页
 * @param installedInfo 查询项目本地是否已安装，键为平台与平台项目ID
 * @param extraFilter 额外的过滤器UI
 */
@Composable
fun SearchAssetsScreen(
    mainScreenKey: TitledNavKey?,
    parentScreenKey: TitledNavKey,
    parentCurrentKey: TitledNavKey?,
    screenKey: TitledNavKey,
    currentKey: TitledNavKey?,
    platformClasses: PlatformClasses,
    initialPlatform: Platform,
    onPlatformChange: (Platform) -> Unit = {},
    enablePlatform: Boolean = true,
    getCategories: (Platform) -> List<PlatformFilterCode>,
    enableModLoader: Boolean = false,
    getModloaders: (Platform) -> List<PlatformDisplayLabel> = { emptyList() },
    mapCategories: (Platform, String) -> PlatformFilterCode?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> },
    installedInfo: ((Platform, projectId: String) -> InstalledMod?)? = null,
    extraFilter: (LazyListScope.() -> Unit)? = null
) {
    val viewModel: SearchScreenViewModel = rememberSearchAssetsViewModel(
        navKey = screenKey,
        initialPlatform = initialPlatform,
        platformClasses = platformClasses
    )

    //跟随平台自动变更的内容
    val categories = remember(viewModel.searchPlatform) {
        getCategories(viewModel.searchPlatform)
    }
    val modloaders = remember(viewModel.searchPlatform) {
        getModloaders(viewModel.searchPlatform)
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.Download::class.java, mainScreenKey)
        ),
        Triple(parentScreenKey, parentCurrentKey, false),
        Triple(screenKey, currentKey, false)
    ) { isVisible ->
        Row {
            val filterCtx = rememberAnimatedContext(isVisible = isVisible)
            val searchedMcMods by viewModel.searchedMcMods.collectAsStateWithLifecycle()
            val searchedVersions by viewModel.searchedVersions.collectAsStateWithLifecycle()
            val density = androidx.compose.ui.platform.LocalDensity.current
            val panelShape = remember(density) {
                with(density) { cryoTacticalPanelShape(true, 20.dp.toPx(), 10.dp.toPx()) }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(3f)
                    .animated(filterCtx, from = AnimatedDirection.Left)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                    .clip(panelShape)
                    .cryoTacticalBackdrop(panelShape, true, 0.5f + 0.5f * cryoFillFactor().coerceAtMost(1f), corner = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = CryoGlassColors.Ice
                    )
                    Text(
                        text = stringResource(R.string.cryo_deco_scanner),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = CryoGlassColors.Ice
                    )
                }
            SearchFilter(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 10.dp, top = 10.dp, bottom = 14.dp, end = 26.dp),
                enablePlatform = enablePlatform,
                searchPlatform = viewModel.searchPlatform,
                onPlatformChange = {
                    viewModel.searchPlatform = it
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(categories = emptyList(), modloader = null)
                    )
                    onPlatformChange(it)
                },
                searchName = viewModel.searchFilter.searchName,
                onSearchNameChange = {
                    viewModel.updateNameFilter(it)
                },
                onSearch = {
                    viewModel.resetSearch()
                },
                searchedMcMods = searchedMcMods,
                searchedVersions = searchedVersions,
                gameVersion = viewModel.searchFilter.gameVersion,
                onGameVersionChange = {
                    viewModel.updateVersionFilter(it)
                },
                sortField = viewModel.searchFilter.sortField,
                onSortFieldChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(sortField = it)
                    )
                },
                allCategories = categories,
                categories = viewModel.searchFilter.categories,
                onCategoryChanged = { categories ->
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(categories = categories)
                    )
                },
                enableModLoader = enableModLoader,
                modloaders = modloaders,
                modloader = viewModel.searchFilter.modloader,
                onModLoaderChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(modloader = it)
                    )
                },
                extraFilter = extraFilter
            )
            }

            val resultCtx = rememberAnimatedContext(isVisible = isVisible)
            ResultListLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(7f)
                    .animated(resultCtx, from = AnimatedDirection.Right),
                classes = platformClasses,
                searchState = viewModel.searchResult,
                onReload = {
                    viewModel.search()
                },
                swapToDownload = swapToDownload,
                installedInfo = installedInfo,
                onPreviousPage = { pageNumber ->
                    previousPage(
                        pageNumber = pageNumber,
                        pages = viewModel.pages,
                        index = viewModel.searchFilter.index,
                        limit = viewModel.searchFilter.limit,
                        onSuccess = { previousPage ->
                            viewModel.searchResult = SearchAssetsState.Success(previousPage)
                        },
                        onSearch = { newIndex ->
                            viewModel.searchFilter = viewModel.searchFilter.copy(index = newIndex)
                            viewModel.search() //搜索上一页
                        }
                    )
                },
                onNextPage = { pageNumber, isLastPage ->
                    nextPage(
                        pageNumber = pageNumber,
                        isLastPage = isLastPage,
                        pages = viewModel.pages,
                        index = viewModel.searchFilter.index,
                        limit = viewModel.searchFilter.limit,
                        onSuccess = { nextPage ->
                            viewModel.searchResult = SearchAssetsState.Success(nextPage)
                        },
                        onSearch = { newIndex ->
                            viewModel.searchFilter = viewModel.searchFilter.copy(index = newIndex)
                            viewModel.search() //搜索下一页
                        }
                    )
                },
                onNavigatePage = { pageNumber ->
                    navigatePage(
                        pageNumber = pageNumber,
                        pages = viewModel.pages,
                        limit = viewModel.searchFilter.limit,
                        onSuccess = { nextPage ->
                            viewModel.searchResult = SearchAssetsState.Success(nextPage)
                        },
                        onSearch = { newIndex ->
                            viewModel.searchFilter = viewModel.searchFilter.copy(index = newIndex)
                            viewModel.search() //搜索目标页
                        }
                    )
                }
            )

        }
    }
}
