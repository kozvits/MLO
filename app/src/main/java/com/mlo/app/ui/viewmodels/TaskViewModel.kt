package com.mlo.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mlo.app.data.local.*
import com.mlo.app.data.model.GContextModel
import com.mlo.app.data.repository.TaskRepository
import com.mlo.app.domain.PriorityEngine
import com.mlo.app.domain.notification.GeofenceSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state.asStateFlow()

    init {
        loadAllData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAllData() {
        viewModelScope.launch {
            repository.getAllTasks().collect { allTasks ->
                val activeTasks = allTasks
                    .filter { it.status == "ACTIVE" }
                    .map { task ->
                        val score = PriorityEngine.calculatePriorityScore(
                            task = task,
                            allTasks = allTasks
                        )
                        task to score
                    }
                    .sortedByDescending { it.second }

                _state.update {
                    it.copy(allTasks = allTasks, activeTasks = activeTasks)
                }
            }
        }

        viewModelScope.launch {
            repository.getAllContexts().collect { ctxList ->
                val models = ctxList.map { GContextModel.fromEntity(it) }
                _state.update { it.copy(contexts = models) }
            }
        }

        viewModelScope.launch {
            repository.getAllGoals().collect { goals ->
                _state.update { it.copy(goals = goals) }
            }
        }

        viewModelScope.launch {
            repository.getAllFlags().collect { flags ->
                _state.update { it.copy(flags = flags) }
            }
        }
    }

    // ── Task CRUD ──

    fun toggleComplete(taskId: Long) {
        viewModelScope.launch { repository.toggleComplete(taskId) }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { repository.deleteTaskById(taskId) }
    }

    fun insertTask(task: TaskEntity, parentId: Long?) {
        viewModelScope.launch {
            val sortOrder = if (parentId == null) {
                (repository.getMaxRootSortOrder() ?: -1) + 1
            } else {
                (repository.getMaxChildSortOrder(parentId) ?: -1) + 1
            }
            val newTask = task.copy(
                parentId = parentId,
                sortOrder = sortOrder,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertTask(newTask)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    suspend fun getTaskById(id: Long): TaskEntity? = repository.getTaskById(id)

    // ── Flags for tasks ──

    fun loadTaskFlags(taskId: Long) {
        viewModelScope.launch {
            val flags = repository.getFlagsForTask(taskId)
            val map = _state.value.taskFlags.toMutableMap()
            map[taskId] = flags
            _state.update { it.copy(taskFlags = map) }
        }
    }

    fun addFlagToTask(taskId: Long, flagId: Long) {
        viewModelScope.launch { repository.addFlagToTask(taskId, flagId) }
    }

    fun removeFlagFromTask(taskId: Long, flagId: Long) {
        viewModelScope.launch { repository.removeFlagFromTask(taskId, flagId) }
    }

    fun saveTaskFlags(taskId: Long, flagIds: List<Long>) {
        viewModelScope.launch { repository.saveTaskFlags(taskId, flagIds) }
    }

    // ── Reminders ──

    fun loadTaskReminders(taskId: Long) {
        viewModelScope.launch {
            val reminders = repository.getRemindersForTask(taskId)
            val map = _state.value.reminders.toMutableMap()
            map[taskId] = reminders
            _state.update { it.copy(reminders = map) }
        }
    }

    fun addTimeReminder(taskId: Long, triggerTime: Long) {
        viewModelScope.launch {
            repository.insertReminder(
                ReminderEntity(
                    taskId = taskId,
                    type = "TIME",
                    triggerTime = triggerTime
                )
            )
            loadTaskReminders(taskId)
        }
    }

    fun addLocationReminder(
        taskId: Long,
        lat: Double,
        lon: Double,
        radiusMeters: Int = 100
    ) {
        viewModelScope.launch {
            repository.insertReminder(
                ReminderEntity(
                    taskId = taskId,
                    type = "LOCATION",
                    locationLat = lat,
                    locationLon = lon,
                    locationRadiusMeters = radiusMeters
                )
            )
            val ctx = getApplication<Application>()
            GeofenceSyncWorker.schedule(ctx)
            loadTaskReminders(taskId)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            loadTaskReminders(reminder.taskId)
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
            loadTaskReminders(reminder.taskId)
        }
    }

    // ── Tree expansion ──

    fun toggleExpand(rootId: Long) {
        _state.update {
            val expanded = it.expandedTaskIds.toMutableSet()
            if (expanded.contains(rootId)) expanded.remove(rootId)
            else expanded.add(rootId)
            it.copy(expandedTaskIds = expanded)
        }
    }

    fun expandAll() {
        _state.update {
            val allIds = it.allTasks.map { task -> task.id }.toSet()
            it.copy(expandedTaskIds = allIds + 0L)
        }
    }

    fun collapseAll() {
        _state.update { it.copy(expandedTaskIds = setOf(0L)) }
    }

    fun focusTask(taskId: Long) {
        _state.update {
            it.copy(
                selectedTaskId = taskId,
                focusedTaskId = if (it.focusedTaskId == taskId) null else taskId
            )
        }
    }

    fun selectTask(taskId: Long) {
        _state.update { it.copy(selectedTaskId = taskId) }
    }
}
