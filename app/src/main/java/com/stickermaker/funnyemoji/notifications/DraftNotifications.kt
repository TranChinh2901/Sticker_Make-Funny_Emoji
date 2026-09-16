package com.stickermaker.funnyemoji.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.stickermaker.funnyemoji.MainActivity
import com.stickermaker.funnyemoji.R

object DraftNotifications {
    const val CHANNEL_ID = "draft_reminders"
    const val REMINDER_NOTIFICATION_ID = 1002
    const val ACTION_RESUME_DRAFT = "com.stickermaker.funnyemoji.RESUME_DRAFT"

    fun cancelReminder(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(REMINDER_NOTIFICATION_ID)
    }

    fun showReminder(context: Context): Boolean = show(
        context, REMINDER_NOTIFICATION_ID, context.getString(R.string.draft_reminder_title),
        context.getString(R.string.draft_reminder_body), ACTION_RESUME_DRAFT,
    )


    private fun show(context: Context, id: Int, title: String, body: String, action: String?): Boolean {
        createChannel(context)
        if (!isAllowed(context)) return false
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED) return false

        val openApp = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).setAction(action).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setLargeIcon(ContextCompat.getDrawable(context, R.drawable.notification_avatar)?.toBitmap(192, 192))
            .setColor(0xFF0DB58A.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        return try {
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
            true
        } catch (_: SecurityException) {
      
            false
        }
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.draft_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.draft_notification_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun isAllowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return manager.areNotificationsEnabled() && channel != null &&
            channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}
