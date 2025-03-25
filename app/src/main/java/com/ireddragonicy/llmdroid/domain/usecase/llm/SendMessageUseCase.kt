package com.ireddragonicy.llmdroid.domain.usecase.llm

import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(prompt: String, currentMessages: List<ChatMessage>): Flow<Result<Pair<String, Boolean>>> {
        // Validasi dasar bisa ditambahkan di sini
        if (prompt.isBlank()) {
            // Kembalikan error flow atau handle di ViewModel
        }
        return modelRepository.generateResponse(prompt, currentMessages)
    }
}