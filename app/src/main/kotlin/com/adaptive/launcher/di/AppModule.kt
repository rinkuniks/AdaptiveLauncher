package com.adaptive.launcher.di

import android.content.Context
import androidx.room.Room
import com.adaptive.launcher.data.apps.LauncherAppsProvider
import com.adaptive.launcher.data.apps.SystemLauncherAppsProvider
import com.adaptive.launcher.data.favorites.AppDatabase
import com.adaptive.launcher.data.favorites.FavoriteDao
import com.adaptive.launcher.data.streams.StreamDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBinds {
    @Binds
    @Singleton
    abstract fun bindLauncherAppsProvider(impl: SystemLauncherAppsProvider): LauncherAppsProvider
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvides {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "adaptive.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideStreamDao(db: AppDatabase): StreamDao = db.streamDao()
}
