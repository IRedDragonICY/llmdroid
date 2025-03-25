package com.ireddragonicy.llmdroid.domain.usecase.auth

import android.content.Intent
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.AuthRepository
import javax.inject.Inject

class InitiateLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Intent> {
        return authRepository.initiateLogin()
    }
}