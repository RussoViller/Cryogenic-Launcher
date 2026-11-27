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

package com.russoviller.cryogenic.game.launch

import com.russoviller.cryogenic.utils.logging.Logger
import android.content.Context
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.coroutine.Task
import com.russoviller.cryogenic.coroutine.TaskFlowExecutor
import com.russoviller.cryogenic.coroutine.TitledTask
import com.russoviller.cryogenic.coroutine.addTask
import com.russoviller.cryogenic.coroutine.buildPhase
import com.russoviller.cryogenic.game.account.Account
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.account.auth_server.AuthServerHelper
import com.russoviller.cryogenic.game.account.isLocalAccount
import com.russoviller.cryogenic.game.account.isMicrosoftAccount
import com.russoviller.cryogenic.game.account.isReloginRequired
import com.russoviller.cryogenic.game.account.microsoft.validateAccessToken
import com.russoviller.cryogenic.game.account.refreshMicrosoft
import com.russoviller.cryogenic.game.download.game.GameLibDownloader
import com.russoviller.cryogenic.game.support.lwjgl3ify.patchLwjgl3ifyIfNeeded
import com.russoviller.cryogenic.game.version.download.BaseMinecraftDownloader
import com.russoviller.cryogenic.game.version.download.DownloadMode
import com.russoviller.cryogenic.game.version.download.MinecraftDownloader
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionFolders
import com.russoviller.cryogenic.game.version.installed.VersionInfoParser
import com.russoviller.cryogenic.game.version.mod.AllModReader
import com.russoviller.cryogenic.game.version.mod.isEnabled
import com.russoviller.cryogenic.game.versioninfo.models.GameManifest
import com.russoviller.cryogenic.ui.activities.runGame
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.utils.GSON
import com.russoviller.cryogenic.utils.network.isNetworkAvailable
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow

/** 账号凭据已被服务端拒绝 */
private class LaunchReloginRequired(
    val account: Account
) : RuntimeException()

/** 账号校验或刷新失败时抛出 */
private class LaunchCheckFailed(
    val account: Account,
    cause: Throwable
) : RuntimeException(cause)


/** 自带 FPE 模拟的模组，与 SFPEW 兼容层冲突 */
private val FPE_EMULATOR_MOD_IDS = setOf("angelica", "actinium")

/** Mods que leem a música tocando no sistema (MPRIS) e precisam do acesso às notificações */
private val MEDIA_MOD_IDS = setOf("showthelyrics")

/**
 * 游戏启动器
 */
class GameLaunchFlow(scope: CoroutineScope) {
    private val taskExecutor = TaskFlowExecutor(scope)
    val tasksFlow: StateFlow<List<TitledTask>> = taskExecutor.tasksFlow

    /**
     * 启动游戏
     * @param version 指定版本
     * @param skipAccountRefresh 跳过启动前的账号校验，直接使用现有凭据
     */
    fun launch(
        context: Context,
        version: Version,
        skipAccountRefresh: Boolean = false,
        exitActivity: () -> Unit,
        onReloginRequired: (Account) -> Unit = {},
        onRefreshFailed: (Account, Throwable) -> Unit = { _, _ -> },
        isRunning: () -> Unit = {},
        onComplete: () -> Unit,
        onError: (ErrorViewModel.ThrowableMessage) -> Unit,
    ) {
        if (taskExecutor.isRunning()) {
            //正在启动中，阻止这次启动请求
            isRunning()
            return
        }

        val account = AccountsManager.currentAccountFlow.value ?: return
        var errorReported = false
        val handleError: (ErrorViewModel.ThrowableMessage) -> Unit = { message ->
            if (!errorReported) {
                errorReported = true
                onError(message)
            }
        }

        taskExecutor.executePhasesAsync(
            onStart = {
                taskExecutor.addPhases(
                    listOf(
                        buildLaunchPhases(
                            context = context,
                            version = version,
                            account = account,
                            skipAccountRefresh = skipAccountRefresh,
                            exitActivity = exitActivity,
                            onError = handleError
                        )
                    )
                )
            },
            onComplete = onComplete,
            onError = { th ->
                when (th) {
                    is LaunchReloginRequired -> onReloginRequired(th.account)
                    is LaunchCheckFailed -> onRefreshFailed(th.account, th.cause ?: th)
                    else -> handleError(
                        ErrorViewModel.ThrowableMessage(
                            title = androidText(R.string.generic_error),
                            message = androidText(th.getMessageOrToString())
                        )
                    )
                }
            },
        )
    }

    /**
     * 取消当前的启动流程
     */
    fun cancel() {
        taskExecutor.cancel()
    }

