import Foundation
import CoreGraphics

struct SymptomRegion: Identifiable, Codable {
    var id: String
    let xMin: CGFloat
    let yMin: CGFloat
    let xMax: CGFloat
    let yMax: CGFloat
}

struct NutrientDetail: Identifiable, Codable {
    var id: String { name }
    let name: String
    let status: String
    let severity: String
    let description: String
    let foods: [String]
}

struct SufficientNutrientDetail: Identifiable, Codable {
    var id: String { name }
    let name: String
    let description: String
}

struct NailFeatures {
    let averageRedness: Double
    let averageSaturation: Double
    let averageBrightness: Double
    let whiteSpotRatio: Double
    let darkEdgeRatio: Double
    let brightnessStdDev: Double
    let rednessUniformity: Double
    
    let isPale: Bool
    let hasWhiteSpots: Bool
    let isDarkEdges: Bool
    let isUnevenTexture: Bool
    let isLowRedness: Bool
}

struct NailAnalysisResult: Identifiable, Codable {
    let id: String
    let date: String
    let imagePath: String
    let symptoms: [String]
    let deficientNutrients: [NutrientDetail]
    let sufficientNutrients: [SufficientNutrientDetail]
    let overallAdvice: String
    let symptomRegions: [SymptomRegion]
}
