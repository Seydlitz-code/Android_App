package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 카메라 허브 2×2 타일에서 쓰는 픽토그램 리소스 ID (프리로드·캐시 키용) */
val CAMERA_ENTRY_PICTOGRAM_RES_IDS: IntArray = intArrayOf(
    R.drawable.ic_camera_mode_object,
    R.drawable.ic_camera_mode_space_2d,
    R.drawable.ic_camera_mode_space_3d,
    R.drawable.ic_camera_mode_mobile_space,
)

private fun cacheKey(resId: Int, ink: Color): String = "${resId}_${ink.toArgb()}"

/**
 * 화면에 작게 보이므로 긴 변 기준 다운샘플 후 마스킹·틴트 — 픽셀 수를 줄여 첫 표시를 단축한다.
 */
private fun decodeResourceScaledForCameraPictogram(context: Context, resId: Int, maxSide: Int = 256): Bitmap? {
    val res = context.resources
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(res, resId, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return BitmapFactory.decodeResource(res, resId)
    var sample = 1
    while (max(w / sample, h / sample) > maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeResource(res, resId, opts)
}

/**
 * 가장자리 BFS로 밝은 배경을 투명 처리하고 실루엣은 [ink]로 칠한다.
 * (CameraEntry 전용 픽토그램 PNG — 흰 배경·검 실루엣·내부 흰 컷아웃)
 */
private fun processCameraEntryPictogramBitmap(source: Bitmap, ink: Color): Bitmap {
    val w = source.width
    val h = source.height
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    if (bitmap != source) source.recycle()
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val outside = BooleanArray(w * h)
    val q = ArrayDeque<Int>(512)

    fun isPassable(idx: Int): Boolean {
        val p = pixels[idx]
        val a = p ushr 24 and 0xFF
        if (a < 14) return true
        val r = p shr 16 and 0xFF
        val g = p shr 8 and 0xFF
        val b = p and 0xFF
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val delta = mx - mn
        val luma = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
        val greyish = delta < 40
        if (luma < 118) return false
        if (greyish && luma < 172) return false
        return true
    }

    fun enqueue(x: Int, y: Int) {
        if (x !in 0 until w || y !in 0 until h) return
        val i = y * w + x
        if (outside[i]) return
        if (!isPassable(i)) return
        outside[i] = true
        q.addLast(i)
    }

    for (x in 0 until w) {
        enqueue(x, 0)
        enqueue(x, h - 1)
    }
    for (y in 0 until h) {
        enqueue(0, y)
        if (w > 1) enqueue(w - 1, y)
    }

    while (q.isNotEmpty()) {
        val i = q.removeFirst()
        val x = i % w
        val y = i / w
        if (x > 0) {
            val ni = i - 1
            if (!outside[ni] && isPassable(ni)) {
                outside[ni] = true
                q.addLast(ni)
            }
        }
        if (x < w - 1) {
            val ni = i + 1
            if (!outside[ni] && isPassable(ni)) {
                outside[ni] = true
                q.addLast(ni)
            }
        }
        if (y > 0) {
            val ni = i - w
            if (!outside[ni] && isPassable(ni)) {
                outside[ni] = true
                q.addLast(ni)
            }
        }
        if (y < h - 1) {
            val ni = i + w
            if (!outside[ni] && isPassable(ni)) {
                outside[ni] = true
                q.addLast(ni)
            }
        }
    }

    fun isNearWhiteCutout(p: Int): Boolean {
        val a = (p ushr 24) and 0xFF
        if (a < 14) return true
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        return r >= 247 && g >= 247 && b >= 247
    }

    val ir = (ink.red * 255f).roundToInt().coerceIn(0, 255)
    val ig = (ink.green * 255f).roundToInt().coerceIn(0, 255)
    val ib = (ink.blue * 255f).roundToInt().coerceIn(0, 255)

    for (i in pixels.indices) {
        if (outside[i]) {
            pixels[i] = 0
        } else if (isNearWhiteCutout(pixels[i])) {
            pixels[i] = 0
        } else {
            val a = (pixels[i] ushr 24) and 0xFF
            pixels[i] = (a shl 24) or (ir shl 16) or (ig shl 8) or ib
        }
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    return bitmap
}

// 메인 화면 진입 시 프리웜·타일에서 재사용. 최대 MAX_CACHE_ENTRIES 개만 보관해 RAM 누수를 방지합니다.
// 캐싱 레이어(빠른 순서): 1) 메모리 ConcurrentHashMap  2) 디스크(filesDir/pictogram_cache/NNN.png)  3) BFS 풀 처리
object CameraEntryPictogramCache {
    private const val MAX_CACHE_ENTRIES = 16
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val buildMutexes = ConcurrentHashMap<String, Mutex>()

    private fun mutexFor(key: String): Mutex =
        buildMutexes.computeIfAbsent(key) { Mutex() }

    fun peek(resId: Int, ink: Color): ImageBitmap? = cache[cacheKey(resId, ink)]

    // --- 디스크 캐시 헬퍼 ---

    private fun diskCacheFile(context: Context, resId: Int, inkArgb: Int): File {
        val dir = File(context.filesDir, "pictogram_cache").apply { mkdirs() }
        return File(dir, "${resId}_$inkArgb.png")
    }

    private fun loadFromDisk(context: Context, resId: Int, inkArgb: Int): Bitmap? = try {
        val file = diskCacheFile(context, resId, inkArgb)
        if (file.exists() && file.length() > 0L) BitmapFactory.decodeFile(file.absolutePath)
        else null
    } catch (_: Exception) { null }

    private fun saveToDisk(context: Context, resId: Int, inkArgb: Int, bitmap: Bitmap) {
        try {
            FileOutputStream(diskCacheFile(context, resId, inkArgb)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (_: Exception) {}
    }

    // --- 메인 API ---
    // 1) 메모리 히트 → 즉시 반환  2) 디스크 히트 → IO 스레드에서 PNG 디코딩  3) 미스 → BFS 처리 후 디스크 저장
    suspend fun ensureLoaded(context: Context, resId: Int, ink: Color): ImageBitmap {
        peek(resId, ink)?.let { return it }
        val key = cacheKey(resId, ink)
        val mutex = mutexFor(key)
        return mutex.withLock {
            peek(resId, ink)?.let { return@withLock it }
            val appCtx = context.applicationContext
            val inkArgb = ink.toArgb()

            val image = withContext(Dispatchers.IO) {
                // 1차: 디스크 캐시 (PNG 디코딩 — 수 ms)
                val diskBmp = loadFromDisk(appCtx, resId, inkArgb)
                if (diskBmp != null) {
                    diskBmp.asImageBitmap()
                } else {
                    // 2차: BFS 처리 후 디스크에 저장 (최초 1회)
                    val raw = decodeResourceScaledForCameraPictogram(appCtx, resId)
                        ?: BitmapFactory.decodeResource(appCtx.resources, resId)
                    if (raw == null) {
                        solidColorBitmap(ink)
                    } else {
                        try {
                            val processed = processCameraEntryPictogramBitmap(raw, ink)
                            saveToDisk(appCtx, resId, inkArgb, processed)
                            processed.asImageBitmap()
                        } catch (_: Throwable) {
                            raw.recycle()
                            solidColorBitmap(ink)
                        }
                    }
                }
            }

            // 캐시 항목이 상한에 도달하면 가장 오래된 항목을 제거해 메모리 누적을 방지합니다.
            if (cache.size >= MAX_CACHE_ENTRIES) {
                cache.keys.firstOrNull()?.let { cache.remove(it) }
            }
            cache[key] = image
            image
        }
    }

    suspend fun warmup(context: Context, ink: Color) {
        val appCtx = context.applicationContext
        coroutineScope {
            CAMERA_ENTRY_PICTOGRAM_RES_IDS.map { id ->
                async(Dispatchers.IO) {
                    ensureLoaded(appCtx, id, ink)
                }
            }.awaitAll()
        }
    }

    private fun solidColorBitmap(ink: Color): ImageBitmap {
        val b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val ir = (ink.red * 255f).roundToInt().coerceIn(0, 255)
        val ig = (ink.green * 255f).roundToInt().coerceIn(0, 255)
        val ib = (ink.blue * 255f).roundToInt().coerceIn(0, 255)
        b.eraseColor(0xFF000000.toInt() or (ir shl 16) or (ig shl 8) or ib)
        return b.asImageBitmap()
    }
}
