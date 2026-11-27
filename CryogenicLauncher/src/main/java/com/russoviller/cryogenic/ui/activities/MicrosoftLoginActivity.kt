package com.russoviller.cryogenic.ui.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.russoviller.cryogenic.game.account.microsoft.MicrosoftWebLogin

class MicrosoftLoginActivity : ComponentActivity() {
    private var done = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(6, 16, 31)) }
        val progress = ProgressBar(this).apply { isIndeterminate = true }
        val web = WebView(this)
        CookieManager.getInstance().removeAllCookies(null)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            //O processo da página morreu (falta de memória): sem retornar true o Android fecha o Launcher inteiro
            override fun onRenderProcessGone(view: android.webkit.WebView, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                runCatching { (view.parent as? android.view.ViewGroup)?.removeView(view); view.destroy() }
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = handle(request.url)

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (handle(Uri.parse(url))) view.stopLoading()
                progress.visibility = ProgressBar.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = ProgressBar.GONE
            }
        }
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(
            progress,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack()
                } else {
                    finishWith(null, null)
                }
            }
        })
        web.loadUrl(MicrosoftWebLogin.authorizeUrl)
    }

    private fun handle(uri: Uri): Boolean {
        if (done || !uri.toString().startsWith(MicrosoftWebLogin.REDIRECT)) return false
        finishWith(
            uri.getQueryParameter("code"),
            uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error")
        )
        return true
    }

    private fun finishWith(code: String?, error: String?) {
        if (done) return
        done = true
        if (code != null) MicrosoftWebLogin.deliverCode(code) else MicrosoftWebLogin.deliverError(error)
        finish()
    }

    override fun onDestroy() {
        if (!done) {
            done = true
            MicrosoftWebLogin.deliverError(null)
        }
        super.onDestroy()
    }
}
