package com.example.app_01

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 서버 3DGS·PLY 파이프라인 결과를 **로컬에서** OOXML(.docx)로 정리하고,
 * PC에서 `python-docx`로 동일 보고서를 재생성할 수 있는 **Python 스크립트**를 함께 둡니다.
 */
object PoliceInsuranceDocxWriter {

    private const val MAX_BODY_CHARS_PER_SECTION = 48_000

    private fun escapeXml(text: String): String = buildString(text.length + 8) {
        for (ch in text) {
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

    /** 긴 본문을 Word가 잘 받아들이도록 줄 단위 문단으로 나눈다. */
    private fun linesToParagraphs(lines: List<String>): String = buildString {
        for (line in lines) {
            append("<w:p>")
            append("<w:r><w:t xml:space=\"preserve\">")
            append(escapeXml(line.ifEmpty { " " }))
            append("</w:t></w:r></w:p>\n")
        }
    }

    private fun headingParagraph(title: String): String =
        "<w:p>" +
            "<w:pPr><w:jc w:val=\"left\"/></w:pPr>" +
            "<w:r><w:rPr><w:b/><w:sz w:val=\"28\"/></w:rPr>" +
            "<w:t xml:space=\"preserve\">" + escapeXml(title) + "</w:t>" +
            "</w:r></w:p>\n"

    private fun buildDocumentXml(sections: List<Pair<String, String>>): String {
        val sb = StringBuilder(64_000)
        sb.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ",
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">",
            "<w:body>\n",
        )
        sb.append(
            headingParagraph("3DGS·PLY 분석 보고서 (경찰·보험 참고용)") +
                "<w:p><w:r><w:t xml:space=\"preserve\">" +
                escapeXml(
                    "자동 생성 문서입니다. 첨부 서버 결과(JSON·CSV·품질 리포트 등)를 바탕으로 정리했습니다.",
                ) +
                "</w:t></w:r></w:p>\n",
        )
        for ((title, body) in sections) {
            sb.append(headingParagraph(title))
            val trimmed =
                if (body.length > MAX_BODY_CHARS_PER_SECTION) {
                    body.take(MAX_BODY_CHARS_PER_SECTION) +
                        "\n\n… (이하 생략, 원문 ${body.length}자)"
                } else body
            val lines = trimmed.split('\n')
            sb.append(linesToParagraphs(lines))
        }
        sb.append(
            "</w:body></w:document>",
        )
        return sb.toString()
    }

    private fun contentTypesXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" " +
            "ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" " +
            "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
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

    private fun collectSections(bundle: ServerPipelineResultBundle): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val meta = buildString {
            appendLine("task_id: ${bundle.taskId}")
            appendLine("다운로드된 파일 키: ${bundle.filesByKey.keys.sorted().joinToString(", ")}")
            val ply = bundle.plyFile
            if (ply.exists()) {
                appendLine()
                appendLine("[PLY] ${ply.name} (${ply.length() / 1024} KB)")
                appendLine(ply.absolutePath)
            }
            bundle.filesByKey["glb"]?.takeIf { it.exists() }?.let { glb ->
                appendLine()
                appendLine("[GLB] ${glb.name} (${glb.length() / 1024} KB)")
                appendLine(glb.absolutePath)
            }
        }
        out.add("1. 서버 결과 메타" to meta.trim())

        bundle.filesByKey["analysis_json"]?.takeIf { it.exists() && it.isFile }?.let { f ->
            try {
                f.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
                out.add("2. 분석 JSON (analysis_result.json)\n원본: ${f.name}" to raw)
            }
        }

        val qualityJson = bundle.filesByKey["quality_json"]?.takeIf { it.exists() && it.isFile }
        val qualityTxt = bundle.filesByKey["quality_txt"]?.takeIf { it.exists() && it.isFile }
        val qFile = qualityJson ?: qualityTxt
        if (qFile != null) {
            val raw = try {
                qFile.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
            if (!raw.isNullOrBlank()) {
                val title = if (qualityJson != null && qFile == qualityJson) {
                    "3. 포인트 클라우드 품질 (quality_report.json)"
                } else {
                    "3. 포인트 클라우드 품질 (레거시 quality_report.txt)"
                }
                val body = if (qualityJson != null && qFile == qualityJson) {
                    val summary = parsePointCloudQualityReportJson(qFile)?.let { formatPointCloudQualityReportKorean(it) }
                    buildString {
                        if (!summary.isNullOrBlank()) {
                            appendLine(summary)
                            appendLine()
                            appendLine("--- 원본 JSON ---")
                            appendLine()
                        }
                        append(raw.trim())
                    }
                } else {
                    raw.trim()
                }
                out.add("$title\n원본: ${qFile.name}" to body)
            }
        }

        val tailKeys = listOf(
            "vehicle_csv" to "4. 차량 분석 (vehicle_analysis.csv)",
            "contact_csv" to "5. 접촉 분석 (contact_analysis.csv)",
            "contact_points_csv" to "6. 접촉 후보 점 (contact_candidate_points.csv)",
        )
        for ((key, title) in tailKeys) {
            val f = bundle.filesByKey[key] ?: continue
            val raw = try {
                f.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                continue
            }
            out.add("$title\n원본: ${f.name}" to raw.trim())
        }
        return out
    }

    data class PoliceInsuranceExportResult(
        val docxFile: File,
        val pythonFile: File,
    )

    /**
     * [outDir] 아래에 `.docx`와 `.py`를 쓴 뒤 반환한다.
     */
    fun writeReports(context: Context, bundle: ServerPipelineResultBundle): PoliceInsuranceExportResult? {
        return try {
            val outDir = File(context.getExternalFilesDir(null), "police_insurance_reports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val base = "PLY_분석_${bundle.taskId}_$stamp"
            val docx = File(outDir, "$base.docx")
            val py = File(outDir, "${base}_generate_docx.py")
            val sections = collectSections(bundle)
            val documentXml = buildDocumentXml(sections)
            writeZipDocx(docx, documentXml)
            py.writeText(buildPythonGeneratorScript(sections, outputDocxName = "$base.docx"), Charsets.UTF_8)
            PoliceInsuranceExportResult(docxFile = docx, pythonFile = py)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Base64로 본문을 넘겨 따옴표 이슈를 피한다. */
    private fun buildPythonGeneratorScript(
        sections: List<Pair<String, String>>,
        outputDocxName: String,
    ): String = buildString {
        val b64list = sections.joinToString(",\n        ") { (title, body) ->
            val t = java.util.Base64.getEncoder().encodeToString(title.toByteArray(Charsets.UTF_8))
            val b = java.util.Base64.getEncoder().encodeToString(body.toByteArray(Charsets.UTF_8))
            "(\"$t\", \"$b\")"
        }
        appendLine("#!/usr/bin/env python3")
        appendLine("# -*- coding: utf-8 -*-")
        appendLine("# pip install python-docx")
        appendLine("# 실행: python 이파일이름.py")
        appendLine("# → 같은 폴더에 Word 파일이 생성됩니다: $outputDocxName")
        appendLine("import base64")
        appendLine("from docx import Document")
        appendLine()
        appendLine("SECTIONS_B64 = [")
        append("        ")
        append(b64list)
        appendLine()
        appendLine("]")
        appendLine()
        appendLine("def main():")
        appendLine("    doc = Document()")
        appendLine("    doc.add_heading(\"3DGS·PLY 분석 보고서 (경찰·보험 참고용)\", level=0)")
        appendLine(
            "    doc.add_paragraph(" +
                "\"자동 생성 스크립트입니다. Base64로 인코딩된 서버 분석 본문을 Word 문서로 풀어 씁니다.\")",
        )
        appendLine("    for title_b64, body_b64 in SECTIONS_B64:")
        appendLine("        title = base64.b64decode(title_b64).decode(\"utf-8\")")
        appendLine("        body = base64.b64decode(body_b64).decode(\"utf-8\")")
        appendLine("        title_lines = title.splitlines()")
        appendLine("        if title_lines:")
        appendLine("            doc.add_heading(title_lines[0], level=1)")
        appendLine("        for extra in title_lines[1:]:")
        appendLine("            doc.add_paragraph(extra)")
        appendLine("        for line in body.splitlines():")
        appendLine("            doc.add_paragraph(line if line else \" \")")
        appendLine("    out = \"$outputDocxName\"")
        appendLine("    doc.save(out)")
        appendLine("    print(\"저장 완료:\", out)")
        appendLine()
        appendLine()
        appendLine("if __name__ == \"__main__\":")
        appendLine("    main()")
        appendLine()
    }

    fun openDocx(context: Context, file: File) {
        if (!file.exists() || !file.isFile) {
            Toast.makeText(context, "문서 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "문서를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Word로 열기"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Word 또는 문서 뷰어를 설치한 뒤 다시 시도하세요.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
