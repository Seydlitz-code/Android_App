# PROJECT_INDEX — Android_App

이 문서는 저장소 **Android_App** 안에서 실제 제품 코드가 들어 있는 **`ver.26.2.6`** 모듈을 중심으로, 앱이 어떻게 동작하는지 개발자 관점에서 정리한 인덱스입니다. (형제 폴더 `cv_test_py`, `cv_test_cpp` 등은 실험·보조 용도로 보이며, 본 문서에서는 앱 모듈만 상세히 다룹니다.)

---

## 1. 저장소·모듈 구조

| 경로 | 설명 |
|------|------|
| `ver.26.2.6/` | Gradle Android 프로젝트 루트 (`settings.gradle.kts`, `build.gradle.kts`) |
| `ver.26.2.6/app/` | 애플리케이션 모듈 (`com.example.app_01`) |
| `ver.26.2.6/app/src/main/java/com/example/app_01/` | Kotlin 소스 대부분 (단일 패키지 집중 구조) |
| `ver.26.2.6/app/src/main/AndroidManifest.xml` | 진입 Activity, 권한, FileProvider, 포그라운드 서비스 선언 |

앱은 **Jetpack Compose + 단일 Activity (`MainActivity`)** 패턴입니다. Navigation Compose 라이브러리 대신 `MainActivity` 내부의 `when(selectedTab)` 및 플래그(`showSettings`, `showServerSettings` 등)로 화면을 전환합니다.

---

## 2. 제품 개요

모바일에서 **사진·동영상 촬영(카메라·ARCore 옵션)**, **갤러리·데이터셋·3D 에셋 라이브러리 관리**, **원격 서버(Jetson/FastAPI 등)로 ZIP 업로드 후 DA3·3DGS 파이프라인 실행**, **결과(PLY·GLB·PNG·JSON 등) 다운로드·표시**, **AI 채팅(Claude 등)으로 보고서용 스크립트·분석 연동**, **온디바이스/보조 서버 배경 제거·AI CAD(OpenSCAD 경유)** 까지 한 앱에서 처리하는 통합 도구에 가깝습니다.

---

## 3. 기술 스택 (요약)

- **언어**: Kotlin 11 타깃  
- **UI**: Compose Material3, Coil/Glide 이미지 로딩  
- **카메라**: CameraX (사진·동영상·연속 촬영)  
- **AR**: ARCore (선택 하드웨어, 매니페스트 `optional`)  
- **3D**: SceneView/Filament(GLB), 커스텀 OpenGL(PHY/OBJ/STL 미리보기 등), 모바일 Gaussian Splatting 관련 스크립트·GL 뷰  
- **네트워크**: OkHttp 중심 (`ServerPipelineNetworking.kt`), Retrofit 모듈은 AI CAD 원격 호출 등에 사용  
- **로컬 HTTP**: NanoHTTPD (`PipelineCallbackHttpServer.kt`) — 서버가 멀티파트로 결과를 **POST 콜백**할 때 수신  
- **ML**: MediaPipe Tasks Vision, ONNX Runtime Android(U²-Net 계열 배경 제거 게이트 등)

API 키는 `local.properties`의 `claude_api_key`, `openai_api_key`, `gemini_api_key`가 `BuildConfig`로 주입됩니다 (`app/build.gradle.kts`).

---

## 4. 앱 진입·전역 생명주기

### 4.1 `AppApplication`

- 미처리 JVM 예외 로깅용 **`AppWarningLogInstaller`** 설치 (`AppApplication.kt`).

### 4.2 `MainActivity`

- Compose `setContent`로 테마·팔레트(`AppUiPalette`, `LocalAppUiPalette`)를 두고 최상위 UI 구성.
- **`selectedTab: MainTab`** — 하단 바 탭 (라이브러리 / AI / 카메라 / 3DGS / 프로필).
- **`selectedLibraryTab: LibraryTab`** — 라이브러리 탭 내부 서브 탭 (갤러리, 데이터셋, 모델 3D, AI CAD, DA3 관련 서버 결과 탭, JSON·ARCore·GLB 등).
- **`GalleryScreen`** (`LibraryTabScreens.kt`)에 갤러리 미디어 목록·서버 결과 번들·업로드 콜백 등 많은 상태를 넘깁니다.
- 오버레이 화면: 설정, 서버 설정, ARCore 설정, 센서 점검, 권한, 테마, 경고 로그 등은 별도 `@Composable` 화면으로 풀스크린 전환.

