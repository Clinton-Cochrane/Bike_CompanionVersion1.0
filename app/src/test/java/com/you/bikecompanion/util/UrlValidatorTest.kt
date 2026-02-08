package com.you.bikecompanion.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UrlValidator].
 */
class UrlValidatorTest {

    @Test
    fun isValidHttpUrl_returnsFalse_forNull() {
        assertFalse(UrlValidator.isValidHttpUrl(null))
    }

    @Test
    fun isValidHttpUrl_returnsFalse_forBlank() {
        assertFalse(UrlValidator.isValidHttpUrl(""))
        assertFalse(UrlValidator.isValidHttpUrl("   "))
    }

    @Test
    fun isValidHttpUrl_returnsTrue_forValidHttps() {
        assertTrue(UrlValidator.isValidHttpUrl("https://example.com"))
        assertTrue(UrlValidator.isValidHttpUrl("https://example.com/path?q=1"))
    }

    @Test
    fun isValidHttpUrl_returnsTrue_forValidHttp() {
        assertTrue(UrlValidator.isValidHttpUrl("http://example.com"))
    }

    @Test
    fun isValidHttpUrl_returnsFalse_forInvalidUrl() {
        assertFalse(UrlValidator.isValidHttpUrl("not-a-url"))
        assertFalse(UrlValidator.isValidHttpUrl("ftp://example.com"))
    }
}
