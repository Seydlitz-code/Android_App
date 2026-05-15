package com.example.app_01

import android.app.Application

/**
 * [AppWarningLogInstaller] 로 처리되지 않은 JVM 예외를 파일에 기록하기 위해 매니페스트에 등록함.
 */
class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppWarningLogInstaller.install(this)
    }
}
