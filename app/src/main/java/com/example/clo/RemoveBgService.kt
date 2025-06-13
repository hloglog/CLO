package com.example.clo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RemoveBgService(private val context: Context) {
    companion object {
        private const val TAG = "RemoveBgService"
        private const val API_KEY = "4TJ9YWaadYC772j6ePwD7LxP"
        private const val API_URL = "https://api.remove.bg/v1.0/removebg"
    }

    private val client = OkHttpClient()

    fun removeBackground(imageFile: File, callback: (Result<File>) -> Unit) {
        if (!imageFile.exists() || !imageFile.canRead()) {
            callback(Result.failure(IOException("이미지 파일을 읽을 수 없습니다.")))
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image_file", imageFile.name, imageFile.asRequestBody("image/*".toMediaType()))
            .addFormDataPart("size", "auto")
            .build()

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("X-Api-Key", API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API call failed", e)
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "API error: $errorBody")
                    callback(Result.failure(IOException("API 오류: ${response.code}")))
                    return
                }

                val responseBody = response.body?.bytes()
                if (responseBody == null) {
                    Log.e(TAG, "Null response from API")
                    callback(Result.failure(IOException("API 응답이 null입니다.")))
                    return
                }

                if (responseBody.isEmpty()) {
                    Log.e(TAG, "Empty response from API")
                    callback(Result.failure(IOException("API 응답이 비어있습니다.")))
                    return
                }

                Log.d(TAG, "Received response from API: ${responseBody.size} bytes")

                try {
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode response as bitmap")
                        callback(Result.failure(IOException("이미지 디코딩 실패")))
                        return
                    }

                    val outputFile = saveProcessedImage(bitmap, context)
                    if (outputFile == null) {
                        Log.e(TAG, "Failed to save processed image")
                        callback(Result.failure(IOException("이미지 저장 실패")))
                        return
                    }

                    // Verify the saved file
                    if (!outputFile.exists() || outputFile.length() == 0L) {
                        Log.e(TAG, "Saved file is invalid: exists=${outputFile.exists()}, length=${outputFile.length()}")
                        outputFile.delete()
                        callback(Result.failure(IOException("저장된 이미지 파일이 유효하지 않습니다.")))
                        return
                    }

                    Log.d(TAG, "Successfully processed image: ${outputFile.absolutePath}, size=${outputFile.length()} bytes")
                    callback(Result.success(outputFile))
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing API response", e)
                    callback(Result.failure(e))
                }
            }
        })
    }

    private fun saveProcessedImage(bitmap: Bitmap, context: Context): File? {
        return try {
            // Create a unique filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "processed_${timestamp}.png"
            
            // Create output file in app's private directory
            val outputFile = File(context.filesDir, filename)
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            // Save bitmap to file
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            
            // Verify file was created and is readable
            if (!outputFile.exists() || !outputFile.canRead()) {
                Log.e(TAG, "Failed to save processed image: File not accessible")
                return null
            }
            
            // Verify file size
            if (outputFile.length() == 0L) {
                Log.e(TAG, "Failed to save processed image: File is empty")
                outputFile.delete()
                return null
            }
            
            Log.d(TAG, "Successfully saved processed image: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving processed image", e)
            null
        }
    }


} 