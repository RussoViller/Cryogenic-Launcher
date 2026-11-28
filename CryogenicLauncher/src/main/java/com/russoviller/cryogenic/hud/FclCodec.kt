package com.russoviller.cryogenic.hud

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Formato de HUD do FoldCraft Launcher (FCL, controllerVersion 21).
 * Implementação própria do formato JSON; campos que o Cryogenic não edita são mantidos ao salvar.
 */
object FclCodec : HudCodec {
    override val scheme = HudScheme.FCL
    const val CONTROLLER_VERSION = 21

    private const val MOUSE_LEFT = 1000
    private const val MOUSE_MIDDLE = 1001
    private const val MOUSE_RIGHT = 1002
    private const val SCROLL_UP = 1003
    private const val SCROLL_DOWN = 1004

    // ------------------------------------------------------------------ teclas

    fun actionOf(code: Int): HudAction? = when (code) {
        MOUSE_LEFT -> HudAction.Mouse(HudKeys.MOUSE_LEFT)
        MOUSE_MIDDLE -> HudAction.Mouse(HudKeys.MOUSE_MIDDLE)
        MOUSE_RIGHT -> HudAction.Mouse(HudKeys.MOUSE_RIGHT)
        SCROLL_UP -> HudAction.Scroll(true)
        SCROLL_DOWN -> HudAction.Scroll(false)
        else -> HudKeys.fromEvdev(code)?.let { HudAction.Key(it.glfw) }
    }

    fun codeOf(action: HudAction): Int? = when (action) {
        is HudAction.Key -> HudKeys.byGlfw(action.glfw)?.evdev
        is HudAction.Mouse -> when (action.button) {
            HudKeys.MOUSE_LEFT -> MOUSE_LEFT
            HudKeys.MOUSE_RIGHT -> MOUSE_RIGHT
            HudKeys.MOUSE_MIDDLE -> MOUSE_MIDDLE
            else -> null
        }
        is HudAction.Scroll -> if (action.up) SCROLL_UP else SCROLL_DOWN
        else -> null
    }

    // ------------------------------------------------------------------ leitura

    private fun visibility(s: String) = when (s) {
        "IN_GAME" -> HudVisibility.IN_GAME
        "IN_MENU" -> HudVisibility.IN_MENU
        else -> HudVisibility.ALWAYS
    }

    private fun visibilityName(v: HudVisibility) = v.name

    private fun buttonStyle(o: JsonObject, name: String = o.str("name")): HudStyle = HudStyle(
        fill = o.int("fillColor", 0),
        fillPressed = o.int("fillColorPressed", 0xFFCCCCCC.toInt()),
        text = o.int("textColor", -1),
        textPressed = o.int("textColorPressed", -1),
        stroke = o.int("strokeColor", 0xFF444444.toInt()),
        strokePressed = o.int("strokeColorPressed", 0xFF444444.toInt()),
        strokeDp = o.int("strokeWidth", 10) / 10f,
        radiusDp = o.int("cornerRadius", 100) / 10f,
        textSp = o.int("textSize", 12).toFloat(),
        name = name
    )

    private fun directionStyle(o: JsonObject): Pair<HudStickMode, HudStyle> {
        val name = o.str("name")
        return if (o.str("styleType") == "ROCKER") {
            val r = o.obj("rockerStyle") ?: JsonObject(emptyMap())
            HudStickMode.ROCKER to HudStyle(
                fill = r.int("bgFillColor", 0),
                fillPressed = r.int("bgFillColor", 0),
                stroke = r.int("bgStrokeColor", 0xFF444444.toInt()),
                strokePressed = r.int("bgStrokeColor", 0xFF444444.toInt()),
                strokeDp = r.int("bgStrokeWidth", 20) / 10f,
                radiusRatio = (r.int("bgCornerRadius", 500) / 1000f).coerceIn(0f, 0.5f),
                knobRadiusRatio = (r.int("rockerCornerRadius", 500) / 1000f).coerceIn(0f, 0.5f),
                knobRatio = (r.int("rockerSize", 400) / 1000f).coerceIn(0.1f, 0.9f),
                knobFill = r.int("rockerFillColor", 0xFF888888.toInt()),
                knobStroke = r.int("rockerStrokeColor", 0xFF444444.toInt()),
                name = name
            )
        } else {
            val b = o.obj("buttonStyle") ?: JsonObject(emptyMap())
            HudStickMode.DPAD to buttonStyle(b, name).copy(gapRatio = (b.int("interval", 50) / 1000f).coerceIn(0f, 0.3f))
        }
    }

