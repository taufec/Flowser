package com.flowser.app.window

import org.junit.Assert.assertEquals
import org.junit.Test

class ResizeHandlePolicyTest {
    @Test
    fun corner_resize_zone_has_no_visible_alpha() {
        assertEquals(0f, ResizeHandlePolicy.visualAlpha(), 0f)
    }

    @Test
    fun both_bottom_corners_use_40dp_touch_zones() {
        assertEquals(40, ResizeHandlePolicy.touchTargetDp())
    }
}
