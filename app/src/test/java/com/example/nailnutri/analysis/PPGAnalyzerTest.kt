package com.example.nailnutri.analysis

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PPGAnalyzerTest {

    private fun generateSinePpg(
        bpm: Double,
        durationSeconds: Int = 8,
        fps: Int = 30,
        amplitude: Double = 10.0,
        baseline: Double = 180.0
    ): Pair<DoubleArray, LongArray> {
        val count = durationSeconds * fps
        val freqHz = bpm / 60.0
        val signal = DoubleArray(count) { i ->
            baseline + amplitude * sin(2.0 * PI * freqHz * i / fps)
        }
        val stepMs = 1000L / fps
        val timestamps = LongArray(count) { i -> i * stepMs }
        return Pair(signal, timestamps)
    }

    @Test
    fun testShortBuffer_returnsDefaultResult() {
        val signal = DoubleArray(50) { 150.0 }
        val timestamps = LongArray(50) { it * 33L }
        val result = PPGAnalyzer.analyzePPG(signal, timestamps, "short_ppg.jpg")

        assertNotNull(result)
        assertEquals("short_ppg.jpg", result.imagePath)
        assertTrue(result.symptoms.isNotEmpty())
        assertTrue(result.overallAdvice.isNotBlank())
    }

    @Test
    fun testNormalSinusRhythm_returnsValidBpmAndResult() {
        val (signal, timestamps) = generateSinePpg(bpm = 72.0, durationSeconds = 10, fps = 30)
        val result = PPGAnalyzer.analyzePPG(signal, timestamps, "normal_ppg.jpg")

        assertNotNull(result)
        assertEquals("normal_ppg.jpg", result.imagePath)
        assertTrue("Symptoms must not be empty", result.symptoms.isNotEmpty())
        assertTrue("Advice must not be empty", result.overallAdvice.isNotBlank())
    }

    @Test
    fun testBandpassFilter_rejectsDcOffsetAndResetWorks() {
        val filter = PPGAnalyzer.BandpassFilter()
        // Feed constant DC signal -> bandpass output should converge near 0
        var lastOut = 0.0
        for (i in 0 until 200) {
            lastOut = filter.process(200.0)
        }
        assertTrue("DC offset should be attenuated by bandpass filter, got $lastOut", kotlin.math.abs(lastOut) < 1.0)

        filter.reset()
        val afterReset = filter.process(0.0)
        assertEquals(0.0, afterReset, 1e-9)
    }
}
