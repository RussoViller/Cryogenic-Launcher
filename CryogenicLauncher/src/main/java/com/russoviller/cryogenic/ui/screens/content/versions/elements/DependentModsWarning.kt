package com.russoviller.cryogenic.ui.screens.content.versions.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Caixa de aviso: estes mods param de funcionar se o(s) mod(s) for(em) apagado(s). */
@Composable
fun DependentModsWarning(title: String, names: List<String>) {
    if (names.isEmpty()) return
    val warn = Color(0xFFFFB547)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(warn.copy(alpha = 0.12f))
            .border(1.dp, warn.copy(alpha = 0.8f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "⚠ $title", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = warn)
        names.distinct().sorted().forEach {
            Text(text = "• $it", style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}
