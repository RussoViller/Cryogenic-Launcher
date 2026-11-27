package com.russoviller.cryogenic.ui.screens.content.elements

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.version.mod.conflict.ModConflictChecker
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.fadeEdge
import com.russoviller.cryogenic.ui.components.rememberDialogMaxHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val Danger = Color(0xFFFF5C7A)
private val Caution = Color(0xFFFFB547)

/**
 * Aviso de mods em conflito antes de abrir a versão.
 * Cada grupo mostra os mods que brigam entre si; desativar/excluir um deles faz uma nova verificação.
 * Quando não sobra conflito, o jogo continua abrindo sozinho.
 */
@Composable
fun ModConflictDialog(
    groups: List<ModConflictChecker.Group>,
    recheck: suspend () -> List<ModConflictChecker.Group>,
    onAllSolved: () -> Unit,
    onContinueAnyway: () -> Unit,
    onCancel: () -> Unit,
    minecraft: String? = null,
    loader: com.russoviller.cryogenic.game.addons.modloader.ModLoader? = null
) {
    val context = LocalContext.current
    var versionsFor by remember { mutableStateOf<ModConflictChecker.ScannedMod?>(null) }
    val scope = rememberCoroutineScope()
    val current = remember { mutableStateListOf<ModConflictChecker.Group>().apply { addAll(groups) } }
    var busy by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.cryo_conflict_failed)

    fun act(file: File, delete: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (delete) Files.delete(file.toPath())
                    else Files.move(file.toPath(), File(file.absolutePath + ".disabled").toPath(), StandardCopyOption.REPLACE_EXISTING)
                }.isSuccess
            }
            if (!ok) Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
            val next = runCatching { recheck() }.getOrDefault(emptyList())
            busy = false
            if (next.isEmpty()) {
                onAllSolved()
            } else {
                current.clear()
                current.addAll(next)
            }
        }
    }

    fun refresh() {
        busy = true
        scope.launch {
            val next = runCatching { recheck() }.getOrDefault(emptyList())
            busy = false
            if (next.isEmpty()) onAllSolved() else {
                current.clear()
                current.addAll(next)
            }
        }
    }

    versionsFor?.let { mod ->
        ModVersionPickerDialog(
            mod = mod,
            minecraft = minecraft,
            loader = loader,
            onDismiss = { versionsFor = null },
            onInstalled = {
                versionsFor = null
                refresh()
            }
        )
    }

    Dialog(
        onDismissRequest = { if (!busy) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 720.dp)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .cryoGlass(shape = RoundedCornerShape(28.dp), cornerRadius = 28.dp, window = true)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                //Cabeçalho
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Danger, Caution))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_warning_filled),
                            contentDescription = null,
                            tint = CryoGlassColors.Deep
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cryo_conflict_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.cryo_conflict_summary),
                            style = MaterialTheme.typography.labelMedium,
                            color = CryoGlassColors.Frost.copy(alpha = 0.85f)
                        )
                    }
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CryoGlassColors.Ice, strokeWidth = 2.dp)
                    }
                }

                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fadeEdge(state = listState),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(current, key = { _, g -> g.mods.joinToString { it.file.absolutePath } }) { index, group ->
                        ConflictGroupCard(
                            number = index + 1,
                            group = group,
                            enabled = !busy,
                            onDisable = { act(it, delete = false) },
                            onDelete = { act(it, delete = true) },
                            onVersions = { versionsFor = it }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CryoTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = onCancel
                    ) {
                        MarqueeText(text = stringResource(R.string.generic_cancel))
                    }
                    //Prosseguir mesmo assim: botão de alerta (não é a ação principal)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .border(1.5.dp, Danger, CircleShape)
                            .background(Danger.copy(alpha = 0.12f))
                            .clickable(enabled = !busy, onClick = onContinueAnyway)
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MarqueeText(
                            text = stringResource(R.string.cryo_conflict_continue),
                            color = Danger,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictGroupCard(
    number: Int,
    group: ModConflictChecker.Group,
    enabled: Boolean,
    onDisable: (File) -> Unit,
    onDelete: (File) -> Unit,
    onVersions: (ModConflictChecker.ScannedMod) -> Unit
) {
    val accent = if (group.softOnly) Caution else Danger
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CryoGlassColors.Deep.copy(alpha = 0.5f))
            .border(1.dp, accent.copy(alpha = 0.6f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.cryo_conflict_group, number),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                text = stringResource(if (group.softOnly) R.string.cryo_conflict_soft else R.string.cryo_conflict_blocking),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        //Motivos
        group.links.forEach { link ->
            Text(
                text = "• " + reasonText(link),
                style = MaterialTheme.typography.labelMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.9f)
            )
        }
        //Aviso: algum mod do grupo é dependência de outros mods
        if (group.dependents.isNotEmpty()) {
            Text(
                text = "⚠ " + stringResource(R.string.cryo_conflict_dependents_note),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Caution
            )
        }
        //Mods do grupo
        group.mods.forEach { mod ->
            ConflictModRow(
                neededBy = group.dependents[mod.file].orEmpty().map { it.name },
                mod = mod,
                enabled = enabled,
                onDisable = { onDisable(mod.file) },
                onDelete = { onDelete(mod.file) },
                onVersions = { onVersions(mod) }
            )
        }
    }
}

