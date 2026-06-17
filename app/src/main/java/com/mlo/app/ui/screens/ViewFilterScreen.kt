package com.mlo.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.ViewEntity
import com.mlo.app.data.model.ViewFilter
import com.mlo.app.ui.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFilterScreen(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onApplyFilter: (ViewFilter) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val contexts = state.contexts
    val flags = state.flags
    val savedViews = state.savedViews

    var currentFilter by remember { mutableStateOf(ViewFilter()) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фильтры и группировка") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { currentFilter = ViewFilter() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Сбросить")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saved views section
            if (savedViews.isNotEmpty()) {
                item {
                    Text(
                        "Сохранённые виды",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(savedViews) { view ->
                    SavedViewChip(view, onClick = {
                        currentFilter = ViewFilter.fromEntity(view)
                        onApplyFilter(currentFilter)
                    }, onDelete = {
                        viewModel.deleteView(view)
                    })
                }
                item { HorizontalDivider() }
            }

            // Status filter
            item {
                Text("Статус", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentFilter.statusFilter == "ACTIVE",
                        onClick = {
                            currentFilter = currentFilter.copy(
                                statusFilter = if (currentFilter.statusFilter == "ACTIVE") "" else "ACTIVE"
                            )
                        },
                        label = { Text("Активные") }
                    )
                    FilterChip(
                        selected = currentFilter.showCompleted,
                        onClick = { currentFilter = currentFilter.copy(showCompleted = !currentFilter.showCompleted) },
                        label = { Text("Завершённые") }
                    )
                }
            }

            // Context filter
            item {
                Text("Контекст", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contexts.take(10).forEach { context ->
                        val isSelected = currentFilter.contextIds.contains(context.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentFilter = currentFilter.copy(
                                    contextIds = if (isSelected) currentFilter.contextIds - context.id
                                    else currentFilter.contextIds + context.id
                                )
                            },
                            label = { Text(context.label.ifEmpty { context.name }) }
                        )
                    }
                }
            }

            // Flag filter
            item {
                Text("Метки", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    flags.forEach { flag ->
                        val isSelected = currentFilter.flagIds.contains(flag.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentFilter = currentFilter.copy(
                                    flagIds = if (isSelected) currentFilter.flagIds - flag.id
                                    else currentFilter.flagIds + flag.id
                                )
                            },
                            label = { Text(flag.label.ifEmpty { flag.name }) }
                        )
                    }
                }
            }

            // Date filters
            item {
                Text("Дата", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentFilter.hasDueDate,
                        onClick = { currentFilter = currentFilter.copy(hasDueDate = !currentFilter.hasDueDate) },
                        label = { Text("С датой") }
                    )
                    FilterChip(
                        selected = currentFilter.isOverdue,
                        onClick = { currentFilter = currentFilter.copy(isOverdue = !currentFilter.isOverdue) },
                        label = { Text("Просрочено") }
                    )
                }
            }

            // Group by
            item {
                Text("Группировка", style = MaterialTheme.typography.titleSmall)
                val groupOptions = listOf(
                    "NONE" to "Без групп",
                    "CONTEXT" to "По контексту",
                    "GOAL" to "По цели",
                    "STATUS" to "По статусу",
                    "FLAG" to "По метке",
                    "MONTH" to "По месяцу"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = currentFilter.groupBy == value,
                            onClick = { currentFilter = currentFilter.copy(groupBy = value) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Sort by
            item {
                Text("Сортировка", style = MaterialTheme.typography.titleSmall)
                val sortOptions = listOf(
                    "PRIORITY" to "Приоритет",
                    "DUE_DATE" to "Срок",
                    "CREATED_DATE" to "Дата создания",
                    "NAME" to "Название"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = currentFilter.sortBy == value,
                            onClick = {
                                currentFilter = currentFilter.copy(
                                    sortBy = value,
                                    isAscending = if (currentFilter.sortBy == value) !currentFilter.isAscending else false
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentFilter.isAscending,
                        onClick = { currentFilter = currentFilter.copy(isAscending = !currentFilter.isAscending) },
                        label = { Text("По возрастанию") }
                    )
                }
            }

            // Apply button
            item {
                Button(
                    onClick = { onApplyFilter(currentFilter) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Применить фильтр")
                }
            }
        }
    }

    // Save dialog
    if (showSaveDialog) {
        SaveViewDialog(
            onSave = { name ->
                viewModel.saveView(currentFilter.copy(name = name))
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun SavedViewChip(view: ViewEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                view.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SaveViewDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сохранить вид") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название вида") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
