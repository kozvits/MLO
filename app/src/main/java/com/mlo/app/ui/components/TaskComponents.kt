package com.mlo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlo.app.data.local.TaskEntity
import com.mlo.app.data.model.TaskStatus
import com.mlo.app.domain.PriorityEngine
import com.mlo.app.ui.theme.*

@Composable
fun TaskTreeItem(
    task: TaskEntity,
    depth: Int,
    allTasks: List<TaskEntity>,
    isExpanded: Boolean,
    hasChildren: Boolean,
    isSelected: Boolean,
    isFocused: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveImp = remember(task, allTasks) {
        PriorityEngine.calculateEffectiveImportance(task, allTasks)
    }
    val effectiveUrg = remember(task, allTasks) {
        PriorityEngine.calculateEffectiveUrgency(task, allTasks)
    }

    val score = remember(task, allTasks) {
        PriorityEngine.calculatePriorityScore(task, allTasks)
    }

    val isCompleted = task.status == "COMPLETED"
    val priorityColor = when {
        isCompleted -> TaskCompleted
        score >= 150 -> TaskPriorityCritical
        score >= 100 -> TaskPriorityHigh
        score >= 60 -> TaskPriorityMedium
        else -> TaskPriorityLow
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 24).dp, end = 4.dp, top = 1.dp, bottom = 1.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isSelected -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Expand/Collapse button
            if (hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Complete checkbox
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = GreenActive,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name.ifBlank { "(без названия)" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) TaskCompleted else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Context chips + metadata row
                if (task.contexts.isNotBlank() || task.dueDate != null || task.durationMinutes != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.contexts.isNotBlank()) {
                            task.contexts.split(",").take(2).forEach { ctx ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = ctx.trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        task.dueDate?.let { due ->
                            val isOverdue = due < System.currentTimeMillis()
                            Text(
                                text = formatTimestamp(due),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) RedOverdue else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Priority score badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = priorityColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "%.0f".format(score),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TodoTaskItem(
    task: TaskEntity,
    score: Double,
    allTasks: List<TaskEntity>,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = task.dueDate != null && task.dueDate < System.currentTimeMillis()
    val priorityColor = when {
        score >= 150 -> TaskPriorityCritical
        score >= 100 -> TaskPriorityHigh
        score >= 60 -> TaskPriorityMedium
        else -> TaskPriorityLow
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue)
                RedOverdue.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(GreenActive)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Checkbox(
                checked = false,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = GreenActive)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    task.dueDate?.let { due ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isOverdue) RedOverdue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatTimestamp(due),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) RedOverdue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (task.durationMinutes != null) {
                        Text(
                            text = "${task.durationMinutes}мин",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Score + priority bar
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "%.0f".format(score),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (isOverdue) {
                    Text(
                        text = "ПРОСРОЧЕНО",
                        style = MaterialTheme.typography.labelSmall,
                        color = RedOverdue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
