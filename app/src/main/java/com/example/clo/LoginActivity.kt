package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.*

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth.apply {
            firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
        database = FirebaseDatabase.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSwitchToSignUp = findViewById<TextView>(R.id.tvSwitchToSignUp)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "로그인 중..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    btnLogin.isEnabled = true
                    btnLogin.text = "로그인"

                    if (task.isSuccessful) {
                        // 로그인 성공 시 마지막 로그인 시간 업데이트
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            val userId = firebaseUser.uid
                            database.reference.child("users").child(userId)
                                .child("lastLogin")
                                .setValue(Date().time)
                        }
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "로그인에 실패했습니다: ${task.exception?.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvSwitchToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
} 