package com.ireddragonicy.llmdroid

import androidx.compose.runtime.toMutableStateList

const val USER_PREFIX = "user"
const val MODEL_PREFIX = "model"

interface UiState {
    val messages: List<ChatMessage>

    fun createLoadingMessage()


    fun appendMessage(text: String, done: Boolean = false)

    fun addMessage(text: String, author: String)

    fun clearMessages()

    fun formatPrompt(text:String) : String
}


class GenericUiState(
    messages: List<ChatMessage> = emptyList()
) : UiState {
    private val _messages: MutableList<ChatMessage> = messages.toMutableStateList()
    override val messages: List<ChatMessage> = _messages.asReversed()
    private var _currentMessageId = ""

    override fun createLoadingMessage() {
        val chatMessage = ChatMessage(author = MODEL_PREFIX, isLoading = true)
        _messages.add(chatMessage)
        _currentMessageId= chatMessage.id
    }
    
    override fun appendMessage(text: String, done: Boolean){
        val index = _messages.indexOfFirst { it.id == _currentMessageId }
        if (index != -1) {
            val newText = _messages[index].rawMessage + text
            _messages[index] = _messages[index].copy(rawMessage = newText, isLoading = false)
        }
    }

    override fun addMessage(text: String, author: String) {
        val chatMessage = ChatMessage(
            rawMessage = text,
            author = author
        )
        _messages.add(chatMessage)
        _currentMessageId = chatMessage.id
    }

    override fun clearMessages() {
        _messages.clear()
    }

    override fun formatPrompt(text: String): String {
        return text
    }
}


class DeepSeekUiState(
    messages: List<ChatMessage> = emptyList()
) : UiState {
    private var START_TOKEN = "<｜begin▁of▁sentence｜>"
    private var PROMPT_PREFIX = "<｜User｜>"
    private var PROMPT_SUFFIX = "<｜Assistant｜>"
    private var THINKING_MARKER_START = "<think>"
    private var THINKING_MARKER_END = "</think>"

    private val _messages: MutableList<ChatMessage> = messages.toMutableStateList()
    override val messages: List<ChatMessage> = _messages.asReversed()
    private var _currentMessageId = ""

    override fun createLoadingMessage() {
        val chatMessage = ChatMessage(author = MODEL_PREFIX, isLoading = true, isThinking = false)
        _messages.add(chatMessage)
        _currentMessageId = chatMessage.id
    }

    override fun appendMessage(text: String, done: Boolean) {
        val index = _messages.indexOfFirst { it.id == _currentMessageId }

        if (text.contains(THINKING_MARKER_START)) {
            _messages[index] = _messages[index].copy(
                isThinking = true
            )
        }

        if (text.contains(THINKING_MARKER_END)) {
            val thinkingEnd = text.indexOf(THINKING_MARKER_END) + THINKING_MARKER_END.length

            val prefix = text.substring(0, thinkingEnd)
            val suffix = text.substring(thinkingEnd)

            appendToMessage(_currentMessageId, prefix)

            if (_messages[index].isEmpty) {
                _messages[index] = _messages[index].copy(
                    isThinking = false
                )
                appendToMessage(_currentMessageId, suffix)
            } else {
                val message = ChatMessage(
                    rawMessage = suffix,
                    author = MODEL_PREFIX,
                    isLoading = true,
                    isThinking = false
                )
                _messages.add(message)
                _currentMessageId = message.id
            }
        } else {
            appendToMessage(_currentMessageId, text)
        }
    }

    private fun appendToMessage(id: String, suffix: String) : Int {
        val index = _messages.indexOfFirst { it.id == id }
        val newText =  suffix.replace(THINKING_MARKER_START, "").replace(THINKING_MARKER_END, "")
        _messages[index] = _messages[index].copy(
            rawMessage = _messages[index].rawMessage + newText,
            isLoading = false
        )
        return index
    }

    override fun addMessage(text: String, author: String) {
        val chatMessage = ChatMessage(
            rawMessage = text,
            author = author
        )
        _messages.add(chatMessage)
        _currentMessageId = chatMessage.id
    }

    override fun clearMessages() {
        _messages.clear()
    }

    override fun formatPrompt(text: String): String {
       return "$START_TOKEN$PROMPT_PREFIX$text$PROMPT_SUFFIX"
    }
}
