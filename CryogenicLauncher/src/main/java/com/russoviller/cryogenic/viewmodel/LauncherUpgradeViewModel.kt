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

package com.russoviller.cryogenic.viewmodel

import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.header
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.path.GLOBAL_CLIENT
import com.russoviller.cryogenic.path.GLOBAL_JSON
import com.russoviller.cryogenic.path.URL_PROJECT_INFO
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleListDialog
import com.russoviller.cryogenic.ui.screens.content.elements.DisabledAlpha
import com.russoviller.cryogenic.ui.upgrade.UpgradeDialog
import com.russoviller.cryogenic.ui.upgrade.UpgradeFilesDialog
import com.russoviller.cryogenic.upgrade.GithubContentApi
import com.russoviller.cryogenic.upgrade.RemoteData
import com.russoviller.cryogenic.upgrade.TooFrequentOperationException
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.network.safeBodyAsJson
import com.russoviller.cryogenic.utils.network.withRetry
import com.russoviller.cryogenic.utils.string.decodeBase64
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "LauncherUpgradeVM"

sealed interface LauncherUpgradeOperation {
    data object None : LauncherUpgradeOperation
    /** 已检查到启动器存在新版本，展示更新信息 */
    data class Upgrade(val data: RemoteData) : LauncherUpgradeOperation
    /** 选择要安装的安装包文件 */
    data class SelectApk(val data: RemoteData) : LauncherUpgradeOperation
    /** 打开网盘分享 */
    data class OpenCloudDrive(val cloudDrive: RemoteData.CloudDrive) : LauncherUpgradeOperation
}

/**
 * 最新版本的信息获取源
 */
private const val LATEST_VERSION = "latest_version_md.json"
private const val LATEST_API_URL = "$URL_PROJECT_INFO/$LATEST_VERSION"
const val CRYO_RELEASES_PAGE = "https://github.com/RussoViller/Cryogenic-Launcher/releases"
private const val CRYO_RELEASES_API = "https://api.github.com/repos/RussoViller/Cryogenic-Launcher/releases/latest"

private val cryoVersionRegex = Regex("(?<![\\d.])(\\d{1,3})\\.(\\d{1,3})(?:\\.(\\d{1,3}))?(?![\\d.])")

fun cryoVersionCode(tag: String): Int? {
    val match = cryoVersionRegex.find(tag) ?: return null
    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch = match.groupValues[3].ifEmpty { "0" }.toInt()
    return major * 100000 + minor * 1000 + patch
}

/** APK da versão IS (armazenamento interno): nome termina em -IS.apk. */
fun isInternalStorageApk(name: String): Boolean = Regex("[-_ .]IS\\.apk$", RegexOption.IGNORE_CASE).containsMatchIn(name)

/** Arquitetura deste APK (a do build, não a do aparelho): o v7a atualiza para v7a mesmo num aparelho 64 bits. */
fun cryoBuildArch(): RemoteData.RemoteFile.Arch = when (com.russoviller.cryogenic.BuildKeys.BUILD_ARCH) {
    "arm64" -> RemoteData.RemoteFile.Arch.ARM64
    "arm" -> RemoteData.RemoteFile.Arch.ARM
    "x86_64" -> RemoteData.RemoteFile.Arch.X86_64
    "x86" -> RemoteData.RemoteFile.Arch.X86
    else -> RemoteData.RemoteFile.Arch.ALL
}

