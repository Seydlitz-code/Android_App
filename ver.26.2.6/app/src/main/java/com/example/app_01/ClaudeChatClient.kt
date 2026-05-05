package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

/**
 * Claude API를 통한 채팅 클라이언트.
 * - sendMessage / sendAiCadMessage : 전체 응답을 한 번에 반환 (기존)
 * - streamMessage / streamAiCadMessage / streamMobile3dGsAnalysisMessage : SSE 스트리밍
 */
object ClaudeChatClient {
    private const val TAG = "ClaudeChat"
    private const val MODEL = "claude-sonnet-4-6"
    private const val OPENAI_MODEL = "gpt-4o-mini"
    private const val GEMINI_MODEL = "gemini-2.0-flash"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getApiKey(): String {
        val ctx = appContext
        if (ctx != null) {
            return LlmApiKeyStore.getEffectiveApiKey(ctx)
        }
        return try {
            BuildConfig.CLAUDE_API_KEY?.takeIf { it.isNotBlank() } ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun isAvailable(): Boolean = getApiKey().isNotBlank()

    sealed class ChatResult {
        data class Success(val text: String) : ChatResult()
        data class Error(val message: String) : ChatResult()
    }

    enum class AiCadInputOption {
        /** 사용자가 치수·형태를 자연어로 직접 말하고, 부족한 수치는 웹 요약·추론으로 보완 */
        DIMENSIONS_DIRECT,
        /** 사용자 요청만으로 웹 요약에서 치수·특징을 우선 반영 */
        INTERNET_REF
    }

    // ─────────────────────── 스트리밍 API ───────────────────────

    /**
     * 일반 대화 스트리밍: 토큰이 수신될 때마다 [onDelta]를 Main 스레드에서 호출.
     * 완료 후 전체 텍스트가 담긴 [ChatResult]를 반환.
     */
    suspend fun streamMessage(
        text: String,
        imageBase64List: List<String> = emptyList(),
        system: String? = null,
        maxTokens: Int = 4096,
        onDelta: suspend (String) -> Unit
    ): ChatResult = streamInternal(
        userText = text,
        imageBase64List = imageBase64List,
        system = system,
        maxTokens = maxTokens,
        onDelta = onDelta
    )

    /**
     * AI CAD 스트리밍: 웹 요약 후 OpenSCAD 코드를 스트리밍으로 생성.
     */
    suspend fun streamAiCadMessage(
        userText: String,
        imageBase64List: List<String>,
        option: AiCadInputOption,
        onDelta: suspend (String) -> Unit
    ): ChatResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext ChatResult.Error("API 키가 설정되지 않았습니다. 프로필 → LLM API 키에서 입력하거나 local.properties에 claude_api_key를 추가하세요.")
        }
        val webSnippet = try {
            fetchWebSnippet(userText)
        } catch (e: Exception) {
            Log.w(TAG, "웹 요약 실패", e)
            ""
        }
        val wrappedUser = buildAiCadUserPayload(userText, webSnippet, option, imageBase64List.isNotEmpty())
        streamInternal(
            userText = wrappedUser,
            imageBase64List = imageBase64List,
            system = AI_CAD_SYSTEM,
            maxTokens = 8192,
            onDelta = onDelta
        )
    }

    /**
     * Mobile 3D Gaussian Splatting 분석 모드: COLMAP·촬영·품질·앱 파이프라인 관련 질문에 답변 (스트리밍).
     * 선택한 LLM 제공자(Claude/OpenAI/Gemini) 경로를 [streamInternal]과 동일하게 사용합니다.
     */
    suspend fun streamMobile3dGsAnalysisMessage(
        userText: String,
        imageBase64List: List<String> = emptyList(),
        onDelta: suspend (String) -> Unit
    ): ChatResult = streamInternal(
        userText = userText.trim(),
        imageBase64List = imageBase64List,
        system = MOBILE_3DGS_ANALYSIS_SYSTEM,
        maxTokens = 8192,
        onDelta = onDelta
    )

