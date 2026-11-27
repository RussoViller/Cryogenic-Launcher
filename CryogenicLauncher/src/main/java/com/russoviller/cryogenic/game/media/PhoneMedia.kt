package com.russoviller.cryogenic.game.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.russoviller.cryogenic.utils.logging.Logger

/**
 * Serviço vazio: existe só para o Android liberar a leitura das sessões de mídia
 * (Spotify, YouTube, YouTube Music...) depois que o usuário permite o acesso às notificações.
 * O launcher não lê nem guarda notificações.
 */
class CryoMediaListenerService : NotificationListenerService()

/** Música que está tocando no celular, no formato que os mods de PC esperam (MPRIS). */
data class PhoneTrack(
    val title: String,
    val artists: List<String>,
    val album: String,
    val lengthUs: Long,
    val positionUs: Long,
    val status: String,
    val rate: Double,
    val source: String
)

object PhoneMedia {
    private const val TAG = "PhoneMedia"

    fun listenerComponent(context: Context) = ComponentName(context, CryoMediaListenerService::class.java)

    /** Acesso às notificações liberado para o Cryogenic. */
    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Abre a tela do sistema para liberar o acesso (direto no Cryogenic quando o Android permite). */
    fun openAccessSettings(context: Context) {
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                listenerComponent(context).flattenToString()
            )
        } else null
        val generic = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        for (intent in listOfNotNull(detail, generic)) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                Logger.warning(TAG, "Cannot open notification access settings: ${e.message}")
            }
        }
    }

    /**
     * Sessão de mídia ativa mais relevante: a que está tocando; se nenhuma estiver, a mais recente
     * que tenha título. Retorna null sem acesso ou sem nada tocando.
     */
    fun current(context: Context): PhoneTrack? {
        if (!hasAccess(context)) return null
        val controllers = try {
            val manager = context.getSystemService(MediaSessionManager::class.java) ?: return null
            manager.getActiveSessions(listenerComponent(context))
        } catch (e: SecurityException) {
            return null
        } catch (e: Exception) {
            Logger.warning(TAG, "Cannot read media sessions: ${e.message}")
            return null
        }
        val candidates = controllers.filter { !titleOf(it).isNullOrBlank() }
        val chosen = candidates.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: candidates.firstOrNull()
            ?: return null
        return toTrack(chosen)
    }

    private fun titleOf(controller: MediaController): String? {
        val metadata = controller.metadata ?: return null
        return metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
    }

    private fun toTrack(controller: MediaController): PhoneTrack {
        val metadata = controller.metadata
        val state = controller.playbackState
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val playing = state?.state == PlaybackState.STATE_PLAYING
        val speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        var positionMs = state?.position?.coerceAtLeast(0L) ?: 0L
        if (playing && state != null && state.lastPositionUpdateTime > 0L) {
            //a posição informada é a do último update; avança pelo tempo decorrido
            positionMs += ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * speed).toLong()
        }
        if (durationMs > 0) positionMs = positionMs.coerceAtMost(durationMs)
        val status = when (state?.state) {
            PlaybackState.STATE_PLAYING -> "Playing"
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> "Paused"
            else -> "Stopped"
        }
        return PhoneTrack(
            title = titleOf(controller).orEmpty(),
            artists = artist.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            lengthUs = durationMs.coerceAtLeast(0L) * 1000L,
            positionUs = positionMs * 1000L,
            status = status,
            rate = if (playing) speed.toDouble() else 1.0,
            source = controller.packageName
        )
    }
}
