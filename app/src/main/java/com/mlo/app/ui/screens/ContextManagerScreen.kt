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
import androidx.compose.ui.unit.dp
import com.mlo.app.data.local.ContextEntity
import com.mlo.app.data.model.GContextModel
import com.mlo.app.ui.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextManagerScreen(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val contexts = state.contexts
    var newContextName by remember { mutableStateOf("") }
    var editingContext by remember { mutableStateOf<GContextModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление контекстами") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Add new context row ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newContextName,
                            onValueChange = { newContextName = it },
                            placeholder = { Text("Новый контекст (напр. @Дом)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                if (newContextName.isNotBlank()) {
                                    viewModel.createContext(newContextName.trim())
                                    newContextName = ""
                                }
                            },
                            enabled = newContextName.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Добавить")
                        }
                    }
                }
            }

            // ── Empty state ──
            if (contexts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Нет контекстов",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Введите название контекста выше и нажмите «Добавить»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ── Context list ──
            items(contexts) { context ->
                ContextCard(
                    model = context,
                    onEdit = { editingContext = context },
                    onDelete = { viewModel.deleteContext(context) }
                )
            }
        }
    }

    // ── Edit Context Dialog ──

    if (editingContext != null) {
        val context = editingContext!!
        var editName by remember(context.id) { mutableStateOf(context.name) }
        var editLabel by remember(context.id) { mutableStateOf(context.label) }

        AlertDialog(
            onDismissRequest = { editingContext = null },
            title = { Text("Изменить контекст") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Имя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Отображаемое название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.saveContext(
                                ContextEntity(
                                    id = context.id,
                                    name = editName.trim(),
                                    label = editLabel.trim(),
                                    color = context.color,
                                    iconName = context.iconName,
                                    locationLat = context.locationLat,
                                    locationLon = context.locationLon,
                                    locationRadiusMeters = context.locationRadiusMeters,
                                    isLocationOnly = context.isLocationOnly,
                                    includeIds = context.includeIds.joinToString(",")
                                )
                            )
                            editingContext = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingContext = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ContextCard(
    model: GContextModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location indicator
            if (model.isLocationOnly) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    model.label.ifEmpty { model.name },
                    style = MaterialTheme.typography.titleSmall
                )
                if (model.locationLat != null && model.locationLon != null) {
                    Text(
                        "📍 %.4f, %.4f".format(model.locationLat, model.locationLon),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (model.isLocationOnly) {
                    Text(
                        "Активируется только по геопозиции",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Open indicator
            if (model.isOpenNow) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Открыт",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
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
