package com.russoviller.cryogenic.ui.screens.content.settings

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.GamepadInputMode
import com.russoviller.cryogenic.setting.unit.floatRange
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedColumn
import com.russoviller.cryogenic.ui.components.CryoAlertDialog
import com.russoviller.cryogenic.ui.components.CryoChipButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSegmentedTabs
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleEditDialog
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlassShape
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoTile
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.control.GamepadBindingKeyboard
import com.russoviller.cryogenic.ui.control.gamepad.DpadDirection
import com.russoviller.cryogenic.ui.control.gamepad.GamepadBindingMode
import com.russoviller.cryogenic.ui.control.gamepad.GamepadBindingModes
import com.russoviller.cryogenic.ui.control.gamepad.GamepadExtraButtons
import com.russoviller.cryogenic.ui.control.gamepad.GamepadGlyphStyle
import com.russoviller.cryogenic.ui.control.gamepad.GamepadInputTap
import com.russoviller.cryogenic.ui.control.gamepad.GamepadMap
import com.russoviller.cryogenic.ui.control.gamepad.JoystickMode
import com.russoviller.cryogenic.ui.control.gamepad.bindingCode
import com.russoviller.cryogenic.ui.control.gamepad.connectedGamepads
import com.russoviller.cryogenic.ui.control.gamepad.dpadBindingCode
import com.russoviller.cryogenic.ui.control.gamepad.gamepadButtonCode
import com.russoviller.cryogenic.ui.control.gamepad.gamepadButtonLabel
import com.russoviller.cryogenic.ui.control.gamepad.getNameByGamepadEvent
import com.russoviller.cryogenic.ui.control.gamepad.remapperMMKV
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.CardPosition
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.EnumSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCard
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SettingsCardColumn
import com.russoviller.cryogenic.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.GAMEPAD_CONFIG_NAME_LENGTH
import com.russoviller.cryogenic.viewmodel.GamepadViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs

/*
 * Aba "Gamepad" das configurações: suporte completo a controle físico (cabo ou Bluetooth).
 * - Geral: liga/desliga, modo de entrada, analógicos, gatilhos, curva, inversão, estilo dos nomes.
 * - Mapeamento: desenho do controle; tocar num botão abre a ligação (várias teclas, modo
 *   segurar/alternar/turbo), separado para jogo e menus, com perfis e botões extras (paddles,
 *   C/Z, Fn...) capturados direto do controle.
 * - Testar: tudo o que o Android recebe do controle, ao vivo.
 */

/** Estado ao vivo do controle (só enquanto a aba está aberta). */
private class LiveGamepad {
    val pressed = mutableStateMapOf<Int, Boolean>()
    var lx by mutableFloatStateOf(0f)
    var ly by mutableFloatStateOf(0f)
    var rx by mutableFloatStateOf(0f)
    var ry by mutableFloatStateOf(0f)
    var lt by mutableFloatStateOf(0f)
    var rt by mutableFloatStateOf(0f)
    var last by mutableStateOf("")
    var device by mutableStateOf<InputDevice?>(null)
}

private val DPAD_KEYS = mapOf(
    KeyEvent.KEYCODE_DPAD_UP to DpadDirection.Up,
    KeyEvent.KEYCODE_DPAD_DOWN to DpadDirection.Down,
    KeyEvent.KEYCODE_DPAD_LEFT to DpadDirection.Left,
    KeyEvent.KEYCODE_DPAD_RIGHT to DpadDirection.Right
)

/** Código que o jogo usa para o evento (gatilho digital vira o eixo; direcional vira código próprio). */
private fun KeyEvent.layoutCode(): Int = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L2 -> MotionEvent.AXIS_LTRIGGER
    KeyEvent.KEYCODE_BUTTON_R2 -> MotionEvent.AXIS_RTRIGGER
    in DPAD_KEYS -> dpadBindingCode(DPAD_KEYS.getValue(keyCode))
    else -> gamepadButtonCode()
}

/** Códigos que já estão no desenho do controle. */
private val STANDARD_CODES: Set<Int> = GamepadMap.entries.map { it.bindingCode() }.toSet() + KeyEvent.KEYCODE_BUTTON_MODE

