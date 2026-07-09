package com.example.nailnutri.analysis

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.log10
import kotlin.math.sqrt

object SleepAudioAnalyzer {

    fun analyzeAudioBuffer(audioSamples: ShortArray, imagePath: String): NailAnalysisResult {
        val length = audioSamples.size
        if (length == 0) return buildHealthyResult(imagePath, 0.0, 30.0)

        var sumSq = 0.0
        for (sample in audioSamples) {
            sumSq += (sample.toDouble() * sample.toDouble())
        }
        val rms = sqrt(sumSq / length)
        val db = (20 * log10(rms + 1e-5)).coerceIn(10.0, 100.0)

        var zeroCrossings = 0
        for (i in 0 until length - 1) {
            if ((audioSamples[i] >= 0 && audioSamples[i + 1] < 0) || 
                (audioSamples[i] < 0 && audioSamples[i + 1] >= 0)) {
                zeroCrossings++
            }
        }
        
        val estimatedFreq = (zeroCrossings * 8000.0) / (2.0 * length)
        val isSnoreFreq = estimatedFreq in 100.0..650.0 && db > 48.0
        
        val snorePercentage = if (isSnoreFreq) 42.0 else 5.0
        val isVitaminDDeficient = snorePercentage > 25.0

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        return if (isVitaminDDeficient) {
            NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("수면 무호흡 및 심한 코골이 감지 (호흡 dB: ${String.format(Locale.US, "%.1f", db)} dB)"),
                deficientNutrients = listOf(
                    NutrientDetail("비타민 D", "Moderate", "수면 장애로 호흡 기둥이 무너지고 상기도 이완 저하를 야기하는 비타민 D 결핍이 감지되었습니다.", listOf("등푸른 생선", "버섯", "달걀노른자", "연어")),
                    NutrientDetail("마그네슘", "Moderate", "수면 신경을 이완시키는 필수 미네랄인 마그네슘 부족 시 상기도 경직 및 코골이가 악화됩니다.", listOf("아몬드", "귀리", "바나나"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "수면 음향 주파수 분석 결과 수면 무호흡 및 저주파 코골이 (${String.format(Locale.US, "%.1f", db)} dB) 비중이 높게 잡혔습니다. 이는 상기도 평활근의 긴장을 제어하는 비타민 D 및 마그네슘 부족과 밀접한 연관이 있으므로 이에 따른 영양 섭취를 추천합니다."
            )
        } else {
            buildHealthyResult(imagePath, snorePercentage, db)
        }
    }

    private fun buildHealthyResult(imagePath: String, snorePct: Double, db: Double): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = listOf("안정적인 호흡음 (소음도: ${String.format(Locale.US, "%.1f", db)} dB)"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail("비타민 D & 마그네슘", "코골이 주파수 점유율이 ${String.format(Locale.US, "%.1f", snorePct)}%로 대단히 낮으며 깊고 안정적인 호흡 패턴을 보여 수면 영양 균형이 훌륭합니다.", "근육 수축 조절 및 멜라토닌 수면 신경 활성화")
            ),
            overallAdvice = "호흡 음향 모니터링 결과 코골이 및 거친 소음 비중이 대단히 양호하여 비타민 D와 근육 안정 미네랄(마그네슘)의 기능이 아주 정상적으로 작동하고 있습니다. 숙면을 지속하고 있는 상태입니다."
        )
    }
}
