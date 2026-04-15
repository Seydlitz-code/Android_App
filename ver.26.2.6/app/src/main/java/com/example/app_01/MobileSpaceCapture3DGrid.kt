package com.example.app_01

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

/**
 * 이동식 공간 데이터셋: 두 프레임 간 0~1 유사도(64×64 RGB 거리 평균).
 */
fun calculateImageSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
    val width = 64
    val height = 64
    val s1 = Bitmap.createScaledBitmap(bitmap1, width, height, true)
    val s2 = Bitmap.createScaledBitmap(bitmap2, width, height, true)

    val pixels1 = IntArray(width * height)
    val pixels2 = IntArray(width * height)

    s1.getPixels(pixels1, 0, width, 0, 0, width, height)
    s2.getPixels(pixels2, 0, width, 0, 0, width, height)

    var similaritySum = 0.0
    val maxDist = sqrt(255.0 * 255.0 * 3.0)

    for (i in pixels1.indices) {
        val c1 = pixels1[i]
        val c2 = pixels2[i]

        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        val dist = sqrt(
            ((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)).toDouble()
        )
        similaritySum += (1.0 - (dist / maxDist))
    }

    if (s1 != bitmap1) s1.recycle()
    if (s2 != bitmap2) s2.recycle()

    return (similaritySum / (width * height)).toFloat()
}

private fun Bitmap.toSmall64(): Bitmap = Bitmap.createScaledBitmap(this, 64, 64, true)

@Immutable
data class MobileSpaceGridCellSnapshot(
    val ix: Int,
    val iy: Int,
    val iz: Int,
    val sampleCount: Int,
    val isComplete: Boolean
)

@Immutable
data class MobileSpaceGridOverlayState(
    val activeRegionIndex: Int,
    val azimuthBins: Int,
    val pitchBins: Int,
    val rollBins: Int,
    val cells: List<MobileSpaceGridCellSnapshot>,
    /** 바닥 투시 격자 (ix,iy) 셀 — 롤 축 합산 ≥ requiredSamplesPerFloorCell 이면 true (초록 면만) */
    val floorCellComplete: List<Boolean>
)

/**
 * 이동식 공간: **사각형** 방위×피치×롤 3D 격자(실내 이동 촬영에 적합).
 * 구역은 장면 앵커 유사도로 분리·복귀 시 기존 구역 재사용(디스크 기억).
 */
