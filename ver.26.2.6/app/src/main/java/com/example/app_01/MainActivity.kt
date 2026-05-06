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
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
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
import android.content.pm.ServiceInfo
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
import androidx.compose.material.icons.outlined.Build
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.core.app.ServiceCompat
import androidx.core.content.FileProvider
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app_01.ui.theme.App_01Theme
import java.io.BufferedOutputStream
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

// 서버 설정
private const val UPLOAD_ENDPOINT = "/upload"
private const val STATUS_ENDPOINT = "/status"
private const val DOWNLOAD_ENDPOINT = "/download"
/** FastAPI `GET /results/{task_id}` — 결과 파일 목록 + 개별 다운로드 URL */
private const val RESULTS_ENDPOINT = "/results"
/** 서버에서 PLY·대용량 결과 수신 시 네트워크→파일 스트리밍 복사 버퍼 (기본 8KB보다 처리량↑). */
private const val SERVER_DOWNLOAD_BUFFER_BYTES = 512 * 1024
private const val DEFAULT_SERVER_ADDRESS = "192.168.0.88"
private const val DEFAULT_SERVER_PORT = 8000
private const val DEFAULT_USE_HTTPS = false // HTTP 기본값

// SAM3 배경제거 서버 (sam3_server.py, 기본 포트 8001)
private const val SAM3_BG_REMOVE_ENDPOINT = "/bg-remove"
private const val SAM3_DEFAULT_PORT = 8001


// SharedPreferences 키
private const val PREF_SERVER_ADDRESS = "server_address"
private const val PREF_SERVER_PORT = "server_port"
private const val PREF_USE_HTTPS = "use_https"

// SharedPreferences에서 서버 주소 가져오기
internal fun getServerAddress(context: Context): String {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getString(PREF_SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS) ?: DEFAULT_SERVER_ADDRESS
}

// SharedPreferences에서 서버 포트 가져오기
internal fun getServerPort(context: Context): Int {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getInt(PREF_SERVER_PORT, DEFAULT_SERVER_PORT)
}

// SharedPreferences에서 HTTPS 사용 여부 가져오기
internal fun getUseHttps(context: Context): Boolean {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getBoolean(PREF_USE_HTTPS, DEFAULT_USE_HTTPS)
}

/** 이미지 URI의 가로·세로 픽셀 크기 반환 (inJustDecodeBounds) */
internal fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0) return null

        // EXIF 회전 확인: 90/270도 회전된 이미지는 가로/세로를 교환해야
        // Coil이 EXIF를 반영해 표시하므로, 터치 좌표 계산도 표시 기준으로 맞춰야 함
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_TRANSPOSE -> h to w  // 가로/세로 교환
            else -> w to h
        }
    } catch (_: Exception) {
        null
    }
}

/** OOM 방지: 최대 변 길이 이하로 디코딩. EXIF 회전을 적용해 표시 방향과 일치시킴 */
internal fun decodeBitmapWithMaxDimension(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        // 원본 크기 확인
        val sizeOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, sizeOpts) }
        val rawW = sizeOpts.outWidth
        val rawH = sizeOpts.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        // EXIF 회전 정보 읽기
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val rotates90or270 = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE

        // 회전 후 실효 크기 기준으로 sampleSize 계산
        val effectiveW = if (rotates90or270) rawH else rawW
        val effectiveH = if (rotates90or270) rawW else rawH
        var sampleSize = 1
        if (effectiveW > maxDim || effectiveH > maxDim) {
            val halfW = effectiveW / 2
            val halfH = effectiveH / 2
            while (halfW / sampleSize >= maxDim && halfH / sampleSize >= maxDim) {
                sampleSize *= 2
            }
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inJustDecodeBounds = false
        }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        // 회전 행렬 적용 (Coil과 동일한 방향으로 보정)
        val rotationDeg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> 180f
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE   -> 270f
            else -> 0f
        }
        val needsMirror = orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE

        if (rotationDeg == 0f && !needsMirror) return raw

        val matrix = Matrix().apply {
            if (needsMirror) postScale(-1f, 1f, raw.width / 2f, raw.height / 2f)
            if (rotationDeg != 0f) postRotate(rotationDeg, raw.width / 2f, raw.height / 2f)
        }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            .also { if (it !== raw) raw.recycle() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// SharedPreferences에 서버 설정 저장하기
internal fun saveServerSettings(context: Context, address: String, port: Int, useHttps: Boolean) {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    prefs.edit()
        .putString(PREF_SERVER_ADDRESS, address)
        .putInt(PREF_SERVER_PORT, port)
        .putBoolean(PREF_USE_HTTPS, useHttps)
        .apply()
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ClaudeChatClient.init(applicationContext)

        // (삭제) 광택/반사 제거 옵션 제거에 따라 OpenCV 초기화 제거

        setContent {
            var themeMode by remember { mutableStateOf(readAppUiThemeMode(this@MainActivity)) }
            val palette = remember(themeMode) { appUiPaletteFor(themeMode) }
            val setThemeMode = remember {
                { m: AppUiThemeMode ->
                    themeMode = m
                    writeAppUiThemeMode(this@MainActivity, m)
                }
            }
            App_01Theme(
                darkTheme = themeMode == AppUiThemeMode.DARK,
                dynamicColor = false
            ) {
                CompositionLocalProvider(
                    LocalAppUiPalette provides palette,
                    LocalAppUiThemeMode provides themeMode,
                    LocalSetAppUiThemeMode provides setThemeMode
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        CameraApp(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var showLlmApiKeySettings by remember { mutableStateOf(false) }
    var showServerSettings by remember { mutableStateOf(false) }
    var showSensorCheck by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(MainTab.CAMERA) }
    var selectedLibraryTab by remember { mutableStateOf(LibraryTab.GALLERY) }
    /** true: 갤럭시 갤러리 스타일 앨범 허브, false: 선택한 라이브러리 구역 */
    var showLibraryHub by remember { mutableStateOf(true) }
    /** AI CAD 라이브러리 목록 갱신 (채팅에서 STL 저장 시 증가) */
    var aiCadLibraryVersion by remember { mutableStateOf(0) }
    var cameraEntryMode by remember { mutableStateOf(CameraEntryMode.OBJECT) }
    var isCameraActive by remember { mutableStateOf(false) }
    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var lastCapturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    // ── 최초 실행 시 필수 권한 일괄 요청 ─────────────────────────────────────
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val allPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            results[Manifest.permission.READ_MEDIA_IMAGES] == true
        else
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        prefs.edit().putBoolean("permissions_requested", true).apply()
    }

    LaunchedEffect(Unit) {
        val alreadyRequested = prefs.getBoolean("permissions_requested", false)
        if (!alreadyRequested) {
            val permList = buildList {
                add(Manifest.permission.CAMERA)
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            allPermissionsLauncher.launch(permList.toTypedArray())
        } else {
            // 이미 요청된 적 있으면 카메라·저장소만 개별 확인
            if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            if (!hasStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                else
                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // 갤러리 그리드 스크롤 상태 — 이미지 상세 화면 이동 후 복귀 시 위치 유지
    val galleryGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val galleryScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val images = withContext(Dispatchers.IO) { loadCapturedMediaSync(context) }
        capturedImages = images
        if (images.isNotEmpty()) {
            lastCapturedImageUri = images.first()
        }
    }

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    var viewingMediaList by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var pending3dgsServerAutoSend by remember { mutableStateOf<Pending3dgsServerAutoSend?>(null) }
    var serverPipelineCompleteBundle by remember { mutableStateOf<ServerPipelineResultBundle?>(null) }
    var serverArtifactLibraryVersion by remember { mutableIntStateOf(0) }
    var server3dgsLlmAutoHandledTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val rootPalette = LocalAppUiPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(rootPalette.background)
    ) {
        if (selectedMediaUri != null && viewingMediaList.isNotEmpty()) {
            MediaDetailScreen(
                mediaList = viewingMediaList,
                initialIndex = selectedMediaIndex,
                onBack = {
                    selectedMediaUri = null
                    // 갤러리로 복귀 시 마지막으로 본 이미지가 있는 행으로 스크롤
                    galleryScope.launch {
                        galleryGridState.scrollToItem(
                            gridItemIndexForMediaIndex(selectedMediaIndex, capturedImages, context)
                        )
                    }
                },
                onMediaChanged = { index ->
                    if (index in viewingMediaList.indices) {
                        selectedMediaUri = viewingMediaList[index]
                        selectedMediaIndex = index
                    }
                },
                onGalleryUpdated = {
                    galleryScope.launch(Dispatchers.IO) {
                        val images = loadCapturedMediaSync(context)
                        withContext(Dispatchers.Main) {
                            capturedImages = images
                            if (images.isNotEmpty()) lastCapturedImageUri = images.first()
                        }
                    }
                }
            )
        } else if (showLlmApiKeySettings) {
            LlmApiKeySettingsScreen(
                onBack = { showLlmApiKeySettings = false }
            )
        } else if (showServerSettings) {
            ServerSettingsScreen(
                onBack = { showServerSettings = false }
            )
        } else if (showSensorCheck) {
            SensorCheckScreen(
                onBack = { showSensorCheck = false }
            )
        } else if (showPermissions) {
            PermissionManagementScreen(
                onBack = { showPermissions = false }
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    MainTab.LIBRARY -> {
                        GalleryScreen(
                            images = capturedImages,
                            libraryTab = selectedLibraryTab,
                            aiCadLibraryVersion = aiCadLibraryVersion,
                            showLibraryHub = showLibraryHub,
                            onLibraryHubVisibilityChange = { showLibraryHub = it },
                            onLibraryTabChange = { selectedLibraryTab = it },
                            onMediaSelected = { uri, list ->
                                viewingMediaList = list
                                val index = list.indexOf(uri)
                                if (index >= 0) {
                                    selectedMediaIndex = index
                                    selectedMediaUri = uri
                                }
                            },
                            onImageDeleted = {
                                galleryScope.launch(Dispatchers.IO) {
                                    val images = loadCapturedMediaSync(context)
                                    withContext(Dispatchers.Main) {
                                        capturedImages = images
                                        if (images.isNotEmpty()) {
                                            lastCapturedImageUri = images.first()
                                        } else {
                                            lastCapturedImageUri = null
                                        }
                                    }
                                }
                            },
                            onAiCadLibraryInvalidate = { aiCadLibraryVersion++ },
                            galleryGridState = galleryGridState,
                            serverPipelineCompleteBundle = serverPipelineCompleteBundle,
                            onServerPipelineCompleteBundleChange = { bundle ->
                                serverPipelineCompleteBundle = bundle
                                if (bundle != null) serverArtifactLibraryVersion++
                            },
                            serverArtifactLibraryVersion = serverArtifactLibraryVersion,
                            onEnqueueBackground3dgsFromBundle = { bundle ->
                                if (bundle.taskId !in server3dgsLlmAutoHandledTaskIds) {
                                    server3dgsLlmAutoHandledTaskIds =
                                        server3dgsLlmAutoHandledTaskIds + bundle.taskId
                                    val payload = buildPoliceInsurance3dgsPayload(context, bundle)
                                    val jobPayload = Pending3dgsServerAutoSend(
                                        nonce = System.nanoTime(),
                                        promptText = payload.first,
                                        imageUris = payload.second,
                                        switchToAiTab = false,
                                        sourceServerTaskId = bundle.taskId
                                    )
                                    galleryScope.launch {
                                        val ok = runServer3dgsAnalysisInBackground(context, jobPayload)
                                        if (ok) {
                                            Toast.makeText(
                                                context,
                                                "3DGS 분석을 새 대화로 저장했습니다. AI 탭에서 확인할 수 있습니다.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "백그라운드 3DGS 분석에 실패했습니다. API 키·네트워크를 확인하세요.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onServerPipelineOpenImageViewer = { uris, startIndex ->
                                if (uris.isNotEmpty()) {
                                    viewingMediaList = uris
                                    val i = startIndex.coerceIn(0, uris.size - 1)
                                    selectedMediaIndex = i
                                    selectedMediaUri = uris[i]
                                }
                            },
                            onServerPipelineStart3dgsAi = { pending ->
                                pending.sourceServerTaskId?.let { tid ->
                                    server3dgsLlmAutoHandledTaskIds =
                                        server3dgsLlmAutoHandledTaskIds + tid
                                }
                                pending3dgsServerAutoSend = pending
                                selectedTab = MainTab.CLAUDE
                            }
                        )
                    }
                    MainTab.CLAUDE -> {
                        ClaudeChatScreen(
                            galleryImages = capturedImages,
                            onGalleryUpdated = {
                                galleryScope.launch(Dispatchers.IO) {
                                    val images = loadCapturedMediaSync(context)
                                    withContext(Dispatchers.Main) {
                                        capturedImages = images
                                        if (images.isNotEmpty()) lastCapturedImageUri = images.first()
                                    }
                                }
                            },
                            onAiCadSavedToLibrary = { aiCadLibraryVersion++ },
                            pending3dgsServerAutoSend = pending3dgsServerAutoSend,
                            onPending3dgsServerAutoSendConsumed = { pending3dgsServerAutoSend = null }
                        )
                    }
                    MainTab.CAMERA -> {
                        if (hasCameraPermission) {
                            if (isCameraActive) {
                                BackHandler {
                                    isCameraActive = false
                                }
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CameraScreen(
                                        cameraEntryMode = cameraEntryMode,
                                        lastCapturedImageUri = lastCapturedImageUri,
                                        onImageCaptured = { uri ->
                                            lastCapturedImageUri = uri
                                            galleryScope.launch(Dispatchers.IO) {
                                                val images = loadCapturedMediaSync(context)
                                                withContext(Dispatchers.Main) {
                                                    capturedImages = images
                                                    if (images.isNotEmpty()) {
                                                        lastCapturedImageUri = images.first()
                                                    }
                                                }
                                            }
                                        },
                                        onVideoCaptured = { uri ->
                                            lastCapturedImageUri = uri
                                            galleryScope.launch(Dispatchers.IO) {
                                                val images = loadCapturedMediaSync(context)
                                                withContext(Dispatchers.Main) {
                                                    capturedImages = images
                                                    if (images.isNotEmpty()) {
                                                        lastCapturedImageUri = images.first()
                                                    }
                                                }
                                            }
                                        },
                                        onGalleryClick = {
                                            selectedLibraryTab = LibraryTab.GALLERY
                                            showLibraryHub = false
                                            selectedTab = MainTab.LIBRARY
                                            isCameraActive = false
                                        }
                                    )
                                }
                            } else {
                                CameraEntryScreen(
                                    selectedMode = cameraEntryMode,
                                    onModeSelected = { mode ->
                                        cameraEntryMode = mode
                                        isCameraActive = true
                                    }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "카메라 권한이 필요합니다",
                                    fontSize = 18.sp,
                                    color = LocalAppUiPalette.current.onBackground
                                )
                            }
                        }
                    }
                    MainTab.CREATE -> {
                        Mobile3dGsScreen()
                    }
                    MainTab.PROFILE -> {
                        ProfileScreen(
                            onLlmApiKeyClick = { showLlmApiKeySettings = true },
                            onServerSettingsClick = { showServerSettings = true },
                            onSensorCheckClick = { showSensorCheck = true },
                            onPermissionsClick = { showPermissions = true }
                        )
                    }
                }
            }

            // 키보드가 열리면 BottomNavigationBar를 숨김: BottomNavBar가 ClaudeChatScreen과 키보드 사이에 끼어
            // imePadding()이 80dp 덜 밀어올려 검은 공간이 생기는 문제를 방지
            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            val showBottomBar = !(selectedTab == MainTab.CAMERA && isCameraActive) && !imeVisible
            if (showBottomBar) {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        if (tab == MainTab.LIBRARY) {
                            showLibraryHub = true
                        }
                        if (tab != MainTab.CAMERA) {
                            isCameraActive = false
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun MediaDetailScreen(
    mediaList: List<Uri>,
    initialIndex: Int,
    onBack: () -> Unit,
    onMediaChanged: (Int) -> Unit,
    onGalleryUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 내부 변경 가능한 리스트 (삭제 시 즉시 반영)
    val mutableMediaList = remember(mediaList) { mediaList.toMutableStateList() }
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, mediaList.size - 1)) }

    // 핀치 줌 / 패닝 상태
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // 필름스트립 스크롤 상태
    val filmstripState = rememberLazyListState()

    // 옵션 바 상태
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var showImageEdit by remember { mutableStateOf(false) }

    // 인덱스 변경 시 콜백 + 줌 리셋 + 필름스트립 자동 스크롤
    LaunchedEffect(currentIndex) {
        onMediaChanged(currentIndex)
        scale = 1f; offsetX = 0f; offsetY = 0f
        if (mutableMediaList.size > 1) {
            filmstripState.animateScrollToItem(maxOf(0, currentIndex - 2))
        }
    }

    // 액션 메시지 3초 후 자동 소거
    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            kotlinx.coroutines.delay(3000)
            actionMessage = null
        }
    }

    val currentMediaUri = if (currentIndex in mutableMediaList.indices) mutableMediaList[currentIndex] else null
    val isVideo = currentMediaUri?.let { isVideoUri(context, it) } ?: false

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ─── 메인 미디어 영역 (필름스트립 + 옵션 바 위쪽) ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (mutableMediaList.size > 1) 168.dp else 64.dp)
                .fillMaxHeight()
        ) {
            if (currentMediaUri != null) {
                if (isVideo) {
                    // 동영상 재생
                    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
                    LaunchedEffect(currentMediaUri) {
                        videoViewRef?.let { view ->
                            view.stopPlayback()
                            view.setVideoURI(currentMediaUri)
                            val mc = MediaController(context)
                            mc.setAnchorView(view)
                            view.setMediaController(mc)
                            view.start()
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(currentMediaUri)
                                val mc = MediaController(ctx)
                                mc.setAnchorView(this)
                                setMediaController(mc)
                                start()
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view -> videoViewRef = view }
                    )
                } else {
                    // 이미지: 핀치 줌(2손가락) + 패닝(줌된 상태) + 스와이프 내비게이션(1손가락·줌 해제)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentIndex) {
                                // size는 PointerInputScope의 프로퍼티 → awaitPointerEventScope 진입 전 캡처
                                val viewWidth  = size.width.toFloat()
                                val viewHeight = size.height.toFloat()

                                // 오프셋 경계 클램핑 헬퍼:
                                // scale 배율에서 이미지가 뷰 밖으로 나가지 않도록 최대 이동 범위를 제한
                                // maxOffset = (scale - 1) × viewDim / 2
                                fun clampOffsets() {
                                    val maxX = ((scale - 1f) * viewWidth  / 2f).coerceAtLeast(0f)
                                    val maxY = ((scale - 1f) * viewHeight / 2f).coerceAtLeast(0f)
                                    offsetX = offsetX.coerceIn(-maxX, maxX)
                                    offsetY = offsetY.coerceIn(-maxY, maxY)
                                }

                                // 단일 awaitPointerEventScope 블록으로 핀치줌+스와이프를 모두 처리
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitFirstDown(requireUnconsumed = false)

                                        var dragX = 0f
                                        var pinching = false
                                        var prevDist = 0f

                                        // 손가락이 모두 떼어질 때까지 이벤트 처리
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) break

                                            if (pressed.size >= 2) {
                                                // ─ 두 손가락: 핀치 줌 + 패닝 ─
                                                pinching = true
                                                val p1 = pressed[0]; val p2 = pressed[1]
                                                val dx = p1.position.x - p2.position.x
                                                val dy = p1.position.y - p2.position.y
                                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                                if (prevDist > 0f && dist > 0f) {
                                                    scale = (scale * dist / prevDist).coerceIn(1f, 5f)
                                                    if (scale > 1f) {
                                                        val cx  = (p1.position.x + p2.position.x) / 2f
                                                        val pcx = (p1.previousPosition.x + p2.previousPosition.x) / 2f
                                                        val cy  = (p1.position.y + p2.position.y) / 2f
                                                        val pcy = (p1.previousPosition.y + p2.previousPosition.y) / 2f
                                                        offsetX += cx - pcx
                                                        offsetY += cy - pcy
                                                    } else {
                                                        offsetX = 0f; offsetY = 0f
                                                    }
                                                }
                                                prevDist = dist
                                                // 핀치 후 경계 클램핑 적용
                                                clampOffsets()
                                                pressed.forEach { it.consume() }

                                            } else if (pressed.size == 1 && !pinching) {
                                                // ─ 한 손가락: 줌 상태이면 패닝, 아니면 내비게이션 스와이프 ─
                                                val delta = pressed[0].position - pressed[0].previousPosition
                                                if (scale > 1.05f) {
                                                    offsetX += delta.x
                                                    offsetY += delta.y
                                                    // 패닝 후 경계 클램핑 적용
                                                    clampOffsets()
                                                } else {
                                                    dragX += delta.x
                                                }
                                                pressed[0].consume()
                                            }
                                        }

                                        // 스와이프 내비게이션 판정 (핀치 없음 + 줌 해제 상태)
                                        if (!pinching && scale <= 1.05f) {
                                            val threshold = viewWidth * 0.3f
                                            when {
                                                dragX > threshold && currentIndex > 0 -> currentIndex--
                                                dragX < -threshold && currentIndex < mutableMediaList.size - 1 -> currentIndex++
                                            }
                                        }
                                        // scale 1 미만 방지 + 최종 경계 클램핑
                                        if (scale < 1f) { scale = 1f; offsetX = 0f; offsetY = 0f }
                                        else clampOffsets()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(currentMediaUri),
                            contentDescription = "미디어 상세",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // ─── 하단: 필름스트립 + 옵션 바 ────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // 필름스트립 (2장 이상일 때)
            if (mutableMediaList.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(vertical = 8.dp)
                ) {
                    LazyRow(
                        state = filmstripState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(mutableMediaList) { idx, uri ->
                            val isCurrent = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 74.dp else 56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isCurrent) 2.dp else 0.dp,
                                        color = if (isCurrent) Color(0xFF9CD83B) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch { currentIndex = idx }
                                    }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = if (isCurrent) 1f else 0.55f
                                )
                            }
                        }
                    }
                }
            }

            // ─── 옵션 바 (항상 표시) ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 이미지 편집 (연필 아이콘)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (currentMediaUri != null && !isVideo) showImageEdit = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "이미지 편집",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // 구분선
                Box(modifier = Modifier.width(0.5.dp).height(28.dp).background(Color(0xFF444444)))
                // 2차 사물 배경 분리 (브러시 아이콘)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* TODO: 2차 사물 배경 분리 */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "2차 사물 배경 분리",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // 구분선
                Box(modifier = Modifier.width(0.5.dp).height(28.dp).background(Color(0xFF444444)))
                // 이미지 내보내기 (업로드 아이콘)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            currentMediaUri?.let { uri ->
                                scope.launch {
                                    val result = exportImagesToSystemGallery(context, listOf(uri))
                                    actionMessage = if (result.successCount > 0) "갤러리에 내보냈습니다" else "내보내기 실패"
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "이미지 내보내기",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // 구분선
                Box(modifier = Modifier.width(0.5.dp).height(28.dp).background(Color(0xFF444444)))
                // 이미지 삭제 (휴지통 아이콘)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "이미지 삭제",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 액션 메시지 토스트 (내보내기 결과 등)
        actionMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(msg, color = Color.White, fontSize = 14.sp)
            }
        }

        // 이미지 편집 화면: 외부 Box 안에서 오버레이로 표시해야 전체 화면 덮기 가능
        if (showImageEdit && currentMediaUri != null) {
            ImageEditScreen(
                imageUri = currentMediaUri,
                onBack = { showImageEdit = false },
                onSaved = { newUri ->
                    if (newUri != currentMediaUri) {
                        mutableMediaList[currentIndex] = newUri
                    }
                    onGalleryUpdated()
                    showImageEdit = false
                }
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("이미지 삭제") },
            text = { Text("이 이미지를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    currentMediaUri?.let { uri ->
                        scope.launch {
                            try {
                                val file = java.io.File(uri.path ?: "")
                                if (file.exists()) file.delete()
                            } catch (e: Exception) { e.printStackTrace() }
                            val removedIdx = currentIndex
                            mutableMediaList.removeAt(removedIdx)
                            if (mutableMediaList.isEmpty()) {
                                onGalleryUpdated()
                                onBack()
                            } else {
                                currentIndex = removedIdx.coerceIn(0, mutableMediaList.size - 1)
                                onGalleryUpdated()
                            }
                        }
                    }
                }) {
                    Text("삭제", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", color = Color(0xFFAAAAAA))
                }
            },
            containerColor = Color(0xFF2A2A2A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 자르기 모드 지원 타입
// ──────────────────────────────────────────────────────────────────────────────
private enum class CropHandle { NONE, MOVE, NW, NE, SW, SE, N, E, S, W }

internal data class PendingCropData(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val imgAreaTopPx: Float,
    val imgAreaHeightPx: Float,
    val containerW: Float,
    val containerH: Float,
    val cropZoom: Float = 1f,
    val cropPanX: Float = 0f,
    val cropPanY: Float = 0f
)

// ──────────────────────────────────────────────────────────────────────────────
// 이미지 편집 화면
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun ImageEditScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 현재 표시 중인 URI (누끼 따기 결과로 갱신 가능)
    var currentUri by remember { mutableStateOf(imageUri) }

    // 변환 상태
    var buttonRotation by remember { mutableStateOf(0f) }
    var dialDegrees    by remember { mutableStateOf(0f) }
    var isFlipped      by remember { mutableStateOf(false) }
    var pendingCrop    by remember { mutableStateOf<PendingCropData?>(null) }
    var isCropMode     by remember { mutableStateOf(false) }
    var isDrawMode     by remember { mutableStateOf(false) }   // 누끼 따기 모드
    // 자르기 모드 전용 줌/패닝 상태
    var cropZoom  by remember { mutableStateOf(1f) }
    var cropPanX  by remember { mutableStateOf(0f) }
    var cropPanY  by remember { mutableStateOf(0f) }

    val totalRotation = buttonRotation + dialDegrees
    // 현재 URI가 원본과 달라도(누끼 따기 후) hasChanges = true
    val hasChanges    = buttonRotation != 0f || dialDegrees != 0f || isFlipped || pendingCrop != null || currentUri != imageUri

    var showSaveDialog by remember { mutableStateOf(false) }
    var isSaving       by remember { mutableStateOf(false) }

    fun requestBack() { if (hasChanges) showSaveDialog = true else onBack() }

    BackHandler { requestBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 메인 이미지 (모드별 하단 패딩) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, bottom = when {
                    isCropMode -> 60.dp
                    isDrawMode -> 80.dp
                    else       -> 148.dp
                })
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(currentUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ    = totalRotation
                        val flipSign = if (isFlipped) -1f else 1f
                        scaleX       = flipSign * if (isCropMode) cropZoom else 1f
                        scaleY       = if (isCropMode) cropZoom else 1f
                        translationX = if (isCropMode) cropPanX else 0f
                        translationY = if (isCropMode) cropPanY else 0f
                    },
                contentScale = ContentScale.Fit
            )
        }

        // ── 상단 바: X | 이미지 편집 | 원본 복원  저장 ──
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { requestBack() }) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            // 원본 복원 버튼
            TextButton(
                onClick = {
                    buttonRotation = 0f
                    dialDegrees    = 0f
                    isFlipped      = false
                    pendingCrop    = null
                    currentUri     = imageUri
                },
                enabled = hasChanges
            ) {
                Text(
                    "원본 복원",
                    color    = if (hasChanges) Color(0xFFCCCCCC) else Color(0xFF555555),
                    fontSize = 14.sp
                )
            }
            // 저장 버튼
            TextButton(
                onClick  = { showSaveDialog = true },
                enabled  = hasChanges
            ) {
                Text(
                    "저장",
                    color      = if (hasChanges) Color.White else Color(0xFF555555),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── 하단: 다이얼 바 + 기능 버튼 (자르기/그리기 모드가 아닐 때만) ──
        if (!isCropMode && !isDrawMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                EditDialBar(
                    degrees = dialDegrees,
                    onDegreesChanged = { dialDegrees = it }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditActionButton(icon = Icons.Default.RotateLeft, label = "회전") {
                        buttonRotation = ((buttonRotation - 90f) % 360f + 360f) % 360f
                    }
                    EditActionButton(icon = Icons.Default.Flip, label = "좌우반전") {
                        isFlipped = !isFlipped
                    }
                    EditActionButton(icon = Icons.Default.Crop, label = "자르기") {
                        isCropMode = true
                    }
                    EditActionButton(icon = Icons.Default.AutoFixHigh, label = "누끼따기") {
                        isDrawMode = true
                    }
                }
            }
        }

        // ── 자르기 모드 오버레이 ──
        if (isCropMode) {
            CropModeOverlay(
                imageUri          = currentUri,
                topBarDp          = 56.dp,
                bottomBarDp       = 60.dp,
                buttonRotation    = buttonRotation,
                cropZoom          = cropZoom,
                cropPanX          = cropPanX,
                cropPanY          = cropPanY,
                hasEditChanges    = hasChanges,
                onZoomChange      = { z, px, py ->
                    cropZoom = z; cropPanX = px; cropPanY = py
                },
                onConfirm         = { crop ->
                    pendingCrop    = crop
                    isCropMode     = false
                    cropZoom = 1f; cropPanX = 0f; cropPanY = 0f
                    showSaveDialog = true
                },
                onRestoreOriginal = {
                    pendingCrop    = null
                    buttonRotation = 0f
                    dialDegrees    = 0f
                    isFlipped      = false
                    cropZoom = 1f; cropPanX = 0f; cropPanY = 0f
                    isCropMode     = false
                },
                onCancel = {
                    cropZoom = 1f; cropPanX = 0f; cropPanY = 0f
                    isCropMode = false
                }
            )
        }

        // ── 누끼 따기 모드 오버레이 ──
        if (isDrawMode) {
            SubjectSegmentOverlay(
                sourceUri     = currentUri,
                totalRotation = totalRotation,
                isFlipped     = isFlipped,
                pendingCrop   = pendingCrop,
                onResult      = { newUri ->
                    currentUri     = newUri
                    isDrawMode     = false
                    buttonRotation = 0f
                    dialDegrees    = 0f
                    isFlipped      = false
                    pendingCrop    = null
                },
                onCancel = { isDrawMode = false }
            )
        }

        // 저장 중 오버레이
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF9CD83B))
            }
        }
    }

    // ── 저장 확인 다이얼로그 ──
    if (showSaveDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSaveDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "변경된 이미지를 저장하시겠습니까?",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF444444))
                // 편집 이미지로 저장 (원본 덮어쓰기)
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        scope.launch {
                            isSaving = true
                            val saved = saveEditedImage(context, currentUri, totalRotation, isFlipped, overwrite = true, cropData = pendingCrop)
                            isSaving = false
                            onSaved(saved ?: currentUri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("편집 이미지로 저장", color = Color(0xFF9CD83B), fontSize = 15.sp)
                }
                HorizontalDivider(color = Color(0xFF444444))
                // 복사본으로 저장
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        scope.launch {
                            isSaving = true
                            val saved = saveEditedImage(context, currentUri, totalRotation, isFlipped, overwrite = false, cropData = pendingCrop)
                            isSaving = false
                            onSaved(saved ?: currentUri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("복사본으로 저장", color = Color.White, fontSize = 15.sp)
                }
                HorizontalDivider(color = Color(0xFF444444))
                // 저장하지 않음
                TextButton(
                    onClick = { showSaveDialog = false; onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("저장하지 않음", color = Color(0xFFAAAAAA), fontSize = 15.sp)
                }
            }
        }
    }
}

