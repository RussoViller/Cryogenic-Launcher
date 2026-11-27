package com.russoviller.cryogenic.streamer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Modo streamer: sobreposições (páginas web transparentes) desenhadas por cima do jogo e por
 * baixo dos botões do HUD. Elas não recebem toques: tudo continua indo para o jogo/controles.
 *
 * Como uma fonte de navegador do OBS, cada página é montada num tamanho fixo e nunca muda de
 * layout ao mover/redimensionar: a moldura só amplia/reduz a imagem e mostra o recorte escolhido.
 */

/** Script que controla o volume de tudo que toca na página (áudio, vídeo e Web Audio). */
private const val VOLUME_SCRIPT = """
(function(){
  if (window.__cryoVol) return;
  window.__cryoVol = { v: 1 };
  var P = HTMLMediaElement.prototype;
  var d = Object.getOwnPropertyDescriptor(P, 'volume');
  function realSet(m, x){ try { d.set.call(m, Math.max(0, Math.min(1, x))); } catch(e){} }
  Object.defineProperty(P, 'volume', {
    configurable: true,
    get: function(){ return this.__pv === undefined ? 1 : this.__pv; },
    set: function(x){ this.__pv = x; realSet(this, x * window.__cryoVol.v); }
  });
  var op = P.play;
  P.play = function(){ realSet(this, (this.__pv === undefined ? 1 : this.__pv) * window.__cryoVol.v); return op.apply(this, arguments); };
  window.__cryoGains = [];
  if (window.AudioNode && window.AudioDestinationNode) {
    var oc = AudioNode.prototype.connect;
    AudioNode.prototype.connect = function(dest){
      if (dest instanceof AudioDestinationNode) {
        var ctx = dest.context;
        if (!ctx.__cryoGain) {
          var g = ctx.createGain();
          g.gain.value = window.__cryoVol.v;
          oc.call(g, dest);
          ctx.__cryoGain = g;
          window.__cryoGains.push(g);
        }
        if (this !== ctx.__cryoGain) {
          var args = Array.prototype.slice.call(arguments);
          args[0] = ctx.__cryoGain;
          return oc.apply(this, args);
        }
      }
      return oc.apply(this, arguments);
    };
  }
  window.__cryoSetVolume = function(v){
    window.__cryoVol.v = v;
    document.querySelectorAll('audio,video').forEach(function(m){ realSet(m, (m.__pv === undefined ? 1 : m.__pv) * v); });
    window.__cryoGains.forEach(function(g){ try { g.gain.value = v; } catch(e){} });
  };
})();
"""

/** Fixa a largura de layout da página (como a largura da fonte de navegador no OBS). */
private fun viewportScript(width: Int) = """
(function(){
  function fix(){
    var m = document.querySelector('meta[name=viewport]');
    if (!m) { m = document.createElement('meta'); m.name = 'viewport'; (document.head || document.documentElement).appendChild(m); }
    var c = 'width=$width, user-scalable=no';
    if (m.getAttribute('content') !== c) m.setAttribute('content', c);
  }
  fix();
  document.addEventListener('DOMContentLoaded', fix);
})();
"""

private var webViewProcessReady = false

/** O WebView do processo do jogo precisa de uma pasta própria (o Launcher usa outra). */
internal fun prepareWebViewProcess() {
    if (webViewProcessReady) return
    webViewProcessReady = true
    if (Build.VERSION.SDK_INT >= 28) {
        runCatching { WebView.setDataDirectorySuffix("game_overlays") }
    }
}

/**
 * Moldura que ignora toques (eles passam direto para o jogo e os controles).
 * A página fica num WebView de tamanho fixo [pageW] x [pageH] px; a moldura mostra só o recorte,
 * ampliando/reduzindo por transformação (sem refazer o layout da página).
 */
@SuppressLint("ViewConstructor")
private class OverlayFrame(context: Context, private val pageW: Int, private val pageH: Int) : FrameLayout(context) {
    private var lastVolume = -1f
    /** O processo da página morreu: esta WebView não serve mais (só pode ser destruída). */
    private var gone = false
    private val scripts = VOLUME_SCRIPT + viewportScript(pageW)

