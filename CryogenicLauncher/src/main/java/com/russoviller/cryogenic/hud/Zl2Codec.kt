package com.russoviller.cryogenic.hud

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.roundToInt

/**
 * Formato de HUD do Cryogenic (LayerController, o mesmo do Zalith Launcher 2).
 * Usado só pelo editor novo: o jogo continua usando o ControlBoxLayout.
 * Caixas de texto, traduções, estilos de joystick e opções avançadas são mantidos ao salvar.
 */
object Zl2Codec : HudCodec {
    override val scheme = HudScheme.CRYOGENIC
    const val EDITOR_VERSION = 12

    private val DIRS = listOf("east", "north_east", "north", "north_west", "west", "south_west", "south", "south_east")

    val defaultStyle = HudStyle(
        fill = 0x80000000.toInt(), fillPressed = 0xB3888888.toInt(), text = -1, textPressed = -1,
        stroke = -1, strokePressed = -1, strokeDp = 0f, radiusDp = 0f, radiusPercent = 0f, textSp = 14f, name = "Default"
    )

    // ------------------------------------------------------------------ cores / estilos

    private fun color(e: JsonElement?, def: Int): Int {
        val v = (e as? JsonPrimitive)?.longOrNull ?: return def
        val high = (v ushr 32) and 0xFFFFFFFFL
        return if (high != 0L) (v ushr 32).toInt() else v.toInt()
    }

    private fun colorJson(argb: Int): JsonElement = jp(argb.toLong() and 0xFFFFFFFFL)

    private fun config(o: JsonObject, name: String): HudStyle = HudStyle(
        fill = color(o["backgroundColor"], defaultStyle.fill),
        fillPressed = color(o["pressedBackgroundColor"], defaultStyle.fillPressed),
        text = color(o["contentColor"], -1),
        textPressed = color(o["pressedContentColor"], -1),
        stroke = color(o["borderColor"], -1),
        strokePressed = color(o["pressedBorderColor"], -1),
        strokeDp = o.int("borderWidth", 0).toFloat(),
        radiusDp = 0f,
        radiusPercent = o.obj("borderRadius")?.float("topStart", 0f) ?: 0f,
        textSp = o.int("fontSize", 14).toFloat(),
        opacity = o.float("alpha", 1f).coerceIn(0f, 1f),
        name = name
    )

    /** Estilo usado com o launcher no tema escuro (o mesmo que o jogo usa). */
    private fun style(o: JsonObject): HudStyle {
        val cfg = if (o.bool("commonStyle", true)) o.obj("lightStyle") else o.obj("darkStyle") ?: o.obj("lightStyle")
        return config(cfg ?: JsonObject(emptyMap()), o.str("name"))
    }

    private fun configJson(s: HudStyle, raw: JsonObject?): JsonObject {
        val r = (s.radiusPercent ?: 0f).coerceIn(0f, 50f)
        val shape = jobj("topStart" to jp(r), "topEnd" to jp(r), "bottomEnd" to jp(r), "bottomStart" to jp(r))
        val base = raw ?: JsonObject(emptyMap())
        val keepRadius = raw != null && config(raw, s.name).radiusPercent == s.radiusPercent
        return base.with(
            "alpha" to jp(s.opacity), "pressedAlpha" to jp(raw?.float("pressedAlpha", s.opacity) ?: s.opacity),
            "backgroundColor" to colorJson(s.fill), "pressedBackgroundColor" to colorJson(s.fillPressed),
            "contentColor" to colorJson(s.text), "pressedContentColor" to colorJson(s.textPressed),
            "fontSize" to jp(s.textSp.roundToInt().coerceIn(2, 30)), "pressedFontSize" to jp(s.textSp.roundToInt().coerceIn(2, 30)),
            "borderWidth" to jp(s.strokeDp.roundToInt().coerceIn(0, 50)), "pressedBorderWidth" to jp(s.strokeDp.roundToInt().coerceIn(0, 50)),
            "borderColor" to colorJson(s.stroke), "pressedBorderColor" to colorJson(s.strokePressed),
            "borderRadius" to (if (keepRadius) raw!!["borderRadius"]!! else shape),
            "pressedBorderRadius" to (if (keepRadius) raw!!["pressedBorderRadius"] ?: shape else shape)
        )
    }

