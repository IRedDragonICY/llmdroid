package com.ireddragonicy.llmdroid

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ireddragonicy.llmdroid.data.SecureStorage
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import java.security.MessageDigest
import java.security.SecureRandom

class LoginActivity : AppCompatActivity() {
  private lateinit var authService: AuthorizationService
  private lateinit var codeVerifier: String
  private lateinit var codeChallenge: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    authService = AuthorizationService(this)

    val loginButton: ImageButton = findViewById(R.id.btnLogin)
    loginButton.setOnClickListener {
      loginWithHuggingFace()
    }
  }

  private fun loginWithHuggingFace() {
    codeVerifier = generateCodeVerifier()
    codeChallenge = generateCodeChallenge(codeVerifier)

    SecureStorage.saveCodeVerifier(applicationContext, codeVerifier)

    val authRequest = AuthorizationRequest.Builder(
      AuthConfig.authServiceConfig,
      AuthConfig.clientId,
      ResponseTypeValues.CODE,
      Uri.parse(AuthConfig.redirectUri)
    ).setScope("read-repos")
      .setCodeVerifier(codeVerifier, codeChallenge, "S256")
      .build()

    val authIntent = authService.getAuthorizationRequestIntent(authRequest)
    startActivity(authIntent)
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
