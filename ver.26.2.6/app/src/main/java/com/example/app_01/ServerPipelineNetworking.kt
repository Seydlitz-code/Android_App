package com.example.app_01

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// --- 서버 설정 · 엔드포인트 (MainActivity UI 등 동일 패키지에서 참조) ---
internal const val UPLOAD_ENDPOINT = "/upload"
/** FastAPI 기본 문서 — 연결 테스트용 GET(가벼움). `/upload`는 POST만 있어 HEAD/GET이 405여도 “테스트 성공”으로 오해할 수 있음 */
internal const val SERVER_CONNECTIVITY_GET_PATH = "/docs"
internal const val STATUS_ENDPOINT = "/status"
internal const val DOWNLOAD_ENDPOINT = "/download"
/** FastAPI `GET /results/{task_id}` — 결과 파일 목록 + 개별 다운로드 URL */
internal const val RESULTS_ENDPOINT = "/results"

/** Jetson DA3 파이프라인 기본 호스트(새 설치·설정 초기화 시 사용). HTTPS + 443 */
internal const val DEFAULT_SERVER_ADDRESS = "fifth-theatrics-bulldog.ngrok-free.dev"
internal const val DEFAULT_SERVER_PORT = 443
internal const val DEFAULT_USE_HTTPS = true

/** 업로드 시 `callback_url` 경로 — 베이스는 [buildServerOrigin]과 동일해야 함. */
private const val PIPELINE_CALLBACK_PATH = "/pipeline/callback"

/** 파이프라인 대기용 웨이크락 최대 시간 (uploadZipAndRunPipeline 등) */
internal const val SERVER_PIPELINE_WAKE_MAX_MS = 4L * 60L * 60L * 1000L

/**
 * 마지막으로 **성공한** GET `/status/{task_id}` 응답 이후, 연속으로 실패(null)가 나와도
 * 이 시간(밀리초)이 지나기 전에는 파이프라인을 "응답 없음"으로 종료하지 않는다.
 * (서버가 노이즈 제거 등으로 워커를 점유해 /status 가 수십 초~수 분 늦게 오는 경우 대비)
 */
internal const val SERVER_STATUS_POLL_MAX_SILENCE_MS = 10L * 60L * 1000L

/** FastAPI `POST /upload` 의 `file_pc: UploadFile = File(...)` 필드명 */
internal const val SERVER_PIPELINE_PART_PC = "file_pc"
/** FastAPI `file_gs: Optional[UploadFile] = File(None)` — 3DGS·보조 ZIP(선택) */
internal const val SERVER_PIPELINE_PART_GS = "file_gs"
internal const val SERVER_PIPELINE_ZIP_NAME_DATASET = "dataset.zip"
internal const val SERVER_PIPELINE_ZIP_NAME_ARCORE = "arcore.zip"

// SAM3 배경제거 서버 (sam3_server.py, 기본 포트 8001)
private const val SAM3_BG_REMOVE_ENDPOINT = "/bg-remove"
private const val SAM3_DEFAULT_PORT = 8001

private const val PREF_SERVER_ADDRESS = "server_address"
private const val PREF_SERVER_PORT = "server_port"
private const val PREF_USE_HTTPS = "use_https"

/**
 * 스트림 읽기·쓰기 버퍼 — 2 MiB.
 * LAN (1 Gbps) 에서 초당 100 MB/s 이상의 단일 스트림 처리량을 확보합니다.
 * 소켓 버퍼·OkHttp·Okio 세그먼트와 조화를 이루도록 설정합니다.
 */
private const val SERVER_DOWNLOAD_STREAM_BUFFER_BYTES = 2 * 1024 * 1024

/** 코루틴 yield 간격 — 16 MiB 마다 UI·다른 코루틴에 CPU 회복 기회 제공 */
private const val SERVER_DOWNLOAD_YIELD_INTERVAL_BYTES = 16 * 1024 * 1024

/** 순차 다운로드 간 완충 (ms) */
private const val BETWEEN_SERVER_ARTIFACT_DOWNLOAD_MS = 20L

private const val SERVER_DOWNLOAD_PROGRESS_MIN_INTERVAL_MS = 5_000L

private const val SERVER_DOWNLOAD_STAGING_SUFFIX = ".downloading"
private const val SERVER_DOWNLOAD_PART_SUFFIX = ".part"

/** multipart `filename=` — 경로 제거·빈 값은 fallback·`.zip` 보장. */
private fun multipartZipFilenameForServer(raw: String, fallback: String): String {
    val leaf = raw.trim().substringAfterLast('/').substringAfterLast('\\').trim()
    val base = leaf.ifBlank { fallback }
    return if (base.endsWith(".zip", ignoreCase = true)) base else "$base.zip"
}

internal fun getServerAddress(context: Context): String {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getString(PREF_SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS) ?: DEFAULT_SERVER_ADDRESS
}

internal fun getServerPort(context: Context): Int {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getInt(PREF_SERVER_PORT, DEFAULT_SERVER_PORT)
}

internal fun getUseHttps(context: Context): Boolean {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getBoolean(PREF_USE_HTTPS, DEFAULT_USE_HTTPS)
}

internal fun saveServerSettings(context: Context, address: String, port: Int, useHttps: Boolean) {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    prefs.edit()
        .putString(PREF_SERVER_ADDRESS, address)
        .putInt(PREF_SERVER_PORT, port)
        .putBoolean(PREF_USE_HTTPS, useHttps)
        .apply()
}

/**
 * DA3 서버 오리진 (`https://host` … 비표준 포트만 `:포트` 포함).
 * 기본(HTTPS·443)이면 호스트만 두어 `https://fifth-theatrics-bulldog.ngrok-free.dev/upload` 형태와 맞춥니다.
 */
internal fun buildServerOriginFromParts(address: String, port: Int, useHttps: Boolean): String {
    val protocol = if (useHttps) "https" else "http"
    val host = address.trim()
    val omitPort = (useHttps && port == 443) || (!useHttps && port == 80)
    return if (omitPort) "$protocol://$host" else "$protocol://$host:$port"
}

internal fun buildServerOrigin(context: Context): String =
    buildServerOriginFromParts(
        getServerAddress(context),
        getServerPort(context),
        getUseHttps(context),
    )

/** `POST /upload`의 `callback_url` — FastAPI(ngrok) 쪽 자체 엔드포인트용. [resolvePipelineCallbackUrlForUpload] 가 우선합니다. */
internal fun getPipelineCallbackUrl(context: Context): String =
    buildServerOrigin(context) + PIPELINE_CALLBACK_PATH

/**
 * 업로드 폼의 `callback_url`.
 *
 * **원인:** [getPipelineCallbackUrl] 만 쓰면 값이 `https://…ngrok…/pipeline/callback` 이 되어,
 * Jetson 워커는 **원격 서버**로만 POST 하고, 휴대폰에서 띄운 [PipelineCallbackHttpServer]에는 도달하지 않습니다.
 * 같은 Wi‑Fi에서는 `http://(휴대폰 IPv4):(NanoHTTPd포트)/pipeline/callback` 을 넘겨야 푸시가 기기로 옵니다.
 *
 * LAN 주소·포트를 쓸 수 없으면 원격 URL로 폴백(앱은 `/status` 폴링·결과 HTTP 다운로드로 완료 처리).
 */
