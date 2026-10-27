package com.example.nailnutri.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object NailClassifier {

    private fun getMultiCrops(original: Bitmap): List<Bitmap> {
        val width = original.width
        val height = original.height
        val cx = (width * 0.1).toInt()
        val cy = (height * 0.1).toInt()
        val cWidth = (width * 0.8).toInt().coerceAtLeast(1)
        val cHeight = (height * 0.8).toInt().coerceAtLeast(1)
        
        // 1. Center Crop (Core body)
        val centerCrop = Bitmap.createBitmap(original, cx, cy, cWidth, cHeight)
        
        // 2. Top Crop (Tip area)
        val tHeight = (height * 0.45).toInt().coerceAtLeast(1)
        val topCrop = Bitmap.createBitmap(original, cx, cy, cWidth, tHeight)
        
        // 3. Bottom Crop (Lunar/Cuticle area)
        val bHeight = (height * 0.45).toInt().coerceAtLeast(1)
        val bY = (height * 0.45).toInt().coerceAtMost(height - bHeight)
        val bottomCrop = Bitmap.createBitmap(original, cx, bY, cWidth, bHeight)
        
        return listOf(centerCrop, topCrop, bottomCrop)
    }

    private fun localizeSymptoms(finalLabel: String, bitmap: Bitmap): List<com.example.nailnutri.data.SymptomRegion> {
        val regions = mutableListOf<com.example.nailnutri.data.SymptomRegion>()
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return regions

        // 1. Center crop matching Android camera crop (60% width, 1.33 aspect ratio)
        val cropW = (width * 0.6).toInt().coerceAtLeast(1)
        val cropH = (cropW * 1.33).toInt().coerceAtLeast(1)
        val cropX = (width - cropW) / 2
        val cropY = (height - cropH) / 2
        val safeX = cropX.coerceIn(0, (width - cropW).coerceAtLeast(0))
        val safeY = cropY.coerceIn(0, (height - cropH).coerceAtLeast(0))

        val cropW_safe = minOf(cropW, width - safeX)
        val cropH_safe = minOf(cropH, height - safeY)

        // 2. Downsample to 150x200 to speed up scanning
        val croppedTemp = Bitmap.createBitmap(bitmap, safeX, safeY, cropW_safe, cropH_safe)
        val scaled = Bitmap.createScaledBitmap(croppedTemp, 150, 200, true)
        if (croppedTemp != bitmap && croppedTemp != scaled) {
            croppedTemp.recycle()
        }

        // Calculate illumination parameters
        var vSum = 0.0
        var pixelCount = 0
        val allV = DoubleArray(150 * 200)
        
        for (y in 0 until 200) {
            for (x in 0 until 150) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val maxVal = maxOf(r, maxOf(g, b)) / 255.0
                allV[y * 150 + x] = maxVal
                vSum += maxVal
                pixelCount++
            }
        }
        val avgV = if (pixelCount > 0) vSum / pixelCount else 0.5
        val whiteVThreshold = (avgV * 1.35).coerceIn(0.60, 0.92)

        when (finalLabel.lowercase(Locale.ROOT)) {
            "white_spots" -> {
                var minX = 150; var maxX = 0; var minY = 200; var maxY = 0
                var foundCount = 0
                for (y in 40 until 160) {
                    for (x in 30 until 120) {
                        val pixel = scaled.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        
                        val maxColor = maxOf(r, maxOf(g, b))
                        val minColor = minOf(r, minOf(g, b))
                        val delta = (maxColor - minColor).toFloat()
                        val s = if (maxColor > 0) delta / maxColor else 0f
                        val v = maxColor / 255.0
                        
                        if (s < 0.15f && v > whiteVThreshold && v > avgV * 1.15) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                            foundCount++
                        }
                    }
                }
                
                if (foundCount >= 3) {
                    val relXMin = (safeX + (minX / 150f) * cropW_safe) / width
                    val relXMax = (safeX + (maxX / 150f) * cropW_safe) / width
                    val relYMin = (safeY + (minY / 200f) * cropH_safe) / height
                    val relYMax = (safeY + (maxY / 200f) * cropH_safe) / height
                    
                    // Tight bounding box (no margin) to highlight only the exact spot
                    regions.add(com.example.nailnutri.data.SymptomRegion(
                        "white_spots_region_1",
                        relXMin.coerceAtLeast(0.1f),
                        relYMin.coerceAtLeast(0.1f),
                        relXMax.coerceAtMost(0.9f),
                        relYMax.coerceAtMost(0.9f)
                    ))
                } else {
                    regions.add(com.example.nailnutri.data.SymptomRegion("white_spots_region_2", 0.40f, 0.40f, 0.60f, 0.60f))
                }
            }
            
            "vertical_ridges" -> {
                val colGradients = DoubleArray(150)
                for (x in 20 until 129) {
                    var gradSum = 0.0
                    for (y in 30 until 170) {
                        val vSelf = allV[y * 150 + x]
                        val vRight = allV[y * 150 + (x + 1)]
                        gradSum += Math.abs(vSelf - vRight)
                    }
                    colGradients[x] = gradSum
                }
                
                val bestCol = (20 until 129).maxByOrNull { colGradients[it] } ?: 75
                
                // Dynamically scan columns exceeding 1.22x the mean vertical variance
                val meanGrad = colGradients.average()
                val thresholdGrad = meanGrad * 1.22
                var scanMinX = 150
                var scanMaxX = 0
                for (x in 20 until 129) {
                    if (colGradients[x] > thresholdGrad) {
                        if (x < scanMinX) scanMinX = x
                        if (x > scanMaxX) scanMaxX = x
                    }
                }
                if (scanMaxX <= scanMinX) {
                    scanMinX = (bestCol - 6).coerceAtLeast(15)
                    scanMaxX = (bestCol + 6).coerceAtMost(135)
                }
                
                val relXMin = (safeX + (scanMinX / 150f) * cropW_safe) / width
                val relXMax = (safeX + (scanMaxX / 150f) * cropW_safe) / width
                
                regions.add(com.example.nailnutri.data.SymptomRegion(
                    "vertical_ridges_region",
                    relXMin,
                    0.28f,
                    relXMax,
                    0.72f
                ))
            }
            
            "spoon_nails" -> {
                regions.add(com.example.nailnutri.data.SymptomRegion("spoon_nails_region_1", 0.3f, 0.35f, 0.7f, 0.65f))
            }
            
            "brittle" -> {
                regions.add(com.example.nailnutri.data.SymptomRegion("brittle_region", 0.25f, 0.15f, 0.75f, 0.40f))
            }
            
            "beaus_lines" -> {
                val rowGrads = DoubleArray(200)
                val rowV = DoubleArray(200)
                for (y in 30 until 170) {
                    var sumV = 0.0
                    var gradSum = 0.0
                    for (x in 25 until 125) {
                        val vSelf = allV[y * 150 + x]
                        val vNext = allV[(y + 1) * 150 + x]
                        sumV += vSelf
                        gradSum += Math.abs(vSelf - vNext)
                    }
                    rowV[y] = sumV / 100.0
                    rowGrads[y] = gradSum
                }

                // Identify candidate groove row bestY with local minimum in rowV or high vertical row gradients
                var bestY = 100
                var maxScore = -1.0
                for (y in 35 until 165) {
                    val prevV = (rowV[y - 3] + rowV[y - 2]) / 2.0
                    val nextV = (rowV[y + 3] + rowV[y + 2]) / 2.0
                    val dip = maxOf(0.0, ((prevV + nextV) / 2.0) - rowV[y])
                    val score = rowGrads[y] + (dip * 50.0)
                    if (score > maxScore) {
                        maxScore = score
                        bestY = y
                    }
                }

                val minY = (bestY - 5).coerceAtLeast(25)
                val maxY = (bestY + 5).coerceAtMost(175)
                val minX = 25
                val maxX = 125

                val relXMin = ((safeX + (minX / 150f) * cropW_safe) / width).coerceIn(0.05f, 0.90f)
                val relXMax = maxOf(relXMin + 0.1f, ((safeX + (maxX / 150f) * cropW_safe) / width).coerceIn(0.10f, 0.95f))
                val relYMin = ((safeY + (minY / 200f) * cropH_safe) / height).coerceIn(0.10f, 0.85f)
                val relYMax = maxOf(relYMin + 0.04f, ((safeY + (maxY / 200f) * cropH_safe) / height).coerceIn(0.15f, 0.90f))

                regions.add(
                    com.example.nailnutri.data.SymptomRegion(
                        "beaus_lines_region",
                        relXMin,
                        relYMin,
                        relXMax,
                        relYMax
                    )
                )
            }

            "lunula_abnormality" -> {
                // Proximal crescent base of the nail plate near the cuticle (lower proximal area)
                val minX = 35
                val maxX = 115
                val minY = 145
                val maxY = 185

                val relXMin = ((safeX + (minX / 150f) * cropW_safe) / width).coerceIn(0.10f, 0.85f)
                val relXMax = maxOf(relXMin + 0.1f, ((safeX + (maxX / 150f) * cropW_safe) / width).coerceIn(0.15f, 0.90f))
                val relYMin = ((safeY + (minY / 200f) * cropH_safe) / height).coerceIn(0.50f, 0.90f)
                val relYMax = maxOf(relYMin + 0.05f, ((safeY + (maxY / 200f) * cropH_safe) / height).coerceIn(0.55f, 0.95f))

                regions.add(
                    com.example.nailnutri.data.SymptomRegion(
                        "lunula_abnormality_region",
                        relXMin,
                        relYMin,
                        relXMax,
                        relYMax
                    )
                )
            }

            "onychomycosis" -> {
                var minX = 150; var maxX = 0; var minY = 200; var maxY = 0
                var foundCount = 0
                for (y in 25 until 175) {
                    for (x in 20 until 130) {
                        val pixel = scaled.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        
                        val maxColor = maxOf(r, maxOf(g, b))
                        val minColor = minOf(r, minOf(g, b))
                        val delta = (maxColor - minColor).toFloat()
                        
                        var h = 0f
                        if (delta > 0) {
                            h = if (maxColor == r) {
                                (g - b) / delta
                            } else if (maxColor == g) {
                                2f + (b - r) / delta
                            } else {
                                4f + (r - g) / delta
                            }
                            h *= 60f
                            if (h < 0) h += 360f
                        }
                        
                        if (delta > 15 && h in 18.0f..68.0f && maxColor > 60) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                            foundCount++
                        }
                    }
                }
                
                if (foundCount >= 5) {
                    val relXMin = (safeX + (minX / 150f) * cropW_safe) / width
                    val relXMax = (safeX + (maxX / 150f) * cropW_safe) / width
                    val relYMin = (safeY + (minY / 200f) * cropH_safe) / height
                    val relYMax = (safeY + (maxY / 200f) * cropH_safe) / height
                    
                    val finalXMin = relXMin.coerceIn(0.05f, 0.85f)
                    val finalXMax = maxOf(finalXMin + 0.05f, relXMax.coerceIn(0.10f, 0.95f))
                    val finalYMin = relYMin.coerceIn(0.05f, 0.85f)
                    val finalYMax = maxOf(finalYMin + 0.05f, relYMax.coerceIn(0.10f, 0.95f))

                    regions.add(com.example.nailnutri.data.SymptomRegion(
                        "onychomycosis_region_1",
                        finalXMin,
                        finalYMin,
                        finalXMax,
                        finalYMax
                    ))
                } else {
                    regions.add(com.example.nailnutri.data.SymptomRegion("onychomycosis_region_2", 0.25f, 0.2f, 0.75f, 0.4f))
                }
            }
            
            "melanonychia" -> {
                val colSums = DoubleArray(150)
                for (x in 20 until 130) {
                    var sum = 0.0
                    for (y in 25 until 175) {
                        sum += allV[y * 150 + x]
                    }
                    colSums[x] = sum
                }
                
                val minCol = (20 until 130).minByOrNull { colSums[it] } ?: 75
                // Narrow stripe highlighting (only 4px width to tightly align with the dark stripe)
                val minX = (minCol - 4).coerceAtLeast(15)
                val maxX = (minCol + 4).coerceAtMost(135)
                
                val relXMin = (safeX + (minX / 150f) * cropW_safe) / width
                val relXMax = (safeX + (maxX / 150f) * cropW_safe) / width

                val finalXMin = relXMin.coerceIn(0.05f, 0.90f)
                val finalXMax = maxOf(finalXMin + 0.03f, relXMax.coerceIn(0.08f, 0.95f))
                
                regions.add(com.example.nailnutri.data.SymptomRegion(
                    "melanonychia_region",
                    finalXMin,
                    0.18f,
                    finalXMax,
                    0.82f
                ))
            }
        }
        
        scaled.recycle()
        return regions
    }

    fun classify(bitmap: Bitmap, imagePath: String, context: Context? = null): NailAnalysisResult {
        if (context != null) {
            try {
                if (TFLiteClassifier.load(context)) {
                    val crops = getMultiCrops(bitmap)
                    val predictions = crops.map { TFLiteClassifier.getTopPrediction(it) }
                    
                    val labelScores = predictions.groupBy { it.first }
                        .mapValues { entry -> entry.value.sumOf { it.second.toDouble() } }
                    val bestLabelEntry = labelScores.maxByOrNull { it.value }
                    
                    val finalLabel = bestLabelEntry?.key ?: "healthy"
                    val finalConfidence = ((bestLabelEntry?.value ?: 0.0) / crops.size).toFloat()
                    
                    if (finalConfidence > 0.35f) {
                        val condition = TFLiteClassifier.mapToCondition(finalLabel)
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        val mockId = UUID.randomUUID().toString()
                        val symptomRegions = localizeSymptoms(condition, bitmap).ifEmpty { localizeSymptoms(finalLabel, bitmap) }
                        return buildSingleConditionResult(condition, imagePath, dateStr, mockId, symptomRegions)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        val features = NailFeatureExtractor.extract(bitmap)
        return buildResultFromFeatures(features, imagePath, bitmap)
    }

    fun buildResultForCondition(condition: String, imagePath: String): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()
        return buildSingleConditionResult(condition, imagePath, dateStr, mockId, emptyList())
    }

    private fun buildResultFromFeatures(features: NailFeatures, imagePath: String, bitmap: Bitmap): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        val regions = mutableListOf<com.example.nailnutri.data.SymptomRegion>()
        val activeConditions = mutableListOf<String>()
        if (features.hasWhiteSpots) {
            activeConditions.add("white_spots")
            regions.addAll(localizeSymptoms("white_spots", bitmap))
        }
        if (features.isUnevenTexture) {
            activeConditions.add("vertical_ridges")
            regions.addAll(localizeSymptoms("vertical_ridges", bitmap))
        }
        if (features.isDarkEdges || features.isPale || features.isLowRedness) {
            activeConditions.add("spoon_nails")
            regions.addAll(localizeSymptoms("spoon_nails", bitmap))
        }
        if (features.hasBeausLines) {
            activeConditions.add("beaus_lines")
            regions.addAll(localizeSymptoms("beaus_lines", bitmap))
        }
        if (features.hasLunulaAbnormality) {
            activeConditions.add("lunula_abnormality")
            regions.addAll(localizeSymptoms("lunula_abnormality", bitmap))
        }
        if (features.hasOnychomycosis) {
            activeConditions.add("onychomycosis")
            regions.addAll(localizeSymptoms("onychomycosis", bitmap))
        }
        if (features.hasMelanonychia) {
            activeConditions.add("melanonychia")
            regions.addAll(localizeSymptoms("melanonychia", bitmap))
        }
        if (activeConditions.isEmpty()) activeConditions.add("healthy")

        val distinctConditions = activeConditions.distinct()
        val results = distinctConditions.map { buildSingleConditionResult(it, imagePath, dateStr, mockId, emptyList()) }

        val allSymptoms = results.flatMap { it.symptoms }.distinct()
        val seenNutrients = mutableSetOf<String>()
        val combinedDeficient = results.flatMap { r ->
            r.deficientNutrients.filter { seenNutrients.add(it.name) }
        }
        val seenSufficient = mutableSetOf<String>()
        val combinedSufficient = results.flatMap { r ->
            r.sufficientNutrients.filter { seenSufficient.add(it.name) }
        }
        val combinedAdvice = results.joinToString("\n\n") { it.overallAdvice }

        return NailAnalysisResult(
            id = mockId,
            date = dateStr,
            imagePath = imagePath,
            symptoms = allSymptoms.ifEmpty { listOf("특이사항 없음 (건강함)") },
            deficientNutrients = combinedDeficient,
            sufficientNutrients = combinedSufficient,
            overallAdvice = combinedAdvice,
            symptomRegions = regions
        )
    }

    private fun buildSingleConditionResult(
        condition: String,
        imagePath: String,
        dateStr: String,
        mockId: String,
        symptomRegions: List<com.example.nailnutri.data.SymptomRegion> = emptyList()
    ): NailAnalysisResult {
        return when (condition.lowercase(Locale.ROOT)) {
            "healthy" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("특이사항 없음 (건강함)"),
                deficientNutrients = emptyList(),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("단백질 (Keratin)", "손톱 표면이 매끄럽고 윤기가 있어 충분한 단백질 공급을 보입니다.", "손톱의 기본 구조 형성에 기여"),
                    SufficientNutrientDetail("아연 (Zinc)", "흰 반점이 관찰되지 않아 아연 수치가 양호해 보입니다.", "세포 분열 및 케라틴 합성 촉진"),
                    SufficientNutrientDetail("철분 (Iron)", "네일 베드 색상이 붉고 생기 있어 산소 공급이 원활합니다.", "산소 운반 및 건강한 세포 형성"),
                    SufficientNutrientDetail("비오틴 (Biotin)", "손톱 끝이 얇아지거나 부서지지 않고 탄탄합니다.", "손톱 두께 및 단단함 유지")
                ),
                overallAdvice = "축하합니다! 현재 손톱 상태는 매우 건강합니다. 현재의 균형 잡힌 영양 식단을 유지하고 건조해지지 않도록 가벼운 핸드크림 케어를 계속해주세요.",
                symptomRegions = symptomRegions
            )
            "white_spots" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("손톱 표면의 흰 반점 (Leukonychia)"),
                deficientNutrients = listOf(
                    NutrientDetail("아연 (Zinc)", "Severe", "손톱 중간에 산발적인 흰 반점이 나타나는 것은 세포 분열과 단백질 합성을 돕는 아연의 결핍을 강하게 시사합니다.", listOf("굴", "소고기", "호박씨", "아몬드")),
                    NutrientDetail("칼슘 (Calcium)", "Moderate", "가로방향의 흰색 띠가 발견되는 경우 칼슘 결핍이나 대사 스트레스 상태일 수 있습니다.", listOf("우유", "치즈", "멸치", "두부"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("비오틴", "손톱이 깨지거나 찢어지지 않아 기본 비오틴 공급은 원활합니다.", "손톱 구조 강화"),
                    SufficientNutrientDetail("철분", "네일 베드가 혈색이 좋아 심한 철분 부족은 아닙니다.", "혈액 생성")
                ),
                overallAdvice = "아연 결핍 증상인 흰 반점이 눈에 띕니다. 아연이 풍부한 견과류 and 육류 섭취를 늘리고, 필요시 단기간 칼슘/아연 보충제 섭취를 고려해 볼 수 있습니다.",
                symptomRegions = symptomRegions
            )
            "vertical_ridges" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("세로줄 현상 (Vertical Ridges)"),
                deficientNutrients = listOf(
                    NutrientDetail("비타민 B12", "Moderate", "세로 방향으로 깊게 파인 줄은 비타민 B12 결핍으로 인한 손톱 세포 성장 둔화를 나타낼 수 있습니다.", listOf("육류", "달걀", "연어", "유제품")),
                    NutrientDetail("마그네슘 (Magnesium)", "Moderate", "손톱 표면의 세로 홈과 울퉁불퉁함은 신체 대사를 돕는 마그네슘 부족과 연관될 수 있습니다.", listOf("시금치", "바나나", "아보카도", "다크 초콜릿"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("아연", "흰색 반점이나 가로 반점은 관찰되지 않아 아연 상태는 양호합니다.", "아연은 손톱 성장에 기여"),
                    SufficientNutrientDetail("단백질", "기본적인 손톱 강도는 잘 유지되고 있습니다.", "케라틴 합성")
                ),
                overallAdvice = "노화의 자연스러운 현상일 수도 있지만, 비타민 B12와 마그네슘 부족도 큰 원인이 됩니다. 잡곡밥, 녹색 채소, 그리고 적당한 고기류가 포함된 식단이 권장됩니다.",
                symptomRegions = symptomRegions
            )
            "spoon_nails" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("숟가락 모양 굽어짐 (Koilonychia)"),
                deficientNutrients = listOf(
                    NutrientDetail("철분 (Iron)", "Severe", "손톱 끝이 뒤집어지고 가운데가 움푹 파여 숟가락 모양을 띠는 현상은 대표적인 철분 결핍성 빈혈의 주요 증상입니다. 빠른 철분 보충이 필요합니다.", listOf("붉은 고기", "시금치", "렌틸콩", "조개류")),
                    NutrientDetail("단백질 (Protein)", "Moderate", "손톱 판이 얇아지고 쉽게 구부러지는 것은 기본 구성 물질인 단백질 부족을 뜻합니다.", listOf("닭가슴살", "달걀", "두부", "생선"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("칼슘", "손톱 표면이 전반적으로 깨끗하며 칼슘 분배는 양호합니다.", "골격 및 네일 강도 유지")
                ),
                overallAdvice = "숟가락 모양의 손톱(Koilonychia)은 심각한 철분 부족 빈혈의 신호일 수 있습니다. 붉은 고기와 녹색 잎채소를 다량 섭취하시고 철분의 흡수율을 높이기 위해 비타민 C와 함께 복용하는 것을 강력히 권장합니다.",
                symptomRegions = symptomRegions
            )
            "brittle" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("손톱 갈라짐 및 깨짐 (Onychorrhexis)"),
                deficientNutrients = listOf(
                    NutrientDetail("비오틴 (Biotin)", "Severe", "손톱이 메마르고 건조하며 끝부분이 갈라지고 부서지는 증상은 각질 구조를 단단히 하는 비오틴(비타민 B7) 결핍과 관련이 큽니다.", listOf("계란 노른자", "견과류", "콜리플라워", "고구마")),
                    NutrientDetail("수분/필수지방산", "Moderate", "손톱의 탄력을 잃고 껍질처럼 벗겨지는 현상은 필수 지방산(오메가-3) 및 수분 부족 증상입니다.", listOf("들기름", "연어", "호두", "물 하루 8잔"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("철분", "빈혈 증상이 없고 네일 베드의 혈색은 건강한 핑크빛을 띱니다.", "산소 활성 공급")
                ),
                overallAdvice = "갈라지고 건조한 손톱은 비오틴과 지방산 공급이 최우선입니다. 계란이나 아몬드를 매일 드셔 보시고 네일 오일이나 핸드크림을 자주 발라 외부 수분을 공급해 주는 것도 아주 중요합니다.",
                symptomRegions = symptomRegions
            )
            "beaus_lines" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("손톱 가로줄 및 수평 홈 (Beau's Lines)"),
                deficientNutrients = listOf(
                    NutrientDetail(
                        "아연 (Zinc)",
                        "Severe",
                        "손톱 기질(Matrix) 세포 분열이 일시적으로 급격히 중단되어 발생하는 가로 홈(Beau's lines)은 고열, 급성 전신 대사 스트레스 및 급성 중증 아연 결핍을 강력히 시사합니다. 아연은 케라티노사이트의 급속한 단백질 합성과 세포 증식의 필수 보조인자입니다.",
                        listOf("굴", "소고기", "호박씨", "달걀노른자", "렌틸콩")
                    ),
                    NutrientDetail(
                        "단백질 (Protein)",
                        "Severe",
                        "체내 급격한 소모성 대사나 단백질 공급 결핍으로 인해 네일 플레이트의 두께를 형성하는 케라틴 기질 형성이 일시 중단된 흔적입니다. 필수 아미노산(메티오닌, 시스테인) 보충이 시급합니다.",
                        listOf("닭가슴살", "소고기 우둔살", "연어", "두부", "달걀흰자")
                    )
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail(
                        "칼슘 (Calcium)",
                        "손톱 판 전반의 석회화 및 기본적인 경도는 유지되고 있습니다.",
                        "골격 및 손톱 케라틴 플레이트 경도 유지"
                    ),
                    SufficientNutrientDetail(
                        "비타민 C (Vitamin C)",
                        "급성 면역 저하 후 회복기 항산화 및 콜라겐 결합 조직 재생을 뒷받침합니다.",
                        "단백질 합성 촉진 및 활성산소 억제"
                    )
                ),
                overallAdvice = "손톱을 가로지르는 깊은 수평 홈(Beau's Lines)은 과거 몇 주~몇 달 전 고열, 급성 질환, 중증 스트레스나 극심한 영양 결핍으로 인해 손톱 성장이 일시 멈췄던 흔적입니다. 현재 체력과 면역력이 회복 중이더라도 아연과 고품질 단백질을 적극 보충하고, 충분한 휴식과 수면을 통해 손톱 뿌리(기질)의 정상적인 세포 분열을 지원해주세요.",
                symptomRegions = symptomRegions
            )
            "lunula_abnormality" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("반월(손톱 반달) 결손 및 순환 저하 (Lunula Abnormality)"),
                deficientNutrients = listOf(
                    NutrientDetail(
                        "철분 (Iron)",
                        "Severe",
                        "근위부 손톱 반달(반월, Lunula)이 흐릿해지거나 완전히 소실되는 현상은 만성적인 헤모글로빈 부족 및 체내 철 저장량(페리틴) 고갈로 인한 기저부 미세순환 저하의 전형적인 징후입니다.",
                        listOf("붉은 소고기", "선지", "시금치", "바지락/조개류", "다크 초콜릿")
                    ),
                    NutrientDetail(
                        "비타민 B12 & 엽산",
                        "Moderate",
                        "적혈구 생성 및 말초 혈관 신경 전달에 필수적인 비타민 B12와 엽산 결핍은 말초 혈류량을 감소시켜 반월 영역의 경계를 모호하게 만들 수 있습니다.",
                        listOf("동물 간", "달걀", "고등어", "브로콜리", "아스파라거스")
                    )
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail(
                        "비오틴 (Biotin)",
                        "손톱 표면의 심한 박리나 부스러짐이 없어 말단 각질 결합 상태는 양호합니다.",
                        "각질 표면 코팅 및 단단함 보존"
                    )
                ),
                overallAdvice = "손톱 기저부의 반월(하얀 반달)이 잘 보이지 않거나 소실된 경우 만성 빈혈이나 손발 끝 말초 혈액순환 저하, 신진대사 기능 저하를 의심할 수 있습니다. 붉은 살코기, 조개류 등 흡수율이 높은 헴철(Heme iron)과 비타민 C를 함께 섭취하시고, 손끝 족욕이나 가벼운 유산소 운동으로 말초 혈류를 촉진해 주세요.",
                symptomRegions = symptomRegions
            )
            "onychomycosis" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("조갑진균증 및 각질 비후 (Onychomycosis)"),
                deficientNutrients = listOf(
                    NutrientDetail(
                        "아연 (Zinc)",
                        "Severe",
                        "진균의 침투와 케라틴 기질 분해는 체내 흉선 호르몬 및 T세포 활성에 관여하는 아연 결핍에 따른 항진균 면역 장벽 약화와 밀접합니다. 아연은 상피 조직 재생과 항균 펩타이드 생성의 핵심입니다.",
                        listOf("굴", "소고기", "호박씨", "캐슈넛", "버섯류")
                    ),
                    NutrientDetail(
                        "비타민 A & D",
                        "Moderate",
                        "피부 및 점막, 각질 면역 장벽의 방어력을 유지하는 지용성 비타민(A, D)이 부족하면 진균류의 각질층 침투가 쉬워집니다.",
                        listOf("당근", "달걀노른자", "연어", "치즈", "시금치")
                    )
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail(
                        "철분 (Iron)",
                        "조갑 기저부의 기본적인 혈류 산소 운반은 유지되고 있습니다.",
                        "네일 베드 조직 산소 공급"
                    )
                ),
                overallAdvice = "조갑진균증(손발톱 무좀)은 곰팡이균의 증식과 함께 손톱 각질이 두꺼워지거나 부스러지고 황갈색으로 변하는 질환입니다. 영양학적으로는 아연과 면역 비타민을 보충하여 신체 저항력을 키우는 것이 중요하며, 진균 감염 특성상 반드시 피부과 전문의의 항진균 치료(외용제/경구약)를 병행하고 손톱을 항상 건조하고 청결하게 유지해야 합니다.",
                symptomRegions = symptomRegions
            )
            "melanonychia" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("세로 흑색선조 밴드 (Longitudinal Melanonychia)"),
                deficientNutrients = listOf(
                    NutrientDetail(
                        "비타민 B12 (Cobalamin)",
                        "Severe",
                        "세로 방향의 흑갈색 선조(Melanonychia)는 멜라닌 세포의 비정상적 활성화와 연관되며, 영양학적으로는 비타민 B12 결핍으로 인한 멜라닌 합성 조절 이상 및 과다 색소 침착 증상으로 흔히 나타납니다.",
                        listOf("소간/닭간", "연어", "참치", "우유", "달걀", "조개류")
                    ),
                    NutrientDetail(
                        "항산화 영양소 (비타민 C/E & 셀레늄)",
                        "Moderate",
                        "자외선, 국소 외상 및 산화 스트레스로부터 멜라닌 세포의 과민 반응을 억제하고 색소 침착을 완화하는 항산화 방어 영양소 보충이 권장됩니다.",
                        listOf("브라질너트", "해바라기씨", "파프리카", "키위", "아몬드")
                    )
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail(
                        "칼슘 & 마그네슘",
                        "손톱 표면의 홈이나 깨짐 없이 네일 플레이트의 평활도와 단단함은 잘 유지되고 있습니다.",
                        "손톱 표면 평활성 유지"
                    )
                ),
                overallAdvice = "세로 흑색선조(Melanonychia)는 손톱 뿌리의 멜라닌 세포 활성화나 비타민 B12 결핍성 색소 침착, 혹은 만성 외상으로 인해 발생할 수 있습니다. 비타민 B12와 항산화 식단을 보충해 주시고, 만약 선의 너비가 3mm 이상으로 넓어지거나 색상이 불균일해지고 손톱 주변 피부까지 번지는 경우(허친슨 징후)에는 즉시 피부과 전문의의 정밀 진단(더모스코피)을 받으셔야 합니다.",
                symptomRegions = symptomRegions
            )
            else -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("기타 상태 식별"),
                deficientNutrients = listOf(
                    NutrientDetail("철분 (Iron)", "Moderate", "분석 결과 미세한 철분 부족 신호가 감지되었습니다.", listOf("붉은 고기", "시금치", "두부"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "일반적인 균형 잡힌 다이어트 식단을 구성하시고, 비타민 C가 풍부한 과일 및 야채 섭취를 늘려보세요.",
                symptomRegions = symptomRegions
            )
        }
    }
}
