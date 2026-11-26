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

        // 1. 입안 헐음 / 설염 / 만성 피로 / 무기력 / 번아웃
        if (lowerText.contains("입안") || lowerText.contains("입병") || lowerText.contains("헐") ||
            lowerText.contains("설염") || lowerText.contains("피곤") || lowerText.contains("피로") ||
            lowerText.contains("번아웃") || lowerText.contains("기력") || lowerText.contains("힘이 없") ||
            lowerText.contains("기운이 없") || lowerText.contains("에너지") || lowerText.contains("졸음") ||
            lowerText.contains("나른")) {
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

        // 2. 눈밑 떨림 / 근육 경련 / 수면 장애 / 야간 경련 / 다리 쥐
        if (lowerText.contains("눈밑") || lowerText.contains("떨림") || lowerText.contains("경련") ||
            lowerText.contains("쥐") || lowerText.contains("잠") || lowerText.contains("불면") ||
            lowerText.contains("수면") || lowerText.contains("잠을 못") || lowerText.contains("잠이 안") ||
            lowerText.contains("야간") || lowerText.contains("다리 저") || lowerText.contains("다리가 저") ||
            lowerText.contains("근육통") || lowerText.contains("근육이 뭉") || lowerText.contains("뭉침")) {
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

        // 3. 손톱 약화 / 잘 부러짐 / 흰 반점 / 탈모 / 머리카락 빠짐
        if (lowerText.contains("손톱") || lowerText.contains("부러") || lowerText.contains("갈라") ||
            lowerText.contains("반점") || lowerText.contains("머리") || lowerText.contains("탈모") ||
            lowerText.contains("머리카락") || lowerText.contains("머리가 빠") || lowerText.contains("빠짐") ||
            lowerText.contains("모발") || lowerText.contains("가늘어") || lowerText.contains("힘없는")) {
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

        // 4. 어지러움 / 빈혈 / 창백함 / 숨참 / 무기력
        if (lowerText.contains("어지러") || lowerText.contains("빈혈") || lowerText.contains("창백") ||
            lowerText.contains("숨") || lowerText.contains("무기력") || lowerText.contains("어질") ||
            lowerText.contains("핑 돌") || lowerText.contains("눈앞이 캄") || lowerText.contains("심장이 빨리") ||
            lowerText.contains("가슴이 두근") || lowerText.contains("두근") || lowerText.contains("심박")) {
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

        // 5. 관절 통증 / 뼈 약화 / 햇빛 부족 / 허리 통증
        if (lowerText.contains("관절") || lowerText.contains("뼈") || lowerText.contains("무릎") ||
            lowerText.contains("햇빛") || lowerText.contains("허리") || lowerText.contains("등이 아") ||
            lowerText.contains("뼈가 아") || lowerText.contains("골다공") || lowerText.contains("실내") ||
            lowerText.contains("야외 활동") || lowerText.contains("햇볕")) {
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

        // 6. 손톱 가로줄 / 가로 홈 / 보우선 (Beau's Lines)
        if (lowerText.contains("가로줄") || lowerText.contains("가로 홈") || lowerText.contains("가로홈") ||
            lowerText.contains("보우선") || lowerText.contains("보우") || lowerText.contains("수평선") || lowerText.contains("수평 홈")) {
            detectedSymptoms.add("손톱 가로 패임 및 일시적 성장 정지 (Beau's Lines)")
            deficientNutrients.add(
                NutrientDetail(
                    name = "아연 & 단백질",
                    severity = "Severe",
                    symptomExplanation = "고열, 급성 질환, 중증 대사 스트레스 후 나타나는 손톱 가로 홈(보우선)은 세포 분열 아연 및 케라틴 합성 단백질의 급격한 소모/결핍을 시사합니다.",
                    recommendedFoods = listOf("굴", "소고기", "호박씨", "닭가슴살", "달걀")
                )
            )
        }

        // 7. 반월 결손 / 손톱 반달 이상 (Lunula Abnormality)
        if (lowerText.contains("반월") || lowerText.contains("반달") || lowerText.contains("손톱 반달") ||
            lowerText.contains("반달이 없") || lowerText.contains("반월 소실") || lowerText.contains("반월 이상")) {
            detectedSymptoms.add("손톱 기저부 반월 소실 및 미세순환 저하")
            deficientNutrients.add(
                NutrientDetail(
                    name = "철분 & 비타민 B12",
                    severity = "Severe",
                    symptomExplanation = "손톱 하단 반월(반달)의 소실이나 변색은 만성 빈혈, 헤모글로빈 부족 및 말초 모세혈관 순환 저하와 밀접한 연관이 있습니다.",
                    recommendedFoods = listOf("붉은 고기", "시금치", "조개류", "달걀노른자", "연어")
                )
            )
        }

        // 8. 조갑진균증 / 손톱 무좀 / 변색 (Onychomycosis)
        if (lowerText.contains("무좀") || lowerText.contains("진균") || lowerText.contains("곰팡이") ||
            lowerText.contains("손톱이 두꺼") || lowerText.contains("노랗게") || lowerText.contains("황갈색") ||
            lowerText.contains("조갑진균") || lowerText.contains("조갑진균증") || lowerText.contains("변색")) {
            detectedSymptoms.add("손발톱 진균 감염 및 각질 비후 (Onychomycosis)")
            deficientNutrients.add(
                NutrientDetail(
                    name = "아연 & 비타민 A/D",
                    severity = "Severe",
                    symptomExplanation = "진균 증식 및 각질층 파괴는 체내 T세포 면역 활성을 담당하는 아연과 피부 점막 방어 비타민(A, D) 부족에 따른 면역 저하 상태와 연관될 수 있습니다.",
                    recommendedFoods = listOf("굴", "소고기", "당근", "버섯류", "호박씨")
                )
            )
        }

        // 9. 흑색선조 / 검은 세로줄 (Melanonychia)
        if (lowerText.contains("흑색") || lowerText.contains("검은 줄") || lowerText.contains("검은줄") ||
            lowerText.contains("검은 선") || lowerText.contains("검은선") || lowerText.contains("멜라닌") ||
            lowerText.contains("흑색선조") || lowerText.contains("검은 세로줄") || lowerText.contains("검은세로줄")) {
            detectedSymptoms.add("손톱 세로 흑색선조 및 멜라닌 색소 과다 침착 (Melanonychia)")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 B12 & 항산화 영양소",
                    severity = "Severe",
                    symptomExplanation = "세로 방향의 흑갈색 선조는 멜라닌 세포의 과민 반응 및 비타민 B12 결핍으로 인한 색소 조절 이상과 관련될 수 있습니다.",
                    recommendedFoods = listOf("소간", "연어", "참치", "달걀", "브라질너트")
                )
            )
        }

        // 10. 피부 트러블 / 여드름 / 각질 / 건조한 피부
        if (lowerText.contains("피부") || lowerText.contains("여드름") || lowerText.contains("각질") ||
            lowerText.contains("트러블") || lowerText.contains("뾰루지") || lowerText.contains("건조") ||
            lowerText.contains("피부가 거칠") || lowerText.contains("피부가 푸석") || lowerText.contains("푸석") ||
            lowerText.contains("가렵") || lowerText.contains("두드러기") || lowerText.contains("발진")) {
            detectedSymptoms.add("피부 장벽 기능 저하 및 피지 조절 이상")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 A & 오메가-3",
                    severity = "Moderate",
                    symptomExplanation = "피부 각질 세포 재생을 조절하는 비타민 A(레티놀)와 염증 억제에 필수적인 오메가-3 지방산이 부족할 경우 피부 건조·각질·여드름이 악화됩니다.",
                    recommendedFoods = listOf("당근", "고구마", "고등어", "아보카도", "달걀노른자")
                )
            )
        }

        // 11. 입술 갈라짐 / 입꼬리 헐음 / 구순염
        if (lowerText.contains("입술") || lowerText.contains("입꼬리") || lowerText.contains("구순") ||
            lowerText.contains("입술이 갈라") || lowerText.contains("입술 건조") || lowerText.contains("입이 건조") ||
            lowerText.contains("입술이 트") || lowerText.contains("구강 건조") || lowerText.contains("입이 마르")) {
            detectedSymptoms.add("구순부 점막 재생 장애 및 리보플라빈 결핍 징후")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 B2 (리보플라빈) & 철분",
                    severity = "Moderate",
                    symptomExplanation = "입술 양 끝이 갈라지거나 헐고 구강 점막이 건조한 증상은 비타민 B2(리보플라빈)와 철분 부족의 대표적 신호입니다.",
                    recommendedFoods = listOf("우유", "요거트", "아몬드", "쇠간", "시금치")
                )
            )
        }

        // 12. 손발 저림 / 감각 이상 / 찌릿찌릿
        if (lowerText.contains("저림") || lowerText.contains("저려") || lowerText.contains("찌릿") ||
            lowerText.contains("손발이 차") || lowerText.contains("손이 차") || lowerText.contains("발이 차") ||
            lowerText.contains("감각이 없") || lowerText.contains("감각이 둔") || lowerText.contains("손발 저") ||
            lowerText.contains("손발이 저") || lowerText.contains("손가락이 저") || lowerText.contains("발가락이 저")) {
            detectedSymptoms.add("말초 신경 전도 이상 및 미세순환 저하")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 B12 & 비타민 E",
                    severity = "High",
                    symptomExplanation = "손발 저림과 찌릿한 감각 이상은 말초 신경의 마이엘린 수초를 보호하는 비타민 B12 결핍과 항산화·혈행 개선을 돕는 비타민 E 부족의 전형적 증상입니다.",
                    recommendedFoods = listOf("연어", "소간", "달걀", "아몬드", "해바라기씨")
                )
            )
        }

        // 13. 부종 / 얼굴 붓기 / 눈이 붓는다
        if (lowerText.contains("부종") || lowerText.contains("붓기") || lowerText.contains("부어") ||
            lowerText.contains("얼굴이 붓") || lowerText.contains("눈이 붓") || lowerText.contains("다리가 붓") ||
            lowerText.contains("손이 붓") || lowerText.contains("발이 붓") || lowerText.contains("부었")) {
            detectedSymptoms.add("체내 수분 조절 이상 및 림프순환 저하")
            deficientNutrients.add(
                NutrientDetail(
                    name = "칼륨 & 비타민 B1 (티아민)",
                    severity = "Moderate",
                    symptomExplanation = "세포 삼투압과 수분 균형을 조절하는 칼륨이 부족하면 말초 부종이 발생합니다. 비타민 B1 결핍 역시 심장·혈관 기능 저하로 부종을 유발할 수 있습니다.",
                    recommendedFoods = listOf("바나나", "고구마", "아보카도", "돼지고기", "귀리")
                )
            )
        }

        // 14. 시력 저하 / 눈이 침침 / 야맹증 / 눈 피로
        if (lowerText.contains("시력") || lowerText.contains("눈이 침침") || lowerText.contains("야맹") ||
            lowerText.contains("눈 피로") || lowerText.contains("눈이 피로") || lowerText.contains("눈이 건조") ||
            lowerText.contains("눈이 뻑뻑") || lowerText.contains("눈이 충혈") || lowerText.contains("눈이 빨개") ||
            lowerText.contains("눈이 아프") || lowerText.contains("안구 건조")) {
            detectedSymptoms.add("안구 망막 세포 및 시신경 영양 공급 저하")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 A & 루테인/지아잔틴",
                    severity = "Moderate",
                    symptomExplanation = "야간 시력을 담당하는 로돕신 합성에 필수적인 비타민 A와 황반 색소를 구성하는 루테인·지아잔틴 부족 시 시력 저하·안구 건조가 발생합니다.",
                    recommendedFoods = listOf("당근", "케일", "계란 노른자", "블루베리", "시금치")
                )
            )
        }

        // 15. 구취 / 입냄새 / 소화 불량 / 속이 더부룩
        if (lowerText.contains("구취") || lowerText.contains("입냄새") || lowerText.contains("소화") ||
            lowerText.contains("더부룩") || lowerText.contains("속이 안 좋") || lowerText.contains("속이 불편") ||
            lowerText.contains("가스") || lowerText.contains("방귀") || lowerText.contains("변비") ||
            lowerText.contains("설사") || lowerText.contains("장이") || lowerText.contains("장 건강")) {
            detectedSymptoms.add("장내 미생물 불균형 및 소화 효소 활성 저하")
            deficientNutrients.add(
                NutrientDetail(
                    name = "프로바이오틱스 & 아연",
                    severity = "Mild",
                    symptomExplanation = "구취 및 소화 불량은 장내 세균총 불균형과 위산 생성에 필요한 아연 결핍의 간접 신호일 수 있습니다. 유익균을 늘려 소화 기능을 개선하세요.",
                    recommendedFoods = listOf("요거트", "김치", "된장", "귀리", "굴")
                )
            )
        }

        // 16. 집중력 저하 / 기억력 감퇴 / 멍해짐 / 뇌 안개
        if (lowerText.contains("집중") || lowerText.contains("기억") || lowerText.contains("멍") ||
            lowerText.contains("집중이 안") || lowerText.contains("기억력") || lowerText.contains("건망") ||
            lowerText.contains("뇌가") || lowerText.contains("머리가 안 돌") || lowerText.contains("머리가 무거") ||
            lowerText.contains("브레인 포그") || lowerText.contains("생각이 안")) {
            detectedSymptoms.add("인지 기능 저하 및 신경전달물질 합성 부족")
            deficientNutrients.add(
                NutrientDetail(
                    name = "오메가-3 & 비타민 B6",
                    severity = "Moderate",
                    symptomExplanation = "뇌 세포막을 구성하는 DHA(오메가-3)와 신경전달물질(세로토닌·도파민) 합성에 관여하는 비타민 B6가 부족하면 집중력·기억력이 저하됩니다.",
                    recommendedFoods = listOf("고등어", "호두", "아보카도", "닭가슴살", "바나나")
                )
            )
        }

        // 17. 면역력 저하 / 자주 감기 / 상처가 안 낫는다
        if (lowerText.contains("면역") || lowerText.contains("감기") || lowerText.contains("자주 아프") ||
            lowerText.contains("상처") || lowerText.contains("회복") || lowerText.contains("낫지 않") ||
            lowerText.contains("상처가 안") || lowerText.contains("염증") || lowerText.contains("면역력이") ||
            lowerText.contains("병에 자주") || lowerText.contains("잔병") || lowerText.contains("코감기")) {
            detectedSymptoms.add("면역 세포 활성 저하 및 항산화 방어 체계 약화")
            deficientNutrients.add(
                NutrientDetail(
                    name = "비타민 C & 아연",
                    severity = "High",
                    symptomExplanation = "면역 세포(NK세포·T세포) 증식에 필수적인 비타민 C와 상처 아물기·항바이러스 반응에 관여하는 아연 부족 시 잦은 감염과 회복 지연이 나타납니다.",
                    recommendedFoods = listOf("귤", "키위", "파프리카", "굴", "호박씨")
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
