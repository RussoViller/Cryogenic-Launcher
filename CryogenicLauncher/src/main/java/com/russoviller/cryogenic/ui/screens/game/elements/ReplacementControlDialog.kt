/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.russoviller.cryogenic.ui.screens.game.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.ui.components.CryoChipButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoIconOrb
import com.russoviller.cryogenic.ui.components.CryoTag
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoTile
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.control.ControlData
import com.russoviller.cryogenic.game.control.ControlManager
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.SimpleListDialog
import com.russoviller.cryogenic.ui.components.SimpleListItem
import com.russoviller.cryogenic.ui.screens.main.control_editor.orUnspecified
import java.io.File

sealed interface ReplacementControlState {
    data object None : ReplacementControlState
    /** 显示更换控制布局的对话框 */
    data object Show : ReplacementControlState
}

@Composable
fun ReplacementControlOperation(
    operation: ReplacementControlState,
    onChange: (ReplacementControlState) -> Unit,
    currentLayout: File?,
    replacementControl: (File) -> Unit
) {
    when (operation) {
        ReplacementControlState.None -> {}
        ReplacementControlState.Show -> {
            ReplacementControlDialog(
                currentLayout = currentLayout,
                onLayoutSelected = { data ->
                    replacementControl(data.file)
                },
                onDismissRequest = {
                    onChange(ReplacementControlState.None)
                }
            )
        }
    }
}

@Composable
private fun ReplacementControlDialog(
    currentLayout: File?,
    onLayoutSelected: (ControlData) -> Unit,
    onDismissRequest: (selected: Boolean) -> Unit
) {
    val dataList by ControlManager.dataList.collectAsStateWithLifecycle()
    val controls = remember(dataList) { dataList.filter { it.isSupport } }

    if (controls.isNotEmpty()) {
        val locale = LocalConfiguration.current.locales[0]

        val current = remember(controls) {
            controls.find { currentLayout?.name == it.file.name }
        }

        CryoLayoutPickerDialog(
            controls = controls,
            current = current,
            locale = locale,
            onSelect = { data ->
                onLayoutSelected(data)
                onDismissRequest(true)
            },
            onDismiss = { onDismissRequest(false) }
        )
    } else {
        SimpleAlertDialog(
            title = stringResource(R.string.game_menu_option_replacement_control),
            text = stringResource(R.string.control_manage_list_empty),
            onConfirm = {
                onDismissRequest(false)
            },
            dismissText = stringResource(R.string.generic_refresh),
            onDismiss = {
                ControlManager.refresh()
            }
        )
    }
}


/** Janela "Mudar layout do HUD": um cartão por HUD com nome, autor, versão e camadas. */
@Composable
private fun CryoLayoutPickerDialog(
    controls: List<ControlData>,
    current: ControlData?,
    locale: java.util.Locale,
    onSelect: (ControlData) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .androidxWidthIn()
                .androidxHeightIn(rememberDialogMaxHeight())
                .cryoGlass(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), cornerRadius = 28.dp, window = true)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                CryoIconOrb(R.drawable.ic_cryo_swap, size = 44)
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.game_menu_option_replacement_control),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.cryo_layout_subtitle),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                    )
                }
                CryoChipButton(text = stringResource(R.string.generic_close), iconRes = R.drawable.ic_close, onClick = onDismiss)
            }
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                state = listState,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                items(controls.size) { index ->
                    val item = controls[index]
                    LayoutCard(
                        data = item,
                        isCurrent = item == current,
                        locale = locale,
                        onClick = { if (item != current) onSelect(item) else onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutCard(data: ControlData, isCurrent: Boolean, locale: java.util.Locale, onClick: () -> Unit) {
    val info = data.controlLayout.info
    val layers by data.controlLayout.layers.collectAsStateWithLifecycle()
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCurrent) Modifier.cryoGlow(18.dp, selected = true, strength = 0.7f) else Modifier)
            .cryoTile(active = isCurrent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        CryoIconOrb(R.drawable.ic_dashboard_filled, size = 42, active = isCurrent)
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
            androidx.compose.material3.Text(
                text = info.name.translate(locale).orUnspecified(),
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                CryoTag(text = stringResource(R.string.cryo_ctrl_layers_count, layers.size))
                info.versionName.takeIf { it.isNotBlank() }?.let { CryoTag(text = it, accent = CryoGlassColors.Aurora) }
                info.author.translate(locale).takeIf { it.isNotBlank() }?.let {
                    CryoTag(text = stringResource(R.string.cryo_layout_by, it), accent = CryoGlassColors.Frost, muted = true)
                }
            }
        }
        if (isCurrent) {
            CryoTag(text = stringResource(R.string.cryo_layout_in_use), icon = androidx.compose.ui.res.painterResource(R.drawable.ic_check))
        } else {
            androidx.compose.material3.Text(
                text = "›",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = CryoGlassColors.Ice
            )
        }
    }
}

private fun Modifier.androidxWidthIn(): Modifier = this.then(Modifier.widthIn(max = 720.dp))
private fun Modifier.androidxHeightIn(max: androidx.compose.ui.unit.Dp): Modifier = this.then(Modifier.heightIn(max = max))
