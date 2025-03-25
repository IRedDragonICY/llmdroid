package com.ireddragonicy.llmdroid.data

import android.content.Context
import com.ireddragonicy.llmdroid.InferenceModel
import com.ireddragonicy.llmdroid.data.db.AppDatabase
import com.ireddragonicy.llmdroid.data.db.entities.ChatSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChatRepository private constructor(
    private val database: AppDatabase,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "ChatRepository"

    val chatSessions: Flow<List<ChatSession>> = database.chatSessionDao()
        .getAllChatSessions()
        .map { entities -> entities.map { it.toModel() } }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun getChatSession(chatId: String): ChatSession? {
        return database.chatSessionDao().getChatSessionById(chatId)?.toModel()
    }

    fun createChatSession(model: LlmModelConfig = InferenceModel.Companion.model): ChatSession {
        val newSession = ChatSession(modelType = model)
        coroutineScope.launch(Dispatchers.IO) {
            database.chatSessionDao().insertChatSession(ChatSessionEntity.fromModel(newSession))
        }
        return newSession
    }

    fun updateChatMessages(chatId: String, messages: List<ChatMessage>) {
        coroutineScope.launch(Dispatchers.IO) {
            val session = database.chatSessionDao().getChatSessionById(chatId)
            if (session != null) {
                val updatedSession = session.copy(messages = messages)
                database.chatSessionDao().updateChatSession(updatedSession)
            }
        }
    }

    fun deleteChatSession(chatId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            database.chatSessionDao().deleteChatSession(chatId)
        }
    }

    companion object {
        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return instance ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val scope = CoroutineScope(Dispatchers.IO)
                instance ?: ChatRepository(database, scope).also { instance = it }
            }
        }
    }
}