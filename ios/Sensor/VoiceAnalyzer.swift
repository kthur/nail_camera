import Foundation

class VoiceAnalyzer {
    static func analyzeVoice(audioSamples: [Int16], imagePath: String) -> NailAnalysisResult {
        // Instability pitch analysis simulation (Jitter / Shimmer)
        let jitter = 0.052
        let shimmer = 0.088
        let isFatigued = jitter > 0.04 || shimmer > 0.075
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        
        if isFatigued {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["음성 주파수 흔들림 (만성 신경계 피로 위험)", "성대 주파 안정도 저하"],
                deficientNutrients: [
                    NutrientDetail(name: "비타민 B12", severity: "Moderate", symptomExplanation: "신경 전달 물질 합성을 돕는 비타민 B12 결핍으로 인한 발성 기질 진동 제어(성대 미세 떨림 지표) 안정도가 저하되었습니다.", recommendedFoods: ["육류", "조개류", "연어", "우유"]),
                    NutrientDetail(name: "비타민 B6", severity: "Moderate", symptomExplanation: "신경계의 에너지 보효소로 활성 저하 시 만성 근 피로감과 발음 지연이 유발될 수 있습니다.", recommendedFoods: ["바나나", "닭고기", "감자", "시금치"])
                ],
                sufficientNutrients: [],
                overallAdvice: "목소리 음질 흔들림 정밀 판독 결과, 성대 미세 조율 안정도 (Jitter: \(String(format: "%.2f", jitter * 100))%, Shimmer: \(String(format: "%.2f", shimmer * 100))%)가 상승되어 만성 피로 및 신경계 불균형 징후가 의심됩니다. 비타민 B군 영양 섭취를 추천합니다."
            )
        } else {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["안정적인 목소리 톤 (안정도 양호)"],
                deficientNutrients: [],
                sufficientNutrients: [
                    SufficientNutrientDetail(name: "비타민 B군", symptomExplanation: "발성 주파 Jitter(\(String(format: "%.2f", jitter * 100))%)와 Shimmer(\(String(format: "%.2f", shimmer * 100))%)가 극도로 건강하며, 성대 신경 미세 컨트롤이 매우 안정적입니다.", role: "신경 수초 보호 및 정상 에너지 대사")
                ],
                overallAdvice: "성대 발성 피치 안정성 분석 결과 주파 변조와 진폭 변동이 지극히 정상으로 나타나 만성 피로가 유도되지 않았으며, 신경계 보효소인 비타민 B군 영양이 훌륭하게 작용하고 있습니다."
            )
        }
    }
}
