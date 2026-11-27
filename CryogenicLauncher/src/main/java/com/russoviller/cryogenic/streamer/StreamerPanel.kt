package com.russoviller.cryogenic.streamer

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.MenuSliderLayout
import com.russoviller.cryogenic.ui.components.MenuSwitchButton
import com.russoviller.cryogenic.ui.components.MenuTextButton
import com.russoviller.cryogenic.ui.components.lazyScrollWithBar

/**
 * Painel "Modo streamer" do menu do jogo.
 * Início: "Widgets na tela", "PNGs na tela" e "Textos na tela". Dentro: volume (vale para as 4), e para cada sobreposição:
 * ligar/desligar, link e opacidade; e o botão para ajustar as posições na tela.
 */
@Composable
fun StreamerPanel(
    modifier: Modifier,
    onAdjustPositions: () -> Unit,
    onCloseMenu: () -> Unit = {},
    color: Color,
    contentColor: Color
) {
    LaunchedEffect(Unit) {
        StreamOverlayStore.ensureLoaded()
        StreamExtrasStore.ensureLoaded()
    }
    var page by remember { mutableIntStateOf(0) }
    var selectedImage by remember { mutableIntStateOf(0) }
    var selectedText by remember { mutableIntStateOf(0) }
    val picker = rememberStreamImagePicker(onImported = { selectedImage = it })
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.lazyScrollWithBar(listState),
        state = listState,
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (page == 0) {
            item(key = "home") {
                StreamerHome(
                    onOpen = { page = it },
                    onPlaceWidgets = onAdjustPositions,
                    onPlaceImages = {
                        StreamExtrasStore.croppingImage = null
                        StreamExtrasStore.editing = StreamEditMode.IMAGES
                        onCloseMenu()
                    },
                    onPlaceTexts = {
                        StreamExtrasStore.editing = StreamEditMode.TEXTS
                        onCloseMenu()
                    }
                )
            }
        } else if (page == 2) {
            backHeader(R.string.cryo_stream_images) { page = 0 }
            streamImagesPage(
                selected = selectedImage,
                picker = picker,
                onSelect = { selectedImage = it },
                onOpenEditor = { crop ->
                    StreamExtrasStore.croppingImage = crop
                    StreamExtrasStore.editing = StreamEditMode.IMAGES
                    onCloseMenu()
                },
                color = color,
                contentColor = contentColor
            )
        } else if (page == 3) {
            backHeader(R.string.cryo_stream_texts) { page = 0 }
            streamTextsPage(
                selected = selectedText,
                onSelect = { selectedText = it },
                onOpenEditor = {
                    StreamExtrasStore.editing = StreamEditMode.TEXTS
                    onCloseMenu()
                },
                color = color,
                contentColor = contentColor
            )
        } else {
            item {
                Text(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { page = 0 }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    text = "‹  " + stringResource(R.string.cryo_stream_overlays),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Ice
                )
            }
            item {
                MenuSliderLayout(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.cryo_stream_volume),
                    value = StreamOverlayStore.volume,
                    onValueChange = { StreamOverlayStore.changeVolume(it) },
                    valueRange = 0f..100f,
                    suffix = "%",
                    color = color,
                    contentColor = contentColor
                )
            }
            item {
                MenuTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cryo_stream_adjust),
                    enabled = StreamOverlayStore.slots.any { it.enabled },
                    color = color,
                    contentColor = contentColor,
                    onClick = onAdjustPositions
                )
            }
            for (i in 0 until StreamOverlayStore.COUNT) {
                item(key = "slot$i") {
                    OverlaySlotCard(index = i, color = color, contentColor = contentColor)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.backHeader(title: Int, onBack: () -> Unit) {
    item(key = "back$title") {
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            text = "‹  " + stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Ice
        )
    }
}

@Composable
private fun OverlaySlotCard(index: Int, color: Color, contentColor: Color) {
    val slot = StreamOverlayStore.slots.getOrNull(index) ?: return
    val ice = CryoGlassColors.Ice
    val focus = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CryoGlassColors.Deep.copy(alpha = 0.55f))
            .border(1.dp, ice.copy(alpha = if (slot.enabled) 0.7f else 0.25f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cryo_stream_overlay_n, index + 1),
            switch = slot.enabled,
            onSwitch = { on -> StreamOverlayStore.update(index) { it.copy(enabled = on) } },
            color = color,
            contentColor = contentColor
        )

        // link: só é aplicado ao confirmar (✓ ou "Concluído" no teclado), para a página não
        // recarregar a cada letra digitada
        var text by remember(index) { mutableStateOf(slot.url) }
        val dirty = text.trim() != slot.url.trim()
        val apply = {
            StreamOverlayStore.update(index) { it.copy(url = text.trim()) }
            focus.clearFocus()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, ice.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cryo_stream_url_hint),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it.replace("\n", "") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(ice),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { apply() })
                )
            }
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (dirty) ice else Color.Transparent)
                    .clickable(enabled = dirty) { apply() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                text = "✓",
                color = if (dirty) CryoGlassColors.Deep else ice.copy(alpha = 0.4f),
                fontWeight = FontWeight.Black
            )
        }

        MenuSliderLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.cryo_stream_opacity),
            value = slot.opacity,
            onValueChange = { v -> StreamOverlayStore.update(index, save = false) { it.copy(opacity = v) } },
            onValueChangeFinished = { StreamOverlayStore.save() },
            valueRange = 0f..100f,
            suffix = "%",
            color = color,
            contentColor = contentColor
        )
    }
}

