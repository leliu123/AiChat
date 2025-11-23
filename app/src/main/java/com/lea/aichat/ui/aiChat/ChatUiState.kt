package com.lea.aichat.ui.aiChat

import com.lea.aichat.data.AIChat.chat.ChatMessage

data class ChatUiState(
    val chatMessages: List<ChatMessage> = emptyList(),
    val inputText: String="",
    val isSending: Boolean = false,
    val isLoading: Boolean = false,
    val modelName: String = "",
    val error: String? = null

)