package com.mlo.app.data.local

import androidx.room.*

/**
 * Many-to-many relation between tasks and flags
 */
@Entity(
    tableName = "task_flag_cross_ref",
    primaryKeys = ["taskId", "flagId"],
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FlagEntity::class, parentColumns = ["id"], childColumns = ["flagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("flagId")]
)
data class TaskFlagCrossRef(
    val taskId: Long,
    val flagId: Long
)

/**
 * DAO for task-flag cross references
 */
@Dao
interface TaskFlagDao {
    @Query("SELECT flagId FROM task_flag_cross_ref WHERE taskId = :taskId")
    fun getFlagIdsForTask(taskId: Long): kotlinx.coroutines.flow.Flow<List<Long>>

    @Query("SELECT flagId FROM task_flag_cross_ref WHERE taskId = :taskId")
    suspend fun getFlagIdsForTaskSync(taskId: Long): List<Long>

    @Query("SELECT * FROM task_flag_cross_ref WHERE taskId = :taskId")
    suspend fun getCrossRefsForTask(taskId: Long): List<TaskFlagCrossRef>

    @Query("SELECT * FROM flags INNER JOIN task_flag_cross_ref ON flags.id = task_flag_cross_ref.flagId WHERE task_flag_cross_ref.taskId = :taskId")
    suspend fun getFlagsForTask(taskId: Long): List<FlagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: TaskFlagCrossRef): Long

    @Delete
    suspend fun delete(ref: TaskFlagCrossRef)

    @Query("DELETE FROM task_flag_cross_ref WHERE taskId = :taskId AND flagId = :flagId")
    suspend fun deleteByTaskAndFlag(taskId: Long, flagId: Long)

    @Query("DELETE FROM task_flag_cross_ref WHERE taskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<TaskFlagCrossRef>)
}
