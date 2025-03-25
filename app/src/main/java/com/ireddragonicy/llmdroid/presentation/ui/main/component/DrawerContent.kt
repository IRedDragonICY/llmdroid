package com.ireddragonicy.llmdroid.presentation.ui.main.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ireddragonicy.llmdroid.domain.model.ChatSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    chatSessions: List<ChatSession>,
    isLoading: Boolean,
    currentChatId: String?,
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    // --- MODIFIED SIGNATURE ---
    onDeleteSession: (chatIdToDelete: String, currentChatId: String?) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = 16.dp)
    ) {
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Chat")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Chat History",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )

        if (isLoading && chatSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else if (!isLoading && chatSessions.isEmpty()) {
             Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("No chats yet", style = MaterialTheme.typography.bodyMedium)
            }
        }
        else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(items = chatSessions, key = { it.id }) { session ->
                    ChatSessionItem(
                        session = session,
                        isSelected = session.id == currentChatId,
                        onClick = { onSessionClick(session.id) },
                        // --- PASS currentChatId to onDelete lambda ---
                        onDelete = { onDeleteSession(session.id, currentChatId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit, // No change here, the correct lambda is passed from DrawerContent
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.getPreviewText().ifEmpty { "Empty chat" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                 Text(
                     text = session.getFormattedDate(),
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.outline
                 )
            }
             IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${session.title}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}