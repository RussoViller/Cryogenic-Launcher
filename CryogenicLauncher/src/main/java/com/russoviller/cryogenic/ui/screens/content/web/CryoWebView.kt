package com.russoviller.cryogenic.ui.screens.content.web

import com.russoviller.cryogenic.ui.components.cryoChamferShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import com.russoviller.cryogenic.ui.components.cryoBackground

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russoviller.cryogenic.BuildConfig
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import com.russoviller.cryogenic.ui.components.cryoFillFactor
import com.russoviller.cryogenic.ui.components.cryoGlow
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGradient

const val RV_WEBPAGE_URL = "https://russoviller.shardweb.app/index.html"
private const val RV_HOST = "russoviller.shardweb.app"

/**
 * @param fresh usa um carimbo único, o que força o WebView a recarregar a página inteira
 * (sem isso, trocar só o "#âncora" é uma navegação no mesmo documento e o site não reage).
 */
fun rvWebPageUrl(anchor: String = "inicio", fresh: Boolean = false): String {
    val now = System.currentTimeMillis()
    val stamp = if (fresh) now else now / 60_000L
    return "$RV_WEBPAGE_URL?app=cryogenic&v=$stamp#$anchor"
}

private val RV_VIEW_ID = Regex("[A-Za-z0-9_-]{1,32}")

/**
 * O site é uma página única: quem troca de seção é a função `ir(id)` dele, e ele não escuta
 * mudanças de "#âncora". Então a navegação pela barra lateral chama `ir` direto (ou clica num
 * botão `data-go` equivalente) em vez de recarregar a URL.
 */
private fun rvNavScript(anchor: String): String =
    "(function(id){try{if(typeof ir==='function'){ir(id);return 'ok';}" +
        "var b=document.querySelector('[data-go=\"'+id+'\"]');if(b){b.click();return 'ok';}}catch(e){}" +
        "return 'no';})('$anchor')"

/** Clica no botão "voltar" da seção aberta no site. */
private const val RV_BACK_SCRIPT =
    "(function(){try{var b=document.querySelector('.view.active:not(.saindo) .back')||" +
        "document.querySelector('.view.active .back');if(b){b.click();return 'ok';}}catch(e){}return 'no';})()"

/** Avisa o launcher sempre que a seção ativa do site mudar (inclusive por cliques dentro do site). */
private const val RV_HOOK_SCRIPT =
    "(function(){if(window.__cryoHook)return;window.__cryoHook=true;var last='';" +
        "function rep(){try{var v=document.querySelector('.view.active:not(.saindo)')||document.querySelector('.view.active');" +
        "var id=v?v.id:'';if(id&&id!==last){last=id;CryoBridge.onView(id);}}catch(e){}}" +
        "try{new MutationObserver(rep).observe(document.querySelector('main')||document.body," +
        "{subtree:true,attributes:true,attributeFilter:['class']});}catch(e){}rep();})()"

/**
 * Passa o tema atual do launcher para o site: classe "in-app" e as cores do tema como variáveis CSS
 * (trios R,G,B). Assim o site acompanha qualquer tema escolhido no launcher, não só o Cryogenic.
 */
private fun rvThemeScript(): String {
    fun rgb(c: Color) = "${(c.red * 255f).roundToInt()},${(c.green * 255f).roundToInt()},${(c.blue * 255f).roundToInt()}"
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(CryoGlassColors.Ice.toArgb(), hsl)
    // 198.3 graus é o matiz do gelo do tema Cryogenic, que é a base das cores e do fundo do site
    val hueShift = (((hsl[0] - 198.3f) % 360f) + 360f) % 360f
    val themed = hueShift > 3f && hueShift < 357f
    return "(function(){try{var r=document.documentElement,s=r.style;r.classList.add('in-app');" +
        "s.setProperty('--ice-rgb','${rgb(CryoGlassColors.Ice)}');" +
        "s.setProperty('--ice-soft-rgb','${rgb(CryoGlassColors.IceSoft)}');" +
        "s.setProperty('--aurora-rgb','${rgb(CryoGlassColors.Aurora)}');" +
        "s.setProperty('--frost-rgb','${rgb(CryoGlassColors.Frost)}');" +
        "s.setProperty('--deep-rgb','${rgb(CryoGlassColors.Deep)}');" +
        "s.setProperty('--navy-rgb','${rgb(CryoGlassColors.Navy)}');" +
        "s.setProperty('--bg-hue','${hueShift.roundToInt()}deg');" +
        "r.classList.toggle('themed',$themed);}catch(e){}})()"
}

