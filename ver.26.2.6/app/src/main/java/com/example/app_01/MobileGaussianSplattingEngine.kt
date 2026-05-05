package com.example.app_01

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Mobile-GS 개념 기반 Android 온디바이스 3D 가우시안 스플랫 엔진 (v3).
 *
 * ## 참조 소스 (Mobile-GS ICLR 2026, github.com/xiaobiaodu/Mobile-GS)
 *
 *  - scene/cameras.py         : 핀홀 카메라 모델, FoV ↔ focal 변환
 *  - scene/gaussian_model.py  : 가우시안 파라미터 초기화 (xyz, RGB→SH, opacity, scale)
 *  - scene/dataset_readers.py : getNerfppNorm — 씬 정규화
 *  - utils/graphics_utils.py  : focal2fov / getWorld2View2
 *
 * ## 사진만 입력받는 온디바이스 파이프라인
 *
 * 1. **EXIF 카메라 내부 파라미터 추론**
 *    - `ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM` 에서 35mm 환산 초점거리 읽기
 *    - NDC 초점거리: `f_ndc = f_35mm / 18`  (35mm 센서 반폭 18mm 기준)
 *    - EXIF 없으면 27mm(일반 스마트폰 메인 카메라) 기본값 사용
 *
 * 2. **피보나치 구체(Fibonacci Sphere) 카메라 포즈 근사** (SfM 대체)
 *    - N장의 사진을 구체면에 균등 배치 → 수평 층(layer) 방지
 *    - 극(pole)에 가까울 때 right 벡터 안전 처리
 *
 * 3. **다중 단서 깊이 추정** (단안 추정, SfM 불필요)
 *    - 다중 스케일 라플라시안 선명도 (세밀 3×3 + 거친 7×7)
 *    - 로컬 분산, 채도, 이미지 중심 가중치, 휘도 역전
 *
 * 4. **일반화 look-at 역투영** (구체 위 임의 위치 카메라 지원)
 *    - right = normalize(cross(forward, worldUp))
 *    - up = cross(right, forward)
 *    - d_world = rx·right + ry·up + rz·backward
 *
 * 5. **가우시안 초기화 + 씬 정규화** (NeRF++ / getNerfppNorm 스타일)
 */
data class MobileSplatScene(
    val positions: FloatArray,
    val colors: FloatArray,
    val sizes: FloatArray,
    val splatCount: Int,
)

/** EXIF 에서 추출한 카메라 내부 파라미터 */
data class CameraInfo(
    val focalNdc: Float,    // NDC 초점거리 = f_35mm / 18
    val f35mm: Int,         // 35mm 환산 초점거리 (mm), 0이면 EXIF 없음
    val orientDeg: Int,     // EXIF 회전 각도 (0·90·180·270)
)

object MobileGaussianSplattingEngine {

    // ── 설정 상수 ─────────────────────────────────────────────────────────────
    private const val MAX_SPLATS     = 20_000
    /** COLMAP points3D 기반 뷰어: 점이 많을 때도 디테일 유지 (메모리·GPU 여유 전제) */
    private const val MAX_COLMAP_SPLATS = 120_000
    private const val DECODE_SIDE    = 224
    private const val BASE_GRID_STEP = 4
    private const val SCENE_RADIUS   = 0.9f
    /** 가상 카메라 구체 반경 */
    private const val CAM_SPHERE_R   = 1.0f

    // 35mm 환산 기본 초점거리 (EXIF 없는 경우) — 27mm ≈ 스마트폰 메인 카메라
    private const val DEFAULT_F35MM    = 27
    // NDC 초점거리 = f_35mm / 18  (35mm 센서 반폭 18mm, graphics_utils.py 방식)
    private val DEFAULT_FOCAL_NDC = DEFAULT_F35MM.toFloat() / 18f   // ≈ 1.50

    // SH degree-0 상수 (Mobile-GS sh_utils.py C0 = 1/(2√π))
    private const val C0 = 0.28209479177387814f

    // =========================================================================
    // 공개 API — 바이너리 스플랫 파싱 (서버 연동 시)
    // =========================================================================

