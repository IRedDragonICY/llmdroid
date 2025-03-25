package com.ireddragonicy.llmdroid.domain.usecase.model

import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetSelectedModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(): StateFlow<LlmModelConfig?> {
        return modelRepository.selectedModel
    }
}