@Stable
class MobileSpaceCaptureSession(
    private val filesDir: File,
    private val azimuthBins: Int = 6,
    private val pitchBins: Int = 4,
    private val rollBins: Int = 3,
    val requiredSamplesPerVoxel: Int = 3,
    /** 바닥 면 시각화: (ix,iy)에 대해 롤 축 합산 샘플 ≥ 이 값이면 초록 */
    private val requiredSamplesPerFloorCell: Int = 3,
    private val rejoinRegionSimilarity: Float = 0.42f,
    private val leaveRegionSimilarity: Float = 0.28f,
    private val leaveStreakRequired: Int = 4
) {
    private class Region(
        val id: Int,
        var anchor: Bitmap,
        val counts: IntArray
    )

    private val regions = ArrayList<Region>()
    var activeRegionIndex: Int = 0
        private set

    /** 현재까지 감지된 구역 수. 새 구역 생성 여부를 외부에서 확인하는 데 사용 */
    val regionCount: Int get() = regions.size

    private var nextRegionId = 0
    private var leaveStreak = 0

    private val storageDir: File = File(filesDir, "mobile_space_session").apply { mkdirs() }
    private val stateFile = File(storageDir, "regions_state.json")

    private fun cellIndex(azimuthDeg: Float, pitchDeg: Float, rollDeg: Float): Triple<Int, Int, Int> {
        val az = ((azimuthDeg % 360f + 360f) % 360f)
        val ix = ((az / 360f) * azimuthBins).toInt().coerceIn(0, azimuthBins - 1)
        val pitchClamped = pitchDeg.coerceIn(45f, 135f)
        val iy = (((pitchClamped - 45f) / 90f) * pitchBins).toInt().coerceIn(0, pitchBins - 1)
        val rollNorm = ((rollDeg + 180f) % 360f + 360f) % 360f
        val iz = ((rollNorm / 360f) * rollBins).toInt().coerceIn(0, rollBins - 1)
        return Triple(ix, iy, iz)
    }

    private fun linear(ix: Int, iy: Int, iz: Int) =
        (iz * pitchBins + iy) * azimuthBins + ix

    fun resetForModeExit() {
        for (r in regions) if (!r.anchor.isRecycled) r.anchor.recycle()
        regions.clear()
        activeRegionIndex = 0
        nextRegionId = 0
        leaveStreak = 0
    }

    fun loadFromDisk() {
        if (!stateFile.exists()) return
        runCatching {
            for (r in regions) if (!r.anchor.isRecycled) r.anchor.recycle()
            regions.clear()
            val text = stateFile.readText()
            val arr = JSONArray(text)
            var maxId = -1
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.getInt("id")
                val anchorFile = File(o.getString("anchorPath"))
                if (!anchorFile.exists()) continue
                val bmp = BitmapFactory.decodeFile(anchorFile.absolutePath) ?: continue
                val countsJson = o.getJSONArray("counts")
                val counts = IntArray(azimuthBins * pitchBins * rollBins)
                val n = countsJson.length().coerceAtMost(counts.size)
                for (k in 0 until n) counts[k] = countsJson.getInt(k)
                regions.add(Region(id, bmp, counts))
                if (id > maxId) maxId = id
            }
            nextRegionId = maxId + 1
            if (regions.isNotEmpty()) activeRegionIndex = 0
        }
    }

    fun persistToDisk() {
        runCatching {
            val arr = JSONArray()
            for (r in regions) {
                val anchorFile = File(storageDir, "region_${r.id}_anchor.jpg")
                FileOutputStream(anchorFile).use { out ->
                    r.anchor.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                val o = JSONObject()
                o.put("id", r.id)
                o.put("anchorPath", anchorFile.absolutePath)
                val cj = JSONArray()
                for (v in r.counts) cj.put(v)
                o.put("counts", cj)
                arr.put(o)
            }
            stateFile.writeText(arr.toString())
        }
    }

    /**
     * 장면 프레임(축소 비트맵). 기존 구역과 매칭되면 활성 구역 전환, 새 공간이면 구역 추가.
     * @return UI 갱신 필요 여부
     */
    fun onSceneFrame(suitableFrame: Bitmap): Boolean {
        val small = suitableFrame.toSmall64()
        try {
            if (regions.isEmpty()) {
                regions.add(
                    Region(
                        nextRegionId++,
                        small.copy(Bitmap.Config.ARGB_8888, false),
                        IntArray(azimuthBins * pitchBins * rollBins)
                    )
                )
                activeRegionIndex = 0
                return true
            }
            var bestIdx = 0
            var bestSim = -1f
            for (i in regions.indices) {
                val sim = calculateImageSimilarity(regions[i].anchor, small)
                if (sim > bestSim) {
                    bestSim = sim
                    bestIdx = i
                }
            }
            if (bestSim >= rejoinRegionSimilarity) {
                leaveStreak = 0
                val changed = bestIdx != activeRegionIndex
                activeRegionIndex = bestIdx
                val alpha = 0.12f
                val reg = regions[activeRegionIndex]
                val oldA = reg.anchor
                val blended = blendAnchors(oldA, small, alpha)
                if (!oldA.isRecycled) oldA.recycle()
                reg.anchor = blended
                return changed
            }

            val simActive = calculateImageSimilarity(regions[activeRegionIndex].anchor, small)
            if (simActive < leaveRegionSimilarity) {
                leaveStreak++
                if (leaveStreak >= leaveStreakRequired) {
                    regions.add(
                        Region(
                            nextRegionId++,
                            small.copy(Bitmap.Config.ARGB_8888, false),
                            IntArray(azimuthBins * pitchBins * rollBins)
                        )
                    )
                    activeRegionIndex = regions.lastIndex
                    leaveStreak = 0
                    return true
                }
            } else {
                leaveStreak = 0
            }
            return false
        } finally {
            if (!small.isRecycled) small.recycle()
        }
    }

    private fun blendAnchors(a: Bitmap, b: Bitmap, t: Float): Bitmap {
        val w = 64
        val h = 64
        val s1 = if (a.width == w && a.height == h) a else a.toSmall64()
        val s2 = if (b.width == w && b.height == h) b else b.toSmall64()
        val p1 = IntArray(w * h)
        val p2 = IntArray(w * h)
        s1.getPixels(p1, 0, w, 0, 0, w, h)
        s2.getPixels(p2, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (i in p1.indices) {
            val c1 = p1[i]
            val c2 = p2[i]
            fun ch(c: Int, sh: Int) = (c shr sh) and 0xFF
            val r = (ch(c1, 16) * (1 - t) + ch(c2, 16) * t).toInt().coerceIn(0, 255)
            val g = (ch(c1, 8) * (1 - t) + ch(c2, 8) * t).toInt().coerceIn(0, 255)
            val bch = (ch(c1, 0) * (1 - t) + ch(c2, 0) * t).toInt().coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bch
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        if (s1 != a && !s1.isRecycled) s1.recycle()
        if (s2 != b && !s2.isRecycled) s2.recycle()
        return bmp
    }

    fun recordAcceptedSample(azimuthDeg: Float, pitchDeg: Float, rollDeg: Float) {
        if (regions.isEmpty()) return
        val (ix, iy, iz) = cellIndex(azimuthDeg, pitchDeg, rollDeg)
        val r = regions.getOrNull(activeRegionIndex) ?: return
        val li = linear(ix, iy, iz)
        r.counts[li] = (r.counts[li] + 1).coerceAtMost(255)
    }

    fun snapshotOverlay(): MobileSpaceGridOverlayState {
        val list = ArrayList<MobileSpaceGridCellSnapshot>(azimuthBins * pitchBins * rollBins)
        val floorOk = BooleanArray(azimuthBins * pitchBins)
        if (regions.isEmpty()) {
            return MobileSpaceGridOverlayState(0, azimuthBins, pitchBins, rollBins, list, floorOk.toList())
        }
        val r = regions[activeRegionIndex]
        for (iz in 0 until rollBins) {
            for (iy in 0 until pitchBins) {
                for (ix in 0 until azimuthBins) {
                    val n = r.counts[linear(ix, iy, iz)]
                    val done = n >= requiredSamplesPerVoxel
                    list.add(MobileSpaceGridCellSnapshot(ix, iy, iz, n, done))
                }
            }
        }
        for (iy in 0 until pitchBins) {
            for (ix in 0 until azimuthBins) {
                var sum = 0
                for (iz in 0 until rollBins) {
                    sum += r.counts[linear(ix, iy, iz)]
                }
                floorOk[iy * azimuthBins + ix] = sum >= requiredSamplesPerFloorCell
            }
        }
        return MobileSpaceGridOverlayState(
            activeRegionIndex = activeRegionIndex,
            azimuthBins = azimuthBins,
            pitchBins = pitchBins,
            rollBins = rollBins,
            cells = list,
            floorCellComplete = floorOk.toList()
        )
    }
}
