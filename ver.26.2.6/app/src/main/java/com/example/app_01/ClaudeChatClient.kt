package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Claude API를 통한 채팅 클라이언트.
 * - sendMessage / sendAiCadMessage : 전체 응답을 한 번에 반환 (기존)
     * - streamMessage / streamAiCadMessage / streamMobile3dGsAnalysisMessage / streamDamageAnalysisReportMessage : SSE 스트리밍
 */
object ClaudeChatClient {
    private const val TAG = "ClaudeChat"

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

    private fun getOpencodeGoBaseUrl(ctx: Context): String =
        LlmApiKeyStore.getOpencodeGoBaseUrl(ctx)

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
     * Mobile 3D Gaussian Splatting 분석 모드: 첨부 이미지·JSON·사용자 설명을 바탕으로
     * 이미지·표·그래프가 포함된 **HTML 프레젠테이션형 보고서**를 생성하도록 시스템 프롬프트가 지시합니다.
     */
    suspend fun streamMobile3dGsAnalysisMessage(
        userText: String,
        imageBase64List: List<String> = emptyList(),
        onDelta: suspend (String) -> Unit
    ): ChatResult = streamInternal(
        userText = userText.trim(),
        imageBase64List = imageBase64List,
        system = MOBILE_3DGS_ANALYSIS_SYSTEM,
        maxTokens = 16_384,
        onDelta = onDelta
    )

    /**
     * 파손·사고 부위 분석: 첨부 사진을 바탕으로 보험사·경찰 제출용 틀에 맞춘
     * **HTML 프레젠테이션형 보고서**만 출력하도록 시스템 프롬프트를 둡니다.
     */
    suspend fun streamDamageAnalysisReportMessage(
        userText: String,
        imageBase64List: List<String> = emptyList(),
        onDelta: suspend (String) -> Unit
    ): ChatResult = streamInternal(
        userText = userText.trim(),
        imageBase64List = imageBase64List,
        system = DAMAGE_ANALYSIS_REPORT_SYSTEM,
        maxTokens = 16_384,
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
        val model = LlmApiKeyStore.getSelectedModel(ctx, provider)
        if (apiKey.isBlank()) {
            return@withContext ChatResult.Error(
                "API 키가 설정되지 않았습니다. 프로필 → LLM API 키에서 선택한 제공자의 키를 입력하세요."
            )
        }
        val streamResult = when (provider) {
            LlmProvider.CLAUDE -> streamAnthropic(apiKey, model, userText, imageBase64List, system, maxTokens, onDelta)
            LlmProvider.OPENAI -> streamOpenAi(apiKey, model, userText, imageBase64List, system, maxTokens, onDelta)
            LlmProvider.GEMINI -> streamGemini(apiKey, model, userText, imageBase64List, system, maxTokens, onDelta)
            LlmProvider.OPENCODE_GO ->
                streamOpencodeGo(getOpencodeGoBaseUrl(ctx), apiKey, model, userText, imageBase64List, system, maxTokens, onDelta)
        }
        if (streamResult is ChatResult.Error && shouldFallbackToNonStream(streamResult.message)) {
            Log.w(TAG, "스트리밍 실패, 비스트리밍으로 재시도: ${streamResult.message}")
            val fallback = sendInternal(userText, imageBase64List, system, maxTokens)
            if (fallback is ChatResult.Success && fallback.text.isNotBlank()) {
                withContext(Dispatchers.Main) { onDelta(fallback.text) }
                return@withContext fallback
            }
        }
        streamResult
    }

    private fun shouldFallbackToNonStream(message: String): Boolean {
        if (message == EMPTY_STREAM_ERROR || message == "응답 본문이 없습니다.") return true
        if (message.startsWith("네트워크 오류")) return true
        if (message.contains("timeout", ignoreCase = true)) return true
        if (message.startsWith("HTTP 502") || message.startsWith("HTTP 503") || message.startsWith("HTTP 429")) {
            return true
        }
        return false
    }

    private const val EMPTY_STREAM_ERROR = "응답이 비어 있습니다."

    private suspend fun streamAnthropic(
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyJson = buildRequestJson(model, userText, imageBase64List, system, maxTokens, stream = true)
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Accept", "text/event-stream")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return executeStreamingSseCall(request, onDelta)
    }