    private fun trigger(o: JsonObject?): HudTrigger {
        if (o == null) return HudTrigger()
        val acts = ArrayList<HudAction>()
        o.ints("outputKeycodes").forEach { code -> actionOf(code)?.let(acts::add) }
        if (o.bool("openMenu")) acts += HudAction.Menu
        if (o.bool("input")) acts += HudAction.Keyboard
        if (o.bool("switchMouseMode")) acts += HudAction.MouseMode
        o.str("outputText").takeIf { it.isNotEmpty() }?.let { acts += HudAction.Text(it) }
        val binds = o.strings("bindViewGroup")
        val toggle = binds.filter { ':' !in it }
        val show = binds.filter { it.endsWith(":show") }.map { it.substringBefore(':') }
        val hide = binds.filter { it.endsWith(":hide") }.map { it.substringBefore(':') }
        if (toggle.isNotEmpty()) acts += HudAction.Layer(toggle, 0)
        if (show.isNotEmpty()) acts += HudAction.Layer(show, 1)
        if (hide.isNotEmpty()) acts += HudAction.Layer(hide, 2)
        return HudTrigger(acts, keep = o.bool("autoKeep"), repeat = o.bool("autoClick"))
    }

    private fun size(base: JsonObject, width: Boolean): HudSize {
        val absolute = base.int(if (width) "absoluteWidth" else "absoluteHeight", 50)
        val pct = base.obj(if (width) "percentageWidth" else "percentageHeight")
        return if (base.str("sizeType") == "ABSOLUTE") {
            HudSize(absolute.toFloat(), (pct?.int("size", 100) ?: 100) / 1000f, pct?.str("reference") == "SCREEN_WIDTH")
        } else {
            HudSize(null, ((pct?.int("size", 100) ?: 100) / 1000f).coerceIn(0.001f, 1f), pct?.str("reference") == "SCREEN_WIDTH")
        }
    }

