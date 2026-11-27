package com.russoviller.cryogenic.ui.screens.content.extras

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.russoviller.cryogenic.extras.CryoPreset
import com.russoviller.cryogenic.extras.CryoPresets
import com.russoviller.cryogenic.extras.CryoWallpaperLibrary
import com.russoviller.cryogenic.extras.WallpaperRef
import com.russoviller.cryogenic.filemanager.ui.components.HeroWallpaperStore
import com.russoviller.cryogenic.filemanager.ui.components.FmWallpaperStore
import com.russoviller.cryogenic.filemanager.ui.components.PortraitWallpaperStore
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.isLauncherInDarkTheme
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoColorAdjust
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoOutlinedButton
import com.russoviller.cryogenic.ui.components.CryoSpec
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.SimpleTextInputField
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoSpecFor
import com.russoviller.cryogenic.ui.screens.content.settings.themeTypeName
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import com.russoviller.cryogenic.ui.theme.launcherColorScheme
import com.russoviller.cryogenic.viewmodel.LocalBackgroundViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Item especial da lista: o visual que está na tela agora (para salvar como predefinição). */
private const val CURRENT = "__current__"

/** Os wallpapers de um visual, já como miniaturas. */
private class LookThumbs(
    val background: ImageBitmap?, val hero: ImageBitmap?, val portrait: ImageBitmap?, val fm: ImageBitmap? = null
)

/** Dados de um visual para a prévia e para a lista (predefinição salva ou visual atual). */
private class Look(
    val type: ColorThemeType,
    val customColor: Int,
    val palette: com.materialkolor.PaletteStyle,
    val saturation: Int,
    val vibrance: Int,
    val contrast: Int,
    val animated: Boolean,
    val background: WallpaperRef?,
    val hero: WallpaperRef?,
    val portrait: WallpaperRef?
)

private fun CryoPreset.toLook() = Look(
    themeType, customColor, palette, saturation, vibrance, contrast, gradientAnimated, background, hero, portrait
)

/**
 * Janela de Predefinições: mesmo formato do Estúdio de temas. À esquerda a lista
 * (visual atual + predefinições salvas), à direita a prévia e as ações.
 */
