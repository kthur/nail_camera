package com.example.nailnutri.analysis

import com.example.nailnutri.data.*
import org.junit.Assert.*
import org.junit.Test

class HolisticBiomarkerEngineTest {

    private val sampleSession = SessionReport(
        id = "session_test_001",
        createdAt = "2026-11-24 15:30",
        label = "통합 건강 검진 세션",
        resultIds = listOf("res_1", "res_2"),
        topDeficiencies = listOf("철분", "마그네슘"),
        overallScore = 75
    )

    @Test
    fun testEmptyResultsReturnsOptimalVitality() {
        val report = HolisticBiomarkerEngine.evaluate(sampleSession, emptyList())

        assertEquals("session_test_001", report.sessionId)
        assertEquals(95, report.vitalityIndex)
        assertEquals("최상 (Optimal Vitality)", report.vitalityGrade)
        assertTrue("빈 결과일 때 증후군이 검출되지 않아야 합니다", report.detectedSyndromes.isEmpty())
        assertTrue("빈 결과일 때 시간영양 계획이 비어있어야 합니다", report.chronoSchedule.isEmpty())

        for (pillar in HealthPillar.entries) {
            val score = report.pillarScores[pillar]
            assertNotNull(score)
            assertEquals(95, score!!.score)
            assertEquals("최적", score.status)
        }
    }

    @Test
    fun testSolitaryNailScanIsolatesKeratinPillar() {
        val nailScan = NailAnalysisResult(
            id = "nail_001",
            date = "2026-11-24",
            imagePath = "demo_nail.jpg",
            symptoms = listOf("손톱 세로줄 및 갈라짐"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "비오틴",
                    severity = "Moderate",
                    symptomExplanation = "손톱 케라틴 기질 합성 저하",
                    recommendedFoods = listOf("달걀노른자", "연어")
                ),
                NutrientDetail(
                    name = "아연",
                    severity = "Moderate",
                    symptomExplanation = "조갑 세포 분열 지연",
                    recommendedFoods = listOf("굴", "소고기")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "손톱 영양 보충 필요"
        )

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, listOf(nailScan))

        val keratinScore = report.pillarScores[HealthPillar.KERATIN_TISSUE]!!.score
        val hematologicScore = report.pillarScores[HealthPillar.HEMATOLOGIC]!!.score

        assertTrue("케라틴 기질 기둥 점수는 감소해야 합니다 ($keratinScore)", keratinScore < 90)
        assertEquals("조혈 기둥은 영향을 받지 않아 100이어야 합니다", 100, hematologicScore)
    }

