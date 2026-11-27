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

package com.russoviller.cryogenic.ui.components

import androidx.media3.exoplayer.DefaultLoadControl
import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.FloatRange
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.russoviller.cryogenic.R

/**
 * 简单的沉浸式视频播放层
 * @param autoPlay 准备好后，是否立即播放视频
 * @param loop 是否循环播放视频
 * @param muted 是否开启静音
 * @param volume 音量
 * @param useSurfaceView Desenha o vídeo em uma SurfaceView (camada própria, abaixo da janela) em vez de
 * uma TextureView. O vídeo continua rodando liso mesmo quando a interface está ocupada (troca de
 * página, abertura de janelas). Só serve para vídeo em tela cheia sem desfoque, recorte arredondado
 * nem transparência aplicados nele.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    muted: Boolean = true,
    @FloatRange(from = 0.0, to = 1.0) volume: Float = if (muted) 0.0f else 1.0f,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    refreshTrigger: Any? = null,
    useSurfaceView: Boolean = false
) {
    val context = LocalContext.current
    val player = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true) //开启解码器 fallback

        //Wallpaper é arquivo local em loop: um buffer de poucos segundos basta.
        //O padrão do ExoPlayer (até 50 s de vídeo) podia segurar mais de 100 MB de RAM por vídeo.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_500, 4_000, 500, 1_000)
            .setTargetBufferBytes(6 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttr, false)

            this.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            this.volume = volume
        }
    }

    val playerView = remember {
        val playerView = LayoutInflater.from(context)
            .inflate(
                if (useSurfaceView) R.layout.player_surface_view else R.layout.player_texture_view,
                null
            ) as PlayerView

        playerView.apply {
            this.player = player
            this.resizeMode = resizeMode
        }
    }

    LaunchedEffect(videoUri, refreshTrigger) {
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    LaunchedEffect(autoPlay) {
        player.playWhenReady = autoPlay
    }

    LaunchedEffect(loop) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    //Fade do som dos wallpapers durante a cutscene do Liquid Glass: muda só o volume do player
    //(sem reativar/desativar a faixa de áudio a cada quadro)
    val currentVolume = androidx.compose.runtime.rememberUpdatedState(volume)
    LaunchedEffect(player) {
        androidx.compose.runtime.snapshotFlow { CryoInterface2.audioScale.floatValue }
            .collect { scale -> player.volume = currentVolume.value * scale }
    }

    LaunchedEffect(muted, volume) {
        player.volume = volume * CryoInterface2.audioScale.floatValue
        //Volume zero: não decodifica o áudio do vídeo (menos CPU e RAM).
        //Vale só o volume: o fundo do Launcher passa o volume sem mexer em "muted".
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, volume <= 0f)
            .build()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAutoPlayState = androidx.compose.runtime.rememberUpdatedState(autoPlay)

    //Se o decodificador der erro (ex.: o sistema tomou o decodificador de vídeo enquanto o jogo
    //estava aberto), o ExoPlayer fica parado em IDLE e o wallpaper congelava até reabrir o Launcher.
    //Agora ele tenta preparar de novo sozinho.
    DisposableEffect(player) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var retries = 0
        val retry = Runnable {
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0 &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            ) {
                player.prepare()
                player.playWhenReady = currentAutoPlayState.value
            }
        }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (retries < 6) {
                    handler.removeCallbacks(retry)
                    handler.postDelayed(retry, 600L * (retries + 1))
                    retries++
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) retries = 0
            }
        }
        player.addListener(listener)
        onDispose {
            handler.removeCallbacks(retry)
            player.removeListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    //Volta do segundo plano: recria o decodificador e continua de onde parou
                    if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
                        player.prepare()
                    }
                    player.playWhenReady = currentAutoPlayState.value
                }
                Lifecycle.Event.ON_STOP -> {
                    //Em segundo plano (ex.: jogando) solta o decodificador de vídeo e o buffer,
                    //em vez de só pausar e deixar tudo ocupando RAM e o hardware de vídeo
                    player.playWhenReady = false
                    player.stop()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            //release() trava a thread principal até o decodificador ser solto. Como isto roda no meio
            //da troca de página, aqui só paramos o player (não bloqueia) e deixamos o release() para
            //quando a interface estiver parada.
            playerView.player = null
            player.playWhenReady = false
            player.stop()
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.postDelayed({
                android.os.Looper.myQueue().addIdleHandler {
                    runCatching { player.release() }
                    false
                }
            }, 900L)
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = modifier.fillMaxSize()
    )
}