@Composable
fun PresetsDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val background = LocalBackgroundViewModel.current
    val presets = CryoPresets.presets
    var selectedId by remember { mutableStateOf(CURRENT) }
    var busy by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var activeId by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        CryoPresets.load()
        HeroWallpaperStore.ensureLoaded()
        PortraitWallpaperStore.ensureLoaded()
        //O gerenciador de arquivos roda em outro processo: relê o arquivo dele sempre que a janela abre
        FmWallpaperStore.refresh()
        AllSettings.cryoFmBackgroundOpacity.getValue()
        AllSettings.cryoFmWallpaperVolume.getValue()
    }
    //O "em uso" depende do visual atual: relê sempre que algo muda
    val settingsKey = listOf(
        AllSettings.launcherColorTheme.state, AllSettings.launcherCustomColor.state, AllSettings.cryoThemeSaturation.state,
        AllSettings.cryoThemeVibrance.state, AllSettings.cryoThemeContrast.state, AllSettings.cryoGradientAnimated.state,
        AllSettings.launcherBackgroundOpacity.state, AllSettings.cryoActivePresetId.state, AllSettings.cryoActivePresetSignature.state,
        HeroWallpaperStore.generation, PortraitWallpaperStore.generation, FmWallpaperStore.generation,
        background?.refreshTrigger, refresh
    )
    LaunchedEffect(settingsKey) { activeId = CryoPresets.activeId() }

    val selected = presets.firstOrNull { it.id == selectedId }
    if (selectedId != CURRENT && selected == null) selectedId = CURRENT

    val context = LocalContext.current
    val exportedText = stringResource(R.string.cryo_presets_exported)
    val importedText = stringResource(R.string.cryo_presets_imported)
    val invalidText = stringResource(R.string.cryo_presets_import_invalid)
    var exportTarget by remember { mutableStateOf<CryoPreset?>(null) }
    //Exportar: o seletor do sistema deixa escolher a pasta; o nome já vem preenchido (ex.: Russo-teste.cryo)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val preset = exportTarget
        exportTarget = null
        if (uri != null && preset != null) {
            busy = R.string.cryo_presets_exporting
            message = null
            scope.launch {
                val result = runCatching { CryoPresets.export(context, preset, uri) }
                busy = null
                message = if (result.isSuccess) exportedText else null
                if (result.isFailure) message = context.getString(R.string.cryo_presets_error)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = R.string.cryo_presets_importing
            message = null
            scope.launch {
                val result = runCatching { CryoPresets.import(context, uri) }
                busy = null
                result.onSuccess { selectedId = it.id; message = importedText; refresh++ }
                    .onFailure { message = if (it is CryoPresets.InvalidCryoFile) invalidText else context.getString(R.string.cryo_presets_error) }
            }
        }
    }

    val savedText = stringResource(R.string.cryo_presets_saved)
    val appliedText = stringResource(R.string.cryo_presets_applied)
    val errorText = stringResource(R.string.cryo_presets_error)

    Dialog(
        onDismissRequest = { if (busy == null) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight()
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
                    text = stringResource(R.string.cryo_presets_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Text(
                    text = stringResource(R.string.cryo_presets_pick),
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
                    CurrentLookTile(
                        selected = selectedId == CURRENT,
                        refresh = refresh,
                        onClick = { selectedId = CURRENT; message = null }
                    )
                    if (CryoPresets.loaded && presets.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            text = stringResource(R.string.cryo_presets_empty),
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoGlassColors.Frost.copy(alpha = 0.7f)
                        )
                    }
                    presets.forEach { preset ->
                        PresetTile(
                            preset = preset,
                            selected = preset.id == selectedId,
                            active = preset.id == activeId,
                            onClick = { selectedId = preset.id; message = null }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = stringResource(R.string.cryo_presets_count, presets.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Ice
                )
                Spacer(Modifier.height(6.dp))
                CryoOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = busy == null,
                    onClick = { importLauncher.launch(arrayOf("*/*")) }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_download_2_filled),
                        contentDescription = null
                    )
                    MarqueeText(text = stringResource(R.string.cryo_presets_import))
                }
                Spacer(Modifier.height(6.dp))
                CryoTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = busy == null,
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.generic_close))
                }
            }

            // ---------------- Prévia e ações ----------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selected == null) {
                    CurrentLookPanel(
                        refresh = refresh,
                        busy = busy != null,
                        onSave = { name ->
                            busy = R.string.cryo_presets_saving
                            message = null
                            scope.launch {
                                val result = runCatching { CryoPresets.saveCurrent(name) }
                                busy = null
                                result.onSuccess { selectedId = it.id; message = savedText; refresh++ }
                                    .onFailure { message = errorText }
                            }
                        }
                    )
                } else {
                    PresetPanel(
                        preset = selected,
                        active = selected.id == activeId,
                        busy = busy != null,
                        onApply = {
                            busy = R.string.cryo_presets_applying
                            message = null
                            scope.launch {
                                val result = runCatching { CryoPresets.apply(selected, background) }
                                busy = null
                                refresh++
                                message = if (result.isSuccess) appliedText else errorText
                            }
                        },
                        onExport = {
                            exportTarget = selected
                            message = null
                            runCatching { exportLauncher.launch(CryoPresets.exportFileName(selected)) }
                                .onFailure { exportTarget = null; message = errorText }
                        },
                        onRename = { name -> scope.launch { CryoPresets.rename(selected.id, name) } },
                        onDelete = {
                            busy = R.string.cryo_presets_deleting
                            message = null
                            scope.launch {
                                runCatching { CryoPresets.delete(selected.id) }
                                busy = null
                                selectedId = CURRENT
                            }
                        }
                    )
                }

                val status = busy?.let { stringResource(it) } ?: message
                if (status != null) {
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
                    text = stringResource(R.string.cryo_presets_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost
                )
            }
        }
    }
}

