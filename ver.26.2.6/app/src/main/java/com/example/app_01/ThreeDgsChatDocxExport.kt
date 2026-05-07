package com.example.app_01

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * LLM이 출력한 `python-docx` 스크립트에서 아래 패턴의 **문자열 리터럴**을 순서대로 추출해
 * 기기에서 최소 OOXML(.docx)을 조립합니다. (Python 런타임 없음)
 *
 * - `.add_heading("…", level=…)`
 * - `.add_paragraph("…")` (첫 인자가 문자열인 호출만)
 * - `.add_run("…")` (표·문단 런 — LLM이 자주 사용)
 * - `…text = "…"` / `…text = '…'` (`table.cell` / `cells[i].text` 등 **할당**만; `==` 제외)
 *
 * 괄호 짝 맞추기는 문자열·`#` 주석 안의 괄호를 무시하므로, 본문에 `(` `)` 가 있어도 이후 코드가 통째로
 * 누락되지 않습니다. (이전 구현의 잘림 버그 수정)
 */
object ThreeDgsChatDocxExport {

    sealed class DocPiece {
        data class Heading(val level: Int, val text: String) : DocPiece()
        data class Paragraph(val text: String) : DocPiece()
    }

    data class ExportResult(
        val scriptFile: File,
        val docxFile: File,
        val extractedPieceCount: Int,
    )

