package com.example.nailnutri.analysis

import com.example.nailnutri.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified Multi-Biomarker Holistic Health Engine
 *
 * Synthesizes diagnostic signals across 6 biometric modalities (Nail vision, Conjunctiva,
 * PPG pulse, LFA kit, Sleep audio, and Voice fatigue) into 5 physiological health pillars,
 * detects cross-modal clinical syndromes, and generates circadian chrono-nutrition schedules.
 */
object HolisticBiomarkerEngine {

    fun evaluate(session: SessionReport, results: List<NailAnalysisResult>): HolisticHealthReport {
        if (results.isEmpty()) {
            return buildDefaultOptimalReport(session.id)
        }

        // 1. Compute 5-Pillar Vitality Scores
        val pillarScores = HealthPillar.entries.associateWith { pillar ->
            computePillarScore(pillar, results)
        }

        // 2. Detect Cross-Modal Clinical Syndromes
        val syndromes = detectSyndromes(pillarScores, results)

        // 3. Generate Chrono-Nutrition Schedule
        val chronoSchedule = generateChronoSchedule(results)

        // 4. Calculate Composite Vitality Index (PVI)
        val avgPillarScore = pillarScores.values.map { it.score }.average().toInt()
        val syndromePenalty = (syndromes.size * 8).coerceAtMost(24)
        val vitalityIndex = (avgPillarScore - syndromePenalty).coerceIn(10, 100)

        val vitalityGrade = when {
            vitalityIndex >= 85 -> "최상 (Optimal Vitality)"
            vitalityIndex >= 70 -> "양호 (Good Condition)"
            vitalityIndex >= 50 -> "관리 필요 (Caution Required)"
            else -> "집중 개선 요망 (Critical Attention)"
        }

        val primaryAdvice = when {
            syndromes.isNotEmpty() -> {
                "총 ${syndromes.size}개의 복합 생체 증후군이 감지되었습니다. ${syndromes.first().title}에 집중한 영양/휴식 관리가 시급합니다."
            }
            pillarScores.values.any { it.score < 80 } -> {
                val lowPillars = pillarScores.filter { it.value.score < 80 }.keys.joinToString { it.displayName }
                "일부 생체 지표(${lowPillars})에서 영양 주의 소견이 있습니다. 맞춤형 시간영양 가이드에 따라 영양소를 섭취하세요."
            }
            else -> {
                "전반적인 5대 생체 항상성이 고르게 유지되고 있습니다. 현재의 영양 균형과 수면 패턴을 꾸준히 이어가세요."
            }
        }

        return HolisticHealthReport(
            sessionId = session.id,
            calculatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            vitalityIndex = vitalityIndex,
            vitalityGrade = vitalityGrade,
            pillarScores = pillarScores,
            detectedSyndromes = syndromes,
            chronoSchedule = chronoSchedule,
            primaryClinicalAdvice = primaryAdvice
        )
    }

    private fun computePillarScore(pillar: HealthPillar, results: List<NailAnalysisResult>): PillarScore {
        var baseScore = 100
        val symptomsFound = mutableListOf<String>()
        val nutrientsFound = mutableListOf<String>()

        for (res in results) {
            var matchedInResult = false

            for (nutrient in res.deficientNutrients) {
                val weight = if (nutrient.severity.equals("severe", ignoreCase = true)) 18 else 10
                val matched = isNutrientMatchingPillar(pillar, nutrient.name, nutrient.symptomExplanation)
                if (matched) {
                    baseScore -= weight
                    nutrientsFound.add(nutrient.name)
                    matchedInResult = true
                }
            }

            // Check if any symptoms in this scan match the pillar
            val relevantSymptoms = res.symptoms.filter { isSymptomMatchingPillar(pillar, it) }
            if (relevantSymptoms.isNotEmpty()) {
                symptomsFound.addAll(relevantSymptoms)
                if (!matchedInResult) {
                    baseScore -= 10
                }
            }
        }

        val finalScore = baseScore.coerceIn(15, 100)
        val status = when {
            finalScore >= 80 -> "최적"
            finalScore >= 60 -> "주의"
            else -> "위험"
        }

        return PillarScore(
            pillar = pillar,
            score = finalScore,
            status = status,
            contributingSymptoms = symptomsFound.distinct().take(4),
            targetNutrients = nutrientsFound.distinct()
        )
    }

