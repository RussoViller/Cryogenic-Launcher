package com.russoviller.cryogenic.streamer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.colorpicker.rememberColorPickerController
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.ColorPickerDialog
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.MenuSliderLayout
import com.russoviller.cryogenic.ui.components.MenuSwitchButton
import com.russoviller.cryogenic.ui.components.MenuTextButton
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

/** Cores rápidas para texto e fundo (a última bolinha abre o seletor completo). */
private val quickColors = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFF7FE7FF, 0xFF3D8BFF, 0xFFB57BFF, 0xFFFF5CA8,
    0xFFFF4D4D, 0xFFFF9F1C, 0xFFFFE14D, 0xFF4DFF88, 0xFF0A1A2E
).map { Color(it) }

// =====================================================================
// PNGs na tela
// =====================================================================

fun LazyListScope.streamImagesPage(
    selected: Int,
    picker: StreamImagePicker,
    onSelect: (Int) -> Unit,
    onOpenEditor: (cropIndex: Int?) -> Unit,
    color: Color,
    contentColor: Color
) {
    item(key = "img_adjust") {
        MenuTextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_adjust),
            enabled = StreamExtrasStore.images.any { it.enabled && it.hasImage },
            color = color,
            contentColor = contentColor,
            onClick = { onOpenEditor(null) }
        )
    }
    item(key = "img_grid") {
        ImageGrid(selected = selected, onSelect = onSelect, picker = picker)
    }
    item(key = "img_settings_$selected") {
        val img = StreamExtrasStore.images.getOrNull(selected)
        if (img != null && img.hasImage) {
            ImageSettings(index = selected, img = img, picker = picker, onOpenEditor = onOpenEditor, color = color, contentColor = contentColor)
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = stringResource(R.string.cryo_stream_image_pick_hint),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

/** Seletor da galeria (fica fora da lista para não ser desmontado ao rolar). */
class StreamImagePicker internal constructor() {
    var loading by mutableIntStateOf(-1)
        internal set
    internal var launch: (Int) -> Unit = {}
    fun pick(slot: Int) = launch(slot)
}

@Composable
fun rememberStreamImagePicker(onImported: (Int) -> Unit): StreamImagePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val holder = remember { StreamImagePicker() }
    var pending by remember { mutableIntStateOf(-1) }
    val failed = stringResource(R.string.cryo_stream_image_failed)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val slot = pending
        pending = -1
        if (uri != null && slot >= 0) {
            holder.loading = slot
            scope.launch {
                val ok = StreamExtrasStore.importImage(context, slot, uri)
                holder.loading = -1
                if (ok) onImported(slot) else Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    holder.launch = { slot ->
        pending = slot
        runCatching {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure {
            pending = -1
            Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
        }
    }
    return holder
}

@Composable
private fun ImageGrid(selected: Int, onSelect: (Int) -> Unit, picker: StreamImagePicker) {
    val perRow = 4
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowStart in 0 until StreamExtrasStore.IMAGE_COUNT step perRow) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in rowStart until min(rowStart + perRow, StreamExtrasStore.IMAGE_COUNT)) {
                    val img = StreamExtrasStore.images.getOrNull(i) ?: StreamImage()
                    ImageSquare(
                        modifier = Modifier.weight(1f),
                        index = i,
                        img = img,
                        selected = i == selected,
                        loading = i == picker.loading,
                        onClick = { if (img.hasImage) onSelect(i) else picker.pick(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageSquare(
    modifier: Modifier,
    index: Int,
    img: StreamImage,
    selected: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(CryoGlassColors.Deep.copy(alpha = 0.55f))
            .then(
                if (img.hasImage) Modifier.border(if (selected) 2.5.dp else 1.dp, ice.copy(alpha = if (selected) 1f else 0.4f), shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (img.hasImage) {
            val bmp by produceState(initialValue = StreamImageCache.get(img.file), img.file) {
                if (value == null) value = StreamImageCache.load(img.file)
            }
            bmp?.let { b ->
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .alpha(if (img.enabled) 1f else 0.35f)
                ) {
                    // mostra o recorte, inteiro dentro do quadrado
                    val l = (img.cropL * b.width).roundToInt().coerceIn(0, b.width - 1)
                    val t = (img.cropT * b.height).roundToInt().coerceIn(0, b.height - 1)
                    val r = (img.cropR * b.width).roundToInt().coerceIn(l + 1, b.width)
                    val bt = (img.cropB * b.height).roundToInt().coerceIn(t + 1, b.height)
                    val s = min(size.width / (r - l), size.height / (bt - t))
                    val dw = ((r - l) * s).roundToInt().coerceAtLeast(1)
                    val dh = ((bt - t) * s).roundToInt().coerceAtLeast(1)
                    drawImage(
                        image = b,
                        srcOffset = IntOffset(l, t),
                        srcSize = IntSize(r - l, bt - t),
                        dstOffset = IntOffset(((size.width - dw) / 2f).roundToInt(), ((size.height - dh) / 2f).roundToInt()),
                        dstSize = IntSize(dw, dh),
                        filterQuality = FilterQuality.Medium
                    )
                }
            }
            Text(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CryoGlassColors.Deep.copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                text = "${index + 1}",
                color = ice,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall
            )
        } else {
            // quadrado vazio: borda tracejada com +
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = ice.copy(alpha = 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ice, strokeWidth = 2.dp)
            } else {
                Text(text = "+", color = ice, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
private fun ImageSettings(
    index: Int,
    img: StreamImage,
    picker: StreamImagePicker,
    onOpenEditor: (cropIndex: Int?) -> Unit,
    color: Color,
    contentColor: Color
) {
    SettingsCard(highlight = img.enabled) {
        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_image_n, index + 1),
            switch = img.enabled,
            onSwitch = { on -> StreamExtrasStore.updateImage(index) { it.copy(enabled = on) } },
            color = color,
            contentColor = contentColor
        )
        MenuSliderLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.cryo_stream_size),
            value = (img.w * 100f).roundToInt(),
            onValueChange = { v -> StreamExtrasStore.updateImage(index, save = false) { it.copy(w = v / 100f) } },
            onValueChangeFinished = { StreamExtrasStore.saveImages() },
            valueRange = 3f..100f,
            suffix = "%",
            color = color,
            contentColor = contentColor
        )
        MenuSliderLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.cryo_stream_opacity),
            value = img.opacity,
            onValueChange = { v -> StreamExtrasStore.updateImage(index, save = false) { it.copy(opacity = v) } },
            onValueChangeFinished = { StreamExtrasStore.saveImages() },
            valueRange = 0f..100f,
            suffix = "%",
            color = color,
            contentColor = contentColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(Modifier.weight(1f), "✂ " + stringResource(R.string.cryo_stream_crop), primary = true) {
                onOpenEditor(index)
            }
            PillButton(Modifier.weight(1f), stringResource(R.string.cryo_stream_image_change)) {
                picker.pick(index)
            }
            PillButton(Modifier.weight(1f), stringResource(R.string.cryo_stream_remove), danger = true) {
                StreamExtrasStore.removeImage(index)
            }
        }
    }
}

// =====================================================================
// Textos na tela
// =====================================================================

fun LazyListScope.streamTextsPage(
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpenEditor: () -> Unit,
    color: Color,
    contentColor: Color
) {
    item(key = "txt_adjust") {
        MenuTextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_adjust),
            enabled = StreamExtrasStore.texts.any { it.enabled && it.text.isNotBlank() },
            color = color,
            contentColor = contentColor,
            onClick = onOpenEditor
        )
    }
    item(key = "txt_tabs") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until StreamExtrasStore.TEXT_COUNT) {
                val t = StreamExtrasStore.texts.getOrNull(i) ?: StreamText()
                val active = i == selected
                val ice = CryoGlassColors.Ice
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) ice else CryoGlassColors.Deep.copy(alpha = 0.55f))
                        .border(1.dp, ice.copy(alpha = if (t.enabled && t.text.isNotBlank()) 0.9f else 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T${i + 1}",
                        color = if (active) CryoGlassColors.Deep else ice,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
    item(key = "txt_settings_$selected") {
        TextSettings(index = selected, color = color, contentColor = contentColor)
    }
}

@Composable
private fun TextSettings(index: Int, color: Color, contentColor: Color) {
    val item = StreamExtrasStore.texts.getOrNull(index) ?: return
    val ice = CryoGlassColors.Ice
    SettingsCard(highlight = item.enabled) {
        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_text_n, index + 1),
            switch = item.enabled,
            onSwitch = { on -> StreamExtrasStore.updateText(index) { it.copy(enabled = on) } },
            color = color,
            contentColor = contentColor
        )
        // texto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, ice.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (item.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.cryo_stream_text_hint),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = item.text,
                onValueChange = { v -> StreamExtrasStore.updateText(index) { it.copy(text = v.take(300)) } },
                maxLines = 4,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(ice)
            )
        }
        // prévia
        if (item.text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B3A4A))
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                StreamTextView(item = item.copy(size = item.size.coerceAtMost(40), opacity = item.opacity))
            }
        }
        MenuSliderLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.cryo_stream_size),
            value = item.size,
            onValueChange = { v -> StreamExtrasStore.updateText(index, save = false) { it.copy(size = v) } },
            onValueChangeFinished = { StreamExtrasStore.saveTexts() },
            valueRange = StreamExtrasStore.TEXT_MIN.toFloat()..StreamExtrasStore.TEXT_MAX.toFloat(),
            suffix = "sp",
            color = color,
            contentColor = contentColor
        )
        ColorRow(
            title = stringResource(R.string.cryo_stream_text_color),
            current = Color(item.color),
            onPick = { c -> StreamExtrasStore.updateText(index) { it.copy(color = c.toArgb()) } }
        )
        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_bold),
            switch = item.bold,
            onSwitch = { on -> StreamExtrasStore.updateText(index) { it.copy(bold = on) } },
            color = color,
            contentColor = contentColor
        )
        MenuSliderLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.cryo_stream_glow),
            value = item.glow,
            onValueChange = { v -> StreamExtrasStore.updateText(index, save = false) { it.copy(glow = v) } },
            onValueChangeFinished = { StreamExtrasStore.saveTexts() },
            valueRange = 0f..100f,
            suffix = "%",
            color = color,
            contentColor = contentColor
        )
        MenuSliderLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.cryo_stream_opacity),
            value = item.opacity,
            onValueChange = { v -> StreamExtrasStore.updateText(index, save = false) { it.copy(opacity = v) } },
            onValueChangeFinished = { StreamExtrasStore.saveTexts() },
            valueRange = 0f..100f,
            suffix = "%",
            color = color,
            contentColor = contentColor
        )
        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_bg),
            switch = item.bg,
            onSwitch = { on -> StreamExtrasStore.updateText(index) { it.copy(bg = on) } },
            color = color,
            contentColor = contentColor
        )
        if (item.bg) {
            ColorRow(
                title = stringResource(R.string.cryo_stream_bg_color),
                current = Color(item.bgColor),
                onPick = { c -> StreamExtrasStore.updateText(index) { it.copy(bgColor = c.copy(alpha = 1f).toArgb()) } }
            )
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.cryo_stream_bg_opacity),
                value = item.bgOpacity,
                onValueChange = { v -> StreamExtrasStore.updateText(index, save = false) { it.copy(bgOpacity = v) } },
                onValueChangeFinished = { StreamExtrasStore.saveTexts() },
                valueRange = 0f..100f,
                suffix = "%",
                color = color,
                contentColor = contentColor
            )
            MenuSliderLayout(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.cryo_stream_bg_round),
                value = item.bgRound,
                onValueChange = { v -> StreamExtrasStore.updateText(index, save = false) { it.copy(bgRound = v) } },
                onValueChangeFinished = { StreamExtrasStore.saveTexts() },
                valueRange = 0f..40f,
                suffix = "dp",
                color = color,
                contentColor = contentColor
            )
        }
    }
}

