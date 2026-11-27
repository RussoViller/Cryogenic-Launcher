package com.russoviller.cryogenic.music

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Player de música do próprio Launcher (toca dentro do app, sem Spotify).
 * Toca músicas online (Audius) e arquivos do aparelho, com volume só da música:
 * mexer aqui não muda o som do jogo.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object CryoPlayer {
    private const val TAG = "CryoPlayer"

    data class Song(
        val id: String,
        val title: String,
        val artist: String,
        /** Capa online (URL) — músicas do aparelho usam a capa embutida no arquivo. */
        val artUrl: String?,
        /** Capa pequena para listas (gasta menos memória e internet). */
        val thumbUrl: String?,
        val uri: String,
        val durationMs: Long,
        val local: Boolean
    )

    enum class Repeat { OFF, ALL, ONE }

    var queue by mutableStateOf<List<Song>>(emptyList())
        private set
    var index by mutableIntStateOf(-1)
        private set
    val current: Song? get() = queue.getOrNull(index)

    var playing by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    /** Mensagem do último erro de reprodução (null = tudo certo). */
    var error by mutableStateOf<String?>(null)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    /** Capa embutida do arquivo local que está tocando. */
    var localArt by mutableStateOf<ImageBitmap?>(null)
        private set
    var volume by mutableFloatStateOf(runCatching { AllSettings.cryoPlayerVolume.getValue() / 100f }.getOrDefault(0.7f))
        private set
    var repeat by mutableStateOf(Repeat.entries[runCatching { AllSettings.cryoPlayerRepeat.getValue() }.getOrDefault(0).coerceIn(0, 2)])
        private set
    var shuffle by mutableStateOf(runCatching { AllSettings.cryoPlayerShuffle.getValue() }.getOrDefault(false))
        private set

    private var appContext: Context? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artJob: Job? = null
    private var resumeOnStart = false
    private var errorsInRow = 0
    /** Posição da música recuperada da sessão anterior (antes de o player ser criado). */
    private var pendingPositionMs = 0L
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    /** Salva a posição de tempos em tempos enquanto toca (para recuperar depois de um crash). */
    private val periodicSave = object : Runnable {
        override fun run() {
            if (player?.isPlaying == true) {
                saveSession()
                mainHandler.postDelayed(this, 15_000L)
            }
        }
    }

    /** Posição atual (ms). */
    fun positionMs(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: pendingPositionMs

    private fun itemsOf(songs: List<Song>) = songs.map { song ->
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(Uri.parse(song.uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build()
            )
            .build()
    }

    /**
     * Ao entrar no jogo de novo (com o Launcher ainda aberto): volta a música que estava
     * carregada, PAUSADA e na mesma posição. Nada toca sozinho e o player só é criado no play.
     */
    fun restoreSession(context: Context) {
        appContext = context.applicationContext
        if (queue.isNotEmpty()) return
        val saved = CryoMusicSession.load(context.applicationContext) ?: return
        queue = saved.songs
        index = saved.index
        pendingPositionMs = saved.positionMs
        durationMs = current?.durationMs ?: 0L
        playing = false
        loadLocalArt()
    }

    /** Cria o player para a fila recuperada (sem tocar). */
    private fun ensurePrepared(): ExoPlayer? {
        player?.let { return it }
        val context = appContext ?: return null
        if (queue.isEmpty()) return null
        val p = ensurePlayer(context)
        p.setMediaItems(itemsOf(queue), index.coerceIn(0, queue.lastIndex), pendingPositionMs)
        p.prepare()
        pendingPositionMs = 0L
        return p
    }

    /** Salva a fila e a posição (gravação fora da thread principal). */
    private fun saveSession() {
        val context = appContext ?: return
        val songs = queue
        if (songs.isEmpty()) return
        val idx = index
        val pos = positionMs()
        scope.launch(Dispatchers.IO) { CryoMusicSession.save(context, songs, idx, pos) }
    }

    private fun ensurePlayer(context: Context): ExoPlayer {
        appContext = context.applicationContext
        player?.let { return it }
        val app = context.applicationContext
        //só áudio: não cria os decodificadores de vídeo/legenda que um player comum carrega
        val renderers = RenderersFactory { handler, _, audioListener, _, _ ->
            arrayOf<Renderer>(MediaCodecAudioRenderer(app, MediaCodecSelector.DEFAULT, handler, audioListener))
        }
        //guarda pouco áudio adiantado (o padrão reserva memória pensando em vídeo)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
            .setTargetBufferBytes(3 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
        val p = ExoPlayer.Builder(app, renderers).setLoadControl(loadControl).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                //pede o foco de áudio: pausa o Spotify/YouTube quando o player do Launcher toca (e vice-versa)
                true
            )
            setHandleAudioBecomingNoisy(true)
            volume = this@CryoPlayer.volume
            applyModes(this)
            addListener(listener)
        }
        player = p
        return p
    }

    private fun applyModes(p: Player) {
        p.repeatMode = when (repeat) {
            Repeat.OFF -> Player.REPEAT_MODE_OFF
            Repeat.ALL -> Player.REPEAT_MODE_ALL
            Repeat.ONE -> Player.REPEAT_MODE_ONE
        }
        p.shuffleModeEnabled = shuffle
    }

    /** Discord: "Ouvindo: música" só enquanto toca (pausado, parado ou sem música = nada). */
    private fun updateDiscord() {
        val p = player
        val active = p != null && p.playWhenReady &&
            p.playbackState != Player.STATE_IDLE && p.playbackState != Player.STATE_ENDED
        val title = if (active) current?.title?.takeIf { it.isNotBlank() } else null
        runCatching { com.russoviller.cryogenic.discord.CryoDiscordPresence.setListening(title) }
    }

    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateDiscord()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing = isPlaying
            updateDiscord()
            mainHandler.removeCallbacks(periodicSave)
            if (isPlaying) {
                error = null
                errorsInRow = 0
                mainHandler.postDelayed(periodicSave, 15_000L)
            } else {
                saveSession()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            buffering = playbackState == Player.STATE_BUFFERING
            updateDiscord()
            if (playbackState == Player.STATE_READY) {
                player?.duration?.takeIf { it != C.TIME_UNSET && it > 0 }?.let { durationMs = it }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val p = player ?: return
            index = p.currentMediaItemIndex
            durationMs = current?.durationMs ?: 0L
            loadLocalArt()
            saveSession()
            updateDiscord()
        }

        override fun onPlayerError(e: PlaybackException) {
            Logger.warning(TAG, "Playback error: ${e.errorCodeName}", e)
            error = e.errorCodeName
            buffering = false
            val p = player ?: return
            //música que não abre: passa para a próxima (no máximo 3 seguidas, para não ficar em loop)
            errorsInRow++
            if (errorsInRow <= 3 && p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
                p.prepare()
                p.play()
            }
        }
    }

    /** Toca a lista a partir da música [start]. */
    fun playQueue(context: Context, songs: List<Song>, start: Int) {
        if (songs.isEmpty()) return
        val p = ensurePlayer(context)
        val items = itemsOf(songs)
        pendingPositionMs = 0L
        queue = songs
        index = start.coerceIn(0, songs.lastIndex)
        durationMs = current?.durationMs ?: 0L
        error = null
        errorsInRow = 0
        p.setMediaItems(items, index, 0L)
        p.prepare()
        p.play()
        loadLocalArt()
        saveSession()
    }

    fun togglePlay() {
        val p = player ?: ensurePrepared() ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_IDLE) p.prepare()
            if (p.playbackState == Player.STATE_ENDED) p.seekTo(0, 0L)
            p.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun next() {
        val p = player ?: ensurePrepared() ?: return
        if (p.hasNextMediaItem()) p.seekToNextMediaItem() else if (queue.isNotEmpty()) p.seekTo(0, 0L)
        p.play()
    }

    /** Voltar: no começo da música vai para a anterior; depois de 3 s volta ao início da atual. */
    fun previous() {
        val p = player ?: ensurePrepared() ?: return
        p.seekToPrevious()
        p.play()
    }

    fun seekTo(ms: Long) {
        val p = player
        if (p == null) pendingPositionMs = ms.coerceAtLeast(0L) else p.seekTo(ms.coerceAtLeast(0L))
    }

    fun setVolumeLevel(value: Float) {
        val v = value.coerceIn(0f, 1f)
        volume = v
        player?.volume = v
    }

    /** Salva o volume (chamar quando soltar a barra). */
    fun saveVolume() {
        AllSettings.cryoPlayerVolume.save((volume * 100).toInt())
    }

    fun cycleRepeat() {
        repeat = Repeat.entries[(repeat.ordinal + 1) % Repeat.entries.size]
        AllSettings.cryoPlayerRepeat.save(repeat.ordinal)
        player?.let { applyModes(it) }
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        AllSettings.cryoPlayerShuffle.save(shuffle)
        player?.let { applyModes(it) }
    }

    /** Capa embutida no arquivo local (MP3/FLAC/M4A...). */
    private fun loadLocalArt() {
        artJob?.cancel()
        localArt = null
        val song = current ?: return
        if (!song.local) return
        val context = appContext ?: return
        artJob = scope.launch {
            localArt = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, Uri.parse(song.uri))
                        retriever.embeddedPicture?.let { bytes ->
                            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 1 }
                            //lê só o tamanho primeiro; capas gigantes são reduzidas com qualidade
                            opts.inJustDecodeBounds = true
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                            val maxSide = 1024
                            var sample = 1
                            //só divide por 2 enquanto o resultado ainda ficar acima do tamanho final (sem perder nitidez)
                            while (opts.outWidth / (sample * 2) >= maxSide && opts.outHeight / (sample * 2) >= maxSide) sample *= 2
                            val real = android.graphics.BitmapFactory.Options().apply {
                                inSampleSize = sample
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val decoded: Bitmap? = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, real)
                            val bmp = decoded?.let { src ->
                                val side = maxOf(src.width, src.height)
                                if (side > maxSide) {
                                    val scale = maxSide.toFloat() / side
                                    Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
                                        .also { if (it !== src) src.recycle() }
                                } else src
                            }
                            bmp?.prepareToDraw()
                            bmp?.asImageBitmap()
                        }
                    } finally {
                        runCatching { retriever.release() }
                    }
                }.getOrNull()
            }
        }
    }

    /** O Launcher foi para segundo plano: pausa e lembra se estava tocando. */
    fun onBackground() {
        saveSession()
        val p = player ?: return
        resumeOnStart = p.isPlaying
        if (resumeOnStart) p.pause()
    }

    /** O Launcher voltou: continua se estava tocando antes. */
    fun onForeground() {
        if (resumeOnStart) player?.play()
        resumeOnStart = false
    }

    fun release() {
        mainHandler.removeCallbacks(periodicSave)
        //guarda onde parou: ao entrar no jogo de novo a música volta pausada neste ponto
        player?.let { pendingPositionMs = it.currentPosition.coerceAtLeast(0L) }
        saveSession()
        artJob?.cancel()
        runCatching { player?.removeListener(listener) }
        runCatching { player?.release() }
        player = null
        playing = false
        buffering = false
        updateDiscord()
    }

    /** Só para as prévias de tela nos testes. */
    @androidx.annotation.VisibleForTesting
    internal fun previewState(songs: List<Song>, index: Int, playing: Boolean) {
        queue = songs
        this.index = index
        this.playing = playing
        durationMs = songs.getOrNull(index)?.durationMs ?: 0L
    }
}
