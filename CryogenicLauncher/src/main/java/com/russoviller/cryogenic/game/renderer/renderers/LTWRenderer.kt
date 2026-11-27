package com.russoviller.cryogenic.game.renderer.renderers

import com.russoviller.cryogenic.game.renderer.RendererInterface

/**
 * LTW (Large Thin Wrapper): traduz OpenGL core para OpenGL ES 3.
 * Só funciona com o OpenGL moderno do Minecraft (1.17+).
 * Fonte: https://github.com/alexytomi/LTW (LGPL-3.0), compilado do código-fonte.
 */
object LTWRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3_ltw"

    override fun getUniqueIdentifier(): String = "e7621314-1452-437d-8b24-9be515d1cc34"

    override fun getRendererName(): String = "LTW"

    override fun getMinMCVersion(): String = "1.17"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf("LIBGL_ES" to "3")
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libltw.so"

    //o LTW intercepta a criação do contexto EGL (precisa de contexto OpenGL ES 3)
    override fun getRendererEGL(): String = "libltw.so"
}
