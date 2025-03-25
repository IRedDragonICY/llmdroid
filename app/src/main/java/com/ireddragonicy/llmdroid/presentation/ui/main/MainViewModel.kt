package com.ireddragonicy.llmdroid.presentation.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.usecase.chat.CreateChatSessionUseCase
import com.ireddragonicy.llmdroid.domain.usecase.chat.DeleteChatSessionUseCase
import com.ireddragonicy.llmdroid.domain.usecase.chat.GetChatSessionsUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.CheckModelRequirementsUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.GetSelectedModelUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.SelectModelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val chatSessions: List<ChatSession> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getChatSessionsUseCase: GetChatSessionsUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val deleteChatSessionUseCase: DeleteChatSessionUseCase,
    private val selectModelUseCase: SelectModelUseCase,
    getSelectedModelUseCase: GetSelectedModelUseCase, // Ubah nama variabel lokal
    private val checkModelRequirementsUseCase: CheckModelRequirementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Gunakan nama variabel lokal yang diinject
    val selectedModel: StateFlow<LlmModelConfig?> = getSelectedModelUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadChatSessions()
    }

    private fun loadChatSessions() {
        viewModelScope.launch {
            getChatSessionsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                chatSessions = result.data.sortedByDescending { s -> s.lastMessageTimestamp },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = "Failed to load chats: ${result.exception.message}")
                        }
                        Log.e("MainViewModel", "Error loading chat sessions", result.exception)
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun selectModel(
        model: LlmModelConfig,
        // Ubah nama parameter callback agar lebih jelas
        onRequirementsChecked: (requiresAuth: Boolean, requiresLicense: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            selectModelUseCase(model) // Update selected model state

            when (val reqResult = checkModelRequirementsUseCase(model)) {
                is Result.Success -> {
                    val requirements = reqResult.data
                    // Panggil callback dengan nilai yang benar
                    onRequirementsChecked(requirements.needsAuth, requirements.needsLicenseAcknowledgement)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = "Could not check model requirements: ${reqResult.exception.message}") }
                    // Jika error, anggap tidak ada requirement khusus?
                    onRequirementsChecked(false, false)
                }
                 is Result.Loading -> { /* Optional: Show loading */ }
            }
        }
    }

    // Fungsi ini mungkin tidak diperlukan atau perlu diimplementasikan berbeda
    // Menyetel model ke null bisa bermasalah. Lebih baik handle navigasi kembali ke WelcomeScreen.
    fun clearSelectedModel() {
        viewModelScope.launch {
             Log.d("MainViewModel", "clearSelectedModel called. Intended behavior might need review.")
             // Hindari memilih null:
             // selectModelUseCase(null)
             // Mungkin emit event untuk navigasi kembali ke welcome?
             // _navigationEvent.emit(NavigationEvent.NavigateToWelcome) // Jika ada event seperti ini
        }
    }


    fun createNewChat(onSuccess: (newChatId: String) -> Unit) {
        viewModelScope.launch {
            val currentModel = selectedModel.value
            if (currentModel == null) {
                 _uiState.update { it.copy(error = "Please select a model first.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            when(val result = createChatSessionUseCase(currentModel)) {
                is Result.Success -> {
                    onSuccess(result.data.id)
                }
                is Result.Error -> {
                     _uiState.update { it.copy(isLoading = false, error = "Failed to create new chat: ${result.exception.message}") }
                }
                 is Result.Loading -> { /* isLoading is true */ }
            }
        }
    }

    fun deleteChatSession(chatId: String) {
        viewModelScope.launch {
            val deleteResult = deleteChatSessionUseCase(chatId)
            when(deleteResult) {
                 is Result.Success -> {
                     Log.d("MainViewModel", "Deleted session $chatId")
                     // Session list akan update otomatis via flow
                 }
                 is Result.Error -> {
                      _uiState.update { it.copy(error = "Failed to delete chat: ${deleteResult.exception.message}") }
                 }
                 is Result.Loading -> { /* Optional */ }
            }
        }
    }

    fun triggerLoginNavigation() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToLogin)
        }
    }

    fun triggerLicenseNavigation() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToLicense)
        }
    }

    sealed class NavigationEvent {
        object NavigateToLogin : NavigationEvent()
        object NavigateToLicense : NavigationEvent()
        data class NavigateToChat(val chatId: String) : NavigationEvent()
        object NavigateToLoading: NavigationEvent() // Mungkin tidak perlu lagi dari sini
        // object NavigateToWelcome: NavigationEvent() // Contoh jika clearSelectedModel diubah
    }
}