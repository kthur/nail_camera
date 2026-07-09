import SwiftUI

struct SleepAudioView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var repository: DataRepository
    var onAnalysisComplete: (String) -> Void
    
    @State private var isRecording = false
    @State private var countdown = 5
    @State private var decibel: Float = 30.0
    @State private var waveHeight: CGFloat = 10.0
    
    let timer = Timer.publish(every: 1.0, on: .main, in: .common).autoconnect()
    
    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Button(action: {
                    self.presentationMode.wrappedValue.dismiss()
                }) {
                    Image(systemName: "chevron.left").foregroundColor(.white)
                    Text("이전").foregroundColor(.white)
                }
                Spacer()
                Text("수면 숨소리/코골이 분석").bold().foregroundColor(.white)
                Spacer()
            }
            .padding()
            
            Text(isRecording ? "수면 호흡 음질 분석 중... (\(countdown)초)" : "기기를 침대 협탁에 두고 5초간 분석을 시작해보세요")
                .foregroundColor(.white)
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding()
            
            // Visualizer wave effect
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(red: 0.12, green: 0.16, blue: 0.23))
                    .frame(height: 200)
                
                HStack(spacing: 6) {
                    ForEach(0..<15) { _ in
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color(red: 0.75, green: 0.52, blue: 0.99))
                            .frame(width: 6, height: isRecording ? CGFloat.random(in: 20...150) : 10)
                    }
                }
            }
            .padding()
            
            if isRecording {
                Text("실시간 음량: \(String(format: "%.1f", decibel)) dB")
                    .bold()
                    .font(.title2)
                    .foregroundColor(Color(red: 0.75, green: 0.52, blue: 0.99))
            }
            
            Spacer()
            
            Button(action: {
                isRecording = true
                countdown = 5
                decibel = 32.0
            }) {
                Text(isRecording ? "진단 음향 채집 중..." : "5초 수면 호흡 검사 시작")
                    .bold()
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color(red: 0.75, green: 0.52, blue: 0.99))
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .padding()
            .disabled(isRecording)
        }
        .background(Color(red: 0.06, green: 0.09, blue: 0.16).edgesIgnoringSafeArea(.all))
        .navigationBarHidden(true)
        .onReceive(timer) { _ in
            if isRecording {
                countdown -= 1
                decibel = Float.random(in: 40...65)
                
                if countdown <= 0 {
                    isRecording = false
                    let result = SleepAudioAnalyzer.analyzeAudioBuffer(audioSamples: [], imagePath: "ios_sleep_snore.wav")
                    repository.saveResult(result)
                    self.presentationMode.wrappedValue.dismiss()
                    onAnalysisComplete(result.id)
                }
            }
        }
    }
}
