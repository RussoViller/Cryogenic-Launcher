package com.russoviller.cryogenic.ui.screens.content.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import com.materialkolor.PaletteStyle
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoCustomGradient
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.DefaultSwitch
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoSpecFor
import com.russoviller.cryogenic.ui.components.withGradient
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import com.russoviller.cryogenic.ui.theme.launcherColorScheme

/*
 * Cor personalizada do Launcher.
 * - Seletor: quadro de saturação/brilho, barra de matiz e barra Escuro ↔ Claro.
 * - Degradê personalizado: até 5 cores (liga/desliga cada uma do meio), paleta enorme em cada
 *   cor e degradês prontos.
 */

/** Paleta grande: 12 matizes × 7 tons + 12 cinzas. */
private val bigPalette: List<Color> = buildList {
    val tones = floatArrayOf(0.18f, 0.30f, 0.42f, 0.54f, 0.66f, 0.78f, 0.90f)
    tones.forEach { l ->
        for (i in 0 until 12) {
            add(Color(ColorUtils.HSLToColor(floatArrayOf(i * 30f, 0.85f, l))))
        }
    }
    for (i in 0 until 12) {
        val v = i / 11f
        add(Color(v, v, v))
    }
}

/** Degradês prontos. */
private val gradientPresets: List<List<Long>> = listOf(
    listOf(0xFFFFD36B, 0xFFFFA0A0, 0xFFF6C3A4, 0xFFE29B3C, 0xFFDB3A21),
    listOf(0xFF4FB8F0, 0xFF7FD8FF, 0xFF6FE3D2),
    listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364),
    listOf(0xFFFF512F, 0xFFF09819, 0xFFFFE259),
    listOf(0xFF8E2DE2, 0xFF4A00E0, 0xFF00C6FF),
    listOf(0xFFFC466B, 0xFF3F5EFB),
    listOf(0xFF00F260, 0xFF0575E6),
    listOf(0xFFF953C6, 0xFFB91D73),
    listOf(0xFFFF9A8B, 0xFFFF6A88, 0xFFFF99AC),
    listOf(0xFF43CEA2, 0xFF185A9D),
    listOf(0xFFFFE000, 0xFF799F0C),
    listOf(0xFFDA22FF, 0xFF9733EE, 0xFF4A00E0),
    listOf(0xFFFF0844, 0xFFFFB199),
    listOf(0xFF1FA2FF, 0xFF12D8FA, 0xFFA6FFCB),
    listOf(0xFFF7971E, 0xFFFFD200),
    listOf(0xFF833AB4, 0xFFFD1D1D, 0xFFFCB045),
    listOf(0xFF00C9FF, 0xFF92FE9D),
    listOf(0xFFEE0979, 0xFFFF6A00),
    listOf(0xFF7F00FF, 0xFFE100FF),
    listOf(0xFF2193B0, 0xFF6DD5ED),
    listOf(0xFFC33764, 0xFF1D2671),
    listOf(0xFFB993D6, 0xFF8CA6DB),
    listOf(0xFF56AB2F, 0xFFA8E063),
    listOf(0xFFFF4E50, 0xFFF9D423),
    listOf(0xFF3A1C71, 0xFFD76D77, 0xFFFFAF7B),
    listOf(0xFF0BA360, 0xFF3CBA92, 0xFF30DD8A),
    listOf(0xFFE65C00, 0xFFF9D423),
    listOf(0xFF8360C3, 0xFF2EBF91),
    listOf(0xFFFFFFFF, 0xFFB0BEC5, 0xFF37474F),
    listOf(0xFFFF6B6B, 0xFFFFD93D, 0xFF6BCB77, 0xFF4D96FF, 0xFF9B5DE5),
)

