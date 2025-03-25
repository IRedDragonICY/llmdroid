package com.ireddragonicy.llmdroid.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ireddragonicy.llmdroid.data.db.dao.ChatSessionDao
import com.ireddragonicy.llmdroid.data.db.entities.ChatSessionEntity

@Database(entities = [ChatSessionEntity::class], version = 1, exportSchema = false)
@TypeConverters(DateTimeConverter::class, ModelConfigConverter::class, ChatMessageListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "llmdroid_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}