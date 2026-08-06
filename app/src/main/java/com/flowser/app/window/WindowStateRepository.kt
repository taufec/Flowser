package com.flowser.app.window

import android.content.Context

internal object WindowStateCodec {
    const val KEY_MODE = "mode"
    const val KEY_X = "x"
    const val KEY_Y = "y"
    const val KEY_WIDTH = "width"
    const val KEY_HEIGHT = "height"
    const val KEY_NORMAL_X = "normal_x"
    const val KEY_NORMAL_Y = "normal_y"
    const val KEY_NORMAL_WIDTH = "normal_width"
    const val KEY_NORMAL_HEIGHT = "normal_height"
    const val KEY_PRE_MINIMIZE_MODE = "pre_minimize_mode"
    const val KEY_BUBBLE_X = "bubble_x"
    const val KEY_BUBBLE_Y = "bubble_y"
    const val KEY_CURRENT_URL = "current_url"
    const val KEY_DESKTOP_MODE = "desktop_mode"
    const val KEY_ZOOM_PERCENT = "zoom_percent"
    const val KEY_LEGACY_LAST_URL = "last_url"

    fun encode(state: BrowserWindowState): Map<String, String> = linkedMapOf(
        KEY_MODE to state.mode.name,
        KEY_X to state.geometry.x.toString(),
        KEY_Y to state.geometry.y.toString(),
        KEY_WIDTH to state.geometry.width.toString(),
        KEY_HEIGHT to state.geometry.height.toString(),
        KEY_NORMAL_X to state.lastNormalGeometry.x.toString(),
        KEY_NORMAL_Y to state.lastNormalGeometry.y.toString(),
        KEY_NORMAL_WIDTH to state.lastNormalGeometry.width.toString(),
        KEY_NORMAL_HEIGHT to state.lastNormalGeometry.height.toString(),
        KEY_PRE_MINIMIZE_MODE to state.preMinimizeMode.name,
        KEY_BUBBLE_X to state.bubbleX.toString(),
        KEY_BUBBLE_Y to state.bubbleY.toString(),
        KEY_CURRENT_URL to state.currentUrl,
        KEY_DESKTOP_MODE to state.desktopMode.toString(),
        KEY_ZOOM_PERCENT to state.zoomPercent.coerceIn(50, 200).toString()
    )

    fun decode(
        values: Map<String, String>,
        fallback: BrowserWindowState
    ): BrowserWindowState {
        val mode = parseMode(values[KEY_MODE]) ?: WindowMode.WINDOWED
        val preMinimizeMode = parseMode(values[KEY_PRE_MINIMIZE_MODE])
            ?.takeIf { it == WindowMode.WINDOWED || it == WindowMode.MAXIMIZED }
            ?: WindowMode.WINDOWED
        val url = values[KEY_CURRENT_URL]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: values[KEY_LEGACY_LAST_URL]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: fallback.currentUrl

        return BrowserWindowState(
            mode = mode,
            geometry = WindowGeometry(
                x = intValue(values, KEY_X, fallback.geometry.x),
                y = intValue(values, KEY_Y, fallback.geometry.y),
                width = intValue(values, KEY_WIDTH, fallback.geometry.width),
                height = intValue(values, KEY_HEIGHT, fallback.geometry.height)
            ),
            lastNormalGeometry = WindowGeometry(
                x = intValue(values, KEY_NORMAL_X, fallback.lastNormalGeometry.x),
                y = intValue(values, KEY_NORMAL_Y, fallback.lastNormalGeometry.y),
                width = intValue(
                    values,
                    KEY_NORMAL_WIDTH,
                    fallback.lastNormalGeometry.width
                ),
                height = intValue(
                    values,
                    KEY_NORMAL_HEIGHT,
                    fallback.lastNormalGeometry.height
                )
            ),
            preMinimizeMode = preMinimizeMode,
            bubbleX = intValue(values, KEY_BUBBLE_X, fallback.bubbleX),
            bubbleY = intValue(values, KEY_BUBBLE_Y, fallback.bubbleY),
            currentUrl = url,
            desktopMode = booleanValue(
                values[KEY_DESKTOP_MODE],
                fallback.desktopMode
            ),
            zoomPercent = intValue(
                values,
                KEY_ZOOM_PERCENT,
                fallback.zoomPercent
            ).coerceIn(50, 200)
        )
    }

    private fun parseMode(value: String?): WindowMode? =
        WindowMode.entries.firstOrNull { it.name == value }

    private fun intValue(
        values: Map<String, String>,
        key: String,
        fallback: Int
    ): Int = values[key]?.toIntOrNull() ?: fallback

    private fun booleanValue(value: String?, fallback: Boolean): Boolean = when (value) {
        "true" -> true
        "false" -> false
        else -> fallback
    }
}

class WindowStateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(defaultGeometry: WindowGeometry): BrowserWindowState {
        val legacyUrl = appContext.getSharedPreferences(
            LEGACY_PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(WindowStateCodec.KEY_LEGACY_LAST_URL, null)

        val fallback = BrowserWindowState(
            mode = WindowMode.WINDOWED,
            geometry = defaultGeometry,
            lastNormalGeometry = defaultGeometry,
            preMinimizeMode = WindowMode.WINDOWED,
            bubbleX = 0,
            bubbleY = 100,
            currentUrl = legacyUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_URL,
            desktopMode = false,
            zoomPercent = 100
        )
        val values = preferences.all
            .mapValues { (_, value) -> value?.toString().orEmpty() }
            .toMutableMap()
        if (!values.containsKey(WindowStateCodec.KEY_CURRENT_URL) && !legacyUrl.isNullOrBlank()) {
            values[WindowStateCodec.KEY_LEGACY_LAST_URL] = legacyUrl
        }
        return WindowStateCodec.decode(values, fallback)
    }

    fun save(state: BrowserWindowState) {
        val editor = preferences.edit()
        WindowStateCodec.encode(state).forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    fun saveGeometry(geometry: WindowGeometry) {
        preferences.edit()
            .putString(WindowStateCodec.KEY_X, geometry.x.toString())
            .putString(WindowStateCodec.KEY_Y, geometry.y.toString())
            .putString(WindowStateCodec.KEY_WIDTH, geometry.width.toString())
            .putString(WindowStateCodec.KEY_HEIGHT, geometry.height.toString())
            .apply()
    }

    fun saveBubblePosition(x: Int, y: Int) {
        preferences.edit()
            .putString(WindowStateCodec.KEY_BUBBLE_X, x.toString())
            .putString(WindowStateCodec.KEY_BUBBLE_Y, y.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "flowser_window_state"
        private const val LEGACY_PREFS_NAME = "flowser_preferences"
        private const val DEFAULT_URL = "https://example.com"
    }
}
