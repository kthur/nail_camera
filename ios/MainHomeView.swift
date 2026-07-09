import SwiftUI

struct MainHomeView: View {
    @State private var isShowingCamera = false
    @State private var capturedImage: UIImage?
    @State private var isShowingAnalysis = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 30) {
                Spacer()
                
                // Modern Premium Branding Logo (Stylized Nail Scan)
                ZStack {
                    Circle()
                        .fill(LinearGradient(gradient: Gradient(colors: [Color.teal, Color.purple]), startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 140, height: 140)
                        .opacity(0.15)
                        .blur(radius: 5)
                    
                    VStack(spacing: 8) {
                        Image(systemName: "camera.viewfinder")
                            .font(.system(size: 60))
                            .foregroundColor(.teal)
                        
                        Image(systemName: "waveform.path")
                            .font(.system(size: 24))
                            .foregroundColor(.purple)
                            .offset(y: -10)
                    }
                }
                
                VStack(spacing: 8) {
                    Text("NailNutri iOS")
                        .font(.largeTitle)
                        .fontWeight(.black)
                        .foregroundColor(.white)
                    
                    Text("손톱 촬영 분석 및 1대1 영양 카운슬러")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                // Instructions / Cards
                VStack(spacing: 12) {
                    HStack(spacing: 15) {
                        Image(systemName: "1.circle.fill")
                            .foregroundColor(.teal)
                        Text("손가락의 손톱이 정중앙에 맞춰지도록 촬영")
                            .font(.footnote)
                            .foregroundColor(.white)
                        Spacer()
                    }
                    .padding()
                    .background(Color(white: 0.12))
                    .cornerRadius(12)
                    
                    HStack(spacing: 15) {
                        Image(systemName: "2.circle.fill")
                            .foregroundColor(.teal)
                        Text("빛 반사가 없는 그늘이나 일반 조명에서 촬영")
                            .font(.footnote)
                            .foregroundColor(.white)
                        Spacer()
                    }
                    .padding()
                    .background(Color(white: 0.12))
                    .cornerRadius(12)
                }
                .padding(.horizontal)
                
                // Call To Actions
                VStack(spacing: 15) {
                    Button(action: {
                        isShowingCamera = true
                    }) {
                        HStack {
                            Image(systemName: "camera.fill")
                            Text("손톱 영양 상태 정밀 분석하기")
                                .fontWeight(.bold)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(LinearGradient(gradient: Gradient(colors: [Color.teal, Color.teal.opacity(0.8)]), startPoint: .leading, endPoint: .trailing))
                        .cornerRadius(14)
                        .shadow(color: .teal.opacity(0.3), radius: 10, y: 5)
                    }
                    .sheet(isPresented: $isShowingCamera) {
                        CameraView(capturedImage: $capturedImage, isShowingAnalysis: $isShowingAnalysis)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 40)
                
                // Navigation Link for analysis result view
                NavigationLink(
                    destination: AnalysisResultView(image: capturedImage ?? UIImage()),
                    isActive: $isShowingAnalysis,
                    label: { EmptyView() }
                )
            }
            .background(Color(red: 0.08, green: 0.08, blue: 0.10).ignoresSafeArea())
            .navigationBarHidden(true)
        }
    }
}
