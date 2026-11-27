package com.example.nailnutri.analysis

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PersonalizationCalibratorTest {

    @Before
    fun setUp() {
        PersonalizationCalibrator.reset()
    }

    @After
    fun tearDown() {
        PersonalizationCalibrator.reset()
    }

    @Test
    fun testInitialState_isZero() {
        assertEquals(0.0f, PersonalizationCalibrator.thresholdOffset, 1e-6f)
        assertEquals(0, PersonalizationCalibrator.totalFeedbackCount)
        assertEquals(0, PersonalizationCalibrator.positiveFeedbackCount)
    }

    @Test
    fun testInaccurateFeedback_raisesThresholdOffsetWithinBounds() {
        repeat(10) {
            PersonalizationCalibrator.recordFeedback(isAccurate = false)
        }
        assertEquals(10, PersonalizationCalibrator.totalFeedbackCount)
        assertEquals(0, PersonalizationCalibrator.positiveFeedbackCount)
        // Clamped at MAX_OFFSET = 0.10f
        assertEquals(0.10f, PersonalizationCalibrator.thresholdOffset, 1e-6f)
    }

    @Test
    fun testAccurateFeedback_lowersThresholdOffsetWithinBounds() {
        repeat(20) {
            PersonalizationCalibrator.recordFeedback(isAccurate = true)
        }
        assertEquals(20, PersonalizationCalibrator.totalFeedbackCount)
        assertEquals(20, PersonalizationCalibrator.positiveFeedbackCount)
        // Clamped at MIN_OFFSET = -0.05f
        assertEquals(-0.05f, PersonalizationCalibrator.thresholdOffset, 1e-6f)
    }

    @Test
    fun testLoadState_restoresAndClampsValues() {
        PersonalizationCalibrator.loadState(totalCount = 15, positiveCount = 12, savedOffset = 0.25f)
        assertEquals(15, PersonalizationCalibrator.totalFeedbackCount)
        assertEquals(12, PersonalizationCalibrator.positiveFeedbackCount)
        assertEquals(0.10f, PersonalizationCalibrator.thresholdOffset, 1e-6f)
    }
}
