package com.example.nailnutri.analysis

/**
 * D1: On-device Personalization Calibrator.
 *
 * Adjusts TFLite classification confidence thresholds dynamically based on
 * user accuracy feedback stored locally in EncryptedSharedPreferences.
 */
object PersonalizationCalibrator {

    private const val MIN_OFFSET = -0.05f
    private const val MAX_OFFSET = 0.10f
    private const val STEP_INACCURATE = 0.02f
    private const val STEP_ACCURATE = -0.005f

    @Volatile
    var thresholdOffset: Float = 0.0f
        private set

    @Volatile
    var totalFeedbackCount: Int = 0
        private set

    @Volatile
    var positiveFeedbackCount: Int = 0
        private set

    /**
     * Synchronizes in-memory calibration state from encrypted storage.
     */
    fun loadState(totalCount: Int, positiveCount: Int, savedOffset: Float) {
        totalFeedbackCount = totalCount.coerceAtLeast(0)
        positiveFeedbackCount = positiveCount.coerceIn(0, totalFeedbackCount)
        thresholdOffset = savedOffset.coerceIn(MIN_OFFSET, MAX_OFFSET)
    }

    /**
     * Updates calibration offset given a single user feedback event and returns the new offset.
     */
    fun recordFeedback(isAccurate: Boolean): Float {
        totalFeedbackCount++
        if (isAccurate) {
            positiveFeedbackCount++
            thresholdOffset = (thresholdOffset + STEP_ACCURATE).coerceIn(MIN_OFFSET, MAX_OFFSET)
        } else {
            thresholdOffset = (thresholdOffset + STEP_INACCURATE).coerceIn(MIN_OFFSET, MAX_OFFSET)
        }
        return thresholdOffset
    }

    /**
     * Resets calibration state (primarily for tests or user settings reset).
     */
    fun reset() {
        totalFeedbackCount = 0
        positiveFeedbackCount = 0
        thresholdOffset = 0.0f
    }
}