    private fun normStyle(s: HudStyle) = listOf(
        s.fill, s.fillPressed, s.text, s.textPressed, s.stroke, s.strokePressed,
        s.strokeDp.roundToInt(), ((s.radiusPercent ?: 0f) * 10).roundToInt(), s.textSp.roundToInt(), (s.opacity * 100).roundToInt()
    )

    // ------------------------------------------------------------------ eventos

    fun action(e: JsonObject): HudAction? {
        val key = e.str("key")
        return when (e.str("type")) {
            "key" -> HudKeys.mouseFromName(key)?.let { HudAction.Mouse(it) } ?: HudKeys.byName(key)?.let { HudAction.Key(it.glfw) }
            "launcher_event" -> when (key) {
                "launcher.event.switch_ime" -> HudAction.Keyboard
                "launcher.event.switch_menu" -> HudAction.Menu
                "launcher.event.scroll_up" -> HudAction.Scroll(true, true)
                "launcher.event.scroll_up.single" -> HudAction.Scroll(true)
                "launcher.event.scroll_down" -> HudAction.Scroll(false, true)
                "launcher.event.scroll_down.single" -> HudAction.Scroll(false)
                else -> HudKeys.mouseFromName(key)?.let { HudAction.Mouse(it) }
            }
            "switch_layer" -> HudAction.Layer(listOf(key), 0)
            "show_layer" -> HudAction.Layer(listOf(key), 1)
            "hide_layer" -> HudAction.Layer(listOf(key), 2)
            "send_text" -> HudAction.Text(key)
            else -> null
        }
    }

    fun eventsJson(a: HudAction): List<JsonObject> {
        fun ev(type: String, key: String) = jobj("type" to jp(type), "key" to jp(key))
        return when (a) {
            is HudAction.Key -> HudKeys.byGlfw(a.glfw)?.let { listOf(ev("key", it.name)) } ?: emptyList()
            is HudAction.Mouse -> listOf(ev("key", HudKeys.mouseName(a.button)))
            is HudAction.Scroll -> listOf(ev("launcher_event", when {
                a.up && a.repeat -> "launcher.event.scroll_up"
                a.up -> "launcher.event.scroll_up.single"
                a.repeat -> "launcher.event.scroll_down"
                else -> "launcher.event.scroll_down.single"
            }))
            HudAction.Keyboard -> listOf(ev("launcher_event", "launcher.event.switch_ime"))
            HudAction.Menu -> listOf(ev("launcher_event", "launcher.event.switch_menu"))
            is HudAction.Text -> listOf(ev("send_text", a.text))
            is HudAction.Layer -> a.ids.map { ev(when (a.mode) { 1 -> "show_layer"; 2 -> "hide_layer"; else -> "switch_layer" }, it) }
            HudAction.MouseMode, HudAction.HideHud -> emptyList()
        }
    }

    private fun events(o: JsonObject, key: String): List<HudAction> = o.objects(key).mapNotNull(::action)

    private fun eventsArray(acts: List<HudAction>, raw: JsonObject?, key: String): JsonElement {
        if (raw != null && raw.containsKey(key) && events(raw, key) == acts) return raw[key]!!
        return jarr(acts.flatMap(::eventsJson))
    }

    // ------------------------------------------------------------------ partes

    private fun visibility(s: String) = when (s) {
        "in_game" -> HudVisibility.IN_GAME
        "in_menu" -> HudVisibility.IN_MENU
        else -> HudVisibility.ALWAYS
    }

    private fun visibilityJson(v: HudVisibility) = jp(when (v) {
        HudVisibility.ALWAYS -> "always"
        HudVisibility.IN_GAME -> "in_game"
        HudVisibility.IN_MENU -> "in_menu"
    })

