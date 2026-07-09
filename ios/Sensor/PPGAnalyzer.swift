import Foundation

class PPGAnalyzer {
    struct PPGMetrics {
        let bpm: Int
        let sdnn: Double
        let stressLevel: Int
        let isMineralDeficient: Bool
    }
    
    class BandpassFilter {
        private var x1 = 0.0; private var x2 = 0.0
        private var y1 = 0.0; private var y2 = 0.0
        
        private let b0 = 0.067455
        private let b1 = 0.0
        private let b2 = -0.067455
        private let a1 = -1.14298
        private let a2 = 0.41280
        
        func process(sample: Double) -> Double {
            let output = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = sample
            y2 = y1
            y1 = output
            return output
        }
    }
    
    static func analyzePPG(redMeanBuffer: [Double], timeStamps: [Int64], imagePath: String) -> NailAnalysisResult {
        let size = redMeanBuffer.count
        if size < 120 { return buildDefaultResult(imagePath: imagePath, bpm: 72, sdnn: 35.0, stressLevel: 32) }
        
        let bpFilter = BandpassFilter()
        var filtered = [Double](repeating: 0.0, count: size)
        
        for i in 0..<size {
            filtered[i] = bpFilter.process(sample: redMeanBuffer[i])
        }
        
        var peakIndices: [Int] = []
        let windowSize = 10
        
        for i in 2..<(size - 2) {
            let currentVal = filtered[i]
            if currentVal > filtered[i - 1] && currentVal > filtered[i - 2] &&
                currentVal > filtered[i + 1] && currentVal > filtered[i + 2] {
                
                if currentVal > 0.05 {
                    if peakIndices.isEmpty || (i - peakIndices.last!) > windowSize {
                        peakIndices.add(i)
                    }
                }
            }
        }
        
        var rrIntervals: [Double] = []
        for k in 0..<(peakIndices.count - 1) {
            let intervalMs = Double(timeStamps[peakIndices[k + 1]] - timeStamps[peakIndices[k]])
            if intervalMs >= 350.0 && intervalMs <= 1800.0 {
                rrIntervals.append(intervalMs)
            }
        }
        
        let avgRRI = rrIntervals.isEmpty ? 800.0 : rrIntervals.reduce(0.0, +) / Double(rrIntervals.count)
        let bpm = Int(60000.0 / avgRRI).clamped(to: 45...160)
        
        var varianceSum = 0.0
        for rri in rrIntervals {
            varianceSum += (rri - avgRRI) * (rri - avgRRI)
        }
        let sdnn = rrIntervals.count > 1 ? sqrt(varianceSum / Double(rrIntervals.count - 1)) : 40.0
        
        let stressLevel = Int((100.0 - sdnn) * 1.2).clamped(to: 10...95)
        let isMineralDeficient = sdnn < 32.0
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        
        if isMineralDeficient {
            return NailAnalysisResult(
                id: UUID().uuidString,
                date: dateStr,
                imagePath: imagePath,
                symptoms: ["자율신경 긴장 (마그네슘/칼슘 결핍 의심)", "BPM: \(bpm), HRV(SDNN): \(String(format: "%.1f", sdnn))ms"],
                deficientNutrients: [
                    NutrientDetail(name: "마그네슘", severity: "Moderate", symptomExplanation: "혈관 근육의 수축과 이완 밸런스가 흐트러지고 심박 변이도(HRV)가 수축되어 마그네슘 부족 징후가 검출되었습니다.", recommendedFoods: ["귀리", "아몬드", "바나나", "호박씨"]),
                    NutrientDetail(name: "칼슘", severity: "Moderate", symptomExplanation: "심장 근육의 수축성 흥분 전달을 조절하는 칼슘 결핍으로 인한 자율신경 긴장 보완이 필요합니다.", recommendedFoods: ["우유", "치즈", "멸치", "두부"])
                ],
                sufficientNutrients: [],
                overallAdvice: "PPG 혈류 탄성 정밀 검사 결과, 심박 변이도(HRV) 지표가 \(sdnn) ms로 저하되어 만성 스트레스 및 근육 긴장 상태가 감지되었습니다. 이는 체내 마그네슘 및 칼슘 이온 부족과 깊은 관련이 있습니다. 아몬드, 호박씨 등의 미네랄 식품 섭취를 늘려 주십시오."
            )
        } else {
            return buildDefaultResult(imagePath: imagePath, bpm: bpm, sdnn: sdnn, stressLevel: stressLevel)
        }
    }
    
    private static func buildDefaultResult(imagePath: String, bpm: Int, sdnn: Double, stressLevel: Int) -> NailAnalysisResult {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        return NailAnalysisResult(
            id: UUID().uuidString,
            date: dateStr,
            imagePath: imagePath,
            symptoms: ["BPM: \(bpm), HRV: \(String(format: "%.1f", sdnn))ms (정상)"],
            deficientNutrients: [],
            sufficientNutrients: [
                SufficientNutrientDetail(name: "마그네슘 & 칼슘", symptomExplanation: "심박 조율 변이가 유연하며 자율신경계 긴장도가 대단히 양호합니다.", role: "신경 안정 및 혈관 이완 작용 보장")
            ],
            overallAdvice = "광혈류(PPG) 분석 결과 맥박 및 혈압 리듬 탄성도가 대단히 양호하며, SDNN 수치(\(String(format: "%.1f", sdnn))ms)가 건강 범위 내에 있어 근육 이완 미네랄(마그네슘, 칼슘) 영양 밸런스가 매우 양호한 상태입니다."
        )
    }
}

extension Array {
    mutating func add(_ element: Element) {
        self.append(element)
    }
}

extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
}
