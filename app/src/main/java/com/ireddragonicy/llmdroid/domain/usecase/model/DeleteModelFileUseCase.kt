package com.ireddragonicy.llmdroid.domain.usecase.model

import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import javax.inject.Inject

class DeleteModelFileUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(model: LlmModelConfig): Boolean {
       return modelRepository.deleteModelFile(model)
    }
}