    override fun parse(text: String, fallbackName: String): HudLayout {
        val root = HudJson.parseObject(text)
        require(root.containsKey("viewGroups")) { "Not an FCL layout" }
        val buttonStyles = root.objects("buttonStyles").associate { it.str("name") to buttonStyle(it) }
        val directionStyles = root.objects("directionStyles").associate { it.str("name") to directionStyle(it) }
        val defaultButtonStyle = buttonStyles.values.firstOrNull() ?: HudStyle(name = "Default")

        val layers = ArrayList<HudLayer>()
        val buttons = ArrayList<HudButton>()
        val sticks = ArrayList<HudStick>()
        root.objects("viewGroups").forEach { g ->
            val gid = g.str("id").ifBlank { newHudId() }
            layers += HudLayer(gid, g.str("name").ifBlank { gid }, g.str("visibility", "VISIBLE") != "INVISIBLE", raw = g)
            val data = g.obj("viewData") ?: JsonObject(emptyMap())
            data.objects("buttonList").forEach { b ->
                val base = b.obj("baseInfo") ?: JsonObject(emptyMap())
                val ev = b.obj("event") ?: JsonObject(emptyMap())
                buttons += HudButton(
                    id = b.str("id").ifBlank { newHudId() },
                    text = b.str("text"),
                    layer = gid,
                    x = (base.int("xPosition", 0) / 1000f).coerceIn(0f, 1f),
                    y = (base.int("yPosition", 0) / 1000f).coerceIn(0f, 1f),
                    width = size(base, true),
                    height = size(base, false),
                    visibility = visibility(base.str("visibilityType")),
                    press = trigger(ev.obj("pressEvent")),
                    tap = trigger(ev.obj("clickEvent")),
                    long = trigger(ev.obj("longPressEvent")),
                    double = trigger(ev.obj("doubleClickEvent")),
                    repeatCps = 50f,
                    swipe = ev.bool("swipable"),
                    look = ev.bool("pointerFollow"),
                    movable = ev.bool("Movable"),
                    style = (buttonStyles[b.str("style")] ?: defaultButtonStyle)
                        .copy(image = b.str("cryoImage"), imagePressed = b.str("cryoImagePressed")),
                    raw = b
                )
            }
            data.objects("directionList").forEach { d ->
                val base = d.obj("baseInfo") ?: JsonObject(emptyMap())
                val ev = d.obj("event") ?: JsonObject(emptyMap())
                fun keys(k: String, def: Int) = (if (ev.containsKey(k)) ev.ints(k) else listOf(def)).mapNotNull(::actionOf)
                val up = keys("upKeycode", 17); val down = keys("downKeycode", 31)
                val left = keys("leftKeycode", 30); val right = keys("rightKeycode", 32)
                val (mode, style) = directionStyles[d.str("style")] ?: (HudStickMode.ROCKER to HudStick("").style)
                val sneakKey = actionOf(ev.int("sneakKeycode", 42))
                sticks += HudStick(
                    id = d.str("id").ifBlank { newHudId() },
                    layer = gid,
                    x = (base.int("xPosition", 0) / 1000f).coerceIn(0f, 1f),
                    y = (base.int("yPosition", 0) / 1000f).coerceIn(0f, 1f),
                    size = size(base, true),
                    visibility = visibility(base.str("visibilityType")),
                    dirs = listOf(right, up + right, up, up + left, left, down + left, down, down + right),
                    mode = mode,
                    follow = when (ev.str("followOption", "CENTER_FOLLOW")) {
                        "FIXED" -> HudStickFollow.FIXED
                        "FOLLOW" -> HudStickFollow.FOLLOW
                        else -> HudStickFollow.CENTER
                    },
                    canLock = ev.bool("canLock"),
                    lockThreshold = ev.int("lockThreshold", 30) / 100f,
                    deadZone = (ev.int("deadZone", 0) / 100f).coerceIn(0f, 0.9f),
                    doubleActs = if (ev.bool("sneak", true) && sneakKey != null) listOf(sneakKey) else emptyList(),
                    doubleKeep = true,
                    style = style.copy(image = d.str("cryoImage"), imagePressed = d.str("cryoImagePressed")),
                    raw = d
                )
            }
        }
        return HudLayout(
            scheme = HudScheme.FCL,
            name = root.str("name").ifBlank { fallbackName },
            author = root.str("author"),
            version = root.str("version"),
            description = root.str("description"),
            layers = layers,
            buttons = buttons,
            sticks = sticks,
            raw = root
        )
    }

    // ------------------------------------------------------------------ escrita

    private fun buttonStyleJson(s: HudStyle, name: String): JsonObject {
        val strokeW = jp((s.strokeDp * 10).toInt())
        val radius = jp((s.radiusDp * 10).toInt())
        val textSize = jp(s.textSp.toInt())
        return jobj(
            "name" to jp(name), "textColor" to jp(s.text), "textSize" to textSize,
            "strokeColor" to jp(s.stroke), "strokeWidth" to strokeW, "cornerRadius" to radius, "fillColor" to jp(s.fill),
            "textColorPressed" to jp(s.textPressed), "textSizePressed" to textSize,
            "strokeColorPressed" to jp(s.strokePressed), "strokeWidthPressed" to strokeW,
            "cornerRadiusPressed" to radius, "fillColorPressed" to jp(s.fillPressed)
        )
    }

