package com.mlo.app.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.mlo.app.data.local.*
import com.mlo.app.data.model.*
import com.mlo.app.data.repository.TaskRepository
import com.mlo.app.domain.ProfileTemplateData
import com.mlo.app.domain.dropbox.DropboxConfig
import com.mlo.app.domain.dropbox.DropboxSyncManager
import com.mlo.app.domain.dropbox.SyncData
import com.mlo.app.domain.dropbox.SyncTask
import com.mlo.app.domain.dropbox.SyncContext
import com.mlo.app.domain.dropbox.SyncGoal
import com.mlo.app.domain.dropbox.SyncFlag
import com.mlo.app.domain.dropbox.SyncView
import com.mlo.app.domain.notification.GeofenceSyncWorker
import com.mlo.app.domain.notification.ReminderCheckWorker
import com.mlo.app.data.sync.DropboxSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AppUiState(
    val contexts: List<GContextModel> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val flags: List<FlagEntity> = emptyList(),
    val savedViews: List<ViewEntity> = emptyList(),
    val templates: List<TemplateProfile> = emptyList(),
    val statistics: StatisticsData? = null,
    val isDropboxConnected: Boolean = false,
    val lastSyncTime: Long? = null,
    val dropboxSyncError: String? = null,
    val isDropboxSyncing: Boolean = false,
    val dropboxTokenSet: Boolean = false,
    val syncIntervalMinutes: Int = 0, // 0 = manual
    val error: String? = null
)

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository,
    private val dropboxSyncManager: DropboxSyncManager
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("myorganizer_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        loadSavedToken()
        loadSavedSyncInterval()
        loadData()
    }

    // ── Token persistence ──

    private fun loadSavedToken() {
        val savedToken = prefs.getString("dropbox_token", null)
        if (!savedToken.isNullOrBlank()) {
            DropboxConfig.setToken(savedToken)
            _state.update { it.copy(dropboxTokenSet = true) }
            // Auto-test connection
            connectDropbox()
        }
    }

    fun saveDropboxToken(token: String) {
        prefs.edit().putString("dropbox_token", token).apply()
        DropboxConfig.setToken(token)
        _state.update { it.copy(dropboxTokenSet = true) }
    }

    fun clearDropboxToken() {
        prefs.edit().remove("dropbox_token").apply()
        DropboxConfig.setToken("YOUR_DROPBOX_TOKEN_HERE")
        _state.update { it.copy(dropboxTokenSet = false) }
    }

    // ── Sync interval persistence ──

    private fun loadSavedSyncInterval() {
        val savedInterval = prefs.getInt("sync_interval_minutes", 0)
        _state.update { it.copy(syncIntervalMinutes = savedInterval) }
        if (savedInterval > 0 && DropboxConfig.isTokenSet) {
            schedulePeriodicSync(savedInterval)
        }
    }

    fun setSyncInterval(minutes: Int) {
        prefs.edit().putInt("sync_interval_minutes", minutes).apply()
        _state.update { it.copy(syncIntervalMinutes = minutes) }
        val ctx = getApplication<Application>()
        WorkManager.getInstance(ctx).cancelUniqueWork("dropbox_periodic_sync")
        if (minutes > 0 && DropboxConfig.isTokenSet) {
            schedulePeriodicSync(minutes)
        }
    }

    private fun schedulePeriodicSync(minutes: Int) {
        val ctx = getApplication<Application>()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<DropboxSyncWorker>(
            minutes.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "dropbox_periodic_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    // ── Data loading ──

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllContexts().collect { ctxList ->
                val models = ctxList.map { GContextModel.fromEntity(it) }
                _state.update { it.copy(contexts = models) }
            }
        }

        viewModelScope.launch {
            repository.getAllGoals().collect { goals ->
                _state.update { it.copy(goals = goals) }
            }
        }

        viewModelScope.launch {
            repository.getAllFlags().collect { flags ->
                _state.update { it.copy(flags = flags) }
            }
        }

        viewModelScope.launch {
            repository.getAllViews().collect { views ->
                _state.update { it.copy(savedViews = views) }
            }
        }

        viewModelScope.launch {
            repository.getAllTemplates().collect { templates ->
                _state.update {
                    it.copy(templates = templates.map { entity ->
                        TemplateProfile.fromEntity(entity)
                    })
                }
            }
        }

        // Load built-in templates if none exist
        viewModelScope.launch {
            val existing = repository.getAllTemplates().first()
            if (existing.isEmpty()) {
                ProfileTemplateData.allTemplates.forEach { def ->
                    repository.insertTemplate(
                        ProfileTemplateEntity(
                            name = def.name,
                            description = def.description,
                            category = def.category,
                            isBuiltIn = true,
                            templateJson = def.templateJson
                        )
                    )
                }
            }
        }
    }

    // ── Flags ──

    fun addFlag(flag: FlagEntity) {
        viewModelScope.launch { repository.insertFlag(flag) }
    }

    fun updateFlag(flag: FlagEntity) {
        viewModelScope.launch { repository.updateFlag(flag) }
    }

    fun deleteFlag(flag: FlagEntity) {
        viewModelScope.launch { repository.deleteFlag(flag) }
    }

    // ── Contexts ──

    fun saveContext(context: ContextEntity) {
        viewModelScope.launch { repository.updateContext(context) }
    }

    fun deleteContext(context: ContextEntity) {
        viewModelScope.launch { repository.deleteContext(context) }
    }

    fun deleteContext(model: GContextModel) {
        viewModelScope.launch {
            repository.deleteContext(
                ContextEntity(id = model.id, name = model.name, label = model.label)
            )
        }
    }

    fun saveContextHours(contextId: Long, hours: List<ContextHourEntity>) {
        viewModelScope.launch { repository.saveContextHours(contextId, hours) }
    }

    // ── Views ──

    fun saveView(filter: ViewFilter) {
        viewModelScope.launch { repository.insertView(filter.toEntity()) }
    }

    fun updateView(view: ViewEntity) {
        viewModelScope.launch { repository.updateView(view) }
    }

    fun deleteView(view: ViewEntity) {
        viewModelScope.launch { repository.deleteView(view) }
    }

    // ── Templates ──

    fun applyTemplate(template: TemplateProfile) {
        viewModelScope.launch {
            try {
                val gson = com.google.gson.Gson()
                val data = gson.fromJson(template.templateJson, Map::class.java)

                @Suppress("UNCHECKED_CAST")
                val contexts = (data["contexts"] as? List<Map<String, Any?>>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val goals = (data["goals"] as? List<Map<String, Any?>>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val flags = (data["flags"] as? List<Map<String, Any?>>) ?: emptyList()

                for (ctx in contexts) {
                    val name = ctx["name"] as? String ?: continue
                    val label = ctx["label"] as? String ?: name
                    val color = (ctx["color"] as? Double)?.toInt()
                    @Suppress("UNCHECKED_CAST")
                    val incIds = (ctx["includeIds"] as? List<Double>)?.map { it.toLong() } ?: emptyList()
                    repository.insertContext(
                        ContextEntity(
                            name = name,
                            label = label,
                            color = color,
                            includeIds = incIds.joinToString(",")
                        )
                    )
                }

                for (g in goals) {
                    val name = g["name"] as? String ?: continue
                    val title = g["title"] as? String ?: name
                    val color = (g["color"] as? Double)?.toInt()
                    repository.insertGoal(
                        GoalEntity(name = name, title = title, color = color)
                    )
                }

                for (f in flags) {
                    val name = f["name"] as? String ?: continue
                    val label = f["label"] as? String ?: name
                    val color = (f["color"] as? Double)?.toInt()
                    repository.insertFlag(
                        FlagEntity(name = name, label = label, color = color)
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка применения шаблона: ${e.message}") }
            }
        }
    }

    // ── Statistics ──

    fun refreshStatistics() {
        viewModelScope.launch {
            val allTasks = repository.getAllTasksSync()
            val now = System.currentTimeMillis()
            val todayStart = now - (now % 86400000L)
            val weekAgo = now - 7 * 86400000L
            val monthAgo = now - 30 * 86400000L

            val activeTasks = allTasks.filter { it.status == "ACTIVE" }
            val completedTasks = allTasks.filter { it.status == "COMPLETED" }

            val completedToday = completedTasks.count { it.updatedAt >= todayStart }
            val completedThisWeek = completedTasks.count { it.updatedAt >= weekAgo }
            val completedThisMonth = completedTasks.count { it.updatedAt >= monthAgo }
            val overdueTasks = activeTasks.count {
                it.dueDate != null && it.dueDate < now
            }

            val avgPriority = if (activeTasks.isNotEmpty()) {
                activeTasks.map { it.importance.toDouble() }.average()
            } else 0.0

            val completionRate = if (allTasks.isNotEmpty()) {
                completedTasks.size.toFloat() / allTasks.size
            } else 0f

            // By context
            val contexts = repository.getAllContexts().first()
            val tasksByContext = mutableMapOf<String, Int>()
            contexts.forEach { ctx ->
                val count = allTasks.count { it.contexts.contains(ctx.name) }
                if (count > 0) tasksByContext[ctx.label.ifEmpty { ctx.name }] = count
            }

            // By goal
            val goals = repository.getAllGoals().first()
            val tasksByGoal = mutableMapOf<String, Int>()
            goals.forEach { g ->
                val count = allTasks.count { it.goalId == g.id }
                if (count > 0) tasksByGoal[g.title.ifEmpty { g.name }] = count
            }

            // Trend (last 7 days)
            val trendWeek = (0..6).map { dayOffset ->
                val dayStart = todayStart - dayOffset * 86400000L
                val dayEnd = dayStart + 86400000L
                val completed = completedTasks.count { it.updatedAt in dayStart until dayEnd }
                val added = allTasks.count { it.createdAt in dayStart until dayEnd }
                val date = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault())
                    .format(java.util.Date(dayStart))
                DailyStats(date = date, completed = completed, added = added)
            }.reversed()

            _state.update {
                it.copy(
                    statistics = StatisticsData(
                        totalTasks = allTasks.size,
                        activeTasks = activeTasks.size,
                        completedToday = completedToday,
                        completedThisWeek = completedThisWeek,
                        completedThisMonth = completedThisMonth,
                        overdueTasks = overdueTasks,
                        tasksByContext = tasksByContext,
                        tasksByGoal = tasksByGoal,
                        completionRate = completionRate,
                        avgPriority = avgPriority,
                        trendWeek = trendWeek
                    )
                )
            }
        }
    }

    // ── Notifications ──

    fun scheduleReminderCheck() {
        val context = getApplication<android.app.Application>()
        ReminderCheckWorker.schedule(context)
    }

    fun scheduleGeofenceSync() {
        val context = getApplication<android.app.Application>()
        GeofenceSyncWorker.schedule(context)
    }

    // ── Dropbox Sync ──

    fun connectDropbox() {
        viewModelScope.launch {
            if (!DropboxConfig.isTokenSet) {
                _state.update { it.copy(dropboxSyncError = "Сначала введите токен доступа") }
                return@launch
            }
            _state.update { it.copy(isDropboxSyncing = true, dropboxSyncError = null) }
            val result = dropboxSyncManager.testConnection()
            result.onSuccess {
                _state.update { it.copy(isDropboxConnected = true, isDropboxSyncing = false) }
                // Reschedule periodic sync if interval was set
                val interval = _state.value.syncIntervalMinutes
                if (interval > 0) {
                    schedulePeriodicSync(interval)
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isDropboxConnected = false,
                        isDropboxSyncing = false,
                        dropboxSyncError = "Ошибка подключения: ${error.message}"
                    )
                }
            }
        }
    }

    fun disconnectDropbox() {
        val ctx = getApplication<Application>()
        WorkManager.getInstance(ctx).cancelUniqueWork("dropbox_periodic_sync")
        clearDropboxToken()
        _state.update {
            it.copy(
                isDropboxConnected = false,
                lastSyncTime = null,
                dropboxSyncError = null
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            if (!DropboxConfig.isTokenSet) {
                _state.update { it.copy(dropboxSyncError = "Сначала введите токен доступа") }
                return@launch
            }
            _state.update { it.copy(isDropboxSyncing = true, dropboxSyncError = null) }
            try {
                // 1. Gather all local data
                val tasks = repository.getAllTasksSync().map { task ->
                    SyncTask(
                        id = task.id,
                        name = task.name,
                        status = task.status,
                        importance = task.importance,
                        urgency = task.urgency,
                        dueDate = task.dueDate,
                        createdAt = task.createdAt,
                        updatedAt = task.updatedAt,
                        contexts = task.contexts,
                        parentId = task.parentId,
                        sortOrder = task.sortOrder,
                        goalId = task.goalId,
                        isCompleted = task.status == "COMPLETED"
                    )
                }
                val contexts = repository.getAllContextsSync().map { ctx ->
                    SyncContext(
                        id = ctx.id,
                        name = ctx.name,
                        label = ctx.label,
                        color = ctx.color,
                        includeIds = ctx.includeIds
                    )
                }
                val goals = repository.getAllGoalsSync().map { goal ->
                    SyncGoal(id = goal.id, name = goal.name, title = goal.title, color = goal.color)
                }
                val flags = repository.getAllFlagsSync().map { flag ->
                    SyncFlag(
                        id = flag.id,
                        name = flag.name,
                        label = flag.label,
                        color = flag.color,
                        iconName = flag.iconName
                    )
                }
                val views = repository.getAllViewsSync().map { view ->
                    SyncView(id = view.id, name = view.name, groupBy = view.groupBy, sortBy = view.sortBy, isAscending = view.isAscending)
                }

                val syncData = SyncData(
                    deviceId = "",
                    syncTimestamp = System.currentTimeMillis(),
                    tasks = tasks,
                    contexts = contexts,
                    goals = goals,
                    flags = flags,
                    views = views
                )

                // 2. Upload to Dropbox
                val result = dropboxSyncManager.uploadSyncData(syncData)
                result.onSuccess {
                    _state.update {
                        it.copy(
                            isDropboxSyncing = false,
                            lastSyncTime = System.currentTimeMillis(),
                            dropboxSyncError = null
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isDropboxSyncing = false,
                            dropboxSyncError = "Ошибка синхронизации: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isDropboxSyncing = false,
                        dropboxSyncError = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    // ── Context CRUD ──

    fun createContext(name: String) {
        viewModelScope.launch {
            repository.insertContext(
                ContextEntity(name = name, label = name)
            )
        }
    }

    fun updateContext(model: GContextModel) {
        viewModelScope.launch {
            repository.updateContext(
                ContextEntity(
                    id = model.id,
                    name = model.name,
                    label = model.label,
                    iconName = model.iconName,
                    locationLat = model.locationLat,
                    locationLon = model.locationLon,
                    locationRadiusMeters = model.locationRadiusMeters,
                    isLocationOnly = model.isLocationOnly
                )
            )
        }
    }

    // ── Goal CRUD ──

    fun createGoal(name: String) {
        viewModelScope.launch {
            repository.insertGoal(
                GoalEntity(name = name, title = name)
            )
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}
