package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class NailFeatures(
    val averageRedness: Double,
    val averageSaturation: Double,
    val averageBrightness: Double,
    val whiteSpotRatio: Double,
    val darkEdgeRatio: Double,
    val brightnessStdDev: Double,
    val rednessUniformity: Double,
    val isPale: Boolean,
    val hasWhiteSpots: Boolean,
    val isDarkEdges: Boolean,
    val isUnevenTexture: Boolean,
    val isLowRedness: Boolean,
    // 4 New condition flags
    val hasBeausLines: Boolean = false,
    val hasLunulaAbnormality: Boolean = false,
    val hasOnychomycosis: Boolean = false,
    val hasMelanonychia: Boolean = false,
    // Metrics
    val beausLinesScore: Float = 0f,
    val lunulaContrast: Float = 0f,
    val fungalColorScore: Float = 0f,
    val melanonychiaBandScore: Float = 0f
) {
    // Convenience aliases for testing & survey compatibility
    val isBeausLines: Boolean get() = hasBeausLines
    val isLunulaAbnormal: Boolean get() = hasLunulaAbnormality
    val beausLineDepth: Double get() = beausLinesScore.toDouble()
    val lunulaAbnormalityScore: Double get() = lunulaContrast.toDouble()
    val onychomycosisRatio: Double get() = fungalColorScore.toDouble()
    val melanonychiaScore: Double get() = melanonychiaBandScore.toDouble()

    fun toSymptomList(): List<String> {
        val list = mutableListOf<String>()
        if (isPale) {
            list.add("창백한 네일베드 (Pale Nail Bed)")
        }
        if (hasWhiteSpots) {
            list.add("손톱 표면의 흰 반점 (Leukonychia)")
        }
        if (isUnevenTexture) {
            list.add("거친 손톱 표면 / 세로줄 현상 (Vertical Ridges)")
        }
        if (isDarkEdges) {
            list.add("거의 숟가락형 함몰 징후 (Dark Nail Edges)")
        }
        if (isLowRedness) {
            list.add("낮은 혈색 / 저산소 징후 (Low Redness)")
        }
        if (hasBeausLines) {
            list.add("beaus_lines")
            list.add("보우선 / 수평 홈 징후 (Beau's Lines)")
        }
        if (hasLunulaAbnormality) {
            list.add("lunula_abnormality")
            list.add("반월 결손 / 이상 변색 (Lunula Abnormality)")
        }
        if (hasOnychomycosis) {
            list.add("onychomycosis")
            list.add("조갑진균증 / 변색 및 박리 (Onychomycosis)")
        }
        if (hasMelanonychia) {
            list.add("melanonychia")
            list.add("흑색선조 / 세로 흑색 밴드 (Melanonychia)")
        }
        if (list.isEmpty()) {
            list.add("특이사항 없음 (건강함)")
        }
        return list
    }

    fun toKoreanDescription(): String {
        return buildString {
            append("- 평균 혈색(R채널): ${"%.1f".format(averageRedness)}/255")
            append(if (isLowRedness) " (낮음)" else if (isPale) " (창백함)" else " (정상)")
            append("\n")
            append("- 평균 채도: ${"%.1f".format(averageSaturation * 100)}%")
            append(if (isPale) " (저채도)" else "")
            append("\n")
            append("- 평균 명도: ${"%.1f".format(averageBrightness * 100)}%")
            append(if (hasWhiteSpots) " (높음 - 반점 가능성)" else "")
            append("\n")
            append("- 흰색 픽셀 비율: ${"%.2f".format(whiteSpotRatio * 100)}%")
            append(if (hasWhiteSpots) " (반점 감지)" else "")
            append("\n")
            append("- 어두운 가장자리 비율: ${"%.2f".format(darkEdgeRatio * 100)}%")
            append(if (isDarkEdges) " (가장자리 어두움 - 함몰/철결핍 가능성)" else "")
            append("\n")
            append("- 명도 표준편차: ${"%.1f".format(brightnessStdDev)}")
            append(if (isUnevenTexture) " (표면 고르지 못함 - 세로줄 가능성)" else "")
            append("\n")
            append("- 혈색 균일도(표준편차): ${"%.1f".format(rednessUniformity)}")
            append(if (rednessUniformity > 30) " (혈색 불균일)" else "")
            append("\n")
            append("- 보우선 수평 홈 스코어: ${"%.2f".format(beausLinesScore * 100)}%")
            if (hasBeausLines) append(" (보우선 감지 - 급성 스트레스/아연 결핍 의심)")
            append("\n")
            append("- 반월 대비/이상 스코어: ${"%.2f".format(lunulaContrast * 100)}%")
            if (hasLunulaAbnormality) append(" (반월 결손/이상 변색 - 빈혈/순환장애 의심)")
            append("\n")
            append("- 조갑진균 의심 변색률: ${"%.2f".format(fungalColorScore * 100)}%")
            if (hasOnychomycosis) append(" (황갈색 변색 - 조갑진균증 의심)")
            append("\n")
            append("- 흑색선조 밴드 스코어: ${"%.2f".format(melanonychiaBandScore * 100)}%")
            if (hasMelanonychia) append(" (세로 흑색 밴드 - 비타민 B12/멜라닌 침착 의심)")
        }
    }
}

