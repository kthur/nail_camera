import UIKit
import CoreGraphics

struct NailFeatureExtractor {
    
    static func extract(image: UIImage) -> NailFeatures {
        guard let cgImage = image.cgImage else {
            return NailFeatures(averageRedness: 120, averageSaturation: 0.3, averageBrightness: 0.5, whiteSpotRatio: 0, darkEdgeRatio: 0, brightnessStdDev: 0, rednessUniformity: 0, isPale: false, hasWhiteSpots: false, isDarkEdges: false, isUnevenTexture: false, isLowRedness: false)
        }
        
        let width = 150
        let height = 200
        
        // Render to 150x200 pixel buffer to analyze features quickly
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        var pixelData = [UInt8](repeating: 0, count: width * height * 4)
        
        guard let context = CGContext(
            data: &pixelData,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
        ) else {
            return NailFeatures(averageRedness: 120, averageSaturation: 0.3, averageBrightness: 0.5, whiteSpotRatio: 0, darkEdgeRatio: 0, brightnessStdDev: 0, rednessUniformity: 0, isPale: false, hasWhiteSpots: false, isDarkEdges: false, isUnevenTexture: false, isLowRedness: false)
        }
        
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        
        var redValues: [Double] = []
        var greenValues: [Double] = []
        var blueValues: [Double] = []
        var brightnessValues: [Double] = []
        
        var skinPixelCount = 0
        var whiteSpotPixelCount = 0
        var darkEdgePixelCount = 0
        
        var localVDiffs: [Double] = []
        var localRDiffs: [Double] = []
        
        // Calculate adaptive threshold for lighting
        var vSum = 0.0
        for y in 0..<height {
            for x in 0..<width {
                let offset = (y * width + x) * 4
                let r = Double(pixelData[offset])
                let g = Double(pixelData[offset + 1])
                let b = Double(pixelData[offset + 2])
                vSum += max(r, max(g, b)) / 255.0
            }
        }
        let avgVOverall = vSum / Double(width * height)
        let whiteVThreshold = min(max(avgVOverall * 1.35, 0.60), 0.92)
        let skinVMin = min(max(avgVOverall * 0.40, 0.20), 0.35)
        
        // Run analysis scan
        for y in 0..<height {
            for x in 0..<width {
                let offset = (y * width + x) * 4
                let r = Double(pixelData[offset])
                let g = Double(pixelData[offset + 1])
                let b = Double(pixelData[offset + 2])
                
                // Color statistics
                redValues.append(r)
                greenValues.append(g)
                blueValues.append(b)
                
                let v = max(r, max(g, b)) / 255.0
                brightnessValues.append(v)
                
                // RGB to HSV conversion
                let maxColor = max(r, max(g, b))
                let minColor = min(r, min(g, b))
                let delta = maxColor - minColor
                
                let s = maxColor > 0 ? delta / maxColor : 0.0
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
                
                // Skin/Nail bed mask logic
                let isSkin = (s >= 0.13 && s <= 0.75) && (v >= skinVMin) && (h <= 50.0 || h >= 320.0)
                if isSkin {
                    skinPixelCount += 1
                }
                
                // White spot scanning
                if s < 0.15 && v > whiteVThreshold && v > avgVOverall * 1.15 {
                    whiteSpotPixelCount += 1
                }
                
                // Edge darkening check (spoon nails / dark edges)
                let isNearBboxEdge = (x < 25 || x > 125 || y < 35 || y > 165)
                if isNearBboxEdge && v < 0.35 && r < 80 {
                    darkEdgePixelCount += 1
                }
                
                // Horizontal gradients for vertical ridges
                if x < width - 1 {
                    let nextOffset = (y * width + (x + 1)) * 4
                    let nextR = Double(pixelData[nextOffset])
                    let nextMax = max(nextR, max(Double(pixelData[nextOffset+1]), Double(pixelData[nextOffset+2]))) / 255.0
                    
                    localVDiffs.append(abs(v - nextMax))
                    localRDiffs.append(abs(r - nextR))
                }
            }
        }
        
        let totalPixels = Double(width * height)
        let avgR = redValues.reduce(0, +) / totalPixels
        let avgB = blueValues.reduce(0, +) / totalPixels
        let avgS = (totalPixels > 0) ? (Double(skinPixelCount) / totalPixels) : 0.3
        
        let avgLocalVGrad = localVDiffs.isEmpty ? 0.0 : (localVDiffs.reduce(0, +) / Double(localVDiffs.count))
        let avgLocalRGrad = localRDiffs.isEmpty ? 0.0 : (localRDiffs.reduce(0, +) / Double(localRDiffs.count))
        
        let rBRatio = avgR / (avgB + 1e-5)
        
        let isDarkEdges = (Double(darkEdgePixelCount) / totalPixels) > 0.08
        let isLowRedness = (avgR < 135.0 || rBRatio < 1.12) && !isDarkEdges && avgVOverall < 0.85
        let isPale = avgS < 0.20 && avgVOverall > avgVOverall * 0.96 && avgR < 185.0 && avgVOverall < 0.85
        let hasWhiteSpots = (Double(whiteSpotPixelCount) / totalPixels) > 0.015
        let isUnevenTexture = (avgLocalVGrad > 0.022 || avgLocalRGrad > 5.5) && !isDarkEdges
        
        return NailFeatures(
            averageRedness: avgR,
            averageSaturation: avgS,
            averageBrightness: avgVOverall,
            whiteSpotRatio: Double(whiteSpotPixelCount) / totalPixels,
            darkEdgeRatio: Double(darkEdgePixelCount) / totalPixels,
            brightnessStdDev: 0.1,
            rednessUniformity: 5.0,
            isPale: isPale,
            hasWhiteSpots: hasWhiteSpots,
            isDarkEdges: isDarkEdges,
            isUnevenTexture: isUnevenTexture,
            isLowRedness: isLowRedness
        )
    }
}