    @Test
    fun testAnemiaAndPpgTriggerHypoxicSyndrome() {
        val conjunctivaScan = NailAnalysisResult(
            id = "conj_001",
            date = "2026-11-24",
            imagePath = "conj.jpg",
            symptoms = listOf("결막 창백 징후 (철분 부족 의심)"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "철분",
                    severity = "Severe",
                    symptomExplanation = "헤모글로빈 생성 부족으로 빈혈 징후",
                    recommendedFoods = listOf("소고기", "시금치")
                ),
                NutrientDetail(
                    name = "비타민 C",
                    severity = "Severe",
                    symptomExplanation = "철분 흡수 보조 인자 결핍",
                    recommendedFoods = listOf("오렌지", "방울토마토")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "철분 보충 시급"
        )

        val ppgScan = NailAnalysisResult(
            id = "ppg_001",
            date = "2026-11-24",
            imagePath = "ppg.jpg",
            symptoms = listOf("자율신경 긴장 (SDNN < 32ms)", "보상성 심박 증가 (BPM 98)"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "마그네슘",
                    severity = "Severe",
                    symptomExplanation = "신경 긴장 및 심박변이도 저하",
                    recommendedFoods = listOf("바나나", "아몬드")
                ),
                NutrientDetail(
                    name = "칼슘",
                    severity = "Severe",
                    symptomExplanation = "근신경 흥분성 조절 장애",
                    recommendedFoods = listOf("우유", "멸치")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "휴식 및 미네랄 공급 권장"
        )

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, listOf(conjunctivaScan, ppgScan))

        val hemScore = report.pillarScores[HealthPillar.HEMATOLOGIC]!!.score
        val autScore = report.pillarScores[HealthPillar.AUTONOMIC]!!.score
        assertTrue("조혈 점수는 70 미만이어야 합니다 ($hemScore)", hemScore < 70)
        assertTrue("자율신경 점수는 70 미만이어야 합니다 ($autScore)", autScore < 70)

        val hypoxicSyndrome = report.detectedSyndromes.find { it.id == "SYN_HYPOXIC_AUTONOMIC" }
        assertNotNull("저산소증-자율신경 긴장 복합 증후군이 검출되어야 합니다", hypoxicSyndrome)
        assertEquals("High", hypoxicSyndrome!!.severity)
        assertTrue(hypoxicSyndrome.biomarkerEvidence.isNotEmpty())
        assertTrue(hypoxicSyndrome.clinicalActionPlan.contains("철분"))
        assertTrue(report.primaryClinicalAdvice.contains("저산소증 피로"))
    }

    @Test
    fun testSleepAndPpgTriggerSleepMineralSyndrome() {
        val sleepScan = NailAnalysisResult(
            id = "sleep_001",
            date = "2026-11-24",
            imagePath = "audio.wav",
            symptoms = listOf("수면 중 저주파 코골이 감지 (52dB)", "야간 무호흡 의심"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "비타민 D",
                    severity = "Severe",
                    symptomExplanation = "상기도 근력 저하 및 수면 호흡 불안정",
                    recommendedFoods = listOf("연어", "버섯")
                ),
                NutrientDetail(
                    name = "비타민 D3",
                    severity = "Severe",
                    symptomExplanation = "야간 수면 구조 장애",
                    recommendedFoods = listOf("계란")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "수면 호흡 관리 권고"
        )

        val ppgScan = NailAnalysisResult(
            id = "ppg_002",
            date = "2026-11-24",
            imagePath = "ppg.jpg",
            symptoms = listOf("자율신경 긴장 (SDNN 28ms)"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "마그네슘",
                    severity = "Severe",
                    symptomExplanation = "신경 이완 장애 및 스트레스",
                    recommendedFoods = listOf("아몬드")
                ),
                NutrientDetail(
                    name = "칼슘",
                    severity = "Severe",
                    symptomExplanation = "심박 조절 불균형",
                    recommendedFoods = listOf("치즈")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "스트레스 완화 필요"
        )

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, listOf(sleepScan, ppgScan))

        val sleepSyndrome = report.detectedSyndromes.find { it.id == "SYN_SLEEP_MINERAL_DEPLETION" }
        assertNotNull("수면-미네랄 소진 증후군이 검출되어야 합니다", sleepSyndrome)
        assertEquals("High", sleepSyndrome!!.severity)
        assertTrue(sleepSyndrome.clinicalActionPlan.contains("마그네슘"))
    }

    @Test
    fun testKeratinAndMetabolicTriggerKeratinImmuneSyndrome() {
        val nailScan = NailAnalysisResult(
            id = "nail_002",
            date = "2026-11-24",
            imagePath = "nail.jpg",
            symptoms = listOf("손톱 갈라짐 및 조갑 기질 요철"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "아연",
                    severity = "Severe",
                    symptomExplanation = "조갑 세포 분열 지연",
                    recommendedFoods = listOf("굴")
                ),
                NutrientDetail(
                    name = "비오틴",
                    severity = "Severe",
                    symptomExplanation = "손톱 케라틴 결핍",
                    recommendedFoods = listOf("계란노른자")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "손톱 기질 집중 관리"
        )

        val voiceScan = NailAnalysisResult(
            id = "voice_001",
            date = "2026-11-24",
            imagePath = "voice.wav",
            symptoms = listOf("만성 피로 호소 및 구강 점막 헐음", "성대 피로도 상승"),
            deficientNutrients = listOf(
                NutrientDetail(
                    name = "비타민 B12",
                    severity = "Severe",
                    symptomExplanation = "구강 점막 재생 지연 및 피로",
                    recommendedFoods = listOf("육류", "조개류")
                ),
                NutrientDetail(
                    name = "비타민 A",
                    severity = "Severe",
                    symptomExplanation = "점막 상피 세포 면역 저하",
                    recommendedFoods = listOf("당근")
                )
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "비타민 B/A 복합 섭취"
        )

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, listOf(nailScan, voiceScan))

        val keratinSyndrome = report.detectedSyndromes.find { it.id == "SYN_KERATIN_IMMUNE_COLLAPSE" }
        assertNotNull("조갑 기질 약화 및 면역 저하 증후군이 검출되어야 합니다", keratinSyndrome)
        assertEquals("Moderate", keratinSyndrome!!.severity)
        assertTrue(keratinSyndrome.clinicalActionPlan.contains("아연"))
    }

    @Test
    fun testChronoNutritionSpacingAndAntagonism() {
        val multiScan = NailAnalysisResult(
            id = "multi_001",
            date = "2026-11-24",
            imagePath = "scan.jpg",
            symptoms = listOf("피로 및 손톱 약화"),
            deficientNutrients = listOf(
                NutrientDetail("철분", "Severe", "빈혈", listOf("소고기")),
                NutrientDetail("비타민 B12", "Moderate", "피로", listOf("연어")),
                NutrientDetail("비타민 D", "Moderate", "뼈 건강", listOf("버섯")),
                NutrientDetail("마그네슘", "Severe", "근육 경련", listOf("바나나"))
            ),
            sufficientNutrients = emptyList(),
            overallAdvice = "종합 영양 보충"
        )

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, listOf(multiScan))
        val schedule = report.chronoSchedule

        assertTrue("아침 공복 철분 계획이 포함되어야 합니다", schedule.any { it.timeSlot.contains("아침 공복") && it.nutrientName.contains("철분") })
        assertTrue("아침 식후 비타민 B 계획이 포함되어야 합니다", schedule.any { it.timeSlot.contains("아침 식후") && it.nutrientName.contains("비타민 B") })
        assertTrue("점심 비타민 D 계획이 포함되어야 합니다", schedule.any { it.timeSlot.contains("점심") && it.nutrientName.contains("비타민 D") })
        assertTrue("취침 전 마그네슘 계획이 포함되어야 합니다", schedule.any { it.timeSlot.contains("취침") && it.nutrientName.contains("마그네슘") })

        val ironPlan = schedule.first { it.nutrientName.contains("철분") }
        val warning = ironPlan.antagonismWarning
        assertNotNull("철분 복용 시 탄닌/칼슘 길항 경고가 포함되어야 합니다", warning)
        assertTrue(warning?.contains("커피") == true || warning?.contains("칼슘") == true)
    }

    @Test
    fun testMathematicalInvariantsAndClamping() {
        // Build 10 severe deficiency scans to aggressively stress test boundaries
        val extremeResults = (1..10).map { i ->
            NailAnalysisResult(
                id = "extreme_$i",
                date = "2026-11-24",
                imagePath = "path_$i.jpg",
                symptoms = listOf("빈혈", "자율신경 긴장", "손톱 갈라짐", "코골이", "만성 피로"),
                deficientNutrients = listOf(
                    NutrientDetail("철분", "Severe", "빈혈", listOf("소고기")),
                    NutrientDetail("마그네슘", "Severe", "신경 긴장", listOf("바나나")),
                    NutrientDetail("아연", "Severe", "손톱 케라틴", listOf("굴")),
                    NutrientDetail("비타민 D", "Severe", "수면 호흡", listOf("버섯")),
                    NutrientDetail("비타민 B", "Severe", "면역 대사", listOf("연어"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "집중 관리 필요"
            )
        }

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, extremeResults)

        for ((pillar, score) in report.pillarScores) {
            assertTrue("기둥 점수($pillar: ${score.score})는 15점 이상이어야 합니다", score.score >= 15)
            assertTrue("기둥 점수($pillar: ${score.score})는 100점 이하이어야 합니다", score.score <= 100)
            assertEquals("극단적 결핍 시 상태는 '위험'이어야 합니다", "위험", score.status)
        }

        assertTrue("활력 지수(${report.vitalityIndex})는 10점 이상이어야 합니다", report.vitalityIndex >= 10)
        assertTrue("활력 지수(${report.vitalityIndex})는 100점 이하이어야 합니다", report.vitalityIndex <= 100)
        assertEquals("집중 개선 요망 (Critical Attention)", report.vitalityGrade)
    }

    @Test
    fun testSingleScanGracefulEvaluation() {
        val singleResult = NailAnalysisResult(
            id = "single_001",
            date = "2026-11-24",
            imagePath = "single.jpg",
            symptoms = listOf("정상 손톱"),
            deficientNutrients = emptyList(),
            sufficientNutrients = emptyList(),
            overallAdvice = "이상 없음"
        )

        val report = HolisticBiomarkerEngine.evaluate(sampleSession, listOf(singleResult))

        assertEquals(100, report.vitalityIndex)
        assertEquals("최상 (Optimal Vitality)", report.vitalityGrade)
        assertTrue(report.detectedSyndromes.isEmpty())
        assertTrue(report.chronoSchedule.isEmpty())
        assertTrue(report.primaryClinicalAdvice.contains("생체 항상성"))
    }
}
