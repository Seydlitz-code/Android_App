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
import androidx.compose.material.icons.outlined.Description
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
import kotlin.text.Charsets
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

private const val MAX_JSON_APPENDIX_TOTAL_CHARS = 200_000
private const val MAX_JSON_APPENDIX_PER_FILE_CHARS = 80_000

private suspend fun readJsonAppendixForLlm(context: Context, uris: List<Uri>): String =
    withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext ""
        val sb = StringBuilder()
        for (uri in uris) {
            if (sb.length >= MAX_JSON_APPENDIX_TOTAL_CHARS) break
            val label = uri.lastPathSegment ?: uri.toString()
            val raw = try {
                when (uri.scheme) {
                    "file" -> {
                        val p = uri.path ?: ""
                        val f = File(p)
                        if (f.isFile) f.readText(Charsets.UTF_8) else ""
                    }
                    else -> context.contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() } ?: ""
                }
            } catch (_: Exception) {
                ""
            }
            if (raw.isEmpty()) continue
            val chunk = if (raw.length > MAX_JSON_APPENDIX_PER_FILE_CHARS) {
                raw.take(MAX_JSON_APPENDIX_PER_FILE_CHARS) + "\n...(파일 길이로 인해 잘림)"
            } else raw
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append("### ").append(label).append('\n').append(chunk)
            if (sb.length > MAX_JSON_APPENDIX_TOTAL_CHARS) break
        }
        sb.toString().take(MAX_JSON_APPENDIX_TOTAL_CHARS)
    }

