package com.mlo.app

import com.mlo.app.data.local.TaskEntity
import com.mlo.app.data.model.PriorityConfig
import com.mlo.app.domain.PriorityEngine
import org.junit.Assert.*
import org.junit.Test

class PriorityEngineTest {

    private val now = System.currentTimeMillis()
    private val hourMs = 1000L * 60 * 60

    // ── Helper to create tasks ──

    private fun task(
        id: Long,
        name: String,
        parentId: Long? = null,
        importance: Int = 100,
        urgency: Int = 100,
        dueDate: Long? = null,
        startDate: Long? = null,
        status: String = "ACTIVE",
        dependencyIds: String = "",
        weeklyGoalWeight: Double = 100.0
    ) = TaskEntity(
        id = id,
        name = name,
        parentId = parentId,
        importance = importance,
        urgency = urgency,
        dueDate = dueDate,
        startDate = startDate,
        status = status,
        dependencyIds = dependencyIds,
        weeklyGoalWeight = weeklyGoalWeight
    )

    // ── calculateEffectiveImportance ──

    @Test
    fun `effective importance defaults to task importance when no parent`() {
        val t = task(1, "Root", importance = 150)
        val result = PriorityEngine.calculateEffectiveImportance(t, listOf(t))
        assertEquals(150, result)
    }

    @Test
    fun `effective importance inherits from parent`() {
        val parent = task(1, "Parent", importance = 200)
        val child = task(2, "Child", parentId = 1, importance = 50)
        val tasks = listOf(parent, child)

        // child effective = parent(200) * child(50) / 100 = 100
        val result = PriorityEngine.calculateEffectiveImportance(child, tasks)
        assertEquals(100, result)
    }

    @Test
    fun `effective importance handles deep nesting`() {
        val gparent = task(1, "GP", importance = 200)
        val parent = task(2, "Parent", parentId = 1, importance = 100)
        val child = task(3, "Child", parentId = 2, importance = 50)
        val tasks = listOf(gparent, parent, child)

        // parent effective = 200 * 100 / 100 = 200
        // child effective = 200 * 50 / 100 = 100
        val result = PriorityEngine.calculateEffectiveImportance(child, tasks)
        assertEquals(100, result)
    }

    @Test
    fun `effective importance clamps to 0-200 range`() {
        val parent = task(1, "Parent", importance = 200)
        val child = task(2, "Child", parentId = 1, importance = 200)
        val tasks = listOf(parent, child)

        // 200 * 200 / 100 = 400, clamped to 200
        val result = PriorityEngine.calculateEffectiveImportance(child, tasks)
        assertEquals(200, result)
    }

    // ── calculateEffectiveUrgency ──

    @Test
    fun `effective urgency inherits same as importance`() {
        val parent = task(1, "Parent", urgency = 150)
        val child = task(2, "Child", parentId = 1, urgency = 60)
        val tasks = listOf(parent, child)

        // child effective = 150 * 60 / 100 = 90
        val result = PriorityEngine.calculateEffectiveUrgency(child, tasks)
        assertEquals(90, result)
    }

    // ── calculateTimeFactor ──

