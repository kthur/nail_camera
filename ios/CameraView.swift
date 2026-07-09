import SwiftUI
import AVFoundation

struct CameraView: View {
    @Binding var capturedImage: UIImage?
    @Binding var isShowingAnalysis: Bool
    @Environment(\.presentationMode) var presentationMode
    
    @StateObject private var camera = CameraModel()
    
    var body: some View {
        ZStack {
            // Camera Preview
            CameraPreviewView(camera: camera)
                .ignoresSafeArea()
            
            // Custom overlay guides (Nail Guide)
            VStack {
                Text("가이드라인 안에 손톱을 맞춰 정렬하세요")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(20)
                    .padding(.top, 30)
                
                Spacer()
                
                // Finger nail overlay frame matching Android screen (60% width, 1.33 aspect)
                GeometryReader { geo in
                    let w = geo.size.width * 0.6
                    let h = w * 1.33
                    
                    ZStack {
                        // Bounding Box
                        RoundedRectangle(cornerRadius: 30)
                            .stroke(Color.teal, style: StrokeStyle(lineWidth: 3, lineCap: .round, dash: [8, 5]))
                            .frame(width: w, height: h)
                            .shadow(color: .teal.opacity(0.5), radius: 10)
                        
                        // Central crosshair
                        Circle()
                            .fill(Color.teal)
                            .frame(width: 8, height: 8)
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                }
                
                Spacer()
                
                // Camera controls
                HStack(spacing: 60) {
                    // Back button
                    Button(action: {
                        presentationMode.wrappedValue.dismiss()
                    }) {
                        Image(systemName: "xmark")
                            .font(.title2)
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }
                    
                    // Capture button
                    Button(action: {
                        camera.takePic { image in
                            self.capturedImage = image
                            self.isShowingAnalysis = true
                        }
                    }) {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 80, height: 80)
                            .overlay(
                                Circle()
                                    .stroke(Color.teal, lineWidth: 4)
                                    .scaleEffect(0.9)
                            )
                            .shadow(radius: 10)
                    }
                    
                    // Spacer to match balance
                    Spacer()
                        .frame(width: 56)
                }
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            camera.checkPermissions()
        }
    }
}

class CameraModel: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate {
    @Published var session = AVCaptureSession()
    @Published var output = AVCapturePhotoOutput()
    private var onCaptureCompletion: ((UIImage) -> Void)?
    
    func checkPermissions() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            setup()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { status in
                if status {
                    DispatchQueue.main.async {
                        self.setup()
                    }
                }
            }
        default:
            break
        }
    }
    
    func setup() {
        do {
            self.session.beginConfiguration()
            
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else { return }
            let input = try AVCaptureDeviceInput(device: device)
            
            if self.session.canAddInput(input) {
                self.session.addInput(input)
            }
            
            if self.session.canAddOutput(self.output) {
                self.session.addOutput(self.output)
            }
            
            self.session.commitConfiguration()
            
            DispatchQueue.global(qos: .background).async {
                self.session.startRunning()
            }
        } catch {
            print(error.localizedDescription)
        }
    }
    
    func takePic(completion: @escaping (UIImage) -> Void) {
        self.onCaptureCompletion = completion
        let settings = AVCapturePhotoSettings()
        self.output.capturePhoto(with: settings, delegate: self)
    }
    
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard error == nil else { return }
        guard let imageData = photo.fileDataRepresentation(),
              let image = UIImage(data: imageData) else { return }
        
        DispatchQueue.main.async {
            self.onCaptureCompletion?(image)
        }
    }
}

struct CameraPreviewView: UIViewRepresentable {
    @ObservedObject var camera: CameraModel
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: UIScreen.main.bounds)
        
        let preview = AVCaptureVideoPreviewLayer(session: camera.session)
        preview.frame = view.frame
        preview.videoGravity = .resizeAspectFill
        view.layer.addSublayer(preview)
        
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {}
}
