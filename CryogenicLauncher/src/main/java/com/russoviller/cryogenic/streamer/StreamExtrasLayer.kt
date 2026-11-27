package com.russoviller.cryogenic.streamer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import kotlin.math.abs
import kotlin.math.min
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
 * Modo streamer: PNGs e textos na tela. Ficam por cima do jogo e por baixo dos botões do HUD,
 * e não recebem toques (tudo continua indo para o jogo e os controles).
 */

@Composable
private fun rememberStreamBitmap(path: String): ImageBitmap? {
    val bmp by produceState(initialValue = StreamImageCache.get(path), path) {
        if (value == null && path.isNotEmpty()) value = StreamImageCache.load(path)
    }
    return bmp
}

/** Desenha só a parte recortada da imagem, ocupando o tamanho todo (sem esticar: a moldura tem a mesma proporção). */
@Composable
private fun CroppedImage(modifier: Modifier, image: StreamImage, alpha: Float = 1f) {
    if (image.file.endsWith(".gif", ignoreCase = true) && android.os.Build.VERSION.SDK_INT >= 28) {
        CroppedGif(modifier, image, alpha)
        return
    }
    val bmp = rememberStreamBitmap(image.file) ?: return
    Canvas(modifier = modifier) {
        val l = (image.cropL * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
        val t = (image.cropT * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
        val r = (image.cropR * bmp.width).roundToInt().coerceIn(l + 1, bmp.width)
        val b = (image.cropB * bmp.height).roundToInt().coerceIn(t + 1, bmp.height)
        drawImage(
            image = bmp,
            srcOffset = IntOffset(l, t),
            srcSize = IntSize(r - l, b - t),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
            alpha = alpha,
            filterQuality = FilterQuality.Medium
        )
    }
}

/**
 * GIF animado (Android 9+). O próprio Android avança os quadros; a tela só é redesenhada
 * quando o GIF troca de quadro. Em Android 8 o GIF aparece parado (primeiro quadro).
 */
@androidx.annotation.RequiresApi(28)
@Composable
private fun CroppedGif(modifier: Modifier, image: StreamImage, alpha: Float) {
    val path = image.file
    val drawable by produceState<android.graphics.drawable.AnimatedImageDrawable?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = android.graphics.ImageDecoder.createSource(java.io.File(path))
                android.graphics.ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    //GIF muito grande: reduz na decodificação (economiza memória)
                    val side = maxOf(info.size.width, info.size.height)
                    var sample = 1
                    while (side / (sample * 2) >= 1024) sample *= 2
                    if (sample > 1) decoder.setTargetSampleSize(sample)
                } as? android.graphics.drawable.AnimatedImageDrawable
            }.getOrNull()
        }
    }
    val gif = drawable
    if (gif == null) {
        //não é animado (ou falhou): mostra a imagem parada
        val bmp = rememberStreamBitmap(path) ?: return
        Canvas(modifier = modifier) {
            val l = (image.cropL * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
            val t = (image.cropT * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
            val r = (image.cropR * bmp.width).roundToInt().coerceIn(l + 1, bmp.width)
            val b = (image.cropB * bmp.height).roundToInt().coerceIn(t + 1, bmp.height)
            drawImage(
                image = bmp,
                srcOffset = IntOffset(l, t),
                srcSize = IntSize(r - l, b - t),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                alpha = alpha,
                filterQuality = FilterQuality.Medium
            )
        }
        return
    }
    val frame = remember(gif) { androidx.compose.runtime.mutableIntStateOf(0) }
    DisposableEffect(gif) {
        val callback = object : android.graphics.drawable.Drawable.Callback {
            override fun invalidateDrawable(who: android.graphics.drawable.Drawable) {
                frame.intValue++
            }
            override fun scheduleDrawable(who: android.graphics.drawable.Drawable, what: Runnable, `when`: Long) {}
            override fun unscheduleDrawable(who: android.graphics.drawable.Drawable, what: Runnable) {}
        }
        gif.callback = callback
        gif.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
        gif.start()
        onDispose {
            gif.stop()
            gif.callback = null
        }
    }
    Canvas(modifier = modifier) {
        //lê o quadro atual: redesenha só quando o GIF avança
        frame.intValue
        val iw = gif.intrinsicWidth.coerceAtLeast(1)
        val ih = gif.intrinsicHeight.coerceAtLeast(1)
        val cropW = ((image.cropR - image.cropL) * iw).coerceAtLeast(1f)
        val cropH = ((image.cropB - image.cropT) * ih).coerceAtLeast(1f)
        gif.setBounds(0, 0, iw, ih)
        gif.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val save = native.save()
            native.clipRect(0f, 0f, size.width, size.height)
            native.scale(size.width / cropW, size.height / cropH)
            native.translate(-image.cropL * iw, -image.cropT * ih)
            gif.draw(native)
            native.restoreToCount(save)
        }
    }
}

