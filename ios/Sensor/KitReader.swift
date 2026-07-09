import Foundation
import UIKit

class KitReader {
    static func readKit(image: UIImage, imagePath: String) -> NailAnalysisResult {
        // Mock LFA Strip Dipping ratios
        let vitDLevel = 22.4 // Vitamin D level in ng/mL (<30 indicates deficiency)
        let isDeficient = vitDLevel < 30.0
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        
        if isDeficient {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["비타민 D 결핍 수치 감지 (\(String(format: "%.1f", vitDLevel)) ng/mL)"],
                deficientNutrients: [
                    NutrientDetail(name: "비타민 D", severity: "Moderate", symptomExplanation: "시약 스트립의 검사선 발색 비율 판독 결과 체내 활성 비타민 D 농도가 \(String(format: "%.1f", vitDLevel)) ng/mL로 임계치인 30.0 ng/mL를 크게 밑도는 결핍 상태입니다.", recommendedFoods: ["등푸른 생선", "표고버섯", "달걀노른자", "연어"]),
                    NutrientDetail(name: "칼슘", severity: "Moderate", symptomExplanation: "비타민 D는 장내 칼슘 흡수율을 제어하는 핵심 영양소로 장기 결핍 시 골조직에 영향을 줍니다.", recommendedFoods: ["우유", "치즈", "멸치", "두부"])
                ],
                sufficientNutrients: [],
                overallAdvice: "자가 진단 시약 LFA 리더기 판정 결과 비타민 D가 \(String(format: "%.1f", vitDLevel)) ng/mL로 결핍 수준입니다. 실내 활동 위주의 생활을 피하시고 하루 15분 이상 햇빛을 쬐거나 연어, 달걀노른자 섭취 및 영양 보충제 복용을 추천합니다."
            )
        } else {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["비타민 D 농도 양호 (\(String(format: "%.1f", vitDLevel)) ng/mL)"],
                deficientNutrients: [],
                sufficientNutrients: [
                    SufficientNutrientDetail(name: "비타민 D", symptomExplanation: "화학 시약선 정량 대조 판정 수치가 \(String(format: "%.1f", vitDLevel)) ng/mL로 정상 범주에 부합합니다.", role: "면역력 유지 및 골세포 형성 촉진")
                ],
                overallAdvice: "LFA 스트립 스캔 판독 결과 비타민 D 지수가 \(String(format: "%.1f", vitDLevel)) ng/mL로 지극히 정상이며, 면역 기능 유지 및 칼슘 흡수 사이클이 훌륭하게 유지되고 있습니다."
            )
        }
    }
}
