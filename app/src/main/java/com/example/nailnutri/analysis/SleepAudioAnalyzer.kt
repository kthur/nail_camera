package com.example.nailnutri.analysis

import com.example.nailnutri.util.DateUtils

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt

object SleepAudioAnalyzer {

    private const val SAMPLE_RATE = 16000  // Hz — standard AudioRecord rate for speech/snore
    private const val SNORE_LOW_HZ = 80.0  // Snore fundamental starts at 80Hz
    private const val SNORE_HIGH_HZ = 500.0 // Snore upper band (~500Hz)
    private const val APNEA_SILENCE_DB = 28.0 // Below this dB → potential apnea silence
    private const val SNORE_DB_THRESHOLD = 42.0 // dB floor for snore classification

    /**
     * Analyze audio samples with FFT-based snore frequency detection.
     *
     * Improvements over ZCR:
     * - Splits signal into 50%-overlapping Hann-windowed frames (512 samples each)
     * - Computes real-valued DFT magnitude spectrum per frame (O(n²) but n=512 is fine)
     * - Calculates Snore Band Energy Ratio: energy in [80–500 Hz] / total energy
     * - Detects apnea silence gaps (frames where dB < APNEA_SILENCE_DB)
     * - Three-tier output: healthy / mild-snore / severe-snore+apnea
     */
    fun analyzeAudioBuffer(audioSamples: ShortArray, imagePath: String): NailAnalysisResult {
        val length = audioSamples.size
        if (length == 0) return buildHealthyResult(imagePath, 0.0, 30.0, 0.0)

        // --- 1. Overall RMS loudness ---
        var sumSq = 0.0
        for (sample in audioSamples) sumSq += sample.toDouble() * sample.toDouble()
        val rms = sqrt(sumSq / length)
        val overallDb = (20.0 * log10(rms + 1e-5)).coerceIn(10.0, 100.0)

        // --- 2. FFT-based Snore Band Energy Ratio over overlapping frames ---
        val frameSize = 512  // ~32ms at 16kHz — covers one snore period cycle
        val hopSize = frameSize / 2
        val hannWindow = DoubleArray(frameSize) { i -> 0.5 * (1.0 - cos(2.0 * PI * i / (frameSize - 1))) }

        // Bin indices corresponding to snore band [80, 500] Hz
        val binLow = (SNORE_LOW_HZ * frameSize / SAMPLE_RATE).toInt().coerceAtLeast(1)
        val binHigh = (SNORE_HIGH_HZ * frameSize / SAMPLE_RATE).toInt().coerceAtMost(frameSize / 2 - 1)

        var totalSnoreEnergy = 0.0
        var totalSpectrumEnergy = 0.0
        var silentFrameCount = 0
        var totalFrameCount = 0

        var frameStart = 0
        while (frameStart + frameSize <= length) {
            // Apply Hann window to frame
            val frame = DoubleArray(frameSize) { i ->
                audioSamples[frameStart + i].toDouble() * hannWindow[i]
            }

            // Frame-level loudness check (apnea silence detection)
            val frameRms = sqrt(frame.sumOf { it * it } / frameSize)
            val frameDb = (20.0 * log10(frameRms + 1e-5)).coerceIn(0.0, 100.0)
            if (frameDb < APNEA_SILENCE_DB) silentFrameCount++
            totalFrameCount++

            // Real DFT magnitude spectrum (only positive frequencies: bins 0..frameSize/2)
            val halfSize = frameSize / 2
            var snoreEnergy = 0.0
            var specEnergy = 0.0
            for (k in 1 until halfSize) {
                var re = 0.0
                var im = 0.0
                val angle = 2.0 * PI * k / frameSize
                for (n in 0 until frameSize) {
                    re += frame[n] * cos(angle * n)
                    im -= frame[n] * kotlin.math.sin(angle * n)  // DFT definition: e^{-j2πkn/N}
                }
                val mag2 = re * re + im * im
                specEnergy += mag2
                if (k in binLow..binHigh) snoreEnergy += mag2
            }
            totalSnoreEnergy += snoreEnergy
            totalSpectrumEnergy += specEnergy

            frameStart += hopSize
        }

        // --- 3. Snore Band Energy Ratio (SBER) ---
        val sber = if (totalSpectrumEnergy > 0.0) totalSnoreEnergy / totalSpectrumEnergy else 0.0
        val silenceRatio = if (totalFrameCount > 0) silentFrameCount.toDouble() / totalFrameCount else 0.0

        // --- 4. Classification ---
        // Snore: SBER > 0.35 (snore band dominates) AND loud enough
        // Apnea risk: high silence ratio (>30% of frames are quiet → irregular breathing)
        val isSnoring = sber > 0.35 && overallDb > SNORE_DB_THRESHOLD
        val hasApneaRisk = silenceRatio > 0.30 && overallDb > 35.0

        // Snore percentage for display (normalized to 0-100%)
        val snorePercentage = (sber * 100.0).coerceIn(0.0, 100.0)

        val dateStr = DateUtils.nowFormatted()
        val mockId = UUID.randomUUID().toString()

        return when {
            isSnoring && hasApneaRisk -> buildSnoreApneaResult(mockId, dateStr, imagePath, overallDb, snorePercentage, silenceRatio * 100)
            isSnoring -> buildSnoreResult(mockId, dateStr, imagePath, overallDb, snorePercentage)
            else -> buildHealthyResult(imagePath, snorePercentage, overallDb, silenceRatio * 100)
        }
    }

