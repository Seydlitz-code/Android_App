package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 4열 그리드 기준 한 변의 디코딩 목표 픽셀(대략 셀 너비). 원본 전체 디코딩을 피해 메모리·스크롤 부담을 줄입니다.
 */
@Composable
fun rememberGalleryGridThumbEdgePx(columns: Int): Int {
    val cfg = LocalConfiguration.current
    return remember(cfg.screenWidthDp, cfg.densityDpi, columns) {
        val gutterApprox = 24f
        val cellDp = ((cfg.screenWidthDp - gutterApprox) / columns.toFloat()).coerceAtLeast(40f)
        (cellDp * cfg.densityDpi / 160f).toInt().coerceIn(120, 720)
    }
}

/** 갤러리·데이터셋 그리드: 셀 크기에 맞춰 다운샘플링 후 표시 */
@Composable
fun rememberGalleryGridPhotoPainter(uri: Uri, thumbEdgePx: Int): AsyncImagePainter {
    val context = LocalContext.current
    val request = remember(uri, thumbEdgePx, context) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(thumbEdgePx, thumbEdgePx)
            .precision(Precision.INEXACT)
            .scale(Scale.FILL)
            .crossfade(false)
            .build()
    }
    return rememberAsyncImagePainter(request)
}

private fun downscaleBitmapIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxEdge && h <= maxEdge) return bitmap
    val scale = kotlin.math.min(maxEdge.toFloat() / w, maxEdge.toFloat() / h)
    val nw = max(1, (w * scale).toInt())
    val nh = max(1, (h * scale).toInt())
    val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
}

/** 그리드용 동영상 첫 프레임: 가능하면 [MediaMetadataRetriever.getScaledFrameAtTime] 사용 */
suspend fun decodeVideoGridThumbnail(context: Context, uri: Uri, maxEdge: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    0L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    maxEdge,
                    maxEdge
                )
            } else {
                @Suppress("DEPRECATION")
                retriever.getFrameAtTime(0L)?.let { downscaleBitmapIfNeeded(it, maxEdge) }
            }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }
