package com.russoviller.cryogenic.live

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Orientação do vídeo enviado para a live. */
enum class LiveOrientation { LANDSCAPE, PORTRAIT }

/** Canto da tela onde o chat aparece dentro do jogo. */
enum class LiveChatCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** Microfone da live. AUTO = o do sistema + a voz do Voice Chat quando ele estiver ligado. */
enum class LiveMicDevice { AUTO, PHONE, BLUETOOTH, WIRED }

/**
 * Configuração da live do TikTok (Painel da Live).
 * Fica num JSON privado do app (files/live/tiktok.json): o launcher e o processo do jogo leem o mesmo arquivo.
 * A chave da live nunca sai desse arquivo privado.
 */
data class LiveConfig(
    /** "URL do servidor" do TikTok LIVE Producer (rtmp://…) */
    val serverUrl: String = "",
    /** "Chave da transmissão" */
    val streamKey: String = "",
    /** altura do vídeo: 480, 540 ou 720 */
    val resolution: Int = 720,
    /** 30 ou 60 */
    val fps: Int = 60,
    /** kbps; 0 = automático pela qualidade */
    val bitrateKbps: Int = 0,
    val orientation: LiveOrientation = LiveOrientation.LANDSCAPE,
    val micEnabled: Boolean = true,
    /** 0..100 */
    val micVolume: Int = 100,
    /** 0..100 */
    val internalVolume: Int = 100,
    /** tela bloqueada para quem assiste */
    val privacyEnabled: Boolean = false,
    /** PNG mostrado com a tela bloqueada (vazio = tela preta) */
    val privacyImagePath: String = "",
    /** @usuário do TikTok para ler o chat da live */
    val tiktokUser: String = "",
    val chatInGame: Boolean = true,
    /** 70..160 (% do tamanho do texto) */
    val chatSize: Int = 100,
    /** 10..100 */
    val chatOpacity: Int = 80,
    /** 20..60 (% da largura da tela) */
    val chatWidth: Int = 34,
    val chatCorner: LiveChatCorner = LiveChatCorner.TOP_LEFT,
    /** botão flutuante com chat e controles quando o app sai da tela */
    val floatingBubble: Boolean = true,
    val micDevice: LiveMicDevice = LiveMicDevice.AUTO,
    /** chave (opcional) do Euler Stream para o chat: mais conexões por dia que o grátis */
    val chatApiKey: String = ""
) {
    /** Endereço completo para o RTMP: servidor + chave. */
    fun fullUrl(): String {
        val server = serverUrl.trim().trimEnd('/')
        val key = streamKey.trim().trimStart('/')
        return if (key.isEmpty()) server else "$server/$key"
    }

    val isReady: Boolean
        get() = serverUrl.trim().let { it.startsWith("rtmp://", true) || it.startsWith("rtmps://", true) } &&
            streamKey.isNotBlank()

    /** Tamanho do vídeo (largura x altura) com dimensões pares. */
    fun videoSize(): Pair<Int, Int> {
        val h = resolution.coerceIn(360, 720)
        val w = ((h * 16 / 9) + 1) and 1.inv()
        return if (orientation == LiveOrientation.LANDSCAPE) w to h else h to w
    }

    /** kbps efetivo (automático: valores recomendados para live de jogo). */
    fun effectiveBitrateKbps(): Int {
        if (bitrateKbps > 0) return bitrateKbps.coerceIn(500, 8000)
        val base = when {
            resolution >= 720 -> 3500
            resolution >= 540 -> 2500
            else -> 1500
        }
        return if (fps >= 60) base * 3 / 2 else base
    }

    fun toJson(): JSONObject = JSONObject()
        .put("serverUrl", serverUrl)
        .put("streamKey", streamKey)
        .put("resolution", resolution)
        .put("fps", fps)
        .put("bitrateKbps", bitrateKbps)
        .put("orientation", orientation.name)
        .put("micEnabled", micEnabled)
        .put("micVolume", micVolume)
        .put("internalVolume", internalVolume)
        .put("privacyEnabled", privacyEnabled)
        .put("privacyImagePath", privacyImagePath)
        .put("tiktokUser", tiktokUser)
        .put("chatInGame", chatInGame)
        .put("chatSize", chatSize)
        .put("chatOpacity", chatOpacity)
        .put("chatWidth", chatWidth)
        .put("chatCorner", chatCorner.name)
        .put("floatingBubble", floatingBubble)
        .put("micDevice", micDevice.name)
        .put("chatApiKey", chatApiKey)

    companion object {
        val RESOLUTIONS = listOf(480, 540, 720)
        val FRAME_RATES = listOf(30, 60)

        fun fromJson(o: JSONObject): LiveConfig {
            val d = LiveConfig()
            return LiveConfig(
                serverUrl = o.optString("serverUrl", d.serverUrl),
                streamKey = o.optString("streamKey", d.streamKey),
                resolution = o.optInt("resolution", d.resolution).let { r -> RESOLUTIONS.firstOrNull { it == r } ?: d.resolution },
                fps = o.optInt("fps", d.fps).let { f -> if (f >= 60) 60 else 30 },
                bitrateKbps = o.optInt("bitrateKbps", d.bitrateKbps).coerceIn(0, 8000),
                orientation = runCatching { LiveOrientation.valueOf(o.optString("orientation")) }.getOrDefault(d.orientation),
                micEnabled = o.optBoolean("micEnabled", d.micEnabled),
                micVolume = o.optInt("micVolume", d.micVolume).coerceIn(0, 100),
                internalVolume = o.optInt("internalVolume", d.internalVolume).coerceIn(0, 100),
                privacyEnabled = o.optBoolean("privacyEnabled", d.privacyEnabled),
                privacyImagePath = o.optString("privacyImagePath", d.privacyImagePath),
                tiktokUser = o.optString("tiktokUser", d.tiktokUser),
                chatInGame = o.optBoolean("chatInGame", d.chatInGame),
                chatSize = o.optInt("chatSize", d.chatSize).coerceIn(70, 160),
                chatOpacity = o.optInt("chatOpacity", d.chatOpacity).coerceIn(10, 100),
                chatWidth = o.optInt("chatWidth", d.chatWidth).coerceIn(20, 60),
                chatCorner = runCatching { LiveChatCorner.valueOf(o.optString("chatCorner")) }.getOrDefault(d.chatCorner),
                floatingBubble = o.optBoolean("floatingBubble", d.floatingBubble),
                micDevice = runCatching { LiveMicDevice.valueOf(o.optString("micDevice")) }.getOrDefault(d.micDevice),
                chatApiKey = o.optString("chatApiKey", d.chatApiKey).trim().take(200)
            )
        }

        fun file(context: Context): File = File(context.filesDir, "live/tiktok.json")

        fun load(context: Context): LiveConfig = runCatching {
            val f = file(context)
            if (f.isFile) fromJson(JSONObject(f.readText())) else LiveConfig()
        }.getOrDefault(LiveConfig())

        fun save(context: Context, config: LiveConfig) {
            runCatching {
                val f = file(context)
                f.parentFile?.mkdirs()
                val tmp = File(f.parentFile, "tiktok.json.tmp")
                tmp.writeText(config.toJson().toString())
                if (!tmp.renameTo(f)) {
                    f.writeText(config.toJson().toString())
                    tmp.delete()
                }
            }
        }

        /** Limpa o @ e espaços do usuário do TikTok. */
        fun normalizeUser(raw: String): String =
            raw.trim().removePrefix("https://www.tiktok.com/").removePrefix("@").substringBefore('/').substringBefore('?').trim()
    }
}

/** Mensagem do chat da live. */
data class LiveChatMessage(val id: Long, val user: String, val text: String)
