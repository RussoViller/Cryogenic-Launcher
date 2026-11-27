package com.russoviller.cryogenic.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object GlassAccess {
    @Composable
    fun mod(): Modifier = Modifier.cryoGlass(shape = RoundedCornerShape(22.dp), cornerRadius = 22.dp)
}