    private fun size(o: JsonObject?, width: Boolean): HudSize {
        if (o == null) return HudSize(50f)
        val dp = o.float(if (width) "widthDp" else "heightDp", 50f)
        return when (o.str("type", "dp")) {
            "percentage" -> HudSize(
                null,
                (o.int(if (width) "widthPercentage" else "heightPercentage", 1000) / 10000f).coerceIn(0.01f, 1f),
                o.str(if (width) "widthReference" else "heightReference") == "screen_width"
            )
            else -> HudSize(dp)
        }
    }

    private fun sizeJson(w: HudSize, h: HudSize, raw: JsonObject?): JsonElement {
        if (raw != null && size(raw, true) == w && size(raw, false) == h) return raw
        val percent = w.dp == null || h.dp == null
        return jobj(
            "type" to jp(if (percent) "percentage" else "dp"),
            "widthDp" to jp((w.dp ?: raw?.float("widthDp", 50f) ?: 50f).coerceAtLeast(5f)),
            "heightDp" to jp((h.dp ?: raw?.float("heightDp", 50f) ?: 50f).coerceAtLeast(5f)),
            "widthPercentage" to jp(if (w.dp == null) (w.fraction * 10000).roundToInt().coerceIn(100, 10000) else 1000),
            "heightPercentage" to jp(if (h.dp == null) (h.fraction * 10000).roundToInt().coerceIn(100, 10000) else 1000),
            "widthReference" to jp(if (w.dp == null && w.ofWidth) "screen_width" else "screen_height"),
            "heightReference" to jp(if (h.dp == null && h.ofWidth) "screen_width" else "screen_height")
        )
    }

    private fun positionJson(x: Float, y: Float) =
        jobj("x" to jp((x * 10000).roundToInt().coerceIn(0, 10000)), "y" to jp((y * 10000).roundToInt().coerceIn(0, 10000)))

    private fun text(o: JsonObject?): String = o?.str("default") ?: ""

    private fun textJson(text: String, raw: JsonObject?): JsonObject =
        (raw ?: jobj("matchQueue" to jarr(emptyList()))).with("default" to jp(text))

    private fun stickSize(o: JsonObject): HudSize =
        if (o.str("sizeType", "percentage") == "dp") HudSize(o.float("sizeDp", 200f))
        else HudSize(null, (o.int("sizePercentage", 2500).coerceAtLeast(2000) / 10000f), false)

    // ------------------------------------------------------------------ leitura

