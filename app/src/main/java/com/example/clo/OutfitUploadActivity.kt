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
} 