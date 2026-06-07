package com.example.app_01

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * [AppWarningLogInstaller] 로 처리되지 않은 JVM 예외를 파일에 기록하기 위해 매니페스트에 등록함.
 * Coil 메모리·디스크 캐시 상한과 앱 전역 [appScope]를 제공합니다.
 */
class AppApplication : Application(), ImageLoaderFactory {

    val appScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        migrateLegacyServerSettingsIfNeeded(this)
        AppWarningLogInstaller.install(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(256L * 1024L * 1024L)
                    .build()
            }
            .crossfade(false)
            .build()
    }
}

internal fun Application.appCoroutineScope(): CoroutineScope =
    (this as? AppApplication)?.appScope
        ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
