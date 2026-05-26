package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 밀리초까지 포함된 촬영·미디어 파일명용 타임스탬프 (yyyy-MM-dd-HH-mm-ss-SSS) */
internal fun timestampMs(): String =
    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())

/** 간결한 파일명용 타임스탬프 (yyyyMMdd_HHmmss) */
internal fun timestampCompact(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

/** 확장자가 주어지면 "{timestampMs()}.{ext}" 형태의 파일명을 반환 */
internal fun mediaFileName(ext: String): String =
    "${timestampMs()}.$ext"

/** 확장자 없이 "{timestampMs()}" 타임스탬프만 반환 */
internal fun timestampMsString(): String = timestampMs()

/** ServerPipelineResultBundle 에서 키에 해당하는 파일이 실제로 존재하는지 확인 */
internal fun ServerPipelineResultBundle.existingFile(key: String): File? =
    filesByKey[key]?.takeIf { it.exists() && it.isFile }

/** URI 를 디코딩 → base64 변환 후 Bitmap 을 안전하게 재활용합니다. */
internal suspend fun decodeUriToBase64(context: Context, uri: Uri, maxDim: Int = 1280): String? {
    var bitmap: Bitmap? = null
    return try {
        bitmap = decodeBitmapWithMaxDimension(context, uri, maxDim)
        bitmap?.let { ClaudeChatClient.bitmapToBase64ForLlm(it) }
    } catch (_: Exception) {
        null
    } finally {
        try {
            bitmap?.let { if (!it.isRecycled) it.recycle() }
        } catch (_: Exception) {
        }
    }
}
