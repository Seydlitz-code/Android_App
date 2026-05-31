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

### 7.6 서버 연결 기본값·LAN 전환 (ver 5.29 작업 중)

- **기본 서버**: `192.168.0.17:8000` · **HTTP** (`DEFAULT_USE_HTTPS = false`). 동일 Wi‑Fi LAN에서 PC FastAPI(`uvicorn main:app --host 0.0.0.0 --port 8000`)에 직접 연결하는 구성을 전제로 합니다.
- **레거시 ngrok 마이그레이션**: `AppApplication.onCreate` → `migrateLegacyServerSettingsIfNeeded()` — 기존 ngrok 호스트·443·HTTPS 설정을 LAN 기본값으로 자동 교체.
- **LAN 호스트 정규화**: `isLikelyLanHost`, `normalizeServerUseHttps` — 사설 IP에 HTTPS가 켜져 있으면 HTTP로 강제 정규화(SSL 오류 방지).
- **연결 테스트 강화**: `testServerConnection` → `ServerConnectionTestResult` — `/`, `/docs`, `/openapi.json`, `/health`, `/upload` 순으로 GET 프로브 후 `/upload` 도달 여부 확인. 성공 시 설정 자동 저장.
- **콜백 URL**: `resolvePipelineCallbackUrlForUpload` — 같은 Wi‑Fi에서는 휴대폰 로컬 IP(`PipelineCallbackHttpServer`)를 `callback_url`로 우선 사용.
- **업로드 실패 UX**: `formatServerUploadFailurePopup` — 진행 메시지에 서버 상세 오류가 있으면 팝업에 그대로 표시.
- **네트워크 보안**: `res/xml/network_security_config.xml` — LAN cleartext HTTP 허용(매니페스트 `networkSecurityConfig` 참조).

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
  - **ver 5.28~5.29**: `TRACKING` 상태 프레임만 `poses.json`에 포함. JPEG **실제 저장 해상도**(`readJpegFileDimensions`, `CaptureFrameMetaRegistry`)로 intrinsics 스케일. 동영상 타임라인은 인덱스 기반 매칭(`video_timeline_index`). CameraX `imageInfo.timestamp`를 촬영 시각으로 사용.
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
| `CaptureFrameMetaRegistry.kt` | 촬영 프레임 메타(JPEG 해상도·타임스탬프) — poses.json 1:1 매칭 |
| `HtmlReportWebViewScreen.kt` / `ThreeDgsChatHtmlExport.kt` | HTML 보고서 WebView·내보내기 (ver 5.27) |
| `ThreeDgsChatPdfExport.kt` | 채팅 PDF 내보내기 (ver 5.19) |
| `res/xml/network_security_config.xml` | LAN cleartext HTTP 허용 (ver 5.29) |
| `BackgroundRemovalProcessor.kt` / `GlareRemovalProcessor.kt` | 이미지 후처리 |
| `AppUiPalette.kt` / `ui/theme/Theme.kt` | 테마·색상 |

(전체 74개 내외 Kotlin 파일 — 나머지는 특정 기능(OpenSCAD 내보내기, COLMAP 바이너리, DuckDuckGo 보조 검색 등) 단위 모듈입니다.)

---

## 14. 빌드·실행 시 참고

- **Gradle**: `ver.26.2.6` 디렉터리에서 `./gradlew assembleDebug` 등.
- **클리어텍스트**: 매니페스트 `usesCleartextTraffic=true` + `network_security_config.xml` — LAN HTTP 업·다운로드 허용; 외부 배포 시 보안 검토 권장.
- **대용량 에셋**: `noCompress`에 `tflite`, `onnx`, `gz` 등이 포함되어 있습니다.

---

## 15. 업데이트 기록

Git 커밋 메시지·변경 파일을 기준으로 정리했습니다. 최신 항목이 위에 옵니다.

### ver 5.29 (작업 중, 미커밋)

| 영역 | 내용 |
|------|------|
| 서버·네트워크 | ngrok 기본값 → **LAN HTTP**(`192.168.0.17:8000`) 전환. 레거시 설정 자동 마이그레이션, LAN HTTPS 정규화, 다중 경로 연결 테스트·실패 메시지 상세화, 업로드 로그·`Accept: application/json` 헤더 |
| 네트워크 보안 | `network_security_config.xml` 추가 — 사설 IP·localhost cleartext 허용 |
| ARCore | `TRACKING` 전용 프레임 필터·`waitForTrackingFrame`, JPEG 실제 픽셀 크기 반영, 동영상 타임라인 인덱스 매칭, intrinsics 재스케일 |
| UI/UX | `Gs3dWebViewScreen`·`HtmlReportWebViewScreen` 시스템 뒤로가기(`BackHandler`) 처리. 서버 설정 화면 LAN 안내 문구·플레이스홀더 갱신 |
| 업로드 UX | `formatServerUploadFailurePopup` — 서버 오류 상세를 팝업에 표시 |

### ver 5.28 (2026-05-28)

- **ARCore 버그 수정**: `ArcorePoseSnapshotter` 대폭 개선 — 포즈·intrinsics·타임스탬프 매칭 정확도 향상, `CaptureFrameMetaRegistry` 확장.
- **카메라**: `CameraTabScreens` ARCore 메타 저장 시 `TRACKING` 검증, `ObjectOutOfFrameWarning` 조정.
- **최적화**: `AppMainEnums`, `MainActivity` 촬영 메타 기록 경로 정리.

