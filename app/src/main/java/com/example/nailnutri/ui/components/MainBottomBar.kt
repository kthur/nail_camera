package com.example.nailnutri.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.nailnutri.Home
import com.example.nailnutri.History
import com.example.nailnutri.SensorDashboard
import com.example.nailnutri.Settings

sealed class MainTab(
    val route: NavKey,
    val title: String,
    val icon: ImageVector
) {
    object HomeTab : MainTab(Home, "홈", Icons.Default.Home)
    object ScanSuiteTab : MainTab(SensorDashboard, "진단 스위트", Icons.Default.MedicalServices)
    object HistoryTab : MainTab(History, "기록 & 리포트", Icons.Default.BarChart)
    object SettingsTab : MainTab(Settings, "설정", Icons.Default.Settings)

    companion object {
        val tabs = listOf(HomeTab, ScanSuiteTab, HistoryTab, SettingsTab)
    }
}

@Composable
fun MainBottomBar(
    currentRoute: NavKey,
    onTabSelected: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show bottom bar for main top-level tab routes
    val isMainTab = MainTab.tabs.any { it.route::class == currentRoute::class }

    AnimatedVisibility(
        visible = isMainTab,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            MainTab.tabs.forEach { tab ->
                val selected = currentRoute::class == tab.route::class
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            onTabSelected(tab.route)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}
