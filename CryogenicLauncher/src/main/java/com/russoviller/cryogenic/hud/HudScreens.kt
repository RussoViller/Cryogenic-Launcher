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

/** Seletor do esquema de HUD usado no jogo. */
@Composable
fun HudSchemeSelector(selected: HudScheme, onSelect: (HudScheme) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.padding(end = 4.dp)) {
            Text(stringResource(R.string.cryo_hud_scheme_title), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(stringResource(R.string.cryo_hud_scheme_hint), color = Color(0xFFCFEFFF).copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 2)
        }
        HudScheme.entries.forEach { scheme ->
            val sel = scheme == selected
            val shape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        if (sel) Brush.horizontalGradient(listOf(scheme.accent.copy(alpha = 0.32f), scheme.accent.copy(alpha = 0.08f)))
                        else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                    )
                    .border(if (sel) 1.5.dp else 1.dp, scheme.accent.copy(alpha = if (sel) 0.9f else 0.25f), shape)
                    .clickable { onSelect(scheme) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (sel) scheme.accent else scheme.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(scheme.glyph, color = if (sel) Color(0xFF05101C) else scheme.accent, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(scheme.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                    Text(
                        if (sel) stringResource(R.string.cryo_hud_scheme_active) else scheme.subtitle(),
                        color = if (sel) scheme.accent else Color(0xFFCFEFFF).copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                layout.sticks.forEach { s -> if (s.layer !in hidden) drawHudStick(s, geo.stickRect(s), HudStickVisual(), 1f, geo.density) }
                layout.buttons.forEach { b -> if (b.layer !in hidden) drawHudButton(b, geo.buttonRect(b), false, 1f, geo.density, measurer) }
            }
        }
    }
}

@Composable
private fun RoundAction(iconRes: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), contentDescription = description, tint = tint, modifier = Modifier.size(17.dp))
    }
}

/**
 * Menu próprio dos esquemas FCL e PojaRush: lista, prévia, importar, criar, editar, compartilhar…
 */
