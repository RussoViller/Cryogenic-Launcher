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

package com.russoviller.cryogenic.ui.screens.content.download.assets.elements

import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.CryoButton
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.DependencyRequest
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses
import com.russoviller.cryogenic.game.download.assets.platform.PlatformDependencyType
import com.russoviller.cryogenic.game.download.assets.platform.PlatformDisplayLabel
import com.russoviller.cryogenic.game.download.assets.platform.PlatformProject
import com.russoviller.cryogenic.game.download.assets.platform.PlatformVersion
import com.russoviller.cryogenic.game.download.assets.platform.cacheKey
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.screens.content.elements.CommonVersionInfoLayout
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.itemColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.ui.theme.onItemColor
import com.russoviller.cryogenic.utils.formatNumberByLocale

/**
 * 下载对话框中的依赖项
 * @param dependency 依赖信息
 * @param project 依赖项目信息，为null代表信息获取失败
 * @param notFound 依赖项目在平台上不存在
 */
class DependencyEntry(
    val dependency: PlatformVersion.PlatformDependency,
    val project: PlatformProject?,
    val notFound: Boolean = false
)

/**
 * 操作状态：下载单个资源文件
 */
sealed interface DownloadSingleOperation {
    data object None : DownloadSingleOperation
    /** 警告用户正在使用移动网络 */
    data class WarningForMobileData(
        val classes: PlatformClasses,
        val version: PlatformVersion,
        val dependencyEntries: List<DependencyEntry>
    ) : DownloadSingleOperation
    /** 选择版本 */
    data class SelectVersion(
        val classes: PlatformClasses,
        val version: PlatformVersion,
        val dependencyEntries: List<DependencyEntry>
    ) : DownloadSingleOperation
    /** 安装 */
    data class Install(
        val classes: PlatformClasses,
        val version: PlatformVersion,
        val versions: List<Version>,
        val dependencies: List<DependencyRequest>
    ) : DownloadSingleOperation
}

@Composable
fun DownloadSingleOperation(
    operation: DownloadSingleOperation,
    changeOperation: (DownloadSingleOperation) -> Unit,
    doInstall: (PlatformClasses, PlatformVersion, List<Version>, List<DependencyRequest>) -> Unit,
    installedProjects: Map<Platform, Set<String>> = emptyMap(),
    onDependencyClicked: (Platform, String, PlatformClasses) -> Unit = { _, _, _ -> }
) {
    when (operation) {
        DownloadSingleOperation.None -> {}
        is DownloadSingleOperation.WarningForMobileData -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.download_install_warning_mobile_data),
                confirmText = stringResource(R.string.generic_anyway),
                onDismiss = {
                    changeOperation(DownloadSingleOperation.None)
                },
                onConfirm = {
                    //用户坚持使用移动网络
                    changeOperation(
                        DownloadSingleOperation.SelectVersion(
                            classes = operation.classes,
                            version = operation.version,
                            dependencyEntries = operation.dependencyEntries
                        )
                    )
                }
            )
        }
        is DownloadSingleOperation.SelectVersion -> {
            val dependencyEntries = operation.dependencyEntries
            val classes = operation.classes

            DownloadDialog(
                dependencyEntries = dependencyEntries,
                classes = classes,
                installedProjects = installedProjects,
                onDismiss = {
                    changeOperation(DownloadSingleOperation.None)
                },
                onInstall = { versions, dependencies ->
                    changeOperation(
                        DownloadSingleOperation.Install(classes, operation.version, versions, dependencies)
                    )
                },
                onDependencyClicked = { platform, projectId, dependencyClasses ->
                    changeOperation(DownloadSingleOperation.None)
                    onDependencyClicked(platform, projectId, dependencyClasses)
                }
            )
        }
        is DownloadSingleOperation.Install -> {
            LaunchedEffect(Unit) {
                doInstall(operation.classes, operation.version, operation.versions, operation.dependencies)
                changeOperation(DownloadSingleOperation.None)
            }
        }
    }
}

@Composable
private fun rememberValidVersions(): State<List<Version>> {
    val vers by VersionsManager.versions.collectAsStateWithLifecycle()
    return remember(vers) {
        derivedStateOf {
            vers.filter { it.isValid() }
        }
    }
}

