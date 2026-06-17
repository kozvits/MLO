package com.mlo.app.domain

import com.mlo.app.data.local.TaskEntity
import com.mlo.app.data.model.PriorityConfig
import java.util.Calendar

/**
 * Core priority calculation engine for MLO-style scoring.
 * Implements effective importance/urgency inheritance and
 * computed-score-based prioritization.
 */
object PriorityEngine {

    /**
     * Calculate effective importance by walking up the parent chain.
     * effective = parentEffective * task.importance / 100
     */
    fun calculateEffectiveImportance(
        task: TaskEntity,
        allTasks: List<TaskEntity>,
        visited: MutableSet<Long> = mutableSetOf()
    ): Int {
        if (task.parentId == null) return task.importance.coerceIn(0, 200)

        // Guard against circular references
        if (!visited.add(task.id)) return task.importance.coerceIn(0, 200)

        val parent = allTasks.find { it.id == task.parentId } ?: return task.importance.coerceIn(0, 200)
        val parentEffective = calculateEffectiveImportance(parent, allTasks, visited)
        return (parentEffective * task.importance / 100).coerceIn(0, 200)
    }

    /**
     * Calculate effective urgency by walking up the parent chain.
     * Same logic as importance inheritance.
     */
    fun calculateEffectiveUrgency(
        task: TaskEntity,
        allTasks: List<TaskEntity>,
        visited: MutableSet<Long> = mutableSetOf()
    ): Int {
        if (task.parentId == null) return task.urgency.coerceIn(0, 200)

        if (!visited.add(task.id)) return task.urgency.coerceIn(0, 200)

        val parent = allTasks.find { it.id == task.parentId } ?: return task.urgency.coerceIn(0, 200)
        val parentEffective = calculateEffectiveUrgency(parent, allTasks, visited)
        return (parentEffective * task.urgency / 100).coerceIn(0, 200)
    }

    /**
     * Compute the time factor T for the priority score.
     * - Overdue: boost by hours past due date
     * - Has due date: 100 - remaining hours
     * - No due date: 100 (neutral)
     */
    fun calculateTimeFactor(
        task: TaskEntity,
        now: Long = System.currentTimeMillis()
    ): Double {
        return when {
            task.dueDate != null && now > task.dueDate -> {
                // Overdue — boost
                val hoursOverdue = (now - task.dueDate) / 1000.0 / 60.0 / 60.0
                (200.0 + hoursOverdue * 10.0).coerceIn(0.0, 500.0)
            }
            task.dueDate != null -> {
                val remaining = (task.dueDate - now) / 1000.0 / 60.0 / 60.0
                (100.0 - remaining).coerceIn(0.0, 200.0)
            }
            else -> 100.0 // neutral
        }
    }

    /**
     * Calculate priority score using the MLO computed-score algorithm.
     * Score = wI*I + wU*U + wT*T + wG*G
     */
    fun calculatePriorityScore(
        task: TaskEntity,
        allTasks: List<TaskEntity>,
        config: PriorityConfig = PriorityConfig(),
        now: Long = System.currentTimeMillis()
    ): Double {
        val I = calculateEffectiveImportance(task, allTasks).toDouble()
        val U = calculateEffectiveUrgency(task, allTasks).toDouble()
        val T = calculateTimeFactor(task, now)
        val G = task.weeklyGoalWeight

        return config.wI * I + config.wU * U + config.wT * T + config.wG * G
    }

    /**
     * Determine if a task is currently active (ready to work on).
     */
    fun isTaskActive(
        task: TaskEntity,
        allTasks: List<TaskEntity>,
        now: Long = System.currentTimeMillis(),
        activeContextIds: Set<Long> = emptySet(),
        contextHoursMap: Map<Long, List<Pair<Int, Int>>> = emptyMap()
    ): Boolean {
        // 1. Status check
        if (task.status != "ACTIVE") return false

        // 2. Start date check
        if (task.startDate != null && task.startDate > now) return false

        // 3. Dependencies check — all must be completed
        if (task.dependencyIds.isNotBlank()) {
            val depIds = task.dependencyIds.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
            if (depIds.isNotEmpty()) {
                val completedIds = allTasks
                    .filter { it.status == "COMPLETED" }
                    .map { it.id }
                    .toSet()
                if (depIds.any { it !in completedIds }) return false
            }
        }

        // 4. Context hours check (if configured)
        if (activeContextIds.isNotEmpty() && task.contexts.isNotBlank()) {
            val taskContextNames = task.contexts.split(",")
                .map { it.trim() }
            // If any task context is in the "active" set, consider it available
            // (This is simplified — real MLO checks hours per context)
        }

        return true
    }

