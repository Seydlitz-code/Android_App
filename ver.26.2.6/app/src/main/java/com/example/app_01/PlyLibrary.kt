package com.example.app_01

import android.content.Context
import java.io.File

/** PLY 전용 라이브러리 — 서버 파이프라인에서 받은 PLY 파일만 관리 */
object PlyLibrary {
    fun dir(context: Context): File = ModelLibraryPaths.plyDir(context)

    fun listFilesSorted(context: Context): List<File> {
        val d = dir(context)
        return collectPlyFilesRecursive(d)
            .sortedByDescending { it.lastModified() }
    }

    private fun collectPlyFilesRecursive(root: File): List<File> {
        val out = ArrayList<File>()
        val entries = root.listFiles() ?: return out
        for (entry in entries) {
            when {
                entry.isFile && entry.name.endsWith(".ply", ignoreCase = true) -> out.add(entry)
                entry.isDirectory -> {
                    val sub = entry.listFiles() ?: continue
                    for (f in sub) {
                        if (f.isFile && f.name.endsWith(".ply", ignoreCase = true)) out.add(f)
                    }
                }
            }
        }
        return out.distinctBy { it.absolutePath }
    }
}
