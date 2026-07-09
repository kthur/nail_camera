package com.example.nailnutri.ui.sensor

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.nailnutri.analysis.SleepAudioAnalyzer
import com.example.nailnutri.data.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAudioScreen(
    repository: DataRepository,
    onBackClick: () -> Unit,
    onAnalysisComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }
    var currentDb by remember { mutableFloatStateOf(30f) }

    val audioHistory = remember { mutableStateListOf<Float>() }
    val recordBufferList = remember { mutableListOf<Short>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("수면 숨소리/코골이 분석", fontWeight = FontWeight.Bold, color = Color.White) },
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = if (isRecording) "수면 호흡 음질 분석 중... (${countdown}초)" else "침대 옆 협탁에 기기를 거치하고\n5초간 호흡 또는 코골이 소리를 측정해 보세요",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (audioHistory.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                val w = size.width
                                val h = size.height
                                val path = Path()
                                
                                val stepX = w / 20f
                                val centerY = h / 2f
                                
                                path.moveTo(0f, centerY)
                                val pointsToDraw = audioHistory.takeLast(20)
                                for (i in pointsToDraw.indices) {
                                    val x = i * stepX
                                    val volumeMultiplier = (pointsToDraw[i] / 80f).coerceIn(0.05f, 0.95f)
                                    val amplitude = centerY * volumeMultiplier
                                    
                                    if (i % 2 == 0) {
                                        path.lineTo(x, centerY - amplitude)
                                    } else {
                                        path.lineTo(x, centerY + amplitude)
                                    }
                                }
                                path.lineTo(w, centerY)
                                
                                drawPath(
                                    path = path,
                                    color = Color(0xFFC084FC),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        } else {
                            Text(
                                text = "숨소리 음색 수집 대기 중",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                if (isRecording) {
                    Text(
                        text = "실시간 음량: ${String.format(Locale.US, "%.1f", currentDb)} dB",
                        color = Color(0xFFC084FC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        isRecording = true
                        countdown = 5
                        audioHistory.clear()
                        recordBufferList.clear()
                        
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val sampleRate = 8000
                                val bufferSize = AudioRecord.getMinBufferSize(
                                    sampleRate,
                                    AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT
                                ).coerceAtLeast(1024)
                                
                                val audioRecord = try {
                                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        AudioRecord(
                                            MediaRecorder.AudioSource.MIC,
                                            sampleRate,
                                            AudioFormat.CHANNEL_IN_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT,
                                            bufferSize
                                        )
                                    } else { null }
                                } catch (e: Exception) { null }
                                
                                val buffer = ShortArray(bufferSize)
                                audioRecord?.startRecording()
                                
                                val sampleLoops = 25
                                for (loop in 0 until sampleLoops) {
                                    if (!isRecording) break
                                    
                                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                                    if (read > 0) {
                                        var sumSq = 0.0
                                        for (i in 0 until read) {
                                            recordBufferList.add(buffer[i])
                                            sumSq += (buffer[i].toDouble() * buffer[i].toDouble())
                                        }
                                        
                                        val rms = sqrt(sumSq / read)
                                        val db = (20 * log10(rms + 1e-5)).coerceIn(10.0, 100.0).toFloat()
                                        currentDb = db
                                        
                                        withContext(Dispatchers.Main) {
                                            audioHistory.add(db)
                                        }
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
                                val res = SleepAudioAnalyzer.analyzeAudioBuffer(recordBufferList.toShortArray(), "sleep_snoring_record.wav")
                                repository.saveResult(res)
                                res.id
                            }
                            onAnalysisComplete(resultId)
                        }
                    },
                    enabled = !isRecording,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC084FC))
                ) {
                    Text(
                        text = if (isRecording) "진단 음향 채집 중..." else "5초 꿀잠 수면 분석 체험",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
