package com.example.nailnutri.ui.sensor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.nailnutri.analysis.VoiceAnalyzer
import com.example.nailnutri.data.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var countdown by remember { mutableIntStateOf(3) }
    var voiceSignalProgress by remember { mutableFloatStateOf(0f) }

    val recordBufferList = remember { mutableListOf<Short>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3초 발성 신경 피로도 분석", fontWeight = FontWeight.Bold, color = Color.White) },
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
            if (!hasAudioPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "마이크 권한 필요",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "3초 아- 발성 음성 주파수 변동(만성 피로)을 분석하기 위해 오디오 녹음 접근 권한이 반드시 필요합니다.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24))
                    ) {
                        Text("마이크 권한 허용하기", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("설정 앱으로 이동하여 직접 권한 허용", color = Color.White)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "3초 목소리 피치 분석",
                            color = Color(0xFFFBBF24),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "마이크 가까이 입을 대고,\n아-- 소리를 3초간 편안하게 내주세요.",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(if (isRecording) 140.dp + (20 * voiceSignalProgress).dp else 140.dp)
                                .clip(CircleShape),
                            color = Color(0xFFFBBF24).copy(alpha = if (isRecording) 0.15f else 0.08f)
                        ) {}
                        Surface(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape),
                            color = Color(0xFFFBBF24).copy(alpha = 0.25f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isRecording) "${countdown}초" else "준비",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isRecording = true
                            countdown = 3
                            recordBufferList.clear()
                            voiceSignalProgress = 0.2f
                            
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
                                    
                                    for (loop in 0 until 15) {
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
                                    val res = VoiceAnalyzer.analyzeVoice(recordBufferList.toShortArray(), "voice_fatigue_record.wav")
                                    repository.saveResult(res)
                                    res.id
                                }
                                onAnalysisComplete(resultId)
                            }
                        },
                        enabled = !isRecording,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24))
                    ) {
                        Text(
                            text = if (isRecording) "성대 파장 취득 중..." else "성대 마이크 측정 시작",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
