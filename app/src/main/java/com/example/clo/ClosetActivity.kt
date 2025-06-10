package com.example.clo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ClosetActivity : AppCompatActivity() {
    private lateinit var topButton: Button
    private lateinit var bottomButton: Button
    private lateinit var shoesButton: Button
    private lateinit var accessoriesButton: Button
    private lateinit var clothesRecyclerView: RecyclerView
    private var currentPhotoPath: String? = null
    private var pendingAction: (() -> Unit)? = null
    private var isActivityActive = true
    
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle the camera photo
            currentPhotoPath?.let { path ->
                // Process the captured image
                Toast.makeText(this, "이미지가 저장되었습니다: $path", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle the selected image
                Toast.makeText(this, "이미지가 선택되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val PERMISSIONS_REQUEST = 100
        private const val TAG = "ClosetActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_closet)
        try {
            initializeViews()
            setupBottomNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "앱 초기화 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        // Initialize views
        topButton = findViewById(R.id.topButton)
        bottomButton = findViewById(R.id.bottomButton)
        shoesButton = findViewById(R.id.shoesButton)
        accessoriesButton = findViewById(R.id.accessoriesButton)
        clothesRecyclerView = findViewById(R.id.clothesRecyclerView)

        // Set underline for category buttons
        topButton.paintFlags = topButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        bottomButton.paintFlags = bottomButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        shoesButton.paintFlags = shoesButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        accessoriesButton.paintFlags = accessoriesButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Initialize RecyclerView
        clothesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        // TODO: Set adapter when you have data to display

        // Set up add button click listener
        findViewById<Button>(R.id.addButton).setOnClickListener {
            if (isActivityActive) {
                showImageSourceDialog()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_mypage

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (!isActivityActive) return@setOnItemSelectedListener false
            
            when (item.itemId) {
                R.id.menu_home -> {
                    try {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to Home", e)
                        Toast.makeText(this, "화면 전환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.menu_mypage -> {
                    try {
                        startActivity(Intent(this, MyPageActivity::class.java))
                        finish()
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to MyPage", e)
                        Toast.makeText(this, "화면 전환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityActive = false
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(this)
            .setTitle("이미지 선택")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> checkGalleryPermissionAndLaunch()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingAction = { launchCamera() }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_REQUEST
            )
        } else {
            launchCamera()
        }
    }

    private fun checkGalleryPermissionAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                pendingAction = { launchGallery() }
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSIONS_REQUEST
                )
            } else {
                launchGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingAction = { launchGallery() }
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST
                )
            } else {
                launchGallery()
            }
        }
    }

    private fun launchCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e(TAG, "Error occurred while creating the File", ex)
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.clo.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingAction?.invoke()
            } else {
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
            pendingAction = null
        }
    }

    private fun resetAllUnderlines() {
        topButton.paintFlags = topButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        bottomButton.paintFlags = bottomButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        shoesButton.paintFlags = shoesButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        accessoriesButton.paintFlags = accessoriesButton.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
    }
} 