    private fun directionStyleJson(mode: HudStickMode, s: HudStyle, name: String, raw: JsonObject?): JsonObject {
        val base = raw ?: JsonObject(emptyMap())
        return if (mode == HudStickMode.ROCKER) {
            val rocker = (base.obj("rockerStyle") ?: JsonObject(emptyMap())).with(
                "rockerSize" to jp((s.knobRatio * 1000).toInt()),
                "bgCornerRadius" to jp((s.radiusRatio * 1000).toInt()),
                "bgStrokeWidth" to jp((s.strokeDp * 10).toInt()),
                "bgStrokeColor" to jp(s.stroke),
                "bgFillColor" to jp(s.fill),
                "rockerCornerRadius" to jp((s.knobRadiusRatio * 1000).toInt()),
                "rockerStrokeWidth" to jp(base.obj("rockerStyle")?.int("rockerStrokeWidth", 10) ?: 10),
                "rockerStrokeColor" to jp(s.knobStroke),
                "rockerFillColor" to jp(s.knobFill)
            )
            base.with("name" to jp(name), "styleType" to jp("ROCKER"), "rockerStyle" to rocker,
                "buttonStyle" to (base.obj("buttonStyle") ?: buttonStyleJson(HudStyle(), name).with("interval" to jp(50))))
        } else {
            val btn = buttonStyleJson(s, name).toMutableMap().apply { remove("name"); put("interval", jp((s.gapRatio * 1000).toInt())) }
            base.with("name" to jp(name), "styleType" to jp("BUTTON"), "buttonStyle" to JsonObject(btn),
                "rockerStyle" to (base.obj("rockerStyle") ?: jobj(
                    "rockerSize" to jp(400), "bgCornerRadius" to jp(500), "bgStrokeWidth" to jp(20),
                    "bgStrokeColor" to jp(0xFF444444.toInt()), "bgFillColor" to jp(0), "rockerCornerRadius" to jp(500),
                    "rockerStrokeWidth" to jp(10), "rockerStrokeColor" to jp(0xFF444444.toInt()), "rockerFillColor" to jp(0xFF888888.toInt())
                )))
        }
    }

    private val emptyEvent = jobj(
        "autoKeep" to jp(false), "autoClick" to jp(false), "openMenu" to jp(false), "switchTouchMode" to jp(false),
        "switchMouseMode" to jp(false), "input" to jp(false), "quickInput" to jp(false), "outputText" to jp(""),
        "outputKeycodes" to jarr(emptyList()), "bindViewGroup" to jarr(emptyList())
    )

    private fun triggerJson(t: HudTrigger, raw: JsonObject?): JsonObject {
        val base = raw ?: emptyEvent
        // mantém códigos que o Cryogenic não conhece quando as ações não mudaram
        val keycodes: List<JsonElement> = if (raw != null && trigger(raw) == t) {
            raw.arr("outputKeycodes")?.toList() ?: emptyList()
        } else t.acts.mapNotNull(::codeOf).map { jp(it) }
        val binds = t.acts.filterIsInstance<HudAction.Layer>().flatMap { l ->
            l.ids.map { id -> jp(when (l.mode) { 1 -> "$id:show"; 2 -> "$id:hide"; else -> id }) }
        }
        return base.with(
            "autoKeep" to jp(t.keep),
            "autoClick" to jp(t.repeat),
            "openMenu" to jp(HudAction.Menu in t.acts),
            "input" to jp(HudAction.Keyboard in t.acts),
            "switchMouseMode" to jp(HudAction.MouseMode in t.acts),
            "outputText" to jp(t.acts.filterIsInstance<HudAction.Text>().firstOrNull()?.text ?: ""),
            "outputKeycodes" to jarr(keycodes),
            "bindViewGroup" to jarr(binds)
        )
    }

    private fun baseInfoJson(raw: JsonObject?, x: Float, y: Float, w: HudSize, h: HudSize, v: HudVisibility): JsonObject {
        val base = raw ?: JsonObject(emptyMap())
        fun pct(s: HudSize, key: String) = (base.obj(key) ?: JsonObject(emptyMap())).with(
            "reference" to jp(if (s.ofWidth) "SCREEN_WIDTH" else "SCREEN_HEIGHT"),
            "size" to jp((s.fraction * 1000).toInt().coerceIn(1, 1000))
        )
        return base.with(
            "visibilityType" to jp(visibilityName(v)),
            "xPosition" to jp((x * 1000).toInt().coerceIn(0, 1000)),
            "yPosition" to jp((y * 1000).toInt().coerceIn(0, 1000)),
            "sizeType" to jp(if (w.dp != null) "ABSOLUTE" else "PERCENTAGE"),
            "absoluteWidth" to jp((w.dp ?: base.int("absoluteWidth", 50).toFloat()).toInt().coerceAtLeast(5)),
            "absoluteHeight" to jp((h.dp ?: base.int("absoluteHeight", 50).toFloat()).toInt().coerceAtLeast(5)),
            "percentageWidth" to pct(w, "percentageWidth"),
            "percentageHeight" to pct(h, "percentageHeight")
        )
    }

