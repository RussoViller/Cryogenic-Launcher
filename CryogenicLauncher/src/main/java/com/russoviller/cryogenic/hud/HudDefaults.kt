package com.russoviller.cryogenic.hud

/** HUD inicial original do Cryogenic, gerado para os esquemas FCL e PojaRush. */
object HudDefaults {
    private fun k(name: String) = HudAction.Key(HudKeys.byName("GLFW_KEY_$name")!!.glfw)

    fun starter(scheme: HudScheme, name: String = "Cryogenic Starter", author: String = "Cryogenic Launcher"): HudLayout {
        val main = if (scheme == HudScheme.FCL) "cryo-main" else "main"
        val more = if (scheme == HudScheme.FCL) "cryo-more" else "more"
        val style = when (scheme) {
            HudScheme.FCL -> HudStyle(
                fill = 0x55101C2C, fillPressed = 0xAA7FD8FF.toInt(), stroke = 0xCC7FD8FF.toInt(), strokePressed = 0xFFFFFFFF.toInt(),
                strokeDp = 1.5f, radiusDp = 12f, textSp = 13f, name = "Cryogenic"
            )
            else -> HudStyle(
                fill = 0x55101C2C, fillPressed = 0xAA7FD8FF.toInt(), stroke = 0xCC7FD8FF.toInt(), strokePressed = 0xCC7FD8FF.toInt(),
                strokeDp = 1.5f, radiusDp = 12f, textSp = 14f
            )
        }
        val round = style.copy(radiusDp = 500f, name = if (scheme == HudScheme.FCL) "Cryogenic Round" else "")
        fun h(f: Float) = HudSize(null, f, false)
        fun btn(
            id: String, text: String, x: Float, y: Float, size: Float, vis: HudVisibility = HudVisibility.IN_GAME,
            layer: String = main, press: List<HudAction> = emptyList(), tap: List<HudAction> = emptyList(),
            toggle: List<HudAction> = emptyList(), look: Boolean = false, swipe: Boolean = false, st: HudStyle = style,
            w: Float = size
        ) = HudButton(
            id = if (scheme == HudScheme.FCL) "cryo-$id" else id,
            text = text, layer = layer, x = x, y = y, width = h(w), height = h(size), visibility = vis,
            press = HudTrigger(press),
            // FCL não tem "liga/desliga" separado: vira um "pressionar" travado
            tap = HudTrigger(tap),
            toggle = if (scheme != HudScheme.POJARUSH) HudTrigger() else HudTrigger(toggle, keep = true),
            look = look, swipe = swipe, style = st
        ).let { b ->
            when {
                scheme != HudScheme.POJARUSH && toggle.isNotEmpty() -> b.copy(press = HudTrigger(toggle, keep = true))
                // no Cryogenic os eventos disparam ao tocar
                scheme == HudScheme.CRYOGENIC && tap.isNotEmpty() -> b.copy(press = HudTrigger(press + tap), tap = HudTrigger())
                else -> b
            }
        }

        val buttons = listOf(
            btn("menu", "☰", 0f, 0f, 0.11f, HudVisibility.ALWAYS, tap = listOf(HudAction.Menu)),
            btn("esc", "Esc", 0.075f, 0f, 0.11f, HudVisibility.ALWAYS, press = listOf(k("ESCAPE"))),
            btn("chat", "💬", 0.40f, 0f, 0.11f, press = listOf(k("T"))),
            btn("keyboard", "⌨", 0.47f, 0f, 0.11f, HudVisibility.ALWAYS, tap = listOf(HudAction.Keyboard)),
            btn("camera", "F5", 0.54f, 0f, 0.11f, press = listOf(k("F5"))),
            btn("list", "Tab", 0.61f, 0f, 0.11f, press = listOf(k("TAB"))),
            btn("more", "⋯", 0.68f, 0f, 0.11f, HudVisibility.ALWAYS, tap = listOf(HudAction.Layer(listOf(more), 0))),
            btn("inventory", "E", 1f, 0f, 0.13f, press = listOf(k("E"))),
            btn("drop", "Q", 1f, 0.20f, 0.11f, press = listOf(k("Q"))),
            btn("attack", "⚔", 0.985f, 0.47f, 0.17f, press = listOf(HudAction.Mouse(HudKeys.MOUSE_LEFT)), look = true, st = round),
            btn("use", "✋", 0.86f, 0.62f, 0.15f, press = listOf(HudAction.Mouse(HudKeys.MOUSE_RIGHT)), look = true, st = round),
            btn("jump", "▲", 0.955f, 0.93f, 0.19f, press = listOf(k("SPACE")), look = true, st = round),
            btn("sneak", "⇩", 0.815f, 0.97f, 0.14f, toggle = listOf(k("LEFT_SHIFT")), st = round),
            btn("sprint", "»", 0.30f, 0.80f, 0.13f, press = listOf(k("LEFT_CONTROL")), st = round),
            btn("debug", "F3", 0.40f, 0.17f, 0.11f, HudVisibility.ALWAYS, more, press = listOf(k("F3"))),
            btn("hidegui", "F1", 0.47f, 0.17f, 0.11f, HudVisibility.ALWAYS, more, press = listOf(k("F1"))),
            btn("screenshot", "F2", 0.54f, 0.17f, 0.11f, HudVisibility.ALWAYS, more, press = listOf(k("F2"))),
            btn("swap", "F", 0.61f, 0.17f, 0.11f, layer = more, press = listOf(k("F"))),
            btn("advancements", "L", 0.68f, 0.17f, 0.11f, layer = more, press = listOf(k("L")))
        )
        val stick = HudStick(
            id = if (scheme == HudScheme.FCL) "cryo-move" else "move",
            layer = main,
            x = 0.04f, y = 0.93f, size = h(0.44f),
            visibility = HudVisibility.IN_GAME,
            mode = HudStickMode.ROCKER,
            follow = if (scheme == HudScheme.FCL) HudStickFollow.CENTER else HudStickFollow.FIXED,
            canLock = true, lockThreshold = 0.3f, deadZone = 0.12f,
            doubleActs = if (scheme == HudScheme.CRYOGENIC) emptyList() else listOf(k("LEFT_SHIFT")), doubleKeep = true,
            lockActs = if (scheme == HudScheme.CRYOGENIC) listOf(k("LEFT_CONTROL")) else emptyList(),
            style = HudStyle(
                fill = 0x33101C2C, stroke = 0xAA7FD8FF.toInt(), strokePressed = 0xAA7FD8FF.toInt(), strokeDp = 2f, radiusDp = 500f,
                knobRatio = 0.42f, knobFill = 0xAA7FD8FF.toInt(), knobStroke = 0xFFFFFFFF.toInt(),
                name = if (scheme == HudScheme.FCL) "Cryogenic Rocker" else ""
            )
        )
        val layers = listOf(
            HudLayer(main, if (scheme == HudScheme.FCL) "Cryogenic" else main, true),
            HudLayer(more, if (scheme == HudScheme.FCL) "Cryogenic +" else more, false)
        )
        return HudLayout(scheme, name, author, "1.0", "", layers, buttons, listOf(stick))
    }
}

