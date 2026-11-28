package com.russoviller.cryogenic.hud

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Formato de HUD do PojaRush ({"pojarush":1,...}).
 * Implementação própria e independente do formato JSON, só para compatibilidade de arquivos;
 * nenhum código ou layout do PojaRush é incluído no Cryogenic.
 */
object PojarushCodec : HudCodec {
    override val scheme = HudScheme.POJARUSH
    const val FORMAT_VERSION = 1

    private val defaultStyle = HudStyle(
        fill = 0x66000000, fillPressed = 0x99FFFFFF.toInt(), stroke = 0x80FFFFFF.toInt(),
        strokePressed = 0x80FFFFFF.toInt(), strokeDp = 1f, radiusDp = 10f, textSp = 15f
    )

    // ------------------------------------------------------------------ ações

    fun action(o: JsonObject): HudAction? = when (o.str("t")) {
        "key" -> HudKeys.fromAndroid(o.int("code"))?.let { HudAction.Key(it.glfw) }
        "mouse" -> HudAction.Mouse(o.int("button").coerceIn(0, 2))
        "scroll" -> HudAction.Scroll(o.float("lines", 1f) >= 0f, o.bool("repeat"))
        "keyboard" -> HudAction.Keyboard
        "menu" -> HudAction.Menu
        "toggle_mouse" -> HudAction.MouseMode
        "hide_controls" -> HudAction.HideHud
        "text" -> HudAction.Text(o.str("text"))
        "layer" -> HudAction.Layer(o.strings("ids"), o.int("mode").coerceIn(0, 2))
        else -> null
    }

    fun actionJson(a: HudAction): JsonObject? = when (a) {
        is HudAction.Key -> HudKeys.byGlfw(a.glfw)?.android?.let { jobj("t" to jp("key"), "code" to jp(it)) }
        is HudAction.Mouse -> jobj("t" to jp("mouse"), "button" to jp(a.button.coerceIn(0, 2)))
        is HudAction.Scroll -> jobj("t" to jp("scroll"), "lines" to jp(if (a.up) 1.0 else -1.0), "repeat" to jp(a.repeat))
        HudAction.Keyboard -> jobj("t" to jp("keyboard"))
        HudAction.Menu -> jobj("t" to jp("menu"))
        HudAction.MouseMode -> jobj("t" to jp("toggle_mouse"))
        HudAction.HideHud -> jobj("t" to jp("hide_controls"))
        is HudAction.Text -> jobj("t" to jp("text"), "text" to jp(a.text))
        is HudAction.Layer -> jobj("t" to jp("layer"), "ids" to jarr(a.ids.map { jp(it) }), "mode" to jp(a.mode))
    }

    private fun actions(o: JsonObject, key: String): List<HudAction> = o.objects(key).mapNotNull(::action)

    /** Mantém o array original quando as ações não mudaram (preserva teclas desconhecidas). */
    private fun actionsJson(list: List<HudAction>, raw: JsonObject?, key: String): JsonElement {
        if (raw != null && raw.containsKey(key) && actions(raw, key) == list) return raw[key]!!
        return jarr(list.mapNotNull(::actionJson))
    }

    // ------------------------------------------------------------------ partes

    private fun visibility(s: String) = runCatching { HudVisibility.valueOf(s) }.getOrDefault(HudVisibility.ALWAYS)

    private fun size(o: JsonObject?, def: Float): HudSize = when {
        o == null -> HudSize(def)
        o.containsKey("dp") -> HudSize(o.float("dp", def))
        else -> HudSize(null, o.float("fraction", 0.1f), o.bool("ofWidth"))
    }

    private fun sizeJson(s: HudSize): JsonObject =
        if (s.dp != null) jobj("dp" to jp(s.dp.toDouble()))
        else jobj("fraction" to jp(s.fraction.toDouble()), "ofWidth" to jp(s.ofWidth))

    private fun style(o: JsonObject?): HudStyle {
        if (o == null) return defaultStyle
        val stroke = o.int("stroke", 0)
        return HudStyle(
            fill = o.int("fill", 0), fillPressed = o.int("fillPressed", 0),
            text = o.int("text", -1), textPressed = o.int("textPressed", -1),
            stroke = stroke, strokePressed = stroke,
            strokeDp = o.float("strokeDp", 1f), radiusDp = o.float("radiusDp", 10f), radiusPx = o.bool("radiusInPx"),
            textSp = o.float("textSp", 15f), opacity = o.float("opacity", 1f).coerceIn(0f, 1f),
            knobFill = o.int("fillPressed", 0x99FFFFFF.toInt()), knobStroke = stroke,
            image = o.str("image"), imagePressed = o.str("imagePressed")
        )
    }

    private fun styleJson(s: HudStyle, raw: JsonObject?): JsonObject {
        val base = raw ?: JsonObject(emptyMap())
        val out = base.with(
            "fill" to jp(s.fill), "fillPressed" to jp(s.fillPressed), "text" to jp(s.text), "textPressed" to jp(s.textPressed),
            "stroke" to jp(s.stroke), "strokeDp" to jp(s.strokeDp.toDouble()), "radiusDp" to jp(s.radiusDp.toDouble()),
            "textSp" to jp(s.textSp.toDouble()), "radiusInPx" to jp(s.radiusPx)
        )
        val m = out.toMutableMap()
        if (s.opacity < 0.999f) m["opacity"] = jp(s.opacity.toDouble()) else m.remove("opacity")
        if (s.image.isNotEmpty()) m["image"] = jp(s.image) else m.remove("image")
        if (s.imagePressed.isNotEmpty()) m["imagePressed"] = jp(s.imagePressed) else m.remove("imagePressed")
        return JsonObject(m)
    }

