package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

/**
 * HTML 보고서의 깨진·placeholder `<img src>`를 기기 첨부 사진의 **data URI**로 치환합니다.
 * LLM이 `topview.png` 등 상대 경로나 빈 base64만 넣는 경우가 많아, 저장·WebView 로드 시 앱이 보강합니다.
 */
object HtmlReportImageEmbedder {

    private const val HTML_EMBED_MAX_IMAGES = 12
    private const val HTML_EMBED_MAX_DIM = 1024
    private const val HTML_EMBED_JPEG_QUALITY = 78

    enum class ProjectionKind { TOPVIEW, SIDEVIEW }

    private data class Slot(
        val kind: ProjectionKind?,
        val dataUri: String,
    )

    fun classifyProjectionUri(uri: Uri): ProjectionKind? {
        val path = uri.path?.lowercase(Locale.US) ?: return null
        val base = path.substringAfterLast('/')
        return when {
            base.contains("topview") || base.contains("top_view") -> ProjectionKind.TOPVIEW
            base.contains("sideview") || base.contains("side_view") -> ProjectionKind.SIDEVIEW
            path.contains("topview") || path.contains("top_view") -> ProjectionKind.TOPVIEW
            path.contains("sideview") || path.contains("side_view") -> ProjectionKind.SIDEVIEW
            else -> null
        }
    }

    fun resolveTopAndSideUris(uris: List<Uri>): Pair<Uri?, Uri?> {
        var top: Uri? = null
        var side: Uri? = null
        for (uri in uris) {
            when (classifyProjectionUri(uri)) {
                ProjectionKind.TOPVIEW -> if (top == null) top = uri
                ProjectionKind.SIDEVIEW -> if (side == null) side = uri
                null -> Unit
            }
        }
        if (top == null || side == null) {
            val raster = uris.filter { uri ->
                val p = uri.path?.lowercase(Locale.US) ?: return@filter false
                p.endsWith(".png") || p.endsWith(".jpg") || p.endsWith(".jpeg") || p.endsWith(".webp")
            }
            if (top == null && raster.isNotEmpty()) top = raster[0]
            if (side == null && raster.size >= 2) side = raster[1]
        }
        return top to side
    }

    /** @deprecated 이름 호환 — [embedAttachedImages] 사용 */
    fun embedProjectionImages(
        context: Context,
        html: String,
        outDir: File,
        projectionImageUris: List<Uri>,
    ): String = embedAttachedImages(context, html, outDir, projectionImageUris)

    /**
     * HTML과 같은 [outDir]에 원본을 복사(선택)하고, 모든 깨진 `<img>`를 **data:image/...;base64,...** 로 치환합니다.
     */
    fun embedAttachedImages(
        context: Context,
        html: String,
        outDir: File,
        imageUris: List<Uri>,
        maxImages: Int = HTML_EMBED_MAX_IMAGES,
    ): String {
        if (imageUris.isEmpty()) return html
        val ordered = buildOrderedDistinctUris(imageUris, maxImages)
        if (ordered.isEmpty()) return html

        outDir.mkdirs()
        copySourcesBesideHtml(context, ordered, outDir)

        val slots = ordered.mapNotNull { uri ->
            uriToDataUri(context, uri)?.let { Slot(classifyProjectionUri(uri), it) }
        }
        if (slots.isEmpty()) return html
        return patchHtmlImageSources(html, slots)
    }

    fun htmlNeedsImageEmbedPatch(html: String): Boolean = htmlNeedsProjectionPatch(html)

    fun htmlNeedsProjectionPatch(html: String): Boolean {
        val imgRegex = Regex("""<img\s+[^>]*>""", RegexOption.IGNORE_CASE)
        for (match in imgRegex.findAll(html)) {
            val tag = match.value
            val src = extractAttr(tag, "src") ?: return true
            if (isBrokenImageSrc(src)) return true
        }
        return false
    }

    private fun buildOrderedDistinctUris(uris: List<Uri>, maxImages: Int): List<Uri> {
        val (topUri, sideUri) = resolveTopAndSideUris(uris)
        val ordered = mutableListOf<Uri>()
        val seen = mutableSetOf<String>()
        fun add(uri: Uri?) {
            if (uri == null) return
            val key = uri.toString()
            if (!seen.add(key)) return
            ordered.add(uri)
        }
        add(topUri)
        add(sideUri)
        for (uri in uris) add(uri)
        return ordered.take(maxImages)
    }

    private fun copySourcesBesideHtml(context: Context, uris: List<Uri>, outDir: File) {
        uris.forEachIndexed { index, uri ->
            copyUriToReportFile(context, uri, outDir, "report_photo_${index + 1}.${extensionForUri(uri)}")
        }
    }

    private fun extensionForUri(uri: Uri): String {
        val path = uri.path?.lowercase(Locale.US).orEmpty()
        return when {
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "jpg"
            path.endsWith(".webp") -> "webp"
            else -> "png"
        }
    }

