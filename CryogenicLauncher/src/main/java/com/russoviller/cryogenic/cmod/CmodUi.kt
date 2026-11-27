package com.russoviller.cryogenic.cmod

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.cmod.api.CryoModApi
import com.russoviller.cryogenic.cmod.api.CryoModScreen
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.AnimatedColumn
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoOutlinedButton
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleTextInputField
import androidx.compose.foundation.layout.PaddingValues
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.verticalScrollWithBar
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import kotlinx.coroutines.launch

// ============================================================ Configurações › Launcher

/** Aba "Launcher" das configurações: Mods do Launcher. */
@Composable
fun LauncherExtrasSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.LauncherExtras, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        LaunchedEffect(Unit) { CmodManager.init(context) }
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                var open by remember { mutableStateOf(false) }
                val count = CmodManager.installed.size
                val active = CmodManager.installed.count { CmodManager.isEnabled(it.info.id) }
                com.russoviller.cryogenic.ui.screens.content.settings.CryoSettingsEntryCard(
                    modifier = Modifier.animated(ctx),
                    iconRes = R.drawable.ic_extension_outlined,
                    title = stringResource(R.string.cryo_cmods_launcher_mods),
                    summary = if (count == 0) stringResource(R.string.cryo_cmods_launcher_mods_summary)
                        else stringResource(R.string.cryo_cmods_count, count, active),
                    onOpen = { open = true }
                )
                if (open) LauncherModsDialog(onDismiss = { open = false })
            }
        }
    }
}

// ============================================================ Mods do Launcher (janela)

private const val NONE = ""

/**
 * Janela "Mods do Launcher": importar .cmod, ativar/desativar e apagar. Mesmo formato das
 * Predefinições (lista à esquerda, detalhes à direita).
 */
@Composable
fun LauncherModsDialog(onDismiss: () -> Unit, onOpenCryoMods: (() -> Unit)? = null) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LauncherModsContent(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(),
            onDismiss = onDismiss,
            onOpenCryoMods = onOpenCryoMods
        )
    }
}

