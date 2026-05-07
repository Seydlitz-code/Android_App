package com.example.app_01

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Surface
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix as GLMatrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import androidx.compose.foundation.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import android.util.Size
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Brush
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import android.widget.VideoView
import android.widget.MediaController
import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.example.app_01.ui.theme.App_01Theme
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.TimeZone
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier
import java.security.cert.X509Certificate
import java.util.Collections

private const val CONTINUOUS_CAPTURE_MAX_SHOTS = 200
private const val CONTINUOUS_CAPTURE_INTERVAL_MS = 3_000L
private const val CAMERA_REBIND_WAIT_AFTER_ARCORE_MS = 550L

/**
 * CameraX 언바인드 뒤 한 번의 ARCore 구간에서 여러 장의 포즈 메타를 채우고 JSON·ZIP을 저장한다.
 * 연속 촬영은 샷마다 세션을 열지 않고 종료 시 한 번만 호출해 카메라 재바인드 횟수를 줄인다.
 *
 * @return 메타 누락 또는 저장 IO 실패한 파일 수
 */
private suspend fun runBatchedArcoreMetadataSave(
    context: Context,
    photoFiles: List<File>,
    prepareExclusiveCamera: suspend () -> Unit,
    requestCameraRebind: suspend () -> Unit,
): Int {
    if (photoFiles.isEmpty()) return 0
    prepareExclusiveCamera()
    var failures = 0
    try {
        val map = withContext(Dispatchers.IO) {
            if (photoFiles.size == 1) {
                val f = photoFiles[0]
                val root = ArcorePoseSnapshotter.capturePhotoMetadataOrNull(context, f.name)
                val jo = root?.optJSONArray("frames")?.optJSONObject(0)
                if (jo != null) linkedMapOf(f.name to jo) else linkedMapOf()
            } else {
                ArcorePoseSnapshotter.captureDatasetScreenshotsBestEffort(
                    context,
                    photoFiles.map { it.name },
                )
            }
        }
        withContext(Dispatchers.IO) {
            for (f in photoFiles) {
                val frameJo = map[f.name]
                if (frameJo == null) {
                    failures++
                    continue
                }
                val rootJson = JSONObject().put("frames", JSONArray().put(frameJo))
                runCatching {
                    JsonLibrary.saveArCoreFramesJson(context, rootJson)
                    ArcoreLibrary.savePhotoAndPosesZip(context, f, rootJson.toString())
                }.onFailure { failures++ }
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        failures = maxOf(failures, photoFiles.size)
    } finally {
        requestCameraRebind()
    }
    return failures
}

@Composable
fun CameraScreen(
    cameraEntryMode: CameraEntryMode,
    lastCapturedImageUri: Uri?,
    onImageCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var arcoreMetaEnabled by remember {
        mutableStateOf(CameraArCorePrefs.isArCoreMetaEnabled(context))
    }
    var cameraRebindNonce by remember { mutableIntStateOf(0) }
    var pendingArcoreForVideo by remember { mutableStateOf(false) }
    var pendingVideoDatasetDirForArcore by remember { mutableStateOf<File?>(null) }
    /** Compose 상태와 무관하게 Finalize 시 폴더를 찾기 위한 백업 */
    var pendingVideoDatasetPathForArcore by remember { mutableStateOf<String?>(null) }
    val mediaActionSound = remember {
        MediaActionSound().apply {
            load(MediaActionSound.START_VIDEO_RECORDING)
            load(MediaActionSound.STOP_VIDEO_RECORDING)
        }
    }
    DisposableEffect(Unit) {
        onDispose { mediaActionSound.release() }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<androidx.camera.video.Recorder>? by remember { mutableStateOf(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0L) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var isContinuousBurstActive by remember { mutableStateOf(false) }
    var continuousCapturedCount by remember { mutableIntStateOf(0) }
    var continuousBurstJob by remember { mutableStateOf<Job?>(null) }
    /** 셔터로 연속 촬영 중지 시 취소 토스트 중복 방지 */
    val continuousBurstUserStop = remember { AtomicBoolean(false) }
    /** 사진·동영상 모드 전환 등으로 연속 촬영 코루틴을 취소할 때 조용히 처리 */
    val continuousBurstSilentCancel = remember { AtomicBoolean(false) }
    /** 단일·연속·동영상 ARCore 후처리가 동시에 카메라를 잡지 않도록 직렬화 */
    val arcoreExclusiveMutex = remember { Mutex() }
    /** 연속 촬영: 샷 루프에서는 쌓아두고 종료 시 한 번에 배치 저장 */
    val continuousArcorePending = remember {
        Collections.synchronizedList(ArrayList<File>(32))
    }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var previewOriginInRoot by remember { mutableStateOf<Offset?>(null) }
    var datasetDir by remember { mutableStateOf<File?>(null) }
    var isDatasetCollectionEnabled by remember { mutableStateOf(true) }
    var selectedResolution by remember { mutableStateOf(ResolutionPreset.RESOLUTION_1024x1024) }
    var azimuthDegrees by remember { mutableStateOf(0f) }
    /**
     * 그리드 오버레이 및 커버리지 기록 전용 방위각.
     * alpha=0.15 필터를 사용하는 azimuthDegrees와 달리 alpha=0.7(거의 실시간)을 적용해
     * 카메라 회전 시 그리드가 물리 공간에 즉시 고정되어 보이도록 한다.
     */
    var azimuthForGrid by remember { mutableStateOf(0f) }

    // ── 후면 카메라 바인딩 정보 ──────────────────────────────────────────────
    // telephoto / wide 각각 "논리 카메라 ID" 또는 "물리 카메라 ID + 부모 논리 ID" 중 하나만 설정됨
    var rearTelephotoLogicalId  by remember { mutableStateOf<String?>(null) }  // 논리 망원
    var rearTelephotoPhysId     by remember { mutableStateOf<String?>(null) }  // 물리 망원
    var rearTelephotoPhysParent by remember { mutableStateOf<String?>(null) }  // 물리 망원의 부모 논리 ID
    var rearWideId              by remember { mutableStateOf<String?>(null) }  // 광각 논리

    // 앱 시작 시 한 번: 2단계 탐색 (논리 → 물리 서브카메라 순)
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // ── 1단계: CameraX 논리 카메라 탐색 ──────────────────────────────
            data class LogicalCam(val id: String, val minFocal: Float)

            @Suppress("UnsafeOptInUsageError")
            val logicalBack: List<LogicalCam> = cameraProvider.availableCameraInfos
                .filter { it.lensFacing == CameraSelector.LENS_FACING_BACK }
                .mapNotNull { info ->
                    val id = runCatching {
                        Camera2CameraInfo.from(info).cameraId
                    }.getOrNull() ?: return@mapNotNull null
                    val focals = runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    LogicalCam(id, focals.min())
                }
                .sortedBy { it.minFocal }

            android.util.Log.d("CameraScreen",
                "논리 후면 카메라: ${logicalBack.map { "${it.id}(${it.minFocal}mm)" }}")

            if (logicalBack.size >= 2) {
                // 논리 카메라가 2개 이상 → 초점거리 최대 = 망원, 3.5mm 근접 = 광각
                rearTelephotoLogicalId = logicalBack.maxByOrNull { it.minFocal }?.id
                rearWideId             = logicalBack.minByOrNull { Math.abs(it.minFocal - 3.5f) }?.id
                android.util.Log.d("CameraScreen",
                    "논리 선택 → 망원: $rearTelephotoLogicalId, 광각: $rearWideId")
            } else {
                // ── 2단계: 논리 카메라가 1개(퓨전 카메라)인 경우 → 물리 서브카메라 탐색 ──
                rearWideId = logicalBack.firstOrNull()?.id

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val logicalIds = logicalBack.map { it.id }
                    var bestPhysId: String? = null
                    var bestParentId: String? = null
                    var bestFocal = 0f

                    for (parentId in logicalIds) {
                        val parentChars = runCatching {
                            cameraManager.getCameraCharacteristics(parentId)
                        }.getOrNull() ?: continue
                        for (physId in parentChars.physicalCameraIds) {
                            val chars = runCatching {
                                cameraManager.getCameraCharacteristics(physId)
                            }.getOrNull() ?: continue
                            val focals = chars
                                .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                ?.takeIf { it.isNotEmpty() } ?: continue
                            val maxF = focals.max()
                            if (maxF > bestFocal) {
                                bestFocal = maxF; bestPhysId = physId; bestParentId = parentId
                            }
                        }
                    }

                    // 광각 초점거리보다 1.5배 이상 길어야 망원으로 인정
                    val wideFocal = logicalBack.firstOrNull()?.minFocal ?: 3.5f
                    if (bestPhysId != null && bestFocal > wideFocal * 1.5f) {
                        rearTelephotoPhysId     = bestPhysId
                        rearTelephotoPhysParent = bestParentId
                    }
                    android.util.Log.d("CameraScreen",
                        "물리 선택 → 망원: $rearTelephotoPhysId(${bestFocal}mm, parent=$rearTelephotoPhysParent), 광각: $rearWideId")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CameraScreen", "카메라 탐색 실패: ${e.message}")
        }
    }

    // [추가] 촬영 중 화면 켜짐 유지
    val view = LocalView.current
    DisposableEffect(isRecording) {
        if (isRecording) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    // [추가] 백그라운드 작업 유지를 위한 서비스 제어
    LaunchedEffect(isRecording) {
        val intent = Intent(context, AppForegroundService::class.java)
        if (isRecording) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    // 이동식 공간: 3D 격자 세션(구역 기억·수집 상태)
    val mobileSpaceSession = remember { MobileSpaceCaptureSession(context.filesDir) }
    var mobileSpaceUiRev by remember { mutableIntStateOf(0) }
    val mobileSpaceOverlayState = remember(mobileSpaceUiRev, cameraEntryMode) {
        if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
            mobileSpaceSession.snapshotOverlay()
        } else {
            MobileSpaceGridOverlayState(0, 6, 4, 3, emptyList(), emptyList())
        }
    }
    // 이동식 공간: 방향별 커버리지(빨강→투명 오버레이용)
    val scanCoverage = remember { MobileSpaceScanCoverage() }
    val meshAnalysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraEntryModeState = rememberUpdatedState(cameraEntryMode)
    DisposableEffect(Unit) {
        onDispose { meshAnalysisExecutor.shutdown() }
    }

    // [추가] 경차 촬영(OBJECT) 전용: 사물이 중앙 가상 사각형(1000x1000) 밖으로 벗어났는지 경고
    DisposableEffect(Unit) {
        onDispose {
            try {
                ObjectOutOfFrameWarning.close()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isFlashOn, camera) {
        try {
            camera?.cameraControl?.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    var baseAzimuthDegrees by remember { mutableStateOf<Float?>(null) }
    // 수직(세로) 들었을 때 0도를 기준으로 하는 기울기
    var pitchDegrees by remember { mutableStateOf(0f) }
    /** 이동식 공간 격자 롤 축(3D 보xel) */
    var rollDegrees by remember { mutableStateOf(0f) }
    var basePitchDegrees by remember { mutableStateOf<Float?>(null) }
    var capturedSectors by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val ringSize = 120.dp

    val sectorCount = 30
    val sectorSize = 360f / sectorCount
    // 지면-휴대폰 각도 기준: 90도, 110도, 70도 (공간 스캔 시)
    val pitchTargets = remember(cameraEntryMode) {
        if (cameraEntryMode.isSpaceMode()) listOf(90f, 110f, 70f) else listOf(120f)
    }
    val pitchTolerance = 10f
    var currentPitchIndex by remember { mutableStateOf(0) }
    val currentTargetPitchState = remember {
        derivedStateOf { pitchTargets.getOrNull(currentPitchIndex) }
    }
    val currentTargetPitch by currentTargetPitchState
    val effectivePitchDegreesState = remember {
        // [최적화] pitchDegrees가 이제 중력 기준 절대 각도(수직=90)이므로 그대로 사용
        derivedStateOf { pitchDegrees }
    }
    val effectivePitchDegrees by effectivePitchDegreesState
    val isPitchAlignedState = remember {
        derivedStateOf {
            val target = currentTargetPitchState.value
            target != null && abs(effectivePitchDegrees - target) <= pitchTolerance
        }
    }
    val isPitchAligned by isPitchAlignedState
    val relativeAzimuthDegrees by remember {
        derivedStateOf {
            val base = baseAzimuthDegrees
            if (base == null) {
                azimuthDegrees
            } else {
                var diff = azimuthDegrees - base
                if (diff < 0f) diff += 360f
                diff % 360f
            }
        }
    }
    val displayAzimuthDegrees by remember {
        derivedStateOf { relativeAzimuthDegrees % 360f }
    }

    val currentSectorIndex by remember {
        derivedStateOf {
            ((displayAzimuthDegrees / sectorSize).toInt()).coerceIn(0, sectorCount - 1)
        }
    }

    DisposableEffect(cameraEntryMode) {
        capturedSectors = emptySet()
        currentPitchIndex = 0
        basePitchDegrees = null
        if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
            mobileSpaceSession.loadFromDisk()
            scanCoverage.reset()
            mobileSpaceUiRev++
        } else {
            mobileSpaceSession.persistToDisk()
            mobileSpaceSession.resetForModeExit()
            mobileSpaceUiRev++
        }
        onDispose { }
    }

    // 이동식 공간 진입 시 그리드용 방위를 나침반 방위와 즉시 일치 (초기 프레임 동기화)
    LaunchedEffect(cameraEntryMode) {
        if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
            azimuthForGrid = azimuthDegrees
        }
    }

    DisposableEffect(cameraEntryMode) {
        val shouldUseSensors = cameraEntryMode.isSpaceMode() || cameraEntryMode.isObjectMode()
        if (!shouldUseSensors) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val adjustedRotationMatrix = FloatArray(9)
                val rotation = previewView?.display?.rotation ?: Surface.ROTATION_0
                val (xAxis, yAxis) = when (rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    xAxis,
                    yAxis,
                    adjustedRotationMatrix
                )
                SensorManager.getOrientation(adjustedRotationMatrix, orientation)
                var rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (rawAzimuth < 0f) rawAzimuth += 360f

                // [수정] Low-pass Filter 적용 (노이즈/Jitter 감소)
                val alpha = 0.15f
                val currentAzimuth = azimuthDegrees
                var delta = rawAzimuth - currentAzimuth
                if (delta > 180f) delta -= 360f
                if (delta < -180f) delta += 360f
                var nextAzimuth = currentAzimuth + delta * alpha
                if (nextAzimuth < 0f) nextAzimuth += 360f
                if (nextAzimuth >= 360f) nextAzimuth -= 360f

                azimuthDegrees = nextAzimuth

                // 그리드 전용 방위: alpha=0.7 (거의 실시간) → 그리드가 물리 공간에 즉시 고정
                val gridAlpha = 0.7f
                var gridDelta = rawAzimuth - azimuthForGrid
                if (gridDelta > 180f) gridDelta -= 360f
                if (gridDelta < -180f) gridDelta += 360f
                var nextGridAz = azimuthForGrid + gridDelta * gridAlpha
                if (nextGridAz < 0f) nextGridAz += 360f
                if (nextGridAz >= 360f) nextGridAz -= 360f
                azimuthForGrid = nextGridAz

                val rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                
                // [개선] 중력 벡터를 이용한 절대 기울기 계산 (수직=90도)
                // rotationMatrix[7]: Y축 성분, rotationMatrix[8]: Z축 성분
                val worldZInPhoneY = rotationMatrix[7]
                val worldZInPhoneZ = rotationMatrix[8]
                val angleDeg = Math.toDegrees(Math.atan2(worldZInPhoneZ.toDouble(), worldZInPhoneY.toDouble())).toFloat()
                
                // 앞으로 숙이면 90도 미만, 뒤로 젖히면 90도 초과가 되도록 설정
                pitchDegrees = 90f + angleDeg
                rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(
                listener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(captureMode) {
        if (captureMode != CaptureMode.CONTINUOUS) {
            continuousBurstSilentCancel.set(true)
            continuousBurstJob?.cancel()
            continuousBurstJob = null
            isContinuousBurstActive = false
            continuousCapturedCount = 0
        }
    }

    // 동영상 촬영 시간 업데이트
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(100)
            recordingTime += 100
        }
    }

    // 동영상 촬영 시 데이터셋 수집 제어 (촬영 시작 3초 후 시작)
    LaunchedEffect(isRecording, captureMode) {
        if (captureMode != CaptureMode.VIDEO || !isRecording) {
            isDatasetCollectionEnabled = true
            return@LaunchedEffect
        }
        isDatasetCollectionEnabled = false
        delay(3000)
        if (isRecording && captureMode == CaptureMode.VIDEO) {
            isDatasetCollectionEnabled = true
        }
    }

    // 동영상 촬영 시 구역 체크: 1초 간격으로 현재 구역 기록 (사물/공간 모두)
    LaunchedEffect(isRecording, cameraEntryMode, captureMode, currentPitchIndex, isDatasetCollectionEnabled, datasetDir) {
        // [수정] 이동식 공간 촬영인 경우: 촬영 시작 3초 후(isDatasetCollectionEnabled=true) 1초 간격 자동 촬영
        if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
            if (captureMode == CaptureMode.VIDEO && isRecording && isDatasetCollectionEnabled) {
                var captureCount = 0
                val dir = datasetDir
                val capture = imageCapture
                
                // 직전에 저장된 데이터셋 이미지의 축소본 (유사도 비교용)
                var lastSavedBitmapSmall: android.graphics.Bitmap? = null

                if (dir != null && capture != null) {
                    while (isRecording && isDatasetCollectionEnabled) {
                        captureCount++
                        val fileName = "mobile_${captureCount}.jpg"

                        captureDatasetImageAndAwait(context, 0, 0, dir, capture, fileName) { currentBitmap ->
                            // 그리드 전용 방위(azimuthForGrid)를 기준으로 커버리지 기록.
                            // azimuthForGrid는 alpha=0.7 필터로 거의 실시간이므로
                            // 오버레이(headingDeg=azimuthForGrid)와 동일 좌표계가 됨.
                            val azNow    = azimuthForGrid          // 그리드 좌표계 방위 (0-360°)
                            val pitchNow = effectivePitchDegrees
                            val rollNow  = rollDegrees
                            if (lastSavedBitmapSmall == null) {
                                // 첫 번째 데이터셋은 무조건 저장
                                lastSavedBitmapSmall = android.graphics.Bitmap.createScaledBitmap(currentBitmap, 64, 64, true)
                                mobileSpaceSession.recordAcceptedSample(azNow, pitchNow, rollNow)
                                scanCoverage.recordFov(azNow, pitchNow)
                                mobileSpaceUiRev++
                                true
                            } else {
                                // 현재 이미지와 직전 저장된 이미지 간의 구조적 유사도 비교
                                val similarity = calculateImageSimilarity(lastSavedBitmapSmall!!, currentBitmap)
                                val similarEnough = similarity >= 0.48f && similarity <= 0.985f
                                val almostDuplicate = similarity > 0.985f && (captureCount % 4 == 0)
                                val veryDifferent = similarity < 0.48f && (captureCount % 5 == 0)
                                if (similarEnough || almostDuplicate || veryDifferent) {
                                    // 조건 만족: 저장하고 비교 기준 갱신
                                    lastSavedBitmapSmall = android.graphics.Bitmap.createScaledBitmap(currentBitmap, 64, 64, true)
                                    mobileSpaceSession.recordAcceptedSample(azNow, pitchNow, rollNow)
                                    scanCoverage.recordFov(azNow, pitchNow)
                                    mobileSpaceUiRev++
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                        delay(1000) // 1초 간격
                    }
                }
            }
            return@LaunchedEffect
        }

        if (captureMode == CaptureMode.VIDEO && isRecording && isDatasetCollectionEnabled) {
            // 사물/공간 촬영: 피치 각도 체크 (90도 등)
            if (currentPitchIndex < pitchTargets.size) {
                while (isRecording) {
                    delay(500) // 딜레이 단축 (반응성 향상)
                    val targetPitch = currentTargetPitchState.value
                    val pitchNow = effectivePitchDegreesState.value
                    val isAligned =
                        targetPitch != null && abs(pitchNow - targetPitch) <= pitchTolerance
                    if (isAligned) {
                        val sectorIndex =
                            ((displayAzimuthDegrees / sectorSize).toInt()).coerceIn(0, sectorCount - 1)
                        if (!capturedSectors.contains(sectorIndex)) {
                            // [수정] 선명도 보정(초점 재조정) 로직 제거됨 - 사용자 요청
                            // Joint Reflection Removal and Depth Estimation 준비 단계
                            
                            capturedSectors = capturedSectors + sectorIndex
                            val dir = datasetDir
                            val capture = imageCapture
                            if (dir != null && capture != null && targetPitch != null) {
                                captureDatasetImageAndAwait(
                                    context,
                                    sectorIndex,
                                    targetPitch.toInt(),
                                    dir,
                                    capture,
                                )
                            }
                            
                            // 연속 촬영 방지를 위해 잠시 대기
                            delay(300)
                        }
                    }
                }
            }
        }
    }

    // 한 각도에서 360도 촬영 완료 시 다음 각도로 전환
    LaunchedEffect(capturedSectors, isRecording, cameraEntryMode, captureMode, currentPitchIndex) {
        // [추가] 이동식 공간 촬영인 경우 섹터 로직 생략
        if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
            return@LaunchedEffect
        }

        if (captureMode == CaptureMode.VIDEO &&
            isRecording &&
            currentPitchIndex < pitchTargets.size &&
            capturedSectors.size >= sectorCount
        ) {
            currentPitchIndex += 1
            capturedSectors = emptySet()
        }
    }

    // 카메라 바인딩 함수
    fun bindCamera(view: PreviewView) {
        val executor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // ── 후면 카메라 선택 우선순위 ────────────────────────────────
                //  1) 망원 논리 카메라  2) 망원 물리 카메라(부모 논리 + setPhysicalCameraId)
                //  3) 광각 논리 카메라  4) 기기 기본 후면 카메라(폴백)
                // 전면: 기기 기본 전면 카메라
                @Suppress("UnsafeOptInUsageError")
                fun selectorByLogicalId(logicalId: String) = CameraSelector.Builder()
                    .addCameraFilter { list ->
                        val matched = list.filter {
                            runCatching { Camera2CameraInfo.from(it).cameraId == logicalId }
                                .getOrDefault(false)
                        }
                        matched.ifEmpty { list.filter { it.lensFacing == CameraSelector.LENS_FACING_BACK } }
                    }
                    .build()

                // 물리 망원 카메라 사용 여부와 그 ID (Preview/ImageCapture 빌더에 적용)
                val physicalIdToApply: String? = when {
                    lensFacing == CameraSelector.LENS_FACING_BACK
                        && rearTelephotoLogicalId == null
                        && rearTelephotoPhysId != null -> rearTelephotoPhysId
                    else -> null
                }

                val cameraSelector = when {
                    lensFacing == CameraSelector.LENS_FACING_FRONT ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    rearTelephotoLogicalId != null ->
                        selectorByLogicalId(rearTelephotoLogicalId!!)
                    rearTelephotoPhysParent != null ->
                        selectorByLogicalId(rearTelephotoPhysParent!!)
                    rearWideId != null ->
                        selectorByLogicalId(rearWideId!!)
                    else ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()

                // 선택된 해상도
                val targetSize = Size(selectedResolution.width, selectedResolution.height)
                
                // ViewPort 설정으로 명시적 비율 지정
                val viewPort = androidx.camera.core.ViewPort.Builder(
                    android.util.Rational(
                        selectedResolution.width,
                        selectedResolution.height
                    ),
                    view.display.rotation
                ).setScaleType(androidx.camera.core.ViewPort.FILL_CENTER).build()
                
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val previewBuilder = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                if (physicalIdToApply != null) {
                    @Suppress("UnsafeOptInUsageError")
                    Camera2Interop.Extender(previewBuilder).setPhysicalCameraId(physicalIdToApply)
                }
                val preview = previewBuilder.build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(resolutionSelector)
                if (physicalIdToApply != null) {
                    @Suppress("UnsafeOptInUsageError")
                    Camera2Interop.Extender(imageCaptureBuilder).setPhysicalCameraId(physicalIdToApply)
                }
                val newImageCapture = imageCaptureBuilder.build()

                val includeMobileAnalysis = cameraEntryMode == CameraEntryMode.MOBILE_SPACE

                fun buildMobileSpaceImageAnalysis(): ImageAnalysis {
                    val analysisRes = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                    val iab = ImageAnalysis.Builder()
                        .setResolutionSelector(analysisRes)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    if (physicalIdToApply != null) {
                        @Suppress("UnsafeOptInUsageError")
                        Camera2Interop.Extender(iab).setPhysicalCameraId(physicalIdToApply!!)
                    }
                    return iab.build().also { ia ->
                        ia.setAnalyzer(meshAnalysisExecutor) { image ->
                            try {
                                if (cameraEntryModeState.value != CameraEntryMode.MOBILE_SPACE) return@setAnalyzer
                                val bmp = imageProxyRgbaToBitmap(image) ?: return@setAnalyzer
                                // 신규 구역 생성 여부를 판단하기 위해 호출 전 구역 수 기록
                                val prevRegionCount = mobileSpaceSession.regionCount
                                val changed = mobileSpaceSession.onSceneFrame(bmp)
                                val newRegionCreated = mobileSpaceSession.regionCount > prevRegionCount
                                if (!bmp.isRecycled) bmp.recycle()
                                if (changed) {
                                    Handler(Looper.getMainLooper()).post {
                                        // 새로운 물리적 구역(방)으로 진입한 경우 커버리지 초기화
                                        // → 새 구역 전체가 빨간 그리드로 다시 표시됨
                                        if (newRegionCreated) {
                                            scanCoverage.reset()
                                        }
                                        mobileSpaceUiRev++
                                    }
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            } finally {
                                image.close()
                            }
                        }
                    }
                }

                if (captureMode == CaptureMode.PHOTO || captureMode == CaptureMode.CONTINUOUS) {
                    imageCapture = newImageCapture

                    // UseCaseGroup을 사용하여 ViewPort 적용
                    val photoGroup = androidx.camera.core.UseCaseGroup.Builder()
                        .setViewPort(viewPort)
                        .addUseCase(preview)
                        .addUseCase(imageCapture!!)
                    if (includeMobileAnalysis) {
                        photoGroup.addUseCase(buildMobileSpaceImageAnalysis())
                    }

                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        photoGroup.build()
                    )
                    videoCapture = null
                    isCameraReady = true
                } else {
                // VideoCapture에도 해상도 설정 적용
                // Recorder는 해상도보다는 Quality를 사용하지만,
                // 선택된 해상도에 따라 최대한 근접한 품질을 선택합니다.
                val recorder = androidx.camera.video.Recorder.Builder()
                    .setQualitySelector(
                        androidx.camera.video.QualitySelector.from(
                            androidx.camera.video.Quality.HIGHEST
                        )
                    )
                    .build()

                    videoCapture = VideoCapture.withOutput(recorder)
                    imageCapture = newImageCapture

                    // UseCaseGroup을 사용하여 ViewPort 적용
                    val videoGroup = androidx.camera.core.UseCaseGroup.Builder()
                        .setViewPort(viewPort)
                        .addUseCase(preview)
                        .addUseCase(videoCapture!!)
                        .addUseCase(imageCapture!!)
                    if (includeMobileAnalysis) {
                        videoGroup.addUseCase(buildMobileSpaceImageAnalysis())
                    }

                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        videoGroup.build()
                    )
                    isCameraReady = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, executor)
    }

    // lensFacing, captureMode, selectedResolution, 카메라ID 변경 시 재바인딩
    LaunchedEffect(lensFacing, captureMode, selectedResolution, previewView,
        rearTelephotoLogicalId, rearTelephotoPhysId, rearWideId, cameraEntryMode, cameraRebindNonce) {
        isCameraReady = false
        previewView?.let { bindCamera(it) }
    }

    // 선택된 해상도의 비율 계산 (정사각형: 1024/1024)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 해상도 변경 시 PreviewView를 완전히 재생성하기 위해 key 사용
        key(selectedResolution) {
            // 정사각형 해상도: 1:1 미리보기 강제 적용
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RectangleShape)
                    .onGloballyPositioned { coords ->
                        previewOriginInRoot = coords.positionInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
                    MobileSpaceScanOverlay(
                        headingDeg = azimuthForGrid,        // 그리드 전용 방위 (recordFov 와 동일 좌표계, 실시간)
                        pitchDeg = effectivePitchDegrees,
                        coverage = scanCoverage,
                        revisionTick = mobileSpaceUiRev,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 상단 바 배경
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.Black)
        )

        // 동영상 촬영 시간 표시 (상단 알약)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(Color.Red, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "동영상 촬영 ${formatTime(recordingTime)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 통합 경고 메시지 영역 (기울기/모델링 적합성)
        val shouldShowWarningColumn = captureMode == CaptureMode.VIDEO && isRecording
        if (shouldShowWarningColumn) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (captureMode == CaptureMode.VIDEO && isRecording) {
                    // 기울기 경고 (이동식 공간 촬영 제외)
                    if (cameraEntryMode != CameraEntryMode.MOBILE_SPACE) {
                        val isAllPitchCompleted = currentPitchIndex >= pitchTargets.size
                        if (!isAllPitchCompleted && !isPitchAligned) {
                            val targetPitch = currentTargetPitch
                            val warningMsg = if (targetPitch != null) {
                                val delta = effectivePitchDegrees - targetPitch
                                when {
                                    delta < -pitchTolerance -> "기기를 아래쪽으로 기울여주세요."
                                    delta > pitchTolerance -> "기기를 위쪽으로 기울여주세요."
                                    else -> null
                                }
                            } else null

                            if (warningMsg != null) {
                                Text(
                                    text = warningMsg,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Red, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                }
            }
        }

        // 상단 모드 전환 + 해상도 선택 (한 줄, 알약 형태)
        val topMenuPadding = 8.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topMenuPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopMenuSegmentedTriple(
                    leftText = "사진",
                    midText = "연속",
                    rightText = "동영상",
                    selectedIndex = when (captureMode) {
                        CaptureMode.PHOTO -> 0
                        CaptureMode.CONTINUOUS -> 1
                        CaptureMode.VIDEO -> 2
                    },
                    onLeftClick = { captureMode = CaptureMode.PHOTO },
                    onMidClick = { captureMode = CaptureMode.CONTINUOUS },
                    onRightClick = { captureMode = CaptureMode.VIDEO },
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flash",
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        // 동영상 촬영 중 구역 수집 정보 표시 (사물/공간 모두) - 이동식 공간 촬영은 제외
        if (captureMode == CaptureMode.VIDEO && isRecording && cameraEntryMode != CameraEntryMode.MOBILE_SPACE) {
            // 구역 수집 진행률 표시
            Text(
                text = "${capturedSectors.size} / ${sectorCount}장",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            
            // 동영상 촬영 안내 메시지 (사물/공간 모두 90도 촬영 지원)
            val isAllPitchCompleted = currentPitchIndex >= pitchTargets.size
            val instructionText = when {
                isAllPitchCompleted -> "모든 각도 촬영 완료"
                else -> currentTargetPitch?.let { "${it.toInt()}도 측정입니다" } ?: "촬영을 진행해주세요"
            }
            Text(
                text = instructionText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 230.dp)
                    .background(
                        if (isPitchAligned) Color(0xFF4CAF50).copy(alpha = 0.8f)
                        else Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            }
        
        if (captureMode == CaptureMode.CONTINUOUS &&
            (isContinuousBurstActive || continuousCapturedCount > 0)
        ) {
            Text(
                text = "연속 촬영 ${continuousCapturedCount} / $CONTINUOUS_CAPTURE_MAX_SHOTS · 간격 ${CONTINUOUS_CAPTURE_INTERVAL_MS / 1000}초",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 190.dp)
                    .background(
                        Color(0xFF1565C0).copy(alpha = 0.75f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // 하단 컨트롤 바
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 마지막 촬영 사진 썸네일 또는 갤러리 버튼
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .clickable { onGalleryClick() },
                contentAlignment = Alignment.Center
            ) {
                if (lastCapturedImageUri != null) {
                    val isVideo = isVideoUri(context, lastCapturedImageUri)
                    var videoThumbnail by remember(lastCapturedImageUri) { mutableStateOf<Bitmap?>(null) }
                    
                    // 동영상 썸네일 로드
                    if (isVideo) {
                        LaunchedEffect(lastCapturedImageUri) {
                            videoThumbnail = withContext(Dispatchers.IO) {
                                try {
                                    val retriever = MediaMetadataRetriever()
                                    try {
                                        retriever.setDataSource(context, lastCapturedImageUri)
                                        retriever.getFrameAtTime(0)
                                    } finally {
                                        retriever.release()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }
                            }
                        }
                        
                        // 동영상 썸네일 표시
                        if (videoThumbnail != null) {
                            Image(
                                bitmap = videoThumbnail!!.asImageBitmap(),
                                contentDescription = "마지막 촬영 동영상",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // 동영상 아이콘 오버레이
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = "동영상",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .padding(2.dp)
                            )
                        } else {
                            // 썸네일 로딩 중
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            )
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = "동영상",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // 이미지 표시
                        Image(
                            painter = rememberAsyncImagePainter(lastCapturedImageUri),
                            contentDescription = "마지막 촬영 사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "갤러리",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 동영상 촬영 중 구역 링 표시 (사물/공간 모두) - 이동식 공간 촬영은 제외
            val showRing = captureMode == CaptureMode.VIDEO && isRecording && cameraEntryMode != CameraEntryMode.MOBILE_SPACE
            val captureButtonSize =
                if (isRecording || isContinuousBurstActive) 64.dp else 72.dp

            // 촬영 버튼 + 링 (중심 일치)
            Box(
                modifier = Modifier.size(if (showRing) ringSize else captureButtonSize)
            ) {
                if (showRing) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f
                        val barEnd = radius * 0.84f
                        val barStart = radius * 0.76f
                        val stroke = 10f

                        for (i in 0 until sectorCount) {
                            val angle = i * (360f / sectorCount)
                            val color = when {
                                i == currentSectorIndex -> Color.White
                                capturedSectors.contains(i) -> Color(0xFF4CAF50)
                                else -> Color.DarkGray
                            }
                            rotate(degrees = angle, pivot = canvasCenter) {
                                drawLine(
                                    color = color,
                                    start = Offset(canvasCenter.x, canvasCenter.y - barStart),
                                    end = Offset(canvasCenter.x, canvasCenter.y - barEnd),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(captureButtonSize)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            when {
                                isRecording -> Color.Red
                                isContinuousBurstActive -> Color.Red
                                else -> Color.White
                            },
                        )
                        .clickable(
                            enabled = !isRecording ||
                                captureMode == CaptureMode.VIDEO ||
                                captureMode == CaptureMode.CONTINUOUS,
                        ) {
                            when (captureMode) {
                                CaptureMode.PHOTO -> {
                                    imageCapture?.let { capture ->
                                        val useArcoreMeta = arcoreMetaEnabled &&
                                            lensFacing == CameraSelector.LENS_FACING_BACK
                                        // 셔터 소리를 약 30% 수준으로 낮춤 (MediaActionSound는 볼륨 조절 불가)
                                        SoftShutterSound.play(volume = 0.3f)
                                        takePhoto(context, capture) { uri, file ->
                                            onImageCaptured(uri)
                                            if (useArcoreMeta) {
                                                scope.launch(Dispatchers.Default) {
                                                    try {
                                                        val fails = arcoreExclusiveMutex.withLock {
                                                            runBatchedArcoreMetadataSave(
                                                                context,
                                                                listOf(file),
                                                                prepareExclusiveCamera = {
                                                                    withContext(Dispatchers.Main) {
                                                                        val provider =
                                                                            ProcessCameraProvider.getInstance(
                                                                                context,
                                                                            ).get()
                                                                        provider.unbindAll()
                                                                        isCameraReady = false
                                                                    }
                                                                },
                                                                requestCameraRebind = {
                                                                    withContext(Dispatchers.Main) {
                                                                        cameraRebindNonce++
                                                                    }
                                                                },
                                                            )
                                                        }
                                                        if (fails > 0) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "ARCore 메타를 저장하지 못했습니다. ARCore 설치·지원 여부를 확인하세요.",
                                                                    Toast.LENGTH_LONG,
                                                                ).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context.applicationContext,
                                                                "ARCore 저장 오류: ${e.message ?: e.javaClass.simpleName}",
                                                                Toast.LENGTH_SHORT,
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                CaptureMode.CONTINUOUS -> {
                                    if (continuousBurstJob?.isActive == true) {
                                        val doneCount = continuousCapturedCount
                                        continuousBurstUserStop.set(true)
                                        continuousBurstJob?.cancel()
                                        continuousBurstJob = null
                                        isContinuousBurstActive = false
                                        Toast.makeText(
                                            context,
                                            "연속 촬영을 종료했습니다. (${doneCount}장 · 대기)",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        imageCapture?.let { capture ->
                                            if (!isCameraReady) {
                                                Toast.makeText(
                                                    context,
                                                    "카메라 준비 중입니다.",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                                return@let
                                            }
                                            continuousBurstJob?.cancel()
                                            continuousArcorePending.clear()
                                            isContinuousBurstActive = true
                                            continuousCapturedCount = 0
                                            val useArcoreMeta = arcoreMetaEnabled &&
                                                lensFacing == CameraSelector.LENS_FACING_BACK
                                            continuousBurstJob = scope.launch(Dispatchers.Default) {
                                                continuousBurstUserStop.set(false)
                                                var arcoreFailAccum = 0
                                                var n = 0
                                                try {
                                                    while (isActive && n < CONTINUOUS_CAPTURE_MAX_SHOTS) {
                                                        val cap =
                                                            withContext(Dispatchers.Main) { imageCapture }
                                                                ?: break
                                                        val camReady =
                                                            withContext(Dispatchers.Main) { isCameraReady }
                                                        if (!camReady) {
                                                            delay(100)
                                                            continue
                                                        }
                                                        withContext(Dispatchers.Main) {
                                                            SoftShutterSound.play(volume = 0.3f)
                                                        }
                                                        val shot = withContext(Dispatchers.Main) {
                                                            takePhotoSuspend(context, cap)
                                                        }
                                                        if (shot == null) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context.applicationContext,
                                                                    "연속 촬영: 사진 저장 실패 (${n}장까지 완료)",
                                                                    Toast.LENGTH_LONG,
                                                                ).show()
                                                            }
                                                            break
                                                        }
                                                        val (uri, file) = shot
                                                        withContext(Dispatchers.Main) {
                                                            onImageCaptured(uri)
                                                        }

                                                        if (useArcoreMeta) {
                                                            continuousArcorePending.add(file)
                                                        }

                                                        n++
                                                        withContext(Dispatchers.Main) {
                                                            continuousCapturedCount = n
                                                        }

                                                        if (n >= CONTINUOUS_CAPTURE_MAX_SHOTS) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "연속 촬영 완료 (${CONTINUOUS_CAPTURE_MAX_SHOTS}장)",
                                                                    Toast.LENGTH_LONG,
                                                                ).show()
                                                            }
                                                            break
                                                        }
                                                        delay(CONTINUOUS_CAPTURE_INTERVAL_MS)
                                                    }
                                                } catch (e: CancellationException) {
                                                    val silent =
                                                        continuousBurstUserStop.getAndSet(false) ||
                                                            continuousBurstSilentCancel.getAndSet(false)
                                                    if (!silent) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context.applicationContext,
                                                                "연속 촬영 종료 (${n}장)",
                                                                Toast.LENGTH_SHORT,
                                                            ).show()
                                                        }
                                                    }
                                                    throw e
                                                } finally {
                                                    if (useArcoreMeta) {
                                                        val batch = synchronized(continuousArcorePending) {
                                                            ArrayList(continuousArcorePending).also {
                                                                continuousArcorePending.clear()
                                                            }
                                                        }
                                                        if (batch.isNotEmpty()) {
                                                            withContext(NonCancellable) {
                                                                try {
                                                                    val extraFails = arcoreExclusiveMutex.withLock {
                                                                        runBatchedArcoreMetadataSave(
                                                                            context,
                                                                            batch,
                                                                            prepareExclusiveCamera = {
                                                                                withContext(Dispatchers.Main) {
                                                                                    ProcessCameraProvider
                                                                                        .getInstance(context)
                                                                                        .get()
                                                                                        .unbindAll()
                                                                                    isCameraReady = false
                                                                                }
                                                                            },
                                                                            requestCameraRebind = {
                                                                                withContext(Dispatchers.Main) {
                                                                                    cameraRebindNonce++
                                                                                }
                                                                            },
                                                                        )
                                                                    }
                                                                    arcoreFailAccum += extraFails
                                                                    delay(CAMERA_REBIND_WAIT_AFTER_ARCORE_MS)
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                    arcoreFailAccum += batch.size
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (arcoreFailAccum > 0) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context.applicationContext,
                                                                "연속 촬영: ARCore 저장 실패 ${arcoreFailAccum}장",
                                                                Toast.LENGTH_LONG,
                                                            ).show()
                                                        }
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        isContinuousBurstActive = false
                                                        continuousBurstJob = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                CaptureMode.VIDEO -> {
                                if (!isRecording) {
                                    // 동영상 촬영 시작 - 카메라가 준비되었는지 확인
                                    if (isCameraReady && videoCapture != null) {
                                        videoCapture?.let { capture ->
                                            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)
                                            startVideoRecording(
                                                context,
                                                capture,
                                                onRecordingStarted = { recordingInstance ->
                                                    pendingArcoreForVideo = arcoreMetaEnabled &&
                                                        lensFacing == CameraSelector.LENS_FACING_BACK
                                                    recording = recordingInstance
                                                    isRecording = true
                                                    recordingTime = 0L
                                                    val sessionId = SimpleDateFormat(
                                                        "yyyy-MM-dd-HH-mm-ss-SSS",
                                                        Locale.US
                                                    ).format(System.currentTimeMillis())
                                                    val root = File(context.getExternalFilesDir(null), "datasets")
                                                    if (!root.exists()) {
                                                        root.mkdirs()
                                                    }
                                                    val sessionFolder = File(root, sessionId).apply { mkdirs() }
                                                    datasetDir = sessionFolder
                                                    pendingVideoDatasetDirForArcore = sessionFolder
                                                    pendingVideoDatasetPathForArcore = sessionFolder.absolutePath
                                                    // 동영상 촬영 시작 시 방위각 기준 설정
                                                    baseAzimuthDegrees = azimuthDegrees
                                                    capturedSectors = emptySet()
                                                    basePitchDegrees = pitchDegrees
                                                    currentPitchIndex = 0
                                                },
                                                onVideoSaved = { uri, videoFile ->
                                                    val datasetDirForCleanup =
                                                        pendingVideoDatasetDirForArcore
                                                            ?: pendingVideoDatasetPathForArcore?.let { p ->
                                                                File(p).takeIf { it.isDirectory }
                                                            }
                                                    val doArcore = pendingArcoreForVideo
                                                    pendingArcoreForVideo = false
                                                    pendingVideoDatasetDirForArcore = null
                                                    pendingVideoDatasetPathForArcore = null
                                                    onVideoCaptured(uri)
                                                    if (doArcore) {
                                                        scope.launch(Dispatchers.Default) {
                                                            arcoreExclusiveMutex.withLock {
                                                            try {
                                                                withContext(Dispatchers.Main) {
                                                                    val provider =
                                                                        ProcessCameraProvider.getInstance(
                                                                            context,
                                                                        ).get()
                                                                    provider.unbindAll()
                                                                    isCameraReady = false
                                                                }
                                                                delay(550L)

                                                                val durationMs = withContext(Dispatchers.IO) {
                                                                    try {
                                                                        MediaMetadataRetriever().use { r ->
                                                                            r.setDataSource(videoFile.absolutePath)
                                                                            r.extractMetadata(
                                                                                MediaMetadataRetriever.METADATA_KEY_DURATION,
                                                                            )?.toLongOrNull() ?: 0L
                                                                        }
                                                                    } catch (_: Exception) {
                                                                        0L
                                                                    }
                                                                }

                                                                val timelineJson =
                                                                    withContext(Dispatchers.IO) {
                                                                        ArcorePoseSnapshotter
                                                                            .captureFullVideoTimelineOrNull(
                                                                                context,
                                                                                videoFile,
                                                                            )
                                                                    }
                                                                if (timelineJson == null) {
                                                                    withContext(Dispatchers.Main) {
                                                                        val msg =
                                                                            if (ArcorePoseSnapshotter.availabilityInstalled(
                                                                                    context,
                                                                                )
                                                                            ) {
                                                                                "동영상 길이에 맞춘 ARCore 타임라인을 만들지 못했습니다. 다시 시도해 주세요."
                                                                            } else {
                                                                                "ARCore가 없어 포즈 JSON은 비어 저장됩니다. ARCore 설치 후 이용할 수 있습니다."
                                                                            }
                                                                        Toast.makeText(
                                                                            context,
                                                                            msg,
                                                                            Toast.LENGTH_LONG,
                                                                        ).show()
                                                                    }
                                                                }

                                                                val rootJson = timelineJson
                                                                    ?: JSONObject()
                                                                        .put("mediaType", "video_full_timeline")
                                                                        .put("videoFileName", videoFile.name)
                                                                        .put("durationMs", durationMs)
                                                                        .put("sampleIntervalMs", 33L)
                                                                        .put(
                                                                            "captureNoteKo",
                                                                            if (ArcorePoseSnapshotter.availabilityInstalled(
                                                                                    context,
                                                                                )
                                                                            ) {
                                                                                "ARCore 세션을 열었으나 포즈 타임라인을 수집하지 못했습니다."
                                                                            } else {
                                                                                "ARCore가 설치되어 있지 않습니다. frames는 비어 있습니다."
                                                                            },
                                                                        )
                                                                        .put("frames", JSONArray())

                                                                withContext(Dispatchers.IO) {
                                                                    JsonLibrary.saveArCoreFramesJson(
                                                                        context,
                                                                        rootJson,
                                                                    )
                                                                    ArcoreLibrary.saveVideoFullTimelineArCoreZip(
                                                                        context,
                                                                        videoFile,
                                                                        rootJson.toString(),
                                                                    )
                                                                    datasetDirForCleanup?.let { d ->
                                                                        if (d.exists() && d.isDirectory) {
                                                                            val fs = d.listFiles()
                                                                            if (fs == null || fs.isEmpty()) {
                                                                                d.delete()
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(
                                                                        context.applicationContext,
                                                                        "동영상 ARCore 저장 오류: ${e.message ?: e.javaClass.simpleName}",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                }
                                                            } finally {
                                                                withContext(Dispatchers.Main) {
                                                                    cameraRebindNonce++
                                                                }
                                                            }
                                                            }
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                } else {
                                    // 동영상 촬영 중지
                                    mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                                    recording?.stop()
                                    recording = null
                                    isRecording = false
                                    recordingTime = 0L

                                    if (cameraEntryMode == CameraEntryMode.MOBILE_SPACE) {
                                        mobileSpaceSession.persistToDisk()
                                        mobileSpaceUiRev++
                                    }

                                    // [추가] 빈 데이터셋 폴더 정리 — ImageCapture 비동기 저장 중이면 비어 보일 수 있어
                                    // ARCore·데이터셋 ZIP을 위해 pendingArcoreForVideo인 경우 즉시 삭제하지 않는다.
                                    val targetDir = datasetDir
                                    val skipImmediateEmptyCleanup = pendingArcoreForVideo
                                    if (targetDir != null && targetDir.exists() && targetDir.isDirectory && !skipImmediateEmptyCleanup) {
                                        scope.launch(Dispatchers.IO) {
                                            delay(2800L)
                                            if (!targetDir.exists() || !targetDir.isDirectory) return@launch
                                            val files = targetDir.listFiles()
                                            if (files == null || files.isEmpty()) {
                                                targetDir.delete()
                                            }
                                        }
                                    }

                                    datasetDir = null
                                    // 동영상 촬영 종료 시 상태 초기화
                                    baseAzimuthDegrees = null
                                    capturedSectors = emptySet()
                                    basePitchDegrees = null
                                    currentPitchIndex = 0
                                }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (captureMode) {
                        CaptureMode.PHOTO -> {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = "촬영",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp),
                            )
                        }

                        CaptureMode.CONTINUOUS -> {
                            if (isContinuousBurstActive) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.BurstMode,
                                    contentDescription = "연속 촬영",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }

                        CaptureMode.VIDEO -> {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = "동영상 촬영",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 전면/후면 카메라 전환 버튼
            Icon(
                imageVector = Icons.Filled.Cameraswitch,
                contentDescription = "카메라 전환",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                    }
            )
        }

        // 링은 촬영 버튼 내부로 이동
    }
}

@Composable
fun CameraEntryScreen(
    selectedMode: CameraEntryMode,
    onModeSelected: (CameraEntryMode) -> Unit
) {
    val palette = LocalAppUiPalette.current
    val gridGap = 14.dp
    val screenBg = if (palette.isDark) Color.Black else Color.White
    val titleColor = if (palette.isDark) Color.White else Color.Black
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Text(
            text = "카메라",
            color = titleColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gridGap)
            ) {
                CameraEntryPictogramTile(
                    label = "경차 촬영",
                    pictogramRes = R.drawable.ic_camera_mode_object,
                    isSelected = selectedMode == CameraEntryMode.OBJECT,
                    onClick = { onModeSelected(CameraEntryMode.OBJECT) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
                CameraEntryPictogramTile(
                    label = "중형 차량 촬영",
                    pictogramRes = R.drawable.ic_camera_mode_space_2d,
                    isSelected = selectedMode == CameraEntryMode.SPACE_2D,
                    onClick = { onModeSelected(CameraEntryMode.SPACE_2D) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
            Spacer(modifier = Modifier.height(gridGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gridGap)
            ) {
                CameraEntryPictogramTile(
                    label = "대형 차량 촬영",
                    pictogramRes = R.drawable.ic_camera_mode_space_3d,
                    isSelected = selectedMode == CameraEntryMode.SPACE_3D,
                    onClick = { onModeSelected(CameraEntryMode.SPACE_3D) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
                CameraEntryPictogramTile(
                    label = "사고 현장 촬영",
                    pictogramRes = R.drawable.ic_camera_mode_mobile_space,
                    isSelected = selectedMode == CameraEntryMode.MOBILE_SPACE,
                    onClick = { onModeSelected(CameraEntryMode.MOBILE_SPACE) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun CameraEntryPictogramImage(
    pictogramRes: Int,
    ink: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(
        initialValue = CameraEntryPictogramCache.peek(pictogramRes, ink),
        key1 = pictogramRes,
        key2 = ink,
    ) {
        value = CameraEntryPictogramCache.ensureLoaded(context, pictogramRes, ink)
    }
    val bitmap = bmp
    if (bitmap != null) {
        Image(
            painter = remember(bitmap) {
                BitmapPainter(bitmap, filterQuality = FilterQuality.Low)
            },
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
private fun CameraEntryPictogramTile(
    label: String,
    pictogramRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppUiPalette.current
    val shape = RoundedCornerShape(20.dp)
    val cardBg = if (palette.isDark) Color.Black else Color.White
    val ink = if (palette.isDark) Color.White else Color.Black
    val borderColor = when {
        isSelected -> if (palette.isDark) Color.White else Color.Black
        palette.isDark -> Color.White.copy(alpha = 0.55f)
        else -> Color.Black.copy(alpha = 0.55f)
    }
    val outerBorder = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    Column(
        modifier = modifier
            .fillMaxSize()
            .border(outerBorder, shape)
            .clip(shape)
            .background(cardBg, shape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CameraEntryPictogramImage(
                pictogramRes = pictogramRes,
                ink = ink,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = label,
            color = ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun TopMenuPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .border(1.dp, Color.White, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TopMenuSegmentedTriple(
    leftText: String,
    midText: String,
    rightText: String,
    selectedIndex: Int,
    onLeftClick: () -> Unit,
    onMidClick: () -> Unit,
    onRightClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .border(1.dp, Color.White, RoundedCornerShape(18.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (selectedIndex == 0) Color.White else Color.Black)
                .clickable { onLeftClick() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = leftText,
                color = if (selectedIndex == 0) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (selectedIndex == 1) Color.White else Color.Black)
                .clickable { onMidClick() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = midText,
                color = if (selectedIndex == 1) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (selectedIndex == 2) Color.White else Color.Black)
                .clickable { onRightClick() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rightText,
                color = if (selectedIndex == 2) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun TopMenuSegmented(
    leftText: String,
    rightText: String,
    isLeftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        bottomStart = 18.dp,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(if (isLeftSelected) Color.White else Color.Black)
                .clickable { onLeftClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leftText,
                color = if (isLeftSelected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .background(if (!isLeftSelected) Color.White else Color.Black)
                .clickable { onRightClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rightText,
                color = if (!isLeftSelected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
