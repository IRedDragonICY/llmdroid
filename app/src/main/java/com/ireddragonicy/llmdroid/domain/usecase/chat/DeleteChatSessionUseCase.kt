package com.ireddragonicy.llmdroid.domain.usecase.chat

import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteChatSessionUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): Result<Unit> {
        return chatRepository.deleteChatSession(chatId)
    }
}