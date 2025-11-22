package com.lea.aichat.ui.aiChat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lea.aichat.data.chat.ChatMessage
import com.lea.aichat.data.repository.ChatRepository
import com.lea.aichat.ui.aiChat.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AIChatViewModel(private val chatRepository: ChatRepository = ChatRepository()): ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var messageIdCounter = 0L
    fun processIntent(intent: ChatIntent) {
        when(intent) {
            is ChatIntent.InputTextChange -> {
                _uiState.update {  currentState->
                    currentState.copy(inputText = intent.text)
                }
                Log.d("AIChatViewModel", "NowinputText: ${uiState.value.inputText}")
            }
            is ChatIntent.SendMessage->{
                sendMessage()
            }
            is ChatIntent.ClearChat ->{
                clearChar()
            }
        }
    }
    private fun sendMessage(){
        val inputText=_uiState.value.inputText.trim()
        if(inputText.isEmpty()){
            return
        }
        _uiState.update { currentState->
            val userMessage= ChatMessage(
                id=messageIdCounter++,
                text = inputText,
                isUser = true
            )
            currentState.copy(
                inputText = "",
                chatMessages = currentState.chatMessages + userMessage,
                isLoading = true,
                error = null
            )
        }
        viewModelScope.launch {
            val result=chatRepository.sendMessage(
                userMessage = inputText,
                chatHistory = _uiState.value.chatMessages
            )

            result.fold(
                onSuccess = { assistantResponse ->
                    // 成功：添加 AI 回复消息
                    _uiState.update { currentState ->
                        val assistantMessage = ChatMessage(
                            id = messageIdCounter++,
                            text = assistantResponse,
                            isUser = false
                        )
                        currentState.copy(
                            chatMessages = currentState.chatMessages + assistantMessage,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    // 失败：显示错误信息
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = exception.message ?: "未知错误"
                        )
                    }
                    Log.e("AIChatViewModel", "Send message failed", exception)
                }
            )
        }

    }
    private fun clearChar(){
        _uiState.update { currentState ->
            currentState.copy(
                chatMessages = emptyList(),
                inputText = "",
                error = null
            )
        }
        messageIdCounter = 0L
    }


}