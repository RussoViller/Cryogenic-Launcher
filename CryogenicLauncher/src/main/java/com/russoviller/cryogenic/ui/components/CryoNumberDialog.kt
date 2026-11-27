package com.russoviller.cryogenic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import java.util.Locale

/**
 * Janela para digitar um número (usada por todos os controles deslizantes):
 * visor grande, botões − / +, atalhos de mínimo / meio / máximo e uma barra mostrando
 * onde o valor fica dentro do intervalo.
 */
@Composable
fun CryoNumberDialog(
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    isInt: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    fun format(v: Float): String =
        if (isInt) v.toInt().toString()
        else String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')

    var field by remember {
        val text = format(value)
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val parsed = (if (isInt) field.text.toIntOrNull()?.toFloat() else field.text.replace(',', '.').toFloatOrNull())
    val numberFormatError = stringResource(R.string.generic_input_failed_to_number)
    val numberTooSmallError = stringResource(R.string.generic_input_too_small, valueRange.start.toInt())
    val numberTooLargeError = stringResource(R.string.generic_input_too_large, valueRange.endInclusive.toInt())
    val error = when {
        parsed == null -> numberFormatError
        parsed < valueRange.start -> numberTooSmallError
        parsed > valueRange.endInclusive -> numberTooLargeError
        else -> null
    }
    val step = if (isInt) 1f else 0.1f
    fun set(v: Float) {
        val text = format(v.coerceIn(valueRange.start, valueRange.endInclusive))
        field = TextFieldValue(text, selection = TextRange(text.length))
    }
    val confirm = {
        if (error == null && parsed != null) {
            onConfirm(parsed)
            onDismissRequest()
        }
    }
    val ice = CryoGlassColors.Ice
    val aurora = CryoGlassColors.Aurora
    val danger = Color(0xFFFF5C7A)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(0.9f)
                    .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                //Título com marcador
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 20.dp)
                            .background(Brush.verticalGradient(listOf(ice, aurora)))
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2
                    )
                    Text(
                        text = "${format(valueRange.start)} – ${format(valueRange.endInclusive)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CryoGlassColors.Frost
                    )
                }

                //Visor: − [ número ] +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepButton(R.drawable.ic_remove, enabled = (parsed ?: value) > valueRange.start) {
                        set((parsed ?: value) - step)
                    }
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(shape)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), CryoGlassColors.Deep.copy(alpha = 0.7f))))
                            .border(
                                1.5.dp,
                                if (error != null) SolidColor(danger)
                                else Brush.horizontalGradient(listOf(ice, aurora)),
                                shape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .focusRequester(focus),
                            value = field,
                            onValueChange = { new ->
                                //Só números (e separador decimal quando não é inteiro)
                                val ok = new.text.all { it.isDigit() || it == '-' || (!isInt && (it == '.' || it == ',')) }
                                if (ok && new.text.length <= 12) field = new
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                color = if (error != null) danger else Color.White
                            ),
                            cursorBrush = SolidColor(ice),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { confirm() })
                        )
                    }
                    StepButton(R.drawable.ic_add, enabled = (parsed ?: value) < valueRange.endInclusive) {
                        set((parsed ?: value) + step)
                    }
                }

                //Barra do intervalo com a posição do valor
                val fraction = ((parsed ?: value) - valueRange.start) /
                    (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (error != null) SolidColor(danger) else Brush.horizontalGradient(listOf(ice, aurora)))
                    )
                }

                //Atalhos: mínimo, meio e máximo
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mid = valueRange.start + (valueRange.endInclusive - valueRange.start) / 2f
                    listOf(valueRange.start, if (isInt) kotlin.math.round(mid) else mid, valueRange.endInclusive).forEach { preset ->
                        val selected = parsed == preset
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (selected) Modifier.cryoBackground(cryoGradient())
                                    else Modifier
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, ice.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                )
                                .clickable { set(preset) }
                                .padding(vertical = 7.dp),
                            text = format(preset),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, shadow = null),
                            fontWeight = FontWeight.Black,
                            color = if (selected) CryoGlassColors.Deep else Color.White
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = danger
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest
                    ) {
                        MarqueeText(text = stringResource(R.string.generic_cancel))
                    }
                    CryoButton(
                        modifier = Modifier.weight(1f),
                        enabled = error == null,
                        onClick = confirm
                    ) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.03f))
            .border(1.5.dp, cryoBorderBrush(), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) CryoGlassColors.Ice else CryoGlassColors.Frost.copy(alpha = 0.4f)
        )
    }
}
