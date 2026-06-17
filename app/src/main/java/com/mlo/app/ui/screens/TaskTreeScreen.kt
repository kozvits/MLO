package com.mlo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.ui.components.TaskTreeItem
import com.mlo.app.ui.viewmodels.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTreeScreen(
    viewModel: TaskViewModel,
    onAddTask: (Long?) -> Unit,
    onEditTask: (Long) -> Unit,
    onAddSubTask: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // Filter tree by search query
    val filteredTree = remember(state.flatTree, searchQuery) {
        if (searchQuery.isBlank()) state.flatTree
        else state.flatTree.filter { (task, _) ->
            task.name.contains(searchQuery, ignoreCase = true) ||
                    task.contexts.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Схема")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = { viewModel.expandAll() }) {
                        Icon(Icons.Default.UnfoldMore, contentDescription = "Развернуть всё")
                    }
                    IconButton(onClick = { viewModel.collapseAll() }) {
                        Icon(Icons.Default.UnfoldLess, contentDescription = "Свернуть всё")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTask(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        },
        modifier = modifier
    ) { padding ->
        if (filteredTree.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Нет задач",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите + чтобы добавить",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 4.dp, end = 4.dp, top = 4.dp, bottom = 80.dp
                )
            ) {
                items(
                    items = filteredTree,
                    key = { (task, _) -> task.id }
                ) { (task, depth) ->
                    val allTasks = state.allTasks
                    val children = allTasks.filter { it.parentId == task.id }
                    val hasChildren = children.isNotEmpty()
                    val isExpanded = state.expandedTaskIds.contains(task.id) || state.focusedTaskId == task.id

                    TaskTreeItem(
                        task = task,
                        depth = depth,
                        allTasks = allTasks,
                        isExpanded = isExpanded,
                        hasChildren = hasChildren,
                        isSelected = state.selectedTaskId == task.id,
                        isFocused = state.focusedTaskId == task.id,
                        onToggleExpand = { viewModel.toggleExpand(task.id) },
                        onClick = { onEditTask(task.id) },
                        onLongClick = { viewModel.focusTask(task.id) },
                        onToggleComplete = { viewModel.toggleComplete(task.id) },
                        onAddSubTask = { onAddSubTask(task) },
                        onDeleteTask = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}
