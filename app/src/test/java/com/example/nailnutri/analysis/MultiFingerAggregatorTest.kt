package com.example.nailnutri.analysis

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import org.junit.Assert.*
import org.junit.Test

class MultiFingerAggregatorTest {

    private fun makeFingerResult(
        idx: Int,
        symptom: String,
        deficient: List<NutrientDetail> = emptyList(),
        sufficient: List<SufficientNutrientDetail> = emptyList(),
        confidence: Float? = null
    ) = NailAnalysisResult(
        id = "f_$idx",
        date = "2026-11-27 22:00",
        imagePath = "finger_$idx.jpg",
        symptoms = listOf(symptom),
        deficientNutrients = deficient,
        sufficientNutrients = sufficient,
        overallAdvice = "Advice $idx",
        confidence = confidence
    )

    @Test
    fun testEmptyList_returnsHealthyDefault() {
        val result = MultiFingerAggregator.aggregate(emptyList())
        assertNotNull(result)
        assertTrue(result.symptoms.isNotEmpty())
    }

    @Test
    fun testSingleFinger_returnsSameResult() {
        val single = makeFingerResult(1, "창백함", confidence = 0.8f)
        val result = MultiFingerAggregator.aggregate(listOf(single))
        assertEquals(single.id, result.id)
        assertEquals(0.8f, result.confidence!!, 1e-6f)
    }

    @Test
    fun testMajorityVoting_filtersSingleOutlierAndKeepsCommonDeficiency() {
        val iron = NutrientDetail("철분 (Iron)", "Moderate", "빈혈 징후", listOf("소고기"))
        val zinc = NutrientDetail("아연 (Zinc)", "Severe", "흰 반점", listOf("굴"))

        // 4 out of 5 fingers show Iron deficiency; only 1 finger shows Zinc (noise/outlier)
        val fingers = listOf(
            makeFingerResult(1, "창백함", deficient = listOf(iron), confidence = 0.80f),
            makeFingerResult(2, "창백함", deficient = listOf(iron), confidence = 0.85f),
            makeFingerResult(3, "창백함", deficient = listOf(iron), confidence = 0.90f),
            makeFingerResult(4, "창백함", deficient = listOf(iron, zinc), confidence = 0.75f),
            makeFingerResult(5, "정상", sufficient = listOf(SufficientNutrientDetail("비오틴", "양호", "보호")), confidence = 0.70f)
        )

        val aggregated = MultiFingerAggregator.aggregate(fingers)

        assertTrue("Majority Iron deficiency should be retained",
            aggregated.deficientNutrients.any { it.name.contains("철분") }
        )
        assertFalse("Single-finger Zinc outlier should be filtered out",
            aggregated.deficientNutrients.any { it.name.contains("아연") }
        )
        assertTrue("Symptom should show 4/5 count",
            aggregated.symptoms.any { it.contains("4/5") }
        )
        assertEquals(0.80f, aggregated.confidence!!, 1e-4f)
    }

    @Test
    fun testSeverityAveraging_promotesSevereWhenMajorityAreSevere() {
        val severeZinc = NutrientDetail("아연 (Zinc)", "Severe", "심한 결핍", listOf("굴"))
        val moderateZinc = NutrientDetail("아연 (Zinc)", "Moderate", "중간 결핍", listOf("굴"))

        val fingers = listOf(
            makeFingerResult(1, "흰 반점", deficient = listOf(severeZinc)),
            makeFingerResult(2, "흰 반점", deficient = listOf(severeZinc)),
            makeFingerResult(3, "흰 반점", deficient = listOf(moderateZinc))
        )

        val aggregated = MultiFingerAggregator.aggregate(fingers)
        val zincResult = aggregated.deficientNutrients.first { it.name.contains("아연") }
        // (3 + 3 + 2) / 3 = 2.67 >= 2.5 -> "Severe"
        assertEquals("Severe", zincResult.severity)
    }
}