    /**
     * Filter tasks by their active status and return scored list.
     */
    fun getActiveTasksScored(
        tasks: List<TaskEntity>,
        config: PriorityConfig = PriorityConfig(),
        now: Long = System.currentTimeMillis()
    ): List<Pair<TaskEntity, Double>> {
        return tasks
            .filter { isTaskActive(it, tasks, now) }
            .map { it to calculatePriorityScore(it, tasks, config, now) }
            .sortedByDescending { it.second }
    }

    /**
     * Build a flat tree from hierarchical tasks with depth information.
     */
    fun buildFlatTree(
        tasks: List<TaskEntity>,
        parentId: Long? = null,
        depth: Int = 0
    ): List<Pair<TaskEntity, Int>> {
        val result = mutableListOf<Pair<TaskEntity, Int>>()
        val children = tasks.filter { it.parentId == parentId }
            .sortedBy { it.sortOrder }

        for (child in children) {
            result.add(child to depth)
            result.addAll(buildFlatTree(tasks, child.id, depth + 1))
        }

        return result
    }

    /**
     * Recurring task helper — compute next occurrence date.
     */
    fun computeNextOccurrence(pattern: String, fromDate: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromDate }
        return when (pattern.uppercase()) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            "WEEKLY" -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.timeInMillis
            }
            "MONTHLY" -> {
                cal.add(Calendar.MONTH, 1)
                cal.timeInMillis
            }
            "YEARLY" -> {
                cal.add(Calendar.YEAR, 1)
                cal.timeInMillis
            }
            "WEEKDAY" -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                         cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                cal.timeInMillis
            }
            else -> fromDate
        }
    }

    /**
     * Parse a natural-language task input into structured fields.
     * Example: "Buy tickets @Errands 2026-06-20 30min"
     */
    fun parseTaskInput(input: String): ParsedTaskInput {
        var remaining = input.trim()

        // Extract due date: YYYY-MM-DD or YYYY/MM/DD
        val dateRegex = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b""")
        val dateMatch = dateRegex.find(remaining)
        val dueDate: Long? = dateMatch?.let {
            try {
                val parts = it.value.replace("/", "-").split("-")
                val cal = Calendar.getInstance()
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                cal.timeInMillis
            } catch (e: Exception) { null }
        }
        if (dateMatch != null) remaining = remaining.replace(dateMatch.value, "").trim()

        // Extract duration: Nmin, Nm, Nminute(s) — use (?!\\w) to handle non-ASCII text
        val durationRegex = Regex("""\b(\d+)\s*(мин|min|m|minute|minutes)(?!\w)""")
        val durationMatch = durationRegex.find(remaining)
        val durationMinutes: Int? = durationMatch?.let {
            try { it.groupValues[1].toInt() } catch (e: Exception) { null }
        }
        if (durationMatch != null) remaining = remaining.replace(durationMatch.value, "").trim()

        // Extract contexts: @Word (Unicode-aware for Cyrillic)
        val contextRegex = Regex("""@(\p{L}+)""")
        val contexts = contextRegex.findAll(remaining).map { it.value }.toList()

        // Extract name (remaining text minus contexts)
        remaining = remaining.replace(contextRegex, "").trim()

        return ParsedTaskInput(
            name = remaining,
            dueDate = dueDate,
            durationMinutes = durationMinutes,
            contexts = contexts
        )
    }
}

data class ParsedTaskInput(
    val name: String,
    val dueDate: Long?,
    val durationMinutes: Int?,
    val contexts: List<String>
)
