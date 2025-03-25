package com.ireddragonicy.llmdroid

import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val messages: List<ChatMessage> = emptyList(),
    val modelType: Model = InferenceModel.model
) {
    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return createdAt.format(formatter)
    }
    
    fun getPreviewText(): String {
        val userMessage = messages.firstOrNull { it.isFromUser }
        return userMessage?.message?.take(30)?.plus(if ((userMessage.message.length > 30)) "..." else "") ?: ""
    }
}