    override fun parse(text: String, fallbackName: String): HudLayout {
        val root = HudJson.parseObject(text)
        require(root.containsKey("editorVersion")) { "Not a Cryogenic layout" }
        val styles = root.objects("styles").associate { it.str("uuid") to style(it) }
        val info = root.obj("info") ?: JsonObject(emptyMap())
        val rawLayers = root.objects("layers")
        val layers = rawLayers.map { l ->
            HudLayer(l.str("uuid").ifBlank { newHudId() }, l.str("name"), !l.bool("hide"), visibility(l.str("visibilityType")), raw = l)
        }
        val buttons = ArrayList<HudButton>()
        val sticks = ArrayList<HudStick>()
        // a primeira camada fica por cima: desenha de baixo para cima
        for (i in rawLayers.indices.reversed()) {
            val l = rawLayers[i]
            val lid = layers[i].id
            l.objects("joystickButtons").forEach { j ->
                val dirsObj = j.obj("directionEvents")
                sticks += HudStick(
                    id = j.str("uuid").ifBlank { newHudId() },
                    layer = lid,
                    x = j.obj("position")?.int("x", 0)?.div(10000f) ?: 0f,
                    y = j.obj("position")?.int("y", 0)?.div(10000f) ?: 0f,
                    size = stickSize(j),
                    visibility = visibility(j.str("visibilityType", "always")),
                    dirs = if (dirsObj == null) HudStick.defaultDirs()
                    else DIRS.map { d -> dirsObj.objects(d).mapNotNull(::action) },
                    mode = HudStickMode.ROCKER,
                    follow = HudStickFollow.FIXED,
                    canLock = j.bool("canLock", true),
                    lockThreshold = j.float("lockThreshold", 0.3f),
                    lockActs = if (j.containsKey("lockEvents")) events(j, "lockEvents") else listOf(HudAction.Key(341)),
                    deadZone = j.float("deadZoneRatio", 0.5f),
                    style = HudStyle(fill = 0x33000000, stroke = 0x99FFFFFF.toInt(), strokeDp = 2f,
                        knobRatio = 0.45f, knobFill = 0xCCFFFFFF.toInt(), knobStroke = 0x55000000),
                    raw = j
                )
            }
            l.objects("normalButtons").forEach { b ->
                val st = styles[b.str("buttonStyle")] ?: defaultStyle
                buttons += HudButton(
                    id = b.str("uuid").ifBlank { newHudId() },
                    text = text(b.obj("text")),
                    layer = lid,
                    x = b.obj("position")?.int("x", 5000)?.div(10000f) ?: 0.5f,
                    y = b.obj("position")?.int("y", 5000)?.div(10000f) ?: 0.5f,
                    width = size(b.obj("buttonSize"), true),
                    height = size(b.obj("buttonSize"), false),
                    visibility = visibility(b.str("visibilityType", "always")),
                    press = HudTrigger(events(b, "clickEvents"), keep = b.bool("isToggleable")),
                    swipe = b.bool("isSwipple"),
                    look = b.bool("isPenetrable"),
                    style = st.copy(image = b.str("cryoImage"), imagePressed = b.str("cryoImagePressed")),
                    raw = b
                )
            }
        }
        return HudLayout(
            scheme = HudScheme.CRYOGENIC,
            name = text(info.obj("name")).ifBlank { fallbackName },
            author = text(info.obj("author")),
            version = info.str("versionName"),
            description = text(info.obj("description")),
            layers = layers,
            buttons = buttons,
            sticks = sticks,
            raw = root
        )
    }

    // ------------------------------------------------------------------ escrita

