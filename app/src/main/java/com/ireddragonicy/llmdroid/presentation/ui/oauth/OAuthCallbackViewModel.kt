package com.ireddragonicy.llmdroid.presentation.ui.oauth

import android.content.Intent // <<< TAMBAHKAN IMPORT
import android.net.Uri // Mungkin tidak dibutuhkan lagi di sini
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.usecase.auth.HandleOAuthCallbackUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.CheckModelRequirementsUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.GetSelectedModelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OAuthCallbackViewModel @Inject constructor(
    private val handleOAuthCallbackUseCase: HandleOAuthCallbackUseCase,
    private val getSelectedModelUseCase: GetSelectedModelUseCase,
    private val checkModelRequirementsUseCase: CheckModelRequirementsUseCase
) : ViewModel() {

    private val _callbackResult = MutableStateFlow<Result<String>?>(null)
    val callbackResult: StateFlow<Result<String>?> = _callbackResult.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<NavigationEvent> = _navigationEvent.receiveAsFlow()

    private var isProcessing = false

    // Ubah parameter menjadi Intent
    fun handleAuthorizationCallback(callbackIntent: Intent) {
        if (isProcessing) return
        isProcessing = true

        viewModelScope.launch {
            _callbackResult.value = Result.Loading
            // Teruskan Intent ke use case
            val result = handleOAuthCallbackUseCase(callbackIntent)
            _callbackResult.value = result

            if (result is Result.Success) {
                checkRequirementsAndNavigate()
            } else {
                isProcessing = false
            }
        }
    }

    private suspend fun checkRequirementsAndNavigate() {
        val selectedModel = getSelectedModelUseCase().firstOrNull()
        if (selectedModel == null) {
            Log.e("OAuthCallbackVM", "No model selected after successful login.")
            _navigationEvent.send(NavigationEvent.NavigateToMain)
            isProcessing = false
            return
        }

        when (val reqResult = checkModelRequirementsUseCase(selectedModel)) {
            is Result.Success -> {
                val requirements = reqResult.data
                when {
                    requirements.needsLicenseAcknowledgement -> _navigationEvent.send(NavigationEvent.NavigateToLicense)
                    else -> _navigationEvent.send(NavigationEvent.NavigateToLoading)
                }
            }
            is Result.Error -> {
                Log.e("OAuthCallbackVM", "Failed to check model requirements after login", reqResult.exception)
                _callbackResult.value = Result.Error(reqResult.exception)
                _navigationEvent.send(NavigationEvent.NavigateToMain)
            }
            is Result.Loading -> { /* Ignore */ }
        }
        isProcessing = false
    }

    sealed class NavigationEvent {
        object NavigateToLicense : NavigationEvent()
        object NavigateToLoading : NavigationEvent()
        object NavigateToMain : NavigationEvent()
    }
}