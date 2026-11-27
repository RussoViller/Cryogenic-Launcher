package com.russoviller.cryogenic.game.version.mod.conflict

import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import com.russoviller.cryogenic.game.download.assets.platform.PlatformVersion
import com.russoviller.cryogenic.game.download.assets.platform.getProjectByVersion
import com.russoviller.cryogenic.game.download.assets.platform.getVersionByLocalFile
import com.russoviller.cryogenic.game.download.assets.platform.getVersions
import com.russoviller.cryogenic.game.download.assets.platform.mcim.mapMCIMMirrorUrls
import com.russoviller.cryogenic.game.version.mod.update.normalizeMc
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.ui.screens.content.download.assets.elements.initAll
import com.russoviller.cryogenic.utils.file.calculateFileSha1
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.network.downloadFileFromSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Trocar a versão de um mod (para resolver conflito de versão entre mods):
 * acha o mod no Modrinth/CurseForge pelo arquivo, lista as versões feitas para o mesmo
 * Minecraft e carregador, baixa a escolhida e substitui o arquivo atual.
 */
object ModVersionSwap {
    private const val TAG = "ModVersionSwap"

    class Option(val version: PlatformVersion, val current: Boolean)

    class Listing(val title: String, val options: List<Option>)

    /** null = o mod não foi encontrado nas plataformas (ou sem internet). */
    suspend fun list(file: File, minecraft: String?, loader: ModLoader?): Listing? = withContext(Dispatchers.IO) {
        runCatching {
            val sha1 = calculateFileSha1(file)
            val current = getVersionByLocalFile(file, sha1) ?: return@runCatching null
            val projectId = current.platformProjectId()
            val platform = current.platform()
            val all = getVersions(projectId, platform).initAll(projectId)
            val title = runCatching {
                getProjectByVersion(projectId, platform, printLog = false).platformTitle()
            }.getOrNull() ?: file.nameWithoutExtension

            val loaderName = loader?.displayName?.lowercase()
            val accepted = buildSet {
                loaderName?.let { add(it) }
                if (loaderName == "quilt") add("fabric") //mods Fabric rodam no Quilt
            }
            val target = minecraft?.let(::normalizeMc)
            val options = all.filter { v ->
                val loaders = v.platformLoaders().map { it.getDisplayName().lowercase() }
                val loaderOk = accepted.isEmpty() || loaders.isEmpty() || loaders.any { it in accepted }
                val mcOk = target == null || v.platformGameVersion().any { normalizeMc(it) == target }
                (loaderOk && mcOk) || v.platformId() == current.platformId()
            }.sortedByDescending { it.platformDatePublished() }
                .map { Option(it, it.platformId() == current.platformId()) }
            Listing(title, options)
        }.onFailure { Logger.warning(TAG, "Could not list versions of ${file.name}", it) }.getOrNull()
    }

    /** Baixa [version] e coloca no lugar de [oldFile]. Retorna o arquivo novo, ou null se falhar. */
    suspend fun install(oldFile: File, version: PlatformVersion): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(PathManager.DIR_CACHE_MOD_UPDATER, "swap").apply { mkdirs() }
            val temp = File(dir, version.platformFileName())
            temp.delete()
            downloadFileFromSources(version.platformDownloadUrl().mapMCIMMirrorUrls(), temp, version.platformSha1())
            val target = File(oldFile.parentFile, version.platformFileName())
            if (target.absolutePath != oldFile.absolutePath) oldFile.delete()
            temp.copyTo(target, overwrite = true)
            temp.delete()
            target
        }.onFailure { Logger.warning(TAG, "Could not install ${version.platformFileName()}", it) }.getOrNull()
    }
}
