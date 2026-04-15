package com.example.app_01

/**
 * 이동식 공간 방향별 촬영 커버리지 추적기.
 *
 * 핵심 원칙:
 *  - 데이터셋을 저장할 때 카메라 중심 주변 RECORD_FOV_H × RECORD_FOV_V 범위만
 *    수집 완료로 표시한다 (전체 시야각이 아닌 좁은 범위).
 *  - 좁은 기록 범위 덕분에 그리드 셀이 빠르게 포화되지 않아,
 *    새 구역으로 이동했을 때 해당 방향에 빨간 그리드가 정상적으로 표시된다.
 *  - 수집 여부는 이진(Boolean): 기록 범위 안에 들어온 방향은 완료.
 *  - 절대 방위(azimuth 0-360°) × 피치(PITCH_MIN ~ PITCH_MAX)로 저장하므로,
 *    같은 물리적 방향으로 돌아오면 이전 수집 상태가 유지된다.
 *  - 새 물리적 구역(방) 진입 시에는 외부에서 reset()을 호출해 전체 초기화.
 */
class MobileSpaceScanCoverage(
    /** 방위 빈 크기(°). 작을수록 세밀하지만 배열 크기 증가 */
    private val azDegPerBin: Float = AZ_DEG_PER_BIN,
    /** 피치 빈 크기(°) */
    private val pitchDegPerBin: Float = PITCH_DEG_PER_BIN,
    private val pitchMinDeg: Float = PITCH_MIN_DEG,
    private val pitchMaxDeg: Float = PITCH_MAX_DEG
) {
    companion object {
        /** 오버레이와 반드시 동일한 값을 사용해야 한다 */
        const val AZ_DEG_PER_BIN    = 3f
        const val PITCH_DEG_PER_BIN = 3f
        const val PITCH_MIN_DEG     = 5f
        const val PITCH_MAX_DEG     = 175f
        /** 실제 카메라 시야각 (오버레이 투영 계산용) */
        const val FOV_H             = 70f
        const val FOV_V             = 70f
        /**
         * recordFov() 호출 시 실제로 기록되는 범위.
         * - 너무 넓으면(70°) 10~20장 만에 전 방향이 포화되어 새 구역의 빨간 그리드가 사라짐.
         * - 너무 좁으면(25°) 화면상 다이아몬드 간격(~18°)보다 작아 한 번 촬영 시
         *   셀 1개만 바뀌어 "데이터가 반영되지 않는" 것처럼 보임.
         * 40°(±20°)로 설정하면 한 번 촬영 시 2~3개의 인접 셀이 동시에 전환되어
         * 자연스러운 커버리지 축적이 가능하다.
         */
        /** 데이터셋 1회당 기록 범위(±절반). 오버레이 셀 간격과 겹치게 잡아 한 번에 여러 셀이 갱신되도록 함 */
        const val RECORD_FOV_H      = 48f
        const val RECORD_FOV_V      = 48f
    }
    private val azBins = (360f / azDegPerBin).toInt()                                 // 120
    private val pitchBins = ((pitchMaxDeg - pitchMinDeg) / pitchDegPerBin).toInt()   // 43

    /** true = 이 방향은 수집 완료 */
    private val captured = BooleanArray(azBins * pitchBins)

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private fun idx(az: Int, p: Int) = az * pitchBins + p

    private fun azBin(azimuthDeg: Float): Int =
        (((azimuthDeg % 360f) + 360f) % 360f / azDegPerBin).toInt().coerceIn(0, azBins - 1)

    private fun pitchBin(pitchDeg: Float): Int =
        ((pitchDeg.coerceIn(pitchMinDeg, pitchMaxDeg) - pitchMinDeg) / pitchDegPerBin)
            .toInt().coerceIn(0, pitchBins - 1)

    // ──────────────────────────────────────────────
    // 공개 API
    // ──────────────────────────────────────────────

    /**
     * 현재 카메라가 바라보는 방향(headingDeg, pitchDeg) 주변 좁은 범위를
     * 수집 완료로 표시한다.
     *
     * 기본값으로 RECORD_FOV_H × RECORD_FOV_V 범위를 기록한다 (상수 참고).
     * 전체 시야(70°)를 한꺼번에 기록하면 방향 맵이 빠르게 포화되므로
     * 중간 크기의 원뿔로 기록한다.
     *
     * @param headingDeg  카메라 방위각 (azimuth, 0-360°)
     * @param pitchDeg    카메라 피치 (수평=90°, 위=<90°, 아래=>90°)
     * @param fovH        기록할 가로 범위(°)
     * @param fovV        기록할 세로 범위(°)
     */
    fun recordFov(
        headingDeg: Float,
        pitchDeg: Float,
        fovH: Float = RECORD_FOV_H,
        fovV: Float = RECORD_FOV_V
    ) {
        val halfH = fovH / 2f
        val halfV = fovV / 2f

        var dAz = -halfH
        while (dAz <= halfH + azDegPerBin * 0.5f) {
            var dP = -halfV
            while (dP <= halfV + pitchDegPerBin * 0.5f) {
                val az = azBin(headingDeg + dAz)
                val p = pitchBin(pitchDeg + dP)
                captured[idx(az, p)] = true
                dP += pitchDegPerBin
            }
            dAz += azDegPerBin
        }
    }

    /**
     * 주어진 방향이 수집 완료인지 반환한다.
     * @return 1.0f = 수집 완료, 0.0f = 미수집
     */
    fun getCoverage(azimuthDeg: Float, pitchDeg: Float): Float {
        val az = azBin(azimuthDeg)
        val p = pitchBin(pitchDeg)
        return if (captured[idx(az, p)]) 1f else 0f
    }

    /**
     * 중심 빈 ± 인접 빈까지 OR로 판정.
     * 오버레이 셀 중심 각도가 3° 빈 경계 근처에 있을 때 기록과 표시가 어긋나는 것을 완화한다.
     */
    fun getCoverageNearby(
        azimuthDeg: Float,
        pitchDeg: Float,
        azNeighborBins: Int = 1,
        pitchNeighborBins: Int = 1
    ): Float {
        val az0 = azBin(azimuthDeg)
        val p0 = pitchBin(pitchDeg)
        for (da in -azNeighborBins..azNeighborBins) {
            for (dp in -pitchNeighborBins..pitchNeighborBins) {
                val az = ((az0 + da) % azBins + azBins) % azBins
                val p = (p0 + dp).coerceIn(0, pitchBins - 1)
                if (captured[idx(az, p)]) return 1f
            }
        }
        return 0f
    }

    /**
     * 세션 초기화 — MOBILE_SPACE 모드 진입 시 호출.
     * (같은 모드 안에서는 계속 축적되므로 돌아온 구역도 기억됨)
     */
    fun reset() = captured.fill(false)

    /**
     * 현재 화면(headingDeg, pitchDeg 기준 FOV 내)의 수집 완료 비율(0.0~1.0).
     * 진행률 표시용.
     */
    fun viewCoverageRatio(
        headingDeg: Float,
        pitchDeg: Float,
        fovH: Float = 70f,
        fovV: Float = 70f,
        gridCols: Int = 40,
        gridRows: Int = 40
    ): Float {
        var covered = 0
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val dAz = (col + 0.5f - gridCols * 0.5f) * (fovH / gridCols)
                val dP = (row + 0.5f - gridRows * 0.5f) * (fovV / gridRows)
                if (getCoverage(headingDeg + dAz, pitchDeg + dP) >= 1f) covered++
            }
        }
        return covered.toFloat() / (gridCols * gridRows)
    }
}
