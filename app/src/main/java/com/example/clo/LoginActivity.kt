package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth.apply {
            firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
        db = Firebase.firestore

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
                        // 로그인 성공 시
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            val userId = firebaseUser.uid
                            
                            // Firestore에 마지막 로그인 시간 업데이트
                            db.collection("users").document(userId)
                                .update("lastLogin", Date())
                                .addOnSuccessListener {
                                    Log.d(TAG, "마지막 로그인 시간 업데이트 성공")
                                    
                                    // SharedPreferences에 로그인 상태 저장
                                    val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
                                    with (sharedPref.edit()) {
                                        putBoolean("is_logged_in", true)
                                        putString("user_id", userId)
                                        apply()
                                    }
                                    
                                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "마지막 로그인 시간 업데이트 실패", e)
                                    // 로그인 시간 업데이트 실패해도 로그인은 성공으로 처리
                                    val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
                                    with (sharedPref.edit()) {
                                        putBoolean("is_logged_in", true)
                                        putString("user_id", userId)
                                        apply()
                                    }
                                    
                                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                        }
                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("no user record") == true ->
                                "등록되지 않은 이메일입니다"
                            task.exception?.message?.contains("password is invalid") == true ->
                                "잘못된 비밀번호입니다"
                            else -> "로그인 실패: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvSwitchToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
} 