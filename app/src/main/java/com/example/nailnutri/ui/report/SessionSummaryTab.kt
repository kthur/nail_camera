package com.example.nailnutri.ui.report

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nailnutri.data.*
import com.example.nailnutri.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SessionHeaderCard(session: SessionReport, report: HolisticHealthReport) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    session.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "PVI ${report.vitalityIndex}점",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                session.createdAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "포함된 측정: ${session.resultIds.size}건 • 평가 상태: ${report.vitalityGrade}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
internal fun VitalityGaugeCard(report: HolisticHealthReport) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "통합 생체 활력 지수 (PVI)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        report.vitalityIndex >= 85 -> NutriGreen.copy(alpha = 0.15f)
                        report.vitalityIndex >= 70 -> NutriTeal.copy(alpha = 0.15f)
                        report.vitalityIndex >= 50 -> NutriAmber.copy(alpha = 0.15f)
                        else -> NutriCoral.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        report.vitalityGrade,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            report.vitalityIndex >= 85 -> NutriGreen
                            report.vitalityIndex >= 70 -> NutriTeal
                            report.vitalityIndex >= 50 -> NutriAmber
                            else -> NutriCoral
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                    val gaugeColor = when {
                        report.vitalityIndex >= 85 -> NutriGreen
                        report.vitalityIndex >= 70 -> NutriTeal
                        report.vitalityIndex >= 50 -> NutriAmber
                        else -> NutriCoral
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = gaugeColor.copy(alpha = 0.15f),
                            style = Stroke(width = 10.dp.toPx())
                        )
                        drawArc(
                            color = gaugeColor,
                            startAngle = -90f,
                            sweepAngle = 360f * (report.vitalityIndex / 100f),
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${report.vitalityIndex}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = gaugeColor
                        )
                        Text(
                            "/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.primaryClinicalAdvice,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PillarRadarChart(
    report: HolisticHealthReport,
    modifier: Modifier = Modifier
) {
    val pillars = remember {
        listOf(
            HealthPillar.HEMATOLOGIC,
            HealthPillar.AUTONOMIC,
            HealthPillar.KERATIN_TISSUE,
            HealthPillar.RESPIRATORY_SLEEP,
            HealthPillar.METABOLIC_IMMUNE
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
    val textPaintColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val maxRadius = minOf(size.width, size.height) / 2f * 0.65f
                val n = pillars.size
                val angleStep = (2 * Math.PI / n).toFloat()
                val startAngle = (-Math.PI / 2).toFloat()

                // 1. Concentric Grid Polygons (20%, 40%, 60%, 80%, 100%)
                val ringFractions = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
                for (fraction in ringFractions) {
                    val r = maxRadius * fraction
                    val ringPath = Path()
                    for (i in 0 until n) {
                        val angle = startAngle + i * angleStep
                        val x = centerX + r * cos(angle)
                        val y = centerY + r * sin(angle)
                        if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                    }
                    ringPath.close()
                    drawPath(
                        path = ringPath,
                        color = gridColor,
                        style = Stroke(width = if (fraction == 1.0f) 1.5.dp.toPx() else 1.dp.toPx())
                    )
                }

                // 2. Radial Axis Lines
                for (i in 0 until n) {
                    val angle = startAngle + i * angleStep
                    val endX = centerX + maxRadius * cos(angle)
                    val endY = centerY + maxRadius * sin(angle)
                    drawLine(
                        color = axisColor,
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 3. User Data Polygon
                val dataPath = Path()
                val points = mutableListOf<Offset>()
                for (i in 0 until n) {
                    val pillar = pillars[i]
                    val score = report.pillarScores[pillar]?.score ?: 100
                    val r = maxRadius * (score / 100f).coerceIn(0.15f, 1.0f)
                    val angle = startAngle + i * angleStep
                    val pt = Offset(centerX + r * cos(angle), centerY + r * sin(angle))
                    points.add(pt)
                    if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
                }
                dataPath.close()

                // Draw filled polygon with gradient
                drawPath(
                    path = dataPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.40f),
                            primaryColor.copy(alpha = 0.15f)
                        ),
                        center = Offset(centerX, centerY),
                        radius = maxRadius
                    ),
                    style = Fill
                )
                // Draw perimeter
                drawPath(
                    path = dataPath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 4. Vertex dots & Axis Labels
                val labelPaint = Paint().apply {
                    color = textPaintColor
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }

                for (i in 0 until n) {
                    val pt = points[i]
                    // Point glow
                    drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)

                    // Text label outside maxRadius
                    val pillar = pillars[i]
                    val angle = startAngle + i * angleStep
                    val labelRadius = maxRadius + 32.dp.toPx()
                    val labelX = centerX + labelRadius * cos(angle)
                    val labelY = centerY + labelRadius * sin(angle) + 10f

                    val shortName = when (pillar) {
                        HealthPillar.HEMATOLOGIC -> "조혈"
                        HealthPillar.AUTONOMIC -> "자율신경"
                        HealthPillar.KERATIN_TISSUE -> "조갑기질"
                        HealthPillar.RESPIRATORY_SLEEP -> "수면호흡"
                        HealthPillar.METABOLIC_IMMUNE -> "대사면역"
                    }
                    val score = report.pillarScores[pillar]?.score ?: 100
                    drawContext.canvas.nativeCanvas.drawText(
                        "$shortName $score",
                        labelX,
                        labelY,
                        labelPaint
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pillar Details Table
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pillars.forEach { pillar ->
                val pillarScore = report.pillarScores[pillar]
                val score = pillarScore?.score ?: 100
                val status = pillarScore?.status ?: "최적"
                val statusColor = when (status) {
                    "최적" -> NutriGreen
                    "주의" -> NutriAmber
                    else -> NutriCoral
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                pillar.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                pillar.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (!pillarScore?.contributingSymptoms.isNullOrEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "관련 징후: " + pillarScore.contributingSymptoms.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$score 점",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun MedicalDisclaimerCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "📋 의학적 안내 및 주의사항",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "본 리포트의 활력 지수 및 영양 가이드는 스마트폰 카메라, PPG 맥파, 수면 음향 및 음성 센서 분석 결과를 바탕으로 산출된 웰니스 최적화 참고 정보입니다. 특정 질병의 임상적 확진이나 의사의 처방을 대체하지 않으므로, 이상 징후가 지속될 경우 전문 의료기관을 방문하시기 바랍니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun DeficiencyCard(deficiencies: List<String>) {
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
internal fun SessionResultItem(result: NailAnalysisResult) {
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

