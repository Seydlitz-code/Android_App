package com.example.app_01

import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * 서버 `quality_report.json` (multipart 키 [quality_json]) 파싱·한국어 요약.
 * 기존 quality_report.txt / quality_report.png 는 더 이상 사용하지 않음.
 */
data class PointCloudQualityMetrics(
    val totalPoints: Long,
    val noiseRatio: Double?,
    val isolatedRatio: Double?,
    val normalDeviationDeg: Double?,
    val densityCv: Double?,
    val groundTiltDeg: Double?,
    val meanDensity: Double?,
)

internal fun parsePointCloudQualityReportJson(file: File): PointCloudQualityMetrics? {
    return try {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        if (!json.has("total_points")) return null
        PointCloudQualityMetrics(
            totalPoints = json.getLong("total_points"),
            noiseRatio = json.optFiniteDouble("noise_ratio"),
            isolatedRatio = json.optFiniteDouble("isolated_ratio"),
            normalDeviationDeg = json.optFiniteDouble("normal_deviation_deg"),
            densityCv = json.optFiniteDouble("density_cv"),
            groundTiltDeg = json.optFiniteDouble("ground_tilt_deg"),
            meanDensity = json.optFiniteDouble("mean_density"),
        )
    } catch (_: Throwable) {
        null
    }
}

private fun JSONObject.optFiniteDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return v.takeUnless { it.isNaN() || it.isInfinite() }
}

internal fun formatPointCloudQualityReportKorean(m: PointCloudQualityMetrics): String = buildString {
    appendLine("포인트 클라우드 품질 평가")
    appendLine()
    appendLine("총 포인트 수: ${m.totalPoints}")
    m.noiseRatio?.let {
        appendLine("노이즈 비율: ${String.format(Locale.US, "%.2f", it * 100.0)}%")
    }
    m.isolatedRatio?.let {
        appendLine("고립 포인트 비율: ${String.format(Locale.US, "%.2f", it * 100.0)}%")
    }
    m.normalDeviationDeg?.let {
        appendLine("표면 법선 편차: ${String.format(Locale.US, "%.2f", it)}°")
    }
    m.densityCv?.let {
        appendLine("밀도 균일도: ${String.format(Locale.US, "%.4f", it)}")
    }
    m.groundTiltDeg?.let {
        appendLine("지면 기울기: ${String.format(Locale.US, "%.4f", it)}°")
    }
    m.meanDensity?.let {
        appendLine("포인트 밀도: ${String.format(Locale.US, "%.2f", it)} 개/5cm")
    }
}.trimEnd()
