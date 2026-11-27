package com.russoviller.cryogenic.music

/**
 * "O que está tocando agora" no player do Launcher.
 * O painel e o player flutuante leem tudo daqui.
 */
object CryoNowPlaying {
    enum class Source { PLAYER }

    val source: Source? get() = if (CryoPlayer.current != null) Source.PLAYER else null

    val key: String? get() = CryoPlayer.current?.id
    val title: String? get() = CryoPlayer.current?.title
    val artist: String? get() = CryoPlayer.current?.artist

    /** Capa embutida no arquivo da música (ou null). */
    val art: Any? get() = CryoPlayer.localArt
    val artSmall: Any? get() = CryoPlayer.localArt

    val playing: Boolean get() = CryoPlayer.playing
    val buffering: Boolean get() = CryoPlayer.buffering
    val durationMs: Long get() = CryoPlayer.durationMs

    fun positionMs(): Long = CryoPlayer.positionMs()
    fun togglePlay() = CryoPlayer.togglePlay()
    fun next() = CryoPlayer.next()
    fun previous() = CryoPlayer.previous()
    fun seekTo(ms: Long) = CryoPlayer.seekTo(ms)

    val volumeAvailable: Boolean get() = true

    /** Volume só da música, de 0 a 1 (não muda o som do jogo). */
    val volume: Float get() = CryoPlayer.volume
    fun setVolume(value: Float) = CryoPlayer.setVolumeLevel(value)
    fun saveVolume() = CryoPlayer.saveVolume()
}
