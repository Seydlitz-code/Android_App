package com.example.app_01

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AI CAD 변환·저장 파이프라인 (단일 진입점).
 *
 * ## 설계 원칙 (v2 — 단순화)
 *
 * 기존 "color 파트별 개별 WASM 렌더 → 병합" 방식은 파트 간 geometry 겹침(Z-fighting)과
 * 쐐기(wedge) 형태 왜곡의 원인이었습니다.
 *
 * 새 방식: **단일 WASM 렌더**
 *  1. 전처리 (`AiCadScadPreprocessor`) — LLM 설명 줄 주석 처리 등
 *  2. 괄호 균형 검증 (`AiCadCodeVerifier`)
 *  3. OpenSCAD WASM 렌더 → 바이너리 STL (`OpenScadStlExporter`)
 *  4. STL → GLB 변환 (`StlToGlbConverter`) — 라이브러리 미리보기용
 *  5. STL/GLB/OBJ 저장 (`AiCadLibrary`)
 *
 * color() 블록은 WASM 렌더 전 자동 제거되므로 LLM이 color()를 써도 geometry에 영향 없습니다.
 */
object AiCadPipeline {
    private const val TAG = "AiCadPipeline"

    data class SavedArtifacts(
        val stlFile: File,
        val glbFile: File?,
        val objFile: File?
    )

    // ── 메인 진입점 ─────────────────────────────────────────────────────────

    /**
     * OpenSCAD 소스 → STL/GLB/OBJ 변환 후 AI CAD 라이브러리에 저장합니다.
     *
     * @param preferredName 파일명 접두 (null이면 타임스탬프, [useRandomWhenNameBlank]=true면 랜덤)
     */
    suspend fun exportVerifiedScadToLibrary(
        context: Context,
        scadSource: String,
        preferredName: String? = null,
        useRandomWhenNameBlank: Boolean = false
    ): Result<SavedArtifacts> {

        // ── Step 1: 전처리 ────────────────────────────────────────────────
        // · 마크다운 펜스 제거
        // · LLM 설명 줄(비OpenSCAD) → // 주석 처리
        // · color() 제거 (WASM 렌더 전 필수)
        val prepared = AiCadScadPreprocessor.prepareForRender(scadSource)
        Log.d(TAG, "prepareForRender 완료 (${prepared.length} chars)")

        // ── Step 2: 괄호 균형 검증 ────────────────────────────────────────
        val verified = AiCadCodeVerifier.verify(prepared)
            .getOrElse { return Result.failure(it) }

        // ── Step 3: OpenSCAD WASM 렌더 → 바이너리 STL ────────────────────
        val stlBytes = renderWithRetry(context, verified)
            .getOrElse { return Result.failure(it) }
        Log.i(TAG, "WASM 렌더 완료: ${stlBytes.size} bytes")

        // ── Step 4: STL 유효성 확인 ───────────────────────────────────────
        if (!StlToGlbConverter.hasRenderableTriangles(stlBytes)) {
            return Result.failure(
                IllegalStateException(
                    "빈 메쉬가 생성됐습니다. geometry(union/difference 등)를 확인하세요."
                )
            )
        }
        if (!StlToGlbConverter.hasRenderableMeshExtent(stlBytes)) {
            return Result.failure(
                IllegalStateException(
                    "메쉬 좌표가 한 점에 몰려 있습니다. 치수와 \$fn을 확인하세요."
                )
            )
        }

        // ── Step 5: 저장 ──────────────────────────────────────────────────
        return saveStlAndConvert(context, stlBytes, preferredName, useRandomWhenNameBlank)
    }

    /**
     * 이미 얻은 STL 바이트를 라이브러리에 저장하고 GLB/OBJ를 생성합니다.
     * (외부에서 WASM 렌더 결과를 직접 넘길 때 사용)
     */
    suspend fun saveRenderedStlToLibrary(
        context: Context,
        stlBytes: ByteArray,
        preferredName: String? = null,
        useRandomWhenNameBlank: Boolean = false,
        @Suppress("UNUSED_PARAMETER") glbOverride: ByteArray? = null
    ): Result<SavedArtifacts> {
        if (!StlToGlbConverter.hasRenderableTriangles(stlBytes)) {
            return Result.failure(
                IllegalStateException(
                    "렌더 결과 STL에 표시할 삼각형이 없습니다. union()/geometry를 확인하세요."
                )
            )
        }
        if (!StlToGlbConverter.hasRenderableMeshExtent(stlBytes)) {
            return Result.failure(
                IllegalStateException(
                    "렌더된 메쉬가 한 점으로만 모이거나 유효하지 않은 좌표입니다."
                )
            )
        }
        return saveStlAndConvert(context, stlBytes, preferredName, useRandomWhenNameBlank)
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    private suspend fun renderWithRetry(
        context: Context,
        scadSource: String
    ): Result<ByteArray> {
        var result = OpenScadStlExporter.renderStlBytes(context.applicationContext, scadSource)
        if (result.isFailure) {
            Log.w(TAG, "1차 렌더 실패, 정규화 후 재시도")
            result = OpenScadStlExporter.renderStlBytes(
                context.applicationContext,
                scadSource.replace("\r\n", "\n").trim()
            )
        }
        return result
    }

    private suspend fun saveStlAndConvert(
        context: Context,
        stlBytes: ByteArray,
        preferredName: String?,
        useRandomWhenNameBlank: Boolean
    ): Result<SavedArtifacts> = withContext(Dispatchers.IO) {
        val name = resolveFileName(preferredName, useRandomWhenNameBlank)
        val stlFile = AiCadLibrary.saveStlFile(context, stlBytes, name)
        val base = stlFile.nameWithoutExtension
        val dir = stlFile.parentFile ?: AiCadLibrary.getLibraryDir(context)

        val glbFile = StlToGlbConverter.binaryStlToGlb(stlBytes).fold(
            onSuccess = { glbBytes ->
                File(dir, "$base.glb").also { it.writeBytes(glbBytes) }
            },
            onFailure = { e ->
                Log.w(TAG, "GLB 변환 실패 (무시): ${e.message}")
                null
            }
        )

        val objFile = StlToObjExporter.writeObjFromBinaryStl(
            stlBytes,
            File(dir, "$base.obj")
        ).fold(
            onSuccess = { File(dir, "$base.obj") },
            onFailure = { null }
        )

        Log.i(TAG, "저장 완료: ${stlFile.name} (glb=${glbFile != null}, obj=${objFile != null})")
        Result.success(SavedArtifacts(stlFile = stlFile, glbFile = glbFile, objFile = objFile))
    }

    private fun resolveFileName(
        preferredName: String?,
        useRandomWhenNameBlank: Boolean
    ): String? = when {
        !preferredName.isNullOrBlank() -> preferredName.trim()
        useRandomWhenNameBlank -> buildString {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            repeat(12) { append(chars.random()) }
        }
        else -> null
    }
}
