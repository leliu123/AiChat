package com.lea.aichat.ui.aiChat

sealed class ChatIntent {
    data class InputTextChange(val text: String): ChatIntent()


    data object SendMessage: ChatIntent()
    data object ClearChat: ChatIntent()






}