    private fun isNutrientMatchingPillar(pillar: HealthPillar, name: String, explanation: String): Boolean {
        return when (pillar) {
            HealthPillar.HEMATOLOGIC -> {
                name.contains("철분") || name.contains("비타민 C") ||
                        explanation.contains("빈혈") || explanation.contains("헤모글로빈") ||
                        explanation.contains("적혈구") || explanation.contains("산소")
            }
            HealthPillar.AUTONOMIC -> {
                name.contains("마그네슘") || name.contains("칼슘") ||
                        explanation.contains("신경") || explanation.contains("스트레스") ||
                        explanation.contains("심박") || explanation.contains("긴장")
            }
            HealthPillar.KERATIN_TISSUE -> {
                name.contains("아연") || name.contains("비오틴") || name.contains("단백질") ||
                        explanation.contains("손톱") || explanation.contains("케라틴") ||
                        explanation.contains("조갑") || explanation.contains("모발")
            }
            HealthPillar.RESPIRATORY_SLEEP -> {
                name.contains("비타민 D") ||
                        explanation.contains("수면") || explanation.contains("호흡") ||
                        explanation.contains("기도") || explanation.contains("코골이")
            }
            HealthPillar.METABOLIC_IMMUNE -> {
                name.contains("비타민 B") || name.contains("비타민 A") ||
                        explanation.contains("면역") || explanation.contains("대사") ||
                        explanation.contains("진균") || explanation.contains("구강") ||
                        explanation.contains("염증") || explanation.contains("피로")
            }
        }
    }

    private fun isSymptomMatchingPillar(pillar: HealthPillar, symptom: String): Boolean {
        // Exclude purely normal/healthy statements
        val isExplicitlyHealthy = symptom.contains("정상") || symptom.contains("건강") || symptom.contains("양호")
        if (isExplicitlyHealthy && !symptom.contains("의심") && !symptom.contains("이상") && !symptom.contains("부족")) {
            return false
        }

        return when (pillar) {
            HealthPillar.HEMATOLOGIC -> {
                symptom.contains("빈혈") || symptom.contains("결막") || symptom.contains("창백") ||
                        symptom.contains("어지러") || symptom.contains("혈색")
            }
            HealthPillar.AUTONOMIC -> {
                symptom.contains("긴장") || symptom.contains("스트레스") || symptom.contains("떨림") ||
                        symptom.contains("빈맥") || symptom.contains("불안정") ||
                        (symptom.contains("SDNN") && (symptom.contains("<") || symptom.contains("저하"))) ||
                        (symptom.contains("BPM") && (symptom.contains(">") || symptom.contains("증가") || symptom.contains("이상")))
            }
            HealthPillar.KERATIN_TISSUE -> {
                symptom.contains("갈라") || symptom.contains("세로줄") ||
                        symptom.contains("가로홈") || symptom.contains("Beau") || symptom.contains("스푼") ||
                        symptom.contains("조갑") || symptom.contains("요철") || symptom.contains("깨짐") ||
                        symptom.contains("부스러") || symptom.contains("백색") || symptom.contains("점상")
            }
            HealthPillar.RESPIRATORY_SLEEP -> {
                symptom.contains("코골이") || symptom.contains("무호흡") ||
                        (symptom.contains("호흡") && (symptom.contains("장애") || symptom.contains("불안") || symptom.contains("곤란"))) ||
                        (symptom.contains("dB") && !symptom.contains("정상"))
            }
            HealthPillar.METABOLIC_IMMUNE -> {
                symptom.contains("피로") || symptom.contains("구강") || symptom.contains("무좀") ||
                        symptom.contains("진균") || symptom.contains("염증") || symptom.contains("성대") ||
                        symptom.contains("입안") || symptom.contains("혓바늘") || symptom.contains("설염")
            }
        }
    }

