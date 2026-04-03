package com.example.app_01

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * 온디바이스 **빛반사·광택 완화** (갤럭시 AI 지우개 ‘빛반사 제거’와 유사한 단계적 접근).
 *
 * 삼성 전용 API는 없으므로, 공개적으로 알려진 흐름인
 * **① 반사(하이라이트) 영역 탐지 → ② 마스크 정제 → ③ 주변 맥락으로 색·구조 보간 → ④ 경계·질감 정리**
 * 을 순수 Kotlin으로 구현한다. ONNX/외부 딥러닝 없음 → 네이티브 크래시·모델 의존성 없음.
 *
 * 단계:
 * 1. Glide 로드 (소프트웨어 비트맵, 최장변 제한)
 * 2. HSV + 국소 밝기 대비로 반사 후보 마스크
 * 3. 형태학적 정제 + 경계 페더
 * 4. 저주파(큰 가우시안)로 조명층 완화 블렌딩
 * 5. 비반사 이웃 샘플로 색 조화(컨텍스트 채움)
 * 6. 엣지 가중 마스크로 경계 자연스럽게
 * 7. 언샤프로 미세 질감, 채도 보정
 * 8. 저장
 */
object GlareRemovalProcessor {

    private const val TAG = "GlareRemoval"
    const val TOTAL_STEPS = 8

    /** 메모리·속도 균형 (갤럭시 일반 해상도 대응) */
    private const val MAX_LONG_EDGE = 1024

    private const val GLARE_V_MIN = 0.88f
    private const val GLARE_S_MAX = 0.28f
    /** 국소 평균 대비 이 배율 이상이면 반사 후보 */
    private const val LOCAL_BRIGHT_RATIO = 1.28f
    private const val LOCAL_LUM_FLOOR = 0.52f

    private const val MIN_MASK_FRACTION = 0.0008f

    sealed class Result {
        data class Success(val bitmap: Bitmap, val savedUri: Uri?) : Result()
        data class Error(val message: String) : Result()
    }

