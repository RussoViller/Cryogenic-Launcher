package com.russoviller.cryogenic.filemanager.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.mmkv.MMKV
import java.io.File

/** Pastas favoritas do gerenciador de arquivos (no máximo [MAX]). */
object FmFavorites {
    const val MAX = 5
    private const val MMKV_ID = "cryogenic_file_manager"
    private const val KEY = "favorite_dirs"

    enum class Result { ADDED, REMOVED, FULL }

    private fun mmkv(): MMKV = MMKV.mmkvWithID(MMKV_ID, MMKV.SINGLE_PROCESS_MODE)

    /** Caminhos absolutos, na ordem em que foram favoritados. */
    var paths by mutableStateOf(load())
        private set

    private fun load(): List<String> = runCatching {
        mmkv().decodeString(KEY).orEmpty().split('\n').filter { it.isNotBlank() }.distinct().take(MAX)
    }.getOrDefault(emptyList())

    private fun save(list: List<String>) {
        paths = list
        runCatching { mmkv().putString(KEY, list.joinToString("\n")) }
    }

    fun isFavorite(path: String): Boolean = path in paths

    fun toggle(path: String): Result {
        val current = paths
        return when {
            path in current -> {
                save(current - path)
                Result.REMOVED
            }
            current.size >= MAX -> Result.FULL
            else -> {
                save(current + path)
                Result.ADDED
            }
        }
    }

    fun remove(path: String) {
        if (path in paths) save(paths - path)
    }

    /** Favoritas que ainda existem. */
    fun existing(): List<File> = paths.map(::File).filter { it.isDirectory }
}
