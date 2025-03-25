package com.ireddragonicy.llmdroid.domain.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String, // Buat non-nullable, generate di repository/usecase
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val messages: List<ChatMessage> = emptyList(),
    val modelType: LlmModelConfig // Model yang digunakan sesi ini
) {
    fun getFormattedDate(): String {
        // Pertimbangkan locale user
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return createdAt.format(formatter)
    }

    fun getPreviewText(): String {
        // Ambil pesan non-loading terakhir (bisa user atau model)
        val lastMeaningfulMessage = messages.lastOrNull { !it.isLoading && it.message.isNotBlank() }
        val preview = lastMeaningfulMessage?.message?.take(40) ?: ""
        return if (preview.length >= 40) "$preview..." else preview
    }

    val lastMessageTimestamp: Long
        get() = messages.lastOrNull()?.timestamp ?: createdAt.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
}