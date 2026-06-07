package com.example.app_01

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 갤러리 목록을 IO에서 다시 읽고 메인 스레드에서 [onUpdated]를 호출합니다. */
internal suspend fun refreshCapturedImages(
    context: Context,
    invalidateCache: Boolean = false,
    onUpdated: (List<Uri>) -> Unit,
) {
    if (invalidateCache) CapturedMediaCache.invalidateList()
    val images = withContext(Dispatchers.IO) { loadCapturedMediaSync(context) }
    withContext(Dispatchers.Main) { onUpdated(images) }
}
