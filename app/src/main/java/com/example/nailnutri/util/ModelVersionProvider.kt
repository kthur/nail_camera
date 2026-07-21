package com.example.nailnutri.util

import android.content.Context

/**
 * Returns the filename of the TFLite model asset.
 * Reads model_version.txt to determine whether to use model_v2.tflite or nail_classifier.tflite.
 */
object ModelVersionProvider {
    fun getModelPath(context: Context): String {
        return try {
            val version = context.assets.open("model_version.txt").bufferedReader().use { it.readText().trim() }
            if (version.isNotEmpty()) {
                val fullPath = "model_$version.tflite"
                // Verify the asset exists
                context.assets.openFd(fullPath).close()
                fullPath
            } else {
                "nail_classifier.tflite"
            }
        } catch (e: Exception) {
            "nail_classifier.tflite"
        }
    }
}