// =====================================================================
// Visual atual
// =====================================================================

/** Visual que está na tela agora, montado a partir das configurações e dos 3 arquivos. */
@Composable
private fun rememberCurrentLook(refresh: Int): Pair<Look, LookThumbs> {
    val background = LocalBackgroundViewModel.current
    val bgValid = background?.isValid == true
    val bgVideo = background?.isVideo == true
    val look = Look(
        type = AllSettings.launcherColorTheme.state,
        customColor = AllSettings.launcherCustomColor.state,
        palette = AllSettings.launcherCustomPaletteStyle.state,
        saturation = AllSettings.cryoThemeSaturation.state,
        vibrance = AllSettings.cryoThemeVibrance.state,
        contrast = AllSettings.cryoThemeContrast.state,
        animated = AllSettings.cryoGradientAnimated.state,
        background = null,
        hero = null,
        portrait = null
    )
    val thumbs by produceState(
        LookThumbs(null, null, null),
        bgValid, bgVideo, background?.refreshTrigger,
        HeroWallpaperStore.generation, PortraitWallpaperStore.generation, FmWallpaperStore.generation, refresh
    ) {
        value = withContext(Dispatchers.IO) {
            fun thumb(file: File, valid: Boolean, video: Boolean) =
                if (valid) CryoWallpaperLibrary.decodeThumb(file, video, 480)?.asImageBitmap() else null
            LookThumbs(
                thumb(PathManager.FILE_LAUNCHER_BACKGROUND, bgValid, bgVideo),
                thumb(HeroWallpaperStore.file, HeroWallpaperStore.isValid, HeroWallpaperStore.isVideo),
                thumb(PortraitWallpaperStore.file, PortraitWallpaperStore.isValid, PortraitWallpaperStore.isVideo),
                thumb(FmWallpaperStore.file, FmWallpaperStore.isValid, FmWallpaperStore.isVideo)
            )
        }
    }
    return look to thumbs
}

@Composable
private fun CurrentLookTile(selected: Boolean, refresh: Int, onClick: () -> Unit) {
    val (look, thumbs) = rememberCurrentLook(refresh)
    val spec = rememberLookSpec(look)
    ListTile(
        spec = spec,
        selected = selected,
        thumb = thumbs.background,
        title = stringResource(R.string.cryo_presets_current),
        subtitle = stringResource(R.string.cryo_presets_current_hint),
        badgeIcon = R.drawable.ic_add,
        onClick = onClick
    )
}

@Composable
private fun CurrentLookPanel(refresh: Int, busy: Boolean, onSave: (String) -> Unit) {
    val (look, thumbs) = rememberCurrentLook(refresh)
    val spec = rememberLookSpec(look)
    val base = stringResource(R.string.cryo_presets_default_name)
    var name by remember(CryoPresets.presets.size) { mutableStateOf(CryoPresets.suggestedName(base)) }

    PreviewHeader(
        spec = spec,
        look = look,
        thumbs = thumbs,
        label = stringResource(R.string.cryo_presets_preview),
        title = stringResource(R.string.cryo_presets_current),
        status = stringResource(R.string.cryo_presets_current_status),
        statusColor = spec.aurora
    )
    Section(stringResource(R.string.cryo_presets_save_title)) {
        Text(
            text = stringResource(R.string.cryo_presets_name_label),
            style = MaterialTheme.typography.labelMedium,
            color = CryoGlassColors.Frost
        )
        SimpleTextInputField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it.take(CryoPresets.NAME_MAX) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            hint = {
                Text(
                    text = stringResource(R.string.cryo_presets_name_hint),
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
            MarqueeText(text = stringResource(R.string.cryo_presets_save))
        }
        Text(
            text = stringResource(R.string.cryo_presets_save_summary),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.75f)
        )
    }
    SlotsSection(
        thumbs = thumbs,
        bgVideo = LocalBackgroundViewModel.current?.isVideo == true,
        heroVideo = HeroWallpaperStore.isVideo,
        portraitVideo = PortraitWallpaperStore.isVideo,
        fmVideo = FmWallpaperStore.isVideo
    )
}