// ── 다이얼 바: 50칸 / 양끝 페이드 아웃 / 검은 배경 ────────────────────────
// ─────────────────────────────────────────────────────────────────────────────
// 누끼 따기 — 영역 직접 드로잉 + U²-Net 사물 분리
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun SubjectSegmentOverlay(
    sourceUri: Uri,
    totalRotation: Float,
    isFlipped: Boolean,
    pendingCrop: PendingCropData?,
    onResult: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()

    val topBarDp  = 56.dp
    val botBarDp  = 80.dp

    // 현재 편집 상태가 반영된 작업용 비트맵 (비동기 준비)
    var workingBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isPreparing   by remember { mutableStateOf(true) }

    // 드로잉 경로 & UI 상태 (LaunchedEffect보다 먼저 선언)
    var pathPoints   by remember { mutableStateOf(listOf<Offset>()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            workingBitmap = withContext(Dispatchers.IO) {
                buildWorkingBitmap(context, sourceUri, totalRotation, isFlipped, pendingCrop)
            }
        } catch (e: Throwable) {
            android.util.Log.e("SubjectSegmentOverlay", "이미지 로딩 오류", e)
            errorMsg = "이미지를 불러오는 데 실패했습니다.\n다시 시도해 주세요."
        } finally {
            isPreparing = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }
        val topPx      = with(density) { topBarDp.toPx() }
        val botPx      = with(density) { botBarDp.toPx() }
        val imgAreaH   = containerH - topPx - botPx

        // 이미지 실제 표시 경계 (ContentScale.Fit 기준)
        var dispLeft by remember { mutableStateOf(0f) }
        var dispTop  by remember { mutableStateOf(topPx) }
        var dispW    by remember { mutableStateOf(containerW) }
        var dispH    by remember { mutableStateOf(imgAreaH) }

        LaunchedEffect(workingBitmap) {
            val bm = workingBitmap ?: return@LaunchedEffect
            val s  = minOf(containerW / bm.width, imgAreaH / bm.height)
            val dW = bm.width  * s
            val dH = bm.height * s
            dispLeft = (containerW - dW) / 2f
            dispTop  = topPx + (imgAreaH - dH) / 2f
            dispW    = dW; dispH = dH
        }

        // ── 작업용 이미지 표시 ──
        workingBitmap?.let { bm ->
            Image(
                bitmap = bm.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(top = topBarDp, bottom = botBarDp)
                    .fillMaxSize()
            )
        }

        // ── 드로잉 캔버스 ──
        val composePath = remember(pathPoints) {
            if (pathPoints.size < 2) null
            else androidx.compose.ui.graphics.Path().apply {
                moveTo(pathPoints[0].x, pathPoints[0].y)
                pathPoints.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            val pts  = mutableListOf(down.position)
                            pathPoints = pts.toList()
                            var event = awaitPointerEvent()
                            while (event.changes.any { it.pressed }) {
                                event.changes.firstOrNull()?.let { ch ->
                                    pts += ch.position
                                    if (pts.size % 3 == 0) pathPoints = pts.toList()
                                    ch.consume()
                                }
                                event = awaitPointerEvent()
                            }
                            pathPoints = pts.toList()
                        }
                    }
                }
        ) {
            composePath?.let { path ->
                drawPath(path, Color.White.copy(alpha = 0.22f), style = Fill)
                drawPath(path, Color(0xFF9CD83B), style = Stroke(width = 3.dp.toPx()))
            }
            if (pathPoints.isNotEmpty()) {
                drawCircle(Color(0xFF9CD83B), radius = 6.dp.toPx(), center = pathPoints[0])
            }
        }

        // ── 상단 바 ──
        val canApply = pathPoints.size >= 6 && workingBitmap != null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarDp)
                .background(Color.Black)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "취소", tint = Color.White)
            }
            Text(
                "영역 그리기",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick  = { pathPoints = emptyList() },
                    enabled  = pathPoints.isNotEmpty()
                ) {
                    Text(
                        "초기화",
                        color    = if (pathPoints.isNotEmpty()) Color(0xFFCCCCCC) else Color(0xFF555555),
                        fontSize = 13.sp
                    )
                }
                TextButton(
                    onClick = {
                        val bm = workingBitmap ?: return@TextButton
                        scope.launch {
                            isProcessing = true
                            val result = performSubjectSegmentation(
                                context   = context,
                                bitmap    = bm,
                                pathPoints = pathPoints,
                                dispLeft  = dispLeft, dispTop = dispTop,
                                dispW     = dispW,    dispH   = dispH
                            )
                            if (result != null) {
                                val outFile = withContext(Dispatchers.IO) {
                                    val f = File(
                                        context.getExternalFilesDir(null),
                                        "seg_${System.currentTimeMillis()}.png"
                                    )
                                    FileOutputStream(f).use { result.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                                    f
                                }
                                isProcessing = false
                                onResult(android.net.Uri.fromFile(outFile))
                            } else {
                                isProcessing = false
                                errorMsg = "사물 분리에 실패했습니다.\n다시 시도해 주세요."
                            }
                        }
                    },
                    enabled = canApply
                ) {
                    Text(
                        "적용",
                        color      = if (canApply) Color.White else Color(0xFF555555),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── 하단 안내 바 ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(botBarDp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (pathPoints.isEmpty())
                    "분리할 사물 주변을 손가락으로 따라 그리세요"
                else
                    "경계를 그렸으면 [적용]을 탭하거나 [초기화]로 다시 그리세요",
                color     = Color(0xFF888888),
                fontSize  = 12.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 24.dp)
            )
        }

        // ── 준비 중 오버레이 ──
        if (isPreparing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF9CD83B))
                    Spacer(Modifier.height(12.dp))
                    Text("이미지 준비 중...", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // ── 처리 중 오버레이 ──
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF9CD83B))
                    Spacer(Modifier.height(12.dp))
                    Text("사물을 분리하는 중...", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // ── 에러 토스트 ──
        errorMsg?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                errorMsg = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC333333), RoundedCornerShape(10.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(msg, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 자르기 오버레이 (이미지 위에 전체 화면으로 덮음)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun CropModeOverlay(
    imageUri: Uri,
    topBarDp: androidx.compose.ui.unit.Dp,
    bottomBarDp: androidx.compose.ui.unit.Dp,
    buttonRotation: Float,
    cropZoom: Float,
    cropPanX: Float,
    cropPanY: Float,
    hasEditChanges: Boolean,           // 외부(회전·뒤집기) 변경 여부
    onZoomChange: (zoom: Float, panX: Float, panY: Float) -> Unit,
    onConfirm: (PendingCropData) -> Unit,
    onRestoreOriginal: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // pointerInput 안에서 항상 최신값을 읽기 위한 래퍼
    val latestZoom = androidx.compose.runtime.rememberUpdatedState(cropZoom)
    val latestPanX = androidx.compose.runtime.rememberUpdatedState(cropPanX)
    val latestPanY = androidx.compose.runtime.rememberUpdatedState(cropPanY)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }
        val topPx      = with(density) { topBarDp.toPx() }
        val imgAreaTop = topPx
        val imgAreaH   = containerH - topPx - with(density) { bottomBarDp.toPx() }

        // 실제 이미지 표시 경계 (이미지 로드 후 업데이트)
        var dispLeft   by remember { mutableStateOf(0f) }
        var dispTop    by remember { mutableStateOf(imgAreaTop) }
        var dispRight  by remember { mutableStateOf(containerW) }
        var dispBottom by remember { mutableStateOf(imgAreaTop + imgAreaH) }

        // 자르기 사각형 상태 (dispLeft/dispTop 기준 초기값)
        var cropLeft  by remember { mutableStateOf(0f) }
        var cropTop   by remember { mutableStateOf(imgAreaTop) }
        var cropW     by remember { mutableStateOf(containerW) }
        var cropH     by remember { mutableStateOf(imgAreaH) }
        var is1to1       by remember { mutableStateOf(false) }
        var activeHdl    by remember { mutableStateOf(CropHandle.NONE) }
        var cropModified by remember { mutableStateOf(false) } // 크롭 영역 조작 여부

        val handleR    = with(density) { 34.dp.toPx() }
        val minCropPx  = with(density) { 80.dp.toPx() }
        val hLen       = with(density) { 22.dp.toPx() }
        val hLineW     = with(density) { 3.dp.toPx() }
        val edgeLen    = hLen * 1.1f   // 모서리 핸들 선 길이 (꼭짓점 L자와 비슷한 크기)
        val edgeLineW  = hLineW * 1.2f // 꼭짓점보다 살짝 두껍게
        // 가로/세로 터치 인식 범위
        val edgeTouchX = with(density) { 48.dp.toPx() }  // N/S: 가로 넓게
        val edgeTouchY = with(density) { 28.dp.toPx() }  // N/S: 세로 보통
        val edgeTouchXv = with(density) { 28.dp.toPx() } // E/W: 가로 보통
        val edgeTouchYv = with(density) { 48.dp.toPx() } // E/W: 세로 넓게

        // 이미지 실제 표시 경계 비동기 계산
        LaunchedEffect(imageUri, buttonRotation) {
            val bounds = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(imageUri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, opts)
                }
                var rawW = opts.outWidth.toFloat()
                var rawH = opts.outHeight.toFloat()
                if (rawW <= 0f || rawH <= 0f) return@withContext null
                val rot90 = ((buttonRotation / 90f).toInt() % 4 + 4) % 4
                if (rot90 % 2 == 1) { rawW = rawH.also { rawH = rawW } }
                val s  = minOf(containerW / rawW, imgAreaH / rawH)
                val dW = rawW * s;  val dH = rawH * s
                val dL = (containerW - dW) / 2f
                val dT = imgAreaTop + (imgAreaH - dH) / 2f
                floatArrayOf(dL, dT, dL + dW, dT + dH)
            } ?: return@LaunchedEffect
            dispLeft   = bounds[0]; dispTop    = bounds[1]
            dispRight  = bounds[2]; dispBottom = bounds[3]
            // 이미지 영역의 90% 크기로 초기 자르기 설정
            val iW = (dispRight - dispLeft) * 0.90f
            val iH = (dispBottom - dispTop) * 0.90f
            cropLeft = dispLeft + (dispRight - dispLeft - iW) / 2f
            cropTop  = dispTop  + (dispBottom - dispTop  - iH) / 2f
            cropW    = iW;  cropH = iH
        }

        // ── 어두운 마스크 + 자르기 테두리 + 격자 + 핸들 ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cr = cropLeft + cropW
            val cb = cropTop  + cropH
            val ov = Color.Black.copy(alpha = 0.6f)
            fun cs(w: Float, h: Float) = androidx.compose.ui.geometry.Size(w, h)

            // 외부 4면 어둡게
            drawRect(ov, Offset.Zero,        cs(containerW, cropTop))
            drawRect(ov, Offset(0f, cb),      cs(containerW, containerH - cb))
            drawRect(ov, Offset(0f, cropTop), cs(cropLeft, cropH))
            drawRect(ov, Offset(cr, cropTop), cs(containerW - cr, cropH))

            // 흰색 테두리
            drawRect(
                color   = Color.White,
                topLeft = Offset(cropLeft, cropTop),
                size    = cs(cropW, cropH),
                style   = androidx.compose.ui.graphics.drawscope.Stroke(width = hLineW * 0.55f)
            )

            // 삼등분 격자선
            for (i in 1..2) {
                val gx = cropLeft + cropW * i / 3f
                val gy = cropTop  + cropH * i / 3f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(gx, cropTop), Offset(gx, cb), strokeWidth = hLineW * 0.3f)
                drawLine(Color.White.copy(alpha = 0.4f), Offset(cropLeft, gy), Offset(cr, gy), strokeWidth = hLineW * 0.3f)
            }

            // L자 꼭짓점 핸들
            fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
                drawLine(Color.White, Offset(cx, cy), Offset(cx + dx, cy), strokeWidth = hLineW)
                drawLine(Color.White, Offset(cx, cy), Offset(cx, cy + dy), strokeWidth = hLineW)
            }
            corner(cropLeft, cropTop,  hLen,  hLen)
            corner(cr,       cropTop, -hLen,  hLen)
            corner(cropLeft, cb,       hLen, -hLen)
            corner(cr,       cb,      -hLen, -hLen)

            // 모서리 중간 핸들 (굵은 가로/세로선)
            val mx = cropLeft + cropW / 2f
            val my = cropTop  + cropH / 2f
            // N (상단 중앙 – 가로선)
            drawLine(Color.White, Offset(mx - edgeLen / 2f, cropTop), Offset(mx + edgeLen / 2f, cropTop), strokeWidth = edgeLineW)
            // S (하단 중앙 – 가로선)
            drawLine(Color.White, Offset(mx - edgeLen / 2f, cb),      Offset(mx + edgeLen / 2f, cb),      strokeWidth = edgeLineW)
            // E (우측 중앙 – 세로선)
            drawLine(Color.White, Offset(cr, my - edgeLen / 2f),      Offset(cr, my + edgeLen / 2f),      strokeWidth = edgeLineW)
            // W (좌측 중앙 – 세로선)
            drawLine(Color.White, Offset(cropLeft, my - edgeLen / 2f), Offset(cropLeft, my + edgeLen / 2f), strokeWidth = edgeLineW)
        }

        // ── 제스처 핸들러 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(is1to1, dispLeft, dispTop, dispRight, dispBottom) {
                    // 이미지 컨테이너 중심 (graphicsLayer 축)
                    val cx = containerW / 2f
                    val cy = imgAreaTop + imgAreaH / 2f
                    val imgDispW = dispRight - dispLeft
                    val imgDispH = dispBottom - dispTop

                    // 현재 줌/패닝을 적용한 유효 이미지 표시 경계 계산
                    fun effBounds(): FloatArray {
                        val z  = latestZoom.value
                        val px = latestPanX.value
                        val py = latestPanY.value
                        return floatArrayOf(
                            cx + (dispLeft   - cx) * z + px,
                            cy + (dispTop    - cy) * z + py,
                            cx + (dispRight  - cx) * z + px,
                            cy + (dispBottom - cy) * z + py
                        )
                    }

                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pos  = down.position
                            val (eL, eT, eR, eB) = effBounds().let {
                                arrayOf(it[0], it[1], it[2], it[3])
                            }
                            val cr2  = cropLeft + cropW
                            val cb2  = cropTop  + cropH
                            fun near(x: Float, y: Float) =
                                kotlin.math.abs(pos.x - x) < handleR && kotlin.math.abs(pos.y - y) < handleR
                            fun nearH(x: Float, y: Float) =
                                kotlin.math.abs(pos.x - x) < edgeTouchX && kotlin.math.abs(pos.y - y) < edgeTouchY
                            fun nearV(x: Float, y: Float) =
                                kotlin.math.abs(pos.x - x) < edgeTouchXv && kotlin.math.abs(pos.y - y) < edgeTouchYv

                            activeHdl = when {
                                near(cropLeft, cropTop)                       -> CropHandle.NW
                                near(cr2,      cropTop)                       -> CropHandle.NE
                                near(cropLeft, cb2)                           -> CropHandle.SW
                                near(cr2,      cb2)                           -> CropHandle.SE
                                nearH(cropLeft + cropW / 2f, cropTop)         -> CropHandle.N
                                nearH(cropLeft + cropW / 2f, cb2)             -> CropHandle.S
                                nearV(cr2,      cropTop + cropH / 2f)         -> CropHandle.E
                                nearV(cropLeft, cropTop + cropH / 2f)         -> CropHandle.W
                                pos.x in cropLeft..cr2 && pos.y in cropTop..cb2 -> CropHandle.MOVE
                                else -> CropHandle.NONE
                            }

                            while (true) {
                                val event   = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size >= 2) {
                                    // ── 두 손가락 핀치: 확대/축소 + 이동 ──
                                    val p1 = pressed[0]; val p2 = pressed[1]
                                    val curDist  = (p2.position - p1.position).getDistance()
                                    val prevDist = (p2.previousPosition - p1.previousPosition).getDistance()
                                    if (prevDist > 1f) {
                                        val z  = latestZoom.value
                                        val px = latestPanX.value; val py = latestPanY.value
                                        val scaleFactor = curDist / prevDist
                                        val newZoom = (z * scaleFactor).coerceIn(1f, 5f)
                                        // 두 손가락 중점이 동일한 이미지 점에 고정되도록 패닝 조정
                                        val midX = (p1.previousPosition.x + p2.previousPosition.x) / 2f
                                        val midY = (p1.previousPosition.y + p2.previousPosition.y) / 2f
                                        val newMidX = (p1.position.x + p2.position.x) / 2f
                                        val newMidY = (p1.position.y + p2.position.y) / 2f
                                        var newPanX = newMidX - cx - ((midX - cx - px) / z) * newZoom
                                        var newPanY = newMidY - cy - ((midY - cy - py) / z) * newZoom
                                        // 패닝 범위 제한 (이미지가 화면에서 너무 벗어나지 않도록)
                                        val maxPX = imgDispW * (newZoom - 1f) / 2f + kotlin.math.abs(dispLeft - cx)
                                        val maxPY = imgDispH * (newZoom - 1f) / 2f + kotlin.math.abs(dispTop  - cy)
                                        newPanX = newPanX.coerceIn(-maxPX, maxPX)
                                        newPanY = newPanY.coerceIn(-maxPY, maxPY)
                                        onZoomChange(newZoom, newPanX, newPanY)
                                    }
                                    pressed.forEach { it.consume() }
                                } else if (pressed.size == 1) {
                                    val ch = pressed[0]
                                    val d  = ch.position - ch.previousPosition
                                    val b  = effBounds()
                                    val efl = b[0]; val eft = b[1]; val efr = b[2]; val efb = b[3]
                                    if (activeHdl != CropHandle.NONE) cropModified = true
                                    if (is1to1) {
                                        // ── 1:1 고정 ──
                                        when (activeHdl) {
                                            CropHandle.MOVE -> {
                                                cropLeft = (cropLeft + d.x).coerceIn(efl, efr - cropW)
                                                cropTop  = (cropTop  + d.y).coerceIn(eft,  efb - cropH)
                                            }
                                            CropHandle.NW, CropHandle.N, CropHandle.W -> {
                                                val pivR = cropLeft + cropW; val pivB = cropTop + cropH
                                                val ds = -(d.x + d.y) / 2f
                                                val ns = (cropW + ds).coerceIn(minCropPx, minOf(pivR - efl, pivB - eft))
                                                cropLeft = (pivR - ns).coerceAtLeast(efl)
                                                cropTop  = (pivB - ns).coerceAtLeast(eft)
                                                cropW = ns; cropH = ns
                                            }
                                            CropHandle.NE -> {
                                                val pivB = cropTop + cropH
                                                val ds = (d.x - d.y) / 2f
                                                val ns = (cropW + ds).coerceIn(minCropPx, minOf(efr - cropLeft, pivB - eft))
                                                cropTop = (pivB - ns).coerceAtLeast(eft)
                                                cropW = ns; cropH = ns
                                            }
                                            CropHandle.SW -> {
                                                val pivR = cropLeft + cropW
                                                val ds = (-d.x + d.y) / 2f
                                                val ns = (cropW + ds).coerceIn(minCropPx, minOf(pivR - efl, efb - cropTop))
                                                cropLeft = (pivR - ns).coerceAtLeast(efl)
                                                cropW = ns; cropH = ns
                                            }
                                            CropHandle.SE, CropHandle.E, CropHandle.S -> {
                                                val ds = (d.x + d.y) / 2f
                                                val ns = (cropW + ds).coerceIn(minCropPx, minOf(efr - cropLeft, efb - cropTop))
                                                cropW = ns; cropH = ns
                                            }
                                            CropHandle.NONE -> {}
                                        }
                                    } else {
                                        // ── 자유 크롭 ──
                                        when (activeHdl) {
                                            CropHandle.MOVE -> {
                                                cropLeft = (cropLeft + d.x).coerceIn(efl, efr - cropW)
                                                cropTop  = (cropTop  + d.y).coerceIn(eft,  efb - cropH)
                                            }
                                            CropHandle.NW -> {
                                                val pivR = cropLeft + cropW; val pivB = cropTop + cropH
                                                val nw = (cropW - d.x).coerceIn(minCropPx, pivR - efl)
                                                val nh = (cropH - d.y).coerceIn(minCropPx, pivB - eft)
                                                cropLeft = pivR - nw; cropTop = pivB - nh
                                                cropW = nw; cropH = nh
                                            }
                                            CropHandle.NE -> {
                                                val pivB = cropTop + cropH
                                                cropW = (cropW + d.x).coerceIn(minCropPx, efr - cropLeft)
                                                val nh = (cropH - d.y).coerceIn(minCropPx, pivB - eft)
                                                cropTop = pivB - nh; cropH = nh
                                            }
                                            CropHandle.SW -> {
                                                val pivR = cropLeft + cropW
                                                val nw = (cropW - d.x).coerceIn(minCropPx, pivR - efl)
                                                cropLeft = pivR - nw; cropW = nw
                                                cropH = (cropH + d.y).coerceIn(minCropPx, efb - cropTop)
                                            }
                                            CropHandle.SE -> {
                                                cropW = (cropW + d.x).coerceIn(minCropPx, efr - cropLeft)
                                                cropH = (cropH + d.y).coerceIn(minCropPx, efb - cropTop)
                                            }
                                            CropHandle.N -> {
                                                val pivB = cropTop + cropH
                                                val nh = (cropH - d.y).coerceIn(minCropPx, pivB - eft)
                                                cropTop = pivB - nh; cropH = nh
                                            }
                                            CropHandle.E -> {
                                                cropW = (cropW + d.x).coerceIn(minCropPx, efr - cropLeft)
                                            }
                                            CropHandle.S -> {
                                                cropH = (cropH + d.y).coerceIn(minCropPx, efb - cropTop)
                                            }
                                            CropHandle.W -> {
                                                val pivR = cropLeft + cropW
                                                val nw = (cropW - d.x).coerceIn(minCropPx, pivR - efl)
                                                cropLeft = pivR - nw; cropW = nw
                                            }
                                            CropHandle.NONE -> {}
                                        }
                                    }
                                    ch.consume()
                                }
                            }
                        }
                    }
                }
        )

        // ── 상단 바 오버레이 (기존 편집 상단 바 위에 덮음) ──
        // 원본 복원: 외부 편집 변경 OR 크롭 조작 시 활성화
        // 저장:     크롭 영역을 실제로 조작했을 때만 활성화
        val canRestore = hasEditChanges || cropModified
        val canSave    = cropModified
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarDp)
                .background(Color.Black)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick  = onRestoreOriginal,
                    enabled  = canRestore
                ) {
                    Text(
                        "원본 복원",
                        color    = if (canRestore) Color(0xFFCCCCCC) else Color(0xFF555555),
                        fontSize = 14.sp
                    )
                }
                TextButton(
                    onClick = {
                        onConfirm(
                            PendingCropData(
                                left            = cropLeft,
                                top             = cropTop,
                                width           = cropW,
                                height          = cropH,
                                imgAreaTopPx    = imgAreaTop,
                                imgAreaHeightPx = imgAreaH,
                                containerW      = containerW,
                                containerH      = containerH,
                                cropZoom        = latestZoom.value,
                                cropPanX        = latestPanX.value,
                                cropPanY        = latestPanY.value
                            )
                        )
                    },
                    enabled = canSave
                ) {
                    Text(
                        "저장",
                        color      = if (canSave) Color.White else Color(0xFF555555),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── 하단 바: 1:1 토글만 ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomBarDp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val btnBorder = if (is1to1) Color(0xFF9CD83B) else Color(0xFF666666)
            val btnText   = if (is1to1) Color(0xFF9CD83B) else Color(0xFF888888)
            val btnBg     = if (is1to1) Color(0xFF9CD83B).copy(alpha = 0.15f) else Color.Transparent
            Box(
                modifier = Modifier
                    .border(1.5.dp, btnBorder, RoundedCornerShape(6.dp))
                    .background(btnBg, RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        is1to1 = !is1to1
                        if (is1to1) {
                            val sq = minOf(cropW, cropH)
                            val cx = cropLeft + cropW / 2f
                            val cy = cropTop  + cropH / 2f
                            cropW    = sq; cropH = sq
                            cropLeft = (cx - sq / 2f).coerceIn(dispLeft, dispRight  - sq)
                            cropTop  = (cy - sq / 2f).coerceIn(dispTop,  dispBottom - sq)
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("1 : 1", color = btnText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
@Composable
internal fun EditDialBar(
    degrees: Float,
    onDegreesChanged: (Float) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)   // Galaxy 수준으로 더 슬림하게
            .background(Color.Black)
    ) {
        val viewWidthPx = with(density) { maxWidth.toPx() }
        val tickWidthPx = viewWidthPx / 50f   // 화면에 50칸
        val visibleHalf = 27

        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        onDegreesChanged(degrees + delta / tickWidthPx)
                    }
                )
        ) {
            // 현재 각도 텍스트 (중앙 상단)
            Text(
                text     = "${"%.1f".format(degrees)}°",
                color    = Color(0xFF9CD83B),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            )

            // 눈금 Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val centerX  = size.width / 2f
                val midY     = size.height / 2f
                val tickH    = size.height * 0.40f  // 균일 작은 눈금
                val originH  = size.height * 0.80f  // 원점만 약간 길게
                val halfW    = size.width / 2f

                val from = (degrees - visibleHalf).toInt()
                val to   = (degrees + visibleHalf).toInt()

                for (i in from..to) {
                    val x = centerX + (i - degrees) * tickWidthPx
                    if (x < 0f || x > size.width) continue

                    // 중심에서 멀수록 점점 투명하게 (페이드 아웃)
                    val dist       = kotlin.math.abs(x - centerX) / halfW   // 0=중심, 1=끝
                    val fadeAlpha  = ((1f - dist / 0.82f) * 1.1f).coerceIn(0f, 1f)

                    val isOrigin   = i == 0
                    val baseColor  = if (isOrigin) Color(0xFF9CD83B) else Color(0xFFAAAAAA)

                    drawLine(
                        color       = baseColor.copy(alpha = fadeAlpha),
                        start       = Offset(x, midY - (if (isOrigin) originH else tickH) / 2f),
                        end         = Offset(x, midY + (if (isOrigin) originH else tickH) / 2f),
                        strokeWidth = if (isOrigin) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // 중앙 기준 포인터 (고정 녹색 선)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .width(2.dp)
                    .height(18.dp)
                    .background(Color(0xFF9CD83B))
            )
        }
    }
}

// ── 편집 기능 버튼 아이템 ──────────────────────────────────────────────────────
@Composable
internal fun EditActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = Color.White,
            modifier           = Modifier.size(30.dp)
        )
    }
}

