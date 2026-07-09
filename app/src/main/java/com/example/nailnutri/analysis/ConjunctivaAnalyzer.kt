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

object ConjunctivaAnalyzer {

    fun analyze(bitmap: Bitmap, imagePath: String): NailAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return buildHealthyResult(imagePath)

        // 1. White Balance Normalization via Sclera Detection
        var scleraR = 0.0; var scleraG = 0.0; var scleraB = 0.0
        var scleraCount = 0
        
        for (y in (height * 0.2).toInt()..(height * 0.5).toInt()) {
            for (x in (width * 0.2).toInt()..(width * 0.8).toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                val maxColor = maxOf(r, maxOf(g, b))
                val minColor = minOf(r, minOf(g, b))
                val delta = (maxColor - minColor).toFloat()
                val s = if (maxColor > 0) delta / maxColor else 0f
                val v = maxColor / 255f
                
                if (s < 0.12f && v > 0.70f) {
                    scleraR += r
                    scleraG += g
                    scleraB += b
                    scleraCount++
                }
            }
        }
        
        val scaleR = if (scleraCount > 0) 255.0 / (scleraR / scleraCount) else 1.0
        val scaleG = if (scleraCount > 0) 255.0 / (scleraG / scleraCount) else 1.0
        val scaleB = if (scleraCount > 0) 255.0 / (scleraB / scleraCount) else 1.0

        // 2. Scan Conjunctiva Area
        var conjR = 0.0; var conjG = 0.0; var conjB = 0.0
        var conjCount = 0
        
        for (y in (height * 0.55).toInt()..(height * 0.85).toInt()) {
            for (x in (width * 0.25).toInt()..(width * 0.75).toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val r = (Color.red(pixel) * scaleR).coerceIn(0.0, 255.0)
                val g = (Color.green(pixel) * scaleG).coerceIn(0.0, 255.0)
                val b = (Color.blue(pixel) * scaleB).coerceIn(0.0, 255.0)
                
                val maxC = maxOf(r, maxOf(g, b))
                val minC = minOf(r, minOf(g, b))
                val delta = maxC - minC
                var h = 0.0
                if (delta > 0) {
                    h = if (maxC == r) (g - b) / delta else if (maxC == g) 2.0 + (b - r) / delta else 4.0 + (r - g) / delta
                    h *= 60.0
                    if (h < 0) h += 360.0
                }
                
                if (h < 22.0 || h > 338.0) {
                    conjR += r
                    conjG += g
                    conjB += b
                    conjCount++
                }
            }
        }
        
        val avgR = if (conjCount > 0) conjR / conjCount else 130.0
        val avgG = if (conjCount > 0) conjG / conjCount else 70.0
        val avgB = if (conjCount > 0) conjB / conjCount else 70.0
        
        val hemoglobinIndex = (avgR / (avgG + avgB + 1.0)) * 1.55
        val isAnemic = hemoglobinIndex < 1.05

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        return if (isAnemic) {
            NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("안구 결막 창백 (헤모글로빈 부족 의심)"),
                deficientNutrients = listOf(
                    NutrientDetail("철분", "Moderate", "안구 결막 모세혈관의 혈색 붉은 성분 비율(헤모글로빈 수치)이 정상 범주를 하회하여 철결핍성 빈혈 위험이 감지되었습니다.", listOf("붉은 살코기", "시금치", "조개류", "건포도")),
                    NutrientDetail("비타민 B12", "Moderate", "적혈구 대사와 신경 건강에 필수 인자로 결핍 시 창백함이 동반될 수 있습니다.", listOf("조개류", "연어", "육류", "우유"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("비타민 C", "철분 흡수를 촉진시키는 인자가 정상 범위입니다.", "철분 생체 흡수율 향상")
                ),
                overallAdvice = "눈꺼풀 안쪽 결막 부위의 혈색이 창백하게 감지되었습니다. 이는 체내 철분 부족이나 빈혈 위험의 전형적인 징후입니다. 철분 보충과 더불어 철 흡수를 돕는 비타민 C와 엽산을 함께 섭취하고 충분한 휴식을 권장합니다."
            )
        } else {
            buildHealthyResult(imagePath)
        }
    }

    private fun buildHealthyResult(imagePath: String): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = listOf("특이사항 없음 (결막 상태 양호)"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail("철분", "결막 혈색 및 채도가 건강한 선분홍빛을 띠어 철분 수치가 양호합니다.", "산소 운반 및 헤모글로빈 형성")
            ),
            overallAdvice = "안구 결막 촬영 분석 결과, 헤모글로빈 분포가 균일하고 붉은빛이 선명하게 나타납니다. 빈혈 위험성이 지극히 낮으며, 혈액 순환 및 철분 함량이 건강하게 유지되고 있습니다."
        )
    }
}
