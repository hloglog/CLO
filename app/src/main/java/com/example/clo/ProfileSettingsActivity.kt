package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextName: EditText
    private lateinit var editTextId: EditText
    private lateinit var buttonLogout: Button
    private lateinit var buttonDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        auth = Firebase.auth

        // 뷰 바인딩
        editTextName = findViewById(R.id.edit_text_name)
        editTextId = findViewById(R.id.edit_text_id)
        buttonLogout = findViewById(R.id.button_logout)
        buttonDone = findViewById(R.id.button_done)

        // TODO: 현재 사용자 정보 (이름, 아이디)를 EditText에 로드

        // 이미지 수정 버튼 클릭 리스너
        findViewById<Button>(R.id.button_edit_image).setOnClickListener {
            // TODO: 이미지 수정 로직 구현 (갤러리 열기 등)
            Toast.makeText(this, "이미지 수정 클릭", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃 버튼 클릭 리스너
        buttonLogout.setOnClickListener {
            // Firebase Authentication 로그아웃
            auth.signOut()

            // SharedPreferences에 저장된 로그인 상태 삭제
            val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putBoolean("is_logged_in", false)
                remove("user_id") // 저장된 사용자 ID도 삭제
                apply()
            }

            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

            // 로그인 화면으로 이동
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // 이전 액티비티 스택 모두 지우기
            startActivity(intent)
            finish()
        }

        // 완료 버튼 클릭 리스너
        buttonDone.setOnClickListener {
            // TODO: 변경된 프로필 정보 저장 로직 구현 (이름, 아이디)
            val newName = editTextName.text.toString()
            val newId = editTextId.text.toString()
            Toast.makeText(this, "완료 클릭: 이름 - $newName, 아이디 - $newId", Toast.LENGTH_SHORT).show()
            finish() // 마이페이지로 돌아가기
        }

        // 하단 네비게이션 바 설정 (마이페이지 아이템 선택 상태로 표시)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_mypage

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_search -> {
                    // TODO: 검색 화면으로 이동
                    Toast.makeText(this, "설정에서 검색 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_home -> {
                    // TODO: 홈 화면으로 이동
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // 현재 액티비티 종료
                    true
                }
                R.id.navigation_mypage -> {
                    // TODO: 마이페이지 화면으로 이동 (현재 화면에서 완료 버튼으로 돌아가므로 여기서는 아무것도 하지 않음)
                    true
                }
                else -> false
            }
        }
    }
} 