package com.example.app_01

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Mobile-GS 온디바이스 파이프라인 오케스트레이션.
 *
 * ## 처리 단계 (이미지만 입력)
 *  1. EXIF 파싱 — 이미지별 초점거리(35mm 환산) 및 방향 추출
 *  2. 피보나치 구체 위에 가상 카메라 N개 균등 배치 (SfM 대체)
 *  3. 다중 스케일 깊이 추정 (선명도·분산·채도·중심·휘도)
 *  4. 역투영: 픽셀 → 3D 가우시안 (cameras.py · gaussian_model.py 참조)
 *  5. 씬 정규화 (getNerfppNorm 개념)
 */
object MobileGaussianSplattingScript {

    suspend fun runPipeline(
        context: Context,
        imageUris: List<Uri>,
        onLog: suspend (String) -> Unit,
        onProgress: suspend (Int) -> Unit,
    ): MobileSplatScene? {

        onLog("=== Mobile-GS 온디바이스 파이프라인 (v3) ===")
        onProgress(2); delay(30)

        require(imageUris.isNotEmpty()) { "입력 이미지가 필요합니다." }

        onLog("• 입력 이미지: ${imageUris.size}장")
        onProgress(5)

        // ── EXIF 파싱 ─────────────────────────────────────────────────────
        // Mobile-GS cameras.py 에서 COLMAP 으로 얻는 내부 파라미터를 EXIF 로 근사합니다.
        onLog("• [1/4] EXIF 카메라 파라미터 추출 중…")
        onProgress(8)

        val camInfos = withContext(Dispatchers.IO) {
            imageUris.map { uri -> MobileGaussianSplattingEngine.readCameraInfo(context, uri) }
        }

        val focalValues = camInfos.map { it.f35mm }.filter { it > 0 }
        if (focalValues.isNotEmpty()) {
            val avgF35 = focalValues.average()
            val minF35 = focalValues.min()
            val maxF35 = focalValues.max()
            onLog("  └ 35mm 환산 초점거리: 평균 ${String.format("%.0f", avgF35)}mm" +
                if (minF35 != maxF35) " (범위 ${minF35}–${maxF35}mm)" else "")
            onLog("  └ NDC 초점거리: ${String.format("%.2f", avgF35 / 18.0)} (FOV ≈ ${
                String.format("%.0f", Math.toDegrees(2 * Math.atan(18.0 / avgF35)))
            }°)")
        } else {
            onLog("  └ EXIF 초점거리 없음 → 기본값 27mm 사용")
        }

        imageUris.forEachIndexed { i, uri ->
            runCatching {
                val opts = android.graphics.BitmapFactory.Options()
                    .apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { s ->
                    android.graphics.BitmapFactory.decodeStream(s, null, opts)
                }
                val cam = camInfos[i]
                if (opts.outWidth > 0) {
                    val orientStr = if (cam.orientDeg != 0) " [회전 ${cam.orientDeg}°]" else ""
                    val focalStr  = if (cam.f35mm > 0) " f=${cam.f35mm}mm" else " f=기본"
                    onLog("  └ [${i+1}/${imageUris.size}] ${opts.outWidth}×${opts.outHeight}$focalStr$orientStr")
                }
            }
            onProgress(8 + (7 * (i + 1)) / imageUris.size)
            delay(15)
        }

        onLog("• [2/4] 피보나치 구체 위에 ${imageUris.size}개 가상 카메라 배치 중…")
        onLog("  └ 균등 구체 배치 → 수평 층(layer) 방지 · 3D 입체 복원")
        onProgress(20); delay(30)

        onLog("• [3/4] 다중 스케일 깊이 추정 + 역투영 중…")
        onProgress(25)

        // ── 씬 빌드 ──────────────────────────────────────────────────────
        val scene = withContext(Dispatchers.IO) {
            MobileGaussianSplattingEngine.buildSceneFromImages(
                context        = context,
                uris           = imageUris,
                cameraInfoList = camInfos.ifEmpty { null },
            )
        }

        if (scene == null || scene.splatCount <= 0) {
            onLog("[오류] 스플랫 생성 실패 — 이미지를 읽을 수 없거나 유효한 픽셀이 없습니다.")
            onProgress(100)
            return null
        }

        onLog("• [4/4] 씬 정규화 완료 (getNerfppNorm)")
        onLog("• 스플랫 수: ${scene.splatCount}개")
        onLog("• GLES2 원근 보정 포인트 스프라이트 렌더러로 전달")
        onProgress(100); delay(40)
        return scene
    }
}