    val web: WebView = WebView(context).apply {
        setBackgroundColor(AndroidColor.TRANSPARENT)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isFocusable = false
        isFocusableInTouchMode = false
        isClickable = false
        isLongClickable = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        pivotX = 0f
        pivotY = 0f
        @SuppressLint("SetJavaScriptEnabled")
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching { WebViewCompat.addDocumentStartJavaScript(this, scripts, setOf("*")) }
        }
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                view.evaluateJavascript(scripts, null)
                applyVolume()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (gone) return
                view.evaluateJavascript(scripts, null)
                applyVolume()
            }

            /*
             * O processo que desenha as páginas morreu (quase sempre falta de memória).
             * Sem este retorno "true" o Android fecha o jogo inteiro na hora, sem log.
             * Aqui a página é descartada e o widget é recriado depois de alguns segundos.
             */
            override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                gone = true
                runCatching {
                    removeView(view)
                    view.destroy()
                }
                StreamOverlayStore.onRendererGone(view.context)
                return true
            }
        }
    }

    init {
        // tamanho fixo, mesmo maior que a moldura (o excesso é cortado)
        addView(web, LayoutParams(pageW, pageH))
        clipChildren = true
        clipToPadding = true
        isClickable = false
        isFocusable = false
    }

    /** Mostra o recorte que começa em ([l], [t]) e vai até [r] da página, ocupando [frameWidth] px. */
    fun applyCrop(l: Float, t: Float, r: Float, frameWidth: Float) {
        if (gone) return
        val s = frameWidth / ((r - l) * pageW).coerceAtLeast(1f)
        if (web.scaleX != s) {
            web.scaleX = s
            web.scaleY = s
        }
        web.translationX = -l * pageW * s
        web.translationY = -t * pageH * s
    }

    fun setVolume(volume: Float) {
        if (volume == lastVolume || gone) return
        lastVolume = volume
        applyVolume()
    }

    private fun applyVolume() {
        if (lastVolume < 0f || gone) return
        web.evaluateJavascript("window.__cryoSetVolume && window.__cryoSetVolume($lastVolume);", null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false

    fun release() {
        if (gone) return
        gone = true
        runCatching {
            web.stopLoading()
            web.loadUrl("about:blank")
            removeView(web)
            web.destroy()
        }
    }
}

/** Uma página de sobreposição numa moldura de [widthPx] de largura, mostrando o recorte. */
@Composable
private fun OverlayView(
    modifier: Modifier,
    url: String,
    slot: StreamOverlay,
    widthPx: Float,
    volume: Float,
    alpha: Float,
    pauseWithLifecycle: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    key(url, slot.pageW, slot.pageH, StreamOverlayStore.rendererEpoch) {
        val holder = remember { arrayOfNulls<OverlayFrame>(1) }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                OverlayFrame(context, slot.pageW, slot.pageH).also {
                    holder[0] = it
                    it.setVolume(volume)
                    it.web.loadUrl(url)
                }
            },
            update = { frame ->
                frame.alpha = alpha
                frame.setVolume(volume)
                frame.applyCrop(slot.cropL, slot.cropT, slot.cropR, widthPx)
            },
            onRelease = { it.release() }
        )
        if (pauseWithLifecycle) {
            // jogo em segundo plano: a página para junto
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> runCatching { holder[0]?.web?.onPause() }
                        Lifecycle.Event.ON_RESUME -> runCatching { holder[0]?.web?.onResume() }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
        }
    }
}

