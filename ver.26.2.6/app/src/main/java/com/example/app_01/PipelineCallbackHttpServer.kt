package com.example.app_01

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * 서버 `event` 폼 필드와 동일한 문자열(단계·종류 구분).
 * [main.py] 에서 동일 상수를 보내도록 맞춥니다.
 */
object PipelineCallbackEvents {
    /** 파이프라인 성공 후 결과 파일·summary multipart */
    const val PIPELINE_RESULT_FILES = "pipeline_result_files"
    /** 파이프라인 실패 알림 (task_id, status, message 등 텍스트만) */
    const val PIPELINE_FAILED = "pipeline_failed"
    /** 서버가 아직 event를 안 보낸 경우(하위 호환) */
    const val LEGACY_UNKNOWN = "legacy_unknown"
}

/**
 * [main.py] `_push_results_to_mobile` 및 실패 시 POST 한 건당 하나.
 * [event]로 콜백 종류를 구분합니다.
 */
data class PipelineCallbackEvent(
    val ordinal: Int,
    /** 서버와 약속한 이벤트 식별자 ([PipelineCallbackEvents]) */
    val event: String,
    val taskId: String,
    val status: String,
    val summaryJson: String?,
    val failureMessage: String?,
    val partFiles: Map<String, File>,
)

internal class PipelineCallbackHttpServer(
    port: Int,
    private val outbound: Channel<PipelineCallbackEvent>,
) : NanoHTTPD(port) {

    private val seq = AtomicInteger(0)

    /**
     * NanoHTTPD는 serve() 리턴 직후 TempFileManager.clear()로 임시 파일을 삭제합니다.
     * 따라서 serve() 안에서 즉시 이 디렉터리로 복사해 폴링 루프가 안전하게 접근할 수 있게 합니다.
     */
    private val pushPartsDir: File by lazy {
        File(System.getProperty("java.io.tmpdir") ?: "/tmp", "pp_cb_parts").also { it.mkdirs() }
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(
                Response.Status.METHOD_NOT_ALLOWED,
                MIME_PLAINTEXT,
                "POST only",
            )
        }
        val path = session.uri.trim('/')
        if (path != "pipeline/callback") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "not found")
        }

        // ── OOM 방어 ──────────────────────────────────────────────────────────
        // NanoHTTPD 2.3.1 은 parseBody() 호출 시 multipart 전체를 ByteArray 로
        // 메모리에 올립니다. PLY·GLB 등 대용량 파일이 포함된 경우 OOM 크래시가
        // 발생합니다. Content-Length > MAX_PUSH_BODY_BYTES 이면 본문을 조각별로
        // 읽어 버린 뒤 200 OK 반환 → 폴링 + HTTP 다운로드 경로로 자동 폴백됩니다.
        val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (contentLength > MAX_PUSH_BODY_BYTES) {
            android.util.Log.i(
                "PipelineCallback",
                "push body too large (${contentLength / 1024 / 1024} MB) — skipping file parsing, will use download path",
            )
            drainStreamQuietly(session.inputStream, contentLength)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
        }

        val tmpPaths = HashMap<String, String>()
        return try {
            session.parseBody(tmpPaths)
            val parms = session.parms
            val taskId = parms["task_id"]?.trim().orEmpty()
            if (taskId.isEmpty()) {
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    MIME_PLAINTEXT,
                    "task_id required",
                )
            }
            val status = parms["status"]?.trim().orEmpty().ifEmpty { "UNKNOWN" }
            val summary = parms["summary"]?.trim()?.takeIf { it.isNotEmpty() }
            val failureMessage = parms["message"]?.trim()?.takeIf { it.isNotEmpty() }
            val eventRaw = parms["event"]?.trim().orEmpty()

            // ── 임시 파일 즉시 영속 복사 ──────────────────────────────────────
            // serve() 리턴 직후 NanoHTTPD 가 tmpPaths 의 파일을 삭제합니다.
            // 폴링 루프(drainPushEvents)가 나중에 접근하려면 여기서 먼저 복사해야 합니다.
            val ord = seq.incrementAndGet()
            val partFiles = LinkedHashMap<String, File>()
            for ((field, tmpPath) in tmpPaths) {
                if (field in SKIP_FIELDS) continue
                val src = File(tmpPath)
                if (!src.isFile) continue
                val ext = src.extension.ifBlank { "bin" }
                val dest = File(pushPartsDir, "${taskId}_${ord}_$field.$ext")
                try {
                    src.copyTo(dest, overwrite = true)
                    if (dest.isFile && dest.length() > 0L) partFiles[field] = dest
                } catch (_: Exception) {
                    // 복사 실패 시 해당 파일만 건너뜀
                }
            }

            val statusUpper = status.uppercase(Locale.US)
            val resolvedEvent = when {
                eventRaw.isNotEmpty() -> eventRaw
                statusUpper == "FAILED" -> PipelineCallbackEvents.PIPELINE_FAILED
                partFiles.isNotEmpty() -> PipelineCallbackEvents.PIPELINE_RESULT_FILES
                else -> PipelineCallbackEvents.LEGACY_UNKNOWN
            }
            val ev = PipelineCallbackEvent(
                ordinal = ord,
                event = resolvedEvent,
                taskId = taskId,
                status = status,
                summaryJson = summary,
                failureMessage = failureMessage,
                partFiles = partFiles,
            )
            outbound.trySend(ev)
            newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
        } catch (t: Throwable) {
            // Exception 뿐 아니라 OutOfMemoryError 등 Error 도 잡아서 앱 크래시 방지.
            // 200 OK 반환 → 서버는 성공으로 인식하고 앱은 다운로드 경로로 폴백합니다.
            android.util.Log.e("PipelineCallback", "serve error — falling back to download path", t)
            newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
        }
    }

    /**
     * 소켓 연결을 깔끔하게 유지하기 위해 [length] 바이트만큼 읽어 버립니다.
     * 고정 크기 버퍼를 사용하므로 대용량 본문에서도 OOM 이 발생하지 않습니다.
     */
    private fun drainStreamQuietly(input: InputStream, length: Long) {
        try {
            val buf = ByteArray(32 * 1024)
            var remaining = length
            while (remaining > 0) {
                val n = input.read(buf, 0, minOf(remaining, buf.size.toLong()).toInt())
                if (n <= 0) break
                remaining -= n
            }
        } catch (_: Exception) {
            // 드레인 실패는 무시 — 연결이 이미 닫혔을 수 있음
        }
    }

    companion object {
        /**
         * 이 크기를 초과하는 본문은 파일 파싱을 건너뜁니다 (OOM 방지).
         * 서버가 PLY·GLB 등 대용량 파일을 포함해 전송하는 경우 해당 콜백은
         * 무시되고 앱은 HTTP GET 다운로드 경로로 파일을 받아옵니다.
         */
        const val MAX_PUSH_BODY_BYTES = 20L * 1024L * 1024L  // 20 MB

        private val SKIP_FIELDS = setOf("task_id", "status", "summary", "message", "event")
    }
}

