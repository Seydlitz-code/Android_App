package com.example.app_01

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * 바이너리 STL → glTF 2.0 GLB (Filament/SceneView 미리보기용).
 * 다이어그램: 포맷 변환(.stl → .glb)
 */
object StlToGlbConverter {

    /**
     * 바이너리 STL 헤더(80~83)의 삼각형 개수와 실제 파일 길이가 안 맞는 경우가 있음(OpenSCAD WASM 등).
     * [ObjViewer]와 같이 실제로 읽을 수 있는 개수는 `min(헤더값, (바이트길이-84)/50)`로 제한합니다.
     */
    private fun effectiveBinaryStlTriangleCount(bytes: ByteArray): Int? {
        if (bytes.size < 84) return null
        val triHeader = ByteBuffer.wrap(bytes, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val maxTriBySize = (bytes.size - 84) / 50
        if (maxTriBySize < 1) return null
        return when {
            triHeader in 1 until 50_000_000 -> minOf(triHeader, maxTriBySize)
            else -> maxTriBySize
        }
    }

    /** 라이브러리 저장·미리보기 전: 실제로 그릴 삼각형이 1개 이상인지 */
    fun hasRenderableTriangles(stlBytes: ByteArray): Boolean =
        (effectiveBinaryStlTriangleCount(stlBytes) ?: 0) >= 1

    /**
     * 삼각형 개수만으로는 통과할 수 있는 **퇴화 메쉬**(모든 정점 동일·NaN)를 걸러냅니다.
     * 이런 STL은 저장 성공 토스트 후에도 미리보기가 비어 보일 수 있음.
     */
    fun hasRenderableMeshExtent(stlBytes: ByteArray): Boolean {
        val pos = readBinaryStlTrianglePositions(stlBytes) ?: return false
        if (pos.isEmpty()) return false
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < pos.size) {
            val x = pos[i]
            val y = pos[i + 1]
            val z = pos[i + 2]
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return false
            minX = minOf(minX, x); maxX = maxOf(maxX, x)
            minY = minOf(minY, y); maxY = maxOf(maxY, y)
            minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
            i += 3
        }
        val ex = maxX - minX
        val ey = maxY - minY
        val ez = maxZ - minZ
        return maxOf(ex, ey, ez) > 1e-6f
    }

    /** 여러 바이너리 STL을 하나의 STL로 합칩니다(삼각형 나열). */
    fun mergeBinaryStls(parts: List<ByteArray>): ByteArray {
        require(parts.isNotEmpty())
        if (parts.size == 1) return parts[0]
        var totalTriangles = 0
        val triangleChunks = ArrayList<ByteArray>(parts.size)
        for (p in parts) {
            val eff = effectiveBinaryStlTriangleCount(p)
                ?: throw IllegalArgumentException("STL 헤더가 잘못되었거나 바이너리 STL이 아닙니다.")
            totalTriangles += eff
            triangleChunks.add(p.copyOfRange(84, 84 + eff * 50))
        }
        val header = ByteArray(80) { 0 }
        val out = ByteBuffer.allocate(84 + totalTriangles * 50).order(ByteOrder.LITTLE_ENDIAN)
        out.put(header)
        out.putInt(totalTriangles)
        for (chunk in triangleChunks) {
            out.put(chunk)
        }
        return out.array()
    }

    /**
     * 부위별 STL과 RGB(0~1)로 glTF GLB를 만듭니다. (미리보기용 다중 재질)
     */
    fun binaryStlsToColoredGlb(parts: List<Pair<ByteArray, FloatArray>>): Result<ByteArray> = runCatching {
        require(parts.isNotEmpty())
        val partPositions = parts.map { (stl, _) ->
            extractPositionsFromBinaryStl(stl)
                ?: throw IllegalArgumentException("STL을 읽을 수 없습니다.")
        }
        val totalFloats = partPositions.sumOf { it.size }
        val combined = FloatArray(totalFloats)
        var off = 0
        for (p in partPositions) {
            p.copyInto(combined, off)
            off += p.size
        }
        val normalized = if (AiCadMeshPostProcessor.shouldApply(combined)) {
            AiCadMeshPostProcessor.centerAndNormalizeExtent(combined, 2f)
        } else {
            combined
        }
        val vertexCounts = partPositions.map { it.size / 3 }
        var vBase = 0
        val ranges = vertexCounts.map { vc ->
            val r = vBase until vBase + vc
            vBase += vc
            r
        }
        buildGlbMultiPrimitive(normalized, ranges, parts.map { it.second })
    }

