package com.mlo.app.data.sync

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.mlo.app.data.local.ContextEntity
import com.mlo.app.data.local.GoalEntity
import com.mlo.app.data.local.TaskDatabase
import com.mlo.app.data.local.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Import tasks from Toodledo CSV export format.
 *
 * CSV columns:
 *   TASK, FOLDER, CONTEXT, GOAL, LOCATION, STARTDATE, STARTTIME,
 *   DUEDATE, DUETIME, REPEAT, LENGTH, TIMER, PRIORITY, TAG, STATUS, STAR, NOTE
 *
 * TASK field uses \\ as hierarchy separator (e.g. "Folder\\Subfolder\\Task Name").
 * Multi-line fields (NOTE) are properly quoted.
 */
@Singleton
class ToodledoImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class ImportResult(
        val tasksInserted: Int = 0,
        val contextsCreated: Int = 0,
        val goalsCreated: Int = 0,
        val errors: List<String> = emptyList()
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val db = TaskDatabase.getInstance(context)
        val taskDao = db.taskDao()
        val contextDao = db.contextDao()
        val goalDao = db.goalDao()

        try {
            // Read CSV content
            val csvContent = buildString {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, "UTF-8")).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            appendLine(line!!)
                        }
                    }
                }
            }

            // Parse CSV rows (handles multi-line quoted fields)
            val rows = parseCsv(csvContent)

            if (rows.isEmpty()) {
                return@withContext ImportResult(errors = listOf("CSV is empty or could not be parsed"))
            }

            // First row = headers
            val headers = rows[0]
            val dataRows = rows.drop(1)

            // Find column indices
            val colIdx = mapOf(
                "TASK" to headers.indexOf("TASK"),
                "FOLDER" to headers.indexOf("FOLDER"),
                "CONTEXT" to headers.indexOf("CONTEXT"),
                "GOAL" to headers.indexOf("GOAL"),
                "LOCATION" to headers.indexOf("LOCATION"),
                "STARTDATE" to headers.indexOf("STARTDATE"),
                "STARTTIME" to headers.indexOf("STARTTIME"),
                "DUEDATE" to headers.indexOf("DUEDATE"),
                "DUETIME" to headers.indexOf("DUETIME"),
                "REPEAT" to headers.indexOf("REPEAT"),
                "LENGTH" to headers.indexOf("LENGTH"),
                "PRIORITY" to headers.indexOf("PRIORITY"),
                "STATUS" to headers.indexOf("STATUS"),
                "STAR" to headers.indexOf("STAR"),
                "NOTE" to headers.indexOf("NOTE")
            )

            // Collect unique contexts and goals
            val contextNames = dataRows.mapNotNull { row ->
                getCol(row, colIdx, "CONTEXT")?.trim()?.takeIf { it.isNotEmpty() }
            }.distinct()

            val goalNames = dataRows.mapNotNull { row ->
                getCol(row, colIdx, "GOAL")?.trim()?.takeIf { it.isNotEmpty() }
            }.distinct()

            // Insert contexts
            val contextNameToId = mutableMapOf<String, Long>()
            for (name in contextNames) {
                val existing = contextDao.getContextByName(name)
                if (existing != null) {
                    contextNameToId[name] = existing.id
                } else {
                    val id = contextDao.insert(
                        ContextEntity(name = name, sortOrder = contextNameToId.size)
                    )
                    contextNameToId[name] = id
                }
            }

            // Insert goals
            val goalNameToId = mutableMapOf<String, Long>()
            for (name in goalNames) {
                val existing = goalDao.getGoalByName(name)
                if (existing != null) {
                    goalNameToId[name] = existing.id
                } else {
                    val id = goalDao.insert(
                        GoalEntity(name = name, sortOrder = goalNameToId.size)
                    )
                    goalNameToId[name] = id
                }
            }

            // Build folder hierarchy from TASK column \\ separators
            // Insert folder-level tasks first, then real tasks with parentId
            val hierarchyTasks = mutableMapOf<String, Long>()  // path -> taskId
            val tasksToInsert = mutableListOf<TaskEntity>()
            var insertedCount = 0

            for (row in dataRows) {
                try {
                    val rawTask = getCol(row, colIdx, "TASK") ?: continue
                    val folder = getCol(row, colIdx, "FOLDER") ?: ""
                    val contextStr = getCol(row, colIdx, "CONTEXT") ?: ""
                    val goalStr = getCol(row, colIdx, "GOAL") ?: ""
                    val startDateStr = getCol(row, colIdx, "STARTDATE") ?: ""
                    val startTimeStr = getCol(row, colIdx, "STARTTIME") ?: ""
                    val dueDateStr = getCol(row, colIdx, "DUEDATE") ?: ""
                    val dueTimeStr = getCol(row, colIdx, "DUETIME") ?: ""
                    val repeat = getCol(row, colIdx, "REPEAT") ?: ""
                    val lengthStr = getCol(row, colIdx, "LENGTH") ?: ""
                    val priorityStr = getCol(row, colIdx, "PRIORITY") ?: ""
                    val status = getCol(row, colIdx, "STATUS") ?: ""
                    val isStarred = getCol(row, colIdx, "STAR") ?: ""
                    val note = getCol(row, colIdx, "NOTE") ?: ""

                    // Parse hierarchy from TASK: "Folder\\Sub\\Name"
                    val parts = rawTask.split("\\\\")
                    val taskName = parts.last().trim()
                    val hierarchyPath = parts.dropLast(1).map { it.trim() }.filter { it.isNotEmpty() }

                    // Create parent tasks for the hierarchy
                    var parentId: Long? = null
                    var currentPath = ""
                    for (folderPart in hierarchyPath) {
                        currentPath = if (currentPath.isEmpty()) folderPart else "$currentPath\\\\$folderPart"
                        val existingId = hierarchyTasks[currentPath]
                        if (existingId != null) {
                            parentId = existingId
                        } else {
                            // Create folder-level task
                            val folderTask = TaskEntity(
                                name = folderPart,
                                parentId = parentId,
                                status = "ACTIVE",
                                isRecurring = false,
                                contexts = contextStr,
                                sortOrder = hierarchyTasks.size
                            )
                            val newId = taskDao.insertTask(folderTask)
                            hierarchyTasks[currentPath] = newId
                            parentId = newId
                            insertedCount++
                        }
                    }

                    // Map fields
                    val startDate = parseDate(startDateStr, startTimeStr)
                    val dueDate = parseDate(dueDateStr, dueTimeStr)
                    val duration = lengthStr.toIntOrNull()?.takeIf { it > 0 }
                    val importance = mapPriority(priorityStr)
                    val mappedStatus = mapStatus(status)
                    val noteText = note.trim().ifEmpty { null }
                    val isRecur = repeat.isNotEmpty()
                    val recurPattern = mapRecurring(repeat)
                    val flags = if (isStarred == "1") "STARRED" else ""

                    // Build contexts string
                    val contexts = buildString {
                        if (contextStr.isNotEmpty()) {
                            append(contextStr)
                        }
                        // Also add FOLDER as context
                        if (folder.isNotEmpty()) {
                            if (isNotEmpty()) append(",")
                            append("@$folder")
                        }
                    }

                    val task = TaskEntity(
                        name = taskName,
                        parentId = parentId,
                        startDate = startDate,
                        dueDate = dueDate,
                        durationMinutes = duration,
                        importance = importance,
                        urgency = importance,  // Default urgency = importance
                        contexts = contexts,
                        goalId = goalNameToId[goalStr],
                        status = mappedStatus,
                        isRecurring = isRecur,
                        recurringPattern = recurPattern,
                        notes = noteText,
                        flags = flags,
                        sortOrder = insertedCount
                    )
                    taskDao.insertTask(task)
                    insertedCount++

                } catch (e: Exception) {
                    errors.add("Row ${dataRows.indexOf(row) + 2}: ${e.message ?: "parse error"}")
                }
            }

            return@withContext ImportResult(
                tasksInserted = insertedCount,
                contextsCreated = contextNames.size,
                goalsCreated = goalNames.size,
                errors = errors
            )

        } catch (e: Exception) {
            return@withContext ImportResult(
                errors = listOf("Import failed: ${e.message ?: "unknown error"}")
            )
        }
    }

    /**
     * Parse CSV with support for quoted fields containing newlines and commas.
     */
    private fun parseCsv(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val ch = content[i]

            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        // Escaped quote ""
                        currentField.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = !inQuotes
                    i++
                }
                ch == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    i++
                }
                ch == '\n' && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    rows.add(currentRow.toList())
                    currentRow.clear()
                    i++
                }
                ch == '\r' && !inQuotes -> {
                    // Skip \r (Windows line endings)
                    i++
                }
                else -> {
                    currentField.append(ch)
                    i++
                }
            }
        }

        // Handle last line if it doesn't end with newline
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            rows.add(currentRow.toList())
        }

        return rows
    }

    private fun getCol(row: List<String>, idx: Map<String, Int>, col: String): String? {
        val colIdx = idx[col] ?: return null
        return row.getOrNull(colIdx)?.trim()
    }

    private fun parseDate(dateStr: String, timeStr: String): Long? {
        if (dateStr.isEmpty()) return null
        return try {
            if (timeStr.isNotEmpty()) {
                dateTimeFormat.parse("$dateStr $timeStr")?.time
            } else {
                dateFormat.parse(dateStr)?.time
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Map Toodledo priority to MLO importance (0-200 scale).
     * Toodledo: Top=1, High=2, Medium=3, Low=-1, None=empty
     */
    private fun mapPriority(priority: String): Int = when (priority.trim()) {
        "1" -> 200   // Top
        "2" -> 150   // High
        "3" -> 100   // Medium
        "-1" -> 50   // Low
        else -> 100  // None
    }

    /**
     * Map Toodledo status to MLO status.
     */
    private fun mapStatus(status: String): String = when (status.trim().uppercase()) {
        "ACTIVE", "NEXT ACTION" -> "ACTIVE"
        "PLANNING" -> "ACTIVE"
        "WAITING" -> "DEFERRED"
        "REFERENCE" -> "COMPLETED"
        "HOLD" -> "DEFERRED"
        "POSTPONED" -> "DEFERRED"
        "SOMEDAY" -> "DEFERRED"
        else -> "ACTIVE"
    }

    /**
     * Map Toodledo repeat pattern to MLO recurringPattern.
     */
    private fun mapRecurring(repeat: String): String? = when (repeat.trim().uppercase()) {
        "DAILY" -> "DAILY"
        "WEEKLY" -> "WEEKLY"
        "BIWEEKLY" -> "WEEKLY:2"
        "MONTHLY" -> "MONTHLY"
        "YEARLY" -> "YEARLY"
        "SEMIANNUALLY" -> "MONTHLY:6"
        else -> repeat.trim().ifEmpty { null }
    }
}