    private suspend fun streamInternal(
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult = withContext(Dispatchers.IO) {
        val ctx = appContext
            ?: return@withContext ChatResult.Error("앱 초기화 오류입니다.")
        val provider = LlmApiKeyStore.getSelectedProvider(ctx)
        val apiKey = LlmApiKeyStore.getEffectiveKey(ctx, provider)
        if (apiKey.isBlank()) {
            return@withContext ChatResult.Error(
                "API 키가 설정되지 않았습니다. 프로필 → LLM API 키에서 선택한 제공자의 키를 입력하세요."
            )
        }
        when (provider) {
            LlmProvider.CLAUDE -> streamAnthropic(apiKey, userText, imageBase64List, system, maxTokens, onDelta)
            LlmProvider.OPENAI -> streamOpenAi(apiKey, userText, imageBase64List, system, maxTokens, onDelta)
            LlmProvider.GEMINI -> streamGemini(apiKey, userText, imageBase64List, system, maxTokens, onDelta)
        }
    }

    private suspend fun streamAnthropic(
        apiKey: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyJson = buildRequestJson(userText, imageBase64List, system, maxTokens, stream = true)
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return ChatResult.Error("HTTP ${response.code}: $errBody")
                }
                val source = response.body?.source()
                    ?: return ChatResult.Error("응답 본문이 없습니다.")
                val fullText = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        if (json.optString("type") == "content_block_delta") {
                            val delta = json.optJSONObject("delta")
                            if (delta?.optString("type") == "text_delta") {
                                val token = delta.optString("text", "")
                                if (token.isNotEmpty()) {
                                    fullText.append(token)
                                    withContext(Dispatchers.Main) { onDelta(token) }
                                }
                            }
                        }
                    } catch (_: Exception) { /* 불완전한 JSON 라인 무시 */ }
                }
                if (fullText.isNotBlank()) ChatResult.Success(fullText.toString())
                else ChatResult.Error("응답이 비어 있습니다.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Anthropic 스트리밍 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun streamOpenAi(
        apiKey: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyStr = buildOpenAiRequestJson(userText, imageBase64List, system, maxTokens, stream = true).toString()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return ChatResult.Error("HTTP ${response.code}: $errBody")
                }
                val source = response.body?.source()
                    ?: return ChatResult.Error("응답 본문이 없습니다.")
                val fullText = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices") ?: continue
                        if (choices.length() == 0) continue
                        val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                        val token = delta.optString("content", "")
                        if (token.isNotEmpty()) {
                            fullText.append(token)
                            withContext(Dispatchers.Main) { onDelta(token) }
                        }
                    } catch (_: Exception) { /* 스킵 */ }
                }
                if (fullText.isNotBlank()) ChatResult.Success(fullText.toString())
                else ChatResult.Error("응답이 비어 있습니다.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI 스트리밍 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun streamGemini(
        apiKey: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyStr = buildGeminiRequestJson(userText, imageBase64List, system, maxTokens, stream = true).toString()
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:streamGenerateContent?key=$apiKey&alt=sse"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return ChatResult.Error("HTTP ${response.code}: $errBody")
                }
                val source = response.body?.source()
                    ?: return ChatResult.Error("응답 본문이 없습니다.")
                val fullText = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data.isEmpty()) continue
                    try {
                        val json = JSONObject(data)
                        val candidates = json.optJSONArray("candidates") ?: continue
                        val first = candidates.optJSONObject(0) ?: continue
                        val content = first.optJSONObject("content") ?: continue
                        val parts = content.optJSONArray("parts") ?: continue
                        for (i in 0 until parts.length()) {
                            val part = parts.optJSONObject(i) ?: continue
                            val token = part.optString("text", "")
                            if (token.isNotEmpty()) {
                                fullText.append(token)
                                withContext(Dispatchers.Main) { onDelta(token) }
                            }
                        }
                    } catch (_: Exception) { /* 스킵 */ }
                }
                if (fullText.isNotBlank()) ChatResult.Success(fullText.toString())
                else ChatResult.Error("응답이 비어 있습니다.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 스트리밍 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    // ─────────────────────── 기존 (비스트리밍) API ───────────────────────

    suspend fun sendMessage(
        text: String,
        imageBase64List: List<String> = emptyList(),
        system: String? = null,
        maxTokens: Int = 4096
    ): ChatResult = sendInternal(
        userText = text,
        imageBase64List = imageBase64List,
        system = system,
        maxTokens = maxTokens
    )

    suspend fun sendAiCadMessage(
        userText: String,
        imageBase64List: List<String>,
        option: AiCadInputOption
    ): ChatResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext ChatResult.Error("API 키가 설정되지 않았습니다. 프로필 → LLM API 키에서 입력하거나 local.properties에 claude_api_key를 추가하세요.")
        }
        val webSnippet = try {
            fetchWebSnippet(userText)
        } catch (e: Exception) {
            Log.w(TAG, "웹 요약 실패", e)
            ""
        }
        val wrappedUser = buildAiCadUserPayload(userText, webSnippet, option, imageBase64List.isNotEmpty())
        sendInternal(
            userText = wrappedUser,
            imageBase64List = imageBase64List,
            system = AI_CAD_SYSTEM,
            maxTokens = 8192
        )
    }

    private const val AI_CAD_SYSTEM = """
You are an expert OpenSCAD 3D modeling engine. The app renders **on the phone** with OpenSCAD WASM (no external libraries) and saves the result as a binary STL file.

STRICT OUTPUT RULES:
- Reply with **ONLY** OpenSCAD source code. No Korean or English explanations, no greetings, no bullet lists, no markdown headings.
- Do **not** wrap in markdown code fences unless you must; raw `.scad` text alone is preferred.
- If you use a fence, use **one** final openscad/scad fenced block with the **complete** model. Do not put long alternate examples or templates above the real answer — the app keeps the last such block.
- Geometry must match the user's request (object type, rough proportions). Do not default every request to the same rounded-rectangle hull unless they clearly asked for that shape.

OPENSCAD / COMMENTS:
- Comments: only valid OpenSCAD (`//` line comments or `/* */`). Every comment line MUST start with `//`. Never continue a comment on the next line without `//`.
- Units: millimeters (mm). The first non-whitespace character must start valid OpenSCAD (e.g. //, ${'$'}fn, union, module).

ROUNDED BOX-LIKE SHELLS (only when the user wants that):
- A classic rounded rectangular shell uses hull() around **eight** sphere(r) calls at the eight corners of the box (inset by r). Fewer than eight spheres creates a broken wedge/shard, not a box.
- For mugs, tools, characters, mechanical parts, etc., use cubes/cylinders/spheres/hull/difference appropriate to that object.

MOBILE / PERFORMANCE (mandatory — keep the mesh light):
- Prefer **3–6** main parts only. Model **recognizable features** (body, lid, panel, buttons, feet) with **simple primitives**: `cube`, `cylinder`, `sphere`, `hull`, `difference`, `union`.
- Set **`${'$'}fn` between 16 and 32** (default 24). Never above 48.
- Avoid deep `minkowski()`, huge `hull()` chains, or many `difference()` cuts.

"""

    private const val MOBILE_3DGS_ANALYSIS_SYSTEM = """
You are a senior 3D vision engineer helping users with **Mobile 3D Gaussian Splatting (3DGS)** inside this Android app.

LLM RUNTIME (important):
- The app sends this chat to the user-configured **LLM API** (Profile → LLM API key). When the user chose **Anthropic / Claude**, requests go to the **Claude Messages API** with this system prompt. OpenAI or Gemini are also supported with the same instructions.
- You are not executing COLMAP or training on the device; you give **analysis, checklists, and triage** in Korean.

APP CONTEXT (align answers with the real app):
- **COLMAP path**: User can pick `cameras.bin`, `images.bin`, `points3D.bin` from device storage (SAF). The viewer builds a splat scene primarily from **points3D**; cameras/images may be skipped if parsing fails but points are valid.
- **Photo-only path**: Gallery / dataset folder images (up to 100) drive an **on-device heuristic / depth-style** pipeline when COLMAP is absent.
- **Rendering**: GLES point-sprite splat viewer on phone — not desktop CUDA training; do not promise Vulkan/GPU release builds.

YOUR JOB:
- Answer in **Korean** (Markdown: headings, bullets, short tables OK).
- Cover: multi-view capture, overlap, exposure, COLMAP export (binary vs text), sparse quality, image count, troubleshooting parse errors, and expected on-device viewing behavior.
- If the user attaches images, briefly assess **photogrammetry / 3DGS suitability** (lighting, texture, motion blur, baseline).
- Do **not** output OpenSCAD or STL. Do not claim you ran COLMAP on a server unless the user asks for generic CLI guidance only.
- Stay factual; when uncertain, say what to verify on-device or in COLMAP logs.

"""

    private fun buildAiCadUserPayload(
        rawUser: String,
        webSnippet: String,
        option: AiCadInputOption,
        hasImages: Boolean
    ): String {
        val webBlock = if (webSnippet.isNotBlank()) {
            "[웹에서 가져온 참고 요약]\n$webSnippet\n"
        } else {
            "[웹 참고 요약]\n(검색 결과가 비어 있음 — 일반 지식과 사용자 설명만으로 추론하세요.)\n"
        }
        val imgNote = if (hasImages) {
            "첨부 이미지가 있으면 형상·비율·재질 느낌을 코드에 반영하세요.\n\n"
        } else ""
        return when (option) {
            AiCadInputOption.DIMENSIONS_DIRECT -> """
(옵션: 3D 모델 치수 직접 입력)
사용자가 만들 대상과 치수·형태를 자연어로 설명했습니다. 명시된 치수는 반드시 따르고, 빠진 수치는 아래 웹 참고와 상식으로 보완하세요.
응답은 시스템 지시대로 **OpenSCAD 소스만** 출력하세요(설명 문장 금지).
**형상**: 전자기기·콘솔·차량 등 실제 사물은 `cube`만으로 쌓지 말고, 시스템에 적힌 대로 **hull / minkowski / offset+extrude / 곡면**으로 실루엣을 살리세요.

$webBlock
$imgNote[사용자 입력]
${rawUser.trim()}
""".trimIndent()

            AiCadInputOption.INTERNET_REF -> """
(옵션: 인터넷 참조 사용)
사용자는 무엇을 만들지 자연어로만 설명했습니다. 아래 웹 참고 요약의 치수·특징·용도를 우선 반영해 현실적인 코드를 작성하세요. 웹 요약이 부족하면 일반 지식으로 보완하세요.
응답은 시스템 지시대로 **OpenSCAD 소스만** 출력하세요(설명 문장 금지).
**형상**: 실제 제품의 **곡면·비대칭 패널·라운딩**을 반영하세요. 박스 누적만이 아니라 **hull / minkowski / offset 라운딩** 등으로 가늠 가능한 실물 형태에 가깝게 만드세요.

$webBlock
$imgNote[사용자 입력]
${rawUser.trim()}
""".trimIndent()
        }
    }

    private suspend fun fetchWebSnippet(query: String): String {
        val q = query.trim()
        if (q.length < 2) return ""
        return try {
            AiCadNetworkModule.duckDuckGoApi.instantAnswer(q).use { body ->
                parseDuckDuckGoJson(body.string())
            }
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo 요청 실패", e)
            ""
        }
    }

    private fun parseDuckDuckGoJson(json: String): String {
        return try {
            val root = JSONObject(json)
            val sb = StringBuilder()
            val abs = root.optString("AbstractText", "").trim()
            if (abs.isNotEmpty()) {
                sb.appendLine(abs)
                root.optString("AbstractURL", "").takeIf { it.isNotBlank() }?.let {
                    sb.appendLine("(출처: $it)")
                }
            }
            val related = root.optJSONArray("RelatedTopics") ?: JSONArray()
            var n = 0
            for (i in 0 until related.length()) {
                if (n >= 6) break
                val item = related.optJSONObject(i) ?: continue
                val text = item.optString("Text", "").trim()
                if (text.isNotEmpty()) {
                    sb.appendLine("• $text")
                    n++
                } else {
                    val topics = item.optJSONArray("Topics") ?: continue
                    for (j in 0 until topics.length()) {
                        if (n >= 6) break
                        val sub = topics.optJSONObject(j) ?: continue
                        val t = sub.optString("Text", "").trim()
                        if (t.isNotEmpty()) {
                            sb.appendLine("• $t")
                            n++
                        }
                    }
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo JSON 파싱 실패", e)
            ""
        }
    }

    /** 공통 요청 JSON 빌더 */
    private fun buildRequestJson(
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        stream: Boolean = false
    ): JsonObject {
        val contentArray = JsonArray()
        for (base64 in imageBase64List) {
            contentArray.add(
                JsonObject().apply {
                    addProperty("type", "image")
                    add(
                        "source",
                        JsonObject().apply {
                            addProperty("type", "base64")
                            addProperty("media_type", "image/jpeg")
                            addProperty("data", base64)
                        }
                    )
                }
            )
        }
        contentArray.add(
            JsonObject().apply {
                addProperty("type", "text")
                addProperty(
                    "text",
                    userText.ifBlank {
                        if (imageBase64List.isNotEmpty()) "이 이미지들을 참고해 답해 주세요."
                        else "무엇을 도와드릴까요?"
                    }
                )
            }
        )
        val messagesArr = JsonArray()
        messagesArr.add(
            JsonObject().apply {
                addProperty("role", "user")
                add("content", contentArray)
            }
        )
        return JsonObject().apply {
            addProperty("model", MODEL)
            addProperty("max_tokens", maxTokens)
            add("messages", messagesArr)
            if (!system.isNullOrBlank()) addProperty("system", system)
            if (stream) addProperty("stream", true)
        }
    }

    private suspend fun sendInternal(
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int
    ): ChatResult = withContext(Dispatchers.IO) {
        val ctx = appContext
            ?: return@withContext ChatResult.Error("앱 초기화 오류입니다.")
        val provider = LlmApiKeyStore.getSelectedProvider(ctx)
        val apiKey = LlmApiKeyStore.getEffectiveKey(ctx, provider)
        if (apiKey.isBlank()) {
            Log.w(TAG, "LLM API 키가 설정되지 않았습니다.")
            return@withContext ChatResult.Error(
                "API 키가 설정되지 않았습니다. 프로필 → LLM API 키에서 선택한 제공자의 키를 입력하세요."
            )
        }
        when (provider) {
            LlmProvider.CLAUDE -> {
                val body = buildRequestJson(userText, imageBase64List, system, maxTokens, stream = false)
                try {
                    val resp = AiCadNetworkModule.anthropicApi.createMessage(apiKey, body = body)
                    parseTextFromAnthropicResponse(resp)?.let { ChatResult.Success(it) }
                        ?: ChatResult.Error("응답 파싱 실패")
                } catch (e: HttpException) {
                    val raw = e.response()?.errorBody()?.string().orEmpty()
                    Log.e(TAG, "API 오류: ${e.code()} $raw", e)
                    val errMsg = try {
                        AiCadNetworkModule.gson.fromJson(raw, AnthropicErrorEnvelope::class.java)
                            ?.error?.message?.takeIf { !it.isNullOrBlank() }
                            ?: "HTTP ${e.code()}: $raw"
                    } catch (_: Exception) {
                        "HTTP ${e.code()}: ${e.message ?: ""}"
                    }
                    ChatResult.Error(errMsg)
                } catch (e: Exception) {
                    Log.e(TAG, "API 호출 실패", e)
                    ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
                }
            }
            LlmProvider.OPENAI -> sendOpenAiNonStream(apiKey, userText, imageBase64List, system, maxTokens)
            LlmProvider.GEMINI -> sendGeminiNonStream(apiKey, userText, imageBase64List, system, maxTokens)
        }
    }

    private suspend fun sendOpenAiNonStream(
        apiKey: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int
    ): ChatResult {
        val bodyStr = buildOpenAiRequestJson(userText, imageBase64List, system, maxTokens, stream = false).toString()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.okHttpClient.newCall(request).execute().use { resp ->
                val bodyString = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return ChatResult.Error("HTTP ${resp.code}: $bodyString")
                }
                parseOpenAiMessageText(bodyString)?.let { ChatResult.Success(it) }
                    ?: ChatResult.Error("응답 파싱 실패")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI 호출 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun sendGeminiNonStream(
        apiKey: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int
    ): ChatResult {
        val bodyStr = buildGeminiRequestJson(userText, imageBase64List, system, maxTokens, stream = false).toString()
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.okHttpClient.newCall(request).execute().use { resp ->
                val bodyString = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return ChatResult.Error("HTTP ${resp.code}: $bodyString")
                }
                parseGeminiMessageText(bodyString)?.let { ChatResult.Success(it) }
                    ?: ChatResult.Error("응답 파싱 실패")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 호출 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun buildOpenAiRequestJson(
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        stream: Boolean
    ): JsonObject {
        val messages = JsonArray()
        if (!system.isNullOrBlank()) {
            messages.add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", system)
            })
        }
        val userMsg = JsonObject().apply {
            addProperty("role", "user")
            if (imageBase64List.isEmpty()) {
                addProperty(
                    "content",
                    userText.ifBlank {
                        "무엇을 도와드릴까요?"
                    }
                )
            } else {
                val parts = JsonArray()
                if (userText.isNotBlank()) {
                    parts.add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", userText)
                    })
                }
                for (b64 in imageBase64List) {
                    parts.add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", "data:image/jpeg;base64,$b64")
                        })
                    })
                }
                if (parts.size() == 0) {
                    parts.add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", "이 이미지를 설명해 주세요.")
                    })
                }
                add("content", parts)
            }
        }
        messages.add(userMsg)
        return JsonObject().apply {
            addProperty("model", OPENAI_MODEL)
            addProperty("max_tokens", maxTokens)
            add("messages", messages)
            if (stream) addProperty("stream", true)
        }
    }

    private fun buildGeminiRequestJson(
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        @Suppress("UNUSED_PARAMETER") stream: Boolean
    ): JsonObject {
        val parts = JsonArray()
        if (userText.isNotBlank()) {
            parts.add(JsonObject().apply { addProperty("text", userText) })
        }
        for (b64 in imageBase64List) {
            parts.add(JsonObject().apply {
                add("inline_data", JsonObject().apply {
                    addProperty("mime_type", "image/jpeg")
                    addProperty("data", b64)
                })
            })
        }
        if (parts.size() == 0) {
            parts.add(JsonObject().apply { addProperty("text", "이미지를 설명해 주세요.") })
        }
        val contents = JsonArray()
        contents.add(JsonObject().apply {
            addProperty("role", "user")
            add("parts", parts)
        })
        return JsonObject().apply {
            add("contents", contents)
            if (!system.isNullOrBlank()) {
                add("systemInstruction", JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", system) })
                    })
                })
            }
            add("generationConfig", JsonObject().apply {
                addProperty("maxOutputTokens", maxTokens)
            })
        }
    }

    private fun parseOpenAiMessageText(body: String): String? {
        return try {
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return null
            if (!message.has("content")) return null
            when (val c = message.get("content")) {
                is String -> c.takeIf { it.isNotBlank() }
                is JSONArray -> {
                    val sb = StringBuilder()
                    for (i in 0 until c.length()) {
                        val part = c.optJSONObject(i)
                        if (part?.optString("type") == "text") {
                            sb.append(part.optString("text"))
                        }
                    }
                    sb.toString().takeIf { it.isNotBlank() }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "OpenAI 응답 파싱 실패", e)
            null
        }
    }

    private fun parseGeminiMessageText(body: String): String? {
        return try {
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                sb.append(parts.optJSONObject(i)?.optString("text") ?: "")
            }
            sb.toString().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini 응답 파싱 실패", e)
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseTextFromAnthropicResponse(resp: AnthropicMessagesResponse): String? {
        val sb = StringBuilder()
        resp.content?.forEach { block ->
            if (block.type == "text") sb.append(block.text.orEmpty())
        }
        return sb.toString().takeIf { it.isNotBlank() }
    }
}
