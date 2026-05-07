package com.example.app_01

import android.content.Context

/** 카메라 탭·프로필 ARCore 설정이 공유하는 저장소. */
object CameraArCorePrefs {
    private const val PREFS_NAME = "camera_prefs"
    private const val KEY_ARCORE_PHOTO_META = "arcore_photo_meta"

    fun isArCoreMetaEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ARCORE_PHOTO_META, true)

    fun setArCoreMetaEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ARCORE_PHOTO_META, enabled)
            .apply()
    }
}