    fun parseSplatBinary(bytes: ByteArray): MobileSplatScene? {
        if (bytes.size < 4) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val n = buf.int
        if (n <= 0 || bytes.size < 4 + n * 28) return null
        val pos = FloatArray(n * 3) { buf.float }
        val col = FloatArray(n * 3) { buf.float }
        val siz = FloatArray(n) { buf.float }
        return MobileSplatScene(positions = pos, colors = col, sizes = siz, splatCount = n)
    }

    // =========================================================================
    // EXIF 카메라 내부 파라미터 추론
    // =========================================================================

    /**
     * EXIF 에서 35mm 환산 초점거리를 읽어 NDC 초점거리로 변환합니다.
     *
     * Mobile-GS graphics_utils.py 의 focal2fov / fov2focal 참조:
     *   NDC_focal = f_pixel / (image_width / 2)
     *   f_pixel   = f_35mm  * image_width / 36  (35mm 센서 폭 36mm 기준)
     * → NDC_focal = f_35mm / 18
     *
     * 지원 태그 (우선순위):
     *   1. TAG_FOCAL_LENGTH_IN_35MM_FILM (직접 사용)
     *   2. TAG_FOCAL_LENGTH + TAG_DIGITAL_ZOOM_RATIO (간접 환산 — 부정확할 수 있음)
     */
    fun readCameraInfo(context: Context, uri: Uri): CameraInfo {
        val default = CameraInfo(DEFAULT_FOCAL_NDC, 0, 0)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)

                val f35 = exif.getAttributeInt(
                    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)

