package com.mlo.app.ui.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlo.app.data.local.ContextEntity
import com.mlo.app.data.local.GoalEntity
import com.mlo.app.data.repository.TaskRepository
import com.mlo.app.data.sync.DropboxSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val contexts: List<ContextEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val isDropboxConnected: Boolean = false,
    val lastSyncTime: Long? = null,
    val showSyncIndicator: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showContextManager: Boolean = false,
    val showGoalEditor: Boolean = false
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val syncManager: DropboxSyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        loadContexts()
        loadGoals()
    }

    private fun loadContexts() {
        viewModelScope.launch {
            repository.getAllContexts().collect { contexts ->
                _state.update { it.copy(contexts = contexts) }
            }
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            repository.getAllGoals().collect { goals ->
                _state.update { it.copy(goals = goals) }
            }
        }
    }

    // ── Context operations ──

    fun createContext(name: String) {
        viewModelScope.launch {
            val sortOrder = _state.value.contexts.size
            repository.insertContext(ContextEntity(name = name, sortOrder = sortOrder))
        }
    }

    fun updateContext(context: ContextEntity) {
        viewModelScope.launch {
            repository.updateContext(context)
        }
    }

    fun deleteContext(context: ContextEntity) {
        viewModelScope.launch {
            repository.deleteContext(context)
        }
    }

    // ── Goal operations ──

    fun createGoal(name: String) {
        viewModelScope.launch {
            repository.insertGoal(GoalEntity(name = name))
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // ── Dropbox Sync ──

    fun connectDropbox() {
        syncManager.connectDropbox()
        _state.update { it.copy(showSyncIndicator = true) }
    }

    fun disconnectDropbox() {
        syncManager.disconnectDropbox()
        _state.update { it.copy(isDropboxConnected = false) }
    }

    fun syncNow() {
        _state.update { it.copy(showSyncIndicator = true) }
        syncManager.syncNow()
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _state.update { it.copy(showSyncIndicator = false, lastSyncTime = System.currentTimeMillis()) }
        }
    }

    fun handleAuthResult(intent: Intent?) {
        val success = syncManager.handleAuthResult(intent)
        _state.update { it.copy(isDropboxConnected = success, showSyncIndicator = false) }
    }

    // ── Dialogs ──

    fun showSettings() {
        _state.update { it.copy(showSettingsDialog = true) }
    }

    fun hideSettings() {
        _state.update { it.copy(showSettingsDialog = false) }
    }

    fun showContextManager() {
        _state.update { it.copy(showContextManager = true) }
    }

    fun hideContextManager() {
        _state.update { it.copy(showContextManager = false) }
    }

    fun showGoalEditor() {
        _state.update { it.copy(showGoalEditor = true) }
    }

    fun hideGoalEditor() {
        _state.update { it.copy(showGoalEditor = false) }
    }
}
