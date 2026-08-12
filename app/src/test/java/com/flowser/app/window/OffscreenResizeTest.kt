package com.flowser.app.window

import org.junit.Assert.assertEquals
import org.junit.Test

class OffscreenResizeTest {
    @Test
    fun bottom_right_resize_preserves_partially_offscreen_left_position() {
        val result = WindowGeometryEngine.resizeFromBottomRight(
            start = WindowGeometry(x = -100, y = 100, width = 400, height = 400),
            deltaX = 50,
            deltaY = 0,
            bounds = RectSize(width = 1000, height = 900),
            density = 1f
        )

        assertEquals(WindowGeometry(x = -100, y = 100, width = 450, height = 400), result)
    }
}
