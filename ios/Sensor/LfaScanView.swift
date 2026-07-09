import SwiftUI

struct LfaScanView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var repository: DataRepository
    var onAnalysisComplete: (String) -> Void
    
    @State private var isAnalyzing = false
    
    var body: some View {
        ZStack {
            Color.black.edgesIgnoringSafeArea(.all)
            
            // Mock Camera
            Rectangle().fill(Color.gray.opacity(0.3)).edgesIgnoringSafeArea(.all)
            
            // Guide box
            RoundedRectangle(cornerRadius: 12)
                .stroke(style: StrokeStyle(lineWidth: 3, dash: [10, 5]))
                .foregroundColor(Color(red: 0.38, green: 0.65, blue: 0.98))
                .frame(width: 300, height: 90)
            
            VStack {
                Text("자가 진단 시약 키트가 정방향으로\n가이드 박스 내부에 꽉 차게 맞춰주세요")
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
                        .progressViewStyle(CircularProgressViewStyle(tint: Color(red: 0.38, green: 0.65, blue: 0.98)))
                        .scaleEffect(1.5)
                        .padding(.bottom, 40)
                } else {
                    Button(action: {
                        isAnalyzing = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                            let result = KitReader.readKit(image: UIImage(), imagePath: "ios_lfa.jpg")
                            repository.saveResult(result)
                            isAnalyzing = false
                            self.presentationMode.wrappedValue.dismiss()
                            onAnalysisComplete(result.id)
                        }
                    }) {
                        Circle()
                            .fill(Color(red: 0.38, green: 0.65, blue: 0.98))
                            .frame(width: 76, height: 76)
                            .overlay(Text("판독").bold().foregroundColor(.white))
                    }
                    .padding(.bottom, 40)
                }
            }
        }
        .navigationBarHidden(true)
    }
}