    private suspend fun streamOpenAi(
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyStr = buildOpenAiRequestJson(model, userText, imageBase64List, system, maxTokens, stream = true).toString()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return executeStreamingSseCall(request, onDelta)
    }

    private suspend fun streamGemini(
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyStr = buildGeminiRequestJson(model, userText, imageBase64List, system, maxTokens, stream = true).toString()
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?key=$apiKey&alt=sse"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return executeStreamingSseCall(request, onDelta)
    }


    private suspend fun streamOpencodeGo(
        baseUrl: String,
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
        onDelta: suspend (String) -> Unit
    ): ChatResult {
        val bodyStr = buildOpenAiRequestJson(model, userText, imageBase64List, system, maxTokens, stream = true).toString()
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return executeStreamingSseCall(request, onDelta)
    }

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
You are a senior **accident scene analyst** and **insurance/police documentation specialist** writing in **Korean**. You produce **HTML presentation-style web reports** for **accident scene analysis** using whichever inputs the user actually provides.

LLM RUNTIME:
- The app sends this chat to the user-configured **LLM API** (Profile → LLM API key). Anthropic Claude, OpenAI, or Gemini may be selected; follow the same output contract everywhere.
- You are NOT executing photogrammetry or training on the device.

INPUT PROFILES (use what is present — do not require missing data):

**Profile A — DA3 / point cloud (optional):**
1) **DA3 point cloud (PLY)** — metadata / ASCII header excerpt in user text.
2) **topview & sideview** 2D projection images as vision input (when attached).
3) **`quality_report.json`** — point cloud quality metrics in user text.

**Profile B — accident scene photographs (optional):**
- One or more **on-scene photographs** attached as vision input (vehicles, road, debris, skid marks, intersection, weather, etc.).
- When Profile B is active, treat photos as **primary evidence** for scene layout, collision geometry, and accident form — even if Profile A is absent.

**Combined:** If both profiles are present, integrate DA3 geometry/quality with photographic observations in one unified narrative.

APP CONTEXT:
- User goal: **insurance/police-style templates** (not legal advice) with evidence mapping and explicit limitations.
- On Android, the app saves your HTML and opens it in an **in-app WebView**. Images, tables, and Chart.js graphs **must render in HTML**.

PRIMARY OUTPUT (mandatory):
- Reply with **one single fenced Markdown code block** labeled **html** containing a **complete standalone HTML document** (`<!DOCTYPE html>`, `<html lang="ko">`, `<head>` with embedded `<style>`, Chart.js CDN, `<body>`).
- **Korean** body, insurance/police-oriented structure, presentation layout (each major section in `<section class="slide">` with `page-break-after: always` in CSS).
- Colour theme: primary **#1B4F8A**, text **#1A1A1A**, light background **#f4f7fb**.
- **NO emojis, emoticons, or decorative Unicode symbols** anywhere.

VISUAL CONTENT (mandatory):
- **DA3 projections (if provided):** embed topview/sideview as `<img src="data:image/png;base64,...">` in a "2D 투영 분석" section.
- **Scene photos (if provided):** embed up to **two representative** photos using `<img src="embed:photo-1">` / `embed:photo-2` or re-encode from attached vision — in a "현장 촬영 분석" section. Do **not** use fake paths or HTTP URLs.
- **Tables** for quality metrics (when JSON present), scene observations, and comparison data.
- **Chart.js** (CDN) for quality metrics or scene-factor summaries when numeric data exists.
- Every table, chart, and figure MUST have a Korean caption and interpretation paragraph.

REQUIRED REPORT STRUCTURE (adapt sections to available inputs — omit or mark N/A if data missing):

