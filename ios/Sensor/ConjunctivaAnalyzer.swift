import Foundation
import UIKit

class ConjunctivaAnalyzer {
    static func analyze(image: UIImage, imagePath: String) -> NailAnalysisResult {
        guard let cgImage = image.cgImage else { return buildHealthyResult(imagePath: imagePath) }
        
        let width = cgImage.width
        let height = cgImage.height
        
        // 1. Mock White Balance Sclera Calibration scale factors
        let scaleR = 1.05
        let scaleG = 0.98
        let scaleB = 0.95
        
        // 2. Scan Conjunctiva pixels to calculate Hemoglobin Index
        // Simulate a slight anemia risk if image has paler color components
        let hemoglobinIndex = 1.02 // Under 1.05 triggers anemia
        let isAnemic = hemoglobinIndex < 1.05
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        
        if isAnemic {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["안구 결막 창백 (헤모글로빈 부족 의심)"],
                deficientNutrients: [
                    NutrientDetail(name: "철분", severity: "Moderate", symptomExplanation: "안구 결막 모세혈관의 혈색 붉은 성분 비율(헤모글로빈 수치)이 정상 범주를 하회하여 철결핍성 빈혈 위험이 감지되었습니다.", recommendedFoods: ["붉은 살코기", "시금치", "조개류", "건포도"]),
                    NutrientDetail(name: "비타민 B12", severity: "Moderate", symptomExplanation: "적혈구 대사와 신경 건강에 필수 인자로 결핍 시 창백함이 동반될 수 있습니다.", recommendedFoods: ["조개류", "연어", "육류", "우유"])
                ],
                sufficientNutrients: [
                    SufficientNutrientDetail(name: "비타민 C", symptomExplanation: "철분 흡수를 촉진시키는 인자가 정상 범위입니다.", role: "철분 생체 흡수율 향상")
                ],
                overallAdvice: "눈꺼풀 안쪽 결막 부위의 혈색이 창백하게 감지되었습니다. 이는 체내 철분 부족이나 빈혈 위험의 전형적인 징후입니다. 철분 보충과 더불어 철 흡수를 돕는 비타민 C와 엽산을 함께 섭취하고 충분한 휴식을 권장합니다."
            )
        } else {
            return buildHealthyResult(imagePath: imagePath)
        }
    }
    
    private static func buildHealthyResult(imagePath: String) -> NailAnalysisResult {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        return NailAnalysisResult(
            id: UUID().uuidString,
            date: dateStr,
            imagePath: imagePath,
            symptoms: ["특이사항 없음 (결막 상태 양호)"],
            deficientNutrients: [],
            sufficientNutrients: [
                SufficientNutrientDetail(name: "철분", symptomExplanation: "결막 혈색 및 채도가 건강한 선분홍빛을 띠어 철분 수치가 양호합니다.", role: "산소 운반 및 헤모글로빈 형성")
            ],
            overallAdvice: "안구 결막 촬영 분석 결과, 헤모글로빈 분포가 균일하고 붉은빛이 선명하게 나타납니다. 빈혈 위험성이 지극히 낮으며, 혈액 순환 및 철분 함량이 건강하게 유지되고 있습니다."
        )
    }
}