// ── 편집된 이미지 저장 (회전·반전 적용 후 파일 쓰기) ─────────────────────────
// ─────────────────────────────────────────────────────────────────────────────
// 현재 편집 상태(회전·반전·크롭)를 적용한 비트맵 생성
// ─────────────────────────────────────────────────────────────────────────────
internal suspend fun buildWorkingBitmap(
    context: Context,
    sourceUri: Uri,
    totalRotation: Float,
    isFlipped: Boolean,
    pendingCrop: PendingCropData?
): android.graphics.Bitmap = withContext(Dispatchers.IO) {
    val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        android.graphics.ImageDecoder.decodeBitmap(
            android.graphics.ImageDecoder.createSource(context.contentResolver, sourceUri)
        ) { dec, _, _ -> dec.isMutableRequired = true }
    } else {
        @Suppress("DEPRECATION")
        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, sourceUri)
    }.copy(android.graphics.Bitmap.Config.ARGB_8888, true)

    val matrix = android.graphics.Matrix()
    if (totalRotation != 0f) matrix.postRotate(totalRotation)
    if (isFlipped) matrix.postScale(-1f, 1f, src.width / 2f, src.height / 2f)

    var bm: android.graphics.Bitmap = if (!matrix.isIdentity) {
        android.graphics.Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    } else src

    if (pendingCrop != null) {
        val rotW = bm.width.toFloat(); val rotH = bm.height.toFloat()
        val s    = minOf(pendingCrop.containerW / rotW, pendingCrop.imgAreaHeightPx / rotH)
        val baseL = (pendingCrop.containerW - rotW * s) / 2f
        val baseT = pendingCrop.imgAreaTopPx + (pendingCrop.imgAreaHeightPx - rotH * s) / 2f
        val cx   = pendingCrop.containerW / 2f
        val cy   = pendingCrop.imgAreaTopPx + pendingCrop.imgAreaHeightPx / 2f
        val z = pendingCrop.cropZoom; val px = pendingCrop.cropPanX; val py = pendingCrop.cropPanY
        val effL = cx + (baseL - cx) * z + px
        val effT = cy + (baseT - cy) * z + py
        val effS = s * z
        val cl = ((pendingCrop.left - effL) / effS).coerceIn(0f, rotW - 1f).toInt()
        val ct = ((pendingCrop.top  - effT) / effS).coerceIn(0f, rotH - 1f).toInt()
        val cw = (pendingCrop.width  / effS).coerceIn(1f, rotW - cl).toInt().coerceAtLeast(1)
        val ch = (pendingCrop.height / effS).coerceIn(1f, rotH - ct).toInt().coerceAtLeast(1)
        bm = android.graphics.Bitmap.createBitmap(bm, cl, ct, cw, ch)
    }
    bm
}

