package com.example.nailnutri.analysis

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

object VoiceAnalyzer {

    fun analyzeVoice(audioSamples: ShortArray, imagePath: String): NailAnalysisResult {
        return analyzeVoiceWithText(recognizedText = "", audioSamples = audioSamples, imagePath = imagePath)
    }

    fun analyzeVoiceWithText(
        recognizedText: String,
        audioSamples: ShortArray? = null,
        imagePath: String = "voice_analysis_record.wav"
    ): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        // 1. STT 텍스트 증상 파싱 Engine 수행
        val symptomResult = if (recognizedText.isNotBlank()) {
            SymptomNutrientEngine.analyzeSymptomText(recognizedText)
        } else {
            null
        }

        // 2. 음향 신호처리 (Jitter / Shimmer) 피로도 검사 수행
        var jitter = 0.005
        var shimmer = 0.01
        var isFatiguedByAudio = false

        if (audioSamples != null && audioSamples.size >= 2000) {
            val length = audioSamples.size
            val frameSize = 512
            val overlap = 256
            val numFrames = (length - frameSize) / overlap
            val pitches = mutableListOf<Double>()
            val amplitudes = mutableListOf<Double>()

            for (f in 0 until numFrames) {
                val start = f * overlap
                var maxCorr = 0.0
                var bestLag = -1
                var ampSum = 0.0

                for (lag in 40 until 160) {
                    var corr = 0.0
                    for (i in 0 until frameSize - lag) {
                        corr += (audioSamples[start + i].toDouble() * audioSamples[start + i + lag].toDouble())
                    }
                    if (corr > maxCorr) {
                        maxCorr = corr
                        bestLag = lag
                    }
                }

                for (i in 0 until frameSize) {
                    ampSum += abs(audioSamples[start + i].toDouble())
                }

                if (bestLag != -1) {
                    pitches.add(8000.0 / bestLag)
                    amplitudes.add(ampSum / frameSize)
                }
            }

            var jitterSum = 0.0
            for (i in 0 until pitches.size - 1) {
                jitterSum += abs(pitches[i] - pitches[i + 1])
            }
            jitter = if (pitches.size > 1) (jitterSum / (pitches.size - 1)) / (pitches.average() + 1.0) else 0.005

            var shimmerSum = 0.0
            for (i in 0 until amplitudes.size - 1) {
                shimmerSum += abs(amplitudes[i] - amplitudes[i + 1])
            }
            shimmer = if (amplitudes.size > 1) (shimmerSum / (amplitudes.size - 1)) / (amplitudes.average() + 1.0) else 0.01

            isFatiguedByAudio = jitter > 0.04 || shimmer > 0.075
        }

        // 3. 증상 및 영양소 통합 (STT 결과 + Audio 결과)
        val combinedSymptoms = mutableListOf<String>()
        val combinedDeficient = mutableListOf<NutrientDetail>()
        val combinedSufficient = mutableListOf<SufficientNutrientDetail>()

        if (symptomResult != null) {
            combinedSymptoms.addAll(symptomResult.detectedSymptoms)
            combinedDeficient.addAll(symptomResult.deficientNutrients)
            combinedSufficient.addAll(symptomResult.sufficientNutrients)
        }

        if (isFatiguedByAudio) {
            combinedSymptoms.add("음성 주파수 흔들림 (성대 미세 지터 ${String.format(Locale.US, "%.1f", jitter * 100)}%)")
            val containsVitB = combinedDeficient.any { it.name.contains("비타민 B") }
            if (!containsVitB) {
                combinedDeficient.add(
                    NutrientDetail(
                        name = "비타민 B12",
                        severity = "Moderate",
                        symptomExplanation = "발성 기질 진동 제어 안정도 저하로 인한 신경 전달 보효소 결핍 가능성",
                        recommendedFoods = listOf("육류", "조개류", "연어", "우유")
                    )
                )
            }
        }

        if (combinedSymptoms.isEmpty()) {
            combinedSymptoms.add("안정적인 음성 톤 및 양호한 상태")
        }

        if (combinedDeficient.isEmpty() && combinedSufficient.isEmpty()) {
            combinedSufficient.add(
                SufficientNutrientDetail(
                    name = "비타민 B군 & 필수 미네랄",
                    symptomExplanation = "발성 안정도 및 텍스트 증상 분석 결과 체내 영양 밸런스가 매우 안정적입니다.",
                    role = "신경 수초 보호 및 정상 에너지 대사"
                )
            )
        }

        val adviceText = buildString {
            if (recognizedText.isNotBlank()) {
                append("인식된 음성: \"$recognizedText\"\n")
            }
            if (symptomResult != null) {
                append(symptomResult.advice)
            } else if (isFatiguedByAudio) {
                append("성대 발성 미세 조율 안정도 저하가 감지되어 신경계 보효소인 비타민 B군 영양 섭취를 추천합니다.")
            } else {
                append("발성 피치 안정성 및 증상 파싱 결과 특이 소견이 발견되지 않은 양호한 영양 상태입니다.")
            }
        }

        return NailAnalysisResult(
            id = mockId,
            date = dateStr,
            imagePath = imagePath,
            symptoms = combinedSymptoms,
            deficientNutrients = combinedDeficient,
            sufficientNutrients = combinedSufficient,
            overallAdvice = adviceText
        )
    }
}
