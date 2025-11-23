package com.lea.aichat.ui.aiChat

import android.util.Log
import androidx.compose.ui.layout.Layout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lea.aichat.data.AIChat.chat.ChatMessage
import com.lea.aichat.data.AIChat.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AIChatViewModel(private val chatRepository: ChatRepository = ChatRepository()): ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var messageIdCounter = 0L
    init {
        loadChatHistory()
    }
    private fun loadChatHistory(){
        viewModelScope.launch{
            try{
                val messages=chatRepository.getAllMessages().first()
                if (messages.isNotEmpty()){
                    messageIdCounter=(messages.maxOfOrNull { it.id } ?: -1L)+1
                    _uiState.update { currentState ->
                        currentState.copy(chatMessages = messages)
                    }
                    Log.d("AIChatViewModel", "Loaded ${messages.size} messages from database")
                }

            }catch (e: Exception) {
                Log.e("AIChatViewModel", "Error loading chat history", e)
            }
        }
    }
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

        // 添加用户消息
        val userMessageId = messageIdCounter++

        val userMessage = ChatMessage(
            id=userMessageId,
            text = inputText,
            isUser = true
        )
        // 保存用户消息到数据库
        viewModelScope.launch {
            chatRepository.saveMessage(userMessage)
            Log.d("AIChatViewModel", "Saved user message with ID: $userMessageId")
        }


        _uiState.update { currentState->
            val userMessage= ChatMessage(
                id=userMessageId,
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

        // 创建 AI 消息占位符
        val assistantMessageId = messageIdCounter++
        var assistantMessage = ChatMessage(
            id = assistantMessageId,
            text = "",
            isUser = false,
            isLoading = true
        )

        // 添加空的 AI 消息到列表
        _uiState.update { currentState ->
            currentState.copy(
                chatMessages = currentState.chatMessages + assistantMessage
            )
        }

        viewModelScope.launch {
            chatRepository.sendMessageStream(
                userMessage = inputText,
                chatHistory = _uiState.value.chatMessages.filter { it.id != assistantMessageId }
            ).collect { result ->
                result.fold(
                    onSuccess = { contentChunk ->
                        // 累积流式内容Log
                        Log.d("AIChatViewModel", "Received chunk: $contentChunk")
                        assistantMessage = assistantMessage.copy(

                            text = assistantMessage.text + contentChunk,
                            isLoading = true
                        )

                        // 更新 UI
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.chatMessages.map { message ->
                                if (message.id == assistantMessageId) {
                                    assistantMessage
                                } else {
                                    message
                                }
                            }
                            currentState.copy(
                                chatMessages = updatedMessages,
                                isLoading = true
                            )
                        }
                    },
                    onFailure = { exception ->
                        // 流式响应完成或出错
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.chatMessages.map { message ->
                                if (message.id == assistantMessageId) {
                                    assistantMessage.copy(isLoading = false)
                                } else {
                                    message
                                }
                            }
                            currentState.copy(
                                chatMessages = updatedMessages,
                                isLoading = false,
                                error = exception.message ?: "未知错误"
                            )
                        }
                        Log.e("AIChatViewModel", "Stream failed", exception)
                    }
                )
            }
            assistantMessage = assistantMessage.copy(isLoading = false)
            chatRepository.saveMessage(assistantMessage)

            // 流式响应完成，标记为不再加载
            _uiState.update { currentState ->
                val updatedMessages = currentState.chatMessages.map { message ->
                    if (message.id == assistantMessageId) {
                        assistantMessage
                    } else {
                        message
                    }
                }
                currentState.copy(
                    chatMessages = updatedMessages,
                    isLoading = false
                )
            }

        }


    }
    private fun clearChar(){
        viewModelScope.launch {
            // 清空数据库
            chatRepository.clearAllMessages()
        }
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