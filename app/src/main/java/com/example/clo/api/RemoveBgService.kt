package com.example.clo.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class RemoveBgService(private val context: Context) {

    private val client = OkHttpClient()
    private val REMOVE_BG_API_KEY = "YOUR_REMOVE_BG_API_KEY" // TODO: Replace with your actual API key

    fun removeBackground(imageFile: File, callback: (Result<File>) -> Unit) {
        if (!imageFile.exists()) {
            callback(Result.failure(FileNotFoundException("Image file not found: ${imageFile.absolutePath}")))
            return
        }

        // Check if file is empty
        if (imageFile.length() == 0L) {
            callback(Result.failure(IOException("Image file is empty: ${imageFile.absolutePath}")))
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image_file", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("size", "auto")
            .build()

        val request = Request.Builder()
            .url("https://api.remove.bg/v1.0/removebg")
            .addHeader("X-Api-Key", REMOVE_BG_API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("RemoveBgService", "Background removal API call failed", e)
                callback(Result.failure(e))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.bytes()
                        if (responseBody != null) {
                            try {
                                // Attempt to parse as JSON first (for errors or metadata)
                                val responseString = String(responseBody)
                                if (responseString.startsWith("{") && responseString.endsWith("}")) {
                                    val jsonObject = JSONObject(responseString)
                                    if (jsonObject.has("errors")) {
                                        val errors = jsonObject.getJSONArray("errors")
                                        val errorMessage = errors.optJSONObject(0)?.optString("title", "Unknown API error")
                                        Log.e("RemoveBgService", "Remove.bg API error: $errorMessage")
                                        callback(Result.failure(IOException("API Error: $errorMessage")))
                                        return
                                    }
                                }

                                // If not JSON or no errors, assume it's the image data
                                val outputStream = ByteArrayOutputStream()
                                outputStream.write(responseBody)
                                val noBgBitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())

                                if (noBgBitmap != null) {
                                    val outputDir = context.cacheDir
                                    val outputFile = File(outputDir, "no_bg_${System.currentTimeMillis()}.png")
                                    FileOutputStream(outputFile).use { fos ->
                                        noBgBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                    }
                                    callback(Result.success(outputFile))
                                } else {
                                    callback(Result.failure(IOException("Failed to decode image from response.")))
                                }
                            } catch (e: Exception) {
                                Log.e("RemoveBgService", "Error processing Remove.bg response", e)
                                callback(Result.failure(e))
                            }
                        } else {
                            callback(Result.failure(IOException("Remove.bg API response body is null.")))
                        }
                    } else {
                        val errorBody = response.body?.string()
                        Log.e("RemoveBgService", "Remove.bg API call failed: ${response.code} - $errorBody")
                        callback(Result.failure(IOException("API Call Failed: ${response.code} - $errorBody")))
                    }
                }
            }
        })
    }
}

class FileNotFoundException(message: String) : IOException(message) 