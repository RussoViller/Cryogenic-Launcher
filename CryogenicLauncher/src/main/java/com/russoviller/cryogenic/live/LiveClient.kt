package com.russoviller.cryogenic.live

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Estado da transmissão. */
enum class LiveStatus { IDLE, CONNECTING, LIVE, RECONNECTING, ERROR }

/** Mensagens entre os processos (launcher / jogo) e o [LiveService]. */
internal object LiveProtocol {
    // cliente -> serviço
    const val MSG_REGISTER = 1
    const val MSG_UNREGISTER = 2
    const val MSG_SET_MIC = 3
    const val MSG_SET_MIC_VOLUME = 4
    const val MSG_SET_INTERNAL_VOLUME = 5
    const val MSG_SET_PRIVACY = 6
    const val MSG_STOP = 7
    const val MSG_FOREGROUND = 8
    const val MSG_CONFIG_CHANGED = 9
    const val MSG_RELOAD_CHAT = 10
    /** microfone do Voice Chat do jogo (PCM 16 bits mono) */
    const val MSG_VOICE_PCM = 11

    // serviço -> cliente
    const val MSG_STATE = 100
    const val MSG_CHAT = 101
    const val MSG_CHAT_CLEAR = 102

    const val KEY_STATUS = "status"
    const val KEY_ERROR = "error"
    const val KEY_MIC = "mic"
    const val KEY_MIC_VOLUME = "micVolume"
    const val KEY_INTERNAL_VOLUME = "internalVolume"
    const val KEY_PRIVACY = "privacy"
    const val KEY_BITRATE = "bitrate"
    const val KEY_STARTED_AT = "startedAt"
    const val KEY_CHAT_STATE = "chatState"
    const val KEY_CHAT_DETAIL = "chatDetail"
    const val KEY_PCM = "pcm"
    const val KEY_RATE = "rate"
    const val KEY_VOICE_SHARED = "voiceShared"
    const val KEY_MIC_NOTE = "micNote"
    /** Messenger da voz (thread de áudio do serviço) */
    const val KEY_VOICE_BINDER = "voiceBinder"
    const val KEY_HAS_MIC = "hasMic"
    const val KEY_VIEWERS = "viewers"
    const val KEY_IDS = "ids"
    const val KEY_USERS = "users"
    const val KEY_TEXTS = "texts"
    const val KEY_CLIENT = "client"
}

/**
 * Lado cliente da live (um por processo: launcher e jogo).
 * Liga-se ao [LiveService] sem criá-lo: quando a live começa, a conexão acontece sozinha.
 * O estado fica em Compose state para a UI.
 */
object LiveClient {
    var status by mutableStateOf(LiveStatus.IDLE)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var micOn by mutableStateOf(true)
        private set
    var hasMic by mutableStateOf(false)
        private set
    var micVolume by mutableStateOf(100)
        private set
    var internalVolume by mutableStateOf(100)
        private set
    var privacy by mutableStateOf(false)
        private set
    var bitrateKbps by mutableLongStateOf(0L)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set
    /** estado do leitor de chat (texto curto para a UI) */
    var chatState by mutableStateOf("")
        private set
    /** linha de diagnóstico do leitor do chat */
    var chatDetail by mutableStateOf("")
        private set
    /** espectadores como a página da live mostra (vazio = desconhecido) */
    var viewers by mutableStateOf("")
        private set
    val chat = mutableStateListOf<LiveChatMessage>()

    val active: Boolean get() = status == LiveStatus.CONNECTING || status == LiveStatus.LIVE || status == LiveStatus.RECONNECTING

    private const val MAX_CHAT = 150
    private val clientId = "${android.os.Process.myPid()}"
    private var service: Messenger? = null
    private var bound = false
    private var appContext: Context? = null
    private var foreground = false