/** As sobreposições ativas, cada uma no seu lugar. Fica entre o jogo e os controles. */
@Composable
fun StreamOverlayLayer(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { StreamOverlayStore.ensureLoaded() }
    val slots = StreamOverlayStore.slots
    if (slots.none { it.enabled && StreamOverlayStore.normalizeUrl(it.url) != null }) return
    //o processo das páginas caiu várias vezes: widgets pausados para proteger o jogo
    if (StreamOverlayStore.rendererPaused) return
    prepareWebViewProcess()
    val volume = StreamOverlayStore.volume / 100f
    // durante o recorte, a página aparece no editor (o som fica só com a sobreposição da tela)
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        slots.forEachIndexed { index, raw ->
            val url = StreamOverlayStore.normalizeUrl(raw.url)
            if (raw.enabled && url != null) {
                val slot = StreamOverlayStore.fitToScreen(raw, sw, sh)
                val wPx = slot.w * sw
                val hPx = slot.h * sh
                key(index) {
                    OverlayView(
                        modifier = Modifier
                            .offset { IntOffset((slot.x * sw).roundToInt(), (slot.y * sh).roundToInt()) }
                            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() }),
                        url = url,
                        slot = slot,
                        widthPx = wPx,
                        volume = volume,
                        alpha = slot.opacity / 100f
                    )
                }
            }
        }
    }
}

// =====================================================================
// Ajuste de posição e recorte
// =====================================================================

/**
 * Ajuste: molduras arrastáveis por cima de tudo (inclusive do HUD).
 * Arrastar move; o canto de baixo à direita muda o tamanho (sem distorcer); ✂ abre o recorte.
 */
