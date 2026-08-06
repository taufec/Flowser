package com.flowser.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private var pendingUrl: String? = null
    private var awaitingOverlayPermission = false

    private val preferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
    }

    override fun onResume() {
        super.onResume()
        if (!awaitingOverlayPermission) return
        awaitingOverlayPermission = false

        val url = pendingUrl
        if (Settings.canDrawOverlays(this) && url != null) {
            launchFloatingBrowser(url)
        } else {
            pendingUrl = null
            statusText.text = "Overlay permission was not granted."
        }
    }

    private fun createContentView(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
            setBackgroundColor(Color.rgb(248, 249, 252))
        }

        val title = TextView(this).apply {
            text = "Flowser 0.2"
            textSize = 30f
            setTextColor(Color.rgb(20, 24, 32))
        }

        val description = TextView(this).apply {
            text = "Open a website above other apps. Move it, resize it, maximize it, or minimize it into a bubble."
            textSize = 16f
            setTextColor(Color.rgb(75, 82, 96))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }

        urlInput = EditText(this).apply {
            hint = "https://example.com"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
            setText(preferences.getString(KEY_LAST_URL, DEFAULT_URL))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val startButton = Button(this).apply {
            text = "Open floating browser"
            isAllCaps = false
            setOnClickListener { startRequestedUrl() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }

        statusText = TextView(this).apply {
            text = "Drag the top bar to move. Drag the bottom-right handle to resize. Use _ to minimize."
            textSize = 14f
            setTextColor(Color.rgb(75, 82, 96))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }

        root.addView(title)
        root.addView(description)
        root.addView(urlInput)
        root.addView(startButton)
        root.addView(statusText)
        return root
    }

    private fun startRequestedUrl() {
        val normalizedUrl = UrlNormalizer.normalize(urlInput.text.toString())
        if (normalizedUrl == null) {
            urlInput.error = "Enter a valid HTTP or HTTPS website address"
            statusText.text = "The URL could not be opened."
            return
        }

        urlInput.error = null
        urlInput.setText(normalizedUrl)
        preferences.edit().putString(KEY_LAST_URL, normalizedUrl).apply()

        if (Settings.canDrawOverlays(this)) {
            launchFloatingBrowser(normalizedUrl)
            return
        }

        pendingUrl = normalizedUrl
        awaitingOverlayPermission = true
        statusText.text = "Allow Flowser to display over other apps, then return here."

        val permissionIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(permissionIntent)
        } catch (_: Exception) {
            awaitingOverlayPermission = false
            pendingUrl = null
            statusText.text = "Android could not open the overlay permission screen."
        }
    }

    private fun launchFloatingBrowser(url: String) {
        val serviceIntent = Intent(this, FloatingBrowserService::class.java)
            .setAction(FloatingBrowserService.ACTION_OPEN)
            .putExtra(FloatingBrowserService.EXTRA_URL, url)
        startForegroundService(serviceIntent)
        pendingUrl = null
        statusText.text = "Flowser is running. Controls are also available in the notification."
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS_NAME = "flowser_preferences"
        private const val KEY_LAST_URL = "last_url"
        private const val DEFAULT_URL = "https://example.com"
    }
}
