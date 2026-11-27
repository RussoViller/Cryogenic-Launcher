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

package com.russoviller.cryogenic

import com.russoviller.cryogenic.setting.enums.applyLanguage
import android.app.Application
import android.content.res.Configuration
import android.os.Process
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.kyant.fishnet.Fishnet
import com.russoviller.cryogenic.context.GlobalContext
import com.russoviller.cryogenic.context.refreshContext
import com.russoviller.cryogenic.coroutine.TaskSystem
import com.russoviller.cryogenic.game.account.AccountsManager
import com.russoviller.cryogenic.game.path.GamePathManager
import com.russoviller.cryogenic.keepalive.TaskKeepAlive
import com.russoviller.cryogenic.path.IMAGE_LOADER_CLIENT
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.loadAllSettings
import com.russoviller.cryogenic.ui.activities.showFatalError
import com.russoviller.cryogenic.ui.activities.showLauncherCrash
import com.russoviller.cryogenic.utils.device.Architecture
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.network.TLSSecurity
import com.russoviller.cryogenic.utils.writeCrashFile
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath
import kotlin.properties.Delegates

@HiltAndroidApp
class ZLApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        @JvmStatic
        var DEVICE_ARCHITECTURE by Delegates.notNull<Int>()
    }

    override fun onCreate() {
        GlobalContext = this
        TLSSecurity.install()
        refreshContext(this)
        //初始化任务保活控制器，需在任何任务开始前完成初始化
        TaskKeepAlive.initialize(this)
        //música: marca uma nova abertura do Launcher (só no processo principal)
        runCatching { com.russoviller.cryogenic.music.CryoMusicSession.onApplicationCreate(this) }

        Thread.setDefaultUncaughtExceptionHandler { _, th ->
            //停止所有任务
            TaskSystem.stopAll()

            val throwable = if (th is SplashException) th.cause!!
            else th

            Logger.error("Startup", "An exception occurred", throwable)

            writeCrashFile(
                file = PathManager.FILE_CRASH_REPORT,
                throwable = throwable
            ) { t ->
                Logger.error("AppCrash", "An exception occurred while saving the crash report", t)
            }

            showLauncherCrash(this@ZLApplication, throwable, th !is SplashException)
            Process.killProcess(Process.myPid())
        }

        super.onCreate()
        runCatching {
            Fishnet.init(this, PathManager.DIR_NATIVE_LOGS.absolutePath)

            MMKV.initialize(this)
            loadAllSettings(this)
            applyLanguage(com.russoviller.cryogenic.setting.AllSettings.launcherLanguage.getValue())

            Logger.initialize(this)

            initializeData()
            //Artes grandes do APK já decodificadas antes da primeira tela
            com.russoviller.cryogenic.ui.components.CryoArtCache.prewarm(this)
            //Restos das versões de teste da Área Rust (removida): arquivos do jogo e a sessão
            //salva ficavam em files/rust; a pasta é apagada uma vez, em segundo plano
            java.io.File(filesDir, "rust").takeIf { it.exists() }?.let { leftover ->
                Thread({ runCatching { leftover.deleteRecursively() } }, "cryo-cleanup").start()
            }
            PathManager.DIR_FILES_PRIVATE = getDir("files", MODE_PRIVATE)
            DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            //Force x86 lib directory for Asus x86 based zenfones
            if (Architecture.isx86Device() && Architecture.is32BitsDevice) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir
                applicationInfo.nativeLibraryDir = originalJNIDirectory.take(originalJNIDirectory.lastIndexOf("/")) + "/x86"
            }
        }.onFailure { launchTh ->
            writeCrashFile(
                file = PathManager.FILE_CRASH_REPORT,
                throwable = launchTh
            ) {
                Log.w("ZLApplication", "An exception occurred while saving the crash report", it)
            }
            showFatalError(this, launchTh)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        //Launcher fora da tela (ex.: jogo aberto) ou memória apertada: solta os bitmaps em cache
        //para deixar mais RAM livre para o jogo
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            runCatching { SingletonImageLoader.get(this).memoryCache?.clear() }
            com.russoviller.cryogenic.ui.components.CryoArtCache.clear()
            com.russoviller.cryogenic.ui.screens.content.elements.WallpaperImageLoader.clearMemory()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshContext(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(20L * 1024 * 1024) // 20MB
                    .weakReferencesEnabled(true) //弱引用
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .directory(PathManager.DIR_IMAGE_CACHE.toOkioPath())
                    .build()
            }
            .components {
                add(KtorNetworkFetcherFactory(IMAGE_LOADER_CLIENT))
                if (android.os.Build.VERSION.SDK_INT >= 28) add(coil3.gif.AnimatedImageDecoder.Factory())
                add(GifDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    private fun initializeData() {
        AccountsManager.initialize(this)
        GamePathManager.initialize(this)
    }
}
