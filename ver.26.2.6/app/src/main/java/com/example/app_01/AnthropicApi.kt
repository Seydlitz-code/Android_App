package com.example.app_01

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Claude API (Anthropic Messages) — AI CAD 파이프라인의 LLM 단계.
 * OkHttp 직접 호출 대신 Retrofit2로 통일.
 */
interface AnthropicApi {
    @POST("messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body body: JsonObject
    ): AnthropicMessagesResponse
}
