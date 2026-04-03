package com.example.app_01

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

/** 인터넷 참조(DuckDuckGo Instant Answer) — Retrofit GET */
interface DuckDuckGoApi {
    @GET("/")
    suspend fun instantAnswer(
        @Query("q") q: String,
        @Query("format") format: String = "json",
        @Query("no_html") noHtml: Int = 1,
        @Query("skip_disambig") skipDisambig: Int = 1
    ): ResponseBody
}