/**
 * @return 서버 인스턴스와 실제 바인딩 포트
 */
internal fun startPipelineCallbackServer(
    outbound: Channel<PipelineCallbackEvent>,
): Pair<PipelineCallbackHttpServer, Int>? {
    for (p in 28880..28899) {
        try {
            val s = PipelineCallbackHttpServer(p, outbound)
            s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            return s to p
        } catch (_: Exception) {
            // 다음 포트 시도
        }
    }
    return null
}

/** 동일 LAN에서 서버가 접근할 수 있는 기기 IPv4 (콜백 URL용). */
internal fun getDeviceLanIpv4OrNull(): String? {
    return try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val ni = en.nextElement()
            if (!ni.isUp || ni.isLoopback) continue
            val ads = ni.inetAddresses
            while (ads.hasMoreElements()) {
                val a = ads.nextElement()
                if (a.isLoopbackAddress || a !is Inet4Address) continue
                val h = a.hostAddress ?: continue
                if (h.startsWith("169.254.")) continue
                return h
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

internal fun formatPipelineCallbackHint(lanIp: String?, port: Int): String {
    if (lanIp.isNullOrBlank()) return "콜백 URL을 만들 수 없습니다(IPv4 없음). 폴링·다운로드만 사용합니다."
    return String.format(
        Locale.US,
        "콜백: http://%s:%d/pipeline/callback (서버와 동일 Wi-Fi 필요)",
        lanIp,
        port,
    )
}
