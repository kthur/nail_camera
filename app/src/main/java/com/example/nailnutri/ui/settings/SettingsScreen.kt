package com.example.nailnutri.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nailnutri.data.DataRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: DataRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiKey by repository.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val isMockMode by repository.isMockMode.collectAsStateWithLifecycle(initialValue = true)
    val useGemma by repository.useGemma.collectAsStateWithLifecycle(initialValue = false)
    val gemmaModelPath by repository.gemmaModelPath.collectAsStateWithLifecycle(initialValue = "/data/local/tmp/gemma.bin")
    
    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var mockEnabled by remember(isMockMode) { mutableStateOf(isMockMode) }
    var gemmaActive by remember(useGemma) { mutableStateOf(useGemma) }
    var modelPathInput by remember(gemmaModelPath) { mutableStateOf(gemmaModelPath) }
    var keyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testingConnection by remember { mutableStateOf(false) }
    var modelValidationResult by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Mode Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "데모 분석 모드 (Mock Mode)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "활성화 시 실제 카메라 촬영 대신 사전에 수집된 네일 샘플(흰반점, 세로줄 등)을 선택해 테스트해볼 수 있습니다. 에뮬레이터에서 유용합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = mockEnabled,
                            onCheckedChange = {
                                mockEnabled = it
                                coroutineScope.launch {
                                    repository.setMockMode(it)
                                    snackbarHostState.showSnackbar(
                                        if (it) "데모 모드가 활성화되었습니다." else "실제 API 분석 모드가 활성화되었습니다."
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Gemma On-Device Switch Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "온디바이스 Gemma LLM 사용",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "활성화 시 인터넷 연결 없이 기기에서 Gemma 모델을 사용하여 진단 피드백을 생성합니다. 비활성화 시 클라우드 Gemini API를 사용합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = gemmaActive,
                            onCheckedChange = {
                                gemmaActive = it
                                coroutineScope.launch {
                                    repository.setUseGemma(it)
                                    snackbarHostState.showSnackbar(
                                        if (it) "온디바이스 Gemma 분석이 설정되었습니다." else "클라우드 Gemini 분석이 설정되었습니다."
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (!gemmaActive) {
                // Gemini API Key Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Gemini API 설정",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "손톱의 영양 분석(실시간 이미지 인식)을 위해 Google AI Studio의 Gemini API 키가 필요합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(
                                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "비밀번호 보이기 토글"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.setApiKey(inputKey)
                                        snackbarHostState.showSnackbar("API 키가 저장되었습니다.")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("키 저장")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (inputKey.isBlank()) {
                                        testResult = "API 키를 입력해주세요."
                                        return@OutlinedButton
                                    }
                                    testingConnection = true
                                    testResult = null
                                    coroutineScope.launch {
                                        try {
                                            val model = com.google.ai.client.generativeai.GenerativeModel(
                                                modelName = "gemini-1.5-flash",
                                                apiKey = inputKey
                                            )
                                            val response = model.generateContent("Say 'Connection Successful' in Korean in 5 words.")
                                            testResult = response.text?.trim() ?: "응답이 비어있습니다."
                                        } catch (e: Exception) {
                                            testResult = "연결 실패: ${e.localizedMessage}"
                                        } finally {
                                            testingConnection = false
                                        }
                                    }
                                },
                                enabled = !testingConnection,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (testingConnection) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text("연결 테스트")
                                }
                            }
                        }

                        testResult?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (result.contains("실패") || result.contains("입력")) 
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                                        else 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (result.contains("실패") || result.contains("입력"))
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                // Gemma On-Device Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "온디바이스 Gemma 모델 설정",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "로컬 추론을 위해 Gemma 모델 파일(.bin)이 기기에 업로드되어 있어야 합니다. (추천: gemma-2b-it CPU/GPU)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = modelPathInput,
                            onValueChange = { modelPathInput = it },
                            label = { Text("Gemma 모델 로컬 경로") },
                            placeholder = { Text("/data/local/tmp/gemma.bin") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Downloader Section
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Text(
                            text = "모델 파일 자동 다운로드",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = "Gemma 2B-it 모델(약 1.4GB)을 원격 서버로부터 앱 내부 저장소로 다운로드합니다. 대용량 파일이므로 Wi-Fi 연결을 권장합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        if (isDownloading) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "모델 다운로드 중...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f%%", downloadProgress * 100f),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    isDownloading = true
                                    downloadProgress = 0f
                                    downloadError = null
                                    coroutineScope.launch {
                                        downloadGemmaModel(
                                            context = context,
                                            onProgress = { progress ->
                                                downloadProgress = progress
                                            },
                                            onComplete = { absolutePath ->
                                                isDownloading = false
                                                modelPathInput = absolutePath
                                                coroutineScope.launch {
                                                    repository.setGemmaModelPath(absolutePath)
                                                    snackbarHostState.showSnackbar("Gemma 모델 다운로드 및 경로 저장 완료!")
                                                }
                                            },
                                            onError = { errorMsg ->
                                                isDownloading = false
                                                downloadError = errorMsg
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Gemma 모델 자동 다운로드 시작")
                            }
                        }

                        downloadError?.let { error ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "다운로드 실패: $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.setGemmaModelPath(modelPathInput)
                                        snackbarHostState.showSnackbar("Gemma 모델 경로가 저장되었습니다.")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("경로 저장")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (modelPathInput.isBlank()) {
                                        modelValidationResult = "경로를 먼저 입력해주세요."
                                        return@OutlinedButton
                                    }
                                    val file = java.io.File(modelPathInput)
                                    modelValidationResult = if (file.exists()) {
                                        val lengthGb = file.length().toDouble() / 1024.0 / 1024.0 / 1024.0
                                        "파일 확인 성공! 크기: " + String.format(java.util.Locale.US, "%.2f", lengthGb) + " GB"
                                    } else {
                                        "파일을 찾을 수 없습니다.\n입력 경로: " + file.absolutePath + "\n(참고: 'adb push gemma-2b-it-gpu.bin /data/local/tmp/gemma.bin' 명령으로 파일을 밀어 넣을 수 있습니다.)"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("파일 검증")
                            }
                        }

                        modelValidationResult?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (result.contains("성공")) 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                        else 
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (result.contains("성공"))
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "안내 및 의료 면책 조항",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "본 어플리케이션은 손톱의 상태와 일반적인 영양소 결핍과의 연관 정보만을 제공합니다. 이는 의학적 자가진단이나 처방이 아니므로 자세한 건강 상담은 전문 의사나 영양사와 상의하십시오.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun downloadGemmaModel(
    context: android.content.Context,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    val targetFile = File(context.filesDir, "gemma.bin")
    // Google Storage public dataset direct URL for Gemma-2b-it CPU-int4 model
    val urlString = "https://storage.googleapis.com/jmstore/jm-gemma-2b-it-cpu-int4.bin"
    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("서버 응답 오류 (HTTP ${connection.responseCode})")
        }

        val fileLength = connection.contentLengthLong
        val input = connection.inputStream
        val output = FileOutputStream(targetFile)

        val data = ByteArray(16384) // 16KB buffer
        var total: Long = 0
        var count: Int
        
        while (input.read(data).also { count = it } != -1) {
            total += count
            if (fileLength > 0) {
                onProgress(total.toFloat() / fileLength.toFloat())
            }
            output.write(data, 0, count)
        }

        output.flush()
        output.close()
        input.close()
        onComplete(targetFile.absolutePath)
    } catch (e: java.io.IOException) {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        onError("네트워크 다운로드 실패: ${e.localizedMessage ?: "I/O Error"}")
    } catch (e: Exception) {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        onError(e.localizedMessage ?: "다운로드 중 알 수 없는 오류 발생")
    }
}
