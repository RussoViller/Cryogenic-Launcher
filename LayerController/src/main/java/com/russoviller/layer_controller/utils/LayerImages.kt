package com.russoviller.layer_controller.utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Carregador das imagens (PNG) dos botões, definido pelo launcher.
 * Recebe o nome guardado no botão e devolve a imagem pronta (ou null).
 */
object LayerImages {
    @Volatile
    var loader: ((String) -> ImageBitmap?)? = null

    fun load(name: String?): ImageBitmap? {
        if (name.isNullOrEmpty()) return null
        return runCatching { loader?.invoke(name) }.getOrNull()
    }
}
