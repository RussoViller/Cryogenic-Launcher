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

import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.download.assets.platform.PlatformClasses
import com.russoviller.cryogenic.game.download.assets.platform.PlatformDisplayLabel
import com.russoviller.cryogenic.game.download.assets.platform.PlatformProject
import com.russoviller.cryogenic.game.download.assets.platform.PlatformVersion
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.game.version.mod.InstalledMod
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.components.IconTextButton
import com.russoviller.cryogenic.ui.components.LittleTextLabel
import com.russoviller.cryogenic.ui.components.ShimmerBox
import com.russoviller.cryogenic.ui.components.rememberMaxHeight
import com.russoviller.cryogenic.ui.screens.content.elements.backgroundGlass
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.formatNumberByLocale
import com.russoviller.cryogenic.utils.getTimeAgo
import com.russoviller.cryogenic.utils.isChinese
import com.russoviller.cryogenic.utils.string.isNotEmptyOrBlank
import org.jackhuang.hmcl.addon.AddonProjectInfo
import org.jackhuang.hmcl.util.versioning.GameVersionNumber

sealed interface DownloadAssetsState<T> {
    class Getting<T> : DownloadAssetsState<T>
    data class Success<T>(val result: T) : DownloadAssetsState<T>
    data class Error<T>(val message: AndroidStringText) : DownloadAssetsState<T>
}

sealed interface DownloadAssetsVersionLoading {
    data object None: DownloadAssetsVersionLoading
    /** 开始加载分页数据 */
    data object StartLoadPage: DownloadAssetsVersionLoading
    /** 加载分页数据 */
    data class LoadingPage(val chunk: Int, val page: Int): DownloadAssetsVersionLoading
}

/**
 * 版本、模组加载器 版本信息分组
 */
class VersionInfoMap(
    val gameVersion: String,
    val loader: PlatformDisplayLabel?,
    val versions: List<PlatformVersion>,
    val isAdapt: Boolean
)

suspend fun <E: PlatformVersion> List<E>.initAllGeneric(
    currentProjectId: String,
    also: suspend (E) -> Unit = {}
): List<E> {
    return mapNotNull { version ->
        if (!version.initFile(currentProjectId)) return@mapNotNull null
        version.also {
            also(it)
        }
    }.sortedByDescending {
        //排序：最新的版本在前
        it.platformDatePublished()
    }
}

/**
 * 初始化全部版本数据，并筛选出成功初始化的所有版本
 */
suspend fun List<PlatformVersion>.initAll(
    currentProjectId: String,
    also: suspend (PlatformVersion) -> Unit = {}
): List<PlatformVersion> {
    return initAllGeneric<PlatformVersion>(currentProjectId, also)
}

fun List<PlatformVersion>.mapWithVersions(classes: PlatformClasses): List<VersionInfoMap> {
    val grouped = mutableMapOf<Pair<String, PlatformDisplayLabel?>, MutableList<PlatformVersion>>()

    forEach { version ->
        val labels = version.platformLoaders().ifEmpty { listOf(null) }
        version.platformGameVersion().forEach { gameVer ->
            labels.forEach { loaderLabel ->
                grouped.getOrPut(gameVer to loaderLabel) { mutableListOf() } += version
            }
        }
    }

    val currentVersion = VersionsManager.currentVersion.value

    return grouped.map { (key, versions) ->
        VersionInfoMap(
            gameVersion = key.first,
            loader = key.second,
            versions = versions,
            isAdapt = when (classes) {
                PlatformClasses.MOD_PACK -> false //整合包将作为单独的版本下载，不再需要与现有版本进行匹配
                else -> isVersionAdapt(currentVersion, key.first, key.second)
            }
        )
    }.sortedByVersionAndLoader()
}

private fun List<VersionInfoMap>.sortedByVersionAndLoader(): List<VersionInfoMap> {
    return sortedWith { a, b ->
        // 比较版本号
        val versionCompare = -GameVersionNumber.compare(a.gameVersion, b.gameVersion)
        if (versionCompare != 0) {
            versionCompare
        } else {
            when {
                a.loader == null && b.loader == null -> 0
                a.loader == null -> 1
                b.loader == null -> -1
                else -> a.loader.getDisplayName().compareTo(b.loader.getDisplayName())
            }
        }
    }
}

