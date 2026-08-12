package com.flowser.app.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPreferencesTest {
    @Test
    fun zoom_below_minimum_is_clamped_to_50_percent() {
        assertEquals(50, BrowserPreferences.clampZoom(40))
    }

    @Test
    fun zoom_above_maximum_is_clamped_to_200_percent() {
        assertEquals(200, BrowserPreferences.clampZoom(210))
    }

    @Test
    fun supported_zoom_is_unchanged() {
        assertEquals(100, BrowserPreferences.clampZoom(100))
    }

    @Test
    fun desktop_mode_uses_desktop_linux_user_agent() {
        val mobile = "Mozilla/5.0 (Linux; Android 15; Pixel) AppleWebKit/537.36 Mobile Safari/537.36"

        val desktop = BrowserPreferences.applyDesktopUserAgent(mobile, enabled = true)

        assertTrue(desktop.contains("X11; Linux x86_64"))
        assertFalse(desktop.contains(" Mobile "))
        assertFalse(desktop.contains("Android"))
    }

    @Test
    fun disabling_desktop_mode_returns_original_mobile_user_agent() {
        val mobile = "Mozilla/5.0 (Linux; Android 15; Pixel) Mobile Safari/537.36"

        assertEquals(
            mobile,
            BrowserPreferences.applyDesktopUserAgent(mobile, enabled = false)
        )
    }
}
