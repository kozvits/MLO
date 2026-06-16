package com.mlo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val durationMinutes: Int? = null,
    val importance: Int = 100,
    val urgency: Int = 100,
    val contexts: String = "",         // comma-separated: "@Office,@Computer"
    val dependencyIds: String = "",    // comma-separated: "1,3,5"
    val goalId: Long? = null,
    val projectId: Long? = null,
    val status: String = "ACTIVE",     // ACTIVE, COMPLETED, DEFERRED, DELEGATED
    val weeklyGoalWeight: Double = 100.0,
    val flags: String = "",            // "STARRED,DELEGATED"
    val locationLat: Double? = null,
    val locationLon: Double? = null,
    val locationRadiusMeters: Int? = null,
    val isRecurring: Boolean = false,
    val recurringPattern: String? = null,  // "DAILY|WEEKLY|MONTHLY|CUSTOM"
    val notes: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