/**
 * 当前资源版本是否与当前选择的游戏版本匹配
 */
private fun isVersionAdapt(
    currentVersion: Version?,
    gameVersion: String,
    loader: PlatformDisplayLabel?
): Boolean {
    return if (currentVersion == null) {
        false //没安装版本，无法判断
    } else {
        if (currentVersion.getVersionInfo()?.minecraftVersion != gameVersion) {
            false //游戏版本不匹配
        } else {
            //判断模组加载器匹配情况，多加载器组合时任一加载器匹配即可
            val loaderInfos = currentVersion.getVersionInfo()?.loaderInfos.orEmpty()
            when {
                loader == null -> true //资源没有模组加载器信息，直接判定适配
                loaderInfos.isEmpty() -> false //资源有模组加载器，但当前版本没有模组加载器信息，不适配
                else -> loaderInfos.any { it.loader.displayName.equals(loader.getDisplayName(), true) }
            }
        }
    }
}

/**
 * 资源版本分组可折叠列表
 * @param defaultExpanded 是否默认展开，默认为是否适配当前版本
 * @param installedChecker 查询版本本地是否已安装，null 则不进行已安装标注
 */
@Composable
fun AssetsVersionItemLayout(
    modifier: Modifier = Modifier,
    infoMap: VersionInfoMap,
    defaultExpanded: Boolean = infoMap.isAdapt,
    maxListHeight: Dp = rememberMaxHeight(),
    shape: Shape = MaterialTheme.shapes.large,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    installedChecker: ((PlatformVersion) -> InstalledMod?)? = null,
    onItemClicked: (PlatformVersion) -> Unit = {}
) {
    var expanded by remember(infoMap) { mutableStateOf(defaultExpanded) }
    val hasInstalled = installedChecker != null && infoMap.versions.any { installedChecker(it) != null }
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .then(if (expanded) Modifier.cryoGlow(18.dp, selected = true, strength = 0.5f) else Modifier)
            .cryoGlass(shape = cardShape, glow = false, cornerRadius = 18.dp, selected = expanded)
    ) {
        AssetsVersionHeadLayout(
            modifier = Modifier.fillMaxWidth(),
            infoMap = infoMap,
            isAdapt = infoMap.isAdapt,
            hasInstalled = hasInstalled,
            expanded = expanded,
            onClick = { expanded = !expanded }
        )

        if (infoMap.versions.isNotEmpty()) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = getAnimateTween()),
                exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(260.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(infoMap.versions.size) { index ->
                        val version = infoMap.versions[index]
                        AssetsVersionListItem(
                            modifier = Modifier.fillMaxWidth(),
                            version = version,
                            installed = installedChecker?.invoke(version),
                            onClick = { onItemClicked(version) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetsVersionHeadLayout(
    modifier: Modifier = Modifier,
    infoMap: VersionInfoMap,
    isAdapt: Boolean,
    hasInstalled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .cryoBackground(
                    if (expanded) cryoGradient()
                    else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.18f), CryoGlassColors.Navy.copy(alpha = 0.5f)))
                )
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (expanded) 0.9f else 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_package_2_outlined),
                contentDescription = null,
                tint = if (expanded) CryoGlassColors.Deep else CryoGlassColors.Ice
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = infoMap.gameVersion,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                infoMap.loader?.let { loader ->
                    CryoMiniTag(text = loader.getDisplayName(), accent = CryoGlassColors.Ice)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.cryo_assets_files_count, infoMap.versions.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                )
                if (isAdapt) {
                    CryoMiniTag(text = stringResource(R.string.cryo_assets_adapt), accent = CryoGlassColors.Aurora, iconRes = R.drawable.ic_star_filled)
                }
                if (hasInstalled) {
                    CryoMiniTag(text = stringResource(R.string.cryo_assets_installed), accent = Color(0xFF7CF5A8))
                }
            }
        }
        if (infoMap.versions.isNotEmpty()) {
            val rotation by animateFloatAsState(
                targetValue = if (expanded) -180f else 0f,
                animationSpec = getAnimateTween()
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                    contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse),
                    tint = CryoGlassColors.Ice
                )
            }
        }
    }
}

