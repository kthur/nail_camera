package com.example.nailnutri.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.nailnutri.*
import androidx.compose.material.icons.filled.Assessment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDashboardScreen(
    onNavigate: (NavKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("센서 자가 진단 스위트", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기")
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
                .background(Color(0xFF0F172A))
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "디바이스의 카메라, 플래시, 마이크 하드웨어 센서를 활용해 신체 영양 및 긴장 징후를 판독합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            SensorCard(
                title = "안구 결막 빈혈 진단 (철분 결핍)",
                description = "눈 아래 결막의 붉은 혈색 채도 비율을 정규화 스크리닝하여 철결핍성 빈혈 위험을 판독합니다.",
                icon = Icons.Default.Face,
                iconTint = Color(0xFFF87171),
                onClick = { onNavigate(AnemiaScan) }
            )

            SensorCard(
                title = "실시간 PPG 미네랄 측정 (마그네슘/칼슘)",
                description = "카메라와 플래시에 손가락을 밀착하여 심박변이도(HRV) 파형을 판독하고 스트레스 지수를 측정합니다.",
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFF34D399),
                onClick = { onNavigate(PpgScan) }
            )

            SensorCard(
                title = "LFA 시약 키트 판독 (비타민 D/B12)",
                description = "소변 진단 키트의 발색 색조 변화 강도를 촬영해 영양소 수치 농도를 정량 측정합니다.",
                icon = Icons.Default.CameraAlt,
                iconTint = Color(0xFF60A5FA),
                onClick = { onNavigate(LfaScan) }
            )

            SensorCard(
                title = "코골이 수면 소리 수집 (비타민 D/마그네슘)",
                description = "수면 중 호흡음 저주파 대역 점유 에너지를 판독해 근육 피로 및 비타민 D 결핍 징후를 예방합니다.",
                icon = Icons.Default.NightlightRound,
                iconTint = Color(0xFFC084FC),
                onClick = { onNavigate(SleepScan) }
            )

            SensorCard(
                title = "목소리 3초 피로도 진단 (비타민 B군)",
                description = "3초간의 발성 주파수 변동(Jitter)과 크기 흔들림(Shimmer)을 모니터링해 신경계 피로를 추적합니다.",
                icon = Icons.Default.Mic,
                iconTint = Color(0xFFFBBF24),
                onClick = { onNavigate(VoiceScan) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            SensorCard(
                title = "📊 종합 세션 리포트",
                description = "여러 센서 측정 결과를 하나로 묶어 영양 건강 점수와 주요 결핍 영양소를 종합 분석하고 공유합니다.",
                icon = Icons.Default.Assessment,
                iconTint = Color(0xFF818CF8),
                onClick = { onNavigate(SessionListNavKey) }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SensorCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .size(46.dp)
                    .clip(CircleShape),
                color = iconTint.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
