package com.example.nailnutri.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.example.nailnutri.ui.scan.ImageCaptureProcessor
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ImageCaptureProcessorTest {

    @Test
    fun testCropNailRegionDimensions() {
        val original = Bitmap(200, 200)
        val cropped = ImageCaptureProcessor.cropNailRegion(original)
        assertNotNull(cropped)
        assertTrue("Cropped width must be > 0", cropped.width > 0)
        assertTrue("Cropped height must be > 0", cropped.height > 0)
        assertTrue("Cropped width must be <= original width", cropped.width <= original.width)
        assertTrue("Cropped height must be <= original height", cropped.height <= original.height)
    }

    @Test
    fun testAverageBitmapsPixelMath() {
        val b1 = Bitmap(10, 10)
        val b2 = Bitmap(10, 10)
        // b1 is solid red (255, 0, 0), b2 is solid blue (0, 0, 255)
        for (y in 0 until 10) {
            for (x in 0 until 10) {
                b1.setPixel(x, y, Color.rgb(200, 100, 50))
                b2.setPixel(x, y, Color.rgb(100, 50, 150))
            }
        }
        val avg = ImageCaptureProcessor.averageBitmaps(listOf(b1, b2))
        assertEquals(10, avg.width)
        assertEquals(10, avg.height)

        val samplePixel = avg.getPixel(5, 5)
        val expectedR = (200 + 100) / 2
        val expectedG = (100 + 50) / 2
        val expectedB = (50 + 150) / 2
        assertEquals(expectedR, Color.red(samplePixel))
        assertEquals(expectedG, Color.green(samplePixel))
        assertEquals(expectedB, Color.blue(samplePixel))
    }

    @Test
    fun testCleanupBurstFilesKeepsPrimaryOnly() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val file0 = File(tempDir, "test_burst_0_${System.nanoTime()}.jpg").apply { writeText("0") }
        val file1 = File(tempDir, "test_burst_1_${System.nanoTime()}.jpg").apply { writeText("1") }
        val file2 = File(tempDir, "test_burst_2_${System.nanoTime()}.jpg").apply { writeText("2") }

        val burstFiles = listOf(file0, file1, file2)
        try {
            ImageCaptureProcessor.cleanupBurstFiles(burstFiles, keepIndex = 0)
            assertTrue("Primary file (index 0) must be preserved", file0.exists())
            assertFalse("Non-primary file 1 must be deleted", file1.exists())
            assertFalse("Non-primary file 2 must be deleted", file2.exists())
        } finally {
            file0.delete()
            file1.delete()
            file2.delete()
        }
    }

    @Test
    fun testCleanupAllFilesOnFailure() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val file0 = File(tempDir, "test_fail_0_${System.nanoTime()}.jpg").apply { writeText("0") }
        val file1 = File(tempDir, "test_fail_1_${System.nanoTime()}.jpg").apply { writeText("1") }

        val burstFiles = listOf(file0, file1)
        ImageCaptureProcessor.cleanupAllFiles(burstFiles)
        assertFalse("File 0 must be deleted on failure cleanup", file0.exists())
        assertFalse("File 1 must be deleted on failure cleanup", file1.exists())
    }
}
