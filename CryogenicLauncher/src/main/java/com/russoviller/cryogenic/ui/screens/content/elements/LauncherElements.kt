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

package com.russoviller.cryogenic.ui.screens.content.elements

import com.russoviller.cryogenic.ui.components.cryoBackdropSource
import coil3.request.CachePolicy
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.DisposableEffect
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.account.accountErrorText
import com.russoviller.cryogenic.game.account.auth_server.AuthServerHelper
import com.russoviller.cryogenic.game.account.isMicrosoftAccount
import com.russoviller.cryogenic.game.account.microsoftLogin
import com.russoviller.cryogenic.game.plugin.ApkPlugin
import com.russoviller.cryogenic.game.plugin.natives.NativePluginManager
import com.russoviller.cryogenic.game.plugin.renderer.RendererPluginManager
import com.russoviller.cryogenic.game.renderer.RendererInterface
import com.russoviller.cryogenic.game.renderer.Renderers
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.hasVulkanBackend
import com.russoviller.cryogenic.game.version.installed.utils.isBiggerVer
import com.russoviller.cryogenic.game.version.installed.utils.isLowerVer
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.BackgroundBlur
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.components.ButtonPosition
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.PositionButton
import com.russoviller.cryogenic.ui.components.PositionFilledTonalButton
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.VideoPlayer
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import com.russoviller.cryogenic.ui.screens.content.FirstLoginMenu
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.canHandlePermission
import com.russoviller.cryogenic.utils.checkStoragePermissions
import com.russoviller.cryogenic.utils.file.InvalidFilenameException
import com.russoviller.cryogenic.utils.file.checkFilenameValidity
import com.russoviller.cryogenic.utils.hasStoragePermission
import com.russoviller.cryogenic.utils.image.isGifFile
import com.russoviller.cryogenic.viewmodel.BackgroundViewModel
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.LaunchGameViewModel
import com.russoviller.cryogenic.viewmodel.sendToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
sealed interface QuickPlay : Parcelable {
    /** 快速启动游玩存档  仅支持 1.20+ 23w14a+ */
    @Parcelize
    data class Save(val saveName: String): QuickPlay

    /** 快速启动游玩服务器 */
    @Parcelize
    data class Server(val serverAddress: String): QuickPlay
}

sealed interface LaunchGameOperation {
    data object None : LaunchGameOperation
    /** 没有安装版本/没有选中有效版本 */
    data object NoVersion : LaunchGameOperation
    /** 版本名称非法时 */
    data class InvalidVersionName(val th: InvalidFilenameException) : LaunchGameOperation
    /** 没有可用账号 */
    data object NoAccount : LaunchGameOperation

    /** 渲染器可配置，但需要用到文件管理权限 */
    data class RendererNoStoragePermission(
        val renderer: RendererInterface,
        val version: Version,
        val quickPlay: QuickPlay?
    ) : LaunchGameOperation

    /** 当前渲染器不支持选中版本 */
    data class UnsupportedRenderer(
        val renderer: RendererInterface,
        val version: Version,
        val quickPlay: QuickPlay?
    ): LaunchGameOperation

    /** 当前已加载的插件不支持选中的版本 */
    data class UnsupportedPlugins(
        val plugins: List<ApkPlugin>,
        val version: Version,
        val quickPlay: QuickPlay?
    ): LaunchGameOperation

    /** 尝试启动：启动前检查一些东西 */
    data class TryLaunch(
        val version: Version?,
        val quickPlay: QuickPlay? = null,
        /** Pular a verificação de mods em conflito ("Prosseguir mesmo assim" ou já resolvido) */
        val skipModCheck: Boolean = false
    ) : LaunchGameOperation

    /** Mods que não funcionam juntos foram encontrados na versão */
    data class ModConflicts(
        val groups: List<com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker.Group>,
        val version: Version,
        val quickPlay: QuickPlay?
    ) : LaunchGameOperation

    /** 账号凭据已被服务端拒绝，需要重新登录 */
    data class AccountRelogin(
        val account: Account,
        val version: Version,
        val quickPlay: QuickPlay?,
        val logging: Boolean = false,
        val error: Throwable? = null
    ) : LaunchGameOperation

    /** 账号刷新失败，可选择跳过刷新继续启动 */
    data class AccountRefreshFailed(
        val account: Account,
        val error: Throwable,
        val version: Version,
        val quickPlay: QuickPlay?
    ) : LaunchGameOperation

