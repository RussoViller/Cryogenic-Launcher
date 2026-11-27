/*
 * Cryogenic Launcher - Discord Rich Presence.
 * Based on the Discord Social SDK RPC approach used by Aerix Launcher
 * (https://github.com/entitybrian69-bit/Aerix-launcher, GPL-3.0) and the AIDL interfaces
 * reverse-engineered by Nightdavisao (antisocialcord-kt, MIT).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.russoviller.cryogenic.discord

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.discord.socialsdk.rpc.IDiscordRpcCallback
import com.discord.socialsdk.rpc.IDiscordRpcConnection
import com.discord.socialsdk.rpc.IDiscordRpcService
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.utils.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

object CryoDiscordPresence {
    private const val TAG = "CryoDiscordPresence"
    private const val ACTION = "com.discord.socialsdk.rpc.IDiscordRpcService"
    private const val RPC_VERSION = "1"
    private const val HANDLER_PID = 1
    private const val LARGE_IMAGE_KEY = "cryogenic"
    private const val LARGE_IMAGE_ASSET_ID = 1569439131594002493L
    private const val LARGE_IMAGE_TEXT = "Cryogenic Launcher"
    /** Botão do status: leva às versões do Launcher no GitHub. */
    private const val DOWNLOAD_LABEL = "Baixar"
    private const val DOWNLOAD_URL = "https://github.com/RussoViller/Cryogenic-Launcher/releases"
    private const val MIN_SEND_INTERVAL_MS = 4_500L
    private const val RATE_LIMIT_BACKOFF_MS = 20_000L
    private const val MAX_TRACKED_FRAMES = 16
    private const val CLOSE_NORMAL = 1000
    private const val CLOSE_INVALID_CLIENT_ID = 4000
    private const val CLEAR_FLUSH_DELAY_MS = 400L
    private const val READY_TIMEOUT_MS = 10_000L
    private const val MAX_READY_ATTEMPTS = 2
    private const val MAX_BACKOFF_MS = 60_000L
    private const val TEXT_LIMIT = 128

    const val CRYOGENIC_APP_ID = 1568342132098465953L
    const val MINECRAFT_PRIMARY_APP_ID = 1402418491272986635L
    const val MINECRAFT_LEGACY_APP_ID = 356875570916753438L

    private val DISCORD_PACKAGES = listOf("com.discord", "cocobo1.pupu.app")

    enum class Status { DISABLED, NOT_INSTALLED, CONNECTING, WAITING, CONNECTED, ERROR }

    data class Snapshot(val status: Status, val appId: Long = 0L, val message: String? = null)

    private val _status = MutableStateFlow(Snapshot(Status.DISABLED))
    val status: StateFlow<Snapshot> = _status.asStateFlow()

    /**
     * Toda a conversa com o Discord roda numa thread própria. As chamadas ao serviço do Discord
     * (sendFrame) são síncronas entre processos: na thread principal, cada troca de página
     * esperava o Discord responder e a tela travava.
     */
    private val main = Handler(
        android.os.HandlerThread("cryo-discord", android.os.Process.THREAD_PRIORITY_BACKGROUND)
            .apply { start() }
            .looper
    )
    private var appContext: Context? = null
    private var service: IDiscordRpcService? = null
    private var serviceBinder: IBinder? = null
    private var connection: IDiscordRpcConnection? = null
    private var connectedAppId = 0L
    private var bound = false
    private var ready = false
    private var wanted = false
    private enum class ImageMode { ASSET_URL, ASSET_ID, ASSET_KEY, NONE }

    private class SentFrame(
        val appId: Long,
        val imageMode: ImageMode,
        val frame: String,
        val serverIcon: Boolean = false,
        val button: Boolean = false
    )

    private var imageMode = ImageMode.ASSET_URL
    private val sentFrames = LinkedHashMap<String, SentFrame>()
    private var lastSentAt = 0L
    private var readyAttempts = 0
    private var retryCount = 0
    private val rejectedIds = HashSet<Long>()

    @Volatile
    private var handingOff = false

    private var inGame = false
    private var details = ""
    private var state = ""
    private var startedAt = 0L

    /** Endereço público do servidor em que a pessoa está (ícone grande no Discord), ou null. */
    private var serverAddress: String? = null
    /** O Discord recusou a imagem do servidor: volta a usar só a imagem do Launcher. */
    private var serverIconRejected = false
    /** O Discord recusou o botão "Baixar" nesta conexão: manda sem ele. */
    private var buttonsRejected = false

    /** Música que está tocando no player do Launcher (null = nada tocando). */
    private var listening: String? = null
    private var pendingListening: String? = null

    private val listeningRunnable = Runnable {
        if (listening == pendingListening) return@Runnable
        listening = pendingListening
        if (ready) sendActivity()
    }

    /**
     * Player de música do Launcher: mostra "Ouvindo: música" abaixo da linha principal.
     * Ao parar, espera um pouco antes de tirar (troca de faixa e carregamento não piscam o status).
     */
    fun setListening(title: String?) {
        main.post {
            pendingListening = title?.trim()?.takeIf { it.isNotEmpty() }
            main.removeCallbacks(listeningRunnable)
            if (pendingListening != null) listeningRunnable.run()
            else main.postDelayed(listeningRunnable, 2_000L)
        }
    }

    private fun showMusic(): Boolean = runCatching { AllSettings.cryoDiscordShowMusic.getValue() }.getOrDefault(true)

    private fun listeningText(): String? {
        val song = listening ?: return null
        if (!showMusic()) return null
        val ctx = appContext ?: return "Listening: $song"
        return ctx.getString(com.russoviller.cryogenic.R.string.cryo_discord_listening, song)
    }

    private fun serverIconUrl(address: String) = "https://api.mcsrvstat.us/icon/$address"

    private val resendDelays = longArrayOf(6000L)

    private fun logI(msg: String) = Logger.info(TAG, msg)
    private fun logW(msg: String, t: Throwable? = null) = Logger.warning(TAG, msg, t)

    private fun largeImageUrl(appId: Long) = "https://cdn.discordapp.com/app-assets/$appId/$LARGE_IMAGE_ASSET_ID.png?size=1024"

    private fun publishStatus(status: Status, message: String? = null) {
        _status.value = Snapshot(status, connectedAppId, message)
    }

    private val deathRecipient = IBinder.DeathRecipient {
        main.post {
            logW("Discord RPC service died")
            onServiceLost("Discord RPC service died")
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null) {
                main.post { onServiceLost("Discord returned an empty binder") }
                return
            }
            main.post {
                service = IDiscordRpcService.Stub.asInterface(binder)
                serviceBinder = binder
                runCatching { binder.linkToDeath(deathRecipient, 0) }
                logI("Bound to Discord RPC service: $name")
                connectDesired()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            main.post { onServiceLost("Discord RPC service disconnected") }
        }

        override fun onBindingDied(name: ComponentName?) {
            main.post {
                unbind()
                onServiceLost("Discord RPC binding died")
            }
        }

        override fun onNullBinding(name: ComponentName?) {
            main.post {
                logW("Discord refused the RPC binding")
                unbind()
                publishStatus(Status.ERROR, "Discord refused the RPC binding")
                scheduleRetry()
            }
        }
    }

    private val callback = object : IDiscordRpcCallback.Stub() {
        override fun onFrame(frame: String?) {
            if (frame == null) return
            main.post { handleFrame(frame) }
        }

        override fun onClose(code: Int, message: String?) {
            main.post { handleClose(code, message) }
        }
    }

    private val readyTimeout = Runnable {
        if (ready || connection == null) return@Runnable
        val id = connectedAppId
        readyAttempts++
        logW("Discord did not answer READY for $id (attempt $readyAttempts)")
        dropConnection()
        if (readyAttempts >= MAX_READY_ATTEMPTS) {
            rejectedIds.add(id)
            readyAttempts = 0
        }
        connectDesired()
    }

    private val retryRunnable = Runnable {
        val ctx = appContext ?: return@Runnable
        if (wanted) attach(ctx)
    }

    private val resendRunnable = Runnable { if (ready) sendActivity() }

    private val flushRunnable = Runnable { if (ready) sendActivity() }

    fun isDiscordInstalled(context: Context): Boolean = resolveService(context.applicationContext) != null

    private fun enabled(): Boolean = runCatching { AllSettings.cryoDiscordPresence.getValue() }.getOrDefault(false)

    private fun customAppId(): Long? =
        runCatching { AllSettings.cryoDiscordAppId.getValue() }.getOrDefault("")
            .trim().trim('"', '\'')
            .takeIf { it.length in 15..20 }
            ?.toLongOrNull()?.takeIf { it > 0L }

    private fun desiredAppId(): Long? {
        val candidates = buildList {
            customAppId()?.let { add(it) }
            add(CRYOGENIC_APP_ID)
            add(MINECRAFT_PRIMARY_APP_ID)
            add(MINECRAFT_LEGACY_APP_ID)
        }
        return candidates.firstOrNull { it !in rejectedIds }
    }

    private fun usingMinecraftId(id: Long) = id == MINECRAFT_PRIMARY_APP_ID || id == MINECRAFT_LEGACY_APP_ID

    private fun resolveService(context: Context): Intent? {
        val pm = context.packageManager
        for (pkg in DISCORD_PACKAGES) {
            val intent = Intent(ACTION).setPackage(pkg)
            val found = runCatching { pm.queryIntentServices(intent, 0) }.getOrNull()
            if (!found.isNullOrEmpty()) {
                val info = found.first().serviceInfo
                return Intent(ACTION).setComponent(ComponentName(info.packageName, info.name))
            }
        }
        return null
    }

    private fun attach(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (!enabled()) {
            publishStatus(Status.DISABLED)
            return
        }
        if (bound && service != null) {
            connectDesired()
            return
        }
        if (bound) return
        val intent = resolveService(app)
        if (intent == null) {
            logI("Discord app with RPC service not found")
            publishStatus(Status.NOT_INSTALLED)
            return
        }
        publishStatus(Status.CONNECTING)
        bound = try {
            app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            logW("No permission to bind Discord RPC service", e)
            publishStatus(Status.ERROR, e.message ?: "SecurityException")
            false
        } catch (e: Exception) {
            logW("Failed to bind Discord RPC service", e)
            publishStatus(Status.ERROR, e.message ?: e.javaClass.simpleName)
            false
        }
        if (!bound) scheduleRetry()
    }

    private fun connectDesired() {
        if (!wanted || !enabled()) return
        val rpc = service ?: return
        val desired = desiredAppId()
        if (desired == null) {
            publishStatus(Status.ERROR, "Discord rejected every application ID")
            return
        }
        if (connection != null && connectedAppId == desired) {
            if (ready) sendActivity()
            return
        }
        if (connection != null) dropConnection()
        val conn = try {
            rpc.connect(desired, RPC_VERSION, callback)
        } catch (e: Exception) {
            logW("connect($desired) threw", e)
            publishStatus(Status.ERROR, e.message ?: e.javaClass.simpleName)
            null
        }
        if (conn == null) {
            logW("Discord refused RPC connection for $desired")
            rejectedIds.add(desired)
            if (desiredAppId() != null) connectDesired() else scheduleRetry()
            return
        }
        connection = conn
        connectedAppId = desired
        ready = false
        publishStatus(Status.WAITING)
        main.removeCallbacks(readyTimeout)
        main.postDelayed(readyTimeout, READY_TIMEOUT_MS)
    }

    private fun handleFrame(frame: String) {
        val json = runCatching { JSONObject(frame) }.getOrNull()
        val evt = json?.optString("evt").orEmpty()
        val cmd = json?.optString("cmd").orEmpty()
        when {
            evt == "READY" || (json == null && frame.contains("\"READY\"")) -> {
                main.removeCallbacks(readyTimeout)
                ready = true
                readyAttempts = 0
                retryCount = 0
                logI("Discord READY for $connectedAppId")
                publishStatus(Status.WAITING)
                sendActivity()
                scheduleResends()
            }
            evt == "ERROR" || (json == null && frame.contains("\"ERROR\"")) -> handleError(json, cmd, frame)
            cmd == "SET_ACTIVITY" -> {
                val nonce = json?.optString("nonce").orEmpty()
                val sent = sentFrames.remove(nonce)
                val assets = json?.optJSONObject("data")?.optJSONObject("assets")
                logI("SET_ACTIVITY accepted by $connectedAppId (image=${sent?.imageMode}, assets=$assets)")
                publishStatus(Status.CONNECTED)
            }
        }
    }

    private fun handleError(json: JSONObject?, cmd: String, frame: String) {
        val data = json?.optJSONObject("data")
        val code = data?.optInt("code", -1) ?: -1
        val message = data?.optString("message")?.takeIf { it.isNotBlank() } ?: frame.take(160)
        val nonce = json?.optString("nonce").orEmpty()
        val sent = sentFrames.remove(nonce)
        logW("Discord RPC ERROR for cmd=$cmd app=$connectedAppId code=$code message=$message")
        logW("Discord RPC ERROR frame: $frame")
        if (sent != null) logW("Rejected frame (image=${sent.imageMode}, app=${sent.appId}): ${sent.frame}")
        if (cmd != "SET_ACTIVITY") {
            publishStatus(Status.ERROR, message)
            return
        }
        val lower = message.lowercase()
        when {
            "rate" in lower || "limit" in lower || "too many" in lower -> {
                logW("SET_ACTIVITY rate limited, retrying in ${RATE_LIMIT_BACKOFF_MS}ms with the same image")
                lastSentAt = SystemClock.elapsedRealtime() + RATE_LIMIT_BACKOFF_MS - MIN_SEND_INTERVAL_MS
                main.removeCallbacks(flushRunnable)
                main.postDelayed(flushRunnable, RATE_LIMIT_BACKOFF_MS)
            }
            sent != null && sent.button && "button" in lower -> {
                logW("Discord rejected the download button; sending without it")
                buttonsRejected = true
                sendActivity()
            }
            sent != null && sent.serverIcon && isImageError(lower) -> {
                logW("Discord rejected the server icon; using the launcher image only")
                serverIconRejected = true
                sendActivity()
            }
            sent != null && sent.imageMode != ImageMode.NONE && isImageError(lower) -> {
                val next = when (sent.imageMode) {
                    ImageMode.ASSET_URL -> ImageMode.ASSET_ID
                    ImageMode.ASSET_ID -> ImageMode.ASSET_KEY
                    else -> ImageMode.NONE
                }
                logW("Discord rejected image mode ${sent.imageMode}; switching to $next")
                if (next.ordinal > imageMode.ordinal) imageMode = next
                if (next == ImageMode.NONE) publishStatus(Status.ERROR, message)
                sendActivity()
            }
            else -> publishStatus(Status.ERROR, message)
        }
    }

    private fun isImageError(lower: String): Boolean =
        listOf("asset", "image", "large", "small", "url", "media", "external").any { it in lower }

    private fun handleClose(code: Int, message: String?) {
        val closed = connectedAppId
        logI("Discord closed RPC for $closed: $code $message")
        main.removeCallbacks(readyTimeout)
        ready = false
        connection = null
        connectedAppId = 0L
        if (!wanted || closed == 0L) return
        val invalidId = code == CLOSE_INVALID_CLIENT_ID || message?.contains("client", ignoreCase = true) == true
        if (invalidId) {
            rejectedIds.add(closed)
            publishStatus(Status.ERROR, "Invalid application ID $closed ($code)")
            connectDesired()
        } else if (code != CLOSE_NORMAL) {
            publishStatus(Status.ERROR, "Discord closed the connection ($code ${message.orEmpty()})".trim())
            scheduleRetry()
        }
    }

    private fun onServiceLost(reason: String) {
        main.removeCallbacks(readyTimeout)
        runCatching { serviceBinder?.unlinkToDeath(deathRecipient, 0) }
        serviceBinder = null
        service = null
        connection = null
        connectedAppId = 0L
        ready = false
        if (wanted) {
            publishStatus(Status.ERROR, reason)
            unbind()
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        if (!wanted) return
        main.removeCallbacks(retryRunnable)
        val delay = (2000L shl retryCount.coerceAtMost(5)).coerceAtMost(MAX_BACKOFF_MS)
        retryCount++
        main.postDelayed(retryRunnable, delay)
    }

    private fun scheduleResends() {
        main.removeCallbacks(resendRunnable)
        resendDelays.forEach { main.postDelayed(resendRunnable, it) }
    }

    /** Última seção do Launcher enviada (para reenviar a mesma ao voltar do jogo). */
    @Volatile
    var lastLauncherSection: String? = null
        private set

    fun inLauncher(context: Context, launcherDetails: String, section: String) {
        lastLauncherSection = section
        val app = context.applicationContext
        main.post {
            appContext = app
            handingOff = false
            wanted = true
            inGame = false
            serverAddress = null
            details = launcherDetails
            state = section
            if (startedAt == 0L) startedAt = System.currentTimeMillis()
            attach(app)
            if (ready) {
                sendActivity()
                scheduleResends()
            }
        }
    }

    fun playing(context: Context, playingDetails: String, playingState: String) {
        val app = context.applicationContext
        main.post {
            appContext = app
            wanted = true
            inGame = true
            serverAddress = null
            details = playingDetails
            state = playingState
            startedAt = System.currentTimeMillis()
            attach(app)
            if (ready) {
                sendActivity()
                scheduleResends()
            }
        }
    }

    /** Troca só a linha de baixo do status (ex.: servidor em que está jogando), mantendo o resto. */
    fun updatePlayingState(playingState: String, serverIconAddress: String? = null) {
        main.post {
            if (!inGame || (state == playingState && serverAddress == serverIconAddress)) return@post
            state = playingState
            serverAddress = serverIconAddress
            if (ready) sendActivity()
        }
    }

    fun handoffToGame() {
        handingOff = true
        main.post {
            wanted = false
            main.removeCallbacks(resendRunnable)
            main.removeCallbacks(retryRunnable)
            detach()
        }
    }

    fun launcherStopped(finishing: Boolean) {
        if (handingOff) return
        stop()
    }

    fun stop() {
        main.post {
            wanted = false
            main.removeCallbacks(resendRunnable)
            main.removeCallbacks(retryRunnable)
            main.removeCallbacks(readyTimeout)
            val target = connection
            if (target != null && ready) {
                runCatching { target.sendFrame(clearFrame()) }
                main.postDelayed({ if (!wanted) detach() }, CLEAR_FLUSH_DELAY_MS)
            } else {
                detach()
            }
            startedAt = 0L
            publishStatus(Status.DISABLED)
        }
    }

    fun reconnect(context: Context) {
        val app = context.applicationContext
        main.post {
            rejectedIds.clear()
            retryCount = 0
            readyAttempts = 0
            imageMode = ImageMode.ASSET_URL
            serverIconRejected = false
            buttonsRejected = false
            sentFrames.clear()
            detach()
            wanted = true
            if (details.isBlank()) {
                details = app.getString(com.russoviller.cryogenic.R.string.cryo_discord_in_launcher)
                state = app.getString(com.russoviller.cryogenic.R.string.cryo_discord_state_menu)
            }
            if (startedAt == 0L) startedAt = System.currentTimeMillis()
            attach(app)
        }
    }

    /** Reenvia a atividade atual (ex.: depois de mudar uma opção que só afeta o texto). */
    fun refresh() {
        main.post { if (ready) sendActivity() }
    }

    fun settingsChanged(context: Context) {
        if (enabled()) {
            val app = context.applicationContext
            main.post {
                details = app.getString(com.russoviller.cryogenic.R.string.cryo_discord_in_launcher)
                state = app.getString(com.russoviller.cryogenic.R.string.cryo_discord_state_menu)
            }
            reconnect(app)
        } else {
            stop()
        }
    }

    private fun dropConnection() {
        main.removeCallbacks(readyTimeout)
        main.removeCallbacks(flushRunnable)
        sentFrames.clear()
        ready = false
        runCatching { connection?.disconnect() }
        connection = null
        connectedAppId = 0L
    }

    private fun unbind() {
        if (bound) runCatching { appContext?.unbindService(serviceConnection) }
        bound = false
    }

    private fun detach() {
        dropConnection()
        runCatching { serviceBinder?.unlinkToDeath(deathRecipient, 0) }
        serviceBinder = null
        service = null
        unbind()
    }

    private fun clearFrame(): String = JSONObject()
        .put("cmd", "SET_ACTIVITY")
        .put("nonce", UUID.randomUUID().toString())
        .put("args", JSONObject().put("pid", HANDLER_PID))
        .toString()

    private fun fit(text: String, fallback: String): String {
        val clean = text.trim().ifBlank { fallback }
        val padded = if (clean.length < 2) "$clean " else clean
        return if (padded.length > TEXT_LIMIT) padded.take(TEXT_LIMIT - 1) + "…" else padded
    }

    private fun sendActivity() {
        val target = connection ?: return
        if (!ready || !wanted || !enabled()) return
        val now = SystemClock.elapsedRealtime()
        val wait = lastSentAt + MIN_SEND_INTERVAL_MS - now
        if (lastSentAt != 0L && wait > 0) {
            main.removeCallbacks(flushRunnable)
            main.postDelayed(flushRunnable, wait)
            return
        }
        main.removeCallbacks(flushRunnable)
        val appId = connectedAppId
        val usingMinecraft = usingMinecraftId(appId)
        val mode = when {
            usingMinecraft -> ImageMode.NONE
            appId != CRYOGENIC_APP_ID && imageMode != ImageMode.NONE -> ImageMode.ASSET_URL
            else -> imageMode
        }
        val activity = JSONObject()
            .put("name", if (usingMinecraft) "Minecraft" else "Cryogenic Launcher")
            .put("type", 0)
            .put("details", fit(details, "Cryogenic Launcher"))
        //No jogo, a segunda linha só existe quando há um servidor/mundo (ou música) para mostrar.
        //O Discord só tem duas linhas de texto: a música divide a segunda com o que já estava lá.
        val music = listeningText()
        val base = state.trim()
        val second = when {
            music == null -> base
            base.isEmpty() -> music
            else -> "$base · $music"
        }
        if (!inGame || second.isNotBlank()) {
            activity.put("state", fit(second, if (inGame) "Minecraft" else "Launcher"))
        }
        activity
            .put("timestamps", JSONObject().put("start", startedAt.coerceAtLeast(1L)))
            .put("instance", false)
        //botão à direita do status, para quem vê o perfil baixar o Launcher
        val withButton = !buttonsRejected
        if (withButton) {
            activity.put(
                "buttons",
                org.json.JSONArray().put(JSONObject().put("label", DOWNLOAD_LABEL).put("url", DOWNLOAD_URL))
            )
        }
        val launcherImage = when (mode) {
            ImageMode.ASSET_KEY -> LARGE_IMAGE_KEY
            ImageMode.ASSET_ID -> LARGE_IMAGE_ASSET_ID.toString()
            ImageMode.ASSET_URL -> largeImageUrl(CRYOGENIC_APP_ID)
            ImageMode.NONE -> null
        }
        //Em um servidor: ícone do servidor grande e a imagem do Launcher pequena no canto
        val server = serverAddress?.takeIf { inGame && !serverIconRejected && launcherImage != null }
        if (launcherImage != null) {
            val assets = JSONObject()
            if (server != null) {
                assets.put("large_image", serverIconUrl(server))
                    .put("large_text", fit(server, LARGE_IMAGE_TEXT))
                    .put("small_image", launcherImage)
                    .put("small_text", LARGE_IMAGE_TEXT)
            } else {
                assets.put("large_image", launcherImage)
                    .put("large_text", LARGE_IMAGE_TEXT)
            }
            activity.put("assets", assets)
        }
        val nonce = UUID.randomUUID().toString()
        val frame = JSONObject()
            .put("cmd", "SET_ACTIVITY")
            .put("nonce", nonce)
            .put("args", JSONObject().put("pid", HANDLER_PID).put("activity", activity))
            .toString()
        try {
            target.sendFrame(frame)
            lastSentAt = now
            sentFrames[nonce] = SentFrame(appId, mode, frame, server != null, withButton)
            while (sentFrames.size > MAX_TRACKED_FRAMES) sentFrames.remove(sentFrames.keys.first())
            logI("SET_ACTIVITY sent to $appId (image=$mode): $frame")
            if (_status.value.status != Status.ERROR) publishStatus(Status.CONNECTED)
        } catch (e: Exception) {
            logW("sendFrame failed", e)
            publishStatus(Status.ERROR, e.message ?: e.javaClass.simpleName)
            onServiceLost("sendFrame failed")
        }
    }
}
