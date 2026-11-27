package com.russoviller.cryogenic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class CryoButtonKind { Primary, Tonal, Outlined }

private val CryoButtonShape = RoundedCornerShape(14.dp)

@Composable
private fun CryoButtonBase(
    kind: CryoButtonKind,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape?,
    colors: ButtonColors?,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    content: @Composable RowScope.() -> Unit
) {
    val buttonShape = shape ?: CryoButtonShape
    val custom = colors?.containerColor?.takeIf { it != Color.Unspecified }
    val fillFactor = cryoFillFactor().coerceAtMost(1f)
    val fill: Brush = when {
        custom != null -> Brush.linearGradient(listOf(custom, custom))
        kind == CryoButtonKind.Primary -> cryoGradient()
        kind == CryoButtonKind.Tonal -> Brush.verticalGradient(
            listOf(
                CryoGlassColors.Ice.copy(alpha = 0.10f + 0.12f * fillFactor),
                CryoGlassColors.Navy.copy(alpha = 0.35f + 0.35f * fillFactor)
            )
        )
        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val contentColor = when {
        custom != null -> colors.contentColor
        kind == CryoButtonKind.Primary -> CryoGlassColors.Deep
        else -> CryoGlassColors.Frost
    }
    val borderBrush = border?.brush ?: if (kind == CryoButtonKind.Primary) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f)))
    } else cryoBorderBrush()
    val baseStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge)
    val textStyle = if (kind == CryoButtonKind.Primary || custom != null) baseStyle.copy(shadow = null) else baseStyle
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .cryoGlow(14.dp, selected = kind == CryoButtonKind.Primary, strength = 0.9f)
            .clip(buttonShape)
            .cryoBackground(fill)
            .border(if (kind == CryoButtonKind.Primary) 1.5.dp else 1.dp, borderBrush, buttonShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides textStyle.copy(fontWeight = FontWeight.Bold, color = contentColor)
        ) {
            content()
        }
    }
}

@Composable
fun CryoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = CryoButtonBase(CryoButtonKind.Primary, onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, content)

@Composable
fun CryoTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = CryoButtonBase(CryoButtonKind.Tonal, onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, content)

@Composable
fun CryoOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = CryoButtonBase(CryoButtonKind.Outlined, onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, content)