@Composable
private fun reasonText(link: ModConflictChecker.Link): String {
    val a = link.mods.getOrNull(0)?.name.orEmpty()
    val b = link.mods.getOrNull(1)?.name.orEmpty()
    return when (link.kind) {
        ModConflictChecker.Kind.DECLARED -> stringResource(R.string.cryo_conflict_declared, a, b)
        ModConflictChecker.Kind.DECLARED_SOFT -> stringResource(R.string.cryo_conflict_declared_soft, a, b)
        ModConflictChecker.Kind.DUPLICATE -> stringResource(R.string.cryo_conflict_duplicate, link.note ?: a)
        ModConflictChecker.Kind.KNOWN -> stringResource(
            if (link.note == "shaders") R.string.cryo_conflict_known_shaders else R.string.cryo_conflict_known_renderer,
            a, b
        )
    }
}

@Composable
private fun ConflictModRow(
    neededBy: List<String> = emptyList(),
    mod: ModConflictChecker.ScannedMod,
    enabled: Boolean,
    onDisable: () -> Unit,
    onDelete: () -> Unit,
    onVersions: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    var confirmDelete by remember(mod.file) { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, ice.copy(alpha = 0.2f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mod.name + if (mod.version.isNotBlank() && !mod.version.contains("\${")) "  ${mod.version}" else "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mod.file.name,
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (neededBy.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.cryo_dep_needed_by, neededBy.joinToString(", ")),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Caution,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .border(1.dp, ice.copy(alpha = 0.7f), CircleShape)
                .clickable(enabled = enabled, onClick = onVersions)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            text = stringResource(R.string.cryo_conflict_versions),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = ice,
            maxLines = 1
        )
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .background(ice)
                .clickable(enabled = enabled, onClick = onDisable)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            text = stringResource(R.string.cryo_conflict_disable),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Deep,
            maxLines = 1
        )
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (confirmDelete) Danger else Color.Transparent)
                .border(1.dp, Danger, CircleShape)
                .clickable(enabled = enabled) {
                    if (confirmDelete) onDelete() else confirmDelete = true
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_delete_outlined),
                contentDescription = null,
                tint = if (confirmDelete) CryoGlassColors.Deep else Danger
            )
            Text(
                text = stringResource(if (confirmDelete) R.string.cryo_conflict_delete_confirm else R.string.cryo_conflict_delete),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = if (confirmDelete) CryoGlassColors.Deep else Danger,
                maxLines = 1
            )
        }
    }
}


/**
 * Escolher outra versão do mod (mais nova ou mais antiga) feita para este Minecraft/carregador.
 * Ao instalar, o arquivo atual é apagado e o novo entra no lugar.
 */
@Composable
private fun ModVersionPickerDialog(
    mod: ModConflictChecker.ScannedMod,
    minecraft: String?,
    loader: com.russoviller.cryogenic.game.addons.modloader.ModLoader?,
    onDismiss: () -> Unit,
    onInstalled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ice = CryoGlassColors.Ice
    var loading by remember { mutableStateOf(true) }
    var listing by remember { mutableStateOf<com.russoviller.cryogenic.game.version.mod.conflict.ModVersionSwap.Listing?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.cryo_conflict_versions_install_failed)

    androidx.compose.runtime.LaunchedEffect(mod.file) {
        listing = com.russoviller.cryogenic.game.version.mod.conflict.ModVersionSwap.list(mod.file, minecraft, loader)
        loading = false
    }

    Dialog(
        onDismissRequest = { if (!installing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 640.dp)
                .heightIn(max = rememberDialogMaxHeight())
                .cryoGlass(shape = RoundedCornerShape(28.dp), cornerRadius = 28.dp, window = true)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_conflict_versions_title, listing?.title ?: mod.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.cryo_conflict_versions_summary, minecraft ?: "?", loader?.displayName ?: "?"),
                style = MaterialTheme.typography.labelMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
            val options = listing?.options.orEmpty()
            when {
                loading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ice)
                }
                listing == null -> Text(
                    text = stringResource(R.string.cryo_conflict_versions_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Caution
                )
                options.none { !it.current } -> Text(
                    text = stringResource(R.string.cryo_conflict_versions_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Caution
                )
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fadeEdge(state = listState),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(options, key = { _, o -> o.version.platformId() }) { _, option ->
                            val v = option.version
                            val active = selected == v.platformId()
                            val shape = RoundedCornerShape(14.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .background(if (active) ice.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
                                    .border(if (active) 1.5.dp else 1.dp, ice.copy(alpha = if (active) 1f else 0.2f), shape)
                                    .clickable(enabled = !option.current && !installing) { selected = v.platformId() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = v.platformVersion().ifBlank { v.platformDisplayName() },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = v.platformFileName(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CryoGlassColors.Frost.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val type = v.platformReleaseType()
                                Text(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(1.dp, type.color, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    text = stringResource(type.textRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = type.color
                                )
                                if (option.current) {
                                    Text(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(ice)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        text = stringResource(R.string.cryo_conflict_versions_current),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = CryoGlassColors.Deep
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !installing,
                    onClick = onDismiss
                ) {
                    MarqueeText(text = stringResource(R.string.generic_cancel))
                }
                val chosen = options.firstOrNull { it.version.platformId() == selected }
                com.russoviller.cryogenic.ui.components.CryoButton(
                    modifier = Modifier.weight(1f),
                    enabled = chosen != null && !installing,
                    onClick = {
                        val option = chosen ?: return@CryoButton
                        installing = true
                        scope.launch {
                            val ok = com.russoviller.cryogenic.game.version.mod.conflict.ModVersionSwap.install(mod.file, option.version)
                            installing = false
                            if (ok != null) onInstalled() else Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    if (installing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        MarqueeText(text = stringResource(R.string.cryo_conflict_versions_install))
                    }
                }
            }
        }
    }
}
