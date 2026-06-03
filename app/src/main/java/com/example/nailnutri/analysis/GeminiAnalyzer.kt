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
        val resizedBitmap = resizeBitmap(bitmap, 1600)

        // 2. Initialize Gemini Model with JSON response config
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )

        val prompt = """
            You are an expert clinical nutritional analyst specialized in dermatological and fingernail biometrics. 
            Perform a precise step-by-step diagnostic analysis on this fingernail image to identify nutritional deficiencies and sufficiencies.
            
            Follow this Chain-of-Thought (CoT) diagnostic procedure:
            1. Stage 1 - Texture and Surface Examination: Scan the nail plate closely from proximal to distal. Check for tiny elevated ridges, deep vertical lines, or horizontal indentations (Beau's lines). If fine vertical ridges are present, note that this is commonly linked to Vitamin B12, Magnesium deficiency, or aging.
            2. Stage 2 - Discoloration and Spotting Detection: Scan for localized white specks, opaque dots (Leukonychia), or transverse white bands. Opaque white spots are a strong, classic indicator of Zinc or Calcium deficiency.
            3. Stage 3 - Shape and Curvature Check: Assess the lateral profile. Check if the nail is flat, overly curved downward (brittleness/splitting), or concave/spoon-shaped with raised edges (Koilonychia, a severe indicator of Iron deficiency anemia).
            4. Stage 4 - Nail Bed Vascular Colorization: Evaluate the pinkish tint of the nail bed underneath. If it is pale or white instead of pink, suggest potential Iron deficiency or general anemia.
            5. Stage 5 - Formulation: Synthesize these visual facts into structured nutritional mapping:
               - Deficient Nutrients: Assign severity levels ("Severe" or "Moderate" or "None") based on the prominence of the observed symptom, explain the correlation, and list highly bioavailable food sources in Korean.
               - Sufficient Nutrients: Identify nutrients that seem adequate due to the absence of corresponding symptoms (e.g., "Zinc" is sufficient if no white spots are found), describe their positive role in Korean.
               - Overall Wellness Advice: Synthesize a general nutritional recommendation in clear, professional Korean.
            
            Return the output strictly in the following JSON schema:
            {
              "symptoms": ["List of detected nail symptoms in Korean (e.g. '손톱 흰 반점', '세로줄 홈')"],
              "deficientNutrients": [
                {
                  "name": "Nutrient Name (e.g., 아연, 철분, 비오틴)",
                  "severity": "Severe" or "Moderate" or "None",
                  "symptomExplanation": "Clinical correlation between symptom and nutrient deficiency in Korean",
                  "recommendedFoods": ["Food 1", "Food 2", "Food 3"]
                }
              ],
              "sufficientNutrients": [
                {
                  "name": "Nutrient Name",
                  "symptomExplanation": "Feedback on why it is sufficient and positive signs in Korean",
                  "role": "Biological role of this nutrient in maintaining nail health in Korean"
                }
              ],
              "overallAdvice": "Synthesized dietary recommendations and healthy habits in Korean"
            }
            Do not output any markdown code blocks, markers, or text outside the JSON. Return only the raw JSON string.
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