internal fun resolvePipelineCallbackUrlForUpload(
    context: Context,
    lanIpv4: String?,
    localNanoHttpdPort: Int?,
): String {
    val lip = lanIpv4?.trim().orEmpty()
    val p = localNanoHttpdPort
    if (lip.isNotEmpty() && p != null && p in 1..65535) {
        val url = "http://$lip:$p$PIPELINE_CALLBACK_PATH"
        android.util.Log.i(
            "ServerPipeline",
            "callback_url → 기기 LAN 수신 ($url). Jetson/PC와 휴대폰 동일 LAN 필요.",
        )
        return url
    }
    val remote = getPipelineCallbackUrl(context)
    android.util.Log.i(
        "ServerPipeline",
        "callback_url → 원격 ($remote). LAN 미수신 — 푸시는 서버가 처리, 앱은 폴링·다운로드.",
    )
    return remote
}

/**
 * ngrok 무료 터널(`*.ngrok-free.dev` / `*.ngrok-free.app` 등)은 기본적으로
 * **브라우저 경고 HTML** 또는 비 JSON 응답을 끼워 넣을 수 있어 `POST /upload` 가 실패하는데,
 * `GET /docs` 같은 가벼운 요청은 200으로 통과해 「연결 성공」만 뜨는 불일치가 납니다.
 * 공식 우회 헤더로 경고 페이지 건너뛰기.
 *
 * @see [ngrok docs — Skip browser warning](https://ngrok.com/docs/errors/http-403-permission/)
 */
private object NgrokFreeBrowserWarningInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (!isNgrokFreeStyleHost(req.url.host)) {
            return chain.proceed(req)
        }
        val next = req.newBuilder()
            .header("ngrok-skip-browser-warning", "true")
            .build()
        return chain.proceed(next)
    }

    private fun isNgrokFreeStyleHost(host: String): Boolean {
        val h = host.lowercase(Locale.US)
        return h.endsWith(".ngrok-free.dev") ||
            h.endsWith(".ngrok-free.app") ||
            h.endsWith(".ngrok.app") ||
            h.endsWith(".ngrok.io")
    }
}

private val okHttpBaseLock = Any()
@Volatile
private var okHttpBasePlain: OkHttpClient? = null
@Volatile
private var okHttpBaseTrustAll: OkHttpClient? = null

internal fun createOkHttpClient(useHttps: Boolean = DEFAULT_USE_HTTPS): OkHttpClient {
    if (useHttps) {
        okHttpBaseTrustAll?.let { return it }
        synchronized(okHttpBaseLock) {
            okHttpBaseTrustAll?.let { return it }
            return buildOkHttpClientBase(useHttps).also { okHttpBaseTrustAll = it }
        }
    }
    okHttpBasePlain?.let { return it }
    synchronized(okHttpBaseLock) {
        okHttpBasePlain?.let { return it }
        return buildOkHttpClientBase(useHttps).also { okHttpBasePlain = it }
    }
}

internal fun buildOkHttpClientBase(useHttps: Boolean): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .addInterceptor(NgrokFreeBrowserWarningInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)

    if (useHttps) {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
    }

    return builder.build()
}

private val serverDownloadClientLock = Any()
private val serverDownloadClientPlainCache = AtomicReference<OkHttpClient?>(null)
private val serverDownloadClientHttpsCache = AtomicReference<OkHttpClient?>(null)

internal fun getServerDownloadOkHttpClient(useHttps: Boolean): OkHttpClient {
    val cache = if (useHttps) serverDownloadClientHttpsCache else serverDownloadClientPlainCache
    cache.get()?.let { return it }
    synchronized(serverDownloadClientLock) {
        cache.get()?.let { return it }
        val c = createOkHttpClient(useHttps).newBuilder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
        cache.set(c)
        return c
    }
}

// #region agent log
@Suppress("unused")
private fun debugIngestLoopbackHost(): String {
    val fp = Build.FINGERPRINT ?: ""
    val model = Build.MODEL ?: ""
    return if (
        fp.startsWith("generic") ||
        fp.startsWith("unknown") ||
        model.contains("sdk_gphone", ignoreCase = true) ||
        model.contains("Emulator", ignoreCase = true) ||
        model.contains("Android SDK built for x86", ignoreCase = true)
    ) {
        "10.0.2.2"
    } else {
        "127.0.0.1"
    }
}

@Suppress("unused")
private fun agentDebugNdjson(
    context: Context,
    hypothesisId: String,
    location: String,
    message: String,
    data: JSONObject = JSONObject(),
    runId: String = "pre-fix",
) {
    try {
        val payload = JSONObject()
        payload.put("sessionId", "d9340a")
        payload.put("runId", runId)
        payload.put("hypothesisId", hypothesisId)
        payload.put("location", location)
        payload.put("message", message)
        payload.put("timestamp", System.currentTimeMillis())
        payload.put("data", data)
        try {
            val appCtx = context.applicationContext
            File(appCtx.filesDir, "debug-d9340a.log").appendText(payload.toString() + "\n")
        } catch (_: Exception) {
        }
        android.util.Log.i("AgentDbg_d9340a", payload.toString())
        val url =
            "http://${debugIngestLoopbackHost()}:7593/ingest/cf4a6b22-fe0f-42e1-8299-e30d10a3cef4"
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header("X-Debug-Session-Id", "d9340a")
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    } catch (_: Exception) {
    }
}

@Suppress("unused")
private fun agentDebugHeapSnapshot(
    context: Context,
    label: String,
    resultsJsonNull: Boolean? = null,
    resultsFilesLen: Int? = null,
) {
    val rt = Runtime.getRuntime()
    val d = JSONObject()
    d.put("label", label)
    d.put("maxHeapMb", rt.maxMemory() / (1024 * 1024))
    d.put("totalHeapMb", rt.totalMemory() / (1024 * 1024))
    d.put("freeHeapMb", rt.freeMemory() / (1024 * 1024))
    resultsJsonNull?.let { d.put("resultsJsonNull", it) }
    resultsFilesLen?.let { d.put("resultsFilesLen", it) }
    agentDebugNdjson(context, "OOM", "ServerPipelineNetworking.kt:heap", "heap_snapshot", d)
}
// #endregion

/**
 * ZIP 업로드(POST /upload) 직후 서버가 돌려준 task_id 또는 실패 사유.
 */
internal data class ServerUploadStartResult(
    val taskId: String?,
    val errorDetail: String?,
    val gsEnabled: Boolean = false,
)

/**
 * [JSONObject.optString]은 JSON null → "null"을 반환하므로,
 * [JSONObject.opt] + 타입 체크로 올바른 String만 추출합니다.
 */
