# Mobile-GS upstream ↔ Android 앱 매핑

`upstream/` 폴더에는 PC용 **Mobile-GS** 저장소(xiaobiaodu/Mobile-GS, CUDA 학습·렌더)에서 가져온 참조 스크립트가 들어 있습니다.  
온디바이스 앱은 Python을 실행하지 않으며, 수식·파일 포맷 일치를 위해 동일 소스를 에셋으로 묶어 두었습니다.

## 번들된 파일 (요약)

| 에셋 파일 | 역할 (upstream) | 앱 쪽 대응 |
|-----------|-----------------|------------|
| `scene_colmap_loader.py` | COLMAP `cameras.bin` / `images.bin` / `points3D.bin` 텍스트·바이너리 읽기 | `ColmapBinaryReader.kt` |
| `scene_cameras.py` | 학습용 Camera / MiniCam, world-view·projection | `MobileGaussianSplattingEngine` (EXIF·역투영 개념만 유사) |
| `utils_graphics_utils.py` | `focal2fov`, `getWorld2View2`, projection 행렬 | `MobileGaussianSplattingEngine.readCameraInfo` 등 |
| `scene_dataset_readers.py` | COLMAP 데이터셋 로드, **getNerfppNorm** | 씬 반경·중심 정규화 로직 |
| `scene_gaussian_model.py` | 3D Gaussian 파라미터·초기화·SH | `MobileGaussianSplattingEngine.buildSceneFromImages` / COLMAP 점군 변환 |
| `pretrain.py` / `train.py` / `render.py` | 데스크톱 학습·평가 파이프라인 엔트리 | 앱 미실행 — 참고만 |
| `requirements.txt` | Python 의존성 | — |
| `README_Mobile-GS.md` | 프로젝트 설명·실행 명령 | — |

## 앱에서 실제로 도는 경로

1. **입력**: 갤러리·데이터셋 폴더 URI, 또는 SAF로 선택한 COLMAP 바이너리 3개 — `MainActivity` `Mobile3dGsScreen`.
2. **오케스트레이션**: `MobileGaussianSplattingScript.runFromSelectedInputs` → `runPipeline`(이미지) 또는 `runColmapPointCloudPipeline`(COLMAP).
3. **씬 빌드**: `MobileGaussianSplattingEngine`.
4. **뷰어**: `MobileGaussianSplatGlView` (OpenGL ES 2.0, Brush 스타일 가우시안 스플랫 근사).

업스트림 README에 적힌 **모바일 Vulkan 뷰어는 비공개**이므로, 본 앱은 GLES2 전용 뷰어로 대체합니다.
