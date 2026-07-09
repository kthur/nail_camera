import SwiftUI

struct MainHomeView: View {
    @State private var isShowingCamera = false
    @State private var capturedImage: UIImage?
    @State private var isShowingAnalysis = false
    
    // Sensor diagnostic flows
    @State private var isShowingSensorDashboard = false
    @State private var selectedResultId: String? = nil
    @State private var isShowingSensorResult = false
    
    // Shared reference to repository for simplicity
    private var repository = DataRepository.shared
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Spacer()
                
                // Modern Premium Branding Logo (Stylized Nail Scan)
                ZStack {
                    Circle()
                        .fill(LinearGradient(gradient: Gradient(colors: [Color.teal, Color.purple]), startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 120, height: 120)
                        .opacity(0.15)
                        .blur(radius: 5)
                    
                    VStack(spacing: 8) {
                        Image(systemName: "camera.viewfinder")
                            .font(.system(size: 50))
                            .foregroundColor(.teal)
                        
                        Image(systemName: "waveform.path")
                            .font(.system(size: 20))
                            .foregroundColor(.purple)
                            .offset(y: -8)
                    }
                }
                
                VStack(spacing: 6) {
                    Text("NailNutri iOS")
                        .font(.title)
                        .fontWeight(.black)
                        .foregroundColor(.white)
                    
                    Text("손톱 촬영 분석 및 1대1 영양 카운슬러")
                        .font(.footnote)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                // Smart Sensor Diagnostics Suite Entry Card
                Button(action: {
                    isShowingSensorDashboard = true
                }) {
                    HStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(Color.teal.opacity(0.15))
                                .frame(width: 44, height: 44)
                            Image(systemName: "sensor.tag.radiowaves.forward.fill")
                                .foregroundColor(.teal)
                                .font(.system(size: 18))
                        }
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("지능형 센서 자가 진단 스위트")
                                .font(.subheadline).bold()
                                .foregroundColor(.white)
                            Text("카메라 결막 혈색, 플래시 PPG 맥파, LFA 키트 분석 등")
                                .font(.caption2)
                                .foregroundColor(.white.opacity(0.6))
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.gray)
                            .font(.system(size: 14))
                    }
                    .padding()
                    .background(Color(red: 0.12, green: 0.16, blue: 0.23))
                    .cornerRadius(16)
                }
                .padding(.horizontal)
                .sheet(isPresented: $isShowingSensorDashboard) {
                    SensorDashboardView(repository: repository) { resultId in
                        self.selectedResultId = resultId
                        self.isShowingSensorResult = true
                    }
                }
                
                // Call To Actions
                VStack(spacing: 12) {
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
                        .padding(.vertical, 14)
                        .background(LinearGradient(gradient: Gradient(colors: [Color.teal, Color.teal.opacity(0.8)]), startPoint: .leading, endPoint: .trailing))
                        .cornerRadius(14)
                    }
                    .sheet(isPresented: $isShowingCamera) {
                        CameraView(capturedImage: $capturedImage, isShowingAnalysis: $isShowingAnalysis)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 30)
                
                // Navigation Link for nail analysis result view
                NavigationLink(
                    destination: AnalysisResultView(image: capturedImage ?? UIImage()),
                    isActive: $isShowingAnalysis,
                    label: { EmptyView() }
                )
                
                // Navigation Link for sensor results
                NavigationLink(
                    destination: SensorResultDetailView(resultId: selectedResultId ?? ""),
                    isActive: $isShowingSensorResult,
                    label: { EmptyView() }
                )
            }
            .background(Color(red: 0.08, green: 0.08, blue: 0.10).ignoresSafeArea())
            .navigationBarHidden(true)
        }
    }
}

// Stub view to show diagnostic results
struct SensorResultDetailView: View {
    let resultId: String
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("자가 진단 완료 리포트")
                    .font(.title2).bold()
                    .foregroundColor(.white)
                
                Text("진단 ID: \\(resultId)")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                Divider().background(Color.gray)
                
                VStack(alignment: .leading, spacing: 10) {
                    Text("진단 결과 요약")
                        .font(.headline)
                        .foregroundColor(.teal)
                    
                    Text("하드웨어 센싱 데이터 분석 결과 체내 영양소 불균형 가능성이 판독되었습니다. 상세 권장 가이드는 마이페이지 히스토리에서 다시 확인하실 수 있습니다.")
                        .font(.body)
                        .foregroundColor(.white.opacity(0.8))
                }
                .padding()
                .background(Color(white: 0.12))
                .cornerRadius(12)
            }
            .padding()
        }
        .background(Color(red: 0.08, green: 0.08, blue: 0.10).ignoresSafeArea())
    }
}
