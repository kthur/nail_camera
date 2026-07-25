package com.example.nailnutri.analysis

import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail

data class SymptomAnalysisOutput(
    val detectedSymptoms: List<String>,
    val deficientNutrients: List<NutrientDetail>,
    val sufficientNutrients: List<SufficientNutrientDetail>,
    val advice: String
)

object SymptomNutrientEngine {

    fun analyzeSymptomText(text: String): SymptomAnalysisOutput {
        val lowerText = text.trim().lowercase()
        val detectedSymptoms = mutableListOf<String>()
        val deficientNutrients = mutableListOf<NutrientDetail>()
        val sufficientNutrients = mutableListOf<SufficientNutrientDetail>()

        // 1. 입안 헐음 / 설염 / 만성 피로
        if (lowerText.contains("입안") || lowerText.contains("입병") || lowerText.contains("헐") || lowerText.contains("설염") || lowerText.contains("피곤") || lowerText.contains("피로")) {
            detectedSymptoms.add("구강 점막 재생 저하 및 만성 피로")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 B군 (B2/B6/B12)",
                    severity = "Moderate",
                    symptomExplanation = "음성 증상 분석 결과, 구강 점막 비정상 헐음 및 만성 피로는 체내 비타민 B군 코엔자임 결핍과 밀접하게 연관되어 있습니다.",
                    recommendedFoods = listOf("계란", "우유", "시금치", "닭고기", "연어")
                )
            )
        }

        // 2. 눈밑 떨림 / 근육 경련 / 수면 장애
        if (lowerText.contains("눈밑") || lowerText.contains("떨림") || lowerText.contains("경련") || lowerText.contains("쥐") || lowerText.contains("잠") || lowerText.contains("불면")) {
            detectedSymptoms.add("신경-근육 흥분성 증가 (눈밑/근육 미세 떨림)")
            deficientNutrients.add(
                NutrientDetail(
                    name = "마그네슘",
                    severity = "High",
                    symptomExplanation = "신경 자극 전달 조정 및 근육 이완에 필수적인 마그네슘 부족 시 미세 눈밑 떨림과 근육 경련이 유발될 수 있습니다.",
                    recommendedFoods = listOf("아몬드", "바나나", "다크 초콜릿", "시금치", "콩류")
                )
            )
        }

        // 3. 손톱 약화 / 잘 부러짐 / 흰 반점 / 탈모
        if (lowerText.contains("손톱") || lowerText.contains("부러") || lowerText.contains("갈라") || lowerText.contains("반점") || lowerText.contains("머리") || lowerText.contains("탈모")) {
            detectedSymptoms.add("손톱 케라틴 및 모질 조직 약화")
            deficientNutrients.add(
                NutrientDetail(
                    name = "아연 & 비오틴",
                    severity = "Moderate",
                    symptomExplanation = "단백질 대사 및 케라틴 합성을 돕는 아연과 비오틴이 부족할 경우 손톱 결이 약해지고 쉽게 잘 갈라집니다.",
                    recommendedFoods = listOf("굴", "붉은 고기", "계란 노른자", "아몬드", "귀리")
                )
            )
        }

        // 4. 어지러움 / 빈혈 / 창백함
        if (lowerText.contains("어지러") || lowerText.contains("빈혈") || lowerText.contains("창백") || lowerText.contains("숨") || lowerText.contains("무기력")) {
            detectedSymptoms.add("적혈구 헤모글로빈 생성 부족 징후")
            deficientNutrients.add(
                NutrientDetail(
                    name = "철분 & 비타민 C",
                    severity = "High",
                    symptomExplanation = "산소 운반을 담당하는 철분 수치 저하 시 창백함, 어지럼증, 무기력감이 나타나며, 비타민 C와 함께 섭취 시 흡수율이 증대됩니다.",
                    recommendedFoods = listOf("소고기", "시금치", "깻잎", "귤", "브로콜리")
                )
            )
        }

        // 5. 관절 통증 / 뼈 약화 / 햇빛 부족
        if (lowerText.contains("관절") || lowerText.contains("뼈") || lowerText.contains("무릎") || lowerText.contains("햇빛")) {
            detectedSymptoms.add("골밀도 유지 및 칼슘 흡수율 저하")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 D & 칼슘",
                    severity = "Moderate",
                    symptomExplanation = "실내 활동 증가 및 비타민 D 합성 부족은 칼슘 흡수 장애와 골밀도 약화를 초래할 수 있습니다.",
                    recommendedFoods = listOf("연어", "우유", "치즈", "계란 노른자", "표고버섯")
                )
            )
        }

        // 기본 Fallback 또는 양호한 상태 처리
        if (detectedSymptoms.isEmpty()) {
            detectedSymptoms.add("음성 기반 증상 진술 (특이 정황 미감지)")
            sufficientNutrients.add(
                SufficientNutrientDetail(
                    name = "종합 필수 영양소",
                    symptomExplanation = "음성 텍스트 진술상 중증 영양 결핍 징후가 직접 발견되지 않았으며, 전반적인 체내 영양 균형 상태가 양호합니다.",
                    role = "항상성 유지 및 대사 원활"
                )
            )
        }

        val advice = if (deficientNutrients.isNotEmpty()) {
            "음성 증상 인식 파싱 결과, 총 ${deficientNutrients.size}가지 영양소 수치 보충이 추천됩니다. ${deficientNutrients.joinToString { it.name }} 관련 식품 섭취를 권장합니다."
        } else {
            "음성 진술상 특이 결핍 징후가 나타나지 않았습니다. 현재 건강한 식단과 밸런스를 계속 유지해 주세요."
        }

        return SymptomAnalysisOutput(
            detectedSymptoms = detectedSymptoms,
            deficientNutrients = deficientNutrients,
            sufficientNutrients = sufficientNutrients,
            advice = advice
        )
    }
}
