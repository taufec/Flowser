package com.flowser.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this).apply {
            text = "Start Flowser Floating Browser"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@SettingsActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    Toast.makeText(this@SettingsActivity, "Allow overlay permission", Toast.LENGTH_LONG).show()
                } else {
                    startService(Intent(this@SettingsActivity, FloatingService::class.java))
                }
            }
        }

        setContentView(LinearLayout(this).apply {
            addView(button)
        })
    }
}
