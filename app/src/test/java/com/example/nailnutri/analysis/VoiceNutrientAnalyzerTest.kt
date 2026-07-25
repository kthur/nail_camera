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
}
