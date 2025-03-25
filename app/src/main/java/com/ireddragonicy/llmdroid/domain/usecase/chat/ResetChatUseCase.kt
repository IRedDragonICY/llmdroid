package com.ireddragonicy.llmdroid.domain.usecase.chat

import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import javax.inject.Inject

class ResetChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository // Butuh ini untuk reset LLM session
) {
    suspend operator fun invoke(chatId: String): Result<Unit> {
        // 1. Reset LLM session state
        val llmResetResult = modelRepository.resetSession()
        if (llmResetResult is Result.Error) {
            return Result.Error(Exception("Failed to reset LLM session during chat reset", llmResetResult.exception))
        }

        // 2. Hapus pesan dari database (atau update dengan list kosong)
        val dbUpdateResult = chatRepository.updateChatMessages(chatId, emptyList())
        // Atau jika ingin menghapus sesi: chatRepository.deleteChatSession(chatId) tapi itu beda use case

        return if (dbUpdateResult is Result.Success) {
            Result.Success(Unit)
        } else {
            Result.Error(Exception("Failed to clear messages in database during chat reset", (dbUpdateResult as? Result.Error)?.exception))
        }
    }
}