@Composable
fun ClaudeChatScreen(
    galleryImages: List<Uri>,
    onGalleryUpdated: () -> Unit = {},
    onAiCadSavedToLibrary: () -> Unit = {},
    pending3dgsServerAutoSend: Pending3dgsServerAutoSend? = null,
    onPending3dgsServerAutoSendConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isStreaming by remember { mutableStateOf(false) }
    var streamingText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showImageSelectDialog by remember { mutableStateOf(false) }
    var datasetFolders by remember { mutableStateOf<List<DatasetFolder>>(emptyList()) }
    var attachedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var attachedJsonUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showJsonLibraryPicker by remember { mutableStateOf(false) }
    var aiTabMode by remember { mutableStateOf(AiChatTabMode.CLAUDE) }
    var aiCadOption by remember { mutableStateOf(ClaudeChatClient.AiCadInputOption.DIMENSIONS_DIRECT) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var stlDialogForIndex by remember { mutableStateOf<Int?>(null) }
    var stlSaveNameInput by remember { mutableStateOf("") }
    var stlBusyMessageIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    // 스레드 관리 상태
    var currentThreadId by remember { mutableStateOf<String?>(null) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var allThreads by remember { mutableStateOf<List<ConversationThread>>(emptyList()) }

    LaunchedEffect(stlDialogForIndex) {
        if (stlDialogForIndex != null) stlSaveNameInput = ""
    }

    // 갤러리: 이미지만 (동영상 제외)
    val galleryImageUris = remember(galleryImages) {
        galleryImages.filter { uri ->
            val path = uri.path ?: ""
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) ||
                path.endsWith(".png", true) || path.endsWith(".webp", true) ||
                path.endsWith(".heic", true) || path.endsWith(".heif", true)
        }
    }

    LaunchedEffect(Unit) {
        loadDatasetFolders(context) { datasetFolders = it }
        allThreads = ChatThreadStorage.loadAll(context)
    }

    // 새 메시지 추가 또는 스트리밍 시작 시 최하단으로 스크롤
    LaunchedEffect(messages.size, isStreaming) {
        val total = messages.size + if (isStreaming) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .imePadding()
    ) {
        // ── 메인 콘텐츠 ──
        Column(modifier = Modifier.fillMaxSize()) {

        // ── 상단 헤더: [메뉴] [모드 드롭다운] [새 채팅] ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 드로어 열기
            IconButton(onClick = { isDrawerOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "메뉴",
                    tint = palette.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 중앙: 모드 드롭다운 (기존 방식)
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { modeMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (aiTabMode) {
                            AiChatTabMode.CLAUDE -> "클로드"
                            AiChatTabMode.AI_CAD -> "AI CAD"
                            AiChatTabMode.MOBILE_3DGS -> "3DGS 분석"
                        },
                        color = palette.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "모드 선택",
                        tint = palette.onBackground.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                DropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false },
                    modifier = Modifier.background(palette.dropdownMenuBg)
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("클로드 AI LLM", color = palette.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "자연어 입력을 기반으로 답변을 생성합니다.",
                                    color = palette.onBackground.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.CLAUDE
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("AI CAD", color = palette.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "OpenSCAD로 곡면·라운딩을 포함한 형상을 생성합니다.",
                                    color = palette.onBackground.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.AI_CAD
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("3DGS 분석", color = palette.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "3DGS 분석: Anthropic Claude API(또는 프로필에서 고른 LLM)로 Mobile 3DGS·COLMAP·촬영 품질을 질의합니다.",
                                    color = palette.onBackground.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.MOBILE_3DGS
                            modeMenuExpanded = false
                        }
                    )
                }
            }

            // 우측: 새 채팅
            IconButton(onClick = {
                messages.clear()
                currentThreadId = null
                streamingText = ""
                errorMessage = null
                attachedJsonUris = emptyList()
            }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "새 채팅",
                    tint = palette.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.onBackground.copy(alpha = 0.3f))
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(messages) { index, msg ->
                val code = remember(msg.text) { extractOpenCadCode(msg.text) }
                val useAiCadWindow =
                    aiTabMode == AiChatTabMode.AI_CAD && !msg.isUser && code != null && msg.imageUris.isEmpty()
                if (useAiCadWindow) {
                    AiCadScriptWindowBubble(
                        codeText = code!!,
                        isConverting = stlBusyMessageIndex == index,
                        onSaveClick = { stlDialogForIndex = index }
                    )
                } else {
                    ChatMessageItem(message = msg)
                }
            }
            // 스트리밍 중: 실시간으로 들어오는 텍스트 표시
            if (isStreaming) {
                item(key = "streaming") {
                    ChatMessageItem(
                        message = ChatMessage(text = streamingText, isUser = false),
                        isStreaming = true
                    )
                }
            }
        }
        if (stlDialogForIndex != null) {
            AlertDialog(
                onDismissRequest = {
                    if (stlBusyMessageIndex == null) stlDialogForIndex = null
                },
                title = {
                    Text("모델 저장", color = palette.onBackground, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = buildString {
                                append("이름을 입력하거나 비워 두면 무작위 이름이 붙습니다.")
                                append("\n\n이 기기에서 OpenSCAD(WASM)로 렌더한 뒤 AI CAD 라이브러리에 저장합니다.")
                            },
                            color = palette.onBackground.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = stlSaveNameInput,
                            onValueChange = { stlSaveNameInput = it },
                            singleLine = true,
                            label = { Text("파일 이름 (선택)", color = palette.onBackground.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.onBackground,
                                unfocusedTextColor = palette.onBackground,
                                focusedBorderColor = palette.brand,
                                unfocusedBorderColor = palette.onBackground.copy(alpha = 0.35f),
                                cursorColor = palette.brand
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = stlBusyMessageIndex == null,
                        onClick = {
                            val idx = stlDialogForIndex ?: return@TextButton
                            val raw = messages.getOrNull(idx)?.text ?: return@TextButton
                            val c = extractOpenCadCode(raw) ?: return@TextButton
                            stlDialogForIndex = null
                            scope.launch {
                                stlBusyMessageIndex = idx
                                try {
                                    val trimmed = stlSaveNameInput.trim()
                                    val useRandom = trimmed.isEmpty()
                                    AiCadSaveCoordinator.exportToLibrary(
                                        context,
                                        c,
                                        preferredName = trimmed.takeIf { it.isNotEmpty() },
                                        useRandomWhenNameBlank = useRandom
                                    ).fold(
                                        onSuccess = {
                                            Toast.makeText(
                                                context,
                                                "기기에서 렌더한 모델을 AI CAD 라이브러리에 저장했습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onAiCadSavedToLibrary()
                                        },
                                        onFailure = { e ->
                                            Toast.makeText(
                                                context,
                                                "저장 실패: ${e.userMessageForAiCad()}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                } catch (e: Throwable) {
                                    Toast.makeText(
                                        context,
                                        "저장 실패: ${e.userMessageForAiCad()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    stlBusyMessageIndex = null
                                }
                            }
                        }
                    ) {
                        Text("저장", color = palette.brand, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { stlDialogForIndex = null },
                        enabled = stlBusyMessageIndex == null
                    ) {
                        Text("취소", color = palette.onBackground.copy(alpha = 0.85f))
                    }
                },
                containerColor = palette.dialogSurface
            )
        }
        // ── 하단 입력 영역 (GPT 스타일) ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.chatInputBarBg)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
        ) {
            // 에러 메시지
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = palette.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // AI CAD 모드 옵션 칩 (컴팩트)
            if (aiTabMode == AiChatTabMode.AI_CAD) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        ClaudeChatClient.AiCadInputOption.DIMENSIONS_DIRECT to "치수 직접 입력",
                        ClaudeChatClient.AiCadInputOption.INTERNET_REF to "인터넷 참조"
                    ).forEach { (option, label) ->
                        val isSelected = aiCadOption == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) palette.brand else palette.chatComposerPill
                                )
                                .clickable { aiCadOption = option }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) palette.onBrand else palette.onBackground.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (aiTabMode == AiChatTabMode.MOBILE_3DGS) {
                Text(
                    text = "프로필 → LLM API 키에서 Anthropic(클로드)를 선택하면 이 탭이 Claude 메시지 API로 동작합니다. 이미지 첨부 시 촬영 적합도 코멘트를 요청할 수 있습니다.",
                    color = palette.onBackgroundMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // 전송 로직 — 스트리밍 방식 (토큰 단위 onDelta 콜백)
            val doSend: () -> Unit = sendLambda@{
                if (isStreaming) return@sendLambda
                val text = messageText.trim()
                val images = attachedImages
                val jsonSnap =
                    if (aiTabMode == AiChatTabMode.MOBILE_3DGS) attachedJsonUris else emptyList()
                if (text.isEmpty() && images.isEmpty() && jsonSnap.isEmpty()) return@sendLambda

                val userBubbleText = when {
                    text.isNotEmpty() -> text
                    images.size > 1 -> "[${images.size}장의 이미지]"
                    images.size == 1 -> "[이미지 1장]"
                    jsonSnap.isNotEmpty() && images.isEmpty() -> "[JSON ${jsonSnap.size}개]"
                    jsonSnap.isNotEmpty() -> "[이미지 ${images.size}장·JSON ${jsonSnap.size}개]"
                    else -> ""
                }

                messages.add(
                    ChatMessage(
                        text = userBubbleText,
                        isUser = true,
                        imageUris = images,
                        jsonUris = jsonSnap
                    )
                )
                messageText = ""
                attachedImages = emptyList()
                attachedJsonUris = emptyList()
                isStreaming = true
                streamingText = ""
                errorMessage = null

                scope.launch {
                    val imageBase64List = images.mapNotNull { uri ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bitmap = BitmapFactory.decodeStream(stream)
                                bitmap?.let { ClaudeChatClient.bitmapToBase64(it) }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val jsonAppendix = if (aiTabMode == AiChatTabMode.MOBILE_3DGS && jsonSnap.isNotEmpty()) {
                        readJsonAppendixForLlm(context, jsonSnap)
                    } else ""
                    val defaultImgPrompt = when (aiTabMode) {
                        AiChatTabMode.MOBILE_3DGS -> when {
                            imageBase64List.size > 1 ->
                                "첨부된 여러 이미지를 3D Gaussian Splatting·다시점 재구성 관점에서 검토해 주세요."
                            imageBase64List.size == 1 ->
                                "첨부된 이미지를 3DGS 촬영·데이터 품질 관점에서 검토해 주세요."
                            else -> ""
                        }
                        else -> when {
                            imageBase64List.size > 1 -> "이 이미지들에 대해 설명해 주세요."
                            imageBase64List.size == 1 -> "이 이미지에 대해 설명해 주세요."
                            else -> ""
                        }
                    }
                    val streamBuffer = StringBuilder()
                    val result = when (aiTabMode) {
                        AiChatTabMode.CLAUDE -> ClaudeChatClient.streamMessage(
                            text = text.ifBlank { defaultImgPrompt },
                            imageBase64List = imageBase64List,
                            onDelta = { delta ->
                                streamBuffer.append(delta)
                                streamingText = streamBuffer.toString()
                            }
                        )
                        AiChatTabMode.AI_CAD -> ClaudeChatClient.streamAiCadMessage(
                            userText = text.ifBlank { "첨부 이미지를 참고해 3D 모델 코드를 작성해 주세요." },
                            imageBase64List = imageBase64List,
                            option = aiCadOption,
                            onDelta = { delta ->
                                streamBuffer.append(delta)
                                streamingText = streamBuffer.toString()
                            }
                        )
                        AiChatTabMode.MOBILE_3DGS -> {
                            val basePrompt = text.ifBlank {
                                when {
                                    imageBase64List.isNotEmpty() -> defaultImgPrompt
                                    jsonAppendix.isNotBlank() ->
                                        "첨부된 JSON 데이터를 바탕으로 Mobile 3DGS·COLMAP·촬영 데이터 관점에서 분석해 주세요."
                                    else ->
                                        "이 앱의 Mobile 3DGS(COLMAP 바이너리·갤러리 사진) 파이프라인을 소개하고, 흔한 문제 진단 방법을 알려 주세요."
                                }
                            }
                            val fullText = if (jsonAppendix.isNotBlank()) {
                                basePrompt + "\n\n--- 첨부 JSON ---\n" + jsonAppendix
                            } else {
                                basePrompt
                            }
                            ClaudeChatClient.streamMobile3dGsAnalysisMessage(
                                userText = fullText,
                                imageBase64List = imageBase64List,
                                onDelta = { delta ->
                                    streamBuffer.append(delta)
                                    streamingText = streamBuffer.toString()
                                }
                            )
                        }
                    }
                    isStreaming = false
                    streamingText = ""
                    when (result) {
                        is ClaudeChatClient.ChatResult.Success -> {
                            val finalText = result.text
                            messages.add(ChatMessage(text = finalText, isUser = false))
                            // 스레드 자동 저장
                            if (messages.isNotEmpty()) {
                                val title = messages.firstOrNull { it.isUser }?.text
                                    ?.take(48)?.replace('\n', ' ') ?: "새 대화"
                                val threadId = currentThreadId ?: UUID.randomUUID().toString()
                                val thread = ConversationThread(
                                    id = threadId,
                                    title = title,
                                    modeName = aiTabMode.name,
                                    messages = messages.map { m ->
                                        PersistedMessage(
                                            text = m.text,
                                            isUser = m.isUser,
                                            imageUriStrings = m.imageUris.map { it.toString() },
                                            jsonUriStrings = m.jsonUris.map { it.toString() }
                                                .takeIf { it.isNotEmpty() }
                                        )
                                    },
                                    updatedAt = System.currentTimeMillis()
                                )
                                currentThreadId = threadId
                                ChatThreadStorage.save(context, thread)
                                allThreads = ChatThreadStorage.loadAll(context)
                            }
                            if (aiTabMode == AiChatTabMode.AI_CAD) {
                                val code = extractOpenCadCode(finalText)
                                if (code != null) {
                                    val assistantIdx = messages.lastIndex
                                    stlBusyMessageIndex = assistantIdx
                                    try {
                                        AiCadSaveCoordinator.exportToLibrary(
                                            context,
                                            code
                                        ).fold(
                                            onSuccess = {
                                                Toast.makeText(
                                                    context,
                                                    "기기에서 렌더한 모델을 AI CAD 라이브러리에 저장했습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onAiCadSavedToLibrary()
                                            },
                                            onFailure = { e ->
                                                Toast.makeText(
                                                    context,
                                                    "자동 저장 실패: ${e.userMessageForAiCad()}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    } catch (e: Throwable) {
                                        Toast.makeText(
                                            context,
                                            "자동 저장 실패: ${e.userMessageForAiCad()}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        stlBusyMessageIndex = null
                                    }
                                }
                            }
                        }
                        is ClaudeChatClient.ChatResult.Error ->
                            errorMessage = result.message
                    }
                }
            }

            var lastHandled3dgsNonce by remember { mutableStateOf(-1L) }
            LaunchedEffect(pending3dgsServerAutoSend?.nonce) {
                val pending = pending3dgsServerAutoSend ?: return@LaunchedEffect
                if (!pending.switchToAiTab) return@LaunchedEffect
                if (pending.nonce <= lastHandled3dgsNonce) return@LaunchedEffect
                lastHandled3dgsNonce = pending.nonce
                aiTabMode = AiChatTabMode.MOBILE_3DGS
                messageText = pending.promptText
                attachedImages = pending.imageUris.toList()
                delay(400)
                doSend()
                onPending3dgsServerAutoSendConsumed()
            }

            // 입력 바 (GPT 스타일 pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 왼쪽 첨부 버튼 (원형)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.chatComposerPill)
                        .clickable { showImageSelectDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (aiTabMode == AiChatTabMode.AI_CAD)
                            Icons.Filled.Add else Icons.Filled.AddPhotoAlternate,
                        contentDescription = "첨부",
                        tint = if (attachedImages.isNotEmpty()) palette.brand
                               else palette.onBackground.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (aiTabMode == AiChatTabMode.MOBILE_3DGS) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.chatComposerPill)
                            .clickable { showJsonLibraryPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "JSON 라이브러리에서 첨부",
                            tint = if (attachedJsonUris.isNotEmpty()) palette.brand
                            else palette.onBackground.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Pill 입력 컨테이너
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(palette.chatComposerPill)
                        .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (messageText.isEmpty()) {
                                Text(
                                    text = when (aiTabMode) {
                                        AiChatTabMode.AI_CAD -> "모델·치수 입력"
                                        AiChatTabMode.MOBILE_3DGS -> "3DGS·COLMAP 질문 입력…"
                                        else -> "메시지 입력…"
                                    },
                                    color = palette.placeholder,
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                textStyle = TextStyle(
                                    color = palette.onBackground,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(palette.onBackground),
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // 전송 버튼 (pill 내부 오른쪽, 원형)
                        val canSend = !isStreaming && (
                            messageText.isNotBlank() || attachedImages.isNotEmpty() ||
                                (aiTabMode == AiChatTabMode.MOBILE_3DGS && attachedJsonUris.isNotEmpty())
                            )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) palette.brand else palette.chatComposerPillInactive
                                )
                                .clickable(enabled = canSend) { doSend() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isStreaming) {
                                CircularProgressIndicator(
                                    color = palette.onBackground,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "전송",
                                    tint = if (canSend) palette.onBrand else palette.onBackground.copy(alpha = 0.3f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showJsonLibraryPicker) {
            JsonLibraryPickerDialog(
                onDismiss = { showJsonLibraryPicker = false },
                onPickFile = { file ->
                    val uri = Uri.fromFile(file)
                    if (attachedJsonUris.none { it.path == uri.path }) {
                        if (attachedJsonUris.size >= 8) {
                            Toast.makeText(
                                context,
                                "JSON 첨부는 최대 8개까지입니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            attachedJsonUris = attachedJsonUris + uri
                        }
                    }
                    showJsonLibraryPicker = false
                }
            )
        }

        // 이미지 선택 다이얼로그 (갤러리·데이터셋 폴더)
        if (showImageSelectDialog) {
            ClaudeImageSelectDialog(
                galleryImages = galleryImageUris,
                datasetFolders = datasetFolders,
                onGalleryImageSelected = { uri ->
                    attachedImages = listOf(uri)
                    showImageSelectDialog = false
                },
                onDatasetFolderSelected = { folder ->
                    val imageExts = setOf("jpg", "jpeg", "png", "webp")
                    val uris = folder.dir.listFiles { f ->
                        f.isFile && imageExts.contains(f.extension.lowercase())
                    }?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
                        ?.map { Uri.fromFile(it) } ?: emptyList()
                    attachedImages = uris
                    showImageSelectDialog = false
                },
                onDismiss = { showImageSelectDialog = false }
            )
        }

        } // end inner Column

        // ── 드로어 스크림 ──
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { isDrawerOpen = false }
            )
        }

        // ── 드로어 패널 ──
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            ChatThreadDrawer(
                threads = allThreads,
                currentThreadId = currentThreadId,
                onThreadSelected = { thread ->
                    messages.clear()
                    messages.addAll(thread.messages.map { msg ->
                        ChatMessage(
                            text = msg.text,
                            isUser = msg.isUser,
                            imageUris = msg.imageUriStrings.mapNotNull { s ->
                                runCatching { android.net.Uri.parse(s) }.getOrNull()
                            },
                            jsonUris = (msg.jsonUriStrings ?: emptyList()).mapNotNull { s ->
                                runCatching { android.net.Uri.parse(s) }.getOrNull()
                            }
                        )
                    })
                    aiTabMode = when (thread.modeName) {
                        "AI_CAD" -> AiChatTabMode.AI_CAD
                        "MOBILE_3DGS" -> AiChatTabMode.MOBILE_3DGS
                        "GEMMA4_ON_DEVICE" -> AiChatTabMode.CLAUDE // 구버전 스레드 호환
                        else -> AiChatTabMode.CLAUDE
                    }
                    currentThreadId = thread.id
                    streamingText = ""
                    errorMessage = null
                    isDrawerOpen = false
                },
                onNewChat = {
                    messages.clear()
                    currentThreadId = null
                    streamingText = ""
                    errorMessage = null
                    isDrawerOpen = false
                },
                onDeleteThread = { threadId ->
                    ChatThreadStorage.delete(context, threadId)
                    if (currentThreadId == threadId) {
                        messages.clear()
                        currentThreadId = null
                    }
                    allThreads = ChatThreadStorage.loadAll(context)
                },
                onClose = { isDrawerOpen = false }
            )
        }

    } // end outer Box
}

@Composable
private fun ClaudeImageSelectDialog(
    galleryImages: List<Uri>,
    datasetFolders: List<DatasetFolder>,
    onGalleryImageSelected: (Uri) -> Unit,
    onDatasetFolderSelected: (DatasetFolder) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAppUiPalette.current
    var selectedTab by remember { mutableStateOf(0) } // 0: 갤러리, 1: 데이터셋(폴더)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .background(palette.dialogSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이미지 선택 (갤러리·데이터셋폴더)",
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
                            .clickable { onDismiss() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("갤러리 (${galleryImages.size})" to 0, "데이터셋폴더 (${datasetFolders.size})" to 1).forEach { (label, index) ->
                        val isSelected = selectedTab == index
                        Text(
                            text = label,
                            color = if (isSelected) palette.onBrand else palette.onBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) palette.brand else palette.onBackground.copy(alpha = 0.12f)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                when (selectedTab) {
                    0 -> {
                        if (galleryImages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "갤러리에 이미지가 없습니다.",
                                    color = palette.onBackgroundMuted,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(galleryImages) { uri ->
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onGalleryImageSelected(uri) }
                                            .border(1.dp, palette.divider, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (datasetFolders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "데이터셋폴더가 없습니다.",
                                    color = palette.onBackgroundMuted,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(datasetFolders) { folder ->
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, palette.divider, RoundedCornerShape(8.dp))
                                            .clickable { onDatasetFolderSelected(folder) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        ) {
                                            folder.coverUri?.let { uri ->
                                                Image(
                                                    painter = rememberAsyncImagePainter(uri),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = "${folder.name} (${folder.count}장)",
                                                    color = palette.onBackground,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
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
    }
}

@Composable
private fun JsonLibraryPickerDialog(
    onDismiss: () -> Unit,
    onPickFile: (File) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(Unit) {
        files = withContext(Dispatchers.IO) { JsonLibrary.listFilesSorted(context) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(palette.dialogSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JSON 라이브러리에서 선택",
                        color = palette.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = palette.onBackground)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "저장된 JSON이 없습니다.\n서버 파이프라인 완료 후 분석 JSON이 라이브러리에 저장됩니다.",
                            color = palette.onBackgroundMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files, key = { it.absolutePath }) { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.chatComposerPill)
                                    .clickable { onPickFile(f) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = palette.brand,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = f.name,
                                        color = palette.onBackground,
                                        fontSize = 14.sp,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "${f.length() / 1024} KB",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 11.sp
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

private enum class AiChatTabMode { CLAUDE, AI_CAD, MOBILE_3DGS }

// ─────────────────────────────────────────────────────────────
// 대화 스레드 드로어 UI
// ─────────────────────────────────────────────────────────────

@Composable
private fun ChatThreadDrawer(
    threads: List<ConversationThread>,
    currentThreadId: String?,
    onThreadSelected: (ConversationThread) -> Unit,
    onNewChat: () -> Unit,
    onDeleteThread: (String) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalAppUiPalette.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.82f)
            .background(palette.drawerPanelBg)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "채팅",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "닫기", tint = palette.onBackgroundMuted)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNewChat() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "새 채팅",
                tint = palette.brand,
                modifier = Modifier.size(18.dp)
            )
            Text("새 채팅", color = palette.onBackground, fontSize = 15.sp)
        }

        androidx.compose.material3.HorizontalDivider(color = palette.divider)

        if (threads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "아직 대화 기록이 없습니다.",
                    color = palette.onBackgroundMuted,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Text(
                "최근 채팅",
                color = palette.onBackgroundMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(threads, key = { it.id }) { thread ->
                    ThreadListItem(
                        thread = thread,
                        isActive = thread.id == currentThreadId,
                        onClick = { onThreadSelected(thread) },
                        onDelete = { onDeleteThread(thread.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreadListItem(
    thread: ConversationThread,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalAppUiPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) palette.threadRowActive else Color.Transparent)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = thread.title,
                color = palette.onBackground,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modeLabel = when (thread.modeName) {
                    "AI_CAD" -> "AI CAD"
                    "MOBILE_3DGS" -> "3DGS"
                    else -> "클로드"
                }
                val isAiCad = thread.modeName == "AI_CAD"
                val is3dgs = thread.modeName == "MOBILE_3DGS"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isAiCad -> palette.brand.copy(alpha = 0.15f)
                                is3dgs -> Color(0xFFE8A838).copy(alpha = 0.2f)
                                else -> Color(0xFF4A4AFF).copy(alpha = 0.18f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = modeLabel,
                        color = when {
                            isAiCad -> palette.brand
                            is3dgs -> Color(0xFFC9780A)
                            else -> Color(0xFF9898FF)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formatThreadTime(thread.updatedAt),
                    color = palette.onBackgroundMuted,
                    fontSize = 11.sp
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "삭제",
                tint = palette.onBackground.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatThreadTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "방금"
        diff < 3_600_000L -> "${diff / 60_000}분 전"
        diff < 86_400_000L -> "오늘"
        diff < 172_800_000L -> "어제"
        diff < 604_800_000L -> "${diff / 86_400_000}일 전"
        else -> {
            val cal = Calendar.getInstance().also { it.timeInMillis = timestamp }
            "${cal.get(Calendar.MONTH) + 1}월 ${cal.get(Calendar.DAY_OF_MONTH)}일"
        }
    }
}

private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageUris: List<Uri> = emptyList(),
    val jsonUris: List<Uri> = emptyList(),
)

/** 웹 LLM 스타일 OpenSCAD 창: 상단 저장, 본문 전체 스크롤(말줄임 없음) */
@Composable
private fun AiCadScriptWindowBubble(
    codeText: String,
    isConverting: Boolean,
    onSaveClick: () -> Unit
) {
    val palette = LocalAppUiPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = palette.aiCadWindowBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.aiCadWindowBorder),
                shadowElevation = 3.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.aiCadWindowHeader)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Color(0xFFFF5F57),
                                Color(0xFFFFBD2E),
                                Color(0xFF28C840)
                            ).forEach { dot ->
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(dot, CircleShape)
                                )
                            }
                            Text(
                                text = "openscad",
                                color = palette.brand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isConverting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = palette.brand,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "STL 변환 중…",
                                    color = palette.onBackground.copy(alpha = 0.88f),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            TextButton(
                                onClick = onSaveClick,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "저장",
                                    color = palette.brand,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (isConverting) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = palette.brand,
                            trackColor = palette.progressTrack
                        )
                    }
                    val codeScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(codeScroll)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = codeText,
                            color = palette.aiCadCodeText,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/** GPT 스타일 메시지 아이템: 사용자 = 우측 말풍선, AI = 말풍선 없이 마크다운 렌더링 */
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isStreaming: Boolean = false
) {
    val palette = LocalAppUiPalette.current
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                    .background(palette.surfaceCardAlt)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (message.imageUris.isNotEmpty()) {
                        val showUris = message.imageUris.take(6)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            showUris.chunked(3).forEach { rowUris ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    rowUris.forEach { uri ->
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            if (message.imageUris.size > 6) {
                                Text(
                                    text = "외 ${message.imageUris.size - 6}장",
                                    color = palette.onSurfaceCard.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (message.jsonUris.isNotEmpty() || message.text.isNotEmpty()) Spacer(Modifier.height(6.dp))
                    }
                    if (message.jsonUris.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            message.jsonUris.take(6).forEach { ju ->
                                Surface(
                                    color = palette.onBackground.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = null,
                                            tint = palette.brand,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = ju.lastPathSegment ?: "json",
                                            color = palette.onSurfaceCard,
                                            fontSize = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            if (message.jsonUris.size > 6) {
                                Text(
                                    text = "외 JSON ${message.jsonUris.size - 6}개",
                                    color = palette.onSurfaceCard.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (message.text.isNotEmpty()) Spacer(Modifier.height(6.dp))
                    }
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            color = palette.onSurfaceCard,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(end = 16.dp)) {
            if (message.imageUris.isNotEmpty()) {
                val showUris = message.imageUris.take(6)
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    showUris.chunked(3).forEach { rowUris ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowUris.forEach { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
            val displayText = when {
                isStreaming && message.text.isEmpty() -> "▊"
                isStreaming -> message.text + " ▊"
                else -> message.text
            }
            MarkdownText(text = displayText)
        }
    }
}

// ─────────────────────── 마크다운 렌더러 ───────────────────────

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String, val depth: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    object HRule : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 코드 블록
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // 닫는 ``` 건너뜀
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        // 제목
        val headingMatch = Regex("^(#{1,3}) (.+)").matchEntire(trimmed)
        if (headingMatch != null) {
            blocks.add(MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++; continue
        }

        // 불릿
        val bulletMatch = Regex("^([ \t]*)[-*+] (.+)").matchEntire(line)
        if (bulletMatch != null) {
            val depth = bulletMatch.groupValues[1].length / 2
            blocks.add(MarkdownBlock.BulletItem(bulletMatch.groupValues[2], depth))
            i++; continue
        }

        // 번호 목록
        val numMatch = Regex("^\\s*(\\d+)[.)\\s] (.+)").matchEntire(trimmed)
        if (numMatch != null) {
            blocks.add(MarkdownBlock.NumberedItem(numMatch.groupValues[1].toIntOrNull() ?: 1, numMatch.groupValues[2]))
            i++; continue
        }

        // 수평선
        if (trimmed.matches(Regex("^[-*_]{3,}$"))) {
            blocks.add(MarkdownBlock.HRule); i++; continue
        }

        // 빈 줄
        if (trimmed.isEmpty()) { i++; continue }

        // 단락 (연속 줄 묶음)
        val paraLines = mutableListOf(line)
        i++
        while (i < lines.size) {
            val next = lines[i]; val nt = next.trim()
            if (nt.isEmpty() || nt.startsWith("```") || Regex("^#{1,3} ").containsMatchIn(nt)
                || Regex("^[-*+] ").containsMatchIn(nt) || Regex("^\\d+[.)\\s]").containsMatchIn(nt)
                || nt.matches(Regex("^[-*_]{3,}$"))) break
            paraLines.add(next); i++
        }
        blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
    }
    return blocks
}

/** 인라인 마크다운 파싱 (Bold, Italic, InlineCode) → AnnotatedString */
private fun buildInlineAnnotated(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // Bold+Italic: ***
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 3
                } else append(text[i++])
            }
            // Bold: **
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else append(text[i++])
            }
            // Italic: _text_
            text.startsWith("_", i) && (i == 0 || text[i - 1] != '_') -> {
                val end = text.indexOf("_", i + 1)
                if (end != -1 && (end + 1 >= text.length || text[end + 1] != '_')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else append(text[i++])
            }
            // Inline code: `
            text.startsWith("`", i) && !text.startsWith("```", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFF2A2A2A),
                        color = Color(0xFF9CD83B),
                        fontSize = 13.sp
                    )) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else append(text[i++])
            }
            else -> append(text[i++])
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    fontSize: TextUnit = 15.sp
) {
    val palette = LocalAppUiPalette.current
    val resolvedColor = textColor ?: palette.markdownDefault
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingSize: TextUnit = when (block.level) {
                        1 -> (fontSize.value + 7).sp
                        2 -> (fontSize.value + 4).sp
                        else -> (fontSize.value + 2).sp
                    }
                    val headingWeight = if (block.level <= 2) FontWeight.Bold else FontWeight.SemiBold
                    val headingTopPad = when (block.level) { 1 -> 10.dp; 2 -> 8.dp; else -> 6.dp }
                    Text(
                        text = buildInlineAnnotated(block.text),
                        color = resolvedColor,
                        fontSize = headingSize,
                        fontWeight = headingWeight,
                        lineHeight = (headingSize.value * 1.3).sp,
                        modifier = Modifier.padding(top = headingTopPad, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(modifier = Modifier.padding(start = (block.depth * 14).dp)) {
                        Text(
                            "•",
                            color = resolvedColor.copy(alpha = 0.55f),
                            fontSize = fontSize,
                            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                        )
                        Text(
                            text = buildInlineAnnotated(block.text),
                            color = resolvedColor,
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.5).sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row {
                        Text(
                            "${block.number}.",
                            color = resolvedColor.copy(alpha = 0.55f),
                            fontSize = fontSize,
                            modifier = Modifier.width(26.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = buildInlineAnnotated(block.text),
                            color = resolvedColor,
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.5).sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    MarkdownCodeBlock(language = block.language, code = block.code)
                }
                is MarkdownBlock.HRule -> {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = resolvedColor.copy(alpha = 0.18f)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineAnnotated(block.text),
                        color = resolvedColor,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.6).sp
                    )
                }
            }
        }
    }
}

/** 마크다운 코드 블록 (AI CAD 전용 AiCadScriptWindowBubble과 별개) */
@Composable
private fun MarkdownCodeBlock(language: String, code: String) {
    val palette = LocalAppUiPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.codeBlockBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.codeBlockBorder)
    ) {
        Column {
            if (language.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.codeBlockHeaderBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = language,
                        color = palette.codeBlockMeta,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            val scroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    color = palette.aiCadCodeText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    softWrap = false
                )
            }
        }
    }
}
