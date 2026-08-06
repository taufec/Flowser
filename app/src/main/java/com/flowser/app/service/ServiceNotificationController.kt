package com.flowser.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import com.flowser.app.FloatingBrowserService
import com.flowser.app.window.WindowMode

class ServiceNotificationController(
    private val service: Service
) {
    private val notificationManager =
        service.getSystemService(NotificationManager::class.java)

    init {
        createChannel()
    }

    fun build(title: String, mode: WindowMode): Notification {
        val builder = Notification.Builder(service, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Flowser")
            .setContentText(title.ifBlank { "Floating browser active" })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(commandIntent(FloatingBrowserService.ACTION_RESTORE, REQUEST_RESTORE))
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Restore",
                    commandIntent(FloatingBrowserService.ACTION_RESTORE, REQUEST_RESTORE)
                ).build()
            )

        if (mode != WindowMode.MINIMIZED) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "Minimize",
                    commandIntent(FloatingBrowserService.ACTION_MINIMIZE, REQUEST_MINIMIZE)
                ).build()
            )
        }

        builder.addAction(
            Notification.Action.Builder(
                null,
                "Close",
                commandIntent(FloatingBrowserService.ACTION_CLOSE, REQUEST_CLOSE)
            ).build()
        )
        return builder.build()
    }

    fun notify(title: String, mode: WindowMode) {
        notificationManager.notify(NOTIFICATION_ID, build(title, mode))
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun commandIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(service, FloatingBrowserService::class.java).setAction(action)
        return PendingIntent.getService(
            service,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating browser",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls the active Flowser floating browser"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_ID = 4202
        private const val CHANNEL_ID = "flowser_browser"
        private const val REQUEST_RESTORE = 1
        private const val REQUEST_MINIMIZE = 2
        private const val REQUEST_CLOSE = 3
    }
}
