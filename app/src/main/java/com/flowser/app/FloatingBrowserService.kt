package com.flowser.app

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class FloatingBrowserService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayRoot: View? = null
    private var webView: WebView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val requestedUrl = intent
            ?.getStringExtra(EXTRA_URL)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_URL

        showOverlay(requestedUrl)
        return START_NOT_STICKY
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay(url: String) {
        removeOverlay()

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val width = minOf((screenWidth - dp(24)).coerceAtLeast(dp(260)), dp(720))
        val height = minOf(
            (screenHeight * 0.72f).toInt(),
            (screenHeight - dp(96)).coerceAtLeast(dp(320)),
            dp(900)
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(12).toFloat()
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.rgb(50, 56, 68))
            }
            clipToOutline = true
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundColor(Color.rgb(31, 36, 48))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        }

        val title = TextView(this).apply {
            text = Uri.parse(url).host ?: "Flowser"
            textSize = 15f
            setTextColor(Color.WHITE)
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        }

        val refreshButton = toolbarButton("↻", "Refresh page").apply {
            setOnClickListener { webView?.reload() }
        }

        val closeButton = toolbarButton("×", "Close Flowser").apply {
            setOnClickListener { stopSelf() }
        }

        toolbar.addView(title)
        toolbar.addView(refreshButton)
        toolbar.addView(closeButton)

        val browser = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.WHITE)
            isFocusable = true
            isFocusableInTouchMode = true

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = false

                @Deprecated("Deprecated in Android")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = false
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }

        root.addView(toolbar)
        root.addView(browser)

        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(72)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        installDragHandler(toolbar, root, params, screenWidth, screenHeight)

        try {
            windowManager.addView(root, params)
            overlayRoot = root
            overlayParams = params
            webView = browser
            browser.requestFocus()
        } catch (_: Exception) {
            browser.destroy()
            stopSelf()
        }
    }

    private fun toolbarButton(label: String, description: String): Button =
        Button(this).apply {
            text = label
            contentDescription = description
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = dp(48)
            minimumWidth = dp(48)
            minHeight = dp(48)
            minimumHeight = dp(48)
            setPadding(0, 0, 0, 0)
        }

    private fun installDragHandler(
        dragView: View,
        root: View,
        params: WindowManager.LayoutParams,
        screenWidth: Int,
        screenHeight: Int
    ) {
        dragView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                return when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val proposedX = initialX + (event.rawX - initialTouchX).toInt()
                        val proposedY = initialY + (event.rawY - initialTouchY).toInt()
                        val maxX = (screenWidth - params.width).coerceAtLeast(0)
                        val maxY = (screenHeight - dp(48)).coerceAtLeast(0)

                        params.x = proposedX.coerceIn(0, maxX)
                        params.y = proposedY.coerceIn(0, maxY)
                        runCatching { windowManager.updateViewLayout(root, params) }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.performClick()
                        true
                    }

                    else -> false
                }
            }
        })
    }

    private fun removeOverlay() {
        val root = overlayRoot
        if (root != null) {
            runCatching { windowManager.removeViewImmediate(root) }
        }

        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }

        overlayRoot = null
        overlayParams = null
        webView = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val DEFAULT_URL = "https://example.com"
    }
}
