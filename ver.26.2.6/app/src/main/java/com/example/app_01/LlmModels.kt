package com.example.app_01

/** LLM 제공자별 사용 가능한 모델 정보 */
object LlmModels {

    data class ModelInfo(
        val apiId: String,
        val displayName: String,
    )

    /** 클로드(Anthropic) — 최신 2개, 이전 2개, Flash 2개 */
    val CLAUDE_MODELS = listOf(
        ModelInfo("claude-sonnet-4-20250514", "Claude 4 Sonnet"),
        ModelInfo("claude-opus-4-20250514", "Claude 4 Opus"),
        ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet"),
        ModelInfo("claude-3-opus-20240229", "Claude 3 Opus"),
        ModelInfo("claude-haiku-4-20250514", "Claude 4 Haiku"),
        ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku"),
    )

    /** GPT(OpenAI) — 최신 2개, 이전 2개, Flash 2개 */
    val GPT_MODELS = listOf(
        ModelInfo("gpt-4o-2024-11-20", "GPT-4o"),
        ModelInfo("gpt-4-turbo-2024-04-09", "GPT-4 Turbo"),
        ModelInfo("gpt-4-0613", "GPT-4"),
        ModelInfo("gpt-3.5-turbo-0125", "GPT-3.5 Turbo"),
        ModelInfo("gpt-4o-mini-2024-07-18", "GPT-4o Mini"),
        ModelInfo("gpt-4o-mini", "GPT-4o Mini (latest)"),
    )

    /** 제미나이(Google) — 최신 2개, 이전 2개, Flash 2개 */
    val GEMINI_MODELS = listOf(
        ModelInfo("gemini-2.5-pro-preview-05-06", "Gemini 2.5 Pro"),
        ModelInfo("gemini-2.0-pro-exp-02-05", "Gemini 2.0 Pro"),
        ModelInfo("gemini-1.5-pro-002", "Gemini 1.5 Pro"),
        ModelInfo("gemini-1.0-pro-002", "Gemini 1.0 Pro"),
        ModelInfo("gemini-2.5-flash-preview-05-06", "Gemini 2.5 Flash"),
        ModelInfo("gemini-1.5-flash-002", "Gemini 1.5 Flash"),
    )

    /** 오픈코드 GO — DeepSeek + Kimi */
    val OPENCODE_GO_MODELS = listOf(
        ModelInfo("deepseek-v4-pro", "DeepSeek V4 Pro"),
        ModelInfo("deepseek-v4-flash", "DeepSeek V4 Flash"),
        ModelInfo("kimi-k2.6", "Kimi K2.6"),
        ModelInfo("kimi-k2.5", "Kimi K2.5"),
    )

    fun modelsFor(provider: LlmProvider): List<ModelInfo> = when (provider) {
        LlmProvider.CLAUDE -> CLAUDE_MODELS
        LlmProvider.OPENAI -> GPT_MODELS
        LlmProvider.GEMINI -> GEMINI_MODELS
        LlmProvider.OPENCODE_GO -> OPENCODE_GO_MODELS
    }

    fun defaultModelFor(provider: LlmProvider): String =
        modelsFor(provider).first().apiId

    fun displayNameFor(provider: LlmProvider, apiId: String): String {
        return modelsFor(provider).find { it.apiId == apiId }?.displayName
            ?: modelsFor(provider).first().displayName
    }
}
