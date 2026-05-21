package com.example.app_01

import android.util.Log

/**
 * AI CAD: LLM이 만든 OpenSCAD를 WASM 렌더 전에 정리합니다.
 *
 * ## 재설계 원칙 (v2)
 *
 * 기존 "설명 패턴 열거" 방식은 LLM 출력 변형이 생길 때마다 계속 패치가 필요한 구조적 한계가 있었습니다.
 * 새 방식은 **"OpenSCAD 문으로 확신할 수 없으면 주석으로"** 원칙을 따릅니다.
 *
 * ### 핵심 규칙
 * 1. **주석 연속(Comment Continuation)**: 이전 줄이 `//` 주석이고
 *    현재 줄이 새 OpenSCAD 문으로 시작하지 않으면 → `//` 주석으로 처리합니다.
 *    이 규칙 하나로 `// text\n~239mm x 102mm`, `// text\n(wedge)`,
 *    `// text\nhandheld console` 등 대부분의 LLM 오류를 잡습니다.
 * 2. **치수 서술**: `~239mm x 102mm`, `approx 127mm wide` 등 수치·단위 서술 줄.
 * 3. **괄호형 설명**: `(wedge)`, `(lying flat)` 처럼 괄호 안 단어만 있는 줄.
 * 4. **단어 나열**: `handheld console`, `including bezel` 등.
 * 5. **한글 설명**: 한글 비율이 높은 줄.
 */
object AiCadScadPreprocessor {
    private const val TAG = "AiCadScadPreprocessor"

    // ── 공개 진입점 ────────────────────────────────────────────────────────

    /** `color([r,g,b]) { ... }` 추출용 — stripColor 하지 않습니다. */
    fun prepareForColorExtraction(raw: String): String {
        val s1 = OpenCadSanitizer.sanitizeForScad(raw)
        val s2 = mergeBrokenParenLines(s1)
        val s3 = commentDescriptionLines(s2)
        if (s3 != s1) Log.i(TAG, "[colorExtraction] 설명 줄 주석 처리됨")
        return s3.trim()
    }

    /** WASM 렌더 직전 소스 — color() 제거 포함. */
    fun prepareForRender(raw: String): String {
        val s1 = OpenCadSanitizer.sanitizeForScad(raw)
        val s2 = mergeBrokenParenLines(s1)
        val s3 = commentDescriptionLines(s2)
        val s4 = OpenCadSanitizer.stripColorForWasm(s3).trim()
        if (s3 != s1) Log.i(TAG, "[render] 설명 줄 주석 처리됨")
        return s4
    }

    // ── 1단계: 여러 줄로 끊긴 괄호 병합 ──────────────────────────────────

    /**
     * `(lying` 다음 줄이 `flat)` 처럼 짧은 닫는 조각인 경우 한 줄로 합칩니다.
     * OpenSCAD 형상 줄(translate 등)은 `,`가 있어 조건에 걸리지 않습니다.
     */
    private fun mergeBrokenParenLines(scad: String): String {
        val lines = scad.lines().toMutableList()
        var i = 0
        while (i < lines.size - 1) {
            val cur = lines[i].trim()
            if (cur.startsWith("//") || cur.startsWith("/*")) { i++; continue }
            val open = cur.count { it == '(' }
            val close = cur.count { it == ')' }
            if (open > close) {
                val nextTrim = lines[i + 1].trim()
                if (!nextTrim.startsWith("//") && !nextTrim.startsWith("/*") &&
                    nextTrim.length < 80 &&
                    nextTrim.matches(Regex("""^[a-zA-Z0-9\s)]+$"""))
                ) {
                    lines[i] = lines[i].trimEnd() + " " + nextTrim
                    lines.removeAt(i + 1)
                    Log.d(TAG, "병합: ${lines[i].trim().take(60)}")
                    continue
                }
            }
            i++
        }
        return lines.joinToString("\n")
    }

    // ── 2단계: 설명 줄 → // 주석 처리 ───────────────────────────────────

