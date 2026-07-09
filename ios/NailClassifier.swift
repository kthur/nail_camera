import UIKit

struct NailClassifier {
    
    static func classify(image: UIImage, imagePath: String) -> NailAnalysisResult {
        // Run vision rule extractor
        let features = NailFeatureExtractor.extract(image: image)
        return buildResultFromFeatures(features: features, imagePath: imagePath, image: image)
    }
    
    private static func localizeSymptoms(label: String, image: UIImage) -> [SymptomRegion] {
        var regions: [SymptomRegion] = []
        guard let cgImage = image.cgImage else { return regions }
        
        let width = image.size.width
        let height = image.size.height
        if width <= 0 || height <= 0 { return regions }
        
        // 1. Calculate Android-equivalent camera crop region (60% width, 1.33 aspect ratio)
        let cropW = Int(width * 0.6)
        let cropH = Int(Double(cropW) * 1.33)
        let cropX = Int((width - CGFloat(cropW)) / 2)
        let cropY = Int((height - CGFloat(cropH)) / 2)
        
        let safeX = max(0, min(cropX, Int(width) - cropW))
        let safeY = max(0, min(cropY, Int(height) - cropH))
        
        let cropW_safe = min(cropW, Int(width) - safeX)
        let cropH_safe = min(cropH, Int(height) - safeY)
        
        // 2. Render safe area to 150x200 buffer
        let scanW = 150
        let scanH = 200
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        var pixelData = [UInt8](repeating: 0, count: scanW * scanH * 4)
        
        guard let croppedImage = cgImage.cropping(to: CGRect(x: safeX, y: safeY, width: cropW_safe, height: cropH_safe)),
              let context = CGContext(
                  data: &pixelData,
                  width: scanW,
                  height: scanH,
                  bitsPerComponent: 8,
                  bytesPerRow: scanW * 4,
                  space: colorSpace,
                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
              ) else {
            return regions
        }
        
        context.draw(croppedImage, in: CGRect(x: 0, y: 0, width: scanW, height: scanH))
        
        var vSum = 0.0
        var allV = [Double](repeating: 0.0, count: scanW * scanH)
        for y in 0..<scanH {
            for x in 0..<scanW {
                let offset = (y * scanW + x) * 4
                let r = Double(pixelData[offset])
                let g = Double(pixelData[offset+1])
                let b = Double(pixelData[offset+2])
                let maxVal = max(r, max(g, b)) / 255.0
                allV[y * scanW + x] = maxVal
                vSum += maxVal
            }
        }
        let avgV = vSum / Double(scanW * scanH)
        let whiteVThreshold = min(max(avgV * 1.35, 0.60), 0.92)
        
        switch label.lowercased() {
        case "white_spots":
            var minX = 150; var maxX = 0; var minY = 200; var maxY = 0
            var foundCount = 0
            for y in 40..<160 {
                for x in 30..<120 {
                    let offset = (y * scanW + x) * 4
                    let r = Double(pixelData[offset])
                    let g = Double(pixelData[offset+1])
                    let b = Double(pixelData[offset+2])
                    
                    let maxColor = max(r, max(g, b))
                    let minColor = min(r, min(g, b))
                    let delta = maxColor - minColor
                    let s = maxColor > 0 ? delta / maxColor : 0.0
                    let v = maxColor / 255.0
                    
                    if s < 0.15 && v > whiteVThreshold && v > avgV * 1.15 {
                        if x < minX { minX = x }
                        if x > maxX { maxX = x }
                        if y < minY { minY = y }
                        if y > maxY { maxY = y }
                        foundCount += 1
                    }
                }
            }
            
            if foundCount >= 3 {
                let relXMin = (CGFloat(safeX) + (CGFloat(minX) / 150.0) * CGFloat(cropW_safe)) / width
                let relXMax = (CGFloat(safeX) + (CGFloat(maxX) / 150.0) * CGFloat(cropW_safe)) / width
                let relYMin = (CGFloat(safeY) + (CGFloat(minY) / 200.0) * CGFloat(cropH_safe)) / height
                let relYMax = (CGFloat(safeY) + (CGFloat(maxY) / 200.0) * CGFloat(cropH_safe)) / height
                
                regions.append(SymptomRegion(
                    id: "white_spots_region_1",
                    xMin: max(0.1, relXMin),
                    yMin: max(0.1, relYMin),
                    xMax: min(0.9, relXMax),
                    yMax: min(0.9, relYMax)
                ))
            } else {
                regions.append(SymptomRegion(id: "white_spots_region_2", xMin: 0.40, yMin: 0.40, xMax: 0.60, yMax: 0.60))
            }
            
        case "vertical_ridges":
            var colGradients = [Double](repeating: 0.0, count: scanW)
            for x in 20..<129 {
                var gradSum = 0.0
                for y in 30..<170 {
                    let vSelf = allV[y * scanW + x]
                    let vRight = allV[y * scanW + (x + 1)]
                    gradSum += abs(vSelf - vRight)
                }
                colGradients[x] = gradSum
            }
            
            let bestCol = (20..<129).max(by: { colGradients[$0] < colGradients[$1] }) ?? 75
            let meanGrad = colGradients.reduce(0, +) / Double(colGradients.count)
            let thresholdGrad = meanGrad * 1.22
            
            var scanMinX = 150
            var scanMaxX = 0
            for x in 20..<129 {
                if colGradients[x] > thresholdGrad {
                    if x < scanMinX { scanMinX = x }
                    if x > scanMaxX { scanMaxX = x }
                }
            }
            if scanMaxX <= scanMinX {
                scanMinX = max(15, bestCol - 6)
                scanMaxX = min(135, bestCol + 6)
            }
            
            let relXMin = (CGFloat(safeX) + (CGFloat(scanMinX) / 150.0) * CGFloat(cropW_safe)) / width
            let relXMax = (CGFloat(safeX) + (CGFloat(scanMaxX) / 150.0) * CGFloat(cropW_safe)) / width
            
            regions.append(SymptomRegion(
                id: "vertical_ridges_region",
                xMin: relXMin,
                yMin: 0.28,
                xMax: relXMax,
                yMax: 0.72
            ))
            
        case "spoon_nails":
            regions.append(SymptomRegion(id: "spoon_nails_region_1", xMin: 0.3, yMin: 0.35, xMax: 0.7, yMax: 0.65))
            
        case "brittle":
            regions.append(SymptomRegion(id: "brittle_region", xMin: 0.25, yMin: 0.15, xMax: 0.75, yMax: 0.40))
            
        case "onychomycosis":
            var minX = 150; var maxX = 0; var minY = 200; var maxY = 0
            var foundCount = 0
            for y in 25..<175 {
                for x in 20..<130 {
                    let offset = (y * scanW + x) * 4
                    let r = Double(pixelData[offset])
                    let g = Double(pixelData[offset+1])
                    let b = Double(pixelData[offset+2])
                    
                    let maxColor = max(r, max(g, b))
                    let minColor = min(r, min(g, b))
                    let delta = maxColor - minColor
                    
                    var h = 0.0
                    if delta > 0 {
                        if maxColor == r {
                            h = (g - b) / delta
                        } else if maxColor == g {
                            h = 2.0 + (b - r) / delta
                        } else {
                            h = 4.0 + (r - g) / delta
                        }
                        h *= 60.0
                        if h < 0 { h += 360.0 }
                    }
                    
                    if delta > 15 && h >= 18.0 && h <= 68.0 && maxColor > 60 {
                        if x < minX { minX = x }
                        if x > maxX { maxX = x }
                        if y < minY { minY = y }
                        if y > maxY { maxY = y }
                        foundCount += 1
                    }
                }
            }
            
            if foundCount >= 5 {
                let relXMin = (CGFloat(safeX) + (CGFloat(minX) / 150.0) * CGFloat(cropW_safe)) / width
                let relXMax = (CGFloat(safeX) + (CGFloat(maxX) / 150.0) * CGFloat(cropW_safe)) / width
                let relYMin = (CGFloat(safeY) + (CGFloat(minY) / 200.0) * CGFloat(cropH_safe)) / height
                let relYMax = (CGFloat(safeY) + (CGFloat(maxY) / 200.0) * CGFloat(cropH_safe)) / height
                
                regions.append(SymptomRegion(
                    id: "onychomycosis_region_1",
                    xMin: max(0.1, relXMin),
                    yMin: max(0.1, relYMin),
                    xMax: min(0.9, relXMax),
                    yMax: min(0.9, relYMax)
                ))
            } else {
                regions.append(SymptomRegion(id: "onychomycosis_region_2", xMin: 0.25, yMin: 0.2, xMax: 0.75, yMax: 0.4))
            }
            
        case "melanonychia":
            var colSums = [Double](repeating: 0.0, count: scanW)
            for x in 20..<130 {
                var sum = 0.0
                for y in 25..<175 {
                    sum += allV[y * scanW + x]
                }
                colSums[x] = sum
            }
            
            let minCol = (20..<130).min(by: { colSums[$0] < colSums[$1] }) ?? 75
            let minX = max(15, minCol - 4)
            let maxX = min(135, minCol + 4)
            
            let relXMin = (CGFloat(safeX) + (CGFloat(minX) / 150.0) * CGFloat(cropW_safe)) / width
            let relXMax = (CGFloat(safeX) + (CGFloat(maxX) / 150.0) * CGFloat(cropW_safe)) / width
            
            regions.append(SymptomRegion(
                id: "melanonychia_region",
                xMin: relXMin,
                yMin: 0.18,
                xMax: relXMax,
                yMax: 0.82
            ))
            
        default:
            break
        }
        
        return regions
    }
    
