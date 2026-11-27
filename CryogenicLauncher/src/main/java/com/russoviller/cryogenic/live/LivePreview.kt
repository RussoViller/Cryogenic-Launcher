package com.russoviller.cryogenic.live

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView

/**
 * Prévia da transmissão na tela do launcher: a tela (TextureView) entrega a sua superfície e o
 * [LiveService] (mesmo processo) desenha nela o mesmo quadro enviado para a live, com a tela
 * bloqueada/PNG incluída. Tudo na thread principal.
 */
internal object LivePreview {
    private class Target(val surface: Surface, var width: Int, var height: Int)

    /** Superfícies abertas; a de cima (a última, ex.: tela cheia) é a que recebe a prévia. */
    private val targets = ArrayList<Target>()

    /** Definido pelo serviço enquanto a live roda: (superfície ou null, largura, altura). */
    var listener: ((Surface?, Int, Int) -> Unit)? = null
        set(value) {
            field = value
            targets.lastOrNull()?.let { value?.invoke(it.surface, it.width, it.height) }
        }

    fun attach(s: Surface, w: Int, h: Int) {
        targets.removeAll { it.surface === s }
        targets.add(Target(s, w, h))
        listener?.invoke(s, w, h)
    }

    /** Mudou de tamanho: só reaplica se for a que está recebendo a prévia. */
    fun resize(s: Surface, w: Int, h: Int) {
        val t = targets.firstOrNull { it.surface === s } ?: return
        t.width = w
        t.height = h
        if (targets.lastOrNull() === t) listener?.invoke(s, w, h)
    }

    fun detach(s: Surface) {
        val wasTop = targets.lastOrNull()?.surface === s
        targets.removeAll { it.surface === s }
        if (!wasTop) return
        // a tela cheia fechou: a prévia volta para a telinha
        val top = targets.lastOrNull()
        listener?.invoke(top?.surface, top?.width ?: 0, top?.height ?: 0)
    }

    /** TextureView que se liga sozinha à prévia. */
    fun createView(context: android.content.Context): TextureView = TextureView(context).apply {
        isOpaque = false
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            private var current: Surface? = null

            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                val s = Surface(st)
                current = s
                attach(s, w, h)
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                current?.let { resize(it, w, h) }
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                val s = current
                current = null
                s?.let { detach(it) }
                // solta depois: a thread de vídeo pode estar terminando um quadro nesta superfície
                Handler(Looper.getMainLooper()).postDelayed({
                    runCatching { s?.release() }
                    runCatching { st.release() }
                }, 500L)
                return false
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }
}
