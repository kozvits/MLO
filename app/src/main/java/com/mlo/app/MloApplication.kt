package com.mlo.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MloApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Sync channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Синхронизация",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о синхронизации с Dropbox"
            }
            notificationManager.createNotificationChannel(syncChannel)

            // Geo channel
            val geoChannel = NotificationChannel(
                CHANNEL_GEO,
                "Геонапоминания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления при входе/выходе из зон задач"
            }
            notificationManager.createNotificationChannel(geoChannel)

            // Tasks channel
            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напоминания о просроченных задачах"
            }
            notificationManager.createNotificationChannel(tasksChannel)
        }
    }

    companion object {
        const val CHANNEL_SYNC = "mlo_sync"
        const val CHANNEL_GEO = "mlo_geo"
        const val CHANNEL_TASKS = "mlo_tasks"
    }
}
