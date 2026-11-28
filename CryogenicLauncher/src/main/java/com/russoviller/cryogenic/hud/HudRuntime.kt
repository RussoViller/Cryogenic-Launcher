package com.russoviller.cryogenic.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/**
 * Executa um HUD dos esquemas FCL / PojaRush durante o jogo.
 * O conteúdo (mouse virtual) fica por baixo, igual ao ControlBoxLayout do esquema Cryogenic.
 */
class HudRuntime(
    val layout: HudLayout,
    private val scope: CoroutineScope,
    private val send: (HudAction, Boolean) -> Unit
) {
    companion object {
        const val TAP_MS = 220L
        const val LONG_MS = 400L
        const val DOUBLE_MS = 400L
        const val STICK_DOUBLE_MS = 300L
        const val TAP_SLOP_DP = 12f
    }

    val layerVisible = mutableStateMapOf<String, Boolean>().apply {
        layout.layers.forEach { put(it.id, it.visible) }
    }
    val pressedIds = mutableStateMapOf<String, Boolean>()
    val stickVisual = mutableStateMapOf<String, HudStickVisual>()
    val movedTo = mutableStateMapOf<String, Offset>()
    var hidden by mutableStateOf(false)

    private class ButtonState {
        var pressOn = false
        var tapOn = false
        var longOn = false
        var longFired = false
        var doubleOn = false
        var toggleOn = false
        var downTime = 0L
        var lastQuickTap = 0L
        var longJob: Job? = null
        var pressRepeat: Job? = null
        var longRepeat: Job? = null
        var holders = 0
    }

    private class StickState {
        var held: List<HudAction> = emptyList()
        var locked = false
        var armed = false
        var lastDown = 0L
        var sneakOn = false
        var center: Offset = Offset.Zero
    }

    private sealed interface Track {
        class Button(var id: String, val origin: String, val down: Offset, val time: Long, val look: Boolean) : Track {
            var swiped = false
            var moved = false
            var grab = Offset.Zero
        }
        class Stick(val id: String) : Track
    }

    private val buttonStates = HashMap<String, ButtonState>()
    private val stickStates = HashMap<String, StickState>()
    private val tracks = HashMap<PointerId, Track>()
    private val holdCount = HashMap<HudAction, Int>()

    private val buttonsById = layout.buttons.associateBy { it.id }
    private val sticksById = layout.sticks.associateBy { it.id }

    // ------------------------------------------------------------------ ações

    private fun holdable(a: HudAction) = a.isHoldable || (a is HudAction.Scroll && a.repeat)

    private fun down(a: HudAction) {
        if (holdable(a)) {
            val n = holdCount[a] ?: 0
            holdCount[a] = n + 1
            if (n == 0) send(a, true)
        } else instant(a)
    }

    private fun up(a: HudAction) {
        if (!holdable(a)) return
        val n = holdCount[a] ?: return
        if (n <= 1) {
            holdCount.remove(a)
            send(a, false)
        } else holdCount[a] = n - 1
    }

    private fun instant(a: HudAction) {
        when (a) {
            is HudAction.Layer -> a.ids.forEach { id ->
                layerVisible[id] = when (a.mode) {
                    1 -> true
                    2 -> false
                    else -> layerVisible[id] == false
                }
            }
            HudAction.HideHud -> {
                hidden = true
                releaseAll()
            }
            else -> {
                send(a, true)
                send(a, false)
            }
        }
    }

    /** Clique rápido: aperta e solta depois de um instante (o jogo precisa ver os dois). */
    private fun click(acts: List<HudAction>) {
        val holds = acts.filter(::holdable)
        acts.filterNot(::holdable).forEach(::instant)
        if (holds.isEmpty()) return
        holds.forEach(::down)
        scope.launch {
            delay(40)
            holds.forEach(::up)
        }
    }

    private fun startRepeat(acts: List<HudAction>, cps: Float): Job {
        val period = (1000f / cps.coerceIn(1f, 50f)).toLong().coerceAtLeast(20L)
        acts.filterNot(::holdable).forEach(::instant)
        val holds = acts.filter(::holdable)
        return scope.launch {
            while (isActive) {
                holds.forEach(::down)
                delay(minOf(15L, period / 2))
                holds.forEach(::up)
                delay(period - minOf(15L, period / 2))
            }
        }
    }

    /** Ativa/desativa um gatilho. on=false só solta as teclas seguradas / para a repetição. */
    private fun fire(t: HudTrigger, on: Boolean, cps: Float, job: () -> Job?, setJob: (Job?) -> Unit) {
        if (t.isEmpty) return
        if (t.repeat) {
            job()?.cancel()
            setJob(if (on) startRepeat(t.acts, cps) else null)
        } else if (on) t.acts.forEach(::down) else t.acts.forEach(::up)
    }

    // ------------------------------------------------------------------ botões

    private fun state(id: String) = buttonStates.getOrPut(id) { ButtonState() }

    private fun refreshPressed(id: String) {
        val s = state(id)
        val on = s.holders > 0 || s.pressOn || s.toggleOn || s.longOn || s.tapOn || s.doubleOn
        if (on) pressedIds[id] = true else pressedIds.remove(id)
    }

    private fun firePress(b: HudButton, s: ButtonState, on: Boolean) = fire(b.press, on, b.repeatCps, { s.pressRepeat }, { s.pressRepeat = it })
    private fun fireLong(b: HudButton, s: ButtonState, on: Boolean) = fire(b.long, on, b.repeatCps, { s.longRepeat }, { s.longRepeat = it })

    private fun buttonDown(b: HudButton, now: Long) {
        val s = state(b.id)
        s.holders++
        s.downTime = now
        s.longFired = false
        // pressionar
        if (b.press.keep) {
            s.pressOn = !s.pressOn
            firePress(b, s, s.pressOn)
        } else firePress(b, s, true)
        // liga/desliga (PojaRush)
        if (!b.toggle.isEmpty) {
            s.toggleOn = !s.toggleOn
            if (s.toggleOn) b.toggle.acts.forEach(::down) else b.toggle.acts.forEach(::up)
        }
        // segurar
        if (!b.long.isEmpty) {
            s.longJob?.cancel()
            s.longJob = scope.launch {
                delay(LONG_MS)
                s.longFired = true
                if (b.long.keep) {
                    s.longOn = !s.longOn
                    fireLong(b, s, s.longOn)
                } else {
                    s.longOn = true
                    fireLong(b, s, true)
                }
                refreshPressed(b.id)
            }
        }
        refreshPressed(b.id)
    }

    /** quick = foi um toque rápido sem arrastar (conta como clique). */
    private fun buttonUp(b: HudButton, now: Long, quick: Boolean) {
        val s = state(b.id)
        if (s.holders > 0) s.holders--
        s.longJob?.cancel()
        s.longJob = null
        if (s.longFired && !b.long.keep && s.longOn) {
            s.longOn = false
            fireLong(b, s, false)
        }
        if (!b.press.keep) firePress(b, s, false)
        if (quick) {
            if (!b.tap.isEmpty) {
                if (b.tap.keep) {
                    s.tapOn = !s.tapOn
                    if (s.tapOn) b.tap.acts.forEach(::down) else b.tap.acts.forEach(::up)
                } else click(b.tap.acts)
            }
            if (!b.double.isEmpty) {
                if (s.lastQuickTap > 0 && now - s.lastQuickTap < DOUBLE_MS) {
                    s.lastQuickTap = 0
                    if (b.double.keep) {
                        s.doubleOn = !s.doubleOn
                        if (s.doubleOn) b.double.acts.forEach(::down) else b.double.acts.forEach(::up)
                    } else click(b.double.acts)
                } else s.lastQuickTap = now
            }
        }
        refreshPressed(b.id)
    }

    /** Saída do dedo de um botão numa cadeia de deslize: solta só o "pressionar". */
    private fun buttonSwipeExit(b: HudButton) {
        val s = state(b.id)
        if (s.holders > 0) s.holders--
        s.longJob?.cancel()
        if (s.longOn && !b.long.keep) {
            s.longOn = false
            fireLong(b, s, false)
        }
        if (!b.press.keep) firePress(b, s, false)
        refreshPressed(b.id)
    }

    private fun buttonSwipeEnter(b: HudButton) {
        val s = state(b.id)
        s.holders++
        if (!b.press.keep) firePress(b, s, true)
        refreshPressed(b.id)
    }

    // ------------------------------------------------------------------ joysticks

    private fun stickState(id: String) = stickStates.getOrPut(id) { StickState() }

    private fun setStickActions(st: StickState, next: List<HudAction>) {
        val old = st.held
        old.filter { it !in next }.forEach(::up)
        next.filter { it !in old }.forEach(::down)
        st.held = next
    }

    private fun stickDown(s: HudStick, rect: Rect, pos: Offset, now: Long) {
        val st = stickState(s.id)
        if (st.locked) {
            st.locked = false
            s.lockActs.forEach(::up)
            setStickActions(st, emptyList())
        }
        if (s.doubleActs.isNotEmpty() && st.lastDown > 0 && now - st.lastDown < STICK_DOUBLE_MS &&
            hypot(pos.x - rect.center.x, pos.y - rect.center.y) < rect.width * s.style.knobRatio / 2f + rect.width * 0.1f
        ) {
            st.lastDown = 0
            if (s.doubleKeep) {
                st.sneakOn = !st.sneakOn
                if (st.sneakOn) s.doubleActs.forEach(::down) else s.doubleActs.forEach(::up)
            } else click(s.doubleActs)
        } else st.lastDown = now
        val knobR = rect.width * s.style.knobRatio / 2f
        st.center = when {
            s.mode == HudStickMode.DPAD -> rect.center
            s.follow == HudStickFollow.FOLLOW -> pos
            s.follow == HudStickFollow.CENTER && hypot(pos.x - rect.center.x, pos.y - rect.center.y) <= knobR -> pos
            else -> rect.center
        }
        stickMove(s, rect, pos)
    }

    private fun stickMove(s: HudStick, rect: Rect, pos: Offset) {
        val st = stickState(s.id)
        val radius = rect.width / 2f
        val dx = pos.x - st.center.x
        val dy = pos.y - st.center.y
        val dist = hypot(dx, dy)
        val ratio = if (radius > 0f) dist / radius else 0f
        val k = if (dist > radius && dist > 0f) radius / dist else 1f
        val dead = s.deadZone.coerceIn(0f, 0.9f)
        val dir = if (ratio < dead) -1 else {
            val deg = (Math.toDegrees(atan2(-dy, dx).toDouble()) + 360.0) % 360.0
            (((deg + 22.5) / 45.0).toInt()) % 8
        }
        val forward = dir == 1 || dir == 2 || dir == 3
        val run = if (s.autoRun && forward) s.lockActs.ifEmpty { listOf(HudAction.Key(341)) } else emptyList()
        setStickActions(st, if (dir < 0) emptyList() else (s.dirs.getOrNull(dir).orEmpty() + run).distinct())
        st.armed = s.canLock && dir == 2 && dist >= radius * (1f + s.lockThreshold.coerceIn(0f, 1f))
        stickVisual[s.id] = HudStickVisual(
            knob = Offset(dx * k / max(1f, radius), dy * k / max(1f, radius)),
            center = st.center,
            dir = dir, armed = st.armed, locked = false, active = true
        )
    }

    private fun stickUp(s: HudStick) {
        val st = stickState(s.id)
        if (st.armed) {
            st.armed = false
            st.locked = true
            setStickActions(st, s.dirs.getOrNull(2).orEmpty())
            s.lockActs.forEach(::down)
            stickVisual[s.id] = HudStickVisual(knob = Offset(0f, -1f), dir = 2, locked = true)
        } else {
            setStickActions(st, emptyList())
            stickVisual[s.id] = HudStickVisual()
        }
    }

    // ------------------------------------------------------------------ visibilidade

    fun isShown(layer: String, vis: HudVisibility, grabbing: Boolean): Boolean {
        if (layerVisible[layer] == false) return false
        return when (vis) {
            HudVisibility.ALWAYS -> true
            HudVisibility.IN_GAME -> grabbing
            HudVisibility.IN_MENU -> !grabbing
        }
    }

    /** Solta tudo (troca de tela, menu aberto, HUD escondido…). */
    fun releaseAll() {
        tracks.clear()
        buttonStates.values.forEach { s ->
            s.longJob?.cancel(); s.pressRepeat?.cancel(); s.longRepeat?.cancel()
        }
        buttonStates.clear()
        stickStates.clear()
        holdCount.keys.toList().forEach { send(it, false) }
        holdCount.clear()
        pressedIds.clear()
        stickVisual.clear()
    }

    // ------------------------------------------------------------------ toques

    private fun hitButton(geo: HudGeometry, pos: Offset, grabbing: Boolean, swipeOnly: Boolean, except: String? = null): HudButton? {
        for (i in layout.buttons.indices.reversed()) {
            val b = layout.buttons[i]
            if (!b.interactive || b.id == except) continue
            if (swipeOnly && !b.swipe) continue
            if (!isShown(b.layer, b.visibility, grabbing)) continue
            if (geo.buttonRect(b, movedTo[b.id]).contains(pos)) return b
        }
        return null
    }

    private fun hitStick(geo: HudGeometry, pos: Offset, grabbing: Boolean): HudStick? {
        for (i in layout.sticks.indices.reversed()) {
            val s = layout.sticks[i]
            if (!isShown(s.layer, s.visibility, grabbing)) continue
            if (geo.stickRect(s).contains(pos)) return s
        }
        return null
    }

    /**
     * @return true para consumir o toque; false para deixar a câmera usar (botões "olhar").
     */
    internal fun onDown(id: PointerId, pos: Offset, now: Long, geo: HudGeometry, grabbing: Boolean, markLook: () -> Unit): Boolean? {
        if (hidden) {
            // alça para mostrar o HUD de novo
            if (hiddenHandle(geo).contains(pos)) {
                hidden = false
                return true
            }
            return null
        }
        hitButton(geo, pos, grabbing, swipeOnly = false)?.let { b ->
            val rect = geo.buttonRect(b, movedTo[b.id])
            tracks[id] = Track.Button(b.id, b.id, pos, now, b.look).also { it.grab = pos - rect.topLeft }
            buttonDown(b, now)
            if (b.look) markLook()
            return !b.look
        }
        hitStick(geo, pos, grabbing)?.let { s ->
            tracks[id] = Track.Stick(s.id)
            stickDown(s, geo.stickRect(s), pos, now)
            return true
        }
        return null
    }

    internal fun onMove(id: PointerId, pos: Offset, geo: HudGeometry, grabbing: Boolean): Boolean {
        when (val t = tracks[id] ?: return false) {
            is Track.Stick -> {
                val s = sticksById[t.id] ?: return true
                stickMove(s, geo.stickRect(s), pos)
                return true
            }
            is Track.Button -> {
                val slop = TAP_SLOP_DP * geo.density
                if (!t.moved && hypot(pos.x - t.down.x, pos.y - t.down.y) > slop) t.moved = true
                val origin = buttonsById[t.origin] ?: return !t.look
                if (origin.movable && t.id == t.origin) {
                    val size = geo.buttonSize(origin)
                    val tl = pos - t.grab
                    movedTo[origin.id] = Offset(
                        tl.x.coerceIn(0f, max(0f, geo.width - size.width)),
                        tl.y.coerceIn(0f, max(0f, geo.height - size.height))
                    )
                } else if (origin.swipe) {
                    val current = buttonsById[t.id]
                    val inside = current != null && geo.buttonRect(current, movedTo[current.id]).contains(pos)
                    if (!inside) {
                        val next = hitButton(geo, pos, grabbing, swipeOnly = true, except = t.id)
                        if (next != null || current != null) {
                            if (current != null && current.id != next?.id) buttonSwipeExit(current)
                            if (next != null) {
                                buttonSwipeEnter(next)
                                t.id = next.id
                            } else t.id = ""
                            t.swiped = true
                        }
                    }
                }
                return !t.look
            }
        }
    }

    internal fun onUp(id: PointerId, now: Long) {
        when (val t = tracks.remove(id) ?: return) {
            is Track.Stick -> sticksById[t.id]?.let(::stickUp)
            is Track.Button -> {
                if (t.swiped) {
                    buttonsById[t.id]?.let(::buttonSwipeExit)
                    return
                }
                val b = buttonsById[t.id] ?: return
                val quick = !t.moved && now - t.time <= TAP_MS
                buttonUp(b, now, quick)
            }
        }
    }

    fun hiddenHandle(geo: HudGeometry): Rect {
        val s = 34f * geo.density
        return Rect(Offset((geo.width - s) / 2f, 4f * geo.density), Size(s, s))
    }
}

