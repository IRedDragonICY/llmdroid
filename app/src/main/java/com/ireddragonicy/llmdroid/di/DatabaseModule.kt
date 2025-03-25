package com.ireddragonicy.llmdroid.di

import android.content.Context
import androidx.room.Room
import com.ireddragonicy.llmdroid.data.local.db.AppDatabase
import com.ireddragonicy.llmdroid.data.local.db.dao.ChatSessionDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "llmdroid_database"
        )
            .fallbackToDestructiveMigration() // Use proper migrations in production
            .build()
    }

    @Provides
    @Singleton
    fun provideChatSessionDao(appDatabase: AppDatabase): ChatSessionDao {
        return appDatabase.chatSessionDao()
    }
}