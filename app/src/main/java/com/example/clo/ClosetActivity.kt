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
    private lateinit var topButton: com.google.android.material.button.MaterialButton
    private lateinit var bottomButton: com.google.android.material.button.MaterialButton
    private lateinit var shoesButton: com.google.android.material.button.MaterialButton
    private lateinit var accessoriesButton: com.google.android.material.button.MaterialButton
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

        // Set up category buttons
        setupCategoryButtons()

        // Initialize RecyclerView
        clothesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        // TODO: Set adapter when you have data to display

        // Set up add button click listener
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.addButton).setOnClickListener {
            if (isActivityActive) {
                showImageSourceDialog()
            }
        }
    }

    private fun setupCategoryButtons() {
        // 초기 상태: 상의 선택
        selectCategory("상의")
        
        topButton.setOnClickListener { selectCategory("상의") }
        bottomButton.setOnClickListener { selectCategory("하의") }
        shoesButton.setOnClickListener { selectCategory("신발") }
        accessoriesButton.setOnClickListener { selectCategory("기타") }
    }

    private fun selectCategory(category: String) {
        // 모든 버튼을 기본 스타일로 초기화
        resetAllButtons()
        
        when (category) {
            "상의" -> {
                topButton.setBackgroundColor(resources.getColor(R.color.primary_color, theme))
                topButton.setTextColor(resources.getColor(R.color.white, theme))
            }
            "하의" -> {
                bottomButton.setBackgroundColor(resources.getColor(R.color.primary_color, theme))
                bottomButton.setTextColor(resources.getColor(R.color.white, theme))
            }
            "신발" -> {
                shoesButton.setBackgroundColor(resources.getColor(R.color.primary_color, theme))
                shoesButton.setTextColor(resources.getColor(R.color.white, theme))
            }
            "기타" -> {
                accessoriesButton.setBackgroundColor(resources.getColor(R.color.primary_color, theme))
                accessoriesButton.setTextColor(resources.getColor(R.color.white, theme))
            }
        }
    }

    private fun resetAllButtons() {
        val buttons = listOf(topButton, bottomButton, shoesButton, accessoriesButton)
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(R.color.white, theme))
            button.setTextColor(resources.getColor(R.color.text_secondary, theme))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_home

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_search -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("fragment", "search")
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("fragment", "home")
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_mypage -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("fragment", "profile")
                    startActivity(intent)
                    finish()
                    true
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
} 