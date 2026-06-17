package com.mlo.app.domain.dropbox

/**
 * Dropbox configuration.
 *
 * Token is stored in SharedPreferences and loaded at startup.
 * Use DropboxConfig.setToken() to update at runtime.
 */
object DropboxConfig {
    const val API_BASE_URL = "https://api.dropboxapi.com/2/"
    const val CONTENT_BASE_URL = "https://content.dropboxapi.com/2/"

    /** Runtime token — set via setToken() or loaded from SharedPreferences. */
    var accessToken: String = "YOUR_DROPBOX_TOKEN_HERE"
        private set

    fun setToken(token: String) {
        accessToken = token
    }

    val isTokenSet: Boolean
        get() = accessToken.isNotBlank() && accessToken != "YOUR_DROPBOX_TOKEN_HERE"

    /** Dropbox API path for storing MyOrganizer backup files. */
    const val BACKUP_DIR = "/MLO"

    /** Filename prefix for sync files. */
    const val BACKUP_PREFIX = "mlo_backup_"
}
