package com.ireddragonicy.llmdroid.presentation.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.llmdroid.R
import kotlinx.coroutines.launch

@Composable
internal fun ChatRoute(
    chatId: String, // Ensure chatId is passed correctly via navigation
    viewModel: ChatViewModel = hiltViewModel() // Hilt provides ViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        uiState = uiState,
        onSendMessage = viewModel::sendMessage,
        onRefreshChat = viewModel::clearChatHistory,
        onMessageInputChange = viewModel::onUserMessageChanged,
        onRefreshTokens = viewModel::refreshTokens
    )
}

@Composable
fun ChatScreen(
    uiState: ChatScreenUiState,
    onSendMessage: (String) -> Unit,
    onRefreshChat: () -> Unit,
    onMessageInputChange: (String) -> Unit,
    onRefreshTokens: () -> Unit
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
             coroutineScope.launch {
                listState.animateScrollToItem(0) // 0 because reverseLayout=true
            }
        }
    }

    // Refresh tokens when screen gains focus or model changes
     LaunchedEffect(uiState.currentModel) {
         onRefreshTokens()
     }


    Column(
        modifier = Modifier.fillMaxSize(),
        // Arrangement.Bottom is handled by LazyColumn reverseLayout + Row below it
    ) {
        // Top Info Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.currentModel?.name ?: "Loading Model...",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Text(
                    text = when {
                        uiState.tokensRemaining < 0 -> "..." // Loading/Unknown
                        else -> "${uiState.tokensRemaining} ${stringResource(R.string.tokens_remaining)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                     modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(
                    onClick = onRefreshChat,
                    enabled = uiState.isTextInputEnabled // Enable only when not generating
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.clear_chat_description)
                    )
                }
            }
        }

        // Context Full Warning
        if (uiState.tokensRemaining == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.context_full_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Error Display
        uiState.error?.let { error ->
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }


        // Chat Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = false // Newest messages at the bottom
        ) {
            items(items = uiState.messages, key = { it.id }) { chat ->
                ChatItem(chat)
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding() // Handles keyboard padding
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = {
                    userMessage = it
                    onMessageInputChange(it)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = { Text(stringResource(R.string.chat_label)) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        // Recompute tokens when text field gains focus
                        if (focusState.isFocused) {
                             onMessageInputChange(userMessage) // Trigger immediate check on focus
                        }
                    },
                enabled = uiState.isTextInputEnabled,
                placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5 // Allow some vertical expansion
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button( // Using Button instead of IconButton for better touch target
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = "" // Clear input after sending
                         onMessageInputChange("") // Update token count for empty input
                    }
                },
                 modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp) // Ensure good size
                    .padding(start = 0.dp), // Adjust padding if needed
                 enabled = uiState.isTextInputEnabled && userMessage.isNotBlank() && uiState.tokensRemaining != 0,
                 shape = MaterialTheme.shapes.medium, // Consistent shape
                 contentPadding = PaddingValues(16.dp) // Internal padding
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.action_send)
                )
            }
        }
    }
}

// ChatItem remains largely the same as before, using domain ChatMessage
@Composable
fun ChatItem(
    chatMessage: com.ireddragonicy.llmdroid.domain.model.ChatMessage // Use Domain model
) {
     val isUser = chatMessage.author == USER_PREFIX
     val isThinking = chatMessage.isThinking // Assuming this state comes from ViewModel processing

    val backgroundColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer // User messages
         chatMessage.isLoading -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) // Loading placeholder
         isThinking -> MaterialTheme.colorScheme.tertiaryContainer // Model "thinking" phase
        else -> MaterialTheme.colorScheme.secondaryContainer // Model response
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        val authorLabel = when {
            isUser -> stringResource(R.string.user_label)
            isThinking -> stringResource(R.string.thinking_label)
            chatMessage.isLoading -> stringResource(R.string.model_label) // Show model label even when loading
            else -> stringResource(R.string.model_label)
        }

        Text(
            text = authorLabel,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

         Row(
             modifier = Modifier.fillMaxWidth(),
             horizontalArrangement = if(isUser) Arrangement.End else Arrangement.Start
         ) {
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(min = 40.dp, max = maxWidth * 0.85f) // Ensure min width, limit max
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (chatMessage.isLoading && chatMessage.rawMessage.isBlank()) { // Show spinner only if loading AND empty
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).align(Alignment.Center),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = chatMessage.message, // Use the trimmed message
                            )
                        }
                    }
                }
            }
        }
    }
}