private fun parseGithubRelease(text: String): RemoteData {
    val root = com.google.gson.JsonParser.parseString(text).asJsonObject
    val tag = root.get("tag_name").asString
    val name = root.get("name")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    val assetNames = root.getAsJsonArray("assets")?.mapNotNull { it.asJsonObject.get("name")?.asString }.orEmpty()
    val candidates = listOf(name, tag) + assetNames.filter { it.endsWith(".apk", ignoreCase = true) }
    val code = candidates.firstNotNullOfOrNull { cryoVersionCode(it) } ?: 0
    val version = candidates.firstOrNull { cryoVersionCode(it) != null }?.let { source ->
        cryoVersionRegex.find(source)?.value?.let { "r$it" }
    } ?: tag
    val body = root.get("body")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    //cada APK atualiza para o mesmo tipo: normal → normal, IS → IS
    val thisIsIS = com.russoviller.cryogenic.BuildKeys.STORAGE_MODE == "is"
    val assets = root.getAsJsonArray("assets")?.mapNotNull { element ->
        val asset = element.asJsonObject
        val name = asset.get("name")?.asString ?: return@mapNotNull null
        if (!name.endsWith(".apk", ignoreCase = true)) return@mapNotNull null
        if (isInternalStorageApk(name) != thisIsIS) return@mapNotNull null
        RemoteData.RemoteFile(
            fileName = name,
            uri = asset.get("browser_download_url").asString,
            arch = when {
                name.contains("arm64", ignoreCase = true) -> RemoteData.RemoteFile.Arch.ARM64
                name.contains("armeabi-v7a", ignoreCase = true) || name.contains("arm32", ignoreCase = true) -> RemoteData.RemoteFile.Arch.ARM
                name.contains("x86_64", ignoreCase = true) -> RemoteData.RemoteFile.Arch.X86_64
                else -> RemoteData.RemoteFile.Arch.ALL
            },
            size = asset.get("size")?.asLong ?: 0L
        )
    }.orEmpty()
    val page = root.get("html_url")?.takeIf { !it.isJsonNull }?.asString ?: CRYO_RELEASES_PAGE
    return RemoteData(
        code = code,
        version = version,
        createdAt = root.get("published_at")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
        files = assets.ifEmpty {
            listOf(RemoteData.RemoteFile(fileName = tag, uri = page, arch = RemoteData.RemoteFile.Arch.ALL))
        },
        defaultBody = RemoteData.RemoteBody(language = "en", markdown = body.ifBlank { page }),
        bodies = emptyList()
    )
}
private const val LATEST_API_CHINESE_URL = "https://repo.miawa.cn/zalith-info/v2/$LATEST_VERSION"

/**
 * 用于记录启动器更新 ViewModel
 */
class LauncherUpgradeViewModel: ViewModel() {
    var operation by mutableStateOf<LauncherUpgradeOperation>(LauncherUpgradeOperation.None)

    private val checkMutex = Mutex()

    /**
     * 检查是否在限频时间内
     * @param time 限频时间（毫秒）
     * @param lastCheckTime 上次检查的时间戳
     */
    private fun isWithinRateLimit(
        time: Long,
        lastCheckTime: Long
    ): Boolean {
        val currentTime = System.currentTimeMillis()
        if (lastCheckTime > currentTime) {
            //用户调整到了未来的时间，无法正常判断
            //直接允许进行检查
            return false
        }
        return currentTime - lastCheckTime < time
    }

    /**
     * 更新最后一次检查的时间
     */
    private fun updateLastCheckTime() {
        AllSettings.lastUpgradeCheck.save(System.currentTimeMillis())
    }

    /**
     * 在启动时，快速完成所有的检查
     */
    fun checkOnAppStart(
        onIsLatest: suspend () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (
                isWithinRateLimit(
                    time = TimeUnit.HOURS.toMillis(1L),
                    lastCheckTime = AllSettings.lastUpgradeCheck.getValue()
                )
            ) {
                Logger.info(TAG, "App start check: Within rate limit, skipping")
                return@launch
            }

            val data = fetchRemoteData()
            if (data != null) {
                checkForUpgrade(
                    data = data,
                    lastIgnored = AllSettings.lastIgnoredVersion.getValue(),
                    ignoreDismissedVersions = true, //启动时检查忽略用户已忽略的版本
                    onUpgrade = { data ->
                        operation = LauncherUpgradeOperation.Upgrade(data)
                    },
                    onIsLatest = onIsLatest
                )
            }
            updateLastCheckTime()
        }
    }

    /**
     * 用户在设置内手动点击检查更新
     * @param onInProgress 准备检查更新
     * @param onIsLatest 当前启动器是最新版
     */
    suspend fun checkManually(
        onInProgress: suspend () -> Unit = {},
        onIsLatest: suspend () -> Unit = {}
    ): Boolean {
        return checkMutex.withLock {
            if (
                isWithinRateLimit(
                    time = TimeUnit.SECONDS.toMillis(5L),
                    lastCheckTime = AllSettings.lastUpgradeCheck.getValue()
                )
            ) throw TooFrequentOperationException()

            onInProgress()

            val data = fetchRemoteData()
            if (data != null) {
                checkForUpgrade(
                    data = data,
                    lastIgnored = AllSettings.lastIgnoredVersion.getValue(),
                    ignoreDismissedVersions = false,
                    onUpgrade = { data ->
                        operation = LauncherUpgradeOperation.Upgrade(data)
                    },
                    onIsLatest = onIsLatest
                )
            }
            updateLastCheckTime()
            data != null
        }
    }

    /**
     * 从远端获取最新的启动器信息
     */
    private suspend fun fetchRemoteData(): RemoteData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                withRetry(logTag = "LauncherUpgrade", maxRetries = 2) {
                    val text = GLOBAL_CLIENT.get(CRYO_RELEASES_API) {
                        header("Accept", "application/vnd.github+json")
                    }.bodyAsText()
                    parseGithubRelease(text)
                }
            }.getOrElse { e ->
                Logger.warning(TAG, "Failed to check for launcher upgrade!", e)
                null
            }
        }
    }

    /**
     * 检查启动器是否需要更新
     * @param lastIgnored 上次弹出更新弹窗时，用户所忽略的版本号
     * @param ignoreDismissedVersions 是否忽略用户已忽略的版本
     * @param onUpgrade 发现需要更新时调用
     * @param onIsLatest 当前已是最新版本时
     */
    private suspend fun checkForUpgrade(
        data: RemoteData,
        lastIgnored: Int?,
        ignoreDismissedVersions: Boolean,
        onUpgrade: suspend (RemoteData) -> Unit,
        onIsLatest: suspend () -> Unit = {}
    ) {
        val currentVersionCode = cryoVersionCode(BuildConfig.VERSION_NAME) ?: BuildConfig.VERSION_CODE
        if (data.code > 0 && currentVersionCode < data.code) {
            //启动器为旧版本
            when {
                ignoreDismissedVersions && lastIgnored == data.code -> {
                    //忽略这次更新
                    Logger.info(TAG, "Launcher update detected: $currentVersionCode -> ${data.code}, but ignored by user")
                }
                else -> {
                    //弹出更新弹窗
                    Logger.info(TAG, "Launcher update detected: $currentVersionCode -> ${data.code}, dialog shown to user")
                    onUpgrade(data)
                }
            }
        } else {
            Logger.info(TAG, "Launcher is running the latest version: $currentVersionCode")
            onIsLatest()
        }
    }
}

