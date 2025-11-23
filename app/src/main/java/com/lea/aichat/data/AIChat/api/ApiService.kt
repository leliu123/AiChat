package com.lea.aichat.data.AIChat.api
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ApiService {
    @POST("api/v3/chat/completions")
    suspend fun sendChatMessage(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @Streaming
    @POST("api/v3/chat/completions")
    suspend fun sendChatMessageStream(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Accept") accept: String = "text/event-stream",  // 添加 Accept 头
        @Body request: ChatRequest
    ): Response<ResponseBody>
}