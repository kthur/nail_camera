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

    /**
     * 1. Healthy: Solid pink color.
     * All feature flags must evaluate to false.
     */
    @Test
    fun testHealthyNail() {
        val bitmap = createSyntheticBitmap { _, _ -> Color.rgb(220, 150, 160) }
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
    }

    /**
     * 2. Pale: Solid desaturated grayish pink.
     * isPale must evaluate to true, other flags to false.
     */
    @Test
    fun testPaleNail() {
        val bitmap = createSyntheticBitmap { _, _ -> Color.rgb(150, 135, 135) }
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
    }

    /**
     * 3. White Spots: Pink background with a small white square in the center.
     * hasWhiteSpots must evaluate to true, other flags to false.
     */
    @Test
    fun testWhiteSpotsNail() {
        val bitmap = createSyntheticBitmap { x, y ->
            if (x in 43..57 && y in 43..57) {
                Color.rgb(255, 255, 255) // White spot pixel (225 pixels = 2.25% of image)
            } else {
                Color.rgb(220, 150, 160) // Healthy pink background
            }
        }
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
    }

    /**
     * 4. Vertical Ridges: Alternating vertical stripes of pink and dark-brownish pink.
     * isUnevenTexture must evaluate to true, other flags to false.
     */
    @Test
    fun testVerticalRidgesNail() {
        val bitmap = createSyntheticBitmap { x, _ ->
            if (x % 2 == 0) {
                Color.rgb(220, 150, 160) // Pink
            } else {
                Color.rgb(110, 80, 90) // Dark brownish-pink
            }
        }
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
    }

    /**
     * 5. Dark Edges: Dark gray edge border (margin 12%) with custom bluish-purple interior.
     * isDarkEdges must evaluate to true, other flags (including low redness) to false.
     */
    @Test
    fun testDarkEdgesNail() {
        val edgeThreshold = 12
        val bitmap = createSyntheticBitmap { x, y ->
            val isEdge = x < edgeThreshold || x >= 100 - edgeThreshold ||
                         y < edgeThreshold || y >= 100 - edgeThreshold
            if (isEdge) {
                Color.rgb(59, 59, 59) // Dark edge pixels (42.24% of image)
            } else {
                Color.rgb(120, 60, 140) // Custom interior to balance averages
            }
        }
        val features = NailFeatureExtractor.extract(bitmap)

        assertEquals(94.23, features.averageRedness, 0.01)
        assertEquals(0.3298, features.averageSaturation, 0.01)
        assertEquals(0.4146, features.averageBrightness, 0.01)
        assertEquals(0.0, features.whiteSpotRatio, 0.01)
        assertEquals(0.4224, features.darkEdgeRatio, 0.001)
        assertEquals(30.13, features.rednessUniformity, 0.01)

        assertFalse("Should not be pale", features.isPale)
        assertFalse("Should not have low redness", features.isLowRedness)
        assertFalse("Should not have white spots", features.hasWhiteSpots)
        assertTrue("Should have dark edges", features.isDarkEdges)
        assertFalse("Should not have uneven texture", features.isUnevenTexture)
    }

    /**
     * 6. Performance: Verify average execution time is under 50ms.
     */
    @Test
    fun testPerformance() {
        val bitmap = createSyntheticBitmap { _, _ -> Color.rgb(220, 150, 160) }
        
        // Warm up
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
}
