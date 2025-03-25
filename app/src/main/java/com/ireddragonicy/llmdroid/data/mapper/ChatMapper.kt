package com.ireddragonicy.llmdroid.data.mapper

import com.ireddragonicy.llmdroid.data.local.db.entity.ChatSessionEntity
import com.ireddragonicy.llmdroid.domain.model.ChatSession


fun ChatSessionEntity.toDomainModel(): ChatSession {
    return ChatSession(
        id = this.id,
        title = this.title,
        createdAt = this.createdAt,
        messages = this.messages,
        modelType = this.modelType
    )
}

fun ChatSession.toDataEntity(): ChatSessionEntity {
    return ChatSessionEntity(
        id = this.id,
        title = this.title,
        createdAt = this.createdAt,
        messages = this.messages,
        modelType = this.modelType
    )
}