private class CryoWebBridge(private val view: WebView, private val onViewChanged: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun onView(id: String?) {
        if (id == null || !RV_VIEW_ID.matches(id)) return
        view.post { onViewChanged(id) }
    }
}

/** Seções internas do site que não têm botão próprio na barra lateral -> botão que fica aceso. */
private val webSectionParents = mapOf(
    "cry" to "zalith",
    "mg" to "zalith",
    "trabalho" to "downloads",
    "curseMain" to "outros",
    "modMain" to "outros",
    "config" to "inicio",
    "avaliar" to "inicio"
)

private fun isInternal(uri: Uri): Boolean {
    val host = uri.host ?: return false
    if (host == RV_HOST || host == "unavatar.io" || host == "api.github.com") return true
    if (host == "github.com" && uri.path?.contains("/releases/download/") == true) return true
    if (host.endsWith("githubusercontent.com")) return true
    return false
}

private fun openExternal(context: Context, uri: Uri) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun dispositionName(value: String?): String? {
    if (value.isNullOrBlank()) return null
    Regex("filename\\*\\s*=\\s*[^']*''([^;]+)", RegexOption.IGNORE_CASE).find(value)?.let {
        return runCatching { java.net.URLDecoder.decode(it.groupValues[1].trim().trim('"'), "UTF-8") }.getOrNull()
    }
    Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(value)?.let {
        return it.groupValues[1].trim()
    }
    return null
}

fun cryoDownloadName(url: String, contentDisposition: String?, mimeType: String?): String {
    val uri = Uri.parse(url)
    val candidates = listOfNotNull(
        dispositionName(contentDisposition),
        dispositionName(uri.getQueryParameter("response-content-disposition")),
        uri.lastPathSegment?.takeIf { it.contains('.') }
    )
    var name = candidates.firstOrNull { it.isNotBlank() } ?: URLUtil.guessFileName(url, contentDisposition, mimeType)
    name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "download" }
    if (!name.endsWith(".zip", ignoreCase = true)) {
        name = name.removeSuffix(".bin").removeSuffix(".BIN").substringBeforeLast(".zip") + ".zip"
    }
    return name
}

data class CryoWebDownload(
    val id: Long,
    val name: String,
    val progress: Float = 0f,
    val status: Int = DownloadManager.STATUS_PENDING
)

private fun startDownload(context: Context, url: String, contentDisposition: String?, mimeType: String?): CryoWebDownload? {
    return runCatching {
        val fileName = cryoDownloadName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setMimeType("application/zip")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Cryogenic/$fileName")
        val id = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, context.getString(R.string.cryo_web_downloading, fileName), Toast.LENGTH_LONG).show()
        CryoWebDownload(id, fileName)
    }.onFailure {
        openExternal(context, Uri.parse(url))
    }.getOrNull()
}

private data class WebSection(val anchor: String, val iconRes: Int, val labelRes: Int)