private fun safeOptString(json: JSONObject, key: String): String? {
    val v = json.opt(key) ?: return null
    if (v !is String) return null
    val s = v.trim()
    return if (s.isNotEmpty() && s != "null") s else null
}

private fun parseFastApiErrorDetail(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val o = JSONObject(body)
        if (!o.has("detail")) return null
        when (val d = o.get("detail")) {
            is String -> d
            is JSONArray -> {
                val sb = StringBuilder()
                for (i in 0 until d.length()) {
                    val item = d.optJSONObject(i)
                    val msg = item?.optString("msg")
                        ?.takeIf { it.isNotBlank() }
                        ?: item?.toString()
                    if (!msg.isNullOrBlank()) {
                        if (sb.isNotEmpty()) sb.append("; ")
                        sb.append(msg.trim())
                    }
                }
                sb.toString().takeIf { it.isNotEmpty() }
            }
            else -> d.toString().trim().takeIf { it.isNotEmpty() }
        }
    } catch (_: JSONException) {
        null
    }
}

/**
 * 미디어 ZIP을 서버 `POST /upload` 로 전송 (`file_pc` 필수, `file_gs` 선택).
 */
internal suspend fun startServerTaskWithZip(
    context: Context,
    zipPcFile: File,
    prompt: String = "",
    contentDispositionPcFilename: String = SERVER_PIPELINE_ZIP_NAME_DATASET,
    gsZipFile: File? = null,
    contentDispositionGsFilename: String = SERVER_PIPELINE_ZIP_NAME_ARCORE,
    callbackUrl: String? = null,
): ServerUploadStartResult {
    return withContext(Dispatchers.IO) {
        try {
            val useHttps = getUseHttps(context)
            val client = createOkHttpClient(useHttps).newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .callTimeout(0, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            if (!zipPcFile.name.endsWith(".zip", ignoreCase = true)) {
                return@withContext ServerUploadStartResult(null, "ZIP 파일만 업로드할 수 있습니다.")
            }
            if (!zipPcFile.isFile || !zipPcFile.canRead()) {
                return@withContext ServerUploadStartResult(null, "업로드할 파일을 읽을 수 없습니다.")
            }
            val gs = gsZipFile
            var gsValid = false
            if (gs != null) {
                if (!gs.name.endsWith(".zip", ignoreCase = true)) {
                    return@withContext ServerUploadStartResult(null, "file_gs 는 ZIP 파일만 허용됩니다.")
                }
                if (!gs.isFile || !gs.canRead()) {
                    return@withContext ServerUploadStartResult(null, "file_gs 파일을 읽을 수 없습니다 (파일 없음·권한).")
                }
                val gsLen = gs.length()
                if (gsLen <= 0L) {
                    return@withContext ServerUploadStartResult(null, "file_gs 빈 파일입니다 (${gsLen} B). ARCore ZIP을 다시 확인하세요.")
                }
                gsValid = true
                android.util.Log.i(
                    "ServerPipelineUpload",
                    "file_gs ready: ${gs.absolutePath} (${gsLen / 1024} KB)",
                )
            } else {
                android.util.Log.i("ServerPipelineUpload", "file_gs not provided (null)")
            }

            val pcPartName = multipartZipFilenameForServer(
                contentDispositionPcFilename,
                SERVER_PIPELINE_ZIP_NAME_DATASET,
            )
            val gsPartName = multipartZipFilenameForServer(
                contentDispositionGsFilename,
                SERVER_PIPELINE_ZIP_NAME_ARCORE,
            )

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    SERVER_PIPELINE_PART_PC,
                    pcPartName,
                    zipPcFile.asRequestBody("application/zip".toMediaType())
                )
            if (gs != null) {
                multipartBuilder.addFormDataPart(
                    SERVER_PIPELINE_PART_GS,
                    gsPartName,
                    gs.asRequestBody("application/zip".toMediaType())
                )
            }
            val p = prompt.trim()
            if (p.isNotEmpty()) {
                multipartBuilder.addFormDataPart("text_prompt", p)
            }
            val cu = callbackUrl?.trim().orEmpty()
            if (cu.isNotEmpty()) {
                multipartBuilder.addFormDataPart("callback_url", cu)
            }
            val requestBody = multipartBuilder.build()

            val url = buildServerOrigin(context) + UPLOAD_ENDPOINT
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = try {
                response.body?.string()
            } finally {
                response.close()
            }

            if (response.isSuccessful && body != null) {
                try {
                    val json = JSONObject(body)
                    val tid = json.optString("task_id").takeIf { it.isNotBlank() }
                    if (tid != null) {
                        val gsEnabled = json.optBoolean("gs_enabled", false)
                        android.util.Log.i(
                            "ServerUpload",
                            "Upload OK: task_id=$tid gsValid=${gs != null} gsServerEnabled=$gsEnabled" +
                                (if (gs != null && !gsEnabled) " — WARN: file_gs ignored by server" else ""),
                        )
                        return@withContext ServerUploadStartResult(tid, null, gsEnabled)
                    }
                    return@withContext ServerUploadStartResult(
                        null,
                        "서버 응답에 task_id가 없습니다.",
                    )
                } catch (e: JSONException) {
                    return@withContext ServerUploadStartResult(
                        null,
                        "서버 응답(JSON)을 해석할 수 없습니다: ${body.take(160)}",
                    )
                }
            }

            val apiDetail = parseFastApiErrorDetail(body)
            val suffix = when {
                !apiDetail.isNullOrBlank() -> ": $apiDetail"
                !body.isNullOrBlank() -> ": ${body.trim().take(200)}"
                else -> ""
            }
            return@withContext ServerUploadStartResult(
                null,
                "업로드 실패 (HTTP ${response.code})$suffix",
            )
        } catch (e: SocketTimeoutException) {
            ServerUploadStartResult(
                null,
                "연결 시간 초과입니다. ZIP 용량·Wi-Fi 상태·서버 부하를 확인하세요. (${e.message})",
            )
        } catch (e: UnknownHostException) {
            ServerUploadStartResult(
                null,
                "서버 주소를 찾을 수 없습니다(DNS). 주소·HTTPS 여부를 확인하세요. (${e.message})",
            )
        } catch (e: ConnectException) {
            ServerUploadStartResult(
                null,
                "서버에 연결할 수 없습니다(연결 거부·방화벽·포트). (${e.message})",
            )
        } catch (e: SSLException) {
            ServerUploadStartResult(
                null,
                "SSL/TLS 오류입니다. HTTPS 설정·인증서를 확인하세요. (${e.message})",
            )
        } catch (e: IOException) {
            ServerUploadStartResult(
                null,
                "네트워크 오류: ${e.message ?: e.javaClass.simpleName}",
            )
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            ServerUploadStartResult(
                null,
                "오류: ${t.message ?: t.javaClass.simpleName}",
            )
        }
    }
}

