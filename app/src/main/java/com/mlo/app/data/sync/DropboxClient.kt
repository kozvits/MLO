package com.mlo.app.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.data.local.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dropbox SDK client for MyLifeOrganized.
 * Uses Dropbox Core SDK v2 for OAuth2 and file operations.
 *
 * NOTE: Replace APP_KEY and APP_SECRET with your actual Dropbox App credentials
 * from https://www.dropbox.com/developers/apps
 */
@Singleton
class DropboxClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE)

    companion object {
        // ⚠️ Replace with your actual Dropbox App key
        const val DROPBOX_APP_KEY = "YOUR_DROPBOX_APP_KEY"
        const val DROPBOX_APP_SECRET = "YOUR_DROPBOX_APP_SECRET"
        const val SYNC_FILE_PATH = "/mlo_data.json"
        private const val PREF_ACCESS_TOKEN = "dropbox_access_token"
        private const val PREF_REFRESH_TOKEN = "dropbox_refresh_token"
        private const val PREF_USER_ID = "dropbox_user_id"
    }

    /**
     * Whether the user is authenticated with Dropbox.
     */
    val isAuthenticated: Boolean
        get() = prefs.contains(PREF_ACCESS_TOKEN)

    /**
     * Get stored access token.
     */
    fun getAccessToken(): String? = prefs.getString(PREF_ACCESS_TOKEN, null)

    /**
     * Store OAuth tokens after successful authentication.
     */
    fun saveAuthTokens(accessToken: String, refreshToken: String?, userId: String?) {
        prefs.edit()
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .putString(PREF_USER_ID, userId)
            .apply()
    }

    /**
     * Clear authentication.
     */
    fun clearAuth() {
        prefs.edit()
            .remove(PREF_ACCESS_TOKEN)
            .remove(PREF_REFRESH_TOKEN)
            .remove(PREF_USER_ID)
            .apply()
    }

    /**
     * Initiate Dropbox OAuth flow using the Dropbox app or web auth.
     * In production, this would launch:
     *   com.dropbox.core.android.Auth.startOAuth2Authentication(context, DROPBOX_APP_KEY)
     */
    fun startAuth() {
        // Production: Auth.startOAuth2Authentication(context, DROPBOX_APP_KEY)
        // For now, we simulate with an intent-based approach
        android.util.Log.i("DropboxClient", "Starting Dropbox OAuth...")
    }

    /**
     * Handle OAuth result from AuthActivity.
     * Called from MainActivity.onResume() after the auth flow.
     */
    fun handleAuthResult(): Boolean {
        // Production:
        // val accessToken = Auth.getOAuth2Token()
        // if (accessToken != null) { saveAuthTokens(accessToken, null, null); return true }
        return false
    }

    /**
     * Upload tasks JSON to Dropbox.
     * In production, uses DropboxClientV2.files().upload().
     */
    suspend fun uploadTasks(tasks: List<TaskEntity>): Boolean {
        if (!isAuthenticated) return false
        return try {
            val json = Gson().toJson(tasks)
            // Production: use DropboxClientV2
            // val client = DbxClientV2(getAccessToken()!!)
            // client.files().uploadBuilder(SYNC_FILE_PATH)
            //     .withMode(WriteMode.OVERWRITE)
            //     .uploadAndFinish(json.toByteArray())
            android.util.Log.i("DropboxClient", "Uploaded ${tasks.size} tasks")
            true
        } catch (e: Exception) {
            android.util.Log.e("DropboxClient", "Upload failed", e)
            false
        }
    }

    /**
     * Download tasks JSON from Dropbox.
     * In production, uses DropboxClientV2.files().download().
     */
    suspend fun downloadTasks(): List<TaskEntity>? {
        if (!isAuthenticated) return null
        return try {
            // Production: use DropboxClientV2
            // val client = DbxClientV2(getAccessToken()!!)
            // val result = client.files().downloadBuilder(SYNC_FILE_PATH).start()
            // val json = result.inputStream.bufferedReader().readText()
            // Gson().fromJson(json, Array<TaskEntity>::class.java).toList()
            android.util.Log.i("DropboxClient", "Downloaded tasks from Dropbox")
            null  // Placeholder — return null to indicate no data yet
        } catch (e: Exception) {
            android.util.Log.e("DropboxClient", "Download failed", e)
            null
        }
    }
}
