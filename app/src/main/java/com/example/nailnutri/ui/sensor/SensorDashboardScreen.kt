package com.example.nailnutri.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nailnutri.*

enum class SensorCategory(val label: String) {
    ALL("전체"),
    VISION("카메라 & 비전"),
    BIO("바이오 센서"),
    VOICE("음성 & 오디오")
}

data class SensorItem(
    val title: String,
    val description: String,
    val category: SensorCategory,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeText: String,
    val route: NavKey
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDashboardScreen(
    onNavigate: (NavKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedCategory by remember { mutableStateOf(SensorCategory.ALL) }

    val sensorItems = remember {
        listOf(
            SensorItem(
                title = "안구 결막 빈혈 진단 (철분 결핍)",
                description = "눈 아래 결막의 붉은 혈색 채도 비율을 정규화 스크리닝하여 철결핍성 빈혈 위험을 판독합니다.",
                category = SensorCategory.VISION,
                icon = Icons.Default.Face,
                iconTint = Color(0xFFF87171),
                badgeText = "철분/빈혈",
                route = AnemiaScan
            ),
            SensorItem(
                title = "손톱 AI 픽셀 카메라 스캔",
                description = "손톱 표면 변색, 세로줄, 표면 요철 및 스푼 모양 정밀 픽셀 분석",
                category = SensorCategory.VISION,
                icon = Icons.Default.Videocam,
                iconTint = Color(0xFF38BDF8),
                badgeText = "비타민/아연",
                route = CameraScan
            ),
            SensorItem(
                title = "LFA 시약 키트 판독 (비타민 D/B12)",
                description = "소변 진단 키트의 발색 색조 변화 강도를 촬영해 영양소 수치 농도를 정량 측정합니다.",
                category = SensorCategory.VISION,
                icon = Icons.Default.CameraAlt,
                iconTint = Color(0xFF60A5FA),
                badgeText = "정밀 키트",
                route = LfaScan
            ),
            SensorItem(
                title = "실시간 PPG 미네랄 측정 (마그네슘/칼슘)",
                description = "카메라와 플래시에 손가락을 밀착하여 심박변이도(HRV) 파형을 판독하고 스트레스 지수를 측정합니다.",
                category = SensorCategory.BIO,
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFF34D399),
                badgeText = "마그네슘/스트레스",
                route = PpgScan
            ),
            SensorItem(
                title = "음성 증상 영양 진단 (AI 파싱)",
                description = "눈밑 떨림, 피로, 구강 헐음 등 말로 설명하는 신체 증상을 AI가 파싱하여 영양소 추정",
                category = SensorCategory.VOICE,
                icon = Icons.Default.Mic,
                iconTint = Color(0xFFFBBF24),
                badgeText = "음성 AI",
                route = VoiceScan
            ),
            SensorItem(
                title = "목소리 3초 피로도 진단 (비타민 B군)",
                description = "3초간의 발성 주파수 변동(Jitter)과 크기 흔들림(Shimmer)을 모니터링해 신경계 피로를 추적합니다.",
                category = SensorCategory.VOICE,
                icon = Icons.Default.Mic,
                iconTint = Color(0xFFF472B6),
                badgeText = "신경피로",
                route = VoiceScan
            ),
            SensorItem(
                title = "코골이 수면 소리 수집 (비타민 D/마그네슘)",
                description = "수면 중 호흡음 저주파 대역 점유 에너지를 판독해 근육 피로 및 비타민 D 결핍 징후를 예방합니다.",
                category = SensorCategory.VOICE,
                icon = Icons.Default.NightlightRound,
                iconTint = Color(0xFFC084FC),
                badgeText = "수면 호흡",
                route = SleepScan
            )
        )
    }

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == SensorCategory.ALL) sensorItems
        else sensorItems.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("정밀 자가 진단 스위트", fontWeight = FontWeight.Bold)
                        Text(
                            "카메라 · 바이오 · 오디오 하드웨어 센서 기반",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SensorCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Sensor Cards
            filteredItems.forEach { item ->
                SensorCardItem(
                    item = item,
                    onClick = { onNavigate(item.route) }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Comprehensive Report Entry Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(SessionListNavKey) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📊 종합 세션 리포트 관리",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "여러 센서 측정 결과를 하나로 묶어 영양 건강 점수와 결핍 영양소를 종합 분석 및 공유합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SensorCardItem(
    item: SensorItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                color = item.iconTint.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = item.iconTint.copy(alpha = 0.12f),
                        contentColor = item.iconTint,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

