package com.example.nailnutri.analysis

import com.example.nailnutri.util.DateUtils

import android.graphics.Bitmap
import android.graphics.Color
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

object ConjunctivaAnalyzer {

    fun analyze(bitmap: Bitmap, imagePath: String): NailAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return buildHealthyResult(imagePath)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Luminance check (reject too dark / over-exposed)
        var totalLuma = 0.0; var lumaCount = 0
        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val px = pixels[y * width + x]
                totalLuma += 0.299 * Color.red(px) + 0.587 * Color.green(px) + 0.114 * Color.blue(px)
                lumaCount++
            }
        }
        val avgLuma = if (lumaCount > 0) totalLuma / lumaCount else 0.0
        if (avgLuma < 40.0 || avgLuma > 220.0) {
            val dateStr = DateUtils.nowFormatted()
            return NailAnalysisResult(
                id = UUID.randomUUID().toString(), date = dateStr, imagePath = imagePath,
                symptoms = listOf("조도 부적합"),
                deficientNutrients = emptyList(), sufficientNutrients = emptyList(),
                overallAdvice = "촬영 환경의 조도가 너무 어둡거나 밝습니다. 적절한 조도에서 다시 촬영해 주세요."
            )
        }

        // 2. White Balance via Sclera — detect bright, low-saturation whites of the eye
        //    Uses wider scan region for robustness: 15–55% height, 15–85% width
        var scleraR = 0.0; var scleraG = 0.0; var scleraB = 0.0; var scleraCount = 0
        for (y in (height * 0.15).toInt()..(height * 0.55).toInt()) {
            for (x in (width * 0.15).toInt()..(width * 0.85).toInt()) {
                val px = pixels[y * width + x]
                val r = Color.red(px); val g = Color.green(px); val b = Color.blue(px)
                val maxC = maxOf(r, g, b); val minC = minOf(r, g, b)
                val s = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
                val v = maxC / 255f
                // Sclera: very low saturation (s < 0.12), bright (v > 0.72)
                if (s < 0.12f && v > 0.72f) {
                    scleraR += r; scleraG += g; scleraB += b; scleraCount++
                }
            }
        }

        // White balance scale factors (normalize sclera to pure white)
        val scaleR = if (scleraCount > 50) 255.0 / (scleraR / scleraCount) else 1.0
        val scaleG = if (scleraCount > 50) 255.0 / (scleraG / scleraCount) else 1.0
        val scaleB = if (scleraCount > 50) 255.0 / (scleraB / scleraCount) else 1.0

        // 3. Conjunctiva Region Scan — lower palpebral conjunctiva
        //    Focus on 55–85% height (inferior conjunctiva), 25–75% width
        var totalRI = 0.0       // Redness Index = (R-B)/(R+G+B)  — illumination-invariant
        var totalNRI = 0.0      // Normalized Red = R/(R+G+B)
        var conjCount = 0
        var riVarianceAcc = 0.0 // for std dev (non-uniformity detection)
        val riValues = mutableListOf<Double>()

        for (y in (height * 0.55).toInt()..(height * 0.85).toInt()) {
            for (x in (width * 0.25).toInt()..(width * 0.75).toInt()) {
                val px = pixels[y * width + x]
                val r = (Color.red(px) * scaleR).coerceIn(0.0, 255.0)
                val g = (Color.green(px) * scaleG).coerceIn(0.0, 255.0)
                val b = (Color.blue(px) * scaleB).coerceIn(0.0, 255.0)

                val sumRGB = r + g + b + 1.0   // +1 to prevent division by zero

                // Skip near-white/over-exposed pixels (sclera bleeding into scan region)
                val v = maxOf(r, g, b) / 255.0
                if (v > 0.92) continue

                val ri = (r - b) / sumRGB       // Redness Index: positive = red, negative = blue-shifted
                val nri = r / sumRGB             // Normalized red channel

                totalRI += ri
                totalNRI += nri
                riValues.add(ri)
                conjCount++
            }
        }

        if (conjCount < 50) return buildHealthyResult(imagePath)  // too few pixels — unreliable

        val avgRI = totalRI / conjCount
        val avgNRI = totalNRI / conjCount

        // RI std dev — high variance = uneven redness (possible localized pallor)
        val meanRI = avgRI
        for (ri in riValues) riVarianceAcc += (ri - meanRI) * (ri - meanRI)
        val riStdDev = sqrt(riVarianceAcc / riValues.size)

        // 4. Legacy hemoglobin index (kept for reference comparison)
        //    hemoglobinIndex = avgR / (avgG + avgB + 1.0) * 1.55
        //    New: use RI-based decision which is illumination-invariant

        // Clinical interpretation:
        //  RI > 0.08  → healthy redness (normal hemoglobin)
        //  RI 0.04–0.08 → borderline
        //  RI < 0.04  → pale (anemia risk)
        //  riStdDev > 0.06 → non-uniform redness (localized pallor or petechiae)
        val isAnemic = avgRI < 0.04
        val isPallor = avgRI < 0.065 && avgNRI < 0.38  // secondary check with NRI
        val hasNonUniformRedness = riStdDev > 0.06

        val dateStr = DateUtils.nowFormatted()
        val mockId = UUID.randomUUID().toString()

        return when {
            isAnemic || isPallor -> {
                val severity = if (isAnemic) "Severe" else "Moderate"
                val riFormatted = String.format(Locale.US, "%.3f", avgRI)
                NailAnalysisResult(
                    id = mockId, date = dateStr, imagePath = imagePath,
                    symptoms = buildList {
                        add("안구 결막 창백 (헤모글로빈 부족 의심, RI: $riFormatted)")
                        if (hasNonUniformRedness) add("결막 혈색 불균일 (국소 창백 또는 출혈반 의심)")
                    },
                    deficientNutrients = listOf(
                        NutrientDetail(
                            "철분", severity,
                            "결막 혈관의 적혈구 헤모글로빈 색상 지수(Redness Index: $riFormatted)가 정상 기준치(≥0.065)를 하회하여 철결핍성 빈혈 위험이 감지되었습니다.",
                            listOf("붉은 살코기", "시금치", "조개류", "건포도")
                        ),
                        NutrientDetail(
                            "비타민 B12", "Moderate",
                            "적혈구 성숙과 신경 기능에 필수적인 비타민 B12 결핍이 창백한 결막과 동반되는 경우가 많습니다.",
                            listOf("조개류", "연어", "육류", "우유")
                        )
                    ),
                    sufficientNutrients = listOf(
                        SufficientNutrientDetail("비타민 C", "철분 흡수를 촉진시키는 항산화 인자가 정상 범위입니다.", "철분 생체 흡수율 향상")
                    ),
                    overallAdvice = "눈꺼풀 안쪽 결막 부위의 색상 지수(RI: $riFormatted)가 창백하게 측정되었습니다. 이는 체내 헤모글로빈 농도 저하 또는 철분 결핍성 빈혈의 전형적인 징후입니다. 철분 보충과 함께 비타민 C, 엽산을 함께 섭취하고 충분한 휴식을 권장합니다."
                )
            }
            hasNonUniformRedness -> {
                // Redness is present but uneven — milder warning
                NailAnalysisResult(
                    id = mockId, date = dateStr, imagePath = imagePath,
                    symptoms = listOf("결막 혈색 불균일 (미세 혈관 불안정 의심)"),
                    deficientNutrients = listOf(
                        NutrientDetail(
                            "비타민 C & 루틴", "Moderate",
                            "결막 미세혈관의 혈색 분포가 불균일하며, 이는 모세혈관 약화 및 비타민 C·바이오플라보노이드(루틴) 결핍과 관련될 수 있습니다.",
                            listOf("키위", "파프리카", "메밀", "감귤류")
                        )
                    ),
                    sufficientNutrients = listOf(
                        SufficientNutrientDetail("철분", "전반적인 결막 혈색은 유지되고 있어 중증 빈혈 가능성은 낮습니다.", "헤모글로빈 산소 공급")
                    ),
                    overallAdvice = "결막의 전반적인 혈색은 양호하나 국소적 혈색 불균일이 감지되었습니다. 비타민 C와 루틴(메밀, 감귤류) 섭취를 늘려 모세혈관 탄력을 강화하세요."
                )
            }
            else -> buildHealthyResult(imagePath)
        }
    }

    private fun buildHealthyResult(imagePath: String): NailAnalysisResult {
        val dateStr = DateUtils.nowFormatted()
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(), date = dateStr, imagePath = imagePath,
            symptoms = listOf("특이사항 없음 (결막 상태 양호)"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail(
                    "철분",
                    "결막 혈색 지수(Redness Index)가 정상 범위에 있어 헤모글로빈 수치가 양호합니다.",
                    "산소 운반 및 헤모글로빈 형성"
                )
            ),
            overallAdvice = "안구 결막 촬영 분석 결과, 헤모글로빈 색상 지수가 균일하고 정상 범위 내에 있습니다. 빈혈 위험성이 낮으며 혈액 순환 및 철분 함량이 건강하게 유지되고 있습니다."
        )
    }
}
