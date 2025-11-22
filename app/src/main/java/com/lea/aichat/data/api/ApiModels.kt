package com.lea.aichat.data.api

import com.google.gson.annotations.SerializedName

// API 请求模型
data class ChatRequest(
    @SerializedName("model")
    val model: String = "doubao-seed-1-6", // 默认模型，可根据需要修改
    @SerializedName("messages")
    val messages: List<Message>,
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2000
)

data class Message(
    @SerializedName("role")
    val role: String, // "user" 或 "assistant"
    @SerializedName("content")
    val content: String
)

// API 响应模型
data class ChatResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("object")
    val objectType: String,
    @SerializedName("created")
    val created: Long,
    @SerializedName("model")
    val model: String,
    @SerializedName("choices")
    val choices: List<Choice>,
    @SerializedName("usage")
    val usage: Usage?
)

data class Choice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: Message,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

// 错误响应模型
data class ErrorResponse(
    @SerializedName("error")
    val error: ApiError?
)

data class ApiError(
    @SerializedName("message")
    val message: String,
    @SerializedName("type")
    val type: String?,
    @SerializedName("code")
    val code: String?
)