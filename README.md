# MWO — GTD / MLO Task Manager for Android

Android-приложение для управления задачами по методологии **GTD / MyLifeOrganized (MLO)**.
Бесплатный аналог MLO v5 — все фичи без paywall.

## Возможности

- **Иерархическое дерево задач** — бесконечная вложенность (parent → child → subtask)
- **Computed-Score приоритизация** — автоматический расчёт приоритета по формуле: `wI·I + wU·U + wT·T + wG·G`
- **Наследование Importance/Urgency** — дочерние задачи наследуют приоритет от родительских
- **To-Do список** — автоматически формируемый список активных действий (только те, что доступны сейчас)
- **Контексты (@Дом, @Офис, @Компьютер)** — объединение задач по локациям с почасовой доступностью
- **Геонапоминания** — уведомления при входе/выходе из геозоны задачи (Geofencing API)
- **Флаги** — цветные метки для категоризации задач (Importance/Urgency presets)
- **Фильтры (Saved Views)** — сохраняемые фильтры для быстрой смены фокуса
- **Шаблоны профилей** — быстрая настройка контекстов, флагов и приоритетов
- **Повторяющиеся задачи** — daily, weekly, monthly, yearly, weekdays
- **Зависимости** — задача недоступна, пока не завершены зависимые (dependsOn)
- **Парсинг естественного языка** — `"Купить билеты @Продукты 2026-06-20 30мин"`
- **Напоминания** — отложенные уведомления на конкретную дату/время
- **Статистика** — графики выполнения, пиковые часы продуктивности
- **Dropbox-синхронизация** — бэкап и восстановление данных через Dropbox API v2
- **Widgets** — Today Tasks widget (Glance) + Quick Settings Tile
- **Material Design 3** — Material You темы (светлая / тёмная)
- **Clean Architecture + MVVM + Hilt + Room + WorkManager**

## Технологии

| Компонент | Версия |
|---|---|
| **Kotlin** | 1.9.22 |
| **Jetpack Compose** | 1.6.0 / BOM 2024.02 |
| **Material Design 3** | Compose Material 3 |
| **Room** | 2.6.1 (SQLite, 9 entities, KSP) |
| **Hilt** | 2.50 (Dependency Injection) |
| **WorkManager** | 2.9.0 (фоновые задачи) |
| **KSP** | 1.9.22-1.0.17 (символьная обработка) |
| **OkHttp** | 4.12.0 (Dropbox API) |
| **Gson** | 2.10.1 (Dropbox JSON) |
| **Glance** | 1.0.0 (App Widgets) |
| **AndroidX Navigation** | 2.7.7 |
| **Coroutines** | 1.8.0 |
| **compileSdk / targetSdk** | 35 |

## Требования

- Android Studio Hedgehog (2023.1.1) или новее / IntelliJ IDEA
- Android SDK 35
- JDK 17
- Gradle 8.5+

## Быстрый старт

### 1. Клонирование

```bash
git clone https://github.com/kozvits/Kislo-GTD.git
cd Kislo-GTD
```

### 2. Сборка APK

