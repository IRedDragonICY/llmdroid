package com.ireddragonicy.llmdroid.domain.usecase.auth

import android.content.Intent // <<< TAMBAHKAN IMPORT
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.AuthRepository
import javax.inject.Inject

class HandleOAuthCallbackUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(callbackIntent: Intent): Result<String> {
        // Langsung teruskan Intent ke repository
        return authRepository.handleCallback(callbackIntent)
    }
}