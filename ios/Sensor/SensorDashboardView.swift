import SwiftUI

struct SensorDashboardView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var repository: DataRepository
    
    var onAnalysisComplete: (String) -> Void
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    Text("디바이스의 카메라, 플래시, 마이크 하드웨어 센서를 활용해 신체 영양 및 긴장 징후를 판독합니다.")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.7))
                        .padding(.top, 10)
                        .padding(.horizontal)
                    
                    NavigationLink(destination: AnemiaScanView(repository: repository, onAnalysisComplete: onAnalysisComplete)) {
                        SensorRow(title: "안구 결막 빈혈 진단 (철분 결핍)",
                                  description: "눈 아래 결막의 붉은 혈색 채도 비율을 정규화 스크리닝하여 철결핍성 빈혈 위험을 판독합니다.",
                                  icon: "face.smiling",
                                  iconColor: Color(red: 0.97, green: 0.44, blue: 0.44))
                    }
                    
                    NavigationLink(destination: PpgScanView(repository: repository, onAnalysisComplete: onAnalysisComplete)) {
                        SensorRow(title: "실시간 PPG 미네랄 측정 (마그네슘/칼슘)",
                                  description: "카메라와 플래시에 손가락을 밀착하여 심박변이도(HRV) 파형을 판독하고 스트레스 지수를 측정합니다.",
                                  icon: "heart.fill",
                                  iconColor: Color(red: 0.2, green: 0.83, blue: 0.6))
                    }
                    
                    NavigationLink(destination: LfaScanView(repository: repository, onAnalysisComplete: onAnalysisComplete)) {
                        SensorRow(title: "LFA 시약 키트 판독 (비타민 D/B12)",
                                  description: "소변 진단 키트의 발색 색조 변화 강도를 촬영해 영양소 수치 농도를 정량 측정합니다.",
                                  icon: "camera.fill",
                                  iconColor: Color(red: 0.38, green: 0.65, blue: 0.98))
                    }
                    
                    NavigationLink(destination: SleepAudioView(repository: repository, onAnalysisComplete: onAnalysisComplete)) {
                        SensorRow(title: "코골이 수면 소리 수집 (비타민 D/마그네슘)",
                                  description: "수면 중 호흡음 저주파 대역 점유 에너지를 판독해 근육 피로 및 비타민 D 결핍 징후를 예방합니다.",
                                  icon: "moon.stars.fill",
                                  iconColor: Color(red: 0.75, green: 0.52, blue: 0.99))
                    }
                    
                    NavigationLink(destination: VoiceAnalysisView(repository: repository, onAnalysisComplete: onAnalysisComplete)) {
                        SensorRow(title: "목소리 3초 피로도 진단 (비타민 B군)",
                                  description: "3초간의 발성 주파수 변동(Jitter)과 크기 흔들림(Shimmer)을 모니터링해 신경계 피로를 추적합니다.",
                                  icon: "mic.fill",
                                  iconColor: Color(red: 0.98, green: 0.75, blue: 0.14))
                    }
                }
                .padding()
            }
            .background(Color(red: 0.06, green: 0.09, blue: 0.16).edgesIgnoringSafeArea(.all))
            .navigationBarTitle("센서 자가 진단 스위트", displayMode: .inline)
            .navigationBarItems(leading: Button(action: {
                self.presentationMode.wrappedValue.dismiss()
            }) {
                Image(systemName: "chevron.left")
                    .foregroundColor(.white)
                Text("뒤로")
                    .foregroundColor(.white)
            })
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

struct SensorRow: View {
    let title: String
    let description: String
    let icon: String
    let iconColor: Color
    
    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(iconColor.opacity(0.12))
                    .frame(width: 48, height: 48)
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(iconColor)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.white)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.6))
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
            
            Spacer()
        }
        .padding()
        .background(Color(red: 0.12, green: 0.16, blue: 0.23))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}
