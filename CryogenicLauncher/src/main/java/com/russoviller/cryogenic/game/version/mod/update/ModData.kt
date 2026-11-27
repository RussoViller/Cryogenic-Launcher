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

package com.russoviller.cryogenic.game.version.mod.update

import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import com.russoviller.cryogenic.game.download.assets.platform.PlatformVersion
import com.russoviller.cryogenic.game.download.assets.platform.getVersions
import com.russoviller.cryogenic.game.version.mod.ModFile
import com.russoviller.cryogenic.game.version.mod.ModProject
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.initAll
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.string.parseInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jackhuang.hmcl.addon.AddonProjectInfo
import java.io.File

private const val TAG = "ModData"

/**
 * 需要更新的模组的数据类，记录模组文件和模组所属的项目
 * @param modFile 模组在模组平台上对应的文件
 * @param project 模组在模组平台上所属的项目
 * @param mcMod 模组翻译信息
 */
data class ModData(
    val file: File,
    val modFile: ModFile,
    val project: ModProject,
    val mcMod: AddonProjectInfo?
) {
    /**
     * 当前模组的版本号，用于新旧对比
     */
    var currentVersion: String? = null
        private set

    /**
     * Verifica se há versão nova do mod para a versão do Minecraft e o carregador atuais.
     *
     * Melhorias do Cryogenic:
     * - Ao trocar a instância de versão (ex.: 26.2 -> 26.3), o arquivo instalado não serve para a
     *   versão nova; nesse caso qualquer arquivo compatível conta como atualização, mesmo que tenha
     *   sido publicado antes do arquivo instalado (antes esses eram ignorados pela data).
     * - Versões do Minecraft comparadas de forma tolerante ("26.3" == "26.3.0").
     * - Quilt aceita arquivos Fabric quando não há arquivo Quilt.
     * - Até 3 tentativas quando a consulta à plataforma falha (limite de requisições, rede).
     */
    suspend fun checkUpdate(
        minecraftVer: String,
        modLoader: ModLoader
    ): UpdateCheck {
        return withContext(Dispatchers.IO) {
            val versions = fetchVersionsWithRetry() ?: return@withContext UpdateCheck.Failed

            runCatching {
                val currentLoaderName = modLoader.displayName.lowercase()
                val currentFileLoaders = modFile.loaders
                    .map { it.getDisplayName().lowercase() }
                    .toSet()
                val targetLoaders = when {
                    currentLoaderName in currentFileLoaders -> setOf(currentLoaderName)
                    currentFileLoaders.isNotEmpty() -> currentFileLoaders
                    else -> setOf(currentLoaderName)
                }
                //Mods Fabric rodam no Quilt
                val fallbackLoaders = if ("quilt" in targetLoaders) setOf("fabric") else emptySet()
                val target = normalizeMc(minecraftVer)

                val currentEntry = versions.firstOrNull { it.platformId() == modFile.id }
                if (currentEntry != null) currentVersion = currentEntry.platformVersion()
                val datePublished = currentEntry?.platformDatePublished() ?: run {
                    Logger.debug(TAG, "Installed version ${modFile.id} not found in the fetched list of ${file.name}, falling back to the cached date")
                    parseInstant(modFile.datePublished)
                }
                //O arquivo instalado já é feito para esta versão do Minecraft?
                val installedFitsTarget = currentEntry?.platformGameVersion()?.any { normalizeMc(it) == target } == true

                fun compatible(loaders: Set<String>) = versions.filter { version ->
                    val loaderNames = version.platformLoaders().map { it.getDisplayName().lowercase() }.toSet()
                    version.platformGameVersion().any { normalizeMc(it) == target } &&
                        (loaderNames.isEmpty() || loaderNames.any { it in loaders })
                }.sortedByDescending { it.platformDatePublished() }

                val compatibleVersions = compatible(targetLoaders).ifEmpty { compatible(fallbackLoaders) }
                if (compatibleVersions.isEmpty()) return@runCatching UpdateCheck.NoCompatible

                val newest = compatibleVersions.first()
                val update = if (installedFitsTarget) {
                    newest.takeIf { it.platformId() != modFile.id && it.platformDatePublished() > datePublished }
                } else {
                    //Instância mudou de versão: o mais novo compatível substitui o arquivo antigo
                    newest.takeIf { it.platformId() != modFile.id }
                }
                if (update != null) {
                    Logger.info(TAG, "Detected update for mod ${file.name}: $currentVersion -> ${update.platformVersion()}")
                    UpdateCheck.Found(update)
                } else {
                    UpdateCheck.UpToDate
                }
            }.onFailure { th ->
                Logger.warning(TAG, "An error occurred while checking the versions of the mod.", th)
            }.getOrDefault(UpdateCheck.Failed)
        }
    }

    private suspend fun fetchVersionsWithRetry(): List<PlatformVersion>? {
        var last: Throwable? = null
        repeat(3) { attempt ->
            try {
                return getVersions(project.id, project.platform).initAll(project.id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                last = e
                kotlinx.coroutines.delay(1_500L * (attempt + 1))
            }
        }
        Logger.warning(TAG, "Could not fetch the versions of ${file.name}", last)
        return null
    }
}

/** Resultado da verificação de atualização de um mod. */
sealed interface UpdateCheck {
    data class Found(val version: PlatformVersion) : UpdateCheck
    /** Já está na versão mais nova para este Minecraft/carregador. */
    data object UpToDate : UpdateCheck
    /** O mod não tem nenhum arquivo para esta versão do Minecraft/carregador. */
    data object NoCompatible : UpdateCheck
    /** Não foi possível consultar a plataforma. */
    data object Failed : UpdateCheck
}

/** "26.3.0" -> "26.3", " 1.21 " -> "1.21" (para comparar versões do Minecraft). */
internal fun normalizeMc(version: String): String {
    var v = version.trim().lowercase()
    while (v.count { it == '.' } > 1 && v.endsWith(".0")) v = v.removeSuffix(".0")
    return v
}
