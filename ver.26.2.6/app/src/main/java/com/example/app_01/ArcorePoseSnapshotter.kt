package com.example.app_01

import android.content.Context
import android.media.MediaMetadataRetriever
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.SystemClock
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

/**
 * CameraX와 동시에 후면 카메라를 잡고 있을 수 없어, 촬영 파일이 준비된 뒤 잠시 세션을 연다.
 *
 * poses.json 전송 시:
 * - fx/fy/cx/cy·imageWidth/imageHeight → [CaptureFrameMeta]의 **저장 JPEG 실제 해상도**로 스케일
 * - timestampNs → 촬영 시각([CaptureFrameMeta.imageTimestampNs]) 기준 (인덱스·타임라인 정렬)
 * - trackingState == TRACKING 인 프레임만 포함
 */
object ArcorePoseSnapshotter {

    private const val MAX_DATASET_ARCORE_FRAMES = 500
    /** 단일 사진 직후 ARCore — 이 이내면 실시간 캡처로 간주 */
    private const val SINGLE_PHOTO_MAX_DELTA_NS = 5_000_000_000L

    private data class ScaledIntrinsics(
        val fx: Double,
        val fy: Double,
        val cx: Double,
        val cy: Double,
        val imageWidth: Int,
        val imageHeight: Int,
    )

    private data class FrameBuildConfig(
        val captureWidth: Int,
        val captureHeight: Int,
        val imageTimestampNs: Long?,
        val videoTimeSec: Double? = null,
        val datasetScreenshotIndex: Int? = null,
        val timestampMatchMethod: String = "index_pairing",
    )

    /** ARCore intrinsics → 실제 저장 JPEG 해상도 기준으로 선형 스케일 */
    private fun scaleIntrinsicsToCaptureResolution(
        focal: FloatArray,
        principal: FloatArray,
        arcoreWidth: Int,
        arcoreHeight: Int,
        captureWidth: Int,
        captureHeight: Int,
    ): ScaledIntrinsics {
        if (arcoreWidth <= 0 || arcoreHeight <= 0) {
            return ScaledIntrinsics(
                fx = focal[0].toDouble(),
                fy = focal[1].toDouble(),
                cx = principal[0].toDouble(),
                cy = principal[1].toDouble(),
                imageWidth = captureWidth,
                imageHeight = captureHeight,
            )
        }
        val scaleX = captureWidth.toDouble() / arcoreWidth.toDouble()
        val scaleY = captureHeight.toDouble() / arcoreHeight.toDouble()
        return ScaledIntrinsics(
            fx = focal[0] * scaleX,
            fy = focal[1] * scaleY,
            cx = principal[0] * scaleX,
            cy = principal[1] * scaleY,
            imageWidth = captureWidth,
            imageHeight = captureHeight,
        )
    }

    private fun configFromMeta(meta: CaptureFrameMeta): FrameBuildConfig =
        FrameBuildConfig(
            captureWidth = meta.captureWidth,
            captureHeight = meta.captureHeight,
            imageTimestampNs = meta.imageTimestampNs,
            timestampMatchMethod = if (meta.videoOffsetNs != null) "video_offset" else "index_pairing",
        )

    /**
     * TRACKING 상태 프레임만 반환. PAUSED/STOPPED 는 버리고 재시도한다.
     */
    private fun waitForTrackingFrame(
        session: Session,
        maxAttempts: Int = 60,
        deadlineMs: Long = 2_500L,
    ): Frame? {
        val deadline = SystemClock.elapsedRealtime() + deadlineMs
        var attempts = 0
        while (attempts < maxAttempts && SystemClock.elapsedRealtime() < deadline) {
            val frame = session.update()
            if (frame.camera.trackingState == TrackingState.TRACKING) {
                return frame
            }
            attempts++
            try {
                Thread.sleep(20L)
            } catch (_: InterruptedException) {
                break
            }
        }
        return null
    }

    private fun rescaleIntrinsicsInJson(
        frameJo: JSONObject,
        targetWidth: Int,
        targetHeight: Int,
    ) {
        val srcW = frameJo.optInt("imageWidth", targetWidth)
        val srcH = frameJo.optInt("imageHeight", targetHeight)
        if (srcW <= 0 || srcH <= 0 || (srcW == targetWidth && srcH == targetHeight)) {
            frameJo.put("imageWidth", targetWidth)
            frameJo.put("imageHeight", targetHeight)
            return
        }
        val sx = targetWidth.toDouble() / srcW.toDouble()
        val sy = targetHeight.toDouble() / srcH.toDouble()
        frameJo.put("fx", frameJo.optDouble("fx") * sx)
        frameJo.put("fy", frameJo.optDouble("fy") * sy)
        frameJo.put("cx", frameJo.optDouble("cx") * sx)
        frameJo.put("cy", frameJo.optDouble("cy") * sy)
        frameJo.put("imageWidth", targetWidth)
        frameJo.put("imageHeight", targetHeight)
    }