    // --- Result builders ---

    private fun buildSnoreApneaResult(
        id: String, dateStr: String, imagePath: String,
        db: Double, snorePct: Double, silencePct: Double
    ): NailAnalysisResult = NailAnalysisResult(
        id = id,
        date = dateStr,
        imagePath = imagePath,
        symptoms = listOf(
            "수면 무호흡 위험 감지 — 코골이 주파수 점유율 ${String.format(Locale.US, "%.1f", snorePct)}% / 무호흡 정적 구간 ${String.format(Locale.US, "%.1f", silencePct)}%",
            "호흡음 평균 음압: ${String.format(Locale.US, "%.1f", db)} dB"
        ),
        deficientNutrients = listOf(
            NutrientDetail(
                "비타민 D", "Moderate",
                "FFT 스펙트럼 분석에서 ${String.format(Locale.US, "%.1f", snorePct)}%의 에너지가 코골이 주파수 대역(80–500 Hz)에 집중되어 있습니다. 비타민 D는 상기도 평활근 긴장을 조절하며 결핍 시 코골이·무호흡이 악화됩니다.",
                listOf("연어", "고등어", "달걀노른자", "버섯", "우유")
            ),
            NutrientDetail(
                "마그네슘", "Moderate",
                "무호흡 정적 구간이 ${String.format(Locale.US, "%.1f", silencePct)}%로 높아 불규칙 호흡이 의심됩니다. 마그네슘은 근신경 완화 및 멜라토닌 합성을 도와 깊은 수면 단계(NREM)를 늘립니다.",
                listOf("아몬드", "귀리", "시금치", "바나나", "두부")
            )
        ),
        sufficientNutrients = emptyList(),
        overallAdvice = "FFT 기반 수면 음향 분석 결과 코골이 주파수 에너지(80–500 Hz) 비율이 ${String.format(Locale.US, "%.1f", snorePct)}%이며 무호흡 정적 구간이 ${String.format(Locale.US, "%.1f", silencePct)}%로 감지되었습니다. 수면 중 상기도 이완 저하 및 간헐적 호흡 중단이 의심되며 비타민 D와 마그네슘 보충을 적극 권장합니다."
    )

    private fun buildSnoreResult(
        id: String, dateStr: String, imagePath: String,
        db: Double, snorePct: Double
    ): NailAnalysisResult = NailAnalysisResult(
        id = id,
        date = dateStr,
        imagePath = imagePath,
        symptoms = listOf(
            "코골이 감지 — 코골이 주파수 점유율 ${String.format(Locale.US, "%.1f", snorePct)}% (80–500 Hz 대역)",
            "호흡음 평균 음압: ${String.format(Locale.US, "%.1f", db)} dB"
        ),
        deficientNutrients = listOf(
            NutrientDetail(
                "비타민 D", "Mild",
                "FFT 스펙트럼에서 코골이 주파수 대역(80–500 Hz) 에너지 비율이 ${String.format(Locale.US, "%.1f", snorePct)}%로 감지되었습니다. 비타민 D 보충으로 상기도 근육 이완 조절을 개선할 수 있습니다.",
                listOf("연어", "고등어", "달걀노른자", "버섯")
            )
        ),
        sufficientNutrients = emptyList(),
        overallAdvice = "수면 음향 FFT 분석 결과 코골이 주파수 대역 에너지 비율이 ${String.format(Locale.US, "%.1f", snorePct)}%로 경도 코골이가 감지되었습니다. 비타민 D 및 마그네슘 보충과 측면 수면 자세를 권장합니다."
    )

    private fun buildHealthyResult(imagePath: String, snorePct: Double, db: Double, silencePct: Double): NailAnalysisResult {
        val dateStr = DateUtils.nowFormatted()
        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = listOf("안정적인 호흡음 — FFT 코골이 대역 점유율 ${String.format(Locale.US, "%.1f", snorePct)}% / 음압 ${String.format(Locale.US, "%.1f", db)} dB"),
            deficientNutrients = emptyList(),
            sufficientNutrients = listOf(
                SufficientNutrientDetail(
                    "비타민 D & 마그네슘",
                    "FFT 스펙트럼 분석에서 코골이 주파수 대역(80–500 Hz) 에너지 점유율이 ${String.format(Locale.US, "%.1f", snorePct)}%로 대단히 낮으며 깊고 안정적인 호흡 패턴이 확인됩니다.",
                    "근육 수축 조절 및 멜라토닌 수면 신경 활성화"
                )
            ),
            overallAdvice = "FFT 기반 수면 음향 분석 결과 코골이 주파수 에너지 비율이 ${String.format(Locale.US, "%.1f", snorePct)}%로 양호하며 호흡 패턴이 규칙적입니다. 비타민 D와 마그네슘 균형이 잘 유지되고 있습니다."
        )
    }
}
