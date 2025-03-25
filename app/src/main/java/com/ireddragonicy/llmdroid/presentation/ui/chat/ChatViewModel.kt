package com.ireddragonicy.llmdroid.presentation.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.llmdroid.domain.model.ChatMessage
import com.ireddragonicy.llmdroid.domain.model.LlmModelConfig
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.usecase.chat.GetChatSessionUseCase
import com.ireddragonicy.llmdroid.domain.usecase.chat.ResetChatUseCase
import com.ireddragonicy.llmdroid.domain.usecase.chat.UpdateChatMessagesUseCase
import com.ireddragonicy.llmdroid.domain.usecase.llm.EstimateTokensUseCase
import com.ireddragonicy.llmdroid.domain.usecase.llm.SendMessageUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.GetSelectedModelUseCase
import com.ireddragonicy.llmdroid.domain.usecase.model.LoadOrRefreshLlmSessionUseCase
import com.ireddragonicy.llmdroid.presentation.navigation.AppDestinationsArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val USER_PREFIX = "user"
const val MODEL_PREFIX = "model"
const val THINKING_LABEL = "thinking"

data class ChatScreenUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentModel: LlmModelConfig? = null,
    val isTextInputEnabled: Boolean = true,
    val tokensRemaining: Int = -1, // -1 means unknown/not calculated
    val sessionTitle: String = "Chat"
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getChatSessionUseCase: GetChatSessionUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val estimateTokensUseCase: EstimateTokensUseCase,
    private val updateChatMessagesUseCase: UpdateChatMessagesUseCase,
    private val resetChatUseCase: ResetChatUseCase,
    private val getSelectedModelUseCase: GetSelectedModelUseCase, // Inject this
    private val loadOrRefreshLlmSessionUseCase: LoadOrRefreshLlmSessionUseCase // Inject this
) : ViewModel() {

    private val chatId: String = savedStateHandle[AppDestinationsArgs.CHAT_ID_ARG]!!

    private val _uiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> = _uiState.asStateFlow()

    // Debounce job for token estimation
    private var tokenEstimationJob: Job? = null
    private var sendMessageJob: Job? = null


    // Store the current "thinking" message ID if applicable
    private var currentThinkingMessageId: String? = null
     private var currentModelResponseBuffer = ""


    init {
        observeSelectedModel()
        loadChatSession()
        ensureLlmSessionReady() // Try to load session on init
    }

     private fun observeSelectedModel() {
        viewModelScope.launch {
            getSelectedModelUseCase().collect { model ->
                _uiState.update { it.copy(currentModel = model) }
                 // If model changes, ensure session is reloaded
                 if (model != null) {
                      ensureLlmSessionReady()
                      recomputeTokens("") // Recompute tokens for new model context
                 }
            }
        }
    }

     private fun ensureLlmSessionReady() {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loadOrRefreshLlmSessionUseCase()) {
                is Result.Success -> {
                     _uiState.update { it.copy(isLoading = false) }
                     Log.d("ChatViewModel", "LLM Session ready.")
                     recomputeTokens("") // Recompute tokens now that session is loaded
                }
                is Result.Error -> {
                     _uiState.update { it.copy(isLoading = false, error = "Failed to load model: ${result.exception.message}") }
                     Log.e("ChatViewModel", "LLM Session load failed", result.exception)
                }
                else -> {} // Handle Loading state if used
            }
        }
     }

    private fun loadChatSession() {
        viewModelScope.launch {
            when (val result = getChatSessionUseCase(chatId)) {
                is Result.Success -> {
                    val session = result.data
                    _uiState.update {
                        it.copy(
                            messages = session.messages,
                            sessionTitle = session.title,
                            // Maybe set model from session if different?
                            // currentModel = session.modelType
                            isLoading = false
                        )
                    }
                     // Recompute tokens after loading history
                     recomputeTokens("")
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load chat: ${result.exception.message}")
                    }
                }
                 is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank() || !_uiState.value.isTextInputEnabled) return
         sendMessageJob?.cancel() // Cancel previous if still running (unlikely but safe)

        val newUserMessage = ChatMessage(author = USER_PREFIX, rawMessage = userMessage)
        val loadingMessage = ChatMessage(author = MODEL_PREFIX, isLoading = true)

        val messagesWithUser = _uiState.value.messages + newUserMessage
        val messagesWithLoading = messagesWithUser + loadingMessage

        // Optimistically update UI
        _uiState.update {
            it.copy(
                messages = messagesWithLoading,
                isTextInputEnabled = false,
                tokensRemaining = -1 // Invalidate token count during generation
            )
        }

        // Persist user message immediately
        saveMessages(messagesWithUser)


        sendMessageJob = viewModelScope.launch {
            val history = messagesWithUser // Pass history *before* model's response
             var finalModelMessage = ""

            sendMessageUseCase(userMessage, history).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val (partialResult, isDone) = result.data
                         finalModelMessage += partialResult // Append results

                         // Update the loading message with the partial/final result
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            val loadingIndex = updatedMessages.indexOfFirst { it.id == loadingMessage.id }
                            if (loadingIndex != -1) {
                                updatedMessages[loadingIndex] = loadingMessage.copy(
                                     rawMessage = finalModelMessage, // Use accumulated message
                                     isLoading = !isDone // Stop loading only when done
                                )
                            }
                            currentState.copy(messages = updatedMessages, isTextInputEnabled = isDone)
                        }

                        if (isDone) {
                            // Persist final model message
                             val finalMessages = messagesWithUser + loadingMessage.copy(rawMessage = finalModelMessage, isLoading = false)
                             saveMessages(finalMessages)
                             recomputeTokens("") // Recompute tokens after response
                        }
                    }
                    is Result.Error -> {
                         val errorMessage = ChatMessage(author = MODEL_PREFIX, rawMessage = "Error: ${result.exception.message}")
                         _uiState.update { currentState ->
                            // Replace loading message with error message
                             val updatedMessages = currentState.messages.toMutableList()
                             val loadingIndex = updatedMessages.indexOfFirst { it.id == loadingMessage.id }
                             if (loadingIndex != -1) {
                                 updatedMessages[loadingIndex] = errorMessage
                             } else {
                                 // Should not happen if optimistic update worked
                                 updatedMessages.add(errorMessage)
                             }
                             currentState.copy(messages = updatedMessages, isTextInputEnabled = true, error = result.exception.message)
                         }
                         // Persist messages including the error
                         val finalMessages = messagesWithUser + errorMessage
                         saveMessages(finalMessages)
                         recomputeTokens("") // Still recompute tokens
                    }
                    is Result.Loading -> { /* Handled by initial isLoading=true state */ }
                }
            }
        }
    }

    // Saves messages to the repository
     private fun saveMessages(messages: List<ChatMessage>) {
         viewModelScope.launch {
            updateChatMessagesUseCase(chatId, messages) // Fire and forget for now
         }
     }

    fun onUserMessageChanged(message: String) {
        tokenEstimationJob?.cancel()
        tokenEstimationJob = viewModelScope.launch {
            delay(300) // Debounce
            recomputeTokens(message)
        }
    }

     fun refreshTokens() {
         tokenEstimationJob?.cancel()
          tokenEstimationJob = viewModelScope.launch {
             recomputeTokens("") // Pass empty string or current input field value
         }
     }

    // Internal function to call the use case
    private suspend fun recomputeTokens(currentUserInput: String) {
        if (_uiState.value.currentModel == null) {
             _uiState.update { it.copy(tokensRemaining = -1) } // Can't compute without model
             return
        }

        when (val result = estimateTokensUseCase(currentUserInput, _uiState.value.messages)) {
            is Result.Success -> _uiState.update { it.copy(tokensRemaining = result.data) }
            is Result.Error -> {
                Log.e("ChatViewModel", "Token estimation failed", result.exception)
                _uiState.update { it.copy(tokensRemaining = 0) } // Indicate error state? Or -1?
            }
            is Result.Loading -> { /* Usually fast, maybe ignore */ }
        }
    }


    fun clearChatHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(messages = emptyList(), isLoading = true) } // Optimistic clear
             when (val result = resetChatUseCase(chatId)) { // UseCase handles DB and LLM session reset
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, tokensRemaining = -1) } // Reset tokens
                    recomputeTokens("") // Recompute for empty history
                     Log.d("ChatViewModel", "Chat reset successful.")
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to reset chat: ${result.exception.message}") }
                     // Might need to reload original messages if DB clear failed but LLM reset worked? Complex.
                     loadChatSession() // Reload to be safe
                     Log.e("ChatViewModel", "Chat reset failed", result.exception)
                }
                 is Result.Loading -> {} // isLoading already true
            }
        }
    }
}