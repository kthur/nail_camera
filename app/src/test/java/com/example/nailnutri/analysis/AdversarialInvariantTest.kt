package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import com.example.nailnutri.data.SymptomRegion
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

class AdversarialInvariantTest {

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

    private val localizeSymptomsMethod: Method by lazy {
        NailClassifier::class.java.getDeclaredMethod("localizeSymptoms", String::class.java, Bitmap::class.java).apply {
            isAccessible = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokeLocalizeSymptoms(label: String, bitmap: Bitmap): List<SymptomRegion> {
        return localizeSymptomsMethod.invoke(NailClassifier, label, bitmap) as List<SymptomRegion>
    }

    private fun assertValidSymptomRegion(region: SymptomRegion, context: String) {
        assertNotNull("$context: region cannot be null", region)
        assertTrue("$context: label must not be blank", region.label.isNotBlank())
        assertTrue("$context: xMin (${region.xMin}) must be >= 0.0f", region.xMin >= 0.0f)
        assertTrue("$context: xMax (${region.xMax}) must be <= 1.0f", region.xMax <= 1.0f)
        assertTrue("$context: yMin (${region.yMin}) must be >= 0.0f", region.yMin >= 0.0f)
        assertTrue("$context: yMax (${region.yMax}) must be <= 1.0f", region.yMax <= 1.0f)
        assertTrue("$context: xMin (${region.xMin}) must be strictly less than xMax (${region.xMax})", region.xMin < region.xMax)
        assertTrue("$context: yMin (${region.yMin}) must be strictly less than yMax (${region.yMax})", region.yMin < region.yMax)
    }

    // =========================================================================
    // 1. INVARIANT TESTS: SymptomRegion bounds under normal, edge, & extreme inputs
    // =========================================================================

    @Test
    fun testAllConditionsSymptomRegionInvariantsStandardBitmaps() {
        val conditions = listOf(
            "white_spots" to createSyntheticBitmap(100, 100) { x, y ->
                if (x in 40..60 && y in 40..60) Color.rgb(255, 255, 255) else Color.rgb(220, 150, 160)
            },
            "vertical_ridges" to createSyntheticBitmap(100, 100) { x, _ ->
                if (x % 2 == 0) Color.rgb(220, 150, 160) else Color.rgb(110, 80, 90)
            },
            "spoon_nails" to createSyntheticBitmap(100, 100) { _, _ -> Color.rgb(220, 150, 160) },
            "brittle" to createSyntheticBitmap(100, 100) { _, _ -> Color.rgb(220, 150, 160) },
            "beaus_lines" to createSyntheticBitmap(100, 100) { x, y ->
                if (y in 48..52 && x in 12..88) Color.rgb(130, 85, 95) else Color.rgb(220, 150, 160)
            },
            "lunula_abnormality" to createSyntheticBitmap(100, 100) { x, y ->
                if (x in 35..65 && y in 74..86) Color.rgb(110, 130, 195) else Color.rgb(220, 150, 160)
            },
            "onychomycosis" to createSyntheticBitmap(100, 100) { x, y ->
                if (x in 20..60 && y in 14..28) Color.rgb(195, 155, 55) else Color.rgb(220, 150, 160)
            },
            "melanonychia" to createSyntheticBitmap(100, 100) { x, y ->
                if (x in 48..52 && y in 12..87) Color.rgb(40, 25, 20) else Color.rgb(220, 150, 160)
            }
        )

        for ((condition, bitmap) in conditions) {
            val regions = invokeLocalizeSymptoms(condition, bitmap)
            assertTrue("Regions for $condition should not be empty", regions.isNotEmpty())
            for (region in regions) {
                assertValidSymptomRegion(region, "Standard $condition")
            }
        }
    }

    @Test
    fun testAllConditionsSymptomRegionInvariantsCameraScaleBitmaps() {
        val scales = listOf(Pair(160, 200), Pair(300, 400), Pair(150, 150))
        val conditionLabels = listOf("beaus_lines", "lunula_abnormality", "onychomycosis", "melanonychia", "spoon_nails", "brittle")

        for ((w, h) in scales) {
            val bitmap = createSyntheticBitmap(w, h) { x, y ->
                // Mixed pattern bitmap
                when {
                    y in (h * 0.45).toInt()..(h * 0.55).toInt() -> Color.rgb(130, 85, 95)
                    x in (w * 0.48).toInt()..(w * 0.52).toInt() -> Color.rgb(40, 25, 20)
                    y > (h * 0.75).toInt() -> Color.rgb(110, 130, 195)
                    else -> Color.rgb(220, 150, 160)
                }
            }

            for (label in conditionLabels) {
                val regions = invokeLocalizeSymptoms(label, bitmap)
                assertTrue("Regions for $label at scale ${w}x${h} should not be empty", regions.isNotEmpty())
                for (region in regions) {
                    assertValidSymptomRegion(region, "Scale ${w}x${h} label $label")
                }
            }
        }
    }

    @Test
    fun testNailClassifierClassifyAll9ConditionsSymptomRegionInvariants() {
        // Test classify() end-to-end on all 9 generator conditions
        val generators = listOf(
            "healthy" to createSyntheticBitmap { _, _ -> Color.rgb(220, 150, 160) },
            "pale" to createSyntheticBitmap { _, _ -> Color.rgb(150, 135, 135) },
            "white_spots" to createSyntheticBitmap { x, y ->
                if (x in 43..57 && y in 43..57) Color.rgb(255, 255, 255) else Color.rgb(220, 150, 160)
            },
            "vertical_ridges" to createSyntheticBitmap { x, _ ->
                if (x % 2 == 0) Color.rgb(220, 150, 160) else Color.rgb(110, 80, 90)
            },
            "dark_edges" to createSyntheticBitmap { x, y ->
                val isEdge = x < 12 || x >= 88 || y < 12 || y >= 88
                if (isEdge) Color.rgb(59, 59, 59) else Color.rgb(120, 60, 140)
            },
            "beaus_lines" to createSyntheticBitmap { x, y ->
                val isGroove = (y in 48..52) && (x in 12..88)
                if (isGroove) Color.rgb(130, 85, 95) else Color.rgb(220, 150, 160)
            },
            "lunula_abnormality" to createSyntheticBitmap { x, y ->
                val isLunula = (x in 35..65) && (y in 74..86)
                if (isLunula) Color.rgb(110, 130, 195) else Color.rgb(220, 150, 160)
            },
            "onychomycosis" to createSyntheticBitmap { x, y ->
                val isFungal = (x in 20..60) && (y in 14..28)
                if (isFungal) Color.rgb(195, 155, 55) else Color.rgb(220, 150, 160)
            },
            "melanonychia" to createSyntheticBitmap { x, y ->
                val isStripe = (x in 48..52) && (y in 12..87)
                if (isStripe) Color.rgb(40, 25, 20) else Color.rgb(220, 150, 160)
            }
        )

        for ((name, bmp) in generators) {
            val result = NailClassifier.classify(bmp, "adversarial_$name.jpg", null)
            assertNotNull(result)
            for (region in result.symptomRegions) {
                assertValidSymptomRegion(region, "classify() for $name")
            }
        }
    }

    // =========================================================================
    // 2. CROSS-INTERFERENCE TESTS: Stress-test mutual exclusivity between pairs
    // =========================================================================

    @Test
    fun testVerticalRidgesCannotTriggerBeausLines() {
        // Vertical ridges at varying frequencies and contrast levels
        for (step in listOf(2, 3, 4, 5)) {
            val bmp = createSyntheticBitmap(100, 100) { x, _ ->
                if ((x / step) % 2 == 0) Color.rgb(220, 150, 160) else Color.rgb(90, 60, 70)
            }
            val features = NailFeatureExtractor.extract(bmp)
            assertFalse("Vertical ridges (step $step) must NOT trigger hasBeausLines", features.hasBeausLines)
            assertFalse("Vertical ridges (step $step) must NOT trigger isBeausLines", features.isBeausLines)
        }
    }

    @Test
    fun testPerimeterDarkEdgesCannotTriggerMelanonychia() {
        // Perimeter dark edges at different borders (8%, 12%, 16%, 20%)
        for (borderPct in listOf(0.08, 0.12, 0.16, 0.20)) {
            val bmp = createSyntheticBitmap(100, 100) { x, y ->
                val b = (100 * borderPct).toInt()
                val isEdge = x < b || x >= 100 - b || y < b || y >= 100 - b
                if (isEdge) Color.rgb(30, 30, 30) else Color.rgb(200, 140, 150)
            }
            val features = NailFeatureExtractor.extract(bmp)
            assertFalse("Dark border (margin $borderPct) must NOT trigger hasMelanonychia", features.hasMelanonychia)
        }
    }

    @Test
    fun testWhiteSpotsCannotTriggerOnychomycosis() {
        // White spots with different sizes and intensities (pure white, off-white, light gray)
        for (spotColor in listOf(Color.rgb(255, 255, 255), Color.rgb(245, 240, 240), Color.rgb(230, 230, 230))) {
            val bmp = createSyntheticBitmap(100, 100) { x, y ->
                if (x in 40..60 && y in 40..60) spotColor else Color.rgb(220, 150, 160)
            }
            val features = NailFeatureExtractor.extract(bmp)
            assertFalse("White spots must NOT trigger hasOnychomycosis", features.hasOnychomycosis)
        }
    }

    @Test
    fun testBeausLinesCannotTriggerVerticalRidges() {
        // Horizontal groove at different vertical rows
        for (grooveY in listOf(30, 50, 70)) {
            val bmp = createSyntheticBitmap(100, 100) { x, y ->
                val isGroove = (y in (grooveY - 2)..(grooveY + 2)) && (x in 15..85)
                if (isGroove) Color.rgb(120, 75, 85) else Color.rgb(220, 150, 160)
            }
            val features = NailFeatureExtractor.extract(bmp)
            assertTrue("Beau's lines at y=$grooveY must be detected", features.hasBeausLines)
            assertFalse("Beau's lines at y=$grooveY must NOT trigger isUnevenTexture", features.isUnevenTexture)
        }
    }

    @Test
    fun testMelanonychiaCannotTriggerDarkEdges() {
        // Narrow longitudinal stripe at various x-columns
        for (stripeX in listOf(30, 50, 70)) {
            val bmp = createSyntheticBitmap(100, 100) { x, y ->
                val isStripe = (x in (stripeX - 2)..(stripeX + 2)) && (y in 15..85)
                if (isStripe) Color.rgb(40, 25, 20) else Color.rgb(220, 150, 160)
            }
            val features = NailFeatureExtractor.extract(bmp)
            assertTrue("Melanonychia at x=$stripeX must be detected", features.hasMelanonychia)
            assertFalse("Melanonychia at x=$stripeX must NOT trigger isDarkEdges", features.isDarkEdges)
        }
    }

    // =========================================================================
    // 3. CLINICAL PROFILE COMPLETENESS: Detailed verification of nutrients & advice
    // =========================================================================

    @Test
    fun testCompletenessOfAllFourNewConditionsNutrientProfiles() {
        val newConditions = listOf("beaus_lines", "lunula_abnormality", "onychomycosis", "melanonychia")

        for (cond in newConditions) {
            val result = NailClassifier.buildResultForCondition(cond, "test_$cond.jpg")

            // Symptoms
            assertTrue("$cond must have non-empty symptoms", result.symptoms.isNotEmpty())
            assertTrue("$cond symptoms must not contain blank strings", result.symptoms.all { it.isNotBlank() })

            // Deficient nutrients
            assertTrue("$cond must have non-empty deficient nutrients", result.deficientNutrients.isNotEmpty())
            for (nutrient in result.deficientNutrients) {
                assertTrue("$cond: nutrient name must not be blank", nutrient.name.isNotBlank())
                assertTrue("$cond: severity must be Severe or Moderate (was ${nutrient.severity})",
                    nutrient.severity in listOf("Severe", "Moderate", "High"))
                assertTrue("$cond: explanation must not be blank", nutrient.symptomExplanation.isNotBlank())
                assertTrue("$cond: recommended foods must not be empty", nutrient.recommendedFoods.isNotEmpty())
                assertTrue("$cond: recommended foods must all be non-blank", nutrient.recommendedFoods.all { it.isNotBlank() })
            }

            // Sufficient nutrients
            assertTrue("$cond must have non-empty sufficient nutrients", result.sufficientNutrients.isNotEmpty())
            for (suf in result.sufficientNutrients) {
                assertTrue("$cond: sufficient nutrient name must not be blank", suf.name.isNotBlank())
                assertTrue("$cond: sufficient nutrient explanation must not be blank", suf.symptomExplanation.isNotBlank())
                assertTrue("$cond: sufficient nutrient role must not be blank", suf.role.isNotBlank())
            }

            // Overall advice
            assertTrue("$cond must have non-empty overallAdvice", result.overallAdvice.isNotBlank())
        }
    }

    @Test
    fun testDermatologistGuidancePresenceForClinicalAlertConditions() {
        // Onychomycosis (fungal infection) requires dermatologist advice
        val onychoResult = NailClassifier.buildResultForCondition("onychomycosis", "dummy.jpg")
        assertTrue(
            "Onychomycosis advice must advise consulting a dermatologist (피부과 전문의)",
            onychoResult.overallAdvice.contains("피부과")
        )

        // Melanonychia (longitudinal dark stripe / Hutchinson's sign risk) requires dermatologist advice
        val melanoResult = NailClassifier.buildResultForCondition("melanonychia", "dummy.jpg")
        assertTrue(
            "Melanonychia advice must advise consulting a dermatologist (피부과 전문의)",
            melanoResult.overallAdvice.contains("피부과")
        )
    }

    @Test
    fun testExistingConditionsProfileIntegrity() {
        val existing = listOf("healthy", "white_spots", "vertical_ridges", "spoon_nails", "brittle")
        for (cond in existing) {
            val result = NailClassifier.buildResultForCondition(cond, "test_$cond.jpg")
            assertTrue("$cond: symptoms not empty", result.symptoms.isNotEmpty())
            assertTrue("$cond: overallAdvice not empty", result.overallAdvice.isNotBlank())

            if (cond == "healthy") {
                assertTrue("healthy has 0 deficient nutrients", result.deficientNutrients.isEmpty())
                assertTrue("healthy has at least 4 sufficient nutrients", result.sufficientNutrients.size >= 4)
            } else {
                assertTrue("$cond: deficient nutrients not empty", result.deficientNutrients.isNotEmpty())
                for (d in result.deficientNutrients) {
                    assertTrue("$cond: foods not empty", d.recommendedFoods.isNotEmpty())
                }
            }
        }
    }
}
