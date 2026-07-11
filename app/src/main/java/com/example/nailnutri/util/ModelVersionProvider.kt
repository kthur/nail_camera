package com.example.nailnutri.util

import android.content.Context

/**
 * Returns the filename of the TFLite model asset.
 * The Gradle task `trainNailModel` generates `nail_classifier.tflite`.
 */
object ModelVersionProvider {
    private const val MODEL_NAME = "nail_classifier.tflite"

    fun getModelPath(context: Context): String = MODEL_NAME
}
