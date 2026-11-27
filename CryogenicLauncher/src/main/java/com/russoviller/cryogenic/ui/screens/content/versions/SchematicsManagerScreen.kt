package com.russoviller.cryogenic.ui.screens.content.versions

import com.russoviller.cryogenic.ui.components.CryoSelectableOverlay
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.context.getFileName
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.ui.androidText
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.SimpleAlertDialog
import com.russoviller.cryogenic.ui.components.animated
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.rememberAnimatedContext
import com.russoviller.cryogenic.ui.screens.NestedNavKey
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey
import com.russoviller.cryogenic.utils.file.formatFileSize
import com.russoviller.cryogenic.utils.string.getMessageOrToString
import com.russoviller.cryogenic.viewmodel.ErrorViewModel
import com.russoviller.cryogenic.viewmodel.EventViewModel
import com.russoviller.cryogenic.viewmodel.sendToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

private val SchematicExtensions = setOf("litematic", "schem", "schematic", "nbt")

private fun listSchematics(dir: File): List<File> =
    dir.listFiles { f -> f.isFile && f.extension.lowercase() in SchematicExtensions }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

@Composable
fun SchematicsManagerScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val schematicsDir = remember(version) { File(version.getGameDir(), "schematics") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    var selecting by remember { mutableStateOf(false) }
    val selectedNames = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var confirmBulkDelete by remember { mutableStateOf(false) }

    val files by produceState(emptyList<File>(), schematicsDir, refreshKey) {
        value = withContext(Dispatchers.IO) { listSchematics(schematicsDir) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            var imported = 0
            var skipped = 0
            try {
                withContext(Dispatchers.IO) {
                    schematicsDir.mkdirs()
                    uris.forEach { uri ->
                        val name = context.getFileName(uri)
                        if (name == null || name.substringAfterLast('.', "").lowercase() !in SchematicExtensions) {
                            skipped++
                            return@forEach
                        }
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            File(schematicsDir, name).outputStream().use { output -> input.copyTo(output) }
                            imported++
                        }
                    }
                }
                if (imported > 0) {
                    eventViewModel.sendToast(androidText(R.string.cryo_schematics_imported, imported))
                }
                if (skipped > 0) {
                    eventViewModel.sendToast(androidText(R.string.cryo_schematics_skipped, skipped))
                }
            } catch (e: Exception) {
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.cryo_schematics_failed),
                        message = androidText(e.getMessageOrToString())
                    )
                )
            } finally {
                importing = false
                refreshKey++
            }
        }
    }

    if (confirmBulkDelete) {
        SimpleAlertDialog(
            title = stringResource(R.string.generic_warning),
            text = stringResource(R.string.cryo_selection_delete_confirm, selectedNames.size),
            onConfirm = {
                confirmBulkDelete = false
                val targets = selectedNames.map { File(schematicsDir, it) }
                selectedNames.clear()
                selecting = false
                scope.launch {
                    withContext(Dispatchers.IO) { targets.forEach { it.delete() } }
                    refreshKey++
                }
            },
            onDismiss = { confirmBulkDelete = false }
        )
    }

    pendingDelete?.let { file ->
        SimpleAlertDialog(
            title = stringResource(R.string.generic_warning),
            text = stringResource(R.string.cryo_schematics_delete, file.name),
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    withContext(Dispatchers.IO) { file.delete() }
                    refreshKey++
                }
            },
            onDismiss = { pendingDelete = null }
        )
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.SchematicsManager, versionsScreenKey, false)
    ) { isVisible ->
        val ctx = rememberAnimatedContext(isVisible = isVisible)
        LaunchedEffect(isVisible) { if (isVisible) refreshKey++ }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .animated(ctx),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.russoviller.cryogenic.ui.screens.content.versions.layouts.CryoTacticalInfoPanel(
                iconRes = R.drawable.ic_package_2_outlined,
                title = stringResource(R.string.cryo_schematics),
                description = stringResource(R.string.cryo_schematics_summary),
                folder = "schematics",
                tips = emptyList(),
                extra = stringResource(R.string.cryo_schematics_count, files.size),
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
            ) {
                CryoButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !importing,
                    onClick = { picker.launch(arrayOf("*/*")) }
                ) {
                    if (importing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CryoGlassColors.Deep)
                    } else {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null
                        )
                    }
                    Text(text = stringResource(R.string.cryo_schematics_import))
                }
                CryoTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        schematicsDir.mkdirs()
                        eventViewModel.sendEvent(
                            EventViewModel.Event.OpenFileManager(
                                rootPath = version.getGameDir().absolutePath,
                                currentPath = schematicsDir.absolutePath
                            )
                        )
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_folder_filled),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.cryo_schematics_open_folder))
                }
                CryoTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = files.isNotEmpty(),
                    onClick = {
                        selecting = !selecting
                        if (!selecting) selectedNames.clear()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_list_alt_check_outlined),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.cryo_selection_mode), maxLines = 1)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                com.russoviller.cryogenic.ui.components.CryoSelectionBar(
                    visible = selecting && files.isNotEmpty(),
                    count = selectedNames.size,
                    total = files.size,
                    onSelectAll = {
                        if (selectedNames.size == files.size) selectedNames.clear() else {
                            selectedNames.clear()
                            selectedNames.addAll(files.map { it.name })
                        }
                    },
                    onDelete = { if (selectedNames.isNotEmpty()) confirmBulkDelete = true },
                    onCancel = {
                        selecting = false
                        selectedNames.clear()
                    }
                )
                if (files.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .cryoGlass(shape = RoundedCornerShape(20.dp), glow = false),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.cryo_schematics_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(files, key = { it.name }) { file ->
                            Box {
                                SchematicItem(file = file, onDelete = { pendingDelete = file })
                                CryoSelectableOverlay(
                                    selecting = selecting,
                                    selected = file.name in selectedNames,
                                    onToggle = {
                                        if (file.name in selectedNames) selectedNames.remove(file.name) else selectedNames.add(file.name)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchematicItem(file: File, onDelete: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    val date = remember(file) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(file.lastModified())) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cryoGlass(shape = RoundedCornerShape(16.dp), glow = false)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = file.extension.uppercase().take(5),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice,
                maxLines = 1
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.nameWithoutExtension,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatFileSize(file.length())} • $date",
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(error.copy(alpha = 0.15f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_delete_filled),
                contentDescription = stringResource(R.string.generic_delete),
                tint = error
            )
        }
    }
}
