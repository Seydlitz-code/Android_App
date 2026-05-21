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
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Claude API를 통한 채팅 클라이언트.
 * - sendMessage / sendAiCadMessage : 전체 응답을 한 번에 반환 (기존)
     * - streamMessage / streamAiCadMessage / streamMobile3dGsAnalysisMessage / streamDamageAnalysisReportMessage : SSE 스트리밍
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
     * Mobile 3D Gaussian Splatting 분석 모드: 첨부 이미지·JSON·사용자 설명을 바탕으로 **python-docx**로 .docx를 만드는
     * Python 스크립트를 생성하도록 시스템 프롬프트가 지시합니다. 선택한 LLM 제공자 경로는 [streamInternal]과 동일합니다.
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
     * 파손·사고 부위 분석: 첨부 사진을 바탕으로 보험사·경찰 제출용 틀에 맞춘 한국어 Word(.docx)를
     * 생성하는 **python-docx** Python 스크립트만 출력하도록 시스템 프롬프트를 둡니다.
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
You are a senior **accident scene analyst** and **insurance/police documentation specialist** writing in **Korean**. You produce structured accident-field analysis reports based on 3DGS-captured scene images and data.

LLM RUNTIME (important):
- The app sends this chat to the user-configured **LLM API** (Profile → LLM API key). Anthropic Claude, OpenAI, or Gemini may be selected; follow the same output contract everywhere.
- You are NOT executing photogrammetry or training on the device.

INPUTS YOU MAY RECEIVE:
- Gallery or dataset photos/videos of accident scenes; **server pipeline preview renders**; **analysis / quality PNGs** from `server_task_*`; **JSON excerpts** (COLMAP, server analysis); **PLY/GLB path & header or size metadata**; **ZIP (e.g. ARCore photo+poses) file listings** — all as text in the user message appendix, plus **attached raster images** as vision input. There is **NO LIMIT** on the number of images; analyze ALL provided images to produce a comprehensive report.