// =====================================================================
// Peças comuns
// =====================================================================

@Composable
private fun SettingsCard(highlight: Boolean, content: @Composable () -> Unit) {
    val ice = CryoGlassColors.Ice
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CryoGlassColors.Deep.copy(alpha = 0.55f))
            .border(1.dp, ice.copy(alpha = if (highlight) 0.7f else 0.25f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun PillButton(
    modifier: Modifier,
    text: String,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    val red = Color(0xFFFF5C7A)
    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(if (primary) ice else Color.Transparent)
            .border(1.dp, if (danger) red else ice.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        text = text,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = when {
            primary -> CryoGlassColors.Deep
            danger -> red
            else -> ice
        },
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1
    )
}

@Composable
private fun ColorRow(title: String, current: Color, onPick: (Color) -> Unit) {
    val ice = CryoGlassColors.Ice
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp),
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickColors.forEach { c ->
                val active = c.toArgb() == current.copy(alpha = 1f).toArgb()
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (active) 3.dp else 1.dp, if (active) ice else Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { onPick(c) }
                )
            }
            // seletor completo
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.sweepGradient(
                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
    if (showPicker) {
        val controller = rememberColorPickerController(initialColor = current.copy(alpha = 1f))
        ColorPickerDialog(
            colorController = controller,
            onCancel = { showPicker = false },
            onConfirm = { c ->
                showPicker = false
                onPick(c.copy(alpha = 1f))
            },
            showAlpha = false
        )
    }
}
