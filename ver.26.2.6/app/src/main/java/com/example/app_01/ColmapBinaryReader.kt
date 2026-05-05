package com.example.app_01

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * COLMAP sparse reconstruction 바이너리 형식 파서.
 *
 * 참조: Mobile-GS `scene/colmap_loader.py` (원본 Inria / COLMAP `read_write_model.py` 계열)
 *
 * 세 파일: `cameras.bin`, `images.bin`, `points3D.bin`
 */
object ColmapBinaryReader {

    data class CameraBin(
        val id: Int,
        val modelId: Int,
        val width: Long,
        val height: Long,
        val params: DoubleArray,
    )

    data class ImageBin(
        val id: Long,
        val qvec: DoubleArray,
        val tvec: DoubleArray,
        val cameraId: Int,
        val name: String,
    )

    data class Points3dBin(
        val xyz: FloatArray,
        val rgb: FloatArray,
        /** COLMAP 재투영 오차(픽셀), 가중 선택·스플랫 크기에 사용 */
        val reprojErr: FloatArray,
        /** 트랙 길이(관측 수), 신뢰도 가중에 사용 */
        val trackLen: IntArray,
        val count: Int,
    )

    data class ColmapTriple(
        val cameras: Map<Int, CameraBin>,
        val images: List<ImageBin>,
        val points: Points3dBin,
    )

    /**
     * COLMAP sparse 3파일을 한 번에 해석합니다.
     * 1) 파일명 힌트(확장자 없는 SAF 표시명 포함) 2) 실패 시 3바이트 배열의 6순열 자동 시도
     */
    fun resolveColmapTriple(
        a: Pair<String, ByteArray>,
        b: Pair<String, ByteArray>,
        c: Pair<String, ByteArray>,
    ): ColmapTriple? {
        assignByFileName(Triple(a, b, c))?.let { return it }
        val blobs = listOf(a.second, b.second, c.second)
        val perms = listOf(
            intArrayOf(0, 1, 2),
            intArrayOf(0, 2, 1),
            intArrayOf(1, 0, 2),
            intArrayOf(1, 2, 0),
            intArrayOf(2, 0, 1),
            intArrayOf(2, 1, 0),
        )
        for (p in perms) {
            parseTriple(blobs[p[0]], blobs[p[1]], blobs[p[2]])?.let { return it }
        }
        return null
    }

    /**
     * [resolveColmapTriple] 후, 온디바이스 뷰어는 **points3D 점군**만 필요하므로
     * cameras/images 파싱이 실패해도 **points3D.bin 내용만 유효하면** 씬을 만들 수 있게 합니다.
     */
    fun resolveColmapTripleWithPointsFallback(
        a: Pair<String, ByteArray>,
        b: Pair<String, ByteArray>,
        c: Pair<String, ByteArray>,
    ): ColmapTriple? {
        resolveColmapTriple(a, b, c)?.let { return it }
        val list = listOf(a, b, c)
        for ((name, bytes) in list) {
            if (looksLikeColmapAsciiText(bytes)) continue
            val n = normalizeColmapFileHint(name)
            if (n.contains("points3") || n.contains("points_3")) {
                readPoints3DBinary(bytes)?.let { pts ->
                    if (pts.count > 0) {
                        return ColmapTriple(emptyMap(), emptyList(), pts)
                    }
                }
            }
        }
        for ((_, bytes) in list) {
            if (looksLikeColmapAsciiText(bytes)) continue
            readPoints3DBinary(bytes)?.let { pts ->
                if (pts.count > 0) {
                    return ColmapTriple(emptyMap(), emptyList(), pts)
                }
            }
        }
        return null
    }

