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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mlo.app.data.model.TemplateProfile
import com.mlo.app.ui.viewmodels.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTemplateScreen(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onApplyTemplate: (TemplateProfile) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val templates = state.templates

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Шаблоны профилей") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Выберите шаблон продуктивности для настройки контекстов, целей и меток",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (templates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Нет шаблонов. Перезапустите приложение для загрузки встроенных шаблонов.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(templates) { template ->
                TemplateCard(
                    template = template,
                    onApply = { onApplyTemplate(template) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Info card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Применение шаблона создаст базовые контексты, цели и метки. " +
                                    "Ваши существующие данные не будут удалены.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateProfile,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!template.description.isNullOrBlank()) {
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (template.isBuiltIn) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Встроенный", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            FilledTonalButton(onClick = onApply) {
                Text("Применить")
            }
        }
    }
}
