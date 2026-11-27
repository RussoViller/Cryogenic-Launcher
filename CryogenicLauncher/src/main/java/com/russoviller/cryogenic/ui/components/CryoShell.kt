package com.russoviller.cryogenic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.russoviller.cryogenic.R

class CryoNavEntry(
    val icon: Painter,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun CryoSidebar(
    entries: List<CryoNavEntry>,
    bottomEntries: List<CryoNavEntry>,
    footer: String,
    modifier: Modifier = Modifier,
    /** Segunda página de botões, aberta pela seta abaixo do último item. */
    moreEntries: List<CryoNavEntry> = emptyList(),
    moreLabel: String = "",
    backLabel: String = ""
) {
    Column(
        modifier = modifier
            .width(172.dp)
            .fillMaxHeight()
            .padding(start = 10.dp, top = 6.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CryoBrandHeader(version = footer)
        //Liquid Glass: uma gota desliza até o item selecionado
        val blob = if (cryoLiquid) remember { LiquidBlob(vertical = true) } else null
        blob?.Animate()
        val blobColors = CryoTheme.spec.gradient ?: listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora)
        val blobGlow = CryoGlassColors.Ice
        val moreSelected = moreEntries.any { it.selected }
        //abre na página onde está a tela atual
        var page by remember { mutableIntStateOf(if (moreSelected) 1 else 0) }
        LaunchedEffect(moreSelected) { if (moreSelected) page = 1 }
        val pageEntries = if (page == 0 || moreEntries.isEmpty()) {
            if (moreEntries.isEmpty()) entries
            else entries + CryoNavEntry(painterResource(R.drawable.ic_keyboard_arrow_right), moreLabel, moreSelected) { page = 1 }
        } else {
            listOf(CryoNavEntry(painterResource(R.drawable.ic_keyboard_arrow_left), backLabel, false) { page = 0 }) + moreEntries
        }
        //entrada da página: só transforma a camada (não mexe no layout nem na posição da gota)
        val enter = remember(page) { Animatable(if (moreEntries.isEmpty()) 1f else 0f) }
        LaunchedEffect(page) { enter.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
        val direction = if (page == 0) -1f else 1f
        val pageLayer = Modifier.graphicsLayer {
            val t = enter.value
            alpha = t
            translationX = (1f - t) * 22.dp.toPx() * direction
        }
        //sem item selecionado nesta página, a gota some (não aponta para um botão de outra página)
        val showBlob = blob != null && (pageEntries.any { it.selected } || bottomEntries.any { it.selected })
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .cryoGlass(shape = cryoGlassShape(20.dp), cornerRadius = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(6.dp)
                .then(if (showBlob && blob != null) Modifier.liquidBlob(blob, blobColors, blobGlow) else Modifier),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            pageEntries.forEach { CryoSidebarItem(it, blob, pageLayer) }
            if (bottomEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, CryoGlassColors.Ice.copy(alpha = 0.45f))))
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(CryoGlassColors.Ice)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.45f), Color.Transparent)))
                    )
                }
                bottomEntries.forEach { CryoSidebarItem(it, blob) }
            }
        }
    }
}

/**
 * Cabeçalho da barra lateral: logo animada, nome do launcher e selo da versão.
 */
@Composable
private fun CryoBrandHeader(version: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .cryoGlass(shape = cryoGlassShape(25.dp), glow = true, cornerRadius = 25.dp)
            .padding(start = 5.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .drawBehind {
                    drawCircle(
                        brush = safeRadialGradient(
                            colors = listOf(
                                CryoGlassColors.Ice.copy(alpha = 0.45f),
                                CryoGlassColors.Ice.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.62f
                        ),
                        radius = safeGradientRadius(size.minDimension * 0.62f)
                    )
                }
                .padding(1.dp)
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            CryoGlassColors.Ice.copy(alpha = 0.75f),
                            CryoGlassColors.Aurora.copy(alpha = 0.55f)
                        )
                    ),
                    CircleShape
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            CryoAnimatedLogo(modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Cryogenic",
                style = MaterialTheme.typography.titleSmall.copy(
                    brush = Brush.verticalGradient(listOf(Color.White, CryoGlassColors.IceSoft)),
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.4.sp,
                    shadow = Shadow(color = CryoGlassColors.Ice.copy(alpha = 0.55f), blurRadius = 10f)
                ),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = "LAUNCHER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                        letterSpacing = 1.6.sp,
                        shadow = null
                    ),
                    fontWeight = FontWeight.Bold,
                    color = CryoGlassColors.Frost.copy(alpha = 0.8f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                CryoVersionBadge(version)
            }
        }
    }
}

/**
 * Selo da versão: fonte monoespaçada, alto contraste e borda de gelo para leitura fácil.
 */