// ─────────────────────────────────────────────────────────────────────────────
// ML Kit Subject Segmentation + 드로잉 경로 마스크 합성
// ─────────────────────────────────────────────────────────────────────────────
internal suspend fun performSubjectSegmentation(
    context: Context,
    bitmap: android.graphics.Bitmap,
    pathPoints: List<Offset>,
    dispLeft: Float,
    dispTop: Float,
    dispW: Float,
    dispH: Float
): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (dispW <= 0f || dispH <= 0f || pathPoints.size < 3) return@withContext null

        // ── OOM 방지: 처리 크기를 최대 768px로 제한 ───────────────────────────
        val MAX_PROC_DIM = 768
        val scaleFactor = if (bitmap.width > MAX_PROC_DIM || bitmap.height > MAX_PROC_DIM)
            MAX_PROC_DIM.toFloat() / maxOf(bitmap.width, bitmap.height)
        else 1f
        val procBitmap = if (scaleFactor < 1f) {
            val nw = (bitmap.width  * scaleFactor).toInt().coerceAtLeast(1)
            val nh = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
            android.graphics.Bitmap.createScaledBitmap(bitmap, nw, nh, true)
        } else bitmap
        val needRecycleProcBm = procBitmap !== bitmap

        try {
            // 1. 경로를 처리용 비트맵 좌표로 변환 (scaleFactor 반영)
            val scaleX = procBitmap.width  / dispW
            val scaleY = procBitmap.height / dispH
            val imgPath = pathPoints.map {
                Offset((it.x - dispLeft) * scaleX, (it.y - dispTop) * scaleY)
            }
            val pw = procBitmap.width.toFloat()
            val ph = procBitmap.height.toFloat()

            // 2. 경로 중심점 계산 → InteractiveSegmenter 가이드 포인트
            val centroidX = imgPath.map { it.x }.average().toFloat().coerceIn(0f, pw)
            val centroidY = imgPath.map { it.y }.average().toFloat().coerceIn(0f, ph)
            val normX = (centroidX / pw).coerceIn(0f, 1f)
            val normY = (centroidY / ph).coerceIn(0f, 1f)

            // 3. MobileSAM (경량화 SAM 계열) 전경 분리
            //    Encoder(26.9MB) + Decoder(15.7MB) ONNX, 카테고리 제한 없음
            val foregroundBm: android.graphics.Bitmap? =
                BackgroundRemovalProcessor.segmentForegroundMobileSAM(context, procBitmap, normX, normY)

            // 4. 경로 마스크 (처리 크기 기준, 흰색 = 선택 영역)
            val pathMask = android.graphics.Bitmap.createBitmap(
                procBitmap.width, procBitmap.height, android.graphics.Bitmap.Config.ARGB_8888
            )
            val pc = android.graphics.Canvas(pathMask)
            val ap = android.graphics.Path().apply {
                moveTo(
                    imgPath[0].x.coerceIn(0f, pw - 1f),
                    imgPath[0].y.coerceIn(0f, ph - 1f)
                )
                imgPath.drop(1).forEach {
                    lineTo(it.x.coerceIn(0f, pw - 1f), it.y.coerceIn(0f, ph - 1f))
                }
                close()
            }
            pc.drawPath(ap, android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            })

            // 5. 합성: (MediaPipe 전경 또는 원본 복사) DST_IN 경로 마스크
            val baseCopied = foregroundBm == null
            val base = foregroundBm
                ?: procBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            val result = android.graphics.Bitmap.createBitmap(
                procBitmap.width, procBitmap.height, android.graphics.Bitmap.Config.ARGB_8888
            )
            val rc = android.graphics.Canvas(result)
            rc.drawBitmap(base, 0f, 0f, null)
            rc.drawBitmap(pathMask, 0f, 0f, android.graphics.Paint().apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
            })
            // 중간 비트맵 해제
            pathMask.recycle()
            if (baseCopied) base.recycle() else foregroundBm?.recycle()

            result
        } finally {
            if (needRecycleProcBm) procBitmap.recycle()
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
internal suspend fun saveEditedImage(
    context: Context,
    originalUri: Uri,
    rotationDegrees: Float,
    isFlipped: Boolean,
    overwrite: Boolean,
    cropData: PendingCropData? = null
): Uri? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        // 비트맵 로드
        val src = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, originalUri)
            android.graphics.ImageDecoder.decodeBitmap(source) { dec, _, _ ->
                dec.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, originalUri)
        }.copy(android.graphics.Bitmap.Config.ARGB_8888, true)

        // 회전/반전 행렬 적용
        val matrix = android.graphics.Matrix()
        if (rotationDegrees != 0f) matrix.postRotate(rotationDegrees)
        if (isFlipped)             matrix.postScale(-1f, 1f, src.width / 2f, src.height / 2f)

        var result = if (!matrix.isIdentity) {
            android.graphics.Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } else src

        // 자르기 적용 (회전/반전 후 화면상 좌표 → 비트맵 좌표 변환, 줌/패닝 포함)
        if (cropData != null) {
            val rotW = result.width.toFloat()
            val rotH = result.height.toFloat()
            // ContentScale.Fit 기준 디스플레이 스케일 및 오프셋
            val s    = minOf(cropData.containerW / rotW, cropData.imgAreaHeightPx / rotH)
            val baseDispLeft = (cropData.containerW - rotW * s) / 2f
            val baseDispTop  = cropData.imgAreaTopPx + (cropData.imgAreaHeightPx - rotH * s) / 2f
            // graphicsLayer 중심 (축 기준점)
            val cx = cropData.containerW / 2f
            val cy = cropData.imgAreaTopPx + cropData.imgAreaHeightPx / 2f
            // 줌/패닝 적용 후 실제 화면상 이미지 좌상단 위치
            val z  = cropData.cropZoom
            val px = cropData.cropPanX; val py = cropData.cropPanY
            val effDispLeft = cx + (baseDispLeft - cx) * z + px
            val effDispTop  = cy + (baseDispTop  - cy) * z + py
            val effScale    = s * z   // 유효 px-per-image-pixel

            val imgCropLeft = ((cropData.left - effDispLeft) / effScale).coerceIn(0f, rotW - 1f).toInt()
            val imgCropTop  = ((cropData.top  - effDispTop)  / effScale).coerceIn(0f, rotH - 1f).toInt()
            val imgCropW = (cropData.width  / effScale).coerceIn(1f, rotW - imgCropLeft).toInt().coerceAtLeast(1)
            val imgCropH = (cropData.height / effScale).coerceIn(1f, rotH - imgCropTop ).toInt().coerceAtLeast(1)

            result = android.graphics.Bitmap.createBitmap(result, imgCropLeft, imgCropTop, imgCropW, imgCropH)
        }

        // 저장 경로 결정 — PNG 소스(투명 배경)는 PNG 유지, 그 외 JPEG
        val originalPath = originalUri.path ?: return@withContext null
        val originalFile = java.io.File(originalPath)
        val isPng   = originalPath.endsWith(".png", ignoreCase = true)
        val ext     = if (isPng) "png" else "jpg"
        val format  = if (isPng) android.graphics.Bitmap.CompressFormat.PNG
                      else       android.graphics.Bitmap.CompressFormat.JPEG
        val quality = if (isPng) 100 else 95

        val outFile = if (overwrite) {
            originalFile
        } else {
            java.io.File(
                originalFile.parent ?: context.getExternalFilesDir(null)!!.absolutePath,
                "edited_${System.currentTimeMillis()}.$ext"
            )
        }

        java.io.FileOutputStream(outFile).use { fos ->
            result.compress(format, quality, fos)
        }
        android.net.Uri.fromFile(outFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

internal fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoTaken: (Uri) -> Unit
) {
    val photoFile = File(
        context.getExternalFilesDir(null),
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onPhotoTaken(Uri.fromFile(photoFile))
            }
        }
    )
}

internal fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<androidx.camera.video.Recorder>,
    onRecordingStarted: (Recording) -> Unit,
    onVideoSaved: (Uri) -> Unit
) {
    try {
        val videoFile = File(
            context.getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
        )

        val fileOutputOptions = androidx.camera.video.FileOutputOptions.Builder(videoFile).build()

        val recording = videoCapture.output
            .prepareRecording(context, fileOutputOptions)
            .apply {
                // 오디오 권한이 있으면 오디오 활성화, 없으면 오디오 없이 촬영
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // 촬영 시작됨
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            // 동영상 저장 완료
                            val videoUri = Uri.fromFile(videoFile)
                            onVideoSaved(videoUri)
                        } else {
                            // 오류 발생
                            event.cause?.printStackTrace()
                        }
                    }
                }
            }
        
        // recording 객체를 콜백으로 전달
        onRecordingStarted(recording)
    } catch (e: Exception) {
        e.printStackTrace()
        // 오류 발생 시 빈 Recording 객체 전달하지 않음
    }
}

