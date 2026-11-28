package com.russoviller.cryogenic.hud

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudRuntimeTest {
    private val geo = HudGeometry(1000, 500, 1f)
    private val events = ArrayList<Pair<HudAction, Boolean>>()
    private val held = HashSet<HudAction>()

    private fun record(a: HudAction, p: Boolean) {
        events += a to p
        if (p) held += a else held -= a
    }

    private val W = HudAction.Key(87)
    private val SPACE = HudAction.Key(32)
    private val SHIFT = HudAction.Key(340)
    private val E = HudAction.Key(69)
    private val Q = HudAction.Key(81)

    /** Botão de 100x100 com o centro em (cx, cy). */
    private fun btn(id: String, cx: Float, cy: Float, layer: String = "main", mod: (HudButton) -> HudButton = { it }): HudButton =
        mod(HudButton(id, id, layer, x = (cx - 50f) / 900f, y = (cy - 50f) / 400f, width = HudSize(100f), height = HudSize(100f)))

    private fun layout(buttons: List<HudButton>, sticks: List<HudStick> = emptyList()) = HudLayout(
        HudScheme.POJARUSH, "t", layers = listOf(HudLayer("main"), HudLayer("extra", visible = false)), buttons = buttons, sticks = sticks
    )

    private fun p(i: Long) = PointerId(i)

    @Test
    fun pressHoldsUntilRelease() = runBlocking {
        val rt = HudRuntime(layout(listOf(btn("jump", 500f, 250f) { it.copy(press = HudTrigger(listOf(SPACE))) })), this, ::record)
        assertEquals(true, rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) {})
        assertTrue(SPACE in held)
        rt.onUp(p(1), 1000)
        assertFalse(SPACE in held)
        assertEquals(listOf(SPACE to true, SPACE to false), events)
    }

    @Test
    fun tapClicksQuickly() = runBlocking {
        val rt = HudRuntime(layout(listOf(btn("inv", 500f, 250f) { it.copy(tap = HudTrigger(listOf(E))) })), this, ::record)
        rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) {}
        assertTrue(events.isEmpty())
        rt.onUp(p(1), 100)
        delay(120)
        assertEquals(listOf(E to true, E to false), events)
        // segurar demais não é toque
        events.clear()
        rt.onDown(p(2), Offset(500f, 250f), 1000, geo, true) {}
        rt.onUp(p(2), 1000 + HudRuntime.TAP_MS + 50)
        delay(80)
        assertTrue(events.isEmpty())
    }

    @Test
    fun toggleLatches() = runBlocking {
        val rt = HudRuntime(layout(listOf(btn("sneak", 500f, 250f) { it.copy(toggle = HudTrigger(listOf(SHIFT), keep = true)) })), this, ::record)
        rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) {}; rt.onUp(p(1), 50)
        assertTrue(SHIFT in held)
        assertEquals(true, rt.pressedIds["sneak"])
        rt.onDown(p(2), Offset(500f, 250f), 500, geo, true) {}; rt.onUp(p(2), 550)
        assertFalse(SHIFT in held)
    }

    @Test
    fun longAndDoublePress() = runBlocking {
        val rt = HudRuntime(layout(listOf(btn("b", 500f, 250f) { it.copy(long = HudTrigger(listOf(Q)), double = HudTrigger(listOf(E))) })), this, ::record)
        rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) {}
        delay(HudRuntime.LONG_MS + 80)
        assertTrue(Q in held)
        rt.onUp(p(1), 600)
        assertFalse(Q in held)
        events.clear()
        rt.onDown(p(2), Offset(500f, 250f), 1000, geo, true) {}; rt.onUp(p(2), 1050)
        rt.onDown(p(3), Offset(500f, 250f), 1200, geo, true) {}; rt.onUp(p(3), 1250)
        delay(120)
        assertEquals(listOf(E to true, E to false), events)
    }

    @Test
    fun swipeMovesBetweenButtons() = runBlocking {
        val rt = HudRuntime(layout(listOf(
            btn("a", 300f, 250f) { it.copy(swipe = true, press = HudTrigger(listOf(W))) },
            btn("b", 420f, 250f) { it.copy(swipe = true, press = HudTrigger(listOf(SPACE))) }
        )), this, ::record)
        rt.onDown(p(1), Offset(300f, 250f), 0, geo, true) {}
        assertTrue(W in held)
        rt.onMove(p(1), Offset(420f, 250f), geo, true)
        assertFalse(W in held)
        assertTrue(SPACE in held)
        rt.onUp(p(1), 500)
        assertTrue(held.isEmpty())
    }

    @Test
    fun lookButtonLetsCameraUseTheFinger() = runBlocking {
        var marked = false
        val rt = HudRuntime(layout(listOf(btn("atk", 500f, 250f) { it.copy(look = true, press = HudTrigger(listOf(HudAction.Mouse(0)))) })), this, ::record)
        assertEquals(false, rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) { marked = true })
        assertTrue(marked)
        assertTrue(HudAction.Mouse(0) in held)
        assertFalse(rt.onMove(p(1), Offset(560f, 260f), geo, true))
        rt.onUp(p(1), 900)
        assertTrue(held.isEmpty())
    }

    @Test
    fun layersAndVisibility() = runBlocking {
        val rt = HudRuntime(layout(listOf(
            btn("more", 200f, 250f) { it.copy(tap = HudTrigger(listOf(HudAction.Layer(listOf("extra"), 0)))) },
            btn("f3", 500f, 250f, "extra") { it.copy(press = HudTrigger(listOf(Q))) },
            btn("menuOnly", 800f, 250f) { it.copy(visibility = HudVisibility.IN_MENU, press = HudTrigger(listOf(E))) }
        )), this, ::record)
        assertEquals(null, rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) {}) // camada escondida
        rt.onDown(p(2), Offset(200f, 250f), 0, geo, true) {}; rt.onUp(p(2), 30)
        assertEquals(true, rt.layerVisible["extra"])
        assertEquals(true, rt.onDown(p(3), Offset(500f, 250f), 100, geo, true) {})
        rt.onUp(p(3), 200)
        assertEquals(null, rt.onDown(p(4), Offset(800f, 250f), 300, geo, true) {}) // só no menu
        assertEquals(true, rt.onDown(p(5), Offset(800f, 250f), 300, geo, false) {})
        rt.onUp(p(5), 400)
    }

    @Test
    fun stickDirectionsAndSprintLock() = runBlocking {
        val stick = HudStick("s", "main", x = 0f, y = 1f, size = HudSize(200f), canLock = true, lockThreshold = 0.3f, deadZone = 0.1f)
        val rt = HudRuntime(layout(emptyList(), listOf(stick)), this, ::record)
        val c = geo.stickRect(stick).center
        rt.onDown(p(1), c, 0, geo, true) {}
        assertTrue(held.isEmpty())
        rt.onMove(p(1), c + Offset(0f, -80f), geo, true)
        assertEquals(setOf<HudAction>(W), held)
        rt.onMove(p(1), c + Offset(70f, -70f), geo, true)
        assertEquals(setOf(W, HudAction.Key(68)), held)
        rt.onMove(p(1), c + Offset(0f, -140f), geo, true) // além da borda → arma a trava
        rt.onUp(p(1), 500)
        assertEquals(setOf<HudAction>(W), held) // continua andando
        rt.onDown(p(2), c, 2000, geo, true) {}
        assertTrue(held.isEmpty()) // destrava
        rt.onUp(p(2), 2100)
        // dois toques no centro: liga o agachar (doubleKeep)
        val s2 = stick.copy(doubleActs = listOf(SHIFT), doubleKeep = true)
        val rt2 = HudRuntime(layout(emptyList(), listOf(s2)), this, ::record)
        rt2.onDown(p(3), c, 5000, geo, true) {}; rt2.onUp(p(3), 5050)
        rt2.onDown(p(4), c, 5150, geo, true) {}; rt2.onUp(p(4), 5200)
        assertTrue(SHIFT in held)
    }

    @Test
    fun hideHudAndRestore() = runBlocking {
        val rt = HudRuntime(layout(listOf(btn("hide", 500f, 250f) { it.copy(tap = HudTrigger(listOf(HudAction.HideHud))) })), this, ::record)
        rt.onDown(p(1), Offset(500f, 250f), 0, geo, true) {}; rt.onUp(p(1), 40)
        assertTrue(rt.hidden)
        assertEquals(null, rt.onDown(p(2), Offset(500f, 250f), 100, geo, true) {})
        val handle = rt.hiddenHandle(geo).center
        assertEquals(true, rt.onDown(p(3), handle, 200, geo, true) {})
        assertFalse(rt.hidden)
    }
}
