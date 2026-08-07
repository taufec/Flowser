package com.flowser.app.window

internal data class OverlayInputState(
    val browserOwnsKeyboard: Boolean
)

internal object OverlayInputPolicy {
    fun afterBrowserTouch(): OverlayInputState = OverlayInputState(
        browserOwnsKeyboard = true
    )

    fun afterOutsideTouch(): OverlayInputState = OverlayInputState(
        browserOwnsKeyboard = false
    )
}
