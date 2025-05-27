package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // 2초 후에 로그인 상태 확인
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginState()
        }, 2000)
    }

    private fun checkLoginState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 이미 로그인된 사용자가 있으면 메인 화면으로 이동
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // 로그인된 사용자가 없으면 로그인 화면으로 이동
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
} 