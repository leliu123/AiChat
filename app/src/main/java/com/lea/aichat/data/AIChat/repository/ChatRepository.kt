package com.lea.aichat.data.AIChat.repository

import android.util.Log
import com.lea.aichat.DataBsae.ChatDatabase
import com.lea.aichat.data.AIChat.Entity.ChatMessageEntity
import com.lea.aichat.data.AIChat.api.ApiService
import com.lea.aichat.data.AIChat.api.ChatRequest
import com.lea.aichat.data.AIChat.api.Message
import com.lea.aichat.data.AIChat.api.NetworkModule
import com.lea.aichat.data.AIChat.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import com.lea.aichat.data.AIChat.api.SseParser
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(
    private val apiService: ApiService = NetworkModule.apiService,
    private val database: ChatDatabase?=null

) {
    // 将 Entity 转换为 ChatMessage
    private fun ChatMessageEntity.toChatMessage(): ChatMessage{
        return ChatMessage(
            id = this.id,
            text = this.text,
            isUser = this.isUser,
            isLoading = false,
            timestamp = this.timestamp
        )
    }

    // 将 ChatMessage 转换为 Entity
    private fun ChatMessage.toChatMessageEntity(): ChatMessageEntity{
        return ChatMessageEntity(
            id = this.id,
            text = this.text,
            isUser = this.isUser,
            timestamp = this.timestamp
        )
    }
    // 从数据库加载所有消息
    // 从数据库加载所有消息
    fun getAllMessages(): Flow<List<ChatMessage>> {
        return database?.chatMessageDao()?.getAllMessages()
            ?.map { entities -> entities.map { it.toChatMessage() } }
            ?: flow { emit(emptyList()) }
    }

    // 保存消息到数据库
    suspend fun saveMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            try {
                database?.chatMessageDao()?.insertMessage(message.toChatMessageEntity())
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error saving message", e)
            }
        }
    }

    // 批量保存消息
    suspend fun saveMessages(messages: List<ChatMessage>) {
        withContext(Dispatchers.IO) {
            try {
                database?.chatMessageDao()?.insertMessages(messages.map { it.toChatMessageEntity() })
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error saving messages", e)
            }
        }
    }

    // 清空所有消息
    suspend fun clearAllMessages() {
        withContext(Dispatchers.IO) {
            try {
                database?.chatMessageDao()?.deleteAllMessages()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error clearing messages", e)
            }
        }
    }







    suspend fun sendMessage(
        userMessage: String,
        chatHistory: List<ChatMessage>
    ): Result<String> {
        return try {
            // 将聊天历史转换为 API 消息格式
            val messages = chatHistory.map { chatMessage ->
                Message(
                    role = if (chatMessage.isUser) "user" else "assistant",
                    content = chatMessage.text
                )
            }.toMutableList()

            // 添加当前用户消息
            messages.add(Message(role = "user", content = userMessage))

            // 构建请求
            val request = ChatRequest(
                model = "doubao-seed-1-6",
                messages = messages,
                temperature = 0.7,
                maxTokens = 2000
            )

            // 发送请求
            val response = apiService.sendChatMessage(
                authorization = "Bearer ${NetworkModule.getApiKey()}",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val chatResponse = response.body()!!
                val assistantMessage = chatResponse.choices.firstOrNull()?.message?.content
                    ?: "抱歉，无法获取回复"
                Result.success(assistantMessage)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "API Error: $errorBody")
                Result.failure(Exception("API 错误: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Log.e("ChatRepository", "HTTP Exception: ${e.message}", e)
            Result.failure(Exception("网络请求失败: ${e.message}"))
        } catch (e: IOException) {
            Log.e("ChatRepository", "IO Exception: ${e.message}", e)
            Result.failure(Exception("网络连接失败，请检查网络设置"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unknown Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    //流式方法
    suspend fun sendMessageStream(
        userMessage: String,
        chatHistory: List<ChatMessage>
    ): Flow<Result<String>> = flow{
        try {
            val messages = chatHistory.map { chatMessage ->
                Message(
                    role = if (chatMessage.isUser) "user" else "assistant",
                    content = chatMessage.text
                )
            }.toMutableList()

            // 添加当前用户消息
            messages.add(Message(role = "user", content = userMessage))
            val request = ChatRequest(
                model = "doubao-seed-1-6",
                messages = messages,
                temperature = 0.7,
                maxTokens = 2000,
                stream = true  // 启用流式
            )
            
            // 在 IO 线程执行网络请求
            val response = withContext(Dispatchers.IO) {
                apiService.sendChatMessageStream(
                    authorization = "Bearer ${NetworkModule.getApiKey()}",
                    request = request
                )
            }
            
            if (response.isSuccessful && response.body() != null){
                // 立即开始解析流，不等待
                SseParser.parseStream(response.body()!!)
                    .catch { e->
                        Log.e("ChatRepository", "Stream error", e)
                        emit(Result.failure<String>(e))
                    }
                    .collect { content ->
                        emit(Result.success(content))
                    }
            }else{
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "API Error: $errorBody")
                emit(Result.failure(Exception("API 错误: ${response.code()}")))
            }


        }catch (e: HttpException) {
            Log.e("ChatRepository", "HTTP Exception: ${e.message}", e)
            emit(Result.failure(Exception("网络请求失败: ${e.message}")))
        } catch (e: IOException) {
            Log.e("ChatRepository", "IO Exception: ${e.message}", e)
            emit(Result.failure(Exception("网络连接失败，请检查网络设置")))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unknown Exception: ${e.message}", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)  // 确保整个 flow 在 IO 线程执行



}