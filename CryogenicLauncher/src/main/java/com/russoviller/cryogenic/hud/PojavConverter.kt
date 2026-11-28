package com.russoviller.cryogenic.hud

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Converte HUDs do Zalith Launcher 1 / PojavLauncher / MojoLauncher (mControlDataList…)
 * para o formato de HUD do Cryogenic (LayerController, editorVersion 12).
 *
 * As posições dinâmicas (${right} - ${width} …) são calculadas com o tamanho da tela deste aparelho.
 */
object PojavConverter {
    const val TARGET_EDITOR_VERSION = 12

    class Screen(val width: Int, val height: Int, val density: Float) {
        init {
            require(width > 0 && height > 0 && density > 0f)
        }
    }

    private const val SPECIAL_KEYBOARD = -1
    private const val SPECIAL_TOGGLE_CONTROLS = -2
    private const val SPECIAL_MOUSE_LEFT = -3
    private const val SPECIAL_MOUSE_RIGHT = -4
    private const val SPECIAL_VIRTUAL_MOUSE = -5
    private const val SPECIAL_MOUSE_MIDDLE = -6
    private const val SPECIAL_SCROLL_UP = -7
    private const val SPECIAL_SCROLL_DOWN = -8
    private const val SPECIAL_MENU = -9

    class Result(val json: String, val name: String, val buttons: Int, val joysticks: Int, val skippedKeys: Int)

    fun isPojav(root: JsonObject) = root.containsKey("mControlDataList")

    private fun uuid(): String = newHudId().replace("-", "").take(12)

    private class Ctx(val screen: Screen, val scale: Float) {
        var skipped = 0
        val styles = LinkedHashMap<String, JsonObject>() // chave visual -> estilo
        val styleIds = HashMap<String, String>()
    }

    // ------------------------------------------------------------------ expressões

    /** Avalia as expressões dinâmicas de posição do Pojav (números, + - * /, parênteses, dp(), px()). */
    internal fun eval(expr: String, vars: Map<String, Float>, density: Float): Float {
        var s = expr
        vars.forEach { (k, v) -> s = s.replace("\${$k}", v.toString()) }
        return ExprParser(s, density).parse()
    }

