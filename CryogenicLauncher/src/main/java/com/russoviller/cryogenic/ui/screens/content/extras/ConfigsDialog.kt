package com.russoviller.cryogenic.ui.screens.content.extras

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.extras.CryoConfig
import com.russoviller.cryogenic.extras.CryoConfigs
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoOutlinedButton
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleTextInputField
import com.russoviller.cryogenic.ui.components.cryoGlass
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.text.DateFormat
import java.util.Date

/** Item especial da lista: as configurações que estão em uso agora (para salvar). */
private const val CURRENT = "__current__"

/**
 * Janela "Salvar Configurações" (Funções Extras): mesmo formato das Predefinições.
 * Salva todas as configurações do Launcher, menos wallpapers/opacidade/volume (são das Predefinições),
 * e exporta/importa como arquivo .cryoconf.
 */
@Composable
fun ConfigsDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigsDialogContent(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(),
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun ConfigsDialogContent(modifier: Modifier, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configs = CryoConfigs.configs
    var selectedId by remember { mutableStateOf(CURRENT) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { runCatching { CryoConfigs.load() } }
    //Valores atuais (para o "em uso"): relidos quando algo é salvo/aplicado
    var current by remember { mutableStateOf<Map<String, JsonElement>>(emptyMap()) }
    LaunchedEffect(refresh, configs) { current = runCatching { CryoConfigs.snapshot() }.getOrDefault(emptyMap()) }

    val selected = configs.firstOrNull { it.id == selectedId }
    if (selectedId != CURRENT && selected == null) selectedId = CURRENT

    val savedText = stringResource(R.string.cryo_configs_saved)
    val errorText = stringResource(R.string.cryo_presets_error)
    val exportedText = stringResource(R.string.cryo_configs_exported)
    val importedText = stringResource(R.string.cryo_configs_imported)
    val wrongText = stringResource(R.string.cryo_configs_import_wrong)
    val invalidText = stringResource(R.string.cryo_configs_import_invalid)

    var exportTarget by remember { mutableStateOf<CryoConfig?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val config = exportTarget
        exportTarget = null
        if (uri != null && config != null) {
            busy = true
            scope.launch {
                val result = runCatching { CryoConfigs.export(context, config, uri) }
                busy = false
                message = if (result.isSuccess) exportedText else errorText
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            message = null
            scope.launch {
                val result = runCatching { CryoConfigs.import(context, uri) }
                busy = false
                result.onSuccess { selectedId = it.id; message = importedText }
                    .onFailure {
                        message = when (it) {
                            is CryoConfigs.WrongExtension -> wrongText
                            is CryoConfigs.InvalidConfigFile -> invalidText
                            else -> errorText
                        }
                    }
            }
        }
    }

    run {
        Row(
            modifier = modifier
                .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, opaque = true)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------------- Lista ----------------
            Column(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = stringResource(R.string.cryo_configs_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Text(
                    text = stringResource(R.string.cryo_configs_pick),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ConfigTile(
                        title = stringResource(R.string.cryo_configs_current),
                        subtitle = stringResource(R.string.cryo_configs_current_hint),
                        selected = selectedId == CURRENT,
                        badge = "+",
                        onClick = { selectedId = CURRENT; message = null }
                    )
                    if (CryoConfigs.loaded && configs.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            text = stringResource(R.string.cryo_configs_empty),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                        )
                    }
                    configs.forEach { config ->
                        ConfigTile(
                            title = config.name,
                            subtitle = formatDate(config.createdAt),
                            selected = config.id == selectedId,
                            badge = if (CryoConfigs.isCurrent(config, current)) "✓" else null,
                            onClick = { selectedId = config.id; message = null }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = stringResource(R.string.cryo_configs_count, configs.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Ice
                )
                Spacer(Modifier.height(6.dp))
                CryoOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = { runCatching { importLauncher.launch(arrayOf("*/*")) } }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_download_2_filled),
                        contentDescription = null
                    )
                    MarqueeText(text = stringResource(R.string.cryo_configs_import))
                }
                Spacer(Modifier.height(6.dp))
                CryoTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.generic_close))
                }
            }

            // ---------------- Detalhes e ações ----------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selected == null) {
                    CurrentConfigPanel(
                        values = current,
                        busy = busy,
                        onSave = { name ->
                            busy = true
                            message = null
                            scope.launch {
                                val result = runCatching { CryoConfigs.saveCurrent(name) }
                                busy = false
                                refresh++
                                result.onSuccess { selectedId = it.id; message = savedText }
                                    .onFailure { message = errorText }
                            }
                        }
                    )
                } else {
                    ConfigPanel(
                        config = selected,
                        inUse = CryoConfigs.isCurrent(selected, current),
                        busy = busy,
                        onApply = {
                            busy = true
                            message = null
                            scope.launch {
                                val result = runCatching { CryoConfigs.apply(selected) }
                                busy = false
                                refresh++
                                message = result.fold(
                                    onSuccess = { context.getString(R.string.cryo_configs_applied, it) },
                                    onFailure = { errorText }
                                )
                            }
                        },
                        onExport = {
                            exportTarget = selected
                            message = null
                            runCatching { exportLauncher.launch(CryoConfigs.exportFileName(selected)) }
                                .onFailure { exportTarget = null; message = errorText }
                        },
                        onRename = { name -> scope.launch { CryoConfigs.rename(selected.id, name) } },
                        onDelete = {
                            busy = true
                            scope.launch {
                                runCatching { CryoConfigs.delete(selected.id) }
                                busy = false
                                selectedId = CURRENT
                            }
                        }
                    )
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

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
                        .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    text = stringResource(R.string.cryo_configs_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost
                )
            }
        }
    }
}

