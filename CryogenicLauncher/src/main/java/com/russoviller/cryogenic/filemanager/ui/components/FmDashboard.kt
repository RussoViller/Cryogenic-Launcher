package com.russoviller.cryogenic.filemanager.ui.components

import com.russoviller.cryogenic.filemanager.config.FmConfig
import com.russoviller.cryogenic.filemanager.viewmodel.SortConfig
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.util.Locale

private data class FmStorageInfo(val total: Long, val free: Long)

private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
        value /= 1024
        i++
    }
    return String.format(Locale.ROOT, if (i == 0) "%.0f %s" else "%.2f %s", value, units[i])
}

@Composable
fun FmDashboard(
    rootDir: Path?,
    currentDir: Path?,
    multiSelect: Boolean,
    canBack: Boolean,
    canForward: Boolean,
    canParent: Boolean,
    clipboardPresent: Boolean,
    onNavigate: (Path) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onNew: () -> Unit,
    onParent: () -> Unit,
    onPaste: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTrash: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRangeSelectHelp: () -> Unit,
    showHidden: Boolean,
    searchEnabled: Boolean,
    onImportFile: () -> Unit,
    onImportDir: () -> Unit,
    onBrowseExternal: () -> Unit,
    onOpenSort: () -> Unit,
    onToggleHidden: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenWallpaper: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val storage by produceState<FmStorageInfo?>(null, rootDir, currentDir) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val stat = StatFs((rootDir ?: return@runCatching null).toString())
                FmStorageInfo(stat.totalBytes, stat.availableBytes)
            }.getOrNull()
        }
    }
    val shortcuts by produceState<List<Path>>(emptyList(), rootDir) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val root = rootDir?.toFile() ?: return@runCatching emptyList()
                root.listFiles()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.sortedBy { it.name.lowercase() }
                    ?.take(8)
                    ?.map(File::toPath)
                    .orEmpty()
            }.getOrDefault(emptyList())
        }
    }

    Column(
        modifier = modifier
            .width(236.dp)
            .fillMaxHeight()
            .padding(start = 10.dp, top = 8.dp, bottom = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StorageCard(storage)

        DashSection(title = stringResource(R.string.cryo_fm_locations)) {
            rootDir?.let { root ->
                LocationRow(
                    icon = painterResource(R.drawable.ic_home_filled),
                    label = stringResource(R.string.cryo_fm_root),
                    selected = currentDir == root,
                    onClick = { onNavigate(root) }
                )
            }
            shortcuts.forEach { path ->
                LocationRow(
                    icon = painterResource(R.drawable.ic_folder_filled),
                    label = path.fileName?.toString() ?: path.toString(),
                    selected = currentDir == path,
                    onClick = { onNavigate(path) }
                )
            }
        }

        //pastas favoritas (segure uma pasta e toque em Favoritar)
        val favoritePaths = com.russoviller.cryogenic.filemanager.config.FmFavorites.paths
        val favorites by produceState<List<Path>>(emptyList(), favoritePaths) {
            value = withContext(Dispatchers.IO) {
                favoritePaths.map(::File).filter { it.isDirectory }.map(File::toPath)
            }
        }
        if (favorites.isNotEmpty()) {
            DashSection(title = stringResource(R.string.cryo_fm_favorites, favorites.size, com.russoviller.cryogenic.filemanager.config.FmFavorites.MAX)) {
                favorites.forEach { path ->
                    LocationRow(
                        icon = painterResource(R.drawable.ic_star_filled),
                        label = path.fileName?.toString() ?: path.toString(),
                        selected = currentDir == path,
                        onClick = { onNavigate(path) }
                    )
                }
            }
        }

        DashSection(title = stringResource(R.string.cryo_fm_tools)) {
            FmToolsGrid(
                multiSelect = multiSelect,
                canBack = canBack,
                canForward = canForward,
                canParent = canParent,
                clipboardPresent = clipboardPresent,
                showHidden = showHidden,
                searchEnabled = searchEnabled,
                onBack = onBack,
                onForward = onForward,
                onNew = onNew,
                onParent = onParent,
                onPaste = onPaste,
                onSearch = onSearch,
                onRefresh = onRefresh,
                onOpenTrash = onOpenTrash,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onRangeSelectHelp = onRangeSelectHelp,
                onImportFile = onImportFile,
                onImportDir = onImportDir,
                onBrowseExternal = onBrowseExternal,
                onOpenSort = onOpenSort,
                onToggleHidden = onToggleHidden,
                onToggleOrientation = onToggleOrientation,
                onOpenWallpaper = onOpenWallpaper
            )
        }
    }
}

