package com.ireddragonicy.llmdroid.data.repository

import com.ireddragonicy.llmdroid.data.local.db.dao.ChatSessionDao
import com.ireddragonicy.llmdroid.data.local.db.entity.ChatSessionEntity
import com.ireddragonicy.llmdroid.di.IoDispatcher
import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatRepository {

    override fun getChatSessions(): Flow<Result<List<ChatSession>>> {
        return chatSessionDao.getAllChatSessions()
            .map<List<ChatSessionEntity>, Result<List<ChatSession>>> { entities ->
                Result.Success(entities.map { it.toDomainModel() })
            }
            .catch { e -> emit(Result.Error(Exception("Failed to load chat sessions", e))) }
            .flowOn(ioDispatcher)
    }

    override suspend fun getChatSession(chatId: String): Result<ChatSession> = withContext(ioDispatcher) {
        try {
            val entity = chatSessionDao.getChatSessionById(chatId)
            if (entity != null) {
                Result.Success(entity.toDomainModel())
            } else {
                Result.Error(NoSuchElementException("Chat session with id $chatId not found"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Failed to get chat session $chatId", e))
        }
    }

    override suspend fun createChatSession(model: LlmModelConfig, title: String?): Result<ChatSession> = withContext(ioDispatcher) {
        try {
            val newSession = ChatSession(
                id = UUID.randomUUID().toString(),
                // Generate title based on model or use provided title
                title = title ?: "Chat (${model.name.take(10)}.. ${LocalDateTime.now().toLocalTime().toString().substringBefore('.')})",
                createdAt = LocalDateTime.now(),
                messages = emptyList(),
                modelType = model
            )
            chatSessionDao.insertChatSession(ChatSessionEntity.fromDomainModel(newSession))
            Result.Success(newSession)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to create chat session", e))
        }
    }

    override suspend fun updateChatMessages(chatId: String, messages: List<ChatMessage>): Result<Unit> = withContext(ioDispatcher) {
        try {
            val session = chatSessionDao.getChatSessionById(chatId)
            if (session != null) {
                // Hanya update message list dan mungkin title jika perlu
                 val updatedSession = session.copy(
                     messages = messages,
                     // Optional: Update title based on first user message if empty
                     title = if (session.title.startsWith("Chat (") && messages.any { it.isFromUser }) {
                        messages.firstOrNull { it.isFromUser }?.message?.take(25)?.let { it + if(it.length >= 25) "..." else ""} ?: session.title
                     } else {
                         session.title
                     }
                 )
                chatSessionDao.updateChatSession(updatedSession)
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Cannot update messages, chat session $chatId not found"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Failed to update messages for chat $chatId", e))
        }
    }

     override suspend fun updateChatSessionTitle(chatId: String, newTitle: String): Result<Unit> = withContext(ioDispatcher) {
         try {
            val session = chatSessionDao.getChatSessionById(chatId)
            if (session != null) {
                chatSessionDao.updateChatSession(session.copy(title = newTitle))
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Cannot update title, chat session $chatId not found"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Failed to update title for chat $chatId", e))
        }
     }

    override suspend fun deleteChatSession(chatId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            chatSessionDao.deleteChatSession(chatId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to delete chat session $chatId", e))
        }
    }

     override suspend fun deleteAllChatSessions(): Result<Unit> = withContext(ioDispatcher) {
         try {
             chatSessionDao.deleteAllChatSessions()
             Result.Success(Unit)
         } catch (e: Exception) {
             Result.Error(Exception("Failed to delete all chat sessions", e))
         }
     }
}