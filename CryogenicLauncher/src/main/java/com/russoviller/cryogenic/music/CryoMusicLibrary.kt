package com.russoviller.cryogenic.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Biblioteca de músicas do Launcher: a pasta "Cryogenic Music" no armazenamento interno.
 * - Arquivos soltos na pasta = "Músicas".
 * - Cada pasta dentro dela = uma playlist.
 * - Um ".nomedia" esconde as músicas da galeria/players do celular (não aparecem duplicadas).
 */
object CryoMusicLibrary {
    private const val TAG = "CryoMusicLibrary"
    const val FOLDER_NAME = "Cryogenic Music"
    private const val CACHE_NAME = ".cryo_tags.json"
    private val EXTENSIONS = setOf("mp3", "flac", "ogg", "oga", "opus", "m4a", "aac", "wav", "wma", "mka", "amr", "3gp")
    private val BAD_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    data class Playlist(val name: String, val dir: File, val songs: List<CryoPlayer.Song>)
    data class Library(val songs: List<CryoPlayer.Song>, val playlists: List<Playlist>)

    /** Música do celular (fora da pasta Cryogenic Music) que pode ser importada. */
    data class DeviceSong(
        /** Chave única: "m:<id>" (biblioteca do Android) ou o caminho do arquivo (pastas). */
        val key: String,
        val uri: Uri,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val displayName: String,
        val size: Long,
        val folder: String
    )

    /** Pasta do celular que tem músicas .mp3. */
    data class DeviceFolder(val dir: File, val name: String, val path: String, val count: Int)

    data class ImportResult(val copied: Int, val skipped: Int, val failed: Int)

    var library by mutableStateOf<Library?>(null)
        private set
    var scanning by mutableStateOf(false)
        private set

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tagCache: MutableMap<String, JSONObject>? = null

    /** Só para as prévias de tela nos testes. */
    @androidx.annotation.VisibleForTesting
    internal var previewAccess = false

    @androidx.annotation.VisibleForTesting
    internal fun previewState(lib: Library) {
        previewAccess = true
        library = lib
    }

    fun root(): File = File(Environment.getExternalStorageDirectory(), FOLDER_NAME)

