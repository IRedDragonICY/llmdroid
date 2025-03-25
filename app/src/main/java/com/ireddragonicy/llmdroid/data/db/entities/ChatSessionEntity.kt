package com.ireddragonicy.llmdroid.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ireddragonicy.llmdroid.data.ChatMessage
import com.ireddragonicy.llmdroid.data.ChatSession
import com.ireddragonicy.llmdroid.data.LlmModelConfig
import com.ireddragonicy.llmdroid.data.db.ChatMessageListConverter
import com.ireddragonicy.llmdroid.data.db.DateTimeConverter
import com.ireddragonicy.llmdroid.data.db.ModelConfigConverter
import java.time.LocalDateTime

@Entity(tableName = "chat_sessions")
@TypeConverters(DateTimeConverter::class, ModelConfigConverter::class, ChatMessageListConverter::class)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: LocalDateTime,
    val messages: List<ChatMessage>,
    val modelType: LlmModelConfig
) {
    fun toModel(): ChatSession {
        return ChatSession(
            id = id,
            title = title,
            createdAt = createdAt,
            messages = messages,
            modelType = modelType
        )
    }

    companion object {
        fun fromModel(chatSession: ChatSession): ChatSessionEntity {
            return ChatSessionEntity(
                id = chatSession.id,
                title = chatSession.title,
                createdAt = chatSession.createdAt,
                messages = chatSession.messages,
                modelType = chatSession.modelType
            )
        }
    }
}