package com.ireddragonicy.llmdroid.util

object Constants {
    const val MAX_TOKENS_DEFAULT = 1024 // Default max tokens if not specified by model
    const val DECODE_TOKEN_OFFSET = 256 // Contoh konstanta (jika masih relevan)

    // Prefix bisa dipindahkan ke sini dari ChatMessage.kt
    const val USER_PREFIX = "user"
    const val MODEL_PREFIX = "model"
    const val THINKING_LABEL = "thinking" // Jika digunakan secara global

    // Nama preferensi jika tidak di SecureStorage
    const val PLAIN_PREFS_NAME = "llmdroid_plain_prefs"
}