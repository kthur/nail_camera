package com.example.nailnutri.ui.sensor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.nailnutri.analysis.VoiceAnalyzer
import com.example.nailnutri.data.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAnalysisScreen(
    repository: DataRepository,
    onBackClick: () -> Unit,
    onAnalysisComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }
    var voiceSignalProgress by remember { mutableFloatStateOf(0f) }
    var recognizedText by remember { mutableStateOf("") }
    var customInputText by remember { mutableStateOf("") }

    val recordBufferList = remember { mutableListOf<Short>() }

    // Quick Symptom Chips
    val symptomChips = listOf(
        "눈밑이 떨리고 너무 피곤해요",
        "손톱이 잘 갈라지고 부러져요",
        "입안이 헐고 자주 입병이 납니다",
        "어지럽고 손톱에 흰 반점이 생겼어요",
        "관절이 쑤시고 뼈가 약해진 느낌이에요"
    )

    val startSpeechRecognition = remember(context) {
        { onResult: (String) -> Unit ->
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toString())
                }
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {}
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                recognizer.startListening(intent)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("음성 증상 인식 & 영양 진단", fontWeight = FontWeight.Bold, color = Color.White) },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Voice Nutrition Scanner",
                            color = Color(0xFFFBBF24),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "평소 느끼는 신체 상태나 증상을 음성으로 말씀해 주시면, 음향 떨림과 증상 자연어를 분석하여 필요한 결핍 영양소를 찾아드립니다.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Voice Recording Interactive Hub
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(if (isRecording) 160.dp + (30 * voiceSignalProgress).dp else 150.dp)
                            .clip(CircleShape),
                        color = Color(0xFFFBBF24).copy(alpha = if (isRecording) 0.2f else 0.08f)
                    ) {}

                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        color = Color(0xFFFBBF24).copy(alpha = 0.3f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Mic",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = if (isRecording) "${countdown}초" else "말하기",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Subtitle / Recognized Text Display Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "실시간 인식된 증상 문장",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (recognizedText.isNotBlank()) "\"$recognizedText\"" else if (isRecording) "음성을 듣고 있습니다..." else "버튼을 누르고 증상을 편하게 말씀하세요.",
                            color = if (recognizedText.isNotBlank()) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action Recording Button
                Button(
                    onClick = {
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@Button
                        }

                        isRecording = true
                        countdown = 5
                        recordBufferList.clear()
                        voiceSignalProgress = 0.2f
                        recognizedText = ""

                        startSpeechRecognition { resultText ->
                            recognizedText = resultText
                        }

                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val sampleRate = 8000
                                val bufferSize = AudioRecord.getMinBufferSize(
                                    sampleRate,
                                    AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT
                                ).coerceAtLeast(1024)

                                val audioRecord = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    AudioRecord(
                                        MediaRecorder.AudioSource.MIC,
                                        sampleRate,
                                        AudioFormat.CHANNEL_IN_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        bufferSize
                                    )
                                } else { null }

                                val buffer = ShortArray(bufferSize)
                                audioRecord?.startRecording()

                                for (loop in 0 until 25) {
                                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                                    if (read > 0) {
                                        for (i in 0 until read) {
                                            recordBufferList.add(buffer[i])
                                        }

                                        var absSum = 0f
                                        for (i in 0 until read) {
                                            absSum += kotlin.math.abs(buffer[i].toFloat())
                                        }
                                        voiceSignalProgress = (absSum / (read * 8000f)).coerceIn(0.1f, 1.0f)
                                    }
                                    delay(200)
                                    if (loop % 5 == 0 && loop > 0) {
                                        countdown--
                                    }
                                }

                                audioRecord?.stop()
                                audioRecord?.release()
                            }

                            isRecording = false
                            val resultId = withContext(Dispatchers.IO) {
                                val res = VoiceAnalyzer.analyzeVoiceWithText(
                                    recognizedText = recognizedText,
                                    audioSamples = recordBufferList.toShortArray(),
                                    imagePath = "voice_ai_analysis.wav"
                                )
                                repository.saveResult(res)
                                res.id
                            }
                            onAnalysisComplete(resultId)
                        }
                    },
                    enabled = !isRecording,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24))
                ) {
                    Text(
                        text = if (isRecording) "음성 및 신호 분석 중..." else "음성 녹음 시작하기",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                // Quick Symptom Chips Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "빠른 증상 선택 (1-Tap 음성 진단)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    symptomChips.forEach { chipText ->
                        SuggestionChip(
                            onClick = {
                                recognizedText = chipText
                                scope.launch {
                                    val resultId = withContext(Dispatchers.IO) {
                                        val res = VoiceAnalyzer.analyzeVoiceWithText(
                                            recognizedText = chipText,
                                            audioSamples = null,
                                            imagePath = "voice_quick_chip.wav"
                                        )
                                        repository.saveResult(res)
                                        res.id
                                    }
                                    onAnalysisComplete(resultId)
                                }
                            },
                            label = { Text(chipText, color = Color.White, fontSize = 13.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF1E293B))
                        )
                    }
                }

                // Fallback Text Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        placeholder = { Text("증상을 직접 텍스트로 입력하셔도 됩니다", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFBBF24),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    IconButton(
                        onClick = {
                            if (customInputText.isNotBlank()) {
                                scope.launch {
                                    val resultId = withContext(Dispatchers.IO) {
                                        val res = VoiceAnalyzer.analyzeVoiceWithText(
                                            recognizedText = customInputText,
                                            audioSamples = null,
                                            imagePath = "voice_text_input.wav"
                                        )
                                        repository.saveResult(res)
                                        res.id
                                    }
                                    onAnalysisComplete(resultId)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFBBF24))
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "전송", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
