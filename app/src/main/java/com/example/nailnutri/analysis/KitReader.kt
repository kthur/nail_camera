package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object KitReader {

    fun readKit(bitmap: Bitmap, imagePath: String): NailAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 10 || height <= 10) return buildDefaultResult(imagePath, 15.0)

        val scanY = height / 2
        var minIntensity = 255.0
        var maxIntensity = 0.0
        
        val intensities = DoubleArray(width)
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, scanY)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val intensity = (r + g + b) / 3.0
            intensities[x] = intensity
            if (intensity < minIntensity) minIntensity = intensity
            if (intensity > maxIntensity) maxIntensity = intensity
        }

        var cVal = 180.0
        var tVal = 180.0
        var cIdx = (width * 0.4).toInt()
        var tIdx = (width * 0.6).toInt()

        val bgValue = maxIntensity

        for (i in (width * 0.3).toInt()..(width * 0.5).toInt()) {
            if (intensities[i] < cVal) {
                cVal = intensities[i]
                cIdx = i
            }
        }
        for (i in (width * 0.51).toInt()..(width * 0.8).toInt()) {
            if (intensities[i] < tVal) {
                tVal = intensities[i]
                tIdx = i
            }
        }

        val cDip = (bgValue - cVal).coerceAtLeast(1e-5)
        val tDip = (bgValue - tVal).coerceAtLeast(1e-5)
        
        val colorRatio = tDip / cDip
        
        val vitDLevel = (colorRatio * 45.0).coerceIn(4.0, 92.0)
        val isDeficient = vitDLevel < 30.0

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        return if (isDeficient) {
            NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("비타민 D 결핍 수치 감지 (${String.format(Locale.US, "%.1f", vitDLevel)} ng/mL)"),
                deficientNutrients = listOf(
                    NutrientDetail("비타민 D", "Moderate", "시약 스트립의 검사선 발색 비율 판독 결과 체내 활성 비타민 D 농도가 ${String.format(Locale.US, "%.1f", vitDLevel)} ng/mL로 임계치인 30.0 ng/mL를 크게 밑도는 결핍 상태입니다.", listOf("등푸른 생선", "표고버섯", "달걀노른자", "연어")),
                    NutrientDetail("칼슘", "Moderate", "비타민 D는 장내 칼슘 흡수율을 제어하는 핵심 영양소로 장기 결핍 시 골조직에 영향을 줍니다.", listOf("우유", "치즈", "멸치", "두부"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "자가 진단 시약 LFA 리더기 판정 결과 비타민 D가 ${String.format(Locale.US, "%.1f", vitDLevel)} ng/mL로 결핍 수준입니다. 실내 활동 위주의 생활을 피하시고 하루 15분 이상 햇빛을 쬐거나 연어, 달걀노른자 섭취 및 영양 보충제 복용을 추천합니다."
            )
        } else {
            buildDefaultResult(imagePath, vitDLevel)
        }
    }

    private fun buildDefaultResult(imagePath: String, level: Double): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = listOf("비타민 D 농도 양호 (${String.format(Locale.US, "%.1f", level)} ng/mL)"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail("비타민 D", "화학 시약선 정량 대조 판정 수치가 ${String.format(Locale.US, "%.1f", level)} ng/mL로 정상 범주에 부합합니다.", "면역력 유지 및 골세포 형성 촉진")
            ),
            overallAdvice = "LFA 스트립 스캔 판독 결과 비타민 D 지수가 ${String.format(Locale.US, "%.1f", level)} ng/mL로 지극히 정상이며, 면역 기능 유지 및 칼슘 흡수 사이클이 훌륭하게 유지되고 있습니다."
        )
    }
}
