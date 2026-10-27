package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class NailFeatureExtractorTest {

    private fun createSyntheticBitmap(
        width: Int = 100,
        height: Int = 100,
        generator: (x: Int, y: Int) -> Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, generator(x, y))
            }
        }
        return bitmap
    }

    // 1. Healthy Nail Generator
    private fun createHealthyNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { _, _ -> Color.rgb(220, 150, 160) }
    }

    // 2. Pale Nail Generator
    private fun createPaleNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { _, _ -> Color.rgb(150, 135, 135) }
    }

    // 3. White Spots Nail Generator
    private fun createWhiteSpotsNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            if (x in 43..57 && y in 43..57) {
                Color.rgb(255, 255, 255) // White spot pixel (225 pixels = 2.25% of image)
            } else {
                Color.rgb(220, 150, 160) // Healthy pink background
            }
        }
    }

    // 4. Vertical Ridges Nail Generator
    private fun createVerticalRidgesNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, _ ->
            if (x % 2 == 0) {
                Color.rgb(220, 150, 160) // Pink
            } else {
                Color.rgb(110, 80, 90) // Dark brownish-pink
            }
        }
    }

    // 5. Dark Edges Nail Generator
    private fun createDarkEdgesNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        val edgeThreshold = 12
        return createSyntheticBitmap(width, height) { x, y ->
            val isEdge = x < edgeThreshold || x >= width - edgeThreshold ||
                         y < edgeThreshold || y >= height - edgeThreshold
            if (isEdge) {
                Color.rgb(59, 59, 59) // Dark edge pixels
            } else {
                Color.rgb(120, 60, 140) // Custom interior
            }
        }
    }

    // 6. Beau's Lines Nail Generator: Horizontal groove/brightness dip across transverse rows
    private fun createBeausLinesNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isGroove = (y in 48..52) && (x in 12 until width - 12)
            if (isGroove) {
                Color.rgb(130, 85, 95) // Shadow depression band (lower brightness)
            } else {
                Color.rgb(220, 150, 160) // Normal pink nail plate
            }
        }
    }

    // 7. Lunula Abnormality Nail Generator: Proximal bottom base anomaly (azure/cyanotic discoloration)
    private fun createLunulaAbnormalNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isLunulaAnomaly = (x in 35..65) && (y in 74..86)
            if (isLunulaAnomaly) {
                Color.rgb(110, 130, 195) // Discolored azure/blue crescent
            } else {
                Color.rgb(220, 150, 160) // Normal pink nail plate
            }
        }
    }

    // 8. Onychomycosis Nail Generator: Distal yellow-brown / mustard discoloration patch
    private fun createOnychomycosisNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isFungalPatch = (x in 20..60) && (y in 14..28)
            if (isFungalPatch) {
                Color.rgb(195, 155, 55) // Yellow-brown fungal keratin patch (H ~43°, S ~0.72, V ~0.76)
            } else {
                Color.rgb(220, 150, 160) // Normal pink nail plate
            }
        }
    }

    // 9. Melanonychia Nail Generator: Longitudinal narrow dark column band spanning proximal to distal
    private fun createMelanonychiaNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isMelanonychiaStripe = (x in 48..52) && (y in 12..87)
            if (isMelanonychiaStripe) {
                Color.rgb(40, 25, 20) // Deep dark brown/black melanin pigment band
            } else {
                Color.rgb(220, 150, 160) // Normal pink nail plate
            }
        }
    }

    /**
     * 1. Healthy: Solid pink color.
     * All 8 feature anomaly flags must evaluate to false.
     */
    @Test
    fun testHealthyNail() {
        val bitmap = createHealthyNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertEquals(220.0, features.averageRedness, 0.01)
        assertEquals(0.318, features.averageSaturation, 0.01)
        assertEquals(0.863, features.averageBrightness, 0.01)
        assertEquals(0.0, features.whiteSpotRatio, 0.01)
        assertEquals(0.0, features.darkEdgeRatio, 0.01)
        assertEquals(0.0, features.brightnessStdDev, 0.01)
        assertEquals(0.0, features.rednessUniformity, 0.01)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have Beau's lines (alias)", features.isBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have lunula abnormality (alias)", features.isLunulaAbnormal)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)

        val symptoms = features.toSymptomList()
        assertTrue("Symptom list should contain healthy status", symptoms.contains("특이사항 없음 (건강함)"))
    }

    /**
     * 2. Pale: Solid desaturated grayish pink.
     * isPale must evaluate to true, all other anomaly flags to false.
     */
    @Test
    fun testPaleNail() {
        val bitmap = createPaleNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertEquals(150.0, features.averageRedness, 0.01)
        assertEquals(0.10, features.averageSaturation, 0.01)
        assertEquals(0.588, features.averageBrightness, 0.01)
        assertEquals(0.0, features.whiteSpotRatio, 0.01)
        assertEquals(0.0, features.darkEdgeRatio, 0.01)
        assertEquals(0.0, features.brightnessStdDev, 0.01)
        assertEquals(0.0, features.rednessUniformity, 0.01)

        assertTrue("Should be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)
    }

    /**
     * 3. White Spots: Pink background with a small white square in the center.
     * hasWhiteSpots must evaluate to true, all other anomaly flags to false.
     */
    @Test
    fun testWhiteSpotsNail() {
        val bitmap = createWhiteSpotsNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertEquals(220.7875, features.averageRedness, 0.01)
        assertEquals(0.3108, features.averageSaturation, 0.01)
        assertEquals(0.8658, features.averageBrightness, 0.01)
        assertEquals(0.0225, features.whiteSpotRatio, 0.001)
        assertEquals(0.0, features.darkEdgeRatio, 0.01)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertTrue("Should have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)
    }

    /**
     * 4. Vertical Ridges: Alternating vertical stripes of pink and dark-brownish pink.
     * isUnevenTexture must evaluate to true, all other anomaly flags to false.
     */
    @Test
    fun testVerticalRidgesNail() {
        val bitmap = createVerticalRidgesNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertEquals(165.0, features.averageRedness, 0.01)
        assertEquals(0.295, features.averageSaturation, 0.01)
        assertEquals(0.647, features.averageBrightness, 0.01)
        assertEquals(0.0, features.whiteSpotRatio, 0.01)
        assertEquals(0.0, features.darkEdgeRatio, 0.01)
        assertEquals(55.0, features.rednessUniformity, 0.01)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertTrue("Should have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)
    }

    /**
     * 5. Dark Edges: Dark gray edge border (margin 12%) with custom bluish-purple interior.
     * isDarkEdges must evaluate to true, all other anomaly flags to false.
     */
    @Test
    fun testDarkEdgesNail() {
        val bitmap = createDarkEdgesNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertEquals(94.23, features.averageRedness, 0.01)
        assertEquals(0.3298, features.averageSaturation, 0.01)
        assertEquals(0.4146, features.averageBrightness, 0.01)
        assertEquals(0.0, features.whiteSpotRatio, 0.01)
        assertEquals(0.4224, features.darkEdgeRatio, 0.001)
        assertEquals(0.0, features.rednessUniformity, 0.01)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertTrue("Should have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)
    }

    /**
     * 6. Beau's Lines: Horizontal transverse groove/brightness dip.
     * hasBeausLines and isBeausLines must be true, all other anomaly flags to false.
     */
    @Test
    fun testBeausLinesNail() {
        val bitmap = createBeausLinesNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertTrue("Should have Beau's lines", features.hasBeausLines)
        assertTrue("Should match isBeausLines alias", features.isBeausLines)
        assertTrue("Beau's lines score should exceed threshold (> 0.16)", features.beausLinesScore > 0.16f)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)

        val symptoms = features.toSymptomList()
        assertTrue("Symptom list should contain beaus_lines", symptoms.contains("beaus_lines"))
        assertTrue("Symptom list should contain Beau's lines Korean title",
            symptoms.any { it.contains("보우선") })
    }

    /**
     * 7. Lunula Abnormality: Proximal bottom crescent anomaly / cyanotic discoloration.
     * hasLunulaAbnormality and isLunulaAbnormal must be true, all other anomaly flags to false.
     */
    @Test
    fun testLunulaAbnormalityNail() {
        val bitmap = createLunulaAbnormalNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertTrue("Should have lunula abnormality", features.hasLunulaAbnormality)
        assertTrue("Should match isLunulaAbnormal alias", features.isLunulaAbnormal)
        assertTrue("Lunula contrast score should exceed threshold (> 0.20)", features.lunulaContrast > 0.20f)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)

        val symptoms = features.toSymptomList()
        assertTrue("Symptom list should contain lunula_abnormality", symptoms.contains("lunula_abnormality"))
        assertTrue("Symptom list should contain Lunula Korean title",
            symptoms.any { it.contains("반월") })
    }

    /**
     * 8. Onychomycosis: Distal yellow-brown fungal keratin discoloration.
     * hasOnychomycosis must be true, all other anomaly flags to false.
     */
    @Test
    fun testOnychomycosisNail() {
        val bitmap = createOnychomycosisNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertTrue("Should have onychomycosis", features.hasOnychomycosis)
        assertTrue("Fungal color score should exceed threshold (> 0.035)", features.fungalColorScore > 0.035f)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)

        val symptoms = features.toSymptomList()
        assertTrue("Symptom list should contain onychomycosis", symptoms.contains("onychomycosis"))
        assertTrue("Symptom list should contain Onychomycosis Korean title",
            symptoms.any { it.contains("조갑진균증") })
    }

    /**
     * 9. Melanonychia: Longitudinal dark column band spanning proximal to distal in interior.
     * hasMelanonychia must be true, all other anomaly flags to false.
     */
    @Test
    fun testMelanonychiaNail() {
        val bitmap = createMelanonychiaNailBitmap()
        val features = NailFeatureExtractor.extract(bitmap)

        assertTrue("Should have melanonychia", features.hasMelanonychia)
        assertTrue("Melanonychia band score should exceed threshold (> 0.25)", features.melanonychiaBandScore > 0.25f)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)

        val symptoms = features.toSymptomList()
        assertTrue("Symptom list should contain melanonychia", symptoms.contains("melanonychia"))
        assertTrue("Symptom list should contain Melanonychia Korean title",
            symptoms.any { it.contains("흑색선조") })
    }

    /**
     * 10. Production Resolution (Camera Scale): Verifies center-crop, scaling, and adaptive extraction pipeline.
     */
    @Test
    fun testCameraScaleHealthyExtraction() {
        val bitmap = createHealthyNailBitmap(width = 160, height = 200)
        val features = NailFeatureExtractor.extract(bitmap)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertFalse("Should not have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
        assertFalse("Should not have Beau's lines", features.hasBeausLines)
        assertFalse("Should not have lunula abnormality", features.hasLunulaAbnormality)
        assertFalse("Should not have onychomycosis", features.hasOnychomycosis)
        assertFalse("Should not have melanonychia", features.hasMelanonychia)
    }

    /**
     * 11. Performance Benchmark: 20 warm-up + 100 timed iterations; assert average execution time < 50.0ms.
     */
    @Test
    fun testPerformance() {
        val bitmap = createHealthyNailBitmap()
        
        // Warm up JIT
        for (i in 0 until 20) {
            NailFeatureExtractor.extract(bitmap)
        }
        
        val iterations = 100
        val startTime = System.nanoTime()
        for (i in 0 until iterations) {
            NailFeatureExtractor.extract(bitmap)
        }
        val durationNs = System.nanoTime() - startTime
        val avgTimeMs = durationNs.toDouble() / (iterations * 1_000_000.0)
        
        println("Average execution time: $avgTimeMs ms")
        assertTrue("Average execution time should be under 50ms, was $avgTimeMs ms", avgTimeMs < 50.0)
    }

    /**
     * 12. Multi-Condition Latency Benchmark: Verifies extraction performance across all 9 conditions < 50.0ms.
     */
    @Test
    fun testPerformanceAcrossAllConditions() {
        val conditions = listOf(
            createHealthyNailBitmap(),
            createPaleNailBitmap(),
            createWhiteSpotsNailBitmap(),
            createVerticalRidgesNailBitmap(),
            createDarkEdgesNailBitmap(),
            createBeausLinesNailBitmap(),
            createLunulaAbnormalNailBitmap(),
            createOnychomycosisNailBitmap(),
            createMelanonychiaNailBitmap()
        )

        // Warm up JIT
        for (bmp in conditions) {
            for (i in 0 until 5) {
                NailFeatureExtractor.extract(bmp)
            }
        }

        val iterationsPerCondition = 20
        val startTime = System.nanoTime()
        for (bmp in conditions) {
            for (i in 0 until iterationsPerCondition) {
                NailFeatureExtractor.extract(bmp)
            }
        }
        val totalIterations = conditions.size * iterationsPerCondition
        val durationNs = System.nanoTime() - startTime
        val avgTimeMs = durationNs.toDouble() / (totalIterations * 1_000_000.0)

        println("Average execution time across all 9 conditions: $avgTimeMs ms")
        assertTrue("Average execution time across all conditions must be under 50ms, was $avgTimeMs ms", avgTimeMs < 50.0)
    }
}
