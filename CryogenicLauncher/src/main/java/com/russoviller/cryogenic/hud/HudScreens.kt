package com.russoviller.cryogenic.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.screens.content.elements.ImportMultipleFileButton
import com.russoviller.cryogenic.utils.file.shareFile
import com.russoviller.cryogenic.viewmodel.EventViewModel
import java.io.File
import kotlinx.coroutines.launch

/** Cor de destaque de cada esquema (cada um tem o próprio menu). */
val HudScheme.accent: Color
    get() = when (this) {
        HudScheme.CRYOGENIC -> Color(0xFF7FD8FF)
        HudScheme.FCL -> Color(0xFF5BD38A)
        HudScheme.POJARUSH -> Color(0xFFFFA24C)
    }

private val HudScheme.glyph: String
    get() = when (this) {
        HudScheme.CRYOGENIC -> "❄"
        HudScheme.FCL -> "◆"
        HudScheme.POJARUSH -> "▲"
    }

private val Panel = Color(0xCC0B1522)

@Composable
private fun HudScheme.subtitle(): String = stringResource(
    when (this) {
        HudScheme.CRYOGENIC -> R.string.cryo_hud_scheme_cryogenic_desc
        HudScheme.FCL -> R.string.cryo_hud_scheme_fcl_desc
        HudScheme.POJARUSH -> R.string.cryo_hud_scheme_pojarush_desc
    }
)

@Composable
private fun HudScheme.icon(): Int? = when (this) {
    HudScheme.CRYOGENIC -> R.drawable.img_hud_cryogenic
    HudScheme.FCL -> R.drawable.img_hud_fcl
    HudScheme.POJARUSH -> null
}

@Composable
private fun SchemeBadge(scheme: HudScheme, size: androidx.compose.ui.unit.Dp, selected: Boolean) {
    val icon = scheme.icon()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(
            if (icon != null) Color.White.copy(alpha = 0.06f) else if (selected) scheme.accent else scheme.accent.copy(alpha = 0.18f)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                painterResource(icon), contentDescription = scheme.title,
                modifier = Modifier.fillMaxSize().padding(2.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Text(scheme.glyph, color = if (selected) Color(0xFF05101C) else scheme.accent, fontWeight = FontWeight.Black, fontSize = (size.value * 0.45f).sp)
        }
    }
}