@Composable
fun LauncherUpgradeOperation(
    operation: LauncherUpgradeOperation,
    onChanged: (LauncherUpgradeOperation) -> Unit,
    onIgnoredClick: (code: Int) -> Unit,
    onLinkClick: (String) -> Unit
) {
    when (operation) {
        is LauncherUpgradeOperation.None -> {}
        is LauncherUpgradeOperation.Upgrade -> {
            UpgradeDialog(
                data = operation.data,
                onDismissRequest = {
                    onChanged(LauncherUpgradeOperation.None)
                },
                onFilesClick = {
                    onChanged(LauncherUpgradeOperation.SelectApk(operation.data))
                },
                onIgnored = {
                    onIgnoredClick(operation.data.code)
                },
                onLinkClick = onLinkClick,
                onCloudDriveClick = { cloudDrive ->
                    onChanged(LauncherUpgradeOperation.OpenCloudDrive(cloudDrive))
                }
            )
        }
        is LauncherUpgradeOperation.SelectApk -> {
            UpgradeFilesDialog(
                data = operation.data,
                onDismissRequest = {
                    onChanged(LauncherUpgradeOperation.None)
                },
                onFileSelected = { file ->
                    onLinkClick(file.uri)
                    onChanged(LauncherUpgradeOperation.None)
                }
            )
        }
        is LauncherUpgradeOperation.OpenCloudDrive -> {
            val current by remember(operation) {
                mutableStateOf<RemoteData.CloudDrive.Link?>(null)
            }
            SimpleListDialog(
                title = stringResource(R.string.upgrade_cloud_drive),
                items = operation.cloudDrive.links,
                onItemSelected = { link ->
                    onLinkClick(link.link)
                },
                onDismissRequest = {
                    onChanged(LauncherUpgradeOperation.None)
                },
                current = current,
                itemLayout = { item, isCurrent, onClick ->
                    CloudDriveLayout(
                        link = item,
                        selected = isCurrent,
                        onClick = onClick
                    )
                },
                showConfirm = true,
                confirmText = {
                    MarqueeText(text = stringResource(R.string.generic_confirm))
                }
            )
        }
    }
}


@Composable
private fun CloudDriveLayout(
    link: RemoteData.CloudDrive.Link,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
        Column(
            modifier = Modifier.alpha(if (enabled) 1.0f else DisabledAlpha),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            //网盘名称
            MarqueeText(
                modifier = Modifier.fillMaxWidth(),
                text = link.name,
                style = MaterialTheme.typography.labelMedium
            )
            //网盘链接
            MarqueeText(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.7f),
                text = link.link,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
