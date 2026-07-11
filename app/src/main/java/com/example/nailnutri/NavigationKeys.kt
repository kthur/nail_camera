package com.example.nailnutri

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Onboarding : NavKey
@Serializable data object CameraScan : NavKey
@Serializable data class AnalysisResult(val resultId: String, val isNewScan: Boolean = false) : NavKey
@Serializable data object History : NavKey
@Serializable data object Settings : NavKey
@Serializable data object SensorDashboard : NavKey
@Serializable data object AnemiaScan : NavKey
@Serializable data object PpgScan : NavKey
@Serializable data object LfaScan : NavKey
@Serializable data object SleepScan : NavKey
@Serializable data object VoiceScan : NavKey
@Serializable data class SessionReportScreen(val sessionId: String) : NavKey
@Serializable data object SessionListNavKey : NavKey

