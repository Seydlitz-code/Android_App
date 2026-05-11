package com.example.app_01

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Mobile-GS 온디바이스 파이프라인 오케스트레이션.
 *
 * ## 메인 화면(Mobile 3DGS)에서의 입력 흐름
 * 1. **사진 데이터셋** — `Mobile3dGsScreen`에서 “사진 데이터셋 선택”을 누르면 팝업이 열리고,
 *    - **사진**: 앱 내 갤러리 라이브러리에서 [1, MAX_DATASET_IMAGES]장 다중 선택.
 *    - **데이터셋 폴더**: 앱 내 데이터셋 폴더 목록에서 폴더 하나 선택 → 이미지 확장자 파일을 순서대로 읽어
 *      최대 [MAX_DATASET_IMAGES]장까지 URI 목록으로 전달.
 * 2. **COLMAP 바이너리** — “COLMAP 바이너리 파일 선택”으로 SAF([OpenMultipleDocuments])를 열어
 *    기기 내 파일 **정확히 3개**(일반적으로 `cameras.bin`, `images.bin`, `points3D.bin`)를 선택.
 *    순서는 자유이며 [ColmapBinaryReader.resolveColmapTriple]이 파일 내용으로 판별.
 * 3. **실행** — [runFromSelectedInputs]가 위 URI들을 받아 분기:
 *    - COLMAP 3개가 있으면 **points3D** 기반 씬 생성(선택된 사진 장수는 로그·추후 확장 참고용).
 *    - COLMAP 없이 사진만 있으면 **이미지 전용** 깊이·역투영 파이프라인([runPipeline]).
 * 4. **출력** — 메모리상 [MobileSplatScene] → [MobileGaussianSplatGlView] 전용 뷰어.
 *
 * PC용 Mobile-GS 업스트림 Python(학습·렌더 등)은 APK에 포함하지 않습니다. 필요 시 저장소 또는
 * `scripts/fetch_mobile_gs_reference.ps1` 로 별도 내려받아 참고하세요.
 *
 * ## 처리 단계 (이미지만 입력, [runPipeline])
 *  1. EXIF 파싱 — 이미지별 초점거리(35mm 환산) 및 방향 추출
 *  2. 피보나치 구체 위에 가상 카메라 N개 균등 배치 (SfM 대체)
 *  3. 다중 스케일 깊이 추정 (선명도·분산·채도·중심·휘도)
 *  4. 역투영: 픽셀 → 3D 가우시안 (cameras.py · gaussian_model.py 참조)
 *  5. 씬 정규화 (getNerfppNorm 개념)
 */
object MobileGaussianSplattingScript {

    /** 갤러리·데이터셋 폴더에서 허용하는 최대 이미지 장수 (UI와 동일). */
    const val MAX_DATASET_IMAGES = 100

    /**
     * 메인 화면에서 모은 **사진 URI 목록**과 **COLMAP 바이너리 URI(0 또는 3개)**로 씬을 생성합니다.
     *
     * 우선순위: COLMAP 3개가 준비되어 있으면 [runColmapPointCloudPipeline],
     * 아니면 사진 1장 이상으로 [runPipeline].
     */
    suspend fun runFromSelectedInputs(
        context: Context,
        imageUris: List<Uri>,
        colmapUris: List<Uri>,
        onLog: suspend (String) -> Unit = {},
        onProgress: suspend (Int) -> Unit,
    ): MobileSplatScene? {
        val colmapOk = colmapUris.size == 3
        val nImg = imageUris.size
        if (nImg > MAX_DATASET_IMAGES) {
            onLog("[오류] 사진은 최대 ${MAX_DATASET_IMAGES}장까지입니다.")
            onProgress(100)
            return null
        }
        return when {
            colmapOk -> {
                if (nImg > 0) {
                    onLog("• 입력: COLMAP 바이너리 3개 + 참고 사진 ${nImg}장")
                } else {
                    onLog("• 입력: COLMAP 바이너리 3개 (사진 미선택)")
                }
                runColmapPointCloudPipeline(
                    context,
                    colmapUris,
                    datasetImageCount = nImg,
                    onLog = onLog,
                    onProgress = onProgress,
                )
            }
            nImg >= 1 -> {
                onLog("• 입력: 사진 ${nImg}장 (온디바이스 이미지 파이프라인)")
                runPipeline(context, imageUris, onLog, onProgress)
            }
            else -> {
                onLog("[오류] COLMAP 파일 3개 또는 사진 1~${MAX_DATASET_IMAGES}장이 필요합니다.")
                onProgress(100)
                null
            }
        }
    }

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

