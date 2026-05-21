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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.window.DialogProperties
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
import java.io.InputStream
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
private const val MAX_3DGS_AUX_ATTACHMENTS = 12

private const val GLTF_MAGIC = 0x46546C67

/** 시각 자료·분석 문단 같이 쓰기 — 모바일 PDF는 문자열만 추출하므로 그림 설명은 반드시 add_paragraph에 병기 */
private const val REPORT_IMAGE_WORKFLOW_HINT =
    "보고서에 도식·비교 그림·파손 개요 도면 등 시각 자료가 필요하면 다음 순서를 따르세요: " +
        "(1) 분석 관점에서 어떤 그림이 필요한지 먼저 정한 뒤, 스크립트 안에서 matplotlib 또는 PIL로 PNG 파일을 생성합니다. " +
        "(2) 같은 스크립트에서 `document.add_picture(생성된_png_경로, width=Inches(...))` 등으로 본문 적절한 위치에 삽입합니다(PC에서 python-docx 실행 시 Word에 포함). " +
        "(3) 바로 이어서 `add_paragraph()`로 해당 그림을 설명·해석하는 한국어 분석 텍스트를 작성합니다. " +
        "모바일 앱은 주로 add_paragraph·add_heading 문자열만 PDF로 추출하므로, 그림의 핵심 내용·결론은 반드시 인접 문단에 텍스트로도 남겨 주세요. " +
        "앱에서 인식하지 않는 '차트:bar' 등 표식은 사용하지 마세요."

/** 파손 분석 모드: 텍스트가 비어 있고 이미지만 보낼 때 LLM에 넣는 기본 요청 */
private const val DAMAGE_ANALYSIS_DEFAULT_PROMPT =
    "첨부된 사고 차량 사진을 종합 분석하여, 한국어 python-docx 스크립트(단일 ```python 블록)를 출력하세요. " +
        REPORT_IMAGE_WORKFLOW_HINT + " " +
        "이미지 개별 설명은 절대 금지 — 모든 사진을 하나의 통합 차량 파손 데이터셋으로 분석합니다. " +
        "보고서는 다음 구조로 작성합니다: " +
        "【제1페이지: 표지】 보고서 제목·부제·생성일시·작성도구·면책 문구 포함. " +
        "【제2페이지: 목차】 add_paragraph()로 각 항목 표시 (표 사용 금지). " +
        "【제3페이지 이후: 본문】 아래 섹션을 각각 page_break로 분리: " +
        "(1) 차량 모델 정보 — 브랜드·차급·모델·연식·색상·번호판 유추 (간결하게), " +
        "(2) 사고 발생 형태 분석 — 충돌 유형·방향·접촉 지점·2차 피해, " +
        "(3) 【핵심】파손 부위 정리 — 모든 가시 파손 부위를 개별 add_paragraph()로 정리, " +
        "각 항목: [파손 부위 | 파손 유형 | 심각도 | 파손 깊이(추정) | 파손 면적(추정) | 비고], " +
        "(4) 파손 깊이 상세 분석 — add_paragraph()로 각 부위별 깊이·변형·영향·측정 한계 기록, " +
        "(5) 【핵심】수리 예상 견적 — add_paragraph()로 각 부위별 수리 방법·공임·부품비·총비용·기간 기록, " +
        "(6) 사고 원인 추론 — add_paragraph()로 [추론 항목 | 관찰 근거 | 신뢰도] (간결하게), " +
        "(7) 법적 면책 정보 — 면책 문구와 보고서 생성 시각 포함. " +
        "모든 데이터는 add_paragraph()로만 표현하세요 (표·table.cell 사용 금지). " +
        "차트(차트:bar, 차트:pie, 차트:line)도 사용하지 마세요. " +
        "\"이미지 1에서는\", \"사진에서 보이듯\" 등 개별 사진 언급 표현은 절대 사용하지 말고, " +
        "차량 전체에 대한 통합 파손 분석 보고서로 작성하세요."

private fun uriFileExtension(uri: Uri): String {
    val name = uri.lastPathSegment?.substringAfterLast('/', missingDelimiterValue = "") ?: ""
    val dot = name.lastIndexOf('.')
    return if (dot >= 0) name.substring(dot + 1).lowercase(Locale.getDefault()) else ""
}

