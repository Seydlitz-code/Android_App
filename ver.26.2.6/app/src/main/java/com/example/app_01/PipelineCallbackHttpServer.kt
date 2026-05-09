package com.example.app_01

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.channels.Channel
import java.io.File
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
            val partFiles = LinkedHashMap<String, File>()
            for ((field, tmpPath) in tmpPaths) {
                if (
                    field == "task_id" || field == "status" || field == "summary" ||
                    field == "message" || field == "event"
                ) {
                    continue
                }
                val f = File(tmpPath)
                if (f.isFile) partFiles[field] = f
            }
            val statusUpper = status.uppercase(Locale.US)
            val resolvedEvent = when {
                eventRaw.isNotEmpty() -> eventRaw
                statusUpper == "FAILED" -> PipelineCallbackEvents.PIPELINE_FAILED
                partFiles.isNotEmpty() -> PipelineCallbackEvents.PIPELINE_RESULT_FILES
                else -> PipelineCallbackEvents.LEGACY_UNKNOWN
            }
            val ev = PipelineCallbackEvent(
                ordinal = seq.incrementAndGet(),
                event = resolvedEvent,
                taskId = taskId,
                status = status,
                summaryJson = summary,
                failureMessage = failureMessage,
                partFiles = partFiles,
            )
            outbound.trySend(ev)
            newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                MIME_PLAINTEXT,
                e.message ?: "bad request",
            )
        }
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