@Composable
fun FmToolsGrid(
    multiSelect: Boolean,
    canBack: Boolean,
    canForward: Boolean,
    canParent: Boolean,
    clipboardPresent: Boolean,
    showHidden: Boolean,
    searchEnabled: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onNew: () -> Unit,
    onParent: () -> Unit,
    onPaste: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTrash: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRangeSelectHelp: () -> Unit,
    onImportFile: () -> Unit,
    onImportDir: () -> Unit,
    onBrowseExternal: () -> Unit,
    onOpenSort: () -> Unit,
    onToggleHidden: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenWallpaper: () -> Unit = {},
    columns: Int = 3
) {
    val tools = if (multiSelect) {
        listOf(
            Tool(R.drawable.ic_select_all, R.string.fm_select_all, onSelectAll),
            Tool(R.drawable.ic_deselect, R.string.fm_clear_selection, onClearSelection),
            Tool(R.drawable.ic_swipe, R.string.fm_range_select_help, onRangeSelectHelp)
        )
    } else {
        listOf(
            Tool(R.drawable.ic_keyboard_arrow_left, R.string.fm_nav_back, onBack, canBack),
            Tool(R.drawable.ic_keyboard_arrow_right, R.string.fm_nav_forward, onForward, canForward),
            Tool(R.drawable.ic_arrow_upward, R.string.fm_nav_parent, onParent, canParent),
            Tool(R.drawable.ic_add, R.string.cryo_fm_new, onNew),
            Tool(R.drawable.ic_assignment_filled, R.string.generic_paste, onPaste, clipboardPresent),
            Tool(R.drawable.ic_search, R.string.generic_search, onSearch, searchEnabled),
            Tool(R.drawable.ic_refresh, R.string.cryo_fm_refresh, onRefresh),
            Tool(R.drawable.ic_text_snippet_filled, R.string.cryo_fm_import_file, onImportFile),
            Tool(R.drawable.ic_create_new_folder_filled, R.string.cryo_fm_import_dir, onImportDir),
            Tool(R.drawable.ic_document_search_filled, R.string.cryo_fm_external, onBrowseExternal),
            Tool(R.drawable.ic_sort, R.string.cryo_fm_sort, onOpenSort),
            Tool(
                if (showHidden) R.drawable.ic_visibility_outlined else R.drawable.ic_visibility_off_outlined,
                R.string.cryo_fm_hidden,
                onToggleHidden,
                active = showHidden
            ),
            Tool(R.drawable.ic_delete_filled, R.string.fm_trash, onOpenTrash),
            Tool(R.drawable.ic_mobile_rotate_filled, R.string.cryo_fm_rotate, onToggleOrientation),
            Tool(R.drawable.ic_image_filled, R.string.cryo_fm_wallpaper_tool, onOpenWallpaper)
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tools.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tool ->
                    ToolTile(
                        modifier = Modifier.weight(1f),
                        tool = tool
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun FmSortDialog(
    sortConfig: SortConfig,
    onSortChange: (SortConfig) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp, opaque = true)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    FmConfig.SortField.NAME to R.string.fm_sort_name,
                    FmConfig.SortField.SIZE to R.string.fm_sort_size,
                    FmConfig.SortField.MODIFIED to R.string.fm_sort_modified
                ).forEach { (field, label) ->
                    val selected = sortConfig.field == field
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Aurora))
                                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                            )
                            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.35f), RoundedCornerShape(50))
                            .clickable { onSortChange(sortConfig.copy(field = field)) }
                            .padding(vertical = 8.dp),
                        text = stringResource(label),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) CryoGlassColors.Deep else Color.White,
                        maxLines = 1
                    )
                }
            }
            SortToggle(stringResource(R.string.fm_sort_ascending), sortConfig.ascending) {
                onSortChange(sortConfig.copy(ascending = it))
            }
            SortToggle(stringResource(R.string.fm_sort_folder_first), sortConfig.folderFirst) {
                onSortChange(sortConfig.copy(folderFirst = it))
            }
            com.russoviller.cryogenic.ui.components.CryoButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.generic_close))
            }
        }
    }
}

@Composable
private fun SortToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        com.russoviller.cryogenic.ui.components.DefaultSwitch(checked = checked, onCheckedChange = onChange)
    }
}

private class Tool(val icon: Int, val label: Int, val onClick: () -> Unit, val enabled: Boolean = true, val active: Boolean = false)

@Composable
private fun StorageCard(storage: FmStorageInfo?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoGlass()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Aurora))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_folder_filled),
                    contentDescription = null,
                    tint = CryoGlassColors.Deep
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cryo_fm_storage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = storage?.let {
                        stringResource(R.string.cryo_fm_storage_free, formatBytes(it.free))
                    } ?: stringResource(R.string.generic_loading),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
            }
        }
        val used = storage?.let { (it.total - it.free).coerceAtLeast(0) } ?: 0L
        val fraction = storage?.takeIf { it.total > 0 }?.let { used.toFloat() / it.total } ?: 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CryoGlassColors.Aurora,
                                CryoGlassColors.Ice,
                                if (fraction > 0.85f) Color(0xFFFF7A90) else CryoGlassColors.IceSoft
                            )
                        )
                    )
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.weight(1f),
                text = storage?.let { "${formatBytes(used)} / ${formatBytes(it.total)}" } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f),
                maxLines = 1
            )
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice
            )
        }
    }
}

@Composable
private fun DashSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cryoGlass(glow = false)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice
        )
        content()
    }
}

@Composable
private fun LocationRow(icon: Painter, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft.copy(alpha = 0.55f), CryoGlassColors.Aurora.copy(alpha = 0.20f)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .border(1.dp, if (selected) CryoGlassColors.Ice.copy(alpha = 0.8f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = icon,
            contentDescription = null,
            tint = if (selected) Color.White else CryoGlassColors.Ice
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToolTile(tool: Tool, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(58.dp)
            .alpha(if (tool.enabled) 1f else 0.38f)
            .clip(shape)
            .background(if (tool.active) CryoGlassColors.Ice.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (tool.active) 0.9f else 0.30f), shape)
            .clickable(enabled = tool.enabled, onClick = tool.onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(tool.icon),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
        Text(
            modifier = Modifier.padding(top = 3.dp),
            text = stringResource(tool.label),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
