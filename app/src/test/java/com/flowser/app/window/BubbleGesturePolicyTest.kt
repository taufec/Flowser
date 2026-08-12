package com.flowser.app.window

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleGesturePolicyTest {
    @Test
    fun movement_below_8dp_is_a_tap() {
        assertTrue(BubbleGesturePolicy.isTap(deltaX = 7, deltaY = 7, density = 1f))
    }

    @Test
    fun movement_at_8dp_is_a_drag() {
        assertFalse(BubbleGesturePolicy.isTap(deltaX = 8, deltaY = 0, density = 1f))
    }

    @Test
    fun threshold_scales_with_density() {
        assertTrue(BubbleGesturePolicy.isTap(deltaX = 15, deltaY = 0, density = 2f))
        assertFalse(BubbleGesturePolicy.isTap(deltaX = 16, deltaY = 0, density = 2f))
    }
}
