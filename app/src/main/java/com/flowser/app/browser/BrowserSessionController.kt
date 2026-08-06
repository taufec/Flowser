package com.flowser.app.browser

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

internal object BrowserPreferences {
    private const val MIN_ZOOM = 50
    private const val MAX_ZOOM = 200
    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun clampZoom(value: Int): Int = value.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun applyDesktopUserAgent(mobileDefault: String, enabled: Boolean): String =
        if (enabled) DESKTOP_USER_AGENT else mobileDefault
}

data class BrowserUiState(
    val url: String,
    val title: String,
    val isLoading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val desktopMode: Boolean,
    val zoomPercent: Int
)

interface BrowserUiListener {
    fun onBrowserUiStateChanged(state: BrowserUiState)
    fun onRendererGone()
}

class BrowserSessionController(
    context: Context,
    private val listener: BrowserUiListener,
    private val homeUrl: String = DEFAULT_HOME_URL
) {
    val view: WebView = WebView(context)

    private val mobileUserAgent: String
    private var currentUrl: String = homeUrl
    private var currentTitle: String = "Flowser"
    private var isLoading: Boolean = false
    private var desktopMode: Boolean = false
    private var zoomPercent: Int = 100
    private var destroyed: Boolean = false

    init {
        mobileUserAgent = view.settings.userAgentString.orEmpty()
        configureWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = true
        }

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                webView: WebView,
                request: WebResourceRequest
            ): Boolean = false

            @Deprecated("Deprecated in Android")
            override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean = false

            override fun onPageStarted(webView: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                currentUrl = url?.takeIf { it.isNotBlank() } ?: currentUrl
                isLoading = true
                notifyState()
            }

            override fun onPageFinished(webView: WebView, url: String?) {
                currentUrl = url?.takeIf { it.isNotBlank() } ?: currentUrl
                isLoading = false
                notifyState()
            }

            override fun onRenderProcessGone(
                webView: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                if (!destroyed) {
                    destroyed = true
                    runCatching { webView.destroy() }
                    listener.onRendererGone()
                }
                return true
            }
        }

        view.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(webView: WebView?, title: String?) {
                currentTitle = title?.takeIf { it.isNotBlank() } ?: currentTitle
                notifyState()
            }

            override fun onProgressChanged(webView: WebView?, newProgress: Int) {
                val loading = newProgress < 100
                if (loading != isLoading) {
                    isLoading = loading
                    notifyState()
                }
            }
        }
    }

    fun load(url: String) {
        if (destroyed) return
        currentUrl = url
        view.loadUrl(url)
        notifyState()
    }

    fun goBack() {
        if (!destroyed && view.canGoBack()) view.goBack()
    }

    fun goForward() {
        if (!destroyed && view.canGoForward()) view.goForward()
    }

    fun reload() {
        if (!destroyed) view.reload()
    }

    fun stopLoading() {
        if (!destroyed) {
            view.stopLoading()
            isLoading = false
            notifyState()
        }
    }

    fun goHome() = load(homeUrl)

    fun setDesktopMode(enabled: Boolean) {
        if (destroyed || desktopMode == enabled) return
        desktopMode = enabled
        view.settings.userAgentString = BrowserPreferences.applyDesktopUserAgent(
            mobileUserAgent,
            enabled
        )
        view.settings.useWideViewPort = enabled
        view.settings.loadWithOverviewMode = enabled
        view.reload()
        notifyState()
    }

    fun zoomIn() = applyZoom(zoomPercent + ZOOM_STEP)

    fun zoomOut() = applyZoom(zoomPercent - ZOOM_STEP)

    fun resetZoom() = applyZoom(100)

    fun setZoomPercent(value: Int) = applyZoom(value)

    fun currentState(): BrowserUiState = snapshot()

    fun destroy() {
        if (destroyed) return
        destroyed = true
        runCatching {
            view.stopLoading()
            view.loadUrl("about:blank")
            view.clearHistory()
            view.removeAllViews()
            view.destroy()
        }
    }

    private fun applyZoom(value: Int) {
        if (destroyed) return
        zoomPercent = BrowserPreferences.clampZoom(value)
        view.settings.textZoom = zoomPercent
        view.setInitialScale(zoomPercent)
        notifyState()
    }

    private fun notifyState() {
        if (!destroyed) listener.onBrowserUiStateChanged(snapshot())
    }

    private fun snapshot(): BrowserUiState {
        val host = runCatching { Uri.parse(currentUrl).host }.getOrNull()
        val title = currentTitle.takeIf { it.isNotBlank() }
            ?: host
            ?: "Flowser"
        return BrowserUiState(
            url = currentUrl,
            title = title,
            isLoading = isLoading,
            canGoBack = !destroyed && view.canGoBack(),
            canGoForward = !destroyed && view.canGoForward(),
            desktopMode = desktopMode,
            zoomPercent = zoomPercent
        )
    }

    companion object {
        private const val DEFAULT_HOME_URL = "https://example.com"
        private const val ZOOM_STEP = 10
    }
}
