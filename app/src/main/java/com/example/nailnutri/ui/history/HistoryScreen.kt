package com.example.nailnutri.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.SessionReport
import com.example.nailnutri.theme.NutriAmber
import com.example.nailnutri.theme.NutriCoral
import com.example.nailnutri.theme.NutriGreen
import com.example.nailnutri.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onResultClick: (String) -> Unit,
    onSessionClick: (String) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.history.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val sortedHistoryList = remember(historyList) { historyList.sortedByDescending { it.date } }
    
    var selectedTab by remember { mutableStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기록 & 리포트 센터", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (selectedTab == 0 && sortedHistoryList.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "전체 삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("스캔 히스토리", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("종합 세션 리포트", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Tab 0: Scan History List & Trend Chart
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TrendChartCard(historyList = sortedHistoryList)
                    }
                    
                    if (sortedHistoryList.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.6f)
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "저장된 스캔 기록이 없습니다.",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "첫 손톱 또는 센서 분석을 완료하면 이곳에 자동으로 기록됩니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(sortedHistoryList, key = { it.id }) { result ->
                            HistoryItemCard(
                                result = result,
                                onClick = { onResultClick(result.id) },
                                onDelete = {
                                    viewModel.deleteResult(result.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // Tab 1: Comprehensive Session Reports
                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "저장된 종합 세션이 없습니다.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "센서 진단 스위트에서 여러 스캔을 묶어\n종합 리포트를 생성할 수 있습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessions, key = { s -> s.id }) { s ->
                            Card(
                                onClick = { onSessionClick(s.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${s.overallScore}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(s.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        if (s.topDeficiencies.isNotEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "주요 결핍: " + s.topDeficiencies.joinToString(", "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NutriCoral
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confirm clear dialog
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("기록 전체 삭제") },
                    text = { Text("정말로 모든 분석 기록을 삭제하시겠습니까? 삭제된 기록은 복구할 수 없습니다.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearHistory()
                                showClearDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("삭제")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("취소")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendChartCard(
    historyList: List<NailAnalysisResult>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "영양소 결핍 위험 트렌드",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "기록 없음",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                val nutrients = listOf("철분", "비타민D", "마그네슘", "칼슘", "비타민B군")
                val nutrientColors = listOf(
                    Color(0xFFF43F5E), // 철분: Coral
                    Color(0xFFF59E0B), // 비타민D: Amber
                    Color(0xFF0EA5E9), // 마그네슘: Blue
                    Color(0xFF8B5CF6), // 칼슘: Purple
                    Color(0xFFEC4899)  // 비타민B군: Pink
                )

                // Chronological order (oldest to newest)
                val chronologicalList = remember(historyList) { historyList.reversed() }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val paddingLeft = 100f
                    val paddingRight = 40f
                    val paddingTop = 20f
                    val paddingBottom = 40f
                    
                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom
                    
                    // Draw Y-axis guide lines & labels
                    val yLevels = listOf(0f, 1f, 2f)
                    val yLabels = listOf("양호(0)", "의심(1)", "주의(2)")
                    
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    
                    yLevels.forEachIndexed { index, level ->
                        val y = paddingTop + chartHeight * (1f - level / 2f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                            end = androidx.compose.ui.geometry.Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            yLabels[index],
                            paddingLeft - 15f,
                            y + 8f,
                            paint
                        )
                    }

                    // Draw X-axis points and lines
                    val pointsCount = chronologicalList.size
                    val xCoords = FloatArray(pointsCount)
                    chronologicalList.forEachIndexed { index, _ ->
                        xCoords[index] = if (pointsCount > 1) {
                            paddingLeft + chartWidth * (index.toFloat() / (pointsCount - 1))
                        } else {
                            paddingLeft + chartWidth / 2f
                        }
                    }

                    val textPaintX = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    
                    if (pointsCount > 0) {
                        val firstDate = chronologicalList.first().date.split(" ").firstOrNull() ?: ""
                        val lastDate = chronologicalList.last().date.split(" ").firstOrNull() ?: ""
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            firstDate,
                            xCoords.first(),
                            height - 10f,
                            textPaintX
                        )
                        if (pointsCount > 1) {
                            drawContext.canvas.nativeCanvas.drawText(
                                lastDate,
                                xCoords.last(),
                                height - 10f,
                                textPaintX
                            )
                        }
                    }

                    // Now draw trend line for each nutrient
                    nutrients.forEachIndexed { nutrientIndex, nutrientName ->
                        val color = nutrientColors[nutrientIndex]
                        val path = androidx.compose.ui.graphics.Path()
                        
                        chronologicalList.forEachIndexed { pointIndex, result ->
                            val level = getSeverityLevel(result, nutrientName).toFloat()
                            val x = xCoords[pointIndex]
                            val y = paddingTop + chartHeight * (1f - level / 2f)
                            
                            if (pointIndex == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            
                            drawCircle(
                                color = color,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                        
                        if (pointsCount > 1) {
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Legend
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nutrients.forEachIndexed { index, nutrient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(nutrientColors[index], shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = nutrient,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getSeverityLevel(result: NailAnalysisResult, target: String): Int {
    val matched = result.deficientNutrients.find { item ->
        val name = item.name.replace(" ", "")
        when (target) {
            "철분" -> name.contains("철분") || name.contains("철")
            "비타민D" -> name.contains("비타민D") || (name.contains("비타민") && name.contains("D"))
            "비타민B군" -> name.contains("비타민B") || (name.contains("비타민") && name.contains("B"))
            "마그네슘" -> name.contains("마그네슘")
            "칼슘" -> name.contains("칼슘")
            else -> false
        }
    }
    return when (matched?.severity) {
        "Severe" -> 2
        "Moderate" -> 1
        else -> 0
    }
}

@Composable
fun NutrientBadge(name: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = name,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryItemCard(
    result: NailAnalysisResult,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.symptoms.firstOrNull() ?: "건강한 손톱",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Badges representing deficient and sufficient nutrients
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    result.deficientNutrients.forEach { nutrient ->
                        val badgeColor = when (nutrient.severity) {
                            "Severe" -> NutriCoral
                            "Moderate" -> NutriAmber
                            else -> NutriAmber
                        }
                        NutrientBadge(name = nutrient.name, color = badgeColor)
                    }
                    result.sufficientNutrients.forEach { nutrient ->
                        NutrientBadge(name = nutrient.name, color = NutriGreen)
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
