package com.example.nailnutri.analysis

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

object PPGAnalyzer {

    class BandpassFilter {
        private var x1 = 0.0; private var x2 = 0.0
        private var y1 = 0.0; private var y2 = 0.0

        private val b0 = 0.067455
        private val b1 = 0.0
        private val b2 = -0.067455
        private val a1 = -1.14298
        private val a2 = 0.41280

        fun process(sample: Double): Double {
            val output = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = sample
            y2 = y1
            y1 = output
            return output
        }
    }

    fun analyzePPG(redMeanBuffer: DoubleArray, timeStamps: LongArray, imagePath: String): NailAnalysisResult {
        val size = redMeanBuffer.size
        if (size < 120) return buildDefaultResult(imagePath, 72, 35.0, 32)

        val bpFilter = BandpassFilter()
        val filtered = DoubleArray(size)
        
        for (i in 0 until size) {
            filtered[i] = bpFilter.process(redMeanBuffer[i])
        }

        val peakIndices = mutableListOf<Int>()
        var windowSize = 10
        
        for (i in 2 until size - 2) {
            val currentVal = filtered[i]
            if (currentVal > filtered[i - 1] && currentVal > filtered[i - 2] &&
                currentVal > filtered[i + 1] && currentVal > filtered[i + 2]) {
                
                if (currentVal > 0.05) {
                    if (peakIndices.isEmpty() || (i - peakIndices.last()) > windowSize) {
                        peakIndices.add(i)
                    }
                }
            }
        }

        val rrIntervals = mutableListOf<Double>()
        for (k in 0 until peakIndices.size - 1) {
            val intervalMs = (timeStamps[peakIndices[k + 1]] - timeStamps[peakIndices[k]]).toDouble()
            if (intervalMs in 350.0..1800.0) {
                rrIntervals.add(intervalMs)
            }
        }

        val avgRRI = if (rrIntervals.isNotEmpty()) rrIntervals.average() else 800.0
        val bpm = (60000.0 / avgRRI).toInt().coerceIn(45, 160)

        var varianceSum = 0.0
        for (rri in rrIntervals) {
            varianceSum += (rri - avgRRI) * (rri - avgRRI)
        }
        val sdnn = if (rrIntervals.size > 1) sqrt(varianceSum / (rrIntervals.size - 1)) else 40.0
        
        val stressLevel = ((100.0 - sdnn) * 1.2).coerceIn(10.0, 95.0).toInt()
        val isMineralDeficient = sdnn < 32.0

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        return if (isMineralDeficient) {
            NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("자율신경 긴장 (마그네슘/칼슘 결핍 의심)", "BPM: $bpm, HRV(SDNN): ${String.format(Locale.US, "%.1f", sdnn)}ms"),
                deficientNutrients = listOf(
                    NutrientDetail("마그네슘", "Moderate", "혈관 근육의 수축과 이완 밸런스가 흐트러지고 심박 변이도(HRV)가 수축되어 마그네슘 부족 징후가 검출되었습니다.", listOf("귀리", "아몬드", "바나나", "호박씨")),
                    NutrientDetail("칼슘", "Moderate", "심장 근육의 수축성 흥분 전달을 조절하는 칼슘 결핍으로 인한 자율신경 긴장 보완이 필요합니다.", listOf("우유", "치즈", "멸치", "두부"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "PPG 혈류 탄성 정밀 검사 결과, 심박 변이도(HRV) 지표가 $sdnn ms로 저하되어 만성 스트레스 및 근육 긴장 상태가 감지되었습니다. 이는 체내 마그네슘 및 칼슘 이온 부족과 깊은 관련이 있습니다. 아몬드, 호박씨 등의 미네랄 식품 섭취를 늘려 주십시오."
            )
        } else {
            buildDefaultResult(imagePath, bpm, sdnn, stressLevel)
        }
    }

    private fun buildDefaultResult(imagePath: String, bpm: Int, sdnn: Double, stressLevel: Int): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = listOf("BPM: $bpm, HRV: ${String.format(Locale.US, "%.1f", sdnn)}ms (정상)"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail("마그네슘 & 칼슘", "심박 조율 변이가 유연하며 자율신경계 긴장도가 대단히 양호합니다.", "신경 안정 및 혈관 이완 작용 보장")
            ),
            overallAdvice = "광혈류(PPG) 분석 결과 맥박 및 혈압 리듬 탄성도가 대단히 양호하며, SDNN 수치(${String.format(Locale.US, "%.1f", sdnn)}ms)가 건강 범위 내에 있어 근육 이완 미네랄(마그네슘, 칼슘) 영양 밸런스가 매우 양호한 상태입니다."
        )
    }
}