    /** 포즈·Intrinsics 스냅샷 전용: 평면/깊이/조명 추정 등 부가 파이프라인을 끄고 CPU·GPU 부하를 줄인다. */
    private fun configForPoseSnapshotOnly(session: Session): Config =
        Config(session).apply {
            planeFindingMode = Config.PlaneFindingMode.DISABLED
            lightEstimationMode = Config.LightEstimationMode.DISABLED
            depthMode = Config.DepthMode.DISABLED
            instantPlacementMode = Config.InstantPlacementMode.DISABLED
        }

    fun availabilityInstalled(context: Context): Boolean {
        return try {
            ArCoreApk.getInstance().checkAvailability(context.applicationContext) ==
                ArCoreApk.Availability.SUPPORTED_INSTALLED
        } catch (_: Exception) {
            false
        }
    }

    fun capturePhotoMetadataOrNull(context: Context, imageFileName: String): JSONObject? {
        if (!availabilityInstalled(context)) return null
        val meta = CaptureFrameMetaRegistry.get(imageFileName)
        val buildConfig = if (meta != null) {
            configFromMeta(meta)
        } else {
            val preset = captureResolutionForContext(context)
            FrameBuildConfig(
                captureWidth = preset.width,
                captureHeight = preset.height,
                imageTimestampNs = null,
                timestampMatchMethod = "single_photo",
            )
        }

        return runArCoreSession(context) { session ->
            val frame = waitForTrackingFrame(session) ?: return@runArCoreSession null
            JSONObject().put(
                "frames",
                JSONArray().put(
                    frameJsonObject(
                        frame,
                        imageFileName,
                        buildConfig.copy(
                            imageTimestampNs = buildConfig.imageTimestampNs
                                ?: meta?.imageTimestampNs
                                ?: frame.timestamp,
                            timestampMatchMethod = "single_photo",
                        ),
                        frameIndex = null,
                    ),
                ),
            )
        }
    }

    /**
     * 동영상 전체: 재생 길이 구간에 맞춰 ARCore를 샘플링하고,
     * [recordingStartTimestampNs] + frameIndex × interval 로 이미지 타임라인과 정렬한다.
     */
    fun captureFullVideoTimelineOrNull(
        context: Context,
        videoFile: File,
        recordingStartTimestampNs: Long? = VideoRecordingSessionRegistry.recordingStartTimestampNs,
        captureWidth: Int = captureResolutionForContext(context).width,
        captureHeight: Int = captureResolutionForContext(context).height,
    ): JSONObject? {
        if (!availabilityInstalled(context)) return null
        if (!videoFile.isFile) return null

        val durationMs = try {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(videoFile.absolutePath)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) {
            0L
        }

        val targetWallMs = durationMs.coerceIn(100L, 600_000L)
        val intervalMs = 33L
        val maxFrames = 1200
        val timelineStartNs = recordingStartTimestampNs
            ?: (SystemClock.elapsedRealtimeNanos() - targetWallMs * 1_000_000L)

        return runArCoreSession(context) { session ->
            waitForTrackingFrame(session, maxAttempts = 48, deadlineMs = 1_200L)

            val frames = JSONArray()
            val wallStart = SystemClock.elapsedRealtime()
            var idx = 0
            while (SystemClock.elapsedRealtime() - wallStart < targetWallMs && idx < maxFrames) {
                val arFrame = waitForTrackingFrame(session, maxAttempts = 8, deadlineMs = 400L)
                    ?: break
                val videoTimeSec = (idx * intervalMs) / 1000.0
                val imageTimestampNs = timelineStartNs + idx * intervalMs * 1_000_000L
                frames.put(
                    frameJsonObject(
                        arFrame,
                        videoFile.name,
                        FrameBuildConfig(
                            captureWidth = captureWidth,
                            captureHeight = captureHeight,
                            imageTimestampNs = imageTimestampNs,
                            videoTimeSec = videoTimeSec,
                            timestampMatchMethod = "video_timeline",
                        ),
                        frameIndex = idx,
                    ),
                )
                idx++
                try {
                    Thread.sleep(intervalMs)
                } catch (_: InterruptedException) {
                    break
                }
            }
            if (frames.length() == 0) {
                return@runArCoreSession null
            }
            JSONObject()
                .put("mediaType", "video_full_timeline")
                .put("videoFileName", videoFile.name)
                .put("durationMs", durationMs)
                .put("sampleIntervalMs", intervalMs)
                .put("recordingStartTimestampNs", timelineStartNs)
                .put("captureWidth", captureWidth)
                .put("captureHeight", captureHeight)
                .put(
                    "captureNoteKo",
                    "녹화 시작 시각 기준으로 videoTimeSec·timestampNs를 정렬했습니다. " +
                        "포즈는 녹화 종료 후 ARCore 세션에서 샘플링되며, intrinsics는 실제 촬영 해상도로 스케일됩니다.",
                )
                .put("frames", frames)
        }
    }