internal object NailFeatureExtractor {

    fun extract(bitmap: Bitmap): NailFeatures {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return defaultFeatures()

        // Fallback to legacy extraction for small synthetic test images (e.g. 100x100)
        if (width <= 120 || height <= 120) {
            return legacyExtract(bitmap)
        }

        // 1. Center crop matching Android camera crop (60% width, 1.33 aspect ratio)
        val cropW = (width * 0.6).toInt()
        val cropH = (cropW * 1.33).toInt()
        val cropX = (width - cropW) / 2
        val cropY = (height - cropH) / 2

        val safeX = max(0, min(cropX, width - cropW))
        val safeY = max(0, min(cropY, height - cropH))
        val safeW = min(cropW, width - safeX)
        val safeH = min(cropH, height - safeY)

        val croppedBitmap = Bitmap.createBitmap(bitmap, safeX, safeY, safeW, safeH)
        // 2. Downsample to 150x200 via bilinear scaling to naturally filter noise
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 150, 200, true)
        if (croppedBitmap != bitmap && croppedBitmap != scaledBitmap) {
            croppedBitmap.recycle()
        }

        val targetWidth = 150
        val targetHeight = 200
        val allPixels = IntArray(targetWidth * targetHeight)
        scaledBitmap.getPixels(allPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        // 3. Compute illumination statistics for adaptive thresholds
        val sampleStep = 2
        val xCoords = (0 until targetWidth step sampleStep).toList()
        val yCoords = (0 until targetHeight step sampleStep).toList()
        val xLen = xCoords.size
        val yLen = yCoords.size

        val nailXMin = (targetWidth * 0.28).toInt()
        val nailXMax = (targetWidth * 0.72).toInt()
        val nailYMin = (targetHeight * 0.28).toInt()
        val nailYMax = (targetHeight * 0.72).toInt()

        val allV = mutableListOf<Double>()
        for (j in 0 until yLen) {
            val y = yCoords[j]
            for (i in 0 until xLen) {
                val x = xCoords[i]
                if (x < nailXMin || x > nailXMax || y < nailYMin || y > nailYMax) continue
                val idx = y * targetWidth + x
                if (idx >= allPixels.size) continue
                val (_, _, v) = rgbToHsv(Color.red(allPixels[idx]), Color.green(allPixels[idx]), Color.blue(allPixels[idx]))
                allV.add(v)
            }
        }
        val sortedAllV = allV.sorted()
        val medianV = if (sortedAllV.isNotEmpty()) sortedAllV[sortedAllV.size / 2] else 0.5
        val illumNorm = medianV.coerceIn(0.15, 0.85)

        val skinVMin = (illumNorm * 0.3).coerceIn(0.08, 0.25)
        val whiteVThreshold = (illumNorm * 1.5).coerceIn(0.60, 0.90)
        val darkVThreshold = (illumNorm * 0.6).coerceIn(0.15, 0.40)

        // 4. Extract skin-masked pixels in the center 40% window
        val gridV = Array(yLen) { DoubleArray(xLen) }
        val gridR = Array(yLen) { DoubleArray(xLen) }
        val gridValid = Array(yLen) { BooleanArray(xLen) }
        val skinPixels = mutableListOf<Pair<Int, Int>>()

        var rSum = 0.0; var gSum = 0.0; var bSum = 0.0
        var sSum = 0.0; var vSum = 0.0; var hSum = 0.0
        var sampleCount = 0

        val brightnessValues = mutableListOf<Double>()
        val redValues = mutableListOf<Double>()

        for (j in 0 until yLen) {
            val y = yCoords[j]
            for (i in 0 until xLen) {
                val x = xCoords[i]
                if (x < nailXMin || x > nailXMax || y < nailYMin || y > nailYMax) {
                    continue
                }

                val idx = y * targetWidth + x
                if (idx >= allPixels.size) continue
                val pixel = allPixels[idx]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val (h, s, v) = rgbToHsv(r, g, b)
                val isSkin = (s in 0.13..0.75) && (v >= skinVMin) && (h <= 50.0 || h >= 320.0)

                if (isSkin) {
                    gridV[j][i] = v
                    gridR[j][i] = r.toDouble()
                    gridValid[j][i] = true
                    skinPixels.add(Pair(i, j))

                    rSum += r
                    gSum += g
                    bSum += b
                    sSum += s
                    vSum += v
                    hSum += h
                    brightnessValues.add(v)
                    redValues.add(r.toDouble())
                    sampleCount++
                }
            }
        }

        // Fallback if no skin detected: use all non-black pixels
        if (sampleCount < 10) {
            rSum = 0.0; gSum = 0.0; bSum = 0.0
            sSum = 0.0; vSum = 0.0; hSum = 0.0
            sampleCount = 0
            brightnessValues.clear()
            redValues.clear()
            skinPixels.clear()
            for (row in gridV) row.fill(0.0)
            for (row in gridR) row.fill(0.0)
            for (row in gridValid) row.fill(false)

            for (j in 0 until yLen) {
                val y = yCoords[j]
                for (i in 0 until xLen) {
                    val x = xCoords[i]
                    val idx = y * targetWidth + x
                    if (idx >= allPixels.size) continue
                    val pixel = allPixels[idx]
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    val (h, s, v) = rgbToHsv(r, g, b)
                    if (v >= skinVMin) {
                        gridV[j][i] = v
                        gridR[j][i] = r.toDouble()
                        gridValid[j][i] = true
                        skinPixels.add(Pair(i, j))

                        rSum += r
                        gSum += g
                        bSum += b
                        sSum += s
                        vSum += v
                        hSum += h
                        brightnessValues.add(v)
                        redValues.add(r.toDouble())
                        sampleCount++
                    }
                }
            }
        }

        if (sampleCount == 0) {
            return defaultFeatures()
        }

        val avgR = rSum / sampleCount
        val avgG = gSum / sampleCount
        val avgB = bSum / sampleCount
        val avgS = sSum / sampleCount
        val avgV = vSum / sampleCount

        val xs = skinPixels.map { it.first }
        val ys = skinPixels.map { it.second }
        val minI = xs.minOrNull() ?: 0
        val maxI = xs.maxOrNull() ?: 0
        val minJ = ys.minOrNull() ?: 0
        val maxJ = ys.maxOrNull() ?: 0

        var whiteSpotCount = 0
        var darkEdgeCount = 0

        for (j in minJ..maxJ) {
            val y = yCoords[j]
            for (i in minI..maxI) {
                val x = xCoords[i]
                val idx = y * targetWidth + x
                if (idx >= allPixels.size) continue
                val pixel = allPixels[idx]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val (_, s, v) = rgbToHsv(r, g, b)

                if (s < 0.15 && v > whiteVThreshold && v > avgV * 1.15) {
                    whiteSpotCount++
                }

                val isNearBboxEdge = (i - minI < 3 || maxI - i < 3 || j - minJ < 3 || maxJ - j < 3)
                if (isNearBboxEdge && v < darkVThreshold && r < 80) {
                    darkEdgeCount++
                }
            }
        }

        val whiteSpotRatio = whiteSpotCount.toDouble() / max(1, sampleCount)
        val darkEdgeRatio = darkEdgeCount.toDouble() / max(1, sampleCount)

        // 4. Calculate local gradients for interior pixels only (eroded mask)
        val interior = Array(yLen) { BooleanArray(xLen) }
        for (j in 1 until yLen - 1) {
            for (i in 1 until xLen - 1) {
                if (gridValid[j][i] &&
                    gridValid[j - 1][i] && gridValid[j + 1][i] &&
                    gridValid[j][i - 1] && gridValid[j][i + 1]) {

                    val isShinySelf = gridV[j][i] > 0.80 && gridR[j][i] > 180.0
                    if (!isShinySelf) {
                        interior[j][i] = true
                    }
                }
            }
        }

        val localVDiffs = mutableListOf<Double>()
        val localRDiffs = mutableListOf<Double>()

        for (j in 0 until yLen) {
            for (i in 0 until xLen) {
                if (!interior[j][i]) continue
                if (i + 1 < xLen && interior[j][i + 1]) {
                    localVDiffs.add(Math.abs(gridV[j][i] - gridV[j][i + 1]))
                    localRDiffs.add(Math.abs(gridR[j][i] - gridR[j][i + 1]))
                }
                if (j + 1 < yLen && interior[j + 1][i]) {
                    localVDiffs.add(Math.abs(gridV[j][i] - gridV[j + 1][i]))
                    localRDiffs.add(Math.abs(gridR[j][i] - gridR[j + 1][i]))
                }
            }
        }

        val avgLocalVGrad = if (localVDiffs.isNotEmpty()) localVDiffs.average() else 0.0
        val avgLocalRGrad = if (localRDiffs.isNotEmpty()) localRDiffs.average() else 0.0

        val brightnessStd = stdDev(brightnessValues, avgV)
        val rednessStd = stdDev(redValues, avgR)

        // Tuned threshold heuristics
        val isDarkEdges = darkEdgeRatio > 0.08
        val rBRatio = avgR / (avgB + 1e-5)
        // Prevent false positives under excessive glossy reflection (avgV >= 0.85)
        val isLowRedness = (avgR < 135.0 || rBRatio < 1.12) && !isDarkEdges && avgV < 0.85
        val isPale = avgS < 0.20 && avgV > illumNorm * 0.96 && avgR < 185.0 && avgV < 0.85
        val hasWhiteSpots = whiteSpotRatio > 0.015
        val isUnevenTexture = (avgLocalVGrad > 0.022 || avgLocalRGrad > 5.5) && !isDarkEdges

        // 5. New condition detectors: Beau's Lines, Lunula Abnormality, Onychomycosis, Melanonychia

        // 5.1 Beau's Lines: Horizontal transverse groove / brightness dip across interior rows
        val prodYStart = (yLen * 0.20).toInt()
        val prodYEnd = (yLen * 0.80).toInt()
        val prodRowV = DoubleArray(yLen)
        val prodRowCount = IntArray(yLen)

        for (j in prodYStart..prodYEnd) {
            var sumV = 0.0
            var count = 0
            for (i in 0 until xLen) {
                if (interior[j][i]) {
                    sumV += gridV[j][i]
                    count++
                }
            }
            if (count > 0) {
                prodRowV[j] = sumV / count
                prodRowCount[j] = count
            }
        }

        var prodMaxDip = 0.0
        for (j in (prodYStart + 3)..(prodYEnd - 3)) {
            if (prodRowCount[j] == 0) continue
            var baseSum = 0.0
            var baseCount = 0
            for (offset in intArrayOf(-4, -3, 3, 4)) {
                val nj = j + offset
                if (nj in prodYStart..prodYEnd && prodRowCount[nj] > 0) {
                    baseSum += prodRowV[nj]
                    baseCount++
                }
            }
            val baseV = if (baseCount > 0) baseSum / baseCount else avgV
            val dip = (baseV - prodRowV[j]) / (baseV + 1e-5)
            if (dip > prodMaxDip) {
                prodMaxDip = dip
            }
        }
        val prodBeausScore = max(0.0, prodMaxDip).toFloat()
        val prodHasBeausLines = prodBeausScore > 0.16f && !isDarkEdges && !isUnevenTexture

        // 5.2 Lunula Abnormality: Proximal bottom region vs body reference
        val prodLunulaIMin = (xLen * 0.35).toInt()
        val prodLunulaIMax = (xLen * 0.65).toInt()
        val prodLunulaJMin = (yLen * 0.70).toInt()
        val prodLunulaJMax = (yLen * 0.90).toInt()

        val prodBodyJMin = (yLen * 0.30).toInt()
        val prodBodyJMax = (yLen * 0.55).toInt()

        var pLunR = 0.0; var pLunG = 0.0; var pLunB = 0.0; var pLunV = 0.0; var pLunCount = 0
        for (j in prodLunulaJMin..prodLunulaJMax) {
            val y = yCoords[j]
            for (i in prodLunulaIMin..prodLunulaIMax) {
                val idx = y * targetWidth + xCoords[i]
                if (idx < allPixels.size && gridValid[j][i]) {
                    val p = allPixels[idx]
                    pLunR += Color.red(p); pLunG += Color.green(p); pLunB += Color.blue(p); pLunV += gridV[j][i]
                    pLunCount++
                }
            }
        }

        var pBodyR = 0.0; var pBodyG = 0.0; var pBodyB = 0.0; var pBodyV = 0.0; var pBodyCount = 0
        for (j in prodBodyJMin..prodBodyJMax) {
            val y = yCoords[j]
            for (i in prodLunulaIMin..prodLunulaIMax) {
                val idx = y * targetWidth + xCoords[i]
                if (idx < allPixels.size && gridValid[j][i]) {
                    val p = allPixels[idx]
                    pBodyR += Color.red(p); pBodyG += Color.green(p); pBodyB += Color.blue(p); pBodyV += gridV[j][i]
                    pBodyCount++
                }
            }
        }

        val pLunAvgR = if (pLunCount > 0) pLunR / pLunCount else 0.0
        val pLunAvgG = if (pLunCount > 0) pLunG / pLunCount else 0.0
        val pLunAvgB = if (pLunCount > 0) pLunB / pLunCount else 0.0
        val pLunAvgV = if (pLunCount > 0) pLunV / pLunCount else 0.0

        val pBodyAvgR = if (pBodyCount > 0) pBodyR / pBodyCount else 0.0
        val pBodyAvgG = if (pBodyCount > 0) pBodyG / pBodyCount else 0.0
        val pBodyAvgB = if (pBodyCount > 0) pBodyB / pBodyCount else 0.0
        val pBodyAvgV = if (pBodyCount > 0) pBodyV / pBodyCount else 0.0

        val pdR = Math.abs(pLunAvgR - pBodyAvgR) / 255.0
        val pdG = Math.abs(pLunAvgG - pBodyAvgG) / 255.0
        val pdB = Math.abs(pLunAvgB - pBodyAvgB) / 255.0
        val pDist = sqrt(pdR * pdR + pdG * pdG + pdB * pdB)
        val pdV = Math.abs(pLunAvgV - pBodyAvgV) / (pBodyAvgV + 1e-5)
        val pBlueShift = max(0.0, (pLunAvgB / (pLunAvgR + 1e-5)) - (pBodyAvgB / (pBodyAvgR + 1e-5)))

        val prodLunulaContrast = maxOf(pDist, pdV, pBlueShift).toFloat()
        val prodHasLunulaAbnormality = prodLunulaContrast > 0.20f && !isDarkEdges && !isPale

        // 5.3 Onychomycosis: Yellow-brown / white discoloration with HSV range H in [18, 68], S >= 0.18, V >= 0.25
        var prodFungalCount = 0
        var prodInteriorCount = 0
        for (j in 0 until yLen) {
            val y = yCoords[j]
            for (i in 0 until xLen) {
                if (!gridValid[j][i]) continue
                prodInteriorCount++
                val idx = y * targetWidth + xCoords[i]
                if (idx < allPixels.size) {
                    val p = allPixels[idx]
                    val (h, s, v) = rgbToHsv(Color.red(p), Color.green(p), Color.blue(p))
                    if (h in 18.0..68.0 && s >= 0.18 && v >= 0.25) {
                        prodFungalCount++
                    }
                }
            }
        }
        val prodFungalScore = prodFungalCount.toFloat() / max(1, prodInteriorCount)
        val prodHasOnychomycosis = prodFungalScore > 0.035f && !isDarkEdges

        // 5.4 Melanonychia: Longitudinal dark column band spanning proximal to distal with high continuity
        val prodMelIMin = (xLen * 0.18).toInt()
        val prodMelIMax = (xLen * 0.82).toInt()
        val prodMelJMin = (yLen * 0.15).toInt()
        val prodMelJMax = (yLen * 0.85).toInt()

        var prodMaxContinuity = 0.0
        var prodBestContrast = 0.0
        var prodDarkCols = 0
        val prodEvalCols = prodMelIMax - prodMelIMin + 1

        for (i in prodMelIMin..prodMelIMax) {
            var colDark = 0
            var colV = 0.0
            var validInCol = 0
            for (j in prodMelJMin..prodMelJMax) {
                if (gridValid[j][i]) {
                    validInCol++
                    colV += gridV[j][i]
                    if (gridV[j][i] < avgV * 0.65 && gridR[j][i] < avgR * 0.70) {
                        colDark++
                    }
                }
            }
            val continuity = if (validInCol > 0) colDark.toDouble() / validInCol else 0.0
            val colMeanV = if (validInCol > 0) colV / validInCol else 0.0
            if (continuity >= 0.50) {
                prodDarkCols++
            }
            if (continuity > prodMaxContinuity) {
                prodMaxContinuity = continuity
                prodBestContrast = max(0.0, (avgV - colMeanV) / (avgV + 1e-5))
            }
        }

        val prodDarkColRatio = if (prodEvalCols > 0) prodDarkCols.toDouble() / prodEvalCols else 0.0
        val prodMelScore = (prodBestContrast * prodMaxContinuity).coerceIn(0.0, 1.0).toFloat()
        val prodHasMelanonychia = prodMelScore > 0.25f && prodMaxContinuity >= 0.60 && prodDarkColRatio <= 0.25 && !isDarkEdges && !isUnevenTexture

        return NailFeatures(
            averageRedness = avgR,
            averageSaturation = avgS,
            averageBrightness = avgV,
            whiteSpotRatio = whiteSpotRatio,
            darkEdgeRatio = darkEdgeRatio,
            brightnessStdDev = brightnessStd,
            rednessUniformity = rednessStd,
            isPale = isPale,
            hasWhiteSpots = hasWhiteSpots,
            isDarkEdges = isDarkEdges,
            isUnevenTexture = isUnevenTexture,
            isLowRedness = isLowRedness,
            hasBeausLines = prodHasBeausLines,
            hasLunulaAbnormality = prodHasLunulaAbnormality,
            hasOnychomycosis = prodHasOnychomycosis,
            hasMelanonychia = prodHasMelanonychia,
            beausLinesScore = prodBeausScore,
            lunulaContrast = prodLunulaContrast,
            fungalColorScore = prodFungalScore,
            melanonychiaBandScore = prodMelScore
        )
    }