internal data class ServerTaskStatus(
    val status: String,
    val progressPercent: Int,
    val message: String,
    val downloadUrl: String?,
    val gsStatus: String? = null,
    val gsViewerUrl: String? = null,
)

internal suspend fun fetchServerTaskStatus(context: Context, taskId: String): ServerTaskStatus? {
    return withContext(Dispatchers.IO) {
        try {
            val useHttps = getUseHttps(context)
            // 장시간 동기 단계(노이즈 제거 등)에서 서버가 /status 응답을 늦출 수 있음.
            // 기존 callTimeout 60초는 단일 폴링이 그 한도를 넘기며 실패 → 연속 null → 짧은 침묵 한도로 오판 종료를 유발했다.
            val client = createOkHttpClient(useHttps).newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .callTimeout(0, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val url = buildServerOrigin(context) + "$STATUS_ENDPOINT/$taskId"
            val request = Request.Builder().url(url).get().build()
            // response.use: 예외 발생 시에도 커넥션이 반드시 닫히도록 보장 (커넥션 누수 방지)
            client.newCall(request).execute().use { response ->
                val body = if (response.isSuccessful) response.body?.string() else null
                if (body == null) return@withContext null
                val json = JSONObject(body)
                val pct = when {
                    json.has("progress") -> json.optInt("progress", 0)
                    json.has("progress_percent") -> json.optInt("progress_percent", 0)
                    else -> 0
                }
                ServerTaskStatus(
                    status = json.optString("status"),
                    progressPercent = pct,
                    message = json.optString("message", "처리 중..."),
                    downloadUrl = json.optString("download_url").takeIf { it.isNotBlank() },
                    gsStatus = safeOptString(json, "gs_status"),
                    gsViewerUrl = safeOptString(json, "gs_viewer_url"),
                )
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            t.printStackTrace()
            null
        }
    }
}

/**
 * [GET /results] 본문 상한([maxBytes]) 안에서 루트 객체의 `files` 배열만 스트리밍 파싱합니다.
 * 전체 JSON을 String·루트 [JSONObject] 한 번에 적재하지 않아 메모리 피크(OOM)를 줄입니다.
 */
private class ByteCapInputStream(
    private val delegate: InputStream,
    private val maxBytes: Long,
) : InputStream() {
    private var totalRead = 0L

    override fun close() {
        delegate.close()
    }

    override fun read(): Int {
        if (totalRead >= maxBytes) return -1
        val b = delegate.read()
        if (b >= 0) totalRead++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (totalRead >= maxBytes) return -1
        val cap = minOf(len.toLong(), maxBytes - totalRead).toInt()
        if (cap <= 0) return -1
        val n = delegate.read(b, off, cap)
        if (n > 0) totalRead += n
        return n
    }
}

private fun readServerResultsFilesArray(jr: JsonReader): JSONArray {
    val arr = JSONArray()
    jr.beginArray()
    while (jr.hasNext()) {
        if (jr.peek() != JsonToken.BEGIN_OBJECT) {
            jr.skipValue()
            continue
        }
        jr.beginObject()
        val o = JSONObject()
        while (jr.hasNext()) {
            val name = jr.nextName()
            when (name) {
                "key", "url", "filename" -> {
                    if (jr.peek() == JsonToken.NULL) {
                        jr.nextNull()
                        o.put(name, "")
                    } else {
                        o.put(name, jr.nextString())
                    }
                }
                else -> jr.skipValue()
            }
        }
        jr.endObject()
        arr.put(o)
    }
    jr.endArray()
    return arr
}

private fun parseResultsFilesRootStreaming(
    body: okhttp3.ResponseBody,
    maxBytes: Int,
): JSONObject? {
    return try {
        body.byteStream().use { raw ->
            ByteCapInputStream(raw, maxBytes.toLong()).use { capped ->
                InputStreamReader(capped, Charsets.UTF_8).use { isr ->
                    JsonReader(isr).use { jr ->
                        jr.isLenient = false
                        var files: JSONArray? = null
                        jr.beginObject()
                        while (jr.hasNext()) {
                            when (jr.nextName()) {
                                "files" -> {
                                    files = when (jr.peek()) {
                                        JsonToken.NULL -> {
                                            jr.nextNull()
                                            JSONArray()
                                        }
                                        JsonToken.BEGIN_ARRAY ->
                                            readServerResultsFilesArray(jr)
                                        else -> {
                                            jr.skipValue()
                                            JSONArray()
                                        }
                                    }
                                }
                                else -> jr.skipValue()
                            }
                        }
                        jr.endObject()
                        JSONObject().apply {
                            put("files", files ?: JSONArray())
                        }
                    }
                }
            }
        }
    } catch (t: Throwable) {
        android.util.Log.w("fetchServerResultsJson", "streaming results parse", t)
        null
    }
}

internal suspend fun fetchServerResultsJson(context: Context, taskId: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        try {
            val useHttps = getUseHttps(context)
            val client = getServerDownloadOkHttpClient(useHttps)
            val url = buildServerOrigin(context) + "$RESULTS_ENDPOINT/$taskId"
            val request = Request.Builder().url(url).get().build()
            // response.use: 예외/조기 반환 시에도 커넥션이 반드시 닫히도록 보장
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                if (!response.isSuccessful) {
                    // #region agent log
                    agentDebugNdjson(
                        context,
                        "H1",
                        "ServerPipelineNetworking.kt:fetchServerResultsJson",
                        "results_fail",
                        JSONObject().apply {
                            put("reason", "http_not_success")
                            put("taskId", taskId)
                            put("httpCode", httpCode)
                        },
                    )
                    // #endregion
                    return@withContext null
                }
                val body = response.body ?: run {
                    // #region agent log
                    agentDebugNdjson(
                        context,
                        "H1",
                        "ServerPipelineNetworking.kt:fetchServerResultsJson",
                        "results_fail",
                        JSONObject().apply {
                            put("reason", "body_null")
                            put("taskId", taskId)
                            put("httpCode", httpCode)
                        },
                    )
                    // #endregion
                    return@withContext null
                }
                // OOM 방지: Content-Length 헤더가 있으면 먼저 확인, 없으면(-1) 스트리밍 읽기로 상한 적용.
                // ARCore 결과처럼 파일이 많은 경우 서버가 큰 JSON을 보내므로, 본문 전체를 String으로 적재하지 않고
                // JsonReader로 `files` 배열만 스트리밍 파싱합니다.
                val maxJsonBytes = 4 * 1024 * 1024
                val contentLength = body.contentLength()
                if (contentLength > maxJsonBytes) {
                    android.util.Log.w("fetchServerResultsJson", "결과 JSON이 너무 큼(${contentLength / 1024} KB), 스킵")
                    // #region agent log
                    agentDebugNdjson(
                        context,
                        "H1",
                        "ServerPipelineNetworking.kt:fetchServerResultsJson",
                        "results_fail",
                        JSONObject().apply {
                            put("reason", "content_length_over_cap")
                            put("taskId", taskId)
                            put("contentLength", contentLength)
                            put("cap", maxJsonBytes)
                        },
                    )
                    // #endregion
                    return@withContext null
                }
                val jo = parseResultsFilesRootStreaming(body, maxJsonBytes)
                if (jo == null) {
                    android.util.Log.w(
                        "fetchServerResultsJson",
                        "결과 JSON 스트리밍 파싱 실패 또는 본문 상한(${maxJsonBytes / 1024} KB) 초과",
                    )
                    // #region agent log
                    agentDebugNdjson(
                        context,
                        "H1",
                        "ServerPipelineNetworking.kt:fetchServerResultsJson",
                        "results_fail",
                        JSONObject().apply {
                            put("reason", "stream_parse_or_cap")
                            put("taskId", taskId)
                            put("cap", maxJsonBytes)
                        },
                    )
                    // #endregion
                    return@withContext null
                }
                // #region agent log
                agentDebugNdjson(
                    context,
                    "H1",
                    "ServerPipelineNetworking.kt:fetchServerResultsJson",
                    "results_ok",
                    JSONObject().apply {
                        put("taskId", taskId)
                        put("filesLen", jo.optJSONArray("files")?.length() ?: -1)
                        put("httpCode", httpCode)
                        put("parseMode", "streaming_files_only")
                    },
                )
                // #endregion
                jo
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            // #region agent log
            agentDebugNdjson(
                context,
                "H1",
                "ServerPipelineNetworking.kt:fetchServerResultsJson",
                "results_fail",
                JSONObject().apply {
                    put("reason", "exception")
                    put("taskId", taskId)
                    put("err", t.javaClass.simpleName)
                },
            )
            // #endregion
            t.printStackTrace()
            null
        }
    }
}

internal suspend fun downloadHttpUrlToFile(
    context: Context,
    absoluteUrl: String,
    outFile: File,
    onStreamProgress: ((bytesRead: Long, contentLength: Long) -> Unit)? = null,
): Boolean {
    return withContext(Dispatchers.IO) {
        val partFile = File(outFile.parentFile ?: File("."), "${outFile.name}$SERVER_DOWNLOAD_PART_SUFFIX")
        try {
            val useHttps = getUseHttps(context)
            val client = getServerDownloadOkHttpClient(useHttps)
            val request = Request.Builder()
                .url(absoluteUrl)
                .get()
                .header("Connection", "keep-alive")
                .cacheControl(okhttp3.CacheControl.Builder().noStore().build())
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body
                // #region agent log
                agentDebugNdjson(
                    context,
                    "H2",
                    "ServerPipelineNetworking.kt:downloadHttpUrlToFile",
                    "http_response",
                    JSONObject().apply {
                        put("httpCode", response.code)
                        put("hasBody", body != null)
                        put("declaredCl", body?.contentLength() ?: -1L)
                        put("urlSample", absoluteUrl.take(180))
                    },
                )
                // #endregion
                if (!response.isSuccessful || body == null) {
                    return@withContext false
                }
                val contentLength = body.contentLength()
                outFile.parentFile?.mkdirs()
                try {
                    partFile.delete()
                } catch (_: Exception) {
                }
                var lastProgressAt = 0L
                body.source().use { source ->
                    val sink = partFile.sink().buffer()
                    sink.use { s ->
                        var total = 0L
                        var sinceYield = 0L
                        while (true) {
                            val buf = s.buffer
                            val n = source.read(buf, SERVER_DOWNLOAD_STREAM_BUFFER_BYTES.toLong())
                            if (n <= 0L) break
                            total += n
                            s.emitCompleteSegments()
                            sinceYield += n
                            if (sinceYield >= SERVER_DOWNLOAD_YIELD_INTERVAL_BYTES) {
                                sinceYield = 0L
                                yield()
                            }
                            if (onStreamProgress != null) {
                                val now = System.currentTimeMillis()
                                val done = contentLength > 0 && total >= contentLength
                                if (done || now - lastProgressAt >= SERVER_DOWNLOAD_PROGRESS_MIN_INTERVAL_MS) {
                                    lastProgressAt = now
                                    onStreamProgress(total, contentLength)
                                }
                            }
                        }
                        s.flush()
                    }
                }
                if (!partFile.exists() || partFile.length() <= 0L) {
                    try { partFile.delete() } catch (_: Exception) {}
                    return@withContext false
                }
                if (contentLength > 0L && partFile.length() != contentLength) {
                    android.util.Log.w(
                        "downloadHttpUrlToFile",
                        "incomplete download: ${partFile.length()} / $contentLength bytes ($absoluteUrl)",
                    )
                    try { partFile.delete() } catch (_: Exception) {}
                    // #region agent log
                    agentDebugNdjson(
                        context,
                        "H3",
                        "ServerPipelineNetworking.kt:downloadHttpUrlToFile",
                        "incomplete_vs_content_length",
                        JSONObject().apply {
                            put("partLen", partFile.length())
                            put("declaredCl", contentLength)
                            put("urlSample", absoluteUrl.take(180))
                        },
                    )
                    // #endregion
                    return@withContext false
                }
                try {
                    outFile.delete()
                } catch (_: Exception) {
                }
                if (!partFile.renameTo(outFile)) {
                    partFile.copyTo(outFile, overwrite = true)
                    partFile.delete()
                }
                val okFinal = outFile.exists() && outFile.length() > 0L
                // #region agent log
                agentDebugNdjson(
                    context,
                    "H3",
                    "ServerPipelineNetworking.kt:downloadHttpUrlToFile",
                    "download_finished",
                    JSONObject().apply {
                        put("ok", okFinal)
                        put("finalLen", if (outFile.exists()) outFile.length() else -1L)
                        put("declaredCl", contentLength)
                        put("urlSample", absoluteUrl.take(180))
                    },
                )
                // #endregion
                okFinal
            }
        } catch (t: Throwable) {
            // CancellationException은 구조적 동시성을 위해 반드시 재투소
            if (t is kotlinx.coroutines.CancellationException) throw t
            // #region agent log
            agentDebugNdjson(
                context,
                "H2",
                "ServerPipelineNetworking.kt:downloadHttpUrlToFile",
                "download_exception",
                JSONObject().apply {
                    put("err", t.javaClass.simpleName)
                    put("urlSample", absoluteUrl.take(180))
                },
            )
            // #endregion
            android.util.Log.w("downloadHttpUrlToFile", absoluteUrl, t)
            try {
                outFile.delete()
            } catch (_: Exception) {
            }
            try {
                partFile.delete()
            } catch (_: Exception) {
            }
            false
        }
    }
}