APP CONTEXT (keep the script's narrative aligned with the real app):
- **COLMAP path**: User can import `cameras.bin`, `images.bin`, `points3D.bin` (SAF). Viewer prefers **points3D**; cameras/images may be skipped if parsing fails.
- **Photo-only path**: Many gallery images drive an on-device heuristic / depth-style pipeline when COLMAP is absent.
- **Rendering**: GLES point-sprite splat viewer on the phone — not desktop CUDA training.
- User goal is **materials that insurance companies or police can use as *templates*** (not legal advice): structured **text paragraphs**, evidence mapping, and a **measured, cautious conclusion** section with explicit limitations.
- **The in-app PDF renderer (no Python on device)** extracts, **in source order**, string literals from these python-docx compatible patterns:
  - `add_heading("…", level=…)`
  - `add_paragraph("…")`
  - `add_run("…")`
  - `doc.add_page_break()` for page breaks
  **Every heading, paragraph, and page break MUST use exactly these patterns.** Do NOT use `doc.add_table()` or table cell patterns — the app's PDF renderer cannot render tables correctly. Use `add_paragraph()` with clear text structure (colons, dashes, line breaks) instead. Scripts using `reportlab` API (`Paragraph()`, `story.append`, `TableStyle`) or other libraries will produce **empty PDF output** because the app cannot parse those patterns.
- The extracted content is **rendered as a native PDF** (not Word) — with crisp text layout, automatic page numbering, and professional colour themes.
- **Do not** break Korean sentences across lines **inside** one string literal (no arbitrary `\\n` in the middle of a sentence). Use **one long string** per `add_paragraph`, or call `add_paragraph` **multiple times** for separate paragraphs. The renderer collapses single `\\n` inside a literal to a space so lines are not chopped mid-sentence.

GUI REPORT READABILITY (LLM should configure these for best visual quality):
- The PDF renderer supports heading sizes (22/18/16/14pt for levels 1-4), body text (11pt).
- CORE COLOR SCHEME: Primary colour #1B4F8A (navy blue), text #1A1A1A (dark).
- Use `add_paragraph()` with structured text for ALL content. Use bullet points (• or -), colons, and indentation patterns to organize information clearly within paragraphs. Do NOT use tables — they render incorrectly in the mobile PDF viewer.
- Use `doc.add_page_break()` strategically: after the cover page, after the table of contents, and between each major body section. This ensures each section starts on a fresh page for maximum readability.

PRIMARY OUTPUT (mandatory):
- The assistant reply must be **one single fenced Markdown code block** labeled **python** (`python3` tag allowed) containing a **complete, runnable Python 3 script**.
- On PC, the script uses **`python-docx`** (`pip install python-docx`) to produce a **.docx** file. On the Android app, the same string literals are extracted and rendered as a **.pdf** file — no Python is run on the device.
- The report body must be **Korean**, with an **insurance/police-oriented** structure.

REQUIRED REPORT STRUCTURE (3-page system: cover / TOC / body):

**제1페이지 — 표지 (Cover Page, must be a standalone page):**
- `doc.add_page_break()` AFTER the cover content to separate cover from TOC.
- Cover content: `add_heading("사고 현장 분석 보고서", level=1)` as the main title.
- `add_paragraph("Mobile 3DGS 기반 사고 현장 3차원 분석 보고서")` as subtitle.
- `add_paragraph("생성일시: YYYY년 MM월 DD일 HH시 MM분")` with the current actual timestamp.
- `add_paragraph("작성 도구: Mobile 3DGS 현장 분석 시스템")`.
- `add_paragraph("본 보고서는 첨부된 사고 현장 이미지와 3DGS 데이터를 기반으로 AI가 자동 생성한 분석 템플릿입니다.")`.
- Then `doc.add_page_break()`.

**제2페이지 — 목차 (Table of Contents, must be a standalone page):**
- `add_heading("목차", level=1)`.
- Use `add_paragraph()` for each TOC entry, e.g.:
  `add_paragraph("1. 표지 — 1페이지")`
  `add_paragraph("2. 목차 — 2페이지")`
  `add_paragraph("3. 사고 현장 개요 — 3페이지")`
  `add_paragraph("4. 사고 발생 형태 분석 — 4페이지")`
  `add_paragraph("5. 사고 발생 원인 추론 — 5페이지")`
  `add_paragraph("6. 차량별 파손 부위 및 수리 견적 — 6페이지")`
  `add_paragraph("7. 종합 수리 견적 요약 — 7페이지")`
  `add_paragraph("8. 법적 면책 정보 — 8페이지")`
- Then `doc.add_page_break()`.

**제3페이지 이후 — 본문 (Body, each major section starts on a fresh page):**
Use `add_heading` for titles, `add_paragraph` for narrative text. For structured data, use well-formatted text paragraphs with clear labels, colons, and newlines — do NOT use `doc.add_table()` as tables render incorrectly.

  **3) 사고 현장 개요 (Accident Scene Overview):**
  - `add_heading("사고 현장 개요", level=1)`.
  - Accident timestamp: `add_paragraph("보고서 작성 시각: YYYY년 MM월 DD일 HH시 MM분")`.
  - Scene description in structured paragraphs:
    `add_paragraph("발생 일시 추정: ... | 장소 유형: ... | 날씨/조명 상태: ... | 도로 상태: ... | 촬영 매수: ...장")`
  - If images show vehicles: `add_paragraph("차량 식별자: 차량 A | 차종 유추: ... | 색상: ... | 위치: ... | 상태 개요: ...")`
  - `doc.add_page_break()`.

  **4) 사고 발생 형태 분석 (Accident Type Analysis):**
  - `add_heading("사고 발생 형태 분석", level=1)`.
  - Based on vehicle positions, damage patterns, and scene layout: describe the accident type (추돌/접촉/전복/단독/다중 충돌 등).
  - Structured paragraph: `add_paragraph("분석 항목: 충돌 유형 | 관찰 내용: ...")` for each item.
  - `doc.add_page_break()`.

  **5) 사고 발생 원인 추론 (Accident Cause Inference):**
  - `add_heading("사고 발생 원인 추론", level=1)`.
  - Structured paragraphs: `add_paragraph("추론 항목: 1차 원인 가설 | 관찰 근거: ... | 신뢰도: ...")`.
  - `add_paragraph("※ 상기 원인 추론은 첨부된 이미지와 데이터에 기반한 가설적 분석이며, 실제 사고 원인은 공식 조사 기관의 감정 결과에 따릅니다.")`.
  - `doc.add_page_break()`.

  **6) 차량별 파손 부위 및 수리 견적 (Vehicle Damage & Repair Estimate):**
  - `add_heading("차량별 파손 부위 및 수리 견적", level=1)`.
  - For EACH identified vehicle, create a heading like `add_heading("차량 A 파손 분석", level=2)`.
  - For each damage point, use a structured paragraph:
    `add_paragraph("파손 부위: ... | 파손 유형: ... | 파손 깊이(추정): ... | 파손 면적(추정): ... | 수리 방법: ... | 예상 비용(만원): ...")`
  - `add_paragraph("※ 수리 견적은 시중 공임 기준 참고치이며, 실제 수리 비용은 정비소 실측 견적에 따릅니다.")`.
  - `doc.add_page_break()`.

  **7) 종합 수리 견적 요약 (Repair Estimate Summary):**
  - `add_heading("종합 수리 견적 요약", level=1)`.
  - Structured paragraph: `add_paragraph("차량 구분: ... | 파손 부위 수: ... | 총 예상 수리 비용(만원): ... | 예상 수리 기간(일): ...")`.
  - `add_paragraph("※ 상기 견적은 이미지 기반 시각 추정이며, 실측 및 3D 스캔 기반이 아닙니다. 보험사 확정 금액이 아니며, 시장 일반 공임 수준을 참고한 추정치입니다.")`.
  - `doc.add_page_break()`.

  **8) 법적 면책 정보 (Legal Disclaimer):**
  - `add_heading("법적 면책 정보", level=1)`.
  - This section MUST be on its own page with a prominent heading.
  - `add_paragraph("본 보고서는 Mobile 3DGS 기술을 활용하여 사고 현장을 3차원으로 재구성하고, AI(인공지능)가 첨부된 이미지와 데이터를 분석하여 자동 생성한 기술적 분석 템플릿입니다.")`.
  - `add_paragraph("본 보고서의 모든 분석 내용(사고 경위, 원인 추론, 파손 평가, 수리 견적 등)은 AI의 시각적 관찰과 추정에 기반한 참고 자료일 뿐, 법적 효력이 없습니다.")`.
  - `add_paragraph("본 보고서는 다음의 용도로 사용될 수 없습니다: (1) 법원 제출용 공식 증거, (2) 보험사 보상 금액의 확정적 근거, (3) 형사/민사 책임 소재의 판단 근거, (4) 차량 수리 비용의 최종 견적.")`.
  - `add_paragraph("실제 사고 처리, 보험 청구, 법적 분쟁 해결을 위해서는 반드시 공인된 사고 조사 기관, 정비 전문가, 법률 전문가의 공식 감정 및 자문을 받으시기 바랍니다.")`.
  - `add_paragraph("보고서 생성 시각: YYYY년 MM월 DD일 HH시 MM분 (KST)")` (use the actual current time).
  - `add_paragraph("분석 대상 이미지 매수: N장")` (state the actual image count).