private enum class ModFilter { ALL, ON, OFF }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LauncherModsContent(
    modifier: Modifier,
    onDismiss: () -> Unit,
    onOpenCryoMods: (() -> Unit)? = null,
    initialSelectedId: String = NONE
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { CmodManager.init(context) } }
    val mods = CmodManager.installed
    var selectedId by remember { mutableStateOf(initialSelectedId) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ModFilter.ALL) }
    var logsFor by remember { mutableStateOf<InstalledCmod?>(null) }
    val selected = mods.firstOrNull { it.info.id == selectedId }
    val shown = mods.filter { mod ->
        val on = CmodManager.isEnabled(mod.info.id)
        val q = query.trim()
        (filter == ModFilter.ALL || (filter == ModFilter.ON) == on) &&
            (q.isEmpty() || mod.info.name.contains(q, true) || mod.info.id.contains(q, true) ||
                mod.info.author.contains(q, true) || mod.info.description.contains(q, true))
    }

    val wrongText = stringResource(R.string.cryo_cmods_import_wrong)
    val errorText = stringResource(R.string.cryo_presets_error)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            message = null
            scope.launch {
                val result = runCatching { CmodManager.import(context, uri) }
                busy = false
                result.onSuccess { info ->
                    selectedId = info.id
                    message = context.getString(R.string.cryo_cmods_imported, info.name)
                }.onFailure {
                    message = when (it) {
                        is CmodManager.WrongExtension -> wrongText
                        is CmodManager.InvalidCmod -> context.getString(R.string.cryo_cmods_import_invalid, it.message.orEmpty())
                        else -> errorText
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier
            .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, opaque = true)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------------- Lista ----------------
        Column(modifier = Modifier.width(250.dp).fillMaxHeight()) {
            Text(
                text = stringResource(R.string.cryo_cmods_launcher_mods),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                )
            )
            Text(
                text = if (mods.isEmpty()) stringResource(R.string.cryo_cmods_launcher_mods_summary)
                    else stringResource(R.string.cryo_cmods_count, mods.size, mods.count { CmodManager.isEnabled(it.info.id) }),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(8.dp))
            if (mods.isNotEmpty()) {
                SimpleTextInputField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    hint = {
                        Text(
                            text = stringResource(R.string.cryo_cmods_search_mods),
                            style = MaterialTheme.typography.labelMedium,
                            color = CryoGlassColors.Frost.copy(alpha = 0.55f)
                        )
                    },
                    contextPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    textStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModFilter.entries.forEach { f ->
                        ModChip(
                            text = stringResource(
                                when (f) {
                                    ModFilter.ALL -> R.string.cryo_cmods_filter_all
                                    ModFilter.ON -> R.string.cryo_cmods_status_on
                                    ModFilter.OFF -> R.string.cryo_cmods_status_off
                                }
                            ),
                            selected = filter == f,
                            onClick = { filter = f }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (mods.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        text = stringResource(R.string.cryo_cmods_empty_installed),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                    )
                } else if (shown.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        text = stringResource(R.string.cryo_cmods_search_empty_mods),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                    )
                }
                shown.forEach { mod ->
                    val status = modStatus(mod)
                    ModTile(
                        icon = mod.icon,
                        title = mod.info.name,
                        subtitle = stringResource(status.text),
                        subtitleColor = status.color,
                        selected = mod.info.id == selectedId,
                        dot = status.color.takeIf { status != ModStatus.OFF },
                        badge = mod.info.version.takeIf { it.isNotEmpty() }?.let { "v$it" },
                        onClick = { selectedId = mod.info.id; message = null }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CryoButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = { runCatching { importLauncher.launch(arrayOf("*/*")) } }
            ) {
                Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_download_2_filled), contentDescription = null)
                MarqueeText(text = stringResource(R.string.cryo_cmods_import))
            }
            Spacer(Modifier.height(6.dp))
            CryoTonalButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onDismiss) {
                Text(text = stringResource(R.string.generic_close))
            }
        }

        // ---------------- Detalhes ----------------
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selected == null) {
                ModHero(
                    label = stringResource(R.string.cryo_cmods_launcher_mods),
                    title = stringResource(R.string.cryo_cmods_what_title),
                    lines = listOf(stringResource(R.string.cryo_cmods_what_body)),
                    icon = null
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        R.string.cryo_cmods_cap_launcher, R.string.cryo_cmods_cap_game,
                        R.string.cryo_cmods_cap_jvm, R.string.cryo_cmods_cap_args
                    ).forEach { ModChip(text = stringResource(it), selected = false) }
                }
            } else {
                val info = selected.info
                val enabled = CmodManager.isEnabled(info.id)
                val active = CmodManager.isActive(info.id)
                val error = CmodManager.errors[info.id]
                val status = modStatus(selected)
                ModHero(
                    label = "CMOD · ${info.id}",
                    title = info.name,
                    lines = listOfNotNull(
                        listOf(info.version.takeIf { it.isNotEmpty() }?.let { "v$it" }, info.author.takeIf { it.isNotEmpty() })
                            .filterNotNull().joinToString(" · ").ifEmpty { null },
                        info.description.ifEmpty { null }
                    ),
                    icon = selected.icon
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModChip(text = stringResource(status.text), selected = true, color = status.color)
                    if (info.hasLauncherSide) ModChip(text = stringResource(R.string.cryo_cmods_cap_launcher), selected = false)
                    if (info.hasGameSide) ModChip(text = stringResource(R.string.cryo_cmods_cap_game), selected = false)
                    if (info.jvmArgs.isNotEmpty()) ModChip(text = stringResource(R.string.cryo_cmods_cap_jvm), selected = false)
                    if (info.gameArgs.isNotEmpty()) ModChip(text = stringResource(R.string.cryo_cmods_cap_args), selected = false)
                }
                // ficha técnica
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoLine(stringResource(R.string.cryo_cmods_info_id), info.id)
                    if (info.version.isNotEmpty()) InfoLine(stringResource(R.string.cryo_cmods_info_version), info.version)
                    if (info.author.isNotEmpty()) InfoLine(stringResource(R.string.cryo_cmods_info_author), info.author)
                    InfoLine("API", "v${info.apiVersion}")
                    InfoLine(stringResource(R.string.cryo_cmods_info_size), formatSize(selected.sizeBytes))
                    if (info.dependencies.isNotEmpty()) {
                        InfoLine(stringResource(R.string.cryo_cmods_info_requires), info.dependencies.joinToString(", ") { d ->
                            mods.firstOrNull { it.info.id == d }?.info?.name ?: d
                        })
                    }
                }
                if (error != null && enabled) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FF5252))
                            .border(1.dp, Color(0xAAFF5252), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        text = stringResource(R.string.cryo_cmods_error, error),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                var confirmDelete by remember(info.id) { mutableStateOf(false) }
                var confirmClear by remember(info.id) { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoButton(
                        modifier = Modifier.weight(1.2f),
                        enabled = !busy,
                        onClick = { CmodManager.setEnabled(info.id, !enabled) }
                    ) {
                        MarqueeText(text = stringResource(if (enabled) R.string.cryo_cmods_disable else R.string.cryo_cmods_enable))
                    }
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy && enabled,
                        onClick = {
                            CmodManager.restart(info.id)
                            message = context.getString(R.string.cryo_cmods_restarted, info.name)
                        }
                    ) {
                        Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_restart_alt), contentDescription = null)
                        MarqueeText(text = stringResource(R.string.cryo_cmods_restart))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoTonalButton(modifier = Modifier.weight(1f), onClick = { logsFor = selected }) {
                        MarqueeText(text = stringResource(R.string.cryo_cmods_logs))
                    }
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                runCatching { CmodManager.share(context, CmodManager.export(context, info.id)) }
                                    .onFailure { message = errorText }
                                busy = false
                            }
                        }
                    ) {
                        Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_share_filled), contentDescription = null)
                        MarqueeText(text = stringResource(R.string.cryo_cmods_share))
                    }
                    if (info.website.isNotEmpty()) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.website)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }
                        ) {
                            MarqueeText(text = stringResource(R.string.cryo_cmods_website))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoOutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = { confirmClear = !confirmClear; confirmDelete = false }
                    ) {
                        MarqueeText(text = stringResource(R.string.cryo_cmods_clear_data))
                    }
                    CryoOutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = { confirmDelete = !confirmDelete; confirmClear = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_delete_outlined), contentDescription = null)
                        MarqueeText(text = stringResource(R.string.cryo_presets_delete))
                    }
                }
                if (confirmClear) {
                    ConfirmBox(
                        text = stringResource(R.string.cryo_cmods_clear_data_confirm, info.name),
                        confirm = stringResource(R.string.cryo_cmods_clear_data),
                        onConfirm = {
                            CmodManager.clearData(info.id)
                            confirmClear = false
                            message = context.getString(R.string.cryo_cmods_data_cleared)
                        },
                        onCancel = { confirmClear = false }
                    )
                }
                if (confirmDelete) {
                    ConfirmBox(
                        text = stringResource(R.string.cryo_cmods_delete_confirm, info.name),
                        confirm = stringResource(R.string.cryo_presets_delete),
                        onConfirm = {
                            CmodManager.delete(info.id)
                            selectedId = NONE
                        },
                        onCancel = { confirmDelete = false }
                    )
                }
                if (onOpenCryoMods != null && active && info.hasLauncherSide) {
                    CryoTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenCryoMods) {
                        MarqueeText(text = stringResource(R.string.cryo_cmods_open_screen))
                    }
                }
                if (info.hasGameSide) NoteBox(stringResource(R.string.cryo_cmods_game_note))
                if (info.jvmArgs.isNotEmpty() || info.gameArgs.isNotEmpty()) {
                    NoteBox(stringResource(R.string.cryo_cmods_args_note, (info.jvmArgs + info.gameArgs).joinToString(" ")), mono = true)
                }
            }

            message?.let { status ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            //aviso: o código do mod roda dentro do Launcher
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFB300).copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                text = stringResource(R.string.cryo_cmods_warning),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFE6A3)
            )
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = stringResource(R.string.cryo_cmods_api_version, CryoModApi.API_VERSION),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice
            )
        }
    }
    logsFor?.let { mod -> ModLogsDialog(mod = mod, onDismiss = { logsFor = null }) }
}

