package com.example.app_01

/** 프로필에서 선택하는 LLM 제공자 (AI 메뉴·AI CAD 공통) */
enum class LlmProvider {
    CLAUDE,
    OPENAI,
    GEMINI;

    companion object {
        fun fromStoredName(s: String?): LlmProvider {
            return when (s?.trim()?.uppercase()) {
                "OPENAI", "GPT" -> OPENAI
                "GEMINI", "GOOGLE" -> GEMINI
                else -> CLAUDE
            }
        }

        fun toStoredName(p: LlmProvider): String = when (p) {
            CLAUDE -> "CLAUDE"
            OPENAI -> "OPENAI"
            GEMINI -> "GEMINI"
        }
    }
}
