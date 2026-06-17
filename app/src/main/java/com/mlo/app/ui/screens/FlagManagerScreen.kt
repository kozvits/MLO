package com.mlo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.FlagEntity
import com.mlo.app.data.model.FlagModel
import com.mlo.app.ui.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagManagerScreen(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val flags = state.flags
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFlag by remember { mutableStateOf<FlagEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление флагами") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить флаг")
                    }
                }
            )
        }
    ) { padding ->
        if (flags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.OutlinedFlag,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Нет флагов",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить первый флаг")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Default flags hint
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Флаги — это цветовые метки для задач. " +
                                        "Можно назначить несколько флагов на одну задачу.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                items(flags, key = { it.id }) { flag ->
                    FlagItem(
                        flag = flag,
                        onEdit = { editingFlag = flag },
                        onDelete = { viewModel.deleteFlag(flag) }
                    )
                }
            }
        }
    }

    // Add/Edit dialog
    if (showAddDialog || editingFlag != null) {
        FlagEditDialog(
            initial = editingFlag,
            onSave = { entity ->
                if (editingFlag != null) viewModel.updateFlag(entity)
                else viewModel.addFlag(entity)
                showAddDialog = false
                editingFlag = null
            },
            onDismiss = {
                showAddDialog = false
                editingFlag = null
            }
        )
    }
}

@Composable
private fun FlagItem(
    flag: FlagEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(flag.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flag.label.ifEmpty { flag.name },
                    style = MaterialTheme.typography.titleSmall
                )
                if (flag.label.isNotEmpty()) {
                    Text(
                        text = flag.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Изменить")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FlagEditDialog(
    initial: FlagEntity?,
    onSave: (FlagEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var colorValue by remember { mutableStateOf(initial?.color ?: 0xFF1565C0.toInt()) }
    var iconName by remember { mutableStateOf(initial?.iconName ?: "") }

    val presetColors = listOf(
        0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
        0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF1565C0.toInt(), 0xFF03A9F4.toInt(),
        0xFF00BCD4.toInt(), 0xFF009688.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(),
        0xFFFFEB3B.toInt(), 0xFFFFC107.toInt(), 0xFFFF9800.toInt(), 0xFFFF5722.toInt(),
        0xFF795548.toInt(), 0xFF9E9E9E.toInt(), 0xFF607D8B.toInt(), 0xFF000000.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Изменить флаг" else "Новый флаг") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Кодовое имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Отображаемое имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Цвет", style = MaterialTheme.typography.labelMedium)
                // Color picker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetColors.take(10).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (colorValue == color) Modifier
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorValue == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { colorValue = color }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetColors.drop(10).take(10).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                        ) {
                            if (colorValue == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { colorValue = color }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            FlagEntity(
                                id = initial?.id ?: 0,
                                name = name.uppercase().replace(" ", "_"),
                                label = label,
                                color = colorValue,
                                iconName = iconName.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this
