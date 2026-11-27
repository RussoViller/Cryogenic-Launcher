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

package com.russoviller.cryogenic.game.multirt

import android.system.Os
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.components.jre.Jre
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.utils.file.child
import com.russoviller.cryogenic.utils.file.ensureDirectory
import com.russoviller.cryogenic.utils.file.ensureParentDirectory
import com.russoviller.cryogenic.utils.logging.Logger
import com.russoviller.cryogenic.utils.math.findNearestPositive
import com.russoviller.cryogenic.utils.string.compareVersion
import com.russoviller.cryogenic.utils.string.extractUntilCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "RuntimesManager"

/**
 * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/multirt/MultiRTUtils.java)
 */
object RuntimesManager {
    private val cache = ConcurrentHashMap<String, Runtime>()

    private val RUNTIME_FOLDER = PathManager.DIR_MULTIRT
    private const val JAVA_VERSION_STR: String = "JAVA_VERSION=\""
    private const val OS_ARCH_STR: String = "OS_ARCH=\""

    fun getRuntimes(forceLoad: Boolean = false): List<Runtime> {
        if (!RUNTIME_FOLDER.exists()) {
            Logger.warning(TAG, "Runtime directory not found: ${RUNTIME_FOLDER.absolutePath}")
            return emptyList()
        }

        return RUNTIME_FOLDER.listFiles()
            //pastas que começam com "." são extrações em andamento (ou restos de uma que falhou)
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.mapNotNull { loadRuntime(it.name, forceLoad = forceLoad) }
            ?.sortedWith { o1, o2 ->
                val thisVer = o1.versionString ?: o1.name
                -thisVer.compareVersion(o2.versionString ?: o2.name)
            }
            ?: throw IllegalStateException("Failed to access runtime directory")
    }

    fun getExactJreName(majorVersion: Int): String? {
        return getRuntimes().firstOrNull { it.javaVersion == majorVersion }?.name
    }

    fun getNearestJreName(majorVersion: Int): String? {
        return findNearestPositive(majorVersion, getRuntimes()) { it.javaVersion }?.value?.name
    }

    fun forceReload(name: String): Runtime {
        cache.remove(name)
        return loadRuntime(name)
    }

    fun loadRuntime(name: String, forceLoad: Boolean = false): Runtime {
        return cache[name]?.takeIf { !forceLoad } ?: run {
            val runtimeDir = File(RUNTIME_FOLDER, name)
            val releaseFile = File(runtimeDir, "release")

            if (!releaseFile.exists()) return Runtime(name).also { cache[name] = it }

            runCatching {
                val content = releaseFile.readText()
                val javaVersion = content.extractUntilCharacter(JAVA_VERSION_STR, '"')
                val osArch = content.extractUntilCharacter(OS_ARCH_STR, '"')

                if (javaVersion != null && osArch != null) {
                    val versionParts = javaVersion.split('.')
                    val majorVersion = if (versionParts.first() == "1") {
                        versionParts.getOrNull(1)?.toIntOrNull() ?: 0
                    } else {
                        versionParts.first().toIntOrNull() ?: 0
                    }

                    Runtime(
                        name = name,
                        versionString = javaVersion,
                        arch = osArch,
                        javaVersion = majorVersion,
                        isProvidedByLauncher = Jre.entries.any { it.jreName == name },
                        isJDK8 = isJDK8(runtimeDir.absolutePath)
                    )
                } else {
                    Runtime(name)
                }
            }.onFailure { e ->
                Logger.error(TAG, "Failed to load runtime $name", e)
            }.getOrElse {
                Runtime(name)
            }.also { cache[name] = it }
        }
    }

    @Throws(IOException::class)
    suspend fun installRuntime(
        nativeLibDir: String,
        inputStream: InputStream,
        name: String,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(name)
        //extrai numa pasta temporária: o destino só é trocado quando tudo terminou certo
        val temp = tempDirOf(name)
        try {
            if (temp.exists()) FileUtils.deleteDirectory(temp)
            uncompressTarXZ(inputStream, temp, updateProgress)
            unpack200(nativeLibDir, temp.absolutePath)
            replaceWithTemp(temp, dest)
            cache.remove(name)
            loadRuntime(name).also { runtime ->
                postPrepare(runtime)
            }
        } catch (e: Exception) {
            runCatching { FileUtils.deleteDirectory(temp) }
            throw e
        }
    }

    /** Pasta temporária da extração de [name] (oculta: não aparece na lista de runtimes). */
    private fun tempDirOf(name: String): File = RUNTIME_FOLDER.child(".$name.part")

