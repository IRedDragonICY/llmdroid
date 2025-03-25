package com.ireddragonicy.llmdroid.presentation.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.usecase.auth.InitiateLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val initiateLoginUseCase: InitiateLoginUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Channel to send the Intent or error back to the Activity
    private val _loginEvent = Channel<Result<Intent>>(Channel.BUFFERED)
    val loginEvent: Flow<Result<Intent>> = _loginEvent.receiveAsFlow()

    fun initiateLogin() {
        if (_isLoading.value) return // Prevent multiple clicks

        viewModelScope.launch {
            _isLoading.value = true
            _loginEvent.send(Result.Loading) // Send loading state

            val result = initiateLoginUseCase()
            _loginEvent.send(result) // Send success (with Intent) or error

            _isLoading.value = false
        }
    }
}