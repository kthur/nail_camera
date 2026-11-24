package com.example.nailnutri.data

import kotlinx.serialization.Serializable

@Serializable
enum class HealthPillar(val displayName: String, val description: String) {
    HEMATOLOGIC("조혈 및 산소운반", "헤모글로빈, 결막 혈색, 적혈구 산소 공급 능력"),
    AUTONOMIC("자율신경 및 심혈관", "HRV 심박변이도, 교감-부교감 신경 균형, 스트레스 지수"),
    KERATIN_TISSUE("조갑 및 케라틴 기질", "손톱 표면 요철, 세로줄/가로홈, 케라틴 단백질 합성"),
    RESPIRATORY_SLEEP("수면 호흡 및 회복", "수면 중 상기도 안정성, 코골이 저주파 에너지, 야간 회복"),
    METABOLIC_IMMUNE("대사 항상성 및 면역", "진균 감염 방어, 구강 점막 재생, 미세 비타민 코엔자임")
}

@Serializable
data class PillarScore(
    val pillar: HealthPillar,
    val score: Int, // 15..100
    val status: String, // "최적", "주의", "위험"
    val contributingSymptoms: List<String>,
    val targetNutrients: List<String>
)

@Serializable
data class CrossModalSyndrome(
    val id: String,
    val title: String,
    val severity: String, // "High", "Moderate", "Watch"
    val physiologicalMechanism: String,
    val biomarkerEvidence: List<String>,
    val clinicalActionPlan: String
)

@Serializable
data class ChronoNutrientPlan(
    val timeSlot: String, // e.g. "🌅 아침 공복 (07:00 ~ 08:00)"
    val nutrientName: String,
    val primaryFoods: List<String>,
    val synergyTip: String,
    val antagonismWarning: String? = null
)

@Serializable
data class HolisticHealthReport(
    val sessionId: String,
    val calculatedAt: String,
    val vitalityIndex: Int, // 10..100 composite score
    val vitalityGrade: String, // "최상 (Optimal Vitality)", "양호 (Good Condition)", "관리 필요 (Caution Required)", "집중 개선 요망 (Critical Attention)"
    val pillarScores: Map<HealthPillar, PillarScore>,
    val detectedSyndromes: List<CrossModalSyndrome>,
    val chronoSchedule: List<ChronoNutrientPlan>,
    val primaryClinicalAdvice: String
)