private enum class ModStatus(val text: Int, val color: Color) {
    RUNNING(R.string.cryo_cmods_status_running, Color(0xFF7CF5B8)),
    GAME(R.string.cryo_cmods_status_game, Color(0xFF8EC9FF)),
    ERROR(R.string.cryo_cmods_status_error, Color(0xFFFF8A80)),
    OFF(R.string.cryo_cmods_status_off, Color(0xA6D6E6F2))
}

private fun modStatus(mod: InstalledCmod): ModStatus {
    val id = mod.info.id
    return when {
        !CmodManager.isEnabled(id) -> ModStatus.OFF
        CmodManager.errors[id] != null || !CmodManager.isActive(id) -> ModStatus.ERROR
        mod.info.hasLauncherSide -> ModStatus.RUNNING
        else -> ModStatus.GAME
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1048576.0)
    bytes >= 1024 -> String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            modifier = Modifier.width(92.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Ice,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White
        )
    }
}

@Composable
private fun ModChip(text: String, selected: Boolean, color: Color = CryoGlassColors.Ice, onClick: (() -> Unit)? = null) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, color.copy(alpha = if (selected) 0.9f else 0.35f), RoundedCornerShape(50))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else CryoGlassColors.Frost.copy(alpha = 0.85f),
        maxLines = 1
    )
}

