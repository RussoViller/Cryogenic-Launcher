package com.russoviller.cryogenic.hud

import android.view.KeyEvent

/**
 * Tabela de teclas comum aos esquemas de HUD. Internamente o HUD usa os códigos GLFW (os mesmos que o
 * jogo recebe); cada esquema guarda a tecla do seu jeito:
 * - Cryogenic / Zalith 1 / Pojav / Mojo: GLFW (nome "GLFW_KEY_W" ou número 87)
 * - FCL (FoldCraft Launcher): códigos do Linux (evdev, KEY_W = 17); mouse 1000..1004
 * - PojaRush: códigos de tecla do Android (KeyEvent.KEYCODE_W)
 */
object HudKeys {
    class Key(val glfw: Int, val name: String, val label: String, val evdev: Int?, val android: Int?)

    val all: List<Key> = buildList {
        fun k(glfw: Int, name: String, label: String, evdev: Int?, android: Int?) = add(Key(glfw, name, label, evdev, android))
        // letras
        val evLetters = mapOf(
            'A' to 30, 'B' to 48, 'C' to 46, 'D' to 32, 'E' to 18, 'F' to 33, 'G' to 34, 'H' to 35, 'I' to 23,
            'J' to 36, 'K' to 37, 'L' to 38, 'M' to 50, 'N' to 49, 'O' to 24, 'P' to 25, 'Q' to 16, 'R' to 19,
            'S' to 31, 'T' to 20, 'U' to 22, 'V' to 47, 'W' to 17, 'X' to 45, 'Y' to 21, 'Z' to 44
        )
        for (c in 'A'..'Z') k(65 + (c - 'A'), "GLFW_KEY_$c", c.toString(), evLetters[c], KeyEvent.KEYCODE_A + (c - 'A'))
        // números
        for (d in 0..9) k(48 + d, "GLFW_KEY_$d", d.toString(), if (d == 0) 11 else d + 1, KeyEvent.KEYCODE_0 + d)
        // F1..F12 (evdev: F1..F10 = 59..68, F11 = 87, F12 = 88)
        for (f in 1..12) k(289 + f, "GLFW_KEY_F$f", "F$f", if (f <= 10) 58 + f else 76 + f, KeyEvent.KEYCODE_F1 + f - 1)
        k(32, "GLFW_KEY_SPACE", "Space", 57, KeyEvent.KEYCODE_SPACE)
        k(39, "GLFW_KEY_APOSTROPHE", "'", 40, KeyEvent.KEYCODE_APOSTROPHE)
        k(44, "GLFW_KEY_COMMA", ",", 51, KeyEvent.KEYCODE_COMMA)
        k(45, "GLFW_KEY_MINUS", "-", 12, KeyEvent.KEYCODE_MINUS)
        k(46, "GLFW_KEY_PERIOD", ".", 52, KeyEvent.KEYCODE_PERIOD)
        k(47, "GLFW_KEY_SLASH", "/", 53, KeyEvent.KEYCODE_SLASH)
        k(59, "GLFW_KEY_SEMICOLON", ";", 39, KeyEvent.KEYCODE_SEMICOLON)
        k(61, "GLFW_KEY_EQUAL", "=", 13, KeyEvent.KEYCODE_EQUALS)
        k(91, "GLFW_KEY_LEFT_BRACKET", "[", 26, KeyEvent.KEYCODE_LEFT_BRACKET)
        k(92, "GLFW_KEY_BACKSLASH", "\\", 43, KeyEvent.KEYCODE_BACKSLASH)
        k(93, "GLFW_KEY_RIGHT_BRACKET", "]", 27, KeyEvent.KEYCODE_RIGHT_BRACKET)
        k(96, "GLFW_KEY_GRAVE_ACCENT", "`", 41, KeyEvent.KEYCODE_GRAVE)
        k(256, "GLFW_KEY_ESCAPE", "Esc", 1, KeyEvent.KEYCODE_ESCAPE)
        k(257, "GLFW_KEY_ENTER", "Enter", 28, KeyEvent.KEYCODE_ENTER)
        k(258, "GLFW_KEY_TAB", "Tab", 15, KeyEvent.KEYCODE_TAB)
        k(259, "GLFW_KEY_BACKSPACE", "⌫", 14, KeyEvent.KEYCODE_DEL)
        k(260, "GLFW_KEY_INSERT", "Ins", 110, KeyEvent.KEYCODE_INSERT)
        k(261, "GLFW_KEY_DELETE", "Del", 111, KeyEvent.KEYCODE_FORWARD_DEL)
        k(262, "GLFW_KEY_RIGHT", "→", 106, KeyEvent.KEYCODE_DPAD_RIGHT)
        k(263, "GLFW_KEY_LEFT", "←", 105, KeyEvent.KEYCODE_DPAD_LEFT)
        k(264, "GLFW_KEY_DOWN", "↓", 108, KeyEvent.KEYCODE_DPAD_DOWN)
        k(265, "GLFW_KEY_UP", "↑", 103, KeyEvent.KEYCODE_DPAD_UP)
        k(266, "GLFW_KEY_PAGE_UP", "PgUp", 104, KeyEvent.KEYCODE_PAGE_UP)
        k(267, "GLFW_KEY_PAGE_DOWN", "PgDn", 109, KeyEvent.KEYCODE_PAGE_DOWN)
        k(268, "GLFW_KEY_HOME", "Home", 102, KeyEvent.KEYCODE_MOVE_HOME)
        k(269, "GLFW_KEY_END", "End", 107, KeyEvent.KEYCODE_MOVE_END)
        k(280, "GLFW_KEY_CAPS_LOCK", "Caps", 58, KeyEvent.KEYCODE_CAPS_LOCK)
        k(281, "GLFW_KEY_SCROLL_LOCK", "ScrLk", 70, KeyEvent.KEYCODE_SCROLL_LOCK)
        k(282, "GLFW_KEY_NUM_LOCK", "NumLk", 69, KeyEvent.KEYCODE_NUM_LOCK)
        k(283, "GLFW_KEY_PRINT_SCREEN", "PrtSc", 99, KeyEvent.KEYCODE_SYSRQ)
        k(284, "GLFW_KEY_PAUSE", "Pause", 119, KeyEvent.KEYCODE_BREAK)
        for (n in 0..9) {
            val ev = intArrayOf(82, 79, 80, 81, 75, 76, 77, 71, 72, 73)[n]
            k(320 + n, "GLFW_KEY_KP_$n", "Num$n", ev, KeyEvent.KEYCODE_NUMPAD_0 + n)
        }
        k(330, "GLFW_KEY_KP_DECIMAL", "Num.", 83, KeyEvent.KEYCODE_NUMPAD_DOT)
        k(331, "GLFW_KEY_KP_DIVIDE", "Num/", 98, KeyEvent.KEYCODE_NUMPAD_DIVIDE)
        k(332, "GLFW_KEY_KP_MULTIPLY", "Num*", 55, KeyEvent.KEYCODE_NUMPAD_MULTIPLY)
        k(333, "GLFW_KEY_KP_SUBTRACT", "Num-", 74, KeyEvent.KEYCODE_NUMPAD_SUBTRACT)
        k(334, "GLFW_KEY_KP_ADD", "Num+", 78, KeyEvent.KEYCODE_NUMPAD_ADD)
        k(335, "GLFW_KEY_KP_ENTER", "NumEnter", 96, KeyEvent.KEYCODE_NUMPAD_ENTER)
        k(336, "GLFW_KEY_KP_EQUAL", "Num=", 117, KeyEvent.KEYCODE_NUMPAD_EQUALS)
        k(340, "GLFW_KEY_LEFT_SHIFT", "Shift", 42, KeyEvent.KEYCODE_SHIFT_LEFT)
        k(341, "GLFW_KEY_LEFT_CONTROL", "Ctrl", 29, KeyEvent.KEYCODE_CTRL_LEFT)
        k(342, "GLFW_KEY_LEFT_ALT", "Alt", 56, KeyEvent.KEYCODE_ALT_LEFT)
        k(343, "GLFW_KEY_LEFT_SUPER", "Super", 125, KeyEvent.KEYCODE_META_LEFT)
        k(344, "GLFW_KEY_RIGHT_SHIFT", "RShift", 54, KeyEvent.KEYCODE_SHIFT_RIGHT)
        k(345, "GLFW_KEY_RIGHT_CONTROL", "RCtrl", 97, KeyEvent.KEYCODE_CTRL_RIGHT)
        k(346, "GLFW_KEY_RIGHT_ALT", "RAlt", 100, KeyEvent.KEYCODE_ALT_RIGHT)
        k(347, "GLFW_KEY_RIGHT_SUPER", "RSuper", 126, KeyEvent.KEYCODE_META_RIGHT)
        k(348, "GLFW_KEY_MENU", "Menu", 127, KeyEvent.KEYCODE_MENU)
    }