    /** 正式启动 */
    data class RealLaunch(
        val version: Version,
        val quickPlay: QuickPlay?,
        val skipAccountRefresh: Boolean = false
    ) : LaunchGameOperation
}

@Composable
fun LaunchGameOperation(
    activity: Activity,
    eventViewModel: EventViewModel,
    launchGameViewModel: LaunchGameViewModel,
    exitActivity: () -> Unit,
    ensureVulkanSupported: suspend (Version) -> Boolean,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    toAccountManageScreen: (FirstLoginMenu) -> Unit = {},
    toVersionManageScreen: () -> Unit = {},
    navigateToWeb: (String) -> Unit = {},
    backToMain: () -> Unit = {},
    checkIfInWebScreen: () -> Boolean = { false }
) {
    val launchGameOperation by launchGameViewModel.launchGameOperation.collectAsStateWithLifecycle()

    when (val operation = launchGameOperation) {
        is LaunchGameOperation.None -> {}
        is LaunchGameOperation.NoVersion -> {
            LaunchedEffect(Unit) {
                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                toVersionManageScreen()
                launchGameViewModel.updateOperation(LaunchGameOperation.None)
            }
        }
        is LaunchGameOperation.InvalidVersionName -> {
            val th = operation.th
            SimpleAlertDialog(
                title = stringResource(R.string.versions_manage_invalid),
                text = th.getInvalidSummary(),
                confirmText = stringResource(R.string.generic_cancel),
                onDismiss = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.None)
                }
            )
        }
        is LaunchGameOperation.NoAccount -> {
            LaunchedEffect(Unit) {
                eventViewModel.sendToast(androidText(R.string.game_launch_no_account))
                toAccountManageScreen(FirstLoginMenu.NORMAL)
                launchGameViewModel.updateOperation(LaunchGameOperation.None)
            }
        }
        is LaunchGameOperation.RendererNoStoragePermission -> {
            LaunchedEffect(Unit) {
                val renderer = operation.renderer
                val version = operation.version
                val quickPlay = operation.quickPlay
                withContext(Dispatchers.Main) {
                    checkStoragePermissions(
                        activity = activity,
                        message = activity.getString(R.string.renderer_version_storage_permissions, renderer.getRendererName()),
                        messageSdk30 = activity.getString(R.string.renderer_version_storage_permissions_sdk30, renderer.getRendererName()),
                        onDialogCancel = {
                            //用户拒绝授权，但仍然允许启动（不过这会导致配置无法读取）
                            launchGameViewModel.updateOperation(LaunchGameOperation.RealLaunch(version, quickPlay))
                        }
                    )
                }
                launchGameViewModel.updateOperation(LaunchGameOperation.None)
            }
        }
        is LaunchGameOperation.UnsupportedRenderer -> {
            val renderer = operation.renderer
            val version = operation.version
            val quickPlay = operation.quickPlay
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.renderer_version_unsupported_warning, renderer.getRendererName()),
                confirmText = stringResource(R.string.generic_anyway),
                onConfirm = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.RealLaunch(version, quickPlay))
                },
                onDismiss = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.None)
                }
            )
        }
        is LaunchGameOperation.UnsupportedPlugins -> {
            val plugins = operation.plugins
            val version = operation.version
            val quickPlay = operation.quickPlay
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.plugin_unsupported_warning, plugins.joinToString(", ") { it.appName }),
                confirmText = stringResource(R.string.generic_anyway),
                onConfirm = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.RealLaunch(version, quickPlay))
                },
                onDismiss = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.None)
                }
            )
        }
        is LaunchGameOperation.ModConflicts -> {
            val version = operation.version
            val quickPlay = operation.quickPlay
            ModConflictDialog(
                groups = operation.groups,
                recheck = { com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker.check(version) },
                onAllSolved = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.TryLaunch(version, quickPlay, skipModCheck = true))
                },
                onContinueAnyway = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.TryLaunch(version, quickPlay, skipModCheck = true))
                },
                onCancel = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.None)
                },
                minecraft = remember(version) { runCatching { version.getVersionInfo()?.minecraftVersion }.getOrNull() },
                loader = remember(version) { runCatching { version.getVersionInfo()?.primaryLoader?.loader }.getOrNull() }
            )
        }
        is LaunchGameOperation.TryLaunch -> {
            LaunchedEffect(Unit) {
                val version = operation.version ?: run {
                    launchGameViewModel.updateOperation(LaunchGameOperation.NoVersion)
                    return@LaunchedEffect
                }

                try {
                    checkFilenameValidity(version.getVersionName())
                } catch (th: InvalidFilenameException) {
                    launchGameViewModel.updateOperation(LaunchGameOperation.InvalidVersionName(th))
                    return@LaunchedEffect
                }

                val quickPlay = operation.quickPlay

                AccountsManager.currentAccountFlow.value ?: run {
                    launchGameViewModel.updateOperation(LaunchGameOperation.NoAccount)
                    return@LaunchedEffect
                }

                //Mods em conflito entre si: avisa antes de abrir (evita crash)
                if (!operation.skipModCheck) {
                    val conflicts = runCatching {
                        com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker.check(version)
                    }.getOrDefault(emptyList())
                    if (conflicts.isNotEmpty()) {
                        launchGameViewModel.updateOperation(LaunchGameOperation.ModConflicts(conflicts, version, quickPlay))
                        return@LaunchedEffect
                    }
                }

                Renderers.setCurrentRenderer(version.getRenderer())
                val currentRenderer = Renderers.getCurrentRenderer()

                val mcVer = version.getVersionInfo()!!.minecraftVersion

                // 设备完全支持 Vulkan 时跳过渲染器的版本支持检查
                if (!version.hasVulkanBackend() || !ensureVulkanSupported(version)) {
                    val isRendererUnsupported =
                        (currentRenderer.getMinMCVersion()?.let { mcVer.isLowerVer(it) } ?: false) ||
                                (currentRenderer.getMaxMCVersion()?.let { mcVer.isBiggerVer(it) } ?: false)

                    if (isRendererUnsupported) {
                        launchGameViewModel.updateOperation(LaunchGameOperation.UnsupportedRenderer(currentRenderer, version, quickPlay))
                        return@LaunchedEffect
                    }
                }

                val unsupportedPlugins = NativePluginManager.getCheckedPlugins().filter { plugin ->
                    (plugin.minMCVer?.let { mcVer.isLowerVer(it) } ?: false) ||
                            (plugin.maxMCVer?.let { mcVer.isBiggerVer(it) } ?: false)
                }
                if (unsupportedPlugins.isNotEmpty()) {
                    launchGameViewModel.updateOperation(LaunchGameOperation.UnsupportedPlugins(unsupportedPlugins, version, quickPlay))
                    return@LaunchedEffect
                }

                //为可配置的渲染器检查文件管理权限
                //前提：系统支持这个设置
                if (
                    canHandlePermission &&  !hasStoragePermission &&
                    RendererPluginManager.isConfigurablePlugin(version.getRenderer())
                ) {
                    launchGameViewModel.updateOperation(LaunchGameOperation.RendererNoStoragePermission(currentRenderer, version, quickPlay))
                    return@LaunchedEffect
                }

                //正式启动游戏
                launchGameViewModel.updateOperation(LaunchGameOperation.RealLaunch(version, quickPlay))
            }
        }
        is LaunchGameOperation.AccountRelogin -> {
            if (operation.account.isMicrosoftAccount()) {
                var selectLoginMethod by remember { mutableStateOf(false) }

                val startReloginLogin: (useSystemBrowser: Boolean) -> Unit = { useSystemBrowser ->
                    launchGameViewModel.updateOperation(LaunchGameOperation.None)
                    microsoftLogin(
                        context = activity,
                        useSystemBrowser = useSystemBrowser,
                        toWeb = navigateToWeb,
                        toSystemBrowser = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        backToMain = backToMain,
                        checkIfInWebScreen = checkIfInWebScreen,
                        updateOperation = {},
                        showToast = { text, duration -> eventViewModel.sendToast(text, duration) },
                        submitError = submitError
                    ) {
                        activity.runOnUiThread {
                            launchGameViewModel.updateOperation(
                                LaunchGameOperation.RealLaunch(
                                    operation.version,
                                    operation.quickPlay
                                )
                            )
                        }
                    }
                }

                if (selectLoginMethod) {
                    MicrosoftLoginMethodDialog(
                        onDismissRequest = {
                            selectLoginMethod = false
                        },
                        onSystemBrowser = {
                            startReloginLogin(true)
                        },
                        onInternalBrowser = {
                            startReloginLogin(false)
                        }
                    )
                } else {
                    MicrosoftReloginDialog(
                        onDismissRequest = {
                            launchGameViewModel.updateOperation(LaunchGameOperation.None)
                        },
                        onConfirm = {
                            selectLoginMethod = true
                        }
                    )
                }
            } else {
                OtherAccountReloginDialog(
                    account = operation.account,
                    logging = operation.logging,
                    error = operation.error,
                    onDismissRequest = {
                        launchGameViewModel.updateOperation(LaunchGameOperation.None)
                    },
                    onConfirm = { password ->
                        launchGameViewModel.updateOperation(operation.copy(logging = true, error = null))
                        AuthServerHelper(
                            baseUrl = operation.account.otherBaseUrl!!,
                            serverName = operation.account.accountType!!,
                            email = operation.account.otherAccount!!,
                            password = password,
                            onSuccess = { acc, _ ->
                                AccountsManager.markSessionValidated(acc)
                                AccountsManager.suspendSaveAccount(acc)
                                activity.runOnUiThread {
                                    launchGameViewModel.updateOperation(
                                        LaunchGameOperation.RealLaunch(
                                            operation.version,
                                            operation.quickPlay)
                                    )
                                }
                            },
                            onFailed = { th ->
                                activity.runOnUiThread {
                                    launchGameViewModel.updateOperation(
                                        operation.copy(
                                            logging = false,
                                            error = th
                                        ))
                                }
                            }
                        ).let { helper ->
                            TaskSystem.submitTask(helper.justLogin(activity, operation.account))
                        }
                    }
                )
            }
        }
        is LaunchGameOperation.AccountRefreshFailed -> {
            val state = operation
            AccountRefreshFailedDialog(
                error = state.error,
                onSkip = {
                    launchGameViewModel.updateOperation(
                        LaunchGameOperation.RealLaunch(
                            state.version,
                            state.quickPlay,
                            skipAccountRefresh = true
                        )
                    )
                },
                onRetry = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.RealLaunch(state.version, state.quickPlay))
                },
                onCancel = {
                    launchGameViewModel.updateOperation(LaunchGameOperation.None)
                }
            )
        }
        is LaunchGameOperation.RealLaunch -> {
            LaunchedEffect(Unit) {
                val version = operation.version
                val quickPlay = operation.quickPlay
                version.apply {
                    offlineAccountLogin = false
                    quickPlaySingle = quickPlay
                }
                launchGameViewModel.start(
                    activity = activity,
                    version = version,
                    exitActivity = exitActivity,
                    submitError = submitError,
                    quickPlay = quickPlay,
                    skipAccountRefresh = operation.skipAccountRefresh
                )
                launchGameViewModel.updateOperation(LaunchGameOperation.None)
            }
        }
    }
}

