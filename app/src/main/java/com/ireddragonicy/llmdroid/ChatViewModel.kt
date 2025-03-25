package com.ireddragonicy.llmdroid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ireddragonicy.llmdroid.data.ChatMessage
import com.ireddragonicy.llmdroid.data.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class ChatViewModel(
    private var inferenceModel: InferenceModel,
    private val chatRepository: ChatRepository,
    private val chatId: String = ""
) : ViewModel() {
    private val TAG = "ChatViewModel"

    private val _uiState = MutableStateFlow<UiState>(GenericUiState()) // Use GenericUiState here
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    init {
        loadChatMessages()
    }

    private fun loadChatMessages() {
        viewModelScope.launch {
            val session = chatRepository.getChatSession(chatId)
            if (session != null) {
                _uiState.update { currentState -> // Update uiState correctly
                    currentState.clearMessages() // Clear existing messages
                    session.messages.forEach { message ->
                        currentState.addMessage(message.rawMessage, message.author) // Add loaded messages
                    }
                    currentState // Return the updated state
                }
            }
        }
    }

    fun resetInferenceModel(newModel: InferenceModel) {
        inferenceModel = newModel
    }

    fun sendMessage(userMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            _uiState.value.createLoadingMessage()
            setInputEnabled(false)
            try {
                val asyncInference =  inferenceModel.generateResponseAsync(userMessage) { partialResult, done ->
                    _uiState.update { currentState -> // Update uiState within sendMessage
                        currentState.appendMessage(partialResult, done)
                        currentState // Return the updated state
                    }
                    if (done) {
                        setInputEnabled(true)
                    } else {
                        _tokensRemaining.update { max(0, it - 1) }
                    }
                }
                asyncInference.addListener({
                    viewModelScope.launch(Dispatchers.IO) {
                        recomputeSizeInTokens(userMessage)
                    }
                }, Dispatchers.Main.asExecutor())
            } catch (e: Exception) {
                _uiState.update { currentState -> // Update uiState on error
                    currentState.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                    currentState // Return the updated state
                }
                setInputEnabled(true)
            }
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    fun recomputeSizeInTokens(message: String) {
        val remainingTokens = inferenceModel.estimateTokensRemaining(message)
        _tokensRemaining.value = remainingTokens
    }

    companion object {
        fun getFactory(context: Context, chatId: String = "") = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.Companion.getInstance(context)
                val repository = ChatRepository.getInstance(context)
                return ChatViewModel(inferenceModel, repository, chatId) as T
            }
        }
    }
}