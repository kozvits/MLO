package com.mlo.app.ui.widgets

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mlo.app.MainActivity

/**
 * Quick Settings Tile — quickly add a new task to Inbox.
 */
class QuickTaskTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        // Update tile state
        qsTile?.let { tile ->
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Быстрая задача"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Open main activity with "new task" action
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("action", "new_task")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivityAndCollapse(intent)
    }
}