// =====================================================================
// Predefinição salva
// =====================================================================

@Composable
private fun rememberPresetThumbs(preset: CryoPreset): LookThumbs {
    val thumbs by produceState(LookThumbs(null, null, null), preset.background, preset.hero, preset.portrait, preset.fm) {
        value = withContext(Dispatchers.IO) {
            fun load(ref: WallpaperRef?) = ref?.let {
                runCatching { BitmapFactory.decodeFile(CryoWallpaperLibrary.thumb(it.id).absolutePath)?.asImageBitmap() }.getOrNull()
            }
            LookThumbs(load(preset.background), load(preset.hero), load(preset.portrait), load(preset.fm))
        }
    }
    return thumbs
}

@Composable
internal fun PresetTile(preset: CryoPreset, selected: Boolean, active: Boolean, onClick: () -> Unit) {
    val look = remember(preset) { preset.toLook() }
    val spec = rememberLookSpec(look)
    val thumbs = rememberPresetThumbs(preset)
    ListTile(
        spec = spec,
        selected = selected,
        thumb = thumbs.background ?: thumbs.hero ?: thumbs.portrait,
        title = preset.name,
        subtitle = themeTypeName(preset.themeType),
        badgeIcon = if (active) R.drawable.ic_check else null,
        onClick = onClick
    )
}

