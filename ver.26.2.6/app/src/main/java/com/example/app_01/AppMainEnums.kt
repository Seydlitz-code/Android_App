package com.example.app_01

enum class CaptureMode {
    PHOTO,
    /** 5초 간격·최대 200장 사진 + (옵션) ARCore */
    CONTINUOUS,
    VIDEO,
}

enum class ResolutionPreset(val width: Int, val height: Int) {
    RESOLUTION_1024x1024(1024, 1024)
}

enum class MainTab {
    LIBRARY, CLAUDE, CAMERA, CREATE, PROFILE
}

enum class LibraryTab {
    GALLERY, DATASET, MODEL_3D, AI_CAD,
    /** 서버 파이프라인 top/side 등 미리보기 PNG */
    GS_PREVIEW,
    /** 서버 파이프라인 quality 등 분석 결과 이미지 */
    GS_ANALYSIS,
    /** LLM 3DGS·서버 분석용 JSON 저장소 */
    JSON_LIBRARY,
    /** ARCore용 에셋(모델·설정 등) 로컬 저장소 */
    AR_CORE_LIBRARY,
}

enum class LibraryDetailScreen {
    NONE,
    DATASET_FOLDER,
    MODEL_VIEWER,
    OBJ_VIEWER,
    MODEL_3D_PLY_LIST,
    MODEL_3D_OBJ_LIST,
}

enum class CameraEntryMode {
    OBJECT, SPACE_2D, SPACE_3D, MOBILE_SPACE
}

internal fun CameraEntryMode.isSpaceMode(): Boolean = this != CameraEntryMode.OBJECT
internal fun CameraEntryMode.isObjectMode(): Boolean = this == CameraEntryMode.OBJECT