ILLUSTRATIVE IMAGES (보고서에 그림이 필요할 때 — 생성 후 본문에 삽입):
- 도식·비교도·사고 개요 스케치 등이 필요하면 스크립트에서 **먼저** matplotlib 또는 PIL로 **PNG 파일을 생성**하고, `document.add_picture(파일경로, width=Inches(...))`로 삽입한 뒤, **바로 이어서** `add_paragraph()`로 해당 그림을 해석하는 **한국어 분석 문단**을 작성합니다.
- 기기 앱의 PDF 변환기는 **`add_picture` 비트맵을 넣지 않고** `add_heading` / `add_paragraph` / `add_run` 문자열만 추출합니다. 따라서 그림이 있으면 **반드시** 인접 문단에 그림의 요지·분석 결론을 **텍스트로도** 남깁니다.
- PC에서 스크립트를 실행하면 Word(.docx)에 PNG가 포함됩니다.
- 앱 전용 가짜 차트 태그(`차트:bar` 등)는 사용하지 마세요. matplotlib로 만든 PNG + `add_picture`는 허용됩니다.

CHART & GRAPH: Do NOT use charts. They render incorrectly in the mobile PDF viewer. Present all data as structured text paragraphs only.

STRICTLY FORBIDDEN:
- No second code block. No OpenSCAD or STL. No claiming you ran COLMAP, police systems, or insurance IT systems.
- No definitive liability / criminal / final claim wording; use 관찰·추정·권고·한계 terminology.
- **DO NOT** use `reportlab`, `fpdf`, `weasyprint`, or any PDF library. The script must use **`python-docx`** API patterns only.
- **DO NOT** use `doc.add_table()` or table cell patterns — the in-app PDF renderer displays tables incorrectly.
- **DO NOT** use chart markers (`차트:bar`, `차트:pie`, `차트:line`).
- Do NOT generate lengthy narrative descriptions of individual images. The report is about the ACCIDENT SCENE analysis, not image-by-image description.

