package com.example.app_01

import kotlin.math.abs
import kotlin.math.max

/**
 * STL 삼각형 정점 후처리 (다이어그램: 메쉬 후처리 — MeshLab/Open3D 대체 경량 단계).
 * 중심을 원점으로 옮기고, 최대 변 길이가 [targetMaxExtent]가 되도록 균일 스케일합니다.
 */
object AiCadMeshPostProcessor {

    fun centerAndNormalizeExtent(positions: FloatArray, targetMaxExtent: Float = 2f): FloatArray {
        if (positions.isEmpty()) return positions
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < positions.size) {
            val x = positions[i]
            val y = positions[i + 1]
            val z = positions[i + 2]
            minX = minOf(minX, x); maxX = maxOf(maxX, x)
            minY = minOf(minY, y); maxY = maxOf(maxY, y)
            minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
            i += 3
        }
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val extent = max(max(maxX - minX, maxY - minY), maxZ - minZ)
        val scale = if (extent > 1e-6f) targetMaxExtent / extent else 1f
        val out = FloatArray(positions.size)
        i = 0
        while (i < positions.size) {
            out[i] = (positions[i] - cx) * scale
            out[i + 1] = (positions[i + 1] - cy) * scale
            out[i + 2] = (positions[i + 2] - cz) * scale
            i += 3
        }
        return out
    }

    /** 변화가 미미하면 원본 유지 */
    fun shouldApply(positions: FloatArray): Boolean {
        if (positions.size < 9) return false
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < positions.size) {
            val x = positions[i]
            minX = minOf(minX, x); maxX = maxOf(maxX, x)
            i += 3
        }
        return abs(maxX - minX) > 1e-3f
    }
}
