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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
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

        // Handle deep link / intent actions
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

    // Dialog states for new feature screens
    var showFlagManager by remember { mutableStateOf(false) }
    var showViewFilter by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var showProfileTemplates by remember { mutableStateOf(false) }
    var showContextManager by remember { mutableStateOf(false) }

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
                    icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                    label = { Text("Фильтры и группировка") },
                    selected = false,
                    onClick = {
                        showViewFilter = true
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
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Контексты и локации") },
                    selected = false,
                    onClick = {
                        showContextManager = true
                        scope.launch { drawerState.close() }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = {
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
                        // Filter indicator
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
                            val newTask = com.mlo.app.data.local.TaskEntity(name = "Новая задача", parentId = parentId)
                            taskViewModel.insertTask(newTask, parentId)
                        },
                        onEditTask = { taskId ->
                            taskViewModel.loadTaskFlags(taskId)
                            taskViewModel.loadTaskReminders(taskId)
                        }
                    )
                    1 -> TodoScreen(
                        viewModel = taskViewModel,
                        onEditTask = { taskId ->
                            taskViewModel.loadTaskFlags(taskId)
                            taskViewModel.loadTaskReminders(taskId)
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
}
