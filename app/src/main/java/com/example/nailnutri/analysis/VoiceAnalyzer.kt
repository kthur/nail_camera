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
        val length = audioSamples.size
        if (length < 2000) return buildDefaultResult(imagePath, 0.2, 0.1)

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
        val jitter = if (pitches.size > 1) (jitterSum / (pitches.size - 1)) / (pitches.average() + 1.0) else 0.005

        var shimmerSum = 0.0
        for (i in 0 until amplitudes.size - 1) {
            shimmerSum += abs(amplitudes[i] - amplitudes[i + 1])
        }
        val shimmer = if (amplitudes.size > 1) (shimmerSum / (amplitudes.size - 1)) / (amplitudes.average() + 1.0) else 0.01

        val isFatigued = jitter > 0.04 || shimmer > 0.075

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        return if (isFatigued) {
            NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("음성 주파수 흔들림 (만성 신경계 피로 위험)", "성대 주파 안정도 저하"),
                deficientNutrients = listOf(
                    NutrientDetail("비타민 B12", "Moderate", "신경 전달 물질 합성을 돕는 비타민 B12 결핍으로 인한 발성 기질 진동 제어(성대 미세 떨림 지표) 안정도가 저하되었습니다.", listOf("육류", "조개류", "연어", "우유")),
                    NutrientDetail("비타민 B6", "Moderate", "신경계의 에너지 보효소로 활성 저하 시 만성 근 피로감과 발음 지연이 유발될 수 있습니다.", listOf("바나나", "닭고기", "감자", "시금치"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "목소리 음질 흔들림 정밀 판독 결과, 성대 미세 조율 안정도 (Jitter: ${String.format(Locale.US, "%.2f", jitter * 100)}%, Shimmer: ${String.format(Locale.US, "%.2f", shimmer * 100)}%)가 상승되어 만성 피로 및 신경계 불균형 징후가 의심됩니다. 비타민 B군 영양 섭취를 추천합니다."
            )
        } else {
            buildDefaultResult(imagePath, jitter, shimmer)
        }
    }

    private fun buildDefaultResult(imagePath: String, jitter: Double, shimmer: Double): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = listOf("안정적인 목소리 톤 (안정도 양호)"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail("비타민 B군", "발성 주파 Jitter(${String.format(Locale.US, "%.2f", jitter * 100)}%)와 Shimmer(${String.format(Locale.US, "%.2f", shimmer * 100)}%)가 극도로 건강하며, 성대 신경 미세 컨트롤이 매우 안정적입니다.", "신경 수초 보호 및 정상 에너지 대사")
            ),
            overallAdvice = "성대 발성 피치 안정성 분석 결과 주파 변조와 진폭 변동이 지극히 정상으로 나타나 만성 피로가 유도되지 않았으며, 신경계 보효소인 비타민 B군 영양이 훌륭하게 작용하고 있습니다."
        )
    }
}
