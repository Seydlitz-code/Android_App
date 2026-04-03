package com.example.app_01



import android.content.Context



/**

 * AI CAD 저장: **항상 기기 내** OpenSCAD(WASM)로 렌더링해 라이브러리에 저장합니다.

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

