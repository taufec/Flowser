package com.flowser.app.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserToolbarPolicyTest {
    @Test
    fun width_below_420dp_uses_compact_toolbar() {
        assertTrue(BrowserToolbarPolicy.isCompact(windowWidthPx = 838, density = 2f))
    }

    @Test
    fun width_at_420dp_uses_wide_toolbar() {
        assertFalse(BrowserToolbarPolicy.isCompact(windowWidthPx = 840, density = 2f))
    }

    @Test
    fun loading_page_uses_stop_label() {
        assertEquals("Stop", BrowserToolbarPolicy.reloadLabel(isLoading = true))
    }

    @Test
    fun idle_page_uses_reload_label() {
        assertEquals("Reload", BrowserToolbarPolicy.reloadLabel(isLoading = false))
    }

    @Test
    fun maximized_window_uses_restore_label() {
        assertEquals("Restore", BrowserToolbarPolicy.maximizeLabel(isMaximized = true))
    }

    @Test
    fun windowed_mode_uses_maximize_label() {
        assertEquals("Maximize", BrowserToolbarPolicy.maximizeLabel(isMaximized = false))
    }
}
