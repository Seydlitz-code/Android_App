package com.example.app_01

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.ContentResolver
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.material.icons.outlined.Description
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
fun LibraryTabButton(
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
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false
        )
    }
}





// ── 권한 관리 화면 ─────────────────────────────────────────────────────────────

@Composable
fun PermissionManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current

    // 권한 상태를 실시간 반영하기 위해 화면이 포커스를 가질 때마다 다시 체크
    var refreshKey by remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissions = remember(refreshKey) { AppPermissions.list(context) }

    // 미승인 권한 일괄 요청 런처
    val requestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshKey++ }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // ── 헤더 ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< 권한 관리",
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            // 미승인 권한이 있으면 일괄 요청 버튼 표시
            val ungrantedApplicable = permissions.filter { !it.isGranted(context) }
            if (ungrantedApplicable.isNotEmpty()) {
                Text(
                    text = "전체 허용",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A6EBB))
                        .clickable {
                            requestLauncher.launch(
                                ungrantedApplicable.map { it.manifestPermission }.toTypedArray()
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        // ── 안내 문구 ──────────────────────────────────────────────────────────
        Text(
            text = "항목을 탭하면 안드로이드 설정에서 권한을 직접 변경할 수 있습니다.",
            color = palette.onBackgroundMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // ── 권한 목록 ──────────────────────────────────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(permissions) { perm ->
                val granted = perm.isGranted(context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 앱 설정 화면으로 이동
                            val intent = Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 상태 아이콘
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (granted) Color(0xFF1A6B2F) else Color(0xFF6B1A1A)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // 권한 이름 + 설명
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perm.name,
                            color = palette.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = perm.description,
                            color = palette.onBackgroundMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 상태 뱃지
                    Text(
                        text = if (granted) "허용됨" else "거부됨",
                        color = if (granted) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                (if (granted) Color(0xFF4CAF50) else Color(0xFFFF6B6B))
                                    .copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 66.dp)
                        .height(0.5.dp)
                        .background(palette.divider.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun SensorCheckScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    
    BackHandler {
        onBack()
    }

    // 어플 구동(공간 촬영 및 기울기 측정)에 필수적인 센서만 구성
    val sensorItems = remember {
        listOf(
            SensorInfo("회전 벡터(게임/일반)", Sensor.TYPE_ROTATION_VECTOR, Icons.Outlined.Explore),
            SensorInfo("자이로스코프", Sensor.TYPE_GYROSCOPE, Icons.Outlined.ScreenRotation),
            SensorInfo("가속도계", Sensor.TYPE_ACCELEROMETER, Icons.Outlined.Speed),
            SensorInfo("자기장 센서", Sensor.TYPE_MAGNETIC_FIELD, Icons.Outlined.Navigation)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "센서 확인",
                    color = palette.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(2.dp)
                        .background(palette.brand)
                )
            }
        }

        // 3열 그리드 레이아웃
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sensorItems) { item ->
                val isPresent = if (item.type == Sensor.TYPE_ROTATION_VECTOR) {
                    sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
                        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
                } else {
                    sensorManager.getDefaultSensor(item.type) != null
                }
                SensorGridItem(item.name, item.icon, isPresent)
            }
        }
    }
}

data class SensorInfo(val name: String, val type: Int, val icon: ImageVector)

data class DatasetFolder(
    val name: String,
    val dir: File,
    val coverUri: Uri?,
    val count: Int
)

data class PlyModel(
    val name: String,
    val file: File,
    val lastModified: Long
)

internal data class Model3dSplitLibrary(
    val plyModels: List<PlyModel>,
    val objModels: List<PlyModel>,
)

/** 갤러리 오버플로 메뉴에서 선택한 뒤 이미지 선택·확인으로 이어지는 동작 */
private enum class PendingGalleryMenuAction {
    None,
    BackgroundRemove,
    GlareRemove,
    CreateDatasetFolder,
    Export,
    Share
}

/** 데이터셋폴더 목록에서 오버플로 메뉴 → 폴더 선택 후 확인 시 수행할 동작 */
private enum class PendingDatasetMenuAction {
    None,
    BackgroundRemove,
    GlareRemove,
    Share
}

/** 서버 파이프라인 완료 후 로컬에 저장된 결과(PLY·분석 이미지·JSON 등) */
data class ServerPipelineResultBundle(
    val taskId: String,
    val plyFile: File,
    val directory: File,
    val filesByKey: Map<String, File>,
)


@Composable
fun SensorGridItem(name: String, icon: ImageVector, isPresent: Boolean) {
    val palette = LocalAppUiPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isPresent) palette.onBackground else palette.error,
                modifier = Modifier.size(48.dp)
            )
            
            // 미보유 시 빨간색 금지 표시 (원 + 사선)
            if (!isPresent) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    // 빨간 원
                    drawCircle(
                        color = Color.Red,
                        style = Stroke(width = strokeWidth)
                    )
                    // 사선
                    drawLine(
                        color = Color.Red,
                        start = Offset(size.width * 0.2f, size.height * 0.2f),
                        end = Offset(size.width * 0.8f, size.height * 0.8f),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = name,
            color = if (isPresent) palette.onBackground else palette.error,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}


private val LibraryHubThumbRadius = 26.dp