    override fun write(layout: HudLayout): String {
        val root = layout.raw ?: JsonObject(emptyMap())

        // estilos de botão: mantém os originais e cria novos só quando necessário
        val bStyles = LinkedHashMap<String, JsonObject>()
        root.objects("buttonStyles").forEach { bStyles[it.str("name")] = it }
        fun normB(s: HudStyle) = listOf(s.fill, s.fillPressed, s.text, s.textPressed, s.stroke, s.strokePressed,
            (s.strokeDp * 10).toInt(), (s.radiusDp * 10).toInt(), s.textSp.toInt())
        fun bStyleName(s: HudStyle): String {
            val plain = normB(s)
            bStyles[s.name]?.let { if (normB(buttonStyle(it)) == plain) return s.name }
            bStyles.entries.firstOrNull { normB(buttonStyle(it.value)) == plain }?.let { return it.key }
            val base = s.name.ifBlank { "Cryogenic" }
            var name = base; var i = 2
            while (bStyles.containsKey(name)) name = "$base ${i++}"
            bStyles[name] = buttonStyleJson(s, name)
            return name
        }
        val dStyles = LinkedHashMap<String, JsonObject>()
        root.objects("directionStyles").forEach { dStyles[it.str("name")] = it }
        fun dStyleName(st: HudStick): String {
            fun norm(m: HudStickMode, s: HudStyle) = if (m == HudStickMode.ROCKER)
                listOf(m, s.fill, s.stroke, (s.strokeDp * 10).toInt(), (s.radiusRatio * 1000).toInt(), (s.knobRatio * 1000).toInt(),
                    (s.knobRadiusRatio * 1000).toInt(), s.knobFill, s.knobStroke)
            else listOf(m, s.fill, s.fillPressed, s.stroke, s.strokePressed, s.text, s.textPressed, (s.strokeDp * 10).toInt(),
                (s.radiusDp * 10).toInt(), s.textSp.toInt(), (s.gapRatio * 1000).toInt())
            val want = norm(st.mode, st.style)
            fun same(o: JsonObject) = directionStyle(o).let { (m, s) -> norm(m, s) } == want
            dStyles[st.style.name]?.let { if (same(it)) return st.style.name }
            dStyles.entries.firstOrNull { same(it.value) }?.let { return it.key }
            val base = st.style.name.ifBlank { "Cryogenic" }
            var name = base; var i = 2
            while (dStyles.containsKey(name)) name = "$base ${i++}"
            dStyles[name] = directionStyleJson(st.mode, st.style, name, dStyles[st.style.name])
            return name
        }

        val layers = layout.layers.ifEmpty { listOf(HudLayer(newHudId(), "Default")) }
        val firstLayer = layers.first().id
        val known = layers.map { it.id }.toSet()
        fun layerOf(id: String) = if (id in known) id else firstLayer

        val groups = layers.map { layer ->
            val buttons = layout.buttons.filter { layerOf(it.layer) == layer.id }.map { b ->
                val rawEv = b.raw?.obj("event")
                val event = (rawEv ?: JsonObject(emptyMap())).with(
                    "pointerFollow" to jp(b.look),
                    "Movable" to jp(b.movable),
                    "swipable" to jp(b.swipe),
                    "pressEvent" to triggerJson(b.press, rawEv?.obj("pressEvent")),
                    "longPressEvent" to triggerJson(b.long, rawEv?.obj("longPressEvent")),
                    "clickEvent" to triggerJson(b.tap, rawEv?.obj("clickEvent")),
                    "doubleClickEvent" to triggerJson(b.double, rawEv?.obj("doubleClickEvent"))
                )
                (b.raw ?: JsonObject(emptyMap())).with(
                    "id" to jp(b.id),
                    "text" to jp(b.text),
                    "style" to jp(bStyleName(b.style)),
                    "baseInfo" to baseInfoJson(b.raw?.obj("baseInfo"), b.x, b.y, b.width, b.height, b.visibility),
                    "event" to event
                ).withImages(b.style)
            }
            val directions = layout.sticks.filter { layerOf(it.layer) == layer.id }.map { s ->
                fun codes(list: List<HudAction>) = jarr(list.mapNotNull { a -> (a as? HudAction.Key)?.let(::codeOf)?.let { jp(it) } })
                val rawEv = s.raw?.obj("event") ?: JsonObject(emptyMap())
                val sneak = s.doubleActs.firstOrNull { it is HudAction.Key }?.let(::codeOf)
                val event = rawEv.with(
                    "upKeycode" to codes(s.dirs[2]),
                    "downKeycode" to codes(s.dirs[6]),
                    "leftKeycode" to codes(s.dirs[4]),
                    "rightKeycode" to codes(s.dirs[0]),
                    "followOption" to jp(when (s.follow) {
                        HudStickFollow.FIXED -> "FIXED"
                        HudStickFollow.FOLLOW -> "FOLLOW"
                        HudStickFollow.CENTER -> "CENTER_FOLLOW"
                    }),
                    "sneak" to jp(sneak != null),
                    "sneakKeycode" to jp(sneak ?: rawEv.int("sneakKeycode", 42)),
                    "deadZone" to jp((s.deadZone * 100).toInt()),
                    "canLock" to jp(s.canLock),
                    "lockThreshold" to jp((s.lockThreshold * 100).toInt())
                )
                (s.raw ?: JsonObject(emptyMap())).with(
                    "id" to jp(s.id),
                    "style" to jp(dStyleName(s)),
                    "baseInfo" to baseInfoJson(s.raw?.obj("baseInfo"), s.x, s.y, s.size, s.size, s.visibility)
                        .with("sizeType" to jp(if (s.size.dp != null) "ABSOLUTE" else "PERCENTAGE")),
                    "event" to event
                ).withImages(s.style)
            }
            val rawData = layer.raw?.obj("viewData") ?: JsonObject(emptyMap())
            (layer.raw ?: JsonObject(emptyMap())).with(
                "id" to jp(layer.id),
                "name" to jp(layer.name),
                "visibility" to jp(if (layer.visible) "VISIBLE" else "INVISIBLE"),
                "viewData" to rawData.with("buttonList" to jarr(buttons), "directionList" to jarr(directions))
            )
        }
        if (bStyles.isEmpty()) bStyles["Default"] = buttonStyleJson(HudStyle(fill = 0, fillPressed = 0xFFCCCCCC.toInt(), radiusDp = 10f), "Default")
        if (dStyles.isEmpty()) dStyles["Default"] = directionStyleJson(HudStickMode.ROCKER, HudStick("").style, "Default", null)

        val out = root.with(
            "id" to jp(root.str("id").ifBlank { newHudId() }),
            "name" to jp(layout.name),
            "version" to jp(layout.version.ifBlank { "1.0" }),
            "versionCode" to jp(root.int("versionCode", 1)),
            "author" to jp(layout.author),
            "description" to jp(layout.description),
            "controllerVersion" to jp(root.int("controllerVersion", CONTROLLER_VERSION)),
            "buttonStyles" to jarr(bStyles.values.toList()),
            "directionStyles" to jarr(dStyles.values.toList()),
            "viewGroups" to jarr(groups)
        )
        return HudJson.encode(out)
    }

    override fun empty(name: String, author: String): HudLayout {
        val layer = HudLayer(newHudId(), "Default")
        return HudLayout(HudScheme.FCL, name, author, "1.0", layers = listOf(layer))
    }
}

/** Imagens PNG dos botões (campo extra do Cryogenic; o FCL ignora). */
private fun JsonObject.withImages(s: HudStyle): JsonObject {
    val m = toMutableMap()
    if (s.image.isNotEmpty()) m["cryoImage"] = jp(s.image) else m.remove("cryoImage")
    if (s.imagePressed.isNotEmpty()) m["cryoImagePressed"] = jp(s.imagePressed) else m.remove("cryoImagePressed")
    return JsonObject(m)
}