internal suspend fun downloadServerPipelineArtifacts(
    context: Context,
    taskId: String,
    onProgress: (Int, String) -> Unit,
): ServerPipelineResultBundle? {
    return try {
        downloadServerPipelineArtifactsImpl(context, taskId, onProgress)
    } catch (t: Throwable) {
        if (t is kotlinx.coroutines.CancellationException) throw t
        // #region agent log
        runCatching {
            agentDebugNdjson(
                context,
                "OOM",
                "ServerPipelineNetworking.kt:downloadServerPipelineArtifacts",
                "download_caught_throwable",
                JSONObject().apply {
                    put("taskId", taskId)
                    put("errClass", t.javaClass.name)
                    put("isOutOfMemoryError", t is OutOfMemoryError)
                    put("msg", (t.message ?: "").take(240))
                },
            )
            agentDebugHeapSnapshot(context, "catch_after_download_error")
        }
        // #endregion
        android.util.Log.e("downloadServerPipelineArtifacts", "아티팩트 다운로드 중 예기치 못한 오류", t)
        runCatching {
            AppWarningLog.record(
                context.applicationContext,
                "downloadServerPipelineArtifacts",
                t.message ?: t.javaClass.simpleName,
                t,
            )
        }
        null
    }
}

private suspend fun downloadServerPipelineArtifactsImpl(
    context: Context,
    taskId: String,
    onProgress: (Int, String) -> Unit,
): ServerPipelineResultBundle? {
    val plyDir = ModelLibraryPaths.plyDir(context)
    val finalDir = File(plyDir, "server_task_$taskId")
    val stagingDir = File(plyDir, "server_task_$taskId$SERVER_DOWNLOAD_STAGING_SUFFIX")
    try {
        stagingDir.deleteRecursively()
    } catch (_: Exception) {
    }
    stagingDir.mkdirs()
    // #region agent log
    agentDebugNdjson(
        context,
        "H5",
        "ServerPipelineNetworking.kt:downloadServerPipelineArtifactsImpl",
        "artifacts_flow_start",
        JSONObject().apply {
            put("taskId", taskId)
            put("server", getServerAddress(context))
            put("port", getServerPort(context))
            put("useHttps", getUseHttps(context))
        },
    )
    // #endregion
    // #region agent log
    agentDebugHeapSnapshot(context, "pre_fetch_results")
    // #endregion
    val json = fetchServerResultsJson(context, taskId)
    val filesArr: JSONArray? = json?.optJSONArray("files")
    // #region agent log
    agentDebugHeapSnapshot(
        context,
        "post_fetch_results",
        resultsJsonNull = json == null,
        resultsFilesLen = filesArr?.length() ?: -1,
    )
    // #endregion
    val staged = LinkedHashMap<String, File>()
    val progressLock = Any()
    fun safeProgress(p: Int, msg: String) {
        synchronized(progressLock) {
            onProgress(p.coerceIn(0, 100), msg)
        }
    }

    data class FileEntry(val key: String, val url: String, val filename: String)

    try {
        if (filesArr != null && filesArr.length() > 0) {
            val usedNames = HashSet<String>()
            val entries = buildList {
                for (i in 0 until filesArr.length()) {
                    val o = filesArr.optJSONObject(i) ?: continue
                    val key = o.optString("key").trim()
                    val url = o.optString("url").trim()
                    val rawName = o.optString("filename").ifBlank { "file_$i" }
                    if (key.isBlank() || url.isBlank()) continue
                    val artifactKey = normalizeMobileServerArtifactKey(key, rawName) ?: continue
                    val preferred = preferredServerPushArtifactName(
                        taskId,
                        artifactKey,
                        safeServerArtifactFilename(rawName, key, i),
                    )
                    val filename = uniqueServerArtifactFilename(
                        safeServerArtifactFilename(preferred, key, i),
                        usedNames,
                    )
                    add(FileEntry(artifactKey, url, filename))
                }
            }
            val total = entries.size
            if (filesArr.length() > total) {
                safeProgress(
                    95,
                    "모바일 표시용 결과만 다운로드 중... ($total / ${filesArr.length()}개)",
                )
            }
            for ((idx, e) in entries.withIndex()) {
                yield()
                val fileNum = idx + 1
                safeProgress(
                    (95 + (idx * 3 / maxOf(total, 1))).coerceIn(0, 98),
                    "다운로드 ($fileNum/$total): ${e.filename}",
                )
                val dest = File(stagingDir, e.filename)
                val ok = downloadHttpUrlToFile(context, e.url, dest) { read, cl ->
                    val mbRead = read / (1024L * 1024L)
                    val detail = if (cl > 0L) {
                        val mbTotal = cl / (1024L * 1024L)
                        "${mbRead} / ${mbTotal} MB"
                    } else {
                        "${mbRead} MB"
                    }
                    safeProgress(
                        (95 + (idx * 3 / maxOf(total, 1))).coerceIn(0, 98),
                        "($fileNum/$total) ${e.filename} · $detail",
                    )
                }
                if (ok) {
                    staged[e.key] = dest
                } else if (e.key == "ply") {
                    safeProgress(99, "필수 PLY 다운로드 실패")
                    return null
                }
                if (idx < entries.lastIndex) {
                    delay(BETWEEN_SERVER_ARTIFACT_DOWNLOAD_MS)
                    yield()
                }
            }
        }

        if (staged["ply"] == null) {
            yield()
            safeProgress(99, "PLY 다운로드 중...")
            downloadPlyResultToDirectory(context, taskId, stagingDir) { p, msg -> safeProgress(p, msg) }
                ?.let { staged["ply"] = it }
        }
        if (staged["ply"] == null) return null

        safeProgress(99, "다운로드 검증 중...")
        val published = publishDownloadedServerTask(stagingDir, finalDir)
        // #region agent log
        agentDebugNdjson(
            context,
            "H4",
            "ServerPipelineNetworking.kt:downloadServerPipelineArtifactsImpl",
            "publish_staging_to_final",
            JSONObject().apply {
                put("published", published)
                put(
                    "stagingFileCount",
                    stagingDir.listFiles()?.count { it.isFile } ?: -1,
                )
            },
        )
        // #endregion
        if (!published) return null

        try {
            finalDir.setLastModified(System.currentTimeMillis())
        } catch (_: Exception) {
        }

        val finalMap = staged.mapValues { (_, f) -> File(finalDir, f.name) }
            .filterValues { it.exists() && it.isFile }
        val plyFile = finalMap["ply"] ?: return null
        val bundle = ServerPipelineResultBundle(
            taskId = taskId,
            plyFile = plyFile,
            directory = finalDir,
            filesByKey = finalMap,
        )
        // #region agent log
        agentDebugHeapSnapshot(context, "bundle_ready_ok")
        // #endregion
        return bundle
    } finally {
        try {
            if (stagingDir.exists()) stagingDir.deleteRecursively()
        } catch (_: Exception) {
        }
    }
}