    private const val MAX_INDIVIDUAL_FALLBACK_FRAMES = 80

    /**
     * 촬영 순서대로 ARCore 프레임을 1:1 샘플링하고 [orderedMeta]의 imageTimestampNs와 매칭한다.
     */
    fun captureDatasetScreenshotsBestEffort(
        context: Context,
        imageFileNamesInOrder: List<String>,
    ): LinkedHashMap<String, JSONObject> {
        val orderedMeta = CaptureFrameMetaRegistry.orderedMetaFor(imageFileNamesInOrder)
        return captureDatasetScreenshotsBestEffort(context, orderedMeta, imageFileNamesInOrder)
    }

    fun captureDatasetScreenshotsBestEffort(
        context: Context,
        orderedMeta: List<CaptureFrameMeta>,
        imageFileNamesInOrder: List<String> = orderedMeta.map { it.fileName },
    ): LinkedHashMap<String, JSONObject> {
        val result = LinkedHashMap<String, JSONObject>()
        if (!availabilityInstalled(context) || imageFileNamesInOrder.isEmpty()) {
            return result
        }

        val names = if (imageFileNamesInOrder.size > MAX_DATASET_ARCORE_FRAMES) {
            imageFileNamesInOrder.take(MAX_DATASET_ARCORE_FRAMES)
        } else {
            imageFileNamesInOrder
        }
        val metaByName = orderedMeta.associateBy { it.fileName }

        val fromBatch = runDatasetBatchSession(context, names, metaByName)
        for ((k, v) in fromBatch) {
            result[k] = v
        }

        val missing = names.filter { !result.containsKey(it) }
        if (missing.isEmpty()) return result

        val limit = minOf(missing.size, MAX_INDIVIDUAL_FALLBACK_FRAMES)
        for (i in 0 until limit) {
            val fileName = missing[i]
            val idx = names.indexOf(fileName)
            val meta = metaByName[fileName]
            captureSingleDatasetFrameInNewSession(context, fileName, idx.coerceAtLeast(0), meta)?.let {
                result[fileName] = it
            }
            try {
                Thread.sleep(48L)
            } catch (_: InterruptedException) {
            }
        }
        return result
    }

    /**
     * 동영상 타임라인 JSON에서 각 데이터셋 이미지에 가장 가까운 포즈를 찾아 반환한다.
     */
    fun matchDatasetImagesToTimeline(
        timelineRoot: JSONObject,
        datasetMeta: List<CaptureFrameMeta>,
    ): LinkedHashMap<String, JSONObject> {
        val framesArr = timelineRoot.optJSONArray("frames") ?: return linkedMapOf()
        if (framesArr.length() == 0 || datasetMeta.isEmpty()) return linkedMapOf()

        val sampleIntervalMs = timelineRoot.optLong("sampleIntervalMs", 33L).coerceAtLeast(1L)
        val sampleIntervalNs = sampleIntervalMs * 1_000_000L
        val recordingStartNs = timelineRoot.optLong("recordingStartTimestampNs", -1L)

        val out = LinkedHashMap<String, JSONObject>()
        for ((index, meta) in datasetMeta.withIndex()) {
            val timelineIdx = when {
                meta.videoOffsetNs != null && sampleIntervalNs > 0L ->
                    (meta.videoOffsetNs / sampleIntervalNs).toInt()
                recordingStartNs >= 0L && sampleIntervalNs > 0L ->
                    ((meta.imageTimestampNs - recordingStartNs) / sampleIntervalNs).toInt()
                else -> index
            }.coerceIn(0, framesArr.length() - 1)

            val sourceJo = framesArr.optJSONObject(timelineIdx) ?: continue
            if (sourceJo.optString("trackingState") != TrackingState.TRACKING.name) continue

            val matched = JSONObject(sourceJo.toString()).apply {
                put("filename", meta.fileName)
                put("imageTimestampNs", meta.imageTimestampNs)
                put("timestampNs", meta.imageTimestampNs)
                put("timelineSampleIndex", timelineIdx)
                put("timestampMatchDeltaNs", 0L)
                put("timestampMatchMethod", "video_timeline_index")
                meta.videoOffsetNs?.let { put("videoOffsetNs", it) }
                put("datasetScreenshotIndex", index)
            }
            rescaleIntrinsicsInJson(matched, meta.captureWidth, meta.captureHeight)
            out[meta.fileName] = matched
        }
        return out
    }

