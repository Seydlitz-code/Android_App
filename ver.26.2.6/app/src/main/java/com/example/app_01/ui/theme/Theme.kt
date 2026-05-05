package com.example.app_01.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Material3: 앱 하단 라ime·채팅 브랜드에 맞춘 다크 스킴(보라 기본값 대체). */
private val AppBrandDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD83B),
    onPrimary = Color(0xFF0D0F0A),
    primaryContainer = Color(0xFF2D3D18),
    onPrimaryContainer = Color(0xFFE3F5C8),
    secondary = Color(0xFFB8D4A0),
    onSecondary = Color(0xFF1A1C16),
    tertiary = Color(0xFFC8E67A),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFC8C8C8),
    outline = Color(0xFF4A4A4A),
)

/** Material3: 화이트 모드 전용 — 밝은 서피스·라임 포인트(시스템 동적 색상과 구분). */
private val AppBrandLightColorScheme = lightColorScheme(
    primary = Color(0xFF6FA026),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F0D0),
    onPrimaryContainer = Color(0xFF1E2E0C),
    secondary = Color(0xFF4B7A12),
    onSecondary = Color.White,
    tertiary = Color(0xFF9CD83B),
    onTertiary = Color(0xFF1A1F14),
    background = Color(0xFFF2F5F9),
    onBackground = Color(0xFF121518),
    surface = Color.White,
    onSurface = Color(0xFF121518),
    surfaceVariant = Color(0xFFE8EEF4),
    onSurfaceVariant = Color(0xFF3D4654),
    outline = Color(0xFFC9D4E0),
)

@Composable
fun App_01Theme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AppBrandDarkColorScheme
        else -> AppBrandLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}