    fun removeGlare(
        context: Context,
        sourceUri: Uri,
        outputDir: File? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result {
        return try {
            val appCtx = context.applicationContext

            onProgress(1, TOTAL_STEPS)
            val work = loadBitmap(appCtx, sourceUri)
                ?: return Result.Error("이미지를 불러올 수 없습니다.\n지원 형식: JPG, PNG, WEBP")

            val w = work.width
            val h = work.height
            if (w <= 0 || h <= 0) {
                work.recycle()
                return Result.Error("유효하지 않은 이미지 크기입니다.")
            }

            val pixels = IntArray(w * h)
            try {
                work.getPixels(pixels, 0, w, 0, 0, w, h)
            } catch (t: Throwable) {
                work.recycle()
                Log.e(TAG, "픽셀 읽기 실패", t)
                return Result.Error("이미지를 읽을 수 없습니다.\n다른 사진으로 시도해 주세요.")
            }
            work.recycle()

            onProgress(2, TOTAL_STEPS)
            val lum = luminance01(pixels, w, h)
            val localMean = boxBlurFloat(lum, w, h, radius = 10)
            val rawMask = buildRawGlareMask(pixels, lum, localMean, w, h)
            val maskSum = rawMask.sum()
            if (maskSum < MIN_MASK_FRACTION * w * h) {
                return Result.Error(
                    "뚜렷한 빛반사·광택이 검출되지 않았습니다.\n" +
                        "유리·모니터 반사가 보이는 사진을 선택해 주세요."
                )
            }

            onProgress(3, TOTAL_STEPS)
            var mask = morphOpenF(rawMask, w, h, r = 1)
            mask = morphDilateF(mask, w, h, r = 2)
            mask = boxBlurFloat(mask, w, h, 2)
            mask = boxBlurFloat(mask, w, h, 2)
            for (i in mask.indices) mask[i] = mask[i].coerceIn(0f, 1f)

            onProgress(4, TOTAL_STEPS)
            val gray = toGray01(pixels, w, h)
            val edge01 = sobelEdge01(gray, w, h)
            val maskEdge = FloatArray(w * h) { i ->
                (mask[i] * (1f - 0.38f * edge01[i])).coerceIn(0f, 1f)
            }

            val coarseBlur = gaussianBlurRgb(pixels, w, h, radius = 14)
            var blended = blendByMask(pixels, coarseBlur, maskEdge)

            onProgress(5, TOTAL_STEPS)
            blended = harmonizeFromNeighbors(blended, maskEdge, w, h, sampleRadius = 14)

            onProgress(6, TOTAL_STEPS)
            val blurSmall = gaussianBlurRgb(blended, w, h, radius = 2)
            blended = unsharpLike(blended, blurSmall, amount = 0.42f)

            onProgress(7, TOTAL_STEPS)
            val finalPx = adjustSaturation(blended, satMul = 1.035f)

            val resultBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            resultBmp.setPixels(finalPx, 0, w, 0, 0, w, h)

            onProgress(TOTAL_STEPS, TOTAL_STEPS)
            val dir = outputDir ?: context.getExternalFilesDir(null)
            val savedUri = saveResult(context, resultBmp, dir)
            Log.d(TAG, "빛반사 완화 완료 uri=$savedUri")

            Result.Success(resultBmp, savedUri)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM", e)
            Result.Error("메모리 부족입니다.\n다른 앱을 종료한 뒤 다시 시도해 주세요.")
        } catch (e: Exception) {
            Log.e(TAG, "처리 실패", e)
            Result.Error("처리 중 오류: ${e.message}")
        } catch (t: Throwable) {
            Log.e(TAG, "치명적 오류", t)
            Result.Error("예기치 않은 오류가 발생했습니다.\n앱을 다시 실행한 뒤 시도해 주세요.")
        }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions().disallowHardwareConfig())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .override(MAX_LONG_EDGE, MAX_LONG_EDGE)
                .fitCenter()
                .submit(MAX_LONG_EDGE, MAX_LONG_EDGE)
                .get()
                ?.let { bmp ->
                    if (bmp.config == Bitmap.Config.ARGB_8888) bmp
                    else bmp.copy(Bitmap.Config.ARGB_8888, false).also { if (it !== bmp) bmp.recycle() }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Glide 로드 실패", e)
            null
        }
    }

    private fun luminance01(pixels: IntArray, w: Int, h: Int): FloatArray {
        return FloatArray(pixels.size) { i ->
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            (0.299f * r + 0.587f * g + 0.114f * b).coerceIn(0f, 1f)
        }
    }

    private fun toGray01(pixels: IntArray, w: Int, h: Int): FloatArray = luminance01(pixels, w, h)