    private val incoming = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                LiveProtocol.MSG_STATE -> applyState(msg.data)
                LiveProtocol.MSG_CHAT -> {
                    val data = msg.data
                    val ids = data.getLongArray(LiveProtocol.KEY_IDS) ?: return
                    val users = data.getStringArray(LiveProtocol.KEY_USERS) ?: return
                    val texts = data.getStringArray(LiveProtocol.KEY_TEXTS) ?: return
                    val known = chat.lastOrNull()?.id ?: -1L
                    for (i in ids.indices) {
                        if (ids[i] > known) chat.add(LiveChatMessage(ids[i], users[i], texts[i]))
                    }
                    while (chat.size > MAX_CHAT) chat.removeAt(0)
                }
                LiveProtocol.MSG_CHAT_CLEAR -> chat.clear()
                else -> super.handleMessage(msg)
            }
        }
    })

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null) return
            service = Messenger(binder)
            send(LiveProtocol.MSG_REGISTER)
            send(LiveProtocol.MSG_FOREGROUND, if (foreground) 1 else 0)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            applyState(Bundle())
        }
    }

    private fun applyState(b: Bundle) {
        status = runCatching { LiveStatus.valueOf(b.getString(LiveProtocol.KEY_STATUS) ?: "IDLE") }.getOrDefault(LiveStatus.IDLE)
        error = b.getString(LiveProtocol.KEY_ERROR)
        micOn = b.getBoolean(LiveProtocol.KEY_MIC, micOn)
        hasMic = b.getBoolean(LiveProtocol.KEY_HAS_MIC, false)
        micVolume = b.getInt(LiveProtocol.KEY_MIC_VOLUME, micVolume)
        internalVolume = b.getInt(LiveProtocol.KEY_INTERNAL_VOLUME, internalVolume)
        privacy = b.getBoolean(LiveProtocol.KEY_PRIVACY, privacy)
        bitrateKbps = b.getLong(LiveProtocol.KEY_BITRATE, 0L)
        startedAt = b.getLong(LiveProtocol.KEY_STARTED_AT, 0L)
        chatState = b.getString(LiveProtocol.KEY_CHAT_STATE) ?: ""
        chatDetail = b.getString(LiveProtocol.KEY_CHAT_DETAIL) ?: ""
        viewers = b.getString(LiveProtocol.KEY_VIEWERS) ?: ""
        voiceShared = b.getBoolean(LiveProtocol.KEY_VOICE_SHARED, false)
        voiceService = b.getBinder(LiveProtocol.KEY_VOICE_BINDER)?.let { Messenger(it) }
        micNote = b.getString(LiveProtocol.KEY_MIC_NOTE) ?: ""
        voiceTeeWanted = active && micOn && hasMic
    }

    /** Só para testes/prévias de UI. */
    @androidx.annotation.VisibleForTesting
    internal fun applyTestState(status: LiveStatus, startedAt: Long, privacy: Boolean, micOn: Boolean, messages: List<LiveChatMessage>) {
        this.status = status
        this.startedAt = startedAt
        this.privacy = privacy
        this.micOn = micOn
        this.hasMic = true
        chat.clear()
        chat.addAll(messages)
    }

    /** Conecta ao serviço (idempotente). Chame em onCreate da Activity. */
    fun connect(context: Context) {
        if (bound) return
        val app = context.applicationContext
        appContext = app
        bound = runCatching {
            // sem BIND_AUTO_CREATE: só conecta quando a live estiver rodando
            app.bindService(Intent(app, LiveService::class.java), connection, 0)
        }.getOrDefault(false)
    }

    /** Liga de novo depois que a live foi iniciada (o bind sem auto-create já espera, isso só garante). */
    fun reconnect(context: Context) {
        if (!bound) connect(context)
    }

    /** Avisa se alguma tela do app está visível (o botão flutuante só aparece fora do app). */
    fun setForeground(value: Boolean) {
        foreground = value
        send(LiveProtocol.MSG_FOREGROUND, if (value) 1 else 0)
    }

    fun setMic(on: Boolean) {
        micOn = on
        send(LiveProtocol.MSG_SET_MIC, if (on) 1 else 0)
    }

    fun changeMicVolume(value: Int) {
        micVolume = value
        send(LiveProtocol.MSG_SET_MIC_VOLUME, value)
    }

    fun changeInternalVolume(value: Int) {
        internalVolume = value
        send(LiveProtocol.MSG_SET_INTERNAL_VOLUME, value)
    }

    fun changePrivacy(on: Boolean) {
        privacy = on
        send(LiveProtocol.MSG_SET_PRIVACY, if (on) 1 else 0)
    }

    fun configChanged() = send(LiveProtocol.MSG_CONFIG_CHANGED)

    /** Reabre a página do chat (depois de entrar na conta / resolver a verificação). */
    fun reloadChat() = send(LiveProtocol.MSG_RELOAD_CHAT)

    fun stop(context: Context) {
        if (!send(LiveProtocol.MSG_STOP)) {
            runCatching { context.startService(Intent(context, LiveService::class.java).setAction(LiveService.ACTION_STOP)) }
        }
    }

    /**
     * Lido pelo jogo (Voice Chat): a live quer o microfone dele? Só com a live ligada e o microfone
     * dela ativo. Pode ser lido de qualquer thread.
     */
    @Volatile
    var voiceTeeWanted: Boolean = false
        private set

    /** A live está usando a voz do Voice Chat agora (mostrado no painel). */
    var voiceShared by mutableStateOf(false)
        private set

    /** Situação do microfone escolhido (fone encontrado ou não). */
    var micNote by mutableStateOf("")
        private set

    /** Microfone do Voice Chat -> live (qualquer thread; chamado pelo jogo ~50x por segundo). */
    fun sendVoice(pcm: ByteArray, sampleRate: Int) {
        if (!voiceTeeWanted || pcm.isEmpty() || sampleRate !in 8000..192000) return
        val target = voiceService ?: service ?: return
        try {
            target.send(Message.obtain(null, LiveProtocol.MSG_VOICE_PCM, sampleRate, 0).apply {
                data = Bundle().apply { putByteArray(LiveProtocol.KEY_PCM, pcm) }
            })
        } catch (e: RemoteException) {
            voiceService = null
        } catch (e: RuntimeException) {
        }
    }

    @Volatile
    private var voiceService: Messenger? = null

    private fun send(what: Int, arg1: Int = 0): Boolean {
        val target = service ?: return false
        return try {
            target.send(Message.obtain(null, what, arg1, 0).apply {
                replyTo = incoming
                data = Bundle().apply { putString(LiveProtocol.KEY_CLIENT, clientId) }
            })
            true
        } catch (e: RemoteException) {
            service = null
            false
        } catch (e: RuntimeException) {
            false
        }
    }
}