@Composable
private fun PresetPanel(
    preset: CryoPreset,
    active: Boolean,
    busy: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val look = remember(preset) { preset.toLook() }
    val spec = rememberLookSpec(look)
    val thumbs = rememberPresetThumbs(preset)
    var renaming by remember(preset.id) { mutableStateOf(false) }
    var newName by remember(preset.id) { mutableStateOf(preset.name) }
    var confirmDelete by remember(preset.id) { mutableStateOf(false) }

    PreviewHeader(
        spec = spec,
        look = look,
        thumbs = thumbs,
        label = stringResource(R.string.cryo_presets_preview),
        title = preset.name,
        status = stringResource(if (active) R.string.cryo_presets_in_use else R.string.cryo_presets_not_applied),
        statusColor = if (active) spec.aurora else spec.ice
    )

    //Linha 1: as duas ações principais (usar e compartilhar). Linha 2: editar e excluir, longe do exportar.
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
            MarqueeText(text = stringResource(R.string.cryo_presets_apply))
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
            MarqueeText(text = stringResource(R.string.cryo_presets_export))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CryoTonalButton(
            modifier = Modifier.weight(1f),
            enabled = !busy,
            onClick = { renaming = !renaming; confirmDelete = false; newName = preset.name }
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
        Section(stringResource(R.string.cryo_presets_rename)) {
            SimpleTextInputField(
                modifier = Modifier.fillMaxWidth(),
                value = newName,
                onValueChange = { newName = it.take(CryoPresets.NAME_MAX) },
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
                text = stringResource(R.string.cryo_presets_delete_confirm, preset.name),
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

    SlotsSection(
        thumbs = thumbs,
        bgVideo = preset.background?.video == true,
        heroVideo = preset.hero?.video == true,
        portraitVideo = preset.portrait?.video == true,
        fmVideo = preset.fm?.video == true,
        //Predefinição antiga: não guarda o fundo do gerenciador, então não mostra esse espaço
        showFm = preset.hasFm
    )
}

// =====================================================================
// Peças visuais
// =====================================================================

@Composable
private fun rememberLookSpec(look: Look): CryoSpec {
    val context = LocalContext.current
    val dark = isLauncherInDarkTheme()
    return remember(look.type, look.customColor, look.palette, look.saturation, look.vibrance, look.contrast, dark) {
        val adjust = CryoColorAdjust(look.saturation / 100f, look.vibrance / 100f, look.contrast / 200f)
        val scheme = adjust.apply(launcherColorScheme(context, look.type, dark, Color(look.customColor), look.palette))
        adjust.apply(cryoSpecFor(look.type, scheme, Color(look.customColor)))
    }
}

private fun gradient(spec: CryoSpec) = Brush.horizontalGradient(listOf(spec.iceSoft, spec.ice, spec.aurora))

@Composable
private fun ListTile(
    spec: CryoSpec,
    selected: Boolean,
    thumb: ImageBitmap?,
    title: String,
    subtitle: String,
    badgeIcon: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(spec.iceSoft.copy(alpha = 0.55f), spec.aurora.copy(alpha = 0.15f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, (if (selected) spec.ice else CryoGlassColors.Ice).copy(alpha = if (selected) 0.9f else 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gradient(spec))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumb != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            if (badgeIcon != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(spec.deep.copy(alpha = 0.75f))
                        .border(1.dp, spec.ice, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(13.dp),
                        painter = painterResource(badgeIcon),
                        contentDescription = null,
                        tint = spec.ice
                    )
                }
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

@Composable
private fun PreviewHeader(
    spec: CryoSpec,
    look: Look,
    thumbs: LookThumbs,
    label: String,
    title: String,
    status: String,
    statusColor: Color
) {
    //A prévia é gravada em uma camada gráfica: o botão abaixo salva essa camada como imagem
    val previewLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingImage by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                previewLayer.record { this@drawWithContent.drawContent() }
                drawLayer(previewLayer)
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LookHomePreview(
            modifier = Modifier.weight(1.35f),
            spec = spec,
            thumbs = thumbs
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
                color = spec.ice
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = themeTypeName(look.type),
                style = MaterialTheme.typography.labelMedium,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(spec.iceSoft, spec.ice, spec.aurora, spec.navy).forEach {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(it)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(gradient(spec))
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
    CryoTonalButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = !savingImage,
        onClick = {
            savingImage = true
            scope.launch {
                val saved = runCatching {
                    val image = previewLayer.toImageBitmap()
                    withContext(Dispatchers.IO) { savePreviewImage(context, image, title) }
                }.getOrNull()
                savingImage = false
                android.widget.Toast.makeText(
                    context,
                    if (saved != null) context.getString(R.string.cryo_presets_image_saved, saved)
                    else context.getString(R.string.cryo_presets_image_failed),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_download),
            contentDescription = null
        )
        MarqueeText(text = stringResource(R.string.cryo_presets_save_image))
    }
}

/** Pasta (dentro de Imagens) onde as prévias são salvas. */
private const val PREVIEW_IMAGE_DIR = "Cryogenic Launcher"

/**
 * Salva a imagem da prévia como PNG na galeria (Imagens/Cryogenic Launcher).
 * Retorna o caminho mostrado ao usuário, ou lança exceção se não conseguir salvar.
 */
private fun savePreviewImage(context: android.content.Context, image: ImageBitmap, title: String): String {
    var bitmap = image.asAndroidBitmap()
    //Bitmaps de hardware não podem ser comprimidos: copia para memória comum antes
    if (android.os.Build.VERSION.SDK_INT >= 26 && bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
        bitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            ?: error("Não foi possível copiar a imagem")
    }
    val safeTitle = title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().replace(' ', '_').take(40).ifEmpty { "Preview" }
    val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val fileName = "Cryogenic_${safeTitle}_$stamp.png"
    val shownPath = "${android.os.Environment.DIRECTORY_PICTURES}/$PREVIEW_IMAGE_DIR/$fileName"

    if (android.os.Build.VERSION.SDK_INT >= 29) {
        //Android 10+: entra na galeria pelo MediaStore, sem pedir permissão
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                "${android.os.Environment.DIRECTORY_PICTURES}/$PREVIEW_IMAGE_DIR"
            )
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore recusou a imagem")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out))
            } ?: error("Sem acesso para gravar a imagem")
            values.clear()
            values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    } else {
        //Android 9 ou anterior: grava direto em Imagens (usa a permissão de armazenamento do Launcher)
        @Suppress("DEPRECATION")
        val dir = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
            PREVIEW_IMAGE_DIR
        )
        dir.mkdirs()
        val file = File(dir, fileName)
        file.outputStream().use { out ->
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out))
        }
        android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
    }
    return shownPath
}

/**
 * Mini tela inicial com os 3 wallpapers: fundo inteiro, cartão principal à esquerda
 * e wallpaper 9:16 à direita, tudo nas cores do tema da predefinição.
 */
@Composable
private fun LookHomePreview(
    modifier: Modifier = Modifier,
    spec: CryoSpec,
    thumbs: LookThumbs
) {
    val corner = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(corner)
            .background(spec.deep)
            .border(1.dp, spec.ice.copy(alpha = 0.6f), corner)
    ) {
        if (thumbs.background != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = thumbs.background,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(spec.deep.copy(alpha = 0.10f), spec.deep.copy(alpha = 0.55f))))
        )
        // barra lateral
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .background(spec.navy.copy(alpha = 0.85f))
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index == 0) gradient(spec) else Brush.linearGradient(listOf(spec.ice.copy(alpha = 0.18f), spec.ice.copy(alpha = 0.18f))))
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 26.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // cartão principal (wallpaper do cartão)
                MiniCard(spec, Modifier.fillMaxWidth().weight(1.25f), thumbs.hero) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp)
                            .size(width = 34.dp, height = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(gradient(spec))
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MiniCard(spec, Modifier.weight(1f).fillMaxHeight(), null) {}
                    MiniCard(spec, Modifier.weight(1f).fillMaxHeight(), null) {}
                }
            }
            // wallpaper 9:16
            MiniCard(spec, Modifier.fillMaxHeight().aspectRatio(9f / 16f), thumbs.portrait) {}
        }
    }
}

