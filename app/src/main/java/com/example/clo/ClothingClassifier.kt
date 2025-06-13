package com.example.clo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.FloatBuffer

class ClothingClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private val imageSize = 224
    
    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            Log.d("ClothingClassifier", "Starting classifier setup...")
            
            val model = FileUtil.loadMappedFile(context, "clothing_classifier.tflite")
            interpreter = Interpreter(model)
            
            isInitialized = true
            Log.d("ClothingClassifier", "Classifier initialized successfully")
        } catch (e: Exception) {
            Log.e("ClothingClassifier", "Error initializing classifier", e)
            isInitialized = false
            throw IllegalStateException("모델 초기화 실패: ${e.message}")
        }
    }

    fun classify(image: Bitmap): String {
        if (!isInitialized || interpreter == null) {
            throw IllegalStateException("분류기가 초기화되지 않았습니다.")
        }

        try {
            // Preprocess the image
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(imageSize, imageSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))  // Normalize to [0, 1]
                .build()

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
            
            // Prepare output buffer
            val outputBuffer = FloatBuffer.allocate(4)  // Assuming 4 classes
            
            // Run inference
            interpreter?.run(tensorImage.buffer, outputBuffer)
            
            // Process results
            outputBuffer.rewind()
            val results = FloatArray(4)
            outputBuffer.get(results)
            
            // Find the class with highest probability
            var maxIndex = 0
            var maxProb = results[0]
            for (i in 1 until results.size) {
                if (results[i] > maxProb) {
                    maxProb = results[i]
                    maxIndex = i
                }
            }
            
            // Map the result to our categories
            return when (maxIndex) {
                0 -> "상의"
                1 -> "하의"
                2 -> "신발"
                else -> "기타"
            }
        } catch (e: Exception) {
            Log.e("ClothingClassifier", "Error during classification", e)
            throw IllegalStateException("이미지 분류 중 오류 발생: ${e.message}")
        }
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("ClothingClassifier", "Error closing classifier", e)
        }
    }
} 