package com.example.app_01

import android.content.Context

/**
 * AI CAD 저장 진입점.
 * 항상 기기 내 OpenSCAD WASM으로 렌더링한 뒤 라이브러리에 저장합니다.
 *
 * 파이프라인: [AiCadScadPreprocessor] → [AiCadCodeVerifier] →
 *             [OpenScadStlExporter](WASM) → [StlToGlbConverter] → [AiCadLibrary]
 */
object AiCadSaveCoordinator {

    suspend fun exportToLibrary(
        context: Context,
        scadSource: String,
        preferredName: String? = null,
        useRandomWhenNameBlank: Boolean = false
    ): Result<AiCadPipeline.SavedArtifacts> =
        AiCadPipeline.exportVerifiedScadToLibrary(
            context,
            scadSource,
            preferredName,
            useRandomWhenNameBlank
        )
}
