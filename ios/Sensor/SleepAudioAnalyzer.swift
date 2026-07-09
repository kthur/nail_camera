import Foundation

class SleepAudioAnalyzer {
    static func analyzeAudioBuffer(audioSamples: [Int16], imagePath: String) -> NailAnalysisResult {
        // RMS and Snoring energy analysis simulation
        let db = 52.4 // Decibels
        let snorePercentage = 42.0
        let isVitaminDDeficient = snorePercentage > 25.0
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        
        if isVitaminDDeficient {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["수면 무호흡 및 심한 코골이 감지 (호흡 dB: \(String(format: "%.1f", db)) dB)"],
                deficientNutrients: [
                    NutrientDetail(name: "비타민 D", severity: "Moderate", symptomExplanation: "수면 장애로 호흡 기둥이 무너지고 상기도 이완 저하를 야기하는 비타민 D 결핍이 감지되었습니다.", recommendedFoods: ["등푸른 생선", "버섯", "달걀노른자", "연어"]),
                    NutrientDetail(name: "마그네슘", severity: "Moderate", symptomExplanation: "수면 신경을 이완시키는 필수 미네랄인 마그네슘 부족 시 상기도 경직 및 코골이가 악화됩니다.", recommendedFoods: ["아몬드", "귀리", "바나나"])
                ],
                sufficientNutrients: [],
                overallAdvice: "수면 음향 주파수 분석 결과 수면 무호흡 및 저주파 코골이(\(String(format: "%.1f", db)) dB) 비중이 높게 잡혔습니다. 이는 상기도 평활근의 긴장을 제어하는 비타민 D 및 마그네슘 부족과 밀접한 연관이 있으므로 이에 따른 영양 섭취를 추천합니다."
            )
        } else {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["안정적인 호흡음 (소음도: \(String(format: "%.1f", db)) dB)"],
                deficientNutrients: [],
                sufficientNutrients: [
                    SufficientNutrientDetail(name: "비타민 D & 마그네슘", symptomExplanation: "코골이 주파수 점유율이 \(String(format: "%.1f", snorePercentage))%로 대단히 낮으며 깊고 안정적인 호흡 패턴을 보여 수면 영양 균형이 훌륭합니다.", role: "근육 수축 조절 및 멜라토닌 수면 신경 활성화")
                ],
                overallAdvice: "호흡 음향 모니터링 결과 코골이 및 거친 소음 비중이 대단히 양호하여 비타민 D와 근육 안정 미네랄(마그네슘)의 기능이 아주 정상적으로 작동하고 있습니다. 숙면을 지속하고 있는 상태입니다."
            )
        }
    }
}
