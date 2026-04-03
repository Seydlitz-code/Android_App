package com.example.app_01

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AI CAD 다이어그램 파이프라인 (단일 진입점):
 * 코드 검증 → OpenSCAD(WASM, 기기 내 WebView) → STL → GLB/OBJ → 로컬 저장.
 * `color([r,g,b])` 블록이 있으면 부위별로 렌더 후 STL 병합·다중 재질 GLB로 저장합니다.
 */
object AiCadPipeline {

    data class SavedArtifacts(
        val stlFile: File,
        val glbFile: File?,
        val objFile: File?
    )

    /**
     * @param preferredName 저장 파일명 접두(정규화됨). null이면 타임스탬프/랜덤 규칙은 [AiCadLibrary]와 동일.
     */
    suspend fun exportVerifiedScadToLibrary(
        context: Context,
        scadSource: String,
        preferredName: String? = null,
        useRandomWhenNameBlank: Boolean = false
    ): Result<SavedArtifacts> {
        val prepared = AiCadScadPreprocessor.prepareForRender(scadSource)
        val verified = AiCadCodeVerifier.verify(prepared).getOrElse { return Result.failure(it) }
        val colorParts = AiCadScadColorParts.extractColorParts(verified)

        if (colorParts.isEmpty()) {
            var stlResult = OpenScadStlExporter.renderStlBytes(context.applicationContext, verified)
            if (stlResult.isFailure) {
                stlResult = OpenScadStlExporter.renderStlBytes(
                    context.applicationContext,
                    verified.replace("\r\n", "\n").trim()
                )
            }
            val stlBytes = stlResult.getOrElse { return Result.failure(it) }
            return saveRenderedStlToLibrary(
                context,
                stlBytes,
                preferredName,
                useRandomWhenNameBlank,
                glbOverride = null
            )
        }

        val fnPrefix = AiCadScadColorParts.extractFnPrefix(verified)
        val pairs = mutableListOf<Pair<ByteArray, FloatArray>>()
        for (part in colorParts) {
            val wrapped = AiCadScadColorParts.wrapPartForScad(part.bodyScad, fnPrefix)
            val sanitized = AiCadScadPreprocessor.prepareForRender(wrapped)
            var stlResult = OpenScadStlExporter.renderStlBytes(context.applicationContext, sanitized)
            if (stlResult.isFailure) {
                stlResult = OpenScadStlExporter.renderStlBytes(
                    context.applicationContext,
                    sanitized.replace("\r\n", "\n").trim()
                )
            }
            val stlBytes = stlResult.getOrElse { return Result.failure(it) }
            pairs.add(stlBytes to part.rgb)
        }

        val mergedStl = try {
            StlToGlbConverter.mergeBinaryStls(pairs.map { it.first })
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val glbBytes = StlToGlbConverter.binaryStlsToColoredGlb(pairs).getOrElse {
            return Result.failure(it)
        }
        return saveRenderedStlToLibrary(
            context,
            mergedStl,
            preferredName,
            useRandomWhenNameBlank,
            glbOverride = glbBytes
        )
    }

    /**
     * 이미 얻은 바이너리 STL을 AI CAD 라이브러리에 저장하고 GLB/OBJ를 생성합니다.
     */
    suspend fun saveRenderedStlToLibrary(
        context: Context,
        stlBytes: ByteArray,
        preferredName: String? = null,
        useRandomWhenNameBlank: Boolean = false,
        glbOverride: ByteArray? = null
    ): Result<SavedArtifacts> = withContext(Dispatchers.IO) {
        if (!StlToGlbConverter.hasRenderableTriangles(stlBytes)) {
            return@withContext Result.failure(
                IllegalStateException(
                    "렌더 결과 STL에 표시할 삼각형이 없습니다. union()/difference()와 변수 할당(이름=값;)을 확인하세요."
                )
            )
        }
        if (!StlToGlbConverter.hasRenderableMeshExtent(stlBytes)) {
            return@withContext Result.failure(
                IllegalStateException(
                    "렌더된 메쉬가 한 점으로만 모이거나 유효하지 않은 좌표입니다. 치수·${'$'}fn·CSG를 확인하세요."
                )
            )
        }
        val stlFile = AiCadLibrary.saveStlFile(context, stlBytes, namePart(preferredName, useRandomWhenNameBlank))
        val base = stlFile.nameWithoutExtension
        val dir = stlFile.parentFile ?: AiCadLibrary.getLibraryDir(context)

        val glbFile = when {
            glbOverride != null && glbOverride.isNotEmpty() ->
                File(dir, "$base.glb").also { it.writeBytes(glbOverride) }
            else -> StlToGlbConverter.binaryStlToGlb(stlBytes).fold(
                onSuccess = { glbBytes ->
                    File(dir, "$base.glb").also { it.writeBytes(glbBytes) }
                },
                onFailure = { null }
            )
        }

        val objFile = StlToObjExporter.writeObjFromBinaryStl(stlBytes, File(dir, "$base.obj")).fold(
            onSuccess = { File(dir, "$base.obj") },
            onFailure = { null }
        )

        Result.success(SavedArtifacts(stlFile = stlFile, glbFile = glbFile, objFile = objFile))
    }

    private fun namePart(preferredName: String?, useRandomWhenNameBlank: Boolean): String? {
        return when {
            !preferredName.isNullOrBlank() -> preferredName.trim()
            useRandomWhenNameBlank -> buildString(12) {
                val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
                repeat(12) { append(chars.random()) }
            }
            else -> null
        }
    }
}
