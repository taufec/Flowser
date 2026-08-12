package com.flowser.app.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TickTickCompatibilityTest {
    @Test
    fun injects_only_for_ticktick_web_hosts() {
        assertTrue(TickTickCompatibility.shouldInject("https://ticktick.com/webapp/"))
        assertTrue(TickTickCompatibility.shouldInject("https://www.ticktick.com/webapp/"))
        assertFalse(TickTickCompatibility.shouldInject("https://example.com/"))
        assertFalse(TickTickCompatibility.shouldInject("https://not-ticktick.com/"))
    }

    @Test
    fun shim_only_translates_finger_touch_into_mouse_events() {
        val script = TickTickCompatibility.script()

        assertTrue(script.contains("CodeMirror-widget"))
        assertTrue(script.contains("touchend"))
        assertTrue(script.contains("mousedown"))
        assertTrue(script.contains("mouseup"))
        assertTrue(script.contains("click"))
        assertTrue(script.contains("event.preventDefault()"))
        assertFalse(script.contains("replaceRange"))
        assertFalse(script.contains("coordsChar"))
        assertFalse(script.contains("getLine"))
        assertFalse(script.contains("md-item-checked"))
        assertFalse(script.contains("md-item-unchecked"))
    }
}
