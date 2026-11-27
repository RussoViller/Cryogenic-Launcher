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

package com.russoviller.cryogenic.ui.base

import android.content.Intent
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.ui.activities.SplashActivity
import com.russoviller.cryogenic.ui.activities.VMActivity
import android.app.ActivityManager
import android.os.Bundle
import androidx.annotation.CallSuper
import com.russoviller.cryogenic.BuildKeys
import com.russoviller.cryogenic.context.refreshContext
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.setting.loadAllSettings
import com.russoviller.cryogenic.utils.checkStoragePermissionsForInit
import dagger.hilt.android.AndroidEntryPoint

/** Já perguntou do acesso a arquivos nesta execução (versão IS). */
private var internalStoragePrompted = false

@AndroidEntryPoint
open class BaseAppCompatActivity : FullScreenAppCompatActivity() {
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshContext(this)
        checkStoragePermissions()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        loadAllSettings(this, true)
        checkStoragePermissions()
        refreshTaskDescription()
        checkInternalStorageMode()
    }

    /**
     * Versão IS: pede o acesso a todos os arquivos; quando ele é dado com o launcher ainda usando a
     * pasta do app (Android/data), reinicia para tudo abrir já na pasta CryogenicLauncher.
     */
    private fun checkInternalStorageMode() {
        if (!PathManager.isInternalStorageBuild || this is VMActivity) return
        val granted = com.russoviller.cryogenic.utils.hasAllFilesAccess(this)
        if (granted && !PathManager.usingInternalStorage) {
            //só reinicia se a pasta do armazenamento interno puder mesmo ser usada (evita ciclo)
            refreshContext(this)
            if (PathManager.usingInternalStorage) restartLauncher()
        } else if (!granted && !internalStoragePrompted) {
            internalStoragePrompted = true
            com.russoviller.cryogenic.utils.checkStoragePermissions(
                activity = this,
                message = getString(R.string.cryo_is_permission),
                messageSdk30 = getString(R.string.cryo_is_permission)
            )
        }
    }

    private fun restartLauncher() {
        val intent = Intent(this, SplashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finishAffinity()
        android.os.Handler(mainLooper).postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 300)
    }

    /**
     * @return 该 Activity 在最近任务中显示的标题
     */
    protected open fun getTaskDescriptionTitle(): String? = null

    @Suppress("DEPRECATION")
    private fun refreshTaskDescription() {
        val title = getTaskDescriptionTitle() ?: BuildKeys.LAUNCHER_NAME
        setTaskDescription(ActivityManager.TaskDescription(title))
    }

    protected fun refreshData() {
        AccountsManager.reloadAccounts()
        AccountsManager.reloadAuthServers()
        GamePathManager.reloadPath()
    }

    private fun checkStoragePermissions() {
        //检查所有文件管理权限
        checkStoragePermissionsForInit(this)
        //Música: cria a pasta "Cryogenic Music" (com .nomedia) assim que houver permissão de arquivos
        if (com.russoviller.cryogenic.utils.hasStoragePermission) {
            com.russoviller.cryogenic.music.CryoMusicLibrary.ensureFolderAsync()
        }
    }

    protected fun runFinish() = run { finish() }
}
