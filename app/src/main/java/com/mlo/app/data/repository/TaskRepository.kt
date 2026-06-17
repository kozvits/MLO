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
    private val goalDao: GoalDao,
    private val flagDao: FlagDao,
    private val taskFlagDao: TaskFlagDao,
    private val reminderDao: ReminderDao,
    private val profileTemplateDao: ProfileTemplateDao,
    private val viewDao: ViewDao
) {

    // ── Task operations ──

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getRootTasks(): Flow<List<TaskEntity>> = taskDao.getRootTasks()

    fun getChildTasks(parentId: Long): Flow<List<TaskEntity>> = taskDao.getChildTasks(parentId)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun getAllTasksSync(): List<TaskEntity> = taskDao.getAllTasksSync()

    suspend fun getMaxRootSortOrder(): Int? = taskDao.getMaxRootSortOrder()

    suspend fun getMaxChildSortOrder(parentId: Long): Int? = taskDao.getMaxChildSortOrder(parentId)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun toggleComplete(id: Long) {
        val task = taskDao.getTaskById(id) ?: return
        val newStatus = if (task.status == "COMPLETED") "ACTIVE" else "COMPLETED"
        taskDao.updateTask(task.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }

    // ── Context operations ──

    fun getAllContexts(): Flow<List<ContextEntity>> = contextDao.getAllContexts()

    suspend fun getAllContextsSync(): List<ContextEntity> = contextDao.getAllContextsSync()

    suspend fun getContextById(id: Long): ContextEntity? = contextDao.getContextById(id)

    suspend fun insertContext(context: ContextEntity): Long = contextDao.insert(context)

    suspend fun updateContext(context: ContextEntity) = contextDao.update(context)

    suspend fun deleteContext(context: ContextEntity) = contextDao.delete(context)

    // ── Context Hour operations ──

    suspend fun getHoursForContext(contextId: Long): List<ContextHourEntity> =
        contextHourDao.getHoursForContext(contextId)

    fun getAllContextHours(): Flow<List<ContextHourEntity>> =
        contextHourDao.getAllContextHours()

    suspend fun saveContextHours(contextId: Long, hours: List<ContextHourEntity>) {
        contextHourDao.deleteForContext(contextId)
        contextHourDao.insertAll(hours)
    }

    // ── Goal operations ──

    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun getAllGoalsSync(): List<GoalEntity> = goalDao.getAllGoalsSync()

    suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insert(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    // ── Flag operations ──

    fun getAllFlags(): Flow<List<FlagEntity>> = flagDao.getAllFlags()

    suspend fun getAllFlagsSync(): List<FlagEntity> = flagDao.getAllFlagsSync()

    suspend fun insertFlag(flag: FlagEntity): Long = flagDao.insert(flag)

    suspend fun updateFlag(flag: FlagEntity) = flagDao.update(flag)

    suspend fun deleteFlag(flag: FlagEntity) = flagDao.delete(flag)

    suspend fun getFlagsForTask(taskId: Long): List<FlagEntity> =
        taskFlagDao.getFlagsForTask(taskId)

    suspend fun addFlagToTask(taskId: Long, flagId: Long) =
        taskFlagDao.insert(TaskFlagCrossRef(taskId, flagId))

    suspend fun removeFlagFromTask(taskId: Long, flagId: Long) =
        taskFlagDao.deleteByTaskAndFlag(taskId, flagId)

    suspend fun saveTaskFlags(taskId: Long, flagIds: List<Long>) {
        taskFlagDao.deleteAllForTask(taskId)
        flagIds.forEach { fid -> taskFlagDao.insert(TaskFlagCrossRef(taskId, fid)) }
    }

    // ── Reminder operations ──

    suspend fun getRemindersForTask(taskId: Long): List<ReminderEntity> =
        reminderDao.getRemindersForTask(taskId)

    suspend fun insertReminder(reminder: ReminderEntity): Long =
        reminderDao.insert(reminder)

    suspend fun updateReminder(reminder: ReminderEntity) =
        reminderDao.update(reminder)

    suspend fun deleteReminder(reminder: ReminderEntity) =
        reminderDao.delete(reminder)

    suspend fun getEnabledLocationReminders(): List<ReminderEntity> =
        reminderDao.getEnabledLocationReminders()

    suspend fun getOverdueTimeReminders(now: Long): List<ReminderEntity> =
        reminderDao.getOverdueTimeReminders(now)

    // ── Profile Template operations ──

    fun getAllTemplates(): Flow<List<ProfileTemplateEntity>> =
        profileTemplateDao.getAllTemplates()

    suspend fun insertTemplate(template: ProfileTemplateEntity) =
        profileTemplateDao.insert(template)

    suspend fun deleteTemplate(template: ProfileTemplateEntity) =
        profileTemplateDao.delete(template)

    // ── View operations ──

    fun getAllViews(): Flow<List<ViewEntity>> = viewDao.getAllViews()

    suspend fun getAllViewsSync(): List<ViewEntity> = viewDao.getAllViewsSync()

    suspend fun insertView(view: ViewEntity): Long = viewDao.insert(view)

    suspend fun updateView(view: ViewEntity) = viewDao.update(view)

    suspend fun deleteView(view: ViewEntity) = viewDao.delete(view)
}