@Composable
private fun CryoMiniTag(text: String, accent: Color, iconRes: Int? = null) {
    com.russoviller.cryogenic.ui.components.CryoTag(
        text = text,
        accent = accent,
        icon = iconRes?.let { painterResource(it) },
        iconSize = 11.dp
    )
}

@Composable
private fun AssetsVersionListItem(
    modifier: Modifier = Modifier,
    version: PlatformVersion,
    installed: InstalledMod? = null,
    onClick: () -> Unit = {}
) {
    val releaseType = remember { version.platformReleaseType() }
    val displayName = remember { version.platformDisplayName() }
    val downloadCount = remember { version.platformDownloadCount() }
    val date = remember { version.platformDatePublished() }
    val context = LocalContext.current
    val isInstalled = installed != null
    val tileShape = RoundedCornerShape(14.dp)
    val green = Color(0xFF7CF5A8)

    Row(
        modifier = modifier
            .clip(tileShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        (if (isInstalled) green else releaseType.color).copy(alpha = 0.14f),
                        CryoGlassColors.Navy.copy(alpha = 0.45f),
                        CryoGlassColors.Deep.copy(alpha = 0.55f)
                    )
                )
            )
            .border(1.dp, (if (isInstalled) green else CryoGlassColors.Ice).copy(alpha = if (isInstalled) 0.7f else 0.22f), tileShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(releaseType.color.copy(alpha = 0.18f))
                .border(1.5.dp, releaseType.color.copy(alpha = 0.8f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = releaseType.name.take(1),
                style = MaterialTheme.typography.titleMedium.copy(shadow = null),
                fontWeight = FontWeight.Black,
                color = releaseType.color
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionMeta(R.drawable.ic_download_2_outlined, formatNumberByLocale(context, downloadCount))
                VersionMeta(R.drawable.ic_autorenew, getTimeAgo(context = context, pastInstant = date))
            }
            Text(
                text = if (isInstalled) stringResource(R.string.cryo_assets_installed) else stringResource(releaseType.textRes),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, shadow = null),
                fontWeight = FontWeight.Black,
                color = if (isInstalled) green else releaseType.color,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_download_2_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
    }
}

@Composable
private fun VersionMeta(iconRes: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(13.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice.copy(alpha = 0.8f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

/**
 * 项目相关链接UI
 */
@Composable
fun ProjectUrlsContent(
    platform: Platform,
    urls: PlatformProject.Urls,
    mcmod: AddonProjectInfo?,
    openLink: (String) -> Unit,
) {
    urls.projectUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = when (platform) {
                Platform.CURSEFORGE -> painterResource(R.drawable.img_platform_curseforge)
                Platform.MODRINTH -> painterResource(R.drawable.img_platform_modrinth)
            },
            text = stringResource(R.string.download_assets_project_link)
        )
    }
    urls.sourceUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_code),
            text = stringResource(R.string.download_assets_source_link)
        )
    }
    urls.issuesUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_chat_info),
            text = stringResource(R.string.download_assets_issues_link)
        )
    }
    urls.wikiUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_import_contacts_outlined),
            text = stringResource(R.string.download_assets_wiki_link)
        )
    }
    val context = LocalContext.current
    mcmod?.takeIf {
        isChinese(context)
    }?.mcmodUrl()?.takeIf {
        it.isNotEmptyOrBlank()
    }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_link),
            text = "MC 百科" //品牌名不需要翻译，硬编码
        )
    }
}

/**
 * 屏幕截图与描述UI
 */
@Composable
fun ScreenshotItemLayout(
    screenshot: PlatformProject.Screenshot,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    val context = LocalContext.current

    val imageRequest = remember(screenshot) {
        screenshot.imageUrl.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        placeholder = null,
        error = painterResource(R.drawable.ic_unknown_icon)
    )

    val state by painter.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            AsyncImagePainter.State.Empty -> {
                //NONE
            }
            is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Loading -> {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(shape)
                )
            }
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
                        .clip(shape)
                )
            }
        }

        //标题与简介部分
        if (screenshot.title != null && screenshot.title == screenshot.description) {
            //标题与简介内容相同，则不需要两个都显示
            //会有作者喜欢把标题与简介设置成一样的内容
            Text(
                text = screenshot.title,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        } else {
            screenshot.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
            screenshot.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
