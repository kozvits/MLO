package com.mlo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.domain.PriorityEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    task: TaskEntity,
    allContexts: List<String>,
    allGoals: List<Pair<Long, String>>,
    allTasks: List<TaskEntity>,
    onSave: (TaskEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(task) { mutableStateOf(task.name) }
    var importance by remember(task) { mutableStateOf(task.importance) }
    var urgency by remember(task) { mutableStateOf(task.urgency) }
    var startDate by remember(task) { mutableStateOf(task.startDate) }
    var dueDate by remember(task) { mutableStateOf(task.dueDate) }
    var durationMin by remember(task) { mutableStateOf(task.durationMinutes?.toString() ?: "") }
    var contexts by remember(task) { mutableStateOf(task.contexts) }
    var notes by remember(task) { mutableStateOf(task.notes ?: "") }
    var isRecurring by remember(task) { mutableStateOf(task.isRecurring) }
    var recurringPattern by remember(task) { mutableStateOf(task.recurringPattern ?: "WEEKLY") }
    var weeklyGoalWeight by remember(task) { mutableStateOf(task.weeklyGoalWeight.toString()) }
    var dependencyIds by remember(task) { mutableStateOf(task.dependencyIds) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerField by remember { mutableStateOf("") }

    val taskNameIsEmpty = name.isBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (task.id == 0L) "Новая задача" else "Редактирование")
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    if (task.id != 0L) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                    TextButton(
                        onClick = {
                            val updated = task.copy(
                                name = name.trim(),
                                importance = importance,
                                urgency = urgency,
                                startDate = startDate,
                                dueDate = dueDate,
                                durationMinutes = durationMin.toIntOrNull(),
                                contexts = contexts,
                                notes = notes.ifBlank { null },
                                isRecurring = isRecurring,
                                recurringPattern = if (isRecurring) recurringPattern else null,
                                weeklyGoalWeight = weeklyGoalWeight.toDoubleOrNull() ?: 100.0,
                                dependencyIds = dependencyIds,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        enabled = !taskNameIsEmpty
                    ) {
                        Text("Сохранить")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Name ──
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название задачи") },
                placeholder = { Text("Введите название...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = taskNameIsEmpty
            )

            // ── Importance & Urgency ──
            Text("Приоритет", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Importance slider
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Важность: $importance",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = importance.toFloat(),
                        onValueChange = { importance = it.toInt() },
                        valueRange = 0f..200f,
                        steps = 7
                    )
                    // Gradient scale bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFBBDEFB), // light blue
                                        Color(0xFF1565C0), // mid blue
                                        Color(0xFF0D47A1)  // deep blue
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("200", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0, 25, 50, 75, 100, 125, 150, 175, 200).forEach { v ->
                            TextButton(
                                onClick = { importance = v },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                            ) {
                                Text("$v", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Urgency slider
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Срочность: $urgency",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = urgency.toFloat(),
                        onValueChange = { urgency = it.toInt() },
                        valueRange = 0f..200f,
                        steps = 7
                    )
                    // Gradient scale bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFBBDEFB), // light blue
                                        Color(0xFF1565C0), // mid blue
                                        Color(0xFF0D47A1)  // deep blue
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("200", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0, 25, 50, 75, 100, 125, 150, 175, 200).forEach { v ->
                            TextButton(
                                onClick = { urgency = v },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                            ) {
                                Text("$v", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Dates ──
            Text("Даты и время", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDate?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = {},
                    label = { Text("Дата начала") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerField = "start"; showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Выбрать дату")
                        }
                    }
                )
                OutlinedTextField(
                    value = dueDate?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = {},
                    label = { Text("Срок") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerField = "due"; showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Выбрать дату")
                        }
                    }
                )
            }

            OutlinedTextField(
                value = durationMin,
                onValueChange = { durationMin = it },
                label = { Text("Длительность (минут)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            HorizontalDivider()

            // ── Contexts ──
            Text("Контексты", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            // Context multi-select chips
            if (allContexts.isNotEmpty()) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allContexts.forEach { ctx ->
                        val selected = contexts.contains(ctx)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                contexts = if (selected) {
                                    contexts.split(",")
                                        .filter { it.trim() != ctx }
                                        .joinToString(",")
                                } else {
                                    if (contexts.isBlank()) ctx else "$contexts,$ctx"
                                }
                            },
                            label = { Text(ctx) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Нет контекстов. Создайте их в настройках.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Manual context input
            OutlinedTextField(
                value = contexts,
                onValueChange = { contexts = it },
                label = { Text("Контексты (через запятую)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            // ── Dependencies ──
            Text("Зависимости", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = dependencyIds,
                onValueChange = { dependencyIds = it },
                label = { Text("ID зависимостей (через запятую)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("ID задач, которые должны быть завершены") }
            )

            // Show available tasks for dependencies
            val availableDeps = allTasks.filter { it.id != task.id && it.id != 0L }.take(5)
            if (availableDeps.isNotEmpty()) {
                Text(
                    text = "Доступные задачи:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                availableDeps.forEach { dep ->
                    TextButton(
                        onClick = {
                            val ids = if (dependencyIds.isBlank()) "" else "$dependencyIds,"
                            dependencyIds = "$ids${dep.id}"
                        },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "#${dep.id} ${dep.name.take(30)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Weekly Goal Weight ──
            Text("Цели", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = weeklyGoalWeight,
                onValueChange = { weeklyGoalWeight = it },
                label = { Text("Вес недельной цели (0-200)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            HorizontalDivider()

            // ── Recurring ──
            Text("Повторение", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecurring) "Повторяющаяся задача" else "Не повторяется")
            }

            if (isRecurring) {
                var expanded by remember { mutableStateOf(false) }
                val patterns = listOf("DAILY" to "Ежедневно", "WEEKLY" to "Еженедельно", "MONTHLY" to "Ежемесячно", "YEARLY" to "Ежегодно", "WEEKDAY" to "По будням")
                val selectedPattern = patterns.find { it.first == recurringPattern }?.second ?: recurringPattern

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPattern,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Паттерн") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patterns.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    recurringPattern = value
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Notes ──
            Text("Заметки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // ── Parsed input helper ──
            var parseInput by remember { mutableStateOf("") }
            HorizontalDivider()
            Text("Быстрый ввод", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Пример: Купить билеты @Продукты 2026-06-20 30мин",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = parseInput,
                    onValueChange = { parseInput = it },
                    placeholder = { Text("Введите текст...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val parsed = PriorityEngine.parseTaskInput(parseInput)
                        if (parsed.name.isNotBlank()) {
                            name = parsed.name
                            parsed.dueDate?.let { dueDate = it }
                            parsed.durationMinutes?.let { durationMin = it.toString() }
                            if (parsed.contexts.isNotEmpty()) {
                                contexts = parsed.contexts.joinToString(",")
                            }
                            parseInput = ""
                        }
                    }
                ) {
                    Text("Разобрать")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = when (datePickerField) {
                    "start" -> startDate ?: System.currentTimeMillis()
                    "due" -> dueDate ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                }
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            when (datePickerField) {
                                "start" -> startDate = selected
                                "due" -> dueDate = selected
                            }
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Clear date
                        when (datePickerField) {
                            "start" -> startDate = null
                            "due" -> dueDate = null
                        }
                        showDatePicker = false
                    }) {
                        Text("Очистить")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
