package com.ireddragonicy.llmdroid.data.remote.auth

import net.openid.appauth.AuthorizationServiceConfiguration
import android.net.Uri

object AuthConfig {
    const val clientId = "b92b40ac-4062-4792-bca7-4a87d5cdae76"
    const val redirectUri = "com.ireddragonicy.llmdroid://oauth2callback"
    const val scope = "read-repos" // Or other required scopes
    const val codeChallengeMethod = "S256"

    private const val authEndpoint = "https://huggingface.co/oauth/authorize"
    private const val tokenEndpoint = "https://huggingface.co/oauth/token"

    val authServiceConfig = AuthorizationServiceConfiguration(
        Uri.parse(authEndpoint),
        Uri.parse(tokenEndpoint)
    )
}