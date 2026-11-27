package com.russoviller.cryogenic.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.russoviller.cryogenic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * As artes grandes que vêm no APK (cartão principal, 9:16, fundo padrão, cartões da tela inicial).
 * O painterResource decodifica a imagem de novo na thread principal toda vez que a tela entra
 * (trocar de opção, trocar de área): aqui ela é decodificada uma vez, fora da thread principal,
 * e reaproveitada. O cache é solto quando o Launcher sai da tela (jogo aberto) ou a RAM aperta.
 */
object CryoArtCache {
    private val bitmaps = ConcurrentHashMap<Int, ImageBitmap>()

    /** As que aparecem logo na primeira tela: decodificadas em segundo plano ao abrir o Launcher. */
    private val WARM = intArrayOf(
        R.drawable.img_cryo_hero,
        R.drawable.img_cryo_portrait,
        R.drawable.img_cryo_wallpaper
    )

    fun peek(res: Int): ImageBitmap? = bitmaps[res]

    fun decode(context: Context, res: Int): ImageBitmap =
        bitmaps[res] ?: BitmapFactory.decodeResource(context.resources, res)
            .asImageBitmap()
            .also { bitmaps[res] = it }

    suspend fun load(context: Context, res: Int): ImageBitmap =
        bitmaps[res] ?: withContext(Dispatchers.IO) { decode(context, res) }

    fun prewarm(context: Context) {
        val app = context.applicationContext
        Thread({
            WARM.forEach { res -> runCatching { decode(app, res) } }
        }, "cryo-art-prewarm").apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    fun clear() = bitmaps.clear()
}

private val Transparent = ColorPainter(Color.Transparent)

/** Igual ao painterResource para imagens do APK, mas com cache e decodificação fora da thread principal. */
@Composable
fun cryoArtPainter(res: Int): Painter {
    val context = LocalContext.current
    //O produceState guarda o valor anterior quando a imagem pedida muda (ex.: trocar o atalho de um
    //cartão da tela inicial). Por isso a imagem é sempre conferida contra o recurso pedido agora:
    //se a que está em mãos é de outro recurso, usa a do cache na hora ou carrega a nova.
    val loaded by produceState<Pair<Int, ImageBitmap>?>(CryoArtCache.peek(res)?.let { res to it }, res) {
        if (value?.first != res) {
            value = CryoArtCache.peek(res)?.let { res to it }
                ?: runCatching { res to CryoArtCache.load(context, res) }.getOrNull()
        }
    }
    val bitmap = loaded?.takeIf { it.first == res }?.second ?: CryoArtCache.peek(res)
    return remember(bitmap) { bitmap?.let { BitmapPainter(it) } ?: Transparent }
}