                val orientRaw = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)
                val orientDeg = when (orientRaw) {
                    ExifInterface.ORIENTATION_ROTATE_90,
                    ExifInterface.ORIENTATION_TRANSVERSE  -> 90
                    ExifInterface.ORIENTATION_ROTATE_180,
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
                    ExifInterface.ORIENTATION_ROTATE_270,
                    ExifInterface.ORIENTATION_TRANSPOSE   -> 270
                    else -> 0
                }

                if (f35 in 8..500) {
                    CameraInfo(
                        focalNdc  = f35.toFloat() / 18f,
                        f35mm     = f35,
                        orientDeg = orientDeg,
                    )
                } else {
                    default.copy(orientDeg = orientDeg)
                }
            } ?: default
        }.getOrElse { default }
    }

    // =========================================================================
    // 피보나치 구체(Fibonacci Sphere) 카메라 배치
    // =========================================================================

    /**
     * 피보나치 구체 위의 i번째 점을 단위 벡터로 반환합니다.
     *
     * 황금각(golden angle = π(3−√5))을 이용해 N개의 점을 구면에 균등 배치합니다.
     * Mobile-GS 는 COLMAP으로 실제 포즈를 쓰지만, 온디바이스에서는 이 근사를 사용합니다.
     *
     * @return FloatArray[3] = (cx, cy, cz), |v| = 1
     */
    private fun fibonacciSpherePoint(i: Int, n: Int): FloatArray {
        val goldenAngle = PI * (3.0 - sqrt(5.0))
        val nSafe = maxOf(n, 2)
        // cy: 북극(1) → 남극(-1) 균등 배치
        val cy = 1.0 - (i.toDouble() / (nSafe - 1)) * 2.0
        val r  = sqrt(maxOf(0.0, 1.0 - cy * cy))
        val theta = goldenAngle * i
        return floatArrayOf(
            (cos(theta) * r).toFloat(),
            cy.toFloat(),
            (sin(theta) * r).toFloat(),
        )
    }

    /**
     * 구체 위의 카메라 위치 (cx,cy,cz)에서 원점(씬 중심)을 향하는 right/up/backward 축을 반환합니다.
     *
     * Mobile-GS cameras.py 의 world_view_transform 로직 참조:
     *   forward  = normalize(-camPos)
     *   right    = normalize(cross(forward, worldUp))   ← worldUp = (0,1,0)
     *   up       = cross(right, forward)
     *   backward = -forward = camPos / |camPos|
     *
     * 극(pole) 근처(rHoriz < ε)에서는 worldUp 대신 (0,0,1) 사용해 퇴화 방지.
     *
     * @return FloatArray[9] = (right.xyz, up.xyz, backward.xyz)
     */
    private fun cameraAxesFromSpherePos(cx: Float, cy: Float, cz: Float): FloatArray {
        val rHoriz = sqrt(cx * cx + cz * cz)

        val rightX: Float; val rightY: Float; val rightZ: Float
        val upX: Float;    val upY: Float;    val upZ: Float

        if (rHoriz > 1e-4f) {
            // 일반 케이스: right = normalize(cz, 0, -cx)
            rightX = cz / rHoriz;  rightY = 0f;  rightZ = -cx / rHoriz
            // up = cross(right, -camPos/1) = cross(right, forward)
            // forward = (-cx,-cy,-cz), right = (rightX,0,rightZ)
            // up.x = right.y*fwd.z - right.z*fwd.y = 0*(-cz) - rightZ*(-cy) = rightZ*cy
            // up.y = right.z*fwd.x - right.x*fwd.z = rightZ*(-cx) - rightX*(-cz)
            //      = -rightZ*cx + rightX*cz = (-cx/rHoriz)*cx + (cz/rHoriz)*cz
            //      = (cz²+cx²)/rHoriz... wait let me redo
            // Actually: up = cross(right, forward) where forward = (-cx,-cy,-cz)
            val fwdX = -cx; val fwdY = -cy; val fwdZ = -cz
            upX = rightY * fwdZ - rightZ * fwdY   // = 0*fwdZ - rightZ*fwdY = -rightZ*(-cy) = rightZ*cy
            upY = rightZ * fwdX - rightX * fwdZ   // = rightZ*(-cx) - rightX*(-cz) = -rightZ*cx + rightX*cz
            upZ = rightX * fwdY - rightY * fwdX   // = rightX*(-cy) - 0 = -rightX*cy
        } else {
            // 극(pole) 케이스: cy ≈ ±1
            // 카메라가 위에서 아래(cy=+1) 또는 아래서 위(cy=-1)를 향함
            // right = +X축, up은 forward에 수직인 축으로 설정
            rightX = 1f; rightY = 0f; rightZ = 0f
            // forward = (0,-cy,0) 대략
            // up = cross(right=(1,0,0), forward=(0,sign(-cy),0)) = (0*0-0*sign(-cy), 0*0-1*0, 1*sign(-cy)-0*0) = (0,0,-cy)
            upX = 0f; upY = 0f; upZ = -cy
        }
        // backward = camPos (단위 벡터)
        return floatArrayOf(rightX, rightY, rightZ, upX, upY, upZ, cx, cy, cz)
    }

    // =========================================================================
    // 공개 API — 이미지에서 스플랫 씬 빌드
    // =========================================================================

    /**
     * 이미지 URI 목록에서 [MobileSplatScene]을 생성합니다.
     *
     * @param cameraInfoList  외부에서 미리 읽은 EXIF 정보 (없으면 내부에서 읽음)
     */
    fun buildSceneFromImages(
        context: Context,
        uris: List<Uri>,
        cameraInfoList: List<CameraInfo>? = null,
    ): MobileSplatScene? {

        if (uris.isEmpty()) return null

        val allPos = ArrayList<Float>(MAX_SPLATS * 3)
        val allCol = ArrayList<Float>(MAX_SPLATS * 3)
        val allSiz = ArrayList<Float>(MAX_SPLATS)

        val n = uris.size

        // ── 이미지 전체의 평균 focal 계산 (logging 목적) ─────────────────────
        val camInfos: List<CameraInfo> = cameraInfoList
            ?: uris.map { readCameraInfo(context, it) }
        val avgF35 = camInfos.map { it.f35mm }.filter { it > 0 }
            .let { if (it.isEmpty()) DEFAULT_F35MM else it.average().toInt() }

        // ── gridStep 결정 (첫 이미지 크기 기준) ──────────────────────────────
        var gridStep = BASE_GRID_STEP
        var gridStepSet = false

        for ((idx, uri) in uris.withIndex()) {
            if (allPos.size / 3 >= MAX_SPLATS) break

            val bmp = decodeBitmapSoftware(context, uri, DECODE_SIDE) ?: continue
            val w = bmp.width; val h = bmp.height

            if (!gridStepSet) {
                val remaining = (MAX_SPLATS - allPos.size / 3).coerceAtLeast(1)
                gridStep = calcGridStep(n, w, h, remaining)
                gridStepSet = true
            }

            val pixels = IntArray(w * h)
            try {
                bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            } catch (_: Exception) { bmp.recycle(); continue }
            bmp.recycle()

            // ── 깊이 맵 계산 ───────────────────────────────────────────────
            val depthMap = computeDepthMap(pixels, w, h)

            // ── 피보나치 구체 카메라 포즈 ──────────────────────────────────
            val spherePos = fibonacciSpherePoint(idx, n)
            val camX = CAM_SPHERE_R * spherePos[0]
            val camY = CAM_SPHERE_R * spherePos[1]
            val camZ = CAM_SPHERE_R * spherePos[2]

            // ── EXIF 초점거리 ──────────────────────────────────────────────
            val focalNdc = camInfos[idx].focalNdc

            // ── 역투영 ─────────────────────────────────────────────────────
            extractGaussiansFromView(
                pixels    = pixels,
                depthMap  = depthMap,
                w         = w, h = h,
                camX      = camX, camY = camY, camZ = camZ,
                focalNdc  = focalNdc,
                viewIdx   = idx, totalViews = n,
                gridStep  = gridStep,
                pos       = allPos, col = allCol, siz = allSiz,
            )
        }

        if (allPos.size < 9) return null
        normalizeScene(allPos)

        val count = allPos.size / 3
        return MobileSplatScene(
            positions  = allPos.toFloatArray(),
            colors     = allCol.toFloatArray(),
            sizes      = allSiz.toFloatArray(),
            splatCount = count,
        )
    }

    // =========================================================================
    // (1) 다중 스케일 깊이 추정 (monocular, scene/cameras.py 개념 참조)
    // =========================================================================

    /**
     * 픽셀 배열에서 [0,1] 깊이 맵을 반환합니다. 0 = 가까움, 1 = 멂.
     *
     * Mobile-GS 에는 없는 단안 추정 로직이지만, 피처 기반 깊이 단서를 사용합니다:
     *  1. 다중 스케일 라플라시안 선명도 (3×3 세밀 + 7×7 거친)
     *  2. 로컬 분산 (질감 풍부도)
     *  3. 채도 (피사체 강조)
     *  4. 중심 가중치 (360° 데이터셋: 피사체는 항상 중심에 위치)
     *  5. 휘도 역전 (어두운 = 가까움 — 낮은 가중치)
     */
    private fun computeDepthMap(pixels: IntArray, w: Int, h: Int): FloatArray {
        val luma = FloatArray(w * h)
        val sat  = FloatArray(w * h)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)   / 255f
            val g = Color.green(p) / 255f
            val b = Color.blue(p)  / 255f
            luma[i] = 0.299f * r + 0.587f * g + 0.114f * b
            val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
            sat[i] = if (mx > 1e-4f) (mx - mn) / mx else 0f
        }

        // 세밀 라플라시안 (3×3)
        val lapFine = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                lapFine[y * w + x] = abs(
                    luma[(y-1)*w+x] + luma[(y+1)*w+x] +
                    luma[y*w+(x-1)] + luma[y*w+(x+1)] - 4f * luma[y*w+x])
            }
        }
        // 거친 라플라시안 (7×7 — 박스 블러 후 차분)
        val lumaBlur7 = boxBlur(luma, w, h, radius = 3)
        val lapCoarse = FloatArray(w * h) { i ->
            abs(luma[i] - lumaBlur7[i])
        }

        val sharpFine    = boxBlur(lapFine, w, h, radius = 2)
        val sharpCoarse  = boxBlur(lapCoarse, w, h, radius = 4)
        val maxSF = sharpFine.max().coerceAtLeast(1e-4f)
        val maxSC = sharpCoarse.max().coerceAtLeast(1e-4f)

        val variance = localVariance(luma, w, h, radius = 3)
        val maxVar   = variance.max().coerceAtLeast(1e-4f)

        val satSmooth = boxBlur(sat, w, h, radius = 3)
        val maxSat    = satSmooth.max().coerceAtLeast(1e-4f)

        val cx = (w - 1) / 2f; val cy = (h - 1) / 2f
        val maxDist = sqrt(cx * cx + cy * cy).coerceAtLeast(1f)

        val depth = FloatArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val sf   = sharpFine[i]    / maxSF
                val sc   = sharpCoarse[i]  / maxSC
                val vr   = variance[i]     / maxVar
                val sa   = satSmooth[i]    / maxSat
                val cw   = 1f - sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maxDist
                val li   = 1f - luma[i]

                // 가중치 (Mobile-GS 논문 기반 경험적 조정):
                //   - 중심 가중치(cw) 최고: 360° 데이터셋에서 피사체 = 중심
                //   - 채도(sa): 유채색 피사체 강조
                //   - 다중 스케일 선명도: 엣지+전경 모두 포착
                val nearScore = sf * 0.22f +
                                sc * 0.12f +
                                vr * 0.18f +
                                sa * 0.23f +
                                cw * 0.22f +
                                li * 0.03f
                depth[i] = (1f - nearScore).coerceIn(0f, 1f)
            }
        }
        return depth
    }

    // =========================================================================
    // (2) 역투영 + 가우시안 초기화 (cameras.py · gaussian_model.py 참조)
    // =========================================================================

    /**
     * 한 뷰(카메라)에서 픽셀을 3D 점으로 역투영하고 가우시안 파라미터를 초기화합니다.
     *
     * ### 카메라 모델 (cameras.py 참조)
     * - 핀홀 카메라: `xNdc / focalNdc = tan(angH)`, `yNdc * aspectInv / focalNdc = tan(angV)`
     * - 카메라 위치: 피보나치 구체 위의 점 `(camX, camY, camZ)`, 원점을 향함
     * - 회전 행렬: `cameraAxesFromSpherePos` 로 일반화 look-at 계산
     *
     * ### 깊이 범위 (graphics_utils.py 참조)
     * - 카메라~원점 거리 = CAM_SPHERE_R = 1.0
     * - 피사체(차량) 반경 ≈ 0.45 → 근면: 1.0−0.45=0.55, 원면: 1.0+0.45=1.45
     * - 매핑: `dist = 0.55 + d * 0.90`  (d ∈ [0,1] 깊이 맵)
     *
     * ### 가우시안 초기화 (gaussian_model.py create_from_pcd 참조)
     * - 위치: `camPos + rayDir * dist`
     * - 색상: RGB 직접 (SH DC = `(rgb−0.5)/C0` 역변환 없이 렌더러에 RGB 전달)
     * - 스케일(gl_PointSize): 깊이 불확실도 비례
     *
     * ### 그리드 위상 엇갈리기
     * - 뷰마다 시작 픽셀 offset을 달리해 층(layer) 정렬 방지
     *
     * @param focalNdc  EXIF에서 읽은 NDC 초점거리 = f_35mm / 18
     */
    private fun extractGaussiansFromView(
        pixels: IntArray,
        depthMap: FloatArray,
        w: Int, h: Int,
        camX: Float, camY: Float, camZ: Float,
        focalNdc: Float,
        viewIdx: Int, totalViews: Int,
        gridStep: Int,
        pos: ArrayList<Float>,
        col: ArrayList<Float>,
        siz: ArrayList<Float>,
    ) {
        // ── 카메라 축 계산 (일반화 look-at) ──────────────────────────────────
        // camPos 를 단위 구체 위로 정규화 (CAM_SPHERE_R=1.0 이므로 이미 단위 벡터)
        val camLen = sqrt(camX*camX + camY*camY + camZ*camZ).coerceAtLeast(1e-6f)
        val ucx = camX / camLen;  val ucy = camY / camLen;  val ucz = camZ / camLen
        val axes = cameraAxesFromSpherePos(ucx, ucy, ucz)
        // axes[0..2] = right, axes[3..5] = up, axes[6..8] = backward
        val rX = axes[0]; val rY = axes[1]; val rZ = axes[2]
        val uX = axes[3]; val uY = axes[4]; val uZ = axes[5]
        val bX = axes[6]; val bY = axes[7]; val bZ = axes[8]

        // ── 그리드 위상 엇갈리기 ────────────────────────────────────────────
        val phaseY = (viewIdx * 3) % gridStep
        val phaseX = (viewIdx * 5) % gridStep

        val aspectInv = h.toFloat() / w.toFloat()

        var y = phaseY
        while (y < h) {
            var x = phaseX
            while (x < w) {
                if (pos.size / 3 >= MAX_SPLATS) return

                val idx   = y * w + x
                val pixel = pixels[idx]
                if (Color.alpha(pixel) < 8) { x += gridStep; continue }

                // ── 색상 ─────────────────────────────────────────────────────
                val rf = Color.red(pixel)   / 255f
                val gf = Color.green(pixel) / 255f
                val bf = Color.blue(pixel)  / 255f

                // ── 배경 픽셀 필터링 ─────────────────────────────────────────
                // 1) 밝고 채도 낮은 픽셀: 하늘·흰 배경
                val lumaL = 0.299f * rf + 0.587f * gf + 0.114f * bf
                val maxC  = maxOf(rf, gf, bf)
                val satL  = if (maxC > 0.02f) (maxC - minOf(rf, gf, bf)) / maxC else 0f
                if (lumaL > 0.87f && satL < 0.15f) { x += gridStep; continue }
                // 2) 매우 어두운 그림자
                if (lumaL < 0.04f) { x += gridStep; continue }

                // ── 깊이 → 카메라~3D점 거리 ──────────────────────────────────
                // dist 범위 [0.55, 1.45]: 카메라 반경 1.0 기준으로 피사체 앞뒤를 포괄
                // depth=0(선명·근접) → dist=0.55, depth=1(배경) → dist=1.45
                val d    = depthMap[idx]
                val dist = 0.55f + d * 0.90f

                // ── 픽셀 → NDC (cameras.py 핀홀 모델) ─────────────────────────
                val xNdc =  (x / (w - 1f)) * 2f - 1f
                val yNdc = -((y / (h - 1f)) * 2f - 1f) * aspectInv

                // ── 카메라 공간 방향 벡터 ─────────────────────────────────────
                // xNdc / focalNdc = tan(angH)  ←→  cameras.py focal2fov 역변환
                val rx = xNdc / focalNdc
                val ry = yNdc / focalNdc
                val rz = -1f   // 카메라 -Z 방향이 전방
                val rLen = sqrt(rx*rx + ry*ry + rz*rz)
                val rxN = rx / rLen;  val ryN = ry / rLen;  val rzN = rz / rLen

                // ── 카메라 공간 → 월드 공간 (일반화 회전 행렬) ────────────────
                // d_world = rxN*right + ryN*up + rzN*backward
                val wdx = rxN * rX + ryN * uX + rzN * bX
                val wdy = rxN * rY + ryN * uY + rzN * bY
                val wdz = rxN * rZ + ryN * uZ + rzN * bZ

                // ── 월드 3D 위치: camPos + rayDir * dist ────────────────────
                val px = camX + wdx * dist
                val py = camY + wdy * dist
                val pz = camZ + wdz * dist

                pos.add(px); pos.add(py); pos.add(pz)

                // ── 색상 (RGB 직접 전달) ─────────────────────────────────────
                // gaussian_model.py: f_dc = (rgb - 0.5) / C0
                // GLES 렌더러는 RGB 직접 사용하므로 변환 없이 전달
                col.add(rf); col.add(gf); col.add(bf)

                // ── gl_PointSize (gaussian_model.py scaling 참조) ────────────
                // GLES 셰이더에서 gl_Position.w 로 나누므로 기준 거리 기반으로 설정
                val depthGrad = if (idx > 0 && idx < depthMap.size - 1)
                    abs(depthMap[idx + 1] - depthMap[idx - 1]) * 0.5f else 0f
                val scaleFactor = 1f + depthGrad * 3f
                val screenSize  = (5f + 13f * (1f - d)) * scaleFactor * (DECODE_SIDE.toFloat() / maxOf(w, h))
                siz.add(screenSize.coerceIn(2f, 38f))

                x += gridStep
            }
            y += gridStep
        }
    }

    // =========================================================================
    // (3) 씬 정규화 (dataset_readers.py — getNerfppNorm)
    // =========================================================================

    /**
     * 점 구름을 GLES2 뷰어 범위([-SCENE_RADIUS, SCENE_RADIUS])로 정규화합니다.
     *
     * dataset_readers.py getNerfppNorm:
     *   center = mean(all_points)
     *   radius = max(dist(point, center))
     *   translate = -center,  scale = SCENE_RADIUS / radius
     */
    private fun normalizeScene(pos: ArrayList<Float>) {
        val n = pos.size / 3; if (n == 0) return
        var cx = 0f; var cy = 0f; var cz = 0f
        for (i in 0 until n) { cx += pos[i*3]; cy += pos[i*3+1]; cz += pos[i*3+2] }
        cx /= n; cy /= n; cz /= n
        var radius = 0f
        for (i in 0 until n) {
            val dx = pos[i*3]-cx; val dy = pos[i*3+1]-cy; val dz = pos[i*3+2]-cz
            radius = maxOf(radius, sqrt(dx*dx + dy*dy + dz*dz))
        }
        if (radius < 1e-6f) return
        val scale = SCENE_RADIUS / radius
        for (i in 0 until n) {
            pos[i*3]   = (pos[i*3]   - cx) * scale
            pos[i*3+1] = (pos[i*3+1] - cy) * scale
            pos[i*3+2] = (pos[i*3+2] - cz) * scale
        }
    }

    // =========================================================================
    // 유틸리티
    // =========================================================================

    private fun calcGridStep(imgCount: Int, bmpW: Int, bmpH: Int, maxImgSplats: Int = MAX_SPLATS): Int {
        if (imgCount <= 0 || bmpW <= 0 || bmpH <= 0 || maxImgSplats <= 0) return BASE_GRID_STEP
        val perImg = maxImgSplats.toDouble() / imgCount.toDouble()
        val step = sqrt((bmpW.toDouble() * bmpH.toDouble()) / perImg.coerceAtLeast(1.0))
        return max(BASE_GRID_STEP, ceil(step).toInt())
    }

    private fun decodeBitmapSoftware(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val origW = bounds.outWidth; val origH = bounds.outHeight
        if (origW <= 0 || origH <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize       = calcSampleSize(origW, origH, maxSide)
            inPreferredConfig  = Bitmap.Config.ARGB_8888
        }
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        return if (bmp.config == Bitmap.Config.HARDWARE) {
            val soft = bmp.copy(Bitmap.Config.ARGB_8888, false); bmp.recycle(); soft
        } else bmp
    }

    private fun calcSampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= maxSide && height <= maxSide) return 1
        var s = 1; val longest = max(width, height)
        while (longest / (s * 2) >= maxSide) s *= 2
        if (longest / s > maxSide) s *= 2
        return max(1, s)
    }

    private fun boxBlur(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val tmp = FloatArray(src.size); val dst = FloatArray(src.size)
        for (y in 0 until h) {
            var sum = 0f; var cnt = 0
            for (x in 0 until minOf(radius, w)) { sum += src[y*w+x]; cnt++ }
            for (x in 0 until w) {
                val add = x + radius; val rem = x - radius - 1
                if (add < w) { sum += src[y*w+add]; cnt++ }
                if (rem >= 0) { sum -= src[y*w+rem]; cnt-- }
                tmp[y*w+x] = sum / cnt.coerceAtLeast(1)
            }
        }
        for (x in 0 until w) {
            var sum = 0f; var cnt = 0
            for (y in 0 until minOf(radius, h)) { sum += tmp[y*w+x]; cnt++ }
            for (y in 0 until h) {
                val add = y + radius; val rem = y - radius - 1
                if (add < h) { sum += tmp[add*w+x]; cnt++ }
                if (rem >= 0) { sum -= tmp[rem*w+x]; cnt-- }
                dst[y*w+x] = sum / cnt.coerceAtLeast(1)
            }
        }
        return dst
    }

    private fun localVariance(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val mean = boxBlur(src, w, h, radius)
        val sq   = FloatArray(src.size) { i -> src[i] * src[i] }
        val meanSq = boxBlur(sq, w, h, radius)
        return FloatArray(src.size) { i -> max(0f, meanSq[i] - mean[i] * mean[i]) }
    }

    // =========================================================================
    // COLMAP points3D.bin → 스플랫 씬 (Mobile-GS scene/colmap_loader.py 참조)
    // =========================================================================

    /**
     * COLMAP `points3D.bin` 에서 읽은 점군으로 [MobileSplatScene] 을 만듭니다.
     *
     * - 상한 [maxSplats] 초과 시 **재투영 오차가 작고 트랙(관측)이 긴 점**을 우선 선택합니다.
     * - 스플랫 반경은 오차·관측 수에 따라 달리 해 빈 공간을 줄입니다.
     */
    fun buildSceneFromColmapPointCloud(
        bundle: ColmapBinaryReader.Points3dBin,
        maxSplats: Int = MAX_COLMAP_SPLATS,
    ): MobileSplatScene? {
        val nIn = bundle.count
        if (nIn <= 0) return null
        val cap = minOf(maxSplats, nIn)

        val sortedIdx: List<Int>? =
            if (nIn > cap) {
                (0 until nIn).sortedWith(
                    compareBy({ bundle.reprojErr[it] }, { -bundle.trackLen[it] }),
                )
            } else {
                null
            }

        val densityScale =
            kotlin.math.sqrt((35_000.0 / cap.toDouble()).coerceIn(0.22, 1.05)).toFloat()

        val allPos = ArrayList<Float>(cap * 3)
        val allCol = ArrayList<Float>(cap * 3)
        val allSiz = ArrayList<Float>(cap)
        for (k in 0 until cap) {
            val i = sortedIdx?.get(k) ?: k
            val o = i * 3
            allPos.add(bundle.xyz[o])
            allPos.add(bundle.xyz[o + 1])
            allPos.add(bundle.xyz[o + 2])
            val cr = (bundle.rgb[o] * 1.06f).coerceIn(0f, 1f)
            val cg = (bundle.rgb[o + 1] * 1.06f).coerceIn(0f, 1f)
            val cb = (bundle.rgb[o + 2] * 1.06f).coerceIn(0f, 1f)
            allCol.add(cr)
            allCol.add(cg)
            allCol.add(cb)

            val err = bundle.reprojErr.getOrElse(i) { 1f }.coerceIn(0f, 80f)
            val tr = bundle.trackLen.getOrElse(i) { 1 }.coerceAtLeast(1)
            val conf = (1.12f / (0.2f + err)).coerceIn(0.42f, 2.5f)
            val obsBoost = (0.68f + 0.15f * ln(tr.toFloat())).coerceIn(0.68f, 1.7f)
            val sz = 2.45f * conf * obsBoost * densityScale
            allSiz.add(sz.coerceIn(1.15f, 15f))
        }
        if (allPos.size < 9) return null
        normalizeScene(allPos)
        val count = allPos.size / 3
        return MobileSplatScene(
            positions  = allPos.toFloatArray(),
            colors     = allCol.toFloatArray(),
            sizes      = allSiz.toFloatArray(),
            splatCount = count,
        )
    }
}
