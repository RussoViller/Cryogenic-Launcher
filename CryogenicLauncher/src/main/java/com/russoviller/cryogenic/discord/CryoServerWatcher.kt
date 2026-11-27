package com.russoviller.cryogenic.discord

import android.content.Context
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.bridge.GameLogHub
import com.russoviller.cryogenic.bridge.LoggerBridge
import com.russoviller.cryogenic.setting.AllSettings

/**
 * Descobre, pelo log do jogo, em que servidor a pessoa está e atualiza o Discord.
 *
 * O Minecraft escreve no log:
 *  - `Connecting to <endereço>, <porta>` ao entrar em um servidor (todas as versões);
 *  - `Starting integrated minecraft server` ao abrir um mundo um jogador;
 *  - `Stopping server`, `Stopping!`, `Client disconnected...`, `Disconnected from server`,
 *    `Lost connection`... ao sair (nem toda versão escreve algo ao sair de um servidor).
 *
 * O log chega em pedaços que podem cortar uma linha no meio: cada pedaço é lido junto com o
 * final do anterior.
 */
object CryoServerWatcher {
    private val CONNECT = Regex("""Connecting to ([^\s,]+),\s*(\d{1,5})""")
    private val SINGLEPLAYER = listOf("Starting integrated minecraft server")
    private val LEAVE = listOf(
        "Stopping server",
        "Stopping!",
        "Client disconnected with reason",
        "Disconnected from server",
        "Lost connection",
        "Disconnecting from server",
        "Connection closed",
        "Failed to connect",
        "Can't connect to server",
        "Couldn't connect to server"
    )

    private sealed interface Place {
        data object Menu : Place
        data object Singleplayer : Place
        data class Server(val label: String, val icon: String?) : Place
    }

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var menuState: String = ""
    @Volatile
    private var place: Place = Place.Menu
    private var tail = ""
    private var started = false

    private val listener = LoggerBridge.EventLogListener { chunk -> onChunk(chunk) }

    /**
     * Começa a acompanhar o log. [menuState] é o texto normal do Discord (versão e loader),
     * usado enquanto a pessoa não está em nenhum mundo.
     */
    fun start(context: Context, menuState: String) {
        appContext = context.applicationContext
        this.menuState = menuState
        place = Place.Menu
        tail = ""
        if (!started) {
            started = true
            GameLogHub.addWatcher(listener)
        }
    }

    private fun showServer(): Boolean = runCatching { AllSettings.cryoDiscordShowServer.getValue() }.getOrDefault(true)

    private fun onChunk(chunk: String) {
        if (chunk.isEmpty()) return
        val text = tail + chunk
        tail = if (text.length > 240) text.substring(text.length - 240) else text
        // filtro barato antes das buscas
        if (!text.contains("onnect") && !text.contains("Stopping") && !text.contains("integrated") && !text.contains("ost connection")) return

        var next: Place? = null
        var lastIndex = -1
        CONNECT.findAll(text).lastOrNull()?.let { m ->
            if (m.range.first >= lastIndex) {
                lastIndex = m.range.first
                next = server(m.groupValues[1], m.groupValues[2])
            }
        }
        SINGLEPLAYER.forEach { key ->
            val i = text.lastIndexOf(key)
            if (i > lastIndex) { lastIndex = i; next = Place.Singleplayer }
        }
        LEAVE.forEach { key ->
            val i = text.lastIndexOf(key)
            if (i > lastIndex) { lastIndex = i; next = Place.Menu }
        }
        val target = next ?: return
        // o mesmo trecho pode ser lido de novo junto com o próximo pedaço: só age quando muda
        if (target == place) return
        place = target
        runCatching { com.russoviller.cryogenic.utils.logging.Logger.info("CryoServerWatcher", "Discord: $target") }
        // limpa o que já foi tratado para não repetir
        tail = ""
        publish()
    }

    /**
     * Endereço que vai para o Discord. IPs e redes locais não são mostrados, e só servidores
     * com domínio público têm o ícone buscado.
     */
    private fun server(rawHost: String, rawPort: String): Place.Server {
        val host = rawHost.trim().trimEnd('.').lowercase()
        val ctx = appContext
        val isIp = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) || host.contains(':')
        val local = host == "localhost" || host.endsWith(".local") || host.endsWith(".lan") ||
            host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("127.") ||
            Regex("""^172\.(1[6-9]|2\d|3[01])\.""").containsMatchIn(host)
        val validDomain = host.contains('.') && host.matches(Regex("""[a-z0-9._-]+"""))
        return when {
            local -> Place.Server(ctx?.getString(R.string.cryo_discord_server_lan) ?: "LAN", null)
            isIp -> Place.Server(ctx?.getString(R.string.cryo_discord_server_private) ?: "Private server", null)
            rawPort.isNotEmpty() && rawPort != "25565" -> "$host:$rawPort".let { Place.Server(it, it.takeIf { validDomain }) }
            else -> Place.Server(host, host.takeIf { validDomain })
        }
    }

    private fun publish() {
        val ctx = appContext ?: return
        val state = when (val p = place) {
            Place.Menu -> menuState
            Place.Singleplayer -> if (showServer()) ctx.getString(R.string.cryo_discord_state_singleplayer) else menuState
            is Place.Server -> if (showServer()) ctx.getString(R.string.cryo_discord_state_server, p.label) else menuState
        }
        val icon = (place as? Place.Server)?.icon?.takeIf { showServer() }
        CryoDiscordPresence.updatePlayingState(state, icon)
    }
}
