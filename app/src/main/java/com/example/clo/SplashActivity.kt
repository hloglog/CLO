package com.example.clo

import android.content.Context
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
        val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        if (currentUser != null && isLoggedIn) {
            // FirebaseAuth에도 로그인되어 있고 SharedPreferences에도 로그인 상태가 true이면 홈 화면으로 이동
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // 로그인되지 않았거나 자동 로그인 상태가 아니면 로그인 화면으로 이동
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }

    // SharedPreferences에서 로그인 상태 확인 (HomeActivity에서 복사)
    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }
} 