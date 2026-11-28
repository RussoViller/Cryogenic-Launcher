package com.russoviller.cryogenic.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import kotlin.math.max
import kotlin.math.roundToInt

private val Ice = Color(0xFF7FD8FF)
private val Frost = Color(0xFFCFEFFF)
private val PanelBg = Color(0xEE0B1522)
private val Line = Color(0x557FD8FF)

private sealed interface HudSel {
    data class Btn(val id: String) : HudSel
    data class Stk(val id: String) : HudSel
}

private enum class HudPanel { NONE, PROPS, LAYERS, INFO }

private class HudEditorState(initial: HudLayout) {
    var layout by mutableStateOf(initial)
    var saved by mutableStateOf(initial)
    var selected by mutableStateOf<HudSel?>(null)
    var panel by mutableStateOf(HudPanel.NONE)
    var grid by mutableStateOf(true)
    var panelLeft by mutableStateOf(false)
    var activeLayer by mutableStateOf(initial.layers.firstOrNull()?.id ?: "")
    val hiddenInEditor = mutableStateListOf<String>()
    private val undo = ArrayDeque<HudLayout>()
    var canUndo by mutableStateOf(false)

    val dirty: Boolean get() = layout != saved

    fun push() {
        undo.addLast(layout)
        if (undo.size > 60) undo.removeFirst()
        canUndo = true
    }

    fun edit(push: Boolean = true, f: (HudLayout) -> HudLayout) {
        val next = f(layout)
        if (next == layout) return
        if (push) push()
        layout = next
    }

    fun undo() {
        undo.removeLastOrNull()?.let { layout = it }
        canUndo = undo.isNotEmpty()
        if (selected != null && selectedButton() == null && selectedStick() == null) selected = null
    }

    fun selectedButton(): HudButton? = (selected as? HudSel.Btn)?.let { s -> layout.buttons.firstOrNull { it.id == s.id } }
    fun selectedStick(): HudStick? = (selected as? HudSel.Stk)?.let { s -> layout.sticks.firstOrNull { it.id == s.id } }

    fun updateButton(id: String, push: Boolean = true, f: (HudButton) -> HudButton) =
        edit(push) { l -> l.copy(buttons = l.buttons.map { if (it.id == id) f(it) else it }) }

    fun updateStick(id: String, push: Boolean = true, f: (HudStick) -> HudStick) =
        edit(push) { l -> l.copy(sticks = l.sticks.map { if (it.id == id) f(it) else it }) }

    fun visibleInEditor(layer: String) = layer !in hiddenInEditor
}

private fun HudStick.withFclDirs(up: List<HudAction>, down: List<HudAction>, left: List<HudAction>, right: List<HudAction>) =
    copy(dirs = listOf(right, up + right, up, up + left, left, down + left, down, down + right))

/**
 * Editor próprio dos esquemas FCL e PojaRush (cada esquema com seus termos e opções).
 */
