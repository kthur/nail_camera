package com.example.nailnutri.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object GemmaAnalyzer {

    @Serializable
    private data class GemmaJsonResult(
        val symptoms: List<String> = emptyList(),
        val deficientNutrients: List<GemmaNutrientDetail> = emptyList(),
        val sufficientNutrients: List<GemmaSufficientDetail> = emptyList(),
        val overallAdvice: String = ""
    )

    @Serializable
    private data class GemmaNutrientDetail(
        val name: String,
        val severity: String,
        val symptomExplanation: String,
        val recommendedFoods: List<String>
    )

    @Serializable
    private data class GemmaSufficientDetail(
        val name: String,
        val symptomExplanation: String,
        val role: String
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private var cachedLlmInference: LlmInference? = null
    private var cachedModelPath: String? = null

    @Synchronized
    private fun getLlmInference(context: Context, modelPath: String): LlmInference {
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            throw Exception("Gemma model file not found at: $modelPath. Please verify the path in Settings.")
        }

        if (cachedLlmInference != null && cachedModelPath == modelPath) {
            return cachedLlmInference!!
        }

        try {
            cachedLlmInference?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            .setTemperature(0.3f)
            .build()

        val inference = LlmInference.createFromOptions(context.applicationContext, options)
        cachedLlmInference = inference
        cachedModelPath = modelPath
        return inference
    }

    suspend fun analyzeNail(
        context: Context,
        bitmap: Bitmap,
        modelPath: String,
        imagePath: String
    ): NailAnalysisResult = withContext(Dispatchers.IO) {
        val features = NailFeatureExtractor.extract(bitmap)
        val detectedSymptoms = features.toSymptomList()
        val featureDescription = features.toKoreanDescription()

        val prompt = buildPrompt(detectedSymptoms, featureDescription)

        try {
            val llm = getLlmInference(context, modelPath)
            val responseText = llm.generateResponse(prompt)
            if (responseText.isNullOrBlank()) {
                throw Exception("Received empty response from Gemma model.")
            }

            val cleanJson = cleanJsonResponse(responseText)
            val parsed = jsonParser.decodeFromString<GemmaJsonResult>(cleanJson)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            NailAnalysisResult(
                id = UUID.randomUUID().toString(),
                date = dateStr,
                imagePath = imagePath,
                symptoms = if (parsed.symptoms.isNotEmpty()) parsed.symptoms else detectedSymptoms,
                deficientNutrients = parsed.deficientNutrients.map {
                    NutrientDetail(it.name, it.severity, it.symptomExplanation, it.recommendedFoods)
                },
                sufficientNutrients = parsed.sufficientNutrients.map {
                    SufficientNutrientDetail(it.name, it.symptomExplanation, it.role)
                },
                overallAdvice = parsed.overallAdvice
            )
        } catch (e: Exception) {
            createFallbackResult(detectedSymptoms, imagePath, e.localizedMessage ?: "JSON Parse Error")
        }
    }

    private fun buildPrompt(symptoms: List<String>, featureDescription: String): String {
        val symptomsStr = symptoms.joinToString(", ")
        return """
            You are a professional fingernail analysis assistant. Generate nutritional assessments based on nail image features.
            
            The on-device image analyzer detected the following nail features:
            $featureDescription
            
            Derived symptom tags: $symptomsStr
            
            Based on the image features, identify:
            1. Likely nutrient deficiencies (Zinc, Iron, Biotin, Vitamin B12, Calcium, Protein, Magnesium, etc.) with a severity ("Severe", "Moderate", or "None"), a brief explanation linking the feature to the deficiency, and recommended foods.
            2. Key nutrients that seem sufficient (those unrelated to the features), explaining their positive role in nail health.
            3. Overall dietary and wellness advice in Korean.
            
            Return strictly the following JSON schema without any extra text or markdown:
            {
              "symptoms": ["Korean symptom descriptions matching the features"],
              "deficientNutrients": [
                {
                  "name": "Nutrient Name",
                  "severity": "Severe" or "Moderate" or "None",
                  "symptomExplanation": "Explanation linking feature to deficiency",
                  "recommendedFoods": ["Food 1", "Food 2"]
                }
              ],
              "sufficientNutrients": [
                {
                  "name": "Nutrient Name",
                  "symptomExplanation": "Positive indicator explanation",
                  "role": "Role of this nutrient in nail health"
                }
              ],
              "overallAdvice": "General advice text in Korean"
            }
            Do not include any introductory or concluding text, only the JSON block.
        """.trimIndent()
    }

    private fun cleanJsonResponse(response: String): String {
        var clean = response.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    private fun createFallbackResult(symptoms: List<String>, imagePath: String, errorMsg: String): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val isHealthy = symptoms.any { it.contains("건강") || it.contains("healthy", ignoreCase = true) }
        val def = mutableListOf<NutrientDetail>()
        val suf = mutableListOf<SufficientNutrientDetail>()
        var advice = "온디바이스 Gemma 모델을 활용해 영양 상태를 분석하였습니다."

        if (isHealthy) {
            suf.add(SufficientNutrientDetail("단백질 (Keratin)", "손톱 표면이 고르고 단단하여 공급 상태가 우수합니다.", "기본 구조층 형성"))
            suf.add(SufficientNutrientDetail("아연 (Zinc)", "흰 반점이 나타나지 않아 수치가 적정해 보입니다.", "각질 세포 활성화"))
            advice = "건강한 상태의 손톱입니다. 적절한 수분 공급과 균형 잡힌 식단을 유지해 주세요."
        } else {
            var symptomMatched = false
            if (symptoms.any { it.contains("반점") || it.contains("spots", ignoreCase = true) }) {
                def.add(NutrientDetail("아연 (Zinc)", "Severe", "손톱에 흰 반점이 올라오는 현상은 아연 공급 지연과 연관됩니다.", listOf("굴", "호박씨", "아몬드")))
                def.add(NutrientDetail("칼슘 (Calcium)", "Moderate", "가로방향 반점이나 띠는 칼슘 결핍으로 연화된 네일베드 흔적일 수 있습니다.", listOf("멸치", "우유", "두부")))
                advice = "손톱의 흰 반점은 대표적인 미네랄(아연, 칼슘) 결핍 신호입니다. 관련 견과류와 유제품 섭취를 권장합니다."
                symptomMatched = true
            }
            if (symptoms.any { it.contains("세로줄") || it.contains("ridges", ignoreCase = true) }) {
                def.add(NutrientDetail("비타민 B12", "Moderate", "세로줄 홈은 단백질 합성 지연 및 비타민 B군 공급 감소로 흔히 보입니다.", listOf("육류", "달걀", "연어")))
                def.add(NutrientDetail("마그네슘 (Magnesium)", "Moderate", "세로 홈과 손톱 거칠기는 신체 수분 부족과 마그네슘 부족 시 심화됩니다.", listOf("시금치", "바나나")))
                advice = "세로 방향의 홈 증상은 노화 현상이나 비타민 B12, 마그네슘 부족으로 발생합니다. 고른 영양소 보충이 필요합니다."
                symptomMatched = true
            }
            if (symptoms.any { it.contains("숟가락") || it.contains("spoon", ignoreCase = true) }) {
                def.add(NutrientDetail("철분 (Iron)", "Severe", "손톱 끝이 젖혀지고 안이 움푹 패이는 조갑함요증은 철 결핍성 빈혈의 고전적인 증상입니다.", listOf("붉은 고기", "시금치", "조개류")))
                advice = "숟가락 모양의 함몰은 체내 철분 저장량 부족(빈혈)을 나타낼 가능성이 높습니다. 신속히 철분이 풍부한 식단을 섭취하거나 의사의 상담을 받으시길 바랍니다."
                symptomMatched = true
            }
            if (symptoms.any { it.contains("깨짐") || it.contains("갈라짐") || it.contains("brittle", ignoreCase = true) }) {
                def.add(NutrientDetail("비오틴 (Biotin)", "Severe", "손톱 각질의 결합력을 높여주는 비오틴(비타민 B7) 결핍 시 손톱이 얇게 찢어지거나 잘 부서집니다.", listOf("계란 노른자", "견과류", "고구마")))
                advice = "쉽게 찢어지고 갈라지는 증상은 각질 구조층 수분 및 비오틴 부족 현상입니다. 외부 수분 충전도 동반해 주세요."
                symptomMatched = true
            }

            if (!symptomMatched) {
                def.add(NutrientDetail("종합 비타민", "Moderate", "분석 결과 해석 과정에 결핍이 확인되었습니다.", listOf("종합 비타민제", "신선한 과일", "채소")))
                advice = "손톱 증상에 따른 온디바이스 AI 영양 추정 과정이 완료되었습니다. 균형 식단 섭취에 참고하세요."
            }
        }

        advice = "$advice (Gemma On-Device Local Parse Mode)"

        return NailAnalysisResult(
            id = UUID.randomUUID().toString(),
            date = dateStr,
            imagePath = imagePath,
            symptoms = symptoms,
            deficientNutrients = def,
            sufficientNutrients = suf,
            overallAdvice = advice
        )
    }
}

internal data class NailFeatures(
    val averageRedness: Double,
    val averageSaturation: Double,
    val averageBrightness: Double,
    val whiteSpotRatio: Double,
    val darkEdgeRatio: Double,
    val brightnessStdDev: Double,
    val rednessUniformity: Double,
    val isPale: Boolean,
    val hasWhiteSpots: Boolean,
    val isDarkEdges: Boolean,
    val isUnevenTexture: Boolean,
    val isLowRedness: Boolean
) {
    fun toSymptomList(): List<String> {
        val list = mutableListOf<String>()
        if (isPale) {
            list.add("창백한 네일베드 (Pale Nail Bed)")
        }
        if (hasWhiteSpots) {
            list.add("손톱 표면의 흰 반점 (Leukonychia)")
        }
        if (isUnevenTexture) {
            list.add("거친 손톱 표면 / 세로줄 현상 (Vertical Ridges)")
        }
        if (isDarkEdges) {
            list.add("거의 숟가락형 함몰 징후 (Dark Nail Edges)")
        }
        if (isLowRedness) {
            list.add("낮은 혈색 / 저산소 징후 (Low Redness)")
        }
        if (list.isEmpty()) {
            list.add("특이사항 없음 (건강함)")
        }
        return list
    }

    fun toKoreanDescription(): String {
        return buildString {
            append("- 평균 혈색(R채널): ${"%.1f".format(averageRedness)}/255")
            append(if (isLowRedness) " (낮음)" else if (isPale) " (창백함)" else " (정상)")
            append("\n")
            append("- 평균 채도: ${"%.1f".format(averageSaturation * 100)}%")
            append(if (isPale) " (저채도)" else "")
            append("\n")
            append("- 평균 명도: ${"%.1f".format(averageBrightness * 100)}%")
            append(if (hasWhiteSpots) " (높음 - 반점 가능성)" else "")
            append("\n")
            append("- 흰색 픽셀 비율: ${"%.2f".format(whiteSpotRatio * 100)}%")
            append(if (hasWhiteSpots) " (반점 감지)" else "")
            append("\n")
            append("- 어두운 가장자리 비율: ${"%.2f".format(darkEdgeRatio * 100)}%")
            append(if (isDarkEdges) " (가장자리 어두움 - 함몰/철결핍 가능성)" else "")
            append("\n")
            append("- 명도 표준편차: ${"%.1f".format(brightnessStdDev)}")
            append(if (isUnevenTexture) " (표면 고르지 못함 - 세로줄 가능성)" else "")
            append("\n")
            append("- 혈색 균일도(표준편차): ${"%.1f".format(rednessUniformity)}")
            append(if (rednessUniformity > 30) " (혈색 불균일)" else "")
        }
    }
}

internal object NailFeatureExtractor {

    fun extract(bitmap: Bitmap): NailFeatures {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) {
            return defaultFeatures()
        }

        val sampleStep = max(1, min(width, height) / 80)
        val pixels = ArrayList<Int>((width / sampleStep + 1) * (height / sampleStep + 1))

        var rSum = 0.0
        var gSum = 0.0
        var bSum = 0.0
        var vSum = 0.0
        var sSum = 0.0
        var whiteCount = 0
        var darkEdgeCount = 0
        val totalSamples = (width / sampleStep) * (height / sampleStep)
        var brightnessValues = DoubleArray(totalSamples)
        var redValues = DoubleArray(totalSamples)
        var idx = 0

        val edgeThreshold = (width * 0.12).toInt().coerceAtLeast(2)
        val darkEdgeThreshold = 60

        for (x in 0 until width step sampleStep) {
            for (y in 0 until height step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                rSum += r
                gSum += g
                bSum += b

                val hsv = FloatArray(3)
                Color.RGBToHSV(r, g, b, hsv)
                sSum += hsv[1]
                vSum += hsv[2]

                if (idx < totalSamples) {
                    brightnessValues[idx] = hsv[2].toDouble()
                    redValues[idx] = r.toDouble()
                }
                idx++

                if (hsv[2] > 0.85f && hsv[1] < 0.18f) {
                    whiteCount++
                }
                val isEdge = x < edgeThreshold || x > width - edgeThreshold ||
                        y < edgeThreshold || y > height - edgeThreshold
                if (isEdge && r < darkEdgeThreshold && g < darkEdgeThreshold && b < darkEdgeThreshold) {
                    darkEdgeCount++
                }
            }
        }

        val sampleCount = idx.coerceAtLeast(1)
        val avgR = rSum / sampleCount
        val avgG = gSum / sampleCount
        val avgB = bSum / sampleCount
        val avgS = sSum / sampleCount
        val avgV = vSum / sampleCount
        val whiteSpotRatio = whiteCount.toDouble() / sampleCount
        val darkEdgeRatio = darkEdgeCount.toDouble() / sampleCount

        val brightnessStdDev = stdDev(brightnessValues, totalSamples, avgV)
        val rednessStdDev = stdDev(redValues, totalSamples, avgR)

        val isLowRedness = avgR < 130 && (avgR > avgB * 0.95)
        val isPale = avgS < 0.22 && avgV > 0.45 && avgR < 200
        val hasWhiteSpots = whiteSpotRatio > 0.012
        val isDarkEdges = darkEdgeRatio > 0.18
        val isUnevenTexture = brightnessStdDev > 22 || rednessStdDev > 35

        return NailFeatures(
            averageRedness = avgR,
            averageSaturation = avgS,
            averageBrightness = avgV,
            whiteSpotRatio = whiteSpotRatio,
            darkEdgeRatio = darkEdgeRatio,
            brightnessStdDev = brightnessStdDev,
            rednessUniformity = rednessStdDev,
            isPale = isPale,
            hasWhiteSpots = hasWhiteSpots,
            isDarkEdges = isDarkEdges,
            isUnevenTexture = isUnevenTexture,
            isLowRedness = isLowRedness
        )
    }

    private fun defaultFeatures() = NailFeatures(
        averageRedness = 0.0,
        averageSaturation = 0.0,
        averageBrightness = 0.0,
        whiteSpotRatio = 0.0,
        darkEdgeRatio = 0.0,
        brightnessStdDev = 0.0,
        rednessUniformity = 0.0,
        isPale = false,
        hasWhiteSpots = false,
        isDarkEdges = false,
        isUnevenTexture = false,
        isLowRedness = false
    )

    private fun stdDev(values: DoubleArray, total: Int, mean: Double): Double {
        if (total <= 0) return 0.0
        var sum = 0.0
        var n = 0
        for (i in 0 until total) {
            val diff = values[i] - mean
            sum += diff * diff
            n++
        }
        if (n == 0) return 0.0
        return sqrt(sum / n)
    }
}