private fun safeServerArtifactFilename(raw: String, key: String, index: Int): String {
    val leaf = raw.trim().substringAfterLast('/').substringAfterLast('\\').trim()
    val fallback = if (key.isNotBlank()) "$key.bin" else "file_$index.bin"
    val cleaned = leaf.ifBlank { fallback }
        .replace(Regex("[\\r\\n\\t]"), "_")
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    return cleaned.take(160).ifBlank { fallback }
}

private fun uniqueServerArtifactFilename(filename: String, used: MutableSet<String>): String {
    if (used.add(filename)) return filename
    val dot = filename.lastIndexOf('.')
    val base = if (dot > 0) filename.substring(0, dot) else filename
    val ext = if (dot > 0) filename.substring(dot) else ""
    var n = 2
    while (true) {
        val candidate = "${base}_$n$ext"
        if (used.add(candidate)) return candidate
        n++
    }
}

private fun publishDownloadedServerTask(stagingDir: File, finalDir: File): Boolean {
    if (!stagingDir.isDirectory) return false
    return try {
        if (finalDir.exists()) finalDir.deleteRecursively()
        if (stagingDir.renameTo(finalDir)) return true
        finalDir.mkdirs()
        stagingDir.listFiles()?.forEach { src ->
            if (src.isFile && !src.name.endsWith(SERVER_DOWNLOAD_PART_SUFFIX)) {
                src.copyTo(File(finalDir, src.name), overwrite = true)
            }
        }
        finalDir.isDirectory && (finalDir.listFiles()?.any { it.isFile } == true)
    } catch (t: Throwable) {
        android.util.Log.e("downloadArtifacts", "결과 폴더 게시 실패", t)
        false
    }
}

