package com.example.app_01

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    onLlmApiKeyClick: () -> Unit,
    onServerSettingsClick: () -> Unit,
    onArCoreSettingsClick: () -> Unit,
    onSensorCheckClick: () -> Unit,
    onPermissionsClick: () -> Unit = {}
) {
    val palette = LocalAppUiPalette.current
    val mode = LocalAppUiThemeMode.current
    val setMode = LocalSetAppUiThemeMode.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Text(
            text = "프로필",
            color = palette.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

        Text(
            text = "테마",
            color = palette.onBackgroundMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 4.dp)
        )
        Text(
            text = "시스템 다크 모드와 무관하게 이 앱에만 적용됩니다.",
            color = palette.onBackgroundMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp)
        )
        ThemeSettingsOptionRow(
            title = "화이트 모드",
            subtitle = "밝은 배경·카드형 표면·짙은 텍스트",
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
            subtitle = "검은 배경·라임 포인트 컬러에 가깝게",
            selected = mode == AppUiThemeMode.DARK,
            onClick = { setMode(AppUiThemeMode.DARK) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )

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
                val context = androidx.compose.ui.platform.LocalContext.current
                val allGranted = AppPermissions.list(context).all { it.isGranted(context) }
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )
    }
}

@Composable
fun ThemeSettingsOptionRow(
    title: String,
    subtitle: String,
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = palette.onBackgroundMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
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
