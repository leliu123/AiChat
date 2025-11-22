package com.lea.aichat.data.repository

import android.util.Log
import com.lea.aichat.data.api.ApiService
import com.lea.aichat.data.api.ChatRequest
import com.lea.aichat.data.api.Message
import com.lea.aichat.data.api.NetworkModule
import com.lea.aichat.data.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import com.lea.aichat.data.api.SseParser
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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