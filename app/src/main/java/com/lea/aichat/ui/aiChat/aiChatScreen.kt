package com.lea.aichat.ui.aiChat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lea.aichat.R
import com.lea.aichat.data.AIChat.chat.ChatMessage
import com.lea.aichat.ui.aiChat.AIChatViewModelFactory
import com.lea.aichat.ui.theme.AiChatTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun AiChatScreen(
    viewModel: AIChatViewModel = viewModel(factory = AIChatViewModelFactory(LocalContext.current)),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChatHeader(
                title = stringResource(R.string.aiName),
                subTitle = if (uiState.isLoading) "正在思考..." else "和 AI 随时畅聊",
                onClear = { viewModel.processIntent(ChatIntent.ClearChat) },
                enabled = uiState.chatMessages.isNotEmpty() && !uiState.isLoading
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                if (uiState.chatMessages.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.chatMessages) { chatMessage ->
                            ChatMessageItem(chatMessage = chatMessage)
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                LoadingIndicatorBar()
            }

            ChatInputBar(
                text = uiState.inputText,
                onTextChange = {
                    Log.d("AiChatScreen", "InputTextChange: $it")
                    viewModel.processIntent(ChatIntent.InputTextChange(it))
                },
                onSend = { viewModel.processIntent(ChatIntent.SendMessage) },
                enabled = uiState.inputText.isNotEmpty() && !uiState.isLoading,
                onClearDraft = { viewModel.processIntent(ChatIntent.InputTextChange("")) }
            )
        }
    }
}

@Composable
fun ChatMessageItem(chatMessage: ChatMessage) {
    val alignment = if (chatMessage.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (chatMessage.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val onBubbleColor = if (chatMessage.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = if (chatMessage.isUser) 2.dp else 20.dp,
                        bottomStart = if (chatMessage.isUser) 20.dp else 2.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = 1.dp,
                    color = bubbleColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 320.dp)
        ) {
            Text(
                text = if (chatMessage.isUser) "你" else "AI",
                style = MaterialTheme.typography.labelSmall,
                color = onBubbleColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (chatMessage.isUser) {
                Text(
                    text = chatMessage.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBubbleColor
                )
            } else {
                MarkdownText(
                    modifier = Modifier.fillMaxWidth(),
                    markdown = chatMessage.text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = onBubbleColor)
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    title: String,
    subTitle: String,
    onClear: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClear, enabled = enabled) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clearChat))
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    onClearDraft: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入消息...") },
            singleLine = false,
            maxLines = 4,
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onClearDraft, enabled = text.isNotEmpty()) {
            Icon(Icons.Default.Refresh, contentDescription = "清空输入")
        }
        IconButton(
            onClick = onSend,
            enabled = enabled
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
        }
    }
}

@Composable
private fun LoadingIndicatorBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(20.dp)
                .height(20.dp),
            strokeWidth = 2.dp
        )
        Text(
            text = "AI 正在回复...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "开始一段新的对话",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "发送你的第一个问题吧，我已经准备好啦。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatScreenPreview() {
    AiChatTheme {
        AiChatScreen()
    }
}

