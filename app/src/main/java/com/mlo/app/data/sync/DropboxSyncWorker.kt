package com.mlo.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mlo.app.data.local.TaskDatabase
import com.mlo.app.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic Dropbox synchronization.
 * Syncs tasks to/from Dropbox every interval.
 */
@HiltWorker
class DropboxSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting Dropbox sync...")

        return try {
            val db = TaskDatabase.getInstance(applicationContext)
            val allTasks = db.taskDao().getAllTasksSync()
            val dropboxClient = DropboxClient(applicationContext)

            if (!dropboxClient.isAuthenticated) {
                Log.i(TAG, "Dropbox not authenticated. Skipping sync.")
                return Result.success()
            }

            // 1. Upload local tasks
            val uploadSuccess = dropboxClient.uploadTasks(allTasks)
            if (!uploadSuccess) {
                Log.w(TAG, "Upload failed or skipped. Continuing to download.")
            }

            // 2. Download remote tasks
            val remoteTasks = dropboxClient.downloadTasks()
            if (remoteTasks != null) {
                // 3. Merge (last updatedAt wins)
                db.taskDao().mergeTasks(remoteTasks)
                Log.i(TAG, "Merged ${remoteTasks.size} remote tasks")
            }

            Log.i(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "DropboxSyncWorker"
        const val WORK_NAME = "dropbox_sync"
        const val SYNC_INTERVAL_MINUTES = 15L

        /**
         * Schedule periodic sync.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DropboxSyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Trigger an immediate one-time sync.
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DropboxSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Cancel periodic sync.
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
