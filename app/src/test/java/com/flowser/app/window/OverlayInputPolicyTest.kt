package com.flowser.app.window

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayInputPolicyTest {
    @Test
    fun browser_touch_gives_keyboard_focus_to_flowser() {
        val state = OverlayInputPolicy.afterBrowserTouch()

        assertTrue(state.browserOwnsKeyboard)
    }

    @Test
    fun outside_touch_releases_keyboard_focus_to_background_app() {
        val state = OverlayInputPolicy.afterOutsideTouch()

        assertFalse(state.browserOwnsKeyboard)
    }
}
