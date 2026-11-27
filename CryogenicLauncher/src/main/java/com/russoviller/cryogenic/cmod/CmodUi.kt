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

@Composable
internal fun LauncherModsContent(modifier: Modifier, onDismiss: () -> Unit, onOpenCryoMods: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { CmodManager.init(context) } }
    val mods = CmodManager.installed
    var selectedId by remember { mutableStateOf(NONE) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val selected = mods.firstOrNull { it.info.id == selectedId }

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
        Column(modifier = Modifier.width(240.dp).fillMaxHeight()) {
            Text(
                text = stringResource(R.string.cryo_cmods_launcher_mods),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                )
            )
            Text(
                text = stringResource(R.string.cryo_cmods_launcher_mods_summary),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(8.dp))
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
                }
                mods.forEach { mod ->
                    val enabled = CmodManager.isEnabled(mod.info.id)
                    val error = CmodManager.errors[mod.info.id]
                    ModTile(
                        icon = mod.icon,
                        title = mod.info.name,
                        subtitle = when {
                            error != null && enabled -> stringResource(R.string.cryo_cmods_status_error)
                            enabled -> stringResource(R.string.cryo_cmods_status_on)
                            else -> stringResource(R.string.cryo_cmods_status_off)
                        },
                        subtitleColor = when {
                            error != null && enabled -> Color(0xFFFF8A80)
                            enabled -> CryoGlassColors.Aurora
                            else -> CryoGlassColors.Frost.copy(alpha = 0.65f)
                        },
                        selected = mod.info.id == selectedId,
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
            } else {
                val info = selected.info
                val enabled = CmodManager.isEnabled(info.id)
                val error = CmodManager.errors[info.id]
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoButton(
                        modifier = Modifier.weight(1.2f),
                        enabled = !busy,
                        onClick = { CmodManager.setEnabled(info.id, !enabled) }
                    ) {
                        MarqueeText(text = stringResource(if (enabled) R.string.cryo_cmods_disable else R.string.cryo_cmods_enable))
                    }
                    CryoOutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = { confirmDelete = !confirmDelete },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(modifier = Modifier.size(18.dp), painter = painterResource(R.drawable.ic_delete_outlined), contentDescription = null)
                        MarqueeText(text = stringResource(R.string.cryo_presets_delete))
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
                            text = stringResource(R.string.cryo_cmods_delete_confirm, info.name),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CryoOutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    CmodManager.delete(info.id)
                                    selectedId = NONE
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text(text = stringResource(R.string.cryo_presets_delete)) }
                            CryoTonalButton(modifier = Modifier.weight(1f), onClick = { confirmDelete = false }) {
                                Text(text = stringResource(R.string.generic_cancel))
                            }
                        }
                    }
                }
                if (onOpenCryoMods != null && enabled) {
                    CryoTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenCryoMods) {
                        MarqueeText(text = stringResource(R.string.cryo_cmods_open_screen))
                    }
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
private fun ModTile(icon: Bitmap?, title: String, subtitle: String, subtitleColor: Color, selected: Boolean, onClick: () -> Unit) {
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
        ModIcon(icon, 36)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
