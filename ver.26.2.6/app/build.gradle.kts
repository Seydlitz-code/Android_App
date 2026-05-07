import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.app_01"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.app_01"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API 키: local.properties에 claude_api_key, openai_api_key, gemini_api_key 추가
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        buildConfigField(
            "String",
            "CLAUDE_API_KEY",
            "\"${localProperties.getProperty("claude_api_key", "")}\""
        )
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${localProperties.getProperty("openai_api_key", "")}\""
        )
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("gemini_api_key", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // 모델 파일 압축 방지 (assets에서 직접 로드)
    androidResources {
        noCompress += listOf("tflite", "onnx", "gz")
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // CameraX (16KB 페이지 크기 호환성 개선 포함)
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-video:${cameraxVersion}")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Glide — URI·파일에서 Bitmap 로드 등
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")
    
    // OkHttp + Retrofit + Gson (AI CAD 파이프라인: API 클라이언트)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    val retrofitVer = "2.11.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVer")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVer")
    implementation("com.google.code.gson:gson:2.11.0")

    // SceneView / Filament — GLB 미리보기 (Maven Central에 게시된 버전)
    val sceneViewVer = "2.3.0"
    implementation("io.github.sceneview:sceneview:$sceneViewVer")

    // ARCore — 사진 촬영 시 포즈·카메라 Intrinsics 메타데이터 수집
    implementation("com.google.ar:core:1.48.0")

    // EXIF (이미지 회전/방향 보정)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // MediaPipe Tasks (사물 경계/크기 분석 - ObjectDetector, Interactive Segmenter)
    val mediaPipeVer = "0.10.21"
    implementation("com.google.mediapipe:tasks-vision:$mediaPipeVer")

    // ONNX Runtime Android — U²-Net(u2netp) 카테고리 제한 없는 배경 제거
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}