/** Um texto com fundo, cor, tamanho e brilho. */
@Composable
fun StreamTextView(modifier: Modifier = Modifier, item: StreamText, placeholder: String? = null) {
    val textColor = Color(item.color)
    val content = item.text.ifEmpty { placeholder ?: return }
    val base = TextStyle(
        color = textColor,
        fontSize = item.size.sp,
        fontWeight = if (item.bold) FontWeight.Black else FontWeight.Normal,
        lineHeight = (item.size * 1.15f).sp
    )
    val density = LocalDensity.current
    val sizePx = with(density) { item.size.sp.toPx() }
    Box(
        modifier = modifier
            .alpha(item.opacity / 100f)
            .then(
                if (item.bg) Modifier.background(
                    Color(item.bgColor).copy(alpha = item.bgOpacity / 100f),
                    RoundedCornerShape(item.bgRound.dp)
                ) else Modifier
            )
            .padding(horizontal = (item.size * 0.45f).dp, vertical = (item.size * 0.2f).dp)
    ) {
        if (item.glow > 0) {
            // brilho: duas camadas desfocadas da cor do texto
            val g = item.glow / 100f
            Text(text = content, style = base.copy(shadow = Shadow(textColor, Offset.Zero, sizePx * (0.25f + g * 0.9f))))
            Text(text = content, style = base.copy(shadow = Shadow(textColor.copy(alpha = 0.6f + g * 0.4f), Offset.Zero, sizePx * 0.18f * g + 1f)))
        } else {
            Text(text = content, style = base)
        }
    }
}

/** Os PNGs e textos ligados, cada um no seu lugar. Fica entre o jogo e os controles. */
@Composable
fun StreamExtrasLayer(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { StreamExtrasStore.ensureLoaded() }
    val images = StreamExtrasStore.images
    val texts = StreamExtrasStore.texts
    if (images.none { it.enabled && it.hasImage } && texts.none { it.enabled && it.text.isNotBlank() }) return
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        images.forEachIndexed { index, raw ->
            if (raw.enabled && raw.hasImage) {
                val img = StreamExtrasStore.fitToScreen(raw, sw, sh)
                key("img$index") {
                    CroppedImage(
                        modifier = Modifier
                            .offset { IntOffset((img.x * sw).roundToInt(), (img.y * sh).roundToInt()) }
                            .size(with(density) { (img.w * sw).toDp() }, with(density) { (img.heightOn(sw, sh) * sh).toDp() }),
                        image = img,
                        alpha = img.opacity / 100f
                    )
                }
            }
        }
        texts.forEachIndexed { index, item ->
            if (item.enabled && item.text.isNotBlank()) {
                key("txt$index") {
                    StreamTextView(
                        modifier = Modifier.offset { IntOffset((item.x * sw).roundToInt(), (item.y * sh).roundToInt()) },
                        item = item
                    )
                }
            }
        }
    }
}

// =====================================================================
// Ajuste de posição, tamanho e recorte
// =====================================================================

@Composable
fun StreamExtrasEditor(modifier: Modifier = Modifier) {
    val mode = StreamExtrasStore.editing ?: return
    StreamExtrasStore.ensureLoaded()
    when (mode) {
        StreamEditMode.IMAGES -> {
            val cropping = StreamExtrasStore.croppingImage
            if (cropping != null) ImageCropEditor(modifier, cropping) else ImagesEditor(modifier)
        }
        StreamEditMode.TEXTS -> TextsEditor(modifier)
    }
}