    /** COLMAP 텍스트 내보내기(# 로 시작)인지 감지 — `.bin` 확장자여도 내용이 텍스트면 바이너리 파서가 실패함 */
    fun looksLikeColmapAsciiText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        var i = 0
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            i = 3
        }
        while (i < bytes.size) {
            val code = bytes[i].toInt() and 0xFF
            if (code != 0x09 && code != 0x0A && code != 0x0D && code != 0x20) break
            i++
        }
        if (i >= bytes.size) return false
        return bytes[i] == '#'.code.toByte()
    }

    /**
     * 전체 파싱 실패 시 사용자에게 보여 줄 원인 요약(한국어).
     */
    fun diagnoseColmapTripleFailure(
        a: Pair<String, ByteArray>,
        b: Pair<String, ByteArray>,
        c: Pair<String, ByteArray>,
    ): String {
        val parts = listOf(a, b, c)
        val emptyName = parts.firstOrNull { it.second.isEmpty() }?.first
        if (emptyName != null) return "파일이 비어 있습니다: $emptyName"
        val ascii = parts.filter { looksLikeColmapAsciiText(it.second) }
        if (ascii.isNotEmpty()) {
            return "선택한 파일이 COLMAP 바이너리가 아니라 텍스트(.txt)로 보입니다 (${ascii.first().first}). " +
                "COLMAP sparse 폴더의 cameras.bin, images.bin, points3D.bin 바이너리를 내보낸 뒤 다시 선택해 주세요."
        }

        val roles = assignByFileNameRoles(parts)
            ?: return "파일 이름에 cameras, images, points3D(또는 points3)가 각각 들어가야 합니다. " +
                "예: cameras.bin, images.bin, points3D.bin"

        val pts = readPoints3DBinary(roles.pts)
        if (pts == null || pts.count <= 0) {
            return "[points3D] (${roles.ptsName})를 COLMAP 바이너리로 읽지 못했거나 점이 0개입니다. " +
                "텍스트 내보내기(.txt)를 .bin으로 바꿔 저장하지 않았는지 확인하세요."
        }

        val camOk = readCamerasBinary(roles.cam) != null
        val imgOk = readImagesBinary(roles.img) != null
        return buildString {
            append("points3D만으로도 표시 가능해야 하는데 복구에 실패했습니다. ")
            if (!camOk) append("[cameras:${roles.camName} 파싱 실패] ")
            if (!imgOk) append("[images:${roles.imgName} 파싱 실패] ")
            append("앱을 최신 빌드로 업데이트했는지 확인해 주세요.")
        }
    }

    private data class RoleTriple(
        val cam: ByteArray,
        val img: ByteArray,
        val pts: ByteArray,
        val camName: String,
        val imgName: String,
        val ptsName: String,
    )

    /** 파일명으로 세 덩어리를 역할에 매핑 (한 번에 하나씩만) */
    private fun assignByFileNameRoles(
        parts: List<Pair<String, ByteArray>>,
    ): RoleTriple? {
        var cam: Pair<String, ByteArray>? = null
        var img: Pair<String, ByteArray>? = null
        var pts: Pair<String, ByteArray>? = null
        for (pair in parts) {
            val n = normalizeColmapFileHint(pair.first)
            when {
                n.contains("points3") || n.contains("points_3") -> {
                    if (pts != null) return null
                    pts = pair
                }
                n.contains("cameras") -> {
                    if (cam != null) return null
                    cam = pair
                }
                n.contains("images") && !n.contains("point") -> {
                    if (img != null) return null
                    img = pair
                }
            }
        }
        if (cam == null || img == null || pts == null) return null
        return RoleTriple(
            cam.second, img.second, pts.second,
            cam.first, img.first, pts.first,
        )
    }

    /** 파일명(또는 경로 마지막 세그먼트)으로 cameras / images / points3D 식별 */
    fun assignByFileName(
        triple: Triple<Pair<String, ByteArray>, Pair<String, ByteArray>, Pair<String, ByteArray>>,
    ): ColmapTriple? {
        val list = listOf(triple.first, triple.second, triple.third)
        var cam: ByteArray? = null
        var img: ByteArray? = null
        var pts: ByteArray? = null
        for ((name, bytes) in list) {
            val n = normalizeColmapFileHint(name)
            when {
                n.contains("points3") || n.contains("points_3") -> pts = bytes
                n.contains("cameras") -> cam = bytes
                n.contains("images") && !n.contains("point") -> img = bytes
            }
        }
        if (cam == null || img == null || pts == null) return null
        return parseTriple(cam, img, pts)
    }

    /** SAF 등에서 온 경로/표시명 → 소문자 파일 힌트 (확장자 생략 허용) */
    private fun normalizeColmapFileHint(name: String): String {
        return name.lowercase()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
    }

    fun parseTriple(camBytes: ByteArray, imgBytes: ByteArray, ptsBytes: ByteArray): ColmapTriple? {
        val cameras = readCamerasBinary(camBytes) ?: return null
        val images = readImagesBinary(imgBytes) ?: return null
        val points = readPoints3DBinary(ptsBytes) ?: return null
        return ColmapTriple(cameras, images, points)
    }

    /**
     * COLMAP `camera_models.h` 기준 num_params (= focal + principal + extra).
     * 모델 ID 0–15 (COLMAP 4.x `CameraModelId` 열거와 동일).
     */
    private val cameraModelNumParams = intArrayOf(
        3,  // 0 SIMPLE_PINHOLE
        4,  // 1 PINHOLE
        4,  // 2 SIMPLE_RADIAL
        5,  // 3 RADIAL
        8,  // 4 OPENCV
        8,  // 5 OPENCV_FISHEYE
        12, // 6 FULL_OPENCV
        5,  // 7 FOV
        4,  // 8 SIMPLE_RADIAL_FISHEYE
        5,  // 9 RADIAL_FISHEYE
        12, // 10 THIN_PRISM_FISHEYE
        16, // 11 RAD_TAN_THIN_PRISM_FISHEYE
        4,  // 12 SIMPLE_DIVISION
        5,  // 13 DIVISION
        3,  // 14 SIMPLE_FISHEYE
        4,  // 15 FISHEYE
    )

    private fun readCamerasBinary(bytes: ByteArray): Map<Int, CameraBin>? {
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 8) return null
            val num = buf.long.toInt()
            if (num <= 0 || num > 100_000) return null
            val cams = HashMap<Int, CameraBin>(num)
            repeat(num) {
                if (buf.remaining() < 24) return null
                val cameraId = buf.int
                val modelId = buf.int
                val width = buf.long
                val height = buf.long
                if (modelId !in cameraModelNumParams.indices) return null
                val np = cameraModelNumParams[modelId]
                if (buf.remaining() < np * 8) return null
                val params = DoubleArray(np) { buf.double }
                cams[cameraId] = CameraBin(cameraId, modelId, width, height, params)
            }
            cams
        } catch (_: Exception) {
            null
        }
    }

    /**
     * images.bin — [read_extrinsics_binary] in colmap_loader.py
     * 각 이미지: 64바이트 헤더 + null 종료 파일명 + point2D 트랙
     */
    private fun readImagesBinary(bytes: ByteArray): List<ImageBin>? {
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 8) return null
            val numReg = buf.long
            if (numReg <= 0 || numReg > 1_000_000) return null
            val out = ArrayList<ImageBin>(numReg.toInt())
            repeat(numReg.toInt()) {
                if (buf.remaining() < 64) return null
                // COLMAP: "idddddddi" = int32 image_id, 4 doubles qvec, 3 doubles tvec, int32 camera_id
                val imageId = buf.int.toLong()
                val qw = buf.double
                val qx = buf.double
                val qy = buf.double
                val qz = buf.double
                val tx = buf.double
                val ty = buf.double
                val tz = buf.double
                val cameraId = buf.int
                val qvec = doubleArrayOf(qw, qx, qy, qz)
                val tvec = doubleArrayOf(tx, ty, tz)
                // null-terminated name (COLMAP: UTF-8, 이론상 길이 제한 없음 — 무결 종료 없으면 파손으로 간주)
                val nameSb = StringBuilder()
                var nameBytes = 0
                while (buf.hasRemaining()) {
                    val b = buf.get().toInt() and 0xFF
                    if (b == 0) break
                    nameBytes++
                    if (nameBytes > 32_768) return null
                    nameSb.append(b.toChar())
                }
                val name = nameSb.toString()
                if (buf.remaining() < 8) return null
                val numP2d = buf.long
                if (numP2d < 0L || numP2d > 50_000_000L) return null
                val skipBytes = numP2d * 24L
                if (skipBytes > buf.remaining()) return null
                buf.position(buf.position() + skipBytes.toInt())
                out.add(ImageBin(imageId, qvec, tvec, cameraId, name))
            }
            out
        } catch (_: Exception) {
            null
        }
    }

    /** points3D.bin */
    private fun readPoints3DBinary(bytes: ByteArray): Points3dBin? {
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 8) return null
            val numPoints = buf.long
            if (numPoints <= 0 || numPoints > 50_000_000) return null
            val n = numPoints.toInt()
            val xyz = FloatArray(n * 3)
            val rgb = FloatArray(n * 3)
            val reprojErr = FloatArray(n)
            val trackLenArr = IntArray(n)
            var pi = 0
            repeat(n) {
                if (buf.remaining() < 43) return null
                buf.long // point id
                val x = buf.double.toFloat()
                val y = buf.double.toFloat()
                val z = buf.double.toFloat()
                val r = (buf.get().toInt() and 0xFF) / 255f
                val g = (buf.get().toInt() and 0xFF) / 255f
                val b = (buf.get().toInt() and 0xFF) / 255f
                val err = buf.double
                reprojErr[pi] = err.toFloat().coerceAtLeast(0f)
                val trackLen = buf.long
                if (trackLen < 0L || trackLen > 50_000_000L) return null
                // COLMAP: 각 트랙 원소는 (IMAGE_ID int32, POINT2D_IDX int32) = 8바이트
                val trackSkip = trackLen * 8L
                if (trackSkip > buf.remaining() || trackSkip > Int.MAX_VALUE - 16) return null
                buf.position(buf.position() + trackSkip.toInt())
                trackLenArr[pi] = minOf(trackLen, Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(0)
                xyz[pi * 3] = x
                xyz[pi * 3 + 1] = y
                xyz[pi * 3 + 2] = z
                rgb[pi * 3] = r
                rgb[pi * 3 + 1] = g
                rgb[pi * 3 + 2] = b
                pi++
            }
            Points3dBin(xyz, rgb, reprojErr, trackLenArr, pi)
        } catch (_: Exception) {
            null
        }
    }

    /** COLMAP qvec (w,x,y,z) → 회전 행렬 R (world→camera), row-major 9 floats */
    fun qvecToRotMat(qvec: DoubleArray): FloatArray {
        val w = qvec[0].toFloat()
        val x = qvec[1].toFloat()
        val y = qvec[2].toFloat()
        val z = qvec[3].toFloat()
        return floatArrayOf(
            1f - 2f * (y * y + z * z),
            2f * (x * y - w * z),
            2f * (x * z + w * y),
            2f * (x * y + w * z),
            1f - 2f * (x * x + z * z),
            2f * (y * z - w * x),
            2f * (x * z - w * y),
            2f * (y * z + w * x),
            1f - 2f * (x * x + y * y),
        )
    }

    /** 카메라 중심 (월드): C = -R^T * t  (COLMAP 관례, X_cam = R*X_world + t) */
    fun cameraCenterWorld(R: FloatArray, tvec: DoubleArray): FloatArray {
        val tx = tvec[0].toFloat()
        val ty = tvec[1].toFloat()
        val tz = tvec[2].toFloat()
        val r00 = R[0]; val r01 = R[3]; val r02 = R[6]
        val r10 = R[1]; val r11 = R[4]; val r12 = R[7]
        val r20 = R[2]; val r21 = R[5]; val r22 = R[8]
        val cx = -(r00 * tx + r10 * ty + r20 * tz)
        val cy = -(r01 * tx + r11 * ty + r21 * tz)
        val cz = -(r02 * tx + r12 * ty + r22 * tz)
        return floatArrayOf(cx, cy, cz)
    }

    fun normalize(v: FloatArray): FloatArray {
        val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).coerceAtLeast(1e-8f)
        return floatArrayOf(v[0] / l, v[1] / l, v[2] / l)
    }

    /**
     * PINHOLE: fx, fy, cx, cy — 첫 번째 이미지에 대해 NDC 초점거리 근사 (엔진과 동일 스케일)
     */
    fun focalNdcFromPinhole(fx: Double, fy: Double, width: Int, height: Int): Float {
        val fxn = (fx / (width * 0.5)).toFloat()
        val fyn = (fy / (height * 0.5)).toFloat()
        return (fxn + fyn) * 0.5f
    }
}
