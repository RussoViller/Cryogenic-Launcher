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

package com.russoviller.cryogenic.ui.screens.main

import com.russoviller.cryogenic.ui.components.cryoShockwave
import com.russoviller.cryogenic.ui.components.cryoBackground

import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoGradient
import androidx.compose.foundation.shape.CircleShape
import com.russoviller.cryogenic.ui.components.cryoGlassShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.CryoWallpaper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.ui.screens.content.FirstLoginMenu
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.ui.screens.content.elements.PlayerFace
import com.russoviller.cryogenic.ui.theme.cryoShardShape
import com.russoviller.cryogenic.ui.components.CryoAnimatedTitle
import com.russoviller.cryogenic.ui.components.CryoNavEntry
import com.russoviller.cryogenic.ui.components.CryoSidebar
import com.russoviller.cryogenic.ui.components.cryoFrostEdge
import com.russoviller.cryogenic.ui.components.CryoBackdrop
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.russoviller.cryogenic.BuildKeys
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.russoviller.cryogenic.discord.CryoDiscordPresence
import com.russoviller.cryogenic.ui.components.CryoIdleMode
import com.russoviller.cryogenic.ui.components.cryoGlow
import kotlinx.coroutines.delay
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionsManager
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.AndroidStringText
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.components.BackgroundCard
import com.russoviller.cryogenic.ui.components.CardTitleLayout
import com.russoviller.cryogenic.ui.components.TextRailItem
import com.russoviller.cryogenic.ui.guide.sendStartGuideOnce
import com.russoviller.cryogenic.ui.screens.BackStackNavKey
import com.russoviller.cryogenic.ui.screens.NavStateHost
import com.russoviller.cryogenic.ui.screens.NestedNavDisplay
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.AccountManageScreen
import com.russoviller.cryogenic.ui.screens.content.DownloadScreen
import com.russoviller.cryogenic.ui.screens.content.FileSelectorScreen
import com.russoviller.cryogenic.ui.screens.content.LauncherScreen
import com.russoviller.cryogenic.ui.screens.content.LicenseScreen
import com.russoviller.cryogenic.ui.screens.content.LogViewScreen
import com.russoviller.cryogenic.ui.screens.content.SettingsScreen
import com.russoviller.cryogenic.ui.screens.content.VersionExportScreen
import com.russoviller.cryogenic.ui.screens.content.VersionSettingsScreen
import com.russoviller.cryogenic.ui.screens.content.VersionsManageScreen
import com.russoviller.cryogenic.ui.screens.content.WebViewScreen
import com.russoviller.cryogenic.ui.screens.content.assetinfo.AssetInfoScreen
import com.russoviller.cryogenic.ui.screens.content.navigateToDownload
import com.russoviller.cryogenic.ui.screens.navigateTo
import com.russoviller.cryogenic.ui.screens.onBack
import com.russoviller.cryogenic.ui.screens.rememberTransitionSpec
import com.russoviller.cryogenic.ui.theme.backgroundColor
import com.russoviller.cryogenic.ui.theme.cardColor
import com.russoviller.cryogenic.ui.theme.onBackgroundColor
import com.russoviller.cryogenic.ui.theme.onCardColor
import com.russoviller.cryogenic.utils.animation.getAnimateTween
import com.russoviller.cryogenic.utils.file.formatFileSize
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel
import com.russoviller.cryogenic.viewmodel.ModifyVersionViewModel
import com.russoviller.cryogenic.viewmodel.ModpackImportViewModel
import com.russoviller.cryogenic.viewmodel.ScreenBackStackViewModel
import com.russoviller.cryogenic.viewmodel.sendKeepScreen
import com.russoviller.cryogenic.viewmodel.sendToast

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    //监控当前是否有任务正在进行
    var taskRunning by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val noTasks = tasks.isEmpty()
            eventViewModel.sendKeepScreen(!noTasks)
            taskRunning = noTasks
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f

    val backgroundColor = if (isBackgroundValid) {
        backgroundColor().copy(alpha = launcherBackgroundOpacity)
    } else backgroundColor()

    val inDownloadScreen = mainScreenKey is NestedNavKey.Download
    val inSettingsScreen = mainScreenKey is NestedNavKey.Settings
    val inVersionsScreen = mainScreenKey is NormalNavKey.VersionsManager || mainScreenKey is NestedNavKey.VersionSettings
    val inAccountsScreen = mainScreenKey is NormalNavKey.AccountManager
    val inWebPageScreen = mainScreenKey is NormalNavKey.RvWebPage
    val inDonationScreen = mainScreenKey is NormalNavKey.Donation
    val inChunksScreen = mainScreenKey is NormalNavKey.ChunkEraser
    val inChunkBaseScreen = mainScreenKey is NormalNavKey.ChunkBase
    val inLiveScreen = mainScreenKey is NormalNavKey.TikTokLive
    val inCryoModsScreen = mainScreenKey is NormalNavKey.CryoMods

    val toSettingsScreen: () -> Unit = {
        screenBackStackModel.mainScreen.removeAndNavigateTo(
            removes = screenBackStackModel.clearBeforeNavKeys,
            screenKey = screenBackStackModel.settingsScreen
        )
    }
    val toVersionsScreen: () -> Unit = {
        screenBackStackModel.mainScreen.removeAndNavigateTo(
            remove = NestedNavKey.VersionSettings::class,
            screenKey = NormalNavKey.VersionsManager
        )
    }
    val toAccountsScreen: () -> Unit = {
        screenBackStackModel.mainScreen.navigateTo(
            screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
        )
    }
    val openFileManager: () -> Unit = {
        eventViewModel.sendEvent(
            EventViewModel.Event.OpenFileManager(
                rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
            )
        )
    }

    // ---- Modo inativo ----
    val idle = CryoIdleMode.active
    val toggleIdle: () -> Unit = {
        if (CryoIdleMode.active) {
            CryoIdleMode.touch()
            CryoIdleMode.active = false
        } else {
            //Em outra tela: volta para a tela inicial antes de ficar inativo
            if (!inLauncherScreen) toMainScreen()
            CryoIdleMode.active = true
        }
    }
    //O modo inativo só existe na tela inicial
    LaunchedEffect(inLauncherScreen) {
        if (!inLauncherScreen) CryoIdleMode.active = false
    }
    BackHandler(enabled = idle) {
        CryoIdleMode.touch()
        CryoIdleMode.active = false
    }
    //Entrada automática depois de um tempo sem toque na tela inicial
    val idleAutoEnabled = AllSettings.cryoIdleAutoEnabled.state
    val idleTimeoutSeconds = AllSettings.cryoIdleTimeoutSeconds.state
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(inLauncherScreen, idle, idleAutoEnabled, idleTimeoutSeconds, lifecycleOwner) {
        if (!idleAutoEnabled || !inLauncherScreen || idle) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            CryoIdleMode.touch()
            val timeoutMs = CryoIdleMode.clampSeconds(idleTimeoutSeconds) * 1000L
            while (true) {
                val remaining = timeoutMs - (SystemClock.elapsedRealtime() - CryoIdleMode.lastInteraction)
                if (remaining <= 0L) {
                    CryoIdleMode.active = true
                    break
                }
                delay(remaining.coerceAtMost(1000L))
            }
        }
    }
    //Discord: mostra quando o usuário está inativo
    val presenceContext = LocalContext.current
    val presenceDetails = stringResource(R.string.cryo_discord_in_launcher)
    //Discord: mostra a seção do Launcher aberta agora (ou "Inativo")
    val presenceState = stringResource(
        when {
            idle -> R.string.cryo_discord_state_idle
            inLauncherScreen -> R.string.cryo_discord_state_home
            mainScreenKey is NestedNavKey.VersionSettings -> R.string.cryo_discord_state_version_settings
            inVersionsScreen -> R.string.cryo_discord_state_versions
            inDownloadScreen -> R.string.cryo_discord_state_download
            inAccountsScreen -> R.string.cryo_discord_state_accounts
            inSettingsScreen -> R.string.cryo_discord_state_settings
            inWebPageScreen -> R.string.cryo_discord_state_webpage
            inDonationScreen -> R.string.cryo_discord_state_donate
            inChunksScreen -> R.string.cryo_discord_state_chunks
            inChunkBaseScreen -> R.string.cryo_discord_state_chunkbase
            inLiveScreen -> R.string.cryo_discord_state_live
            else -> R.string.cryo_discord_state_menu
        }
    )
    LaunchedEffect(idle, presenceDetails, presenceState) {
        CryoDiscordPresence.inLauncher(presenceContext, presenceDetails, presenceState)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                //Só observa os toques (sem consumir) para zerar o contador de inatividade
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        CryoIdleMode.touch()
                    }
                }
            }
    ) {
        if (!isBackgroundValid) {
            CryoWallpaper(modifier = Modifier.fillMaxSize(), backdrop = true)
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                //onda de vidro da cutscene do Liquid Glass passando pela interface (Android 13+)
                .cryoShockwave(
                    radiusFraction = { com.russoviller.cryogenic.ui.components.CryoInterface2.revealFraction() },
                    amplitude = { com.russoviller.cryogenic.ui.components.CryoInterface2.waveAmplitude() }
                ),
            color = Color.Transparent,
            contentColor = onBackgroundColor()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = !idle,
                    enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                ) {
                    CryoSidebar(
                        entries = listOf(
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_home_filled),
                                label = stringResource(R.string.cryo_nav_home),
                                selected = inLauncherScreen,
                                onClick = { if (!inLauncherScreen) toMainScreen() }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_videogame_asset_outlined),
                                label = stringResource(R.string.cryo_nav_versions),
                                selected = inVersionsScreen,
                                onClick = { if (!inVersionsScreen) toVersionsScreen() }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_download_2_filled),
                                label = stringResource(R.string.cryo_nav_download),
                                selected = inDownloadScreen,
                                onClick = { if (!inDownloadScreen) screenBackStackModel.navigateToDownload(screenBackStackModel.downloadGameScreen) }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_person_outlined),
                                label = stringResource(R.string.cryo_nav_accounts),
                                selected = inAccountsScreen,
                                onClick = { if (!inAccountsScreen) toAccountsScreen() }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_settings_filled),
                                label = stringResource(R.string.cryo_nav_settings),
                                selected = inSettingsScreen,
                                onClick = { if (!inSettingsScreen) toSettingsScreen() }
                            )
                        ),
                        bottomEntries = listOf(
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_public),
                                label = stringResource(R.string.cryo_nav_webpage),
                                selected = inWebPageScreen,
                                onClick = {
                                    if (!inWebPageScreen) {
                                        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.RvWebPage)
                                    }
                                }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_favorite_filled),
                                label = stringResource(R.string.cryo_nav_donate),
                                selected = inDonationScreen,
                                onClick = {
                                    if (!inDonationScreen) {
                                        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.Donation)
                                    }
                                }
                            ),
                            //CryoMods: funções adicionadas pelos Mods do Launcher (.cmod)
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_extension_outlined),
                                label = stringResource(R.string.cryo_cmods_title),
                                selected = inCryoModsScreen,
                                onClick = {
                                    if (!inCryoModsScreen) {
                                        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.CryoMods)
                                    }
                                }
                            )
                        ),
                        footer = BuildConfig.VERSION_NAME,
                        moreEntries = listOf(
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_cryo_stream),
                                label = stringResource(R.string.cryo_nav_live),
                                selected = inLiveScreen,
                                onClick = {
                                    if (!inLiveScreen) {
                                        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.TikTokLive)
                                    }
                                }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_cryo_chunks),
                                label = stringResource(R.string.cryo_nav_chunks),
                                selected = inChunksScreen,
                                onClick = {
                                    if (!inChunksScreen) {
                                        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.ChunkEraser)
                                    }
                                }
                            ),
                            CryoNavEntry(
                                icon = painterResource(R.drawable.ic_public),
                                label = stringResource(R.string.cryo_nav_chunkbase),
                                selected = inChunkBaseScreen,
                                onClick = {
                                    if (!inChunkBaseScreen) {
                                        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.ChunkBase)
                                    }
                                }
                            )
                        ),
                        moreLabel = stringResource(R.string.cryo_nav_more),
                        backLabel = stringResource(R.string.cryo_nav_back)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val bannerContext = androidx.compose.ui.platform.LocalContext.current
                    com.russoviller.cryogenic.legal.CryoUnofficialBanner(
                        openLink = { url ->
                            runCatching {
                                bannerContext.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                    TopBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        mainScreenKey = mainScreenKey,
                        inLauncherScreen = inLauncherScreen,
                        taskRunning = taskRunning,
                        isTasksExpanded = isTaskMenuExpanded,
                        contentColor = onBackgroundColor(),
                        onScreenBack = {
                            screenBackStackModel.mainScreen.backStack.removeFirstOrNull()
                        },
                        toAccountsScreen = toAccountsScreen,
                        openFileManager = openFileManager,
                        changeExpandedState = {
                            changeTasksExpandedState()
                        },
                        idle = idle,
                        onToggleIdle = toggleIdle,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                            NavigationUI(
                                modifier = Modifier.fillMaxSize(),
                                screenBackStackModel = screenBackStackModel,
                                toMainScreen = toMainScreen,
                                eventViewModel = eventViewModel,
                                modpackImportViewModel = modpackImportViewModel,
                                modifyVersionViewModel = modifyVersionViewModel,
                                submitError = submitError
                            )
                        }

                        TaskMenu(
                            isExpanded = isTaskMenuExpanded && !idle,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.3f)
                                .align(Alignment.CenterStart)
                                .padding(all = 6.dp)
                        ) {
                            changeTasksExpandedState()
                        }
                    }
                }
            }
        }

        //Cutscene da Interface 2.0: por cima de qualquer tela do Launcher
        com.russoviller.cryogenic.ui.components.Interface2Cutscene(modifier = Modifier.fillMaxSize())

        com.russoviller.cryogenic.sponsor.CryoSponsorsHost(
            onSupport = {
                if (!inDonationScreen) {
                    screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.Donation)
                }
            }
        )
    }
}

