# MyLifeOrganized (MLO) — Android Task Manager

Android-приложение для управления задачами по методологии **MyLifeOrganized (MLO)**.

## Возможности

- **Иерархическое дерево задач** — бесконечная вложенность с drag-and-drop
- **Computed-Score приоритизация** — автоматический расчёт приоритета на основе важности, срочности, времени и веса цели
- **Наследование Importance/Urgency** — дочерние задачи наследуют приоритет от родительских
- **To-Do список** — автоматически формируемый список активных действий
- **Контексты** — @Дом, @Офис, @Компьютер и другие с гибкой системой
- **Зависимости** — задача недоступна, пока не завершены зависимые
- **Геонапоминания** — уведомления при входе/выходе из зоны задачи
- **Повторяющиеся задачи** — daily, weekly, monthly, yearly, weekdays
- **Парсинг естественного языка** — "Купить билеты @Продукты 2026-06-20 30мин"
- **Синхронизация через Dropbox** — двусторонняя синхронизация с Windows-версией
- **Material Design 3** — тёмная и светлая темы
- **MVVM + Hilt + Room + WorkManager**

## Технологии

- **Kotlin** 1.9.22
- **Jetpack Compose** 1.6.0
- **Material Design 3**
- **Room** 2.6.1 (локальная SQLite база)
- **Hilt** 2.50 (dependency injection)
- **WorkManager** 2.9.0 (фоновая синхронизация)
- **Navigation Compose**
- **KSP** (символьная обработка)

## Требования

- Android Studio Hedgehog (2023.1.1) или новее
- Android SDK 34
- JDK 17
- Gradle 8.2+

## Сборка и запуск

### 1. Клонирование

```bash
git clone https://github.com/yourusername/MyLifeOrganized.git
cd MyLifeOrganized
```

### 2. Настройка Dropbox (опционально)

1. Создайте приложение на https://www.dropbox.com/developers/apps
2. Выберите "Scoped access" → "Full Dropbox" или "App folder"
3. В `app/src/main/java/com/mlo/app/data/sync/DropboxClient.kt` замените `DROPBOX_APP_KEY` и `DROPBOX_APP_SECRET`
4. В `AndroidManifest.xml` замените `db-APP_KEY` на `db-` + ваш App Key

### 3. Сборка

```bash
# Debug APK
./gradlew assembleDebug

# Запустить тесты
./gradlew test

# Полный билд
./gradlew build
```

APK будет в `app/build/outputs/apk/debug/app-debug.apk`

### 4. Установка на устройство

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Структура проекта

```
MyLifeOrganized/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/mlo/app/
│   │   │   │   ├── MloApplication.kt          # Application class (Hilt + WorkManager)
│   │   │   │   ├── MainActivity.kt             # Entry point + tab navigation
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── TaskEntity.kt       # Room entity для задач
│   │   │   │   │   │   ├── TaskDao.kt          # Room DAO
│   │   │   │   │   │   ├── TaskDatabase.kt     # Room Database
│   │   │   │   │   │   ├── ContextAndGoalEntities.kt  # Context/Goal entities + DAOs
│   │   │   │   │   │   └── Converters.kt       # Type converters
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── Models.kt           # UI models, enums, PriorityConfig
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── TaskRepository.kt   # Data repository
│   │   │   │   │   └── sync/
│   │   │   │   │       ├── DropboxClient.kt    # Dropbox SDK integration
│   │   │   │   │       ├── DropboxSyncWorker.kt # WorkManager sync worker
│   │   │   │   │       └── DropboxSyncManager.kt # Sync orchestrator
│   │   │   │   ├── di/
│   │   │   │   │   ├── DatabaseModule.kt       # Hilt DB module
│   │   │   │   │   └── SyncModule.kt           # Hilt sync module
│   │   │   │   ├── domain/
│   │   │   │   │   └── PriorityEngine.kt       # Core business logic
│   │   │   │   └── ui/
│   │   │   │       ├── theme/
│   │   │   │       │   └── Theme.kt            # Material3 theme
│   │   │   │       ├── viewmodels/
│   │   │   │       │   ├── TaskViewModel.kt    # Task CRUD + priority
│   │   │   │       │   └── AppViewModel.kt     # App-level state
│   │   │   │       ├── components/
│   │   │   │       │   └── TaskComponents.kt   # Task tree/to-do items
│   │   │   │       └── screens/
│   │   │   │           ├── TaskTreeScreen.kt   # Hierarchical tree
│   │   │   │           ├── TodoScreen.kt       # Active actions list
│   │   │   │           ├── TaskEditorScreen.kt # Task properties editor
│   │   │   │           └── SettingsScreens.kt  # Settings, contexts, goals
│   │   │   └── res/
│   │   └── test/java/com/mlo/app/
│   │       └── PriorityEngineTest.kt           # Unit tests
├── gradle/
│   └── libs.versions.toml                      # Version catalog
├── build.gradle.kts                            # Root build file
├── settings.gradle.kts
└── README.md
```

## API Usage Examples

### Создание задачи

```kotlin
// Simple task
taskViewModel.createTask("Купить молоко")

// Task with parent (subtask)
taskViewModel.createTask("Подзадача", parentId = 1)

// Task with parsed input
taskViewModel.createTaskFromParsedInput(
    "Купить билеты @Продукты 2026-06-20 30мин"
)
```

### Priority Score

```kotlin
val config = PriorityConfig(
    wI = 0.4, // Importance вес
    wU = 0.3, // Urgency вес
    wT = 0.2, // Time вес
    wG = 0.1  // Weekly Goal вес
)

val score = PriorityEngine.calculatePriorityScore(
    task = myTask,
    allTasks = allTasks,
    config = config
)
// score > 100 = high priority
// score > 150 = critical
```

### Проверка активных задач

```kotlin
val isActive = PriorityEngine.isTaskActive(
    task = myTask,
    allTasks = allTasks
)
// Returns true if: status=ACTIVE, startDate<=now, dependencies done
```

### Синхронизация

```kotlin
// Sync now
syncManager.syncNow()

// Connect Dropbox
syncManager.connectDropbox()

// Schedule periodic sync (every 15 min)
syncManager.schedulePeriodicSync()
```

## Тестирование

```bash
./gradlew testDebug
```

Unit тесты покрывают:
- `calculateEffectiveImportance` — наследование важности
- `calculateEffectiveUrgency` — наследование срочности
- `calculateTimeFactor` — временной фактор
- `calculatePriorityScore` — вычисление приоритета
- `isTaskActive` — фильтр активных задач
- `getActiveTasksScored` — сортировка активных
- `parseTaskInput` — парсинг естественного языка
- `computeNextOccurrence` — повторение задач
- `buildFlatTree` — построение дерева

## Лицензия

MIT
