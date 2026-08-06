package com.flowser.app.window

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WindowGeometryEngine {
    private const val MIN_WIDTH_DP = 280
    private const val MIN_HEIGHT_DP = 320
    private const val DEFAULT_PHONE_WIDTH_RATIO = 0.92f
    private const val DEFAULT_PHONE_HEIGHT_RATIO = 0.70f
    private const val DEFAULT_TABLET_WIDTH_RATIO = 0.72f
    private const val DEFAULT_TABLET_HEIGHT_RATIO = 0.72f
    private const val TABLET_BREAKPOINT_DP = 600

    fun defaultGeometry(bounds: RectSize, density: Float): WindowGeometry {
        val safeDensity = density.coerceAtLeast(0.1f)
        val isTablet = bounds.width / safeDensity >= TABLET_BREAKPOINT_DP
        val widthRatio = if (isTablet) DEFAULT_TABLET_WIDTH_RATIO else DEFAULT_PHONE_WIDTH_RATIO
        val heightRatio = if (isTablet) DEFAULT_TABLET_HEIGHT_RATIO else DEFAULT_PHONE_HEIGHT_RATIO
        val width = (bounds.width * widthRatio).roundToInt()
        val height = (bounds.height * heightRatio).roundToInt()
        val centered = WindowGeometry(
            x = (bounds.width - width) / 2,
            y = (bounds.height - height) / 2,
            width = width,
            height = height
        )
        return clampWindow(centered, bounds, safeDensity)
    }

    fun clampWindow(
        geometry: WindowGeometry,
        bounds: RectSize,
        density: Float
    ): WindowGeometry {
        val safeWidth = bounds.width.coerceAtLeast(0)
        val safeHeight = bounds.height.coerceAtLeast(0)
        val minimumWidth = min(dp(MIN_WIDTH_DP, density), safeWidth)
        val minimumHeight = min(dp(MIN_HEIGHT_DP, density), safeHeight)
        val width = geometry.width.coerceIn(minimumWidth, safeWidth)
        val height = geometry.height.coerceIn(minimumHeight, safeHeight)
        val maxX = max(0, safeWidth - width)
        val maxY = max(0, safeHeight - height)

        return WindowGeometry(
            x = geometry.x.coerceIn(0, maxX),
            y = geometry.y.coerceIn(0, maxY),
            width = width,
            height = height
        )
    }

    fun resizeFromBottomRight(
        start: WindowGeometry,
        deltaX: Int,
        deltaY: Int,
        bounds: RectSize,
        density: Float
    ): WindowGeometry {
        val safeWidth = bounds.width.coerceAtLeast(0)
        val safeHeight = bounds.height.coerceAtLeast(0)
        val minimumWidth = min(dp(MIN_WIDTH_DP, density), safeWidth)
        val minimumHeight = min(dp(MIN_HEIGHT_DP, density), safeHeight)
        val x = start.x.coerceIn(0, max(0, safeWidth - minimumWidth))
        val y = start.y.coerceIn(0, max(0, safeHeight - minimumHeight))
        val maximumWidth = max(minimumWidth, safeWidth - x)
        val maximumHeight = max(minimumHeight, safeHeight - y)
        val width = (start.width + deltaX).coerceIn(minimumWidth, maximumWidth)
        val height = (start.height + deltaY).coerceIn(minimumHeight, maximumHeight)

        return WindowGeometry(
            x = x,
            y = y,
            width = width,
            height = height
        )
    }

    fun maximizedGeometry(bounds: RectSize): WindowGeometry = WindowGeometry(
        x = 0,
        y = 0,
        width = bounds.width.coerceAtLeast(0),
        height = bounds.height.coerceAtLeast(0)
    )

    fun snapBubble(
        x: Int,
        y: Int,
        bubbleSizePx: Int,
        bounds: RectSize
    ): IntPoint {
        val safeBubbleSize = bubbleSizePx.coerceAtLeast(0)
        val maxX = max(0, bounds.width - safeBubbleSize)
        val maxY = max(0, bounds.height - safeBubbleSize)
        val snappedX = if (x + safeBubbleSize / 2 < bounds.width / 2) 0 else maxX
        return IntPoint(
            x = snappedX,
            y = y.coerceIn(0, maxY)
        )
    }

    private fun dp(value: Int, density: Float): Int =
        (value * density.coerceAtLeast(0.1f)).roundToInt()
}
