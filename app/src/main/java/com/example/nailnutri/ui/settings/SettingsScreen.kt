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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: DataRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apiKey by repository.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val isMockMode by repository.isMockMode.collectAsStateWithLifecycle(initialValue = true)
    
    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var mockEnabled by remember(isMockMode) { mutableStateOf(isMockMode) }
    var keyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testingConnection by remember { mutableStateOf(false) }

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