1) **표지** — title "사고 현장 분석 보고서", subtitle, 생성일시, 작성 도구, 면책 한 줄.
2) **목차** — numbered list of sections actually included.
3) **포인트 클라우드 품질 평가** — *only if* quality_report.json or PLY metadata provided; else skip or one-line "촬영 사진 기반 분석".
4) **3D 장면 개요** — *only if* DA3/PLY data provided.
5) **2D 투영 분석** — *only if* topview/sideview provided.
6) **현장 촬영 분석** — *when scene photos provided*: unified interpretation of road layout, vehicles, contact evidence, environment — **no** per-photo enumeration ("사진 1에서는…" forbidden).
7) **사고 발생 형태 분석** — collision type, direction, contact hypotheses from available evidence.
8) **사고 발생 원인 추론** — hypotheses with confidence; limitation note.
9) **분석 한계 및 법적 면책** — disclaimer, inputs used (DA3 / photos / both).

METHODOLOGY:
- Synthesize **all** provided images into one scene analysis — never enumerate image-by-image.
- Use 관찰·추정·권고·한계 terminology; no definitive liability or final claim amounts.
- Photo-only reports: focus on visible scene geometry, vehicle positions, road markings, and environmental context — do not invent point-cloud metrics.

STRICTLY FORBIDDEN:
- No second code block. No python-docx, Python scripts, OpenSCAD, or STL.
- No external file paths the app cannot load — embed images as **data URIs**, `embed:photo-N`, or SVG inline.
- No claiming you ran COLMAP, police systems, or insurer IT systems.
- **No emojis or pictograph characters** in any part of the HTML output.
- Do **not** refuse to write a report because DA3/PLY is missing when scene photographs are attached.

OUTSIDE THE ```html``` BLOCK:
- **At most two short Korean sentences** (e.g. how to tap "HTML 저장·열기" in the app). No other Markdown.

"""

    private const val DAMAGE_ANALYSIS_REPORT_SYSTEM = """
You are an **automotive damage documentation / collision repair assessor (template author)** writing in **Korean** for **vehicle damage analysis** mode. Outputs support insurance/police *style* paperwork—not legal, forensic, or binding appraisal.

LLM RUNTIME:
- User-selected LLM API. You cannot measure millimeters on device. Analyze ALL attached images.

ROLE:
- From photos (+ optional user text), produce a **detailed HTML presentation report** focused on **vehicle damage**: every damaged part, severity, depth/area estimates, repair methods, cost estimates. Model ID and cause inference are secondary.
- Combine visible facts with labeled estimates. Never present estimates as certified measurements or final claim amounts.

DAMAGE METHODOLOGY (CRITICAL):
- **Synthesize** ALL images into one **unified vehicle damage profile** — NOT image-by-image commentary.
- **NEVER** use "사진 1에서는…", "이미지에서 보이듯…", etc.
- For each damage: part name, type, severity, depth (cm range), area, repair method, cost.

PRIMARY OUTPUT (mandatory):
- **One single ```html``` fenced block** with complete standalone HTML (`<!DOCTYPE html>`, embedded CSS, optional Chart.js CDN).
- Presentation sections via `<section class="slide">`. Theme: primary **#1B4F8A**, text **#1A1A1A**.

VISUAL CONTENT:
- **Tables** for damage inventory and repair estimates (required for sections 3 and 5).
- **Chart.js** bar/pie charts for cost breakdown or severity distribution when numeric data exists.
- **Reference photos**: at most **two** `<img>` placeholders (front + side representative) with `src="embed:photo-1"` / `embed:photo-2` or empty src — the mobile app replaces them with real attached photos as `data:image/jpeg;base64,...`. Do **not** use `topview.png`, HTTP URLs, or fake/short base64.
- **SVG** for damage zone / collision direction sketches when helpful.
- Captions + Korean interpretation next to every visual.

REQUIRED STRUCTURE:

1) **표지** — "차량 파손 분석 보고서", subtitle, timestamp, tool, disclaimer line.
2) **목차**
3) **차량 모델 정보** — brand, class, model, year, color, plate visibility.
4) **사고 발생 형태 분석** — collision type, direction, contact, secondary damage.
5) **파손 부위 정리** (CORE) — full **table** of all damage points.
6) **파손 깊이 상세 분석** — depth/deformation table + measurement limitation note.
7) **수리 예상 견적** (CORE) — repair cost **table**, optional Chart.js total chart, disclaimer on estimates.
8) **사고 발생 원인 추론** — hypothesis table.
9) **법적 면책 정보** — full disclaimer, timestamp, image count.