    private fun detectSyndromes(
        pillars: Map<HealthPillar, PillarScore>,
        results: List<NailAnalysisResult>
    ): List<CrossModalSyndrome> {
        val syndromes = mutableListOf<CrossModalSyndrome>()
        val hemScore = pillars[HealthPillar.HEMATOLOGIC]?.score ?: 100
        val autScore = pillars[HealthPillar.AUTONOMIC]?.score ?: 100
        val kerScore = pillars[HealthPillar.KERATIN_TISSUE]?.score ?: 100
        val slpScore = pillars[HealthPillar.RESPIRATORY_SLEEP]?.score ?: 100
        val metScore = pillars[HealthPillar.METABOLIC_IMMUNE]?.score ?: 100

        // 1. Hypoxic Fatigue & Anemia Overdrive (Hematologic + Autonomic)
        if (hemScore < 70 && autScore < 70) {
            val evidence = mutableListOf<String>()
            val hemEvidence = pillars[HealthPillar.HEMATOLOGIC]?.contributingSymptoms ?: emptyList()
            val autEvidence = pillars[HealthPillar.AUTONOMIC]?.contributingSymptoms ?: emptyList()
            if (hemEvidence.isNotEmpty()) evidence.add(hemEvidence.first()) else evidence.add("결막 창백 징후 / 철분 결핍 소견")
            if (autEvidence.isNotEmpty()) evidence.add(autEvidence.first()) else evidence.add("PPG 심박변이도(SDNN) 저하")
            evidence.add("만성 산소전달 저하로 인한 자율신경 보상성 과긴장")

            syndromes.add(
                CrossModalSyndrome(
                    id = "SYN_HYPOXIC_AUTONOMIC",
                    title = "저산소증 피로 및 심혈관 긴장 복합 증후군",
                    severity = "High",
                    physiologicalMechanism = "적혈구 헤모글로빈 생성 부족으로 말초 조직 산소 전달이 저하되어, 심장이 보상성 빈맥(BPM 증가) 및 자율신경 긴장을 촉발하는 상태입니다.",
                    biomarkerEvidence = evidence,
                    clinicalActionPlan = "철분과 비타민 C를 아침 공복에 섭취하고, 마그네슘을 저녁에 보충하여 교감신경 흥분을 안정시키세요."
                )
            )
        }

        // 2. Sleep Disordered Mineral Depletion (Autonomic + Respiratory/Sleep)
        if (autScore < 70 && slpScore < 70) {
            val evidence = mutableListOf<String>()
            val slpEvidence = pillars[HealthPillar.RESPIRATORY_SLEEP]?.contributingSymptoms ?: emptyList()
            val autEvidence = pillars[HealthPillar.AUTONOMIC]?.contributingSymptoms ?: emptyList()
            if (slpEvidence.isNotEmpty()) evidence.add(slpEvidence.first()) else evidence.add("수면 음향 저주파 코골이 에너지 감지")
            if (autEvidence.isNotEmpty()) evidence.add(autEvidence.first()) else evidence.add("PPG 스트레스 지수 상승")
            evidence.add("야간 저산소증으로 인한 체내 마그네슘·비타민 D 급속 소진")

            syndromes.add(
                CrossModalSyndrome(
                    id = "SYN_SLEEP_MINERAL_DEPLETION",
                    title = "수면 호흡 장애 및 신경-미네랄 소진 증후군",
                    severity = "High",
                    physiologicalMechanism = "수면 중 코골이/무호흡으로 인한 야간 저산소증이 체내 마그네슘과 비타민 D를 급격히 소모하여 신경 흥분성을 유발합니다.",
                    biomarkerEvidence = evidence,
                    clinicalActionPlan = "취침 1시간 전 킬레이트 마그네슘(300mg)을 섭취하고, 낮 시간 비타민 D(2000IU)를 식사와 함께 복용하세요."
                )
            )
        }

        // 3. Keratin Barrier Immunodeficiency (Keratin Tissue + Metabolic/Immune)
        if (kerScore < 70 && metScore < 70) {
            val evidence = mutableListOf<String>()
            val kerEvidence = pillars[HealthPillar.KERATIN_TISSUE]?.contributingSymptoms ?: emptyList()
            val metEvidence = pillars[HealthPillar.METABOLIC_IMMUNE]?.contributingSymptoms ?: emptyList()
            if (kerEvidence.isNotEmpty()) evidence.add(kerEvidence.first()) else evidence.add("손톱 세로줄 / 표면 요철 / 깨짐")
            if (metEvidence.isNotEmpty()) evidence.add(metEvidence.first()) else evidence.add("구강 점막 헐음 / 전신 피로")
            evidence.add("조갑 기질 단백질 합성 저하 및 상피 세포 면역 장벽 약화")

            syndromes.add(
                CrossModalSyndrome(
                    id = "SYN_KERATIN_IMMUNE_COLLAPSE",
                    title = "조갑 기질 약화 및 피부점막 면역 저하 징후",
                    severity = "Moderate",
                    physiologicalMechanism = "아연 및 비타민 B 코엔자임 결핍으로 조갑 기질 세포 분열과 단백질 합성이 저해되어 진균 침투 방어가 취약해진 상태입니다.",
                    biomarkerEvidence = evidence,
                    clinicalActionPlan = "아연(15~30mg)과 비오틴(5000mcg), 고품질 필수 아미노산(단백질) 섭취를 4주 이상 지속하세요."
                )
            )
        }

        return syndromes
    }