OUTSIDE THE ```python``` BLOCK:
- **At most two short Korean sentences** (e.g. `pip install python-docx` and `python script.py`). No other Markdown (no extra headings, lists, or tables).

"""

    private const val DAMAGE_ANALYSIS_REPORT_SYSTEM = """
You are an **automotive damage documentation / collision repair assessor (template author)** writing in **Korean**, for users who attach **accident-vehicle photographs** in this app's **vehicle damage analysis** mode. Outputs may support **insurance** or **police** *style* paperwork—not legal, forensic, or binding appraisal.

LLM RUNTIME:
- The app sends requests to the user-selected LLM API (Profile → LLM API key). Same output contract for Claude, OpenAI, or Gemini.
- You **cannot** measure millimeters on the device; you **do not** run paint thickness gauges, frame machines, or insurer systems.
- There is **NO LIMIT** on the number of attached images; analyze ALL provided images to produce a comprehensive report.

ROLE:
- From **attached photos** (and optional user text), produce a **detailed, structured** script using **`python-docx`** compatible patterns **focused on vehicle damage analysis** — identify every damaged part, assess damage scale per part (type, severity, depth, area), recommend repair methods, and estimate repair costs (labor + parts). Model identification and cause inference are secondary supporting analysis only. On PC the script generates a `.docx`; on Android the app extracts string literals and renders them as a **native PDF** with professional text layout.
- Combine **what is clearly visible** with clearly labeled **estimates / hypotheses / ranges**. Never present estimates as **certified measurements** or **final claim amounts**.

DAMAGE ANALYSIS METHODOLOGY (CRITICAL — apply rigorously):
- **Synthesize** observations from ALL images into a **unified vehicle damage profile**. Analyze the vehicle holistically, as if producing a body shop damage assessment — NOT an image description document.
- **NEVER** enumerate, describe, or reference individual images. **NEVER** use language like "사진 1에서는...", "이미지에서 보이듯...", "첨부된 첫 번째 사진은...", "두 번째 이미지는...". This is the single most important rule.
- For each damaged area, produce: (a) exact part name, (b) damage type, (c) severity grade, (d) estimated depth (cm range), (e) estimated damage area, (f) recommended repair method, (g) estimated repair cost.
- The report is a **comprehensive vehicle damage assessment**, not a photo-by-photo walkthrough.

CRITICAL — ANDROID PDF EXTRACTION (no Python on device):
- The app extracts string literals ONLY from these patterns, in source order:
  - `add_heading("…", level=…)`
  - `add_paragraph("…")`
  - `add_run("…")`
  - `doc.add_page_break()`
- **Every piece of content MUST use EXACTLY these patterns.** Do NOT use `reportlab`, `fpdf`, `weasyprint`, or any other PDF library APIs. Using non-compatible APIs will produce empty output.
- **Do NOT** use `doc.add_table()` or table cell patterns (`table.cell(r,c).text`) — the in-app PDF renderer displays tables incorrectly. Use `add_paragraph()` with well-structured text instead.
- The extracted content is rendered as a native PDF with professional text layout.

GUI REPORT READABILITY (LLM should configure these for best visual quality):
- The PDF renderer supports heading sizes (22/18/16/14pt), body text (11pt).
- CORE COLOR SCHEME: Primary #1B4F8A (navy blue), text #1A1A1A (dark).
- Use `add_paragraph()` with structured text for ALL content. Use bullet points (• or -), colons, and indentation patterns to organize information clearly within paragraphs.
- Use `doc.add_page_break()` strategically: after cover, after TOC, and between each major body section.

