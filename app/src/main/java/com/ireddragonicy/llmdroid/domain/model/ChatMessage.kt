package com.ireddragonicy.llmdroid.domain.model

import java.util.UUID

// Definisikan prefix di domain/util/Constants.kt atau domain layer jika perlu
const val USER_PREFIX = "user"
const val MODEL_PREFIX = "model"

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val rawMessage: String = "",
    val author: String, // e.g., USER_PREFIX, MODEL_PREFIX
    val timestamp: Long = System.currentTimeMillis(), // Tambahkan timestamp
    val isLoading: Boolean = false,
    val isThinking: Boolean = false, // State spesifik model? Mungkin bisa di-handle di UI/ViewModel
    val error: String? = null // Untuk menampilkan error pada message item
) {
    val isEmpty: Boolean
        get() = rawMessage.trim().isEmpty()

    val isFromUser: Boolean
        get() = author == USER_PREFIX

    val message: String
        get() = rawMessage.trim() // Pesan yang sudah dibersihkan untuk display
}