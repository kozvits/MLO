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
import com.mlo.app.data.local.*
import com.mlo.app.ui.viewmodels.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    taskViewModel: TaskViewModel,
    taskId: Long?, // null = создание новой задачи, non-null = редактирование
    onDismiss: () -> Unit
) {
    val state by taskViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Find existing task if editing
    val existingTask = remember(taskId, state.allTasks) {
        taskId?.let { id -> state.allTasks.firstOrNull { it.id == id } }
    }

    // ── State fields ──
    var taskName by remember(existingTask) { mutableStateOf(existingTask?.name ?: "") }
    var dueDateMillis by remember(existingTask) { mutableStateOf(existingTask?.dueDate) }
    var durationMinutes by remember(existingTask) { mutableStateOf(existingTask?.durationMinutes?.toString() ?: "") }
    var importance by remember(existingTask) { mutableStateOf(existingTask?.importance ?: 100) }
    var urgency by remember(existingTask) { mutableStateOf(existingTask?.urgency ?: 100) }
    var status by remember(existingTask) { mutableStateOf(existingTask?.status ?: "ACTIVE") }
    var notes by remember(existingTask) { mutableStateOf(existingTask?.notes ?: "") }
    var selectedContext by remember(existingTask) { mutableStateOf(existingTask?.contexts ?: "") }
    var selectedGoalId by remember(existingTask) { mutableStateOf(existingTask?.goalId) }
    var selectedFlags by remember(existingTask) { mutableStateOf(existingTask?.flags ?: "") }
    var parentId by remember(existingTask) { mutableStateOf(existingTask?.parentId) }

    // UI control flags
    var showDatePicker by remember { mutableStateOf(false) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showGoalPicker by remember { mutableStateOf(false) }
    var showFlagPicker by remember { mutableStateOf(false) }
    var showAddChildDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    // Load flags/reminders for existing task
    LaunchedEffect(taskId) {
        if (taskId != null) {
            taskViewModel.loadTaskFlags(taskId)
            taskViewModel.loadTaskReminders(taskId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = { Text(if (taskId == null) "Создание задачи" else "Редактирование задачи") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Scrollable form content ──
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    item {
                        OutlinedTextField(
                            value = taskName,
                            onValueChange = { taskName = it },
                            label = { Text("Название задачи") },
                            placeholder = { Text("Введите название задачи") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Status
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Статус:", style = MaterialTheme.typography.bodyMedium)
                            FilterChip(
                                selected = status == "ACTIVE",
                                onClick = { status = "ACTIVE" },
                                label = { Text("Активна") }
                            )
                            FilterChip(
                                selected = status == "COMPLETED",
                                onClick = { status = "COMPLETED" },
                                label = { Text("Готова") }
                            )
                            FilterChip(
                                selected = status == "DEFERRED",
                                onClick = { status = "DEFERRED" },
                                label = { Text("Отложена") }
                            )
                        }
                    }

                    // Due Date
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = dueDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
                                onValueChange = {},
                                label = { Text("Срок") },
                                placeholder = { Text("Не установлен") },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    if (dueDateMillis != null) {
                                        IconButton(onClick = { dueDateMillis = null }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                        }
                                    }
                                }
                            )
                            IconButton(onClick = { showDatePicker = !showDatePicker }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Выбрать дату")
                            }
                        }
                        // Inline date picker
                        if (showDatePicker) {
                            DatePickerDialog(
                                initialDateMillis = dueDateMillis,
                                onDateSelected = { millis ->
                                    dueDateMillis = millis
                                    showDatePicker = false
                                },
                                onDismiss = { showDatePicker = false }
                            )
                        }
                    }

                    // Duration
                    item {
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Длительность (минут)") },
                            placeholder = { Text("Не установлена") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Importance / Urgency sliders
                    item {
                        Text("Importance: $importance", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = importance.toFloat(),
                            onValueChange = { importance = it.toInt() },
                            valueRange = 0f..200f,
                            steps = 19
                        )
                        Text("Urgency: $urgency", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = urgency.toFloat(),
                            onValueChange = { urgency = it.toInt() },
                            valueRange = 0f..200f,
                            steps = 19
                        )
                    }

                    // Context / Goal / Flags
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Контекст: ${selectedContext.ifEmpty { "не выбран" }}", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { showContextPicker = !showContextPicker }) {
                                    Text("Выбрать")
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                                val goalName = selectedGoalId?.let { id ->
                                    state.goals.firstOrNull { it.id == id }?.name
                                }
                                Text("Цель: ${goalName ?: "не выбрана"}", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { showGoalPicker = !showGoalPicker }) {
                                    Text("Выбрать")
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Флаги: ${selectedFlags.ifEmpty { "не выбраны" }}", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { showFlagPicker = !showFlagPicker }) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }

                    // Context picker dropdown
                    if (showContextPicker && state.contexts.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Выберите контекст", style = MaterialTheme.typography.labelMedium)
                                    state.contexts.forEach { ctx ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            RadioButton(
                                                selected = selectedContext == ctx.name,
                                                onClick = { selectedContext = ctx.name; showContextPicker = false }
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(ctx.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    TextButton(onClick = { selectedContext = ""; showContextPicker = false }) {
                                        Text("Без контекста")
                                    }
                                }
                            }
                        }
                    }

                    // Goal picker dropdown
                    if (showGoalPicker && state.goals.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Выберите цель", style = MaterialTheme.typography.labelMedium)
                                    state.goals.forEach { goal ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            RadioButton(
                                                selected = selectedGoalId == goal.id,
                                                onClick = { selectedGoalId = goal.id; showGoalPicker = false }
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(goal.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    TextButton(onClick = { selectedGoalId = null; showGoalPicker = false }) {
                                        Text("Без цели")
                                    }
                                }
                            }
                        }
                    }

                    // Flag picker dropdown
                    if (showFlagPicker && state.flags.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Выберите флаги", style = MaterialTheme.typography.labelMedium)
                                    val currentFlags = selectedFlags.split(",").filter { it.isNotBlank() }.toMutableList()
                                    state.flags.forEach { flag ->
                                        val isSelected = currentFlags.contains(flag.name)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        currentFlags.add(flag.name)
                                                    } else {
                                                        currentFlags.remove(flag.name)
                                                    }
                                                    selectedFlags = currentFlags.joinToString(",")
                                                }
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(flag.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    TextButton(onClick = { showFlagPicker = false }) {
                                        Text("Готово")
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Заметки") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp),
                            maxLines = 4
                        )
                    }

                    // Reminders section (existing tasks only)
                    if (taskId != null) {
                        val taskReminders = state.reminders[taskId] ?: emptyList()
                        if (taskReminders.isNotEmpty()) {
                            item {
                                HorizontalDivider()
                                Text("Напоминания", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            items(taskReminders) { reminder ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (reminder.type == "TIME") Icons.Default.Alarm else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (reminder.type == "TIME") {
                                                Text(
                                                    text = "Время: ${reminder.triggerTime?.let { dateFormat.format(Date(it)) } ?: "-"}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            } else {
                                                Text(
                                                    text = "Гео: (${"%.4f".format(reminder.locationLat ?: 0.0)}, ${"%.4f".format(reminder.locationLon ?: 0.0)})",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        IconButton(onClick = { taskViewModel.deleteReminder(reminder) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add child (for existing tasks)
                    if (taskId != null && showAddChildDialog) {
                        item {
                            var childName by remember { mutableStateOf("") }
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Новая подзадача", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = childName,
                                        onValueChange = { childName = it },
                                        placeholder = { Text("Название подзадачи") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            onClick = {
                                                if (childName.isNotBlank()) {
                                                    val childTask = TaskEntity(name = childName.trim())
                                                    taskViewModel.insertTask(childTask, taskId)
                                                    childName = ""
                                                    showAddChildDialog = false
                                                }
                                            },
                                            enabled = childName.isNotBlank()
                                        ) {
                                            Text("Добавить")
                                        }
                                        OutlinedButton(onClick = { showAddChildDialog = false }) {
                                            Text("Отмена")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Spacer for bottom buttons
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Bottom action buttons ──
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add child task button (only for existing tasks)
                    if (taskId != null) {
                        OutlinedButton(
                            onClick = { showAddChildDialog = !showAddChildDialog },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Подзадача")
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Delete button (only for existing tasks)
                    if (taskId != null) {
                        OutlinedButton(
                            onClick = {
                                taskViewModel.deleteTask(taskId)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }

                    // Save / Create button
                    Button(
                        onClick = {
                            scope.launch {
                                if (taskId == null) {
                                    val newTask = TaskEntity(
                                        name = taskName.ifBlank { "Новая задача" },
                                        dueDate = dueDateMillis,
                                        durationMinutes = durationMinutes.toIntOrNull(),
                                        importance = importance,
                                        urgency = urgency,
                                        status = status,
                                        contexts = selectedContext,
                                        goalId = selectedGoalId,
                                        flags = selectedFlags,
                                        notes = notes.ifBlank { null },
                                        parentId = parentId
                                    )
                                    taskViewModel.insertTask(newTask, parentId)
                                } else {
                                    val updatedTask = existingTask?.copy(
                                        name = taskName.ifBlank { "Новая задача" },
                                        dueDate = dueDateMillis,
                                        durationMinutes = durationMinutes.toIntOrNull(),
                                        importance = importance,
                                        urgency = urgency,
                                        status = status,
                                        contexts = selectedContext,
                                        goalId = selectedGoalId,
                                        flags = selectedFlags,
                                        notes = notes.ifBlank { null },
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    if (updatedTask != null) {
                                        taskViewModel.updateTask(updatedTask)
                                    }
                                }
                                onDismiss()
                            }
                        },
                        enabled = taskName.isNotBlank() || taskId == null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (taskId == null) Icons.Default.Add else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (taskId == null) "Создать" else "Сохранить")
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите дату и время") },
        text = {
            DatePicker(state = state)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(millis)
                    }
                },
                enabled = state.selectedDateMillis != null
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
