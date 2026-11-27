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

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import com.russoviller.cryogenic.ui.components.CryoDropdownMenu as DropdownMenu
import com.russoviller.cryogenic.ui.components.CryoDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.filemanager.config.FmConfig
import com.russoviller.cryogenic.filemanager.ui.theme.fmSecondaryTextColor
import com.russoviller.cryogenic.filemanager.ui.theme.fmTopbarColors
import com.russoviller.cryogenic.filemanager.viewmodel.SortConfig

/** 文件管理器顶部应用栏 */
@Composable
fun FmAppBar(
    currentPath: String,
    folderCount: Int,
    fileCount: Int,
    selectedCount: Int,
    multiSelect: Boolean,
    clipboardPresent: Boolean,
    showHidden: Boolean,
    onJump: () -> Unit,
    onPaste: () -> Unit,
    onRefresh: () -> Unit,
    onImportFile: () -> Unit,
    onImportDir: () -> Unit,
    onBrowseExternal: () -> Unit,
    onSearch: () -> Unit,
    searchEnabled: Boolean = true,
    onSortChange: (SortConfig) -> Unit,
    sortConfig: SortConfig,
    onToggleHidden: () -> Unit,
    onOpenTrash: () -> Unit,
    onExit: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    TopAppBar(
        colors = fmTopbarColors(),
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJump() }
            ) {
                Text(
                    text = currentPath.trimEnd('/').substringAfterLast('/').ifEmpty { "/" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentPath,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val countsTexts = listOfNotNull(
                    stringResource(R.string.fm_count_folders, folderCount),
                    stringResource(R.string.fm_count_files, fileCount),
                    if (multiSelect && selectedCount > 0) {
                        stringResource(R.string.fm_count_selected, selectedCount)
                    } else null
                )
                AppBarSubTexts(
                    texts = countsTexts
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(com.russoviller.cryogenic.ui.components.CryoGlassColors.Deep.copy(alpha = 0.6f))
                    .border(1.dp, com.russoviller.cryogenic.ui.components.cryoBorderBrush(), CircleShape)
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_exit_to_app),
                    contentDescription = stringResource(R.string.fm_exit),
                    tint = com.russoviller.cryogenic.ui.components.CryoGlassColors.Ice
                )
            }
        }
    )
}

@Composable
fun AppBarSubTexts(
    texts: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.basicMarquee(Int.MAX_VALUE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        texts.forEach { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = fmSecondaryTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun SortSubmenu(
    sortConfig: SortConfig,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortChange: (SortConfig) -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(stringResource(R.string.sort_by))
        },
        trailingIcon = {
            val rotation by animateFloatAsState(
                if (expanded) 180f else 0f
            )
            Icon(
                modifier = Modifier.rotate(rotation),
                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                contentDescription = null
            )
        },
        onClick = { onExpandedChange(true) }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onExpandedChange(false) }
    ) {
        SortMenuItem(
            labelRes = R.string.fm_sort_name,
            candidate = sortConfig.copy(field = FmConfig.SortField.NAME),
            current = sortConfig,
            onSortChange = onSortChange
        )
        SortMenuItem(
            labelRes = R.string.fm_sort_size,
            candidate = sortConfig.copy(field = FmConfig.SortField.SIZE),
            current = sortConfig,
            onSortChange = onSortChange
        )
        SortMenuItem(
            labelRes = R.string.fm_sort_modified,
            candidate = sortConfig.copy(field = FmConfig.SortField.MODIFIED),
            current = sortConfig,
            onSortChange = onSortChange
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.fm_sort_ascending))
            },
            trailingIcon = {
                Row {
                    Spacer(Modifier.width(12.dp))
                    Checkbox(
                        checked = sortConfig.ascending,
                        onCheckedChange = { onSortChange(sortConfig.copy(ascending = it)) }
                    )
                }
            },
            onClick = { onSortChange(sortConfig.copy(ascending = !sortConfig.ascending)) }
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.fm_sort_folder_first))
            },
            trailingIcon = {
                Row {
                    Spacer(Modifier.width(12.dp))
                    Checkbox(
                        checked = sortConfig.folderFirst,
                        onCheckedChange = { onSortChange(sortConfig.copy(folderFirst = it)) }
                    )
                }
            },
            onClick = { onSortChange(sortConfig.copy(folderFirst = !sortConfig.folderFirst)) }
        )
    }
}

@Composable
private fun SortMenuItem(
    labelRes: Int,
    candidate: SortConfig,
    current: SortConfig,
    onSortChange: (SortConfig) -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(stringResource(labelRes))
        },
        trailingIcon = {
            if (candidate.field == current.field) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null
                )
            }
        },
        onClick = {
            onSortChange(candidate)
        }
    )
}
