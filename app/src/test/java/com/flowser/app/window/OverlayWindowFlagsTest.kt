package com.flowser.app.window

import android.view.WindowManager
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayWindowFlagsTest {
    @Test
    fun floating_window_allows_layout_beyond_display_bounds() {
        val flags = OverlayWindowFlags.baseFlags()

        assertTrue(flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0)
    }
}
