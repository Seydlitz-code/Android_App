package com.example.app_01

import android.content.Context
import android.content.SharedPreferences

/**
 * LLM 제공자 및 제공자별 API 키.
 * - 기본 제공자: 클로드(Anthropic)
 * - 키 미저장 시: [BuildConfig]의 claude_api_key / openai_api_key / gemini_api_key(local.properties)
 */
object LlmApiKeyStore {
    private const val PREF_NAME = "app_settings"
    private const val KEY_PROVIDER = "llm_provider"
    private const val KEY_CLAUDE = "llm_key_claude"
    private const val KEY_OPENAI = "llm_key_openai"
    private const val KEY_GEMINI = "llm_key_gemini"
    /** 구버전 단일 키 → 클로드 키로 이전 */
    private const val LEGACY_LLM_API_KEY = "llm_api_key"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun migrateLegacy(p: SharedPreferences) {
        if (!p.contains(LEGACY_LLM_API_KEY)) return
        if (p.contains(KEY_CLAUDE)) {
            p.edit().remove(LEGACY_LLM_API_KEY).apply()
            return
        }
        val old = p.getString(LEGACY_LLM_API_KEY, null) ?: ""
        p.edit().putString(KEY_CLAUDE, old).remove(LEGACY_LLM_API_KEY).apply()
    }

    fun getSelectedProvider(context: Context): LlmProvider {
        val p = prefs(context)
        migrateLegacy(p)
        val name = p.getString(KEY_PROVIDER, null)
        return LlmProvider.fromStoredName(name)
    }

    fun saveProvider(context: Context, provider: LlmProvider) {
        prefs(context).edit()
            .putString(KEY_PROVIDER, LlmProvider.toStoredName(provider))
            .apply()
    }

    private fun defaultKey(provider: LlmProvider): String {
        return try {
            when (provider) {
                LlmProvider.CLAUDE -> BuildConfig.CLAUDE_API_KEY?.trim().orEmpty()
                LlmProvider.OPENAI -> BuildConfig.OPENAI_API_KEY?.trim().orEmpty()
                LlmProvider.GEMINI -> BuildConfig.GEMINI_API_KEY?.trim().orEmpty()
            }
        } catch (_: Throwable) {
            ""
        }
    }

    private fun keyPref(provider: LlmProvider): String = when (provider) {
        LlmProvider.CLAUDE -> KEY_CLAUDE
        LlmProvider.OPENAI -> KEY_OPENAI
        LlmProvider.GEMINI -> KEY_GEMINI
    }

    /** [provider]에 대해 실제 요청에 쓸 키 */
    fun getEffectiveKey(context: Context, provider: LlmProvider): String {
        val p = prefs(context)
        migrateLegacy(p)
        val prefName = keyPref(provider)
        if (!p.contains(prefName)) return defaultKey(provider)
        val stored = p.getString(prefName, "")?.trim().orEmpty()
        return if (stored.isNotEmpty()) stored else defaultKey(provider)
    }

    /** 현재 선택된 제공자의 키 */
    fun getEffectiveApiKey(context: Context): String =
        getEffectiveKey(context, getSelectedProvider(context))

    /** 설정 화면 초기값(미저장이면 빌드 기본) */
    fun getValueForEditing(context: Context, provider: LlmProvider): String {
        val p = prefs(context)
        migrateLegacy(p)
        val prefName = keyPref(provider)
        if (!p.contains(prefName)) return defaultKey(provider)
        return p.getString(prefName, "")?.trim() ?: ""
    }

    fun saveKey(context: Context, provider: LlmProvider, key: String) {
        prefs(context).edit()
            .putString(keyPref(provider), key.trim())
            .apply()
    }

    /** 한 번에 제공자 + 세 키 저장 */
    fun saveAll(
        context: Context,
        provider: LlmProvider,
        claudeKey: String,
        openaiKey: String,
        geminiKey: String
    ) {
        prefs(context).edit()
            .putString(KEY_PROVIDER, LlmProvider.toStoredName(provider))
            .putString(KEY_CLAUDE, claudeKey.trim())
            .putString(KEY_OPENAI, openaiKey.trim())
            .putString(KEY_GEMINI, geminiKey.trim())
            .apply()
    }
}