    /**
     * IO 스레드에서 호출하세요.
     * @param subdirectory [getExternalFilesDir] 하위 폴더명
     * @param fileBasePrefix 저장 파일 접두어
     * @param mirrorDocTitle·mirrorIntro OOXML 미러 문서의 표제·안내 문단
     */
    fun tryExportToFiles(
        context: Context,
        fullMarkdown: String,
        subdirectory: String = "3dgs_llm_exports",
        fileBasePrefix: String = "3dgs_report",
        mirrorDocTitle: String = "3DGS 분석 보고서 (LLM 스크립트 미러)",
        mirrorIntro: String =
            "이 문서는 기기에서 Python을 실행하지 않고, LLM 스크립트에서 " +
                "add_heading · add_paragraph · add_run 의 문자열 인자와 .text = \"…\" 셀 할당만 " +
                "순서대로 추출해 자동 생성했습니다. (표 레이아웃은 동일하지 않을 수 있으나 본문·표 텍스트는 위 패턴으로 수집됩니다.)",
    ): ExportResult? {
        val py = extractFirstPythonFence(fullMarkdown) ?: return null
        return try {
            val outDir = File(context.getExternalFilesDir(null), subdirectory).apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val base = "${fileBasePrefix}_$stamp"
            val pyFile = File(outDir, "$base.py")
            val docxFile = File(outDir, "$base.docx")
            pyFile.writeText(py, Charsets.UTF_8)
            val pieces = extractPiecesFromPythonDocxScript(py)
            val xml = buildDocumentXml(
                pieces = pieces,
                mirrorDocTitle = mirrorDocTitle,
                mirrorIntro = mirrorIntro,
            )
            writeZipDocx(docxFile, xml)
            ExportResult(scriptFile = pyFile, docxFile = docxFile, extractedPieceCount = pieces.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun extractFirstPythonFence(markdown: String): String? {
        if (markdown.isBlank()) return null
        val text = markdown.replace("\r\n", "\n")

        // 펜스 언어: python, py, python3, python-docx 등 (LLM 출력 차이)
        val pyOpenTag = "(?:python-docx|python3?|py\\d*|python)\\b"

        // 1) 라벨된 닫힌 펜스
        val labeled = Regex(
            "```\\s*$pyOpenTag[\\s\\n]*([\\s\\S]*?)```",
            RegexOption.IGNORE_CASE,
        )
        labeled.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        // 2) 언어 태그 없거나 기타 태그 — 줄바꿈 없이 코드가 이어지는 경우도 허용
        val generic = Regex("```[a-z0-9+#.\\-]{0,48}\\s*\\n?([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        for (m in generic.findAll(text)) {
            val body = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (body.isNotBlank() && looksLikePythonDocxScript(body)) return body
        }

        // 3) 닫는 ``` 없이 끝나는 응답(스트림 잘림 등)
        val unclosed = Regex(
            "```\\s*$pyOpenTag[\\s\\n]*([\\s\\S]+)",
            RegexOption.IGNORE_CASE,
        )
        unclosed.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { body ->
            val cleaned = body.removeSuffix("```").trim()
            if (cleaned.isNotBlank()) return cleaned
        }

        return null
    }

    /** AiChat 마크다운: 이 코드 블록에 Word 내보내기 버튼을 붙일지 */
    fun shouldOfferDocxExportForCodeBlock(language: String, code: String): Boolean {
        val lang = language.trim().lowercase(Locale.US)
        if (lang == "python" || lang == "py") return true
        if (lang == "python-docx" || lang.startsWith("python") || Regex("^py\\d*$").matches(lang)) return true
        if (lang.isEmpty() || lang == "plaintext" || lang == "text") {
            return code.isNotBlank() && looksLikePythonDocxScript(code)
        }
        return false
    }

    private fun looksLikePythonDocxScript(s: String): Boolean {
        val low = s.lowercase(Locale.US)
        return "from docx" in low || "import docx" in low ||
            "document(" in low || "python-docx" in low ||
            ".add_paragraph" in low || ".add_heading" in low || ".add_run" in low ||
            (".text" in low && "=" in low) ||
            "#!/usr/bin/env python" in low || "#!/usr/bin/env python3" in low ||
            "from docx import" in low || "from docx." in low
    }

    private data class Extracted(val index: Int, val piece: DocPiece)

    fun extractPiecesFromPythonDocxScript(py: String): List<DocPiece> {
        val raw = ArrayList<Extracted>()
        collectCallLiterals(py, ".add_heading", raw, isHeading = true)
        collectCallLiterals(py, ".add_paragraph", raw, isHeading = false)
        collectCallLiterals(py, ".add_run", raw, isHeading = false)
        collectTextAssignments(py, raw)
        if (raw.isEmpty()) return emptyList()
        return raw.sortedBy { it.index }.map { it.piece }
    }

    private fun collectCallLiterals(py: String, needle: String, out: MutableList<Extracted>, isHeading: Boolean) {
        var search = 0
        while (true) {
            val found = py.indexOf(needle, search, ignoreCase = true)
            if (found < 0) return
            val open = py.indexOf('(', found)
            if (open < 0) {
                search = found + needle.length
                continue
            }
            val parsed = readFirstStringArg(py, open + 1) ?: run {
                search = open + 1
                continue
            }
            val (text, afterStrEnd) = parsed
            val close = indexOfMatchingCloseParen(py, open)
            if (close <= open) {
                search = open + 1
                continue
            }
            if (isHeading) {
                val level = extractLevel(py, afterStrEnd, close)
                out.add(Extracted(found, DocPiece.Heading(level.coerceIn(0, 3), text)))
            } else {
                out.add(Extracted(found, DocPiece.Paragraph(text)))
            }
            search = close + 1
        }
    }

    private fun collectTextAssignments(py: String, out: MutableList<Extracted>) {
        var search = 0
        while (true) {
            val idx = py.indexOf(".text", search)
            if (idx < 0) return
            var j = idx + 5
            j = skipWs(py, j)
            if (j >= py.length) {
                search = idx + 5
                continue
            }
            if (py[j] != '=' || py.getOrNull(j + 1) == '=') {
                search = idx + 5
                continue
            }
            j = skipWs(py, j + 1)
            val parsed = readFirstStringArg(py, j) ?: run {
                search = idx + 5
                continue
            }
            out.add(Extracted(idx, DocPiece.Paragraph(parsed.first)))
            search = parsed.second
        }
    }

    /** 문자열·`#` 줄주석 안의 괄호는 깊이 계산에서 제외 */
    private fun indexOfMatchingCloseParen(s: String, openParen: Int): Int {
        require(openParen < s.length && s[openParen] == '(')
        var depth = 1
        var i = openParen + 1
        var state = ParenScanState.NORMAL
        while (i < s.length && depth > 0) {
            val c = s[i]
            when (state) {
                ParenScanState.NORMAL -> {
                    when {
                        c == '#' -> {
                            while (i < s.length && s[i] != '\n' && s[i] != '\r') i++
                            if (i < s.length) i++
                        }
                        c == '"' -> {
                            if (s.startsWith("\"\"\"", i)) {
                                state = ParenScanState.TRIPLE_DQUOTE
                                i += 3
                            } else {
                                state = ParenScanState.DQUOTE
                                i++
                            }
                        }
                        c == '\'' -> {
                            if (s.startsWith("'''", i)) {
                                state = ParenScanState.TRIPLE_SQUOTE
                                i += 3
                            } else {
                                state = ParenScanState.SQUOTE
                                i++
                            }
                        }
                        c == '(' -> {
                            depth++
                            i++
                        }
                        c == ')' -> {
                            depth--
                            if (depth == 0) return i
                            i++
                        }
                        else -> i++
                    }
                }
                ParenScanState.DQUOTE -> {
                    when {
                        c == '\\' && i + 1 < s.length -> i += 2
                        c == '"' -> {
                            state = ParenScanState.NORMAL
                            i++
                        }
                        else -> i++
                    }
                }
                ParenScanState.SQUOTE -> {
                    when {
                        c == '\\' && i + 1 < s.length -> i += 2
                        c == '\'' -> {
                            state = ParenScanState.NORMAL
                            i++
                        }
                        else -> i++
                    }
                }
                ParenScanState.TRIPLE_DQUOTE -> {
                    if (s.startsWith("\"\"\"", i)) {
                        state = ParenScanState.NORMAL
                        i += 3
                    } else {
                        i++
                    }
                }
                ParenScanState.TRIPLE_SQUOTE -> {
                    if (s.startsWith("'''", i)) {
                        state = ParenScanState.NORMAL
                        i += 3
                    } else {
                        i++
                    }
                }
            }
        }
        return if (depth == 0) i - 1 else s.lastIndex
    }

    private enum class ParenScanState {
        NORMAL, DQUOTE, SQUOTE, TRIPLE_DQUOTE, TRIPLE_SQUOTE
    }

    private fun readFirstStringArg(py: String, afterOpenParen: Int): Pair<String, Int>? {
        var p = skipWs(py, afterOpenParen)
        p = skipPythonStringPrefix(py, p)
        return readPythonStringLiteral(py, p)
    }

    /** r/f/b/u 및 rf 등(최대 2글자) 프롕트 */
    private fun skipPythonStringPrefix(s: String, start: Int): Int {
        var i = start
        var n = 0
        while (i < s.length && n < 2) {
            val c = s[i]
            if (c == 'r' || c == 'R' || c == 'f' || c == 'F' ||
                c == 'b' || c == 'B' || c == 'u' || c == 'U'
            ) {
                i++
                n++
            } else {
                break
            }
        }
        return i
    }

    private fun skipWs(s: String, from: Int): Int {
        var j = from
        while (j < s.length && s[j].isWhitespace()) j++
        return j
    }

    private fun extractLevel(s: String, from: Int, closeParenIdx: Int): Int {
        val end = closeParenIdx.coerceAtMost(s.length).coerceAtLeast(from)
        if (from >= end) return 1
        val slice = s.substring(from, end)
        return Regex("""level\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(slice)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
    }

    private fun readPythonStringLiteral(s: String, start: Int): Pair<String, Int>? {
        var i = start
        if (i >= s.length) return null
        when {
            s.startsWith("\"\"\"", i) -> {
                val end = s.indexOf("\"\"\"", i + 3)
                if (end < 0) return null
                return unescapeTriple(s.substring(i + 3, end)) to end + 3
            }
            s.startsWith("'''", i) -> {
                val end = s.indexOf("'''", i + 3)
                if (end < 0) return null
                return unescapeTriple(s.substring(i + 3, end)) to end + 3
            }
        }
        val q = s[i]
        if (q != '"' && q != '\'') return null
        i++
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\', '"', '\'' -> sb.append(n)
                    else -> {
                        sb.append('\\')
                        sb.append(n)
                    }
                }
                i += 2
                continue
            }
            if (c == q) return sb.toString() to (i + 1)
            sb.append(c)
            i++
        }
        return null
    }

    private fun unescapeTriple(t: String): String =
        t.replace("\\\"", "\"").replace("\\'", "'")

    /** WordprocessingML 1.0 — 잘못된 제어 문자는 공백으로 치환 */
    private fun sanitizeXmlText(input: String): String = buildString(input.length) {
        for (ch in input) {
            val cp = ch.code
            when {
                ch == '\t' || ch == '\n' || ch == '\r' -> append(ch)
                cp >= 0x20 && cp <= 0xD7FF -> append(ch)
                cp in 0xE000..0xFFFD -> append(ch)
                else -> append(' ')
            }
        }
    }

    private fun escapeXml(text: String): String {
        val clean = sanitizeXmlText(text)
        return buildString(clean.length + 8) {
            for (ch in clean) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }
    }

    private fun headingParagraph(title: String, level: Int): String {
        val sz = when (level) {
            0 -> 36
            1 -> 30
            2 -> 26
            else -> 24
        }
        return "<w:p>" +
            "<w:pPr><w:jc w:val=\"left\"/></w:pPr>" +
            "<w:r><w:rPr><w:b/><w:sz w:val=\"$sz\"/></w:rPr>" +
            "<w:t xml:space=\"preserve\">" + escapeXml(title) + "</w:t>" +
            "</w:r></w:p>\n"
    }

    private fun bodyParagraph(line: String): String =
        "<w:p><w:r><w:t xml:space=\"preserve\">" +
            escapeXml(if (line.isEmpty()) " " else line) +
            "</w:t></w:r></w:p>\n"

    private fun buildDocumentXml(
        pieces: List<DocPiece>,
        mirrorDocTitle: String = "3DGS 분석 보고서 (LLM 스크립트 미러)",
        mirrorIntro: String =
            "이 문서는 기기에서 Python을 실행하지 않고, LLM 스크립트에서 " +
                "add_heading · add_paragraph · add_run 의 문자열 인자와 .text = \"…\" 셀 할당만 " +
                "순서대로 추출해 자동 생성했습니다. (표 레이아웃은 동일하지 않을 수 있으나 본문·표 텍스트는 위 패턴으로 수집됩니다.)",
    ): String {
        val sb = StringBuilder(32_768)
        sb.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ",
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">",
            "<w:body>\n",
        )
        sb.append(
            headingParagraph(mirrorDocTitle, 0),
            bodyParagraph(mirrorIntro),
        )
        for (piece in pieces) {
            when (piece) {
                is DocPiece.Heading -> sb.append(headingParagraph(piece.text, piece.level))
                is DocPiece.Paragraph -> {
                    for (line in piece.text.split('\n')) {
                        sb.append(bodyParagraph(line))
                    }
                }
            }
        }
        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun contentTypesXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" " +
            "ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" " +
            (
                "ContentType=\"application/vnd.openxmlformats-officedocument." +
                    "wordprocessingml.document.main+xml\"/>"
            ) +
            "<Override PartName=\"/word/styles.xml\" " +
            "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>" +
            "</Types>"

    private fun rootRelsXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"" +
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" " +
            "Target=\"word/document.xml\"/>" +
            "</Relationships>"

    private fun documentRelsXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"" +
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" " +
            "Target=\"styles.xml\"/>" +
            "</Relationships>"

    private fun stylesXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:docDefaults><w:rPrDefault><w:rPr/></w:rPrDefault></w:docDefaults>" +
            "</w:styles>"

    private fun writeZipDocx(outFile: File, documentXml: String) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
            fun put(name: String, utf8: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(utf8.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            put("[Content_Types].xml", contentTypesXml())
            put("_rels/.rels", rootRelsXml())
            put("word/_rels/document.xml.rels", documentRelsXml())
            put("word/styles.xml", stylesXml())
            put("word/document.xml", documentXml)
        }
    }
}
