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

/**
 * CameraX와 동시에 후면 카메라를 잡고 있을 수 없어, 촬영 파일이 준비된 뒤 잠시 세션을 연다.
 *
 * - **사진**: 단일 프레임.
 * - **동영상 전체**: 파일 재생 길이만큼 종료 직후 ARCore를 ~30Hz로 샘플링한 `frames[]`(녹화 프레임과 1:1 픽셀 동기는 아님).
 * - 데이터셋 스크린샷별 매칭은 [captureDatasetScreenshotsBestEffort] 참고.
 */
object ArcorePoseSnapshotter {

    private const val MAX_DATASET_ARCORE_FRAMES = 500

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

        return runArCoreSession(context) { session ->
            var chosen: Frame? = null
            val deadlineMs = SystemClock.elapsedRealtime() + 1_200L
            var i = 0
            while (i < 24 && SystemClock.elapsedRealtime() < deadlineMs) {
                val frame = session.update()
                chosen = frame
                if (frame.camera.trackingState == TrackingState.TRACKING) break
                i++
            }
            val frame = chosen ?: return@runArCoreSession null
            JSONObject().put("frames", JSONArray().put(frameJsonObject(frame, imageFileName, null, null, null)))
        }
    }

    /**
     * 동영상 전체: 재생 [duration] 구간을 녹화 종료 직후 **실시간**으로 ARCore를 샘플링한다.
     * 각 항목의 `filename`은 동영상 파일명, `frameIndex`, `videoTimeSec`는 간격 기준 타임라인 위치.
     */
    fun captureFullVideoTimelineOrNull(context: Context, videoFile: File): JSONObject? {
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

        return runArCoreSession(context) { session ->
            val warmupDeadline = SystemClock.elapsedRealtime() + 1_000L
            while (SystemClock.elapsedRealtime() < warmupDeadline) {
                val f = session.update()
                if (f.camera.trackingState == TrackingState.TRACKING) break
            }

            val frames = JSONArray()
            val wallStart = SystemClock.elapsedRealtime()
            var idx = 0
            while (SystemClock.elapsedRealtime() - wallStart < targetWallMs && idx < maxFrames) {
                val arFrame = session.update()
                val videoTimeSec = (idx * intervalMs) / 1000.0
                frames.put(
                    frameJsonObject(arFrame, videoFile.name, idx, videoTimeSec, null),
                )
                idx++
                try {
                    Thread.sleep(intervalMs)
                } catch (_: InterruptedException) {
                    break
                }
            }
            if (frames.length() == 0) {
                val arFrame = session.update()
                frames.put(frameJsonObject(arFrame, videoFile.name, 0, 0.0, null))
            }
            JSONObject()
                .put("mediaType", "video_full_timeline")
                .put("videoFileName", videoFile.name)
                .put("durationMs", durationMs)
                .put("sampleIntervalMs", intervalMs)
                .put(
                    "captureNoteKo",
                    "녹화 종료 직후, 동영상 재생 길이와 같은 실시간 구간에서 ARCore 포즈를 샘플링했습니다. " +
                        "디코딩된 영상의 각 프레임과 1:1 시각 동기는 보장되지 않습니다.",
                )
                .put("frames", frames)
        }
    }

    private const val MAX_INDIVIDUAL_FALLBACK_FRAMES = 80

    /**
     * 데이터셋 스크린샷 파일명(정렬된 순서)마다 ARCore 프레임을 채운다.
     * 한 세션 배치가 실패하거나 일부만 성공하면, 누락분에 한해 **새 세션으로 1프레임씩** 재시도한다.
     * ARCore 미설치 시 빈 맵(이미지·ZIP은 호출측에서 그대로 저장).
     */
    fun captureDatasetScreenshotsBestEffort(
        context: Context,
        imageFileNamesInOrder: List<String>,
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

        val fromBatch = runDatasetBatchSession(context, names)
        for ((k, v) in fromBatch) {
            result[k] = v
        }

        val missing = names.filter { !result.containsKey(it) }
        if (missing.isEmpty()) return result

        val limit = minOf(missing.size, MAX_INDIVIDUAL_FALLBACK_FRAMES)
        for (i in 0 until limit) {
            val fileName = missing[i]
            val idx = names.indexOf(fileName)
            captureSingleDatasetFrameInNewSession(context, fileName, idx.coerceAtLeast(0))?.let {
                result[fileName] = it
            }
            try {
                Thread.sleep(48L)
            } catch (_: InterruptedException) {
            }
        }
        return result
    }

    private fun runDatasetBatchSession(
        context: Context,
        names: List<String>,
    ): LinkedHashMap<String, JSONObject> {
        val batch = runArCoreSession(context) { session ->
            val out = LinkedHashMap<String, JSONObject>()
            val warmupDeadline = SystemClock.elapsedRealtime() + 1_000L
            while (SystemClock.elapsedRealtime() < warmupDeadline) {
                val f = session.update()
                if (f.camera.trackingState == TrackingState.TRACKING) break
            }
            names.forEachIndexed { index, fileName ->
                try {
                    val arFrame = session.update()
                    out[fileName] = frameJsonObject(
                        arFrame,
                        fileName,
                        null,
                        null,
                        index,
                    )
                    Thread.sleep(8L)
                } catch (_: Throwable) {
                }
            }
            out
        }
        return batch ?: linkedMapOf()
    }

    /** 배치 세션이 깨졌을 때, 파일 하나당 짧은 ARCore 세션으로 1프레임만 수집 */
    private fun captureSingleDatasetFrameInNewSession(
        context: Context,
        fileName: String,
        datasetIndex: Int,
    ): JSONObject? {
        return runArCoreSession(context) { session ->
            val warmupDeadline = SystemClock.elapsedRealtime() + 800L
            while (SystemClock.elapsedRealtime() < warmupDeadline) {
                session.update()
            }
            val arFrame = session.update()
            frameJsonObject(arFrame, fileName, null, null, datasetIndex)
        }
    }

    private fun frameJsonObject(
        frame: Frame,
        mediaFileName: String,
        frameIndex: Int?,
        videoTimeSec: Double?,
        datasetScreenshotIndex: Int?,
    ): JSONObject {
        val camera = frame.camera
        val intr = camera.imageIntrinsics
        val focal = FloatArray(2)
        val principal = FloatArray(2)
        val dims = IntArray(2)
        intr.getFocalLength(focal, 0)
        intr.getPrincipalPoint(principal, 0)
        intr.getImageDimensions(dims, 0)

        val pose = camera.displayOrientedPose
        val t = FloatArray(3)
        val q = FloatArray(4)
        pose.getTranslation(t, 0)
        pose.getRotationQuaternion(q, 0)

        val obj = JSONObject()
        obj.put("filename", mediaFileName)
        if (frameIndex != null) obj.put("frameIndex", frameIndex)
        if (videoTimeSec != null) obj.put("videoTimeSec", videoTimeSec)
        if (datasetScreenshotIndex != null) {
            obj.put("datasetScreenshotIndex", datasetScreenshotIndex)
            obj.put(
                "capturePipelineNote",
                "동영상 데이터셋 스크린샷과 1:1로 묶인 ARCore 값; 포즈는 녹화 종료 후 동일 순서로 샘플링됨 " +
                    "(녹화 중 셔터 시각과 픽셀 동기는 ARCore 공유 카메라 없이 불가).",
            )
        }
        obj.put("fx", focal[0].toDouble())
        obj.put("fy", focal[1].toDouble())
        obj.put("cx", principal[0].toDouble())
        obj.put("cy", principal[1].toDouble())
        obj.put("imageWidth", dims[0])
        obj.put("imageHeight", dims[1])
        obj.put("tx", t[0].toDouble())
        obj.put("ty", t[1].toDouble())
        obj.put("tz", t[2].toDouble())
        obj.put("qx", q[0].toDouble())
        obj.put("qy", q[1].toDouble())
        obj.put("qz", q[2].toDouble())
        obj.put("qw", q[3].toDouble())
        obj.put("trackingState", camera.trackingState.name)
        obj.put("timestampNs", frame.timestamp)
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
