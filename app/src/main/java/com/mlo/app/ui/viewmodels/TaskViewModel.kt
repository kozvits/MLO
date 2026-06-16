package com.mlo.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.data.model.*
import com.mlo.app.data.repository.TaskRepository
import com.mlo.app.domain.PriorityEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val flatTree: List<Pair<TaskEntity, Int>> = emptyList(),
    val activeTasks: List<Pair<TaskEntity, Double>> = emptyList(),
    val selectedTaskId: Long? = null,
    val expandedTaskIds: Set<Long> = emptySet(),
    val focusedTaskId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val priorityConfig: PriorityConfig = PriorityConfig(),
    val sortBy: SortBy = SortBy.PRIORITY,
    val groupBy: GroupBy = GroupBy.NONE,
    val editorTask: TaskEntity? = null,
    val showEditor: Boolean = false
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.getAllTasks().collect { tasks ->
                _state.update { current ->
                    val flat = PriorityEngine.buildFlatTree(tasks)
                    val active = if (current.searchQuery.isBlank()) {
                        PriorityEngine.getActiveTasksScored(tasks, current.priorityConfig)
                    } else {
                        emptyList()
                    }

                    current.copy(
                        allTasks = tasks,
                        flatTree = flat,
                        activeTasks = active,
                        isLoading = false
                    )
                }
            }
        }
    }

    // ── Task CRUD ──

    fun createTask(name: String, parentId: Long? = null) {
        viewModelScope.launch {
            val sortOrder = if (parentId != null) {
                repository.getMaxChildSortOrder(parentId) + 1
            } else {
                repository.getMaxRootSortOrder() + 1
            }

            val task = TaskEntity(
                name = name,
                parentId = parentId,
                sortOrder = sortOrder
            )
            repository.insertTask(task)
        }
    }

    fun createTaskWithDetails(
        name: String,
        parentId: Long? = null,
        startDate: Long? = null,
        dueDate: Long? = null,
        durationMinutes: Int? = null,
        importance: Int = 100,
        urgency: Int = 100,
        contexts: List<String> = emptyList(),
        notes: String? = null
    ) {
        viewModelScope.launch {
            val sortOrder = if (parentId != null) {
                repository.getMaxChildSortOrder(parentId) + 1
            } else {
                repository.getMaxRootSortOrder() + 1
            }

            val task = TaskEntity(
                name = name,
                parentId = parentId,
                startDate = startDate,
                dueDate = dueDate,
                durationMinutes = durationMinutes,
                importance = importance,
                urgency = urgency,
                contexts = contexts.joinToString(","),
                notes = notes,
                sortOrder = sortOrder
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
            closeEditor()
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTaskById(taskId)
        }
    }

    fun toggleComplete(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch

            if (task.status == "COMPLETED") {
                repository.toggleComplete(taskId)
            } else {
                repository.toggleComplete(taskId)

                // Handle recurring tasks
                if (task.isRecurring && task.recurringPattern != null) {
                    val nextDate = PriorityEngine.computeNextOccurrence(
                        task.recurringPattern,
                        task.dueDate ?: System.currentTimeMillis()
                    )
                    val newTask = task.copy(
                        id = 0,
                        status = "ACTIVE",
                        dueDate = nextDate,
                        startDate = System.currentTimeMillis(),
                        parentId = task.parentId
                    )
                    repository.insertTask(newTask)
                }
            }
        }
    }

    fun moveTask(taskId: Long, newParentId: Long?) {
        viewModelScope.launch {
            val sortOrder = if (newParentId != null) {
                repository.getMaxChildSortOrder(newParentId) + 1
            } else {
                repository.getMaxRootSortOrder() + 1
            }
            repository.moveTask(taskId, newParentId, sortOrder)
        }
    }

    // ── Editor ──

    fun openEditor(taskId: Long? = null) {
        viewModelScope.launch {
            val task = if (taskId != null) repository.getTaskById(taskId) else null
            _state.update { it.copy(editorTask = task, showEditor = true) }
        }
    }

    fun openNewTaskEditor(parentId: Long? = null) {
        val defaults = if (parentId != null) {
            TaskEntity(name = "", parentId = parentId, sortOrder = 0)
        } else {
            TaskEntity(name = "")
        }
        _state.update { it.copy(editorTask = defaults, showEditor = true) }
    }

    fun closeEditor() {
        _state.update { it.copy(editorTask = null, showEditor = false) }
    }

    // ── Parsed input ──

    fun createTaskFromParsedInput(input: String, parentId: Long? = null) {
        val parsed = PriorityEngine.parseTaskInput(input)
        createTaskWithDetails(
            name = parsed.name,
            parentId = parentId,
            dueDate = parsed.dueDate,
            durationMinutes = parsed.durationMinutes,
            contexts = parsed.contexts
        )
    }

    // ── Focus / Expand ──

    fun focusTask(taskId: Long) {
        _state.update { it.copy(focusedTaskId = taskId) }
    }

    fun clearFocus() {
        _state.update { it.copy(focusedTaskId = null) }
    }

    fun toggleExpand(taskId: Long) {
        _state.update { current ->
            val expanded = current.expandedTaskIds.toMutableSet()
            if (expanded.contains(taskId)) expanded.remove(taskId) else expanded.add(taskId)
            current.copy(expandedTaskIds = expanded)
        }
    }

    fun expandAll() {
        _state.update { current ->
            current.copy(expandedTaskIds = current.allTasks.map { it.id }.toSet())
        }
    }

    fun collapseAll() {
        _state.update { it.copy(expandedTaskIds = emptySet()) }
    }

    // ── Search ──

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    // ── Priority Config ──

    fun updatePriorityConfig(config: PriorityConfig) {
        _state.update { it.copy(priorityConfig = config) }
    }
}
