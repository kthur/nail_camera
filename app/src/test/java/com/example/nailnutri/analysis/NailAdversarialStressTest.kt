package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class NailAdversarialStressTest {

    private fun createUniformBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun createNoiseBitmap(width: Int, height: Int, seed: Long = 42L): Bitmap {
        val rand = Random(seed)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val r = rand.nextInt(256)
                val g = rand.nextInt(256)
                val b = rand.nextInt(256)
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bitmap
    }

    private fun createSaltAndPepperBitmap(width: Int, height: Int, seed: Long = 1337L): Bitmap {
        val rand = Random(seed)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if (rand.nextBoolean()) Color.rgb(255, 255, 255) else Color.rgb(0, 0, 0)
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    /**
     * 1. Boundary Image Dimensions Stress Test
     * Tests minimum, threshold boundary, extreme aspect ratio, and production camera dimensions:
     * 1x1, 2x2, 10x10, 120x120 (legacy bound), 121x121 (prod bound), 1000x121, 121x1000, 1080x1920, 1920x1080.
     */
    @Test
    fun testBoundaryImageDimensions() {
        val dimensions = listOf(
            Pair(1, 1),
            Pair(2, 2),
            Pair(10, 10),
            Pair(120, 120),
            Pair(121, 121),
            Pair(150, 200),
            Pair(200, 150),
            Pair(1000, 121),
            Pair(121, 1000),
            Pair(1080, 1920),
            Pair(1920, 1080)
        )

        for ((w, h) in dimensions) {
            val bmp = createUniformBitmap(w, h, Color.rgb(220, 150, 160))
            val features = NailFeatureExtractor.extract(bmp)
            assertNotNull("Features should not be null for dimensions ${w}x${h}", features)

            // Test full classification pipeline
            val result = NailClassifier.classify(bmp, "test_${w}x${h}.jpg", null)
            assertNotNull("Analysis result should not be null for ${w}x${h}", result)
            assertTrue("Symptoms should not be empty for ${w}x${h}", result.symptoms.isNotEmpty())

            // Assert SymptomRegion normalization contract on boundary dimensions
            for (region in result.symptomRegions) {
                assertTrue("xMin >= 0.0 for ${region.label} at ${w}x${h}", region.xMin >= 0.0f)
                assertTrue("xMax <= 1.0 for ${region.label} at ${w}x${h}", region.xMax <= 1.0f)
                assertTrue("yMin >= 0.0 for ${region.label} at ${w}x${h}", region.yMin >= 0.0f)
                assertTrue("yMax <= 1.0 for ${region.label} at ${w}x${h}", region.yMax <= 1.0f)
                assertTrue("xMin < xMax for ${region.label} at ${w}x${h}", region.xMin < region.xMax)
                assertTrue("yMin < yMax for ${region.label} at ${w}x${h}", region.yMin < region.yMax)
            }
        }
    }

    /**
     * 2. Extreme Inputs: Completely Dark, Completely White, and Uniform Solids
     * Tests behavior with solid black (0,0,0), solid white (255,255,255), pure RGB primaries, and mid-gray.
     */
    @Test
    fun testExtremeSolidColorInputs() {
        val testConfigs = listOf(
            Pair("Solid Black (0,0,0) - Synthetic", createUniformBitmap(100, 100, Color.rgb(0, 0, 0))),
            Pair("Solid Black (0,0,0) - Camera Scale", createUniformBitmap(160, 200, Color.rgb(0, 0, 0))),
            Pair("Solid White (255,255,255) - Synthetic", createUniformBitmap(100, 100, Color.rgb(255, 255, 255))),
            Pair("Solid White (255,255,255) - Camera Scale", createUniformBitmap(160, 200, Color.rgb(255, 255, 255))),
            Pair("Pure Red (255,0,0)", createUniformBitmap(120, 120, Color.rgb(255, 0, 0))),
            Pair("Pure Green (0,255,0)", createUniformBitmap(120, 120, Color.rgb(0, 255, 0))),
            Pair("Pure Blue (0,0,255)", createUniformBitmap(120, 120, Color.rgb(0, 0, 255))),
            Pair("Neutral Gray (128,128,128)", createUniformBitmap(120, 120, Color.rgb(128, 128, 128)))
        )

        for ((desc, bmp) in testConfigs) {
            val features = NailFeatureExtractor.extract(bmp)
            assertNotNull("Features must not be null for $desc", features)

            // Verify numeric outputs are finite and not NaN
            assertFalse("averageRedness must not be NaN for $desc", features.averageRedness.isNaN())
            assertFalse("averageSaturation must not be NaN for $desc", features.averageSaturation.isNaN())
            assertFalse("averageBrightness must not be NaN for $desc", features.averageBrightness.isNaN())
            assertFalse("beausLinesScore must not be NaN for $desc", features.beausLinesScore.isNaN())
            assertFalse("lunulaContrast must not be NaN for $desc", features.lunulaContrast.isNaN())
            assertFalse("fungalColorScore must not be NaN for $desc", features.fungalColorScore.isNaN())
            assertFalse("melanonychiaBandScore must not be NaN for $desc", features.melanonychiaBandScore.isNaN())

            val result = NailClassifier.classify(bmp, "extreme_${desc}.jpg", null)
            assertNotNull("Classifier result must not be null for $desc", result)
            assertTrue("Should produce at least one symptom for $desc", result.symptoms.isNotEmpty())

            for (r in result.symptomRegions) {
                assertTrue("xMin in [0,1] for $desc", r.xMin in 0.0f..1.0f)
                assertTrue("xMax in [0,1] for $desc", r.xMax in 0.0f..1.0f)
                assertTrue("yMin in [0,1] for $desc", r.yMin in 0.0f..1.0f)
                assertTrue("yMax in [0,1] for $desc", r.yMax in 0.0f..1.0f)
                assertTrue("xMin < xMax for $desc", r.xMin < r.xMax)
                assertTrue("yMin < yMax for $desc", r.yMin < r.yMax)
            }
        }
    }

    /**
     * 3. Noisy Salt-and-Pepper and High-Frequency Noise Bitmaps
     * Verifies that noise does not cause index out of bounds, division by zero, or crash.
     */
    @Test
    fun testNoisyBitmapsRobustness() {
        val noisyBitmaps = listOf(
            Pair("Salt-and-Pepper 100x100", createSaltAndPepperBitmap(100, 100)),
            Pair("Salt-and-Pepper 200x200", createSaltAndPepperBitmap(200, 200)),
            Pair("RGB Noise 100x100", createNoiseBitmap(100, 100)),
            Pair("RGB Noise 200x250", createNoiseBitmap(200, 250))
        )

        for ((desc, bmp) in noisyBitmaps) {
            val features = NailFeatureExtractor.extract(bmp)
            assertNotNull("Features must not be null for $desc", features)

            val symptoms = features.toSymptomList()
            assertTrue("toSymptomList must return non-empty list for $desc", symptoms.isNotEmpty())

            val descKorean = features.toKoreanDescription()
            assertTrue("Korean description must be non-empty for $desc", descKorean.isNotBlank())

            val result = NailClassifier.classify(bmp, "noisy_${desc}.jpg", null)
            assertNotNull("Classification result must not be null for $desc", result)

            for (r in result.symptomRegions) {
                assertTrue("xMin < xMax for $desc (${r.label})", r.xMin < r.xMax)
                assertTrue("yMin < yMax for $desc (${r.label})", r.yMin < r.yMax)
                assertTrue("Coords in [0, 1] for $desc", r.xMin >= 0f && r.xMax <= 1f && r.yMin >= 0f && r.yMax <= 1f)
            }
        }
    }

    /**
     * 4. Zero-Division & Arithmetic Safety Hazards
     * Tests conditions specifically crafted to trigger zero denominators:
     * - Uniform flat image where neighbor differences are exactly 0.0
     * - Uniform image with 0 brightness (v=0)
     * - Image where red=0, green=0, blue=0 (HSV max=0, df=0)
     */
    @Test
    fun testZeroDivisionHazards() {
        val zeroDenomBmp = createUniformBitmap(150, 200, Color.rgb(0, 0, 0))
        val features = NailFeatureExtractor.extract(zeroDenomBmp)

        assertEquals("Score should be 0 for flat zero image", 0f, features.beausLinesScore, 0.001f)
        assertEquals("Lunula contrast should be 0", 0f, features.lunulaContrast, 0.001f)
        assertEquals("Fungal score should be 0", 0f, features.fungalColorScore, 0.001f)
        assertEquals("Melanonychia score should be 0", 0f, features.melanonychiaBandScore, 0.001f)

        val flatPinkBmp = createUniformBitmap(150, 200, Color.rgb(220, 150, 160))
        val flatFeatures = NailFeatureExtractor.extract(flatPinkBmp)

        assertFalse("Flat pink should have no Beau's lines", flatFeatures.hasBeausLines)
        assertFalse("Flat pink should have no Lunula abnormality", flatFeatures.hasLunulaAbnormality)
        assertFalse("Flat pink should have no Onychomycosis", flatFeatures.hasOnychomycosis)
        assertFalse("Flat pink should have no Melanonychia", flatFeatures.hasMelanonychia)
    }

    /**
     * 5. Extraction Latency Under Stress (Camera Scale 1080x1920)
     * Must remain strictly under 50ms per frame.
     */
    @Test
    fun testCameraScaleLatencyUnder50ms() {
        val cameraBmp = createUniformBitmap(1080, 1920, Color.rgb(220, 150, 160))

        // JIT warm-up
        for (i in 0 until 5) {
            NailFeatureExtractor.extract(cameraBmp)
        }

        val iterations = 30
        val startNs = System.nanoTime()
        for (i in 0 until iterations) {
            NailFeatureExtractor.extract(cameraBmp)
        }
        val elapsedNs = System.nanoTime() - startNs
        val avgLatencyMs = elapsedNs.toDouble() / (iterations * 1_000_000.0)

        println("Average extraction latency for 1080x1920 camera frame: $avgLatencyMs ms")
        assertTrue(
            "Extraction latency must be strictly under 50ms, but was $avgLatencyMs ms",
            avgLatencyMs < 50.0
        )
    }

    /**
     * 6. Noisy Frame Latency Under Stress (Camera Scale 540x960)
     * Must remain strictly under 50ms even with maximum texture noise.
     */
    @Test
    fun testNoisyFrameLatencyUnder50ms() {
        val noisyBmp = createNoiseBitmap(540, 960)

        // JIT warm-up
        for (i in 0 until 5) {
            NailFeatureExtractor.extract(noisyBmp)
        }

        val iterations = 20
        val startNs = System.nanoTime()
        for (i in 0 until iterations) {
            NailFeatureExtractor.extract(noisyBmp)
        }
        val elapsedNs = System.nanoTime() - startNs
        val avgLatencyMs = elapsedNs.toDouble() / (iterations * 1_000_000.0)

        println("Average extraction latency for 540x960 noisy frame: $avgLatencyMs ms")
        assertTrue(
            "Noisy extraction latency must be strictly under 50ms, but was $avgLatencyMs ms",
            avgLatencyMs < 50.0
        )
    }

    /**
     * 7. Full Classification Pipeline End-to-End Latency Under Stress
     * Verifies that NailClassifier.classify() on a high-resolution frame executes < 50ms.
     */
    @Test
    fun testFullClassifyLatencyUnder50ms() {
        val cameraBmp = createUniformBitmap(1080, 1920, Color.rgb(220, 150, 160))

        // JIT warm-up
        for (i in 0 until 3) {
            NailClassifier.classify(cameraBmp, "perf_test.jpg", null)
        }

        val iterations = 20
        val startNs = System.nanoTime()
        for (i in 0 until iterations) {
            NailClassifier.classify(cameraBmp, "perf_test.jpg", null)
        }
        val elapsedNs = System.nanoTime() - startNs
        val avgLatencyMs = elapsedNs.toDouble() / (iterations * 1_000_000.0)

        println("Average full classify latency for 1080x1920 camera frame: $avgLatencyMs ms")
        assertTrue(
            "Full classify latency must be strictly under 50ms, but was $avgLatencyMs ms",
            avgLatencyMs < 50.0
        )
    }
}
