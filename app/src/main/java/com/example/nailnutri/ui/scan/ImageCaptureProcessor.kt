package com.example.nailnutri.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ImageCaptureProcessor {

    suspend fun captureImageAsync(
        imageCapture: ImageCapture,
        context: Context,
        executor: Executor,
        index: Int
    ): File = suspendCoroutine { continuation ->
        val outputDirectory = context.cacheDir
        val photoFile = File(
            outputDirectory,
            "nail_capture_${System.currentTimeMillis()}_$index.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    continuation.resume(photoFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    fun captureImage(
        imageCapture: ImageCapture,
        context: Context,
        executor: Executor,
        onSuccess: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val photoFile = File(
            context.cacheDir,
            "nail_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSuccess(photoFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    suspend fun processBurstCaptures(files: List<File>): Bitmap = withContext(Dispatchers.Default) {
        val scaledBitmaps = mutableListOf<Bitmap>()
        for (file in files) {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            val targetSize = 640
            val sampleSize = when {
                maxDim > targetSize * 4 -> 8
                maxDim > targetSize * 2 -> 4
                maxDim > targetSize -> 2
                else -> 1
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: continue
            val rotatedBitmap = rotateBitmapIfRequired(rawBitmap, file.absolutePath)
            if (rotatedBitmap != rawBitmap) rawBitmap.recycle()
            val croppedBitmap = cropNailRegion(rotatedBitmap)
            if (croppedBitmap != rotatedBitmap) rotatedBitmap.recycle()
            val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 200, 150, true)
            if (scaledBitmap != croppedBitmap) croppedBitmap.recycle()
            scaledBitmaps.add(scaledBitmap)
        }
        if (scaledBitmaps.isEmpty()) {
            throw Exception("이미지 디코딩에 실패했습니다. 다시 시도해 주세요.")
        }
        averageBitmaps(scaledBitmaps)
    }

    fun averageBitmaps(bitmaps: List<Bitmap>): Bitmap {
        if (bitmaps.size == 1) return bitmaps[0]
        val w = bitmaps[0].width
        val h = bitmaps[0].height
        val n = bitmaps.size
        val totalPixels = w * h
        val sumR = IntArray(totalPixels)
        val sumG = IntArray(totalPixels)
        val sumB = IntArray(totalPixels)

        for (bitmap in bitmaps) {
            val pixels = IntArray(totalPixels)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            for (i in 0 until totalPixels) {
                sumR[i] += android.graphics.Color.red(pixels[i])
                sumG[i] += android.graphics.Color.green(pixels[i])
                sumB[i] += android.graphics.Color.blue(pixels[i])
            }
            if (bitmap !== bitmaps[0]) bitmap.recycle()
        }

        val outPixels = IntArray(totalPixels)
        for (i in 0 until totalPixels) {
            outPixels[i] = android.graphics.Color.rgb(sumR[i] / n, sumG[i] / n, sumB[i] / n)
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            result.setPixels(outPixels, 0, w, 0, 0, w, h)
        } catch (e: Throwable) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    result.setPixel(x, y, outPixels[y * w + x])
                }
            }
        }
        return result
    }

    fun cropNailRegion(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val cropWidth = (originalWidth * 0.6f).toInt()
        val cropHeight = (cropWidth * 1.33f).toInt()
        val cropX = (originalWidth - cropWidth) / 2
        val cropY = (originalHeight - cropHeight) / 2

        val safeX = maxOf(0, minOf(cropX, originalWidth - cropWidth))
        val safeY = maxOf(0, minOf(cropY, originalHeight - cropHeight))
        val safeWidth = minOf(cropWidth, originalWidth - safeX)
        val safeHeight = minOf(cropHeight, originalHeight - safeY)

        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
    }

    fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
        return try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            var rotate = 0f
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270f
            }
            if (rotate != 0f) {
                matrix.postRotate(rotate)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    fun cleanupBurstFiles(files: List<File>, keepIndex: Int = 0) {
        for (i in files.indices) {
            if (i != keepIndex) {
                try {
                    if (files[i].exists()) {
                        files[i].delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cleanupAllFiles(files: List<File>) {
        cleanupBurstFiles(files, keepIndex = -1)
    }
}
