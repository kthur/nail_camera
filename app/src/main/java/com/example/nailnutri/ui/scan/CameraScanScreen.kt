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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.analysis.GeminiAnalyzer
import com.example.nailnutri.analysis.NailClassifier
import com.example.nailnutri.viewmodel.MainViewModel
import com.example.nailnutri.theme.NutriGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
            if (analyzing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "AI 분석 진행 중...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "손톱의 무늬, 질감, 색상을 파악하여 영양 결핍 정보를 산출하고 있습니다.",
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else if (!hasCameraPermission) {
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
                                                onAnalysisComplete
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
                                                try {
                                                    val executor = ContextCompat.getMainExecutor(context)
                                                    val files = mutableListOf<File>()
                                                    // 1. 3장 연속 촬영 (250ms 간격)
                                                    for (i in 1..3) {
                                                        val file = captureImageAsync(imageCapture, context, executor, i)
                                                        files.add(file)
                                                        kotlinx.coroutines.delay(250)
                                                    }

                                                    // 2. 각 이미지 전처리 및 다운스케일
                                                    val scaledBitmaps = mutableListOf<Bitmap>()
                                                    for (file in files) {
                                                        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                                                        val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                                                        val targetSize = 640
                                                        val sampleSize = when {
                                                            maxDim > targetSize * 4 -> 8
                                                            maxDim > targetSize * 2 -> 4
                                                            maxDim > targetSize -> 2
                                                            else -> 1
                                                        }
                                                        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                                        val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)!!
                                                        val rotatedBitmap = rotateBitmapIfRequired(rawBitmap, file.absolutePath)
                                                        val bitmap = cropNailRegion(rotatedBitmap)
                                                        if (rotatedBitmap != rawBitmap) {
                                                            rawBitmap.recycle()
                                                        }
                                                        // 공통 크기(224x224)로 다운스케일
                                                        scaledBitmaps.add(Bitmap.createScaledBitmap(bitmap, 224, 224, true))
                                                        if (bitmap != scaledBitmaps.last()) bitmap.recycle()
                                                    }

                                                    // 3. 3장 픽셀 평균화 → 일관된 단일 이미지
                                                    val avgBitmap = averageBitmaps(scaledBitmaps)

                                                    // 4. 첫 번째 파일 경로로 최종 분석
                                                    val finalFile = files[0]
                                                    if (useOnDeviceVision) {
                                                        val result = com.example.nailnutri.analysis.NailClassifier.classify(
                                                            bitmap = avgBitmap,
                                                            imagePath = finalFile.absolutePath,
                                                            context = context
                                                        )
                                                        viewModel.saveResult(result)
                                                        onAnalysisComplete(result.id)
                                                    } else if (useGemma) {
                                                        val result = com.example.nailnutri.analysis.GemmaAnalyzer.analyzeNail(
                                                            context = context,
                                                            bitmap = avgBitmap,
                                                            modelPath = gemmaModelPath,
                                                            imagePath = finalFile.absolutePath
                                                        )
                                                        viewModel.saveResult(result)
                                                        onAnalysisComplete(result.id)
                                                    } else {
                                                        val result = GeminiAnalyzer.analyzeNail(
                                                            bitmap = avgBitmap,
                                                            apiKey = apiKey,
                                                            imagePath = finalFile.absolutePath
                                                        )
                                                        viewModel.saveResult(result)
                                                        onAnalysisComplete(result.id)
                                                    }
                                                } catch (e: Exception) {
                                                    errorMessage = e.message ?: "촬영 및 분석 중 오류 발생"
                                                    analyzing = false
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

                            Box(modifier = Modifier.width(100.dp))
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
                                        onAnalysisComplete
                                    )
                                }
                            }
                        )
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