/**
 * Camada de HUD dos esquemas FCL / PojaRush.
 */
@Composable
fun HudRuntimeLayout(
    modifier: Modifier = Modifier,
    runtime: HudRuntime?,
    opacity: () -> Float,
    isCursorGrabbing: Boolean,
    enabled: Boolean,
    checkOccupiedPointers: (PointerId) -> Boolean,
    markPointerAsMoveOnly: (PointerId) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current.density
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val geo = remember(w, h, density) { HudGeometry(w, h, density) }
        val currentGrabbing by rememberUpdatedState(isCursorGrabbing)
        val currentEnabled by rememberUpdatedState(enabled)
        val currentCheck by rememberUpdatedState(checkOccupiedPointers)
        val currentMark by rememberUpdatedState(markPointerAsMoveOnly)

        DisposableEffect(runtime) {
            onDispose { runtime?.releaseAll() }
        }
        DisposableEffect(enabled) {
            if (!enabled) runtime?.releaseAll()
            onDispose { }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(runtime, geo) {
                    if (runtime == null) return@pointerInput
                    awaitPointerEventScope {
                        val owned = HashSet<PointerId>()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            for (change in event.changes) {
                                val id = change.id
                                if (!change.pressed) {
                                    if (owned.remove(id)) runtime.onUp(id, change.uptimeMillis)
                                    continue
                                }
                                if (!currentEnabled) continue
                                if (!change.previousPressed) {
                                    if (change.type != PointerType.Touch && change.type != PointerType.Stylus) continue
                                    if (change.isConsumed || currentCheck(id)) continue
                                    val consume = runtime.onDown(id, change.position, change.uptimeMillis, geo, currentGrabbing) { currentMark(id) }
                                    if (consume != null) {
                                        owned += id
                                        if (consume) change.consume()
                                    }
                                } else if (id in owned) {
                                    if (runtime.onMove(id, change.position, geo, currentGrabbing)) change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            content()
            if (runtime != null) HudCanvas(runtime, geo, opacity, isCursorGrabbing)
        }
    }
}

@Composable
private fun HudCanvas(runtime: HudRuntime, geo: HudGeometry, opacity: () -> Float, grabbing: Boolean) {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val layout = runtime.layout
    Canvas(modifier = Modifier.fillMaxSize()) {
        val alpha = opacity().coerceIn(0f, 1f)
        if (runtime.hidden) {
            val r = runtime.hiddenHandle(geo)
            drawCircle(androidx.compose.ui.graphics.Color(0x66101C2C), r.width / 2f, r.center)
            drawCircle(androidx.compose.ui.graphics.Color(0xCC7FD8FF), r.width / 2f, r.center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * geo.density))
            drawCircle(androidx.compose.ui.graphics.Color(0xCC7FD8FF), r.width / 7f, r.center)
            return@Canvas
        }
        for (s in layout.sticks) {
            if (!runtime.isShown(s.layer, s.visibility, grabbing)) continue
            drawHudStick(s, geo.stickRect(s), runtime.stickVisual[s.id] ?: HudStickVisual(), alpha, geo.density)
        }
        for (b in layout.buttons) {
            if (!runtime.isShown(b.layer, b.visibility, grabbing)) continue
            drawHudButton(b, geo.buttonRect(b, runtime.movedTo[b.id]), runtime.pressedIds[b.id] == true, alpha, geo.density, measurer)
        }
    }
}

/** Carrega o runtime do HUD escolhido no esquema atual (FCL / PojaRush). */
@Composable
fun rememberHudRuntime(layout: HudLayout?, send: (HudAction, Boolean) -> Unit): HudRuntime? {
    val scope = rememberCoroutineScope()
    val currentSend by rememberUpdatedState(send)
    return remember(layout) { layout?.let { HudRuntime(it, scope) { a, p -> currentSend(a, p) } } }
}
