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
