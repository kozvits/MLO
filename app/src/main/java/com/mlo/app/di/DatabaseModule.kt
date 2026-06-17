package com.mlo.app.di

import android.content.Context
import com.mlo.app.data.local.*
import com.mlo.app.data.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TaskDatabase {
        return TaskDatabase.getInstance(context)
    }

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideContextDao(database: TaskDatabase): ContextDao = database.contextDao()

    @Provides
    fun provideContextHourDao(database: TaskDatabase): ContextHourDao = database.contextHourDao()

    @Provides
    fun provideGoalDao(database: TaskDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideFlagDao(database: TaskDatabase): FlagDao = database.flagDao()

    @Provides
    fun provideTaskFlagDao(database: TaskDatabase): TaskFlagDao = database.taskFlagDao()

    @Provides
    fun provideReminderDao(database: TaskDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideProfileTemplateDao(database: TaskDatabase): ProfileTemplateDao = database.profileTemplateDao()

    @Provides
    fun provideViewDao(database: TaskDatabase): ViewDao = database.viewDao()

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        contextDao: ContextDao,
        contextHourDao: ContextHourDao,
        goalDao: GoalDao,
        flagDao: FlagDao,
        taskFlagDao: TaskFlagDao,
        reminderDao: ReminderDao,
        profileTemplateDao: ProfileTemplateDao,
        viewDao: ViewDao
    ): TaskRepository {
        return TaskRepository(
            taskDao = taskDao,
            contextDao = contextDao,
            contextHourDao = contextHourDao,
            goalDao = goalDao,
            flagDao = flagDao,
            taskFlagDao = taskFlagDao,
            reminderDao = reminderDao,
            profileTemplateDao = profileTemplateDao,
            viewDao = viewDao
        )
    }
}
