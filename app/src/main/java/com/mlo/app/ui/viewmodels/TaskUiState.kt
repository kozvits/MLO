package com.mlo.app.ui.viewmodels

import com.mlo.app.data.local.*
import com.mlo.app.data.model.GContextModel
import com.mlo.app.domain.PriorityEngine

data class TaskUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val activeTasks: List<Pair<TaskEntity, Double>> = emptyList(),
    val contexts: List<GContextModel> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val flags: List<FlagEntity> = emptyList(),
    val taskFlags: Map<Long, List<FlagEntity>> = emptyMap(),
    val reminders: Map<Long, List<ReminderEntity>> = emptyMap(),
    val editingTaskId: Long? = null,
    val selectedTaskId: Long? = null,
    val focusedTaskId: Long? = null,
    val expandedTaskIds: Set<Long> = setOf(0L)
) {
    val flatTree: List<Pair<TaskEntity, Int>> by lazy {
        flattenTree(allTasks.filter { it.parentId == null }, 0)
    }

    private fun flattenTree(tasks: List<TaskEntity>, depth: Int): List<Pair<TaskEntity, Int>> {
        val result = mutableListOf<Pair<TaskEntity, Int>>()
        for (task in tasks.sortedBy { it.sortOrder }) {
            result.add(task to depth)
            if (expandedTaskIds.contains(task.id)) {
                val children = allTasks.filter { it.parentId == task.id }
                result.addAll(flattenTree(children, depth + 1))
            }
        }
        return result
    }
}
