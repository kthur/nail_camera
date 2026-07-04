package com.example.nailnutri.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads the current model version from an asset file and constructs the model asset name.
 * Expected asset: `model_version.txt` containing a version string like "v2".
 * If the file is missing or empty, falls back to the default model name `model_v2.tflite`.
 */
object ModelVersionProvider {
    private const val VERSION_FILE = "model_version.txt"
    private const val DEFAULT_MODEL_NAME = "model_v2.tflite"

    /** Returns the full asset path for the TFLite model, e.g. "model_v2.tflite" */
    fun getModelPath(context: Context): String {
        return try {
            val input = context.assets.open(VERSION_FILE)
            val version = BufferedReader(InputStreamReader(input)).use { it.readLine()?.trim() }
            if (version.isNullOrEmpty()) {
                DEFAULT_MODEL_NAME
            } else {
                "model_$${"$"}version.tflite"
            }
        } catch (e: Exception) {
            // If the version file cannot be read, use the default model name.
            DEFAULT_MODEL_NAME
        }
    }
}
