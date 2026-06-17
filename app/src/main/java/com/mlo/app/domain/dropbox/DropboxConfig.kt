package com.mlo.app.domain.dropbox

/**
 * Dropbox configuration.
 *
 * WARNING: The token below is a short-lived OAuth 2.0 access token.
 * It expires ~4 hours after issue. Replace with a refresh-token-based flow
 * for production use (see Dropbox OAuth Guide).
 *
 * In production, the token should be stored in EncryptedSharedPreferences,
 * not hardcoded.
 */
object DropboxConfig {
    const val API_BASE_URL = "https://api.dropboxapi.com/2/"
    const val CONTENT_BASE_URL = "https://content.dropboxapi.com/2/"

    /** Token placeholder — set via DropboxConfig.init() or EncryptedSharedPreferences. */
    const val ACCESS_TOKEN = "YOUR_DROPBOX_TOKEN_HERE"

    /** Dropbox API path for storing MLO backup files. */
    const val BACKUP_DIR = "/MLO"

    /** Filename prefix for sync files. */
    const val BACKUP_PREFIX = "mlo_backup_"
}