private fun Uri.lengthBytesOrNull(context: Context): Long? {
    return try {
        when (scheme) {
            "file" -> {
                val p = path ?: return null
                val f = File(p)
                if (f.isFile) f.length() else null
            }
            else -> {
                val len = context.contentResolver.openAssetFileDescriptor(this, "r")?.use { it.length }
                if (len != null && len >= 0) len else null
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun Uri.looksLikeRasterImagePath(): Boolean {
    val pl = path?.lowercase(Locale.getDefault()) ?: return false
    return pl.endsWith(".png") || pl.endsWith(".jpg") || pl.endsWith(".jpeg") ||
        pl.endsWith(".webp") || pl.endsWith(".heic") || pl.endsWith(".heif")
}

private fun Uri.isChatPickVideoUri(): Boolean {
    val pl = path?.lowercase(Locale.getDefault()) ?: return false
    return pl.endsWith(".mp4") || pl.endsWith(".mov") || pl.endsWith(".webm")
}

private fun Uri.isLikelyImageOrVideoUri(context: Context): Boolean {
    if (isChatPickVideoUri()) return true
    val pl = path?.lowercase(Locale.getDefault()) ?: ""
    if (pl.endsWith(".jpg") || pl.endsWith(".jpeg") || pl.endsWith(".png") ||
        pl.endsWith(".webp") || pl.endsWith(".heic") || pl.endsWith(".heif")
    ) {
        return true
    }
    val t = context.contentResolver.getType(this)?.lowercase(Locale.getDefault()) ?: ""
    return t.startsWith("image/") || t.startsWith("video/")
}

/** 이미지·동영상 URI와 그 외(JSON·PLY·GLB·ZIP 등) 첨부 URI 분리 */
private fun partitionMediaAndAuxUris(context: Context, uris: List<Uri>): Pair<List<Uri>, List<Uri>> {
    val media = ArrayList<Uri>()
    val aux = ArrayList<Uri>()
    val seenM = HashSet<String>()
    val seenA = HashSet<String>()
    for (u in uris) {
        val k = u.toString()
        if (u.isLikelyImageOrVideoUri(context)) {
            if (seenM.add(k)) media.add(u)
        } else {
            if (seenA.add(k)) aux.add(u)
        }
    }
    return media to aux
}

private fun readPlyHeaderExcerpt(inp: InputStream, maxLinesAfterHeader: Int = 0): String {
    val reader = inp.bufferedReader(Charsets.US_ASCII)
    val sb = StringBuilder()
    var pastHeader = false
    var extra = 0
    while (true) {
        val line = reader.readLine() ?: break
        sb.appendLine(line)
        if (line.trim() == "end_header") {
            pastHeader = true
            if (maxLinesAfterHeader <= 0) break
        } else if (pastHeader) {
            extra++
            if (extra >= maxLinesAfterHeader) break
        }
        if (sb.length > 24_000) {
            sb.appendLine("...(PLY 헤더/본문 발췌 상한)")
            break
        }
    }
    return sb.toString()
}

private fun buildOne3dgsAppendixBlock(context: Context, uri: Uri): String? {
    val label = uri.lastPathSegment ?: uri.toString()
    val ext = uriFileExtension(uri)
    val len = uri.lengthBytesOrNull(context)
    return when (ext) {
        "json" -> {
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
            if (raw.isEmpty()) return null
            val chunk = if (raw.length > MAX_JSON_APPENDIX_PER_FILE_CHARS) {
                raw.take(MAX_JSON_APPENDIX_PER_FILE_CHARS) + "\n...(파일 길이로 인해 잘림)"
            } else raw
            "### $label (JSON)\n${chunk}"
        }
        "ply" -> {
            val header = try {
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    readPlyHeaderExcerpt(inp)
                } ?: ""
            } catch (_: Exception) {
                ""
            }
            if (header.isEmpty()) return null
            val sizeStr = len?.toString() ?: "알 수 없음"
            "### $label (PLY)\n크기(bytes): $sizeStr\n--- ASCII 헤더 발췌 ---\n$header"
        }
        "glb" -> {
            val summary = try {
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    val hdr = ByteArray(12)
                    if (inp.read(hdr) < 12) return@use "(GLB 헤더 12바이트 미만)"
                    val bb = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
                    val magic = bb.int
                    val ver = bb.int
                    val declared = bb.int
                    val magicOk = magic == GLTF_MAGIC
                    "magic glTF: $magicOk, version: $ver, declared total length: $declared bytes"
                } ?: ""
            } catch (e: Exception) {
                "(GLB 요약 실패: ${e.message})"
            }
            val sizeStr = len?.toString() ?: "알 수 없음"
            "### $label (GLB)\n크기(bytes): $sizeStr\n$summary"
        }
        "zip" -> {
            val listing = try {
                context.contentResolver.openInputStream(uri)?.use { rawIn ->
                    ZipInputStream(rawIn).use { zis ->
                        val names = ArrayList<String>()
                        while (names.size < 200) {
                            val e = zis.nextEntry ?: break
                            if (!e.isDirectory) names.add(e.name)
                            zis.closeEntry()
                        }
                        buildString {
                            appendLine(names.joinToString("\n"))
                            if (names.size >= 200) appendLine("...(ZIP 항목 최대 200개만 표시)")
                        }.trimEnd()
                    }
                } ?: ""
            } catch (e: Exception) {
                "(ZIP 목록 실패: ${e.message})"
            }
            if (listing.isEmpty()) return null
            val sizeStr = len?.toString() ?: "알 수 없음"
            "### $label (ZIP)\n크기(bytes): $sizeStr\n--- 엔트리(일부) ---\n$listing"
        }
        else -> {
            val maxProbe = 120_000L
            if (len != null && len > maxProbe) {
                "### $label ($ext)\n크기(bytes): $len\n(바이너리·대용량으로 간주, 본문 생략)"
            } else {
                val raw = try {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()
                        ?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    ""
                }
                if (raw.isEmpty()) {
                    val sz = len?.toString() ?: "?"
                    "### $label ($ext)\n크기(bytes): $sz\n(UTF-8 텍스트로 읽지 못함)"
                } else {
                    val chunk = if (raw.length > MAX_JSON_APPENDIX_PER_FILE_CHARS) {
                        raw.take(MAX_JSON_APPENDIX_PER_FILE_CHARS) + "\n...(잘림)"
                    } else raw
                    "### $label ($ext, 텍스트로 추정)\n$chunk"
                }
            }
        }
    }
}

private suspend fun read3dgsDataAppendixForLlm(context: Context, uris: List<Uri>): String =
    withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext ""
        val sb = StringBuilder()
        for (uri in uris) {
            if (sb.length >= MAX_JSON_APPENDIX_TOTAL_CHARS) break
            val block = buildOne3dgsAppendixBlock(context, uri) ?: continue
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(block)
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
    /** 서버 파이프라인 결과 폴더가 바뀔 때마다 증가시켜 3DGS 이미지 피커 목록을 갱신합니다. */
    serverArtifactLibraryVersion: Int = 0,
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
    /** 3DGS: JSON·PLY·GLB·ZIP 등 LLM 텍스트 부록용 (비이미지) */
    var attachedAuxUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imagePickerSession by remember { mutableIntStateOf(0) }
    var serverDa3RasterUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var serverDa3QualityJsonUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var aiTabMode by remember { mutableStateOf(AiChatTabMode.CLAUDE) }
    var aiCadOption by remember { mutableStateOf(ClaudeChatClient.AiCadInputOption.DIMENSIONS_DIRECT) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var stlDialogForIndex by remember { mutableStateOf<Int?>(null) }
    var stlSaveNameInput by remember { mutableStateOf("") }
    var stlBusyMessageIndex by remember { mutableStateOf<Int?>(null) }
    var docxBusyMessageIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    // 스레드 관리 상태
    var currentThreadId by remember { mutableStateOf<String?>(null) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var allThreads by remember { mutableStateOf<List<ConversationThread>>(emptyList()) }

    LaunchedEffect(stlDialogForIndex) {
        if (stlDialogForIndex != null) stlSaveNameInput = ""
    }

    val galleryImageUris = remember(galleryImages) {
        galleryImages.filter { uri ->
            val path = uri.path ?: ""
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) ||
                path.endsWith(".png", true) || path.endsWith(".webp", true) ||
                path.endsWith(".heic", true) || path.endsWith(".heif", true)
        }
    }

    /** 3DGS·파손 분석: 갤러리 이미지+동영상. 그 외 모드: 이미지만 */
    val galleryForImagePicker = remember(galleryImages, aiTabMode) {
        if (aiTabMode == AiChatTabMode.MOBILE_3DGS || aiTabMode == AiChatTabMode.DAMAGE_ANALYSIS) {
            galleryImages.filter { uri ->
                val pl = uri.path?.lowercase(Locale.getDefault()) ?: ""
                pl.endsWith(".jpg") || pl.endsWith(".jpeg") || pl.endsWith(".png") ||
                    pl.endsWith(".webp") || pl.endsWith(".heic") || pl.endsWith(".heif") ||
                    pl.endsWith(".mp4") || pl.endsWith(".mov") || pl.endsWith(".webm")
            }
        } else {
            galleryImageUris
        }
    }

    LaunchedEffect(Unit) {
        loadDatasetFolders(context) { datasetFolders = it }
        allThreads = ChatThreadStorage.loadAll(context)
    }

    LaunchedEffect(serverArtifactLibraryVersion) {
        val pair = withContext(Dispatchers.IO) {
            val infos = scanServerTaskManifestInfos(context)
            da3MergedRasterUrisForAiPicker(infos) to da3QualityJsonUrisForServerTasks(infos)
        }
        serverDa3RasterUris = pair.first
        serverDa3QualityJsonUris = pair.second
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
        Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isDrawerOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "메뉴",
                    tint = palette.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

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
                            AiChatTabMode.MOBILE_3DGS -> "사고 현장 분석"
                            AiChatTabMode.DAMAGE_ANALYSIS -> "파손부위 분석"
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
                            Text(
                                "클로드 AI LLM",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.CLAUDE
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "AI CAD",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.AI_CAD
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "사고 현장 분석",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.MOBILE_3DGS
                            modeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "파손부위 분석",
                                color = palette.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            aiTabMode = AiChatTabMode.DAMAGE_ANALYSIS
                            modeMenuExpanded = false
                        }
                    )
                }
            }

            IconButton(onClick = {
                messages.clear()
                currentThreadId = null
                streamingText = ""
                errorMessage = null
                attachedImages = emptyList()
                attachedAuxUris = emptyList()
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
                    val threeDgsExport =
                        if (!msg.isUser && (
                            aiTabMode == AiChatTabMode.MOBILE_3DGS ||
                                aiTabMode == AiChatTabMode.DAMAGE_ANALYSIS
                            )
                        ) {
                        MarkdownThreeDgsExport(
                            isExporting = docxBusyMessageIndex == index,
                            onExport = {
                                scope.launch {
                                    docxBusyMessageIndex = index
                                    val docxResult = withContext(Dispatchers.IO) {
                                        when (aiTabMode) {
                                            AiChatTabMode.DAMAGE_ANALYSIS ->
                                                ThreeDgsChatPdfExport.tryExportToPdf(
                                                    context,
                                                    msg.text,
                                                    subdirectory = "damage_llm_exports",
                                                    fileBasePrefix = "damage_report",
                                                    docTitle = "교통사고 파손·부위 분석 보고서",
                                                )
                                            else ->
                                                ThreeDgsChatPdfExport.tryExportToPdf(context, msg.text)
                                        }
                                    }
                                    docxBusyMessageIndex = null
                                    if (docxResult == null) {
                                        Toast.makeText(
                                            context,
                                            "python 코드 블록이 없거나 저장에 실패했습니다.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        if (docxResult.extractedPieceCount == 0) {
                                            Toast.makeText(
                                                context,
                                                "스크립트에서 문단 문자열을 찾지 못했습니다. .py·.pdf는 저장되었습니다.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "저장: ${docxResult.pdfFile.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        openPdf(context, docxResult.pdfFile)
                                    }
                                }
                            }
                        )
                    } else {
                        null
                    }
                    ChatMessageItem(message = msg, threeDgsExport = threeDgsExport)
                }
            }
            // 스트리밍 중: 실시간으로 들어오는 텍스트 표시
            if (isStreaming) {
                item(key = "streaming") {
                    ChatMessageItem(
                        message = ChatMessage(text = streamingText, isUser = false),
                        isStreaming = true,
                        threeDgsExport = null,
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
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = palette.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

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


            // 전송 로직 — 스트리밍 방식 (토큰 단위 onDelta 콜백)
            val doSend: () -> Unit = sendLambda@{
                if (isStreaming) return@sendLambda
                val text = messageText.trim()
                val images = attachedImages
                val imagesForLlmRaw = if (
                    aiTabMode == AiChatTabMode.MOBILE_3DGS ||
                        aiTabMode == AiChatTabMode.DAMAGE_ANALYSIS
                ) {
                    images.filter { !it.isChatPickVideoUri() }
                } else {
                    images
                }
                val imagesForLlm = if (imagesForLlmRaw.size > MAX_LLM_VISION_IMAGES_PER_REQUEST) {
                    Toast.makeText(
                        context,
                        "API 전송 용량 제한으로 사진 ${imagesForLlmRaw.size}장 중 ${MAX_LLM_VISION_IMAGES_PER_REQUEST}장만 보냅니다. (전체 구간에서 고르게 선택)",
                        Toast.LENGTH_LONG,
                    ).show()
                    evenlySampleListForLlm(imagesForLlmRaw, MAX_LLM_VISION_IMAGES_PER_REQUEST)
                } else {
                    imagesForLlmRaw
                }
                val rawAuxSnap =
                    if (aiTabMode == AiChatTabMode.MOBILE_3DGS) attachedAuxUris else emptyList()
                val auxSnap = if (rawAuxSnap.size > MAX_3DGS_AUX_ATTACHMENTS) {
                    Toast.makeText(
                        context,
                        "데이터 파일(JSON·PLY·ZIP 등)은 최대 ${MAX_3DGS_AUX_ATTACHMENTS}개까지 전송됩니다.",
                        Toast.LENGTH_LONG,
                    ).show()
                    rawAuxSnap.take(MAX_3DGS_AUX_ATTACHMENTS)
                } else rawAuxSnap
                if (text.isEmpty() && imagesForLlm.isEmpty() && auxSnap.isEmpty()) {
                    if (images.isNotEmpty() && (
                        aiTabMode == AiChatTabMode.MOBILE_3DGS ||
                            aiTabMode == AiChatTabMode.DAMAGE_ANALYSIS
                        )
                    ) {
                        Toast.makeText(
                            context,
                            "동영상만 선택되었습니다. 이미지를 첨부하세요.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@sendLambda
                }

                val userBubbleText = when {
                    text.isNotEmpty() -> text
                    images.size > 1 -> "[${images.size}개 미디어]"
                    images.size == 1 -> if (images.single().isChatPickVideoUri()) "[동영상 1개]" else "[이미지 1장]"
                    auxSnap.isNotEmpty() && images.isEmpty() -> "[데이터 파일 ${auxSnap.size}개]"
                    auxSnap.isNotEmpty() -> "[미디어 ${images.size}개·데이터 파일 ${auxSnap.size}개]"
                    else -> ""
                }

                messages.add(
                    ChatMessage(
                        text = userBubbleText,
                        isUser = true,
                        imageUris = images,
                        jsonUris = auxSnap
                    )
                )
                messageText = ""
                attachedImages = emptyList()
                attachedAuxUris = emptyList()
                isStreaming = true
                streamingText = ""
                errorMessage = null

                scope.launch {
                    val imageBase64List = imagesForLlm.mapNotNull { uri ->
                        var bitmap: android.graphics.Bitmap? = null
                        try {
                            bitmap = decodeBitmapWithMaxDimension(context, uri, 1280)
                            bitmap?.let { ClaudeChatClient.bitmapToBase64ForLlm(it) }
                        } catch (e: Exception) {
                            null
                        } finally {
                            bitmap?.let { bmp ->
                                try { if (!bmp.isRecycled) bmp.recycle() } catch (_: Exception) {}
                            }
                        }
                    }
                    val dataAppendix = if (aiTabMode == AiChatTabMode.MOBILE_3DGS && auxSnap.isNotEmpty()) {
                        read3dgsDataAppendixForLlm(context, auxSnap)
                    } else ""
                    val defaultImgPrompt = when (aiTabMode) {
                        AiChatTabMode.MOBILE_3DGS -> when {
                            imageBase64List.size > 1 ->
                                "첨부된 여러 사고 현장 이미지를 근거로, 한국어 python-docx 스크립트(단일 ```python 블록)를 출력하세요. " +
                                    REPORT_IMAGE_WORKFLOW_HINT + " " +
                                    "보고서는 반드시 표지(1페이지)·목차(2페이지)·본문(3페이지 이후) 구조로 작성하고, 다음을 포함하세요: 사고 현장 개요, 사고 발생 형태 분석, 사고 원인 추론, 차량별 파손 부위 및 수리 견적, 종합 수리 견적 요약, 법적 면책 정보. 모든 데이터는 add_paragraph()로만 표현하고 table.cell은 절대 사용하지 마세요. 앱 전용 차트 표식은 사용하지 마세요. 섹션마다 page_break로 분리하세요. 이미지별 개별 설명이 아닌 종합 분석을 제공하세요."
                            imageBase64List.size == 1 ->
                                "첨부된 사고 현장 이미지를 근거로, 한국어 python-docx 스크립트(단일 ```python 블록)를 출력하세요. " +
                                    REPORT_IMAGE_WORKFLOW_HINT + " " +
                                    "보고서는 표지·목차·본문 구조로 작성하고, 사고 현장 개요, 사고 형태 분석, 원인 추론, 차량 파손 및 수리 견적, 법적 면책 정보를 포함하세요. 모든 데이터는 add_paragraph()로만 표현하고 table.cell은 절대 사용하지 마세요. 섹션마다 page_break로 분리하세요."
                            else -> ""
                        }
                        AiChatTabMode.DAMAGE_ANALYSIS -> when {
                            imageBase64List.isNotEmpty() -> DAMAGE_ANALYSIS_DEFAULT_PROMPT
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
                                    dataAppendix.isNotBlank() ->
                                        "첨부 데이터 파일(JSON·PLY/GLB·ZIP 등)을 반영해 사고 현장 분석 보고서를 작성하세요. " +
                                            REPORT_IMAGE_WORKFLOW_HINT + " " +
                                            "보고서는 표지(1페이지)·목차(2페이지)·본문(3페이지 이후) 구조로, 사고 현장 개요·사고 형태 분석·원인 추론·차량별 파손 부위 및 수리 견적·종합 수리 견적 요약·법적 면책 정보를 포함하세요. 모든 데이터는 add_paragraph()로만 표현하고 table.cell과 앱 전용 차트 표식은 사용하지 마세요. 섹션마다 page_break로 분리하고, 한국어 python-docx 스크립트(단일 ```python 블록)로만 출력하세요."
                                    else ->
                                        "이 앱의 Mobile 3DGS 사고 현장 분석 시스템을 위한 보고서 샘플을 작성하세요. " +
                                            REPORT_IMAGE_WORKFLOW_HINT + " " +
                                            "보고서는 표지·목차·본문 구조로, 사고 현장 개요·사고 형태 분석·원인 추론·차량 파손 및 수리 견적·종합 수리 견적 요약·법적 면책 정보를 포함한 한국어 python-docx 스크립트(단일 ```python 블록)로 출력하세요. 모든 데이터는 add_paragraph()로만 표현하고 table.cell과 앱 전용 차트 표식은 사용하지 마세요. 섹션마다 page_break로 분리하세요."
                                }
                            }
                            val fullText = if (dataAppendix.isNotBlank()) {
                                basePrompt + "\n\n--- 첨부 데이터 파일 요약(JSON·PLY·GLB·ZIP 등) ---\n" + dataAppendix
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
                        AiChatTabMode.DAMAGE_ANALYSIS -> {
                            val basePrompt = text.ifBlank {
                                if (imageBase64List.isNotEmpty()) {
                                    DAMAGE_ANALYSIS_DEFAULT_PROMPT
                                } else {
                                    "사고 차량 사진을 종합 분석하여 표지·목차·본문 구조의 차량 파손 분석 보고서를 한국어 python-docx 스크립트(단일 ```python 블록)로 출력하세요. " +
                                        REPORT_IMAGE_WORKFLOW_HINT + " " +
                                        "이미지 개별 언급 없이, 파손 부위별 정리를 add_paragraph()로 하고, 차량 모델 정보·사고 형태 분석·원인 추론·수리 예상 견적·법적 면책 정보를 포함하세요. 모든 데이터는 add_paragraph()로만 표현하고 table.cell과 앱 전용 차트 표식은 사용하지 마세요. 섹션마다 page_break로 분리하세요."
                                }
                            }
                            ClaudeChatClient.streamDamageAnalysisReportMessage(
                                userText = basePrompt,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.chatComposerPill)
                        .clickable {
                            imagePickerSession++
                            showImageSelectDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (aiTabMode == AiChatTabMode.AI_CAD)
                            Icons.Filled.Add else Icons.Filled.AddPhotoAlternate,
                        contentDescription = "첨부",
                        tint = if (attachedImages.isNotEmpty() ||
                            (aiTabMode == AiChatTabMode.MOBILE_3DGS && attachedAuxUris.isNotEmpty())
                        ) {
                            palette.brand
                        } else {
                            palette.onBackground.copy(alpha = 0.85f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

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
                                        AiChatTabMode.MOBILE_3DGS -> "Word 보고서 스크립트 요청 또는 맥락 입력…"
                                        AiChatTabMode.DAMAGE_ANALYSIS -> "파손 보고서 스크립트 요청 또는 맥락 입력…"
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
                        val canSend = !isStreaming && (
                            messageText.isNotBlank() || attachedImages.isNotEmpty() ||
                                (aiTabMode == AiChatTabMode.MOBILE_3DGS && attachedAuxUris.isNotEmpty())
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

        if (showImageSelectDialog) {
            ClaudeImageSelectDialog(
                dialogSession = imagePickerSession,
                initialSelection = attachedImages + attachedAuxUris,
                galleryMedia = galleryForImagePicker,
                datasetFolders = datasetFolders,
                includeServerLibraryTabs = aiTabMode == AiChatTabMode.MOBILE_3DGS ||
                    aiTabMode == AiChatTabMode.DAMAGE_ANALYSIS,
                include3dgsFileLibraryTabs = aiTabMode == AiChatTabMode.MOBILE_3DGS,
                serverDa3RasterUris = serverDa3RasterUris,
                serverDa3QualityJsonUris = serverDa3QualityJsonUris,
                onPickMedia = { uris ->
                    val (media, aux) = partitionMediaAndAuxUris(context, uris)
                    val auxTrim = if (aux.size > MAX_3DGS_AUX_ATTACHMENTS) {
                        Toast.makeText(
                            context,
                            "데이터 파일(JSON·PLY 등)은 최대 ${MAX_3DGS_AUX_ATTACHMENTS}개까지 첨부됩니다.",
                            Toast.LENGTH_LONG,
                        ).show()
                        aux.take(MAX_3DGS_AUX_ATTACHMENTS)
                    } else aux
                    attachedImages = media
                    attachedAuxUris = auxTrim
                    showImageSelectDialog = false
                },
                onDismiss = { showImageSelectDialog = false }
            )
        }

        } // end inner Column

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
                        "DAMAGE_ANALYSIS" -> AiChatTabMode.DAMAGE_ANALYSIS
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

private enum class ClaudeImageLibraryTab {
    GALLERY,
    DATASET,
    GS_ANALYSIS,
    GS_QUALITY_EVAL,
    JSON_LIBRARY,
    MODEL_3D,
    ARCORE_LIBRARY,
    DEVICE_FILES,
}

private fun scanModel3dLibraryFiles(context: Context): List<File> {
    val root = ModelLibraryPaths.plyDir(context)
    if (!root.isDirectory) return emptyList()
    return root.walkTopDown()
        .maxDepth(6)
        .filter { it.isFile }
        .filter { f ->
            val e = f.extension.lowercase(Locale.getDefault())
            e == "ply" || e == "glb"
        }
        .sortedByDescending { it.lastModified() }
        .take(500)
        .toList()
}

private fun scanArcoreAttachFiles(context: Context): List<File> {
    return ArcoreLibrary.listFilesSorted(context).filter { f ->
        val e = f.extension.lowercase(Locale.getDefault())
        e == "zip" || e == "json" || e == "glb"
    }
}

private fun datasetFolderMediaUrisForPicker(folder: DatasetFolder): List<Uri> {
    val exts = setOf("jpg", "jpeg", "png", "webp", "mp4", "mov", "webm")
    return folder.dir.listFiles { f ->
        f.isFile && exts.contains(f.extension.lowercase(Locale.getDefault()))
    }?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
        ?.map { Uri.fromFile(it) } ?: emptyList()
}

@Composable
private fun ChatPickMediaThumbnail(
    uri: Uri,
    palette: AppUiPalette,
    restrictVideoForLlm: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val thumbPx = rememberGalleryGridThumbEdgePx(columns = 3)
    val isVid = remember(uri) { uri.isChatPickVideoUri() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (restrictVideoForLlm && isVid) {
                    Toast.makeText(
                        context,
                        "동영상은 이 모드에서 LLM에 전송되지 않습니다. 이미지를 선택하세요.",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    onToggle()
                }
            }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) palette.brand else palette.divider,
                shape = RoundedCornerShape(8.dp),
            ),
    ) {
        if (isVid) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            Image(
                painter = rememberGalleryGridPhotoPainter(uri, thumbPx),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (isSelected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(palette.brand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = palette.onBrand,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatPickAuxFileRow(
    file: File,
    palette: AppUiPalette,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) palette.brand.copy(alpha = 0.18f)
                else palette.chatComposerPill,
            )
            .border(
                1.dp,
                if (isSelected) palette.brand else palette.divider,
                RoundedCornerShape(10.dp),
            )
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = palette.brand,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = palette.onBackground,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${file.length() / 1024} KB · ${file.extension.uppercase(Locale.getDefault())}",
                color = palette.onBackgroundMuted,
                fontSize = 11.sp,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = palette.brand,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ClaudeImageSelectDialog(
    /** 다이얼로그를 열 때마다 증가시켜 탭 선택을 초기화합니다. */
    dialogSession: Int,
    /** 열릴 때 이미 첨부된 항목(이미지 URI + 데이터 파일 URI, 다시 열어 추가 선택 가능) */
    initialSelection: List<Uri>,
    galleryMedia: List<Uri>,
    datasetFolders: List<DatasetFolder>,
    includeServerLibraryTabs: Boolean = false,
    /** 3DGS: JSON/PLY·GLB/ARCore 라이브러리 + 기기 다중 파일 탭 */
    include3dgsFileLibraryTabs: Boolean = false,
    serverDa3RasterUris: List<Uri> = emptyList(),
    serverDa3QualityJsonUris: List<Uri> = emptyList(),
    onPickMedia: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    var selectedTab by remember(dialogSession) { mutableStateOf(ClaudeImageLibraryTab.GALLERY) }
    var datasetBrowseFolder by remember(dialogSession) { mutableStateOf<DatasetFolder?>(null) }

    var jsonLibFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var model3dLibFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var arcoreLibFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(dialogSession, include3dgsFileLibraryTabs) {
        if (!include3dgsFileLibraryTabs) {
            jsonLibFiles = emptyList()
            model3dLibFiles = emptyList()
            arcoreLibFiles = emptyList()
        } else {
            val triple = withContext(Dispatchers.IO) {
                Triple(
                    JsonLibrary.listFilesSorted(context),
                    scanModel3dLibraryFiles(context),
                    scanArcoreAttachFiles(context),
                )
            }
            jsonLibFiles = triple.first
            model3dLibFiles = triple.second
            arcoreLibFiles = triple.third
        }
    }

    val selection = remember(dialogSession) {
        mutableStateListOf<Uri>().apply { addAll(initialSelection) }
    }

    fun uriKey(u: Uri) = u.toString()
    fun isPicked(u: Uri) = selection.any { uriKey(it) == uriKey(u) }
    fun togglePick(u: Uri) {
        val k = uriKey(u)
        val idx = selection.indexOfFirst { uriKey(it) == k }
        if (idx >= 0) selection.removeAt(idx) else selection.add(u)
    }

    val openMultiDocs = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { result ->
        if (result.isNullOrEmpty()) return@rememberLauncherForActivityResult
        for (u in result) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
            }
            togglePick(u)
        }
    }

    val visibleTabs = remember(includeServerLibraryTabs, include3dgsFileLibraryTabs) {
        buildList {
            add(ClaudeImageLibraryTab.GALLERY)
            add(ClaudeImageLibraryTab.DATASET)
            if (includeServerLibraryTabs) {
                add(ClaudeImageLibraryTab.GS_ANALYSIS)
                add(ClaudeImageLibraryTab.GS_QUALITY_EVAL)
            }
            if (include3dgsFileLibraryTabs) {
                add(ClaudeImageLibraryTab.JSON_LIBRARY)
                add(ClaudeImageLibraryTab.MODEL_3D)
                add(ClaudeImageLibraryTab.ARCORE_LIBRARY)
                add(ClaudeImageLibraryTab.DEVICE_FILES)
            }
        }
    }

    LaunchedEffect(visibleTabs, dialogSession) {
        if (selectedTab !in visibleTabs) {
            selectedTab = ClaudeImageLibraryTab.GALLERY
        }
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab != ClaudeImageLibraryTab.DATASET) {
            datasetBrowseFolder = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(screenH * 0.88f),
            shape = RoundedCornerShape(16.dp),
            color = palette.dialogSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                include3dgsFileLibraryTabs ->
                                    "이미지·동영상·데이터 파일 (탭마다 해당 라이브러리만)"
                                includeServerLibraryTabs ->
                                    "이미지·동영상 여러 개 선택 (라이브러리별)"
                                else ->
                                    "이미지 여러 개 선택 (갤러리·데이터셋)"
                            },
                            color = palette.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = palette.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val libraryTabStripScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(libraryTabStripScroll),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    visibleTabs.chunked(2).forEach { rowTabs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowTabs.forEach { tab ->
                                val label = when (tab) {
                                    ClaudeImageLibraryTab.GALLERY ->
                                        "갤러리 (${galleryMedia.size})"
                                    ClaudeImageLibraryTab.DATASET ->
                                        "데이터셋 (${datasetFolders.size})"
                                    ClaudeImageLibraryTab.GS_ANALYSIS ->
                                        "DA3 분석 (${serverDa3RasterUris.size})"
                                    ClaudeImageLibraryTab.GS_QUALITY_EVAL ->
                                        "DA3 품질평가 (${serverDa3QualityJsonUris.size})"
                                    ClaudeImageLibraryTab.JSON_LIBRARY ->
                                        "JSON (${jsonLibFiles.size})"
                                    ClaudeImageLibraryTab.MODEL_3D ->
                                        "PLY/GLB (${model3dLibFiles.size})"
                                    ClaudeImageLibraryTab.ARCORE_LIBRARY ->
                                        "ARCore (${arcoreLibFiles.size})"
                                    ClaudeImageLibraryTab.DEVICE_FILES ->
                                        "기기 파일"
                                }
                                val isSel = tab == selectedTab
                                Text(
                                    text = label,
                                    color = if (isSel) palette.onBrand else palette.onBackground,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 36.dp, max = 44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSel) palette.brand
                                            else palette.onBackground.copy(alpha = 0.12f),
                                        )
                                        .clickable { selectedTab = tab }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .wrapContentHeight(Alignment.CenterVertically),
                                )
                            }
                            if (rowTabs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (selectedTab) {
                        ClaudeImageLibraryTab.GALLERY -> {
                            if (galleryMedia.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "갤러리에 항목이 없습니다.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(galleryMedia, key = { it.toString() }) { uri ->
                                        ChatPickMediaThumbnail(
                                            uri = uri,
                                            palette = palette,
                                            restrictVideoForLlm = includeServerLibraryTabs,
                                            isSelected = isPicked(uri),
                                            onToggle = { togglePick(uri) },
                                        )
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.DATASET -> {
                            if (datasetFolders.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "데이터셋 폴더가 없습니다.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                    )
                                }
                            } else if (datasetBrowseFolder == null) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(datasetFolders, key = { it.dir.absolutePath }) { folder ->
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, palette.divider, RoundedCornerShape(8.dp))
                                                .clickable { datasetBrowseFolder = folder },
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f),
                                            ) {
                                                folder.coverUri?.let { cUri ->
                                                    Image(
                                                        painter = rememberAsyncImagePainter(cUri),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                }
                                                Box(
                                                    Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.62f))
                                                        .padding(4.dp),
                                                ) {
                                                    Text(
                                                        "${folder.name} (${folder.count})",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val folder = datasetBrowseFolder!!
                                val media = remember(folder) { datasetFolderMediaUrisForPicker(folder) }
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(onClick = { datasetBrowseFolder = null }) {
                                            Text(
                                                "← 폴더",
                                                color = palette.brand,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                val usable = media.filter { u ->
                                                    !includeServerLibraryTabs || !u.isChatPickVideoUri()
                                                }
                                                if (usable.isEmpty() && media.isNotEmpty() && includeServerLibraryTabs) {
                                                    Toast.makeText(
                                                        context,
                                                        "이 폴더에는 LLM에 보낼 이미지가 없습니다 (동영상만 있음).",
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                                } else {
                                                    var added = 0
                                                    for (u in usable) {
                                                        if (selection.none { uriKey(it) == uriKey(u) }) {
                                                            selection.add(u)
                                                            added++
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = media.isNotEmpty(),
                                        ) {
                                            Text(
                                                "이 폴더 전체 추가 (${media.size})",
                                                color = palette.brand,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            )
                                        }
                                    }
                                    if (media.isEmpty()) {
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "이 폴더에 이미지·동영상이 없습니다.",
                                                color = palette.onBackgroundMuted,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            items(media, key = { it.toString() }) { uri ->
                                                ChatPickMediaThumbnail(
                                                    uri = uri,
                                                    palette = palette,
                                                    restrictVideoForLlm = includeServerLibraryTabs,
                                                    isSelected = isPicked(uri),
                                                    onToggle = { togglePick(uri) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.GS_ANALYSIS -> {
                            if (serverDa3RasterUris.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "DA3 분석용 이미지가 없습니다.\n상·하향 미리보기·품질·분석 PNG 등이 포함된 작업을 내려받으세요.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(serverDa3RasterUris, key = { it.toString() }) { uri ->
                                        ChatPickMediaThumbnail(
                                            uri = uri,
                                            palette = palette,
                                            restrictVideoForLlm = false,
                                            isSelected = isPicked(uri),
                                            onToggle = { togglePick(uri) },
                                        )
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.GS_QUALITY_EVAL -> {
                            if (serverDa3QualityJsonUris.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "DA3 품질평가용 JSON이 없습니다.\nquality_report 등 JSON이 포함된 작업을 내려받으세요.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(serverDa3QualityJsonUris, key = { it.toString() }) { uri ->
                                        val path = uri.path
                                        val file =
                                            if (path.isNullOrBlank()) null else File(path).takeIf { it.isFile }
                                        if (file != null) {
                                            ChatPickAuxFileRow(
                                                file = file,
                                                palette = palette,
                                                isSelected = isPicked(uri),
                                                onToggle = { togglePick(uri) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.JSON_LIBRARY -> {
                            if (jsonLibFiles.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "JSON 라이브러리에 파일이 없습니다.\n서버 파이프라인 결과가 수집되면 여기에 표시됩니다.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(jsonLibFiles, key = { it.absolutePath }) { f ->
                                        val u = Uri.fromFile(f)
                                        ChatPickAuxFileRow(
                                            file = f,
                                            palette = palette,
                                            isSelected = isPicked(u),
                                            onToggle = { togglePick(u) },
                                        )
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.MODEL_3D -> {
                            if (model3dLibFiles.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "PLY/GLB 라이브러리에 파일이 없습니다.\n모델을 내려받거나 옮긴 뒤 다시 시도하세요.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(model3dLibFiles, key = { it.absolutePath }) { f ->
                                        val u = Uri.fromFile(f)
                                        ChatPickAuxFileRow(
                                            file = f,
                                            palette = palette,
                                            isSelected = isPicked(u),
                                            onToggle = { togglePick(u) },
                                        )
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.ARCORE_LIBRARY -> {
                            if (arcoreLibFiles.isEmpty()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "ARCore 라이브러리에 ZIP/JSON/GLB이 없습니다.",
                                        color = palette.onBackgroundMuted,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(arcoreLibFiles, key = { it.absolutePath }) { f ->
                                        val u = Uri.fromFile(f)
                                        ChatPickAuxFileRow(
                                            file = f,
                                            palette = palette,
                                            isSelected = isPicked(u),
                                            onToggle = { togglePick(u) },
                                        )
                                    }
                                }
                            }
                        }
                        ClaudeImageLibraryTab.DEVICE_FILES -> {
                            val devScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(devScroll)
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text(
                                    text = "시스템 파일 선택기에서 사진·동영상·JSON·PLY·GLB·ZIP 등을 한 번에 여러 개 선택할 수 있습니다. 선택한 항목은 다른 탭에서 고른 항목과 합쳐집니다.",
                                    color = palette.onBackgroundMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )
                                Button(
                                    onClick = {
                                        openMultiDocs.launch(arrayOf("*/*"))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.brand,
                                        contentColor = palette.onBrand,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FileUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "기기에서 여러 파일 선택…",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = palette.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "선택 ${selection.size}개",
                        color = palette.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { selection.clear() },
                            enabled = selection.isNotEmpty(),
                        ) {
                            Text("초기화", color = palette.brand, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onPickMedia(selection.toList()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brand,
                                contentColor = palette.onBrand,
                            ),
                        ) {
                            Text("선택 완료", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private enum class AiChatTabMode { CLAUDE, AI_CAD, MOBILE_3DGS, DAMAGE_ANALYSIS }

// 대화 스레드 드로어 UI

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
                    "DAMAGE_ANALYSIS" -> "파손"
                    else -> "클로드"
                }
                val isAiCad = thread.modeName == "AI_CAD"
                val isDocxScript = thread.modeName == "MOBILE_3DGS" ||
                    thread.modeName == "DAMAGE_ANALYSIS"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isAiCad -> palette.brand.copy(alpha = 0.15f)
                                isDocxScript -> Color(0xFFE8A838).copy(alpha = 0.2f)
                                else -> Color(0xFF4A4AFF).copy(alpha = 0.18f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = modeLabel,
                        color = when {
                            isAiCad -> palette.brand
                            isDocxScript -> Color(0xFFC9780A)
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

private data class MarkdownThreeDgsExport(
    val isExporting: Boolean,
    val onExport: () -> Unit,
)

/** @param imageUris 비전 입력용 이미지·동영상, @param jsonUris 3DGS 등 텍스트 부록용(JSON·PLY·ZIP 등) URI (영속 필드명 유지) */
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
    isStreaming: Boolean = false,
    threeDgsExport: MarkdownThreeDgsExport? = null,
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
                                            text = ju.lastPathSegment ?: "파일",
                                            color = palette.onSurfaceCard,
                                            fontSize = 12.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            if (message.jsonUris.size > 6) {
                                Text(
                                    text = "외 파일 ${message.jsonUris.size - 6}개",
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
            MarkdownText(
                text = displayText,
                threeDgsExport = if (isStreaming) null else threeDgsExport
            )
        }
    }
}


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

        val headingMatch = Regex("^(#{1,3}) (.+)").matchEntire(trimmed)
        if (headingMatch != null) {
            blocks.add(MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++; continue
        }

        val bulletMatch = Regex("^([ \t]*)[-*+] (.+)").matchEntire(line)
        if (bulletMatch != null) {
            val depth = bulletMatch.groupValues[1].length / 2
            blocks.add(MarkdownBlock.BulletItem(bulletMatch.groupValues[2], depth))
            i++; continue
        }

        val numMatch = Regex("^\\s*(\\d+)[.)\\s] (.+)").matchEntire(trimmed)
        if (numMatch != null) {
            blocks.add(MarkdownBlock.NumberedItem(numMatch.groupValues[1].toIntOrNull() ?: 1, numMatch.groupValues[2]))
            i++; continue
        }

        if (trimmed.matches(Regex("^[-*_]{3,}$"))) {
            blocks.add(MarkdownBlock.HRule); i++; continue
        }

        if (trimmed.isEmpty()) { i++; continue }

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
private fun buildInlineAnnotated(text: String, baseColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 3
                } else append(text[i++])
            }
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else append(text[i++])
            }
            text.startsWith("_", i) && (i == 0 || text[i - 1] != '_') -> {
                val end = text.indexOf("_", i + 1)
                if (end != -1 && (end + 1 >= text.length || text[end + 1] != '_')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else append(text[i++])
            }
            text.startsWith("`", i) && !text.startsWith("```", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = baseColor.copy(alpha = 0.14f),
                        color = baseColor,
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
    fontSize: TextUnit = 15.sp,
    threeDgsExport: MarkdownThreeDgsExport? = null,
) {
    val palette = LocalAppUiPalette.current
    val resolvedColor = textColor ?: palette.markdownDefault
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    val firstPythonIdx = remember(blocks, threeDgsExport) {
        if (threeDgsExport == null) {
            -1
        } else {
            blocks.indexOfFirst { b ->
                if (b !is MarkdownBlock.CodeBlock) return@indexOfFirst false
                ThreeDgsChatDocxExport.shouldOfferDocxExportForCodeBlock(b.language, b.code)
            }
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        blocks.forEachIndexed { idx, block ->
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
                        text = buildInlineAnnotated(block.text, resolvedColor),
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
                            text = buildInlineAnnotated(block.text, resolvedColor),
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
                            text = buildInlineAnnotated(block.text, resolvedColor),
                            color = resolvedColor,
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.5).sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    if (idx == firstPythonIdx && threeDgsExport != null) {
                        MarkdownPythonCodeBlock3dgs(
                            code = block.code,
                            isExporting = threeDgsExport.isExporting,
                            onExport = threeDgsExport.onExport,
                        )
                    } else {
                        MarkdownCodeBlock(language = block.language, code = block.code)
                    }
                }
                is MarkdownBlock.HRule -> {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = resolvedColor.copy(alpha = 0.18f)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineAnnotated(block.text, resolvedColor),
                        color = resolvedColor,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.6).sp
                    )
                }
            }
        }
    }
}

/** 3DGS: python-docx 스크립트 블록 + Word 저장 버튼 */
@Composable
private fun MarkdownPythonCodeBlock3dgs(
    code: String,
    isExporting: Boolean,
    onExport: () -> Unit,
) {
    val palette = LocalAppUiPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.codeBlockBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.codeBlockBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.codeBlockHeaderBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "python",
                    color = palette.codeBlockMeta,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (isExporting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = palette.brand,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "저장 중…",
                            color = palette.onBackground.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    TextButton(
                        onClick = onExport,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PDF 저장·열기",
                            color = palette.brand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

private fun openPdf(context: Context, pdfFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "PDF로 열기"))
    } catch (e: Exception) {
        Toast.makeText(context, "PDF 뷰어를 설치한 뒤 다시 시도하세요.", Toast.LENGTH_LONG).show()
    }
}
