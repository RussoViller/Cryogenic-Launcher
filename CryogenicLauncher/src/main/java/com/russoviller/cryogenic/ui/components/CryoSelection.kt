package com.russoviller.cryogenic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R

val CryoDanger = Color(0xFFFF6B8A)

@Composable
fun BoxScope.CryoSelectableOverlay(
    selecting: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    cornerRadius: Int = 16
) {
    if (!selecting) return
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .background(if (selected) CryoGlassColors.Ice.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.18f))
            .border(if (selected) 2.dp else 1.dp, if (selected) CryoGlassColors.Ice else CryoGlassColors.Ice.copy(alpha = 0.3f), shape)
            .clickable(onClick = onToggle)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(28.dp)
                .clip(CircleShape)
                .cryoBackground(if (selected) cryoGradient() else Brush.linearGradient(listOf(CryoGlassColors.Deep, CryoGlassColors.Deep)))
                .border(1.5.dp, CryoGlassColors.Ice, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    modifier = Modifier.size(17.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
        }
    }
}

@Composable
fun CryoSelectionBar(
    visible: Boolean,
    count: Int,
    total: Int,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.18f), CryoGlassColors.Navy.copy(alpha = 0.6f))))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), RoundedCornerShape(50))
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cryo_selection_count, count, total),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
            SelectionPill(R.drawable.ic_select_all, stringResource(R.string.cryo_selection_all), CryoGlassColors.Ice, onSelectAll)
            SelectionPill(R.drawable.ic_delete_outlined, stringResource(R.string.cryo_selection_delete, count), CryoDanger, onDelete, enabled = count > 0)
            SelectionPill(R.drawable.ic_close, stringResource(R.string.generic_cancel), CryoGlassColors.Frost, onCancel)
        }
    }
}

@Composable
private fun SelectionPill(iconRes: Int, text: String, accent: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = if (enabled) 0.14f else 0.05f))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.6f else 0.2f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accent.copy(alpha = if (enabled) 1f else 0.4f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(shadow = null),
            fontWeight = FontWeight.Bold,
            color = accent.copy(alpha = if (enabled) 1f else 0.4f),
            maxLines = 1
        )
    }
}