private suspend fun downloadPlyResultToDirectory(
    context: Context,
    taskId: String,
    outDir: File,
    onDownloadProgress: ((progressPercent: Int, message: String) -> Unit)? = null,
): File? {
    val url = buildServerOrigin(context) + "$DOWNLOAD_ENDPOINT/$taskId?format=ply"
    val outFile = File(outDir, "result_${taskId}.ply")

    val maxAttempts = 3
    repeat(maxAttempts) { attempt ->
        yield()
        try {
            outFile.delete()
        } catch (_: Exception) {
        }
        val ok = downloadHttpUrlToFile(context, url, outFile) { read, cl ->
            if (cl > 0L) {
                val pct = (95 + (read * 4.0 / cl).toInt().coerceIn(0, 4)).coerceAtMost(99)
                val mbRead = read / (1024L * 1024L)
                val mbTotal = cl / (1024L * 1024L)
                onDownloadProgress?.invoke(pct, "PLY · $mbRead / $mbTotal MB")
            } else {
                val mbRead = read / (1024L * 1024L)
                onDownloadProgress?.invoke(97, "PLY · ${mbRead} MB 수신 중...")
            }
        }
        if (ok && outFile.exists() && outFile.length() > 0L) return outFile
        if (attempt < maxAttempts - 1) delay(5_000L * (attempt + 1))
    }
    return null
}

/**
 * `server_task_*` 디렉토리를 최신 [keepLatest]개만 남기고 오래된 것을 삭제합니다.
 * 현재 태스크 폴더는 절대 삭제하지 않습니다.
 */
