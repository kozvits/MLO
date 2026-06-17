package com.mlo.app.domain.dropbox

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Loads seed sync data from assets or provides programmatic defaults.
 *
 * Used on first app launch to populate the database with sensible defaults
 * and to immediately establish a baseline backup on Dropbox.
 */
object SyncSeedData {

    private val gson: Gson = GsonBuilder().setLenient().create()

    /**
     * Parse seed data from an assets JSON file.
     * Returns null if parsing fails.
     */
    fun loadFromJson(inputStream: InputStream): SyncData? {
        return try {
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, SyncData::class.java).also { reader.close() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Hardcoded fallback seed data matching the JSON template.
     * Used when the assets file is unavailable.
     */
    fun getDefaultSeed(): SyncData = SyncData(
        deviceId = "seed-device",
        syncTimestamp = System.currentTimeMillis(),
        tasks = listOf(
            SyncTask(
                id = 1001, name = "Настроить MLO", status = "ACTIVE",
                importance = 100, urgency = 100, dueDate = null,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                contexts = "@Computer", parentId = null, sortOrder = 1, goalId = null,
                isCompleted = false, completedAt = null
            ),
            SyncTask(
                id = 1002, name = "Купить продукты", status = "ACTIVE",
                importance = 50, urgency = 80, dueDate = null,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                contexts = "@Home", parentId = null, sortOrder = 2, goalId = 2001,
                isCompleted = false, completedAt = null
            ),
            SyncTask(
                id = 1003, name = "Заказать подарок маме", status = "ACTIVE",
                importance = 90, urgency = 60, dueDate = System.currentTimeMillis() + 7 * 86400000L,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                contexts = "@Computer", parentId = null, sortOrder = 3, goalId = 2001,
                isCompleted = false, completedAt = null
            ),
            SyncTask(
                id = 1004, name = "Прочитать «Успеватель»", status = "ACTIVE",
                importance = 70, urgency = 30, dueDate = null,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                contexts = "@Anywhere", parentId = null, sortOrder = 4, goalId = 2002,
                isCompleted = false, completedAt = null
            ),
            SyncTask(
                id = 1005, name = "Сделать зарядку", status = "COMPLETED",
                importance = 80, urgency = 100, dueDate = null,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                contexts = "@Home", parentId = null, sortOrder = 5, goalId = 2002,
                isCompleted = true, completedAt = System.currentTimeMillis()
            )
        ),
        contexts = listOf(
            SyncContext(id = 1, name = "@Computer", label = "Компьютер", color = -13382656),
            SyncContext(id = 2, name = "@Home", label = "Дом", color = -16711936),
            SyncContext(id = 3, name = "@Anywhere", label = "Любое место", color = -1),
            SyncContext(id = 4, name = "@Phone", label = "Телефон", color = -16776961),
            SyncContext(id = 5, name = "@Office", label = "Офис", color = -65536)
        ),
        goals = listOf(
            SyncGoal(id = 2001, name = "Семья", title = "Семейные цели", color = -13382656),
            SyncGoal(id = 2002, name = "Здоровье", title = "Саморазвитие и здоровье", color = -16711936),
            SyncGoal(id = 2003, name = "Работа", title = "Рабочие проекты", color = -65536)
        ),
        flags = listOf(
            SyncFlag(id = 1, name = "STARRED", label = "Избранное", color = -769226, iconName = "star"),
            SyncFlag(id = 2, name = "DELEGATED", label = "Делегировано", color = -13382656, iconName = "person"),
            SyncFlag(id = 3, name = "WAITING", label = "Ожидание", color = -16711936, iconName = "schedule"),
            SyncFlag(id = 4, name = "URGENT", label = "Срочно", color = -65536, iconName = "priority_high")
        ),
        views = listOf(
            SyncView(id = 1, name = "Все задачи", groupBy = "NONE", sortBy = "PRIORITY", isAscending = false),
            SyncView(id = 2, name = "По контекстам", groupBy = "CONTEXT", sortBy = "PRIORITY", isAscending = true),
            SyncView(id = 3, name = "По целям", groupBy = "GOAL", sortBy = "PRIORITY", isAscending = true)
        )
    )
}
