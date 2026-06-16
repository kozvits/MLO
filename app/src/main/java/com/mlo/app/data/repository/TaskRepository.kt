package com.mlo.app.data.repository

import com.mlo.app.data.local.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val contextDao: ContextDao,
    private val contextHourDao: ContextHourDao,
    private val goalDao: GoalDao
) {

    // ── Task operations ──

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getRootTasks(): Flow<List<TaskEntity>> = taskDao.getRootTasks()

    fun getChildTasks(parentId: Long): Flow<List<TaskEntity>> = taskDao.getChildTasks(parentId)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)

    fun getActiveTasks(): Flow<List<TaskEntity>> = taskDao.getActiveTasks()

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.safeDeleteTask(task.id)

    suspend fun deleteTaskById(id: Long) = taskDao.safeDeleteTask(id)

    suspend fun toggleComplete(id: Long) = taskDao.toggleComplete(id)

    suspend fun moveTask(taskId: Long, newParentId: Long?, sortOrder: Int) {
        taskDao.moveTask(taskId, newParentId, sortOrder)
    }

    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    suspend fun getAllTasksSync(): List<TaskEntity> = taskDao.getAllTasksSync()

    suspend fun insertTasks(tasks: List<TaskEntity>) = taskDao.insertTasks(tasks)

    suspend fun getMaxRootSortOrder(): Int = taskDao.getMaxRootSortOrder() ?: 0

    suspend fun getMaxChildSortOrder(parentId: Long): Int = taskDao.getMaxChildSortOrder(parentId) ?: 0

    // ── Context operations ──

    fun getAllContexts(): Flow<List<ContextEntity>> = contextDao.getAllContexts()

    suspend fun getContextById(id: Long): ContextEntity? = contextDao.getContextById(id)

    suspend fun insertContext(context: ContextEntity): Long = contextDao.insert(context)

    suspend fun updateContext(context: ContextEntity) = contextDao.update(context)

    suspend fun deleteContext(context: ContextEntity) = contextDao.delete(context)

    suspend fun getContextHours(contextId: Long): List<ContextHourEntity> =
        contextHourDao.getHoursForContext(contextId)

    fun getAllContextHours(): Flow<List<ContextHourEntity>> = contextHourDao.getAllContextHours()

    suspend fun saveContextHours(contextId: Long, hours: List<ContextHourEntity>) {
        contextHourDao.deleteForContext(contextId)
        contextHourDao.insertAll(hours.map { it.copy(contextId = contextId) })
    }

    // ── Goal operations ──

    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insert(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    // ── Merge for sync ──

    suspend fun mergeTasks(tasks: List<TaskEntity>) = taskDao.mergeTasks(tasks)
}