    private class ExprParser(val s: String, val density: Float) {
        var i = 0
        fun parse(): Float {
            val v = expr()
            skip()
            require(i >= s.length) { "Unexpected '${s.substring(i)}' in expression" }
            return v
        }
        fun skip() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun expr(): Float {
            var v = term()
            while (true) {
                skip()
                if (i < s.length && (s[i] == '+' || s[i] == '-')) {
                    val op = s[i++]; val r = term()
                    v = if (op == '+') v + r else v - r
                } else return v
            }
        }
        fun term(): Float {
            var v = factor()
            while (true) {
                skip()
                if (i < s.length && (s[i] == '*' || s[i] == '/')) {
                    val op = s[i++]; val r = factor()
                    v = if (op == '*') v * r else if (r != 0f) v / r else 0f
                } else return v
            }
        }
        fun factor(): Float {
            skip()
            require(i < s.length) { "Unexpected end of expression" }
            val c = s[i]
            if (c == '-') { i++; return -factor() }
            if (c == '+') { i++; return factor() }
            if (c == '(') { i++; val v = expr(); skip(); require(i < s.length && s[i] == ')') { "Missing ')'" }; i++; return v }
            if (c.isLetter()) {
                val start = i
                while (i < s.length && s[i].isLetter()) i++
                val name = s.substring(start, i).lowercase()
                skip()
                require(i < s.length && s[i] == '(') { "Unknown name '$name'" }
                i++
                val v = expr(); skip()
                require(i < s.length && s[i] == ')') { "Missing ')'" }
                i++
                return when (name) {
                    "dp" -> v * density
                    "px" -> v / density
                    else -> throw IllegalArgumentException("Unknown function '$name'")
                }
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'E' || s[i] == 'e' ||
                        ((s[i] == '-' || s[i] == '+') && i > start && (s[i - 1] == 'E' || s[i - 1] == 'e')))) i++
            require(i > start) { "Unexpected '$c'" }
            return s.substring(start, i).toFloat()
        }
    }

    // ------------------------------------------------------------------ partes

    private fun color(argb: Int): JsonElement = jp(argb.toLong() and 0xFFFFFFFFL)

    private fun pressedColor(argb: Int): Int {
        val a = (argb ushr 24) and 0xFF
        fun mix(c: Int) = (c + (255 - c) * 0.35f).roundToInt().coerceIn(0, 255)
        val r = mix((argb shr 16) and 0xFF); val g = mix((argb shr 8) and 0xFF); val b = mix(argb and 0xFF)
        return (max(a, 0x80) shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun styleFor(ctx: Ctx, b: JsonObject, wPx: Float, hPx: Float): String {
        val bg = b.int("bgColor", 0x4D000000)
        val stroke = b.int("strokeColor", -1)
        val strokeW = b.float("strokeWidth", 0f).coerceIn(0f, 50f).roundToInt()
        // Pojav: % da metade do menor lado · Cryogenic: % do menor lado
        val radius = (b.float("cornerRadius", 0f) / 2f).coerceIn(0f, 50f)
        val opacity = b.float("opacity", 1f).coerceIn(0f, 1f)
        val key = "$bg|$stroke|$strokeW|$radius|$opacity"
        ctx.styleIds[key]?.let { return it }
        val id = uuid()
        val shape = jobj("topStart" to jp(radius), "topEnd" to jp(radius), "bottomEnd" to jp(radius), "bottomStart" to jp(radius))
        val config = jobj(
            "alpha" to jp(opacity), "pressedAlpha" to jp(opacity),
            "backgroundColor" to color(bg), "pressedBackgroundColor" to color(pressedColor(bg)),
            "contentColor" to color(-1), "pressedContentColor" to color(-1),
            "borderWidth" to jp(strokeW), "pressedBorderWidth" to jp(strokeW),
            "borderColor" to color(stroke), "pressedBorderColor" to color(stroke),
            "borderRadius" to shape, "pressedBorderRadius" to shape
        )
        ctx.styles[key] = jobj(
            "name" to jp("Pojav ${ctx.styles.size + 1}"), "uuid" to jp(id), "animateSwap" to jp(false), "commonStyle" to jp(true),
            "lightStyle" to config, "darkStyle" to config
        )
        ctx.styleIds[key] = id
        return id
    }

    private fun events(ctx: Ctx, codes: List<Int>): List<JsonObject> = codes.mapNotNull { code ->
        fun key(name: String) = jobj("type" to jp("key"), "key" to jp(name))
        fun launcher(name: String) = jobj("type" to jp("launcher_event"), "key" to jp(name))
        when {
            code > 0 -> HudKeys.byGlfw(code)?.let { key(it.name) } ?: run { ctx.skipped++; null }
            code == 0 -> null
            code == SPECIAL_KEYBOARD -> launcher("launcher.event.switch_ime")
            code == SPECIAL_MENU -> launcher("launcher.event.switch_menu")
            code == SPECIAL_MOUSE_LEFT -> key(HudKeys.mouseName(HudKeys.MOUSE_LEFT))
            code == SPECIAL_MOUSE_RIGHT -> key(HudKeys.mouseName(HudKeys.MOUSE_RIGHT))
            code == SPECIAL_MOUSE_MIDDLE -> key(HudKeys.mouseName(HudKeys.MOUSE_MIDDLE))
            code == SPECIAL_SCROLL_UP -> launcher("launcher.event.scroll_up")
            code == SPECIAL_SCROLL_DOWN -> launcher("launcher.event.scroll_down")
            code == SPECIAL_TOGGLE_CONTROLS || code == SPECIAL_VIRTUAL_MOUSE -> { ctx.skipped++; null }
            else -> { ctx.skipped++; null }
        }
    }

    private fun visibility(b: JsonObject): String {
        val game = b.bool("displayInGame", true)
        val menu = b.bool("displayInMenu", true)
        return when {
            game && !menu -> "in_game"
            menu && !game -> "in_menu"
            else -> "always"
        }
    }

    private class Box(val x: Float, val y: Float, val w: Float, val h: Float)

    /** Calcula posição/tamanho em px na tela deste aparelho. */
    private fun box(ctx: Ctx, b: JsonObject, forceW: Float? = null, forceH: Float? = null): Box {
        val s = ctx.screen
        val w = forceW ?: (b.float("width", 50f) * ctx.scale * s.density).coerceAtLeast(5f * s.density)
        val h = forceH ?: (b.float("height", 50f) * ctx.scale * s.density).coerceAtLeast(5f * s.density)
        val vars = mapOf(
            "top" to 0f, "left" to 0f,
            // no Pojav, ${right}/${bottom} já descontam o tamanho do botão
            "right" to s.width - w, "bottom" to s.height - h,
            "width" to w, "height" to h,
            "screen_width" to s.width.toFloat(), "screen_height" to s.height.toFloat(),
            "margin" to 2f * s.density, "preferred_scale" to 100f
        )
        fun pos(dynamic: String, plain: String): Float {
            val expr = b.str(dynamic)
            if (expr.isNotBlank()) runCatching { return eval(expr, vars, s.density) }
            return b.float(plain, 0f)
        }
        return Box(pos("dynamicX", "x"), pos("dynamicY", "y"), w, h)
    }

    private fun position(ctx: Ctx, box: Box): JsonObject {
        val s = ctx.screen
        val freeX = s.width - box.w
        val freeY = s.height - box.h
        val x = if (freeX > 0f) (box.x.coerceIn(0f, freeX) / freeX) else 0f
        val y = if (freeY > 0f) (box.y.coerceIn(0f, freeY) / freeY) else 0f
        return jobj("x" to jp((x * 10000).roundToInt().coerceIn(0, 10000)), "y" to jp((y * 10000).roundToInt().coerceIn(0, 10000)))
    }

    private fun sizeJson(ctx: Ctx, box: Box): JsonObject {
        val d = ctx.screen.density
        return jobj(
            "type" to jp("dp"),
            "widthDp" to jp((box.w / d).coerceAtLeast(5f)),
            "heightDp" to jp((box.h / d).coerceAtLeast(5f)),
            "widthPercentage" to jp(1000), "heightPercentage" to jp(1000),
            "widthReference" to jp("screen_height"), "heightReference" to jp("screen_height")
        )
    }

    private fun button(ctx: Ctx, b: JsonObject, box: Box, extraEvents: List<JsonObject> = emptyList()): JsonObject {
        val text = b.str("name")
        return jobj(
            "text" to jobj("default" to jp(text), "matchQueue" to jarr(emptyList())),
            "uuid" to jp(uuid()),
            "position" to position(ctx, box),
            "buttonSize" to sizeJson(ctx, box),
            "buttonStyle" to jp(styleFor(ctx, b, box.w, box.h)),
            "textAlignment" to jp("Center"),
            "visibilityType" to jp(visibility(b)),
            "clickEvents" to jarr(events(ctx, b.ints("keycodes")) + extraEvents),
            "isSwipple" to jp(b.bool("isSwipeable")),
            "isPenetrable" to jp(b.bool("passThruEnabled")),
            "isToggleable" to jp(b.bool("isToggle"))
        )
    }

    private fun layer(name: String, uuid: String, hide: Boolean, buttons: List<JsonObject>, joysticks: List<JsonObject> = emptyList()) = jobj(
        "name" to jp(name), "uuid" to jp(uuid), "hide" to jp(hide),
        "visibilityType" to jp("always"),
        "normalButtons" to jarr(buttons), "textBoxes" to jarr(emptyList()), "joystickButtons" to jarr(joysticks)
    )

    // ------------------------------------------------------------------ conversão

    fun convert(text: String, fallbackName: String, screen: Screen): Result {
        val root = HudJson.parseObject(text)
        require(isPojav(root)) { "Not a Zalith 1 / Pojav layout" }
        val scaledAt = root.float("scaledAt", 100f).takeIf { it > 0f } ?: 100f
        val ctx = Ctx(screen, 100f / scaledAt)

        val mainButtons = root.objects("mControlDataList").map { b -> button(ctx, b, box(ctx, b)) }

        // gavetas (drawers) viram camadas que abrem/fecham
        val drawerLayers = ArrayList<JsonObject>()
        val drawerButtons = root.objects("mDrawerDataList").mapIndexed { index, drawer ->
            val props = drawer.obj("properties") ?: JsonObject(emptyMap())
            val layerId = uuid()
            val main = box(ctx, props)
            val orientation = drawer.str("orientation", "FREE").uppercase()
            val subs = drawer.objects("buttonProperties").mapIndexed { i, sub ->
                val subBox = when (orientation) {
                    "RIGHT" -> Box(main.x + main.w * (i + 1), main.y, main.w, main.h)
                    "LEFT" -> Box(main.x - main.w * (i + 1), main.y, main.w, main.h)
                    "UP" -> Box(main.x, main.y - main.h * (i + 1), main.w, main.h)
                    "DOWN" -> Box(main.x, main.y + main.h * (i + 1), main.w, main.h)
                    else -> box(ctx, sub)
                }
                button(ctx, sub, subBox)
            }
            val name = props.str("name").ifBlank { "Drawer ${index + 1}" }
            drawerLayers += layer(name, layerId, true, subs)
            button(ctx, props, main, listOf(jobj("type" to jp("switch_layer"), "key" to jp(layerId))))
        }

        val joysticks = root.objects("mJoystickDataList").map { j ->
            val bx = box(ctx, j)
            val sizePx = min(bx.w, bx.h)
            val sq = Box(bx.x, bx.y, sizePx, sizePx)
            jobj(
                "uuid" to jp(uuid()),
                "position" to position(ctx, sq),
                "sizeType" to jp("dp"),
                "sizeDp" to jp((sizePx / screen.density).coerceAtLeast(40f)),
                "sizePercentage" to jp(2500),
                "visibilityType" to jp(visibility(j)),
                "deadZoneRatio" to jp(0.15f),
                "lockThreshold" to jp(0.3f),
                "canLock" to jp(j.bool("forwardLock", true))
            )
        }

        val layers = listOf(layer("Zalith 1 / Pojav", uuid(), false, mainButtons + drawerButtons, joysticks)) + drawerLayers
        val name = root.str("name").ifBlank { fallbackName }
        val out = jobj(
            "info" to jobj(
                "name" to jobj("default" to jp(name), "matchQueue" to jarr(emptyList())),
                "author" to jobj("default" to jp(root.str("author")), "matchQueue" to jarr(emptyList())),
                "description" to jobj("default" to jp("Zalith 1 / Pojav / Mojo → Cryogenic"), "matchQueue" to jarr(emptyList())),
                "versionCode" to jp(0),
                "versionName" to jp(root.int("version", 0).takeIf { it > 0 }?.let { "pojav-v$it" } ?: "")
            ),
            "layers" to jarr(layers.reversed()),
            "styles" to jarr(ctx.styles.values.toList()),
            "editorVersion" to jp(TARGET_EDITOR_VERSION)
        )
        return Result(HudJson.encode(out), name, mainButtons.size + drawerButtons.size + drawerLayers.sumOf { it.arr("normalButtons")?.size ?: 0 }, joysticks.size, ctx.skipped)
    }
}
