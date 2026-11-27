package com.russoviller.cryogenic.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/** Fonte das etiquetas: condensada e em negrito (nítida em qualquer fundo). */
val CryoTagFont: FontFamily = FontFamily(
    androidx.compose.ui.text.font.Typeface(Typeface.create("sans-serif-condensed", Typeface.BOLD))
)

/**
 * Etiqueta do Cryogenic (tipo de conteúdo, carregador, plataforma, avisos...):
 * pílula arredondada como os botões, ponto/ícone na cor da etiqueta, texto condensado em
 * maiúsculas, sem sombra (não borra).
 */
@Composable
fun CryoTag(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = CryoGlassColors.Ice,
    icon: Painter? = null,
    iconSize: Dp = 12.dp,
    fontSize: TextUnit = 10.sp,
    marquee: Boolean = false,
    /** Etiqueta apagada (informação secundária). */
    muted: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    val size = if (fontSize.isSpecified) fontSize else 10.sp
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (muted) 0.10f else 0.26f),
                        CryoGlassColors.Deep.copy(alpha = if (muted) 0.55f else 0.82f)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = if (muted) 0.35f else 0.85f), shape)
            .padding(start = 8.dp, end = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        //ponto de cor quando não tem ícone
        if (icon == null) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.5f).dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (muted) 0.5f else 1f))
            )
        }
        icon?.let {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = it,
                contentDescription = null,
                tint = accent
            )
        }
        Text(
            modifier = Modifier
                .then(if (marquee) Modifier.basicMarquee(Int.MAX_VALUE) else Modifier)
                .padding(vertical = 3.dp),
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = CryoTagFont,
                fontWeight = FontWeight.Bold,
                fontSize = size,
                letterSpacing = 0.8.sp,
                color = if (muted) lerp(accent, Color.White, 0.75f).copy(alpha = 0.8f) else lerp(accent, Color.White, 0.6f),
                shadow = null
            ),
            maxLines = 1
        )
    }
}
