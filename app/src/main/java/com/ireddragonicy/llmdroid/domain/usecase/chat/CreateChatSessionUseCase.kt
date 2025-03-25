package com.ireddragonicy.llmdroid.domain.usecase.chat

import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import javax.inject.Inject

class CreateChatSessionUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(model: LlmModelConfig): Result<ChatSession> {
        // Bisa tambahkan validasi model di sini jika perlu
        return chatRepository.createChatSession(model)
    }
}