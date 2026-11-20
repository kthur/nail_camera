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
            symptoms = runCatching { json.decodeFromString<List<String>>(entity.symptomsJson) }.getOrDefault(emptyList()),
            deficientNutrients = runCatching { json.decodeFromString<List<NutrientDetail>>(entity.deficientNutrientsJson) }.getOrDefault(emptyList()),
            sufficientNutrients = runCatching { json.decodeFromString<List<SufficientNutrientDetail>>(entity.sufficientNutrientsJson) }.getOrDefault(emptyList()),
            overallAdvice = entity.overallAdvice,
            symptomRegions = runCatching { json.decodeFromString<List<SymptomRegion>>(entity.symptomRegionsJson) }.getOrDefault(emptyList())
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
            resultIds = runCatching { json.decodeFromString<List<String>>(entity.resultIdsJson) }.getOrDefault(emptyList()),
            topDeficiencies = runCatching { json.decodeFromString<List<String>>(entity.topDeficienciesJson) }.getOrDefault(emptyList()),
            overallScore = entity.overallScore
        )
    }
}