@Composable
private fun PlyModelThumbnailImage(
    model: PlyModel,
    thumbRefresh: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val themeMode = LocalAppUiThemeMode.current
    var thumbUri by remember(model.file.absolutePath) { mutableStateOf<Uri?>(null) }
    LaunchedEffect(model.file.absolutePath, model.lastModified, thumbRefresh, themeMode) {
        thumbUri = try {
            Model3dThumbnail.generateOrGetAsync(context, model.file)?.let { Uri.fromFile(it) }
        } catch (_: Throwable) {
            null
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (thumbUri != null) {
            Image(
                painter = rememberAsyncImagePainter(thumbUri),
                contentDescription = model.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = "3D Model",
                tint = palette.onBackground.copy(alpha = 0.48f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/** AI CAD 라이브러리 STL 그리드 셀: [Model3dThumbnail] + 그리드용 Coil 크기 */
@Composable
private fun AiCadStlGridThumbnail(
    stlFile: File,
    libraryVersion: Int,
    thumbEdgePx: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val themeMode = LocalAppUiThemeMode.current
    var thumbUri by remember(stlFile.absolutePath, stlFile.lastModified(), libraryVersion) {
        mutableStateOf<Uri?>(null)
    }
    LaunchedEffect(stlFile.absolutePath, stlFile.lastModified(), libraryVersion, themeMode) {
        thumbUri = try {
            Model3dThumbnail.generateOrGetAsync(context, stlFile)?.let { Uri.fromFile(it) }
        } catch (_: Throwable) {
            null
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (thumbUri != null) {
            Image(
                painter = rememberGalleryGridPhotoPainter(thumbUri!!, thumbEdgePx),
                contentDescription = "AI CAD STL 미리보기",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = "STL",
                tint = palette.onBackground.copy(alpha = 0.48f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Model3dLibraryGridItem(
    model: PlyModel,
    thumbRefresh: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalAppUiPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (palette.isDark) Color(0xFF1A1A1A) else Color.White)
                .border(
                    width = if (isSelected) 4.dp else 1.dp,
                    color = when {
                        isSelected -> Color.Blue
                        palette.isDark -> Color.White.copy(alpha = 0.2f)
                        else -> Color.Black
                    },
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            PlyModelThumbnailImage(
                model = model,
                thumbRefresh = thumbRefresh,
                modifier = Modifier.fillMaxSize()
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color(0xFF7ED321), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = model.file.name,
            color = palette.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** 3D 모델 탭: PLY / OBJ 포맷별 서브 라이브러리 카드 (메인 허브와 동일한 카드 스타일) */
@Composable
private fun Model3dFormatHubGrid(
    plyCount: Int,
    objCount: Int,
    plyCoverUri: Uri?,
    objCoverUri: Uri?,
    onOpenPly: () -> Unit,
    onOpenObj: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppUiPalette.current
    val thumbBg = if (palette.isDark) Color(0xFF1C1C1E) else Color.White
    val thumbBorder = if (palette.isDark) Color.White.copy(alpha = 0.22f) else Color.Black
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(2, key = { if (it == 0) "hub_ply" else "hub_obj" }) { idx ->
            val title = if (idx == 0) "PLY" else "OBJ"
            val countLabel = if (idx == 0) "${plyCount}개" else "${objCount}개"
            val coverUri = if (idx == 0) plyCoverUri else objCoverUri
            val tint = palette.onBackground.copy(alpha = 0.48f)
            val onOpen = if (idx == 0) onOpenPly else onOpenObj
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(LibraryHubThumbRadius))
                        .border(1.dp, thumbBorder, RoundedCornerShape(LibraryHubThumbRadius))
                        .background(thumbBg)
                ) {
                    if (coverUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(coverUri),
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = null,
                                tint = tint.copy(alpha = 0.85f),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    color = palette.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = countLabel,
                    color = palette.onBackgroundMuted,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** 갤럭시 갤러리 스타일 라이브러리 앨범 허브 (데이터셋 / 분석 데이터 구역) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryAlbumHubGrid(
    images: List<Uri>,
    datasetFolders: List<DatasetFolder>,
    model3dTotalCount: Int,
    model3dCoverUri: Uri?,
    aiCadStlFiles: List<File>,
    gsPreviewCount: Int,
    gsAnalysisCount: Int,
    gsPreviewCoverUri: Uri?,
    gsAnalysisCoverUri: Uri?,
    jsonLibraryCount: Int,
    arcoreLibraryCount: Int,
    onOpenSection: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppUiPalette.current
    val thumbBg = if (palette.isDark) Color(0xFF1C1C1E) else Color.White
    val thumbBorder = if (palette.isDark) Color.White.copy(alpha = 0.22f) else Color.Black
    val placeholderInk = palette.onBackground.copy(alpha = 0.48f)
    val galleryCover = images.firstOrNull()
    val datasetCover = datasetFolders.maxByOrNull { it.dir.lastModified() }?.coverUri
        ?: datasetFolders.firstOrNull()?.coverUri
    val cadCoverUri: Uri? = aiCadStlFiles.firstOrNull()?.let { stl ->
        val glb = File(stl.parent ?: "", "${stl.nameWithoutExtension}.glb")
        if (glb.isFile) Uri.fromFile(glb) else null
    }

    data class HubEntry(
        val tab: LibraryTab,
        val title: String,
        val countLabel: String,
        val coverUri: Uri?,
        val placeholderIcon: ImageVector,
        val placeholderTint: Color
    )

    val datasetEntries = listOf(
        HubEntry(
            LibraryTab.AI_CAD, "AICAD",
            "${aiCadStlFiles.size}개",
            cadCoverUri,
            Icons.Outlined.Build,
            placeholderInk
        ),
        HubEntry(
            LibraryTab.DATASET, "데이터셋 폴더",
            "${datasetFolders.size}개",
            datasetCover,
            Icons.Outlined.Folder,
            placeholderInk
        ),
        HubEntry(
            LibraryTab.GALLERY, "갤러리",
            "${images.size}장",
            galleryCover,
            Icons.Outlined.PhotoLibrary,
            placeholderInk
        ),
        HubEntry(
            LibraryTab.AR_CORE_LIBRARY, "ARCore",
            "${arcoreLibraryCount}개",
            null,
            Icons.Filled.ViewInAr,
            placeholderInk
        )
    )
    val analysisEntries = listOf(
        HubEntry(
            LibraryTab.MODEL_3D, "3D 모델",
            "${model3dTotalCount}개",
            model3dCoverUri,
            Icons.Outlined.Public,
            placeholderInk
        ),
        HubEntry(
            LibraryTab.GS_PREVIEW, "3DGS 미리보기",
            "${gsPreviewCount}개",
            gsPreviewCoverUri,
            Icons.Filled.ViewInAr,
            placeholderInk
        ),
        HubEntry(
            LibraryTab.GS_ANALYSIS, "3DGS 분석 이미지",
            "${gsAnalysisCount}개",
            gsAnalysisCoverUri,
            Icons.Filled.AutoFixHigh,
            placeholderInk
        ),
        HubEntry(
            LibraryTab.JSON_LIBRARY, "JSON",
            "${jsonLibraryCount}개",
            null,
            Icons.Outlined.Description,
            placeholderInk
        )
    )

    @Composable
    fun HubTile(e: HubEntry) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSection(e.tab) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(LibraryHubThumbRadius))
                    .border(1.dp, thumbBorder, RoundedCornerShape(LibraryHubThumbRadius))
                    .background(thumbBg)
            ) {
                if (e.coverUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(e.coverUri),
                        contentDescription = e.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = e.placeholderIcon,
                            contentDescription = null,
                            tint = e.placeholderTint.copy(alpha = 0.85f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                if (e.tab == LibraryTab.GALLERY) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(
                                if (palette.isDark) Color.Black.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.92f),
                                CircleShape
                            )
                            .padding(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = if (palette.isDark) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = e.title,
                modifier = Modifier.fillMaxWidth(),
                color = palette.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = e.countLabel,
                modifier = Modifier.fillMaxWidth(),
                color = palette.onBackgroundMuted,
                fontSize = 13.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "데이터셋",
                    color = palette.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = palette.divider)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        items(datasetEntries.size, key = { "ds_${datasetEntries[it].tab.name}" }) { idx ->
            HubTile(datasetEntries[idx])
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "분석 데이터",
                    color = palette.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = palette.divider)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        items(analysisEntries.size, key = { "an_${analysisEntries[it].tab.name}" }) { idx ->
            HubTile(analysisEntries[idx])
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    images: List<Uri>,
    libraryTab: LibraryTab,
    aiCadLibraryVersion: Int = 0,
    showLibraryHub: Boolean,
    onLibraryHubVisibilityChange: (Boolean) -> Unit,
    onLibraryTabChange: (LibraryTab) -> Unit,
    onMediaSelected: (Uri, List<Uri>) -> Unit,
    onImageDeleted: () -> Unit,
    onAiCadLibraryInvalidate: () -> Unit = {},
    galleryGridState: androidx.compose.foundation.lazy.grid.LazyGridState =
        androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    onServerPipelineOpenImageViewer: (List<Uri>, Int) -> Unit = { _, _ -> },
    onServerPipelineStart3dgsAi: (Pending3dgsServerAutoSend) -> Unit = {},
    serverPipelineCompleteBundle: ServerPipelineResultBundle?,
    onServerPipelineCompleteBundleChange: (ServerPipelineResultBundle?) -> Unit,
    serverArtifactLibraryVersion: Int,
    onEnqueueBackground3dgsFromBundle: (ServerPipelineResultBundle) -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val themeMode = LocalAppUiThemeMode.current
    /** 그리드 셀에 맞춘 디코딩 크기 — 사진(Coil)·동영상 첫 프레임 공통 */
    val gridThumbPx = rememberGalleryGridThumbEdgePx(columns = 4)
    LaunchedEffect(images, gridThumbPx, libraryTab, showLibraryHub) {
        if (!showLibraryHub && libraryTab == LibraryTab.GALLERY && images.isNotEmpty()) {
            prefetchGalleryGridThumbnails(
                context.applicationContext,
                images,
                gridThumbPx,
            )
        }
    }
    var serverTaskManifestInfos by remember { mutableStateOf<List<ServerTaskManifestInfo>>(emptyList()) }
    var jsonLibraryFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var jsonLibraryDetailFile by remember { mutableStateOf<File?>(null) }
    var arcoreLibraryFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var arcoreLibraryDetailFile by remember { mutableStateOf<File?>(null) }
    /** 길게 누르거나 선택 모드에서 탭해 지정한 ARCore·JSON·3DGS 미리보기/분석 삭제 대상(정규화 절대 경로) */
    var selectedLibraryDeletePaths by remember { mutableStateOf(emptySet<String>()) }
    var showDeleteLibraryItemsConfirm by remember { mutableStateOf(false) }
    var libraryMiscRefresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(libraryTab, serverArtifactLibraryVersion, libraryMiscRefresh) {
        serverTaskManifestInfos = withContext(Dispatchers.IO) { scanServerTaskManifestInfos(context) }
        jsonLibraryFiles = withContext(Dispatchers.IO) { JsonLibrary.listFilesSorted(context) }
        arcoreLibraryFiles = withContext(Dispatchers.IO) { ArcoreLibrary.listFilesSorted(context) }
    }
    val gsPreviewUris = remember(serverTaskManifestInfos) { previewUrisForServerTasks(serverTaskManifestInfos) }
    val gsAnalysisUris = remember(serverTaskManifestInfos) { analysisImageUrisForServerTasks(serverTaskManifestInfos) }
    val gsPreviewCount = remember(serverTaskManifestInfos) { countPreviewTasks(serverTaskManifestInfos) }
    val gsAnalysisCount = remember(serverTaskManifestInfos) { countAnalysisTasks(serverTaskManifestInfos) }
    val gsPreviewCoverUri = gsPreviewUris.firstOrNull()
    val gsAnalysisCoverUri = gsAnalysisUris.firstOrNull()
    val noServerResponseMsg = "서버에 대한 응답이 없습니다.\n서버 연결을 확인해주십시오."
    var isEditMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0 to 0) } // (current, total)
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var uploadSourceTab by remember { mutableStateOf<LibraryTab?>(null) }
    var showUploadResultPopup by remember { mutableStateOf(false) }
    var uploadResultPopupMessage by remember { mutableStateOf<String?>(null) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var isDatasetEditMode by remember { mutableStateOf(false) }
    var selectedDatasetFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDatasetDeleteConfirm by remember { mutableStateOf(false) }
    var libraryAssetEditMode by remember { mutableStateOf(false) }
    var selectedLibraryAssetPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showLibraryAssetDeleteConfirm by remember { mutableStateOf(false) }
    var showGalleryOverflowMenu by remember { mutableStateOf(false) }
    var showDatasetOverflowMenu by remember { mutableStateOf(false) }
    var pendingGalleryMenuAction by remember { mutableStateOf(PendingGalleryMenuAction.None) }
    var pendingDatasetMenuAction by remember { mutableStateOf(PendingDatasetMenuAction.None) }
    /** 갤러리 [selectedItems] 대신 배경 제거 대상(데이터셋폴더에서 모은 URI) */
    var pendingBulkImageUrisForBgRemove by remember { mutableStateOf<List<Uri>?>(null) }
    /** 배경 제거가 데이터셋 폴더에서 시작된 경우, 결과 저장용 새 폴더 이름에 쓸 원본 폴더 경로 집합 */
    var pendingDatasetFolderPathsForBulkBgRemove by remember { mutableStateOf<Set<String>?>(null) }
    var showDatasetDeleteAllConfirm by remember { mutableStateOf(false) }
    var showNewDatasetFolderNameDialog by remember { mutableStateOf(false) }
    var newDatasetFolderNameInput by remember { mutableStateOf("") }
    var pendingDatasetFolderImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var datasetFolders by remember { mutableStateOf<List<DatasetFolder>>(emptyList()) }
    var datasetImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentDatasetFolder by remember { mutableStateOf<DatasetFolder?>(null) }
    var libraryDetailScreen by remember { mutableStateOf(LibraryDetailScreen.NONE) }
    var plyLibraryModels by remember { mutableStateOf<List<PlyModel>>(emptyList()) }
    var objLibraryModels by remember { mutableStateOf<List<PlyModel>>(emptyList()) }
    val allModel3dLibrary = plyLibraryModels + objLibraryModels
    var currentPlyModel by remember { mutableStateOf<PlyModel?>(null) }

    val objViewerPathKey =
        if (libraryDetailScreen == LibraryDetailScreen.OBJ_VIEWER && currentPlyModel != null) {
            currentPlyModel!!.file.absolutePath
        } else {
            null
        }
    var objViewerConverting by remember(objViewerPathKey) { mutableStateOf(true) }
    var objViewerObjFile by remember(objViewerPathKey) { mutableStateOf<File?>(null) }
    var objViewerPreviewMesh by remember(objViewerPathKey) { mutableStateOf<ObjParseResult?>(null) }
    var objViewerError by remember(objViewerPathKey) { mutableStateOf<String?>(null) }
    var objViewerSaving by remember(objViewerPathKey) { mutableStateOf(false) }

    var libraryHubModel3dCoverUri by remember { mutableStateOf<Uri?>(null) }
    var plySubHubCoverUri by remember { mutableStateOf<Uri?>(null) }
    var objSubHubCoverUri by remember { mutableStateOf<Uri?>(null) }
    var libraryModelThumbRefresh by remember { mutableStateOf(0) }

    var aiCadStlFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedAiCadStlFile by remember { mutableStateOf<File?>(null) }

    // [추가] 내보내기/가져오기 작업 상태
    var isTransferring by remember { mutableStateOf(false) }
    val transferScope = rememberCoroutineScope()

    LaunchedEffect(objViewerPathKey) {
        if (objViewerPathKey == null) return@LaunchedEffect
        val plyFile = File(objViewerPathKey)
        objViewerConverting = true
        objViewerError = null
        objViewerObjFile = null
        objViewerPreviewMesh = null
        objViewerSaving = false
        try {
            if (plyFile.extension.equals("obj", ignoreCase = true)) {
                val mesh = withContext(Dispatchers.IO) {
                    parseObjVertices(plyFile)
                }
                objViewerPreviewMesh = mesh
                objViewerObjFile = plyFile
                objViewerError = if (mesh == null) {
                    "OBJ를 읽을 수 없습니다."
                } else {
                    null
                }
                objViewerConverting = false
            } else {
                val savedObj = File(ModelLibraryPaths.objDir(context), "${plyFile.nameWithoutExtension}.obj")
                if (savedObj.exists() &&
                    savedObj.length() > 0L &&
                    savedObj.lastModified() >= plyFile.lastModified()
                ) {
                    val mesh = withContext(Dispatchers.IO) {
                        parseObjVertices(savedObj)
                    }
                    objViewerPreviewMesh = mesh
                    objViewerObjFile = savedObj
                    objViewerError = if (mesh == null) {
                        "저장된 OBJ를 읽을 수 없습니다."
                    } else {
                        null
                    }
                    objViewerConverting = false
                } else {
                    val result = withContext(Dispatchers.IO) {
                        convertPlyToObjCached(context, plyFile)
                    }
                    val out = result.file
                    objViewerPreviewMesh = result.previewMesh
                    if (out == null || !out.exists()) {
                        objViewerError = result.error ?: "OBJ 변환에 실패했습니다."
                        objViewerConverting = false
                    } else {
                        objViewerObjFile = out
                        objViewerConverting = false
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            objViewerError = "처리 중 오류: ${t.message ?: t.javaClass.simpleName}"
            objViewerConverting = false
        }
    }

    val model3dCoverSource = remember(plyLibraryModels, objLibraryModels) {
        (plyLibraryModels + objLibraryModels).maxByOrNull { it.lastModified }
    }
    LaunchedEffect(
        model3dCoverSource?.file?.absolutePath,
        model3dCoverSource?.lastModified,
        libraryModelThumbRefresh,
        themeMode
    ) {
        val f = model3dCoverSource?.file
        if (f == null) {
            libraryHubModel3dCoverUri = null
            return@LaunchedEffect
        }
        libraryHubModel3dCoverUri = try {
            Model3dThumbnail.generateOrGetAsync(context, f)?.let { Uri.fromFile(it) }
        } catch (_: Throwable) {
            null
        }
    }

    val plySubHubSource = remember(plyLibraryModels) {
        plyLibraryModels.maxByOrNull { it.lastModified }?.file
    }
    LaunchedEffect(
        plySubHubSource?.absolutePath,
        plySubHubSource?.lastModified(),
        libraryModelThumbRefresh,
        themeMode
    ) {
        val f = plySubHubSource
        if (f == null) {
            plySubHubCoverUri = null
            return@LaunchedEffect
        }
        plySubHubCoverUri = try {
            Model3dThumbnail.generateOrGetAsync(context, f)?.let { Uri.fromFile(it) }
        } catch (_: Throwable) {
            null
        }
    }

    val objSubHubSource = remember(objLibraryModels) {
        objLibraryModels.maxByOrNull { it.lastModified }?.file
    }
    LaunchedEffect(
        objSubHubSource?.absolutePath,
        objSubHubSource?.lastModified(),
        libraryModelThumbRefresh,
        themeMode
    ) {
        val f = objSubHubSource
        if (f == null) {
            objSubHubCoverUri = null
            return@LaunchedEffect
        }
        objSubHubCoverUri = try {
            Model3dThumbnail.generateOrGetAsync(context, f)?.let { Uri.fromFile(it) }
        } catch (_: Throwable) {
            null
        }
    }

    // [추가] 1차 배경제거 작업 상태
    var isBgRemoving by remember { mutableStateOf(false) }
    var showBgRemoveDialog by remember { mutableStateOf(false) }
    var bgRemovePrompt by remember { mutableStateOf("") }
    var bgRemovePromptError by remember { mutableStateOf(false) }

    // [추가] 광택 제거 작업 상태
    var isGlareRemoving by remember { mutableStateOf(false) }
    var glareProgressPercent by remember { mutableStateOf(0) }
    var glareProgressMessage by remember { mutableStateOf("") }
    var glareResultMessage by remember { mutableStateOf<String?>(null) }

    // 3D 모델링 전송 팝업 상태
    var show3DModelingDialog by remember { mutableStateOf(false) }
    var modelingPromptText by remember { mutableStateOf("") }
    var modelingPromptError by remember { mutableStateOf(false) }
    var pending3DSourceTab by remember { mutableStateOf<LibraryTab?>(null) }
    var pending3DGalleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pending3DDatasetFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    /** ARCore 라이브러리 ZIP 선택 시 서버 전송용 절대 경로 목록 */
    var pending3DArcoreZipPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    /** 데이터셋 폴더 업로드 시 추가로 보낼 ARCore ZIP(content Uri, 선택) */
    var pending3DArcoreZipUriForDataset by remember { mutableStateOf<Uri?>(null) }
    /** 3D 모델링 다이얼로그에서 ARCore ZIP을 앱 ARCore 라이브러리에서 고를 때 */
    var showDatasetArcoreLibraryPicker by remember { mutableStateOf(false) }
    LaunchedEffect(show3DModelingDialog) {
        if (!show3DModelingDialog) showDatasetArcoreLibraryPicker = false
    }
    val datasetArcoreZipChoices = remember(arcoreLibraryFiles) {
        arcoreLibraryFiles.filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
    }
    var sam3ProgressPercent by remember { mutableStateOf(0) }
    var sam3ProgressMessage by remember { mutableStateOf("") }
    var sam3ResultMessage by remember { mutableStateOf<String?>(null) }

    // [추가] 가져오기 런처 (시스템 갤러리/파일 선택)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            uploadSourceTab = LibraryTab.GALLERY
            uploadMessage = "가져오기가 취소되었습니다."
            return@rememberLauncherForActivityResult
        }
        if (isUploading || isTransferring) {
            uploadSourceTab = LibraryTab.GALLERY
            uploadMessage = "다른 작업이 진행 중입니다."
            return@rememberLauncherForActivityResult
        }
        isTransferring = true
        uploadSourceTab = LibraryTab.GALLERY
        uploadMessage = "가져오는 중..."
        transferScope.launch {
            val result = withContext(Dispatchers.IO) {
                importImagesToAppLibrary(context, uris)
            }
            uploadMessage = result.message
            isTransferring = false
            if (result.successCount > 0) {
                onImageDeleted() // 목록 갱신
            }
        }
    }

    fun runExportGallerySelection() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(context, "선택된 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isUploading || isTransferring) {
            Toast.makeText(context, "다른 작업이 진행 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        isTransferring = true
        uploadSourceTab = LibraryTab.GALLERY
        uploadMessage = "내보내는 중..."
        val items = selectedItems.toList()
        transferScope.launch {
            val result = withContext(Dispatchers.IO) {
                exportImagesToSystemGallery(context, items)
            }
            uploadMessage = result.message
            isTransferring = false
            if (pendingGalleryMenuAction == PendingGalleryMenuAction.Export) {
                pendingGalleryMenuAction = PendingGalleryMenuAction.None
            }
        }
    }

    fun runShareGallerySelection(urisOverride: List<Uri>? = null) {
        val list = urisOverride ?: selectedItems.toList()
        if (list.isEmpty()) {
            Toast.makeText(context, "선택된 미디어가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        shareGalleryMediaUris(context, list)
        if (pendingGalleryMenuAction == PendingGalleryMenuAction.Share) {
            pendingGalleryMenuAction = PendingGalleryMenuAction.None
        }
    }

    fun runGlareRemovalOnGallerySelection(
        urisOverride: List<Uri>? = null,
        datasetSourceFolderPaths: Set<String>? = null
    ) {
        val itemsFiltered = (urisOverride ?: selectedItems.toList())
            .filter { !isVideoUri(context, it) }
        val glareEnabled =
            itemsFiltered.isNotEmpty() && !isUploading && !isTransferring && !isGlareRemoving && !isBgRemoving
        if (!glareEnabled) {
            Toast.makeText(
                context,
                "선택된 이미지가 없거나 다른 작업 중입니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val fromDatasetFolders = urisOverride != null
        val outputDir: File = if (
            fromDatasetFolders &&
            !datasetSourceFolderPaths.isNullOrEmpty()
        ) {
            createDatasetBatchResultFolder(context, datasetSourceFolderPaths, "광택제거")
        } else {
            context.getExternalFilesDir(null) ?: context.filesDir
        }
        isGlareRemoving = true
        glareProgressPercent = 0
        glareProgressMessage = "준비 중..."
        val items = itemsFiltered
        val total = items.size
        transferScope.launch {
            val glareStartMs = System.currentTimeMillis()
            startOrUpdateForegroundService(
                context, "광택 제거 중 (${total}장)", 0, "준비 중...", glareStartMs
            )
            var successCount = 0
            var failCount = 0
            items.forEachIndexed { index, uri ->
                val itemLabel = if (total > 1) " (${index + 1}/$total)" else ""
                val basePercent = index * 95 / total
                val nextPercent = (index + 1) * 95 / total

                glareProgressPercent = basePercent
                glareProgressMessage = "이미지 로드 중...$itemLabel"
                startOrUpdateForegroundService(
                    context, "광택 제거 중 (${total}장)",
                    basePercent, "이미지 로드 중...$itemLabel", glareStartMs
                )

                val result = withContext(Dispatchers.IO) {
                    GlareRemovalProcessor.removeGlare(
                        context = context,
                        sourceUri = uri,
                        outputDir = outputDir,
                        onProgress = { step, totalSteps ->
                            if (totalSteps > 0) {
                                val frac = step.toFloat() / totalSteps
                                val mapped = basePercent +
                                    (frac * (nextPercent - basePercent)).toInt()
                                glareProgressPercent = mapped.coerceIn(basePercent, nextPercent - 1)
                                val stepMsg = when (step) {
                                    1 -> "이미지 로드...$itemLabel"
                                    2 -> "빛반사 영역 탐지...$itemLabel"
                                    3 -> "마스크 정제...$itemLabel"
                                    4 -> "조명층 완화...$itemLabel"
                                    5 -> "주변 색 맞춤...$itemLabel"
                                    6 -> "경계·질감...$itemLabel"
                                    7 -> "색 보정...$itemLabel"
                                    else -> "저장 중...$itemLabel"
                                }
                                glareProgressMessage = stepMsg
                                startOrUpdateForegroundService(
                                    context, "광택 제거 중 (${total}장)",
                                    glareProgressPercent, stepMsg, glareStartMs
                                )
                            }
                        }
                    )
                }

                when (result) {
                    is GlareRemovalProcessor.Result.Success -> {
                        successCount++
                        glareProgressPercent = nextPercent
                        glareProgressMessage = "완료$itemLabel"
                    }
                    is GlareRemovalProcessor.Result.Error -> {
                        failCount++
                        glareProgressMessage = "오류: ${result.message}"
                    }
                }
            }
            glareProgressPercent = 100
            isGlareRemoving = false
            glareResultMessage = when {
                successCount > 0 && failCount == 0 && fromDatasetFolders ->
                    "광택 제거 완료\n${successCount}장이 새 데이터셋 폴더 「${outputDir.name}」에 저장되었습니다."
                successCount > 0 && failCount == 0 ->
                    "광택 제거 완료\n${successCount}장이 앱 갤러리에 저장되었습니다."
                successCount > 0 && fromDatasetFolders ->
                    "광택 제거 완료\n${successCount}장 성공, ${failCount}장 실패\n성공분은 「${outputDir.name}」에 저장되었습니다."
                successCount > 0 ->
                    "광택 제거 완료\n${successCount}장 성공, ${failCount}장 실패"
                else ->
                    "광택 제거 실패\n이미지에서 광택이 검출되지 않았거나 처리 중 오류가 발생했습니다."
            }
            val doneMsg = if (successCount > 0) "${successCount}장 완료" else "처리 실패"
            stopForegroundService(context, "광택 제거 완료", doneMsg)
            if (successCount > 0) {
                selectedItems = emptySet()
                isEditMode = false
                pendingGalleryMenuAction = PendingGalleryMenuAction.None
                if (fromDatasetFolders) {
                    isDatasetEditMode = false
                    selectedDatasetFolders = emptySet()
                    pendingDatasetMenuAction = PendingDatasetMenuAction.None
                    loadDatasetFolders(context) { datasetFolders = it }
                }
                onImageDeleted()
            }
        }
    }

    // 뒤로가기 버튼 처리 (편집 모드에서만)
    BackHandler(enabled = isEditMode || isDatasetEditMode || libraryAssetEditMode) {
        when {
            libraryTab == LibraryTab.DATASET && isDatasetEditMode -> {
                isDatasetEditMode = false
                selectedDatasetFolders = emptySet()
                pendingDatasetMenuAction = PendingDatasetMenuAction.None
            }
            libraryAssetEditMode -> {
                libraryAssetEditMode = false
                selectedLibraryAssetPaths = emptySet()
            }
            else -> {
                isEditMode = false
                selectedItems = emptySet()
                pendingGalleryMenuAction = PendingGalleryMenuAction.None
            }
        }
    }

    val atLibrarySectionRoot = !showLibraryHub &&
        libraryDetailScreen == LibraryDetailScreen.NONE &&
        selectedAiCadStlFile == null &&
        !isEditMode &&
        !isDatasetEditMode &&
        !libraryAssetEditMode

    BackHandler(enabled = atLibrarySectionRoot) {
        onLibraryHubVisibilityChange(true)
    }

    LaunchedEffect(showLibraryHub) {
        if (showLibraryHub) {
            selectedLibraryDeletePaths = emptySet()
            showDeleteLibraryItemsConfirm = false
        }
    }

    LaunchedEffect(libraryTab) {
        libraryAssetEditMode = false
        selectedLibraryAssetPaths = emptySet()
        showLibraryAssetDeleteConfirm = false
        selectedLibraryDeletePaths = emptySet()
        showDeleteLibraryItemsConfirm = false
        if (libraryTab != LibraryTab.GALLERY) {
            showGalleryOverflowMenu = false
            pendingGalleryMenuAction = PendingGalleryMenuAction.None
        }
        if (libraryTab != LibraryTab.GALLERY && isEditMode) {
            isEditMode = false
            selectedItems = emptySet()
        }
        if (libraryTab != LibraryTab.DATASET && isDatasetEditMode) {
            isDatasetEditMode = false
            selectedDatasetFolders = emptySet()
        }
        if (libraryTab == LibraryTab.DATASET) {
            loadDatasetFolders(context) { folders ->
                datasetFolders = folders
            }
        }
        if (libraryTab == LibraryTab.MODEL_3D) {
            loadModel3dLibrary(context) { lib ->
                plyLibraryModels = lib.plyModels
                objLibraryModels = lib.objModels
            }
        }
        if (libraryTab != LibraryTab.MODEL_3D) {
            if (libraryDetailScreen == LibraryDetailScreen.OBJ_VIEWER ||
                libraryDetailScreen == LibraryDetailScreen.MODEL_3D_PLY_LIST ||
                libraryDetailScreen == LibraryDetailScreen.MODEL_3D_OBJ_LIST
            ) {
                libraryDetailScreen = LibraryDetailScreen.NONE
                currentPlyModel = null
            }
        }
    }

    LaunchedEffect(showLibraryHub) {
        if (showLibraryHub) {
            libraryAssetEditMode = false
            selectedLibraryAssetPaths = emptySet()
            showLibraryAssetDeleteConfirm = false
            showGalleryOverflowMenu = false
            pendingGalleryMenuAction = PendingGalleryMenuAction.None
        }
    }

    LaunchedEffect(libraryTab, aiCadLibraryVersion) {
        if (libraryTab == LibraryTab.AI_CAD) {
            aiCadStlFiles = withContext(Dispatchers.IO) { AiCadLibrary.listStlFiles(context) }
        } else {
            selectedAiCadStlFile = null
        }
    }

    // [추가] 데이터셋 탭에서 0장(빈) 폴더 주기적 자동 삭제/갱신
    // - 폴더 상세 화면에서는 폴더가 사라지면 UX가 깨질 수 있어 제외
    LaunchedEffect(libraryTab, libraryDetailScreen) {
        if (libraryTab != LibraryTab.DATASET) return@LaunchedEffect
        if (libraryDetailScreen == LibraryDetailScreen.DATASET_FOLDER) return@LaunchedEffect

        while (true) {
            val folders = withContext(Dispatchers.IO) { loadDatasetFoldersSync(context) }
            datasetFolders = folders
            delay(10_000) // 10초마다 정리/갱신
        }
    }

    // 앨범 허브에 표시할 최신 개수·표지용 데이터 선로드
    LaunchedEffect(showLibraryHub, aiCadLibraryVersion, images.size, serverArtifactLibraryVersion) {
        if (!showLibraryHub) return@LaunchedEffect
        loadDatasetFolders(context) { datasetFolders = it }
        loadModel3dLibrary(context) { lib ->
            plyLibraryModels = lib.plyModels
            objLibraryModels = lib.objModels
        }
        aiCadStlFiles = withContext(Dispatchers.IO) { AiCadLibrary.listStlFiles(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // 헤더 (허브: 제목만 / 구역: ‹ 뒤로 + 현재 구역명 + 취소)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (showLibraryHub) {
                Text(
                    text = "라이브러리",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onBackground
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        color = palette.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                when {
                                    libraryTab == LibraryTab.MODEL_3D &&
                                        libraryDetailScreen == LibraryDetailScreen.OBJ_VIEWER -> {
                                        val f = currentPlyModel?.file
                                        libraryDetailScreen =
                                            if (f?.extension?.equals("obj", ignoreCase = true) == true) {
                                                LibraryDetailScreen.MODEL_3D_OBJ_LIST
                                            } else {
                                                LibraryDetailScreen.MODEL_3D_PLY_LIST
                                            }
                                        currentPlyModel = null
                                    }
                                    libraryTab == LibraryTab.MODEL_3D && (
                                        libraryDetailScreen == LibraryDetailScreen.MODEL_3D_PLY_LIST ||
                                            libraryDetailScreen == LibraryDetailScreen.MODEL_3D_OBJ_LIST
                                        ) -> {
                                        libraryDetailScreen = LibraryDetailScreen.NONE
                                        libraryAssetEditMode = false
                                        selectedLibraryAssetPaths = emptySet()
                                    }
                                    else -> onLibraryHubVisibilityChange(true)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            libraryTab == LibraryTab.MODEL_3D &&
                                libraryDetailScreen == LibraryDetailScreen.OBJ_VIEWER ->
                                currentPlyModel?.file?.name ?: "3D 모델"
                            libraryTab == LibraryTab.MODEL_3D &&
                                libraryDetailScreen == LibraryDetailScreen.MODEL_3D_PLY_LIST ->
                                "PLY"
                            libraryTab == LibraryTab.MODEL_3D &&
                                libraryDetailScreen == LibraryDetailScreen.MODEL_3D_OBJ_LIST ->
                                "OBJ"
                            else -> when (libraryTab) {
                                LibraryTab.MODEL_3D -> "3D 모델"
                                LibraryTab.AI_CAD -> "AI CAD"
                                LibraryTab.DATASET -> "데이터셋폴더"
                                LibraryTab.GALLERY -> "갤러리"
                                LibraryTab.GS_PREVIEW -> "3DGS 미리보기"
                                LibraryTab.GS_ANALYSIS -> "3DGS 분석 이미지"
                                LibraryTab.JSON_LIBRARY -> "JSON"
                                LibraryTab.AR_CORE_LIBRARY -> "ARCore"
                            }
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    val showLibraryItemDeleteAction =
                        !showLibraryHub &&
                            selectedLibraryDeletePaths.isNotEmpty() &&
                            (libraryTab == LibraryTab.GS_PREVIEW ||
                                libraryTab == LibraryTab.GS_ANALYSIS ||
                                libraryTab == LibraryTab.JSON_LIBRARY ||
                                libraryTab == LibraryTab.AR_CORE_LIBRARY)
                    if (showLibraryItemDeleteAction) {
                        if (libraryTab == LibraryTab.AR_CORE_LIBRARY) {
                            IconButton(
                                onClick = {
                                    if (isUploading || isTransferring) {
                                        Toast.makeText(
                                            context,
                                            "이미 업로드 중입니다.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        return@IconButton
                                    }
                                    val zips = selectedLibraryDeletePaths.map { File(it) }
                                        .filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
                                    if (zips.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "서버 전송은 압축(ZIP) 파일만 가능합니다.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        return@IconButton
                                    }
                                    pending3DArcoreZipPaths = zips.map { f ->
                                        runCatching { f.canonicalPath }.getOrDefault(f.absolutePath)
                                    }
                                    pending3DSourceTab = LibraryTab.AR_CORE_LIBRARY
                                    modelingPromptText = ""
                                    modelingPromptError = false
                                    show3DModelingDialog = true
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = "서버로 전송",
                                    tint = palette.onBackground,
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteLibraryItemsConfirm = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "${selectedLibraryDeletePaths.size}개 삭제",
                                tint = Color(0xFFFF5252),
                            )
                        }
                    }
                    if (libraryTab == LibraryTab.GALLERY && !showLibraryHub &&
                        isEditMode &&
                        pendingGalleryMenuAction != PendingGalleryMenuAction.None
                    ) {
                        Text(
                            text = "확인",
                            color = Color(0xFF9CD83B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF9CD83B), RoundedCornerShape(12.dp))
                                .clickable {
                                    when (pendingGalleryMenuAction) {
                                        PendingGalleryMenuAction.BackgroundRemove -> {
                                            if (selectedItems.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "선택된 이미지가 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                bgRemovePrompt = ""
                                                bgRemovePromptError = false
                                                showBgRemoveDialog = true
                                            }
                                        }
                                        PendingGalleryMenuAction.GlareRemove -> {
                                            runGlareRemovalOnGallerySelection()
                                        }
                                        PendingGalleryMenuAction.CreateDatasetFolder -> {
                                            val imageUris = selectedItems.filter { !isVideoUri(context, it) }
                                            if (imageUris.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "폴더에 넣을 이미지를 선택해 주세요. (동영상은 제외됩니다.)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                pendingDatasetFolderImageUris = imageUris
                                                newDatasetFolderNameInput = ""
                                                showNewDatasetFolderNameDialog = true
                                            }
                                        }
                                        PendingGalleryMenuAction.Export -> {
                                            runExportGallerySelection()
                                        }
                                        PendingGalleryMenuAction.Share -> {
                                            runShareGallerySelection()
                                        }
                                        PendingGalleryMenuAction.None -> {}
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (libraryTab == LibraryTab.DATASET && !showLibraryHub &&
                        libraryDetailScreen == LibraryDetailScreen.NONE &&
                        isDatasetEditMode &&
                        pendingDatasetMenuAction != PendingDatasetMenuAction.None
                    ) {
                        Text(
                            text = "확인",
                            color = Color(0xFF9CD83B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF9CD83B), RoundedCornerShape(12.dp))
                                .clickable {
                                    when (pendingDatasetMenuAction) {
                                        PendingDatasetMenuAction.BackgroundRemove -> {
                                            if (selectedDatasetFolders.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "선택된 폴더가 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val uris = collectImageUrisFromDatasetFolderPaths(
                                                    selectedDatasetFolders
                                                ).filter { !isVideoUri(context, it) }
                                                if (uris.isEmpty()) {
                                                    Toast.makeText(
                                                        context,
                                                        "처리할 이미지가 없습니다.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    pendingBulkImageUrisForBgRemove = uris
                                                    pendingDatasetFolderPathsForBulkBgRemove =
                                                        selectedDatasetFolders.toSet()
                                                    bgRemovePrompt = ""
                                                    bgRemovePromptError = false
                                                    showBgRemoveDialog = true
                                                    pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                                }
                                            }
                                        }
                                        PendingDatasetMenuAction.GlareRemove -> {
                                            if (selectedDatasetFolders.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "선택된 폴더가 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val uris = collectImageUrisFromDatasetFolderPaths(
                                                    selectedDatasetFolders
                                                ).filter { !isVideoUri(context, it) }
                                                if (uris.isEmpty()) {
                                                    Toast.makeText(
                                                        context,
                                                        "처리할 이미지가 없습니다.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    runGlareRemovalOnGallerySelection(
                                                        urisOverride = uris,
                                                        datasetSourceFolderPaths = selectedDatasetFolders.toSet()
                                                    )
                                                    pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                                }
                                            }
                                        }
                                        PendingDatasetMenuAction.Share -> {
                                            if (selectedDatasetFolders.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "선택된 폴더가 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val uris = collectImageUrisFromDatasetFolderPaths(
                                                    selectedDatasetFolders
                                                ).filter { !isVideoUri(context, it) }
                                                if (uris.isEmpty()) {
                                                    Toast.makeText(
                                                        context,
                                                        "공유할 이미지가 없습니다.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    runShareGallerySelection(uris)
                                                    isDatasetEditMode = false
                                                    selectedDatasetFolders = emptySet()
                                                    pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                                }
                                            }
                                        }
                                        PendingDatasetMenuAction.None -> {}
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (isEditMode || isDatasetEditMode || libraryAssetEditMode) {
                        Text(
                            text = "취소",
                            color = palette.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, palette.onBackground, RoundedCornerShape(12.dp))
                                .clickable {
                                    when {
                                        libraryTab == LibraryTab.DATASET && isDatasetEditMode -> {
                                            isDatasetEditMode = false
                                            selectedDatasetFolders = emptySet()
                                            pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                        }
                                        libraryAssetEditMode -> {
                                            libraryAssetEditMode = false
                                            selectedLibraryAssetPaths = emptySet()
                                        }
                                        else -> {
                                            isEditMode = false
                                            selectedItems = emptySet()
                                            pendingGalleryMenuAction = PendingGalleryMenuAction.None
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else if (
                        libraryTab == LibraryTab.MODEL_3D &&
                        !showLibraryHub &&
                        libraryDetailScreen == LibraryDetailScreen.OBJ_VIEWER &&
                        currentPlyModel != null
                    ) {
                        val plyF = currentPlyModel!!.file
                        if (!objViewerConverting && objViewerError == null && objViewerObjFile != null &&
                            plyF.extension.equals("ply", ignoreCase = true)
                        ) {
                            Text(
                                text = if (objViewerSaving) "저장 중…" else "저장",
                                color = if (objViewerSaving) palette.onBackground.copy(alpha = 0.5f)
                                else Color(0xFF9CD83B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !objViewerSaving) {
                                        val cached = objViewerObjFile ?: return@clickable
                                        objViewerSaving = true
                                        transferScope.launch {
                                            val err = withContext(Dispatchers.IO) {
                                                saveConvertedObjToModelsLibrary(context, plyF, cached)
                                            }
                                            objViewerSaving = false
                                            if (err != null) {
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            } else {
                                                Model3dThumbnail.invalidateForModelFile(context, plyF)
                                                Model3dThumbnail.invalidateForModelFile(
                                                    context,
                                                    File(
                                                        ModelLibraryPaths.objDir(context),
                                                        "${plyF.nameWithoutExtension}.obj"
                                                    )
                                                )
                                                libraryModelThumbRefresh++
                                                Toast.makeText(
                                                    context,
                                                    "OBJ가 라이브러리에 저장되었습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                loadModel3dLibrary(context) { lib ->
                                                    plyLibraryModels = lib.plyModels
                                                    objLibraryModels = lib.objModels
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = {
                                val f = currentPlyModel?.file
                                libraryDetailScreen =
                                    if (f?.extension?.equals("obj", ignoreCase = true) == true) {
                                        LibraryDetailScreen.MODEL_3D_OBJ_LIST
                                    } else {
                                        LibraryDetailScreen.MODEL_3D_PLY_LIST
                                    }
                                currentPlyModel = null
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "닫기",
                                tint = palette.onBackground
                            )
                        }
                    } else if (libraryTab == LibraryTab.GALLERY && !showLibraryHub) {
                        Box {
                            IconButton(
                                onClick = { showGalleryOverflowMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "갤러리 메뉴",
                                    tint = palette.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = showGalleryOverflowMenu,
                                onDismissRequest = { showGalleryOverflowMenu = false },
                                modifier = Modifier
                                    .widthIn(min = 220.dp)
                                    .background(palette.surfaceCard, RoundedCornerShape(14.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "1차 배경 제거",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        isEditMode = true
                                        selectedItems = emptySet()
                                        pendingGalleryMenuAction = PendingGalleryMenuAction.BackgroundRemove
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "광택 제거",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        isEditMode = true
                                        selectedItems = emptySet()
                                        pendingGalleryMenuAction = PendingGalleryMenuAction.GlareRemove
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "폴더 만들기",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        if (!isUploading && !isTransferring && !isGlareRemoving) {
                                            isEditMode = true
                                            selectedItems = emptySet()
                                            pendingGalleryMenuAction =
                                                PendingGalleryMenuAction.CreateDatasetFolder
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "다른 작업이 진행 중입니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "가져오기",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        if (!isUploading && !isTransferring) {
                                            importLauncher.launch("image/*")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "내보내기",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        isEditMode = true
                                        selectedItems = emptySet()
                                        pendingGalleryMenuAction = PendingGalleryMenuAction.Export
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "공유하기",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        isEditMode = true
                                        selectedItems = emptySet()
                                        pendingGalleryMenuAction = PendingGalleryMenuAction.Share
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "전체선택",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        if (images.isNotEmpty()) {
                                            isEditMode = true
                                            selectedItems = images.toSet()
                                            pendingGalleryMenuAction = PendingGalleryMenuAction.None
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "선택할 미디어가 없습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "전체삭제",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showGalleryOverflowMenu = false
                                        if (!isUploading && !isTransferring) {
                                            showDeleteAllConfirm = true
                                        }
                                    }
                                )
                            }
                        }
                    } else if (
                        libraryTab == LibraryTab.DATASET &&
                        !showLibraryHub &&
                        libraryDetailScreen == LibraryDetailScreen.NONE
                    ) {
                        Box {
                            IconButton(
                                onClick = { showDatasetOverflowMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "데이터셋폴더 메뉴",
                                    tint = palette.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = showDatasetOverflowMenu,
                                onDismissRequest = { showDatasetOverflowMenu = false },
                                modifier = Modifier
                                    .widthIn(min = 220.dp)
                                    .background(palette.surfaceCard, RoundedCornerShape(14.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "1차 배경 제거",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showDatasetOverflowMenu = false
                                        if (!isUploading && !isTransferring && !isGlareRemoving && !isBgRemoving) {
                                            isDatasetEditMode = true
                                            selectedDatasetFolders = emptySet()
                                            pendingDatasetMenuAction =
                                                PendingDatasetMenuAction.BackgroundRemove
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "다른 작업이 진행 중입니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "광택 제거",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showDatasetOverflowMenu = false
                                        if (!isUploading && !isTransferring && !isGlareRemoving && !isBgRemoving) {
                                            isDatasetEditMode = true
                                            selectedDatasetFolders = emptySet()
                                            pendingDatasetMenuAction =
                                                PendingDatasetMenuAction.GlareRemove
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "다른 작업이 진행 중입니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "공유하기",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showDatasetOverflowMenu = false
                                        if (!isUploading && !isTransferring) {
                                            isDatasetEditMode = true
                                            selectedDatasetFolders = emptySet()
                                            pendingDatasetMenuAction = PendingDatasetMenuAction.Share
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "다른 작업이 진행 중입니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "전체선택",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showDatasetOverflowMenu = false
                                        if (datasetFolders.isNotEmpty()) {
                                            isDatasetEditMode = true
                                            selectedDatasetFolders =
                                                datasetFolders.map { it.dir.absolutePath }.toSet()
                                            pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "선택할 폴더가 없습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "전체삭제",
                                            color = palette.onBackground,
                                            fontSize = 15.sp
                                        )
                                    },
                                    onClick = {
                                        showDatasetOverflowMenu = false
                                        if (!isUploading && !isTransferring) {
                                            showDatasetDeleteAllConfirm = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showLibraryHub) {
                LibraryAlbumHubGrid(
                    images = images,
                    datasetFolders = datasetFolders,
                    model3dTotalCount = allModel3dLibrary.size,
                    model3dCoverUri = libraryHubModel3dCoverUri,
                    aiCadStlFiles = aiCadStlFiles,
                    gsPreviewCount = gsPreviewCount,
                    gsAnalysisCount = gsAnalysisCount,
                    gsPreviewCoverUri = gsPreviewCoverUri,
                    gsAnalysisCoverUri = gsAnalysisCoverUri,
                    jsonLibraryCount = jsonLibraryFiles.size,
                    arcoreLibraryCount = arcoreLibraryFiles.size,
                    onOpenSection = { tab ->
                        onLibraryTabChange(tab)
                        onLibraryHubVisibilityChange(false)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (libraryTab == LibraryTab.MODEL_3D) {
                if (libraryDetailScreen == LibraryDetailScreen.OBJ_VIEWER && currentPlyModel != null) {
                    BackHandler {
                        val f = currentPlyModel?.file
                        libraryDetailScreen =
                            if (f?.extension?.equals("obj", ignoreCase = true) == true) {
                                LibraryDetailScreen.MODEL_3D_OBJ_LIST
                            } else {
                                LibraryDetailScreen.MODEL_3D_PLY_LIST
                            }
                        currentPlyModel = null
                    }
                    val viewerSourceFile = currentPlyModel!!.file

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            objViewerConverting -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (viewerSourceFile.extension.equals("obj", ignoreCase = true)) {
                                            "모델 불러오는 중..."
                                        } else {
                                            "OBJ 변환 중..."
                                        },
                                        color = palette.onBackground,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            objViewerError != null -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = objViewerError ?: "오류",
                                        color = palette.onBackground,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            objViewerObjFile != null -> {
                                key(objViewerObjFile!!.absolutePath) {
                                    AndroidView(
                                        factory = { ctx ->
                                            ObjSurfaceView(ctx).apply {
                                                val mesh = objViewerPreviewMesh
                                                if (mesh != null) {
                                                    applyParsedMesh(mesh)
                                                } else {
                                                    loadModel(objViewerObjFile!!)
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val listModels = when (libraryDetailScreen) {
                        LibraryDetailScreen.MODEL_3D_PLY_LIST -> plyLibraryModels
                        LibraryDetailScreen.MODEL_3D_OBJ_LIST -> objLibraryModels
                        else -> emptyList()
                    }
                    if (libraryDetailScreen == LibraryDetailScreen.MODEL_3D_PLY_LIST ||
                        libraryDetailScreen == LibraryDetailScreen.MODEL_3D_OBJ_LIST
                    ) {
                        BackHandler {
                            libraryDetailScreen = LibraryDetailScreen.NONE
                            libraryAssetEditMode = false
                            selectedLibraryAssetPaths = emptySet()
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (libraryAssetEditMode &&
                            (libraryDetailScreen == LibraryDetailScreen.MODEL_3D_PLY_LIST ||
                                libraryDetailScreen == LibraryDetailScreen.MODEL_3D_OBJ_LIST)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "공유하기",
                                    tint = if (selectedLibraryAssetPaths.isNotEmpty()) Color(0xFF9CD83B)
                                    else Color(0xFF9CD83B).copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable(enabled = selectedLibraryAssetPaths.isNotEmpty()) {
                                            val files = listModels
                                                .filter { selectedLibraryAssetPaths.contains(it.file.absolutePath) }
                                                .map { it.file }
                                            shareLibraryFiles(context, files)
                                        }
                                )
                                Text(
                                    text = "삭제",
                                    color = if (selectedLibraryAssetPaths.isNotEmpty()) palette.onBackground
                                    else palette.onBackground.copy(alpha = 0.4f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedLibraryAssetPaths.isNotEmpty()) Color.Red
                                            else Color.Red.copy(alpha = 0.4f)
                                        )
                                        .clickable(enabled = selectedLibraryAssetPaths.isNotEmpty()) {
                                            showLibraryAssetDeleteConfirm = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                                Text(
                                    text = "전체 선택",
                                    color = palette.onBackground,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF3A3A3A))
                                        .clickable {
                                            selectedLibraryAssetPaths =
                                                listModels.map { it.file.absolutePath }.toSet()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                        when (libraryDetailScreen) {
                            LibraryDetailScreen.NONE -> {
                                if (plyLibraryModels.isEmpty() && objLibraryModels.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Outlined.Public,
                                                contentDescription = null,
                                                tint = palette.onBackground.copy(alpha = 0.42f),
                                                modifier = Modifier.size(52.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "3D 모델이 아직 없습니다",
                                                color = palette.onBackground.copy(alpha = 0.7f),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Model3dFormatHubGrid(
                                        plyCount = plyLibraryModels.size,
                                        objCount = objLibraryModels.size,
                                        plyCoverUri = plySubHubCoverUri,
                                        objCoverUri = objSubHubCoverUri,
                                        onOpenPly = {
                                            libraryDetailScreen = LibraryDetailScreen.MODEL_3D_PLY_LIST
                                        },
                                        onOpenObj = {
                                            libraryDetailScreen = LibraryDetailScreen.MODEL_3D_OBJ_LIST
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            LibraryDetailScreen.MODEL_3D_PLY_LIST,
                            LibraryDetailScreen.MODEL_3D_OBJ_LIST -> {
                                if (listModels.isEmpty()) {
                                    val emptyMsg = if (libraryDetailScreen == LibraryDetailScreen.MODEL_3D_PLY_LIST) {
                                        "PLY 파일이 없습니다"
                                    } else {
                                        "OBJ 파일이 없습니다"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Outlined.Public,
                                                contentDescription = null,
                                                tint = palette.onBackground.copy(alpha = 0.42f),
                                                modifier = Modifier.size(52.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = emptyMsg,
                                                color = palette.onBackground.copy(alpha = 0.7f),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    val modelDayGroups = remember(listModels) {
                                        groupByDayConsecutiveDescending(
                                            listModels.sortedByDescending { it.lastModified }
                                        ) { it.lastModified }
                                    }
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        modelDayGroups.forEach { (dayStart, modelsInDay) ->
                                            item(
                                                span = { GridItemSpan(this.maxLineSpan) },
                                                key = "m3d_day_$dayStart"
                                            ) {
                                                Text(
                                                    text = formatKoreanDateHeader(dayStart),
                                                    color = palette.onBackground,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                                )
                                            }
                                            items(
                                                items = modelsInDay,
                                                key = { it.file.absolutePath }
                                            ) { model ->
                                                val path = model.file.absolutePath
                                                val isSelected = selectedLibraryAssetPaths.contains(path)
                                                Model3dLibraryGridItem(
                                                    model = model,
                                                    thumbRefresh = libraryModelThumbRefresh,
                                                    isSelected = isSelected,
                                                    onClick = {
                                                        if (libraryAssetEditMode) {
                                                            selectedLibraryAssetPaths =
                                                                if (isSelected) selectedLibraryAssetPaths - path
                                                                else selectedLibraryAssetPaths + path
                                                        } else {
                                                            currentPlyModel = model
                                                            libraryDetailScreen = LibraryDetailScreen.OBJ_VIEWER
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!libraryAssetEditMode) {
                                                            libraryAssetEditMode = true
                                                            selectedLibraryAssetPaths = setOf(path)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else if (libraryTab == LibraryTab.AI_CAD) {
            if (selectedAiCadStlFile != null) {
                val stlFile = selectedAiCadStlFile!!
                var viewerLoading by remember(stlFile.absolutePath) { mutableStateOf(true) }
                BackHandler { selectedAiCadStlFile = null }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D1A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111111))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedAiCadStlFile = null }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "목록으로",
                                tint = palette.onBackground
                            )
                        }
                        Text(
                            text = stlFile.name,
                            color = palette.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }
                    val glbPreviewFile = File(stlFile.parent ?: "", "${stlFile.nameWithoutExtension}.glb")
                    Text(
                        text = buildString {
                            append(stlFile.length())
                            append(" bytes · ")
                            if (glbPreviewFile.isFile && glbPreviewFile.length() > 0L) {
                                append("GLB·OBJ 동봉 · STL 미리보기")
                            } else {
                                append("STL 3D 미리보기")
                            }
                        },
                        fontSize = 11.sp,
                        color = palette.onBackground.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // GLB(Filament) 우선 시 일부 기기에서 빈 화면이 되는 사례가 있어,
                        // 저장된 STL과 동일한 OpenGL 경로로만 미리보기(렌더 일치 보장).
                        key(stlFile.absolutePath) {
                            AndroidView(
                                factory = { ctx ->
                                    ObjSurfaceView(ctx).apply {
                                        loadMeshFile(stlFile) { viewerLoading = false }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (viewerLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0D0D1A).copy(alpha = 0.82f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = palette.brand,
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "3D 모델 로딩 중…",
                                        color = palette.onBackground,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (aiCadStlFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = palette.onBackground.copy(alpha = 0.42f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "저장된 AI CAD 모델이 없습니다",
                            color = palette.onBackground.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI 탭 → AI CAD 모드에서 OpenSCAD 스크립트가 오면\n자동으로 STL이 저장됩니다. 코드 창 상단 「저장」에서 이름을 정해 다시 저장할 수 있습니다.",
                            color = palette.onBackground.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (libraryAssetEditMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "공유하기",
                                tint = if (selectedLibraryAssetPaths.isNotEmpty()) Color(0xFF9CD83B)
                                else Color(0xFF9CD83B).copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(enabled = selectedLibraryAssetPaths.isNotEmpty()) {
                                        val files = aiCadStlFiles
                                            .filter { selectedLibraryAssetPaths.contains(it.absolutePath) }
                                        shareLibraryFiles(context, files)
                                    }
                            )
                            Text(
                                text = "삭제",
                                color = if (selectedLibraryAssetPaths.isNotEmpty()) palette.onBackground
                                else palette.onBackground.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedLibraryAssetPaths.isNotEmpty()) Color.Red
                                        else Color.Red.copy(alpha = 0.4f)
                                    )
                                    .clickable(enabled = selectedLibraryAssetPaths.isNotEmpty()) {
                                        showLibraryAssetDeleteConfirm = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "전체 선택",
                                color = palette.onBackground,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF3A3A3A))
                                    .clickable {
                                        selectedLibraryAssetPaths =
                                            aiCadStlFiles.map { it.absolutePath }.toSet()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                    val aiCadDayGroups = remember(aiCadStlFiles) {
                        groupByDayConsecutiveDescending(
                            aiCadStlFiles.sortedByDescending { it.lastModified() }
                        ) { it.lastModified() }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aiCadDayGroups.forEach { (dayStart, filesInDay) ->
                            item(
                                span = { GridItemSpan(this.maxLineSpan) },
                                key = "aicad_day_$dayStart"
                            ) {
                                Text(
                                    text = formatKoreanDateHeader(dayStart),
                                    color = palette.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                            }
                            items(
                                items = filesInDay,
                                key = { it.absolutePath }
                            ) { file ->
                                val path = file.absolutePath
                                val isSelected = selectedLibraryAssetPaths.contains(path)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (libraryAssetEditMode) {
                                                    selectedLibraryAssetPaths =
                                                        if (isSelected) selectedLibraryAssetPaths - path
                                                        else selectedLibraryAssetPaths + path
                                                } else {
                                                    selectedAiCadStlFile = file
                                                }
                                            },
                                            onLongClick = {
                                                if (!libraryAssetEditMode) {
                                                    libraryAssetEditMode = true
                                                    selectedLibraryAssetPaths = setOf(path)
                                                }
                                            }
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (palette.isDark) Color(0xFF1A1A1A) else Color.White)
                                            .border(
                                                width = if (isSelected) 4.dp else 1.dp,
                                                color = when {
                                                    isSelected -> Color.Blue
                                                    palette.isDark -> palette.onBackground.copy(alpha = 0.2f)
                                                    else -> Color.Black
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AiCadStlGridThumbnail(
                                            stlFile = file,
                                            libraryVersion = aiCadLibraryVersion,
                                            thumbEdgePx = gridThumbPx,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .size(28.dp)
                                                    .background(Color(0xFF7ED321), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "선택됨",
                                                    tint = palette.onBackground,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = file.nameWithoutExtension,
                                        color = palette.onBackground,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (libraryTab == LibraryTab.DATASET) {
            if (libraryDetailScreen == LibraryDetailScreen.DATASET_FOLDER && currentDatasetFolder != null) {
                BackHandler {
                    libraryDetailScreen = LibraryDetailScreen.NONE
                    currentDatasetFolder = null
                }
                if (datasetImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "데이터셋폴더에 이미지가 없습니다",
                            color = palette.onBackground.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val datasetImageDayGroups = remember(datasetImages) {
                        groupImagesByDayInOrder(context, datasetImages)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        datasetImageDayGroups.forEach { (dayStart, uris) ->
                            item(
                                span = { GridItemSpan(this.maxLineSpan) },
                                key = "ds_img_day_$dayStart"
                            ) {
                                Text(
                                    text = formatKoreanDateHeader(dayStart),
                                    color = palette.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                            }
                            items(
                                items = uris,
                                key = { it.toString() }
                            ) { uri ->
                                Image(
                                    painter = rememberGalleryGridPhotoPainter(uri, gridThumbPx),
                                    contentDescription = "데이터셋폴더 이미지",
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onMediaSelected(uri, datasetImages) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            } else
            if (datasetFolders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = palette.onBackground.copy(alpha = 0.42f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "데이터셋폴더가 아직 없습니다",
                            color = palette.onBackground.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Box 안에서 Row와 LazyVerticalGrid를 형제로 두면 그리드가 fillMaxSize로 메뉴 위에 그려짐 → Column으로 분리
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isDatasetEditMode && pendingDatasetMenuAction != PendingDatasetMenuAction.None) {
                        Text(
                            text = "폴더를 탭하여 선택한 뒤 상단 「확인」을 누르세요.",
                            color = palette.onBackground.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    // 메뉴(배경제거·광택·공유 등)로 진입한 폴더 선택 모드에서는 업로드/삭제 바 숨김 — 길게 눌러 진입한 편집 모드에서만 표시
                    // 배경 제거 프롬프트 다이얼로그가 뜨는 동안에는 pendingMenu가 None으로 바뀌므로, 대기 URI·다이얼로그 표시로 별도 판별
                    if (isDatasetEditMode &&
                        pendingDatasetMenuAction == PendingDatasetMenuAction.None &&
                        !showBgRemoveDialog &&
                        pendingBulkImageUrisForBgRemove == null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "선택 ${selectedDatasetFolders.size}",
                                color = palette.onBackground.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = "업로드",
                                    tint = if (selectedDatasetFolders.isNotEmpty()) palette.onBackground else palette.onBackground.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            uploadSourceTab = LibraryTab.DATASET
                                            when {
                                                isUploading -> {
                                                    uploadMessage = "이미 업로드 중입니다"
                                                }
                                                selectedDatasetFolders.isEmpty() -> {
                                                    uploadMessage = "선택된 폴더가 없습니다"
                                                }
                                                else -> {
                                                    // 3D 모델링 프롬프트 팝업을 먼저 표시
                                                    pending3DSourceTab = LibraryTab.DATASET
                                                    pending3DDatasetFolders = selectedDatasetFolders.toList()
                                                    pending3DArcoreZipUriForDataset = null
                                                    modelingPromptText = ""
                                                    modelingPromptError = false
                                                    show3DModelingDialog = true
                                                }
                                            }
                                        }
                                )
                                Text(
                                    text = "삭제",
                                    color = if (selectedDatasetFolders.isNotEmpty()) Color.White else palette.onBackground.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedDatasetFolders.isNotEmpty()) Color.Red else Color.Red.copy(alpha = 0.4f))
                                        .clickable(enabled = selectedDatasetFolders.isNotEmpty()) {
                                            showDatasetDeleteConfirm = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    val datasetFolderDayGroups = remember(datasetFolders) {
                        groupByDayConsecutiveDescending(
                            datasetFolders.sortedByDescending { it.dir.lastModified() }
                        ) { it.dir.lastModified() }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        datasetFolderDayGroups.forEach { (dayStart, foldersInDay) ->
                            item(
                                span = { GridItemSpan(this.maxLineSpan) },
                                key = "ds_fold_day_$dayStart"
                            ) {
                                Text(
                                    text = formatKoreanDateHeader(dayStart),
                                    color = palette.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                            }
                            items(
                                items = foldersInDay,
                                key = { it.dir.absolutePath }
                            ) { folder ->
                        val folderPath = folder.dir.absolutePath
                        val isSelected = selectedDatasetFolders.contains(folderPath)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (palette.isDark) Color(0xFF1A1A1A) else Color.White)
                                    .border(
                                        1.dp,
                                        if (palette.isDark) palette.onBackground.copy(alpha = 0.6f) else Color.Black,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (isDatasetEditMode) {
                                                selectedDatasetFolders = if (isSelected) {
                                                    selectedDatasetFolders - folderPath
                                                } else {
                                                    selectedDatasetFolders + folderPath
                                                }
                                            } else {
                                                currentDatasetFolder = folder
                                                loadDatasetImages(folder.dir) { images ->
                                                    datasetImages = images
                                                    libraryDetailScreen = LibraryDetailScreen.DATASET_FOLDER
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!isDatasetEditMode) {
                                                isDatasetEditMode = true
                                            }
                                            selectedDatasetFolders = if (isSelected) {
                                                selectedDatasetFolders - folderPath
                                            } else {
                                                selectedDatasetFolders + folderPath
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                folder.coverUri?.let { uri ->
                                    Image(
                                        painter = rememberGalleryGridPhotoPainter(uri, gridThumbPx),
                                        contentDescription = "데이터셋폴더 표지",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "선택됨",
                                        tint = palette.onBackground,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = folder.name,
                                color = palette.onBackground,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Text(
                                text = "${folder.count}장",
                                color = palette.onBackground.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    }
                }
                }
            }
        } else if (libraryTab == LibraryTab.GS_PREVIEW) {
            if (gsPreviewUris.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "저장된 3DGS 미리보기 이미지가 없습니다.",
                        color = palette.onBackground.copy(alpha = 0.65f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(gsPreviewUris, key = { _, uri -> uri.toString() }) { index, uri ->
                        val filePath = remember(uri) {
                            if (uri.scheme == "file") {
                                uri.path?.let { p ->
                                    runCatching { File(p).canonicalPath }.getOrNull() ?: p
                                }
                            } else {
                                null
                            }
                        }
                        val delHighlight =
                            filePath != null && filePath in selectedLibraryDeletePaths
                        Image(
                            painter = rememberGalleryGridPhotoPainter(uri, gridThumbPx),
                            contentDescription = "3DGS 미리보기",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (delHighlight) {
                                        Modifier.border(
                                            2.dp,
                                            Color(0xFFFF5252),
                                            RoundedCornerShape(8.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (selectedLibraryDeletePaths.isNotEmpty()) {
                                            filePath?.let { p ->
                                                selectedLibraryDeletePaths =
                                                    if (p in selectedLibraryDeletePaths) {
                                                        selectedLibraryDeletePaths - p
                                                    } else {
                                                        selectedLibraryDeletePaths + p
                                                    }
                                            }
                                        } else {
                                            onServerPipelineOpenImageViewer(gsPreviewUris, index)
                                        }
                                    },
                                    onLongClick = {
                                        filePath?.let { p ->
                                            selectedLibraryDeletePaths =
                                                if (p in selectedLibraryDeletePaths) {
                                                    selectedLibraryDeletePaths - p
                                                } else {
                                                    selectedLibraryDeletePaths + p
                                                }
                                        }
                                    },
                                ),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        } else if (libraryTab == LibraryTab.GS_ANALYSIS) {
            if (gsAnalysisUris.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "저장된 3DGS 분석 이미지가 없습니다.",
                        color = palette.onBackground.copy(alpha = 0.65f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(gsAnalysisUris, key = { _, uri -> uri.toString() }) { index, uri ->
                        val filePath = remember(uri) {
                            if (uri.scheme == "file") {
                                uri.path?.let { p ->
                                    runCatching { File(p).canonicalPath }.getOrNull() ?: p
                                }
                            } else {
                                null
                            }
                        }
                        val delHighlight =
                            filePath != null && filePath in selectedLibraryDeletePaths
                        Image(
                            painter = rememberGalleryGridPhotoPainter(uri, gridThumbPx),
                            contentDescription = "3DGS 분석 이미지",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (delHighlight) {
                                        Modifier.border(
                                            2.dp,
                                            Color(0xFFFF5252),
                                            RoundedCornerShape(8.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (selectedLibraryDeletePaths.isNotEmpty()) {
                                            filePath?.let { p ->
                                                selectedLibraryDeletePaths =
                                                    if (p in selectedLibraryDeletePaths) {
                                                        selectedLibraryDeletePaths - p
                                                    } else {
                                                        selectedLibraryDeletePaths + p
                                                    }
                                            }
                                        } else {
                                            onServerPipelineOpenImageViewer(gsAnalysisUris, index)
                                        }
                                    },
                                    onLongClick = {
                                        filePath?.let { p ->
                                            selectedLibraryDeletePaths =
                                                if (p in selectedLibraryDeletePaths) {
                                                    selectedLibraryDeletePaths - p
                                                } else {
                                                    selectedLibraryDeletePaths + p
                                                }
                                        }
                                    },
                                ),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        } else if (libraryTab == LibraryTab.JSON_LIBRARY) {
            if (jsonLibraryFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "저장된 JSON 파일이 없습니다.\n서버 파이프라인 완료 시 분석 JSON이 여기에 저장됩니다.",
                        color = palette.onBackground.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(jsonLibraryFiles, key = { it.absolutePath }) { file ->
                        val fPath = remember(file.absolutePath, file.lastModified()) {
                            runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
                        }
                        val delHighlight = fPath in selectedLibraryDeletePaths
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, palette.onBackground.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                .then(
                                    if (delHighlight) {
                                        Modifier.border(2.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .background(palette.surfaceCard.copy(alpha = 0.35f))
                                .combinedClickable(
                                    onClick = {
                                        if (selectedLibraryDeletePaths.isNotEmpty()) {
                                            selectedLibraryDeletePaths =
                                                if (fPath in selectedLibraryDeletePaths) {
                                                    selectedLibraryDeletePaths - fPath
                                                } else {
                                                    selectedLibraryDeletePaths + fPath
                                                }
                                        } else {
                                            jsonLibraryDetailFile = file
                                        }
                                    },
                                    onLongClick = {
                                        selectedLibraryDeletePaths =
                                            if (fPath in selectedLibraryDeletePaths) {
                                                selectedLibraryDeletePaths - fPath
                                            } else {
                                                selectedLibraryDeletePaths + fPath
                                            }
                                    },
                                )
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = palette.brand,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = file.name,
                                color = palette.onBackground,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "${file.length() / 1024} KB",
                                color = palette.onBackgroundMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (libraryTab == LibraryTab.AR_CORE_LIBRARY) {
            val arcoreDir = remember(context) { ArcoreLibrary.dir(context) }
            val arcoreHint =
                "파일을 PC에서 연결하거나 파일 관리자로 다음 폴더에 복사할 수 있습니다.\n${arcoreDir.absolutePath}"
            if (arcoreLibraryFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ARCore용 파일이 없습니다.\n\n$arcoreHint",
                        color = palette.onBackground.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(arcoreLibraryFiles, key = { it.absolutePath }) { file ->
                        val isZip = file.name.endsWith(".zip", ignoreCase = true)
                        var archiveCounts by remember(file.absolutePath, file.lastModified()) {
                            mutableStateOf<Pair<Int, Int>?>(null)
                        }
                        LaunchedEffect(file.absolutePath, file.lastModified(), isZip) {
                            archiveCounts =
                                if (isZip) {
                                    withContext(Dispatchers.IO) {
                                        ArcoreLibrary.readArchiveSummaryCounts(file)
                                    }
                                } else {
                                    null
                                }
                        }
                        val fPath = remember(file.absolutePath, file.lastModified()) {
                            runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
                        }
                        val delHighlight = fPath in selectedLibraryDeletePaths
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, palette.onBackground.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                .then(
                                    if (delHighlight) {
                                        Modifier.border(2.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .background(palette.surfaceCard.copy(alpha = 0.35f))
                                .combinedClickable(
                                    onClick = {
                                        if (selectedLibraryDeletePaths.isNotEmpty()) {
                                            selectedLibraryDeletePaths =
                                                if (fPath in selectedLibraryDeletePaths) {
                                                    selectedLibraryDeletePaths - fPath
                                                } else {
                                                    selectedLibraryDeletePaths + fPath
                                                }
                                        } else {
                                            arcoreLibraryDetailFile = file
                                        }
                                    },
                                    onLongClick = {
                                        selectedLibraryDeletePaths =
                                            if (fPath in selectedLibraryDeletePaths) {
                                                selectedLibraryDeletePaths - fPath
                                            } else {
                                                selectedLibraryDeletePaths + fPath
                                            }
                                    },
                                )
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ViewInAr,
                                contentDescription = null,
                                tint = palette.brand,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = file.name,
                                color = palette.onBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "${file.length() / 1024} KB",
                                color = palette.onBackgroundMuted,
                                fontSize = 10.sp
                            )
                            archiveCounts?.let { (imageCount, jsonCount) ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "이미지 ${imageCount}장",
                                    color = palette.onBackgroundMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp,
                                )
                                Text(
                                    text = "JSON ${jsonCount}개",
                                    color = palette.onBackgroundMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        } else if (libraryTab == LibraryTab.GALLERY) {
            Column(modifier = Modifier.fillMaxSize()) {
            if (isEditMode && pendingGalleryMenuAction != PendingGalleryMenuAction.None) {
                Text(
                    text = if (pendingGalleryMenuAction == PendingGalleryMenuAction.CreateDatasetFolder) {
                        "폴더에 넣을 이미지를 선택한 뒤 상단 「확인」을 누르세요."
                    } else {
                        "이미지를 탭하여 선택한 뒤 상단 「확인」을 누르세요."
                    },
                    color = palette.onBackground.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            // 길게 눌러 진입한 편집 모드에서만 서버 전송·삭제 툴바 표시 (메뉴로 진입한 선택 화면과 분리)
            if (isEditMode && pendingGalleryMenuAction == PendingGalleryMenuAction.None) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 업로드 버튼 (데이터셋 라이브러리 편집 툴바와 동일 터치 영역)
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "업로드",
                        tint = if (selectedItems.isNotEmpty()) palette.onBackground else palette.onBackground.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                uploadSourceTab = LibraryTab.GALLERY
                                when {
                                    isUploading || isTransferring -> {
                                        uploadMessage = "이미 업로드 중입니다"
                                    }
                                    selectedItems.isEmpty() -> {
                                        uploadMessage = "선택된 미디어가 없습니다"
                                    }
                                    else -> {
                                        // 3D 모델링 프롬프트 팝업을 먼저 표시
                                        pending3DSourceTab = LibraryTab.GALLERY
                                        pending3DGalleryUris = selectedItems.toList()
                                        pending3DArcoreZipUriForDataset = null
                                        modelingPromptText = ""
                                        modelingPromptError = false
                                        show3DModelingDialog = true
                                    }
                                }
                            }
                    )

                    // 선택 삭제 버튼
                    Text(
                        text = "삭제",
                        color = if (selectedItems.isNotEmpty()) Color.White else palette.onBackground.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedItems.isNotEmpty()) Color.Red else Color.Red.copy(alpha = 0.4f))
                            .clickable(enabled = !isUploading && !isTransferring && selectedItems.isNotEmpty()) {
                                showDeleteConfirm = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }



            // 미디어 그리드 (편집 메뉴 Row와 세로로 나열 — Box 형제 배치 시 그리드가 메뉴를 덮음)
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = palette.onBackground.copy(alpha = 0.42f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "촬영한 미디어가 없습니다",
                            fontSize = 16.sp,
                            color = palette.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val galleryDayGroups = remember(images) {
                    groupImagesByDayInOrder(context, images)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = galleryGridState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    galleryDayGroups.forEach { (dayStart, uris) ->
                        item(
                            span = { GridItemSpan(this.maxLineSpan) },
                            key = "gal_day_$dayStart"
                        ) {
                            Text(
                                text = formatKoreanDateHeader(dayStart),
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                        items(
                            items = uris,
                            key = { it.toString() }
                        ) { mediaUri ->
                        val isVideo = isVideoUri(context, mediaUri)
                        val isSelected = selectedItems.contains(mediaUri)
                        var videoThumbnail by remember(mediaUri) { mutableStateOf<Bitmap?>(null) }

                        // 동영상 썸네일 로드
                        if (isVideo) {
                            LaunchedEffect(mediaUri, gridThumbPx) {
                                videoThumbnail = decodeVideoGridThumbnail(context, mediaUri, gridThumbPx)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 4.dp else 0.dp,
                                    color = Color.Blue,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .pointerInput(mediaUri, isEditMode, isSelected) {
                                    detectTapGestures(
                                        onTap = {
                                            if (isEditMode) {
                                                // 편집 모드: 이미지 선택/해제 토글
                                                // 이미 선택된 이미지를 다시 누르면 선택 해제
                                                // 선택되지 않은 이미지를 누르면 선택
                                                selectedItems = if (isSelected) {
                                                    // 선택 해제: selectedItems에서 해당 mediaUri 제거
                                                    selectedItems - mediaUri
                                                } else {
                                                    // 선택: selectedItems에 해당 mediaUri 추가
                                                    selectedItems + mediaUri
                                                }
                                            } else {
                                                // 일반 모드: 상세 보기 화면으로 이동
                                                onMediaSelected(mediaUri, images)
                                            }
                                        },
                                        onLongPress = {
                                            // 길게 누르면 편집 모드로 전환하고 해당 이미지 선택
                                            if (!isEditMode) {
                                                isEditMode = true
                                                selectedItems = setOf(mediaUri)
                                            }
                                        }
                                    )
                                }
                        ) {
                            if (isVideo) {
                                // 동영상 썸네일 또는 배경
                                if (videoThumbnail != null) {
                                    Image(
                                        bitmap = videoThumbnail!!.asImageBitmap(),
                                        contentDescription = "동영상 썸네일",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 썸네일 로딩 중 배경
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = "동영상",
                                        tint = palette.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(48.dp)
                                    )
                                }
                                // 동영상 아이콘 오버레이 (우측 하단)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = "동영상",
                                        tint = palette.onBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                // 이미지
                                Image(
                                    painter = rememberGalleryGridPhotoPainter(mediaUri, gridThumbPx),
                                    contentDescription = "촬영한 미디어",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // 선택 표시
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(28.dp)
                                        .background(Color(0xFF7ED321), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "선택됨",
                                        tint = palette.onBackground,
                                        modifier = Modifier.size(18.dp)
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
        }

        // 업로드 팝업(UI) - 탭별로 분리 표시
        if (uploadSourceTab == libraryTab) {
            if (isUploading) {
                val total = uploadProgress.second.takeIf { it > 0 } ?: 100
                val current = uploadProgress.first.coerceIn(0, total)
                val percent = ((current.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
                val progressFraction = current.toFloat() / total.toFloat()

                Dialog(onDismissRequest = { /* 전송 중에는 닫지 않음 */ }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "서버로 전송 중",
                                color = palette.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${percent}%",
                                color = palette.onBackground,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(palette.onBackground.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                        .background(palette.onBackground, RoundedCornerShape(6.dp))
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uploadMessage ?: "처리 중...",
                                color = palette.onBackground.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                // 업로드 결과 팝업 (완료: 서버 결과 번들 / 실패·무응답: 기존 알림)
                if (serverPipelineCompleteBundle != null) {
                    val spb = serverPipelineCompleteBundle!!
                    val completionBtnBg = if (palette.isDark) Color.Black else Color.White
                    val completionBtnFg = if (palette.isDark) Color.White else Color.Black
                    val completionBtnDisabled = if (palette.isDark) Color(0xFF444444) else Color(0xFFB8C0CC)
                    val completionBtnFgDisabled =
                        if (palette.isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.38f)
                    Dialog(onDismissRequest = { onServerPipelineCompleteBundleChange(null) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, palette.divider), RoundedCornerShape(16.dp))
                                .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                                .padding(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "서버 전송 완료",
                                    color = palette.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "결과 파일을 기기에 저장했습니다.",
                                    color = palette.onBackgroundMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(palette.onBackground.copy(alpha = 0.08f))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { 1f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = palette.onBackground,
                                            trackColor = palette.onBackground.copy(alpha = 0.12f),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "100%",
                                            color = palette.onBackground,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(completionBtnBg)
                                            .clickable {
                                                val ply = spb.plyFile
                                                if (!ply.exists()) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "PLY 파일을 찾을 수 없습니다.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@clickable
                                                }
                                                onEnqueueBackground3dgsFromBundle(spb)
                                                onLibraryTabChange(LibraryTab.MODEL_3D)
                                                onLibraryHubVisibilityChange(false)
                                                currentPlyModel = PlyModel(
                                                    name = ply.nameWithoutExtension.ifBlank { "model" },
                                                    file = ply,
                                                    lastModified = ply.lastModified()
                                                )
                                                libraryDetailScreen = LibraryDetailScreen.OBJ_VIEWER
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "PLY 확인",
                                            color = completionBtnFg,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    val analysisPng = spb.filesByKey["quality_png"]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (analysisPng?.exists() == true) completionBtnBg
                                                else completionBtnDisabled
                                            )
                                            .clickable(enabled = analysisPng?.exists() == true) {
                                                val f = analysisPng
                                                if (f == null || !f.exists()) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "분석 이미지가 없습니다.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@clickable
                                                }
                                                onEnqueueBackground3dgsFromBundle(spb)
                                                val u = uriToShareableContentUri(context, Uri.fromFile(f))
                                                if (u == null) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "이미지를 열 수 없습니다.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    val ai = gsAnalysisUris.indexOfFirst { it == u }
                                                    if (ai >= 0) {
                                                        onServerPipelineOpenImageViewer(gsAnalysisUris, ai)
                                                    } else {
                                                        onServerPipelineOpenImageViewer(listOf(u), 0)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "3DGS 분석\n이미지",
                                            color = if (analysisPng?.exists() == true) completionBtnFg else completionBtnFgDisabled,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(completionBtnBg)
                                            .clickable {
                                                // AI 탭 3DGS 분석 모드로 전환해 LLM에 자동 전송.
                                                // LLM 응답 후 메시지 버블에 "Word 저장·열기" 버튼이 나타납니다.
                                                val payload = buildPoliceInsurance3dgsPayload(
                                                    context,
                                                    spb,
                                                    basePrompt = "위 입력 파일을 기반으로 사고현장 분석 보고서를 작성하라",
                                                )
                                                onServerPipelineStart3dgsAi(
                                                    Pending3dgsServerAutoSend(
                                                        nonce = System.nanoTime(),
                                                        promptText = payload.first,
                                                        imageUris = payload.second,
                                                        switchToAiTab = true,
                                                        sourceServerTaskId = spb.taskId,
                                                    )
                                                )
                                                // 다이얼로그 닫기
                                                onServerPipelineCompleteBundleChange(null)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "PLY 분석",
                                            color = completionBtnFg,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    val top = spb.filesByKey["topview"]
                                    val side = spb.filesByKey["sideview"]
                                    val previewsOk = top?.exists() == true && side?.exists() == true
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (previewsOk) completionBtnBg else completionBtnDisabled)
                                            .clickable(enabled = previewsOk) {
                                                if (!previewsOk) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "미리보기 이미지가 없습니다.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@clickable
                                                }
                                                onEnqueueBackground3dgsFromBundle(spb)
                                                val u1 = uriToShareableContentUri(context, Uri.fromFile(top!!))
                                                val u2 = uriToShareableContentUri(context, Uri.fromFile(side!!))
                                                if (u1 == null || u2 == null) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "이미지를 열 수 없습니다.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    val pi = gsPreviewUris.indexOfFirst { it == u1 }
                                                    if (pi >= 0) {
                                                        onServerPipelineOpenImageViewer(gsPreviewUris, pi)
                                                    } else {
                                                        onServerPipelineOpenImageViewer(listOf(u1, u2), 0)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "3DGS\n미리보기",
                                            color = if (previewsOk) completionBtnFg else completionBtnFgDisabled,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(completionBtnBg.copy(alpha = 0.55f))
                                        .clickable { onServerPipelineCompleteBundleChange(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "닫기",
                                        color = completionBtnFg,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                if (showUploadResultPopup && uploadResultPopupMessage != null) {
                    Dialog(
                        onDismissRequest = {
                            showUploadResultPopup = false
                            uploadResultPopupMessage = null
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "알림",
                                    color = palette.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uploadResultPopupMessage!!,
                                    color = palette.onBackground.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "확인",
                                        color = palette.onBrand,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(12.dp))
                                            .background(palette.brand)
                                            .clickable {
                                                showUploadResultPopup = false
                                                uploadResultPopupMessage = null
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 업로드 메시지(완료/실패)는 기존처럼 배너로 잠깐 표시
                uploadMessage?.let { message ->
                    LaunchedEffect(message) {
                        delay(3000)
                        uploadMessage = null
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (message.contains("실패") || message.contains("20장 이상")) Color.Red.copy(alpha = 0.8f)
                                else Color.Green.copy(alpha = 0.8f)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = message,
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        jsonLibraryDetailFile?.let { jf ->
            val bodyText = remember(jf.absolutePath, jf.lastModified()) {
                try {
                    jf.readText(Charsets.UTF_8).let { t ->
                        if (t.length > 48_000) t.take(48_000) + "\n\n…(이하 생략)" else t
                    }
                } catch (_: Exception) {
                    "(파일을 읽을 수 없습니다)"
                }
            }
            AlertDialog(
                onDismissRequest = { jsonLibraryDetailFile = null },
                title = {
                    Text(
                        text = jf.name,
                        color = palette.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        item {
                            Text(
                                text = bodyText,
                                color = palette.onBackground.copy(alpha = 0.92f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { jsonLibraryDetailFile = null }) {
                        Text("닫기", color = palette.brand)
                    }
                },
                containerColor = palette.dialogSurface
            )
        }

        arcoreLibraryDetailFile?.let { af ->
            val ext = af.extension.lowercase()
            val bodyText = remember(af.absolutePath, af.lastModified()) {
                when {
                    ext in setOf("json", "txt", "xml", "gltf", "csv") || af.length() <= 512_000 -> try {
                        af.readText(Charsets.UTF_8).let { t ->
                            if (t.length > 48_000) t.take(48_000) + "\n\n…(이하 생략)" else t
                        }
                    } catch (_: Exception) {
                        "(텍스트를 읽을 수 없습니다)"
                    }
                    else -> "(미리보기: ${af.length() / 1024} KB)\n${af.absolutePath}"
                }
            }
            AlertDialog(
                onDismissRequest = { arcoreLibraryDetailFile = null },
                title = {
                    Text(
                        text = af.name,
                        color = palette.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            Text(
                                text = bodyText,
                                color = palette.onBackground.copy(alpha = 0.92f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { arcoreLibraryDetailFile = null }) {
                        Text("닫기", color = palette.brand)
                    }
                },
                containerColor = palette.dialogSurface
            )
        }

        // --- 다이얼로그 구역 (어떤 탭에서도 보일 수 있도록 탭 분기 바깥에 배치) ---

        // [추가] 1차 배경제거 진행률 팝업 (온디바이스)
        if (isBgRemoving) {
            val fraction = (sam3ProgressPercent.coerceIn(0, 100) / 100f)
            Dialog(onDismissRequest = { /* 처리 중 닫기 불가 */ }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "1차 배경 제거 중",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MediaPipe Image Segmentation",
                            color = Color(0xFF9CD83B),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // 퍼센트 숫자
                        Text(
                            text = "${sam3ProgressPercent.coerceIn(0, 100)}%",
                            color = Color(0xFF9CD83B),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // 진행 바
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(palette.onBackground.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .background(Color(0xFF9CD83B), RoundedCornerShape(6.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 단계 메시지
                        Text(
                            text = sam3ProgressMessage,
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // [추가] SAM3 완료 결과 팝업
        sam3ResultMessage?.let { resultMsg ->
            Dialog(onDismissRequest = { sam3ResultMessage = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = if (resultMsg.contains("실패")) "처리 결과" else "완료",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = resultMsg,
                            color = palette.onBackground.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "확인",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (resultMsg.contains("실패")) Color.Red
                                        else Color(0xFF1A6B2F)
                                    )
                                    .clickable { sam3ResultMessage = null }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // [추가] 광택 제거 진행 다이얼로그
        if (isGlareRemoving) {
            val fraction = glareProgressPercent.coerceIn(0, 100) / 100f
            Dialog(onDismissRequest = { /* 처리 중 닫기 불가 */ }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "광택 제거 중",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "온디바이스 탐지·맥락 채움·질감 보정 (AI 지우개 유사)",
                            color = Color(0xFFD4820A),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${glareProgressPercent.coerceIn(0, 100)}%",
                            color = Color(0xFFD4820A),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(palette.onBackground.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .background(Color(0xFFD4820A), RoundedCornerShape(6.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = glareProgressMessage,
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // [추가] 광택 제거 결과 다이얼로그
        glareResultMessage?.let { resultMsg ->
            Dialog(onDismissRequest = { glareResultMessage = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = if (resultMsg.contains("실패")) "처리 결과" else "완료",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = resultMsg,
                            color = palette.onBackground.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "확인",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (resultMsg.contains("실패")) Color.Red
                                        else Color(0xFF7B4F1E)
                                    )
                                    .clickable { glareResultMessage = null }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 새 데이터셋폴더: 이름 입력 후 복사 (앱 갤러리 원본은 유지)
        if (showNewDatasetFolderNameDialog) {
            Dialog(onDismissRequest = {
                if (!isTransferring) showNewDatasetFolderNameDialog = false
            }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "데이터셋폴더 이름",
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newDatasetFolderNameInput,
                            onValueChange = { newDatasetFolderNameInput = it },
                            placeholder = {
                                Text(
                                    text = "비워 두면 자동으로 이름이 붙습니다.",
                                    color = palette.onBackground.copy(alpha = 0.35f),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            enabled = !isTransferring,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.onBackground,
                                unfocusedTextColor = palette.onBackground,
                                focusedBorderColor = Color(0xFF9CD83B),
                                unfocusedBorderColor = palette.onBackground.copy(alpha = 0.4f),
                                cursorColor = Color(0xFF9CD83B),
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                disabledTextColor = palette.onBackground.copy(alpha = 0.5f),
                                disabledBorderColor = palette.onBackground.copy(alpha = 0.2f),
                                disabledContainerColor = Color(0xFF1E1E1E)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isTransferring) {
                                        showNewDatasetFolderNameDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTransferring) "저장 중…" else "확인",
                                color = if (!isTransferring && pendingDatasetFolderImageUris.isNotEmpty()) {
                                    palette.onBackground
                                } else {
                                    palette.onBackground.copy(alpha = 0.35f)
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (!isTransferring && pendingDatasetFolderImageUris.isNotEmpty()) {
                                            Color(0xFF1A6B2F)
                                        } else {
                                            Color(0xFF1A6B2F).copy(alpha = 0.3f)
                                        }
                                    )
                                    .clickable(
                                        enabled = !isTransferring && pendingDatasetFolderImageUris.isNotEmpty()
                                    ) {
                                        val urisCopy = pendingDatasetFolderImageUris.toList()
                                        val nameInput = newDatasetFolderNameInput
                                        showNewDatasetFolderNameDialog = false
                                        isTransferring = true
                                        uploadSourceTab = LibraryTab.GALLERY
                                        uploadMessage = "데이터셋폴더에 복사 중..."
                                        transferScope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                copyImagesToDatasetFolder(context, urisCopy, nameInput)
                                            }
                                            isTransferring = false
                                            uploadMessage = result.message
                                            Toast.makeText(
                                                context,
                                                result.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            if (result.successCount > 0) {
                                                selectedItems = emptySet()
                                                isEditMode = false
                                                pendingGalleryMenuAction = PendingGalleryMenuAction.None
                                                pendingDatasetFolderImageUris = emptyList()
                                                newDatasetFolderNameInput = ""
                                                loadDatasetFolders(context) { folders ->
                                                    datasetFolders = folders
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // [추가] 1차 배경제거 프롬프트 입력 다이얼로그
        if (showBgRemoveDialog) {
            val isPromptValid = !bgRemovePromptError
            val canApply = isPromptValid
            /** 갤러리: bulk URI 없음 / 데이터셋: 확인 직후 bulk URI 설정 — 후자일 때 닫기면 폴더 선택·편집 모드까지 종료 */
            val dismissBgRemoveDialogWithoutApply = {
                val wasDatasetBulk = pendingBulkImageUrisForBgRemove != null
                showBgRemoveDialog = false
                pendingBulkImageUrisForBgRemove = null
                pendingDatasetFolderPathsForBulkBgRemove = null
                bgRemovePrompt = ""
                bgRemovePromptError = false
                if (wasDatasetBulk) {
                    isDatasetEditMode = false
                    selectedDatasetFolders = emptySet()
                    pendingDatasetMenuAction = PendingDatasetMenuAction.None
                }
            }

            Dialog(onDismissRequest = dismissBgRemoveDialogWithoutApply) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "어떤 사물을 추출하시겠습니까?",
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = bgRemovePrompt,
                            onValueChange = { input ->
                                bgRemovePrompt = input
                                bgRemovePromptError = input.isNotEmpty() && !input.all { it.isLetter() && it.code < 128 || it == ' ' }
                            },
                            placeholder = {
                                Text(
                                    text = "예: cup, mouse, bottle",
                                    color = palette.onBackground.copy(alpha = 0.35f),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            isError = bgRemovePromptError,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.onBackground,
                                unfocusedTextColor = palette.onBackground,
                                focusedBorderColor = if (bgRemovePromptError) Color(0xFFFF5252) else Color(0xFF9CD83B),
                                unfocusedBorderColor = if (bgRemovePromptError) Color(0xFFFF5252) else palette.onBackground.copy(alpha = 0.4f),
                                cursorColor = Color(0xFF9CD83B),
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                errorBorderColor = Color(0xFFFF5252),
                                errorContainerColor = Color(0xFF1E1E1E),
                                errorTextColor = palette.onBackground,
                                errorCursorColor = Color(0xFFFF5252)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (bgRemovePromptError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "영어로 입력해 주세요",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp + 18.sp.value.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable(onClick = dismissBgRemoveDialogWithoutApply)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "적용",
                                color = if (canApply) Color.White else palette.onBackground.copy(alpha = 0.35f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (canApply) Color(0xFF1A6B2F)
                                        else Color(0xFF1A6B2F).copy(alpha = 0.3f)
                                    )
                                    .clickable(enabled = canApply) {
                                        val bulkSnapshot = pendingBulkImageUrisForBgRemove
                                        val fromDatasetFolders = bulkSnapshot != null
                                        val datasetPathsSnapshot = pendingDatasetFolderPathsForBulkBgRemove
                                        showBgRemoveDialog = false
                                        pendingBulkImageUrisForBgRemove = null
                                        pendingDatasetFolderPathsForBulkBgRemove = null
                                        isBgRemoving = true
                                        sam3ProgressPercent = 0
                                        sam3ProgressMessage = "준비 중..."
                                        val prompt = bgRemovePrompt.trim()
                                        val items = bulkSnapshot ?: selectedItems.toList()
                                        val total = items.size
                                        val outputDir: File = if (
                                            fromDatasetFolders &&
                                            !datasetPathsSnapshot.isNullOrEmpty()
                                        ) {
                                            createDatasetBatchResultFolder(
                                                context,
                                                datasetPathsSnapshot,
                                                "배경제거"
                                            )
                                        } else {
                                            context.getExternalFilesDir(null) ?: context.filesDir
                                        }
                                        transferScope.launch {
                                            val bgStartMs = System.currentTimeMillis()
                                            // 포그라운드 서비스 시작
                                            startOrUpdateForegroundService(
                                                context, "배경 제거 중 (${total}장)", 0, "준비 중...", bgStartMs
                                            )
                                            var successCount = 0
                                            var failCount = 0
                                            val savedUris = mutableListOf<Uri>()
                                            items.forEachIndexed { index, uri ->
                                                val itemLabel = if (total > 1) " (${index + 1}/$total)" else ""
                                                val basePercent = index * 95 / total
                                                val nextPercent  = (index + 1) * 95 / total
                                                val itemRange = (nextPercent - basePercent).coerceAtLeast(1)

                                                sam3ProgressPercent = basePercent
                                                sam3ProgressMessage = "이미지 로드 중...$itemLabel"
                                                startOrUpdateForegroundService(
                                                    context, "배경 제거 중 (${total}장)",
                                                    basePercent, "이미지 로드 중...$itemLabel", bgStartMs
                                                )

                                                // 비트맵 디코딩
                                                val bitmap = withContext(Dispatchers.IO) {
                                                    decodeBitmapWithMaxDimension(context, uri, 1024)
                                                }
                                                if (bitmap == null) {
                                                    failCount++
                                                    return@forEachIndexed
                                                }

                                                sam3ProgressPercent = basePercent + itemRange / 8
                                                sam3ProgressMessage = "MediaPipe 세그멘테이션 준비 중...$itemLabel"

                                                // MediaPipe Image Segmentation 기반 온디바이스 배경 제거
                                                val result = withContext(Dispatchers.IO) {
                                                    BackgroundRemovalProcessor.removeBackground(
                                                        context    = context,
                                                        sourceBitmap = bitmap,
                                                        userPrompt = prompt,
                                                        outputDir  = outputDir,
                                                        onProgress = { iter, totalIter ->
                                                            if (totalIter > 0) {
                                                                val frac = iter.toFloat() / totalIter
                                                                val mapped = basePercent +
                                                                    (itemRange * 0.15f + frac * itemRange * 0.75f).toInt()
                                                                sam3ProgressPercent = mapped.coerceIn(basePercent, nextPercent - 1)
                                                                val stepMsg = "세그멘테이션 ${"%.0f".format(frac * 100)}%$itemLabel"
                                                                sam3ProgressMessage = stepMsg
                                                                startOrUpdateForegroundService(
                                                                    context, "배경 제거 중 (${total}장)",
                                                                    sam3ProgressPercent, stepMsg, bgStartMs
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                                bitmap.recycle()

                                                when (result) {
                                                    is BackgroundRemovalProcessor.Result.Success -> {
                                                        result.savedUri?.let { savedUris.add(it) }
                                                        successCount++
                                                        sam3ProgressPercent = nextPercent
                                                        sam3ProgressMessage = "완료$itemLabel"
                                                    }
                                                    is BackgroundRemovalProcessor.Result.Error -> {
                                                        failCount++
                                                        sam3ProgressMessage = "오류: ${result.message}"
                                                    }
                                                }
                                            }
                                            sam3ProgressPercent = 100
                                            isBgRemoving = false
                                            sam3ResultMessage = when {
                                                successCount > 0 && failCount == 0 && fromDatasetFolders ->
                                                    "배경 제거 완료\n${successCount}장이 새 데이터셋 폴더 「${outputDir.name}」에 저장되었습니다."
                                                successCount > 0 && failCount == 0 ->
                                                    "배경 제거 완료\n${successCount}장이 앱 갤러리에 저장되었습니다."
                                                successCount > 0 && fromDatasetFolders ->
                                                    "배경 제거 완료\n${successCount}장 성공, ${failCount}장 실패\n성공분은 「${outputDir.name}」에 저장되었습니다."
                                                successCount > 0 ->
                                                    "배경 제거 완료\n${successCount}장 성공, ${failCount}장 실패"
                                                else ->
                                                    "배경 제거 실패\n객체를 찾지 못했거나 모델이 없습니다.\n(assets/models/ 폴더를 확인하세요)"
                                            }
                                            // 포그라운드 서비스 완료 처리
                                            val doneMsg = if (successCount > 0) "${successCount}장 완료" else "처리 실패"
                                            stopForegroundService(context, "배경 제거 완료", doneMsg)
                                            if (successCount > 0) {
                                                selectedItems = emptySet()
                                                isEditMode = false
                                                pendingGalleryMenuAction = PendingGalleryMenuAction.None
                                                if (fromDatasetFolders) {
                                                    isDatasetEditMode = false
                                                    selectedDatasetFolders = emptySet()
                                                    pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                                    loadDatasetFolders(context) { datasetFolders = it }
                                                }
                                                onImageDeleted()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3D 모델링 프롬프트 입력 다이얼로그
        if (show3DModelingDialog) {
            val isValid3DPrompt = modelingPromptText.isNotEmpty() && !modelingPromptError
            val noServerResponseMsg = "서버에 대한 응답이 없습니다.\n서버 연결을 확인해주십시오."
            val modelingPromptFieldBg =
                if (palette.isDark) Color(0xFF1E1E1E) else palette.surfaceCardAlt

            Dialog(onDismissRequest = { show3DModelingDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "3D 모델링",
                            color = Color(0xFF9CD83B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "어떤 사물을 3D 모델링하시겠습니까?",
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = modelingPromptText,
                            onValueChange = { input ->
                                modelingPromptText = input
                                // 영문자, 숫자, 스페이스, 특수기호(ASCII 0x20~0x7E)만 허용
                                modelingPromptError = input.isNotEmpty() && !input.all { it.code in 0x20..0x7E }
                            },
                            placeholder = {
                                Text(
                                    text = "예: gundam figure, white cup",
                                    color = palette.onBackground.copy(alpha = 0.35f),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            isError = modelingPromptError,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.onBackground,
                                unfocusedTextColor = palette.onBackground,
                                focusedBorderColor = if (modelingPromptError) Color(0xFFFF5252) else Color(0xFF9CD83B),
                                unfocusedBorderColor = if (modelingPromptError) Color(0xFFFF5252) else palette.onBackground.copy(alpha = 0.4f),
                                cursorColor = Color(0xFF9CD83B),
                                focusedContainerColor = modelingPromptFieldBg,
                                unfocusedContainerColor = modelingPromptFieldBg,
                                errorBorderColor = Color(0xFFFF5252),
                                errorContainerColor = modelingPromptFieldBg,
                                errorTextColor = palette.onBackground,
                                errorCursorColor = Color(0xFFFF5252)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (modelingPromptError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "영어, 숫자, 특수기호만 입력 가능합니다 (한국어 불가)",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp + 18.sp.value.dp))
                        }
                        if (pending3DSourceTab == LibraryTab.DATASET) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ARCore ZIP (선택) · poses.json 등이 포함된 ZIP",
                                color = palette.onBackground.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val arcoreZipLabel = pending3DArcoreZipUriForDataset?.let { u ->
                                if (u.scheme == ContentResolver.SCHEME_FILE || u.scheme.isNullOrEmpty()) {
                                    u.path?.let { p -> File(p).name }
                                } else {
                                    context.contentResolver.query(
                                        u,
                                        arrayOf(OpenableColumns.DISPLAY_NAME),
                                        null,
                                        null,
                                        null,
                                    )?.use { c ->
                                        if (c.moveToFirst()) c.getString(0) else null
                                    } ?: u.lastPathSegment
                                }
                            } ?: "선택 안 됨"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = arcoreZipLabel ?: "ZIP",
                                    color = palette.onBackground.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "선택",
                                    color = palette.brand,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showDatasetArcoreLibraryPicker = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                                if (pending3DArcoreZipUriForDataset != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "지우기",
                                        color = palette.onBackground.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { pending3DArcoreZipUriForDataset = null }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { show3DModelingDialog = false }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "전송",
                                color = if (isValid3DPrompt) Color.White else palette.onBackground.copy(alpha = 0.35f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isValid3DPrompt) Color(0xFF1B4F8A)
                                        else Color(0xFF1B4F8A).copy(alpha = 0.3f)
                                    )
                                    .clickable(enabled = isValid3DPrompt) {
                                        val promptSnapshot = modelingPromptText.trim()
                                        show3DModelingDialog = false
                                        uploadSourceTab = pending3DSourceTab

                                        when (pending3DSourceTab) {
                                            LibraryTab.GALLERY -> {
                                                val urisSnapshot = pending3DGalleryUris
                                                isUploading = true
                                                uploadProgress = 0 to 100
                                                uploadMessage = "업로드 준비 중..."
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val mainHandler = Handler(Looper.getMainLooper())
                                                    try {
                                                        val zipFile = createZipFromUris(
                                                            context = context,
                                                            uris = urisSnapshot,
                                                            zipPrefix = "media"
                                                        )
                                                        val bundle = if (zipFile != null) {
                                                            uploadZipAndRunPipeline(
                                                                context = context,
                                                                zipFile = zipFile,
                                                                prompt = promptSnapshot,
                                                                onProgress = { p, msg ->
                                                                    uploadProgress = p to 100
                                                                    uploadMessage = msg
                                                                }
                                                            )
                                                        } else null
                                                        mainHandler.post {
                                                            isUploading = false
                                                            if (bundle != null) {
                                                                onServerPipelineCompleteBundleChange(bundle)
                                                                libraryModelThumbRefresh++
                                                                selectedItems = emptySet()
                                                                isEditMode = false
                                                            } else {
                                                                uploadResultPopupMessage =
                                                                    if (uploadMessage == noServerResponseMsg) noServerResponseMsg else "업로드 실패"
                                                                showUploadResultPopup = true
                                                                if (uploadMessage != noServerResponseMsg) uploadMessage = "업로드 실패"
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        mainHandler.post {
                                                            isUploading = false
                                                            uploadMessage = "업로드 실패"
                                                        }
                                                    }
                                                }
                                            }
                                            LibraryTab.DATASET -> {
                                                val foldersSnapshot = pending3DDatasetFolders
                                                // Compose 상태는 메인 스레드에서만 안전하게 읽을 수 있으므로
                                                // IO 코루틴 진입 전에 스냅샷으로 캡처합니다.
                                                val arcoreUriSnapshot = pending3DArcoreZipUriForDataset
                                                isUploading = true
                                                uploadProgress = 0 to 100
                                                uploadMessage = "업로드 준비 중..."
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val mainHandler = Handler(Looper.getMainLooper())
                                                    try {
                                                        val folderFiles = foldersSnapshot
                                                            .map { File(it) }
                                                            .filter { it.exists() && it.isDirectory }
                                                        val rawDatasetZip = createZipFromFolders(
                                                            context = context,
                                                            folders = folderFiles,
                                                            zipPrefix = "dataset"
                                                        )
                                                        val bundle = if (rawDatasetZip != null) {
                                                            // ── ARCore ZIP 처리 ──────────────────────────────
                                                            // 서버(main.py)는 file_pc 안의 poses.json만 ARCore로 사용.
                                                            // → poses.json 을 file_pc(데이터셋 ZIP)에 병합.
                                                            // → ARCore ZIP 원본은 file_gs 로도 전달(GS_ENABLE 시 3DGS 서버용).
                                                            val arcoreZipForGs: File?
                                                            val fileForPc: File
                                                            val arcoreUri = arcoreUriSnapshot
                                                            if (arcoreUri != null) {
                                                                val arcoreTmp =
                                                                    copyContentUriToTempZipFile(context, arcoreUri)
                                                                if (arcoreTmp != null) {
                                                                    // poses.json 을 데이터셋 ZIP에 병합 → file_pc
                                                                    val merged = mergeArcorePosesIntoDatasetZip(
                                                                        datasetZip = rawDatasetZip,
                                                                        arcoreZip  = arcoreTmp,
                                                                        context    = context,
                                                                    )
                                                                    if (merged != null) {
                                                                        // 병합 성공 — 원본 데이터셋 ZIP 삭제
                                                                        try { rawDatasetZip.delete() } catch (_: Exception) {}
                                                                        fileForPc = merged
                                                                    } else {
                                                                        // poses.json 없음 — 데이터셋 ZIP 그대로 사용
                                                                        mainHandler.post {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "ARCore ZIP에서 poses.json을 찾지 못했습니다.\n데이터셋만 file_pc로 전송합니다.",
                                                                                Toast.LENGTH_LONG,
                                                                            ).show()
                                                                        }
                                                                        fileForPc = rawDatasetZip
                                                                    }
                                                                    arcoreZipForGs = arcoreTmp  // file_gs: 3DGS 서버용
                                                                } else {
                                                                    mainHandler.post {
                                                                        Toast.makeText(
                                                                            context,
                                                                            "ARCore ZIP 복사에 실패했습니다. 데이터셋만 전송합니다.",
                                                                            Toast.LENGTH_LONG,
                                                                        ).show()
                                                                    }
                                                                    fileForPc      = rawDatasetZip
                                                                    arcoreZipForGs = null
                                                                }
                                                            } else {
                                                                fileForPc      = rawDatasetZip
                                                                arcoreZipForGs = null
                                                            }

                                                            uploadZipAndRunPipeline(
                                                                context = context,
                                                                zipFile = fileForPc,
                                                                prompt  = promptSnapshot,
                                                                onProgress = { p, msg ->
                                                                    uploadProgress = p to 100
                                                                    uploadMessage = msg
                                                                },
                                                                gsZipFile                  = arcoreZipForGs,
                                                                contentDispositionFilename   = SERVER_PIPELINE_ZIP_NAME_DATASET,
                                                                contentDispositionGsFilename = SERVER_PIPELINE_ZIP_NAME_ARCORE,
                                                            )
                                                        } else null
                                                        mainHandler.post {
                                                            isUploading = false
                                                            if (bundle != null) {
                                                                pending3DArcoreZipUriForDataset = null
                                                                onServerPipelineCompleteBundleChange(bundle)
                                                                libraryModelThumbRefresh++
                                                                selectedDatasetFolders = emptySet()
                                                                isDatasetEditMode = false
                                                            } else {
                                                                uploadResultPopupMessage =
                                                                    if (uploadMessage == noServerResponseMsg) noServerResponseMsg else "업로드 실패"
                                                                showUploadResultPopup = true
                                                                if (uploadMessage != noServerResponseMsg) uploadMessage = "업로드 실패"
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        mainHandler.post {
                                                            isUploading = false
                                                            uploadMessage = "업로드 실패"
                                                        }
                                                    }
                                                }
                                            }
                                            LibraryTab.AR_CORE_LIBRARY -> {
                                                val pathsSnapshot = pending3DArcoreZipPaths
                                                isUploading = true
                                                uploadProgress = 0 to 100
                                                uploadMessage = "업로드 준비 중..."
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val mainHandler = Handler(Looper.getMainLooper())
                                                    try {
                                                        var lastBundle: ServerPipelineResultBundle? = null
                                                        var anyFail = false
                                                        for ((idx, pathStr) in pathsSnapshot.withIndex()) {
                                                            val src = File(pathStr)
                                                            if (!src.isFile || !src.name.endsWith(".zip", ignoreCase = true)) {
                                                                anyFail = true
                                                                continue
                                                            }
                                                            val tmp = File(
                                                                context.cacheDir,
                                                                "arcore_upload_${System.currentTimeMillis()}_${idx}_${src.name}",
                                                            )
                                                            src.copyTo(tmp, overwrite = true)
                                                            val bundle = uploadZipAndRunPipeline(
                                                                context = context,
                                                                zipFile = tmp,
                                                                prompt = promptSnapshot,
                                                                onProgress = { p, msg ->
                                                                    mainHandler.post {
                                                                        uploadProgress = p to 100
                                                                        uploadMessage = msg
                                                                    }
                                                                },
                                                                contentDispositionFilename = SERVER_PIPELINE_ZIP_NAME_ARCORE,
                                                            )
                                                            if (bundle != null) {
                                                                lastBundle = bundle
                                                            } else {
                                                                anyFail = true
                                                            }
                                                        }
                                                        mainHandler.post {
                                                            isUploading = false
                                                            if (lastBundle != null) {
                                                                onServerPipelineCompleteBundleChange(lastBundle)
                                                                libraryModelThumbRefresh++
                                                                selectedLibraryDeletePaths = emptySet()
                                                                if (anyFail) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "일부 ZIP 전송에 실패했습니다.",
                                                                        Toast.LENGTH_LONG,
                                                                    ).show()
                                                                }
                                                            } else {
                                                                uploadResultPopupMessage =
                                                                    if (uploadMessage == noServerResponseMsg) noServerResponseMsg else "업로드 실패"
                                                                showUploadResultPopup = true
                                                                if (uploadMessage != noServerResponseMsg) {
                                                                    uploadMessage = "업로드 실패"
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        mainHandler.post {
                                                            isUploading = false
                                                            uploadMessage = "업로드 실패"
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDatasetArcoreLibraryPicker) {
            Dialog(onDismissRequest = { showDatasetArcoreLibraryPicker = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.dialogSurface, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                ) {
                    Column {
                        Text(
                            text = "ARCore 라이브러리",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "전송에 사용할 ZIP을 선택하세요.",
                            color = palette.onBackgroundMuted,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (datasetArcoreZipChoices.isEmpty()) {
                            Text(
                                text = "ARCore 라이브러리에 ZIP이 없습니다.\n라이브러리 탭에서 파일을 추가한 뒤 다시 시도하세요.",
                                color = palette.onBackgroundMuted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(datasetArcoreZipChoices, key = { it.absolutePath }) { f ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                pending3DArcoreZipUriForDataset = Uri.fromFile(f)
                                                showDatasetArcoreLibraryPicker = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = f.name,
                                            color = palette.onBackground,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { showDatasetArcoreLibraryPicker = false }) {
                                Text(text = "닫기", color = palette.brand)
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            Dialog(onDismissRequest = { showDeleteConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "삭제 확인",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "삭제하시겠습니까?",
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { showDeleteConfirm = false }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "삭제",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red)
                                    .clickable {
                                        val itemsToDelete = selectedItems.toList()
                                        itemsToDelete.forEach { uri ->
                                            deleteMediaByUri(context, uri)
                                        }
                                        selectedItems = emptySet()
                                        isEditMode = false
                                        showDeleteConfirm = false
                                        onImageDeleted()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteAllConfirm) {
            Dialog(onDismissRequest = { showDeleteAllConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "전체 삭제",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "정말로 모든 미디어를 삭제하겠습니까?",
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { showDeleteAllConfirm = false }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "삭제",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red)
                                    .clickable {
                                        deleteAllMedia(context)
                                        selectedItems = emptySet()
                                        isEditMode = false
                                        showDeleteAllConfirm = false
                                        onImageDeleted()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDatasetDeleteAllConfirm) {
            Dialog(onDismissRequest = { showDatasetDeleteAllConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "데이터셋폴더 전체 삭제",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "모든 데이터셋 폴더와 그 안의 이미지를 삭제하겠습니까?",
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { showDatasetDeleteAllConfirm = false }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "삭제",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red)
                                    .clickable {
                                        deleteAllDatasetFolders(context)
                                        showDatasetDeleteAllConfirm = false
                                        isDatasetEditMode = false
                                        selectedDatasetFolders = emptySet()
                                        pendingDatasetMenuAction = PendingDatasetMenuAction.None
                                        loadDatasetFolders(context) { datasetFolders = it }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDatasetDeleteConfirm) {
            Dialog(onDismissRequest = { showDatasetDeleteConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "폴더 삭제",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "정말로 삭제하시겠습니까?",
                            color = palette.onBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { showDatasetDeleteConfirm = false }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "삭제",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red)
                                    .clickable {
                                        val pathsToDelete = selectedDatasetFolders.toList()
                                        pathsToDelete.forEach { path ->
                                            try {
                                                File(path).deleteRecursively()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        selectedDatasetFolders = emptySet()
                                        isDatasetEditMode = false
                                        showDatasetDeleteConfirm = false
                                        if (currentDatasetFolder != null &&
                                            pathsToDelete.contains(currentDatasetFolder!!.dir.absolutePath)
                                        ) {
                                            currentDatasetFolder = null
                                            datasetImages = emptyList()
                                            libraryDetailScreen = LibraryDetailScreen.NONE
                                        }
                                        loadDatasetFolders(context) { folders ->
                                            datasetFolders = folders
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteLibraryItemsConfirm && selectedLibraryDeletePaths.isNotEmpty()) {
            val pathsToDelete = selectedLibraryDeletePaths.toList()
            val deleteSummaryText =
                if (pathsToDelete.size == 1) {
                    "「${File(pathsToDelete[0]).name}」을(를) 삭제하시겠습니까?"
                } else {
                    "선택한 ${pathsToDelete.size}개 항목을 삭제하시겠습니까?"
                }
            Dialog(onDismissRequest = { showDeleteLibraryItemsConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                ) {
                    Column {
                        Text(
                            text = "삭제 확인",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = deleteSummaryText,
                            color = palette.onBackground.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        palette.onBackground.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        showDeleteLibraryItemsConfirm = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "삭제",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red)
                                    .clickable {
                                        showDeleteLibraryItemsConfirm = false
                                        transferScope.launch(Dispatchers.IO) {
                                            val pathSet = pathsToDelete.toSet()
                                            var failCount = 0
                                            for (targetPath in pathsToDelete) {
                                                val f = File(targetPath)
                                                val ok = try {
                                                    !f.exists() || f.delete()
                                                } catch (_: Exception) {
                                                    false
                                                }
                                                if (!ok) failCount++
                                            }
                                            withContext(Dispatchers.Main) {
                                                val jsonCanon = jsonLibraryDetailFile?.let { jf ->
                                                    runCatching { jf.canonicalPath }
                                                        .getOrDefault(jf.absolutePath)
                                                }
                                                if (jsonCanon != null && jsonCanon in pathSet) {
                                                    jsonLibraryDetailFile = null
                                                }
                                                val arCanon = arcoreLibraryDetailFile?.let { af ->
                                                    runCatching { af.canonicalPath }
                                                        .getOrDefault(af.absolutePath)
                                                }
                                                if (arCanon != null && arCanon in pathSet) {
                                                    arcoreLibraryDetailFile = null
                                                }
                                                selectedLibraryDeletePaths = emptySet()
                                                libraryMiscRefresh++
                                                Toast.makeText(
                                                    context,
                                                    when {
                                                        failCount == 0 -> "삭제했습니다."
                                                        failCount == pathsToDelete.size ->
                                                            "삭제에 실패했습니다."
                                                        else -> "일부 항목 삭제에 실패했습니다."
                                                    },
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        if (showLibraryAssetDeleteConfirm) {
            Dialog(onDismissRequest = { showLibraryAssetDeleteConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "삭제 확인",
                            color = palette.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "삭제하시겠습니까?",
                            color = palette.onBackground.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "취소",
                                color = palette.onBackground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.onBackground.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { showLibraryAssetDeleteConfirm = false }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "삭제",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Red)
                                    .clickable {
                                        val paths = selectedLibraryAssetPaths.toList()
                                        var anyFail = false
                                        when (libraryTab) {
                                            LibraryTab.MODEL_3D -> {
                                                paths.forEach { p ->
                                                    val f = File(p)
                                                    if (!deleteLibraryModelFile(f)) anyFail = true
                                                    if (currentPlyModel?.file?.absolutePath == p) {
                                                        libraryDetailScreen =
                                                            if (f.extension.equals("obj", ignoreCase = true)) {
                                                                LibraryDetailScreen.MODEL_3D_OBJ_LIST
                                                            } else {
                                                                LibraryDetailScreen.MODEL_3D_PLY_LIST
                                                            }
                                                        currentPlyModel = null
                                                    }
                                                }
                                                loadModel3dLibrary(context) { lib ->
                                                    plyLibraryModels = lib.plyModels
                                                    objLibraryModels = lib.objModels
                                                }
                                            }
                                            LibraryTab.AI_CAD -> {
                                                paths.forEach { p ->
                                                    val f = File(p)
                                                    Model3dThumbnail.invalidateForModelFile(context, f)
                                                    if (!deleteAiCadArtifactsForStl(f)) anyFail = true
                                                    if (selectedAiCadStlFile?.absolutePath == p) {
                                                        selectedAiCadStlFile = null
                                                    }
                                                }
                                                onAiCadLibraryInvalidate()
                                            }
                                            else -> {}
                                        }
                                        if (anyFail) {
                                            Toast.makeText(
                                                context,
                                                "일부 항목 삭제에 실패했습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        libraryAssetEditMode = false
                                        selectedLibraryAssetPaths = emptySet()
                                        showLibraryAssetDeleteConfirm = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
