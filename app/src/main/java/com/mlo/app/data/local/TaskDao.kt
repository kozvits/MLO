package com.mlo.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun getRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY sortOrder ASC, createdAt ASC")
    fun getChildTasks(parentId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun getTasksByIds(ids: List<Long>): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE' ORDER BY sortOrder")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY updatedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks WHERE name LIKE '%' || :query || '%' ORDER BY sortOrder")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET parentId = :newParentId, sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun moveTask(taskId: Long, newParentId: Long?, sortOrder: Int)

    @Query("UPDATE tasks SET status = :status, updatedAt = :now WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun updateSortOrder(taskId: Long, sortOrder: Int)

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE parentId IS NULL")
    suspend fun getMaxRootSortOrder(): Int?

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE parentId = :parentId")
    suspend fun getMaxChildSortOrder(parentId: Long): Int?

    @Query("UPDATE tasks SET projectId = :projectId WHERE id = :taskId")
    suspend fun updateProject(taskId: Long, projectId: Long?)

    @Query("UPDATE tasks SET goalId = :goalId WHERE id = :taskId")
    suspend fun updateGoal(taskId: Long, goalId: Long?)

    @Transaction
    suspend fun toggleComplete(taskId: Long) {
        val task = getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        val newStatus = if (task.status == "COMPLETED") "ACTIVE" else "COMPLETED"
        updateStatus(taskId, newStatus, now)
    }

    @Transaction
    suspend fun mergeTasks(remoteTasks: List<TaskEntity>) {
        for (remote in remoteTasks) {
            val local = getTaskById(remote.id)
            if (local == null) {
                // New from remote — insert without id (let Room auto-generate)
                insertTask(remote.copy(id = 0))
            } else if (remote.updatedAt > local.updatedAt) {
                // Remote is newer — update
                updateTask(remote)
            }
            // If local is newer, keep local
        }
    }

    @Transaction
    suspend fun safeDeleteTask(taskId: Long) {
        // Reassign children to parent
        val task = getTaskById(taskId) ?: return
        val children = getTasksByParentSync(taskId)
        for (child in children) {
            moveTask(child.id, task.parentId, child.sortOrder)
        }
        deleteTaskById(taskId)
    }

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY sortOrder")
    suspend fun getTasksByParentSync(parentId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id IN (SELECT parentId FROM tasks WHERE id = :taskId)")
    suspend fun getParentOf(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE locationLat IS NOT NULL AND locationLon IS NOT NULL AND status = 'ACTIVE'")
    fun getGeofencedActiveTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET updatedAt = :now WHERE updatedAt < :now")
    suspend fun touchAll(now: Long = System.currentTimeMillis())
}
