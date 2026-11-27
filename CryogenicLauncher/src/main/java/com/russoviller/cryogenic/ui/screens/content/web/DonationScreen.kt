package com.russoviller.cryogenic.ui.screens.content.web

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.russoviller.cryogenic.BuildConfig
import com.russoviller.cryogenic.R
import com.russoviller.cryogenic.ui.base.BaseScreen
import com.russoviller.cryogenic.ui.components.CryoGlassColors
import com.russoviller.cryogenic.ui.components.cryoBackground
import com.russoviller.cryogenic.ui.components.cryoBorderBrush
import com.russoviller.cryogenic.ui.components.cryoChamferShape
import com.russoviller.cryogenic.ui.components.cryoGlass
import com.russoviller.cryogenic.ui.components.cryoGlow
import com.russoviller.cryogenic.ui.components.cryoGradient
import com.russoviller.cryogenic.ui.screens.NormalNavKey
import com.russoviller.cryogenic.ui.screens.TitledNavKey

/** Página de doação (Pix) do RussoViller. */
const val CRYO_DONATION_URL = "https://pixgg.com/russoViller02"
const val CRYO_AFDIAN_URL = "https://afdian.com/a/russoviller00"

/** Formas de apoio: Pix (PixGG, Brasil e exterior) e afdian (Ásia, principalmente China). */
enum class DonationMethod(val url: String, val address: String) {
    PIX(CRYO_DONATION_URL, "pixgg.com/russoViller02"),
    AFDIAN(CRYO_AFDIAN_URL, "afdian.com/a/russoviller00")
}

/** Quem usa o Launcher em chinês já abre no afdian; o resto começa no Pix. */
private fun defaultDonationMethod(): DonationMethod =
    if (java.util.Locale.getDefault().language == "zh") DonationMethod.AFDIAN else DonationMethod.PIX
private const val DONATION_HOST = "pixgg.com"

private fun isDonationHost(url: String?): Boolean {
    val host = url?.let { Uri.parse(it).host } ?: return false
    return host == DONATION_HOST || host.endsWith(".$DONATION_HOST")
}

/**
 * Faz o "copiar" do site funcionar dentro do Launcher: o código Pix copiado vai para a área de
 * transferência do Android. Só responde quando a página aberta é a de doação.
 */
private class DonationClipboardBridge(
    private val context: Context,
    private val view: WebView
) {
    @JavascriptInterface
    fun copy(text: String?) {
        if (text.isNullOrEmpty() || text.length > 4096) return
        view.post {
            if (!isDonationHost(view.url)) return@post
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@post
            clipboard.setPrimaryClip(ClipData.newPlainText("Pix", text))
            Toast.makeText(context, R.string.cryo_donate_copied, Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    fun copied() {
        view.post {
            if (isDonationHost(view.url)) Toast.makeText(context, R.string.cryo_donate_copied, Toast.LENGTH_LONG).show()
        }
    }
}

/** Substitui navigator.clipboard.writeText (que não funciona em WebView) pela ponte acima. */
private const val CLIPBOARD_POLYFILL =
    "(function(){if(window.__cryoClip)return;window.__cryoClip=true;try{var c=navigator.clipboard||{};" +
        "c.writeText=function(t){try{CryoClipboard.copy(String(t));}catch(e){}return Promise.resolve();};" +
        "try{Object.defineProperty(navigator,'clipboard',{value:c,configurable:true});}catch(e){}" +
        "document.addEventListener('copy',function(){try{CryoClipboard.copied();}catch(e){}},true);}catch(e){}})()"

/**
 * A página do PixGG foi feita para telas em pé: o formulário fica centralizado com posição absoluta
 * e a rolagem da página é bloqueada. No painel deitado do Launcher ele ficava fora da área visível
 * (só aparecia o fundo branco). Este estilo põe o formulário no fluxo normal e libera a rolagem.
 */
private const val DONATION_LAYOUT_CSS =
    "html,body{overflow-x:hidden!important;overflow-y:auto!important;height:auto!important;min-height:100%!important;background:#0e0e12!important}main.loggedOutPage{display:block!important;height:auto!important;min-height:100vh!important;overflow:visible!important;padding:14px 10px 28px!important;box-sizing:border-box!important}main.loggedOutPage>span,main.loggedOutPage section.content-center{display:block!important;position:relative!important;height:auto!important;width:100%!important}#payment-form{position:relative!important;top:auto!important;left:auto!important;right:auto!important;bottom:auto!important;transform:none!important;margin:0 auto!important;max-width:100%!important;z-index:5!important}"

private const val DONATION_LAYOUT_SCRIPT =
    "(function(){try{if(document.getElementById('cryo-fit'))return;var s=document.createElement('style');" +
        "s.id='cryo-fit';s.textContent=" + "\"" + DONATION_LAYOUT_CSS + "\"" + ";" +
        "(document.head||document.documentElement).appendChild(s);}catch(e){}})()"

@Composable
fun DonationScreen(
    key: NormalNavKey.Donation,
    currentKey: TitledNavKey?
) {
    BaseScreen(
        screenKey = key,
        currentKey = currentKey
    ) { isVisible ->
        var method by rememberSaveable { mutableStateOf(defaultDonationMethod()) }
        if (isVisible) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DonationInfoPanel(
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxHeight(),
                    method = method,
                    onMethodChange = { method = it }
                )
                DonationWebView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    method = method
                )
            }
        }
    }
}

