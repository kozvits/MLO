package com.mlo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mlo.app.ui.screens.*
import com.mlo.app.ui.theme.MloTheme
import com.mlo.app.ui.viewmodels.AppViewModel
import com.mlo.app.ui.viewmodels.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MloTheme {
                MloMainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Handle Dropbox auth result
        val appViewModel = androidx.lifecycle.ViewModelProvider(this)
            .get(AppViewModel::class.java)
        appViewModel.handleAuthResult()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MloMainScreen() {
    val taskViewModel: TaskViewModel = hiltViewModel()
    val appViewModel: AppViewModel = hiltViewModel()
    val taskState by taskViewModel.state.collectAsState()
    val appState by appViewModel.state.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Схема" to Icons.Default.AccountTree,
        "To-Do" to Icons.Default.Checklist
    )

    // Context names list for the editor
    val contextNames = remember(appState.contexts) {
        appState.contexts.map { it.name }
    }
    val goalsList = remember(appState.goals) {
        appState.goals.map { it.id to it.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyLifeOrganized") },
                actions = {
                    IconButton(onClick = { appViewModel.showSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (title, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> TaskTreeScreen(
                    viewModel = taskViewModel,
                    onAddTask = { parentId ->
                        if (parentId != null) {
                            taskViewModel.openNewTaskEditor(parentId = parentId)
                        } else {
                            taskViewModel.openNewTaskEditor()
                        }
                    },
                    onEditTask = { taskId ->
                        taskViewModel.openEditor(taskId)
                    }
                )
                1 -> TodoScreen(
                    viewModel = taskViewModel,
                    onEditTask = { taskId ->
                        taskViewModel.openEditor(taskId)
                    }
                )
            }
        }
    }

    // ── Task editor dialog ──
    if (taskState.showEditor && taskState.editorTask != null) {
        TaskEditorScreen(
            task = taskState.editorTask!!,
            allContexts = contextNames,
            allGoals = goalsList,
            allTasks = taskState.allTasks,
            onSave = { taskViewModel.updateTask(it) },
            onDelete = {
                taskState.editorTask?.let { taskViewModel.deleteTask(it.id) }
                taskViewModel.closeEditor()
            },
            onDismiss = { taskViewModel.closeEditor() }
        )
    }

    // ── Settings dialog ──
    if (appState.showSettingsDialog) {
        SettingsDialog(
            viewModel = appViewModel,
            onDismiss = { appViewModel.hideSettings() }
        )
    }

    // ── Context manager dialog ──
    if (appState.showContextManager) {
        ContextManagerDialog(
            viewModel = appViewModel,
            onDismiss = { appViewModel.hideContextManager() }
        )
    }

    // ── Goal manager dialog ──
    if (appState.showGoalEditor) {
        GoalManagerDialog(
            viewModel = appViewModel,
            onDismiss = { appViewModel.hideGoalEditor() }
        )
    }
}