@Composable
private fun EditorTopBar(
    modifier: Modifier,
    hint: String,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    onDone: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    Row(
        modifier = modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(CryoGlassColors.Deep.copy(alpha = 0.94f))
            .border(1.dp, ice.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = hint, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        if (secondary != null) {
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, ice.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onSecondary)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                text = secondary,
                color = ice,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .background(ice)
                .clickable(onClick = onDone)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            text = stringResource(R.string.cryo_stream_done),
            color = CryoGlassColors.Deep,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun SizeHandle(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(topStart = 10.dp))
            .background(CryoGlassColors.Ice),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "⤡", color = CryoGlassColors.Deep, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun NumberBadge(n: Int) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CryoGlassColors.Deep.copy(alpha = 0.85f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        text = "$n",
        color = CryoGlassColors.Ice,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.labelMedium
    )
}

/** PNGs: arrastar move; o canto muda o tamanho (sem esticar); ✂ abre o recorte. */
@Composable
private fun ImagesEditor(modifier: Modifier) {
    val ice = CryoGlassColors.Ice
    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(indication = null, interactionSource = null) {}
    ) {
        val density = LocalDensity.current
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        StreamExtrasStore.images.forEachIndexed { index, raw ->
            if (!raw.enabled || !raw.hasImage) return@forEachIndexed
            val img = StreamExtrasStore.fitToScreen(raw, sw, sh)
            Box(
                modifier = Modifier
                    .offset { IntOffset((img.x * sw).roundToInt(), (img.y * sh).roundToInt()) }
                    .size(with(density) { (img.w * sw).toDp() }, with(density) { (img.heightOn(sw, sh) * sh).toDp() })
                    .border(2.dp, ice, RoundedCornerShape(4.dp))
                    .pointerInput(index, sw, sh) {
                        detectDragGestures(onDragEnd = { StreamExtrasStore.saveImages() }) { change, drag ->
                            change.consume()
                            StreamExtrasStore.updateImage(index, save = false) {
                                StreamExtrasStore.fitToScreen(it.copy(x = it.x + drag.x / sw, y = it.y + drag.y / sh), sw, sh)
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberBadge(index + 1)
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ice)
                            .clickable { StreamExtrasStore.croppingImage = index }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = "✂",
                        color = CryoGlassColors.Deep,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                SizeHandle(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .pointerInput(index, sw, sh) {
                            detectDragGestures(onDragEnd = { StreamExtrasStore.saveImages() }) { change, drag ->
                                change.consume()
                                StreamExtrasStore.updateImage(index, save = false) {
                                    // o maior movimento decide quanto cresce/encolhe (mantém a proporção)
                                    val byX = drag.x / sw
                                    val byY = drag.y / sw * (it.cropWidth / it.cropHeight)
                                    val grow = if (abs(byX) >= abs(byY)) byX else byY
                                    StreamExtrasStore.fitToScreen(
                                        it.copy(w = (it.w + grow).coerceIn(StreamExtrasStore.MIN_SIZE, 1f - it.x)),
                                        sw, sh
                                    )
                                }
                            }
                        }
                )
            }
        }
        EditorTopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            hint = stringResource(R.string.cryo_stream_edit_images_hint),
            onDone = {
                StreamExtrasStore.saveImages()
                StreamExtrasStore.editing = null
            }
        )
    }
}

/** Textos: arrastar move; o canto muda o tamanho da letra. */
@Composable
private fun TextsEditor(modifier: Modifier) {
    val ice = CryoGlassColors.Ice
    val measured = remember { mutableStateMapOf<Int, IntSize>() }
    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(indication = null, interactionSource = null) {}
    ) {
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        StreamExtrasStore.texts.forEachIndexed { index, item ->
            if (!item.enabled || item.text.isBlank()) return@forEachIndexed
            val box = measured[index] ?: IntSize.Zero
            fun clampX(x: Float) = x.coerceIn(0f, (1f - box.width / sw).coerceAtLeast(0f))
            fun clampY(y: Float) = y.coerceIn(0f, (1f - box.height / sh).coerceAtLeast(0f))
            Box(
                modifier = Modifier
                    .offset { IntOffset((item.x * sw).roundToInt(), (item.y * sh).roundToInt()) }
                    .onSizeChanged { measured[index] = it }
                    .border(2.dp, ice, RoundedCornerShape(4.dp))
                    .pointerInput(index, sw, sh) {
                        detectDragGestures(onDragEnd = { StreamExtrasStore.saveTexts() }) { change, drag ->
                            change.consume()
                            StreamExtrasStore.updateText(index, save = false) {
                                it.copy(x = clampX(it.x + drag.x / sw), y = clampY(it.y + drag.y / sh))
                            }
                        }
                    }
            ) {
                StreamTextView(modifier = Modifier.padding(end = 14.dp, bottom = 14.dp), item = item)
                Box(modifier = Modifier.align(Alignment.TopStart).padding(2.dp)) { NumberBadge(index + 1) }
                SizeHandle(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .pointerInput(index) {
                            var acc = 0f
                            detectDragGestures(
                                onDragStart = { acc = 0f },
                                onDragEnd = { StreamExtrasStore.saveTexts() }
                            ) { change, drag ->
                                change.consume()
                                acc += (drag.x + drag.y) / 2f / density
                                // a cada ~3 dp de arrasto, 1 sp a mais ou a menos
                                val steps = (acc / 3f).toInt()
                                if (steps != 0) {
                                    acc -= steps * 3f
                                    StreamExtrasStore.updateText(index, save = false) {
                                        it.copy(size = (it.size + steps).coerceIn(StreamExtrasStore.TEXT_MIN, StreamExtrasStore.TEXT_MAX))
                                    }
                                }
                            }
                        }
                )
            }
        }
        EditorTopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            hint = stringResource(R.string.cryo_stream_edit_texts_hint),
            onDone = {
                StreamExtrasStore.saveTexts()
                StreamExtrasStore.editing = null
            }
        )
    }
}

