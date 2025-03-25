package com.ireddragonicy.llmdroid.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
// Gunakan model domain untuk list message
import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.data.local.db.converter.ChatMessageListConverter
import com.ireddragonicy.llmdroid.data.local.db.converter.DateTimeConverter
import com.ireddragonicy.llmdroid.data.local.db.converter.ModelConfigConverter
import java.time.LocalDateTime

@Entity(tableName = "chat_sessions")
@TypeConverters(
    DateTimeConverter::class,
    ModelConfigConverter::class,
    ChatMessageListConverter::class
)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: LocalDateTime,
    val messages: List<ChatMessage>, // Simpan sebagai domain model
    val modelType: LlmModelConfig // Simpan sebagai domain model
) {
    // Mapper ke Domain Model
    fun toDomainModel(): ChatSession {
        return ChatSession(
            id = id,
            title = title,
            createdAt = createdAt,
            messages = messages,
            modelType = modelType
        )
    }

    companion object {
        // Mapper dari Domain Model
        fun fromDomainModel(chatSession: ChatSession): ChatSessionEntity {
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