@Composable
fun CameraOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val cardWidth = width * 0.5f
        val cardHeight = cardWidth * 1.5f
        val xOffset = (width - cardWidth) / 2
        val yOffset = (height - cardHeight) / 2

        val outerPath = Path().apply {
            addRect(
                androidx.compose.ui.geometry.Rect(0f, 0f, width, height)
            )
        }

        val innerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = xOffset,
                    top = yOffset,
                    right = xOffset + cardWidth,
                    bottom = yOffset + cardHeight,
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            )
        }

        val resultPath = Path.combine(
            androidx.compose.ui.graphics.PathOperation.Difference,
            outerPath,
            innerPath
        )

        drawPath(
            path = resultPath,
            color = Color.Black.copy(alpha = 0.5f)
        )

        drawRoundRect(
            color = NutriGreen,
            topLeft = androidx.compose.ui.geometry.Offset(xOffset, yOffset),
            size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // 1. 손가락 실루엣 (점선)
        val fingerPath = Path().apply {
            moveTo(xOffset + cardWidth * 0.2f, yOffset + cardHeight)
            quadraticBezierTo(
                xOffset + cardWidth * 0.2f, yOffset + cardHeight * 0.3f,
                xOffset + cardWidth * 0.5f, yOffset + cardHeight * 0.25f
            )
            quadraticBezierTo(
                xOffset + cardWidth * 0.8f, yOffset + cardHeight * 0.3f,
                xOffset + cardWidth * 0.8f, yOffset + cardHeight * 0.9f
            )
            lineTo(xOffset + cardWidth * 0.8f, yOffset + cardHeight)
        }
        drawPath(
            path = fingerPath,
            color = Color.White.copy(alpha = 0.6f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // 2. 손톱 가이드라인 (녹색 실선)
        val nailPath = Path().apply {
            val nailLeft = xOffset + cardWidth * 0.35f
            val nailRight = xOffset + cardWidth * 0.65f
            val nailTop = yOffset + cardHeight * 0.33f
            val nailBottom = yOffset + cardHeight * 0.55f
            
            moveTo(nailLeft, nailBottom)
            quadraticBezierTo(
                nailLeft - 5f, nailTop + 15f,
                xOffset + cardWidth * 0.5f, nailTop
            )
            quadraticBezierTo(
                nailRight + 5f, nailTop + 15f,
                nailRight, nailBottom
            )
            quadraticBezierTo(
                xOffset + cardWidth * 0.5f, nailBottom - 10f,
                nailLeft, nailBottom
            )
        }
        drawPath(
            path = nailPath,
            color = NutriGreen,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 3. 방향 화살표 (↑)
        val arrowPath = Path().apply {
            val arrowX = xOffset + cardWidth * 0.5f
            val arrowY = yOffset + cardHeight * 0.72f
            moveTo(arrowX, arrowY)
            lineTo(arrowX, arrowY - 40f)
            moveTo(arrowX - 15f, arrowY - 25f)
            lineTo(arrowX, arrowY - 40f)
            lineTo(arrowX + 15f, arrowY - 25f)
        }
        drawPath(
            path = arrowPath,
            color = NutriGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 260.dp)
        ) {
            Text(
                text = "손가락 끝을 녹색 프레임에 맞춰 정렬하세요.",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun DemoSelectorPanel(
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "테스트용 샘플 손톱 유형 선택",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoButton("건강함", Modifier.weight(1f)) { onSelect("healthy") }
                DemoButton("흰 반점", Modifier.weight(1f)) { onSelect("white_spots") }
                DemoButton("세로줄", Modifier.weight(1f)) { onSelect("vertical_ridges") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoButton("숟가락", Modifier.weight(1f)) { onSelect("spoon_nails") }
                DemoButton("깨짐/갈라짐", Modifier.weight(1f)) { onSelect("brittle") }
            }
        }
    }
}

@Composable
fun DemoButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    context: Context,
    executor: Executor,
    onSuccess: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "nail_${System.currentTimeMillis()}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSuccess(photoFile)
            }
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun triggerMockAnalysis(
    condition: String,
    viewModel: MainViewModel,
    coroutineScope: CoroutineScope,
    onAnalysisComplete: (String) -> Unit
) {
    coroutineScope.launch {
        val result = NailClassifier.buildResultForCondition(condition, "demo_$condition")
        viewModel.saveResult(result)
        onAnalysisComplete(result.id)
    }
}

private fun averageBitmaps(bitmaps: List<Bitmap>): Bitmap {
    if (bitmaps.size == 1) return bitmaps[0]
    val w = bitmaps[0].width
    val h = bitmaps[0].height
    val n = bitmaps.size
    val totalPixels = w * h
    val sumR = IntArray(totalPixels)
    val sumG = IntArray(totalPixels)
    val sumB = IntArray(totalPixels)

    for (bitmap in bitmaps) {
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in 0 until totalPixels) {
            sumR[i] += android.graphics.Color.red(pixels[i])
            sumG[i] += android.graphics.Color.green(pixels[i])
            sumB[i] += android.graphics.Color.blue(pixels[i])
        }
        if (bitmap !== bitmaps[0]) bitmap.recycle()
    }

    val outPixels = IntArray(totalPixels)
    for (i in 0 until totalPixels) {
        outPixels[i] = android.graphics.Color.rgb(sumR[i] / n, sumG[i] / n, sumB[i] / n)
    }
    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    result.setPixels(outPixels, 0, w, 0, 0, w, h)
    return result
}

private fun cropNailRegion(bitmap: Bitmap): Bitmap {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height

    val cropWidth = (originalWidth * 0.6f).toInt()
    val cropHeight = (cropWidth * 1.33f).toInt()
    val cropX = (originalWidth - cropWidth) / 2
    val cropY = (originalHeight - cropHeight) / 2

    val safeX = maxOf(0, minOf(cropX, originalWidth - cropWidth))
    val safeY = maxOf(0, minOf(cropY, originalHeight - cropHeight))
    val safeWidth = minOf(cropWidth, originalWidth - safeX)
    val safeHeight = minOf(cropHeight, originalHeight - safeY)

    return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
}

private fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
    return try {
        val exifInterface = ExifInterface(path)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        var rotate = 0f
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270f
        }
        if (rotate != 0f) {
            matrix.postRotate(rotate)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    }
}

private suspend fun captureImageAsync(
    imageCapture: ImageCapture,
    context: Context,
    executor: Executor,
    index: Int
): File = kotlin.coroutines.suspendCoroutine { continuation ->
    val outputDirectory = context.cacheDir
    val photoFile = File(
        outputDirectory,
        "nail_capture_${System.currentTimeMillis()}_$index.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                continuation.resume(photoFile)
            }
            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        }
    )
}
