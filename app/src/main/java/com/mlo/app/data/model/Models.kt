package com.mlo.app.data.model

/**
 * UI-friendly representation of a task with computed fields
 */
data class TaskItem(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val startDate: Long?,
    val dueDate: Long?,
    val durationMinutes: Int?,
    val importance: Int,
    val urgency: Int,
    val effectiveImportance: Int,
    val effectiveUrgency: Int,
    val contexts: List<String>,
    val dependencyIds: List<Long>,
    val goalId: Long?,
    val projectId: Long?,
    val status: TaskStatus,
    val weeklyGoalWeight: Double,
    val flags: Set<String>,
    val locationLat: Double?,
    val locationLon: Double?,
    val locationRadiusMeters: Int?,
    val isRecurring: Boolean,
    val recurringPattern: String?,
    val notes: String?,
    val priorityScore: Double,
    val isActive: Boolean,
    val depth: Int,
    val hasChildren: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

enum class TaskStatus {
    ACTIVE,
    COMPLETED,
    DEFERRED,
    DELEGATED;

    companion object {
        fun fromString(s: String): TaskStatus = when (s.uppercase()) {
            "ACTIVE" -> ACTIVE
            "COMPLETED" -> COMPLETED
            "DEFERRED" -> DEFERRED
            "DELEGATED" -> DELEGATED
            else -> ACTIVE
        }
    }
}

/**
 * Configuration for priority score calculation
 */
data class PriorityConfig(
    val wI: Double = 0.4,   // Importance weight
    val wU: Double = 0.3,   // Urgency weight
    val wT: Double = 0.2,   // Time factor weight
    val wG: Double = 0.1    // Weekly Goal weight
)

/**
 * Saved view/filter configuration
 */
data class SavedView(
    val id: String = "",
    val name: String,
    val contextFilter: List<String> = emptyList(),
    val statusFilter: TaskStatus? = null,
    val sortBy: SortBy = SortBy.PRIORITY,
    val groupBy: GroupBy = GroupBy.NONE,
    val isAscending: Boolean = false
)

enum class SortBy {
    PRIORITY,
    DUE_DATE,
    CREATED_DATE,
    NAME,
    IMPORTANCE,
    URGENCY,
    CUSTOM
}

enum class GroupBy {
    NONE,
    CONTEXT,
    GOAL,
    PROJECT,
    STATUS
}
