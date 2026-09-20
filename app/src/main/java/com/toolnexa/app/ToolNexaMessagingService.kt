package com.toolnexa.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ToolNexaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AnalyticsTracker(this).event(
            "fcm_token_refreshed"
        )
    }

    override fun onMessageReceived(
        message: RemoteMessage
    ) {
        super.onMessageReceived(message)

        AnalyticsTracker(this).event(
            "notification_received",
            "has_data" to message.data.isNotEmpty().toString()
        )

        val title =
            message.notification?.title
                ?: message.data["title"]
                ?: "ToolNexa"

        val body =
            message.notification?.body
                ?: message.data["body"]
                ?: "Há uma novidade no ToolNexa."

        showNotification(title, body)
    }

    private fun showNotification(
        title: String,
        body: String
    ) {
        val channelId = "toolnexa_updates"
        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Atualizações do ToolNexa",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pending = PendingIntent.getActivity(
            this,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setSmallIcon(
                    R.drawable.ic_toolnexa
                )
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE)
                .toInt(),
            notification
        )
    }
}
