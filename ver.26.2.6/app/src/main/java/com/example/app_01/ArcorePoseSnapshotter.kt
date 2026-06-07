package com.example.app_01

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.LinkedHashMap

/**
 * ARCore 포즈 스냅샷 기능 — 비활성화됨.
 * 모든 함수는 null / empty 를 반환하며 실제 ARCore 세션을 열지 않습니다.
 */
object ArcorePoseSnapshotter {

    fun availabilityInstalled(context: Context): Boolean = false

    fun capturePhotoMetadataOrNull(context: Context, imageFileName: String): JSONObject? = null

    fun captureFullVideoTimelineOrNull(
        context: Context,
        videoFile: File,
        recordingStartTimestampNs: Long? = null,
        captureWidth: Int = 0,
        captureHeight: Int = 0,
    ): JSONObject? = null

    fun captureDatasetScreenshotsBestEffort(
        context: Context,
        imageFileNamesInOrder: List<String>,
    ): LinkedHashMap<String, JSONObject> = linkedMapOf()

    fun captureDatasetScreenshotsBestEffort(
        context: Context,
        orderedMeta: List<CaptureFrameMeta>,
        imageFileNamesInOrder: List<String> = orderedMeta.map { it.fileName },
    ): LinkedHashMap<String, JSONObject> = linkedMapOf()

    fun matchDatasetImagesToTimeline(
        timelineRoot: JSONObject,
        datasetMeta: List<CaptureFrameMeta>,
    ): LinkedHashMap<String, JSONObject> = linkedMapOf()
}
