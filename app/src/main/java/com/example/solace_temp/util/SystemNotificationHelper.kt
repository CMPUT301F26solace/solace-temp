/**
 * SystemNotificationHelper.kt
 *
 * Purpose: Manages the Android notification channel and dispatches system-level
 * status-bar notifications to the user's phone for lottery wins and invitations.
 */
package com.example.solace_temp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.solace_temp.MainActivity
import com.example.solace_temp.R
import com.example.solace_temp.models.AppNotification

/**
 * Utility helper for posting Android system notifications.
 */
object SystemNotificationHelper {

    private const val CHANNEL_ID = "solace_lottery_channel"
    private const val CHANNEL_NAME = "Solace Lottery Updates"
    private const val CHANNEL_DESCRIPTION = "Notifications for lottery results and booking invitations."

    /**
     * Initializes the notification channel on Android O (API 26) and above.
     *
     * @param context Application context used to access system services.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches a system status-bar notification for a winning lottery notification.
     *
     * @param context Application context to trigger system notification.
     * @param notification The lottery notification payload.
     */
    fun postLotteryWinNotification(context: Context, notification: AppNotification) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title.ifBlank { "You won the lottery!" })
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notification.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Handled when notification permission is denied on Android 13+
        }
    }
}