### 4.3 `AppForegroundService` (`MainActivity.kt` 하단부에 정의)

- 업로드·배경 제거 등 장시간 작업 시 **포그라운드 알림**으로 프로세스 우선순위 유지 (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`).
- 매니페스트에 등록되어 있으며, 채널 ID·진행 메시지 extras로 갱신 가능한 구조입니다.

---

## 5. 하단 네비게이션과 화면 역할

정의: `MainBottomNavigation.kt`, 열거형 `MainTab` (`AppMainEnums.kt`).

| 탭 | `MainTab` | 주요 Composable / 파일 |
|----|-----------|-------------------------|
| 라이브러리 | `LIBRARY` | `GalleryScreen` — `LibraryTabScreens.kt` (대형 파일: 허브·갤러리·데이터셋·서버 산출물·PLY/GLB 뷰어 등) |
| AI | `CLAUDE` | `AiChatTabScreens.kt` — 멀티 모드 채팅(클로드, AI CAD, DA3 분석, 파손 분석), 스레드 저장(`ChatThreadStorage`) |
| 카메라 | `CAMERA` | `CameraTabScreens.kt` — CameraX, ARCore 포즈 스냅샷, 연속 촬영·데이터셋 폴더 저장 |
| 3DGS | `CREATE` | `Mobile3dGsTabScreen.kt` 등 — 모바일 3DGS·관련 워크플로 UI |
| 프로필 | `PROFILE` | `ProfileTabScreens.kt` |

카메라 활성 중이거나 IME 표시 등 조건에서 하단 바를 숨기는 분기가 `MainActivity`에 있습니다.

---

## 6. 라이브러리 탭 (`GalleryScreen`) 동작 요약

- **갤러리**: 촬영·임포트된 미디어 URI 목록 (`GalleryImageLoad.kt`, `MainActivity`의 `loadCapturedMediaSync` 등). 그리드 썸네일은 Coil 요청 크기 제한으로 메모리 최적화.
- **데이터셋 폴더**: `getExternalFilesDir(null)/datasets/` 아래 폴더 단위 라이브러리 (`DatasetFolder`, `loadDatasetFoldersSync` — `MainActivity.kt`).
- **모델 3D**: `ModelLibraryPaths` — `models/ply`, `models/obj` 등 외부 저장소 하위 경로.
- **서버 작업 결과**: `models/ply/server_task_<taskId>/` 형태로 다운로드된 번들 (`ServerPipelineResultBundle`). 메타는 `.server_artifacts.json`, 스캔은 `ServerTaskArtifactHelpers.scanServerTaskManifestInfos`.
- **JSON 라이브러리**: `JsonLibrary` — `models/json/`, 파이프라인 산출 JSON 복사·정렬 목록.
- **ARCore 라이브러리**: `ArcoreLibrary` — ZIP/JSON/GLB 등 정렬 목록.
- **GLB 라이브러리**: 서버 GLB 결과 뷰어 연동.

편집·배치 기능 예: 배경 제거(MediaPipe/ONNX/SAM3 서버 경로), 광택 제거, 폴더 생성·공유·내보내기, **서버 ZIP 업로드** 다이얼로그 등이 같은 파일 군에 포함됩니다.

---

## 7. 서버 파이프라인 (핵심 데이터 플로우)

구현 허브: **`ServerPipelineNetworking.kt`** (엔드포인트 상수, OkHttp 클라이언트, 업로드·상태 폴링·결과 목록·파일 스트리밍 다운로드).

### 7.1 업로드

- 일반적으로 **`POST /upload`** 에 멀티파트 전송.
- 필드 **`file_pc`**: 데이터셋 또는 미디어 ZIP (필수에 가깝게 사용).
- 필드 **`file_gs`**: ARCore 등 보조 ZIP (선택).
- 요청 시 로컬에서 돌아가는 **`PipelineCallbackHttpServer`** 의 공개 URL을 **`callback_url`** 로 넣어, 서버가 완료 후 결과를 역으로 POST 하도록 설계된 흐름이 있습니다 (`PIPELINE_CALLBACK_PATH`).

### 7.2 콜백 수신 (`PipelineCallbackHttpServer.kt`)

- NanoHTTPD로 **`POST .../pipeline/callback`** 만 허용.
- 대용량 멀티파트는 parseBody OOM 방지를 위해 **스트리밍/직접 파싱** 등 방어 코드가 많음.
- 파트를 임시가 아닌 고정 디렉터리로 복사한 뒤, **`Channel<PipelineCallbackEvent>`** 로 메인 로직에 넘김.
- 이벤트 종류: `PipelineCallbackEvents` — 결과 파일 묶음, 실패, `3DGS_COMPLETED` / `3DGS_FAILED`, 레거시 등.

### 7.3 상태·다운로드

- **`GET /status/{task_id}`** 폴링 — 장시간 무응답 허용 윈도우(`SERVER_STATUS_POLL_MAX_SILENCE_MS`) 등 세밀한 타임아웃 정책.
- **`GET /results/{task_id}`** 로 파일 목록·URL 확보 후 순차 다운로드 (버퍼·yield 간격으로 UI·OOM 완화).
- 신뢰되지 않은 HTTPS 환경용 커스텀 SSL 우회 옵션 등이 포함되어 있습니다(현장 디바이스 편의 목적 — 운영 시 보안 검토 필요).

### 7.4 다운로드 후 처리 (`ServerPipelinePostDownload.kt`)

- 매니페스트 기록 (`writeServerTaskArtifactManifest`).
- 백그라운드 단일 스레드에서 **`JsonLibrary.ingestFromPipelineOutputDir`**, 오래된 `server_task_*` 폴더 **`pruneOldServerTaskDirs`** 등 무거운 작업을 지연 실행.

### 7.5 AI 탭·파일 선택과의 연결

- 서버 산출물 URI 목록은 `ServerTaskArtifactHelpers`의 함수들로 집계됩니다 (예: 미리보기 래스터, DA3 분석 이미지 병합 `da3MergedRasterUrisForAiPicker`, 품질 평가용 JSON `da3QualityJsonUrisForServerTasks`).
- **`AiChatTabScreens.kt`** 의 파일 선택 다이얼로그는 위 목록과 갤러리·데이터셋·로컬 JSON/PLY 라이브러리 탭을 묶어 첨부합니다.

---

## 8. AI 채팅 탭 (`AiChatTabScreens.kt`)

- **모드**: 클로드 LLM, AI CAD(OpenSCAD/STL 파이프라인 연계 — `AiCadPipeline.kt`, `ClaudeChatClient.kt` 등), DA3/3DGS 분석용 스크립트·첨부, 파손 분석 모드 등.
- 메시지에 이미지·비디오·보조 파일(JSON·PLY·GLB·ZIP) URI를 함께 실을 수 있으며, 마크다운 렌더링·코드 블록에서 python-docx 내보내기 버튼 등이 연결됩니다 (`ThreeDgsChatDocxExport.kt`, `ThreeDgsChatPdfExport.kt`).
- 대화 스레드는 **`ChatThreadStorage`** 로 디스크에 저장 (`ChatThread.kt` 모델).

---

## 9. 카메라 탭 (`CameraTabScreens.kt`)

- CameraX Preview/ImageCapture/VideoCapture 및 분석용 ImageAnalysis 등 모드별 바인딩.
- **ARCore**: 세션·포즈 메타를 프레임과 함께 기록 (`ArcorePoseSnapshotter`, `ArcoreLibrary`, `JsonLibrary.saveArCoreFramesJson` 등과 연계).
- 연속 촬영·동영상·모바일 스페이스 스캔 관련 보조 UI (`MobileSpace*` 클래스들과 연동).

---

## 10. 3DGS 탭 및 뷰어

- **`Mobile3dGsTabScreen.kt`**, **`MobileGaussianSplatGlView.kt`**, **`Gs3dWebViewScreen.kt`** 등으로 로컬·URL 기반 3DGS 뷰잉 및 서버 완료 후 WebViewer URL 표시가 연결됩니다.
- 라이브러리에서 완료 번들을 열 때 **`onGs3dWaitingChange`**, 알림 채널 `gs3d_completion` 등 UX가 `MainActivity`에 묶여 있습니다.

---

## 11. AI CAD 서브시스템 (요약)

- **`AiCadPipeline.kt`**: LLM 출력 검증(`AiCadCodeVerifier.kt`), OpenSCAD 실행 경로, STL/GLB 변환(`StlToGlbConverter.kt`), 라이브러리 저장 조정(`AiCadSaveCoordinator.kt`).
- **`AiCadRemoteServerClient.kt` / `AiCadNetworkModule.kt`**: 원격 서버에 메시지 전송 시 Retrofit 사용.
- 결과 메시는 **`AiCadLibrary`** 및 라이브러리 탭 **AI CAD** 영역과 연동.

---

## 12. 로컬 디렉터리 관례

| 영역 | 경로 (대표) |
|------|----------------|
| 외부 앱 전용 저장소 루트 | `context.getExternalFilesDir(null)` |
| 데이터셋 라이브러리 | `.../datasets/<폴더>/` |
| PLY/OBJ/썸네일 등 | `.../models/ply`, `.../models/obj`, `ModelLibraryPaths` |
| 서버 태스크 결과 | `.../models/ply/server_task_<id>/` + `.server_artifacts.json` |
| 수집 JSON | `.../models/json/` (`JsonLibrary`) |
| AI CAD 라이브러리 등 | 코드 내 `aicad_library` 등 문자열 참조 (`MainActivity` 백업·정리 허용 경로) |

---

## 13. 주요 소스 파일 역할 빠른 참조

| 파일 | 역할 |
|------|------|
| `MainActivity.kt` | 단일 Activity, 탭·풀스크린 설정·갤러리 로드·데이터셋 유틸·포그라운드 서비스 클래스 포함 등 초대형 허브 |
| `LibraryTabScreens.kt` | 라이브러리 UI 전반 (`GalleryScreen`) |
| `AiChatTabScreens.kt` | AI 탭 채팅·마크다운·파일 선택 다이얼로그 |
| `CameraTabScreens.kt` | 카메라·ARCore·촬영 모드 |
| `Mobile3dGsTabScreen.kt` | 3DGS 탭 화면 |
| `ProfileTabScreens.kt` | 프로필 |
| `ServerPipelineNetworking.kt` | 서버 업로드/폴링/다운로드·설정 키 |
| `PipelineCallbackHttpServer.kt` | 로컬 콜백 HTTP 서버 |
| `ServerTaskArtifactHelpers.kt` | 서버 태스크 매니페스트 스캔·URI 수집 |
| `ServerPipelinePostDownload.kt` | 다운로드 후 지연 후처리 |
| `ClaudeChatClient.kt` / `AnthropicApi.kt` | Anthropic API 호출 |
| `ChatThreadStorage.kt` | 채팅 스레드 영속화 |
| `ArcoreLibrary.kt` / `ArcorePoseSnapshotter.kt` | ARCore 에셋·포즈 |
| `BackgroundRemovalProcessor.kt` / `GlareRemovalProcessor.kt` | 이미지 후처리 |
| `AppUiPalette.kt` / `ui/theme/Theme.kt` | 테마·색상 |

(전체 74개 내외 Kotlin 파일 — 나머지는 특정 기능(OpenSCAD 내보내기, COLMAP 바이너리, DuckDuckGo 보조 검색 등) 단위 모듈입니다.)

---

## 14. 빌드·실행 시 참고

- **Gradle**: `ver.26.2.6` 디렉터리에서 `./gradlew assembleDebug` 등.
- **클리어텍스트**: 매니페스트 `usesCleartextTraffic=true` — 로컬/테스트 편의용; 배포 빌드에서는 네트워크 보안 설정 검토 권장.
- **대용량 에셋**: `noCompress`에 `tflite`, `onnx`, `gz` 등이 포함되어 있습니다.

---

## 15. 문서 유지보수

- 이 인덱스는 코드 스냅샷 기준이며, 화면 이름·문구 리팩터링 후에는 해당 파일 경로만 확인하면 됩니다.
- 서버 API 계약은 실제 배포되는 **`main.py`(저장소 외부)** 와 주석·상수를 반드시 교차 검증하세요 (`PipelineCallbackEvents`, multipart 필드명 등).
