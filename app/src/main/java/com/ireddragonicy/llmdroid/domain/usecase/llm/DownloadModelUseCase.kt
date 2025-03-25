package com.ireddragonicy.llmdroid.domain.usecase.model

import com.ireddragonicy.llmdroid.data.model.DownloadProgress
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(model: LlmModelConfig): Flow<Result<DownloadProgress>> {
        return modelRepository.downloadModel(model)
    }
}