    private fun copyUriToReportFile(
        context: Context,
        uri: Uri,
        outDir: File,
        destName: String,
    ) {
        try {
            val dest = File(outDir, destName)
            when (uri.scheme?.lowercase(Locale.US)) {
                "file" -> {
                    val src = File(uri.path ?: return)
                    if (src.isFile) src.copyTo(dest, overwrite = true)
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("HtmlReportImageEmbedder", "copy failed: ${e.message}")
        }
    }

    private fun uriToDataUri(context: Context, uri: Uri): String? {
        var bitmap: Bitmap? = null
        return try {
            bitmap = decodeBitmapWithMaxDimension(context, uri, HTML_EMBED_MAX_DIM)
            if (bitmap == null) {
                readRawBytesDataUri(context, uri)
            } else {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, HTML_EMBED_JPEG_QUALITY, stream)
                val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                "data:image/jpeg;base64,$b64"
            }
        } catch (e: Exception) {
            android.util.Log.w("HtmlReportImageEmbedder", "data uri failed: ${e.message}")
            null
        } finally {
            try {
                bitmap?.let { if (!it.isRecycled) it.recycle() }
            } catch (_: Exception) {
            }
        }
    }

    private fun readRawBytesDataUri(context: Context, uri: Uri): String? {
        val bytes = when (uri.scheme?.lowercase(Locale.US)) {
            "file" -> File(uri.path ?: return null).takeIf { it.isFile }?.readBytes()
            else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return null
        val path = uri.path?.lowercase(Locale.US).orEmpty()
        val mime = when {
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".webp") -> "image/webp"
            else -> "image/jpeg"
        }
        return "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    private fun patchHtmlImageSources(html: String, slots: List<Slot>): String {
        val topSlot = slots.firstOrNull { it.kind == ProjectionKind.TOPVIEW }
        val sideSlot = slots.firstOrNull { it.kind == ProjectionKind.SIDEVIEW }
        val generalQueue = ArrayDeque(slots.map { it.dataUri })

        val imgRegex = Regex("""<img\s+([^>]*?)>""", RegexOption.IGNORE_CASE)
        var topAssigned = false
        var sideAssigned = false

        return imgRegex.replace(html) { match ->
            val attrs = match.groupValues[1]
            val src = extractAttr(attrs, "src")
            if (src != null && !isBrokenImageSrc(src)) return@replace match.value

            val kind = detectProjectionKind(attrs)
            val newSrc = when (kind) {
                ProjectionKind.TOPVIEW -> {
                    topAssigned = true
                    topSlot?.dataUri ?: generalQueue.removeFirstOrNull()
                }
                ProjectionKind.SIDEVIEW -> {
                    sideAssigned = true
                    sideSlot?.dataUri ?: generalQueue.removeFirstOrNull()
                }
                null -> null
            } ?: when {
                !topAssigned && topSlot != null -> {
                    topAssigned = true
                    topSlot.dataUri
                }
                !sideAssigned && sideSlot != null -> {
                    sideAssigned = true
                    sideSlot.dataUri
                }
                else -> generalQueue.removeFirstOrNull()
            }

            if (newSrc.isNullOrBlank()) match.value
            else rebuildImgTag(attrs, newSrc)
        }
    }

    private fun detectProjectionKind(attrs: String): ProjectionKind? {
        val hay = attrs.lowercase(Locale.US)
        return when {
            hay.contains("topview") || hay.contains("top view") || hay.contains("top_view") ||
                hay.contains("top view projection") || hay.contains("상향") || hay.contains("bird") ||
                hay.contains("조감") -> ProjectionKind.TOPVIEW
            hay.contains("sideview") || hay.contains("side view") || hay.contains("side_view") ||
                hay.contains("side view projection") || hay.contains("측면") || hay.contains("elevation") ||
                hay.contains("측면도") -> ProjectionKind.SIDEVIEW
            hay.contains("projection") && hay.contains("top") -> ProjectionKind.TOPVIEW
            hay.contains("projection") && hay.contains("side") -> ProjectionKind.SIDEVIEW
            else -> null
        }
    }

    internal fun isBrokenImageSrc(src: String): Boolean {
        val s = src.trim()
        if (s.isEmpty()) return true
        if (s.startsWith("embed:", ignoreCase = true)) return true
        if (s.startsWith("app:", ignoreCase = true)) return true
        if (s.startsWith("data:image", ignoreCase = true)) {
            val payload = s.substringAfter("base64,", "")
            if (payload.isEmpty()) return true
            if (payload.length < 200) return true
            if (payload.contains("...") || payload.contains("PLACEHOLDER", ignoreCase = true)) return true
            if (payload.contains("YOUR_BASE64", ignoreCase = true)) return true
            if (payload.contains("BASE64_HERE", ignoreCase = true)) return true
            return false
        }
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            return true
        }
        return true
    }

    private fun extractAttr(attrs: String, name: String): String? {
        val regex = Regex("""\b${Regex.escape(name)}\s*=\s*("([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
        val m = regex.find(attrs) ?: return null
        return m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
            ?: m.groupValues.getOrNull(3)
    }

    private fun rebuildImgTag(attrs: String, newSrc: String): String {
        val withoutSrc = attrs.replace(
            Regex("""\bsrc\s*=\s*("[^"]*"|'[^']*')""", RegexOption.IGNORE_CASE),
            "",
        ).trim()
        return if (withoutSrc.isEmpty()) """<img src="$newSrc">"""
        else """<img src="$newSrc" $withoutSrc>"""
    }
}
