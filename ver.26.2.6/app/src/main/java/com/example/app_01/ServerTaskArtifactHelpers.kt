package com.example.app_01

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.LinkedHashMap

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
        val destName = preferredServerPushArtifactName(taskId, key, src.name)
        val dest = File(outDir, destName)
        try {
            src.copyTo(dest, overwrite = true)
        } catch (_: Exception) {
            continue
        }
        if (dest.exists() && dest.length() > 0L) map[key] = dest
    }
    val plyFile = map["ply"] ?: return null
    writeServerTaskArtifactManifest(outDir, taskId, map)
    JsonLibrary.ingestFromPipelineOutputDir(context, outDir, taskId, map)
    return ServerPipelineResultBundle(taskId, plyFile, outDir, map)
}

internal fun writeServerTaskArtifactManifest(outDir: File, taskId: String, filesByKey: Map<String, File>) {
    try {
        val o = JSONObject()
        o.put("taskId", taskId)
        o.put("savedAt", System.currentTimeMillis())
        val keys = JSONObject()
        filesByKey.forEach { (k, f) -> keys.put(k, f.absolutePath) }
        o.put("filesByKey", keys)
        File(outDir, ".server_artifacts.json").writeText(o.toString())
    } catch (_: Exception) {
    }
}

internal fun scanServerTaskManifestInfos(context: Context): List<ServerTaskManifestInfo> {
    val plyDir = ModelLibraryPaths.plyDir(context)
    val dirs = plyDir.listFiles { f ->
        f.isDirectory && f.name.startsWith("server_task_")
    } ?: return emptyList()

    fun inferFromDir(dir: File, taskId: String): ServerTaskManifestInfo? {
        val files = dir.listFiles()?.filter { it.isFile } ?: return null
        val map = mutableMapOf<String, File>()
        for (f in files) {
            val lower = f.name.lowercase()
            when {
                lower.contains("top") && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ->
                    map["topview"] = f
                lower.contains("side") && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ->
                    map["sideview"] = f
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
            } catch (_: Exception) {
                inferFromDir(dir, taskId)
            }
        } else {
            inferFromDir(dir, taskId)
        }
    }.sortedByDescending { it.directory.lastModified() }
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
            k.contains("quality") || k.contains("analysis") || k == "quality_png"
        }
    }
