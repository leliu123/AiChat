package com.lea.aichattab.AIChat.chat

data class ChatMessage(
    val id : Long,
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),

)