@Composable
private fun AccountRefreshFailedDialog(
    error: Throwable,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_refresh_failed_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.size(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        AndroidStringText(
                            text = accountErrorText(error),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.account_refresh_failed_skip_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        PositionButton(
                            modifier = Modifier.fillMaxWidth(),
                            position = ButtonPosition.Top,
                            onClick = onRetry
                        ) {
                            MarqueeText(text = stringResource(R.string.account_refresh_failed_retry))
                        }
                        PositionFilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            position = ButtonPosition.Middle,
                            onClick = onSkip
                        ) {
                            MarqueeText(text = stringResource(R.string.account_refresh_failed_skip))
                        }
                        PositionFilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            position = ButtonPosition.Bottom,
                            onClick = onCancel
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 启动器背景图片/视频层
 */
@Composable
fun Background(
    viewModel: BackgroundViewModel,
    modifier: Modifier = Modifier,
    allowVideo: Boolean = true,
    videoOnSurface: Boolean = false
) {
    val blur = com.russoviller.cryogenic.ui.components.CryoLiquid.blurRadius
    val opacity = AllSettings.launcherBackgroundOpacity.state
    val backgroundMode = com.russoviller.cryogenic.ui.components.CryoLiquid.blurType == BackgroundBlur.Background
    val backgroundBlurEnabled = backgroundMode && blur > 0 && opacity < 100
    val layerCapture = !backgroundMode && blur > 0 && opacity < 100 && viewModel.isValid
    //Liquid Glass (Android 13+): o fundo é gravado para os vidros refratarem
    val liquidBackdrop = com.russoviller.cryogenic.ui.components.CryoBackdrop.supported &&
        com.russoviller.cryogenic.ui.components.CryoLiquid.on && viewModel.isValid
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .then(
                if (backgroundBlurEnabled && Build.VERSION.SDK_INT >= 31) {
                    Modifier.blur(blur.dp)
                } else {
                    Modifier
                }
            )
            .backgroundCapture(
                store = viewModel,
                recordContent = layerCapture,
                blurRadiusPx = blur * density.density,
                whiteOverlayAlpha = if (backgroundBlurEnabled) whiteOverlayAlpha(blur) else 0f
            )
            .then(
                if (liquidBackdrop) Modifier.cryoBackdropSource() else Modifier
            )
    ) {
        if (viewModel.isValid) {
            when {
                viewModel.isVideo && allowVideo -> {
                    //Sem desfoque no fundo, o vídeo vai para uma SurfaceView: ele não depende mais da
                    //thread da interface e não congela quando uma página está sendo montada.
                    //Com desfoque continua na TextureView (a SurfaceView não pode ser desfocada).
                    val onSurface = videoOnSurface && !backgroundBlurEnabled && !layerCapture && !liquidBackdrop
                    androidx.compose.runtime.key(onSurface) {
                        VideoPlayer(
                            videoUri = Uri.fromFile(viewModel.backgroundFile),
                            modifier = Modifier.fillMaxSize(),
                            refreshTrigger = viewModel.refreshTrigger,
                            volume = AllSettings.videoBackgroundVolume.state / 100f,
                            useSurfaceView = onSurface
                        )
                    }
                }
                viewModel.isImage -> {
                    val blurred = viewModel.blurredBackground
                    if (backgroundBlurEnabled && Build.VERSION.SDK_INT < 31 && blurred != null) {
                        Image(
                            bitmap = blurred,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        BackgroundImage(
                            modifier = Modifier.fillMaxSize(),
                            imageFile = viewModel.backgroundFile,
                            refreshTrigger = viewModel.refreshTrigger
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundImage(
    refreshTrigger: Any,
    imageFile: File,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    //Um único loader para todos os wallpapers, com cache em memória: trocar de opção ou de área
    //reaproveita o bitmap que já está na RAM em vez de decodificar a imagem de novo
    val imageLoader = remember { WallpaperImageLoader.get(context) }
    val request = remember(refreshTrigger, imageFile) {
        ImageRequest.Builder(context)
            .data(imageFile)
            .memoryCacheKeyExtra("cryo_generation", refreshTrigger.toString())
            .diskCachePolicy(CachePolicy.DISABLED)
            .allowHardware(false)
            .crossfade(false)
            .build()
    }

    //GIF 的动画帧由绘制 → invalidateSelf → 再绘制的自续循环推进，
    //任何一帧失效丢失都会让动画永久冻结，由帧时钟显式逐帧驱动重绘，保证循环自愈
    val isAnimatedState = remember(refreshTrigger) { mutableStateOf(false) }
    LaunchedEffect(refreshTrigger) {
        isAnimatedState.value = withContext(Dispatchers.IO) {
            imageFile.isGifFile()
        }
    }
    val isAnimated = isAnimatedState.value

    val frameTick = remember { mutableIntStateOf(0) }
    if (isAnimated) {
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        //recomeça o relógio dos quadros a cada recarga do wallpaper
        LaunchedEffect(lifecycleOwner, refreshTrigger) {
            //Só redesenha o GIF com o Launcher visível e fora do jogo: em segundo plano não gasta CPU/GPU
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    withFrameNanos { }
                    //Com o Launcher em primeiro plano (RESUMED) o GIF anda mesmo que a marcação de
                    //"jogo aberto" tenha ficado presa: antes ele congelava até reabrir o Launcher
                    val resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if (resumed || !com.russoviller.cryogenic.ui.components.CryoTheme.inGame) frameTick.intValue++
                    else kotlinx.coroutines.delay(500L)
                }
            }
        }
    }

    AsyncImage(
        modifier = modifier.then(
            if (isAnimated) {
                Modifier.drawBehind {
                    frameTick.intValue
                }
            } else {
                Modifier
            }
        ),
        model = request,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

/**
 * O loader dos wallpapers (fundo, cartão principal e 9:16). Guarda só esses poucos bitmaps, no
 * tamanho em que aparecem; o cache é solto quando o jogo abre (ZLApplication.onTrimMemory).
 */
object WallpaperImageLoader {
    @Volatile
    private var loader: ImageLoader? = null

    fun get(context: android.content.Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizeBytes(24L * 1024 * 1024)
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache { null }
            .diskCachePolicy(CachePolicy.DISABLED)
            .components {
                //GIF pelo decodificador de quadros (Movie), avançado pelo relógio de quadros do
                //BackgroundImage. O AnimatedImageDrawable (Android 9+) parava de animar sozinho quando
                //perdia uma invalidação, e o wallpaper congelava do nada.
                add(GifDecoder.Factory())
            }
            .build()
            .also { loader = it }
    }

    fun clearMemory() {
        runCatching { loader?.memoryCache?.clear() }
    }
}
