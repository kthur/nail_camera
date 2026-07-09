package com.example.nailnutri.ui.sensor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nailnutri.analysis.PPGAnalyzer
import com.example.nailnutri.data.DataRepository
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalGetImage
@Composable
fun PpgScanScreen(
    repository: DataRepository,
    onBackClick: () -> Unit,
    onAnalysisComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isMeasuring by remember { mutableStateOf(false) }
    var isFingerDetected by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val waveformPoints = remember { mutableStateListOf<Float>() }
    val redBuffer = remember { mutableListOf<Double>() }
    val timestampBuffer = remember { mutableListOf<Long>() }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("실시간 PPG 혈류 진단", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "카메라 권한 필요",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "손가락의 펄스 맥동 혈관 변화(PPG)를 감지하기 위해 후면 카메라 권한이 필요합니다.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                    ) {
                        Text("설정 앱으로 이동하여 권한 허용하기", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .clip(CircleShape)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    
                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val buffer = mediaImage.planes[0].buffer
                                        val bytes = ByteArray(buffer.remaining())
                                        buffer.get(bytes)
                                        
                                        var rSum = 0.0; var gSum = 0.0; var bSum = 0.0
                                        val step = (bytes.size / 200).coerceAtLeast(4)
                                        var count = 0
                                        
                                        for (i in 0 until bytes.size - 4 step step) {
                                            val r = bytes[i].toInt() and 0xFF
                                            val g = bytes[i+1].toInt() and 0xFF
                                            val b = bytes[i+2].toInt() and 0xFF
                                            rSum += r; gSum += g; bSum += b
                                            count++
                                        }
                                        
                                        val avgR = rSum / count
                                        val avgG = gSum / count
                                        val avgB = bSum / count
                                        
                                        val fingerPresent = avgR > 200 && avgG < 65 && avgB < 65
                                        isFingerDetected = fingerPresent
                                        
                                        if (fingerPresent && isMeasuring) {
                                            val signal = avgR - (avgG * 1.5)
                                            redBuffer.add(signal)
                                            timestampBuffer.add(System.currentTimeMillis())
                                            
                                            waveformPoints.add(signal.toFloat())
                                            if (waveformPoints.size > 100) {
                                                waveformPoints.removeAt(0)
                                            }
                                            
                                            progress += 1.0f / (30.0f * 10.0f)
                                            if (progress >= 1f) {
                                                isMeasuring = false
                                                
                                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                                                } else {
                                                    vibrator.vibrate(120)
                                                }
                                                
                                                scope.launch {
                                                    val res = PPGAnalyzer.analyzePPG(redBuffer.toDoubleArray(), timestampBuffer.toLongArray(), "ppg_heart_pulse.png")
                                                    repository.saveResult(res)
                                                    onAnalysisComplete(res.id)
                                                }
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }
                                
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    cam.cameraControl.enableTorch(true)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isFingerDetected) "지속적으로 손가락을 대주세요" else "후면 카메라와 플래시 렌즈에\n검지 손가락 끝을 밀착해 대주세요",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 30.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (waveformPoints.size > 2) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val path = Path()
                                    
                                    val minVal = waveformPoints.min()
                                    val maxVal = waveformPoints.max()
                                    val range = (maxVal - minVal).coerceAtLeast(1f)
                                    
                                    val stepX = w / (waveformPoints.size - 1)
                                    
                                    val yStart = h - ((waveformPoints[0] - minVal) / range) * h
                                    path.moveTo(0f, yStart)
                                    
                                    for (i in 1 until waveformPoints.size) {
                                        val x = i * stepX
                                        val y = h - ((waveformPoints[i] - minVal) / range) * h
                                        path.lineTo(x, y)
                                    }
                                    
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF34D399),
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            } else {
                                Text(
                                    text = "혈류 파형 측정 준비 중...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    if (isMeasuring) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "영양 분석 진행도: ${(progress * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (isFingerDetected) {
                                    isMeasuring = true
                                    progress = 0f
                                    redBuffer.clear()
                                    timestampBuffer.clear()
                                    waveformPoints.clear()
                                }
                            },
                            enabled = isFingerDetected,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                        ) {
                            Text(
                                text = if (isFingerDetected) "측정 시작하기" else "손가락 접촉 대기 중",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
