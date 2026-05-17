package com.example.app_01

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onThemeSettingsClick: () -> Unit = {},
    onLlmApiKeyClick: () -> Unit,
    onServerSettingsClick: () -> Unit,
    onArCoreSettingsClick: () -> Unit,
    onSensorCheckClick: () -> Unit,
    onWarningLogClick: () -> Unit,
    onPermissionsClick: () -> Unit = {},
) {
    val palette = LocalAppUiPalette.current
    val mode = LocalAppUiThemeMode.current
    val setMode = LocalSetAppUiThemeMode.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCleaningOrphanedData by remember { mutableStateOf(false) }

    fun runCleanupOrphanedData() {
        if (isCleaningOrphanedData) return
        isCleaningOrphanedData = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    cleanupOrphanedAppData(context.applicationContext)
                }
                val msg = if (result.isEmpty) {
                    "잉여 데이터가 없습니다."
                } else {
                    "잉여 데이터 정리 완료: 파일 ${result.deletedFiles}개, 폴더 ${result.deletedDirs}개 삭제"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Toast.makeText(context, "데이터 정리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                isCleaningOrphanedData = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "프로필",
            color = palette.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onThemeSettingsClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "테마",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLlmApiKeyClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "LLM API 키",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onServerSettingsClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "서버 설정",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onWarningLogClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "경고 로그",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onArCoreSettingsClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "ARCore",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSensorCheckClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "센서 확인",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPermissionsClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "권한 관리",
                    color = palette.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val allGranted = AppPermissions.list(ctx).all { it.isGranted(ctx) }
                if (!allGranted) {
                    Text(
                        text = "미승인 항목 있음",
                        color = palette.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.errorBadgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isCleaningOrphanedData) { runCleanupOrphanedData() }
                .padding(16.dp)
        ) {
            Text(
                text = if (isCleaningOrphanedData) "잉여 데이터 정리 중..." else "삭제 데이터 & 캐시 정리",
                color = palette.onBackground.copy(alpha = if (isCleaningOrphanedData) 0.5f else 1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
    }
}

@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val palette = LocalAppUiPalette.current
    val mode = LocalAppUiThemeMode.current
    val setMode = LocalSetAppUiThemeMode.current

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로",
                    tint = palette.onBackground
                )
            }
            Text(
                text = "테마",
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "앱에 적용할 테마를 선택합니다.",
                color = palette.onBackgroundMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(palette.divider)
            )
            ThemeSettingsOptionRow(
                title = "화이트 모드",
                selected = mode == AppUiThemeMode.LIGHT,
                onClick = { setMode(AppUiThemeMode.LIGHT) }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(palette.divider)
            )
            ThemeSettingsOptionRow(
                title = "다크 모드",
                selected = mode == AppUiThemeMode.DARK,
                onClick = { setMode(AppUiThemeMode.DARK) }
            )
        }
    }
}

@Composable
fun WarningLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    val scroll = rememberScrollState()
    var logText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        logText = withContext(Dispatchers.IO) { AppWarningLog.readLogText(context) }
    }

    LaunchedEffect(logText) {
        if (!logText.isNullOrEmpty()) {
            delay(80)
            scroll.scrollTo(scroll.maxValue)
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로",
                    tint = palette.onBackground
                )
            }
            Text(
                text = "경고 로그",
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        AppWarningLog.clear(context)
                        val t = AppWarningLog.readLogText(context)
                        withContext(Dispatchers.Main) { logText = t }
                    }
                }
            ) {
                Text("비우기", color = palette.error)
            }
            TextButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val t = AppWarningLog.readLogText(context)
                        withContext(Dispatchers.Main) { logText = t }
                    }
                }
            ) {
                Text("새로 고침", color = palette.brand)
            }
        }

        SelectionContainer(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = logText ?: "불러오는 중…",
                color = palette.onBackgroundMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
fun ThemeSettingsOptionRow(
    title: String,
    subtitle: String = "",
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppUiPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = palette.onBackgroundMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = palette.brand,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun ArCoreSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalAppUiPalette.current
    var arcoreMetaEnabled by remember {
        mutableStateOf(CameraArCorePrefs.isArCoreMetaEnabled(context))
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로",
                    tint = palette.onBackground
                )
            }
            Text(
                text = "ARCore",
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "촬영 시 ARCore로 포즈·카메라 내부 파라미터(Intrinsics 등)를 함께 저장합니다. 사진·연속 촬영·동영상(후면 카메라)에 적용됩니다. 기기에 ARCore가 없거나 미지원이면 저장이 생략되거나 비어 있을 수 있습니다.",
                color = palette.onBackgroundMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ARCore 메타 수집",
                        color = palette.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "켜면 JSON·ZIP 등에 포즈·카메라 메타가 포함됩니다.",
                        color = palette.onBackgroundMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = arcoreMetaEnabled,
                    onCheckedChange = {
                        arcoreMetaEnabled = it
                        CameraArCorePrefs.setArCoreMetaEnabled(context, it)
                    }
                )
            }
        }
    }
}
