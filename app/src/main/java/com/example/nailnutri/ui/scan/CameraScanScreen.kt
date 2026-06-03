package com.example.nailnutri.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.analysis.GeminiAnalyzer
import com.example.nailnutri.data.DataRepository
import com.example.nailnutri.data.NailAnalysisResult
import com.example.nailnutri.data.NutrientDetail
import com.example.nailnutri.data.SufficientNutrientDetail
import com.example.nailnutri.theme.NutriGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    repository: DataRepository,
    onBackClick: () -> Unit,
    onAnalysisComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isMockMode by repository.isMockMode.collectAsStateWithLifecycle(initialValue = true)
    val apiKey by repository.apiKey.collectAsStateWithLifecycle(initialValue = "")

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.White)
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
                // Scanning view inside container
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
                // Request Permission Screen
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
                // Main Camera View with alignment guides
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Guidelines overlay
                    CameraOverlay(modifier = Modifier.fillMaxSize())

                    // Error text if fails
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

                    // Bottom controls panel
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Demo mode helper tip
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
                            // Empty box or Demo trigger
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

                            // Capture trigger
                            Surface(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isMockMode) {
                                            // Trigger random mock if click capture in mock mode
                                            triggerMockAnalysis(
                                                "healthy",
                                                repository,
                                                coroutineScope,
                                                onAnalysisComplete
                                            )
                                        } else {
                                            if (apiKey.isBlank()) {
                                                errorMessage =
                                                    "Gemini API 키가 없습니다. 설정에서 키를 등록하거나 데모 모드를 활성화해주세요."
                                                return@clickable
                                            }
                                            analyzing = true
                                            errorMessage = null
                                            captureImage(
                                                imageCapture,
                                                context,
                                                ContextCompat.getMainExecutor(context),
                                                onSuccess = { file ->
                                                    coroutineScope.launch {
                                                        try {
                                                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                                            val result = GeminiAnalyzer.analyzeNail(
                                                                bitmap = bitmap,
                                                                apiKey = apiKey,
                                                                imagePath = file.absolutePath
                                                            )
                                                            repository.saveResult(result)
                                                            onAnalysisComplete(result.id)
                                                        } catch (e: Exception) {
                                                            errorMessage = e.message ?: "분석 중 오류 발생"
                                                            analyzing = false
                                                        }
                                                    }
                                                },
                                                onError = { exc ->
                                                    errorMessage = "촬영 실패: ${exc.message}"
                                                    analyzing = false
                                                }
                                            )
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

                    // Demo select panel
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
                                    // Delay slightly to show visual scanning transition
                                    kotlinx.coroutines.delay(1500)
                                    triggerMockAnalysis(
                                        condition,
                                        repository,
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

        // Define card target dimensions
        val cardWidth = width * 0.5f
        val cardHeight = cardWidth * 1.5f
        val xOffset = (width - cardWidth) / 2
        val yOffset = (height - cardHeight) / 2

        // Draw transparent black around target area
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

        // Clip difference to overlay dim background
        val resultPath = Path.combine(
            androidx.compose.ui.graphics.PathOperation.Difference,
            outerPath,
            innerPath
        )

        drawPath(
            path = resultPath,
            color = Color.Black.copy(alpha = 0.5f)
        )

        // Draw green target borders
        drawRoundRect(
            color = NutriGreen,
            topLeft = androidx.compose.ui.geometry.Offset(xOffset, yOffset),
            size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
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
    repository: DataRepository,
    coroutineScope: CoroutineScope,
    onAnalysisComplete: (String) -> Unit
) {
    coroutineScope.launch {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val mockId = UUID.randomUUID().toString()

        val result = when (condition) {
            "healthy" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = "demo_healthy",
                symptoms = listOf("특이사항 없음 (건강함)"),
                deficientNutrients = emptyList(),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("단백질 (Keratin)", "손톱 표면이 매끄럽고 윤기가 있어 충분한 단백질 공급을 보입니다.", "손톱의 기본 구조 형성에 기여"),
                    SufficientNutrientDetail("아연 (Zinc)", "흰 반점이 관찰되지 않아 아연 수치가 양호해 보입니다.", "세포 분열 및 케라틴 합성 촉진"),
                    SufficientNutrientDetail("철분 (Iron)", "네일 베드 색상이 붉고 생기 있어 산소 공급이 원활합니다.", "산소 운반 및 건강한 세포 형성"),
                    SufficientNutrientDetail("비오틴 (Biotin)", "손톱 끝이 얇아지거나 부서지지 않고 탄탄합니다.", "손톱 두께 및 단단함 유지")
                ),
                overallAdvice = "축하합니다! 현재 손톱 상태는 매우 건강합니다. 현재의 균형 잡힌 영양 식단을 유지하고 건조해지지 않도록 가벼운 핸드크림 케어를 계속해주세요."
            )
            "white_spots" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = "demo_white_spots",
                symptoms = listOf("손톱 표면의 흰 반점 (Leukonychia)"),
                deficientNutrients = listOf(
                    NutrientDetail("아연 (Zinc)", "Severe", "손톱 중간에 산발적인 흰 반점이 나타나는 것은 세포 분열과 단백질 합성을 돕는 아연의 결핍을 강하게 시사합니다.", listOf("굴", "소고기", "호박씨", "아몬드")),
                    NutrientDetail("칼슘 (Calcium)", "Moderate", "가로방향의 흰색 띠가 발견되는 경우 칼슘 결핍이나 대사 스트레스 상태일 수 있습니다.", listOf("우유", "치즈", "멸치", "두부"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("비오틴", "손톱이 깨지거나 찢어지지 않아 기본 비오틴 공급은 원활합니다.", "손톱 구조 강화"),
                    SufficientNutrientDetail("철분", "네일 베드가 혈색이 좋아 심한 철분 부족은 아닙니다.", "혈액 생성")
                ),
                overallAdvice = "아연 결핍 증상인 흰 반점이 눈에 띕니다. 아연이 풍부한 견과류와 육류 섭취를 늘리고, 필요시 단기간 칼슘/아연 보충제 섭취를 고려해 볼 수 있습니다. 증상이 지속되면 의사와 상의하세요."
            )
            "vertical_ridges" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = "demo_vertical_ridges",
                symptoms = listOf("세로줄 현상 (Vertical Ridges)"),
                deficientNutrients = listOf(
                    NutrientDetail("비타민 B12", "Moderate", "세로 방향으로 깊게 파인 줄은 비타민 B12 결핍으로 인한 손톱 세포 성장 둔화를 나타낼 수 있습니다.", listOf("육류", "달걀", "연어", "유제품")),
                    NutrientDetail("마그네슘 (Magnesium)", "Moderate", "손톱 표면의 세로 홈과 울퉁불퉁함은 신체 대사를 돕는 마그네슘 부족과 연관될 수 있습니다.", listOf("시금치", "바나나", "아보카도", "다크 초콜릿"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("아연", "흰색 반점이나 가로 반점은 관찰되지 않아 아연 상태는 양호합니다.", "아연은 손톱 성장에 기여"),
                    SufficientNutrientDetail("단백질", "기본적인 손톱 강도는 잘 유지되고 있습니다.", "케라틴 합성")
                ),
                overallAdvice = "노화의 자연스러운 현상일 수도 있지만, 비타민 B12와 마그네슘 부족도 큰 원인이 됩니다. 잡곡밥, 녹색 채소, 그리고 적당한 고기류가 포함된 식단이 권장됩니다."
            )
            "spoon_nails" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = "demo_spoon_nails",
                symptoms = listOf("숟가락 모양 굽어짐 (Koilonychia)"),
                deficientNutrients = listOf(
                    NutrientDetail("철분 (Iron)", "Severe", "손톱 끝이 뒤집어지고 가운데가 움푹 파여 숟가락 모양을 띠는 현상은 대표적인 철분 결핍성 빈혈의 주요 증상입니다. 빠른 철분 보충이 필요합니다.", listOf("붉은 고기", "시금치", "렌틸콩", "조개류")),
                    NutrientDetail("단백질 (Protein)", "Moderate", "손톱 판이 얇아지고 쉽게 구부러지는 것은 기본 구성 물질인 단백질 부족을 뜻합니다.", listOf("닭가슴살", "달걀", "두부", "생선"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("칼슘", "손톱 표면이 전반적으로 깨끗하며 칼슘 분배는 양호합니다.", "골격 및 네일 강도 유지")
                ),
                overallAdvice = "숟가락 모양의 손톱(Koilonychia)은 심각한 철분 부족 빈혈의 신호일 수 있습니다. 붉은 고기와 녹색 잎채소를 다량 섭취하시고 철분의 흡수율을 높이기 위해 비타민 C와 함께 복용하는 것을 강력히 권장합니다."
            )
            "brittle" -> NailAnalysisResult(
                id = mockId,
                date = dateStr,
                imagePath = "demo_brittle",
                symptoms = listOf("손톱 갈라짐 및 깨짐 (Onychorrhexis)"),
                deficientNutrients = listOf(
                    NutrientDetail("비오틴 (Biotin)", "Severe", "손톱이 메마르고 건조하며 끝부분이 갈라지고 부서지는 증상은 각질 구조를 단단히 하는 비오틴(비타민 B7) 결핍과 관련이 큽니다.", listOf("계란 노른자", "견과류", "콜리플라워", "고구마")),
                    NutrientDetail("수분/필수지방산", "Moderate", "손톱의 탄력을 잃고 껍질처럼 벗겨지는 현상은 필수 지방산(오메가-3) 및 수분 부족 증상입니다.", listOf("들기름", "연어", "호두", "물 하루 8잔"))
                ),
                sufficientNutrients = listOf(
                    SufficientNutrientDetail("철분", "빈혈 증상이 없고 네일 베드의 혈색은 건강한 핑크빛을 띱니다.", "산소 활성 공급")
                ),
                overallAdvice = "갈라지고 건조한 손톱은 비오틴과 지방산 공급이 최우선입니다. 계란이나 아몬드를 매일 드셔 보시고 네일 오일이나 핸드크림을 자주 발라 외부 수분을 공급해 주는 것도 아주 중요합니다."
            )
            else -> throw IllegalArgumentException("Unknown mock condition")
        }

        repository.saveResult(result)
        onAnalysisComplete(result.id)
    }
}