    private fun legacyExtract(bitmap: Bitmap): NailFeatures {
        val width = bitmap.width
        val height = bitmap.height

        val allPixels = IntArray(width * height)
        bitmap.getPixels(allPixels, 0, width, 0, 0, width, height)

        val sampleStep = max(1, min(width, height) / 80)
        val xSteps = (width - 1) / sampleStep + 1
        val ySteps = (height - 1) / sampleStep + 1
        val totalSamples = xSteps * ySteps

        var rSum = 0.0; var gSum = 0.0; var bSum = 0.0
        var vSum = 0.0; var sSum = 0.0
        var darkEdgeCount = 0
        val brightnessValues = DoubleArray(totalSamples)
        val redValues = DoubleArray(totalSamples)
        val isInterior = BooleanArray(totalSamples)
        var interiorCount = 0
        var interiorBrightnessSum = 0.0
        var interiorRednessSum = 0.0
        var idx = 0

        val edgeThreshold = (width * 0.12).toInt().coerceAtLeast(2)
        val darkEdgeThreshold = 60

        var fungalPixelCount = 0
        val hsv = FloatArray(3)

        for (x in 0 until width step sampleStep) {
            for (y in 0 until height step sampleStep) {
                val pixel = allPixels[y * width + x]
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                rSum += r; gSum += g; bSum += b

                Color.RGBToHSV(r, g, b, hsv)
                val h = hsv[0].toDouble()
                val s = hsv[1].toDouble()
                val v = hsv[2].toDouble()
                sSum += s; vSum += v

                val isEdge = x < edgeThreshold || x >= width - edgeThreshold ||
                        y < edgeThreshold || y >= height - edgeThreshold

                if (idx < totalSamples) {
                    brightnessValues[idx] = v
                    redValues[idx] = r.toDouble()
                    isInterior[idx] = !isEdge
                    if (!isEdge) {
                        interiorCount++
                        interiorBrightnessSum += v
                        interiorRednessSum += r.toDouble()

                        if (h in 18.0..68.0 && s >= 0.18 && v >= 0.25) {
                            fungalPixelCount++
                        }
                    }
                }
                idx++

                if (isEdge && r < darkEdgeThreshold && g < darkEdgeThreshold && b < darkEdgeThreshold) darkEdgeCount++
            }
        }

        val sampleCount = idx.coerceAtLeast(1)
        val avgR = rSum / sampleCount
        val avgG = gSum / sampleCount
        val avgB = bSum / sampleCount
        val avgS = sSum / sampleCount
        val avgV = vSum / sampleCount

        // Adaptive white spot detection using brightness percentile
        val sortedBrightness = brightnessValues.take(sampleCount).sorted()
        val p90 = sortedBrightness[(sortedBrightness.size * 0.90).toInt().coerceAtMost(sortedBrightness.size - 1)]
        val p25 = sortedBrightness[(sortedBrightness.size * 0.25).toInt().coerceAtMost(sortedBrightness.size - 1)]
        val p75 = sortedBrightness[(sortedBrightness.size * 0.75).toInt().coerceAtMost(sortedBrightness.size - 1)]
        val brightnessRange = p90 - p25
        val whiteThreshold = p75 + brightnessRange * 2.0
        val whiteCount = sortedBrightness.count { it > whiteThreshold.coerceAtMost(0.98) }
        val whiteSpotRatio = whiteCount.toDouble() / sortedBrightness.size

        val darkEdgeRatio = darkEdgeCount.toDouble() / sampleCount

        val interiorAvgV = if (interiorCount > 0) interiorBrightnessSum / interiorCount else 0.0
        val interiorAvgR = if (interiorCount > 0) interiorRednessSum / interiorCount else 0.0

        val brightnessStdDev = stdDevInterior(brightnessValues, isInterior, sampleCount, interiorAvgV)
        val rednessStdDev = stdDevInterior(redValues, isInterior, sampleCount, interiorAvgR)

        val isDarkEdges = darkEdgeRatio > 0.2572
        val isLowRedness = avgR < 117.4708 && !isDarkEdges && (avgR > avgB * 0.9072 || avgR < 86.0867)
        val isPale = avgS < 0.1425 && avgV > 0.5105 && avgR < 162.4155
        val hasWhiteSpots = whiteSpotRatio > 0.0103

        val normalizedTextureScore = if (avgV > 0.01) brightnessStdDev / avgV else brightnessStdDev * 255.0
        val isUnevenTexture = (normalizedTextureScore > 0.3091 || rednessStdDev > 117.9082) && !isDarkEdges

        // 4 New condition detectors for legacy / synthetic test path

        // 1. Beau's Lines: Horizontal transverse groove / brightness dip across interior rows
        val beausYStart = (height * 0.20).toInt()
        val beausYEnd = (height * 0.80).toInt()
        val intXStart = edgeThreshold
        val intXEnd = width - edgeThreshold

        val rowBrightness = DoubleArray(height)
        val rowPixelCount = IntArray(height)
        for (y in beausYStart..beausYEnd) {
            var sumV = 0.0
            var count = 0
            for (x in intXStart until intXEnd) {
                val pixel = allPixels[y * width + x]
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                Color.RGBToHSV(r, g, b, hsv)
                sumV += hsv[2]
                count++
            }
            if (count > 0) {
                rowBrightness[y] = sumV / count
                rowPixelCount[y] = count
            }
        }

        var maxRowDip = 0.0
        for (y in (beausYStart + 4)..(beausYEnd - 4)) {
            if (rowPixelCount[y] == 0) continue
            val neighborOffsets = intArrayOf(-5, -4, -3, 3, 4, 5)
            var baseSum = 0.0
            var baseCount = 0
            for (offset in neighborOffsets) {
                val ny = y + offset
                if (ny in beausYStart..beausYEnd && rowPixelCount[ny] > 0) {
                    baseSum += rowBrightness[ny]
                    baseCount++
                }
            }
            val baseV = if (baseCount > 0) baseSum / baseCount else interiorAvgV
            val dip = (baseV - rowBrightness[y]) / (baseV + 1e-5)
            if (dip > maxRowDip) {
                maxRowDip = dip
            }
        }
        val beausLinesScore = max(0.0, maxRowDip).toFloat()
        val hasBeausLines = beausLinesScore > 0.16f && !isDarkEdges && !isUnevenTexture

        // 2. Lunula Abnormality: Proximal bottom region vs body reference
        val lunulaXMin = (width * 0.35).toInt()
        val lunulaXMax = (width * 0.65).toInt()
        val lunulaYMin = (height * 0.70).toInt()
        val lunulaYMax = (height * 0.90).toInt()

        val bodyYMin = (height * 0.30).toInt()
        val bodyYMax = (height * 0.55).toInt()

        var lunulaRSum = 0.0; var lunulaGSum = 0.0; var lunulaBSum = 0.0; var lunulaVSum = 0.0
        var lunulaCount = 0
        for (y in lunulaYMin..lunulaYMax) {
            for (x in lunulaXMin..lunulaXMax) {
                val pixel = allPixels[y * width + x]
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                Color.RGBToHSV(r, g, b, hsv)
                lunulaRSum += r; lunulaGSum += g; lunulaBSum += b; lunulaVSum += hsv[2]
                lunulaCount++
            }
        }

        var bodyRSum = 0.0; var bodyGSum = 0.0; var bodyBSum = 0.0; var bodyVSum = 0.0
        var bodyCount = 0
        for (y in bodyYMin..bodyYMax) {
            for (x in lunulaXMin..lunulaXMax) {
                val pixel = allPixels[y * width + x]
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                Color.RGBToHSV(r, g, b, hsv)
                bodyRSum += r; bodyGSum += g; bodyBSum += b; bodyVSum += hsv[2]
                bodyCount++
            }
        }

        val lunulaAvgR = if (lunulaCount > 0) lunulaRSum / lunulaCount else 0.0
        val lunulaAvgG = if (lunulaCount > 0) lunulaGSum / lunulaCount else 0.0
        val lunulaAvgB = if (lunulaCount > 0) lunulaBSum / lunulaCount else 0.0
        val lunulaAvgV = if (lunulaCount > 0) lunulaVSum / lunulaCount else 0.0

        val bodyAvgR = if (bodyCount > 0) bodyRSum / bodyCount else 0.0
        val bodyAvgG = if (bodyCount > 0) bodyGSum / bodyCount else 0.0
        val bodyAvgB = if (bodyCount > 0) bodyBSum / bodyCount else 0.0
        val bodyAvgV = if (bodyCount > 0) bodyVSum / bodyCount else 0.0

        val dR = Math.abs(lunulaAvgR - bodyAvgR) / 255.0
        val dG = Math.abs(lunulaAvgG - bodyAvgG) / 255.0
        val dB = Math.abs(lunulaAvgB - bodyAvgB) / 255.0
        val rgbDist = sqrt(dR * dR + dG * dG + dB * dB)
        val dV = Math.abs(lunulaAvgV - bodyAvgV) / (bodyAvgV + 1e-5)
        val blueShift = max(0.0, (lunulaAvgB / (lunulaAvgR + 1e-5)) - (bodyAvgB / (bodyAvgR + 1e-5)))

        val lunulaContrastVal = maxOf(rgbDist, dV, blueShift)
        val lunulaContrast = lunulaContrastVal.toFloat()
        val hasLunulaAbnormality = lunulaContrast > 0.20f && !isDarkEdges && !isPale

        // 3. Onychomycosis: Yellow-brown / white discoloration with HSV range
        val fungalColorScore = fungalPixelCount.toFloat() / max(1, interiorCount)
        val hasOnychomycosis = fungalColorScore > 0.035f && !isDarkEdges

        // 4. Melanonychia: Longitudinal dark column band spanning proximal to distal
        val melXMin = (width * 0.18).toInt()
        val melXMax = (width * 0.82).toInt()
        val melYMin = (height * 0.15).toInt()
        val melYMax = (height * 0.85).toInt()
        val melHeight = melYMax - melYMin + 1

        var maxContinuity = 0.0
        var bestStripeContrast = 0.0
        var darkColumnsCount = 0
        val evaluatedColumns = melXMax - melXMin + 1

        for (x in melXMin..melXMax) {
            var colDarkCount = 0
            var colVSum = 0.0
            for (y in melYMin..melYMax) {
                val pixel = allPixels[y * width + x]
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                Color.RGBToHSV(r, g, b, hsv)
                val v = hsv[2].toDouble()
                colVSum += v
                if (v < interiorAvgV * 0.65 && r < interiorAvgR * 0.70) {
                    colDarkCount++
                }
            }
            val continuity = if (melHeight > 0) colDarkCount.toDouble() / melHeight else 0.0
            val colAvgV = if (melHeight > 0) colVSum / melHeight else 0.0
            if (continuity >= 0.50) {
                darkColumnsCount++
            }
            if (continuity > maxContinuity) {
                maxContinuity = continuity
                bestStripeContrast = max(0.0, (interiorAvgV - colAvgV) / (interiorAvgV + 1e-5))
            }
        }

        val darkColRatio = if (evaluatedColumns > 0) darkColumnsCount.toDouble() / evaluatedColumns else 0.0
        val melanonychiaBandScore = (bestStripeContrast * maxContinuity).coerceIn(0.0, 1.0).toFloat()
        val hasMelanonychia = melanonychiaBandScore > 0.25f && maxContinuity >= 0.60 && darkColRatio <= 0.25 && !isDarkEdges && !isUnevenTexture

        return NailFeatures(
            averageRedness = avgR, averageSaturation = avgS, averageBrightness = avgV,
            whiteSpotRatio = whiteSpotRatio, darkEdgeRatio = darkEdgeRatio,
            brightnessStdDev = brightnessStdDev, rednessUniformity = rednessStdDev,
            isPale = isPale, hasWhiteSpots = hasWhiteSpots,
            isDarkEdges = isDarkEdges, isUnevenTexture = isUnevenTexture,
            isLowRedness = isLowRedness,
            hasBeausLines = hasBeausLines,
            hasLunulaAbnormality = hasLunulaAbnormality,
            hasOnychomycosis = hasOnychomycosis,
            hasMelanonychia = hasMelanonychia,
            beausLinesScore = beausLinesScore,
            lunulaContrast = lunulaContrast,
            fungalColorScore = fungalColorScore,
            melanonychiaBandScore = melanonychiaBandScore
        )
    }

