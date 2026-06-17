package com.mlo.app.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mlo.app.MainActivity
import com.mlo.app.R

object NotificationHelper {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_GEOFENCING = "geofencing"
    const val CHANNEL_SYNC = "sync"
    const val CHANNEL_WIDGET = "widget_updates"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_REMINDERS, "Напоминания", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о наступлении сроков задач"
            },
            NotificationChannel(
                CHANNEL_GEOFENCING, "Гео-напоминания", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления при входе/выходе из геозон"
            },
            NotificationChannel(
                CHANNEL_SYNC, "Синхронизация", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Статус синхронизации с облаком"
            },
            NotificationChannel(
                CHANNEL_WIDGET, "Виджеты", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Обновления виджетов"
            }
        )

        channels.forEach { channel ->
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(context: Context, taskTitle: String, taskId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Напоминание")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.toInt(), notification)
    }

    fun showGeofenceNotification(context: Context, taskTitle: String, taskId: Long, entering: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.toInt() + 10000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (entering) "Вы в зоне задачи" else "Вы покинули зону задачи"

        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(R.drawable.ic_notification_geofence)
            .setContentTitle(title)
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.toInt() + 10000, notification)
    }
}
