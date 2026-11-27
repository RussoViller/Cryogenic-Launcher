package com.russoviller.cryogenic.ui.screens.content.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoIdleMode
import com.russoviller.cryogenic.ui.components.CryoOutlinedButton
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGradient

/**
 * Cartão em Aparência que abre a tela de configurações do modo inativo.
 */
@Composable
fun IdleModeCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val enabled = AllSettings.cryoIdleAutoEnabled.state
    val seconds = CryoIdleMode.clampSeconds(AllSettings.cryoIdleTimeoutSeconds.state)
    Row(
        modifier = modifier
            .fillMaxWidth()
            //Segue a opacidade da interface, igual aos outros cartões dos Ajustes
            .cryoGlass(shape = RoundedCornerShape(20.dp), cornerRadius = 20.dp)
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(R.drawable.ic_visibility_outlined),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.cryo_idle_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                )
            )
            Text(
                text = stringResource(R.string.cryo_idle_card_summary),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
            )
        }
        Text(
            text = if (enabled) CryoIdleMode.format(seconds) else stringResource(R.string.cryo_idle_card_off),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = if (enabled) CryoGlassColors.Ice else CryoGlassColors.Frost.copy(alpha = 0.6f)
        )
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            tint = CryoGlassColors.Frost
        )
    }
}

/**
 * Janela do modo inativo. Fundo opaco, igual ao Estúdio de temas e ao Estúdio de mouses:
 * a opacidade do fundo do Launcher não mexe nela.
 */
@Composable
fun IdleModeDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.russoviller.cryogenic.ui.components.LocalCryoGlassFixed provides true
        ) {
        Column(
            modifier = Modifier
                .padding(all = 12.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
                //Segue a opacidade da interface escolhida pelo usuário
                .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
                .verticalScroll(rememberScrollState())
                .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp),
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
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_visibility_outlined),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.cryo_idle_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, cryoBorderBrush(), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.generic_close),
                        tint = CryoGlassColors.Frost
                    )
                }
            }
            IdleTimeoutEditor()
        }
        }
    }
}

@Composable
private fun IdleTimeoutEditor() {
    val enabled = AllSettings.cryoIdleAutoEnabled.state
    val total = CryoIdleMode.clampSeconds(AllSettings.cryoIdleTimeoutSeconds.state)
    // Muda a cada gravação para as caixinhas voltarem a mostrar o valor real (já limitado).
    var revision by remember { mutableIntStateOf(0) }

    fun save(seconds: Int) {
        AllSettings.cryoIdleTimeoutSeconds.save(CryoIdleMode.clampSeconds(seconds))
        revision++
    }

    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.cryo_idle_timeout_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdleTimeBox(
                value = hours,
                revision = revision,
                enabled = enabled,
                label = stringResource(R.string.cryo_idle_hours),
                onCommit = { save(it * 3600 + minutes * 60 + secs) }
            )
            IdleTimeSeparator()
            IdleTimeBox(
                value = minutes,
                revision = revision,
                enabled = enabled,
                label = stringResource(R.string.cryo_idle_minutes),
                onCommit = { save(hours * 3600 + it * 60 + secs) }
            )
            IdleTimeSeparator()
            IdleTimeBox(
                value = secs,
                revision = revision,
                enabled = enabled,
                label = stringResource(R.string.cryo_idle_seconds),
                onCommit = { save(hours * 3600 + minutes * 60 + it) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_warning_outlined),
                contentDescription = null,
                tint = Color(0xFFFFC94D)
            )
            Text(
                text = stringResource(R.string.cryo_idle_limits),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFE08A)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CryoOutlinedButton(
                enabled = enabled && total > CryoIdleMode.MIN_SECONDS,
                onClick = { save(total - 60) }
            ) {
                Text(text = stringResource(R.string.cryo_idle_minus))
            }
            CryoOutlinedButton(
                enabled = enabled && total < CryoIdleMode.MAX_SECONDS,
                onClick = { save(total + 60) }
            ) {
                Text(text = stringResource(R.string.cryo_idle_plus))
            }
            CryoTonalButton(
                enabled = enabled && total < CryoIdleMode.MAX_SECONDS,
                onClick = { save(CryoIdleMode.MAX_SECONDS) }
            ) {
                Text(text = stringResource(R.string.cryo_idle_max))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.5f), Color.Transparent)))
        )

        if (enabled) {
            CryoOutlinedButton(
                onClick = { AllSettings.cryoIdleAutoEnabled.save(false) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_visibility_off_outlined),
                    contentDescription = null
                )
                Text(text = stringResource(R.string.cryo_idle_disable))
            }
        } else {
            CryoButton(
                onClick = { AllSettings.cryoIdleAutoEnabled.save(true) }
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_visibility_outlined),
                    contentDescription = null
                )
                Text(text = stringResource(R.string.cryo_idle_enable))
            }
        }
        Text(
            text = stringResource(if (enabled) R.string.cryo_idle_enabled_note else R.string.cryo_idle_disabled_note),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun IdleTimeSeparator() {
    Box(
        modifier = Modifier.height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice
        )
    }
}

/**
 * Quadradinho de tempo (horas, minutos ou segundos). Aceita até 2 dígitos; o valor é gravado
 * quando o usuário confirma no teclado ou sai do campo, e sempre respeita o mínimo e o máximo.
 */
@Composable
private fun IdleTimeBox(
    value: Int,
    revision: Int,
    enabled: Boolean,
    label: String,
    onCommit: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var text by remember(value, revision) { mutableStateOf("%02d".format(value)) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    fun commit() {
        val typed = text.toIntOrNull() ?: 0
        if (typed != value || text.length != 2) onCommit(typed)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 56.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(CryoGlassColors.Ice.copy(alpha = 0.16f), CryoGlassColors.Deep.copy(alpha = 0.85f))
                    )
                )
                .border(if (focused) 2.dp else 1.dp, cryoBorderBrush(if (focused) 1.3f else 1f), shape),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                modifier = Modifier
                    .width(72.dp)
                    .onFocusChanged { state ->
                        if (focused && !state.isFocused) commit()
                        focused = state.isFocused
                    },
                value = text,
                onValueChange = { input -> text = input.filter(Char::isDigit).take(2) },
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                ),
                cursorBrush = SolidColor(CryoGlassColors.Ice),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    commit()
                    focusManager.clearFocus()
                })
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(0.dp))
    }
}
