package com.ireddragonicy.llmdroid.data.local.db.converter

import android.util.Log // Tambahkan import Log
import androidx.room.TypeConverter
import com.google.gson.Gson // Gson tidak diperlukan lagi untuk enum
import com.google.gson.reflect.TypeToken
import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
}

// --- PERBAIKAN DI SINI ---
class ModelConfigConverter {
    @TypeConverter
    fun fromLlmModelConfig(value: LlmModelConfig?): String? {
        // Simpan nama unik enum sebagai String
        return value?.name
    }

    @TypeConverter
    fun toLlmModelConfig(value: String?): LlmModelConfig? {
        // Konversi String nama kembali ke enum constant
        return value?.let { name ->
            try {
                LlmModelConfig.valueOf(name)
            } catch (e: IllegalArgumentException) {
                // Tangani kasus jika nama yang disimpan tidak valid
                // (misalnya, enum diganti namanya, data rusak)
                Log.e("ModelConfigConverter", "Failed to convert '$name' to LlmModelConfig enum", e)
                null // Kembalikan null atau nilai default jika sesuai
            }
        }
    }
}
// --- AKHIR PERBAIKAN ---

class ChatMessageListConverter {
    private val gson = Gson()
    private val listType = object : TypeToken<List<ChatMessage>>() {}.type

    @TypeConverter
    fun fromChatMessageList(value: List<ChatMessage>?): String? {
        // --- PERBAIKAN DI SINI ---
        // Konversi list ke JSON secara langsung.
        // Jika value null, hasilnya null.
        // Jika value emptyList(), hasilnya "[]" (String non-null).
        // Jika value list berisi data, hasilnya "[...]" (String non-null).
        return value?.let { gson.toJson(it) }
        // --- AKHIR PERBAIKAN ---
    }

    @TypeConverter
    fun toChatMessageList(value: String?): List<ChatMessage> { // Return tetap non-nullable
        return value?.let {
            try {
                // Gunakan elvis operator untuk default ke emptyList jika parsing menghasilkan null
                gson.fromJson<List<ChatMessage>>(it, listType) ?: emptyList()
            } catch (e: Exception) {
                Log.e("ChatMessageListConverter", "Failed to parse ChatMessage list JSON", e)
                emptyList<ChatMessage>() // Kembalikan list kosong jika ada error parsing
            }
        } ?: emptyList() // Kembalikan list kosong jika value string adalah null
    }
}