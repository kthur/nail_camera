import SwiftUI

struct AnemiaScanView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var repository: DataRepository
    var onAnalysisComplete: (String) -> Void
    
    @State private var isAnalyzing = false
    
    var body: some View {
        ZStack {
            Color.black.edgesIgnoringSafeArea(.all)
            
            // Mock Camera Preview
            Rectangle()
                .fill(Color.gray.opacity(0.3))
                .edgesIgnoringSafeArea(.all)
            
            // Target guidance red ellipse
            VStack {
                Spacer()
                RoundedRectangle(cornerRadius: 16)
                    .stroke(style: StrokeStyle(lineWidth: 3, dash: [10, 5]))
                    .foregroundColor(.red)
                    .frame(width: 250, height: 80)
                Spacer()
            }
            
            VStack {
                Text("눈 아래 꺼풀을 뒤집어 붉은 결막 영역이\n빨간 가이드 박스 내부에 들어오게 맞춰주세요")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding()
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(12)
                    .padding(.top, 20)
                
                Spacer()
                
                if isAnalyzing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .red))
                        .scaleEffect(1.5)
                        .padding(.bottom, 40)
                } else {
                    Button(action: {
                        isAnalyzing = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                            let result = ConjunctivaAnalyzer.analyze(image: UIImage(), imagePath: "ios_conjunctiva.jpg")
                            repository.saveResult(result)
                            isAnalyzing = false
                            self.presentationMode.wrappedValue.dismiss()
                            onAnalysisComplete(result.id)
                        }
                    }) {
                        Circle()
                            .fill(Color.red)
                            .frame(width: 76, height: 76)
                            .overlay(Text("촬영").bold().foregroundColor(.white))
                    }
                    .padding(.bottom, 40)
                }
            }
        }
        .navigationBarHidden(true)
    }
}
