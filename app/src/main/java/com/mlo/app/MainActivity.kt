package com.mlo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.mlo.app.domain.notification.GeofenceSyncWorker
import com.mlo.app.ui.screens.*
import com.mlo.app.ui.theme.MloTheme
import com.mlo.app.ui.viewmodels.AppViewModel
import com.mlo.app.ui.viewmodels.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        } else true

        if (fineLocation && backgroundLocation) {
            GeofenceSyncWorker.schedule(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId = intent?.getLongExtra("taskId", -1L) ?: -1L
        val action = intent?.getStringExtra("action")

        setContent {
            MloTheme {
                MainScreen(
                    initialTaskId = if (taskId > 0) taskId else null,
                    initialAction = action,
                    onRequestLocationPermissions = {
                        requestLocationPermissions()
                    }
                )
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        locationPermissionRequest.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTaskId: Long? = null,
    initialAction: String? = null,
    onRequestLocationPermissions: () -> Unit = {}
) {
    val appViewModel: AppViewModel = hiltViewModel()
    val taskViewModel: TaskViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Dialog states
    var showFlagManager by remember { mutableStateOf(false) }
    var showViewFilter by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var showProfileTemplates by remember { mutableStateOf(false) }
    var showContextManager by remember { mutableStateOf(false) }
    var showGoalManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Task edit dialog: null = closed, 0L = create new, >0 = edit existing
    var showTaskEditId by remember { mutableStateOf<Long?>(null) }

    // Dropbox dialogs
    var showDropboxTokenDialog by remember { mutableStateOf(false) }
    var showSyncIntervalDialog by remember { mutableStateOf(false) }

    // Apply default filter when coming from saved view
    var appliedFilter by remember { mutableStateOf<com.mlo.app.data.model.ViewFilter?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Request location permissions on first launch if not granted
    LaunchedEffect(Unit) {
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocation) {
            // Auto-request when users open geofence features
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Меню") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
                HorizontalDivider()

                // ── Tools section ──
                Text(
                    "Инструменты",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    label = { Text("Управление флагами") },
                    selected = false,
                    onClick = {
                        showFlagManager = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Label, contentDescription = null) },
                    label = { Text("Управление контекстами") },
                    selected = false,
                    onClick = {
                        showContextManager = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    label = { Text("Управление целями") },
                    selected = false,
                    onClick = {
                        showGoalManager = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                    label = { Text("Фильтры и группировка") },
                    selected = false,
                    onClick = {
                        showViewFilter = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Шаблоны профилей") },
                    selected = false,
                    onClick = {
                        showProfileTemplates = true
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("Статистика") },
                    selected = false,
                    onClick = {
                        showStatistics = true
                        appViewModel.refreshStatistics()
                        scope.launch { drawerState.close() }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Sync section ──
                Text(
                    "Синхронизация",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Connection status
                if (appState.isDropboxConnected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Dropbox подключён",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Dropbox не подключён",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Connect / Disconnect button
                if (appState.isDropboxConnected) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
                        label = { Text("Отключить Dropbox") },
                        selected = false,
                        onClick = {
                            appViewModel.disconnectDropbox()
                            scope.launch { drawerState.close() }
                        }
                    )
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        label = { Text("Подключить Dropbox") },
                        selected = false,
                        onClick = {
                            if (appState.dropboxTokenSet) {
                                appViewModel.connectDropbox()
                            } else {
                                showDropboxTokenDialog = true
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                // Sync Now button (only when connected)
                if (appState.isDropboxConnected) {
                    NavigationDrawerItem(
                        icon = {
                            if (appState.isDropboxSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                            }
                        },
                        label = { Text("Синхронизировать сейчас") },
                        selected = false,
                        onClick = {
                            appViewModel.syncNow()
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                // Sync interval setting
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    label = {
                        val intervalLabel = when (appState.syncIntervalMinutes) {
                            0 -> "Интервал: вручную"
                            15 -> "Интервал: 15 мин"
                            30 -> "Интервал: 30 мин"
                            60 -> "Интервал: 1 час"
                            240 -> "Интервал: 4 часа"
                            else -> "Интервал: ${appState.syncIntervalMinutes} мин"
                        }
                        Text(intervalLabel)
                    },
                    selected = false,
                    onClick = {
                        showSyncIntervalDialog = true
                        scope.launch { drawerState.close() }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Settings
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = {
                        showSettings = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    title = { Text(if (selectedTab == 0) "Схема" else "To-Do") },
                    actions = {
                        if (appliedFilter != null) {
                            IconButton(onClick = { appliedFilter = null }) {
                                Icon(
                                    Icons.Default.FilterAltOff,
                                    contentDescription = "Сбросить фильтр"
                                )
                            }
                        }
                        IconButton(onClick = {
                            showViewFilter = true
                        }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Фильтр")
                        }
                        IconButton(onClick = {
                            showStatistics = true
                            appViewModel.refreshStatistics()
                        }) {
                            Icon(Icons.Default.Assessment, contentDescription = "Статистика")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                        label = { Text("Схема") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                        label = { Text("To-Do") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> TaskTreeScreen(
                        viewModel = taskViewModel,
                        onAddTask = { parentId ->
                            showTaskEditId = parentId ?: 0L
                        },
                        onEditTask = { taskId ->
                            showTaskEditId = taskId
                        },
                        onAddSubTask = { parent ->
                            val child = com.mlo.app.data.local.TaskEntity(
                                name = "Новая подзадача",
                                contexts = parent.contexts,
                                goalId = parent.goalId,
                                flags = parent.flags,
                                importance = parent.importance,
                                urgency = parent.urgency,
                                parentId = parent.id
                            )
                            taskViewModel.insertTask(child, parent.id)
                        },
                        onDeleteTask = { taskId ->
                            taskViewModel.deleteTask(taskId)
                            if (showTaskEditId == taskId) showTaskEditId = null
                        }
                    )
                    1 -> TodoScreen(
                        viewModel = taskViewModel,
                        onEditTask = { taskId ->
                            showTaskEditId = taskId
                        },
                        onAddSubTask = { parent ->
                            val child = com.mlo.app.data.local.TaskEntity(
                                name = "Новая подзадача",
                                contexts = parent.contexts,
                                goalId = parent.goalId,
                                flags = parent.flags,
                                importance = parent.importance,
                                urgency = parent.urgency,
                                parentId = parent.id
                            )
                            taskViewModel.insertTask(child, parent.id)
                        },
                        onDeleteTask = { taskId ->
                            taskViewModel.deleteTask(taskId)
                            if (showTaskEditId == taskId) showTaskEditId = null
                        }
                    )
                }
            }
        }
    }

    // ── Overlay Dialogs / Screens ──

    if (showFlagManager) {
        FlagManagerScreen(
            viewModel = appViewModel,
            onDismiss = { showFlagManager = false }
        )
    }

    if (showViewFilter) {
        ViewFilterScreen(
            viewModel = appViewModel,
            onDismiss = { showViewFilter = false },
            onApplyFilter = { filter ->
                appliedFilter = filter
                showViewFilter = false
            }
        )
    }

    if (showStatistics) {
        StatisticsScreen(
            viewModel = appViewModel,
            onDismiss = { showStatistics = false }
        )
    }

    if (showProfileTemplates) {
        ProfileTemplateScreen(
            viewModel = appViewModel,
            onDismiss = { showProfileTemplates = false },
            onApplyTemplate = { template ->
                appViewModel.applyTemplate(template)
                showProfileTemplates = false
            }
        )
    }

    if (showContextManager) {
        ContextManagerScreen(
            viewModel = appViewModel,
            onDismiss = { showContextManager = false }
        )
    }

    if (showGoalManager) {
        GoalManagerDialog(
            viewModel = appViewModel,
            onDismiss = { showGoalManager = false }
        )
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = appViewModel,
            onDismiss = { showSettings = false }
        )
    }

    // ── Task Edit Dialog ──

    showTaskEditId?.let { taskId ->
        TaskEditDialog(
            taskViewModel = taskViewModel,
            taskId = if (taskId == 0L) null else taskId, // 0L = create new, otherwise edit existing
            onDismiss = { showTaskEditId = null }
        )
    }

    // ── Dropbox Token Dialog ──

    if (showDropboxTokenDialog) {
        var tokenInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDropboxTokenDialog = false },
            title = { Text("Подключение к Dropbox") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Введите токен доступа Dropbox:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Access Token") },
                        placeholder = { Text("sl.xxxxxxxx...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Токен можно получить в Dropbox App Console (https://www.dropbox.com/developers/apps).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            appViewModel.saveDropboxToken(tokenInput.trim())
                            appViewModel.connectDropbox()
                            showDropboxTokenDialog = false
                        }
                    },
                    enabled = tokenInput.isNotBlank()
                ) {
                    Text("Подключить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDropboxTokenDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ── Sync Interval Dialog ──

    if (showSyncIntervalDialog) {
        val options = listOf(
            0 to "Вручную",
            15 to "Каждые 15 минут",
            30 to "Каждые 30 минут",
            60 to "Каждый час",
            240 to "Каждые 4 часа"
        )
        AlertDialog(
            onDismissRequest = { showSyncIntervalDialog = false },
            title = { Text("Интервал синхронизации") },
            text = {
                Column {
                    options.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appState.syncIntervalMinutes == minutes,
                                onClick = {
                                    appViewModel.setSyncInterval(minutes)
                                    showSyncIntervalDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncIntervalDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
