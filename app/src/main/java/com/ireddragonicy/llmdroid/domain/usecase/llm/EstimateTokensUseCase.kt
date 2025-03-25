package com.ireddragonicy.llmdroid.domain.usecase.llm

import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import javax.inject.Inject

class EstimateTokensUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(prompt: String, currentMessages: List<ChatMessage>): Result<Int> {
        return modelRepository.estimateTokensRemaining(prompt, currentMessages)
    }
}