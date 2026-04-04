package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * PLY/OBJ용 미리보기 PNG. [Context.filesDir] 전용 — 시스템 갤러리에 스캔되지 않습니다.
 * 생성은 [thumbMutex]로 직렬화하여 동시 다수 로드 시 프레임 드랍을 줄입니다.
 */
object Model3dThumbnail {

    private const val SIZE_PX = 192

    private val thumbMutex = Mutex()

    private fun thumbnailDir(context: Context): File =
        File(context.filesDir, "app_model_thumbs").apply { mkdirs() }

    fun thumbnailFileForModel(context: Context, modelFile: File): File {
        val id = stableThumbId(modelFile)
        return File(thumbnailDir(context), "$id.png")
    }

    private fun stableThumbId(modelFile: File): String {
        val p = modelFile.absolutePath
        val h = p.hashCode().toUInt().toString(16)
        val n = modelFile.name.hashCode().toUInt().toString(16)
        return "${h}_$n"
    }

    fun invalidateForModelFile(context: Context, modelFile: File) {
        try {
            thumbnailFileForModel(context, modelFile).delete()
        } catch (_: Exception) {
        }
    }

    fun needsRegenerate(modelFile: File, thumbFile: File): Boolean {
        if (!modelFile.exists()) return false
        if (!thumbFile.exists() || thumbFile.length() == 0L) return true
        return modelFile.lastModified() > thumbFile.lastModified()
    }

    /**
     * UI에서 호출: 한 번에 하나의 썸네일만 디스크/파싱 부하가 걸리도록 직렬화합니다.
     */
    suspend fun generateOrGetAsync(context: Context, modelFile: File): File? =
        thumbMutex.withLock {
            withContext(Dispatchers.IO) {
                generateOrGet(context, modelFile)
            }
        }

    fun generateOrGet(context: Context, modelFile: File): File? {
        return try {
            val thumb = thumbnailFileForModel(context, modelFile)
            if (!needsRegenerate(modelFile, thumb) && thumb.exists() && thumb.length() > 0L) {
                return thumb
            }
            val mesh = loadModelForThumbnailMesh(modelFile) ?: return null
            val bmp = renderMeshToBitmap(mesh, SIZE_PX) ?: return null
            try {
                FileOutputStream(thumb).use { out ->
                    if (!bmp.compress(Bitmap.CompressFormat.PNG, 90, out)) return null
                }
                thumb
            } catch (_: Exception) {
                null
            } finally {
                bmp.recycle()
            }
        } catch (_: Throwable) {
            null
        }
    }
}

private fun renderMeshToBitmap(mesh: ObjParseResult, sizePx: Int): Bitmap? {
    val pts = mesh.points
    if (mesh.count <= 0 || pts.size < 3) return null

    val pad = 10f
    val bgRgb = Color.rgb(26, 26, 26)
    val defaultPointRgb = Color.rgb(0x7e, 0xd3, 0x21)
    val triStrokeRgb = Color.rgb(0x7e, 0xd3, 0x21)
    val triFillRgb = Color.argb(210, 0x4a, 0x8c, 0x6a)

    val projX = FloatArray(mesh.count)
    val projY = FloatArray(mesh.count)
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    for (i in 0 until mesh.count) {
        val x = pts[i * 3]
        val y = pts[i * 3 + 1]
        val z = pts[i * 3 + 2]
        val sx = x - z * 0.45f
        val sy = -y + z * 0.38f
        projX[i] = sx
        projY[i] = sy
        if (sx < minX) minX = sx
        if (sx > maxX) maxX = sx
        if (sy < minY) minY = sy
        if (sy > maxY) maxY = sy
    }

    val bw = maxX - minX
    val bh = maxY - minY
    if (bw < 1e-6f || bh < 1e-6f) return null
    val inner = sizePx - 2 * pad
    val scale = inner / max(bw, bh)
    val ox = pad + (inner - bw * scale) / 2f - minX * scale
    val oy = pad + (inner - bh * scale) / 2f - minY * scale

    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(bgRgb)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = triStrokeRgb
    }
    val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val path = Path()
    val vc = mesh.vertexColors

    if (mesh.drawMode == MeshDrawMode.TRIANGLES && mesh.count >= 3) {
        val triCount = mesh.count / 3
        for (ti in 0 until triCount) {
            val i0 = ti * 3
            val i1 = i0 + 1
            val i2 = i0 + 2
            fun px(i: Int) = ox + projX[i] * scale
            fun py(i: Int) = oy + projY[i] * scale
            path.reset()
            path.moveTo(px(i0), py(i0))
            path.lineTo(px(i1), py(i1))
            path.lineTo(px(i2), py(i2))
            path.close()
            if (vc != null && vc.size >= (i2 + 1) * 3) {
                fillPaint.color = Color.rgb(
                    (vc[i0 * 3] * 255f).toInt().coerceIn(0, 255),
                    (vc[i0 * 3 + 1] * 255f).toInt().coerceIn(0, 255),
                    (vc[i0 * 3 + 2] * 255f).toInt().coerceIn(0, 255)
                )
            } else {
                fillPaint.color = triFillRgb
            }
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    } else {
        val step = max(1, mesh.count / 10_000)
        for (i in 0 until mesh.count step step) {
            val cx = ox + projX[i] * scale
            val cy = oy + projY[i] * scale
            if (vc != null && vc.size >= (i + 1) * 3) {
                pointPaint.color = Color.rgb(
                    (vc[i * 3] * 255f).toInt().coerceIn(0, 255),
                    (vc[i * 3 + 1] * 255f).toInt().coerceIn(0, 255),
                    (vc[i * 3 + 2] * 255f).toInt().coerceIn(0, 255)
                )
            } else {
                pointPaint.color = defaultPointRgb
            }
            canvas.drawCircle(cx, cy, 2.1f, pointPaint)
        }
    }
    return bmp
}
