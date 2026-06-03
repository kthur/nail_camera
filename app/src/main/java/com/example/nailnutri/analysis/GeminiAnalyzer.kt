package com.example.nailnutri.analysis

import android.graphics.Bitmap
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object GeminiAnalyzer {

    @Serializable
    private data class GeminiJsonResult(
        val symptoms: List<String> = emptyList(),
        val deficientNutrients: List<GeminiNutrientDetail> = emptyList(),
        val sufficientNutrients: List<GeminiSufficientDetail> = emptyList(),
        val overallAdvice: String = ""
    )

    @Serializable
    private data class GeminiNutrientDetail(
        val name: String,
        val severity: String,
        val symptomExplanation: String,
        val recommendedFoods: List<String>
    )

    @Serializable
    private data class GeminiSufficientDetail(
        val name: String,
        val symptomExplanation: String,
        val role: String
    )

    private val jsonParser = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    suspend fun analyzeNail(
        bitmap: Bitmap,
        apiKey: String,
        imagePath: String
    ): NailAnalysisResult = withContext(Dispatchers.IO) {
        // 1. Resize bitmap to prevent OOM and reduce upload latency
        val resizedBitmap = resizeBitmap(bitmap, 800)

        // 2. Initialize Gemini Model with JSON response config
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )

        val prompt = """
            You are a professional fingernail analysis assistant. Your task is to analyze this fingernail image for potential nutritional indicators.
            
            1. Identify any visual nail symptoms: e.g., "White Spots", "Vertical Ridges", "Horizontal Ridges (Beau's Lines)", "Spoon Shape (Koilonychia)", "Brittle/Splitting Edges", "Pale Nail Bed", or "Healthy Nail".
            2. Map these symptoms to likely nutrient deficiencies (e.g. Zinc, Iron, Biotin, Vitamin B12, Calcium, Protein, Magnesium, etc.) with a severity ("Severe", "Moderate", "None"), a brief explanation of how it relates to the nail's appearance, and recommended foods.
            3. Highlight which key nutrients appear sufficient (e.g., if there are no white spots, Zinc levels seem sufficient). Provide their role and a positive feedback explanation.
            4. Provide overall dietary or wellness advice.
            
            Return the output strictly in the following JSON schema:
            {
              "symptoms": ["List of detected nail symptoms"],
              "deficientNutrients": [
                {
                  "name": "Nutrient Name",
                  "severity": "Severe" or "Moderate" or "None",
                  "symptomExplanation": "Explanation linking symptom to deficiency",
                  "recommendedFoods": ["Food 1", "Food 2", "Food 3"]
                }
              ],
              "sufficientNutrients": [
                {
                  "name": "Nutrient Name",
                  "symptomExplanation": "Positive indicator explanation",
                  "role": "Role of this nutrient in nail health"
                }
              ],
              "overallAdvice": "General advice text"
            }
        """.trimIndent()

        val inputContent = content {
            image(resizedBitmap)
            text(prompt)
        }

        try {
            val response = model.generateContent(inputContent)
            val jsonText = response.text ?: throw Exception("Empty response from AI model")
            
            // Parse JSON response
            val parsed = jsonParser.decodeFromString<GeminiJsonResult>(jsonText)
            
            // Map to Domain models
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            
            NailAnalysisResult(
                id = UUID.randomUUID().toString(),
                date = dateStr,
                imagePath = imagePath,
                symptoms = parsed.symptoms,
                deficientNutrients = parsed.deficientNutrients.map {
                    NutrientDetail(it.name, it.severity, it.symptomExplanation, it.recommendedFoods)
                },
                sufficientNutrients = parsed.sufficientNutrients.map {
                    SufficientNutrientDetail(it.name, it.symptomExplanation, it.role)
                },
                overallAdvice = parsed.overallAdvice
            )
        } catch (e: Exception) {
            throw Exception("Analysis failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun resizeBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            if (width <= maxDimension) return source
            newWidth = maxDimension
            newHeight = (height * (maxDimension.toFloat() / width.toFloat())).toInt()
        } else {
            if (height <= maxDimension) return source
            newHeight = maxDimension
            newWidth = (width * (maxDimension.toFloat() / height.toFloat())).toInt()
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }
}
