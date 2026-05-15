package com.example.app_01

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.LinkedHashMap

private const val SERVER_ARTIFACT_MANIFEST_MAX_BYTES = 256L * 1024L

private val SERVER_MOBILE_ARTIFACT_KEYS = setOf(
    "ply",
    "glb",
    "topview",
    "sideview",
    "top_view",
    "side_view",
    "quality_json",
    "quality_txt",
    "quality_png",
    "analysis_json",
    "analysis_png",
    "vehicle_csv",
    "contact_csv",
    "contact_points_csv",
)

/**
 * 모바일 앱이 소비하는 서버 결과만 허용합니다.
 * 서버가 데이터셋 원본, 중간 산출물, 미설정 파일을 추가로 보내도 앱은 조용히 건너뜁니다.
 */
internal fun normalizeMobileServerArtifactKey(key: String, filename: String = ""): String? {
    val k = key.trim().lowercase()
    when (k) {
        "top_view" -> return "topview"
        "side_view" -> return "sideview"
        in SERVER_MOBILE_ARTIFACT_KEYS -> return k
    }

    val name = filename.trim().substringAfterLast('/').substringAfterLast('\\').lowercase()
    return when {
        name.endsWith(".ply") -> "ply"
        name.endsWith(".glb") -> "glb"
        name == "quality_report.json" -> "quality_json"
        name == "analysis_result.json" -> "analysis_json"
        name == "topview.png" -> "topview"
        name == "sideview.png" -> "sideview"
        name == "vehicle_analysis.csv" -> "vehicle_csv"
        name == "contact_analysis.csv" -> "contact_csv"
        name == "contact_candidate_points.csv" -> "contact_points_csv"
        else -> null
    }
}

internal fun isMobileServerArtifact(key: String, filename: String = ""): Boolean =
    normalizeMobileServerArtifactKey(key, filename) != null

/**
 * 서버 파이프라인 결과 폴더 `models/ply/server_task_{taskId}/` 인덱싱.
 * 다운로드 직후 [.server_artifacts.json] 메타를 쓰고, 라이브러리 탭에서 미리보기·분석 이미지를 목록화한다.
 */
data class ServerTaskManifestInfo(
    val taskId: String,
    val directory: File,
    val filesByKey: Map<String, File>,
)

internal fun preferredServerPushArtifactName(taskId: String, key: String, original: String): String {
    return when (key) {
        "ply" -> "result_${taskId}.ply"
        "glb" -> "result_${taskId}.glb"
        /** 서버 콜백 키 [quality_json] → 단일 [quality_report.json] (txt/png 제거) */
        "quality_json" -> "quality_report.json"
        "quality_txt" -> "quality_report.txt"
        "quality_png" -> "quality_report.png"
        "analysis_json" -> "analysis_result.json"
        "vehicle_csv" -> "vehicle_analysis.csv"
        "contact_csv" -> "contact_analysis.csv"
        "contact_points_csv" -> "contact_candidate_points.csv"
        "topview" -> "topview.png"
        "sideview" -> "sideview.png"
        else -> original.ifBlank { "$key.bin" }
    }
}

/**
 * 서버가 [callback_url]로 multipart POST 한 결과 파일을 `server_task_{taskId}` 아래로 복사해 번들을 만듭니다.
 */
internal fun buildServerPipelineBundleFromPushedFiles(
    context: Context,
    taskId: String,
    partFiles: Map<String, File>,
): ServerPipelineResultBundle? {
    if (partFiles.isEmpty()) return null
    val plyDir = ModelLibraryPaths.plyDir(context)
    val outDir = File(plyDir, "server_task_$taskId").apply { mkdirs() }
    val map = LinkedHashMap<String, File>()
    for ((key, src) in partFiles) {
        if (!src.exists() || !src.isFile) continue
        val artifactKey = normalizeMobileServerArtifactKey(key, src.name) ?: continue
        val destName = preferredServerPushArtifactName(taskId, artifactKey, src.name)
        val dest = File(outDir, destName)
        try {
            src.copyTo(dest, overwrite = true)
        } catch (_: Exception) {
            continue
        }
        if (dest.exists() && dest.length() > 0L) map[artifactKey] = dest
    }
    val plyFile = map["ply"] ?: return null
    writeServerTaskArtifactManifest(outDir, taskId, map)
    return ServerPipelineResultBundle(taskId, plyFile, outDir, map)
}

internal fun writeServerTaskArtifactManifest(
    outDir: File,
    taskId: String,
    filesByKey: Map<String, File>,
) {
    try {
        val o = JSONObject()
        o.put("taskId", taskId)
        o.put("savedAt", System.currentTimeMillis())
        val keys = JSONObject()
        filesByKey.forEach { (k, f) -> keys.put(k, f.absolutePath) }
        o.put("filesByKey", keys)
        File(outDir, ".server_artifacts.json").writeText(o.toString())
    } catch (_: Throwable) {
    }
}

