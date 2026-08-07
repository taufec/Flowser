package com.flowser.app

import android.app.AlertDialog
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.flowser.app.browser.BrowserSessionController
import com.flowser.app.browser.BrowserToolbarController
import com.flowser.app.browser.BrowserUiListener
import com.flowser.app.browser.BrowserUiState
import com.flowser.app.browser.BrowserWindowActions
import com.flowser.app.service.ServiceNotificationController
import com.flowser.app.window.BrowserWindowState
import com.flowser.app.window.BubbleController
import com.flowser.app.window.BubbleListener
import com.flowser.app.window.FloatingWindowController
import com.flowser.app.window.FloatingWindowListener
import com.flowser.app.window.RectSize
import com.flowser.app.window.WindowGeometry
import com.flowser.app.window.WindowGeometryEngine
import com.flowser.app.window.WindowMode
import com.flowser.app.window.WindowStateRepository

class FloatingBrowserService : Service(),
    BrowserUiListener,
    BrowserWindowActions,
    FloatingWindowListener,
    BubbleListener {

    private lateinit var repository: WindowStateRepository
    private lateinit var notificationController: ServiceNotificationController

    private var browserController: BrowserSessionController? = null
    private var toolbarController: BrowserToolbarController? = null
    private var windowController: FloatingWindowController? = null
    private var bubbleController: BubbleController? = null
    private var state: BrowserWindowState? = null
    private var lastBrowserState: BrowserUiState? = null
    private var shuttingDown = false

    override fun onCreate() {
        super.onCreate()
        repository = WindowStateRepository(this)
        notificationController = ServiceNotificationController(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_OPEN
        if (action == ACTION_CLOSE) {
            closeSession()
            return START_NOT_STICKY
        }

        enterForeground()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Flowser needs Display over other apps permission.", Toast.LENGTH_LONG).show()
            closeSession()
            return START_NOT_STICKY
        }

        if (browserController == null) {
            initializeSession(intent?.getStringExtra(EXTRA_URL))
        }

        when (action) {
            ACTION_OPEN -> {
                intent?.getStringExtra(EXTRA_URL)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { browserController?.load(it) }
                restoreWindow()
            }

            ACTION_RESTORE -> restoreWindow()
            ACTION_MINIMIZE -> minimizeWindow()
        }
        return START_NOT_STICKY
    }

    private fun initializeSession(requestedUrl: String?) {
        val metrics = resources.displayMetrics
        val defaultGeometry = WindowGeometryEngine.defaultGeometry(
            RectSize(metrics.widthPixels, metrics.heightPixels),
            metrics.density
        )
        var loaded = repository.load(defaultGeometry)
        if (loaded.mode == WindowMode.CLOSED) loaded = loaded.copy(mode = WindowMode.WINDOWED)
        val url = requestedUrl?.takeIf { it.isNotBlank() } ?: loaded.currentUrl
        loaded = loaded.copy(currentUrl = url)
        state = loaded

        val browser = BrowserSessionController(this, this)
        browserController = browser
        val toolbar = BrowserToolbarController(this, browser, this)
        toolbarController = toolbar
        windowController = FloatingWindowController(
            context = this,
            browser = browser,
            toolbar = toolbar,
            repository = repository,
            listener = this
        )
        bubbleController = BubbleController(this, repository, this)

        browser.setDesktopMode(loaded.desktopMode)
        browser.setZoomPercent(loaded.zoomPercent)
        browser.load(url)

        if (loaded.mode == WindowMode.MINIMIZED) {
            bubbleController?.show(loaded, browser.currentFavicon())
        } else {
            windowController?.show(loaded)
        }
        persistState()
    }

    private fun enterForeground() {
        val current = state
        val title = lastBrowserState?.title ?: current?.currentUrl ?: "Floating browser active"
        val mode = current?.mode ?: WindowMode.WINDOWED
        val notification = notificationController.build(title, mode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ServiceNotificationController.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(ServiceNotificationController.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val current = state ?: return
        notificationController.notify(
            lastBrowserState?.title ?: current.currentUrl,
            current.mode
        )
    }

    override fun onBrowserUiStateChanged(state: BrowserUiState) {
        lastBrowserState = state
        windowController?.updateToolbar(state)
        this.state = this.state?.copy(
            currentUrl = state.url,
            desktopMode = state.desktopMode,
            zoomPercent = state.zoomPercent
        )
        persistState()
        updateNotification()
    }

    override fun onFaviconChanged(favicon: Bitmap?) {
        if (state?.mode == WindowMode.MINIMIZED) {
            bubbleController?.updateFavicon(favicon)
        }
    }

    override fun onRendererGone() {
        Toast.makeText(this, "The website process stopped. Close and reopen Flowser.", Toast.LENGTH_LONG).show()
        closeSession()
    }

    override fun minimize() = minimizeWindow()

    private fun minimizeWindow() {
        val current = state ?: return
        if (current.mode == WindowMode.MINIMIZED) return
        val window = windowController ?: return
        val previousMode = window.currentMode().takeIf {
            it == WindowMode.WINDOWED || it == WindowMode.MAXIMIZED
        } ?: WindowMode.WINDOWED
        val minimized = current.copy(
            mode = WindowMode.MINIMIZED,
            preMinimizeMode = previousMode,
            geometry = window.currentGeometry(),
            lastNormalGeometry = window.normalGeometry()
        )
        state = minimized
        window.hidePreservingBrowser()
        bubbleController?.show(minimized, browserController?.currentFavicon())
        persistState()
        updateNotification()
    }

    private fun restoreWindow() {
        val current = state ?: return
        bubbleController?.hide()
        val restoreMode = if (current.mode == WindowMode.MINIMIZED) {
            current.preMinimizeMode.takeIf {
                it == WindowMode.WINDOWED || it == WindowMode.MAXIMIZED
            } ?: WindowMode.WINDOWED
        } else {
            current.mode.takeIf { it == WindowMode.WINDOWED || it == WindowMode.MAXIMIZED }
                ?: WindowMode.WINDOWED
        }
        val restored = current.copy(
            mode = restoreMode,
            geometry = if (restoreMode == WindowMode.WINDOWED) {
                current.lastNormalGeometry
            } else {
                current.geometry
            }
        )
        state = restored
        windowController?.show(restored)
        persistState()
        updateNotification()
    }

    override fun toggleMaximize() {
        windowController?.toggleMaximize()
    }

    override fun close() = closeSession()

    override fun editAddress(currentUrl: String) {
        val input = EditText(this).apply {
            setText(currentUrl)
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Open website")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val normalized = UrlNormalizer.normalize(input.text.toString())
                if (normalized == null) {
                    input.error = "Enter a valid HTTP or HTTPS address"
                } else {
                    browserController?.load(normalized)
                    dialog.dismiss()
                }
            }
        }
        runCatching { dialog.show() }
            .onFailure {
                Toast.makeText(this, "Could not open the address editor.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun openExternally(url: String) {
        runExternalAction(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            "No browser can open this address."
        )
    }

    override fun copyUrl(url: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Flowser URL", url))
        Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show()
    }

    override fun shareUrl(url: String) {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        runExternalAction(
            Intent.createChooser(share, "Share website").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            "No app can share this address."
        )
    }

    private fun runExternalAction(intent: Intent, errorMessage: String) {
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show() }
    }

    override fun onMinimizeRequested() = minimizeWindow()

    override fun onCloseRequested() = closeSession()

    override fun onGeometryChanged(geometry: WindowGeometry) {
        val current = state ?: return
        val normal = windowController?.normalGeometry() ?: current.lastNormalGeometry
        state = current.copy(
            geometry = geometry,
            lastNormalGeometry = normal
        )
        persistState()
    }

    override fun onModeChanged(mode: WindowMode) {
        val current = state ?: return
        val window = windowController ?: return
        state = current.copy(
            mode = mode,
            geometry = window.currentGeometry(),
            lastNormalGeometry = window.normalGeometry()
        )
        persistState()
        updateNotification()
    }

    override fun onRestoreRequested() = restoreWindow()

    override fun onBubblePositionChanged(x: Int, y: Int) {
        state = state?.copy(bubbleX = x, bubbleY = y)
        persistState()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (state?.mode == WindowMode.MINIMIZED) {
            bubbleController?.onConfigurationChanged()
        } else {
            windowController?.onConfigurationChanged()
        }
    }

    private fun persistState() {
        state?.let(repository::save)
    }

    private fun closeSession() {
        if (shuttingDown) return
        shuttingDown = true
        state = state?.copy(mode = WindowMode.CLOSED)
        persistState()
        bubbleController?.destroy()
        windowController?.destroy()
        browserController?.destroy()
        bubbleController = null
        windowController = null
        toolbarController = null
        browserController = null
        notificationController.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (!shuttingDown) {
            shuttingDown = true
            bubbleController?.destroy()
            windowController?.destroy()
            browserController?.destroy()
            notificationController.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_OPEN = "com.flowser.app.action.OPEN"
        const val ACTION_RESTORE = "com.flowser.app.action.RESTORE"
        const val ACTION_MINIMIZE = "com.flowser.app.action.MINIMIZE"
        const val ACTION_CLOSE = "com.flowser.app.action.CLOSE"
        const val EXTRA_URL = "extra_url"
    }
}