    private static func buildResultFromFeatures(features: NailFeatures, imagePath: String, image: UIImage) -> NailAnalysisResult {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        let dateStr = formatter.string(from: Date())
        let mockId = UUID().uuidString
        
        var regions: [SymptomRegion] = []
        var activeConditions: [String] = []
        
        if features.hasWhiteSpots {
            activeConditions.append("white_spots")
            regions.append(contentsOf: localizeSymptoms(label: "white_spots", image: image))
        }
        if features.isUnevenTexture {
            activeConditions.append("vertical_ridges")
            regions.append(contentsOf: localizeSymptoms(label: "vertical_ridges", image: image))
        }
        if features.isDarkEdges {
            activeConditions.append("spoon_nails")
            regions.append(contentsOf: localizeSymptoms(label: "spoon_nails", image: image))
        }
        if features.isPale || features.isLowRedness {
            activeConditions.append("spoon_nails")
            regions.append(contentsOf: localizeSymptoms(label: "spoon_nails", image: image))
        }
        
        if activeConditions.isEmpty {
            activeConditions.append("healthy")
        }
        
        // Single condition translation logic
        var symptoms: [String] = []
        var deficient: [NutrientDetail] = []
        var sufficient: [SufficientNutrientDetail] = []
        var advice = ""
        
        for condition in activeConditions {
            switch condition {
            case "white_spots":
                symptoms.append("흰 반점 (백색선조)")
                deficient.append(NutrientDetail(name: "아연", status: "결핍 위험", severity: "보통", description: "손톱의 세포 분열에 기여하는 아연이 결핍되어 단백질 합성 장애로 생성된 흰색 불완전 각화층입니다.", foods: ["굴", "소고기", "달걀노른자", "견과류"]))
                deficient.append(NutrientDetail(name: "칼슘", status: "보조 결핍", severity: "낮음", description: "골밀도 유지와 손톱 지지 구조 강도 강화를 돕습니다.", foods: ["우유", "치즈", "멸치", "두부"]))
                sufficient.append(SufficientNutrientDetail(name: "비타민 D", description: "아연 및 칼슘의 대사 활성을 돕는 인자입니다."))
                advice += "아연이 부족하면 손톱 밑바닥의 세포 성장이 방해받아 미세한 공기 주머니가 형성되면서 하얀 반점으로 드러납니다. 굴이나 견과류, 양질의 고기 단백질을 섭취해 주는 것이 좋습니다.\n\n"
                
            case "vertical_ridges":
                symptoms.append("세로 홈 / 세로줄 결")
                deficient.append(NutrientDetail(name: "비오틴", status: "결핍 위험", severity: "보통", description: "케라틴 단백질의 황 화합물 가교 결합을 형성하는 필수 보효소로, 결핍 시 두께가 얇아지며 결이 울퉁불퉁해집니다.", foods: ["귀리", "바나나", "시금치", "연어"]))
                deficient.append(NutrientDetail(name: "수분", status: "만성 부족", severity: "보통", description: "노화 및 수분 섭취 부족으로 손톱 베드가 건조해져 요철이 선명해지는 원인입니다.", foods: ["물 하루 1.5L", "오이", "수박"]))
                sufficient.append(SufficientNutrientDetail(name: "비타민 E", description: "항산화 보습 작용으로 거친 텍스처 현상을 예방합니다."))
                advice += "손톱의 수분과 단백질 탄력이 줄어들면 세로 홈이 발생합니다. 비오틴(B7) 영양제를 권장하며, 핸드크림이나 네일오일로 표면 보습에 신경 써 주세요.\n\n"
                
            case "spoon_nails":
                symptoms.append("숟가락 손톱 (철결핍성 창백/함몰)")
                deficient.append(NutrientDetail(name: "철분", status: "주의 및 관리 요망", severity: "높음", description: "적혈구 헤모글로빈 생성을 방해하여 손톱 베드 조직에 만성 산소 부족 및 숟가락 형태로 뒤집히는 함몰 현상을 유발합니다.", foods: ["시금치", "붉은 살코기", "조개류", "건포도"]))
                deficient.append(NutrientDetail(name: "엽산 (비타민 B9)", status: "보조 결핍", severity: "보통", description: "철분 대사 및 혈액 순환 조직 활성을 촉진합니다.", foods: ["브로콜리", "아스파라거스", "오렌지"]))
                sufficient.append(SufficientNutrientDetail(name: "비타민 C", description: "철분의 체내 흡수율을 대폭 끌어올려 주는 시너지 영양소입니다."))
                advice += "손톱 중심부가 오목하게 들어가며 주변부가 뜨거나 극도로 창백한 경우, 전형적인 철 결핍성 빈혈(Koilonychia)의 전조 증상일 수 있습니다. 철분 함유 음식과 함께 흡수율을 높여주는 비타민 C를 섭취하십시오.\n\n"
                
            case "brittle":
                symptoms.append("갈라짐 및 깨짐 (연조증)")
                deficient.append(NutrientDetail(name: "비오틴", status: "결핍 위험", severity: "보통", description: "케라틴 단백질 생성을 방해하여 두께가 얇아지며 손톱 끝부분이 쉽게 찢어지거나 갈라집니다.", foods: ["귀리", "달걀", "연어", "바나나"]))
                deficient.append(NutrientDetail(name: "콜라겐", status: "보조 결핍", severity: "보통", description: "손톱 판 조직의 유연성과 탄력을 담당하는 고분자 단백질입니다.", foods: ["피시 콜라겐", "돼지 껍데기", "닭발"]))
                sufficient.append(SufficientNutrientDetail(name: "셀레늄", description: "케라틴 생성 및 활성산소로부터 조갑 기질 세포를 보호합니다."))
                advice += "끝부분이 쉽게 쪼개지거나 부서지는 현상은 단백질 보효소인 비오틴 결핍의 영향이 큽니다. 달걀이나 연어 등의 섭취와 손톱 강화제 도포를 권장합니다.\n\n"
                
            case "healthy":
                advice += "손톱이 투명한 연분홍빛을 띠고 요철이 없으며 단단하여 매우 이상적인 영양 밸런스를 유지하고 있습니다. 현재의 영양 습관을 계속 유지해 주세요."
                
            default:
                break
            }
        }
        
        return NailAnalysisResult(
            id: mockId,
            date: dateStr,
            imagePath: imagePath,
            symptoms: symptoms.isEmpty ? ["특이사항 없음 (건강함)"] : Array(Set(symptoms)),
            deficientNutrients: deficient,
            sufficientNutrients: sufficient,
            overallAdvice: advice.trimmingCharacters(in: .whitespacesAndNewlines),
            symptomRegions: regions
        )
    }
}
