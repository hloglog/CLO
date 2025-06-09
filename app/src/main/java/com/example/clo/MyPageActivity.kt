package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyPageActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textUsername: TextView
    private lateinit var textFollowers: TextView
    private lateinit var imageProfile: ImageView
    private val TAG = "MyPageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        auth = Firebase.auth
        db = Firebase.firestore

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 뷰 바인딩
        textUsername = findViewById(R.id.text_username)
        textFollowers = findViewById(R.id.text_followers)
        imageProfile = findViewById(R.id.image_profile)

        // 현재 로그인된 사용자 정보 표시
        loadUserProfile()

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_mypage // 마이페이지 아이템 선택 상태로 표시

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_home -> {
                    // TODO: 홈 화면으로 이동
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_mypage -> {
                    // 현재 화면
                    true
                }
                else -> false
            }
        }

        // 프로필 설정 버튼 클릭 리스너
        val buttonProfileSettings = findViewById<Button>(R.id.button_profile_settings)
        buttonProfileSettings.setOnClickListener {
            val intent = Intent(this, ProfileSettingsActivity::class.java)
            startActivity(intent)
        }

        // CLOSET 버튼 클릭 리스너
        val buttonCloset = findViewById<Button>(R.id.button_closet)
        buttonCloset.setOnClickListener {
            val intent = Intent(this, ClosetActivity::class.java)
            startActivity(intent)
        }

        // TODO: TODAY 섹션 착장 목록 표시 (RecyclerView 등)
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 포커스를 받을 때마다 프로필 정보 새로고침
        loadUserProfile()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        // Firestore에서 사용자 정보 로드
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val followers = document.getLong("followers") ?: 0
                    
                    textUsername.text = name ?: auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                    textFollowers.text = "팔로워 ${followers}명"
                    
                    // TODO: 프로필 이미지 로드 (Glide 등 사용)
                } else {
                    Log.d(TAG, "사용자 문서가 존재하지 않음")
                    textUsername.text = auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                    textFollowers.text = "팔로워 0명"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 정보 로드 실패", e)
                Toast.makeText(this, 
                    "프로필 정보 로드 실패: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                textUsername.text = auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                textFollowers.text = "팔로워 0명"
            }
    }
} 