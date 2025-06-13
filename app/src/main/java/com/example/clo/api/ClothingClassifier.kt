package com.example.clo.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ClothingClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val modelPath = "clothing_classifier.tflite" // TODO: Ensure this model file exists in assets
    private val labels = arrayOf("상의", "하의", "신발", "기타") // TODO: Adjust labels based on your model

    init {
        try {
            interpreter = Interpreter(loadModelFile(context, modelPath))
            Log.d("ClothingClassifier", "TFLite model loaded successfully.")
        } catch (e: Exception) {
            Log.e("ClothingClassifier", "Failed to load TFLite model: ${e.message}")
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(bitmap: Bitmap): String {
        if (interpreter == null) {
            Log.e("ClothingClassifier", "Interpreter is not initialized.")
            return "알 수 없음" // Or throw an exception
        }

        // Preprocess the bitmap (resize, normalize pixels)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true) // Example size, adjust to your model's input
        val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3) // Float32 for 3 channels
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat((((`val` shr 16 and 0xFF) - 127.5f) / 127.5f))
                byteBuffer.putFloat((((`val` shr 8 and 0xFF) - 127.5f) / 127.5f))
                byteBuffer.putFloat((((`val` and 0xFF) - 127.5f) / 127.5f))
            }
        }

        // Run inference
        val output = Array(1) { FloatArray(labels.size) }
        interpreter?.run(byteBuffer, output)

        // Post-process the output to get the class with highest probability
        var maxConfidence = -1.0f
        var predictedCategory = "알 수 없음"

        for (i in labels.indices) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i]
                predictedCategory = labels[i]
            }
        }
        Log.d("ClothingClassifier", "Classified as: $predictedCategory with confidence $maxConfidence")
        return predictedCategory
    }

    fun close() {
        interpreter?.close()
    }
} 