// (삭제) TFLite 기반 광택/반사 제거 기능: 요청에 따라 Inpainting 기반으로 전면 교체됨

// [수정] 모델링 적합성 판단 (이동식 공간 촬영용)
// - 기존: "가상 점 5개가 모두 같은 공간"과 유사한 개념을 십자선 전체 표준편차로 간접 판단
// - 변경: 3x3 배열(9개)의 "가상 점(샘플 포인트)"을 중앙 주변에 배치하고,
//        9개 중 7개 이상이 동일한 공간(=주변 RGB/밝기 특성이 동일한 영역)으로 판정되면 경고(false)
// true: 적합, false: 부적합(경고 필요)
internal fun checkModelingSuitability(bitmap: android.graphics.Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 2 || height <= 2) return true

    val cx = width / 2
    val cy = height / 2

    // 3x3 샘플 포인트 배치 (중앙 기준)
    // - 화면 해상도에 따라 자동 스케일
    val spacing = (minOf(width, height) * 0.18f).toInt().coerceIn(80, 260)
    val offsets = intArrayOf(-spacing, 0, spacing)

    // 각 포인트에서 작은 패치(주변 픽셀) 평균 밝기를 구해 "공간 ID"로 사용
    // - quantStep이 작을수록 민감 (동일 판정이 어려움), 클수록 둔감
    val patchRadius = 10 // 21x21
    val quantStep = 16   // 0~255 -> 16단계(0~15)

    val spaceIdCounts = HashMap<Int, Int>(16)
    var totalPoints = 0

    fun lumaOf(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    for (dy in offsets) {
        for (dx in offsets) {
            val px = (cx + dx).coerceIn(0, width - 1)
            val py = (cy + dy).coerceIn(0, height - 1)

            var sum = 0L
            var count = 0
            val y0 = (py - patchRadius).coerceIn(0, height - 1)
            val y1 = (py + patchRadius).coerceIn(0, height - 1)
            val x0 = (px - patchRadius).coerceIn(0, width - 1)
            val x1 = (px + patchRadius).coerceIn(0, width - 1)

            for (y in y0..y1) {
                for (x in x0..x1) {
                    sum += lumaOf(bitmap.getPixel(x, y)).toLong()
                    count++
                }
            }

            if (count <= 0) continue
            val meanLuma = (sum / count).toInt().coerceIn(0, 255)
            val spaceId = (meanLuma / quantStep).coerceIn(0, 255 / quantStep)

            spaceIdCounts[spaceId] = (spaceIdCounts[spaceId] ?: 0) + 1
            totalPoints++
        }
    }

    if (totalPoints <= 0) return true

    // 9개 중 7개 이상이 동일 공간이면 "깊이/텍스처 정보 부족"으로 간주
    val maxSame = spaceIdCounts.values.maxOrNull() ?: 0
    if (maxSame >= 7) return false

    // 보조 안전장치: 중앙 십자선 영역의 텍스처(표준편차)도 너무 낮으면 부적합 처리
    // (샘플링만으로 놓치는 케이스 방지)
    val halfLen = 350
    val halfThick = 10
    val lumValues = ArrayList<Int>(4000)
    for (x in (cx - halfLen) until (cx + halfLen)) {
        for (y in (cy - halfThick) until (cy + halfThick)) {
            if (x in 0 until width && y in 0 until height) lumValues.add(lumaOf(bitmap.getPixel(x, y)))
        }
    }
    for (y in (cy - halfLen) until (cy + halfLen)) {
        for (x in (cx - halfThick) until (cx + halfThick)) {
            if (x in 0 until width && y in 0 until height) lumValues.add(lumaOf(bitmap.getPixel(x, y)))
        }
    }
    if (lumValues.isEmpty()) return true
    val mean = lumValues.sum().toDouble() / lumValues.size
    var varianceSum = 0.0
    for (lum in lumValues) varianceSum += (lum - mean) * (lum - mean)
    val stdDev = Math.sqrt(varianceSum / lumValues.size)

    return stdDev > 30.0
}

// (삭제) IID/MSA 기반 광택 제거 기능: 요청에 따라 Inpainting 기반으로 전면 교체됨

