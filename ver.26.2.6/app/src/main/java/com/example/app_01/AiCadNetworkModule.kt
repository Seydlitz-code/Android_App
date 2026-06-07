package com.example.app_01

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * AI CAD 파이프라인 상단: Retrofit2 + OkHttp 공유 클라이언트.
 * (다이어그램: API 클라이언트 — Retrofit2 / OkHttp)
 */
object AiCadNetworkModule {
    private const val DEFAULT_TIMEOUT_SEC = 120L
    /** LLM 스트리밍·대용량 vision 요청: 첫 토큰 지연·장문 생성에 read 타임아웃 없음 */
    private const val LLM_CONNECT_TIMEOUT_SEC = 60L
    private const val LLM_WRITE_TIMEOUT_SEC = 300L
    private const val LLM_CALL_TIMEOUT_SEC = 600L

    /** Gson (에러 본문 파싱·Retrofit 본문과 동일 인스턴스) */
    val gson: Gson = GsonBuilder().create()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Claude·GPT·Gemini·Opencode GO 등 LLM API 전용 (장시간 SSE·대용량 업로드) */
    val llmOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(LLM_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(LLM_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .callTimeout(LLM_CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val anthropicApi: AnthropicApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/v1/")
            .client(llmOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AnthropicApi::class.java)
    }

    val duckDuckGoApi: DuckDuckGoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.duckduckgo.com/")
            .client(okHttpClient)
            .build()
            .create(DuckDuckGoApi::class.java)
    }
}