@Composable
private fun <E: TitledNavKey> TopBar(
    mainScreenKey: E?,
    inLauncherScreen: Boolean,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    modifier: Modifier = Modifier,
    contentColor: Color,
    onScreenBack: () -> Unit,
    toAccountsScreen: () -> Unit,
    openFileManager: () -> Unit,
    changeExpandedState: () -> Unit,
    idle: Boolean,
    onToggleIdle: () -> Unit,
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        Row(
            modifier = modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            val parentRes = mainScreenKey?.title
            val childRes = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title

            AnimatedVisibility(
                visible = !idle,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .cryoGlass(shape = cryoGlassShape(19.dp), glow = false, cornerRadius = 19.dp)
                    .padding(start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .cryoBackground(cryoGradient())
                        .clickable(enabled = !inLauncherScreen) {
                            backDispatcher?.onBackPressed() ?: run { onScreenBack() }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(if (inLauncherScreen) R.drawable.ic_cryo_snowflake else R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.generic_back),
                        tint = CryoGlassColors.Deep
                    )
                }
                Spacer(Modifier.width(10.dp))
                Crossfade(targetState = parentRes to childRes) { (parent, child) ->
                    val style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                    if (parent == null) {
                        Text(
                            text = stringResource(R.string.cryo_nav_home),
                            style = style,
                            color = Color.White
                        )
                    } else {
                        val titleText = if (child != null) {
                            androidText(parent, androidText("  ›  "), child)
                        } else {
                            parent
                        }
                        AndroidStringText(
                            text = titleText,
                            style = style.copy(color = Color.White),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
            }
            //Modo Descanso: no lugar do título, o botão de Temas rápidos (predefinições salvas)
            AnimatedVisibility(
                visible = idle,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                com.russoviller.cryogenic.ui.screens.content.extras.QuickThemesButton()
            }
            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = !(isTasksExpanded || taskRunning) && !idle,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(shape = MaterialTheme.shapes.large)
                        .clickable { changeExpandedState() }
                        .padding(all = 8.dp)
                        .width(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_assignment_filled),
                        contentDescription = stringResource(R.string.main_task_menu)
                    )
                }
            }

            if (!idle) {
                com.russoviller.cryogenic.ui.components.Interface2Button(modifier = Modifier.padding(end = 8.dp))
            }
            IdleToggleButton(idle = idle, onClick = onToggleIdle)

            AnimatedVisibility(
                visible = !idle,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .height(38.dp)
                    .cryoGlass(shape = cryoGlassShape(19.dp), glow = false, cornerRadius = 19.dp)
                    .clickable(onClick = openFileManager)
                    .padding(start = 4.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .cryoBackground(cryoGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(17.dp),
                        painter = painterResource(R.drawable.ic_folder_filled),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Text(
                    text = stringResource(R.string.cryo_nav_files),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .cryoGlass(shape = cryoGlassShape(19.dp), glow = false)
                    .clickable(onClick = toAccountsScreen)
                    .padding(start = 4.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val current = account
                if (current != null) {
                    PlayerFace(account = current, avatarSize = 26.dp)
                } else {
                    Icon(
                        modifier = Modifier.padding(start = 4.dp).size(20.dp),
                        painter = painterResource(R.drawable.ic_person_outlined),
                        contentDescription = null
                    )
                }
                Text(
                    text = current?.username ?: stringResource(R.string.account_add_new_account),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            }
            }
        }
    }
}

/**
 * Botão do olho: liga/desliga o modo inativo. No modo inativo vira uma pílula com o aviso de como sair.
 */
@Composable
private fun IdleToggleButton(idle: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (idle) R.string.cryo_idle_exit else R.string.cryo_idle_enter)
    Row(
        modifier = Modifier
            .height(38.dp)
            .then(if (idle) Modifier.cryoGlow(19.dp, selected = true, strength = 0.8f) else Modifier)
            .cryoGlass(shape = cryoGlassShape(19.dp), glow = false, cornerRadius = 19.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .animateContentSize()
            .padding(start = 4.dp, end = if (idle) 14.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(if (idle) R.drawable.ic_visibility_off_outlined else R.drawable.ic_visibility_outlined),
                contentDescription = label,
                tint = CryoGlassColors.Deep
            )
        }
        if (idle) {
            Text(
                text = stringResource(R.string.cryo_idle_badge),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        onLaunchGame = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onQuickPlayServer = { version, address ->
                            val target = (version ?: VersionsManager.currentVersion.value)?.takeIf { it.isValid() }
                            if (target != null) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.PlayServer(target, address)
                                )
                            } else {
                                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.VersionsManager)
                            }
                        },
                        onOpenLink = {
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                        },
                        startGuideOnce = { keys ->
                            eventViewModel.sendStartGuideOnce(keys)
                        },
                        openFileManager = {
                            eventViewModel.sendEvent(
                                EventViewModel.Event.OpenFileManager(
                                    rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                                )
                            )
                        }
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.ChunkEraser> { key ->
                    com.russoviller.cryogenic.ui.screens.content.chunks.ChunkEraserScreen(
                        key = key,
                        currentKey = screenBackStackModel.mainScreen.currentKey
                    )
                }
                entry<NormalNavKey.CryoMods> { key ->
                    com.russoviller.cryogenic.cmod.CryoModsScreen(
                        key = key,
                        currentKey = screenBackStackModel.mainScreen.currentKey,
                        openLauncherMods = {
                            screenBackStackModel.settingsScreen.clearWith(NormalNavKey.Settings.LauncherExtras)
                            screenBackStackModel.mainScreen.removeAndNavigateTo(
                                removes = screenBackStackModel.clearBeforeNavKeys,
                                screenKey = screenBackStackModel.settingsScreen
                            )
                        }
                    )
                }
                entry<NormalNavKey.TikTokLive> { key ->
                    com.russoviller.cryogenic.live.TikTokLiveScreen(
                        key = key,
                        currentKey = screenBackStackModel.mainScreen.currentKey
                    )
                }
                entry<NormalNavKey.ChunkBase> { key ->
                    com.russoviller.cryogenic.ui.screens.content.chunks.ChunkBaseScreen(
                        key = key,
                        currentKey = screenBackStackModel.mainScreen.currentKey
                    )
                }
                entry<NormalNavKey.Donation> { key ->
                    com.russoviller.cryogenic.ui.screens.content.web.DonationScreen(
                        key = key,
                        currentKey = screenBackStackModel.mainScreen.currentKey
                    )
                }
                entry<NormalNavKey.RvWebPage> { key ->
                    com.russoviller.cryogenic.ui.screens.content.web.RvWebPageScreen(
                        key = key,
                        currentKey = screenBackStackModel.mainScreen.currentKey
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        modifyViewModel = modifyVersionViewModel,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.AssetInfo> { key ->
                    AssetInfoScreen(
                        key = key,
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        assetInfoScreenKey = key.currentKey,
                        eventViewModel = eventViewModel,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