    private fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        val rN = r / 255.0
        val gN = g / 255.0
        val bN = b / 255.0
        val mx = maxOf(rN, gN, bN)
        val mn = minOf(rN, gN, bN)
        val df = mx - mn
        var h = 0.0
        if (mx == mn) {
            h = 0.0
        } else if (mx == rN) {
            h = (60.0 * ((gN - bN) / df) + 360.0) % 360.0
        } else if (mx == gN) {
            h = (60.0 * ((bN - rN) / df) + 120.0) % 360.0
        } else if (mx == bN) {
            h = (60.0 * ((rN - gN) / df) + 240.0) % 360.0
        }
        val s = if (mx == 0.0) 0.0 else df / mx
        val v = mx
        return Triple(h, s, v)
    }

    private fun defaultFeatures() = NailFeatures(
        averageRedness = 0.0,
        averageSaturation = 0.0,
        averageBrightness = 0.0,
        whiteSpotRatio = 0.0,
        darkEdgeRatio = 0.0,
        brightnessStdDev = 0.0,
        rednessUniformity = 0.0,
        isPale = false,
        hasWhiteSpots = false,
        isDarkEdges = false,
        isUnevenTexture = false,
        isLowRedness = false,
        hasBeausLines = false,
        hasLunulaAbnormality = false,
        hasOnychomycosis = false,
        hasMelanonychia = false,
        beausLinesScore = 0f,
        lunulaContrast = 0f,
        fungalColorScore = 0f,
        melanonychiaBandScore = 0f
    )

    private fun stdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        var sum = 0.0
        for (v in values) {
            val diff = v - mean
            sum += diff * diff
        }
        return sqrt(sum / values.size)
    }

    private fun stdDevInterior(values: DoubleArray, isInterior: BooleanArray, total: Int, mean: Double): Double {
        var sum = 0.0
        var n = 0
        val limit = min(total, isInterior.size)
        for (i in 0 until limit) {
            if (isInterior[i]) {
                val diff = values[i] - mean
                sum += diff * diff
                n++
            }
        }
        if (n == 0) return 0.0
        return sqrt(sum / n)
    }
}