/** Seletor do esquema de HUD usado no jogo. */
@Composable
fun HudSchemeSelector(selected: HudScheme, onSelect: (HudScheme) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.widthIn(max = 170.dp).padding(end = 2.dp)) {
            Text(stringResource(R.string.cryo_hud_scheme_title), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
            Text(stringResource(R.string.cryo_hud_scheme_hint), color = Color(0xFFCFEFFF).copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 2)
        }
        HudScheme.entries.forEach { scheme ->
            val sel = scheme == selected
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (sel) Color(0xE60B1522) else Color(0xB30B1522))
                    .background(
                        if (sel) Brush.horizontalGradient(listOf(scheme.accent.copy(alpha = 0.30f), scheme.accent.copy(alpha = 0.06f)))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .border(if (sel) 2.dp else 1.dp, scheme.accent.copy(alpha = if (sel) 0.95f else 0.35f), shape)
                    .clickable { onSelect(scheme) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchemeBadge(scheme, 38.dp, sel)
                Column(Modifier.weight(1f)) {
                    Text(scheme.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                    Text(
                        if (sel) stringResource(R.string.cryo_hud_scheme_active) else scheme.subtitle(),
                        color = if (sel) scheme.accent else Color(0xFFCFEFFF).copy(alpha = 0.8f), fontSize = 10.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (sel) Text("✓", color = scheme.accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

/** Miniatura do HUD na proporção da tela deste aparelho. */
@Composable
fun HudPreview(layout: HudLayout, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val screenW = maxOf(metrics.widthPixels, metrics.heightPixels).toFloat()
    val screenH = minOf(metrics.widthPixels, metrics.heightPixels).toFloat()
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(screenW / screenH)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF243B55), Color(0xFF141E30))))
            .border(1.dp, Color(0x557FD8FF), RoundedCornerShape(14.dp))
    ) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        if (w <= 0 || h <= 0) return@BoxWithConstraints
        val scale = w / screenW
        val outer = LocalDensity.current
        val scaled = Density(outer.density * scale, outer.fontScale)
        CompositionLocalProvider(LocalDensity provides scaled) {
            val measurer = rememberTextMeasurer(cacheSize = 64)
            val geo = remember(w, h, scaled.density) { HudGeometry(w, h, scaled.density) }
            Canvas(Modifier.fillMaxSize()) {
                val hidden = layout.layers.filter { !it.visible }.map { it.id }.toSet()
                layout.sticks.forEach { s -> if (s.layer !in hidden) drawHudStick(s, geo.stickRect(s), HudStickVisual(), 1f, geo.density) { HudImages.bitmap(layout.scheme, it) } }
                layout.buttons.forEach { b -> if (b.layer !in hidden) drawHudButton(b, geo.buttonRect(b), false, 1f, geo.density, measurer) { HudImages.bitmap(layout.scheme, it) } }
            }
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    iconRes: Int,
    accent: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (danger) Color(0xFFFF8A80) else accent
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (filled) color else color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.5f), RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), null, tint = if (filled) Color(0xFF05101C) else color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (filled) Color(0xFF05101C) else color, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Menu de HUDs de cada esquema (Cryogenic, FCL, PojaRush): grade com prévia de todos os HUDs,
 * detalhes, usar no jogo, editar, importar (HUD, Zalith 1/Pojav/Mojo, imagens), compartilhar…
 */
@Composable
fun HudSchemeManager(
    scheme: HudScheme,
    eventViewModel: EventViewModel,
    modifier: Modifier = Modifier,
    onClassicEditor: ((File) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val entries by HudStore.entries(scheme).collectAsState()
    val loading by HudStore.isLoading(scheme).collectAsState()
    val activeName = when (scheme) {
        HudScheme.FCL -> AllSettings.hudLayoutFcl.state
        HudScheme.POJARUSH -> AllSettings.hudLayoutPojarush.state
        HudScheme.CRYOGENIC -> AllSettings.controlLayout.state
    }
    var focusedName by remember(scheme) { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HudStore.Entry?>(null) }
    var editing by remember { mutableStateOf<HudStore.Entry?>(null) }
    var importMenu by remember { mutableStateOf(false) }
    val accent = scheme.accent
    LaunchedEffect(scheme) { HudStore.refreshNow(scheme) }
    // o ControlManager (HUD Cryogenic) também muda de fora: acompanha a lista dele
    if (scheme == HudScheme.CRYOGENIC) {
        val cryoList by com.russoviller.cryogenic.game.control.ControlManager.dataList.collectAsState()
        LaunchedEffect(cryoList) { HudStore.refreshNow(scheme) }
    }

    val active = entries.firstOrNull { it.file.name == activeName && it.layout != null }
    val focused = entries.firstOrNull { it.file.name == focusedName } ?: active ?: entries.firstOrNull()

    val importHud = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) eventViewModel.sendEvent(EventViewModel.Event.ImportControls(uris, scheme)) }
    val importPojav = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) eventViewModel.sendEvent(EventViewModel.Event.ImportControls(uris, scheme, pojavOnly = true)) }
    val importImages = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var n = 0
            uris.forEach { uri ->
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                    if (HudImages.isZip(bytes)) n += HudImages.unpack(scheme, bytes).images
                    else { HudImages.save(scheme, hudUriName(context, uri) ?: "image.png", bytes.inputStream()); n++ }
                }
            }
            HudStore.refreshNow(scheme)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, context.getString(R.string.cryo_hud_images_imported, n), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // ------------------------------------------------ grade de HUDs
        Column(
            Modifier.weight(0.58f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(Panel)
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SchemeBadge(scheme, 30.dp, true)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cryo_hud_list_scheme, scheme.title),
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(stringResource(R.string.cryo_hud_count, entries.size), color = accent, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(R.drawable.ic_refresh, stringResource(R.string.generic_refresh), accent) {
                        HudStore.refresh(scheme)
                        if (scheme == HudScheme.CRYOGENIC) com.russoviller.cryogenic.game.control.ControlManager.refresh()
                    }
                    RoundAction(R.drawable.ic_add_box_outlined, stringResource(R.string.control_manage_create_new), accent) { creating = true }
                    Box {
                        ActionPill(stringResource(R.string.generic_import), R.drawable.ic_add, accent, filled = true) { importMenu = true }
                        androidx.compose.material3.DropdownMenu(expanded = importMenu, onDismissRequest = { importMenu = false }) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.cryo_hud_import_scheme, scheme.title)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_add), null) },
                                onClick = { importMenu = false; importHud.launch("*/*") }
                            )
                            if (scheme == HudScheme.CRYOGENIC) androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.cryo_hud_convert_pojav)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_cryo_swap), null) },
                                onClick = { importMenu = false; importPojav.launch("*/*") }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.cryo_hud_import_images)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_cryo_layers), null) },
                                onClick = { importMenu = false; importImages.launch("*/*") }
                            )
                        }
                    }
                }
            }
            when {
                loading && entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
                entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.control_manage_list_empty), color = Color(0xFFCFEFFF), fontSize = 13.sp)
                }
                else -> androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.file.name }) { e ->
                        val isActive = e.file.name == active?.file?.name
                        val isFocused = e.file.name == focused?.file?.name
                        val shape = RoundedCornerShape(16.dp)
                        Column(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (isFocused) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
                                .border(if (isFocused) 2.dp else 1.dp, accent.copy(alpha = if (isFocused) 0.95f else if (isActive) 0.6f else 0.18f), shape)
                                .clickable { focusedName = e.file.name }
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box {
                                if (e.layout != null) HudPreview(e.layout, Modifier.fillMaxWidth())
                                else Box(
                                    Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33FF5252)),
                                    contentAlignment = Alignment.Center
                                ) { Text("!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 22.sp) }
                                if (isActive) Text(
                                    stringResource(R.string.cryo_hud_active), color = Color(0xFF05101C), fontWeight = FontWeight.Black, fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(accent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(e.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (e.layout == null) stringResource(R.string.cryo_hud_broken)
                                else stringResource(R.string.hude_stats, e.layout.buttons.size, e.layout.sticks.size, e.layout.layers.size),
                                color = if (e.layout == null) MaterialTheme.colorScheme.error else Color(0xFFCFEFFF).copy(alpha = 0.65f),
                                fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ------------------------------------------------ detalhes
        Column(
            Modifier.weight(0.42f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(Panel)
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        ) {
            val e = focused
            val layout = e?.layout
            if (e == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.control_manage_info_empty), color = Color(0xFFCFEFFF), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                val isActive = e.file.name == active?.file?.name
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(if (isActive) R.string.cryo_hud_scheme_active else R.string.cryo_hud_selected),
                        color = accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                    )
                    Text(e.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (layout != null) {
                        HudPreview(layout, Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoChip(stringResource(R.string.control_manage_create_new_author), layout.author.ifBlank { "—" }, accent, Modifier.weight(1f))
                            InfoChip(stringResource(R.string.control_manage_create_new_version_name), layout.version.ifBlank { "—" }, accent, Modifier.weight(1f))
                        }
                        InfoChip(
                            stringResource(R.string.hude_info),
                            stringResource(R.string.hude_stats, layout.buttons.size, layout.sticks.size, layout.layers.size), accent, Modifier.fillMaxWidth()
                        )
                        val missing = remember(layout) { runCatching { HudImages.missing(scheme, layout) }.getOrDefault(emptyList()) }
                        if (missing.isNotEmpty()) InfoChip(
                            stringResource(R.string.cryo_hud_images_missing_title),
                            stringResource(R.string.cryo_hud_images_missing, missing.size, missing.take(4).joinToString()), Color(0xFFFF8A80), Modifier.fillMaxWidth()
                        )
                        if (layout.description.isNotBlank()) InfoChip(stringResource(R.string.control_manage_info_description), layout.description, accent, Modifier.fillMaxWidth())
                    } else {
                        Text(e.error ?: stringResource(R.string.cryo_hud_broken), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (layout != null && !isActive) ActionPill(stringResource(R.string.cryo_hud_use), R.drawable.ic_check, accent, filled = true, modifier = Modifier.weight(1f)) {
                            HudStore.select(scheme, e.file)
                        }
                        if (layout != null) ActionPill(stringResource(R.string.cryo_hud_edit_in, scheme.title), R.drawable.ic_edit_filled, accent,
                            filled = isActive, modifier = Modifier.weight(1f)) { editing = e }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionPill(stringResource(R.string.generic_share), R.drawable.ic_share_filled, accent, filled = false, modifier = Modifier.weight(1f)) {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val f = runCatching { HudStore.shareable(scheme, e, context.cacheDir) }.getOrDefault(e.file)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { shareFile(context, f) }
                            }
                        }
                        if (layout != null) ActionPill(stringResource(R.string.generic_copy), R.drawable.ic_copy_all_outlined, accent, filled = false, modifier = Modifier.weight(1f)) {
                            HudStore.duplicate(scheme, e)
                        }
                        ActionPill(stringResource(R.string.generic_delete), R.drawable.ic_delete_outlined, accent, filled = false, danger = true, modifier = Modifier.weight(1f)) {
                            deleting = e
                        }
                    }
                }
            }
        }
    }

    if (creating) NewHudDialog(scheme, onDismiss = { creating = false }) { name, author, starter ->
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val f = runCatching { HudStore.create(scheme, name, author, starter) }.getOrNull()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (f != null) focusedName = f.name
            }
        }
        creating = false
    }

    deleting?.let { e ->
        Dialog(onDismissRequest = { deleting = null }) {
            DialogBox(accent) {
                Text(stringResource(R.string.generic_delete), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(stringResource(R.string.cryo_hud_delete_confirm, e.name), color = Color(0xFFCFEFFF), fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.generic_cancel), color = Color(0xFFCFEFFF)) }
                    TextButton(onClick = { HudStore.delete(scheme, e); deleting = null; focusedName = null }) {
                        Text(stringResource(R.string.generic_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editing?.let { e ->
        val layout = e.layout
        if (layout != null) HudEditorDialog(
            initial = layout,
            onSave = { l -> runCatching { HudStore.save(e.file, l) }.isSuccess.also { HudStore.refresh(scheme) } },
            onExit = { editing = null },
            onClassicEditor = if (scheme == HudScheme.CRYOGENIC && onClassicEditor != null) {
                { editing = null; onClassicEditor(e.file) }
            } else null
        )
    }
}

@Composable
private fun RoundAction(iconRes: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), contentDescription = description, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun InfoChip(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(title, color = Color(0xFFCFEFFF).copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DialogBox(accent: Color, content: @Composable () -> Unit) {
    Column(
        Modifier.widthIn(max = 420.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xF00B1522))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
private fun NewHudDialog(scheme: HudScheme, onDismiss: () -> Unit, onCreate: (String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var starter by remember { mutableStateOf(true) }
    val accent = scheme.accent
    Dialog(onDismissRequest = onDismiss) {
        DialogBox(accent) {
            Text(stringResource(R.string.cryo_hud_new_title, scheme.title), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                label = { Text(stringResource(R.string.control_manage_create_new_name)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = author, onValueChange = { author = it.take(40) }, singleLine = true,
                label = { Text(stringResource(R.string.control_manage_create_new_author)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.cryo_hud_template), color = Color(0xFFCFEFFF), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true to R.string.cryo_hud_template_starter, false to R.string.cryo_hud_template_empty).forEach { (v, res) ->
                    val sel = v == starter
                    Text(
                        stringResource(res), textAlign = TextAlign.Center,
                        color = if (sel) Color(0xFF05101C) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) accent else Color.White.copy(alpha = 0.08f))
                            .clickable { starter = v }.padding(vertical = 10.dp)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel), color = Color(0xFFCFEFFF)) }
                TextButton(onClick = { onCreate(name.trim().ifEmpty { "HUD ${scheme.title}" }, author.trim(), starter) }) {
                    Text(stringResource(R.string.control_manage_create_new), color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Trocar de HUD dentro do jogo (esquemas FCL / PojaRush). */
@Composable
fun HudPickerDialog(scheme: HudScheme, current: File?, onPick: (File) -> Unit, onDismiss: () -> Unit) {
    val entries by HudStore.entries(scheme).collectAsState()
    LaunchedEffect(scheme) { HudStore.refreshNow(scheme) }
    val accent = scheme.accent
    Dialog(onDismissRequest = onDismiss) {
        DialogBox(accent) {
            Text(stringResource(R.string.game_menu_option_replacement_control), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(scheme.title, color = accent, fontSize = 11.sp, letterSpacing = 2.sp)
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (entries.isEmpty()) Text(stringResource(R.string.control_manage_list_empty), color = Color(0xFFCFEFFF), fontSize = 13.sp)
                entries.filter { it.layout != null }.forEach { e ->
                    val sel = e.file.name == current?.name
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (sel) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, accent.copy(alpha = if (sel) 0.9f else 0.2f), RoundedCornerShape(12.dp))
                            .clickable { onPick(e.file); onDismiss() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(e.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (sel) Text("✓", color = accent, fontWeight = FontWeight.Black)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_close), color = Color(0xFFCFEFFF)) }
            }
        }
    }
}
