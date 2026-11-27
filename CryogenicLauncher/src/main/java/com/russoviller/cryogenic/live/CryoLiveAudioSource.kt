package com.russoviller.cryogenic.live

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import com.pedro.encoder.Frame
import com.pedro.encoder.input.audio.GetMicrophoneData
import com.pedro.encoder.input.sources.audio.AudioSource
import com.russoviller.cryogenic.utils.logging.Logger

/**
 * Áudio da live: som do celular (jogo, Voice Chat dos outros) + microfone + a voz que o próprio
 * jogo capturou para o Voice Chat.
 *
 * O Android entrega o microfone a um capturador por vez: com o Voice Chat ligado, o microfone da
 * live pode passar a receber só silêncio. Por isso o jogo também manda para cá o que o Voice Chat
 * capturou ([injectVoice]), e a live usa essa voz quando:
 * - o microfone é o automático/do celular (é o mesmo microfone; evita voz duplicada), ou
 * - o microfone escolhido (fone Bluetooth/com fio) está entregando só silêncio.
 */
@RequiresApi(29)
class CryoLiveAudioSource(
    private val projection: MediaProjection,
    private val useMic: Boolean,
    /** true = microfone do fone escolhido: a voz do jogo só entra se ele estiver mudo */
    private val explicitMic: Boolean,
    /** microfone preferido (null = o do sistema) */
    private val micDevice: AudioDeviceInfo?,
    /** fone Bluetooth: captura no modo de comunicação (rota do fone) */
    private val communication: Boolean
) : AudioSource() {

    @Volatile var micVolume = 1f
    @Volatile var internalVolume = 1f

    /** A voz do Voice Chat está entrando na live agora. */
    @Volatile var voiceShared = false
        private set

    private var internalRecord: AudioRecord? = null
    private var micRecord: AudioRecord? = null
    @Volatile private var running = false
    private var mixThread: Thread? = null
    private var micThread: Thread? = null
    private var target: GetMicrophoneData? = null
    private var channels = 2

    private val micRing = VoiceRing()
    private val voiceRing = VoiceRing()
    private var micJitter = JitterBuffer(micRing, 1, 1, 1)
    private var voiceJitter = JitterBuffer(voiceRing, 1, 1, 1)
    private val voiceResampler = LinearResampler()
    @Volatile private var lastVoiceAt = 0L
    @Volatile private var lastMicSoundAt = 0L

    @SuppressLint("MissingPermission")
    override fun create(sampleRate: Int, isStereo: Boolean, echoCanceler: Boolean, noiseSuppressor: Boolean): Boolean {
        channels = if (isStereo) 2 else 1
        micRing.capacity = sampleRate / 2
        voiceRing.capacity = sampleRate / 2
        // espera juntar um pouco antes de tocar: atrasos pequenos na entrega não picotam a voz
        micJitter = JitterBuffer(micRing, prebuffer = ms(sampleRate, 40), maxLatency = ms(sampleRate, 200), keepLatency = ms(sampleRate, 60))
        voiceJitter = JitterBuffer(voiceRing, prebuffer = ms(sampleRate, 80), maxLatency = ms(sampleRate, 300), keepLatency = ms(sampleRate, 120))
        val inMask = if (isStereo) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val min = AudioRecord.getMinBufferSize(sampleRate, inMask, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(8192)
        return runCatching {
            val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            internalRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(inMask)
                        .build()
                )
                .setBufferSizeInBytes(min * 4)
                .build()
            if (internalRecord?.state != AudioRecord.STATE_INITIALIZED) error("internal capture not initialized")
            if (useMic) {
                val monoMin = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    .coerceAtLeast(4096)
                val source = if (communication) MediaRecorder.AudioSource.VOICE_COMMUNICATION else MediaRecorder.AudioSource.MIC
                micRecord = runCatching {
                    AudioRecord(source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, monoMin * 4)
                        .takeIf { it.state == AudioRecord.STATE_INITIALIZED }
                }.getOrNull()
                micDevice?.let { d -> runCatching { micRecord?.setPreferredDevice(d) } }
                if (micRecord == null) Logger.warning(TAG, "Microphone not available; live continues with internal audio")
            }
            true
        }.getOrElse {
            Logger.error(TAG, "Live audio create failed", it)
            release()
            false
        }
    }

    override fun start(getMicrophoneData: GetMicrophoneData) {
        if (running) return
        target = getMicrophoneData
        val internal = internalRecord ?: return
        running = true
        runCatching { internal.startRecording() }
        micRecord?.let { mic ->
            runCatching { mic.startRecording() }
            micThread = Thread({ micLoop(mic) }, "CryoLiveMic").apply { start() }
        }
        mixThread = Thread({ mixLoop(internal) }, "CryoLiveAudio").apply { start() }
    }

    override fun stop() {
        running = false
        runCatching { mixThread?.join(500) }
        runCatching { micThread?.join(500) }
        mixThread = null
        micThread = null
        runCatching { internalRecord?.stop() }
        runCatching { micRecord?.stop() }
        micJitter.reset()
        voiceJitter.reset()
        voiceShared = false
    }

    override fun isRunning(): Boolean = running

    override fun release() {
        if (running) stop()
        runCatching { internalRecord?.release() }
        runCatching { micRecord?.release() }
        internalRecord = null
        micRecord = null
    }

    /** Voz do Voice Chat (PCM 16 bits mono, little-endian) vinda do jogo. */
    fun injectVoice(pcm: ByteArray, rate: Int) {
        if (!running || pcm.size < 2) return
        val n = pcm.size / 2
        val samples = ShortArray(n) { i -> ((pcm[i * 2].toInt() and 0xFF) or (pcm[i * 2 + 1].toInt() shl 8)).toShort() }
        val resampled = synchronized(voiceResampler) { voiceResampler.process(samples, rate, sampleRate) }
        voiceRing.push(resampled, resampled.size)
        lastVoiceAt = System.nanoTime()
    }

    /** Troca o microfone preferido durante a live. */
    fun setMicDevice(device: AudioDeviceInfo?) {
        runCatching { micRecord?.setPreferredDevice(device) }
    }

    private fun micLoop(mic: AudioRecord) {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
        val buffer = ShortArray(sampleRate / 50)
        while (running) {
            val n = runCatching { mic.read(buffer, 0, buffer.size) }.getOrDefault(-1)
            if (n <= 0) {
                if (n < 0) Thread.sleep(10)
                continue
            }
            for (i in 0 until n) {
                if (buffer[i].toInt() != 0) {
                    lastMicSoundAt = System.nanoTime()
                    break
                }
            }
            micRing.push(buffer, n)
        }
    }

    private fun mixLoop(internal: AudioRecord) {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
        val frames = sampleRate / 50 // 20 ms
        val internalBuf = ShortArray(frames * channels)
        val micBuf = ShortArray(frames)
        val voiceBuf = ShortArray(frames)
        val out = ByteArray(frames * channels * 2)
        while (running) {
            val read = runCatching { internal.read(internalBuf, 0, internalBuf.size, AudioRecord.READ_BLOCKING) }.getOrDefault(-1)
            if (read <= 0) {
                if (read < 0) Thread.sleep(10)
                continue
            }
            val frameCount = read / channels
            val now = System.nanoTime()
            val voiceActive = now - lastVoiceAt < VOICE_ACTIVE_NS
            val micSilent = micRecord == null || now - lastMicSoundAt > MIC_SILENT_NS
            val useVoice = voiceActive && (!explicitMic || micSilent)
            voiceShared = useVoice

            val micGot = micJitter.read(micBuf, frameCount)
            val voiceGot = voiceJitter.read(voiceBuf, frameCount)
            val mv = micVolume
            val iv = internalVolume
            for (f in 0 until frameCount) {
                val voice = if (useVoice) (if (f < voiceGot) voiceBuf[f].toInt() else 0)
                else (if (f < micGot) micBuf[f].toInt() else 0)
                val v = voice * mv
                for (c in 0 until channels) {
                    val idx = f * channels + c
                    val mixed = (internalBuf[idx] * iv + v).toInt().coerceIn(-32768, 32767)
                    out[idx * 2] = mixed.toByte()
                    out[idx * 2 + 1] = (mixed shr 8).toByte()
                }
            }
            val size = frameCount * channels * 2
            runCatching {
                target?.inputPCMData(Frame(out.copyOf(size), 0, size, System.nanoTime() / 1000))
            }
        }
    }

    companion object {
        private const val TAG = "CryoLiveAudio"
        /** voz do jogo recebida há menos de 1 s = Voice Chat capturando */
        private const val VOICE_ACTIVE_NS = 1_000_000_000L

        private fun ms(rate: Int, millis: Int) = (rate.toLong() * millis / 1000).toInt()
        /** microfone da live sem nenhum som há 600 ms = o Android silenciou ele */
        private const val MIC_SILENT_NS = 600_000_000L
    }
}

