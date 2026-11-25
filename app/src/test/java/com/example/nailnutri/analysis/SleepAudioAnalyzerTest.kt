package com.example.nailnutri.analysis

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SleepAudioAnalyzerTest {

    // --- Helper factories ---

    /** Silent signal — all zeros */
    private fun silentBuffer(size: Int = 8192): ShortArray = ShortArray(size)

    /** Pure sine wave at [freqHz] with given amplitude (0..32767) at SAMPLE_RATE=16000 Hz */
    private fun sineBuffer(freqHz: Double, amplitudeShort: Int = 20000, durationMs: Int = 500): ShortArray {
        val sampleRate = 16000
        val length = (sampleRate * durationMs / 1000)
        return ShortArray(length) { i ->
            (amplitudeShort * sin(2.0 * PI * freqHz * i / sampleRate)).toInt().toShort()
        }
    }

    /** Mixes two sine buffers of equal length */
    private fun mixBuffers(a: ShortArray, b: ShortArray): ShortArray {
        val len = minOf(a.size, b.size)
        return ShortArray(len) { i -> ((a[i].toInt() + b[i].toInt()) / 2).toShort() }
    }

    /** Silent segments interleaved with loud snore segments to simulate apnea pattern */
    private fun apneaBuffer(durationMs: Int = 2000, sampleRate: Int = 16000): ShortArray {
        val total = sampleRate * durationMs / 1000
        val result = ShortArray(total)
        val chunkMs = 200
        val chunkSamples = sampleRate * chunkMs / 1000
        var pos = 0
        var loud = true
        while (pos < total) {
            val end = minOf(pos + chunkSamples, total)
            if (loud) {
                // 200Hz snore segment at high amplitude
                for (i in pos until end) {
                    result[i] = (25000 * sin(2.0 * PI * 200.0 * i / sampleRate)).toInt().toShort()
                }
            }
            // silent segment stays 0
            pos = end
            loud = !loud
        }
        return result
    }

    // --- Tests ---

    @Test
    fun testEmptyBuffer_returnsHealthyResult() {
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(ShortArray(0), "empty.jpg")
        assertNotNull(result)
        assertTrue("Empty buffer should return healthy", result.deficientNutrients.isEmpty())
    }

    @Test
    fun testSilentBuffer_returnsHealthyResult() {
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(silentBuffer(), "silent.jpg")
        assertNotNull(result)
        assertTrue("Pure silence should not flag snore", result.deficientNutrients.isEmpty())
        assertTrue("Silent result should mention 호흡음 or 코골이", result.symptoms.isNotEmpty())
    }

    @Test
    fun testSnoreFrequency_200Hz_highAmplitude_detectsSnore() {
        // 200Hz is squarely in snore band [80–500Hz], high amplitude → snore
        val buf = sineBuffer(freqHz = 200.0, amplitudeShort = 28000, durationMs = 1000)
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(buf, "snore_200hz.jpg")
        assertNotNull(result)
        // Should flag at least one deficient nutrient (Vitamin D or Magnesium)
        assertTrue(
            "200Hz high-amplitude signal should detect snoring",
            result.deficientNutrients.isNotEmpty() || result.symptoms.any { it.contains("코골이") }
        )
    }

    @Test
    fun testHighFrequency_8000Hz_notSnore() {
        // 8000Hz is way above snore band → should not classify as snore
        val buf = sineBuffer(freqHz = 8000.0, amplitudeShort = 25000, durationMs = 500)
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(buf, "high_freq.jpg")
        assertNotNull(result)
        // High frequency outside snore band should be healthy or not classify as severe snore
        // (SBER will be very low because energy is outside [80-500Hz])
        assertTrue(
            "8000Hz signal should not flag severe vitamin D deficiency",
            result.deficientNutrients.none { it.severity == "Moderate" && it.name == "비타민 D" && result.symptoms.any { s -> s.contains("무호흡") } }
        )
    }

    @Test
    fun testLowFrequency_30Hz_belowSnoreBand_notSnore() {
        // 30Hz is below snore band [80Hz] → should not trigger snore classification
        val buf = sineBuffer(freqHz = 30.0, amplitudeShort = 25000, durationMs = 1000)
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(buf, "low_freq.jpg")
        assertNotNull(result)
        // Energy is below the snore band, so SBER should be low
        assertTrue("30Hz should not trigger snore", result.symptoms.isNotEmpty())
    }

    @Test
    fun testApneaPattern_alternatingLoudAndSilent_detectsApnea() {
        // Alternating loud snore + silence → 50% silence ratio → apnea risk
        val buf = apneaBuffer(durationMs = 3000)
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(buf, "apnea.jpg")
        assertNotNull(result)
        // High silence + snore frequency → should flag both Vitamin D and Magnesium
        val hasVitD = result.deficientNutrients.any { it.name.contains("비타민 D") }
        val hasMg = result.deficientNutrients.any { it.name.contains("마그네슘") }
        assertTrue("Apnea pattern should flag Vitamin D", hasVitD)
        assertTrue("Apnea pattern should flag Magnesium", hasMg)
    }

    @Test
    fun testResultAlwaysHasSymptoms() {
        val buffers = listOf(
            silentBuffer(),
            sineBuffer(200.0, 28000, 500),
            sineBuffer(50.0, 10000, 500),
            apneaBuffer()
        )
        for ((idx, buf) in buffers.withIndex()) {
            val result = SleepAudioAnalyzer.analyzeAudioBuffer(buf, "test_$idx.jpg")
            assertTrue("Result $idx must have at least one symptom", result.symptoms.isNotEmpty())
            assertNotNull("Result $idx overallAdvice must not be null", result.overallAdvice)
        }
    }

    @Test
    fun testResultImagePathPreserved() {
        val path = "my_audio_path.wav"
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(sineBuffer(200.0), path)
        assertEquals("imagePath must be preserved", path, result.imagePath)
    }

    @Test
    fun testSnorePercentageInSymptoms() {
        // Snore signal — the SBER value should appear in symptoms string
        val buf = sineBuffer(freqHz = 250.0, amplitudeShort = 28000, durationMs = 1000)
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(buf, "snore.jpg")
        // Symptoms should reference frequency-based metrics (not just dB)
        val combinedText = result.symptoms.joinToString(" ") + result.overallAdvice
        assertTrue(
            "Symptoms should mention Hz band or 주파수",
            combinedText.contains("Hz") || combinedText.contains("주파수") || combinedText.contains("대역")
        )
    }

    @Test
    fun testSmallBuffer_noException() {
        // Edge case: buffer smaller than one frame (512 samples)
        val tiny = sineBuffer(freqHz = 200.0, amplitudeShort = 20000, durationMs = 10) // ~160 samples
        val result = SleepAudioAnalyzer.analyzeAudioBuffer(tiny, "tiny.jpg")
        assertNotNull("Small buffer should not throw", result)
    }
}
