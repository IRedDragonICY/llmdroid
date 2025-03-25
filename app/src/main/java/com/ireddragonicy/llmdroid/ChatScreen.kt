// filepath: d:\Project\llmdroid\app\src\main\java\com\ireddragonicy\llmdroid\ChatScreen.kt
package com.ireddragonicy.llmdroid

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ChatRoute(
    chatId: String = ""
) {
    val context = LocalContext.current.applicationContext
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Companion.getFactory(context, chatId),
        key = chatId
    )

    LaunchedEffect(Unit) {
        val inferenceModel = InferenceModel.Companion.getInstance(context)
        chatViewModel.resetInferenceModel(inferenceModel)
    }

    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()
    ChatScreen(
        context,
        uiState,
        textInputEnabled,
        remainingTokens = chatViewModel.tokensRemaining,
        resetTokenCount = {
            chatViewModel.recomputeSizeInTokens("")
        },
        onSendMessage = { message ->
            chatViewModel.sendMessage(message)
        },
        onChangedMessage = { message ->
            chatViewModel.recomputeSizeInTokens(message)
        },
    )
}

@Composable
fun ChatScreen(
    context: Context,
    uiState: UiState,
    textInputEnabled: Boolean,
    remainingTokens: StateFlow<Int>,
    resetTokenCount: () -> Unit,
    onSendMessage: (String) -> Unit,
    onChangedMessage: (String) -> Unit,
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    val tokens by remainingTokens.collectAsState(initial = -1)
    val coroutineScope = rememberCoroutineScope()
    var debounceJob: Job? = remember { null }
    
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = InferenceModel.Companion.model.toString(),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (tokens >= 0) "$tokens ${stringResource(R.string.tokens_remaining)}" else "",
                style = MaterialTheme.typography.titleSmall
            )
            Row {
                IconButton(
                    onClick = {
                        InferenceModel.Companion.getInstance(context).resetSession()
                        uiState.clearMessages()
                        resetTokenCount()
                    },
                    enabled = textInputEnabled
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear Chat")
                }
            }
        }

        if (tokens == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.context_full_message),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(uiState.messages) { chat ->
                ChatItem(chat)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column { }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = userMessage,
                onValueChange = { newMessage ->
                    userMessage = newMessage
                    
                    // Debounce token estimation to avoid calling it too frequently
                    debounceJob?.cancel()
                    debounceJob = coroutineScope.launch {
                        delay(300) // Wait 300ms before estimating tokens
                        onChangedMessage(newMessage)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = {
                    Text(stringResource(R.string.chat_label))
                },
                modifier = Modifier
                    .weight(0.85f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onChangedMessage(userMessage)
                        }
                    },
                enabled = textInputEnabled
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f),
                enabled = textInputEnabled && tokens != 0 // Changed to only disable when tokens are explicitly 0
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.action_send),
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {
    val backgroundColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (chatMessage.isThinking) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val bubbleShape = if (chatMessage.isFromUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (chatMessage.isFromUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        val author = if (chatMessage.isFromUser) {
            stringResource(R.string.user_label)
        } else if (chatMessage.isThinking) {
            stringResource(R.string.thinking_label)
        } else {
            stringResource(R.string.model_label)
        }
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row {
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.9f)
                ) {
                    if (chatMessage.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Text(
                            text = chatMessage.message,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
