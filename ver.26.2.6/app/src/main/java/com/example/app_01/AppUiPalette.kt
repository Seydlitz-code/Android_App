package com.example.app_01

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppUiThemeMode {
    DARK,
    LIGHT
}

private const val APP_PREFS = "app_prefs"
private const val PREF_UI_APP_THEME = "ui_app_theme"

fun readAppUiThemeMode(context: Context): AppUiThemeMode {
    val v = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        .getString(PREF_UI_APP_THEME, "dark") ?: "dark"
    return if (v == "light") AppUiThemeMode.LIGHT else AppUiThemeMode.DARK
}

fun writeAppUiThemeMode(context: Context, mode: AppUiThemeMode) {
    context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE).edit()
        .putString(PREF_UI_APP_THEME, if (mode == AppUiThemeMode.LIGHT) "light" else "dark")
        .apply()
}

/**
 * 앱 크롬(탭·프로필·설정·채팅 등)용 색. 카메라 프리뷰·미디어 상세 등은 기존 고정 다크 유지.
 */
data class AppUiPalette(
    val isDark: Boolean,
    val background: Color,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    val divider: Color,
    val surfaceCard: Color,
    val onSurfaceCard: Color,
    val surfaceCardAlt: Color,
    val chatInputBarBg: Color,
    val chatComposerPill: Color,
    val chatComposerPillInactive: Color,
    val placeholder: Color,
    val drawerPanelBg: Color,
    val threadRowActive: Color,
    val dialogSurface: Color,
    val scrim: Color,
    val dropdownMenuBg: Color,
    val brand: Color,
    val onBrand: Color,
    val bottomBarBg: Color,
    val bottomNavContent: Color,
    val bottomNavContentMuted: Color,
    val chipUnselectedBg: Color,
    val chipUnselectedLabel: Color,
    val error: Color,
    val errorBadgeBg: Color,
    val codeBlockBg: Color,
    val codeBlockBorder: Color,
    val codeBlockHeaderBg: Color,
    val codeBlockMeta: Color,
    val markdownDefault: Color,
    val aiCadWindowBg: Color,
    val aiCadWindowBorder: Color,
    val aiCadWindowHeader: Color,
    val aiCadCodeText: Color,
    val progressTrack: Color,
    val dropdownMenuBorder: Color,
    /** Mobile 3DGS 메인 CTA (활성) */
    val mobileGsCtaEnabledBg: Color,
    val mobileGsCtaOnEnabled: Color,
)

fun appUiPaletteFor(mode: AppUiThemeMode): AppUiPalette = when (mode) {
    AppUiThemeMode.DARK -> AppUiPalette(
        isDark = true,
        background = Color.Black,
        onBackground = Color.White,
        onBackgroundMuted = Color.White.copy(alpha = 0.75f),
        divider = Color.White.copy(alpha = 0.3f),
        surfaceCard = Color(0xFF2A2A2A),
        onSurfaceCard = Color.White,
        surfaceCardAlt = Color(0xFF2C2C2E),
        chatInputBarBg = Color(0xFF111111),
        chatComposerPill = Color(0xFF2A2A2A),
        chatComposerPillInactive = Color(0xFF3A3A3A),
        placeholder = Color(0xFF888888),
        drawerPanelBg = Color(0xFF1C1C1E),
        threadRowActive = Color(0xFF2A2A2E),
        dialogSurface = Color(0xFF252525),
        scrim = Color.Black.copy(alpha = 0.55f),
        dropdownMenuBg = Color(0xFF2A2A2A),
        brand = Color.White,
        onBrand = Color.Black,
        bottomBarBg = Color.Black,
        bottomNavContent = Color.White,
        bottomNavContentMuted = Color.White.copy(alpha = 0.55f),
        chipUnselectedBg = Color(0xFF2A2A2A),
        chipUnselectedLabel = Color.White,
        error = Color(0xFFFF6B6B),
        errorBadgeBg = Color(0xFFFF6B6B).copy(alpha = 0.15f),
        codeBlockBg = Color(0xFF1A1A1A),
        codeBlockBorder = Color(0xFF3A3A3A),
        codeBlockHeaderBg = Color(0xFF252525),
        codeBlockMeta = Color(0xFF888888),
        markdownDefault = Color.White,
        aiCadWindowBg = Color(0xFF0D0D0D),
        aiCadWindowBorder = Color(0xFF3F3F3F),
        aiCadWindowHeader = Color(0xFF1A1A1A),
        aiCadCodeText = Color(0xFFD4D4D4),
        progressTrack = Color(0xFF2C2C2C),
        dropdownMenuBorder = Color.Transparent,
        mobileGsCtaEnabledBg = Color.White,
        mobileGsCtaOnEnabled = Color.Black,
    )
    AppUiThemeMode.LIGHT -> AppUiPalette(
        isDark = false,
        background = Color(0xFFF2F5F9),
        onBackground = Color(0xFF101418),
        onBackgroundMuted = Color(0xFF3D4654),
        divider = Color(0xFFD0DCE8),
        surfaceCard = Color.White,
        onSurfaceCard = Color(0xFF151A20),
        surfaceCardAlt = Color(0xFFE9F0F8),
        chatInputBarBg = Color.White,
        chatComposerPill = Color(0xFFEEF3FA),
        chatComposerPillInactive = Color(0xFFE0E8F2),
        placeholder = Color(0xFF64748B),
        drawerPanelBg = Color.White,
        threadRowActive = Color(0xFFE5F2D4),
        dialogSurface = Color.White,
        scrim = Color.Black.copy(alpha = 0.38f),
        dropdownMenuBg = Color.White,
        brand = Color.Black,
        onBrand = Color.White,
        bottomBarBg = Color.White,
        bottomNavContent = Color.Black,
        bottomNavContentMuted = Color.Black.copy(alpha = 0.52f),
        chipUnselectedBg = Color(0xFFE4EBF3),
        chipUnselectedLabel = Color(0xFF374151),
        error = Color(0xFFDC2626),
        errorBadgeBg = Color(0xFFFECACA),
        codeBlockBg = Color(0xFFF8FAFC),
        codeBlockBorder = Color(0xFFDCE5EE),
        codeBlockHeaderBg = Color(0xFFEEF3FA),
        codeBlockMeta = Color(0xFF64748B),
        markdownDefault = Color(0xFF151A20),
        aiCadWindowBg = Color(0xFFF8FAFC),
        aiCadWindowBorder = Color(0xFFDCE5EE),
        aiCadWindowHeader = Color(0xFFEEF3FA),
        aiCadCodeText = Color(0xFF334155),
        progressTrack = Color(0xFFE2E8F0),
        dropdownMenuBorder = Color(0xFFDCE5EE),
        mobileGsCtaEnabledBg = Color.Black,
        mobileGsCtaOnEnabled = Color.White,
    )
}

val LocalAppUiPalette = staticCompositionLocalOf { appUiPaletteFor(AppUiThemeMode.DARK) }
val LocalAppUiThemeMode = staticCompositionLocalOf { AppUiThemeMode.DARK }
val LocalSetAppUiThemeMode = staticCompositionLocalOf<(AppUiThemeMode) -> Unit> { { } }
