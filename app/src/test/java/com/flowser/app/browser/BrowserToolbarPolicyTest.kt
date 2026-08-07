package com.flowser.app.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserToolbarPolicyTest {
    @Test
    fun toolbar_height_is_half_the_original_48dp() {
        assertEquals(24, BrowserToolbarPolicy.toolbarHeightDp())
    }

    @Test
    fun title_tap_opens_browser_menu() {
        assertEquals(
            ToolbarInteraction.OPEN_MENU,
            BrowserToolbarPolicy.titleTapInteraction()
        )
    }

    @Test
    fun minimize_tap_minimizes_window() {
        assertEquals(
            ToolbarInteraction.MINIMIZE,
            BrowserToolbarPolicy.minimizeTapInteraction()
        )
    }

    @Test
    fun minimize_long_press_closes_window() {
        assertEquals(
            ToolbarInteraction.CLOSE,
            BrowserToolbarPolicy.minimizeLongPressInteraction()
        )
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
