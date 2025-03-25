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
    private val chatId: String = ""
) : ViewModel() {
    private val chatRepository = ChatRepository.getInstance()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(object : UiState {
        override val messages: List<ChatMessage>
            get() = _messages.value.asReversed()

        override fun createLoadingMessage() {
            val chatMessage = ChatMessage(author = MODEL_PREFIX, isLoading = true)
            _messages.update { it + chatMessage }
            _currentMessageId = chatMessage.id
            chatRepository.updateChatMessages(chatId, _messages.value)
        }

        override fun appendMessage(text: String, done: Boolean) {
            val index = _messages.value.indexOfFirst { it.id == _currentMessageId }
            if (index != -1) {
                _messages.update { messages ->
                    messages.toMutableList().apply {
                        val currentMsg = this[index]
                        val newText = currentMsg.rawMessage + text
                        this[index] = currentMsg.copy(rawMessage = newText, isLoading = false)
                    }
                }
                chatRepository.updateChatMessages(chatId, _messages.value)
            }
        }

        override fun addMessage(text: String, author: String) {
            val chatMessage = ChatMessage(
                rawMessage = text,
                author = author
            )
            _messages.update { it + chatMessage }
            _currentMessageId = chatMessage.id
            chatRepository.updateChatMessages(chatId, _messages.value)
        }

        override fun clearMessages() {
            _messages.value = emptyList()
            chatRepository.updateChatMessages(chatId, emptyList())
        }

        override fun formatPrompt(text: String): String {
            return inferenceModel.uiState.formatPrompt(text)
        }

        private var _currentMessageId = ""
    })
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    init {
        loadChatMessages()
    }

    private fun loadChatMessages() {
        val session = chatRepository.getChatSession(chatId)
        if (session != null) {
            _messages.value = session.messages
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
                    _uiState.value.appendMessage(partialResult, done)
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
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
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
                return ChatViewModel(inferenceModel, chatId) as T
            }
        }
    }
}
