package com.russoviller.cryogenic.ui.screens.content.settings

import android.os.Build
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.setting.AllSettings
import com.russoviller.cryogenic.setting.enums.isLauncherInDarkTheme
import com.russoviller.cryogenic.ui.components.CryoButton
import com.russoviller.cryogenic.ui.components.CryoColorAdjust
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.CryoSlider
import com.russoviller.cryogenic.ui.components.CryoSpec
import com.russoviller.cryogenic.ui.components.CryoTheme
import com.russoviller.cryogenic.ui.components.CryoTonalButton
import com.russoviller.cryogenic.ui.components.DefaultSwitch
import com.russoviller.cryogenic.ui.components.MarqueeText
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.components.cryoSpecFor
import com.russoviller.cryogenic.ui.components.withGradient
import com.russoviller.cryogenic.ui.theme.ColorThemeType
import com.russoviller.cryogenic.ui.theme.launcherColorScheme

@Composable
fun themeTypeName(type: ColorThemeType): String = stringResource(
    when (type) {
        ColorThemeType.DYNAMIC -> R.string.theme_color_dynamic
        ColorThemeType.CRYOGENIC -> R.string.theme_color_cryogenic
        ColorThemeType.URBAN_ASH -> R.string.theme_color_urban_ash
        ColorThemeType.VOLCANIC -> R.string.cryo_theme_volcanic
        ColorThemeType.MARINE -> R.string.cryo_theme_marine
        ColorThemeType.GALAXY -> R.string.cryo_theme_galaxy
        ColorThemeType.VERDEJANTE -> R.string.cryo_theme_verdejante
        ColorThemeType.DESERT -> R.string.cryo_theme_desert
        ColorThemeType.GOLD -> R.string.cryo_theme_gold
        ColorThemeType.SAKURA -> R.string.cryo_theme_sakura
        ColorThemeType.SILVER -> R.string.cryo_theme_silver
        ColorThemeType.CUSTOM -> R.string.generic_custom
    }
)

@Composable
private fun themeTag(type: ColorThemeType): String = when (type) {
    ColorThemeType.DYNAMIC -> stringResource(R.string.cryo_theme_tag_dynamic)
    ColorThemeType.CUSTOM -> stringResource(R.string.cryo_theme_tag_custom)
    else -> stringResource(R.string.cryo_theme_tag_gradient)
}

private fun themeAvailable(type: ColorThemeType) =
    type != ColorThemeType.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
private fun rememberThemeSpec(type: ColorThemeType, adjust: CryoColorAdjust): CryoSpec {
    val context = LocalContext.current
    val dark = isLauncherInDarkTheme()
    val customColor = Color(AllSettings.launcherCustomColor.state)
    val style = AllSettings.launcherCustomPaletteStyle.state
    val customGradient = AllSettings.cryoCustomGradient.state
    return remember(type, adjust, dark, customColor, style, customGradient) {
        val scheme = adjust.apply(launcherColorScheme(context, type, dark, customColor, style))
        val spec = cryoSpecFor(type, scheme, customColor)
        adjust.apply(
            if (type == ColorThemeType.CUSTOM) {
                spec.withGradient(com.russoviller.cryogenic.ui.components.CryoCustomGradient.activeColors(customGradient))
            } else spec
        )
    }
}

private fun specGradient(spec: CryoSpec): Brush =
    Brush.horizontalGradient(spec.gradient ?: listOf(spec.iceSoft, spec.ice, spec.aurora))

private fun Modifier.specGradientFill(spec: CryoSpec, animated: Boolean): Modifier = drawBehind {
    if (animated) {
        val w = size.width.coerceAtLeast(1f)
        val shift = CryoTheme.gradientPhase.floatValue * 2f * w
        drawRect(
            Brush.linearGradient(
                colors = spec.gradient?.let { it + it.reversed().drop(1) }
                    ?: listOf(spec.iceSoft, spec.ice, spec.aurora, spec.ice, spec.iceSoft),
                start = Offset(shift - 2f * w, 0f),
                end = Offset(shift, size.height * 0.35f),
                tileMode = TileMode.Repeated
            )
        )
    } else {
        drawRect(specGradient(spec))
    }
}

@Composable
fun ThemeStudioCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val current = AllSettings.launcherColorTheme.state
    Row(
        modifier = modifier
            .fillMaxWidth()
            //Sem fundo próprio: fica transparente como as outras opções (usa o vidro do bloco)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(R.drawable.ic_format_paint_outlined),
                contentDescription = null,
                tint = CryoGlassColors.Deep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.cryo_theme_studio_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                )
            )
            Text(
                text = stringResource(R.string.cryo_theme_studio_summary),
                style = MaterialTheme.typography.labelSmall,
                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.cryo_theme_current).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
                color = CryoGlassColors.Ice
            )
            Text(
                text = themeTypeName(current),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
    }
}

