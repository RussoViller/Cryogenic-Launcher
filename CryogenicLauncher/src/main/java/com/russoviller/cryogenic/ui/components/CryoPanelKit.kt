package com.russoviller.cryogenic.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Peças do visual dos painéis do Cryogenic (menu do jogo, editor do HUD...):
 * cartões arredondados de vidro escuro, ícone em círculo com degradê, títulos com subtítulo.
 */

private val tileShape = RoundedCornerShape(18.dp)

/** Fundo dos cartões dos painéis. */
@Composable
fun Modifier.cryoTile(active: Boolean = false, accent: Color = CryoGlassColors.Ice): Modifier {
    if (cryoLiquid) {
        //Liquid Glass: vidro claro com brilho em cima e aro branco
        return this
            .clip(tileShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (active) 0.26f else 0.14f),
                        accent.copy(alpha = if (active) 0.20f else 0.05f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(if (active) 1.5.dp else 1.dp, cryoLiquidRim(if (active) 1.1f else 0.7f), tileShape)
    }
    return this
        .clip(tileShape)
        .background(
            Brush.linearGradient(
                listOf(
                    accent.copy(alpha = if (active) 0.22f else 0.08f),
                    CryoGlassColors.Navy.copy(alpha = 0.45f),
                    CryoGlassColors.Deep.copy(alpha = 0.65f)
                )
            )
        )
        .border(
            if (active) 1.5.dp else 1.dp,
            Brush.linearGradient(
                listOf(accent.copy(alpha = if (active) 0.95f else 0.35f), accent.copy(alpha = 0.06f), CryoGlassColors.Aurora.copy(alpha = if (active) 0.6f else 0.2f))
            ),
            tileShape
        )
}

/** Título de seção: texto pequeno espaçado com linha de destaque. */
@Composable
fun CryoSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, shadow = null),
            fontWeight = FontWeight.Bold,
            color = CryoGlassColors.Ice
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.5f), Color.Transparent)))
        )
    }
}

/** Ícone em círculo com degradê. */
@Composable
fun CryoIconOrb(iconRes: Int, modifier: Modifier = Modifier, size: Int = 40, active: Boolean = true, accent: Color? = null) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .then(
                when {
                    accent != null -> Modifier.background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f))))
                    active -> Modifier.cryoBackground(cryoGradient())
                    else -> Modifier
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.35f), CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size((size * 0.5f).dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active || accent != null) CryoGlassColors.Deep else CryoGlassColors.Ice
        )
    }
}

/** Cartão de ação (atalho): ícone, título, subtítulo e seta. */
@Composable
fun CryoActionTile(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .cryoTile()
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CryoIconOrb(iconRes, size = 36)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(text = "›", style = MaterialTheme.typography.titleLarge, color = CryoGlassColors.Ice, fontWeight = FontWeight.Black)
    }
}

/** Cartão com chave liga/desliga. */
@Composable
fun CryoToggleTile(
    iconRes: Int?,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .cryoTile(active = checked && enabled)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .heightIn(min = 60.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        iconRes?.let { CryoIconOrb(it, active = checked) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DefaultSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** Cartão com controle deslizante e valor em destaque. */
@Composable
fun CryoSliderTile(
    iconRes: Int?,
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = ""
) {
    Column(
        modifier = modifier
            .cryoTile()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            iconRes?.let { CryoIconOrb(it, size = 34, active = false) }
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .cryoBackground(cryoGradient())
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                text = "$value$suffix",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, shadow = null),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Deep
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = CryoGlassColors.Ice,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

/** Abas em formato de pílula com a seleção deslizando. */
@Composable
fun CryoSegmentedTabs(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (titles.isEmpty()) return
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(CryoGlassColors.Deep.copy(alpha = 0.6f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), CircleShape)
            .padding(4.dp)
    ) {
        val segment = maxWidth / titles.size
        val offset by animateDpAsState(segment * selected.coerceIn(0, titles.lastIndex), label = "tab")
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(CircleShape)
                .cryoBackground(cryoGradient())
        )
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            titles.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    MarqueeText(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(shadow = null, textAlign = TextAlign.Center),
                        color = if (index == selected) CryoGlassColors.Deep else CryoGlassColors.Frost
                    )
                }
            }
        }
    }
}

/** Botão pequeno em pílula (ações secundárias). */
@Composable
fun CryoChipButton(text: String, modifier: Modifier = Modifier, iconRes: Int? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(CryoGlassColors.Ice.copy(alpha = 0.10f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.55f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        iconRes?.let {
            Icon(modifier = Modifier.size(15.dp), painter = painterResource(it), contentDescription = null, tint = CryoGlassColors.Ice)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(shadow = null),
            fontWeight = FontWeight.Bold,
            color = CryoGlassColors.Ice,
            maxLines = 1
        )
    }
}
