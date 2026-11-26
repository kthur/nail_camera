package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ConjunctivaAnalyzerTest {

    private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, color)
            }
        }
        return bmp
    }

    @Test
    fun testTooDarkImage_returnsIlluminationWarning() {
        val darkBmp = createSolidBitmap(100, 100, Color.rgb(10, 10, 10))
        val result = ConjunctivaAnalyzer.analyze(darkBmp, "dark_eye.jpg")

        assertTrue(result.symptoms.any { it.contains("조도") })
        assertTrue(result.deficientNutrients.isEmpty())
    }

    @Test
    fun testTooBrightImage_returnsIlluminationWarning() {
        val brightBmp = createSolidBitmap(100, 100, Color.rgb(245, 245, 245))
        val result = ConjunctivaAnalyzer.analyze(brightBmp, "bright_eye.jpg")

        assertTrue(result.symptoms.any { it.contains("조도") })
        assertTrue(result.deficientNutrients.isEmpty())
    }

    @Test
    fun testPaleConjunctiva_detectsAnemiaAndIronDeficiency() {
        // Pale / low-redness gray-pink conjunctiva (RI = (R - B)/(R + G + B) < 0.04)
        val paleBmp = createSolidBitmap(100, 100, Color.rgb(155, 145, 145))
        val result = ConjunctivaAnalyzer.analyze(paleBmp, "pale_eye.jpg")

        assertTrue("Pale conjunctiva should detect anemia or iron deficiency",
            result.deficientNutrients.any { it.name.contains("철분") } ||
                result.symptoms.any { it.contains("창백") || it.contains("빈혈") }
        )
    }

    @Test
    fun testHealthyRedConjunctiva_returnsHealthyResult() {
        // Healthy pink-red lower eyelid conjunctiva (high R relative to B)
        val healthyBmp = createSolidBitmap(100, 100, Color.rgb(200, 105, 100))
        val result = ConjunctivaAnalyzer.analyze(healthyBmp, "healthy_eye.jpg")

        assertEquals("healthy_eye.jpg", result.imagePath)
        assertTrue("Healthy conjunctiva should have no iron deficiency",
            result.deficientNutrients.none { it.name.contains("철분") }
        )
        assertTrue(result.sufficientNutrients.isNotEmpty())
    }
}
