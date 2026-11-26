package com.example.nailnutri.analysis

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class KitReaderTest {

    private fun createKitBitmap(
        width: Int = 200,
        height: Int = 100,
        bandYMin: Int = 35,
        bandYMax: Int = 65,
        cLineBrightness: Int = 40,
        tLineBrightness: Int = 40
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bg = Color.rgb(235, 235, 235)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, bg)
            }
        }
        val cXStart = (width * 0.38).toInt()
        val cXEnd = (width * 0.42).toInt()
        val tXStart = (width * 0.62).toInt()
        val tXEnd = (width * 0.66).toInt()

        for (y in bandYMin..bandYMax) {
            for (x in cXStart..cXEnd) {
                bmp.setPixel(x, y, Color.rgb(cLineBrightness, cLineBrightness, cLineBrightness))
            }
            for (x in tXStart..tXEnd) {
                bmp.setPixel(x, y, Color.rgb(tLineBrightness, tLineBrightness, tLineBrightness))
            }
        }
        return bmp
    }

    @Test
    fun testUniformBlankImage_returnsKitNotDetected() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                bmp.setPixel(x, y, Color.rgb(220, 220, 220))
            }
        }
        val result = KitReader.readKit(bmp, "blank_kit.jpg")
        assertTrue(result.symptoms.any { it.contains("키트 미감지") })
    }

    @Test
    fun testStrongTestLine_returnsHealthyVitaminD() {
        // Strong T-line comparable to C-line -> ratio ~1.0 -> vitD ~45 ng/mL (sufficient)
        val bmp = createKitBitmap(cLineBrightness = 40, tLineBrightness = 40)
        val result = KitReader.readKit(bmp, "healthy_kit.jpg")

        assertTrue("Strong T-line should indicate normal Vitamin D",
            result.deficientNutrients.isEmpty() && result.sufficientNutrients.isNotEmpty()
        )
    }

    @Test
    fun testFaintTestLine_detectsVitaminDDeficiency() {
        // Faint T-line (210) vs strong C-line (30) -> low ratio -> Vitamin D deficient (< 30 ng/mL)
        val bmp = createKitBitmap(cLineBrightness = 30, tLineBrightness = 210)
        val result = KitReader.readKit(bmp, "deficient_kit.jpg")

        assertTrue("Faint T-line should detect Vitamin D deficiency",
            result.deficientNutrients.any { it.name.contains("비타민 D") }
        )
    }

    @Test
    fun testOffCenterKitBand_detectedByMultiRowScan() {
        // Lines located only around y = 35..42 (outside exact center y=50), should be found by B3 multi-row scan
        val bmp = createKitBitmap(
            width = 200,
            height = 100,
            bandYMin = 35,
            bandYMax = 42,
            cLineBrightness = 30,
            tLineBrightness = 210
        )
        val result = KitReader.readKit(bmp, "offcenter_kit.jpg")

        assertFalse("Off-center kit should be detected by multi-row scan",
            result.symptoms.any { it.contains("키트 미감지") }
        )
        assertTrue("Off-center faint T-line should detect Vitamin D deficiency",
            result.deficientNutrients.any { it.name.contains("비타민 D") }
        )
    }
}
