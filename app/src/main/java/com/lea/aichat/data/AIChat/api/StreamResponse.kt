package com.lea.aichat.data.AIChat.api

import com.google.gson.annotations.SerializedName

data class StreamChunk(
    @SerializedName("id")
    val id: String?,
    @SerializedName("object")
    val objectType: String?,
    @SerializedName("created")
    val created: Long?,
    @SerializedName("model")
    val model: String?,
    @SerializedName("choices")
    val choices: List<StreamChoice>?
)
data class StreamChoice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("delta")
    val delta: StreamDelta?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class StreamDelta(
    @SerializedName("role")
    val role: String?,
    @SerializedName("content")
    val content: String?
)