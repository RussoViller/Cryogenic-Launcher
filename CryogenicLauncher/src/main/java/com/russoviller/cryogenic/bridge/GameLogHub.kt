package com.russoviller.cryogenic.bridge

/**
 * O registro nativo do jogo aceita um único ouvinte. Este hub é esse ouvinte e repassa cada
 * trecho do log para quem precisa dele: a caixa de log na tela (quando aberta) e o detector de
 * servidor do Discord.
 *
 * Os trechos chegam pela thread que lê a saída do jogo: quem recebe precisa ser rápido.
 */
object GameLogHub {
    /** Caixa de log na tela (null = fechada). */
    @Volatile
    private var console: LoggerBridge.EventLogListener? = null

    /** Ouvintes fixos (ex.: detector de servidor). */
    private val watchers = java.util.concurrent.CopyOnWriteArrayList<LoggerBridge.EventLogListener>()

    @Volatile
    private var installed = false

    private val dispatcher = LoggerBridge.EventLogListener { text ->
        if (text == null) return@EventLogListener
        console?.let { runCatching { it.onEventLogged(text) } }
        watchers.forEach { runCatching { it.onEventLogged(text) } }
    }

    private fun ensureInstalled() {
        if (installed) return
        synchronized(this) {
            if (installed) return
            LoggerBridge.setListener(dispatcher)
            installed = true
        }
    }

    /** Liga/desliga a caixa de log na tela. */
    fun setConsole(listener: LoggerBridge.EventLogListener?) {
        console = listener
        if (listener != null || watchers.isNotEmpty()) ensureInstalled()
    }

    fun addWatcher(listener: LoggerBridge.EventLogListener) {
        if (!watchers.contains(listener)) watchers.add(listener)
        ensureInstalled()
    }

    fun removeWatcher(listener: LoggerBridge.EventLogListener) {
        watchers.remove(listener)
    }
}