```bash
# Debug
./gradlew :app:assembleDebug

# Release
./gradlew :app:assembleRelease
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Установка

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Тесты

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug

# Полная проверка
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Настройка Dropbox (опционально)

Для работы синхронизации с Dropbox:

1. Создайте приложение на [Dropbox App Console](https://www.dropbox.com/developers/apps)
2. Во вкладке **Permissions** включите scopes:
   - `files.metadata.read`
   - `files.metadata.write`
   - `files.content.read`
   - `files.content.write`
3. В `app/src/main/java/com/mlo/app/domain/dropbox/DropboxConfig.kt` замените `ACCESS_TOKEN` на ваш токен
4. В `AndroidManifest.xml` замените `android:scheme="db-APP_KEY"` на `db-` + ваш App Key

## Структура проекта

```
MyOrganizer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/mlo/app/
│   │   │   │   ├── MloApplication.kt                # Hilt Application + WorkManager init
│   │   │   │   ├── MainActivity.kt                   # Entry point, drawer, tab nav
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── TaskEntity.kt             # Room entity: задачи
│   │   │   │   │   │   ├── ContextEntitiesMin.kt     # ContextEntity + DAO
│   │   │   │   │   │   ├── GoalAndHourEntities.kt    # GoalEntity + ContextHourEntity
│   │   │   │   │   │   ├── FlagEntity.kt             # FlagEntity + FlagDao
│   │   │   │   │   │   ├── ViewEntity.kt             # SavedView/SavedViewFilter
│   │   │   │   │   │   ├── ReminderEntity.kt         # ReminderEntity + ReminderDao
│   │   │   │   │   │   ├── ProfileTemplateEntity.kt  # TemplateProfile entity
│   │   │   │   │   │   ├── TaskFlagCrossRef.kt       # M:N Task ↔ Flag
│   │   │   │   │   │   ├── TaskDao.kt                # Task DAO (main query interface)
│   │   │   │   │   │   ├── TaskDatabase.kt           # Room DB (9 entities, 9 DAOs)
│   │   │   │   │   │   └── Converters.kt             # Room TypeConverters
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Models.kt                 # UI enums, states
│   │   │   │   │   │   ├── PriorityConfig.kt         # Weights: wI, wU, wT, wG
│   │   │   │   │   │   └── GContextModel.kt          # Context UI model
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── TaskRepository.kt          # Single source of truth
│   │   │   │   │   └── sync/
│   │   │   │   │       ├── DropboxClient.kt           # (legacy) Dropbox HTTP client
│   │   │   │   │       ├── DropboxSyncWorker.kt       # (legacy) WorkManager worker
│   │   │   │   │       └── DropboxSyncManager.kt     # (legacy) sync orchestrator
│   │   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   ├── DatabaseModule.kt             # Hilt module: DB + DAOs
│   │   │   │   │   └── SyncModule.kt                 # Hilt module: sync workers
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── PriorityEngine.kt             # Core scoring algorithm
│   │   │   │   │   ├── ProfileTemplateData.kt        # Preset templates
│   │   │   │   │   ├── dropbox/
│   │   │   │   │   │   ├── DropboxConfig.kt          # Token, endpoints
│   │   │   │   │   │   ├── DropboxSyncManager.kt     # OkHttp-based sync v2
│   │   │   │   │   │   ├── SyncData.kt               # Serialization models
│   │   │   │   │   │   └── SyncSeedData.kt           # Seed data loader
│   │   │   │   │   └── notification/
│   │   │   │   │       ├── NotificationHelper.kt     # Notification channels
│   │   │   │   │       ├── ReminderCheckWorker.kt    # Reminder scheduler
│   │   │   │   │       ├── GeofenceBroadcastReceiver.kt  # Geofence events
│   │   │   │   │       └── GeofenceSyncWorker.kt     # Geofence setup daemon
│   │   │   │   │
│   │   │   │   └── ui/
│   │   │   │       ├── theme/
│   │   │   │       │   └── Theme.kt                  # M3 theme (light/dark)
│   │   │   │       ├── viewmodels/
│   │   │   │       │   ├── AppViewModel.kt           # App-level state + Dropbox
│   │   │   │       │   ├── TaskViewModel.kt          # Task CRUD, priority
│   │   │   │       │   └── TaskUiState.kt            # UI models for task screen
│   │   │   │       ├── components/
│   │   │   │       │   └── TaskComponents.kt         # TaskItem, TaskNode, etc.
│   │   │   │       ├── screens/
│   │   │   │       │   ├── TaskTreeScreen.kt         # Hierarchical task tree
│   │   │   │       │   ├── TodoScreen.kt             # Active actions tab
│   │   │   │       │   ├── TaskEditorScreen.kt       # Task edit properties
│   │   │   │       │   ├── SettingsScreens.kt        # Settings dialog
│   │   │   │       │   ├── FlagManagerScreen.kt      # Flag CRUD
│   │   │   │       │   ├── ViewFilterScreen.kt       # Saved view filters
│   │   │   │       │   ├── StatisticsScreen.kt       # Stats charts
│   │   │   │       │   ├── ProfileTemplateScreen.kt  # Template management
│   │   │   │       │   └── ContextManagerScreen.kt   # Contexts + hours
│   │   │   │       └── widgets/
│   │   │   │           ├── TodayTasksWidget.kt       # Glance widget
│   │   │   │           └── QuickTaskTileService.kt   # Quick Settings tile
│   │   │   │
│   │   │   └── res/
│   │   │       ├── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
│   │   │       │   ├── ic_launcher.png
│   │   │       │   └── ic_launcher_round.png
│   │   │       ├── drawable/
│   │   │       │   └── ic_launcher_foreground.xml
│   │   │       └── values/
│   │   │           ├── strings.xml
│   │   │           └── ic_launcher_background.xml
│   │   │
│   │   └── test/java/com/mlo/app/
│   │       └── PriorityEngineTest.kt            # 21 unit tests
│   │
├── gradle/
│   └── libs.versions.toml                       # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## API Usage Examples

