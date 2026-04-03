package com.example.app_01

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 앱이 저장하는 ASCII PLY(삼각형 수프 + 정점 uchar RGB) 읽기/쓰기.
 */
fun writeColoredTriangleSoupPly(
    positions: FloatArray,
    colors: FloatArray,
    vertexCount: Int,
    file: File
) {
    require(vertexCount * 3 == positions.size && colors.size == positions.size)
    val faceCount = vertexCount / 3
    val sb = StringBuilder(256 + vertexCount * 48 + faceCount * 16)
    sb.append("ply\nformat ascii 1.0\ncomment cad mesh colored\n")
    sb.append("element vertex ").append(vertexCount).append('\n')
    sb.append("property float x\nproperty float y\nproperty float z\n")
    sb.append("property uchar red\nproperty uchar green\nproperty uchar blue\n")
    sb.append("element face ").append(faceCount).append('\n')
    sb.append("property list uchar int vertex_indices\nend_header\n")
    for (i in 0 until vertexCount) {
        val x = positions[i * 3].toDouble()
        val y = positions[i * 3 + 1].toDouble()
        val z = positions[i * 3 + 2].toDouble()
        val r = (colors[i * 3] * 255f).toInt().coerceIn(0, 255)
        val g = (colors[i * 3 + 1] * 255f).toInt().coerceIn(0, 255)
        val b = (colors[i * 3 + 2] * 255f).toInt().coerceIn(0, 255)
        sb.append(x).append(' ').append(y).append(' ').append(z).append(' ')
            .append(r).append(' ').append(g).append(' ').append(b).append('\n')
    }
    var idx = 0
    repeat(faceCount) {
        sb.append("3 ").append(idx).append(' ').append(idx + 1).append(' ').append(idx + 2).append('\n')
        idx += 3
    }
    file.writeText(sb.toString(), StandardCharsets.UTF_8)
}

/** [writeColoredTriangleSoupPly] 형식 PLY → [ObjParseResult] */
fun loadColoredPlyMesh(file: File): ObjParseResult? {
    if (!file.exists() || file.length() < 20) return null
    return try {
        val text = file.readText(StandardCharsets.UTF_8)
        val lines = text.lines()
        var i = 0
        var vertexCount = 0
        var faceCount = 0
        var inHeader = true
        var hasRgb = false
        while (i < lines.size && inHeader) {
            val line = lines[i].trim()
            when {
                line.startsWith("element vertex") -> vertexCount = line.split(Regex("\\s+")).getOrNull(2)?.toIntOrNull() ?: 0
                line.startsWith("element face") -> faceCount = line.split(Regex("\\s+")).getOrNull(2)?.toIntOrNull() ?: 0
                line.contains("property uchar red") -> hasRgb = true
                line == "end_header" -> inHeader = false
            }
            i++
        }
        if (vertexCount <= 0 || !hasRgb) return null
        val pos = FloatArray(vertexCount * 3)
        val col = FloatArray(vertexCount * 3)
        var v = 0
        while (v < vertexCount && i < lines.size) {
            val parts = lines[i].trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.size >= 6) {
                pos[v * 3] = parts[0].toFloat()
                pos[v * 3 + 1] = parts[1].toFloat()
                pos[v * 3 + 2] = parts[2].toFloat()
                col[v * 3] = (parts[3].toIntOrNull() ?: 0) / 255f
                col[v * 3 + 1] = (parts[4].toIntOrNull() ?: 0) / 255f
                col[v * 3 + 2] = (parts[5].toIntOrNull() ?: 0) / 255f
                v++
            }
            i++
        }
        if (v != vertexCount) return null
        val normalized = normalizeMeshPointsPublic(pos)
        ObjParseResult(normalized, vertexCount, MeshDrawMode.TRIANGLES, col)
    } catch (_: Exception) {
        null
    }
}

/** [normalizeMeshPoints]와 동일 로직 — CadMeshPlyIo에서만 사용 */
private fun normalizeMeshPointsPublic(points: FloatArray): FloatArray {
    val count = points.size / 3
    if (count <= 0) return points
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var maxZ = -Float.MAX_VALUE
    for (i in 0 until count) {
        val x = points[i * 3]
        val y = points[i * 3 + 1]
        val z = points[i * 3 + 2]
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (z < minZ) minZ = z
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        if (z > maxZ) maxZ = z
    }
    val cx = (minX + maxX) / 2f
    val cy = (minY + maxY) / 2f
    val cz = (minZ + maxZ) / 2f
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
