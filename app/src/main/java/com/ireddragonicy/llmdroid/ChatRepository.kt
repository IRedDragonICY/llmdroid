package com.ireddragonicy.llmdroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatRepository private constructor() {
    private val TAG = "ChatRepository"
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    fun getChatSession(chatId: String): ChatSession? {
        return _chatSessions.value.find { it.id == chatId }
    }

    fun createChatSession(model: Model = InferenceModel.model): ChatSession {
        val newSession = ChatSession(modelType = model)
        _chatSessions.update { listOf(newSession) + it }
        return newSession
    }

    fun updateChatMessages(chatId: String, messages: List<ChatMessage>) {
        _chatSessions.update { sessions ->
            sessions.map { session ->
                if (session.id == chatId) {
                    session.copy(messages = messages)
                } else {
                    session
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository().also { instance = it }
            }
        }
    }
}
