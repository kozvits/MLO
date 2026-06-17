package com.mlo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.ui.components.TodoTaskItem
import com.mlo.app.ui.viewmodels.TaskViewModel

@Composable
fun TodoScreen(
    viewModel: TaskViewModel,
    onEditTask: (Long) -> Unit,
    onAddSubTask: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Tasks sorted by priority score descending
    val sortedTasks = remember(state.activeTasks) {
        state.activeTasks.sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
    ) {
        if (sortedTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет активных задач",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(
            items = sortedTasks,
            key = { (task, _) -> task.id }
        ) { (task, score) ->
            TodoTaskItem(
                task = task,
                score = score,
                allTasks = state.allTasks,
                onClick = { onEditTask(task.id) },
                onToggleComplete = { viewModel.toggleComplete(task.id) },
                onAddSubTask = { onAddSubTask(task) },
                onDeleteTask = { onDeleteTask(task.id) }
            )
        }
    }
}
