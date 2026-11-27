package com.russoviller.cryogenic.ui.screens.game.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.sdl.SdlBridge
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.unit.floatRange
import com.russoviller.cryogenic.ui.components.CryoActionTile
import com.russoviller.cryogenic.ui.components.CryoChipButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoIconOrb
import com.russoviller.cryogenic.ui.components.CryoSectionHeader
import com.russoviller.cryogenic.ui.components.CryoSliderTile
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.components.CryoToggleTile
import com.russoviller.cryogenic.ui.components.cryoTile
import com.russoviller.cryogenic.ui.components.lazyScrollWithBar
import com.russoviller.cryogenic.ui.screens.main.control_editor.orUnspecified
import com.russoviller.layer_controller.observable.ObservableControlLayer
import com.russoviller.layer_controller.observable.ObservableControlLayout

/** Cartão "HUD em uso": nome, autor, versão e quantas camadas tem. */
@Composable
private fun CurrentHudCard(layout: ObservableControlLayout?, onSwap: () -> Unit, onEdit: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val layers by (layout?.layers ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cryoTile(active = true)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CryoIconOrb(R.drawable.ic_dashboard_filled, size = 48)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.cryo_ctrl_current_hud).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Ice
            )
            Text(
                text = (layout?.info?.name?.translate(locale) ?: "").orUnspecified(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CryoTag(text = stringResource(R.string.cryo_ctrl_layers_count, layers.size))
                layout?.info?.versionName?.takeIf { it.isNotBlank() }?.let { CryoTag(text = it, accent = CryoGlassColors.Aurora) }
                layout?.info?.author?.translate(locale)?.takeIf { it.isNotBlank() }?.let {
                    CryoTag(text = it, accent = CryoGlassColors.Frost, muted = true)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
            CryoChipButton(text = stringResource(R.string.cryo_ctrl_swap), iconRes = R.drawable.ic_cryo_swap, onClick = onSwap)
            CryoChipButton(text = stringResource(R.string.cryo_ctrl_edit), iconRes = R.drawable.ic_edit_filled, onClick = onEdit)
        }
    }
}

/** Painel "Controles" do menu do jogo. */
@Composable
fun CryoControlOverview(
    modifier: Modifier = Modifier,
    layout: ObservableControlLayout?,
    closeScreen: () -> Unit,
    onInputMethod: () -> Unit,
    onSendKeycode: () -> Unit,
    onReplacementControl: () -> Unit,
    onEditLayout: () -> Unit
) {
    val listState = rememberLazyListState()
    val sdlEnabled by SdlBridge.enabled.collectAsState()

    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CurrentHudCard(
                layout = layout,
                onSwap = { onReplacementControl(); closeScreen() },
                onEdit = { onEditLayout(); closeScreen() }
            )
        }
        item { CryoSectionHeader(stringResource(R.string.cryo_ctrl_shortcuts)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoActionTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_keyboard_filled,
                    title = stringResource(R.string.game_menu_option_input_method),
                    subtitle = stringResource(R.string.cryo_ctrl_keyboard_sub),
                    onClick = { onInputMethod(); closeScreen() }
                )
                CryoActionTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_cryo_send,
                    title = stringResource(R.string.game_menu_option_send_keycode),
                    subtitle = stringResource(R.string.cryo_ctrl_sendkey_sub),
                    onClick = { onSendKeycode(); closeScreen() }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoActionTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_cryo_swap,
                    title = stringResource(R.string.game_menu_option_replacement_control),
                    subtitle = stringResource(R.string.cryo_ctrl_layout_sub),
                    onClick = { onReplacementControl(); closeScreen() }
                )
                CryoActionTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_edit_filled,
                    title = stringResource(R.string.control_manage_info_edit),
                    subtitle = stringResource(R.string.cryo_ctrl_edit_sub),
                    onClick = { onEditLayout(); closeScreen() }
                )
            }
        }
        item { CryoSectionHeader(stringResource(R.string.cryo_ctrl_adjustments)) }
        item {
            CryoToggleTile(
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.ic_keyboard_filled,
                title = stringResource(R.string.game_menu_option_auto_show_ime),
                subtitle = stringResource(R.string.cryo_ctrl_ime_sub),
                checked = AllSettings.sdlAutoShowIme.state,
                onCheckedChange = { AllSettings.sdlAutoShowIme.save(it) },
                enabled = sdlEnabled
            )
        }
        item {
            CryoSliderTile(
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.ic_cryo_opacity,
                title = stringResource(R.string.game_menu_option_controls_opacity),
                value = AllSettings.controlsOpacity.state,
                valueRange = AllSettings.controlsOpacity.floatRange,
                onValueChange = { AllSettings.controlsOpacity.updateState(it) },
                onValueChangeFinished = { AllSettings.controlsOpacity.save(AllSettings.controlsOpacity.state) },
                suffix = "%"
            )
        }
    }
}

/**
 * Painel "Camadas do HUD" do menu do jogo: mostra/esconde camadas na hora, sem abrir o editor.
 * Vale só enquanto o jogo está aberto (o HUD salvo não muda).
 */
@Composable
fun CryoGameLayersPanel(
    modifier: Modifier = Modifier,
    layout: ObservableControlLayout?
) {
    val layers by (layout?.layers ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = stringResource(R.string.cryo_layers_hint),
                style = MaterialTheme.typography.labelMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
            )
        }
        if (layers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.cryo_layers_empty), color = CryoGlassColors.Frost)
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoChipButton(
                        text = stringResource(R.string.cryo_layers_show_all),
                        iconRes = R.drawable.ic_visibility_outlined,
                        onClick = { layers.forEach { it.hide = false } }
                    )
                    CryoChipButton(
                        text = stringResource(R.string.cryo_layers_hide_all),
                        iconRes = R.drawable.ic_visibility_off_outlined,
                        onClick = { layers.forEach { it.hide = true } }
                    )
                }
            }
            items(layers, key = { it.uuid }) { layer ->
                GameLayerRow(layer)
            }
        }
    }
}

@Composable
private fun GameLayerRow(layer: ObservableControlLayer) {
    val buttons by layer.normalButtons.collectAsState()
    val joysticks by layer.joystickButtons.collectAsState()
    CryoToggleTile(
        modifier = Modifier.fillMaxWidth(),
        iconRes = if (layer.hide) R.drawable.ic_visibility_off_outlined else R.drawable.ic_visibility_outlined,
        title = layer.name.orUnspecified(),
        subtitle = stringResource(R.string.cryo_layers_widgets, buttons.size, joysticks.size),
        checked = !layer.hide,
        onCheckedChange = { layer.hide = !it }
    )
}
