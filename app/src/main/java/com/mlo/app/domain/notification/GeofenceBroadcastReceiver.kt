package com.mlo.app.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.mlo.app.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives geofence transitions and shows notifications.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorMessage = when (geofencingEvent?.errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence service not available"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many geofences"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
                else -> "Unknown geofence error"
            }
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        scope.launch {
            for (geofence in triggeringGeofences) {
                val requestId = geofence.requestId // "task_{taskId}_{reminderId}"
                val parts = requestId.split("_")
                if (parts.size < 2) continue
                val taskId = parts[1].toLongOrNull() ?: continue

                val task = repository.getTaskById(taskId)
                if (task != null) {
                    val entering = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER
                    NotificationHelper.showGeofenceNotification(
                        context, task.name, taskId, entering
                    )
                }
            }
        }
    }
}
