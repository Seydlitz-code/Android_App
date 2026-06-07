# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose / Kotlin
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Retrofit / Gson (AI CAD)
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.app_01.** { *; }

# ONNX / MediaPipe — JNI·네이티브 엔트리 보존
-keep class ai.onnxruntime.** { *; }
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# SceneView / Filament
-keep class io.github.sceneview.** { *; }
-dontwarn dev.romainguy.**

# NanoHTTPD callback server
-keep class fi.iki.elonen.** { *; }
