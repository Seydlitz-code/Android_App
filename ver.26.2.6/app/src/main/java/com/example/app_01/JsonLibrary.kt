package com.example.app_01

import android.content.Context
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

    /** 서버 작업 출력 폴더의 `.json`을 라이브러리로 복사한다 (`{taskId}_{원본파일명}`). */
    fun ingestFromPipelineOutputDir(context: Context, taskDir: File, taskId: String) {
        if (!taskDir.isDirectory) return
        val destRoot = dir(context)
        taskDir.listFiles()?.forEach { f ->
            if (!f.isFile || !f.extension.equals("json", ignoreCase = true)) return@forEach
            val dest = File(destRoot, "${taskId}_${f.name}")
            try {
                f.copyTo(dest, overwrite = true)
            } catch (_: Exception) {
            }
        }
    }
}
