package com.example.nailnutri.analysis

import org.junit.Assert.*
import org.junit.Test

class VoiceNutrientAnalyzerTest {

    @Test
    fun testSymptomEyeTwitchingAndFatigue() {
        val input = "요즘 눈밑이 자주 떨리고 너무 피곤합니다"
        val result = SymptomNutrientEngine.analyzeSymptomText(input)

        assertTrue("증상에 눈밑/경련이 포함되어야 함", result.detectedSymptoms.any { it.contains("눈밑") || it.contains("경련") })
        assertTrue("결핍 영양소에 마그네슘이 포함되어야 함", result.deficientNutrients.any { it.name.contains("마그네슘") })
    }

    @Test
    fun testSymptomMouthUlcer() {
        val input = "입안이 자꾸 헐고 혀가 아파요"
        val result = SymptomNutrientEngine.analyzeSymptomText(input)

        assertTrue("결핍 영양소에 비타민 B군이 포함되어야 함", result.deficientNutrients.any { it.name.contains("비타민 B") })
        assertTrue("추천 음식에 계란이나 시금치가 포함되어야 함", result.deficientNutrients.any { it.recommendedFoods.contains("계란") || it.recommendedFoods.contains("시금치") })
    }

    @Test
    fun testSymptomNailBrittle() {
        val input = "손톱이 잘 갈라지고 자꾸 부러집니다"
        val result = SymptomNutrientEngine.analyzeSymptomText(input)

        assertTrue("결핍 영양소에 아연이나 비오틴이 포함되어야 함", result.deficientNutrients.any { it.name.contains("아연") || it.name.contains("비오틴") })
    }

    @Test
    fun testSymptomDizzinessAndAnemia() {
        val input = "어지럽고 얼굴이 창백해 보여요"
        val result = SymptomNutrientEngine.analyzeSymptomText(input)

        assertTrue("결핍 영양소에 철분이 포함되어야 함", result.deficientNutrients.any { it.name.contains("철분") })
    }

    @Test
    fun testVoiceAnalyzerIntegration() {
        val textInput = "눈밑이 떨리고 입안이 헐었어요"
        val analysisResult = VoiceAnalyzer.analyzeVoiceWithText(recognizedText = textInput)

        assertNotNull(analysisResult.id)
        assertTrue("symptoms 리스트가 비어있지 않아야 함", analysisResult.symptoms.isNotEmpty())
        assertTrue("deficientNutrients 리스트가 비어있지 않아야 함", analysisResult.deficientNutrients.isNotEmpty())
        assertTrue("overallAdvice에 인식된 텍스트가 명시되어야 함", analysisResult.overallAdvice.contains(textInput))
    }

    // --- B1: 새 키워드 커버리지 테스트 ---

    @Test
    fun testSymptomSkinTrouble() {
        val result = SymptomNutrientEngine.analyzeSymptomText("요즘 피부 트러블이 심하고 각질이 많이 일어나요")
        assertTrue("비타민 A 또는 오메가-3 감지", result.deficientNutrients.any { it.name.contains("비타민 A") || it.name.contains("오메가") })
    }

    @Test
    fun testSymptomDryLips() {
        val result = SymptomNutrientEngine.analyzeSymptomText("입술이 자꾸 갈라지고 입꼬리가 헐어요")
        assertTrue("비타민 B2 또는 철분 감지", result.deficientNutrients.any { it.name.contains("B2") || it.name.contains("철분") })
    }

    @Test
    fun testSymptomNumbness() {
        val result = SymptomNutrientEngine.analyzeSymptomText("손발이 자주 저리고 찌릿찌릿해요")
        assertTrue("비타민 B12 또는 비타민 E 감지", result.deficientNutrients.any { it.name.contains("B12") || it.name.contains("비타민 E") })
    }

    @Test
    fun testSymptomEdema() {
        val result = SymptomNutrientEngine.analyzeSymptomText("아침마다 얼굴이 붓고 다리도 부어요")
        assertTrue("칼륨 또는 비타민 B1 감지", result.deficientNutrients.any { it.name.contains("칼륨") || it.name.contains("B1") })
    }

    @Test
    fun testSymptomVisionProblem() {
        val result = SymptomNutrientEngine.analyzeSymptomText("눈이 침침하고 안구가 건조해서 뻑뻑해요")
        assertTrue("비타민 A 또는 루테인 감지", result.deficientNutrients.any { it.name.contains("비타민 A") || it.name.contains("루테인") })
    }

    @Test
    fun testSymptomDigestion() {
        val result = SymptomNutrientEngine.analyzeSymptomText("소화가 잘 안 되고 더부룩하며 변비가 있어요")
        assertTrue("프로바이오틱스 또는 아연 감지", result.deficientNutrients.any { it.name.contains("프로바이오틱스") || it.name.contains("아연") })
    }

    @Test
    fun testSymptomConcentration() {
        val result = SymptomNutrientEngine.analyzeSymptomText("집중력이 떨어지고 기억력이 안 좋아졌어요")
        assertTrue("오메가-3 또는 비타민 B6 감지", result.deficientNutrients.any { it.name.contains("오메가") || it.name.contains("B6") })
    }

    @Test
    fun testSymptomImmunity() {
        val result = SymptomNutrientEngine.analyzeSymptomText("면역력이 떨어진 것 같아요. 감기를 자주 달고 살아요")
        assertTrue("비타민 C 또는 아연 감지", result.deficientNutrients.any { it.name.contains("비타민 C") || it.name.contains("아연") })
    }

    @Test
    fun testSymptomFatigueBurnout() {
        val result = SymptomNutrientEngine.analyzeSymptomText("요즘 번아웃인지 기력이 없고 나른하게 졸려요")
        assertTrue("비타민 B군 감지", result.deficientNutrients.any { it.name.contains("비타민 B") })
    }

    @Test
    fun testSymptomJointAndSunlight() {
        val result = SymptomNutrientEngine.analyzeSymptomText("실내에만 있어서 햇볕을 못 받고 무릎이 아파요")
        assertTrue("비타민 D 또는 칼슘 감지", result.deficientNutrients.any { it.name.contains("비타민 D") || it.name.contains("칼슘") })
    }

    @Test
    fun testUnknownSymptom_returnsHealthyFallback() {
        val result = SymptomNutrientEngine.analyzeSymptomText("오늘 날씨가 맑고 기분이 좋아요")
        assertTrue("미감지 시 양호 메시지", result.sufficientNutrients.isNotEmpty())
        assertTrue("미감지 시 결핍 없음", result.deficientNutrients.isEmpty())
    }
}
