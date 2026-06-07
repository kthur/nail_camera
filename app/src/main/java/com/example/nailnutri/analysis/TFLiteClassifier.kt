package com.example.nailnutri.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object TFLiteClassifier {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isLoaded = false

    fun load(context: Context): Boolean {
        if (isLoaded) return true
        try {
            val modelBuffer = loadModelFile(context)
            interpreter = Interpreter(modelBuffer)
            labels = loadLabels(context)
            isLoaded = true
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun isAvailable(): Boolean = isLoaded

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val interp = interpreter ?: return emptyList()
        val inputImage = resizeBitmap(bitmap, 224, 224)
        val inputBuffer = bitmapToByteBuffer(inputImage)
        val output = Array(1) { FloatArray(labels.size.coerceAtLeast(1)) }
        interp.run(inputBuffer, output)
        val probs = output[0]
        return labels.mapIndexed { i, label -> label to probs[i] }
            .sortedByDescending { it.second }
    }

    fun getTopPrediction(bitmap: Bitmap): Pair<String, Float> {
        val results = classify(bitmap)
        return if (results.isNotEmpty()) results.first() else ("healthy" to 0f)
    }

    fun mapToCondition(rawLabel: String): String {
        return when {
            rawLabel.contains("Healthy", ignoreCase = true) -> "healthy"
            rawLabel.contains("Onychomycosis", ignoreCase = true) -> "white_spots"
            rawLabel.contains("Psoriasis", ignoreCase = true) ||
            rawLabel.contains("Pitting", ignoreCase = true) -> "vertical_ridges"
            rawLabel.contains("Onychogryphosis", ignoreCase = true) -> "spoon_nails"
            else -> "brittle"
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val afd = context.assets.openFd("nail_classifier.tflite")
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
            listOf("healthy", "white_spots", "vertical_ridges", "spoon_nails", "brittle")
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputChannels = 3
        val inputSize = 224
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * inputChannels)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val rf = (r - 127.5f) / 127.5f
            val gf = (g - 127.5f) / 127.5f
            val bf = (b - 127.5f) / 127.5f
            byteBuffer.putFloat(rf)
            byteBuffer.putFloat(gf)
            byteBuffer.putFloat(bf)
        }
        byteBuffer.rewind()
        return byteBuffer
    }
}