    /** Troca o runtime antigo pelo recém-extraído. */
    @Throws(IOException::class)
    private fun replaceWithTemp(temp: File, dest: File) {
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        if (!temp.renameTo(dest)) {
            throw IOException("Failed to move ${temp.absolutePath} to ${dest.absolutePath}")
        }
    }

    /**
     * O runtime terminou de ser instalado? (a extração antiga deletava a pasta e só gravava
     * "version" no fim; aqui também conferimos o executável, para não reaproveitar algo pela metade)
     */
    fun isRuntimeComplete(name: String): Boolean {
        val dir = RUNTIME_FOLDER.child(name)
        return dir.child("bin", "java").exists() || dir.child("jre", "bin", "java").exists()
    }

    @Throws(IOException::class)
    suspend fun postPrepare(name: String) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(name)
        if (!dest.exists()) return@withContext
        val runtime = loadRuntime(name)
        postPrepare(runtime)
    }

    @Throws(IOException::class)
    suspend fun postPrepare(runtime: Runtime) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(runtime.name)
        if (!dest.exists()) return@withContext
        var libFolder = "lib"

        val arch = runtime.arch
        if (arch != null && dest.child(libFolder, arch).exists()) {
            libFolder += "/$arch"
        }

        val isJDK8 = isJDK8(dest.absolutePath)
        if (isJDK8) {
            libFolder = "jre/$libFolder"
        }

        val ftIn = dest.child(libFolder, "libfreetype.so.6")
        val ftOut = dest.child(libFolder, "libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }

        val ft2In = dest.child(libFolder, "libfreetype.so")
        if (isJDK8 && ft2In.exists()) {
            ft2In.renameTo(ftOut)
        }

        val localXawtLib = File(PathManager.DIR_NATIVE_LIB, "libawt_xawt.so")
        val targetXawtLib = dest.child(libFolder, "libawt_xawt.so")
        if (targetXawtLib.exists()) targetXawtLib.delete()
        FileUtils.copyFile(localXawtLib, targetXawtLib)

        // 补装 libjsound.so
        val localJsoundLib = File(PathManager.DIR_NATIVE_LIB, "libjsound.so")
        if (localJsoundLib.exists()) {
            val targetJsoundLib = dest.child(libFolder, "libjsound.so")
            if (targetJsoundLib.exists()) targetJsoundLib.delete()
            FileUtils.copyFile(localJsoundLib, targetJsoundLib)
        }
    }

    /**
     * Garante que o runtime tenha o libjsound.so atual do APK (javax.sound para mods).
     * O postPrepare só roda na extração, e o arquivo "version" dos JREs internos não muda
     * entre versões do launcher: runtimes extraídos antes do bridge (ou de um build antigo)
     * ficariam sem ele, e Clip/SourceDataLine falhariam em silêncio.
     * @return true se copiou/atualizou o arquivo
     */
    @Throws(IOException::class)
    fun ensureJsound(runtime: Runtime): Boolean {
        val local = File(PathManager.DIR_NATIVE_LIB, "libjsound.so")
        if (!local.isFile) return false
        val dest = RUNTIME_FOLDER.child(runtime.name)
        if (!dest.exists()) return false
        val base = if (isJDK8(dest.absolutePath)) dest.child("jre") else dest
        val arch = runtime.arch
        val libDir = if (arch != null && base.child("lib", arch).isDirectory) base.child("lib", arch) else base.child("lib")
        if (!libDir.isDirectory) return false
        val target = File(libDir, "libjsound.so")
        if (target.isFile && target.length() == local.length() &&
            FileUtils.contentEquals(local, target)
        ) return false
        if (target.exists()) target.delete()
        FileUtils.copyFile(local, target)
        return true
    }

    @Throws(IOException::class)
    suspend fun installRuntimeBinPack(
        universalFileInputStream: InputStream,
        platformBinsInputStream: InputStream,
        name: String,
        binPackVersion: String,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(name)
        //extrai numa pasta temporária: o destino só é trocado quando tudo terminou certo,
        //então uma extração interrompida ou corrompida nunca fica no lugar do runtime
        val temp = tempDirOf(name)
        try {
            if (temp.exists()) FileUtils.deleteDirectory(temp)
            installRuntimeNoRemove(universalFileInputStream, temp, updateProgress)
            installRuntimeNoRemove(platformBinsInputStream, temp, updateProgress)

            unpack200(PathManager.DIR_NATIVE_LIB, temp.absolutePath)

            //"version" marca a instalação como completa: só é gravado depois de extrair tudo
            val versionFile = File(temp, "version")
            versionFile.writeText(binPackVersion)

            replaceWithTemp(temp, dest)
            forceReload(name)
        } catch (e: Exception) {
            runCatching { FileUtils.deleteDirectory(temp) }
            throw e
        }
    }

    fun loadInternalRuntimeVersion(name: String): String? {
        val versionFile = RUNTIME_FOLDER.child(name, "version")
        try {
            return if (versionFile.exists()) {
                versionFile.readText()
            } else {
                null
            }
        } catch (_: IOException) {
            return null
        }
    }

    @Throws(IOException::class)
    fun removeRuntime(name: String) {
        val dest: File = RUNTIME_FOLDER.child(name).takeIf { it.exists() } ?: return
        FileUtils.deleteDirectory(dest)
        cache.remove(name)
    }

    fun getRuntimeHome(name: String): File {
        val dest = RUNTIME_FOLDER.child(name)
        if (!dest.exists() || forceReload(name).versionString == null) {
            throw RuntimeException("Selected runtime is broken!")
        }

        return dest
    }

    /**
     * Unpacks all .pack files into .jar Serves only for java 8, as java 9 brought project jigsaw
     * @param nativeLibraryDir The native lib path, required to execute the unpack200 binary
     * @param runtimePath The path to the runtime to walk into
     */
    private suspend fun unpack200(
        nativeLibraryDir: String,
        runtimePath: String
    ) = withContext(Dispatchers.Default) {
            val basePath = File(runtimePath)
            val files: Collection<File> = FileUtils.listFiles(basePath, arrayOf("pack"), true)

            val workDir = File(nativeLibraryDir)
            val processBuilder = ProcessBuilder().directory(workDir)

            files.forEach { jarFile ->
                ensureActive()
                runCatching {
                    val destPath = jarFile.absolutePath.replace(".pack", "")
                    processBuilder.command(
                        "./libunpack200.so",
                        "-r",
                        jarFile.absolutePath,
                        destPath
                    ).start().apply {
                        waitFor()
                    }
                }.onFailure { e ->
                    if (e is IOException) {
                        Logger.error(TAG, "Failed to unpack the runtime!", e)
                    } else throw e
                }
            }
        }

    @Throws(IOException::class)
    private suspend fun installRuntimeNoRemove(
        inputStream: InputStream,
        dest: File,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        uncompressTarXZ(inputStream, dest, updateProgress)
        inputStream.close()
    }

    @Throws(IOException::class)
    private suspend fun uncompressTarXZ(
        inputStream: InputStream,
        dest: File,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        dest.ensureDirectory()
        val buffer = ByteArray(8192)

        val input = BufferedInputStream(inputStream, 64 * 1024)
        if (!hasXZMagic(input)) {
            input.close()
            throw NotXZArchiveException()
        }

        TarArchiveInputStream(XZCompressorInputStream(input)).use { tarIn ->
            generateSequence { tarIn.nextEntry }.forEach { tarEntry ->
                ensureActive()
                val tarEntryName = tarEntry.name
                updateProgress(R.string.generic_unpacking, arrayOf(tarEntryName))

                val destPath = File(dest, tarEntryName).ensureParentDirectory()

                when {
                    tarEntry.isSymbolicLink -> try {
                        if (destPath.exists()) destPath.delete()
                        Os.symlink(tarEntry.linkName, destPath.absolutePath)
                    } catch (e: Throwable) {
                        Logger.warning(TAG, "Failed to create symbolic link: ${destPath.absolutePath} -> ${tarEntry.linkName} (${e.message})")
                    }

                    tarEntry.isDirectory -> destPath.ensureDirectory()
                    !destPath.exists() || destPath.length() != tarEntry.size ->
                        FileOutputStream(destPath).use { os ->
                            IOUtils.copyLarge(tarIn, os, buffer)
                        }
                }
            }
        }
    }

    /** Os 6 primeiros bytes de todo arquivo XZ: FD 37 7A 58 5A 00. */
    private val XZ_MAGIC = byteArrayOf(0xFD.toByte(), 0x37, 0x7A, 0x58, 0x5A, 0x00)

    private fun hasXZMagic(input: BufferedInputStream): Boolean {
        input.mark(XZ_MAGIC.size)
        val header = ByteArray(XZ_MAGIC.size)
        val read = IOUtils.read(input, header)
        input.reset()
        return read == XZ_MAGIC.size && header.contentEquals(XZ_MAGIC)
    }

    /** O arquivo não é um .xz de verdade (ex.: página de erro ou arquivo de outro tipo renomeado). */
    class NotXZArchiveException : IOException("The file is not a valid XZ archive")

    fun isJDK8(runtimeDir: String): Boolean {
        return File(runtimeDir, "jre").exists() && File(runtimeDir, "bin/javac").exists()
    }
}
