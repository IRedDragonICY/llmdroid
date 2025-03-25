package com.ireddragonicy.llmdroid.domain.usecase.model

import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import javax.inject.Inject

class LoadOrRefreshLlmSessionUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return modelRepository.loadOrRefreshSession()
    }
}