    /** O Launcher pode ler e gravar no armazenamento interno? */
    fun canAccess(context: Context): Boolean {
        if (previewAccess) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Cria a pasta e o .nomedia (se ainda não existirem). */
    fun ensureFolder(): Boolean = runCatching {
        val dir = root()
        if (!dir.isDirectory && !dir.mkdirs()) return@runCatching false
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        true
    }.onFailure { Logger.warning(TAG, "Could not create the music folder", it) }.getOrDefault(false)

    fun ensureFolderAsync() {
        Thread({ ensureFolder() }, "CryoMusicFolder").apply { isDaemon = true }.start()
    }

    private fun isAudio(file: File) = file.isFile && file.extension.lowercase() in EXTENSIONS && !file.name.startsWith(".")

    // ------------------------------------------------------------------ leitura

    /** Relê a pasta (rápido: as informações das músicas ficam guardadas). */
    fun refresh(context: Context) {
        if (previewAccess || !canAccess(context)) return
        scope.launch { reload() }
    }

    private suspend fun reload() = mutex.withLock {
        scanning = true
        try {
            val (lib, missing) = withContext(Dispatchers.IO) {
                ensureFolder()
                scan()
            }
            library = lib
            //músicas novas: lê nome/artista/duração do arquivo em segundo plano e atualiza uma vez só
            if (missing.isNotEmpty()) {
                val changed = withContext(Dispatchers.IO) { readTags(missing) }
                if (changed) library = withContext(Dispatchers.IO) { scan().first }
            }
        } catch (e: Exception) {
            Logger.warning(TAG, "Library scan failed", e)
        } finally {
            scanning = false
        }
    }

    private fun cache(): MutableMap<String, JSONObject> {
        tagCache?.let { return it }
        val map = HashMap<String, JSONObject>()
        runCatching {
            val file = File(root(), CACHE_NAME)
            if (file.isFile && file.length() < 4 * 1024 * 1024) {
                val json = JSONObject(file.readText())
                json.keys().forEach { key -> json.optJSONObject(key)?.let { map[key] = it } }
            }
        }
        tagCache = map
        return map
    }

    private fun saveCache() {
        val map = tagCache ?: return
        runCatching {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            val tmp = File(root(), "$CACHE_NAME.tmp")
            tmp.writeText(json.toString())
            tmp.renameTo(File(root(), CACHE_NAME))
        }.onFailure { Logger.warning(TAG, "Could not save the tag cache", it) }
    }

    private fun keyOf(file: File) = file.relativeTo(root()).path

    private fun songOf(file: File, missing: MutableList<File>): CryoPlayer.Song {
        val entry = cache()[keyOf(file)]
        val valid = entry != null && entry.optLong("s") == file.length() && entry.optLong("m") == file.lastModified()
        val (fileArtist, fileTitle) = splitName(file.nameWithoutExtension)
        if (!valid) missing += file
        return CryoPlayer.Song(
            id = file.absolutePath,
            title = entry?.takeIf { valid }?.optString("t")?.ifBlank { null } ?: fileTitle,
            artist = entry?.takeIf { valid }?.optString("a")?.ifBlank { null } ?: fileArtist,
            artUrl = null,
            thumbUrl = null,
            uri = Uri.fromFile(file).toString(),
            durationMs = entry?.takeIf { valid }?.optLong("d") ?: 0L,
            local = true
        )
    }

    /** "Artista - Música" vira artista e título. */
    private fun splitName(base: String): Pair<String, String> {
        val parts = base.split(" - ", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0].trim() to parts[1].trim() else "" to base
    }

    private fun scan(): Pair<Library, List<File>> {
        val dir = root()
        val missing = ArrayList<File>()
        val entries = dir.listFiles().orEmpty()
        val order = compareBy<CryoPlayer.Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
        val songs = entries.filter(::isAudio).map { songOf(it, missing) }.sortedWith(order)
        val playlists = entries
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .map { pl -> Playlist(pl.name, pl, pl.listFiles().orEmpty().filter(::isAudio).map { songOf(it, missing) }.sortedWith(order)) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        return Library(songs, playlists) to missing
    }

    /** Lê as etiquetas (título, artista, duração) de arquivos ainda não conhecidos. */
    private fun readTags(files: List<File>): Boolean {
        val map = cache()
        var changed = false
        val retriever = MediaMetadataRetriever()
        try {
            for (file in files) {
                val entry = JSONObject().put("s", file.length()).put("m", file.lastModified())
                runCatching {
                    retriever.setDataSource(file.absolutePath)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotEmpty() }?.let { entry.put("t", it.take(200)) }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()?.takeIf { it.isNotEmpty() }?.let { entry.put("a", it.take(200)) }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { entry.put("d", it) }
                }
                map[keyOf(file)] = entry
                changed = true
            }
        } finally {
            runCatching { retriever.release() }
        }
        if (changed) saveCache()
        return changed
    }

    // ------------------------------------------------------------------ músicas do celular

    /** Músicas do celular para importar (as da pasta Cryogenic Music ficam de fora). */
    suspend fun deviceSongs(context: Context): List<DeviceSong> = withContext(Dispatchers.IO) {
        val out = ArrayList<DeviceSong>()
        val rootPath = root().absolutePath + File.separator
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        @Suppress("DEPRECATION")
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        )
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                @Suppress("DEPRECATION")
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext() && out.size < 5000) {
                    val path = c.getString(dataCol).orEmpty()
                    if (path.startsWith(rootPath)) continue
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol).orEmpty()
                    out += DeviceSong(
                        key = "m:$id",
                        uri = ContentUris.withAppendedId(collection, id),
                        title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.'),
                        artist = c.getString(artistCol)?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }.orEmpty(),
                        durationMs = c.getLong(durCol),
                        displayName = name,
                        size = c.getLong(sizeCol),
                        folder = File(path).parentFile?.name.orEmpty()
                    )
                }
            }
        }.onFailure { Logger.warning(TAG, "MediaStore query failed", it) }
        out
    }

    /**
     * Pastas do celular com arquivos .mp3 (olha o armazenamento direto, então acha até pastas
     * que a biblioteca do Android ignora). Ficam de fora: pastas ocultas, Android/data, Android/obb
     * e a própria Cryogenic Music.
     */
    suspend fun deviceFolders(): List<DeviceFolder> = withContext(Dispatchers.IO) {
        val storage = Environment.getExternalStorageDirectory()
        val skip = setOf(
            root().absolutePath,
            File(storage, "Android/data").absolutePath,
            File(storage, "Android/obb").absolutePath
        )
        val out = ArrayList<DeviceFolder>()
        var visited = 0
        fun walk(dir: File, depth: Int) {
            if (depth > 10 || visited > 30_000) return
            visited++
            val children = dir.listFiles() ?: return
            var count = 0
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    if (child.absolutePath !in skip) walk(child, depth + 1)
                } else if (child.extension.equals("mp3", ignoreCase = true)) {
                    count++
                }
            }
            if (count > 0) {
                val rel = dir.relativeToOrNull(storage)?.path?.ifEmpty { null } ?: dir.name
                out += DeviceFolder(dir, dir.name.ifEmpty { rel }, rel, count)
            }
        }
        runCatching { walk(storage, 0) }.onFailure { Logger.warning(TAG, "Folder scan failed", it) }
        out.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.path })
    }

    /** Os .mp3 de uma pasta (só os arquivos dela, sem subpastas). */
    suspend fun folderSongs(folder: File): List<DeviceSong> = withContext(Dispatchers.IO) {
        folder.listFiles().orEmpty()
            .filter { it.isFile && !it.name.startsWith(".") && it.extension.equals("mp3", ignoreCase = true) }
            .map { file ->
                val (artist, title) = splitName(file.nameWithoutExtension)
                DeviceSong(
                    key = file.absolutePath,
                    uri = Uri.fromFile(file),
                    title = title,
                    artist = artist,
                    durationMs = 0L,
                    displayName = file.name,
                    size = file.length(),
                    folder = folder.name
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
    }

    // ------------------------------------------------------------------ ações

    private fun clean(name: String): String =
        name.replace(BAD_CHARS, "_").trim().trimStart('.').take(120).trim()

    /** Nome livre: "Música.mp3", "Música (2).mp3"... */
    private fun freeFile(dir: File, base: String, ext: String): File {
        var file = File(dir, "$base.$ext")
        var n = 2
        while (file.exists()) {
            file = File(dir, "$base ($n).$ext")
            n++
        }
        return file
    }

    /** Copia as músicas escolhidas para [target] (pasta principal ou uma playlist). */
    suspend fun import(
        context: Context,
        songs: List<DeviceSong>,
        target: File,
        onProgress: (done: Int, total: Int) -> Unit
    ): ImportResult {
        val result = mutex.withLock {
            withContext(Dispatchers.IO) {
                ensureFolder()
                target.mkdirs()
                var copied = 0
                var skipped = 0
                var failed = 0
                val map = cache()
                songs.forEachIndexed { index, song ->
                    withContext(Dispatchers.Main) { onProgress(index, songs.size) }
                    val ext = song.displayName.substringAfterLast('.', "mp3").lowercase().ifBlank { "mp3" }
                    val base = clean(if (song.artist.isNotBlank()) "${song.artist} - ${song.title}" else song.title)
                        .ifBlank { clean(song.displayName.substringBeforeLast('.')) }
                        .ifBlank { "Música" }
                    val same = File(target, "$base.$ext")
                    if (same.isFile && same.length() == song.size) {
                        skipped++
                        return@forEachIndexed
                    }
                    val dest = freeFile(target, base, ext)
                    val part = File(target, ".${dest.name}.part")
                    try {
                        context.contentResolver.openInputStream(song.uri).use { input ->
                            requireNotNull(input) { "No stream" }
                            part.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                        }
                        if (!part.renameTo(dest)) error("Rename failed")
                        //já guarda as informações da música (não precisa ler o arquivo depois)
                        map[keyOf(dest)] = JSONObject()
                            .put("s", dest.length())
                            .put("m", dest.lastModified())
                            .put("t", song.title.take(200))
                            .put("a", song.artist.take(200))
                            .put("d", song.durationMs)
                        copied++
                    } catch (e: Exception) {
                        Logger.warning(TAG, "Import failed: ${song.displayName}", e)
                        part.delete()
                        failed++
                    }
                }
                withContext(Dispatchers.Main) { onProgress(songs.size, songs.size) }
                saveCache()
                ImportResult(copied, skipped, failed)
            }
        }
        reload()
        return result
    }

    /** Move uma música para [targetDir] (playlist ou pasta principal). */
    suspend fun move(song: CryoPlayer.Song, targetDir: File): Boolean {
        val ok = mutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(song.id)
                    if (!file.isFile) return@runCatching false
                    if (file.parentFile?.absolutePath == targetDir.absolutePath) return@runCatching true
                    targetDir.mkdirs()
                    val dest = freeFile(targetDir, file.nameWithoutExtension, file.extension)
                    val oldKey = keyOf(file)
                    val moved = file.renameTo(dest) || (runCatching { file.copyTo(dest); file.delete() }.getOrDefault(false))
                    if (moved) {
                        val map = cache()
                        map.remove(oldKey)?.let { map[keyOf(dest)] = it.put("m", dest.lastModified()) }
                        saveCache()
                    }
                    moved
                }.getOrDefault(false)
            }
        }
        reload()
        return ok
    }

    suspend fun delete(song: CryoPlayer.Song): Boolean {
        if (CryoPlayer.current?.id == song.id) CryoPlayer.next()
        val ok = mutex.withLock {
            withContext(Dispatchers.IO) {
                val file = File(song.id)
                runCatching { file.delete() }.getOrDefault(false).also { if (it) cache().remove(keyOf(file)) }
            }
        }
        reload()
        return ok
    }

    /** Cria uma playlist (pasta). Retorna false se o nome for inválido ou já existir. */
    suspend fun createPlaylist(name: String): Boolean {
        val clean = clean(name)
        if (clean.isEmpty()) return false
        val ok = withContext(Dispatchers.IO) {
            ensureFolder()
            val dir = File(root(), clean)
            !dir.exists() && dir.mkdirs()
        }
        reload()
        return ok
    }

    /** Renomeia a playlist. Retorna a pasta nova (ou null se o nome for inválido ou já existir). */
    suspend fun renamePlaylist(playlist: Playlist, name: String): File? {
        val clean = clean(name)
        if (clean.isEmpty()) return null
        val dest = File(root(), clean)
        val ok = mutex.withLock {
            withContext(Dispatchers.IO) {
                if (dest.exists()) return@withContext false
                val moved = playlist.dir.renameTo(dest)
                if (moved) {
                    //as informações guardadas acompanham a pasta nova
                    val map = cache()
                    val prefix = playlist.dir.name + File.separator
                    map.keys.filter { it.startsWith(prefix) }.forEach { key ->
                        map.remove(key)?.let { map[clean + File.separator + key.removePrefix(prefix)] = it }
                    }
                    saveCache()
                }
                moved
            }
        }
        reload()
        return if (ok) dest else null
    }

    /** Apaga a playlist: as músicas dela voltam para a pasta principal (nada é perdido). */
    suspend fun deletePlaylist(playlist: Playlist): Boolean {
        val ok = mutex.withLock {
            withContext(Dispatchers.IO) {
                val rootDir = root()
                playlist.dir.listFiles().orEmpty().filter(::isAudio).forEach { file ->
                    val dest = freeFile(rootDir, file.nameWithoutExtension, file.extension)
                    file.renameTo(dest)
                }
                tagCache = null //relê na próxima vez
                playlist.dir.delete()
            }
        }
        reload()
        return ok
    }
}
