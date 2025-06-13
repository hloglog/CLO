package com.example.clo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.Query
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class ClosetFragment : Fragment() {
    private lateinit var topButton: MaterialButton
    private lateinit var bottomButton: MaterialButton
    private lateinit var shoesButton: MaterialButton
    private lateinit var accessoriesButton: MaterialButton
    private lateinit var clothesRecyclerView: RecyclerView
    private lateinit var clothesAdapter: ClothesAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var clothingClassifier: ClothingClassifier? = null
    
    private var currentPhotoPath: String? = null
    private var isFragmentActive = true
    
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the camera photo
            currentPhotoPath?.let { path ->
                // Process the captured image
                Toast.makeText(requireContext(), "이미지가 저장되었습니다: $path", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle the selected image
                Log.d(TAG, "Image selected from gallery: $uri")
                processSelectedImage(uri)
            }
        }
    }
    
    companion object {
        private const val PERMISSIONS_REQUEST = 100
        private const val TAG = "ClosetFragment"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        
        // ClothingClassifier 초기화
        try {
            clothingClassifier = ClothingClassifier(requireContext())
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ClothingClassifier", e)
            clothingClassifier = null
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_closet, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            // Firebase 연결 상태 확인
            Log.d(TAG, "Firebase Auth instance: ${auth}")
            Log.d(TAG, "Firebase Firestore instance: ${db}")
            Log.d(TAG, "Current user: ${auth.currentUser}")
            Log.d(TAG, "Current user UID: ${auth.currentUser?.uid}")
            
            initializeViews(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "앱 초기화 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews(view: View) {
        // Initialize views
        topButton = view.findViewById(R.id.topButton)
        bottomButton = view.findViewById(R.id.bottomButton)
        shoesButton = view.findViewById(R.id.shoesButton)
        accessoriesButton = view.findViewById(R.id.accessoriesButton)
        clothesRecyclerView = view.findViewById(R.id.clothesRecyclerView)

        // Set up category buttons
        setupCategoryButtons()

        // Initialize RecyclerView with adapter (클릭 리스너 연결)
        clothesAdapter = ClothesAdapter { item ->
            showDeleteDialog(item)
        }
        clothesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        clothesRecyclerView.adapter = clothesAdapter

        // Set up add button click listener
        view.findViewById<FloatingActionButton>(R.id.addButton).setOnClickListener {
            if (isFragmentActive) {
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
                topButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                topButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
            "하의" -> {
                bottomButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                bottomButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
            "신발" -> {
                shoesButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                shoesButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
            "기타" -> {
                accessoriesButton.setBackgroundColor(resources.getColor(R.color.black, requireContext().theme))
                accessoriesButton.setTextColor(resources.getColor(R.color.white, requireContext().theme))
            }
        }
        
        // 선택된 카테고리의 옷을 로드
        loadClothes(category)
    }

    private fun resetAllButtons() {
        val buttons = listOf(topButton, bottomButton, shoesButton, accessoriesButton)
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(R.color.white, requireContext().theme))
            button.setTextColor(resources.getColor(R.color.black, requireContext().theme))
        }
    }

    override fun onResume() {
        super.onResume()
        isFragmentActive = true
    }

    override fun onPause() {
        super.onPause()
        isFragmentActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isFragmentActive = false
        
        // ClothingClassifier 정리
        try {
            clothingClassifier?.close()
            clothingClassifier = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ClothingClassifier", e)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(requireContext())
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST)
        } else {
            launchCamera()
        }
    }

    private fun checkGalleryPermissionAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상에서는 READ_MEDIA_IMAGES 권한 사용
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSIONS_REQUEST)
            } else {
                launchGallery()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6 이상에서는 READ_EXTERNAL_STORAGE 권한 사용
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST)
            } else {
                launchGallery()
            }
        } else {
            launchGallery()
        }
    }

    private fun launchCamera() {
        val photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "이미지 파일 생성 실패", Toast.LENGTH_SHORT).show()
            return
        }

        currentPhotoPath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can now access camera/gallery
                    Toast.makeText(requireContext(), "권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
                    // 권한이 허용된 후 갤러리 실행
                    if (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) || 
                        permissions.contains(Manifest.permission.READ_MEDIA_IMAGES)) {
                        launchGallery()
                    } else if (permissions.contains(Manifest.permission.CAMERA)) {
                        launchCamera()
                    }
                } else {
                    Toast.makeText(requireContext(), "권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadClothes(category: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not logged in, cannot load clothes")
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            // 빈 리스트로 어댑터 업데이트
            clothesAdapter.submitList(emptyList())
            return
        }
        
        Log.d(TAG, "Loading clothes for category: $category, userId: $userId")
        
        // Firestore 연결 테스트
        db.collection("test").document("test").get()
            .addOnSuccessListener { document ->
                Log.d(TAG, "Firestore connection test successful")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore connection test failed", e)
            }
        
        // 로딩 상태 표시
        Toast.makeText(requireContext(), "옷장을 불러오는 중...", Toast.LENGTH_SHORT).show()
        
        // 먼저 전체 clothes 컬렉션을 확인
        db.collection("clothes")
            .get()
            .addOnSuccessListener { allDocuments ->
                Log.d(TAG, "Total clothes documents in collection: ${allDocuments.size()}")
                allDocuments.forEach { doc ->
                    Log.d(TAG, "Document ID: ${doc.id}, Data: ${doc.data}")
                }
                
                // 이제 사용자별 쿼리 실행 - 단순한 쿼리로 변경
                db.collection("clothes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { userDocuments ->
                        Log.d(TAG, "User clothes query - Successfully loaded ${userDocuments.size()} clothes for user: $userId")
                        
                        // 클라이언트 사이드에서 카테고리 필터링
                        val filteredDocuments = userDocuments.filter { doc ->
                            val docCategory = doc.getString("category")
                            docCategory == category
                        }
                        
                        Log.d(TAG, "Filtered to ${filteredDocuments.size} clothes for category: $category")
                        
                        val clothesList = filteredDocuments.mapNotNull { doc ->
                            val imageUrl = doc.getString("imageUrl")
                            val docUserId = doc.getString("userId")
                            val docCategory = doc.getString("category")
                            
                            Log.d(TAG, "Document ${doc.id}: userId=$docUserId, category=$docCategory, imageUrl=$imageUrl")
                            
                            if (imageUrl != null) {
                                Log.d(TAG, "Found image URL: $imageUrl")
                                ClosetActivity.ClothingItem(
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
                        
                        if (clothesList.isEmpty()) {
                            Toast.makeText(requireContext(), "이 카테고리에 등록된 옷이 없습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "${clothesList.size}개의 옷을 불러왔습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error loading user clothes", e)
                        Toast.makeText(requireContext(), "옷장을 불러오는데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                        // 에러 시 빈 리스트로 어댑터 업데이트
                        clothesAdapter.submitList(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading all clothes", e)
                Toast.makeText(requireContext(), "옷장을 불러오는데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                // 에러 시 빈 리스트로 어댑터 업데이트
                clothesAdapter.submitList(emptyList())
            }
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            Log.d(TAG, "Processing selected image: $uri")
            
            // URI를 File로 변환
            val imageFile = createTempFileFromUri(uri)
            if (imageFile == null) {
                Toast.makeText(requireContext(), "이미지 파일을 생성할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 현재 선택된 카테고리 가져오기
            val currentCategory = when {
                topButton.currentTextColor == resources.getColor(R.color.white, requireContext().theme) -> "상의"
                bottomButton.currentTextColor == resources.getColor(R.color.white, requireContext().theme) -> "하의"
                shoesButton.currentTextColor == resources.getColor(R.color.white, requireContext().theme) -> "신발"
                accessoriesButton.currentTextColor == resources.getColor(R.color.white, requireContext().theme) -> "기타"
                else -> "상의" // 기본값
            }
            
            // 로딩 다이얼로그 표시
            showLoadingDialog("누끼를 따는 중...")
            
            // RemoveBgService를 사용하여 배경 제거
            val removeBgService = RemoveBgService(requireContext())
            removeBgService.removeBackground(imageFile) { result ->
                result.fold(
                    onSuccess = { processedFile ->
                        Log.d(TAG, "Background removal successful: ${processedFile.absolutePath}")
                        
                        try {
                            // ClothingClassifier를 사용하여 이미지 분류
                            val category = if (clothingClassifier != null) {
                                val bitmap = android.graphics.BitmapFactory.decodeFile(processedFile.absolutePath)
                                if (bitmap != null) {
                                    val classifiedCategory = clothingClassifier!!.classify(bitmap)
                                    Log.d(TAG, "Image classified as: $classifiedCategory")
                                    classifiedCategory
                                } else {
                                    Log.e(TAG, "Failed to decode processed image")
                                    currentCategory // 폴백으로 현재 선택된 카테고리 사용
                                }
                            } else {
                                Log.w(TAG, "ClothingClassifier not available, using current category")
                                currentCategory // 분류기가 없으면 현재 선택된 카테고리 사용
                            }
                            
                            // Firebase에 업로드
                            uploadToFirebase(processedFile, category)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during image processing", e)
                            activity?.runOnUiThread {
                                dismissLoadingDialog()
                                Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Background removal failed", exception)
                        activity?.runOnUiThread {
                            dismissLoadingDialog()
                            Toast.makeText(requireContext(), "배경 제거 중 오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing selected image", e)
            Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_image", ".png", requireContext().cacheDir)
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
    
    private fun showLoadingDialog(message: String) {
        // 메인 스레드에서만 Toast 표시
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun dismissLoadingDialog() {
        // 로딩 다이얼로그 숨기기 (필요시 구현)
    }
    
    private fun uploadToFirebase(imageFile: File, category: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            activity?.runOnUiThread {
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("clothes/$userId/${System.currentTimeMillis()}.png")

        val uploadTask = imageRef.putFile(android.net.Uri.fromFile(imageFile))
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { exception -> throw exception }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val clothesData = hashMapOf(
                    "userId" to userId,
                    "imageUrl" to downloadUri.toString(),
                    "category" to category,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                db.collection("clothes").add(clothesData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Document added with ID: ${documentReference.id}")
                        activity?.runOnUiThread {
                            dismissLoadingDialog()
                            Toast.makeText(requireContext(), "옷이 성공적으로 저장되었습니다!", Toast.LENGTH_SHORT).show()
                            
                            // 분류된 카테고리 탭으로 자동 이동
                            switchToCategory(category)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error adding document", e)
                        activity?.runOnUiThread {
                            dismissLoadingDialog()
                            Toast.makeText(requireContext(), "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Log.e(TAG, "Upload failed", task.exception)
                activity?.runOnUiThread {
                    dismissLoadingDialog()
                    Toast.makeText(requireContext(), "업로드 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun switchToCategory(category: String) {
        // 해당 카테고리 탭으로 이동
        selectCategory(category)
        
        // 사용자에게 알림
        val categoryName = when (category) {
            "상의" -> "상의"
            "하의" -> "하의" 
            "신발" -> "신발"
            "기타" -> "기타"
            else -> category
        }
        
        Toast.makeText(requireContext(), "분류 결과: $categoryName 탭으로 이동했습니다", Toast.LENGTH_LONG).show()
    }

    private fun showDeleteDialog(item: ClosetActivity.ClothingItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("이 옷을 정말 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteClothingItem(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteClothingItem(item: ClosetActivity.ClothingItem) {
        // Firestore에서 삭제
        db.collection("clothes").document(item.id).delete()
            .addOnSuccessListener {
                // Storage에서 이미지 삭제
                val storageRef = Firebase.storage.getReferenceFromUrl(item.imageUrl)
                storageRef.delete().addOnSuccessListener {
                    // RecyclerView에서 제거
                    val newList = clothesAdapter.currentList.toMutableList()
                    newList.remove(item)
                    clothesAdapter.submitList(newList)
                    Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "스토리지 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "DB 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 