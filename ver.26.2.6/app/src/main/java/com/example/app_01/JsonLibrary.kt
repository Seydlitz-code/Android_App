package com.example.app_01

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 앱 내 JSON 라이브러리 (`models/json/`). 서버 파이프라인·LLM 3DGS 분석용 JSON 보관 */
object JsonLibrary {
    fun dir(context: Context): File {
        val d = File(ModelLibraryPaths.legacyModelsDir(context), "json")
        d.mkdirs()
        return d
    }

    fun listFilesSorted(context: Context): List<File> {
        val d = dir(context)
        return d.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /** 사진 촬영 후 ARCore 포즈·Intrinsics `frames` JSON을 JSON 라이브러리에 저장.
     *  내용이 비어있을 경우 그럴듯한 제목 텍스트만 포함된 빈 JSON을 생성한다. */
    fun saveArCoreFramesJson(context: Context, root: JSONObject): File {
        val dest = File(dir(context), "arcore_${System.currentTimeMillis()}.json")
        val effective = if (root.optJSONArray("frames")?.length() == 0) {
            JSONObject().apply {
                put("title", "Camera Pose Metadata")
                put("description", "ARCore frame capture log")
                put("version", "1.0")
                put("generatedAt", System.currentTimeMillis())
                put("note", "No tracking data available")
                put("frames", JSONArray())
            }
        } else root
        dest.writeText(effective.toString(), Charsets.UTF_8)
        return dest
    }

    /** 로컬 메타는 JSON 라이브러리에 넣지 않음 */
    private const val SERVER_TASK_META_JSON = ".server_artifacts.json"

    /** `models/json` 으로 복사할 JSON 상한(OOM 방지). 그 이상은 task 폴더 원본만 사용 */
    private const val MAX_JSON_INGEST_COPY_BYTES = 24L * 1024L * 1024L

    fun ingestFromPipelineOutputDir(
        context: Context,
        taskDir: File,
        taskId: String,
        filesByKey: Map<String, File> = emptyMap(),
    ) {
        if (!taskDir.isDirectory) return
        val destRoot = dir(context)
        val ingestedCanonical = HashSet<String>()
        val candidates = LinkedHashSet<File>()
        taskDir.listFiles()?.forEach { f -> if (f.isFile) candidates.add(f) }
        for (f in filesByKey.values) {
            if (f.isFile && f.exists()) candidates.add(f)
        }
        for (source in candidates) {
            if (source.name == SERVER_TASK_META_JSON) continue
            val can = try {
                source.canonicalPath
            } catch (_: Exception) {
                source.absolutePath
            }
            if (!ingestedCanonical.add(can)) continue
            if (!isJsonLibraryCandidateFile(source)) continue
            val storedName =
                if (source.extension.equals("json", ignoreCase = true)) source.name
                else "${source.name}.json"
            val dest = File(destRoot, "${taskId}_$storedName")
            try {
                source.copyTo(dest, overwrite = true)
            } catch (_: Exception) {
            }
        }
    }

    private fun isJsonLibraryCandidateFile(f: File): Boolean {
        if (!f.isFile) return false
        if (f.name == SERVER_TASK_META_JSON) return false
        val len = f.length()
        if (len <= 0L) return false
        if (len > MAX_JSON_INGEST_COPY_BYTES) return false
        if (f.extension.equals("json", ignoreCase = true)) return fileStartsLikeJson(f)
        if (len > 2_000_000L) return false
        return fileStartsLikeJson(f)
    }

    private fun fileStartsLikeJson(f: File): Boolean {
        val toRead = f.length().coerceAtMost(4096).toInt()
        if (toRead <= 0) return false
        return try {
            f.inputStream().use { inp ->
                val buf = ByteArray(toRead)
                var total = 0
                while (total < toRead) {
                    val r = inp.read(buf, total, toRead - total)
                    if (r <= 0) break
                    total += r
                }
                var i = 0
                if (total >= 3 &&
                    buf[0] == 0xEF.toByte() && buf[1] == 0xBB.toByte() && buf[2] == 0xBF.toByte()
                ) {
                    i = 3
                }
                while (i < total) {
                    val b = buf[i].toInt() and 0xff
                    when (b) {
                        ' '.code, '\t'.code, '\r'.code, '\n'.code -> i++
                        '{'.code, '['.code -> return true
                        else -> return false
                    }
                }
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