/** Fila curta de amostras mono (descarta as mais antigas para não acumular atraso). */
class VoiceRing {
    @Volatile var capacity: Int = 22050
    private var data = ShortArray(0)
    private var size = 0

    @Synchronized
    fun push(samples: ShortArray, count: Int) {
        val n = count.coerceAtMost(samples.size)
        if (n <= 0) return
        if (data.size < capacity) data = data.copyOf(capacity)
        if (n >= capacity) {
            System.arraycopy(samples, n - capacity, data, 0, capacity)
            size = capacity
            return
        }
        val overflow = size + n - capacity
        if (overflow > 0) {
            System.arraycopy(data, overflow, data, 0, size - overflow)
            size -= overflow
        }
        System.arraycopy(samples, 0, data, size, n)
        size += n
    }

    /** Copia até [count] amostras para [out]; retorna quantas havia. */
    @Synchronized
    fun pull(out: ShortArray, count: Int): Int {
        val n = minOf(count, size, out.size)
        if (n <= 0) return 0
        System.arraycopy(data, 0, out, 0, n)
        System.arraycopy(data, n, data, 0, size - n)
        size -= n
        return n
    }

    @Synchronized
    fun available(): Int = size

    /** Descarta as [n] amostras mais antigas (atraso acumulado). */
    @Synchronized
    fun skip(n: Int) {
        val k = n.coerceIn(0, size)
        if (k == 0) return
        System.arraycopy(data, k, data, 0, size - k)
        size -= k
    }

