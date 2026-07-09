import SwiftUI

struct AnalysisResultView: View {
    let image: UIImage
    @State private var result: NailAnalysisResult?
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header navigation
                HStack {
                    Button(action: { presentationMode.wrappedValue.dismiss() }) {
                        Image(systemName: "chevron.left")
                            .font(.title2)
                            .foregroundColor(.white)
                    }
                    Spacer()
                    Text("손톱 영양 상태 분석 결과")
                        .font(.headline)
                        .foregroundColor(.white)
                    Spacer()
                    // Spacer for symmetry
                    Spacer().frame(width: 24)
                }
                .padding(.horizontal)
                .padding(.top, 20)
                
                if let result = result {
                    // Image with Target Masks (Bounding box overlay matching Android custom canvas)
                    ZStack {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(height: 320)
                            .cornerRadius(16)
                            .shadow(radius: 10)
                        
                        // Custom Canvas for drawing target dots and dynamic text labels
                        GeometryReader { geo in
                            // Find the actual rendered size of the image to map relative coordinates
                            let imgWidth = geo.size.width
                            let imgHeight = geo.size.height
                            
                            ForEach(result.symptomRegions) { region in
                                let rx = region.xMin * imgWidth
                                let ry = region.yMin * imgHeight
                                let rw = (region.xMax - region.xMin) * imgWidth
                                let rh = (region.yMax - region.yMin) * imgHeight
                                
                                ZStack {
                                    // Bounding Box
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color.red, style: StrokeStyle(lineWidth: 2, lineCap: .round, dash: [4, 3]))
                                        .frame(width: rw, height: rh)
                                        .position(x: rx + rw/2, y: ry + rh/2)
                                    
                                    // Custom Overlay Speech Balloon
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(getKoreanLabel(for: region.id))
                                            .font(.caption2)
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                    }
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .background(Color.red.opacity(0.85))
                                    .cornerRadius(6)
                                    .position(x: rx + rw/2, y: max(10, ry - 14))
                                }
                            }
                        }
                        .frame(height: 320)
                    }
                    .padding(.horizontal)
                    
                    // Symptom Labels
                    HStack {
                        Image(systemName: "camera.filters")
                            .foregroundColor(.teal)
                        Text("검출된 증상:")
                            .fontWeight(.medium)
                        Text(result.symptoms.joined(separator: ", "))
                            .foregroundColor(.red)
                            .fontWeight(.bold)
                        Spacer()
                    }
                    .padding(.horizontal)
                    
                    // Deficient nutrients list
                    VStack(alignment: .leading, spacing: 10) {
                        Text("⚠️ 보충이 시급한 영양소")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                            .padding(.horizontal)
                        
                        ForEach(result.deficientNutrients) { nutrient in
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(nutrient.name)
                                        .font(.headline)
                                        .foregroundColor(.white)
                                    Spacer()
                                    Text(nutrient.status)
                                        .font(.subheadline)
                                        .fontWeight(.bold)
                                        .foregroundColor(.orange)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 2)
                                        .background(Color.orange.opacity(0.2))
                                        .cornerRadius(8)
                                }
                                
                                Text(nutrient.description)
                                    .font(.subheadline)
                                    .foregroundColor(.gray)
                                
                                HStack {
                                    Text("추천 식품:")
                                        .font(.caption)
                                        .foregroundColor(.teal)
                                    Text(nutrient.foods.joined(separator: ", "))
                                        .font(.caption)
                                        .foregroundColor(.white)
                                }
                            }
                            .padding()
                            .background(Color(white: 0.12))
                            .cornerRadius(12)
                            .padding(.horizontal)
                        }
                    }
                    
                    // Sufficient nutrients list
                    if !result.sufficientNutrients.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("✅ 현재 양호한 영양소")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundColor(.teal)
                                .padding(.horizontal)
                            
                            ForEach(result.sufficientNutrients) { nutrient in
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(nutrient.name)
                                        .font(.headline)
                                        .foregroundColor(.white)
                                    Text(nutrient.description)
                                        .font(.subheadline)
                                        .foregroundColor(.gray)
                                }
                                .padding()
                                .background(Color(white: 0.12))
                                .cornerRadius(12)
                                .padding(.horizontal)
                            }
                        }
                    }
                    
                    // Overall advice
                    VStack(alignment: .leading, spacing: 8) {
                        Text("📋 전문가 영양 가이드")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.teal)
                        
                        Text(result.overallAdvice)
                            .font(.subheadline)
                            .foregroundColor(.white)
                            .lineSpacing(4)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.teal.opacity(0.12))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    .padding(.bottom, 30)
                    
                } else {
                    Spacer().frame(height: 100)
                    ProgressView("손톱의 픽셀 특징값 정밀 추출 중...")
                        .progressViewStyle(CircularProgressViewStyle(tint: .teal))
                    Spacer()
                }
            }
        }
        .background(Color(red: 0.08, green: 0.08, blue: 0.10).ignoresSafeArea())
        .onAppear {
            DispatchQueue.global(qos: .userInitiated).async {
                let analysis = NailClassifier.classify(image: image, imagePath: "temp_ios_photo.jpg")
                DispatchQueue.main.async {
                    self.result = analysis
                }
            }
        }
    }
    
    private func getKoreanLabel(for id: String) -> String {
        if id.contains("white_spots") {
            return "흰색 불완전 각화층 발견"
        } else if id.contains("vertical_ridges") {
            return "세로 홈 요철 결"
        } else if id.contains("spoon") {
            return "철결핍성 손톱 함몰"
        } else if id.contains("brittle") {
            return "조조증 부서짐 의심"
        } else if id.contains("onychomycosis") {
            return "무좀/진균 변색 주의"
        } else if id.contains("melanonychia") {
            return "멜라닌 침착 세로 띠"
        }
        return "의심 부위"
    }
}
