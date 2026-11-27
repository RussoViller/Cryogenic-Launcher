package com.russoviller.cryogenic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/*
 * Menu de opções (3 pontinhos, ordenar, etc.) no estilo do Cryogenic Launcher.
 * Mesma assinatura do DropdownMenu/DropdownMenuItem do Material 3, para trocar só o import.
 * O posicionamento e a animação continuam sendo do Material (popup); o visual é próprio.
 */

private val MenuShape = RoundedCornerShape(22.dp)
private val ItemShape = RoundedCornerShape(14.dp)
private val DangerColor = Color(0xFFFF6B6B)

@Composable
fun CryoDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    @Suppress("UNUSED_PARAMETER") shape: Shape = MenuShape,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") tonalElevation: Dp = 0.dp,
    @Suppress("UNUSED_PARAMETER") shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = MenuShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        CryoMenuPanel(content = content)
    }
}

/** Fundo do menu: vidro escuro do tema, borda em degradê e faixa de destaque em cima. */
@Composable
fun CryoMenuPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .widthIn(min = 210.dp, max = 340.dp)
            .clip(MenuShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        CryoGlassColors.Navy.copy(alpha = 0.97f),
                        CryoGlassColors.Deep.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        CryoGlassColors.Ice.copy(alpha = 0.75f),
                        CryoGlassColors.Aurora.copy(alpha = 0.25f),
                        CryoGlassColors.Ice.copy(alpha = 0.45f)
                    )
                ),
                shape = MenuShape
            )
    ) {
        //faixa de destaque
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            CryoGlassColors.Ice.copy(alpha = 0.9f),
                            CryoGlassColors.Aurora.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(modifier = Modifier.padding(6.dp)) {
            content()
        }
    }
}

@Composable
fun CryoDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    @Suppress("UNUSED_PARAMETER") colors: MenuItemColors? = null,
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val accent = if (destructive) DangerColor else CryoGlassColors.Ice
    val textColor = when {
        !enabled -> Color.White.copy(alpha = 0.38f)
        destructive -> DangerColor
        else -> Color.White
    }
    val bg by animateColorAsState(
        if (pressed) accent.copy(alpha = 0.22f) else Color.Transparent,
        label = "cryo_menu_item_bg"
    )
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "cryo_menu_item_scale")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .scale(scale)
            .clip(ItemShape)
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = source,
                indication = androidx.compose.material3.ripple(color = accent),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = if (enabled) 0.26f else 0.10f),
                                (if (destructive) DangerColor else CryoGlassColors.Aurora).copy(alpha = if (enabled) 0.12f else 0.05f)
                            )
                        )
                    )
                    .border(1.dp, accent.copy(alpha = if (enabled) 0.45f else 0.15f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides if (enabled) accent else Color.White.copy(alpha = 0.3f)
                ) {
                    leadingIcon()
                }
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(6.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalContentColor provides textColor) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = textColor)
                ) {
                    text()
                }
            }
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            CompositionLocalProvider(
                LocalContentColor provides if (enabled) CryoGlassColors.Frost.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f)
            ) {
                trailingIcon()
            }
        }
    }
}

/** Separador fino entre grupos de opções. */
@Composable
fun CryoMenuDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, CryoGlassColors.Ice.copy(alpha = 0.35f), Color.Transparent)
                )
            )
    )
}