    @Test
    fun `time factor is neutral with no due date`() {
        val t = task(1, "No due")
        val result = PriorityEngine.calculateTimeFactor(t, now)
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun `time factor boosts when overdue`() {
        val overdue = now - 2 * hourMs // 2 hours overdue
        val t = task(1, "Overdue", dueDate = overdue)
        val result = PriorityEngine.calculateTimeFactor(t, now)

        // 200 + 2 * 10 = 220
        assertEquals(220.0, result, 0.01)
    }

    @Test
    fun `time factor decreases with remaining time`() {
        val future = now + 24 * hourMs // 24 hours from now
        val t = task(1, "Future", dueDate = future)
        val result = PriorityEngine.calculateTimeFactor(t, now)

        // 100 - 24 = 76
        assertEquals(76.0, result, 0.01)
    }

    // ── calculatePriorityScore ──

    @Test
    fun `priority score uses weighted formula`() {
        // Default config: wI=0.4, wU=0.3, wT=0.2, wG=0.1
        val t = task(1, "Test", importance = 100, urgency = 100, weeklyGoalWeight = 100.0)
        val config = PriorityConfig()
        val score = PriorityEngine.calculatePriorityScore(t, listOf(t), config, now)

        // I=100, U=100, T=100, G=100
        // 0.4*100 + 0.3*100 + 0.2*100 + 0.1*100 = 100
        assertEquals(100.0, score, 0.01)
    }

    @Test
    fun `higher importance gives higher score`() {
        val t1 = task(1, "Low", importance = 50)
        val t2 = task(2, "High", importance = 200)
        val tasks = listOf(t1, t2)

        val s1 = PriorityEngine.calculatePriorityScore(t1, tasks, PriorityConfig(), now)
        val s2 = PriorityEngine.calculatePriorityScore(t2, tasks, PriorityConfig(), now)
        assertTrue("Higher importance should give higher score", s2 > s1)
    }

    // ── isTaskActive ──

    @Test
    fun `active task without conditions is active`() {
        val t = task(1, "Active")
        assertTrue(PriorityEngine.isTaskActive(t, listOf(t), now))
    }

    @Test
    fun `completed task is not active`() {
        val t = task(1, "Done", status = "COMPLETED")
        assertFalse(PriorityEngine.isTaskActive(t, listOf(t), now))
    }

    @Test
    fun `future start date task is not active`() {
        val future = now + 24 * hourMs
        val t = task(1, "Future", startDate = future)
        assertFalse(PriorityEngine.isTaskActive(t, listOf(t), now))
    }

    @Test
    fun `task with incomplete dependency is not active`() {
        val dep = task(1, "Dep", status = "ACTIVE")
        val t = task(2, "Main", dependencyIds = "1")
        assertFalse(PriorityEngine.isTaskActive(t, listOf(dep, t), now))
    }

    @Test
    fun `task with completed dependency is active`() {
        val dep = task(1, "Dep", status = "COMPLETED")
        val t = task(2, "Main", dependencyIds = "1")
        assertTrue(PriorityEngine.isTaskActive(t, listOf(dep, t), now))
    }

    @Test
    fun `task with multiple completed dependencies is active`() {
        val dep1 = task(1, "Dep1", status = "COMPLETED")
        val dep2 = task(2, "Dep2", status = "COMPLETED")
        val t = task(3, "Main", dependencyIds = "1,2")
        assertTrue(PriorityEngine.isTaskActive(t, listOf(dep1, dep2, t), now))
    }

    // ── getActiveTasksScored ──

    @Test
    fun `getActiveTasksScored returns only active tasks sorted by score`() {
        val active1 = task(1, "A1", importance = 150)
        val active2 = task(2, "A2", importance = 50)
        val completed = task(3, "Done", status = "COMPLETED")
        val tasks = listOf(active1, active2, completed)

        val result = PriorityEngine.getActiveTasksScored(tasks, PriorityConfig(), now)
        assertEquals(2, result.size)
        assertEquals("A1", result[0].first.name) // higher importance first
        assertEquals("A2", result[1].first.name)
    }

    // ── parseTaskInput ──

    @Test
    fun `parseTaskInput extracts name, date, duration, contexts`() {
        val input = "Купить билеты @Продукты 2026-06-20 30мин"
        val parsed = PriorityEngine.parseTaskInput(input)

        assertEquals("Купить билеты", parsed.name.trim())
        assertNotNull(parsed.dueDate)
        assertEquals(30, parsed.durationMinutes)
        assertEquals(listOf("@Продукты"), parsed.contexts)
    }

    @Test
    fun `parseTaskInput handles input without date or duration`() {
        val input = "Простая задача @Дом"
        val parsed = PriorityEngine.parseTaskInput(input)

        assertEquals("Простая задача", parsed.name.trim())
        assertNull(parsed.dueDate)
        assertNull(parsed.durationMinutes)
        assertEquals(listOf("@Дом"), parsed.contexts)
    }

    // ── computeNextOccurrence ──

    @Test
    fun `daily recurrence advances by one day`() {
        val base = 1000000L
        val next = PriorityEngine.computeNextOccurrence("DAILY", base)
        assertTrue("Next occurrence should be later", next > base)
        assertEquals(86400000.0, (next - base).toDouble(), 1000.0)
    }

    // ── buildFlatTree ──

    @Test
    fun `buildFlatTree flattens hierarchy with depth`() {
        val root = task(1, "Root")
        val child = task(2, "Child", parentId = 1)
        val grandchild = task(3, "GC", parentId = 2)
        val tasks = listOf(root, child, grandchild)

        val tree = PriorityEngine.buildFlatTree(tasks)
        assertEquals(3, tree.size)

        val (rootItem, rootDepth) = tree[0]
        val (childItem, childDepth) = tree[1]
        val (gcItem, gcDepth) = tree[2]

        assertEquals(0, rootDepth)
        assertEquals(1, childDepth)
        assertEquals(2, gcDepth)
    }
}
