package com.lea.aichat.data.repository

import android.util.Log
import com.lea.aichat.data.api.ApiService
import com.lea.aichat.data.api.ChatRequest
import com.lea.aichat.data.api.Message
import com.lea.aichat.data.api.NetworkModule
import com.lea.aichat.data.chat.ChatMessage
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {
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
}