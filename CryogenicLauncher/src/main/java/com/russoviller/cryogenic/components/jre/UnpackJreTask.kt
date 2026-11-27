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

package com.russoviller.cryogenic.components.jre

import android.content.Context
import android.content.res.AssetManager
import com.russoviller.cryogenic.ZLApplication
import com.russoviller.cryogenic.components.AbstractUnpackTask
import com.russoviller.cryogenic.components.InstallableItem
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.multirt.RuntimesManager
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.utils.device.Architecture
import com.russoviller.cryogenic.utils.file.readString
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import java.io.IOException

private const val TAG = "UnpackJreTask"

/** Quantas vezes a extração é tentada de novo, do zero, se os dados vierem corrompidos. */
private const val MAX_ATTEMPTS = 3

/** Os .tar.xz do Java ficam entre 3x e 5,4x maiores ao extrair: 6x dá margem. */
private const val EXTRACT_RATIO = 6L
private const val EXTRA_SPACE = 64L * 1024 * 1024

/** Espaço livre insuficiente para extrair (não adianta tentar de novo). */
class UnpackSpaceException(message: String) : IOException(message)

class UnpackJreTask(
    private val context: Context,
    private val jre: Jre
) : AbstractUnpackTask() {
    private lateinit var assetManager: AssetManager
    private lateinit var launcherRuntimeVersion: String
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            assetManager = context.assets
            if (!isJreArchSupported()) {
                //当前设备架构不支持使用这个环境
                isCheckFailed = true
                return@runCatching
            }
            launcherRuntimeVersion = assetManager.open(jre.jrePath + "/version").readString()
        }.onFailure { e ->
            Logger.warning(TAG, "Failed to init jre version. assetsPath=${jre.jrePath}/version", e)
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun checkState(): InstallableItem.State {
        if (isCheckFailed) return InstallableItem.State.NOT_EXISTS

        return runCatching {
            val installedRuntimeVersion = RuntimesManager.loadInternalRuntimeVersion(jre.jreName)
            when {
                //未安装该环境
                installedRuntimeVersion == null -> InstallableItem.State.NOT_STARTED
                //instalação incompleta ou danificada: refaz em vez de reaproveitar
                !RuntimesManager.isRuntimeComplete(jre.jreName) -> InstallableItem.State.NOT_STARTED
                launcherRuntimeVersion != installedRuntimeVersion -> InstallableItem.State.PENDING
                else -> InstallableItem.State.FINISHED
            }
        }.onFailure { e ->
            Logger.error("CheckJre", "An exception occurred while detecting the Java Runtime.", e)
        }.getOrElse {
            //检查失败，要求重新进行安装
            InstallableItem.State.NOT_STARTED
        }
    }

    private fun isJreArchSupported(): Boolean {
        return runCatching {
            val allPacks = assetManager.list(jre.jrePath) ?: return@runCatching false
            //检查是否包含符合当前设备架构的环境
            val runtime = getRuntimeByArch()
            allPacks.contains(runtime).also {
                Logger.info(TAG, "Device requires environment: ${jre.jrePath}/$runtime, contains = $it")
            }
        }.getOrElse { e ->
            Logger.warning(TAG, "Failed to list assets directory", e)
            false
        }
    }

    private fun getRuntimeByArch() = "bin-" + Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE) + ".tar.xz"

    override suspend fun run() {
        val universalPath = jre.jrePath + "/universal.tar.xz"
        val binsPath = jre.jrePath + "/" + getRuntimeByArch()
        checkFreeSpace(universalPath, binsPath)

        var lastError: IOException? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                //os arquivos são abertos de novo a cada tentativa (leitura do zero)
                assetManager.open(universalPath).use { universal ->
                    assetManager.open(binsPath).use { bins ->
                        RuntimesManager.installRuntimeBinPack(
                            universalFileInputStream = universal,
                            platformBinsInputStream = bins,
                            name = jre.jreName,
                            binPackVersion = launcherRuntimeVersion,
                            updateProgress = { textRes, textArgs ->
                                updateMessage(context.getString(textRes, *textArgs))
                            }
                        )
                    }
                }
                RuntimesManager.postPrepare(jre.jreName)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                //CorruptedInputException, EOFException e outros erros de leitura/gravação
                lastError = e
                Logger.warning(TAG, "Internal JRE unpack failed (${jre.jreName}, attempt $attempt/$MAX_ATTEMPTS)", e)
                if (isOutOfSpace(e)) break
            }
        }

        val error = lastError!!
        Logger.error("UnpackJre", "Internal JRE unpack failed", error)
        throw when {
            error is UnpackSpaceException -> error
            isOutOfSpace(error) -> UnpackSpaceException(context.getString(R.string.cryo_unpack_no_space_simple)).apply { initCause(error) }
            else -> IOException(
                context.getString(R.string.cryo_unpack_corrupt, MAX_ATTEMPTS) + "\n(" + (error.message ?: error.javaClass.simpleName) + ")",
                error
            )
        }
    }

    /** Confere o espaço livre antes de extrair (estimado pelo tamanho dos arquivos compactados). */
    private fun checkFreeSpace(vararg assetPaths: String) {
        val compressed = assetPaths.sumOf { path ->
            runCatching { assetManager.open(path).use { it.available().toLong() } }.getOrDefault(0L)
        }
        if (compressed <= 0L) return
        val folder = PathManager.DIR_MULTIRT.apply { mkdirs() }
        val free = folder.usableSpace
        val needed = compressed * EXTRACT_RATIO + EXTRA_SPACE
        //0 = o sistema não informou; nesse caso segue e o erro de gravação (se houver) é tratado
        if (free in 1 until needed) {
            throw UnpackSpaceException(
                context.getString(
                    R.string.cryo_unpack_no_space,
                    android.text.format.Formatter.formatShortFileSize(context, needed),
                    android.text.format.Formatter.formatShortFileSize(context, free)
                )
            )
        }
    }

    private fun isOutOfSpace(e: Throwable): Boolean {
        var t: Throwable? = e
        while (t != null) {
            if (t is UnpackSpaceException) return true
            val msg = t.message.orEmpty()
            if ("ENOSPC" in msg || "No space left" in msg) return true
            t = t.cause
        }
        return false
    }
}
