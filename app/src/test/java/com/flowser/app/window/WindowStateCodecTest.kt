package com.flowser.app.window

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowStateCodecTest {
    @Test
    fun encoded_state_round_trips_all_values() {
        val original = state(
            mode = WindowMode.MINIMIZED,
            preMinimizeMode = WindowMode.MAXIMIZED,
            currentUrl = "https://ticktick.com",
            desktopMode = true,
            zoomPercent = 130
        )

        val decoded = WindowStateCodec.decode(
            WindowStateCodec.encode(original),
            fallback = state()
        )

        assertEquals(original, decoded)
    }

    @Test
    fun invalid_mode_falls_back_to_windowed() {
        val values = WindowStateCodec.encode(state()).toMutableMap().apply {
            this[WindowStateCodec.KEY_MODE] = "BROKEN"
        }

        val decoded = WindowStateCodec.decode(values, fallback = state())

        assertEquals(WindowMode.WINDOWED, decoded.mode)
    }

    @Test
    fun invalid_pre_minimize_mode_falls_back_to_windowed() {
        val values = WindowStateCodec.encode(state()).toMutableMap().apply {
            this[WindowStateCodec.KEY_PRE_MINIMIZE_MODE] = "CLOSED"
        }

        val decoded = WindowStateCodec.decode(values, fallback = state())

        assertEquals(WindowMode.WINDOWED, decoded.preMinimizeMode)
    }

    @Test
    fun zoom_is_clamped_to_supported_range() {
        val low = WindowStateCodec.decode(
            WindowStateCodec.encode(state()).toMutableMap().apply {
                this[WindowStateCodec.KEY_ZOOM_PERCENT] = "20"
            },
            fallback = state()
        )
        val high = WindowStateCodec.decode(
            WindowStateCodec.encode(state()).toMutableMap().apply {
                this[WindowStateCodec.KEY_ZOOM_PERCENT] = "900"
            },
            fallback = state()
        )

        assertEquals(50, low.zoomPercent)
        assertEquals(200, high.zoomPercent)
    }

    @Test
    fun blank_url_uses_fallback_url() {
        val fallback = state(currentUrl = "https://fallback.example")
        val values = WindowStateCodec.encode(state()).toMutableMap().apply {
            this[WindowStateCodec.KEY_CURRENT_URL] = "   "
        }

        val decoded = WindowStateCodec.decode(values, fallback)

        assertEquals("https://fallback.example", decoded.currentUrl)
    }

    @Test
    fun legacy_last_url_is_used_when_v2_url_is_missing() {
        val values = WindowStateCodec.encode(state()).toMutableMap().apply {
            remove(WindowStateCodec.KEY_CURRENT_URL)
            this[WindowStateCodec.KEY_LEGACY_LAST_URL] = "https://legacy.example"
        }

        val decoded = WindowStateCodec.decode(values, fallback = state())

        assertEquals("https://legacy.example", decoded.currentUrl)
    }

    private fun state(
        mode: WindowMode = WindowMode.WINDOWED,
        preMinimizeMode: WindowMode = WindowMode.WINDOWED,
        currentUrl: String = "https://example.com",
        desktopMode: Boolean = false,
        zoomPercent: Int = 100
    ): BrowserWindowState = BrowserWindowState(
        mode = mode,
        geometry = WindowGeometry(10, 20, 500, 600),
        lastNormalGeometry = WindowGeometry(30, 40, 700, 800),
        preMinimizeMode = preMinimizeMode,
        bubbleX = 900,
        bubbleY = 250,
        currentUrl = currentUrl,
        desktopMode = desktopMode,
        zoomPercent = zoomPercent
    )
}
