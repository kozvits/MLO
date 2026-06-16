package com.mlo.app.di

import android.content.Context
import com.mlo.app.data.sync.DropboxClient
import com.mlo.app.data.sync.DropboxSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideDropboxClient(@ApplicationContext context: Context): DropboxClient {
        return DropboxClient(context)
    }

    @Provides
    @Singleton
    fun provideDropboxSyncManager(
        @ApplicationContext context: Context,
        dropboxClient: DropboxClient
    ): DropboxSyncManager {
        return DropboxSyncManager(context, dropboxClient)
    }
}