@Composable
private fun DonationInfoPanel(
    modifier: Modifier = Modifier,
    method: DonationMethod,
    onMethodChange: (DonationMethod) -> Unit
) {
    val afdian = method == DonationMethod.AFDIAN
    Column(
        modifier = modifier
            .cryoGlass(shape = cryoChamferShape(18.dp), cornerRadius = 18.dp, opaque = true)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //Escolha da forma de apoio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(CryoGlassColors.Deep.copy(alpha = 0.7f))
                .border(1.dp, cryoBorderBrush(), CircleShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DonationMethodTab("Pix", method == DonationMethod.PIX) { onMethodChange(DonationMethod.PIX) }
            DonationMethodTab(stringResource(R.string.cryo_donate_afdian), afdian) { onMethodChange(DonationMethod.AFDIAN) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .cryoGlow(22.dp, selected = true, strength = 0.8f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFF6B9A), Color(0xFFFF9EC4))))
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_favorite_filled),
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (afdian) "RV // AFDIAN" else "RV // PIX",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp, fontSize = 9.sp),
                    color = CryoGlassColors.Ice
                )
                Text(
                    text = stringResource(R.string.cryo_donate_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.horizontalGradient(listOf(Color.White, CryoGlassColors.Ice))
                    )
                )
            }
        }
        Text(
            text = stringResource(if (afdian) R.string.cryo_donate_afdian_summary else R.string.cryo_donate_summary),
            style = MaterialTheme.typography.bodySmall,
            color = CryoGlassColors.Frost
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(CryoGlassColors.Ice.copy(alpha = 0.6f), Color.Transparent)))
        )
        if (afdian) {
            DonationStep(1, stringResource(R.string.cryo_donate_afdian_step1))
            DonationStep(2, stringResource(R.string.cryo_donate_afdian_step2))
            DonationStep(3, stringResource(R.string.cryo_donate_afdian_step3))
        } else {
            DonationStep(1, stringResource(R.string.cryo_donate_step1))
            DonationStep(2, stringResource(R.string.cryo_donate_step2))
            DonationStep(3, stringResource(R.string.cryo_donate_step3))
        }
        Spacer(Modifier.weight(1f, fill = false))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CryoGlassColors.Aurora.copy(alpha = 0.10f))
                .border(1.dp, CryoGlassColors.Aurora.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            text = stringResource(if (afdian) R.string.cryo_donate_afdian_note else R.string.cryo_donate_note),
            style = MaterialTheme.typography.labelSmall,
            color = CryoGlassColors.Frost
        )
    }
}