@Composable
fun GamepadSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Gamepad, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        val vm: GamepadViewModel = viewModel()
        val live = remember { LiveGamepad() }
        var page by remember { mutableIntStateOf(0) }
        var refresh by remember { mutableIntStateOf(0) }
        var capturing by remember { mutableStateOf(false) }
        val devices = remember { mutableStateListOf<InputDevice>() }

        //controles conectados (atualiza sozinho ao ligar/desligar)
        LaunchedEffect(isVisible) {
            while (isVisible) {
                val now = connectedGamepads()
                if (now.map { it.descriptor } != devices.map { it.descriptor }) {
                    devices.clear()
                    devices.addAll(now)
                }
                delay(1500)
            }
        }

        val configName = AllSettings.gamepadMappingConfig.state
        val styleSetting = AllSettings.gamepadGlyphStyle.state
        val style = if (styleSetting == GamepadGlyphStyle.Auto) GamepadGlyphStyle.forDevice(live.device ?: devices.firstOrNull()) else styleSetting

        //lê o controle enquanto a aba está aberta (botões não navegam pelo launcher aqui)
        DisposableEffect(isVisible) {
            if (isVisible) {
                GamepadInputTap.listener = object : GamepadInputTap.Listener {
                    override fun onKey(event: KeyEvent): Boolean {
                        if (event.repeatCount > 0) return true
                        val code = event.layoutCode()
                        val down = event.action != KeyEvent.ACTION_UP
                        live.pressed[code] = down
                        live.device = event.device
                        live.last = "${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode}) · scan ${event.scanCode} · ${event.device?.name ?: "?"}"
                        if (down && capturing) {
                            capturing = false
                            if (code in STANDARD_CODES) {
                                Toast.makeText(context, context.getString(R.string.cryo_gamepad_extras_exists), Toast.LENGTH_SHORT).show()
                            } else {
                                GamepadExtraButtons.add(configName, code)
                                refresh++
                                Toast.makeText(context, context.getString(R.string.cryo_gamepad_extras_added, gamepadButtonLabel(code, style)), Toast.LENGTH_SHORT).show()
                            }
                        }
                        return true
                    }

                    override fun onMotion(event: MotionEvent): Boolean {
                        live.device = event.device
                        live.lx = event.getAxisValue(MotionEvent.AXIS_X)
                        live.ly = event.getAxisValue(MotionEvent.AXIS_Y)
                        live.rx = event.getAxisValue(MotionEvent.AXIS_Z)
                        live.ry = event.getAxisValue(MotionEvent.AXIS_RZ)
                        live.lt = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
                        live.rt = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
                        val limit = AllSettings.gamepadTriggerThreshold.state / 100f
                        live.pressed[MotionEvent.AXIS_LTRIGGER] = live.lt >= limit
                        live.pressed[MotionEvent.AXIS_RTRIGGER] = live.rt >= limit
                        val hx = event.getAxisValue(MotionEvent.AXIS_HAT_X)
                        val hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                        live.pressed[dpadBindingCode(DpadDirection.Left)] = hx < -0.5f
                        live.pressed[dpadBindingCode(DpadDirection.Right)] = hx > 0.5f
                        live.pressed[dpadBindingCode(DpadDirection.Up)] = hy < -0.5f
                        live.pressed[dpadBindingCode(DpadDirection.Down)] = hy > 0.5f
                        return true
                    }
                }
            }
            onDispose {
                GamepadInputTap.listener = null
                capturing = false
            }
        }

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                Column(modifier = Modifier.fillMaxWidth().animated(ctx), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeviceBanner(devices, style)
                    CryoSegmentedTabs(
                        titles = listOf(
                            stringResource(R.string.cryo_gamepad_page_general),
                            stringResource(R.string.cryo_gamepad_page_mapping),
                            stringResource(R.string.cryo_gamepad_page_test)
                        ),
                        selected = page,
                        onSelect = { page = it; capturing = false }
                    )
                }
            }
            AnimatedItem { ctx ->
                Box(modifier = Modifier.fillMaxWidth().animated(ctx)) {
                    when (page) {
                        0 -> GeneralPage()
                        1 -> MappingPage(
                            vm = vm,
                            style = style,
                            configName = configName,
                            refresh = refresh,
                            onRefresh = { refresh++ },
                            capturing = capturing,
                            onCapture = { capturing = it },
                            live = live
                        )
                        else -> TestPage(live = live, style = style, devices = devices)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- topo

@Composable
private fun DeviceBanner(devices: List<InputDevice>, style: GamepadGlyphStyle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cryoGlass(shape = cryoGlassShape(18.dp), cornerRadius = 18.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_cryo_gamepad), null, tint = CryoGlassColors.Deep, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.cryo_gamepad_no_device), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                devices.forEach { d ->
                    Text(d.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(stringResource(R.string.cryo_gamepad_banner_hint), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.8f))
        }
        CryoTag(text = stringResource(style.titleRes))
    }
}

// ---------------------------------------------------------------------------- Geral

@Composable
private fun GeneralPage() {
    val enabled = AllSettings.gamepadControl.state
    val mapped = enabled && AllSettings.gamepadInputMode.state == GamepadInputMode.Mapped
    var askRecalibrate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                unit = AllSettings.gamepadControl,
                title = stringResource(R.string.settings_gamepad_title),
                summary = stringResource(R.string.settings_gamepad_summary)
            )
            EnumSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.gamepadInputMode,
                entries = GamepadInputMode.entries,
                title = stringResource(R.string.settings_gamepad_input_mode_title),
                summary = stringResource(
                    if (AllSettings.gamepadInputMode.state == GamepadInputMode.Mapped) R.string.settings_gamepad_input_mode_mapped_summary
                    else R.string.settings_gamepad_input_mode_sdl_summary
                ),
                getRadioText = { stringResource(it.titleRes) },
                getRadioEnable = { enabled }
            )
            EnumSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.joystickControlMode,
                entries = JoystickMode.entries,
                title = stringResource(R.string.settings_gamepad_joystick_mode_title),
                summary = stringResource(R.string.settings_gamepad_joystick_mode_summary),
                getRadioText = { stringResource(it.titleRes) },
                getRadioEnable = { mapped }
            )
            EnumSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Bottom,
                unit = AllSettings.gamepadGlyphStyle,
                entries = GamepadGlyphStyle.entries,
                title = stringResource(R.string.cryo_gamepad_glyph_title),
                summary = stringResource(R.string.cryo_gamepad_glyph_summary),
                getRadioText = { stringResource(it.titleRes) },
                getRadioEnable = { true }
            )
        }

        SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                unit = AllSettings.gamepadDeadZoneScale,
                title = stringResource(R.string.settings_gamepad_deadzone_title),
                summary = stringResource(R.string.settings_gamepad_deadzone_summary),
                valueRange = AllSettings.gamepadDeadZoneScale.floatRange,
                suffix = "%",
                enabled = mapped,
                fineTuningControl = true
            )
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.gamepadResponseCurve,
                title = stringResource(R.string.cryo_gamepad_curve_title),
                summary = stringResource(R.string.cryo_gamepad_curve_summary),
                valueRange = AllSettings.gamepadResponseCurve.floatRange,
                suffix = "÷10",
                enabled = mapped,
                fineTuningControl = true
            )
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.gamepadTriggerThreshold,
                title = stringResource(R.string.cryo_gamepad_trigger_title),
                summary = stringResource(R.string.cryo_gamepad_trigger_summary),
                valueRange = AllSettings.gamepadTriggerThreshold.floatRange,
                suffix = "%",
                enabled = mapped,
                fineTuningControl = true
            )
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.gamepadCameraSensitivity,
                title = stringResource(R.string.settings_gamepad_camera_sensitivity_title),
                summary = stringResource(R.string.settings_gamepad_camera_sensitivity_summary),
                valueRange = AllSettings.gamepadCameraSensitivity.floatRange,
                suffix = "%",
                enabled = mapped,
                fineTuningControl = true
            )
            IntSliderSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.gamepadCursorSensitivity,
                title = stringResource(R.string.settings_gamepad_cursor_sensitivity_title),
                summary = stringResource(R.string.settings_gamepad_cursor_sensitivity_summary),
                valueRange = AllSettings.gamepadCursorSensitivity.floatRange,
                suffix = "%",
                enabled = mapped,
                fineTuningControl = true
            )
            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.gamepadInvertY,
                title = stringResource(R.string.cryo_gamepad_invert_y),
                enabled = mapped
            )
            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Bottom,
                unit = AllSettings.gamepadInvertX,
                title = stringResource(R.string.cryo_gamepad_invert_x),
                enabled = mapped
            )
        }

        SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Single,
                onClick = { askRecalibrate = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.cryo_gamepad_recalibrate_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.cryo_gamepad_recalibrate_summary), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if (askRecalibrate) {
        SimpleAlertDialog(
            title = stringResource(R.string.cryo_gamepad_recalibrate_title),
            text = stringResource(R.string.cryo_gamepad_recalibrate_summary),
            onConfirm = {
                askRecalibrate = false
                remapperMMKV().clearAll()
                Toast.makeText(context, context.getString(R.string.cryo_gamepad_recalibrate_done), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { askRecalibrate = false }
        )
    }
}

// ---------------------------------------------------------------------------- Mapeamento

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MappingPage(
    vm: GamepadViewModel,
    style: GamepadGlyphStyle,
    configName: String,
    refresh: Int,
    onRefresh: () -> Unit,
    capturing: Boolean,
    onCapture: (Boolean) -> Unit,
    live: LiveGamepad
) {
    val context = LocalContext.current
    var inGame by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val mapping = remember(refresh, configName) { vm.currentMapping }
    val configs = remember(refresh, configName) { vm.getAllConfigKeys().sorted() }
    val extras = remember(refresh, configName) { GamepadExtraButtons.list(configName) }
    val modes = remember(refresh, configName, inGame) { GamepadBindingModes.load(configName, inGame) }

    @Composable
    fun bindingText(code: Int): String {
        val keys = mapping?.findTargets(code, inGame).orEmpty()
        if (keys.isEmpty()) return "—"
        val names = keys.map { getNameByGamepadEvent(it) }
        val mode = modes[code]?.first
        val prefix = when (mode) {
            GamepadBindingMode.Toggle -> "⇄ "
            GamepadBindingMode.Turbo -> "⚡ "
            else -> ""
        }
        return prefix + names.joinToString(" + ")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        //perfil + contexto
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cryoGlass(shape = cryoGlassShape(18.dp), cornerRadius = 18.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.settings_gamepad_config_title).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Black, color = CryoGlassColors.Ice)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                configs.forEach { name ->
                    Pill(name, selected = name == configName) {
                        AllSettings.gamepadMappingConfig.save(name)
                        vm.reloadAllMappings()
                        onRefresh()
                    }
                }
                Pill("+ " + stringResource(R.string.settings_gamepad_config_create), selected = false) { creating = true }
                if (configs.size > 1) {
                    Pill(stringResource(R.string.settings_gamepad_config_delete), selected = false, danger = true) { deleting = true }
                }
            }
            CryoSegmentedTabs(
                titles = listOf(stringResource(R.string.settings_gamepad_mapping_in_game), stringResource(R.string.settings_gamepad_mapping_in_menu)),
                selected = if (inGame) 0 else 1,
                onSelect = { inGame = it == 0 }
            )
            Text(stringResource(R.string.cryo_gamepad_bind_hint), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.8f))
        }

        //desenho do controle
        ControllerDiagram(
            style = style,
            pressed = { live.pressed[it] == true },
            caption = { bindingText(it) },
            sticks = null,
            onClick = { editing = it }
        )

        //botões extras
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cryoGlass(shape = cryoGlassShape(18.dp), cornerRadius = 18.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.cryo_gamepad_extras_title).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Black, color = CryoGlassColors.Ice)
            Text(stringResource(R.string.cryo_gamepad_extras_summary), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.85f))
            if (extras.isEmpty()) {
                Text(stringResource(R.string.cryo_gamepad_extras_none), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    extras.forEach { code ->
                        ButtonChip(
                            glyph = gamepadButtonLabel(code, style),
                            caption = bindingText(code),
                            pressed = live.pressed[code] == true,
                            wide = true,
                            onClick = { editing = code }
                        )
                    }
                }
            }
            CryoChipButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (capturing) stringResource(R.string.cryo_gamepad_extras_waiting) else stringResource(R.string.cryo_gamepad_extras_add),
                iconRes = R.drawable.ic_add,
                onClick = { onCapture(!capturing) }
            )
        }
    }

    editing?.let { code ->
        val map = mapping
        if (map != null) {
            BindingDialog(
                code = code,
                label = gamepadButtonLabel(code, style),
                initialKeys = map.findTargets(code, inGame).orEmpty(),
                initialMode = GamepadBindingModes.mode(configName, inGame, code),
                initialRate = GamepadBindingModes.turboRate(configName, inGame, code),
                isExtra = code !in STANDARD_CODES,
                onDismiss = { editing = null },
                onSave = { keys, mode, rate ->
                    map.saveTargets(code, keys, inGame)
                    GamepadBindingModes.save(configName, inGame, code, mode, rate)
                    editing = null
                    onRefresh()
                },
                onDefault = GamepadMap.entries.firstOrNull { it.bindingCode() == code }?.let { gm ->
                    {
                        map.resetMapping(gm, inGame)
                        GamepadBindingModes.save(configName, inGame, code, GamepadBindingMode.Hold, 10)
                        editing = null
                        onRefresh()
                    }
                },
                onRemove = {
                    map.removeCode(code)
                    GamepadExtraButtons.remove(configName, code)
                    GamepadBindingModes.save(configName, true, code, GamepadBindingMode.Hold, 10)
                    GamepadBindingModes.save(configName, false, code, GamepadBindingMode.Hold, 10)
                    editing = null
                    onRefresh()
                }
            )
        } else editing = null
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        SimpleEditDialog(
            title = stringResource(R.string.settings_gamepad_config_create),
            value = name,
            onValueChange = { name = it.take(GAMEPAD_CONFIG_NAME_LENGTH) },
            label = { Text(stringResource(R.string.settings_gamepad_config_create_name)) },
            singleLine = true,
            onDismissRequest = { creating = false },
            onConfirm = {
                val n = name.trim()
                if (n.isNotEmpty()) {
                    if (vm.containsConfig(n)) {
                        Toast.makeText(context, context.getString(R.string.settings_gamepad_config_create_contains), Toast.LENGTH_SHORT).show()
                    } else {
                        vm.createNewConfig(n, onContainsConfig = {})
                        creating = false
                        onRefresh()
                    }
                }
            }
        )
    }
    if (deleting) {
        SimpleAlertDialog(
            title = stringResource(R.string.settings_gamepad_config_delete),
            text = stringResource(R.string.settings_gamepad_config_delete_message),
            onConfirm = {
                deleting = false
                GamepadBindingModes.deleteConfig(configName)
                GamepadExtraButtons.deleteConfig(configName)
                vm.deleteConfig(configName)
                vm.reloadAllMappings()
                onRefresh()
            },
            onDismiss = { deleting = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BindingDialog(
    code: Int,
    label: String,
    initialKeys: Set<String>,
    initialMode: GamepadBindingMode,
    initialRate: Int,
    isExtra: Boolean,
    onDismiss: () -> Unit,
    onSave: (Set<String>, GamepadBindingMode, Int) -> Unit,
    onDefault: (() -> Unit)?,
    onRemove: () -> Unit
) {
    val keys = remember(code) { mutableStateListOf<String>().apply { addAll(initialKeys) } }
    var mode by remember(code) { mutableStateOf(initialMode) }
    var rate by remember(code) { mutableIntStateOf(initialRate) }
    var picking by remember { mutableStateOf(false) }

    CryoAlertDialog(
        title = stringResource(R.string.cryo_gamepad_bind_title, label),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.generic_confirm),
        onConfirm = { onSave(keys.toSet(), mode, rate) },
        dismissText = stringResource(R.string.generic_cancel),
        onCancel = onDismiss,
        iconRes = R.drawable.ic_cryo_gamepad
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            //teclas ligadas
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (keys.isEmpty()) {
                    Text(stringResource(R.string.cryo_gamepad_bind_none), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                } else keys.forEach { k -> CryoTag(text = getNameByGamepadEvent(k), fontSize = 12.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoChipButton(modifier = Modifier.weight(1f), text = stringResource(R.string.cryo_gamepad_bind_keys), iconRes = R.drawable.ic_keyboard_filled, onClick = { picking = true })
                CryoChipButton(modifier = Modifier.weight(1f), text = stringResource(R.string.cryo_gamepad_bind_clear), iconRes = R.drawable.ic_deselect, enabled = keys.isNotEmpty(), onClick = { keys.clear() })
            }
            //modo
            Text(stringResource(R.string.cryo_gamepad_mode_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            CryoSegmentedTabs(
                titles = GamepadBindingMode.entries.map { stringResource(it.titleRes) },
                selected = mode.ordinal,
                onSelect = { mode = GamepadBindingMode.entries[it] }
            )
            Text(
                stringResource(
                    when (mode) {
                        GamepadBindingMode.Hold -> R.string.cryo_gamepad_mode_hold_summary
                        GamepadBindingMode.Toggle -> R.string.cryo_gamepad_mode_toggle_summary
                        GamepadBindingMode.Turbo -> R.string.cryo_gamepad_mode_turbo_summary
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
            if (mode == GamepadBindingMode.Turbo) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cryo_gamepad_turbo_rate), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    StepButton("−") { rate = (rate - 1).coerceAtLeast(2) }
                    Text("$rate/s", style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 44.dp), textAlign = TextAlign.Center)
                    StepButton("+") { rate = (rate + 1).coerceAtMost(20) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDefault != null) {
                    CryoChipButton(modifier = Modifier.weight(1f), text = stringResource(R.string.cryo_gamepad_bind_default), iconRes = R.drawable.ic_undo, onClick = onDefault)
                }
                if (isExtra) {
                    CryoChipButton(modifier = Modifier.weight(1f), text = stringResource(R.string.cryo_gamepad_extras_remove), iconRes = R.drawable.ic_delete_filled, onClick = onRemove)
                }
            }
        }
    }

    if (picking) {
        GamepadBindingKeyboard(
            selectedKeys = keys,
            onKeyAdd = { if (it !in keys) keys.add(it) },
            onKeyRemove = { keys.remove(it) },
            onDismissRequest = { picking = false }
        )
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val accent = if (danger) Color(0xFFFF6B8A) else CryoGlassColors.Ice
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(Color.White.copy(alpha = 0.07f)).border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) CryoGlassColors.Deep else if (danger) accent else Color.White, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------- Testar

@Composable
private fun TestPage(live: LiveGamepad, style: GamepadGlyphStyle, devices: List<InputDevice>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ControllerDiagram(
            style = style,
            pressed = { live.pressed[it] == true },
            caption = null,
            sticks = live,
            onClick = null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cryoGlass(shape = cryoGlassShape(18.dp), cornerRadius = 18.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.cryo_gamepad_test_hint), style = MaterialTheme.typography.labelSmall, color = CryoGlassColors.Frost.copy(alpha = 0.85f))
            AxisRow("LX", live.lx); AxisRow("LY", live.ly); AxisRow("RX", live.rx); AxisRow("RY", live.ry)
            AxisRow(gamepadButtonLabel(MotionEvent.AXIS_LTRIGGER, style), live.lt, positiveOnly = true)
            AxisRow(gamepadButtonLabel(MotionEvent.AXIS_RTRIGGER, style), live.rt, positiveOnly = true)
            val pressedNow = live.pressed.filterValues { it }.keys.sorted()
            Text(
                stringResource(R.string.cryo_gamepad_test_pressed, if (pressedNow.isEmpty()) "—" else pressedNow.joinToString("  ") { gamepadButtonLabel(it, style) }),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color.White
            )
            if (live.last.isNotEmpty()) {
                Text(stringResource(R.string.cryo_gamepad_test_last, live.last), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CryoGlassColors.Ice)
            }
            devices.forEach { d ->
                Text(
                    "${d.name} · VID ${"%04X".format(d.vendorId)} PID ${"%04X".format(d.productId)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AxisRow(label: String, value: Float, positiveOnly: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = CryoGlassColors.Ice, maxLines = 1)
        Canvas(modifier = Modifier.weight(1f).height(10.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Color.White.copy(alpha = 0.1f), cornerRadius = r)
            if (positiveOnly) {
                drawRoundRect(CryoGlassColors.Ice, size = Size(size.width * value.coerceIn(0f, 1f), size.height), cornerRadius = r)
            } else {
                val mid = size.width / 2
                val w = mid * abs(value.coerceIn(-1f, 1f))
                val x = if (value < 0) mid - w else mid
                drawRoundRect(CryoGlassColors.Ice, topLeft = Offset(x, 0f), size = Size(w, size.height), cornerRadius = r)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(mid, 0f), Offset(mid, size.height), 1f)
            }
        }
        Text("%+.2f".format(value), modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.White, textAlign = TextAlign.End)
    }
}

// ---------------------------------------------------------------------------- desenho do controle

/**
 * Controle desenhado com os botões no lugar de verdade, todos dentro do corpo do controle.
 * [caption] = ligação de cada botão (mapeamento); [sticks] = posição ao vivo dos analógicos (teste).
 */
@Composable
private fun ControllerDiagram(
    style: GamepadGlyphStyle,
    pressed: (Int) -> Boolean,
    caption: (@Composable (Int) -> String)?,
    sticks: LiveGamepad?,
    onClick: ((Int) -> Unit)?
) {
    val ice = CryoGlassColors.Ice
    val navy = CryoGlassColors.Navy
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = if (caption != null) 640.dp else 560.dp)
                .fillMaxWidth()
                .aspectRatio(if (caption != null) 1.5f else 1.65f)
        ) {
            val w = maxWidth
            val h = maxHeight
            Canvas(modifier = Modifier.fillMaxSize()) {
                val W = size.width
                val H = size.height
                val fill = Brush.verticalGradient(listOf(lerp(navy, Color.Black, 0.25f), lerp(navy, Color.Black, 0.6f)))
                val rim = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.65f), ice.copy(alpha = 0.35f)))
                val stroke = Stroke(2.dp.toPx())
                //gatilhos (atrás do corpo)
                for (left in listOf(true, false)) {
                    val x0 = if (left) W * 0.07f else W * 0.61f
                    val tl = Offset(x0, 0f)
                    val sz = Size(W * 0.32f, H * 0.26f)
                    val r = CornerRadius(H * 0.08f)
                    drawRoundRect(Brush.verticalGradient(listOf(navy.copy(alpha = 0.7f), navy.copy(alpha = 0.45f))), tl, sz, r)
                    drawRoundRect(rim, tl, sz, r, style = stroke)
                }
                val body = Path().apply {
                    moveTo(W * 0.20f, H * 0.20f)
                    cubicTo(W * 0.36f, H * 0.17f, W * 0.64f, H * 0.17f, W * 0.80f, H * 0.20f)
                    cubicTo(W * 0.93f, H * 0.23f, W * 0.995f, H * 0.58f, W * 0.99f, H * 0.84f)
                    cubicTo(W * 0.985f, H * 0.995f, W * 0.84f, H * 1.02f, W * 0.765f, H * 0.93f)
                    cubicTo(W * 0.71f, H * 0.86f, W * 0.64f, H * 0.82f, W * 0.5f, H * 0.82f)
                    cubicTo(W * 0.36f, H * 0.82f, W * 0.29f, H * 0.86f, W * 0.235f, H * 0.93f)
                    cubicTo(W * 0.16f, H * 1.02f, W * 0.015f, H * 0.995f, W * 0.01f, H * 0.84f)
                    cubicTo(W * 0.005f, H * 0.58f, W * 0.07f, H * 0.23f, W * 0.20f, H * 0.20f)
                    close()
                }
                drawPath(body, fill)
                drawPath(body, rim, style = stroke)
                //placa do direcional e dos botões de ação
                drawCircle(Color.White.copy(alpha = 0.05f), H * 0.17f, Offset(W * 0.36f, H * 0.62f))
                drawCircle(Color.White.copy(alpha = 0.05f), H * 0.17f, Offset(W * 0.78f, H * 0.42f))
            }

            @Composable
            fun button(code: Int, fx: Float, fy: Float, wide: Boolean = false) {
                CenteredAt(w * fx, h * fy) {
                    ButtonChip(
                        glyph = gamepadButtonLabel(code, style),
                        caption = caption?.invoke(code),
                        pressed = pressed(code),
                        wide = wide,
                        onClick = onClick?.let { { it(code) } }
                    )
                }
            }

            @Composable
            fun stick(code: Int, fx: Float, fy: Float, x: Float, y: Float) {
                CenteredAt(w * fx, h * fy) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(if (onClick != null) Modifier.clickable { onClick(code) } else Modifier)
                        ) { StickView(x, y, pressed(code)) }
                        Text(
                            gamepadButtonLabel(code, style) + (caption?.invoke(code)?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (pressed(code)) ice else Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 104.dp)
                        )
                    }
                }
            }

            //gatilhos e bumpers (dentro das peças de cima)
            button(MotionEvent.AXIS_LTRIGGER, 0.145f, 0.085f, wide = true)
            button(KeyEvent.KEYCODE_BUTTON_L1, 0.31f, 0.085f, wide = true)
            button(KeyEvent.KEYCODE_BUTTON_R1, 0.69f, 0.085f, wide = true)
            button(MotionEvent.AXIS_RTRIGGER, 0.855f, 0.085f, wide = true)
            //centro
            button(KeyEvent.KEYCODE_BUTTON_SELECT, 0.42f, 0.33f, wide = true)
            button(KeyEvent.KEYCODE_BUTTON_START, 0.58f, 0.33f, wide = true)
            button(KeyEvent.KEYCODE_BUTTON_MODE, 0.50f, 0.50f, wide = true)
            //analógicos (tocar no analógico = clique dele, L3/R3)
            stick(KeyEvent.KEYCODE_BUTTON_THUMBL, 0.19f, 0.44f, sticks?.lx ?: 0f, sticks?.ly ?: 0f)
            stick(KeyEvent.KEYCODE_BUTTON_THUMBR, 0.64f, 0.64f, sticks?.rx ?: 0f, sticks?.ry ?: 0f)
            //direcional
            button(dpadBindingCode(DpadDirection.Up), 0.36f, 0.515f)
            button(dpadBindingCode(DpadDirection.Left), 0.295f, 0.62f)
            button(dpadBindingCode(DpadDirection.Right), 0.425f, 0.62f)
            button(dpadBindingCode(DpadDirection.Down), 0.36f, 0.725f)
            //botões de ação
            button(KeyEvent.KEYCODE_BUTTON_Y, 0.78f, 0.315f)
            button(KeyEvent.KEYCODE_BUTTON_X, 0.715f, 0.42f)
            button(KeyEvent.KEYCODE_BUTTON_B, 0.845f, 0.42f)
            button(KeyEvent.KEYCODE_BUTTON_A, 0.78f, 0.525f)
        }
    }
}