@Composable
private fun DownloadDialog(
    dependencyEntries: List<DependencyEntry>,
    classes: PlatformClasses,
    installedProjects: Map<Platform, Set<String>>,
    onDismiss: () -> Unit,
    onInstall: (List<Version>, List<DependencyRequest>) -> Unit,
    onDependencyClicked: (Platform, String, PlatformClasses) -> Unit
) {
    val versions by rememberValidVersions()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val version0 = version

    if (version0 == null || versions.isEmpty()) {
        SimpleAlertDialog(
            title = stringResource(R.string.generic_warning),
            text = stringResource(R.string.download_assets_no_installed_versions),
            confirmText = stringResource(R.string.generic_got_it),
            onDismiss = onDismiss
        )
    } else {
        //当前选择的版本，将会把资源安装到该版本
        val selectedVersions = remember { mutableStateListOf(version0) }

        //拆分依赖项目、可选项目
        val dependencies = remember(dependencyEntries) {
            dependencyEntries.filter { it.dependency.type == PlatformDependencyType.REQUIRED }
        }
        val optionals = remember(dependencyEntries) {
            dependencyEntries.filter { it.dependency.type == PlatformDependencyType.OPTIONAL }
        }
        val hasDeps = dependencies.isNotEmpty() || optionals.isNotEmpty()

        //用户是否手动修改过依赖的勾选状态
        var userChangedDependencies by remember(dependencyEntries) { mutableStateOf(false) }

        val selectedDependencyKeys = remember(dependencyEntries) { mutableStateListOf<String>() }
        val onDependencySelectedChange: (String, Boolean) -> Unit = { key, checked ->
            userChangedDependencies = true
            if (checked) {
                if (!selectedDependencyKeys.contains(key)) selectedDependencyKeys.add(key)
            } else {
                selectedDependencyKeys.remove(key)
            }
        }

        // 默认勾选必装依赖，本地已安装的模组依赖默认不勾选
        // 已安装信息可能晚于对话框到达，用户未手动修改过时重新套用默认状态
        LaunchedEffect(dependencyEntries, installedProjects, userChangedDependencies) {
            if (userChangedDependencies) return@LaunchedEffect
            selectedDependencyKeys.clear()
            selectedDependencyKeys.addAll(
                dependencies
                    .filter { it.isInstallable() && !it.isModInstalled(installedProjects, classes) }
                    .map { it.dependency.cacheKey() }
            )
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction = if (hasDeps) 0.8f else 0.5f
                    )
                    .heightIn(max = rememberDialogMaxHeight())
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .padding(all = 6.dp)
                            .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                            .wrapContentHeight()
                            .cryoGlass(shape = RoundedCornerShape(28.dp), cornerRadius = 28.dp, window = true)
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        InstallDialogHeader(
                            selectedVersions = selectedVersions.size,
                            selectedDependencies = selectedDependencyKeys.size
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasDeps) {
                                val listState = rememberLazyListState()

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .cryoPanel()
                                        .fadeEdge(state = listState)
                                        .nonInteractiveScrollbar(
                                            state = listState.scrollIndicatorState!!,
                                            orientation = Orientation.Vertical,
                                        ),
                                    contentPadding = PaddingValues(all = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    state = listState
                                ) {
                                    dependencies.takeIf { it.isNotEmpty() }?.let { dependencies ->
                                        dependencyLayout(
                                            list = dependencies,
                                            titleRes = R.string.download_assets_dependency_projects,
                                            defaultClasses = classes,
                                            selectedKeys = selectedDependencyKeys,
                                            onSelectedChange = onDependencySelectedChange,
                                            installedProjects = installedProjects,
                                            onDependencyClicked = onDependencyClicked
                                        )
                                    }
                                    optionals.takeIf { it.isNotEmpty() }?.let { optionals ->
                                        dependencyLayout(
                                            list = optionals,
                                            titleRes = R.string.download_assets_optional_projects,
                                            defaultClasses = classes,
                                            selectedKeys = selectedDependencyKeys,
                                            onSelectedChange = onDependencySelectedChange,
                                            installedProjects = installedProjects,
                                            onDependencyClicked = onDependencyClicked
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .cryoPanel()
                                    .padding(top = 10.dp, start = 6.dp, end = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SectionTitle(
                                        modifier = Modifier.weight(1f),
                                        text = stringResource(R.string.download_assets_install_assets_for_versions)
                                    )
                                    SmallChip(
                                        text = stringResource(R.string.cryo_install_all),
                                        active = selectedVersions.size == versions.size
                                    ) {
                                        versions.forEach { if (it !in selectedVersions) selectedVersions.add(it) }
                                    }
                                    SmallChip(
                                        text = stringResource(R.string.cryo_install_none),
                                        active = selectedVersions.isEmpty()
                                    ) {
                                        selectedVersions.clear()
                                    }
                                }

                                val listState = rememberLazyListState()

                                LaunchedEffect(Unit) {
                                    val target = selectedVersions.firstOrNull() ?: return@LaunchedEffect
                                    runCatching {
                                        val index = versions.indexOf(target)
                                        if (index >= 0) {
                                            listState.scrollToItem(index)
                                        }
                                    }
                                }

                                //选择游戏版本
                                ChoseGameVersionLayout(
                                    modifier = Modifier.fadeEdge(state = listState),
                                    versions = versions,
                                    selectedVersions = selectedVersions,
                                    onVersionSelected = { selectedVersions.add(it) },
                                    onVersionUnSelected = { selectedVersions.remove(it) },
                                    listState = listState
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CryoTonalButton(
                                modifier = Modifier.weight(0.5f),
                                onClick = onDismiss
                            ) {
                                MarqueeText(text = stringResource(R.string.generic_cancel))
                            }
                            CryoButton(
                                modifier = Modifier.weight(0.5f),
                                enabled = selectedVersions.isNotEmpty(),
                                onClick = {
                                    if (selectedVersions.isNotEmpty()) {
                                        onInstall(
                                            selectedVersions,
                                            dependencyEntries.mapNotNull { entry ->
                                                val project = entry.project ?: return@mapNotNull null
                                                if (entry.dependency.cacheKey() !in selectedDependencyKeys) {
                                                    return@mapNotNull null
                                                }
                                                DependencyRequest(
                                                    platform = entry.dependency.platform,
                                                    projectId = project.platformId(),
                                                    versionId = entry.dependency.versionId,
                                                    classes = project.platformClasses(classes),
                                                    projectTitle = project.platformTitle()
                                                )
                                            }
                                        )
                                    }
                                }
                            ) {
                                MarqueeText(
                                    text = stringResource(R.string.download_install) +
                                            if (selectedVersions.size > 1) " (${selectedVersions.size})" else ""
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
private fun ChoseGameVersionLayout(
    modifier: Modifier = Modifier,
    versions: List<Version>,
    selectedVersions: List<Version>,
    onVersionSelected: (Version) -> Unit,
    onVersionUnSelected: (Version) -> Unit,
    listState: LazyListState
) {
    if (versions.isNotEmpty()) {
        LazyColumn(
            modifier = modifier.nonInteractiveScrollbar(
                state = listState.scrollIndicatorState!!,
                orientation = Orientation.Vertical,
            ),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            items(versions) { version ->
                SelectVersionListItem(
                    modifier = Modifier.fillMaxWidth(),
                    version = version,
                    checked = selectedVersions.contains(version),
                    onChose = {
                        onVersionSelected(version)
                    },
                    onCancel = {
                        onVersionUnSelected(version)
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectVersionListItem(
    modifier: Modifier = Modifier,
    version: Version,
    checked: Boolean,
    onChose: () -> Unit,
    onCancel: () -> Unit
) {
    CryoPickCard(
        modifier = modifier,
        selected = checked,
        onClick = { if (checked) onCancel() else onChose() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CryoCheck(checked = checked)
            CommonVersionInfoLayout(
                modifier = Modifier.weight(1f),
                version = version,
                iconSize = 40.dp
            )
        }
    }
}

private fun LazyListScope.dependencyLayout(
    list: List<DependencyEntry>,
    titleRes: Int,
    defaultClasses: PlatformClasses,
    selectedKeys: List<String>,
    onSelectedChange: (String, Boolean) -> Unit,
    installedProjects: Map<Platform, Set<String>>,
    onDependencyClicked: (Platform, String, PlatformClasses) -> Unit
) {
    if (list.isEmpty()) return

    item {
        SectionTitle(text = stringResource(titleRes))
    }
    //前置项目列表
    items(list) { entry ->
        val dependency = entry.dependency
        val project = entry.project
        if (project == null) {
            //依赖项目信息获取失败，展示占位项且不可选
            AssetsUnavailableDependencyItem(
                modifier = Modifier.fillMaxWidth(),
                title = dependency.projectId ?: dependency.versionId.orEmpty(),
                statusRes = if (entry.notFound) {
                    R.string.download_assets_id_not_found
                } else {
                    R.string.download_assets_dependency_project_unavailable
                }
            )
            return@items
        }

        val key = dependency.cacheKey()
        val dependencyClasses = project.platformClasses(defaultClasses)
        AssetsVersionDependencyItem(
            modifier = Modifier.fillMaxWidth(),
            project = project,
            defaultClasses = defaultClasses,
            checked = selectedKeys.contains(key),
            onCheckedChange = { onSelectedChange(key, it) },
            installed = dependencyClasses == PlatformClasses.MOD &&
                    installedProjects[dependency.platform]?.contains(project.platformId()) == true,
            onClick = {
                onDependencyClicked(dependency.platform, project.platformId(), dependencyClasses)
            }
        )
    }
}

/**
 * 是否可安装：依赖项目信息获取成功的才能被选中安装
 */
private fun DependencyEntry.isInstallable(): Boolean = project != null

/**
 * 该依赖是否为本地已安装的模组项目
 */
private fun DependencyEntry.isModInstalled(
    installedProjects: Map<Platform, Set<String>>,
    defaultClasses: PlatformClasses
): Boolean {
    val project = project ?: return false
    return project.platformClasses(defaultClasses) == PlatformClasses.MOD &&
            installedProjects[dependency.platform]?.contains(project.platformId()) == true
}

/**
 * 依赖项目信息获取失败时的占位项
 */
@Composable
private fun AssetsUnavailableDependencyItem(
    modifier: Modifier = Modifier,
    title: String,
    @StringRes statusRes: Int,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
) {
    CryoPickCard(
        modifier = modifier,
        selected = false,
        onClick = null,
        dimmed = true
    ) {
        Column(
            modifier = Modifier.padding(all = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssetsIcon(
                    modifier = Modifier.clip(shape = RoundedCornerShape(10.dp)),
                    size = 48.dp,
                    iconUrl = null
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CryoCheck(checked = false, enabled = false)
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(statusRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AssetsVersionDependencyItem(
    modifier: Modifier = Modifier,
    project: PlatformProject,
    defaultClasses: PlatformClasses,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    installed: Boolean,
    onClick: () -> Unit = {},
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
) {
    //项目基本信息
    val platform = remember { project.platform() }
    val title = remember { project.platformTitle() }
    val summary = remember { project.platformSummary() }
    val iconUrl = remember { project.platformIconUrl() }
    val downloads = remember { project.platformDownloadCount() }
    val follows = remember { project.platformFollows() }
    val modLoaders = remember { project.platformModLoaders() }
    val classes = remember { project.platformClasses(defaultClasses) }

    CryoPickCard(
        modifier = modifier,
        selected = checked,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(all = 10.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                AssetsIcon(
                    modifier = Modifier.clip(shape = RoundedCornerShape(10.dp)),
                    size = 48.dp,
                    iconUrl = iconUrl
                )
                Spacer(modifier = Modifier.weight(1f))
                CryoCheck(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally),
                    checked = checked,
                    onToggle = { onCheckedChange(!checked) }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProjectTitleHead(
                    platform = platform,
                    title = title,
                    author = null //ui太小，展示不下
                )
                summary?.let { summary ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        //描述
                        Text(
                            modifier = Modifier.weight(1f),
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        AssetsDependencyCounts(
                            downloads = downloads,
                            follows = follows
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                //加载器标签与已安装标注
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetsDependencyTags(
                        modifier = Modifier.weight(1f),
                        modLoaders = modLoaders
                    )
                    if (summary == null) {
                        //没有摘要时，计数移到底部行展示
                        AssetsDependencyCounts(
                            downloads = downloads,
                            follows = follows
                        )
                    }
                    ClassesIdentifier(classes)
                    if (installed) {
                        InstalledModBadge()
                    }
                }
            }
        }
    }
}

/**
 * 依赖项目支持的模组加载器标签
 */
@Composable
private fun AssetsDependencyTags(
    modifier: Modifier = Modifier,
    modLoaders: List<PlatformDisplayLabel>?
) {
    Row(
        modifier = modifier
            .basicMarquee(iterations = Int.MAX_VALUE),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modLoaders?.forEach { modLoader ->
            com.russoviller.cryogenic.ui.components.CryoTag(text = modLoader.getDisplayName())
        }
    }
}

/**
 * 依赖项目的下载量与收藏量
 */
@Composable
private fun AssetsDependencyCounts(
    modifier: Modifier = Modifier,
    downloads: Long,
    follows: Long?
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.alpha(0.7f),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_download_2_outlined),
                contentDescription = null
            )
            Text(
                text = formatNumberByLocale(context, downloads),
                style = MaterialTheme.typography.labelSmall
            )
        }
        follows?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.ic_favorite_outlined),
                    contentDescription = null
                )
                Text(
                    text = formatNumberByLocale(context, it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


// =====================================================================
// Visual do Cryogenic
// =====================================================================

/** Painel interno escuro (lista de dependências / lista de versões). */
@Composable
private fun Modifier.cryoPanel(): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return this
        .clip(shape)
        .background(CryoGlassColors.Deep.copy(alpha = 0.45f))
        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), shape)
}

/** Título de seção com barrinha de destaque. */
@Composable
private fun SectionTitle(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora)))
        )
        MarqueeText(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
    }
}

@Composable
private fun SmallChip(text: String, active: Boolean, onClick: () -> Unit) {
    val ice = CryoGlassColors.Ice
    Text(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) ice.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, ice.copy(alpha = if (active) 0.9f else 0.4f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = ice,
        maxLines = 1
    )
}

/** Cabeçalho: ícone de download, título e contadores do que vai ser instalado. */
@Composable
private fun InstallDialogHeader(selectedVersions: Int, selectedDependencies: Int) {
    val ice = CryoGlassColors.Ice
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(R.drawable.ic_download_2_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.download_install),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.cryo_install_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CounterPill(value = selectedVersions, label = stringResource(R.string.cryo_install_versions_label), color = ice)
        if (selectedDependencies > 0) {
            CounterPill(value = selectedDependencies, label = stringResource(R.string.cryo_install_deps_label), color = CryoGlassColors.Aurora)
        }
    }
}

@Composable
private fun CounterPill(value: Int, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/** Cartão selecionável: borda e fundo acendem quando marcado. */
@Composable
private fun CryoPickCard(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)?,
    dimmed: Boolean = false,
    content: @Composable () -> Unit
) {
    val ice = CryoGlassColors.Ice
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .alpha(if (dimmed) 0.7f else 1f)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(ice.copy(alpha = 0.20f), CryoGlassColors.Aurora.copy(alpha = 0.10f)))
                else SolidColor(Color.White.copy(alpha = 0.04f))
            )
            .border(if (selected) 1.5.dp else 1.dp, if (selected) cryoBorderBrush() else SolidColor(ice.copy(alpha = 0.18f)), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/** Caixa de marcar do Cryogenic (quadrado arredondado com degradê e ✓). */
@Composable
private fun CryoCheck(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    val ice = CryoGlassColors.Ice
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(26.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .then(
                if (checked) Modifier.cryoBackground(cryoGradient())
                else Modifier
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.5.dp, ice.copy(alpha = 0.55f), shape)
            )
            .then(if (onToggle != null && enabled) Modifier.clickable(onClick = onToggle) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
    }
}
