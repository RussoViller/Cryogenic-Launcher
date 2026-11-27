package com.russoviller.cryogenic.ui.control.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.russoviller.cryogenic.R
import com.tencent.mmkv.MMKV

/*
 * Suporte extra a controles físicos (Cryogenic):
 * - botões além do padrão (C, Z, 1..16, Home, paddles traseiros que o Android entrega como
 *   tecla desconhecida + scancode);
 * - modo de cada botão (segurar, alternar, turbo);
 * - nomes dos botões no estilo do controle (Xbox, PlayStation, Nintendo);
 * - ponte para a tela de configurações ler o controle (teste e captura de botões).
 */

/** Códigos de tecla desconhecida viram SCANCODE_BASE + scancode (paddles e botões fora do padrão). */
const val SCANCODE_BASE = 0x10000

/** Código usado nas ligações de cada direção do direcional. */
fun dpadBindingCode(direction: DpadDirection): Int = -100 - direction.ordinal

/** Código do botão no evento (desconhecido com scancode vira código próprio). */
fun KeyEvent.gamepadButtonCode(): Int =
    if (keyCode == KeyEvent.KEYCODE_UNKNOWN && scanCode > 0) SCANCODE_BASE + scanCode else keyCode

enum class GamepadGlyphStyle(val titleRes: Int) {
    Auto(R.string.cryo_gamepad_glyph_auto),
    Xbox(R.string.cryo_gamepad_glyph_xbox),
    PlayStation(R.string.cryo_gamepad_glyph_playstation),
    Nintendo(R.string.cryo_gamepad_glyph_nintendo);

    companion object {
        /** Estilo pelo fabricante (USB vendor id) do controle conectado. */
        fun forDevice(device: InputDevice?): GamepadGlyphStyle = when (device?.vendorId) {
            0x054C -> PlayStation
            0x057E -> Nintendo
            else -> Xbox
        }
    }
}

/** Nome curto do botão para o estilo escolhido. */
fun gamepadButtonLabel(code: Int, style: GamepadGlyphStyle): String {
    val ps = style == GamepadGlyphStyle.PlayStation
    val nin = style == GamepadGlyphStyle.Nintendo
    return when (code) {
        KeyEvent.KEYCODE_BUTTON_A -> if (ps) "✕" else if (nin) "B" else "A"
        KeyEvent.KEYCODE_BUTTON_B -> if (ps) "○" else if (nin) "A" else "B"
        KeyEvent.KEYCODE_BUTTON_X -> if (ps) "□" else if (nin) "Y" else "X"
        KeyEvent.KEYCODE_BUTTON_Y -> if (ps) "△" else if (nin) "X" else "Y"
        KeyEvent.KEYCODE_BUTTON_L1 -> if (ps) "L1" else if (nin) "L" else "LB"
        KeyEvent.KEYCODE_BUTTON_R1 -> if (ps) "R1" else if (nin) "R" else "RB"
        MotionEvent.AXIS_LTRIGGER, KeyEvent.KEYCODE_BUTTON_L2 -> if (ps) "L2" else if (nin) "ZL" else "LT"
        MotionEvent.AXIS_RTRIGGER, KeyEvent.KEYCODE_BUTTON_R2 -> if (ps) "R2" else if (nin) "ZR" else "RT"
        KeyEvent.KEYCODE_BUTTON_SELECT -> if (ps) "Create" else if (nin) "−" else "View"
        KeyEvent.KEYCODE_BUTTON_START -> if (ps) "Options" else if (nin) "+" else "Menu"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> if (ps) "L3" else "LS"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> if (ps) "R3" else "RS"
        KeyEvent.KEYCODE_BUTTON_MODE -> if (ps) "PS" else if (nin) "Home" else "Guide"
        KeyEvent.KEYCODE_BUTTON_C -> "C"
        KeyEvent.KEYCODE_BUTTON_Z -> "Z"
        dpadBindingCode(DpadDirection.Up) -> "↑"
        dpadBindingCode(DpadDirection.Down) -> "↓"
        dpadBindingCode(DpadDirection.Left) -> "←"
        dpadBindingCode(DpadDirection.Right) -> "→"
        in KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16 -> "B${code - KeyEvent.KEYCODE_BUTTON_1 + 1}"
        else -> when {
            code >= SCANCODE_BASE -> "SC ${Integer.toHexString(code - SCANCODE_BASE).uppercase()}"
            else -> KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_").removePrefix("BUTTON_")
        }
    }
}

/** Código padrão do jogo (o que o GamepadViewModel envia) para cada item do mapa padrão. */
fun GamepadMap.bindingCode(): Int = dpadDirection?.let { dpadBindingCode(it) } ?: gamepad

