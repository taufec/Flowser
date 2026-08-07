package com.flowser.app.window

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.hypot

internal object BubbleGesturePolicy {
    private const val TAP_THRESHOLD_DP = 8

    fun isTap(deltaX: Int, deltaY: Int, density: Float): Boolean {
        val threshold = (TAP_THRESHOLD_DP * density.coerceAtLeast(0.1f)).toInt()
        return abs(deltaX) < threshold && abs(deltaY) < threshold
    }
}

interface BubbleListener {
    fun onRestoreRequested()
    fun onCloseRequested()
    fun onBubblePositionChanged(x: Int, y: Int)
}

class BubbleController(
    private val context: Context,
    private val repository: WindowStateRepository,
    private val listener: BubbleListener
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val bubble = FrameLayout(context)
    private val faviconView = ImageView(context)
    private val fallbackView = TextView(context)
    private val closeTarget = TextView(context)

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var closeParams: WindowManager.LayoutParams? = null
    private var bubbleAttached = false
    private var closeAttached = false
    private var currentX = 0
    private var currentY = 100
    private var longPressTriggered = false
    private var snapAnimator: ValueAnimator? = null

    init {
        buildBubble()
        buildCloseTarget()
        installGestureHandler()
    }

    fun show(state: BrowserWindowState, favicon: Bitmap? = null) {
        updateFavicon(favicon)
        val area = displayArea()
        val point = WindowGeometryEngine.snapBubble(
            state.bubbleX,
            state.bubbleY,
            bubbleSize(),
            area.size
        )
        currentX = point.x
        currentY = point.y
        if (bubbleAttached) {
            updateBubbleLayout(area)
            return
        }

        val params = WindowManager.LayoutParams(
            bubbleSize(),
            bubbleSize(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX + area.offsetX
            y = currentY + area.offsetY
        }
        bubbleParams = params
        try {
            windowManager.addView(bubble, params)
            bubbleAttached = true
        } catch (_: Exception) {
            bubbleParams = null
            listener.onCloseRequested()
        }
    }

    fun updateFavicon(favicon: Bitmap?) {
        when (BubbleIconPolicy.mode(favicon != null)) {
            BubbleIconMode.FAVICON -> {
                faviconView.setImageBitmap(favicon)
                faviconView.visibility = View.VISIBLE
                fallbackView.visibility = View.GONE
            }

            BubbleIconMode.FALLBACK -> {
                faviconView.setImageDrawable(null)
                faviconView.visibility = View.GONE
                fallbackView.visibility = View.VISIBLE
            }
        }
    }

    fun hide() {
        cancelSnapAnimation()
        hideCloseTarget()
        detachBubble()
    }

    fun onConfigurationChanged() {
        if (!bubbleAttached) return
        val area = displayArea()
        val snapped = WindowGeometryEngine.snapBubble(
            currentX,
            currentY,
            bubbleSize(),
            area.size
        )
        currentX = snapped.x
        currentY = snapped.y
        updateBubbleLayout(area)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        cancelSnapAnimation()
        hideCloseTarget()
        detachBubble()
        faviconView.setImageDrawable(null)
    }

    private fun detachBubble() {
        if (bubbleAttached) {
            runCatching { windowManager.removeViewImmediate(bubble) }
            bubbleAttached = false
        }
        bubbleParams = null
    }

    private fun buildBubble() {
        bubble.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.rgb(31, 36, 48))
            setStroke(dp(2), Color.WHITE)
        }
        bubble.elevation = dp(10).toFloat()
        bubble.contentDescription = "Flowser minimized browser"
        bubble.clipToOutline = true

        faviconView.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            contentDescription = "Website favicon"
        }
        bubble.addView(
            faviconView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(5), dp(5), dp(5), dp(5))
            }
        )

        fallbackView.apply {
            text = "F"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = false
        }
        bubble.addView(
            fallbackView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun buildCloseTarget() {
        closeTarget.text = "×"
        closeTarget.textSize = 34f
        closeTarget.gravity = Gravity.CENTER
        closeTarget.setTextColor(Color.WHITE)
        closeTarget.background = closeTargetBackground(false)
        closeTarget.elevation = dp(12).toFloat()
    }

    private fun installGestureHandler() {
        val longPressRunnable = Runnable {
            longPressTriggered = true
            hideCloseTarget()
            showLongPressMenu()
        }

        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var startLayoutX = 0
            private var startLayoutY = 0
            private var startTouchX = 0f
            private var startTouchY = 0f
            private var dragging = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                val area = displayArea()
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelSnapAnimation()
                        startLayoutX = currentX
                        startLayoutY = currentY
                        startTouchX = event.rawX
                        startTouchY = event.rawY
                        dragging = false
                        longPressTriggered = false
                        handler.postDelayed(
                            longPressRunnable,
                            ViewConfiguration.getLongPressTimeout().toLong()
                        )
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - startTouchX).toInt()
                        val dy = (event.rawY - startTouchY).toInt()
                        if (!dragging && !BubbleGesturePolicy.isTap(dx, dy, density())) {
                            dragging = true
                            handler.removeCallbacks(longPressRunnable)
                            showCloseTarget(area)
                        }
                        if (dragging) {
                            currentX = (startLayoutX + dx).coerceIn(
                                0,
                                (area.size.width - bubbleSize()).coerceAtLeast(0)
                            )
                            currentY = (startLayoutY + dy).coerceIn(
                                0,
                                (area.size.height - bubbleSize()).coerceAtLeast(0)
                            )
                            updateBubbleLayout(area)
                            setCloseTargetActive(isInsideCloseTarget(area))
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        if (dragging) {
                            if (isInsideCloseTarget(area)) {
                                hideCloseTarget()
                                listener.onCloseRequested()
                            } else {
                                hideCloseTarget()
                                animateToSnap(area)
                            }
                        } else if (!longPressTriggered && event.actionMasked == MotionEvent.ACTION_UP) {
                            view.performClick()
                            listener.onRestoreRequested()
                        }
                    }
                }
                return true
            }
        })
    }

    private fun showLongPressMenu() {
        if (!bubbleAttached) return
        PopupMenu(context, bubble).apply {
            menu.add(0, MENU_RESTORE, 0, "Restore")
            menu.add(0, MENU_CLOSE, 1, "Close")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_RESTORE -> listener.onRestoreRequested()
                    MENU_CLOSE -> listener.onCloseRequested()
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    private fun showCloseTarget(area: DisplayArea) {
        if (closeAttached) return
        val size = closeTargetSize()
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = area.offsetX + (area.size.width - size) / 2
            y = area.offsetY + area.size.height - size - dp(24)
        }
        closeParams = params
        try {
            windowManager.addView(closeTarget, params)
            closeAttached = true
        } catch (_: Exception) {
            closeParams = null
        }
    }

    private fun hideCloseTarget() {
        if (closeAttached) {
            runCatching { windowManager.removeViewImmediate(closeTarget) }
            closeAttached = false
        }
        closeParams = null
        setCloseTargetActive(false)
    }

    private fun setCloseTargetActive(active: Boolean) {
        closeTarget.background = closeTargetBackground(active)
        closeTarget.scaleX = if (active) 1.15f else 1f
        closeTarget.scaleY = if (active) 1.15f else 1f
    }

    private fun closeTargetBackground(active: Boolean): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (active) Color.rgb(211, 47, 47) else Color.argb(220, 75, 82, 96))
        setStroke(dp(2), Color.WHITE)
    }

    private fun isInsideCloseTarget(area: DisplayArea): Boolean {
        if (!closeAttached) return false
        val targetX = (area.size.width - closeTargetSize()) / 2
        val targetY = area.size.height - closeTargetSize() - dp(24)
        val bubbleCenterX = currentX + bubbleSize() / 2f
        val bubbleCenterY = currentY + bubbleSize() / 2f
        val targetCenterX = targetX + closeTargetSize() / 2f
        val targetCenterY = targetY + closeTargetSize() / 2f
        return hypot(
            (bubbleCenterX - targetCenterX).toDouble(),
            (bubbleCenterY - targetCenterY).toDouble()
        ) <= closeTargetSize() / 2.0
    }

    private fun animateToSnap(area: DisplayArea) {
        val destination = WindowGeometryEngine.snapBubble(
            currentX,
            currentY,
            bubbleSize(),
            area.size
        )
        val startX = currentX
        val startY = currentY
        cancelSnapAnimation()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 160L
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                currentX = (startX + (destination.x - startX) * fraction).toInt()
                currentY = (startY + (destination.y - startY) * fraction).toInt()
                updateBubbleLayout(area)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    currentX = destination.x
                    currentY = destination.y
                    updateBubbleLayout(area)
                    repository.saveBubblePosition(currentX, currentY)
                    listener.onBubblePositionChanged(currentX, currentY)
                    snapAnimator = null
                }
            })
            start()
        }
    }

    private fun cancelSnapAnimation() {
        val animator = snapAnimator
        snapAnimator = null
        animator?.cancel()
    }

    private fun updateBubbleLayout(area: DisplayArea = displayArea()) {
        val params = bubbleParams ?: return
        params.x = currentX + area.offsetX
        params.y = currentY + area.offsetY
        if (bubbleAttached) runCatching { windowManager.updateViewLayout(bubble, params) }
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
                RectSize(
                    (bounds.width() - insets.left - insets.right).coerceAtLeast(1),
                    (bounds.height() - insets.top - insets.bottom).coerceAtLeast(1)
                ),
                insets.left,
                insets.top
            )
        }
        val metrics = context.resources.displayMetrics
        return DisplayArea(
            RectSize(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1)),
            0,
            0
        )
    }

    private fun bubbleSize(): Int = dp(56)
    private fun closeTargetSize(): Int = dp(72)
    private fun density(): Float = context.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density()).toInt()

    private data class DisplayArea(
        val size: RectSize,
        val offsetX: Int,
        val offsetY: Int
    )

    companion object {
        private const val MENU_RESTORE = 1
        private const val MENU_CLOSE = 2
    }
}
