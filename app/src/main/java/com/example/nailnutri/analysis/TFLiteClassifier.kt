package com.example.nailnutri.analysis

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import com.example.nailnutri.util.ModelVersionProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object TFLiteClassifier {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isLoaded = false

    fun load(context: Context): Boolean {
        if (isLoaded && interpreter != null) return true
        try {
            val modelPath = ModelVersionProvider.getModelPath(context)
            val modelBuffer = loadModelFile(context, modelPath)
            
            // Try loading with GPU Delegate first, fall back to multi-threaded CPU if GPU delegate fails
            var loadedInterp: Interpreter? = null
            try {
                val gpuOptions = Interpreter.Options().apply {
                    addDelegate(GpuDelegate())
                    setNumThreads(4)
                }
                loadedInterp = Interpreter(modelBuffer, gpuOptions)
            } catch (e: Exception) {
                // Fallback to CPU interpreter
                val cpuOptions = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                loadedInterp = Interpreter(modelBuffer, cpuOptions)
            }
            
            interpreter = loadedInterp
            labels = loadLabels(context)
            isLoaded = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            isLoaded = false
            return false
        }
    }

    fun isAvailable(): Boolean = isLoaded && interpreter != null

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val interp = interpreter ?: return emptyList()
        try {
            // Dynamically inspect model input and output tensor shapes
            val inputTensor = interp.getInputTensor(0)
            val inputShape = inputTensor.shape() // [1, height, width, channels]
            val h = if (inputShape.size >= 4) inputShape[1] else 150
            val w = if (inputShape.size >= 4) inputShape[2] else 200

            val outputTensor = interp.getOutputTensor(0)
            val outputShape = outputTensor.shape() // [1, numClasses]
            val numClasses = if (outputShape.size >= 2) outputShape[1] else labels.size.coerceAtLeast(1)

            val inputImage = resizeBitmap(bitmap, w, h)
            val inputBuffer = bitmapToByteBuffer(inputImage, w, h)
            val output = Array(1) { FloatArray(numClasses) }

            interp.run(inputBuffer, output)
            val probs = output[0]

            val effectiveLabels = if (labels.size == numClasses) {
                labels
            } else {
                // Fallback label mapping matching python train script defaults
                val defaultLabels = listOf("NORMAL", "ONYCHOMYCOSIS", "PALLOR", "DISCOLORATION", "UNKNOWN")
                if (defaultLabels.size == numClasses) defaultLabels else List(numClasses) { "Class_$it" }
            }

            return effectiveLabels.mapIndexed { i, label -> label to probs[i] }
                .sortedByDescending { it.second }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getTopPrediction(bitmap: Bitmap): Pair<String, Float> {
        val results = classify(bitmap)
        return if (results.isNotEmpty()) results.first() else ("healthy" to 0f)
    }

    fun mapToCondition(rawLabel: String): String {
        return when {
            rawLabel.contains("Healthy", ignoreCase = true) || rawLabel.contains("NORMAL", ignoreCase = true) -> "healthy"
            rawLabel.contains("Melanonychia", ignoreCase = true) -> "pigmentation"
            rawLabel.contains("Nail_Lichen_Planus", ignoreCase = true) -> "vertical_ridges"
            rawLabel.contains("Onychomycosis", ignoreCase = true) -> "white_spots"
            rawLabel.contains("Psoriasis", ignoreCase = true) ||
            rawLabel.contains("Pitting", ignoreCase = true) -> "vertical_ridges"
            rawLabel.contains("Onychogryphosis", ignoreCase = true) -> "spoon_nails"
            rawLabel.contains("PALLOR", ignoreCase = true) -> "spoon_nails"
            rawLabel.contains("DISCOLORATION", ignoreCase = true) -> "white_spots"
            else -> "brittle"
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val afd = context.assets.openFd(modelPath)
        val inputStream = afd.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = afd.startOffset
        val declaredLength = afd.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context): List<String> {
        return try {
            context.assets.open("nail_classifier.txt")
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            listOf("NORMAL", "ONYCHOMYCOSIS", "PALLOR", "DISCOLORATION", "UNKNOWN")
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap, width: Int, height: Int): ByteBuffer {
        val inputChannels = 3
        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * inputChannels)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val rf = r / 255.0f
            val gf = g / 255.0f
            val bf = b / 255.0f
            byteBuffer.putFloat(rf)
            byteBuffer.putFloat(gf)
            byteBuffer.putFloat(bf)
        }
        byteBuffer.rewind()
        return byteBuffer
    }
}