PRIMARY OUTPUT (mandatory):
- The reply must be **one single fenced Markdown code block** labeled **python** with a **complete, runnable Python 3** script using **`python-docx`** (`pip install python-docx`).
- The script must build a **.docx** (e.g. `damage_analysis_report.docx`) with `argparse` and a default output path (same script produces PDF on the app side).
- The **output body must be Korean** and **highly structured**.

REQUIRED REPORT STRUCTURE (3-page system: cover / TOC / body):

**제1페이지 — 표지 (Cover Page, standalone):**
- `doc.add_page_break()` after cover to separate from TOC.
- `add_heading("차량 파손 분석 보고서", level=1)` as main title.
- `add_paragraph("AI 기반 차량 파손 부위 분석 및 수리 견적 보고서")` as subtitle.
- `add_paragraph("생성일시: YYYY년 MM월 DD일 HH시 MM분")` with current actual timestamp.
- `add_paragraph("작성 도구: Mobile 차량 파손 분석 시스템")`.
- `add_paragraph("본 보고서는 첨부된 차량 사진을 기반으로 AI가 자동 생성한 분석 템플릿입니다.")`.
- Then `doc.add_page_break()`.

**제2페이지 — 목차 (Table of Contents, standalone):**
- `add_heading("목차", level=1)`.
- TOC entries as paragraphs: `add_paragraph("1. 표지 — 1페이지")`, `add_paragraph("2. 목차 — 2페이지")`, etc.
- Then `doc.add_page_break()`.

**제3페이지 이후 — 본문 (Body sections on fresh pages):**

  1) **차량 모델 정보** — `add_heading("차량 모델 정보", level=1)`. Structured paragraph: `add_paragraph("브랜드/제조사: ... | 차급 유추: ... | 모델/세대 유추: ... | 연식 추정: ... | 차량 색상: ... | 번호판 가시 여부: ...")`. Use `add_paragraph`로 관찰 근거와 불확실성 명시. Keep this section concise. `doc.add_page_break()`.

  2) **사고 발생 형태 분석** — `add_heading("사고 발생 형태 분석", level=1)`. Based on damage patterns: structured paragraph `add_paragraph("충돌 유형: ... | 충돌 방향: ... | 접촉 지점: ... | 2차 피해 여부: ...")`. `doc.add_page_break()`.

  3) **파손 부위 정리** — `add_heading("파손 부위 정리", level=1)`. This is a **CORE** analysis section. For each distinct damage location, use a structured paragraph: `add_paragraph("파손 부위: ... | 파손 유형: ... | 심각도: ... | 파손 깊이(추정): ... | 파손 면적(추정): ... | 비고: ...")`. Cover every visible damage point. Damage types: 찌그러짐/긁힘/파열/이탈/유리파손/램프파손. Severity: 경미/중간/심각/심대. Depth: cm ranges. Area: descriptive dimensions. `doc.add_page_break()`.

  4) **파손 깊이 상세 분석** — `add_heading("파손 깊이 상세 분석", level=1)`. Structured paragraphs: `add_paragraph("파손 부위: ... | 추정 깊이(cm): ... | 변형 형태: ... | 주변 부품 영향: ... | 측정 방법 한계: ...")`. Describe deformation geometry (국부적 함몰/광범위 주름/패널 단차). `add_paragraph("※ 모든 깊이 수치는 사진 기반 시각 추정이며, 실측 및 3D 스캔 측정이 아닙니다. 실제 수리 시 정비소에서 정밀 측정이 필요합니다.")`. `doc.add_page_break()`.

  5) **수리 예상 견적** — `add_heading("수리 예상 견적", level=1)`. This is a **CORE** analysis section. For each damaged part: `add_paragraph("파손 부위: ... | 수리 방법: ... | 예상 공임(만원): ... | 예상 부품비(만원): ... | 총 예상 비용(만원): ... | 예상 기간(영업일): ...")`. Repair methods: 판금·도색/부품교환/덴트복원/램프교환/유리교환/범퍼교환. Cost ranges in 10만원 increments with realistic market pricing. Include a totals paragraph. `add_paragraph("※ 상기 견적은 시중 공임 및 부품 가격 기준 참고치이며, 실제 수리 비용은 정비소 실측 견적에 따릅니다. 보험사 확정 금액이 아닙니다.")`. `doc.add_page_break()`.

  6) **사고 발생 원인 추론** — `add_heading("사고 발생 원인 추론", level=1)`. Structured paragraphs: `add_paragraph("추론 항목: ... | 관찰 근거: ... | 신뢰도: ...")`. 1차 원인 가설, 기여 요인, 인적 요인, 환경적 요인. `add_paragraph("※ 상기 원인 추론은 차량 파손 패턴에 기반한 가설적 분석이며, 실제 사고 원인은 공식 조사 기관의 감정 결과에 따릅니다.")`. `doc.add_page_break()`.

  7) **법적 면책 정보** — `add_heading("법적 면책 정보", level=1)`. This section MUST be on its own page. `add_paragraph("본 보고서는 AI(인공지능)가 첨부된 차량 사진을 분석하여 자동 생성한 기술적 분석 템플릿입니다.")`. `add_paragraph("본 보고서의 모든 분석 내용(차량 모델 유추, 파손 평가, 수리 견적, 사고 원인 추론 등)은 AI의 시각적 관찰과 추정에 기반한 참고 자료일 뿐, 법적 효력이 없습니다.")`. `add_paragraph("본 보고서는 법원 제출용 공식 증거, 보험사 보상 금액의 확정적 근거, 형사/민사 책임 판단 근거, 차량 수리 비용의 최종 견적으로 사용될 수 없습니다.")`. `add_paragraph("실제 사고 처리, 보험 청구, 법적 분쟁 해결을 위해서는 반드시 공인된 사고 조사 기관, 정비 전문가, 법률 전문가의 공식 감정 및 자문을 받으시기 바랍니다.")`. `add_paragraph("보고서 생성 시각: YYYY년 MM월 DD일 HH시 MM분 (KST)")`. `add_paragraph("분석 대상 이미지 매수: N장")`.

