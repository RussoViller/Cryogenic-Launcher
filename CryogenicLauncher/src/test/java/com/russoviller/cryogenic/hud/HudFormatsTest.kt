package com.russoviller.cryogenic.hud

import com.russoviller.layer_controller.layout.loadLayoutFromString
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HudFormatsTest {

    private val fclDefault = File("src/main/assets/hud/fcl_default.json").readText()

    @Test
    fun keysAreUniquePerScheme() {
        val glfw = HudKeys.all.map { it.glfw }
        assertEquals(glfw.size, glfw.toSet().size)
        val evdev = HudKeys.all.mapNotNull { it.evdev }
        assertEquals(evdev.size, evdev.toSet().size)
        val androidCodes = HudKeys.all.mapNotNull { it.android }
        assertEquals(androidCodes.size, androidCodes.toSet().size)
        assertEquals(87, HudKeys.fromEvdev(17)!!.glfw) // W
        assertEquals(340, HudKeys.fromEvdev(42)!!.glfw) // Shift
        assertEquals(32, HudKeys.fromAndroid(android.view.KeyEvent.KEYCODE_SPACE)!!.glfw)
    }

    @Test
    fun detectsEveryFormat() {
        assertEquals(HudFormat.FCL, HudDetector.detect(fclDefault))
        assertEquals(HudFormat.POJARUSH, HudDetector.detect(PojarushCodec.write(HudDefaults.starter(HudScheme.POJARUSH))))
        assertEquals(HudFormat.POJAV, HudDetector.detect("""{"mControlDataList":[],"scaledAt":100,"version":4}"""))
        assertEquals(HudFormat.CRYOGENIC, HudDetector.detect(File("src/main/assets/default_layout.json").readText()))
        assertEquals(HudFormat.UNKNOWN, HudDetector.detect("[]"))
        assertEquals(HudScheme.CRYOGENIC, HudFormat.POJAV.scheme)
    }

    @Test
    fun fclDefaultRoundTrip() {
        val a = FclCodec.parse(fclDefault, "x")
        assertEquals("Default", a.name)
        assertTrue(a.buttons.size >= 10)
        assertEquals(1, a.sticks.size)
        // a seta de mouse do FCL (1000) vira clique esquerdo
        assertTrue(a.buttons.any { b -> HudAction.Mouse(HudKeys.MOUSE_LEFT) in b.press.acts })
        val stick = a.sticks.first()
        assertEquals(listOf(HudAction.Key(87)), stick.dirs[2])
        assertEquals(listOf(HudAction.Key(87), HudAction.Key(68)), stick.dirs[1])

        val written = FclCodec.write(a)
        val b = FclCodec.parse(written, "x")
        assertEquals(a.buttons.map { it.copy(raw = null) }, b.buttons.map { it.copy(raw = null) })
        assertEquals(a.sticks.map { it.copy(raw = null) }, b.sticks.map { it.copy(raw = null) })
        // sem estilos novos quando nada mudou
        val root = HudJson.parseObject(written)
        assertEquals(HudJson.parseObject(fclDefault).objects("buttonStyles").size, root.objects("buttonStyles").size)
        assertEquals(21, root.int("controllerVersion"))
        // campos que o Cryogenic não edita continuam lá
        val ev = root.objects("viewGroups")[0].obj("viewData")!!.objects("buttonList")[0].obj("event")!!.obj("pressEvent")!!
        assertTrue(ev.containsKey("quickInput") && ev.containsKey("switchTouchMode"))
    }

    @Test
    fun fclEditCreatesStyleAndKeepsOthers() {
        val a = FclCodec.parse(fclDefault, "x")
        val first = a.buttons.first()
        val edited = a.copy(buttons = a.buttons.map { if (it.id == first.id) it.copy(style = it.style.copy(fill = 0x7F00FF00)) else it })
        val b = FclCodec.parse(FclCodec.write(edited), "x")
        assertEquals(0x7F00FF00, b.buttons.first { it.id == first.id }.style.fill)
        assertEquals(a.buttons[1].style.fill, b.buttons[1].style.fill)
        // salvar de novo não cria mais estilos
        val again = HudJson.parseObject(FclCodec.write(b)).objects("buttonStyles").size
        assertEquals(HudJson.parseObject(FclCodec.write(edited)).objects("buttonStyles").size, again)
    }

    @Test
    fun startersRoundTrip() {
        for (scheme in listOf(HudScheme.FCL, HudScheme.POJARUSH)) {
            val codec = HudCodecs.of(scheme)!!
            val s = HudDefaults.starter(scheme)
            val back = codec.parse(codec.write(s), "x")
            assertEquals(scheme.name, s.buttons.size, back.buttons.size)
            assertEquals(scheme.name, s.sticks.size, back.sticks.size)
            for ((x, y) in s.buttons.zip(back.buttons)) {
                assertEquals(x.id, y.id)
                assertEquals(x.text, y.text)
                assertEquals(scheme.name + " " + x.id, x.press.acts, y.press.acts)
                assertEquals(x.tap.acts, y.tap.acts)
                assertEquals(x.look, y.look)
                assertEquals(x.x, y.x, 0.002f)
                assertEquals(x.y, y.y, 0.002f)
            }
            assertEquals(s.layers.map { it.id }, back.layers.map { it.id })
            assertEquals(s.sticks[0].canLock, back.sticks[0].canLock)
        }
    }

    @Test
    fun pojarushParseAndKeepUnknown() {
        val json = """
        {"pojarush":1,"name":"Teste","layers":[{"id":"main","visible":true,"visibility":"ALWAYS"},{"id":"extra","visible":false,"visibility":"IN_GAME"}],
         "buttons":[
          {"id":"j","text":"Pular","layer":"main","x":0.9,"y":0.8,"width":{"dp":60},"height":{"fraction":0.15,"ofWidth":false},
           "visibility":"IN_GAME","press":[{"t":"key","code":62}],"tap":[],"toggle":[],"swipe":true,"penetrable":true,
           "interactive":true,"spam":false,"spamCps":10,"style":{"fill":1711276032,"fillPressed":-1,"text":-1,"textPressed":-16777216,
           "stroke":-1,"strokeDp":2,"radiusDp":30,"textSp":14,"radiusInPx":false,"image":"pulo.png"}},
          {"id":"l","text":"+","layer":"main","x":0,"y":0,"width":{"dp":40},"height":{"dp":40},"visibility":"ALWAYS",
           "press":[],"tap":[{"t":"layer","ids":["extra"],"mode":0},{"t":"key","code":99999}],"toggle":[{"t":"key","code":59}]}
         ],
         "sticks":[{"id":"s","layer":"main","x":0.05,"y":0.9,"size":{"fraction":0.4,"ofWidth":false},"visibility":"IN_GAME",
           "directions":{"2":[{"t":"key","code":51}],"6":[{"t":"key","code":47}]},"lock":[],"canLock":true,"lockThreshold":0.25,
           "deadZone":0.2,"doubleTap":[{"t":"key","code":59}],"autoRun":true}]}
        """.trimIndent()
        val l = PojarushCodec.parse(json, "x")
        assertEquals("Teste", l.name)
        val j = l.buttons[0]
        assertEquals(listOf(HudAction.Key(32)), j.press.acts)
        assertTrue(j.look && j.swipe)
        assertEquals(60f, j.width.dp)
        assertEquals(0.15f, j.height.fraction, 0.0001f)
        val plus = l.buttons[1]
        assertEquals(listOf(HudAction.Layer(listOf("extra"), 0)), plus.tap.acts) // tecla desconhecida ignorada no modelo
        assertEquals(listOf(HudAction.Key(340)), plus.toggle.acts)
        assertEquals(listOf(HudAction.Key(87)), l.sticks[0].dirs[2])
        assertTrue(l.sticks[0].autoRun && l.sticks[0].canLock)
        // regravar sem editar mantém a tecla desconhecida e a imagem
        val out = HudJson.parseObject(PojarushCodec.write(l))
        val b1 = out.objects("buttons")[1]
        assertEquals(2, b1.objects("tap").size)
        assertEquals("pulo.png", out.objects("buttons")[0].obj("style")!!.str("image"))
    }

    @Test
    fun pojavExpressions() {
        val vars = mapOf("margin" to 4f, "width" to 100f, "screen_width" to 1000f)
        assertEquals(212f, PojavConverter.eval("\${margin} * 3 + \${width} * 2", vars, 2f), 0.001f)
        assertEquals(450f, PojavConverter.eval("\${screen_width} / 2 - \${width} / 2", vars, 2f), 0.001f)
        assertEquals(20f, PojavConverter.eval("dp(10)", vars, 2f), 0.001f)
        assertEquals(-8f, PojavConverter.eval("-(\${margin} * 2)", vars, 2f), 0.001f)
    }

    @Test
    fun pojavConvertsToCryogenic() {
        val screen = PojavConverter.Screen(2400, 1080, 2.75f)
        val text = """
        {"mControlDataList":[
          {"name":"Keyboard","keycodes":[-1,0,0,0],"width":80,"height":30,"dynamicX":"${'$'}{margin} * 3 + ${'$'}{width} * 2","dynamicY":"${'$'}{margin}","bgColor":1291845632,"opacity":1,"strokeWidth":0,"cornerRadius":0},
          {"name":"Jump","keycodes":[32,0,0,0],"width":50,"height":50,"dynamicX":"${'$'}{right} - ${'$'}{margin}","dynamicY":"${'$'}{bottom} - ${'$'}{margin}","bgColor":1291845632,"opacity":0.8,"cornerRadius":100,"strokeWidth":1,"passThruEnabled":true,"isToggle":false},
          {"name":"PRI","keycodes":[-3,0,0,0],"width":50,"height":50,"dynamicX":"${'$'}{margin}","dynamicY":"${'$'}{screen_height} / 2","displayInMenu":false},
          {"name":"Strange","keycodes":[-5,999999,0,0],"width":50,"height":50,"x":10,"y":10}
        ],
        "mDrawerDataList":[{"properties":{"name":"F","keycodes":[0,0,0,0],"width":50,"height":50,"dynamicX":"${'$'}{screen_width} / 2","dynamicY":"${'$'}{margin}"},
           "buttonProperties":[{"name":"F1","keycodes":[290,0,0,0],"width":50,"height":50},{"name":"F2","keycodes":[291,0,0,0],"width":50,"height":50}],"orientation":"DOWN"}],
        "mJoystickDataList":[{"name":"","width":140,"height":140,"dynamicX":"${'$'}{margin}","dynamicY":"${'$'}{bottom} - ${'$'}{margin}","forwardLock":true}],
        "scaledAt":100,"version":7}
        """.trimIndent()
        val r = PojavConverter.convert(text, "pojav", screen)
        val layout = loadLayoutFromString(r.json)
        assertEquals("pojav", layout.info.name.default)
        assertEquals(2, layout.layers.size) // principal + gaveta
        val main = layout.layers.last()
        assertEquals(5, main.normalButtons.size)
        assertEquals(1, main.joystickButtons.size)
        val jump = main.normalButtons.first { it.text.default == "Jump" }
        assertTrue(jump.isPenetrable)
        assertEquals("GLFW_KEY_SPACE", jump.clickEvents.single().key)
        // ${right} - margin ≈ canto direito
        assertTrue(jump.position.x > 9900)
        val pri = main.normalButtons.first { it.text.default == "PRI" }
        assertEquals("GLFW_MOUSE_BUTTON_LEFT", pri.clickEvents.single().key)
        val drawerBtn = main.normalButtons.first { it.text.default == "F" }
        assertEquals(com.russoviller.layer_controller.event.ClickEvent.Type.SwitchLayer, drawerBtn.clickEvents.single().type)
        assertTrue(layout.layers.first().hide)
        assertEquals(2, layout.layers.first().normalButtons.size)
        assertTrue(r.skippedKeys >= 2)
        // arquivo real do PojavLauncher (opcional)
        System.getProperty("cryo.pojavSample")?.takeIf { it.isNotBlank() && File(it).isFile }?.let { path ->
            val real = PojavConverter.convert(File(path).readText(), "real", screen)
            val l2 = loadLayoutFromString(real.json)
            val buttons = l2.layers.sumOf { it.normalButtons.size }
            println("real sample: $buttons buttons, skipped=${real.skippedKeys}")
            assertTrue(buttons >= 10)
        }
    }

    @Test
    fun mismatchMessageNamesBothSystems() {
        val e = HudSchemeMismatchException(HudFormat.FCL, HudScheme.POJARUSH)
        assertTrue(e.message!!.contains("FCL") && e.message!!.contains("PojaRush"))
        assertNotNull(HudCodecs.of(HudScheme.FCL))
        assertEquals(null, HudCodecs.of(HudScheme.CRYOGENIC))
    }

    @Suppress("unused")
    private fun JsonObject.keysSorted() = keys.sorted()
}
