package com.russoviller.cryogenic.live

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.view.Surface
import com.pedro.common.ConnectChecker
import com.pedro.encoder.audio.AudioEncoder
import com.pedro.encoder.audio.GetAudioData
import com.pedro.encoder.input.sources.OrientationConfig
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.utils.gl.AspectRatioMode
import com.pedro.library.view.GlStreamInterface
import com.pedro.rtmp.rtmp.RtmpClient
import com.russoviller.cryogenic.utils.logging.Logger
import java.nio.ByteBuffer

/**
 * Transmissão da tela com o menor custo possível para o jogo.
 *
 * **Modo direto** (o normal, durante o jogo): a tela espelhada (VirtualDisplay) vai direto para a
 * entrada do codificador de vídeo do aparelho. Nenhuma cópia de imagem passa pela GPU do app: é o
 * mesmo caminho dos gravadores de tela do sistema. O codificador descarta sozinho os quadros acima
 * do FPS da live ([KEY_MAX_FPS]) e repete o último quadro quando a tela fica parada.
 *
 * **Modo GL** (só quando precisa): para a prévia da live no launcher e para a tela bloqueada
 * (privacidade), a imagem passa pelo [GlStreamInterface] do RootEncoder, como antes.
 *
 * Envio por RTMP/RTMPS com o [RtmpClient] do RootEncoder; áudio com o [AudioEncoder] (AAC).
 */
