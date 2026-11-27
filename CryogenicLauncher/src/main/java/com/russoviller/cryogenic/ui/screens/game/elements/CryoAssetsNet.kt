package com.russoviller.cryogenic.ui.screens.game.elements

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.game.download.assets.platform.Platform
import com.russoviller.cryogenic.game.download.assets.platform.PlatformVersion
import com.russoviller.cryogenic.game.download.assets.platform.getVersions
import com.russoviller.cryogenic.game.download.assets.platform.mirroredCurseForgeSource
import com.russoviller.cryogenic.game.download.assets.platform.mirroredPlatformSearcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/*
 * Rede do painel de Texturas/Shaders dentro do jogo.
 *
 * No processo do jogo, uma requisição HTTP interrompida no meio (busca trocada, ícone que saiu da tela,
 * página extra cancelada) fecha o jogo. Por isso aqui nenhuma requisição é cancelada: tudo roda num
 * escopo próprio que nunca é cancelado, e o resultado que não serve mais é só ignorado.
 */

private const val CONNECT_TIMEOUT = 15_000
private const val READ_TIMEOUT = 30_000
private const val MAX_ICON_BYTES = 4 * 1024 * 1024

/** Escopo que nunca é cancelado: as requisições sempre terminam sozinhas. */
private val netScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun open(url: String): HttpURLConnection {
    var current = url
    repeat(5) {
        val conn = URL(current).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "CryogenicLauncher")
        val code = conn.responseCode
        if (code in 300..399) {
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            if (location.isNullOrBlank()) throw IOException("Redirect without location: $current")
            current = URL(URL(current), location).toString()
        } else if (code !in 200..299) {
            conn.disconnect()
            throw IOException("HTTP $code: $current")
        } else {
            return conn
        }
    }
    throw IOException("Too many redirects: $url")
}

/** Ícones dos resultados, baixados sem nunca interromper a conexão. */
object CryoAssetIcons {
    private val cache = LruCache<String, ImageBitmap>(96)
    private val loading = HashSet<String>()
    private val failed = HashSet<String>()
    /** Muda quando um ícone termina de carregar (para recompor quem está mostrando). */
    val ready = mutableStateMapOf<String, Boolean>()

    fun get(url: String): ImageBitmap? = cache.get(url)

    fun isFailed(url: String): Boolean = synchronized(loading) { url in failed }

    fun load(url: String, sizePx: Int) {
        synchronized(loading) {
            if (cache.get(url) != null || url in loading || url in failed) return
            loading.add(url)
        }
        netScope.launch {
            val bitmap = runCatching {
                val bytes = open(url).let { conn ->
                    try {
                        conn.inputStream.use { input ->
                            val out = java.io.ByteArrayOutputStream()
                            val buf = ByteArray(16 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                if (out.size() > MAX_ICON_BYTES) throw IOException("Icon too large")
                            }
                            out.toByteArray()
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                val target = sizePx.coerceAtLeast(1)
                while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.asImageBitmap()
            }.getOrNull()
            synchronized(loading) {
                loading.remove(url)
                if (bitmap != null) cache.put(url, bitmap) else failed.add(url)
            }
            ready[url] = bitmap != null
        }
    }
}

@Composable
internal fun CryoAssetIcon(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val key = url?.takeIf { it.isNotBlank() }
    if (key != null) {
        LaunchedEffect(key) { CryoAssetIcons.load(key, px) }
    }
    //lê o estado para recompor quando o ícone ficar pronto
    val done = key?.let { CryoAssetIcons.ready[it] }
    val bitmap = key?.let { CryoAssetIcons.get(it) }
    Box(modifier = modifier.size(size).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
            key == null || done == false -> Image(painter = painterResource(R.drawable.ic_unknown_icon), contentDescription = null, modifier = Modifier.size(size))
        }
    }
}

/**
 * Lista de versões de um projeto. No CurseForge as páginas são pedidas uma por vez
 * (a busca paralela do launcher cancela páginas extras no meio, o que fecha o jogo).
 */
internal suspend fun fetchVersionsSafely(projectId: String, platform: Platform): List<PlatformVersion> = when (platform) {
    Platform.MODRINTH -> getVersions(projectId, platform)
    Platform.CURSEFORGE -> {
        val pageSize = 50
        val all = ArrayList<PlatformVersion>()
        var index = 0
        while (true) {
            val page = mirroredPlatformSearcher(searchers = mirroredCurseForgeSource()) { searcher ->
                searcher.getVersions(projectID = projectId, index = index, pageSize = pageSize)
            }
            all.addAll(page.data)
            if (page.data.size < pageSize || all.size >= 2000) break
            index += pageSize
        }
        all
    }
}

/**
 * Baixa um arquivo em um único fluxo, sem dividir em partes e sem cortar a conexão no meio.
 * Confere o SHA-1 quando a plataforma informa.
 */
internal fun downloadSafely(urls: List<String>, output: File, sha1: String?, onBytes: (Long) -> Unit) {
    var lastError: Exception? = null
    for (url in urls) {
        try {
            val digest = MessageDigest.getInstance("SHA-1")
            val conn = open(url)
            try {
                conn.inputStream.use { input ->
                    output.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            onBytes(n.toLong())
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            if (!sha1.isNullOrBlank()) {
                val got = digest.digest().joinToString("") { "%02x".format(it) }
                if (!got.equals(sha1, ignoreCase = true)) throw IOException("SHA-1 mismatch")
            }
            return
        } catch (e: Exception) {
            lastError = e
            output.delete()
        }
    }
    throw lastError ?: IOException("No download source")
}
