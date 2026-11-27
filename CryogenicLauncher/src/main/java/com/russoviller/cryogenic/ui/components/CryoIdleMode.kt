package com.russoviller.cryogenic.ui.components

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Modo inativo do launcher: esconde a barra lateral e os botões do topo e deixa a tela inicial
 * só para exibição dos wallpapers (os cartões continuam lá, mas não respondem ao toque).
 */
object CryoIdleMode {
    /** Tempo mínimo (e padrão) para a entrada automática: 3 minutos. */
    const val MIN_SECONDS = 180

    /** Tempo máximo para a entrada automática: 1 hora. */
    const val MAX_SECONDS = 3600

    var active by mutableStateOf(false)

    /** Instante (elapsedRealtime) do último toque na tela do launcher. */
    @Volatile
    var lastInteraction: Long = SystemClock.elapsedRealtime()
        private set

    fun touch() {
        lastInteraction = SystemClock.elapsedRealtime()
    }

    fun clampSeconds(seconds: Int): Int = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    /** Formata como HH:MM:SS. */
    fun format(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }
}
