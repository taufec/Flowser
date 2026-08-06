package com.flowser.app.window

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowGeometryEngineTest {
    @Test
    fun default_geometry_is_centered_and_within_phone_bounds() {
        val result = WindowGeometryEngine.defaultGeometry(
            bounds = RectSize(width = 400, height = 800),
            density = 1f
        )

        assertEquals(WindowGeometry(x = 16, y = 120, width = 368, height = 560), result)
    }

    @Test
    fun default_geometry_uses_larger_tablet_size_without_exceeding_bounds() {
        val result = WindowGeometryEngine.defaultGeometry(
            bounds = RectSize(width = 1200, height = 800),
            density = 1f
        )

        assertEquals(WindowGeometry(x = 168, y = 112, width = 864, height = 576), result)
    }

    @Test
    fun clamp_enforces_280dp_by_320dp_minimum() {
        val result = WindowGeometryEngine.clampWindow(
            geometry = WindowGeometry(x = 20, y = 30, width = 100, height = 120),
            bounds = RectSize(width = 800, height = 1600),
            density = 2f
        )

        assertEquals(WindowGeometry(x = 20, y = 30, width = 560, height = 640), result)
    }

    @Test
    fun clamp_keeps_toolbar_reachable() {
        val result = WindowGeometryEngine.clampWindow(
            geometry = WindowGeometry(x = 700, y = 790, width = 400, height = 500),
            bounds = RectSize(width = 1000, height = 800),
            density = 1f
        )

        assertEquals(600, result.x)
        assertEquals(752, result.y)
    }

    @Test
    fun resize_grows_from_bottom_right() {
        val result = WindowGeometryEngine.resizeFromBottomRight(
            start = WindowGeometry(x = 10, y = 20, width = 300, height = 400),
            deltaX = 100,
            deltaY = 150,
            bounds = RectSize(width = 1000, height = 1000),
            density = 1f
        )

        assertEquals(WindowGeometry(x = 10, y = 20, width = 400, height = 550), result)
    }

    @Test
    fun resize_stops_at_display_edges() {
        val result = WindowGeometryEngine.resizeFromBottomRight(
            start = WindowGeometry(x = 100, y = 100, width = 300, height = 300),
            deltaX = 2000,
            deltaY = 2000,
            bounds = RectSize(width = 1000, height = 800),
            density = 1f
        )

        assertEquals(WindowGeometry(x = 100, y = 100, width = 900, height = 700), result)
    }

    @Test
    fun maximize_fills_usable_bounds() {
        assertEquals(
            WindowGeometry(x = 0, y = 0, width = 1280, height = 720),
            WindowGeometryEngine.maximizedGeometry(RectSize(width = 1280, height = 720))
        )
    }

    @Test
    fun bubble_snaps_left_when_center_is_left_of_screen_center() {
        val result = WindowGeometryEngine.snapBubble(
            x = 100,
            y = 300,
            bubbleSizePx = 56,
            bounds = RectSize(width = 1000, height = 800)
        )

        assertEquals(IntPoint(x = 0, y = 300), result)
    }

    @Test
    fun bubble_snaps_right_when_center_is_right_of_screen_center() {
        val result = WindowGeometryEngine.snapBubble(
            x = 800,
            y = 300,
            bubbleSizePx = 56,
            bounds = RectSize(width = 1000, height = 800)
        )

        assertEquals(IntPoint(x = 944, y = 300), result)
    }

    @Test
    fun bubble_y_is_clamped_inside_bounds() {
        val top = WindowGeometryEngine.snapBubble(
            x = 10,
            y = -100,
            bubbleSizePx = 56,
            bounds = RectSize(width = 1000, height = 800)
        )
        val bottom = WindowGeometryEngine.snapBubble(
            x = 900,
            y = 900,
            bubbleSizePx = 56,
            bounds = RectSize(width = 1000, height = 800)
        )

        assertEquals(0, top.y)
        assertEquals(744, bottom.y)
    }
}
