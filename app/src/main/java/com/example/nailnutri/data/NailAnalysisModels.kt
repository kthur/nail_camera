package com.example.nailnutri.data

import kotlinx.serialization.Serializable

@Serializable
data class NutrientDetail(
    val name: String,
    val severity: String, // "Severe", "Moderate", "None"
    val symptomExplanation: String,
    val recommendedFoods: List<String>
)

@Serializable
data class SufficientNutrientDetail(
    val name: String,
    val symptomExplanation: String,
    val role: String
)

@Serializable
data class SymptomRegion(
    val label: String,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
)

@Serializable
data class NailAnalysisResult(
    val id: String,
    val date: String,
    val imagePath: String, // local file uri or demo identifier
    val symptoms: List<String>,
    val deficientNutrients: List<NutrientDetail>,
    val sufficientNutrients: List<SufficientNutrientDetail>,
    val overallAdvice: String,
    val symptomRegions: List<SymptomRegion> = emptyList()
)