### Создание задачи

```kotlin
// Простая задача
taskViewModel.createTask("Купить молоко")

// Подзадача с родителем
taskViewModel.createTask("Подзадача", parentId = 1)

// С парсингом естественного языка
taskViewModel.createTaskFromParsedInput(
    "Купить билеты @Продукты 2026-06-20 30мин"
)
```

### Priority Score

```kotlin
val config = PriorityConfig(
    wI = 0.4, // Importance вес (40%)
    wU = 0.3, // Urgency вес (30%)
    wT = 0.2, // Time factor вес (20%)
    wG = 0.1  // Weekly Goal вес (10%)
)

val score = PriorityEngine.calculatePriorityScore(
    task = myTask,
    allTasks = allTasks,
    config = config
)
// score range: 0 — 200+
// score > 100 = high priority
// score > 150 = critical
```

### Проверка активных задач

```kotlin
val isActive = PriorityEngine.isTaskActive(
    task = myTask,
    allTasks = allTasks
)
// true если: status=ACTIVE, startDate<=now, dependencies выполнены
```

### Работа с флагами

```kotlin
// Установить флаг задаче
flagDao.insertTaskFlag(TaskFlagCrossRef(taskId = 1, flagId = 2))

// Получить все задачи с флагом
val flagged: List<TaskWithFlags> = taskDao.getAllTasksWithFlags()
```

### Dropbox-синхронизация

```kotlin
// Подключиться (проверяет токен)
viewModel.connectDropbox()

// Выполнить синхронизацию
viewModel.syncNow()

// Отключиться
viewModel.disconnectDropbox()
```

Синхронизация выгружает в `/MLO/mlo_backup_TIMESTAMP.json` все задачи, контексты, цели, флаги и фильтры.

### Stats / Статистика

```kotlin
viewModel.refreshStatistics()

// State содержит:
// - overdueCount / completedTodayCount
// - peakHour (час с макс. выполнением)
// - averageCompletionRate (доля завершённых в срок)
// - monthlyCompletionChart + weeklyDistribution
```

## Room Database

9 entities, 9 DAOs:

| Entity | DAO | Назначение |
|---|---|---|
| `TaskEntity` | `TaskDao` | Основные задачи (parentId, importance, urgency, dueDate, etc.) |
| `ContextEntity` | `ContextDao` | Контексты (@Дом, @Офис) |
| `ContextHourEntity` | `ContextDao` | Часы доступности контекстов |
| `GoalEntity` | `GoalDao` | Цели (weeklyGoalWeight, colour) |
| `FlagEntity` | `FlagDao` | Цветные флаги-метки |
| `TaskFlagCrossRef` | `FlagDao` | M:N связь задача↔флаг |
| `SavedView` | `ViewDao` | Сохранённые фильтры |
| `ReminderEntity` | `ReminderDao` | Напоминания с датой |
| `ProfileTemplate` | `ProfileTemplateDao` | Шаблоны профилей |

## Тестирование

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug
```

21 unit tests покрывают:

| Тест | Описание |
|---|---|
| `effective importance defaults to task importance when no parent` | Базовая важность |
| `effective importance inherits from parent` | Наследование важности |
| `effective importance handles deep nesting` | Глубокое наследование |
| `effective importance clamps to 0-200 range` | Клиппинг |
| `effective urgency inherits same as importance` | Наследование срочности |
| `time factor is neutral with no due date` | Нейтральный фактор времени |
| `time factor boosts when overdue` | Бонус просрочки |
| `time factor decreases with remaining time` | Убывание при запасе времени |
| `priority score uses weighted formula` | Корректность формулы |
| `higher importance gives higher score` | Более важные — выше |
| `active task without conditions is active` | Базовая активность |
| `completed task is not active` | Завершённые неактивны |
| `future start date task is not active` | Отложенные неактивны |
| `task with incomplete dependency is not active` | Зависимости блокируют |
| `task with completed dependency is active` | Выполненные зависимости |
| `task with multiple completed dependencies is active` | Множественные зависимости |
| `getActiveTasksScored returns only active tasks sorted by score` | Фильтр + сортировка |
| `parseTaskInput extracts name, date, duration, contexts` | NL-парсинг |
| `parseTaskInput handles input without date or duration` | NL-парсинг (minimal) |
| `daily recurrence advances by one day` | Повседневная периодичность |
| `buildFlatTree flattens hierarchy with depth` | Построение дерева |

## Лицензия

MIT