@Composable
fun StreamOverlayEditor(modifier: Modifier = Modifier) {
    if (!StreamOverlayStore.editing) return
    StreamOverlayStore.ensureLoaded()
    val cropping = StreamOverlayStore.cropping
    if (cropping != null) {
        OverlayCropEditor(modifier = modifier, index = cropping)
        return
    }
    val ice = CryoGlassColors.Ice
    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(indication = null, interactionSource = null) {}
    ) {
        val density = LocalDensity.current
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        StreamOverlayStore.slots.forEachIndexed { index, raw ->
            if (!raw.enabled) return@forEachIndexed
            val slot = StreamOverlayStore.fitToScreen(raw, sw, sh)
            Box(
                modifier = Modifier
                    .offset { IntOffset((slot.x * sw).roundToInt(), (slot.y * sh).roundToInt()) }
                    .size(with(density) { (slot.w * sw).toDp() }, with(density) { (slot.h * sh).toDp() })
                    .border(2.dp, ice, RoundedCornerShape(6.dp))
                    .pointerInput(index, sw, sh) {
                        detectDragGestures(onDragEnd = { StreamOverlayStore.save() }) { change, drag ->
                            change.consume()
                            StreamOverlayStore.update(index, save = false) {
                                StreamOverlayStore.fitToScreen(it.copy(x = it.x + drag.x / sw, y = it.y + drag.y / sh), sw, sh)
                            }
                        }
                    }
            ) {
                // número e recortar (canto de cima)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CryoGlassColors.Deep.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        text = "${index + 1}",
                        color = ice,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ice)
                            .clickable { StreamOverlayStore.cropping = index }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = "✂ " + stringResource(R.string.cryo_stream_crop),
                        color = CryoGlassColors.Deep,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                // alça de tamanho (mantém a proporção do recorte)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp))
                        .background(ice)
                        .pointerInput(index, sw, sh) {
                            detectDragGestures(onDragEnd = { StreamOverlayStore.save() }) { change, drag ->
                                change.consume()
                                StreamOverlayStore.update(index, save = false) {
                                    // o maior movimento (horizontal ou vertical) decide quanto cresce/encolhe
                                    val byX = drag.x / sw
                                    val byY = drag.y / sw * (it.cropWidth / it.cropHeight)
                                    val grow = if (abs(byX) >= abs(byY)) byX else byY
                                    StreamOverlayStore.fitToScreen(
                                        it.copy(w = (it.w + grow).coerceIn(StreamOverlayStore.MIN_SIZE, 1f - it.x)),
                                        sw, sh
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⤡", color = CryoGlassColors.Deep, fontWeight = FontWeight.Black)
                }
            }
        }

        EditorBar(
            modifier = Modifier.align(Alignment.TopCenter),
            hint = stringResource(R.string.cryo_stream_edit_hint),
            secondary = stringResource(R.string.cryo_stream_reset),
            onSecondary = { StreamOverlayStore.resetPositions() },
            onDone = {
                StreamOverlayStore.save()
                StreamOverlayStore.editing = false
            }
        )
    }
}

@Composable
private fun EditorBar(
    modifier: Modifier,
    hint: String,
    secondary: String?,
    onSecondary: () -> Unit,
    onDone: () -> Unit
) {
    val ice = CryoGlassColors.Ice
    Row(
        modifier = modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(CryoGlassColors.Deep.copy(alpha = 0.94f))
            .border(1.dp, ice.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = hint,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
        if (secondary != null) {
            Text(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, ice.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onSecondary)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                text = secondary,
                color = ice,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
        Text(
            modifier = Modifier
                .clip(CircleShape)
                .background(ice)
                .clickable(onClick = onDone)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            text = stringResource(R.string.cryo_stream_done),
            color = CryoGlassColors.Deep,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

/** Partes do recorte que podem ser arrastadas. */
private enum class CropHandle { MOVE, L, T, R, B, LT, RT, LB, RB }

/**
 * Recorte: a página inteira aparece no meio da tela (sem som) com um retângulo por cima.
 * Arrastar as bordas/cantos ajusta o recorte; arrastar o meio move o recorte.
 * O tamanho da página (como a resolução no OBS) fica logo abaixo da barra.
 */
@Composable
private fun OverlayCropEditor(modifier: Modifier, index: Int) {
    val slot = StreamOverlayStore.slots.getOrNull(index)
    if (slot == null) {
        StreamOverlayStore.cropping = null
        return
    }
    val url = StreamOverlayStore.normalizeUrl(slot.url)
    val ice = CryoGlassColors.Ice
    val current by rememberUpdatedState(slot)
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(indication = null, interactionSource = null) {}
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            EditorBar(
                modifier = Modifier,
                hint = stringResource(R.string.cryo_stream_crop_hint),
                secondary = stringResource(R.string.cryo_stream_crop_all),
                onSecondary = {
                    StreamOverlayStore.update(index) { it.copy(cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f) }
                },
                onDone = {
                    StreamOverlayStore.save()
                    StreamOverlayStore.cropping = null
                }
            )
        }
        // tamanho da página
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.cryo_stream_page_size),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
            StreamOverlayStore.pageSizes.forEach { (pw, ph) ->
                val active = pw == slot.pageW && ph == slot.pageH
                Text(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) ice else Color.Transparent)
                        .border(1.dp, ice.copy(alpha = 0.6f), CircleShape)
                        .clickable { StreamOverlayStore.update(index) { it.copy(pageW = pw, pageH = ph) } }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    text = "${pw}×${ph}",
                    color = if (active) CryoGlassColors.Deep else ice,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val availW = constraints.maxWidth.toFloat()
            val availH = constraints.maxHeight.toFloat()
            val scale = min(availW / slot.pageW, availH / slot.pageH)
            val boxW = slot.pageW * scale
            val boxH = slot.pageH * scale
            Box(
                modifier = Modifier.size(with(density) { boxW.toDp() }, with(density) { boxH.toDp() })
            ) {
                // xadrez discreto para ver o que é transparente
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cell = 16.dp.toPx()
                    var yy = 0f
                    var row = 0
                    while (yy < size.height) {
                        var xx = if (row % 2 == 0) 0f else cell
                        while (xx < size.width) {
                            drawRect(Color.White.copy(alpha = 0.06f), Offset(xx, yy), Size(cell, cell))
                            xx += cell * 2
                        }
                        yy += cell
                        row++
                    }
                }
                if (url != null) {
                    OverlayView(
                        modifier = Modifier.fillMaxSize(),
                        url = url,
                        slot = slot.copy(cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f),
                        widthPx = boxW,
                        volume = 0f,
                        alpha = 1f,
                        pauseWithLifecycle = false
                    )
                }
                // escurece fora do recorte e desenha o retângulo com alças
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(index, boxW, boxH) {
                            var handle = CropHandle.MOVE
                            val grab = 26.dp.toPx()
                            detectDragGestures(
                                onDragStart = { p ->
                                    val s = current
                                    val l = s.cropL * boxW
                                    val t = s.cropT * boxH
                                    val r = s.cropR * boxW
                                    val b = s.cropB * boxH
                                    val nearL = abs(p.x - l) < grab
                                    val nearR = abs(p.x - r) < grab
                                    val nearT = abs(p.y - t) < grab
                                    val nearB = abs(p.y - b) < grab
                                    handle = when {
                                        nearL && nearT -> CropHandle.LT
                                        nearR && nearT -> CropHandle.RT
                                        nearL && nearB -> CropHandle.LB
                                        nearR && nearB -> CropHandle.RB
                                        nearL && p.y in t..b -> CropHandle.L
                                        nearR && p.y in t..b -> CropHandle.R
                                        nearT && p.x in l..r -> CropHandle.T
                                        nearB && p.x in l..r -> CropHandle.B
                                        else -> CropHandle.MOVE
                                    }
                                },
                                onDragEnd = { StreamOverlayStore.save() }
                            ) { change, drag ->
                                change.consume()
                                val dx = drag.x / boxW
                                val dy = drag.y / boxH
                                val m = StreamOverlayStore.MIN_CROP
                                StreamOverlayStore.update(index, save = false) { s ->
                                    var l = s.cropL
                                    var t = s.cropT
                                    var r = s.cropR
                                    var b = s.cropB
                                    if (handle == CropHandle.MOVE) {
                                        val w = r - l
                                        val h = b - t
                                        l = (l + dx).coerceIn(0f, 1f - w); r = l + w
                                        t = (t + dy).coerceIn(0f, 1f - h); b = t + h
                                    } else {
                                        if (handle == CropHandle.L || handle == CropHandle.LT || handle == CropHandle.LB) l = (l + dx).coerceIn(0f, r - m)
                                        if (handle == CropHandle.R || handle == CropHandle.RT || handle == CropHandle.RB) r = (r + dx).coerceIn(l + m, 1f)
                                        if (handle == CropHandle.T || handle == CropHandle.LT || handle == CropHandle.RT) t = (t + dy).coerceIn(0f, b - m)
                                        if (handle == CropHandle.B || handle == CropHandle.LB || handle == CropHandle.RB) b = (b + dy).coerceIn(t + m, 1f)
                                    }
                                    s.copy(cropL = l, cropT = t, cropR = r, cropB = b)
                                }
                            }
                        }
                ) {
                    val l = slot.cropL * size.width
                    val t = slot.cropT * size.height
                    val r = slot.cropR * size.width
                    val b = slot.cropB * size.height
                    val shade = Color.Black.copy(alpha = 0.6f)
                    drawRect(shade, Offset(0f, 0f), Size(size.width, t))
                    drawRect(shade, Offset(0f, b), Size(size.width, size.height - b))
                    drawRect(shade, Offset(0f, t), Size(l, b - t))
                    drawRect(shade, Offset(r, t), Size(size.width - r, b - t))
                    drawRect(Color.White.copy(alpha = 0.35f), Offset(0f, 0f), size, style = Stroke(1.dp.toPx()))
                    drawRect(ice, Offset(l, t), Size(r - l, b - t), style = Stroke(2.5.dp.toPx()))
                    // terços (ajudam a enquadrar)
                    for (k in 1..2) {
                        val gx = l + (r - l) * k / 3f
                        val gy = t + (b - t) * k / 3f
                        drawLine(ice.copy(alpha = 0.3f), Offset(gx, t), Offset(gx, b), 1.dp.toPx())
                        drawLine(ice.copy(alpha = 0.3f), Offset(l, gy), Offset(r, gy), 1.dp.toPx())
                    }
                    val hs = 9.dp.toPx()
                    listOf(
                        Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b),
                        Offset((l + r) / 2f, t), Offset((l + r) / 2f, b), Offset(l, (t + b) / 2f), Offset(r, (t + b) / 2f)
                    ).forEach { c ->
                        drawCircle(Color.Black.copy(alpha = 0.5f), hs + 2.dp.toPx(), c)
                        drawCircle(ice, hs, c)
                    }
                }
            }
        }
    }
}
