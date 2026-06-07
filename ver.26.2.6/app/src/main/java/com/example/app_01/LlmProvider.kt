package com.example.app_01

/** 프로필에서 선택하는 LLM 제공자 (AI 메뉴·AI CAD 공통) */
enum class LlmProvider {
    CLAUDE,
    OPENAI,
    GEMINI,
    OPENCODE_GO;

    companion object {
        fun fromStoredName(s: String?): LlmProvider {
            return when (s?.trim()?.uppercase()) {
                "OPENAI", "GPT" -> OPENAI
                "GEMINI", "GOOGLE" -> GEMINI
                "OPENCODE_GO", "OPENCODE", "OPENCODEGO" -> OPENCODE_GO
                else -> CLAUDE
            }
        }

        fun toStoredName(p: LlmProvider): String = when (p) {
            CLAUDE -> "CLAUDE"
            OPENAI -> "OPENAI"
            GEMINI -> "GEMINI"
            OPENCODE_GO -> "OPENCODE_GO"
        }
    }
}
