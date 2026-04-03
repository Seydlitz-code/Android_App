package com.example.app_01

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * PC AICAD 모델링 서버와 통신합니다.
 *
 * ## API (PC 측에서 구현)
 * - **렌더**: `POST {BASE_URL}/api/aicad/render`
 *   - `multipart/form-data` 파트 이름 `file`, 파일명 권장 `model.scad`, 본문 UTF-8 OpenSCAD 소스
 * - **응답 200** (택일):
 *   - `Content-Type`: `application/octet-stream` 또는 `model/stl` → 본문이 바이너리 STL
 *   - `application/json`: `{ "stl_base64": "..." }` (필수), 선택 `{ "message": "ok" }`
 * - **오류**: 4xx/5xx, 본문 JSON `{ "message": "..." }` 가능
 *
 * - **헬스(연결 테스트)**: `GET {BASE_URL}/api/aicad/health` → 2xx 이면 성공
 */
object AiCadRemoteServerClient {
    private const val TAG = "AiCadRemoteServer"
    private const val RENDER_PATH = "/api/aicad/render"
    private const val HEALTH_PATH = "/api/aicad/health"

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun normalizeBaseUrl(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        while (s.endsWith('/')) s = s.dropLast(1)
        if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
            s = "http://$s"
        }
        return s
    }

    suspend fun testConnection(baseUrlRaw: String): Boolean = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(baseUrlRaw)
        if (base.isEmpty()) return@withContext false
        val url = "$base$HEALTH_PATH"
        return@withContext try {
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "health check failed: $url", e)
            false
        }
    }

    suspend fun uploadScadAndDownloadStl(baseUrlRaw: String, scadSource: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(baseUrlRaw)
            if (base.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("AICAD 서버 주소가 비어 있습니다."))
            }
            val url = "$base$RENDER_PATH"
            val partBody = scadSource.toByteArray(Charsets.UTF_8)
                .toRequestBody("application/octet-stream".toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "model.scad", partBody)
                .build()
            val req = Request.Builder()
                .url(url)
                .post(multipart)
                .build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    val bodyBytes = resp.body?.bytes() ?: ByteArray(0)
                    if (!resp.isSuccessful) {
                        val msg = try {
                            JSONObject(String(bodyBytes, Charsets.UTF_8)).optString("message")
                        } catch (_: Exception) {
                            ""
                        }.ifBlank { "HTTP ${resp.code}: ${resp.message}" }
                        return@withContext Result.failure(IllegalStateException(msg))
                    }
                    val ct = resp.header("Content-Type")?.lowercase() ?: ""
                    when {
                        ct.contains("json") -> parseJsonStl(bodyBytes)
                        else -> {
                            if (bodyBytes.isEmpty()) {
                                Result.failure(IllegalStateException("서버 응답 본문이 비어 있습니다."))
                            } else {
                                Result.success(bodyBytes)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "render request failed: $url", e)
                Result.failure(e)
            }
        }

    private fun parseJsonStl(bodyBytes: ByteArray): Result<ByteArray> = runCatching {
        val root = JSONObject(String(bodyBytes, Charsets.UTF_8))
        if (root.has("stl_base64")) {
            val b64 = root.getString("stl_base64")
            Base64.decode(b64, Base64.DEFAULT)
        } else {
            throw IllegalStateException(root.optString("message", "JSON에 stl_base64가 없습니다."))
        }
    }
}