    private fun runDatasetBatchSession(
        context: Context,
        names: List<String>,
        metaByName: Map<String, CaptureFrameMeta>,
    ): LinkedHashMap<String, JSONObject> {
        val batch = runArCoreSession(context) { session ->
            val out = LinkedHashMap<String, JSONObject>()
            waitForTrackingFrame(session, maxAttempts = 48, deadlineMs = 1_200L)
            names.forEachIndexed { index, fileName ->
                try {
                    val arFrame = waitForTrackingFrame(session, maxAttempts = 40, deadlineMs = 1_500L)
                        ?: return@forEachIndexed
                    val meta = metaByName[fileName]
                    val buildConfig = if (meta != null) {
                        FrameBuildConfig(
                            captureWidth = meta.captureWidth,
                            captureHeight = meta.captureHeight,
                            imageTimestampNs = meta.imageTimestampNs,
                            videoTimeSec = meta.videoOffsetNs?.let { it / 1_000_000_000.0 },
                            datasetScreenshotIndex = index,
                            timestampMatchMethod = if (meta.videoOffsetNs != null) {
                                "video_offset_index"
                            } else {
                                "index_pairing"
                            },
                        )
                    } else {
                        val preset = captureResolutionForContext(context)
                        FrameBuildConfig(
                            captureWidth = preset.width,
                            captureHeight = preset.height,
                            imageTimestampNs = null,
                            datasetScreenshotIndex = index,
                        )
                    }
                    out[fileName] = frameJsonObject(
                        arFrame,
                        fileName,
                        buildConfig,
                        frameIndex = null,
                    )
                    Thread.sleep(8L)
                } catch (_: Throwable) {
                }
            }
            out
        }
        return batch ?: linkedMapOf()
    }

    private fun captureSingleDatasetFrameInNewSession(
        context: Context,
        fileName: String,
        datasetIndex: Int,
        meta: CaptureFrameMeta?,
    ): JSONObject? {
        return runArCoreSession(context) { session ->
            waitForTrackingFrame(session, maxAttempts = 40, deadlineMs = 1_000L)
            val arFrame = waitForTrackingFrame(session, maxAttempts = 40, deadlineMs = 1_500L)
                ?: return@runArCoreSession null
            val buildConfig = if (meta != null) {
                FrameBuildConfig(
                    captureWidth = meta.captureWidth,
                    captureHeight = meta.captureHeight,
                    imageTimestampNs = meta.imageTimestampNs,
                    videoTimeSec = meta.videoOffsetNs?.let { it / 1_000_000_000.0 },
                    datasetScreenshotIndex = datasetIndex,
                )
            } else {
                val preset = captureResolutionForContext(context)
                FrameBuildConfig(
                    captureWidth = preset.width,
                    captureHeight = preset.height,
                    imageTimestampNs = null,
                    datasetScreenshotIndex = datasetIndex,
                )
            }
            frameJsonObject(arFrame, fileName, buildConfig, frameIndex = null)
        }
    }

    private fun frameJsonObject(
        frame: Frame,
        mediaFileName: String,
        buildConfig: FrameBuildConfig,
        frameIndex: Int?,
    ): JSONObject {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            throw IllegalStateException("TRACKING 프레임만 poses.json에 포함할 수 있습니다: ${camera.trackingState}")
        }
        val intr = camera.imageIntrinsics
        val focal = FloatArray(2)
        val principal = FloatArray(2)
        val dims = IntArray(2)
        intr.getFocalLength(focal, 0)
        intr.getPrincipalPoint(principal, 0)
        intr.getImageDimensions(dims, 0)

        val scaled = scaleIntrinsicsToCaptureResolution(
            focal,
            principal,
            dims[0],
            dims[1],
            buildConfig.captureWidth,
            buildConfig.captureHeight,
        )

        val pose = camera.displayOrientedPose
        val t = FloatArray(3)
        val q = FloatArray(4)
        pose.getTranslation(t, 0)
        pose.getRotationQuaternion(q, 0)

        val arcoreTimestampNs = frame.timestamp
        val imageTimestampNs = buildConfig.imageTimestampNs ?: arcoreTimestampNs
        val rawClockDeltaNs = abs(arcoreTimestampNs - imageTimestampNs)

