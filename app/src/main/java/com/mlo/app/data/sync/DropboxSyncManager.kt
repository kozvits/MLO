package com.mlo.app.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level sync manager that coordinates Dropbox sync operations.
 */
@Singleton
class DropboxSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dropboxClient: DropboxClient
) {

    val isAuthenticated: Boolean get() = dropboxClient.isAuthenticated

    /**
     * Start the OAuth flow to connect Dropbox.
     */
    fun connectDropbox() {
        dropboxClient.startAuth()
    }

    /**
     * Disconnect from Dropbox.
     */
    fun disconnectDropbox() {
        dropboxClient.clearAuth()
        DropboxSyncWorker.cancelSync(context)
    }

    /**
     * Handle OAuth result (called from Activity).
     */
    fun handleAuthResult(): Boolean {
        val success = dropboxClient.handleAuthResult()
        if (success) {
            // Schedule periodic sync after successful auth
            DropboxSyncWorker.schedule(context)
        }
        return success
    }

    /**
     * Trigger immediate sync.
     */
    fun syncNow() {
        DropboxSyncWorker.syncNow(context)
    }

    /**
     * Schedule periodic sync.
     */
    fun schedulePeriodicSync() {
        if (dropboxClient.isAuthenticated) {
            DropboxSyncWorker.schedule(context)
        }
    }
}