internal fun pruneOldServerTaskDirs(
    context: Context,
    keepLatest: Int = 8,
    protectedTaskId: String? = null,
) {
    try {
        val plyDir = ModelLibraryPaths.plyDir(context)
        val protectedName = protectedTaskId?.let { "server_task_$it" }
        val taskDirs = plyDir.listFiles { f ->
            f.isDirectory &&
                f.name.startsWith("server_task_") &&
                !f.name.endsWith(SERVER_DOWNLOAD_STAGING_SUFFIX) &&
                f.name != protectedName
        } ?: return
        if (taskDirs.size <= keepLatest) return
        // 수정 시각 내림차순 정렬 → 오래된 것이 뒤에 위치
        val sorted = taskDirs.sortedByDescending { it.lastModified() }
        sorted.drop(keepLatest).forEach { dir ->
            try {
                dir.deleteRecursively()
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
}

internal suspend fun runServer3dgsAnalysisInBackground(
    context: Context,
    pending: Pending3dgsServerAutoSend
): Boolean {
    val imageBase64List = withContext(Dispatchers.IO) {
        val uris = pending.imageUris
        uris.mapNotNull { uri ->
            try {
                decodeBitmapWithMaxDimension(context, uri, 1280)
                    ?.let { ClaudeChatClient.bitmapToBase64ForLlm(it) }
            } catch (_: Exception) {
                null
            }
        }
    }
    val result = ClaudeChatClient.streamMobile3dGsAnalysisMessage(
        userText = pending.promptText,
        imageBase64List = imageBase64List,
        onDelta = { }
    )
    return when (result) {
        is ClaudeChatClient.ChatResult.Success -> {
            val threadId = UUID.randomUUID().toString()
            val title = "서버 3DGS 분석 " + SimpleDateFormat(
                "MM/dd HH:mm",
                Locale.getDefault()
            ).format(Date())
            ChatThreadStorage.save(
                context,
                ConversationThread(
                    id = threadId,
                    title = title,
                    modeName = "MOBILE_3DGS",
                    messages = listOf(
                        PersistedMessage(
                            pending.promptText.take(8000),
                            true,
                            pending.imageUris.map { it.toString() },
                            null
                        ),
                        PersistedMessage(result.text, false, emptyList(), null)
                    ),
                    updatedAt = System.currentTimeMillis()
                )
            )
            true
        }
        is ClaudeChatClient.ChatResult.Error -> false
    }
}

internal fun buildPoliceInsurance3dgsPayload(
    context: Context,
    bundle: ServerPipelineResultBundle,
    basePrompt: String = "첨부된 PLY·3DGS·JSON 분석 파일과 사고 현장 이미지를 바탕으로, 사고 현장 분석 보고서를 작성하세요. 보고서에 도식 등 시각 자료가 필요하면 matplotlib/PIL로 PNG를 생성한 뒤 document.add_picture로 삽입하고, 바로 이어 add_paragraph로 분석 텍스트를 작성하세요(모바일 PDF는 문자열만 추출하므로 그림 요지는 반드시 문단에 적습니다). 보고서는 표지(1페이지)·목차(2페이지)·본문(3페이지 이후) 구조로, 사고 현장 개요, 사고 형태 분석, 사고 원인 추론, 차량별 파손 부위 및 수리 견적, 종합 수리 견적 요약, 법적 면책 정보를 포함하세요. 모든 데이터는 add_paragraph()로만 표현하고 table.cell과 앱 전용 차트 표식은 사용하지 마세요. 섹션마다 page_break로 분리하세요. 한국어 python-docx 스크립트(단일 ```python 블록)로만 출력하세요.",
    galleryImageUris: List<Uri> = emptyList(),
): Pair<String, List<Uri>> {
    val sb = StringBuilder(basePrompt)
    sb.append("\n\n--- 서버 결과 메타 ---\n")
    sb.append("task_id: ").append(bundle.taskId).append('\n')
    sb.append("다운로드된 파일 키: ").append(bundle.filesByKey.keys.sorted().joinToString(", ")).append('\n')

    val ply = bundle.plyFile
    if (ply.exists()) {
        sb.append("\n[PLY] ").append(ply.name).append(" (")
            .append(ply.length() / 1024).append(" KB)\n")
            .append(ply.absolutePath).append('\n')
    }
    bundle.filesByKey["glb"]?.takeIf { it.exists() }?.let { glb ->
        sb.append("\n[GLB] ").append(glb.name).append(" (").append(glb.length() / 1024)
            .append(" KB)\n").append(glb.absolutePath).append('\n')
    }

    val textKeys = listOf(
        "analysis_json" to "analysis_result.json",
        /** 서버는 [quality_json] 단일 파일로 교체 (txt/png 제거). 레거시 번들용 [quality_txt] 유지 */
        "quality_json" to "quality_report.json",
        "quality_txt" to "quality_report.txt",
        "vehicle_csv" to "vehicle_analysis.csv",
        "contact_csv" to "contact_analysis.csv",
        "contact_points_csv" to "contact_candidate_points.csv",
    )
    val maxPerFile = 24_000
    val maxTotal = 120_000
    for ((key, label) in textKeys) {
        val f = bundle.filesByKey[key] ?: continue
        if (sb.length >= maxTotal) break
        val raw = try {
            f.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            continue
        }
        val enriched = if (key == "quality_json") {
            val summary = parsePointCloudQualityReportJson(f)?.let { formatPointCloudQualityReportKorean(it) }
            if (!summary.isNullOrBlank()) "$summary\n\n--- 원본 JSON ---\n$raw" else raw
        } else raw
        val chunk = if (enriched.length > maxPerFile) {
            enriched.take(maxPerFile) + "\n...(이하 생략, 원문 ${enriched.length}자)..."
        } else enriched
        sb.append("\n--- ").append(label).append(" (").append(f.name).append(") ---\n")
            .append(chunk).append('\n')
    }
    var text = sb.toString()
    if (text.length > maxTotal) {
        text = text.take(maxTotal) + "\n...(전체 본문 길이 제한으로 잘림)"
    }

    val imageExts = setOf("png", "jpg", "jpeg", "webp")
    val uris = ArrayList<Uri>()

    if (galleryImageUris.isNotEmpty()) {
        uris.addAll(galleryImageUris)
    }

    val preferredOrder = listOf("topview", "sideview", "quality_png", "analysis_png")
    val seen = HashSet<String>()
    fun addFile(f: File) {
        val path = f.absolutePath
        if (path in seen || !f.exists() || !f.isFile) return
        if (f.extension.lowercase() !in imageExts) return
        val u = uriToShareableContentUri(context, Uri.fromFile(f)) ?: Uri.fromFile(f)
        seen.add(path)
        uris.add(u)
    }
    for (k in preferredOrder) {
        bundle.filesByKey[k]?.let(::addFile)
    }
    for (f in bundle.filesByKey.values) {
        addFile(f)
    }
    return text to uris
}

internal suspend fun sam2RemoveBackground(
    context: Context,
    imageUri: Uri,
    prompt: String,
    itemIndex: Int = 0,
    itemTotal: Int = 1,
    onProgress: suspend (percent: Int, message: String) -> Unit = { _, _ -> },
): Uri? = withContext(Dispatchers.IO) {
    val itemLabel = if (itemTotal > 1) " (${itemIndex + 1}/$itemTotal)" else ""
    try {
        onProgress(5, "이미지 로드 중...$itemLabel")

        val serverAddress = getServerAddress(context)
        val useHttps = getUseHttps(context)
        val protocol = if (useHttps) "https" else "http"
        val url = "$protocol://$serverAddress:$SAM3_DEFAULT_PORT$SAM3_BG_REMOVE_ENDPOINT"

        val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: return@withContext null

        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
        val extension = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }

        onProgress(20, "SAM2 서버로 전송 중...$itemLabel")

        val client = createOkHttpClient(useHttps).newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(240, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "input.$extension",
                imageBytes.toRequestBody(mimeType.toMediaType())
            )
            .addFormDataPart("prompt", prompt)
            .build()

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        onProgress(35, "SAM2 객체 감지 및 세그멘테이션 중...$itemLabel")

        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            android.util.Log.e("SAM2", "서버 오류 ${response.code}: $errBody")
            response.close()
            return@withContext null
        }

        onProgress(85, "결과 수신 중...$itemLabel")

        val pngBytes = response.body?.bytes()
        response.close()
        if (pngBytes == null || pngBytes.isEmpty()) return@withContext null

        onProgress(93, "앱 라이브러리 저장 중...$itemLabel")

        val outputDir = context.getExternalFilesDir(null) ?: return@withContext null
        outputDir.mkdirs()
        val outFile = File(outputDir, "sam2_bg_removed_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { it.write(pngBytes) }

        onProgress(98, "완료$itemLabel")
        Uri.fromFile(outFile)
    } catch (e: Exception) {
        android.util.Log.e("SAM2", "SAM2 서버 통신 실패", e)
        null
    }
}

internal suspend fun downloadPlyResult(
    context: Context,
    taskId: String,
    onDownloadProgress: ((progressPercent: Int, message: String) -> Unit)? = null,
): File? {
    val url = buildServerOrigin(context) + "$DOWNLOAD_ENDPOINT/$taskId?format=ply"
    val modelsDir = ModelLibraryPaths.plyDir(context)
    val outFile = File(modelsDir, "3d_model_$taskId.ply")

    val maxAttempts = 3
    repeat(maxAttempts) { attempt ->
        yield()
        try {
            outFile.delete()
        } catch (_: Exception) {
        }
        val ok = downloadHttpUrlToFile(context, url, outFile) { read, cl ->
            if (cl > 0L) {
                val pct = (95 + (read * 4.0 / cl).toInt().coerceIn(0, 4)).coerceAtMost(99)
                val mbRead = read / (1024L * 1024L)
                val mbTotal = cl / (1024L * 1024L)
                onDownloadProgress?.invoke(pct, "PLY · $mbRead / $mbTotal MB")
            } else {
                val mbRead = read / (1024L * 1024L)
                onDownloadProgress?.invoke(97, "PLY · ${mbRead} MB 수신 중…")
            }
        }
        if (ok && outFile.exists() && outFile.length() > 0L) return outFile
        if (attempt < maxAttempts - 1) delay(5_000L * (attempt + 1))
    }
    return null
}

/**
 * 서버 연결 테스트 (GET [SERVER_CONNECTIVITY_GET_PATH]).
 */
internal suspend fun testServerConnection(
    @Suppress("UNUSED_PARAMETER") context: Context,
    serverAddress: String,
    serverPort: Int,
    useHttps: Boolean
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val client = createOkHttpClient(useHttps).newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val url =
                buildServerOriginFromParts(serverAddress.trim(), serverPort, useHttps) + SERVER_CONNECTIVITY_GET_PATH
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val success = response.code in 200..499
            response.close()

            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
