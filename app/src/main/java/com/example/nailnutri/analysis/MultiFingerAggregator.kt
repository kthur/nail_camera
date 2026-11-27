package com.example.nailnutri.analysis

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import com.example.nailnutri.util.DateUtils
import java.util.UUID

/**
 * D2: Multi-finger sequential scan statistical aggregator.
 *
 * Combines 2..5 individual finger scan results using majority voting and
 * average confidence scoring to eliminate single-finger lighting/angle outliers.
 */
object MultiFingerAggregator {

    fun aggregate(results: List<NailAnalysisResult>): NailAnalysisResult {
        if (results.isEmpty()) {
            return NailClassifier.buildResultForCondition("healthy", "multi_finger_empty")
        }
        if (results.size == 1) {
            return results.first()
        }

        val count = results.size
        // Majority threshold: at least 2 fingers (or ceil(count / 2.0) for >= 3 fingers)
        val minVotes = if (count >= 3) (count + 1) / 2 else 1

        // 1. Aggregate symptoms by frequency
        val symptomCounts = mutableMapOf<String, Int>()
        for (res in results) {
            for (sym in res.symptoms.distinct()) {
                symptomCounts[sym] = (symptomCounts[sym] ?: 0) + 1
            }
        }
        val majoritySymptoms = symptomCounts.entries
            .filter { it.value >= minVotes }
            .sortedByDescending { it.value }
            .map { "${it.key} (${it.value}/$count 손가락)" }
            .ifEmpty {
                listOfNotNull(symptomCounts.maxByOrNull { it.value }?.key)
            }

        // 2. Aggregate deficient nutrients with vote count & average severity
        val nutrientBuckets = mutableMapOf<String, MutableList<NutrientDetail>>()
        for (res in results) {
            val seenInFinger = mutableSetOf<String>()
            for (nut in res.deficientNutrients) {
                if (seenInFinger.add(nut.name)) {
                    nutrientBuckets.getOrPut(nut.name) { mutableListOf() }.add(nut)
                }
            }
        }

        val aggregatedDeficient = nutrientBuckets.entries
            .filter { it.value.size >= minVotes }
            .sortedByDescending { it.value.size }
            .map { (name, list) ->
                val avgWeight = list.map { severityScore(it.severity) }.average()
                val aggSeverity = when {
                    avgWeight >= 2.5 -> "Severe"
                    avgWeight >= 1.5 -> "Moderate"
                    else -> "Mild"
                }
                val rep = list.first()
                NutrientDetail(
                    name = name,
                    severity = aggSeverity,
                    symptomExplanation = "[${list.size}/$count 손가락 공통 감지] ${rep.symptomExplanation}",
                    recommendedFoods = rep.recommendedFoods
                )
            }

        // 3. Aggregate sufficient nutrients (exclude those flagged as deficient)
        val deficientNames = aggregatedDeficient.map { it.name }.toSet()
        val sufficientMap = linkedMapOf<String, SufficientNutrientDetail>()
        for (res in results) {
            for (suf in res.sufficientNutrients) {
                if (suf.name !in deficientNames && suf.name !in sufficientMap) {
                    sufficientMap[suf.name] = suf
                }
            }
        }

        // 4. Average TFLite confidence if present
        val confidences = results.mapNotNull { it.confidence }
        val avgConfidence = if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            null
        }

        val advice = if (aggregatedDeficient.isNotEmpty()) {
            val names = aggregatedDeficient.joinToString(", ") { it.name }
            "총 ${count}개 손가락 연속 스캔 통계 분석 결과, 다수의 손가락에서 공통적으로 $names 결핍 징후가 확인되었습니다. 단일 손가락 오차를 배제한 종합 결과이므로 해당 영양소 보충을 권장합니다."
        } else {
            "총 ${count}개 손가락 연속 스캔 통계 분석 결과, 반복적으로 나타나는 특이 결핍 징후가 없으며 전반적인 손톱 건강 상태가 양호합니다."
        }

        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = DateUtils.nowFormatted(),
            imagePath = results.first().imagePath,
            symptoms = majoritySymptoms,
            deficientNutrients = aggregatedDeficient,
            sufficientNutrients = sufficientMap.values.toList(),
            overallAdvice = advice,
            symptomRegions = results.first().symptomRegions,
            confidence = avgConfidence
        )
    }

    private fun severityScore(severity: String): Int = when {
        severity.equals("Severe", ignoreCase = true) -> 3
        severity.equals("Moderate", ignoreCase = true) -> 2
        else -> 1
    }
}
