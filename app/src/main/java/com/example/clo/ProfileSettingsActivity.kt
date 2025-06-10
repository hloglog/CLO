package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var editTextName: EditText
    private lateinit var editTextId: EditText
    private lateinit var buttonLogout: Button
    private lateinit var buttonDone: Button
    private val TAG = "ProfileSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        auth = Firebase.auth
        db = Firebase.firestore

        // 뷰 바인딩
        editTextName = findViewById(R.id.edit_text_name)
        editTextId = findViewById(R.id.edit_text_id)
        buttonLogout = findViewById(R.id.button_logout)
        buttonDone = findViewById(R.id.button_complete)

        // 현재 사용자 정보 로드
        loadUserProfile()

        // 이미지 수정 버튼 클릭 리스너
        findViewById<Button>(R.id.button_change_image).setOnClickListener {
            // TODO: 이미지 수정 로직 구현 (갤러리 열기 등)
            Toast.makeText(this, "이미지 수정 클릭", Toast.LENGTH_SHORT).show()
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

        // 하단 네비게이션 바 설정
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.menu_mypage

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_search -> {
                    Toast.makeText(this, "설정에서 검색 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_mypage -> true
                else -> false
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
                    editTextName.setText(name ?: userIdFromEmail) // 이름이 없으면 이메일 ID 사용
                } else {
                    Log.d(TAG, "사용자 문서가 존재하지 않음")
                    editTextName.setText(userIdFromEmail)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 정보 로드 실패", e)
                Toast.makeText(this, 
                    "프로필 정보 로드 실패: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                editTextName.setText(userIdFromEmail)
            }
    }
} 