@Composable
private fun NoteBox(text: String, mono: Boolean = false) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
            .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall.let { if (mono) it.copy(fontFamily = FontFamily.Monospace) else it },
        color = CryoGlassColors.Frost
    )
}

@Composable
private fun ConfirmBox(text: String, confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CryoOutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { MarqueeText(text = confirm) }
            CryoTonalButton(modifier = Modifier.weight(1f), onClick = onCancel) {
                Text(text = stringResource(R.string.generic_cancel))
            }
        }
    }
}

/** Log do mod: lado do Launcher (desta sessão) e lado do jogo (última vez que o jogo abriu). */
@Composable
private fun ModLogsDialog(mod: InstalledCmod, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var gameTab by remember { mutableStateOf(false) }
    val revision = CmodLogs.revision
    var reload by remember { mutableStateOf(0) }
    val lines = remember(revision, gameTab, reload) {
        if (gameTab) CmodLogs.gameLines(mod.info.id) else CmodLogs.get(mod.info.id)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.92f)
                .cryoGlass(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp, opaque = true)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModIcon(mod.icon, 28)
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${mod.info.name} · ${stringResource(R.string.cryo_cmods_logs)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ModChip(text = stringResource(R.string.cryo_cmods_cap_launcher), selected = !gameTab, onClick = { gameTab = false })
                ModChip(text = stringResource(R.string.cryo_cmods_cap_game), selected = gameTab, onClick = { gameTab = true; reload++ })
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .verticalScroll(rememberScrollState(), reverseScrolling = true)
                    .padding(10.dp)
            ) {
                if (lines.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cryo_cmods_logs_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                    )
                }
                lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = when {
                            line.length > 9 && line[9] == 'E' -> Color(0xFFFF8A80)
                            else -> CryoGlassColors.Frost
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = lines.isNotEmpty(),
                    onClick = {
                        runCatching {
                            context.getSystemService(android.content.ClipboardManager::class.java)
                                ?.setPrimaryClip(android.content.ClipData.newPlainText(mod.info.name, lines.joinToString("\n")))
                        }
                    }
                ) { MarqueeText(text = stringResource(R.string.cryo_cmods_copy)) }
                CryoOutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = lines.isNotEmpty(),
                    onClick = { CmodLogs.clear(mod.info.id); reload++ }
                ) { MarqueeText(text = stringResource(R.string.cryo_cmods_clear)) }
                CryoButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                    Text(text = stringResource(R.string.generic_close))
                }
            }
        }
    }
}

