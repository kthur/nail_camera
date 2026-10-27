package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class NailClassifierTest {

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

    private fun createBeausLinesNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isGroove = (y in 48..52) && (x in 12 until width - 12)
            if (isGroove) Color.rgb(130, 85, 95) else Color.rgb(220, 150, 160)
        }
    }

    private fun createLunulaAbnormalNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isLunula = (x in 35..65) && (y in 74..86)
            if (isLunula) Color.rgb(110, 130, 195) else Color.rgb(220, 150, 160)
        }
    }

    private fun createOnychomycosisNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isFungal = (x in 20..60) && (y in 14..28)
            if (isFungal) Color.rgb(195, 155, 55) else Color.rgb(220, 150, 160)
        }
    }

    private fun createMelanonychiaNailBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return createSyntheticBitmap(width, height) { x, y ->
            val isStripe = (x in 48..52) && (y in 12..87)
            if (isStripe) Color.rgb(40, 25, 20) else Color.rgb(220, 150, 160)
        }
    }

    /**
     * Test Beau's Lines clinical mapping in NailClassifier:
     * - Severe Zinc (아연) and Severe Protein (단백질)
     * - Sufficient Calcium and Vitamin C
     * - Tailored care advice
     */
    @Test
    fun testBeausLinesNutrientMapping() {
        val result = NailClassifier.buildResultForCondition("beaus_lines", "test_beaus.jpg")

        assertEquals("test_beaus.jpg", result.imagePath)
        assertTrue("Symptoms should mention Beau's lines",
            result.symptoms.any { it.contains("Beau's Lines") || it.contains("가로줄") })

        val defNutrients = result.deficientNutrients
        assertTrue("Should have deficient nutrients", defNutrients.isNotEmpty())

        val zinc = defNutrients.find { it.name.contains("아연") }
        assertNotNull("Zinc must be in deficient nutrients", zinc)
        assertEquals("Severe", zinc?.severity)
        assertTrue("Zinc recommended foods should include 굴/소고기/호박씨",
            zinc?.recommendedFoods?.any { it.contains("굴") || it.contains("소고기") } == true)

        val protein = defNutrients.find { it.name.contains("단백질") }
        assertNotNull("Protein must be in deficient nutrients", protein)
        assertEquals("Severe", protein?.severity)

        val sufNutrients = result.sufficientNutrients
        assertTrue("Should have sufficient nutrients", sufNutrients.isNotEmpty())
        assertTrue("Sufficient nutrients should include Calcium or Vitamin C",
            sufNutrients.any { it.name.contains("칼슘") || it.name.contains("비타민 C") })

        assertTrue("Advice should mention stress or recovery",
            result.overallAdvice.contains("스트레스") || result.overallAdvice.contains("아연") || result.overallAdvice.contains("단백질"))
    }

    /**
     * Test Lunula Abnormality clinical mapping in NailClassifier:
     * - Severe Iron (철분) and Moderate Vitamin B12 & Folic Acid
     * - Sufficient Biotin
     * - Advice regarding anemia and circulation
     */
    @Test
    fun testLunulaAbnormalityNutrientMapping() {
        val result = NailClassifier.buildResultForCondition("lunula_abnormality", "test_lunula.jpg")

        assertTrue("Symptoms should mention Lunula",
            result.symptoms.any { it.contains("반월") || it.contains("Lunula") })

        val defNutrients = result.deficientNutrients
        val iron = defNutrients.find { it.name.contains("철분") }
        assertNotNull("Iron must be in deficient nutrients", iron)
        assertEquals("Severe", iron?.severity)
        assertTrue("Iron foods should include red meat or spinach",
            iron?.recommendedFoods?.any { it.contains("소고기") || it.contains("시금치") } == true)

        val b12 = defNutrients.find { it.name.contains("B12") || it.name.contains("엽산") }
        assertNotNull("Vitamin B12 or Folic acid should be present", b12)
        assertEquals("Moderate", b12?.severity)

        assertTrue("Advice should mention circulation or anemia",
            result.overallAdvice.contains("빈혈") || result.overallAdvice.contains("순환"))
    }

    /**
     * Test Onychomycosis clinical mapping in NailClassifier:
     * - Severe Zinc (아연) and Moderate Vitamin A & D
     * - Advice regarding fungal treatment and dermatologist
     */
    @Test
    fun testOnychomycosisNutrientMapping() {
        val result = NailClassifier.buildResultForCondition("onychomycosis", "test_onycho.jpg")

        assertTrue("Symptoms should mention Onychomycosis",
            result.symptoms.any { it.contains("조갑진균증") || it.contains("Onychomycosis") })

        val defNutrients = result.deficientNutrients
        val zinc = defNutrients.find { it.name.contains("아연") }
        assertNotNull("Zinc must be in deficient nutrients", zinc)
        assertEquals("Severe", zinc?.severity)

        val vitAD = defNutrients.find { it.name.contains("비타민 A") || it.name.contains("지용성") }
        assertNotNull("Vitamin A/D should be present", vitAD)
        assertEquals("Moderate", vitAD?.severity)

        assertTrue("Advice should mention antifungal care or dermatologist",
            result.overallAdvice.contains("진균") || result.overallAdvice.contains("피부과"))
    }

    /**
     * Test Melanonychia clinical mapping in NailClassifier:
     * - Severe Vitamin B12 and Moderate Antioxidants
     * - Advice regarding longitudinal streak, Hutchinson sign, dermatologist
     */
    @Test
    fun testMelanonychiaNutrientMapping() {
        val result = NailClassifier.buildResultForCondition("melanonychia", "test_melano.jpg")

        assertTrue("Symptoms should mention Melanonychia",
            result.symptoms.any { it.contains("흑색선조") || it.contains("Melanonychia") })

        val defNutrients = result.deficientNutrients
        val b12 = defNutrients.find { it.name.contains("B12") }
        assertNotNull("Vitamin B12 must be present", b12)
        assertEquals("Severe", b12?.severity)

        val antioxidants = defNutrients.find { it.name.contains("항산화") || it.name.contains("셀레늄") }
        assertNotNull("Antioxidants should be present", antioxidants)
        assertEquals("Moderate", antioxidants?.severity)

        assertTrue("Advice should mention B12 or dermatologist warning",
            result.overallAdvice.contains("멜라닌") || result.overallAdvice.contains("피부과") || result.overallAdvice.contains("전문의"))
    }

    /**
     * Test Healthy Condition in NailClassifier:
     * - No deficient nutrients
     * - Four sufficient nutrients (Keratin, Zinc, Iron, Biotin)
     */
    @Test
    fun testHealthyNutrientMapping() {
        val result = NailClassifier.buildResultForCondition("healthy", "test_healthy.jpg")

        assertTrue("Deficient nutrients should be empty for healthy", result.deficientNutrients.isEmpty())
        assertTrue("Sufficient nutrients should have 4 items", result.sufficientNutrients.size >= 4)
        assertTrue("Advice should congratulate healthy state", result.overallAdvice.contains("건강"))
    }

    /**
     * Test SymptomRegion bounding box coordinates contract:
     * - All coordinates must be strictly normalized within [0.0, 1.0]
     * - xMin < xMax and yMin < yMax
     * - Non-blank label
     */
    @Test
    fun testSymptomRegionNormalization() {
        val testBitmaps = listOf(
            Pair("beaus_lines", createBeausLinesNailBitmap()),
            Pair("lunula_abnormality", createLunulaAbnormalNailBitmap()),
            Pair("onychomycosis", createOnychomycosisNailBitmap()),
            Pair("melanonychia", createMelanonychiaNailBitmap())
        )

        for ((conditionName, bitmap) in testBitmaps) {
            val result = NailClassifier.classify(bitmap, "test_${conditionName}.jpg", null)
            val regions = result.symptomRegions

            assertTrue("Symptom regions should not be empty for $conditionName", regions.isNotEmpty())

            for (region in regions) {
                assertTrue("Region label should not be blank", region.label.isNotBlank())

                assertTrue("xMin must be >= 0.0f, was ${region.xMin} for ${region.label}", region.xMin >= 0.0f)
                assertTrue("xMax must be <= 1.0f, was ${region.xMax} for ${region.label}", region.xMax <= 1.0f)
                assertTrue("yMin must be >= 0.0f, was ${region.yMin} for ${region.label}", region.yMin >= 0.0f)
                assertTrue("yMax must be <= 1.0f, was ${region.yMax} for ${region.label}", region.yMax <= 1.0f)

                assertTrue("xMin must be strictly less than xMax: ${region.xMin} < ${region.xMax} for ${region.label}",
                    region.xMin < region.xMax)
                assertTrue("yMin must be strictly less than yMax: ${region.yMin} < ${region.yMax} for ${region.label}",
                    region.yMin < region.yMax)
            }
        }
    }
}
