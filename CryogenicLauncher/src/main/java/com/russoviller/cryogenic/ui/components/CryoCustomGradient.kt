package com.russoviller.cryogenic.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Degradê personalizado do tema "Personalizado": até 5 cores, cada uma pode ser ligada ou
 * desligada (a primeira e a última ficam sempre ligadas).
 *
 * Guardado como texto: `1|FFFFD36B:1,FFFFA0A0:1,...` (1/0 = degradê ligado, depois cor:ligada).
 */
object CryoCustomGradient {
    const val MAX_STOPS = 5

    data class Stop(val color: Color, val enabled: Boolean)

    val DEFAULT_STOPS = listOf(
        Stop(Color(0xFFFFD36B), true),
        Stop(Color(0xFFFFA0A0), true),
        Stop(Color(0xFFF6C3A4), true),
        Stop(Color(0xFFE29B3C), true),
        Stop(Color(0xFFDB3A21), true)
    )

    data class Config(val enabled: Boolean, val stops: List<Stop>) {
        /** Cores que entram no degradê (só as ligadas, na ordem). */
        val colors: List<Color>
            get() = stops.filterIndexed { i, s -> s.enabled || i == 0 || i == stops.lastIndex }.map { it.color }
    }

    fun parse(raw: String?): Config {
        if (raw.isNullOrBlank()) return Config(false, DEFAULT_STOPS)
        return runCatching {
            val (flag, list) = raw.split('|', limit = 2)
            val stops = list.split(',').filter { it.isNotBlank() }.take(MAX_STOPS).map { part ->
                val (hex, on) = part.split(':', limit = 2)
                Stop(Color(hex.toLong(16).toInt()), on == "1")
            }
            val full = (stops + DEFAULT_STOPS.drop(stops.size)).take(MAX_STOPS)
            Config(flag == "1", full)
        }.getOrDefault(Config(false, DEFAULT_STOPS))
    }

    fun encode(config: Config): String =
        (if (config.enabled) "1" else "0") + "|" + config.stops.joinToString(",") { s ->
            String.format("%08X", s.color.toArgb()) + ":" + (if (s.enabled) "1" else "0")
        }

    /** Cores ativas do degradê, ou null quando ele está desligado. */
    fun activeColors(raw: String?): List<Color>? {
        val c = parse(raw)
        return if (c.enabled && c.colors.size >= 2) c.colors else null
    }
}

/** Aplica um degradê personalizado ao tema: as cores principais passam a sair dele. */
fun CryoSpec.withGradient(colors: List<Color>?): CryoSpec {
    if (colors == null || colors.size < 2) return this
    return copy(
        iceSoft = colors.first(),
        ice = colors[colors.size / 2],
        aurora = colors.last(),
        gradient = colors
    )
}