@Composable
fun HudEditor(
    initial: HudLayout,
    inGame: Boolean,
    onSave: (HudLayout) -> Boolean,
    onExit: () -> Unit,
    backSignal: Int = 0
) {
    val state = remember(initial) { HudEditorState(initial) }
    val fcl = initial.scheme == HudScheme.FCL
    var askExit by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<((HudAction) -> Unit)?>(null) }
    var pickerKeysOnly by remember { mutableStateOf(false) }
    var savedToast by remember { mutableStateOf(false) }

    fun save(): Boolean {
        val ok = onSave(state.layout)
        if (ok) state.saved = state.layout
        savedToast = ok
        return ok
    }

    fun back() {
        when {
            picker != null -> picker = null
            state.panel != HudPanel.NONE -> state.panel = HudPanel.NONE
            state.dirty -> askExit = true
            else -> onExit()
        }
    }
    if (!inGame) androidx.activity.compose.BackHandler { back() }
    androidx.compose.runtime.LaunchedEffect(backSignal) { if (backSignal > 0) back() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (inGame) Modifier.background(Color(0x55000000))
                else Modifier.background(Brush.verticalGradient(listOf(Color(0xFF0B1522), Color(0xFF050A12))))
            )
    ) {
        val density = LocalDensity.current.density
        val geo = remember(constraints.maxWidth, constraints.maxHeight, density) {
            HudGeometry(constraints.maxWidth, constraints.maxHeight, density)
        }
        HudEditCanvas(state, geo)

        // barra de ferramentas
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PanelBg)
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(painterResource(R.drawable.ic_add), stringResource(R.string.hude_add_button)) {
                val id = newHudId()
                state.edit { l ->
                    val style = l.buttons.lastOrNull()?.style ?: if (fcl) HudStyle(name = "Default") else PojarushCodec.defaultButtonStyle
                    l.copy(buttons = l.buttons + HudButton(id, "?", layer = state.activeLayer, style = style))
                }
                state.selected = HudSel.Btn(id)
                state.panel = HudPanel.PROPS
            }
            ToolText("◎", stringResource(if (fcl) R.string.hude_add_direction else R.string.hude_add_stick)) {
                val id = newHudId()
                state.edit { l ->
                    l.copy(sticks = l.sticks + HudStick(id, layer = state.activeLayer, x = 0.5f, y = 0.5f,
                        follow = if (fcl) HudStickFollow.CENTER else HudStickFollow.FIXED,
                        doubleActs = listOf(HudAction.Key(340))))
                }
                state.selected = HudSel.Stk(id)
                state.panel = HudPanel.PROPS
            }
            ToolButton(painterResource(R.drawable.ic_cryo_layers), stringResource(if (fcl) R.string.hude_groups else R.string.hude_layers),
                active = state.panel == HudPanel.LAYERS) {
                state.panel = if (state.panel == HudPanel.LAYERS) HudPanel.NONE else HudPanel.LAYERS
            }
            ToolButton(painterResource(R.drawable.ic_edit_outlined), stringResource(R.string.hude_properties),
                active = state.panel == HudPanel.PROPS, enabled = state.selected != null) {
                state.panel = if (state.panel == HudPanel.PROPS) HudPanel.NONE else HudPanel.PROPS
            }
            ToolButton(painterResource(R.drawable.ic_copy_all_outlined), stringResource(R.string.generic_copy), enabled = state.selected != null) {
                state.selectedButton()?.let { b ->
                    val id = newHudId()
                    state.edit { l -> l.copy(buttons = l.buttons + b.copy(id = id, raw = null, x = (b.x + 0.04f).coerceAtMost(1f), y = (b.y + 0.04f).coerceAtMost(1f))) }
                    state.selected = HudSel.Btn(id)
                }
                state.selectedStick()?.let { s ->
                    val id = newHudId()
                    state.edit { l -> l.copy(sticks = l.sticks + s.copy(id = id, raw = null, x = (s.x + 0.05f).coerceAtMost(1f))) }
                    state.selected = HudSel.Stk(id)
                }
            }
            ToolButton(painterResource(R.drawable.ic_delete_outlined), stringResource(R.string.generic_delete), enabled = state.selected != null) {
                when (val sel = state.selected) {
                    is HudSel.Btn -> state.edit { l -> l.copy(buttons = l.buttons.filterNot { it.id == sel.id }) }
                    is HudSel.Stk -> state.edit { l -> l.copy(sticks = l.sticks.filterNot { it.id == sel.id }) }
                    null -> {}
                }
                state.selected = null
                if (state.panel == HudPanel.PROPS) state.panel = HudPanel.NONE
            }
            ToolButton(painterResource(R.drawable.ic_undo), stringResource(R.string.hude_undo), enabled = state.canUndo) { state.undo() }
            ToolText("#", stringResource(R.string.hude_grid), active = state.grid) { state.grid = !state.grid }
            ToolButton(painterResource(R.drawable.ic_info_outlined), stringResource(R.string.hude_info), active = state.panel == HudPanel.INFO) {
                state.panel = if (state.panel == HudPanel.INFO) HudPanel.NONE else HudPanel.INFO
            }
            ToolButton(painterResource(R.drawable.ic_cryo_swap), stringResource(R.string.hude_panel_side)) { state.panelLeft = !state.panelLeft }
            ToolButton(painterResource(R.drawable.ic_save_filled), stringResource(R.string.generic_save), active = state.dirty) { save() }
            ToolButton(painterResource(R.drawable.ic_close), stringResource(R.string.generic_close)) {
                if (state.dirty) askExit = true else onExit()
            }
        }

        // título do esquema
        Text(
            text = (if (fcl) "FCL" else "PojaRush") + " · " + state.layout.name + if (state.dirty) " •" else "",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                .clip(RoundedCornerShape(10.dp)).background(PanelBg).padding(horizontal = 10.dp, vertical = 3.dp),
            color = Frost, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        if (state.panel != HudPanel.NONE) {
            Column(
                modifier = Modifier
                    .align(if (state.panelLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(top = 52.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                    .width(330.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelBg)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val openPicker: (Boolean, (HudAction) -> Unit) -> Unit = { keysOnly, cb -> pickerKeysOnly = keysOnly; picker = cb }
                when (state.panel) {
                    HudPanel.PROPS -> {
                        val b = state.selectedButton()
                        val s = state.selectedStick()
                        when {
                            b != null -> ButtonProps(state, b, fcl, geo, openPicker)
                            s != null -> StickProps(state, s, fcl, openPicker)
                            else -> Text(stringResource(R.string.hude_select_hint), color = Frost, fontSize = 13.sp)
                        }
                    }
                    HudPanel.LAYERS -> LayersPanel(state, fcl)
                    HudPanel.INFO -> InfoPanel(state)
                    HudPanel.NONE -> {}
                }
            }
        }
    }

    picker?.let { cb ->
        ActionPickerDialog(
            fcl = fcl,
            keysOnly = pickerKeysOnly,
            layers = state.layout.layers,
            onPick = { a -> cb(a); picker = null },
            onDismiss = { picker = null }
        )
    }

    if (askExit) {
        Dialog(onDismissRequest = { askExit = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(PanelBg).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.hude_unsaved_title), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(stringResource(R.string.hude_unsaved_text), color = Frost, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { askExit = false }) { Text(stringResource(R.string.generic_cancel), color = Frost) }
                    TextButton(onClick = { askExit = false; onExit() }) { Text(stringResource(R.string.hude_discard), color = Color(0xFFFF8A80)) }
                    TextButton(onClick = { askExit = false; if (save()) onExit() }) { Text(stringResource(R.string.generic_save), color = Ice) }
                }
            }
        }
    }

    if (savedToast) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1400)
            savedToast = false
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                stringResource(R.string.generic_saved),
                modifier = Modifier.padding(bottom = 34.dp).clip(RoundedCornerShape(12.dp)).background(Ice).padding(horizontal = 14.dp, vertical = 6.dp),
                color = Color(0xFF05101C), fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
    }
}

// ---------------------------------------------------------------------- canvas

