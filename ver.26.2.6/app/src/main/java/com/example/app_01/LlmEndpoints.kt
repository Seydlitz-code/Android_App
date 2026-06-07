package com.example.app_01

/**
 * LLM 제공자별 API 엔드포인트.
 * Opencode GO는 OpenAI 호환 `/v1/chat/completions` 경로를 [ClaudeChatClient]가 base URL 뒤에 붙입니다.
 */
object LlmEndpoints {
    /** Opencode GO 공식 OpenAI-compatible base (경로에 `/v1` 미포함) */
    const val OPENCODE_GO_DEFAULT_BASE_URL = "https://opencode.ai/zen/go"

    /**
     * 저장된 Opencode GO Base URL을 정규화합니다.
     * 비어 있거나 구버전 경로면 [OPENCODE_GO_DEFAULT_BASE_URL]을 사용합니다.
     */
    fun effectiveOpencodeGoBaseUrl(stored: String?): String {
        return normalizeOpencodeGoBaseUrl(stored?.trim().orEmpty())
            ?: OPENCODE_GO_DEFAULT_BASE_URL
    }

    private fun normalizeOpencodeGoBaseUrl(raw: String): String? {
        if (raw.isBlank()) return null
        var url = raw.trimEnd('/')
        when (url) {
            "https://opencode.ai/go",
            "https://opencode.ai/go/v1",
            OPENCODE_GO_DEFAULT_BASE_URL,
            "$OPENCODE_GO_DEFAULT_BASE_URL/v1" -> return OPENCODE_GO_DEFAULT_BASE_URL
        }
        if (url.endsWith("/v1")) {
            url = url.removeSuffix("/v1")
        }
        return url.ifBlank { null }
    }
}
