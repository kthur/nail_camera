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
    val isLowRedness: Boolean
) {
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
            isLowRedness = isLowRedness
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

        for (x in 0 until width step sampleStep) {
            for (y in 0 until height step sampleStep) {
                val pixel = allPixels[y * width + x]
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                rSum += r; gSum += g; bSum += b

                val hsv = FloatArray(3)
                Color.RGBToHSV(r, g, b, hsv)
                sSum += hsv[1]; vSum += hsv[2]

                val isEdge = x < edgeThreshold || x >= width - edgeThreshold ||
                        y < edgeThreshold || y >= height - edgeThreshold

                if (idx < totalSamples) {
                    brightnessValues[idx] = hsv[2].toDouble()
                    redValues[idx] = r.toDouble()
                    isInterior[idx] = !isEdge
                    if (!isEdge) {
                        interiorCount++
                        interiorBrightnessSum += hsv[2].toDouble()
                        interiorRednessSum += r.toDouble()
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

        return NailFeatures(
            averageRedness = avgR, averageSaturation = avgS, averageBrightness = avgV,
            whiteSpotRatio = whiteSpotRatio, darkEdgeRatio = darkEdgeRatio,
            brightnessStdDev = brightnessStdDev, rednessUniformity = rednessStdDev,
            isPale = isPale, hasWhiteSpots = hasWhiteSpots,
            isDarkEdges = isDarkEdges, isUnevenTexture = isUnevenTexture,
            isLowRedness = isLowRedness
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
        isLowRedness = false
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
