package com.example.nailnutri.ui.report

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.data.DataRepository
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.SessionReport
import com.example.nailnutri.theme.NutriAmber
import com.example.nailnutri.theme.NutriCoral
import com.example.nailnutri.theme.NutriGreen
import com.example.nailnutri.theme.NutriTeal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────
//  세션 리포트 화면
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionReportScreen(
    sessionId: String,
    repository: DataRepository,
    onBackClick: () -> Unit,
    onDeleteSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by repository.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repository.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val session = sessions.find { it.id == sessionId }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (session == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("세션을 찾을 수 없습니다.", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val includedResults = history.filter { it.id in session.resultIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("종합 세션 리포트", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 텍스트 공유
                        val text = buildShareText(session, includedResults)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, "NailNutri 영양 분석 리포트")
                        }
                        context.startActivity(Intent.createChooser(intent, "리포트 공유"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "공유")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 헤더 카드
            item { SessionHeaderCard(session) }

            // 건강 점수 게이지
            item { HealthScoreCard(session.overallScore) }

            // 상위 결핍 영양소
            if (session.topDeficiencies.isNotEmpty()) {
                item {
                    DeficiencyCard(session.topDeficiencies)
                }
            }

            // 포함된 결과 목록
            item {
                Text(
                    "포함된 센서 분석 결과 (${includedResults.size}건)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (includedResults.isEmpty()) {
                item {
                    Text(
                        "연결된 분석 기록이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(includedResults, key = { it.id }) { result ->
                    SessionResultItem(result)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("세션 삭제") },
            text = { Text("이 세션 리포트를 삭제하시겠습니까? 개별 분석 기록은 유지됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteSession(session.id)
                            showDeleteDialog = false
                            onDeleteSession()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun SessionHeaderCard(session: SessionReport) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                session.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                session.createdAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "포함된 측정: ${session.resultIds.size}건",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HealthScoreCard(score: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "종합 건강 점수",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 원형 점수 게이지
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    val gaugeColor = when {
                        score >= 80 -> NutriGreen
                        score >= 50 -> NutriAmber
                        else -> NutriCoral
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 배경 원
                        drawCircle(color = gaugeColor.copy(alpha = 0.15f), style = Stroke(width = 10.dp.toPx()))
                        // 점수 호
                        drawArc(
                            color = gaugeColor,
                            startAngle = -90f,
                            sweepAngle = 360f * (score / 100f),
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "$score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor
                    )
                }
                Column {
                    val label = when {
                        score >= 80 -> "양호"
                        score >= 60 -> "보통"
                        score >= 40 -> "주의"
                        else -> "위험"
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            score >= 80 -> NutriGreen
                            score >= 60 -> NutriAmber
                            else -> NutriCoral
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "결핍 영양소 수와 심각도를 기반으로\n산출된 종합 점수입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DeficiencyCard(deficiencies: List<String>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NutriCoral.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "⚠️ 주요 결핍 영양소",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NutriCoral
            )
            Spacer(Modifier.height(8.dp))
            deficiencies.forEachIndexed { idx, name ->
                Text(
                    "${idx + 1}. $name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionResultItem(result: NailAnalysisResult) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.symptoms.firstOrNull() ?: "건강한 상태",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    result.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (result.deficientNutrients.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "결핍: " + result.deficientNutrients.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = NutriCoral,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun buildShareText(session: SessionReport, results: List<NailAnalysisResult>): String {
    val sb = StringBuilder()
    sb.appendLine("📊 NailNutri 종합 영양 분석 리포트")
    sb.appendLine("세션: ${session.label}")
    sb.appendLine("날짜: ${session.createdAt}")
    sb.appendLine("건강 점수: ${session.overallScore}/100")
    sb.appendLine()
    if (session.topDeficiencies.isNotEmpty()) {
        sb.appendLine("⚠️ 주요 결핍 영양소:")
        session.topDeficiencies.forEachIndexed { i, name -> sb.appendLine("  ${i + 1}. $name") }
        sb.appendLine()
    }
    sb.appendLine("📋 세부 분석 결과:")
    results.forEach { result ->
        sb.appendLine("• ${result.symptoms.firstOrNull() ?: "정상"} (${result.date})")
        if (result.deficientNutrients.isNotEmpty()) {
            sb.appendLine("  결핍: " + result.deficientNutrients.joinToString { it.name })
        }
    }
    sb.appendLine()
    sb.appendLine("* 이 리포트는 스마트폰 센서 기반 참고 정보입니다. 정확한 진단은 의사와 상담하세요.")
    return sb.toString()
}

// ─────────────────────────────────────────────
//  세션 대시보드 화면 (세션 목록 + 생성)
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    repository: DataRepository,
    onSessionClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by repository.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repository.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("세션 리포트", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // 최근 7일 결과를 모아 새 세션 생성
                    val recentResults = history.take(5)
                    if (recentResults.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar("분석 기록이 없어 세션을 만들 수 없습니다.") }
                        return@ExtendedFloatingActionButton
                    }
                    val allDeficiencies = recentResults
                        .flatMap { it.deficientNutrients }
                        .groupBy { it.name }
                        .entries
                        .sortedByDescending { it.value.size }
                        .take(3)
                        .map { it.key }
                    val totalDeficits = recentResults.sumOf { it.deficientNutrients.size }
                    val score = (100 - (totalDeficits * 12).coerceAtMost(80)).coerceAtLeast(10)
                    val label = SimpleDateFormat("M월 d일 세션", Locale.KOREAN).format(Date())
                    val report = SessionReport(
                        id = UUID.randomUUID().toString(),
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                        label = label,
                        resultIds = recentResults.map { it.id },
                        topDeficiencies = allDeficiencies,
                        overallScore = score
                    )
                    scope.launch {
                        repository.saveSession(report)
                        snackbarHostState.showSnackbar("세션 리포트가 생성되었습니다!")
                    }
                },
                icon = { Icon(Icons.Default.Share, contentDescription = null) },
                text = { Text("새 세션 생성") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "저장된 세션이 없습니다.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "'새 세션 생성' 버튼으로 최근 분석 결과를\n종합 리포트로 묶어 보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionListItem(session, onClick = { onSessionClick(session.id) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SessionListItem(session: SessionReport, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 점수 원
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                val gaugeColor = when {
                    session.overallScore >= 80 -> NutriGreen
                    session.overallScore >= 50 -> NutriAmber
                    else -> NutriCoral
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = gaugeColor.copy(alpha = 0.15f), style = Stroke(width = 6.dp.toPx()))
                    drawArc(
                        color = gaugeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (session.overallScore / 100f),
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    "${session.overallScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(session.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(session.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                if (session.topDeficiencies.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "주요 결핍: " + session.topDeficiencies.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = NutriCoral
                    )
                }
            }
        }
    }
}
