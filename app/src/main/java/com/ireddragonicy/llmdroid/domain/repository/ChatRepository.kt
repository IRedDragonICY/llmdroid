package com.ireddragonicy.llmdroid.domain.repository

import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /**
     * Mendapatkan semua chat session sebagai Flow, diurutkan terbaru dulu.
     * @return Flow yang memancarkan Result berisi List<ChatSession>.
     */
    fun getChatSessions(): Flow<Result<List<ChatSession>>>

    /**
     * Mendapatkan satu chat session berdasarkan ID.
     * @param chatId ID sesi yang dicari.
     * @return Result berisi ChatSession jika ditemukan.
     */
    suspend fun getChatSession(chatId: String): Result<ChatSession>

    /**
     * Membuat chat session baru.
     * @param model Model LLM yang akan digunakan untuk sesi ini.
     * @param title Judul awal opsional untuk sesi.
     * @return Result berisi ChatSession yang baru dibuat.
     */
    suspend fun createChatSession(model: LlmModelConfig, title: String? = null): Result<ChatSession>

    /**
     * Memperbarui daftar pesan dalam chat session tertentu.
     * @param chatId ID sesi yang akan diperbarui.
     * @param messages List pesan yang baru.
     */
    suspend fun updateChatMessages(chatId: String, messages: List<ChatMessage>): Result<Unit>

     /**
      * Memperbarui judul chat session tertentu.
      * @param chatId ID sesi yang akan diperbarui.
      * @param newTitle Judul baru untuk sesi.
      */
     suspend fun updateChatSessionTitle(chatId: String, newTitle: String): Result<Unit>


    /**
     * Menghapus chat session berdasarkan ID.
     * @param chatId ID sesi yang akan dihapus.
     */
    suspend fun deleteChatSession(chatId: String): Result<Unit>

    /**
     * Menghapus semua chat session. Hati-hati menggunakan ini!
     */
    suspend fun deleteAllChatSessions(): Result<Unit>
}