    /**
     * 각 줄을 순회하며 OpenSCAD 문이 아닌 설명 줄을 `//` 주석으로 바꿉니다.
     *
     * [commentOrphanDescriptionLines]는 하위 호환을 위해 같은 함수를 가리킵니다.
     */
    fun commentDescriptionLines(scad: String): String {
        val lines = scad.lines()
        val out = ArrayList<String>(lines.size)
        // 이전 **비어 있지 않은** 줄이 주석이었는지 추적합니다.
        // 빈 줄은 상태를 리셋하지 않습니다(LLM이 설명 블록 사이에 빈 줄을 넣기도 함).
        var prevWasComment = false

        for (line in lines) {
            val t = line.trim()

            if (t.isEmpty()) {
                out.add(line)
                continue
            }

            if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")) {
                out.add(line)
                prevWasComment = true
                continue
            }

            // ── Rule 1: 주석 연속 ──────────────────────────────────────────
            // 이전 줄이 주석이고, 현재 줄이 새 OpenSCAD 문이 아니면 주석 연속입니다.
            // `~239mm x 102mm x 13.9mm`, `(wedge)`, `handheld console` 등을 포함한
            // 모든 LLM 설명 연속을 이 규칙 하나로 처리합니다.
            if (prevWasComment && !looksLikeNewOpenScadStatement(t)) {
                out.add(toComment(line, t))
                prevWasComment = true   // 연쇄 적용
                continue
            }

            // 이전 줄과 무관하게 치수 서술로 보이는 줄입니다.
            if (looksLikeDimensionProse(t)) {
                out.add(toComment(line, t))
                prevWasComment = true
                continue
            }

            // ── Rule 3: 괄호 안 단어 설명 — `(wedge)`, `(lying flat)` ──────
            if (looksLikeParentheticalProse(t)) {
                out.add(toComment(line, t))
                prevWasComment = true
                continue
            }

            // ── Rule 4: 한글 설명 줄 ──────────────────────────────────────
            if (looksLikeHangulDescription(t)) {
                out.add(toComment(line, t))
                prevWasComment = true
                continue
            }

            if (looksLikeEnglishProseLine(t)) {
                out.add(toComment(line, t))
                prevWasComment = true
                continue
            }

            out.add(line)
            prevWasComment = false
        }
        return out.joinToString("\n")
    }

    /** 하위 호환 별칭 */
    fun commentOrphanDescriptionLines(scad: String): String = commentDescriptionLines(scad)

    // ── 판별 함수들 ────────────────────────────────────────────────────────

    /**
     * 이 줄이 새 OpenSCAD 문(모듈 정의·변수 할당·형상 호출 등)의 시작인지 판별합니다.
     * true이면 Rule 1(주석 연속)을 적용하지 않습니다.
     *
     * 의도: "OpenSCAD라고 확신할 수 있는 것만 허용". 나머지는 주석으로.
     */
    private fun looksLikeNewOpenScadStatement(t: String): Boolean {
        for (c in t) {
            when (c) {
                '=', ';', '{', '[', ']', '}' -> return true
            }
        }
        if (t.startsWith("\$")) return true

        // ③ 줄 시작이 식별자 호출 — `cube(`, `translate([` 등. `(see diagram)` 같은 설명 줄은 false
        if (Regex("""(?i)^\$?[a-z_][\w$]*\s*\(""").find(t)?.range?.first == 0) return true

        // ④ OpenSCAD 키워드로 시작하는 문 (괄호 없이 단독으로 오는 `union`, `difference` 등 허용)
        val lower = t.lowercase()
        val keywords = listOf(
            "module ", "function ", "union", "difference", "intersection",
            "hull", "minkowski", "for ", "for(", "if ", "if(", "else",
            "let ", "each ", "cube", "cylinder", "sphere", "polyhedron",
            "linear_extrude", "rotate_extrude", "projection", "import",
            "translate", "rotate", "scale", "mirror", "multmatrix", "offset",
            "color", "echo", "assert", "include ", "use "
        )
        return keywords.any { lower.startsWith(it) }
    }

    /**
     * 치수 서술 줄:
     * - `~239mm x 102mm x 13.9mm`  (tilde 접두, mm 단위, x·× 구분)
     * - `approx 127mm wide, 118mm tall`
     * - `301 x 182 x 77` 처럼 단위 없는 치수 나열
     */
    private fun looksLikeDimensionProse(t: String): Boolean {
        if (t.startsWith("//")) return false
        if (looksLikeNewOpenScadStatement(t)) return false

        // ~N.Nmm x N.Nmm … 또는 ≈N 형태 (tilde, approx 기호)
        if (Regex("""(?i)[~≈]?\s*\d+(\.\d+)?\s*mm\s*[x×]\s*\d+""").containsMatchIn(t)) return true

        if (Regex("""(?i)\(?\s*approx\s*\)?\s*:?\s*""").containsMatchIn(t) &&
            !t.contains('=')) return true

        if (Regex("""(?i)\d+(\.\d+)?\s*[x×]\s*\d+(\.\d+)?\s*[x×]\s*\d+(\.\d+)?""")
                .containsMatchIn(t) && !t.contains('=') && !t.contains(';')) return true

        if (Regex("""(?i)\bmm\b""").containsMatchIn(t) &&
            Regex("""(?i)\b(wide|width|tall|height|thick|thickness|length|depth|diameter|radius)\b""")
                .containsMatchIn(t)) return true

        return false
    }

    /**
     * 괄호 안에 인자 구분(`,` `=` `[`)이 없고 영어 단어만 있으면 설명으로 간주합니다.
     * `(wedge)`, `(lying flat)`, `(PS5 Slim)` 등을 잡습니다.
     */
    private fun looksLikeParentheticalProse(t: String): Boolean {
        if (!t.contains('(') || !t.contains(')')) return false
        val open = t.indexOf('(')
        val close = t.lastIndexOf(')')
        if (open < 0 || close <= open) return false
        val inside = t.substring(open + 1, close).trim()
        if (inside.isEmpty()) return false
        if (',' in inside || '=' in inside || '[' in inside) return false
        val tokens = inside.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        if (tokens.all { it.matches(Regex("""^[+-]?[0-9.]+$""")) }) return false
        val prefix = t.substring(0, open).trim()
        if (prefix.isEmpty() || prefix.all { it.isLetter() || it == '_' || it.isWhitespace() }) {
            if (tokens.all { it.matches(Regex("""^[a-zA-Z][a-zA-Z0-9_'-]*$""")) }) return true
        }
        return false
    }

    private fun looksLikeHangulDescription(t: String): Boolean {
        var hangul = 0
        var other = 0
        for (ch in t) {
            if (ch in '\uAC00'..'\uD7A3') hangul++
            else if (ch.isLetter()) other++
        }
        return hangul > 0 && hangul >= other
    }

    /** `including bezel`, `handheld console` 등 영어 단어 2개 이상·구조 문자 없음 */
    private fun looksLikeEnglishProseLine(t: String): Boolean {
        if (t.any { it in '\uAC00'..'\uD7A3' }) return false
        val parts = t.removeSuffix(";").trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return false
        return parts.all { it.matches(Regex("""^[a-zA-Z][a-zA-Z0-9_'-]*$""")) }
    }

    private fun toComment(originalLine: String, trimmed: String): String {
        val idx = originalLine.indexOf(trimmed)
        val indent = if (idx > 0) originalLine.substring(0, idx) else ""
        return "$indent// $trimmed"
    }
}
