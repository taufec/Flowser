package com.flowser.app.window

enum class WindowMode {
    WINDOWED,
    MAXIMIZED,
    MINIMIZED,
    CLOSED
}

data class RectSize(
    val width: Int,
    val height: Int
)

data class IntPoint(
    val x: Int,
    val y: Int
)

data class WindowGeometry(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class BrowserWindowState(
    val mode: WindowMode,
    val geometry: WindowGeometry,
    val lastNormalGeometry: WindowGeometry,
    val preMinimizeMode: WindowMode,
    val bubbleX: Int,
    val bubbleY: Int,
    val currentUrl: String,
    val desktopMode: Boolean,
    val zoomPercent: Int
)
