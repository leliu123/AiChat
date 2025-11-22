package com.lea.aichat.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // TODO: 将 API Key 和 Base URL 移到配置文件或环境变量中
    private const val BASE_URL = "https://ark.cn-beijing.volces.com/" // 根据你的 API 提供商修改
    private const val API_KEY = "b8743a58-3553-47dc-b6ea-b6675360f777" // 需要替换为实际的 API Key

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 改为 NONE 或 HEADERS，避免缓冲响应体
        // BODY 级别会缓冲整个响应用于日志，导致流式响应延迟
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
            
            // 如果是流式请求，添加 Accept 头
            val url = original.url.toString()
            if (url.contains("chat/completions")) {
                // 检查是否是流式请求（通过检查请求体中的 stream 参数）
                // 或者直接为所有 chat/completions 请求添加
                requestBuilder.header("Accept", "text/event-stream")
            }
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // 流式响应设置为 0（无限等待）
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    // 获取 API Key（可以从 SharedPreferences 或其他地方读取）
    fun getApiKey(): String = API_KEY
}