    /**
     * 바이너리 STL 정점을 읽습니다. [OBJ] 변환 등에서 헤더·파일 길이 불일치를 허용합니다.
     */
    fun readBinaryStlTrianglePositions(stlBytes: ByteArray): FloatArray? =
        extractPositionsFromBinaryStl(stlBytes)

    fun binaryStlToGlb(stlBytes: ByteArray): Result<ByteArray> = runCatching {
        val positions = extractPositionsFromBinaryStl(stlBytes)
            ?: throw IllegalArgumentException("STL을 읽을 수 없습니다(바이너리 형식이 아니거나 비어 있습니다).")
        val processed = if (AiCadMeshPostProcessor.shouldApply(positions)) {
            AiCadMeshPostProcessor.centerAndNormalizeExtent(positions, targetMaxExtent = 2f)
        } else {
            positions
        }
        buildGlb(processed)
    }

    private fun extractPositionsFromBinaryStl(bytes: ByteArray): FloatArray? {
        val triCount = effectiveBinaryStlTriangleCount(bytes) ?: return null
        val out = FloatArray(triCount * 9)
        var off = 84
        var w = 0
        repeat(triCount) {
            off += 12 // normal (skip)
            repeat(3) {
                val x = ByteBuffer.wrap(bytes, off, 4).order(ByteOrder.LITTLE_ENDIAN).float
                off += 4
                val y = ByteBuffer.wrap(bytes, off, 4).order(ByteOrder.LITTLE_ENDIAN).float
                off += 4
                val z = ByteBuffer.wrap(bytes, off, 4).order(ByteOrder.LITTLE_ENDIAN).float
                off += 4
                out[w++] = x; out[w++] = y; out[w++] = z
            }
            off += 2 // attribute
        }
        return out
    }

    private fun buildGlb(positions: FloatArray): ByteArray {
        val vertexCount = positions.size / 3
        require(vertexCount > 0 && vertexCount % 3 == 0)

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
            minX = kotlin.math.min(minX, x); maxX = max(maxX, x)
            minY = kotlin.math.min(minY, y); maxY = max(maxY, y)
            minZ = kotlin.math.min(minZ, z); maxZ = max(maxZ, z)
            i += 3
        }

        val binSize = positions.size * 4
        val bb = ByteBuffer.allocate(binSize).order(ByteOrder.LITTLE_ENDIAN)
        i = 0
        while (i < positions.size) {
            bb.putFloat(positions[i])
            bb.putFloat(positions[i + 1])
            bb.putFloat(positions[i + 2])
            i += 3
        }
        val binChunk = bb.array()