    private val byGlfw = all.associateBy { it.glfw }
    private val byName = all.associateBy { it.name }
    private val byEvdev = all.filter { it.evdev != null }.associateBy { it.evdev!! }
    private val byAndroid = all.filter { it.android != null }.associateBy { it.android!! }

    fun byGlfw(code: Int): Key? = byGlfw[code]
    fun byName(name: String): Key? = byName[name]
    fun fromEvdev(code: Int): Key? = byEvdev[code]
    fun fromAndroid(code: Int): Key? = byAndroid[code]

    fun label(glfw: Int): String = byGlfw[glfw]?.label ?: "#$glfw"

    // ------------------------------------------------------------------ mouse

    const val MOUSE_LEFT = 0
    const val MOUSE_RIGHT = 1
    const val MOUSE_MIDDLE = 2

    fun mouseName(button: Int): String = when (button) {
        MOUSE_LEFT -> "GLFW_MOUSE_BUTTON_LEFT"
        MOUSE_RIGHT -> "GLFW_MOUSE_BUTTON_RIGHT"
        MOUSE_MIDDLE -> "GLFW_MOUSE_BUTTON_MIDDLE"
        else -> "GLFW_MOUSE_BUTTON_${button + 1}"
    }

    fun mouseFromName(name: String): Int? = when (name) {
        "GLFW_MOUSE_BUTTON_LEFT", "GLFW_MOUSE_BUTTON_1" -> MOUSE_LEFT
        "GLFW_MOUSE_BUTTON_RIGHT", "GLFW_MOUSE_BUTTON_2" -> MOUSE_RIGHT
        "GLFW_MOUSE_BUTTON_MIDDLE", "GLFW_MOUSE_BUTTON_3" -> MOUSE_MIDDLE
        else -> name.removePrefix("GLFW_MOUSE_BUTTON_").toIntOrNull()?.minus(1)?.takeIf { it in 0..7 }
    }

    fun mouseLabel(button: Int): String = when (button) {
        MOUSE_LEFT -> "LMB"
        MOUSE_RIGHT -> "RMB"
        MOUSE_MIDDLE -> "MMB"
        else -> "M${button + 1}"
    }
}
