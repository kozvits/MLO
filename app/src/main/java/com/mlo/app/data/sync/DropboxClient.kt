package com.mlo.app.data.sync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.mlo.app.data.local.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dropbox client for MyLifeOrganized.
 * Uses OAuth2 implicit grant flow + Dropbox HTTP API v2.
 *
 * NOTE: Replace DROPBOX_APP_KEY with your actual Dropbox App key
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
        const val SYNC_FILE_PATH = "/mlo_data.json"
        private const val PREF_ACCESS_TOKEN = "dropbox_access_token"
        private const val PREF_REFRESH_TOKEN = "dropbox_refresh_token"
        private const val PREF_USER_ID = "dropbox_user_id"
        private const val DROPBOX_CONTENT_BASE = "https://content.dropboxapi.com/2"
    }

    private val gson = Gson()

    val isAuthenticated: Boolean
        get() = prefs.contains(PREF_ACCESS_TOKEN)

    fun getAccessToken(): String? = prefs.getString(PREF_ACCESS_TOKEN, null)

    private fun saveAuthTokens(accessToken: String, refreshToken: String?, userId: String?) {
        prefs.edit()
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .putString(PREF_USER_ID, userId)
            .apply()
    }

    fun clearAuth() {
        prefs.edit()
            .remove(PREF_ACCESS_TOKEN)
            .remove(PREF_REFRESH_TOKEN)
            .remove(PREF_USER_ID)
            .apply()
    }

    /**
     * Start Dropbox OAuth2 implicit grant flow.
     * Opens the authorization URL in the device browser.
     * After user authorizes, browser redirects to db-APP_KEY://1/connect#access_token=...
     * which MainActivity intercepts via intent filter.
     */
    fun startAuth() {
        val authUri = Uri.parse("https://www.dropbox.com/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", DROPBOX_APP_KEY)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", "db-$DROPBOX_APP_KEY://1/connect")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Handle OAuth2 redirect from Dropbox.
     * Called from MainActivity.onResume/onNewIntent with the intent that
     * launched the activity (containing db-APP_KEY://... URI with access token).
     */
    fun handleAuthResult(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        // Scheme is "db-{APP_KEY}" - verify it looks like a Dropbox redirect
        if (uri.scheme?.startsWith("db-") != true) return false

        // Parse fragment: #access_token=TOKEN&token_type=bearer&uid=USER_ID
        val fragment = uri.encodedFragment ?: ""
        val params = fragment.split("&")
            .mapNotNull { part ->
                val eq = part.indexOf("=")
                if (eq > 0) part.substring(0, eq) to part.substring(eq + 1) else null
            }
            .toMap()

        val accessToken = params["access_token"] ?: return false
        val userId = params["uid"]
        saveAuthTokens(accessToken, null, userId)
        return true
    }

    /**
     * Upload tasks JSON to Dropbox using API v2.
     */
    suspend fun uploadTasks(tasks: List<TaskEntity>): Boolean = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext false
        return@withContext try {
            val json = gson.toJson(tasks)
            val url = URL("$DROPBOX_CONTENT_BASE/files/upload")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty(
                "Dropbox-API-Arg",
                """{"path":"$SYNC_FILE_PATH","mode":"overwrite"}"""
            )
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.doOutput = true
            conn.outputStream.write(json.toByteArray())
            conn.outputStream.flush()
            conn.outputStream.close()
            val success = conn.responseCode in 200..299
            conn.disconnect()
            success
        } catch (e: Exception) {
            android.util.Log.e("DropboxClient", "Upload failed", e)
            false
        }
    }

    /**
     * Download tasks JSON from Dropbox using API v2.
     */
    suspend fun downloadTasks(): List<TaskEntity>? = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext null
        return@withContext try {
            val url = URL("$DROPBOX_CONTENT_BASE/files/download")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty(
                "Dropbox-API-Arg",
                """{"path":"$SYNC_FILE_PATH"}"""
            )
            conn.doInput = true
            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val json = reader.readText()
            reader.close()
            conn.disconnect()
            gson.fromJson(json, Array<TaskEntity>::class.java)?.toList()
        } catch (e: Exception) {
            android.util.Log.e("DropboxClient", "Download failed", e)
            null
        }
    }
}