@Composable
private fun HudEditCanvas(state: HudEditorState, geo: HudGeometry) {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val currentState by rememberUpdatedState(state)
    val handleR = 14f * geo.density
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(geo) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val st = currentState
                    val l = st.layout
                    val pos = down.position
                    // 1) alça de redimensionar do selecionado
                    var mode = 0 // 0 nada, 1 mover, 2 redimensionar
                    var targetBtn: HudButton? = null
                    var targetStk: HudStick? = null
                    val selB = st.selectedButton()
                    val selS = st.selectedStick()
                    if (selB != null) {
                        val r = geo.buttonRect(selB)
                        if ((pos - r.bottomRight).getDistance() <= handleR * 1.6f) { mode = 2; targetBtn = selB }
                    } else if (selS != null) {
                        val r = geo.stickRect(selS)
                        if ((pos - r.bottomRight).getDistance() <= handleR * 1.6f) { mode = 2; targetStk = selS }
                    }
                    if (mode == 0) {
                        targetBtn = l.buttons.lastOrNull { st.visibleInEditor(it.layer) && geo.buttonRect(it).contains(pos) }
                        if (targetBtn == null) targetStk = l.sticks.lastOrNull { st.visibleInEditor(it.layer) && geo.stickRect(it).contains(pos) }
                        mode = if (targetBtn != null || targetStk != null) 1 else 0
                        st.selected = targetBtn?.let { HudSel.Btn(it.id) } ?: targetStk?.let { HudSel.Stk(it.id) }
                    }
                    if (mode == 0) return@awaitEachGesture
                    down.consume()
                    val startRect = targetBtn?.let { geo.buttonRect(it) } ?: geo.stickRect(targetStk!!)
                    var total = Offset.Zero
                    var pushed = false
                    val step = 5f * geo.density
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        total += ch.positionChange()
                        ch.consume()
                        if (!pushed && total.getDistance() < 4f * geo.density) continue
                        if (!pushed) { st.push(); pushed = true }
                        fun snap(v: Float) = if (st.grid) (v / step).roundToInt() * step else v
                        if (mode == 1) {
                            val tl = Offset(snap(startRect.left + total.x), snap(startRect.top + total.y))
                            val f = geo.fractionOf(tl, startRect.size)
                            targetBtn?.let { b -> st.updateButton(b.id, push = false) { it.copy(x = f.x, y = f.y) } }
                            targetStk?.let { s -> st.updateStick(s.id, push = false) { it.copy(x = f.x, y = f.y) } }
                        } else {
                            val minPx = 16f * geo.density
                            targetBtn?.let { b ->
                                val w = max(minPx, snap(startRect.width + total.x))
                                val h = max(minPx, snap(startRect.height + total.y))
                                val f = geo.fractionOf(startRect.topLeft, Size(w, h))
                                st.updateButton(b.id, push = false) {
                                    it.copy(
                                        width = HudSize.ofPx(w, it.width, geo.width, geo.height, geo.density),
                                        height = HudSize.ofPx(h, it.height, geo.width, geo.height, geo.density),
                                        x = f.x, y = f.y
                                    )
                                }
                            }
                            targetStk?.let { s ->
                                val d = max(total.x, total.y)
                                val size = max(minPx * 2, snap(startRect.width + d))
                                val f = geo.fractionOf(startRect.topLeft, Size(size, size))
                                st.updateStick(s.id, push = false) {
                                    it.copy(size = HudSize.ofPx(size, it.size, geo.width, geo.height, geo.density), x = f.x, y = f.y)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val st = currentState
        val l = st.layout
        if (st.grid) {
            val gs = 40f * geo.density
            var x = 0f
            while (x < size.width) { drawLine(Color(0x147FD8FF), Offset(x, 0f), Offset(x, size.height)); x += gs }
            var y = 0f
            while (y < size.height) { drawLine(Color(0x147FD8FF), Offset(0f, y), Offset(size.width, y)); y += gs }
            drawLine(Color(0x337FD8FF), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
            drawLine(Color(0x337FD8FF), Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
        }
        for (s in l.sticks) {
            if (!st.visibleInEditor(s.layer)) continue
            val dim = if (l.layers.firstOrNull { it.id == s.layer }?.visible == false) 0.5f else 1f
            drawHudStick(s, geo.stickRect(s), HudStickVisual(), 0.9f * dim, geo.density)
        }
        for (b in l.buttons) {
            if (!st.visibleInEditor(b.layer)) continue
            val dim = if (l.layers.firstOrNull { it.id == b.layer }?.visible == false) 0.5f else 1f
            drawHudButton(b, geo.buttonRect(b), false, 0.9f * dim, geo.density, measurer)
        }
        val selRect: Rect? = st.selectedButton()?.let { geo.buttonRect(it) } ?: st.selectedStick()?.let { geo.stickRect(it) }
        if (selRect != null) {
            drawRect(
                Ice, selRect.topLeft - Offset(3f, 3f), Size(selRect.width + 6f, selRect.height + 6f),
                style = Stroke(2f * geo.density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )
            drawCircle(Ice, handleR * 0.7f, selRect.bottomRight)
            drawCircle(Color(0xFF05101C), handleR * 0.35f, selRect.bottomRight)
        }
    }
}

// ---------------------------------------------------------------------- barra

@Composable
private fun ToolButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Ice.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp)
            .widthIn(min = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painter, contentDescription = label, modifier = Modifier.size(18.dp), tint = if (enabled) Ice else Ice.copy(alpha = 0.3f))
        Text(label, fontSize = 9.sp, color = if (enabled) Frost else Frost.copy(alpha = 0.3f), maxLines = 1)
    }
}

@Composable
private fun ToolText(glyph: String, label: String, active: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Ice.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp)
            .widthIn(min = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(glyph, fontSize = 15.sp, color = Ice, fontWeight = FontWeight.Black, modifier = Modifier.height(18.dp))
        Text(label, fontSize = 9.sp, color = Frost, maxLines = 1)
    }
}

// ---------------------------------------------------------------------- componentes do painel

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(), color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun TextRow(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, fontSize = 11.sp) },
        singleLine = singleLine, modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
    )
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, format: (Float) -> String, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Frost, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(format(value), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.height(30.dp),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Ice, activeTrackColor = Ice, inactiveTrackColor = Color(0x337FD8FF),
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, hint: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onChange(!value) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 13.sp)
            if (hint != null) Text(hint, color = Frost.copy(alpha = 0.6f), fontSize = 10.sp)
        }
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF05101C), checkedTrackColor = Ice,
                uncheckedThumbColor = Frost.copy(alpha = 0.7f), uncheckedTrackColor = Color(0x22FFFFFF), uncheckedBorderColor = Line
            )
        )
    }
}

