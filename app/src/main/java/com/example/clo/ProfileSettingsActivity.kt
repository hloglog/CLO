package com.example.clo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var editTextName: EditText
    private lateinit var editTextId: EditText
    private lateinit var buttonLogout: Button
    private lateinit var buttonDone: Button
    private lateinit var imageProfile: ImageView
    private val TAG = "ProfileSettings"
    
    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100

    // 갤러리에서 이미지 선택을 위한 ActivityResultLauncher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            uploadImageToFirebase(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        auth = Firebase.auth
        db = Firebase.firestore
        storage = Firebase.storage

        // 뷰 바인딩
        editTextName = findViewById(R.id.edit_text_name)
        editTextId = findViewById(R.id.edit_text_id)
        buttonLogout = findViewById(R.id.button_logout)
        buttonDone = findViewById(R.id.button_complete)
        imageProfile = findViewById(R.id.image_profile)

        // 현재 사용자 정보 로드
        loadUserProfile()

        // 이미지 수정 버튼 클릭 리스너
        findViewById<Button>(R.id.button_change_image).setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // 로그아웃 버튼 클릭 리스너
        buttonLogout.setOnClickListener {
            auth.signOut()

            // SharedPreferences에 저장된 로그인 상태 삭제
            val editor = getSharedPreferences("login_status", Context.MODE_PRIVATE).edit()
            editor.putBoolean("is_logged_in", false)
            editor.apply()

            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // 완료 버튼 클릭 리스너
        buttonDone.setOnClickListener {
            try {
                val newName = editTextName.text.toString().trim()
                
                if (newName.isEmpty()) {
                    Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Log.d(TAG, "이름 업데이트 시작: $newName")

                // 버튼 비활성화 및 로딩 상태 표시
                buttonDone.isEnabled = false
                buttonDone.text = "저장 중..."

                // 현재 로그인된 사용자의 ID 가져오기
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "사용자 ID가 null입니다")
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    buttonDone.isEnabled = true
                    buttonDone.text = "완료"
                    return@setOnClickListener
                }

                Log.d(TAG, "사용자 ID: $userId")

                // Firestore에 사용자 정보 업데이트
                db.collection("users").document(userId)
                    .update("name", newName)
                    .addOnSuccessListener {
                        Log.d(TAG, "이름 업데이트 성공")
                        Toast.makeText(this, "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                        buttonDone.isEnabled = true
                        buttonDone.text = "완료"
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "이름 업데이트 실패", e)
                        Toast.makeText(this, "프로필 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        buttonDone.isEnabled = true
                        buttonDone.text = "완료"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "예외 발생", e)
                Toast.makeText(this, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                buttonDone.isEnabled = true
                buttonDone.text = "완료"
            }
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        
        // 로딩 상태 표시
        Toast.makeText(this, "이미지 업로드 중...", Toast.LENGTH_SHORT).show()
        
        // Firebase Storage에 업로드할 파일 경로 생성
        val fileName = "profile_images/${userId}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)
        
        // 이미지 업로드
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // 업로드 성공 시 다운로드 URL 가져오기
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Firestore에 프로필 이미지 URL 저장
                    saveProfileImageUrl(downloadUri.toString())
                }.addOnFailureListener { e ->
                    Log.e(TAG, "다운로드 URL 가져오기 실패", e)
                    Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "이미지 업로드 실패", e)
                Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileImageUrl(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener {
                Log.d(TAG, "프로필 이미지 URL 저장 성공")
                Toast.makeText(this, "프로필 이미지가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                
                // 이미지뷰에 새 이미지 표시
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .into(imageProfile)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 이미지 URL 저장 실패", e)
                Toast.makeText(this, "프로필 이미지 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        // 사용자 ID는 이메일에서 추출
        val userEmail = auth.currentUser?.email ?: return
        val userIdFromEmail = userEmail.split("@")[0]
        editTextId.setText(userIdFromEmail)
        editTextId.isEnabled = false // ID는 수정 불가능

        // Firestore에서 사용자 정보 로드
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val profileImageUrl = document.getString("profileImageUrl")
                    
                    editTextName.setText(name ?: userIdFromEmail) // 이름이 없으면 이메일 ID 사용
                    
                    // 프로필 이미지 로드
                    if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.default_profile_image)
                            .error(R.drawable.default_profile_image)
                            .into(imageProfile)
                    } else {
                        imageProfile.setImageResource(R.drawable.default_profile_image)
                    }
                } else {
                    Log.d(TAG, "사용자 문서가 존재하지 않음")
                    editTextName.setText(userIdFromEmail)
                    imageProfile.setImageResource(R.drawable.default_profile_image)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 정보 로드 실패", e)
                Toast.makeText(this, 
                    "프로필 정보 로드 실패: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                editTextName.setText(userIdFromEmail)
                imageProfile.setImageResource(R.drawable.default_profile_image)
            }
    }
} 