private fun hsvOf(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

private fun colorOf(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

private fun hexOf(color: Color): String = String.format("%06X", color.toArgb() and 0xFFFFFF)

private fun lightnessOf(color: Color): Float {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    return hsl[2]
}

private fun withLightness(color: Color, l: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[2] = l.coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
fun ColorStudioDialog(
    initialColor: Color,
    initialStyle: PaletteStyle,
    initialGradient: String,
    onCancel: () -> Unit,
    onConfirm: (Color, PaletteStyle, String) -> Unit
) {
    val context = LocalContext.current
    var baseColor by remember { mutableStateOf(initialColor) }
    var style by remember { mutableStateOf(initialStyle) }
    val startGradient = remember { CryoCustomGradient.parse(initialGradient) }
    var gradientOn by remember { mutableStateOf(startGradient.enabled) }
    val stops = remember { mutableStateListOf<CryoCustomGradient.Stop>().apply { addAll(startGradient.stops) } }
    //O que o seletor está editando: -1 = cor base; 0..4 = uma cor do degradê
    var editing by remember { mutableIntStateOf(if (startGradient.enabled) 0 else -1) }

    val target: Color = if (editing >= 0) stops[editing].color else baseColor
    //HSV do seletor (guarda o matiz mesmo em cinzas, para a barra não "pular")
    val start = remember { hsvOf(target) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hexText by remember { mutableStateOf(hexOf(target)) }

    fun write(c: Color) {
        if (editing >= 0) stops[editing] = stops[editing].copy(color = c) else baseColor = c
        hexText = hexOf(c)
    }

    fun load(c: Color) {
        val hsv = hsvOf(c)
        if (hsv[1] > 0.01f && hsv[2] > 0.01f) hue = hsv[0]
        sat = hsv[1]; value = hsv[2]
        hexText = hexOf(c)
    }

    fun select(index: Int) {
        editing = index
        load(if (index >= 0) stops[index].color else baseColor)
    }

    fun setFromPicker() = write(colorOf(hue, sat, value))

    val config = CryoCustomGradient.Config(gradientOn, stops.toList())
    val activeGradient = if (gradientOn) config.colors else null
    val seed = if (gradientOn) config.colors[config.colors.size / 2] else baseColor
    val spec = remember(seed, style, activeGradient) {
        cryoSpecFor(ColorThemeType.CUSTOM, launcherColorScheme(context, ColorThemeType.CUSTOM, true, seed, style), seed)
            .withGradient(activeGradient)
    }
    val previewColors = spec.gradient ?: listOf(spec.iceSoft, spec.ice, spec.aurora)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(com.russoviller.cryogenic.ui.components.LocalCryoGlassFixed provides true) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight()
                .cryoGlass(shape = cryoChamferShape(24.dp), cornerRadius = 24.dp, window = true)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------- cabeçalho ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cryo_color_kicker),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                        color = CryoGlassColors.Ice
                    )
                    Text(
                        text = stringResource(R.string.cryo_color_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            brush = Brush.horizontalGradient(listOf(Color.White) + previewColors)
                        )
                    )
                }
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Deep.copy(alpha = 0.7f))
                        .border(1.dp, target.copy(alpha = 0.9f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    text = stringResource(
                        R.string.cryo_color_editing,
                        if (editing >= 0) stringResource(R.string.cryo_gradient_stop, editing + 1)
                        else stringResource(R.string.cryo_color_base)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---------- seletor ----------
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hueColor = colorOf(hue, 1f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(editing) {
                                    detectTapGestures {
                                        sat = (it.x / size.width).coerceIn(0f, 1f)
                                        value = 1f - (it.y / size.height).coerceIn(0f, 1f)
                                        setFromPicker()
                                    }
                                }
                                .pointerInput(editing) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                        value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                        setFromPicker()
                                    }
                                }
                        ) {
                            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                            val p = Offset(sat * size.width, (1f - value) * size.height)
                            drawCircle(Color.Black.copy(alpha = 0.5f), 13.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                            drawCircle(Color.White, 11.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                            drawCircle(target, 8.dp.toPx(), p)
                        }
                    }

                    // matiz: barra mais grossa, com as 12 cores e um marcador com a cor atual
                    PickerBar(
                        label = stringResource(R.string.cryo_color_hue),
                        valueText = "${hue.toInt()}°",
                        fraction = hue / 360f,
                        thumbColor = hueColor,
                        key = editing,
                        paint = { drawRect(Brush.horizontalGradient((0..12).map { colorOf(it * 30f, 1f, 1f) })) },
                        onChange = { f ->
                            hue = f * 360f
                            if (sat < 0.05f) sat = 0.85f
                            if (value < 0.05f) value = 0.85f
                            setFromPicker()
                        }
                    )

                    // escuro ↔ claro: preto → cor → branco
                    val light = lightnessOf(target)
                    PickerBar(
                        label = stringResource(R.string.cryo_color_dark) + "  ↔  " + stringResource(R.string.cryo_color_light),
                        valueText = "${(light * 100).toInt()}%",
                        fraction = light,
                        thumbColor = target,
                        key = editing,
                        paint = {
                            val mid = withLightness(colorOf(hue, sat.coerceAtLeast(0.5f), 1f), 0.5f)
                            drawRect(Brush.horizontalGradient(listOf(Color.Black, mid, Color.White)))
                        },
                        onChange = { f ->
                            val c = withLightness(colorOf(hue, sat.coerceAtLeast(0.02f), value.coerceAtLeast(0.02f)), f)
                            val hsv = hsvOf(c)
                            sat = hsv[1]; value = hsv[2]
                            write(c)
                        }
                    )
                }

                // ---------- direita: hex, degradê, paletas ----------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(cryoChamferShape(12.dp))
                                .background(target)
                                .border(2.dp, Color.White.copy(alpha = 0.7f), cryoChamferShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HEX",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = CryoGlassColors.Ice
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CryoGlassColors.Deep.copy(alpha = 0.7f))
                                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = CryoGlassColors.Ice
                                )
                                BasicTextField(
                                    modifier = Modifier.weight(1f),
                                    value = hexText,
                                    onValueChange = { input ->
                                        val clean = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                                        hexText = clean
                                        if (clean.length == 6) {
                                            val c = Color(0xFF000000 or clean.toLong(16))
                                            load(c)
                                            write(c)
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                    cursorBrush = SolidColor(CryoGlassColors.Ice)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(previewColors))
                    )

                    // ----- degradê personalizado -----
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cryo_gradient_custom_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.cryo_gradient_custom_summary),
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                            )
                        }
                        DefaultSwitch(
                            checked = gradientOn,
                            onCheckedChange = {
                                gradientOn = it
                                select(if (it) 0 else -1)
                            }
                        )
                    }
                    if (gradientOn) {
                        GradientEditor(
                            stops = stops,
                            editing = editing,
                            onSelect = { select(it) },
                            onToggle = { i, on -> stops[i] = stops[i].copy(enabled = on) },
                            onPick = { i, c ->
                                stops[i] = stops[i].copy(color = c)
                                select(i)
                            }
                        )
                        Text(
                            text = stringResource(R.string.cryo_gradient_presets),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                            color = CryoGlassColors.Ice
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gradientPresets.forEach { preset ->
                                val colors = preset.map { Color(it) }
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 26.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.horizontalGradient(colors))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                                        .clickable {
                                            //Distribui as cores do degradê pronto nas 5 posições
                                            val n = colors.size
                                            for (i in 0 until CryoCustomGradient.MAX_STOPS) {
                                                val on = when (n) {
                                                    2 -> i == 0 || i == 4
                                                    3 -> i == 0 || i == 2 || i == 4
                                                    4 -> i != 2
                                                    else -> true
                                                }
                                                val idx = when (n) {
                                                    2 -> if (i < 2) 0 else 1
                                                    3 -> i / 2
                                                    4 -> if (i < 2) i else (i - 1).coerceAtMost(3)
                                                    else -> i.coerceAtMost(n - 1)
                                                }
                                                stops[i] = CryoCustomGradient.Stop(colors[idx], on)
                                            }
                                            select(editing.coerceAtLeast(0))
                                        }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.cryo_color_presets),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                            color = CryoGlassColors.Ice
                        )
                        PaletteGrid(selected = baseColor) { c ->
                            load(c)
                            write(c)
                        }
                    }

                    Text(
                        text = stringResource(R.string.cryo_color_style),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                        color = CryoGlassColors.Ice
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaletteStyle.entries.forEach { entry ->
                            val active = entry == style
                            Text(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (active) Modifier.cryoBackground(cryoGradient())
                                        else Modifier.background(CryoGlassColors.Deep.copy(alpha = 0.6f))
                                    )
                                    .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (active) 0.9f else 0.3f), RoundedCornerShape(10.dp))
                                    .clickable { style = entry }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                text = entry.name,
                                style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                                fontWeight = FontWeight.Bold,
                                color = if (active) CryoGlassColors.Deep else Color.White
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CryoTonalButton(modifier = Modifier.weight(1f), onClick = onCancel) {
                    MarqueeText(text = stringResource(R.string.generic_cancel))
                }
                CryoButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        //Com degradê, a cor base do tema (fundos, textos) acompanha a cor do meio
                        onConfirm(seed, style, CryoCustomGradient.encode(config))
                    }
                ) {
                    MarqueeText(text = stringResource(R.string.cryo_theme_apply))
                }
            }
        }
            }
    }
}

