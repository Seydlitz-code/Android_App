package com.example.app_01

import android.content.Context
import java.io.File

// App external files: models/ply, models/obj; thumbnails remain under models/.thumbnails
object ModelLibraryPaths {

    private const val MIGRATION_FLAG = ".split_library_v1"

    fun storageRoot(context: Context): File =
        context.getExternalFilesDir(null) ?: context.filesDir

    fun legacyModelsDir(context: Context): File =
        File(storageRoot(context), "models")

    fun plyDir(context: Context): File {
        val d = File(legacyModelsDir(context), "ply")
        d.mkdirs()
        return d
    }

    fun objDir(context: Context): File {
        val d = File(legacyModelsDir(context), "obj")
        d.mkdirs()
        return d
    }

    // One-time move from flat models/ into ply/ and obj/
    fun migrateFlatModelsIfNeeded(context: Context) {
        val root = legacyModelsDir(context)
        root.mkdirs()
        if (!root.isDirectory) return
        val flag = File(root, MIGRATION_FLAG)
        if (flag.exists()) return

        val plyD = plyDir(context)
        val objD = objDir(context)
        root.listFiles()?.forEach { f ->
            if (!f.isFile) return@forEach
            when {
                f.name.endsWith(".ply", ignoreCase = true) -> {
                    val dest = File(plyD, f.name)
                    if (!dest.exists()) f.renameTo(dest)
                }
                f.name.endsWith(".obj", ignoreCase = true) -> {
                    val dest = File(objD, f.name)
                    if (!dest.exists()) f.renameTo(dest)
                }
                f.name.endsWith(".mtl", ignoreCase = true) -> {
                    val dest = File(objD, f.name)
                    if (!dest.exists()) f.renameTo(dest)
                }
            }
        }
        try {
            flag.createNewFile()
        } catch (_: Exception) {
        }
    }
}