class CryoScreenStream(
    private val context: Context,
    connectChecker: ConnectChecker,
    private val audioSource: AudioSource
) {
    private val rtmp = RtmpClient(connectChecker)
    private val control = HandlerThread("CryoLiveControl").apply { start() }
    private val controlHandler = Handler(control.looper)
    private val encoderThread = HandlerThread("CryoLiveEncoder", Process.THREAD_PRIORITY_DEFAULT).apply { start() }

    private var width = 0
    private var height = 0
    private var fps = 30
    private var targetBitrate = 0
    @Volatile private var currentBitrate = 0

    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var display: VirtualDisplay? = null
    private var projection: MediaProjection? = null

    private val audioEncoder = AudioEncoder(object : GetAudioData {
        override fun getAudioData(audioBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            if (streaming) rtmp.sendAudio(audioBuffer, info)
        }
        override fun onAudioFormat(mediaFormat: MediaFormat) {}
    })

    @Volatile var streaming = false
        private set

    /** início da live: relógio do RootEncoder (elapsedRealtime) e o das imagens da tela (nanoTime) */
    private var startElapsedUs = 0L
    /** 1º quadro: tempo dele no relógio da tela e no relógio da live (ver [videoPts]) */
    private var firstFrameSrcUs = -1L
    private var firstFrameLiveUs = 0L
    private var lastVideoPts = -1L
    @Volatile private var spsSent = false
    @Volatile private var framesOut = 0L

    // ---------------------------------------------------------------- modo GL (prévia / privacidade)

    private var gl: GlStreamInterface? = null
    @Volatile private var glWanted = false
    @Volatile private var glActive = false
    /** o modo direto falhou neste aparelho: fica no GL até o fim da live */
    @Volatile private var directBroken = false
    private val glReady = mutableListOf<(GlStreamInterface) -> Unit>()

    /** "direct" ou "gl" (diagnóstico) */
    val mode: String get() = if (glActive) "gl" else "direct"

    // ---------------------------------------------------------------- preparar / iniciar

    fun prepare(width: Int, height: Int, fps: Int, bitrate: Int, sampleRate: Int, stereo: Boolean, audioBitrate: Int): Boolean {
        this.width = width
        this.height = height
        this.fps = fps
        this.targetBitrate = bitrate
        this.currentBitrate = bitrate
        if (!prepareVideo()) return false
        if (!audioSource.init(sampleRate, stereo, false, false)) return false
        rtmp.setAudioInfo(sampleRate, stereo)
        return audioEncoder.prepareAudioEncoder(audioBitrate, sampleRate, stereo)
    }

    private fun prepareVideo(): Boolean {
        releaseCodec()
        val info = chooseEncoder() ?: return false
        val base = {
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, currentBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                val caps = runCatching { info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC) }.getOrNull()
                val cbr = caps?.encoderCapabilities?.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR) == true
                setInteger(
                    MediaFormat.KEY_BITRATE_MODE,
                    if (cbr) MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR else MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                )
            }
        }
        // extras que deixam o codificador leve; se o aparelho recusar algum, tenta sem eles
        val tuned = base().apply {
            // a tela pode gerar 120 quadros/s com o jogo a 120 FPS: o codificador só usa o FPS da live
            setFloat(KEY_MAX_FPS, fps.toFloat())
            // tela parada (menu/pausa): repete o último quadro para a live não congelar
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L)
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            if (Build.VERSION.SDK_INT >= 29) setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
            highProfile(info)?.let { (profile, level) ->
                setInteger(MediaFormat.KEY_PROFILE, profile)
                setInteger(MediaFormat.KEY_LEVEL, level)
            }
        }
        for (format in listOf(tuned, base().apply { setFloat(KEY_MAX_FPS, fps.toFloat()); setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L) }, base())) {
            val c = runCatching { MediaCodec.createByCodecName(info.name) }.getOrNull() ?: return false
            val ok = runCatching {
                c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = c.createInputSurface()
            }.onFailure { Logger.warning(TAG, "Encoder config rejected (${it.message}), trying simpler") }.isSuccess
            if (ok) {
                codec = c
                Logger.info(TAG, "Video encoder ${info.name} ${width}x$height@$fps ${currentBitrate / 1000}kbps")
                return true
            }
            runCatching { c.release() }
        }
        return false
    }

    fun start(url: String, mediaProjection: MediaProjection) {
        if (streaming) return
        projection = mediaProjection
        streaming = true
        spsSent = false
        lastVideoPts = -1L
        framesOut = 0L
        rtmp.setVideoResolution(width, height)
        rtmp.setFps(fps)
        rtmp.setReTries(10)
        rtmp.connect(url)
        startElapsedUs = SystemClock.elapsedRealtimeNanos() / 1000
        firstFrameSrcUs = -1L
        startVideoCodec()
        audioSource.start(audioEncoder)
        audioEncoder.start(startElapsedUs)
        val dpi = context.resources.displayMetrics.densityDpi
        display = mediaProjection.createVirtualDisplay(
            "CryoLive", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            if (glWanted) null else inputSurface, null, controlHandler
        ) ?: throw IllegalStateException("Virtual display failed")
        if (glWanted) controlHandler.post { switchToGl() }
        controlHandler.postDelayed(watchdog, WATCHDOG_MS)
    }

    private fun startVideoCodec() {
        val c = codec ?: return
        c.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                try {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) handleOutput(buffer, info)
                } catch (t: Throwable) {
                    Logger.warning(TAG, "Video output failed: ${t.message}")
                } finally {
                    runCatching { codec.releaseOutputBuffer(index, false) }
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Logger.error(TAG, "Video encoder error", e)
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                val sps = format.getByteBuffer("csd-0")
                val pps = format.getByteBuffer("csd-1")
                if (sps != null) {
                    rtmp.setVideoInfo(sps.duplicate(), pps?.duplicate(), null)
                    spsSent = true
                }
            }
        }, Handler(encoderThread.looper))
        c.start()
    }

    private fun handleOutput(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            // sps/pps sem onOutputFormatChanged (raro): separa pelos códigos de início
            if (!spsSent) splitConfig(buffer, info)?.let { (sps, pps) ->
                rtmp.setVideoInfo(sps, pps, null)
                spsSent = true
            }
            return
        }
        if (!streaming) return
        framesOut++
        var pts = videoPts(info.presentationTimeUs)
        if (pts <= lastVideoPts) pts = lastVideoPts + 1
        lastVideoPts = pts
        info.presentationTimeUs = pts
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        rtmp.sendVideo(buffer, info)
    }

    /**
     * Tempo do quadro na linha do tempo da live (a mesma do áudio): mantém o espaçamento real entre
     * os quadros (tempo de captura) e ancora o primeiro no relógio da live, seja qual for o relógio
     * que a tela/GL usa. Se os dois se afastarem (troca de modo, pausa longa), ancora de novo.
     */
    private fun videoPts(srcUs: Long): Long {
        val elapsedNow = SystemClock.elapsedRealtimeNanos() / 1000
        val liveNow = elapsedNow - startElapsedUs
        // normal: a tela usa o relógio monotônico; converte para o da live (mesmo do áudio)
        val direct = srcUs + (elapsedNow - System.nanoTime() / 1000) - startElapsedUs
        if (kotlin.math.abs(direct - liveNow) < 1_000_000L) return direct
        if (firstFrameSrcUs < 0) {
            firstFrameSrcUs = srcUs
            firstFrameLiveUs = liveNow
        }
        var pts = firstFrameLiveUs + (srcUs - firstFrameSrcUs)
        if (kotlin.math.abs(pts - liveNow) > 500_000L) {
            firstFrameSrcUs = srcUs
            firstFrameLiveUs = liveNow
            pts = liveNow
        }
        return pts
    }

    // ---------------------------------------------------------------- troca de modo

    /** Liga o modo GL (prévia/privacidade) ou volta ao direto. [onReady] roda quando o GL estiver pronto. */
    fun setGlNeeded(needed: Boolean, onReady: ((GlStreamInterface) -> Unit)? = null) {
        controlHandler.post {
            glWanted = needed || directBroken
            if (onReady != null) {
                val g = gl
                if (glActive && g != null) runCatching { onReady(g) } else if (glWanted) glReady += onReady
            }
            if (display == null) return@post
            if (glWanted && !glActive) switchToGl() else if (!glWanted && glActive) switchToDirect()
        }
    }

    /** Roda com o GL se ele estiver ativo agora (filtros da privacidade). */
    fun withGl(block: (GlStreamInterface) -> Unit) {
        controlHandler.post { gl?.takeIf { glActive }?.let { g -> runCatching { block(g) } } }
    }

    private fun switchToGl() {
        val vd = display ?: return
        val surface = inputSurface ?: return
        val g = gl ?: GlStreamInterface(context).also { gl = it }
        runCatching {
            g.setEncoderSize(width, height)
            g.setOrientationConfig(OrientationConfig(cameraOrientation = 0, isPortrait = height > width))
            g.autoHandleOrientation = false
            g.setCameraOrientation(0)
            g.setAspectRatioMode(AspectRatioMode.Adjust)
            g.start()
            g.setForceRender(true, fps)
            g.forceFpsLimit(fps)
            // a tela passa a desenhar na textura do GL; o codificador é liberado para o GL
            vd.setSurface(Surface(g.surfaceTexture))
        }.onFailure {
            Logger.error(TAG, "GL mode failed", it)
            return
        }
        glActive = true
        attachEncoderToGl(g, surface, 0)
    }

    /** O sistema solta a entrada do codificador logo depois do setSurface: tenta algumas vezes. */
    private fun attachEncoderToGl(g: GlStreamInterface, surface: Surface, attempt: Int) {
        controlHandler.postDelayed({
            if (!glActive || gl !== g) return@postDelayed
            val ok = runCatching { g.addMediaCodecSurface(surface) }.isSuccess
            if (ok) {
                val callbacks = glReady.toList()
                glReady.clear()
                callbacks.forEach { cb -> runCatching { cb(g) } }
            } else if (attempt < 8) {
                attachEncoderToGl(g, surface, attempt + 1)
            } else {
                Logger.error(TAG, "Could not attach the encoder to GL")
            }
        }, if (attempt == 0) 150L else 250L)
    }

    private fun switchToDirect() {
        val vd = display ?: return
        val surface = inputSurface ?: return
        val g = gl
        glActive = false
        runCatching { g?.deAttachPreview() }
        // solta a entrada do codificador (o EGL desconecta na hora) e devolve para a tela
        runCatching { g?.removeMediaCodecSurface() }
        runCatching { vd.setSurface(surface) }.onFailure { Logger.error(TAG, "Direct mode failed", it) }
        val before = framesOut
        controlHandler.postDelayed({
            if (gl === g && !glActive) {
                runCatching { g?.stop() }
                gl = null
            }
        }, 400L)
        // confere se os quadros voltaram; se não, este aparelho não aceita a troca: fica no GL
        controlHandler.postDelayed({
            if (!glActive && streaming && framesOut == before) {
                Logger.warning(TAG, "No frames after switching to direct mode: staying on GL")
                directBroken = true
                glWanted = true
                switchToGl()
            }
        }, 2_500L)
    }

    /** Sem quadros por muito tempo (tela desconectada?): reconecta a entrada. */
    private var lastFramesSeen = 0L
    private val watchdog = object : Runnable {
        override fun run() {
            if (!streaming) return
            val now = framesOut
            if (now == lastFramesSeen) {
                Logger.warning(TAG, "No video frames in ${WATCHDOG_MS}ms (mode=$mode)")
                if (glActive) {
                    gl?.let { g -> inputSurface?.let { s -> runCatching { g.removeMediaCodecSurface() }; attachEncoderToGl(g, s, 1) } }
                } else {
                    display?.let { vd ->
                        runCatching { vd.setSurface(null) }
                        controlHandler.postDelayed({ if (!glActive) runCatching { vd.setSurface(inputSurface) } }, 300L)
                    }
                }
            }
            lastFramesSeen = now
            controlHandler.postDelayed(this, WATCHDOG_MS)
        }
    }

    // ---------------------------------------------------------------- prévia

    fun attachPreview(surface: Surface, w: Int, h: Int) {
        setGlNeeded(true) { g ->
            g.setPreviewResolution(w, h)
            g.attachPreview(surface)
        }
    }

    fun detachPreview(stillNeedGl: Boolean) {
        controlHandler.post { runCatching { gl?.deAttachPreview() } }
        setGlNeeded(stillNeedGl)
    }

    // ---------------------------------------------------------------- bitrate / rede

    fun requestKeyframe() {
        runCatching { codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }) }
    }

    private fun applyBitrate(bps: Int) {
        if (bps == currentBitrate) return
        currentBitrate = bps
        runCatching { codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps) }) }
    }

    /**
     * Chamado a cada medição de envio (~1 s): com a rede engasgando, baixa o bitrate (a live
     * atrasaria e cairia); quando a fila esvazia, volta aos poucos ao bitrate escolhido.
     */
    fun onBitrateTick() {
        if (!streaming || targetBitrate <= 0) return
        val min = (targetBitrate * 0.35f).toInt()
        val next = if (rtmp.hasCongestion()) (currentBitrate * 0.8f).toInt().coerceAtLeast(min)
        else (currentBitrate * 1.08f).toInt().coerceAtMost(targetBitrate)
        applyBitrate(next)
    }

    fun shouldRetry(reason: String): Boolean = rtmp.shouldRetry(reason)

    fun reConnect(delayMs: Long) = rtmp.reConnect(delayMs)

    // ---------------------------------------------------------------- parar

    fun stop() {
        if (!streaming && display == null) return
        streaming = false
        controlHandler.removeCallbacksAndMessages(null)
        runCatching { rtmp.disconnect() }
        runCatching { display?.release() }
        display = null
        runCatching { gl?.removeMediaCodecSurface() }
        runCatching { gl?.stop() }
        gl = null
        glActive = false
        glReady.clear()
        runCatching { audioSource.stop() }
        runCatching { audioEncoder.stop() }
        releaseCodec()
        projection = null
    }

    fun release() {
        stop()
        runCatching { audioSource.release() }
        control.quitSafely()
        encoderThread.quitSafely()
    }

    private fun releaseCodec() {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
        runCatching { inputSurface?.release() }
        inputSurface = null
    }

    // ---------------------------------------------------------------- utilidades

    private fun chooseEncoder(): MediaCodecInfo? {
        val all = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) }
        }
        fun isSoftware(i: MediaCodecInfo): Boolean {
            if (Build.VERSION.SDK_INT >= 29) return i.isSoftwareOnly
            val n = i.name.lowercase()
            return n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.contains(".sw.")
        }
        // codificador de hardware: não usa a CPU que o jogo precisa
        return all.firstOrNull { !isSoftware(it) } ?: all.firstOrNull()
    }

    private fun highProfile(info: MediaCodecInfo): Pair<Int, Int>? = runCatching {
        val levels = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).profileLevels
        val high = levels.filter { it.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh }
        val level = high.maxOfOrNull { it.level } ?: return@runCatching null
        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh to level
    }.getOrNull()

    private fun splitConfig(buffer: ByteBuffer, info: MediaCodec.BufferInfo): Pair<ByteBuffer, ByteBuffer?>? {
        val bytes = ByteArray(info.size)
        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.get(bytes, 0, info.size)
        val starts = ArrayList<Int>()
        var i = 0
        while (i + 3 < bytes.size) {
            if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte() && bytes[i + 2] == 0.toByte() && bytes[i + 3] == 1.toByte()) {
                starts += i
                i += 4
            } else i++
        }
        if (starts.isEmpty()) return null
        val nals = starts.mapIndexed { idx, s -> bytes.copyOfRange(s, starts.getOrElse(idx + 1) { bytes.size }) }
        val sps = nals.firstOrNull { it.size > 4 && (it[4].toInt() and 0x1f) == 7 } ?: return null
        val pps = nals.firstOrNull { it.size > 4 && (it[4].toInt() and 0x1f) == 8 }
        return ByteBuffer.wrap(sps) to pps?.let { ByteBuffer.wrap(it) }
    }

    companion object {
        private const val TAG = "CryoScreenStream"
        /** MediaFormat.KEY_MAX_FPS_TO_ENCODER (existe desde o Android 10; o nome funciona antes também) */
        private const val KEY_MAX_FPS = "max-fps-to-encoder"
        private const val WATCHDOG_MS = 4_000L
    }
}
