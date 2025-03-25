package com.ireddragonicy.llmdroid.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.ireddragonicy.llmdroid.data.local.SecureStorage
import com.ireddragonicy.llmdroid.data.remote.auth.AuthConfig
import com.ireddragonicy.llmdroid.di.IoDispatcher
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
// Hapus import AuthorizationResponse karena kita tidak menggunakannya untuk parsing sukses lagi
// import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: AuthorizationService,
    private val secureStorage: SecureStorage,
    private val authConfig: AuthConfig,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"

    override suspend fun initiateLogin(): Result<Intent> = withContext(ioDispatcher) {
        try {
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            secureStorage.saveCodeVerifier(codeVerifier)

            val authRequest = AuthorizationRequest.Builder(
                authConfig.authServiceConfig,
                authConfig.clientId,
                ResponseTypeValues.CODE,
                Uri.parse(authConfig.redirectUri)
            ).setScope(authConfig.scope)
             .setCodeVerifier(codeVerifier, codeChallenge, authConfig.codeChallengeMethod)
             .build()

            val authIntent = authService.getAuthorizationRequestIntent(authRequest)
            Result.Success(authIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate login", e)
            Result.Error(AuthException("Login initiation failed: ${e.message}", e))
        }
    }

    override suspend fun handleCallback(callbackIntent: Intent): Result<String> = withContext(ioDispatcher) {
        // 1. Cek Error terlebih dahulu menggunakan AuthorizationException
        val authException = AuthorizationException.fromIntent(callbackIntent)
        if (authException != null) {
            Log.e(TAG, "Authorization exception: ${authException.errorDescription}", authException)
            return@withContext Result.Error(
                AuthException(
                    authException.errorDescription ?: authException.error ?: "Authorization error",
                    authException
                )
            )
        }

        // 2. Jika tidak ada error, ambil URI dari data Intent
        val responseUri: Uri? = callbackIntent.data
        if (responseUri == null) {
            Log.e(TAG, "Callback intent data URI is null and no exception found.")
            return@withContext Result.Error(AuthException("Invalid authorization callback: Missing data URI"))
        }

        // 3. Parse 'code' secara manual dari URI
        val authCode: String? = responseUri.getQueryParameter("code")
        // val state: String? = responseUri.getQueryParameter("state") // Ambil state jika perlu validasi manual

        if (authCode.isNullOrBlank()) {
            Log.e(TAG, "Authorization 'code' not found in callback URI: $responseUri")
            return@withContext Result.Error(AuthException("Authorization code not found in callback"))
        }

        // (Opsional: Validasi 'state' jika diperlukan di sini, meskipun AppAuth biasanya menanganinya implisit)

        // 4. Ambil Code Verifier
        val codeVerifier = secureStorage.getCodeVerifier()
        if (codeVerifier == null) {
            Log.e(TAG, "Code verifier not found in storage. Possible session timeout or configuration error.")
            return@withContext Result.Error(AuthException("Login session expired or invalid, please try again"))
        }
        secureStorage.removeCodeVerifier() // Hapus setelah diambil

        // 5. Buat TokenRequest menggunakan code yang diparse manual
        val tokenRequest = TokenRequest.Builder(
            authConfig.authServiceConfig,
            authConfig.clientId
        )
            .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
            .setAuthorizationCode(authCode) // <-- Gunakan authCode yang diparse manual
            .setRedirectUri(Uri.parse(authConfig.redirectUri))
            .setCodeVerifier(codeVerifier)
            .build()

        // 6. Lakukan pertukaran token
        try {
            val tokenResponse = performTokenRequestSuspend(tokenRequest)
            val accessToken = tokenResponse.accessToken
            if (accessToken != null) {
                secureStorage.saveToken(accessToken)
                Log.d(TAG, "Access token obtained and saved successfully.")
                Result.Success(accessToken)
            } else {
                Log.e(TAG, "Access token is null in the response")
                Result.Error(AuthException("Failed to obtain access token from provider"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange failed", e)
            Result.Error(AuthException("Token exchange failed: ${e.message}", e))
        }
    }

    private suspend fun performTokenRequestSuspend(request: TokenRequest): TokenResponse {
         return suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(request) { response, ex ->
                if (continuation.isActive) {
                    if (response != null) {
                        continuation.resume(response)
                    } else {
                        val exceptionToSend = ex?.let { AuthException("Token request failed", it) }
                                              ?: AuthException("Unknown token exchange error")
                        continuation.resumeWithException(exceptionToSend)
                    }
                }
            }
        }
    }

    override suspend fun getToken(): Result<String?> = withContext(ioDispatcher) {
        try {
            Result.Success(secureStorage.getToken())
        } catch (e: Exception) {
             Log.e(TAG, "Failed to get token", e)
             Result.Error(AuthException("Failed to retrieve token: ${e.message}", e))
        }
    }

    override suspend fun removeToken(): Result<Unit> = withContext(ioDispatcher) {
         try {
            secureStorage.removeToken()
            Result.Success(Unit)
        } catch (e: Exception) {
             Log.e(TAG, "Failed to remove token", e)
             Result.Error(AuthException("Failed to clear token: ${e.message}", e))
        }
    }

    private fun generateCodeVerifier(): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.encodeToString(random, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
         val bytes = codeVerifier.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)