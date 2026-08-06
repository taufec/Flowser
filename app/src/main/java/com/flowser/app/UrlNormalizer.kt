package com.flowser.app

import java.net.URI

object UrlNormalizer {
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val candidate = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains(":") && !trimmed.matches(Regex("^[A-Za-z0-9.-]+:\\d+(/.*)?$")) -> return null
            else -> "https://$trimmed"
        }

        return try {
            val uri = URI(candidate)
            if ((uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()) {
                candidate
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
