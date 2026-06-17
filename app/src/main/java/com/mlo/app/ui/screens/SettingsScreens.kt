package com.mlo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.ContextEntity
import com.mlo.app.data.local.ContextHourEntity
import com.mlo.app.data.local.GoalEntity
import com.mlo.app.ui.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onRequestImportCsv: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Dropbox Sync section
                Text("Синхронизация", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                if (state.isDropboxConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dropbox подключён")
                            state.lastSyncTime?.let {
                                Text(
                                    text = "Последняя синхронизация: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.syncNow() },
                            enabled = !state.isDropboxSyncing
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (state.isDropboxSyncing) "Синхронизация..." else "Синхронизировать")
                        }
                        TextButton(onClick = { viewModel.disconnectDropbox() }) {
                            Text("Отключить")
                        }
                    }
                } else if (state.isDropboxSyncing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Подключение...")
                    }
                } else {
                    TextButton(onClick = { viewModel.connectDropbox() }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Подключить Dropbox")
                    }
                }

                state.dropboxSyncError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                // Sync interval
                Text("Интервал синхронизации", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val intervalLabel = when (state.syncIntervalMinutes) {
                    0 -> "Вручную"
                    15 -> "15 мин"
                    30 -> "30 мин"
                    60 -> "1 час"
                    240 -> "4 часа"
                    else -> "${state.syncIntervalMinutes} мин"
                }
                Text(
                    text = "Текущий: $intervalLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Inline interval selector
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0 to "Вручную", 15 to "15м", 30 to "30м", 60 to "1ч", 240 to "4ч").forEach { (min, label) ->
                        FilterChip(
                            selected = state.syncIntervalMinutes == min,
                            onClick = { viewModel.setSyncInterval(min) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider()

                // Import
                Text("Импорт", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRequestImportCsv()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Импорт из Toodledo CSV")
                }

                HorizontalDivider()

                // About
                Text(
                    text = "MyOrganizer v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextManagerDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var newContextName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление контекстами") },
        text = {
            Column {
                // Add new context
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newContextName,
                        onValueChange = { newContextName = it },
                        placeholder = { Text("Новый контекст (напр. @Дом)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newContextName.isNotBlank()) {
                            viewModel.createContext(newContextName.trim())
                            newContextName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.contexts.isEmpty()) {
                    Text(
                        text = "Нет контекстов. Добавьте @Дом, @Офис, @Компьютер и т.д.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.contexts) { ctx ->
                            var isEditing by remember { mutableStateOf(false) }
                            var editName by remember(ctx) { mutableStateOf(ctx.name) }

                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editName,
                                            onValueChange = { editName = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        IconButton(onClick = {
                                            viewModel.updateContext(ctx.copy(name = editName))
                                            isEditing = false
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Сохранить")
                                        }
                                    } else {
                                        Text(
                                            text = ctx.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { isEditing = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteContext(ctx) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalManagerDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var newGoalName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление целями") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newGoalName,
                        onValueChange = { newGoalName = it },
                        placeholder = { Text("Новая цель") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newGoalName.isNotBlank()) {
                            viewModel.createGoal(newGoalName.trim())
                            newGoalName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.goals.isEmpty()) {
                    Text(
                        text = "Нет целей",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.goals) { goal ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = goal.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.deleteGoal(goal) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        },
        modifier = modifier
    )
}