/** Barra de ajuste arrastável, com rótulo, valor e marcador que mostra a cor escolhida. */
@Composable
private fun PickerBar(
    label: String,
    valueText: String,
    fraction: Float,
    thumbColor: Color,
    key: Any,
    paint: DrawScope.() -> Unit,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Frost.copy(alpha = 0.85f),
                maxLines = 1
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = CryoGlassColors.Ice
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(key) {
                    detectTapGestures {
                        val pad = size.height / 2f
                        onChange(((it.x - pad) / (size.width - pad * 2)).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(key) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val pad = size.height / 2f
                        onChange(((change.position.x - pad) / (size.width - pad * 2)).coerceIn(0f, 1f))
                    }
                }
        ) {
            val track = size.height * 0.62f
            val top = (size.height - track) / 2f
            val r = CornerRadius(track / 2f)
            // trilho com a pintura recortada em pílula
            val path = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, top, size.width, top + track, r))
            }
            clipPath(path) { paint() }
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(0f, top),
                size = Size(size.width, track),
                cornerRadius = r,
                style = Stroke(1.dp.toPx())
            )
            // marcador: círculo com a cor atual, anel branco e contorno escuro
            val pad = size.height / 2f
            val x = pad + fraction.coerceIn(0f, 1f) * (size.width - pad * 2)
            val c = Offset(x, size.height / 2f)
            drawCircle(Color.Black.copy(alpha = 0.45f), size.height / 2f, c)
            drawCircle(Color.White, size.height / 2f - 2.dp.toPx(), c)
            drawCircle(thumbColor, size.height / 2f - 5.dp.toPx(), c)
            if (thumbColor.luminance() > 0.85f) {
                drawCircle(Color.Black.copy(alpha = 0.25f), size.height / 2f - 5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/** Editor do degradê: barra vertical com o resultado + 5 cores (as do meio podem ser desligadas). */
@Composable
private fun GradientEditor(
    stops: List<CryoCustomGradient.Stop>,
    editing: Int,
    onSelect: (Int) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onPick: (Int, Color) -> Unit
) {
    val colors = CryoCustomGradient.Config(true, stops).colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height((CryoCustomGradient.MAX_STOPS * 44 - 8).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(colors))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stops.forEachIndexed { i, stop ->
                val fixed = i == 0 || i == stops.lastIndex
                Row(
                    modifier = Modifier.height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // caixa de seleção (a primeira e a última cor ficam sempre ligadas)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .then(
                                if (fixed) Modifier
                                else Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (stop.enabled) Color(0xFFE6EAF0) else Color.Transparent)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .clickable { onToggle(i, !stop.enabled) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!fixed && stop.enabled) {
                            Text(text = "✓", color = Color(0xFF1C2230), fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                    // cor (toque = editar no seletor)
                    val active = i == editing
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(stop.color.copy(alpha = if (stop.enabled || fixed) 1f else 0.35f))
                            .border(
                                if (active) 2.5.dp else 1.dp,
                                if (active) Color.White else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelect(i) }
                    )
                    // ▼ = paleta grande
                    var open by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { open = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            text = "▼",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        DropdownMenu(
                            expanded = open,
                            onDismissRequest = { open = false },
                            containerColor = CryoGlassColors.Deep
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                PaletteGrid(selected = stop.color, swatch = 22) { c ->
                                    onPick(i, c)
                                    open = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Grade com a paleta grande (96 cores). */
@Composable
private fun PaletteGrid(selected: Color, swatch: Int = 24, onPick: (Color) -> Unit) {
    val sel = selected.toArgb() and 0xFFFFFF
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        bigPalette.chunked(12).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { c ->
                    val active = (c.toArgb() and 0xFFFFFF) == sel
                    Box(
                        modifier = Modifier
                            .size(swatch.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(c)
                            .border(
                                if (active) 2.dp else 0.5.dp,
                                if (active) Color.White else Color.White.copy(alpha = 0.18f),
                                RoundedCornerShape(5.dp)
                            )
                            .clickable { onPick(c) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
