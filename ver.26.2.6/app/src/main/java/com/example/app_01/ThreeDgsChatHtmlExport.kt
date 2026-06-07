package com.example.app_01

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LLM이 출력한 HTML 보고서(```html 펜스 또는 완전한 HTML 문서)를 추출해
 * 기기에 .html로 저장합니다. WebView에서 이미지·표·Chart.js 그래프를 렌더링합니다.
 */
object ThreeDgsChatHtmlExport {

    data class HtmlExportResult(
        val htmlFile: File,
        val extractedFromFence: Boolean,
    )

    data class HtmlDownloadsSaveResult(
        val fileName: String,
        val displayPath: String,
    )

    fun tryExportToHtml(
        context: Context,
        fullMarkdown: String,
        subdirectory: String = "3dgs_llm_exports",
        fileBasePrefix: String = "3dgs_report",
        docTitle: String = "3DGS 분석 보고서",
        /** 보고서 `<img>` 보강용 첨부·DA3 투영 이미지 URI (앱이 data URI로 삽입) */
        attachmentImageUris: List<Uri> = emptyList(),
    ): HtmlExportResult? {
        val prepared = prepareHtmlContent(
            context = context,
            fullMarkdown = fullMarkdown,
            subdirectory = subdirectory,
            docTitle = docTitle,
            attachmentImageUris = attachmentImageUris,
        ) ?: return null
        return try {
            val htmlFile = File(prepared.outDir, "${fileBasePrefix}_${prepared.stamp}.html")
            htmlFile.writeText(prepared.html, Charsets.UTF_8)
            HtmlExportResult(htmlFile = htmlFile, extractedFromFence = prepared.fromFence)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** HTML 보고서를 기기 **다운로드** 폴더에 저장합니다. */
    fun trySaveToDownloads(
        context: Context,
        fullMarkdown: String,
        subdirectory: String = "3dgs_llm_exports",
        fileBasePrefix: String = "3dgs_report",
        docTitle: String = "3DGS 분석 보고서",
        attachmentImageUris: List<Uri> = emptyList(),
    ): HtmlDownloadsSaveResult? {
        val prepared = prepareHtmlContent(
            context = context,
            fullMarkdown = fullMarkdown,
            subdirectory = subdirectory,
            docTitle = docTitle,
            attachmentImageUris = attachmentImageUris,
        ) ?: return null
        val fileName = "${fileBasePrefix}_${prepared.stamp}.html"
        return saveHtmlContentToDownloads(context, prepared.html, fileName)
    }

    private data class PreparedHtml(
        val html: String,
        val outDir: File,
        val stamp: String,
        val fromFence: Boolean,
    )

    private fun prepareHtmlContent(
        context: Context,
        fullMarkdown: String,
        subdirectory: String,
        docTitle: String,
        attachmentImageUris: List<Uri>,
    ): PreparedHtml? {
        val raw = extractHtmlDocument(fullMarkdown) ?: return null
        return try {
            val outDir = File(context.getExternalFilesDir(null), subdirectory).apply { mkdirs() }
            val stamp = timestampCompact()
            val fromFence = extractFirstHtmlFence(fullMarkdown) != null
            var finalHtml = ensureCompleteHtmlDocument(raw, docTitle)
            if (attachmentImageUris.isNotEmpty()) {
                finalHtml = HtmlReportImageEmbedder.embedAttachedImages(
                    context,
                    finalHtml,
                    outDir,
                    attachmentImageUris,
                )
            }
            PreparedHtml(html = finalHtml, outDir = outDir, stamp = stamp, fromFence = fromFence)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveHtmlContentToDownloads(
        context: Context,
        htmlContent: String,
        fileName: String,
    ): HtmlDownloadsSaveResult? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(htmlContent.toByteArray(Charsets.UTF_8))
                } ?: run {
                    resolver.delete(uri, null, null)
                    return null
                }
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
                HtmlDownloadsSaveResult(
                    fileName = fileName,
                    displayPath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists() && !dir.mkdirs()) return null
                val outFile = File(dir, fileName)
                outFile.writeText(htmlContent, Charsets.UTF_8)
                HtmlDownloadsSaveResult(
                    fileName = fileName,
                    displayPath = outFile.absolutePath,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun extractHtmlDocument(markdown: String): String? {
        extractFirstHtmlFence(markdown)?.let { return it }
        val trimmed = markdown.trim()
        if (looksLikeHtmlDocument(trimmed)) return trimmed
        return null
    }

    fun extractFirstHtmlFence(markdown: String): String? {
        if (markdown.isBlank()) return null
        val text = markdown.replace("\r\n", "\n")

        val labeled = Regex(
            """```\s*(?:html|htm)\s*\n?([\s\S]*?)```""",
            RegexOption.IGNORE_CASE,
        )
        labeled.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val generic = Regex("```[a-z0-9+#.\\-]{0,48}\\s*\\n?([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        for (m in generic.findAll(text)) {
            val body = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (body.isNotBlank() && looksLikeHtmlDocument(body)) return body
        }

        val unclosed = Regex(
            """```\s*(?:html|htm)\s*\n?([\s\S]+)""",
            RegexOption.IGNORE_CASE,
        )
        unclosed.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { body ->
            val cleaned = body.removeSuffix("```").trim()
            if (cleaned.isNotBlank()) return cleaned
        }

        return null
    }

    fun shouldOfferHtmlExportForCodeBlock(language: String, code: String): Boolean {
        val lang = language.trim().lowercase(Locale.US)
        if (lang == "html" || lang == "htm") return true
        if (lang.isEmpty() || lang == "plaintext" || lang == "text") {
            return code.isNotBlank() && looksLikeHtmlDocument(code)
        }
        return code.isNotBlank() && looksLikeHtmlDocument(code)
    }

    fun looksLikeHtmlDocument(s: String): Boolean {
        val low = s.trim().lowercase(Locale.US)
        if (low.startsWith("<!doctype html") || low.startsWith("<html")) return true
        return "<body" in low && ("<head" in low || "<style" in low || "<main" in low || "<section" in low)
    }

    fun ensureCompleteHtmlDocument(bodyOrDoc: String, title: String): String {
        val trimmed = bodyOrDoc.trim()
        if (looksLikeHtmlDocument(trimmed) && trimmed.lowercase(Locale.US).contains("<html")) {
            return trimmed
        }
        return wrapInPresentationShell(trimmed, title)
    }

    private fun wrapInPresentationShell(bodyHtml: String, title: String): String {
        val safeTitle = escapeHtml(title)
        return """
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>$safeTitle</title>
<style>
${presentationBaseCss()}
</style>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body>
<header class="report-cover">
  <h1>$safeTitle</h1>
  <p class="report-meta">Mobile 분석 보고서 · ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN).format(Date())}</p>
</header>
<main class="report-body">
$bodyHtml
</main>
<footer class="report-footer">
  <p>본 문서는 AI가 생성한 참고용 분석 템플릿이며 법적 효력이 없습니다.</p>
</footer>
</body>
</html>
""".trimIndent()
    }

    fun presentationBaseCss(): String = """
:root {
  --primary: #1B4F8A;
  --text: #1A1A1A;
  --muted: #5a6472;
  --bg: #f4f7fb;
  --card: #ffffff;
  --border: #d8e2ef;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: "Noto Sans KR", "Malgun Gothic", sans-serif;
  color: var(--text);
  background: var(--bg);
  line-height: 1.65;
}
.report-cover {
  min-height: 72vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  background: linear-gradient(160deg, #0f2d52 0%, var(--primary) 55%, #2a6cb8 100%);
  color: #fff;
  padding: 48px 24px;
  page-break-after: always;
}
.report-cover h1 { font-size: clamp(1.6rem, 5vw, 2.4rem); margin: 0 0 12px; }
.report-meta { opacity: 0.9; font-size: 0.95rem; }
.report-body { max-width: 920px; margin: 0 auto; padding: 24px 16px 48px; }
.report-body > section,
.report-body > article,
.slide {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(27, 79, 138, 0.06);
  page-break-after: always;
  min-height: 60vh;
}
h1, h2, h3 { color: var(--primary); margin-top: 0; }
h2 { border-bottom: 2px solid var(--primary); padding-bottom: 8px; }
table {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 0.92rem;
}
th, td {
  border: 1px solid var(--border);
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
}
th { background: #e8f0fe; color: var(--primary); }
tr:nth-child(even) td { background: #f5f8fc; }
img, svg, canvas {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 12px auto;
  border-radius: 8px;
}
.chart-wrap { position: relative; height: 280px; margin: 16px 0; }
ul, ol { padding-left: 1.4rem; }
.report-footer {
  text-align: center;
  color: var(--muted);
  font-size: 0.85rem;
  padding: 24px 16px 40px;
}
@media print {
  body { background: #fff; }
  .slide, section, article { box-shadow: none; page-break-after: always; }
}
""".trimIndent()

    private fun escapeHtml(text: String): String = buildString(text.length + 8) {
        for (ch in text) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(ch)
            }
        }
    }
}
