package com.russoviller.cryogenic.streamer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russoviller.cryogenic.setting.AllSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Uma sobreposição do modo streamer (página web transparente: meta de doações, alertas, LivePix...).
 *
 * Funciona como uma fonte de navegador do OBS: a página é montada num tamanho fixo
 * ([pageW] x [pageH]) e nunca muda de layout; na tela aparece só o recorte escolhido
 * ([cropL]..[cropR], [cropT]..[cropB], frações da página), ampliado ou reduzido sem distorcer.
 * Posição ([x], [y]) e largura ([w]) são frações da tela; a altura sai da proporção do recorte.
 */
@Serializable
data class StreamOverlay(
    val url: String = "",
    val enabled: Boolean = false,
    val opacity: Int = 100,
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0.3f,
    val h: Float = 0.2f,
    val pageW: Int = 1280,
    val pageH: Int = 720,
    val cropL: Float = 0f,
    val cropT: Float = 0f,
    val cropR: Float = 1f,
    val cropB: Float = 1f
) {
    /** Tamanho do recorte, em pixels da página. */
    val cropWidth: Float get() = (cropR - cropL) * pageW
    val cropHeight: Float get() = (cropB - cropT) * pageH

    /** Altura na tela (fração), para a largura [w] numa tela [screenW] x [screenH]. */
    fun heightOn(screenW: Float, screenH: Float): Float =
        if (screenH <= 0f || cropWidth <= 0f) h else w * screenW * cropHeight / cropWidth / screenH
}

/** Estado das sobreposições no processo do jogo (lido e salvo nos ajustes). */
object StreamOverlayStore {
    const val COUNT = 4
    const val MIN_SIZE = 0.06f
    const val MIN_CROP = 0.04f

    /** Tamanhos de página (como a resolução de uma fonte de navegador do OBS). */
    val pageSizes = listOf(1920 to 1080, 1280 to 720, 800 to 600, 800 to 300, 600 to 150)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Posições iniciais: um canto da tela para cada uma. */
    val defaults = listOf(
        StreamOverlay(x = 0.02f, y = 0.04f, w = 0.30f, h = 0.18f),
        StreamOverlay(x = 0.68f, y = 0.04f, w = 0.30f, h = 0.18f),
        StreamOverlay(x = 0.02f, y = 0.66f, w = 0.30f, h = 0.28f),
        StreamOverlay(x = 0.68f, y = 0.66f, w = 0.30f, h = 0.28f)
    )

    val slots = mutableStateListOf<StreamOverlay>()
    var volume by mutableIntStateOf(100)
        private set

    /**
     * Muda quando o processo das páginas (WebView) morre: as sobreposições são recriadas com ele.
     * Sem tratar isso o Android fecha o jogo junto, sem log nenhum.
     */
    var rendererEpoch by mutableIntStateOf(0)
        private set

    /** Processo das páginas morreu vezes demais: os widgets ficam pausados até reabrir o jogo. */
    var rendererPaused by mutableStateOf(false)
        private set

    private var rendererRestarts = 0
    private var lastRendererGone = 0L

    /** Chamado (na thread principal) quando o processo das páginas morreu. */
    fun onRendererGone(context: android.content.Context) {
        val now = android.os.SystemClock.uptimeMillis()
        //todas as páginas avisam da mesma queda: conta uma vez só
        if (now - lastRendererGone < 1500L) return
        lastRendererGone = now
        if (rendererRestarts >= 2) {
            if (!rendererPaused) {
                rendererPaused = true
                runCatching {
                    android.widget.Toast.makeText(context, context.getString(com.russoviller.cryogenic.R.string.cryo_stream_widgets_paused), android.widget.Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        rendererRestarts++
        //dá um tempo para a memória se recuperar antes de abrir as páginas de novo
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ rendererEpoch++ }, 2500L)
    }

    /** Tela de ajuste de posição aberta. */
    var editing by mutableStateOf(false)

    /** Sobreposição sendo recortada (null = nenhuma). */
    var cropping by mutableStateOf<Int?>(null)

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val saved = runCatching {
            json.decodeFromString<List<StreamOverlay>>(AllSettings.cryoStreamOverlays.getValue())
        }.getOrNull().orEmpty()
        slots.clear()
        for (i in 0 until COUNT) slots.add(saved.getOrNull(i)?.let(::sanitize) ?: defaults[i])
        volume = runCatching { AllSettings.cryoStreamOverlayVolume.getValue() }.getOrDefault(100).coerceIn(0, 100)
    }

    private fun sanitize(o: StreamOverlay): StreamOverlay {
        val w = o.w.coerceIn(MIN_SIZE, 1f)
        val l = o.cropL.coerceIn(0f, 1f - MIN_CROP)
        val t = o.cropT.coerceIn(0f, 1f - MIN_CROP)
        return o.copy(
            opacity = o.opacity.coerceIn(0, 100),
            w = w,
            x = o.x.coerceIn(0f, 1f - w),
            y = o.y.coerceIn(0f, 0.98f),
            pageW = o.pageW.coerceIn(200, 2560),
            pageH = o.pageH.coerceIn(100, 1600),
            cropL = l,
            cropT = t,
            cropR = o.cropR.coerceIn(l + MIN_CROP, 1f),
            cropB = o.cropB.coerceIn(t + MIN_CROP, 1f)
        )
    }

    /** Mantém a sobreposição inteira dentro da tela (a altura depende da tela). */
    fun fitToScreen(o: StreamOverlay, screenW: Float, screenH: Float): StreamOverlay {
        var w = o.w
        var h = o.copy(w = w).heightOn(screenW, screenH)
        if (h > 1f) {
            w *= 1f / h
            h = 1f
        }
        return o.copy(w = w, h = h, x = o.x.coerceIn(0f, 1f - w), y = o.y.coerceIn(0f, (1f - h).coerceAtLeast(0f)))
    }

    fun update(index: Int, save: Boolean = true, change: (StreamOverlay) -> StreamOverlay) {
        ensureLoaded()
        slots[index] = sanitize(change(slots[index]))
        if (save) save()
    }

    fun changeVolume(value: Int) {
        volume = value.coerceIn(0, 100)
        AllSettings.cryoStreamOverlayVolume.save(volume)
    }

    fun resetPositions() {
        for (i in 0 until COUNT) {
            val d = defaults[i]
            slots[i] = slots[i].copy(x = d.x, y = d.y, w = d.w)
        }
        save()
    }

    fun save() {
        AllSettings.cryoStreamOverlays.save(json.encodeToString(slots.toList()))
    }

    /** Link utilizável (com https:// quando faltar), ou null se estiver vazio/inválido. */
    fun normalizeUrl(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        val withScheme = if (s.startsWith("http://", true) || s.startsWith("https://", true)) s else "https://$s"
        return withScheme.takeIf { android.util.Patterns.WEB_URL.matcher(it).find() }
    }
}