/** Modo de cada botão ligado. */
enum class GamepadBindingMode(val titleRes: Int) {
    /** Segura enquanto o botão estiver apertado (padrão) */
    Hold(R.string.cryo_gamepad_mode_hold),
    /** Um toque liga, outro desliga (ex.: correr/agachar sem segurar) */
    Toggle(R.string.cryo_gamepad_mode_toggle),
    /** Repete o clique enquanto segura */
    Turbo(R.string.cryo_gamepad_mode_turbo)
}

/**
 * Modos e velocidade do turbo de cada botão, por configuração e contexto (jogo/menu).
 * Guardado à parte do mapeamento (formato antigo continua igual).
 */
object GamepadBindingModes {
    private fun mmkv(): MMKV = MMKV.mmkvWithID("CryoGamepadModes", MMKV.MULTI_PROCESS_MODE)
    private fun key(config: String, inGame: Boolean, code: Int) = "$config|${if (inGame) 1 else 0}|$code"

    fun mode(config: String, inGame: Boolean, code: Int): GamepadBindingMode =
        GamepadBindingMode.entries.getOrNull(mmkv().decodeInt(key(config, inGame, code), 0)) ?: GamepadBindingMode.Hold

    /** Toques por segundo do turbo (2..20). */
    fun turboRate(config: String, inGame: Boolean, code: Int): Int =
        mmkv().decodeInt(key(config, inGame, code) + "|rate", 10).coerceIn(2, 20)

    fun save(config: String, inGame: Boolean, code: Int, mode: GamepadBindingMode, rate: Int) {
        val k = key(config, inGame, code)
        mmkv().apply {
            if (mode == GamepadBindingMode.Hold) removeValueForKey(k) else encode(k, mode.ordinal)
            encode("$k|rate", rate.coerceIn(2, 20))
        }
    }

    /** Todos os modos diferentes de "segurar" da configuração, por código (para o jogo). */
    fun load(config: String, inGame: Boolean): Map<Int, Pair<GamepadBindingMode, Int>> {
        val prefix = "$config|${if (inGame) 1 else 0}|"
        val m = mmkv()
        val out = HashMap<Int, Pair<GamepadBindingMode, Int>>()
        m.allKeys()?.forEach { k ->
            if (!k.startsWith(prefix) || k.endsWith("|rate")) return@forEach
            val code = k.removePrefix(prefix).toIntOrNull() ?: return@forEach
            val mode = GamepadBindingMode.entries.getOrNull(m.decodeInt(k, 0)) ?: return@forEach
            if (mode != GamepadBindingMode.Hold) out[code] = mode to m.decodeInt("$k|rate", 10).coerceIn(2, 20)
        }
        return out
    }

    fun deleteConfig(config: String) {
        val m = mmkv()
        m.allKeys()?.filter { it.startsWith("$config|") }?.forEach { m.removeValueForKey(it) }
    }
}

/** Botões extras adicionados pelo usuário (paddles, C/Z, botões numerados...), por configuração. */
object GamepadExtraButtons {
    private fun mmkv(): MMKV = MMKV.mmkvWithID("CryoGamepadExtras", MMKV.MULTI_PROCESS_MODE)

    fun list(config: String): List<Int> =
        mmkv().decodeString(config, "")!!.split(',').mapNotNull { it.trim().toIntOrNull() }.distinct()

    fun add(config: String, code: Int) {
        val now = list(config)
        if (code !in now) mmkv().encode(config, (now + code).joinToString(","))
    }

    fun remove(config: String, code: Int) {
        mmkv().encode(config, list(config).filter { it != code }.joinToString(","))
    }

    fun deleteConfig(config: String) {
        mmkv().removeValueForKey(config)
    }
}

/**
 * Ponte entre a Activity do launcher e a tela de configurações do controle: enquanto houver um
 * ouvinte, os eventos do controle vão para ele (e não navegam pelo launcher).
 */
object GamepadInputTap {
    interface Listener {
        fun onKey(event: KeyEvent): Boolean
        fun onMotion(event: MotionEvent): Boolean
    }

    @Volatile var listener: Listener? = null

    fun dispatchKey(event: KeyEvent): Boolean {
        val l = listener ?: return false
        if (!event.isGamepadKeyEvent() && !(event.keyCode == KeyEvent.KEYCODE_UNKNOWN && event.device.isGamepadDevice())) return false
        return l.onKey(event)
    }

    fun dispatchMotion(event: MotionEvent): Boolean {
        val l = listener ?: return false
        if (!event.isGamepadEvent()) return false
        return l.onMotion(event)
    }
}

/** Controles conectados agora (nome e id). */
fun connectedGamepads(): List<InputDevice> =
    InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }
        .filter { !it.isVirtual && it.isGamepadDevice() }
        .distinctBy { it.descriptor }