### ver 5.27 (2026-05-27)

- **AI 채팅 리팩터**: `ClaudeChatClient.kt`·`AiChatTabScreens.kt` 클린코드 기반 함수 최적화.
- **HTML 보고서**: `HtmlReportWebViewScreen.kt`, `ThreeDgsChatHtmlExport.kt` 신규 — 채팅 결과 HTML 미리보기·내보내기.
- **공통 유틸**: `AppExtensions.kt` 추가.
- **기타**: `PoliceInsuranceDocxWriter`, `LibraryTabScreens`, `ServerPipelineNetworking` 소규모 개선.

### ver 5.21.x (2026-05-21)

- **5.21.4** — AI 탭 UI/UX 버그 픽스.
- **5.21.3 / 5.21.2** — 연속 패치(커밋 메시지: 버전 업데이트).
- **5.21.1** — 미사용 스크립트·실험 폴더(`cv_test_py`, `cv_test_cpp`) 정리. **`PROJECT_INDEX.md` 최초 작성**. `ArcoreLibrary`, `PipelineCallbackHttpServer` 보강.
- **5.21** — **서로 다른 Wi‑Fi 환경 통신**: `resolvePipelineCallbackUrlForUpload` — LAN일 때 휴대폰 로컬 콜백 URL 우선. `PlyLibrary.kt` 추가. 라이브러리·카메라 UI 대규모 리팩터.

### ver 5.20 (2026-05-20)

- **서버 전송 방식 수정**: `LibraryTabScreens` 업로드 플로우 개선.

### ver 5.19 (2026-05-19)

- **대규모 기능 추가**: AI 채팅·카메라·라이브러리·프로필 전면 개편.
- **PDF 내보내기**: `ThreeDgsChatPdfExport.kt` 신규(약 500줄).
- **3DGS WebView**: `Gs3dWebViewScreen.kt` 개선.
- **캐시·서버**: `AppCacheCleaner`, `ServerPipelineNetworking` 확장.

### ver 5.16 ~ 5.17 (2026-05-16 ~ 05-17)

- **5.16.2 / 5.16** — **서버 다운로드 중 강제 종료(OOM) 수정**. `ServerPipelineNetworking.kt`를 `MainActivity`에서 분리(1,500줄+). `AppWarningLog`, `AppCacheCleaner`, `ServerPipelinePostDownload.kt`, `PointCloudQualityReport.kt` 추가. `PipelineCallbackHttpServer` 스트리밍·OOM 방어 강화.
- **5.7 (5.17)** — **3DGS WebView 수신 오류 수정**. 콜백 서버·라이브러리·프로필 리팩터.

### ver 5.11 ~ 5.9 (2026-05-09 ~ 05-11)

- **5.11.2 / 5.11** — 서버 파일 다운로드 크래시 수정. 다운로드 후 분석 파이프라인 단일화. `PipelineCallbackHttpServer` 이벤트 처리 개선.
- **5.9** — **서버 업로드**: `file_pc`·`file_gs`(ARCore ZIP) 동시 멀티파트 전송. `PipelineCallbackHttpServer.kt` 신규(161줄). `ServerTaskArtifactHelpers` 확장.

### ver 5.8.x ~ 5.7 (2026-05-07 ~ 05-08)

- **5.8.2 / 5.8.1 / 5.7.3 / 5.7.2 / 5.7** — 연속 안정화·최적화 패치(카메라, 라이브러리, 서버 연동 세부 조정).

### ver 5.6 ~ 5.5 (2026-05-05 ~ 05-06)

- 버전 업데이트·기능 보완(커밋 메시지: 버전 업데이트).

### ver 4.29 (2026-04-29)

- 버전 업데이트.

### ver 3.x — 초기 AI CAD·3D 뷰어 (2026-03 ~ 04)

| 버전 | 주요 내용 |
|------|-----------|
| **기능 업데이트 (4월)** | 3D 뷰어·라이브러리 UI 개선. `Model3dThumbnail.kt`, `ModelLibraryPaths.kt`, `ObjViewer.kt` 대폭 확장 |
| **VGGT 뷰어 (4/3)** | AI CAD 파이프라인 전체 도입 — `AiCadPipeline.kt`, OpenSCAD→STL→GLB(`StlToGlbConverter.kt`), `ClaudeChatClient` 확장, `GlareRemovalProcessor.kt`, 채팅 스레드 저장(`ChatThreadStorage.kt`) |
| **3.18 ~ 3.19 (3월)** | 초기 Compose 앱 골격·카메라·라이브러리 기반 구축 |

### 저장소 초기 (2026-02-09)

- **Initial commit** — `ver.26.2.6` Android 프로젝트 생성.

---

## 16. 문서 유지보수

- 이 인덱스는 코드 스냅샷 기준이며, 화면 이름·문구 리팩터링 후에는 해당 파일 경로만 확인하면 됩니다.
- **업데이트 기록(§15)** 은 커밋·작업 트리 변경 시 함께 갱신하세요.
- 서버 API 계약은 실제 배포되는 **`main.py`(저장소 외부)** 와 주석·상수를 반드시 교차 검증하세요 (`PipelineCallbackEvents`, multipart 필드명 등).
