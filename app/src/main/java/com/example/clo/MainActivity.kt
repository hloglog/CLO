package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 2초 후에 로그인 화면으로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            // 로그인 화면으로 이동
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // 스플래시 액티비티 종료
        }, 2000) // 2초 딜레이
    }
}