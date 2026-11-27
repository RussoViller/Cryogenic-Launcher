package com.russoviller.cryogenic.ui.screens.content.download.game

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient

val LoadoutSideWidth: Dp = 232.dp

@Composable
fun LoadoutFrame(
    modifier: Modifier = Modifier,
    side: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .cryoGlass(shape = cryoChamferShape(26.dp), cornerRadius = 26.dp)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val panel = cryoChamferShape(18.dp)
        Column(
            modifier = Modifier
                .width(LoadoutSideWidth)
                .fillMaxHeight()
                .clip(panel)
                .background(
                    Brush.verticalGradient(
                        listOf(CryoGlassColors.Ice.copy(alpha = 0.22f), CryoGlassColors.Deep.copy(alpha = 0.92f))
                    )
                )
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.6f), panel)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            //o conteúdo rola quando não cabe (fonte grande, tela baixa); o rodapé (botão) fica sempre visível
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val minHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = minHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = side
                )
            }
            footer?.invoke(this)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

@Composable
fun LoadoutKicker(text: String, color: Color = CryoGlassColors.Ice) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
        color = color,
        maxLines = 2
    )
}

@Composable
fun LoadoutTitle(text: String, maxLines: Int = 1, large: Boolean = true) {
    Text(
        text = text,
        style = (if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge).copy(
            fontWeight = FontWeight.Black,
            brush = Brush.verticalGradient(listOf(Color.White, CryoGlassColors.Ice))
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun LoadoutArt(gameVersion: String, modifier: Modifier = Modifier) {
    val alpha = categoryImageAlpha()
    val shape = cryoChamferShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CryoGlassColors.Navy, CryoGlassColors.Deep)))
            .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.5f), shape)
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            painter = painterResource(minecraftArtFor(gameVersion)),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun LoadoutPrimaryButton(
    text: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .cryoBackground(cryoGradient())
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Deep
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            fontWeight = FontWeight.Black,
            color = CryoGlassColors.Deep,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun LoadoutDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.6f), Color.Transparent)))
    )
}
