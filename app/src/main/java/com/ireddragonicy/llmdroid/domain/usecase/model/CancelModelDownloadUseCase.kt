package com.ireddragonicy.llmdroid.domain.usecase.model

import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import javax.inject.Inject

class CancelModelDownloadUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    operator fun invoke() {
        modelRepository.cancelModelDownload()
    }
}