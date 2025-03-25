package com.ireddragonicy.llmdroid.domain.usecase.chat

import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatSessionUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): Result<ChatSession> {
        return chatRepository.getChatSession(chatId)
    }
}