package com.flowser.app.window

internal enum class BubbleIconMode {
    FAVICON,
    FALLBACK
}

internal object BubbleIconPolicy {
    fun mode(hasFavicon: Boolean): BubbleIconMode =
        if (hasFavicon) BubbleIconMode.FAVICON else BubbleIconMode.FALLBACK
}
