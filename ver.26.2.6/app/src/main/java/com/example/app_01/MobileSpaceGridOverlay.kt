package com.example.app_01

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * 월드-락 교차 마름모 타일링 오버레이 (틈 없는 버전).
 *
 * ── 틈이 생기던 원인 ─────────────────────────────────────────────────
 *   halfH(다이아몬드 반높이)를 화면 픽셀로 구한 뒤,
 *   행 반복은 PITCH_BIN(3°) 단위로 하면 실제 행 간격이 halfH의 N배가 됨.
 *
 * ── 수정 원칙 ────────────────────────────────────────────────────────
 *   pitchStepDeg = halfH / pxPerPitch
 *   → 행 간격을 정확히 halfH 픽셀에 맞춤 (꼭짓점이 항상 맞닿음)
 *   azStepDeg    = 2·halfW / pxPerAz
 *   → 열 간격도 정확히 2·halfW 픽셀에 맞춤
 *
 * ── 타일 크기 ────────────────────────────────────────────────────────
 *   halfH = min(세로 목표, 가로 한계) — 세로만 맞추면 halfW=2·halfH 가
 *   세로형 화면에서 cw 를 넘어 천장/바닥만 그리드가 차지하는 현상 발생.
 *   halfW = halfH × 2  (가로:세로 = 2:1)
 *
 * ── 시야 밖·팬 시 빈 화면 방지 ───────────────────────────────────────
 *   시야(FOV)보다 각도 반복 범위를 넓히고, 화면 교차 판정에 여유 픽셀을 둔다.
 */
@Composable
fun MobileSpaceScanOverlay(
    headingDeg: Float,
    pitchDeg: Float,
    coverage: MobileSpaceScanCoverage,
    revisionTick: Int,
    modifier: Modifier = Modifier
) {
    val P_MIN = MobileSpaceScanCoverage.PITCH_MIN_DEG
    val P_MAX = MobileSpaceScanCoverage.PITCH_MAX_DEG
    val FOV_H = MobileSpaceScanCoverage.FOV_H
    val FOV_V = MobileSpaceScanCoverage.FOV_V

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // 커버리지 갱신 시 캔버스 무효화 보장 (mutable 객체만 바뀌는 경우)
            @Suppress("UNUSED_VARIABLE") val _invalidate = revisionTick

            val cw = size.width
            val ch = size.height
            if (cw < 2f || ch < 2f) return@Canvas

            val pxPerAz    = cw / FOV_H
            val pxPerPitch = ch / FOV_V

            // 세로 기준 크기 + 가로 한계(마름모 전폭 4·halfH ≤ 화면의 ~88%)
            val ROWS_TARGET   = 24f
            val halfHByHeight = ch / ROWS_TARGET
            val halfHByWidth  = (cw * 0.88f) / 4f
            val halfH         = minOf(halfHByHeight, halfHByWidth)
            val halfW         = halfH * 2f

            val pitchStepDeg = halfH / pxPerPitch
            val azStepDeg    = halfW * 2f / pxPerAz
            val staggerDeg   = halfW / pxPerAz

            val pitchMargin = pitchStepDeg * 5f
            // 시야(±FOV/2)를 피치 각도로 직접 잡는다. P_MIN/P_MAX 로 여기서 자르면 안 된다.
            // 천장을 볼 때 pitchDeg 가 P_MIN(5°) 근처·이하인데도 start 를 5°로 막아버리면
            // pitchC < pitchDeg 인 행(= 화면 위쪽)이 한 줄도 생성되지 않아 그리드가 화면 하단에만 깔린다.
            val pitchLo = pitchDeg - FOV_V * 0.5f - pitchMargin
            val pitchHi = pitchDeg + FOV_V * 0.5f + pitchMargin

            val firstRowIdx = floor((pitchLo - P_MIN) / pitchStepDeg).toInt()
            var pitchC      = P_MIN + (firstRowIdx + 0.5f) * pitchStepDeg
            var rowIdx      = firstRowIdx

            val azMargin   = azStepDeg * 5f
            val azVisLeft  = headingDeg - FOV_H * 0.5f - azMargin
            val azVisRight = headingDeg + FOV_H * 0.5f + azMargin

            val path = Path()
            val edgeSlack = 2f

            while (pitchC <= pitchHi) {
                val dPitch  = pitchC - pitchDeg
                val screenY = ch * 0.5f + dPitch * pxPerPitch

                if (screenY + halfH > -edgeSlack && screenY - halfH < ch + edgeSlack) {
                    val rowStagger = if (rowIdx % 2 == 0) 0f else staggerDeg

                    val rawFirst = floor((azVisLeft - rowStagger) / azStepDeg) * azStepDeg + rowStagger
                    var azC = rawFirst

                    while (azC <= azVisRight) {
                        var dAz = azC - headingDeg
                        if (dAz > 180f)  dAz -= 360f
                        if (dAz < -180f) dAz += 360f

                        val screenX = cw * 0.5f + dAz * pxPerAz

                        if (screenX + halfW > -edgeSlack && screenX - halfW < cw + edgeSlack) {
                            val azNorm = ((azC % 360f) + 360f) % 360f
                            // 커버리지 맵은 P_MIN~P_MAX 만 저장 — 화면 밖 각도는 가장자리 빈으로 조회
                            val pitchForCoverage = pitchC.coerceIn(P_MIN, P_MAX)
                            val captured = coverage.getCoverageNearby(azNorm, pitchForCoverage) >= 1f

                            path.reset()
                            path.moveTo(screenX,          screenY - halfH)
                            path.lineTo(screenX + halfW,  screenY        )
                            path.lineTo(screenX,          screenY + halfH)
                            path.lineTo(screenX - halfW,  screenY        )
                            path.close()

                            if (!captured) {
                                drawPath(path, color = Color(0xFFFF3D3D), alpha = 0.42f)
                            }
                            drawPath(
                                path,
                                color = if (captured) Color(0xFF69F0AE).copy(alpha = 0.50f)
                                        else          Color.White.copy(alpha = 0.20f),
                                style = Stroke(0.8f)
                            )
                        }
                        azC += azStepDeg
                    }
                }
                pitchC += pitchStepDeg
                rowIdx++
            }
        }

        // ── 진행률 레이블 ───────────────────────────────────────────────
        val SAMPLE = 16
        val progressPct = run {
            var covered = 0
            for (row in 0 until SAMPLE) {
                for (col in 0 until SAMPLE) {
                    val dAz = (col + 0.5f - SAMPLE * 0.5f) * (FOV_H / SAMPLE)
                    val dP  = (row + 0.5f - SAMPLE * 0.5f) * (FOV_V / SAMPLE)
                    if (coverage.getCoverageNearby(headingDeg + dAz, pitchDeg + dP) >= 1f) covered++
                }
            }
            covered * 100 / (SAMPLE * SAMPLE)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                progressPct >= 100 -> Color(0xFF69F0AE)
                                progressPct > 0    -> Color(0xFFFFAB40)
                                else               -> Color(0xFFFF3D3D)
                            }
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "현재 뷰 수집 $progressPct%  ·  빨강=미수집  투명=완료",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