    private fun generateChronoSchedule(results: List<NailAnalysisResult>): List<ChronoNutrientPlan> {
        val schedule = mutableListOf<ChronoNutrientPlan>()
        val allDeficiencies = results.flatMap { it.deficientNutrients }.map { it.name }.distinct()

        // 1. Morning Empty Stomach (07:00 ~ 08:00)
        if (allDeficiencies.any { it.contains("철분") || it.contains("비타민 C") }) {
            schedule.add(
                ChronoNutrientPlan(
                    timeSlot = "🌅 아침 공복 (07:00 ~ 08:00)",
                    nutrientName = "철분 + 비타민 C",
                    primaryFoods = listOf("소고기", "시금치", "방울토마토", "오렌지"),
                    synergyTip = "비타민 C와 함께 섭취 시 십이지장 흡수율이 최대 300%까지 증가합니다.",
                    antagonismWarning = "⚠️ 커피, 녹차의 탄닌 및 칼슘/우유와 함께 먹으면 흡수가 차단되므로 2시간 이상 간격을 두세요."
                )
            )
        }

        // 2. Morning After Breakfast (08:30 ~ 09:00)
        if (allDeficiencies.any { it.contains("비타민 B") }) {
            schedule.add(
                ChronoNutrientPlan(
                    timeSlot = "🌅 아침 식후 (08:30 ~ 09:00)",
                    nutrientName = "비타민 B군 복합체",
                    primaryFoods = listOf("계란", "연어", "귀리"),
                    synergyTip = "에너지 대사 코엔자임으로 작용하므로 아침에 복용해야 낮 동안의 활력 증진에 가장 효과적입니다.",
                    antagonismWarning = null
                )
            )
        }

        // 3. Lunch After Meal (12:30 ~ 13:30)
        if (allDeficiencies.any { it.contains("비타민 D") || it.contains("아연") || it.contains("비타민 A") }) {
            schedule.add(
                ChronoNutrientPlan(
                    timeSlot = "☀️ 점심 식후 (12:30 ~ 13:30)",
                    nutrientName = "비타민 D & 아연",
                    primaryFoods = listOf("굴", "버섯류", "견과류"),
                    synergyTip = "비타민 D는 지용성이므로 식사 중 포함된 지방과 함께 섭취할 때 흡수율이 극대화됩니다.",
                    antagonismWarning = "⚠️ 고용량 아연은 구리 및 철분과 동시 섭취 시 흡수를 방해할 수 있습니다."
                )
            )
        }

        if (allDeficiencies.any { it.contains("비오틴") || it.contains("단백질") }) {
            schedule.add(
                ChronoNutrientPlan(
                    timeSlot = "☀️ 점심 식후 (12:30 ~ 13:30)",
                    nutrientName = "비오틴 & 복합 아미노산",
                    primaryFoods = listOf("달걀노른자", "콩류", "닭가슴살"),
                    synergyTip = "케라틴 단백질 합성을 촉진하여 모발과 손톱 강도를 개선합니다.",
                    antagonismWarning = null
                )
            )
        }

        // 4. Bedtime 1 Hour Before Sleep (22:00 ~ 23:00)
        if (allDeficiencies.any { it.contains("마그네슘") || it.contains("칼슘") }) {
            schedule.add(
                ChronoNutrientPlan(
                    timeSlot = "🌙 취침 1시간 전 (22:00 ~ 23:00)",
                    nutrientName = "마그네슘 & L-테아닌",
                    primaryFoods = listOf("바나나", "아몬드", "따뜻한 캐모마일"),
                    synergyTip = "GABA 수용체를 자극하여 중추신경을 이완시키고 수면 중 근육 경련과 코골이를 완화합니다.",
                    antagonismWarning = "⚠️ 아침의 철분제와 완전히 분리되어 상호 흡수 저해가 발생하지 않습니다."
                )
            )
        }

        return schedule
    }

    private fun buildDefaultOptimalReport(sessionId: String): HolisticHealthReport {
        val optimalPillars = HealthPillar.entries.associateWith { pillar ->
            PillarScore(pillar, 95, "최적", emptyList(), emptyList())
        }
        return HolisticHealthReport(
            sessionId = sessionId,
            calculatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            vitalityIndex = 95,
            vitalityGrade = "최상 (Optimal Vitality)",
            pillarScores = optimalPillars,
            detectedSyndromes = emptyList(),
            chronoSchedule = emptyList(),
            primaryClinicalAdvice = "등록된 결핍 소견이 없습니다. 현재의 건강한 생활 습관을 유지해 주세요."
        )
    }
}
