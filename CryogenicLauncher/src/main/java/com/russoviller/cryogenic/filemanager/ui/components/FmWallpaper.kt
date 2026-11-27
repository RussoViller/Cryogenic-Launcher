package com.russoviller.cryogenic.filemanager.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.context.copyLocalFile
import com.russoviller.cryogenic.contract.MediaPickerContract
import com.russoviller.cryogenic.path.PathManager
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.SimpleTextSlider
import com.russoviller.cryogenic.ui.components.VideoPlayer
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.screens.content.elements.BackgroundImage
import com.russoviller.cryogenic.utils.image.extractMotionPhotoVideo
import com.russoviller.cryogenic.utils.image.isImageFile
import com.russoviller.cryogenic.utils.video.isVideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

val FmWallpaperStore = CryoMediaWallpaper("fm_wallpaper.file")
val HeroWallpaperStore = CryoMediaWallpaper("hero_wallpaper.file")
val PortraitWallpaperStore = CryoMediaWallpaper("portrait_wallpaper.file")

class CryoMediaWallpaper(private val fileName: String) {
    val file: File get() = File(PathManager.DIR_BACKGROUND, fileName)

    var isImage by mutableStateOf(false)
        private set
    var isVideo by mutableStateOf(false)
        private set
    var generation by mutableStateOf(0)
        private set
    private var loaded = false

    val isValid: Boolean get() = isImage || isVideo

    suspend fun ensureLoaded() {
        if (loaded) return
        loaded = true
        refresh()
    }

    //Tamanho e data do arquivo na última leitura, para notar quando outro processo trocou o wallpaper
    @Volatile
    private var stamp: String? = null

    private fun stampOf(f: File) = if (f.isFile) "${f.length()}:${f.lastModified()}" else "-"

    /** Relê o arquivo só se ele mudou no disco desde a última leitura. */
    suspend fun refreshIfChanged() {
        loaded = true
        val changed = withContext(Dispatchers.IO) { stampOf(file) != stamp }
        if (changed) refresh()
    }

    suspend fun refresh() {
        val (image, video) = withContext(Dispatchers.IO) {
            val f = file
            stamp = stampOf(f)
            val image = f.exists() && f.isImageFile()
            val video = !image && f.exists() && f.isVideoFile()
            image to video
        }
        withContext(Dispatchers.Main) {
            isImage = image
            isVideo = video
            generation++
        }
    }

    suspend fun import(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            val target = file
            target.parentFile?.mkdirs()
            FileUtils.deleteQuietly(target)
            context.copyLocalFile(uri, target)
            runCatching { target.extractMotionPhotoVideo() }
            if (!target.isImageFile() && !target.isVideoFile()) {
                FileUtils.deleteQuietly(target)
                error("The selected file is not an image or a video!")
            }
        }
        refresh()
    }

    suspend fun delete() {
        withContext(Dispatchers.IO) { FileUtils.deleteQuietly(file) }
        refresh()
    }
}

@Composable
fun FmWallpaperLayer(modifier: Modifier = Modifier) {
    //O Launcher (outro processo) pode trocar este wallpaper ao aplicar uma predefinição:
    //relê o arquivo e os ajustes toda vez que o gerenciador volta para a tela
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            FmWallpaperStore.refreshIfChanged()
            AllSettings.cryoFmBackgroundOpacity.getValue()
            AllSettings.cryoFmWallpaperVolume.getValue()
        }
    }
    val store = FmWallpaperStore
    if (!store.isValid) return
    val generation = store.generation
    val dim = AllSettings.cryoFmBackgroundOpacity.state / 100f
    Box(modifier = modifier) {
        if (store.isVideo) {
            VideoPlayer(
                videoUri = Uri.fromFile(store.file),
                modifier = Modifier.fillMaxSize(),
                refreshTrigger = generation,
                muted = false,
                volume = AllSettings.cryoFmWallpaperVolume.state / 100f
            )
        } else {
            BackgroundImage(
                modifier = Modifier.fillMaxSize(),
                imageFile = store.file,
                refreshTrigger = generation
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CryoGlassColors.Deep.copy(alpha = dim))
        )
    }
}

@Composable
fun FmWallpaperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by androidx.compose.runtime.remember { mutableStateOf(false) }
    val errorText = stringResource(R.string.error_import_image)
    val picker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(allowImages = true, allowVideos = true, allowMultiple = false)
    ) { result ->
        val uri = result?.firstOrNull() ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching { FmWallpaperStore.import(context, uri) }
                .onFailure { Toast.makeText(context, errorText, Toast.LENGTH_LONG).show() }
            busy = false
        }
    }
    LaunchedEffect(Unit) { FmWallpaperStore.ensureLoaded() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .cryoGlass(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp, opaque = true)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.cryo_fm_wallpaper_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                )
            )
            Text(
                text = stringResource(R.string.cryo_fm_wallpaper_summary),
                style = MaterialTheme.typography.bodySmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f)
            )
            Text(
                text = when {
                    busy -> stringResource(R.string.settings_launcher_background_importing)
                    FmWallpaperStore.isVideo -> stringResource(R.string.cryo_fm_wallpaper_state_video)
                    FmWallpaperStore.isImage -> stringResource(R.string.cryo_fm_wallpaper_state_image)
                    else -> stringResource(R.string.cryo_fm_wallpaper_state_none)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Ice
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CryoButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = { picker.launch(Unit) }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_image_filled),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.cryo_fm_wallpaper_pick), maxLines = 1)
                }
                CryoTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy && FmWallpaperStore.isValid,
                    onClick = { scope.launch { FmWallpaperStore.delete() } }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_delete_outlined),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.cryo_fm_wallpaper_remove), maxLines = 1)
                }
            }
            FmSliderRow(
                title = stringResource(R.string.cryo_fm_wallpaper_volume),
                value = AllSettings.cryoFmWallpaperVolume.state,
                enabled = FmWallpaperStore.isVideo,
                onChange = { AllSettings.cryoFmWallpaperVolume.updateState(it) },
                onFinished = { AllSettings.cryoFmWallpaperVolume.save(AllSettings.cryoFmWallpaperVolume.state) }
            )
            FmSliderRow(
                title = stringResource(R.string.cryo_fm_background_opacity),
                value = AllSettings.cryoFmBackgroundOpacity.state,
                enabled = FmWallpaperStore.isValid,
                onChange = { AllSettings.cryoFmBackgroundOpacity.updateState(it) },
                onFinished = { AllSettings.cryoFmBackgroundOpacity.save(AllSettings.cryoFmBackgroundOpacity.state) }
            )
            CryoTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.generic_close))
            }
        }
    }
}

@Composable
private fun FmSliderRow(
    title: String,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    onFinished: () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CryoGlassColors.Frost
        )
        SimpleTextSlider(
            modifier = Modifier.fillMaxWidth(),
            value = value.toFloat(),
            enabled = enabled,
            onValueChange = { onChange(it.toInt()) },
            onValueChangeFinished = onFinished,
            toInt = true,
            suffix = "%",
            valueRange = 0f..100f
        )
    }
}
