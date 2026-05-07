package com.example.app_01

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val palette = LocalAppUiPalette.current
    val aiNavPainter = painterResource(R.drawable.ic_bottom_nav_ai)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bottomBarBg)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "라이브러리",
                icon = Icons.Outlined.PhotoLibrary,
                isSelected = selectedTab == MainTab.LIBRARY,
                onClick = { onTabSelected(MainTab.LIBRARY) }
            )
            BottomNavItem(
                label = "AI",
                painter = aiNavPainter,
                isSelected = selectedTab == MainTab.CLAUDE,
                onClick = { onTabSelected(MainTab.CLAUDE) }
            )
            BottomNavItem(
                label = "카메라",
                icon = Icons.Outlined.CameraAlt,
                isSelected = selectedTab == MainTab.CAMERA,
                onClick = { onTabSelected(MainTab.CAMERA) }
            )
            BottomNavItem(
                label = "창작마당",
                icon = Icons.Outlined.Public,
                isSelected = selectedTab == MainTab.CREATE,
                onClick = { onTabSelected(MainTab.CREATE) }
            )
            BottomNavItem(
                label = "프로필",
                icon = Icons.Outlined.Person,
                isSelected = selectedTab == MainTab.PROFILE,
                onClick = { onTabSelected(MainTab.PROFILE) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    BottomNavItem(label, isSelected, onClick) { contentColor ->
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun BottomNavItem(
    label: String,
    painter: Painter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    BottomNavItem(label, isSelected, onClick) { contentColor ->
        Icon(
            painter = painter,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (contentColor: Color) -> Unit
) {
    val palette = LocalAppUiPalette.current
    val contentColor =
        if (isSelected) palette.bottomNavContent else palette.bottomNavContentMuted
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(contentColor)
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp
        )
    }
}
