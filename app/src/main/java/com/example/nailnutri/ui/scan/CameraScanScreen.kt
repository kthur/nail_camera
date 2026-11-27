package com.example.nailnutri.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.analysis.GeminiAnalyzer
import com.example.nailnutri.analysis.MultiFingerAggregator
import com.example.nailnutri.analysis.NailClassifier
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.viewmodel.MainViewModel
import com.example.nailnutri.theme.NutriGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onAnalysisComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isMockMode by viewModel.isMockMode.collectAsStateWithLifecycle(initialValue = true)
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val useGemma by viewModel.useGemma.collectAsStateWithLifecycle(initialValue = false)
    val useOnDeviceVision by viewModel.useOnDeviceVision.collectAsStateWithLifecycle(initialValue = false)
    val gemmaModelPath by viewModel.gemmaModelPath.collectAsStateWithLifecycle(initialValue = "/data/local/tmp/gemma.bin")

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDemoSelector by remember { mutableStateOf(true) }
    var isMultiFingerMode by remember { mutableStateOf(false) }
    val fingerLabels = remember { listOf("엄지", "검지", "중지", "약지", "새끼") }
    val fingerResults = remember { mutableStateListOf<NailAnalysisResult>() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(flashEnabled) {
        imageCapture.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("손톱 촬영 및 분석", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "플래시",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "카메라 권한이 필요합니다.",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "손톱을 촬영해 영양소를 분석하려면 카메라 권한을 승인해야 합니다.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("권한 요청하기")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize()
                    )

                    CameraOverlay(modifier = Modifier.fillMaxSize())

                    // D2: Multi-finger sequential scan progress banner
                    if (isMultiFingerMode) {
                        val currentStep = fingerResults.size
                        val nextFinger = fingerLabels.getOrElse(currentStep) { "완료" }
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "5손가락 정밀 스캔 (${currentStep}/5) • 다음: $nextFinger",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (currentStep >= 2) {
                                        TextButton(
                                            onClick = {
                                                val agg = MultiFingerAggregator.aggregate(fingerResults.toList())
                                                fingerResults.clear()
                                                viewModel.saveResult(agg)
                                                onAnalysisComplete(agg.id)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("지금 집계", color = NutriGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (currentStep > 0) {
                                        TextButton(
                                            onClick = { fingerResults.clear() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) {
                                            Text("초기화", color = Color.LightGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { currentStep / 5f },
                                color = NutriGreen,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    errorMessage?.let { error ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(Color.Red.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            Text(
                                text = error,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isMockMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NutriGreen)
                                Text(
                                    text = "현재 데모 분석 모드입니다. 아래 샘플 중 하나를 선택하면 모의 분석을 빠르게 수행합니다.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            if (isMockMode) {
                                OutlinedButton(
                                    onClick = { showDemoSelector = !showDemoSelector },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("샘플선택", fontSize = 12.sp)
                                }
                            } else {
                                Box(modifier = Modifier.width(100.dp))
                            }

                            Surface(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isMockMode) {
                                            triggerMockAnalysis(
                                                "healthy",
                                                viewModel,
                                                coroutineScope,
                                                onAnalysisComplete,
                                                isMultiFingerMode = isMultiFingerMode
                                            )
                                        } else {
                                            if (!useGemma && !useOnDeviceVision && apiKey.isBlank()) {
                                                errorMessage =
                                                    "Gemini API 키가 없습니다. 설정에서 키를 등록하거나 데모 모드를 활성화해주세요."
                                                return@clickable
                                            }
                                            if (useGemma && gemmaModelPath.isBlank()) {
                                                errorMessage =
                                                    "Gemma 모델 경로가 설정되지 않았습니다. 설정에서 경로를 지정해 주세요."
                                                return@clickable
                                            }
                                            analyzing = true
                                            errorMessage = null
                                            coroutineScope.launch {
                                                val files = mutableListOf<File>()
                                                try {
                                                    val executor = ContextCompat.getMainExecutor(context)
                                                    // 1. 3장 연속 촬영 (250ms 간격)
                                                    for (i in 1..3) {
                                                        val file = ImageCaptureProcessor.captureImageAsync(imageCapture, context, executor, i)
                                                        files.add(file)
                                                        kotlinx.coroutines.delay(250)
                                                    }

                                                    // 2 & 3. 각 이미지 전처리 및 3장 픽셀 평균화 (Dispatchers.Default)
                                                    val avgBitmap = ImageCaptureProcessor.processBurstCaptures(files)

                                                    // 4. M11: files[1], files[2] 정리 (files[0]만 유지)
                                                    ImageCaptureProcessor.cleanupBurstFiles(files, keepIndex = 0)

                                                    // 5. 첫 번째 파일 경로로 최종 분석 (Dispatchers.Default)
                                                    val finalFile = files[0]
                                                    val result = withContext(Dispatchers.Default) {
                                                        if (useOnDeviceVision) {
                                                            NailClassifier.classify(
                                                                bitmap = avgBitmap,
                                                                imagePath = finalFile.absolutePath,
                                                                context = context
                                                            )
                                                        } else if (useGemma) {
                                                            com.example.nailnutri.analysis.GemmaAnalyzer.analyzeNail(
                                                                context = context,
                                                                bitmap = avgBitmap,
                                                                modelPath = gemmaModelPath,
                                                                imagePath = finalFile.absolutePath
                                                            )
                                                        } else {
                                                            GeminiAnalyzer.analyzeNail(
                                                                bitmap = avgBitmap,
                                                                apiKey = apiKey,
                                                                imagePath = finalFile.absolutePath
                                                            )
                                                        }
                                                    }
                                                    if (isMultiFingerMode) {
                                                        fingerResults.add(result)
                                                        analyzing = false
                                                        if (fingerResults.size >= 5) {
                                                            val aggregated = MultiFingerAggregator.aggregate(fingerResults.toList())
                                                            fingerResults.clear()
                                                            viewModel.saveResult(aggregated)
                                                            onAnalysisComplete(aggregated.id)
                                                        }
                                                    } else {
                                                        viewModel.saveResult(result)
                                                        onAnalysisComplete(result.id)
                                                    }
                                                } catch (t: Throwable) {
                                                    t.printStackTrace()
                                                    errorMessage = t.message ?: "촬영 및 분석 중 오류 발생"
                                                    analyzing = false
                                                    ImageCaptureProcessor.cleanupAllFiles(files)
                                                }
                                            }
                                        }
                                    },
                                color = Color.White,
                                border = BorderStroke(4.dp, NutriGreen)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture",
                                        tint = Color.Black,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // D2: Single-finger vs 5-finger scan mode toggle
                            OutlinedButton(
                                onClick = {
                                    if (!analyzing) {
                                        isMultiFingerMode = !isMultiFingerMode
                                        fingerResults.clear()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isMultiFingerMode) NutriGreen else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMultiFingerMode) NutriGreen else Color.White.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text(
                                    text = if (isMultiFingerMode) "5손가락 ON" else "5손가락 모드",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isMockMode && showDemoSelector) {
                        DemoSelectorPanel(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp)
                                .padding(horizontal = 16.dp),
                            onSelect = { condition ->
                                analyzing = true
                                showDemoSelector = false
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1500)
                                    triggerMockAnalysis(
                                        condition,
                                        viewModel,
                                        coroutineScope,
                                        onAnalysisComplete,
                                        isMultiFingerMode = isMultiFingerMode
                                    )
                                }
                            }
                        )
                    }

                    // 분석 중 로딩 오버레이
                    if (analyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NutriGreen,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    "손톱 분석 중...",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                                Text(
                                    "잠시만 기다려 주세요",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val preview = remember { Preview.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private fun triggerMockAnalysis(
    condition: String,
    viewModel: MainViewModel,
    coroutineScope: CoroutineScope,
    onAnalysisComplete: (String) -> Unit,
    isMultiFingerMode: Boolean = false
) {
    coroutineScope.launch {
        val result = if (isMultiFingerMode) {
            val samples = (1..5).map { idx ->
                NailClassifier.buildResultForCondition(condition, "demo_${condition}_finger_$idx")
            }
            MultiFingerAggregator.aggregate(samples)
        } else {
            NailClassifier.buildResultForCondition(condition, "demo_$condition")
        }
        viewModel.saveResult(result)
        onAnalysisComplete(result.id)
    }
}
