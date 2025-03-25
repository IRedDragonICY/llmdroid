package com.ireddragonicy.llmdroid.domain.usecase.chat

import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import javax.inject.Inject

class UpdateChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, messages: List<ChatMessage>): Result<Unit> {
        return chatRepository.updateChatMessages(chatId, messages)
    }
}