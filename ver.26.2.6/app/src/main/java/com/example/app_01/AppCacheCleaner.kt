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

/**
 * 삭제되지 않고 남아있는 잉여 데이터셋·임시 파일·실패 잔여물을 전역 정리합니다.
 * [clearApplicationCacheJunk]를 포함하며, 추가로 빈 데이터셋 폴더·고아 서버 작업 디렉터리·
 * 임시 ZIP·오래된 배치 결과 폴더까지 정리합니다.
 */
internal fun cleanupOrphanedAppData(context: Context): AppCacheCleanResult {
    val cacheResult = clearApplicationCacheJunk(context)
    val appCtx = context.applicationContext
    val accumulator = CacheDeleteAccumulator()
    accumulator.absorb(cacheResult)

    val storageRoot = ModelLibraryPaths.storageRoot(appCtx)
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60L * 60L * 1000L
    val staleThresholdMs = 7L * dayMs // 7일 이상 수정되지 않은 잉여물

    // ── 빈 데이터셋 폴더 정리 ─────────────────────────────────────────────
    val datasetsDir = File(storageRoot, "datasets")
    if (datasetsDir.isDirectory) {
        val imageExts = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
        datasetsDir.listFiles()?.filter { it.isDirectory }?.forEach { dsDir ->
            val hasImages = dsDir.listFiles { f ->
                f.isFile && imageExts.contains(f.extension.lowercase())
            }?.isNotEmpty() == true
            if (!hasImages) {
                accumulator.deleteRecursively(dsDir)
            }
        }
    }

    // ── 서버 다운로드 중단 잔여물 ─────────────────────────────────────────
    val plyDir = ModelLibraryPaths.plyDir(appCtx)
    if (plyDir.isDirectory) {
        plyDir.listFiles()?.forEach { entry ->
            when {
                entry.isDirectory && entry.name.endsWith(".downloading") ->
                    accumulator.deleteRecursively(entry)
                entry.isFile && entry.name.endsWith(".part") ->
                    accumulator.deleteRecursively(entry)
                entry.isDirectory && entry.name.startsWith("server_task_") -> {
                    val hasPly = entry.listFiles { f ->
                        f.isFile && f.name.endsWith(".ply", ignoreCase = true) && f.length() > 0L
                    }?.isNotEmpty() == true
                    if (!hasPly) {
                        accumulator.deleteRecursively(entry)
                    }
                }
            }
        }
    }

    // ── 임시 ZIP 파일 정리 ───────────────────────────────────────────────
    val cacheDir = appCtx.cacheDir ?: return@cleanupOrphanedAppData accumulator.toResult()
    cacheDir.listFiles()?.forEach { f ->
        if (f.isFile && (f.name.endsWith(".zip") || f.name.startsWith("arcore_upload_"))) {
            if (now - f.lastModified() > staleThresholdMs) {
                accumulator.deleteRecursively(f)
            }
        }
    }

    // ── 저장소 루트의 임시 ZIP 정리 ──────────────────────────────────────
    storageRoot.listFiles()?.forEach { f ->
        if (f.isFile && f.name.endsWith(".zip") && now - f.lastModified() > staleThresholdMs) {
            accumulator.deleteRecursively(f)
        }
    }

    // ── 빈 배치 결과 폴더 정리 ───────────────────────────────────────────
    if (datasetsDir.isDirectory) {
        datasetsDir.listFiles()?.filter { it.isDirectory }?.forEach { batchDir ->
            if (now - batchDir.lastModified() > staleThresholdMs) {
                val hasContent = batchDir.listFiles()?.any { true } == true
                if (!hasContent) {
                    accumulator.deleteRecursively(batchDir)
                }
            }
        }
    }

    // ── PLY→OBJ 변환 캐시 정리 ──────────────────────────────────────────
    accumulator.deleteRecursively(File(storageRoot, "models_obj"))

    // ── WebView 캐시 ─────────────────────────────────────────────────────
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

    fun absorb(result: AppCacheCleanResult) {
        files += result.deletedFiles
        dirs += result.deletedDirs
        bytes += result.deletedBytes
    }

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
