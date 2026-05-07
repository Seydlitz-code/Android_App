package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
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
private fun decodeResourceScaledForCameraPictogram(context: Context, resId: Int, maxSide: Int = 384): Bitmap? {
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

/**
 * 메인 화면 진입 시 프리웜·타일에서 재사용. 테마(ink)별로 최대 4장만 보관.
 */
object CameraEntryPictogramCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val buildLock = Any()

    fun peek(resId: Int, ink: Color): ImageBitmap? = cache[cacheKey(resId, ink)]

    suspend fun ensureLoaded(context: Context, resId: Int, ink: Color): ImageBitmap {
        peek(resId, ink)?.let { return it }
        return withContext(Dispatchers.Default) {
            synchronized(buildLock) {
                peek(resId, ink)?.let { return@withContext it }
                val raw = decodeResourceScaledForCameraPictogram(context, resId)
                    ?: BitmapFactory.decodeResource(context.resources, resId)
                val image = if (raw == null) {
                    solidColorBitmap(ink)
                } else {
                    try {
                        processCameraEntryPictogramBitmap(raw, ink).asImageBitmap()
                    } catch (_: Throwable) {
                        raw.recycle()
                        solidColorBitmap(ink)
                    }
                }
                cache[cacheKey(resId, ink)] = image
                image
            }
        }
    }

    suspend fun warmup(context: Context, ink: Color) {
        withContext(Dispatchers.Default) {
            for (id in CAMERA_ENTRY_PICTOGRAM_RES_IDS) {
                if (peek(id, ink) != null) continue
                ensureLoaded(context, id, ink)
            }
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