// [추가] 이미지 파일 로드 시 EXIF 회전 정보 반영
internal fun loadBitmapWithRotation(path: String): android.graphics.Bitmap? {
    return try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        
        if (rotation != 0f) {
            val matrix = Matrix()
            matrix.postRotate(rotation)
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// (삭제) 픽셀 억제/금속/반광/통합 파이프라인: 요청에 따라 Inpainting 기반으로 전면 교체됨


internal fun saveBitmapToFile(bitmap: android.graphics.Bitmap, file: File) {
    try {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun captureDatasetImage(
    context: Context,
    sectorIndex: Int,
    pitchAngle: Int,
    dir: File,
    capture: ImageCapture,
    customFileName: String? = null,
    validationCallback: ((android.graphics.Bitmap) -> Boolean)? = null
) {
    try {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = if (customFileName != null) {
            File(dir, customFileName)
        } else {
            File(dir, "${pitchAngle}_${sectorIndex + 1}.jpg")
        }
        
        // 메모리로 캡처 후 후처리 적용
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        
                        // 회전 보정
                        val rotation = image.imageInfo.rotationDegrees
                        if (rotation != 0 && bitmap != null) {
                            val matrix = Matrix()
                            matrix.postRotate(rotation.toFloat())
                            bitmap = android.graphics.Bitmap.createBitmap(
                                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                            )
                        }

                        if (bitmap != null) {
                            // [추가] 1:1 비율로 중앙 크롭 및 1024x1024 리사이징
                            val originalWidth = bitmap.width
                            val originalHeight = bitmap.height
                            val size = if (originalWidth < originalHeight) originalWidth else originalHeight
                            val xOffset = (originalWidth - size) / 2
                            val yOffset = (originalHeight - size) / 2
                            
                            // 중앙 정사각형 크롭
                            var croppedBitmap = android.graphics.Bitmap.createBitmap(
                                bitmap, xOffset, yOffset, size, size
                            )
                            
                            // 1024x1024로 리사이징
                            if (size != 1024) {
                                croppedBitmap = android.graphics.Bitmap.createScaledBitmap(
                                    croppedBitmap, 1024, 1024, true
                                )
                            }
                            bitmap = croppedBitmap

                            // [추가] 저장 전 검증 (유사도 체크 등)
                            if (validationCallback != null) {
                                if (!validationCallback(bitmap)) {
                                    // 검증 실패 시 저장하지 않음 (유사도 조건 불만족 등)
                                    return
                                }
                            }

                            // [변경] 촬영 시에는 원본을 그대로 저장 (광택/반사 보정은 폴더 선택 후 옵션에서 수행)
                            saveBitmapToFile(bitmap, file)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun formatTime(millis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

internal fun deleteMediaByUri(context: Context, uri: Uri) {
    try {
        if (uri.scheme == "content") {
            context.contentResolver.delete(uri, null, null)
        } else {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun deleteAllMedia(context: Context) {
    try {
        val mediaDir = context.getExternalFilesDir(null)
        if (mediaDir != null && mediaDir.exists()) {
            mediaDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun deleteDatasetFolder(folder: DatasetFolder) {
    try {
        if (folder.dir.exists()) {
            folder.dir.deleteRecursively()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/** 데이터셋폴더 라이브러리 전체 비우기 (`datasets/` 하위만) */
internal fun deleteAllDatasetFolders(context: Context) {
    try {
        val root = File(context.getExternalFilesDir(null), "datasets")
        if (root.exists()) {
            root.listFiles()?.forEach { child ->
                if (child.isDirectory) child.deleteRecursively()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 데이터셋 라이브러리에서 배경/광택 제거 등 배치 처리 시, [datasets] 아래 원본과 분리된 새 폴더를 만든다.
 * 단일 원본 폴더: `원본폴더명_작업라벨_시각`, 복수: `작업라벨_N개폴더_시각`
 */
internal fun createDatasetBatchResultFolder(
    context: Context,
    sourceFolderPaths: Set<String>,
    operationLabel: String
): File {
    val root = File(context.getExternalFilesDir(null), "datasets")
    root.mkdirs()
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val safeOp = operationLabel.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val folderName = when (sourceFolderPaths.size) {
        1 -> {
            val base = File(sourceFolderPaths.first()).name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            "${base}_${safeOp}_$ts"
        }
        else -> "${safeOp}_${sourceFolderPaths.size}개폴더_$ts"
    }
    val out = File(root, folderName)
    out.mkdirs()
    return out
}

/** 선택한 데이터셋 폴더 경로들에서 이미지 파일 URI 수집 (동영상 제외는 호출부에서) */
internal fun collectImageUrisFromDatasetFolderPaths(
    folderPaths: Set<String>
): List<Uri> {
    val imageExts = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
    val uris = mutableListOf<Uri>()
    for (path in folderPaths) {
        val dir = File(path)
        if (!dir.isDirectory) continue
        dir.listFiles { f ->
            f.isFile && imageExts.contains(f.extension.lowercase())
        }?.sortedByDescending { it.lastModified() }?.forEach { f ->
            uris.add(Uri.fromFile(f))
        }
    }
    return uris
}

internal fun isVideoUri(context: Context, uri: Uri): Boolean {
    return try {
        if (uri.scheme == "content") {
            val type = context.contentResolver.getType(uri)
            type?.startsWith("video/") == true
        } else {
            uri.toString().endsWith(".mp4", ignoreCase = true)
        }
    } catch (e: Exception) {
        false
    }
}

/** EXIF 촬영/생성 시각(없으면 null) — 정렬용 */
internal fun exifSortTimeMillis(path: String): Long? {
    return try {
        val exif = ExifInterface(path)
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        if (raw.isNullOrBlank()) return null
        val fmt = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        fmt.parse(raw)?.time
    } catch (_: Exception) {
        null
    }
}

internal fun parseVideoMetadataDateMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim()
    if (s.all { it.isDigit() }) {
        when (s.length) {
            13 -> return s.toLongOrNull()
            10 -> return s.toLongOrNull()?.times(1000L)
        }
    }
    val patterns = arrayOf(
        "yyyyMMdd'T'HHmmss",
        "yyyyMMdd'T'HHmmss.SSS",
        "yyyyMMdd'T'HHmmss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyyMMddHHmmss"
    )
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.US)
            sdf.isLenient = false
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(s)?.time?.let { return it }
        } catch (_: Exception) {
        }
    }
    try {
        val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
        return sdf.parse(s)?.time
    } catch (_: Exception) {
    }
    return null
}

internal fun videoSortTimeMillis(file: File): Long {
    var r: MediaMetadataRetriever? = null
    return try {
        r = MediaMetadataRetriever()
        r.setDataSource(file.absolutePath)
        val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        parseVideoMetadataDateMillis(d) ?: file.lastModified()
    } catch (_: Exception) {
        file.lastModified()
    } finally {
        try {
            r?.release()
        } catch (_: Exception) {
        }
    }
}

/**
 * 갤러리 정렬용: 사진·이미지는 EXIF 촬영 시각, 동영상은 메타데이터 촬영 시각, 없으면 [File.lastModified].
 */
internal fun mediaSortTimeMillis(file: File): Long {
    if (!file.isFile) return 0L
    return when (file.extension.lowercase(Locale.ROOT)) {
        "mp4" -> videoSortTimeMillis(file)
        "jpg", "jpeg", "png", "webp", "heic", "heif" ->
            exifSortTimeMillis(file.absolutePath) ?: file.lastModified()
        else -> file.lastModified()
    }
}

/** 로컬 자정 기준 일 단위 키 (날짜 헤더·그룹용) */
internal fun startOfDayLocalMillis(millis: Long): Long {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 갤러리 날짜 헤더: `2024년 12월 15일` */
internal fun formatKoreanDateHeader(dayStartMillis: Long): String {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.timeInMillis = dayStartMillis
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "${y}년 ${m}월 ${d}일"
}

/**
 * [sortedItemsNewestFirst]는 시각 내림차순(최신 먼저)이어야 같은 날짜끼리 연속으로 묶입니다.
 */
internal fun <T> groupByDayConsecutiveDescending(
    sortedItemsNewestFirst: List<T>,
    millisOf: (T) -> Long
): List<Pair<Long, List<T>>> {
    if (sortedItemsNewestFirst.isEmpty()) return emptyList()
    val out = mutableListOf<Pair<Long, List<T>>>()
    var currentDay: Long? = null
    var bucket = mutableListOf<T>()
    for (item in sortedItemsNewestFirst) {
        val day = startOfDayLocalMillis(millisOf(item))
        when {
            currentDay == null -> {
                currentDay = day
                bucket.add(item)
            }
            day == currentDay -> bucket.add(item)
            else -> {
                out.add(currentDay!! to bucket.toList())
                currentDay = day
                bucket = mutableListOf(item)
            }
        }
    }
    if (bucket.isNotEmpty() && currentDay != null) {
        out.add(currentDay to bucket.toList())
    }
    return out
}

internal fun gallerySortMillisForUri(context: Context, uri: Uri): Long {
    return try {
        when (uri.scheme?.lowercase(Locale.ROOT)) {
            "file" -> {
                val path = uri.path ?: return System.currentTimeMillis()
                val f = File(path)
                if (f.isFile) mediaSortTimeMillis(f) else System.currentTimeMillis()
            }
            "content" -> {
                val p = uri.path
                if (p != null && p.startsWith("/")) {
                    val f = File(p)
                    if (f.isFile) return mediaSortTimeMillis(f)
                }
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DATE_TAKEN),
                    null,
                    null,
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                        if (idx >= 0) {
                            val dt = c.getLong(idx)
                            if (dt > 0L) return dt
                        }
                    }
                }
                System.currentTimeMillis()
            }
            else -> System.currentTimeMillis()
        }
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

internal fun groupImagesByDayInOrder(context: Context, images: List<Uri>): List<Pair<Long, List<Uri>>> {
    return groupByDayConsecutiveDescending(images) { gallerySortMillisForUri(context, it) }
}

/** 날짜 헤더 포함 시 [images]에서 미디어 인덱스에 해당하는 그리드 행 인덱스 */
internal fun gridItemIndexForMediaIndex(
    mediaIndex: Int,
    images: List<Uri>,
    context: Context
): Int {
    if (mediaIndex !in images.indices) return 0
    val groups = groupImagesByDayInOrder(context, images)
    var gridIdx = 0
    var mediaIdx = 0
    for ((_, uris) in groups) {
        gridIdx++
        for (u in uris) {
            if (mediaIdx == mediaIndex) return gridIdx
            gridIdx++
            mediaIdx++
        }
    }
    return 0
}

internal fun loadDatasetFolders(
    context: Context,
    onLoaded: (List<DatasetFolder>) -> Unit
) {
    onLoaded(loadDatasetFoldersSync(context))
}

// [추가] 데이터셋 폴더 로드(동기) + 빈 폴더 자동 정리
internal fun loadDatasetFoldersSync(context: Context): List<DatasetFolder> {
    val root = File(context.getExternalFilesDir(null), "datasets")
    if (!root.exists()) {
        return emptyList()
    }

    // [추가] 0장(빈) 데이터셋 폴더 자동 정리
    // - 너무 최근에 생성된 폴더(촬영 직후 등)는 오탐 방지를 위해 잠시 유예
    val now = System.currentTimeMillis()
    val minAgeMs = 60_000L // 60초보다 오래된 "빈" 폴더만 삭제

    val imageExts = setOf("jpg", "jpeg", "png", "webp")
    return root.listFiles { file -> file.isDirectory }
        ?.mapNotNull { dir ->
            val images = dir.listFiles { f ->
                f.isFile && imageExts.contains(f.extension.lowercase())
            }?.sortedByDescending { mediaSortTimeMillis(it) } ?: emptyList()

            if (images.isEmpty()) {
                // 이미지가 0개인 폴더는 주기적으로 자동 삭제
                if (now - dir.lastModified() >= minAgeMs) {
                    try {
                        dir.deleteRecursively()
                    } catch (_: Exception) {
                    }
                    return@mapNotNull null
                }
            }

            val cover = images.firstOrNull()?.let { Uri.fromFile(it) }
            DatasetFolder(
                name = dir.name,
                dir = dir,
                coverUri = cover,
                count = images.size
            )
        }
        ?.sortedByDescending { it.dir.lastModified() }
        ?: emptyList()
}

/** Gemini 전송용: 모든 데이터셋 폴더의 이미지 URI 목록 로드 */
internal fun loadAllDatasetImages(context: Context, onLoaded: (List<Uri>) -> Unit) {
    val folders = loadDatasetFoldersSync(context)
    val imageExts = setOf("jpg", "jpeg", "png", "webp")
    val uris = folders.flatMap { folder ->
        folder.dir.listFiles { f ->
            f.isFile && imageExts.contains(f.extension.lowercase())
        }?.sortedByDescending { mediaSortTimeMillis(it) }?.map { Uri.fromFile(it) } ?: emptyList()
    }
    onLoaded(uris)
}

internal fun loadDatasetImages(
    dir: File,
    onLoaded: (List<Uri>) -> Unit
) {
    if (!dir.exists()) {
        onLoaded(emptyList())
        return
    }
    val imageExts = setOf("jpg", "jpeg", "png", "webp")
    val images = dir.listFiles { f ->
        f.isFile && imageExts.contains(f.extension.lowercase())
    }?.sortedByDescending { mediaSortTimeMillis(it) } ?: emptyList()

    onLoaded(images.map { Uri.fromFile(it) })
}

internal fun shareLibraryFile(context: Context, file: File) {
    if (!file.exists() || !file.isFile) {
        Toast.makeText(context, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "공유를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    val mime = when (file.extension.lowercase()) {
        "stl" -> "model/stl"
        "ply" -> "model/ply"
        else -> "application/octet-stream"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "file", uri)
    }
    context.startActivity(Intent.createChooser(intent, "공유"))
}

/**
 * 갤러리에 저장된 [Uri]를 공유용으로 정규화합니다.
 * [loadCapturedMediaSync] 등은 [Uri.fromFile]을 쓰므로 `file://`인데, 다른 앱으로 넘기려면
 * FileProvider `content://`가 필요합니다(API 24+).
 */
internal fun uriToShareableContentUri(context: Context, uri: Uri): Uri? {
    if (uri.scheme.equals("content", ignoreCase = true)) {
        return uri
    }
    if (!uri.scheme.equals("file", ignoreCase = true)) {
        return null
    }
    val path = uri.path ?: return null
    val file = File(path)
    if (!file.exists() || !file.isFile) {
        return null
    }
    return try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/** 앱 갤러리 미디어(Uri) 공유 — 단일/다중 스트림 */
internal fun shareGalleryMediaUris(context: Context, uris: List<Uri>) {
    val list = uris.filter { it != Uri.EMPTY }
    if (list.isEmpty()) {
        Toast.makeText(context, "공유할 항목이 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    val shareUris = list.mapNotNull { uriToShareableContentUri(context, it) }
    if (shareUris.isEmpty()) {
        Toast.makeText(context, "공유를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        if (shareUris.size == 1) {
            val uri = shareUris.first()
            val mime = context.contentResolver.getType(uri)
                ?: when {
                    uri.toString().contains(".mp4", ignoreCase = true) -> "video/*"
                    else -> "image/*"
                }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, "media", uri)
            }
            context.startActivity(Intent.createChooser(intent, "공유"))
            return
        }
        val resolver = context.contentResolver
        val clip = ClipData.newUri(resolver, "media", shareUris.first())
        for (i in 1 until shareUris.size) {
            clip.addItem(ClipData.Item(shareUris[i]))
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = clip
        }
        context.startActivity(Intent.createChooser(intent, "공유"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "공유를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

internal fun shareLibraryFiles(context: Context, files: List<File>) {
    val existing = files.filter { it.exists() && it.isFile }
    if (existing.isEmpty()) {
        Toast.makeText(context, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    if (existing.size == 1) {
        shareLibraryFile(context, existing.first())
        return
    }
    val uris = ArrayList<Uri>()
    val authority = "${context.packageName}.fileprovider"
    for (file in existing) {
        try {
            uris.add(FileProvider.getUriForFile(context, authority, file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (uris.isEmpty()) {
        Toast.makeText(context, "공유를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    val resolver = context.contentResolver
    val clip = ClipData.newUri(resolver, "files", uris.first())
    for (i in 1 until uris.size) {
        clip.addItem(ClipData.Item(uris[i]))
    }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = clip
    }
    context.startActivity(Intent.createChooser(intent, "공유"))
}

internal fun deleteLibraryModelFile(file: File): Boolean {
    return try {
        if (!file.exists() || !file.isFile) return false
        val ok = file.delete()
        if (file.extension.equals("obj", ignoreCase = true)) {
            val mtl = File(file.parentFile, "${file.nameWithoutExtension}.mtl")
            if (mtl.exists()) {
                try {
                    mtl.delete()
                } catch (_: Exception) {
                }
            }
        }
        ok
    } catch (_: Exception) {
        false
    }
}

/** STL과 동일 베이스명의 GLB·OBJ를 함께 제거합니다. */
internal fun deleteAiCadArtifactsForStl(stlFile: File): Boolean {
    return try {
        val dir = stlFile.parentFile ?: return false
        val base = stlFile.nameWithoutExtension
        var hadAny = false
        var allOk = true
        for (ext in listOf("stl", "glb", "obj")) {
            val f = File(dir, "$base.$ext")
            if (f.exists() && f.isFile) {
                hadAny = true
                if (!f.delete()) allOk = false
            }
        }
        hadAny && allOk
    } catch (_: Exception) {
        false
    }
}

/**
 * 서버 파이프라인 결과는 [plyRoot]/server_task_{taskId}/아래에 PLY가 저장된다.
 * 기존 로직은 ply 루트의 파일만 나열해 서버 작업 PLY가 라이브러리에 안 보였음 → 1단계 하위 폴더까지 포함.
 */
private fun collectPlyFilesForLibrary(plyRoot: File): List<File> {
    val out = ArrayList<File>()
    val top = plyRoot.listFiles() ?: return out
    for (entry in top) {
        when {
            entry.isFile && entry.name.endsWith(".ply", ignoreCase = true) -> out.add(entry)
            entry.isDirectory -> {
                val sub = entry.listFiles() ?: continue
                for (f in sub) {
                    if (f.isFile && f.name.endsWith(".ply", ignoreCase = true)) out.add(f)
                }
            }
        }
    }
    return out.distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
}

private fun displayNameForPlyLibraryEntry(plyFile: File, plyRoot: File): String {
    val base = plyFile.nameWithoutExtension
    val parent = plyFile.parentFile ?: return base
    return try {
        if (parent.canonicalPath == plyRoot.canonicalPath) base
        else "${parent.name}_$base"
    } catch (_: Exception) {
        "${parent.name}_$base"
    }
}

internal fun loadModel3dLibrary(
    context: Context,
    onLoaded: (Model3dSplitLibrary) -> Unit
) {
    ModelLibraryPaths.migrateFlatModelsIfNeeded(context)
    val plyD = ModelLibraryPaths.plyDir(context)
    val objD = ModelLibraryPaths.objDir(context)
    val plyList = collectPlyFilesForLibrary(plyD).map { f ->
        PlyModel(
            name = displayNameForPlyLibraryEntry(f, plyD),
            file = f,
            lastModified = f.lastModified()
        )
    }
    val objList = (objD.listFiles { f -> f.isFile && f.name.endsWith(".obj", ignoreCase = true) }
        ?: emptyArray())
        .map { f ->
            PlyModel(
                name = f.nameWithoutExtension,
                file = f,
                lastModified = f.lastModified()
            )
        }
        .sortedByDescending { it.lastModified }
    onLoaded(Model3dSplitLibrary(plyList, objList))
}

/**
 * PLY→OBJ 변환 캐시 파일을 `models/obj/`에 OBJ(+MTL)로 복사합니다.
 * PLY 원본과 models_obj 캐시는 삭제하지 않습니다. `mtllib` 경로는 저장 위치에 맞게 조정합니다.
 * @return null 이면 성공, 아니면 오류 메시지
 */
internal fun saveConvertedObjToModelsLibrary(
    context: Context,
    plySource: File,
    cachedObj: File
): String? {
    if (!plySource.name.endsWith(".ply", ignoreCase = true)) {
        return "PLY에서 변환된 OBJ만 저장할 수 있습니다."
    }
    if (!cachedObj.exists() || cachedObj.length() == 0L) return "OBJ 파일이 없습니다."
    val modelsDir = ModelLibraryPaths.objDir(context)
    val baseName = plySource.nameWithoutExtension
    val destObj = File(modelsDir, "$baseName.obj")
    val destMtlName = "$baseName.mtl"
    val destMtl = File(modelsDir, destMtlName)
    val cachedMtl = File(cachedObj.parentFile, "${cachedObj.nameWithoutExtension}.mtl")

    return try {
        val tmpObj = File(modelsDir, "$baseName.obj.tmp")
        try {
            if (tmpObj.exists()) tmpObj.delete()
        } catch (_: Exception) {
        }
        cachedObj.bufferedReader(StandardCharsets.UTF_8).use { reader ->
            tmpObj.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                reader.forEachLine { line ->
                    val t = line.trim()
                    if (t.startsWith("mtllib", ignoreCase = true)) {
                        writer.append("mtllib ")
                        writer.append(destMtlName)
                        writer.newLine()
                    } else {
                        writer.append(line)
                        writer.newLine()
                    }
                }
            }
        }
        if (!tmpObj.exists() || tmpObj.length() == 0L) return "OBJ 저장에 실패했습니다."
        if (destObj.exists()) {
            try {
                destObj.delete()
            } catch (_: Exception) {
            }
        }
        if (!tmpObj.renameTo(destObj)) {
            try {
                tmpObj.copyTo(destObj, overwrite = true)
                tmpObj.delete()
            } catch (e: Exception) {
                return "OBJ 저장에 실패했습니다: ${e.message ?: e.javaClass.simpleName}"
            }
        }
        if (!destObj.exists() || destObj.length() == 0L) return "OBJ 저장에 실패했습니다."

        if (cachedMtl.exists() && cachedMtl.length() > 0L) {
            val tmpMtl = File(modelsDir, "$baseName.mtl.tmp")
            try {
                if (tmpMtl.exists()) tmpMtl.delete()
                cachedMtl.copyTo(tmpMtl, overwrite = true)
                if (destMtl.exists()) {
                    try {
                        destMtl.delete()
                    } catch (_: Exception) {
                    }
                }
                if (!tmpMtl.renameTo(destMtl)) {
                    tmpMtl.copyTo(destMtl, overwrite = true)
                    try {
                        tmpMtl.delete()
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (destMtl.exists()) {
                try {
                    destMtl.delete()
                } catch (_: Exception) {}
            }
        }
        null
    } catch (e: Exception) {
        e.printStackTrace()
        "저장 실패: ${e.message ?: e.javaClass.simpleName}"
    }
}

/** 갤러리 스캔 시 재귀를 막을 앱 전용 폴더 (gemma4 extracted·models 등은 파일 수가 매우 많음) */
internal fun skipGallerySubtreeDirName(name: String): Boolean = when (name.lowercase(Locale.ROOT)) {
    "datasets", "gemma4", "models", "aicad_library" -> true
    else -> false
}

internal fun loadCapturedMediaSync(context: Context): List<Uri> {
    val mediaDir = context.getExternalFilesDir(null)
    if (mediaDir == null || !mediaDir.exists()) {
        return emptyList()
    }

    val datasetsRoot = File(mediaDir, "datasets").absolutePath
    val entries = mediaDir.walkTopDown()
        .onEnter { dir ->
            dir == mediaDir || !skipGallerySubtreeDirName(dir.name)
        }
        .filter { file ->
            file.isFile &&
                (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true) ||
                    file.name.endsWith(".png", ignoreCase = true) ||
                    file.name.endsWith(".webp", ignoreCase = true) ||
                    file.name.endsWith(".heic", ignoreCase = true) ||
                    file.name.endsWith(".heif", ignoreCase = true) ||
                    file.name.endsWith(".mp4", ignoreCase = true)) &&
                !file.absolutePath.startsWith(datasetsRoot)
        }
        .map { file -> Uri.fromFile(file) to mediaSortTimeMillis(file) }
        .toList()

    return entries.sortedByDescending { it.second }.map { it.first }
}

/** 앱 내 갤러리 스캔 결과 중 이미지 파일만 (동영상 제외). Mobile 3DGS 이미지 선택 등에 사용 */
internal fun loadCapturedImageUrisOnlySync(context: Context): List<Uri> {
    return loadCapturedMediaSync(context).filter { uri ->
        val path = uri.path?.lowercase(Locale.ROOT) ?: return@filter false
        path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") ||
            path.endsWith(".webp") || path.endsWith(".heic") || path.endsWith(".heif")
    }
}

/**
 * HTTPS를 지원하는 OkHttpClient 베이스 인스턴스.
 * 호출부에서 [OkHttpClient.newBuilder]로 타임아웃만 조정하므로, SSL 초기화 비용은 모드별 1회로 캐시합니다.
 */
private val okHttpBaseLock = Any()
@Volatile
private var okHttpBasePlain: OkHttpClient? = null
@Volatile
private var okHttpBaseTrustAll: OkHttpClient? = null

/**
 * HTTPS를 지원하는 OkHttpClient 생성
 */
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)

    // HTTPS 사용 시 SSL 인증서 검증 우회 (개발/테스트 환경)
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

/**
 * 서버 결과 파일 다운로드 전용 클라이언트.
 * read/write/call 타임아웃 0 = 무제한(OkHttp 규약) — 수 GB·저속 회선에서 조기 종료 방지.
 */
internal fun buildServerDownloadOkHttpClient(useHttps: Boolean): OkHttpClient {
    return createOkHttpClient(useHttps).newBuilder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()
}

internal fun resolveDisplayName(context: Context, uri: Uri): String? {
    return try {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
        } else {
            uri.lastPathSegment?.substringAfterLast('/')
        }
    } catch (_: Exception) {
        null
    }
}

internal suspend fun createZipFromUris(
    context: Context,
    uris: List<Uri>,
    zipPrefix: String
): File? {
    return withContext(Dispatchers.IO) {
        try {
            val zipFileName =
                "${zipPrefix}_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}.zip"
            val zipFile = File(context.getExternalFilesDir(null), zipFileName)

            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    uris.forEachIndexed { index, uri ->
                        val name = resolveDisplayName(context, uri)
                            ?: "item_${index + 1}"
                        val safeName = name.replace('\\', '_').replace('/', '_')

                        val input = if (uri.scheme == "content") {
                            context.contentResolver.openInputStream(uri)
                        } else {
                            val path = uri.path
                            if (path.isNullOrBlank()) null else FileInputStream(File(path))
                        } ?: return@forEachIndexed

                        input.use { ins ->
                            zos.putNextEntry(ZipEntry(safeName))
                            ins.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }

            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

internal suspend fun createZipFromFolders(
    context: Context,
    folders: List<File>,
    zipPrefix: String
): File? {
    return withContext(Dispatchers.IO) {
        try {
            val zipFileName =
                "${zipPrefix}_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}.zip"
            val zipFile = File(context.getExternalFilesDir(null), zipFileName)

            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    folders.forEach { folder ->
                        if (!folder.exists() || !folder.isDirectory) return@forEach
                        folder.walkTopDown()
                            .filter { it.isFile }
                            .forEach { file ->
                                val rel = try {
                                    file.relativeTo(folder).path.replace('\\', '/')
                                } catch (_: Exception) {
                                    file.name
                                }
                                val entryName = "${folder.name}/$rel"
                                zos.putNextEntry(ZipEntry(entryName))
                                FileInputStream(file).use { fis ->
                                    fis.copyTo(zos)
                                }
                                zos.closeEntry()
                            }
                    }
                }
            }

            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * 미디어 파일을 서버에 업로드하는 함수
 * @param context 컨텍스트
 * @param file 업로드할 파일
 * @return 업로드 성공 여부
 */
internal suspend fun startServerTaskWithZip(
    context: Context,
    zipFile: File,
    prompt: String = ""
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val serverAddress = getServerAddress(context)
            val serverPort = getServerPort(context)
            val useHttps = getUseHttps(context)
            val client = createOkHttpClient(useHttps).newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()

            if (!zipFile.name.endsWith(".zip", ignoreCase = true)) {
                return@withContext null
            }

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    zipFile.name,
                    zipFile.asRequestBody("application/zip".toMediaType())
                )
            if (prompt.isNotBlank()) {
                multipartBuilder.addFormDataPart("text_prompt", prompt)
                multipartBuilder.addFormDataPart("prompt", prompt)
            }
            val requestBody = multipartBuilder.build()

            val protocol = if (useHttps) "https" else "http"
            val url = "$protocol://$serverAddress:$serverPort$UPLOAD_ENDPOINT"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            val success = response.isSuccessful && body != null
            response.close()

            if (!success) return@withContext null
            val json = JSONObject(body)
            json.optString("task_id").takeIf { it.isNotBlank() }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// (삭제) 서버 기반 광택/반사 제거 파이프라인: 요청에 따라 로컬 Inpainting 기반으로 교체됨

internal data class ServerTaskStatus(
    val status: String,
    val progressPercent: Int,
    val message: String,
    val downloadUrl: String?
)

internal suspend fun fetchServerTaskStatus(context: Context, taskId: String): ServerTaskStatus? {
    return withContext(Dispatchers.IO) {
        try {
            val serverAddress = getServerAddress(context)
            val serverPort = getServerPort(context)
            val useHttps = getUseHttps(context)
            val client = createOkHttpClient(useHttps).newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()

            val protocol = if (useHttps) "https" else "http"
            val url = "$protocol://$serverAddress:$serverPort$STATUS_ENDPOINT/$taskId"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            val ok = response.isSuccessful && body != null
            response.close()
            if (!ok) return@withContext null

            val json = JSONObject(body)
            // FastAPI 서버(main_0316_postprocess.py)는 "progress" 키를 사용한다.
            // 레거시 호환: "progress_percent"
            val pct = when {
                json.has("progress") -> json.optInt("progress", 0)
                json.has("progress_percent") -> json.optInt("progress_percent", 0)
                else -> 0
            }
            ServerTaskStatus(
                status = json.optString("status"),
                progressPercent = pct,
                message = json.optString("message", "처리 중..."),
                downloadUrl = json.optString("download_url").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

internal suspend fun fetchServerResultsJson(context: Context, taskId: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        try {
            val serverAddress = getServerAddress(context)
            val serverPort = getServerPort(context)
            val useHttps = getUseHttps(context)
            val client = createOkHttpClient(useHttps).newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(150, TimeUnit.SECONDS)
                .build()
            val protocol = if (useHttps) "https" else "http"
            val url = "$protocol://$serverAddress:$serverPort$RESULTS_ENDPOINT/$taskId"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            val ok = response.isSuccessful && body != null
            response.close()
            if (!ok) return@withContext null
            JSONObject(body)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

internal suspend fun downloadHttpUrlToFile(context: Context, absoluteUrl: String, outFile: File): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val useHttps = getUseHttps(context)
            val client = buildServerDownloadOkHttpClient(useHttps)
            val request = Request.Builder().url(absoluteUrl).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    return@withContext false
                }
                outFile.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    BufferedOutputStream(FileOutputStream(outFile), SERVER_DOWNLOAD_BUFFER_BYTES).use { output ->
                        input.copyTo(output, SERVER_DOWNLOAD_BUFFER_BYTES)
                    }
                }
                outFile.exists() && outFile.length() > 0L
            }
        } catch (e: Exception) {
            android.util.Log.w("downloadHttpUrlToFile", absoluteUrl, e)
            false
        }
    }
}

/**
 * `GET /results/{task_id}`로 목록을 받아 각 파일을 내려받습니다.
 * 엔드포인트가 없거나 실패하면 PLY 단일 다운로드로 폴백합니다.
 */
internal suspend fun downloadServerPipelineArtifacts(
    context: Context,
    taskId: String,
    onProgress: suspend (Int, String) -> Unit,
): ServerPipelineResultBundle? {
    val plyDir = ModelLibraryPaths.plyDir(context)
    val outDir = File(plyDir, "server_task_$taskId").apply { mkdirs() }
    val json = fetchServerResultsJson(context, taskId)
    val filesArr: JSONArray? = json?.optJSONArray("files")
    val map = mutableMapOf<String, File>()
    if (filesArr != null && filesArr.length() > 0) {
        val total = filesArr.length()
        for (i in 0 until total) {
            val o = filesArr.optJSONObject(i) ?: continue
            val key = o.optString("key")
            val url = o.optString("url")
            val filename = o.optString("filename").ifBlank { "file_$i" }
            if (key.isBlank() || url.isBlank()) continue
            val pct = 95 + ((i + 1) * 4 / maxOf(total, 1)).coerceIn(0, 4)
            onProgress(pct.coerceAtMost(99), "결과 파일 다운로드 (${i + 1}/$total)...")
            val dest = File(outDir, filename)
            if (downloadHttpUrlToFile(context, url, dest)) {
                map[key] = dest
            }
        }
    }
    if (map["ply"] == null) {
        onProgress(99, "PLY 다운로드 중...")
        val ply = downloadPlyResult(context, taskId)
        if (ply != null && ply.exists()) map["ply"] = ply
    }
    val plyFile = map["ply"] ?: return null
    writeServerTaskArtifactManifest(outDir, taskId, map.toMap())
    JsonLibrary.ingestFromPipelineOutputDir(context, outDir, taskId)
    return ServerPipelineResultBundle(
        taskId = taskId,
        plyFile = plyFile,
        directory = outDir,
        filesByKey = map.toMap(),
    )
}

/**
 * AI 탭 밖에서 호출: 서버 3DGS 분석을 새 MOBILE_3DGS 스레드로 저장한다.
 */
internal suspend fun runServer3dgsAnalysisInBackground(
    context: Context,
    pending: Pending3dgsServerAutoSend
): Boolean {
    val imageBase64List = withContext(Dispatchers.IO) {
        pending.imageUris.mapNotNull { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    bitmap?.let { ClaudeChatClient.bitmapToBase64(it) }
                }
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

/** 경찰·보험 조사용 3DGS 분석 탭: 프롬프트 + 서버에서 내려받은 텍스트/JSON/CSV 본문 + 이미지(가능한 모든 PNG·JPG) URI */
internal fun buildPoliceInsurance3dgsPayload(context: Context, bundle: ServerPipelineResultBundle): Pair<String, List<Uri>> {
    val base =
        "해당 파일을 바탕으로 PLY, 3DGS 파일을 분석해 경찰, 보험사에서 사용가능한 텍스트 형식으로 분석 출력해줘"
    val sb = StringBuilder(base)
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
        val chunk = if (raw.length > maxPerFile) {
            raw.take(maxPerFile) + "\n...(이하 생략, 원문 ${raw.length}자)..."
        } else raw
        sb.append("\n--- ").append(label).append(" (").append(f.name).append(") ---\n")
            .append(chunk).append('\n')
    }
    var text = sb.toString()
    if (text.length > maxTotal) {
        text = text.take(maxTotal) + "\n...(전체 본문 길이 제한으로 잘림)"
    }

    val imageExts = setOf("png", "jpg", "jpeg", "webp")
    val preferredOrder = listOf("topview", "sideview", "quality_png")
    val seen = HashSet<String>()
    val uris = ArrayList<Uri>()
    fun addFile(f: File) {
        val path = f.absolutePath
        if (path in seen || !f.exists() || !f.isFile) return
        if (f.extension.lowercase() !in imageExts) return
        uriToShareableContentUri(context, Uri.fromFile(f))?.let {
            seen.add(path)
            uris.add(it)
        }
    }
    for (k in preferredOrder) {
        bundle.filesByKey[k]?.let(::addFile)
    }
    for (f in bundle.filesByKey.values) {
        addFile(f)
    }
    return text to uris
}

/**
 * SAM2 서버(/bg-remove)에 이미지와 텍스트 프롬프트를 전송하여
 * 배경이 제거된 투명 PNG를 받아 앱 라이브러리에 저장합니다.
 *
 * 서버: scripts/sam2_server.py (포트 8001)
 *
 * @param context   앱 컨텍스트
 * @param imageUri  처리할 원본 이미지 URI
 * @param prompt    남길 사물의 영문 텍스트 (예: "cup", "mouse")
 * @param itemIndex 현재 처리 중인 이미지 순번 (0-based)
 * @param itemTotal 처리할 이미지 총 수
 * @param onProgress 진행률 콜백 (percent 0~100, message)
 * @return 앱 라이브러리에 저장된 PNG Uri, 실패 시 null
 */
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

        // URI → 바이트 배열 읽기
        val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: return@withContext null

        // MIME 타입 판별
        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
        val extension = when {
            mimeType.contains("png")  -> "png"
            mimeType.contains("webp") -> "webp"
            else                      -> "jpg"
        }

        onProgress(20, "SAM2 서버로 전송 중...$itemLabel")

        val client = createOkHttpClient(useHttps).newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS) // SAM2 추론 대기
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

        // 결과 PNG를 앱 라이브러리에 저장
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

internal suspend fun downloadPlyResult(context: Context, taskId: String): File? {
    // 큰 PLY 파일 다운로드 — 타임아웃을 길게 설정하고 최대 3회 재시도
    val maxAttempts = 3
    repeat(maxAttempts) { attempt ->
        val result = withContext(Dispatchers.IO) {
            try {
                val serverAddress = getServerAddress(context)
                val serverPort = getServerPort(context)
                val useHttps = getUseHttps(context)
                val client = buildServerDownloadOkHttpClient(useHttps)

                val protocol = if (useHttps) "https" else "http"
                val url = "$protocol://$serverAddress:$serverPort$DOWNLOAD_ENDPOINT/$taskId?format=ply"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Connection", "keep-alive")
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) {
                        return@withContext null
                    }

                    val modelsDir = ModelLibraryPaths.plyDir(context)
                    val outFile = File(modelsDir, "3d_model_$taskId.ply")
                    body.byteStream().use { input ->
                        BufferedOutputStream(FileOutputStream(outFile), SERVER_DOWNLOAD_BUFFER_BYTES).use { output ->
                            input.copyTo(output, SERVER_DOWNLOAD_BUFFER_BYTES)
                        }
                    }
                    outFile
                }
            } catch (e: Exception) {
                android.util.Log.w("downloadPlyResult", "다운로드 시도 ${attempt + 1}/$maxAttempts 실패", e)
                null
            }
        }
        if (result != null) return result
        // 마지막 시도가 아니면 지수 백오프 후 재시도 (5초, 10초)
        if (attempt < maxAttempts - 1) delay(5_000L * (attempt + 1))
    }
    return null
}

internal suspend fun uploadZipAndRunPipeline(
    context: Context,
    zipFile: File,
    prompt: String = "",
    onProgress: suspend (progress: Int, message: String) -> Unit
): ServerPipelineResultBundle? {
    val taskTitle = "3D 모델 생성 중"
    val uploadStartMs = System.currentTimeMillis()
    // 마지막으로 표시한 퍼센트 — 표시 값이 역행하지 않도록 추적
    var lastShownProgress = 0

    // 공통 헬퍼: UI 콜백 + 포그라운드 서비스 알림 동시 업데이트
    // 항상 lastShownProgress 이상의 값만 표시해 퍼센트 역행을 방지
    suspend fun updateNotification(p: Int, msg: String) {
        val safeP = p.coerceAtLeast(lastShownProgress).coerceIn(0, 100)
        lastShownProgress = safeP
        onProgress(safeP, msg)
        startOrUpdateForegroundService(context, taskTitle, safeP, msg, uploadStartMs)
    }

    // 서비스 시작
    startOrUpdateForegroundService(context, taskTitle, 0, "업로드 준비 중...", uploadStartMs)

    try {
        // 1) 업로드 -> task_id 확보
        updateNotification(5, "파일 업로드 중...")
        val noResponseMsg = "서버에 대한 응답이 없습니다.\n서버 연결을 확인해주십시오."
        val taskId = startServerTaskWithZip(context, zipFile, prompt)
        if (taskId.isNullOrBlank()) {
            onProgress(0, noResponseMsg)
            context.stopService(Intent(context, AppForegroundService::class.java))
            return null
        }

        // 로컬 ZIP은 서버에 올라갔으면 삭제(저장공간 확보)
        try { zipFile.delete() } catch (_: Exception) {}

        // 2) 상태 폴링 — 처음 2분은 2초 간격, 이후 5초 간격
        val start = System.currentTimeMillis()
        val timeoutMs = 30L * 60L * 1000L // 30분
        var lastServerResponseAt = System.currentTimeMillis()
        var pollCount = 0
        while (System.currentTimeMillis() - start < timeoutMs) {
            val st = fetchServerTaskStatus(context, taskId)
            if (st != null) {
                lastServerResponseAt = System.currentTimeMillis()
                // 서버 STATUS_PROGRESS(5~100) 그대로 반영 — 잘못된 키 파싱 시 0만 오므로 역행 방지는 updateNotification 내부
                val serverPct = st.progressPercent.coerceIn(0, 100)
                updateNotification(serverPct, st.message)
                when (st.status) {
                    "COMPLETED" -> break
                    "FAILED" -> {
                        context.stopService(Intent(context, AppForegroundService::class.java))
                        return null
                    }
                }
            } else {
                // 90초 이상 서버 응답이 없으면 중단 (일시적 네트워크 지연 허용)
                if (System.currentTimeMillis() - lastServerResponseAt >= 90_000L) {
                    onProgress(lastShownProgress, noResponseMsg)
                    context.stopService(Intent(context, AppForegroundService::class.java))
                    return null
                }
            }
            pollCount++
            // 처음 60회(2초 간격 = 2분)는 빠르게, 이후 5초 간격
            delay(if (pollCount <= 60) 2_000L else 5_000L)
        }

        val finalStatus = fetchServerTaskStatus(context, taskId)
        if (finalStatus?.status != "COMPLETED") {
            onProgress(lastShownProgress, "처리 시간이 초과되었거나 완료되지 않았습니다.")
            context.stopService(Intent(context, AppForegroundService::class.java))
            return null
        }

        // 3) 결과 다운로드 (.ply)
        // lastShownProgress 이상 값으로 고정(역행 방지): 서버가 99%를 줬다면 다운로드도 99%에서 시작
        updateNotification(lastShownProgress.coerceAtLeast(95), "결과 다운로드 중...")
        val bundle = downloadServerPipelineArtifacts(context, taskId, ::updateNotification) ?: run {
            context.stopService(Intent(context, AppForegroundService::class.java))
            return null
        }

        updateNotification(100, "완료되었습니다!")
        stopForegroundService(context, "3D 모델 생성 완료", "완료되었습니다!")
        return bundle
    } catch (e: Exception) {
        e.printStackTrace()
        context.stopService(Intent(context, AppForegroundService::class.java))
        return null
    }
}

internal fun listImageFiles(dir: File): List<File> {
    val exts = setOf("jpg", "jpeg", "png", "webp")
    return dir.listFiles()
        ?.filter { it.isFile && exts.contains(it.extension.lowercase()) }
        ?.sortedBy { it.name }
        ?: emptyList()
}

internal fun sharpenBitmapFast(src: Bitmap, amount: Float = 1.2f): Bitmap {
    val w = src.width
    val h = src.height
    if (w < 3 || h < 3) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)

    val inPixels = IntArray(w * h)
    src.getPixels(inPixels, 0, w, 0, 0, w, h)
    val outPixels = IntArray(w * h)

    fun clamp(v: Int): Int = when {
        v < 0 -> 0
        v > 255 -> 255
        else -> v
    }

    fun r(p: Int) = (p shr 16) and 0xFF
    fun g(p: Int) = (p shr 8) and 0xFF
    fun b(p: Int) = p and 0xFF
    fun a(p: Int) = (p ushr 24) and 0xFF

    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            val idx = row + x
            if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                outPixels[idx] = inPixels[idx]
                continue
            }

            val c = inPixels[idx]
            val up = inPixels[idx - w]
            val down = inPixels[idx + w]
            val left = inPixels[idx - 1]
            val right = inPixels[idx + 1]

            val cr = r(c); val cg = g(c); val cb = b(c)
            val blurR = (cr + r(up) + r(down) + r(left) + r(right)) / 5
            val blurG = (cg + g(up) + g(down) + g(left) + g(right)) / 5
            val blurB = (cb + b(up) + b(down) + b(left) + b(right)) / 5

            val outR = (cr + amount * (cr - blurR)).roundToInt()
            val outG = (cg + amount * (cg - blurG)).roundToInt()
            val outB = (cb + amount * (cb - blurB)).roundToInt()

            outPixels[idx] =
                (a(c) shl 24) or (clamp(outR) shl 16) or (clamp(outG) shl 8) or clamp(outB)
        }
    }

    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(outPixels, 0, w, 0, 0, w, h)
    return out
}

internal fun applyExifOrientation(src: Bitmap, orientation: Int): Bitmap {
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
        return src
    }
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { // flip + rotate 90
            m.preScale(-1f, 1f)
            m.postRotate(90f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { // flip + rotate 270
            m.preScale(-1f, 1f)
            m.postRotate(270f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        else -> return src
    }
    return try {
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    } catch (_: Exception) {
        src
    }
}

internal suspend fun enhanceDatasetFolders(
    context: Context,
    folders: List<File>,
    onProgress: (processed: Int, total: Int, message: String, etaMs: Long?) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (folders.isEmpty()) return@withContext false

            val allImageFilesByFolder = folders.associateWith { listImageFiles(it) }
            val total = allImageFilesByFolder.values.sumOf { it.size }
            if (total <= 0) return@withContext false

            val startAt = SystemClock.elapsedRealtime()
            var processed = 0

            fun computeEtaMs(): Long? {
                if (processed <= 0) return null
                val elapsed = SystemClock.elapsedRealtime() - startAt
                val avg = elapsed.toDouble() / processed.toDouble()
                val remain = (total - processed).coerceAtLeast(0)
                return (avg * remain.toDouble()).toLong().coerceAtLeast(0L)
            }

            onProgress(0, total, "선명도 보정 시작...", computeEtaMs())

            for ((folder, files) in allImageFilesByFolder) {
                if (files.isEmpty()) continue

                val parent = folder.parentFile ?: continue
                val tmp = File(parent, "${folder.name}__enhance_tmp_${System.currentTimeMillis()}").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }

                for (f in files) {
                    val raw = BitmapFactory.decodeFile(f.absolutePath)
                    if (raw == null) {
                        processed++
                        onProgress(processed, total, "이미지 로드 실패: ${f.name}", computeEtaMs())
                        continue
                    }
                    val oriented = try {
                        val exif = ExifInterface(f.absolutePath)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        applyExifOrientation(raw, orientation)
                    } catch (_: Exception) {
                        raw
                    }.also { bmp2 ->
                        if (bmp2 !== raw) {
                            try { raw.recycle() } catch (_: Exception) {}
                        }
                    }

                    val out = try {
                        sharpenBitmapFast(oriented, amount = 1.25f)
                    } finally {
                        try { oriented.recycle() } catch (_: Exception) {}
                    }

                    val outFile = File(tmp, f.name)
                    try {
                        FileOutputStream(outFile).use { fos ->
                            val fmt = when (f.extension.lowercase()) {
                                "png" -> Bitmap.CompressFormat.PNG
                                "webp" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                                else -> Bitmap.CompressFormat.JPEG
                            }
                            val quality = if (fmt == Bitmap.CompressFormat.PNG) 100 else 95
                            out.compress(fmt, quality, fos)
                        }

                        // 저장된 결과는 EXIF 회전이 다시 적용되지 않도록 NORMAL로 고정
                        if (outFile.extension.lowercase() == "jpg" || outFile.extension.lowercase() == "jpeg") {
                            try {
                                val outExif = ExifInterface(outFile.absolutePath)
                                outExif.setAttribute(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL.toString()
                                )
                                outExif.saveAttributes()
                            } catch (_: Exception) {}
                        }
                    } finally {
                        try { out.recycle() } catch (_: Exception) {}
                    }

                    processed++
                    onProgress(processed, total, "처리 중... (${processed}/${total})", computeEtaMs())
                }

                // 원본 폴더 삭제 + 임시 폴더를 원래 이름으로 교체
                val backup = File(parent, "${folder.name}__old_${System.currentTimeMillis()}")
                val originalPath = folder.absolutePath
                val original = File(originalPath)

                // 1) 원본 폴더를 백업 이름으로 이동(가능하면)
                if (original.exists()) {
                    val renamed = original.renameTo(backup)
                    if (!renamed) {
                        // rename 실패 시, 그래도 요구사항(자동 삭제)을 위해 삭제 시도
                        try { original.deleteRecursively() } catch (_: Exception) {}
                    }
                }

                // 2) tmp를 원래 경로로 rename 시도
                val target = File(originalPath)
                val moved = tmp.renameTo(target)
                if (!moved) {
                    // rename 실패 시 직접 이동
                    target.mkdirs()
                    tmp.listFiles()?.forEach { child ->
                        try {
                            val dest = File(target, child.name)
                            child.copyTo(dest, overwrite = true)
                            child.delete()
                        } catch (_: Exception) {}
                    }
                    try { tmp.deleteRecursively() } catch (_: Exception) {}
                }

                // 3) 백업 삭제
                try { backup.deleteRecursively() } catch (_: Exception) {}
            }

            onProgress(total, total, "선명도 보정 완료", 0L)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class PlyParseResult(
    val points: FloatArray,
    val count: Int,
    /** 정점당 RGB 0~1, 길이 [count * 3]; PLY에 색이 없으면 null */
    val vertexColors: FloatArray? = null
)

fun parsePlyPoints(file: File): PlyParseResult? {
    if (!file.exists()) return null
    return try {
        var vertexCount = 0
        var headerEnded = false
        val rawPoints = ArrayList<Float>(1024)

        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                if (!headerEnded) {
                    if (trimmed.startsWith("element vertex")) {
                        val parts = trimmed.split(" ")
                        if (parts.size >= 3) {
                            vertexCount = parts[2].toIntOrNull() ?: 0
                        }
                    } else if (trimmed == "end_header") {
                        headerEnded = true
                    }
                } else {
                    if (vertexCount > 0 && rawPoints.size >= vertexCount * 3) return@forEach
                    val parts = trimmed.split(Regex("\\s+"))
                    if (parts.size >= 3) {
                        rawPoints.add(parts[0].toFloatOrNull() ?: 0f)
                        rawPoints.add(parts[1].toFloatOrNull() ?: 0f)
                        rawPoints.add(parts[2].toFloatOrNull() ?: 0f)
                    }
                }
            }
        }

        val count = rawPoints.size / 3
        if (count == 0) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (i in 0 until count) {
            val x = rawPoints[i * 3]
            val y = rawPoints[i * 3 + 1]
            val z = rawPoints[i * 3 + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f
        val maxDim = max(maxX - minX, max(maxY - minY, maxZ - minZ))
        val half = if (maxDim > 0f) maxDim / 2f else 1f

        val points = FloatArray(count * 3)
        for (i in 0 until count) {
            val x = rawPoints[i * 3]
            val y = rawPoints[i * 3 + 1]
            val z = rawPoints[i * 3 + 2]
            points[i * 3] = (x - cx) / half
            points[i * 3 + 1] = (y - cy) / half
            points[i * 3 + 2] = (z - cz) / half
        }

        PlyParseResult(points, count)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private class PlyRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var aPos = 0
    private var uMvp = 0
    private var pointBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private var aspectRatio = 1f

    private var rotationX = 0f
    private var rotationY = 0f

    fun setPoints(points: FloatArray, count: Int) {
        val bb = ByteBuffer.allocateDirect(points.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(points)
        fb.position(0)
        pointBuffer = fb
        vertexCount = count
    }

    fun addRotation(dx: Float, dy: Float) {
        rotationY += dx
        rotationX += dy
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1f
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val buffer = pointBuffer ?: return
        if (vertexCount <= 0) return

        GLES20.glUseProgram(program)

        val proj = FloatArray(16)
        val view = FloatArray(16)
        val model = FloatArray(16)
        val mvp = FloatArray(16)

        GLMatrix.perspectiveM(proj, 0, 45f, aspectRatio, 0.1f, 100f)
        GLMatrix.setLookAtM(view, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        GLMatrix.setIdentityM(model, 0)
        GLMatrix.rotateM(model, 0, rotationX, 1f, 0f, 0f)
        GLMatrix.rotateM(model, 0, rotationY, 0f, 1f, 0f)
        GLMatrix.multiplyMM(mvp, 0, view, 0, model, 0)
        GLMatrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                gl_PointSize = 2.5;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(0.49, 0.83, 0.2, 1.0);
            }
        """
    }
}

private class PlySurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = PlyRenderer()
    private var lastX = 0f
    private var lastY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun loadModel(file: File) {
        Thread {
            val parsed = parsePlyPoints(file)
            if (parsed != null) {
                queueEvent {
                    renderer.setPoints(parsed.points, parsed.count)
                }
            }
        }.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                renderer.addRotation(dx * 0.5f, dy * 0.5f)
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }
}

/**
 * 서버 연결 테스트 함수
 * @param context 컨텍스트
 * @param serverAddress 테스트할 서버 주소 (IP 또는 도메인)
 * @param serverPort 테스트할 서버 포트
 * @param useHttps HTTPS 사용 여부
 * @return 연결 성공 여부
 */
internal suspend fun testServerConnection(
    context: Context,
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

            // 간단한 HEAD 요청으로 서버 연결 테스트
            val protocol = if (useHttps) "https" else "http"
            val url = "$protocol://$serverAddress:$serverPort$UPLOAD_ENDPOINT"
            val request = Request.Builder()
                .url(url)
                .head() // HEAD 요청으로 서버 응답 확인
                .build()

            val response = client.newCall(request).execute()
            val success = response.code in 200..499 // 4xx는 서버가 응답했다는 의미
            response.close()
            
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
fun LlmApiKeySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedProvider by remember {
        mutableStateOf(LlmApiKeyStore.getSelectedProvider(context))
    }
    var claudeKey by remember {
        mutableStateOf(LlmApiKeyStore.getValueForEditing(context, LlmProvider.CLAUDE))
    }
    var openaiKey by remember {
        mutableStateOf(LlmApiKeyStore.getValueForEditing(context, LlmProvider.OPENAI))
    }
    var geminiKey by remember {
        mutableStateOf(LlmApiKeyStore.getValueForEditing(context, LlmProvider.GEMINI))
    }

    BackHandler { onBack() }

    val keyLabel = when (selectedProvider) {
        LlmProvider.CLAUDE -> "Anthropic (클로드) API 키"
        LlmProvider.OPENAI -> "OpenAI (GPT) API 키"
        LlmProvider.GEMINI -> "Google AI (제미나이) API 키"
    }
    val keyPlaceholder = when (selectedProvider) {
        LlmProvider.CLAUDE -> "sk-ant-api03-…"
        LlmProvider.OPENAI -> "sk-…"
        LlmProvider.GEMINI -> "AIza…"
    }
    val keyValue = when (selectedProvider) {
        LlmProvider.CLAUDE -> claudeKey
        LlmProvider.OPENAI -> openaiKey
        LlmProvider.GEMINI -> geminiKey
    }
    val onKeyChange: (String) -> Unit = { v ->
        when (selectedProvider) {
            LlmProvider.CLAUDE -> claudeKey = v
            LlmProvider.OPENAI -> openaiKey = v
            LlmProvider.GEMINI -> geminiKey = v
        }
    }

    val palette = LocalAppUiPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "LLM API 키",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = palette.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI 메뉴에서 사용할 LLM을 선택한 뒤 해당 API 키를 입력하세요. 기본값은 local.properties의 claude_api_key·openai_api_key·gemini_api_key(빌드 시 주입)입니다. 필드를 비우고 저장하면 해당 제공자는 빌드 기본 키를 사용합니다.",
            color = palette.onBackgroundMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "AI 메뉴에 사용할 제공자",
            color = palette.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedProvider == LlmProvider.CLAUDE,
                onClick = { selectedProvider = LlmProvider.CLAUDE },
                label = { Text("클로드", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.brand,
                    selectedLabelColor = palette.onBrand,
                    containerColor = palette.chipUnselectedBg,
                    labelColor = palette.chipUnselectedLabel
                )
            )
            FilterChip(
                selected = selectedProvider == LlmProvider.OPENAI,
                onClick = { selectedProvider = LlmProvider.OPENAI },
                label = { Text("GPT", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.brand,
                    selectedLabelColor = palette.onBrand,
                    containerColor = palette.chipUnselectedBg,
                    labelColor = palette.chipUnselectedLabel
                )
            )
            FilterChip(
                selected = selectedProvider == LlmProvider.GEMINI,
                onClick = { selectedProvider = LlmProvider.GEMINI },
                label = { Text("제미나이", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.brand,
                    selectedLabelColor = palette.onBrand,
                    containerColor = palette.chipUnselectedBg,
                    labelColor = palette.chipUnselectedLabel
                )
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = keyValue,
            onValueChange = onKeyChange,
            label = { Text(keyLabel, color = palette.onBackground) },
            placeholder = { Text(keyPlaceholder, color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.onBackground,
                unfocusedTextColor = palette.onBackground,
                focusedBorderColor = palette.brand,
                unfocusedBorderColor = palette.onBackground.copy(alpha = 0.35f),
                cursorColor = palette.brand,
                focusedLabelColor = palette.onBackground,
                unfocusedLabelColor = palette.onBackground.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.LightGray,
                unfocusedPlaceholderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.onBackground),
                border = BorderStroke(1.dp, palette.divider),
                modifier = Modifier.weight(1f)
            ) {
                Text("닫기")
            }
            Button(
                onClick = {
                    LlmApiKeyStore.saveAll(
                        context,
                        selectedProvider,
                        claudeKey,
                        openaiKey,
                        geminiKey
                    )
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brand,
                    contentColor = palette.onBrand
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("저장", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ServerSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    var serverAddress by remember { mutableStateOf(getServerAddress(context)) }
    var serverPort by remember { mutableStateOf(getServerPort(context).toString()) }
    var useHttps by remember { mutableStateOf(getUseHttps(context)) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.onBackground,
        unfocusedTextColor = palette.onBackground,
        focusedBorderColor = palette.brand,
        unfocusedBorderColor = palette.onBackground.copy(alpha = 0.35f),
        cursorColor = palette.brand,
        focusedLabelColor = palette.onBackground,
        unfocusedLabelColor = palette.onBackground.copy(alpha = 0.7f),
        focusedPlaceholderColor = Color.LightGray,
        unfocusedPlaceholderColor = Color.LightGray
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "서버 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.onBackground
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            label = { Text("서버 주소 (IP 또는 도메인)", color = palette.onBackground) },
            placeholder = { Text("예: 192.168.0.88 또는 example.com", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = serverPort,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    serverPort = newValue
                }
            },
            label = { Text("포트 번호", color = palette.onBackground) },
            placeholder = { Text("예: 8000", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HTTPS 사용",
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                color = palette.onBackground
            )
            androidx.compose.material3.Switch(
                checked = useHttps,
                onCheckedChange = { useHttps = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.onBrand,
                    checkedTrackColor = palette.brand,
                    uncheckedThumbColor = palette.onBackground,
                    uncheckedTrackColor = palette.chipUnselectedBg
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "업로드: $UPLOAD_ENDPOINT",
            fontSize = 14.sp,
            color = palette.onBackgroundMuted
        )
        Text(
            text = "상태조회: $STATUS_ENDPOINT/{task_id}",
            fontSize = 12.sp,
            color = palette.onBackgroundMuted
        )
        Text(
            text = "다운로드: $DOWNLOAD_ENDPOINT/{task_id}",
            fontSize = 12.sp,
            color = palette.onBackgroundMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        val portValue = serverPort.toIntOrNull() ?: DEFAULT_SERVER_PORT
        val protocol = if (useHttps) "https" else "http"
        val previewUrl = if (serverAddress.isNotBlank() && serverPort.isNotBlank()) {
            "$protocol://$serverAddress:$portValue$UPLOAD_ENDPOINT"
        } else {
            ""
        }

        if (previewUrl.isNotEmpty()) {
            Text(
                text = "URL: $previewUrl",
                fontSize = 12.sp,
                color = palette.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val canTest = !isTesting && serverAddress.isNotBlank() && serverPort.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (canTest) palette.brand else palette.chatComposerPillInactive)
                .clickable(enabled = canTest) {
                    isTesting = true
                    testResult = null
                    coroutineScope.launch {
                        val port = serverPort.toIntOrNull() ?: DEFAULT_SERVER_PORT
                        val success = testServerConnection(context, serverAddress, port, useHttps)
                        testResult = if (success) {
                            "연결 성공!"
                        } else {
                            "연결 실패. 서버 주소, 포트, 프로토콜을 확인하세요."
                        }
                        isTesting = false
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isTesting) "테스트 중..." else "서버 연결 테스트",
                color = palette.onBrand,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        testResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        palette.surfaceCard,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = result,
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val canSave = serverAddress.isNotBlank() && serverPort.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (canSave) palette.brand else palette.chatComposerPillInactive)
                .clickable(enabled = canSave) {
                    val port = serverPort.toIntOrNull() ?: DEFAULT_SERVER_PORT
                    saveServerSettings(context, serverAddress, port, useHttps)
                    onBack()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "저장",
                color = palette.onBrand,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// ── 포그라운드 서비스 공통 제어 헬퍼 ─────────────────────────────────────────

/**
 * 포그라운드 서비스를 시작하거나 진행률을 업데이트합니다.
 *
 * @param taskTitle  알림 제목 (예: "배경 제거 중", "광택 제거 중", "3D 모델 생성 중")
 * @param progress   0~100 (불확정이면 -1)
 * @param message    현재 단계 메시지
 * @param startMs    작업 시작 시각 (예상 시간 계산용, 0이면 무시)
 */
internal fun startOrUpdateForegroundService(
    context: Context,
    taskTitle: String,
    progress: Int,
    message: String,
    startMs: Long = 0L
) {
    val intent = Intent(context, AppForegroundService::class.java).apply {
        putExtra(AppForegroundService.EXTRA_TASK_TITLE, taskTitle)
        putExtra(AppForegroundService.EXTRA_PROGRESS, progress)
        putExtra(AppForegroundService.EXTRA_MESSAGE, message)
        if (startMs > 0L) putExtra(AppForegroundService.EXTRA_START_MS, startMs)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

/** 포그라운드 서비스를 완료(100%) 상태로 업데이트하고 종료합니다. */
internal fun stopForegroundService(context: Context, taskTitle: String, doneMessage: String) {
    startOrUpdateForegroundService(context, taskTitle, 100, doneMessage)
    context.stopService(Intent(context, AppForegroundService::class.java))
}

// [추가] 백그라운드 작업 유지를 위한 포그라운드 서비스
class AppForegroundService : Service() {
    companion object {
        const val CHANNEL_ID  = "AppForegroundServiceChannel"
        const val NOTIF_ID    = 1
        const val EXTRA_PROGRESS   = "extra_progress"
        const val EXTRA_MESSAGE    = "extra_message"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_START_MS   = "extra_start_ms"

        /** 알림 ID (하위 호환 보존) */
        const val NOTIFICATION_ID  = NOTIF_ID
    }

    private var taskStartMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "백그라운드 작업",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "업로드·배경 제거·광택 제거 진행 상황을 표시합니다."
                setShowBadge(true)
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val progress  = intent?.getIntExtra(EXTRA_PROGRESS, -1) ?: -1
        val message   = intent?.getStringExtra(EXTRA_MESSAGE) ?: ""
        val taskTitle = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: "백그라운드 작업 중"
        val startMs   = intent?.getLongExtra(EXTRA_START_MS, 0L) ?: 0L
        if (startMs > 0L && taskStartMs == 0L) taskStartMs = startMs

        // ── 예상 잔여 시간 계산 ─────────────────────────────────────────────
        val etaText = if (progress in 1..99 && taskStartMs > 0L) {
            val elapsed = System.currentTimeMillis() - taskStartMs
            val estimated = (elapsed / (progress.toDouble() / 100.0)).toLong()
            val remaining = ((estimated - elapsed) / 1000L).coerceAtLeast(0L)
            when {
                remaining >= 3600 -> " · 약 ${remaining / 3600}시간 ${(remaining % 3600) / 60}분 남음"
                remaining >= 60   -> " · 약 ${remaining / 60}분 ${remaining % 60}초 남음"
                remaining > 0     -> " · 약 ${remaining}초 남음"
                else              -> ""
            }
        } else ""

        // ── 앱 복귀 탭 인텐트 ──────────────────────────────────────────────
        val launchIntent = (
            packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val tapIntent = PendingIntent.getActivity(this, 0, launchIntent, pendingFlags)

        // ── 알림 빌드 ──────────────────────────────────────────────────────
        val subText = if (progress in 0..100) "$progress%" else null
        val contentText = when {
            message.isNotBlank() && etaText.isNotBlank() -> "$message$etaText"
            message.isNotBlank() -> message
            etaText.isNotBlank() -> etaText.trimStart(' ', '·', ' ')
            else -> "백그라운드에서 작업 중입니다."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(taskTitle)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (subText != null) builder.setSubText(subText)

        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true) // 불확정 스피너
        }

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        // 100% 완료 시 자동 종료
        if (progress >= 100) {
            taskStartMs = 0L
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}