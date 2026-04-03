package com.example.app_01

import com.google.gson.annotations.SerializedName

/** Anthropic Messages API 응답 (Retrofit + Gson) */
data class AnthropicMessagesResponse(
    @SerializedName("content") val content: List<AnthropicContentBlock>? = null
)

data class AnthropicContentBlock(
    @SerializedName("type") val type: String? = null,
    @SerializedName("text") val text: String? = null
)

data class AnthropicErrorEnvelope(
    @SerializedName("error") val error: AnthropicErrorDetail? = null
)

data class AnthropicErrorDetail(
    @SerializedName("message") val message: String? = null
)