@Composable
private fun MiniCard(
    spec: CryoSpec,
    modifier: Modifier,
    image: ImageBitmap?,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(spec.navy.copy(alpha = 0.75f), spec.deep.copy(alpha = 0.85f))))
            .border(0.5.dp, spec.ice.copy(alpha = 0.55f), shape)
    ) {
        if (image != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}

@Composable
private fun SlotsSection(
    thumbs: LookThumbs, bgVideo: Boolean, heroVideo: Boolean, portraitVideo: Boolean,
    fmVideo: Boolean = false, showFm: Boolean = true
) {
    Section(stringResource(R.string.cryo_presets_wallpapers)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SlotItem(Modifier.weight(1.4f), stringResource(R.string.cryo_presets_slot_background), thumbs.background, bgVideo, 16f / 9f)
            SlotItem(Modifier.weight(1.4f), stringResource(R.string.cryo_presets_slot_hero), thumbs.hero, heroVideo, 16f / 9f)
            SlotItem(Modifier.weight(0.6f), stringResource(R.string.cryo_presets_slot_portrait), thumbs.portrait, portraitVideo, 9f / 16f)
            if (showFm) {
                SlotItem(Modifier.weight(1.4f), stringResource(R.string.cryo_presets_slot_fm), thumbs.fm, fmVideo, 16f / 9f)
            }
        }
    }
}

@Composable
private fun SlotItem(modifier: Modifier, label: String, image: ImageBitmap?, video: Boolean, ratio: Float) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(0.5f),
                    painter = painterResource(R.drawable.ic_image_outlined),
                    contentDescription = null,
                    tint = CryoGlassColors.Frost
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(
                when {
                    image == null -> R.string.cryo_presets_slot_none
                    video -> R.string.cryo_presets_slot_video
                    else -> R.string.cryo_presets_slot_image
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
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
