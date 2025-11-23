package com.lea.aichat.data.AIChat.api
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import java.io.InputStreamReader

object SseParser {
    private val gson = Gson()

    fun parseStream(responseBody: ResponseBody): Flow<String> = flow {
        // 直接使用 InputStream，不使用 BufferedReader 避免缓冲
        val inputStream = responseBody.byteStream()
        val reader = InputStreamReader(inputStream, "UTF-8")

        val lineBuffer = StringBuilder()

        try {
            // 逐字符读取，确保实时性
            val charBuffer = CharArray(1)

            while (reader.read(charBuffer) != -1) {
                val char = charBuffer[0]

                if (char == '\n') {
                    // 遇到换行符，处理完整行
                    val line = lineBuffer.toString().trim()
                    lineBuffer.clear()

                    if (line.isNotEmpty() && !line.startsWith(":")) {
                        if (line.startsWith("data: ")) {
                            val jsonData = line.substring(6)

                            if (jsonData.trim() == "[DONE]") {
                                break
                            }

                            try {
                                val chunk = gson.fromJson(jsonData, StreamChunk::class.java)
                                val content = chunk.choices?.firstOrNull()?.delta?.content

                                if (!content.isNullOrEmpty()) {
                                    emit(content)
                                }
                            } catch (e: Exception) {
                                Log.e("SseParser", "Error parsing chunk: $jsonData", e)
                            }
                        }
                    }
                } else if (char != '\r') {
                    // 忽略 \r，只保留其他字符
                    lineBuffer.append(char)
                }
            }

            // 处理最后一行（如果没有换行符结尾）
            if (lineBuffer.isNotEmpty()) {
                val line = lineBuffer.toString().trim()
                if (line.isNotEmpty() && !line.startsWith(":") && line.startsWith("data: ")) {
                    val jsonData = line.substring(6)
                    if (jsonData.trim() != "[DONE]") {
                        try {
                            val chunk = gson.fromJson(jsonData, StreamChunk::class.java)
                            val content = chunk.choices?.firstOrNull()?.delta?.content

                            if (!content.isNullOrEmpty()) {
                                emit(content)
                            }
                        } catch (e: Exception) {
                            Log.e("SseParser", "Error parsing final chunk: $jsonData", e)
                        }
                    }
                }
            }
        } finally {
            reader.close()
        }
    }
}