@Composable
private fun ModIcon(icon: Bitmap?, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora)))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = icon.asImageBitmap(),
                contentDescription = null
            )
        } else {
            Icon(
                modifier = Modifier.size((size * 0.55f).dp),
                painter = painterResource(R.drawable.ic_extension_outlined),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
    }
}

@Composable
private fun ModTile(
    icon: Bitmap?,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    dot: Color? = null,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft.copy(alpha = 0.55f), CryoGlassColors.Aurora.copy(alpha = 0.15f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box {
            ModIcon(icon, 36)
            if (dot != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(11.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Deep)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dot)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!badge.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice)))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Deep,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModHero(label: String, title: String, lines: List<String>, icon: Bitmap?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModIcon(icon, 64)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            lines.forEach {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = CryoGlassColors.Frost.copy(alpha = 0.85f))
            }
        }
    }
}

// ============================================================ tela CryoMods

/** Seletor "criar arquivo" com tipo e nome escolhidos pelo mod. */
private class CreateFileContract : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.second.ifBlank { "application/octet-stream" })
            .putExtra(Intent.EXTRA_TITLE, input.first)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Tela "CryoMods": botões adicionados pelos Mods do Launcher ativos. Ao tocar num botão, a área da
 * direita fica com o mod (Views dele). Sem mods: "Vazio, não há nada aqui".
 */
