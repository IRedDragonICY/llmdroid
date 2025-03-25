package com.ireddragonicy.llmdroid.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ireddragonicy.llmdroid.data.local.db.converter.ChatMessageListConverter
import com.ireddragonicy.llmdroid.data.local.db.converter.DateTimeConverter
import com.ireddragonicy.llmdroid.data.local.db.converter.ModelConfigConverter
import com.ireddragonicy.llmdroid.data.local.db.dao.ChatSessionDao
import com.ireddragonicy.llmdroid.data.local.db.entity.ChatSessionEntity

@Database(
    entities = [ChatSessionEntity::class],
    // --- PERBAIKAN DI SINI: Naikkan versi database ---
    version = 2, // Naikkan versi dari 1 ke 2 karena perubahan cara penyimpanan LlmModelConfig
    // --- AKHIR PERBAIKAN ---
    exportSchema = false
)
@TypeConverters(
    DateTimeConverter::class,
    ModelConfigConverter::class, // Konverter yang sudah diperbaiki
    ChatMessageListConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatSessionDao(): ChatSessionDao

    // Companion object tidak diperlukan, Hilt akan menyediakan instance.
    // Pastikan fallbackToDestructiveMigration() digunakan di DatabaseModule.kt
    // selama pengembangan, atau implementasikan Migration jika ini untuk produksi.
}