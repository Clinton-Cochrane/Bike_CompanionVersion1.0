package com.you.bikecompanion.util

import java.net.URI

/**
 * Validates that a string is a well-formed URL (http/https).
 * Used for component context purchase link validation.
 */
object UrlValidator {

    /**
     * Returns true if [url] is non-blank and parses as a valid URI with http or https scheme.
     */
    fun isValidHttpUrl(url: String?): Boolean {
        val trimmed = url?.trim() ?: return false
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            scheme == "http" || scheme == "https"
        } catch (_: Exception) {
            false
        }
    }
}
