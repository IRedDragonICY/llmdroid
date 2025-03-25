package com.ireddragonicy.llmdroid.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // Non-encrypted prefs for code verifier as it's less sensitive and needed before decryption keys might be fully ready
    private val plainPrefs = context.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)


    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun removeToken() {
        sharedPreferences.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    fun saveCodeVerifier(codeVerifier: String) {
        plainPrefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply()
    }

    fun getCodeVerifier(): String? {
       return plainPrefs.getString(KEY_CODE_VERIFIER, null)
    }

     fun removeCodeVerifier() {
        plainPrefs.edit().remove(KEY_CODE_VERIFIER).apply()
    }


    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val PLAIN_PREFS_NAME = "llmdroid_plain_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_CODE_VERIFIER = "code_verifier"
    }
}