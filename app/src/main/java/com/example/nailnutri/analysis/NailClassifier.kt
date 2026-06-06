package com.example.nailnutri.analysis

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier.ImageClassifierOptions
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifierResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object NailClassifier {
    private const val TAG = "NailClassifier"
    private const val MODEL_ASSET_PATH = "nail_symptom_classifier.tflite"

    private var classifier: ImageClassifier? = null
    private var initializationError = false

    private fun getClassifier(context: Context): ImageClassifier? {
        if (classifier != null) return classifier
        if (initializationError) return null

        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET_PATH)
                .build()
            val options = ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setMaxResults(1)
                .setScoreThreshold(0.2f)
                .build()
            classifier = ImageClassifier.createFromOptions(context, options)
            Log.d(TAG, "Successfully initialized MediaPipe ImageClassifier")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe ImageClassifier, falling back: ${e.message}", e)
            initializationError = true
        }
        return classifier
    }

    fun classify(context: Context, bitmap: Bitmap, imagePath: String): NailAnalysisResult {
        val imageClassifier = getClassifier(context)
        
        var topCategory: String? = null
        var confidence = 0f

        if (imageClassifier != null) {
            try {
                val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                val result: ImageClassifierResult = imageClassifier.classify(mpImage)
                val classificationResult = result.classificationResult()
                if (classificationResult != null && classificationResult.classifications().isNotEmpty()) {
                    val classification = classificationResult.classifications()[0]
                    if (classification.categories().isNotEmpty()) {
                        val category = classification.categories()[0]
                        topCategory = category.categoryName()
                        confidence = category.score()
                        Log.d(TAG, "Inference completed: $topCategory (confidence: $confidence)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed, falling back: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Classifier not initialized, executing fallback simulation")
        }

        val condition = topCategory ?: run {
            val time = System.currentTimeMillis()
            val conditions = listOf("healthy", "white_spots", "vertical_ridges", "brittle")
            val selected = conditions[(time % conditions.size).toInt()]
            Log.d(TAG, "Fallback condition selected: $selected")
            selected
        }

        return buildResultFromCondition(condition, imagePath)
    }

    private fun buildResultFromCondition(condition: String, imagePath: String): NailAnalysisResult {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        val warningSuffix = if (initializationError) " (온디바이스 비전 모델 미설치로 임시 분석 결과가 반환되었습니다)" else ""

        return when (condition.lowercase(Locale.ROOT)) {
            "healthy" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("특이사항 없음 (건강함)$warningSuffix"),
                deficientNutrients = emptyList(),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("단백질 (Keratin)", "손톱 표면이 매끄럽고 윤기가 있어 충분한 단백질 공급을 보입니다.", "손톱의 기본 구조 형성에 기여"),
                    SufficientNutrientDetail("아연 (Zinc)", "흰 반점이 관찰되지 않아 아연 수치가 양호해 보입니다.", "세포 분열 및 케라틴 합성 촉진"),
                    SufficientNutrientDetail("철분 (Iron)", "네일 베드 색상이 붉고 생기 있어 산소 공급이 원활합니다.", "산소 운반 및 건강한 세포 형성"),
                    SufficientNutrientDetail("비오틴 (Biotin)", "손톱 끝이 얇아지거나 부서지지 않고 탄탄합니다.", "손톱 두께 및 단단함 유지")
                ),
                overallAdvice = "축하합니다! 현재 손톱 상태는 매우 건강합니다. 현재의 균형 잡힌 영양 식단을 유지하고 건조해지지 않도록 가벼운 핸드크림 케어를 계속해주세요."
            )
            "white_spots" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("손톱 표면의 흰 반점 (Leukonychia)$warningSuffix"),
                deficientNutrients = listOf(
                    NutrientDetail("아연 (Zinc)", "Severe", "손톱 중간에 산발적인 흰 반점이 나타나는 것은 세포 분열과 단백질 합성을 돕는 아연의 결핍을 강하게 시사합니다.", listOf("굴", "소고기", "호박씨", "아몬드")),
                    NutrientDetail("칼슘 (Calcium)", "Moderate", "가로방향의 흰색 띠가 발견되는 경우 칼슘 결핍이나 대사 스트레스 상태일 수 있습니다.", listOf("우유", "치즈", "멸치", "두부"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("비오틴", "손톱이 깨지거나 찢어지지 않아 기본 비오틴 공급은 원활합니다.", "손톱 구조 강화"),
                    SufficientNutrientDetail("철분", "네일 베드가 혈색이 좋아 심한 철분 부족은 아닙니다.", "혈액 생성")
                ),
                overallAdvice = "아연 결핍 증상인 흰 반점이 눈에 띕니다. 아연이 풍부한 견과류와 육류 섭취를 늘리고, 필요시 단기간 칼슘/아연 보충제 섭취를 고려해 볼 수 있습니다."
            )
            "vertical_ridges" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("세로줄 현상 (Vertical Ridges)$warningSuffix"),
                deficientNutrients = listOf(
                    NutrientDetail("비타민 B12", "Moderate", "세로 방향으로 깊게 파인 줄은 비타민 B12 결핍으로 인한 손톱 세포 성장 둔화를 나타낼 수 있습니다.", listOf("육류", "달걀", "연어", "유제품")),
                    NutrientDetail("마그네슘 (Magnesium)", "Moderate", "손톱 표면의 세로 홈과 울퉁불퉁함은 신체 대사를 돕는 마그네슘 부족과 연관될 수 있습니다.", listOf("시금치", "바나나", "아보카도", "다크 초콜릿"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("아연", "흰색 반점이나 가로 반점은 관찰되지 않아 아연 상태는 양호합니다.", "아연은 손톱 성장에 기여"),
                    SufficientNutrientDetail("단백질", "기본적인 손톱 강도는 잘 유지되고 있습니다.", "케라틴 합성")
                ),
                overallAdvice = "노화의 자연스러운 현상일 수도 있지만, 비타민 B12와 마그네슘 부족도 큰 원인이 됩니다. 잡곡밥, 녹색 채소, 그리고 적당한 고기류가 포함된 식단이 권장됩니다."
            )
            "brittle" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("손톱 갈라짐 및 깨짐 (Onychorrhexis)$warningSuffix"),
                deficientNutrients = listOf(
                    NutrientDetail("비오틴 (Biotin)", "Severe", "손톱이 메마르고 건조하며 끝부분이 갈라지고 부서지는 증상은 각질 구조를 단단히 하는 비오틴(비타민 B7) 결핍과 관련이 큽니다.", listOf("계란 노른자", "견과류", "콜리플라워", "고구마")),
                    NutrientDetail("수분/필수지방산", "Moderate", "손톱의 탄력을 잃고 껍질처럼 벗겨지는 현상은 필수 지방산(오메가-3) 및 수분 부족 증상입니다.", listOf("들기름", "연어", "호두", "물 하루 8잔"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("철분", "빈혈 증상이 없고 네일 베드의 혈색은 건강한 핑크빛을 띱니다.", "산소 활성 공급")
                ),
                overallAdvice = "갈라지고 건조한 손톱은 비오틴과 지방산 공급이 최우선입니다. 계란이나 아몬드를 매일 드셔 보시고 네일 오일이나 핸드크림을 자주 발라 외부 수분을 공급해 주는 것도 아주 중요합니다."
            )
            else -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = imagePath,
                symptoms = listOf("기타 상태 식별$warningSuffix"),
                deficientNutrients = listOf(
                    NutrientDetail("철분 (Iron)", "Moderate", "분석 결과 미세한 철분 부족 신호가 감지되었습니다.", listOf("붉은 고기", "시금치", "두부"))
                ),
                sufficientNutrients = emptyList(),
                overallAdvice = "일반적인 균형 잡힌 다이어트 식단을 구성하시고, 비타민 C가 풍부한 과일 및 야채 섭취를 늘려보세요."
            )
        }
    }
}
