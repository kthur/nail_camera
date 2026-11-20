package com.example.nailnutri.data

import com.example.nailnutri.analysis.GemmaAnalyzer
import com.example.nailnutri.data.db.DbMapper
import com.example.nailnutri.data.db.NailAnalysisResultEntity
import com.example.nailnutri.data.db.SessionReportEntity
import org.junit.Assert.*
import org.junit.Test

class DbMapperResilienceTest {

    @Test
    fun testCorruptJsonInNailAnalysisEntityGracefullyRecovers() {
        val corruptEntity = NailAnalysisResultEntity(
            id = "test-corrupt-id",
            date = "2026-11-20 12:00",
            imagePath = "/path/to/img.jpg",
            symptomsJson = "{ corrupt invalid json !!! }",
            deficientNutrientsJson = "[{\"invalid\": true}",
            sufficientNutrientsJson = "null_or_broken",
            overallAdvice = "Safe advice",
            symptomRegionsJson = "not_a_json_array"
        )

        // Must not throw SerializationException
        val domain = DbMapper.toDomain(corruptEntity)
        assertEquals("test-corrupt-id", domain.id)
        assertEquals("Safe advice", domain.overallAdvice)
        assertTrue("Corrupted symptoms must fall back to empty list", domain.symptoms.isEmpty())
        assertTrue("Corrupted deficientNutrients must fall back to empty list", domain.deficientNutrients.isEmpty())
        assertTrue("Corrupted sufficientNutrients must fall back to empty list", domain.sufficientNutrients.isEmpty())
        assertTrue("Corrupted symptomRegions must fall back to empty list", domain.symptomRegions.isEmpty())
    }

    @Test
    fun testCorruptJsonInSessionReportEntityGracefullyRecovers() {
        val corruptSession = SessionReportEntity(
            id = "session-1",
            createdAt = "2026-11-20",
            label = "Morning",
            resultIdsJson = "MALFORMED_JSON",
            topDeficienciesJson = "MALFORMED_JSON",
            overallScore = 85
        )

        val domain = DbMapper.toDomain(corruptSession)
        assertEquals("session-1", domain.id)
        assertEquals(85, domain.overallScore)
        assertTrue(domain.resultIds.isEmpty())
        assertTrue(domain.topDeficiencies.isEmpty())
    }

    @Test
    fun testGemmaAnalyzerReleaseIsSafeWhenUninitialized() {
        // Calling release when no model is cached must be completely safe and thread-safe
        GemmaAnalyzer.release()
        GemmaAnalyzer.release()
    }
}