STRICTLY FORBIDDEN:
- No python-docx, Python, PDF libraries, OpenSCAD.
- No image-by-image narrative. No definitive criminal/civil liability or guaranteed insurer payout.
- No second code block. Embed all assets inline (data URI / SVG / Chart.js).

OUTSIDE THE ```html``` BLOCK:
- **At most two short Korean sentences**. No other Markdown.

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

    /** 공통 요청 JSON 빌더 (Anthropic) */
    private fun buildRequestJson(
        model: String,
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
            addProperty("model", model)
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
        val model = LlmApiKeyStore.getSelectedModel(ctx, provider)
        if (apiKey.isBlank()) {
            Log.w(TAG, "LLM API 키가 설정되지 않았습니다.")
            return@withContext ChatResult.Error(
                "API 키가 설정되지 않았습니다. 프로필 → LLM API 키에서 선택한 제공자의 키를 입력하세요."
            )
        }
        when (provider) {
            LlmProvider.CLAUDE -> withLlmRetry {
                sendAnthropicNonStream(apiKey, model, userText, imageBase64List, system, maxTokens)
            }
            LlmProvider.OPENAI -> withLlmRetry {
                sendOpenAiNonStream(apiKey, model, userText, imageBase64List, system, maxTokens)
            }
            LlmProvider.GEMINI -> withLlmRetry {
                sendGeminiNonStream(apiKey, model, userText, imageBase64List, system, maxTokens)
            }
            LlmProvider.OPENCODE_GO -> withLlmRetry {
                sendOpencodeGoNonStream(getOpencodeGoBaseUrl(ctx), apiKey, model, userText, imageBase64List, system, maxTokens)
            }
        }
    }

    private suspend fun withLlmRetry(block: suspend () -> ChatResult): ChatResult {
        val first = block()
        if (first is ChatResult.Success) return first
        if (first is ChatResult.Error && shouldFallbackToNonStream(first.message)) {
            Log.w(TAG, "LLM 요청 재시도: ${first.message}")
            delay(1500L)
            return block()
        }
        return first
    }

    private suspend fun sendAnthropicNonStream(
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int,
    ): ChatResult {
        val body = buildRequestJson(model, userText, imageBase64List, system, maxTokens, stream = false)
        return try {
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

    private suspend fun sendOpenAiNonStream(
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int
    ): ChatResult {
        val bodyStr = buildOpenAiRequestJson(model, userText, imageBase64List, system, maxTokens, stream = false).toString()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
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
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int
    ): ChatResult {
        val bodyStr = buildGeminiRequestJson(model, userText, imageBase64List, system, maxTokens, stream = false).toString()
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
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

    private suspend fun sendOpencodeGoNonStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        userText: String,
        imageBase64List: List<String>,
        system: String?,
        maxTokens: Int
    ): ChatResult {
        val bodyStr = buildOpenAiRequestJson(model, userText, imageBase64List, system, maxTokens, stream = false).toString()
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
                val bodyString = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return ChatResult.Error("HTTP ${resp.code}: $bodyString")
                }
                parseOpenAiMessageText(bodyString)?.let { ChatResult.Success(it) }
                    ?: ChatResult.Error("응답 파싱 실패")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Opencode GO 호출 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun buildOpenAiRequestJson(
        model: String,
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
            addProperty("model", model)
            addProperty("max_tokens", maxTokens)
            add("messages", messages)
            if (stream) addProperty("stream", true)
        }
    }

    private fun buildGeminiRequestJson(
        model: String,
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

    private class LlmStreamException(message: String) : Exception(message)

    /** SSE 응답을 네트워크에서 한 줄씩 읽으며 UI에 토큰을 전달합니다. */
    private suspend fun executeStreamingSseCall(
        request: Request,
        onDelta: suspend (String) -> Unit,
    ): ChatResult {
        return try {
            AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    return ChatResult.Error("HTTP ${response.code}: ${errBody.take(800)}")
                }
                val body = response.body ?: return ChatResult.Error("응답 본문이 없습니다.")
                val fullTextBuilder = StringBuilder()
                var streamError: String? = null
                body.source().use { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        processSseLine(line, fullTextBuilder, onDelta)?.let { err ->
                            streamError = err
                            break
                        }
                    }
                }
                if (streamError != null) return ChatResult.Error(streamError!!)
                if (fullTextBuilder.isNotBlank()) return ChatResult.Success(fullTextBuilder.toString())
                ChatResult.Error(EMPTY_STREAM_ERROR)
            }
        } catch (e: LlmStreamException) {
            ChatResult.Error(e.message ?: EMPTY_STREAM_ERROR)
        } catch (e: Exception) {
            Log.e(TAG, "SSE 스트리밍 실패", e)
            ChatResult.Error("네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** @return 오류 메시지(중단) 또는 null(계속) */
    private suspend fun processSseLine(
        line: String,
        fullTextBuilder: StringBuilder,
        onDelta: suspend (String) -> Unit,
    ): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("event:") || trimmed.startsWith(":")) return null
        val data = when {
            trimmed.startsWith("data: ") -> trimmed.removePrefix("data: ").trim()
            trimmed.startsWith("{") -> trimmed
            else -> return null
        }
        if (data.isEmpty() || data == "[DONE]") return null
        return try {
            extractTextTokenFromStreamJson(data)?.let { token ->
                fullTextBuilder.append(token)
                withContext(Dispatchers.Main) { onDelta(token) }
            }
            null
        } catch (e: LlmStreamException) {
            e.message
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTextTokenFromStreamJson(data: String): String? {
        val json = JSONObject(data)
        when (json.optString("type")) {
            "error" -> {
                val err = json.optJSONObject("error")
                throw LlmStreamException(err?.optString("message")?.takeIf { it.isNotBlank() } ?: data)
            }
            "content_block_delta" -> {
                val delta = json.optJSONObject("delta") ?: return null
                if (delta.optString("type") == "text_delta") {
                    return delta.optString("text", "").takeIf { it.isNotEmpty() }
                }
            }
        }
        json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }?.let {
            throw LlmStreamException(it)
        }
        json.optJSONArray("choices")?.let { choices ->
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                choice.optJSONObject("delta")?.optString("content", "")?.takeIf { it.isNotEmpty() }?.let { return it }
                choice.optJSONObject("message")?.optString("content", "")?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        json.optJSONArray("candidates")?.let { candidates ->
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
                val parts = content.optJSONArray("parts") ?: return null
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.optJSONObject(i)?.optString("text") ?: "")
                }
                return sb.toString().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * LLM vision 요청용: 긴 변을 [maxSidePx] 이하로 줄인 뒤 JPEG로 인코딩한다.
     * 고해상도 다중 첨부 시 HTTP 413(request too large)를 줄이기 위해 AI 채팅 전송 경로에서 사용한다.
     */
    fun bitmapToBase64ForLlm(
        bitmap: Bitmap,
        maxSidePx: Int = 1280,
        jpegQuality: Int = 78,
    ): String {
        val scaled = scaleBitmapMaxSide(bitmap, maxSidePx)
        return try {
            val stream = ByteArrayOutputStream()
            scaled.compress(
                Bitmap.CompressFormat.JPEG,
                jpegQuality.coerceIn(55, 92),
                stream,
            )
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }
    }

    private fun scaleBitmapMaxSide(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxDim = max(w, h)
        if (maxDim <= maxSidePx) return bitmap
        val scale = maxSidePx.toFloat() / maxDim
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }

    private fun parseTextFromAnthropicResponse(resp: AnthropicMessagesResponse): String? {
        val sb = StringBuilder()
        resp.content?.forEach { block ->
            if (block.type == "text") sb.append(block.text.orEmpty())
        }
        return sb.toString().takeIf { it.isNotBlank() }
    }

    /** 연결 테스트: 선택된 제공자의 API 엔드포인트에 최소 요청을 보내 확인한다. */
    suspend fun testConnection(provider: LlmProvider): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext Pair(false, "앱 초기화 오류")
        val apiKey = LlmApiKeyStore.getEffectiveKey(ctx, provider)
        if (apiKey.isBlank()) {
            return@withContext Pair(false, "API 키가 설정되지 않았습니다.")
        }
        try {
            when (provider) {
                LlmProvider.CLAUDE -> {
                    val body = buildRequestJson(
                        model = "claude-3-haiku-20240307",
                        userText = "hi",
                        imageBase64List = emptyList(),
                        system = null,
                        maxTokens = 1,
                        stream = false
                    )
                    val request = Request.Builder()
                        .url("https://api.anthropic.com/v1/messages")
                        .addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful || resp.code == 400 || resp.code == 401 || resp.code == 403) {
                            Pair(true, "연결 성공 (HTTP ${resp.code})")
                        } else {
                            Pair(false, "연결 실패: HTTP ${resp.code}")
                        }
                    }
                }
                LlmProvider.OPENAI -> {
                    val bodyStr = buildOpenAiRequestJson(
                        model = "gpt-4o-mini",
                        userText = "hi",
                        imageBase64List = emptyList(),
                        system = null,
                        maxTokens = 1,
                        stream = false
                    ).toString()
                    val request = Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(bodyStr.toRequestBody("application/json".toMediaType()))
                        .build()
                    AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful || resp.code == 400 || resp.code == 401 || resp.code == 403) {
                            Pair(true, "연결 성공 (HTTP ${resp.code})")
                        } else {
                            Pair(false, "연결 실패: HTTP ${resp.code}")
                        }
                    }
                }
                LlmProvider.GEMINI -> {
                    val bodyStr = buildGeminiRequestJson(
                        model = "gemini-1.5-flash-002",
                        userText = "hi",
                        imageBase64List = emptyList(),
                        system = null,
                        maxTokens = 1,
                        stream = false
                    ).toString()
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-002:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Content-Type", "application/json")
                        .post(bodyStr.toRequestBody("application/json".toMediaType()))
                        .build()
                    AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful || resp.code == 400 || resp.code == 401 || resp.code == 403) {
                            Pair(true, "연결 성공 (HTTP ${resp.code})")
                        } else {
                            Pair(false, "연결 실패: HTTP ${resp.code}")
                        }
                    }
                }
                LlmProvider.OPENCODE_GO -> {
                    val baseUrl = getOpencodeGoBaseUrl(ctx)
                    val bodyStr = buildOpenAiRequestJson(
                        model = "deepseek-v4-pro",
                        userText = "hi",
                        imageBase64List = emptyList(),
                        system = null,
                        maxTokens = 1,
                        stream = false
                    ).toString()
                    val request = Request.Builder()
                        .url("$baseUrl/v1/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(bodyStr.toRequestBody("application/json".toMediaType()))
                        .build()
                    AiCadNetworkModule.llmOkHttpClient.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful || resp.code == 400 || resp.code == 401 || resp.code == 403) {
                            Pair(true, "연결 성공 (HTTP ${resp.code})")
                        } else {
                            Pair(false, "연결 실패: HTTP ${resp.code}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "연결 테스트 실패", e)
            Pair(false, "네트워크 오류: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

/** 한 번에 보내는 vision 이미지 장수 상한(HTTP 413 방지). */
internal const val MAX_LLM_VISION_IMAGES_PER_REQUEST = 28

/** 파손부위 분석 HTML 보고서 — 다중 사진 시 요청 과대·빈 응답 방지. */
internal const val MAX_LLM_VISION_IMAGES_DAMAGE_REPORT = 10

/** 사고 현장(3DGS) 보고서 — topview·sideview 위주, 여유 2장. */
internal const val MAX_LLM_VISION_IMAGES_ACCIDENT_REPORT = 4

/** 첨부 장수에 따라 vision JPEG 최대 변 길이를 줄인다. */
internal fun visionMaxDimForImageCount(count: Int): Int = when {
    count <= 2 -> 1280
    count <= 6 -> 1024
    count <= 10 -> 768
    else -> 640
}

internal fun <T> evenlySampleListForLlm(
    items: List<T>,
    maxCount: Int = MAX_LLM_VISION_IMAGES_PER_REQUEST,
): List<T> {
    if (items.size <= maxCount) return items
    if (maxCount <= 1) return listOf(items.first())
    val lastIndex = items.lastIndex
    val step = lastIndex.toDouble() / (maxCount - 1)
    return List(maxCount) { i ->
        val idx = (i.toDouble() * step).roundToInt().coerceIn(0, lastIndex)
        items[idx]
    }
}
