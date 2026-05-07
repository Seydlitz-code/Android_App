package com.example.app_01

import android.content.Context
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

    /** 사진 촬영 후 ARCore 포즈·Intrinsics `frames` JSON을 JSON 라이브러리에 저장 */
    fun saveArCoreFramesJson(context: Context, root: JSONObject): File {
        val dest = File(dir(context), "arcore_${System.currentTimeMillis()}.json")
        dest.writeText(root.toString(), Charsets.UTF_8)
        return dest
    }

    /** 로컬 메타는 JSON 라이브러리에 넣지 않음 */
    private const val SERVER_TASK_META_JSON = ".server_artifacts.json"

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
        if (f.extension.equals("json", ignoreCase = true)) return true
        val len = f.length()
        if (len <= 0L || len > 2_000_000L) return false
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