@Composable
private fun RowScope.DonationMethodTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.cryoBackground(cryoGradient()) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            fontWeight = FontWeight.Black,
            color = if (selected) CryoGlassColors.Deep else CryoGlassColors.Frost,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DonationStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .cryoBackground(cryoGradient()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium.copy(shadow = null),
                fontWeight = FontWeight.Black,
                color = CryoGlassColors.Deep
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DonationWebView(modifier: Modifier = Modifier, method: DonationMethod) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedMethod by remember { mutableStateOf<DonationMethod?>(null) }
    var clearHistory by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val shownProgress by animateFloatAsState(progress, label = "donate_progress")

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Column(
        modifier = modifier
            .cryoGlass(shape = cryoChamferShape(18.dp), cornerRadius = 18.dp, opaque = true)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //Barra do topo: endereço + recarregar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(CryoGlassColors.Deep.copy(alpha = 0.7f))
                    .border(1.dp, cryoBorderBrush(), CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (loading) CryoGlassColors.Aurora else Color(0xFF7CF5A8))
                )
                Text(
                    text = method.address,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = CryoGlassColors.Frost,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DonationHeaderButton(R.drawable.ic_refresh) {
                failed = false
                webView?.reload()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(CryoGlassColors.Ice.copy(alpha = if (loading) 0.15f else 0f))
        ) {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(shownProgress.coerceIn(0.05f, 1f))
                        .cryoBackground(cryoGradient())
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0E0E12))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.rgb(14, 14, 18))
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.userAgentString = settings.userAgentString + " CryogenicLauncher/" + BuildConfig.VERSION_NAME
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(DonationClipboardBridge(ctx.applicationContext, this), "CryoClipboard")
                        webChromeClient = object : WebChromeClient() {
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
                                //Páginas web continuam dentro do Launcher; outros esquemas (app do banco, intent:) abrem o app certo
                                if (scheme == "http" || scheme == "https") return false
                                runCatching {
                                    val intent = if (scheme == "intent") Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                                        else Intent(Intent.ACTION_VIEW, uri)
                                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                    intent.component = null
                                    intent.selector = null
                                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                if (isDonationHost(url)) {
                                    view?.evaluateJavascript(DONATION_LAYOUT_SCRIPT, null)
                                    view?.evaluateJavascript(CLIPBOARD_POLYFILL, null)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                //Ao trocar de Pix para afdian (ou o contrário), o "voltar" não volta para o outro site
                                if (clearHistory) {
                                    clearHistory = false
                                    view?.clearHistory()
                                }
                                canGoBack = view?.canGoBack() == true
                                if (isDonationHost(url)) {
                                    view?.evaluateJavascript(DONATION_LAYOUT_SCRIPT, null)
                                    view?.evaluateJavascript(CLIPBOARD_POLYFILL, null)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) failed = true
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                canGoBack = view?.canGoBack() == true
                            }
                        }
                        loadedMethod = method
                        loadUrl(method.url)
                        webView = this
                    }
                }
            )
            if (failed) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CryoGlassColors.Deep.copy(alpha = 0.92f))
                        .border(1.dp, cryoBorderBrush(), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cryo_donate_offline),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    DonationHeaderButton(R.drawable.ic_refresh) {
                        failed = false
                        webView?.reload()
                    }
                }
            }
        }
    }

    LaunchedEffect(method, webView) {
        val view = webView ?: return@LaunchedEffect
        if (loadedMethod == method) return@LaunchedEffect
        loadedMethod = method
        failed = false
        clearHistory = true
        view.stopLoading()
        view.loadUrl(method.url)
    }

    //Em segundo plano a página para de rodar scripts e animações
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> webView?.onPause()
                Lifecycle.Event.ON_START -> webView?.onResume()
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
                removeJavascriptInterface("CryoClipboard")
                destroy()
            }
            webView = null
        }
    }
}

@Composable
private fun DonationHeaderButton(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CryoGlassColors.Ice.copy(alpha = 0.10f))
            .border(1.dp, cryoBorderBrush(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CryoGlassColors.Ice
        )
    }
}
