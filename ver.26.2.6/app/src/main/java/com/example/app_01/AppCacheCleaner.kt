package com.example.app_01

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import coil.imageLoader
import java.io.File
import java.util.Locale

data class AppCacheCleanResult(
    val deletedFiles: Int,
    val deletedDirs: Int,
    val deletedBytes: Long,
) {
    val isEmpty: Boolean
        get() = deletedFiles == 0 && deletedDirs == 0 && deletedBytes <= 0L
}

/**
 * 사용자 라이브러리 원본(갤러리·데이터셋·PLY 등)은 보존하고, 재생성 가능한 캐시와
 * 임시·실패 잔여물을 전역으로 정리한다.
 */
internal fun clearApplicationCacheJunk(context: Context): AppCacheCleanResult {
    val appCtx = context.applicationContext
    val accumulator = CacheDeleteAccumulator()

    clearCoilImageCaches(appCtx)

    // cacheDir 자식 전부 삭제 시 ART JIT/프로파일용 code_cache까지 제거되면 재실행·계속 실행 시 불안정할 수 있음
    accumulator.deleteCacheJunkPreservingRuntimeArtifacts(appCtx.cacheDir)
    accumulator.deleteCacheJunkPreservingRuntimeArtifacts(appCtx.externalCacheDir)

    accumulator.deleteRecursively(File(appCtx.filesDir, "app_model_thumbs"))
    accumulator.deleteRecursively(File(appCtx.filesDir, "pictogram_cache"))
    accumulator.deleteRecursively(File(appCtx.filesDir, "mobile_space_session"))

    accumulator.deleteRecursively(File(System.getProperty("java.io.tmpdir") ?: "", "pp_cb_parts"))

    val storageRoot = ModelLibraryPaths.storageRoot(appCtx)
    accumulator.deleteRecursively(File(storageRoot, "models_obj"))
    accumulator.deleteRecursively(File(ModelLibraryPaths.legacyModelsDir(appCtx), ".thumbnails"))

    accumulator.deleteServerDownloadLeftovers(ModelLibraryPaths.plyDir(appCtx))

    clearWebViewCaches()

    return accumulator.toResult()
}

private fun clearCoilImageCaches(context: Context) {
    try {
        val loader = context.applicationContext.imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    } catch (_: Throwable) {
    }
}

private fun clearWebViewCaches() {
    try {
        WebStorage.getInstance().deleteAllData()
    } catch (_: Throwable) {
    }
    try {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    } catch (_: Throwable) {
    }
}

internal fun formatAppCacheCleanResult(result: AppCacheCleanResult): String {
    if (result.isEmpty) return "삭제할 캐시가 없습니다."
    return String.format(
        Locale.US,
        "캐시 정리 완료: 파일 %d개, 폴더 %d개, %s 삭제",
        result.deletedFiles,
        result.deletedDirs,
        formatCacheBytes(result.deletedBytes),
    )
}

private class CacheDeleteAccumulator {
    private var files = 0
    private var dirs = 0
    private var bytes = 0L

    fun toResult(): AppCacheCleanResult = AppCacheCleanResult(files, dirs, bytes)

    /** getCacheDir·getExternalCacheDir 밑 자식만 삭제하되 code_cache 디렉터리는 건너뜀 */
    fun deleteCacheJunkPreservingRuntimeArtifacts(dir: File?) {
        if (dir == null || !dir.isDirectory) return
        dir.listFiles()?.forEach { child ->
            if (child.name.equals("code_cache", ignoreCase = true)) return@forEach
            deleteRecursively(child)
        }
    }

    fun deleteRecursively(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
            if (file.delete()) dirs++
        } else {
            val len = runCatching { file.length() }.getOrDefault(0L)
            if (file.delete()) {
                files++
                bytes += len
            }
        }
    }

    fun deleteServerDownloadLeftovers(plyDir: File) {
        if (!plyDir.isDirectory) return
        plyDir.listFiles()?.forEach { entry ->
            when {
                entry.isDirectory && entry.name.endsWith(".downloading") -> deleteRecursively(entry)
                entry.isFile && entry.name.endsWith(".part") -> deleteRecursively(entry)
                entry.isDirectory && entry.name.startsWith("server_task_") -> {
                    entry.listFiles()?.forEach { child ->
                        if (child.isFile && child.name.endsWith(".part")) deleteRecursively(child)
                    }
                }
            }
        }
    }
}

private fun formatCacheBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
