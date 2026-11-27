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

package com.russoviller.cryogenic.filemanager.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.filemanager.logic.entry.FmEntry
import com.russoviller.cryogenic.ui.components.CryoDanger
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoFillFactor
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.utils.file.formatFileSize
import com.russoviller.cryogenic.utils.formatDate

fun fmEntryBadge(entry: FmEntry): String? {
    if (entry.isDirectory) return null
    val dot = entry.name.lastIndexOf('.')
    if (dot <= 0 || dot == entry.name.length - 1) return null
    return entry.name.substring(dot + 1).take(5).uppercase()
}

@Composable
fun FmEntryItem(
    entry: FmEntry,
    multiSelect: Boolean,
    selected: Boolean,
    cutMarked: Boolean,
    highlighted: Boolean = false,
    position: FmCardPosition = FmCardPosition.Single,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onSwipeTrigger: () -> Unit = {},
    onEdit: () -> Unit = {},
    onRename: () -> Unit = {},
    onProperty: () -> Unit = {},
    onExtract: () -> Unit = {},
    onShare: () -> Unit = {},
    onCompress: () -> Unit = {},
    onCopy: () -> Unit = {},
    onCut: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val swipeState = rememberFmSwipeTriggerState()
    val active = selected || highlighted
    val contentAlpha by animateFloatAsState(if (cutMarked) 0.55f else 1f)
    val shape = cryoChamferShape(14.dp)
    val fill = cryoFillFactor().coerceAtMost(1f)
    val accent = if (entry.isDirectory) CryoGlassColors.Ice else CryoGlassColors.Aurora

    Box(
        modifier = modifier
            .zIndex(if (swipeState.isDragging) 1f else 0f)
            .fillMaxWidth()
            .fmSwipeTrigger(
                state = swipeState,
                triggerable = true,
                onTriggered = onSwipeTrigger
            )
            .then(if (active) Modifier.cryoGlow(14.dp, selected = true) else Modifier)
            .shadow(6.dp * swipeState.dragFraction, shape)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        (if (active) CryoGlassColors.IceSoft.copy(alpha = 0.32f) else CryoGlassColors.Navy.copy(alpha = 0.45f + 0.4f * fill)),
                        CryoGlassColors.Deep.copy(alpha = 0.55f + 0.4f * fill)
                    )
                )
            )
            .border(if (active) 1.5.dp else 1.dp, if (active) cryoBorderBrush(1.4f) else cryoBorderBrush(0.55f), shape)
            .combinedClickable(
                onClick = {
                    if (multiSelect) onClick()
                    else if (entry.isFile) menuExpanded = true
                    else onClick()
                },
                onLongClick = {
                    if (multiSelect) onLongClick() else menuExpanded = true
                }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(34.dp)
                .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.1f))))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FmIcons.IconFor(
                modifier = Modifier.alpha(contentAlpha),
                name = entry.name,
                isDirectory = entry.isDirectory,
            )
            if (!multiSelect) {
                FmEntryMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    entry = entry,
                    onToggleFavorite = { toggleFavorite(context, entry) },
                    onEdit = onEdit,
                    onRename = onRename,
                    onProperty = onProperty,
                    onExtract = onExtract,
                    onShare = onShare,
                    onCompress = onCompress,
                    onCopy = onCopy,
                    onCut = onCut,
                    onDelete = onDelete
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                EntrySubtitle(
                    modifier = Modifier.fillMaxWidth(),
                    entry = entry
                )
            }
            val badge = fmEntryBadge(entry)
            Text(
                modifier = Modifier
                    .alpha(contentAlpha)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                text = badge ?: stringResource(R.string.cryo_fm_badge_folder),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
            if (entry.isDirectory && !multiSelect) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CryoGlassColors.Ice.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                        contentDescription = null,
                        tint = CryoGlassColors.Ice
                    )
                }
            }
        }
    }
}

