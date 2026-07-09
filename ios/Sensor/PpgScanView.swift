import SwiftUI

struct PpgScanView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var repository: DataRepository
    var onAnalysisComplete: (String) -> Void
    
    @State private var isMeasuring = false
    @State private var isFingerDetected = true
    @State private var progress: Float = 0.0
    @State private var waveformPoints: [CGFloat] = (0..<40).map { _ in CGFloat.random(in: 40...100) }
    
    let timer = Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()
    
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
                Text("실시간 PPG 혈류 진단").bold().foregroundColor(.white)
                Spacer()
            }
            .padding()
            
            Spacer()
            
            Text(isFingerDetected ? "지속적으로 손가락을 대주세요" : "후면 카메라와 플래시 렌즈에 검지 손가락 끝을 대주세요")
                .font(.headline)
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
            
            // Pulse waveform Canvas Graph
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(red: 0.12, green: 0.16, blue: 0.23))
                    .frame(height: 180)
                
                Path { path in
                    guard waveformPoints.count > 1 else { return }
                    path.move(to: CGPoint(x: 0, y: waveformPoints[0]))
                    let stepX = UIScreen.main.bounds.width / CGFloat(waveformPoints.count - 1)
                    
                    for idx in 1..<waveformPoints.count {
                        path.addLine(to: CGPoint(x: CGFloat(idx) * stepX, y: waveformPoints[idx]))
                    }
                }
                .stroke(Color(red: 0.2, green: 0.83, blue: 0.6), lineWidth: 3)
                .frame(height: 180)
            }
            .padding()
            
            if isMeasuring {
                ProgressView(value: progress)
                    .progressViewStyle(LinearProgressViewStyle(tint: Color(red: 0.2, green: 0.83, blue: 0.6)))
                    .padding()
                Text("영양 분석 진행도: \(Int(progress * 100))%")
                    .foregroundColor(.white.opacity(0.8))
            } else {
                Button(action: {
                    isMeasuring = true
                    progress = 0.0
                }) {
                    Text("측정 시작하기")
                        .bold()
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color(red: 0.2, green: 0.83, blue: 0.6))
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding()
            }
            
            Spacer()
        }
        .background(Color(red: 0.06, green: 0.09, blue: 0.16).edgesIgnoringSafeArea(.all))
        .navigationBarHidden(true)
        .onReceive(timer) { _ in
            if isMeasuring {
                progress += 0.02
                
                // Shift wave points randomly to simulate pulse ripples
                waveformPoints.removeFirst()
                waveformPoints.append(CGFloat.random(in: 30...130))
                
                if progress >= 1.0 {
                    isMeasuring = false
                    timer.upstream.connect().cancel()
                    
                    let result = PPGAnalyzer.analyzePPG(redMeanBuffer: (0..<120).map { _ in Double.random(in: 200...220) },
                                                        timeStamps: (0..<120).map { Int64($0 * 33) },
                                                        imagePath: "ios_ppg.png")
                    repository.saveResult(result)
                    self.presentationMode.wrappedValue.dismiss()
                    onAnalysisComplete(result.id)
                }
            }
        }
    }
}
