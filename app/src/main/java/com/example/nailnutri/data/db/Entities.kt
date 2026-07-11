package com.example.nailnutri.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nail_analysis_results")
data class NailAnalysisResultEntity(
    @PrimaryKey val id: String,
    val date: String,
    val imagePath: String,
    val symptomsJson: String,             // Serialized List<String>
    val deficientNutrientsJson: String,   // Serialized List<NutrientDetail>
    val sufficientNutrientsJson: String,  // Serialized List<SufficientNutrientDetail>
    val overallAdvice: String,
    val symptomRegionsJson: String        // Serialized List<SymptomRegion>
)

@Entity(tableName = "session_reports")
data class SessionReportEntity(
    @PrimaryKey val id: String,
    val createdAt: String,
    val label: String,
    val resultIdsJson: String,            // Serialized List<String>
    val topDeficienciesJson: String,      // Serialized List<String>
    val overallScore: Int
)