internal fun scanServerTaskManifestInfos(context: Context): List<ServerTaskManifestInfo> {
    val dirs = try {
        val plyDir = ModelLibraryPaths.plyDir(context)
        plyDir.listFiles { f ->
            f.isDirectory &&
                f.name.startsWith("server_task_") &&
                !f.name.endsWith(".downloading")
        } ?: return emptyList()
    } catch (_: Throwable) {
        return emptyList()
    }

    fun inferFromDir(dir: File, taskId: String): ServerTaskManifestInfo? {
        val files = try {
            dir.listFiles()?.filter { it.isFile }
        } catch (_: Throwable) {
            null
        } ?: return null
        val map = mutableMapOf<String, File>()
        for (f in files) {
            val lower = f.name.lowercase()
            when {
                lower.contains("top") && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ->
                    map["topview"] = f
                lower.contains("side") && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ->
                    map["sideview"] = f
                lower.endsWith(".json") && (lower.contains("quality_report") || lower == "quality_report.json") ->
                    map["quality_json"] = f
                lower.contains("quality") || lower.contains("analysis") ->
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                        map["quality_png"] = f
                    }
            }
        }
        return if (map.isNotEmpty()) ServerTaskManifestInfo(taskId, dir, map) else null
    }

    return dirs.mapNotNull { dir ->
        val taskId = dir.name.removePrefix("server_task_")
        val mf = File(dir, ".server_artifacts.json")
        if (mf.exists()) {
            try {
                if (mf.length() > SERVER_ARTIFACT_MANIFEST_MAX_BYTES) {
                    return@mapNotNull inferFromDir(dir, taskId)
                }
                val json = JSONObject(mf.readText())
                val keysObj = json.optJSONObject("filesByKey") ?: return@mapNotNull inferFromDir(dir, taskId)
                val map = mutableMapOf<String, File>()
                val it = keysObj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val pathOrName = keysObj.optString(k)
                    if (pathOrName.isBlank()) continue
                    val f = if (pathOrName.contains('/')) File(pathOrName) else File(dir, pathOrName)
                    if (f.exists()) map[k] = f
                }
                if (map.isEmpty()) inferFromDir(dir, taskId)
                else ServerTaskManifestInfo(taskId, dir, map)
            } catch (_: Throwable) {
                inferFromDir(dir, taskId)
            }
        } else {
            inferFromDir(dir, taskId)
        }
    }.sortedByDescending {
        try {
            it.directory.lastModified()
        } catch (_: Throwable) {
            0L
        }
    }
}

internal fun previewUrisForServerTasks(infos: List<ServerTaskManifestInfo>): List<Uri> {
    val keys = listOf("topview", "sideview", "top_view", "side_view")
    val out = ArrayList<Uri>()
    for (info in infos) {
        for (k in keys) {
            info.filesByKey[k]?.takeIf { it.exists() }?.let { out.add(Uri.fromFile(it)) }
        }
    }
    return out.distinct()
}

internal fun analysisImageUrisForServerTasks(infos: List<ServerTaskManifestInfo>): List<Uri> {
    /** 레거시 PNG 분석 이미지 — [quality_json] 은 이미지가 아니므로 제외 */
    val keys = listOf("quality_png", "analysis_png", "quality", "analysis_json")
    val out = ArrayList<Uri>()
    for (info in infos) {
        for (k in keys) {
            val f = info.filesByKey[k] ?: continue
            if (!f.exists()) continue
            val ext = f.extension.lowercase()
            if (ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp") {
                out.add(Uri.fromFile(f))
            }
        }
    }
    return out.distinct()
}

internal fun countPreviewTasks(infos: List<ServerTaskManifestInfo>): Int =
    infos.count { info ->
        listOf("topview", "sideview", "top_view", "side_view").any { k -> info.filesByKey[k]?.exists() == true }
    }

internal fun countAnalysisTasks(infos: List<ServerTaskManifestInfo>): Int =
    infos.count { info ->
        info.filesByKey.keys.any { k ->
            k == "quality_json" ||
                k.contains("quality") ||
                k.contains("analysis") ||
                k == "quality_png"
        }
    }

/** 3DGS 분석 탭 그리드: 품질 JSON 타일 + 레거시 분석 이미지 타일 */
internal sealed class GsAnalysisGridItem {
    data class ImageTile(val uri: Uri, val canonicalPath: String?) : GsAnalysisGridItem()
    data class QualityJsonTile(val taskId: String, val file: File) : GsAnalysisGridItem() {
        val canonicalPath: String?
            get() = try {
                file.canonicalPath
            } catch (_: Throwable) {
                file.absolutePath
            }
    }
}

internal fun collectGsAnalysisGridItems(infos: List<ServerTaskManifestInfo>): List<GsAnalysisGridItem> {
    val out = ArrayList<GsAnalysisGridItem>()
    for (info in infos) {
        info.filesByKey["quality_json"]
            ?.takeIf { it.exists() && it.isFile && it.extension.equals("json", ignoreCase = true) }
            ?.let { out.add(GsAnalysisGridItem.QualityJsonTile(info.taskId, it)) }
        val legacyImageKeys = listOf("quality_png", "analysis_png")
        for (k in legacyImageKeys) {
            val f = info.filesByKey[k] ?: continue
            if (!f.exists()) continue
            val ext = f.extension.lowercase()
            if (ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp") {
                val path = try {
                    f.canonicalPath
                } catch (_: Throwable) {
                    f.absolutePath
                }
                out.add(GsAnalysisGridItem.ImageTile(Uri.fromFile(f), path))
            }
        }
    }
    return out
}

internal fun gsAnalysisCoverUriFirstImage(items: List<GsAnalysisGridItem>): Uri? =
    items.firstNotNullOfOrNull { item ->
        when (item) {
            is GsAnalysisGridItem.ImageTile -> item.uri
            is GsAnalysisGridItem.QualityJsonTile -> null
        }
    }