/** Um módulo da tela inicial do modo streamer. */
private class StreamModule(
    val page: Int,
    val iconRes: Int,
    val titleRes: Int,
    val subRes: Int,
    val active: Int,
    val filled: Int,
    val total: Int,
    val onPlace: () -> Unit
)

/**
 * Início do modo streamer: um cartão por módulo com o que está ligado,
 * os espaços usados e atalho direto para posicionar na tela.
 */
@Composable
private fun StreamerHome(
    onOpen: (Int) -> Unit,
    onPlaceWidgets: () -> Unit,
    onPlaceImages: () -> Unit,
    onPlaceTexts: () -> Unit
) {
    val accent = CryoGlassColors.Ice
    val overlays = StreamOverlayStore.slots
    val images = StreamExtrasStore.images
    val texts = StreamExtrasStore.texts
    val modules = listOf(
        StreamModule(
            page = 1, iconRes = R.drawable.ic_link, titleRes = R.string.cryo_stream_overlays, subRes = R.string.cryo_stream_card_widgets_sub,
            active = overlays.count { it.enabled && it.url.isNotBlank() }, filled = overlays.count { it.url.isNotBlank() },
            total = StreamOverlayStore.COUNT, onPlace = onPlaceWidgets
        ),
        StreamModule(
            page = 2, iconRes = R.drawable.ic_photo_library_outlined, titleRes = R.string.cryo_stream_images, subRes = R.string.cryo_stream_card_images_sub,
            active = images.count { it.enabled && it.hasImage }, filled = images.count { it.hasImage },
            total = StreamExtrasStore.IMAGE_COUNT, onPlace = onPlaceImages
        ),
        StreamModule(
            page = 3, iconRes = R.drawable.ic_text_format, titleRes = R.string.cryo_stream_texts, subRes = R.string.cryo_stream_card_texts_sub,
            active = texts.count { it.enabled && it.text.isNotBlank() }, filled = texts.count { it.text.isNotBlank() },
            total = StreamExtrasStore.TEXT_COUNT, onPlace = onPlaceTexts
        )
    )
    val onScreen = modules.sumOf { it.active }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        //faixa de status: quantos itens estão aparecendo na live agora
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.25f))))
                .border(1.dp, Brush.horizontalGradient(listOf(accent.copy(alpha = 0.8f), accent.copy(alpha = 0.1f))), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (onScreen > 0) Color(0xFFFF4D5E) else Color.White.copy(alpha = 0.3f))
                    .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cryo_stream_home_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.cryo_stream_home_sub),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = onScreen.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Black,
                    color = accent
                )
                Text(
                    text = stringResource(R.string.cryo_stream_on_screen),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = accent.copy(alpha = 0.85f)
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 560.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    modules.forEach { m -> ModuleCard(Modifier.weight(1f), m, onOpen) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modules.forEach { m -> ModuleCard(Modifier.fillMaxWidth(), m, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(modifier: Modifier, module: StreamModule, onOpen: (Int) -> Unit) {
    val accent = CryoGlassColors.Ice
    val on = module.active > 0
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Black.copy(alpha = 0.3f))))
            .border(if (on) 1.5.dp else 1.dp, accent.copy(alpha = if (on) 0.85f else 0.3f), shape)
            .clickable { onOpen(module.page) }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(accent, CryoGlassColors.Aurora))),
                contentAlignment = Alignment.Center
            ) {
                Icon(modifier = Modifier.size(22.dp), painter = painterResource(module.iconRes), contentDescription = null, tint = CryoGlassColors.Deep)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(module.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(module.subRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        //espaços: cheio e ligado / cheio e desligado / vazio
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(module.total) { i ->
                val state = when {
                    i < module.active -> 2
                    i < module.filled -> 1
                    else -> 0
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                2 -> accent
                                1 -> accent.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.1f)
                            }
                        )
                )
            }
        }
        Text(
            text = stringResource(R.string.cryo_stream_active_count, module.active, module.total),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = if (on) accent else Color.White.copy(alpha = 0.55f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent, CryoGlassColors.Aurora)))
                    .clickable { onOpen(module.page) }
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cryo_stream_open),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = CryoGlassColors.Deep,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (on) 1f else 0.4f)
                    .clip(CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.6f), CircleShape)
                    .clickable(enabled = on, onClick = module.onPlace)
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cryo_stream_place),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