    override fun write(layout: HudLayout): String {
        val root = layout.raw ?: JsonObject(emptyMap())

        val styles = LinkedHashMap<String, JsonObject>()
        root.objects("styles").forEach { styles[it.str("uuid")] = it }
        fun styleRef(s: HudStyle, original: String?): String? {
            val want = normStyle(s)
            if (original == null && want == normStyle(defaultStyle)) return null
            original?.let { id -> styles[id]?.let { if (normStyle(style(it)) == want) return id } }
            styles.entries.firstOrNull { normStyle(style(it.value)) == want }?.let { return it.key }
            val id = newHudId().replace("-", "").take(12)
            val base = original?.let { styles[it] }
            val cfg = configJson(s, base?.let { if (it.bool("commonStyle", true)) it.obj("lightStyle") else it.obj("darkStyle") })
            styles[id] = jobj(
                "name" to jp(s.name.ifBlank { "Cryogenic" }.let { n -> if (styles.values.any { it.str("name") == n }) "$n ${styles.size + 1}" else n }),
                "uuid" to jp(id), "animateSwap" to jp(base?.bool("animateSwap") ?: false), "commonStyle" to jp(true),
                "lightStyle" to cfg, "darkStyle" to cfg
            )
            return id
        }

        val layers = layout.layers.ifEmpty { listOf(HudLayer(newHudId().replace("-", "").take(12), "Layer")) }
        val known = layers.map { it.id }.toSet()
        fun layerOf(id: String) = if (id in known) id else layers.first().id

        val outLayers = layers.map { layer ->
            val buttons = layout.buttons.filter { layerOf(it.layer) == layer.id }.map { b ->
                val raw = b.raw
                val origStyle = raw?.str("buttonStyle")?.takeIf { it.isNotEmpty() }
                val ref = styleRef(b.style, origStyle)
                var o = (raw ?: JsonObject(emptyMap())).with(
                    "text" to textJson(b.text, raw?.obj("text")),
                    "uuid" to jp(b.id.replace("-", "").take(18)),
                    "position" to positionJson(b.x, b.y),
                    "buttonSize" to sizeJson(b.width, b.height, raw?.obj("buttonSize")),
                    "visibilityType" to visibilityJson(b.visibility),
                    "clickEvents" to eventsArray(b.press.acts, raw, "clickEvents"),
                    "isSwipple" to jp(b.swipe),
                    "isPenetrable" to jp(b.look),
                    "isToggleable" to jp(b.press.keep)
                )
                val m = o.toMutableMap()
                if (ref != null) m["buttonStyle"] = jp(ref) else m.remove("buttonStyle")
                if (b.style.image.isNotEmpty()) m["cryoImage"] = jp(b.style.image) else m.remove("cryoImage")
                if (b.style.imagePressed.isNotEmpty()) m["cryoImagePressed"] = jp(b.style.imagePressed) else m.remove("cryoImagePressed")
                o = JsonObject(m)
                o
            }
            val joysticks = layout.sticks.filter { layerOf(it.layer) == layer.id }.map { s ->
                val raw = s.raw
                val dirs = JsonObject(DIRS.mapIndexed { i, d ->
                    d to (raw?.obj("directionEvents")?.let { r -> if (r.objects(d).mapNotNull(::action) == s.dirs[i]) r[d] else null }
                        ?: jarr(s.dirs[i].flatMap(::eventsJson)))
                }.toMap())
                (raw ?: JsonObject(emptyMap())).with(
                    "uuid" to jp(s.id.replace("-", "").take(18)),
                    "position" to positionJson(s.x, s.y),
                    "sizeType" to jp(if (s.size.dp != null) "dp" else "percentage"),
                    "sizeDp" to jp(s.size.dp ?: raw?.float("sizeDp", 200f) ?: 200f),
                    "sizePercentage" to jp(if (s.size.dp == null) (s.size.fraction * 10000).roundToInt().coerceIn(2000, 10000) else raw?.int("sizePercentage", 2500) ?: 2500),
                    "visibilityType" to visibilityJson(s.visibility),
                    "deadZoneRatio" to jp(s.deadZone.coerceIn(0f, 0.9f)),
                    "lockThreshold" to jp(s.lockThreshold.coerceIn(0f, 1f)),
                    "canLock" to jp(s.canLock),
                    "directionEvents" to dirs,
                    "lockEvents" to eventsArray(s.lockActs, raw, "lockEvents")
                )
            }
            val raw = layer.raw ?: JsonObject(emptyMap())
            raw.with(
                "name" to jp(layer.name),
                "uuid" to jp(layer.id),
                "hide" to jp(!layer.visible),
                "visibilityType" to visibilityJson(layer.visibility),
                "normalButtons" to jarr(buttons),
                "textBoxes" to (raw["textBoxes"] ?: jarr(emptyList())),
                "joystickButtons" to jarr(joysticks)
            )
        }
        val info = root.obj("info") ?: JsonObject(emptyMap())
        val out = root.with(
            "info" to info.with(
                "name" to textJson(layout.name, info.obj("name")),
                "author" to textJson(layout.author, info.obj("author")),
                "description" to textJson(layout.description, info.obj("description")),
                "versionCode" to jp(info.int("versionCode", 0)),
                "versionName" to jp(layout.version)
            ),
            "layers" to jarr(outLayers),
            "styles" to jarr(styles.values.toList()),
            "editorVersion" to jp(root.int("editorVersion", EDITOR_VERSION))
        )
        return HudJson.encode(out)
    }

    override fun empty(name: String, author: String): HudLayout =
        HudLayout(HudScheme.CRYOGENIC, name, author, "1.0", layers = listOf(HudLayer(newHudId().replace("-", "").take(12), "Layer")))
}