    // ------------------------------------------------------------------ leitura

    override fun parse(text: String, fallbackName: String): HudLayout {
        val root = HudJson.parseObject(text)
        require(root.containsKey("pojarush") || (root.containsKey("buttons") && root.containsKey("sticks"))) { "Not a PojaRush layout" }
        val layers = root.objects("layers").map {
            val id = it.str("id")
            HudLayer(id, id, it.bool("visible", true), visibility(it.str("visibility")), raw = it)
        }
        val buttons = root.objects("buttons").map { b ->
            val spam = b.bool("spam")
            HudButton(
                id = b.str("id").ifBlank { newHudId() },
                text = b.str("text"),
                layer = b.str("layer"),
                x = b.float("x", 0.5f).coerceIn(0f, 1f),
                y = b.float("y", 0.5f).coerceIn(0f, 1f),
                width = size(b.obj("width"), 50f),
                height = size(b.obj("height"), 50f),
                visibility = visibility(b.str("visibility")),
                press = HudTrigger(actions(b, "press"), repeat = spam),
                tap = HudTrigger(actions(b, "tap")),
                toggle = HudTrigger(actions(b, "toggle"), keep = true),
                repeatCps = b.float("spamCps", 10f).coerceIn(1f, 40f),
                swipe = b.bool("swipe"),
                look = b.bool("penetrable"),
                interactive = b.bool("interactive", true),
                style = style(b.obj("style")),
                raw = b
            )
        }
        val sticks = root.objects("sticks").map { s ->
            val dirs = s.obj("directions")
            HudStick(
                id = s.str("id").ifBlank { newHudId() },
                layer = s.str("layer"),
                x = s.float("x", 0.05f).coerceIn(0f, 1f),
                y = s.float("y", 0.9f).coerceIn(0f, 1f),
                size = size(s.obj("size"), 140f),
                visibility = visibility(s.str("visibility")),
                dirs = (0..7).map { i -> dirs?.let { actions(it, i.toString()) } ?: emptyList() },
                mode = HudStickMode.ROCKER,
                follow = HudStickFollow.FIXED,
                canLock = s.bool("canLock"),
                lockThreshold = s.float("lockThreshold", 0.3f),
                lockActs = actions(s, "lock"),
                deadZone = s.float("deadZone", 0.15f).coerceIn(0f, 0.9f),
                doubleActs = actions(s, "doubleTap"),
                doubleKeep = true,
                autoRun = s.bool("autoRun"),
                style = style(s.obj("style")).let { if (s.obj("style") == null) it.copy(radiusDp = 500f) else it },
                raw = s
            )
        }
        return HudLayout(
            scheme = HudScheme.POJARUSH,
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

    override fun write(layout: HudLayout): String {
        val root = layout.raw ?: JsonObject(emptyMap())
        val layers = layout.layers.map { l ->
            (l.raw ?: JsonObject(emptyMap())).with(
                "id" to jp(l.id), "visible" to jp(l.visible), "visibility" to jp(l.visibility.name)
            )
        }
        val buttons = layout.buttons.map { b ->
            (b.raw ?: JsonObject(emptyMap())).with(
                "id" to jp(b.id), "text" to jp(b.text), "layer" to jp(b.layer),
                "x" to jp(b.x.toDouble()), "y" to jp(b.y.toDouble()),
                "width" to sizeJson(b.width), "height" to sizeJson(b.height),
                "visibility" to jp(b.visibility.name),
                "press" to actionsJson(b.press.acts, b.raw, "press"),
                "tap" to actionsJson(b.tap.acts, b.raw, "tap"),
                "toggle" to actionsJson(b.toggle.acts, b.raw, "toggle"),
                "swipe" to jp(b.swipe), "penetrable" to jp(b.look), "interactive" to jp(b.interactive),
                "spam" to jp(b.press.repeat), "spamCps" to jp(b.repeatCps.toDouble()),
                "style" to styleJson(b.style, b.raw?.obj("style"))
            )
        }
        val sticks = layout.sticks.map { s ->
            val rawDirs = s.raw?.obj("directions")
            val dirs = JsonObject((0..7).associate { i -> i.toString() to actionsJson(s.dirs[i], rawDirs, i.toString()) })
            (s.raw ?: JsonObject(emptyMap())).with(
                "id" to jp(s.id), "layer" to jp(s.layer), "x" to jp(s.x.toDouble()), "y" to jp(s.y.toDouble()),
                "size" to sizeJson(s.size), "visibility" to jp(s.visibility.name),
                "directions" to dirs,
                "lock" to actionsJson(s.lockActs, s.raw, "lock"),
                "canLock" to jp(s.canLock), "lockThreshold" to jp(s.lockThreshold.toDouble()),
                "deadZone" to jp(s.deadZone.toDouble()),
                "doubleTap" to actionsJson(s.doubleActs, s.raw, "doubleTap"),
                "autoRun" to jp(s.autoRun),
                "style" to styleJson(s.style, s.raw?.obj("style"))
            )
        }
        val out = root.with(
            "pojarush" to jp(root.int("pojarush", FORMAT_VERSION)),
            "name" to jp(layout.name),
            "author" to jp(layout.author),
            "version" to jp(layout.version),
            "description" to jp(layout.description),
            "layers" to jarr(layers),
            "buttons" to jarr(buttons),
            "sticks" to jarr(sticks)
        )
        return HudJson.encode(out)
    }

    override fun empty(name: String, author: String): HudLayout =
        HudLayout(HudScheme.POJARUSH, name, author, "1.0", layers = listOf(HudLayer("main", "main")))

    val defaultButtonStyle: HudStyle get() = defaultStyle
}