@Composable
fun CryoModsScreen(
    key: NormalNavKey.CryoMods,
    currentKey: TitledNavKey?,
    openLauncherMods: () -> Unit
) {
    BaseScreen(screenKey = key, currentKey = currentKey) { isVisible ->
        if (!isVisible) return@BaseScreen
        val context = LocalContext.current
        LaunchedEffect(Unit) { CmodManager.init(context) }
        val buttons = CmodManager.buttons
        var openEntry by remember { mutableStateOf<CmodButtonEntry?>(null) }
        var screenTitle by remember { mutableStateOf("") }
        val container = remember { mutableStateOf<FrameLayout?>(null) }
        var pendingCallback by remember { mutableStateOf<CryoModScreen.FileCallback?>(null) }
        var showMods by remember { mutableStateOf(false) }
        var headerVisible by remember { mutableStateOf(true) }
        var query by remember { mutableStateOf("") }
        val shown = remember(buttons.toList(), query) {
            val q = query.trim()
            if (q.isEmpty()) buttons.toList()
            else buttons.filter {
                it.button.title.contains(q, ignoreCase = true) ||
                    it.modName.contains(q, ignoreCase = true) ||
                    it.button.description?.contains(q, ignoreCase = true) == true
            }
        }

        // o mod sumiu (desativado/apagado): fecha a área dele
        LaunchedEffect(buttons.size) {
            val e = openEntry
            if (e != null && buttons.none { it.modId == e.modId && it.button.id == e.button.id }) {
                openEntry = null
                container.value?.removeAllViews()
            }
        }

        fun deliver(uri: Uri?) {
            val cb = pendingCallback
            pendingCallback = null
            val e = openEntry
            if (cb != null) {
                if (e != null) CmodManager.safeRun(e) { cb.onResult(uri) } else runCatching { cb.onResult(uri) }
            }
        }
        val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { deliver(it) }
        val createLauncher = rememberLauncherForActivityResult(CreateFileContract()) { deliver(it) }
        val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { deliver(it) }

        val host = remember {
            object : CryoModScreen {
                override fun getActivity(): Activity = context.findActivity() ?: error("no activity")
                override fun setContent(view: View) {
                    val box = container.value ?: return
                    (view.parent as? ViewGroup)?.removeView(view)
                    box.removeAllViews()
                    box.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
                override fun setTitle(title: String) { screenTitle = title }
                override fun setHeaderVisible(visible: Boolean) { headerVisible = visible }
                private val ui by lazy {
                    CmodUiKit(context.findActivity() ?: context) { block ->
                        val e = openEntry
                        if (e != null) CmodManager.safeRun(e, block) else runCatching(block)
                    }
                }
                override fun getUi(): com.russoviller.cryogenic.cmod.api.CryoModUi = ui
                override fun close() {
                    openEntry = null
                    headerVisible = true
                    container.value?.removeAllViews()
                }
                override fun pickFile(mimeTypes: Array<String>?, callback: CryoModScreen.FileCallback) {
                    pendingCallback = callback
                    val types = mimeTypes?.filter { it.isNotBlank() }?.toTypedArray()?.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
                    runCatching { openLauncher.launch(types) }.onFailure { deliver(null) }
                }
                override fun createFile(suggestedName: String?, mimeType: String?, callback: CryoModScreen.FileCallback) {
                    pendingCallback = callback
                    runCatching { createLauncher.launch((suggestedName ?: "file") to (mimeType ?: "")) }.onFailure { deliver(null) }
                }
                override fun pickFolder(callback: CryoModScreen.FileCallback) {
                    pendingCallback = callback
                    runCatching { folderLauncher.launch(null) }.onFailure { deliver(null) }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 12.dp, bottom = 12.dp)
                .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.width(250.dp).fillMaxHeight()) {
                Text(
                    text = stringResource(R.string.cryo_cmods_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Text(
                    text = stringResource(R.string.cryo_cmods_screen_summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(8.dp))
                SimpleTextInputField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    hint = {
                        Text(
                            text = stringResource(R.string.cryo_cmods_search),
                            style = MaterialTheme.typography.labelMedium,
                            color = CryoGlassColors.Frost.copy(alpha = 0.55f)
                        )
                    },
                    contextPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    textStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (shown.isEmpty() && buttons.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            text = stringResource(R.string.cryo_cmods_search_empty),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                        )
                    }
                    shown.forEach { entry ->
                        val selected = openEntry?.let { it.modId == entry.modId && it.button.id == entry.button.id } == true
                        ModTile(
                            icon = entry.button.icon ?: entry.modIcon,
                            title = entry.button.title,
                            subtitle = entry.button.description?.takeIf { it.isNotBlank() } ?: entry.modName,
                            subtitleColor = CryoGlassColors.Frost.copy(alpha = 0.75f),
                            selected = selected,
                            badge = CmodManager.badges[entry.modId],
                            onClick = {
                                openEntry = entry
                                screenTitle = entry.button.title
                                headerVisible = true
                                container.value?.removeAllViews()
                                CmodManager.safeRun(entry) { entry.button.action.onClick(host) }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CryoTonalButton(modifier = Modifier.fillMaxWidth(), onClick = { showMods = true }) {
                    Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_extension_outlined), contentDescription = null)
                    MarqueeText(text = stringResource(R.string.cryo_cmods_launcher_mods))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            ) {
                // área do mod (sempre montada: o mod pode mostrar a View logo no clique)
                Column(modifier = Modifier.fillMaxSize()) {
                    if (openEntry != null && headerVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            openEntry?.let { e -> ModIcon(e.button.icon ?: e.modIcon, 26) }
                            Text(
                                modifier = Modifier.weight(1f),
                                text = screenTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { host.close() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                text = "✕",
                                color = CryoGlassColors.Ice,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(all = if (headerVisible) 6.dp else 0.dp),
                        factory = { ctx -> FrameLayout(ctx).also { container.value = it } }
                    )
                }
                if (openEntry == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🧩", fontSize = 40.sp)
                        Text(
                            text = stringResource(if (buttons.isEmpty()) R.string.cryo_cmods_empty else R.string.cryo_cmods_pick),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        if (buttons.isEmpty()) {
                            Text(
                                text = stringResource(R.string.cryo_cmods_empty_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            CryoButton(onClick = { showMods = true }) {
                                MarqueeText(text = stringResource(R.string.cryo_cmods_launcher_mods))
                            }
                        }
                    }
                }
            }
        }
        if (showMods) {
            LauncherModsDialog(onDismiss = { showMods = false })
        }
    }
}
