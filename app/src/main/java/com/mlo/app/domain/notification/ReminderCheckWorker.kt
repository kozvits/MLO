package com.mlo.app.domain.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mlo.app.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for time-based reminders.
 * Runs periodically every 15 minutes and fires notifications for overdue time reminders.
 */
@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val overdueReminders = repository.getOverdueTimeReminders(now)

            for (reminder in overdueReminders) {
                val task = repository.getTaskById(reminder.taskId)
                if (task != null) {
                    NotificationHelper.showReminderNotification(
                        applicationContext,
                        task.name,
                        task.id
                    )
                    // Disable one-shot reminders after firing
                    if (reminder.type == "TIME_ONCE") {
                        repository.deleteReminder(reminder)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "reminder_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
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
        }
    }
}
