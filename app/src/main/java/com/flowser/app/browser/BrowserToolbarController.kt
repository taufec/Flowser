package com.flowser.app.browser

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView

internal object BrowserToolbarPolicy {
    private const val COMPACT_BREAKPOINT_DP = 420

    fun isCompact(windowWidthPx: Int, density: Float): Boolean =
        windowWidthPx / density.coerceAtLeast(0.1f) < COMPACT_BREAKPOINT_DP

    fun reloadLabel(isLoading: Boolean): String = if (isLoading) "Stop" else "Reload"

    fun maximizeLabel(isMaximized: Boolean): String =
        if (isMaximized) "Restore" else "Maximize"
}

interface BrowserWindowActions {
    fun minimize()
    fun toggleMaximize()
    fun close()
    fun editAddress(currentUrl: String)
    fun openExternally(url: String)
    fun copyUrl(url: String)
    fun shareUrl(url: String)
}

class BrowserToolbarController(
    private val context: Context,
    private val browser: BrowserSessionController,
    private val actions: BrowserWindowActions
) {
    val view: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.rgb(31, 36, 48))
        setPadding(dp(4), 0, dp(4), 0)
    }

    private val titleView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        isSingleLine = true
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), 0, dp(10), 0)
        contentDescription = "Current website. Tap to edit address."
        setOnClickListener { actions.editAddress(lastState.url) }
    }

    val dragSurface: View
        get() = titleView

    private var lastState = BrowserUiState(
        url = "https://example.com",
        title = "Flowser",
        isLoading = false,
        canGoBack = false,
        canGoForward = false,
        desktopMode = false,
        zoomPercent = 100
    )
    private var lastWidthPx = 0
    private var isMaximized = false

    fun render(
        state: BrowserUiState,
        windowWidthPx: Int,
        maximized: Boolean = isMaximized
    ) {
        lastState = state
        lastWidthPx = windowWidthPx
        isMaximized = maximized
        rebuild()
    }

    private fun rebuild() {
        view.removeAllViews()
        val compact = BrowserToolbarPolicy.isCompact(
            lastWidthPx.coerceAtLeast(1),
            context.resources.displayMetrics.density
        )

        view.addView(toolbarButton("‹", "Back") {
            browser.goBack()
        }.apply { isEnabled = lastState.canGoBack })

        if (!compact) {
            view.addView(toolbarButton("›", "Forward") {
                browser.goForward()
            }.apply { isEnabled = lastState.canGoForward })
        }

        titleView.text = lastState.title.ifBlank {
            runCatching { Uri.parse(lastState.url).host }.getOrNull() ?: "Flowser"
        }
        titleView.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
        view.addView(titleView)

        view.addView(toolbarButton(
            if (lastState.isLoading) "■" else "↻",
            BrowserToolbarPolicy.reloadLabel(lastState.isLoading)
        ) {
            if (lastState.isLoading) browser.stopLoading() else browser.reload()
        })

        view.addView(toolbarButton("_", "Minimize") { actions.minimize() })

        if (!compact) {
            view.addView(toolbarButton(
                if (isMaximized) "❐" else "□",
                BrowserToolbarPolicy.maximizeLabel(isMaximized)
            ) { actions.toggleMaximize() })
        }

        view.addView(toolbarButton("⋮", "Browser menu") { anchor ->
            showMenu(anchor)
        })

        if (!compact) {
            view.addView(toolbarButton("×", "Close Flowser") { actions.close() })
        }
    }

    private fun showMenu(anchor: View) {
        val menu = PopupMenu(context, anchor)
        menu.menu.apply {
            add(0, ITEM_EDIT_ADDRESS, 0, "Edit address")
            add(0, ITEM_BACK, 1, "Back").isEnabled = lastState.canGoBack
            add(0, ITEM_FORWARD, 2, "Forward").isEnabled = lastState.canGoForward
            add(0, ITEM_RELOAD, 3, BrowserToolbarPolicy.reloadLabel(lastState.isLoading))
            add(0, ITEM_HOME, 4, "Home")
            add(0, ITEM_MINIMIZE, 5, "Minimize")
            add(0, ITEM_MAXIMIZE, 6, BrowserToolbarPolicy.maximizeLabel(isMaximized))
            add(0, ITEM_OPEN_EXTERNAL, 7, "Open in default browser")
            add(0, ITEM_COPY, 8, "Copy URL")
            add(0, ITEM_SHARE, 9, "Share URL")
            add(0, ITEM_DESKTOP, 10, if (lastState.desktopMode) "Use mobile site" else "Use desktop site")
                .isCheckable = true
            findItem(ITEM_DESKTOP)?.isChecked = lastState.desktopMode
            add(0, ITEM_ZOOM_IN, 11, "Zoom in (${lastState.zoomPercent}%)")
                .isEnabled = lastState.zoomPercent < 200
            add(0, ITEM_ZOOM_OUT, 12, "Zoom out (${lastState.zoomPercent}%)")
                .isEnabled = lastState.zoomPercent > 50
            add(0, ITEM_ZOOM_RESET, 13, "Reset zoom")
                .isEnabled = lastState.zoomPercent != 100
            add(0, ITEM_CLOSE, 14, "Close Flowser")
        }
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                ITEM_EDIT_ADDRESS -> actions.editAddress(lastState.url)
                ITEM_BACK -> browser.goBack()
                ITEM_FORWARD -> browser.goForward()
                ITEM_RELOAD -> if (lastState.isLoading) browser.stopLoading() else browser.reload()
                ITEM_HOME -> browser.goHome()
                ITEM_MINIMIZE -> actions.minimize()
                ITEM_MAXIMIZE -> actions.toggleMaximize()
                ITEM_OPEN_EXTERNAL -> actions.openExternally(lastState.url)
                ITEM_COPY -> actions.copyUrl(lastState.url)
                ITEM_SHARE -> actions.shareUrl(lastState.url)
                ITEM_DESKTOP -> browser.setDesktopMode(!lastState.desktopMode)
                ITEM_ZOOM_IN -> browser.zoomIn()
                ITEM_ZOOM_OUT -> browser.zoomOut()
                ITEM_ZOOM_RESET -> browser.resetZoom()
                ITEM_CLOSE -> actions.close()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        menu.show()
    }

    private fun toolbarButton(
        label: String,
        description: String,
        action: (View) -> Unit
    ): Button = Button(context).apply {
        text = label
        contentDescription = description
        textSize = 20f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        isAllCaps = false
        minWidth = dp(48)
        minimumWidth = dp(48)
        minHeight = dp(48)
        minimumHeight = dp(48)
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        setOnClickListener { action(it) }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val ITEM_EDIT_ADDRESS = 1
        private const val ITEM_BACK = 2
        private const val ITEM_FORWARD = 3
        private const val ITEM_RELOAD = 4
        private const val ITEM_HOME = 5
        private const val ITEM_MINIMIZE = 6
        private const val ITEM_MAXIMIZE = 7
        private const val ITEM_OPEN_EXTERNAL = 8
        private const val ITEM_COPY = 9
        private const val ITEM_SHARE = 10
        private const val ITEM_DESKTOP = 11
        private const val ITEM_ZOOM_IN = 12
        private const val ITEM_ZOOM_OUT = 13
        private const val ITEM_ZOOM_RESET = 14
        private const val ITEM_CLOSE = 15
    }
}
