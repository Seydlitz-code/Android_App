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

@Composable
fun Mobile3dGsScreen() {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val scope = rememberCoroutineScope()
    val gridThumbPx = rememberGalleryGridThumbEdgePx(columns = 4)

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedColmapUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var showDatasetSourceDialog by remember { mutableStateOf(false) }
    var showGalleryMultiDialog by remember { mutableStateOf(false) }
    var showDatasetFolderDialog by remember { mutableStateOf(false) }

    var galleryImageList by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var tempGallerySelection by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var datasetFoldersList by remember { mutableStateOf<List<DatasetFolder>>(emptyList()) }

    var mobileGsBusy by remember { mutableStateOf(false) }
    var mobileGsProgress by remember { mutableIntStateOf(0) }
    var mobileGsViewerScene by remember { mutableStateOf<MobileSplatScene?>(null) }
    var mobileGsLastError by remember { mutableStateOf("") }

    val colmapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.size != 3) {
            Toast.makeText(
                context,
                "COLMAP 바이너리 파일을 정확히 3개 선택해 주세요. (선택됨: ${uris.size}개)",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }
        selectedColmapUris = uris.toList()
        for (u in uris) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
        }
    }

    LaunchedEffect(showGalleryMultiDialog) {
        if (showGalleryMultiDialog) {
            tempGallerySelection = emptySet()
            galleryImageList = withContext(Dispatchers.IO) {
                loadCapturedImageUrisOnlySync(context)
            }
        }
    }

    LaunchedEffect(showDatasetFolderDialog) {
        if (showDatasetFolderDialog) {
            datasetFoldersList = withContext(Dispatchers.IO) {
                loadDatasetFoldersSync(context)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Mobile 3DGS",
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "사진과 COLMAP 바이너리(cameras, images, points3D)를 선택한 뒤 아래에서 3D를 생성하고 전용 뷰어로 확인합니다.",
                color = palette.onBackground.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 사진 데이터셋
            Text(
                text = "사진 데이터셋",
                color = palette.onBackground.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceCard)
                    .clickable { showDatasetSourceDialog = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = palette.brand,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "사진 데이터셋 선택",
                            color = palette.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedImageUris.isEmpty()) {
                                "미선택 · 갤러리 1~${MobileGaussianSplattingScript.MAX_DATASET_IMAGES}장 또는 데이터셋 폴더"
                            } else {
                                "${selectedImageUris.size}장 선택됨"
                            },
                            color = palette.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.onBackground.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // COLMAP 바이너리
            Text(
                text = "COLMAP 바이너리",
                color = palette.onBackground.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceCard)
                    .clickable {
                        try {
                            colmapLauncher.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "파일 선택을 열 수 없습니다: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = palette.brand,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "COLMAP 바이너리 파일 선택 (3개)",
                            color = palette.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedColmapUris.size == 3) {
                                "3개 파일 선택됨"
                            } else {
                                "기기 저장소에서 파일 3개 선택"
                            },
                            color = palette.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = palette.onBackground.copy(alpha = 0.5f)
                )
            }

            if (selectedColmapUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                selectedColmapUris.forEachIndexed { i, u ->
                    val name = resolveDisplayName(context, u) ?: u.lastPathSegment ?: "file_${i + 1}"
                    Text(
                        text = "${i + 1}. $name",
                        color = palette.onBackground.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (mobileGsBusy) {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { mobileGsProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = palette.brand,
                        trackColor = palette.chatComposerPillInactive,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "생성 중… ${mobileGsProgress}%",
                        color = palette.onBackground.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val canColmap = selectedColmapUris.size == 3
            val canImages = selectedImageUris.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (mobileGsBusy || (!canColmap && !canImages)) palette.chatComposerPillInactive
                        else palette.mobileGsCtaEnabledBg
                    )
                    .clickable(enabled = !mobileGsBusy && (canColmap || canImages)) {
                        scope.launch {
                            if (mobileGsBusy) return@launch
                            mobileGsBusy = true
                            mobileGsProgress = 0
                            mobileGsLastError = ""
                            try {
                                val scene = MobileGaussianSplattingScript.runFromSelectedInputs(
                                    context,
                                    selectedImageUris,
                                    selectedColmapUris,
                                    onLog = { line ->
                                        if (line.startsWith("[오류]")) {
                                            val msg = line.removePrefix("[오류]").trim()
                                            withContext(Dispatchers.Main) {
                                                mobileGsLastError = msg
                                            }
                                        }
                                    },
                                    onProgress = { p ->
                                        withContext(Dispatchers.Main) {
                                            mobileGsProgress = p
                                        }
                                    },
                                )
                                if (scene != null && scene.splatCount > 0) {
                                    mobileGsViewerScene = scene
                                    Toast.makeText(
                                        context,
                                        "3D 씬을 생성했습니다. 전용 뷰어에서 확인하세요. (드래그: 회전)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        if (canColmap) {
                                            mobileGsLastError.ifBlank {
                                                "COLMAP 씬 생성에 실패했습니다. 파일명·형식을 확인하세요."
                                            }.take(350)
                                        } else {
                                            "이미지 기반 씬 생성에 실패했습니다."
                                        },
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } finally {
                                mobileGsBusy = false
                            }
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewInAr,
                    contentDescription = null,
                    tint = if (mobileGsBusy || (!canColmap && !canImages)) {
                        palette.onBackground.copy(alpha = 0.35f)
                    } else {
                        palette.mobileGsCtaOnEnabled
                    },
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (mobileGsBusy) "처리 중…" else "3D 생성 후 뷰어 열기",
                    color = if (mobileGsBusy || (!canColmap && !canImages)) {
                        palette.onBackground.copy(alpha = 0.45f)
                    } else {
                        palette.mobileGsCtaOnEnabled
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    canColmap ->
                        "COLMAP 우선: points3D로 씬을 만듭니다." +
                            if (selectedImageUris.isEmpty()) " (사진 미선택)" else ""
                    canImages ->
                        "사진만 선택됨: 온디바이스 깊이 역투영 파이프라인을 사용합니다."
                    else ->
                        "COLMAP 3개(cameras, images, points3D) 또는 사진을 선택하세요."
                },
                color = palette.onBackground.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }

    // ── 입력 소스: 갤러리(다중) vs 데이터셋 폴더 ──
    if (showDatasetSourceDialog) {
        Dialog(onDismissRequest = { showDatasetSourceDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.dialogSurface)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "사진 데이터셋 선택",
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = palette.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showDatasetSourceDialog = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "갤러리에서 1~${MobileGaussianSplattingScript.MAX_DATASET_IMAGES}장을 고르거나, 데이터셋 폴더 전체를 불러옵니다.",
                        color = palette.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceCard)
                            .clickable {
                                showDatasetSourceDialog = false
                                showGalleryMultiDialog = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = palette.brand,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "사진",
                            color = palette.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceCard)
                            .clickable {
                                showDatasetSourceDialog = false
                                showDatasetFolderDialog = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = palette.brand,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "데이터셋 폴더",
                            color = palette.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ── 갤러리 다중 선택 ──
    if (showGalleryMultiDialog) {
        Dialog(onDismissRequest = { showGalleryMultiDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.dialogSurface)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "갤러리에서 선택 (${tempGallerySelection.size}/$MobileGaussianSplattingScript.MAX_DATASET_IMAGES)",
                            color = palette.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = palette.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showGalleryMultiDialog = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "탭하여 선택/해제 · 1장 이상 ${MobileGaussianSplattingScript.MAX_DATASET_IMAGES}장 이하",
                        color = palette.onBackground.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (galleryImageList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "갤러리에 이미지가 없습니다.",
                                color = palette.onBackground.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(galleryImageList) { uri ->
                                val sel = uri in tempGallerySelection
                                Image(
                                    painter = rememberGalleryGridPhotoPainter(uri, gridThumbPx),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (sel) 3.dp else 1.dp,
                                            color = if (sel) palette.brand else palette.onBackground.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            tempGallerySelection = tempGallerySelection.toMutableSet().apply {
                                                if (contains(uri)) remove(uri)
                                                else {
                                                    if (size >= MobileGaussianSplattingScript.MAX_DATASET_IMAGES) {
                                                        Toast.makeText(
                                                            context,
                                                            "최대 ${MobileGaussianSplattingScript.MAX_DATASET_IMAGES}장까지 선택할 수 있습니다.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else add(uri)
                                                }
                                            }
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        Text(
                            text = "취소",
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showGalleryMultiDialog = false }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        Text(
                            text = "확인",
                            color = palette.brand,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val n = tempGallerySelection.size
                                    if (n !in 1..MobileGaussianSplattingScript.MAX_DATASET_IMAGES) {
                                        Toast.makeText(
                                            context,
                                            "1장 이상 ${MobileGaussianSplattingScript.MAX_DATASET_IMAGES}장 이하로 선택해 주세요.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@clickable
                                    }
                                    selectedImageUris = tempGallerySelection.toList()
                                    showGalleryMultiDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    // ── 데이터셋 폴더 목록 ──
    if (showDatasetFolderDialog) {
        Dialog(onDismissRequest = { showDatasetFolderDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.dialogSurface)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "데이터셋 폴더 선택",
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = palette.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showDatasetFolderDialog = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (datasetFoldersList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "데이터셋 폴더가 없습니다.",
                                color = palette.onBackground.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(datasetFoldersList) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.surfaceCard)
                                        .clickable {
                                            scope.launch(Dispatchers.IO) {
                                                val imageExts = setOf("jpg", "jpeg", "png", "webp")
                                                val files = folder.dir.listFiles { f ->
                                                    f.isFile && imageExts.contains(f.extension.lowercase())
                                                }?.sortedBy {
                                                    it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE
                                                } ?: emptyList()
                                                if (files.isEmpty()) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "이 폴더에 이미지가 없습니다.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    return@launch
                                                }
                                                val capped = files.take(MobileGaussianSplattingScript.MAX_DATASET_IMAGES)
                                                withContext(Dispatchers.Main) {
                                                    if (files.size > MobileGaussianSplattingScript.MAX_DATASET_IMAGES) {
                                                        Toast.makeText(
                                                            context,
                                                            "이미지 ${files.size}장 중 앞의 ${MobileGaussianSplattingScript.MAX_DATASET_IMAGES}장만 사용합니다.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                    selectedImageUris = capped.map { Uri.fromFile(it) }
                                                    showDatasetFolderDialog = false
                                                }
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (folder.coverUri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(folder.coverUri),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = null,
                                            tint = palette.brand,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            color = palette.onBackground,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${folder.count}장",
                                            color = palette.onBackground.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    mobileGsViewerScene?.let { sc ->
        BackHandler { mobileGsViewerScene = null }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(10_000f)
                .systemBarsPadding()
        ) {
            key(sc) {
                AndroidView(
                    factory = { ctx ->
                        MobileGaussianSplatGlView(ctx).apply { setScene(sc) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = { mobileGsViewerScene = null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = palette.onBackground
                )
            }
        }
    }
}
