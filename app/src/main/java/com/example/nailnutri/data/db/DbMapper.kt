package com.example.nailnutri.data.db

import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SessionReport
import com.example.nailnutri.data.SufficientNutrientDetail
import com.example.nailnutri.data.SymptomRegion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DbMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toEntity(domain: NailAnalysisResult): NailAnalysisResultEntity {
        return NailAnalysisResultEntity(
            id = domain.id,
            date = domain.date,
            imagePath = domain.imagePath,
            symptomsJson = json.encodeToString(domain.symptoms),
            deficientNutrientsJson = json.encodeToString(domain.deficientNutrients),
            sufficientNutrientsJson = json.encodeToString(domain.sufficientNutrients),
            overallAdvice = domain.overallAdvice,
            symptomRegionsJson = json.encodeToString(domain.symptomRegions)
        )
    }

    fun toDomain(entity: NailAnalysisResultEntity): NailAnalysisResult {
        return NailAnalysisResult(
            id = entity.id,
            date = entity.date,
            imagePath = entity.imagePath,
            symptoms = json.decodeFromString(entity.symptomsJson),
            deficientNutrients = json.decodeFromString(entity.deficientNutrientsJson),
            sufficientNutrients = json.decodeFromString(entity.sufficientNutrientsJson),
            overallAdvice = entity.overallAdvice,
            symptomRegions = json.decodeFromString(entity.symptomRegionsJson)
        )
    }

    fun toEntity(domain: SessionReport): SessionReportEntity {
        return SessionReportEntity(
            id = domain.id,
            createdAt = domain.createdAt,
            label = domain.label,
            resultIdsJson = json.encodeToString(domain.resultIds),
            topDeficienciesJson = json.encodeToString(domain.topDeficiencies),
            overallScore = domain.overallScore
        )
    }

    fun toDomain(entity: SessionReportEntity): SessionReport {
        return SessionReport(
            id = entity.id,
            createdAt = entity.createdAt,
            label = entity.label,
            resultIds = json.decodeFromString(entity.resultIdsJson),
            topDeficiencies = json.decodeFromString(entity.topDeficienciesJson),
            overallScore = entity.overallScore
        )
    }
}
