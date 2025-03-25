package com.ireddragonicy.llmdroid.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ireddragonicy.llmdroid.data.ChatMessage
import com.ireddragonicy.llmdroid.data.LlmModelConfig
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

class ModelConfigConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLlmModelConfig(value: LlmModelConfig): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLlmModelConfig(value: String): LlmModelConfig {
        return gson.fromJson(value, LlmModelConfig::class.java)
    }
}

class ChatMessageListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromChatMessageList(value: List<ChatMessage>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toChatMessageList(value: String): List<ChatMessage> {
        val listType = object : TypeToken<List<ChatMessage>>() {}.type
        return gson.fromJson(value, listType)
    }
}