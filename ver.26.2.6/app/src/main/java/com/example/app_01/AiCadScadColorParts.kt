package com.example.app_01

import kotlin.math.max

/**
 * OpenSCAD 소스에서 `color([r,g,b]) { ... }` 또는 `color([r,g,b]) primitive();` 형태를 찾아
 * 부위별로 분리합니다. (중첩 color는 지원하지 않음 — LLM은 형제 블록만 사용)
 */
object AiCadScadColorParts {

    data class ColorPart(val rgb: FloatArray, val bodyScad: String)

    /** `color([r,g,b])` 또는 `color([r,g,b,a])` (OpenSCAD 4채널) */
    private val colorRe = Regex(
        """color\s*\(\s*\[\s*([0-9.]+)\s*,\s*([0-9.]+)\s*,\s*([0-9.]+)(?:\s*,\s*[0-9.]+)?\s*\]\s*\)""",
        RegexOption.IGNORE_CASE
    )

    fun extractFnPrefix(source: String): String? =
        source.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("\$fn") }

    /**
     * 소스에서 전역 변수·`$fn`·`module`·`function` 정의를 추출합니다.
     * 각 color 블록을 **개별 WASM 렌더**할 때 이 정의들을 앞에 붙여야
     * 내부에서 `rounded_box()` 등 사용자 모듈을 참조해도 렌더가 성공합니다.
     */
    fun extractDefinitions(source: String): String {
        val lines = source.lines()
        val sb = StringBuilder()
        var i = 0
        while (i < lines.size) {
            val t = lines[i].trim()
            when {
                t.isEmpty() || t.startsWith("//") || t.startsWith("/*") || t.startsWith("*") -> i++

                // module / function 정의 — 중괄호 균형으로 다중 줄 전체 수집
                t.startsWith("module ", ignoreCase = true) ||
                t.startsWith("function ", ignoreCase = true) -> {
                    var depth = 0
                    do {
                        val l = lines[i]
                        sb.appendLine(l)
                        for (c in l) when (c) { '{' -> depth++; '}' -> depth-- }
                        i++
                    } while (i < lines.size && depth > 0)
                }

                // $fn / $fs / $fa 설정
                t.startsWith("\$fn") || t.startsWith("\$fs") || t.startsWith("\$fa") -> {
                    sb.appendLine(lines[i]); i++
                }

                // 전역 변수 할당: `NAME = value;`
                // color / union / difference / intersection / translate / rotate 등 형상 문은 제외
                t.matches(Regex("""^\w+\s*=\s*[^=].*;\s*$""")) &&
                !t.startsWith("color", ignoreCase = true) &&
                !t.startsWith("union", ignoreCase = true) &&
                !t.startsWith("difference", ignoreCase = true) &&
                !t.startsWith("intersection", ignoreCase = true) &&
                !t.startsWith("translate", ignoreCase = true) &&
                !t.startsWith("rotate", ignoreCase = true) &&
                !t.startsWith("mirror", ignoreCase = true) &&
                !t.startsWith("scale", ignoreCase = true) &&
                !t.startsWith("hull", ignoreCase = true) &&
                !t.startsWith("minkowski", ignoreCase = true) -> {
                    sb.appendLine(lines[i]); i++
                }

                else -> i++
            }
        }
        return sb.toString().trim()
    }

    fun extractColorParts(source: String): List<ColorPart> {
        val out = mutableListOf<ColorPart>()
        var searchStart = 0
        while (true) {
            val m = colorRe.find(source, searchStart) ?: break
            val r0 = m.groupValues[1].toFloat()
            val g0 = m.groupValues[2].toFloat()
            val b0 = m.groupValues[3].toFloat()
            val (r, g, b) = normalizeRgbTriplet(r0, g0, b0)
            val rgb = floatArrayOf(r, g, b)
            var i = m.range.last + 1
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length) break
            val endBody: Int
            val body: String
            if (source[i] == '{') {
                val (inner, endIdx) = extractBalanced(source, i)
                body = inner.trim()
                endBody = endIdx
            } else {
                endBody = findEndOfStatement(source, i)
                body = source.substring(i, endBody).trim().trimEnd(';').trim()
            }
            if (body.isNotBlank()) {
                out.add(ColorPart(rgb, body))
            }
            searchStart = max(endBody, m.range.last + 1)
        }
        return out
    }

    private fun normalizeRgbTriplet(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        return if (r > 1f || g > 1f || b > 1f) {
            Triple(
                (r / 255f).coerceIn(0f, 1f),
                (g / 255f).coerceIn(0f, 1f),
                (b / 255f).coerceIn(0f, 1f)
            )
        } else {
            Triple(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        }
    }

    private fun extractBalanced(source: String, start: Int): Pair<String, Int> {
        require(source.getOrNull(start) == '{')
        var depth = 0
        var i = start
        while (i < source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return Pair(source.substring(start + 1, i).trim(), i + 1)
                    }
                }
            }
            i++
        }
        return Pair("", start + 1)
    }

    private fun findEndOfStatement(source: String, start: Int): Int {
        var paren = 0
        var brace = 0
        var i = start
        while (i < source.length) {
            when (source[i]) {
                '(' -> paren++
                ')' -> paren = max(0, paren - 1)
                '{' -> brace++
                '}' -> brace = max(0, brace - 1)
                ';' -> if (paren == 0 && brace == 0) return i + 1
            }
            i++
        }
        return source.length
    }

    /**
     * color 블록 하나를 독립 렌더 가능한 SCAD 소스로 포장합니다.
     * [definitions]에는 `$fn`, 전역 변수, `module`/`function` 정의가 모두 포함되어야
     * 블록 내에서 사용자 모듈을 참조해도 WASM 렌더가 성공합니다.
     */
    fun wrapPartForScad(body: String, definitions: String): String {
        val b = body.trim()
        val sb = StringBuilder()
        if (definitions.isNotBlank()) {
            sb.appendLine(definitions)
            sb.appendLine()
        }
        sb.appendLine("union() {")
        sb.appendLine(b)
        sb.appendLine("}")
        return sb.toString()
    }
}