private fun formatDate(time: Long): String =
    runCatching { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time)) }.getOrDefault("")

@Composable
private fun ConfigTile(title: String, subtitle: String, selected: Boolean, badge: String?, onClick: () -> Unit) {
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
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora)))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_settings_filled),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
            if (badge != null) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Deep)
                        .padding(horizontal = 4.dp),
                    text = badge,
                    color = CryoGlassColors.Ice,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
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
                color = if (selected) Color.White.copy(alpha = 0.85f) else CryoGlassColors.Frost.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Cabeçalho com nome, situação e os destaques (renderizador, driver, Liquid Glass...). */
@Composable
private fun ConfigHeader(title: String, status: String, statusColor: Color, values: Map<String, JsonElement>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.cryo_configs_highlights).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
            color = CryoGlassColors.Ice
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val default = stringResource(R.string.cryo_configs_default)
        val on = stringResource(R.string.cryo_configs_on)
        val off = stringResource(R.string.cryo_configs_off)
        fun text(key: String): String = when (val v = values[key]) {
            null, is JsonNull -> default
            is JsonPrimitive -> v.booleanOrNull?.let { if (it) on else off } ?: v.content.ifBlank { default }
            else -> v.toString()
        }
        HighlightRow(stringResource(R.string.cryo_configs_renderer), text("renderer"))
        HighlightRow(stringResource(R.string.cryo_configs_driver), text("vulkanDriver"))
        HighlightRow(stringResource(R.string.cryo_configs_liquid), text("cryoInterface2"))
        HighlightRow(stringResource(R.string.cryo_configs_resolution), text("resolutionRatio").let { if (it == default) it else "$it%" })
        HighlightRow(stringResource(R.string.cryo_configs_ram), text("ramAllocation").let { if (it == default) it else "$it MB" })
        HighlightRow(stringResource(R.string.cryo_configs_theme), text("launcherColorTheme"))
        Text(
            text = stringResource(R.string.cryo_configs_items, values.size),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora)))
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun HighlightRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.width(130.dp),
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = CryoGlassColors.Frost.copy(alpha = 0.8f),
            maxLines = 1
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CurrentConfigPanel(values: Map<String, JsonElement>, busy: Boolean, onSave: (String) -> Unit) {
    val base = stringResource(R.string.cryo_configs_default_name)
    var name by remember(CryoConfigs.configs.size) { mutableStateOf(CryoConfigs.suggestedName(base)) }
    ConfigHeader(
        title = stringResource(R.string.cryo_configs_current),
        status = stringResource(R.string.cryo_presets_current_status),
        statusColor = CryoGlassColors.Aurora,
        values = values
    )
    ConfigSection(stringResource(R.string.cryo_configs_save)) {
        Text(
            text = stringResource(R.string.cryo_configs_name_label),
            style = MaterialTheme.typography.labelMedium,
            color = CryoGlassColors.Frost
        )
        SimpleTextInputField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it.take(CryoConfigs.NAME_MAX) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            hint = {
                Text(
                    text = stringResource(R.string.cryo_configs_name_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CryoGlassColors.Frost.copy(alpha = 0.5f)
                )
            }
        )
        CryoButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && name.isNotBlank(),
            onClick = { onSave(name) }
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_save_filled),
                contentDescription = null
            )
            MarqueeText(text = stringResource(R.string.cryo_configs_save))
        }
        Text(
            text = stringResource(R.string.cryo_configs_save_summary),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun ConfigPanel(
    config: CryoConfig,
    inUse: Boolean,
    busy: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var renaming by remember(config.id) { mutableStateOf(false) }
    var newName by remember(config.id) { mutableStateOf(config.name) }
    var confirmDelete by remember(config.id) { mutableStateOf(false) }

    ConfigHeader(
        title = config.name,
        status = stringResource(if (inUse) R.string.cryo_configs_in_use else R.string.cryo_configs_not_in_use),
        statusColor = if (inUse) CryoGlassColors.Aurora else CryoGlassColors.Ice,
        values = config.values
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CryoButton(
            modifier = Modifier.weight(1.3f),
            enabled = !busy,
            onClick = onApply
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null
            )
            MarqueeText(text = stringResource(R.string.cryo_configs_apply))
        }
        CryoTonalButton(
            modifier = Modifier.weight(1f),
            enabled = !busy,
            onClick = onExport
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_file_export_outlined),
                contentDescription = null
            )
            MarqueeText(text = stringResource(R.string.cryo_configs_export))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CryoTonalButton(
            modifier = Modifier.weight(1f),
            enabled = !busy,
            onClick = { renaming = !renaming; confirmDelete = false; newName = config.name }
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_edit_outlined),
                contentDescription = null
            )
            MarqueeText(text = stringResource(R.string.cryo_presets_rename))
        }
        CryoOutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = !busy,
            onClick = { confirmDelete = !confirmDelete; renaming = false },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_delete_outlined),
                contentDescription = null
            )
            MarqueeText(text = stringResource(R.string.cryo_presets_delete))
        }
    }

    if (renaming) {
        ConfigSection(stringResource(R.string.cryo_presets_rename)) {
            SimpleTextInputField(
                modifier = Modifier.fillMaxWidth(),
                value = newName,
                onValueChange = { newName = it.take(CryoConfigs.NAME_MAX) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoButton(
                    modifier = Modifier.weight(1f),
                    enabled = newName.isNotBlank(),
                    onClick = { onRename(newName); renaming = false }
                ) { Text(text = stringResource(R.string.generic_save)) }
                CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { renaming = false }
                ) { Text(text = stringResource(R.string.generic_cancel)) }
            }
        }
    }

    if (confirmDelete) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_configs_delete_confirm, config.name),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoOutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(text = stringResource(R.string.cryo_presets_delete)) }
                CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDelete = false }
                ) { Text(text = stringResource(R.string.generic_cancel)) }
            }
        }
    }
}

@Composable
private fun ConfigSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
            color = CryoGlassColors.Ice
        )
        content()
    }
}