@Composable
private fun EntrySubtitle(
    entry: FmEntry,
    modifier: Modifier = Modifier
) {
    val parts = buildList {
        if (!entry.isDirectory) add(formatFileSize(entry.size))
        add(formatDate(entry.modifiedMs))
    }
    Text(
        modifier = modifier.basicMarquee(Int.MAX_VALUE),
        text = parts.joinToString("  ·  "),
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = CryoGlassColors.Frost.copy(alpha = 0.7f),
        maxLines = 1
    )
}

private data class FmMenuAction(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val action: () -> Unit,
    val danger: Boolean = false
)

@Composable
private fun FmEntryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entry: FmEntry,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onProperty: () -> Unit,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    val favorite = entry.isDirectory && com.russoviller.cryogenic.filemanager.config.FmFavorites.paths.contains(entry.path.toAbsolutePath().toString())
    val actions = buildList {
        if (entry.isDirectory) {
            add(
                FmMenuAction(
                    if (favorite) R.string.cryo_fm_unfavorite else R.string.cryo_fm_favorite,
                    if (favorite) R.drawable.ic_star_filled else R.drawable.ic_star_outlined,
                    onToggleFavorite
                )
            )
        }
        if (entry.isFile && entry.archiveType == null) add(FmMenuAction(R.string.generic_edit, R.drawable.ic_edit_outlined, onEdit))
        add(FmMenuAction(R.string.generic_rename, R.drawable.ic_edit_filled, onRename))
        add(FmMenuAction(R.string.fm_archive, R.drawable.ic_archive_filled, onCompress))
        if (entry.archiveType != null) add(FmMenuAction(R.string.fm_extract, R.drawable.ic_unarchive_filled, onExtract))
        add(FmMenuAction(R.string.generic_copy, R.drawable.ic_content_copy_filled, onCopy))
        add(FmMenuAction(R.string.fm_cut, R.drawable.ic_drive_file_move_filled, onCut))
        add(FmMenuAction(R.string.generic_share, R.drawable.ic_share_filled, onShare))
        add(FmMenuAction(R.string.fm_property, R.drawable.ic_info_filled, onProperty))
        add(FmMenuAction(R.string.generic_delete, R.drawable.ic_delete_filled, onDelete, danger = true))
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 360.dp)
                .padding(horizontal = 6.dp)
                .cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp, opaque = true)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FmIcons.IconFor(name = entry.name, isDirectory = entry.isDirectory)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_fm_actions_kicker),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                        color = CryoGlassColors.Ice
                    )
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    EntrySubtitle(entry = entry)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, CryoGlassColors.Ice.copy(alpha = 0.6f), Color.Transparent)))
            )
            actions.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { item ->
                        MenuTile(item = item, onDismiss = onDismiss)
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.MenuTile(item: FmMenuAction, onDismiss: () -> Unit) {
    val tint = if (item.danger) CryoDanger else CryoGlassColors.Ice
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.14f), CryoGlassColors.Deep.copy(alpha = 0.6f))))
            .border(1.dp, tint.copy(alpha = 0.4f), shape)
            .clickable {
                onDismiss()
                item.action()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .then(
                    if (item.danger) Modifier.background(CryoDanger.copy(alpha = 0.25f))
                    else Modifier.cryoBackground(cryoGradient())
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(19.dp),
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = if (item.danger) CryoDanger else CryoGlassColors.Deep
            )
        }
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (item.danger) CryoDanger else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Favorita ou desfavorita a pasta (máximo de 5). */
private fun toggleFavorite(context: android.content.Context, entry: FmEntry) {
    val favorites = com.russoviller.cryogenic.filemanager.config.FmFavorites
    val message = when (favorites.toggle(entry.path.toAbsolutePath().toString())) {
        com.russoviller.cryogenic.filemanager.config.FmFavorites.Result.ADDED -> R.string.cryo_fm_favorite_added
        com.russoviller.cryogenic.filemanager.config.FmFavorites.Result.REMOVED -> R.string.cryo_fm_favorite_removed
        com.russoviller.cryogenic.filemanager.config.FmFavorites.Result.FULL -> R.string.cryo_fm_favorite_full
    }
    android.widget.Toast.makeText(context, context.getString(message, com.russoviller.cryogenic.filemanager.config.FmFavorites.MAX), android.widget.Toast.LENGTH_SHORT).show()
}