@Composable
fun ThemeStudioDialog(
    onDismiss: () -> Unit,
    onEditCustomColor: () -> Unit
) {
    val applied = AllSettings.launcherColorTheme.state
    var selected by remember { mutableStateOf(applied) }
    var saturation by remember { mutableFloatStateOf(AllSettings.cryoThemeSaturation.getValue().toFloat()) }
    var vibrance by remember { mutableFloatStateOf(AllSettings.cryoThemeVibrance.getValue().toFloat()) }
    var contrast by remember { mutableFloatStateOf(AllSettings.cryoThemeContrast.getValue().toFloat()) }
    val animated = AllSettings.cryoGradientAnimated.state
    var speed by remember { mutableFloatStateOf(AllSettings.cryoGradientSpeed.getValue().toFloat()) }
    val adjust = CryoColorAdjust(saturation / 100f, vibrance / 100f, contrast / 200f)
    val previewSpec = rememberThemeSpec(selected, adjust)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(com.russoviller.cryogenic.ui.components.LocalCryoGlassFixed provides true) {
        Row(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight()
                .cryoGlass(shape = RoundedCornerShape(26.dp), cornerRadius = 26.dp, window = true)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = stringResource(R.string.cryo_theme_studio_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
                Text(
                    text = stringResource(R.string.cryo_theme_pick),
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
                    ColorThemeType.entries.forEach { type ->
                        ThemeTile(
                            type = type,
                            selected = type == selected,
                            applied = type == applied,
                            enabled = themeAvailable(type),
                            onClick = { selected = type }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CryoTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.generic_close))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeHomePreview(
                        modifier = Modifier.weight(1.35f),
                        spec = previewSpec,
                        animated = animated
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cryo_theme_preview).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace),
                            color = previewSpec.ice
                        )
                        Text(
                            text = themeTypeName(selected),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(previewSpec.iceSoft, previewSpec.ice, previewSpec.aurora, previewSpec.navy).forEach {
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
                                .specGradientFill(previewSpec, animated)
                        )
                        Text(
                            text = if (selected == applied) stringResource(R.string.cryo_theme_in_use)
                            else stringResource(R.string.cryo_theme_not_applied),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected == applied) previewSpec.aurora else previewSpec.ice
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CryoButton(
                        modifier = Modifier.weight(1f),
                        enabled = selected != applied && themeAvailable(selected),
                        onClick = {
                            AllSettings.launcherColorTheme.save(selected)
                            if (selected == ColorThemeType.CUSTOM) onEditCustomColor()
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null
                        )
                        MarqueeText(text = stringResource(R.string.cryo_theme_apply))
                    }
                    if (selected == ColorThemeType.CUSTOM) {
                        CryoTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onEditCustomColor
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_format_paint_outlined),
                                contentDescription = null
                            )
                            MarqueeText(text = stringResource(R.string.cryo_theme_custom_color))
                        }
                    }
                    CryoTonalButton(
                        modifier = Modifier.weight(0.8f),
                        onClick = {
                            saturation = 0f
                            vibrance = 0f
                            contrast = 0f
                            AllSettings.cryoThemeSaturation.save(0)
                            AllSettings.cryoThemeVibrance.save(0)
                            AllSettings.cryoThemeContrast.save(0)
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_restart_alt),
                            contentDescription = null
                        )
                        MarqueeText(text = stringResource(R.string.cryo_theme_reset_adjust))
                    }
                }

                StudioSection(stringResource(R.string.cryo_theme_adjust_title)) {
                    AdjustSlider(
                        label = stringResource(R.string.cryo_theme_saturation),
                        value = saturation,
                        onChange = { saturation = it },
                        onFinished = { AllSettings.cryoThemeSaturation.save(saturation.toInt()) }
                    )
                    AdjustSlider(
                        label = stringResource(R.string.cryo_theme_vibrance),
                        value = vibrance,
                        onChange = { vibrance = it },
                        onFinished = { AllSettings.cryoThemeVibrance.save(vibrance.toInt()) }
                    )
                    AdjustSlider(
                        label = stringResource(R.string.cryo_theme_contrast),
                        value = contrast,
                        onChange = { contrast = it },
                        onFinished = { AllSettings.cryoThemeContrast.save(contrast.toInt()) }
                    )
                }

                StudioSection(stringResource(R.string.cryo_theme_gradient_title)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cryo_theme_animate_gradient),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.cryo_theme_animate_gradient_summary),
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.75f)
                            )
                        }
                        DefaultSwitch(
                            checked = animated,
                            onCheckedChange = { AllSettings.cryoGradientAnimated.save(it) }
                        )
                    }
                    if (animated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cryo_theme_speed_slow),
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                            )
                            CryoSlider(
                                modifier = Modifier.weight(1f),
                                value = speed,
                                onValueChange = { speed = it },
                                valueRange = 1f..100f,
                                onValueChangeFinished = { AllSettings.cryoGradientSpeed.save(speed.toInt()) }
                            )
                            Text(
                                text = stringResource(R.string.cryo_theme_speed_fast),
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoGlassColors.Frost.copy(alpha = 0.8f)
                            )
                            Text(
                                modifier = Modifier.width(44.dp),
                                text = "${speed.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CryoGlassColors.Ice
                            )
                        }
                    }
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
                        .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    text = stringResource(R.string.cryo_theme_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = CryoGlassColors.Frost
                )
            }
        }
            }
    }
}

