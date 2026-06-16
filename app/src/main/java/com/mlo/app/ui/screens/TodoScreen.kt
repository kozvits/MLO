package com.mlo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.ui.components.TodoTaskItem
import com.mlo.app.ui.theme.GreenActive
import com.mlo.app.ui.viewmodels.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TaskViewModel,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Refresh active tasks periodically
    var refreshTrigger by remember { mutableStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do — Активные задачи") },
                actions = {
                    TextButton(onClick = { refreshTrigger = System.currentTimeMillis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Обновить")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        val activeTasks = state.activeTasks

        if (activeTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = GreenActive.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нет активных задач",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Все задачи завершены или ожидают условий",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Summary bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Активных: ${activeTasks.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    val avgScore = if (activeTasks.isNotEmpty())
                        activeTasks.map { it.second }.average() else 0.0
                    Text(
                        text = "Средний приоритет: %.0f".format(avgScore),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)
            ) {
                items(
                    items = activeTasks,
                    key = { (task, _) -> task.id }
                ) { (task, score) ->
                    TodoTaskItem(
                        task = task,
                        score = score,
                        allTasks = state.allTasks,
                        onClick = { onEditTask(task.id) },
                        onToggleComplete = { viewModel.toggleComplete(task.id) }
                    )
                }
            }
        }
    }
}