@Composable
fun HudSchemeManager(
    scheme: HudScheme,
    eventViewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entries by HudStore.entries(scheme).collectAsState()
    val loading by HudStore.isLoading(scheme).collectAsState()
    val selectedName = when (scheme) {
        HudScheme.FCL -> AllSettings.hudLayoutFcl.state
        HudScheme.POJARUSH -> AllSettings.hudLayoutPojarush.state
        HudScheme.CRYOGENIC -> AllSettings.controlLayout.state
    }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HudStore.Entry?>(null) }
    var editing by remember { mutableStateOf<HudStore.Entry?>(null) }
    val accent = scheme.accent
    LaunchedEffect(scheme) { HudStore.refreshNow(scheme) }

    val active = entries.firstOrNull { it.file.name == selectedName } ?: entries.firstOrNull { it.layout != null }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // lista
        Column(
            Modifier.weight(0.45f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(Panel)
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (scheme == HudScheme.FCL) R.string.cryo_hud_list_fcl else R.string.cryo_hud_list_pojarush),
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp
                    )
                    Text(stringResource(R.string.cryo_hud_count, entries.size), color = accent.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoundAction(R.drawable.ic_refresh, stringResource(R.string.generic_refresh), accent) { HudStore.refresh(scheme) }
                    RoundAction(R.drawable.ic_add_box_outlined, stringResource(R.string.control_manage_create_new), accent) { creating = true }
                }
            }
            Box(
                Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.08f)).border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                ImportMultipleFileButton(
                    extension = "json",
                    text = stringResource(R.string.cryo_hud_import_scheme, scheme.title),
                    progressUris = { uris -> eventViewModel.sendEvent(EventViewModel.Event.ImportControls(uris, scheme)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            when {
                loading && entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
                entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.control_manage_list_empty), color = Color(0xFFCFEFFF), fontSize = 13.sp)
                }
                else -> LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.file.name }) { e ->
                        val sel = e.file.name == active?.file?.name
                        val shape = RoundedCornerShape(16.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(
                                    if (sel) Brush.horizontalGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.06f)))
                                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                                )
                                .border(if (sel) 1.5.dp else 1.dp, accent.copy(alpha = if (sel) 0.9f else 0.2f), shape)
                                .clickable(enabled = e.layout != null && !sel) { HudStore.select(scheme, e.file) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) accent else accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) { Text(scheme.glyph, color = if (sel) Color(0xFF05101C) else accent, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                            Column(Modifier.weight(1f)) {
                                Text(e.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when {
                                        e.layout == null -> stringResource(R.string.cryo_hud_broken)
                                        sel -> stringResource(R.string.cryo_hud_active)
                                        else -> stringResource(R.string.hude_stats, e.layout.buttons.size, e.layout.sticks.size, e.layout.layers.size)
                                    },
                                    color = when {
                                        e.layout == null -> MaterialTheme.colorScheme.error
                                        sel -> accent
                                        else -> Color(0xFFCFEFFF).copy(alpha = 0.6f)
                                    },
                                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (e.layout != null) Icon(
                                painterResource(R.drawable.ic_copy_all_outlined), stringResource(R.string.generic_copy), tint = Color(0xFFCFEFFF),
                                modifier = Modifier.size(32.dp).clip(CircleShape).clickable { HudStore.duplicate(scheme, e) }.padding(6.dp)
                            )
                            Icon(
                                painterResource(R.drawable.ic_delete_outlined), stringResource(R.string.generic_delete), tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp).clip(CircleShape).clickable { deleting = e }.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        // detalhes
        Column(
            Modifier.weight(0.55f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(Panel)
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        ) {
            val layout = active?.layout
            if (active == null || layout == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.control_manage_info_empty), color = Color(0xFFCFEFFF), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.cryo_hud_selected), color = accent, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text(layout.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HudPreview(layout, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoChip(stringResource(R.string.control_manage_create_new_author), layout.author.ifBlank { "—" }, accent, Modifier.weight(1f))
                        InfoChip(stringResource(R.string.control_manage_create_new_version_name), layout.version.ifBlank { "—" }, accent, Modifier.weight(1f))
                    }
                    InfoChip(
                        stringResource(R.string.hude_info),
                        stringResource(R.string.hude_stats, layout.buttons.size, layout.sticks.size, layout.layers.size), accent, Modifier.fillMaxWidth()
                    )
                    if (layout.description.isNotBlank()) InfoChip(stringResource(R.string.control_manage_info_description), layout.description, accent, Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { shareFile(context, active.file) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_share_filled), null, tint = accent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.generic_share), color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(
                        Modifier.weight(1.4f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(accent).clickable { editing = active },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_edit_filled), null, tint = Color(0xFF05101C), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.cryo_hud_edit_in, scheme.title), color = Color(0xFF05101C), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (creating) NewHudDialog(scheme, onDismiss = { creating = false }) { name, author, starter ->
        val f = HudStore.create(scheme, name, author, starter)
        HudStore.select(scheme, f)
        creating = false
    }

    deleting?.let { e ->
        Dialog(onDismissRequest = { deleting = null }) {
            DialogBox(accent) {
                Text(stringResource(R.string.generic_delete), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(stringResource(R.string.cryo_hud_delete_confirm, e.name), color = Color(0xFFCFEFFF), fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.generic_cancel), color = Color(0xFFCFEFFF)) }
                    TextButton(onClick = { HudStore.delete(scheme, e); deleting = null }) {
                        Text(stringResource(R.string.generic_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editing?.let { e ->
        val layout = e.layout
        if (layout == null) editing = null
        else HudEditorDialog(
            initial = layout,
            onSave = { l -> runCatching { HudStore.save(e.file, l) }.isSuccess.also { HudStore.refresh(scheme) } },
            onExit = { editing = null }
        )
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
