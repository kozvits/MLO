package com.mlo.app.domain.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mlo.app.data.local.ReminderEntity
import com.mlo.app.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodically syncs geofence reminders from Room → Google Play Services Geofencing API.
 */
@HiltWorker
class GeofenceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val locationReminders = repository.getEnabledLocationReminders()
            val geofencingClient = LocationServices.getGeofencingClient(applicationContext)

            // Check permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) return Result.success()
            }

            // Remove all existing geofences
            geofencingClient.removeGeofences(getGeofencePendingIntent())

            if (locationReminders.isEmpty()) return Result.success()

            // Build geofences
            val geofences = locationReminders.map { reminder: ReminderEntity ->
                Geofence.Builder()
                    .setRequestId("task_${reminder.taskId}_${reminder.id}")
                    .setCircularRegion(
                        reminder.locationLat ?: 0.0,
                        reminder.locationLon ?: 0.0,
                        reminder.locationRadiusMeters.toFloat()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                                Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            geofencingClient.addGeofences(request, getGeofencePendingIntent())

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = android.content.Intent(
            applicationContext,
            GeofenceBroadcastReceiver::class.java
        )
        return PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val WORK_NAME = "geofence_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GeofenceSyncWorker>(
                2, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            LocationServices.getGeofencingClient(context).removeGeofences(
                PendingIntent.getBroadcast(
                    context, 0,
                    android.content.Intent(context, GeofenceBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }
}