@Composable
private fun ThemeTile(
    type: ColorThemeType,
    selected: Boolean,
    applied: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val spec = rememberThemeSpec(type, CryoColorAdjust())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(spec.iceSoft.copy(alpha = 0.55f), spec.aurora.copy(alpha = 0.15f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, (if (selected) spec.ice else CryoGlassColors.Ice).copy(alpha = if (selected) 0.9f else 0.2f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(spec.iceSoft, spec.ice, spec.aurora)))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (applied) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = spec.deep
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = themeTypeName(type),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = themeTag(type),
                style = MaterialTheme.typography.labelSmall,
                color = spec.ice,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.width(92.dp),
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        CryoSlider(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = { onChange(it.toInt().toFloat()) },
            valueRange = -100f..100f,
            onValueChangeFinished = onFinished
        )
        Text(
            modifier = Modifier.width(52.dp),
            text = (if (value > 0f) "+" else "") + "${value.toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CryoGlassColors.Ice
        )
    }
}

@Composable
private fun StudioSection(title: String, content: @Composable () -> Unit) {
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

@Composable
private fun ThemeHomePreview(
    modifier: Modifier = Modifier,
    spec: CryoSpec,
    animated: Boolean
) {
    val corner = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(corner)
            .background(spec.deep)
            .border(1.dp, spec.ice.copy(alpha = 0.6f), corner)
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.55f),
            painter = com.russoviller.cryogenic.ui.components.cryoArtPainter(R.drawable.img_cryo_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(spec.deep.copy(alpha = 0.15f), spec.deep.copy(alpha = 0.75f))))
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(22.dp)
                .background(spec.navy.copy(alpha = 0.85f))
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .then(
                            if (index == 0) Modifier.specGradientFill(spec, animated)
                            else Modifier.background(spec.ice.copy(alpha = 0.18f))
                        )
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(spec.navy.copy(alpha = 0.8f))
                    .border(0.5.dp, spec.ice.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PreviewBar(spec.frost, 28.dp)
                Spacer(Modifier.weight(1f))
                PreviewBar(spec.ice, 14.dp)
                PreviewBar(spec.ice, 14.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(spec.navy.copy(alpha = 0.55f), Color.Transparent)))
                    .border(0.5.dp, spec.ice.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Cryogenic",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            brush = Brush.horizontalGradient(listOf(Color.White, spec.ice)),
                            shadow = null
                        )
                    )
                    PreviewBar(spec.frost.copy(alpha = 0.6f), 54.dp)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(58.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(50))
                        .specGradientFill(spec, animated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.cryo_home_play),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, shadow = null),
                        fontWeight = FontWeight.Black,
                        color = spec.deep
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) { index ->
                    PreviewCard(spec = spec) {
                        PreviewBar(spec.ice, 22.dp)
                        Spacer(Modifier.height(3.dp))
                        PreviewBar(spec.frost.copy(alpha = 0.5f), 34.dp)
                        if (index == 1) {
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .border(0.5.dp, spec.ice, RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PreviewCard(
    spec: CryoSpec,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(spec.navy.copy(alpha = 0.75f), spec.deep.copy(alpha = 0.85f))))
            .border(0.5.dp, spec.ice.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(4.dp),
        content = content
    )
}

@Composable
private fun PreviewBar(color: Color, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}
