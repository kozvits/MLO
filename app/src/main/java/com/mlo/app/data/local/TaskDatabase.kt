package com.mlo.app.data.local

import androidx.room.*
import android.content.Context

@Database(
    entities = [
        TaskEntity::class,
        ContextEntity::class,
        GoalEntity::class,
        ContextHourEntity::class,
        FlagEntity::class,
        TaskFlagCrossRef::class,
        ReminderEntity::class,
        ProfileTemplateEntity::class,
        ViewEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun contextDao(): ContextDao
    abstract fun goalDao(): GoalDao
    abstract fun contextHourDao(): ContextHourDao
    abstract fun flagDao(): FlagDao
    abstract fun taskFlagDao(): TaskFlagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun profileTemplateDao(): ProfileTemplateDao
    abstract fun viewDao(): ViewDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "mlo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