        val json = JsonObject()
        json.add("asset", JsonObject().apply { addProperty("version", "2.0") })
        json.addProperty("scene", 0)
        json.add("scenes", JsonArray().apply { add(JsonObject().apply { add("nodes", JsonArray().apply { add(0) }) }) })
        json.add("nodes", JsonArray().apply {
            add(JsonObject().apply { addProperty("mesh", 0) })
        })
        json.add(
            "meshes",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        add(
                            "primitives",
                            JsonArray().apply {
                                add(
                                    JsonObject().apply {
                                        add("attributes", JsonObject().apply { addProperty("POSITION", 0) })
                                        addProperty("mode", 4)
                                        addProperty("material", 0)
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
        json.add(
            "materials",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        add(
                            "pbrMetallicRoughness",
                            JsonObject().apply {
                                add(
                                    "baseColorFactor",
                                    JsonArray().apply {
                                        add(JsonPrimitive(0.75)); add(JsonPrimitive(0.78))
                                        add(JsonPrimitive(0.82)); add(JsonPrimitive(1.0))
                                    }
                                )
                                addProperty("metallicFactor", 0.15)
                                addProperty("roughnessFactor", 0.45)
                            }
                        )
                    }
                )
            }
        )
        json.add(
            "accessors",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        addProperty("bufferView", 0)
                        addProperty("byteOffset", 0)
                        addProperty("componentType", 5126)
                        addProperty("count", vertexCount)
                        addProperty("type", "VEC3")
                        add(
                            "min",
                            JsonArray().apply {
                                add(JsonPrimitive(minX.toDouble()))
                                add(JsonPrimitive(minY.toDouble()))
                                add(JsonPrimitive(minZ.toDouble()))
                            }
                        )
                        add(
                            "max",
                            JsonArray().apply {
                                add(JsonPrimitive(maxX.toDouble()))
                                add(JsonPrimitive(maxY.toDouble()))
                                add(JsonPrimitive(maxZ.toDouble()))
                            }
                        )
                    }
                )
            }
        )
        json.add(
            "bufferViews",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        addProperty("buffer", 0)
                        addProperty("byteOffset", 0)
                        addProperty("byteLength", binSize)
                    }
                )
            }
        )
        json.add(
            "buffers",
            JsonArray().apply {
                add(JsonObject().apply { addProperty("byteLength", binSize) })
            }
        )

        var jsonStr = AiCadNetworkModule.gson.toJson(json)
        val jsonBytes = jsonStr.toByteArray(Charsets.UTF_8)
        val jsonPaddedLen = (jsonBytes.size + 3) and 3.inv()
        val jsonPadding = jsonPaddedLen - jsonBytes.size
        if (jsonPadding > 0) {
            jsonStr += " ".repeat(jsonPadding)
        }
        val jsonChunkData = jsonStr.toByteArray(Charsets.UTF_8)

        val binChunkPaddedLen = (binChunk.size + 3) and 3.inv()
        val binPadding = binChunkPaddedLen - binChunk.size
        val binPadded = binChunk.copyOf(binChunkPaddedLen)
        if (binPadding > 0) {
            binPadded.fill(0, binChunk.size, binChunkPaddedLen)
        }

        val totalLen = 12 + 8 + jsonChunkData.size + 8 + binPadded.size
        val out = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(0x46546C67)
        out.putInt(2)
        out.putInt(totalLen)
        out.putInt(jsonChunkData.size)
        out.putInt(0x4E4F534A)
        out.put(jsonChunkData)
        out.putInt(binPadded.size)
        out.putInt(0x004E4942)
        out.put(binPadded)
        return out.array()
    }

    private fun buildGlbMultiPrimitive(
        positions: FloatArray,
        vertexRanges: List<IntRange>,
        rgbs: List<FloatArray>
    ): ByteArray {
        require(vertexRanges.size == rgbs.size)
        val vertexCount = positions.size / 3
        require(vertexCount > 0 && vertexCount % 3 == 0)

        val binSize = positions.size * 4
        val bb = ByteBuffer.allocate(binSize).order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i < positions.size) {
            bb.putFloat(positions[i])
            bb.putFloat(positions[i + 1])
            bb.putFloat(positions[i + 2])
            i += 3
        }
        val binChunk = bb.array()

        val accessors = JsonArray()
        val materials = JsonArray()
        val primitives = JsonArray()
        var byteOffset = 0
        repeat(vertexRanges.size) { idx ->
            val range = vertexRanges[idx]
            val rgb = rgbs[idx]
            val r = range.first
            val count = range.last - r + 1
            // min/max for this slice
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            var vi = r * 3
            val end = (r + count) * 3
            while (vi < end) {
                val x = positions[vi]
                val y = positions[vi + 1]
                val z = positions[vi + 2]
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
                minZ = min(minZ, z); maxZ = max(maxZ, z)
                vi += 3
            }
            accessors.add(
                JsonObject().apply {
                    addProperty("bufferView", 0)
                    addProperty("byteOffset", byteOffset)
                    addProperty("componentType", 5126)
                    addProperty("count", count)
                    addProperty("type", "VEC3")
                    add(
                        "min",
                        JsonArray().apply {
                            add(JsonPrimitive(minX.toDouble()))
                            add(JsonPrimitive(minY.toDouble()))
                            add(JsonPrimitive(minZ.toDouble()))
                        }
                    )
                    add(
                        "max",
                        JsonArray().apply {
                            add(JsonPrimitive(maxX.toDouble()))
                            add(JsonPrimitive(maxY.toDouble()))
                            add(JsonPrimitive(maxZ.toDouble()))
                        }
                    )
                }
            )
            byteOffset += count * 12
            materials.add(
                JsonObject().apply {
                    add(
                        "pbrMetallicRoughness",
                        JsonObject().apply {
                            add(
                                "baseColorFactor",
                                JsonArray().apply {
                                    add(JsonPrimitive(rgb[0].toDouble()))
                                    add(JsonPrimitive(rgb[1].toDouble()))
                                    add(JsonPrimitive(rgb[2].toDouble()))
                                    add(JsonPrimitive(1.0))
                                }
                            )
                            addProperty("metallicFactor", 0.12)
                            addProperty("roughnessFactor", 0.55)
                        }
                    )
                }
            )
            primitives.add(
                JsonObject().apply {
                    add("attributes", JsonObject().apply { addProperty("POSITION", idx) })
                    addProperty("mode", 4)
                    addProperty("material", idx)
                }
            )
        }

        val json = JsonObject()
        json.add("asset", JsonObject().apply { addProperty("version", "2.0") })
        json.addProperty("scene", 0)
        json.add("scenes", JsonArray().apply { add(JsonObject().apply { add("nodes", JsonArray().apply { add(0) }) }) })
        json.add("nodes", JsonArray().apply {
            add(JsonObject().apply { addProperty("mesh", 0) })
        })
        json.add(
            "meshes",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        add("primitives", primitives)
                    }
                )
            }
        )
        json.add("materials", materials)
        json.add("accessors", accessors)
        json.add(
            "bufferViews",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        addProperty("buffer", 0)
                        addProperty("byteOffset", 0)
                        addProperty("byteLength", binSize)
                    }
                )
            }
        )
        json.add(
            "buffers",
            JsonArray().apply {
                add(JsonObject().apply { addProperty("byteLength", binSize) })
            }
        )

        var jsonStr = AiCadNetworkModule.gson.toJson(json)
        val jsonBytes = jsonStr.toByteArray(Charsets.UTF_8)
        val jsonPaddedLen = (jsonBytes.size + 3) and 3.inv()
        val jsonPadding = jsonPaddedLen - jsonBytes.size
        if (jsonPadding > 0) {
            jsonStr += " ".repeat(jsonPadding)
        }
        val jsonChunkData = jsonStr.toByteArray(Charsets.UTF_8)

        val binChunkPaddedLen = (binChunk.size + 3) and 3.inv()
        val binPadding = binChunkPaddedLen - binChunk.size
        val binPadded = binChunk.copyOf(binChunkPaddedLen)
        if (binPadding > 0) {
            binPadded.fill(0, binChunk.size, binChunkPaddedLen)
        }

        val totalLen = 12 + 8 + jsonChunkData.size + 8 + binPadded.size
        val out = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(0x46546C67)
        out.putInt(2)
        out.putInt(totalLen)
        out.putInt(jsonChunkData.size)
        out.putInt(0x4E4F534A)
        out.put(jsonChunkData)
        out.putInt(binPadded.size)
        out.putInt(0x004E4942)
        out.put(binPadded)
        return out.array()
    }
}