private val webSections = listOf(
    WebSection("inicio", R.drawable.ic_home_filled, R.string.cryo_web_home),
    WebSection("downloads", R.drawable.ic_download_2_filled, R.string.cryo_web_downloads),
    WebSection("setas", R.drawable.ic_mouse_filled, R.string.cryo_web_arrows),
    WebSection("wall", R.drawable.ic_image_filled, R.string.cryo_web_wallpapers),
    WebSection("hud", R.drawable.ic_touch_app_filled, R.string.cryo_web_hud),
    WebSection("zalith", R.drawable.ic_sports_esports_filled, R.string.cryo_web_launchers),
    WebSection("github", R.drawable.ic_github, R.string.cryo_web_github),
    WebSection("outros", R.drawable.ic_extension_outlined, R.string.cryo_web_projects),
    WebSection("discord", R.drawable.ic_chat_info, R.string.cryo_web_discord),
    WebSection("social", R.drawable.ic_share_filled, R.string.cryo_web_social)
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CryoWebView(
    url: String,
    modifier: Modifier = Modifier,
    title: String,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var currentAnchor by remember { mutableStateOf(Uri.parse(url).fragment ?: "inicio") }
    val downloads = remember { androidx.compose.runtime.mutableStateListOf<CryoWebDownload>() }

    var pageReady by remember { mutableStateOf(false) }
    var pendingAnchor by remember { mutableStateOf<String?>(null) }
    val highlighted = webSectionParents[currentAnchor] ?: currentAnchor

    fun onRvSite(view: WebView): Boolean = view.url?.let { Uri.parse(it).host } == RV_HOST

    fun go(anchor: String) {
        val view = webView ?: return
        if (!RV_VIEW_ID.matches(anchor)) return
        currentAnchor = anchor
        when {
            !onRvSite(view) -> view.loadUrl(rvWebPageUrl(anchor, fresh = true))
            !pageReady -> pendingAnchor = anchor
            else -> view.evaluateJavascript(rvNavScript(anchor)) { result ->
                if (result == null || !result.contains("ok")) {
                    view.loadUrl(rvWebPageUrl(anchor, fresh = true))
                }
            }
        }
    }

    fun reload() {
        webView?.loadUrl(rvWebPageUrl(currentAnchor, fresh = true))
    }

    fun goBackInSite() {
        val view = webView ?: return
        if (onRvSite(view) && pageReady && currentAnchor != "inicio") {
            view.evaluateJavascript(RV_BACK_SCRIPT) { result ->
                if (result == null || !result.contains("ok")) go("inicio")
            }
        } else if (view.canGoBack()) {
            view.goBack()
        }
    }

    val canStepBack = canGoBack || currentAnchor != "inicio"

    BackHandler(enabled = canStepBack) {
        goBackInSite()
    }

    LaunchedEffect(downloads.size) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (downloads.any { it.status != DownloadManager.STATUS_SUCCESSFUL && it.status != DownloadManager.STATUS_FAILED }) {
            downloads.indices.forEach { index ->
                val item = downloads[index]
                runCatching {
                    manager.query(DownloadManager.Query().setFilterById(item.id))?.use { c ->
                        if (c.moveToFirst()) {
                            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            downloads[index] = item.copy(
                                status = status,
                                progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else item.progress
                            )
                        }
                    }
                }
            }
            kotlinx.coroutines.delay(500)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .width(196.dp)
                .fillMaxHeight()
                .cryoGlass(shape = cryoChamferShape(18.dp), cornerRadius = 18.dp, opaque = true)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .cryoGlow(18.dp, selected = true, strength = 0.7f)
                        .clip(CircleShape)
                        .cryoBackground(cryoGradient())
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_public),
                        contentDescription = null,
                        tint = CryoGlassColors.Deep
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RV // WEB",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, fontSize = 9.sp),
                        color = CryoGlassColors.Ice
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (loading) CryoGlassColors.Aurora else Color(0xFF7CF5A8))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.6f), Color.Transparent)))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                webSections.forEach { section ->
                    WebSectionItem(
                        section = section,
                        selected = highlighted == section.anchor,
                        onClick = { go(section.anchor) }
                    )
                }
            }
            if (downloads.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.cryo_web_downloads_panel),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                    color = CryoGlassColors.Aurora
                )
                downloads.takeLast(3).reversed().forEach { item ->
                    WebDownloadRow(item)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .cryoGlass(shape = cryoChamferShape(18.dp), cornerRadius = 18.dp, opaque = true)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CryoGlassColors.Deep.copy(alpha = 0.7f))
                        .border(1.dp, CryoGlassColors.Ice.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (loading) CryoGlassColors.Aurora else Color(0xFF7CF5A8))
                    )
                    Text(
                        text = "$RV_HOST / #$currentAnchor",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = CryoGlassColors.Frost,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                WebHeaderButton(R.drawable.ic_arrow_back, canStepBack) { goBackInSite() }
                WebHeaderButton(R.drawable.ic_refresh, true) { reload() }
                WebHeaderButton(R.drawable.ic_link, true) {
                    openExternal(context, Uri.parse(webView?.url ?: url))
                }
                if (onClose != null) {
                    WebHeaderButton(R.drawable.ic_close, true, onClose)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(CryoGlassColors.Ice.copy(alpha = 0.12f))
            ) {
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                            .height(2.dp)
                            .cryoBackground(cryoGradient())
                    )
                }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(14.dp)),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.userAgentString = settings.userAgentString + " CryogenicLauncher/" + BuildConfig.VERSION_NAME
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            //O processo da página morreu (falta de memória): sem retornar true o Android fecha o Launcher inteiro
                            override fun onRenderProcessGone(view: android.webkit.WebView, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                runCatching { (view.parent as? android.view.ViewGroup)?.removeView(view); view.destroy() }
                                return true
                            }

                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val uri = request.url
                                val scheme = uri.scheme ?: return false
                                if (scheme != "http" && scheme != "https") {
                                    openExternal(ctx, uri)
                                    return true
                                }
                                if (!isInternal(uri)) {
                                    openExternal(ctx, uri)
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                pageReady = false
                            }

                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                //Aplica as cores do tema assim que a página aparece, antes de terminar de carregar
                                if (view != null && url?.let { Uri.parse(it).host } == RV_HOST) {
                                    view.evaluateJavascript(rvThemeScript(), null)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                canGoBack = view?.canGoBack() == true
                                if (view == null || url?.let { Uri.parse(it).host } != RV_HOST) return
                                pageReady = true
                                view.evaluateJavascript(rvThemeScript(), null)
                                view.evaluateJavascript(RV_HOOK_SCRIPT, null)
                                pendingAnchor?.let { anchor ->
                                    pendingAnchor = null
                                    view.evaluateJavascript(rvNavScript(anchor), null)
                                }
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                canGoBack = view?.canGoBack() == true
                                url?.let { Uri.parse(it).fragment }?.takeIf { it.isNotBlank() }?.let { currentAnchor = it }
                            }
                        }
                        addJavascriptInterface(
                            CryoWebBridge(this) { id -> currentAnchor = id },
                            "CryoBridge"
                        )
                        setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                            startDownload(ctx, downloadUrl, contentDisposition, mimeType)?.let { downloads.add(it) }
                        }
                        loadUrl(url)
                        webView = this
                    }
                }
            )
        }
    }

    //Em segundo plano a página para de rodar scripts e animações
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> webView?.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_START -> webView?.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }
}

