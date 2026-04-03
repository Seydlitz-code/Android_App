package com.example.app_01

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** AI CAD 라이브러리: OpenSCAD 계열 코드에서 추출한 메쉬를 STL로 저장·목록 표시 */
object AiCadLibrary {
    private const val DIR_NAME = "aicad_library"

    fun getLibraryDir(context: Context): File =
        File(context.getExternalFilesDir(null), DIR_NAME).apply { mkdirs() }

    fun listStlFiles(context: Context): List<File> {
        val dir = getLibraryDir(context)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles { f -> f.isFile && f.extension.equals("stl", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * [stlBytes]를 AI CAD 라이브러리에 저장합니다. 확장자 .stl (바이너리 STL).
     */
    fun saveStlFile(context: Context, stlBytes: ByteArray, nameWithoutExt: String? = null): File {
        val dir = getLibraryDir(context)
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safe =
            nameWithoutExt?.replace("[^a-zA-Z0-9가-힣_\\-]".toRegex(), "_")?.takeIf { it.isNotBlank() }
        val base = "${safe ?: "model"}_$ts"
        return File(dir, "$base.stl").also { it.writeBytes(stlBytes) }
    }

    /**
     * OpenSCAD 소스를 검증 후 STL·GLB·OBJ로 변환해 라이브러리에 저장합니다.
     * (파이프라인: 코드 검증 → OpenSCAD 런타임 → STL → 후처리·GLB/OBJ)
     * @param preferredName 비어 있지 않으면 파일명 접두로 사용(정규화됨).
     * @param useRandomWhenNameBlank true이고 [preferredName]이 비어 있으면 무작위 접두를 사용합니다.
     */
    suspend fun exportAndSaveStlFromSource(
        context: Context,
        scadSource: String,
        preferredName: String? = null,
        useRandomWhenNameBlank: Boolean = false
    ): Result<AiCadPipeline.SavedArtifacts> =
        AiCadPipeline.exportVerifiedScadToLibrary(
            context,
            scadSource,
            preferredName,
            useRandomWhenNameBlank
        )
}

private val OPENSCAD_HINT = Regex(
    """(?m)(union|difference|intersection|hull|minkowski|cube|cylinder|sphere|polyhedron|rotate_extrude|linear_extrude|module|function|\\${'$'}fn|include|use\s*<)""",
    RegexOption.IGNORE_CASE
)

/** LLM이 펜스 없이 순수 스크립트만 보낸 경우 추정 */
private fun looksLikeOpenScadSource(s: String): Boolean {
    val t = s.trim()
    if (t.length < 8) return false
    return OPENSCAD_HINT.containsMatchIn(t)
}

/** 마크다운 응답에서 ```opencad``` / ```openscad``` / ```scad``` 블록 추출, 또는 펜스 없는 순수 코드 */
fun extractOpenCadCode(markdown: String): String? {
    val patterns = listOf(
        Regex("```opencad\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE),
        Regex("```openscad\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE),
        Regex("```scad\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    )
    var best: String? = null
    for (p in patterns) {
        for (m in p.findAll(markdown)) {
            val code = m.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: continue
            if (best == null || code.length > (best?.length ?: 0)) best = code
        }
    }
    if (best != null) return best
    val trimmed = markdown.trim()
    if (trimmed.isEmpty()) return null
    // ``` ... ``` 전체가 한 블록인데 언어 태그가 없는 경우
    if (trimmed.startsWith("```") && trimmed.endsWith("```") && trimmed.count { it == '`' } >= 6) {
        val lines = trimmed.lines()
        if (lines.size >= 3 && lines.first().startsWith("```") && lines.last().trim() == "```") {
            val inner = lines.drop(1).dropLast(1).joinToString("\n").trim()
            if (inner.isNotBlank() && looksLikeOpenScadSource(inner)) return inner
        }
    }
    if (!trimmed.contains("```") && looksLikeOpenScadSource(trimmed)) return trimmed
    return null
}
