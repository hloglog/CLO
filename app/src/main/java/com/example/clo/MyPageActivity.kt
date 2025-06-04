package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MyPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_mypage // 마이페이지 아이템 선택 상태로 표시

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_search -> {
                    // TODO: 검색 화면으로 이동
                    Toast.makeText(this, "마이페이지에서 검색 클릭", Toast.LENGTH_SHORT).show()
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
} 