/**
 * Botão de seção da barra lateral, no mesmo estilo da barra lateral principal do launcher:
 * pílula de vidro, ícone em selo redondo e destaque animado quando selecionado.
 */
@Composable
private fun WebSectionItem(section: WebSection, selected: Boolean, onClick: () -> Unit) {
    val progress by animateFloatAsState(if (selected) 1f else 0f, label = "web_section")
    val shape = CircleShape
    val fillFactor = cryoFillFactor().coerceAtMost(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .then(if (selected) Modifier.cryoGlow(20.dp, selected = true, strength = 0.9f) else Modifier)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        CryoGlassColors.Ice.copy(alpha = 0.08f + 0.10f * fillFactor),
                        CryoGlassColors.Navy.copy(alpha = 0.30f + 0.30f * fillFactor)
                    )
                )
            )
            .then(if (progress > 0f) Modifier.cryoBackground(cryoGradient(), alpha = progress) else Modifier)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.15f))) else cryoBorderBrush(),
                shape
            )
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Brush.linearGradient(listOf(CryoGlassColors.Deep.copy(alpha = 0.85f), CryoGlassColors.Navy.copy(alpha = 0.85f)))
                    else Brush.linearGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.22f), CryoGlassColors.Aurora.copy(alpha = 0.10f)))
                )
                .border(1.dp, CryoGlassColors.Ice.copy(alpha = if (selected) 0.9f else 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(section.iconRes),
                contentDescription = null,
                tint = if (selected) CryoGlassColors.Ice else CryoGlassColors.Frost
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(section.labelRes),
            style = if (selected) MaterialTheme.typography.labelLarge.copy(shadow = null) else MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier
                .size(16.dp)
                .alpha(if (selected) 0.9f else 0.35f),
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            tint = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost
        )
    }
}

@Composable
private fun WebDownloadRow(item: CryoWebDownload) {
    val failed = item.status == DownloadManager.STATUS_FAILED
    val done = item.status == DownloadManager.STATUS_SUCCESSFUL
    val accent = when {
        failed -> com.russoviller.cryogenic.ui.components.CryoDanger
        done -> Color(0xFF7CF5A8)
        else -> CryoGlassColors.Ice
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (done) 1f else item.progress.coerceIn(0.03f, 1f))
                    .height(4.dp)
                    .background(accent)
            )
        }
        Text(
            text = when {
                failed -> stringResource(R.string.cryo_web_download_failed)
                done -> stringResource(R.string.cryo_web_download_done)
                else -> "${(item.progress * 100).toInt()}%"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = accent
        )
    }
}

@Composable
private fun WebHeaderButton(iconRes: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CryoGlassColors.Ice.copy(alpha = if (enabled) 0.10f else 0.03f))
            .border(1.dp, cryoBorderBrush(if (enabled) 1f else 0.4f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

@Composable
fun CryoWebDialog(anchor: String, title: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CryoWebView(
            url = rvWebPageUrl(anchor),
            title = title,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            onClose = onDismiss
        )
    }
}

@Composable
fun RvWebPageScreen(
    key: com.russoviller.cryogenic.ui.screens.NormalNavKey.RvWebPage,
    currentKey: com.russoviller.cryogenic.ui.screens.TitledNavKey?
) {
    com.russoviller.cryogenic.ui.base.BaseScreen(
        screenKey = key,
        currentKey = currentKey
    ) { isVisible ->
        if (isVisible) {
            val url = remember { rvWebPageUrl() }
            CryoWebView(
                url = url,
                title = stringResource(R.string.cryo_nav_webpage),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}
