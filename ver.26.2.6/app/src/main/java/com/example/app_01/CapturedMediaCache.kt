package com.example.app_01

import android.net.Uri
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** 갤러리 미디어 디렉터리 스캔 결과·정렬 시각 캐시 — 반복 EXIF/Retriever 호출을 줄입니다. */
internal object CapturedMediaCache {

    data class MediaDirFingerprint(val fileCount: Int, val maxLastModified: Long)

    private data class ListCache(val fingerprint: MediaDirFingerprint, val uris: List<Uri>)

    private data class SortTimeEntry(val fileLastModified: Long, val sortTime: Long)

    @Volatile
    private var listCache: ListCache? = null

    private val sortTimeCache = ConcurrentHashMap<String, SortTimeEntry>()

    private const val MAX_SORT_CACHE_ENTRIES = 8_000

    fun getCachedUrisIfValid(fingerprint: MediaDirFingerprint): List<Uri>? {
        val cached = listCache ?: return null
        return if (cached.fingerprint == fingerprint) cached.uris else null
    }

    fun storeUris(fingerprint: MediaDirFingerprint, uris: List<Uri>) {
        listCache = ListCache(fingerprint, uris)
    }

    fun invalidateList() {
        listCache = null
    }

    fun mediaSortTimeMillisCached(file: File): Long {
        val path = file.absolutePath
        val lm = file.lastModified()
        sortTimeCache[path]?.let { entry ->
            if (entry.fileLastModified == lm) return entry.sortTime
        }
        val sort = mediaSortTimeMillis(file)
        if (sortTimeCache.size >= MAX_SORT_CACHE_ENTRIES) {
            sortTimeCache.clear()
        }
        sortTimeCache[path] = SortTimeEntry(lm, sort)
        return sort
    }
}

/** [loadCapturedMediaSync] 무효화 판별용 — EXIF 없이 파일 수·최신 수정 시각만 집계합니다. */
internal fun computeCapturedMediaFingerprint(mediaDir: File): CapturedMediaCache.MediaDirFingerprint {
    val datasetsRoot = File(mediaDir, "datasets").absolutePath
    var count = 0
    var maxLm = 0L
    mediaDir.walkTopDown()
        .onEnter { dir ->
            dir == mediaDir || !skipGallerySubtreeDirName(dir.name)
        }
        .forEach { file ->
            if (!file.isFile) return@forEach
            if (file.absolutePath.startsWith(datasetsRoot)) return@forEach
            val isMedia = file.name.endsWith(".jpg", ignoreCase = true) ||
                file.name.endsWith(".jpeg", ignoreCase = true) ||
                file.name.endsWith(".png", ignoreCase = true) ||
                file.name.endsWith(".webp", ignoreCase = true) ||
                file.name.endsWith(".heic", ignoreCase = true) ||
                file.name.endsWith(".heif", ignoreCase = true) ||
                file.name.endsWith(".mp4", ignoreCase = true)
            if (!isMedia) return@forEach
            count++
            val lm = file.lastModified()
            if (lm > maxLm) maxLm = lm
        }
    return CapturedMediaCache.MediaDirFingerprint(count, maxLm)
}

internal fun isCapturedGalleryMediaFile(file: File, datasetsRoot: String): Boolean {
    if (!file.isFile) return false
    if (file.absolutePath.startsWith(datasetsRoot)) return false
    return file.name.endsWith(".jpg", ignoreCase = true) ||
        file.name.endsWith(".jpeg", ignoreCase = true) ||
        file.name.endsWith(".png", ignoreCase = true) ||
        file.name.endsWith(".webp", ignoreCase = true) ||
        file.name.endsWith(".heic", ignoreCase = true) ||
        file.name.endsWith(".heif", ignoreCase = true) ||
        file.name.endsWith(".mp4", ignoreCase = true)
}
