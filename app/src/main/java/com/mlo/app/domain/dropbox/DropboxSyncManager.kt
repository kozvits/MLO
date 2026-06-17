package com.mlo.app.domain.dropbox

import android.content.Context
import android.provider.Settings
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages synchronization with Dropbox via its API v2.
 *
 * Uses direct HTTP requests (no official SDK) — lightweight and self-contained.
 * Operations: upload backup, download latest backup, list backups.
 */
@Singleton
class DropboxSyncManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val authHeader get() = "Bearer ${DropboxConfig.accessToken}"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val octetMediaType = "application/octet-stream".toMediaType()

    /** Unique device identifier for conflict resolution. */
    private val deviceId: String by lazy {
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }

    // ── Public API ──

    /** Upload tasks/contexts/etc. to Dropbox as a single JSON file. */
    suspend fun uploadSyncData(data: SyncData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val syncData = data.copy(deviceId = deviceId, syncTimestamp = System.currentTimeMillis())
            val json = gson.toJson(syncData)
            val fileName = "${DropboxConfig.BACKUP_PREFIX}${syncData.syncTimestamp}.json"
            val path = "${DropboxConfig.BACKUP_DIR}/$fileName"

            val argJson = gson.toJson(mapOf(
                "path" to path,
                "mode" to "add",
                "autorename" to true,
                "mute" to false
            ))

            val request = Request.Builder()
                .url("${DropboxConfig.CONTENT_BASE_URL}files/upload")
                .addHeader("Authorization", authHeader)
                .addHeader("Dropbox-API-Arg", argJson)
                .addHeader("Content-Type", "application/octet-stream")
                .post(json.toByteArray().toRequestBody(octetMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                // Clean up old backups (keep last 3)
                cleanupOldBackups()
                Result.success(Unit)
            } else {
                val errorBody = response.body?.string() ?: "unknown error"
                Result.failure(Exception("Dropbox upload failed: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Download the most recent backup file from Dropbox. */
    suspend fun downloadLatestSyncData(): Result<SyncData> = withContext(Dispatchers.IO) {
        try {
            val latestFile = findLatestBackup() ?: return@withContext Result.failure(
                Exception("No backups found in ${DropboxConfig.BACKUP_DIR}")
            )

            val request = Request.Builder()
                .url("${DropboxConfig.CONTENT_BASE_URL}files/download")
                .addHeader("Authorization", authHeader)
                .addHeader("Dropbox-API-Arg", gson.toJson(mapOf("path" to latestFile)))
                .post("".toRequestBody(null))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: ""
                val syncData = gson.fromJson(json, SyncData::class.java)
                Result.success(syncData)
            } else {
                val errorBody = response.body?.string() ?: ""
                Result.failure(Exception("Dropbox download failed: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Test connectivity: list the backup directory to verify credentials. */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = makeApiPost("files/list_folder", mapOf(
                "path" to DropboxConfig.BACKUP_DIR,
                "include_deleted" to false,
                "limit" to 1
            ))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorBody = response.body?.string() ?: ""
                // 409 = path_not_found (directory doesn't exist yet — still authenticated)
                if (response.code == 409) Result.success(true)
                else Result.failure(Exception("Auth failed: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Internal helpers ──

    /** Find the most recent backup file in the MLO directory. */
    private suspend fun findLatestBackup(): String? = withContext(Dispatchers.IO) {
        try {
            // Try listing the directory first
            val listResponse = makeApiPost("files/list_folder", mapOf(
                "path" to DropboxConfig.BACKUP_DIR,
                "include_deleted" to false
            ))
            if (!listResponse.isSuccessful) return@withContext null

            val body = gson.fromJson(listResponse.body?.string(), Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val entries = (body["entries"] as? List<Map<String, Any?>>) ?: return@withContext null

            val backupFiles = entries
                .filter { (it[".tag"] as? String) == "file" }
                .filter { (it["name"] as? String)?.startsWith(DropboxConfig.BACKUP_PREFIX) == true }
                .sortedByDescending { (it["server_modified"] as? String) ?: "" }

            backupFiles.firstOrNull()?.let { it["path_lower"] as? String }
        } catch (_: Exception) {
            null
        }
    }

    /** Remove old backups keeping only the 3 most recent. */
    private suspend fun cleanupOldBackups() {
        try {
            val listResponse = makeApiPost("files/list_folder", mapOf(
                "path" to DropboxConfig.BACKUP_DIR,
                "include_deleted" to false
            ))
            if (!listResponse.isSuccessful) return

            val body = gson.fromJson(listResponse.body?.string(), Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val entries = (body["entries"] as? List<Map<String, Any?>>) ?: return

            val backupFiles = entries
                .filter { (it[".tag"] as? String) == "file" }
                .filter { (it["name"] as? String)?.startsWith(DropboxConfig.BACKUP_PREFIX) == true }
                .sortedByDescending { (it["server_modified"] as? String) ?: "" }

            if (backupFiles.size <= 3) return

            // Delete old files beyond the 3 most recent
            backupFiles.drop(3).forEach { file ->
                val path = file["path_lower"] as? String ?: return@forEach
                makeApiPost("files/delete_v2", mapOf("path" to path))
            }
        } catch (_: Exception) {
            // Cleanup is best-effort
        }
    }

    /** Make a POST request to the Dropbox API v2. */
    private fun makeApiPost(endpoint: String, body: Map<String, Any?>): okhttp3.Response {
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url("${DropboxConfig.API_BASE_URL}$endpoint")
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        return client.newCall(request).execute()
    }
}
