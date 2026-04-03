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
    private const val TIMEOUT_SEC = 120L

    /** Gson (에러 본문 파싱·Retrofit 본문과 동일 인스턴스) */
    val gson: Gson = GsonBuilder().create()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    val anthropicApi: AnthropicApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/v1/")
            .client(okHttpClient)
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
