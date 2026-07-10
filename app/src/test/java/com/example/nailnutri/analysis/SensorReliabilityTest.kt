package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class SensorReliabilityTest {

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

    @Test
    fun testConjunctivaAnalyzerLumaTooLow() {
        val bitmap = createSyntheticBitmap { _, _ -> Color.rgb(20, 20, 20) }
        val result = ConjunctivaAnalyzer.analyze(bitmap, "dummy_path.jpg")
        
        assertTrue(
            "Result symptoms should contain '조도 부적합'",
            result.symptoms.any { it.contains("조도 부적합") }
        )
        assertTrue(result.deficientNutrients.isEmpty())
        assertTrue(result.sufficientNutrients.isEmpty())
    }

    @Test
    fun testConjunctivaAnalyzerLumaTooHigh() {
        val bitmap = createSyntheticBitmap { _, _ -> Color.rgb(230, 230, 230) }
        val result = ConjunctivaAnalyzer.analyze(bitmap, "dummy_path.jpg")
        
        assertTrue(
            "Result symptoms should contain '조도 부적합'",
            result.symptoms.any { it.contains("조도 부적합") }
        )
        assertTrue(result.deficientNutrients.isEmpty())
        assertTrue(result.sufficientNutrients.isEmpty())
    }

    @Test
    fun testKitReaderUniformColorNoKit() {
        val bitmap = createSyntheticBitmap { _, _ -> Color.rgb(180, 180, 180) }
        val result = KitReader.readKit(bitmap, "dummy_path.jpg")
        
        assertTrue(
            "Result symptoms should contain '키트 미감지'",
            result.symptoms.any { it.contains("키트 미감지") }
        )
        assertTrue(result.deficientNutrients.isEmpty())
        assertTrue(result.sufficientNutrients.isEmpty())
    }
}
