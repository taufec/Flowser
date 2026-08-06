package com.flowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {
    @Test
    fun trimsInputAndDefaultsToHttps() {
        assertEquals("https://example.com", UrlNormalizer.normalize("  example.com  "))
    }

    @Test
    fun preservesHttpAndHttpsSchemes() {
        assertEquals("http://192.168.1.10:3000", UrlNormalizer.normalize("http://192.168.1.10:3000"))
        assertEquals("https://example.com/path", UrlNormalizer.normalize("https://example.com/path"))
    }

    @Test
    fun rejectsBlankInput() {
        assertNull(UrlNormalizer.normalize("   "))
    }

    @Test
    fun rejectsUnsupportedSchemes() {
        assertNull(UrlNormalizer.normalize("javascript:alert(1)"))
        assertNull(UrlNormalizer.normalize("file:///sdcard/test.html"))
    }
}