    @Synchronized
    fun clear() {
        size = 0
    }
}

/** Reamostragem linear (ex.: 48000 Hz do Voice Chat -> 44100 Hz da live), com continuidade entre blocos. */
class LinearResampler {
    private var position = 0.0
    private var last: Short = 0
    private var lastRate = 0

    fun process(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        if (input.isEmpty() || fromRate <= 0 || toRate <= 0) return ShortArray(0)
        if (fromRate != lastRate) {
            position = 0.0
            lastRate = fromRate
        }
        if (fromRate == toRate) {
            last = input.last()
            return input.copyOf()
        }
        val step = fromRate.toDouble() / toRate
        val out = ShortArray(((input.size - position) / step).toInt() + 2)
        var count = 0
        // position é relativa ao início deste bloco; -1 = última amostra do bloco anterior
        var pos = position
        while (pos < input.size - 1 && count < out.size) {
            val i = kotlin.math.floor(pos).toInt()
            val frac = pos - i
            val a = if (i < 0) last.toInt() else input[i].toInt()
            val b = input[i + 1].toInt()
            out[count++] = (a + (b - a) * frac).toInt().toShort()
            pos += step
        }
        position = pos - input.size
        last = input.last()
        return out.copyOf(count)
    }
}

/**
 * Leitura com pré-buffer (jitter buffer): só começa a tocar com [prebuffer] amostras guardadas; se
 * acabar (atraso na entrega), suaviza o corte e volta a juntar. Também corta o atraso acumulado
 * acima de [maxLatency], voltando para [keepLatency].
 */
class JitterBuffer(
    private val ring: VoiceRing,
    private val prebuffer: Int,
    private val maxLatency: Int,
    private val keepLatency: Int
) {
    private var playing = false
    private var fadeIn = 0

    fun read(out: ShortArray, count: Int): Int {
        val available = ring.available()
        if (!playing) {
            if (available < prebuffer) return 0
            playing = true
            fadeIn = FADE
        }
        if (available > maxLatency) ring.skip(available - keepLatency)
        val got = ring.pull(out, count)
        if (fadeIn > 0) {
            val n = minOf(fadeIn, got)
            for (i in 0 until n) {
                val step = FADE - fadeIn + i
                out[i] = (out[i] * step / FADE).toShort()
            }
            fadeIn -= n
        }
        if (got < count) {
            // acabou: termina suave (sem estalo) e volta a juntar
            val n = minOf(FADE, got)
            for (i in 0 until n) {
                val idx = got - n + i
                out[idx] = (out[idx] * (n - i) / (n + 1)).toShort()
            }
            playing = false
        }
        return got
    }

    fun reset() {
        playing = false
        fadeIn = 0
        ring.clear()
    }

    private companion object {
        const val FADE = 64
    }
}
