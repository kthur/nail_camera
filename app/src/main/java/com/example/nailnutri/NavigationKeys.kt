package com.example.nailnutri

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object CameraScan : NavKey
@Serializable data class AnalysisResult(val resultId: String, val isNewScan: Boolean = false) : NavKey
@Serializable data object History : NavKey
@Serializable data object Settings : NavKey
