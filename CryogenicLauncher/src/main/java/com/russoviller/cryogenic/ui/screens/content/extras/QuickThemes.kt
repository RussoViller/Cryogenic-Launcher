package com.russoviller.cryogenic.ui.screens.content.extras

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.extras.CryoPresets
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlassShape
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.lazyScrollWithBar
import com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel
import kotlinx.coroutines.launch

/**
 * "Temas rápidos" do Modo Descanso: botão no topo que abre a lista das predefinições salvas
 * (com barra de rolagem). Um toque aplica a predefinição na hora, sem sair do descanso.
 */
@Composable
fun QuickThemesButton(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { CryoPresets.load() }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(38.dp)
                .cryoGlass(shape = cryoGlassShape(19.dp), glow = false, cornerRadius = 19.dp)
                .clickable { open = !open }
                .padding(start = 4.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .cryoBackground(cryoGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(17.dp),
                    painter = painterResource(R.drawable.ic_style_outlined),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Text(
                text = stringResource(R.string.cryo_quick_themes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = if (open) "▴" else "▾",
                color = CryoGlassColors.Ice,
                fontWeight = FontWeight.Black
            )
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(androidx.compose.ui.platform.LocalDensity.current) { 44.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                QuickThemesList(onApplied = { open = false })
            }
        }
    }
}

@Composable
private fun QuickThemesList(onApplied: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val background = LocalBackgroundViewModel.current
    val presets = CryoPresets.presets
    val activeId = AllSettings.cryoActivePresetId.state
    var applying by remember { mutableStateOf<String?>(null) }
    val appliedText = stringResource(R.string.cryo_presets_applied)
    val errorText = stringResource(R.string.cryo_presets_error)
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .width(280.dp)
            .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp, opaque = true)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = stringResource(R.string.cryo_quick_themes_hint, presets.size),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
        )
        if (CryoPresets.loaded && presets.isEmpty()) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                text = stringResource(R.string.cryo_presets_empty),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .lazyScrollWithBar(listState),
                state = listState,
                contentPadding = PaddingValues(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    Box(modifier = Modifier.alpha(if (applying != null && applying != preset.id) 0.5f else 1f)) {
                        PresetTile(
                            preset = preset,
                            selected = applying == preset.id,
                            active = preset.id == activeId,
                            onClick = {
                                if (applying == null) {
                                    applying = preset.id
                                    scope.launch {
                                        val result = runCatching { CryoPresets.apply(preset, background) }
                                        applying = null
                                        Toast.makeText(context, if (result.isSuccess) appliedText else errorText, Toast.LENGTH_SHORT).show()
                                        if (result.isSuccess) onApplied()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        if (applying != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                text = stringResource(R.string.cryo_presets_applying),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
