package com.example.app_01

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** [writeColoredTriangleSoupPly] 형식 PLY → [ObjParseResult]
 *  전체 파일을 String으로 올리지 않고 [BufferedReader]로 줄별 스트리밍해 RAM 피크를 낮춥니다. */
fun loadColoredPlyMesh(file: File): ObjParseResult? {
    if (!file.exists() || file.length() < 20) return null
    return try {
        BufferedReader(InputStreamReader(file.inputStream(), StandardCharsets.UTF_8)).use { reader ->
            var vertexCount = 0
            var hasRgb = false

            while (true) {
                val raw = reader.readLine() ?: return null
                val line = raw.trim()
                when {
                    line.startsWith("element vertex") ->
                        vertexCount = line.split(Regex("\\s+")).getOrNull(2)?.toIntOrNull() ?: 0
                    line.contains("property uchar red") -> hasRgb = true
                    line == "end_header" -> break
                }
            }
            if (vertexCount <= 0 || !hasRgb) return null

            val pos = FloatArray(vertexCount * 3)
            val col = FloatArray(vertexCount * 3)
            var v = 0
            while (v < vertexCount) {
                val raw = reader.readLine() ?: break
                val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                if (parts.size >= 6) {
                    pos[v * 3]     = parts[0].toFloat()
                    pos[v * 3 + 1] = parts[1].toFloat()
                    pos[v * 3 + 2] = parts[2].toFloat()
                    col[v * 3]     = (parts[3].toIntOrNull() ?: 0) / 255f
                    col[v * 3 + 1] = (parts[4].toIntOrNull() ?: 0) / 255f
                    col[v * 3 + 2] = (parts[5].toIntOrNull() ?: 0) / 255f
                    v++
                }
            }
            if (v != vertexCount) return null
            val normalized = normalizePlyMeshPoints(pos)
            ObjParseResult(normalized, vertexCount, MeshDrawMode.TRIANGLES, col)
        }
    } catch (_: Exception) {
        null
    }
}

private fun normalizePlyMeshPoints(points: FloatArray): FloatArray {
    val count = points.size / 3
    if (count <= 0) return points
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
    for (i in 0 until count) {
        val x = points[i * 3]; val y = points[i * 3 + 1]; val z = points[i * 3 + 2]
        if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z
        if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z
    }
    val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f; val cz = (minZ + maxZ) / 2f
    val maxDim = kotlin.math.max(maxX - minX, kotlin.math.max(maxY - minY, maxZ - minZ))
    val half = if (maxDim > 0f) maxDim / 2f else 1f
    val result = FloatArray(points.size)
    for (i in 0 until count) {
        result[i * 3] = (points[i * 3] - cx) / half
        result[i * 3 + 1] = (points[i * 3 + 1] - cy) / half
        result[i * 3 + 2] = (points[i * 3 + 2] - cz) / half
    }
    return result
}
