package com.mlo.app.data.model

import com.mlo.app.data.local.*
import kotlinx.coroutines.flow.Flow

// ── UI Models ──

data class FlagModel(
    val id: Long,
    val name: String,
    val label: String,
    val color: Int?,
    val iconName: String?,
    val isActive: Boolean = false
) {
    companion object {
        fun fromEntity(entity: FlagEntity) = FlagModel(
            id = entity.id,
            name = entity.name,
            label = entity.label,
            color = entity.color,
            iconName = entity.iconName
        )
        fun defaultSet(): List<FlagModel> = listOf(
            FlagModel(0, "STARRED", "Избранное", 0xFFFFD700.toInt(), "star"),
            FlagModel(0, "DELEGATED", "Делегировано", 0xFF2196F3.toInt(), "person"),
            FlagModel(0, "WAITING", "Ожидание", 0xFFFF9800.toInt(), "hourglass_empty"),
            FlagModel(0, "URGENT", "Срочно", 0xFFF44336.toInt(), "priority_high"),
            FlagModel(0, "SOMEDAY", "Когда-нибудь", 0xFF9C27B0.toInt(), "event_note"),
        )
    }
}

data class ReminderDisplayModel(
    val id: Long,
    val taskId: Long,
    val taskTitle: String,
    val type: String,
    val triggerTime: Long?,
    val locationLat: Double?,
    val locationLon: Double?,
    val locationRadiusMeters: Int,
    val isEnabled: Boolean
)

data class ViewFilter(
    val viewId: Long? = null,
    val name: String = "",
    val contextIds: List<Long> = emptyList(),
    val flagIds: List<Long> = emptyList(),
    val statusFilter: String = "",
    val searchQuery: String = "",
    val hasDueDate: Boolean = false,
    val isOverdue: Boolean = false,
    val groupBy: String = "NONE",
    val sortBy: String = "PRIORITY",
    val isAscending: Boolean = false,
    val showCompleted: Boolean = false,
    val showArchived: Boolean = false
) {
    fun toEntity(): ViewEntity = ViewEntity(
        id = viewId ?: 0,
        name = name,
        contextFilter = contextIds.joinToString(","),
        flagFilter = flagIds.joinToString(","),
        statusFilter = statusFilter,
        searchQuery = searchQuery,
        hasDueDate = hasDueDate,
        isOverdue = isOverdue,
        groupBy = groupBy,
        sortBy = sortBy,
        isAscending = isAscending,
        showCompleted = showCompleted,
        showArchived = showArchived
    )

    companion object {
        fun fromEntity(entity: ViewEntity) = ViewFilter(
            viewId = entity.id,
            name = entity.name,
            contextIds = entity.contextFilter.split(",").mapNotNull { it.trim().toLongOrNull() },
            flagIds = entity.flagFilter.split(",").mapNotNull { it.trim().toLongOrNull() },
            statusFilter = entity.statusFilter,
            searchQuery = entity.searchQuery,
            hasDueDate = entity.hasDueDate,
            isOverdue = entity.isOverdue,
            groupBy = entity.groupBy,
            sortBy = entity.sortBy,
            isAscending = entity.isAscending,
            showCompleted = entity.showCompleted,
            showArchived = entity.showArchived
        )
    }
}

data class TemplateProfile(
    val id: Long,
    val name: String,
    val description: String?,
    val category: String,
    val isBuiltIn: Boolean,
    val templateJson: String
) {
    companion object {
        fun fromEntity(entity: ProfileTemplateEntity) = TemplateProfile(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            category = entity.category,
            isBuiltIn = entity.isBuiltIn,
            templateJson = entity.templateJson
        )
    }
}

data class StatisticsData(
    val totalTasks: Int = 0,
    val activeTasks: Int = 0,
    val completedToday: Int = 0,
    val completedThisWeek: Int = 0,
    val completedThisMonth: Int = 0,
    val overdueTasks: Int = 0,
    val tasksByContext: Map<String, Int> = emptyMap(),
    val tasksByGoal: Map<String, Int> = emptyMap(),
    val completionRate: Float = 0f,
    val avgPriority: Double = 0.0,
    val trendWeek: List<DailyStats> = emptyList()
)

data class DailyStats(
    val date: String,
    val completed: Int,
    val added: Int
)

// Navigation state
data class NavigationState(
    val currentTab: Int = 0,
    val selectedTaskId: Long? = null,
    val showFlagManager: Boolean = false,
    val showContextManager: Boolean = false,
    val showViewManager: Boolean = false,
    val showTemplateManager: Boolean = false,
    val showStatistics: Boolean = false,
    val showSettings: Boolean = false
)
