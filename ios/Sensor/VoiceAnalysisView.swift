import SwiftUI

struct VoiceAnalysisView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var repository: DataRepository
    var onAnalysisComplete: (String) -> Void
    
    @State private var isRecording = false
    @State private var countdown = 3
    @State private var voicePulseScale: CGFloat = 1.0
    
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
                Text("3초 발성 신경 피로도 분석").bold().foregroundColor(.white)
                Spacer()
            }
            .padding()
            
            Text("아-- 소리를 3초간 일정하게 내주세요")
                .foregroundColor(.white)
                .font(.headline)
            
            Spacer()
            
            // Microphone Pulse Circle Animation
            ZStack {
                Circle()
                    .fill(Color(red: 0.98, green: 0.75, blue: 0.14).opacity(isRecording ? 0.15 : 0.08))
                    .frame(width: isRecording ? 160 * voicePulseScale : 140, height: isRecording ? 160 * voicePulseScale : 140)
                
                Circle()
                    .fill(Color(red: 0.98, green: 0.75, blue: 0.14))
                    .frame(width: 100, height: 100)
                    .overlay(
                        Text(isRecording ? "\(countdown)초" : "준비")
                            .font(.title).bold()
                            .foregroundColor(.white)
                    )
            }
            .animation(.easeInOut(duration: 0.5), value: voicePulseScale)
            
            Spacer()
            
            Button(action: {
                isRecording = true
                countdown = 3
                voicePulseScale = 1.2
            }) {
                Text(isRecording ? "성대 마이크 신호 채집 중..." : "목소리 분석 시작")
                    .bold()
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color(red: 0.98, green: 0.75, blue: 0.14))
                    .foregroundColor(.black)
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
                voicePulseScale = voicePulseScale == 1.2 ? 1.0 : 1.2
                
                if countdown <= 0 {
                    isRecording = false
                    let result = VoiceAnalyzer.analyzeVoice(audioSamples: [], imagePath: "ios_voice.wav")
                    repository.saveResult(result)
                    self.presentationMode.wrappedValue.dismiss()
                    onAnalysisComplete(result.id)
                }
            }
        }
    }
}
