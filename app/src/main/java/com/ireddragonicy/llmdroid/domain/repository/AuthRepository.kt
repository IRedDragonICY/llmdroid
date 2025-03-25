package com.ireddragonicy.llmdroid.domain.repository

import android.content.Intent
import com.ireddragonicy.llmdroid.domain.model.Result

interface AuthRepository {
    /**
     * Memulai flow otentikasi (misalnya, membuka browser/Custom Tab).
     * Mengembalikan Intent untuk dimulai oleh Activity.
     */
    suspend fun initiateLogin(): Result<Intent>

    /**
     * Menangani callback setelah otentikasi eksternal.
     * Bertukar authorization code (jika ada) dengan access token.
     * @param responseUri URI callback yang diterima.
     * @param error String error jika otentikasi gagal di provider.
     * @return Result yang berisi access token jika berhasil.
     */
    suspend fun handleCallback(callbackIntent: Intent): Result<String>

    /**
     * Mendapatkan access token yang tersimpan.
     * @return Result berisi token (String) atau null jika tidak ada.
     */
    suspend fun getToken(): Result<String?>

    /**
     * Menghapus access token yang tersimpan.
     */
    suspend fun removeToken(): Result<Unit>

    // Opsional: Cek status otentikasi (misal cek validitas token jika ada endpoint)
    // suspend fun checkAuthStatus(): Result<Boolean>
}