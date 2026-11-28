package com.russoviller.cryogenic.live

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.audio.MixAudioSource
import com.pedro.encoder.input.sources.audio.SilenceAudioSource
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.utils.logging.Logger
import java.io.File

/**
 * Live do TikTok (Painel da Live): captura a tela inteira (MediaProjection), o som interno do aparelho e o
 * microfone, codifica em H.264/AAC e envia por RTMP. Roda em primeiro plano no processo do launcher, então a
 * live continua quando o jogo está aberto, quando o usuário volta ao launcher ou sai do app.
 *
 * Launcher e jogo controlam a live por [LiveClient] (Messenger entre processos).
 */
class LiveService : Service(), ConnectChecker {
    companion object {
        const val ACTION_START = "com.russoviller.cryogenic.live.START"
        const val ACTION_STOP = "com.russoviller.cryogenic.live.STOP"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        private const val CHANNEL_ID = "cryo_live"
        private const val NOTIFICATION_ID = 7301
        private const val TAG = "LiveService"
        private const val MAX_CHAT = 150
        // 48 kHz: a taxa do próprio aparelho (sem reamostragem do som interno nem da voz do jogo)
        private const val AUDIO_SAMPLE_RATE = 48000
        private const val AUDIO_BITRATE = 128 * 1000

        fun hasMicPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private val main = Handler(Looper.getMainLooper())
    private val clients = LinkedHashMap<IBinder, Messenger>()
    private val foregroundClients = HashMap<String, Boolean>()

    private var config = LiveConfig()
    private var status = LiveStatus.IDLE
    private var error: String? = null
    private var startedAt = 0L
    private var bitrateKbps = 0L
    private var micOn = true
    private var hasMic = false
    private var micVolume = 100
    private var internalVolume = 100
    private var privacy = false
    private var chatState = ""
    private var chatDetail = ""
    private var viewers = ""
    private var avatarUrl = ""
    private var avatarBitmap: Bitmap? = null
    private var userStopped = false

    private var projection: MediaProjection? = null
    private var stream: CryoScreenStream? = null
    /** prévia da live aberta no launcher (precisa do modo GL) */
    private var previewOn = false
    /** último texto mostrado na notificação (só atualiza quando muda) */
    private var notificationKey = ""
    @Volatile
    private var audio: AudioSource? = null

    private val chat = ArrayDeque<LiveChatMessage>()
    private var nextChatId = 1L
    private var chatReader: TikTokLiveChat? = null
    private var floating: LiveFloatingWindow? = null
    private var lastStateBroadcast = 0L

    private val ticker = object : Runnable {
        override fun run() {
            // só o relógio do painel aberto muda a cada segundo
            floating?.tick()
            // a voz do Voice Chat entrou/saiu da live: avisa os painéis
            val shared = (audio as? CryoLiveAudioSource)?.voiceShared == true
            if (shared != voiceShared) {
                voiceShared = shared
                broadcastState(force = true)
            }
            main.postDelayed(this, 1000L)
        }
    }

    private val messenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val clientId = msg.data?.getString(LiveProtocol.KEY_CLIENT) ?: ""
            when (msg.what) {
                LiveProtocol.MSG_REGISTER -> msg.replyTo?.let { register(it) }
                LiveProtocol.MSG_UNREGISTER -> msg.replyTo?.let { unregister(it.binder, clientId) }
                LiveProtocol.MSG_SET_MIC -> setMic(msg.arg1 == 1)
                LiveProtocol.MSG_SET_MIC_VOLUME -> setMicVolume(msg.arg1)
                LiveProtocol.MSG_SET_INTERNAL_VOLUME -> setInternalVolume(msg.arg1)
                LiveProtocol.MSG_SET_PRIVACY -> setPrivacy(msg.arg1 == 1)
                LiveProtocol.MSG_STOP -> stopLive(byUser = true)
                LiveProtocol.MSG_FOREGROUND -> {
                    foregroundClients[clientId] = msg.arg1 == 1
                    updateFloating()
                }
                LiveProtocol.MSG_CONFIG_CHANGED -> reloadConfig()
                LiveProtocol.MSG_RELOAD_CHAT -> if (stream != null) restartChat()
                LiveProtocol.MSG_VOICE_PCM -> handleVoice(msg)
                else -> super.handleMessage(msg)
            }
        }
    })

    /**
     * Voz do Voice Chat (~50 pacotes por segundo) numa thread de áudio só dela: a thread principal
     * (tela do launcher, página do chat) às vezes atrasa e a voz chegaria picotada.
     */
    private val voiceThread = android.os.HandlerThread("CryoLiveVoice", android.os.Process.THREAD_PRIORITY_AUDIO).apply { start() }
    private val voiceMessenger = Messenger(object : Handler(voiceThread.looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == LiveProtocol.MSG_VOICE_PCM) handleVoice(msg)
        }
    })

    private fun handleVoice(msg: Message) {
        val pcm = msg.data?.getByteArray(LiveProtocol.KEY_PCM) ?: return
        val source = audio as? CryoLiveAudioSource ?: return
        if (micOn) source.injectVoice(pcm, msg.arg1)
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.cryo_live_notification_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                @Suppress("DEPRECATION")
                val data: Intent? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                else intent.getParcelableExtra(EXTRA_RESULT_DATA)
                startLive(code, data)
            }
            ACTION_STOP -> stopLive(byUser = true)
            else -> if (stream == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    // ---------------------------------------------------------------- live

    private fun startLive(resultCode: Int, data: Intent?) {
        if (stream != null) stopLive(byUser = false, keepService = true)
        config = LiveConfig.load(this)
        userStopped = false
        error = null
        hasMic = hasMicPermission(this)
        micOn = config.micEnabled && hasMic
        micVolume = config.micVolume
        internalVolume = config.internalVolume
        privacy = config.privacyEnabled
        status = LiveStatus.CONNECTING
        startedAt = 0L
        notificationKey = ""

        // Android 14+: o serviço precisa estar em primeiro plano (tipo mediaProjection) antes de pegar a projeção
        var types = 0
        if (Build.VERSION.SDK_INT >= 29) types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        if (Build.VERSION.SDK_INT >= 30 && hasMic) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), types)
        } catch (t: Throwable) {
            fail(getString(R.string.cryo_live_error_foreground, t.message ?: t.javaClass.simpleName))
            return
        }
        if (!config.isReady) {
            fail(getString(R.string.cryo_live_error_config))
            return
        }
        if (resultCode == 0 || data == null) {
            fail(getString(R.string.cryo_live_error_permission))
            return
        }
        val pm = getSystemService(MediaProjectionManager::class.java)
        val mp = runCatching { pm.getMediaProjection(resultCode, data) }.getOrNull()
        if (mp == null) {
            fail(getString(R.string.cryo_live_error_permission))
            return
        }
        projection = mp
        val projectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                main.post { if (stream != null) stopLive(byUser = true) }
            }
        }
        runCatching { mp.registerCallback(projectionCallback, main) }

        try {
            val (w, h) = config.videoSize()
            val audioSource: AudioSource = when {
                // som do celular + microfone + voz do Voice Chat do jogo
                Build.VERSION.SDK_INT >= 29 -> {
                    val route = setupMicRoute()
                    CryoLiveAudioSource(
                        projection = mp,
                        useMic = hasMic,
                        explicitMic = route.explicit,
                        micDevice = route.device,
                        communication = route.communication
                    )
                }
                hasMic -> MicrophoneSource()
                else -> SilenceAudioSource()
            }
            audio = audioSource
            // tela direto no codificador de vídeo (sem cópia pela GPU do app): ver CryoScreenStream
            val s = CryoScreenStream(applicationContext, this, audioSource)
            stream = s
            if (!s.prepare(w, h, config.fps, config.effectiveBitrateKbps() * 1000, AUDIO_SAMPLE_RATE, true, AUDIO_BITRATE)) {
                fail(getString(R.string.cryo_live_error_encoder))
                return
            }
            applyAudio()
            // tela bloqueada desde o início: o modo GL já começa ligado
            if (privacy) s.setGlNeeded(true)
            s.start(config.fullUrl(), mp)
            applyPrivacy()
            // prévia na tela do launcher (se ela estiver aberta)
            LivePreview.listener = { surface, pw, ph -> applyPreview(surface, pw, ph) }
        } catch (t: Throwable) {
            Logger.error(TAG, "Failed to start the live", t)
            fail(getString(R.string.cryo_live_error_start, t.message ?: t.javaClass.simpleName))
            return
        }
        restartChat()
        main.removeCallbacks(ticker)
        main.post(ticker)
        updateFloating()
        broadcastState(force = true)
    }

    private fun fail(message: String) {
        error = message
        status = LiveStatus.ERROR
        broadcastState(force = true)
        stopLive(byUser = false, keepError = true)
    }

    private fun stopLive(byUser: Boolean, keepService: Boolean = false, keepError: Boolean = false) {
        userStopped = byUser
        val s = stream
        LivePreview.listener = null
        previewOn = false
        stream = null
        runCatching { s?.release() }
        audio = null
        releaseMicRoute()
        voiceShared = false
        runCatching { projection?.stop() }
        projection = null
        chatReader?.stop()
        chatReader = null
        main.removeCallbacks(ticker)
        floating?.hide()
        if (!keepError) {
            error = null
            status = LiveStatus.IDLE
        }
        startedAt = 0L
        bitrateKbps = 0L
        broadcastState(force = true)
        if (!keepService) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            // dá tempo de os clientes receberem o estado final
            main.postDelayed({ if (stream == null) stopSelf() }, if (keepError) 4000L else 500L)
        }
    }

    override fun onDestroy() {
        if (stream != null) stopLive(byUser = false, keepService = true)
        floating?.hide()
        floating = null
        chatReader?.stop()
        main.removeCallbacksAndMessages(null)
        voiceThread.quitSafely()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- controles

    /** Liga/desliga a prévia da transmissão numa superfície da tela do launcher. */
    private fun applyPreview(surface: android.view.Surface?, w: Int, h: Int) {
        val s = stream ?: return
        runCatching {
            if (surface == null || !surface.isValid || w <= 0 || h <= 0) {
                previewOn = false
                // sem prévia e sem privacidade: volta ao modo direto (o mais leve)
                s.detachPreview(stillNeedGl = privacy)
            } else {
                previewOn = true
                s.attachPreview(surface, w, h)
            }
        }.onFailure { Logger.error(TAG, "Live preview failed", it) }
    }

    // ---------------------------------------------------------------- microfone (fone Bluetooth/com fio)

    private class MicRoute(val device: android.media.AudioDeviceInfo?, val explicit: Boolean, val communication: Boolean)

    private var commDeviceSet = false
    private var scoStarted = false
    private var voiceShared = false
    /** situação do microfone escolhido (ex.: fone Bluetooth não encontrado) */
    private var micNote = ""

    /**
     * Microfone escolhido no painel. Fone Bluetooth: liga a rota de comunicação do fone (o único jeito
     * de o Android usar o microfone dele); o som no fone fica com qualidade de ligação enquanto isso.
     */
    private fun setupMicRoute(): MicRoute {
        micNote = ""
        if (!hasMic) return MicRoute(null, false, false)
        val am = getSystemService(android.media.AudioManager::class.java) ?: return MicRoute(null, false, false)
        val inputs = runCatching { am.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS).toList() }.getOrDefault(emptyList())
        fun find(vararg types: Int) = inputs.firstOrNull { it.type in types }
        return when (config.micDevice) {
            LiveMicDevice.AUTO -> MicRoute(null, false, false)
            LiveMicDevice.PHONE -> MicRoute(find(android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC), false, false)
            LiveMicDevice.WIRED -> {
                val d = find(
                    android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    android.media.AudioDeviceInfo.TYPE_USB_HEADSET,
                    android.media.AudioDeviceInfo.TYPE_USB_DEVICE
                )
                if (d == null) micNote = getString(R.string.cryo_live_mic_wired_missing)
                else micNote = getString(R.string.cryo_live_mic_using, d.productName?.toString().orEmpty().ifBlank { getString(R.string.cryo_live_mic_wired) })
                MicRoute(d, d != null, false)
            }
            LiveMicDevice.BLUETOOTH -> {
                val ok = startBluetoothRoute(am)
                val d = find(android.media.AudioDeviceInfo.TYPE_BLE_HEADSET, android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
                micNote = if (!ok && d == null) getString(R.string.cryo_live_mic_bt_missing)
                else getString(R.string.cryo_live_mic_using, d?.productName?.toString().orEmpty().ifBlank { getString(R.string.cryo_live_mic_bt) })
                MicRoute(d, ok || d != null, ok || d != null)
            }
        }
    }

    private fun startBluetoothRoute(am: android.media.AudioManager): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            val dev = am.availableCommunicationDevices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } ?: return@runCatching false
            commDeviceSet = am.setCommunicationDevice(dev)
            commDeviceSet
        } else {
            @Suppress("DEPRECATION")
            if (!am.isBluetoothScoAvailableOffCall) return@runCatching false
            @Suppress("DEPRECATION")
            am.startBluetoothSco()
            @Suppress("DEPRECATION")
            am.isBluetoothScoOn = true
            scoStarted = true
            true
        }
    }.getOrDefault(false)

    private fun releaseMicRoute() {
        val am = getSystemService(android.media.AudioManager::class.java) ?: return
        runCatching {
            if (commDeviceSet && Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice()
            if (scoStarted) {
                @Suppress("DEPRECATION")
                am.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                am.stopBluetoothSco()
            }
        }
        commDeviceSet = false
        scoStarted = false
        micNote = ""
    }

    private fun applyAudio() {
        val mic = if (micOn && hasMic) micVolume / 100f else 0f
        val internal = internalVolume / 100f
        when (val a = audio) {
            is CryoLiveAudioSource -> {
                a.micVolume = mic
                a.internalVolume = internal
            }
            is MixAudioSource -> {
                a.microphoneVolume = mic
                a.internalVolume = internal
            }
            is InternalAudioSource -> a.internalVolume = internal
            is MicrophoneSource -> a.microphoneVolume = mic
            else -> {}
        }
    }

    private fun setMic(on: Boolean) {
        micOn = on && hasMic
        applyAudio()
        saveConfig { it.copy(micEnabled = on) }
        broadcastState(force = true)
    }

    private fun setMicVolume(value: Int) {
        micVolume = value.coerceIn(0, 100)
        applyAudio()
        saveConfig { it.copy(micVolume = micVolume) }
        broadcastState(force = false)
    }

    private fun setInternalVolume(value: Int) {
        internalVolume = value.coerceIn(0, 100)
        applyAudio()
        saveConfig { it.copy(internalVolume = internalVolume) }
        broadcastState(force = false)
    }

    private fun setPrivacy(on: Boolean) {
        privacy = on
        saveConfig { it.copy(privacyEnabled = on) }
        applyPrivacy()
        broadcastState(force = true)
    }

    /** Tela bloqueada: quem assiste vê preto ou o PNG escolhido; o áudio continua. */
    private fun applyPrivacy() {
        val s = stream ?: return
        if (!privacy) {
            s.withGl { gl ->
                runCatching { gl.clearFilters() }
                if (gl.isVideoMuted()) gl.unMuteVideo()
            }
            s.setGlNeeded(previewOn)
            return
        }
        val (w, h) = config.videoSize()
        val image = buildPrivacyBitmap(w, h, config.privacyImagePath)
        // a tela bloqueada é desenhada pelo GL: liga o modo GL só enquanto ela estiver ativa
        s.setGlNeeded(true) { gl ->
            runCatching {
                gl.clearFilters()
                if (image != null) {
                    if (gl.isVideoMuted()) gl.unMuteVideo()
                    val filter = ImageObjectFilterRender()
                    gl.addFilter(filter)
                    filter.setImage(image)
                    filter.setScale(100f, 100f)
                    filter.setPosition(0f, 0f)
                } else {
                    gl.muteVideo()
                }
            }.onFailure {
                Logger.error(TAG, "Privacy screen failed", it)
                runCatching { gl.muteVideo() }
            }
        }
    }

    /** Fundo preto do tamanho do vídeo com o PNG centralizado (sem distorcer). null = sem imagem válida. */
    private fun buildPrivacyBitmap(w: Int, h: Int, path: String): Bitmap? {
        if (path.isBlank()) return null
        val src = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        val scale = minOf(w / src.width.toFloat(), h / src.height.toFloat())
        val dw = src.width * scale
        val dh = src.height * scale
        val left = (w - dw) / 2f
        val top = (h - dh) / 2f
        canvas.drawBitmap(src, null, RectF(left, top, left + dw, top + dh), Paint(Paint.FILTER_BITMAP_FLAG))
        src.recycle()
        return out
    }

    private fun saveConfig(change: (LiveConfig) -> LiveConfig) {
        config = change(LiveConfig.load(this))
        LiveConfig.save(this, config)
    }

    private fun reloadConfig() {
        val old = config
        config = LiveConfig.load(this)
        if (stream != null) {
            if (old.tiktokUser != config.tiktokUser || old.chatApiKey != config.chatApiKey) restartChat()
            if (old.privacyImagePath != config.privacyImagePath && privacy) applyPrivacy()
            updateFloating()
        }
    }

    // ---------------------------------------------------------------- chat

    private fun restartChat() {
        chatReader?.stop()
        chatReader = null
        chat.clear()
        viewers = ""
        avatarUrl = ""
        sendToClients { Message.obtain(null, LiveProtocol.MSG_CHAT_CLEAR) }
        floating?.updateChat(emptyList())
        chatDetail = ""
        if (config.tiktokUser.isBlank()) {
            chatState = ""
            broadcastState(force = true)
            return
        }
        chatReader = runCatching {
            TikTokLiveChat(
                onMessage = { user, text -> addChat(user, text) },
                onState = { state ->
                    if (state != chatState) {
                        chatState = state
                        broadcastState(force = true)
                    }
                },
                onViewers = { v ->
                    if (v != viewers) {
                        viewers = v
                        broadcastState(force = true)
                    }
                },
                onAvatar = { url -> loadAvatar(url) },
                onDetail = { d ->
                    chatDetail = d
                    broadcastState(force = false)
                },
                apiKey = { config.chatApiKey }
            ).also { it.start(config.tiktokUser) }
        }.onFailure { Logger.error(TAG, "Chat reader failed", it) }.getOrNull()
    }

    /** Baixa a foto de perfil (pequena) para o botão flutuante. */
    private fun loadAvatar(url: String) {
        if (url == avatarUrl) return
        avatarUrl = url
        Thread({
            val bmp = runCatching {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                conn.inputStream.use { input ->
                    val bytes = input.readBytes()
                    if (bytes.size > 2_000_000) null
                    else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { Bitmap.createScaledBitmap(it, 96, 96, true) }
                }
            }.getOrNull()
            main.post {
                if (url == avatarUrl && bmp != null) {
                    avatarBitmap = bmp
                    floating?.setAvatar(bmp)
                }
            }
        }, "LiveAvatar").start()
    }

    private fun addChat(user: String, text: String) {
        val msg = LiveChatMessage(nextChatId++, user, text)
        chat.addLast(msg)
        while (chat.size > MAX_CHAT) chat.removeFirst()
        sendToClients { chatMessage(listOf(msg)) }
        floating?.updateChat(chat.toList())
        floating?.showPreview(msg)
    }

    private fun chatMessage(list: List<LiveChatMessage>): Message =
        Message.obtain(null, LiveProtocol.MSG_CHAT).apply {
            data = Bundle().apply {
                putLongArray(LiveProtocol.KEY_IDS, list.map { it.id }.toLongArray())
                putStringArray(LiveProtocol.KEY_USERS, list.map { it.user }.toTypedArray())
                putStringArray(LiveProtocol.KEY_TEXTS, list.map { it.text }.toTypedArray())
            }
        }

    // ---------------------------------------------------------------- botão flutuante

    private fun updateFloating() {
        // igual ao TikTok: o botão fica na tela durante a live inteira (no jogo, no launcher e fora do app)
        val shouldShow = stream != null && config.floatingBubble
        if (!shouldShow) {
            floating?.hide()
            return
        }
        val window = floating ?: LiveFloatingWindow(this, object : LiveFloatingWindow.Actions {
            override fun setMic(on: Boolean) = this@LiveService.setMic(on)
            override fun setMicVolume(value: Int) = this@LiveService.setMicVolume(value)
            override fun setInternalVolume(value: Int) = this@LiveService.setInternalVolume(value)
            override fun setPrivacy(on: Boolean) = this@LiveService.setPrivacy(on)
            override fun stopLive() = this@LiveService.stopLive(byUser = true)
            override fun openApp() {
                packageManager.getLaunchIntentForPackage(packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    runCatching { startActivity(it) }
                }
            }
            override fun saveOpacity(value: Int) {
                // opacidade do botão flutuante usa a mesma do chat
                saveConfig { it.copy(chatOpacity = value.coerceIn(10, 100)) }
            }
        }).also { floating = it }
        if (!window.showing && window.canShow()) {
            window.setAvatar(avatarBitmap)
            window.show(config.chatOpacity)
            window.updateChat(chat.toList())
        }
        window.update(floatingState())
    }

    private fun floatingState() = LiveFloatingWindow.State(
        status = status,
        startedAt = startedAt,
        mic = micOn,
        hasMic = hasMic,
        micVolume = micVolume,
        internalVolume = internalVolume,
        privacy = privacy,
        viewers = viewers,
        chatState = chatState,
        chatDetail = chatDetail
    )

    // ---------------------------------------------------------------- clientes

    private fun register(client: Messenger) {
        val binder = client.binder
        if (clients.containsKey(binder)) return
        clients[binder] = client
        runCatching { binder.linkToDeath({ main.post { unregister(binder, null) } }, 0) }
        try {
            client.send(stateMessage())
            client.send(Message.obtain(null, LiveProtocol.MSG_CHAT_CLEAR))
            if (chat.isNotEmpty()) client.send(chatMessage(chat.toList()))
        } catch (e: RemoteException) {
            clients.remove(binder)
        }
    }

    private fun unregister(binder: IBinder, clientId: String?) {
        clients.remove(binder)
        if (clientId != null) foregroundClients.remove(clientId)
        // processo do cliente morreu: não sabemos qual id era; recalcula pelo que sobrou
        if (clients.isEmpty()) foregroundClients.clear()
        updateFloating()
    }

    private fun stateMessage(): Message = Message.obtain(null, LiveProtocol.MSG_STATE).apply {
        data = Bundle().apply {
            putString(LiveProtocol.KEY_STATUS, status.name)
            putBinder(LiveProtocol.KEY_VOICE_BINDER, voiceMessenger.binder)
            putString(LiveProtocol.KEY_ERROR, error)
            putBoolean(LiveProtocol.KEY_MIC, micOn)
            putBoolean(LiveProtocol.KEY_HAS_MIC, hasMic)
            putInt(LiveProtocol.KEY_MIC_VOLUME, micVolume)
            putInt(LiveProtocol.KEY_INTERNAL_VOLUME, internalVolume)
            putBoolean(LiveProtocol.KEY_PRIVACY, privacy)
            putLong(LiveProtocol.KEY_BITRATE, bitrateKbps)
            putLong(LiveProtocol.KEY_STARTED_AT, startedAt)
            putString(LiveProtocol.KEY_CHAT_STATE, chatState)
            putString(LiveProtocol.KEY_CHAT_DETAIL, chatDetail)
            putString(LiveProtocol.KEY_VIEWERS, viewers)
            putBoolean(LiveProtocol.KEY_VOICE_SHARED, voiceShared)
            putString(LiveProtocol.KEY_MIC_NOTE, micNote)
        }
    }

    private fun broadcastState(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastStateBroadcast < 500L) return
        lastStateBroadcast = now
        sendToClients { stateMessage() }
        floating?.update(floatingState())
        // a notificação só muda com o estado (atualizar toda hora faz o sistema redesenhar à toa)
        val key = "$status|$privacy|$startedAt|$error|$chatState"
        if (stream != null && key != notificationKey) {
            notificationKey = key
            runCatching { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification()) }
        }
    }

    private fun sendToClients(build: () -> Message) {
        val dead = mutableListOf<IBinder>()
        for ((binder, client) in clients) {
            try {
                client.send(build())
            } catch (e: RemoteException) {
                dead += binder
            }
        }
        dead.forEach { clients.remove(it) }
    }

    private fun buildNotification(): Notification {
        val tiktokEnded = chatState == TikTokLiveChat.STATE_ENDED || chatState == TikTokLiveChat.STATE_SUSPENDED
        val text = when {
            status == LiveStatus.LIVE && tiktokEnded -> getString(R.string.cryo_live_ended_short)
            else -> statusText()
        }
        return notificationBuilder(text)
    }

    private fun statusText(): String {
        return when (status) {
            LiveStatus.LIVE -> getString(if (privacy) R.string.cryo_live_notification_live_private else R.string.cryo_live_notification_live)
            LiveStatus.CONNECTING -> getString(R.string.cryo_live_status_connecting)
            LiveStatus.RECONNECTING -> getString(R.string.cryo_live_status_reconnecting)
            LiveStatus.ERROR -> error ?: getString(R.string.cryo_live_status_error)
            LiveStatus.IDLE -> getString(R.string.cryo_live_status_idle)
        }
    }

    private fun notificationBuilder(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, LiveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.cryo_live_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(status == LiveStatus.LIVE && startedAt > 0L)
            .setWhen(if (startedAt > 0L) startedAt else System.currentTimeMillis())
            .setContentIntent(open)
            .addAction(0, getString(R.string.cryo_live_stop), stopIntent)
            .build()
    }

    // ---------------------------------------------------------------- RTMP (ConnectChecker)

    override fun onConnectionStarted(url: String) {
        main.post {
            if (status != LiveStatus.RECONNECTING) status = LiveStatus.CONNECTING
            broadcastState(force = true)
        }
    }

    override fun onConnectionSuccess() {
        // primeiro quadro completo logo na (re)conexão: quem assiste não espera o próximo quadro-chave
        stream?.requestKeyframe()
        main.post {
            status = LiveStatus.LIVE
            error = null
            if (startedAt == 0L) startedAt = System.currentTimeMillis()
            broadcastState(force = true)
        }
    }

    override fun onConnectionFailed(reason: String) {
        main.post {
            val s = stream ?: return@post
            val retry = runCatching { s.shouldRetry(reason) }.getOrDefault(false)
            if (retry) {
                runCatching { s.reConnect(2000L) }
                status = LiveStatus.RECONNECTING
                error = reason
                broadcastState(force = true)
            } else {
                fail(getString(R.string.cryo_live_error_connection, reason))
            }
        }
    }

    override fun onDisconnect() {
        main.post {
            if (userStopped || stream == null) {
                status = LiveStatus.IDLE
                broadcastState(force = true)
            }
        }
    }

    override fun onAuthError() {
        main.post { fail(getString(R.string.cryo_live_error_auth)) }
    }

    override fun onAuthSuccess() {}

    override fun onNewBitrate(bitrate: Long) {
        stream?.onBitrateTick()
        main.post {
            bitrateKbps = bitrate / 1000L
            broadcastState(force = false)
        }
    }
}
