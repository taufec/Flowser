package com.flowser.app.window

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleIconPolicyTest {
    @Test
    fun favicon_is_used_when_page_icon_exists() {
        assertEquals(BubbleIconMode.FAVICON, BubbleIconPolicy.mode(hasFavicon = true))
    }

    @Test
    fun flowser_fallback_is_used_when_page_icon_is_missing() {
        assertEquals(BubbleIconMode.FALLBACK, BubbleIconPolicy.mode(hasFavicon = false))
    }
}