/** Posiciona o conteúdo com o centro no ponto dado. */
@Composable
private fun CenteredAt(x: Dp, y: Dp, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { p ->
                p.place(x.roundToPx() - p.width / 2, y.roundToPx() - p.height / 2)
            }
        }
    }
}

@Composable
private fun StickView(x: Float, y: Float, clicked: Boolean) {
    val ice = CryoGlassColors.Ice
    Canvas(modifier = Modifier.size(52.dp)) {
        val r = size.minDimension / 2
        drawCircle(Color.Black.copy(alpha = 0.45f), r)
        drawCircle(ice.copy(alpha = 0.6f), r, style = Stroke(1.5.dp.toPx()))
        val c = Offset(size.width / 2 + x.coerceIn(-1f, 1f) * r * 0.55f, size.height / 2 + y.coerceIn(-1f, 1f) * r * 0.55f)
        drawCircle(if (clicked) ice else Color.White.copy(alpha = 0.85f), r * 0.42f, c)
    }
}

@Composable
private fun ButtonChip(glyph: String, caption: String?, pressed: Boolean, wide: Boolean = false, onClick: (() -> Unit)?) {
    val ice = CryoGlassColors.Ice
    Column(
        modifier = Modifier
            .then(if (caption != null) Modifier.widthIn(min = if (wide) 64.dp else 44.dp, max = if (wide) 96.dp else 80.dp) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (pressed) Modifier.cryoBackground(cryoGradient())
                else Modifier.background(Color.Black.copy(alpha = 0.5f))
            )
            .border(1.dp, if (pressed) Color.White else ice.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = if (pressed) CryoGlassColors.Deep else Color.White,
            maxLines = 1
        )
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (pressed) CryoGlassColors.Deep else ice,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Só para testes de imagem: o conteúdo de uma página sem a navegação. */
@Composable
internal fun GamepadSettingsPreview(page: Int, pressed: Set<Int> = emptySet()) {
    val vm: GamepadViewModel = viewModel()
    val live = remember { LiveGamepad().apply { pressed.forEach { this.pressed[it] = true }; lx = 0.6f; ly = -0.3f; rt = 0.8f } }
    val style = AllSettings.gamepadGlyphStyle.state.let { if (it == GamepadGlyphStyle.Auto) GamepadGlyphStyle.Xbox else it }
    Column(modifier = Modifier.padding(12.dp).verticalScrollWithBar(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeviceBanner(emptyList(), style)
        when (page) {
            0 -> GeneralPage()
            1 -> MappingPage(vm, style, AllSettings.gamepadMappingConfig.state, 0, {}, false, {}, live)
            else -> TestPage(live, style, emptyList())
        }
    }
}