@Composable
private fun <T> ChoiceRow(label: String?, options: List<Pair<T, String>>, value: T, onChange: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) Text(label, color = Frost, fontSize = 12.sp)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x22FFFFFF)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEach { (v, text) ->
                val sel = v == value
                Text(
                    text, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (sel) Color(0xFF05101C) else Frost, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (sel) Ice else Color.Transparent)
                        .clickable { onChange(v) }.padding(vertical = 6.dp, horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun actionLabel(a: HudAction, layers: List<HudLayer>, fcl: Boolean): String = when (a) {
    is HudAction.Key -> HudKeys.label(a.glfw)
    is HudAction.Mouse -> HudKeys.mouseLabel(a.button)
    is HudAction.Scroll -> (if (a.up) "⇞ " else "⇟ ") + stringResource(R.string.hude_act_scroll) + if (a.repeat) " ∞" else ""
    HudAction.Keyboard -> "⌨ " + stringResource(R.string.hude_act_keyboard)
    HudAction.Menu -> "☰ " + stringResource(R.string.hude_act_menu)
    HudAction.MouseMode -> "🖱 " + stringResource(if (fcl) R.string.hude_act_mouse_mode else R.string.hude_act_virtual_mouse)
    HudAction.HideHud -> "◌ " + stringResource(R.string.hude_act_hide_hud)
    is HudAction.Text -> "“" + a.text.take(14) + "”"
    is HudAction.Layer -> (when (a.mode) { 1 -> "＋"; 2 -> "－"; else -> "⇄" }) + " " + a.ids.joinToString { id -> layers.firstOrNull { it.id == id }?.name ?: id }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsRow(
    label: String,
    acts: List<HudAction>,
    layers: List<HudLayer>,
    fcl: Boolean,
    keysOnly: Boolean = false,
    openPicker: (Boolean, (HudAction) -> Unit) -> Unit,
    onChange: (List<HudAction>) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = Frost, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            acts.forEachIndexed { i, a ->
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Ice.copy(alpha = 0.18f))
                        .clickable { onChange(acts.filterIndexed { j, _ -> j != i }) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(actionLabel(a, layers, fcl), color = Color.White, fontSize = 12.sp)
                    Text("  ✕", color = Frost.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
            Text(
                "＋", color = Color(0xFF05101C), fontWeight = FontWeight.Black, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Ice)
                    .clickable { openPicker(keysOnly) { a -> if (a !in acts) onChange(acts + a) } }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private val palette = listOf(
    0xFFFFFFFF, 0xFFCFD8DC, 0xFF888888, 0xFF444444, 0xFF000000, 0xFF7FD8FF, 0xFF2196F3, 0xFF3F51B5,
    0xFF9C27B0, 0xFFE91E63, 0xFFF44336, 0xFFFF9800, 0xFFFFEB3B, 0xFF8BC34A, 0xFF4CAF50, 0xFF009688
).map { it.toInt() }

@Composable
private fun ColorRow(label: String, value: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(String.format("#%08X", value), color = Frost.copy(alpha = 0.6f), fontSize = 10.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF666666), Color(0xFFBBBBBB))))
                .background(Color(value)).border(1.dp, Line, RoundedCornerShape(7.dp))
        )
    }
    if (open) ColorDialog(value, onDismiss = { open = false }) { onChange(it); open = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var rgb by remember { mutableStateOf(initial or 0xFF000000.toInt()) }
    var alpha by remember { mutableStateOf(((initial ushr 24) and 0xFF) / 255f) }
    var hex by remember { mutableStateOf(String.format("%08X", initial)) }
    fun current() = ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.widthIn(max = 360.dp).clip(RoundedCornerShape(20.dp)).background(PanelBg).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF555555), Color(0xFFAAAAAA)))).background(Color(current())))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                palette.forEach { c ->
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                        .border(if ((rgb and 0xFFFFFF) == (c and 0xFFFFFF)) 3.dp else 1.dp, if ((rgb and 0xFFFFFF) == (c and 0xFFFFFF)) Ice else Line, CircleShape)
                        .clickable { rgb = c; hex = String.format("%08X", current()) })
                }
            }
            SliderRow(stringResource(R.string.hude_opacity), alpha, 0f..1f, { "${(it * 100).roundToInt()}%" }) {
                alpha = it; hex = String.format("%08X", current())
            }
            OutlinedTextField(
                value = hex, singleLine = true, label = { Text("ARGB") },
                onValueChange = { t ->
                    hex = t.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(8)
                    hex.toLongOrNull(16)?.let { v ->
                        val full = if (hex.length <= 6) (0xFF000000L or v) else v
                        rgb = (full.toInt() or 0xFF000000.toInt()); alpha = ((full ushr 24) and 0xFF) / 255f
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel), color = Frost) }
                TextButton(onClick = { onPick(current()) }) { Text(stringResource(R.string.generic_confirm), color = Ice) }
            }
        }
    }
}

// ---------------------------------------------------------------------- painéis

@Composable
private fun visibilityOptions() = listOf(
    HudVisibility.ALWAYS to stringResource(R.string.hude_vis_always),
    HudVisibility.IN_GAME to stringResource(R.string.hude_vis_game),
    HudVisibility.IN_MENU to stringResource(R.string.hude_vis_menu)
)

@Composable
private fun LayerChoice(state: HudEditorState, fcl: Boolean, value: String, onChange: (String) -> Unit) {
    val layers = state.layout.layers
    if (layers.isEmpty()) return
    ChoiceRow(
        stringResource(if (fcl) R.string.hude_group else R.string.hude_layer),
        layers.map { it.id to it.name }, value.takeIf { v -> layers.any { it.id == v } } ?: layers.first().id, onChange
    )
}

@Composable
private fun SizeRows(label: String, size: HudSize, geo: HudGeometry, fcl: Boolean, onChange: (HudSize) -> Unit) {
    ChoiceRow(
        label,
        listOf(true to stringResource(if (fcl) R.string.hude_size_absolute else R.string.hude_size_dp), false to stringResource(R.string.hude_size_percent)),
        size.dp != null
    ) { dp ->
        val px = size.px(geo.width, geo.height, geo.density)
        onChange(if (dp) size.copy(dp = (px / geo.density).roundToInt().toFloat()) else size.copy(dp = null, fraction = (px / (if (size.ofWidth) geo.width else geo.height)).coerceIn(0.01f, 1f)))
    }
    if (size.dp != null) {
        SliderRow(label + " (dp)", size.dp, 10f..400f, { "${it.roundToInt()} dp" }) { onChange(size.copy(dp = it.roundToInt().toFloat())) }
    } else {
        SliderRow(label + " (%)", size.fraction * 100f, 1f..100f, { String.format("%.1f%%", it) }) {
            onChange(size.copy(fraction = (it * 10).roundToInt() / 1000f))
        }
        ChoiceRow(null, listOf(false to stringResource(R.string.hude_ref_height), true to stringResource(R.string.hude_ref_width)), size.ofWidth) {
            onChange(size.copy(ofWidth = it))
        }
    }
}

