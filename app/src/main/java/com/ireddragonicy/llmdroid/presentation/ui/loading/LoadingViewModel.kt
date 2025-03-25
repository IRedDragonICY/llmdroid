package com.ireddragonicy.llmdroid.presentation.ui.loading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.usecase.chat.CreateChatSessionUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val checkModelExistsUseCase: CheckModelExistsUseCase,
    private val downloadModelUseCase: DownloadModelUseCase,
    private val cancelModelDownloadUseCase: CancelModelDownloadUseCase,
    private val deleteModelFileUseCase: DeleteModelFileUseCase,
    private val loadOrRefreshLlmSessionUseCase: LoadOrRefreshLlmSessionUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoadingUiState())
    val uiState: StateFlow<LoadingUiState> = _uiState.asStateFlow()

    // Channel for signaling completion and navigating
    private val _completionEvent = Channel<String>(Channel.BUFFERED)
    val completionEvent = _completionEvent.receiveAsFlow()

    private var currentModel: LlmModelConfig? = null
    private var initializationJob: Job? = null

    fun initialize(model: LlmModelConfig) {
        if (initializationJob?.isActive == true && currentModel == model) {
            // Already initializing this model
            return
        }
        currentModel = model
        initializationJob?.cancel() // Cancel previous job if any

        initializationJob = viewModelScope.launch {
            _uiState.value = LoadingUiState(isLoadingModel = true, modelName = model.name) // Reset state

            // 1. Check if model file exists
            val exists = checkModelExistsUseCase(model)

            if (!exists) {
                // 2. Download if it doesn't exist
                Log.d("LoadingViewModel", "Model ${model.name} not found. Starting download.")
                downloadModel(model)
            } else {
                // 3. Load LLM session if file exists
                Log.d("LoadingViewModel", "Model ${model.name} found. Loading session.")
                loadLlmSession(model)
            }
        }
    }

    private suspend fun downloadModel(model: LlmModelConfig) {
        downloadModelUseCase(model).collect { result ->
            when (result) {
                is Result.Success -> {
                    val progressData = result.data
                    _uiState.update {
                        it.copy(
                            isLoadingModel = false,
                            isDownloading = !progressData.isComplete,
                            downloadProgress = progressData.percentage,
                            error = null
                        )
                    }
                    if (progressData.isComplete) {
                        Log.d("LoadingViewModel", "Download complete for ${model.name}. Loading session.")
                        loadLlmSession(model) // Proceed to load LLM session
                    }
                }
                is Result.Error -> {
                    Log.e("LoadingViewModel", "Download failed for ${model.name}", result.exception)
                    _uiState.update {
                        it.copy(
                            isLoadingModel = false,
                            isDownloading = false,
                            error = "Download failed: ${result.exception.message}"
                        )
                    }
                    // Clean up potentially corrupt downloaded file
                    deleteModelFileUseCase(model)
                }
                is Result.Loading -> {
                    // Optionally handle initial loading state from flow if needed
                    _uiState.update { it.copy(isLoadingModel = false, isDownloading = true, downloadProgress = 0, error = null) }
                }
            }
        }
    }

    private suspend fun loadLlmSession(model: LlmModelConfig) {
        _uiState.update { it.copy(isLoadingModel = true, isDownloading = false, error = null) }
        when (val result = loadOrRefreshLlmSessionUseCase()) {
            is Result.Success -> {
                Log.d("LoadingViewModel", "LLM Session loaded successfully for ${model.name}.")
                // Session loaded, now create the chat session
                createChatAndComplete(model)
            }
            is Result.Error -> {
                 Log.e("LoadingViewModel", "LLM session load failed for ${model.name}", result.exception)
                _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        isDownloading = false,
                        error = "Failed to load model: ${result.exception.message}"
                    )
                }
                 // Optionally delete the file if loading fails consistently?
                 // deleteModelFileUseCase(model)
            }
            is Result.Loading -> { /* isLoadingModel is already true */ }
        }
    }

     private suspend fun createChatAndComplete(model: LlmModelConfig) {
         when(val chatResult = createChatSessionUseCase(model)) {
             is Result.Success -> {
                 Log.d("LoadingViewModel", "Chat session created: ${chatResult.data.id}")
                 _completionEvent.send(chatResult.data.id) // Send event to navigate
                 // State can remain loading until navigation happens
             }
             is Result.Error -> {
                  Log.e("LoadingViewModel", "Failed to create chat session after loading model", chatResult.exception)
                 _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        isDownloading = false,
                        error = "Failed to create chat session: ${chatResult.exception.message}"
                    )
                }
             }
             is Result.Loading -> {}
         }
     }

    fun cancelDownload() {
        Log.d("LoadingViewModel", "Download cancellation requested.")
        cancelModelDownloadUseCase()
        initializationJob?.cancel() // Cancel the collection job as well
        _uiState.update { it.copy(isDownloading = false, isLoadingModel = false, error = "Download cancelled.") }
        // Go back or allow retry? Depends on UX. Let onBack handle navigation.
    }

    override fun onCleared() {
        super.onCleared()
        initializationJob?.cancel()
        // Cancel download just in case viewmodel is cleared during download
        if (_uiState.value.isDownloading) {
             cancelModelDownloadUseCase()
        }
    }
}