package com.example.app_01

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class AppPermissionInfo(
    val name: String,
    val description: String,
    val manifestPermission: String,
    val minSdk: Int = 0,
    val maxSdk: Int = Int.MAX_VALUE
) {
    fun isApplicable(): Boolean {
        val sdk = Build.VERSION.SDK_INT
        return sdk >= minSdk && sdk <= maxSdk
    }

    fun isGranted(context: Context): Boolean {
        if (!isApplicable()) return true
        return ContextCompat.checkSelfPermission(context, manifestPermission) ==
            PackageManager.PERMISSION_GRANTED
    }
}

object AppPermissions {
    fun list(context: Context): List<AppPermissionInfo> = buildList {
        add(AppPermissionInfo(
            name = "카메라",
            description = "사진 및 동영상 촬영, 3D 스캔에 사용됩니다.",
            manifestPermission = Manifest.permission.CAMERA
        ))
        add(AppPermissionInfo(
            name = "마이크",
            description = "동영상 녹화 시 음성 녹음에 사용됩니다.",
            manifestPermission = Manifest.permission.RECORD_AUDIO
        ))
        add(AppPermissionInfo(
            name = "미디어 파일 (이미지)",
            description = "갤러리 이미지 불러오기 및 저장에 사용됩니다.",
            manifestPermission = Manifest.permission.READ_MEDIA_IMAGES,
            minSdk = Build.VERSION_CODES.TIRAMISU
        ))
        add(AppPermissionInfo(
            name = "저장소",
            description = "이미지 파일 읽기 및 쓰기에 사용됩니다. (Android 12 이하)",
            manifestPermission = Manifest.permission.READ_EXTERNAL_STORAGE,
            maxSdk = Build.VERSION_CODES.S_V2
        ))
        add(AppPermissionInfo(
            name = "저장소 (쓰기)",
            description = "이미지 파일 저장에 사용됩니다. (Android 9 이하)",
            manifestPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            maxSdk = Build.VERSION_CODES.P
        ))
        add(AppPermissionInfo(
            name = "알림",
            description = "업로드·배경 제거·광택 제거 등 백그라운드 작업 진행 상황을 알림 표시줄에 표시합니다.",
            manifestPermission = Manifest.permission.POST_NOTIFICATIONS,
            minSdk = Build.VERSION_CODES.TIRAMISU
        ))
    }.filter { it.isApplicable() }
}