@Composable
private fun CryoVersionBadge(version: String) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CryoGlassColors.Ice.copy(alpha = 0.30f),
                        CryoGlassColors.Deep.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, cryoBorderBrush(1.2f), shape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = version,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                letterSpacing = 0.3.sp,
                shadow = Shadow(color = CryoGlassColors.Ice.copy(alpha = 0.8f), blurRadius = 6f)
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun CryoSidebarItem(entry: CryoNavEntry, blob: LiquidBlob? = null, layer: Modifier = Modifier) {
    val progress by animateFloatAsState(if (entry.selected) 1f else 0f, label = "sidebar_item")
    val shape = CircleShape
    val fillFactor = cryoFillFactor().coerceAtMost(1f)
    val interaction = remember { MutableInteractionSource() }
    val liquid = blob != null
    Row(
        modifier = Modifier
            .liquidBlobTarget(blob, entry.selected)
            .then(layer)
            .fillMaxWidth()
            .height(38.dp)
            .cryoLiquidPress(interaction)
            .then(if (entry.selected && !liquid) Modifier.cryoGlow(19.dp, selected = true, strength = 0.9f) else Modifier)
            .clip(shape)
            .then(
                if (liquid) {
                    //vidro claro; o selecionado fica transparente (a gota aparece por baixo)
                    if (entry.selected) Modifier
                    else Modifier
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.03f))))
                        .border(1.dp, cryoLiquidRim(0.4f), shape)
                } else Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CryoGlassColors.Ice.copy(alpha = 0.08f + 0.10f * fillFactor),
                                CryoGlassColors.Navy.copy(alpha = 0.30f + 0.30f * fillFactor)
                            )
                        )
                    )
                    .then(if (progress > 0f) Modifier.cryoBackground(cryoGradient(), alpha = progress) else Modifier)
                    .border(
                        if (entry.selected) 1.5.dp else 1.dp,
                        if (entry.selected) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.15f))) else cryoBorderBrush(),
                        shape
                    )
            )
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = entry.onClick)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (entry.selected) Brush.linearGradient(listOf(CryoGlassColors.Deep.copy(alpha = 0.85f), CryoGlassColors.Navy.copy(alpha = 0.85f)))
                    else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.22f), CryoGlassColors.Aurora.copy(alpha = 0.10f)))
                )
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (entry.selected) 0.9f else 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = entry.icon,
                contentDescription = null,
                tint = if (entry.selected) CryoGlassColors.Ice else CryoGlassColors.Frost
            )
        }
        Text(
            text = entry.label,
            maxLines = 1,
            style = if (entry.selected) MaterialTheme.typography.labelLarge.copy(shadow = null) else MaterialTheme.typography.labelLarge,
            fontWeight = if (entry.selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (entry.selected) CryoGlassColors.Deep else CryoGlassColors.Frost
        )
    }
}

class CryoTab(
    val icon: @Composable () -> Unit,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    /** Contador opcional ao lado do nome (ex.: quantidade de mods na pasta). */
    val badge: String? = null
)

val CryoShardShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

@Composable
fun CryoTabStrip(
    tabs: List<CryoTab>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val blob = if (cryoLiquid) remember { LiquidBlob(vertical = false) } else null
        blob?.Animate()
        val blobColors = CryoTheme.spec.gradient ?: listOf(CryoGlassColors.IceSoft, CryoGlassColors.Ice, CryoGlassColors.Aurora)
        val blobGlow = CryoGlassColors.Ice
        Row(
            modifier = Modifier
                .cryoGlass(shape = CircleShape, glow = false, cornerRadius = 24.dp)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
                .then(if (blob != null) Modifier.liquidBlob(blob, blobColors, blobGlow) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab -> CryoDockTab(index, tab, blob) }
        }
    }
}

@Composable
private fun CryoDockTab(index: Int, tab: CryoTab, blob: LiquidBlob? = null) {
    val fill by animateFloatAsState(if (tab.selected) 1f else 0f, label = "tab_fill")
    val interaction = remember { MutableInteractionSource() }
    val liquid = blob != null
    Row(
        modifier = Modifier
            .liquidBlobTarget(blob, tab.selected)
            .height(36.dp)
            .cryoLiquidPress(interaction)
            .then(if (tab.selected && !liquid) Modifier.cryoGlow(18.dp, selected = true, strength = 0.8f) else Modifier)
            .clip(CircleShape)
            .then(if (fill > 0f && !liquid) Modifier.cryoBackground(cryoGradient(), alpha = fill) else Modifier)
            .border(
                1.dp,
                if (tab.selected && !liquid) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.12f)))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                CircleShape
            )
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = tab.onClick)
            .padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (tab.selected) CryoGlassColors.Deep.copy(alpha = 0.85f)
                    else CryoGlassColors.Ice.copy(alpha = 0.10f)
                )
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (tab.selected) 0.9f else 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (tab.selected) CryoGlassColors.Ice else CryoGlassColors.Frost.copy(alpha = 0.85f)
            ) {
                Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    tab.icon()
                }
            }
        }
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = "%02d".format(index + 1),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    shadow = null
                ),
                color = if (tab.selected) CryoGlassColors.Deep.copy(alpha = 0.7f) else CryoGlassColors.Ice.copy(alpha = 0.7f),
                maxLines = 1
            )
            Text(
                text = tab.label,
                maxLines = 1,
                style = if (tab.selected) MaterialTheme.typography.labelLarge.copy(shadow = null, lineHeight = 15.sp) else MaterialTheme.typography.labelLarge.copy(lineHeight = 15.sp),
                fontWeight = if (tab.selected) FontWeight.Black else FontWeight.SemiBold,
                color = if (tab.selected) CryoGlassColors.Deep else CryoGlassColors.Frost
            )
        }
        tab.badge?.let { badge ->
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (tab.selected) CryoGlassColors.Deep.copy(alpha = 0.85f)
                        else CryoGlassColors.Ice.copy(alpha = 0.16f)
                    )
                    .padding(horizontal = 7.dp, vertical = 1.dp),
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    shadow = null
                ),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Ice,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CryoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(CryoGlassColors.Ice, CryoGlassColors.Aurora)))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        action?.invoke()
    }
}