    private fun buildRawGlareMask(
        pixels: IntArray,
        lum: FloatArray,
        localMean: FloatArray,
        w: Int,
        h: Int
    ): FloatArray {
        val out = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val specHsv = isSpecularHsv(pixels[i])
            val l = lum[i]
            val loc = max(localMean[i], 1e-4f)
            val localHot = l >= LOCAL_LUM_FLOOR && l >= loc * LOCAL_BRIGHT_RATIO
            out[i] = if (specHsv || localHot) 1f else 0f
        }
        return out
    }

    private fun isSpecularHsv(px: Int): Boolean {
        val hsv = FloatArray(3)
        Color.RGBToHSV(
            (px shr 16) and 0xFF,
            (px shr 8) and 0xFF,
            px and 0xFF,
            hsv
        )
        return hsv[2] >= GLARE_V_MIN && hsv[1] <= GLARE_S_MAX
    }

    private fun morphOpenF(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        return morphDilateF(morphErodeF(src, w, h, r), w, h, r)
    }

    private fun morphErodeF(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var v = 1f
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val ny = (y + dy).coerceIn(0, h - 1)
                        v = min(v, src[ny * w + nx])
                    }
                }
                out[y * w + x] = v
            }
        }
        return out
    }

    private fun morphDilateF(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var v = 0f
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val ny = (y + dy).coerceIn(0, h - 1)
                        v = max(v, src[ny * w + nx])
                    }
                }
                out[y * w + x] = v
            }
        }
        return out
    }

    private fun boxBlurFloat(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        if (radius <= 0) return src.copyOf()
        val tmp = FloatArray(src.size)
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = 0f
                var c = 0
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    s += src[y * w + nx]
                    c++
                }
                tmp[y * w + x] = s / c
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                var s = 0f
                var c = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    s += tmp[ny * w + x]
                    c++
                }
                out[y * w + x] = s / c
            }
        }
        return out
    }

    private fun sobelEdge01(gray: FloatArray, w: Int, h: Int): FloatArray {
        val gxK = intArrayOf(-1, 0, 1, -2, 0, 2, -1, 0, 1)
        val gyK = intArrayOf(-1, -2, -1, 0, 0, 0, 1, 2, 1)
        val mag = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var gx = 0f
                var gy = 0f
                var ki = 0
                for (j in -1..1) {
                    for (i in -1..1) {
                        val v = gray[(y + j) * w + (x + i)]
                        gx += gxK[ki] * v
                        gy += gyK[ki] * v
                        ki++
                    }
                }
                mag[y * w + x] = hypot(gx.toDouble(), gy.toDouble()).toFloat()
            }
        }
        var mx = 1e-5f
        for (v in mag) mx = max(mx, v)
        return FloatArray(mag.size) { (mag[it] / mx).coerceIn(0f, 1f) }
    }

    private fun gaussianBlurRgb(pixels: IntArray, w: Int, h: Int, radius: Int): IntArray {
        val sigma = (radius / 2f + 1f).coerceAtLeast(0.85f)
        val kSize = radius * 2 + 1
        val kernel = FloatArray(kSize) { i ->
            val x = (i - radius).toFloat()
            exp(-(x * x) / (2f * sigma * sigma))
        }
        var sumK = kernel.sum()
        for (i in kernel.indices) kernel[i] /= sumK

        val tmp = IntArray(pixels.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rr = 0f
                var gg = 0f
                var bb = 0f
                for (ki in kernel.indices) {
                    val nx = (x + ki - radius).coerceIn(0, w - 1)
                    val px = pixels[y * w + nx]
                    val wk = kernel[ki]
                    rr += wk * ((px shr 16) and 0xFF)
                    gg += wk * ((px shr 8) and 0xFF)
                    bb += wk * (px and 0xFF)
                }
                tmp[y * w + x] = argb(
                    (rr + 0.5f).toInt().coerceIn(0, 255),
                    (gg + 0.5f).toInt().coerceIn(0, 255),
                    (bb + 0.5f).toInt().coerceIn(0, 255)
                )
            }
        }
        val out = IntArray(pixels.size)
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rr = 0f
                var gg = 0f
                var bb = 0f
                for (ki in kernel.indices) {
                    val ny = (y + ki - radius).coerceIn(0, h - 1)
                    val px = tmp[ny * w + x]
                    val wk = kernel[ki]
                    rr += wk * ((px shr 16) and 0xFF)
                    gg += wk * ((px shr 8) and 0xFF)
                    bb += wk * (px and 0xFF)
                }
                out[y * w + x] = argb(
                    (rr + 0.5f).toInt().coerceIn(0, 255),
                    (gg + 0.5f).toInt().coerceIn(0, 255),
                    (bb + 0.5f).toInt().coerceIn(0, 255)
                )
            }
        }
        return out
    }

    private fun blendByMask(orig: IntArray, lowFreq: IntArray, mask01: FloatArray): IntArray {
        return IntArray(orig.size) { i ->
            val m = mask01[i]
            if (m <= 0f) return@IntArray orig[i]
            val o = orig[i]
            val t = lowFreq[i]
            val or = (o shr 16) and 0xFF
            val og = (o shr 8) and 0xFF
            val ob = o and 0xFF
            val tr = (t shr 16) and 0xFF
            val tg = (t shr 8) and 0xFF
            val tb = t and 0xFF
            argb(
                (or * (1 - m) + tr * m).toInt().coerceIn(0, 255),
                (og * (1 - m) + tg * m).toInt().coerceIn(0, 255),
                (ob * (1 - m) + tb * m).toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * 마스크가 강한 픽셀의 색을, 반사가 약한 이웃에서 가중 평균한 색으로 당겨 옴 (지우개식 맥락 채움).
     */
    private fun harmonizeFromNeighbors(
        pixels: IntArray,
        mask01: FloatArray,
        w: Int,
        h: Int,
        sampleRadius: Int
    ): IntArray {
        val out = pixels.copyOf()
        val r = sampleRadius
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val m = mask01[i]
                if (m < 0.12f) continue
                var sumR = 0.0
                var sumG = 0.0
                var sumB = 0.0
                var sumW = 0.0
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        if (dx * dx + dy * dy > r * r) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue
                        val j = ny * w + nx
                        if (mask01[j] > 0.45f) continue
                        val dist = (dx * dx + dy * dy + 1).toFloat()
                        val ww = 1.0 / dist
                        val p = pixels[j]
                        sumR += ((p shr 16) and 0xFF) * ww
                        sumG += ((p shr 8) and 0xFF) * ww
                        sumB += (p and 0xFF) * ww
                        sumW += ww
                    }
                }
                if (sumW < 1e-6) continue
                val nr = (sumR / sumW).toInt().coerceIn(0, 255)
                val ng = (sumG / sumW).toInt().coerceIn(0, 255)
                val nb = (sumB / sumW).toInt().coerceIn(0, 255)
                val o = out[i]
                val or = (o shr 16) and 0xFF
                val og = (o shr 8) and 0xFF
                val ob = o and 0xFF
                out[i] = argb(
                    (or * (1 - m) + nr * m).toInt().coerceIn(0, 255),
                    (og * (1 - m) + ng * m).toInt().coerceIn(0, 255),
                    (ob * (1 - m) + nb * m).toInt().coerceIn(0, 255)
                )
            }
        }
        return out
    }

    private fun unsharpLike(orig: IntArray, blurred: IntArray, amount: Float): IntArray {
        return IntArray(orig.size) { i ->
            val o = orig[i]
            val bl = blurred[i]
            val or = ((o shr 16) and 0xFF).toFloat()
            val og = ((o shr 8) and 0xFF).toFloat()
            val ob = (o and 0xFF).toFloat()
            val br = ((bl shr 16) and 0xFF).toFloat()
            val bg = ((bl shr 8) and 0xFF).toFloat()
            val bb = (bl and 0xFF).toFloat()
            argb(
                (or + amount * (or - br)).toInt().coerceIn(0, 255),
                (og + amount * (og - bg)).toInt().coerceIn(0, 255),
                (ob + amount * (ob - bb)).toInt().coerceIn(0, 255)
            )
        }
    }

    private fun adjustSaturation(pixels: IntArray, satMul: Float): IntArray {
        val hsv = FloatArray(3)
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            Color.RGBToHSV(
                (p shr 16) and 0xFF,
                (p shr 8) and 0xFF,
                p and 0xFF,
                hsv
            )
            hsv[1] = (hsv[1] * satMul).coerceIn(0f, 1f)
            out[i] = Color.HSVToColor(hsv)
        }
        return out
    }

    private fun argb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun saveResult(context: Context, bitmap: Bitmap, outputDir: File?): Uri? {
        return try {
            val fileName = "reflect_soft_${System.currentTimeMillis()}.jpg"
            if (outputDir != null) {
                outputDir.mkdirs()
                val file = File(outputDir, fileName)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                Uri.fromFile(file)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                uri?.also { u ->
                    context.contentResolver.openOutputStream(u)
                        ?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "저장 실패", e)
            null
        }
    }
}
