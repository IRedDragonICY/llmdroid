package com.ireddragonicy.llmdroid.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ireddragonicy.llmdroid.data.local.db.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :chatId")
    suspend fun getChatSessionById(chatId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(chatSession: ChatSessionEntity)

    @Update
    suspend fun updateChatSession(chatSession: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :chatId")
    suspend fun deleteChatSession(chatId: String)

    // Opsional: Query untuk menghapus semua sesi
    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllChatSessions()
}