package com.mlo.app.data.local

import androidx.room.*

/**
 * Context entity for task organization (e.g., @Home, @Office, @Computer)
 */
@Entity(tableName = "contexts")
data class ContextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // "@Home"
    val icon: String? = null,
    val color: Int? = null,
    val includeIds: String = "",         // included context IDs comma separated
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts ORDER BY sortOrder, name")
    fun getAllContexts(): kotlinx.coroutines.flow.Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getContextById(id: Long): ContextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(context: ContextEntity): Long

    @Update
    suspend fun update(context: ContextEntity)

    @Delete
    suspend fun delete(context: ContextEntity)

    @Query("DELETE FROM contexts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/**
 * Context hours — define when a context is "open" (available)
 */
@Entity(tableName = "context_hours")
data class ContextHourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contextId: Long,
    val dayOfWeek: Int,                   // 1=Monday ... 7=Sunday
    val startHour: Int,                   // 0-23
    val endHour: Int                      // 0-23
)

@Dao
interface ContextHourDao {
    @Query("SELECT * FROM context_hours WHERE contextId = :contextId")
    suspend fun getHoursForContext(contextId: Long): List<ContextHourEntity>

    @Query("SELECT * FROM context_hours")
    fun getAllContextHours(): kotlinx.coroutines.flow.Flow<List<ContextHourEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hour: ContextHourEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hours: List<ContextHourEntity>)

    @Query("DELETE FROM context_hours WHERE contextId = :contextId")
    suspend fun deleteForContext(contextId: Long)
}

/**
 * Goal entity
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val status: String = "ACTIVE",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY sortOrder, name")
    fun getAllGoals(): kotlinx.coroutines.flow.Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)
}
