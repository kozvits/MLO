package com.mlo.app.data.local

import androidx.room.*

// ── Goal ──
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val title: String = "",
    val description: String? = null,
    val color: Int? = null,
    val dueDate: Long? = null,
    val status: String = "ACTIVE",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY sortOrder, name")
    fun getAllGoals(): kotlinx.coroutines.flow.Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY sortOrder, name")
    suspend fun getAllGoalsSync(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long
    
    @Update
    suspend fun update(goal: GoalEntity)
    
    @Delete
    suspend fun delete(goal: GoalEntity)
}

// ── Context Hour ──
@Entity(
    tableName = "context_hours",
    foreignKeys = [
        ForeignKey(entity = ContextEntity::class, parentColumns = ["id"], childColumns = ["contextId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("contextId")]
)
data class ContextHourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contextId: Long,
    val dayOfWeek: Int,
    val startHour: Int,
    val endHour: Int
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