private enum class Handle { MOVE, L, T, R, B, LT, RT, LB, RB }

/** Recorte do PNG: a imagem inteira no meio da tela com um retângulo por cima. */
@Composable
private fun ImageCropEditor(modifier: Modifier, index: Int) {
    val image = StreamExtrasStore.images.getOrNull(index)
    if (image == null || !image.hasImage) {
        StreamExtrasStore.croppingImage = null
        return
    }
    val ice = CryoGlassColors.Ice
    val current by rememberUpdatedState(image)
    val bmp = rememberStreamBitmap(image.file)
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(indication = null, interactionSource = null) {}
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            EditorTopBar(
                modifier = Modifier,
                hint = stringResource(R.string.cryo_stream_crop_hint),
                secondary = stringResource(R.string.cryo_stream_crop_all),
                onSecondary = {
                    StreamExtrasStore.updateImage(index) { it.copy(cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f) }
                },
                onDone = {
                    StreamExtrasStore.saveImages()
                    StreamExtrasStore.croppingImage = null
                }
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val availW = constraints.maxWidth.toFloat()
            val availH = constraints.maxHeight.toFloat()
            val scale = min(availW / image.imgW, availH / image.imgH)
            val boxW = image.imgW * scale
            val boxH = image.imgH * scale
            Box(modifier = Modifier.size(with(density) { boxW.toDp() }, with(density) { boxH.toDp() })) {
                // xadrez para ver o que é transparente
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cell = 12.dp.toPx()
                    var yy = 0f
                    var row = 0
                    while (yy < size.height) {
                        var xx = if (row % 2 == 0) 0f else cell
                        while (xx < size.width) {
                            drawRect(Color.White.copy(alpha = 0.10f), Offset(xx, yy), Size(cell, cell))
                            xx += cell * 2
                        }
                        yy += cell
                        row++
                    }
                }
                if (bmp != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = bmp,
                            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                            filterQuality = FilterQuality.Medium
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(index, boxW, boxH) {
                            var handle = Handle.MOVE
                            val grab = 26.dp.toPx()
                            detectDragGestures(
                                onDragStart = { p ->
                                    val s = current
                                    val l = s.cropL * boxW
                                    val t = s.cropT * boxH
                                    val r = s.cropR * boxW
                                    val b = s.cropB * boxH
                                    val nearL = abs(p.x - l) < grab
                                    val nearR = abs(p.x - r) < grab
                                    val nearT = abs(p.y - t) < grab
                                    val nearB = abs(p.y - b) < grab
                                    handle = when {
                                        nearL && nearT -> Handle.LT
                                        nearR && nearT -> Handle.RT
                                        nearL && nearB -> Handle.LB
                                        nearR && nearB -> Handle.RB
                                        nearL && p.y in t..b -> Handle.L
                                        nearR && p.y in t..b -> Handle.R
                                        nearT && p.x in l..r -> Handle.T
                                        nearB && p.x in l..r -> Handle.B
                                        else -> Handle.MOVE
                                    }
                                },
                                onDragEnd = { StreamExtrasStore.saveImages() }
                            ) { change, drag ->
                                change.consume()
                                val dx = drag.x / boxW
                                val dy = drag.y / boxH
                                val m = StreamExtrasStore.MIN_CROP
                                StreamExtrasStore.updateImage(index, save = false) { s ->
                                    var l = s.cropL
                                    var t = s.cropT
                                    var r = s.cropR
                                    var b = s.cropB
                                    if (handle == Handle.MOVE) {
                                        val w = r - l
                                        val h = b - t
                                        l = (l + dx).coerceIn(0f, 1f - w); r = l + w
                                        t = (t + dy).coerceIn(0f, 1f - h); b = t + h
                                    } else {
                                        if (handle == Handle.L || handle == Handle.LT || handle == Handle.LB) l = (l + dx).coerceIn(0f, r - m)
                                        if (handle == Handle.R || handle == Handle.RT || handle == Handle.RB) r = (r + dx).coerceIn(l + m, 1f)
                                        if (handle == Handle.T || handle == Handle.LT || handle == Handle.RT) t = (t + dy).coerceIn(0f, b - m)
                                        if (handle == Handle.B || handle == Handle.LB || handle == Handle.RB) b = (b + dy).coerceIn(t + m, 1f)
                                    }
                                    // recortar não muda o tamanho do que sobra na tela: a largura acompanha o recorte
                                    val keepScale = s.w / (s.cropR - s.cropL)
                                    s.copy(cropL = l, cropT = t, cropR = r, cropB = b, w = keepScale * (r - l))
                                }
                            }
                        }
                ) {
                    val l = image.cropL * size.width
                    val t = image.cropT * size.height
                    val r = image.cropR * size.width
                    val b = image.cropB * size.height
                    val shade = Color.Black.copy(alpha = 0.6f)
                    drawRect(shade, Offset(0f, 0f), Size(size.width, t))
                    drawRect(shade, Offset(0f, b), Size(size.width, size.height - b))
                    drawRect(shade, Offset(0f, t), Size(l, b - t))
                    drawRect(shade, Offset(r, t), Size(size.width - r, b - t))
                    drawRect(Color.White.copy(alpha = 0.35f), Offset(0f, 0f), size, style = Stroke(1.dp.toPx()))
                    drawRect(ice, Offset(l, t), Size(r - l, b - t), style = Stroke(2.5.dp.toPx()))
                    for (k in 1..2) {
                        val gx = l + (r - l) * k / 3f
                        val gy = t + (b - t) * k / 3f
                        drawLine(ice.copy(alpha = 0.3f), Offset(gx, t), Offset(gx, b), 1.dp.toPx())
                        drawLine(ice.copy(alpha = 0.3f), Offset(l, gy), Offset(r, gy), 1.dp.toPx())
                    }
                    val hs = 9.dp.toPx()
                    listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { c ->
                        drawRect(ice, Offset(c.x - hs / 2f, c.y - hs / 2f), Size(hs, hs))
                    }
                }
            }
        }
    }
}