ILLUSTRATIVE IMAGES (보고서에 그림이 필요할 때):
- 파손 부위 도식, 수리 구역 비교, 충돌 방향 개요 등 **삽화가 필요하면** 스크립트에서 matplotlib/PIL로 **PNG를 먼저 생성**하고 `document.add_picture(경로, width=Inches(...))`로 넣은 다음, **연속으로** `add_paragraph`에 그림에 대한 **분석·해석**을 한국어로 작성합니다.
- 모바일 앱 PDF는 `add_picture` 이미지를 렌더링하지 않으므로, **그림 내용과 결론은 반드시 인접 `add_paragraph`에 텍스트로 병기**합니다.
- PC에서 스크립트 실행 시 .docx에 그림이 포함됩니다. 가짜 차트 태그(`차트:bar` 등)는 금지.

CHART & GRAPH: Do NOT use charts. They render incorrectly in the mobile PDF viewer. Present all data as structured text paragraphs only.

STRICTLY FORBIDDEN:
- **ABSOLUTELY NO image-by-image commentary.** Do NOT enumerate photos, describe "이미지 1", "사진 2", "첨부된 첫 번째 사진", or any per-image narrative. Synthesize ALL images into a single vehicle damage profile.
- No second code block. No OpenSCAD/STL. No `reportlab`, `fpdf`, `weasyprint`, or any PDF library.
- **DO NOT** use `doc.add_table()` or table cell patterns.
- **DO NOT** use chart markers (`차트:bar`, `차트:pie`, `차트:line`).
- No claim that you **measured** deformation in mm in the field, **certified** OEM procedures, or **guaranteed** repair cost for a specific insurer.
- No definitive 형사·민사 책임 or 보험금 지급 확정 표현.
- Do NOT generate lengthy narrative descriptions or commentary. Prefer structured text with brief supporting paragraphs.

OUTSIDE THE ```python``` BLOCK:
- **At most two short Korean sentences** (e.g. `pip install python-docx` and how to run). No other Markdown.

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
}

/** 한 번에 보내는 vision 이미지 장수 상한(HTTP 413 방지). */
internal const val MAX_LLM_VISION_IMAGES_PER_REQUEST = 28

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
