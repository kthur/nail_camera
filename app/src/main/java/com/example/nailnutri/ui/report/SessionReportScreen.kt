package com.example.nailnutri.ui.report

import com.example.nailnutri.util.DateUtils

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.analysis.HolisticBiomarkerEngine
import com.example.nailnutri.data.*
import com.example.nailnutri.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

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
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    if (session == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("세션을 찾을 수 없습니다.", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val includedResults = remember(history, session.resultIds) {
        history.filter { it.id in session.resultIds }
    }

    val holisticReport = remember(session, includedResults) {
        HolisticBiomarkerEngine.evaluate(session, includedResults)
    }

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
                        val text = buildShareText(session, includedResults, holisticReport)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, "NailNutri 종합 영양 및 생체 리포트")
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
            item { SessionHeaderCard(session, holisticReport) }

            // 탭 선택기 (생체 종합 분석 vs 센서 기록)
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("생체 종합 분석", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("센서 측정 기록 (${includedResults.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // 1. 활력 지수 게이지 카드
                item { VitalityGaugeCard(holisticReport) }

                // 2. 5대 생체 항상성 레이더 차트
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "5대 생체 항상성 레이더 (Biomarker Radar)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            PillarRadarChart(report = holisticReport)
                        }
                    }
                }

                // 3. 복합 생체 증후군 경보
                if (holisticReport.detectedSyndromes.isNotEmpty()) {
                    item {
                        Text(
                            "🚨 복합 생체 증후군 경보 (${holisticReport.detectedSyndromes.size}건)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(holisticReport.detectedSyndromes, key = { it.id }) { syndrome ->
                        SyndromeCard(syndrome)
                    }
                }

                // 4. 맞춤형 시간영양 섭취 계획
                if (holisticReport.chronoSchedule.isNotEmpty()) {
                    item {
                        Text(
                            "⏰ 맞춤형 시간영양학 섭취 가이드",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(holisticReport.chronoSchedule, key = { it.timeSlot + it.nutrientName }) { plan ->
                        ChronoPlanCard(plan)
                    }
                }

                // 5. 의료 면책 조항
                item { MedicalDisclaimerCard() }

            } else {
                // Tab 1: 개별 센서 분석 결과 목록 (기존 기능 100% 유지)
                if (session.topDeficiencies.isNotEmpty()) {
                    item {
                        DeficiencyCard(session.topDeficiencies)
                    }
                }

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

// ─────────────────────────────────────────────
//  컴포넌트 구현
// ─────────────────────────────────────────────

private fun buildShareText(
    session: SessionReport,
    results: List<NailAnalysisResult>,
    holisticReport: HolisticHealthReport
): String {
    val sb = StringBuilder()
    sb.appendLine("📊 NailNutri 통합 생체 활력 및 영양 분석 리포트")
    sb.appendLine("세션: ${session.label}")
    sb.appendLine("일시: ${session.createdAt}")
    sb.appendLine("통합 활력 지수(PVI): ${holisticReport.vitalityIndex}/100 (${holisticReport.vitalityGrade})")
    sb.appendLine()

    sb.appendLine("🧭 5대 생체 항상성 기둥 점수:")
    holisticReport.pillarScores.forEach { (pillar, score) ->
        sb.appendLine("  • ${pillar.displayName}: ${score.score}점 (${score.status})")
    }
    sb.appendLine()

    if (holisticReport.detectedSyndromes.isNotEmpty()) {
        sb.appendLine("🚨 감지된 복합 생체 증후군 (${holisticReport.detectedSyndromes.size}건):")
        holisticReport.detectedSyndromes.forEach { syn ->
            sb.appendLine("  [${syn.severity}] ${syn.title}")
            sb.appendLine("   - 기전: ${syn.physiologicalMechanism}")
            sb.appendLine("   - 조치: ${syn.clinicalActionPlan}")
        }
        sb.appendLine()
    }

    if (holisticReport.chronoSchedule.isNotEmpty()) {
        sb.appendLine("⏰ 맞춤형 시간영양 섭취 플랜:")
        holisticReport.chronoSchedule.forEach { plan ->
            sb.appendLine("  • ${plan.timeSlot} - ${plan.nutrientName}")
            sb.appendLine("    시너지: ${plan.synergyTip}")
            if (plan.antagonismWarning != null) {
                sb.appendLine("    ${plan.antagonismWarning}")
            }
        }
        sb.appendLine()
    }

    sb.appendLine("📋 포함된 센서 측정 결과 (${results.size}건):")
    results.forEach { result ->
        sb.appendLine("• ${result.symptoms.firstOrNull() ?: "정상"} (${result.date})")
        if (result.deficientNutrients.isNotEmpty()) {
            sb.appendLine("  결핍: " + result.deficientNutrients.joinToString { it.name })
        }
    }
    sb.appendLine()
    sb.appendLine("* 본 리포트는 스마트폰 멀티 바이오마커 센서 기반 참고 정보입니다. 정확한 진단은 전문의와 상담하세요.")
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
                    val recentResults = history.take(5)
                    if (recentResults.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar("분석 기록이 없어 세션을 만들 수 없습니다.") }
                        return@ExtendedFloatingActionButton
                    }
                    val allDeficiencies = recentResults
                        .flatMap { r -> r.deficientNutrients }
                        .groupBy { n -> n.name }
                        .entries
                        .sortedByDescending { entry -> entry.value.size }
                        .take(3)
                        .map { entry -> entry.key }
                    val totalDeficits = recentResults.sumOf { r -> r.deficientNutrients.size }
                    val score = (100 - (totalDeficits * 12).coerceAtMost(80)).coerceAtLeast(10)
                    val label = DateUtils.nowKoreanSessionLabel()
                    val report = SessionReport(
                        id = UUID.randomUUID().toString(),
                        createdAt = DateUtils.nowFormatted(),
                        label = label,
                        resultIds = recentResults.map { r -> r.id },
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
                items(sessions, key = { s -> s.id }) { s ->
                    SessionListItem(s, onClick = { onSessionClick(s.id) })
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