        val matchMethod = buildConfig.timestampMatchMethod
        val semanticDeltaNs = when (matchMethod) {
            "index_pairing",
            "video_offset_index",
            "video_timeline",
            "video_timeline_index",
            -> 0L
            "single_photo" ->
                if (rawClockDeltaNs <= SINGLE_PHOTO_MAX_DELTA_NS) rawClockDeltaNs else 0L
            else -> rawClockDeltaNs
        }

        val obj = JSONObject()
        obj.put("filename", mediaFileName)
        if (frameIndex != null) obj.put("frameIndex", frameIndex)
        buildConfig.videoTimeSec?.let { obj.put("videoTimeSec", it) }
        buildConfig.datasetScreenshotIndex?.let { idx ->
            obj.put("datasetScreenshotIndex", idx)
        }
        obj.put("fx", scaled.fx)
        obj.put("fy", scaled.fy)
        obj.put("cx", scaled.cx)
        obj.put("cy", scaled.cy)
        obj.put("imageWidth", scaled.imageWidth)
        obj.put("imageHeight", scaled.imageHeight)
        obj.put("arcoreSourceWidth", dims[0])
        obj.put("arcoreSourceHeight", dims[1])
        obj.put("tx", t[0].toDouble())
        obj.put("ty", t[1].toDouble())
        obj.put("tz", t[2].toDouble())
        obj.put("qx", q[0].toDouble())
        obj.put("qy", q[1].toDouble())
        obj.put("qz", q[2].toDouble())
        obj.put("qw", q[3].toDouble())
        obj.put("trackingState", TrackingState.TRACKING.name)
        obj.put("imageTimestampNs", imageTimestampNs)
        obj.put("arcoreTimestampNs", arcoreTimestampNs)
        obj.put("timestampNs", imageTimestampNs)
        obj.put("timestampMatchDeltaNs", semanticDeltaNs)
        if (rawClockDeltaNs > semanticDeltaNs) {
            obj.put("arcoreCaptureDelayNs", rawClockDeltaNs)
        }
        obj.put("timestampMatchMethod", matchMethod)
        return obj
    }

    private fun <T> runArCoreSession(
        context: Context,
        work: (Session) -> T?,
    ): T? {
        val app = context.applicationContext
        var display: EGLDisplay? = null
        var eglContext: EGLContext? = null
        var eglSurface: EGLSurface? = null
        var session: Session? = null
        val tex = IntArray(1)

        try {
            val egl = createMiniEgl()
            display = egl.first
            eglContext = egl.second
            eglSurface = egl.third
            if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) {
                return null
            }

            GLES20.glGenTextures(1, tex, 0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE,
            )

            session = Session(app)
            session.configure(configForPoseSnapshotOnly(session))
            session.setCameraTextureName(tex[0])
            session.resume()

            return work(session)
        } catch (_: Throwable) {
            return null
        } finally {
            try {
                session?.pause()
            } catch (_: Throwable) {
            }
            try {
                session?.close()
            } catch (_: Throwable) {
            }
            val d = display
            if (d != null && eglSurface != null && eglContext != null) {
                try {
                    if (EGL14.eglMakeCurrent(d, eglSurface, eglSurface, eglContext)) {
                        if (tex[0] != 0) {
                            GLES20.glDeleteTextures(1, tex, 0)
                        }
                    }
                } catch (_: Throwable) {
                }
                try {
                    EGL14.eglMakeCurrent(
                        d,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT,
                    )
                } catch (_: Throwable) {
                }
                try {
                    EGL14.eglDestroySurface(d, eglSurface)
                } catch (_: Throwable) {
                }
                try {
                    EGL14.eglDestroyContext(d, eglContext)
                } catch (_: Throwable) {
                }
                try {
                    EGL14.eglTerminate(d)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun createMiniEgl(): Triple<EGLDisplay, EGLContext, EGLSurface> {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(display, version, 0, version, 1)

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE,
            EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE,
            8,
            EGL14.EGL_GREEN_SIZE,
            8,
            EGL14.EGL_BLUE_SIZE,
            8,
            EGL14.EGL_ALPHA_SIZE,
            8,
            EGL14.EGL_SURFACE_TYPE,
            EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, num, 0)
        val config = configs[0]!!

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            ctxAttribs,
            0,
        )

        val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 16, EGL14.EGL_HEIGHT, 16, EGL14.EGL_NONE)
        val surface = EGL14.eglCreatePbufferSurface(display, config, surfAttribs, 0)

        return Triple(display, context, surface)
    }
}
