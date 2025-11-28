package com.lea.aichattab.ui.aiChat

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lea.aichattab.ui.theme.AiChatTheme
import com.lea.aichattab.AIChat.chat.ChatMessage
import com.lea.aichattab.ui.aiChat.AIChatViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lea.aichattab.R
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlin.collections.isNotEmpty

@Composable
fun AiChatScreen(
    viewModel: AIChatViewModel= viewModel(factory = AIChatViewModelFactory(LocalContext.current)),

                 ) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState= rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ){//标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally) ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically

        ){
            Text(
                text = stringResource(R.string.aiName),
                style = MaterialTheme.typography.headlineSmall,
                //正中间


            )
            IconButton(
                onClick = {
                    viewModel.processIntent(ChatIntent.ClearChat)
                },
                enabled =uiState.chatMessages.isNotEmpty()&&!uiState.isLoading
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clearChat))

            }
        }
        Spacer(modifier = Modifier.height(16.dp))

            // 聊天消息列表
        LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.chatMessages) {chatMessage->
                    ChatMessageItem(chatMessage = chatMessage)

                }


            }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ){
                TextField(
                    value = uiState.inputText,
                    onValueChange = {

                        text->
                        Log.d("AiChatScreen", "InputTextChange: $text")
                        viewModel.processIntent(ChatIntent.InputTextChange(text))
                        // 打印输入的文本
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    singleLine = false,
                    maxLines = 3,



                )

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.processIntent(ChatIntent.SendMessage)
                    },
                    enabled = uiState.inputText.isNotEmpty()&&!uiState.isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription ="发送")

                }
            }

        }
}







@Composable
fun ChatMessageItem(chatMessage: ChatMessage) {
    val alignment = if (chatMessage.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (chatMessage.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).weight(1f)
                ) {
                    Text(
                        text = if (chatMessage.isUser) "你" else "AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))


                    MarkdownText(
                        markdown = chatMessage.text,
                        style = MaterialTheme.typography.bodyMedium
                    )

                }



            }
        }
    }
}






@Preview(showBackground = true)
@Composable
fun AiChatScreenPreview() {
    AiChatTheme {
        AiChatScreen()
    }
}