    /**
     * COLMAP sparse 바이너리 3개(cameras.bin, images.bin, points3D.bin)를 읽어
     * [points3D] 점군으로 [MobileSplatScene] 을 만듭니다 (Mobile-GS `colmap_loader.py` 경로).
     *
     * @param datasetImageCount 갤러리/폴더에서 선택한 사진 장수(참고 로그용, 씬 생성에는 points3D 사용).
     */
    suspend fun runColmapPointCloudPipeline(
        context: Context,
        colmapUris: List<Uri>,
        datasetImageCount: Int,
        onLog: suspend (String) -> Unit,
        onProgress: suspend (Int) -> Unit,
    ): MobileSplatScene? {
        require(colmapUris.size == 3) { "COLMAP 바이너리 3개가 필요합니다." }
        onLog("=== COLMAP points3D → Mobile 3DGS ===")
        onProgress(2)
        delay(20)

        onLog("• 바이너리 읽는 중…")
        val triplePairs = withContext(Dispatchers.IO) {
            val list = ArrayList<Pair<String, ByteArray>>(3)
            for (uri in colmapUris) {
                val name = colmapDisplayName(context, uri)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) return@withContext null
                list.add(name to bytes)
            }
            list
        }
        if (triplePairs == null) {
            onLog("[오류] 파일을 읽을 수 없습니다.")
            onProgress(100)
            return null
        }

        onProgress(12)
        val colmap = ColmapBinaryReader.resolveColmapTripleWithPointsFallback(
            triplePairs[0],
            triplePairs[1],
            triplePairs[2],
        )
        if (colmap == null) {
            val hint = ColmapBinaryReader.diagnoseColmapTripleFailure(
                triplePairs[0],
                triplePairs[1],
                triplePairs[2],
            )
            onLog("[오류] $hint")
            onProgress(100)
            return null
        }

        if (colmap.cameras.isEmpty() && colmap.images.isEmpty()) {
            onLog("• 참고: cameras/images는 생략하고 points3D 점군만 사용합니다 (뷰어 표시에 충분).")
        }
        onLog(
            "• COLMAP: cameras=${colmap.cameras.size}, images=${colmap.images.size}, " +
                "points3D=${colmap.points.count}"
        )
        if (datasetImageCount > 0) {
            onLog("• 선택된 사진: ${datasetImageCount}장 (씬은 points3D 기반)")
        }

        onProgress(35)
        delay(10)

        val scene = withContext(Dispatchers.IO) {
            MobileGaussianSplattingEngine.buildSceneFromColmapPointCloud(colmap.points)
        }

        if (scene == null || scene.splatCount <= 0) {
            onLog("[오류] 점군이 비어 있거나 변환에 실패했습니다.")
            onProgress(100)
            return null
        }

        onLog("• 스플랫 수: ${scene.splatCount}")
        onLog("• GLES2 포인트 스플랫 뷰어로 표시")
        onProgress(100)
        delay(30)
        return scene
    }

    private fun colmapDisplayName(context: Context, uri: Uri): String {
        val fromQuery = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            }
        }.getOrNull()
        if (!fromQuery.isNullOrBlank()) return fromQuery
        return uri.lastPathSegment?.substringAfterLast(':') ?: "file.bin"
    }
}
