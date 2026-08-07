package com.flowser.app.window

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.flowser.app.browser.BrowserSessionController
import com.flowser.app.browser.BrowserToolbarController
import com.flowser.app.browser.BrowserUiState
import kotlin.math.abs

interface FloatingWindowListener {
    fun onMinimizeRequested()
    fun onCloseRequested()
    fun onGeometryChanged(geometry: WindowGeometry)
    fun onModeChanged(mode: WindowMode)
}

class FloatingWindowController(
    private val context: Context,
    private val browser: BrowserSessionController,
    private val toolbar: BrowserToolbarController,
    private val repository: WindowStateRepository,
    private val listener: FloatingWindowListener
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val root = OutsideAwareFrameLayout(context) { releaseBrowserInputFocus() }
    private val content = LinearLayout(context)
    private val resizeHandleLeft = View(context)
    private val resizeHandleRight = View(context)

    private var layoutParams: WindowManager.LayoutParams? = null
    private var attached = false
    private var currentGeometry = WindowGeometry(0, 0, 300, 400)
    private var lastNormalGeometry = currentGeometry
    private var mode = WindowMode.WINDOWED
    private var lastBrowserState = browser.currentState()
    private var inputState = OverlayInputPolicy.afterOutsideTouch()

    init {
        buildViewHierarchy()
        installInputFocusHandoff()
        installDragHandler()
        installResizeHandlers()
    }

    fun show(state: BrowserWindowState) {
        val area = displayArea()
        mode = if (state.mode == WindowMode.MAXIMIZED) {
            WindowMode.MAXIMIZED
        } else {
            WindowMode.WINDOWED
        }
        lastNormalGeometry = WindowGeometryEngine.clampPartiallyVisibleWindow(
            state.lastNormalGeometry,
            area.size,
            density(),
            toolbar.heightPx
        )
        currentGeometry = if (mode == WindowMode.MAXIMIZED) {
            WindowGeometryEngine.maximizedGeometry(area.size)
        } else {
            WindowGeometryEngine.clampPartiallyVisibleWindow(
                state.geometry,
                area.size,
                density(),
                toolbar.heightPx
            )
        }
        setResizeHandlesVisible(mode == WindowMode.WINDOWED)
        toolbar.render(lastBrowserState, currentGeometry.width, mode == WindowMode.MAXIMIZED)

        if (!attached) {
            val params = createLayoutParams(currentGeometry, area)
            layoutParams = params
            inputState = OverlayInputPolicy.afterOutsideTouch()
            try {
                windowManager.addView(root, params)
                attached = true
            } catch (_: Exception) {
                layoutParams = null
                listener.onCloseRequested()
            }
        } else {
            applyGeometry(currentGeometry, area)
            applyInputFocusState()
        }
    }

    fun hidePreservingBrowser() {
        releaseBrowserInputFocus()
        if (attached) {
            runCatching { windowManager.removeViewImmediate(root) }
            attached = false
            layoutParams = null
        }
    }

    fun updateToolbar(state: BrowserUiState) {
        lastBrowserState = state
        toolbar.render(state, currentGeometry.width, mode == WindowMode.MAXIMIZED)
    }

    fun toggleMaximize() {
        val area = displayArea()
        if (mode == WindowMode.MAXIMIZED) {
            mode = WindowMode.WINDOWED
            currentGeometry = WindowGeometryEngine.clampPartiallyVisibleWindow(
                lastNormalGeometry,
                area.size,
                density(),
                toolbar.heightPx
            )
            setResizeHandlesVisible(true)
        } else {
            lastNormalGeometry = currentGeometry
            mode = WindowMode.MAXIMIZED
            currentGeometry = WindowGeometryEngine.maximizedGeometry(area.size)
            setResizeHandlesVisible(false)
        }
        applyGeometry(currentGeometry, area)
        toolbar.render(lastBrowserState, currentGeometry.width, mode == WindowMode.MAXIMIZED)
        repository.saveGeometry(currentGeometry)
        listener.onGeometryChanged(currentGeometry)
        listener.onModeChanged(mode)
    }

    fun onConfigurationChanged() {
        val area = displayArea()
        lastNormalGeometry = WindowGeometryEngine.clampPartiallyVisibleWindow(
            lastNormalGeometry,
            area.size,
            density(),
            toolbar.heightPx
        )
        currentGeometry = if (mode == WindowMode.MAXIMIZED) {
            WindowGeometryEngine.maximizedGeometry(area.size)
        } else {
            WindowGeometryEngine.clampPartiallyVisibleWindow(
                currentGeometry,
                area.size,
                density(),
                toolbar.heightPx
            )
        }
        applyGeometry(currentGeometry, area)
        toolbar.render(lastBrowserState, currentGeometry.width, mode == WindowMode.MAXIMIZED)
    }

    fun currentMode(): WindowMode = mode

    fun currentGeometry(): WindowGeometry = currentGeometry

    fun normalGeometry(): WindowGeometry = lastNormalGeometry

    fun destroy() {
        releaseBrowserInputFocus()
        if (attached) {
            runCatching { windowManager.removeViewImmediate(root) }
            attached = false
        }
        layoutParams = null
    }

    private fun buildViewHierarchy() {
        root.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(1), Color.rgb(50, 56, 68))
        }
        root.elevation = dp(12).toFloat()
        root.clipToOutline = true

        content.orientation = LinearLayout.VERTICAL
        content.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        toolbar.view.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            toolbar.heightPx
        )
        browser.view.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        content.addView(toolbar.view)
        content.addView(browser.view)
        root.addView(content)

        configureResizeHandle(resizeHandleLeft, "Resize Flowser from bottom-left corner")
        configureResizeHandle(resizeHandleRight, "Resize Flowser from bottom-right corner")
        val handleSize = dp(ResizeHandlePolicy.touchTargetDp())
        resizeHandleLeft.layoutParams = FrameLayout.LayoutParams(
            handleSize,
            handleSize,
            Gravity.START or Gravity.BOTTOM
        )
        resizeHandleRight.layoutParams = FrameLayout.LayoutParams(
            handleSize,
            handleSize,
            Gravity.END or Gravity.BOTTOM
        )
        root.addView(resizeHandleLeft)
        root.addView(resizeHandleRight)
    }

    private fun configureResizeHandle(handle: View, description: String) {
        handle.contentDescription = description
        handle.alpha = ResizeHandlePolicy.visualAlpha()
        handle.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setResizeHandlesVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        resizeHandleLeft.visibility = visibility
        resizeHandleRight.visibility = visibility
    }

    private fun installInputFocusHandoff() {
        browser.view.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                acquireBrowserInputFocus()
            }
            false
        }
    }

    private fun acquireBrowserInputFocus() {
        inputState = OverlayInputPolicy.afterBrowserTouch()
        applyInputFocusState()
        browser.view.post {
            if (attached && inputState.browserOwnsKeyboard) {
                browser.view.requestFocus()
            }
        }
    }

    private fun releaseBrowserInputFocus() {
        inputState = OverlayInputPolicy.afterOutsideTouch()
        browser.view.clearFocus()
        val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
        runCatching {
            inputMethodManager?.hideSoftInputFromWindow(root.windowToken, 0)
        }
        applyInputFocusState()
    }

    private fun applyInputFocusState() {
        val params = layoutParams ?: return
        val oldFlags = params.flags
        params.flags = if (inputState.browserOwnsKeyboard) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (attached && oldFlags != params.flags) {
            runCatching { windowManager.updateViewLayout(root, params) }
        }
    }

    private fun installDragHandler() {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    toolbar.dragSurface.performClick()
                    return true
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    toggleMaximize()
                    return true
                }
            }
        )

        toolbar.dragSurface.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var dragging = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(event)
                if (mode == WindowMode.MAXIMIZED) return true
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = currentGeometry.x
                        startY = currentGeometry.y
                        touchX = event.rawX
                        touchY = event.rawY
                        dragging = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                        if (dragging) {
                            val area = displayArea()
                            currentGeometry = WindowGeometryEngine.clampDraggedWindow(
                                currentGeometry.copy(x = startX + dx, y = startY + dy),
                                area.size,
                                toolbar.heightPx
                            )
                            applyGeometry(currentGeometry, area)
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragging) persistCurrentGeometry()
                    }
                }
                return true
            }
        })
    }

    private fun installResizeHandlers() {
        installResizeHandler(resizeHandleLeft, ResizeCorner.BOTTOM_LEFT)
        installResizeHandler(resizeHandleRight, ResizeCorner.BOTTOM_RIGHT)
    }

    private fun installResizeHandler(handle: View, corner: ResizeCorner) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        handle.setOnTouchListener(object : View.OnTouchListener {
            private var startGeometry = currentGeometry
            private var touchX = 0f
            private var touchY = 0f
            private var resizing = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                if (mode != WindowMode.WINDOWED) return false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startGeometry = currentGeometry
                        touchX = event.rawX
                        touchY = event.rawY
                        resizing = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (!resizing && (abs(dx) > slop || abs(dy) > slop)) resizing = true
                        if (resizing) {
                            val area = displayArea()
                            currentGeometry = when (corner) {
                                ResizeCorner.BOTTOM_LEFT -> WindowGeometryEngine.resizeFromBottomLeft(
                                    startGeometry,
                                    dx,
                                    dy,
                                    area.size,
                                    density()
                                )

                                ResizeCorner.BOTTOM_RIGHT -> WindowGeometryEngine.resizeFromBottomRight(
                                    startGeometry,
                                    dx,
                                    dy,
                                    area.size,
                                    density()
                                )
                            }
                            lastNormalGeometry = currentGeometry
                            applyGeometry(currentGeometry, area)
                            toolbar.render(lastBrowserState, currentGeometry.width, false)
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (resizing) persistCurrentGeometry()
                        view.performClick()
                    }
                }
                return true
            }
        })
    }

    private fun persistCurrentGeometry() {
        lastNormalGeometry = currentGeometry
        repository.saveGeometry(currentGeometry)
        listener.onGeometryChanged(currentGeometry)
    }

    private fun createLayoutParams(
        geometry: WindowGeometry,
        area: DisplayArea
    ): WindowManager.LayoutParams = WindowManager.LayoutParams(
        geometry.width,
        geometry.height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = geometry.x + area.offsetX
        y = geometry.y + area.offsetY
    }

    private fun applyGeometry(geometry: WindowGeometry, area: DisplayArea = displayArea()) {
        val params = layoutParams ?: return
        params.width = geometry.width
        params.height = geometry.height
        params.x = geometry.x + area.offsetX
        params.y = geometry.y + area.offsetY
        if (attached) runCatching { windowManager.updateViewLayout(root, params) }
    }

    @Suppress("DEPRECATION")
    private fun displayArea(): DisplayArea {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            val bounds = metrics.bounds
            return DisplayArea(
                size = RectSize(
                    width = (bounds.width() - insets.left - insets.right).coerceAtLeast(1),
                    height = (bounds.height() - insets.top - insets.bottom).coerceAtLeast(1)
                ),
                offsetX = insets.left,
                offsetY = insets.top
            )
        }
        val metrics = context.resources.displayMetrics
        return DisplayArea(
            size = RectSize(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1)),
            offsetX = 0,
            offsetY = 0
        )
    }

    private fun density(): Float = context.resources.displayMetrics.density

    private fun dp(value: Int): Int = (value * density()).toInt()

    private data class DisplayArea(
        val size: RectSize,
        val offsetX: Int,
        val offsetY: Int
    )

    private enum class ResizeCorner {
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private class OutsideAwareFrameLayout(
        context: Context,
        private val onOutsideTouch: () -> Unit
    ) : FrameLayout(context) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                onOutsideTouch()
                return true
            }
            return super.dispatchTouchEvent(event)
        }
    }
}