@Composable
private fun TriggerBox(
    title: String,
    t: HudTrigger,
    state: HudEditorState,
    fcl: Boolean,
    showKeep: Boolean,
    showRepeat: Boolean,
    openPicker: (Boolean, (HudAction) -> Unit) -> Unit,
    onChange: (HudTrigger) -> Unit
) {
    ActionsRow(title, t.acts, state.layout.layers, fcl, openPicker = openPicker) { onChange(t.copy(acts = it)) }
    if (showKeep) SwitchRow(stringResource(if (fcl) R.string.hude_fcl_auto_keep else R.string.hude_keep), t.keep,
        stringResource(R.string.hude_keep_hint)) { onChange(t.copy(keep = it)) }
    if (showRepeat) SwitchRow(stringResource(if (fcl) R.string.hude_fcl_auto_click else R.string.hude_spam), t.repeat,
        stringResource(R.string.hude_repeat_hint)) { onChange(t.copy(repeat = it)) }
}

@Composable
private fun ColumnScope.ButtonProps(
    state: HudEditorState,
    b: HudButton,
    fcl: Boolean,
    geo: HudGeometry,
    openPicker: (Boolean, (HudAction) -> Unit) -> Unit
) {
    fun upd(push: Boolean = true, f: (HudButton) -> HudButton) = state.updateButton(b.id, push, f)
    Text(stringResource(R.string.hude_button), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    TextRow(stringResource(R.string.hude_text), b.text) { v -> upd(false) { it.copy(text = v) } }
    LayerChoice(state, fcl, b.layer) { v -> upd { it.copy(layer = v) } }
    ChoiceRow(stringResource(R.string.hude_visibility), visibilityOptions(), b.visibility) { v -> upd { it.copy(visibility = v) } }

    Section(stringResource(R.string.hude_size_position))
    SizeRows(stringResource(R.string.hude_width), b.width, geo, fcl) { v -> upd { it.copy(width = v) } }
    SizeRows(stringResource(R.string.hude_height), b.height, geo, fcl) { v -> upd { it.copy(height = v) } }
    SliderRow("X", b.x * 100f, 0f..100f, { String.format("%.1f%%", it) }) { v -> upd(false) { it.copy(x = (v * 10).roundToInt() / 1000f) } }
    SliderRow("Y", b.y * 100f, 0f..100f, { String.format("%.1f%%", it) }) { v -> upd(false) { it.copy(y = (v * 10).roundToInt() / 1000f) } }

    if (fcl) {
        Section(stringResource(R.string.hude_events))
        TriggerBox(stringResource(R.string.hude_fcl_press), b.press, state, true, true, true, openPicker) { v -> upd { it.copy(press = v) } }
        TriggerBox(stringResource(R.string.hude_fcl_long), b.long, state, true, true, true, openPicker) { v -> upd { it.copy(long = v) } }
        TriggerBox(stringResource(R.string.hude_fcl_click), b.tap, state, true, true, false, openPicker) { v -> upd { it.copy(tap = v) } }
        TriggerBox(stringResource(R.string.hude_fcl_double), b.double, state, true, true, false, openPicker) { v -> upd { it.copy(double = v) } }
        Section(stringResource(R.string.hude_options))
        SwitchRow(stringResource(R.string.hude_fcl_pointer_follow), b.look, stringResource(R.string.hude_look_hint)) { v -> upd { it.copy(look = v) } }
        SwitchRow(stringResource(R.string.hude_fcl_movable), b.movable, stringResource(R.string.hude_movable_hint)) { v -> upd { it.copy(movable = v) } }
        SwitchRow(stringResource(R.string.hude_swipe), b.swipe, stringResource(R.string.hude_swipe_hint)) { v -> upd { it.copy(swipe = v) } }
    } else {
        Section(stringResource(R.string.hude_actions))
        TriggerBox(stringResource(R.string.hude_pr_press), b.press, state, false, false, true, openPicker) { v -> upd { it.copy(press = v) } }
        if (b.press.repeat) SliderRow(stringResource(R.string.hude_spam_cps), b.repeatCps, 1f..40f, { "${it.roundToInt()}/s" }) { v ->
            upd(false) { it.copy(repeatCps = v.roundToInt().toFloat()) }
        }
        ActionsRow(stringResource(R.string.hude_pr_tap), b.tap.acts, state.layout.layers, false, openPicker = openPicker) { v -> upd { it.copy(tap = it.tap.copy(acts = v)) } }
        ActionsRow(stringResource(R.string.hude_pr_toggle), b.toggle.acts, state.layout.layers, false, openPicker = openPicker) { v ->
            upd { it.copy(toggle = HudTrigger(v, keep = true)) }
        }
        Section(stringResource(R.string.hude_options))
        SwitchRow(stringResource(R.string.hude_swipe), b.swipe, stringResource(R.string.hude_swipe_hint)) { v -> upd { it.copy(swipe = v) } }
        SwitchRow(stringResource(R.string.hude_pr_penetrable), b.look, stringResource(R.string.hude_look_hint)) { v -> upd { it.copy(look = v) } }
        SwitchRow(stringResource(R.string.hude_pr_interactive), b.interactive, stringResource(R.string.hude_interactive_hint)) { v -> upd { it.copy(interactive = v) } }
    }

    Section(stringResource(R.string.hude_style))
    if (fcl) TextRow(stringResource(R.string.hude_style_name), b.style.name) { v -> upd(false) { it.copy(style = it.style.copy(name = v)) } }
    StyleRows(b.style, fcl, stick = false) { v -> upd { it.copy(style = v) } }
}

@Composable
private fun StyleRows(st: HudStyle, fcl: Boolean, stick: Boolean, onChange: (HudStyle) -> Unit) {
    ColorRow(stringResource(R.string.hude_color_fill), st.fill) { onChange(st.copy(fill = it)) }
    ColorRow(stringResource(R.string.hude_color_fill_pressed), st.fillPressed) { onChange(st.copy(fillPressed = it)) }
    ColorRow(stringResource(R.string.hude_color_stroke), st.stroke) {
        onChange(if (fcl) st.copy(stroke = it) else st.copy(stroke = it, strokePressed = it))
    }
    if (fcl && !stick) ColorRow(stringResource(R.string.hude_color_stroke_pressed), st.strokePressed) { onChange(st.copy(strokePressed = it)) }
    if (!stick) {
        ColorRow(stringResource(R.string.hude_color_text), st.text) { onChange(st.copy(text = it)) }
        ColorRow(stringResource(R.string.hude_color_text_pressed), st.textPressed) { onChange(st.copy(textPressed = it)) }
        SliderRow(stringResource(R.string.hude_text_size), st.textSp, 6f..40f, { "${it.roundToInt()} sp" }) { onChange(st.copy(textSp = it.roundToInt().toFloat())) }
    }
    SliderRow(stringResource(R.string.hude_stroke_width), st.strokeDp, 0f..10f, { String.format("%.1f dp", it) }) {
        onChange(st.copy(strokeDp = (it * 10).roundToInt() / 10f))
    }
    if (!stick) {
        SliderRow(stringResource(R.string.hude_corner_radius), st.radiusDp.coerceAtMost(100f), 0f..100f, { if (it >= 100f) "○" else "${it.roundToInt()} dp" }) {
            onChange(st.copy(radiusDp = if (it >= 99.5f) 500f else it.roundToInt().toFloat()))
        }
    }
    if (!fcl) SliderRow(stringResource(R.string.hude_opacity), st.opacity, 0f..1f, { "${(it * 100).roundToInt()}%" }) {
        onChange(st.copy(opacity = (it * 100).roundToInt() / 100f))
    }
}

@Composable
private fun ColumnScope.StickProps(
    state: HudEditorState,
    s: HudStick,
    fcl: Boolean,
    openPicker: (Boolean, (HudAction) -> Unit) -> Unit
) {
    fun upd(push: Boolean = true, f: (HudStick) -> HudStick) = state.updateStick(s.id, push, f)
    val layers = state.layout.layers
    Text(stringResource(if (fcl) R.string.hude_direction else R.string.hude_stick), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    LayerChoice(state, fcl, s.layer) { v -> upd { it.copy(layer = v) } }
    ChoiceRow(stringResource(R.string.hude_visibility), visibilityOptions(), s.visibility) { v -> upd { it.copy(visibility = v) } }
    Section(stringResource(R.string.hude_size_position))
    SliderRow(stringResource(R.string.hude_size) + " (%)", (if (s.size.dp == null) s.size.fraction else 0.45f) * 100f, 10f..100f, { String.format("%.1f%%", it) }) { v ->
        upd(false) { it.copy(size = HudSize(null, (v * 10).roundToInt() / 1000f, it.size.ofWidth)) }
    }
    SliderRow("X", s.x * 100f, 0f..100f, { String.format("%.1f%%", it) }) { v -> upd(false) { it.copy(x = (v * 10).roundToInt() / 1000f) } }
    SliderRow("Y", s.y * 100f, 0f..100f, { String.format("%.1f%%", it) }) { v -> upd(false) { it.copy(y = (v * 10).roundToInt() / 1000f) } }

    if (fcl) {
        ChoiceRow(stringResource(R.string.hude_fcl_style_type),
            listOf(HudStickMode.ROCKER to stringResource(R.string.hude_fcl_rocker), HudStickMode.DPAD to stringResource(R.string.hude_fcl_buttons)), s.mode
        ) { v -> upd { it.copy(mode = v) } }
        Section(stringResource(R.string.hude_keys))
        val up = s.dirs[2]; val down = s.dirs[6]; val left = s.dirs[4]; val right = s.dirs[0]
        ActionsRow(stringResource(R.string.hude_dir_up), up, layers, true, keysOnly = true, openPicker = openPicker) { v -> upd { it.withFclDirs(v, down, left, right) } }
        ActionsRow(stringResource(R.string.hude_dir_down), down, layers, true, keysOnly = true, openPicker = openPicker) { v -> upd { it.withFclDirs(up, v, left, right) } }
        ActionsRow(stringResource(R.string.hude_dir_left), left, layers, true, keysOnly = true, openPicker = openPicker) { v -> upd { it.withFclDirs(up, down, v, right) } }
        ActionsRow(stringResource(R.string.hude_dir_right), right, layers, true, keysOnly = true, openPicker = openPicker) { v -> upd { it.withFclDirs(up, down, left, v) } }
        Section(stringResource(R.string.hude_options))
        ChoiceRow(stringResource(R.string.hude_fcl_follow),
            listOf(
                HudStickFollow.FIXED to stringResource(R.string.hude_fcl_follow_fixed),
                HudStickFollow.CENTER to stringResource(R.string.hude_fcl_follow_center),
                HudStickFollow.FOLLOW to stringResource(R.string.hude_fcl_follow_free)
            ), s.follow
        ) { v -> upd { it.copy(follow = v) } }
        SwitchRow(stringResource(R.string.hude_fcl_sneak), s.doubleActs.isNotEmpty(), stringResource(R.string.hude_fcl_sneak_hint)) { v ->
            upd { it.copy(doubleActs = if (v) listOf(HudAction.Key(340)) else emptyList(), doubleKeep = true) }
        }
        if (s.doubleActs.isNotEmpty()) ActionsRow(stringResource(R.string.hude_fcl_sneak_key), s.doubleActs.take(1), layers, true, keysOnly = true,
            openPicker = { k, cb -> openPicker(k) { a -> cb(a) } }) { v -> upd { it.copy(doubleActs = v.takeLast(1)) } }
    } else {
        Section(stringResource(R.string.hude_directions))
        val names = listOf(R.string.hude_dir_e, R.string.hude_dir_ne, R.string.hude_dir_n, R.string.hude_dir_nw,
            R.string.hude_dir_w, R.string.hude_dir_sw, R.string.hude_dir_s, R.string.hude_dir_se)
        names.forEachIndexed { i, res ->
            ActionsRow(stringResource(res), s.dirs[i], layers, false, openPicker = openPicker) { v ->
                upd { it.copy(dirs = it.dirs.mapIndexed { j, d -> if (j == i) v else d }) }
            }
        }
        Section(stringResource(R.string.hude_options))
        ActionsRow(stringResource(R.string.hude_pr_double_tap), s.doubleActs, layers, false, openPicker = openPicker) { v -> upd { it.copy(doubleActs = v, doubleKeep = true) } }
        SwitchRow(stringResource(R.string.hude_pr_auto_run), s.autoRun, stringResource(R.string.hude_pr_auto_run_hint)) { v -> upd { it.copy(autoRun = v) } }
    }
    SliderRow(stringResource(R.string.hude_dead_zone), s.deadZone * 100f, 0f..90f, { "${it.roundToInt()}%" }) { v -> upd(false) { it.copy(deadZone = v.roundToInt() / 100f) } }
    SwitchRow(stringResource(R.string.hude_can_lock), s.canLock, stringResource(R.string.hude_can_lock_hint)) { v -> upd { it.copy(canLock = v) } }
    if (s.canLock) {
        SliderRow(stringResource(R.string.hude_lock_threshold), s.lockThreshold * 100f, 0f..100f, { "${it.roundToInt()}%" }) { v ->
            upd(false) { it.copy(lockThreshold = v.roundToInt() / 100f) }
        }
        if (!fcl) ActionsRow(stringResource(R.string.hude_pr_lock_actions), s.lockActs, layers, false, openPicker = openPicker) { v -> upd { it.copy(lockActs = v) } }
    }
    Section(stringResource(R.string.hude_style))
    if (fcl) TextRow(stringResource(R.string.hude_style_name), s.style.name) { v -> upd(false) { it.copy(style = it.style.copy(name = v)) } }
    StyleRows(s.style, fcl, stick = true) { v -> upd { it.copy(style = v) } }
    if (s.mode == HudStickMode.ROCKER) {
        ColorRow(stringResource(R.string.hude_color_knob), s.style.knobFill) { v -> upd { it.copy(style = it.style.copy(knobFill = v)) } }
        ColorRow(stringResource(R.string.hude_color_knob_stroke), s.style.knobStroke) { v -> upd { it.copy(style = it.style.copy(knobStroke = v)) } }
        SliderRow(stringResource(R.string.hude_knob_size), s.style.knobRatio * 100f, 10f..90f, { "${it.roundToInt()}%" }) { v ->
            upd(false) { it.copy(style = it.style.copy(knobRatio = v.roundToInt() / 100f)) }
        }
        if (fcl) SliderRow(stringResource(R.string.hude_corner_radius), s.style.radiusRatio * 200f, 0f..100f, { "${it.roundToInt()}%" }) { v ->
            upd(false) { it.copy(style = it.style.copy(radiusRatio = v.roundToInt() / 200f, knobRadiusRatio = v.roundToInt() / 200f)) }
        }
    } else {
        ColorRow(stringResource(R.string.hude_color_text), s.style.text) { v -> upd { it.copy(style = it.style.copy(text = v, textPressed = v)) } }
        SliderRow(stringResource(R.string.hude_corner_radius), s.style.radiusDp.coerceAtMost(100f), 0f..100f, { "${it.roundToInt()} dp" }) { v ->
            upd(false) { it.copy(style = it.style.copy(radiusDp = v.roundToInt().toFloat())) }
        }
        SliderRow(stringResource(R.string.hude_dpad_gap), s.style.gapRatio * 100f, 0f..30f, { "${it.roundToInt()}%" }) { v ->
            upd(false) { it.copy(style = it.style.copy(gapRatio = v.roundToInt() / 100f)) }
        }
    }
}

@Composable
private fun ColumnScope.LayersPanel(state: HudEditorState, fcl: Boolean) {
    var renaming by remember { mutableStateOf<HudLayer?>(null) }
    Text(stringResource(if (fcl) R.string.hude_groups else R.string.hude_layers), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    Text(stringResource(R.string.hude_layers_hint), color = Frost.copy(alpha = 0.7f), fontSize = 11.sp)
    state.layout.layers.forEach { layer ->
        val active = layer.id == state.activeLayer
        val count = state.layout.buttons.count { it.layer == layer.id } + state.layout.sticks.count { it.layer == layer.id }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (active) Ice.copy(alpha = 0.16f) else Color(0x14FFFFFF))
                .border(1.dp, if (active) Ice else Color.Transparent, RoundedCornerShape(12.dp))
                .clickable { state.activeLayer = layer.id }
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(layer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.hude_layer_count, count), color = Frost.copy(alpha = 0.6f), fontSize = 10.sp)
                }
                val eye = state.visibleInEditor(layer.id)
                Icon(
                    painterResource(if (eye) R.drawable.ic_visibility_outlined else R.drawable.ic_visibility_off_outlined), null,
                    tint = Ice, modifier = Modifier.size(30.dp).clip(CircleShape).clickable {
                        if (eye) state.hiddenInEditor.add(layer.id) else state.hiddenInEditor.remove(layer.id)
                    }.padding(5.dp)
                )
                Icon(painterResource(R.drawable.ic_edit_outlined), null, tint = Frost,
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable { renaming = layer }.padding(6.dp))
                if (state.layout.layers.size > 1) Icon(painterResource(R.drawable.ic_delete_outlined), null, tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable {
                        val fallback = state.layout.layers.first { it.id != layer.id }.id
                        state.edit { l ->
                            l.copy(
                                layers = l.layers.filterNot { it.id == layer.id },
                                buttons = l.buttons.map { if (it.layer == layer.id) it.copy(layer = fallback) else it },
                                sticks = l.sticks.map { if (it.layer == layer.id) it.copy(layer = fallback) else it }
                            )
                        }
                        if (state.activeLayer == layer.id) state.activeLayer = fallback
                    }.padding(6.dp))
            }
            SwitchRow(stringResource(R.string.hude_layer_visible), layer.visible) { v ->
                state.edit { l -> l.copy(layers = l.layers.map { if (it.id == layer.id) it.copy(visible = v) else it }) }
            }
            if (!fcl) ChoiceRow(null, visibilityOptions(), layer.visibility) { v ->
                state.edit { l -> l.copy(layers = l.layers.map { if (it.id == layer.id) it.copy(visibility = v) else it }) }
            }
        }
    }
    TextButton(onClick = {
        val n = state.layout.layers.size + 1
        val id = if (fcl) newHudId() else "layer$n"
        state.edit { l -> l.copy(layers = l.layers + HudLayer(id, (if (fcl) "Group " else "layer") + n)) }
        state.activeLayer = id
    }) { Text("＋ " + stringResource(if (fcl) R.string.hude_add_group else R.string.hude_add_layer), color = Ice, fontWeight = FontWeight.Bold) }

    renaming?.let { layer ->
        var text by remember(layer.id) { mutableStateOf(layer.name) }
        Dialog(onDismissRequest = { renaming = null }) {
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(PanelBg).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextRow(stringResource(R.string.generic_rename), text) { text = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.generic_cancel), color = Frost) }
                    TextButton(onClick = {
                        val name = text.trim().ifEmpty { layer.name }
                        state.edit { l -> l.copy(layers = l.layers.map { if (it.id == layer.id) it.copy(name = name) else it }) }
                        renaming = null
                    }) { Text(stringResource(R.string.generic_confirm), color = Ice) }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.InfoPanel(state: HudEditorState) {
    val l = state.layout
    Text(stringResource(R.string.hude_info), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    TextRow(stringResource(R.string.control_manage_create_new_name), l.name) { v -> state.edit(false) { it.copy(name = v) } }
    TextRow(stringResource(R.string.control_manage_create_new_author), l.author) { v -> state.edit(false) { it.copy(author = v) } }
    TextRow(stringResource(R.string.control_manage_create_new_version_name), l.version) { v -> state.edit(false) { it.copy(version = v) } }
    TextRow(stringResource(R.string.control_manage_info_description), l.description, singleLine = false) { v -> state.edit(false) { it.copy(description = v) } }
    Text(
        stringResource(R.string.hude_stats, l.buttons.size, l.sticks.size, l.layers.size),
        color = Frost.copy(alpha = 0.7f), fontSize = 11.sp
    )
}

// ---------------------------------------------------------------------- escolher ação

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionPickerDialog(
    fcl: Boolean,
    keysOnly: Boolean,
    layers: List<HudLayer>,
    onPick: (HudAction) -> Unit,
    onDismiss: () -> Unit
) {
    var textMode by remember { mutableStateOf(false) }
    var layerMode by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.92f).heightIn(max = 520.dp).clip(RoundedCornerShape(22.dp)).background(PanelBg)
                .border(1.dp, Line, RoundedCornerShape(22.dp)).padding(14.dp)
        ) {
            Text(stringResource(R.string.hude_pick_action), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                @Composable
                fun chip(label: String, accent: Boolean = false, onClick: () -> Unit) {
                    Text(
                        label, color = if (accent) Color(0xFF05101C) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (accent) Ice else Color(0x22FFFFFF))
                            .border(1.dp, Line, RoundedCornerShape(8.dp)).clickable(onClick = onClick)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                when {
                    textMode -> {
                        TextRow(stringResource(R.string.hude_act_text), text) { text = it }
                        Row { chip(stringResource(R.string.generic_confirm), accent = true) { if (text.isNotEmpty()) onPick(HudAction.Text(text)) } }
                    }
                    layerMode -> {
                        Text(stringResource(if (fcl) R.string.hude_groups else R.string.hude_layers), color = Frost, fontSize = 12.sp)
                        layers.forEach { layer ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(layer.name, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                chip("⇄ " + stringResource(R.string.hude_layer_toggle)) { onPick(HudAction.Layer(listOf(layer.id), 0)) }
                                chip("＋ " + stringResource(R.string.hude_layer_show)) { onPick(HudAction.Layer(listOf(layer.id), 1)) }
                                chip("－ " + stringResource(R.string.hude_layer_hide)) { onPick(HudAction.Layer(listOf(layer.id), 2)) }
                            }
                        }
                    }
                    else -> {
                        if (!keysOnly) {
                            Text(stringResource(R.string.hude_pick_special), color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                chip("LMB · " + stringResource(R.string.hude_mouse_left)) { onPick(HudAction.Mouse(HudKeys.MOUSE_LEFT)) }
                                chip("RMB · " + stringResource(R.string.hude_mouse_right)) { onPick(HudAction.Mouse(HudKeys.MOUSE_RIGHT)) }
                                chip("MMB · " + stringResource(R.string.hude_mouse_middle)) { onPick(HudAction.Mouse(HudKeys.MOUSE_MIDDLE)) }
                                chip("⇞ " + stringResource(R.string.hude_act_scroll_up)) { onPick(HudAction.Scroll(true)) }
                                chip("⇟ " + stringResource(R.string.hude_act_scroll_down)) { onPick(HudAction.Scroll(false)) }
                                if (!fcl) {
                                    chip("⇞ " + stringResource(R.string.hude_act_scroll_up) + " ∞") { onPick(HudAction.Scroll(true, true)) }
                                    chip("⇟ " + stringResource(R.string.hude_act_scroll_down) + " ∞") { onPick(HudAction.Scroll(false, true)) }
                                }
                                chip("☰ " + stringResource(R.string.hude_act_menu)) { onPick(HudAction.Menu) }
                                chip("⌨ " + stringResource(R.string.hude_act_keyboard)) { onPick(HudAction.Keyboard) }
                                chip("🖱 " + stringResource(if (fcl) R.string.hude_act_mouse_mode else R.string.hude_act_virtual_mouse)) { onPick(HudAction.MouseMode) }
                                if (!fcl) chip("◌ " + stringResource(R.string.hude_act_hide_hud)) { onPick(HudAction.HideHud) }
                                chip("“…” " + stringResource(R.string.hude_act_text)) { textMode = true }
                                chip("⇄ " + stringResource(if (fcl) R.string.hude_groups else R.string.hude_layers)) { layerMode = true }
                            }
                        }
                        Text(stringResource(R.string.hude_pick_keys), color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            HudKeys.all.forEach { k ->
                                val usable = if (fcl) k.evdev != null else k.android != null
                                if (usable) chip(k.label) { onPick(HudAction.Key(k.glfw)) }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (textMode || layerMode) TextButton(onClick = { textMode = false; layerMode = false }) { Text(stringResource(R.string.generic_back), color = Frost) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel), color = Frost) }
            }
        }
    }
}

/** Editor em tela cheia (menu de HUDs fora do jogo). */
@Composable
fun HudEditorDialog(initial: HudLayout, onSave: (HudLayout) -> Boolean, onExit: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        HudEditor(initial, inGame = false, onSave = onSave, onExit = onExit)
    }
}