    private fun buildLaunchPhases(
        context: Context,
        version: Version,
        account: Account,
        skipAccountRefresh: Boolean,
        exitActivity: () -> Unit,
        onError: (ErrorViewModel.ThrowableMessage) -> Unit
    ): TaskFlowExecutor.TaskPhase {
        //检查是否联网，根据这个条件决定是否校验账号
        //以及，没有联网时，让微软账号、外置账号作为离线账号登录
        val hasNetwork = isNetworkAvailable(context)
        if (!hasNetwork && !account.isLocalAccount()) {
            version.offlineAccountLogin = true
        }

        return buildPhase {
            var launchManifest: GameManifest? = null

            if (hasNetwork && !skipAccountRefresh && AccountsManager.isLaunchCheckNeeded(account)) {
                //账号管理页正在刷新该账号时，直接使用现有凭据启动
                addTask(
                    icon = R.drawable.ic_login,
                    title = androidText(R.string.account_logging_in, account.username),
                    dispatcher = Dispatchers.IO
                ) { task ->
                    try {
                        if (account.isMicrosoftAccount()) {
                            checkMicrosoftAccount(task, account)
                        } else {
                            checkOtherAccount(task, context, account)
                        }
                        AccountsManager.markSessionValidated(account)
                        AccountsManager.suspendSaveAccount(account)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        if (e.isReloginRequired()) throw LaunchReloginRequired(account)
                        throw LaunchCheckFailed(account, e)
                    }
                }
            }

            //扫描模组列表，开启对应功能
            addTask(
                icon = R.drawable.ic_extension_outlined,
                title = androidText(R.string.launch_check_mods),
                dispatcher = Dispatchers.IO
            ) { task ->
                val patchedManifest = checkMods(version)
                val manifest = patchedManifest ?: VersionInfoParser(version)
                    .setInheriting(skipIfNotExists = true)
                    .build()
                val manifestString = GSON.toJson(manifest)

                launchManifest = manifest
                version.launchManifest = manifestString

                // 如果打了补丁，此处需要重新检索一下依赖库并下载
                patchedManifest?.let {
                    val libDownloader = GameLibDownloader(
                        downloader = BaseMinecraftDownloader(version.getGameHome()),
                        gameJson = manifestString
                    )
                    libDownloader.schedule(task)
                    libDownloader.download(task)
                }
            }

            if (!version.skipGameIntegrityCheck()) {
                //校验并修复游戏文件
                addTask(
                    icon = R.drawable.ic_assignment_filled,
                    title = androidText(R.string.minecraft_download_stat_verify_task),
                    task = createGameDownloadTask(
                        context = context,
                        version = version,
                        onError = onError
                    )
                )
            }

            //启动游戏
            addTask(
                icon = R.drawable.ic_rocket_launch_filled,
                title = androidText(R.string.main_launch_game)
            ) { _ ->
                launchManifest?.let { manifest ->
                    version.launchManifest = GSON.toJson(
                        VersionInfoParser(version)
                            .setManifest(manifest)
                            .setInheriting()
                            .build()
                    )
                }
                runGame(context, version, account)
                exitActivity()
            }
        }
    }

    /**
     * 微软账号：向服务端校验缓存的凭据，被拒绝或已临近过期时静默刷新
     */
    private suspend fun checkMicrosoftAccount(task: Task, account: Account) {
        val expired = System.currentTimeMillis() > account.expiresAt - 5 * 60 * 1000
        if (expired || !validateAccessToken(account)) {
            account.refreshMicrosoft(task, currentCoroutineContext())
        }
    }

    /**
     * 外置账号：依次尝试 validate 与 refresh，均被服务端拒绝时再用账号密码重新登录
     */
    private suspend fun checkOtherAccount(task: Task, context: Context, account: Account) {
        task.updateMessage(androidText(R.string.account_logging_in, account.username))
        val helper = AuthServerHelper(
            baseUrl = account.otherBaseUrl!!,
            serverName = account.accountType!!,
            email = account.otherAccount!!,
            password = account.otherPassword!!
        )
        if (!helper.validateOrRefresh(context, account)) {
            helper.passwordLogin(context, account)
        }
    }

    private fun createGameDownloadTask(
        context: Context,
        version: Version,
        onError: (ErrorViewModel.ThrowableMessage) -> Unit
    ): Task {
        return MinecraftDownloader(
            context = context,
            version = version.getVersionInfo()?.minecraftVersion ?: version.getVersionName(),
            customName = version.getVersionName(),
            gameHome = version.getGameHome(),
            mode = DownloadMode.VERIFY_AND_REPAIR,
            onError = { message ->
                onError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.minecraft_download_failed),
                        message = androidText(message)
                    )
                )
            }
        ).getDownloadTask()
    }

    /**
     * 扫描模组列表并开启对应功能
     * @return 经过补丁的游戏清单，为 null 则表示没什么补丁
     */
    private suspend fun checkMods(version: Version): GameManifest? {
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        val mods = AllModReader(modsDir).readAllLocals()

        //模组自带 FPE 模拟时 SFPEW 兼容层会冲突，启动时自动关闭
        val fpeEmulatorIds = mods.filter { it.file.isEnabled() && it.id in FPE_EMULATOR_MOD_IDS }.map { it.id }
        version.fpeEmulatorModDetected = fpeEmulatorIds.isNotEmpty()
        if (fpeEmulatorIds.isNotEmpty()) {
            Logger.info("GameLaunchFlow", "FPE emulator mod detected (${fpeEmulatorIds.distinct().joinToString()}), SFPEW layer disabled")
        }

        version.mediaModDetected = mods.any { it.file.isEnabled() && it.id in MEDIA_MOD_IDS }

        if (mods.any { it.id == "touchcontroller" && it.file.isEnabled() }) {
            version.enableTouchProxy = true
        }

        val patched = patchLwjgl3ifyIfNeeded(version, mods)
        return patched
    }
}
