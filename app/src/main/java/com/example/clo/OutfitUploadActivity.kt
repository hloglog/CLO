package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class OutfitUploadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit_upload)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.menu_mypage

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_search -> {
                    Toast.makeText(this, "검색 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_mypage -> {
                    startActivity(Intent(this, MyPageActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
} 