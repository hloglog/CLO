package com.example.clo

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.clo.databinding.ActivityClosetBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import com.google.firebase.firestore.Query

class ClosetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClosetBinding
    private lateinit var topButton: Button
    private lateinit var bottomButton: Button
    private lateinit var shoesButton: Button
    private lateinit var accessoriesButton: Button

    private lateinit var clothesRecyclerView: RecyclerView
    private var currentPhotoPath: String? = null
    private var pendingAction: (() -> Unit)? = null
    private var isActivityActive = true
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var progressDialog: ProgressDialog? = null
    private var removeBgService: RemoveBgService? = null
    private var clothingClassifier: ClothingClassifier? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    private lateinit var clothesAdapter: ClothesAdapter
    private var currentCategory: String = "상의" // 기본값
    
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            currentPhotoPath?.let { path ->
                processSelectedImage(File(path))
            }
        }
    }
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Convert URI to File
                val file = createTempFileFromUri(uri)
                if (file != null) {
                    processSelectedImage(file)
                } else {
                    Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    companion object {
        private const val PERMISSIONS_REQUEST = 100
        private const val TAG = "ClosetActivity"
        private const val REMOVE_BG_API_KEY = "YOUR_REMOVE_BG_API_KEY" // TODO: Replace with your API key
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClosetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        try {
            initializeViews()
            initializeServices()
            setupBottomNavigation()
            setupCategoryButtons()
            setupRecyclerView()
            loadClothes(currentCategory)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "초기화 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeServices() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        removeBgService = RemoveBgService(this)
        progressDialog = ProgressDialog(this)
    }

    private fun initializeClassifierAsync() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                clothingClassifier = ClothingClassifier(this@ClosetActivity)
                Log.d(TAG, "Classifier initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing classifier", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ClosetActivity,
                        "이미지 분류기 초기화 실패: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun initializeViews() {
        // Initialize views
        topButton = binding.topButton
        bottomButton = binding.bottomButton
        shoesButton = binding.shoesButton
        accessoriesButton = binding.accessoriesButton
        clothesRecyclerView = binding.clothesRecyclerView

        // Set up category buttons
        setupCategoryButtons()

        // Initialize RecyclerView
        clothesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        // TODO: Set adapter when you have data to display

        // Set up add button click listener
        binding.addButton.setOnClickListener {
            if (isActivityActive) {
                showImageSourceDialog()
            }
        }
    }

    private fun processSelectedImage(imageFile: File) {
        if (isFinishing || isDestroyed) return

        // Show loading dialog for background removal
        showLoadingDialog("누끼 따는 중...")

        // Step 1: Remove background
        removeBgService?.removeBackground(imageFile) { result ->
            result.fold(
                onSuccess = { processedFile ->
                    // Step 2: Classify clothing
                    if (isFinishing || isDestroyed) return@fold
                    runOnUiThread {
                        showLoadingDialog("옷을 분류하는 중...")
                    }
                    
                    try {
                        // Initialize classifier if needed
                        if (clothingClassifier == null) {
                            clothingClassifier = ClothingClassifier(this@ClosetActivity)
                        }

                        val bitmap = BitmapFactory.decodeFile(processedFile.absolutePath)
                        if (bitmap == null) {
                            throw IllegalStateException("이미지를 불러올 수 없습니다.")
                        }
                        
                        val category = clothingClassifier?.classify(bitmap)
                            ?: throw IllegalStateException("이미지 분류를 실패했습니다.")
                        
                        // Step 3: Upload to Firebase
                        if (!isFinishing && !isDestroyed) {
                            uploadToFirebase(processedFile, category)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during image processing", e)
                        runOnUiThread {
                            dismissDialogSafely(progressDialog)
                            if (!isFinishing && !isDestroyed) {
                                val errorMessage = when (e) {
                                    is IllegalStateException -> e.message ?: "옷 분류 중 오류가 발생했습니다."
                                    else -> "이미지 처리 중 오류가 발생했습니다: ${e.message}"
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Background removal failed", exception)
                    runOnUiThread {
                        dismissDialogSafely(progressDialog)
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this, "배경 제거 중 오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    private fun uploadToFirebase(imageFile: File, category: String) {
        if (isFinishing || isDestroyed) return

        // Log file information
        Log.d(TAG, "Uploading file: ${imageFile.absolutePath}, exists=${imageFile.exists()}, length=${imageFile.length()} bytes")

        // Check if file exists and is readable
        if (!imageFile.exists() || !imageFile.canRead()) {
            Log.e(TAG, "File not accessible: ${imageFile.absolutePath}")
            runOnUiThread {
                dismissDialogSafely(progressDialog)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, "이미지 파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        // Check if file is empty (API error case)
        if (imageFile.length() == 0L) {
            Log.e(TAG, "File is empty: ${imageFile.absolutePath}")
            runOnUiThread {
                dismissDialogSafely(progressDialog)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        runOnUiThread {
            showLoadingDialog("옷장에 추가하는 중...")
        }
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            runOnUiThread {
                dismissDialogSafely(progressDialog)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        // Create storage reference
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageRef = storage.reference
        val imageRef = storageRef.child("users/$userId/$category/${timestamp}.png")

        // Convert File to Uri using Uri.fromFile
        val imageUri = Uri.fromFile(imageFile)
        
        // Log additional information
        Log.d(TAG, "File URI: $imageUri")
        Log.d(TAG, "Storage path: users/$userId/$category/${timestamp}.png")

        // Upload file
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "Image upload successful")
                // Get download URL
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d(TAG, "Download URL obtained: $downloadUri")
                    
                    // Create clothing document
                    val clothingItem = hashMapOf(
                        "userId" to userId,
                        "category" to category,
                        "imageUrl" to downloadUri.toString(),
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    // Save to Firestore
                    db.collection("clothes")
                        .add(clothingItem)
                        .addOnSuccessListener { documentRef ->
                            Log.d(TAG, "Firestore document created successfully with ID: ${documentRef.id}")
                            // Refresh the current category view
                            loadClothes(category)
                            runOnUiThread {
                                dismissDialogSafely(progressDialog)
                                if (!isFinishing && !isDestroyed) {
                                    Toast.makeText(this, "옷장에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Firestore document creation failed", e)
                            runOnUiThread {
                                dismissDialogSafely(progressDialog)
                                if (!isFinishing && !isDestroyed) {
                                    Toast.makeText(this, "데이터베이스 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Image upload failed", e)
                runOnUiThread {
                    dismissDialogSafely(progressDialog)
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_image", ".png", cacheDir)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file from URI", e)
            null
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = binding.bottomNavigationView
        bottomNavigationView.selectedItemId = R.id.menu_mypage

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
        clothingClassifier?.close()
        coroutineScope.cancel()
        dismissDialogSafely(progressDialog)
        progressDialog = null
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

    private fun showDialogSafely(dialog: ProgressDialog) {
        if (!isFinishing && !isDestroyed && !dialog.isShowing) {
            dialog.show()
        }
    }

    private fun dismissDialogSafely(dialog: ProgressDialog?) {
        try {
            if (!isFinishing && !isDestroyed && dialog?.isShowing == true) {
                dialog.dismiss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing dialog", e)
        }
    }

    private fun showLoadingDialog(message: String) {
        if (!isFinishing && !isDestroyed) {
            if (progressDialog == null) {
                progressDialog = ProgressDialog(this)
            }
            progressDialog?.setMessage(message)
            progressDialog?.show()
        }
    }

    private fun setupRecyclerView() {
        clothesAdapter = ClothesAdapter()
        binding.clothesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ClosetActivity, 2)
            adapter = clothesAdapter
        }
    }

    private fun setupCategoryButtons() {
        binding.topButton.setOnClickListener {
            updateCategorySelection("상의")
            loadClothes("상의")
        }
        
        binding.bottomButton.setOnClickListener {
            updateCategorySelection("하의")
            loadClothes("하의")
        }
        
        binding.shoesButton.setOnClickListener {
            updateCategorySelection("신발")
            loadClothes("신발")
        }
        
        binding.accessoriesButton.setOnClickListener {
            updateCategorySelection("기타")
            loadClothes("기타")
        }
    }

    private fun updateCategorySelection(selectedCategory: String) {
        currentCategory = selectedCategory
        // 버튼 스타일 업데이트
        binding.topButton.setBackgroundResource(if (selectedCategory == "상의") R.drawable.button_selected else R.drawable.button_normal)
        binding.bottomButton.setBackgroundResource(if (selectedCategory == "하의") R.drawable.button_selected else R.drawable.button_normal)
        binding.shoesButton.setBackgroundResource(if (selectedCategory == "신발") R.drawable.button_selected else R.drawable.button_normal)
        binding.accessoriesButton.setBackgroundResource(if (selectedCategory == "기타") R.drawable.button_selected else R.drawable.button_normal)
    }

    private fun loadClothes(category: String) {
        val userId = auth.currentUser?.uid ?: return
        
        showLoadingDialog("옷장을 불러오는 중...")
        
        Log.d(TAG, "Loading clothes for category: $category")
        
        db.collection("clothes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Successfully loaded ${documents.size()} clothes")
                
                val clothesList = documents.mapNotNull { doc ->
                    val imageUrl = doc.getString("imageUrl")
                    if (imageUrl != null) {
                        Log.d(TAG, "Found image URL: $imageUrl")
                        ClothingItem(
                            id = doc.id,
                            imageUrl = imageUrl,
                            category = category
                        )
                    } else {
                        Log.e(TAG, "Image URL is null for document: ${doc.id}")
                        null
                    }
                }
                
                Log.d(TAG, "Processed ${clothesList.size} valid clothing items")
                clothesAdapter.submitList(clothesList)
                dismissDialogSafely(progressDialog)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading clothes", e)
                dismissDialogSafely(progressDialog)
                Toast.makeText(this, "옷장을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    data class ClothingItem(
        val id: String,
        val imageUrl: String,
        val category: String
    )
} 