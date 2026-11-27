package com.russoviller.cryogenic.music

import android.app.Application
import android.content.Context
import android.os.Build
import com.russoviller.cryogenic.utils.logging.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Guarda a música (fila, música atual e posição) entre uma entrada e outra no jogo.
 * Só é recuperada enquanto o Launcher continua aberto: cada vez que o processo principal
 * do Launcher inicia, ele gera um id novo, e a sessão de um id antigo é ignorada.
 */
object CryoMusicSession {
    private const val TAG = "CryoMusicSession"

    data class Saved(val songs: List<CryoPlayer.Song>, val index: Int, val positionMs: Long)

    private fun dir(context: Context) = File(context.filesDir, "music").apply { mkdirs() }
    private fun launcherIdFile(context: Context) = File(dir(context), "launcher_session")
    private fun sessionFile(context: Context) = File(dir(context), "session.json")

    /** Chamado ao iniciar qualquer processo do app: só o processo principal gera um id novo. */
    fun onApplicationCreate(app: Application) {
        if (processName(app) != app.packageName) return
        val id = UUID.randomUUID().toString()
        Thread({
            runCatching { writeAtomic(launcherIdFile(app), id) }
                .onFailure { Logger.warning(TAG, "Could not write the launcher session id", it) }
        }, "CryoMusicSession").start()
    }

    private fun processName(app: Application): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return Application.getProcessName()
        return runCatching {
            File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n')
        }.getOrNull()
    }

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    private fun launcherId(context: Context): String? =
        runCatching { launcherIdFile(context).takeIf { it.isFile }?.readText()?.trim() }.getOrNull()

    /** Salva a sessão (rápido; chamar fora da thread principal). */
    fun save(context: Context, songs: List<CryoPlayer.Song>, index: Int, positionMs: Long) {
        val id = launcherId(context) ?: return
        runCatching {
            val list = JSONArray()
            songs.forEach { s ->
                list.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("t", s.title)
                        .put("a", s.artist)
                        .put("u", s.uri)
                        .put("d", s.durationMs)
                        .put("l", s.local)
                )
            }
            val json = JSONObject()
                .put("launcher", id)
                .put("index", index)
                .put("pos", positionMs)
                .put("songs", list)
            writeAtomic(sessionFile(context), json.toString())
        }.onFailure { Logger.warning(TAG, "Could not save the music session", it) }
    }

    /** A sessão salva, se ela for desta mesma abertura do Launcher. */
    fun load(context: Context): Saved? = runCatching {
        val file = sessionFile(context)
        if (!file.isFile || file.length() > 8L * 1024 * 1024) return null
        val json = JSONObject(file.readText())
        val id = launcherId(context) ?: return null
        if (json.optString("launcher") != id) return null
        val list = json.optJSONArray("songs") ?: return null
        val songs = (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            val uri = o.optString("u")
            if (uri.isBlank()) return@mapNotNull null
            CryoPlayer.Song(
                id = o.optString("id", uri),
                title = o.optString("t"),
                artist = o.optString("a"),
                artUrl = null,
                thumbUrl = null,
                uri = uri,
                durationMs = o.optLong("d"),
                local = o.optBoolean("l", true)
            )
        }
        if (songs.isEmpty()) return null
        Saved(songs, json.optInt("index").coerceIn(0, songs.lastIndex), json.optLong("pos").coerceAtLeast(0L))
    }.onFailure { Logger.warning(TAG, "Could not read the music session", it) }.getOrNull()
}
