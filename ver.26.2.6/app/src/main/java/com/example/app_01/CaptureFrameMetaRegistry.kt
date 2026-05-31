package com.example.app_01

import android.graphics.BitmapFactory
import android.os.SystemClock
import java.io.File
import java.util.Collections

/** 촬영 직후 기록되는 프레임 메타 — ARCore poses.json과 1:1로 묶을 때 사용 */
data class CaptureFrameMeta(
    val fileName: String,
    /** CameraX ImageProxy.imageInfo.timestamp 또는 셔터 시각(ns, monotonic). */
    val imageTimestampNs: Long,
    val captureWidth: Int,
    val captureHeight: Int,
    /** 동영상 녹화 중 촬영 시 녹화 시작 대비 오프셋(ns). 단일·연속 사진은 null. */
    val videoOffsetNs: Long? = null,
)

/** 저장된 JPEG 실제 픽셀 크기 (EXIF 회전 반영 전 파일 기준). */
fun readJpegFileDimensions(file: File): Pair<Int, Int>? {
    if (!file.isFile || file.length() <= 0L) return null
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        if (opts.outWidth > 0 && opts.outHeight > 0) {
            opts.outWidth to opts.outHeight
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

object CaptureFrameMetaRegistry {
    private val byFileName = Collections.synchronizedMap(LinkedHashMap<String, CaptureFrameMeta>())

    fun record(meta: CaptureFrameMeta) {
        byFileName[meta.fileName] = meta
    }

    fun get(fileName: String): CaptureFrameMeta? = byFileName[fileName]

    fun orderedMetaFor(fileNames: List<String>): List<CaptureFrameMeta> =
        fileNames.mapNotNull { byFileName[it] }

    fun remove(fileName: String) {
        byFileName.remove(fileName)
    }

    fun clear(fileNames: Collection<String>) {
        fileNames.forEach { byFileName.remove(it) }
    }
}

/** 동영상 녹화 구간의 시작 시각 — 데이터셋 스크린샷·타임라인 포즈 정렬용 */
object VideoRecordingSessionRegistry {
    @Volatile
    var recordingStartTimestampNs: Long? = null
        private set

    fun markStart(timestampNs: Long = SystemClock.elapsedRealtimeNanos()) {
        recordingStartTimestampNs = timestampNs
    }

    fun markStop() {
        recordingStartTimestampNs = null
    }

    fun offsetNsAtCapture(captureTimestampNs: Long): Long? {
        val start = recordingStartTimestampNs ?: return null
        return (captureTimestampNs - start).coerceAtLeast(0L)
    }
}

internal fun captureResolutionForContext(context: android.content.Context): ResolutionPreset =
    ResolutionPreset.forArCoreEnabled(CameraArCorePrefs.isArCoreMetaEnabled(context))
