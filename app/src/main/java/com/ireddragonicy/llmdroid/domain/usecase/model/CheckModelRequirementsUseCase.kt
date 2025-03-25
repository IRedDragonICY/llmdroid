package com.ireddragonicy.llmdroid.domain.usecase.model

import android.util.Log // Tambahkan import Log
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.AuthRepository
import javax.inject.Inject

data class ModelRequirements(
    val needsAuth: Boolean = false,
    // Flag ini akan kita set selalu false untuk saat ini
    val needsLicenseAcknowledgement: Boolean = false
)

class CheckModelRequirementsUseCase @Inject constructor(
    private val authRepository: AuthRepository
    // Tambahkan repository/storage lain jika perlu cek status license acknowledgement di masa depan
) {
    suspend operator fun invoke(model: LlmModelConfig): Result<ModelRequirements> {
        Log.d("CheckModelReq", "Checking requirements for: ${model.name}")
        // --- PERUBAHAN UTAMA ---
        // Untuk saat ini, kita anggap tidak ada model yang *mewajibkan* acknowledgment.
        // Jika URL lisensi ada, itu hanya informasi. `needsLicenseAcknowledgement` akan selalu false.
        // Nantinya, jika ada model yang benar-benar WAJIB, kita bisa menambahkan flag
        // `requiresAcknowledgement: Boolean` di LlmModelConfig dan cek statusnya di sini.
        val needsAcknowledgement = false // <-- SET SELALU FALSE UNTUK SEKARANG
        Log.d("CheckModelReq", "Needs Acknowledgement set to: $needsAcknowledgement")


        val initialRequirements = ModelRequirements(
            needsAuth = model.needsAuth, // Ambil dari config model
            needsLicenseAcknowledgement = needsAcknowledgement // Gunakan nilai yang sudah kita tentukan (false)
        )
        Log.d("CheckModelReq", "Initial requirements: Auth=${initialRequirements.needsAuth}, LicenseAck=${initialRequirements.needsLicenseAcknowledgement}")


        // Logika pengecekan Autentikasi (tetap sama)
        // Hanya perlu cek token jika model secara eksplisit menyatakan butuh auth (model.needsAuth == true)
        if (model.needsAuth) {
            Log.d("CheckModelReq", "${model.name} needs auth. Checking token...")
            val tokenResult = authRepository.getToken()
            when (tokenResult) {
                is Result.Success -> {
                    if (tokenResult.data.isNullOrBlank()) {
                        // Butuh auth, tapi token tidak ada/kosong -> needsAuth = true (untuk trigger login)
                        Log.d("CheckModelReq", "Token not found or blank. Setting needsAuth = true")
                        return Result.Success(initialRequirements.copy(needsAuth = true))
                    } else {
                        // Butuh auth, dan token ada -> needsAuth = false (sudah terpenuhi)
                        Log.d("CheckModelReq", "Token found. Setting needsAuth = false")
                        return Result.Success(initialRequirements.copy(needsAuth = false))
                    }
                }
                is Result.Error -> {
                    // Gagal mengambil token (misal error storage), anggap perlu login untuk aman
                    Log.e("CheckModelReq", "Error getting token. Setting needsAuth = true", tokenResult.exception)
                    return Result.Success(initialRequirements.copy(needsAuth = true))
                }
                is Result.Loading -> {
                    // Seharusnya tidak terjadi di sini, tapi jika iya, tunggu atau return error?
                    // Untuk aman, anggap perlu login.
                     Log.w("CheckModelReq", "Token check returned Loading state. Assuming needsAuth = true")
                     return Result.Success(initialRequirements.copy(needsAuth = true))
                }
            }
        } else {
             // Jika model tidak butuh auth (model.needsAuth == false), kembalikan requirements awal
            Log.d("CheckModelReq", "${model.name} does not need auth.")
             return Result.Success(initialRequirements)
        }
    }
}