package com.mlo.app.domain.dropbox

/**
 * JSON-serializable data structure for Dropbox sync.
 * Mirrors relevant fields from Room entities for cross-device compatibility.
 */
data class SyncData(
    val deviceId: String,
    val syncTimestamp: Long,
    val tasks: List<SyncTask> = emptyList(),
    val contexts: List<SyncContext> = emptyList(),
    val goals: List<SyncGoal> = emptyList(),
    val flags: List<SyncFlag> = emptyList(),
    val views: List<SyncView> = emptyList()
)

data class SyncTask(
    val id: Long,
    val name: String,
    val status: String,
    val importance: Int,
    val urgency: Int,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val contexts: String = "",
    val parentId: Long?,
    val sortOrder: Int = 0,
    val goalId: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

data class SyncContext(
    val id: Long,
    val name: String,
    val label: String,
    val color: Int?,
    val includeIds: String = ""
)

data class SyncGoal(
    val id: Long,
    val name: String,
    val title: String,
    val color: Int? = null
)

data class SyncFlag(
    val id: Long,
    val name: String,
    val label: String,
    val color: Int? = null,
    val iconName: String? = null
)

data class SyncView(
    val id: Long,
    val name: String,
    val groupBy: String = "NONE",
    val sortBy: String = "PRIORITY",
    val isAscending: Boolean = false
)
